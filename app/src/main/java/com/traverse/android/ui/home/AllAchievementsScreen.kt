package com.traverse.android.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import kotlin.math.sin

private val CardBackground = Color(0xFF141414)
private val RainbowColors = listOf(
    Color(0xFFFFB6C1), // Pastel Pink
    Color(0xFFB6E3FF), // Pastel Sky Blue
    Color(0xFFFFE4B6), // Pastel Peach
    Color(0xFFB6FFD8), // Pastel Mint
    Color(0xFFE6B6FF), // Pastel Purple
    Color(0xFFFFF0B6)  // Pastel Yellow
)

private val categoryIcons = mapOf(
    "Streaks" to Icons.Default.LocalFireDepartment,
    "Progress" to Icons.Default.TrendingUp,
    "Speed" to Icons.Default.Speed,
    "Premium" to Icons.Default.Star,
    "Social" to Icons.Default.People,
    "Variety" to Icons.Default.Category,
    "Difficulty" to Icons.Default.Psychology,
    "Milestones" to Icons.Default.EmojiEvents,
    "Community" to Icons.Default.Groups,
    "Learning" to Icons.Default.School
)

enum class AchievementFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAchievementsScreen(
    achievements: List<AchievementDetail>,
    stats: AchievementStatsData?,
    onBack: () -> Unit
) {
    var filterMode by remember { mutableStateOf(AchievementFilter.ALL) }
    var expandedAchievementId by remember { mutableStateOf<Int?>(null) }

    val filteredAchievements = remember(achievements, filterMode) {
        when (filterMode) {
            AchievementFilter.ALL -> achievements
            AchievementFilter.UNLOCKED -> achievements.filter { it.unlocked }
            AchievementFilter.LOCKED -> achievements.filter { !it.unlocked }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Achievements",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = CardBackground,
                            modifier = Modifier.width(180.dp)
                        ) {
                            AchievementFilter.entries.forEachIndexed { index, filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                filter.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = BelfastGroteskBlackFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (filterMode == filter) RainbowColors[0] else Color.White
                                                )
                                            )
                                            if (filterMode == filter) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = RainbowColors[0],
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        filterMode = filter
                                        showMenu = false
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                if (index < AchievementFilter.entries.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary card with Rainbow Mesh Shader
            stats?.let { statsData ->
                item {
                    RainbowMeshSummaryCard(
                        total = statsData.total,
                        unlocked = statsData.unlocked,
                        remaining = statsData.total - statsData.unlocked
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Achievements as expandable cards
            items(filteredAchievements, key = { it.id }) { achievement ->
                ExpandableAchievementCard(
                    achievement = achievement,
                    isExpanded = expandedAchievementId == achievement.id,
                    onClick = {
                        expandedAchievementId = if (expandedAchievementId == achievement.id) null else achievement.id
                    }
                )
            }
        }
    }
}

// MARK: - Rainbow Mesh Shader Summary Card
@Composable
private fun RainbowMeshSummaryCard(total: Int, unlocked: Int, remaining: Int) {
    val progress = unlocked.toFloat() / total.coerceAtLeast(1)
    val percentage = (progress * 100).toInt()

    // Smooth infinite animation phase for rainbow mesh shader
    val infiniteTransition = rememberInfiniteTransition(label = "RainbowMeshTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Animated Rainbow Mesh Background Layers
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF0C0E14))
        ) {
            // Orb 1: Pastel Pink (rotating top-left)
            val orb1X = (0.25f + 0.2f * sin(phase))
            val orb1Y = (0.3f + 0.2f * sin(phase + 1f))
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (orb1X * 120).dp - 60.dp, y = (orb1Y * 60).dp - 40.dp)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RainbowColors[0].copy(alpha = 0.45f),
                                RainbowColors[0].copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Orb 2: Pastel Sky Blue (rotating top-right)
            val orb2X = (0.7f - 0.2f * sin(phase + 2f))
            val orb2Y = (0.4f + 0.2f * sin(phase))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (orb2X * 60).dp - 40.dp, y = (orb2Y * 60).dp - 40.dp)
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RainbowColors[1].copy(alpha = 0.4f),
                                RainbowColors[1].copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Orb 3: Pastel Mint / Purple (bottom sweep)
            val orb3X = (0.5f + 0.3f * sin(phase + 3.5f))
            val orb3Y = (0.7f - 0.2f * sin(phase + 1.5f))
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.BottomCenter)
                    .offset(x = (orb3X * 80).dp - 40.dp, y = (orb3Y * 40).dp - 20.dp)
                    .blur(65.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RainbowColors[4].copy(alpha = 0.35f),
                                RainbowColors[3].copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Glass / Translucent Overlay Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.35f),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 3 Stats with vertical dividers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryStatItem(
                        value = "$total",
                        label = "Total",
                        color = RainbowColors[0]
                    )
                    VerticalDivider(
                        modifier = Modifier.height(44.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    SummaryStatItem(
                        value = "$percentage%",
                        label = "Progress",
                        color = RainbowColors[3]
                    )
                    VerticalDivider(
                        modifier = Modifier.height(44.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    SummaryStatItem(
                        value = "$remaining",
                        label = "Remaining",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Rainbow Linear Progress Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        RainbowColors[0],
                                        RainbowColors[1],
                                        RainbowColors[3],
                                        RainbowColors[4]
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = BelfastGroteskBlackFamily,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun ExpandableAchievementCard(
    achievement: AchievementDetail,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val categoryIcon = categoryIcons[achievement.category] ?: Icons.Default.EmojiEvents
    val iconColor = if (achievement.unlocked) RainbowColors[0] else Color.White.copy(alpha = 0.3f)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.unlocked) RainbowColors[0].copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (achievement.unlocked) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = achievement.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }

                if (achievement.unlocked) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Unlocked",
                        tint = RainbowColors[3],
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    )
                    if (achievement.unlocked && achievement.unlockedAt != null) {
                        Text(
                            text = "Unlocked on ${achievement.unlockedAt.take(10)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = RainbowColors[3],
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}
