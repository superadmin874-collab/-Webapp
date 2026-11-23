package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminInvite
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminRole
import com.example.data.model.AdminUser
import com.example.data.model.Announcement
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.data.repository.GeneratedQuizDraft
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.WarmGold

@Composable
fun AdminStudentManagerSection(
    adminUser: AdminUser,
    students: List<StudentProfile>,
    onToggleDisabled: (StudentProfile) -> Unit,
    onDeleteStudent: (StudentProfile) -> Unit,
    onExportCsv: () -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_STUDENTS)
    var search by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<StudentProfile?>(null) }

    val filtered = remember(students, search) {
        val q = search.trim().lowercase()
        students.filter { q.isBlank() || it.fullName.lowercase().contains(q) || it.studentCode.lowercase().contains(q) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search name or DRB ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onExportCsv, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Default.Download, contentDescription = "Export CSV")
                Spacer(Modifier.width(4.dp))
                Text("CSV")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Search,
                        title = "No Students Registered Yet",
                        subtitle = "Students appear automatically when they enter their name on onboarding."
                    )
                }
            } else {
                items(filtered, key = { it.uid }) { student ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(student.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = (if (student.disabled) CrimsonError else EmeraldSuccess).copy(alpha = 0.14f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (student.disabled) "Disabled" else "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (student.disabled) CrimsonError else EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("ID: ${student.studentCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Downloads: ${student.downloadsCount} • Quizzes: ${student.quizzesTaken} • Accuracy: ${student.averageAccuracyPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canManage) {
                                IconButton(onClick = { onToggleDisabled(student) }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = if (student.disabled) Icons.Default.CheckCircle else Icons.Default.Block,
                                        contentDescription = if (student.disabled) "Reactivate student" else "Disable student",
                                        tint = if (student.disabled) EmeraldSuccess else WarmGold
                                    )
                                }
                                IconButton(onClick = { deleteTarget = student }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete student", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove ${deleteTarget!!.fullName}?") },
            text = { Text("This removes their student profile and leaderboard score.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteStudent(deleteTarget!!); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminQuizManagerSection(
    adminUser: AdminUser,
    questions: List<QuizQuestion>,
    aiDrafts: List<GeneratedQuizDraft>,
    isGeneratingAi: Boolean,
    onGenerateAi: (String, QuizDifficulty, Int) -> Unit,
    onApproveDraft: (GeneratedQuizDraft) -> Unit,
    onDiscardDraft: (GeneratedQuizDraft) -> Unit,
    onSaveManual: (QuizQuestion, Boolean) -> Unit,
    onDeleteQuestion: (String) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_QUIZZES)
    var genCategory by remember { mutableStateOf(DarbarCurriculumDefaults.GK_CATEGORIES.first()) }
    var genDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var genCount by remember { mutableIntStateOf(5) }
    var editingQuestion by remember { mutableStateOf<Pair<QuizQuestion, Boolean>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canManage) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("AI GK Question Generator & Review Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DarbarCurriculumDefaults.GK_CATEGORIES.forEach { cat ->
                                FilterChip(selected = genCategory == cat, onClick = { genCategory = cat }, label = { Text(cat) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizDifficulty.entries.forEach { diff ->
                                FilterChip(selected = genDifficulty == diff, onClick = { genDifficulty = diff }, label = { Text(diff.label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 10).forEach { cnt ->
                                FilterChip(selected = genCount == cnt, onClick = { genCount = cnt }, label = { Text("$cnt Qs") })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onGenerateAi(genCategory, genDifficulty, genCount) },
                                enabled = !isGeneratingAi,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("admin_generate_ai_quiz_btn")
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (isGeneratingAi) "Generating..." else "Generate AI Drafts")
                            }
                            OutlinedButton(
                                onClick = {
                                    editingQuestion = QuizQuestion(category = genCategory, difficulty = genDifficulty.name, verifiedByAdmin = true, approved = true) to true
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Manual")
                            }
                        }
                    }
                }
            }
        }

        if (aiDrafts.isNotEmpty()) {
            item {
                Text("Pending Review (${aiDrafts.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(aiDrafts, key = { it.questionText }) { draft ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${draft.category} • ${draft.difficulty.label} • Fact Date: ${draft.factDate}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(draft.questionText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        draft.options.forEachIndexed { idx, opt ->
                            val isAns = idx == draft.correctIndex
                            Text(
                                text = "${'A' + idx}. $opt ${if (isAns) "✓" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isAns) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAns) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text("Explanation: ${draft.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Source: ${draft.source} (${draft.confidenceNote})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { onApproveDraft(draft) }, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Default.Verified, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Verify & Approve")
                            }
                            OutlinedButton(onClick = { onDiscardDraft(draft) }, modifier = Modifier.height(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Discard")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Quiz Question Bank (${questions.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        if (questions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "No Banked Questions Yet",
                    subtitle = "Generate AI drafts above and approve them, or create manual GK questions."
                )
            }
        } else {
            items(questions, key = { it.id }) { q ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${q.category} • ${q.difficulty} ${if (q.verifiedByAdmin) "• Verified" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(q.questionText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Answer: ${q.options.getOrNull(q.correctIndex.toInt()) ?: ""}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                        }
                        if (canManage) {
                            IconButton(onClick = { editingQuestion = q to false }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit question")
                            }
                            IconButton(onClick = { onDeleteQuestion(q.id) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete question", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingQuestion != null) {
        val (q, isNew) = editingQuestion!!
        var qText by remember(q.id) { mutableStateOf(q.questionText) }
        var opt0 by remember(q.id) { mutableStateOf(q.options.getOrElse(0) { "" }) }
        var opt1 by remember(q.id) { mutableStateOf(q.options.getOrElse(1) { "" }) }
        var opt2 by remember(q.id) { mutableStateOf(q.options.getOrElse(2) { "" }) }
        var opt3 by remember(q.id) { mutableStateOf(q.options.getOrElse(3) { "" }) }
        var correctIdx by remember(q.id) { mutableIntStateOf(q.correctIndex.toInt().coerceIn(0, 3)) }
        var explanation by remember(q.id) { mutableStateOf(q.explanation) }
        var source by remember(q.id) { mutableStateOf(q.source) }
        var approved by remember(q.id) { mutableStateOf(q.approved) }

        AlertDialog(
            onDismissRequest = { editingQuestion = null },
            title = { Text(if (isNew) "New GK Question" else "Edit GK Question") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = qText, onValueChange = { qText = it }, label = { Text("Question") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt0, onValueChange = { opt0 = it }, label = { Text("Option A") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option B") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option C") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option D") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C", "D").forEachIndexed { idx, lbl ->
                            FilterChip(selected = correctIdx == idx, onClick = { correctIdx = idx }, label = { Text("Ans $lbl") })
                        }
                    }
                    OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source / Reference") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Approved & Verified")
                        Switch(checked = approved, onCheckedChange = { approved = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = qText.trim().length >= 5 && explanation.isNotBlank() && listOf(opt0, opt1, opt2, opt3).all { it.isNotBlank() },
                    onClick = {
                        onSaveManual(
                            q.copy(
                                questionText = qText,
                                options = listOf(opt0, opt1, opt2, opt3),
                                correctIndex = correctIdx.toLong(),
                                explanation = explanation,
                                source = source,
                                verifiedByAdmin = approved,
                                approved = approved
                            ),
                            isNew
                        )
                        editingQuestion = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingQuestion = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminAnnouncementManagerSection(
    adminUser: AdminUser,
    announcements: List<Announcement>,
    documents: List<StudyDocument>,
    onSaveAnnouncement: (Announcement, Boolean) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_ANNOUNCEMENTS)
    var editingItem by remember { mutableStateOf<Pair<Announcement, Boolean>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canManage) {
            item {
                Button(
                    onClick = { editingItem = Announcement(published = true) to true },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_announcement_button")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Publish New Announcement")
                }
            }
        }
        if (announcements.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Campaign,
                    title = "No Announcements",
                    subtitle = "Publish pinned exam notices, routine updates, or attached PDFs."
                )
            }
        } else {
            items(announcements, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (item.pinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = WarmGold, modifier = Modifier.size(16.dp))
                                }
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${if (item.published) "Published" else "Draft"} • ${item.publishDate} • ${item.attachmentName ?: "No attachment"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (canManage) {
                            IconButton(onClick = { editingItem = item to false }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit notice")
                            }
                            IconButton(onClick = { onDeleteAnnouncement(item.id) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete notice", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingItem != null) {
        val (item, isNew) = editingItem!!
        var title by remember(item.id) { mutableStateOf(item.title) }
        var body by remember(item.id) { mutableStateOf(item.body) }
        var pinned by remember(item.id) { mutableStateOf(item.pinned) }
        var published by remember(item.id) { mutableStateOf(item.published) }
        var attachedDoc by remember(item.id) { mutableStateOf(documents.firstOrNull { it.id == item.attachmentDocId }) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text(if (isNew) "New Announcement" else "Edit Announcement") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Announcement Message") }, maxLines = 4, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pin to Top")
                        Switch(checked = pinned, onCheckedChange = { pinned = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Published")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }
                    if (documents.isNotEmpty()) {
                        Text("Attach Uploaded Document (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = attachedDoc == null, onClick = { attachedDoc = null }, label = { Text("None") })
                            documents.take(8).forEach { d ->
                                FilterChip(
                                    selected = attachedDoc?.id == d.id,
                                    onClick = { attachedDoc = d },
                                    label = { Text(d.title.take(16)) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && body.isNotBlank(),
                    onClick = {
                        onSaveAnnouncement(
                            item.copy(
                                title = title,
                                body = body,
                                pinned = pinned,
                                published = published,
                                attachmentDocId = attachedDoc?.id,
                                attachmentName = attachedDoc?.fileName,
                                attachmentType = attachedDoc?.fileType
                            ),
                            isNew
                        )
                        editingItem = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingItem = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminManagementSection(
    currentAdmin: AdminUser,
    allAdmins: List<AdminUser>,
    invites: List<AdminInvite>,
    onInviteAdmin: (String, String, AdminRole, List<String>) -> Unit,
    onUpdateAdmin: (AdminUser, String, AdminRole, Boolean, List<String>) -> Unit,
    onRemoveAdmin: (AdminUser) -> Unit,
    onRevokeInvite: (String) -> Unit
) {
    val isSuperAdmin = currentAdmin.resolvedRole() == AdminRole.SUPER_ADMIN
    var showInviteDialog by remember { mutableStateOf(false) }
    var editingAdmin by remember { mutableStateOf<AdminUser?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMiniStat("Total Admins", "${allAdmins.size}", Modifier.weight(1f))
                AdminMiniStat("Active", "${allAdmins.count { it.active }}", Modifier.weight(1f))
                AdminMiniStat("Pending Invites", "${invites.count { it.claimedByUid == null }}", Modifier.weight(1f))
            }
        }

        if (!isSuperAdmin) {
            item {
                Surface(
                    color = WarmGold.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = WarmGold)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Super Admin Controls Only: Only the Super Admin (${DarbarCurriculumDefaults.SUPER_ADMIN_EMAIL}) can add, demote, or delete administrators.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            item {
                Button(
                    onClick = { showInviteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("invite_admin_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Invite Admin 2 / Staff Administrator")
                }
            }
        }

        item {
            Text("Administrators (${allAdmins.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        items(allAdmins, key = { it.uid }) { adm ->
            val isTargetSuper = adm.resolvedRole() == AdminRole.SUPER_ADMIN
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (isTargetSuper) WarmGold else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(adm.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(
                                color = (if (isTargetSuper) WarmGold else MaterialTheme.colorScheme.primary).copy(alpha = 0.16f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = adm.resolvedRole().label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(adm.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (adm.active) "Active • Permissions: ${if (isTargetSuper) "Full Super Admin" else "${adm.permissions.size} enabled"}" else "Deactivated",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (adm.active) EmeraldSuccess else CrimsonError
                        )
                    }
                    if (isSuperAdmin && !isTargetSuper) {
                        IconButton(onClick = { editingAdmin = adm }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit admin permissions")
                        }
                        IconButton(onClick = { onRemoveAdmin(adm) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove admin", tint = CrimsonError)
                        }
                    }
                }
            }
        }

        if (invites.isNotEmpty() && isSuperAdmin) {
            item {
                Text("Admin Invitations (${invites.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(invites, key = { it.email }) { inv ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${inv.email} • ${inv.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (inv.claimedByUid != null) "Activated on Google Sign-In" else "Pending first Google Sign-In",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inv.claimedByUid != null) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onRevokeInvite(inv.email) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Revoke invite", tint = CrimsonError)
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(AdminRole.ADMIN) }
        val selectedPerms = remember { mutableStateListOf<String>().apply { addAll(AdminPermissions.ALL_ADMIN_PERMISSIONS) } }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Admin 2 / Co-Admin") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email Address") }, placeholder = { Text("admin2@gmail.com") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AdminRole.ADMIN, AdminRole.MODERATOR).forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                        }
                    }
                    Text("Permissions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    AdminPermissions.ALL_ADMIN_PERMISSIONS.forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = perm in selectedPerms,
                                onCheckedChange = { checked -> if (checked) selectedPerms.add(perm) else selectedPerms.remove(perm) }
                            )
                            Text(AdminPermissions.labelFor(perm), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = email.contains('@'),
                    onClick = {
                        onInviteAdmin(email, name, role, selectedPerms.toList())
                        showInviteDialog = false
                    }
                ) { Text("Send Invite") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") } }
        )
    }

    if (editingAdmin != null) {
        val target = editingAdmin!!
        var name by remember(target.uid) { mutableStateOf(target.displayName) }
        var role by remember(target.uid) { mutableStateOf(target.resolvedRole()) }
        var active by remember(target.uid) { mutableStateOf(target.active) }
        val perms = remember(target.uid) { mutableStateListOf<String>().apply { addAll(target.permissions) } }

        AlertDialog(
            onDismissRequest = { editingAdmin = null },
            title = { Text("Edit ${target.email}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Account Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AdminRole.ADMIN, AdminRole.MODERATOR).forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                        }
                    }
                    AdminPermissions.ALL_ADMIN_PERMISSIONS.forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = perm in perms,
                                onCheckedChange = { checked -> if (checked) perms.add(perm) else perms.remove(perm) }
                            )
                            Text(AdminPermissions.labelFor(perm), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onUpdateAdmin(target, name, role, active, perms.toList())
                        editingAdmin = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingAdmin = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
