package com.traverse.android.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.traverse.android.ui.revisions.RevisionsScreen
import com.traverse.android.ui.settings.SettingsScreen
import com.traverse.android.ui.theme.rememberPalette
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.HomeViewModel
import com.traverse.android.viewmodel.RevisionsViewModel

sealed class MainRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : MainRoute("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Revisions : MainRoute("revisions", "Revisions", Icons.Filled.History, Icons.Outlined.History)
    data object Friends : MainRoute("friends", "Friends", Icons.Filled.People, Icons.Outlined.People)
    data object Settings : MainRoute("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val tabs = listOf(MainRoute.Home, MainRoute.Revisions, MainRoute.Friends, MainRoute.Settings)

@Composable
fun MainNavigation(
    homeViewModel: HomeViewModel,
    revisionsViewModel: RevisionsViewModel,
    friendsViewModel: FriendsViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // iOS `MainTabView` applies `.tint(paletteManager.selectedPalette.primary)` to the TabView,
    // which colours the selected tab item and its selection indicator.
    val palette = rememberPalette()

    // Which of the four root destinations is currently on screen. Drives the animated indicator.
    val selectedIndex = tabs
        .indexOfFirst { tab -> currentDestination?.hierarchy?.any { it.route == tab.route } == true }
        .coerceAtLeast(0)

    // The bar items never change, so build them once. Icons mirror the iOS tab bar's
    // filled-when-selected / outlined-when-idle pairing.
    val barItems = remember {
        tabs.map { tab ->
            BottomBarItem(
                icon = IconSource.Vector(tab.unselectedIcon),
                selectedIcon = IconSource.Vector(tab.selectedIcon),
                label = tab.label
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // Every tab screen owns its own `Scaffold` + `TopAppBar`, which already consumes the
        // status-bar inset. Zeroing the outer content insets here stops that inset being applied a
        // second time (it used to leave an opaque band directly above the navigation bar).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                    // Keep the floating bar clear of the system navigation bar, then inset it from
                    // the screen edges so it reads as a detached, rounded pill.
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    barHeight = 68.dp,
                    contentPadding = 14.dp
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(MainRoute.Home.route) {
                HomeScreen(viewModel = homeViewModel)
            }
            composable(MainRoute.Revisions.route) {
                RevisionsScreen(viewModel = revisionsViewModel)
            }
            composable(MainRoute.Friends.route) {
                FriendsScreen(viewModel = friendsViewModel)
            }
            composable(MainRoute.Settings.route) {
                SettingsScreen(onLogout = onLogout)
            }
        }
    }
}
