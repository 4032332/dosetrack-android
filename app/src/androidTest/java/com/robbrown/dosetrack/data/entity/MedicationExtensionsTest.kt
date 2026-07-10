package com.robbrown.dosetrack.data.entity

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationExtensionsTest {

    private fun medication(
        colorHex: String? = null,
        currentCount: Int = 30,
        refillThreshold: Int = 7,
        totalDosesPerDay: Int = 1,
    ) = Medication(
        id = "m1",
        name = "Metformin",
        dosage = "500mg",
        unit = "pill",
        colorHex = colorHex,
        currentCount = currentCount,
        refillThreshold = refillThreshold,
        totalDosesPerDay = totalDosesPerDay,
    )

    @Test
    fun toColor_parsesHex() {
        assertEquals(Color(0xFF5B8AF0), medication(colorHex = "#5B8AF0").toColor())
    }

    @Test
    fun toColor_missingHex_defaultsToBrandBlue() {
        assertEquals(Color(0xFF5B8AF0), medication(colorHex = null).toColor())
    }

    @Test
    fun isRefillWarning_countAtOrBelowThreshold_true() {
        assertTrue(medication(currentCount = 7, refillThreshold = 7).isRefillWarning)
        assertTrue(medication(currentCount = 5, refillThreshold = 7).isRefillWarning)
    }

    @Test
    fun isRefillWarning_countAboveThreshold_false() {
        assertFalse(medication(currentCount = 30, refillThreshold = 7).isRefillWarning)
    }

    @Test
    fun isRefillWarning_asNeededMedication_neverWarns() {
        assertFalse(medication(currentCount = 0, refillThreshold = 7, totalDosesPerDay = 0).isRefillWarning)
    }
}
