package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RevisionAnalyticsOverview
import com.traverse.android.data.RevisionAnalyticsResponse
import com.traverse.android.data.RevisionAnalyticsStreaks
import com.traverse.android.data.RevisionRetentionItem
import com.traverse.android.data.RevisionStabilityDistribution
import com.traverse.android.data.RevisionTopicMetric
import com.traverse.android.data.WeeklyCompletion
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlin.math.roundToInt

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

@Composable
fun MLAnalyticsScreen(
    analytics: RevisionAnalyticsResponse,
    modifier: Modifier = Modifier
) {
    var showAllTopicsSheet by remember { mutableStateOf(false) }
    var showAllAtRiskSheet by remember { mutableStateOf(false) }
    var infoSheetData by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overview Card (3 column layout)
        RevisionOverviewCard(
            overview = analytics.overview,
            streaks = analytics.streaks
        )

        // 2. Retention Health Distribution Card (Vertical Bars)
        RevisionStabilityDistributionCard(
            distribution = analytics.stabilityDistribution,
            onInfoClick = {
                infoSheetData = "Retention Health" to "Distribution of your tracked DSA problems across FSRS memory stability tiers: Critical (<2d), Weak (2-7d), Developing (7-21d), Strong (21-60d), and Mastered (60d+)."
            }
        )

        // 3. Weekly Activity Bar Chart
        if (analytics.weeklyCompletion.isNotEmpty()) {
            WeeklyCompletionCard(
                weeklyCompletion = analytics.weeklyCompletion,
                onInfoClick = {
                    infoSheetData = "Weekly Activity" to "Tracks the total volume of spaced repetition problem reviews completed each week over the last 4 weeks."
                }
            )
        }

        // 4. Topic Mastery & Speed Card
        if (analytics.topicBreakdown.isNotEmpty()) {
            RevisionTopicBreakdownCard(
                topics = analytics.topicBreakdown,
                onViewAll = { showAllTopicsSheet = true },
                onInfoClick = {
                    infoSheetData = "Topic Mastery & Speed" to "Shows memory retention rates and average solution speed across distinct DSA categories."
                }
            )
        }

        // 5. At-Risk Problems Card
        if (analytics.retentionHeatmap.isNotEmpty()) {
            RevisionRetentionRiskCard(
                items = analytics.retentionHeatmap,
                onViewAll = { showAllAtRiskSheet = true },
                onInfoClick = {
                    infoSheetData = "At-Risk Problems" to "Highlights 'Leeches' (problems forgotten multiple times) and problems with retention dropping below 60% that require immediate review."
                }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // All Topics Sheet
    if (showAllTopicsSheet) {
        AllTopicsSheet(
            topics = analytics.topicBreakdown,
            onDismiss = { showAllTopicsSheet = false }
        )
    }

    // All At Risk Problems Sheet
    if (showAllAtRiskSheet) {
        AllAtRiskProblemsSheet(
            items = analytics.retentionHeatmap,
            onDismiss = { showAllAtRiskSheet = false }
        )
    }

    // Info Sheet
    infoSheetData?.let { (title, desc) ->
        AnalyticsInfoSheet(
            title = title,
            description = desc,
            onDismiss = { infoSheetData = null }
        )
    }
}

// MARK: - 1. Revision Overview Card
@Composable
private fun RevisionOverviewCard(
    overview: RevisionAnalyticsOverview,
    streaks: RevisionAnalyticsStreaks
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tracked
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${overview.totalProblemsTracked}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = Color.White
                    )
                )
                Text(
                    text = "Tracked",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }

            VerticalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.height(36.dp))

            // Avg Retrievability
            val retrievabilityPct = (overview.averageRetrievability * 100).roundToInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$retrievabilityPct%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = AccentPastel
                    )
                )
                Text(
                    text = "Avg Retrievability",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }

            VerticalDivider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.height(36.dp))

            // Completed
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${streaks.totalRevisionsCompleted}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = EasyPastel
                    )
                )
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

