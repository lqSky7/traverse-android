package com.traverse.android.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The single place timestamps get parsed. Mirrors iOS `ActivityTimestamp`.
 *
 * Before this existed, each card did its own `LocalDateTime.parse(raw.take(19))`. That
 * happens to work for `2026-01-05T09:30:00Z` and for a fractional `...T09:30:00.123Z`, so
 * it looked fine — but `take(19)` silently *discards* a numeric UTC offset, so any row the
 * backend stamped with `+05:30` was bucketed into the wrong hour, and any row it stamped
 * with an offset was bucketed into the wrong day. Neither failure produces an error; the
 * chart just quietly loses or misplaces rows.
 *
 * The three parsers below cover every shape the API produces, most-specific first.
 */
object ActivityTimestamp {

    private val dayKeyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Parses an ISO-8601 timestamp. Returns `null` only for genuinely unparseable input.
     *
     * `OffsetDateTime` is tried first because it accepts `Z`, a numeric offset, *and*
     * fractional seconds in one pass — it alone covers both flavours the backend emits.
     * The remaining two are fallbacks for a value with no zone at all, which is read as
     * device-local (the only defensible guess, and the same one the old code made).
     */
    fun parse(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null

        runCatching { OffsetDateTime.parse(raw).toInstant() }
            .onSuccess { return it }

        runCatching { Instant.parse(raw) }
            .onSuccess { return it }

        return runCatching {
            LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
    }

    /** The local calendar day an instant falls on — the bucket key every chart groups by. */
    fun dayKey(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        instant.atZone(zone).toLocalDate()

    /** Convenience for the common `parse` + `dayKey` pair. */
    fun dayKey(raw: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
        parse(raw)?.let { dayKey(it, zone) }

    /** `yyyy-MM-dd` in the local calendar — matches the freeze-date keys the backend returns. */
    fun formatDayKey(date: LocalDate): String = date.format(dayKeyFormatter)

    /** Start of the local day, for grouping. */
    fun startOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): Instant =
        dayKey(instant, zone).atStartOfDay(zone).toInstant()
}

/**
 * When the user last touched this problem.
 *
 * `solvedAt` is stamped once, when a problem is first accepted, and never moves — re-solving
 * or revising a problem only bumps `lastActivityAt`. Anything that claims to describe *when
 * you work* has to read [activityAt], or a revision-heavy account looks like it only ever
 * solved problems on the day it first met them. Mirrors iOS `Solve.activityAt`.
 */
val Solve.activityAt: String
    get() = lastActivityAt ?: solvedAt

val Solve.activityInstant: Instant?
    get() = ActivityTimestamp.parse(activityAt)
