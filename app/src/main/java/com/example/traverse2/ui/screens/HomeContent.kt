package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import com.example.traverse2.ui.viewmodel.HomeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import com.example.traverse2.ui.components.StreakDay

data class DifficultyData(val easy: Int, val medium: Int, val hard: Int)
data class PlatformData(val name: String, val count: Int, val color: Color)
data class SubmissionStats(val accepted: Int, val failed: Int, val total: Int)
data class CategoryData(val name: String, val count: Int)
data class RecentSolve(val problemName: String, val platform: String, val difficulty: String, val timeAgo: String)
data class AchievementData(val name: String, val description: String, val icon: String?, val category: String, val unlocked: Boolean)
data class ProblemItem(val name: String, val platform: String, val difficulty: String, val solved: Boolean)
data class StreakData(val currentStreak: Int, val longestStreak: Int, val totalActiveDays: Int, val averagePerWeek: Float, val streakDays: List<StreakDay> = emptyList())

@Composable
fun HomeContent(
    hazeState: HazeState,
    onLogout: () -> Unit,
    onNavigateToProblems: () -> Unit = {},
    onNavigateToStreak: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onStreakDataReady: (StreakData) -> Unit = {},
    onAchievementsDataReady: (List<AchievementData>) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    // Show loading indicator
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = glassColors.textPrimary)
        }
        return
    }
    
    // Show error state
    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Error: ${uiState.error}", color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap to retry",
                    color = glassColors.textSecondary,
                    modifier = Modifier.clickable { viewModel.refresh() }
                )
            }
        }
        return
    }
    
    // Extract data from state
    val user = uiState.user ?: return
    val solveStats = uiState.solveStats
    val submissionStats = uiState.submissionStats
    val recentSolves = uiState.recentSolves
    val achievements = uiState.achievements
    
    // Convert backend data to UI models
    val difficultyData = if (solveStats != null) {
        DifficultyData(
            easy = solveStats.byDifficulty["easy"] ?: 0,
            medium = solveStats.byDifficulty["medium"] ?: 0,
            hard = solveStats.byDifficulty["hard"] ?: 0
        )
    } else {
        DifficultyData(0, 0, 0)
    }
    
    val platforms = if (solveStats != null) {
        solveStats.byPlatform.map { (name, count) ->
            val color = when (name.lowercase()) {
                "leetcode" -> Color(0xFFFFA116)
                "hackerrank" -> Color(0xFF00EA64)
                "codeforces" -> Color(0xFF1890FF)
                else -> Color.Gray
            }
            PlatformData(name, count, color)
        }
    } else {
        emptyList()
    }
    
    val submissionStatsData = if (submissionStats != null) {
        SubmissionStats(
            accepted = submissionStats.accepted,
            failed = submissionStats.failed,
            total = submissionStats.total
        )
    } else {
        null
    }
    
    val recentSolvesData = recentSolves.map { solve ->
        val timeAgo = calculateTimeAgo(solve.solvedAt)
        RecentSolve(
            problemName = solve.problem.title ?: solve.problem.slug,
            platform = solve.problem.platform,
            difficulty = solve.problem.difficulty ?: "Unknown",
            timeAgo = timeAgo
        )
    }
    
    val achievementsData = achievements.map { achievement ->
        AchievementData(
            name = achievement.name,
            description = achievement.description,
            icon = achievement.icon ?: "trophy",
            category = achievement.category,
            unlocked = achievement.unlocked
        )
    }
    
    // Mock data for cards without backend support
    val categories = listOf(
        CategoryData("Arrays", 0),
        CategoryData("Strings", 0),
        CategoryData("DP", 0),
        CategoryData("Trees", 0),
        CategoryData("Graphs", 0),
        CategoryData("Math", 0)
    )
    val avgSolveTime = "-"
    val bestTime = "-"
    val totalTime = "-"
    val allProblems = emptyList<ProblemItem>()
    
    val problemsCompleted = solveStats?.totalSolves ?: 0
    val totalProblems = 100 // Mock value
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp, bottom = 120.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -20 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stay curious, keep coding!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = glassColors.textSecondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        AnimatedCard(delay = 50, isVisible = isVisible) {
            StreakCard(
                streak = user.currentStreak,
                hazeState = hazeState,
                onClick = {
                    // Generate streak days from calendarSolveDates
                    val today = LocalDate.now()
                    val calendarSolveDates = uiState.calendarSolveDates
                    val streakDays = (0 until 35).map { daysAgo ->
                        val date = today.minusDays(daysAgo.toLong())
                        StreakDay(
                            date = date,
                            isActive = calendarSolveDates.contains(date),
                            isToday = daysAgo == 0
                        )
                    }
                    onStreakDataReady(
                        StreakData(
                            currentStreak = user.currentStreak,
                            longestStreak = user.longestStreak,
                            totalActiveDays = solveStats?.totalStreakDays ?: 0,
                            averagePerWeek = if (solveStats != null) solveStats.totalSolves / 4f else 0f,
                            streakDays = streakDays
                        )
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedCard(delay = 100, isVisible = isVisible) {
            YourWorkCard(
                totalSolves = solveStats?.totalSolves ?: 0,
                totalXp = user.totalXp,
                streak = user.currentStreak,
                hazeState = hazeState,
                glassColors = glassColors
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Problems Summary Card - click to see full details
        AnimatedCard(delay = 130, isVisible = isVisible) {
            ProblemsSummaryCard(
                completed = problemsCompleted,
                total = totalProblems,
                hazeState = hazeState,
                glassColors = glassColors,
                onClick = onNavigateToProblems
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (difficultyData.easy + difficultyData.medium + difficultyData.hard > 0) {
            AnimatedCard(delay = 160, isVisible = isVisible) {
                DifficultyCard(difficultyData, hazeState, glassColors)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        if (platforms.isNotEmpty()) {
            AnimatedCard(delay = 190, isVisible = isVisible) {
                PlatformsCard(platforms, hazeState, glassColors)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        if (submissionStatsData != null && submissionStatsData.total > 0) {
            AnimatedCard(delay = 220, isVisible = isVisible) {
                SubmissionStatsCard(submissionStatsData, hazeState, glassColors)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // ProblemDistributionCard - no backend support yet, using mock data
        AnimatedCard(delay = 250, isVisible = isVisible) {
            ProblemDistributionCard(categories, hazeState, glassColors)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // TimePerformanceCard - no backend support yet, using mock data
        AnimatedCard(delay = 280, isVisible = isVisible) {
            TimePerformanceCard(avgSolveTime, bestTime, totalTime, hazeState, glassColors)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (recentSolvesData.isNotEmpty()) {
            AnimatedCard(delay = 310, isVisible = isVisible) {
                RecentSolvesCard(recentSolvesData, hazeState, glassColors)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // Achievements Section - Always show the card
        AnimatedCard(delay = 340, isVisible = isVisible) {
            if (achievementsData.isNotEmpty()) {
                AchievementsSection(achievementsData, hazeState, glassColors) {
                    onAchievementsDataReady(achievementsData)
                }
            } else {
                EmptyAchievementsSection(hazeState = hazeState, glassColors = glassColors) {
                    onAchievementsDataReady(emptyList())
                }
            }
        }
    }
}

// Helper function to calculate time ago
private fun calculateTimeAgo(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)
        
        when {
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            else -> "${days / 30}mo ago"
        }
    } catch (e: Exception) {
        "Recently"
    }
}

@Composable
private fun AnimatedCard(delay: Int, isVisible: Boolean, content: @Composable () -> Unit) {
    var showCard by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(delay.toLong())
            showCard = true
        }
    }
    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
        ) { 50 }
    ) {
        content()
    }
}

@Composable
private fun GlassCardContainer(
    hazeState: HazeState,
    glassColors: GlassColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
private fun GlassSubsection(
    glassColors: GlassColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val subsectionBg = if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x30FFFFFF)
    val borderColor = if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x20000000)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(subsectionBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector, glassColors: GlassColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = glassColors.textPrimary
        )
    }
}

@Composable
private fun StreakCard(streak: Int, hazeState: HazeState, onClick: () -> Unit = {}) {
    val streakComment = when {
        streak == 0 -> "Start your streak today!"
        streak < 3 -> "Good start! Keep it going!"
        streak < 7 -> "You're building momentum!"
        streak < 14 -> "One week strong! Amazing!"
        streak < 30 -> "Unstoppable! Keep pushing!"
        else -> "Legendary coder! $streak days!"
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = Color(0xFFE91E8C),
                    blurRadius = 40.dp,
                    tint = HazeTint(Color(0x40E91E8C)),
                    noiseFactor = 0.02f
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x80E91E8C), Color(0x80A855F7))
                )
            )
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$streak", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "day streak", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = streakComment, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                Text(text = "Tap for details", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun YourWorkCard(
    totalSolves: Int,
    totalXp: Int,
    streak: Int,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Your Work", icon = Icons.Default.Code, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatColumn(Icons.Default.CheckCircle, totalSolves.toString(), "Total Solves", glassColors)
                StatColumn(Icons.Default.Star, totalXp.toString(), "Total XP", glassColors)
                StatColumn(Icons.Default.LocalFireDepartment, streak.toString(), "Streak", glassColors)
            }
        }
    }
}

@Composable
private fun StatColumn(icon: ImageVector, value: String, label: String, glassColors: GlassColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = glassColors.textSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = glassColors.textPrimary)
        Text(label, fontSize = 12.sp, color = glassColors.textSecondary)
    }
}

