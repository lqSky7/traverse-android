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
import com.traverse.android.data.RevisionStatsResponse
import com.traverse.android.ui.components.EmptyStateView
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.BelfastGroteskBlackFamily
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.RevisionsViewModel
import com.traverse.android.ui.theme.paletteColorAt
import com.traverse.android.ui.theme.palettePrimary

private val CardBackground = Color(0xFF1A1A1A)

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

    Scaffold(
        // Bottom inset is owned by the root navigation Scaffold's bottom bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Revisions",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                actions = {
                    // Single overflow Menu — mirrors iOS `ellipsis.circle` toolbar Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = CardBackground,
                            modifier = Modifier.width(230.dp)
                        ) {
                            // 1. How ML Scheduling Works — iOS: Label("How ML Scheduling Works", systemImage: "brain.head.profile")
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "How ML Scheduling Works",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = paletteColorAt(2)
                                        )
                                    )
                                },
                                onClick = {
                                    showSchedulingInfoSheet = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = paletteColorAt(2)
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // 2. Daily Revision Limit — iOS: Label("Daily Revision Limit", systemImage: "slider.horizontal.3")
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Daily Revision Limit",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = palettePrimary
                                        )
                                    )
                                },
                                onClick = {
                                    showDailyLimitSheet = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = palettePrimary
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // 3. Exam Mode — iOS: "Exam Mode" (graduationcap.fill) / "Stop Exam Mode" (play.circle.fill)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.isExamModeActive) "Stop Exam Mode" else "Exam Mode",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = paletteColorAt(4)
                                        )
                                    )
                                },
                                onClick = {
                                    viewModel.setExamMode(!uiState.isExamModeActive)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.isExamModeActive) {
                                            Icons.Default.PlayCircle
                                        } else {
                                            Icons.Default.School
                                        },
                                        contentDescription = null,
                                        tint = paletteColorAt(4)
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )

                            // Android-only extras (no iOS counterpart)

                            // Show Completed Toggle
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = uiState.showCompletedRevisions,
                                            onCheckedChange = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Show Completed",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = BelfastGroteskBlackFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.toggleShowCompleted()
                                    showMenu = false
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // ML Controls & Pause
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "ML Controls & Pause",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = paletteColorAt(4)
                                        )
                                    )
                                },
                                onClick = {
                                    showMLControlsSheet = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = paletteColorAt(4)
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Calendar Export
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Calendar Feed Sync",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = BelfastGroteskBlackFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = palettePrimary
                                        )
                                    )
                                },
                                onClick = {
                                    showCalendarSheet = true
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = palettePrimary
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row (Upcoming vs Analytics)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Upcoming",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Analytics",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No analytics data available")
                            }
                        }
                    }
                }
                else -> {
                    // Upcoming Tab
                    RevisionsListContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        onRefresh = { viewModel.refresh() },
        isRefreshing = uiState.isLoading,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (uiState.stats != null) 90.dp else 16.dp,
                    // Clears the floating bottom bar. Content still scrolls underneath the pill
                    // because this is a `contentPadding` rather than an outer modifier.
                    bottom = floatingBottomBarContentInset()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
