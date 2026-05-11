package com.tracker.bustracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tracker.bustracker.presentation.navigation.BusTrackerNavHost
import com.tracker.bustracker.presentation.theme.BusTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BusTrackerTheme {
                BusTrackerNavHost()
            }
        }
    }
}
