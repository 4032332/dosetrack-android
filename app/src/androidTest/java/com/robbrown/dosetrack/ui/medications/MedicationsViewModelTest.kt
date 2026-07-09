package com.robbrown.dosetrack.ui.medications

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.repository.MedicationRepository
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
class MedicationsViewModelTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var viewModel: MedicationsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        viewModel = MedicationsViewModel(MedicationRepository(db.medicationDao()))
    }

    @After
    fun tearDown() = db.close()

    private fun medication(id: String, name: String = "Metformin") =
        Medication(id = id, name = name, dosage = "500mg", unit = "pill")

    @Test
    fun medications_startsEmpty() = runTest {
        assertTrue(viewModel.medications.first().isEmpty())
    }

    @Test
    fun addMedication_success_appearsInMedicationsList() = runTest {
        val result = viewModel.addMedication(medication("m1"), isPro = false)

        assertTrue(result.isSuccess)
        assertEquals(listOf("m1"), viewModel.medications.first().map { it.id })
    }

    @Test
    fun addMedication_overFreeLimit_fails_listUnchanged() = runTest {
        repeat(Constants.FREE_TIER_MED_LIMIT) { i -> viewModel.addMedication(medication("m$i"), isPro = false) }

        val result = viewModel.addMedication(medication("over"), isPro = false)

        assertTrue(result.isFailure)
        assertEquals(Constants.FREE_TIER_MED_LIMIT, viewModel.medications.first().size)
    }

    @Test
    fun softDelete_removesFromMedicationsList() = runTest {
        viewModel.addMedication(medication("m1"), isPro = false)
        val stored = viewModel.medications.first().first { it.id == "m1" }

        viewModel.softDelete(stored)

        assertFalse(viewModel.medications.first().any { it.id == "m1" })
    }
}
