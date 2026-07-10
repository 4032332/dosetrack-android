package com.robbrown.dosetrack.data.entity

import androidx.compose.ui.graphics.Color

/** Matches the iOS default (Medication+Extensions.swift `color`): brand blue when unset. */
private const val DEFAULT_COLOR_HEX = "#5B8AF0"

/** Parses [Medication.colorHex] (e.g. "#5B8AF0") into a Compose [Color], mirroring iOS. */
fun Medication.toColor(): Color {
    val hex = (colorHex ?: DEFAULT_COLOR_HEX).removePrefix("#")
    val colorInt = hex.toLong(16) or 0xFF000000
    return Color(colorInt)
}

/**
 * Simplified port of iOS's `isRefillWarning`: true when the medication is on an active
 * dosing schedule and its current count has dropped to/below its refill threshold.
 *
 * iOS additionally warns when `daysOfSupply < 7`, computed from schedule frequency via
 * SupplyMath — that calculation depends on schedules being wired up, which isn't built
 * on Android yet. Add that half once schedules are editable from the UI.
 */
val Medication.isRefillWarning: Boolean
    get() = totalDosesPerDay > 0 && currentCount <= refillThreshold
