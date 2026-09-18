package com.traverse.android.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * One row in the in-app notification inbox.
 *
 * Data payload is typed as [JsonElement] so scalar properties and coalesced items
 * arrays are both absorbed safely without failing the deserializer.
 */
@Serializable
data class AppNotification(
    val id: Int = 0,
    val type: String = "UNKNOWN",
    val title: String = "",
    val body: String = "",
    val link: String? = null,
    val data: JsonElement? = null,
    val readAt: String? = null,
    val createdAt: String = "",
    val coalescedCount: Int? = null
) {
    val isRead: Boolean get() = readAt != null
    val knownType: NotificationType? get() = NotificationType.fromRaw(type)
}

/**
 * Notification types known to the client, used for icon mapping and deep linking.
 * Unknown types fall back to generic bell icon.
 */
enum class NotificationType(val rawValue: String) {
    FRIEND_REQUEST("FRIEND_REQUEST"),
    FRIEND_ACCEPTED("FRIEND_ACCEPTED"),
    STREAK_REQUEST("STREAK_REQUEST"),
    AWARD_UNLOCKED("AWARD_UNLOCKED"),
    STREAK_FREEZE_RECEIVED("STREAK_FREEZE_RECEIVED"),
    RINGS_CLOSED("RINGS_CLOSED"),
    FRIEND_RINGS_CLOSED("FRIEND_RINGS_CLOSED"),
    ANNOUNCEMENT("ANNOUNCEMENT");

    val icon: ImageVector
        get() = when (this) {
            FRIEND_REQUEST -> Icons.Default.PersonAdd
            FRIEND_ACCEPTED -> Icons.Default.HowToReg
            STREAK_REQUEST -> Icons.Default.LocalFireDepartment
            AWARD_UNLOCKED -> Icons.Default.EmojiEvents
            STREAK_FREEZE_RECEIVED -> Icons.Default.AcUnit
            RINGS_CLOSED -> Icons.Default.DonutLarge
            FRIEND_RINGS_CLOSED -> Icons.Default.Groups
            ANNOUNCEMENT -> Icons.Default.Campaign
        }

    companion object {
        fun fromRaw(raw: String): NotificationType? =
            entries.firstOrNull { it.rawValue.equals(raw, ignoreCase = true) }

        fun iconFor(raw: String): ImageVector =
            fromRaw(raw)?.icon ?: Icons.Default.Notifications
    }
}

@Serializable
data class NotificationPreferences(
    val types: List<NotificationTypePreference> = emptyList(),
    val quietHours: QuietHours = QuietHours()
) {
    val configurableTypes: List<NotificationTypePreference>
        get() = types.filter { it.userConfigurable }

    fun isPushEnabled(forType: String): Boolean =
        types.firstOrNull { it.type == forType }?.push ?: true

    fun isInAppEnabled(forType: String): Boolean =
        types.firstOrNull { it.type == forType }?.inApp ?: true
}

@Serializable
data class NotificationTypePreference(
    val type: String = "",
    val label: String = "",
    val description: String = "",
    val userConfigurable: Boolean = true,
    val push: Boolean = true,
    val inApp: Boolean = true
) {
    val knownType: NotificationType? get() = NotificationType.fromRaw(type)
    val icon: ImageVector get() = NotificationType.iconFor(type)
}

/**
 * Quiet hours are a deferral window, not a mute.
 * Midnight wrapping (e.g. 22:00 -> 07:00) is handled.
 */
@Serializable
data class QuietHours(
    val enabled: Boolean = false,
    val start: Int = 22,
    val end: Int = 7
) {
    fun contains(hour: Int): Boolean {
        if (!enabled || start == end) return false
        return if (start < end) hour in start until end else hour >= start || hour < end
    }

    val displayRange: String get() = "${hourLabel(start)} – ${hourLabel(end)}"

    companion object {
        fun hourLabel(hour: Int): String {
            val h = ((hour % 24) + 24) % 24
            return when (h) {
                0 -> "12 AM"
                12 -> "12 PM"
                in 13..23 -> "${h - 12} PM"
                else -> "$h AM"
            }
        }
    }
}

@Serializable
data class NotificationsResponse(
    val notifications: List<AppNotification> = emptyList(),
    val nextCursor: Int? = null,
    val unreadCount: Int = 0
)

@Serializable
data class UnreadCountResponse(
    val unreadCount: Int = 0
)

@Serializable
data class NotificationPreferencesResponse(
    val preferences: NotificationPreferences = NotificationPreferences(),
    val pushConfigured: Boolean = true
)

@Serializable
data class UpdateTypePreference(
    val type: String,
    val push: Boolean? = null,
    val inApp: Boolean? = null
)

@Serializable
data class UpdateNotificationPreferencesRequest(
    val types: List<UpdateTypePreference>? = null,
    val quietHours: QuietHours? = null
)

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: String = "android",
    val deviceId: String? = null
)

@Serializable
data class RegisterPushTokenResponse(
    val success: Boolean,
    val pushToken: PushTokenSummary? = null
)

@Serializable
data class PushTokenSummary(
    val id: Int,
    val platform: String,
    val sandbox: Boolean = false
)

@Serializable
data class UnregisterPushTokenRequest(
    val token: String
)

@Serializable
data class SimpleSuccessResponse(
    val success: Boolean = true,
    val changed: JsonElement? = null,
    val removed: Int? = null
)

