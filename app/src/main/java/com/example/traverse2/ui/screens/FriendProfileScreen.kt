package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.components.GlassTopBar
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

@Composable
fun FriendProfileScreen(
    friend: Friend,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 100.dp, bottom = 120.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Header Card
            AnimatedCard(delay = 0, isVisible = isVisible) {
                ProfileHeaderCard(
                    friend = friend,
                    hazeState = hazeState,
                    glassColors = glassColors
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Stats Grid
            AnimatedCard(delay = 100, isVisible = isVisible) {
                StatsGridCard(
                    friend = friend,
                    hazeState = hazeState,
                    glassColors = glassColors
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Recent Solves Section
            AnimatedCard(delay = 200, isVisible = isVisible) {
                RecentSolvesSection(
                    solves = friend.recentSolves,
                    hazeState = hazeState,
                    glassColors = glassColors
                )
            }
        }
        
        // Fixed Glass Top Bar
        GlassTopBar(
            title = "Profile",
            hazeState = hazeState,
            glassColors = glassColors,
            onBack = onBack
        )
    }
}

@Composable
private fun AnimatedCard(delay: Int, isVisible: Boolean, content: @Composable () -> Unit) {
    var showCard by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(delay.toLong())
            showCard = true
        }
    }
    
    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
        ) { 50 }
    ) {
        content()
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
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
private fun ProfileHeaderCard(
    friend: Friend,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Profile Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                friend.profileColor,
                                friend.profileColor.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = friend.profileColor.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.first().uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Username
            Text(
                text = friend.username,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Email
            Text(
                text = friend.email,
                fontSize = 14.sp,
                color = glassColors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${friend.streak}",
                    label = "Streak",
                    iconColor = Color(0xFFFF6B35),
                    glassColors = glassColors
                )
                
                QuickStat(
                    icon = Icons.Default.Star,
                    value = "${friend.totalXp}",
                    label = "XP",
                    iconColor = Color(0xFFFBBF24),
                    glassColors = glassColors
                )
            }
        }
    }
}

@Composable
private fun QuickStat(
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
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = glassColors.textSecondary
        )
    }
}

@Composable
private fun StatsGridCard(
    friend: Friend,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column {
            Text(
                text = "Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = glassColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Solves
                StatBox(
                    icon = Icons.Default.CheckCircle,
                    value = "${friend.totalSolves}",
                    label = "Total Solves",
                    iconColor = Color(0xFF22C55E),
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
                
                // Friends Count
                StatBox(
                    icon = Icons.Default.People,
                    value = "${friend.friendCount}",
                    label = "Friends",
                    iconColor = Color(0xFF6366F1),
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak
                StatBox(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${friend.streak}",
                    label = "Day Streak",
                    iconColor = Color(0xFFFF6B35),
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
                
                // XP
                StatBox(
                    icon = Icons.Default.Star,
                    value = "${friend.totalXp}",
                    label = "Total XP",
                    iconColor = Color(0xFFFBBF24),
                    glassColors = glassColors,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    glassColors: GlassColors,
    modifier: Modifier = Modifier
) {
    val bgColor = if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x15000000)
    // Use a brighter label color for better visibility
    val labelColor = if (glassColors.isDark) Color(0xFFCCCCCC) else glassColors.textSecondary
    
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassColors.textPrimary
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )
            }
        }
    }
}

@Composable
private fun RecentSolvesSection(
    solves: List<FriendSolve>,
    hazeState: HazeState,
    glassColors: GlassColors
) {
    GlassCard(hazeState = hazeState, glassColors = glassColors) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Recent Solves",
                        tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Solves",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glassColors.textPrimary
                    )
                }
                
                Text(
                    text = "${solves.size} problems",
                    fontSize = 13.sp,
                    color = glassColors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (solves.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent solves",
                        fontSize = 14.sp,
                        color = glassColors.textSecondary
                    )
                }
            } else {
                solves.forEachIndexed { index, solve ->
                    SolveDetailCard(
                        solve = solve,
                        glassColors = glassColors,
                        index = index
                    )
                    if (index < solves.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SolveDetailCard(
    solve: FriendSolve,
    glassColors: GlassColors,
    index: Int
) {
    val bgColor = if (glassColors.isDark) Color(0x12FFFFFF) else Color(0x0C000000)
    
    val difficultyColor = when (solve.difficulty.lowercase()) {
        "easy" -> Color(0xFF22C55E)
        "medium" -> Color(0xFFFBBF24)
        "hard" -> Color(0xFFEF4444)
        else -> glassColors.textSecondary
    }
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 100L)
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "solveAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Solved",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = solve.problemName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Difficulty badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(difficultyColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = solve.difficulty,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = difficultyColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Description
        Text(
            text = solve.description,
            fontSize = 13.sp,
            color = glassColors.textSecondary,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Meta info row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Platform
                MetaTag(
                    text = solve.platform,
                    color = if (glassColors.isDark) Color(0xFFFFA116) else Color(0xFFE59400),
                    glassColors = glassColors
                )
                
                // Language
                MetaTag(
                    text = solve.language,
                    color = if (glassColors.isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    glassColors = glassColors
                )
            }
            
            // Time ago
            Text(
                text = solve.solvedAt,
                fontSize = 11.sp,
                color = glassColors.textSecondary
            )
        }
    }
}

@Composable
private fun MetaTag(
    text: String,
    color: Color,
    glassColors: GlassColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
