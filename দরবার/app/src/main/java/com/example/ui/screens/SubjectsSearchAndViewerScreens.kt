package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.FileStorageHelper
import com.example.data.model.CategoryItem
import com.example.data.model.FavoriteRecord
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.example.ui.components.DocumentRowCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectVisualCard
import com.example.ui.components.parseHexColor
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotoBengaliFontFamily
import com.example.ui.viewmodel.DocumentPreviewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubjectsHubScreen(
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    documents: List<StudyDocument>,
    favorites: List<FavoriteRecord>,
    selectedSubjectId: String?,
    onSelectSubject: (String?) -> Unit,
    onOpenDocument: (StudyDocument) -> Unit,
    onDownloadDocument: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val favIds = remember(favorites) { favorites.map { it.documentId }.toSet() }
    val activeSubject = subjects.firstOrNull { it.id == selectedSubjectId }

    if (activeSubject != null) {
        BackHandler { onSelectSubject(null) }
        SubjectDetailView(
            subject = activeSubject,
            categories = categories.filter { it.subjectId == "ALL" || it.subjectId == activeSubject.id },
            documents = documents.filter { it.subjectId == activeSubject.id },
            favIds = favIds,
            onBack = { onSelectSubject(null) },
            onOpenDocument = onOpenDocument,
            onDownloadDocument = onDownloadDocument,
            onToggleFavorite = onToggleFavorite,
            modifier = modifier
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Six Subject Libraries",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select any subject to explore chapters, PDFs, diagrams, and suggestion papers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(subjects.chunked(2)) { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pair.forEach { subj ->
                        SubjectVisualCard(
                            subject = subj,
                            documentCount = documents.count { it.subjectId == subj.id },
                            onClick = { onSelectSubject(subj.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SubjectDetailView(
    subject: SubjectItem,
    categories: List<CategoryItem>,
    documents: List<StudyDocument>,
    favIds: Set<String>,
    onBack: () -> Unit,
    onOpenDocument: (StudyDocument) -> Unit,
    onDownloadDocument: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedChapter by remember { mutableStateOf("ALL") }
    var selectedType by remember { mutableStateOf("ALL") }
    var query by remember { mutableStateOf("") }

    val chapters = remember(documents) {
        documents.map { it.chapter }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val fileTypes = remember(documents) {
        documents.map { it.fileExtension.uppercase() }.distinct().sorted()
    }

    val filtered = remember(documents, selectedCategory, selectedChapter, selectedType, query) {
        documents.filter { doc ->
            (selectedCategory == "ALL" || doc.categoryId == selectedCategory || doc.categoryName == selectedCategory) &&
                (selectedChapter == "ALL" || doc.chapter == selectedChapter) &&
                (selectedType == "ALL" || doc.fileExtension.equals(selectedType, ignoreCase = true)) &&
                (query.isBlank() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    doc.chapter.contains(query, ignoreCase = true) ||
                    doc.description.contains(query, ignoreCase = true))
        }
    }

    val accent = parseHexColor(subject.accentHex)

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(NavyDeep, accent)))
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to subjects",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${subject.nameBn} • ${subject.nameEn}",
                                fontFamily = NotoBengaliFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Text(
                                text = subject.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search inside ${subject.nameEn}…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // Category filter chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("All Categories") }
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { selectedCategory = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }
        }

        // Chapter & file type filters
        if (chapters.isNotEmpty() || fileTypes.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (chapters.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = selectedChapter == "ALL",
                                onClick = { selectedChapter = "ALL" },
                                label = { Text("All Chapters") }
                            )
                        }
                        items(chapters) { ch ->
                            FilterChip(
                                selected = selectedChapter == ch,
                                onClick = { selectedChapter = ch },
                                label = { Text(ch) }
                            )
                        }
                    }
                    items(fileTypes) { ext ->
                        FilterChip(
                            selected = selectedType == ext,
                            onClick = { selectedType = if (selectedType == ext) "ALL" else ext },
                            label = { Text(ext) }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.FolderOpen,
                        title = "No matching materials in ${subject.nameEn}",
                        subtitle = "Try switching category or chapter filters, or check back after new uploads."
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { doc ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DocumentRowCard(
                        doc = doc,
                        isFavorite = doc.id in favIds,
                        onOpenViewer = { onOpenDocument(doc) },
                        onDownload = { onDownloadDocument(doc) },
                        onToggleFavorite = { onToggleFavorite(doc, doc.id in favIds) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchModal(
    subjects: List<SubjectItem>,
    categories: List<CategoryItem>,
    documents: List<StudyDocument>,
    favorites: List<FavoriteRecord>,
    onDismiss: () -> Unit,
    onOpenDocument: (StudyDocument) -> Unit,
    onDownloadDocument: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var subjectFilter by remember { mutableStateOf("ALL") }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var typeFilter by remember { mutableStateOf("ALL") }
    val favIds = remember(favorites) { favorites.map { it.documentId }.toSet() }
    val types = remember(documents) { documents.map { it.fileExtension.uppercase() }.distinct().sorted() }

    val results = remember(documents, query, subjectFilter, categoryFilter, typeFilter) {
        documents.filter { doc ->
            val matchesQuery = query.isBlank() ||
                doc.title.contains(query, ignoreCase = true) ||
                doc.description.contains(query, ignoreCase = true) ||
                doc.chapter.contains(query, ignoreCase = true) ||
                doc.subjectName.contains(query, ignoreCase = true) ||
                doc.categoryName.contains(query, ignoreCase = true) ||
                doc.tags.any { it.contains(query, ignoreCase = true) }
            matchesQuery &&
                (subjectFilter == "ALL" || doc.subjectId == subjectFilter) &&
                (categoryFilter == "ALL" || doc.categoryId == categoryFilter) &&
                (typeFilter == "ALL" || doc.fileExtension.equals(typeFilter, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search & Filter Library") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search subjects, chapters, titles, tags…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_dialog_input")
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Filters", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = subjectFilter == "ALL",
                                onClick = { subjectFilter = "ALL" },
                                label = { Text("All Subjects") }
                            )
                        }
                        items(subjects) { s ->
                            FilterChip(
                                selected = subjectFilter == s.id,
                                onClick = { subjectFilter = s.id },
                                label = { Text(s.nameEn) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = categoryFilter == "ALL",
                                onClick = { categoryFilter = "ALL" },
                                label = { Text("All Categories") }
                            )
                        }
                        items(categories) { c ->
                            FilterChip(
                                selected = categoryFilter == c.id,
                                onClick = { categoryFilter = c.id },
                                label = { Text(c.name) }
                            )
                        }
                    }
                    if (types.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(types) { ext ->
                                FilterChip(
                                    selected = typeFilter == ext,
                                    onClick = { typeFilter = if (typeFilter == ext) "ALL" else ext },
                                    label = { Text(ext) }
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "${results.size} result(s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(results, key = { it.id }) { doc ->
                    DocumentRowCard(
                        doc = doc,
                        isFavorite = doc.id in favIds,
                        onOpenViewer = { onOpenDocument(doc) },
                        onDownload = { onDownloadDocument(doc) },
                        onToggleFavorite = { onToggleFavorite(doc, doc.id in favIds) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerModal(
    previewState: DocumentPreviewState,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onDownload: (openAfter: Boolean) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val doc = previewState.document ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateLabel = doc.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Recent"

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(doc.mimeType)
    ) { targetUri ->
        val bytes = previewState.bytes ?: return@rememberLauncherForActivityResult
        if (targetUri != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openOutputStream(targetUri)?.use { it.write(bytes) }
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = doc.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close viewer")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle favourite",
                                tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(doc.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${doc.subjectName} • ${doc.categoryName} • ${doc.chapter}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (doc.description.isNotBlank()) {
                                Text(doc.description, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetaBadge("${doc.fileExtension.uppercase()} • ${FileStorageHelper.formatFileSize(doc.fileSizeBytes)}")
                                MetaBadge("Uploaded: $dateLabel")
                                MetaBadge("v${doc.version}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onDownload(false) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("viewer_download_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download")
                                }
                                OutlinedButton(
                                    onClick = { onDownload(true) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open App")
                                }
                            }
                            if (previewState.bytes != null) {
                                OutlinedButton(
                                    onClick = { saveAsLauncher.launch(doc.fileName) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Copy to Device Folder")
                                }
                            }
                        }
                    }
                }

                when {
                    previewState.loading -> {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Loading document preview…", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    !previewState.error.isNullOrBlank() -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = previewState.error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    previewState.pdfPages.isNotEmpty() -> {
                        itemsIndexed(previewState.pdfPages) { index, bmp ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Page ${index + 1} of ${previewState.pdfPages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF Page ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                    previewState.imageBitmap != null -> {
                        item {
                            Image(
                                bitmap = previewState.imageBitmap.asImageBitmap(),
                                contentDescription = doc.title,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                    previewState.textContent != null -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                Text(
                                    text = previewState.textContent,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Description,
                                title = "Ready for Download (.${doc.fileExtension.uppercase()})",
                                subtitle = "Tap 'Download' or 'Open App' above to view this ${doc.fileExtension.uppercase()} file in your preferred office viewer."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
