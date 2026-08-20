package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
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
private val TagColors = listOf(
    Color(0xFFFFB6C1), // Pastel Pink
    Color(0xFFB8D4E3), // Pastel Blue
    Color(0xFFFFD3B6), // Pastel Peach
    Color(0xFFA8E6CF), // Pastel Green
    Color(0xFFE6E6FA)  // Pastel Purple
)

@Composable
fun MistakeTagsCard(
    solves: List<Solve>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mistakeSolves = solves.filter { it.mistakeTags.isNotEmpty() }
    
    // Count tags
    val tagCounts = mutableMapOf<String, Int>()
    mistakeSolves.forEach { solve ->
        solve.mistakeTags.forEach { tag ->
            tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
        }
    }
    
    val topTags = tagCounts.entries.sortedByDescending { it.value }.take(4)
    val maxCount = topTags.maxOfOrNull { it.value } ?: 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAll),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = TagColors[0],
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mistake Analysis",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            if (topTags.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFA8E6CF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "No mistakes detected",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topTags.forEachIndexed { index, (tag, count) ->
                        TagProgressRow(tag, count, maxCount, TagColors[index % TagColors.size])
                    }
                }
            }
        }
    }
}

@Composable
private fun TagProgressRow(tag: String, count: Int, maxCount: Int, color: Color) {
    val progress = count.toFloat() / maxCount
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall.copy(color = color),
            modifier = Modifier.width(24.dp)
        )
    }
}
