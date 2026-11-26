package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.FileStorageHelper
import com.example.core.LocalSessionPrefs
import com.example.core.PickedFilePayload
import com.example.core.SessionPreferences
import com.example.data.model.AdminAccount
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.DocumentVersion
import com.example.data.model.DownloadRecord
import com.example.data.model.FavoriteRecord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentDirectoryEntry
import com.example.data.model.StudentNotification
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiQuizRepository
import com.example.data.repository.ContentRepository
import com.example.data.repository.GeneratedQuizQuestion
import com.example.data.repository.StudentRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DocumentPreviewState(
    val document: StudyDocument? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val bytes: ByteArray? = null,
    val pdfPages: List<Bitmap> = emptyList(),
    val imageBitmap: Bitmap? = null,
    val textContent: String? = null,
    val localDownloadPath: String? = null
)

data class QuizSessionState(
    val active: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val category: String = DefaultCatalog.gkCategories.first(),
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val mode: String = "Category Quiz",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val answerRevealed: Boolean = false,
    val correctCount: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val secondsRemaining: Int = 20,
    val totalElapsedSeconds: Int = 0,
    val completed: Boolean = false,
    val newlyUnlockedAchievements: List<String> = emptyList()
)

data class UploadQueueItem(
    val uri: Uri,
    val payload: PickedFilePayload,
    val title: String
)

