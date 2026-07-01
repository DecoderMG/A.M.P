package com.dmgproductions.amp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dmgproductions.amp.player.ActivityState
import com.dmgproductions.amp.streaming.PlaybackSource
import com.dmgproductions.amp.streaming.StreamingController
import com.dmgproductions.amp.streaming.YouTubeMusicSource
import com.dmgproductions.amp.ui.components.AmpScreenHeader

@Composable
fun SourcesScreen(
    currentSource: PlaybackSource,
    onSelectSource: (PlaybackSource) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = remember { StreamingController.get(context) }

    // Re-check notification access whenever the screen resumes (e.g. returning
    // from system settings after granting it).
    var hasNotificationAccess by remember { mutableStateOf(YouTubeMusicSource.hasNotificationAccess(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = YouTubeMusicSource.hasNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AmpScreenHeader(
            title = "Audio sources",
            subtitle = "Play on-device, or let A.M.P drive an installed streaming app by activity.",
            onBack = onBack,
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlaybackSource.entries.forEach { source ->
                SourceCard(source = source, selected = source == currentSource, onSelect = { onSelectSource(source) })
            }

            when (currentSource) {
                PlaybackSource.YOUTUBE_MUSIC -> YouTubeConfig(
                    controller = controller,
                    hasNotificationAccess = hasNotificationAccess,
                    onGrantAccess = { context.startActivity(YouTubeMusicSource.notificationAccessIntent()) },
                )
                PlaybackSource.SPOTIFY -> SpotifyConfig(controller = controller)
                PlaybackSource.LOCAL -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SourceCard(source: PlaybackSource, selected: Boolean, onSelect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f)) {
                Text(source.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when (source) {
                        PlaybackSource.LOCAL -> "Built-in activity crossfade of bundled tracks"
                        PlaybackSource.YOUTUBE_MUSIC -> "Control the installed YouTube Music app"
                        PlaybackSource.SPOTIFY -> "Control Spotify (requires SDK setup)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun YouTubeConfig(
    controller: StreamingController,
    hasNotificationAccess: Boolean,
    onGrantAccess: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("YouTube Music", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!hasNotificationAccess) {
                Text(
                    "Grant Notification access so A.M.P can read what's playing and show transport controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onGrantAccess) { Text("Grant notification access") }
            } else {
                Text("Notification access granted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Paste a YouTube Music playlist link for each activity. A.M.P opens it when that activity is detected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlaylistFields(controller, PlaybackSource.YOUTUBE_MUSIC, "https://music.youtube.com/playlist?list=…")
        }
    }
}

@Composable
private fun SpotifyConfig(controller: StreamingController) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spotify", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Spotify control needs the App Remote SDK wired in (see docs/streaming/SPOTIFY.md). " +
                    "You can store per-activity playlist URIs now; they'll be used once the SDK is added.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PlaylistFields(controller, PlaybackSource.SPOTIFY, "spotify:playlist:…")
        }
    }
}

@Composable
private fun PlaylistFields(controller: StreamingController, source: PlaybackSource, placeholder: String) {
    ActivityState.entries.forEach { activity ->
        var value by remember(source, activity) { mutableStateOf(controller.playlist(source, activity)) }
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                controller.setPlaylist(source, activity, it)
            },
            label = { Text(activity.label) },
            placeholder = { Text(placeholder, maxLines = 1) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
