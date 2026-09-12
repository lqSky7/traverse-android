package com.traverse.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.Duration
import java.time.LocalDateTime

private val CardBackground = Color(0xFF1A1A1A)

/** SwiftUI `.green` — the unlocked checkmark on iOS. */
private val UnlockedGreen = Color(0xFF34C759)

/** SwiftUI's default accent (`Color.blue`) — used by the iOS filter `Menu`. */
private val MenuAccent = Color(0xFF007AFF)

enum class AchievementFilter(val label: String) {
    ALL("All"),
    UNLOCKED("Unlocked"),
    LOCKED("Locked")
}

/**
 * 1:1 port of the iOS `AllAchievementsView`: a sticky glass summary card (Total / Progress /
 * Remaining), achievements grouped into expandable category cards, and a toolbar filter menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAchievementsScreen(
    achievements: List<AchievementDetail>,
    stats: AchievementStatsData?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterMode by remember { mutableStateOf(AchievementFilter.ALL) }
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    val filteredAchievements = remember(achievements, filterMode) {
        when (filterMode) {
            AchievementFilter.ALL -> achievements
            AchievementFilter.UNLOCKED -> achievements.filter { it.unlocked }
            AchievementFilter.LOCKED -> achievements.filter { !it.unlocked }
        }
    }

    val grouped = remember(filteredAchievements) {
        filteredAchievements.groupBy { it.category }.toSortedMap(compareBy { it.lowercase() })
    }

    Scaffold(
        modifier = modifier,
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
                            AchievementFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = filter.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (filterMode == filter) MenuAccent else Color.White
                                                )
                                            )
                                            if (filterMode == filter) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MenuAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        filterMode = filter
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 146.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Achievements by Category",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                items(grouped.keys.toList(), key = { it }) { category ->
                    AchievementCategoryCard(
                        category = category,
                        achievements = grouped[category].orEmpty(),
                        isExpanded = expandedCategory == category,
                        onToggle = {
                            expandedCategory = if (expandedCategory == category) null else category
                        }
                    )
                }
            }

            // Sticky summary card
            stats?.let { statsData ->
                SummaryCard(
                    total = statsData.total,
                    unlocked = statsData.unlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp)
                )
            }
        }
    }
}

/**
 * Glass-effect summary card. Compose has no native glass material, so this approximates
 * `glassEffect(.regular.interactive())` with a translucent panel and hairline border.
 */
@Composable
private fun SummaryCard(
    total: Int,
    unlocked: Int,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val progressPercentage = if (total > 0) ((unlocked.toDouble() / total) * 100).toInt() else 0
    val remaining = total - unlocked

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SummaryStat(value = "$total", label = "Total", color = palette.colorAt(0), modifier = Modifier.weight(1f))
        SummaryDivider()
        SummaryStat(value = "$progressPercentage%", label = "Progress", color = palette.colorAt(3), modifier = Modifier.weight(1f))
        SummaryDivider()
        SummaryStat(value = "$remaining", label = "Remaining", color = Color.Gray, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
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
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
}

@Composable
private fun AchievementCategoryCard(
    category: String,
    achievements: List<AchievementDetail>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val unlockedCount = achievements.count { it.unlocked }
    val categoryColor = categoryColor(category)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "achievementCategoryChevron"
    )

    val sortedAchievements = achievements.sortedWith(
        compareByDescending<AchievementDetail> { it.unlocked }.thenBy { it.name.lowercase() }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "$unlockedCount of ${achievements.size} unlocked",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.25f))
                sortedAchievements.forEachIndexed { index, achievement ->
                    AchievementRow(achievement = achievement)
                    if (index < sortedAchievements.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 68.dp)
                        ) {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementDetail) {
    val categoryColor = if (achievement.unlocked) categoryColor(achievement.category) else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer { alpha = if (achievement.unlocked) 1f else 0.65f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (achievement.unlocked) categoryColor.copy(alpha = 0.18f)
                    else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achievement.unlocked) {
                    achievementIcon(achievement.icon, achievement.category)
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (achievement.unlocked) categoryColor else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(if (achievement.unlocked) 18.dp else 15.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (achievement.unlocked) Color.White else Color.Gray
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (achievement.unlocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = UnlockedGreen,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (achievement.unlocked && achievement.unlockedAt != null) {
                Text(
                    text = "Unlocked ${formatUnlockedDate(achievement.unlockedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = categoryColor.copy(alpha = 0.9f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun categoryColor(category: String): Color {
    val palette = rememberPalette()
    return when (category.lowercase()) {
        "solve", "solves" -> palette.colorAt(1)
        "streak" -> palette.colorAt(0)
        "social" -> palette.colorAt(2)
        "revision", "revisions", "ml" -> palette.colorAt(4)
        else -> palette.colorAt(3)
    }
}

private fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "solve", "solves" -> Icons.Default.CheckCircle
    "streak" -> Icons.Default.LocalFireDepartment
    "social" -> Icons.Default.People
    "revision", "revisions", "ml" -> Icons.Default.Psychology
    else -> Icons.Default.EmojiEvents
}

/** Maps the backend's SF Symbol icon names onto Material equivalents. */
private fun achievementIcon(icon: String?, category: String): ImageVector {
    val lower = icon?.lowercase().orEmpty()
    return when {
        lower.contains("flame") || lower.contains("fire") -> Icons.Default.LocalFireDepartment
        lower.contains("trophy") -> Icons.Default.EmojiEvents
        lower.contains("star") -> Icons.Default.Star
        lower.contains("bolt") || lower.contains("zap") -> Icons.Default.Bolt
        lower.contains("brain") -> Icons.Default.Psychology
        lower.contains("crown") -> Icons.Default.WorkspacePremium
        lower.contains("target") -> Icons.Default.TrackChanges
        lower.contains("seal") || lower.contains("badge") -> Icons.Default.Verified
        lower.contains("chart") -> Icons.Default.TrendingUp
        lower.contains("sparkle") -> Icons.Default.AutoAwesome
        lower.contains("award") -> Icons.Default.EmojiEvents
        else -> categoryIcon(category)
    }
}

private fun formatUnlockedDate(dateString: String): String {
    val parsed = try {
        LocalDateTime.parse(dateString.take(19))
    } catch (_: Exception) {
        null
    } ?: return "recently"

    val duration = Duration.between(parsed, LocalDateTime.now())
    val days = duration.toDays()
    val hours = duration.toHours()
    val minutes = duration.toMinutes()

    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "just now"
    }
}
