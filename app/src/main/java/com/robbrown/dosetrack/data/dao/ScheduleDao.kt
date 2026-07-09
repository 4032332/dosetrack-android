package com.robbrown.dosetrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.robbrown.dosetrack.data.entity.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [Schedule].
 */
@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: Schedule)

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: String): Schedule?

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    fun observeForMedication(medicationId: String): Flow<List<Schedule>>
}
