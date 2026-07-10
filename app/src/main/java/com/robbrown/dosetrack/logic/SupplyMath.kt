package com.robbrown.dosetrack.logic

/**
 * Port of iOS's supply/refill math — the canonical source of truth for "needs restock soon".
 *
 * Ground truth: dosetrack-ios/DoseTrack/Services/SupplyMath.swift and the `daysOfSupply` /
 * `isRefillWarning` computed properties on dosetrack-ios/DoseTrack/Models/Medication+Extensions.swift.
 *
 * [daysOfSupply] mirrors `Medication.daysOfSupply`: `dpd = max(totalDosesPerDay, 1)`, then
 * integer division `currentCount / dpd` (truncating, not rounding — e.g. 10 pills at 3/day is
 * 3 days of supply, not 3.33 or 4). Doses-per-day is floored at 1 even when 0, so this never
 * divides by zero.
 *
 * [isLowSupply] mirrors `Medication.isRefillWarning`: gated on the medication actually being on
 * an active dosing schedule (`totalDosesPerDay > 0`) — as-needed medications with no schedule
 * never warn, regardless of currentCount or refillThreshold. When on a schedule, it warns when
 * EITHER the raw count has dropped to/below the user's refill threshold, OR the estimated days
 * of supply remaining is under a week (7 days) — whichever signal fires first.
 */
object SupplyMath {

    fun daysOfSupply(currentCount: Int, dosesPerDay: Int): Int {
        val dpd = maxOf(dosesPerDay, 1)
        return currentCount / dpd
    }

    fun isLowSupply(currentCount: Int, refillThreshold: Int, dosesPerDay: Int): Boolean {
        if (dosesPerDay <= 0) return false
        return currentCount <= refillThreshold || daysOfSupply(currentCount, dosesPerDay) < 7
    }
}
