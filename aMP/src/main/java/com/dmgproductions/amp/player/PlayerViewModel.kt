package com.dmgproductions.amp.player

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drives Now Playing with real audio: a [MediaPlayer] plays the bundled track
 * for the current activity, switching files as the activity changes (activities
 * that share a track keep playing seamlessly and just relabel). The scrubber
 * follows the real playback position. The spectrum is a synthesized envelope
 * gated on actual playback — a real FFT tap is a possible follow-up.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var loadedRawResName: String? = null

    private val levels = FloatArray(VISUALIZER_BARS)
    private var phase = 0f

    init {
        viewModelScope.launch { runLoop() }
    }

    // ── Transport ────────────────────────────────────────────────────────────

    fun togglePlay() {
        val s = _state.value
        if (player == null) {
            loadTrack(s.track, play = true)
        } else if (s.isPlaying) {
            pausePlayer()
        } else {
            startPlayer()
        }
    }

    fun next() {
        val order = ActivityState.entries
        val nextActivity = order[(_state.value.activity.ordinal + 1) % order.size]
        selectActivity(nextActivity, keepAuto = true)
    }

    fun previous() {
        runCatching { player?.seekTo(0) }
        _state.update { it.copy(positionMs = 0L) }
    }

    fun seekTo(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * _state.value.durationMs).toLong()
        runCatching { player?.seekTo(target.toInt()) }
        _state.update { it.copy(positionMs = target) }
    }

    /** Manually choose the activity (turns off auto-detect unless [keepAuto]). */
    fun selectActivity(activity: ActivityState, keepAuto: Boolean = false) {
        val track = trackFor(activity)
        val sameFile = player != null && loadedRawResName == track.rawResName
        if (sameFile) {
            // Same audio file — keep playing, just update activity + metadata.
            _state.update {
                it.copy(
                    activity = activity,
                    autoSync = if (keepAuto) it.autoSync else false,
                    track = track.copy(durationMs = it.track.durationMs),
                )
            }
        } else {
            val wasPlaying = _state.value.isPlaying
            _state.update {
                it.copy(activity = activity, autoSync = if (keepAuto) it.autoSync else false)
            }
            loadTrack(track, play = wasPlaying)
        }
    }

    fun setAutoSync(enabled: Boolean) = _state.update { it.copy(autoSync = enabled) }

    // ── MediaPlayer lifecycle ─────────────────────────────────────────────────

    private fun loadTrack(track: Track, play: Boolean) {
        releasePlayer()
        val resId = rawResId(track.rawResName)
        val mp = if (resId != 0) MediaPlayer.create(getApplication<Application>(), resId) else null
        if (mp != null) {
            mp.setOnCompletionListener { next() }
            player = mp
            loadedRawResName = track.rawResName
        }
        val duration = player?.duration?.toLong()?.takeIf { it > 0 } ?: track.durationMs
        _state.update { it.copy(track = track.copy(durationMs = duration), positionMs = 0L) }
        if (play) startPlayer() else _state.update { it.copy(isPlaying = false) }
    }

    private fun startPlayer() {
        runCatching { player?.start() }
        _state.update { it.copy(isPlaying = player != null) }
    }

    private fun pausePlayer() {
        runCatching { player?.pause() }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun releasePlayer() {
        runCatching { player?.release() }
        player = null
        loadedRawResName = null
    }

    private fun rawResId(name: String): Int {
        val context = getApplication<Application>()
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }

    // ── Visualizer + position loop ────────────────────────────────────────────

    private suspend fun runLoop() {
        val frameMs = 33L
        while (currentCoroutineContext().isActive) {
            delay(frameMs)
            val s = _state.value
            val mp = player
            if (s.isPlaying && mp != null) {
                val pos = runCatching { mp.currentPosition.toLong() }.getOrDefault(s.positionMs)
                advanceLevels(s.activity)
                _state.update { it.copy(positionMs = pos, levels = levels.toList()) }
            } else {
                decayLevels()
                _state.update { it.copy(levels = levels.toList()) }
            }
        }
    }

    private fun advanceLevels(activity: ActivityState) {
        val energy = activity.targetBpm / 165f
        phase += 0.10f + energy * 0.18f
        for (i in levels.indices) {
            val base = sin(phase + i * 0.45f) * 0.5f + 0.5f
            val wobble = Random.nextFloat() * 0.45f
            val target = (base * 0.6f + wobble) * (0.35f + energy * 0.65f)
            levels[i] += (target - levels[i]) * 0.35f
        }
    }

    private fun decayLevels() {
        for (i in levels.indices) levels[i] *= 0.82f
    }
}