// MARK: - 2. Stability Distribution (Retention Health)
@Composable
private fun RevisionStabilityDistributionCard(
    distribution: RevisionStabilityDistribution,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = AccentPastel,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Retention Health",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val buckets = listOf(
                Triple("Critical", distribution.critical, Color(0xFFEF4444)),
                Triple("Weak", distribution.weak, HardPastel),
                Triple("Developing", distribution.developing, MediumPastel),
                Triple("Strong", distribution.strong, AccentPastel),
                Triple("Mastered", distribution.mastered, EasyPastel)
            )

            val maxCount = (buckets.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

            // Vertical Bars Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEach { (label, count, color) ->
                    val barHeightFraction = (count.toFloat() / maxCount).coerceIn(0.06f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Count on top of bar
                        Text(
                            text = if (count > 0) "$count" else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = color
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Dedicated Bar Container taking the weighted middle area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(color)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Label
                        Text(
                            text = label.take(4),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// MARK: - 3. Weekly Activity Card
@Composable
private fun WeeklyCompletionCard(
    weeklyCompletion: List<WeeklyCompletion>,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = EasyPastel,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            val totalCompletions = weeklyCompletion.sumOf { it.count }
            val currentWeekCount = weeklyCompletion.lastOrNull()?.count ?: 0
            val previousWeekCount = weeklyCompletion.dropLast(1).lastOrNull()?.count ?: 0
            val delta = currentWeekCount - previousWeekCount

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalCompletions completed (4w)",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f))
                )

                if (delta != 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (delta > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (delta > 0) EasyPastel else HardPastel,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${if (delta > 0) "+$delta" else "$delta"} this wk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (delta > 0) EasyPastel else HardPastel
                            )
                        )
                    }
                }
            }

            val maxCount = (weeklyCompletion.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
            val weekLabels = listOf("3w ago", "2w ago", "Last wk", "This wk")

            // 4 Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyCompletion.takeLast(4).forEachIndexed { idx, item ->
                    val isCurrentWeek = idx == weeklyCompletion.takeLast(4).size - 1
                    val barHeightFraction = (item.count.toFloat() / maxCount).coerceIn(0.06f, 1f)
                    val barColor = if (isCurrentWeek) EasyPastel else EasyPastel.copy(alpha = 0.45f)
                    val label = weekLabels.getOrNull(idx) ?: item.week

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = if (item.count > 0) "${item.count}" else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Dedicated Bar Container taking the weighted middle area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = if (isCurrentWeek) 0.9f else 0.5f),
                                fontWeight = if (isCurrentWeek) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// MARK: - 4. Topic Mastery & Speed Card
@Composable
private fun RevisionTopicBreakdownCard(
    topics: List<RevisionTopicMetric>,
    onViewAll: () -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MediumPastel,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Topic Mastery & Speed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Top 4 Topics
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                topics.take(4).forEach { topic ->
                    val retentionPct = (topic.averageRetention * 100).roundToInt()
                    val retentionColor = when {
                        retentionPct >= 80 -> EasyPastel
                        retentionPct >= 60 -> MediumPastel
                        else -> HardPastel
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = topic.displayTopic,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "• ${topic.problemCount}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.45f))
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (topic.averageTimeMinutes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = "${"%.1f".format(topic.averageTimeMinutes)}m",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.75f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$retentionPct%",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = retentionColor
                                    )
                                )
                            }
                        }

                        // Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = topic.averageRetention.toFloat().coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(retentionColor)
                            )
                        }
                    }
                }
            }

            // View All Topics footer button
            if (topics.size > 4) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewAll() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All Topics (${topics.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentPastel
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AccentPastel,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// MARK: - 5. At-Risk Problems Card
@Composable
private fun RevisionRetentionRiskCard(
    items: List<RevisionRetentionItem>,
    onViewAll: () -> Unit,
    onInfoClick: () -> Unit
) {
    val leechesCount = items.count { it.isLeech }
    val lowRetentionCount = items.count { it.retrievability < 0.6 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = HardPastel,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "At-Risk Problems",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = BelfastGroteskBlackFamily,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Top Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Leeches", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "$leechesCount",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MediumPastel.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Below 60%", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "$lowRetentionCount",
                            fontWeight = FontWeight.Bold,
                            color = MediumPastel
                        )
                    }
                }
            }

            // Focus Items List (sorted by leeches first, then lowest retrievability)
            val focusItems = items.sortedWith(
                compareByDescending<RevisionRetentionItem> { it.isLeech }
                    .thenBy { it.retrievability }
                    .thenByDescending { it.lapses }
            ).take(5)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                focusItems.forEach { item ->
                    val retrievabilityPct = (item.retrievability * 100).roundToInt()
                    val dotColor = when {
                        item.isLeech || retrievabilityPct < 50 -> Color(0xFFEF4444)
                        retrievabilityPct < 70 -> MediumPastel
                        else -> EasyPastel
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        // Title
                        Text(
                            text = item.problemTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Mini progress bar
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = item.retrievability.toFloat().coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(dotColor)
                            )
                        }

                        // Percentage
                        Text(
                            text = "$retrievabilityPct%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = dotColor
                            ),
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }

            // View All Button
            if (items.size > 5) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewAll() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All At-Risk Problems (${items.size})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentPastel
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AccentPastel,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
