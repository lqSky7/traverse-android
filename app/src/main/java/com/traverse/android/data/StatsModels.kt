package com.traverse.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - User Statistics
@Serializable
data class UserStats(
    val username: String,
    val stats: UserStatsData
)

@Serializable
data class UserStatsData(
    val currentStreak: Int,
    val totalXp: Int,
    val totalSolves: Int,
    val totalSubmissions: Int,
    val totalStreakDays: Int,
    val problemsByDifficulty: ProblemsByDifficulty,
    val availableFreezes: Int? = null
)

@Serializable
data class ProblemsByDifficulty(
    val easy: Int = 0,
    val medium: Int = 0,
    val hard: Int = 0
)

// MARK: - Solve Statistics
@Serializable
data class SolveStats(
    val stats: SolveStatsData
)

@Serializable
data class SolveStatsData(
    val totalSolves: Int,
    val totalXp: Int,
    val totalStreakDays: Int,
    val byDifficulty: ProblemsByDifficulty,
    val byPlatform: Map<String, Int> = emptyMap()
)

// MARK: - Solves List
@Serializable
data class SolvesResponse(
    val solves: List<Solve>,
    val pagination: Pagination
)

@Serializable
data class Solve(
    val id: Int,
    val xpAwarded: Int,
    val solvedAt: String,
    val aiAnalysis: String? = null,
    val mistakeTags: List<String>? = null,
    val revision: Boolean = false,
    val problem: Problem,
    val submission: Submission,
    val highlight: Highlight? = null
)

@Serializable
data class Problem(
    val platform: String,
    val slug: String,
    val title: String,
    val difficulty: String
)

@Serializable
data class Submission(
    val language: String,
    val happenedAt: String,
    val aiAnalysis: String? = null,
    val mistakeTags: List<String>? = null,
    val numberOfTries: Int? = null,
    val timeTaken: Int? = null,
    val memory: String? = null,
    val runtime: String? = null,
    val runtimePercentile: Double? = null
)

@Serializable
data class Highlight(
    val id: Int,
    val content: String,
    val note: String,
    val tags: List<String>
)

@Serializable
data class Pagination(
    val total: Int,
    val limit: Int,
    val offset: Int
)

// MARK: - Achievement Statistics
@Serializable
data class AchievementStats(
    val stats: AchievementStatsData
)

@Serializable
data class AchievementStatsData(
    val total: Int,
    val unlocked: Int,
    val percentage: String,
    val byCategory: Map<String, Int> = emptyMap()
)

// MARK: - All Achievements
@Serializable
data class AllAchievementsResponse(
    val achievements: List<AchievementDetail>
)

@Serializable
data class AchievementDetail(
    val id: Int,
    val key: String,
    val name: String,
    val description: String,
    val icon: String? = null,
    val category: String,
    val unlocked: Boolean,
    val unlockedAt: String? = null
)

// MARK: - Freeze Dates
@Serializable
data class FreezeDatesResponse(
    val dates: List<String> = emptyList(),
    val freezeDates: List<String> = emptyList()
) {
    // Support both field names from API
    fun getAllDates(): List<String> = dates.ifEmpty { freezeDates }
}
