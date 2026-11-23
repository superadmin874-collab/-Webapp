package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.StudentFavorite
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.example.ui.components.DocumentListItemCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.subjectColorFor
import com.example.ui.components.subjectIconFor

@Composable
fun SubjectsDirectoryScreen(
    subjects: List<Subject>,
    allDocs: List<StudyDocument>,
    onOpenSubject: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Subjects • বিষয়সমূহ",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select a subject to browse chapter-wise notes, PDFs, diagrams & question papers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(subjects, key = { it.id }) { subject ->
            val accent = subjectColorFor(subject.id, subject.accentHex)
            val docsForSubject = allDocs.filter { it.subjectId == subject.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSubject(subject) }
                    .testTag("directory_subject_${subject.id}"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = subjectIconFor(subject.iconKey),
                            contentDescription = subject.titleEn,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${subject.titleEn} (${subject.titleBn})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subject.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${docsForSubject.size} published file(s) • ${docsForSubject.sumOf { it.downloadCount }} downloads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectDetailScreen(
    subject: Subject,
    categories: List<Category>,
    allDocs: List<StudyDocument>,
    favorites: List<StudentFavorite>,
    downloadingDocId: String?,
    onBack: () -> Unit,
    onPreviewDoc: (StudyDocument) -> Unit,
    onDownloadDoc: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val subjectDocs = remember(allDocs, subject.id) {
        allDocs.filter { it.subjectId == subject.id }
    }
    val chapters = remember(subjectDocs) {
        subjectDocs.map { it.chapter }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val favoriteIds = remember(favorites) { favorites.map { it.docId }.toSet() }
    val accent = subjectColorFor(subject.id, subject.accentHex)

    val filtered = remember(subjectDocs, selectedCategoryId, selectedChapter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        subjectDocs.filter { doc ->
            (selectedCategoryId == null || doc.categoryId == selectedCategoryId) &&
                (selectedChapter == null || doc.chapter.equals(selectedChapter, ignoreCase = true)) &&
                (q.isBlank() || doc.title.lowercase().contains(q) || doc.chapter.lowercase().contains(q) || doc.description.lowercase().contains(q))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Dedicated Subject Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${subject.titleEn} • ${subject.titleBn}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${subjectDocs.size} materials available",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search within ${subject.titleEn}...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Categories", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("All (${subjectDocs.size})") }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        val cnt = subjectDocs.count { it.categoryId == cat.id }
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text("${cat.name} ($cnt)") }
                        )
                    }
                }
            }

            if (chapters.isNotEmpty()) {
                item {
                    Text("Chapters", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedChapter == null,
                                onClick = { selectedChapter = null },
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
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "No Materials in This Section",
                        subtitle = "Select another category or check back when educators upload new study resources."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { doc ->
                    DocumentListItemCard(
                        doc = doc,
                        isFavorite = doc.id in favoriteIds,
                        isDownloading = downloadingDocId == doc.id,
                        onPreview = { onPreviewDoc(doc) },
                        onDownload = { onDownloadDoc(doc) },
                        onToggleFavorite = { onToggleFavorite(doc, doc.id in favoriteIds) }
                    )
                }
            }
        }
    }
}
