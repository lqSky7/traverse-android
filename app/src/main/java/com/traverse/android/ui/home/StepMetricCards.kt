package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.ActivityMetrics
import com.traverse.android.data.DayValue
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

// MARK: - Formatting

/** Mirrors iOS `MetricFormat`. */
object MetricFormat {
    fun minutes(value: Double): String {
        val total = value.roundToInt()
        if (total <= 0) return "0m"
        val hours = total / 60
        val mins = total % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /** Compact form for chart axis labels: "2h", "45m". */
    fun minutesShort(value: Double): String {
        val total = value.roundToInt()
        if (total <= 0) return "0"
        if (total >= 60) {
            val hours = total / 60.0
            return if (hours == floor(hours)) "${hours.toInt()}h"
            else String.format(Locale.US, "%.1fh", hours)
        }
        return "${total}m"
    }

    fun count(value: Double): String = String.format(Locale.US, "%,d", value.roundToInt())
}

// MARK: - Bar point

/**
 * One bar in a card's strip. Positional rather than date-keyed so the axis stays evenly spaced
 * regardless of whether the buckets are hours or days.
 */
data class MetricBarPoint(
    val index: Int,
    val label: String,
    val value: Double
)

private val metricWeekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEEE", Locale.US)

/** Seven days of buckets, oldest first, labelled with single weekday letters. */
private fun weeklyBarPoints(solves: List<Solve>, value: (Solve) -> Double): List<MetricBarPoint> =
    ActivityMetrics.daily(solves = solves, days = 7, value = value)
        .mapIndexed { index, bucket: DayValue ->
            MetricBarPoint(
                index = index,
                label = bucket.date.format(metricWeekdayFormatter),
                value = bucket.value
            )
        }

// MARK: - Time Analysis

/**
 * Time and attempt analysis, rebuilt in the shape of the Apple Fitness "Step Count" tile: a title
 * with a circular chevron, a period caption, one large coloured number, and a thin bar strip
 * underneath. 1:1 port of the iOS `TimeAnalysisCard` / `AttemptsAnalysisCard` / `StepMetricCard`.
 *
 * Both cards default to the last seven days rather than today. A single day is empty for anyone
 * who does not solve every day, and an all-zero strip reads as a broken chart rather than a rest
 * day. Seven bars also give the strip a shape to compare against, which is the whole point of
 * putting it on the feed.
 */
@Composable
fun TimeAnalysisCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = rememberPalette()
    val points = remember(solves) {
        weeklyBarPoints(solves) { (it.submission.timeTaken ?: 0).toDouble() / 60.0 }
    }

    StepMetricCard(
        title = "Time Analysis",
        caption = "This Week",
        value = MetricFormat.minutes(points.sumOf { it.value }),
        points = points,
        accent = palette.colorAt(5),
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun AttemptsAnalysisCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = rememberPalette()
    val points = remember(solves) {
        weeklyBarPoints(solves) { maxOf(it.submission.numberOfTries ?: 1, 1).toDouble() }
    }

    StepMetricCard(
        title = "Attempts Analysis",
        caption = "This Week",
        value = MetricFormat.count(points.sumOf { it.value }),
        points = points,
        accent = palette.colorAt(7),
        modifier = modifier,
        onClick = onClick
    )
}

// MARK: - Shared shell

@Composable
fun StepMetricCard(
    title: String,
    caption: String,
    value: String,
    points: List<MetricBarPoint>,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val maxValue = maxOf(points.maxOfOrNull { it.value } ?: 0.0, 1.0)

    /**
     * A half-width card fits about seven letters, so short series get a label per bar and long ones
     * get every fourth. This is the same rule the detail screen uses for its axis.
     */
    val axisIndices = if (points.size > 8) points.indices.filter { it % 4 == 0 } else points.indices

    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = 0.45f))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoadCardBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = title,
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column {
            Text(
                text = caption,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = value,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val xAxisHeight = 14.dp.toPx()
            val plotHeight = (size.height - xAxisHeight).coerceAtLeast(1f)
            val slot = size.width / points.size.toFloat()
            val barWidth = (slot * 0.62f).coerceAtLeast(1.dp.toPx())

            points.forEach { point ->
                val barHeight = ((point.value / maxValue).toFloat() * plotHeight)
                    .coerceIn(0f, plotHeight)
                if (barHeight <= 0f) return@forEach

                drawRoundRect(
                    color = accent,
                    topLeft = Offset(slot * point.index + (slot - barWidth) / 2f, plotHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }

            axisIndices.forEach { index ->
                val point = points.getOrNull(index) ?: return@forEach
                val layout = textMeasurer.measure(point.label, axisStyle)
                val centerX = slot * (index + 0.5f)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = centerX - layout.size.width / 2f,
                        y = plotHeight + 3.dp.toPx()
                    )
                )
            }
        }
    }
}
