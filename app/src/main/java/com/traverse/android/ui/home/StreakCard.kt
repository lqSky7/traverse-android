package com.traverse.android.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The streak hero at the top of the feed. Mirrors iOS `StreakCard`.
 *
 * This used to be a half-width tile sharing its row with the revision score card, and its
 * contents were laid out to suit that: a small number pinned to the bottom-left, a "BEST"
 * figure pinned to the top-right, everything else empty. It is full width now, so the
 * layout is centred and the type scaled up to fill the space — the number is the point of
 * the card and it was set at 40sp in a 110dp-tall box.
 *
 * Display-only: on iOS the streak card has no button, no navigation and no tap gesture —
 * tapping it does nothing. The rainbow "lighting sun" dispersion is supplied by
 * [LightingSunBackground], which reads the container height, so the card sizes itself from
 * its content rather than a fixed height.
 */
@Composable
fun StreakCard(
    streak: Int,
    maxStreak: Int? = null,
    modifier: Modifier = Modifier
) {
    val daysText = if (streak == 1) "DAY" else "DAYS"

    // The stored best can lag behind a live streak that has already passed it, so show
    // whichever is larger rather than telling the user their best is lower than the number
    // directly above it.
    val maxStreakDisplay = maxOf(streak, maxStreak ?: 0)
    val bestText = "BEST $maxStreakDisplay ${if (maxStreakDisplay == 1) "DAY" else "DAYS"}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        LightingSunBackground(
            streak = streak,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (streak == 0) {
                    Icons.Outlined.LocalFireDepartment
                } else {
                    Icons.Filled.LocalFireDepartment
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$streak",
                fontSize = 68.sp,
                lineHeight = 74.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = daysText,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            if (maxStreakDisplay > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = bestText,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
