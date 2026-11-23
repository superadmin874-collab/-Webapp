package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.DocumentStorageHelper
import com.example.core.unwrapFirestoreError
import com.example.data.model.Announcement
import com.example.data.model.BroadcastNotification
import com.example.data.model.Category
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.StudentDownload
import com.example.data.model.StudentFavorite
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.example.data.model.UiState
import com.example.data.repository.AdminControlRepository
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DocumentPreviewState(
    val document: StudyDocument? = null,
    val isLoading: Boolean = false,
    val bytes: ByteArray? = null,
    val localFile: File? = null,
    val error: String? = null
)

data class SearchFilterState(
    val query: String = "",
    val subjectId: String? = null,
    val categoryId: String? = null,
    val chapter: String? = null,
    val fileType: String? = null,
    val dateWindowDays: Int? = null
)

class StudentViewModel(
    private val documentRepository: DocumentRepository,
    private val adminControlRepository: AdminControlRepository,
    private val currentUserId: String
) : ViewModel() {

    val documentsState: StateFlow<UiState<List<StudyDocument>>> =
        documentRepository.observePublishedDocuments()
            .map<List<StudyDocument>, UiState<List<StudyDocument>>> { UiState.Success(it) }
            .catch { err ->
                val root = unwrapFirestoreError(err)
                Log.w("StudentVM", "Error observing documents", root)
                emit(UiState.Error(root.localizedMessage ?: "Failed to load documents"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val subjectsState: StateFlow<List<Subject>> =
        documentRepository.observeVisibleSubjects()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(DarbarCurriculumDefaults.DEFAULT_SUBJECTS)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DarbarCurriculumDefaults.DEFAULT_SUBJECTS)

    val categoriesState: StateFlow<List<Category>> =
        documentRepository.observeVisibleCategories()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(DarbarCurriculumDefaults.DEFAULT_CATEGORIES)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DarbarCurriculumDefaults.DEFAULT_CATEGORIES)

    val favoritesState: StateFlow<List<StudentFavorite>> =
        documentRepository.observeFavorites(currentUserId)
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val downloadsState: StateFlow<List<StudentDownload>> =
        documentRepository.observeDownloads(currentUserId)
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcementsState: StateFlow<List<Announcement>> =
        adminControlRepository.observePublishedAnnouncements()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationsState: StateFlow<List<BroadcastNotification>> =
        adminControlRepository.observeActiveNotifications()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val readNotificationIds: StateFlow<Set<String>> =
        adminControlRepository.observeReadNotificationIds(currentUserId)
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptySet())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter.asStateFlow()

    val filteredDocuments: StateFlow<List<StudyDocument>> = combine(
        documentsState,
        _searchFilter
    ) { docUi, filter ->
        val all = (docUi as? UiState.Success)?.data ?: emptyList()
        val q = filter.query.trim().lowercase()
        val nowSec = System.currentTimeMillis() / 1000L

        all.filter { doc ->
            val matchesQuery = q.isBlank() ||
                doc.title.lowercase().contains(q) ||
                doc.description.lowercase().contains(q) ||
                doc.chapter.lowercase().contains(q) ||
                doc.subjectName.lowercase().contains(q) ||
                doc.categoryName.lowercase().contains(q) ||
                doc.tags.any { it.lowercase().contains(q) }
            val matchesSubj = filter.subjectId == null || doc.subjectId == filter.subjectId
            val matchesCat = filter.categoryId == null || doc.categoryId == filter.categoryId
            val matchesChapter = filter.chapter == null || doc.chapter.equals(filter.chapter, ignoreCase = true)
            val matchesType = filter.fileType == null || doc.fileType.equals(filter.fileType, ignoreCase = true)
            val matchesDate = filter.dateWindowDays == null || run {
                val docSec = doc.createdAt?.seconds ?: nowSec
                (nowSec - docSec) <= filter.dateWindowDays * 86400L
            }
            matchesQuery && matchesSubj && matchesCat && matchesChapter && matchesType && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _previewState = MutableStateFlow(DocumentPreviewState())
    val previewState: StateFlow<DocumentPreviewState> = _previewState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isDownloadingId = MutableStateFlow<String?>(null)
    val isDownloadingId: StateFlow<String?> = _isDownloadingId.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchFilter.value = _searchFilter.value.copy(query = query)
    }

    fun updateFilters(
        subjectId: String? = _searchFilter.value.subjectId,
        categoryId: String? = _searchFilter.value.categoryId,
        chapter: String? = _searchFilter.value.chapter,
        fileType: String? = _searchFilter.value.fileType,
        dateWindowDays: Int? = _searchFilter.value.dateWindowDays
    ) {
        _searchFilter.value = _searchFilter.value.copy(
            subjectId = subjectId,
            categoryId = categoryId,
            chapter = chapter,
            fileType = fileType,
            dateWindowDays = dateWindowDays
        )
    }

    fun resetFilters() {
        _searchFilter.value = SearchFilterState()
    }

    fun toggleFavorite(doc: StudyDocument, isCurrentlyFav: Boolean) {
        viewModelScope.launch {
            val res = documentRepository.toggleFavorite(doc, isCurrentlyFav)
            res.onSuccess { added ->
                _actionMessage.value = if (added) "Saved '${doc.title}' to Favourites" else "Removed from Favourites"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update favourites"
            }
        }
    }

    fun openPreview(context: Context, doc: StudyDocument) {
        viewModelScope.launch {
            _previewState.value = DocumentPreviewState(document = doc, isLoading = true)
            try {
                val bytes = documentRepository.loadDocumentBytes(doc).getOrThrow()
                val file = DocumentStorageHelper.saveToInternalDocs(context, doc.id, doc.fileName, bytes)
                _previewState.value = DocumentPreviewState(
                    document = doc,
                    isLoading = false,
                    bytes = bytes,
                    localFile = file
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _previewState.value = DocumentPreviewState(
                    document = doc,
                    isLoading = false,
                    error = e.localizedMessage ?: "Unable to preview document"
                )
            }
        }
    }

    fun downloadDocument(doc: StudyDocument, allowDownloads: Boolean) {
        if (!allowDownloads) {
            _actionMessage.value = "Downloads are temporarily disabled by administrators."
            return
        }
        viewModelScope.launch {
            _isDownloadingId.value = doc.id
            try {
                val res = documentRepository.downloadDocumentToDevice(doc)
                res.onSuccess {
                    _actionMessage.value = "Downloaded '${doc.fileName}' to Downloads/Darbar"
                }.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Download failed"
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isDownloadingId.value = null
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            adminControlRepository.markNotificationRead(currentUserId, notifId)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }
}
