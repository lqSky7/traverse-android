package com.traverse.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreakCard(
    streak: Int,
    solvedToday: Boolean,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val streakMessage = when {
        solvedToday -> "Well done! Keep it up!"
        streak == 0 -> "Start your streak!"
        else -> "Get back to work!"
    }
    
    // White bg in dark mode, dark bg in light mode (inverted)
    val backgroundColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val textColor = if (isDarkTheme) Color.Black else Color.White
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = "Streak",
            tint = textColor,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$streak",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        color = textColor
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DAYS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = textColor.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            Text(
                text = streakMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.8f)
                )
            )
        }
    }
}
