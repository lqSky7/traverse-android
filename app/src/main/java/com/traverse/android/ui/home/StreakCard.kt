package com.traverse.android.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RingProgress
import com.traverse.android.ui.components.ActivityRings
import com.traverse.android.ui.theme.rememberPalette

/**
 * The streak hero at the top of the feed. Mirrors iOS `StreakCard`.
 *
 * Left-aligned Row layout: streak block on the left, ring legend, and activity rings on the right.
 * The card is interactive — tapping opens the ring goal customization sheet.
 * The rainbow "lighting sun" dispersion is supplied by [LightingSunBackground].
 */
@Composable
fun StreakCard(
    streak: Int,
    maxStreak: Int? = null,
    rings: RingProgress? = null,
    onRingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ringProgress = rings ?: RingProgress.empty()
    val daysText = if (streak == 1) "DAY" else "DAYS"

    // The stored best can lag behind a live streak that has already passed it, so show
    // whichever is larger rather than telling the user their best is lower than the number
    // directly above it.
    val maxStreakDisplay = maxOf(streak, maxStreak ?: 0)

    val palette = rememberPalette()
    val solveColor = palette.colorAt(0)
    val revisionColor = palette.colorAt(1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onRingsClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$streak day streak, best $maxStreakDisplay, " +
                    "${ringProgress.solves} of ${ringProgress.solveGoal} solved, " +
                    "${ringProgress.revisions} of ${ringProgress.revisionGoal} reviewed"
            }
    ) {
        LightingSunBackground(
            streak = streak,
            modifier = Modifier.matchParentSize()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Left: Streak block
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "$streak",
                    fontSize = 54.sp,
                    lineHeight = 58.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
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

            Spacer(modifier = Modifier.weight(1f))

            // Right: Ring legend
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(solveColor)
                    )
                    Text(
                        text = "${ringProgress.solves}/${ringProgress.solveGoal}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(revisionColor)
                    )
                    Text(
                        text = "${ringProgress.revisions}/${ringProgress.revisionGoal}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Rightmost: Rings
            ActivityRings(
                solveFraction = ringProgress.solveFraction,
                revisionFraction = ringProgress.revisionFraction,
                solveColor = solveColor,
                revisionColor = revisionColor,
                diameter = 76.dp,
                strokeWidth = 8.dp,
                ringGap = 3.dp
            )
        }
    }
}
