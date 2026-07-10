package com.robbrown.dosetrack.logic

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.repository.DoseLogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoseLoggingServiceTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var repository: DoseLogRepository
    private lateinit var service: DoseLoggingService

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DoseLogRepository(db.doseLogDao())
        service = DoseLoggingService(repository, db.doseLogDao())
        db.medicationDao().insert(Medication(id = "med1", name = "Metformin", dosage = "500mg", unit = "pill"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun logTaken_freshOccurrence_createsRowWithTakenStatus() = runTest {
        val result = service.logTaken(medicationId = "med1", scheduledAt = 1000L)

        assertTrue(result.isSuccess)
        val logged = result.getOrThrow()
        assertEquals("med1", logged.medicationId)
        assertEquals(1000L, logged.scheduledAt)
        assertEquals("taken", logged.status)
        assertEquals(1, repository.observeForMedication("med1").first().size)
    }

    @Test
    fun logSkipped_freshOccurrence_createsRowWithSkippedStatus() = runTest {
        val result = service.logSkipped(medicationId = "med1", scheduledAt = 2000L)

        assertTrue(result.isSuccess)
        val logged = result.getOrThrow()
        assertEquals("skipped", logged.status)
        assertEquals(1, repository.observeForMedication("med1").first().size)
    }

    @Test
    fun logAgain_sameMedicationAndScheduledAt_updatesExistingRowInsteadOfDuplicating() = runTest {
        val first = service.logTaken(medicationId = "med1", scheduledAt = 3000L).getOrThrow()

        val second = service.logSkipped(medicationId = "med1", scheduledAt = 3000L).getOrThrow()

        // Same underlying row (id preserved), status updated, no duplicate created.
        assertEquals(first.id, second.id)
        assertEquals("skipped", second.status)
        val all = repository.observeForMedication("med1").first()
        assertEquals(1, all.size)
        assertEquals("skipped", all.first().status)
    }

    @Test
    fun logAgain_withNotes_updatesNotesOnExistingRow() = runTest {
        service.logTaken(medicationId = "med1", scheduledAt = 4000L)

        val updated = service.logTaken(medicationId = "med1", scheduledAt = 4000L, notes = "took it late").getOrThrow()

        assertEquals("took it late", updated.notes)
        assertEquals(1, repository.observeForMedication("med1").first().size)
    }
}
