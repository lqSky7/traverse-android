package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `PerformanceMetricsCard` ("Time Performance"): average / fastest time
 * summary above a line + area chart of the time taken per problem, oldest first.
 *
 * Palette mapping from iOS: the line, its points and the average-time value use
 * `color(at: 5)`; the fastest-time value and the far end of both gradients use `color(at: 6)`.
 */
@Composable
fun PerformanceMetricsCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val lineColor = palette.colorAt(5)
    val fastestColor = palette.colorAt(6)

    // iOS: solves.compactMap { timeTaken }.reversed()
    val timeData = remember(solves) {
        solves.asReversed().mapNotNull { it.submission.timeTaken }
    }

    val averageTime = remember(timeData) {
        if (timeData.isEmpty()) 0 else timeData.sum() / timeData.size
    }
    val fastestTime = remember(timeData) { timeData.minOrNull() ?: 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = lineColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Time Performance",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        if (timeData.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = formatTime(averageTime),
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                    Text(
                        text = "Average Time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTime(fastestTime),
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = fastestColor
                    )
                    Text(
                        text = "Fastest",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (timeData.isEmpty()) return@Canvas

                val maxTime = timeData.maxOrNull() ?: 1
                val minTime = timeData.minOrNull() ?: 0
                val range = (maxTime - minTime).coerceAtLeast(1)

                val padding = 8.dp.toPx()
                val chartWidth = (size.width - 2 * padding).coerceAtLeast(1f)
                val chartHeight = (size.height - 2 * padding).coerceAtLeast(1f)

                val points = timeData.mapIndexed { index, value ->
                    val x = if (timeData.size == 1) {
                        size.width / 2f
                    } else {
                        padding + (index.toFloat() / (timeData.size - 1)) * chartWidth
                    }
                    val normalized = (value - minTime).toFloat() / range.toFloat()
                    Offset(x, padding + (1f - normalized) * chartHeight)
                }

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                // Area fill under the line
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            fastestColor.copy(alpha = 0.1f)
                        )
                    )
                )

                // Line
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(listOf(lineColor, fastestColor)),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Points
                points.forEach { point ->
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = point)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No time data available",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
