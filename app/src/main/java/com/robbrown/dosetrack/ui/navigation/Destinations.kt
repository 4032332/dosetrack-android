package com.robbrown.dosetrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The four bottom-tab destinations, mirroring the iOS TabView
 * (Today / Medications / History / Settings).
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    TODAY("today", "Today", Icons.Filled.Home),
    MEDICATIONS("medications", "Medications", Icons.Filled.Medication),
    HISTORY("history", "History", Icons.Filled.CalendarMonth),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}
