package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.data.ActivityMetrics
import com.traverse.android.data.RingProgress
import com.traverse.android.data.Solve
import com.traverse.android.ui.components.ActivityRings
import com.traverse.android.ui.components.ChromaText
import com.traverse.android.ui.components.SweepGate
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


/** iOS `Color(red: 0.31, green: 0.76, blue: 0.97)` — frozen days. */
private val IceBlue = Color(0xFF4FC3F7)

/** One day in the streak card's week strip. */
private enum class StreakDayState {
    /** Banked — the day has activity against it. */
    SOLVED,

    /** No activity. Shown as a bare track, not as a failure. */
    MISSED,

    /** Held by a streak freeze. Ice blue, matching the heatmap. */
    FROZEN,

    /** Today, already banked. */
    TODAY,

    /** Today, still open. The only day the user can still act on, so it gets its own state. */
    TODAY_PENDING,
}

private data class StreakDay(val date: LocalDate, val state: StreakDayState)

private val weekdayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())

/**
 * The streak hero at the top of the feed. Mirrors iOS `StreakCard`.
 *
 * It has been through four layouts. Originally a half-width tile sharing its row with the revision
 * score card. Then full width with the type scaled up and centred — which fixed the size but left
 * a band of dead space down each side. Then full width with the streak block left and the activity
 * rings right, over a rainbow light-dispersion shader. (That shader's CPU port,
 * `LightingSunBackground.kt`, has since been deleted.)
 *
 * The shader is gone. It was the only card in the feed that wasn't a `#1A1A1A` tile, the only one
 * whose colour ignored the user's palette, and it sprang on appear and on every streak change in a
 * tab that never unloads — with a 246-line CPU port of the Metal shader recomputing a ~14,000-pixel
 * bitmap per frame of that spring. Two attempts to replace it with something quieter both failed on
 * looks: a flat band gradient at low alpha over the tile reads as mud (blending a saturated colour
 * into a dark neutral gives a brownish-grey mid-tone, and the tile stops looking like the standard
 * surface at all), and corner-anchored glows read as a band bleeding in from two corners. The base
 * is now flat, matching every other card.
 *
 * The colour moved into the week strip instead, where it has a job — see [WeekStrip].
 *
 * The card is interactive — tapping opens the ring goal customization sheet.
 */
@Composable
fun StreakCard(
    streak: Int,
    maxStreak: Int? = null,
    rings: RingProgress? = null,
    /** Solve history for the week strip. Empty renders seven open days. */
    solves: List<Solve> = emptyList(),
    /** "YYYY-MM-DD" keys, matching `HomeUiState.frozenDates`. */
    frozenDates: List<String> = emptyList(),
    /**
     * Once-per-visit latch for the number's sweep. Supplied by the screen rather than owned here,
     * because this card sits in a `LazyColumn` and is disposed once it scrolls out of the
     * keep-alive window — a gate held here would reset on scroll and the sweep would replay every
     * time the user scrolled back to the top.
     */
    sweepGate: SweepGate? = null,
    onRingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ringProgress = rings ?: RingProgress.empty()
    val palette = rememberPalette()
    val solveColor = palette.colorAt(0)
    val revisionColor = palette.colorAt(1)

    val daysText = if (streak == 1) "DAY" else "DAYS"

    // The stored best can lag behind a live streak that has already passed it, so show whichever
    // is larger rather than telling the user their best is lower than the number directly above it.
    val maxStreakDisplay = maxOf(streak, maxStreak ?: 0)

    val weekDays = rememberWeekDays(solves = solves, frozenDates = frozenDates)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            // Flat `#1A1A1A`, the same surface as every other card in the feed. Nothing is laid
            // over it — see the note on this function.
            .background(CardBackground)
            .clickable(onClick = onRingsClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("$streak day streak, best $maxStreakDisplay, ")
                    append("${ringProgress.solves} of ${ringProgress.solveGoal} solved, ")
                    append("${ringProgress.revisions} of ${ringProgress.revisionGoal} reviewed")
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Streak",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))

            // The same chevron-in-circle every other tappable card on the feed carries. This card
            // was already a tap target and had no affordance saying so.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActivityRings(
                solveFraction = ringProgress.solveFraction,
                revisionFraction = ringProgress.revisionFraction,
                solveColor = solveColor,
                revisionColor = revisionColor,
                diameter = 84.dp,
                strokeWidth = 9.dp
            )

            StreakBlock(
                streak = streak,
                daysText = daysText,
                maxStreakDisplay = maxStreakDisplay,
                sweepGate = sweepGate
            )

            Spacer(modifier = Modifier.weight(1f))

            RingLegend(
                solveColor = solveColor,
                revisionColor = revisionColor,
                solveValue = "${ringProgress.solves}/${ringProgress.solveGoal}",
                revisionValue = "${ringProgress.revisions}/${ringProgress.revisionGoal}"
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        WeekStrip(days = weekDays)
    }
}

/**
 * Number, then the two labels underneath, at the sizes this card has always used: a tracked-out
 * `DAYS` and a dimmer `BEST`. Kept as a two-line stack rather than a single `DAYS · BEST 21` line —
 * the single line had to drop both to 11.5sp to fit, which lost the hierarchy between "how long" and
 * "how long ever".
 */
