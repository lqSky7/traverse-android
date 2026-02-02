package com.traverse.android.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.AchievementStatsData

private val CardBackground = Color(0xFF1A1A1A)
private val GlowPastelPink = Color(0xFFFFB6C1)
private val GlowPastelPurple = Color(0xFFE6E6FA)

// Category icons
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
                title = { Text("All Achievements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
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
                                                fontFamily = com.traverse.android.ui.theme.BelfastGroteskBlackFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = if (filterMode == filter) GlowPastelPink else Color.White
                                            )
                                        )
                                        if (filterMode == filter) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = GlowPastelPink,
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
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary card
            stats?.let { statsData ->
                item {
                    AchievementSummaryCard(
                        total = statsData.total,
                        unlocked = statsData.unlocked,
                        remaining = statsData.total - statsData.unlocked
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

@Composable
private fun AchievementSummaryCard(total: Int, unlocked: Int, remaining: Int) {
    val progress = unlocked.toFloat() / total.coerceAtLeast(1)
    val percentage = (progress * 100).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatItem(value = "$total", label = "Total", color = GlowPastelPink)
                VerticalDivider(modifier = Modifier.height(50.dp), color = Color.White.copy(alpha = 0.2f))
                SummaryStatItem(value = "$percentage%", label = "Progress", color = GlowPastelPurple)
                VerticalDivider(modifier = Modifier.height(50.dp), color = Color.White.copy(alpha = 0.2f))
                SummaryStatItem(value = "$remaining", label = "Remaining", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(color = color))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f)))
    }
}

@Composable
private fun ExpandableAchievementCard(
    achievement: AchievementDetail,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val glowAlpha = if (achievement.unlocked) 0.4f else 0f
    val categoryIcon = categoryIcons[achievement.category] ?: Icons.Default.EmojiEvents
    
    Box {
        if (glowAlpha > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .blur(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlowPastelPink.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground.copy(alpha = if (achievement.unlocked) 1f else 0.6f)
            ),
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (achievement.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (achievement.unlocked) GlowPastelPink else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = achievement.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (achievement.unlocked) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = achievement.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Expandable details
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = if (achievement.unlocked) 0.7f else 0.4f)
                            )
                        )
                        
                        achievement.unlockedAt?.let { date ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unlocked ${formatDate(date)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = GlowPastelPink.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString.take(10))
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
        date.format(formatter)
    } catch (e: Exception) {
        dateString.take(10)
    }
}
