package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import java.time.LocalDateTime

private val CardBackground = Color(0xFF1A1A1A)

@Composable
fun ProductivityInsightsCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    // Calculate hourly distribution
    val hourCounts = remember(solves) {
        val counts = IntArray(24)
        solves.forEach { solve ->
            try {
                val hour = LocalDateTime.parse(solve.solvedAt.take(19)).hour
                counts[hour]++
            } catch (_: Exception) {}
        }
        counts
    }
    
    val maxCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 12
    
    // Find fastest solve time
    val fastestSolve = solves.minByOrNull { it.submission.timeTaken ?: Int.MAX_VALUE }
    val fastestHour = try {
        LocalDateTime.parse(fastestSolve?.solvedAt?.take(19)).hour
    } catch (_: Exception) { null }
    val fastestTime = fastestSolve?.submission?.timeTaken
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Solving Hours",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatHour(peakHour),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Peak Hour",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
                
                if (fastestHour != null && fastestTime != null) {
                    Spacer(modifier = Modifier.width(24.dp))
                    VerticalDivider(
                        modifier = Modifier.height(50.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Column {
                        Text(
                            text = formatHour(fastestHour),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "Fastest (${fastestTime / 60}m)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bar chart
            HourlyBarChart(
                hourCounts = hourCounts,
                maxCount = maxCount,
                peakHour = peakHour,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12am", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
                Text("6am", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
                Text("12pm", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
                Text("6pm", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}

@Composable
private fun HourlyBarChart(
    hourCounts: IntArray,
    maxCount: Int,
    peakHour: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width / 24f - 2f
        val maxHeight = size.height
        
        for (hour in 0 until 24) {
            val count = hourCounts[hour]
            val barHeight = if (maxCount > 0) (count.toFloat() / maxCount) * maxHeight else 0f
            val isPeak = hour == peakHour
            
            val color = if (isPeak) Color.White else Color.White.copy(alpha = 0.4f)
            
            drawRoundRect(
                color = color,
                topLeft = Offset(hour * (barWidth + 2f), maxHeight - barHeight.coerceAtLeast(if (count > 0) 4f else 0f)),
                size = Size(barWidth, barHeight.coerceAtLeast(if (count > 0) 4f else 0f)),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12am"
        hour < 12 -> "${hour}am"
        hour == 12 -> "12pm"
        else -> "${hour - 12}pm"
    }
}
