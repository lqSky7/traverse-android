package com.example.traverse2.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.ThemeState
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun SettingsScreen(
    hazeState: HazeState,
    onLogout: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Mock user data
    val username = "yasha"
    val email = "yasha@example.com"
    val dayStreak = 7
    val totalXp = 680
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp, bottom = 120.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = glassColors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Profile Card
        ProfileCard(
            username = username,
            email = email,
            dayStreak = dayStreak,
            totalXp = totalXp,
            hazeState = hazeState,
            glassColors = glassColors
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Theme Switcher Card
        ThemeSwitcherCard(
            hazeState = hazeState,
            glassColors = glassColors
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Account Actions Card
        AccountActionsCard(
            hazeState = hazeState,
            glassColors = glassColors,
            onLogout = onLogout
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Delete Account Card
        DeleteAccountCard(
            hazeState = hazeState,
            glassColors = glassColors
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Contact Us Card
        ContactUsCard(
            hazeState = hazeState,
            glassColors = glassColors,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SparkleYRP/"))
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun GlassCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
private fun ProfileCard(
    username: String,
    email: String,
    dayStreak: Int,
    totalXp: Int,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image Placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (glassColors.isDark) {
                                    listOf(Color(0xFF3B3B3B), Color(0xFF1A1A1A))
                                } else {
                                    listOf(Color(0xFFE91E8C), Color(0xFFA855F7))
                                }
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (glassColors.isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = username,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = glassColors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = email,
                            fontSize = 13.sp,
                            color = glassColors.textSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Streak Stat
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "$dayStreak",
                    label = "Day Streak",
                    iconColor = Color(0xFFFF6B35),
                    glassColors = glassColors
                )
                
                // XP Stat
                StatItem(
                    icon = Icons.Default.Star,
                    value = "$totalXp",
                    label = "Total XP",
                    iconColor = Color(0xFFFBBF24),
                    glassColors = glassColors
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    glassColors: GlassColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = glassColors.textSecondary
        )
    }
}

@Composable
private fun ThemeSwitcherCard(
    hazeState: HazeState,
    glassColors: GlassColors
) {
    val isDarkMode = ThemeState.isDarkMode
    
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column {
            Text(
                text = "Appearance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Light Theme Option
                ThemeOptionCard(
                    isSelected = !isDarkMode,
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    gradientColors = listOf(Color(0xFFFCE7F3), Color(0xFFFDF2F8)),
                    iconColor = Color(0xFFE91E8C),
                    onClick = { if (isDarkMode) ThemeState.toggleTheme() },
                    modifier = Modifier.weight(1f)
                )
                
                // Dark Theme Option
                ThemeOptionCard(
                    isSelected = isDarkMode,
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    gradientColors = listOf(Color(0xFF1F1F1F), Color(0xFF0A0A0A)),
                    iconColor = Color.White,
                    onClick = { if (!isDarkMode) ThemeState.toggleTheme() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    isSelected: Boolean,
    title: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = TraverseTheme.glassColors
    
    // Animations
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (glassColors.isDark) Color.White else Color(0xFFE91E8C)
        } else {
            if (glassColors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = iconColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(
                        width = if (isSelected) 24.dp else 8.dp,
                        height = 4.dp
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSelected) iconColor else iconColor.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun AccountActionsCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    onLogout: () -> Unit
) {
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column {
            Text(
                text = "Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Edit Profile
            ActionItem(
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                subtitle = "Update your information",
                iconColor = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
                glassColors = glassColors,
                onClick = { /* TODO: Navigate to edit profile */ }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Security
            ActionItem(
                icon = Icons.Default.Lock,
                title = "Security",
                subtitle = "Change password",
                iconColor = if (glassColors.isDark) Color.White else Color(0xFF6366F1),
                glassColors = glassColors,
                onClick = { /* TODO: Navigate to security */ }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Logout
            ActionItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                subtitle = "Sign out of your account",
                iconColor = Color(0xFFEF4444),
                glassColors = glassColors,
                onClick = onLogout
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    glassColors: GlassColors,
    onClick: () -> Unit
) {
    val bgColor = if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x10000000)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = glassColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = glassColors.textSecondary
            )
        }
    }
}

@Composable
private fun DeleteAccountCard(
    hazeState: HazeState,
    glassColors: GlassColors
) {
    val dangerColor = Color(0xFFEF4444)
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .border(
                width = 1.dp,
                color = dangerColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { /* TODO: Show delete confirmation */ }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(dangerColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = dangerColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Delete Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = dangerColor
                )
                Text(
                    text = "Permanently remove your data",
                    fontSize = 11.sp,
                    color = glassColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ContactUsCard(
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit
) {
    val accentColor = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hazeChild(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = cardBackground,
                    blurRadius = if (glassColors.isDark) 60.dp else 50.dp,
                    tint = HazeTint(cardTint),
                    noiseFactor = 0.02f
                )
            )
            .background(if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Contact Us",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contact Us",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
                Text(
                    text = "Reach out on Telegram",
                    fontSize = 11.sp,
                    color = glassColors.textSecondary
                )
            }
        }
    }
}
