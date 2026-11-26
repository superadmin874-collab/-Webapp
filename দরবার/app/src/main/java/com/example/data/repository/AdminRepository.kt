package com.example.data.repository

import com.example.core.FileStorageHelper
import com.example.core.OperationType
import com.example.core.PickedFilePayload
import com.example.core.handleFirestoreError
import com.example.data.model.AdminAccount
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.DefaultCatalog
import com.example.data.model.DocumentVersion
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentDirectoryEntry
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AdminRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    companion object {
        fun inviteDocIdForEmail(email: String): String =
            "inv_" + email.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")
    }

    private fun <T> Flow<T>.handleAdminErrors(path: String): Flow<T> = catch { error ->
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

    fun observeMyAdminAccount(uid: String): Flow<AdminAccount?> =
        db.collection("admins").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .handleAdminErrors("admins/$uid")

    fun observeAllAdmins(uid: String, isSuperAdminEmail: Boolean): Flow<List<AdminAccount>> {
        val query = if (isSuperAdminEmail) {
            db.collection("admins")
        } else {
            db.collection("admins").whereArrayContains("adminUids", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.role != AdminRole.SUPER_ADMIN.name }
            }
            .handleAdminErrors("admins")
    }

    fun observeAdminInvites(uid: String, isSuperAdminEmail: Boolean): Flow<List<AdminInvite>> {
        val query = if (isSuperAdminEmail) {
            db.collection("admin_invites")
        } else {
            db.collection("admin_invites").whereArrayContains("adminUids", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AdminInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleAdminErrors("admin_invites")
    }

    fun observeStudentDirectory(uid: String, isSuperAdminEmail: Boolean): Flow<List<StudentDirectoryEntry>> {
        val query = if (isSuperAdminEmail) {
            db.collection("student_directory")
        } else {
            db.collection("student_directory").whereArrayContains("adminUids", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(StudentDirectoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastActiveAt?.seconds ?: 0L }
            }
            .handleAdminErrors("student_directory")
    }

    fun observeActivityLogs(uid: String, isSuperAdminEmail: Boolean): Flow<List<AdminActivityLog>> {
        val query = if (isSuperAdminEmail) {
            db.collection("activity_logs")
        } else {
            db.collection("activity_logs").whereArrayContains("adminUids", uid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AdminActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleAdminErrors("activity_logs")
    }

    suspend fun fetchDocumentVersions(documentId: String, uid: String, isSuperAdminEmail: Boolean): Result<List<DocumentVersion>> {
        return try {
            val query = if (isSuperAdminEmail) {
                db.collection("document_versions").whereEqualTo("documentId", documentId)
            } else {
                db.collection("document_versions")
                    .whereEqualTo("documentId", documentId)
                    .whereArrayContains("adminUids", uid)
            }
            val list = query.get().await()
                .toObjects(DocumentVersion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.version }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "document_versions")
            Result.failure(e)
        }
    }

    suspend fun ensureAdminSessionIfEligible(
        uid: String,
        email: String?,
        emailVerified: Boolean,
        displayName: String?
    ): Result<AdminAccount?> {
        if (email.isNullOrBlank() || !emailVerified) return Result.success(null)
        val normalizedEmail = email.trim().lowercase()
        val adminRef = db.collection("admins").document(uid)
        var currentOp = OperationType.GET
        var currentPath = "admins/$uid"
        return try {
            val existing = adminRef.get().await()
            if (existing.exists()) {
                val account = existing.toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (account?.active == true) {
                    currentOp = OperationType.UPDATE
                    currentPath = "admins/$uid"
                    adminRef.update(
                        mapOf(
                            "lastLoginAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    logActivity(
                        adminUid = uid,
                        adminName = account.displayName,
                        actionType = "LOGIN",
                        summary = "${account.displayName} (${account.role}) signed in",
                        targetId = uid,
                        adminUids = (account.adminUids + uid).distinct()
                    )
                }
                return Result.success(account)
            }

            if (normalizedEmail == DefaultCatalog.SUPER_ADMIN_EMAIL) {
                val name = displayName?.takeIf { it.isNotBlank() } ?: "Super Admin"
                val payload = mapOf(
                    "uid" to uid,
                    "email" to normalizedEmail,
                    "displayName" to name,
                    "role" to AdminRole.SUPER_ADMIN.name,
                    "active" to true,
                    "permissions" to DefaultCatalog.allAdminPermissions,
                    "createdByUid" to uid,
                    "adminUids" to listOf(uid),
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                currentOp = OperationType.CREATE
                currentPath = "admins/$uid"
                adminRef.set(payload).await()
                logActivity(uid, name, "LOGIN", "Bootstrapped Super Admin session initialized", uid, listOf(uid))
                currentOp = OperationType.GET
                val created = adminRef.get().await()
                    .toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                return Result.success(created)
            }

            // Check if invited by Super Admin
            val inviteId = inviteDocIdForEmail(normalizedEmail)
            val inviteRef = db.collection("admin_invites").document(inviteId)
            currentOp = OperationType.GET
            currentPath = "admin_invites/$inviteId"
            val inviteSnap = inviteRef.get().await()
            if (inviteSnap.exists()) {
                val invite = inviteSnap.toObject(AdminInvite::class.java)
                if (invite != null && invite.active && invite.email == normalizedEmail) {
                    val adminName = invite.displayName.ifBlank { displayName ?: "Administrator" }
                    val teamUids = (invite.adminUids + uid).distinct().take(20)
                    currentOp = OperationType.CREATE
                    currentPath = "admins/$uid"
                    adminRef.set(
                        mapOf(
                            "uid" to uid,
                            "email" to normalizedEmail,
                            "displayName" to adminName,
                            "role" to invite.role,
                            "active" to true,
                            "permissions" to invite.permissions,
                            "createdByUid" to invite.createdByUid,
                            "adminUids" to teamUids,
                            "lastLoginAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    currentOp = OperationType.UPDATE
                    currentPath = "admin_invites/$inviteId"
                    inviteRef.update(
                        mapOf(
                            "claimedUid" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    logActivity(uid, adminName, "LOGIN", "$adminName activated invited ${invite.role} account", uid, teamUids)
                    currentOp = OperationType.GET
                    currentPath = "admins/$uid"
                    val claimed = adminRef.get().await()
                        .toObject(AdminAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    return Result.success(claimed)
                }
            }

            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, currentOp, currentPath)
            Result.failure(e)
        }
    }

    suspend fun uploadNewDocument(
        adminUid: String,
        adminName: String,
        adminUids: List<String>,
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        categoryId: String,
        categoryName: String,
        chapter: String,
        tags: List<String>,
        featured: Boolean,
        published: Boolean,
        filePayload: PickedFilePayload,
        onProgress: (Float) -> Unit
    ): Result<String> {
        val docRef = db.collection("documents").document()
        val docId = docRef.id
        return try {
            val chunks = FileStorageHelper.splitIntoBase64Chunks(filePayload.bytes)
            val totalSteps = (chunks.size + 1).coerceAtLeast(1)
            chunks.forEachIndexed { idx, b64 ->
                val chunkId = "${docId}_$idx"
                db.collection("document_chunks").document(chunkId).set(
                    mapOf(
                        "documentId" to docId,
                        "chunkIndex" to idx.toLong(),
                        "dataBase64" to b64,
                        "published" to published,
                        "uploadedByUid" to adminUid,
                        "adminUids" to adminUids.take(20),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                onProgress((idx + 1).toFloat() / totalSteps)
            }

            docRef.set(
                mapOf(
                    "title" to title.trim().take(180),
                    "description" to description.trim().take(2000),
                    "subjectId" to subjectId,
                    "subjectName" to subjectName,
                    "categoryId" to categoryId,
                    "categoryName" to categoryName,
                    "chapter" to chapter.trim().ifBlank { "General" }.take(140),
                    "fileName" to filePayload.fileName.take(255),
                    "mimeType" to filePayload.mimeType.take(150),
                    "fileExtension" to filePayload.fileExtension.take(20),
                    "fileSizeBytes" to filePayload.fileSizeBytes,
                    "chunkCount" to chunks.size.toLong(),
                    "tags" to tags.map { it.trim() }.filter { it.isNotBlank() }.take(10),
                    "featured" to featured,
                    "published" to published,
                    "version" to 1L,
                    "downloadCount" to 0L,
                    "uploadedByUid" to adminUid,
                    "uploadedByName" to adminName,
                    "lastEditedByUid" to adminUid,
                    "lastEditedByName" to adminName,
                    "adminUids" to adminUids.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("document_versions").document().set(
                mapOf(
                    "documentId" to docId,
                    "version" to 1L,
                    "title" to title.trim().take(180),
                    "fileName" to filePayload.fileName.take(255),
                    "fileSizeBytes" to filePayload.fileSizeBytes,
                    "changeNote" to "Initial upload",
                    "editedByUid" to adminUid,
                    "editedByName" to adminName,
                    "adminUids" to adminUids.take(20),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logActivity(
                adminUid = adminUid,
                adminName = adminName,
                actionType = "UPLOAD_DOC",
                summary = "Uploaded '${title.trim()}' (${filePayload.fileExtension.uppercase()}) to $subjectName",
                targetId = docId,
                adminUids = adminUids
            )
            onProgress(1f)
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDocumentWithConflictCheck(
        doc: StudyDocument,
        expectedVersion: Long,
        adminUid: String,
        adminName: String,
        adminUids: List<String>,
        newTitle: String,
        newDescription: String,
        newSubjectId: String,
        newSubjectName: String,
        newCategoryId: String,
        newCategoryName: String,
        newChapter: String,
        newTags: List<String>,
        newFeatured: Boolean,
        newPublished: Boolean,
        replacementFile: PickedFilePayload? = null,
        changeNote: String = "Updated details"
    ): Result<Unit> {
        val docRef = db.collection("documents").document(doc.id)
        return try {
            val latestSnap = docRef.get().await()
            val latestVersion = latestSnap.getLong("version") ?: 1L
            if (latestVersion != expectedVersion) {
                return Result.failure(
                    IllegalStateException("Edit conflict detected: another administrator updated this document to v$latestVersion. Refresh and retry.")
                )
            }
            val nextVersion = latestVersion + 1L
            val updates = mutableMapOf<String, Any>(
                "title" to newTitle.trim().take(180),
                "description" to newDescription.trim().take(2000),
                "subjectId" to newSubjectId,
                "subjectName" to newSubjectName,
                "categoryId" to newCategoryId,
                "categoryName" to newCategoryName,
                "chapter" to newChapter.trim().ifBlank { "General" }.take(140),
                "tags" to newTags.take(10),
                "featured" to newFeatured,
                "published" to newPublished,
                "version" to nextVersion,
                "lastEditedByUid" to adminUid,
                "lastEditedByName" to adminName,
                "adminUids" to adminUids.take(20),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            var finalFileName = doc.fileName
            var finalFileSize = doc.fileSizeBytes

            if (replacementFile != null) {
                val oldChunks = db.collection("document_chunks")
                    .whereEqualTo("documentId", doc.id).get().await()
                oldChunks.documents.forEach { it.reference.delete().await() }

                val newChunks = FileStorageHelper.splitIntoBase64Chunks(replacementFile.bytes)
                newChunks.forEachIndexed { idx, b64 ->
                    db.collection("document_chunks").document("${doc.id}_v${nextVersion}_$idx").set(
                        mapOf(
                            "documentId" to doc.id,
                            "chunkIndex" to idx.toLong(),
                            "dataBase64" to b64,
                            "published" to newPublished,
                            "uploadedByUid" to adminUid,
                            "adminUids" to adminUids.take(20),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                updates["fileName"] = replacementFile.fileName.take(255)
                updates["mimeType"] = replacementFile.mimeType.take(150)
                updates["fileExtension"] = replacementFile.fileExtension.take(20)
                updates["fileSizeBytes"] = replacementFile.fileSizeBytes
                updates["chunkCount"] = newChunks.size.toLong()
                finalFileName = replacementFile.fileName
                finalFileSize = replacementFile.fileSizeBytes
            } else if (doc.published != newPublished) {
                val chunks = db.collection("document_chunks")
                    .whereEqualTo("documentId", doc.id).get().await()
                chunks.documents.forEach {
                    it.reference.update(mapOf("published" to newPublished)).await()
                }
            }

            docRef.update(updates).await()

            db.collection("document_versions").document().set(
                mapOf(
                    "documentId" to doc.id,
                    "version" to nextVersion,
                    "title" to newTitle.trim().take(180),
                    "fileName" to finalFileName.take(255),
                    "fileSizeBytes" to finalFileSize,
                    "changeNote" to changeNote.take(300),
                    "editedByUid" to adminUid,
                    "editedByName" to adminName,
                    "adminUids" to adminUids.take(20),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logActivity(
                adminUid, adminName, "EDIT_DOC",
                "Updated '${newTitle.trim()}' to v$nextVersion ($changeNote)", doc.id, adminUids
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(doc: StudyDocument, adminUid: String, adminName: String, adminUids: List<String>): Result<Unit> {
        return try {
            val chunks = db.collection("document_chunks").whereEqualTo("documentId", doc.id).get().await()
            chunks.documents.forEach { it.reference.delete().await() }
            db.collection("documents").document(doc.id).delete().await()
            logActivity(adminUid, adminName, "DELETE_DOC", "Deleted document '${doc.title}'", doc.id, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "documents/${doc.id}")
            Result.failure(e)
        }
    }

    suspend fun bulkSetPublished(
        docs: List<StudyDocument>,
        published: Boolean,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        return try {
            docs.forEach { doc ->
                db.collection("documents").document(doc.id).update(
                    mapOf(
                        "published" to published,
                        "version" to doc.version + 1L,
                        "lastEditedByUid" to adminUid,
                        "lastEditedByName" to adminName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val chunks = db.collection("document_chunks").whereEqualTo("documentId", doc.id).get().await()
                chunks.documents.forEach { it.reference.update(mapOf("published" to published)).await() }
            }
            logActivity(
                adminUid, adminName, "BULK_ACTION",
                "${if (published) "Published" else "Unpublished"} ${docs.size} documents",
                "bulk", adminUids
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "documents")
            Result.failure(e)
        }
    }

    suspend fun bulkDeleteDocuments(
        docs: List<StudyDocument>,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        return try {
            docs.forEach { doc ->
                val chunks = db.collection("document_chunks").whereEqualTo("documentId", doc.id).get().await()
                chunks.documents.forEach { it.reference.delete().await() }
                db.collection("documents").document(doc.id).delete().await()
            }
            logActivity(adminUid, adminName, "BULK_ACTION", "Bulk deleted ${docs.size} documents", "bulk", adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "documents")
            Result.failure(e)
        }
    }

    suspend fun saveSubject(
        subject: SubjectItem,
        isNew: Boolean,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val docId = if (isNew) subject.nameEn.lowercase().replace(Regex("[^a-z0-9]"), "_") + "_" + System.currentTimeMillis() else subject.id
        val ref = db.collection("subjects").document(docId)
        return try {
            if (isNew) {
                ref.set(
                    mapOf(
                        "nameEn" to subject.nameEn.trim(),
                        "nameBn" to subject.nameBn.trim(),
                        "description" to subject.description.trim(),
                        "iconKey" to subject.iconKey,
                        "accentHex" to subject.accentHex,
                        "sortOrder" to subject.sortOrder,
                        "visible" to subject.visible,
                        "createdByUid" to adminUid,
                        "adminUids" to adminUids.take(20),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "nameEn" to subject.nameEn.trim(),
                        "nameBn" to subject.nameBn.trim(),
                        "description" to subject.description.trim(),
                        "visible" to subject.visible,
                        "sortOrder" to subject.sortOrder,
                        "adminUids" to adminUids.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            logActivity(adminUid, adminName, "SUBJECT_ACTION", "Saved subject '${subject.nameEn}'", docId, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(
        categoryId: String?,
        name: String,
        subjectId: String,
        visible: Boolean,
        sortOrder: Long,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val isNew = categoryId.isNullOrBlank()
        val ref = if (isNew) db.collection("categories").document() else db.collection("categories").document(categoryId!!)
        return try {
            if (isNew) {
                ref.set(
                    mapOf(
                        "name" to name.trim().take(100),
                        "subjectId" to subjectId,
                        "visible" to visible,
                        "sortOrder" to sortOrder,
                        "createdByUid" to adminUid,
                        "adminUids" to adminUids.take(20),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to name.trim().take(100),
                        "subjectId" to subjectId,
                        "visible" to visible,
                        "sortOrder" to sortOrder,
                        "adminUids" to adminUids.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            logActivity(adminUid, adminName, "CATEGORY_ACTION", "Saved category '${name.trim()}'", ref.id, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String, name: String, adminUid: String, adminName: String, adminUids: List<String>): Result<Unit> {
        return try {
            db.collection("categories").document(categoryId).delete().await()
            logActivity(adminUid, adminName, "CATEGORY_ACTION", "Deleted category '$name'", categoryId, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "categories/$categoryId")
            Result.failure(e)
        }
    }

    suspend fun inviteAdmin(
        email: String,
        displayName: String,
        role: AdminRole,
        permissions: List<String>,
        superAdminUid: String,
        superAdminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid Google email address."))
        }
        val inviteId = inviteDocIdForEmail(cleanEmail)
        val ref = db.collection("admin_invites").document(inviteId)
        return try {
            ref.set(
                mapOf(
                    "email" to cleanEmail,
                    "displayName" to displayName.trim().ifBlank { "Administrator" }.take(100),
                    "role" to role.name,
                    "permissions" to permissions.take(20),
                    "active" to true,
                    "createdByUid" to superAdminUid,
                    "adminUids" to adminUids.take(20),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                superAdminUid, superAdminName, "ADMIN_ACTION",
                "Invited $cleanEmail as ${role.label}", inviteId, adminUids
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateAdminAccount(
        targetAdmin: AdminAccount,
        newDisplayName: String,
        newRole: AdminRole,
        newActive: Boolean,
        newPermissions: List<String>,
        actorUid: String,
        actorName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val ref = db.collection("admins").document(targetAdmin.uid)
        return try {
            ref.update(
                mapOf(
                    "displayName" to newDisplayName.trim().take(100),
                    "role" to newRole.name,
                    "active" to newActive,
                    "permissions" to newPermissions.take(20),
                    "adminUids" to adminUids.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                actorUid, actorName, "ADMIN_ACTION",
                "Updated admin ${targetAdmin.email}: role=${newRole.name}, active=$newActive",
                targetAdmin.uid, adminUids
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminAccount(
        targetAdmin: AdminAccount,
        actorUid: String,
        actorName: String,
        adminUids: List<String>
    ): Result<Unit> {
        return try {
            db.collection("admins").document(targetAdmin.uid).delete().await()
            logActivity(actorUid, actorName, "ADMIN_ACTION", "Removed admin ${targetAdmin.email}", targetAdmin.uid, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "admins/${targetAdmin.uid}")
            Result.failure(e)
        }
    }

    suspend fun removeAdminInvite(inviteId: String, email: String, actorUid: String, actorName: String, adminUids: List<String>): Result<Unit> {
        return try {
            db.collection("admin_invites").document(inviteId).delete().await()
            logActivity(actorUid, actorName, "ADMIN_ACTION", "Revoked invite for $email", inviteId, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "admin_invites/$inviteId")
            Result.failure(e)
        }
    }

    suspend fun setStudentActiveStatus(
        studentUid: String,
        studentName: String,
        active: Boolean,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        return try {
            val studentRef = db.collection("students").document(studentUid)
            if (studentRef.get().await().exists()) {
                studentRef.update(
                    mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            db.collection("student_directory").document(studentUid).update(
                mapOf(
                    "active" to active,
                    "adminUids" to adminUids.take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                adminUid, adminName, "STUDENT_ACTION",
                "${if (active) "Reactivated" else "Disabled"} student $studentName",
                studentUid, adminUids
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "student_directory/$studentUid")
            Result.failure(e)
        }
    }

    suspend fun removeStudentRecord(
        studentUid: String,
        studentName: String,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        return try {
            db.collection("student_directory").document(studentUid).delete().await()
            val studentRef = db.collection("students").document(studentUid)
            if (studentRef.get().await().exists()) {
                studentRef.delete().await()
            }
            val lbRef = db.collection("leaderboard").document(studentUid)
            if (lbRef.get().await().exists()) {
                lbRef.delete().await()
            }
            logActivity(adminUid, adminName, "STUDENT_ACTION", "Removed student record $studentName", studentUid, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "student_directory/$studentUid")
            Result.failure(e)
        }
    }

    suspend fun saveQuizQuestion(
        question: QuizQuestion,
        isNew: Boolean,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val ref = if (isNew) db.collection("quiz_questions").document() else db.collection("quiz_questions").document(question.id)
        return try {
            if (isNew) {
                ref.set(
                    mapOf(
                        "category" to question.category,
                        "difficulty" to question.difficulty,
                        "questionText" to question.questionText.trim().take(600),
                        "options" to question.options.take(4),
                        "correctIndex" to question.correctIndex.coerceIn(0L, 3L),
                        "explanation" to question.explanation.trim().take(1200),
                        "sourceNote" to question.sourceNote.trim().take(300),
                        "aiGenerated" to question.aiGenerated,
                        "approved" to question.approved,
                        "createdByUid" to adminUid,
                        "adminUids" to adminUids.take(20),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "category" to question.category,
                        "difficulty" to question.difficulty,
                        "questionText" to question.questionText.trim().take(600),
                        "options" to question.options.take(4),
                        "correctIndex" to question.correctIndex.coerceIn(0L, 3L),
                        "explanation" to question.explanation.trim().take(1200),
                        "sourceNote" to question.sourceNote.trim().take(300),
                        "approved" to question.approved,
                        "adminUids" to adminUids.take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            logActivity(adminUid, adminName, "QUIZ_ACTION", "Saved quiz question in ${question.category}", ref.id, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteQuizQuestion(questionId: String, adminUid: String, adminName: String, adminUids: List<String>): Result<Unit> {
        return try {
            db.collection("quiz_questions").document(questionId).delete().await()
            logActivity(adminUid, adminName, "QUIZ_ACTION", "Deleted quiz question", questionId, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "quiz_questions/$questionId")
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(
        announcement: Announcement,
        isNew: Boolean,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val ref = if (isNew) db.collection("announcements").document() else db.collection("announcements").document(announcement.id)
        return try {
            val map = mutableMapOf<String, Any?>(
                "title" to announcement.title.trim().take(180),
                "body" to announcement.body.trim().take(3000),
                "pinned" to announcement.pinned,
                "published" to announcement.published,
                "attachmentDocId" to announcement.attachmentDocId?.takeIf { it.isNotBlank() },
                "attachmentFileName" to announcement.attachmentFileName?.takeIf { it.isNotBlank() },
                "attachmentMimeType" to announcement.attachmentMimeType?.takeIf { it.isNotBlank() },
                "publishDateLabel" to announcement.publishDateLabel.ifBlank { "Today" }.take(40),
                "adminUids" to adminUids.take(20),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNew) {
                map["createdByUid"] = adminUid
                map["createdByName"] = adminName
                map["createdAt"] = FieldValue.serverTimestamp()
                @Suppress("UNCHECKED_CAST")
                ref.set(map.filterValues { it != null } as Map<String, Any>).await()
            } else {
                @Suppress("UNCHECKED_CAST")
                ref.update(map.filterValues { it != null } as Map<String, Any>).await()
            }
            logActivity(adminUid, adminName, "ANNOUNCEMENT_ACTION", "Saved announcement '${announcement.title.trim()}'", ref.id, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String, title: String, adminUid: String, adminName: String, adminUids: List<String>): Result<Unit> {
        return try {
            db.collection("announcements").document(announcementId).delete().await()
            logActivity(adminUid, adminName, "ANNOUNCEMENT_ACTION", "Deleted announcement '$title'", announcementId, adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "announcements/$announcementId")
            Result.failure(e)
        }
    }

    suspend fun updateAppSettings(
        settings: AppSettings,
        adminUid: String,
        adminName: String,
        adminUids: List<String>
    ): Result<Unit> {
        val ref = db.collection("app_settings").document("global")
        return try {
            val snap = ref.get().await()
            val payload = mapOf(
                "appName" to settings.appName.trim().ifBlank { "দরবার" }.take(60),
                "tagline" to settings.tagline.trim().ifBlank { "Your Digital Learning & Knowledge Hub" }.take(140),
                "defaultThemeMode" to settings.defaultThemeMode,
                "maxUploadSizeMb" to settings.maxUploadSizeMb.coerceIn(1L, 20L),
                "allowStudentQuiz" to settings.allowStudentQuiz,
                "allowStudentDownloads" to settings.allowStudentDownloads,
                "dailyChallengeEnabled" to settings.dailyChallengeEnabled,
                "notificationsEnabled" to settings.notificationsEnabled,
                "maintenanceMode" to settings.maintenanceMode,
                "updatedByUid" to adminUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                ref.set(payload).await()
            } else {
                ref.update(payload).await()
            }
            logActivity(adminUid, adminName, "SETTINGS_UPDATE", "Updated application settings", "global", adminUids)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun logActivity(
        adminUid: String,
        adminName: String,
        actionType: String,
        summary: String,
        targetId: String,
        adminUids: List<String>
    ) {
        try {
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to adminUid,
                    "adminName" to adminName.ifBlank { "Admin" }.take(100),
                    "actionType" to actionType.take(40),
                    "summary" to summary.take(400),
                    "targetId" to targetId.take(128),
                    "adminUids" to adminUids.take(20),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "activity_logs")
        }
    }
}
