package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.data.api.ChangePasswordRequest
import com.example.traverse2.data.api.RetrofitClient
import com.example.traverse2.data.api.UpdateProfileRequest
import com.example.traverse2.data.model.User
import com.example.traverse2.ui.components.GlassButton
import com.example.traverse2.ui.components.GlassTextField
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    hazeState: HazeState,
    user: User?,
    onBack: () -> Unit,
    onProfileUpdated: (User) -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Form states
    var email by remember { mutableStateOf(user?.email ?: "") }
    var visibility by remember { mutableStateOf(user?.visibility ?: "public") }

    // Password change states
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // UI states
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(glassColors.glass)
                    .border(1.dp, glassColors.glassBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = glassColors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Edit Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Placeholder for symmetry
            Box(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Success/Error Messages
            AnimatedVisibility(
                visible = successMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = Color(0xFF22C55E),
                        fontSize = 14.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                }
            }

            if (successMessage != null || errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Email Section
            SectionCard(
                title = "Email",
                icon = Icons.Default.Email,
                hazeState = hazeState,
                glassColors = glassColors
            ) {
                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Enter email",
                    hazeState = hazeState
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassButton(
                    text = if (isLoading) "Saving..." else "Update Email",
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                val response = RetrofitClient.api.updateProfile(
                                    UpdateProfileRequest(email = email.ifBlank { null })
                                )
                                if (response.isSuccessful && response.body() != null) {
                                    successMessage = "Email updated successfully"
                                    onProfileUpdated(response.body()!!.user)
                                } else {
                                    errorMessage = "Failed to update email"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Network error"
                            }
                            isLoading = false
                        }
                    },
                    hazeState = hazeState,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visibility Section
            SectionCard(
                title = "Profile Visibility",
                icon = Icons.Default.Public,
                hazeState = hazeState,
                glassColors = glassColors
            ) {
                Text(
                    text = "Control who can see your profile and stats",
                    fontSize = 13.sp,
                    color = glassColors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisibilityOption(
                        title = "Public",
                        icon = Icons.Default.Public,
                        isSelected = visibility == "public",
                        onClick = { visibility = "public" },
                        glassColors = glassColors,
                        modifier = Modifier.weight(1f)
                    )
                    VisibilityOption(
                        title = "Friends",
                        icon = Icons.Default.Visibility,
                        isSelected = visibility == "friends",
                        onClick = { visibility = "friends" },
                        glassColors = glassColors,
                        modifier = Modifier.weight(1f)
                    )
                    VisibilityOption(
                        title = "Private",
                        icon = Icons.Default.VisibilityOff,
                        isSelected = visibility == "private",
                        onClick = { visibility = "private" },
                        glassColors = glassColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassButton(
                    text = if (isLoading) "Saving..." else "Update Visibility",
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            try {
                                val response = RetrofitClient.api.updateProfile(
                                    UpdateProfileRequest(visibility = visibility)
                                )
                                if (response.isSuccessful && response.body() != null) {
                                    successMessage = "Visibility updated to $visibility"
                                    onProfileUpdated(response.body()!!.user)
                                } else {
                                    errorMessage = "Failed to update visibility"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Network error"
                            }
                            isLoading = false
                        }
                    },
                    hazeState = hazeState,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Password Section
            SectionCard(
                title = "Change Password",
                icon = Icons.Default.Lock,
                hazeState = hazeState,
                glassColors = glassColors
            ) {
                if (!showPasswordSection) {
                    GlassButton(
                        text = "Change Password",
                        onClick = { showPasswordSection = true },
                        hazeState = hazeState,
                        isOutlined = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    GlassTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        placeholder = "Current Password",
                        isPassword = true,
                        hazeState = hazeState
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "New Password (min 8 chars)",
                        isPassword = true,
                        hazeState = hazeState
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm New Password",
                        isPassword = true,
                        hazeState = hazeState
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            text = "Cancel",
                            onClick = {
                                showPasswordSection = false
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            },
                            hazeState = hazeState,
                            isOutlined = true,
                            modifier = Modifier.weight(1f)
                        )

                        GlassButton(
                            text = if (isLoading) "..." else "Update",
                            onClick = {
                                when {
                                    currentPassword.isBlank() -> {
                                        errorMessage = "Current password is required"
                                    }
                                    newPassword.length < 8 -> {
                                        errorMessage = "New password must be at least 8 characters"
                                    }
                                    newPassword != confirmPassword -> {
                                        errorMessage = "Passwords do not match"
                                    }
                                    else -> {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            successMessage = null
                                            try {
                                                val response = RetrofitClient.api.changePassword(
                                                    ChangePasswordRequest(
                                                        currentPassword = currentPassword,
                                                        newPassword = newPassword
                                                    )
                                                )
                                                if (response.isSuccessful) {
                                                    successMessage = "Password changed successfully"
                                                    showPasswordSection = false
                                                    currentPassword = ""
                                                    newPassword = ""
                                                    confirmPassword = ""
                                                } else {
                                                    errorMessage = "Current password is incorrect"
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = e.message ?: "Network error"
                                            }
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            hazeState = hazeState,
                            enabled = !isLoading && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    hazeState: HazeState,
    glassColors: com.example.traverse2.ui.theme.GlassColors,
    content: @Composable () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = 24.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = glassColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    glassColors: com.example.traverse2.ui.theme.GlassColors,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && glassColors.isDark -> Color.White.copy(alpha = 0.15f)
        isSelected -> glassColors.accent.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> glassColors.accent
        glassColors.isDark -> Color.White.copy(alpha = 0.1f)
        else -> Color.Black.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) glassColors.accent else glassColors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) glassColors.textPrimary else glassColors.textSecondary
            )
        }
    }
}
