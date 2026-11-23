package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.ActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.BroadcastNotification
import com.example.data.model.StudentProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminControlRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Sign in required.")

    // --- Announcements & Notifications ---
    fun observePublishedAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<Announcement> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
            }

    fun observeAllAnnouncementsAdmin(): Flow<List<Announcement>> =
        db.collection("announcements")
            .snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<Announcement> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
            }

    fun observeActiveNotifications(): Flow<List<BroadcastNotification>> =
        db.collection("notifications")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(BroadcastNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeReadNotificationIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) throw IllegalStateException("Missing UID")
        emitAll(
            db.collection("students").document(uid).collection("notifications")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
        )
    }

    suspend fun markNotificationRead(uid: String, notifId: String) {
        try {
            db.collection("students").document(uid).collection("notifications").document(notifId)
                .set(
                    mapOf(
                        "notifId" to notifId,
                        "read" to true,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "students/$uid/notifications/$notifId")
        }
    }

    suspend fun saveAnnouncement(admin: AdminUser, item: Announcement, isNew: Boolean): Result<Unit> {
        val ref = if (isNew || item.id.isBlank()) db.collection("announcements").document()
        else db.collection("announcements").document(item.id)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        return try {
            val uid = requireUid()
            if (isNew || !ref.get().await().exists()) {
                val payload = mapOf(
                    "title" to item.title.trim().take(160),
                    "body" to item.body.trim().take(2000),
                    "pinned" to item.pinned,
                    "published" to item.published,
                    "attachmentDocId" to item.attachmentDocId?.takeIf { it.isNotBlank() },
                    "attachmentName" to item.attachmentName?.takeIf { it.isNotBlank() },
                    "attachmentType" to item.attachmentType?.takeIf { it.isNotBlank() },
                    "authorUid" to uid,
                    "authorName" to admin.displayName.take(80),
                    "publishDate" to item.publishDate.ifBlank { today }.take(40),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to item.title.trim().take(160),
                    "body" to item.body.trim().take(2000),
                    "pinned" to item.pinned,
                    "published" to item.published,
                    "publishDate" to item.publishDate.ifBlank { today }.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!item.attachmentDocId.isNullOrBlank()) updates["attachmentDocId"] = item.attachmentDocId
                if (!item.attachmentName.isNullOrBlank()) updates["attachmentName"] = item.attachmentName
                if (!item.attachmentType.isNullOrBlank()) updates["attachmentType"] = item.attachmentType
                ref.update(updates).await()
            }

            if (item.published) {
                db.collection("notifications").document().set(
                    mapOf(
                        "title" to "Notice: ${item.title.trim()}".take(120),
                        "body" to item.body.trim().take(400),
                        "type" to "ANNOUNCEMENT",
                        "targetId" to ref.id,
                        "active" to true,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            writeLog(admin, "SAVE_ANNOUNCEMENT", "announcement", ref.id, "Saved notice '${item.title}'")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(admin: AdminUser, id: String): Result<Unit> {
        val ref = db.collection("announcements").document(id)
        return try {
            ref.delete().await()
            writeLog(admin, "DELETE_ANNOUNCEMENT", "announcement", id, "Deleted announcement $id")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    // --- Students Management ---
    fun observeAllStudentsAdmin(): Flow<List<StudentProfile>> =
        db.collection("students")
            .snapshots()
            .map { snap ->
                snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastActiveAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }

    suspend fun setStudentDisabled(admin: AdminUser, student: StudentProfile, disabled: Boolean): Result<Unit> {
        val ref = db.collection("students").document(student.uid)
        return try {
            ref.update(
                mapOf(
                    "disabled" to disabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val lbRef = db.collection("leaderboard").document(student.uid)
            if (lbRef.get().await().exists()) {
                lbRef.update(
                    mapOf(
                        "active" to !disabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeLog(
                admin,
                if (disabled) "DISABLE_STUDENT" else "REACTIVATE_STUDENT",
                "student",
                student.uid,
                "${if (disabled) "Disabled" else "Reactivated"} student ${student.fullName} (${student.studentCode})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(admin: AdminUser, student: StudentProfile): Result<Unit> {
        val ref = db.collection("students").document(student.uid)
        return try {
            ref.delete().await()
            val lbRef = db.collection("leaderboard").document(student.uid)
            if (lbRef.get().await().exists()) lbRef.delete().await()
            writeLog(admin, "REMOVE_STUDENT", "student", student.uid, "Removed student ${student.fullName} (${student.studentCode})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    // --- Multi-Admin Management ---
    fun observeAllAdmins(): Flow<List<AdminUser>> =
        db.collection("admins")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.role != AdminRole.SUPER_ADMIN.name }
            }

    fun observeAdminInvites(): Flow<List<AdminInvite>> =
        db.collection("admin_invites")
            .snapshots()
            .map { snap ->
                snap.toObjects(AdminInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    suspend fun inviteAdmin(
        superAdmin: AdminUser,
        email: String,
        displayName: String,
        role: AdminRole,
        permissions: List<String>
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase(Locale.US)
        if (!cleanEmail.contains('@') || cleanEmail.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid Google email address."))
        }
        if (role == AdminRole.SUPER_ADMIN) {
            return Result.failure(IllegalArgumentException("Super Admin ownership cannot be delegated via invite."))
        }
        val ref = db.collection("admin_invites").document(cleanEmail)
        return try {
            val uid = requireUid()
            ref.set(
                mapOf(
                    "email" to cleanEmail,
                    "displayName" to displayName.trim().ifBlank { cleanEmail.substringBefore('@') }.take(80),
                    "role" to role.name,
                    "active" to true,
                    "permissions" to permissions.take(16),
                    "invitedByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeLog(superAdmin, "INVITE_ADMIN", "admin_invite", cleanEmail, "Invited $cleanEmail as ${role.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateAdminAccount(
        superAdmin: AdminUser,
        target: AdminUser,
        displayName: String,
        role: AdminRole,
        active: Boolean,
        permissions: List<String>
    ): Result<Unit> {
        if (target.resolvedRole() == AdminRole.SUPER_ADMIN && (!active || role != AdminRole.SUPER_ADMIN)) {
            return Result.failure(IllegalArgumentException("Super Admin cannot be deactivated or demoted."))
        }
        val ref = db.collection("admins").document(target.uid)
        return try {
            ref.update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "role" to role.name,
                    "active" to active,
                    "permissions" to permissions.take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeLog(superAdmin, "UPDATE_ADMIN", "admin", target.uid, "Updated ${target.email} (role=${role.name}, active=$active)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminAccount(superAdmin: AdminUser, target: AdminUser): Result<Unit> {
        if (target.resolvedRole() == AdminRole.SUPER_ADMIN || target.uid == superAdmin.uid) {
            return Result.failure(IllegalArgumentException("Cannot remove the Super Admin account."))
        }
        val ref = db.collection("admins").document(target.uid)
        return try {
            ref.delete().await()
            val inviteRef = db.collection("admin_invites").document(target.email.lowercase(Locale.US))
            if (inviteRef.get().await().exists()) inviteRef.delete().await()
            writeLog(superAdmin, "REMOVE_ADMIN", "admin", target.uid, "Removed administrator ${target.email}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun revokeAdminInvite(superAdmin: AdminUser, email: String): Result<Unit> {
        val ref = db.collection("admin_invites").document(email.lowercase(Locale.US))
        return try {
            ref.delete().await()
            writeLog(superAdmin, "REVOKE_INVITE", "admin_invite", email, "Revoked admin invite for $email")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }

    // --- Activity Logs ---
    fun observeActivityLogs(): Flow<List<ActivityLog>> =
        db.collection("activity_logs")
            .snapshots()
            .map { snap ->
                snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    // --- App Settings ---
    suspend fun updateAppSettings(admin: AdminUser, settings: AppSettings): Result<Unit> {
        val ref = db.collection("app_settings").document("global")
        return try {
            val uid = requireUid()
            val isSuper = admin.resolvedRole() == AdminRole.SUPER_ADMIN
            val updates = mutableMapOf<String, Any>(
                "appName" to settings.appName.trim().ifBlank { "দরবার" }.take(40),
                "tagline" to settings.tagline.trim().ifBlank { "Your Digital Learning & Knowledge Hub" }.take(120),
                "defaultTheme" to settings.defaultTheme,
                "maxUploadMb" to settings.maxUploadMb.coerceIn(1L, 10L),
                "allowStudentQuiz" to settings.allowStudentQuiz,
                "allowStudentDownloads" to settings.allowStudentDownloads,
                "notificationsEnabled" to settings.notificationsEnabled,
                "announcementsEnabled" to settings.announcementsEnabled,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isSuper) {
                updates["maintenanceMode"] = settings.maintenanceMode
                updates["requireStrictVerification"] = settings.requireStrictVerification
            }
            if (ref.get().await().exists()) {
                ref.update(updates).await()
            } else if (isSuper) {
                ref.set(updates).await()
            } else {
                return Result.failure(IllegalStateException("Settings must be initialized by Super Admin first."))
            }
            writeLog(admin, "UPDATE_SETTINGS", "app_settings", "global", "Updated application settings")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    private suspend fun writeLog(
        admin: AdminUser,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        try {
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to admin.uid,
                    "adminName" to admin.displayName.take(80),
                    "adminRole" to admin.role,
                    "action" to action.take(80),
                    "targetType" to targetType.take(60),
                    "targetId" to targetId.take(128),
                    "details" to details.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "activity_logs")
        }
    }
}
