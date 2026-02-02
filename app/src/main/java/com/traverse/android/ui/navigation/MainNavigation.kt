package com.traverse.android.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.traverse.android.ui.friends.FriendsScreen
import com.traverse.android.ui.home.HomeScreen
import com.traverse.android.ui.revisions.RevisionsScreen
import com.traverse.android.ui.settings.SettingsScreen
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
    val currentRoute = navBackStackEntry?.destination?.route
    
    Box(modifier = modifier.fillMaxSize()) {
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
        
        // Floating Tab Bar
        FloatingTabBar(
            tabs = tabs,
            currentRoute = currentRoute,
            onTabSelected = { route ->
                navController.navigate(route.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun FloatingTabBar(
    tabs: List<MainRoute>,
    currentRoute: String?,
    onTabSelected: (MainRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
