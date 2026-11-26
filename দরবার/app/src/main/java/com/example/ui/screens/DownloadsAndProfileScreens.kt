package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.FileStorageHelper
import com.example.core.LocalSessionPrefs
import com.example.data.model.DownloadRecord
import com.example.data.model.FavoriteRecord
import com.example.data.model.StudentProfile
import com.example.data.model.StudyDocument
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import java.io.File

@Composable
fun DownloadsAndFavoritesScreen(
    downloads: List<DownloadRecord>,
    favorites: List<FavoriteRecord>,
    allDocuments: List<StudyDocument>,
    onOpenDocument: (StudyDocument) -> Unit,
    onToggleFavorite: (StudyDocument, Boolean) -> Unit,
    onMissingFileMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Downloads (${downloads.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favourites (${favorites.size})") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (downloads.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.DownloadDone,
                            title = "No downloads yet",
                            subtitle = "Files you download from any subject will be stored here for fast access."
                        )
                    }
                } else {
                    items(downloads, key = { it.id }) { dl ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val file = File(dl.localPath)
                                    if (file.exists()) {
                                        FileStorageHelper.openLocalFileExternally(context, file, dl.mimeType).onFailure {
                                            val matched = allDocuments.firstOrNull { d -> d.id == dl.documentId }
                                            if (matched != null) onOpenDocument(matched)
                                            else onMissingFileMessage("No external viewer installed for .${dl.fileExtension}.")
                                        }
                                    } else {
                                        val matched = allDocuments.firstOrNull { d -> d.id == dl.documentId }
                                        if (matched != null) onOpenDocument(matched)
                                        else onMissingFileMessage("File not found locally. Re-download from library.")
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = dl.fileExtension.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dl.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${dl.subjectName} • ${FileStorageHelper.formatFileSize(dl.fileSizeBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val matched = allDocuments.firstOrNull { d -> d.id == dl.documentId }
                                        if (matched != null) onOpenDocument(matched)
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open file")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (favorites.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Favorite,
                            title = "No favourite materials yet",
                            subtitle = "Tap the heart icon on any study document to bookmark it here."
                        )
                    }
                } else {
                    items(favorites, key = { it.id }) { fav ->
                        val doc = allDocuments.firstOrNull { it.id == fav.documentId }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = doc != null) { if (doc != null) onOpenDocument(doc) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fav.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${fav.subjectName} • ${fav.fileExtension.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (doc != null) {
                                    TextButton(onClick = { onToggleFavorite(doc, true) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    student: StudentProfile,
    localPrefs: LocalSessionPrefs,
    isAdminAuthorized: Boolean,
    onUpdateName: (String) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(NavyDeep, RoyalPurple)))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = WarmGold,
                            shape = CircleShape,
                            modifier = Modifier.size(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.fullName.firstOrNull()?.uppercase() ?: "S",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyDeep
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.fullName, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Student ID: ${student.studentCode}", style = MaterialTheme.typography.bodySmall, color = WarmGold)
                        }
                        IconButton(
                            onClick = { showRenameDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Change displayed name", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Student Activity Summary
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatChipCard("Downloads", student.downloadCount.toString(), Modifier.weight(1f))
                StatChipCard("Quizzes", student.quizAttemptsCount.toString(), Modifier.weight(1f))
                StatChipCard("Best Streak", student.bestStreak.toString(), Modifier.weight(1f))
            }
        }

        // Appearance & Preferences
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Appearance & Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "Auto", "LIGHT" to "Light", "DARK" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = localPrefs.themeMode == key,
                                onClick = { onSetThemeMode(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reduced Motion", style = MaterialTheme.typography.titleSmall)
                            Text("Minimize dashboard and counter animations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = localPrefs.reducedMotion, onCheckedChange = onSetReducedMotion)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Study Notifications", style = MaterialTheme.typography.titleSmall)
                            Text("Alerts for new PDFs, notices, and Daily GK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = localPrefs.notificationsEnabled, onCheckedChange = onSetNotificationsEnabled)
                    }
                }
            }
        }

        if (isAdminAuthorized) {
            item {
                Button(
                    onClick = onOpenAdminPanel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_admin_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Admin Control Center")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("student_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out")
            }
        }
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(student.fullName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Change Displayed Name") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.trim().length >= 2) {
                        onUpdateName(nameInput.trim())
                        showRenameDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatChipCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
