package com.traverse.android.ui.problems

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.traverse.android.ui.components.GettingStartedEmptyState
import com.traverse.android.ui.home.AllSolvesScreen
import com.traverse.android.ui.home.ErrorView
import com.traverse.android.ui.home.MistakeTagsAnalysisCard
import com.traverse.android.ui.home.MistakeTagsDetailScreen
import com.traverse.android.ui.home.RecentSolvesCard
import com.traverse.android.ui.navigation.floatingBottomBarContentInset
import com.traverse.android.ui.theme.RingiftFamily
import com.traverse.android.ui.theme.rememberPalette
import com.traverse.android.viewmodel.ProblemsUiState
import com.traverse.android.viewmodel.ProblemsViewModel

/**
 * The Problems tab: a new home for Recent Solves and Mistake Analysis. 1:1 port of the iOS
 * `ProblemsView` and its `NavigationStack`.
 *
 * Both cards read the full solve payload — AI analysis, mistake tags, attempt history — which is the
 * most expensive thing the API returns. They used to sit on the home feed, which meant every
 * pull-to-refresh paid for them even when the user never scrolled that far. Giving them their own
 * tab lets Home ask for a small page and lets this screen ask for a deep one, on demand.
 *
 * The cards and their detail screens still live in `ui.home` because they read the same solve
 * payload the home charts do and are shared with that navigation graph; the package name is
 * historical, not a statement about which tab owns them.
 */
object ProblemsDestinations {
    const val PROBLEMS = "problems_main"
    const val ALL_SOLVES = "problems_all_solves"
    const val MISTAKE_ANALYSIS = "problems_mistake_analysis"
}

@Composable
fun ProblemsScreen(
    viewModel: ProblemsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ProblemsDestinations.PROBLEMS,
        enterTransition = { slideInHorizontally(tween(300)) { it } },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
    ) {
        composable(ProblemsDestinations.PROBLEMS) {
            ProblemsMainContent(
                uiState = uiState,
                onRefresh = { viewModel.load(forceRefresh = true) },
                onNavigateToSolves = { navController.navigate(ProblemsDestinations.ALL_SOLVES) },
                onNavigateToMistakes = { navController.navigate(ProblemsDestinations.MISTAKE_ANALYSIS) },
                modifier = modifier
            )
        }

        // iOS: RecentSolvesCard > "View All" -> AllSolvesView(solves:)
        composable(ProblemsDestinations.ALL_SOLVES) {
            AllSolvesScreen(
                solves = uiState.solves,
                onBack = { navController.popBackStack() }
            )
        }

        // iOS: MistakeTagsAnalysisCard -> MistakeTagsDetailView(solves:)
        composable(ProblemsDestinations.MISTAKE_ANALYSIS) {
            MistakeTagsDetailScreen(
                solves = uiState.solves,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProblemsMainContent(
    uiState: ProblemsUiState,
    onRefresh: () -> Unit,
    onNavigateToSolves: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberPalette()
    val solves = uiState.solves

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
                        text = "Problems",
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
                    // Applied *inside* the scroll container, so the cards still slide underneath
                    // the floating bottom bar while the last one can always be scrolled clear of it.
                    .padding(bottom = floatingBottomBarContentInset()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val errorMessage = uiState.errorMessage

                when {
                    solves.isNotEmpty() -> {
                        MistakeTagsAnalysisCard(
                            solves = solves,
                            onViewAll = onNavigateToMistakes
                        )

                        RecentSolvesCard(
                            solves = solves,
                            onViewAll = onNavigateToSolves
                        )
                    }

                    uiState.isLoading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.primary)
                    }

                    errorMessage != null -> ErrorView(
                        message = errorMessage,
                        onRetry = onRefresh
                    )

                    else -> GettingStartedEmptyState(
                        title = "No problems yet",
                        message = "Recent solves and recurring mistakes appear here once Traverse " +
                            "has seen you solve something in the browser."
                    )
                }
            }
        }
    }
}
