package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.core.DocumentStorageHelper
import com.example.data.model.ActivityLog
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.AppSettings
import com.example.data.model.StudyDocument
import com.example.ui.components.AnimatedStatCounter
import com.example.ui.components.DarbarLogoBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FileTypeDonutChart
import com.example.ui.components.SubjectBarChart
import com.example.ui.components.darbarHeroBrush
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.AdminAnalyticsSummary
import com.example.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    DOCUMENTS("Document Manager", Icons.Default.FolderCopy),
    UPLOAD("Upload Materials", Icons.Default.CloudUpload),
    SUBJECTS("Subject Manager", Icons.Default.Book),
    CATEGORIES("Category Manager", Icons.Default.Category),
    STUDENTS("Student Manager", Icons.Default.People),
    QUIZZES("AI Quiz Manager", Icons.Default.AutoAwesome),
    ANNOUNCEMENTS("Announcement Manager", Icons.Default.Campaign),
    ANALYTICS("Analytics & Reports", Icons.Default.Analytics),
    STORAGE("Storage Manager", Icons.Default.Storage),
    ADMINS("Admin Management", Icons.Default.AdminPanelSettings),
    LOGS("Activity Logs", Icons.Default.History),
    SETTINGS("App Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminShell(
    adminUser: AdminUser,
    appSettings: AppSettings,
    reducedMotion: Boolean,
    isWideLayout: Boolean,
    adminViewModel: AdminViewModel,
    onSwitchToStudentView: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val allDocuments by adminViewModel.allDocuments.collectAsStateWithLifecycle()
    val allSubjects by adminViewModel.allSubjects.collectAsStateWithLifecycle()
    val allCategories by adminViewModel.allCategories.collectAsStateWithLifecycle()
    val allStudents by adminViewModel.allStudents.collectAsStateWithLifecycle()
    val allQuestions by adminViewModel.allQuizQuestions.collectAsStateWithLifecycle()
    val allAnnouncements by adminViewModel.allAnnouncements.collectAsStateWithLifecycle()
    val allAdmins by adminViewModel.allAdmins.collectAsStateWithLifecycle()
    val adminInvites by adminViewModel.adminInvites.collectAsStateWithLifecycle()
    val activityLogs by adminViewModel.activityLogs.collectAsStateWithLifecycle()
    val analytics by adminViewModel.analytics.collectAsStateWithLifecycle()
    val uploadQueue by adminViewModel.uploadQueue.collectAsStateWithLifecycle()
    val isUploading by adminViewModel.isUploading.collectAsStateWithLifecycle()
    val aiDrafts by adminViewModel.aiDrafts.collectAsStateWithLifecycle()
    val isGeneratingAi by adminViewModel.isGeneratingAi.collectAsStateWithLifecycle()
    val versionHistory by adminViewModel.versionHistory.collectAsStateWithLifecycle()
    val adminMessage by adminViewModel.adminMessage.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }

    BackHandler(enabled = currentSection != AdminSection.DASHBOARD) {
        currentSection = AdminSection.DASHBOARD
    }

    LaunchedEffect(adminMessage) {
        if (adminMessage != null) {
            snackbarHostState.showSnackbar(adminMessage!!)
            adminViewModel.clearMessage()
        }
    }

    val drawerBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                DarbarLogoBadge(size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "দরবার Admin",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = NotoSansBengaliFamily),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text("${adminUser.displayName} • ${adminUser.resolvedRole().label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            AdminSection.entries.forEach { section ->
                NavigationDrawerItem(
                    label = { Text(section.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(section.icon, contentDescription = null) },
                    selected = currentSection == section,
                    onClick = {
                        currentSection = section
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.testTag("admin_nav_${section.name.lowercase()}")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            NavigationDrawerItem(
                label = { Text("Student View") },
                icon = { Icon(Icons.Default.School, contentDescription = null) },
                selected = false,
                onClick = onSwitchToStudentView,
                modifier = Modifier.testTag("admin_switch_student_view")
            )
            NavigationDrawerItem(
                label = { Text("Logout") },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.testTag("admin_logout_item")
            )
        }
    }

    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(currentSection.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Logged in as ${adminUser.resolvedRole().label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        if (!isWideLayout) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.size(48.dp).testTag("admin_drawer_toggle")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Open admin menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSwitchToStudentView,
                            modifier = Modifier.size(48.dp).testTag("admin_topbar_student_btn")
                        ) {
                            Icon(Icons.Default.School, contentDescription = "Switch to Student View")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (currentSection) {
                    AdminSection.DASHBOARD -> AdminDashboardSection(
                        adminUser = adminUser,
                        analytics = analytics,
                        recentLogs = activityLogs.take(8),
                        reducedMotion = reducedMotion,
                        onNavigate = { currentSection = it }
                    )
                    AdminSection.DOCUMENTS -> AdminDocumentManagerSection(
                        adminUser = adminUser,
                        documents = allDocuments,
                        subjects = allSubjects,
                        categories = allCategories,
                        versionHistory = versionHistory,
                        maxUploadMb = appSettings.maxUploadMb,
                        onLoadVersions = adminViewModel::loadVersionHistory,
                        onEditDocument = { doc, title, desc, subj, cat, chap, tags, pub, feat, repl, note ->
                            adminViewModel.editDocument(adminUser, doc, title, desc, subj, cat, chap, tags, pub, feat, repl, note)
                        },
                        onBulkPublish = { ids, pub -> adminViewModel.bulkPublish(adminUser, ids, pub) },
                        onBulkDelete = { ids -> adminViewModel.bulkDeleteDocuments(adminUser, ids) }
                    )
                    AdminSection.UPLOAD -> AdminUploadMaterialsSection(
                        adminUser = adminUser,
                        appSettings = appSettings,
                        subjects = allSubjects,
                        categories = allCategories,
                        uploadQueue = uploadQueue,
                        isUploading = isUploading,
                        onStageFiles = { uris -> adminViewModel.stagePickedFiles(context, uris, appSettings.maxUploadMb) },
                        onRemoveQueued = adminViewModel::removeQueuedFile,
                        onClearQueue = adminViewModel::clearUploadQueue,
                        onUploadAll = { title, desc, subj, cat, chap, tags, pub, feat ->
                            adminViewModel.uploadAllQueued(adminUser, title, desc, subj, cat, chap, tags, pub, feat)
                        }
                    )
                    AdminSection.SUBJECTS -> AdminSubjectManagerSection(
                        adminUser = adminUser,
                        subjects = allSubjects,
                        onSaveSubject = { adminViewModel.saveSubject(adminUser, it) }
                    )
                    AdminSection.CATEGORIES -> AdminCategoryManagerSection(
                        adminUser = adminUser,
                        categories = allCategories,
                        onSaveCategory = { cat, isNew -> adminViewModel.saveCategory(adminUser, cat, isNew) },
                        onDeleteCategory = { adminViewModel.deleteCategory(adminUser, it) }
                    )
                    AdminSection.STUDENTS -> AdminStudentManagerSection(
                        adminUser = adminUser,
                        students = allStudents,
                        onToggleDisabled = { adminViewModel.toggleStudentDisabled(adminUser, it) },
                        onDeleteStudent = { adminViewModel.deleteStudent(adminUser, it) },
                        onExportCsv = { adminViewModel.exportStudentsCsv(context) }
                    )
                    AdminSection.QUIZZES -> AdminQuizManagerSection(
                        adminUser = adminUser,
                        questions = allQuestions,
                        aiDrafts = aiDrafts,
                        isGeneratingAi = isGeneratingAi,
                        onGenerateAi = adminViewModel::generateAiQuestionsForReview,
                        onApproveDraft = { adminViewModel.approveDraftQuestion(adminUser, it) },
                        onDiscardDraft = adminViewModel::discardDraft,
                        onSaveManual = { q, isNew -> adminViewModel.saveManualQuestion(adminUser, q, isNew) },
                        onDeleteQuestion = { adminViewModel.deleteQuizQuestion(adminUser, it) }
                    )
                    AdminSection.ANNOUNCEMENTS -> AdminAnnouncementManagerSection(
                        adminUser = adminUser,
                        announcements = allAnnouncements,
                        documents = allDocuments,
                        onSaveAnnouncement = { item, isNew -> adminViewModel.saveAnnouncement(adminUser, item, isNew) },
                        onDeleteAnnouncement = { adminViewModel.deleteAnnouncement(adminUser, it) }
                    )
                    AdminSection.ANALYTICS -> AdminAnalyticsSection(
                        analytics = analytics,
                        onExportCsv = { adminViewModel.exportAnalyticsCsv(context) }
                    )
                    AdminSection.STORAGE -> AdminStorageSection(
                        analytics = analytics,
                        documents = allDocuments,
                        maxUploadMb = appSettings.maxUploadMb
                    )
                    AdminSection.ADMINS -> AdminManagementSection(
                        currentAdmin = adminUser,
                        allAdmins = allAdmins,
                        invites = adminInvites,
                        onInviteAdmin = { email, name, role, perms -> adminViewModel.inviteAdmin(adminUser, email, name, role, perms) },
                        onUpdateAdmin = { target, name, role, active, perms -> adminViewModel.updateAdmin(adminUser, target, name, role, active, perms) },
                        onRemoveAdmin = { adminViewModel.removeAdmin(adminUser, it) },
                        onRevokeInvite = { adminViewModel.revokeInvite(adminUser, it) }
                    )
                    AdminSection.LOGS -> AdminActivityLogsSection(logs = activityLogs)
                    AdminSection.SETTINGS -> AdminSettingsSection(
                        adminUser = adminUser,
                        appSettings = appSettings,
                        onSaveSettings = { adminViewModel.saveSettings(adminUser, it) }
                    )
                }
            }
        }
    }

    if (isWideLayout) {
        PermanentNavigationDrawer(
            drawerContent = { PermanentDrawerSheet(modifier = Modifier.width(272.dp)) { drawerBody() } },
            content = scaffoldContent
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { ModalDrawerSheet(modifier = Modifier.width(288.dp)) { drawerBody() } },
            content = scaffoldContent
        )
    }
}

