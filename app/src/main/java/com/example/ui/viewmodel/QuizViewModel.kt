package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.unwrapFirestoreError
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentProfile
import com.example.data.repository.QuizRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActiveQuizItem(
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val source: String,
    val factDate: String,
    val aiGenerated: Boolean,
    val verifiedByAdmin: Boolean
)

sealed interface QuizSessionState {
    data object Lobby : QuizSessionState
    data class Generating(val message: String) : QuizSessionState
    data class Playing(
        val mode: String,
        val category: String,
        val difficulty: QuizDifficulty,
        val questions: List<ActiveQuizItem>,
        val currentIndex: Int,
        val selectedOption: Int?,
        val correctCount: Int,
        val remainingSeconds: Int,
        val elapsedSeconds: Int
    ) : QuizSessionState

    data class Completed(
        val mode: String,
        val category: String,
        val difficulty: QuizDifficulty,
        val totalQuestions: Int,
        val correctCount: Int,
        val scorePercent: Int,
        val durationSeconds: Int,
        val newlyUnlockedAchievements: List<String>
    ) : QuizSessionState

    data class Error(val message: String) : QuizSessionState
}

class QuizViewModel(
    private val quizRepository: QuizRepository,
    private val currentUserId: String
) : ViewModel() {

    val approvedQuestions: StateFlow<List<QuizQuestion>> =
        quizRepository.observeApprovedQuestions()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> =
        quizRepository.observeLeaderboard()
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val quizHistory: StateFlow<List<QuizAttempt>> =
        quizRepository.observeQuizAttempts(currentUserId)
            .catch { err ->
                unwrapFirestoreError(err)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _sessionState = MutableStateFlow<QuizSessionState>(QuizSessionState.Lobby)
    val sessionState: StateFlow<QuizSessionState> = _sessionState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DarbarCurriculumDefaults.GK_CATEGORIES.first())
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(QuizDifficulty.MEDIUM)
    val selectedDifficulty: StateFlow<QuizDifficulty> = _selectedDifficulty.asStateFlow()

    private val _selectedCount = MutableStateFlow(5)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    private var timerJob: Job? = null

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun selectDifficulty(diff: QuizDifficulty) {
        _selectedDifficulty.value = diff
    }

    fun selectCount(count: Int) {
        _selectedCount.value = count
    }

    fun startQuiz(mode: String, allowQuiz: Boolean) {
        if (!allowQuiz) {
            _sessionState.value = QuizSessionState.Error("GK Challenges are currently paused by administrators.")
            return
        }
        timerJob?.cancel()
        val category = when (mode) {
            "Daily GK Challenge" -> {
                val dayIdx = (System.currentTimeMillis() / 86400000L).toInt() % DarbarCurriculumDefaults.GK_CATEGORIES.size
                DarbarCurriculumDefaults.GK_CATEGORIES[dayIdx]
            }
            "Random GK Challenge" -> DarbarCurriculumDefaults.GK_CATEGORIES.random()
            else -> _selectedCategory.value
        }
        val difficulty = _selectedDifficulty.value
        val targetCount = _selectedCount.value

        viewModelScope.launch {
            _sessionState.value = QuizSessionState.Generating("Preparing $targetCount $difficulty questions for $category...")
            try {
                // First pull matching verified/approved questions from Firestore
                val pool = approvedQuestions.value.filter { q ->
                    (mode == "Random GK Challenge" || q.category.equals(category, ignoreCase = true)) &&
                        q.difficulty == difficulty.name
                }.shuffled()

                val activeList = pool.take(targetCount).map { q ->
                    ActiveQuizItem(
                        questionText = q.questionText,
                        options = q.options,
                        correctIndex = q.correctIndex.toInt(),
                        explanation = q.explanation,
                        source = q.source,
                        factDate = q.factDate,
                        aiGenerated = q.aiGenerated,
                        verifiedByAdmin = q.verifiedByAdmin
                    )
                }.toMutableList()

                val needed = targetCount - activeList.size
                if (needed > 0) {
                    val avoidList = (pool.map { it.questionText } + activeList.map { it.questionText })
                    val aiResult = quizRepository.generateQuestionsWithAi(
                        category = category,
                        difficulty = difficulty,
                        count = needed,
                        existingQuestionsToAvoid = avoidList
                    )
                    aiResult.onSuccess { drafts ->
                        drafts.forEach { d ->
                            activeList += ActiveQuizItem(
                                questionText = d.questionText,
                                options = d.options,
                                correctIndex = d.correctIndex,
                                explanation = d.explanation,
                                source = "${d.source} (${d.confidenceNote})",
                                factDate = d.factDate,
                                aiGenerated = true,
                                verifiedByAdmin = false
                            )
                        }
                    }.onFailure { err ->
                        if (activeList.isEmpty()) {
                            _sessionState.value = QuizSessionState.Error(
                                err.localizedMessage ?: "Failed to generate AI quiz questions. Please check connection and retry."
                            )
                            return@launch
                        }
                    }
                }

                if (activeList.isEmpty()) {
                    _sessionState.value = QuizSessionState.Error("No questions available for this selection.")
                    return@launch
                }

                val initialSeconds = 30
                _sessionState.value = QuizSessionState.Playing(
                    mode = mode,
                    category = category,
                    difficulty = difficulty,
                    questions = activeList,
                    currentIndex = 0,
                    selectedOption = null,
                    correctCount = 0,
                    remainingSeconds = initialSeconds,
                    elapsedSeconds = 0
                )
                startQuestionTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("QuizVM", "Error starting quiz", e)
                _sessionState.value = QuizSessionState.Error(e.localizedMessage ?: "Quiz error")
            }
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _sessionState.value as? QuizSessionState.Playing ?: break
                if (state.selectedOption != null) continue
                val nextRemaining = (state.remainingSeconds - 1).coerceAtLeast(0)
                val nextElapsed = state.elapsedSeconds + 1
                _sessionState.value = state.copy(
                    remainingSeconds = nextRemaining,
                    elapsedSeconds = nextElapsed
                )
                if (nextRemaining == 0) {
                    // Auto-reveal when timer runs out
                    _sessionState.value = state.copy(
                        selectedOption = -1,
                        remainingSeconds = 0,
                        elapsedSeconds = nextElapsed
                    )
                }
            }
        }
    }

    fun selectAnswer(optionIndex: Int) {
        val state = _sessionState.value as? QuizSessionState.Playing ?: return
        if (state.selectedOption != null) return
        val currentQ = state.questions[state.currentIndex]
        val isCorrect = optionIndex == currentQ.correctIndex
        _sessionState.value = state.copy(
            selectedOption = optionIndex,
            correctCount = if (isCorrect) state.correctCount + 1 else state.correctCount
        )
    }

    fun nextQuestion(student: StudentProfile?) {
        val state = _sessionState.value as? QuizSessionState.Playing ?: return
        val nextIdx = state.currentIndex + 1
        if (nextIdx < state.questions.size) {
            _sessionState.value = state.copy(
                currentIndex = nextIdx,
                selectedOption = null,
                remainingSeconds = 30
            )
        } else {
            timerJob?.cancel()
            val total = state.questions.size
            val percent = ((state.correctCount * 100.0) / total).toInt().coerceIn(0, 100)
            viewModelScope.launch {
                val unlocked = if (student != null) {
                    quizRepository.recordStudentQuizAttempt(
                        student = student,
                        category = state.category,
                        difficulty = state.difficulty,
                        mode = state.mode,
                        totalQuestions = total,
                        correctCount = state.correctCount,
                        durationSeconds = state.elapsedSeconds
                    ).getOrElse { emptyList() }
                } else emptyList()

                _sessionState.value = QuizSessionState.Completed(
                    mode = state.mode,
                    category = state.category,
                    difficulty = state.difficulty,
                    totalQuestions = total,
                    correctCount = state.correctCount,
                    scorePercent = percent,
                    durationSeconds = state.elapsedSeconds,
                    newlyUnlockedAchievements = unlocked
                )
            }
        }
    }

    fun returnToLobby() {
        timerJob?.cancel()
        _sessionState.value = QuizSessionState.Lobby
    }

    fun todayDateLabel(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
