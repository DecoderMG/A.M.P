package com.dmgproductions.amp.streaming

import android.content.Context
import com.dmgproductions.amp.player.ActivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide coordinator for external streaming sources. Shared by the player
 * (which routes activity + transport here when a streaming source is active)
 * and the Sources settings screen. LOCAL is handled by the player directly.
 */
class StreamingController private constructor(context: Context) {

    private val appContext = context.applicationContext
    val prefs = StreamingPrefs(appContext)

    private val youtube = YouTubeMusicSource(appContext, prefs)
    private val spotify = SpotifySource(appContext, prefs)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var nowPlayingJob: Job? = null

    private val _source = MutableStateFlow(prefs.source)
    val source: StateFlow<PlaybackSource> = _source.asStateFlow()

    private val _nowPlaying = MutableStateFlow(ExternalNowPlaying())
    val nowPlaying: StateFlow<ExternalNowPlaying> = _nowPlaying.asStateFlow()

    private var active: StreamingSource? = null

    private fun sourceFor(source: PlaybackSource): StreamingSource? = when (source) {
        PlaybackSource.YOUTUBE_MUSIC -> youtube
        PlaybackSource.SPOTIFY -> spotify
        PlaybackSource.LOCAL -> null
    }

    /** Select the active source; immediately points it at [currentActivity]. */
    fun setSource(newSource: PlaybackSource, currentActivity: ActivityState) {
        if (newSource == _source.value && active === sourceFor(newSource)) return
        active?.stop()
        nowPlayingJob?.cancel()

        prefs.source = newSource
        _source.value = newSource
        active = sourceFor(newSource)

        val current = active
        if (current == null) {
            _nowPlaying.value = ExternalNowPlaying()
        } else {
            current.start()
            current.onActivity(currentActivity)
            nowPlayingJob = scope.launch {
                current.nowPlaying.collect { _nowPlaying.value = it }
            }
        }
    }

    fun onActivity(activity: ActivityState) = active?.onActivity(activity) ?: Unit
    fun playPause() = active?.playPause() ?: Unit
    fun next() = active?.next() ?: Unit
    fun previous() = active?.previous() ?: Unit

    fun playlist(source: PlaybackSource, activity: ActivityState): String =
        prefs.playlist(source, activity)

    fun setPlaylist(source: PlaybackSource, activity: ActivityState, value: String) =
        prefs.setPlaylist(source, activity, value)

    companion object {
        @Volatile
        private var instance: StreamingController? = null

        fun get(context: Context): StreamingController =
            instance ?: synchronized(this) {
                instance ?: StreamingController(context).also { instance = it }
            }
    }
}
