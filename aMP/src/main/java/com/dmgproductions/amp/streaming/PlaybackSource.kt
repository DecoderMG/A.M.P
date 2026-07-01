package com.dmgproductions.amp.streaming

import com.dmgproductions.amp.player.ActivityState
import kotlinx.coroutines.flow.StateFlow

/** Where A.M.P's audio comes from. LOCAL is the built-in crossfade player. */
enum class PlaybackSource(val label: String) {
    LOCAL("On-device"),
    YOUTUBE_MUSIC("YouTube Music"),
    SPOTIFY("Spotify"),
}

/** Snapshot of what an external streaming app is currently playing. */
data class ExternalNowPlaying(
    val available: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
)

/**
 * A music source A.M.P can drive by activity. Implementations wrap an external
 * app (YouTube Music, Spotify) — A.M.P controls/launches it, it never streams
 * the catalog itself.
 */
interface StreamingSource {
    val source: PlaybackSource
    val nowPlaying: StateFlow<ExternalNowPlaying>

    /** Becomes the active source. */
    fun start()

    /** No longer the active source. */
    fun stop()

    /** Switch content to match the given activity (e.g. open its playlist). */
    fun onActivity(activity: ActivityState)

    fun playPause()
    fun next()
    fun previous()
}
