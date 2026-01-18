package com.example.traverse2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traverse2.data.SessionManager
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.service.StreakService
import com.example.traverse2.ui.screens.LoginScreen
import com.example.traverse2.ui.screens.MainScreen
import com.example.traverse2.ui.theme.TraverseTheme
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startStreakService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraverseTheme {
                TraverseApp(
                    onLoginSuccess = {
                        requestNotificationPermissionAndStartService()
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startStreakService()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startStreakService()
        }
    }

    private fun startStreakService() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getCurrentUser()
                if (response.isSuccessful) {
                    val user = response.body()?.user
                    if (user != null) {
                        val hoursRemaining = try {
                            val solvesResponse = RetrofitClient.api.getMySolves(limit = 1, offset = 0)
                            if (solvesResponse.isSuccessful) {
                                val solves = solvesResponse.body()?.solves
                                if (!solves.isNullOrEmpty()) {
                                    val lastSolveTime = solves.first().solvedAt
                                    calculateHoursRemaining(lastSolveTime)
                                } else {
                                    24
                                }
                            } else {
                                24
                            }
                        } catch (e: Exception) {
                            24
                        }

                        StreakService.startService(
                            context = this@MainActivity,
                            streakCount = user.currentStreak,
                            nextDeadlineHours = hoursRemaining
                        )
                    } else {
                        StreakService.startService(
                            context = this@MainActivity,
                            streakCount = 0,
                            nextDeadlineHours = 24
                        )
                    }
                } else {
                    StreakService.startService(
                        context = this@MainActivity,
                        streakCount = 0,
                        nextDeadlineHours = 24
                    )
                }
            } catch (e: Exception) {
                StreakService.startService(
                    context = this@MainActivity,
                    streakCount = 0,
                    nextDeadlineHours = 24
                )
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun calculateHoursRemaining(lastSolveTimeStr: String): Int {
        return try {
            val now = ZonedDateTime.now()
            val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val hoursUntilMidnight = ChronoUnit.HOURS.between(now, endOfDay).toInt()
            hoursUntilMidnight.coerceIn(1, 24)
        } catch (e: Exception) {
            24
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    object LoggedIn : AuthState()
}

@Composable
fun TraverseApp(onLoginSuccess: () -> Unit = {}) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val navController = rememberNavController()

    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    var hasNavigated by remember { mutableStateOf(false) }

    // Check auth state on launch
    LaunchedEffect(Unit) {
        val isLoggedIn = sessionManager.isLoggedInSync()
        if (isLoggedIn) {
            // Verify session is still valid with backend
            try {
                val response = RetrofitClient.api.getCurrentUser()
                if (response.isSuccessful && response.body() != null) {
                    authState = AuthState.LoggedIn
                } else {
                    // Session expired - clear it
                    sessionManager.clearSession()
                    RetrofitClient.clearCookies()
                    authState = AuthState.LoggedOut
                }
            } catch (e: Exception) {
                // Network error - assume still logged in (offline mode)
                authState = AuthState.LoggedIn
            }
        } else {
            authState = AuthState.LoggedOut
        }
    }

    // Handle navigation based on auth state
    LaunchedEffect(authState) {
        if (authState != AuthState.Loading && !hasNavigated) {
            hasNavigated = true
            when (authState) {
                AuthState.LoggedIn -> {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                    onLoginSuccess()
                }
                AuthState.LoggedOut -> {
                    // Already at login, do nothing
                }
                AuthState.Loading -> { }
            }
        }
    }

    // Show loading while checking auth
    if (authState == AuthState.Loading) {
        val isDark = TraverseTheme.glassColors.isDark
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color(0xFFFDF2F8)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TraverseTheme.glassColors.textPrimary)
        }
        return
    }

    val startDestination = if (authState == AuthState.LoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    onLoginSuccess()
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            MainScreen(
                onLogout = {
                    // Navigate first, then the logout happens in SettingsScreen
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
