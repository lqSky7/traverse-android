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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Revision
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val CardBackground = Color(0xFF1A1A1A)

private data class DayActivity(
    val label: String,
    val solves: Int,
    val revisions: Int
)

/**
 * 1:1 port of the iOS `ProductivityInsightsCard` ("Weekly Activity"): a grouped bar chart
 * comparing solves against completed revisions for the last 7 days, with a y-axis grid
 * and a two-item legend.
 */
@Composable
fun ProductivityInsightsCard(
    solves: List<Solve>,
    completedRevisions: List<Revision>,
    modifier: Modifier = Modifier
) {
    val days = remember(solves, completedRevisions) {
        buildWeeklyActivity(solves, completedRevisions)
    }

    // iOS: solves -> color(at: 0), revisions -> color(at: 1)
    val palette = rememberPalette()
    val solvesColor = palette.colorAt(0)
    val revisionsColor = palette.colorAt(1)

    val maxValue = days.maxOfOrNull { maxOf(it.solves, it.revisions) } ?: 0
    val ticks = remember(maxValue) { axisTicks(maxValue) }
    val axisMax = (ticks.lastOrNull() ?: 1).coerceAtLeast(1).toFloat()

    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Weekly Activity",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f)
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val yGutter = 26.dp.toPx()
            val xAxisHeight = 16.dp.toPx()
            val plotLeft = yGutter
            val plotRight = size.width
            val plotBottom = size.height - xAxisHeight
            val plotHeight = (plotBottom).coerceAtLeast(1f)
            val gridColor = Color.White.copy(alpha = 0.18f)
            val gridStroke = 0.5.dp.toPx()

            // Y axis grid lines + value labels
            ticks.forEach { tick ->
                val y = plotBottom - (tick / axisMax) * plotHeight
                drawLine(
                    color = gridColor,
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = gridStroke
                )
                val layout = textMeasurer.measure("$tick", axisStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = plotLeft - 6.dp.toPx() - layout.size.width,
                        y = y - layout.size.height / 2f
                    )
                )
            }

            // Grouped bars + x axis labels
            val slot = (plotRight - plotLeft) / days.size.coerceAtLeast(1)
            val barWidth = slot * 0.28f
            val barGap = 2.dp.toPx()
            val corner = CornerRadius(2.dp.toPx())

            days.forEachIndexed { index, day ->
                val centerX = plotLeft + slot * (index + 0.5f)

                drawActivityBar(
                    left = centerX - barGap / 2f - barWidth,
                    value = day.solves,
                    axisMax = axisMax,
                    plotBottom = plotBottom,
                    plotHeight = plotHeight,
                    width = barWidth,
                    color = solvesColor,
                    corner = corner
                )
                drawActivityBar(
                    left = centerX + barGap / 2f,
                    value = day.revisions,
                    axisMax = axisMax,
                    plotBottom = plotBottom,
                    plotHeight = plotHeight,
                    width = barWidth,
                    color = revisionsColor,
                    corner = corner
                )

                val layout = textMeasurer.measure(day.label, axisStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = centerX - layout.size.width / 2f,
                        y = plotBottom + 4.dp.toPx()
                    )
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = solvesColor, label = "Solves")
            LegendDot(color = revisionsColor, label = "Revisions")
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawActivityBar(
    left: Float,
    value: Int,
    axisMax: Float,
    plotBottom: Float,
    plotHeight: Float,
    width: Float,
    color: Color,
    corner: CornerRadius
) {
    if (value <= 0) return
    val barHeight = (value / axisMax) * plotHeight
    drawRoundRect(
        color = color,
        topLeft = Offset(left, plotBottom - barHeight),
        size = Size(width, barHeight),
        cornerRadius = corner
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

/** Buckets solves and completed revisions into the last 7 calendar days. */
private fun buildWeeklyActivity(
    solves: List<Solve>,
    completedRevisions: List<Revision>
): List<DayActivity> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEEE")

    val solveCounts = HashMap<LocalDate, Int>()
    solves.forEach { solve ->
        parseDate(solve.solvedAt)?.let { date ->
            solveCounts[date] = (solveCounts[date] ?: 0) + 1
        }
    }

    val revisionCounts = HashMap<LocalDate, Int>()
    completedRevisions.forEach { revision ->
        revision.completedAt?.let { completedAt ->
            parseDate(completedAt)?.let { date ->
                revisionCounts[date] = (revisionCounts[date] ?: 0) + 1
            }
        }
    }

    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayActivity(
            label = date.format(formatter),
            solves = solveCounts[date] ?: 0,
            revisions = revisionCounts[date] ?: 0
        )
    }
}

private fun parseDate(raw: String): LocalDate? = try {
    LocalDate.parse(raw.take(10))
} catch (_: Exception) {
    null
}

/** Produces "nice" axis tick values, roughly mirroring Swift Charts' automatic marks. */
private fun axisTicks(maxValue: Int): List<Int> {
    val m = maxValue.coerceAtLeast(1)
    if (m <= 3) return (0..m).toList()

    val rough = m / 2.0
    val magnitude = 10.0.pow(floor(log10(rough)))
    val normalized = rough / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = (nice * magnitude).coerceAtLeast(1.0)
    val top = ceil(m / step) * step

    val result = ArrayList<Int>()
    var value = 0.0
    while (value <= top + 1e-6) {
        result.add(value.roundToInt())
        value += step
    }
    return result
}
