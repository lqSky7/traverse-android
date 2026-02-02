package com.traverse.android.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.traverse.android.data.Solve
import java.time.LocalDate

private val CardBackground = Color(0xFF1A1A1A)
private val ActivityPastel = Color(0xFFA8E6CF)

@Composable
fun SolveHeatmapCard(
    solves: List<Solve>,
    frozenDates: List<String>,
    modifier: Modifier = Modifier
) {
    val solvesByDate = remember(solves) {
        solves.groupBy { it.solvedAt.take(10) }.mapValues { it.value.size }
    }
    
    val today = LocalDate.now()
    val weeks = 16
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${solves.size} solves",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Heatmap - full width, auto-sized cells
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val cellSpacing = 3.dp
                val cellSize = ((maxWidth - (cellSpacing * (weeks - 1))) / weeks)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                ) {
                    for (week in 0 until weeks) {
                        Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                            for (day in 0 until 7) {
                                val daysAgo = (weeks - 1 - week) * 7 + (6 - day)
                                val date = today.minusDays(daysAgo.toLong())
                                val dateString = date.toString()
                                val count = solvesByDate[dateString] ?: 0
                                val isFrozen = frozenDates.contains(dateString)
                                
                                val color = when {
                                    isFrozen -> Color(0xFF64B5F6).copy(alpha = 0.7f)
                                    count == 0 -> Color.White.copy(alpha = 0.05f)
                                    count == 1 -> ActivityPastel.copy(alpha = 0.3f)
                                    count <= 3 -> ActivityPastel.copy(alpha = 0.5f)
                                    count <= 5 -> ActivityPastel.copy(alpha = 0.7f)
                                    else -> ActivityPastel
                                }
                                
                                Canvas(modifier = Modifier.size(cellSize)) {
                                    drawRoundRect(
                                        color = color,
                                        cornerRadius = CornerRadius(2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
                Spacer(modifier = Modifier.width(4.dp))
                listOf(0.05f, 0.3f, 0.5f, 0.7f, 1f).forEach { alpha ->
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawRoundRect(
                            color = ActivityPastel.copy(alpha = alpha),
                            cornerRadius = CornerRadius(1.dp.toPx())
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("More", style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}
