package com.example.data.repository

import android.util.Log
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AdminUser
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
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
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedQuizDraft(
    val category: String,
    val difficulty: QuizDifficulty,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val source: String,
    val factDate: String,
    val confidenceNote: String
)

class QuizRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        private const val TAG = "QuizRepository"
    }

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "questionText" to Schema.string(description = "Clear, unambiguous GK question"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct multiple-choice options"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0 to 3) of the single correct option"),
                        "explanation" to Schema.string(description = "Concise, accurate educational explanation"),
                        "source" to Schema.string(description = "Authoritative factual source or reference"),
                        "factDate" to Schema.string(description = "Relevant year or YYYY-MM-DD date for current affairs or historical context"),
                        "confidenceNote" to Schema.string(description = "VERIFIED if indisputable established fact, or VERIFY_RECOMMENDED if time-sensitive")
                    )
                )
            )
        )
    )

    // Lazy initialization per Firebase AI Logic skill guidelines
    private val geminiQuizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    fun observeApprovedQuestions(): Flow<List<QuizQuestion>> =
        db.collection("quiz_questions")
            .whereEqualTo("approved", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllQuestionsAdmin(): Flow<List<QuizQuestion>> =
        db.collection("quiz_questions")
            .snapshots()
            .map { snap ->
                snap.toObjects(QuizQuestion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboard")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.totalScore }
            }

    fun observeQuizAttempts(uid: String): Flow<List<QuizAttempt>> = flow {
        if (uid.isBlank()) throw IllegalStateException("Missing student UID")
        emitAll(
            db.collection("students").document(uid).collection("quiz_attempts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    suspend fun generateQuestionsWithAi(
        category: String,
        difficulty: QuizDifficulty,
        count: Int,
        existingQuestionsToAvoid: List<String> = emptyList()
    ): Result<List<GeneratedQuizDraft>> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val avoidSnippet = existingQuestionsToAvoid.take(15).joinToString("\n- ")
            val prompt = buildString {
                appendLine("Generate $count high-accuracy educational multiple-choice General Knowledge questions.")
                appendLine("Category: $category")
                appendLine("Difficulty: ${difficulty.name}")
                appendLine("Today's Date: $today")
                appendLine("Rules:")
                appendLine("1. Each question must have exactly 4 distinct options and 1 unambiguously correct answer (correctIndex 0..3).")
                appendLine("2. Include a clear educational explanation.")
                appendLine("3. Provide a real authoritative source and factDate (for Current Affairs, cite the date/month/year; never fabricate unverified facts—mark confidenceNote as VERIFY_RECOMMENDED if time-sensitive).")
                if (avoidSnippet.isNotBlank()) {
                    appendLine("4. Do NOT repeat or paraphrase any of these existing questions:")
                    appendLine("- $avoidSnippet")
                }
            }

            val response = withTimeout(60_000L) {
                geminiQuizModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("questions")
            val drafts = mutableListOf<GeneratedQuizDraft>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.getJSONArray("options")
                val opts = (0 until optsArr.length()).map { optsArr.getString(it).trim() }
                if (opts.size == 4) {
                    drafts += GeneratedQuizDraft(
                        category = category,
                        difficulty = difficulty,
                        questionText = obj.getString("questionText").trim(),
                        options = opts,
                        correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                        explanation = obj.getString("explanation").trim(),
                        source = obj.optString("source", "General Educational Reference").trim(),
                        factDate = obj.optString("factDate", today).trim(),
                        confidenceNote = obj.optString("confidenceNote", "VERIFIED").trim()
                    )
                }
            }

            if (drafts.isEmpty()) {
                Result.failure(IllegalStateException("AI could not produce valid quiz questions. Please retry."))
            } else {
                Result.success(drafts)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI quiz generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun saveAdminQuizQuestion(
        admin: AdminUser,
        question: QuizQuestion,
        isNew: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        val ref = if (isNew || question.id.isBlank()) {
            db.collection("quiz_questions").document()
        } else {
            db.collection("quiz_questions").document(question.id)
        }

        return try {
            val cleanOpts = question.options.take(4).let {
                if (it.size == 4) it else listOf("Option A", "Option B", "Option C", "Option D")
            }
            if (isNew || !ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "category" to question.category.trim().take(80),
                        "difficulty" to question.difficulty,
                        "questionText" to question.questionText.trim().take(600),
                        "options" to cleanOpts.map { it.trim().take(200) },
                        "correctIndex" to question.correctIndex.coerceIn(0L, 3L),
                        "explanation" to question.explanation.trim().ifBlank { "Educational reference." }.take(1000),
                        "source" to question.source.trim().take(200),
                        "factDate" to question.factDate.trim().take(40),
                        "aiGenerated" to question.aiGenerated,
                        "verifiedByAdmin" to question.verifiedByAdmin,
                        "approved" to question.approved,
                        "createdByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "category" to question.category.trim().take(80),
                        "difficulty" to question.difficulty,
                        "questionText" to question.questionText.trim().take(600),
                        "options" to cleanOpts.map { it.trim().take(200) },
                        "correctIndex" to question.correctIndex.coerceIn(0L, 3L),
                        "explanation" to question.explanation.trim().ifBlank { "Educational reference." }.take(1000),
                        "source" to question.source.trim().take(200),
                        "factDate" to question.factDate.trim().take(40),
                        "verifiedByAdmin" to question.verifiedByAdmin,
                        "approved" to question.approved,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to if (question.approved) "APPROVE_QUIZ_QUESTION" else "SAVE_QUIZ_QUESTION",
                    "targetType" to "quiz_question",
                    "targetId" to ref.id,
                    "details" to "${question.category}: ${question.questionText.take(80)}",
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

    suspend fun deleteQuizQuestion(admin: AdminUser, questionId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        val ref = db.collection("quiz_questions").document(questionId)
        return try {
            ref.delete().await()
            db.collection("activity_logs").document().set(
                mapOf(
                    "adminUid" to uid,
                    "adminName" to admin.displayName,
                    "adminRole" to admin.role,
                    "action" to "DELETE_QUIZ_QUESTION",
                    "targetType" to "quiz_question",
                    "targetId" to questionId,
                    "details" to "Deleted GK question $questionId",
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

    suspend fun recordStudentQuizAttempt(
        student: StudentProfile,
        category: String,
        difficulty: QuizDifficulty,
        mode: String,
        totalQuestions: Int,
        correctCount: Int,
        durationSeconds: Int
    ): Result<List<String>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sign in required."))
        val attemptRef = db.collection("students").document(uid).collection("quiz_attempts").document()
        val scorePercent = if (totalQuestions <= 0) 0 else ((correctCount * 100.0) / totalQuestions).toInt().coerceIn(0, 100)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        return try {
            attemptRef.set(
                mapOf(
                    "category" to category.take(80),
                    "difficulty" to difficulty.name,
                    "mode" to mode.take(40),
                    "totalQuestions" to totalQuestions.toLong(),
                    "correctCount" to correctCount.toLong(),
                    "scorePercent" to scorePercent.toLong(),
                    "durationSeconds" to durationSeconds.toLong(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val newQuizzesTaken = student.quizzesTaken + 1L
            val newTotalScore = student.totalQuizScore + correctCount.toLong()
            val newTotalQuestions = student.totalQuestionsAnswered + totalQuestions.toLong()

            val unlocked = student.achievements.toMutableSet()
            if (newQuizzesTaken >= 1L) unlocked += "First Quiz Completed"
            if (scorePercent >= 80) unlocked += "Sharp Mind (80%+)"
            if (scorePercent == 100 && totalQuestions >= 5) unlocked += "Perfect Scholar (100%)"
            if (newQuizzesTaken >= 5L) unlocked += "Quiz Explorer (5+ Quizzes)"
            if (newTotalQuestions >= 50L) unlocked += "Knowledge Seeker (50+ Questions)"
            if (mode.contains("Daily", ignoreCase = true)) unlocked += "Daily Challenger"

            val newlyUnlocked = unlocked.filterNot { it in student.achievements }

            db.collection("students").document(uid).update(
                mapOf(
                    "quizzesTaken" to newQuizzesTaken,
                    "totalQuizScore" to newTotalScore,
                    "totalQuestionsAnswered" to newTotalQuestions,
                    "achievements" to unlocked.take(30),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update leaderboard entry
            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            val pointsEarned = correctCount.toLong() * when (difficulty) {
                QuizDifficulty.EASY -> 10L
                QuizDifficulty.MEDIUM -> 15L
                QuizDifficulty.HARD -> 25L
            }
            if (lbSnap.exists()) {
                val prevTotal = lbSnap.getLong("totalScore") ?: 0L
                val prevTaken = lbSnap.getLong("quizzesTaken") ?: 0L
                val prevBest = lbSnap.getLong("bestPercent") ?: 0L
                val prevDailyDate = lbSnap.getString("dailyDate") ?: ""
                val prevDailyScore = if (prevDailyDate == today) (lbSnap.getLong("dailyScore") ?: 0L) else 0L
                lbRef.update(
                    mapOf(
                        "fullName" to student.fullName.take(80),
                        "totalScore" to prevTotal + pointsEarned,
                        "quizzesTaken" to prevTaken + 1L,
                        "bestPercent" to maxOf(prevBest, scorePercent.toLong()),
                        "dailyScore" to prevDailyScore + pointsEarned,
                        "dailyDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.set(
                    mapOf(
                        "uid" to uid,
                        "studentCode" to student.studentCode,
                        "fullName" to student.fullName.take(80),
                        "totalScore" to pointsEarned,
                        "quizzesTaken" to 1L,
                        "bestPercent" to scorePercent.toLong(),
                        "dailyScore" to pointsEarned,
                        "dailyDate" to today,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(newlyUnlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, attemptRef.path)
            Result.failure(e)
        }
    }
}
