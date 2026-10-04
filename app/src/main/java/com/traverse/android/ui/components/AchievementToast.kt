package com.traverse.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
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
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.ui.theme.rememberPalette

@Composable
fun AchievementToastOverlayContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val toastManager = AchievementToastManager.getInstance(context)
    val currentToast by toastManager.currentToast.collectAsState()
    val request by toastManager.incomingRequest.collectAsState()
    request?.let { IncomingSocialRequestDialog(it, toastManager) }

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
        color = CardBackground.copy(alpha = 0.95f),
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

/**
 * iOS `AchievementToastView.categoryColor` — the toast accent follows the selected palette,
 * with a distinct colour index per achievement category.
 */
@Composable
private fun getToastCategoryColor(category: String, count: Int): Color {
    val palette = rememberPalette()
    if (count > 1 || category.equals("multi", ignoreCase = true)) {
        return palette.primary
    }
    return when (category.lowercase()) {
        "friend_request" -> palette.colorAt(2)
        "streak_request" -> palette.colorAt(0)
        "gift_freeze", "freeze" -> palette.colorAt(1)
        "solve", "solves" -> palette.colorAt(1)
        "streak" -> palette.colorAt(0)
        "xp" -> palette.colorAt(3)
        "social" -> palette.colorAt(2)
        "language" -> palette.colorAt(1)
        "revision", "revisions", "ml" -> palette.colorAt(4)
        "fun" -> palette.colorAt(0)
        else -> palette.colorAt(3)
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


@Composable
private fun IncomingSocialRequestDialog(request: IncomingSocialRequest, manager: AchievementToastManager) {
    val palette = rememberPalette()
    val busy by manager.isResponding.collectAsState()
    val error by manager.requestError.collectAsState()
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { manager.dismissRequest() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = CardBackground) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize().padding(28.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
            ) {
                Icon(if (request.isStreak) Icons.Default.LocalFireDepartment else Icons.Default.PersonAdd,
                    contentDescription = null, tint = palette.primary, modifier = Modifier.size(64.dp))
                Text(if (request.isStreak) "Streak invitation" else "Friend request", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text(request.username, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(if (request.isStreak) "Wants to build a streak with you." else "Wants to be your friend on Traverse.", color = Color.White.copy(alpha = 0.7f))
                com.traverse.android.data.ActivityTimestamp.parse(request.createdAt)?.let {
                    Text(it.atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)), color = Color.White.copy(alpha = 0.6f))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                androidx.compose.material3.Button(enabled = !busy, onClick = { manager.respondToRequest(true) }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.primary)) { Text("Accept") }
                androidx.compose.material3.OutlinedButton(enabled = !busy, onClick = { manager.respondToRequest(false) }) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                androidx.compose.material3.TextButton(enabled = !busy, onClick = { manager.dismissRequest() }) { Text("Later") }
                if (busy) androidx.compose.material3.CircularProgressIndicator(color = palette.primary)
            }
        }
    }
}
