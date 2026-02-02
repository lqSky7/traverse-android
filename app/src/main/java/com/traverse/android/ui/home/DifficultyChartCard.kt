package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.ProblemsByDifficulty

private val CardBackground = Color(0xFF1A1A1A)

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)

@Composable
fun DifficultyChartCard(
    difficulty: ProblemsByDifficulty,
    modifier: Modifier = Modifier
) {
    val total = difficulty.easy + difficulty.medium + difficulty.hard
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Difficulty",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Circular chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularDifficultyChart(
                    easy = difficulty.easy,
                    medium = difficulty.medium,
                    hard = difficulty.hard,
                    modifier = Modifier.fillMaxSize(0.8f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "solved",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(color = EasyPastel, label = "${difficulty.easy}")
                LegendDot(color = MediumPastel, label = "${difficulty.medium}")
                LegendDot(color = HardPastel, label = "${difficulty.hard}")
            }
        }
    }
}

@Composable
private fun CircularDifficultyChart(easy: Int, medium: Int, hard: Int, modifier: Modifier = Modifier) {
    val total = (easy + medium + hard).coerceAtLeast(1).toFloat()
    val strokeWidth = 14f
    
    Canvas(modifier = modifier) {
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        
        var startAngle = -90f
        
        val easyAngle = (easy / total) * 360f
        drawArc(color = EasyPastel, startAngle = startAngle, sweepAngle = easyAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        startAngle += easyAngle
        
        val mediumAngle = (medium / total) * 360f
        drawArc(color = MediumPastel, startAngle = startAngle, sweepAngle = mediumAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        startAngle += mediumAngle
        
        val hardAngle = (hard / total) * 360f
        drawArc(color = HardPastel, startAngle = startAngle, sweepAngle = hardAngle, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = color) }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
    }
}
