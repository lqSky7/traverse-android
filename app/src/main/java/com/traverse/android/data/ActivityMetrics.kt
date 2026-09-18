package com.traverse.android.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Shared plumbing for every "how much / when" chart in the app. 1:1 Kotlin port of iOS
 * `Models/ActivityMetrics.swift`.
 *
 * Three problems kept showing up across the home feed and are solved once, here, instead of in
 * each card:
 *
 * 1. Timestamp parsing. The API emits ISO-8601 with fractional seconds for most fields but not
 *    all of them. Cards that did their own `LocalDateTime.parse(raw.take(19))` silently dropped
 *    or mis-placed every row that arrived with a numeric UTC offset — no error, the row just
 *    vanished from the chart, which is how a histogram ends up reporting the wrong peak hour.
 *    See [ActivityTimestamp].
 *
 * 2. Which timestamp to trust. `solvedAt` is stamped once, when a problem is first accepted, and
 *    the backend never moves it: re-solving or revising a problem only bumps `lastActivityAt`.
 *    Anything that claims to describe *when you work* has to read [Solve.activityAt], or a
 *    revision-heavy account looks like it only ever solved problems on the day it first met them.
 *
 * 3. Bucketing. Every card needs the same "group these rows by local calendar day / hour" loop,
 *    and doing it inline is where timezone and start-of-day mistakes creep in.
 */

// MARK: - Difficulty weighting

object DifficultyWeight {
    /** Relative cost of a problem, used so a hard revision counts as more training load than an easy one. */
    fun weight(difficulty: String): Double = when (difficulty.lowercase()) {
        "easy" -> 1.0
        "medium" -> 2.0
        "hard" -> 3.0
        else -> 1.0
    }

    /** Canonical ordering, easiest first. */
    val order: List<String> = listOf("easy", "medium", "hard")

    fun rank(difficulty: String): Int =
        order.indexOf(difficulty.lowercase()).let { if (it < 0) order.size else it }

    fun displayName(difficulty: String): String = when (difficulty.lowercase()) {
        "easy" -> "Easy"
        "medium" -> "Medium"
        "hard" -> "Hard"
        else -> difficulty.replaceFirstChar { it.uppercase() }
    }
}

// MARK: - Bucket value types

data class DayValue(val date: LocalDate, val value: Double)

data class HourValue(val hour: Int, val value: Double)

data class MetricSummary(
    val total: Double,
    val average: Double,
    val maximum: Double,
    val sampleCount: Int
) {
    companion object {
        val EMPTY = MetricSummary(total = 0.0, average = 0.0, maximum = 0.0, sampleCount = 0)
    }
}

data class DifficultyMetric(
    val difficulty: String,
    val solves: Int,
    val attempts: Int
) {
    val averageAttempts: Double
        get() = if (solves > 0) attempts.toDouble() / solves else 0.0
}

// MARK: - Bucketing

object ActivityMetrics {

    // MARK: Revision load

    /** Preferred baseline window. */
    const val LOAD_BASELINE_DAYS = 28

    /**
     * Fallback baseline window for accounts that do not have a month of history yet. Two weeks is
     * still long enough for a 7-day average to mean something.
     */
    const val LOAD_FALLBACK_BASELINE_DAYS = 15

    const val LOAD_RECENT_DAYS = 7

    /**
     * The oldest day in a contribution list, or `null` when there is nothing to measure.
     *
     * Hand-rolled rather than `minOfOrNull { it.first }`: `LocalDate` only implements
     * `Comparable<ChronoLocalDate>`, so the stdlib's `Comparable`-constrained helpers resolve
     * through a widened type parameter that returns `ChronoLocalDate?` rather than `LocalDate?`.
     */
    private fun earliestDay(contributions: List<Pair<LocalDate, Double>>): LocalDate? {
        var earliest: LocalDate? = null
        for ((day, _) in contributions) {
            val current = earliest
            if (current == null || day.isBefore(current)) earliest = day
        }
        return earliest
    }

