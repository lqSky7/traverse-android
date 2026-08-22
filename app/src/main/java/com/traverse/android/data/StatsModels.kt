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
) {
    val displayTopic: String?
        get() = if (topic.isNullOrBlank()) null else formatTopicSlug(topic)
}

fun formatTopicSlug(slug: String?): String {
    if (slug.isNullOrBlank()) return "General"
    val clean = slug.trim()
    val knownNames = mapOf(
        "kadanes-algorithm" to "Kadane's Algorithm",
        "prefix-sum" to "Prefix Sum",
        "prefix-sum-hashmap" to "Prefix Sum + Hash Map",
        "sliding-window-fixed" to "Sliding Window (Fixed)",
        "sliding-window-variable" to "Sliding Window (Variable)",
        "two-pointers-opposite" to "Two Pointers (Opposite)",
        "two-pointers-same" to "Two Pointers (Same Direction)",
        "dutch-national-flag" to "Dutch National Flag",
        "merge-intervals" to "Merge Intervals",
        "cyclic-sort" to "Cyclic Sort",
        "matrix-traversal" to "Matrix Traversal",
        "general-arrays" to "General Arrays",
        "string-matching" to "String Matching",
        "palindrome" to "Palindrome Logic",
        "anagram" to "Anagram",
        "string-parsing" to "String Parsing",
        "general-strings" to "General Strings",
        "fast-slow-pointers" to "Fast & Slow Pointers",
        "linked-list-reversal" to "Linked List Reversal",
        "merge-lists" to "Merge Linked Lists",
        "general-linked-list" to "General Linked List",
        "tree-traversal" to "Tree Traversal",
        "binary-search-tree" to "Binary Search Tree",
        "tree-construction" to "Tree Construction",
        "trie-prefix-tree" to "Trie (Prefix Tree)",
        "segment-tree" to "Segment Tree",
        "lowest-common-ancestor" to "Lowest Common Ancestor",
        "general-trees" to "General Trees",
        "bfs-shortest-path" to "Graph BFS (Shortest Path)",
        "dfs-graph" to "Graph DFS / Traversal",
        "topological-sort" to "Topological Sort",
        "dijkstra" to "Dijkstra's Algorithm",
        "bellman-ford" to "Bellman-Ford",
        "union-find" to "Disjoint Set / Union-Find",
        "minimum-spanning-tree" to "Minimum Spanning Tree",
        "bipartite-check" to "Bipartite Graph Check",
        "general-graphs" to "General Graphs",
        "dp-1d-linear" to "1D Linear DP",
        "dp-2d-grid" to "2D Grid DP",
        "dp-knapsack" to "0/1 Knapsack & Subset DP",
        "dp-lcs" to "LCS / Edit Distance",
        "dp-lis" to "Longest Increasing Subsequence",
        "dp-state-machine" to "State Machine DP",
        "dp-interval" to "Interval DP",
        "dp-tree" to "Tree DP",
        "dp-bitmask" to "Bitmask DP",
        "general-dp" to "General DP",
        "activity-selection" to "Activity Selection",
        "jump-game" to "Jump Game Pattern",
        "task-scheduling" to "Task Scheduling",
        "general-greedy" to "General Greedy",
        "permutations" to "Permutations",
        "combinations-subsets" to "Combinations & Subsets",
        "constraint-satisfaction" to "Constraint Satisfaction",
        "general-backtracking" to "General Backtracking",
        "custom-comparator" to "Custom Comparator Sorting",
        "counting-sort" to "Counting Sort",
        "merge-sort-application" to "Merge Sort Applications",
        "general-sorting" to "General Sorting",
        "binary-search-standard" to "Binary Search (Standard)",
        "binary-search-on-answer" to "Binary Search on Answer",
        "binary-search-rotated" to "Binary Search in Rotated Array",
        "general-searching" to "General Binary Search",
        "monotonic-stack" to "Monotonic Stack",
        "expression-evaluation" to "Expression Evaluation",
        "parenthesis-matching" to "Parentheses Matching",
        "general-stack" to "General Stack",
        "sliding-window-deque" to "Monotonic Deque",
        "bfs-queue" to "Queue BFS",
        "general-queue" to "General Queue",
        "top-k-elements" to "Top K Elements",
        "merge-k-sorted" to "Merge K Sorted Streams",
        "median-finding" to "Two Heaps / Median Finding",
        "general-heap" to "General Heap / PQ",
        "two-sum-pattern" to "Two Sum / Pair Lookup",
        "frequency-counting" to "Frequency Counting",
        "group-by-key" to "Grouping by Key",
        "general-hashing" to "General Hash Table",
        "bit-manipulation" to "Bit Manipulation",
        "modular-arithmetic" to "Modular Arithmetic",
        "gcd-lcm" to "GCD & LCM",
        "prime-sieve" to "Primes & Sieve",
        "general-math" to "General Math"
    )
    return knownNames[clean.lowercase()] ?: if (clean.contains("-")) {
        clean.split("-").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    } else {
        clean
    }
}

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
