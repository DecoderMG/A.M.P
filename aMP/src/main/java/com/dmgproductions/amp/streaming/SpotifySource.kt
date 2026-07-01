package com.dmgproductions.amp.streaming

import android.content.Context
import com.dmgproductions.amp.player.ActivityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Spotify integration **scaffold**.
 *
 * Spotify's App Remote SDK ships as a manual `.aar` (no Maven artifact), so it
 * cannot be compiled in this repo's CI. This class satisfies the
 * [StreamingSource] contract and stores the per-activity playlist URIs; the
 * actual App Remote calls are marked `TODO(spotify)` and require the SDK plus a
 * developer client id / redirect URI.
 *
 * See `docs/streaming/SPOTIFY.md` for the full activation steps.
 */
class SpotifySource(
    context: Context,
    private val prefs: StreamingPrefs,
) : StreamingSource {

    @Suppress("unused")
    private val appContext = context.applicationContext
    override val source = PlaybackSource.SPOTIFY

    private val _nowPlaying = MutableStateFlow(ExternalNowPlaying())
    override val nowPlaying: StateFlow<ExternalNowPlaying> = _nowPlaying.asStateFlow()

    override fun start() {
        // TODO(spotify): SpotifyAppRemote.connect(appContext, ConnectionParams(CLIENT_ID, REDIRECT_URI)) { remote -> ... }
        // On connect, subscribe to remote.playerApi.subscribeToPlayerState() and mirror it into _nowPlaying.
        _nowPlaying.value = ExternalNowPlaying(available = false)
    }

    override fun stop() {
        // TODO(spotify): SpotifyAppRemote.disconnect(appRemote)
        _nowPlaying.value = ExternalNowPlaying()
    }

    override fun onActivity(activity: ActivityState) {
        val uri = prefs.playlist(PlaybackSource.SPOTIFY, activity)
        if (uri.isBlank()) return
        // TODO(spotify): appRemote.playerApi.play(uri)  // e.g. "spotify:playlist:37i9dQ..."
    }

    override fun playPause() {
        // TODO(spotify): toggle appRemote.playerApi.pause() / resume()
    }

    override fun next() {
        // TODO(spotify): appRemote.playerApi.skipNext()
    }

    override fun previous() {
        // TODO(spotify): appRemote.playerApi.skipPrevious()
    }
}
