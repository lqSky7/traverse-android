package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve

private val CardBackground = Color(0xFF1A1A1A)
private val EasyColor = Color(0xFFBBF7D0)    // Pastel mint green
private val MediumColor = Color(0xFFFED7AA)  // Pastel peach
private val HardColor = Color(0xFFFECDD3)    // Pastel rose

@Composable
fun AttemptAnalysisCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Calculate scatter plot data
    val scatterData = remember(solves) {
        solves.takeLast(20).mapNotNull { solve ->
            val attempts = solve.submission.numberOfTries ?: 1
            if (attempts > 0) {
                ScatterPoint(
                    attempts = attempts,
                    difficulty = solve.problem.difficulty
                )
            } else null
        }
    }
    
    val avgTries = remember(solves) {
        val triesWithValue = solves.mapNotNull { it.submission.numberOfTries }.filter { it > 0 }
        if (triesWithValue.isEmpty()) 0.0 else triesWithValue.average()
    }
    
    val firstTryCount = remember(solves) {
        solves.count { (it.submission.numberOfTries ?: 1) == 1 }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFDDD6FE), // Pastel lavender
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attempts Analysis",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Stats Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Average Tries (left side - large)
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = String.format("%.1f", avgTries),
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = Color(0xFFDDD6FE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        )
                    )
                    Text(
                        text = "Average Tries",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                
                // First Try (right side - smaller)
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$firstTryCount",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "First Try",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
            
            // Scatter Plot
            if (scatterData.isNotEmpty()) {
                AttemptsScatterPlot(
                    scatterData = scatterData,
                    density = density,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendItem(color = EasyColor, text = "Easy")
                    LegendItem(color = MediumColor, text = "Medium") 
                    LegendItem(color = HardColor, text = "Hard")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No attempt data",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun AttemptsScatterPlot(
    scatterData: List<ScatterPoint>,
    density: androidx.compose.ui.unit.Density,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawAttemptsScatterPlot(scatterData, density)
    }
}

private fun DrawScope.drawAttemptsScatterPlot(
    scatterData: List<ScatterPoint>,
    density: androidx.compose.ui.unit.Density
) {
    if (scatterData.isEmpty()) return
    
    val maxAttempts = scatterData.maxOfOrNull { it.attempts } ?: 1
    val minAttempts = 1
    
    val width = size.width
    val height = size.height
    val padding = 20f
    val chartWidth = width - 2 * padding
    val chartHeight = height - 2 * padding
    
    // Draw scatter points
    scatterData.forEachIndexed { index, point ->
        // Distribute points horizontally across the chart
        val x = padding + (index.toFloat() / (scatterData.size - 1).coerceAtLeast(1)) * chartWidth
        
        // Position vertically based on attempt count (fix Y-axis)
        val normalizedY = (point.attempts - minAttempts).toFloat() / (maxAttempts - minAttempts).coerceAtLeast(1)
        val y = padding + (1f - normalizedY) * chartHeight  // Fixed: flip Y coordinate
        
        val color = when (point.difficulty.lowercase()) {
            "easy" -> EasyColor
            "medium" -> MediumColor
            "hard" -> HardColor
            else -> Color.Gray
        }
        
        // Make first-try attempts slightly larger
        val radius = with(density) { if (point.attempts == 1) 6.dp.toPx() else 4.dp.toPx() }
        
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private data class ScatterPoint(
    val attempts: Int,
    val difficulty: String
)
