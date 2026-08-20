package com.traverse.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.traverse.android.data.CacheManager
import com.traverse.android.data.DataManager
import com.traverse.android.ui.auth.AuthNavigation
import com.traverse.android.ui.auth.OnboardingFlowDialog
import com.traverse.android.ui.components.AchievementToastManager
import com.traverse.android.ui.components.AchievementToastOverlayContainer
import com.traverse.android.ui.navigation.MainNavigation
import com.traverse.android.ui.theme.TraverseTheme
import com.traverse.android.viewmodel.AuthViewModel
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.HomeViewModel
import com.traverse.android.viewmodel.RevisionsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraverseTheme {
                val authViewModel: AuthViewModel = viewModel()
                val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val toastManager = remember { AchievementToastManager.getInstance(applicationContext) }
                var showOnboardingDialog by remember { mutableStateOf(false) }
                
                // Sync updates and check onboarding on app launch / auth
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        toastManager.syncAppOpenUpdates()
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        // Show main app only when authenticated AND data is loaded
                        uiState.isAuthenticated && uiState.isDataLoaded -> {
                            val homeViewModel: HomeViewModel = viewModel()
                            val revisionsViewModel: RevisionsViewModel = viewModel()
                            val friendsViewModel: FriendsViewModel = viewModel()
                            
                            MainNavigation(
                                homeViewModel = homeViewModel,
                                revisionsViewModel = revisionsViewModel,
                                friendsViewModel = friendsViewModel,
                                onLogout = { authViewModel.logout() }
                            )
                        }
                        // Show loading screen while fetching data after login
                        uiState.isAuthenticated && !uiState.isDataLoaded -> {
                            LoadingScreen()
                        }
                        // Show auth screens
                        else -> {
                            AuthNavigation(authViewModel = authViewModel)
                        }
                    }

                    // Global in-app HUD notification toasts overlay
                    AchievementToastOverlayContainer()

                    // First-time onboarding dialog if requested
                    if (showOnboardingDialog) {
                        OnboardingFlowDialog(
                            onDismiss = { showOnboardingDialog = false },
                            onCompleted = { showOnboardingDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFB8D4E3),
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading your data...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

