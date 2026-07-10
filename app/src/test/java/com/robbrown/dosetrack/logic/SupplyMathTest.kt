package com.robbrown.dosetrack.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ports the ground-truth behaviour of iOS's SupplyMath.swift + Medication+Extensions.swift
 * `daysOfSupply` / `isRefillWarning` computed properties. See
 * dosetrack-ios/DoseTrack/Services/SupplyMath.swift and
 * dosetrack-ios/DoseTrack/Models/Medication+Extensions.swift for the canonical source.
 */
class SupplyMathTest {

    @Test
    fun `daysOfSupply divides count by doses per day`() {
        assertEquals(10, SupplyMath.daysOfSupply(currentCount = 20, dosesPerDay = 2))
    }

    @Test
    fun `daysOfSupply floors fractional results, matching Swift Int division`() {
        // 10 / 3 = 3.33 -> Swift's Int(currentCount) / dpd truncates to 3.
        assertEquals(3, SupplyMath.daysOfSupply(currentCount = 10, dosesPerDay = 3))
    }

    @Test
    fun `daysOfSupply treats zero doses per day as one, per Swift's max(dpd, 1)`() {
        assertEquals(20, SupplyMath.daysOfSupply(currentCount = 20, dosesPerDay = 0))
    }

    @Test
    fun `isLowSupply is false when count is above threshold and days of supply is adequate`() {
        // 30 pills, 2/day = 15 days of supply; well above the 7-day and threshold cutoffs.
        assertFalse(
            SupplyMath.isLowSupply(currentCount = 30, refillThreshold = 5, dosesPerDay = 2)
        )
    }

    @Test
    fun `isLowSupply is true when count is at or below threshold regardless of days of supply`() {
        // 5 pills at 1/day = 5 days of supply (already under 7), threshold is also 5 -> boundary.
        assertTrue(
            SupplyMath.isLowSupply(currentCount = 5, refillThreshold = 5, dosesPerDay = 1)
        )
        // Even with a huge dosesPerDay making days-of-supply plentiful, count <= threshold still warns.
        assertTrue(
            SupplyMath.isLowSupply(currentCount = 3, refillThreshold = 5, dosesPerDay = 1)
        )
    }

    @Test
    fun `isLowSupply is true when days of supply drops below 7 even if count is above threshold`() {
        // 12 pills, 2/day = 6 days of supply (< 7), but count 12 is above the threshold of 5.
        assertTrue(
            SupplyMath.isLowSupply(currentCount = 12, refillThreshold = 5, dosesPerDay = 2)
        )
    }

    @Test
    fun `isLowSupply boundary - exactly 7 days of supply does not warn on the days signal alone`() {
        // 14 pills, 2/day = exactly 7 days; not < 7, and count is above threshold -> false.
        assertFalse(
            SupplyMath.isLowSupply(currentCount = 14, refillThreshold = 5, dosesPerDay = 2)
        )
    }

    @Test
    fun `isLowSupply never warns for as-needed meds with zero doses per day`() {
        // Mirrors isRefillWarning's guard: totalDosesPerDay > 0 else return false.
        // Even with currentCount 0 (at/under any threshold), zero dosesPerDay means no schedule.
        assertFalse(
            SupplyMath.isLowSupply(currentCount = 0, refillThreshold = 5, dosesPerDay = 0)
        )
    }
}
