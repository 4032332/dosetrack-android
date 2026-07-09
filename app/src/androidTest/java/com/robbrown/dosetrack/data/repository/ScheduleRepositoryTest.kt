package com.robbrown.dosetrack.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.entity.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleRepositoryTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var repository: ScheduleRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScheduleRepository(db.scheduleDao())
        db.medicationDao().insert(Medication(id = "med1", name = "Metformin", dosage = "500mg", unit = "pill"))
    }

    @After
    fun tearDown() = db.close()

    private fun schedule(id: String, hour: Int = 8, minute: Int = 30) =
        Schedule(id = id, medicationId = "med1", hour = hour, minute = minute, frequency = "daily")

    @Test
    fun addSchedule_validTime_succeeds() = runTest {
        val result = repository.addSchedule(schedule("s1", hour = 23, minute = 59))

        assertTrue(result.isSuccess)
        assertEquals(1, repository.observeForMedication("med1").first().size)
    }

    @Test
    fun addSchedule_hourOutOfRange_fails_andDoesNotInsert() = runTest {
        val result = repository.addSchedule(schedule("s1", hour = 24))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidScheduleException)
        assertTrue(repository.observeForMedication("med1").first().isEmpty())
    }

    @Test
    fun addSchedule_minuteOutOfRange_fails() = runTest {
        val result = repository.addSchedule(schedule("s1", minute = 60))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidScheduleException)
    }

    @Test
    fun addSchedule_negativeHour_fails() = runTest {
        val result = repository.addSchedule(schedule("s1", hour = -1))

        assertTrue(result.isFailure)
    }
}
