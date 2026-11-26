package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.SessionPreferences
import com.example.core.attemptAutoSignIn
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOut
import com.example.data.model.UiState
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiQuizRepository
import com.example.data.repository.ContentRepository
import com.example.data.repository.StudentRepository
import com.example.ui.components.EmptyStateCard
import com.example.ui.screens.AdminDashboardShell
import com.example.ui.screens.AiGkQuizScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DocumentViewerModal
import com.example.ui.screens.DownloadsAndFavoritesScreen
import com.example.ui.screens.GlobalSearchModal
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.screens.StudentNameGateScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.SubjectsHubScreen
import com.example.ui.theme.DorbarTheme
import com.example.ui.viewmodel.DorbarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class StudentTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SUBJECTS("Subjects", Icons.Default.MenuBook),
    QUIZ("GK Quiz", Icons.Default.AutoAwesome),
    DOWNLOADS("Library", Icons.Default.DownloadDone),
    PROFILE("Profile", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)

        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as Application
            val scope = rememberCoroutineScope()
            val credentialManager = remember(context) { CredentialManager.create(context) }
            val currentUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            LaunchedEffect(Unit) {
                attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {},
                    onUnauthenticated = {},
                    scope = scope
                )
            }

            if (currentUser == null) {
                DorbarTheme {
                    AuthScreen(onAuthSuccess = {})
                }
            } else {
                val user = currentUser!!
                key(user.uid) {
                    val vm: DorbarViewModel = viewModel(
                        key = user.uid,
                        factory = viewModelFactory {
                            initializer {
                                val db = FirebaseFirestore.getInstance(
                                    app.getString(R.string.firestore_database_id)
                                )
                                DorbarViewModel(
                                    currentUserId = user.uid,
                                    userEmail = user.email,
                                    emailVerified = user.isEmailVerified,
                                    userDisplayName = user.displayName,
                                    contentRepo = ContentRepository(db),
                                    studentRepo = StudentRepository(db),
                                    adminRepo = AdminRepository(db),
                                    aiQuizRepo = AiQuizRepository(),
                                    sessionPrefs = SessionPreferences(app)
                                )
                            }
                        }
                    )

                    val localPrefs by vm.localPrefs.collectAsStateWithLifecycle()
                    val appSettings by vm.appSettings.collectAsStateWithLifecycle()
                    val isAdminAuthorized by vm.isAdminAuthorized.collectAsStateWithLifecycle()
                    val isSuperAdmin by vm.isSuperAdmin.collectAsStateWithLifecycle()
                    val studentProfileState by vm.studentProfile.collectAsStateWithLifecycle()
                    val subjectsState by vm.subjectsState.collectAsStateWithLifecycle()
                    val categoriesState by vm.categoriesState.collectAsStateWithLifecycle()
                    val documentsState by vm.documentsState.collectAsStateWithLifecycle()
                    val announcementsState by vm.announcementsState.collectAsStateWithLifecycle()
                    val quizQuestionsState by vm.quizQuestionsState.collectAsStateWithLifecycle()
                    val favorites by vm.favorites.collectAsStateWithLifecycle()
                    val downloads by vm.downloads.collectAsStateWithLifecycle()
                    val quizAttempts by vm.quizAttempts.collectAsStateWithLifecycle()
                    val notifications by vm.notifications.collectAsStateWithLifecycle()
                    val leaderboard by vm.leaderboard.collectAsStateWithLifecycle()
                    val allAdmins by vm.allAdmins.collectAsStateWithLifecycle()
                    val adminInvites by vm.adminInvites.collectAsStateWithLifecycle()
                    val studentDirectory by vm.studentDirectory.collectAsStateWithLifecycle()
                    val activityLogs by vm.activityLogs.collectAsStateWithLifecycle()
                    val statusMessage by vm.statusMessage.collectAsStateWithLifecycle()
                    val previewState by vm.previewState.collectAsStateWithLifecycle()
                    val quizState by vm.quizState.collectAsStateWithLifecycle()
                    val aiDraftQuestions by vm.aiDraftQuestions.collectAsStateWithLifecycle()
                    val aiGenerating by vm.aiGeneratingForAdmin.collectAsStateWithLifecycle()
                    val uploadProgress by vm.uploadProgress.collectAsStateWithLifecycle()
                    val docVersions by vm.docVersions.collectAsStateWithLifecycle()

                    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
                    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
                    val documents = (documentsState as? UiState.Success)?.data.orEmpty()
                    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
                    val quizQuestions = (quizQuestionsState as? UiState.Success)?.data.orEmpty()

                    var activeTab by remember { mutableStateOf(StudentTab.HOME) }
                    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
                    var showGlobalSearch by remember { mutableStateOf(false) }

                    val snackbarHostState = remember { SnackbarHostState() }
                    LaunchedEffect(statusMessage) {
                        val msg = statusMessage
                        if (!msg.isNullOrBlank()) {
                            snackbarHostState.showSnackbar(msg)
                            vm.clearStatusMessage()
                        }
                    }

                    val effectiveTheme = if (localPrefs.themeMode == "SYSTEM") {
                        appSettings.defaultThemeMode
                    } else {
                        localPrefs.themeMode
                    }

                    val performLogout: () -> Unit = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            onBeforeFirebaseSignOut = {
                                vm.logAdminLogout()
                                vm.clearSessionOnSignOut()
                            }
                        )
                    }

                    DorbarTheme(themeMode = effectiveTheme) {
                        val isInAdminMode = isAdminAuthorized && localPrefs.preferredWorkspaceMode == "ADMIN"

                        if (previewState.document != null) {
                            BackHandler { vm.closeDocumentPreview() }
                        } else if (showGlobalSearch) {
                            BackHandler { showGlobalSearch = false }
                        } else if (quizState.active) {
                            BackHandler { vm.exitQuiz() }
                        } else if (isInAdminMode) {
                            BackHandler { vm.setWorkspaceMode("STUDENT") }
                        } else if (activeTab != StudentTab.HOME) {
                            BackHandler { activeTab = StudentTab.HOME }
                        }

                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { rootPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(rootPadding)
                            ) {
                                if (isInAdminMode) {
                                    AdminDashboardShell(
                                        appSettings = appSettings,
                                        currentAdminName = vm.currentAdminName(),
                                        isSuperAdmin = isSuperAdmin,
                                        reducedMotion = localPrefs.reducedMotion,
                                        subjects = subjects,
                                        categories = categories,
                                        documents = documents,
                                        students = studentDirectory,
                                        admins = allAdmins,
                                        invites = adminInvites,
                                        quizQuestions = quizQuestions,
                                        aiDraftQuestions = aiDraftQuestions,
                                        aiGenerating = aiGenerating,
                                        announcements = announcements,
                                        activityLogs = activityLogs,
                                        uploadProgress = uploadProgress,
                                        docVersions = docVersions,
                                        onSwitchToStudent = { vm.setWorkspaceMode("STUDENT") },
                                        onLogout = performLogout,
                                        onPostMessage = vm::postMessage,
                                        onOpenDocumentPreview = { doc ->
                                            vm.openDocumentPreview(context, doc, isAdminView = true)
                                        },
                                        onUploadBatch = vm::uploadDocumentsBatch,
                                        onUpdateDocument = vm::updateDocument,
                                        onLoadDocVersions = vm::loadDocumentVersions,
                                        onDeleteDocument = vm::deleteDocument,
                                        onBulkPublish = vm::bulkPublishDocuments,
                                        onBulkDelete = vm::bulkDeleteDocuments,
                                        onSaveSubject = vm::saveSubject,
                                        onSaveCategory = vm::saveCategory,
                                        onDeleteCategory = vm::deleteCategory,
                                        onSetStudentActive = vm::setStudentActive,
                                        onRemoveStudent = vm::removeStudent,
                                        onGenerateAiQuestions = vm::generateAiQuestionsForAdmin,
                                        onApproveDraftQuestion = vm::approveDraftQuestion,
                                        onDiscardDraftQuestion = vm::discardDraftQuestion,
                                        onSaveQuizQuestion = vm::saveQuizQuestion,
                                        onDeleteQuizQuestion = vm::deleteQuizQuestion,
                                        onSaveAnnouncement = vm::saveAnnouncement,
                                        onDeleteAnnouncement = vm::deleteAnnouncement,
                                        onInviteAdmin = vm::inviteAdmin,
                                        onUpdateAdmin = vm::updateAdmin,
                                        onRemoveAdmin = vm::removeAdmin,
                                        onRemoveInvite = vm::removeInvite,
                                        onUpdateAppSettings = vm::updateAppSettings
                                    )
                                } else {
                                    when (val state = studentProfileState) {
                                        UiState.Loading -> {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize().safeDrawingPadding()
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        is UiState.Error -> {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.widthIn(max = 460.dp)
                                                ) {
                                                    EmptyStateCard(
                                                        icon = Icons.Default.Block,
                                                        title = "Connection Issue",
                                                        subtitle = state.message
                                                    )
                                                    if (isAdminAuthorized) {
                                                        Button(
                                                            onClick = { vm.setWorkspaceMode("ADMIN") },
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text("Open Admin Control Center")
                                                        }
                                                    }
                                                    OutlinedButton(
                                                        onClick = performLogout,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Sign Out")
                                                    }
                                                }
                                            }
                                        }
                                        is UiState.Success -> {
                                            val student = state.data
                                            when {
                                                student == null -> {
                                                    StudentNameGateScreen(
                                                        cachedName = localPrefs.cachedStudentName,
                                                        isAdminAuthorized = isAdminAuthorized,
                                                        onSubmitName = { name -> vm.registerOrUpdateStudentName(name) },
                                                        onOpenAdminPanel = { vm.setWorkspaceMode("ADMIN") },
                                                        onSignOut = performLogout
                                                    )
                                                }
                                                !student.active -> {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)
                                                    ) {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                                            modifier = Modifier.widthIn(max = 460.dp)
                                                        ) {
                                                            EmptyStateCard(
                                                                icon = Icons.Default.Block,
                                                                title = "Student Access Paused",
                                                                subtitle = "Your account (${student.studentCode}) has been disabled by an administrator."
                                                            )
                                                            OutlinedButton(
                                                                onClick = performLogout,
                                                                shape = RoundedCornerShape(14.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text("Log Out")
                                                            }
                                                        }
                                                    }
                                                }
                                                appSettings.maintenanceMode && !isAdminAuthorized -> {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)
                                                    ) {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                                            modifier = Modifier.widthIn(max = 460.dp)
                                                        ) {
                                                            EmptyStateCard(
                                                                icon = Icons.Default.Build,
                                                                title = "${appSettings.appName} Maintenance",
                                                                subtitle = "Administrators are updating study materials. Please check back shortly."
                                                            )
                                                            OutlinedButton(
                                                                onClick = performLogout,
                                                                shape = RoundedCornerShape(14.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text("Log Out")
                                                            }
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    StudentWorkspaceShell(
                                                        activeTab = activeTab,
                                                        onSelectTab = { activeTab = it },
                                                        appName = appSettings.appName,
                                                        tagline = appSettings.tagline,
                                                        quizEnabled = appSettings.allowStudentQuiz,
                                                        dailyEnabled = appSettings.dailyChallengeEnabled,
                                                        student = student,
                                                        localPrefs = localPrefs,
                                                        subjects = subjects,
                                                        categories = categories,
                                                        documents = documents,
                                                        announcements = announcements,
                                                        favorites = favorites,
                                                        downloads = downloads,
                                                        notifications = if (appSettings.notificationsEnabled && localPrefs.notificationsEnabled) notifications else emptyList(),
                                                        quizAttempts = quizAttempts,
                                                        leaderboard = leaderboard,
                                                        quizState = quizState,
                                                        selectedSubjectId = selectedSubjectId,
                                                        isAdminAuthorized = isAdminAuthorized,
                                                        onToggleTheme = {
                                                            val next = if (effectiveTheme == "DARK") "LIGHT" else "DARK"
                                                            vm.setThemeMode(next)
                                                        },
                                                        onOpenSearch = { showGlobalSearch = true },
                                                        onSelectSubjectAndOpenTab = { subj ->
                                                            selectedSubjectId = subj.id
                                                            activeTab = StudentTab.SUBJECTS
                                                        },
                                                        onSelectSubjectId = { selectedSubjectId = it },
                                                        onOpenDocument = { doc ->
                                                            vm.openDocumentPreview(context, doc, isAdminView = false)
                                                        },
                                                        onDownloadDocument = { doc ->
                                                            vm.downloadDocument(context, doc, openAfter = false)
                                                        },
                                                        onToggleFavorite = vm::toggleFavorite,
                                                        onMarkNotificationRead = vm::markNotificationRead,
                                                        onStartQuiz = vm::startQuiz,
                                                        onSelectQuizAnswer = vm::submitQuizAnswer,
                                                        onNextQuizQuestion = vm::advanceQuizQuestion,
                                                        onExitQuiz = vm::exitQuiz,
                                                        onMissingFileMessage = vm::postMessage,
                                                        onUpdateName = { newName -> vm.registerOrUpdateStudentName(newName) },
                                                        onSetThemeMode = vm::setThemeMode,
                                                        onSetReducedMotion = vm::setReducedMotion,
                                                        onSetNotificationsEnabled = vm::setNotificationsEnabled,
                                                        onOpenAdminPanel = { vm.setWorkspaceMode("ADMIN") },
                                                        onSignOut = performLogout
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showGlobalSearch) {
                                    GlobalSearchModal(
                                        subjects = subjects,
                                        categories = categories,
                                        documents = documents,
                                        favorites = favorites,
                                        onDismiss = { showGlobalSearch = false },
                                        onOpenDocument = { doc ->
                                            vm.openDocumentPreview(context, doc, isAdminView = isInAdminMode)
                                        },
                                        onDownloadDocument = { doc ->
                                            vm.downloadDocument(context, doc, openAfter = false)
                                        },
                                        onToggleFavorite = vm::toggleFavorite
                                    )
                                }

                                if (previewState.document != null) {
                                    val previewDoc = previewState.document!!
                                    val isFav = favorites.any { it.documentId == previewDoc.id }
                                    DocumentViewerModal(
                                        previewState = previewState,
                                        isFavorite = isFav,
                                        onClose = vm::closeDocumentPreview,
                                        onDownload = { openAfter ->
                                            vm.downloadDocument(context, previewDoc, openAfter = openAfter)
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(previewDoc, isFav) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentWorkspaceShell(
    activeTab: StudentTab,
    onSelectTab: (StudentTab) -> Unit,
    appName: String,
    tagline: String,
    quizEnabled: Boolean,
    dailyEnabled: Boolean,
    student: com.example.data.model.StudentProfile,
    localPrefs: com.example.core.LocalSessionPrefs,
    subjects: List<com.example.data.model.SubjectItem>,
    categories: List<com.example.data.model.CategoryItem>,
    documents: List<com.example.data.model.StudyDocument>,
    announcements: List<com.example.data.model.Announcement>,
    favorites: List<com.example.data.model.FavoriteRecord>,
    downloads: List<com.example.data.model.DownloadRecord>,
    notifications: List<com.example.data.model.StudentNotification>,
    quizAttempts: List<com.example.data.model.QuizAttempt>,
    leaderboard: List<com.example.data.model.LeaderboardEntry>,
    quizState: com.example.ui.viewmodel.QuizSessionState,
    selectedSubjectId: String?,
    isAdminAuthorized: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectSubjectAndOpenTab: (com.example.data.model.SubjectItem) -> Unit,
    onSelectSubjectId: (String?) -> Unit,
    onOpenDocument: (com.example.data.model.StudyDocument) -> Unit,
    onDownloadDocument: (com.example.data.model.StudyDocument) -> Unit,
    onToggleFavorite: (com.example.data.model.StudyDocument, Boolean) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onStartQuiz: (String, com.example.data.model.QuizDifficulty, Int, String, Boolean) -> Unit,
    onSelectQuizAnswer: (Int) -> Unit,
    onNextQuizQuestion: () -> Unit,
    onExitQuiz: () -> Unit,
    onMissingFileMessage: (String) -> Unit,
    onUpdateName: (String) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    val tabContent: @Composable (Modifier) -> Unit = { contentMod ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = contentMod.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
            ) {
                when (activeTab) {
                    StudentTab.HOME -> StudentHomeScreen(
                        appName = appName,
                        tagline = tagline,
                        student = student,
                        subjects = subjects,
                        documents = documents,
                        announcements = announcements,
                        favorites = favorites,
                        downloads = downloads,
                        notifications = notifications,
                        isAdminAuthorized = isAdminAuthorized,
                        themeMode = localPrefs.themeMode,
                        onToggleTheme = onToggleTheme,
                        onOpenSearch = onOpenSearch,
                        onSelectSubject = onSelectSubjectAndOpenTab,
                        onOpenDocument = onOpenDocument,
                        onDownloadDocument = onDownloadDocument,
                        onToggleFavorite = onToggleFavorite,
                        onOpenAiQuiz = { onSelectTab(StudentTab.QUIZ) },
                        onOpenProfile = { onSelectTab(StudentTab.PROFILE) },
                        onOpenAdminPanel = onOpenAdminPanel,
                        onMarkNotificationRead = onMarkNotificationRead
                    )
                    StudentTab.SUBJECTS -> SubjectsHubScreen(
                        subjects = subjects,
                        categories = categories,
                        documents = documents,
                        favorites = favorites,
                        selectedSubjectId = selectedSubjectId,
                        onSelectSubject = onSelectSubjectId,
                        onOpenDocument = onOpenDocument,
                        onDownloadDocument = onDownloadDocument,
                        onToggleFavorite = onToggleFavorite
                    )
                    StudentTab.QUIZ -> AiGkQuizScreen(
                        quizEnabled = quizEnabled,
                        dailyEnabled = dailyEnabled,
                        student = student,
                        quizState = quizState,
                        attempts = quizAttempts,
                        leaderboard = leaderboard,
                        reducedMotion = localPrefs.reducedMotion,
                        onStartQuiz = onStartQuiz,
                        onSelectAnswer = onSelectQuizAnswer,
                        onNextQuestion = onNextQuizQuestion,
                        onExitQuiz = onExitQuiz
                    )
                    StudentTab.DOWNLOADS -> DownloadsAndFavoritesScreen(
                        downloads = downloads,
                        favorites = favorites,
                        allDocuments = documents,
                        onOpenDocument = onOpenDocument,
                        onToggleFavorite = onToggleFavorite,
                        onMissingFileMessage = onMissingFileMessage
                    )
                    StudentTab.PROFILE -> StudentProfileScreen(
                        student = student,
                        localPrefs = localPrefs,
                        isAdminAuthorized = isAdminAuthorized,
                        onUpdateName = onUpdateName,
                        onSetThemeMode = onSetThemeMode,
                        onSetReducedMotion = onSetReducedMotion,
                        onSetNotificationsEnabled = onSetNotificationsEnabled,
                        onOpenAdminPanel = onOpenAdminPanel,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        if (useRail) {
            Row(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StudentTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = activeTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("student_rail_${tab.name.lowercase()}")
                        )
                    }
                }
                tabContent(Modifier.weight(1f))
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        StudentTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { onSelectTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("student_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                tabContent(Modifier.padding(innerPadding))
            }
        }
    }
}
