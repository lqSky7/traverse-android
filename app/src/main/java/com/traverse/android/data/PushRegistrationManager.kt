package com.traverse.android.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages FCM push token retrieval, registration with the Traverse backend,
 * and unregistration on logout.
 */
class PushRegistrationManager private constructor(private val context: Context) {

    private val networkService = NetworkService.getInstance(context)
    private val cacheManager = CacheManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isRegistered = MutableStateFlow(cacheManager.getPushToken() != null)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    /**
     * Retrieves the current FCM registration token and registers it with the backend
     * if authenticated and not already registered.
     */
    fun register(force: Boolean = false) {
        if (!networkService.isAuthenticated()) return

        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val cachedToken = cacheManager.getPushToken()

                if (force || cachedToken != token) {
                    when (val result = networkService.registerPushToken(token, platform = "android")) {
                        is NetworkResult.Success -> {
                            cacheManager.cachePushToken(token)
                            _isRegistered.value = true
                            Log.d(TAG, "Push token registered successfully")
                        }
                        is NetworkResult.Error -> {
                            Log.e(TAG, "Failed to register push token: ${result.message}")
                        }
                    }
                } else {
                    _isRegistered.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching or registering FCM token", e)
            }
        }
    }

    /**
     * Handles a refreshed FCM token received via [TraverseMessagingService].
     */
    fun onNewToken(token: String) {
        scope.launch {
            if (networkService.isAuthenticated()) {
                when (val result = networkService.registerPushToken(token, platform = "android")) {
                    is NetworkResult.Success -> {
                        cacheManager.cachePushToken(token)
                        _isRegistered.value = true
                        Log.d(TAG, "Refreshed push token registered successfully")
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "Failed to register refreshed push token: ${result.message}")
                    }
                }
            } else {
                cacheManager.cachePushToken(token)
            }
        }
    }

    /**
     * Deletes the registered push token from the backend and clears local push token cache.
     * Called on logout.
     */
    suspend fun unregisterFromServer() {
        val cachedToken = cacheManager.getPushToken()
        if (cachedToken != null && networkService.isAuthenticated()) {
            try {
                networkService.deletePushToken(cachedToken, platform = "android")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting push token from server", e)
            }
        }
        cacheManager.cachePushToken(null)
        _isRegistered.value = false
    }

    companion object {
        private const val TAG = "PushRegManager"

        @Volatile
        private var instance: PushRegistrationManager? = null

        fun getInstance(context: Context): PushRegistrationManager {
            return instance ?: synchronized(this) {
                instance ?: PushRegistrationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
