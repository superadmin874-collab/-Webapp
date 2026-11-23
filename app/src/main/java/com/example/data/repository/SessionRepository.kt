package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AppSettings
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.StudentProfile
import com.example.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

private val Context.darbarPrefsDataStore by preferencesDataStore(name = "darbar_session_prefs")

class SessionRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private object PrefKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val LAST_STUDENT_NAME = stringPreferencesKey("last_student_name")
        val LAST_STUDENT_CODE = stringPreferencesKey("last_student_code")
    }

    val themeModeFlow: Flow<ThemeMode> = context.darbarPrefsDataStore.data.map { prefs ->
        val raw = prefs[PrefKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    val reducedMotionFlow: Flow<Boolean> = context.darbarPrefsDataStore.data.map { prefs ->
        prefs[PrefKeys.REDUCED_MOTION] ?: false
    }

    val rememberedStudentNameFlow: Flow<String> = context.darbarPrefsDataStore.data.map { prefs ->
        prefs[PrefKeys.LAST_STUDENT_NAME] ?: ""
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.darbarPrefsDataStore.edit { it[PrefKeys.THEME_MODE] = mode.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.darbarPrefsDataStore.edit { it[PrefKeys.REDUCED_MOTION] = enabled }
    }

    suspend fun rememberStudentSessionLocally(name: String, code: String) {
        context.darbarPrefsDataStore.edit {
            it[PrefKeys.LAST_STUDENT_NAME] = name
            it[PrefKeys.LAST_STUDENT_CODE] = code
        }
    }

    suspend fun clearRememberedStudentName() {
        context.darbarPrefsDataStore.edit {
            it.remove(PrefKeys.LAST_STUDENT_NAME)
        }
    }

    fun observeAdminProfile(uid: String): Flow<AdminUser?> =
        db.collection("admins").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeStudentProfile(uid: String): Flow<StudentProfile?> =
        db.collection("students").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeAppSettings(): Flow<AppSettings> =
        db.collection("app_settings").document("global")
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) AppSettings(id = "global")
                else snapshot.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AppSettings(id = "global")
            }

    /**
     * Checks if the signed-in Google user is the bootstrapped Super Admin or has an active
     * admin invite in `/admin_invites/{email}`, activates their `/admins/{uid}` profile,
     * updates `lastLoginAt`, logs LOGIN activity, and initializes default subjects/categories/settings.
     */
    suspend fun syncAdminAccessOnLogin(): Result<AdminUser?> {
        val user = auth.currentUser ?: return Result.success(null)
        val uid = user.uid
        val email = (user.email ?: "").trim().lowercase(Locale.US)
        val isVerified = user.isEmailVerified
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').ifBlank { "Administrator" }

        return try {
            val adminRef = db.collection("admins").document(uid)
            val existingAdminSnap = adminRef.get().await()

            var resolvedAdmin: AdminUser? = null

            if (existingAdminSnap.exists()) {
                val existing = existingAdminSnap.toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null && existing.active) {
                    adminRef.update(
                        mapOf(
                            "lastLoginAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    resolvedAdmin = existing
                }
            } else if (isVerified && email == DarbarCurriculumDefaults.SUPER_ADMIN_EMAIL) {
                val payload = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to displayName.take(80),
                    "role" to AdminRole.SUPER_ADMIN.name,
                    "active" to true,
                    "permissions" to listOf("all"),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                adminRef.set(payload).await()
                resolvedAdmin = adminRef.get().await()
                    .toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else if (isVerified && email.isNotBlank()) {
                val inviteRef = db.collection("admin_invites").document(email)
                val inviteSnap = inviteRef.get().await()
                if (inviteSnap.exists()) {
                    val active = inviteSnap.getBoolean("active") ?: false
                    val role = inviteSnap.getString("role") ?: AdminRole.ADMIN.name
                    val inviteName = inviteSnap.getString("displayName")?.takeIf { it.isNotBlank() } ?: displayName
                    @Suppress("UNCHECKED_CAST")
                    val perms = (inviteSnap.get("permissions") as? List<*>)?.filterIsInstance<String>()
                        ?: AdminPermissions.ALL_ADMIN_PERMISSIONS
                    if (active && role != AdminRole.SUPER_ADMIN.name) {
                        val payload = mapOf(
                            "uid" to uid,
                            "email" to email,
                            "displayName" to inviteName.take(80),
                            "role" to role,
                            "active" to true,
                            "permissions" to perms,
                            "lastLoginAt" to FieldValue.serverTimestamp(),
                            "createdBy" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        adminRef.set(payload).await()
                        inviteRef.update(
                            mapOf(
                                "claimedByUid" to uid,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        resolvedAdmin = adminRef.get().await()
                            .toObject(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
            }

            if (resolvedAdmin != null) {
                ensureDefaultsInitialized(resolvedAdmin)
                logLoginActivity(resolvedAdmin)
            }

            Result.success(resolvedAdmin)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("SessionRepo", "Admin check skipped or failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun ensureDefaultsInitialized(admin: AdminUser) {
        try {
            // Initialize the 6 core subjects if missing
            for (subj in DarbarCurriculumDefaults.DEFAULT_SUBJECTS) {
                val ref = db.collection("subjects").document(subj.id)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "titleEn" to subj.titleEn,
                            "titleBn" to subj.titleBn,
                            "description" to subj.description,
                            "orderIndex" to subj.orderIndex,
                            "visible" to subj.visible,
                            "accentHex" to subj.accentHex,
                            "iconKey" to subj.iconKey,
                            "updatedBy" to admin.uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            // Initialize the 8 core categories if missing
            for (cat in DarbarCurriculumDefaults.DEFAULT_CATEGORIES) {
                val ref = db.collection("categories").document(cat.id)
                if (!ref.get().await().exists()) {
                    ref.set(
                        mapOf(
                            "name" to cat.name,
                            "nameBn" to cat.nameBn,
                            "description" to cat.description,
                            "orderIndex" to cat.orderIndex,
                            "visible" to cat.visible,
                            "createdBy" to admin.uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            // Initialize global settings if missing and caller is Super Admin
            if (admin.resolvedRole() == AdminRole.SUPER_ADMIN) {
                val settingsRef = db.collection("app_settings").document("global")
                if (!settingsRef.get().await().exists()) {
                    settingsRef.set(
                        mapOf(
                            "appName" to "দরবার",
                            "tagline" to "Your Digital Learning & Knowledge Hub",
                            "defaultTheme" to "SYSTEM",
                            "maxUploadMb" to 5L,
                            "allowStudentQuiz" to true,
                            "allowStudentDownloads" to true,
                            "notificationsEnabled" to true,
                            "announcementsEnabled" to true,
                            "maintenanceMode" to false,
                            "requireStrictVerification" to true,
                            "updatedBy" to admin.uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "defaults_init")
        }
    }

    private suspend fun logLoginActivity(admin: AdminUser) {
        try {
            val logRef = db.collection("activity_logs").document()
            logRef.set(
                mapOf(
                    "adminUid" to admin.uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "ADMIN_LOGIN",
                    "targetType" to "admin_session",
                    "targetId" to admin.uid,
                    "details" to "${admin.displayName} (${admin.role}) signed in to Darbar Admin Console",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "activity_logs")
        }
    }

    suspend fun registerOrUpdateStudentName(fullName: String): Result<StudentProfile> {
        val cleanName = fullName.trim()
        if (cleanName.length < 2 || cleanName.length > 80) {
            return Result.failure(IllegalArgumentException("Please enter your full name (2–80 characters)."))
        }
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Authentication required."))

        val docRef = db.collection("students").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentCode = existing.getString("studentCode") ?: generateStudentCode(uid)
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                // Also update leaderboard display name if present
                val lbRef = db.collection("leaderboard").document(uid)
                if (lbRef.get().await().exists()) {
                    lbRef.update(
                        mapOf(
                            "fullName" to cleanName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                rememberStudentSessionLocally(cleanName, currentCode)
            } else {
                val newCode = generateStudentCode(uid)
                val payload = mapOf(
                    "uid" to uid,
                    "studentCode" to newCode,
                    "fullName" to cleanName,
                    "disabled" to false,
                    "downloadsCount" to 0L,
                    "quizzesTaken" to 0L,
                    "totalQuizScore" to 0L,
                    "totalQuestionsAnswered" to 0L,
                    "achievements" to emptyList<String>(),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                rememberStudentSessionLocally(cleanName, newCode)
            }
            val saved = docRef.get().await()
                .toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Failed to load student profile after saving.")
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun generateStudentCode(uid: String): String {
        val hashPart = uid.hashCode().let { kotlin.math.abs(it) % 9000 + 1000 }
        val randPart = Random.nextInt(10, 99)
        return "DRB-$hashPart$randPart"
    }
}
