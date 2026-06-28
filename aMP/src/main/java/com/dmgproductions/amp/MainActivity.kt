package com.dmgproductions.amp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dmgproductions.amp.ui.theme.AmpTheme

/**
 * Single Compose host for A.M.P. The entire UI is built with Jetpack Compose +
 * Material 3; this activity just installs the theme and hosts the navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AmpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AmpApp()
                }
            }
        }
    }
}

/** Temporary root — replaced by the navigation shell in Stage 3. */
@Composable
private fun AmpApp() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "A.M.P", style = MaterialTheme.typography.displayLarge)
    }
}
