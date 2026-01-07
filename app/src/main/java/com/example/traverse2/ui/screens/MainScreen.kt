package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.traverse2.ui.components.AnimatedGradientBackground
import com.example.traverse2.ui.components.GlassBottomBar
import com.example.traverse2.ui.components.NavItem
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState

// Sub-screens that can be navigated to from main tabs
sealed class SubScreen {
    object None : SubScreen()
    object Problems : SubScreen()
    object Streak : SubScreen()
    data class FriendProfile(val friend: Friend) : SubScreen()
}

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val hazeState = remember { HazeState() }
    var selectedNavItem by remember { mutableStateOf(NavItem.HOME) }
    var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
    
    AnimatedGradientBackground(
        hazeState = hazeState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Handle sub-screen navigation
            AnimatedContent(
                targetState = currentSubScreen,
                transitionSpec = {
                    if (targetState != SubScreen.None) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "subScreenContent"
            ) { subScreen ->
                when (subScreen) {
                    SubScreen.Problems -> ProblemsScreen(
                        hazeState = hazeState,
                        onBack = { currentSubScreen = SubScreen.None }
                    )
                    SubScreen.Streak -> StreakScreen(
                        hazeState = hazeState,
                        onBack = { currentSubScreen = SubScreen.None }
                    )
                    is SubScreen.FriendProfile -> FriendProfileScreen(
                        friend = subScreen.friend,
                        hazeState = hazeState,
                        onBack = { currentSubScreen = SubScreen.None }
                    )
                    SubScreen.None -> {
                        // Main tab content
                        AnimatedContent(
                            targetState = selectedNavItem,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screenContent",
                            modifier = Modifier.fillMaxSize()
                        ) { navItem ->
                            when (navItem) {
                                NavItem.HOME -> HomeContent(
                                    hazeState = hazeState,
                                    onLogout = onLogout,
                                    onNavigateToProblems = { currentSubScreen = SubScreen.Problems },
                                    onNavigateToStreak = { currentSubScreen = SubScreen.Streak }
                                )
                                NavItem.REVISIONS -> RevisionsScreen(hazeState = hazeState)
                                NavItem.FRIENDS -> FriendsScreen(
                                    hazeState = hazeState,
                                    onFriendClick = { friend -> 
                                        currentSubScreen = SubScreen.FriendProfile(friend)
                                    }
                                )
                                NavItem.SETTINGS -> SettingsScreen(
                                    hazeState = hazeState,
                                    onLogout = onLogout
                                )
                            }
                        }
                    }
                }
            }
            
            // Fading blur overlay for bottom bar area - only show when not in sub-screen
            AnimatedVisibility(
                visible = currentSubScreen == SubScreen.None,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomBarBlurOverlay()
            }
            
            // Bottom navigation bar - only show when not in sub-screen
            AnimatedVisibility(
                visible = currentSubScreen == SubScreen.None,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                GlassBottomBar(
                    selectedItem = selectedNavItem,
                    onItemSelected = { selectedNavItem = it },
                    hazeState = hazeState
                )
            }
        }
    }
}

@Composable
private fun BottomBarBlurOverlay() {
    val glassColors = TraverseTheme.glassColors
    
    // Create a vertical gradient that fades from transparent at top to frosted at bottom
    val overlayGradient = if (glassColors.isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.3f),
                Color.Black.copy(alpha = 0.6f),
                Color.Black.copy(alpha = 0.85f)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.4f),
                Color.White.copy(alpha = 0.7f),
                Color.White.copy(alpha = 0.9f)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(overlayGradient)
    )
}
