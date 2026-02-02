package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.traverse.android.data.Solve

private val CardBackground = Color(0xFF1A1A1A)
private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)

@Composable
fun RecentSolvesCard(
    solves: List<Solve>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Solves",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View all",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                solves.take(4).forEach { solve -> SolveRow(solve = solve) }
            }
        }
    }
}

@Composable
private fun SolveRow(solve: Solve) {
    val difficultyColor = when (solve.problem.difficulty.lowercase()) {
        "easy" -> EasyPastel
        "medium" -> MediumPastel
        "hard" -> HardPastel
        else -> Color.White.copy(alpha = 0.5f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(difficultyColor)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = solve.problem.title,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = solve.problem.platform,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
        
        Text(
            text = "+${solve.xpAwarded} XP",
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f))
        )
    }
}
