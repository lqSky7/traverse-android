package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.traverse2.ui.components.AnimatedGradientBackground
import com.example.traverse2.ui.components.GlassBottomBar
import com.example.traverse2.ui.components.NavItem
import dev.chrisbanes.haze.HazeState

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val hazeState = remember { HazeState() }
    var selectedNavItem by remember { mutableStateOf(NavItem.HOME) }
    
    AnimatedGradientBackground(
        hazeState = hazeState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content area
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
                        onLogout = onLogout
                    )
                    NavItem.REVISIONS -> RevisionsPlaceholder(hazeState = hazeState)
                    NavItem.FRIENDS -> FriendsPlaceholder(hazeState = hazeState)
                    NavItem.SETTINGS -> SettingsPlaceholder(hazeState = hazeState)
                }
            }
            
            // Bottom navigation bar
            GlassBottomBar(
                selectedItem = selectedNavItem,
                onItemSelected = { selectedNavItem = it },
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
