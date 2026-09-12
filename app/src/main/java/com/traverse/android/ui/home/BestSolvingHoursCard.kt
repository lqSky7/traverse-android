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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDateTime

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `BestSolvingHoursCard`: peak / fastest hour summary plus a 24-bar
 * hourly activity histogram with labels at 12am, 6am, 12pm and 6pm.
 *
 * Palette mapping from iOS: header icon, peak hour and bars use `color(at: 6)`;
 * the fastest-hour value uses `color(at: 0)`.
 */
@Composable
fun BestSolvingHoursCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val hoursColor = palette.colorAt(6)
    val fastestColor = palette.colorAt(0)

    val hourCounts = remember(solves) {
        val counts = IntArray(24)
        solves.forEach { solve ->
            parseHour(solve.solvedAt)?.let { hour -> counts[hour]++ }
        }
        counts
    }

    val hourAverages = remember(solves) {
        val totals = DoubleArray(24)
        val counts = IntArray(24)
        solves.forEach { solve ->
            val time = solve.submission.timeTaken
            if (time != null && time > 0) {
                parseHour(solve.solvedAt)?.let { hour ->
                    totals[hour] += time.toDouble()
                    counts[hour]++
                }
            }
        }
        (0 until 24).map { hour ->
            if (counts[hour] == 0) 0.0 else totals[hour] / counts[hour]
        }
    }

    val maxCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakHour = hourCounts.indices.maxByOrNull { hourCounts[it] } ?: 0
    val fastestHourIndex = hourAverages.indices
        .filter { hourAverages[it] > 0.0 }
        .minByOrNull { hourAverages[it] }

    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))

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
                tint = hoursColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Solving Hours",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = formatHour(peakHour),
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = hoursColor
                )
                Text(
                    text = "Peak Hour",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            if (fastestHourIndex != null) {
                Spacer(modifier = Modifier.width(20.dp))
                VerticalDivider(
                    modifier = Modifier.height(50.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = formatHour(fastestHourIndex),
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = fastestColor
                    )
                    Text(
                        text = "Fastest (${formatDuration(hourAverages[fastestHourIndex])})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            val xAxisHeight = 16.dp.toPx()
            val plotHeight = (size.height - xAxisHeight).coerceAtLeast(1f)
            val slot = size.width / 24f
            val barWidth = (slot - 2.dp.toPx()).coerceAtLeast(1f)

            for (hour in 0 until 24) {
                val count = hourCounts[hour]
                if (count <= 0) continue
                val barHeight = (count.toFloat() / maxCount) * plotHeight
                val left = slot * hour + (slot - barWidth) / 2f
                drawRoundRect(
                    color = if (hour == peakHour) hoursColor else hoursColor.copy(alpha = 0.4f),
                    topLeft = Offset(left, plotHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }

            // X axis labels at 0, 6, 12 and 18 — matching the iOS AxisMarks values.
            listOf(0, 6, 12, 18).forEach { hour ->
                val layout = textMeasurer.measure(formatHour(hour), axisStyle)
                val centerX = slot * (hour + 0.5f)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = centerX - layout.size.width / 2f,
                        y = plotHeight + 4.dp.toPx()
                    )
                )
            }
        }
    }
}

private fun parseHour(raw: String): Int? = try {
    LocalDateTime.parse(raw.take(19)).hour
} catch (_: Exception) {
    null
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12am"
    hour < 12 -> "${hour}am"
    hour == 12 -> "12pm"
    else -> "${hour - 12}pm"
}

private fun formatDuration(seconds: Double): String {
    val minutes = seconds.toInt() / 60
    return if (minutes > 0) "${minutes}m" else "${seconds.toInt()}s"
}
