package com.dmgproductions.amp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.dmgproductions.amp.gestures.GestureServiceClient
import com.dmgproductions.amp.ui.AmpApp
import com.dmgproductions.amp.ui.theme.AmpTheme

/**
 * Single Compose host for A.M.P. Installs the theme, requests the audio
 * permission that powers the FFT visualizer and motion detection, then hands
 * off to [AmpApp].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                GestureServiceClient.get(context).setAudioPermissionGranted(granted)
            }
            LaunchedEffect(Unit) {
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (alreadyGranted) {
                    GestureServiceClient.get(context).setAudioPermissionGranted(true)
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

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
