package com.example.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.DocumentStorageHelper
import com.example.data.model.StudyDocument
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SubjectBengaliColor
import com.example.ui.theme.SubjectBiologyColor
import com.example.ui.theme.SubjectChemistryColor
import com.example.ui.theme.SubjectEnglishColor
import com.example.ui.theme.SubjectMathColor
import com.example.ui.theme.SubjectPhysicsColor
import com.example.ui.theme.WarmGold
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DarbarLogoBadge(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.darbar_icon_art),
        contentDescription = "দরবার logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
    )
}

@Composable
fun AnimatedStatCounter(
    target: Int,
    reducedMotion: Boolean,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 550),
        label = "stat_counter"
    )
    Text(
        text = value.toString(),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

fun subjectIconFor(iconKey: String): ImageVector = when (iconKey) {
    "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
    "translate" -> Icons.Default.Translate
    "bolt" -> Icons.Default.Bolt
    "science" -> Icons.Default.Science
    "calculate" -> Icons.Default.Calculate
    "biotech" -> Icons.Default.Biotech
    else -> Icons.Default.AutoStories
}

fun subjectColorFor(subjectId: String, hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        when (subjectId) {
            "bengali" -> SubjectBengaliColor
            "english" -> SubjectEnglishColor
            "physics" -> SubjectPhysicsColor
            "chemistry" -> SubjectChemistryColor
            "mathematics" -> SubjectMathColor
            "biology" -> SubjectBiologyColor
            else -> RoyalPurple
        }
    }
}

@Composable
fun FileTypeBadge(fileType: String, modifier: Modifier = Modifier) {
    val upper = fileType.uppercase(Locale.US)
    val (bg, fg, icon) = when (upper) {
        "PDF" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.PictureAsPdf)
        "JPG", "JPEG", "PNG", "WEBP" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.Image)
        "DOC", "DOCX" -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Icons.Default.Description)
        "PPT", "PPTX" -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Default.Description)
        "XLS", "XLSX" -> Triple(Color(0xFFD1FAE5), Color(0xFF047857), Icons.Default.Description)
        else -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), Icons.Default.Description)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = upper, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DocumentListItemCard(
    doc: StudyDocument,
    isFavorite: Boolean,
    isDownloading: Boolean,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
            .testTag("doc_card_${doc.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    FileTypeBadge(doc.fileType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doc.subjectName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (doc.featured) {
                    Surface(
                        color = WarmGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Featured",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9A6000),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (doc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${doc.categoryName} • Ch: ${doc.chapter} • ${DocumentStorageHelper.formatBytes(doc.fileSizeBytes)} • ${doc.downloadCount} downloads",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTimestamp(doc.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favourites" else "Save to favourites",
                            tint = if (isFavorite) CrimsonError else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview document",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onDownload,
                        enabled = !isDownloading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download document",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubjectBarChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Default.AutoStories,
            title = "No Subject Data Yet",
            subtitle = "Upload documents across subjects to view live distribution."
        )
        return
    }
    val entries = data.entries.toList().take(6)
    val maxVal = (entries.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    val barColors = listOf(
        SubjectBengaliColor, SubjectEnglishColor, SubjectPhysicsColor,
        SubjectChemistryColor, SubjectMathColor, SubjectBiologyColor
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val count = entries.size
            val slotW = size.width / count
            val barW = slotW * 0.52f
            entries.forEachIndexed { idx, entry ->
                val ratio = entry.value.toFloat() / maxVal.toFloat()
                val barH = (size.height - 12f) * ratio.coerceAtLeast(0.06f)
                val left = idx * slotW + (slotW - barW) / 2f
                val top = size.height - barH
                drawRoundRect(
                    color = barColors[idx % barColors.size],
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entries.forEachIndexed { idx, entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${entry.value}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = barColors[idx % barColors.size]
                    )
                    Text(
                        text = entry.key.substringBefore(" (").take(7),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FileTypeDonutChart(
    pdfs: Int,
    images: Int,
    others: Int,
    modifier: Modifier = Modifier
) {
    val total = (pdfs + images + others).coerceAtLeast(1)
    val slices = listOf(
        Triple("PDFs", pdfs, CrimsonError),
        Triple("Images", images, IndigoPrimary),
        Triple("Docs/Other", others, EmeraldSuccess)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            var startAngle = -90f
            val stroke = 24f
            if (pdfs + images + others == 0) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            } else {
                slices.forEach { (_, value, color) ->
                    val sweep = (value.toFloat() / total.toFloat()) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                        )
                        startAngle += sweep
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEach { (label, count, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$label: $count",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun darbarHeroBrush(): Brush = Brush.linearGradient(
    colors = listOf(DeepNavy, Color(0xFF281D6B), RoyalPurple)
)

fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "Just now"
    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(ts.toDate())
}
