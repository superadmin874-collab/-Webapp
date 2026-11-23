package com.example.data.repository

import android.content.Context
import com.example.core.DocumentStorageHelper
import com.example.core.OperationType
import com.example.core.PickedFilePayload
import com.example.core.handleFirestoreError
import com.example.data.model.AdminUser
import com.example.data.model.Category
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.DocumentVersion
import com.example.data.model.StudentDownload
import com.example.data.model.StudentFavorite
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File

class DocumentRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observePublishedDocuments(): Flow<List<StudyDocument>> =
        db.collection("documents")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(StudyDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllDocumentsAdmin(): Flow<List<StudyDocument>> =
        db.collection("documents")
            .snapshots()
            .map { snap ->
                snap.toObjects(StudyDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }

    fun observeVisibleSubjects(): Flow<List<Subject>> =
        db.collection("subjects")
            .whereEqualTo("visible", true)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (list.isEmpty()) DarbarCurriculumDefaults.DEFAULT_SUBJECTS
                else list.sortedBy { it.orderIndex }
            }

    fun observeAllSubjectsAdmin(): Flow<List<Subject>> =
        db.collection("subjects")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(Subject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (list.isEmpty()) DarbarCurriculumDefaults.DEFAULT_SUBJECTS
                else list.sortedBy { it.orderIndex }
            }

    fun observeVisibleCategories(): Flow<List<Category>> =
        db.collection("categories")
            .whereEqualTo("visible", true)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (list.isEmpty()) DarbarCurriculumDefaults.DEFAULT_CATEGORIES
                else list.sortedBy { it.orderIndex }
            }

    fun observeAllCategoriesAdmin(): Flow<List<Category>> =
        db.collection("categories")
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (list.isEmpty()) DarbarCurriculumDefaults.DEFAULT_CATEGORIES
                else list.sortedBy { it.orderIndex }
            }

    fun observeFavorites(uid: String): Flow<List<StudentFavorite>> = flow {
        if (uid.isBlank()) throw IllegalStateException("Missing student UID")
        emitAll(
            db.collection("students").document(uid).collection("favorites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentFavorite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeDownloads(uid: String): Flow<List<StudentDownload>> = flow {
        if (uid.isBlank()) throw IllegalStateException("Missing student UID")
        emitAll(
            db.collection("students").document(uid).collection("downloads")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentDownload::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.downloadedAt?.seconds ?: 0L }
                }
        )
    }

    suspend fun toggleFavorite(doc: StudyDocument, currentlyFavorited: Boolean): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        val favRef = db.collection("students").document(uid).collection("favorites").document(doc.id)
        return try {
            if (currentlyFavorited) {
                favRef.delete().await()
                Result.success(false)
            } else {
                favRef.set(
                    mapOf(
                        "docId" to doc.id,
                        "title" to doc.title.take(160),
                        "subjectId" to doc.subjectId,
                        "subjectName" to doc.subjectName,
                        "fileType" to doc.fileType,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, favRef.path)
            Result.failure(e)
        }
    }

    suspend fun loadDocumentBytes(doc: StudyDocument): Result<ByteArray> {
        return try {
            if (doc.chunkCount <= 0L) {
                return Result.failure(IllegalStateException("No embedded file data for this document."))
            }
            val chunks = mutableListOf<String>()
            val chunksRef = db.collection("documents").document(doc.id).collection("chunks")
            for (i in 0 until doc.chunkCount.toInt()) {
                val snap = chunksRef.document("chunk_$i").get().await()
                val encoded = snap.getString("data")
                    ?: throw IllegalStateException("Missing file chunk $i.")
                chunks += encoded
            }
            Result.success(DocumentStorageHelper.decodeBase64Chunks(chunks))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "documents/${doc.id}/chunks")
            Result.failure(e)
        }
    }

    suspend fun downloadDocumentToDevice(doc: StudyDocument): Result<File> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val bytes = loadDocumentBytes(doc).getOrThrow()
            val internalFile = DocumentStorageHelper.saveToInternalDocs(context, doc.id, doc.fileName, bytes)
            val publicPath = DocumentStorageHelper.exportToPublicDownloads(context, doc.fileName, doc.mimeType, bytes)

            // Record student download history
            val dlRef = db.collection("students").document(uid).collection("downloads").document()
            dlRef.set(
                mapOf(
                    "docId" to doc.id,
                    "title" to doc.title.take(160),
                    "subjectId" to doc.subjectId,
                    "subjectName" to doc.subjectName,
                    "fileName" to doc.fileName.take(200),
                    "fileType" to doc.fileType,
                    "fileSizeBytes" to doc.fileSizeBytes,
                    "localPath" to publicPath.take(500),
                    "downloadedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Safely increment document downloadCount (+1) and student downloadsCount (+1)
            db.collection("documents").document(doc.id).update(
                mapOf(
                    "downloadCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val studentRef = db.collection("students").document(uid)
            if (studentRef.get().await().exists()) {
                studentRef.update(
                    mapOf(
                        "downloadsCount" to FieldValue.increment(1L),
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(internalFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "documents/${doc.id}")
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        admin: AdminUser,
        payload: PickedFilePayload,
        title: String,
        description: String,
        subject: Subject,
        category: Category,
        chapter: String,
        tags: List<String>,
        published: Boolean,
        featured: Boolean,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val docRef = db.collection("documents").document()
        return try {
            val uid = requireUid()
            val chunks = DocumentStorageHelper.splitIntoBase64Chunks(payload.bytes)
            if (chunks.size > 50) {
                return Result.failure(IllegalArgumentException("File exceeds maximum chunk count (50)."))
            }

            val cleanTitle = title.trim().ifBlank { payload.fileName.substringBeforeLast('.') }.take(160)
            val cleanChapter = chapter.trim().ifBlank { "General" }.take(120)
            val cleanTags = tags.map { it.trim() }.filter { it.isNotBlank() }.take(12)

            onProgress(0.1f)
            val docData = mapOf(
                "title" to cleanTitle,
                "description" to description.trim().take(1000),
                "subjectId" to subject.id,
                "subjectName" to "${subject.titleEn} (${subject.titleBn})",
                "categoryId" to category.id,
                "categoryName" to category.name,
                "chapter" to cleanChapter,
                "tags" to cleanTags,
                "fileName" to payload.fileName.take(200),
                "fileType" to payload.fileType.take(20),
                "mimeType" to payload.mimeType.take(120),
                "fileSizeBytes" to payload.bytes.size.toLong(),
                "chunkCount" to chunks.size.toLong(),
                "published" to published,
                "featured" to featured,
                "version" to 1L,
                "downloadCount" to 0L,
                "uploadedByUid" to uid,
                "uploadedByName" to admin.displayName.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(docData).await()

            val chunksColl = docRef.collection("chunks")
            chunks.forEachIndexed { index, encoded ->
                chunksColl.document("chunk_$index").set(
                    mapOf(
                        "docId" to docRef.id,
                        "index" to index.toLong(),
                        "data" to encoded
                    )
                ).await()
                val p = 0.1f + 0.8f * ((index + 1).toFloat() / chunks.size.coerceAtLeast(1))
                onProgress(p)
            }

            // Record initial version snapshot
            docRef.collection("versions").document("v1").set(
                mapOf(
                    "version" to 1L,
                    "title" to cleanTitle,
                    "fileName" to payload.fileName.take(200),
                    "fileSizeBytes" to payload.bytes.size.toLong(),
                    "changeNote" to "Initial upload",
                    "editedByUid" to uid,
                    "editedByName" to admin.displayName.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Log activity
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "UPLOAD_DOCUMENT",
                    "targetType" to "document",
                    "targetId" to docRef.id,
                    "details" to "Uploaded '$cleanTitle' (${payload.fileType}) to ${subject.titleEn}",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Broadcast notification if published immediately
            if (published) {
                db.collection("notifications").document().set(
                    mapOf(
                        "title" to "New Study Material: $cleanTitle".take(120),
                        "body" to "${subject.titleEn} • ${category.name} • Chapter: $cleanChapter".take(400),
                        "type" to "DOCUMENT",
                        "targetId" to docRef.id,
                        "active" to true,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            onProgress(1f)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Edits a document with optimistic concurrency / conflict detection via `expectedVersion`.
     */
    suspend fun updateDocumentWithConflictCheck(
        admin: AdminUser,
        docId: String,
        expectedVersion: Long,
        title: String,
        description: String,
        subject: Subject,
        category: Category,
        chapter: String,
        tags: List<String>,
        published: Boolean,
        featured: Boolean,
        replacementFile: PickedFilePayload?,
        changeNote: String
    ): Result<Long> {
        val docRef = db.collection("documents").document(docId)
        return try {
            val uid = requireUid()
            val liveSnap = docRef.get().await()
            if (!liveSnap.exists()) {
                return Result.failure(IllegalStateException("Document no longer exists."))
            }
            val liveVersion = liveSnap.getLong("version") ?: 1L
            if (liveVersion != expectedVersion) {
                return Result.failure(
                    IllegalStateException(
                        "Conflict detected: Another admin updated this document to v$liveVersion while you were editing v$expectedVersion. Refresh and try again."
                    )
                )
            }

            val nextVersion = liveVersion + 1L
            val updates = mutableMapOf<String, Any>(
                "title" to title.trim().take(160),
                "description" to description.trim().take(1000),
                "subjectId" to subject.id,
                "subjectName" to "${subject.titleEn} (${subject.titleBn})",
                "categoryId" to category.id,
                "categoryName" to category.name,
                "chapter" to chapter.trim().ifBlank { "General" }.take(120),
                "tags" to tags.map { it.trim() }.filter { it.isNotBlank() }.take(12),
                "published" to published,
                "featured" to featured,
                "version" to nextVersion,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            var finalFileName = liveSnap.getString("fileName") ?: ""
            var finalSize = liveSnap.getLong("fileSizeBytes") ?: 0L

            if (replacementFile != null) {
                val chunks = DocumentStorageHelper.splitIntoBase64Chunks(replacementFile.bytes)
                val oldChunks = docRef.collection("chunks").get().await()
                for (c in oldChunks.documents) c.reference.delete().await()
                chunks.forEachIndexed { idx, encoded ->
                    docRef.collection("chunks").document("chunk_$idx").set(
                        mapOf("docId" to docId, "index" to idx.toLong(), "data" to encoded)
                    ).await()
                }
                updates["fileName"] = replacementFile.fileName.take(200)
                updates["fileType"] = replacementFile.fileType.take(20)
                updates["mimeType"] = replacementFile.mimeType.take(120)
                updates["fileSizeBytes"] = replacementFile.bytes.size.toLong()
                updates["chunkCount"] = chunks.size.toLong()
                finalFileName = replacementFile.fileName
                finalSize = replacementFile.bytes.size.toLong()
            }

            docRef.update(updates).await()

            docRef.collection("versions").document("v$nextVersion").set(
                mapOf(
                    "version" to nextVersion,
                    "title" to title.trim().take(160),
                    "fileName" to finalFileName.take(200),
                    "fileSizeBytes" to finalSize,
                    "changeNote" to changeNote.ifBlank { "Updated details" }.take(300),
                    "editedByUid" to uid,
                    "editedByName" to admin.displayName.take(80),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "UPDATE_DOCUMENT",
                    "targetType" to "document",
                    "targetId" to docId,
                    "details" to "Updated '$title' to v$nextVersion ($changeNote)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(nextVersion)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getVersionHistory(docId: String): Result<List<DocumentVersion>> {
        return try {
            val snap = db.collection("documents").document(docId)
                .collection("versions")
                .get()
                .await()
            val list = snap.toObjects(DocumentVersion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.version }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "documents/$docId/versions")
            Result.failure(e)
        }
    }

    suspend fun bulkSetPublished(admin: AdminUser, docIds: List<String>, published: Boolean): Result<Unit> {
        return try {
            val uid = requireUid()
            for (id in docIds) {
                db.collection("documents").document(id).update(
                    mapOf("published" to published, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to if (published) "BULK_PUBLISH" else "BULK_UNPUBLISH",
                    "targetType" to "document",
                    "targetId" to docIds.joinToString(",").take(128),
                    "details" to "${if (published) "Published" else "Unpublished"} ${docIds.size} document(s)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "documents/bulk")
            Result.failure(e)
        }
    }

    suspend fun deleteDocuments(admin: AdminUser, docIds: List<String>): Result<Unit> {
        return try {
            val uid = requireUid()
            for (id in docIds) {
                val docRef = db.collection("documents").document(id)
                val chunks = docRef.collection("chunks").get().await()
                for (c in chunks.documents) c.reference.delete().await()
                val versions = docRef.collection("versions").get().await()
                for (v in versions.documents) v.reference.delete().await()
                docRef.delete().await()
            }
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "DELETE_DOCUMENTS",
                    "targetType" to "document",
                    "targetId" to docIds.joinToString(",").take(128),
                    "details" to "Deleted ${docIds.size} document(s)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "documents")
            Result.failure(e)
        }
    }

    suspend fun saveSubject(admin: AdminUser, subject: Subject): Result<Unit> {
        val id = subject.id.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").lowercase()
        val ref = db.collection("subjects").document(id)
        return try {
            val uid = requireUid()
            val exists = ref.get().await().exists()
            if (exists) {
                ref.update(
                    mapOf(
                        "titleEn" to subject.titleEn.trim().take(60),
                        "titleBn" to subject.titleBn.trim().take(60),
                        "description" to subject.description.trim().take(300),
                        "orderIndex" to subject.orderIndex,
                        "visible" to subject.visible,
                        "accentHex" to subject.accentHex,
                        "iconKey" to subject.iconKey,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "titleEn" to subject.titleEn.trim().take(60),
                        "titleBn" to subject.titleBn.trim().take(60),
                        "description" to subject.description.trim().take(300),
                        "orderIndex" to subject.orderIndex,
                        "visible" to subject.visible,
                        "accentHex" to subject.accentHex,
                        "iconKey" to subject.iconKey,
                        "updatedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "SAVE_SUBJECT",
                    "targetType" to "subject",
                    "targetId" to id,
                    "details" to "Saved subject ${subject.titleEn} (visible=${subject.visible})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(admin: AdminUser, category: Category, isNew: Boolean): Result<Unit> {
        val docId = if (isNew) {
            category.name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "cat_${System.currentTimeMillis()}" }
        } else category.id
        val ref = db.collection("categories").document(docId)
        return try {
            val uid = requireUid()
            if (isNew || !ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "nameBn" to category.nameBn.trim().take(80),
                        "description" to category.description.trim().take(240),
                        "orderIndex" to category.orderIndex,
                        "visible" to category.visible,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "nameBn" to category.nameBn.trim().take(80),
                        "description" to category.description.trim().take(240),
                        "orderIndex" to category.orderIndex,
                        "visible" to category.visible,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "SAVE_CATEGORY",
                    "targetType" to "category",
                    "targetId" to docId,
                    "details" to "Saved category '${category.name}'",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(admin: AdminUser, categoryId: String): Result<Unit> {
        val ref = db.collection("categories").document(categoryId)
        return try {
            val uid = requireUid()
            ref.delete().await()
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "DELETE_CATEGORY",
                    "targetType" to "category",
                    "targetId" to categoryId,
                    "details" to "Deleted category $categoryId",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ref.path)
            Result.failure(e)
        }
    }
}
