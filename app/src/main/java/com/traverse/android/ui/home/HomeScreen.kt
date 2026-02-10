package com.traverse.android.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.traverse.android.data.Solve
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HomeDestinations {
    const val HOME = "home_main"
    const val ALL_SOLVES = "all_solves"
    const val ALL_ACHIEVEMENTS = "all_achievements"
    const val STREAK = "streak"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    friendsViewModel: com.traverse.android.viewmodel.FriendsViewModel? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val friendsUiState = friendsViewModel?.uiState?.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val isDarkTheme = isSystemInDarkTheme()

    NavHost(
        navController = navController,
        startDestination = HomeDestinations.HOME,
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }) {
        composable(HomeDestinations.HOME) {
            HomeMainContent(
                uiState = uiState,
                isDarkTheme = isDarkTheme,
                onRefresh = { viewModel.refresh() },
                onNavigateToSolves = { navController.navigate(HomeDestinations.ALL_SOLVES) },
                onNavigateToAchievements = { navController.navigate(HomeDestinations.ALL_ACHIEVEMENTS) },
                onNavigateToStreak = { navController.navigate(HomeDestinations.STREAK) },
                modifier = modifier
            )
        }

        composable(HomeDestinations.ALL_SOLVES) {
            AllSolvesScreen(
                solves = uiState.recentSolves, onBack = { navController.popBackStack() })
        }

        composable(HomeDestinations.ALL_ACHIEVEMENTS) {
            AllAchievementsScreen(
                achievements = uiState.allAchievements,
                stats = uiState.achievementStats?.stats,
                onBack = { navController.popBackStack() })
        }

        composable(HomeDestinations.STREAK) {
            StreakScreen(
                currentStreak = uiState.userStats?.stats?.currentStreak ?: 0,
                totalStreakDays = uiState.solveStats?.stats?.totalStreakDays ?: 0,
                solvedToday = hasSolvedToday(uiState.recentSolves),
                solveDates = uiState.recentSolves.map { it.solvedAt.take(10) }.distinct(),
                frozenDates = uiState.frozenDates,
                friendStreaks = friendsUiState?.value?.friendStreaks ?: emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMainContent(
    uiState: com.traverse.android.viewmodel.HomeUiState,
    isDarkTheme: Boolean,
    onRefresh: () -> Unit,
    onNavigateToSolves: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToStreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDate = remember {
        val formatter = DateTimeFormatter.ofPattern("EEEE d")
        LocalDate.now().format(formatter)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = currentDate, style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = RingiftFamily
                    )
                )
            })
    }) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isLoading && uiState.userStats == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null && uiState.userStats == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Streak Card (inverted colors) - clickable to navigate to Streak screen
                    uiState.userStats?.let { userStats ->
                        StreakCard(
                            streak = userStats.stats.currentStreak,
                            solvedToday = hasSolvedToday(uiState.recentSolves),
                            isDarkTheme = isDarkTheme,
                            onClick = onNavigateToStreak
                        )
                    }

                    // Main Stats Card
                    uiState.solveStats?.let { solveStats ->
                        MainStatsCard(
                            totalSolves = solveStats.stats.totalSolves,
                            totalXp = solveStats.stats.totalXp,
                            streak = solveStats.stats.totalStreakDays
                        )
                    }

                    // Difficulty + Achievements side by side (equal heights)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.solveStats?.let { solveStats ->
                            DifficultyChartCard(
                                difficulty = solveStats.stats.byDifficulty,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        uiState.achievementStats?.let { achievementStats ->
                            AchievementCard(
                                stats = achievementStats.stats,
                                onClick = onNavigateToAchievements,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // Productivity Insights (bar graph)
                    if (uiState.recentSolves.isNotEmpty()) {
                        ProductivityInsightsCard(solves = uiState.recentSolves)
                    }

                    // Performance Charts (side by side)
                    if (uiState.recentSolves.isNotEmpty()) {
                        TimePerformanceCard(solves = uiState.recentSolves)
                    }

                    // Mistake Tags
                    if (uiState.recentSolves.isNotEmpty()) {
                        MistakeTagsCard(solves = uiState.recentSolves)
                    }

                    // Recent Solves (clickable)
                    if (uiState.recentSolves.isNotEmpty()) {
                        RecentSolvesCard(
                            solves = uiState.recentSolves, onClick = onNavigateToSolves
                        )
                    }

                    // Bottom spacing for floating tab bar
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

private fun hasSolvedToday(solves: List<Solve>): Boolean {
    if (solves.isEmpty()) return false
    val today = LocalDate.now()
    return solves.any { solve ->
        try {
            LocalDate.parse(solve.solvedAt.substring(0, 10)) == today
        } catch (e: Exception) {
            false
        }
    }
}
