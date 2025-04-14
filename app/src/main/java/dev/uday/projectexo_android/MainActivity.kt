package dev.uday.projectexo_android

import AppNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Properly configure edge-to-edge display
        enableEdgeToEdge()

        setContent {
            MaterialTheme() {
                // Using Scaffold to handle insets properly
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    AppNavigation()
                }
            }
        }
    }
}