package com.traverse.android.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Centralized caching system for Traverse app data.
 * Uses SharedPreferences with JSON serialization for persistence.
 * Cache entries have TTL (time-to-live) to auto-expire stale data.
 */
class CacheManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    companion object {
        private const val PREFS_NAME = "traverse_cache"
        private const val KEY_PREFIX = "cache_"
        private const val TIMESTAMP_SUFFIX = "_timestamp"
        
        // Cache keys
        const val KEY_USER_STATS = "user_stats"
        const val KEY_SOLVE_STATS = "solve_stats"
        const val KEY_RECENT_SOLVES = "recent_solves"
        const val KEY_ACHIEVEMENT_STATS = "achievement_stats"
        const val KEY_ALL_ACHIEVEMENTS = "all_achievements"
        const val KEY_FREEZE_DATES = "freeze_dates"
        const val KEY_REVISION_GROUPS = "revision_groups"
        const val KEY_REVISION_STATS = "revision_stats"
        const val KEY_REVISION_MODE = "revision_mode"
        
        // Friends cache keys
        const val KEY_FRIENDS = "friends"
        const val KEY_RECEIVED_REQUESTS = "received_requests"
        const val KEY_SENT_REQUESTS = "sent_requests"
        const val KEY_USER_PROFILE = "user_profile"
        const val KEY_FRIEND_SOLVES = "friend_solves"
        const val KEY_FRIEND_ACHIEVEMENTS = "friend_achievements"
        
        // Profile image cache
        const val KEY_PROFILE_IMAGE = "profile_image"
        
        // Cache TTL in milliseconds (15 minutes for frequently changing, 1 hour for stable)
        private const val TTL_SHORT = 15 * 60 * 1000L  // 15 min
        private const val TTL_LONG = 60 * 60 * 1000L   // 1 hour
        
        @Volatile
        private var instance: CacheManager? = null
        
        fun getInstance(context: Context): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // MARK: - Generic cache operations
    
    private fun getKey(key: String) = "$KEY_PREFIX$key"
    private fun getTimestampKey(key: String) = "$KEY_PREFIX$key$TIMESTAMP_SUFFIX"
    
    private fun isCacheValid(key: String, ttl: Long): Boolean {
        val timestamp = prefs.getLong(getTimestampKey(key), 0)
        return System.currentTimeMillis() - timestamp < ttl
    }
    
    private inline fun <reified T> saveToCache(key: String, data: T) {
        val jsonString = json.encodeToString(data)
        prefs.edit()
            .putString(getKey(key), jsonString)
            .putLong(getTimestampKey(key), System.currentTimeMillis())
            .apply()
    }
    
