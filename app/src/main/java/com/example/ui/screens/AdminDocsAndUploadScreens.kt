package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.DocumentStorageHelper
import com.example.core.PickedFilePayload
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminUser
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.DocumentVersion
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FileTypeBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.components.subjectColorFor
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.UploadQueueItem
import kotlinx.coroutines.launch

@Composable
fun AdminDocumentManagerSection(
    adminUser: AdminUser,
    documents: List<StudyDocument>,
    subjects: List<Subject>,
    categories: List<Category>,
    versionHistory: List<DocumentVersion>,
    maxUploadMb: Long,
    onLoadVersions: (String) -> Unit,
    onEditDocument: (StudyDocument, String, String, Subject, Category, String, String, Boolean, Boolean, PickedFilePayload?, String) -> Unit,
    onBulkPublish: (List<String>, Boolean) -> Unit,
    onBulkDelete: (List<String>) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_DOCUMENTS)
    var search by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PUBLISHED, DRAFT
    val selectedIds = remember { mutableStateListOf<String>() }

    var editingDoc by remember { mutableStateOf<StudyDocument?>(null) }
    var historyDoc by remember { mutableStateOf<StudyDocument?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }

    val filtered = remember(documents, search, subjectFilter, statusFilter) {
        val q = search.trim().lowercase()
        documents.filter { d ->
            val matchQ = q.isBlank() || d.title.lowercase().contains(q) || d.chapter.lowercase().contains(q) || d.fileName.lowercase().contains(q)
            val matchS = subjectFilter == null || d.subjectId == subjectFilter
            val matchStatus = when (statusFilter) {
                "PUBLISHED" -> d.published
                "DRAFT" -> !d.published
                else -> true
            }
            matchQ && matchS && matchStatus
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search documents, chapters, filenames...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("admin_doc_search")
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All (${documents.size})", "PUBLISHED" to "Published", "DRAFT" to "Unpublished").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = subjectFilter == null,
                    onClick = { subjectFilter = null },
                    label = { Text("All Subjects") }
                )
                subjects.forEach { subj ->
                    FilterChip(
                        selected = subjectFilter == subj.id,
                        onClick = { subjectFilter = if (subjectFilter == subj.id) null else subj.id },
                        label = { Text(subj.titleEn) }
                    )
                }
            }

            if (selectedIds.isNotEmpty() && canManage) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${selectedIds.size} selected", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onBulkPublish(selectedIds.toList(), true); selectedIds.clear() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Publish, contentDescription = "Bulk publish", tint = EmeraldSuccess)
                            }
                            IconButton(onClick = { onBulkPublish(selectedIds.toList(), false); selectedIds.clear() }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Unpublished, contentDescription = "Bulk unpublish")
                            }
                            IconButton(onClick = { confirmBulkDelete = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Bulk delete", tint = CrimsonError)
                            }
                        }
                    }
                }
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
                        icon = Icons.Default.UploadFile,
                        title = "No Documents Found",
                        subtitle = "Upload new materials or adjust your search/status filters."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { doc ->
                    val checked = doc.id in selectedIds
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("admin_doc_${doc.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canManage) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedIds.add(doc.id) else selectedIds.remove(doc.id)
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FileTypeBadge(doc.fileType)
                                    Surface(
                                        color = (if (doc.published) EmeraldSuccess else CrimsonError).copy(alpha = 0.14f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (doc.published) "Published" else "Draft",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (doc.published) EmeraldSuccess else CrimsonError,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text("v${doc.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${doc.subjectName} • ${doc.categoryName} • Ch: ${doc.chapter}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${DocumentStorageHelper.formatBytes(doc.fileSizeBytes)} • ${doc.downloadCount} downloads • By ${doc.uploadedByName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                IconButton(
                                    onClick = { onLoadVersions(doc.id); historyDoc = doc },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "Version history")
                                }
                                if (canManage) {
                                    IconButton(
                                        onClick = { editingDoc = doc },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit document", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingDoc != null) {
        EditDocumentDialog(
            doc = editingDoc!!,
            subjects = subjects,
            categories = categories,
            maxUploadMb = maxUploadMb,
            onDismiss = { editingDoc = null },
            onSave = { title, desc, subj, cat, chap, tags, pub, feat, replacement, note ->
                onEditDocument(editingDoc!!, title, desc, subj, cat, chap, tags, pub, feat, replacement, note)
                editingDoc = null
            }
        )
    }

    if (historyDoc != null) {
        AlertDialog(
            onDismissRequest = { historyDoc = null },
            title = { Text("Version History • ${historyDoc!!.title}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (versionHistory.isEmpty()) {
                        Text("Loading version log...")
                    } else {
                        versionHistory.forEach { ver ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                                Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                                    Text("v${ver.version} • ${ver.fileName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    Text("${ver.changeNote} (${DocumentStorageHelper.formatBytes(ver.fileSizeBytes)})", style = MaterialTheme.typography.bodySmall)
                                    Text("Edited by ${ver.editedByName} • ${formatTimestamp(ver.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyDoc = null }) { Text("Close") } }
        )
    }

    if (confirmBulkDelete) {
        AlertDialog(
            onDismissRequest = { confirmBulkDelete = false },
            title = { Text("Delete ${selectedIds.size} Document(s)?") },
            text = { Text("This permanently removes the selected documents and their file chunks.") },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkDelete(selectedIds.toList())
                        selectedIds.clear()
                        confirmBulkDelete = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmBulkDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EditDocumentDialog(
    doc: StudyDocument,
    subjects: List<Subject>,
    categories: List<Category>,
    maxUploadMb: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, Subject, Category, String, String, Boolean, Boolean, PickedFilePayload?, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(doc.id) { mutableStateOf(doc.title) }
    var description by remember(doc.id) { mutableStateOf(doc.description) }
    var selectedSubj by remember(doc.id) { mutableStateOf(subjects.firstOrNull { it.id == doc.subjectId } ?: subjects.firstOrNull()) }
    var selectedCat by remember(doc.id) { mutableStateOf(categories.firstOrNull { it.id == doc.categoryId } ?: categories.firstOrNull()) }
    var chapter by remember(doc.id) { mutableStateOf(doc.chapter) }
    var tagsCsv by remember(doc.id) { mutableStateOf(doc.tags.joinToString(", ")) }
    var published by remember(doc.id) { mutableStateOf(doc.published) }
    var featured by remember(doc.id) { mutableStateOf(doc.featured) }
    var changeNote by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf<PickedFilePayload?>(null) }
    var replaceErr by remember { mutableStateOf<String?>(null) }

    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val maxBytes = maxUploadMb.coerceIn(1L, 10L) * 1024L * 1024L
                DocumentStorageHelper.readPickedFile(context, uri, maxBytes)
                    .onSuccess { replacement = it; replaceErr = null }
                    .onFailure { replaceErr = it.localizedMessage }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document (v${doc.version})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Document Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tagsCsv, onValueChange = { tagsCsv = it }, label = { Text("Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    subjects.forEach { s ->
                        FilterChip(selected = selectedSubj?.id == s.id, onClick = { selectedSubj = s }, label = { Text(s.titleEn) })
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { c ->
                        FilterChip(selected = selectedCat?.id == c.id, onClick = { selectedCat = c }, label = { Text(c.name) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Published (Visible to Students)")
                    Switch(checked = published, onCheckedChange = { published = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Featured Material")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                OutlinedButton(onClick = { replacePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(replacement?.fileName ?: "Replace File (Current: ${doc.fileName})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (replaceErr != null) {
                    Text(replaceErr!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = changeNote,
                    onValueChange = { changeNote = it },
                    label = { Text("Version Change Note") },
                    placeholder = { Text("e.g., Fixed diagram & updated notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && selectedSubj != null && selectedCat != null,
                onClick = {
                    onSave(title, description, selectedSubj!!, selectedCat!!, chapter, tagsCsv, published, featured, replacement, changeNote)
                }
            ) { Text("Save Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminUploadMaterialsSection(
    adminUser: AdminUser,
    appSettings: AppSettings,
    subjects: List<Subject>,
    categories: List<Category>,
    uploadQueue: List<UploadQueueItem>,
    isUploading: Boolean,
    onStageFiles: (List<Uri>) -> Unit,
    onRemoveQueued: (Uri) -> Unit,
    onClearQueue: () -> Unit,
    onUploadAll: (String, String, Subject, Category, String, String, Boolean, Boolean) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_DOCUMENTS)
    var selectedSubj by remember(subjects) { mutableStateOf(subjects.firstOrNull()) }
    var selectedCat by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var customTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("Chapter 1") }
    var tagsCsv by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(true) }
    var featured by remember { mutableStateOf(false) }

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onStageFiles(uris)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!canManage) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.CloudUpload,
                    title = "Upload Permission Required",
                    subtitle = "Your admin role does not include document upload permissions."
                )
            }
            return@LazyColumn
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bulk & Single Study Material Upload", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Supported: PDF, JPG, JPEG, PNG, WEBP, DOC, DOCX, PPT, PPTX, XLS, XLSX, TXT • Max ${appSettings.maxUploadMb} MB per file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { multiFilePicker.launch(arrayOf("*/*")) },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pick_upload_files_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select One or Multiple Files")
                    }
                }
            }
        }

        if (uploadQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upload Queue (${uploadQueue.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearQueue, enabled = !isUploading) { Text("Clear Queue") }
                }
            }
            items(uploadQueue, key = { it.uri.toString() }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = item.error ?: "${item.payload?.fileType ?: "INVALID"} • ${item.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemoveQueued(item.uri) }, enabled = !isUploading, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove queued file")
                            }
                        }
                        if (item.progress > 0f && item.progress < 1f) {
                            LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Classification & Metadata", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    if (uploadQueue.size <= 1) {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Document Title (optional override)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { s ->
                            FilterChip(
                                selected = selectedSubj?.id == s.id,
                                onClick = { selectedSubj = s },
                                label = { Text("${s.titleEn} (${s.titleBn})") }
                            )
                        }
                    }

                    Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { c ->
                            FilterChip(
                                selected = selectedCat?.id == c.id,
                                onClick = { selectedCat = c },
                                label = { Text(c.name) }
                            )
                        }
                    }

                    OutlinedTextField(value = chapter, onValueChange = { chapter = it }, label = { Text("Chapter Name / Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Study Notes Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tagsCsv, onValueChange = { tagsCsv = it }, label = { Text("Custom Tags (comma separated)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Publish Immediately")
                        Switch(checked = published, onCheckedChange = { published = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Mark as Featured")
                        Switch(checked = featured, onCheckedChange = { featured = it })
                    }

                    Button(
                        onClick = {
                            if (selectedSubj != null && selectedCat != null) {
                                onUploadAll(customTitle, description, selectedSubj!!, selectedCat!!, chapter, tagsCsv, published, featured)
                            }
                        },
                        enabled = !isUploading && uploadQueue.any { it.payload != null } && selectedSubj != null && selectedCat != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("upload_all_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isUploading) "Uploading Materials..." else "Publish & Upload Files")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSubjectManagerSection(
    adminUser: AdminUser,
    subjects: List<Subject>,
    onSaveSubject: (Subject) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_SUBJECTS)
    var editingSubject by remember { mutableStateOf<Subject?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(subjects, key = { it.id }) { subj ->
            val accent = subjectColorFor(subj.id, subj.accentHex)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("#${subj.orderIndex} • ${subj.titleEn} (${subj.titleBn})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
                        Text(subj.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (subj.visible) "Visible to students" else "Hidden", style = MaterialTheme.typography.labelSmall, color = if (subj.visible) EmeraldSuccess else CrimsonError)
                    }
                    if (canManage) {
                        IconButton(onClick = { editingSubject = subj }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit subject")
                        }
                    }
                }
            }
        }
    }

    if (editingSubject != null) {
        val subj = editingSubject!!
        var titleEn by remember(subj.id) { mutableStateOf(subj.titleEn) }
        var titleBn by remember(subj.id) { mutableStateOf(subj.titleBn) }
        var desc by remember(subj.id) { mutableStateOf(subj.description) }
        var visible by remember(subj.id) { mutableStateOf(subj.visible) }

        AlertDialog(
            onDismissRequest = { editingSubject = null },
            title = { Text("Edit Subject: ${subj.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("English Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("Bengali Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Visible to Students")
                        Switch(checked = visible, onCheckedChange = { visible = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = titleEn.isNotBlank() && titleBn.isNotBlank(),
                    onClick = {
                        onSaveSubject(subj.copy(titleEn = titleEn, titleBn = titleBn, description = desc, visible = visible))
                        editingSubject = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingSubject = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AdminCategoryManagerSection(
    adminUser: AdminUser,
    categories: List<Category>,
    onSaveCategory: (Category, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val canManage = adminUser.hasPermission(AdminPermissions.MANAGE_CATEGORIES)
    var editingCat by remember { mutableStateOf<Pair<Category, Boolean>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canManage) {
            item {
                Button(
                    onClick = {
                        editingCat = Category(orderIndex = (categories.size + 1).toLong(), visible = true) to true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_category_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create New Category")
                }
            }
        }
        items(categories, key = { it.id }) { cat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (cat.nameBn.isNotBlank()) {
                            Text(cat.nameBn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (cat.description.isNotBlank()) {
                            Text(cat.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (canManage) {
                        IconButton(onClick = { editingCat = cat to false }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit category")
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = CrimsonError)
                        }
                    }
                }
            }
        }
    }

    if (editingCat != null) {
        val (cat, isNew) = editingCat!!
        var name by remember(cat.id) { mutableStateOf(cat.name) }
        var nameBn by remember(cat.id) { mutableStateOf(cat.nameBn) }
        var desc by remember(cat.id) { mutableStateOf(cat.description) }
        var visible by remember(cat.id) { mutableStateOf(cat.visible) }

        AlertDialog(
            onDismissRequest = { editingCat = null },
            title = { Text(if (isNew) "Create Category" else "Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Bengali Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
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
                        onSaveCategory(cat.copy(name = name, nameBn = nameBn, description = desc, visible = visible), isNew)
                        editingCat = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingCat = null }) { Text("Cancel") } }
        )
    }
}
