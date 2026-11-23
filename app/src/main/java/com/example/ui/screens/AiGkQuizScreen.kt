package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DarbarCurriculumDefaults
import com.example.data.model.LeaderboardEntry
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizDifficulty
import com.example.data.model.StudentProfile
import com.example.ui.components.AnimatedStatCounter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.QuizSessionState

@Composable
fun AiGkQuizScreen(
    student: StudentProfile?,
    sessionState: QuizSessionState,
    selectedCategory: String,
    selectedDifficulty: QuizDifficulty,
    selectedCount: Int,
    leaderboard: List<LeaderboardEntry>,
    quizHistory: List<QuizAttempt>,
    todayDate: String,
    reducedMotion: Boolean,
    allowQuiz: Boolean,
    onSelectCategory: (String) -> Unit,
    onSelectDifficulty: (QuizDifficulty) -> Unit,
    onSelectCount: (Int) -> Unit,
    onStartQuiz: (String) -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onReturnToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sessionState !is QuizSessionState.Lobby) {
        ActiveQuizRouter(
            sessionState = sessionState,
            reducedMotion = reducedMotion,
            onSelectAnswer = onSelectAnswer,
            onNextQuestion = onNextQuestion,
            onReturnToLobby = onReturnToLobby,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Challenge") }, icon = { Icon(Icons.Default.AutoAwesome, null) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Leaderboard") }, icon = { Icon(Icons.Default.Leaderboard, null) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") }, icon = { Icon(Icons.Default.History, null) })
        }

        when (selectedTab) {
            0 -> QuizLobbyTab(
                selectedCategory = selectedCategory,
                selectedDifficulty = selectedDifficulty,
                selectedCount = selectedCount,
                allowQuiz = allowQuiz,
                onSelectCategory = onSelectCategory,
                onSelectDifficulty = onSelectDifficulty,
                onSelectCount = onSelectCount,
                onStartQuiz = onStartQuiz
            )
            1 -> LeaderboardTab(
                leaderboard = leaderboard,
                currentUid = student?.uid ?: "",
                todayDate = todayDate
            )
            2 -> QuizHistoryTab(
                quizHistory = quizHistory,
                student = student
            )
        }
    }
}

@Composable
private fun QuizLobbyTab(
    selectedCategory: String,
    selectedDifficulty: QuizDifficulty,
    selectedCount: Int,
    allowQuiz: Boolean,
    onSelectCategory: (String) -> Unit,
    onSelectDifficulty: (QuizDifficulty) -> Unit,
    onSelectCount: (Int) -> Unit,
    onStartQuiz: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepNavy, RoyalPurple)))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gk_banner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "দরবার AI GK CHALLENGE",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Powered by Firebase AI Logic (Gemini). Test yourself across 12 curated categories with instant explanations & sources.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Quick Modes: Daily GK & Random GK
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onStartQuiz("Daily GK Challenge") },
                    enabled = allowQuiz,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("daily_gk_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Daily GK")
                }
                OutlinedButton(
                    onClick = { onStartQuiz("Random GK Challenge") },
                    enabled = allowQuiz,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("random_gk_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Random GK")
                }
            }
        }

        // Difficulty
        item {
            Text("Difficulty Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizDifficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = selectedDifficulty == diff,
                        onClick = { onSelectDifficulty(diff) },
                        label = { Text("${diff.label} (${diff.labelBn})") }
                    )
                }
            }
        }

        // Question Count
        item {
            Text("Number of Questions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20).forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick = { onSelectCount(count) },
                        label = { Text("$count Qs") }
                    )
                }
            }
        }

        // 12 Categories
        item {
            Text("Select Category (12 GK Domains)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DarbarCurriculumDefaults.GK_CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onStartQuiz("Category Quiz") },
                enabled = allowQuiz,
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("start_category_quiz_button"),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start $selectedCategory Quiz ($selectedCount Qs)")
            }
        }
    }
}

