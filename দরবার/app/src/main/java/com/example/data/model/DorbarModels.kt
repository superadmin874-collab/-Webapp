package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AdminRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ADMINISTRATOR("Administrator"),
    MODERATOR("Content Moderator");

    companion object {
        fun safeParse(raw: String?): AdminRole =
            entries.firstOrNull { it.name == raw } ?: ADMINISTRATOR
    }
}

enum class QuizDifficulty(val label: String, val secondsPerQuestion: Int) {
    EASY("Easy", 25),
    MEDIUM("Medium", 20),
    HARD("Hard", 15);

    companion object {
        fun safeParse(raw: String?): QuizDifficulty =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

@Keep
@IgnoreExtraProperties
data class AdminAccount(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.ADMINISTRATOR.name,
    val active: Boolean = true,
    val permissions: List<String> = emptyList(),
    val createdByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.ADMINISTRATOR.name,
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    val claimedUid: String? = null,
    val createdByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubjectItem(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameBn: String = "",
    val description: String = "",
    val iconKey: String = "book",
    val accentHex: String = "#4F46E5",
    val sortOrder: Long = 0L,
    val visible: Boolean = true,
    val createdByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val subjectId: String = "ALL",
    val visible: Boolean = true,
    val sortOrder: Long = 0L,
    val createdByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyDocument(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val chapter: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileExtension: String = "",
    val fileSizeBytes: Long = 0L,
    val chunkCount: Long = 0L,
    val tags: List<String> = emptyList(),
    val featured: Boolean = false,
    val published: Boolean = true,
    val version: Long = 1L,
    val downloadCount: Long = 0L,
    val uploadedByUid: String = "",
    val uploadedByName: String = "",
    val lastEditedByUid: String = "",
    val lastEditedByName: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DocumentChunk(
    @DocumentId val id: String = "",
    val documentId: String = "",
    val chunkIndex: Long = 0L,
    val dataBase64: String = "",
    val published: Boolean = true,
    val uploadedByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DocumentVersion(
    @DocumentId val id: String = "",
    val documentId: String = "",
    val version: Long = 1L,
    val title: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val changeNote: String = "",
    val editedByUid: String = "",
    val editedByName: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val studentCode: String = "",
    val fullName: String = "",
    val active: Boolean = true,
    val downloadCount: Long = 0L,
    val quizAttemptsCount: Long = 0L,
    val totalQuizScore: Long = 0L,
    val totalQuizQuestions: Long = 0L,
    val bestStreak: Long = 0L,
    val achievements: List<String> = emptyList(),
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteRecord(
    @DocumentId val id: String = "",
    val documentId: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val fileExtension: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DownloadRecord(
    @DocumentId val id: String = "",
    val documentId: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileExtension: String = "",
    val fileSizeBytes: Long = 0L,
    val localPath: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val category: String = "",
    val difficulty: String = QuizDifficulty.MEDIUM.name,
    val mode: String = "Category Quiz",
    val totalQuestions: Long = 0L,
    val correctAnswers: Long = 0L,
    val scorePercent: Long = 0L,
    val timeSpentSeconds: Long = 0L,
    val studentName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "SYSTEM",
    val read: Boolean = false,
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentDirectoryEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val studentCode: String = "",
    val displayName: String = "",
    val active: Boolean = true,
    val downloadCount: Long = 0L,
    val quizAttemptsCount: Long = 0L,
    val averageScorePercent: Long = 0L,
    val bestScorePercent: Long = 0L,
    val totalPoints: Long = 0L,
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    @DocumentId val id: String = "",
    val category: String = "",
    val difficulty: String = QuizDifficulty.MEDIUM.name,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val sourceNote: String = "",
    val aiGenerated: Boolean = true,
    val approved: Boolean = true,
    val createdByUid: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val published: Boolean = true,
    val attachmentDocId: String? = null,
    val attachmentFileName: String? = null,
    val attachmentMimeType: String? = null,
    val publishDateLabel: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminActivityLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val actionType: String = "",
    val summary: String = "",
    val targetId: String = "",
    val adminUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val appName: String = "দরবার",
    val tagline: String = "Your Digital Learning & Knowledge Hub",
    val defaultThemeMode: String = "SYSTEM",
    val maxUploadSizeMb: Long = 5L,
    val allowStudentQuiz: Boolean = true,
    val allowStudentDownloads: Boolean = true,
    val dailyChallengeEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val studentCode: String = "",
    val displayName: String = "",
    val totalPoints: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val averageScorePercent: Long = 0L,
    val dailyPoints: Long = 0L,
    val dailyDateKey: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object DefaultCatalog {
    const val SUPER_ADMIN_EMAIL = "realmepx36@gmail.com"

    val defaultSubjects = listOf(
        SubjectSeed("bengali", "Bengali", "বাংলা", "সাহিত্য, ব্যাকরণ, রচনা এবং বোর্ড পরীক্ষার নোটস", "book", "#4F46E5", 1L),
        SubjectSeed("english", "English", "ইংরেজি", "Grammar, Comprehension, Writing Skills & Literature", "language", "#7C3AED", 2L),
        SubjectSeed("physics", "Physics", "পদার্থবিদ্যা", "Mechanics, Optics, Electricity, Magnetism & Modern Physics", "science", "#2563EB", 3L),
        SubjectSeed("chemistry", "Chemistry", "রসায়নবিদ্যা", "Physical, Organic, Inorganic Chemistry & Reactions", "chemistry", "#0D9488", 4L),
        SubjectSeed("mathematics", "Mathematics", "গণিত", "Algebra, Calculus, Geometry, Trigonometry & Formula Sheets", "calculate", "#D97706", 5L),
        SubjectSeed("biology", "Biology", "জীববিদ্যা", "Botany, Zoology, Genetics, Diagrams & Life Sciences", "biotech", "#059669", 6L)
    )

    val defaultCategoryNames = listOf(
        "Chapter-wise Notes",
        "PDF Study Materials",
        "Images and Diagrams",
        "Important Questions",
        "Suggestion Questions",
        "Previous Year Questions",
        "Question Papers",
        "Other Documents"
    )

    val gkCategories = listOf(
        "Indian General Knowledge",
        "World General Knowledge",
        "History",
        "Geography",
        "Indian Constitution",
        "Science and Technology",
        "Sports",
        "Current Affairs",
        "West Bengal General Knowledge",
        "Important Personalities",
        "Space and Astronomy",
        "Environment"
    )

    val allAdminPermissions = listOf(
        "MANAGE_DOCS",
        "MANAGE_SUBJECTS",
        "MANAGE_CATEGORIES",
        "MANAGE_STUDENTS",
        "MANAGE_QUIZ",
        "MANAGE_ANNOUNCEMENTS",
        "VIEW_ANALYTICS",
        "MANAGE_ADMINS",
        "MANAGE_SETTINGS"
    )

    data class SubjectSeed(
        val id: String,
        val nameEn: String,
        val nameBn: String,
        val description: String,
        val iconKey: String,
        val accentHex: String,
        val sortOrder: Long
    )
}
