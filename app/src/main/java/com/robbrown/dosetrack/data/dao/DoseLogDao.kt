package com.robbrown.dosetrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.robbrown.dosetrack.data.entity.DoseLog
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [DoseLog].
 */
@Dao
interface DoseLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doseLog: DoseLog)

    @Update
    suspend fun update(doseLog: DoseLog)

    @Delete
    suspend fun delete(doseLog: DoseLog)

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getById(id: String): DoseLog?

    /** All logs for a medication, most recently scheduled first. */
    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY scheduledAt DESC")
    fun observeForMedication(medicationId: String): Flow<List<DoseLog>>

    /** Logs across all medications whose scheduledAt falls within [startInclusive, endInclusive] — used by the History screen's date-range filter. */
    @Query(
        "SELECT * FROM dose_logs WHERE scheduledAt BETWEEN :startInclusive AND :endInclusive " +
            "ORDER BY scheduledAt ASC"
    )
    fun observeInRange(startInclusive: Long, endInclusive: Long): Flow<List<DoseLog>>
}
