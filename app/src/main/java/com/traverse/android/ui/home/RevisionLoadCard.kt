package com.traverse.android.ui.home

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RevisionLoadBand
import com.traverse.android.data.RevisionLoadBreakdown
import com.traverse.android.data.RevisionLoadSnapshot
import com.traverse.android.ui.theme.ColorPalette
import com.traverse.android.ui.theme.rememberPalette

internal val LoadCardBackground = Color(0xFF1A1A1A)

/**
 * Palette-aware colour for a revision-load band, so the card follows the user's chosen palette the
 * way every other card on the feed does. Mirrors iOS
 * `ColorPaletteManager.loadColor(for:)`.
 *
 * `No Data` deliberately steps outside the palette: it is the absence of a reading, not a reading,
 * and tinting it with a palette slot would make it look like a verdict.
 */
fun loadColorFor(palette: ColorPalette, band: RevisionLoadBand): Color =
    if (band == RevisionLoadBand.NO_DATA) Color.White.copy(alpha = 0.55f)
    else palette.colorAt(band.paletteIndex)

/**
 * Home-feed summary of revision load, modelled on Apple Fitness' "Training Load" tile: a small
 * gauge on the left, the band word in colour on the right, the 7-day vs 28-day comparison
 * underneath, and a second metric sharing the card's footer. Mirrors iOS `RevisionLoadCard`.
 *
 * This replaces the old `RevisionScoreCard` (a bare number with a shader orb bleeding out of the
 * corner). The score itself did not go away — it moved to the footer row, where it reads as one
 * fact among several instead of the only fact on the card.
 */
@Composable
fun RevisionLoadCard(
    breakdown: RevisionLoadBreakdown?,
    revisionScore: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val palette = rememberPalette()
    val snapshot = breakdown?.overall ?: RevisionLoadSnapshot.EMPTY
    val accent = loadColorFor(palette, snapshot.band)

    val subtitle = snapshot.formattedPercentChange
        ?.let { "$it · ${snapshot.comparisonLabel}" }
        ?: snapshot.comparisonLabel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LoadCardBackground)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Revision Load",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RevisionLoadGauge(snapshot = snapshot, accent = accent)

            Spacer(modifier = Modifier.width(16.dp))

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
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Revision Score",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = revisionScore?.toString() ?: "No Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (revisionScore == null) Color.White.copy(alpha = 0.45f)
                else Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * The stacked-bar gauge from the reference card: two neutral caps around a coloured block, with a
 * white progress pill underneath showing where the 7-day average sits against baseline.
 *
 * Flat by design — on iOS the block used to carry a coloured drop shadow and the dot a blurred
 * halo, which on a black background read as a glow bleeding into the card. The band colour is
 * already the signal; it does not need to be emitted.
 *
 * Everything here is static. There is no animation, and that is deliberate: the card's job is to
 * communicate a number, and this codebase has a cautionary tale about what an always-on animation
 * in a tab that never unloads does to battery life (see the deleted `ThinkingOrbScoreView`).
 */
@Composable
fun RevisionLoadGauge(
    snapshot: RevisionLoadSnapshot,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val fillFraction: Float = run {
        if (!snapshot.hasData || snapshot.baselineDailyAverage <= 0.0) return@run 0.06f
        val ratio = snapshot.recentDailyAverage / snapshot.baselineDailyAverage
        // Half of baseline sits at the low end, twice baseline fills the pill.
        (ratio / 2.0).toFloat().coerceIn(0.06f, 1f)
    }

    Column(
        modifier = modifier.width(84.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GaugeCap()

        Box(
            modifier = Modifier
                .size(width = 84.dp, height = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = 0.90f), accent.copy(alpha = 0.55f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.85f))
                )
            }
        }

        GaugeCap()

        Box(
            modifier = Modifier.size(width = 74.dp, height = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
            )

            Box(
                modifier = Modifier
                    // A floor of 5.dp so a near-zero reading still paints a visible sliver rather
                    // than a pill that looks empty for a different reason than it is.
                    .width(maxOf(74f * fillFraction, 5f).dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun GaugeCap() {
    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 16.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.14f))
    )
}