@Composable
private fun DifficultyCard(data: DifficultyData, hazeState: HazeState, glassColors: GlassColors) {
    val total = data.easy + data.medium + data.hard
    val easyProgress = remember { Animatable(0f) }
    val mediumProgress = remember { Animatable(0f) }
    val hardProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        easyProgress.animateTo(data.easy.toFloat() / total, tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(100)
        mediumProgress.animateTo(data.medium.toFloat() / total, tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(200)
        hardProgress.animateTo(data.hard.toFloat() / total, tween(800, easing = FastOutSlowInEasing))
    }
    
    val easyColor = Color(0xFF22C55E)
    val mediumColor = Color(0xFFFBBF24)
    val hardColor = Color(0xFFEF4444)
    
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Difficulty", icon = Icons.Default.PieChart, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                DifficultyBar("Easy", data.easy, easyProgress.value, easyColor, glassColors)
                DifficultyBar("Medium", data.medium, mediumProgress.value, mediumColor, glassColors)
                DifficultyBar("Hard", data.hard, hardProgress.value, hardColor, glassColors)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassSubsection(glassColors = glassColors) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DifficultyPill("Easy", (data.easy * 100 / total), easyColor)
                    DifficultyPill("Medium", (data.medium * 100 / total), mediumColor)
                    DifficultyPill("Hard", (data.hard * 100 / total), hardColor)
                }
            }
        }
    }
}

