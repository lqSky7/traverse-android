package com.traverse.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.traverse.android.data.GITHUB_REDIRECT_URI
import com.traverse.android.data.NotificationRouter
import com.traverse.android.data.OnboardingState
import com.traverse.android.data.PushRegistrationManager
import com.traverse.android.push.TraverseMessagingService
import com.traverse.android.ui.auth.AuthNavigation
import com.traverse.android.ui.auth.OnboardingFlowDialog
import com.traverse.android.ui.components.AchievementToastManager
import com.traverse.android.ui.components.AchievementToastOverlayContainer
import com.traverse.android.ui.navigation.MainNavigation
import com.traverse.android.ui.onboarding.SetupStepsScreen
import com.traverse.android.ui.theme.ColorPaletteManager
import com.traverse.android.ui.theme.TraverseTheme
import com.traverse.android.viewmodel.AuthViewModel
import com.traverse.android.viewmodel.FriendsViewModel
import com.traverse.android.viewmodel.HomeViewModel
import com.traverse.android.viewmodel.NotificationsViewModel
import com.traverse.android.viewmodel.ProblemsViewModel
import com.traverse.android.viewmodel.RevisionsViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()
        handleNotificationIntent(intent)
        // A cold start from the OAuth deep link lands here, before the first
        // composition — the callback has to be kicked off even though there is
        // no UI to observe it yet.
        handleAuthDeepLink(intent)

        // Restore the persisted colour palette before the first composition.
        ColorPaletteManager.init(applicationContext)

        setContent {
            TraverseTheme {
                val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
                val toastManager = remember { AchievementToastManager.getInstance(applicationContext) }
                var showOnboardingDialog by remember { mutableStateOf(false) }

                // Read once, then held in composition state: `markSetupStepsSeen`
                // writes synchronously enough for the next launch but the screen
                // has to disappear in *this* one.
                val onboardingState = remember { OnboardingState.getInstance(applicationContext) }
                var hasSeenSetupSteps by remember {
                    mutableStateOf(onboardingState.hasSeenSetupSteps())
                }

                // Sync updates and check onboarding on app launch / auth
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        toastManager.syncAppOpenUpdates()
                        PushRegistrationManager.getInstance(applicationContext).register()
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        // Show main app only when authenticated AND data is loaded
                        uiState.isAuthenticated && uiState.isDataLoaded -> {
                            val homeViewModel: HomeViewModel = viewModel()
                            val problemsViewModel: ProblemsViewModel = viewModel()
                            val revisionsViewModel: RevisionsViewModel = viewModel()
                            val friendsViewModel: FriendsViewModel = viewModel()
                            val notificationsViewModel: NotificationsViewModel = viewModel()
                            
                            MainNavigation(
                                homeViewModel = homeViewModel,
                                problemsViewModel = problemsViewModel,
                                revisionsViewModel = revisionsViewModel,
                                friendsViewModel = friendsViewModel,
                                onLogout = { authViewModel.logout() },
                                notificationsViewModel = notificationsViewModel
                            )
                        }
                        // Show loading screen while fetching data after login
                        uiState.isAuthenticated && !uiState.isDataLoaded -> {
                            LoadingScreen()
                        }
                        // First boot: the setup walkthrough, before the login
                        // screen. Placed after the authenticated branches so a
                        // restored session never sees it.
                        !hasSeenSetupSteps -> {
                            SetupStepsScreen(
                                onFinish = {
                                    onboardingState.markSetupStepsSeen()
                                    hasSeenSetupSteps = true
                                }
                            )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
        handleAuthDeepLink(intent)
    }

    /**
     * Called on the way back from the browser. If the reader abandoned the
     * GitHub flow the deep link never arrives, and the pending flag would
     * otherwise leave the sign-in button disabled for the rest of the session.
     * When a code *did* arrive this runs after [onNewIntent] has already
     * cleared the flag, so it does nothing.
     */
    override fun onResume() {
        super.onResume()
        authViewModel.onReturnedToForeground()
    }

    /**
     * `traverse-android://auth?code=…` — the tail of the GitHub round-trip.
     *
     * The scheme and host are compared against [GITHUB_REDIRECT_URI] rather than
     * spelled out again, so this and the `android:scheme` / `android:host` pair
     * in AndroidManifest.xml cannot drift apart silently. They are checked at
     * all because this activity also carries the launcher intent filter, so
     * `intent.data` is null on an ordinary launch.
     */
    private fun handleAuthDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val expected = Uri.parse(GITHUB_REDIRECT_URI)
        if (data.scheme != expected.scheme || data.host != expected.host) return

        val code = data.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            authViewModel.handleGitHubCallback(code)
            return
        }

        val error = data.getQueryParameter("error_description")
            ?: data.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            authViewModel.handleGitHubError(error)
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val tab = intent.getStringExtra("tab")
        val url = intent.getStringExtra("url")
        val type = intent.getStringExtra("type")
        if (!tab.isNullOrBlank() || !url.isNullOrBlank() || !type.isNullOrBlank()) {
            NotificationRouter.routeTo(tab = tab, url = url, type = type)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TraverseMessagingService.CHANNEL_ID,
                TraverseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Traverse notifications and reminders"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
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

