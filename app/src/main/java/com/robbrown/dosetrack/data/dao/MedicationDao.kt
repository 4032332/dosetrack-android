package com.robbrown.dosetrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.robbrown.dosetrack.data.entity.Medication
import kotlinx.coroutines.flow.Flow

/**
 * Data access for [Medication].
 */
@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication)

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: String): Medication?

    /** Active medications in user sort order; emits on every change. */
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<Medication>>

    /** Count of active medications — used to enforce the free-tier limit. */
    @Query("SELECT COUNT(*) FROM medications WHERE isActive = 1")
    suspend fun activeCount(): Int
}
