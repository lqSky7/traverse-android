package com.traverse.android.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 1:1 Kotlin port of iOS DataManager.swift.
 * Acts as the single central source of truth, managing reactive StateFlows,
 * cold-start JSON disk persistence in internal filesDir, solve deduplication,
 * and concurrent atomic fetch updates.
 */
class DataManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val networkService by lazy { NetworkService.getInstance(context) }

    // MARK: - Reactive StateFlows (Friends & Streaks)
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _receivedRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val receivedRequests: StateFlow<List<FriendRequest>> = _receivedRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests.asStateFlow()

    private val _receivedStreakRequests = MutableStateFlow<List<FriendStreakRequest>>(emptyList())
    val receivedStreakRequests: StateFlow<List<FriendStreakRequest>> = _receivedStreakRequests.asStateFlow()

    private val _sentStreakRequests = MutableStateFlow<List<FriendStreakRequest>>(emptyList())
    val sentStreakRequests: StateFlow<List<FriendStreakRequest>> = _sentStreakRequests.asStateFlow()

    private val _friendStreaks = MutableStateFlow<List<FriendStreak>>(emptyList())
    val friendStreaks: StateFlow<List<FriendStreak>> = _friendStreaks.asStateFlow()

    // MARK: - Reactive StateFlows (Home & Stats)
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _submissionStats = MutableStateFlow<SubmissionStats?>(null)
    val submissionStats: StateFlow<SubmissionStats?> = _submissionStats.asStateFlow()

    private val _solveStats = MutableStateFlow<SolveStats?>(null)
    val solveStats: StateFlow<SolveStats?> = _solveStats.asStateFlow()

    private val _achievementStats = MutableStateFlow<AchievementStats?>(null)
    val achievementStats: StateFlow<AchievementStats?> = _achievementStats.asStateFlow()

    private val _allAchievements = MutableStateFlow<List<AchievementDetail>>(emptyList())
    val allAchievements: StateFlow<List<AchievementDetail>> = _allAchievements.asStateFlow()

    private val _recentSolves = MutableStateFlow<List<Solve>>(emptyList())
    val recentSolves: StateFlow<List<Solve>> = _recentSolves.asStateFlow()

    private val _todayRevisions = MutableStateFlow<List<Revision>>(emptyList())
    val todayRevisions: StateFlow<List<Revision>> = _todayRevisions.asStateFlow()

    private val _completedRevisions = MutableStateFlow<List<Revision>>(emptyList())
    val completedRevisions: StateFlow<List<Revision>> = _completedRevisions.asStateFlow()

    private val _lastFetchTimestamp = MutableStateFlow<Long?>(null)
    val lastFetchTimestamp: StateFlow<Long?> = _lastFetchTimestamp.asStateFlow()

    // MARK: - Reactive StateFlows (Revisions)
    private val _revisionGroups = MutableStateFlow<List<RevisionGroup>>(emptyList())
    val revisionGroups: StateFlow<List<RevisionGroup>> = _revisionGroups.asStateFlow()

    private val _revisionStats = MutableStateFlow<RevisionStatsResponse?>(null)
    val revisionStats: StateFlow<RevisionStatsResponse?> = _revisionStats.asStateFlow()

    private val _revisionMode = MutableStateFlow("normal")
    val revisionMode: StateFlow<String> = _revisionMode.asStateFlow()

    private var hasFetchedInitialData = false

    val isCacheFresh: Boolean
        get() {
            val timestamp = _lastFetchTimestamp.value ?: return false
            val cacheAgeSeconds = (System.currentTimeMillis() - timestamp) / 1000
            return cacheAgeSeconds < 7200 // 2 hours in seconds
        }

    val hasData: Boolean
        get() = hasFetchedInitialData || _userStats.value != null || _recentSolves.value.isNotEmpty()

    init {
        loadPersistedData()
    }

    // MARK: - Disk Persistence

    private fun getFile(filename: String): File {
        return File(context.filesDir, filename)
    }

    private inline fun <reified T> loadFile(filename: String): T? {
        val file = getFile(filename)
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "Failed to load $filename", e)
            null
        }
    }

    private inline fun <reified T> saveFile(data: T, filename: String) {
        try {
            val file = getFile(filename)
            val content = json.encodeToString(data)
            file.writeText(content)
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "Failed to save $filename", e)
        }
    }

    private fun loadPersistedData() {
        loadFile<List<Friend>>("friends.json")?.let { _friends.value = it }
        loadFile<List<FriendRequest>>("receivedRequests.json")?.let { _receivedRequests.value = it }
        loadFile<List<FriendRequest>>("sentRequests.json")?.let { _sentRequests.value = it }
        loadFile<List<FriendStreakRequest>>("receivedStreakRequests.json")?.let { _receivedStreakRequests.value = it }
        loadFile<List<FriendStreakRequest>>("sentStreakRequests.json")?.let { _sentStreakRequests.value = it }
        loadFile<List<FriendStreak>>("friendStreaks.json")?.let { _friendStreaks.value = it }

        loadFile<UserStats>("userStats.json")?.let { _userStats.value = it }
        loadFile<SubmissionStats>("submissionStats.json")?.let { _submissionStats.value = it }
        loadFile<SolveStats>("solveStats.json")?.let { _solveStats.value = it }
        loadFile<AchievementStats>("achievementStats.json")?.let { _achievementStats.value = it }
        loadFile<List<AchievementDetail>>("allAchievements.json")?.let { _allAchievements.value = it }
        loadFile<List<Solve>>("recentSolves.json")?.let { _recentSolves.value = it }
        loadFile<List<Revision>>("todayRevisions.json")?.let { _todayRevisions.value = it }
        loadFile<List<Revision>>("completedRevisions.json")?.let { _completedRevisions.value = it }

        loadFile<Long>("lastFetchTimestamp.json")?.let { _lastFetchTimestamp.value = it }

        loadFile<List<RevisionGroup>>("revisionGroups.json")?.let { _revisionGroups.value = it }
        loadFile<RevisionStatsResponse>("revisionStats.json")?.let { _revisionStats.value = it }
        loadFile<String>("revisionMode.json")?.let { _revisionMode.value = it }

        if (_userStats.value != null || _recentSolves.value.isNotEmpty() || _friends.value.isNotEmpty()) {
            hasFetchedInitialData = true
        }
    }

    fun persistData() {
        scope.launch(Dispatchers.IO) {
            saveFile(_friends.value, "friends.json")
            saveFile(_receivedRequests.value, "receivedRequests.json")
            saveFile(_sentRequests.value, "sentRequests.json")
            saveFile(_receivedStreakRequests.value, "receivedStreakRequests.json")
            saveFile(_sentStreakRequests.value, "sentStreakRequests.json")
            saveFile(_friendStreaks.value, "friendStreaks.json")

            _userStats.value?.let { saveFile(it, "userStats.json") }
            _submissionStats.value?.let { saveFile(it, "submissionStats.json") }
            _solveStats.value?.let { saveFile(it, "solveStats.json") }
            _achievementStats.value?.let { saveFile(it, "achievementStats.json") }
            saveFile(_allAchievements.value, "allAchievements.json")
            saveFile(_recentSolves.value, "recentSolves.json")
            saveFile(_todayRevisions.value, "todayRevisions.json")
            saveFile(_completedRevisions.value, "completedRevisions.json")

            _lastFetchTimestamp.value?.let { saveFile(it, "lastFetchTimestamp.json") }

            saveFile(_revisionGroups.value, "revisionGroups.json")
            _revisionStats.value?.let { saveFile(it, "revisionStats.json") }
            saveFile(_revisionMode.value, "revisionMode.json")
        }
    }

    // MARK: - Solve Merging & Deduplication (1:1 with iOS mergeAndPersistSolves)

    fun mergeAndPersistSolves(fetchedSolves: List<Solve>): List<Solve> {
        val solvesMap = mutableMapOf<String, Solve>()

        // 1. Load current solves into map
        _recentSolves.value.forEach { solve ->
            val key = "${solve.problem.platform}:${solve.problem.slug}"
            solvesMap[key] = solve
        }

        // 2. Merge newly fetched solves
        fetchedSolves.forEach { solve ->
            val key = "${solve.problem.platform}:${solve.problem.slug}"
            solvesMap[key] = solve
        }

        // 3. Sort descending by solvedAt
        val merged = solvesMap.values.sortedByDescending { it.solvedAt }
        _recentSolves.value = merged
        saveFile(merged, "recentSolves.json")
        return merged
    }

    // MARK: - Revision Mode Management

    fun setRevisionMode(mode: String) {
        _revisionMode.value = mode
        saveFile(mode, "revisionMode.json")
    }

    // MARK: - Atomic Parallel Fetch (1:1 with iOS fetchAllData)

    suspend fun fetchAllData(username: String): Unit = withContext(Dispatchers.IO) {
        val mode = _revisionMode.value

        // Execute all 10 network requests concurrently
        val friendsDeferred = async { networkService.getFriends() }
        val receivedRequestsDeferred = async { networkService.getReceivedFriendRequests() }
        val sentRequestsDeferred = async { networkService.getSentFriendRequests() }
        val receivedStreakRequestsDeferred = async { networkService.getReceivedFriendStreakRequests() }
        val sentStreakRequestsDeferred = async { networkService.getSentFriendStreakRequests() }
        val friendStreaksDeferred = async { networkService.getFriendStreaks() }
        val userStatsDeferred = async { networkService.getUserStats() }
        val submissionStatsDeferred = async { networkService.getSubmissionStats() }
        val solveStatsDeferred = async { networkService.getSolveStats() }
        val achievementStatsDeferred = async { networkService.getAchievementStats() }
        val allAchievementsDeferred = async { networkService.getAllAchievements() }
        val recentSolvesDeferred = async { networkService.getSolves(limit = 200) }
        val revisionsDeferred = async { networkService.getRevisions(upcoming = true, limit = 50, type = mode) }
        val groupedRevisionsDeferred = async { networkService.getGroupedRevisions(includeCompleted = true, type = mode) }
        val revisionStatsDeferred = async { networkService.getRevisionStats(type = mode) }

        // Await results
        val friendsRes = friendsDeferred.await()
        val receivedReqRes = receivedRequestsDeferred.await()
        val sentReqRes = sentRequestsDeferred.await()
        val receivedStreakReqRes = receivedStreakRequestsDeferred.await()
        val sentStreakReqRes = sentStreakRequestsDeferred.await()
        val friendStreaksRes = friendStreaksDeferred.await()
        val userStatsRes = userStatsDeferred.await()
        val submissionStatsRes = submissionStatsDeferred.await()
        val solveStatsRes = solveStatsDeferred.await()
        val achievementStatsRes = achievementStatsDeferred.await()
        val allAchievementsRes = allAchievementsDeferred.await()
        val recentSolvesRes = recentSolvesDeferred.await()
        val revisionsRes = revisionsDeferred.await()
        val groupedRevisionsRes = groupedRevisionsDeferred.await()
        val revisionStatsRes = revisionStatsDeferred.await()

        // Process Friends data
        if (friendsRes is NetworkResult.Success) _friends.value = friendsRes.data.friends
        if (receivedReqRes is NetworkResult.Success) _receivedRequests.value = receivedReqRes.data.requests
        if (sentReqRes is NetworkResult.Success) _sentRequests.value = sentReqRes.data.requests
        if (receivedStreakReqRes is NetworkResult.Success) _receivedStreakRequests.value = receivedStreakReqRes.data.requests
        if (sentStreakReqRes is NetworkResult.Success) _sentStreakRequests.value = sentStreakReqRes.data.requests
        if (friendStreaksRes is NetworkResult.Success) _friendStreaks.value = friendStreaksRes.data.streaks

        // Process Home & Stats data
        if (userStatsRes is NetworkResult.Success) _userStats.value = userStatsRes.data
        if (submissionStatsRes is NetworkResult.Success) _submissionStats.value = submissionStatsRes.data
        if (solveStatsRes is NetworkResult.Success) _solveStats.value = solveStatsRes.data
        if (achievementStatsRes is NetworkResult.Success) _achievementStats.value = achievementStatsRes.data
        if (allAchievementsRes is NetworkResult.Success) _allAchievements.value = allAchievementsRes.data.achievements

        // Process Solves & merge into local cache
        if (recentSolvesRes is NetworkResult.Success) {
            mergeAndPersistSolves(recentSolvesRes.data.solves)
        }

        // Process Revisions data: filter due today + overdue
        val today = LocalDate.now()
        if (revisionsRes is NetworkResult.Success) {
            val dueAndOverdue = revisionsRes.data.revisions.filter { revision ->
                !revision.scheduledDate.isAfter(today)
            }
            _todayRevisions.value = dueAndOverdue
        }

        // Extract completed revisions from last 7 days
        val sevenDaysAgo = today.minusDays(7)
        if (groupedRevisionsRes is NetworkResult.Success) {
            _revisionGroups.value = groupedRevisionsRes.data.groups
            val recentCompleted = groupedRevisionsRes.data.groups.flatMap { it.revisions }
                .filter { revision ->
                    revision.isCompleted && revision.completedDate?.toLocalDate()?.let { !it.isBefore(sevenDaysAgo) } ?: false
                }
            _completedRevisions.value = recentCompleted
        }

        if (revisionStatsRes is NetworkResult.Success) {
            _revisionStats.value = revisionStatsRes.data
        }

        hasFetchedInitialData = true
        _lastFetchTimestamp.value = System.currentTimeMillis()

        persistData()
    }

    // MARK: - Session Cleanup

    fun clearAllData() {
        _friends.value = emptyList()
        _receivedRequests.value = emptyList()
        _sentRequests.value = emptyList()
        _receivedStreakRequests.value = emptyList()
        _sentStreakRequests.value = emptyList()
        _friendStreaks.value = emptyList()

        _userStats.value = null
        _submissionStats.value = null
        _solveStats.value = null
        _achievementStats.value = null
        _allAchievements.value = emptyList()
        _recentSolves.value = emptyList()
        _todayRevisions.value = emptyList()
        _completedRevisions.value = emptyList()

        _revisionGroups.value = emptyList()
        _revisionStats.value = null
        _revisionMode.value = "normal"
        _lastFetchTimestamp.value = null

        hasFetchedInitialData = false

        // Delete persisted files
        val filenames = listOf(
            "friends.json", "receivedRequests.json", "sentRequests.json",
            "receivedStreakRequests.json", "sentStreakRequests.json", "friendStreaks.json",
            "userStats.json", "submissionStats.json", "solveStats.json",
            "achievementStats.json", "allAchievements.json", "recentSolves.json",
            "todayRevisions.json", "completedRevisions.json", "revisionGroups.json",
            "revisionStats.json", "revisionMode.json", "lastFetchTimestamp.json"
        )
        filenames.forEach { filename ->
            getFile(filename).delete()
        }
    }

    companion object {
        @Volatile
        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            return instance ?: synchronized(this) {
                instance ?: DataManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