    private inline fun <reified T> getFromCache(key: String, ttl: Long): T? {
        if (!isCacheValid(key, ttl)) return null
        
        val jsonString = prefs.getString(getKey(key), null) ?: return null
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    // MARK: - Home Tab Cache
    
    fun cacheUserStats(data: UserStats) = saveToCache(KEY_USER_STATS, data)
    fun getUserStats(): UserStats? = getFromCache(KEY_USER_STATS, TTL_SHORT)
    
    fun cacheSolveStats(data: SolveStats) = saveToCache(KEY_SOLVE_STATS, data)
    fun getSolveStats(): SolveStats? = getFromCache(KEY_SOLVE_STATS, TTL_SHORT)
    
    fun cacheRecentSolves(data: SolvesResponse) = saveToCache(KEY_RECENT_SOLVES, data)
    fun getRecentSolves(): SolvesResponse? = getFromCache(KEY_RECENT_SOLVES, TTL_SHORT)
    
    fun cacheAchievementStats(data: AchievementStats) = saveToCache(KEY_ACHIEVEMENT_STATS, data)
    fun getAchievementStats(): AchievementStats? = getFromCache(KEY_ACHIEVEMENT_STATS, TTL_LONG)
    
    fun cacheAllAchievements(data: AllAchievementsResponse) = saveToCache(KEY_ALL_ACHIEVEMENTS, data)
    fun getAllAchievements(): AllAchievementsResponse? = getFromCache(KEY_ALL_ACHIEVEMENTS, TTL_LONG)
    
    fun cacheFreezeDates(data: FreezeDatesResponse) = saveToCache(KEY_FREEZE_DATES, data)
    fun getFreezeDates(): FreezeDatesResponse? = getFromCache(KEY_FREEZE_DATES, TTL_LONG)
    
    // MARK: - Revisions Tab Cache
    
    fun cacheRevisionGroups(data: GroupedRevisionsResponse, mode: String) {
        saveToCache("${KEY_REVISION_GROUPS}_$mode", data)
    }
    fun getRevisionGroups(mode: String): GroupedRevisionsResponse? = 
        getFromCache("${KEY_REVISION_GROUPS}_$mode", TTL_SHORT)
    
    fun cacheRevisionStats(data: RevisionStatsResponse, mode: String) {
        saveToCache("${KEY_REVISION_STATS}_$mode", data)
    }
    fun getRevisionStats(mode: String): RevisionStatsResponse? = 
        getFromCache("${KEY_REVISION_STATS}_$mode", TTL_SHORT)
    
    fun cacheRevisionMode(mode: String) {
        prefs.edit().putString(KEY_REVISION_MODE, mode).apply()
    }
    fun getRevisionMode(): String = prefs.getString(KEY_REVISION_MODE, "normal") ?: "normal"
    
    // MARK: - Subscription Status Cache
    
    private val KEY_SUBSCRIPTION_STATUS = "subscription_status"
    private val KEY_SUBSCRIPTION_LAST_CHECK = "subscription_last_check"
    
    fun cacheSubscriptionStatus(isActive: Boolean) {
        prefs.edit()
            .putBoolean(getKey(KEY_SUBSCRIPTION_STATUS), isActive)
            .putLong(getKey(KEY_SUBSCRIPTION_LAST_CHECK), System.currentTimeMillis())
            .apply()
    }
    
    fun getSubscriptionStatus(): Boolean {
        return prefs.getBoolean(getKey(KEY_SUBSCRIPTION_STATUS), false)
    }
    
    fun shouldCheckSubscription(): Boolean {
        val lastCheck = prefs.getLong(getKey(KEY_SUBSCRIPTION_LAST_CHECK), 0)
        val hoursSinceCheck = (System.currentTimeMillis() - lastCheck) / (1000 * 60 * 60)
        return hoursSinceCheck >= 24 // Check once per day
    }
    
    // MARK: - Friends Tab Cache
    
    fun cacheFriends(data: FriendsListResponse) = saveToCache(KEY_FRIENDS, data)
    fun getFriends(): FriendsListResponse? = getFromCache(KEY_FRIENDS, TTL_SHORT)
    
    fun cacheReceivedRequests(data: FriendRequestsResponse) = saveToCache(KEY_RECEIVED_REQUESTS, data)
    fun getReceivedRequests(): FriendRequestsResponse? = getFromCache(KEY_RECEIVED_REQUESTS, TTL_SHORT)
    
    fun cacheSentRequests(data: FriendRequestsResponse) = saveToCache(KEY_SENT_REQUESTS, data)
    fun getSentRequests(): FriendRequestsResponse? = getFromCache(KEY_SENT_REQUESTS, TTL_SHORT)
    
    fun cacheUserProfile(username: String, data: UserProfileResponse) {
        saveToCache("${KEY_USER_PROFILE}_$username", data)
    }
    fun getUserProfile(username: String): UserProfileResponse? = 
        getFromCache("${KEY_USER_PROFILE}_$username", TTL_SHORT)
    
    fun cacheFriendSolves(username: String, data: FriendSolvesResponse) {
        saveToCache("${KEY_FRIEND_SOLVES}_$username", data)
    }
    fun getFriendSolves(username: String): FriendSolvesResponse? = 
        getFromCache("${KEY_FRIEND_SOLVES}_$username", TTL_SHORT)
    
    fun cacheFriendAchievements(username: String, data: FriendAchievementsResponse) {
        saveToCache("${KEY_FRIEND_ACHIEVEMENTS}_$username", data)
    }
    fun getFriendAchievements(username: String): FriendAchievementsResponse? = 
        getFromCache("${KEY_FRIEND_ACHIEVEMENTS}_$username", TTL_SHORT)
    
    // MARK: - Profile Image Cache (permanent, no TTL)
    
    fun cacheProfileImage(imageUrl: String) {
        prefs.edit()
            .putString(getKey(KEY_PROFILE_IMAGE), imageUrl)
            .apply()
    }
    
    fun getProfileImage(): String? {
        return prefs.getString(getKey(KEY_PROFILE_IMAGE), null)
    }
    
    // Save profile image file path locally
    fun cacheProfileImageFile(filePath: String) {
        prefs.edit()
            .putString(getKey("${KEY_PROFILE_IMAGE}_file"), filePath)
            .apply()
    }
    
    fun getProfileImageFile(): String? {
        return prefs.getString(getKey("${KEY_PROFILE_IMAGE}_file"), null)
    }
    
    // MARK: - Update Check
    
    fun getLastUpdateCheckTime(): Long {
        return prefs.getLong("last_update_check_time", 0)
    }
    
    fun saveLastUpdateCheckTime(timestamp: Long) {
        prefs.edit()
            .putLong("last_update_check_time", timestamp)
            .apply()
    }
    
    // MARK: - Utility
    
    fun clearCache(key: String) {
        prefs.edit()
            .remove(getKey(key))
            .remove(getTimestampKey(key))
            .apply()
    }
    
    fun clearAllCache() {
        // Delete any cached profile image file
        getProfileImageFile()?.let { filePath ->
            try {
                java.io.File(filePath).delete()
            } catch (e: Exception) {
                // Ignore if file deletion fails
            }
        }
        
        prefs.edit().clear().apply()
    }
    
    fun invalidateHomeCache() {
        listOf(KEY_USER_STATS, KEY_SOLVE_STATS, KEY_RECENT_SOLVES, KEY_ACHIEVEMENT_STATS, KEY_FREEZE_DATES)
            .forEach { clearCache(it) }
    }
    
    fun invalidateRevisionCache() {
        listOf("${KEY_REVISION_GROUPS}_normal", "${KEY_REVISION_GROUPS}_ml",
               "${KEY_REVISION_STATS}_normal", "${KEY_REVISION_STATS}_ml")
            .forEach { clearCache(it) }
    }
    
    fun invalidateFriendsCache() {
        listOf(KEY_FRIENDS, KEY_RECEIVED_REQUESTS, KEY_SENT_REQUESTS)
            .forEach { clearCache(it) }
    }
}
