package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.traverse2.ui.components.AnimatedGradientBackground
import com.example.traverse2.ui.components.GlassButton
import com.example.traverse2.ui.components.GlassCard
import com.example.traverse2.ui.components.GlassTextField
import com.example.traverse2.ui.components.GlassTextButton
import com.example.traverse2.ui.theme.ThemeState
import com.example.traverse2.ui.theme.TraverseTheme
import com.example.traverse2.viewmodel.AuthViewModel
import com.example.traverse2.viewmodel.AuthUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

enum class AuthScreen {
    WELCOME, LOGIN, REGISTER
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val hazeState = remember { HazeState() }
    val glassColors = TraverseTheme.glassColors
    val focusManager = LocalFocusManager.current
    
    // Screen navigation state
    var currentScreen by remember { mutableStateOf(AuthScreen.WELCOME) }
    
    // Form fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // ViewModel state
    val uiState by viewModel.uiState.collectAsState()
    
    // Shake animation for errors
    val shakeOffset = remember { Animatable(0f) }
    
    // Trigger shake on error
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 10f else -10f,
                    animationSpec = tween(durationMillis = 50)
                )
            }
            shakeOffset.animateTo(0f)
        }
    }
    
    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
        }
    }
    
    AnimatedGradientBackground(
        hazeState = hazeState
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                when {
                    targetState == AuthScreen.WELCOME -> {
                        (fadeIn(animationSpec = tween(300)) + 
                            slideInHorizontally { -it }) togetherWith
                        (fadeOut(animationSpec = tween(300)) + 
                            slideOutHorizontally { it })
                    }
                    initialState == AuthScreen.WELCOME -> {
                        (fadeIn(animationSpec = tween(300)) + 
                            slideInHorizontally { it }) togetherWith
                        (fadeOut(animationSpec = tween(300)) + 
                            slideOutHorizontally { -it })
                    }
                    else -> {
                        (fadeIn(animationSpec = tween(300)) + 
                            slideInVertically { if (targetState == AuthScreen.REGISTER) it else -it }) togetherWith
                        (fadeOut(animationSpec = tween(300)) + 
                            slideOutVertically { if (targetState == AuthScreen.REGISTER) -it else it })
                    }
                }
            },
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                AuthScreen.WELCOME -> WelcomeContent(
                    hazeState = hazeState,
                    onCreateAccount = { currentScreen = AuthScreen.REGISTER },
                    onLogin = { currentScreen = AuthScreen.LOGIN }
                )
                AuthScreen.LOGIN -> LoginContent(
                    hazeState = hazeState,
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    uiState = uiState,
                    shakeOffset = shakeOffset.value,
                    onLogin = { viewModel.login(email, password) },
                    onBack = { 
                        viewModel.clearError()
                        currentScreen = AuthScreen.WELCOME 
                    },
                    onForgotPassword = { /* TODO */ },
                    onCreateAccount = {
                        viewModel.clearError()
                        currentScreen = AuthScreen.REGISTER
                    }
                )
                AuthScreen.REGISTER -> RegisterContent(
                    hazeState = hazeState,
                    fullName = fullName,
                    onFullNameChange = { fullName = it },
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    uiState = uiState,
                    shakeOffset = shakeOffset.value,
                    onRegister = { viewModel.register(fullName, email, password) },
                    onBack = {
                        viewModel.clearError()
                        currentScreen = AuthScreen.WELCOME
                    },
                    onLogin = {
                        viewModel.clearError()
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    hazeState: HazeState,
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    
    // Glass style for theme toggle button
    val toggleGlassStyle = HazeStyle(
        backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
        blurRadius = 20.dp,
        tint = HazeTint(
            color = if (glassColors.isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.7f)
        ),
        noiseFactor = 0.02f
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Theme toggle button - top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .hazeChild(state = hazeState, style = toggleGlassStyle)
                .border(
                    width = 1.dp,
                    color = glassColors.glassBorder,
                    shape = CircleShape
                )
                .clickable { ThemeState.toggleTheme() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (glassColors.isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Toggle theme",
                tint = glassColors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            
            // Welcome Title - positioned higher
            Text(
                text = "Welcome",
                color = glassColors.textPrimary,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle
            Text(
                text = "The Complete Learning Ecosystem",
                color = glassColors.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Description
            Text(
                text = "Turn DSA into a habit: Streaks, Leaderboards,\nFriends and Achievements - right on your\nexisting coding platforms.",
                color = glassColors.textSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Buttons
            GlassButton(
                text = "Create Account",
                onClick = onCreateAccount,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassButton(
                text = "Log In",
                onClick = onLogin,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth(),
                isOutlined = true
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun LoginContent(
    hazeState: HazeState,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: AuthUiState,
    shakeOffset: Float,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Title Section
        Text(
            text = "Log In",
            color = glassColors.textPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Welcome back to Traverse",
            color = glassColors.textSecondary,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Login Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shakeOffset.dp),
            hazeState = hazeState,
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    hazeState = hazeState,
                    placeholder = "Email or Username",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    hazeState = hazeState,
                    placeholder = "Password",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLogin()
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Forgot Password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassTextButton(
                        text = "Forgot Password?",
                        onClick = onForgotPassword
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error message
                AnimatedVisibility(
                    visible = uiState is AuthUiState.Error,
                    enter = fadeIn() + slideInVertically { -10 },
                    exit = fadeOut()
                ) {
                    val errorMessage = (uiState as? AuthUiState.Error)?.message ?: ""
                    Text(
                        text = errorMessage,
                        color = glassColors.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassButton(
                    text = "Log In",
                    onClick = onLogin,
                    hazeState = hazeState,
                    isLoading = uiState is AuthUiState.Loading,
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Create Account Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                color = glassColors.textSecondary,
                fontSize = 14.sp
            )
            GlassTextButton(
                text = "Create Account",
                onClick = onCreateAccount
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back button
        GlassTextButton(
            text = "Back",
            onClick = onBack
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterContent(
    hazeState: HazeState,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    uiState: AuthUiState,
    shakeOffset: Float,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Title Section
        Text(
            text = "Create Account",
            color = glassColors.textPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Join the Traverse community",
            color = glassColors.textSecondary,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Register Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = shakeOffset.dp),
            hazeState = hazeState,
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    hazeState = hazeState,
                    placeholder = "Username",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    hazeState = hazeState,
                    placeholder = "Email",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    hazeState = hazeState,
                    placeholder = "Password",
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onRegister()
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Error message
                AnimatedVisibility(
                    visible = uiState is AuthUiState.Error,
                    enter = fadeIn() + slideInVertically { -10 },
                    exit = fadeOut()
                ) {
                    val errorMessage = (uiState as? AuthUiState.Error)?.message ?: ""
                    Text(
                        text = errorMessage,
                        color = glassColors.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                GlassButton(
                    text = "Create Account",
                    onClick = onRegister,
                    hazeState = hazeState,
                    isLoading = uiState is AuthUiState.Loading,
                    enabled = fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Login Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                color = glassColors.textSecondary,
                fontSize = 14.sp
            )
            GlassTextButton(
                text = "Log In",
                onClick = onLogin
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back button
        GlassTextButton(
            text = "Back",
            onClick = onBack
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
