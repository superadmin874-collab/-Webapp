package com.example.data.repository

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.DownloadRecord
import com.example.data.model.FavoriteRecord
import com.example.data.model.QuizAttempt
import com.example.data.model.StudentNotification
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class StudentRepository(private val db: FirebaseFirestore) {
    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    private fun <T> Flow<T>.handleStudentErrors(path: String): Flow<T> = catch { error ->
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

    fun observeStudentProfile(uid: String): Flow<StudentProfile?> =
        db.collection("students").document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .handleStudentErrors("students/$uid")

    fun observeFavorites(uid: String): Flow<List<FavoriteRecord>> =
        db.collection("students").document(uid).collection("favorites")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FavoriteRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleStudentErrors("students/$uid/favorites")

    fun observeDownloads(uid: String): Flow<List<DownloadRecord>> =
        db.collection("students").document(uid).collection("downloads")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DownloadRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleStudentErrors("students/$uid/downloads")

    fun observeQuizAttempts(uid: String): Flow<List<QuizAttempt>> =
        db.collection("students").document(uid).collection("quiz_attempts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuizAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleStudentErrors("students/$uid/quiz_attempts")

    fun observeNotifications(uid: String): Flow<List<StudentNotification>> =
        db.collection("students").document(uid).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudentNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .handleStudentErrors("students/$uid/notifications")

    suspend fun registerOrUpdateStudentName(
        fullName: String,
        adminUids: List<String>
    ): Result<StudentProfile> {
        val cleanName = fullName.trim().take(100)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Authentication required."))
        val studentRef = db.collection("students").document(uid)
        val dirRef = db.collection("student_directory").document(uid)

        return try {
            val existing = studentRef.get().await()
            val generatedCode = "DRB-${(abs(uid.hashCode()) % 900000 + 100000)}"
            if (!existing.exists()) {
                val studentPayload = mapOf(
                    "uid" to uid,
                    "studentCode" to generatedCode,
                    "fullName" to cleanName,
                    "active" to true,
                    "downloadCount" to 0L,
                    "quizAttemptsCount" to 0L,
                    "totalQuizScore" to 0L,
                    "totalQuizQuestions" to 0L,
                    "bestStreak" to 0L,
                    "achievements" to listOf("Welcome to দরবার"),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                studentRef.set(studentPayload).await()

                val dirPayload = mapOf(
                    "uid" to uid,
                    "studentCode" to generatedCode,
                    "displayName" to cleanName,
                    "active" to true,
                    "downloadCount" to 0L,
                    "quizAttemptsCount" to 0L,
                    "averageScorePercent" to 0L,
                    "bestScorePercent" to 0L,
                    "totalPoints" to 0L,
                    "adminUids" to adminUids.take(20),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                dirRef.set(dirPayload).await()

                studentRef.collection("notifications").document("welcome_notif").set(
                    mapOf(
                        "title" to "Welcome to দরবার, $cleanName!",
                        "body" to "Your unique Student ID is $generatedCode. Browse all six subjects or start an AI GK Challenge.",
                        "type" to "WELCOME",
                        "read" to false,
                        "targetId" to "",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                studentRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (dirRef.get().await().exists()) {
                    dirRef.update(
                        mapOf(
                            "displayName" to cleanName,
                            "lastActiveAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            val updated = studentRef.get().await()
                .toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: StudentProfile(uid = uid, studentCode = generatedCode, fullName = cleanName)
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "students/$uid")
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(doc: StudyDocument, currentlyFavorite: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        val favRef = db.collection("students").document(uid).collection("favorites").document(doc.id)
        return try {
            if (currentlyFavorite) {
                favRef.delete().await()
            } else {
                favRef.set(
                    mapOf(
                        "documentId" to doc.id,
                        "title" to doc.title,
                        "subjectId" to doc.subjectId,
                        "subjectName" to doc.subjectName,
                        "fileExtension" to doc.fileExtension,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, favRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordDownload(doc: StudyDocument, localPath: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        val studentRef = db.collection("students").document(uid)
        val dirRef = db.collection("student_directory").document(uid)
        val dlRef = studentRef.collection("downloads").document()
        val docRef = db.collection("documents").document(doc.id)

        return try {
            dlRef.set(
                mapOf(
                    "documentId" to doc.id,
                    "title" to doc.title,
                    "subjectId" to doc.subjectId,
                    "subjectName" to doc.subjectName,
                    "fileName" to doc.fileName,
                    "mimeType" to doc.mimeType,
                    "fileExtension" to doc.fileExtension,
                    "fileSizeBytes" to doc.fileSizeBytes,
                    "localPath" to localPath,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (doc.published) {
                try {
                    docRef.update(mapOf("downloadCount" to FieldValue.increment(1L))).await()
                } catch (_: Exception) {
                    // Do not fail local download record if counter increment races
                }
            }

            studentRef.update(
                mapOf(
                    "downloadCount" to FieldValue.increment(1L),
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (dirRef.get().await().exists()) {
                dirRef.update(
                    mapOf(
                        "downloadCount" to FieldValue.increment(1L),
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, dlRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizAttempt(
        profile: StudentProfile,
        category: String,
        difficulty: String,
        mode: String,
        totalQuestions: Int,
        correctAnswers: Int,
        timeSpentSeconds: Int,
        streak: Int
    ): Result<List<String>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        val scorePercent = if (totalQuestions > 0) ((correctAnswers * 100) / totalQuestions) else 0
        val pointsEarned = (correctAnswers * 10) + (if (scorePercent >= 80) 20 else 0)
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val studentRef = db.collection("students").document(uid)
        val attemptRef = studentRef.collection("quiz_attempts").document()
        val dirRef = db.collection("student_directory").document(uid)
        val lbRef = db.collection("leaderboard").document(uid)

        return try {
            attemptRef.set(
                mapOf(
                    "category" to category,
                    "difficulty" to difficulty,
                    "mode" to mode,
                    "totalQuestions" to totalQuestions.toLong(),
                    "correctAnswers" to correctAnswers.toLong(),
                    "scorePercent" to scorePercent.toLong(),
                    "timeSpentSeconds" to timeSpentSeconds.toLong(),
                    "studentName" to profile.fullName.ifBlank { "Student" },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val newAttemptsCount = profile.quizAttemptsCount + 1L
            val newTotalScore = profile.totalQuizScore + correctAnswers.toLong()
            val newTotalQuestions = profile.totalQuizQuestions + totalQuestions.toLong()
            val avgPercent = if (newTotalQuestions > 0) {
                ((newTotalScore * 100L) / newTotalQuestions).coerceIn(0L, 100L)
            } else 0L
            val newBestStreak = maxOf(profile.bestStreak, streak.toLong())

            val achievements = profile.achievements.toMutableSet()
            if (newAttemptsCount >= 1L) achievements.add("First Quiz Finisher")
            if (scorePercent >= 80) achievements.add("Scholar 80%+")
            if (scorePercent == 100 && totalQuestions >= 5) achievements.add("Perfect Score Ace")
            if (newBestStreak >= 5L) achievements.add("5-Answer Streak")
            if (newAttemptsCount >= 5L) achievements.add("Quiz Enthusiast")

            val achievementList = achievements.take(30)

            studentRef.update(
                mapOf(
                    "quizAttemptsCount" to newAttemptsCount,
                    "totalQuizScore" to newTotalScore,
                    "totalQuizQuestions" to newTotalQuestions,
                    "bestStreak" to newBestStreak,
                    "achievements" to achievementList,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val existingLb = lbRef.get().await()
            val prevPoints = existingLb.getLong("totalPoints") ?: 0L
            val prevDailyPoints = if (existingLb.getString("dailyDateKey") == todayKey) {
                existingLb.getLong("dailyPoints") ?: 0L
            } else 0L
            val newTotalPoints = prevPoints + pointsEarned.toLong()
            val newDailyPoints = prevDailyPoints + pointsEarned.toLong()

            if (!existingLb.exists()) {
                lbRef.set(
                    mapOf(
                        "uid" to uid,
                        "studentCode" to profile.studentCode,
                        "displayName" to profile.fullName.ifBlank { "Student" },
                        "totalPoints" to newTotalPoints,
                        "quizzesCompleted" to newAttemptsCount,
                        "averageScorePercent" to avgPercent,
                        "dailyPoints" to newDailyPoints,
                        "dailyDateKey" to todayKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.update(
                    mapOf(
                        "displayName" to profile.fullName.ifBlank { "Student" },
                        "totalPoints" to newTotalPoints,
                        "quizzesCompleted" to newAttemptsCount,
                        "averageScorePercent" to avgPercent,
                        "dailyPoints" to newDailyPoints,
                        "dailyDateKey" to todayKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val dirSnap = dirRef.get().await()
            if (dirSnap.exists()) {
                val prevBest = dirSnap.getLong("bestScorePercent") ?: 0L
                dirRef.update(
                    mapOf(
                        "quizAttemptsCount" to newAttemptsCount,
                        "averageScorePercent" to avgPercent,
                        "bestScorePercent" to maxOf(prevBest, scorePercent.toLong()),
                        "totalPoints" to newTotalPoints,
                        "lastActiveAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(achievementList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, attemptRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        val ref = db.collection("students").document(uid).collection("notifications").document(notifId)
        return try {
            ref.update(mapOf("read" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun pushStudentNotification(
        title: String,
        body: String,
        type: String,
        targetId: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in."))
        val ref = db.collection("students").document(uid).collection("notifications").document()
        return try {
            ref.set(
                mapOf(
                    "title" to title.take(150),
                    "body" to body.take(480),
                    "type" to type.take(35),
                    "read" to false,
                    "targetId" to targetId.take(120),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }
}
