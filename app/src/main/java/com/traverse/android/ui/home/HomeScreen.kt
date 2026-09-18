package com.traverse.android.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.traverse.android.ui.components.GettingStartedEmptyState
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.HomeUiState
import com.traverse.android.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Home navigation graph. Mirrors the iOS `HomeView` `NavigationStack` destinations.
 *
 * Note: iOS has **no** streak destination — tapping the streak card does nothing,
 * so there is deliberately no `STREAK` route here.
 */
object HomeDestinations {
    const val HOME = "home_main"
    const val ALL_SOLVES = "all_solves"
    const val ALL_ACHIEVEMENTS = "all_achievements"
    const val ACTIVITY = "activity"
    const val MISTAKE_ANALYSIS = "mistake_analysis"

    /** One award shelf, e.g. `awards_section/rings`. */
    const val AWARDS_SECTION = "awards_section/{sectionId}"
    fun awardsSection(sectionId: String) = "awards_section/$sectionId"
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

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
                onRefresh = { viewModel.refresh() },
                onNavigateToSolves = { navController.navigate(HomeDestinations.ALL_SOLVES) },
                onNavigateToAchievements = { navController.navigate(HomeDestinations.ALL_ACHIEVEMENTS) },
                onNavigateToActivity = { navController.navigate(HomeDestinations.ACTIVITY) },
                onNavigateToMistakes = { navController.navigate(HomeDestinations.MISTAKE_ANALYSIS) },
                modifier = modifier
            )
        }

        // iOS: RecentSolvesCard > "View All" -> AllSolvesView(solves:)
        composable(HomeDestinations.ALL_SOLVES) {
            AllSolvesScreen(
                solves = uiState.recentSolves,
                onBack = { navController.popBackStack() }
            )
        }

        // iOS: AchievementStatsCard -> AllAchievementsView()
        composable(HomeDestinations.ALL_ACHIEVEMENTS) {
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

        // iOS: MistakeTagsAnalysisCard -> MistakeTagsDetailView(solves:)
        composable(HomeDestinations.MISTAKE_ANALYSIS) {
            MistakeTagsDetailScreen(
                solves = uiState.recentSolves,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * 1:1 port of the iOS `HomeView` body.
 *
 * Layout order (outer `Column(spacedBy(20.dp))` + `.padding(16.dp)`):
 *  1. A brand-new account (no solves at all) gets `GettingStartedEmptyState` instead of the
 *     feed — every card below is built from solve history, so there would be nothing to draw.
 *  2. `StreakCard` full width
 *  3. Revision card
 *  4. MainStatsCard
 *  5. `Column(spacedBy(16.dp))` — achievements/insights row, difficulty/heatmap row,
 *     mistake tags, solving hours, recent solves, performance metrics, tries distribution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMainContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onNavigateToSolves: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDate = remember {
        // iOS uses `formatter.dateFormat = "EEEE, M"` -> e.g. "Saturday, 9"
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, M"))
    }

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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    // Applied *inside* the scroll container, so the cards still slide underneath the
                    // floating bottom bar while the last one can always be scrolled clear of it.
                    .padding(bottom = floatingBottomBarContentInset()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val errorMessage = uiState.errorMessage
                val userStats = uiState.userStats
                if (errorMessage != null) {
                    ErrorView(message = errorMessage, onRetry = onRefresh)
                } else if (userStats != null && userStats.stats.totalSolves == 0) {
                    // A brand-new account. Every card below is built from solve history, so
                    // without this branch the feed is a blank black screen with a date on it
                    // — which reads as a broken app rather than an empty one.
                    GettingStartedEmptyState(
                        title = "Your feed fills in from your first solve",
                        message = "Traverse reads your practice from the browser and reports it " +
                            "back here. There is nothing to show until then."
                    )
                } else {
                    val solves = uiState.recentSolves
                    val solveStats = uiState.solveStats
                    val achievementStats = uiState.achievementStats

                    // MARK: Streak — full width.
                    //
                    // It used to share a row with the revision score card. On iOS that card
                    // became a full-width training-load tile, so the streak takes the whole
                    // row instead of being squeezed into half of it, and its contents are
                    // centred to suit.
                    if (userStats != null) {
                        StreakCard(
                            streak = userStats.stats.currentStreak,
                            // `longestStreak` is the real "best" figure. The fallback only
                            // applies to a cached payload written before the server started
                            // sending it — `totalStreakDays` is a running total, so it is
                            // wrong here, just not wrong-by-a-lot.
                            maxStreak = userStats.stats.longestStreak
                                ?: userStats.stats.totalStreakDays
                        )

                        // Still the old thinking-orb score card. iOS replaced this with the
                        // full-width revision-load tile; porting that is the next piece.
                        RevisionScoreCard(score = uiState.revisionScore?.score ?: 100)
                    }

                    // MARK: Main Stats Card
                    if (solveStats != null) {
                        MainStatsCard(
                            totalSolves = solveStats.stats.totalSolves,
                            totalXp = solveStats.stats.totalXp,
                            streak = solveStats.stats.totalStreakDays
                        )
                    }

                    // MARK: Charts section
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Achievements and Insights side by side
                        if (achievementStats != null && solves.isNotEmpty()) {
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
                        } else if (achievementStats != null) {
                            AchievementStatsCard(
                                stats = achievementStats.stats,
                                onClick = onNavigateToAchievements
                            )
                        }

                        if (solveStats != null) {
                            // Difficulty and Activity side by side
                            // `IntrinsicSize.Min` + `fillMaxHeight()` keeps the Difficulty card exactly
                            // as tall as the taller Activity heatmap card sitting next to it.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                DifficultyChartCard(
                                    difficulty = solveStats.stats.byDifficulty,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )

                                SolveHeatmapCard(
                                    solves = solves,
                                    frozenDates = uiState.frozenDates,
                                    onClick = onNavigateToActivity,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }

                            // Mistake Tags Analysis full width
                            MistakeTagsAnalysisCard(
                                solves = solves,
                                onViewAll = onNavigateToMistakes
                            )

                            // Best Solving Hours
                            BestSolvingHoursCard(solves = solves)
                        }

                        if (solves.isNotEmpty()) {
                            RecentSolvesCard(
                                solves = solves,
                                onViewAll = onNavigateToSolves
                            )

                            // New Performance Charts
                            PerformanceMetricsCard(solves = solves)

                            TriesDistributionCard(solves = solves)
                        }
                    }
                }
            }
        }
    }
}
