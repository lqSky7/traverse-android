package com.example.traverse2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    
    val glassStyle = HazeStyle(
        backgroundColor = if (glassColors.isDark) Color.Black else Color.White,
        blurRadius = 20.dp,
        tint = HazeTint(
            color = if (glassColors.isDark) 
                Color.White.copy(alpha = 0.12f) 
            else 
                Color.White.copy(alpha = 0.75f)
        ),
        noiseFactor = if (glassColors.isDark) 0.04f else 0.02f
    )
    
    val borderColor = if (glassColors.isDark) 
        Color.White.copy(alpha = 0.2f) 
    else 
        Color.White.copy(alpha = 0.6f)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .hazeChild(
                state = hazeState,
                style = glassStyle
            )
            .border(
                width = if (glassColors.isDark) 1.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

@Composable
fun SimpleGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColors.glass)
            .border(
                width = 1.dp,
                color = glassColors.glassBorder,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(24.dp),
        content = content
    )
}