    /**
     * Apple Fitness' "Training Load" idea, translated to problem practice.
     *
     * Load for a day is the difficulty-weighted amount of work done that day: every completed
     * revision plus every problem worked on. The 7-day daily average is compared against a longer
     * baseline average, so a week of heavy practice against a light month reads as "Above".
     *
     * The baseline is 28 days when there is 28 days of history to compare against, and 15 days
     * when there is not — a brand-new account used to get "No Data" for its first month, which is
     * exactly when a load reading is most useful. Below 15 days of history the comparison
     * genuinely is not meaningful, so the card says so instead of inventing a verdict.
     *
     * [revisions] may include not-yet-completed rows; anything without a `completedAt` is skipped.
     */
    fun revisionLoad(
        revisions: List<Revision>,
        solves: List<Solve>,
        today: LocalDate = LocalDate.now()
    ): RevisionLoadSnapshot {
        // Flatten both sources into (day, weight) contributions once.
        val contributions = mutableListOf<Pair<LocalDate, Double>>()

        revisions.forEach { revision ->
            val completed = ActivityTimestamp.dayKey(revision.completedAt) ?: return@forEach
            contributions += completed to DifficultyWeight.weight(revision.problem.difficulty)
        }

        solves.forEach { solve ->
            val day = ActivityTimestamp.dayKey(solve.activityAt) ?: return@forEach
            contributions += day to DifficultyWeight.weight(solve.problem.difficulty)
        }

        val earliest = earliestDay(contributions)
        val historyDays = earliest?.let { ChronoUnit.DAYS.between(it, today).toInt() } ?: 0

        val baselineDays = when {
            historyDays >= LOAD_BASELINE_DAYS -> LOAD_BASELINE_DAYS
            historyDays >= LOAD_FALLBACK_BASELINE_DAYS -> LOAD_FALLBACK_BASELINE_DAYS
            else -> 0
        }

        // Always draw a window, even when there is not enough history to grade it. A chart with
        // no verdict beats a chart with no bars.
        //
        // Filled oldest-first so the map's own iteration order *is* chronological and the series
        // never needs sorting. `LocalDate` only implements `Comparable<ChronoLocalDate>`, so
        // `sortedBy`/`minOfOrNull` over it resolve through a widened type parameter — building the
        // order in avoids that entirely.
        val windowDays = if (baselineDays > 0) baselineDays else LOAD_FALLBACK_BASELINE_DAYS
        val buckets = LinkedHashMap<LocalDate, Double>()
        for (offset in (windowDays - 1) downTo 0) {
            buckets[today.minusDays(offset.toLong())] = 0.0
        }

        contributions.forEach { (day, weight) ->
            if (buckets.containsKey(day)) {
                buckets[day] = (buckets[day] ?: 0.0) + weight
            }
        }

        val series = buckets.map { DailyLoadPoint(it.key, it.value) }
        val windowTotal = series.sumOf { it.value }
        val recentTotal = series.takeLast(LOAD_RECENT_DAYS).sumOf { it.value }
        val recentAverage = recentTotal / LOAD_RECENT_DAYS

        if (baselineDays <= 0 || windowTotal <= 0.0) {
            return RevisionLoadSnapshot(
                band = RevisionLoadBand.NO_DATA,
                percentChange = null,
                recentDailyAverage = recentAverage,
                baselineDailyAverage = 0.0,
                series = series,
                windowTotal = windowTotal,
                baselineDays = null
            )
        }

        val baselineAverage = windowTotal / baselineDays
        if (baselineAverage <= 0.0) {
            return RevisionLoadSnapshot(
                band = RevisionLoadBand.NO_DATA,
                percentChange = null,
                recentDailyAverage = recentAverage,
                baselineDailyAverage = 0.0,
                series = series,
                windowTotal = windowTotal,
                baselineDays = null
            )
        }

        val ratio = recentAverage / baselineAverage
        val percent = ((ratio - 1) * 100).roundToInt()

        return RevisionLoadSnapshot(
            band = RevisionLoadBand.from(ratio),
            percentChange = percent,
            recentDailyAverage = recentAverage,
            baselineDailyAverage = baselineAverage,
            series = series,
            windowTotal = windowTotal,
            baselineDays = baselineDays
        )
    }

