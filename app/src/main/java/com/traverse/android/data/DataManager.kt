package com.traverse.android.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
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
import java.util.concurrent.atomic.AtomicLong

/**
 * 1:1 Kotlin port of iOS DataManager.swift.
 * Acts as the single central source of truth, managing reactive StateFlows,
 * cold-start JSON disk persistence in internal filesDir, solve deduplication,
 * and concurrent atomic fetch updates.
 */
class DataManager internal constructor(
    private val context: Context,
    private val networkService: NetworkService = NetworkService.getInstance(context)
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val persistenceLock = Any()
    private val persistenceGeneration = AtomicLong(0)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }


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

    /** Award shelves (Close Your Rings, Monthly Challenges, …) for the Awards hub. */
    private val _awardSections = MutableStateFlow<List<AwardSection>>(emptyList())
    val awardSections: StateFlow<List<AwardSection>> = _awardSections.asStateFlow()

    /** The challenge the Awards hub leads with — usually the running monthly challenge. */
    private val _featuredAward = MutableStateFlow<AchievementDetail?>(null)
    val featuredAward: StateFlow<AchievementDetail?> = _featuredAward.asStateFlow()

    private val _recentSolves = MutableStateFlow<List<Solve>>(emptyList())
    val recentSolves: StateFlow<List<Solve>> = _recentSolves.asStateFlow()

    /** YYYY-MM-DD strings for days the user froze their streak. Mirrors iOS `HomeViewModel.frozenDates`. */
    private val _frozenDates = MutableStateFlow<List<String>>(emptyList())
    val frozenDates: StateFlow<List<String>> = _frozenDates.asStateFlow()

    private val _todayRevisions = MutableStateFlow<List<Revision>>(emptyList())
    val todayRevisions: StateFlow<List<Revision>> = _todayRevisions.asStateFlow()

    private val _completedRevisions = MutableStateFlow<List<Revision>>(emptyList())
    val completedRevisions: StateFlow<List<Revision>> = _completedRevisions.asStateFlow()

    /**
     * Every revision the server returned, unfiltered.
     *
     * [completedRevisions] is narrowed to the last seven days, which is all the weekly-activity
     * card needs. The revision-load card compares a 7-day average against a **28-day** baseline
     * (15 days for a young account), so reading that narrowed list would silently compute a
     * baseline from a week of data and grade every user as "Optimal". The two views filter this
     * one list differently instead of each keeping their own copy.
     */
    private val _allRevisions = MutableStateFlow<List<Revision>>(emptyList())
    val allRevisions: StateFlow<List<Revision>> = _allRevisions.asStateFlow()

    private val _lastFetchTimestamp = MutableStateFlow<Long?>(null)
    val lastFetchTimestamp: StateFlow<Long?> = _lastFetchTimestamp.asStateFlow()

    // MARK: - Reactive StateFlows (Revisions)
    private val _revisionGroups = MutableStateFlow<List<RevisionGroup>>(emptyList())
    val revisionGroups: StateFlow<List<RevisionGroup>> = _revisionGroups.asStateFlow()

    private val _revisionStats = MutableStateFlow<RevisionStatsResponse?>(null)
    val revisionStats: StateFlow<RevisionStatsResponse?> = _revisionStats.asStateFlow()

    /** 7-day revision health score shown on the Home screen. */
    private val _revisionScore = MutableStateFlow<RevisionScoreResponse?>(null)
    val revisionScore: StateFlow<RevisionScoreResponse?> = _revisionScore.asStateFlow()

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
        val atomicFile = AtomicFile(getFile(filename))
        return try {
            val content = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "Failed to load $filename", e)
            null
        }
    }

    private inline fun <reified T> saveFile(data: T, filename: String) {
        val atomicFile = AtomicFile(getFile(filename))
        var output: java.io.FileOutputStream? = null
        try {
            val content = json.encodeToString(data)
            val outputStream = atomicFile.startWrite()
            output = outputStream
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(outputStream)
        } catch (e: Exception) {
            output?.let(atomicFile::failWrite)
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
        loadFile<List<AwardSection>>("awardSections.json")?.let { _awardSections.value = it }
        loadFile<AchievementDetail>("featuredAward.json")?.let { _featuredAward.value = it }
        loadFile<List<Solve>>("recentSolves.json")?.let { _recentSolves.value = it }
        loadFile<List<String>>("frozenDates.json")?.let { _frozenDates.value = it }
        loadFile<List<Revision>>("todayRevisions.json")?.let { _todayRevisions.value = it }
        loadFile<List<Revision>>("completedRevisions.json")?.let { _completedRevisions.value = it }
        loadFile<List<Revision>>("allRevisions.json")?.let { _allRevisions.value = it }

        loadFile<Long>("lastFetchTimestamp.json")?.let { _lastFetchTimestamp.value = it }

        loadFile<List<RevisionGroup>>("revisionGroups.json")?.let { _revisionGroups.value = it }
        loadFile<RevisionStatsResponse>("revisionStats.json")?.let { _revisionStats.value = it }
        loadFile<RevisionScoreResponse>("revisionScore.json")?.let { _revisionScore.value = it }

        if (_userStats.value != null || _recentSolves.value.isNotEmpty() || _friends.value.isNotEmpty() || _frozenDates.value.isNotEmpty()) {
            hasFetchedInitialData = true
        }
    }

    fun persistData() {
        val generation = persistenceGeneration.get()
        scope.launch(Dispatchers.IO) {
            synchronized(persistenceLock) {
                if (persistenceGeneration.get() != generation) return@synchronized
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
                saveFile(_awardSections.value, "awardSections.json")
                _featuredAward.value?.let { saveFile(it, "featuredAward.json") }
                saveFile(_recentSolves.value, "recentSolves.json")
                if (_frozenDates.value.isNotEmpty()) {
                    saveFile(_frozenDates.value, "frozenDates.json")
                }
                saveFile(_todayRevisions.value, "todayRevisions.json")
                saveFile(_completedRevisions.value, "completedRevisions.json")
                saveFile(_allRevisions.value, "allRevisions.json")

                _lastFetchTimestamp.value?.let { saveFile(it, "lastFetchTimestamp.json") }

                saveFile(_revisionGroups.value, "revisionGroups.json")
                _revisionStats.value?.let { saveFile(it, "revisionStats.json") }
                _revisionScore.value?.let { saveFile(it, "revisionScore.json") }
            }
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

        // 3. Put recently revised problems first as well as new solves.
        val merged = solvesMap.values.sortedByDescending { it.activityAt }
        _recentSolves.value = merged
        saveFile(merged, "recentSolves.json")
        return merged
    }

    // MARK: - Atomic Parallel Fetch (1:1 with iOS fetchAllData)

    /**
     * Lightweight freeze-date fetch. Mirrors iOS `HomeViewModel.loadData` which refreshes
     * freeze dates independently. Silent-fails, because freeze dates are non-critical for display.
     */
    suspend fun fetchFreezeDates() = withContext(Dispatchers.IO) {
        val result = networkService.getUsedFreezeDates()
        if (result is NetworkResult.Success) {
            val dates = result.data.getAllDates()
            _frozenDates.value = dates
            saveFile(dates, "frozenDates.json")
        }
    }

    /**
     * Refresh everything the app caches, in one concurrent wave.
     *
     * [solveLimit] exists because the solve payload is by far the heaviest thing the API returns —
     * each row carries an AI analysis blob, a mistake-tag array and the full attempt history.
     * If we already have cached solves on disk, we only fetch [INCREMENTAL_SOLVE_LIMIT] (15) solves
     * to capture recent activity, cutting payload transfer by ~75%.
     * If cache is empty, we fetch [HOME_SOLVE_LIMIT] (60).
     * The Problems tab is the one screen that genuinely wants a long list and asks for [DEEP_SOLVE_LIMIT].
     * Because every fetch merges into the shared persisted cache (see [mergeAndPersistSolves])
     * rather than replacing it, older solves are preserved.
     */
    suspend fun fetchAllData(
        username: String,
        solveLimit: Int = if (_recentSolves.value.isNotEmpty()) INCREMENTAL_SOLVE_LIMIT else HOME_SOLVE_LIMIT
    ): Unit = withContext(Dispatchers.IO) {
        // Execute all network requests concurrently (including freeze dates in the same wave)
        val freezeDatesDeferred = async { networkService.getUsedFreezeDates() }
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
        val recentSolvesDeferred = async { networkService.getSolves(limit = solveLimit) }
        val revisionsDeferred = async { networkService.getRevisions(upcoming = true, limit = 50) }
        val groupedRevisionsDeferred = async { networkService.getGroupedRevisions(includeCompleted = true) }
        val revisionStatsDeferred = async { networkService.getRevisionStats() }
        val revisionScoreDeferred = async { networkService.getRevisionScore() }

        // Await results
        val freezeDatesRes = freezeDatesDeferred.await()
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
        val revisionScoreRes = revisionScoreDeferred.await()

        // Process Freeze Dates
        if (freezeDatesRes is NetworkResult.Success) {
            _frozenDates.value = freezeDatesRes.data.getAllDates()
        }

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
        if (allAchievementsRes is NetworkResult.Success) {
            _allAchievements.value = allAchievementsRes.data.achievements
            _awardSections.value = allAchievementsRes.data.sections
            _featuredAward.value = allAchievementsRes.data.featured
                ?: allAchievementsRes.data.achievements
                    .filter { it.unlocked }
                    .maxByOrNull { it.unlockedAt ?: "" }
        }

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
            val allFetched = groupedRevisionsRes.data.groups.flatMap { it.revisions }
            // Unfiltered copy first: the revision-load card needs a 28-day window and reads this
            // one. Narrowing it here would make a 28-day baseline out of a week of data.
            _allRevisions.value = allFetched
            _completedRevisions.value = allFetched.filter { revision ->
                revision.isCompleted && revision.completedDate?.toLocalDate()?.let { !it.isBefore(sevenDaysAgo) } ?: false
            }
        }

        if (revisionStatsRes is NetworkResult.Success) {
            _revisionStats.value = revisionStatsRes.data
        }

        if (revisionScoreRes is NetworkResult.Success) {
            _revisionScore.value = revisionScoreRes.data
        }

        hasFetchedInitialData = true
        _lastFetchTimestamp.value = System.currentTimeMillis()

        persistData()
    }

    suspend fun fetchAwards() = withContext(Dispatchers.IO) {
        val generation = persistenceGeneration.get()
        val result = networkService.getAllAchievements()
        ensureActive()
        if (generation != persistenceGeneration.get()) return@withContext
        if (result is NetworkResult.Success) {
            _allAchievements.value = result.data.achievements
            _awardSections.value = result.data.sections
            _featuredAward.value = result.data.featured
                ?: result.data.achievements.filter { it.unlocked }.maxByOrNull { it.unlockedAt ?: "" }
            persistData()
        }
    }

    /** Home publishes each result immediately and never fetches social-tab data. */
    suspend fun fetchHomeData(
        solveLimit: Int = if (_recentSolves.value.isNotEmpty()) INCREMENTAL_SOLVE_LIMIT else HOME_SOLVE_LIMIT
    ): Unit = withContext(Dispatchers.IO) {
        val generation = persistenceGeneration.get()
        suspend fun <T> fetch(
            request: suspend () -> NetworkResult<T>,
            publish: (T) -> Unit
        ): Boolean {
            val result = request()
            ensureActive()
            if (persistenceGeneration.get() != generation) return false
            if (result is NetworkResult.Success) {
                publish(result.data)
                return true
            }
            return false
        }

        val user = async { fetch({ networkService.getUserStats() }) { _userStats.value = it } }
        val stats = async { fetch({ networkService.getSolveStats() }) { _solveStats.value = it } }
        val solves = async {
            fetch({ networkService.getSolves(limit = solveLimit) }) { mergeAndPersistSolves(it.solves) }
        }
        val optional = listOf(
            async { fetch({ networkService.getAchievementStats() }) { _achievementStats.value = it } },
            async { fetch({ networkService.getUsedFreezeDates() }) { _frozenDates.value = it.getAllDates() } },
            async { fetch({ networkService.getRevisionScore() }) { _revisionScore.value = it } },
            async {
                fetch({ networkService.getGroupedRevisions(includeCompleted = true) }) { response ->
                    val revisions = response.groups.flatMap { it.revisions }
                    _allRevisions.value = revisions
                    val since = LocalDate.now().minusDays(7)
                    _completedRevisions.value = revisions.filter { revision ->
                        revision.isCompleted &&
                            (revision.completedDate?.toLocalDate()?.let { !it.isBefore(since) } ?: false)
                    }
                }
            }
        )
        val coreSucceeded = listOf(user, stats, solves).awaitAll().all { it }
        optional.awaitAll()
        ensureActive()
        if (persistenceGeneration.get() != generation) return@withContext
        if (coreSucceeded) {
            hasFetchedInitialData = true
            _lastFetchTimestamp.value = System.currentTimeMillis()
        }
        persistData()
        if (!coreSucceeded) {
            throw java.io.IOException("Could not refresh all Home data. Pull to refresh to try again.")
        }
    }

    // MARK: - Session Cleanup

    fun clearAllData() {
        persistenceGeneration.incrementAndGet()
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
        _awardSections.value = emptyList()
        _featuredAward.value = null
        _recentSolves.value = emptyList()
        _frozenDates.value = emptyList()
        _todayRevisions.value = emptyList()
        _completedRevisions.value = emptyList()
        _allRevisions.value = emptyList()

        _revisionGroups.value = emptyList()
        _revisionStats.value = null
        _revisionScore.value = null
        _lastFetchTimestamp.value = null

        hasFetchedInitialData = false

        // Delete persisted files.
        // "revisionMode.json" is deliberately still listed even though the revisionMode
        // state is gone: older app versions wrote it, and this is the only thing that
        // clears it off devices that upgraded. Do not remove it as dead code.
        val filenames = listOf(
            "friends.json", "receivedRequests.json", "sentRequests.json",
            "receivedStreakRequests.json", "sentStreakRequests.json", "friendStreaks.json",
            "userStats.json", "submissionStats.json", "solveStats.json",
            "achievementStats.json", "allAchievements.json", "awardSections.json",
            "featuredAward.json", "recentSolves.json", "frozenDates.json",
            "todayRevisions.json", "completedRevisions.json", "allRevisions.json",
            "revisionGroups.json",
            "revisionStats.json", "revisionScore.json", "revisionMode.json",
            "lastFetchTimestamp.json"
        )
        synchronized(persistenceLock) {
            filenames.forEach { filename ->
                AtomicFile(getFile(filename)).delete()
            }
        }
    }

    companion object {
        /**
         * How many solves the *home feed* pulls on refresh. Mirrors iOS
         * `HomeViewModel.homeSolveLimit`.
         */
        const val HOME_SOLVE_LIMIT = 60

        /**
         * How many solves to pull when we already have cached solves on disk.
         * Only recent activity is needed, cutting payload size by ~75%.
         */
        const val INCREMENTAL_SOLVE_LIMIT = 15

        /**
         * How many solves the Problems tab pulls. It is the one screen whose content — the full
         * solve list, its topic filter and the mistake-tag rollup — reads the deep payload.
         */
        const val DEEP_SOLVE_LIMIT = 200

        @Volatile
        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            return instance ?: synchronized(this) {
                instance ?: DataManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
