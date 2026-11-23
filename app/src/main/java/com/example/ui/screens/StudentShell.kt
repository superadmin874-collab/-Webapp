package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.data.model.AdminUser
import com.example.data.model.AppSettings
import com.example.data.model.StudentProfile
import com.example.data.model.Subject
import com.example.data.model.UiState
import com.example.ui.components.DarbarLogoBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.StudentViewModel

enum class StudentTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SUBJECTS("Subjects", Icons.AutoMirrored.Filled.MenuBook),
    QUIZ("AI GK Quiz", Icons.Default.AutoAwesome),
    DOWNLOADS("Downloads", Icons.Default.Download),
    PROFILE("Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentShell(
    student: StudentProfile,
    adminUser: AdminUser?,
    appSettings: AppSettings,
    themeMode: ThemeMode,
    reducedMotion: Boolean,
    isWideLayout: Boolean,
    sessionViewModel: SessionViewModel,
    studentViewModel: StudentViewModel,
    quizViewModel: QuizViewModel,
    onOpenAdminConsole: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val docsState by studentViewModel.documentsState.collectAsStateWithLifecycle()
    val filteredDocs by studentViewModel.filteredDocuments.collectAsStateWithLifecycle()
    val subjects by studentViewModel.subjectsState.collectAsStateWithLifecycle()
    val categories by studentViewModel.categoriesState.collectAsStateWithLifecycle()
    val favorites by studentViewModel.favoritesState.collectAsStateWithLifecycle()
    val downloads by studentViewModel.downloadsState.collectAsStateWithLifecycle()
    val announcements by studentViewModel.announcementsState.collectAsStateWithLifecycle()
    val notifications by studentViewModel.notificationsState.collectAsStateWithLifecycle()
    val readNotifIds by studentViewModel.readNotificationIds.collectAsStateWithLifecycle()
    val searchFilter by studentViewModel.searchFilter.collectAsStateWithLifecycle()
    val previewState by studentViewModel.previewState.collectAsStateWithLifecycle()
    val downloadingDocId by studentViewModel.isDownloadingId.collectAsStateWithLifecycle()
    val actionMessage by studentViewModel.actionMessage.collectAsStateWithLifecycle()

    val quizSession by quizViewModel.sessionState.collectAsStateWithLifecycle()
    val quizCategory by quizViewModel.selectedCategory.collectAsStateWithLifecycle()
    val quizDifficulty by quizViewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val quizCount by quizViewModel.selectedCount.collectAsStateWithLifecycle()
    val leaderboard by quizViewModel.leaderboard.collectAsStateWithLifecycle()
    val quizHistory by quizViewModel.quizHistory.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(StudentTab.HOME) }
    var activeSubject by remember { mutableStateOf<Subject?>(null) }
    var viewerOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }

    val publishedDocs = (docsState as? UiState.Success)?.data ?: emptyList()
    val unreadNotifCount = notifications.count { it.id !in readNotifIds }

    LaunchedEffect(actionMessage) {
        if (actionMessage != null) {
            snackbarHostState.showSnackbar(actionMessage!!)
            studentViewModel.clearMessage()
        }
    }

    if (viewerOpen && previewState.document != null) {
        val doc = previewState.document!!
        val isFav = favorites.any { it.docId == doc.id }
        DocumentViewerScreen(
            previewState = previewState,
            isFavorite = isFav,
            isDownloading = downloadingDocId == doc.id,
            onBack = { viewerOpen = false },
            onDownload = { studentViewModel.downloadDocument(it, appSettings.allowStudentDownloads) },
            onToggleFavorite = { d, fav -> studentViewModel.toggleFavorite(d, fav) }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DarbarLogoBadge(size = 34.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = appSettings.appName,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = NotoSansBengaliFamily),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${student.fullName} • ${student.studentCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (adminUser != null && adminUser.active) {
                        IconButton(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier.size(48.dp).testTag("topbar_admin_switch")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = {
                            val next = when (themeMode) {
                                ThemeMode.LIGHT -> ThemeMode.DARK
                                ThemeMode.DARK -> ThemeMode.LIGHT
                                ThemeMode.SYSTEM -> ThemeMode.DARK
                            }
                            sessionViewModel.setThemeMode(next)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (themeMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    if (appSettings.notificationsEnabled) {
                        IconButton(
                            onClick = { notificationsOpen = true },
                            modifier = Modifier.size(48.dp).testTag("notifications_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge { Text("$unreadNotifCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isWideLayout) {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    StudentTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab && activeSubject == null,
                            onClick = {
                                activeSubject = null
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1) },
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isWideLayout) {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    Spacer(Modifier.height(12.dp))
                    StudentTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab && activeSubject == null,
                            onClick = {
                                activeSubject = null
                                currentTab = tab
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (activeSubject != null) {
                    SubjectDetailScreen(
                        subject = activeSubject!!,
                        categories = categories,
                        allDocs = publishedDocs,
                        favorites = favorites,
                        downloadingDocId = downloadingDocId,
                        onBack = { activeSubject = null },
                        onPreviewDoc = { doc ->
                            studentViewModel.openPreview(context, doc)
                            viewerOpen = true
                        },
                        onDownloadDoc = { doc -> studentViewModel.downloadDocument(doc, appSettings.allowStudentDownloads) },
                        onToggleFavorite = { doc, fav -> studentViewModel.toggleFavorite(doc, fav) }
                    )
                } else {
                    when (currentTab) {
                        StudentTab.HOME -> StudentHomeScreen(
                            student = student,
                            appSettings = appSettings,
                            subjects = subjects,
                            categories = categories,
                            allPublishedDocs = publishedDocs,
                            filteredDocs = filteredDocs,
                            searchFilter = searchFilter,
                            announcements = announcements,
                            favorites = favorites,
                            downloads = downloads,
                            downloadingDocId = downloadingDocId,
                            isWideLayout = isWideLayout,
                            onSearchChange = studentViewModel::updateSearchQuery,
                            onApplyFilters = { s, c, ch, t, d -> studentViewModel.updateFilters(s, c, ch, t, d) },
                            onResetFilters = studentViewModel::resetFilters,
                            onOpenSubject = { activeSubject = it },
                            onOpenQuizTab = { currentTab = StudentTab.QUIZ },
                            onOpenDownloadsTab = { currentTab = StudentTab.DOWNLOADS },
                            onPreviewDoc = { doc ->
                                studentViewModel.openPreview(context, doc)
                                viewerOpen = true
                            },
                            onDownloadDoc = { doc -> studentViewModel.downloadDocument(doc, appSettings.allowStudentDownloads) },
                            onToggleFavorite = { doc, fav -> studentViewModel.toggleFavorite(doc, fav) }
                        )
                        StudentTab.SUBJECTS -> SubjectsDirectoryScreen(
                            subjects = subjects,
                            allDocs = publishedDocs,
                            onOpenSubject = { activeSubject = it }
                        )
                        StudentTab.QUIZ -> AiGkQuizScreen(
                            student = student,
                            sessionState = quizSession,
                            selectedCategory = quizCategory,
                            selectedDifficulty = quizDifficulty,
                            selectedCount = quizCount,
                            leaderboard = leaderboard,
                            quizHistory = quizHistory,
                            todayDate = quizViewModel.todayDateLabel(),
                            reducedMotion = reducedMotion,
                            allowQuiz = appSettings.allowStudentQuiz,
                            onSelectCategory = quizViewModel::selectCategory,
                            onSelectDifficulty = quizViewModel::selectDifficulty,
                            onSelectCount = quizViewModel::selectCount,
                            onStartQuiz = { mode -> quizViewModel.startQuiz(mode, appSettings.allowStudentQuiz) },
                            onSelectAnswer = quizViewModel::selectAnswer,
                            onNextQuestion = { quizViewModel.nextQuestion(student) },
                            onReturnToLobby = quizViewModel::returnToLobby
                        )
                        StudentTab.DOWNLOADS -> DownloadsLibraryScreen(
                            downloads = downloads,
                            favorites = favorites,
                            allPublishedDocs = publishedDocs,
                            onPreviewDoc = { doc ->
                                studentViewModel.openPreview(context, doc)
                                viewerOpen = true
                            }
                        )
                        StudentTab.PROFILE -> StudentProfileScreen(
                            student = student,
                            adminUser = adminUser,
                            themeMode = themeMode,
                            reducedMotion = reducedMotion,
                            onUpdateName = { sessionViewModel.saveStudentName(it) },
                            onThemeChange = sessionViewModel::setThemeMode,
                            onReducedMotionChange = sessionViewModel::setReducedMotion,
                            onOpenAdminConsole = onOpenAdminConsole,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }

    if (notificationsOpen) {
        ModalBottomSheet(onDismissRequest = { notificationsOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (notifications.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Notifications,
                        title = "All Caught Up",
                        subtitle = "New study materials, notices, and Daily GK alerts will appear here."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(340.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            val isRead = notif.id in readNotifIds
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { studentViewModel.markNotificationRead(notif.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(notif.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(formatTimestamp(notif.createdAt), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
