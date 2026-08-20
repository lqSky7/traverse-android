package com.traverse.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.ui.theme.Peach
import kotlinx.coroutines.launch

private val RoundedShape = RoundedCornerShape(24.dp)
private val AccentPastel = Color(0xFFB8D4E3)
private val EasyPastel = Color(0xFFA8E6CF)
private val HardPastel = Color(0xFFFFAAA5)

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
    val context = LocalContext.current
    
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
                            username.trim()
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
                            username = username.trim(),
                            code = code.trim(),
                            newPassword = newPassword
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { handleBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title
                Text(
                    text = "traverse",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Step Progress Indicators
                if (currentStep != RecoveryStep.COMPLETE) {
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
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) AccentPastel
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Status Message
                statusMessage?.let { message ->
                    StatusCallout(
                        message = message,
                        tone = statusTone,
                        expiresInMinutes = if (currentStep == RecoveryStep.CODE) expiresInMinutes else null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Step Content
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "step_content"
                ) { step ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (step) {
                            RecoveryStep.ACCOUNT -> {
                                Text(
                                    text = "Enter your username to receive a 6-digit verification code.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedShape,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPastel,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
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
                                    enabled = !isLoading
                                )
                            }
                            
                            RecoveryStep.CODE -> {
                                Text(
                                    text = "Enter the 6-digit verification code sent to your registered email.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                                    label = { Text("6-Digit Code") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.MailOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedShape,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPastel,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
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
                                    enabled = !isLoading
                                )
                            }
                            
                            RecoveryStep.PASSWORD -> {
                                Text(
                                    text = "Choose a strong new password (minimum 8 characters).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = if (showPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedShape,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPastel,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    enabled = !isLoading
                                )
                                
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                            Icon(
                                                imageVector = if (showConfirmPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedShape,
                                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                                    supportingText = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                                        { Text("Passwords don't match", color = HardPastel) }
                                    } else null,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentPastel,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        errorBorderColor = HardPastel,
                                        errorLabelColor = HardPastel
                                    ),
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
                                    enabled = !isLoading
                                )
                            }
                            
                            RecoveryStep.COMPLETE -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(EasyPastel.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EasyPastel,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = "Password Reset Complete",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Text(
                                        text = "Your password has been successfully reset. You can now sign in with your new password.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Submit Button
                Button(
                    onClick = { handleSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedShape,
                    enabled = canSubmit && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPastel,
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = when (currentStep) {
                                    RecoveryStep.ACCOUNT -> "Send Code"
                                    RecoveryStep.CODE -> "Verify Code"
                                    RecoveryStep.PASSWORD -> "Reset Password"
                                    RecoveryStep.COMPLETE -> "Back to Sign In"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Back to login link
                TextButton(
                    onClick = onBack,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Return to Sign In",
                        color = Peach,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusCallout(
    message: String,
    tone: StatusTone,
    expiresInMinutes: Int? = null
) {
    val (iconColor, icon) = when (tone) {
        StatusTone.NEUTRAL -> Pair(AccentPastel, Icons.Default.Info)
        StatusTone.SUCCESS -> Pair(EasyPastel, Icons.Default.CheckCircle)
        StatusTone.WARNING -> Pair(Peach, Icons.Default.Warning)
        StatusTone.ERROR -> Pair(HardPastel, Icons.Default.Error)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
                expiresInMinutes?.let {
                    Text(
                        text = "Code expires in $it minutes",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

