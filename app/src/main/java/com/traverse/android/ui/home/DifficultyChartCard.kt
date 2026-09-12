package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.ProblemsByDifficulty
import com.traverse.android.ui.theme.rememberPalette

private val CardBackground = Color(0xFF1A1A1A)

/**
 * 1:1 port of the iOS `DifficultyChartCard`: hero total plus three horizontal progress
 * bars (Easy / Medium / Hard). No ring chart, no legend dots.
 *
 * Bar colours come from the active palette: Easy = `color(at: 0)`, Medium = `color(at: 1)`,
 * Hard = `color(at: 2)`, exactly as on iOS.
 */
@Composable
fun DifficultyChartCard(
    difficulty: ProblemsByDifficulty,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val total = difficulty.easy + difficulty.medium + difficulty.hard
    val maxCount = maxOf(difficulty.easy, difficulty.medium, difficulty.hard, 1)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        // Fills the height handed down by the caller so this card lines up exactly with the
        // Activity heatmap card beside it; the trailing spacer pushes the bars to the bottom.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Difficulty",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$total",
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Total Solved",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DifficultyProgressRow("Easy", difficulty.easy, maxCount, palette.colorAt(0))
                DifficultyProgressRow("Medium", difficulty.medium, maxCount, palette.colorAt(1))
                DifficultyProgressRow("Hard", difficulty.hard, maxCount, palette.colorAt(2))
            }
        }
    }
}

@Composable
private fun DifficultyProgressRow(
    label: String,
    count: Int,
    maxCount: Int,
    color: Color
) {
    val progress = if (maxCount > 0) count.toFloat() / maxCount.toFloat() else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.width(50.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Deliberately a plain Box rather than a BoxWithConstraints: the parent Row measures with
        // `IntrinsicSize.Min`, which subcompose-based layouts cannot answer.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            )

            if (count > 0) {
                Box(
                    modifier = Modifier
                        // widthIn first so even a count of 1 still paints a 12.dp sliver,
                        // preserving the previous minimum-width behaviour.
                        .widthIn(min = 12.dp)
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f)))
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = color
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}
