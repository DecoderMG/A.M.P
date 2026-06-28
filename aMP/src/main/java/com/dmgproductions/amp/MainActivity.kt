package com.dmgproductions.amp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dmgproductions.amp.ui.AmpApp
import com.dmgproductions.amp.ui.theme.AmpTheme

/**
 * Single Compose host for A.M.P. The entire UI is built with Jetpack Compose +
 * Material 3; this activity installs the theme and hands off to [AmpApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var dynamicColor by rememberSaveable { mutableStateOf(true) }
            AmpTheme(dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AmpApp(
                        dynamicColor = dynamicColor,
                        onDynamicColorChange = { dynamicColor = it },
                    )
                }
            }
        }
    }
}
