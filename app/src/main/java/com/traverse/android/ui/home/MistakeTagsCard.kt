package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.traverse.android.data.Solve

private val CardBackground = Color(0xFF1A1A1A)

private val TagColors = listOf(
    Color(0xFFFFB6C1),
    Color(0xFFB6E3FF),
    Color(0xFFFFE4B6),
    Color(0xFFB6FFD8),
    Color(0xFFE6B6FF)
)

@Composable
fun MistakeTagsCard(
    solves: List<Solve>,
    modifier: Modifier = Modifier
) {
    val tagCounts = mutableMapOf<String, Int>()
    solves.forEach { solve ->
        val tags = solve.mistakeTags ?: solve.submission.mistakeTags ?: emptyList()
        tags.forEach { tag -> tagCounts[tag] = (tagCounts[tag] ?: 0) + 1 }
    }
    
    val topTags = tagCounts.entries.sortedByDescending { it.value }.take(5)
    val maxCount = topTags.maxOfOrNull { it.value } ?: 1
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mistake Analysis",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${tagCounts.size} types",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )
            
            if (topTags.isEmpty()) {
                Text(
                    text = "No mistakes detected! 🎉",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                )
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
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f)),
            modifier = Modifier.width(100.dp),
            maxLines = 1
        )
        
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
                    .fillMaxWidth(fraction = progress.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$count", style = MaterialTheme.typography.labelMedium.copy(color = color), modifier = Modifier.width(30.dp))
    }
}
