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
data class IncomingSocialRequest(
    val requestId: Int,
    val username: String,
    val createdAt: String,
    val isStreak: Boolean
) {
    val key: String get() = "${if (isStreak) "streak" else "friend"}:$requestId"
}

class AchievementToastManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs: SharedPreferences = context.getSharedPreferences("traverse_toasts_prefs", Context.MODE_PRIVATE)

    private val _currentToast = MutableStateFlow<AchievementToastItem?>(null)
    val currentToast: StateFlow<AchievementToastItem?> = _currentToast.asStateFlow()

    private val _incomingRequest = MutableStateFlow<IncomingSocialRequest?>(null)
    val incomingRequest = _incomingRequest.asStateFlow()
    private val _requestError = MutableStateFlow<String?>(null)
    val requestError = _requestError.asStateFlow()
    private val _isResponding = MutableStateFlow(false)
    val isResponding = _isResponding.asStateFlow()
    private val requestQueue = mutableListOf<IncomingSocialRequest>()
    private val presentedRequests = mutableSetOf<String>()

    private fun queueRequests(incoming: List<IncomingSocialRequest>, isStreak: Boolean) {
        val valid = incoming.map { it.key }.toSet()
        requestQueue.removeAll { it.isStreak == isStreak && it.key !in valid }
        _incomingRequest.value?.let {
            if (it.isStreak == isStreak && it.key !in valid && !_isResponding.value) _incomingRequest.value = null
        }
        incoming.asReversed().forEach {
            if (presentedRequests.add(it.key)) requestQueue.add(it)
        }
        presentNextRequest()
    }

    private fun presentNextRequest() {
        if (_incomingRequest.value != null || requestQueue.isEmpty()) return
        _requestError.value = null
        _incomingRequest.value = requestQueue.removeAt(0)
    }

    fun dismissRequest() {
        if (_isResponding.value) return
        _incomingRequest.value = null
        _requestError.value = null
        presentNextRequest()
    }

    fun respondToRequest(accept: Boolean) {
        val request = _incomingRequest.value ?: return
        if (_isResponding.value) return
        _isResponding.value = true
        _requestError.value = null
        responseJob = scope.launch {
            try {
                val service = NetworkService.getInstance(context)
                val result = if (request.isStreak) {
                    if (accept) service.acceptFriendStreakRequest(request.requestId)
                    else service.rejectFriendStreakRequest(request.requestId)
                } else {
                    if (accept) service.acceptFriendRequest(request.requestId)
                    else service.rejectFriendRequest(request.requestId)
                }
                when (result) {
                    is NetworkResult.Success -> {
                        _incomingRequest.value = null
                        presentNextRequest()
                        context.sendBroadcast(android.content.Intent("com.traverse.android.SOCIAL_REQUESTS_CHANGED").setPackage(context.packageName))
                    }
                    is NetworkResult.Error -> _requestError.value = result.message
                }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                _requestError.value = error.message ?: "Couldn't respond. Try again."
            } finally { _isResponding.value = false }
        }
    }

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
    private var syncJob: Job? = null
    private var responseJob: Job? = null

    fun syncAppOpenUpdates(force: Boolean = false) {
        scope.launch {
            val networkService = NetworkService.getInstance(context)
            if (!networkService.isAuthenticated()) return@launch
            val now = System.currentTimeMillis()
            if ((!force && now - lastSyncTimestamp < 10_000L) || isSyncing) return@launch
            isSyncing = true
            syncJob = launch {
                try {
                    // Requests don't depend on optional award/freeze updates.
                    val friends = launch {
                        val result = networkService.getReceivedFriendRequests()
                        if (result is NetworkResult.Success) queueRequests(result.data.requests.map {
                            IncomingSocialRequest(it.id, it.requester?.username ?: "Someone", it.createdAt, false)
                        }, false)
                    }
                    val streaks = launch {
                        val result = networkService.getReceivedFriendStreakRequests()
                        if (result is NetworkResult.Success) queueRequests(result.data.requests.map {
                            IncomingSocialRequest(it.id, it.requester?.username ?: "Someone", it.createdAt, true)
                        }, true)
                    }
                    val result = networkService.getAppUpdates()
                    if (result is NetworkResult.Success) {
                        checkNewAchievements(result.data.achievements)
                        checkFreezeInfo(result.data.freezeInfo)
                    }
                    friends.join()
                    streaks.join()
                    lastSyncTimestamp = System.currentTimeMillis()
                } finally { isSyncing = false }
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
        syncJob?.cancel()
        responseJob?.cancel()
        _incomingRequest.value = null
        _requestError.value = null
        _isResponding.value = false
        requestQueue.clear()
        presentedRequests.clear()
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
