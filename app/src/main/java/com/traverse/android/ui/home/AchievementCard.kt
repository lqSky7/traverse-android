package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.traverse.android.data.AchievementStatsData

private val CardBackground = Color(0xFF1A1A1A)
private val GlowPastelPink = Color(0xFFFFB6C1)
private val GlowPastelPurple = Color(0xFFE6E6FA)

@Composable
fun AchievementCard(
    stats: AchievementStatsData,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = stats.unlocked.toFloat() / stats.total.coerceAtLeast(1)
    val glowAlpha = (progress * 0.6f).coerceIn(0f, 0.6f)
    
    Box(modifier = modifier) {
        // Glow effect behind card
        if (glowAlpha > 0.1f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(8.dp)
                    .blur(24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlowPastelPink.copy(alpha = glowAlpha),
                                GlowPastelPurple.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
                            .weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (progress > 0.5f) GlowPastelPink else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Achievements",
                                style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${stats.unlocked}",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontSize = 56.sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "of ${stats.total}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                        
                        Text(
                            text = stats.percentage,
                            style = MaterialTheme.typography.labelSmall.copy(color = GlowPastelPink),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = GlowPastelPink,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
        }
    }
}