    // MARK: Hourly

    /** 24 buckets for the given local day, oldest hour first. */
    fun hourly(
        solves: List<Solve>,
        day: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        value: (Solve) -> Double
    ): List<HourValue> {
        val buckets = DoubleArray(24)

        solves.forEach { solve ->
            val instant = solve.activityInstant ?: return@forEach
            val zoned = instant.atZone(zone)
            if (zoned.toLocalDate() != day) return@forEach
            val hour = zoned.hour
            if (hour in 0..23) buckets[hour] += value(solve)
        }

        return (0 until 24).map { HourValue(hour = it, value = buckets[it]) }
    }

    // MARK: Daily / monthly

    /**
     * One bucket per local day, oldest first, covering [days] days ending today. Days with no
     * activity are present with a value of `0` so charts keep an even x-axis instead of
     * collapsing gaps.
     */
    fun daily(
        solves: List<Solve>,
        days: Int,
        today: LocalDate = LocalDate.now(),
        value: (Solve) -> Double
    ): List<DayValue> {
        val buckets = LinkedHashMap<LocalDate, Double>()
        for (offset in (maxOf(days, 1) - 1) downTo 0) {
            buckets[today.minusDays(offset.toLong())] = 0.0
        }

        solves.forEach { solve ->
            val day = ActivityTimestamp.dayKey(solve.activityAt) ?: return@forEach
            if (buckets.containsKey(day)) {
                buckets[day] = (buckets[day] ?: 0.0) + value(solve)
            }
        }

        return buckets.map { DayValue(it.key, it.value) }
    }

    /** One bucket per calendar month, oldest first, ending with the current month. */
    fun monthly(
        solves: List<Solve>,
        months: Int,
        today: LocalDate = LocalDate.now(),
        value: (Solve) -> Double
    ): List<DayValue> {
        val currentMonthStart = today.withDayOfMonth(1)
        val buckets = LinkedHashMap<LocalDate, Double>()
        for (offset in (maxOf(months, 1) - 1) downTo 0) {
            buckets[currentMonthStart.minusMonths(offset.toLong())] = 0.0
        }

        solves.forEach { solve ->
            val day = ActivityTimestamp.dayKey(solve.activityAt) ?: return@forEach
            val monthStart = day.withDayOfMonth(1)
            if (buckets.containsKey(monthStart)) {
                buckets[monthStart] = (buckets[monthStart] ?: 0.0) + value(solve)
            }
        }

        return buckets.map { DayValue(it.key, it.value) }
    }

    // MARK: Summaries

    fun summary(solves: List<Solve>, value: (Solve) -> Double?): MetricSummary {
        var total = 0.0
        var maximum = 0.0
        var count = 0

        solves.forEach { solve ->
            val sample = value(solve) ?: return@forEach
            total += sample
            if (sample > maximum) maximum = sample
            count++
        }

        return MetricSummary(
            total = total,
            average = if (count > 0) total / count else 0.0,
            maximum = maximum,
            sampleCount = count
        )
    }

