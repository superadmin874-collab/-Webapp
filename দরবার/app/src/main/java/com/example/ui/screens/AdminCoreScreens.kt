package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FileStorageHelper
import com.example.core.PickedFilePayload
import com.example.data.model.AdminAccount
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.CategoryItem
import com.example.data.model.DocumentVersion
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentDirectoryEntry
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.example.data.repository.GeneratedQuizQuestion
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.DorbarLogoBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotoBengaliFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.UploadQueueItem
import kotlinx.coroutines.launch

enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    DOCUMENT_MANAGER("Document Manager", Icons.Default.Folder),
    UPLOAD_MATERIALS("Upload Materials", Icons.Default.CloudUpload),
    SUBJECT_MANAGER("Subject Manager", Icons.Default.MenuBook),
    CATEGORY_MANAGER("Category Manager", Icons.Default.Category),
    STUDENT_MANAGER("Student Manager", Icons.Default.School),
    AI_QUIZ_MANAGER("AI Quiz Manager", Icons.Default.AutoAwesome),
    ANNOUNCEMENT_MANAGER("Announcement Manager", Icons.Default.Campaign),
    ANALYTICS_REPORTS("Analytics and Reports", Icons.Default.Analytics),
    STORAGE_MANAGER("Storage Manager", Icons.Default.Storage),
    ADMIN_MANAGEMENT("Admin Management", Icons.Default.AdminPanelSettings),
    ACTIVITY_LOGS("Activity Logs", Icons.Default.ReceiptLong),
    APP_SETTINGS("App Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardShell(
    appSettings: AppSettings,
    currentAdminName: String,
    isSuperAdmin: Boolean,
    reducedMotion: Boolean,
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    documents: List<StudyDocument>,
    students: List<StudentDirectoryEntry>,
    admins: List<AdminAccount>,
    invites: List<AdminInvite>,
    quizQuestions: List<QuizQuestion>,
    aiDraftQuestions: List<GeneratedQuizQuestion>,
    aiGenerating: Boolean,
    announcements: List<Announcement>,
    activityLogs: List<AdminActivityLog>,
    uploadProgress: Float?,
    docVersions: List<DocumentVersion>,
    onSwitchToStudent: () -> Unit,
    onLogout: () -> Unit,
    onPostMessage: (String) -> Unit,
    onOpenDocumentPreview: (StudyDocument) -> Unit,
    onUploadBatch: (List<UploadQueueItem>, String, SubjectItem, CategoryItem, String, List<String>, Boolean, Boolean, () -> Unit) -> Unit,
    onUpdateDocument: (StudyDocument, String, String, SubjectItem, CategoryItem, String, List<String>, Boolean, Boolean, PickedFilePayload?, String) -> Unit,
    onLoadDocVersions: (String) -> Unit,
    onDeleteDocument: (StudyDocument) -> Unit,
    onBulkPublish: (List<StudyDocument>, Boolean) -> Unit,
    onBulkDelete: (List<StudyDocument>) -> Unit,
    onSaveSubject: (SubjectItem, Boolean) -> Unit,
    onSaveCategory: (String?, String, String, Boolean, Long) -> Unit,
    onDeleteCategory: (CategoryItem) -> Unit,
    onSetStudentActive: (StudentDirectoryEntry, Boolean) -> Unit,
    onRemoveStudent: (StudentDirectoryEntry) -> Unit,
    onGenerateAiQuestions: (String, QuizDifficulty, Int) -> Unit,
    onApproveDraftQuestion: (GeneratedQuizQuestion, String, QuizDifficulty) -> Unit,
    onDiscardDraftQuestion: (GeneratedQuizQuestion) -> Unit,
    onSaveQuizQuestion: (QuizQuestion, Boolean) -> Unit,
    onDeleteQuizQuestion: (QuizQuestion) -> Unit,
    onSaveAnnouncement: (Announcement, Boolean) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit,
    onInviteAdmin: (String, String, AdminRole, List<String>) -> Unit,
    onUpdateAdmin: (AdminAccount, String, AdminRole, Boolean, List<String>) -> Unit,
    onRemoveAdmin: (AdminAccount) -> Unit,
    onRemoveInvite: (AdminInvite) -> Unit,
    onUpdateAppSettings: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf(AdminSection.DASHBOARD) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val drawerMenuContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            DorbarLogoBadge(
                appName = appSettings.appName,
                subtitle = if (isSuperAdmin) "Super Admin Console" else "Administrator Console"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Signed in: $currentAdminName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            AdminSection.entries.forEach { section ->
                NavigationDrawerItem(
                    label = { Text(section.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = activeSection == section,
                    onClick = {
                        activeSection = section
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(section.icon, contentDescription = null) },
                    modifier = Modifier.testTag("admin_nav_${section.name.lowercase()}")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            NavigationDrawerItem(
                label = { Text("Student Workspace") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onSwitchToStudent()
                },
                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                modifier = Modifier.testTag("admin_switch_student_nav")
            )
            NavigationDrawerItem(
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RoseError) },
                modifier = Modifier.testTag("admin_logout_nav")
            )
        }
    }

    val screenBody: @Composable (PaddingValues) -> Unit = { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeSection) {
                AdminSection.DASHBOARD -> AdminOverviewSection(
                    currentAdminName = currentAdminName,
                    isSuperAdmin = isSuperAdmin,
                    reducedMotion = reducedMotion,
                    subjects = subjects,
                    documents = documents,
                    students = students,
                    admins = admins,
                    activityLogs = activityLogs,
                    onNavigate = { activeSection = it }
                )
                AdminSection.DOCUMENT_MANAGER -> AdminDocumentManagerSection(
                    subjects = subjects,
                    categories = categories,
                    documents = documents,
                    maxUploadMb = appSettings.maxUploadSizeMb,
                    docVersions = docVersions,
                    onOpenPreview = onOpenDocumentPreview,
                    onUpdateDocument = onUpdateDocument,
                    onLoadVersions = onLoadDocVersions,
                    onDeleteDocument = onDeleteDocument,
                    onBulkPublish = onBulkPublish,
                    onBulkDelete = onBulkDelete,
                    onNavigateToUpload = { activeSection = AdminSection.UPLOAD_MATERIALS },
                    onErrorMessage = onPostMessage
                )
                AdminSection.UPLOAD_MATERIALS -> AdminUploadMaterialsSection(
                    subjects = subjects,
                    categories = categories,
                    maxUploadMb = appSettings.maxUploadSizeMb,
                    uploadProgress = uploadProgress,
                    onUploadBatch = onUploadBatch,
                    onErrorMessage = onPostMessage
                )
                AdminSection.SUBJECT_MANAGER -> AdminSubjectManagerSection(
                    subjects = subjects,
                    documents = documents,
                    onSaveSubject = onSaveSubject
                )
                AdminSection.CATEGORY_MANAGER -> AdminCategoryManagerSection(
                    subjects = subjects,
                    categories = categories,
                    onSaveCategory = onSaveCategory,
                    onDeleteCategory = onDeleteCategory
                )
                AdminSection.STUDENT_MANAGER -> AdminStudentManagerSection(
                    students = students,
                    onSetActive = onSetStudentActive,
                    onRemoveStudent = onRemoveStudent,
                    onExportMessage = onPostMessage
                )
                AdminSection.AI_QUIZ_MANAGER -> AdminAiQuizManagerSection(
                    questions = quizQuestions,
                    drafts = aiDraftQuestions,
                    generating = aiGenerating,
                    onGenerate = onGenerateAiQuestions,
                    onApproveDraft = onApproveDraftQuestion,
                    onDiscardDraft = onDiscardDraftQuestion,
                    onSaveQuestion = onSaveQuizQuestion,
                    onDeleteQuestion = onDeleteQuizQuestion
                )
                AdminSection.ANNOUNCEMENT_MANAGER -> AdminAnnouncementManagerSection(
                    announcements = announcements,
                    documents = documents,
                    onSaveAnnouncement = onSaveAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                AdminSection.ANALYTICS_REPORTS -> AdminAnalyticsSection(
                    subjects = subjects,
                    documents = documents,
                    students = students,
                    admins = admins,
                    quizQuestions = quizQuestions,
                    reducedMotion = reducedMotion,
                    onExportMessage = onPostMessage
                )
                AdminSection.STORAGE_MANAGER -> AdminStorageManagerSection(
                    subjects = subjects,
                    documents = documents,
                    appSettings = appSettings,
                    isSuperAdmin = isSuperAdmin,
                    onUpdateAppSettings = onUpdateAppSettings
                )
                AdminSection.ADMIN_MANAGEMENT -> AdminTeamManagementSection(
                    admins = admins,
                    invites = invites,
                    isSuperAdmin = isSuperAdmin,
                    onInviteAdmin = onInviteAdmin,
                    onUpdateAdmin = onUpdateAdmin,
                    onRemoveAdmin = onRemoveAdmin,
                    onRemoveInvite = onRemoveInvite
                )
                AdminSection.ACTIVITY_LOGS -> AdminActivityLogsSection(logs = activityLogs)
                AdminSection.APP_SETTINGS -> AdminAppSettingsSection(
                    appSettings = appSettings,
                    isSuperAdmin = isSuperAdmin,
                    onSaveSettings = onUpdateAppSettings
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp
        if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(276.dp)) {
                        drawerMenuContent()
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(activeSection.label, fontWeight = FontWeight.Bold) },
                            actions = {
                                TextButton(onClick = onSwitchToStudent) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Student View")
                                }
                            }
                        )
                    },
                    content = screenBody
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                        drawerMenuContent()
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(activeSection.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        text = if (isSuperAdmin) "Super Admin" else "Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_drawer_toggle")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open admin menu")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = onSwitchToStudent,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Switch to student view")
                                }
                            }
                        )
                    },
                    content = screenBody
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewSection(
    currentAdminName: String,
    isSuperAdmin: Boolean,
    reducedMotion: Boolean,
    subjects: List<SubjectItem>,
    documents: List<StudyDocument>,
    students: List<StudentDirectoryEntry>,
    admins: List<AdminAccount>,
    activityLogs: List<AdminActivityLog>,
    onNavigate: (AdminSection) -> Unit
) {
    val totalDownloads = remember(documents) { documents.sumOf { it.downloadCount }.toInt() }
    val activeAdmins = remember(admins) { admins.count { it.active } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(NavyDeep, IndigoPrimary, RoyalPurple)))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "দরবার Admin Control Center",
                            fontFamily = NotoBengaliFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$currentAdminName • ${if (isSuperAdmin) "Super Admin Authority" else "Delegated Administrator"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGold
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminKpiCard("Documents", documents.size, Icons.Default.Description, reducedMotion, Modifier.weight(1f))
                    AdminKpiCard("Students", students.size, Icons.Default.People, reducedMotion, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminKpiCard("Downloads", totalDownloads, Icons.Default.FileDownload, reducedMotion, Modifier.weight(1f))
                    AdminKpiCard("Active Admins", activeAdmins.coerceAtLeast(1), Icons.Default.AdminPanelSettings, reducedMotion, Modifier.weight(1f))
                }
            }
        }

        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Button(onClick = { onNavigate(AdminSection.UPLOAD_MATERIALS) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Files")
                    }
                }
                item {
                    OutlinedButton(onClick = { onNavigate(AdminSection.AI_QUIZ_MANAGER) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate AI Quiz")
                    }
                }
                item {
                    OutlinedButton(onClick = { onNavigate(AdminSection.ANNOUNCEMENT_MANAGER) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Notice")
                    }
                }
                item {
                    OutlinedButton(onClick = { onNavigate(AdminSection.ADMIN_MANAGEMENT) }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Admins (${admins.size})")
                    }
                }
            }
        }

        item {
            Text("Subjects Overview (${subjects.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { subj ->
                    val count = documents.count { it.subjectId == subj.id }
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            Column {
                                Text("${subj.nameBn} • ${subj.nameEn}", fontFamily = NotoBengaliFontFamily, fontWeight = FontWeight.SemiBold)
                                Text(if (subj.visible) "Visible to students" else "Hidden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$count files", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("Recent Admin Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (activityLogs.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.ReceiptLong, "No activity logs yet", "Uploads, edits, and quiz approvals will appear here.")
            }
        } else {
            items(activityLogs.take(6), key = { it.id }) { log ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(log.adminName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(log.actionType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(log.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: Int,
    icon: ImageVector,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    Card(shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            AnimatedCounterText(targetValue = value, reducedMotion = reducedMotion)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDocumentManagerSection(
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    documents: List<StudyDocument>,
    maxUploadMb: Long,
    docVersions: List<DocumentVersion>,
    onOpenPreview: (StudyDocument) -> Unit,
    onUpdateDocument: (StudyDocument, String, String, SubjectItem, CategoryItem, String, List<String>, Boolean, Boolean, PickedFilePayload?, String) -> Unit,
    onLoadVersions: (String) -> Unit,
    onDeleteDocument: (StudyDocument) -> Unit,
    onBulkPublish: (List<StudyDocument>, Boolean) -> Unit,
    onBulkDelete: (List<StudyDocument>) -> Unit,
    onNavigateToUpload: () -> Unit,
    onErrorMessage: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filterPublished by remember { mutableStateOf("ALL") }
    var bulkMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var editingDoc by remember { mutableStateOf<StudyDocument?>(null) }
    var historyDoc by remember { mutableStateOf<StudyDocument?>(null) }

    val filtered = remember(documents, query, filterPublished) {
        documents.filter { doc ->
            (filterPublished == "ALL" || (filterPublished == "PUB" && doc.published) || (filterPublished == "DRAFT" && !doc.published)) &&
                (query.isBlank() || doc.title.contains(query, true) || doc.subjectName.contains(query, true) || doc.chapter.contains(query, true))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onNavigateToUpload, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload New")
                }
                OutlinedButton(
                    onClick = {
                        bulkMode = !bulkMode
                        selectedIds.clear()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (bulkMode) "Done Bulk" else "Bulk Select")
                }
            }
        }

        if (bulkMode && selectedIds.isNotEmpty()) {
            val selectedDocs = documents.filter { it.id in selectedIds }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Text("${selectedDocs.size} selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            onBulkPublish(selectedDocs, true)
                            selectedIds.clear()
                        }) { Text("Publish") }
                        TextButton(onClick = {
                            onBulkPublish(selectedDocs, false)
                            selectedIds.clear()
                        }) { Text("Unpublish") }
                        TextButton(onClick = {
                            onBulkDelete(selectedDocs)
                            selectedIds.clear()
                        }) { Text("Delete", color = RoseError) }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search documents…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filterPublished == "ALL", onClick = { filterPublished = "ALL" }, label = { Text("All (${documents.size})") })
                FilterChip(selected = filterPublished == "PUB", onClick = { filterPublished = "PUB" }, label = { Text("Published") })
                FilterChip(selected = filterPublished == "DRAFT", onClick = { filterPublished = "DRAFT" }, label = { Text("Drafts") })
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.Folder, "No documents match", "Upload materials or adjust your search filter.")
            }
        } else {
            items(filtered, key = { it.id }) { doc ->
                val isChecked = doc.id in selectedIds
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (bulkMode) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(doc.id) else selectedIds.remove(doc.id)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${doc.subjectName} • ${doc.categoryName} • ${doc.chapter} • v${doc.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (doc.published) EmeraldSuccess.copy(alpha = 0.16f) else WarmGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (doc.published) "Published" else "Draft",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (doc.published) EmeraldSuccess else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = { onOpenPreview(doc) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview")
                            }
                            IconButton(onClick = { editingDoc = doc }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(
                                onClick = {
                                    onLoadVersions(doc.id)
                                    historyDoc = doc
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Version history")
                            }
                            IconButton(
                                onClick = {
                                    val subj = subjects.firstOrNull { it.id == doc.subjectId } ?: subjects.firstOrNull()
                                    val cat = categories.firstOrNull { it.id == doc.categoryId } ?: categories.firstOrNull()
                                    if (subj != null && cat != null) {
                                        onUpdateDocument(
                                            doc, doc.title, doc.description, subj, cat, doc.chapter,
                                            doc.tags, doc.featured, !doc.published, null,
                                            if (doc.published) "Unpublished document" else "Published document"
                                        )
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    if (doc.published) Icons.Default.Unpublished else Icons.Default.Publish,
                                    contentDescription = if (doc.published) "Unpublish" else "Publish"
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteDocument(doc) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                            }
                        }
                    }
                }
            }
        }
    }

    editingDoc?.let { target ->
        EditDocumentDialog(
            doc = target,
            subjects = subjects,
            categories = categories,
            maxUploadMb = maxUploadMb,
            onDismiss = { editingDoc = null },
            onErrorMessage = onErrorMessage,
            onSave = { title, desc, subj, cat, ch, tags, feat, pub, replacement, note ->
                onUpdateDocument(target, title, desc, subj, cat, ch, tags, feat, pub, replacement, note)
                editingDoc = null
            }
        )
    }

    historyDoc?.let { target ->
        AlertDialog(
            onDismissRequest = { historyDoc = null },
            title = { Text("Version History • ${target.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (docVersions.isEmpty()) {
                        Text("Loading version history…")
                    } else {
                        docVersions.forEach { ver ->
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("v${ver.version} • ${ver.fileName}", style = MaterialTheme.typography.titleSmall)
                                    Text("By ${ver.editedByName} • ${FileStorageHelper.formatFileSize(ver.fileSizeBytes)}", style = MaterialTheme.typography.labelSmall)
                                    Text(ver.changeNote, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyDoc = null }) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDocumentDialog(
    doc: StudyDocument,
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    maxUploadMb: Long,
    onDismiss: () -> Unit,
    onErrorMessage: (String) -> Unit,
    onSave: (String, String, SubjectItem, CategoryItem, String, List<String>, Boolean, Boolean, PickedFilePayload?, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(doc.title) }
    var description by remember { mutableStateOf(doc.description) }
    var chapter by remember { mutableStateOf(doc.chapter) }
    var tagsText by remember { mutableStateOf(doc.tags.joinToString(", ")) }
    var changeNote by remember { mutableStateOf("Updated metadata") }
    var featured by remember { mutableStateOf(doc.featured) }
    var published by remember { mutableStateOf(doc.published) }
    var selectedSubj by remember { mutableStateOf(subjects.firstOrNull { it.id == doc.subjectId } ?: subjects.firstOrNull()) }
    var selectedCat by remember { mutableStateOf(categories.firstOrNull { it.id == doc.categoryId } ?: categories.firstOrNull()) }
    var replacementPayload by remember { mutableStateOf<PickedFilePayload?>(null) }
    var subjExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                FileStorageHelper.readPickedUri(context, uri, maxUploadMb)
                    .onSuccess { replacementPayload = it }
                    .onFailure { onErrorMessage(it.message ?: "Unsupported file") }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document (v${doc.version})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (subjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = subjExpanded, onExpandedChange = { subjExpanded = it }) {
                        OutlinedTextField(
                            value = selectedSubj?.nameEn ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                            subjects.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.nameBn} (${s.nameEn})") }, onClick = {
                                    selectedSubj = s
                                    subjExpanded = false
                                })
                            }
                        }
                    }
                }
                if (categories.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(
                            value = selectedCat?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            categories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                    selectedCat = c
                                    catExpanded = false
                                })
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { replacePicker.launch(FileStorageHelper.SUPPORTED_MIME_TYPES) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(replacementPayload?.fileName ?: "Replace File (Optional)")
                }

                OutlinedTextField(value = changeNote, onValueChange = { changeNote = it }, label = { Text("Version Change Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Featured")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Published")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && selectedSubj != null && selectedCat != null,
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(title, description, selectedSubj!!, selectedCat!!, chapter, tags, featured, published, replacementPayload, changeNote)
                }
            ) { Text("Save v${doc.version + 1}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUploadMaterialsSection(
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    maxUploadMb: Long,
    uploadProgress: Float?,
    onUploadBatch: (List<UploadQueueItem>, String, SubjectItem, CategoryItem, String, List<String>, Boolean, Boolean, () -> Unit) -> Unit,
    onErrorMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue = remember { mutableStateListOf<UploadQueueItem>() }
    var selectedSubj by remember(subjects) { mutableStateOf(subjects.firstOrNull()) }
    var selectedCat by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var chapter by remember { mutableStateOf("Chapter 1") }
    var description by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var featured by remember { mutableStateOf(false) }
    var published by remember { mutableStateOf(true) }
    var subjExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch {
            uris.forEach { uri ->
                FileStorageHelper.readPickedUri(context, uri, maxUploadMb)
                    .onSuccess { payload ->
                        queue.add(UploadQueueItem(uri, payload, payload.fileName.substringBeforeLast('.')))
                    }
                    .onFailure { err ->
                        onErrorMessage(err.message ?: "Skipped invalid file")
                    }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .clickable { multiPicker.launch(FileStorageHelper.SUPPORTED_MIME_TYPES) }
                    .testTag("admin_pick_files_box")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Text("Tap to Select Multiple Educational Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Supports PDF, JPG, PNG, WEBP, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT (Max $maxUploadMb MB each)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uploadProgress != null) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Uploading to Dorbar Cloud… ${(uploadProgress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
            }
        }

        if (queue.isNotEmpty()) {
            item {
                Text("Preview & Rename Queue (${queue.size} files)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(queue, key = { it.uri.toString() + it.payload.fileName }) { item ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${item.payload.fileExtension.uppercase()} • ${FileStorageHelper.formatFileSize(item.payload.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { queue.remove(item) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove from queue", tint = RoseError)
                            }
                        }
                        OutlinedTextField(
                            value = item.title,
                            onValueChange = { updated ->
                                val idx = queue.indexOf(item)
                                if (idx >= 0) queue[idx] = item.copy(title = updated)
                            },
                            label = { Text("Document Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Classification & Publishing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (subjects.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = subjExpanded, onExpandedChange = { subjExpanded = it }) {
                            OutlinedTextField(
                                value = selectedSubj?.let { "${it.nameBn} (${it.nameEn})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Subject") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = subjExpanded, onDismissRequest = { subjExpanded = false }) {
                                subjects.forEach { s ->
                                    DropdownMenuItem(text = { Text("${s.nameBn} (${s.nameEn})") }, onClick = {
                                        selectedSubj = s
                                        subjExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    val matchingCats = categories.filter { it.subjectId == "ALL" || it.subjectId == selectedSubj?.id }
                    if (matchingCats.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                            OutlinedTextField(
                                value = selectedCat?.name ?: matchingCats.first().name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                matchingCats.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                        selectedCat = c
                                        catExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter / Unit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description / Notes") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Custom Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Mark as Featured")
                        Switch(checked = featured, onCheckedChange = { featured = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Publish Immediately to Students")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }

                    Button(
                        enabled = queue.isNotEmpty() && selectedSubj != null && selectedCat != null && uploadProgress == null,
                        onClick = {
                            val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onUploadBatch(
                                queue.toList(),
                                description,
                                selectedSubj!!,
                                selectedCat!!,
                                chapter,
                                tags,
                                featured,
                                published
                            ) {
                                queue.clear()
                                description = ""
                                tagsText = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_publish_upload_button")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (published) "Upload & Publish (${queue.size})" else "Save as Draft (${queue.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSubjectManagerSection(
    subjects: List<SubjectItem>,
    documents: List<StudyDocument>,
    onSaveSubject: (SubjectItem, Boolean) -> Unit
) {
    var editingSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(onClick = { addingNew = true }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Subject")
            }
        }
        items(subjects, key = { it.id }) { subj ->
            val count = documents.count { it.subjectId == subj.id }
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${subj.nameBn} • ${subj.nameEn}", fontFamily = NotoBengaliFontFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(subj.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$count documents • Order #${subj.sortOrder}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Switch(
                        checked = subj.visible,
                        onCheckedChange = { onSaveSubject(subj.copy(visible = it), false) }
                    )
                    IconButton(onClick = { editingSubject = subj }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit subject")
                    }
                }
            }
        }
    }

    if (addingNew || editingSubject != null) {
        val base = editingSubject ?: SubjectItem(sortOrder = (subjects.size + 1).toLong())
        var nameEn by remember(base) { mutableStateOf(base.nameEn) }
        var nameBn by remember(base) { mutableStateOf(base.nameBn) }
        var desc by remember(base) { mutableStateOf(base.description) }
        var order by remember(base) { mutableStateOf(base.sortOrder.toString()) }
        var visible by remember(base) { mutableStateOf(base.visible) }

        AlertDialog(
            onDismissRequest = {
                addingNew = false
                editingSubject = null
            },
            title = { Text(if (addingNew) "Add Subject" else "Edit ${base.nameEn}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("English Name") }, singleLine = true)
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Bengali Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Sort Order") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Visible to Students")
                        Switch(checked = visible, onCheckedChange = { visible = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = nameEn.isNotBlank() && nameBn.isNotBlank(),
                    onClick = {
                        onSaveSubject(
                            base.copy(
                                nameEn = nameEn,
                                nameBn = nameBn,
                                description = desc,
                                sortOrder = order.toLongOrNull() ?: 1L,
                                visible = visible
                            ),
                            addingNew
                        )
                        addingNew = false
                        editingSubject = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    addingNew = false
                    editingSubject = null
                }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCategoryManagerSection(
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    onSaveCategory: (String?, String, String, Boolean, Long) -> Unit,
    onDeleteCategory: (CategoryItem) -> Unit
) {
    var editingCat by remember { mutableStateOf<CategoryItem?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(onClick = { addingNew = true }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Category")
            }
        }
        items(categories, key = { it.id }) { cat ->
            val subjName = if (cat.subjectId == "ALL") "All Six Subjects" else subjects.firstOrNull { it.id == cat.subjectId }?.nameEn ?: cat.subjectId
            Card(shape = RoundedCornerShape(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Applies to: $subjName • Order #${cat.sortOrder}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = cat.visible, onCheckedChange = { onSaveCategory(cat.id, cat.name, cat.subjectId, it, cat.sortOrder) })
                    IconButton(onClick = { editingCat = cat }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit category")
                    }
                    IconButton(onClick = { onDeleteCategory(cat) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = RoseError)
                    }
                }
            }
        }
    }

    if (addingNew || editingCat != null) {
        val base = editingCat ?: CategoryItem(subjectId = "ALL", sortOrder = (categories.size + 1).toLong())
        var name by remember(base) { mutableStateOf(base.name) }
        var subjectId by remember(base) { mutableStateOf(base.subjectId) }
        var order by remember(base) { mutableStateOf(base.sortOrder.toString()) }
        var visible by remember(base) { mutableStateOf(base.visible) }
        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                addingNew = false
                editingCat = null
            },
            title = { Text(if (addingNew) "New Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true)
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (subjectId == "ALL") "All Subjects" else subjects.firstOrNull { it.id == subjectId }?.nameEn ?: subjectId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Scope") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("All Subjects") }, onClick = { subjectId = "ALL"; expanded = false })
                            subjects.forEach { s ->
                                DropdownMenuItem(text = { Text(s.nameEn) }, onClick = { subjectId = s.id; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Sort Order") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Visible")
                        Switch(checked = visible, onCheckedChange = { visible = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSaveCategory(editingCat?.id, name, subjectId, visible, order.toLongOrNull() ?: 1L)
                        addingNew = false
                        editingCat = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    addingNew = false
                    editingCat = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminStudentManagerSection(
    students: List<StudentDirectoryEntry>,
    onSetActive: (StudentDirectoryEntry, Boolean) -> Unit,
    onRemoveStudent: (StudentDirectoryEntry) -> Unit,
    onExportMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(students, query) {
        students.filter {
            query.isBlank() || it.displayName.contains(query, true) || it.studentCode.contains(query, true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search name or DRB ID…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val csv = buildString {
                            appendLine("StudentID,DisplayName,Active,Downloads,Quizzes,AvgScorePercent,BestScorePercent,TotalPoints")
                            students.forEach { s ->
                                val safeName = s.displayName.replace("\"", "'")
                                appendLine("${s.studentCode},\"$safeName\",${s.active},${s.downloadCount},${s.quizAttemptsCount},${s.averageScorePercent},${s.bestScorePercent},${s.totalPoints}")
                            }
                        }
                        FileStorageHelper.shareTextReport(context, "dorbar_students_report.csv", csv)
                            .onFailure { onExportMessage("Failed to export CSV.") }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CSV")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.School, "No students found", "Registered students with unique DRB IDs will appear here.")
            }
        } else {
            items(filtered, key = { it.uid }) { st ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(st.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("ID: ${st.studentCode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                color = if (st.active) EmeraldSuccess.copy(alpha = 0.16f) else RoseError.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (st.active) "Active" else "Disabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (st.active) EmeraldSuccess else RoseError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            "Downloads: ${st.downloadCount} • Quizzes: ${st.quizAttemptsCount} • Avg: ${st.averageScorePercent}% • Best: ${st.bestScorePercent}% • Points: ${st.totalPoints}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onSetActive(st, !st.active) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(if (st.active) Icons.Default.Block else Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (st.active) "Disable" else "Reactivate")
                            }
                            TextButton(onClick = { onRemoveStudent(st) }) {
                                Text("Remove Account", color = RoseError)
                            }
                        }
                    }
                }
            }
        }
    }
}
