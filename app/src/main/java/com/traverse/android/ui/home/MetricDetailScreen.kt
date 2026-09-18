package com.traverse.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.ActivityMetrics
import com.traverse.android.data.DifficultyMetric
import com.traverse.android.data.DifficultyWeight
import com.traverse.android.data.Solve
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.ColorPalette
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

// MARK: - Range

enum class MetricRange(val label: String, val periodLabel: String) {
    DAY("D", "Today"),
    WEEK("W", "This Week"),
    MONTH("M", "This Month"),
    YEAR("Y", "This Year")
}

// MARK: - Kind

enum class MetricKind(
    val navigationTitle: String,
    val metricsButtonTitle: String,
    /**
     * Headline for an empty range. A bar chart with nothing in it looks like a rendering failure,
     * so the chart is replaced outright when the range has no data rather than drawn flat.
     */
    val emptyTitle: String
) {
    TIME(
        navigationTitle = "Time Analysis",
        metricsButtonTitle = "View All Time Metrics",
        emptyTitle = "No time recorded"
    ),
    ATTEMPTS(
        navigationTitle = "Attempts Analysis",
        metricsButtonTitle = "View All Attempt Metrics",
        emptyTitle = "No attempts recorded"
    );

    /** The per-solve sample being bucketed. */
    fun value(solve: Solve): Double = when (this) {
        TIME -> (solve.submission.timeTaken ?: 0).toDouble() / 60.0
        ATTEMPTS -> maxOf(solve.submission.numberOfTries ?: 1, 1).toDouble()
    }

    fun format(value: Double): String = when (this) {
        TIME -> MetricFormat.minutes(value)
        ATTEMPTS -> MetricFormat.count(value)
    }

    fun formatAxis(value: Double): String = when (this) {
        TIME -> MetricFormat.minutesShort(value)
        ATTEMPTS -> MetricFormat.count(value)
    }
}

// MARK: - Formatters

private val narrowWeekdayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEEE", Locale.US)
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)
private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
private val yearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy", Locale.US)

/** One bar, plus the axis label it carries (null when this bar is not a labelled tick). */
private data class RangePoint(val value: Double, val axisLabel: String?)

// MARK: - Screen

/**
 * The pushed screen behind both Step-Count style cards, laid out like Apple Fitness' metric detail:
 * large title, D/W/M/Y range picker, a headline total over a bar chart with real grid lines and
 * axis labels, then a button that expands the all-time numbers. 1:1 port of the iOS
 * `MetricDetailView`.
 *
 * Attempts additionally carries the "By Difficulty" breakdown that used to be its own card on the
 * home feed.
 *
 * The range picker is a Material 3 segmented control rather than the iOS Liquid Glass one — the
 * system picker on iOS *is* Liquid Glass on iOS 26, and there is no equivalent here, so this uses
 * the native Android idiom instead of approximating a glass surface.
 */
