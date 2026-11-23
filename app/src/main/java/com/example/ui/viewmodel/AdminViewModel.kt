package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DocumentStorageHelper
import com.example.core.PickedFilePayload
import com.example.core.unwrapFirestoreError
import com.example.data.model.ActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.DocumentVersion
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.example.data.repository.AdminControlRepository
import com.example.data.repository.DocumentRepository
import com.example.data.repository.GeneratedQuizDraft
import com.example.data.repository.QuizRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UploadQueueItem(
    val uri: Uri,
    val payload: PickedFilePayload?,
    val title: String,
    val progress: Float = 0f,
    val status: String = "Ready",
    val error: String? = null
)

data class AdminAnalyticsSummary(
    val totalStudents: Int = 0,
    val totalAdmins: Int = 0,
    val activeAdmins: Int = 0,
    val totalFiles: Int = 0,
    val totalPdfs: Int = 0,
    val totalImages: Int = 0,
    val totalDownloads: Long = 0L,
    val totalQuizAttempts: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val filesBySubject: Map<String, Int> = emptyMap(),
    val storageBySubject: Map<String, Long> = emptyMap(),
    val topDownloadedDocs: List<StudyDocument> = emptyList()
)

class AdminViewModel(
    private val documentRepository: DocumentRepository,
    private val quizRepository: QuizRepository,
    private val adminControlRepository: AdminControlRepository
) : ViewModel() {

    val allDocuments: StateFlow<List<StudyDocument>> =
        documentRepository.observeAllDocumentsAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allSubjects: StateFlow<List<Subject>> =
        documentRepository.observeAllSubjectsAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(DarbarCurriculumDefaults.DEFAULT_SUBJECTS) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DarbarCurriculumDefaults.DEFAULT_SUBJECTS)

    val allCategories: StateFlow<List<Category>> =
        documentRepository.observeAllCategoriesAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(DarbarCurriculumDefaults.DEFAULT_CATEGORIES) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DarbarCurriculumDefaults.DEFAULT_CATEGORIES)

    val allStudents: StateFlow<List<StudentProfile>> =
        adminControlRepository.observeAllStudentsAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allQuizQuestions: StateFlow<List<QuizQuestion>> =
        quizRepository.observeAllQuestionsAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAnnouncements: StateFlow<List<Announcement>> =
        adminControlRepository.observeAllAnnouncementsAdmin()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAdmins: StateFlow<List<AdminUser>> =
        adminControlRepository.observeAllAdmins()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminInvites: StateFlow<List<AdminInvite>> =
        adminControlRepository.observeAdminInvites()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> =
        adminControlRepository.observeActivityLogs()
            .catch { err -> unwrapFirestoreError(err); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val analytics: StateFlow<AdminAnalyticsSummary> = combine(
        allDocuments,
        allStudents,
        allAdmins
    ) { docs, students, admins ->
        val pdfCount = docs.count { it.fileType.equals("PDF", ignoreCase = true) }
        val imgCount = docs.count { it.fileType.uppercase() in setOf("JPG", "JPEG", "PNG", "WEBP") }
        val totalDownloads = docs.sumOf { it.downloadCount }
        val totalQuizAttempts = students.sumOf { it.quizzesTaken }
        val totalBytes = docs.sumOf { it.fileSizeBytes }
        val bySubject = docs.groupingBy { it.subjectName.ifBlank { it.subjectId } }.eachCount()
        val bytesBySubject = docs.groupBy { it.subjectName.ifBlank { it.subjectId } }
            .mapValues { (_, list) -> list.sumOf { it.fileSizeBytes } }
        AdminAnalyticsSummary(
            totalStudents = students.size,
            totalAdmins = admins.size,
            activeAdmins = admins.count { it.active },
            totalFiles = docs.size,
            totalPdfs = pdfCount,
            totalImages = imgCount,
            totalDownloads = totalDownloads,
            totalQuizAttempts = totalQuizAttempts,
            totalStorageBytes = totalBytes,
            filesBySubject = bySubject,
            storageBySubject = bytesBySubject,
            topDownloadedDocs = docs.sortedByDescending { it.downloadCount }.take(5)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminAnalyticsSummary())

    private val _uploadQueue = MutableStateFlow<List<UploadQueueItem>>(emptyList())
    val uploadQueue: StateFlow<List<UploadQueueItem>> = _uploadQueue.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _aiDrafts = MutableStateFlow<List<GeneratedQuizDraft>>(emptyList())
    val aiDrafts: StateFlow<List<GeneratedQuizDraft>> = _aiDrafts.asStateFlow()

    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    private val _versionHistory = MutableStateFlow<List<DocumentVersion>>(emptyList())
    val versionHistory: StateFlow<List<DocumentVersion>> = _versionHistory.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    fun stagePickedFiles(context: Context, uris: List<Uri>, maxUploadMb: Long) {
        val maxBytes = maxUploadMb.coerceIn(1L, 10L) * 1024L * 1024L
        viewModelScope.launch {
            val items = mutableListOf<UploadQueueItem>()
            for (uri in uris) {
                val res = DocumentStorageHelper.readPickedFile(context, uri, maxBytes)
                res.onSuccess { payload ->
                    items += UploadQueueItem(
                        uri = uri,
                        payload = payload,
                        title = payload.fileName.substringBeforeLast('.'),
                        status = "Ready (${DocumentStorageHelper.formatBytes(payload.bytes.size.toLong())})"
                    )
                }.onFailure { err ->
                    items += UploadQueueItem(
                        uri = uri,
                        payload = null,
                        title = uri.lastPathSegment ?: "Invalid File",
                        status = "Validation failed",
                        error = err.localizedMessage
                    )
                }
            }
            _uploadQueue.value = _uploadQueue.value + items
        }
    }

    fun removeQueuedFile(uri: Uri) {
        _uploadQueue.value = _uploadQueue.value.filterNot { it.uri == uri }
    }

    fun clearUploadQueue() {
        _uploadQueue.value = emptyList()
    }

    fun uploadAllQueued(
        admin: AdminUser,
        customTitleForSingle: String,
        description: String,
        subject: Subject,
        category: Category,
        chapter: String,
        tagsCsv: String,
        published: Boolean,
        featured: Boolean
    ) {
        val validItems = _uploadQueue.value.filter { it.payload != null }
        if (validItems.isEmpty()) {
            _adminMessage.value = "Please select valid files to upload."
            return
        }
        val tags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }

        viewModelScope.launch {
            _isUploading.value = true
            var successCount = 0
            try {
                for (item in validItems) {
                    val payload = item.payload ?: continue
                    val title = if (validItems.size == 1 && customTitleForSingle.isNotBlank()) customTitleForSingle else item.title
                    updateQueueItem(item.uri) { it.copy(status = "Uploading...", progress = 0.05f) }
                    val res = documentRepository.uploadDocument(
                        admin = admin,
                        payload = payload,
                        title = title,
                        description = description,
                        subject = subject,
                        category = category,
                        chapter = chapter,
                        tags = tags,
                        published = published,
                        featured = featured,
                        onProgress = { prog ->
                            updateQueueItem(item.uri) { it.copy(progress = prog) }
                        }
                    )
                    res.onSuccess {
                        successCount++
                        updateQueueItem(item.uri) { it.copy(progress = 1f, status = "Uploaded", error = null) }
                    }.onFailure { err ->
                        updateQueueItem(item.uri) { it.copy(status = "Failed", error = err.localizedMessage) }
                    }
                }
                _adminMessage.value = "Uploaded $successCount of ${validItems.size} file(s)."
                if (successCount == validItems.size) {
                    _uploadQueue.value = emptyList()
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isUploading.value = false
            }
        }
    }

    private fun updateQueueItem(uri: Uri, transform: (UploadQueueItem) -> UploadQueueItem) {
        _uploadQueue.value = _uploadQueue.value.map { if (it.uri == uri) transform(it) else it }
    }

    fun editDocument(
        admin: AdminUser,
        doc: StudyDocument,
        title: String,
        description: String,
        subject: Subject,
        category: Category,
        chapter: String,
        tagsCsv: String,
        published: Boolean,
        featured: Boolean,
        replacementPayload: PickedFilePayload?,
        changeNote: String
    ) {
        viewModelScope.launch {
            val tags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
            val res = documentRepository.updateDocumentWithConflictCheck(
                admin = admin,
                docId = doc.id,
                expectedVersion = doc.version,
                title = title,
                description = description,
                subject = subject,
                category = category,
                chapter = chapter,
                tags = tags,
                published = published,
                featured = featured,
                replacementFile = replacementPayload,
                changeNote = changeNote
            )
            res.onSuccess { ver ->
                _adminMessage.value = "Updated '${title}' to v$ver"
            }.onFailure { err ->
                _adminMessage.value = err.localizedMessage ?: "Failed to update document"
            }
        }
    }

    fun loadVersionHistory(docId: String) {
        viewModelScope.launch {
            documentRepository.getVersionHistory(docId)
                .onSuccess { _versionHistory.value = it }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun bulkPublish(admin: AdminUser, docIds: List<String>, publish: Boolean) {
        viewModelScope.launch {
            documentRepository.bulkSetPublished(admin, docIds, publish)
                .onSuccess { _adminMessage.value = "Updated ${docIds.size} document(s)." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun bulkDeleteDocuments(admin: AdminUser, docIds: List<String>) {
        viewModelScope.launch {
            documentRepository.deleteDocuments(admin, docIds)
                .onSuccess { _adminMessage.value = "Deleted ${docIds.size} document(s)." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun saveSubject(admin: AdminUser, subject: Subject) {
        viewModelScope.launch {
            documentRepository.saveSubject(admin, subject)
                .onSuccess { _adminMessage.value = "Saved subject ${subject.titleEn}" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun saveCategory(admin: AdminUser, category: Category, isNew: Boolean) {
        viewModelScope.launch {
            documentRepository.saveCategory(admin, category, isNew)
                .onSuccess { _adminMessage.value = "Saved category '${category.name}'" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun deleteCategory(admin: AdminUser, categoryId: String) {
        viewModelScope.launch {
            documentRepository.deleteCategory(admin, categoryId)
                .onSuccess { _adminMessage.value = "Category deleted." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun generateAiQuestionsForReview(category: String, difficulty: QuizDifficulty, count: Int) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            try {
                val avoid = allQuizQuestions.value.map { it.questionText }
                quizRepository.generateQuestionsWithAi(category, difficulty, count, avoid)
                    .onSuccess { drafts ->
                        _aiDrafts.value = drafts
                        _adminMessage.value = "Generated ${drafts.size} AI questions for review."
                    }
                    .onFailure { err ->
                        Log.e("AdminVM", "AI generation failed", err)
                        _adminMessage.value = err.localizedMessage ?: "AI generation failed"
                    }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    fun approveDraftQuestion(admin: AdminUser, draft: GeneratedQuizDraft) {
        viewModelScope.launch {
            val q = QuizQuestion(
                category = draft.category,
                difficulty = draft.difficulty.name,
                questionText = draft.questionText,
                options = draft.options,
                correctIndex = draft.correctIndex.toLong(),
                explanation = draft.explanation,
                source = draft.source,
                factDate = draft.factDate,
                aiGenerated = true,
                verifiedByAdmin = true,
                approved = true
            )
            quizRepository.saveAdminQuizQuestion(admin, q, isNew = true)
                .onSuccess {
                    _aiDrafts.value = _aiDrafts.value.filterNot { it.questionText == draft.questionText }
                    _adminMessage.value = "Verified & approved AI question to quiz bank."
                }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun discardDraft(draft: GeneratedQuizDraft) {
        _aiDrafts.value = _aiDrafts.value.filterNot { it.questionText == draft.questionText }
    }

    fun saveManualQuestion(admin: AdminUser, question: QuizQuestion, isNew: Boolean) {
        viewModelScope.launch {
            quizRepository.saveAdminQuizQuestion(admin, question, isNew)
                .onSuccess { _adminMessage.value = "Quiz question saved." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun deleteQuizQuestion(admin: AdminUser, questionId: String) {
        viewModelScope.launch {
            quizRepository.deleteQuizQuestion(admin, questionId)
                .onSuccess { _adminMessage.value = "Quiz question deleted." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun saveAnnouncement(admin: AdminUser, item: Announcement, isNew: Boolean) {
        viewModelScope.launch {
            adminControlRepository.saveAnnouncement(admin, item, isNew)
                .onSuccess { _adminMessage.value = "Announcement saved." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun deleteAnnouncement(admin: AdminUser, id: String) {
        viewModelScope.launch {
            adminControlRepository.deleteAnnouncement(admin, id)
                .onSuccess { _adminMessage.value = "Announcement deleted." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun toggleStudentDisabled(admin: AdminUser, student: StudentProfile) {
        viewModelScope.launch {
            adminControlRepository.setStudentDisabled(admin, student, !student.disabled)
                .onSuccess { _adminMessage.value = "Updated student access." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun deleteStudent(admin: AdminUser, student: StudentProfile) {
        viewModelScope.launch {
            adminControlRepository.deleteStudent(admin, student)
                .onSuccess { _adminMessage.value = "Removed student record." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun inviteAdmin(superAdmin: AdminUser, email: String, name: String, role: AdminRole, perms: List<String>) {
        viewModelScope.launch {
            adminControlRepository.inviteAdmin(superAdmin, email, name, role, perms)
                .onSuccess { _adminMessage.value = "Admin invitation sent for $email" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun updateAdmin(superAdmin: AdminUser, target: AdminUser, name: String, role: AdminRole, active: Boolean, perms: List<String>) {
        viewModelScope.launch {
            adminControlRepository.updateAdminAccount(superAdmin, target, name, role, active, perms)
                .onSuccess { _adminMessage.value = "Updated admin ${target.displayName}" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun removeAdmin(superAdmin: AdminUser, target: AdminUser) {
        viewModelScope.launch {
            adminControlRepository.removeAdminAccount(superAdmin, target)
                .onSuccess { _adminMessage.value = "Removed admin ${target.displayName}" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun revokeInvite(superAdmin: AdminUser, email: String) {
        viewModelScope.launch {
            adminControlRepository.revokeAdminInvite(superAdmin, email)
                .onSuccess { _adminMessage.value = "Revoked invite for $email" }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun saveSettings(admin: AdminUser, settings: AppSettings) {
        viewModelScope.launch {
            adminControlRepository.updateAppSettings(admin, settings)
                .onSuccess { _adminMessage.value = "Application settings updated." }
                .onFailure { _adminMessage.value = it.localizedMessage }
        }
    }

    fun exportStudentsCsv(context: Context) {
        viewModelScope.launch {
            val sb = StringBuilder("StudentCode,FullName,Status,Downloads,QuizzesTaken,AccuracyPercent\n")
            allStudents.value.forEach { s ->
                val status = if (s.disabled) "Disabled" else "Active"
                sb.append("\"${s.studentCode}\",\"${s.fullName.replace("\"", "'")}\",$status,${s.downloadsCount},${s.quizzesTaken},${s.averageAccuracyPercent}\n")
            }
            val ok = DocumentStorageHelper.exportCsvAndShare(context, "darbar_students_report.csv", sb.toString())
            _adminMessage.value = if (ok) "Exported students CSV" else "Unable to share CSV"
        }
    }

    fun exportAnalyticsCsv(context: Context) {
        viewModelScope.launch {
            val a = analytics.value
            val sb = StringBuilder("Metric,Value\n")
            sb.append("Total Students,${a.totalStudents}\n")
            sb.append("Total Admins,${a.totalAdmins}\n")
            sb.append("Active Admins,${a.activeAdmins}\n")
            sb.append("Total Files,${a.totalFiles}\n")
            sb.append("Total PDFs,${a.totalPdfs}\n")
            sb.append("Total Images,${a.totalImages}\n")
            sb.append("Total Downloads,${a.totalDownloads}\n")
            sb.append("Total Quiz Attempts,${a.totalQuizAttempts}\n")
            sb.append("Storage Used,${DocumentStorageHelper.formatBytes(a.totalStorageBytes)}\n")
            a.filesBySubject.forEach { (subj, cnt) ->
                sb.append("\"Subject: $subj\",$cnt\n")
            }
            val ok = DocumentStorageHelper.exportCsvAndShare(context, "darbar_analytics_report.csv", sb.toString())
            _adminMessage.value = if (ok) "Exported analytics CSV" else "Unable to share CSV"
        }
    }

    fun clearMessage() {
        _adminMessage.value = null
    }
}
