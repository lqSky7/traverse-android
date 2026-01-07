package com.example.traverse2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traverse2.ui.theme.GlassColors
import com.example.traverse2.ui.theme.TraverseTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

// Data class for friend
data class Friend(
    val id: String,
    val username: String,
    val email: String,
    val streak: Int,
    val totalXp: Int,
    val totalSolves: Int,
    val friendCount: Int,
    val profileColor: Color = Color(0xFFE91E8C),
    val recentSolves: List<FriendSolve> = emptyList()
)

data class FriendSolve(
    val problemName: String,
    val platform: String,
    val difficulty: String,
    val description: String,
    val solvedAt: String,
    val language: String
)

@Composable
fun FriendsScreen(
    hazeState: HazeState,
    onFriendClick: (Friend) -> Unit
) {
    val glassColors = TraverseTheme.glassColors
    val scrollState = rememberScrollState()
    
    // Mock friends data
    val friends = listOf(
        Friend(
            id = "1",
            username = "alexcoder",
            email = "alex@example.com",
            streak = 14,
            totalXp = 1250,
            totalSolves = 87,
            friendCount = 12,
            profileColor = Color(0xFF6366F1),
            recentSolves = listOf(
                FriendSolve("Two Sum", "LeetCode", "Easy", "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.", "2h ago", "Python"),
                FriendSolve("Valid Parentheses", "LeetCode", "Easy", "Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.", "5h ago", "Java"),
                FriendSolve("Merge Intervals", "LeetCode", "Medium", "Given an array of intervals where intervals[i] = [starti, endi], merge all overlapping intervals.", "1d ago", "C++")
            )
        ),
        Friend(
            id = "2",
            username = "devmaster",
            email = "dev@example.com",
            streak = 21,
            totalXp = 2100,
            totalSolves = 142,
            friendCount = 28,
            profileColor = Color(0xFF10B981),
            recentSolves = listOf(
                FriendSolve("LRU Cache", "LeetCode", "Hard", "Design a data structure that follows the constraints of a Least Recently Used (LRU) cache.", "3h ago", "Python"),
                FriendSolve("Binary Tree Level Order", "LeetCode", "Medium", "Given the root of a binary tree, return the level order traversal of its nodes' values.", "8h ago", "Java")
            )
        ),
        Friend(
            id = "3",
            username = "codequeen",
            email = "queen@example.com",
            streak = 7,
            totalXp = 890,
            totalSolves = 56,
            friendCount = 8,
            profileColor = Color(0xFFF59E0B),
            recentSolves = listOf(
                FriendSolve("Three Sum", "LeetCode", "Medium", "Given an integer array nums, return all the triplets [nums[i], nums[j], nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] + nums[k] == 0.", "1h ago", "JavaScript")
            )
        ),
        Friend(
            id = "4",
            username = "bytewizard",
            email = "wizard@example.com",
            streak = 30,
            totalXp = 3500,
            totalSolves = 210,
            friendCount = 45,
            profileColor = Color(0xFFEF4444),
            recentSolves = listOf(
                FriendSolve("Word Ladder", "LeetCode", "Hard", "Given two words, beginWord and endWord, and a dictionary wordList, return the number of words in the shortest transformation sequence.", "30m ago", "Python"),
                FriendSolve("Median of Two Sorted Arrays", "LeetCode", "Hard", "Given two sorted arrays nums1 and nums2, return the median of the two sorted arrays.", "2h ago", "C++"),
                FriendSolve("Longest Palindromic Substring", "LeetCode", "Medium", "Given a string s, return the longest palindromic substring in s.", "6h ago", "Java"),
                FriendSolve("Container With Most Water", "LeetCode", "Medium", "Given n non-negative integers a1, a2, ..., an, find two lines that together with the x-axis form a container.", "1d ago", "Python")
            )
        ),
        Friend(
            id = "5",
            username = "algopro",
            email = "algo@example.com",
            streak = 5,
            totalXp = 450,
            totalSolves = 32,
            friendCount = 5,
            profileColor = Color(0xFF8B5CF6),
            recentSolves = listOf(
                FriendSolve("Reverse Linked List", "LeetCode", "Easy", "Given the head of a singly linked list, reverse the list, and return the reversed list.", "4h ago", "Python")
            )
        )
    )
    
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 60.dp, bottom = 120.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Friends",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = glassColors.textPrimary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (glassColors.isDark) Color(0x20FFFFFF) else Color(0x20000000)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Friends count",
                    tint = if (glassColors.isDark) Color.White else Color(0xFFE91E8C),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${friends.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Friends list
        friends.forEachIndexed { index, friend ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { 100 }
                )
            ) {
                var showCard by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 80L)
                    showCard = true
                }
                
                if (showCard) {
                    FriendCard(
                        friend = friend,
                        hazeState = hazeState,
                        glassColors = glassColors,
                        onClick = { onFriendClick(friend) }
                    )
                }
            }
            
            if (index < friends.lastIndex) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    hazeState: HazeState,
    glassColors: GlassColors,
    onClick: () -> Unit
) {
    val cardBackground = if (glassColors.isDark) Color.Black else Color.White
    val cardTint = if (glassColors.isDark) Color(0x30000000) else Color(0x30FFFFFF)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
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
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                friend.profileColor,
                                friend.profileColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = friend.profileColor.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.username.first().uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = glassColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Streak
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${friend.streak}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textSecondary
                        )
                    }
                    
                    // XP
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "XP",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${friend.totalXp}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = glassColors.textSecondary
                        )
                    }
                }
            }
            
            // Arrow/chevron indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (glassColors.isDark) Color(0x15FFFFFF) else Color(0x10000000)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "View profile",
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
