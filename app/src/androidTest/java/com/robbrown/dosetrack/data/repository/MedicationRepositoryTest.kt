package com.robbrown.dosetrack.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationRepositoryTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var repository: MedicationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepository(db.medicationDao())
    }

    @After
    fun tearDown() = db.close()

    private fun medication(id: String, name: String = "Metformin") =
        Medication(id = id, name = name, dosage = "500mg", unit = "pill")

    @Test
    fun addMedication_belowFreeLimit_succeeds() = runTest {
        val result = repository.addMedication(medication("m1"), isPro = false)

        assertTrue(result.isSuccess)
        assertEquals(listOf("m1"), repository.observeActiveMedications().first().map { it.id })
    }

    @Test
    fun addMedication_atFreeLimit_freeUser_fails_andDoesNotInsert() = runTest {
        repeat(Constants.FREE_TIER_MED_LIMIT) { i -> repository.addMedication(medication("m$i"), isPro = false) }

        val result = repository.addMedication(medication("m-over"), isPro = false)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FreeTierLimitExceededException)
        assertEquals(Constants.FREE_TIER_MED_LIMIT, repository.observeActiveMedications().first().size)
    }

    @Test
    fun addMedication_atFreeLimit_proUser_succeeds() = runTest {
        repeat(Constants.FREE_TIER_MED_LIMIT) { i -> repository.addMedication(medication("m$i"), isPro = false) }

        val result = repository.addMedication(medication("m-pro"), isPro = true)

        assertTrue(result.isSuccess)
        assertEquals(Constants.FREE_TIER_MED_LIMIT + 1, repository.observeActiveMedications().first().size)
    }

    @Test
    fun softDelete_marksInactive_butRecordStillExists() = runTest {
        repository.addMedication(medication("m1"), isPro = false)
        val stored = db.medicationDao().getById("m1")!!

        repository.softDelete(stored)

        assertFalse(repository.observeActiveMedications().first().any { it.id == "m1" })
        assertEquals(false, db.medicationDao().getById("m1")?.isActive)
    }

    @Test
    fun permanentlyDelete_removesTheRecord() = runTest {
        repository.addMedication(medication("m1"), isPro = false)
        val stored = db.medicationDao().getById("m1")!!

        repository.permanentlyDelete(stored)

        assertEquals(null, db.medicationDao().getById("m1"))
    }
}
