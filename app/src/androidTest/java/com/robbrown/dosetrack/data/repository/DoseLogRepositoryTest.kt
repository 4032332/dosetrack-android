package com.robbrown.dosetrack.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoseLogRepositoryTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var repository: DoseLogRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DoseLogRepository(db.doseLogDao())
        db.medicationDao().insert(Medication(id = "med1", name = "Metformin", dosage = "500mg", unit = "pill"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun logDose_validStatus_insertsAndReturnsIt() = runTest {
        val result = repository.logDose(
            medicationId = "med1",
            scheduledAt = 1000L,
            status = "taken",
            loggedAt = 1050L,
        )

        assertTrue(result.isSuccess)
        val logged = result.getOrThrow()
        assertEquals("med1", logged.medicationId)
        assertEquals(1000L, logged.scheduledAt)
        assertEquals(1050L, logged.loggedAt)
        assertEquals("taken", logged.status)
        assertEquals(listOf(logged.id), repository.observeForMedication("med1").first().map { it.id })
    }

    @Test
    fun logDose_invalidStatus_fails_andDoesNotInsert() = runTest {
        val result = repository.logDose(medicationId = "med1", scheduledAt = 1000L, status = "bogus")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidDoseLogStatusException)
        assertTrue(repository.observeForMedication("med1").first().isEmpty())
    }

    @Test
    fun observeInRange_delegatesToDao() = runTest {
        repository.logDose(medicationId = "med1", scheduledAt = 500L, status = "taken", loggedAt = 500L)
        repository.logDose(medicationId = "med1", scheduledAt = 1500L, status = "taken", loggedAt = 1500L)

        val inRange = repository.observeInRange(startInclusive = 1000L, endInclusive = 2000L).first()

        assertEquals(1, inRange.size)
        assertEquals(1500L, inRange.first().scheduledAt)
    }
}
