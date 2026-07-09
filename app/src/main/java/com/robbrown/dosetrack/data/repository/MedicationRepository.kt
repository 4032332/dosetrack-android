package com.robbrown.dosetrack.data.repository

import com.robbrown.dosetrack.data.dao.MedicationDao
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.util.Constants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown by [MedicationRepository.addMedication] when a free-tier user is at the cap. */
class FreeTierLimitExceededException : Exception(
    "Free tier is limited to ${Constants.FREE_TIER_MED_LIMIT} medications. Upgrade to Pro for unlimited."
)

/**
 * Enforces the medication business rules on top of [MedicationDao]:
 * the free-tier cap (Critical Rule: unlimited medications is Pro, reminders always work
 * free) and soft-delete-before-permanent-delete (Critical Rule: never delete a medication
 * permanently without a soft-delete step first).
 */
@Singleton
class MedicationRepository @Inject constructor(
    private val dao: MedicationDao,
) {
    fun observeActiveMedications(): Flow<List<Medication>> = dao.observeActive()

    /**
     * Inserts [medication] unless a free-tier user is already at
     * [Constants.FREE_TIER_MED_LIMIT] active medications, in which case this returns a
     * failed [Result] wrapping [FreeTierLimitExceededException] and does not insert.
     */
    suspend fun addMedication(medication: Medication, isPro: Boolean): Result<Unit> {
        if (!isPro && dao.activeCount() >= Constants.FREE_TIER_MED_LIMIT) {
            return Result.failure(FreeTierLimitExceededException())
        }
        dao.insert(medication)
        return Result.success(Unit)
    }

    suspend fun updateMedication(medication: Medication) = dao.update(medication)

    /** Marks the medication inactive; the record and its history are kept. */
    suspend fun softDelete(medication: Medication) = dao.update(medication.copy(isActive = false))

    /** Permanently removes the medication (and, via cascade, its schedules/logs). */
    suspend fun permanentlyDelete(medication: Medication) = dao.delete(medication)
}
