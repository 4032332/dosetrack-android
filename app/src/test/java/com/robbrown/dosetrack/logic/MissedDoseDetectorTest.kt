package com.robbrown.dosetrack.logic

import com.robbrown.dosetrack.data.entity.DoseLog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ported from DoseTrack/Services/MissedDoseDetector.swift (iOS). Mirrors the
 * server-side Edge Function overdue-dose logic used for caregiver alerting.
 * `overdueThreshold` is 60 minutes (sync-lag safety margin per spec).
 */
class MissedDoseDetectorTest {

    private val oneHourMillis = 60 * 60 * 1000L
    private val now = 1_000_000_000_000L

    private fun log(scheduledAt: Long, status: String = "taken"): DoseLog = DoseLog(
        id = "log-$scheduledAt-$status",
        medicationId = "med-1",
        scheduledAt = scheduledAt,
        loggedAt = scheduledAt,
        status = status,
    )

    @Test
    fun `occurrence far in the past with no log is missed`() {
        val scheduled = now - (oneHourMillis * 5)
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = emptyList(),
            now = now,
        )
        assertEquals(listOf(scheduled), result)
    }

    @Test
    fun `occurrence with matching taken log is not missed`() {
        val scheduled = now - (oneHourMillis * 5)
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = listOf(log(scheduled, "taken")),
            now = now,
        )
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `occurrence with matching skipped log is not missed`() {
        val scheduled = now - (oneHourMillis * 5)
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = listOf(log(scheduled, "skipped")),
            now = now,
        )
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `occurrence within grace period of now is not yet missed`() {
        // 10 minutes ago - well within the 60 minute threshold.
        val scheduled = now - (10 * 60 * 1000L)
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = emptyList(),
            now = now,
        )
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `occurrence in the future is not missed`() {
        val scheduled = now + oneHourMillis
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = emptyList(),
            now = now,
        )
        assertEquals(emptyList<Long>(), result)
    }

    @Test
    fun `occurrence exactly at the 60 minute threshold is missed (inclusive boundary)`() {
        val scheduled = now - oneHourMillis
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = emptyList(),
            now = now,
        )
        assertEquals(listOf(scheduled), result)
    }

    @Test
    fun `occurrence one millisecond under the threshold is not missed`() {
        val scheduled = now - oneHourMillis + 1
        val result = MissedDoseDetector.findMissed(
            occurrences = listOf(scheduled),
            existingLogs = emptyList(),
            now = now,
        )
        assertEquals(emptyList<Long>(), result)
    }
}
