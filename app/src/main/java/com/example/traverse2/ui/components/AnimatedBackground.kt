package com.example.traverse2.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Animated gradient background with floating orbs for glassmorphic effect.
 * The background with orbs is the haze source, content is rendered as sibling.
 * Supports both light (pink) and dark (black/white) themes.
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    content: @Composable () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    
    // Orb alpha values - higher in dark mode for more prominent glow
    val orbAlpha = if (glassColors.isDark) 0.25f else 0.5f
    val orbAlpha2 = if (glassColors.isDark) 0.2f else 0.4f
    val orbAlpha3 = if (glassColors.isDark) 0.15f else 0.35f
    
    // Animated offset for floating orbs
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )
    
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )
    
    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX1"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background layer with gradient and orbs - THIS is the haze source
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            glassColors.gradientStart,
                            glassColors.gradientMiddle,
                            glassColors.gradientEnd
                        )
                    )
                )
        ) {
            // Floating orb 1 - Top right
            Box(
                modifier = Modifier
                    .offset(x = 200.dp + offsetX1.dp, y = 80.dp + offsetY1.dp)
                    .size(300.dp)
                    .blur(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glassColors.orb1.copy(alpha = orbAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Floating orb 2 - Bottom left
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = 450.dp + offsetY2.dp)
                    .size(350.dp)
                    .blur(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glassColors.orb2.copy(alpha = orbAlpha2),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Floating orb 3 - Top left
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp + offsetX1.dp, y = 150.dp)
                    .size(250.dp)
                    .blur(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glassColors.orb3.copy(alpha = orbAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Floating orb 4 - Center right
            Box(
                modifier = Modifier
                    .offset(x = 280.dp, y = 320.dp + offsetY1.dp)
                    .size(200.dp)
                    .blur(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glassColors.orb4.copy(alpha = orbAlpha2),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Floating orb 5 - Bottom center
            Box(
                modifier = Modifier
                    .offset(x = 100.dp + offsetX1.dp, y = 650.dp + offsetY2.dp)
                    .size(280.dp)
                    .blur(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glassColors.orb5.copy(alpha = orbAlpha3),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Content layer - sibling to the haze source, NOT a descendant
        content()
    }
}
