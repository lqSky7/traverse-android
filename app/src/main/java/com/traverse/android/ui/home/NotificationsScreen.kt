package com.traverse.android.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.traverse.android.ui.theme.SheetPanelBackground
import com.traverse.android.ui.theme.CardBackground
import com.traverse.android.data.AppNotification
import com.traverse.android.data.NotificationRouter
import com.traverse.android.data.NotificationType
import com.traverse.android.data.formatRelativeTime
import com.traverse.android.data.groupNotificationsByBucket
import com.traverse.android.ui.theme.rememberPalette
import com.traverse.android.viewmodel.NotificationsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Panel surface *inside* a sheet — see the note in `RingGoalsSheet`.
 *
 * This was its own `#1C1C1E` on a `#121212` sheet, neither of which appears anywhere else in the
 * app. The sheet now uses [CardBackground] like every other sheet, and the panels lift off it with
 * a translucent white, matching `AllAtRiskProblemsSheet`.
 */
private val CardBg = SheetPanelBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    viewModel: NotificationsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The inbox without the request types.
    //
    // FRIEND_REQUEST and STREAK_REQUEST no longer belong here. A request now arrives as a
    // full-screen prompt with Accept and Reject, which is the right surface for the one thing in
    // this list that actually needs an answer — and leaving a copy in the inbox meant the same
    // request appeared twice, with the inbox copy unable to do anything about it.
    //
    // FRIEND_ACCEPTED deliberately stays. It is the outcome of a request *you* sent, so it has no
    // prompt and no other surface; dropping it would leave you no way to learn it happened.
    val visibleNotifications = remember(uiState.notifications) {
        uiState.notifications.filter {
            it.type != NotificationType.FRIEND_REQUEST.rawValue &&
                it.type != NotificationType.STREAK_REQUEST.rawValue
        }
    }

    val scope = rememberCoroutineScope()
    val palette = rememberPalette()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text("Mark all read", color = palette.primary, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Text(
                    text = "Notifications",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.notifications.isNotEmpty(),
                onRefresh = { viewModel.load(forceRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    // Every branch tests `visibleNotifications`, not the raw list. An inbox holding
                    // nothing but request rows would otherwise fall through to the list branch and
                    // render an empty screen instead of the empty state.
                    uiState.isLoading && visibleNotifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = palette.primary)
                        }
                    }

                    uiState.errorMessage != null && visibleNotifications.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.errorMessage ?: "Error loading notifications",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { viewModel.load(forceRefresh = true) }) {
                                Text("Try again", color = palette.primary)
                            }
                        }
                    }

                    visibleNotifications.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Nothing yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Awards, closed rings and announcements will show up here. Friend and streak requests arrive as a full-screen prompt instead.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        val grouped = groupNotificationsByBucket(visibleNotifications)
                        val listState = rememberLazyListState()

                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val totalItems = listState.layoutInfo.totalItemsCount
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                totalItems > 0 && lastVisible >= totalItems - 2 && uiState.nextCursor != null && !uiState.isLoadingMore
                            }
                        }

                        LaunchedEffect(shouldLoadMore) {
                            if (shouldLoadMore) {
                                viewModel.loadMore()
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            grouped.forEach { (bucket, items) ->
                                item {
                                    Text(
                                        text = bucket.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        color = Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 8.dp)
                                    )
                                }

                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CardBg)
                                    ) {
                                        items.forEachIndexed { index, notification ->
                                            NotificationRowItem(
                                                notification = notification,
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.markRead(notification.id)
                                                        if (NotificationRouter.destinationFor(notification) != null) {
                                                            sheetState.hide()
                                                            onDismiss()
                                                            delay(220)
                                                            NotificationRouter.route(notification)
                                                        }
                                                    }
                                                }
                                            )
                                            if (index < items.size - 1) {
                                                HorizontalDivider(
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    modifier = Modifier.padding(start = 58.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = palette.primary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRowItem(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val palette = rememberPalette()
    val isAward = notification.knownType == NotificationType.AWARD_UNLOCKED
    val iconTint = when {
        isAward -> Color(0xFFFFD93D)
        !notification.isRead -> palette.colorAt(0)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon container with unread dot
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.knownType?.icon ?: NotificationType.iconFor(notification.type),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(CardBg)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .background(palette.colorAt(0))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (notification.isRead) Color.White.copy(alpha = 0.8f) else Color.White,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatRelativeTime(notification.createdAt),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = notification.body,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            if (notification.coalescedCount != null && notification.coalescedCount > 1) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${notification.coalescedCount} in total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.colorAt(1)
                )
            }
        }
    }
}
