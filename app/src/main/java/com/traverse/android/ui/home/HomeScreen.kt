package com.traverse.android.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                onBack = { navController.popBackStack() }
            )
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
 * Layout order (outer `VStack(spacing: 20)` + `.padding()`):
 *  1. `HStack(spacing: 12)` — StreakCard + RevisionScoreCard
 *  2. MainStatsCard
 *  3. `VStack(spacing: 16)` — achievements/insights row, difficulty/heatmap row,
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val errorMessage = uiState.errorMessage
                if (errorMessage != null) {
                    ErrorView(message = errorMessage, onRetry = onRefresh)
                } else {
                    val solves = uiState.recentSolves
                    val userStats = uiState.userStats
                    val solveStats = uiState.solveStats
                    val achievementStats = uiState.achievementStats

                    // MARK: Top row — Streak Card & Revision Score Card side by side
                    if (userStats != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StreakCard(
                                streak = userStats.stats.currentStreak,
                                maxStreak = userStats.stats.totalStreakDays,
                                modifier = Modifier.weight(1f)
                            )

                            RevisionScoreCard(
                                score = uiState.revisionScore?.score ?: 100,
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                DifficultyChartCard(
                                    difficulty = solveStats.stats.byDifficulty,
                                    modifier = Modifier.weight(1f)
                                )

                                SolveHeatmapCard(
                                    solves = solves,
                                    frozenDates = uiState.frozenDates,
                                    onClick = onNavigateToActivity,
                                    modifier = Modifier.weight(1f)
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
