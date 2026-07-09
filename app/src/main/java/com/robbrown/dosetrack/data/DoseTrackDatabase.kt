package com.robbrown.dosetrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.robbrown.dosetrack.data.converters.ListConverters
import com.robbrown.dosetrack.data.dao.DoseLogDao
import com.robbrown.dosetrack.data.dao.MedicationDao
import com.robbrown.dosetrack.data.dao.ScheduleDao
import com.robbrown.dosetrack.data.entity.DoseLog
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.entity.Schedule

/**
 * Room database holding the three core entities carried over from the iOS Core Data
 * model. Schema version 1; migrations are added as the schema evolves.
 */
@Database(
    entities = [Medication::class, Schedule::class, DoseLog::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ListConverters::class)
abstract class DoseTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseLogDao(): DoseLogDao
}