class DorbarViewModel(
    private val currentUserId: String,
    private val userEmail: String?,
    private val emailVerified: Boolean,
    private val userDisplayName: String?,
    private val contentRepo: ContentRepository,
    private val studentRepo: StudentRepository,
    private val adminRepo: AdminRepository,
    private val aiQuizRepo: AiQuizRepository,
    private val sessionPrefs: SessionPreferences
) : ViewModel() {

    private val isBootstrappedSuperAdmin: Boolean =
        emailVerified && userEmail?.trim()?.lowercase() == DefaultCatalog.SUPER_ADMIN_EMAIL

    val localPrefs: StateFlow<LocalSessionPrefs> = sessionPrefs.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalSessionPrefs())

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error in $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val appSettings: StateFlow<AppSettings> = contentRepo.observeAppSettings()
        .catch { emit(AppSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val adminAccount: StateFlow<AdminAccount?> = adminRepo.observeMyAdminAccount(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isAdminAuthorized: StateFlow<Boolean> = adminAccount
        .map { acc -> isBootstrappedSuperAdmin || (acc != null && acc.active) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), isBootstrappedSuperAdmin)

    val isSuperAdmin: StateFlow<Boolean> = adminAccount
        .map { acc ->
            isBootstrappedSuperAdmin || (acc != null && acc.active && acc.role == AdminRole.SUPER_ADMIN.name)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), isBootstrappedSuperAdmin)

    val studentProfile: StateFlow<UiState<StudentProfile?>> =
        studentRepo.observeStudentProfile(currentUserId).toUiState("studentProfile")

    val favorites: StateFlow<List<FavoriteRecord>> = studentRepo.observeFavorites(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloads: StateFlow<List<DownloadRecord>> = studentRepo.observeDownloads(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quizAttempts: StateFlow<List<QuizAttempt>> = studentRepo.observeQuizAttempts(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<StudentNotification>> = studentRepo.observeNotifications(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = contentRepo.observeLeaderboard()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Dynamic catalog & admin streams switched cleanly by authorized admin state
    val subjectsState: StateFlow<UiState<List<SubjectItem>>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            contentRepo.observeSubjects(isAdmin, isBootstrappedSuperAdmin, currentUserId)
        }.toUiState("subjects")

    val categoriesState: StateFlow<UiState<List<CategoryItem>>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            contentRepo.observeCategories(isAdmin, isBootstrappedSuperAdmin, currentUserId)
        }.toUiState("categories")

    val documentsState: StateFlow<UiState<List<StudyDocument>>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            contentRepo.observeDocuments(isAdmin, isBootstrappedSuperAdmin, currentUserId)
        }.toUiState("documents")

    val announcementsState: StateFlow<UiState<List<Announcement>>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            contentRepo.observeAnnouncements(isAdmin, isBootstrappedSuperAdmin, currentUserId)
        }.toUiState("announcements")

    val quizQuestionsState: StateFlow<UiState<List<QuizQuestion>>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            contentRepo.observeQuizQuestions(isAdmin, isBootstrappedSuperAdmin, currentUserId)
        }.toUiState("quizQuestions")

    val allAdmins: StateFlow<List<AdminAccount>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            if (!isAdmin) flowOf(emptyList())
            else adminRepo.observeAllAdmins(currentUserId, isBootstrappedSuperAdmin).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminInvites: StateFlow<List<AdminInvite>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            if (!isAdmin) flowOf(emptyList())
            else adminRepo.observeAdminInvites(currentUserId, isBootstrappedSuperAdmin).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentDirectory: StateFlow<List<StudentDirectoryEntry>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            if (!isAdmin) flowOf(emptyList())
            else adminRepo.observeStudentDirectory(currentUserId, isBootstrappedSuperAdmin).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activityLogs: StateFlow<List<AdminActivityLog>> = isAdminAuthorized
        .flatMapLatest { isAdmin ->
            if (!isAdmin) flowOf(emptyList())
            else adminRepo.observeActivityLogs(currentUserId, isBootstrappedSuperAdmin).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Interactive Transient States
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _previewState = MutableStateFlow(DocumentPreviewState())
    val previewState: StateFlow<DocumentPreviewState> = _previewState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    private val _aiDraftQuestions = MutableStateFlow<List<GeneratedQuizQuestion>>(emptyList())
    val aiDraftQuestions: StateFlow<List<GeneratedQuizQuestion>> = _aiDraftQuestions.asStateFlow()

    private val _aiGeneratingForAdmin = MutableStateFlow(false)
    val aiGeneratingForAdmin: StateFlow<Boolean> = _aiGeneratingForAdmin.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _docVersions = MutableStateFlow<List<DocumentVersion>>(emptyList())
    val docVersions: StateFlow<List<DocumentVersion>> = _docVersions.asStateFlow()

    private val seenQuestionTexts = mutableSetOf<String>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val accResult = adminRepo.ensureAdminSessionIfEligible(
                uid = currentUserId,
                email = userEmail,
                emailVerified = emailVerified,
                displayName = userDisplayName
            )
            val acc = accResult.getOrNull()
            if (isBootstrappedSuperAdmin || (acc != null && acc.active)) {
                val adminName = acc?.displayName ?: userDisplayName ?: "Super Admin"
                val uids = ((acc?.adminUids ?: emptyList()) + currentUserId).distinct()
                contentRepo.seedInitialCatalogIfAdmin(currentUserId, adminName, uids)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { sessionPrefs.setThemeMode(mode) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { sessionPrefs.setReducedMotion(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { sessionPrefs.setNotificationsEnabled(enabled) }
    }

    fun setWorkspaceMode(mode: String) {
        viewModelScope.launch { sessionPrefs.setWorkspaceMode(mode) }
    }

    fun activeTeamAdminUids(): List<String> {
        val fromAdmins = allAdmins.value.filter { it.active }.map { it.uid }
        return (fromAdmins + currentUserId).distinct().take(20)
    }

    fun currentAdminName(): String =
        adminAccount.value?.displayName?.ifBlank { userDisplayName ?: "Admin" }
            ?: userDisplayName
            ?: "Super Admin"

    fun hasPermission(permission: String): Boolean {
        if (isSuperAdmin.value) return true
        val acc = adminAccount.value ?: return false
        return acc.active && (acc.permissions.contains("ALL") || acc.permissions.contains(permission))
    }

    // --- STUDENT PROFILE ---
    fun registerOrUpdateStudentName(fullName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = studentRepo.registerOrUpdateStudentName(fullName, activeTeamAdminUids())
            res.onSuccess { profile ->
                sessionPrefs.cacheStudentIdentity(profile.fullName, profile.studentCode)
                _statusMessage.value = "Welcome, ${profile.fullName} (${profile.studentCode})"
                onComplete(true)
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to save student name."
                onComplete(false)
            }
        }
    }

    fun toggleFavorite(doc: StudyDocument, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            studentRepo.toggleFavorite(doc, currentlyFavorite).onFailure {
                _statusMessage.value = it.message ?: "Could not update favourite."
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { studentRepo.markNotificationRead(notifId) }
    }

    // --- DOCUMENT VIEWING & DOWNLOADING ---
    fun openDocumentPreview(context: Context, doc: StudyDocument, isAdminView: Boolean) {
        viewModelScope.launch {
            _previewState.value = DocumentPreviewState(document = doc, loading = true)
            val bytesResult = contentRepo.fetchDocumentBytes(doc.id, isAdminView)
            bytesResult.onFailure { err ->
                _previewState.value = DocumentPreviewState(
                    document = doc,
                    loading = false,
                    error = err.message ?: "Unable to load document content."
                )
            }.onSuccess { bytes ->
                val ext = doc.fileExtension.lowercase()
                var pdfPages: List<Bitmap> = emptyList()
                var imageBmp: Bitmap? = null
                var textBody: String? = null

                when (ext) {
                    "pdf" -> {
                        val previewFile = FileStorageHelper.saveToPreviewCache(context, doc.id, ext, bytes)
                        pdfPages = FileStorageHelper.renderPdfPages(previewFile)
                    }
                    "jpg", "jpeg", "png", "webp" -> {
                        imageBmp = FileStorageHelper.decodeImageBitmap(bytes)
                    }
                    "txt", "csv" -> {
                        textBody = bytes.toString(Charsets.UTF_8).take(40_000)
                    }
                }
                _previewState.value = DocumentPreviewState(
                    document = doc,
                    loading = false,
                    bytes = bytes,
                    pdfPages = pdfPages,
                    imageBitmap = imageBmp,
                    textContent = textBody
                )
            }
        }
    }

    fun closeDocumentPreview() {
        _previewState.value = DocumentPreviewState()
    }

    fun downloadDocument(context: Context, doc: StudyDocument, openAfter: Boolean = false) {
        viewModelScope.launch {
            if (!appSettings.value.allowStudentDownloads && !isAdminAuthorized.value) {
                _statusMessage.value = "Downloads are temporarily disabled by the administrator."
                return@launch
            }
            val existingBytes = _previewState.value.takeIf { it.document?.id == doc.id }?.bytes
            val bytes = existingBytes ?: contentRepo.fetchDocumentBytes(doc.id, isAdminAuthorized.value)
                .getOrElse { err ->
                    _statusMessage.value = err.message ?: "Download failed."
                    return@launch
                }
            val savedFile = FileStorageHelper.saveToDownloadsDir(context, doc.id, doc.fileName, bytes)
            studentRepo.recordDownload(doc, savedFile.absolutePath)
            _previewState.value = _previewState.value.copy(localDownloadPath = savedFile.absolutePath)
            _statusMessage.value = "Downloaded ${doc.fileName} (${FileStorageHelper.formatFileSize(bytes.size.toLong())})"
            if (openAfter) {
                FileStorageHelper.openLocalFileExternally(context, savedFile, doc.mimeType).onFailure {
                    _statusMessage.value = "Saved to app downloads. No external viewer app found for .${doc.fileExtension}."
                }
            }
        }
    }

    // --- AI GK QUIZ ENGINE ---
    fun startQuiz(
        category: String,
        difficulty: QuizDifficulty,
        questionCount: Int,
        mode: String,
        bilingualHint: Boolean
    ) {
        timerJob?.cancel()
        viewModelScope.launch {
            _quizState.value = QuizSessionState(
                active = true,
                loading = true,
                category = category,
                difficulty = difficulty,
                mode = mode
            )
            val bankQuestions = (quizQuestionsState.value as? UiState.Success)?.data
                ?.filter { it.approved }
                ?.filter { mode != "Category Quiz" || it.category == category }
                ?.filter { it.questionText !in seenQuestionTexts }
                ?.shuffled()
                .orEmpty()

            val selectedFromBank = bankQuestions.take(questionCount)
            val neededFromAi = (questionCount - selectedFromBank.size).coerceAtLeast(0)

            val combined = selectedFromBank.toMutableList()
            if (neededFromAi > 0) {
                val aiRes = aiQuizRepo.generateGkQuestions(
                    category = if (mode == "Category Quiz") category else DefaultCatalog.gkCategories.random(),
                    difficulty = difficulty,
                    count = neededFromAi,
                    avoidQuestions = seenQuestionTexts.toList(),
                    bilingualBengaliHint = bilingualHint
                )
                aiRes.onFailure { err ->
                    if (combined.isEmpty()) {
                        _quizState.value = _quizState.value.copy(
                            loading = false,
                            error = err.message ?: "Unable to generate AI GK questions."
                        )
                        return@launch
                    }
                }.onSuccess { generated ->
                    generated.forEach { g ->
                        combined.add(
                            aiQuizRepo.toQuizQuestionModel(
                                generated = g,
                                category = category,
                                difficulty = difficulty,
                                creatorUid = currentUserId,
                                adminUids = activeTeamAdminUids(),
                                approved = false
                            )
                        )
                    }
                }
            }

            if (combined.isEmpty()) {
                _quizState.value = _quizState.value.copy(
                    loading = false,
                    error = "No questions available for this configuration."
                )
                return@launch
            }

            combined.forEach { seenQuestionTexts.add(it.questionText) }
            _quizState.value = QuizSessionState(
                active = true,
                loading = false,
                category = category,
                difficulty = difficulty,
                mode = mode,
                questions = combined,
                currentIndex = 0,
                secondsRemaining = difficulty.secondsPerQuestion
            )
            startQuestionTimer()
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _quizState.value
                if (!state.active || state.completed || state.answerRevealed || state.loading) {
                    continue
                }
                val nextSec = state.secondsRemaining - 1
                val nextElapsed = state.totalElapsedSeconds + 1
                if (nextSec <= 0) {
                    _quizState.value = state.copy(
                        secondsRemaining = 0,
                        totalElapsedSeconds = nextElapsed,
                        answerRevealed = true,
                        currentStreak = 0
                    )
                } else {
                    _quizState.value = state.copy(
                        secondsRemaining = nextSec,
                        totalElapsedSeconds = nextElapsed
                    )
                }
            }
        }
    }

    fun submitQuizAnswer(optionIndex: Int) {
        val state = _quizState.value
        if (state.answerRevealed || state.completed) return
        val q = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = optionIndex.toLong() == q.correctIndex
        val nextStreak = if (isCorrect) state.currentStreak + 1 else 0
        _quizState.value = state.copy(
            selectedOptionIndex = optionIndex,
            answerRevealed = true,
            correctCount = state.correctCount + if (isCorrect) 1 else 0,
            currentStreak = nextStreak,
            maxStreak = maxOf(state.maxStreak, nextStreak)
        )
    }

    fun advanceQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            finishQuiz()
        } else {
            _quizState.value = state.copy(
                currentIndex = nextIdx,
                selectedOptionIndex = null,
                answerRevealed = false,
                secondsRemaining = state.difficulty.secondsPerQuestion
            )
        }
    }

    private fun finishQuiz() {
        timerJob?.cancel()
        val state = _quizState.value
        _quizState.value = state.copy(completed = true)
        val profile = (studentProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = studentRepo.recordQuizAttempt(
                profile = profile,
                category = state.category,
                difficulty = state.difficulty.name,
                mode = state.mode,
                totalQuestions = state.questions.size,
                correctAnswers = state.correctCount,
                timeSpentSeconds = state.totalElapsedSeconds,
                streak = state.maxStreak
            )
            res.onSuccess { allAchievements ->
                val newOnes = allAchievements.filter { it !in profile.achievements }
                _quizState.value = _quizState.value.copy(newlyUnlockedAchievements = newOnes)
            }
        }
    }

    fun exitQuiz() {
        timerJob?.cancel()
        _quizState.value = QuizSessionState()
    }

    // --- ADMIN ACTIONS ---
    fun uploadDocumentsBatch(
        items: List<UploadQueueItem>,
        description: String,
        subject: SubjectItem,
        category: CategoryItem,
        chapter: String,
        tags: List<String>,
        featured: Boolean,
        published: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (items.isEmpty()) return@launch
            _uploadProgress.value = 0f
            val total = items.size.toFloat()
            var successCount = 0
            items.forEachIndexed { idx, item ->
                val res = adminRepo.uploadNewDocument(
                    adminUid = currentUserId,
                    adminName = currentAdminName(),
                    adminUids = activeTeamAdminUids(),
                    title = item.title.ifBlank { item.payload.fileName.substringBeforeLast('.') },
                    description = description,
                    subjectId = subject.id,
                    subjectName = subject.nameEn,
                    categoryId = category.id,
                    categoryName = category.name,
                    chapter = chapter,
                    tags = tags,
                    featured = featured,
                    published = published,
                    filePayload = item.payload,
                    onProgress = { singleProgress ->
                        _uploadProgress.value = ((idx + singleProgress) / total).coerceIn(0f, 1f)
                    }
                )
                if (res.isSuccess) successCount++
            }
            _uploadProgress.value = null
            _statusMessage.value = "Uploaded $successCount of ${items.size} file(s) to ${subject.nameEn}."
            if (published && successCount > 0) {
                studentRepo.pushStudentNotification(
                    title = "New Study Material in ${subject.nameEn}",
                    body = "${items.first().title} has been published under ${category.name}.",
                    type = "DOCUMENT",
                    targetId = subject.id
                )
            }
            onComplete()
        }
    }

    fun updateDocument(
        doc: StudyDocument,
        newTitle: String,
        newDescription: String,
        subject: SubjectItem,
        category: CategoryItem,
        newChapter: String,
        newTags: List<String>,
        newFeatured: Boolean,
        newPublished: Boolean,
        replacementFile: PickedFilePayload? = null,
        changeNote: String = "Updated document metadata"
    ) {
        viewModelScope.launch {
            adminRepo.updateDocumentWithConflictCheck(
                doc = doc,
                expectedVersion = doc.version,
                adminUid = currentUserId,
                adminName = currentAdminName(),
                adminUids = activeTeamAdminUids(),
                newTitle = newTitle,
                newDescription = newDescription,
                newSubjectId = subject.id,
                newSubjectName = subject.nameEn,
                newCategoryId = category.id,
                newCategoryName = category.name,
                newChapter = newChapter,
                newTags = newTags,
                newFeatured = newFeatured,
                newPublished = newPublished,
                replacementFile = replacementFile,
                changeNote = changeNote
            ).onSuccess {
                _statusMessage.value = "Updated '$newTitle' to v${doc.version + 1}."
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Failed to update document."
            }
        }
    }

    fun loadDocumentVersions(documentId: String) {
        viewModelScope.launch {
            adminRepo.fetchDocumentVersions(documentId, currentUserId, isBootstrappedSuperAdmin)
                .onSuccess { _docVersions.value = it }
        }
    }

    fun deleteDocument(doc: StudyDocument) {
        viewModelScope.launch {
            adminRepo.deleteDocument(doc, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Deleted '${doc.title}'." }
                .onFailure { _statusMessage.value = it.message ?: "Delete failed." }
        }
    }

    fun bulkPublishDocuments(docs: List<StudyDocument>, published: Boolean) {
        viewModelScope.launch {
            adminRepo.bulkSetPublished(docs, published, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "${if (published) "Published" else "Unpublished"} ${docs.size} documents." }
                .onFailure { _statusMessage.value = it.message ?: "Bulk action failed." }
        }
    }

    fun bulkDeleteDocuments(docs: List<StudyDocument>) {
        viewModelScope.launch {
            adminRepo.bulkDeleteDocuments(docs, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Deleted ${docs.size} documents." }
                .onFailure { _statusMessage.value = it.message ?: "Bulk delete failed." }
        }
    }

    fun saveSubject(subject: SubjectItem, isNew: Boolean) {
        viewModelScope.launch {
            adminRepo.saveSubject(subject, isNew, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Saved subject '${subject.nameEn}'." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to save subject." }
        }
    }

    fun saveCategory(categoryId: String?, name: String, subjectId: String, visible: Boolean, sortOrder: Long) {
        viewModelScope.launch {
            adminRepo.saveCategory(categoryId, name, subjectId, visible, sortOrder, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Saved category '$name'." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to save category." }
        }
    }

    fun deleteCategory(category: CategoryItem) {
        viewModelScope.launch {
            adminRepo.deleteCategory(category.id, category.name, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Deleted category '${category.name}'." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to delete category." }
        }
    }

    fun inviteAdmin(email: String, displayName: String, role: AdminRole, permissions: List<String>) {
        viewModelScope.launch {
            adminRepo.inviteAdmin(email, displayName, role, permissions, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Invited $email as ${role.label}." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to invite administrator." }
        }
    }

    fun updateAdmin(target: AdminAccount, displayName: String, role: AdminRole, active: Boolean, permissions: List<String>) {
        viewModelScope.launch {
            adminRepo.updateAdminAccount(target, displayName, role, active, permissions, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Updated ${target.displayName}." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to update admin." }
        }
    }

    fun removeAdmin(target: AdminAccount) {
        viewModelScope.launch {
            adminRepo.removeAdminAccount(target, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Removed ${target.email}." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to remove admin." }
        }
    }

    fun removeInvite(invite: AdminInvite) {
        viewModelScope.launch {
            adminRepo.removeAdminInvite(invite.id, invite.email, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Revoked invite for ${invite.email}." }
        }
    }

    fun setStudentActive(student: StudentDirectoryEntry, active: Boolean) {
        viewModelScope.launch {
            adminRepo.setStudentActiveStatus(student.uid, student.displayName, active, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "${if (active) "Reactivated" else "Disabled"} ${student.displayName}." }
                .onFailure { _statusMessage.value = it.message ?: "Action failed." }
        }
    }

    fun removeStudent(student: StudentDirectoryEntry) {
        viewModelScope.launch {
            adminRepo.removeStudentRecord(student.uid, student.displayName, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Removed ${student.displayName}." }
                .onFailure { _statusMessage.value = it.message ?: "Remove failed." }
        }
    }

    fun generateAiQuestionsForAdmin(category: String, difficulty: QuizDifficulty, count: Int) {
        viewModelScope.launch {
            _aiGeneratingForAdmin.value = true
            val existingTexts = (quizQuestionsState.value as? UiState.Success)?.data?.map { it.questionText }.orEmpty()
            aiQuizRepo.generateGkQuestions(
                category = category,
                difficulty = difficulty,
                count = count,
                avoidQuestions = existingTexts
            ).onSuccess { list ->
                _aiDraftQuestions.value = list
                _statusMessage.value = "Generated ${list.size} AI questions for review."
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "AI generation failed."
            }
            _aiGeneratingForAdmin.value = false
        }
    }

    fun approveDraftQuestion(draft: GeneratedQuizQuestion, category: String, difficulty: QuizDifficulty) {
        viewModelScope.launch {
            val model = aiQuizRepo.toQuizQuestionModel(
                generated = draft,
                category = category,
                difficulty = difficulty,
                creatorUid = currentUserId,
                adminUids = activeTeamAdminUids(),
                approved = true
            )
            adminRepo.saveQuizQuestion(model, true, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess {
                    _aiDraftQuestions.value = _aiDraftQuestions.value - draft
                    _statusMessage.value = "Approved & saved question to GK bank."
                }
                .onFailure { _statusMessage.value = it.message ?: "Failed to save question." }
        }
    }

    fun discardDraftQuestion(draft: GeneratedQuizQuestion) {
        _aiDraftQuestions.value = _aiDraftQuestions.value - draft
    }

    fun saveQuizQuestion(question: QuizQuestion, isNew: Boolean) {
        viewModelScope.launch {
            adminRepo.saveQuizQuestion(question, isNew, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Saved GK question." }
                .onFailure { _statusMessage.value = it.message ?: "Failed to save question." }
        }
    }

    fun deleteQuizQuestion(question: QuizQuestion) {
        viewModelScope.launch {
            adminRepo.deleteQuizQuestion(question.id, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Deleted GK question." }
        }
    }

    fun saveAnnouncement(announcement: Announcement, isNew: Boolean) {
        viewModelScope.launch {
            adminRepo.saveAnnouncement(announcement, isNew, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess {
                    _statusMessage.value = "Saved announcement."
                    if (announcement.published) {
                        studentRepo.pushStudentNotification(
                            title = "Notice: ${announcement.title}",
                            body = announcement.body.take(140),
                            type = "ANNOUNCEMENT",
                            targetId = announcement.id
                        )
                    }
                }
                .onFailure { _statusMessage.value = it.message ?: "Failed to save announcement." }
        }
    }

    fun deleteAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            adminRepo.deleteAnnouncement(announcement.id, announcement.title, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Deleted announcement." }
        }
    }

    fun updateAppSettings(settings: AppSettings) {
        viewModelScope.launch {
            adminRepo.updateAppSettings(settings, currentUserId, currentAdminName(), activeTeamAdminUids())
                .onSuccess { _statusMessage.value = "Saved application settings." }
                .onFailure { _statusMessage.value = it.message ?: "Settings update failed." }
        }
    }

    fun logAdminLogout() {
        viewModelScope.launch {
            if (isAdminAuthorized.value) {
                adminRepo.logActivity(
                    adminUid = currentUserId,
                    adminName = currentAdminName(),
                    actionType = "LOGOUT",
                    summary = "${currentAdminName()} signed out",
                    targetId = currentUserId,
                    adminUids = activeTeamAdminUids()
                )
            }
        }
    }

    fun clearSessionOnSignOut() {
        timerJob?.cancel()
        viewModelScope.launch { sessionPrefs.clearStudentCache() }
    }

    private companion object {
        const val TAG = "DorbarVM"
    }
}
