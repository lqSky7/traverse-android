package com.traverse.android.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.traverse.android.viewmodel.AuthViewModel

sealed class AuthRoute(val route: String) {
    data object Login : AuthRoute("login")
    data object Register : AuthRoute("register")
    data object PasswordReset : AuthRoute("password_reset")
}

@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // GitHub sign-in leaves the app entirely. The ViewModel hands us a URL once
    // it has one; opening it and immediately clearing it keeps a recomposition
    // from launching a second tab.
    LaunchedEffect(uiState.githubAuthUrl) {
        val url = uiState.githubAuthUrl ?: return@LaunchedEffect
        openGitHubAuthPage(context, url)
        authViewModel.consumeGitHubAuthUrl()
    }
    
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login.route,
        enterTransition = { 
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            )
        },
        exitTransition = { 
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            )
        },
        popEnterTransition = { 
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            )
        },
        popExitTransition = { 
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            )
        }
    ) {
        composable(route = AuthRoute.Login.route) {
            LoginScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onLogin = { username, password ->
                    authViewModel.login(username, password)
                },
                onNavigateToRegister = {
                    navController.navigate(AuthRoute.Register.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToPasswordReset = {
                    navController.navigate(AuthRoute.PasswordReset.route) {
                        launchSingleTop = true
                    }
                },
                onClearError = authViewModel::clearError,
                isGitHubSignInInProgress = uiState.isGitHubSignInInProgress,
                onGitHubSignIn = authViewModel::startGitHubSignIn
            )
        }
        
        composable(route = AuthRoute.Register.route) {
            RegisterScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onRegister = { username, email, password ->
                    authViewModel.register(username, email, password)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onClearError = authViewModel::clearError
            )
        }
        
        composable(route = AuthRoute.PasswordReset.route) {
            PasswordResetScreen(
                onBack = {
                    navController.popBackStack()
                },
                onComplete = {
                    navController.popBackStack(AuthRoute.Login.route, inclusive = false)
                }
            )
        }
    }
}
