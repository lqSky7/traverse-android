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
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val totalSolves: Int = 0,
    val totalSubmissions: Int = 0,
    val totalStreakDays: Int = 0,
    val problemsByDifficulty: ProblemsByDifficulty = ProblemsByDifficulty(),
    val availableFreezes: Int? = null
)

@Serializable
data class ProblemsByDifficulty(
    val easy: Int = 0,
    val medium: Int = 0,
    val hard: Int = 0
)

// MARK: - Submission Statistics
@Serializable
data class SubmissionStats(
    val stats: SubmissionStatsData
)

@Serializable
data class SubmissionStatsData(
    val total: Int = 0,
    val accepted: Int = 0,
    val failed: Int = 0,
    val acceptanceRate: String = "0%",
    val languageBreakdown: List<LanguageBreakdown> = emptyList()
)

@Serializable
data class LanguageBreakdown(
    val language: String,
    val count: Int
)

// MARK: - Solve Statistics
@Serializable
data class SolveStats(
    val stats: SolveStatsData
)

@Serializable
data class SolveStatsData(
    val totalSolves: Int = 0,
    val totalXp: Int = 0,
    val totalStreakDays: Int = 0,
    val byDifficulty: ProblemsByDifficulty = ProblemsByDifficulty(),
    val byPlatform: Map<String, Int> = emptyMap()
)

// MARK: - Solves List
@Serializable
data class SolvesResponse(
    val solves: List<Solve>,
    val pagination: Pagination
)

@Serializable
data class CodeAttempt(
    val code: String? = null,
    val language: String? = null,
    val timestamp: String = "",
    val type: String? = null,
    val successful: Boolean? = null,
    val runNumber: Int? = null,
    val submissionNumber: Int? = null
)

@Serializable
data class Solve(
    val id: Int,
    val xpAwarded: Int = 0,
    val solvedAt: String,
    val aiAnalysis: String? = null,
    val mistakeTags: List<String>? = null,
    val cognitiveTier: Int? = null,
    val recallScore: Double? = null,
    val attempts: List<CodeAttempt>? = null,
    val revision: Boolean = false,
    val problem: Problem,
    val submission: Submission,
    val highlight: Highlight? = null
) {
    val allAttempts: List<CodeAttempt>
        get() = attempts ?: submission.attempts ?: emptyList()
}

@Serializable
data class Problem(
    val platform: String,
    val slug: String,
    val title: String,
    val difficulty: String,
    val category: Int? = null,
    val topic: String? = null,
    val subtopic: String? = null
)

@Serializable
data class Submission(
    val language: String = "plaintext",
    val happenedAt: String = "",
    val aiAnalysis: String? = null,
    val mistakeTags: List<String>? = null,
    val cognitiveTier: Int? = null,
    val recallScore: Double? = null,
    val numberOfTries: Int? = null,
    val timeTaken: Int? = null,
    val memory: String? = null,
    val runtime: String? = null,
    val runtimePercentile: Double? = null,
    val attempts: List<CodeAttempt>? = null
)

@Serializable
data class Highlight(
    val id: Int,
    val content: String,
    val note: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class Pagination(
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0
)

// MARK: - Achievement Statistics
@Serializable
data class AchievementStats(
    val stats: AchievementStatsData
)

@Serializable
data class AchievementStatsData(
    val total: Int = 0,
    val unlocked: Int = 0,
    val percentage: String = "0%",
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
    val unlocked: Boolean = false,
    val unlockedAt: String? = null
)

@Serializable
data class MarkNotifiedRequest(
    val achievementIds: List<Int>
)

// MARK: - Freeze Models
@Serializable
data class FreezeDatesResponse(
    val dates: List<String> = emptyList(),
    val freezeDates: List<String> = emptyList()
) {
    fun getAllDates(): List<String> = dates.ifEmpty { freezeDates }
}

@Serializable
data class FreezeInfoResponse(
    val availableFreezes: Int = 0,
    val usedFreezes: Int = 0,
    val totalFreezes: Int = 0,
    val latestGift: FreezeGiftInfo? = null,
    val costs: FreezeCosts = FreezeCosts(100, 70)
)

@Serializable
data class FreezeGiftInfo(
    val id: Int,
    val giftedBy: String? = null,
    val createdAt: String
)

@Serializable
data class FreezeCosts(
    val purchase: Int = 100,
    val gift: Int = 70
)

@Serializable
data class PurchaseFreezeRequest(
    val count: Int = 1
)

@Serializable
data class PurchaseFreezeResponse(
    val message: String,
    val freezesPurchased: Int = 0,
    val xpSpent: Int = 0,
    val availableFreezes: Int = 0,
    val remainingXp: Int = 0
)

@Serializable
data class GiftFreezeRequest(
    val count: Int = 1
)

@Serializable
data class GiftFreezeResponse(
    val message: String,
    val freezesGifted: Int = 0,
    val xpSpent: Int = 0,
    val recipient: String = "",
    val remainingXp: Int = 0
)

typealias FreezePurchaseResponse = PurchaseFreezeResponse
typealias FreezeGiftResponse = GiftFreezeResponse

// MARK: - App Updates & Sync Response
@Serializable
data class AppUpdatesResponse(
    val achievements: List<AchievementDetail> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val streakRequests: List<FriendStreakRequest> = emptyList(),
    val freezeInfo: FreezeInfoResponse = FreezeInfoResponse()
)

// MARK: - Subscription Models
@Serializable
data class SubscriptionStatusResponse(
    val isSubscriptionActive: Boolean = false,
    val plan: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
    val isPro: Boolean = false
)

@Serializable
data class CreateSubscriptionOrderRequest(
    val plan: String // "PRO_MONTHLY" or "PRO_YEARLY"
)

@Serializable
data class CreateSubscriptionOrderResponse(
    val orderId: String,
    val amount: Int,
    val currency: String,
    val keyId: String
)

@Serializable
data class VerifySubscriptionRequest(
    @SerialName("razorpay_payment_id")
    val razorpayPaymentId: String,
    @SerialName("razorpay_order_id")
    val razorpayOrderId: String,
    @SerialName("razorpay_signature")
    val razorpaySignature: String
)

@Serializable
data class VerifySubscriptionResponse(
    val success: Boolean,
    val message: String? = null
)
