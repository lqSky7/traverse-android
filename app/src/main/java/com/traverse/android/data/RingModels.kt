package com.traverse.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A day's ring state, as the server computes it.
 *
 * Two rings: solves (new problems solved today) and revisions (scheduled reviews).
 * Every field has a default so cached payloads pre-dating any field degrade safely.
 * Fractions are clamped to 1.0f.
 */
@Serializable
data class RingProgress(
    val date: String = "",
    val solves: Int = 0,
    val revisions: Int = 0,
    val solveGoal: Int = 1,
    val revisionGoal: Int = 1,
    @SerialName("configuredGoals") val configuredGoals: RingGoals = RingGoals(),
    val solveRingClosed: Boolean = false,
    val revisionRingClosed: Boolean = false,
    val allClosed: Boolean = false,
    val closedAt: String? = null
) {
    val configuredSolveGoal: Int get() = configuredGoals.solveGoal
    val configuredRevisionGoal: Int get() = configuredGoals.revisionGoal

    val solveFraction: Float
        get() = if (solveGoal <= 0) 0f else minOf(solves.toFloat() / solveGoal.toFloat(), 1f)

    val revisionFraction: Float
        get() = if (revisionGoal <= 0) 0f else minOf(revisions.toFloat() / revisionGoal.toFloat(), 1f)

    companion object {
        fun empty() = RingProgress()
    }
}

@Serializable
data class RingGoals(
    val solveGoal: Int = 1,
    val revisionGoal: Int = 1
) {
    companion object {
        const val MINIMUM = 1
        const val MAXIMUM = 50
        val DEFAULT = RingGoals()
    }

    fun clamped() = RingGoals(
        solveGoal = solveGoal.coerceIn(MINIMUM, MAXIMUM),
        revisionGoal = revisionGoal.coerceIn(MINIMUM, MAXIMUM)
    )
}

@Serializable
data class RingsResponse(
    val rings: RingProgress
)

@Serializable
data class UpdateRingGoalsRequest(
    val solveGoal: Int,
    val revisionGoal: Int
)

@Serializable
data class UpdateRingGoalsResponse(
    val goals: RingGoals,
    val rings: RingProgress
)
