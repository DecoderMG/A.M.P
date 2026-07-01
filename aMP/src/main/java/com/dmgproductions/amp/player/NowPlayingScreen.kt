package com.dmgproductions.amp.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmgproductions.amp.ui.components.CircularVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onSelectActivity: (ActivityState) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent by animateColorAsState(state.activity.accent, tween(600), label = "accent")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        ActivityStatusPill(state = state, accent = accent)

        Spacer(Modifier.height(20.dp))

        // ── Hero: album art ringed by the spectrum visualizer ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularVisualizer(
                levels = state.levels,
                accent = accent,
                modifier = Modifier.fillMaxSize(),
            )
            AlbumArt(
                accent = accent,
                modifier = Modifier.fillMaxSize(0.64f),
            )
        }

        Spacer(Modifier.height(24.dp))
        TrackInfo(title = state.displayTitle, subtitle = state.displaySubtitle)

        Spacer(Modifier.height(16.dp))
        if (state.isStreaming) {
            SourceBanner(label = state.source.label, accent = accent)
        } else {
            Scrubber(progress = state.progress, accent = accent, onSeek = onSeek)
            TimeRow(positionMs = state.positionMs, durationMs = state.durationMs)
        }

        Spacer(Modifier.height(8.dp))
        TransportControls(
            isPlaying = state.effectivePlaying,
            accent = accent,
            onTogglePlay = onTogglePlay,
            onNext = onNext,
            onPrevious = onPrevious,
        )

        Spacer(Modifier.height(20.dp))
        ActivitySelector(
            state = state,
            accent = accent,
            onSelectActivity = onSelectActivity,
            onToggleAutoSync = onToggleAutoSync,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ActivityStatusPill(state: PlayerUiState, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(accent),
        )
        AnimatedContent(
            targetState = state.activity,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "activityLabel",
        ) { activity ->
            Column {
                Text(
                    text = if (state.autoSync) "Detected · ${activity.label}" else activity.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Text(
                    text = activity.tagline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(state.activity.icon, contentDescription = null, tint = accent)
    }
}

@Composable
private fun AlbumArt(accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.surface,
                        accent.copy(alpha = 0.30f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxSize(0.42f),
        )
    }
}

@Composable
private fun TrackInfo(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SourceBanner(label: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            "Controlling $label",
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}

@Composable
private fun Scrubber(progress: Float, accent: Color, onSeek: (Float) -> Unit) {
    Slider(
        value = progress,
        onValueChange = onSeek,
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeRow(positionMs: Long, durationMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(formatTime(positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTime(durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    accent: Color,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.size(20.dp))
        FilledIconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(84.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.Black),
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitySelector(
    state: PlayerUiState,
    accent: Color,
    onSelectActivity: (ActivityState) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val entries = ActivityState.entries
            entries.forEachIndexed { index, activity ->
                SegmentedButton(
                    selected = state.activity == activity,
                    onClick = { onSelectActivity(activity) },
                    shape = SegmentedButtonDefaults.itemShape(index, entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = accent.copy(alpha = 0.20f),
                        activeContentColor = accent,
                    ),
                    icon = { Icon(activity.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(activity.label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FilterChip(
            selected = state.autoSync,
            onClick = { onToggleAutoSync(!state.autoSync) },
            label = { Text(if (state.autoSync) "Auto-sync on" else "Auto-sync off", fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = accent.copy(alpha = 0.20f),
                selectedLabelColor = accent,
                selectedLeadingIconColor = accent,
            ),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
