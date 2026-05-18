package com.traverse.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.PasswordResetConfirmRequest
import com.traverse.android.data.PasswordResetRequest
import kotlinx.coroutines.launch

enum class RecoveryStep {
    ACCOUNT, CODE, PASSWORD, COMPLETE
}

enum class StatusTone {
    NEUTRAL, SUCCESS, WARNING, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordResetScreen(
    initialUsername: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    var currentStep by remember { mutableStateOf(RecoveryStep.ACCOUNT) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusTone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var expiresInMinutes by remember { mutableStateOf<Int?>(null) }
    
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    val gradient = remember(currentStep) {
        when (currentStep) {
            RecoveryStep.ACCOUNT -> listOf(
                Color(0xFFDB2777), Color(0xFF9333EA), Color(0xFF6366F1)
            )
            RecoveryStep.CODE -> listOf(
                Color(0xFF6366F1), Color(0xFF3B82F6), Color(0xFF06B6D4)
            )
            RecoveryStep.PASSWORD -> listOf(
                Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF06B6D4)
            )
            RecoveryStep.COMPLETE -> listOf(
                Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF06B6D4)
            )
        }
    }
    
    val canSubmit = remember(currentStep, username, code, newPassword, confirmPassword, isLoading) {
        if (isLoading) return@remember false
        when (currentStep) {
            RecoveryStep.ACCOUNT -> username.trim().isNotEmpty()
            RecoveryStep.CODE -> code.trim().length == 6
            RecoveryStep.PASSWORD -> newPassword.length >= 8 && newPassword == confirmPassword
            RecoveryStep.COMPLETE -> true
        }
    }
    
    fun handleBack() {
        when (currentStep) {
            RecoveryStep.ACCOUNT -> onBack()
            RecoveryStep.CODE -> {
                currentStep = RecoveryStep.ACCOUNT
                statusMessage = null
            }
            RecoveryStep.PASSWORD -> {
                currentStep = RecoveryStep.CODE
                statusMessage = null
            }
            RecoveryStep.COMPLETE -> onComplete()
        }
    }
    
    fun handleSubmit() {
        scope.launch {
            isLoading = true
            statusMessage = null
            
            try {
                when (currentStep) {
                    RecoveryStep.ACCOUNT -> {
                        val response = NetworkService.getInstance(context).requestPasswordReset(
                            PasswordResetRequest(username.trim())
                        )
                        when (response) {
                            is NetworkResult.Success -> {
                                expiresInMinutes = response.data.expiresInMinutes
                                statusMessage = response.data.message
                                statusTone = StatusTone.SUCCESS
                                currentStep = RecoveryStep.CODE
                            }
                            is NetworkResult.Error -> {
                                statusMessage = response.message
                                statusTone = StatusTone.ERROR
                            }
                        }
                    }
                    RecoveryStep.CODE -> {
                        // Just validate code format and move to password step
                        if (code.trim().length == 6) {
                            currentStep = RecoveryStep.PASSWORD
                            statusMessage = null
                        } else {
                            statusMessage = "Code must be 6 digits"
                            statusTone = StatusTone.ERROR
                        }
                    }
                    RecoveryStep.PASSWORD -> {
                        val response = NetworkService.getInstance(context).confirmPasswordReset(
                            PasswordResetConfirmRequest(
                                username = username.trim(),
                                code = code.trim(),
                                newPassword = newPassword
                            )
                        )
                        when (response) {
                            is NetworkResult.Success -> {
                                statusMessage = response.data.message
                                statusTone = StatusTone.SUCCESS
                                currentStep = RecoveryStep.COMPLETE
                            }
                            is NetworkResult.Error -> {
                                statusMessage = response.message
                                statusTone = StatusTone.ERROR
                            }
                        }
                    }
                    RecoveryStep.COMPLETE -> {
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                statusMessage = e.message ?: "An error occurred"
                statusTone = StatusTone.ERROR
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradient,
                        startY = 0f,
                        endY = 1500f
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Text(
                    text = "Stay in the flow. We'll verify your account, confirm the code, then bring you right back to sign in.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecoveryStep.entries.filter { it != RecoveryStep.COMPLETE }.forEach { step ->
                        val isActive = step.ordinal <= currentStep.ordinal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isActive) Color.White
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Message
                statusMessage?.let { message ->
                    StatusCallout(
                        message = message,
                        tone = statusTone,
                        expiresInMinutes = if (currentStep == RecoveryStep.CODE) expiresInMinutes else null
                    )
                }
                
                // Content based on step
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "step_content"
                ) { step ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (step) {
                            RecoveryStep.ACCOUNT -> {
                                StepHeader(
                                    icon = Icons.Default.Person,
                                    title = "Account",
                                    description = "Enter your username to receive a verification code"
                                )
                                
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (canSubmit) handleSubmit()
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        cursorColor = Color.White
                                    )
                                )
                            }
                            
                            RecoveryStep.CODE -> {
                                StepHeader(
                                    icon = Icons.Default.MailOutline,
                                    title = "Verification",
                                    description = "Enter the 6-digit code sent to your email"
                                )
                                
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                                    label = { Text("6-Digit Code") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (canSubmit) handleSubmit()
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        cursorColor = Color.White
                                    )
                                )
                            }
                            
                            RecoveryStep.PASSWORD -> {
                                StepHeader(
                                    icon = Icons.Default.Lock,
                                    title = "New Password",
                                    description = "Choose a strong password (minimum 8 characters)"
                                )
                                
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        cursorColor = Color.White
                                    )
                                )
                                
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    singleLine = true,
                                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (canSubmit) handleSubmit()
                                        }
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                            Icon(
                                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    },
                                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                                    supportingText = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                                        { Text("Passwords don't match", color = Color(0xFFFFCDD2)) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                        cursorColor = Color.White,
                                        errorBorderColor = Color(0xFFEF5350),
                                        errorLabelColor = Color(0xFFFFCDD2)
                                    )
                                )
                            }
                            
                            RecoveryStep.COMPLETE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(80.dp)
                                    )
                                    
                                    Text(
                                        text = "Password Reset Complete",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Text(
                                        text = "Your password has been successfully reset. You can now sign in with your new password.",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.White.copy(alpha = 0.9f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit Button
                Button(
                    onClick = { handleSubmit() },
                    enabled = canSubmit && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradient[1],
                        disabledContainerColor = Color.White.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = gradient[1],
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when (currentStep) {
                                RecoveryStep.ACCOUNT -> "Send Code"
                                RecoveryStep.CODE -> "Verify Code"
                                RecoveryStep.PASSWORD -> "Reset Password"
                                RecoveryStep.COMPLETE -> "Back to Sign In"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.9f)
            )
        )
    }
}

@Composable
private fun StatusCallout(
    message: String,
    tone: StatusTone,
    expiresInMinutes: Int? = null
) {
    val (backgroundColor, iconColor, icon) = when (tone) {
        StatusTone.NEUTRAL -> Triple(
            Color.White.copy(alpha = 0.1f),
            Color.White,
            Icons.Default.Info
        )
        StatusTone.SUCCESS -> Triple(
            Color(0xFF10B981).copy(alpha = 0.2f),
            Color(0xFF10B981),
            Icons.Default.CheckCircle
        )
        StatusTone.WARNING -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.2f),
            Color(0xFFF59E0B),
            Icons.Default.Warning
        )
        StatusTone.ERROR -> Triple(
            Color(0xFFEF4444).copy(alpha = 0.2f),
            Color(0xFFEF4444),
            Icons.Default.Error
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
            expiresInMinutes?.let {
                Text(
                    text = "Code expires in $it minutes",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}
