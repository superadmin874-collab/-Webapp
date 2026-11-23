package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AppSettings
import com.example.data.model.Category
import com.example.data.model.StudentDownload
import com.example.data.model.StudentFavorite
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.data.model.Subject
import com.example.ui.components.DocumentListItemCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.darbarHeroBrush
import com.example.ui.components.subjectColorFor
import com.example.ui.components.subjectIconFor
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.NotoSansBengaliFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.SearchFilterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    student: StudentProfile?,
    appSettings: AppSettings,
    subjects: List<Subject>,
    categories: List<Category>,
    allPublishedDocs: List<StudyDocument>,
    filteredDocs: List<StudyDocument>,
    searchFilter: SearchFilterState,
    announcements: List<Announcement>,
    favorites: List<StudentFavorite>,
    downloads: List<StudentDownload>,
    downloadingDocId: String?,
    isWideLayout: Boolean,
    onSearchChange: (String) -> Unit,
    onApplyFilters: (String?, String?, String?, String?, Int?) -> Unit,
    onResetFilters: () -> Unit,
    onOpenSubject: (Subject) -> Unit,
    onOpenQuizTab: () -> Unit,
    onOpenDownloadsTab: () -> Unit,
    onPreviewDoc: (StudyDocument) -> Unit,
    onDownloadDoc: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    val favoriteIds = remember(favorites) { favorites.map { it.docId }.toSet() }
    val hasActiveFilter = searchFilter.query.isNotBlank() ||
        searchFilter.subjectId != null ||
        searchFilter.categoryId != null ||
        searchFilter.fileType != null ||
        searchFilter.dateWindowDays != null

    val recentDocs = remember(allPublishedDocs) { allPublishedDocs.take(6) }
    val popularDocs = remember(allPublishedDocs) {
        allPublishedDocs.sortedByDescending { it.downloadCount }.take(5)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Welcome Hero Banner
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(darbarHeroBrush())
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_darbar_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.22f,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "ID: ${student?.studentCode ?: "DRB-STUDENT"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WarmGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${allPublishedDocs.size} Study Materials",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "দরবার • Darbar",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = NotoSansBengaliFamily),
                            color = WarmGold,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "স্বাগতম, ${student?.fullName ?: "Scholar"}!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appSettings.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Global Search Bar & Filter Trigger
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchFilter.query,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search subjects, chapters, PDFs, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchFilter.query.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("global_search_input")
                )
                Surface(
                    color = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .size(52.dp)
                        .clickable { showFilterSheet = true }
                        .testTag("global_filter_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter documents",
                            tint = if (hasActiveFilter) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search Results Mode
        if (hasActiveFilter) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Search Results (${filteredDocs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onResetFilters) {
                        Text("Clear All")
                    }
                }
            }
            if (filteredDocs.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Search,
                        title = "No Matching Materials",
                        subtitle = "Try clearing filters or searching another chapter or subject."
                    )
                }
            } else {
                items(filteredDocs, key = { it.id }) { doc ->
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
        } else {
            // 3. Announcements Strip
            if (appSettings.announcementsEnabled && announcements.isNotEmpty()) {
                item {
                    SectionHeader(title = "Important Announcements", subtitle = "Latest notices from educators")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(announcements, key = { it.id }) { notice ->
                            Card(
                                modifier = Modifier
                                    .width(280.dp)
                                    .clickable { selectedAnnouncement = notice },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notice.pinned) {
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (notice.pinned) Icons.Default.PushPin else Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (notice.pinned) "PINNED NOTICE" else notice.publishDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notice.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notice.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. AI GK Challenge Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenQuizTab)
                        .testTag("home_gk_banner"),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1A1552), RoyalPurple)
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_gk_banner),
                            contentDescription = "দরবার AI GK Challenge banner",
                            contentScale = ContentScale.Crop,
                            alpha = 0.28f,
                            modifier = Modifier.matchParentSize()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = WarmGold,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "AI POWERED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepNavy,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "দরবার AI GK CHALLENGE",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Daily GK, 12 categories, instant explanations & live leaderboard.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onOpenQuizTab,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmGold,
                                    contentColor = DeepNavy
                                )
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Six Subjects Grid
            item {
                SectionHeader(title = "Six Core Subjects", subtitle = "Explore notes, PDFs, diagrams & question banks")
                Spacer(modifier = Modifier.height(10.dp))
                val cols = if (isWideLayout) 3 else 2
                val rows = subjects.chunked(cols)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { subject ->
                                val accent = subjectColorFor(subject.id, subject.accentHex)
                                val count = allPublishedDocs.count { it.subjectId == subject.id }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenSubject(subject) }
                                        .testTag("subject_card_${subject.id}"),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = subjectIconFor(subject.iconKey),
                                                contentDescription = subject.titleEn,
                                                tint = accent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = subject.titleBn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                        Text(
                                            text = subject.titleEn,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$count material(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (rowItems.size < cols) {
                                repeat(cols - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // 6. Recently Uploaded Documents
            item {
                SectionHeader(title = "Recently Uploaded", subtitle = "Fresh study materials published by educators")
            }
            if (recentDocs.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "No Published Materials Yet",
                        subtitle = "Documents uploaded and published by administrators will appear here automatically."
                    )
                }
            } else {
                items(recentDocs, key = { "recent_${it.id}" }) { doc ->
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

            // 7. Popular Study Materials
            if (popularDocs.isNotEmpty()) {
                item {
                    SectionHeader(title = "Popular Study Materials", subtitle = "Most downloaded resources across দরবার")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(popularDocs, key = { "pop_${it.id}" }) { doc ->
                            Card(
                                modifier = Modifier
                                    .width(240.dp)
                                    .clickable { onPreviewDoc(doc) },
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = doc.subjectName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${doc.downloadCount} downloads • ${doc.fileType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8. Favourites & Recent Downloads Quick Strips
            if (favorites.isNotEmpty() || downloads.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SectionHeader(
                            title = "Your Library",
                            subtitle = "${favorites.size} favourites • ${downloads.size} downloads"
                        )
                        TextButton(onClick = onOpenDownloadsTab) {
                            Text("View All")
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            FilterBottomSheetContent(
                subjects = subjects,
                categories = categories,
                current = searchFilter,
                onApply = { sub, cat, type, days ->
                    onApplyFilters(sub, cat, null, type, days)
                    showFilterSheet = false
                },
                onReset = {
                    onResetFilters()
                    showFilterSheet = false
                }
            )
        }
    }

    selectedAnnouncement?.let { notice ->
        ModalBottomSheet(onDismissRequest = { selectedAnnouncement = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Posted by ${notice.authorName} • ${notice.publishDate}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!notice.attachmentDocId.isNullOrBlank()) {
                    val linkedDoc = allPublishedDocs.firstOrNull { it.id == notice.attachmentDocId }
                    if (linkedDoc != null) {
                        Button(
                            onClick = {
                                selectedAnnouncement = null
                                onPreviewDoc(linkedDoc)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Attached ${notice.attachmentType ?: "Document"}: ${notice.attachmentName ?: linkedDoc.title}")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilterBottomSheetContent(
    subjects: List<Subject>,
    categories: List<Category>,
    current: SearchFilterState,
    onApply: (String?, String?, String?, Int?) -> Unit,
    onReset: () -> Unit
) {
    var selSub by remember { mutableStateOf(current.subjectId) }
    var selCat by remember { mutableStateOf(current.categoryId) }
    var selType by remember { mutableStateOf(current.fileType) }
    var selDays by remember { mutableStateOf(current.dateWindowDays) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Filter Study Materials", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Subject", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = selSub == null, onClick = { selSub = null }, label = { Text("All") })
            }
            items(subjects) { s ->
                FilterChip(selected = selSub == s.id, onClick = { selSub = s.id }, label = { Text(s.titleEn) })
            }
        }

        Text("Category", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = selCat == null, onClick = { selCat = null }, label = { Text("All") })
            }
            items(categories) { c ->
                FilterChip(selected = selCat == c.id, onClick = { selCat = c.id }, label = { Text(c.name) })
            }
        }

        Text("File Type", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "All", "PDF" to "PDF", "PNG" to "PNG", "JPG" to "JPG", "DOCX" to "DOCX", "PPTX" to "PPTX", "TXT" to "TXT").forEach { (k, label) ->
                item {
                    FilterChip(selected = selType == k, onClick = { selType = k }, label = { Text(label) })
                }
            }
        }

        Text("Upload Date", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Anytime", 7 to "7 Days", 30 to "30 Days").forEach { (days, label) ->
                AssistChip(
                    onClick = { selDays = days },
                    label = { Text(label) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f).height(48.dp)) {
                Text("Reset")
            }
            Button(
                onClick = { onApply(selSub, selCat, selType, selDays) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Apply Filters")
            }
        }
    }
}
