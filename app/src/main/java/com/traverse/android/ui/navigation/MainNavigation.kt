package com.traverse.android.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.traverse.android.ui.components.bottombar.AnimatedBottomBar
import com.traverse.android.ui.components.bottombar.BottomBarDefaults
import com.traverse.android.ui.components.bottombar.BottomBarItem
import com.traverse.android.ui.components.bottombar.IconSource
import com.traverse.android.ui.friends.FriendsScreen
import com.traverse.android.ui.home.HomeScreen
import com.traverse.android.ui.problems.ProblemsScreen
import com.traverse.android.ui.revisions.RevisionsScreen
import com.traverse.android.ui.settings.SettingsScreen
import com.traverse.android.ui.theme.rememberPalette
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traverse.android.data.NotificationRouter
import com.traverse.android.ui.components.bottombar.Badge
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.HomeViewModel
import com.traverse.android.viewmodel.NotificationsViewModel
import com.traverse.android.viewmodel.ProblemsViewModel
import com.traverse.android.viewmodel.RevisionsViewModel

sealed class MainRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : MainRoute("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)

    /**
     * Recent Solves and Mistake Analysis. Mirrors the iOS `MainTabView`'s `Problems` tab
     * (`list.bullet.rectangle`), which exists so the home feed can stop paying for the deep solve
     * payload on every pull-to-refresh.
     */
    data object Problems : MainRoute(
        route = "problems",
        label = "Problems",
        selectedIcon = Icons.Filled.FormatListBulleted,
        unselectedIcon = Icons.Outlined.FormatListBulleted
    )

    data object Revisions : MainRoute("revisions", "Revisions", Icons.Filled.History, Icons.Outlined.History)
    data object Friends : MainRoute("friends", "Friends", Icons.Filled.People, Icons.Outlined.People)
    data object Settings : MainRoute("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/** Tab order matches the iOS `MainTabView`: Home, Problems, Revisions, Friends, Settings. */
val tabs = listOf(
    MainRoute.Home,
    MainRoute.Problems,
    MainRoute.Revisions,
    MainRoute.Friends,
    MainRoute.Settings
)

// MARK: Floating bottom bar metrics

/** Height of the pill itself. Must match the `barHeight` handed to [BottomBarDefaults.style]. */
private val BottomBarHeight = 68.dp

/** Horizontal inset that detaches the pill from the screen edges. */
private val BottomBarHorizontalPadding = 16.dp

/** Vertical inset that detaches the pill from the screen edges. */
private val BottomBarVerticalPadding = 10.dp

/** Breathing room kept between the last scrollable item and the top of the pill. */
private val FloatingBottomBarGap = 16.dp

/**
 * Total vertical space the floating bottom bar occupies, measured from the bottom edge of the app
 * content: the pill plus the padding that detaches it from the screen edges. The system
 * navigation-bar inset is *not* part of this — use [floatingBottomBarContentInset] when padding
 * content, which adds it.
 */
val FloatingBottomBarHeight: Dp = BottomBarHeight + BottomBarVerticalPadding * 2

/**
 * Bottom padding for scrollable content that lives behind the floating bottom bar.
 *
 * The bar floats *over* the content rather than reserving a strip for itself, so every screen has
 * to extend its own scroll range past the pill — otherwise the last item would remain hidden
 * behind it no matter how far the user scrolls.
 *
 * Apply this *inside* the scroll container (a `contentPadding` on a `LazyColumn`, or trailing
 * padding / a `Spacer` on a scrolling `Column`) so that the content itself still visibly slides
 * underneath the bar. Applying it as an outer modifier on the scroll container would instead clip
 * the content at the bar's edge and lose the floating effect.
 */
@Composable
fun floatingBottomBarContentInset(): Dp =
    FloatingBottomBarHeight +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        FloatingBottomBarGap

@Composable
fun MainNavigation(
    homeViewModel: HomeViewModel,
    problemsViewModel: ProblemsViewModel,
    revisionsViewModel: RevisionsViewModel,
    friendsViewModel: FriendsViewModel,
    onLogout: () -> Unit,
    notificationsViewModel: NotificationsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // iOS `MainTabView` applies `.tint(paletteManager.selectedPalette.primary)` to the TabView,
    // which colours the selected tab item and its selection indicator.
    val palette = rememberPalette()
    val unreadCount by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()

    // Handle deep link routing from notifications
    val pendingTab by NotificationRouter.pendingTab.collectAsStateWithLifecycle()
    LaunchedEffect(pendingTab) {
        pendingTab?.let { tabStr ->
            val targetRoute = when (tabStr.lowercase()) {
                "home" -> MainRoute.Home.route
                "problems" -> MainRoute.Problems.route
                "revisions" -> MainRoute.Revisions.route
                "friends" -> MainRoute.Friends.route
                "settings" -> MainRoute.Settings.route
                else -> null
            }
            if (targetRoute != null) {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NotificationRouter.pendingTab.value = null
        }
    }

    // Which of the five root destinations is currently on screen. Drives the animated indicator.
    val selectedIndex = tabs
        .indexOfFirst { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true }
        .coerceAtLeast(0)

    // The bar items with dynamic unread badge on the Home tab. Icons mirror the iOS tab bar's
    // filled-when-selected / outlined-when-idle pairing.
    val barItems = remember(unreadCount) {
        tabs.mapIndexed { index, tab ->
            BottomBarItem(
                icon = IconSource.Vector(tab.unselectedIcon),
                selectedIcon = IconSource.Vector(tab.selectedIcon),
                label = tab.label,
                badge = if (index == 0 && unreadCount > 0) Badge.Count(unreadCount) else null
            )
        }
    }

    // The bottom bar deliberately is *not* a `Scaffold` `bottomBar` slot: a slot would reserve a
    // strip at the bottom of the window and push every screen up above it. Instead the navigation
    // content fills the whole window and the bar is drawn on top of it, so lists scroll underneath
    // the pill. Screens compensate with `floatingBottomBarContentInset()` at the end of their own
    // scroll ranges.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = MainRoute.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(MainRoute.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    notificationsViewModel = notificationsViewModel,
                    unreadCount = unreadCount
                )
            }
            composable(MainRoute.Problems.route) {
                ProblemsScreen(viewModel = problemsViewModel)
            }
            composable(MainRoute.Revisions.route) {
                RevisionsScreen(viewModel = revisionsViewModel)
            }
            composable(MainRoute.Friends.route) {
                FriendsScreen(viewModel = friendsViewModel)
            }
            composable(MainRoute.Settings.route) {
                SettingsScreen(
                    onLogout = onLogout,
                    notificationsViewModel = notificationsViewModel
                )
            }
        }

        AnimatedBottomBar(
            items = barItems,
            selectedIndex = selectedIndex,
            onItemSelected = { index ->
                tabs.getOrNull(index)?.let { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Keep the floating pill clear of the system navigation bar, then inset it from the
                // screen edges so it reads as a detached, rounded pill.
                .navigationBarsPadding()
                .padding(
                    horizontal = BottomBarHorizontalPadding,
                    vertical = BottomBarVerticalPadding
                ),
            style = BottomBarDefaults.style(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                // Everything tinted from the live colour palette, exactly like the rest of the app.
                indicatorColor = palette.primary.copy(alpha = 0.22f),
                selectedIconColor = palette.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                badgeColor = palette.colorAt(2),
                badgeContentColor = Color.White,
                containerShapeRadius = 24.dp,
                iconSize = 24.dp,
                barHeight = BottomBarHeight,
                contentPadding = 14.dp
            )
        )
    }
}
