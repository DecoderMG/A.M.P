package com.dmgproductions.amp.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dmgproductions.amp.ui.theme.ActivityIdle
import com.dmgproductions.amp.ui.theme.NeonCyan
import com.dmgproductions.amp.ui.theme.NeonOrange

/**
 * The motion state A.M.P detects. It's the core of the app: the player swaps and
 * crossfades between tracks depending on what you're physically doing.
 */
enum class ActivityState(
    val label: String,
    val tagline: String,
    val icon: ImageVector,
    val accent: Color,
    /** Beats-per-minute target the player aims music toward for this state. */
    val targetBpm: Int,
) {
    IDLE("Idle", "Resting · ambient mix", Icons.Rounded.SelfImprovement, ActivityIdle, 70),
    WALKING("Walking", "Steady pace · groove", Icons.Rounded.DirectionsWalk, NeonCyan, 120),
    RUNNING("Running", "High energy · drive", Icons.Rounded.DirectionsRun, NeonOrange, 165),
}

/** A playable track. [rawResName] maps to a bundled res/raw file for real playback. */
data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val rawResName: String,
    val durationMs: Long,
)

/** Immutable snapshot of the player, rendered by the Now Playing screen. */
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val activity: ActivityState = ActivityState.IDLE,
    /** When true, [activity] is driven by motion detection; otherwise set by hand. */
    val autoSync: Boolean = true,
    val positionMs: Long = 0L,
    val track: Track = AmbientTrack,
    /** Normalized (0..1) spectrum levels feeding the visualizer. */
    val levels: List<Float> = List(VISUALIZER_BARS) { 0f },
) {
    val durationMs: Long get() = track.durationMs
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

const val VISUALIZER_BARS = 56

// Demo library — three moods mapped to the bundled raw tracks. Artist/album are
// illustrative; the rawResName points at real audio for Stage 6 playback.
val AmbientTrack = Track(
    title = "Low Tide", artist = "Hours", album = "Stillwater",
    rawResName = "faint", durationMs = 198_000L,
)
val WalkingTrack = Track(
    title = "Sidewalk Pulse", artist = "Northbound", album = "City Cadence",
    rawResName = "test", durationMs = 213_000L,
)
val RunningTrack = Track(
    title = "Redline", artist = "VELOCITY", album = "Tempo Run",
    rawResName = "test", durationMs = 184_000L,
)

fun trackFor(activity: ActivityState): Track = when (activity) {
    ActivityState.IDLE -> AmbientTrack
    ActivityState.WALKING -> WalkingTrack
    ActivityState.RUNNING -> RunningTrack
}
