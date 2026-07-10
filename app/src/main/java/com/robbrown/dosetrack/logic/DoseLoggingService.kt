package com.robbrown.dosetrack.logic

import com.robbrown.dosetrack.data.dao.DoseLogDao
import com.robbrown.dosetrack.data.entity.DoseLog
import com.robbrown.dosetrack.data.repository.DoseLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single write path for logging a dose (taken / skipped). Every caller — TodayViewModel,
 * a notification-action handler, a widget intent bridge, etc. — should go through here so the
 * behavior (idempotent update of an existing occurrence, rather than duplicate rows) is
 * identical no matter how a dose gets logged. Ported from iOS's DoseLoggingService.swift.
 *
 * Idempotency: logging a dose for a medication+scheduledAt that already has a [DoseLog]
 * UPDATES the existing row in place (status, loggedAt, notes) instead of inserting a
 * duplicate — this mirrors the Swift source's `existingLog` branch, which reuses the row
 * when the caller already resolved one for that occurrence.
 */
@Singleton
class DoseLoggingService @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val doseLogDao: DoseLogDao,
) {

    suspend fun logTaken(medicationId: String, scheduledAt: Long, notes: String? = null): Result<DoseLog> =
        log(medicationId, scheduledAt, status = "taken", notes = notes)

    suspend fun logSkipped(medicationId: String, scheduledAt: Long, notes: String? = null): Result<DoseLog> =
        log(medicationId, scheduledAt, status = "skipped", notes = notes)

    private suspend fun log(
        medicationId: String,
        scheduledAt: Long,
        status: String,
        notes: String?,
    ): Result<DoseLog> {
        val existing = doseLogRepository.observeForMedication(medicationId).first()
            .firstOrNull { it.scheduledAt == scheduledAt }

        val now = System.currentTimeMillis()

        if (existing != null) {
            val updated = existing.copy(
                status = status,
                loggedAt = now,
                updatedAt = now,
                notes = notes ?: existing.notes,
            )
            doseLogDao.update(updated)

            // TODO(M2): cancel/reschedule the notification for this occurrence once
            // NotificationScheduler exists.

            return Result.success(updated)
        }

        val result = doseLogRepository.logDose(
            medicationId = medicationId,
            scheduledAt = scheduledAt,
            status = status,
            notes = notes,
            loggedAt = now,
        )

        // TODO(M2): cancel/reschedule the notification for this occurrence once
        // NotificationScheduler exists.

        return result
    }
}
