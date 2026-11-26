package com.example.data.repository

import com.example.core.FileStorageHelper
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.DocumentChunk
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ContentRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    private fun <T> Flow<T>.handleSnapshotErrors(path: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    fun observeAppSettings(): Flow<AppSettings> = flow {
        requireUserId()
        emit(Unit)
    }.let {
        db.collection("app_settings").document("global")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AppSettings()
            }
            .handleSnapshotErrors("app_settings/global")
    }

    fun observeSubjects(isAdminView: Boolean, isSuperAdminEmail: Boolean, uid: String): Flow<List<SubjectItem>> {
        val query = when {
            isAdminView && isSuperAdminEmail -> db.collection("subjects")
            isAdminView -> db.collection("subjects").whereArrayContains("adminUids", uid)
            else -> db.collection("subjects").whereEqualTo("visible", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(SubjectItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .handleSnapshotErrors("subjects")
    }

    fun observeCategories(isAdminView: Boolean, isSuperAdminEmail: Boolean, uid: String): Flow<List<CategoryItem>> {
        val query = when {
            isAdminView && isSuperAdminEmail -> db.collection("categories")
            isAdminView -> db.collection("categories").whereArrayContains("adminUids", uid)
            else -> db.collection("categories").whereEqualTo("visible", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(CategoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .handleSnapshotErrors("categories")
    }

    fun observeDocuments(isAdminView: Boolean, isSuperAdminEmail: Boolean, uid: String): Flow<List<StudyDocument>> {
        val query = when {
            isAdminView && isSuperAdminEmail -> db.collection("documents")
            isAdminView -> db.collection("documents").whereArrayContains("adminUids", uid)
            else -> db.collection("documents").whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudyDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleSnapshotErrors("documents")
    }

    fun observeAnnouncements(isAdminView: Boolean, isSuperAdminEmail: Boolean, uid: String): Flow<List<Announcement>> {
        val query = when {
            isAdminView && isSuperAdminEmail -> db.collection("announcements")
            isAdminView -> db.collection("announcements").whereArrayContains("adminUids", uid)
            else -> db.collection("announcements").whereEqualTo("published", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<Announcement> { it.pinned }.thenByDescending { it.createdAt?.seconds ?: 0L })
            }
            .handleSnapshotErrors("announcements")
    }

    fun observeQuizQuestions(isAdminView: Boolean, isSuperAdminEmail: Boolean, uid: String): Flow<List<QuizQuestion>> {
        val query = when {
            isAdminView && isSuperAdminEmail -> db.collection("quiz_questions")
            isAdminView -> db.collection("quiz_questions").whereArrayContains("adminUids", uid)
            else -> db.collection("quiz_questions").whereEqualTo("approved", true)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleSnapshotErrors("quiz_questions")
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboard")
            .whereGreaterThanOrEqualTo("totalPoints", 0L)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.totalPoints }
            }
            .handleSnapshotErrors("leaderboard")

    suspend fun fetchDocumentBytes(documentId: String, isAdminView: Boolean): Result<ByteArray> {
        val path = "document_chunks"
        return try {
            requireUserId()
            require(documentId.isNotBlank()) { "Document ID cannot be blank." }
            val query = if (isAdminView) {
                db.collection(path).whereEqualTo("documentId", documentId)
            } else {
                db.collection(path)
                    .whereEqualTo("documentId", documentId)
                    .whereEqualTo("published", true)
            }
            val snap = query.get().await()
            val chunks = snap.toObjects(DocumentChunk::class.java)
                .sortedBy { it.chunkIndex }
            if (chunks.isEmpty()) {
                return Result.failure(IllegalStateException("Document file data is not available yet."))
            }
            val bytes = FileStorageHelper.decodeBase64Chunks(chunks.map { it.dataBase64 })
            Result.success(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialCatalogIfAdmin(adminUid: String, adminName: String, adminUids: List<String>): Result<Unit> {
        return try {
            val uid = requireUserId()
            val settingsRef = db.collection("app_settings").document("global")
            val settingsSnap = settingsRef.get().await()
            if (!settingsSnap.exists()) {
                settingsRef.set(
                    mapOf(
                        "appName" to "দরবার",
                        "tagline" to "Your Digital Learning & Knowledge Hub",
                        "defaultThemeMode" to "SYSTEM",
                        "maxUploadSizeMb" to 5L,
                        "allowStudentQuiz" to true,
                        "allowStudentDownloads" to true,
                        "dailyChallengeEnabled" to true,
                        "notificationsEnabled" to true,
                        "maintenanceMode" to false,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val existingSubjects = db.collection("subjects").limit(1).get().await()
            if (existingSubjects.isEmpty) {
                DefaultCatalog.defaultSubjects.forEach { seed ->
                    db.collection("subjects").document(seed.id).set(
                        mapOf(
                            "nameEn" to seed.nameEn,
                            "nameBn" to seed.nameBn,
                            "description" to seed.description,
                            "iconKey" to seed.iconKey,
                            "accentHex" to seed.accentHex,
                            "sortOrder" to seed.sortOrder,
                            "visible" to true,
                            "createdByUid" to adminUid,
                            "adminUids" to adminUids,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingCategories = db.collection("categories").limit(1).get().await()
            if (existingCategories.isEmpty) {
                DefaultCatalog.defaultCategoryNames.forEachIndexed { idx, name ->
                    val catId = "cat_${idx + 1}"
                    db.collection("categories").document(catId).set(
                        mapOf(
                            "name" to name,
                            "subjectId" to "ALL",
                            "visible" to true,
                            "sortOrder" to (idx + 1).toLong(),
                            "createdByUid" to adminUid,
                            "adminUids" to adminUids,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingAnnouncements = db.collection("announcements").limit(1).get().await()
            if (existingAnnouncements.isEmpty) {
                db.collection("announcements").document("welcome_notice").set(
                    mapOf(
                        "title" to "Welcome to দরবার — Digital Learning Hub",
                        "body" to "Explore all six subject libraries (Bengali, English, Physics, Chemistry, Mathematics, Biology) and test your skills in the দরবার AI GK Challenge!",
                        "pinned" to true,
                        "published" to true,
                        "publishDateLabel" to "Official Notice",
                        "createdByUid" to adminUid,
                        "createdByName" to adminName,
                        "adminUids" to adminUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "catalog_seed")
            Result.failure(e)
        }
    }
}
