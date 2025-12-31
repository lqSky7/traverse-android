package com.example.traverse2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun RevisionsPlaceholder(hazeState: HazeState) {
    PlaceholderScreen(
        title = "Revisions",
        subtitle = "Spaced repetition coming soon",
        icon = Icons.Default.AutoStories,
        hazeState = hazeState
    )
}

@Composable
fun FriendsPlaceholder(hazeState: HazeState) {
    PlaceholderScreen(
        title = "Friends",
        subtitle = "Social features coming soon",
        icon = Icons.Default.Group,
        hazeState = hazeState
    )
}

@Composable
fun SettingsPlaceholder(hazeState: HazeState) {
    PlaceholderScreen(
        title = "Settings",
        subtitle = "Customization coming soon",
        icon = Icons.Default.Settings,
        hazeState = hazeState
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    hazeState: HazeState
) {
    val glassColors = TraverseTheme.glassColors
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
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
                .background(
                    if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x40FFFFFF)
                )
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = glassColors.textSecondary
                )
            }
        }
    }
}