@Composable
fun MetricDetailScreen(
    kind: MetricKind,
    solves: List<Solve>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val accent = when (kind) {
        MetricKind.TIME -> palette.colorAt(5)
        MetricKind.ATTEMPTS -> palette.colorAt(7)
    }

    // Weekly by default — a single day is mostly empty for anyone who is not solving every day, and
    // an empty chart reads as a broken chart.
    var range by remember { mutableStateOf(MetricRange.WEEK) }
    var showAllMetrics by remember { mutableStateOf(false) }

    val points = remember(solves, range, kind) { buildPoints(kind, solves, range) }
    val rangeTotal = points.sumOf { it.value }

    val headlineValue = when (range) {
        MetricRange.MONTH -> {
            // Divide by the days *elapsed* in the month, not by 30. On the 8th of the month a
            // 30-day divisor reported an average 3.75x lower than the truth, which reads as "you
            // are slacking" on a month that has barely started.
            val elapsed = LocalDate.now().dayOfMonth
            kind.format(rangeTotal / maxOf(elapsed, 1).toDouble())
        }

        else -> kind.format(rangeTotal)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = kind.navigationTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = RingiftFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = floatingBottomBarContentInset()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RangeSelector(selected = range, onSelect = { range = it })

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (range == MetricRange.MONTH) "Daily Average" else "Total",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = headlineValue,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = headlineSubtitle(range),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            if (rangeTotal <= 0.0) {
                EmptyStateView(
                    icon = Icons.Filled.BarChart,
                    title = kind.emptyTitle,
                    message = "Nothing in ${range.periodLabel.lowercase()}. Try a wider range, or " +
                        "solve a problem with the browser extension installed.",
                    compact = true,
                    modifier = Modifier.height(220.dp)
                )
            } else {
                MetricRangeChart(
                    kind = kind,
                    points = points,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            MetricsToggleButton(
                title = if (showAllMetrics) "Hide Metrics" else kind.metricsButtonTitle,
                expanded = showAllMetrics,
                accent = accent,
                onClick = { showAllMetrics = !showAllMetrics }
            )

            // `expandVertically` grows the block downward from where the button already is, so it
            // never covers the chart on the way in — the failure mode the iOS version had with a
            // `.move(edge: .top)` transition.
            AnimatedVisibility(
                visible = showAllMetrics,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                AllTimeMetrics(kind = kind, solves = solves)
            }

            if (kind == MetricKind.ATTEMPTS) {
                DifficultyBreakdown(solves = solves)
            }
        }
    }
}

private fun headlineSubtitle(range: MetricRange): String {
    val today = LocalDate.now()
    return when (range) {
        MetricRange.DAY, MetricRange.WEEK -> range.periodLabel
        MetricRange.MONTH -> today.format(monthYearFormatter).uppercase(Locale.US)
        MetricRange.YEAR -> today.format(yearFormatter)
    }
}

// MARK: - Data

private fun buildPoints(kind: MetricKind, solves: List<Solve>, range: MetricRange): List<RangePoint> =
    when (range) {
        MetricRange.DAY -> {
            val buckets = ActivityMetrics.hourly(solves = solves, value = kind::value)
            buckets.mapIndexed { hour, bucket ->
                RangePoint(
                    value = bucket.value,
                    axisLabel = if (hour % 6 == 0) String.format(Locale.US, "%02d", hour) else null
                )
            }
        }

        MetricRange.WEEK ->
            ActivityMetrics.daily(solves = solves, days = 7, value = kind::value).map { bucket ->
                RangePoint(bucket.value, bucket.date.format(narrowWeekdayFormatter))
            }

        MetricRange.MONTH ->
            ActivityMetrics.daily(solves = solves, days = 30, value = kind::value)
                .mapIndexed { index, bucket ->
                    RangePoint(
                        value = bucket.value,
                        axisLabel = if (index % 7 == 0) bucket.date.dayOfMonth.toString() else null
                    )
                }

        MetricRange.YEAR ->
            ActivityMetrics.monthly(solves = solves, months = 12, value = kind::value).map { bucket ->
                RangePoint(bucket.value, bucket.date.format(monthFormatter))
            }
    }

// MARK: - Range picker

/**
 * A Material 3 segmented control. Four equal segments in a single track with a raised selected
 * segment — the Android counterpart to the iOS system picker, not a copy of it.
 */
@Composable
private fun RangeSelector(selected: MetricRange, onSelect: (MetricRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(2.dp)
    ) {
        MetricRange.entries.forEach { range ->
            val isSelected = range == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .clickable { onSelect(range) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// MARK: - Chart

/**
 * The axis steps worth labelling, ascending. Chosen by hand rather than derived from a power of
 * ten: these values are either minute counts or attempt counts, and a pure 1/2/5 ladder produces
 * labels like "1.7h / 3.3h / 5h" where "2h / 4h / 6h" was available one step away.
 */
private val NICE_STEPS = doubleArrayOf(
    1.0, 2.0, 5.0, 10.0, 15.0, 30.0, 60.0,
    120.0, 180.0, 300.0, 600.0, 900.0, 1800.0, 3600.0
)

/** The smallest step in [NICE_STEPS] that is at least [raw]. */
private fun niceStep(raw: Double): Double =
    NICE_STEPS.firstOrNull { it >= raw } ?: (ceil(raw / 3600.0) * 3600.0)

@Composable
private fun MetricRangeChart(
    kind: MetricKind,
    points: List<RangePoint>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))

    val maxValue = points.maxOfOrNull { it.value } ?: 0.0
    val step = niceStep(maxValue / 3.0)
    // Three steps, so the tallest bar always has headroom above it.
    val yScaleMax = step * 3.0

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val yGutter = 40.dp.toPx()
        val xAxisHeight = 18.dp.toPx()
        val plotWidth = (size.width - yGutter).coerceAtLeast(1f)
        val plotHeight = (size.height - xAxisHeight).coerceAtLeast(1f)
        val slot = plotWidth / points.size.toFloat()
        val barWidth = (slot * 0.62f).coerceAtLeast(1.dp.toPx())

        fun yAt(value: Double): Float =
            plotHeight - ((value / yScaleMax).toFloat() * plotHeight).coerceIn(0f, plotHeight)

        // Horizontal grid lines with their value on the trailing edge, matching the iOS chart's
        // `.automatic(desiredCount: 3)` y-axis.
        for (multiple in 1..3) {
            val value = step * multiple
            val y = yAt(value)

            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(0f, y),
                end = Offset(plotWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                )
            )

            val layout = textMeasurer.measure(kind.formatAxis(value), axisStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = plotWidth + 6.dp.toPx(),
                    y = (y - layout.size.height / 2f).coerceIn(0f, plotHeight)
                )
            )
        }

        // Baseline.
        drawLine(
            color = Color.White.copy(alpha = 0.14f),
            start = Offset(0f, plotHeight),
            end = Offset(plotWidth, plotHeight),
            strokeWidth = 1.dp.toPx()
        )

        points.forEachIndexed { index, point ->
            val barHeight = (plotHeight - yAt(point.value)).coerceAtLeast(0f)
            if (barHeight <= 0f) return@forEachIndexed

            drawRoundRect(
                color = accent,
                topLeft = Offset(slot * index + (slot - barWidth) / 2f, plotHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        points.forEachIndexed { index, point ->
            val label = point.axisLabel ?: return@forEachIndexed
            val layout = textMeasurer.measure(label, axisStyle)
            val centerX = slot * (index + 0.5f)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (centerX - layout.size.width / 2f)
                        .coerceIn(0f, (plotWidth - layout.size.width).coerceAtLeast(0f)),
                    y = plotHeight + 4.dp.toPx()
                )
            )
        }
    }
}

// MARK: - Expand button

@Composable
private fun MetricsToggleButton(
    title: String,
    expanded: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
    }
}

