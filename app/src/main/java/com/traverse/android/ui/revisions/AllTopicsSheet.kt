package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RevisionTopicMetric
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlin.math.roundToInt

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

enum class TopicSortOption(val label: String) {
    LOWEST_RETENTION("Lowest Retention"),
    HIGHEST_RETENTION("Highest Retention"),
    MOST_PROBLEMS("Most Problems"),
    SOLVE_TIME("Solve Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTopicsSheet(
    topics: List<RevisionTopicMetric>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchText by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(TopicSortOption.LOWEST_RETENTION) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredTopics = remember(topics, searchText, sortOption) {
        val trimmed = searchText.trim()
        val list = if (trimmed.isEmpty()) topics else topics.filter { it.topic.contains(trimmed, ignoreCase = true) }

        when (sortOption) {
            TopicSortOption.LOWEST_RETENTION -> list.sortedBy { it.averageRetention }
            TopicSortOption.HIGHEST_RETENTION -> list.sortedByDescending { it.averageRetention }
            TopicSortOption.MOST_PROBLEMS -> list.sortedByDescending { it.problemCount }
            TopicSortOption.SOLVE_TIME -> list.sortedByDescending { it.averageTimeMinutes }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .nestedScroll(rememberSheetOverscrollClamper())
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Topics (${topics.size})",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = BelfastGroteskBlackFamily,
                        color = Color.White
                    )
                )

                Row {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = AccentPastel
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = Color(0xFF242424)
                        ) {
                            TopicSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = if (sortOption == option) AccentPastel else Color.White,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Field
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search topics...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Topic Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredTopics, key = { it.topic }) { topic ->
                    TopicDetailCard(topic = topic)
                }
            }
        }
    }
}

@Composable
private fun TopicDetailCard(topic: RevisionTopicMetric) {
    val retentionPct = (topic.averageRetention * 100).roundToInt()
    val retentionColor = when {
        retentionPct >= 80 -> EasyPastel
        retentionPct >= 60 -> MediumPastel
        else -> HardPastel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = topic.topic,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "• ${topic.problemCount} problem${if (topic.problemCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time pill
                    if (topic.averageTimeMinutes > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${"%.1f".format(topic.averageTimeMinutes)}m",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "$retentionPct%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = retentionColor
                        )
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = topic.averageRetention.toFloat().coerceIn(0.05f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(retentionColor)
                )
            }

            // Average stability footer
            if (topic.averageStability > 0) {
                Text(
                    text = "Avg Stability: ${"%.1f".format(topic.averageStability)}d",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.45f)
                    )
                )
            }
        }
    }
}
