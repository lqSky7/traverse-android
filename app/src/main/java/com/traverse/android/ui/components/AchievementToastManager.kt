package com.traverse.android.ui.components

import android.content.Context
import android.content.SharedPreferences
import com.traverse.android.data.AchievementDetail
import com.traverse.android.data.DataManager
import com.traverse.android.data.FreezeInfoResponse
import com.traverse.android.data.FriendRequest
import com.traverse.android.data.FriendStreakRequest
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AchievementToastItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val icon: String? = null,
    val count: Int = 1
)

/**
 * 1:1 Kotlin port of iOS AchievementToastManager.swift.
 * Manages in-app animated toast notifications for unlocked achievements,
 * friend requests, streak invites, and gifted streak freezes.
 */
class AchievementToastManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs: SharedPreferences = context.getSharedPreferences("traverse_toasts_prefs", Context.MODE_PRIVATE)

    private val _currentToast = MutableStateFlow<AchievementToastItem?>(null)
    val currentToast: StateFlow<AchievementToastItem?> = _currentToast.asStateFlow()

    private val toastQueue = mutableListOf<AchievementToastItem>()
    private var dismissJob: Job? = null

    private var lastSyncTimestamp: Long = 0
    private var isSyncing = false

    private var seenUnlockedKeys: MutableSet<String>
        get() {
            val raw = prefs.getString("seen_unlocked_keys", "") ?: ""
            return if (raw.isEmpty()) mutableSetOf() else raw.split(",").toMutableSet()
        }
        set(value) {
            prefs.edit().putString("seen_unlocked_keys", value.joinToString(",")).apply()
        }

    private var seenFriendRequestKeys: MutableSet<String>
        get() {
            val raw = prefs.getString("seen_friend_request_keys", "") ?: ""
            return if (raw.isEmpty()) mutableSetOf() else raw.split(",").toMutableSet()
        }
        set(value) {
            prefs.edit().putString("seen_friend_request_keys", value.joinToString(",")).apply()
        }

    private var seenStreakRequestKeys: MutableSet<String>
        get() {
            val raw = prefs.getString("seen_streak_request_keys", "") ?: ""
            return if (raw.isEmpty()) mutableSetOf() else raw.split(",").toMutableSet()
        }
        set(value) {
            prefs.edit().putString("seen_streak_request_keys", value.joinToString(",")).apply()
        }

    private var lastAvailableFreezesCount: Int
        get() = prefs.getInt("last_available_freezes", -1)
        set(value) = prefs.edit().putInt("last_available_freezes", value).apply()

    private var lastSeenGiftedFreezeID: Int
        get() = prefs.getInt("last_gifted_freeze_id", -1)
        set(value) = prefs.edit().putInt("last_gifted_freeze_id", value).apply()

    private var hasInitializedAchievements: Boolean
        get() = prefs.getBoolean("has_init_achievements", false)
        set(value) = prefs.edit().putBoolean("has_init_achievements", value).apply()

    /**
     * Sync updates on app open / resume with throttling (minimum 10s between checks)
     */
    fun syncAppOpenUpdates(force: Boolean = false) {
        val networkService = NetworkService.getInstance(context)
        if (!networkService.isAuthenticated()) return

        val now = System.currentTimeMillis()
        if (!force && (now - lastSyncTimestamp) < 10_000L) {
            return
        }

        if (isSyncing) return
        isSyncing = true

        scope.launch(Dispatchers.IO) {
            try {
                val result = networkService.getAppUpdates()
                lastSyncTimestamp = System.currentTimeMillis()

                if (result is NetworkResult.Success) {
                    val updates = result.data
                    val dataManager = DataManager.getInstance(context)

                    // Check newly unlocked / received items
                    checkNewAchievements(updates.achievements)
                    checkFriendRequests(updates.friendRequests)
                    checkStreakRequests(updates.streakRequests)
                    checkFreezeInfo(updates.freezeInfo)
                }
            } catch (e: Exception) {
                android.util.Log.e("AchievementToastManager", "syncAppOpenUpdates failed", e)
            } finally {
                isSyncing = false
            }
        }
    }

    fun checkNewAchievements(achievements: List<AchievementDetail>) {
        val unlockedItems = achievements.filter { it.unlocked }

        if (!hasInitializedAchievements && seenUnlockedKeys.isEmpty()) {
            val initialSet = unlockedItems.map { "${it.id}" }.toMutableSet()
            seenUnlockedKeys = initialSet
            hasInitializedAchievements = true
            return
        }
        hasInitializedAchievements = true

        val localSeen = seenUnlockedKeys
        val newItems = mutableListOf<AchievementDetail>()

        for (item in unlockedItems) {
            val key = "${item.id}"
            if (!localSeen.contains(key)) {
                localSeen.add(key)
                newItems.add(item)
            }
        }

        if (newItems.isNotEmpty()) {
            seenUnlockedKeys = localSeen
            scope.launch(Dispatchers.Main) {
                if (newItems.count() > 1) {
                    showToast(
                        name = "${newItems.count()} achievements unlocked",
                        category = "multi",
                        icon = "sparkles",
                        count = newItems.count()
                    )
                } else if (newItems.isNotEmpty()) {
                    val single = newItems.first()
                    showToast(
                        name = single.name,
                        category = single.category,
                        icon = single.icon,
                        count = 1
                    )
                }

                // Acknowledge on server
                scope.launch(Dispatchers.IO) {
                    NetworkService.getInstance(context).markAchievementsNotified(newItems.map { it.id })
                }
            }
        }
    }

    fun checkFriendRequests(requests: List<FriendRequest>) {
        val localSeen = seenFriendRequestKeys
        val newRequesters = mutableListOf<String>()

        for (req in requests) {
            val key = "${req.id}"
            if (!localSeen.contains(key)) {
                localSeen.add(key)
                req.requester?.username?.let { if (it.isNotEmpty()) newRequesters.add(it) }
            }
        }

        if (newRequesters.isNotEmpty()) {
            seenFriendRequestKeys = localSeen
            scope.launch(Dispatchers.Main) {
                if (newRequesters.count() > 1) {
                    showToast(
                        name = "${newRequesters.count()} friend requests received",
                        category = "friend_request",
                        icon = "person_add",
                        count = newRequesters.count()
                    )
                } else {
                    showToast(
                        name = "${newRequesters.first()} sent a friend request",
                        category = "friend_request",
                        icon = "person_add"
                    )
                }
            }
        }
    }

    fun checkStreakRequests(requests: List<FriendStreakRequest>) {
        val localSeen = seenStreakRequestKeys
        val newRequesters = mutableListOf<String>()

        for (req in requests) {
            val key = "${req.id}"
            if (!localSeen.contains(key)) {
                localSeen.add(key)
                req.requester?.username?.let { if (it.isNotEmpty()) newRequesters.add(it) }
            }
        }

        if (newRequesters.isNotEmpty()) {
            seenStreakRequestKeys = localSeen
            scope.launch(Dispatchers.Main) {
                if (newRequesters.count() > 1) {
                    showToast(
                        name = "${newRequesters.count()} streak requests received",
                        category = "streak_request",
                        icon = "local_fire_department",
                        count = newRequesters.count()
                    )
                } else {
                    showToast(
                        name = "${newRequesters.first()} sent a streak request",
                        category = "streak_request",
                        icon = "local_fire_department"
                    )
                }
            }
        }
    }

    fun checkFreezeInfo(freezeInfo: FreezeInfoResponse, isUserPurchase: Boolean = false) {
        if (lastSeenGiftedFreezeID == -1) {
            lastSeenGiftedFreezeID = freezeInfo.latestGift?.id ?: 0
            lastAvailableFreezesCount = freezeInfo.availableFreezes
            return
        }

        val latestGift = freezeInfo.latestGift
        if (latestGift != null && !isUserPurchase) {
            if (latestGift.id != lastSeenGiftedFreezeID) {
                lastSeenGiftedFreezeID = latestGift.id
                val sender = latestGift.giftedBy
                scope.launch(Dispatchers.Main) {
                    showGiftedFreezeToast(sender)
                }
                lastAvailableFreezesCount = freezeInfo.availableFreezes
                return
            }
        }

        if (lastAvailableFreezesCount >= 0) {
            val diff = freezeInfo.availableFreezes - lastAvailableFreezesCount
            if (diff > 0 && !isUserPurchase) {
                scope.launch(Dispatchers.Main) {
                    showGiftedFreezeToast(count = diff)
                }
            }
        }
        lastAvailableFreezesCount = freezeInfo.availableFreezes
    }

    fun showGiftedFreezeToast(fromUsername: String? = null, count: Int = 1) {
        val nameText = when {
            !fromUsername.isNullOrEmpty() -> "$fromUsername gifted you a freeze!"
            count > 1 -> "Received $count gifted streak freezes!"
            else -> "Received a gifted streak freeze!"
        }
        showToast(
            name = nameText,
            category = "gift_freeze",
            icon = "ac_unit"
        )
    }

    fun showToast(name: String, category: String, icon: String? = null, count: Int = 1) {
        val toast = AchievementToastItem(
            name = name,
            category = category,
            icon = icon,
            count = count
        )
        enqueueToast(toast)
    }

    private fun enqueueToast(item: AchievementToastItem) {
        if (toastQueue.any { it.name == item.name } || _currentToast.value?.name == item.name) return
        toastQueue.add(item)
        processQueue()
    }

    private fun processQueue() {
        if (_currentToast.value != null || toastQueue.isEmpty()) return

        val nextToast = toastQueue.removeAt(0)
        _currentToast.value = nextToast

        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(4000L) // Auto-dismiss after 4 seconds
            dismissCurrentToast()
        }
    }

    fun dismissCurrentToast() {
        dismissJob?.cancel()
        dismissJob = null
        _currentToast.value = null

        scope.launch {
            delay(300L)
            processQueue()
        }
    }

    fun resetState() {
        prefs.edit().clear().apply()
        lastSyncTimestamp = 0
        isSyncing = false
        _currentToast.value = null
        toastQueue.clear()
        dismissJob?.cancel()
        dismissJob = null
    }

    companion object {
        @Volatile
        private var instance: AchievementToastManager? = null

        fun getInstance(context: Context): AchievementToastManager {
            return instance ?: synchronized(this) {
                instance ?: AchievementToastManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
