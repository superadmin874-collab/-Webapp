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
    ADMIN("Administrator"),
    MODERATOR("Moderator");

    companion object {
        fun fromRaw(raw: String?): AdminRole =
            entries.firstOrNull { it.name == raw } ?: ADMIN
    }
}

enum class QuizDifficulty(val label: String, val labelBn: String) {
    EASY("Easy", "সহজ"),
    MEDIUM("Medium", "মধ্যম"),
    HARD("Hard", "কঠিন");

    companion object {
        fun fromRaw(raw: String?): QuizDifficulty =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

object AdminPermissions {
    const val MANAGE_DOCUMENTS = "manage_documents"
    const val MANAGE_SUBJECTS = "manage_subjects"
    const val MANAGE_CATEGORIES = "manage_categories"
    const val MANAGE_STUDENTS = "manage_students"
    const val MANAGE_QUIZZES = "manage_quizzes"
    const val MANAGE_ANNOUNCEMENTS = "manage_announcements"
    const val VIEW_ANALYTICS = "view_analytics"
    const val MANAGE_SETTINGS = "manage_settings"

    val ALL_ADMIN_PERMISSIONS = listOf(
        MANAGE_DOCUMENTS,
        MANAGE_SUBJECTS,
        MANAGE_CATEGORIES,
        MANAGE_STUDENTS,
        MANAGE_QUIZZES,
        MANAGE_ANNOUNCEMENTS,
        VIEW_ANALYTICS,
        MANAGE_SETTINGS
    )

    fun labelFor(perm: String): String = when (perm) {
        MANAGE_DOCUMENTS -> "Documents & Uploads"
        MANAGE_SUBJECTS -> "Subject Manager"
        MANAGE_CATEGORIES -> "Category Manager"
        MANAGE_STUDENTS -> "Student Manager"
        MANAGE_QUIZZES -> "AI Quiz Manager"
        MANAGE_ANNOUNCEMENTS -> "Announcements"
        VIEW_ANALYTICS -> "Analytics & Reports"
        MANAGE_SETTINGS -> "General Settings"
        else -> perm
    }
}

@Keep
@IgnoreExtraProperties
data class AdminUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.ADMIN.name,
    val active: Boolean = true,
    val permissions: List<String> = emptyList(),
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedRole(): AdminRole = AdminRole.fromRaw(role)
    fun hasPermission(perm: String): Boolean =
        active && (resolvedRole() == AdminRole.SUPER_ADMIN || permissions.contains("all") || permissions.contains(perm))
}

@Keep
@IgnoreExtraProperties
data class AdminInvite(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.ADMIN.name,
    val active: Boolean = true,
    val permissions: List<String> = emptyList(),
    val invitedByUid: String = "",
    val claimedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val titleEn: String = "",
    val titleBn: String = "",
    val description: String = "",
    val orderIndex: Long = 0L,
    val visible: Boolean = true,
    val accentHex: String = "#6328E0",
    val iconKey: String = "book",
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameBn: String = "",
    val description: String = "",
    val orderIndex: Long = 0L,
    val visible: Boolean = true,
    val createdBy: String = "",
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
    val tags: List<String> = emptyList(),
    val fileName: String = "",
    val fileType: String = "PDF",
    val mimeType: String = "application/pdf",
    val fileSizeBytes: Long = 0L,
    val chunkCount: Long = 0L,
    val externalUrl: String? = null,
    val published: Boolean = true,
    val featured: Boolean = false,
    val version: Long = 1L,
    val downloadCount: Long = 0L,
    val uploadedByUid: String = "",
    val uploadedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DocumentVersion(
    @DocumentId val id: String = "",
    val version: Long = 1L,
    val title: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val changeNote: String = "",
    val editedByUid: String = "",
    val editedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val studentCode: String = "",
    val fullName: String = "",
    val disabled: Boolean = false,
    val downloadsCount: Long = 0L,
    val quizzesTaken: Long = 0L,
    val totalQuizScore: Long = 0L,
    val totalQuestionsAnswered: Long = 0L,
    val achievements: List<String> = emptyList(),
    @ServerTimestamp val lastActiveAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageAccuracyPercent: Int
        get() = if (totalQuestionsAnswered <= 0L) 0
        else ((totalQuizScore.toDouble() / totalQuestionsAnswered.toDouble()) * 100.0).toInt().coerceIn(0, 100)
}

@Keep
@IgnoreExtraProperties
data class StudentFavorite(
    @DocumentId val id: String = "",
    val docId: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val fileType: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentDownload(
    @DocumentId val id: String = "",
    val docId: String = "",
    val title: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val fileName: String = "",
    val fileType: String = "",
    val fileSizeBytes: Long = 0L,
    val localPath: String? = null,
    @ServerTimestamp val downloadedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val category: String = "",
    val difficulty: String = QuizDifficulty.MEDIUM.name,
    val mode: String = "Category",
    val totalQuestions: Long = 0L,
    val correctCount: Long = 0L,
    val scorePercent: Long = 0L,
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentNotificationRead(
    @DocumentId val id: String = "",
    val notifId: String = "",
    val read: Boolean = true,
    @ServerTimestamp val readAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    @DocumentId val id: String = "",
    val category: String = "",
    val difficulty: String = QuizDifficulty.MEDIUM.name,
    val questionText: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Long = 0L,
    val explanation: String = "",
    val source: String = "",
    val factDate: String = "",
    val aiGenerated: Boolean = false,
    val verifiedByAdmin: Boolean = false,
    val approved: Boolean = false,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val uid: String = "",
    val studentCode: String = "",
    val fullName: String = "",
    val totalScore: Long = 0L,
    val quizzesTaken: Long = 0L,
    val bestPercent: Long = 0L,
    val dailyScore: Long = 0L,
    val dailyDate: String = "",
    val active: Boolean = true,
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
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val authorUid: String = "",
    val authorName: String = "",
    val publishDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val adminRole: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val appName: String = "দরবার",
    val tagline: String = "Your Digital Learning & Knowledge Hub",
    val defaultTheme: String = "SYSTEM",
    val maxUploadMb: Long = 5L,
    val allowStudentQuiz: Boolean = true,
    val allowStudentDownloads: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val requireStrictVerification: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BroadcastNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "SYSTEM",
    val targetId: String = "",
    val active: Boolean = true,
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object DarbarCurriculumDefaults {
    const val SUPER_ADMIN_EMAIL = "superadmin874@gmail.com"

    val DEFAULT_SUBJECTS = listOf(
        Subject(
            id = "bengali",
            titleEn = "Bengali",
            titleBn = "বাংলা",
            description = "সাহিত্য, ব্যাকরণ, রচনা ও পাঠ্যবইয়ের অধ্যায়ভিত্তিক নোট",
            orderIndex = 1L,
            visible = true,
            accentHex = "#7C3AED",
            iconKey = "menu_book"
        ),
        Subject(
            id = "english",
            titleEn = "English",
            titleBn = "ইংরেজি",
            description = "Literature, Grammar, Writing Skills & Comprehension",
            orderIndex = 2L,
            visible = true,
            accentHex = "#2563EB",
            iconKey = "translate"
        ),
        Subject(
            id = "physics",
            titleEn = "Physics",
            titleBn = "পদার্থবিদ্যা",
            description = "Mechanics, Optics, Electricity, Modern Physics & Diagrams",
            orderIndex = 3L,
            visible = true,
            accentHex = "#0891B2",
            iconKey = "bolt"
        ),
        Subject(
            id = "chemistry",
            titleEn = "Chemistry",
            titleBn = "রসায়নবিদ্যা",
            description = "Organic, Inorganic, Physical Chemistry & Equations",
            orderIndex = 4L,
            visible = true,
            accentHex = "#D97706",
            iconKey = "science"
        ),
        Subject(
            id = "mathematics",
            titleEn = "Mathematics",
            titleBn = "গণিত",
            description = "Algebra, Calculus, Geometry, Trigonometry & Practice Sets",
            orderIndex = 5L,
            visible = true,
            accentHex = "#DC2626",
            iconKey = "calculate"
        ),
        Subject(
            id = "biology",
            titleEn = "Biology",
            titleBn = "জীববিদ্যা",
            description = "Botany, Zoology, Cell Biology, Genetics & Diagrams",
            orderIndex = 6L,
            visible = true,
            accentHex = "#059669",
            iconKey = "biotech"
        )
    )

    val DEFAULT_CATEGORIES = listOf(
        Category(id = "chapter_notes", name = "Chapter-wise Notes", nameBn = "অধ্যায়ভিত্তিক নোট", description = "Structured chapter notes", orderIndex = 1L, visible = true),
        Category(id = "pdf_materials", name = "PDF Study Materials", nameBn = "পিডিএফ স্টাডি মেটেরিয়াল", description = "Complete PDF guides and handouts", orderIndex = 2L, visible = true),
        Category(id = "images_diagrams", name = "Images and Diagrams", nameBn = "ছবি ও ডায়াগ্রাম", description = "Labeled diagrams, charts, and visual notes", orderIndex = 3L, visible = true),
        Category(id = "important_questions", name = "Important Questions", nameBn = "গুরুত্বপূর্ণ প্রশ্ন", description = "High-priority exam questions", orderIndex = 4L, visible = true),
        Category(id = "suggestion_questions", name = "Suggestion Questions", nameBn = "সাজেশন প্রশ্ন", description = "Curated exam suggestions", orderIndex = 5L, visible = true),
        Category(id = "previous_year", name = "Previous Year Questions", nameBn = "বিগত বছরের প্রশ্ন", description = "Past board and competitive papers", orderIndex = 6L, visible = true),
        Category(id = "question_papers", name = "Question Papers", nameBn = "প্রশ্নপত্র", description = "Practice tests and model papers", orderIndex = 7L, visible = true),
        Category(id = "other_documents", name = "Other Documents", nameBn = "অন্যান্য নথি", description = "Presentations, spreadsheets, and reference files", orderIndex = 8L, visible = true)
    )

    val GK_CATEGORIES = listOf(
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
}
