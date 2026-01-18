package com.example.traverse2.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isOutlined: Boolean = false
) {
    val glassColors = TraverseTheme.glassColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Theme-aware glass style for button
    val buttonGlassStyle = HazeStyle(
        backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
        blurRadius = 16.dp,
        tint = HazeTint(
            color = if (glassColors.isDark) 
                Color.White.copy(alpha = 0.12f) 
            else 
                Color.White.copy(alpha = 0.8f)
        ),
        noiseFactor = if (glassColors.isDark) 0.03f else 0.01f
    )
    
    // Scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    
    // Opacity animation on press
    val alpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.8f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "alpha"
    )
    
    // Determine button style based on isPrimary and isOutlined
    val showGradient = isPrimary && !isOutlined
    val showGlass = !isPrimary || isOutlined
    val showBorder = !isPrimary || isOutlined
    
    val buttonBackground = if (showGradient) {
        Brush.horizontalGradient(
            colors = listOf(
                glassColors.accent,
                glassColors.accentSecondary
            )
        )
    } else {
        null
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (showGlass) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = buttonGlassStyle
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (buttonBackground != null) {
                    Modifier.background(buttonBackground, alpha = alpha)
                } else {
                    Modifier
                }
            )
            .border(
                width = if (showBorder) 1.5.dp else 0.dp,
                color = if (isOutlined) glassColors.accent.copy(alpha = 0.5f) else if (showBorder) glassColors.glassBorder else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (showGradient) {
                    if (glassColors.isDark) Color.Black else Color.White
                } else glassColors.accent,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = when {
                        showGradient -> if (glassColors.isDark) Color.Black else Color.White
                        isOutlined -> glassColors.accent
                        else -> glassColors.textPrimary
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val glassColors = TraverseTheme.glassColors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "alpha"
    )
    
    Text(
        text = text,
        color = glassColors.accent.copy(alpha = alpha),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
    )
}
