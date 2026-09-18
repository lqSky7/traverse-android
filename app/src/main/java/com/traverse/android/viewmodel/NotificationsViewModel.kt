package com.traverse.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.traverse.android.data.AppNotification
import com.traverse.android.data.CacheManager
import com.traverse.android.data.NetworkResult
import com.traverse.android.data.NetworkService
import com.traverse.android.data.NotificationPreferences
import com.traverse.android.data.NotificationsResponse
import com.traverse.android.data.QuietHours
import com.traverse.android.data.UpdateTypePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val nextCursor: Int? = null,
    val isLoadingMore: Boolean = false,
    val preferences: NotificationPreferences = NotificationPreferences(),
    val pushConfigured: Boolean = true
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val networkService by lazy { NetworkService.getInstance(application) }
    private val cacheManager by lazy { CacheManager.getInstance(application) }

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
        loadPreferences()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh) {
                val cached = cacheManager.getNotifications()
                if (cached != null) {
                    _uiState.update {
                        it.copy(
                            notifications = cached.notifications,
                            unreadCount = cached.unreadCount,
                            nextCursor = cached.nextCursor
                        )
                    }
                }
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = networkService.getNotifications(limit = 30)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    cacheManager.cacheNotifications(data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = data.notifications,
                            unreadCount = data.unreadCount,
                            nextCursor = data.nextCursor
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = networkService.getNotifications(limit = 30, cursor = cursor)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    _uiState.update { current ->
                        val existingIds = current.notifications.map { it.id }.toSet()
                        val uniqueNew = data.notifications.filter { it.id !in existingIds }
                        val combined = current.notifications + uniqueNew
                        cacheManager.cacheNotifications(
                            NotificationsResponse(
                                notifications = combined,
                                nextCursor = data.nextCursor,
                                unreadCount = data.unreadCount
                            )
                        )
                        current.copy(
                            isLoadingMore = false,
                            notifications = combined,
                            nextCursor = data.nextCursor,
                            unreadCount = data.unreadCount
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            when (val result = networkService.getUnreadNotificationCount()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(unreadCount = result.data.unreadCount) }
                }
                is NetworkResult.Error -> {}
            }
        }
    }

    fun markRead(id: Int) {
        val currentNotifications = _uiState.value.notifications
        val target = currentNotifications.firstOrNull { it.id == id } ?: return
        if (target.isRead) return

        val previousState = _uiState.value

        // Optimistic update
        _uiState.update { current ->
            val updated = current.notifications.map {
                if (it.id == id) it.copy(readAt = java.time.Instant.now().toString()) else it
            }
            current.copy(
                notifications = updated,
                unreadCount = maxOf(0, current.unreadCount - 1)
            )
        }

        viewModelScope.launch {
            when (val result = networkService.markNotificationRead(id)) {
                is NetworkResult.Success -> {
                    cacheManager.cacheNotifications(
                        NotificationsResponse(
                            notifications = _uiState.value.notifications,
                            nextCursor = _uiState.value.nextCursor,
                            unreadCount = _uiState.value.unreadCount
                        )
                    )
                }
                is NetworkResult.Error -> {
                    // Rollback
                    _uiState.value = previousState
                }
            }
        }
    }

    fun markAllRead() {
        val previousState = _uiState.value
        val nowIso = java.time.Instant.now().toString()

        // Optimistic update
        _uiState.update { current ->
            val updated = current.notifications.map {
                if (!it.isRead) it.copy(readAt = nowIso) else it
            }
            current.copy(
                notifications = updated,
                unreadCount = 0
            )
        }

        viewModelScope.launch {
            when (val result = networkService.markAllNotificationsRead()) {
                is NetworkResult.Success -> {
                    cacheManager.cacheNotifications(
                        NotificationsResponse(
                            notifications = _uiState.value.notifications,
                            nextCursor = _uiState.value.nextCursor,
                            unreadCount = 0
                        )
                    )
                }
                is NetworkResult.Error -> {
                    // Rollback
                    _uiState.value = previousState
                }
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            val cached = cacheManager.getNotificationPrefs()
            if (cached != null) {
                _uiState.update { it.copy(preferences = cached) }
            }

            when (val result = networkService.getNotificationPreferences()) {
                is NetworkResult.Success -> {
                    val prefs = result.data.preferences
                    cacheManager.cacheNotificationPrefs(prefs)
                    _uiState.update {
                        it.copy(
                            preferences = prefs,
                            pushConfigured = result.data.pushConfigured
                        )
                    }
                }
                is NetworkResult.Error -> {}
            }
        }
    }

    fun updatePreferences(
        types: List<UpdateTypePreference>? = null,
        quietHours: QuietHours? = null
    ) {
        val previousPrefs = _uiState.value.preferences

        // Optimistic update
        _uiState.update { current ->
            var updatedTypes = current.preferences.types
            if (types != null) {
                val updatesMap = types.associateBy { it.type }
                updatedTypes = current.preferences.types.map { existing ->
                    val update = updatesMap[existing.type]
                    if (update != null) {
                        existing.copy(
                            push = update.push ?: existing.push,
                            inApp = update.inApp ?: existing.inApp
                        )
                    } else existing
                }
            }
            val updatedQuiet = quietHours ?: current.preferences.quietHours
            val newPrefs = current.preferences.copy(types = updatedTypes, quietHours = updatedQuiet)
            current.copy(preferences = newPrefs)
        }

        viewModelScope.launch {
            when (val result = networkService.updateNotificationPreferences(types, quietHours)) {
                is NetworkResult.Success -> {
                    val serverPrefs = result.data.preferences
                    cacheManager.cacheNotificationPrefs(serverPrefs)
                    _uiState.update { it.copy(preferences = serverPrefs) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(preferences = previousPrefs) }
                }
            }
        }
    }

    fun clear() {
        _uiState.value = NotificationsUiState()
        cacheManager.clearCache(CacheManager.KEY_NOTIFICATIONS)
        cacheManager.clearCache(CacheManager.KEY_NOTIFICATION_PREFS)
    }
}