@Composable
private fun DifficultyBar(label: String, count: Int, progress: Float, color: Color, glassColors: GlassColors) {
    val maxHeight = 80.dp
    val animatedHeight = maxHeight * progress * 2.5f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = glassColors.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(animatedHeight.coerceAtMost(maxHeight))
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = glassColors.textPrimary
        )
    }
}

@Composable
private fun DifficultyPill(label: String, percentage: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$percentage%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun PlatformsCard(platforms: List<PlatformData>, hazeState: HazeState, glassColors: GlassColors) {
    val total = platforms.sumOf { it.count }
    
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Platforms", icon = Icons.Default.Category, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            
            platforms.forEach { platform ->
                PlatformRow(platform, total, glassColors)
                if (platform != platforms.last()) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PlatformRow(platform: PlatformData, total: Int, glassColors: GlassColors) {
    val progress by animateFloatAsState(
        targetValue = platform.count.toFloat() / total,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "platformProgress"
    )
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(platform.color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(platform.name, fontSize = 14.sp, color = glassColors.textPrimary)
            }
            Text("${platform.count} solved", fontSize = 12.sp, color = glassColors.textSecondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(glassColors.textSecondary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(platform.color)
            )
        }
    }
}

@Composable
private fun SubmissionStatsCard(stats: SubmissionStats, hazeState: HazeState, glassColors: GlassColors) {
    val acceptedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        acceptedProgress.animateTo(stats.accepted.toFloat() / stats.total, tween(1000, easing = FastOutSlowInEasing))
    }
    
    val acceptedColor = Color(0xFF22C55E)
    val failedColor = Color(0xFFEF4444)
    
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Submission Statistics", icon = Icons.Default.Timeline, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassColors.textSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(acceptedProgress.value)
                        .height(24.dp)
                        .background(acceptedColor)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSubsection(glassColors, Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CheckCircle, "Accepted", tint = acceptedColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stats.accepted.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = glassColors.textPrimary)
                        Text("Accepted", fontSize = 11.sp, color = glassColors.textSecondary)
                    }
                }
                GlassSubsection(glassColors, Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Close, "Failed", tint = failedColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stats.failed.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = glassColors.textPrimary)
                        Text("Failed", fontSize = 11.sp, color = glassColors.textSecondary)
                    }
                }
                GlassSubsection(glassColors, Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Timeline, "Total", tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stats.total.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = glassColors.textPrimary)
                        Text("Total", fontSize = 11.sp, color = glassColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProblemDistributionCard(categories: List<CategoryData>, hazeState: HazeState, glassColors: GlassColors) {
    val maxCount = categories.maxOfOrNull { it.count } ?: 1
    val categoryColors = listOf(
        Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFF84CC16)
    )
    
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Problem Distribution", icon = Icons.Default.Category, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(categories) { index, category ->
                    CategoryChip(category, categoryColors[index % categoryColors.size], glassColors)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassSubsection(glassColors = glassColors) {
                Column {
                    categories.take(4).forEachIndexed { index, category ->
                        CategoryBar(category, maxCount, categoryColors[index % categoryColors.size], glassColors)
                        if (index < 3) Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: CategoryData, color: Color, glassColors: GlassColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
            Spacer(modifier = Modifier.width(6.dp))
            Text(category.count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun CategoryBar(category: CategoryData, maxCount: Int, color: Color, glassColors: GlassColors) {
    val progress by animateFloatAsState(
        targetValue = category.count.toFloat() / maxCount,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "categoryProgress"
    )
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(category.name, fontSize = 12.sp, color = glassColors.textSecondary, modifier = Modifier.width(60.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(glassColors.textSecondary.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(category.count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = glassColors.textPrimary, modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun TimePerformanceCard(avgTime: String, bestTime: String, totalTime: String, hazeState: HazeState, glassColors: GlassColors) {
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Time Performance", icon = Icons.Default.Speed, glassColors = glassColors)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FixedTimeStatBox(Icons.Default.AccessTime, avgTime, "Avg Solve", glassColors, null, Modifier.weight(1f))
                FixedTimeStatBox(Icons.Default.Speed, bestTime, "Best Time", glassColors, Color(0xFF22C55E), Modifier.weight(1f))
                FixedTimeStatBox(Icons.Default.History, totalTime, "Total Time", glassColors, null, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FixedTimeStatBox(icon: ImageVector, value: String, label: String, glassColors: GlassColors, accentColor: Color?, modifier: Modifier) {
    val subsectionBg = if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x30FFFFFF)
    val borderColor = if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x20000000)
    
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(subsectionBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, label, tint = accentColor ?: if (glassColors.isDark) Color.White else Color(0xFFE91E8C), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor ?: glassColors.textPrimary, textAlign = TextAlign.Center)
            Text(label, fontSize = 10.sp, color = glassColors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RecentSolvesCard(solves: List<RecentSolve>, hazeState: HazeState, glassColors: GlassColors) {
    GlassCardContainer(hazeState = hazeState, glassColors = glassColors) {
        Column {
            CardHeader(title = "Recent Solves", icon = Icons.Default.History, glassColors = glassColors)
            Spacer(modifier = Modifier.height(16.dp))
            
            solves.forEachIndexed { index, solve ->
                RecentSolveItem(solve, glassColors)
                if (index < solves.lastIndex) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecentSolveItem(solve: RecentSolve, glassColors: GlassColors) {
    val difficultyColor = when (solve.difficulty.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    GlassSubsection(glassColors = glassColors) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(solve.problemName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = glassColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(solve.platform, fontSize = 11.sp, color = glassColors.textSecondary)
                    Text(" · ", fontSize = 11.sp, color = glassColors.textSecondary)
                    Text(solve.difficulty, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = difficultyColor)
                }
            }
            Text(solve.timeAgo, fontSize = 11.sp, color = glassColors.textSecondary)
        }
    }
}

@Composable
private fun AchievementsSection(
    achievements: List<AchievementData>,
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit = {}
) {
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size
    val percentage = if (totalCount > 0) (unlockedCount.toFloat() / totalCount.toFloat()) else 0f
    
    GlassCardContainer(
        hazeState = hazeState, 
        glassColors = glassColors, 
        modifier = Modifier.clickable(enabled = true) { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE91E8C),
                                    Color(0xFFA855F7)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Achievements",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Achievements",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$unlockedCount of $totalCount unlocked",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                }
            }
            
            // Right side - Circular Progress
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = percentage,
                    modifier = Modifier.size(64.dp),
                    color = if (glassColors.isDark) Color(0xFFE91E8C) else Color(0xFFA855F7),
                    strokeWidth = 6.dp,
                    trackColor = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x30E91E8C)
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
                )
            }
        }
    }
}

@Composable
private fun EmptyAchievementsSection(
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit = {}
) {
    GlassCardContainer(
        hazeState = hazeState, 
        glassColors = glassColors, 
        modifier = Modifier.clickable(enabled = true) { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFE91E8C),
                                    Color(0xFFA855F7)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Achievements",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Achievements",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start solving to unlock",
                        fontSize = 12.sp,
                        color = glassColors.textSecondary
                    )
                }
            }
            
            // Right side - Lock icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (glassColors.isDark) Color(0x20FFFFFF) 
                        else Color(0x20E91E8C)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementItem(achievement: AchievementData, glassColors: GlassColors, modifier: Modifier) {
    if (achievement.name.isEmpty()) {
        return
    }
    
    val bgColor = if (achievement.unlocked) {
        if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x50E91E8C)
    } else {
        if (glassColors.isDark) Color(0x10FFFFFF) else Color(0x20000000)
    }
    
    val borderColor = if (achievement.unlocked) {
        if (glassColors.isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFE91E8C).copy(alpha = 0.5f)
    } else {
        if (glassColors.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    }
    
    Box(
        modifier = modifier
            .height(90.dp)  // Fixed height for uniform sizing
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .alpha(if (achievement.unlocked) 1f else 0.5f)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = achievement.name,
                tint = if (achievement.unlocked) {
                    if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
                } else {
                    glassColors.textSecondary
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = achievement.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
private fun ProblemsSummaryCard(
    completed: Int,
    total: Int,
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit
) {
    val percentage = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0
    val remaining = total - completed
    
    val progressColor = if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
    val trackColor = if (glassColors.isDark) Color(0x30FFFFFF) else Color(0x30E91E8C)
    val successColor = Color(0xFF22C55E)
    val warningColor = Color(0xFFFBBF24)
    
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated circular progress
                val progress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    progress.animateTo(
                        if (total > 0) completed.toFloat() / total else 0f,
                        tween(1200, easing = FastOutSlowInEasing)
                    )
                }
                
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(trackColor, -90f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(progressColor, -90f, 360f * progress.value, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Problems",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Solved",
                            tint = successColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$completed",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = successColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remaining",
                            tint = warningColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$remaining",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = warningColor
                        )
                    }
                }
            }
            
            // Arrow indicator
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "View Problems",
                tint = glassColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
