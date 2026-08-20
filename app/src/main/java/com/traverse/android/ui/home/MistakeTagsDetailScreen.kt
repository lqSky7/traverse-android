package com.traverse.android.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

private val TagColors = listOf(
    Color(0xFFFFB6C1),
    Color(0xFFB6E3FF),
    Color(0xFFFFE4B6),
    Color(0xFFB6FFD8),
    Color(0xFFE6B6FF),
    Color(0xFFFFF0B6)
)

private data class MistakeSolveSummary(
    val id: String,
    val title: String,
    val difficulty: String,
    val solvedAt: String
)

private data class TagAnalysisItem(
    val id: String,
    val tag: String,
    val count: Int,
    val matchingSolves: List<MistakeSolveSummary>
)

enum class MistakeSortOption(val label: String) {
    MOST_FREQUENT("Most Frequent"),
    LEAST_FREQUENT("Least Frequent"),
    ALPHABETICAL("Alphabetical")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeTagsDetailScreen(
    solves: List<Solve>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf(MistakeSortOption.MOST_FREQUENT) }
    var expandedTags by remember { mutableStateOf(setOf<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Analysis calculation matching iOS 1:1
    val analysisData = remember(solves, selectedDifficulty, searchText, sortOption) {
        val diffLower = selectedDifficulty.lowercase()
        val filteredSolves = if (selectedDifficulty == "All") {
            solves
        } else {
            solves.filter { it.problem.difficulty.lowercase() == diffLower }
        }

        val tagCounts = mutableMapOf<String, Int>()
        val tagSolves = mutableMapOf<String, MutableList<MistakeSolveSummary>>()
        var solvesWithMistakes = 0

        for (solve in filteredSolves) {
            val tags = solve.mistakeTags ?: solve.submission.mistakeTags ?: emptyList()
            if (tags.isNotEmpty()) {
                solvesWithMistakes++
                val seen = mutableSetOf<String>()
                val summary = MistakeSolveSummary(
                    id = "${solve.id}_${solve.solvedAt}",
                    title = solve.problem.title,
                    difficulty = solve.problem.difficulty,
                    solvedAt = solve.solvedAt
                )
                for (tag in tags) {
                    if (seen.add(tag)) {
                        tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                        tagSolves.getOrPut(tag) { mutableListOf() }.add(summary)
                    }
                }
            }
        }

        val allItems = tagCounts.map { (tag, count) ->
            TagAnalysisItem(
                id = tag,
                tag = tag,
                count = count,
                matchingSolves = tagSolves[tag] ?: emptyList()
            )
        }

        val totalMistakes = allItems.sumOf { it.count }
        val maxCount = (allItems.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
        val cleanSolves = filteredSolves.size - solvesWithMistakes
        val cleanPercentage = if (filteredSolves.isEmpty()) 100 else ((cleanSolves.coerceAtLeast(0).toDouble() / filteredSolves.size) * 100).toInt()

        val searchTrimmed = searchText.trim()
        val filteredBySearch = if (searchTrimmed.isEmpty()) {
            allItems
        } else {
            allItems.filter { item ->
                item.tag.contains(searchTrimmed, ignoreCase = true) ||
                item.matchingSolves.any { it.title.contains(searchTrimmed, ignoreCase = true) }
            }
        }

        val displayedItems = when (sortOption) {
            MistakeSortOption.MOST_FREQUENT -> filteredBySearch.sortedWith(
                compareByDescending<TagAnalysisItem> { it.count }.thenBy { it.tag.lowercase() }
            )
            MistakeSortOption.LEAST_FREQUENT -> filteredBySearch.sortedWith(
                compareBy<TagAnalysisItem> { it.count }.thenBy { it.tag.lowercase() }
            )
            MistakeSortOption.ALPHABETICAL -> filteredBySearch.sortedBy { it.tag.lowercase() }
        }

        object {
            val items = displayedItems
            val total = totalMistakes
            val max = maxCount
            val cleanPercent = cleanPercentage
            val totalTags = allItems.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mistake Analysis",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = CardBackground
                        ) {
                            MistakeSortOption.entries.forEach { option ->
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
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search mistakes or problem names...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Difficulty Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Easy", "Medium", "Hard").forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPastel.copy(alpha = 0.2f),
                                selectedLabelColor = AccentPastel
                            )
                        )
                    }
                }
            }

            // 3. Stats Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            title = "Total Mistakes",
                            value = "${analysisData.total}",
                            color = HardPastel
                        )
                        StatDivider()
                        StatItem(
                            title = "Clean Solves",
                            value = "${analysisData.cleanPercent}%",
                            color = EasyPastel
                        )
                        StatDivider()
                        StatItem(
                            title = "Tag Types",
                            value = "${analysisData.totalTags}",
                            color = AccentPastel
                        )
                    }
                }
            }

            // 4. Mistakes List
            if (analysisData.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchText.isEmpty()) "No mistakes recorded! 🎉" else "No matching mistake tags found.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            } else {
                items(
                    items = analysisData.items,
                    key = { it.id }
                ) { item ->
                    val isExpanded = expandedTags.contains(item.tag)
                    val tagIndex = analysisData.items.indexOf(item)
                    val tagColor = TagColors[tagIndex % TagColors.size]
                    val progress = (item.count.toFloat() / analysisData.max).coerceIn(0.05f, 1f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTags = if (isExpanded) {
                                    expandedTags - item.tag
                                } else {
                                    expandedTags + item.tag
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row with icon, tag name, count and chevron
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(tagColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getDetailTagIcon(item.tag),
                                        contentDescription = null,
                                        tint = tagColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatTagName(item.tag),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${item.matchingSolves.size} problem${if (item.matchingSolves.size == 1) "" else "s"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                Text(
                                    text = "${item.count}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = tagColor
                                    )
                                )

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
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
                                        .fillMaxWidth(fraction = progress)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(tagColor)
                                )
                            }

                            // Expandable Matching Solves list
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    item.matchingSolves.forEach { solve ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = solve.title,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = Color.White.copy(alpha = 0.9f),
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = solve.solvedAt.take(10),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White.copy(alpha = 0.4f)
                                                    )
                                                )
                                            }

                                            val diffColor = when (solve.difficulty.lowercase()) {
                                                "easy" -> EasyPastel
                                                "medium" -> MediumPastel
                                                "hard" -> HardPastel
                                                else -> AccentPastel
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = diffColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = solve.difficulty.replaceFirstChar { it.uppercase() },
                                                    color = diffColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = BelfastGroteskBlackFamily,
                color = color
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
}

private fun formatTagName(tag: String): String {
    return tag.replace("-", " ")
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private fun getDetailTagIcon(tag: String): ImageVector {
    val lower = tag.lowercase()
    return when {
        lower.contains("time") || lower.contains("tle") -> Icons.Default.HourglassEmpty
        lower.contains("memory") || lower.contains("mle") || lower.contains("space") -> Icons.Default.Memory
        lower.contains("edge") || lower.contains("corner") || lower.contains("bound") -> Icons.Default.Warning
        lower.contains("approach") || lower.contains("logic") || lower.contains("algo") -> Icons.Default.Psychology
        lower.contains("base") || lower.contains("recursion") -> Icons.Default.Loop
        lower.contains("null") || lower.contains("pointer") -> Icons.Default.HelpOutline
        lower.contains("syntax") || lower.contains("type") -> Icons.Default.Code
        lower.contains("overflow") -> Icons.Default.TrendingUp
        lower.contains("off-by-one") || lower.contains("index") -> Icons.Default.CompareArrows
        else -> Icons.Default.Tag
    }
}
