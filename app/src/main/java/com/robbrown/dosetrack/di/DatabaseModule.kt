package com.robbrown.dosetrack.di

import android.content.Context
import androidx.room.Room
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.dao.DoseLogDao
import com.robbrown.dosetrack.data.dao.MedicationDao
import com.robbrown.dosetrack.data.dao.ScheduleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the singleton [DoseTrackDatabase] and its DAOs to the Hilt graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoseTrackDatabase =
        Room.databaseBuilder(context, DoseTrackDatabase::class.java, "dosetrack.db").build()

    @Provides
    fun provideMedicationDao(db: DoseTrackDatabase): MedicationDao = db.medicationDao()

    @Provides
    fun provideScheduleDao(db: DoseTrackDatabase): ScheduleDao = db.scheduleDao()

    @Provides
    fun provideDoseLogDao(db: DoseTrackDatabase): DoseLogDao = db.doseLogDao()
}
