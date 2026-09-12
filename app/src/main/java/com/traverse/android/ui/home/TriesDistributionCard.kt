package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.rememberPalette

private val CardBackground = Color(0xFF1A1A1A)

private data class AttemptPoint(
    val tries: Int,
    val difficulty: String
)

/**
 * 1:1 port of the iOS `TriesDistributionCard` ("Attempts Analysis"): average tries /
 * first-try count summary above a scatter plot where dot size encodes first-try solves
 * and colour encodes difficulty.
 *
 * Palette mapping from iOS: title/average use `color(at: 7)`; the Easy/Medium/Hard
 * difficulty colours are `color(at: 0/1/2)`.
 */
@Composable
fun TriesDistributionCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val attemptColor = palette.colorAt(7)
    val easyColor = palette.colorAt(0)
    val mediumColor = palette.colorAt(1)
    val hardColor = palette.colorAt(2)

    // iOS: solves.compactMap { tries > 0 }.reversed()
    val points = remember(solves) {
        solves.asReversed().mapNotNull { solve ->
            val tries = solve.submission.numberOfTries
            if (tries != null && tries > 0) {
                AttemptPoint(tries = tries, difficulty = solve.problem.difficulty)
            } else {
                null
            }
        }
    }

    val averageTries = remember(points) {
        if (points.isEmpty()) 0.0 else points.sumOf { it.tries }.toDouble() / points.size
    }
    val firstTryCount = remember(points) { points.count { it.tries == 1 } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = attemptColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Attempts Analysis",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = String.format("%.1f", averageTries),
                        fontSize = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = attemptColor
                    )
                    Text(
                        text = "Average Tries",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$firstTryCount",
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = easyColor
                    )
                    Text(
                        text = "First Try",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                if (points.isEmpty()) return@Canvas

                val maxTries = points.maxOf { it.tries }
                val minTries = 1
                val range = (maxTries - minTries).coerceAtLeast(1)

                val padding = 8.dp.toPx()
                val chartWidth = (size.width - 2 * padding).coerceAtLeast(1f)
                val chartHeight = (size.height - 2 * padding).coerceAtLeast(1f)

                points.forEachIndexed { index, point ->
                    val x = if (points.size == 1) {
                        size.width / 2f
                    } else {
                        padding + (index.toFloat() / (points.size - 1)) * chartWidth
                    }
                    val normalized = (point.tries - minTries).toFloat() / range.toFloat()
                    val y = padding + (1f - normalized) * chartHeight

                    val color = when (point.difficulty.lowercase()) {
                        "easy" -> easyColor
                        "medium" -> mediumColor
                        "hard" -> hardColor
                        else -> Color.Gray
                    }

                    // iOS: 12pt diameter for first-try solves, 8pt otherwise.
                    val radius = if (point.tries == 1) 6.dp.toPx() else 4.dp.toPx()

                    drawCircle(color = color, radius = radius, center = Offset(x, y))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AttemptLegend(easyColor, "Easy")
                AttemptLegend(mediumColor, "Medium")
                AttemptLegend(hardColor, "Hard")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attempts data available",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
private fun AttemptLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}
