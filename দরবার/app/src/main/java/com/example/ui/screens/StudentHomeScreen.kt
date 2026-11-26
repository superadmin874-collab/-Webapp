package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.DownloadRecord
import com.example.data.model.FavoriteRecord
import com.example.data.model.StudentNotification
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.data.model.SubjectItem
import com.example.ui.components.DocumentRowCard
import com.example.ui.components.DorbarLogoBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SubjectVisualCard
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotoBengaliFontFamily
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    appName: String,
    tagline: String,
    student: StudentProfile,
    subjects: List<SubjectItem>,
    documents: List<StudyDocument>,
    announcements: List<Announcement>,
    favorites: List<FavoriteRecord>,
    downloads: List<DownloadRecord>,
    notifications: List<StudentNotification>,
    isAdminAuthorized: Boolean,
    themeMode: String,
    onToggleTheme: () -> Unit,
    onOpenSearch: () -> Unit,
    onSelectSubject: (SubjectItem) -> Unit,
    onOpenDocument: (StudyDocument) -> Unit,
    onDownloadDocument: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    onOpenAiQuiz: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNotificationsSheet by remember { mutableStateOf(false) }
    val unreadCount = notifications.count { !it.read }
    val favIds = remember(favorites) { favorites.map { it.documentId }.toSet() }
    val recentDocs = remember(documents) { documents.take(5) }
    val popularDocs = remember(documents) { documents.sortedByDescending { it.downloadCount }.take(5) }
    val favDocs = remember(documents, favIds) { documents.filter { it.id in favIds }.take(5) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DorbarLogoBadge(appName = appName, subtitle = tagline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdminAuthorized) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("switch_to_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Open Admin Control Center",
                                tint = RoyalPurple
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (themeMode == "DARK") Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                }
            }
        }

        // Personalized Welcome Banner + Student Profile Pill
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(NavyDeep, RoyalPurple)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "নমস্কার / Hello, ${student.fullName}",
                                fontFamily = NotoBengaliFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Student ID: ${student.studentCode} • ${student.quizAttemptsCount} quizzes • ${student.downloadCount} downloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = WarmGold,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(onClick = onOpenProfile)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.fullName.firstOrNull()?.uppercase() ?: "S",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDeep
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Search Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenSearch)
                    .testTag("global_search_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search subjects, chapters, PDFs, suggestions…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Announcements
        if (announcements.isNotEmpty()) {
            item {
                SectionTitleRow(title = "Important Announcements", badge = "${announcements.size}")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(announcements, key = { it.id }) { ann ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ann.pinned) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.width(290.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (ann.pinned) Icons.Default.PushPin else Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ann.publishDateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ann.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ann.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Six Subjects Grid (2 columns per row inside LazyColumn)
        item {
            SectionTitleRow(title = "Six Subject Libraries", badge = "${subjects.size}")
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                subjects.chunked(2).forEach { pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        pair.forEach { subj ->
                            val count = documents.count { it.subjectId == subj.id }
                            SubjectVisualCard(
                                subject = subj,
                                documentCount = count,
                                onClick = { onSelectSubject(subj) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // AI GK Challenge Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAiQuiz)
                    .testTag("ai_gk_challenge_banner")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gk_challenge_banner),
                        contentDescription = "AI GK Challenge",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NavyDeep.copy(alpha = 0.92f), IndigoPrimary.copy(alpha = 0.45f))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = WarmGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "দরবার AI GK CHALLENGE",
                                    fontFamily = NotoBengaliFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "12 GK Categories • Timed Modes • Daily Rankings • AI Explanations",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Button(
                                onClick = onOpenAiQuiz,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmGold,
                                    contentColor = NavyDeep
                                )
                            ) {
                                Text("Start GK Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Recently Uploaded Documents
        item {
            SectionTitleRow(title = "Recently Uploaded", badge = "${recentDocs.size}")
        }
        if (recentDocs.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "No study materials published yet",
                    subtitle = "Administrators can upload PDFs, diagrams, and notes from the Admin Control Center."
                )
            }
        } else {
            items(recentDocs, key = { "recent_${it.id}" }) { doc ->
                DocumentRowCard(
                    doc = doc,
                    isFavorite = doc.id in favIds,
                    onOpenViewer = { onOpenDocument(doc) },
                    onDownload = { onDownloadDocument(doc) },
                    onToggleFavorite = { onToggleFavorite(doc, doc.id in favIds) }
                )
            }
        }

        // Popular Study Materials
        if (popularDocs.isNotEmpty()) {
            item {
                SectionTitleRow(title = "Popular Study Materials", badge = "Top")
            }
            items(popularDocs, key = { "pop_${it.id}" }) { doc ->
                DocumentRowCard(
                    doc = doc,
                    isFavorite = doc.id in favIds,
                    onOpenViewer = { onOpenDocument(doc) },
                    onDownload = { onDownloadDocument(doc) },
                    onToggleFavorite = { onToggleFavorite(doc, doc.id in favIds) }
                )
            }
        }

        // Favourite Materials & Recent Downloads quick summary
        if (favDocs.isNotEmpty()) {
            item {
                SectionTitleRow(title = "Favourite Materials", badge = "${favorites.size}")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(favDocs, key = { "fav_${it.id}" }) { doc ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onOpenDocument(doc) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(doc.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(doc.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (downloads.isNotEmpty()) {
            item {
                SectionTitleRow(title = "Recent Downloads", badge = "${downloads.size}")
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(downloads.take(5), key = { "dl_${it.id}" }) { dl ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.width(220.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.DownloadDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(dl.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${dl.subjectName} • ${dl.fileExtension.uppercase()}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Notifications", style = MaterialTheme.typography.headlineSmall)
                if (notifications.isEmpty()) {
                    Text(
                        "You're all caught up! New PDFs, announcements, and GK challenges will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                } else {
                    notifications.take(15).forEach { notif ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (notif.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkNotificationRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitleRow(title: String, badge: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
