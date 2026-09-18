package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.theme.rememberPalette

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `MistakeTagsAnalysisCard`: distinct tag count in the header, a hero
 * "Total Mistakes" figure and the top 3 mistake tags as progress bars. Tapping the card
 * opens the full mistake analysis screen.
 *
 * Palette mapping from iOS: header icon and count use `color(at: 5)`; each tag bar uses
 * `color(at: index % 10)`; the empty-state checkmark uses `color(at: 0)`.
 */
@Composable
fun MistakeTagsAnalysisCard(
    solves: List<Solve>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val headerColor = palette.colorAt(5)

    val tagCounts = mutableMapOf<String, Int>()
    solves.forEach { solve ->
        val tags = solve.mistakeTags ?: solve.submission.mistakeTags ?: emptyList()
        val seen = mutableSetOf<String>()
        tags.forEach { tag ->
            if (seen.add(tag)) {
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
    }

    val sortedTags = tagCounts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
    val totalMistakes = sortedTags.sumOf { it.value }
    val maxCount = (sortedTags.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAll),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = headerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mistake Analysis",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${sortedTags.size}",
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor
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

            if (sortedTags.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$totalMistakes",
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Total Mistakes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sortedTags.take(3).forEachIndexed { index, entry ->
                        MistakeTagProgressRow(
                            label = entry.key,
                            count = entry.value,
                            maxCount = maxCount,
                            color = palette.colorAt(index % 10)
                        )
                    }
                }
            } else {
                // A *good* zero state, so it deliberately reads as praise rather than as an error.
                EmptyStateView(
                    icon = Icons.Default.CheckCircle,
                    title = "No mistakes detected",
                    message = "Nothing in your recent submissions was flagged. Keep going.",
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun MistakeTagProgressRow(
    label: String,
    count: Int,
    maxCount: Int,
    color: Color
) {
    val rawProgress = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f
    val progress = if (rawProgress.isFinite()) rawProgress.coerceIn(0f, 1f) else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            val trackWidth = maxWidth
            val fillWidth = if (count > 0) {
                maxOf(trackWidth * progress, 12.dp).coerceAtMost(trackWidth)
            } else {
                0.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f)))
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp)
        )
    }
}
