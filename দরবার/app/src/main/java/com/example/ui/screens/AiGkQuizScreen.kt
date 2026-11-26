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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DefaultCatalog
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDifficulty
import com.example.data.model.StudentProfile
import com.example.ui.components.AnimatedCounterText
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ScoreRingCanvas
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NotoBengaliFontFamily
import com.example.ui.theme.RoseError
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.QuizSessionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiGkQuizScreen(
    quizEnabled: Boolean,
    dailyEnabled: Boolean,
    student: StudentProfile,
    quizState: QuizSessionState,
    attempts: List<QuizAttempt>,
    leaderboard: List<LeaderboardEntry>,
    reducedMotion: Boolean,
    onStartQuiz: (category: String, difficulty: QuizDifficulty, count: Int, mode: String, bilingual: Boolean) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onExitQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!quizEnabled) {
        Box(modifier = modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                icon = Icons.Default.AutoAwesome,
                title = "Quiz Temporarily Paused",
                subtitle = "Administrators have temporarily disabled the AI GK Challenge."
            )
        }
        return
    }

    if (quizState.active) {
        QuizArenaView(
            quizState = quizState,
            reducedMotion = reducedMotion,
            onSelectAnswer = onSelectAnswer,
            onNextQuestion = onNextQuestion,
            onExitQuiz = onExitQuiz,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf(DefaultCatalog.gkCategories.first()) }
    var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.MEDIUM) }
    var selectedCount by remember { mutableIntStateOf(5) }
    var bilingualHint by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Challenge") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Rankings") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Badges") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(148.dp)
                                .clip(RoundedCornerShape(22.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_gk_challenge_banner),
                                contentDescription = "Dorbar AI GK Challenge",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NavyDeep.copy(alpha = 0.9f), RoyalPurple.copy(alpha = 0.5f))
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                    Text(
                                        text = "দরবার AI GK CHALLENGE",
                                        fontFamily = NotoBengaliFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 21.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Powered by Gemini AI • Factual explanations & source citations",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmGold
                                    )
                                }
                            }
                        }
                    }

                    // Quick Challenge Modes
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (dailyEnabled) {
                                QuickModeCard(
                                    icon = Icons.Default.Today,
                                    title = "Daily GK",
                                    subtitle = "10 Qs • Mixed",
                                    accent = RoyalPurple,
                                    onClick = {
                                        onStartQuiz("Current Affairs", selectedDifficulty, 10, "Daily GK Challenge", bilingualHint)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            QuickModeCard(
                                icon = Icons.Default.Shuffle,
                                title = "Random Mix",
                                subtitle = "$selectedCount Qs • All",
                                accent = IndigoPrimary,
                                onClick = {
                                    onStartQuiz(
                                        DefaultCatalog.gkCategories.random(),
                                        selectedDifficulty,
                                        selectedCount,
                                        "Random GK Challenge",
                                        bilingualHint
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Category Selection (12 Categories)
                    item {
                        Text("Choose GK Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DefaultCatalog.gkCategories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }

                    // Difficulty & Question Count
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Difficulty Level", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuizDifficulty.entries.forEach { diff ->
                                        FilterChip(
                                            selected = selectedDifficulty == diff,
                                            onClick = { selectedDifficulty = diff },
                                            label = { Text("${diff.label} (${diff.secondsPerQuestion}s)") }
                                        )
                                    }
                                }
                                Text("Question Count", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5, 10, 15, 20).forEach { cnt ->
                                        FilterChip(
                                            selected = selectedCount == cnt,
                                            onClick = { selectedCount = cnt },
                                            label = { Text("$cnt Qs") }
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Include Bengali Hints", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Helpful Bengali key terms alongside English questions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(checked = bilingualHint, onCheckedChange = { bilingualHint = it })
                                }

                                Button(
                                    onClick = {
                                        onStartQuiz(selectedCategory, selectedDifficulty, selectedCount, "Category Quiz", bilingualHint)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("start_quiz_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start $selectedCategory Quiz")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
                val dailyTop = remember(leaderboard, todayKey) {
                    leaderboard.filter { it.dailyDateKey == todayKey }.sortedByDescending { it.dailyPoints }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text("Daily Ranking ($todayKey)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (dailyTop.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Leaderboard,
                                title = "No daily scores yet",
                                subtitle = "Complete a Daily or Category GK quiz to claim #1 today!"
                            )
                        }
                    } else {
                        itemsIndexed(dailyTop.take(10)) { idx, entry ->
                            LeaderboardRow(rank = idx + 1, entry = entry, points = entry.dailyPoints, isDaily = true)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("All-Time Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(leaderboard.take(20)) { idx, entry ->
                        LeaderboardRow(rank = idx + 1, entry = entry, points = entry.totalPoints, isDaily = false)
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (attempts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.History,
                                title = "No quiz attempts yet",
                                subtitle = "Your completed quiz scores and accuracy trends will be saved here."
                            )
                        }
                    } else {
                        items(attempts, key = { it.id }) { att ->
                            Card(shape = RoundedCornerShape(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.category, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${att.mode} • ${att.difficulty} • ${att.correctAnswers}/${att.totalQuestions} correct • ${att.timeSpentSeconds}s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (att.scorePercent >= 70) EmeraldSuccess.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${att.scorePercent}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (att.scorePercent >= 70) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text("Performance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Quizzes: ${student.quizAttemptsCount} • Best Streak: ${student.bestStreak}", style = MaterialTheme.typography.bodySmall)
                                }
                                val avg = if (student.totalQuizQuestions > 0) ((student.totalQuizScore * 100) / student.totalQuizQuestions).toInt() else 0
                                ScoreRingCanvas(percent = avg, reducedMotion = reducedMotion)
                            }
                        }
                    }
                    items(student.achievements) { badge ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(badge, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.12f),
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, points: Long, isDaily: Boolean) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (rank <= 3) WarmGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("#$rank", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.studentCode} • ${entry.quizzesCompleted} quizzes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "$points pts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDaily) RoyalPurple else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuizArenaView(
    quizState: QuizSessionState,
    reducedMotion: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onExitQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        quizState.loading -> {
            Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Generating verified GK questions with Gemini AI…", style = MaterialTheme.typography.titleMedium)
                    Text("Checking factual references and explanations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        !quizState.error.isNullOrBlank() -> {
            Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(quizState.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onExitQuiz) { Text("Back to Quiz Hub") }
                }
            }
        }
        quizState.completed -> {
            val total = quizState.questions.size.coerceAtLeast(1)
            val pct = (quizState.correctCount * 100) / total
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = modifier.fillMaxSize()
            ) {
                item {
                    ScoreRingCanvas(percent = pct, reducedMotion = reducedMotion)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (pct >= 80) "Outstanding Performance!" else if (pct >= 50) "Good Effort!" else "Keep Practicing!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedCounterText(
                        targetValue = quizState.correctCount,
                        reducedMotion = reducedMotion,
                        suffix = " / $total Correct"
                    )
                    Text(
                        text = "Time: ${quizState.totalElapsedSeconds}s • Best Streak: ${quizState.maxStreak}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (quizState.newlyUnlockedAchievements.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = WarmGold.copy(alpha = 0.16f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Achievements Unlocked!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                quizState.newlyUnlockedAchievements.forEach { ach ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(ach, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = onExitQuiz,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Return to Quiz Hub")
                    }
                }
            }
        }
        else -> {
            val question = quizState.questions.getOrNull(quizState.currentIndex) ?: return
            val progress = (quizState.currentIndex + 1).toFloat() / quizState.questions.size.coerceAtLeast(1)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${quizState.currentIndex + 1} / ${quizState.questions.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (quizState.secondsRemaining <= 5) RoseError.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = "Timer", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${quizState.secondsRemaining}s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))
                    )
                }

                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${question.category} • ${question.difficulty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = question.questionText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                itemsIndexed(question.options) { idx, option ->
                    val isSelected = quizState.selectedOptionIndex == idx
                    val isCorrect = idx.toLong() == question.correctIndex
                    val bgColor = when {
                        !quizState.answerRevealed -> MaterialTheme.colorScheme.surface
                        isCorrect -> EmeraldSuccess.copy(alpha = 0.18f)
                        isSelected && !isCorrect -> RoseError.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val borderColor = when {
                        !quizState.answerRevealed -> MaterialTheme.colorScheme.outlineVariant
                        isCorrect -> EmeraldSuccess
                        isSelected && !isCorrect -> RoseError
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(enabled = !quizState.answerRevealed) { onSelectAnswer(idx) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "${('A' + idx)}.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            if (quizState.answerRevealed && isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = EmeraldSuccess)
                            }
                        }
                    }
                }

                if (quizState.answerRevealed) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                                if (question.sourceNote.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Source: ${question.sourceNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = onNextQuestion,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                if (quizState.currentIndex + 1 >= quizState.questions.size) "Finish & View Results"
                                else "Next Question"
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onExitQuiz,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exit Quiz")
                    }
                }
            }
        }
    }
}
