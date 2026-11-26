package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.FileStorageHelper
import com.example.data.model.AdminAccount
import com.example.data.model.AdminActivityLog
import com.example.data.model.AdminInvite
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.DefaultCatalog
import com.example.data.model.QuizDifficulty
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentDirectoryEntry
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.example.data.repository.GeneratedQuizQuestion
import com.example.ui.components.AnalyticsBarChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun AdminAiQuizManagerSection(
    questions: List<QuizQuestion>,
    drafts: List<GeneratedQuizQuestion>,
    generating: Boolean,
    onGenerate: (String, QuizDifficulty, Int) -> Unit,
    onApproveDraft: (GeneratedQuizQuestion, String, QuizDifficulty) -> Unit,
    onDiscardDraft: (GeneratedQuizQuestion) -> Unit,
    onSaveQuestion: (QuizQuestion, Boolean) -> Unit,
    onDeleteQuestion: (QuizQuestion) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DefaultCatalog.gkCategories.first()) }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var count by remember { mutableIntStateOf(5) }
    var editingQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var addingManual by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI GK Question Generator (Gemini)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DefaultCatalog.gkCategories) { cat ->
                            FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuizDifficulty.entries.forEach { d ->
                            FilterChip(selected = selectedDifficulty == d, onClick = { selectedDifficulty = d }, label = { Text(d.label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15).forEach { n ->
                            FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n Qs") })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            enabled = !generating,
                            onClick = { onGenerate(selectedCategory, selectedDifficulty, count) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("admin_generate_ai_quiz_button")
                        ) {
                            if (generating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (generating) "Generating…" else "Generate Drafts")
                        }
                        OutlinedButton(onClick = { addingManual = true }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual")
                        }
                    }
                }
            }
        }

        if (drafts.isNotEmpty()) {
            item {
                Text("AI Drafts Pending Review (${drafts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(drafts) { draft ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                color = if (draft.confidenceLevel == "VERIFIED") EmeraldSuccess.copy(alpha = 0.2f) else WarmGold.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    draft.confidenceLevel,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Row {
                                IconButton(onClick = { onApproveDraft(draft, selectedCategory, selectedDifficulty) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Check, contentDescription = "Approve question", tint = EmeraldSuccess)
                                }
                                IconButton(onClick = { onDiscardDraft(draft) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Close, contentDescription = "Discard draft", tint = RoseError)
                                }
                            }
                        }
                        Text(draft.questionText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        draft.options.forEachIndexed { idx, opt ->
                            Text(
                                text = "${'A' + idx}. $opt ${if (idx == draft.correctIndex) "✓" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (idx == draft.correctIndex) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text("Source: ${draft.sourceNote}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Text("Approved & Banked Questions (${questions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (questions.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.AutoAwesome, "No saved questions yet", "Generate AI drafts above or add a manual question.")
            }
        } else {
            items(questions, key = { it.id }) { q ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("${q.category} • ${q.difficulty}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Row {
                                Switch(checked = q.approved, onCheckedChange = { onSaveQuestion(q.copy(approved = it), false) })
                                IconButton(onClick = { editingQuestion = q }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit question")
                                }
                                IconButton(onClick = { onDeleteQuestion(q) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete question", tint = RoseError)
                                }
                            }
                        }
                        Text(q.questionText, style = MaterialTheme.typography.titleSmall)
                        if (q.sourceNote.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(q.sourceNote, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (addingManual || editingQuestion != null) {
        val base = editingQuestion ?: QuizQuestion(
            category = selectedCategory,
            difficulty = selectedDifficulty.name,
            options = listOf("", "", "", ""),
            aiGenerated = false,
            approved = true
        )
        var text by remember(base) { mutableStateOf(base.questionText) }
        var opt0 by remember(base) { mutableStateOf(base.options.getOrElse(0) { "" }) }
        var opt1 by remember(base) { mutableStateOf(base.options.getOrElse(1) { "" }) }
        var opt2 by remember(base) { mutableStateOf(base.options.getOrElse(2) { "" }) }
        var opt3 by remember(base) { mutableStateOf(base.options.getOrElse(3) { "" }) }
        var correctIdx by remember(base) { mutableIntStateOf(base.correctIndex.toInt().coerceIn(0, 3)) }
        var explanation by remember(base) { mutableStateOf(base.explanation) }
        var sourceNote by remember(base) { mutableStateOf(base.sourceNote) }

        AlertDialog(
            onDismissRequest = { addingManual = false; editingQuestion = null },
            title = { Text(if (addingManual) "New GK Question" else "Edit GK Question") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Question") })
                    OutlinedTextField(value = opt0, onValueChange = { opt0 = it }, label = { Text("Option A") }, singleLine = true)
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option B") }, singleLine = true)
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option C") }, singleLine = true)
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option D") }, singleLine = true)
                    Text("Correct Answer:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..3).forEach { idx ->
                            FilterChip(selected = correctIdx == idx, onClick = { correctIdx = idx }, label = { Text("${'A' + idx}") })
                        }
                    }
                    OutlinedTextField(value = explanation, onValueChange = { explanation = it }, label = { Text("Explanation") })
                    OutlinedTextField(value = sourceNote, onValueChange = { sourceNote = it }, label = { Text("Source / Date") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = text.length >= 5 && listOf(opt0, opt1, opt2, opt3).all { it.isNotBlank() },
                    onClick = {
                        onSaveQuestion(
                            base.copy(
                                questionText = text,
                                options = listOf(opt0, opt1, opt2, opt3),
                                correctIndex = correctIdx.toLong(),
                                explanation = explanation,
                                sourceNote = sourceNote
                            ),
                            addingManual
                        )
                        addingManual = false
                        editingQuestion = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { addingManual = false; editingQuestion = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AdminAnnouncementManagerSection(
    announcements: List<Announcement>,
    documents: List<StudyDocument>,
    onSaveAnnouncement: (Announcement, Boolean) -> Unit,
    onDeleteAnnouncement: (Announcement) -> Unit
) {
    var editing by remember { mutableStateOf<Announcement?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(onClick = { creating = true }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Announcement")
            }
        }
        if (announcements.isEmpty()) {
            item {
                EmptyStateCard(Icons.Default.Campaign, "No announcements yet", "Create notices, exam alerts, or pin updates for students.")
            }
        } else {
            items(announcements, key = { it.id }) { ann ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (ann.pinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = RoyalPurple, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(ann.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row {
                                IconButton(onClick = { editing = ann }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit announcement")
                                }
                                IconButton(onClick = { onDeleteAnnouncement(ann) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete announcement", tint = RoseError)
                                }
                            }
                        }
                        Text(ann.body, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${ann.publishDateLabel} • ${if (ann.published) "Published" else "Draft"}" +
                                (ann.attachmentFileName?.let { " • Attachment: $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        val today = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
        val base = editing ?: Announcement(publishDateLabel = today, published = true)
        var title by remember(base) { mutableStateOf(base.title) }
        var body by remember(base) { mutableStateOf(base.body) }
        var dateLabel by remember(base) { mutableStateOf(base.publishDateLabel) }
        var pinned by remember(base) { mutableStateOf(base.pinned) }
        var published by remember(base) { mutableStateOf(base.published) }
        var attachDoc by remember(base) { mutableStateOf(documents.firstOrNull { it.id == base.attachmentDocId }) }

        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            title = { Text(if (creating) "Create Announcement" else "Edit Announcement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Announcement Message") })
                    OutlinedTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = { Text("Publish Date") }, singleLine = true)
                    if (documents.isNotEmpty()) {
                        Text("Optional Study Material Attachment:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(selected = attachDoc == null, onClick = { attachDoc = null }, label = { Text("None") })
                            }
                            items(documents.take(8)) { d ->
                                FilterChip(selected = attachDoc?.id == d.id, onClick = { attachDoc = d }, label = { Text(d.title.take(18)) })
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pin to Top")
                        Switch(checked = pinned, onCheckedChange = { pinned = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Published")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && body.isNotBlank(),
                    onClick = {
                        onSaveAnnouncement(
                            base.copy(
                                title = title,
                                body = body,
                                publishDateLabel = dateLabel,
                                pinned = pinned,
                                published = published,
                                attachmentDocId = attachDoc?.id,
                                attachmentFileName = attachDoc?.fileName,
                                attachmentMimeType = attachDoc?.mimeType
                            ),
                            creating
                        )
                        creating = false
                        editing = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { creating = false; editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminAnalyticsSection(
    subjects: List<SubjectItem>,
    documents: List<StudyDocument>,
    students: List<StudentDirectoryEntry>,
    admins: List<AdminAccount>,
    quizQuestions: List<QuizQuestion>,
    reducedMotion: Boolean,
    onExportMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val pdfCount = remember(documents) { documents.count { it.fileExtension.equals("pdf", true) } }
    val imageCount = remember(documents) { documents.count { it.fileExtension.lowercase() in setOf("jpg", "jpeg", "png", "webp") } }
    val totalDownloads = remember(documents) { documents.sumOf { it.downloadCount } }
    val totalQuizAttempts = remember(students) { students.sumOf { it.quizAttemptsCount } }
    val totalStorageBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }

    val subjectDocEntries = remember(subjects, documents) {
        subjects.map { s -> s.nameEn to documents.count { it.subjectId == s.id } }
    }
    val topDocEntries = remember(documents) {
        documents.sortedByDescending { it.downloadCount }.take(5).map { it.title.take(22) to it.downloadCount.toInt() }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Platform Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                val csv = buildString {
                                    appendLine("Metric,Value")
                                    appendLine("TotalStudents,${students.size}")
                                    appendLine("TotalAdmins,${admins.size}")
                                    appendLine("TotalFiles,${documents.size}")
                                    appendLine("TotalPDFs,$pdfCount")
                                    appendLine("TotalImages,$imageCount")
                                    appendLine("TotalDownloads,$totalDownloads")
                                    appendLine("TotalQuizAttempts,$totalQuizAttempts")
                                    appendLine("BankedQuizQuestions,${quizQuestions.size}")
                                    appendLine("StorageBytes,$totalStorageBytes")
                                }
                                FileStorageHelper.shareTextReport(context, "dorbar_analytics.csv", csv)
                                    .onFailure { onExportMessage("Failed to export analytics CSV.") }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }
                    Text("Total Students: ${students.size} • Total Admins: ${admins.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("Total Files: ${documents.size} (PDFs: $pdfCount • Images: $imageCount)", style = MaterialTheme.typography.bodyMedium)
                    Text("Total Downloads: $totalDownloads • Quiz Attempts: $totalQuizAttempts", style = MaterialTheme.typography.bodyMedium)
                    Text("Banked GK Questions: ${quizQuestions.size} • Storage Used: ${FileStorageHelper.formatFileSize(totalStorageBytes)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (subjectDocEntries.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Documents per Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        AnalyticsBarChart(entries = subjectDocEntries, reducedMotion = reducedMotion)
                    }
                }
            }
        }

        if (topDocEntries.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Most Downloaded Study Materials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        AnalyticsBarChart(entries = topDocEntries, reducedMotion = reducedMotion)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStorageManagerSection(
    subjects: List<SubjectItem>,
    documents: List<StudyDocument>,
    appSettings: AppSettings,
    isSuperAdmin: Boolean,
    onUpdateAppSettings: (AppSettings) -> Unit
) {
    val totalBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }
    val byExtension = remember(documents) {
        documents.groupBy { it.fileExtension.uppercase() }
            .mapValues { (_, list) -> list.sumOf { it.fileSizeBytes } }
            .entries.sortedByDescending { it.value }
    }
    var limitMb by remember(appSettings.maxUploadSizeMb) { mutableStateOf(appSettings.maxUploadSizeMb.toFloat()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Total Cloud Storage Consumption", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(FileStorageHelper.formatFileSize(totalBytes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Across ${documents.size} uploaded documents and ${subjects.size} subjects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Configurable Upload Limit: ${limitMb.roundToLong()} MB", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Slider(
                        value = limitMb,
                        onValueChange = { limitMb = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        enabled = isSuperAdmin
                    )
                    if (isSuperAdmin) {
                        Button(
                            onClick = { onUpdateAppSettings(appSettings.copy(maxUploadSizeMb = limitMb.roundToLong())) },
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Apply Upload Limit") }
                    } else {
                        Text("Only Super Admin can change security & upload limits.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text("Breakdown by File Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(byExtension) { (ext, bytes) ->
            val ratio = if (totalBytes > 0) bytes.toFloat() / totalBytes.toFloat() else 0f
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(ext, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(FileStorageHelper.formatFileSize(bytes), style = MaterialTheme.typography.bodySmall)
                    }
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)))
                }
            }
        }
    }
}

@Composable
fun AdminTeamManagementSection(
    admins: List<AdminAccount>,
    invites: List<AdminInvite>,
    isSuperAdmin: Boolean,
    onInviteAdmin: (String, String, AdminRole, List<String>) -> Unit,
    onUpdateAdmin: (AdminAccount, String, AdminRole, Boolean, List<String>) -> Unit,
    onRemoveAdmin: (AdminAccount) -> Unit,
    onRemoveInvite: (AdminInvite) -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var editingAdmin by remember { mutableStateOf<AdminAccount?>(null) }
    val activeCount = admins.count { it.active }
    val inactiveCount = admins.count { !it.active }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Multi-Admin Team (${admins.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Active: $activeCount • Inactive: $inactiveCount • Invites: ${invites.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSuperAdmin) {
                            Button(
                                onClick = { showInviteDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("invite_admin_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Admin")
                            }
                        }
                    }
                    if (!isSuperAdmin) {
                        Text(
                            "Super Admin authority is required to invite, edit roles, or remove administrators.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        items(admins, key = { it.uid }) { acc ->
            val role = AdminRole.safeParse(acc.role)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(acc.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = if (role == AdminRole.SUPER_ADMIN) WarmGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(role.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Text(
                        "Status: ${if (acc.active) "Active" else "Suspended"} • Permissions: ${acc.permissions.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (isSuperAdmin && role != AdminRole.SUPER_ADMIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingAdmin = acc }, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Role & Access")
                            }
                            TextButton(onClick = { onRemoveAdmin(acc) }) {
                                Text("Remove", color = RoseError)
                            }
                        }
                    }
                }
            }
        }

        if (invites.isNotEmpty()) {
            item {
                Text("Invited Administrators (${invites.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(invites, key = { it.id }) { inv ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inv.email, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${AdminRole.safeParse(inv.role).label} • ${if (inv.claimedUid.isNullOrBlank()) "Pending Google Sign-In" else "Activated"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSuperAdmin) {
                            IconButton(onClick = { onRemoveInvite(inv) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke invite", tint = RoseError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        AdminFormDialog(
            title = "Invite New Administrator (Admin 2+)",
            initialEmail = "",
            emailEditable = true,
            initialName = "",
            initialRole = AdminRole.ADMINISTRATOR,
            initialActive = true,
            showActiveToggle = false,
            initialPermissions = listOf("MANAGE_DOCS", "MANAGE_CATEGORIES", "MANAGE_QUIZ", "MANAGE_ANNOUNCEMENTS"),
            onDismiss = { showInviteDialog = false },
            onSubmit = { email, name, role, _, perms ->
                onInviteAdmin(email, name, role, perms)
                showInviteDialog = false
            }
        )
    }

    editingAdmin?.let { target ->
        AdminFormDialog(
            title = "Edit ${target.displayName}",
            initialEmail = target.email,
            emailEditable = false,
            initialName = target.displayName,
            initialRole = AdminRole.safeParse(target.role),
            initialActive = target.active,
            showActiveToggle = true,
            initialPermissions = target.permissions,
            onDismiss = { editingAdmin = null },
            onSubmit = { _, name, role, active, perms ->
                onUpdateAdmin(target, name, role, active, perms)
                editingAdmin = null
            }
        )
    }
}

@Composable
private fun AdminFormDialog(
    title: String,
    initialEmail: String,
    emailEditable: Boolean,
    initialName: String,
    initialRole: AdminRole,
    initialActive: Boolean,
    showActiveToggle: Boolean,
    initialPermissions: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, AdminRole, Boolean, List<String>) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var name by remember { mutableStateOf(initialName) }
    var role by remember { mutableStateOf(initialRole) }
    var active by remember { mutableStateOf(initialActive) }
    val selectedPerms = remember { mutableStateListOf<String>().apply { addAll(initialPermissions) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    enabled = emailEditable,
                    label = { Text("Google Email Address") },
                    singleLine = true
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                Text("Role:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(AdminRole.ADMINISTRATOR, AdminRole.MODERATOR).forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                    }
                }
                if (showActiveToggle) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Account Active")
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
                Text("Permissions:", style = MaterialTheme.typography.labelMedium)
                DefaultCatalog.allAdminPermissions.filter { it != "MANAGE_ADMINS" && it != "MANAGE_SETTINGS" }.forEach { perm ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = perm in selectedPerms,
                            onCheckedChange = { checked -> if (checked) selectedPerms.add(perm) else selectedPerms.remove(perm) }
                        )
                        Text(perm.replace("_", " "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = email.contains("@") && name.isNotBlank(),
                onClick = { onSubmit(email, name, role, active, selectedPerms.toList()) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminActivityLogsSection(logs: List<AdminActivityLog>) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") }
    val types = remember(logs) { listOf("ALL") + logs.map { it.actionType }.distinct().sorted() }
    val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    val filtered = remember(logs, query, typeFilter) {
        logs.filter { log ->
            (typeFilter == "ALL" || log.actionType == typeFilter) &&
                (query.isBlank() || log.summary.contains(query, true) || log.adminName.contains(query, true))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Filter audit trail…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(types) { t ->
                    FilterChip(selected = typeFilter == t, onClick = { typeFilter = t }, label = { Text(t) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyStateCard(Icons.Default.ReceiptLong, "No logs found", "Administrative changes will appear here chronologically.") }
        } else {
            items(filtered, key = { it.id }) { log ->
                val timeStr = log.createdAt?.toDate()?.let { formatter.format(it) } ?: "Just now"
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("${log.adminName} • ${log.actionType}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(log.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAppSettingsSection(
    appSettings: AppSettings,
    isSuperAdmin: Boolean,
    onSaveSettings: (AppSettings) -> Unit
) {
    var appName by remember(appSettings) { mutableStateOf(appSettings.appName) }
    var tagline by remember(appSettings) { mutableStateOf(appSettings.tagline) }
    var theme by remember(appSettings) { mutableStateOf(appSettings.defaultThemeMode) }
    var allowQuiz by remember(appSettings) { mutableStateOf(appSettings.allowStudentQuiz) }
    var allowDownloads by remember(appSettings) { mutableStateOf(appSettings.allowStudentDownloads) }
    var dailyChallenge by remember(appSettings) { mutableStateOf(appSettings.dailyChallengeEnabled) }
    var notifications by remember(appSettings) { mutableStateOf(appSettings.notificationsEnabled) }
    var maxUploadMb by remember(appSettings) { mutableStateOf(appSettings.maxUploadSizeMb.toFloat()) }
    var maintenanceMode by remember(appSettings) { mutableStateOf(appSettings.maintenanceMode) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Platform Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("Application Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Default Theme Mode", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { m ->
                            FilterChip(selected = theme == m, onClick = { theme = m }, label = { Text(m) })
                        }
                    }
                    SettingToggleRow("Enable AI GK Quiz", allowQuiz) { allowQuiz = it }
                    SettingToggleRow("Allow Student Downloads", allowDownloads) { allowDownloads = it }
                    SettingToggleRow("Enable Daily GK Challenge", dailyChallenge) { dailyChallenge = it }
                    SettingToggleRow("Student Notifications", notifications) { notifications = it }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Super Admin Security Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Max Upload Size: ${maxUploadMb.roundToLong()} MB", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = maxUploadMb,
                        onValueChange = { maxUploadMb = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        enabled = isSuperAdmin
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Maintenance Mode", style = MaterialTheme.typography.titleSmall)
                            Text("Temporarily restricts student dashboard access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = maintenanceMode, enabled = isSuperAdmin, onCheckedChange = { maintenanceMode = it })
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSaveSettings(
                        appSettings.copy(
                            appName = appName,
                            tagline = tagline,
                            defaultThemeMode = theme,
                            allowStudentQuiz = allowQuiz,
                            allowStudentDownloads = allowDownloads,
                            dailyChallengeEnabled = dailyChallenge,
                            notificationsEnabled = notifications,
                            maxUploadSizeMb = if (isSuperAdmin) maxUploadMb.roundToLong() else appSettings.maxUploadSizeMb,
                            maintenanceMode = if (isSuperAdmin) maintenanceMode else appSettings.maintenanceMode
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_save_settings_button")
            ) {
                Text("Save Platform Settings")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
