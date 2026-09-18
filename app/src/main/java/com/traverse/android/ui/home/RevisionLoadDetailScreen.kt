package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.LoadScope
import com.traverse.android.data.RevisionLoadBreakdown
import com.traverse.android.data.RevisionLoadSnapshot
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.format.DateTimeFormatter
import java.util.Locale

private val axisLabelFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US)

/**
 * The pushed screen behind the home feed's Revision Load card, laid out like Apple Fitness'
 * Training Load detail screen: scope chips across the top, the band word in colour, the 7-day vs
 * baseline comparison, a paragraph of plain language, then a daily trend with the baseline drawn
 * through it. 1:1 port of the iOS `RevisionLoadDetailView`.
 *
 * Colours come from `ColorPaletteManager`, not from a hard-coded Apple ramp — a card that ignores
 * the user's chosen palette looks pasted onto the feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionLoadDetailScreen(
    breakdown: RevisionLoadBreakdown,
    revisionScore: Int?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    var scope by remember { mutableStateOf(LoadScope.ALL) }
    var showExplanation by remember { mutableStateOf(false) }

    val snapshot = breakdown.snapshot(scope)
    val accent = loadColorFor(palette, snapshot.band)

    val subtitle = snapshot.formattedPercentChange
        ?.let { "$it · ${snapshot.comparisonLabel}" }
        ?: snapshot.comparisonLabel

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revision Load",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExplanation = true }) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About revision load",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ScopeChips(
                breakdown = breakdown,
                selected = scope,
                onSelect = { scope = it }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(LoadCardBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = snapshot.band.label,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle.uppercase(),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = snapshot.band.explanation,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.White.copy(alpha = 0.62f)
                )

                LoadTrendChart(snapshot = snapshot, modifier = Modifier.fillMaxWidth())

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FooterRow(
                        title = "Revision Score",
                        value = revisionScore?.toString()
                    )
                    FooterRow(
                        title = snapshot.baselineLabel,
                        value = if (snapshot.hasData) {
                            String.format(Locale.US, "%.1f", snapshot.baselineDailyAverage)
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    if (showExplanation) {
        RevisionLoadExplanationSheet(onDismiss = { showExplanation = false })
    }
}

// MARK: - Scope chips

/**
 * The workout-type chips Apple puts above its training-load status.
 *
 * Scope changes are applied directly rather than `.id`-swapped, so the chart keeps its identity
 * and simply redraws. Each scope computes its own baseline window, which means two scopes can
 * legitimately have different series lengths (a difficulty with three days of history grades as
 * "No Data" instead of borrowing the overall window and reporting a verdict it cannot support) —
 * so a morph between them is not always possible and a hard redraw is the honest behaviour.
 */
@Composable
private fun ScopeChips(
    breakdown: RevisionLoadBreakdown,
    selected: LoadScope,
    onSelect: (LoadScope) -> Unit
) {
    val palette = rememberPalette()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LoadScope.entries.forEach { candidate ->
            val candidateSnapshot = breakdown.snapshot(candidate)
            val isSelected = candidate == selected

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isSelected) 0.20f else 0.08f))
                    .clickable { onSelect(candidate) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = candidate.title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White
                )

                candidateSnapshot.formattedPercentChange?.let { percent ->
                    Text(
                        text = percent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = loadColorFor(palette, candidateSnapshot.band)
                    )
                }
            }
        }
    }
}

// MARK: - Trend chart

/**
 * Daily load over the baseline window, with the baseline drawn through it as a dashed rule and
 * every point tinted by the band it falls into.
 *
 * Hand-rolled on a `Canvas` because the app's other charts are, and because the per-point tint is
 * a one-line change here versus fighting a chart library's series-level styling.
 */
@Composable
private fun LoadTrendChart(
    snapshot: RevisionLoadSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))

    val series = snapshot.series
    val upperBound = maxOf(series.maxOfOrNull { it.value } ?: 0.0, 1.0) * 1.25

    Canvas(
        modifier = modifier.height(190.dp)
    ) {
        if (series.isEmpty()) return@Canvas

        val xAxisHeight = 18.dp.toPx()
        val plotHeight = (size.height - xAxisHeight).coerceAtLeast(1f)
        val horizontalPadding = 4.dp.toPx()
        val plotWidth = (size.width - 2 * horizontalPadding).coerceAtLeast(1f)

        fun xAt(index: Int): Float =
            if (series.size == 1) size.width / 2f
            else horizontalPadding + (index.toFloat() / (series.size - 1)) * plotWidth

        fun yAt(value: Double): Float =
            plotHeight - ((value / upperBound).toFloat() * plotHeight).coerceIn(0f, plotHeight)

        // Baseline rule. Drawn first so the points sit on top of it.
        if (snapshot.hasData && snapshot.baselineDailyAverage > 0.0) {
            val baselineY = yAt(snapshot.baselineDailyAverage)
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                )
            )
        }

        // The trend itself.
        val path = Path()
        series.forEachIndexed { index, point ->
            val x = xAt(index)
            val y = yAt(point.value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.28f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Points, tinted per band. The newest point is drawn larger so the eye lands on where the
        // user is now rather than on the oldest bar in the window.
        series.forEachIndexed { index, point ->
            val isLatest = index == series.lastIndex
            drawCircle(
                color = loadColorFor(palette, snapshot.bandFor(point.value)),
                radius = if (isLatest) 6.dp.toPx() else 4.dp.toPx(),
                center = Offset(xAt(index), yAt(point.value))
            )
        }

        // X axis labels, every seventh day — matching the iOS AxisMarks stride.
        series.forEachIndexed { index, point ->
            if (index % 7 != 0) return@forEachIndexed
            val layout = textMeasurer.measure(axisLabelFormatter.format(point.day), axisStyle)
            val labelX = (xAt(index) - layout.size.width / 2f)
                .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(labelX, plotHeight + 4.dp.toPx())
            )
        }

        // Keeps a flat zero series from looking like a broken axis: a faint floor line.
        drawRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(0f, plotHeight),
            size = Size(size.width, 1.dp.toPx())
        )
    }
}

// MARK: - Footer

@Composable
private fun FooterRow(title: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value ?: "No Data",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (value == null) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.85f)
        )
    }
}
