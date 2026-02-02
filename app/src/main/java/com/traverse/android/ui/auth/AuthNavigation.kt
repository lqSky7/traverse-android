package com.traverse.android.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.traverse.android.viewmodel.AuthViewModel

sealed class AuthRoute(val route: String) {
    data object Login : AuthRoute("login")
    data object Register : AuthRoute("register")
}

@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    
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
                onClearError = authViewModel::clearError
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
    }
}
