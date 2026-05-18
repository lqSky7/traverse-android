package com.traverse.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// MARK: - Revision
@Serializable
data class Revision(
    val id: Int,
    val solveId: Int,
    val userId: Int,
    val problemId: Int,
    val revisionNumber: Int,
    val scheduledFor: String,
    val completedAt: String? = null,
    val createdAt: String,
    val problem: RevisionProblem,
    val solve: RevisionSolve
) {
    val scheduledDate: LocalDate
        get() = try {
            // Try date-only format first (yyyy-MM-dd)
            LocalDate.parse(scheduledFor.substring(0, 10))
        } catch (e: Exception) {
            LocalDate.now()
        }
    
    val completedDate: LocalDateTime?
        get() = completedAt?.let {
            try {
                LocalDateTime.parse(it.substring(0, 19))
            } catch (e: Exception) {
                null
            }
        }
    
    val isCompleted: Boolean
        get() = completedAt != null
    
    val isOverdue: Boolean
        get() = !isCompleted && scheduledDate.isBefore(LocalDate.now())
}

@Serializable
data class RevisionProblem(
    val id: Int,
    val platform: String,
    val slug: String,
    val title: String,
    val difficulty: String
)

@Serializable
data class RevisionSolve(
    val id: Int,
    val xpAwarded: Int,
    val solvedAt: String
)

// MARK: - Revision Response
@Serializable
data class RevisionsResponse(
    val revisions: List<Revision>,
    val pagination: Pagination? = null
)

// MARK: - Grouped Revisions
@Serializable
data class GroupedRevisionsResponse(
    val groups: List<RevisionGroup>
)

@Serializable
data class RevisionGroup(
    val date: String,
    val revisions: List<Revision>,
    val count: Int
) {
    val displayDate: LocalDate
        get() = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            LocalDate.now()
        }
}

// MARK: - Revision Stats
@Serializable
data class RevisionStatsResponse(
    val total: Int,
    val completed: Int,
    val overdue: Int,
    val dueToday: Int,
    val completionRate: Int
)

// MARK: - Complete Revision Response
@Serializable
data class CompleteRevisionResponse(
    val message: String,
    val revision: Revision
)

// MARK: - ML Revision Attempt
@Serializable
data class RevisionAttemptRequest(
    val outcome: Int, // 0 = failed, 1 = success
    val numTries: Int,
    val timeSpentMinutes: Double
)

@Serializable
data class RevisionAttemptResponse(
    val message: String,
    val attempt: RevisionAttempt,
    val prediction: MLPrediction,
    val nextRevision: Revision? = null
)

@Serializable
data class RevisionAttempt(
    val id: Int,
    val revisionId: Int,
    val userId: Int,
    val problemId: Int,
    val attemptNumber: Int,
    val daysSinceLastAttempt: Double,
    val outcome: Int,
    val numTries: Int,
    val timeSpentMinutes: Double,
    val attemptedAt: String
)

@Serializable
data class MLPrediction(
    @SerialName("next_review_interval_days")
    val nextReviewIntervalDays: Double,
    val confidence: String
)

// MARK: - ML Analytics
@Serializable
data class RevisionAnalyticsResponse(
    val overview: RevisionAnalyticsOverview,
    val stabilityDistribution: RevisionStabilityDistribution,
    val accuracyTrend: List<RevisionAccuracyPoint>,
    val projectedLoad: List<RevisionProjectedLoad>,
    val intervalGrowth: List<RevisionIntervalGrowth>,
    val retentionHeatmap: List<RevisionRetentionItem>,
    val streaks: RevisionAnalyticsStreaks
)

@Serializable
data class RevisionAnalyticsOverview(
    val totalProblemsTracked: Int,
    val masteredProblems: Int,
    val leechProblems: Int,
    val averageStability: Double,
    val averageRetrievability: Double
)

@Serializable
data class RevisionStabilityDistribution(
    val critical: Int,
    val weak: Int,
    val developing: Int,
    val strong: Int,
    val mastered: Int
)

@Serializable
data class RevisionAccuracyPoint(
    val date: String,
    val successRate: Double,
    val totalAttempts: Int
)

@Serializable
data class RevisionProjectedLoad(
    val date: String,
    val dueCount: Int,
    val overdueCount: Int
)

@Serializable
data class RevisionIntervalGrowth(
    val month: String,
    val avgInterval: Double,
    val count: Int
)

@Serializable
data class RevisionRetentionItem(
    val problemId: Int,
    val problemTitle: String,
    val problemSlug: String,
    val platform: String,
    val difficulty: String,
    val retrievability: Double,
    val stability: Double,
    @SerialName("difficulty_D")
    val difficultyD: Double,
    val lapses: Int,
    val lastReviewAt: String? = null,
    val isLeech: Boolean
)

@Serializable
data class RevisionAnalyticsStreaks(
    val totalRevisionsCompleted: Int,
    val totalAttempts: Int,
    val overallSuccessRate: Double
)

// MARK: - Today Summary
@Serializable
data class RevisionTodayResponse(
    val completed: Int,
    val remaining: Int,
    val maxDaily: Int,
    val canDoMore: Boolean
)

// MARK: - ML Recalibration
@Serializable
data class RevisionRecalibrationResponse(
    val message: String,
    val rescheduled: Int,
    val totalPending: Int,
    val maxDaily: Int,
    val dailyBreakdown: List<RecalibrationDailyBreakdown>,
    val recommendations: List<RecalibrationRecommendation>
)

@Serializable
data class RecalibrationDailyBreakdown(
    val date: String,
    val count: Int
)

@Serializable
data class RecalibrationRecommendation(
    val type: String,
    val message: String
)
