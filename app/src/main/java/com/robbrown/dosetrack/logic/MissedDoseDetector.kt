package com.robbrown.dosetrack.logic

import com.robbrown.dosetrack.data.entity.DoseLog

/**
 * Pure logic for deciding which scheduled dose occurrences count as "missed" for
 * caregiver alerting purposes. Ported 1:1 from the iOS
 * `DoseTrack/Services/MissedDoseDetector.swift`, which mirrors the server-side
 * Edge Function logic (a separate task) so it can be unit tested without a live
 * Supabase connection; the Edge Function is the actual source of truth for
 * production alerts.
 *
 * Dependency-free by design: callers supply already-computed occurrence
 * timestamps (epoch millis), e.g. from a schedule expander, rather than a
 * `Schedule` entity directly.
 */
object MissedDoseDetector {

    /** 60 minutes, per spec (sync-lag safety margin). */
    const val OVERDUE_THRESHOLD_MILLIS: Long = 60 * 60 * 1000L

    /**
     * @param occurrences candidate scheduled times (epoch millis) for one medication.
     * @param existingLogs DoseLogs already recorded for that medication (any status).
     * @param now current time (epoch millis), passed explicitly for deterministic tests.
     * @return the occurrence timestamps that count as missed.
     */
    fun findMissed(occurrences: List<Long>, existingLogs: List<DoseLog>, now: Long): List<Long> {
        val loggedTimes = existingLogs.mapNotNull { it.scheduledAt }.toSet()
        return occurrences.filter { scheduled ->
            scheduled !in loggedTimes && (now - scheduled) >= OVERDUE_THRESHOLD_MILLIS
        }
    }
}
