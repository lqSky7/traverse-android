package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette


/**
 * 1:1 port of the iOS `RecentSolvesCard`: header with the solve count and a chevron on the right,
 * then the five most recent solves rendered as expandable [SolveRow]s.
 *
 * Header anatomy deliberately matches `MistakeTagsCard`, which sits directly below it on the same
 * screen — the count belongs in the header row, not in a band of its own. Palette mapping from iOS:
 * the header icon and the count use `colorAt(0)`; the chevron is secondary.
 */
@Composable
fun RecentSolvesCard(
    solves: List<Solve>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val accentColor = palette.colorAt(0)

    val rows = solves.take(5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
    ) {
        // Header — the same anatomy as MistakeTagsCard next to it on this screen: icon + title on
        // the left, the headline figure and a chevron on the right, one divider, then content.
        //
        // This card used to break that: the count sat in its own 40sp band *below* a second divider,
        // and "View All" was spelled out as text. So the top third read as three stacked strips —
        // title, lone number, rows — with uneven padding between them, and the two cards disagreed
        // about where a number belongs and what a navigation affordance looks like. Same screen,
        // same header.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recent Solves",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    // The label is small and the row is not the target, so the hit area is grown
                    // explicitly rather than left to the glyph bounds.
                    .semantics { contentDescription = "View all ${solves.size} solves" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${solves.size}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .padding(bottom = 16.dp)
        ) {
            rows.forEachIndexed { index, solve ->
                SolveRow(solve = solve)

                if (index < minOf(4, rows.size - 1)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