    /** Per-difficulty solve counts and attempt totals, easiest first. */
    fun byDifficulty(solves: List<Solve>): List<DifficultyMetric> {
        val solvesByDifficulty = mutableMapOf<String, Int>()
        val attemptsByDifficulty = mutableMapOf<String, Int>()

        solves.forEach { solve ->
            val key = solve.problem.difficulty.lowercase()
            solvesByDifficulty[key] = (solvesByDifficulty[key] ?: 0) + 1
            attemptsByDifficulty[key] =
                (attemptsByDifficulty[key] ?: 0) + maxOf(solve.submission.numberOfTries ?: 1, 1)
        }

        return DifficultyWeight.order.mapNotNull { key ->
            val solveCount = solvesByDifficulty[key] ?: 0
            if (solveCount <= 0) {
                null
            } else {
                DifficultyMetric(
                    difficulty = key,
                    solves = solveCount,
                    attempts = attemptsByDifficulty[key] ?: solveCount
                )
            }
        }
    }
}

// MARK: - Revision load model

data class DailyLoadPoint(val day: LocalDate, val value: Double)

enum class RevisionLoadBand(val label: String) {
    WELL_BELOW("Well Below"),
    BELOW("Below"),
    OPTIMAL("Optimal"),
    ABOVE("Above"),
    WELL_ABOVE("Well Above"),
    NO_DATA("No Data");

    companion object {
        /**
         * Bands are the same shape Apple uses for training load: a 20% swing either way is normal
         * week-to-week noise, beyond that it is worth naming.
         */
        fun from(ratio: Double): RevisionLoadBand = when {
            ratio < 0.8 -> WELL_BELOW
            ratio < 1.0 -> BELOW
            ratio <= 1.3 -> OPTIMAL
            ratio <= 1.5 -> ABOVE
            else -> WELL_ABOVE
        }
    }

    /**
     * Slot in the user's palette this band draws from.
     *
     * The reference screenshots are Apple Fitness, whose bands are purple / green / teal. This app
     * is palette-driven, and a card that ignores the user's chosen palette looks pasted on — so
     * bands map onto palette slots instead of hard-coded colours: slot 0 (the palette's primary)
     * means "on track", and the slots either side of it get progressively further from it as the
     * reading moves away from Optimal. With the Monochrome palette that reads as a lightness ramp,
     * which is the correct behaviour.
     */
    val paletteIndex: Int
        get() = when (this) {
            OPTIMAL -> 0
            ABOVE -> 1
            WELL_ABOVE -> 2
            BELOW -> 3
            WELL_BELOW -> 4
            NO_DATA -> 3
        }

    /**
     * Plain-language read on what the band means for the user, mirroring the paragraph Apple puts
     * under the status word.
     */
    val explanation: String
        get() = when (this) {
            WELL_BELOW ->
                "Your 7-day load is well below your baseline. That's a real taper — fine after a " +
                    "hard stretch, but memory decays without reviews, so expect more overdue " +
                    "revisions soon."
            BELOW ->
                "Your 7-day load is below your baseline. A lighter week is normal, but if it stays " +
                    "here your retention will start to slip."
            OPTIMAL ->
                "Your 7-day load is in line with your baseline. This is the range where retention " +
                    "grows without burning you out."
            ABOVE ->
                "Your 7-day load is above your baseline. You may see gains in retention, but " +
                    "recover as needed if you feel especially fatigued."
            WELL_ABOVE ->
                "Your 7-day load is well above your baseline. This is a sharp ramp — watch your " +
                    "accuracy and take a lighter day if revisions start failing."
            NO_DATA ->
                "Not enough history yet. Load compares your last 7 days against a longer baseline, " +
                    "and it needs about two weeks of activity before that comparison means " +
                    "anything. Keep going — this fills in on its own."
        }
}

/**
 * Which slice of practice a load figure covers. Mirrors the workout-type chips Apple puts above
 * the training-load status.
 */
enum class LoadScope(val title: String) {
    ALL("All Practice"),
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    /** `null` for [ALL]; otherwise the backend difficulty string. */
    val difficultyKey: String?
        get() = if (this == ALL) null else name.lowercase()
}