@Composable
private fun ActiveQuizRouter(
    sessionState: QuizSessionState,
    reducedMotion: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onReturnToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (sessionState) {
            is QuizSessionState.Generating -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp))
                    Text(
                        text = sessionState.message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Verifying non-repeating questions & sources...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is QuizSessionState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Quiz Unavailable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(sessionState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = onReturnToLobby) { Text("Back to Quiz Lobby") }
                    }
                }
            }
            is QuizSessionState.Playing -> {
                val q = sessionState.questions[sessionState.currentIndex]
                Column(modifier = Modifier.fillMaxSize()) {
                    // Progress & Timer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Question ${sessionState.currentIndex + 1} / ${sessionState.questions.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = if (sessionState.remainingSeconds <= 7) CrimsonError.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${sessionState.remainingSeconds}s",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (sessionState.currentIndex + 1).toFloat() / sessionState.questions.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = sessionState.currentIndex,
                        transitionSpec = {
                            fadeIn(tween(if (reducedMotion) 0 else 220)) togetherWith fadeOut(tween(if (reducedMotion) 0 else 180))
                        },
                        modifier = Modifier.weight(1f),
                        label = "question_transition"
                    ) { idx ->
                        val item = sessionState.questions[idx]
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = sessionState.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (item.aiGenerated && !item.verifiedByAdmin) {
                                        Surface(color = WarmGold.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                text = "AI Practice • Verify Source",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Surface(color = EmeraldSuccess.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                text = "Admin Verified",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Card(shape = MaterialTheme.shapes.large) {
                                    Text(
                                        text = item.questionText,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }

                            itemsIndexed(item.options) { optIdx, optText ->
                                val answered = sessionState.selectedOption != null
                                val isSelected = sessionState.selectedOption == optIdx
                                val isCorrect = optIdx == item.correctIndex
                                val bg = when {
                                    !answered -> MaterialTheme.colorScheme.surface
                                    isCorrect -> EmeraldSuccess.copy(alpha = 0.16f)
                                    isSelected -> CrimsonError.copy(alpha = 0.16f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val borderColor = when {
                                    !answered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    isCorrect -> EmeraldSuccess
                                    isSelected -> CrimsonError
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !answered) { onSelectAnswer(optIdx) }
                                        .testTag("quiz_option_$optIdx"),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    border = BorderStroke(1.5.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${'A' + optIdx}.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (answered && isCorrect) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = EmeraldSuccess)
                                        } else if (answered && isSelected) {
                                            Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = CrimsonError)
                                        }
                                    }
                                }
                            }

                            if (sessionState.selectedOption != null) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Explanation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            Text(item.explanation, style = MaterialTheme.typography.bodyMedium)
                                            if (item.source.isNotBlank() || item.factDate.isNotBlank()) {
                                                Text(
                                                    text = "Source: ${item.source} (${item.factDate})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (sessionState.selectedOption != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNextQuestion,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("quiz_next_button")
                        ) {
                            Text(
                                if (sessionState.currentIndex + 1 < sessionState.questions.size) "Next Question"
                                else "Finish & View Results"
                            )
                        }
                    }
                }
            }
            is QuizSessionState.Completed -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(WarmGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(40.dp))
                        }
                        Text("Quiz Completed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        AnimatedStatCounter(
                            target = sessionState.scorePercent,
                            reducedMotion = reducedMotion,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${sessionState.correctCount} / ${sessionState.totalQuestions} Correct (${sessionState.scorePercent}%) • ${sessionState.durationSeconds}s",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (sessionState.newlyUnlockedAchievements.isNotEmpty()) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Achievements Unlocked!", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    sessionState.newlyUnlockedAchievements.forEach { ach ->
                                        Text("★ $ach", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = onReturnToLobby,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Back to AI GK Hub")
                        }
                    }
                }
            }
            is QuizSessionState.Lobby -> Unit
        }
    }
}

@Composable
private fun LeaderboardTab(
    leaderboard: List<LeaderboardEntry>,
    currentUid: String,
    todayDate: String
) {
    var dailyOnly by remember { mutableIntStateOf(0) }
    val sorted = remember(leaderboard, dailyOnly, todayDate) {
        if (dailyOnly == 1) {
            leaderboard.filter { it.dailyDate == todayDate }.sortedByDescending { it.dailyScore }
        } else {
            leaderboard.sortedByDescending { it.totalScore }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = dailyOnly == 0, onClick = { dailyOnly = 0 }, label = { Text("All-Time Ranking") })
                FilterChip(selected = dailyOnly == 1, onClick = { dailyOnly = 1 }, label = { Text("Daily Ranking ($todayDate)") })
            }
        }
        if (sorted.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Leaderboard,
                    title = "No Leaderboard Entries Yet",
                    subtitle = "Complete an AI GK Challenge to claim the #1 rank!"
                )
            }
        } else {
            itemsIndexed(sorted, key = { _, e -> e.uid }) { idx, entry ->
                val isMe = entry.uid == currentUid
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (idx < 3) WarmGold else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(38.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.fullName + if (isMe) " (You)" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entry.studentCode} • ${entry.quizzesTaken} quizzes • Best ${entry.bestPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${if (dailyOnly == 1) entry.dailyScore else entry.totalScore} pts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizHistoryTab(quizHistory: List<QuizAttempt>, student: StudentProfile?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Quizzes Taken", style = MaterialTheme.typography.labelSmall)
                        Text("${student?.quizzesTaken ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Total Correct", style = MaterialTheme.typography.labelSmall)
                        Text("${student?.totalQuizScore ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Accuracy", style = MaterialTheme.typography.labelSmall)
                        Text("${student?.averageAccuracyPercent ?: 0}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (quizHistory.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.History,
                    title = "No Quiz History",
                    subtitle = "Your completed quiz scores and timing will be tracked here."
                )
            }
        } else {
            items(quizHistory, key = { it.id }) { attempt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attempt.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${attempt.mode} • ${attempt.difficulty} • ${attempt.correctCount}/${attempt.totalQuestions} • ${formatTimestamp(attempt.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${attempt.scorePercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (attempt.scorePercent >= 70) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
