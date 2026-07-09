package com.robbrown.dosetrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.robbrown.dosetrack.ui.navigation.DoseTrackNavHost
import com.robbrown.dosetrack.ui.theme.DoseTrackTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Compose UI. Renders the four-tab navigation shell.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoseTrackTheme {
                DoseTrackNavHost()
            }
        }
    }
}
