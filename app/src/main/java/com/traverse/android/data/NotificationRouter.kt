package com.traverse.android.data

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Maps notifications to destination tab index.
 *
 * Tab indices mirror MainNavigation tabs:
 * 0 = Home, 1 = Problems, 2 = Revisions, 3 = Friends, 4 = Settings
 */
object NotificationRouter {
    /** Set by push banner tap or inbox row tap; consumed by MainNavigation. */
    val pendingTab = MutableStateFlow<Int?>(null)

    fun destinationFor(notification: AppNotification): Int? {
        val fromLink = destinationForLink(notification.link)
        if (fromLink != null) return fromLink
        return destinationForType(notification.type)
    }

    fun destinationForLink(link: String?): Int? {
        if (link.isNullOrBlank()) return null
        val path = if (link.startsWith("traverse://")) {
            val stripped = link.removePrefix("traverse://")
            if (stripped.startsWith("/")) stripped else "/$stripped"
        } else {
            link
        }

        return when {
            path.startsWith("/friends") || path.startsWith("/friend-requests") -> 3
            path.startsWith("/revisions") -> 2
            path.startsWith("/problems") -> 1
            path.startsWith("/home") || path.startsWith("/rings") -> 0
            else -> null
        }
    }

    fun destinationForType(type: String): Int? {
        return when (NotificationType.fromRaw(type)) {
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED,
            NotificationType.STREAK_REQUEST,
            NotificationType.FRIEND_RINGS_CLOSED -> 3 // Friends
            NotificationType.RINGS_CLOSED,
            NotificationType.AWARD_UNLOCKED,
            NotificationType.STREAK_FREEZE_RECEIVED -> 0 // Home
            NotificationType.ANNOUNCEMENT -> null
            null -> null
        }
    }

    fun route(notification: AppNotification) {
        val dest = destinationFor(notification)
        if (dest != null) {
            pendingTab.value = dest
        }
    }

    /**
     * Routes from a push tap. The backend sends `notificationId` and `type` in the
     * FCM `data` payload (not `tab` or `url`), so we resolve from `type` first,
     * then fall back to `url` if present.
     */
    fun routeTo(tab: String? = null, url: String? = null, type: String? = null) {
        val dest = destinationForLink(url)
            ?: destinationForType(type ?: tab ?: "")
        if (dest != null) {
            pendingTab.value = dest
        }
    }
}

/**
 * Formats relative timestamp for inbox item.
 */
fun formatRelativeTime(createdAtIso: String): String {
    if (createdAtIso.isBlank()) return ""
    return try {
        val instant = Instant.parse(createdAtIso)
        val zone = ZoneId.systemDefault()
        val notifDate = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val now = Instant.now()

        val secondsAgo = ChronoUnit.SECONDS.between(instant, now)
        if (secondsAgo < 60) return "Just now"
        val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
        if (minutesAgo < 60) return "${minutesAgo}m"
        val hoursAgo = ChronoUnit.HOURS.between(instant, now)
        if (hoursAgo < 24 && notifDate == today) return "${hoursAgo}h"

        if (notifDate == today.minusDays(1)) return "Yesterday"

        val daysAgo = ChronoUnit.DAYS.between(notifDate, today)
        if (daysAgo < 7) return "${daysAgo}d"

        val notifZoned = instant.atZone(zone)
        if (notifZoned.year == today.year) {
            notifZoned.format(DateTimeFormatter.ofPattern("d MMM"))
        } else {
            notifZoned.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }
    } catch (_: Exception) {
        ""
    }
}

/**
 * Groups notifications into time buckets: Today, Yesterday, This week, Earlier.
 */
fun groupNotificationsByBucket(notifications: List<AppNotification>): List<Pair<String, List<AppNotification>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)

    val buckets = linkedMapOf<String, MutableList<AppNotification>>()
    buckets["Today"] = mutableListOf()
    buckets["Yesterday"] = mutableListOf()
    buckets["This week"] = mutableListOf()
    buckets["Earlier"] = mutableListOf()

    for (notif in notifications) {
        val date = try {
            Instant.parse(notif.createdAt).atZone(zone).toLocalDate()
        } catch (_: Exception) {
            null
        }

        val bucket = when {
            date == null -> "Earlier"
            date == today -> "Today"
            date == yesterday -> "Yesterday"
            ChronoUnit.DAYS.between(date, today) < 7 -> "This week"
            else -> "Earlier"
        }
        buckets[bucket]?.add(notif)
    }

    return buckets.filter { it.value.isNotEmpty() }.map { it.key to it.value }
}
