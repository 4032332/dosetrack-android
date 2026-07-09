package com.robbrown.dosetrack.data.repository

import com.robbrown.dosetrack.data.dao.ScheduleDao
import com.robbrown.dosetrack.data.entity.Schedule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Thrown by [ScheduleRepository.addSchedule] / [updateSchedule] when hour/minute are out of range. */
class InvalidScheduleException(message: String) : Exception(message)

/**
 * Validates and persists [Schedule]s. Rejects an hour/minute outside 0..23 / 0..59 before
 * it reaches the notification engine, since a bad time there would silently fail to fire.
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: ScheduleDao,
) {
    fun observeForMedication(medicationId: String): Flow<List<Schedule>> =
        dao.observeForMedication(medicationId)

    suspend fun addSchedule(schedule: Schedule): Result<Unit> {
        validate(schedule)?.let { return Result.failure(it) }
        dao.insert(schedule)
        return Result.success(Unit)
    }

    suspend fun updateSchedule(schedule: Schedule): Result<Unit> {
        validate(schedule)?.let { return Result.failure(it) }
        dao.update(schedule)
        return Result.success(Unit)
    }

    suspend fun deleteSchedule(schedule: Schedule) = dao.delete(schedule)

    private fun validate(schedule: Schedule): InvalidScheduleException? = when {
        schedule.hour !in 0..23 -> InvalidScheduleException("hour must be 0..23, was ${schedule.hour}")
        schedule.minute !in 0..59 -> InvalidScheduleException("minute must be 0..59, was ${schedule.minute}")
        else -> null
    }
}
