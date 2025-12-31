package com.example.traverse2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traverse2.ui.screens.LoginScreen
import com.example.traverse2.ui.screens.MainScreen
import com.example.traverse2.ui.theme.TraverseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraverseTheme {
                TraverseApp()
            }
        }
    }
}

@Composable
fun TraverseApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "login",
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
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            MainScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TraverseAppPreview() {
    TraverseTheme {
        TraverseApp()
    }
}