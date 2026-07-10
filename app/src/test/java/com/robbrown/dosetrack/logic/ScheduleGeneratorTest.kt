package com.robbrown.dosetrack.logic

import com.robbrown.dosetrack.data.entity.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Ports the day-selection semantics of iOS's `NotificationScheduler.isDue` /
 * `buildRequests` (the pure schedule -> due-day logic, not the DoseLog-driven
 * contraceptive branch in `buildIntervalRequests`).
 */
class ScheduleGeneratorTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun schedule(
        frequency: String,
        hour: Int = 8,
        minute: Int = 30,
        daysOfWeek: List<Int> = emptyList(),
        intervalDays: Int = 0,
        isEnabled: Boolean = true,
    ) = Schedule(
        id = "sched-1",
        medicationId = "med-1",
        hour = hour,
        minute = minute,
        daysOfWeek = daysOfWeek,
        frequency = frequency,
        intervalDays = intervalDays,
        isEnabled = isEnabled,
    )

    private fun epochMillisFor(date: LocalDate, hour: Int, minute: Int): Long =
        ZonedDateTime.of(date, java.time.LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()

    @Test
    fun `daily schedule fires every day regardless of weekday`() {
        val s = schedule(frequency = "daily")
        // Try all seven days of a week.
        val monday = LocalDate.of(2026, 7, 6) // a Monday
        for (offset in 0..6) {
            val date = monday.plusDays(offset.toLong())
            val result = ScheduleGenerator.occurrencesOn(s, date, zone)
            assertEquals(listOf(epochMillisFor(date, 8, 30)), result)
        }
    }

    @Test
    fun `weekly schedule only fires on specified days`() {
        // iOS convention: 1=Sunday..7=Saturday. 2026-07-06 is a Monday -> iOS weekday 2.
        val monday = LocalDate.of(2026, 7, 6)
        val tuesday = monday.plusDays(1)
        val s = schedule(frequency = "weekly", daysOfWeek = listOf(2)) // Monday only

        assertEquals(listOf(epochMillisFor(monday, 8, 30)), ScheduleGenerator.occurrencesOn(s, monday, zone))
        assertTrue(ScheduleGenerator.occurrencesOn(s, tuesday, zone).isEmpty())
    }

    @Test
    fun `empty daysOfWeek means every day for weekly frequency`() {
        val s = schedule(frequency = "weekly", daysOfWeek = emptyList())
        val monday = LocalDate.of(2026, 7, 6)
        val sunday = LocalDate.of(2026, 7, 12)
        assertEquals(listOf(epochMillisFor(monday, 8, 30)), ScheduleGenerator.occurrencesOn(s, monday, zone))
        assertEquals(listOf(epochMillisFor(sunday, 8, 30)), ScheduleGenerator.occurrencesOn(s, sunday, zone))
    }

    @Test
    fun `custom frequency behaves like weekly using daysOfWeek`() {
        val monday = LocalDate.of(2026, 7, 6)
        val tuesday = monday.plusDays(1)
        val s = schedule(frequency = "custom", daysOfWeek = listOf(2), intervalDays = 1)

        assertEquals(listOf(epochMillisFor(monday, 8, 30)), ScheduleGenerator.occurrencesOn(s, monday, zone))
        assertTrue(ScheduleGenerator.occurrencesOn(s, tuesday, zone).isEmpty())
    }

    @Test
    fun `as_needed schedules never produce occurrences`() {
        val s = schedule(frequency = "as_needed")
        val date = LocalDate.of(2026, 7, 6)
        assertTrue(ScheduleGenerator.occurrencesOn(s, date, zone).isEmpty())
    }

    @Test
    fun `disabled schedules never produce occurrences regardless of frequency`() {
        val s = schedule(frequency = "daily", isEnabled = false)
        val date = LocalDate.of(2026, 7, 6)
        assertTrue(ScheduleGenerator.occurrencesOn(s, date, zone).isEmpty())
    }

    @Test
    fun `unknown frequency defaults to due every day, matching iOS default case`() {
        val s = schedule(frequency = "bogus")
        val date = LocalDate.of(2026, 7, 6)
        assertEquals(listOf(epochMillisFor(date, 8, 30)), ScheduleGenerator.occurrencesOn(s, date, zone))
    }

    @Test
    fun `occurrencesInRange collects one occurrence per due day across the range`() {
        val start = LocalDate.of(2026, 7, 6) // Monday
        val end = LocalDate.of(2026, 7, 12) // Sunday
        val s = schedule(frequency = "weekly", daysOfWeek = listOf(2, 4)) // Monday, Wednesday

        val result = ScheduleGenerator.occurrencesInRange(s, start, end, zone)

        val expected = listOf(
            epochMillisFor(LocalDate.of(2026, 7, 6), 8, 30),
            epochMillisFor(LocalDate.of(2026, 7, 8), 8, 30),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `occurrencesInRange is empty for as_needed across a whole range`() {
        val start = LocalDate.of(2026, 7, 6)
        val end = LocalDate.of(2026, 7, 12)
        val s = schedule(frequency = "as_needed")
        assertTrue(ScheduleGenerator.occurrencesInRange(s, start, end, zone).isEmpty())
    }

    @Test
    fun `iOS weekday conversion maps Sunday to 1 and Saturday to 7`() {
        // 2026-07-12 is a Sunday, 2026-07-11 is a Saturday.
        assertEquals(1, ScheduleGenerator.iosWeekday(LocalDate.of(2026, 7, 12)))
        assertEquals(7, ScheduleGenerator.iosWeekday(LocalDate.of(2026, 7, 11)))
        assertEquals(2, ScheduleGenerator.iosWeekday(LocalDate.of(2026, 7, 6))) // Monday
    }
}
