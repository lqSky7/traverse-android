package com.traverse.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun AchievementToastOverlayContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val toastManager = AchievementToastManager.getInstance(context)
    val currentToast by toastManager.currentToast.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .zIndex(100f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeOut()
        ) {
            currentToast?.let { toast ->
                AchievementToastView(
                    toast = toast,
                    onDismiss = { toastManager.dismissCurrentToast() }
                )
            }
        }
    }
}

@Composable
fun AchievementToastView(
    toast: AchievementToastItem,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = getToastCategoryColor(toast.category, toast.count)
    val categoryIcon = getToastCategoryIcon(toast.category, toast.icon, toast.count)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = categoryColor.copy(alpha = 0.4f))
            .border(1.dp, categoryColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF161A26).copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(categoryColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Toast Title Text
            Text(
                text = toast.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Dismiss Button
            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private typealias Void_Or_Unit = () -> Unit

private fun getToastCategoryColor(category: String, count: Int): Color {
    if (count > 1 || category.equals("multi", ignoreCase = true)) {
        return Color(0xFF00E676) // Neon Green
    }
    return when (category.lowercase()) {
        "friend_request" -> Color(0xFF2979FF) // Blue
        "streak_request" -> Color(0xFFFF6D00) // Orange Flame
        "gift_freeze", "freeze" -> Color(0xFF00E5FF) // Ice Cyan
        "solve", "solves" -> Color(0xFF00E676) // Green
        "streak" -> Color(0xFFFF9100) // Flame
        "xp" -> Color(0xFFFFD600) // Gold
        "social" -> Color(0xFF7C4DFF) // Purple
        "revision", "revisions", "ml" -> Color(0xFFE040FB) // Magenta
        else -> Color(0xFFFFD600) // Trophy Gold
    }
}

private fun getToastCategoryIcon(category: String, customIcon: String?, count: Int): ImageVector {
    if (count > 1 || category.equals("multi", ignoreCase = true)) {
        return Icons.Filled.AutoAwesome
    }
    return when (customIcon?.lowercase() ?: category.lowercase()) {
        "person_add", "friend_request" -> Icons.Filled.PersonAdd
        "local_fire_department", "streak_request", "streak", "flame" -> Icons.Filled.LocalFireDepartment
        "ac_unit", "gift_freeze", "freeze", "snowflake" -> Icons.Filled.AcUnit
        "check_circle", "solve", "solves" -> Icons.Filled.CheckCircle
        "star", "xp" -> Icons.Filled.Star
        "psychology", "brain", "revision", "revisions", "ml" -> Icons.Filled.Psychology
        "sparkles", "auto_awesome" -> Icons.Filled.AutoAwesome
        else -> Icons.Filled.EmojiEvents
    }
}
