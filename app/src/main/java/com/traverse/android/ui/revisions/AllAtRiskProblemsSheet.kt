package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.sp
import com.traverse.android.data.RevisionRetentionItem
import com.traverse.android.ui.components.rememberSheetOverscrollClamper
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import kotlin.math.roundToInt

private val EasyPastel = Color(0xFFA8E6CF)
private val MediumPastel = Color(0xFFFFD3B6)
private val HardPastel = Color(0xFFFFAAA5)
private val AccentPastel = Color(0xFFB8D4E3)
private val CardBackground = Color(0xFF1A1A1A)

enum class RiskSortOption(val label: String) {
    LOWEST_RETENTION("Lowest Retention"),
    MOST_LAPSES("Most Lapses"),
    ALPHABETICAL("Alphabetical")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAtRiskProblemsSheet(
    items: List<RevisionRetentionItem>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchText by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(RiskSortOption.LOWEST_RETENTION) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredItems = remember(items, searchText, sortOption) {
        val trimmed = searchText.trim()
        val list = if (trimmed.isEmpty()) items else items.filter {
            it.problemTitle.contains(trimmed, ignoreCase = true) ||
            it.platform.contains(trimmed, ignoreCase = true)
        }

        when (sortOption) {
            RiskSortOption.LOWEST_RETENTION -> list.sortedBy { it.retrievability }
            RiskSortOption.MOST_LAPSES -> list.sortedByDescending { it.lapses }
            RiskSortOption.ALPHABETICAL -> list.sortedBy { it.problemTitle.lowercase() }
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
                    text = "At-Risk Problems (${items.size})",
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
                            RiskSortOption.entries.forEach { option ->
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
                placeholder = { Text("Search problems...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredItems, key = { it.problemId }) { item ->
                    RiskProblemDetailCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun RiskProblemDetailCard(item: RevisionRetentionItem) {
    val retrievabilityPct = (item.retrievability * 100).roundToInt()
    val dotColor = when {
        item.isLeech || retrievabilityPct < 50 -> Color(0xFFEF4444)
        retrievabilityPct < 70 -> MediumPastel
        else -> EasyPastel
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Column {
                        Text(
                            text = item.problemTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.platform.replaceFirstChar { it.uppercase() }} • ${item.difficulty.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                if (item.isLeech) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HardPastel.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "LEECH",
                            color = HardPastel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Metrics row: Retrievability, Stability, Lapses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Retrievability",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                        )
                        Text(
                            text = "$retrievabilityPct%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = dotColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = item.retrievability.toFloat().coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(dotColor)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Lapses: ${item.lapses}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (item.lapses > 3) HardPastel else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "Stability: ${"%.1f".format(item.stability)}d",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    )
                }
            }
        }
    }
}