// MARK: - All-time metrics

@Composable
private fun AllTimeMetrics(kind: MetricKind, solves: List<Solve>) {
    val summary = remember(solves, kind) {
        ActivityMetrics.summary(solves) { kind.value(it) }
    }

    val rows = remember(summary, kind) {
        when (kind) {
            MetricKind.TIME -> listOf(
                "Total Time" to MetricFormat.minutes(summary.total),
                "Average per Problem" to MetricFormat.minutes(summary.average),
                "Longest Single Solve" to MetricFormat.minutes(summary.maximum),
                "Problems Timed" to summary.sampleCount.toString()
            )

            MetricKind.ATTEMPTS -> listOf(
                "Total Attempts" to MetricFormat.count(summary.total),
                "Average per Problem" to String.format(Locale.US, "%.1f", summary.average),
                "Most Attempts" to MetricFormat.count(summary.maximum),
                "Problems Solved" to summary.sampleCount.toString()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoadCardBackground)
            .padding(horizontal = 14.dp)
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.first,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = row.second,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            if (index < rows.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
            }
        }
    }
}

// MARK: - Difficulty (moved off the home feed)

/**
 * The "By Difficulty" breakdown that used to be its own card on the home feed. It lives here now
 * because it is the same data the Attempts chart is drawn from, read a second way — an average of
 * 1.4 attempts overall is a very different story if it is 1.0 on easy and 2.6 on hard.
 */
@Composable
private fun DifficultyBreakdown(solves: List<Solve>) {
    val palette = rememberPalette()
    val metrics = remember(solves) { ActivityMetrics.byDifficulty(solves) }
    val maxSolves = maxOf(metrics.maxOfOrNull { it.solves } ?: 0, 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoadCardBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "By Difficulty",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (metrics.isEmpty()) {
            EmptyStateView(
                icon = Icons.Filled.PieChart,
                title = "No attempts recorded yet",
                message = "Once Traverse has a few attempts to read, they are broken down by " +
                    "difficulty here.",
                compact = true
            )
        } else {
            metrics.forEach { metric ->
                DifficultyBreakdownRow(
                    metric = metric,
                    maxSolves = maxSolves,
                    color = difficultyColor(palette, metric.difficulty)
                )
            }
        }
    }
}

@Composable
private fun DifficultyBreakdownRow(
    metric: DifficultyMetric,
    maxSolves: Int,
    color: Color
) {
    val progress = if (maxSolves > 0) metric.solves.toFloat() / maxSolves.toFloat() else 0f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DifficultyWeight.displayName(metric.difficulty),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.width(52.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )

                if (metric.solves > 0) {
                    Box(
                        modifier = Modifier
                            // widthIn first so even a single solve still paints a sliver.
                            .widthIn(min = 12.dp)
                            .fillMaxWidth(progress)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = metric.solves.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp)
            )
        }

        Text(
            text = String.format(Locale.US, "%.1f attempts per problem", metric.averageAttempts),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 62.dp)
        )
    }
}

/** Easy / Medium / Hard map onto palette slots 0 / 1 / 2, exactly as on iOS. */
private fun difficultyColor(palette: ColorPalette, difficulty: String): Color = when (difficulty.lowercase()) {
    "easy" -> palette.colorAt(0)
    "medium" -> palette.colorAt(1)
    "hard" -> palette.colorAt(2)
    else -> Color.Gray
}
