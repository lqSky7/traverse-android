package com.traverse.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBackground = Color(0xFF1A1A1A)

@Composable
fun MainStatsCard(
    totalSolves: Int,
    totalXp: Int,
    streak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleSmall.copy(color = Color.White)
                )
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = "$totalSolves", label = "Total Solves", icon = Icons.Default.CheckCircle)
                VerticalDivider(modifier = Modifier.height(60.dp), color = Color.White.copy(alpha = 0.1f))
                StatItem(value = "$totalXp", label = "Total XP", icon = Icons.Default.Star)
                VerticalDivider(modifier = Modifier.height(60.dp), color = Color.White.copy(alpha = 0.1f))
                StatItem(value = "$streak", label = "Streak Days", icon = Icons.Default.LocalFireDepartment)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, color = Color.White)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}
