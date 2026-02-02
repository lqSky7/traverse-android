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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import kotlin.math.max
import kotlin.math.min

private val CardBackground = Color(0xFF1A1A1A)
private val LineColor = Color(0xFFDDD6FE) // Pastel lavender

@Composable
fun TimePerformanceCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    // Calculate time data for line chart
    val timeData = remember(solves) {
        solves.takeLast(8).mapNotNull { solve ->
            solve.submission.timeTaken?.toFloat()
        }
    }
    
    val avgTime = remember(solves) {
        val timesWithValue = solves.mapNotNull { it.submission.timeTaken }.filter { it > 0 }
        if (timesWithValue.isEmpty()) 0 else timesWithValue.average().toInt()
    }
    
    val fastestTime = remember(solves) {
        val timesWithValue = solves.mapNotNull { it.submission.timeTaken }.filter { it > 0 }
        if (timesWithValue.isEmpty()) 0 else timesWithValue.minOrNull() ?: 0
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = LineColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Time Performance",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Stats Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Average Time (left side - large)
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = formatTimeLarge(avgTime),
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = LineColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                    )
                    Text(
                        text = "Average Time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                
                // Fastest Time (right side - smaller)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatTimeLarge(fastestTime),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Fastest",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
            
            // Line Chart
            if (timeData.isNotEmpty() && timeData.size > 1) {
                TimeLineChart(
                    timeData = timeData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Need more data for chart",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeLineChart(
    timeData: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawTimeLineChart(timeData)
    }
}

private fun DrawScope.drawTimeLineChart(timeData: List<Float>) {
    if (timeData.size < 2) return
    
    val maxTime = timeData.maxOrNull() ?: 1f
    val minTime = timeData.minOrNull() ?: 0f
    val range = maxTime - minTime
    val normalizedRange = if (range == 0f) 1f else range
    
    val width = size.width
    val height = size.height
    val padding = 20f
    val chartWidth = width - 2 * padding
    val chartHeight = height - 2 * padding
    
    // Calculate points (fix Y-axis - higher values at top)
    val points = timeData.mapIndexed { index, time ->
        val x = padding + (index.toFloat() / (timeData.size - 1)) * chartWidth
        val normalizedY = (time - minTime) / normalizedRange
        val y = padding + (1f - normalizedY) * chartHeight  // Fixed: flip Y coordinate
        Offset(x, y)
    }
    
    // Draw smooth curve path
    val path = Path().apply {
        if (points.isNotEmpty()) {
            moveTo(points[0].x, points[0].y)
            
            if (points.size > 2) {
                // Create smooth Bezier curves between points
                for (i in 1 until points.size) {
                    val prevPoint = points[i - 1]
                    val currentPoint = points[i]
                    
                    // Control points for smooth curve
                    val controlPoint1X = prevPoint.x + (currentPoint.x - prevPoint.x) * 0.5f
                    val controlPoint1Y = prevPoint.y
                    val controlPoint2X = prevPoint.x + (currentPoint.x - prevPoint.x) * 0.5f
                    val controlPoint2Y = currentPoint.y
                    
                    cubicTo(
                        controlPoint1X, controlPoint1Y,
                        controlPoint2X, controlPoint2Y,
                        currentPoint.x, currentPoint.y
                    )
                }
            } else {
                // Fallback to straight line for 2 points
                lineTo(points[1].x, points[1].y)
            }
        }
    }
    
    drawPath(
        path = path,
        color = LineColor,
        style = Stroke(width = 3.dp.toPx())
    )
    
    // Draw points
    points.forEach { point ->
        drawCircle(
            color = LineColor,
            radius = 4.dp.toPx(),
            center = point
        )
    }
}

private fun formatTimeLarge(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}