data class RevisionLoadSnapshot(
    val band: RevisionLoadBand,
    /**
     * Percent change of the 7-day daily average against the baseline daily average. `null` when
     * there is nothing to compare against.
     */
    val percentChange: Int?,
    val recentDailyAverage: Double,
    val baselineDailyAverage: Double,
    val series: List<DailyLoadPoint>,
    val windowTotal: Double,
    /**
     * Length of the baseline window actually in use: 28 normally, 15 while the account is younger
     * than a month, `null` when there is not enough history to grade at all.
     */
    val baselineDays: Int?
) {
    val hasData: Boolean get() = band != RevisionLoadBand.NO_DATA

    /** "+38%" / "-12%" / null. */
    val formattedPercentChange: String?
        get() = percentChange?.let { if (it >= 0) "+$it%" else "$it%" }

    /** "7-day vs. 28-day load" — or 15-day, while that is the window in use. */
    val comparisonLabel: String
        get() = "7-day vs. ${baselineDays ?: ActivityMetrics.LOAD_BASELINE_DAYS}-day load"

    /** "28-Day Daily Load" / "15-Day Daily Load". */
    val baselineLabel: String
        get() = "${baselineDays ?: ActivityMetrics.LOAD_BASELINE_DAYS}-Day Daily Load"

    /** The band a single day's load falls into, so chart points can be tinted. */
    fun bandFor(value: Double): RevisionLoadBand {
        if (baselineDailyAverage <= 0.0) return RevisionLoadBand.NO_DATA
        if (value <= 0.0) return RevisionLoadBand.WELL_BELOW
        return RevisionLoadBand.from(value / baselineDailyAverage)
    }

    companion object {
        val EMPTY = RevisionLoadSnapshot(
            band = RevisionLoadBand.NO_DATA,
            percentChange = null,
            recentDailyAverage = 0.0,
            baselineDailyAverage = 0.0,
            series = emptyList(),
            windowTotal = 0.0,
            baselineDays = null
        )
    }
}

/**
 * The overall load plus a per-difficulty breakdown, so the detail screen can switch chips without
 * recomputing anything on every recomposition.
 */
data class RevisionLoadBreakdown(
    val overall: RevisionLoadSnapshot,
    val easy: RevisionLoadSnapshot,
    val medium: RevisionLoadSnapshot,
    val hard: RevisionLoadSnapshot
) {
    fun snapshot(scope: LoadScope): RevisionLoadSnapshot = when (scope) {
        LoadScope.ALL -> overall
        LoadScope.EASY -> easy
        LoadScope.MEDIUM -> medium
        LoadScope.HARD -> hard
    }

    companion object {
        val EMPTY = RevisionLoadBreakdown(
            overall = RevisionLoadSnapshot.EMPTY,
            easy = RevisionLoadSnapshot.EMPTY,
            medium = RevisionLoadSnapshot.EMPTY,
            hard = RevisionLoadSnapshot.EMPTY
        )

        /**
         * [revisions] may be the unfiltered list — rows without a `completedAt` are skipped inside
         * [ActivityMetrics.revisionLoad], so callers do not have to pre-filter and cannot get the
         * filter subtly wrong.
         */
        fun build(
            revisions: List<Revision>,
            solves: List<Solve>,
            today: LocalDate = LocalDate.now()
        ): RevisionLoadBreakdown {
            fun of(difficulty: String): RevisionLoadSnapshot {
                val scopedSolves = solves.filter { it.problem.difficulty.equals(difficulty, ignoreCase = true) }
                val scopedRevisions = revisions.filter {
                    it.problem.difficulty.equals(difficulty, ignoreCase = true)
                }
                return ActivityMetrics.revisionLoad(scopedRevisions, scopedSolves, today)
            }

            return RevisionLoadBreakdown(
                overall = ActivityMetrics.revisionLoad(revisions, solves, today),
                easy = of("easy"),
                medium = of("medium"),
                hard = of("hard")
            )
        }
    }
}