@Composable
private fun StreakBlock(
    streak: Int,
    daysText: String,
    maxStreakDisplay: Int,
    sweepGate: SweepGate?
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        // Sweeps out of the band and lands on white.
        //
        // White rather than a palette slot: the sweep has to resolve to *something*, and under the
        // Traverse palette slot 2 is amber, which reads as yellow sitting next to the strip. White is
        // also what this number was before it was palette-tinted, and it is the highest-contrast
        // thing on the card.
        //
        // No `key(streak)` here. Re-keying on the streak is what made the number re-sweep while the
        // user was still looking at the card; `sweepGate` is what decides when it plays instead —
        // once per visit to the feed, and again on the way back from another screen.
        ChromaText(
            text = "$streak",
            style = TextStyle(
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                // Tabular figures keep the number from jittering horizontally as it changes,
                // which is visible at this size.
                fontFeatureSettings = "tnum",
                color = Color.White
            ),
            restingColor = Color.White,
            delayMillis = 150,
            durationMillis = 1250,
            gate = sweepGate,
            sweepKey = "streak-number"
        )

        Text(
            text = daysText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            color = Color.White.copy(alpha = 0.85f)
        )

        if (maxStreakDisplay > 0) {
            Text(
                text = "BEST $maxStreakDisplay",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * The counts, with a dot in each ring's colour. Without this the rings say "not yet" but not
 * "one more", and the difference between 0/1 and 4/5 is exactly what the user needs at the end of
 * a day.
 */
@Composable
private fun RingLegend(
    solveColor: Color,
    revisionColor: Color,
    solveValue: String,
    revisionValue: String
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LegendRow(color = solveColor, value = solveValue)
        LegendRow(color = revisionColor, value = revisionValue)
    }
}

@Composable
private fun LegendRow(color: Color, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * Seven day dots under the streak card's divider.
 *
 * **This is the card's only chroma.** Each day takes the band colour at its position, so the strip
 * walks pink → crimson → amber → ice → cobalt from left to right and a longer streak lights more of
 * it. That is the "more streak, more colour" idea the shader used to carry, moved somewhere it does
 * not fight the surface.
 *
 * Deliberately **not** palette-driven. A Monochrome user would get seven grey dots and the brand
 * would vanish for them; the strip is one of the two places in the app pinned to the band regardless
 * of the selected palette (the other is [ChromaText]). Small elements carry full saturation happily
 * — 17dp dots read as brand, a 360dp tile does not.
 */
@Composable
private fun WeekStrip(days: List<StreakDay>, modifier: Modifier = Modifier) {
    val palette = rememberPalette()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = days.joinToString(prefix = "Last seven days: ") { day ->
                    val label = weekdayFormatter.format(day.date)
                    when (day.state) {
                        StreakDayState.SOLVED -> "$label solved"
                        StreakDayState.MISSED -> "$label no activity"
                        StreakDayState.FROZEN -> "$label frozen"
                        StreakDayState.TODAY -> "$label today, solved"
                        StreakDayState.TODAY_PENDING -> "$label today, still open"
                    }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        days.forEachIndexed { index, day ->
            // The dots take their colours from the active palette, matching the text sweep. Both
            // used to be pinned to a hardcoded band, which made the app's two most brand-forward
            // elements its two most palette-blind ones. The default palette (Traverse) is the chroma
            // band, so the out-of-the-box look is unchanged.
            val tint = palette.colorAt(index, of = days.size)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Track, so an untouched day still reads as a day.
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .background(Color.White.copy(alpha = 0.11f), CircleShape)
                    )

                    when (day.state) {
                        StreakDayState.SOLVED, StreakDayState.TODAY ->
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .background(tint, CircleShape)
                            )

                        StreakDayState.FROZEN ->
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .border(2.dp, IceBlue, CircleShape)
                            )

                        StreakDayState.TODAY_PENDING ->
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .border(2.dp, tint.copy(alpha = 0.55f), CircleShape)
                            )

                        StreakDayState.MISSED -> Unit
                    }

                    // Today always carries a ring, so the strip has a fixed "you are here" even on a
                    // day with nothing banked yet.
                    if (day.state == StreakDayState.TODAY || day.state == StreakDayState.TODAY_PENDING) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .border(2.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                    }
                }

                Text(
                    text = weekdayFormatter.format(day.date),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * The last seven days, oldest first, with today rightmost.
 *
 * Reads the same [ActivityMetrics.daily] helper the metric cards use, so the strip and the charts
 * can never disagree about which day a solve belongs to. No new endpoint: `daily` already zero-fills
 * gaps, so the strip keeps an even rhythm on a rest day instead of collapsing.
 *
 * Keyed on both inputs so the seven-bucket walk runs once per change rather than on every
 * recomposition — `daily` sorts and zero-fills a map, which is cheap but not free, and this card
 * recomposes whenever a ring animates.
 */
@Composable
private fun rememberWeekDays(solves: List<Solve>, frozenDates: List<String>): List<StreakDay> =
    remember(solves, frozenDates) {
        val frozen = frozenDates.toSet()
        val buckets = ActivityMetrics.daily(solves = solves, days = 7) { _ -> 1.0 }
        val lastIndex = buckets.size - 1

        buckets.mapIndexed { index, bucket ->
            val state = when {
                // Today is the only day the user can still act on, so it never reads as a gap — it
                // reads as open.
                index == lastIndex ->
                    if (bucket.value > 0) StreakDayState.TODAY else StreakDayState.TODAY_PENDING
                // LocalDate.toString() is ISO-8601, which is the "YYYY-MM-DD" shape frozenDates uses.
                frozen.contains(bucket.date.toString()) -> StreakDayState.FROZEN
                bucket.value > 0 -> StreakDayState.SOLVED
                else -> StreakDayState.MISSED
            }
            StreakDay(date = bucket.date, state = state)
        }
    }
