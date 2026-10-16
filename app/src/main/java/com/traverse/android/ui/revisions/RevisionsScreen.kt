package com.traverse.android.ui.revisions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import com.traverse.android.ui.components.glance.ActionChip
import com.traverse.android.ui.components.glance.ActionChipRow
import com.traverse.android.ui.components.glance.GlanceCard
import com.traverse.android.ui.components.glance.GlanceHeaderHeight
import com.traverse.android.ui.components.glance.GlanceScreenHeader
import com.traverse.android.ui.components.glance.TraverseRefreshIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.data.RevisionStatsResponse
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.RevisionsViewModel
import com.traverse.android.ui.theme.paletteColorAt
import com.traverse.android.ui.theme.palettePrimary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionsScreen(
    viewModel: RevisionsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataManager = remember { com.traverse.android.data.DataManager.getInstance(context) }
    val currentUser = remember { dataManager.userStats.value?.username ?: "" }

    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Analytics
    var showCalendarSheet by remember { mutableStateOf(false) }
    var showMLControlsSheet by remember { mutableStateOf(false) }
    var showDailyLimitSheet by remember { mutableStateOf(false) }
    var showSchedulingInfoSheet by remember { mutableStateOf(false) }
    var showFreezeShop by remember { mutableStateOf(false) }

    // Load analytics when analytics tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && uiState.analytics == null && !uiState.isAnalyticsLoading) {
            viewModel.loadAnalytics()
        }
    }

    // Full screen Exam Mode takeover
    if (uiState.isExamModeActive) {
        ExamModeActiveView(
            isResuming = false,
            onResume = { viewModel.setExamMode(false) }
        )
        return
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val retentionPercent = uiState.analytics?.overview?.averageRetrievability
    val atRiskCount = uiState.analytics?.retentionHeatmap?.count { it.retrievability < 0.7 } ?: 0

    // Same three cards as iOS: due today, average retention, at-risk problems.
    val revisionCards = listOf(
        GlanceCard(
            id = "upcoming",
            icon = Icons.Filled.Event,
            value = "${uiState.stats?.dueToday ?: 0}",
            label = "Due Today",
            tint = paletteColorAt(2),
            onClick = { selectedTab = 0 }
        ),
        GlanceCard(
            id = "analytics",
            icon = Icons.Filled.TrendingUp,
            value = retentionPercent?.let { "${(it * 100).toInt()}%" } ?: "—",
            label = "Avg Retention",
            tint = paletteColorAt(3),
            progress = retentionPercent?.toFloat(),
            onClick = { selectedTab = 1 }
        ),
        GlanceCard(
            id = "atrisk",
            icon = Icons.Filled.WarningAmber,
            value = "$atRiskCount",
            label = "At Risk",
            tint = paletteColorAt(0),
            badge = atRiskCount.takeIf { it > 0 },
            onClick = { selectedTab = 1 }
        )
    )

    val actionChips = listOf(
        ActionChip(
            id = "freezes",
            icon = Icons.Default.AcUnit,
            title = "Freezes",
            tint = paletteColorAt(3),
            onClick = { showFreezeShop = true }
        ),
        ActionChip(
            id = "limit",
            icon = Icons.Default.Tune,
            title = "Daily Limit",
            tint = paletteColorAt(1),
            onClick = { showDailyLimitSheet = true }
        ),
        ActionChip(
            id = "exam",
            icon = if (uiState.isExamModeActive) Icons.Default.PlayCircle else Icons.Default.School,
            title = if (uiState.isExamModeActive) "Stop Exam" else "Exam Mode",
            tint = paletteColorAt(0),
            onClick = { viewModel.setExamMode(!uiState.isExamModeActive) }
        ),
        ActionChip(
            id = "ml",
            icon = Icons.Default.Psychology,
            title = "ML Info",
            tint = paletteColorAt(2),
            onClick = { showSchedulingInfoSheet = true }
        ),
        ActionChip(
            id = "controls",
            icon = Icons.Default.CalendarMonth,
            title = "ML Controls",
            tint = paletteColorAt(3),
            onClick = { showMLControlsSheet = true }
        ),
        ActionChip(
            id = "calendar",
            icon = Icons.Default.CalendarMonth,
            title = "Calendar",
            tint = paletteColorAt(1),
            onClick = { showCalendarSheet = true }
        ),
        ActionChip(
            id = "completed",
            icon = Icons.Default.CheckCircle,
            title = "Completed",
            tint = paletteColorAt(4),
            onClick = { viewModel.toggleShowCompleted() }
        )
    )

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // The glance header (drawn over the list below) replaces the app
        // bar; the old ⋯ menu is now a row of action chips in the list.
        topBar = {}
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The Upcoming/Analytics switch now lives in the header's glance
            // cards: Due Today -> Upcoming, Avg Retention / At Risk -> Analytics.

            // Content based on selected tab
            when (selectedTab) {
                1 -> {
                    // Analytics Tab
                    when {
                        uiState.isAnalyticsLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        uiState.analyticsError != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = uiState.analyticsError ?: "Failed to load analytics",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.loadAnalytics() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        uiState.analytics != null -> {
                            MLAnalyticsScreen(
                                analytics = uiState.analytics!!,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            // `analytics` is null and the load is not in flight, so there is
                            // genuinely nothing to chart — a first-run account that has not
                            // completed a revision yet. A bare "No analytics data available" read
                            // as a broken screen; this names the cause and the way out of it.
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(
                                    icon = Icons.Default.BarChart,
                                    title = "No analytics yet",
                                    message = "These charts are built from your completed " +
                                        "revisions. Finish a few in the Revisions tab and the " +
                                        "trends appear here."
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Upcoming Tab
                    RevisionsListContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        topInset = GlanceHeaderHeight + statusBarTop + 16.dp,
                        chips = actionChips,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Pinned header: title + glance cards over the progressive scrim.
        GlanceScreenHeader(
            title = "Revisions",
            cards = revisionCards,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        }
    }

    // Freeze Shop — moved out of Settings: freezes protect scheduled
    // revisions, not a general preference.
    if (showFreezeShop) {
        com.traverse.android.ui.settings.FreezeShopSheet(
            userXp = dataManager.userStats.value?.stats?.totalXp ?: 0,
            onDismiss = { showFreezeShop = false },
            onXpUpdated = { viewModel.refresh() }
        )
    }

    // Pro Upgrade Dialog if not subscribed
    if (uiState.showProUpgradeDialog) {
        ProUpgradeSheet(onDismiss = { viewModel.dismissProUpgradeDialog() })
    }

    // Calendar Export Sheet
    if (showCalendarSheet) {
        CalendarExportSheet(
            username = currentUser,
            onDismiss = { showCalendarSheet = false }
        )
    }

    // ML Controls Sheet
    if (showMLControlsSheet) {
        MLControlsSheet(
            onDismiss = { showMLControlsSheet = false },
            onUpdated = { viewModel.refresh() }
        )
    }

    // Daily Review Limit Sheet
    if (showDailyLimitSheet) {
        DailyReviewLimitSheet(
            currentLimit = uiState.todaySummary?.maxDaily ?: uiState.dailyReviewLimit,
            todaySummary = uiState.todaySummary,
            onDismiss = { showDailyLimitSheet = false },
            onLimitUpdated = {
                viewModel.refresh()
            }
        )
    }

    // FSRS Spaced Repetition Info Sheet
    if (showSchedulingInfoSheet) {
        MLSchedulingInfoSheet(
            onDismiss = { showSchedulingInfoSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevisionsListContent(
    uiState: com.traverse.android.viewmodel.RevisionsUiState,
    viewModel: RevisionsViewModel,
    topInset: androidx.compose.ui.unit.Dp,
    chips: List<ActionChip>,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        onRefresh = { viewModel.refresh() },
        isRefreshing = uiState.isLoading,
        // Our own palette-themed loader instead of the grey default.
        indicator = { TraverseRefreshIndicator(isRefreshing = uiState.isLoading) },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    // Clears the pinned header so the list scrolls under it.
                    top = topInset,
                    // Clears the floating bottom bar. Content still scrolls underneath the pill
                    // because this is a `contentPadding` rather than an outer modifier.
                    bottom = floatingBottomBarContentInset()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Action chips — first in the scroll content, NOT pinned: they
                // scroll away under the header. This is the old ⋯ menu.
                item(key = "chips") {
                    ActionChipRow(chips = chips)
                }

                // Loading State
                if (uiState.isLoading && uiState.revisionGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Error State
                else if (uiState.errorMessage != null && uiState.revisionGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = uiState.errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.refresh() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                // Empty State
                else if (uiState.revisionGroups.isEmpty()) {
                    item {
                        EmptyRevisionsState(modifier = Modifier.fillParentMaxSize())
                    }
                }

                // List State
                else {
                    items(
                        items = uiState.revisionGroups,
                        key = { it.date }
                    ) { group ->
                        RevisionGroupCard(
                            group = group,
                            onDeleteSingle = { viewModel.deleteRevision(it) },
                            onDeleteProblem = { viewModel.deleteProblemRevisions(it) },
                            onRescheduleDays = { id, days -> viewModel.rescheduleRevision(id, days) }
                        )
                    }
                }
            }

            // Floating Stats Toolbar
            uiState.stats?.let { stats ->
                FloatingStatsToolbar(
                    stats = stats,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingStatsToolbar(
    stats: RevisionStatsResponse,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StatBadge(
            title = "Tracked",
            value = stats.total.toString(),
            color = paletteColorAt(4)
        )
        StatBadge(
            title = "Due Today",
            value = stats.dueToday.toString(),
            color = paletteColorAt(2)
        )
        StatBadge(
            title = "Done",
            value = "${stats.completionRate}%",
            color = paletteColorAt(1)
        )
    }
}

@Composable
private fun StatBadge(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
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
private fun EmptyRevisionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        EmptyStateView(
            icon = Icons.Default.CalendarMonth,
            title = "No Revisions Scheduled",
            message = "Revisions are scheduled for you once you start solving problems. " +
                "Install the browser extension and your first solve sets the schedule up."
        )
    }
}
