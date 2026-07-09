package com.robbrown.dosetrack.data.repository

import com.robbrown.dosetrack.data.dao.DoseLogDao
import com.robbrown.dosetrack.data.entity.DoseLog
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown by [DoseLogRepository.logDose] when `status` isn't one of the allowed values. */
class InvalidDoseLogStatusException(status: String) : Exception(
    "status must be one of ${DoseLogRepository.VALID_STATUSES}, was \"$status\""
)

/**
 * Records dose confirmations. Validates `status` against the schema's allowed set
 * before it reaches the History/adherence calculations.
 */
@Singleton
class DoseLogRepository @Inject constructor(
    private val dao: DoseLogDao,
) {
    fun observeForMedication(medicationId: String): Flow<List<DoseLog>> =
        dao.observeForMedication(medicationId)

    fun observeInRange(startInclusive: Long, endInclusive: Long): Flow<List<DoseLog>> =
        dao.observeInRange(startInclusive, endInclusive)

    suspend fun logDose(
        medicationId: String,
        scheduledAt: Long,
        status: String,
        notes: String? = null,
        loggedAt: Long = System.currentTimeMillis(),
    ): Result<DoseLog> {
        if (status !in VALID_STATUSES) return Result.failure(InvalidDoseLogStatusException(status))
        val entry = DoseLog(
            id = UUID.randomUUID().toString(),
            medicationId = medicationId,
            scheduledAt = scheduledAt,
            loggedAt = loggedAt,
            status = status,
            notes = notes,
        )
        dao.insert(entry)
        return Result.success(entry)
    }

    companion object {
        val VALID_STATUSES = setOf("taken", "skipped", "missed")
    }
}
