package com.traverse.android.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.traverse.android.data.RevisionLoadBreakdown
import com.traverse.android.ui.components.GettingStartedEmptyState
import com.traverse.android.ui.components.SweepGate
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.HomeUiState
import com.traverse.android.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.traverse.android.viewmodel.NotificationsViewModel

/**
 * Home navigation graph. Mirrors the iOS `HomeView` `NavigationStack` destinations.
 */
object HomeDestinations {
    const val HOME = "home_main"
    const val ALL_ACHIEVEMENTS = "all_achievements"
    const val ACTIVITY = "activity"
    const val REVISION_LOAD = "revision_load"

    /** `metric_detail/time` or `metric_detail/attempts`. */
    const val METRIC_DETAIL = "metric_detail/{kind}"
    fun metricDetail(kind: MetricKind) = "metric_detail/${kind.name.lowercase()}"

    /** One award shelf, e.g. `awards_section/rings`. */
    const val AWARDS_SECTION = "awards_section/{sectionId}"
    fun awardsSection(sectionId: String) = "awards_section/$sectionId"
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    notificationsViewModel: NotificationsViewModel? = null,
    unreadCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var showNotificationsSheet by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = HomeDestinations.HOME,
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
    ) {
        composable(HomeDestinations.HOME) {
            HomeMainContent(
                uiState = uiState,
                viewModel = viewModel,
                unreadCount = unreadCount,
                onOpenNotifications = {
                    if (onOpenNotifications != null) {
                        onOpenNotifications()
                    } else {
                        showNotificationsSheet = true
                    }
                },
                onRefresh = { viewModel.refresh() },
                onNavigateToAchievements = { navController.navigate(HomeDestinations.ALL_ACHIEVEMENTS) },
                onNavigateToActivity = { navController.navigate(HomeDestinations.ACTIVITY) },
                onNavigateToRevisionLoad = { navController.navigate(HomeDestinations.REVISION_LOAD) },
                onNavigateToMetric = { kind -> navController.navigate(HomeDestinations.metricDetail(kind)) },
                modifier = modifier
            )
        }

        // iOS: RevisionLoadCard -> RevisionLoadDetailView(breakdown:revisionScore:)
        composable(HomeDestinations.REVISION_LOAD) {
            RevisionLoadDetailScreen(
                breakdown = uiState.revisionLoad ?: RevisionLoadBreakdown.EMPTY,
                revisionScore = uiState.revisionScore?.score,
                onBack = { navController.popBackStack() }
            )
        }

        // iOS: TimeAnalysisCard / AttemptsAnalysisCard -> MetricDetailView(kind:solves:)
        composable(
            route = HomeDestinations.METRIC_DETAIL,
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) { entry ->
            val kind = entry.arguments?.getString("kind")
                ?.let { raw -> MetricKind.entries.firstOrNull { it.name.equals(raw, true) } }

            if (kind != null) {
                MetricDetailScreen(
                    kind = kind,
                    solves = uiState.recentSolves,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // iOS: AchievementStatsCard -> AllAchievementsView()
        composable(HomeDestinations.ALL_ACHIEVEMENTS) {
            LaunchedEffect(Unit) { viewModel.loadAwards() }
            AllAchievementsScreen(
                achievements = uiState.allAchievements,
                stats = uiState.achievementStats?.stats,
                sections = uiState.awardSections,
                featured = uiState.featuredAward,
                onBack = { navController.popBackStack() },
                onOpenSection = { sectionId ->
                    navController.navigate(HomeDestinations.awardsSection(sectionId))
                }
            )
        }

        // iOS: an award shelf card -> AwardsSectionView(section:)
        composable(
            route = HomeDestinations.AWARDS_SECTION,
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType })
        ) { entry ->
            val sectionId = entry.arguments?.getString("sectionId")
            val section = uiState.awardSections.find { it.id == sectionId }
                ?: fallbackSections(uiState.allAchievements).find { it.id == sectionId }

            if (section != null) {
                AwardsSectionScreen(
                    section = section,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // iOS: SolveHeatmapCard -> ActivityDetailView(solves:frozenDates:)
        composable(HomeDestinations.ACTIVITY) {
            ActivityDetailScreen(
                solves = uiState.recentSolves,
                frozenDates = uiState.frozenDates,
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showNotificationsSheet && notificationsViewModel != null) {
        NotificationsSheet(
            viewModel = notificationsViewModel,
            onDismiss = { showNotificationsSheet = false }
        )
    }
}

/**
 * 1:1 port of the iOS `HomeView` body.
 *
 * Cards are separate keyed LazyColumn items so offscreen charts do not stay composed.
 *  1. A brand-new account (no solves at all) gets `GettingStartedEmptyState` instead of the feed —
 *     every card below is built from solve history, so there would be nothing to draw.
 *  2. `StreakCard` full width
 *  3. `RevisionLoadCard` full width, tapping through to its trend screen
 *  4. `MainStatsCard`
 *  5. Achievements/insights row, activity heatmap, solving hours, and
 *     the time/attempts step-count pair.
 *
 * Three cards that used to be here are deliberately gone, matching iOS: the easy/mid/hard
 * difficulty card (folded into the attempts breakdown on its detail screen), and Recent Solves and
 * Mistake Analysis (moved to the Problems tab, which fetches the deep payload they need).
 * `PerformanceMetricsCard` and `TriesDistributionCard` were removed outright — they restated the
 * same attempts and timing data the step-count pair now covers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    unreadCount: Int,
    onOpenNotifications: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToRevisionLoad: () -> Unit,
    onNavigateToMetric: (MetricKind) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDate = remember {
        // iOS uses `formatter.dateFormat = "EEEE, M"` -> e.g. "Saturday, 9"
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, M"))
    }
    var showRingGoalsSheet by remember { mutableStateOf(false) }

    // Once-per-visit latch for the feed's chroma sweeps.
    //
    // Held at screen level, not on the card: the card is a `LazyColumn` item and is disposed when
    // it scrolls out of the keep-alive window, so a gate held there would reset on scroll and the
    // sweep would replay every time the user scrolled back to the top.
    //
    // Reset comes free here. Compose Navigation only composes the current destination, so this
    // screen is disposed on navigate-away and `remember` hands back a fresh gate on return — which
    // is exactly "plays again when you come back from another view". (iOS has to do this
    // explicitly; see `ChromaSweepGate`.)
    val sweepGate = remember { SweepGate() }

    Scaffold(
        containerColor = Color.Black,
        // The bottom bar floats over the content, so the screen is edge-to-edge: the root
        // navigation no longer reserves a strip for the bar, and the status-bar inset is consumed
        // by the `TopAppBar` below rather than by this `Scaffold`.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = RingiftFamily
                        )
                    )
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(if (unreadCount > 9) "9+" else "$unreadCount")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onOpenNotifications) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val userStats = uiState.userStats
            val solves = uiState.recentSolves
            val solveStats = uiState.solveStats
            val achievementStats = uiState.achievementStats
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 16.dp + floatingBottomBarContentInset()
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                uiState.errorMessage?.let { message ->
                    item(key = "error") { ErrorView(message = message, onRetry = onRefresh) }
                }
                if (userStats != null && userStats.stats.totalSolves == 0) {
                    item(key = "getting_started") {
                        GettingStartedEmptyState(
                            title = "Your feed fills in from your first solve",
                            message = "Traverse reads your practice from the browser and reports it " +
                                "back here. There is nothing to show until then."
                        )
                    }
                } else {
                    if (userStats != null) {
                        item(key = "streak") {
                            StreakCard(
                                streak = userStats.stats.currentStreak,
                                maxStreak = userStats.stats.longestStreak ?: userStats.stats.totalStreakDays,
                                rings = uiState.rings,
                                // The week strip reads solve history and freeze days. Both are
                                // already on the ui state for the heatmap, so this is plumbing
                                // rather than a new fetch.
                                solves = uiState.recentSolves,
                                frozenDates = uiState.frozenDates,
                                sweepGate = sweepGate,
                                onRingsClick = { showRingGoalsSheet = true }
                            )
                        }
                        item(key = "revision_load") {
                            RevisionLoadCard(
                                breakdown = uiState.revisionLoad,
                                revisionScore = uiState.revisionScore?.score,
                                onClick = onNavigateToRevisionLoad
                            )
                        }
                    }
                    if (solveStats != null) {
                        item(key = "stats") {
                            MainStatsCard(
                                totalSolves = solveStats.stats.totalSolves,
                                totalXp = solveStats.stats.totalXp,
                                streak = solveStats.stats.totalStreakDays
                            )
                        }
                    }
                    if (achievementStats != null) {
                        item(key = "awards_activity") {
                            if (solves.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    AchievementStatsCard(
                                        stats = achievementStats.stats,
                                        onClick = onNavigateToAchievements,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ProductivityInsightsCard(
                                        solves = solves,
                                        completedRevisions = uiState.completedRevisions,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                AchievementStatsCard(
                                    stats = achievementStats.stats,
                                    onClick = onNavigateToAchievements
                                )
                            }
                        }
                    }
                    if (solves.isNotEmpty()) {
                        item(key = "heatmap") {
                            SolveHeatmapCard(
                                solves = solves,
                                frozenDates = uiState.frozenDates,
                                onClick = onNavigateToActivity
                            )
                        }
                        item(key = "hours") { BestSolvingHoursCard(solves = solves) }
                        item(key = "metrics") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                TimeAnalysisCard(
                                    solves = solves,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToMetric(MetricKind.TIME) }
                                )
                                AttemptsAnalysisCard(
                                    solves = solves,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToMetric(MetricKind.ATTEMPTS) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRingGoalsSheet) {
        RingGoalsSheet(
            rings = uiState.rings,
            onDismiss = { showRingGoalsSheet = false },
            onSaveGoals = { solveGoal, revisionGoal ->
                viewModel.updateRingGoals(solveGoal, revisionGoal)
            }
        )
    }
}
