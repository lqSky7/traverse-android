package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.traverse.android.data.RevisionAnalyticsResponse
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlin.math.roundToInt

// Pastel colors matching Android app's monochromish-pastel theme
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Section
        OverviewSection(analytics.overview)
        
        // Stability Distribution
        StabilityDistributionSection(analytics.stabilityDistribution)
        
        // Streaks Section
        StreaksSection(analytics.streaks)
        
        // Accuracy Trend
        if (analytics.accuracyTrend.isNotEmpty()) {
            AccuracyTrendSection(analytics.accuracyTrend)
        }
        
        // Projected Load
        if (analytics.projectedLoad.isNotEmpty()) {
            ProjectedLoadSection(analytics.projectedLoad)
        }
        
        // Interval Growth
        if (analytics.intervalGrowth.isNotEmpty()) {
            IntervalGrowthSection(analytics.intervalGrowth)
        }
        
        // Retention Heatmap
        if (analytics.retentionHeatmap.isNotEmpty()) {
            RetentionHeatmapSection(analytics.retentionHeatmap)
        }
    }
}

@Composable
private fun OverviewSection(overview: com.traverse.android.data.RevisionAnalyticsOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Tracked",
                    value = overview.totalProblemsTracked.toString(),
                    icon = Icons.Default.Psychology,
                    color = AccentPastel,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Mastered",
                    value = overview.masteredProblems.toString(),
                    icon = Icons.Default.EmojiEvents,
                    color = EasyPastel,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Leeches",
                    value = overview.leechProblems.toString(),
                    icon = Icons.Default.Warning,
                    color = HardPastel,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Avg Stability",
                    value = "${(overview.averageStability * 100).roundToInt()}%",
                    icon = Icons.Default.TrendingUp,
                    color = MediumPastel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
private fun StabilityDistributionSection(distribution: com.traverse.android.data.RevisionStabilityDistribution) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Stability Distribution",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            val total = distribution.critical + distribution.weak + distribution.developing + 
                       distribution.strong + distribution.mastered
            
            if (total > 0) {
                StabilityBar(
                    label = "Critical",
                    count = distribution.critical,
                    total = total,
                    color = Color(0xFFEF4444)
                )
                StabilityBar(
                    label = "Weak",
                    count = distribution.weak,
                    total = total,
                    color = HardPastel
                )
                StabilityBar(
                    label = "Developing",
                    count = distribution.developing,
                    total = total,
                    color = MediumPastel
                )
                StabilityBar(
                    label = "Strong",
                    count = distribution.strong,
                    total = total,
                    color = AccentPastel
                )
                StabilityBar(
                    label = "Mastered",
                    count = distribution.mastered,
                    total = total,
                    color = EasyPastel
                )
            } else {
                Text(
                    text = "No data available yet",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun StabilityBar(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = "$count (${(count.toFloat() / total * 100).roundToInt()}%)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(count.toFloat() / total)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StreaksSection(streaks: com.traverse.android.data.RevisionAnalyticsStreaks) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Performance",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Completed",
                    value = streaks.totalRevisionsCompleted.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = EasyPastel,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Success Rate",
                    value = "${(streaks.overallSuccessRate * 100).roundToInt()}%",
                    icon = Icons.Default.TrendingUp,
                    color = AccentPastel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AccuracyTrendSection(accuracyTrend: List<com.traverse.android.data.RevisionAccuracyPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Accuracy Trend",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Text(
                text = "Last ${accuracyTrend.size} data points",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            
            // Simple list view (charts would require Vico library integration)
            accuracyTrend.takeLast(5).forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = point.date,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${(point.successRate * 100).roundToInt()}% (${point.totalAttempts} attempts)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectedLoadSection(projectedLoad: List<com.traverse.android.data.RevisionProjectedLoad>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Projected Load",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Text(
                text = "Next ${projectedLoad.size} days",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            
            projectedLoad.take(7).forEach { load ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = load.date,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${load.dueCount} due${if (load.overdueCount > 0) " (+${load.overdueCount} overdue)" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (load.overdueCount > 0) HardPastel else Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalGrowthSection(intervalGrowth: List<com.traverse.android.data.RevisionIntervalGrowth>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Interval Growth",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Text(
                text = "Average review intervals by month",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            
            intervalGrowth.forEach { growth ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = growth.month,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${growth.avgInterval.roundToInt()} days (${growth.count} reviews)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RetentionHeatmapSection(retentionHeatmap: List<com.traverse.android.data.RevisionRetentionItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Retention Heatmap",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = BelfastGroteskBlackFamily,
                    color = Color.White
                )
            )
            
            Text(
                text = "Top ${retentionHeatmap.size} problems by retention",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            
            retentionHeatmap.take(10).forEach { item ->
                RetentionItem(item)
            }
        }
    }
}

@Composable
private fun RetentionItem(item: com.traverse.android.data.RevisionRetentionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.problemTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "${item.platform} • ${item.difficulty}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                
                if (item.isLeech) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Leech",
                        tint = HardPastel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Retrievability",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "${(item.retrievability * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Column {
                    Text(
                        text = "Stability",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = "${(item.stability * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Column {
                    Text(
                        text = "Lapses",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        text = item.lapses.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (item.lapses > 3) HardPastel else Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
