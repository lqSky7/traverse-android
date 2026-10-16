package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import com.traverse.android.data.ActivityTimestamp
import com.traverse.android.ui.components.glance.GlanceCard
import com.traverse.android.ui.components.glance.GlanceHeaderHeight
import com.traverse.android.ui.components.glance.GlanceScreenHeader
import com.traverse.android.ui.theme.rememberPalette
import java.time.LocalDate
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.Solve
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.components.GettingStartedEmptyState
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette

/**
 * 1:1 port of the iOS `AllSolvesView`: a search field, a horizontally scrolling topic
 * filter, and the full list of solves rendered as expandable [SolveRow]s.
 *
 * The selected topic chip is filled with `selectedPalette.primary`, matching iOS.
 *
 * It is also how a profile renders someone else's history, which is why it takes a
 * [title] and an optional [onLoadMore]: a friend's feed is cursor-paginated on the
 * server, so the list has to be able to pull the next page rather than assume it
 * holds everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSolvesScreen(
    solves: List<Solve>,
    onBack: () -> Unit,
    title: String = "All Solves",
    /**
     * Invoked when the end of the list is reached and the server reported more rows.
     * The caller is responsible for ignoring repeat calls.
     */
    onLoadMore: (suspend () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val palette = rememberPalette()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // The same four cards as iOS: total, this week (7-day sparkline),
    // average per day, and the single best day.
    val dailyCounts = remember(solves) {
        val today = LocalDate.now()
        val counts = IntArray(7)
        solves.forEach { solve ->
            ActivityTimestamp.dayKey(solve.solvedAt)?.let { day ->
                val offset = java.time.temporal.ChronoUnit.DAYS.between(day, today).toInt()
                if (offset in 0..6) counts[6 - offset] += 1
            }
        }
        counts.toList()
    }
    val weekTotal = dailyCounts.sum()
    val bestDayCount = remember(solves) {
        solves.mapNotNull { ActivityTimestamp.dayKey(it.solvedAt) }
            .groupingBy { it }
            .eachCount()
            .maxOfOrNull { it.value } ?: 0
    }

    val glanceCards = listOf(
        GlanceCard(
            id = "total",
            icon = Icons.Filled.CheckCircle,
            value = "${solves.size}",
            label = "Total",
            tint = palette.colorAt(1)
        ),
        GlanceCard(
            id = "week",
            icon = Icons.Filled.Event,
            value = "$weekTotal",
            label = "This Week",
            tint = palette.colorAt(2),
            sparkline = dailyCounts.map { it.toFloat() }
        ),
        GlanceCard(
            id = "avg",
            icon = Icons.Filled.BarChart,
            value = String.format("%.1f", weekTotal / 7f),
            label = "Avg / Day",
            tint = palette.colorAt(3)
        ),
        GlanceCard(
            id = "best",
            icon = Icons.Filled.LocalFireDepartment,
            value = "$bestDayCount",
            label = "Best Day",
            tint = palette.colorAt(0)
        )
    )

    val availableTopics = remember(solves) {
        solves.mapNotNull { it.problem.topic }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val filteredSolves = remember(solves, searchText, selectedTopic) {
        solves.filter { solve ->
            val query = searchText
            val matchesSearch = query.isEmpty() ||
                solve.problem.title.contains(query, ignoreCase = true) ||
                solve.problem.slug.contains(query, ignoreCase = true) ||
                (solve.problem.topic ?: "").contains(query, ignoreCase = true) ||
                (solve.problem.subtopic ?: "").contains(query, ignoreCase = true)

            val matchesTopic = selectedTopic == null || solve.problem.topic == selectedTopic

            matchesSearch && matchesTopic
        }
    }

    // Only page while the user is looking at the unfiltered list — pulling more rows
    // behind an active search would grow the list under a filter that cannot use them.
    val canLoadMore = onLoadMore != null && searchText.isEmpty() && selectedTopic == null

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // The glance header below carries the title and the back button.
        topBar = {}
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Clears the pinned header so the list scrolls under it.
            Spacer(modifier = Modifier.height(GlanceHeaderHeight + statusBarTop + 8.dp))

            SearchField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            )

            if (availableTopics.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopicChip(
                        label = "All Topics",
                        selected = selectedTopic == null,
                        onClick = { selectedTopic = null }
                    )
                    availableTopics.forEach { topic ->
                        TopicChip(
                            label = topic,
                            selected = selectedTopic == topic,
                            onClick = {
                                selectedTopic = if (selectedTopic == topic) null else topic
                            }
                        )
                    }
                }
            }

            if (filteredSolves.isEmpty()) {
                // Two genuinely different situations, and the old copy conflated them: a brand-new
                // account with no solves at all (installing the extension is the answer), and a
                // filter that excluded everything (clearing the filter is). "No solves match your
                // criteria" told a first-run user to adjust a filter they had never touched.
                if (solves.isEmpty()) {
                    GettingStartedEmptyState(
                        title = "No solves yet",
                        message = "Your solves appear here once Traverse has seen you solve a " +
                            "problem in the browser."
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.SearchOff,
                            title = "Nothing matches those filters",
                            message = "No solve in this list matches the current search and topic.",
                            actionTitle = "Clear Filters",
                            onAction = {
                                searchText = ""
                                selectedTopic = null
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Clears the floating bottom bar; the list still scrolls underneath it.
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = floatingBottomBarContentInset()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSolves, key = { it.id }) { solve ->
                        SolveRow(solve = solve)
                    }

                    if (canLoadMore) {
                        item(key = "load-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            // Fires when the footer scrolls into view, which is when the
                            // next page is actually wanted.
                            LaunchedEffect(Unit) {
                                if (!isLoadingMore) {
                                    isLoadingMore = true
                                    try {
                                        onLoadMore?.invoke()
                                    } finally {
                                        isLoadingMore = false
                                    }
                                }
                            }
                        }
                    }
                }
            }

            }

            // Pinned header: title + stat cards over the progressive scrim.
            // It is the last child of the Box, so it overlays the scrolling
            // list instead of scrolling with it.
            GlanceScreenHeader(
                title = title,
                cards = glanceCards,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
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
            .background(Color.White.copy(alpha = 0.1f))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Search problems, topics, subtopics...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
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
                        .size(18.dp)
                        .clickable { onValueChange("") }
                )
            }
        }
    }
}

@Composable
private fun TopicChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = rememberPalette().primary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) primary else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.Black else Color.White
            )
        )
    }
}
