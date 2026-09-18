package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.AchievementStatsData
import com.traverse.android.data.MedalCatalog
import com.traverse.android.ui.components.MedalBadge

private val CardBackground = Color(0xFF1A1A1A)

/**
 * The Awards card on the home feed.
 *
 * Apple leads with the badge you earned most recently rather than a raw count, so the card
 * shows the newest award's medal and name. The whole card is a navigation target into the
 * Awards shelf.
 */
@Composable
fun AchievementStatsCard(
    stats: AchievementStatsData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val latest = stats.latestAward

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Awards",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Box(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (latest != null) {
                MedalBadge(
                    medal = latest.medalAsset,
                    unlocked = latest.unlocked,
                    size = 92.dp,
                    interactive = false
                )
            } else {
                // Nothing earned yet — keep the card's silhouette rather than collapsing it.
                MedalBadge(
                    medal = MedalCatalog.fallback("first_solve"),
                    unlocked = false,
                    size = 92.dp,
                    interactive = false
                )
            }
        }

        Text(
            text = latest?.name ?: "Solve a problem to earn your first award",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (latest != null) Color.White else Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
