package com.robbrown.dosetrack.logic

import com.robbrown.dosetrack.data.entity.Schedule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Expands a [Schedule] into concrete due-date/time occurrences.
 *
 * Ported from the pure day-selection logic in iOS's
 * `NotificationScheduler.isDue` / `buildRequests` (Services/NotificationScheduler.swift).
 * That logic:
 * - `"daily"` is always due.
 * - `"weekly"` and `"custom"` are due when `daysOfWeek` is empty (= every day, per the
 *   schema comment) or contains the current iOS-convention weekday (1=Sunday..7=Saturday).
 *   Notably, iOS treats `"custom"` identically to `"weekly"` for this day-of-week check —
 *   the *separate* `intervalDays`-driven due-date math (contraceptive-style "every N days
 *   since last taken") lives in `buildIntervalRequests` on the iOS side and is keyed off
 *   DoseLog history rather than a plain calendar walk, so it is out of scope for this
 *   pure `Schedule` -> occurrences generator and is intentionally not replicated here.
 * - `"as_needed"` is never due (produces no occurrences).
 * - Any other/unknown frequency string falls through to iOS's `default: return true` —
 *   i.e. treated as due every day, matching the Swift source exactly.
 * - A disabled schedule (`isEnabled == false`) never produces occurrences; iOS enforces
 *   this by only iterating `schedule.isEnabled` schedules before calling `isDue` at all.
 */
object ScheduleGenerator {

    /**
     * Returns the due occurrence(s) for [schedule] on [date], as epoch millis, or an empty
     * list if the schedule is not due that day. A due day always yields exactly one
     * occurrence (at `schedule.hour:schedule.minute`).
     */
    fun occurrencesOn(schedule: Schedule, date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): List<Long> {
        if (!schedule.isEnabled) return emptyList()
        if (!isDue(schedule, date)) return emptyList()

        val fireDate = ZonedDateTime.of(date, LocalTime.of(schedule.hour, schedule.minute), zoneId)
        return listOf(fireDate.toInstant().toEpochMilli())
    }

    /**
     * Returns the due occurrences for [schedule] across every day from [startDate] to
     * [endDate], inclusive, as epoch millis, in chronological order.
     */
    fun occurrencesInRange(
        schedule: Schedule,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Long> {
        if (startDate.isAfter(endDate)) return emptyList()

        val result = mutableListOf<Long>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            result.addAll(occurrencesOn(schedule, cursor, zoneId))
            cursor = cursor.plusDays(1)
        }
        return result
    }

    /**
     * Converts a [LocalDate] to the iOS/Foundation `Calendar.component(.weekday, from:)`
     * convention (1=Sunday..7=Saturday), from `java.time`'s ISO convention
     * (`DayOfWeek.MONDAY.value == 1` .. `DayOfWeek.SUNDAY.value == 7`).
     *
     * Explicit rather than relying on an implicit off-by-one: ISO Monday(1)->iOS 2,
     * ..., ISO Saturday(6)->iOS 7, ISO Sunday(7)->iOS 1.
     */
    fun iosWeekday(date: LocalDate): Int {
        val isoValue = date.dayOfWeek.value // Monday=1 .. Sunday=7
        return (isoValue % 7) + 1
    }

    private fun isDue(schedule: Schedule, date: LocalDate): Boolean {
        return when (schedule.frequency) {
            "daily" -> true
            "weekly", "custom" -> {
                val days = schedule.daysOfWeek
                days.isEmpty() || days.contains(iosWeekday(date))
            }
            "as_needed" -> false
            else -> true
        }
    }
}
