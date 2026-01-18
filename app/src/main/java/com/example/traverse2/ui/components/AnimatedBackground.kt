package com.example.traverse2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    content: @Composable () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    
    // Orb alpha values - higher in dark mode for more prominent glow
    val orbAlpha = if (glassColors.isDark) 0.25f else 0.5f
    val orbAlpha2 = if (glassColors.isDark) 0.2f else 0.4f
    val orbAlpha3 = if (glassColors.isDark) 0.15f else 0.35f
    
    // Reduced blur radius for better performance (was 80-120dp, now 40-60dp)
    val orbBlur = if (glassColors.isDark) 50.dp else 40.dp
    val orbBlurLarge = if (glassColors.isDark) 60.dp else 50.dp
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background layer with gradient and static orbs - THIS is the haze source
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
            // Static orb 1 - Top right
            Box(
                modifier = Modifier
                    .offset(x = 200.dp, y = 80.dp)
                    .size(300.dp)
                    .blur(orbBlurLarge)
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
            
            // Static orb 2 - Bottom left
            Box(
                modifier = Modifier
                    .offset(x = (-80).dp, y = 450.dp)
                    .size(350.dp)
                    .blur(orbBlurLarge)
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
            
            // Static orb 3 - Top left
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = 150.dp)
                    .size(250.dp)
                    .blur(orbBlur)
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
            
            // Static orb 4 - Center right
            Box(
                modifier = Modifier
                    .offset(x = 280.dp, y = 320.dp)
                    .size(200.dp)
                    .blur(orbBlur)
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
            
            // Static orb 5 - Bottom center
            Box(
                modifier = Modifier
                    .offset(x = 100.dp, y = 650.dp)
                    .size(280.dp)
                    .blur(orbBlur)
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
