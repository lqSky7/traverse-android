package com.traverse.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CardBackground = Color(0xFF1A1A1A)

// iOS `difficultyColor(_:)` uses SwiftUI's system colours rather than the palette.
private val DiffEasyGreen = Color(0xFF34C759)
private val DiffMediumOrange = Color(0xFFFF9500)
private val DiffHardRed = Color(0xFFFF3B30)
private val DiffDefaultBlue = Color(0xFF007AFF)

private val DIFFICULTY_OPTIONS = listOf("All", "Easy", "Medium", "Hard")

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

private data class MistakeAnalysisData(
    val displayedItems: List<TagAnalysisItem>,
    val totalMistakes: Int,
    val maxCount: Int,
    val cleanPercentage: Int,
    val totalTagTypes: Int
)

/**
 * 1:1 port of the iOS `MistakeTagsDetailView`: three summary metrics, a difficulty
 * segmented picker, search + sort controls and the expandable per-tag breakdown.
 */
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

    val palette = rememberPalette()

    val analysisData = remember(solves, selectedDifficulty, searchText, sortOption) {
        buildMistakeAnalysis(solves, selectedDifficulty, searchText, sortOption)
    }

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Summary metrics
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric(
                    value = "${analysisData.totalMistakes}",
                    label = "Total Mistakes",
                    color = palette.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    value = "${analysisData.totalTagTypes}",
                    label = "Mistake Types",
                    color = palette.colorAt(2),
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    value = "${analysisData.cleanPercentage}%",
                    label = "Clean Solves",
                    color = palette.colorAt(1),
                    modifier = Modifier.weight(1f)
                )
            }

            // Difficulty segmented picker
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DIFFICULTY_OPTIONS.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DIFFICULTY_OPTIONS.size
                        ),
                        onClick = { selectedDifficulty = option },
                        selected = selectedDifficulty == option
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Search + sort
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f)
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackground)
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = sortOption.label,
                            style = MaterialTheme.typography.labelSmall.copy(color = palette.primary)
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
                                        color = if (sortOption == option) palette.primary else Color.White,
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

            // Tag list
            if (analysisData.displayedItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = if (searchText.isEmpty()) {
                            "No mistake tags found"
                        } else {
                            "No results for \"$searchText\""
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    analysisData.displayedItems.forEachIndexed { index, item ->
                        val tagColor = palette.colorAt(index % 10)
                        TagAnalysisCard(
                            item = item,
                            tagColor = tagColor,
                            maxCount = analysisData.maxCount,
                            totalMistakes = analysisData.totalMistakes,
                            isExpanded = expandedTags.contains(item.id),
                            onToggle = {
                                expandedTags = if (expandedTags.contains(item.id)) {
                                    expandedTags - item.id
                                } else {
                                    expandedTags + item.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(12.dp)
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Clear search",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onValueChange("") }
                )
            }
        }
    }
}

@Composable
private fun TagAnalysisCard(
    item: TagAnalysisItem,
    tagColor: Color,
    maxCount: Int,
    totalMistakes: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val percentage = if (totalMistakes > 0) {
        ((item.count.toDouble() / totalMistakes) * 100).toInt()
    } else {
        0
    }
    val problemWord = if (item.matchingSolves.size == 1) "problem" else "problems"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tagColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDetailTagIcon(item.tag),
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTagName(item.tag),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$percentage% of all mistakes \u2022 ${item.matchingSolves.size} $problemWord",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${item.count}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = tagColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Progress bar
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val trackWidth = maxWidth
            val ratio = if (maxCount > 0) item.count.toFloat() / maxCount.toFloat() else 0f
            val safeRatio = if (ratio.isFinite()) ratio.coerceIn(0f, 1f) else 0f
            val fillWidth = maxOf(trackWidth * safeRatio, 8.dp).coerceAtMost(trackWidth)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(tagColor, tagColor.copy(alpha = 0.7f))))
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                Text(
                    text = "Recent Problems",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )

                item.matchingSolves.forEach { solve ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = solve.title,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val diffColor = difficultyColor(solve.difficulty)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = diffColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = solve.difficulty.replaceFirstChar { it.uppercase() },
                                color = diffColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatShortDate(solve.solvedAt),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildMistakeAnalysis(
    solves: List<Solve>,
    selectedDifficulty: String,
    searchText: String,
    sortOption: MistakeSortOption
): MistakeAnalysisData {
    val filteredSolves = if (selectedDifficulty == "All") {
        solves
    } else {
        solves.filter { it.problem.difficulty.equals(selectedDifficulty, ignoreCase = true) }
    }

    val tagCounts = HashMap<String, Int>()
    val tagSolves = HashMap<String, MutableList<MistakeSolveSummary>>()
    var solvesWithMistakes = 0

    filteredSolves.forEach { solve ->
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
            tags.forEach { tag ->
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
    val cleanPercentage = if (filteredSolves.isEmpty()) {
        100
    } else {
        ((cleanSolves.coerceAtLeast(0).toDouble() / filteredSolves.size) * 100).toInt()
    }

    val query = searchText.trim()
    val filteredBySearch = if (query.isEmpty()) {
        allItems
    } else {
        allItems.filter { item ->
            item.tag.contains(query, ignoreCase = true) ||
                item.matchingSolves.any { it.title.contains(query, ignoreCase = true) }
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

    return MistakeAnalysisData(
        displayedItems = displayedItems,
        totalMistakes = totalMistakes,
        maxCount = maxCount,
        cleanPercentage = cleanPercentage,
        totalTagTypes = allItems.size
    )
}

private fun difficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "easy" -> DiffEasyGreen
    "medium" -> DiffMediumOrange
    "hard" -> DiffHardRed
    else -> DiffDefaultBlue
}

private fun formatShortDate(dateString: String): String = try {
    LocalDate.parse(dateString.take(10)).format(DateTimeFormatter.ofPattern("MMM d"))
} catch (_: Exception) {
    dateString.take(10)
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
        lower.contains("null") || lower.contains("nil") || lower.contains("pointer") -> Icons.Default.HelpOutline
        lower.contains("syntax") || lower.contains("type") -> Icons.Default.Code
        lower.contains("overflow") -> Icons.Default.TrendingUp
        lower.contains("off-by-one") || lower.contains("index") -> Icons.Default.CompareArrows
        else -> Icons.Default.Tag
    }
}