@Composable
private fun AdminDashboardSection(
    adminUser: AdminUser,
    analytics: AdminAnalyticsSummary,
    recentLogs: List<ActivityLog>,
    reducedMotion: Boolean,
    onNavigate: (AdminSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(darbarHeroBrush()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "দরবার Control Center",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = NotoSansBengaliFamily),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${adminUser.displayName} (${adminUser.resolvedRole().label}) • Real-time sync enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNavigate(AdminSection.UPLOAD) }, modifier = Modifier.height(48.dp)) {
                            Text("Upload Material")
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AdminSection.QUIZZES) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("AI GK Studio", color = WarmGold)
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("Students", analytics.totalStudents, reducedMotion, Modifier.weight(1f))
                KpiCard("Admins", analytics.totalAdmins, reducedMotion, Modifier.weight(1f))
                KpiCard("Documents", analytics.totalFiles, reducedMotion, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard("PDFs", analytics.totalPdfs, reducedMotion, Modifier.weight(1f))
                KpiCard("Downloads", analytics.totalDownloads.toInt(), reducedMotion, Modifier.weight(1f))
                KpiCard("Quiz Runs", analytics.totalQuizAttempts.toInt(), reducedMotion, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Documents by Subject", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    SubjectBarChart(data = analytics.filesBySubject)
                }
            }
        }

        item {
            Text("Recent Admin Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        if (recentLogs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    title = "No Activity Logged Yet",
                    subtitle = "Uploads, edits, announcements, and admin changes appear here in real time."
                )
            }
        } else {
            items(recentLogs, key = { it.id }) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(log.action, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(formatTimestamp(log.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                        Text("${log.adminName} (${log.adminRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, count: Int, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedStatCounter(target = count, reducedMotion = reducedMotion, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminAnalyticsSection(
    analytics: AdminAnalyticsSummary,
    onExportCsv: () -> Unit
) {
    val others = (analytics.totalFiles - analytics.totalPdfs - analytics.totalImages).coerceAtLeast(0)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Analytics CSV Report")
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Subject Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SubjectBarChart(data = analytics.filesBySubject)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("File Format Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FileTypeDonutChart(pdfs = analytics.totalPdfs, images = analytics.totalImages, others = others)
                }
            }
        }
        item {
            Text("Most Downloaded Study Materials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(analytics.topDownloadedDocs, key = { it.id }) { doc ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${doc.subjectName} • ${doc.fileType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${doc.downloadCount} dl", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
            }
        }
    }
}

@Composable
private fun AdminStorageSection(
    analytics: AdminAnalyticsSummary,
    documents: List<StudyDocument>,
    maxUploadMb: Long
) {
    val totalMb = analytics.totalStorageBytes / (1024.0 * 1024.0)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Document Storage Used", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(DocumentStorageHelper.formatBytes(analytics.totalStorageBytes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(progress = { (totalMb.toFloat() / 200f).coerceIn(0.02f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("${documents.size} stored documents • Current per-file upload limit: $maxUploadMb MB", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Storage by Subject", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(analytics.storageBySubject.entries.toList(), key = { it.key }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.key, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(DocumentStorageHelper.formatBytes(entry.value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AdminActivityLogsSection(logs: List<ActivityLog>) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(logs, filter) {
        val q = filter.trim().lowercase()
        logs.filter { q.isBlank() || it.action.lowercase().contains(q) || it.details.lowercase().contains(q) || it.adminName.lowercase().contains(q) }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = { Text("Filter audit logs by admin or action...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(log.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(formatTimestamp(log.createdAt), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                        Text("${log.adminName} • ${log.adminRole}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsSection(
    adminUser: AdminUser,
    appSettings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_SETTINGS)
    val isSuperAdmin = adminUser.resolvedRole() == AdminRole.SUPER_ADMIN

    var appName by remember(appSettings.appName) { mutableStateOf(appSettings.appName) }
    var tagline by remember(appSettings.tagline) { mutableStateOf(appSettings.tagline) }
    var defaultTheme by remember(appSettings.defaultTheme) { mutableStateOf(appSettings.defaultTheme) }
    var maxUploadMb by remember(appSettings.maxUploadMb) { mutableFloatStateOf(appSettings.maxUploadMb.toFloat()) }
    var allowQuiz by remember(appSettings.allowStudentQuiz) { mutableStateOf(appSettings.allowStudentQuiz) }
    var allowDownloads by remember(appSettings.allowStudentDownloads) { mutableStateOf(appSettings.allowStudentDownloads) }
    var notifEnabled by remember(appSettings.notificationsEnabled) { mutableStateOf(appSettings.notificationsEnabled) }
    var announceEnabled by remember(appSettings.announcementsEnabled) { mutableStateOf(appSettings.announcementsEnabled) }
    var maintenanceMode by remember(appSettings.maintenanceMode) { mutableStateOf(appSettings.maintenanceMode) }
    var strictVerif by remember(appSettings.requireStrictVerification) { mutableStateOf(appSettings.requireStrictVerification) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(value = appName, onValueChange = { appName = it }, enabled = canManage, label = { Text("App Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tagline, onValueChange = { tagline = it }, enabled = canManage, label = { Text("Platform Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("Default Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                FilterChip(selected = defaultTheme == mode, onClick = { if (canManage) defaultTheme = mode }, label = { Text(mode) })
            }
        }

        Text("Max Upload Size Limit: ${maxUploadMb.roundToInt()} MB", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Slider(
            value = maxUploadMb,
            onValueChange = { if (canManage) maxUploadMb = it },
            valueRange = 1f..10f,
            steps = 8,
            enabled = canManage
        )

        SettingSwitchRow("Enable Student AI GK Quiz", allowQuiz, canManage) { allowQuiz = it }
        SettingSwitchRow("Enable Student Downloads", allowDownloads, canManage) { allowDownloads = it }
        SettingSwitchRow("Enable In-App Notifications", notifEnabled, canManage) { notifEnabled = it }
        SettingSwitchRow("Enable Announcements Feed", announceEnabled, canManage) { announceEnabled = it }

        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Super Admin Security Controls", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("Platform Maintenance Mode", maintenanceMode, isSuperAdmin) { maintenanceMode = it }
                SettingSwitchRow("Require Verified Quiz Facts", strictVerif, isSuperAdmin) { strictVerif = it }
            }
        }

        if (canManage) {
            Button(
                onClick = {
                    onSaveSettings(
                        appSettings.copy(
                            appName = appName,
                            tagline = tagline,
                            defaultTheme = defaultTheme,
                            maxUploadMb = maxUploadMb.roundToInt().toLong(),
                            allowStudentQuiz = allowQuiz,
                            allowStudentDownloads = allowDownloads,
                            notificationsEnabled = notifEnabled,
                            announcementsEnabled = announceEnabled,
                            maintenanceMode = maintenanceMode,
                            requireStrictVerification = strictVerif
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_app_settings_button")
            ) {
                Text("Save Global Settings")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
