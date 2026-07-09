package com.robbrown.dosetrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.robbrown.dosetrack.ui.history.HistoryScreen
import com.robbrown.dosetrack.ui.medications.MedicationsScreen
import com.robbrown.dosetrack.ui.settings.SettingsScreen
import com.robbrown.dosetrack.ui.today.TodayScreen

/**
 * Root navigation shell: a Scaffold with a bottom NavigationBar hosting the four
 * tab destinations. Equivalent to the iOS TabView.
 */
@Composable
fun DoseTrackNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.TODAY.route) { TodayScreen() }
            composable(Destination.MEDICATIONS.route) { MedicationsScreen() }
            composable(Destination.HISTORY.route) { HistoryScreen() }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
