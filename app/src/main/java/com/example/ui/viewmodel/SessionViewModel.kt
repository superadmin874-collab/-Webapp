package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.unwrapFirestoreError
import com.example.data.model.AdminUser
import com.example.data.model.AppSettings
import com.example.data.model.StudentProfile
import com.example.data.model.UiState
import com.example.data.repository.SessionRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val sessionRepository: SessionRepository,
    private val currentUserId: String
) : ViewModel() {

    val adminState: StateFlow<UiState<AdminUser?>> = sessionRepository.observeAdminProfile(currentUserId)
        .map<AdminUser?, UiState<AdminUser?>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            Log.w("SessionVM", "Admin stream error", root)
            emit(UiState.Success(null))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val studentState: StateFlow<UiState<StudentProfile?>> = sessionRepository.observeStudentProfile(currentUserId)
        .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            Log.w("SessionVM", "Student stream error", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load student profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val appSettings: StateFlow<AppSettings> = sessionRepository.observeAppSettings()
        .catch { err ->
            unwrapFirestoreError(err)
            emit(AppSettings(id = "global"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings(id = "global"))

    val themeMode: StateFlow<ThemeMode> = sessionRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemeMode.SYSTEM)

    val reducedMotion: StateFlow<Boolean> = sessionRepository.reducedMotionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val rememberedStudentName: StateFlow<String> = sessionRepository.rememberedStudentNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    private val _adminConsoleOpen = MutableStateFlow(false)
    val adminConsoleOpen: StateFlow<Boolean> = _adminConsoleOpen.asStateFlow()

    private val _isSavingStudentName = MutableStateFlow(false)
    val isSavingStudentName: StateFlow<Boolean> = _isSavingStudentName.asStateFlow()

    private val _sessionMessage = MutableStateFlow<String?>(null)
    val sessionMessage: StateFlow<String?> = _sessionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val result = sessionRepository.syncAdminAccessOnLogin()
            result.onSuccess { admin ->
                if (admin != null && admin.active) {
                    _adminConsoleOpen.value = true
                }
            }
        }
    }

    fun setAdminConsoleOpen(open: Boolean) {
        _adminConsoleOpen.value = open
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { sessionRepository.setThemeMode(mode) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { sessionRepository.setReducedMotion(enabled) }
    }

    fun saveStudentName(fullName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSavingStudentName.value = true
            _sessionMessage.value = null
            try {
                val res = sessionRepository.registerOrUpdateStudentName(fullName)
                res.onSuccess {
                    onSuccess()
                }.onFailure { err ->
                    _sessionMessage.value = err.localizedMessage ?: "Unable to save name."
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isSavingStudentName.value = false
            }
        }
    }

    fun clearSessionMessage() {
        _sessionMessage.value = null
    }

    suspend fun clearLocalStudentRemembered() {
        sessionRepository.clearRememberedStudentName()
    }
}
