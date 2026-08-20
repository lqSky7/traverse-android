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
    val solve: RevisionSolve? = null
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
    val difficulty: String,
    val category: Int? = null,
    val topic: String? = null,
    val subtopic: String? = null
)

@Serializable
data class RevisionSolve(
    val id: Int,
    val xpAwarded: Int = 0,
    val solvedAt: String,
    val aiAnalysis: String? = null,
    val mistakeTags: List<String>? = null,
    val cognitiveTier: Int? = null,
    val recallScore: Double? = null,
    val attempts: List<CodeAttempt>? = null
)

@Serializable
data class RevisionDetailsResponse(
    val revision: Revision
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
    val total: Int = 0,
    val completed: Int = 0,
    val overdue: Int = 0,
    val dueToday: Int = 0,
    val completionRate: Int = 0
)

// MARK: - Complete Revision Response
@Serializable
data class CompleteRevisionResponse(
    val message: String,
    val revision: Revision
)

// MARK: - Reschedule Revision
@Serializable
data class RescheduleRevisionRequest(
    val days: Int
)

@Serializable
data class RescheduleRevisionResponse(
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
    val daysSinceLastAttempt: Double = 0.0,
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

// MARK: - Pause / Resume Models
@Serializable
data class PauseRevisionsRequest(
    val pauseDays: Int = 7
)

@Serializable
data class PauseRevisionsResponse(
    val message: String,
    val pausedUntil: String,
    val isPaused: Boolean
)

@Serializable
data class ResumeRevisionsRequest(
    val backlogDays: Int = 3
)

@Serializable
data class ResumeRevisionsResponse(
    val message: String,
    val rescheduled: Int,
    val backlogDays: Int,
    val isPaused: Boolean
)

// MARK: - ML Analytics
@Serializable
data class RevisionAnalyticsResponse(
    val overview: RevisionAnalyticsOverview,
    val stabilityDistribution: RevisionStabilityDistribution,
    val topicBreakdown: List<RevisionTopicMetric> = emptyList(),
    val weeklyCompletion: List<WeeklyCompletion> = emptyList(),
    val accuracyTrend: List<RevisionAccuracyPoint> = emptyList(),
    val projectedLoad: List<RevisionProjectedLoad> = emptyList(),
    val intervalGrowth: List<RevisionIntervalGrowth> = emptyList(),
    val retentionHeatmap: List<RevisionRetentionItem> = emptyList(),
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
data class RevisionTopicMetric(
    val topic: String,
    val problemCount: Int,
    val averageRetention: Double,
    val averageStability: Double,
    val averageTimeMinutes: Double
)

@Serializable
data class WeeklyCompletion(
    val week: String,
    val count: Int
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
    val difficultyD: Double = 0.0,
    val lapses: Int,
    val lastReviewAt: String? = null,
    val isLeech: Boolean
)

@Serializable
data class RevisionAnalyticsStreaks(
    val totalRevisionsCompleted: Int = 0,
    val totalAttempts: Int = 0,
    val overallSuccessRate: Double = 0.0
)

// MARK: - Today Summary (ML daily queue)
@Serializable
data class RevisionTodayResponse(
    val revisions: List<Revision> = emptyList(),
    val total: Int = 0,
    val maxDaily: Int = 20,
    val overflow: Int = 0,
    val completed: Int = 0,
    val remaining: Int = 0,
    val isPaused: Boolean? = null,
    val pausedUntil: String? = null,
    val canDoMore: Boolean = true
)

// MARK: - ML Recalibration
@Serializable
data class RevisionRecalibrationResponse(
    val message: String,
    val rescheduled: Int = 0,
    val totalPending: Int = 0,
    val maxDaily: Int = 20,
    val dailyBreakdown: List<RecalibrationDailyBreakdown> = emptyList(),
    val recommendations: List<RecalibrationRecommendation> = emptyList()
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
