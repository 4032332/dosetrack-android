package com.robbrown.dosetrack.ui.medications

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.repository.MedicationRepository
import com.robbrown.dosetrack.ui.theme.DoseTrackTheme
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MedicationsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    @Test
    fun existingMedication_isDisplayed() = runTest {
        db.medicationDao().insert(
            Medication(id = "m1", name = "Metformin", dosage = "500mg", unit = "pill")
        )

        composeRule.setContent { DoseTrackTheme { MedicationsScreen(viewModel = viewModel) } }

        composeRule.onNodeWithText("Metformin").assertExists()
    }

    @Test
    fun tappingFab_thenAdd_addsMedicationToList() {
        composeRule.setContent { DoseTrackTheme { MedicationsScreen(viewModel = viewModel) } }

        composeRule.onNodeWithContentDescription("Add medication").performClick()
        composeRule.onNodeWithText("Name").performTextInput("Ibuprofen")
        composeRule.onNodeWithText("Dosage").performTextInput("200mg")
        composeRule.onNodeWithText("Unit").performTextInput("pill")
        composeRule.onNodeWithText("Add").performClick()

        composeRule.onNodeWithText("Ibuprofen").assertExists()
    }
}
