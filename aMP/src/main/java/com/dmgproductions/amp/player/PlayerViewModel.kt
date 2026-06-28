package com.dmgproductions.amp.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drives the Now Playing UI. For now it simulates playback and the audio
 * spectrum so the whole interface is live and interactive; Stage 6 swaps the
 * internals for a real MediaPlayer crossfade without changing this API.
 */
class PlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val levels = FloatArray(VISUALIZER_BARS)
    private var phase = 0f

    init {
        viewModelScope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        val frameMs = 33L
        while (currentCoroutineContext().isActive) {
            delay(frameMs)
            val s = _state.value
            if (!s.isPlaying) {
                // Settle the visualizer toward silence when paused.
                decayLevels()
                _state.update { it.copy(levels = levels.toList()) }
                continue
            }
            // Advance playback position; wrap to the next track at the end.
            val nextPos = s.positionMs + frameMs
            if (nextPos >= s.durationMs) {
                next()
                continue
            }
            advanceLevels(s.activity)
            _state.update { it.copy(positionMs = nextPos, levels = levels.toList()) }
        }
    }

    private fun advanceLevels(activity: ActivityState) {
        // Energy scales with the activity's target tempo.
        val energy = activity.targetBpm / 165f
        phase += 0.10f + energy * 0.18f
        for (i in levels.indices) {
            val base = (sin(phase + i * 0.45f) * 0.5f + 0.5f)
            val wobble = Random.nextFloat() * 0.45f
            val target = (base * 0.6f + wobble) * (0.35f + energy * 0.65f)
            levels[i] += (target - levels[i]) * 0.35f
        }
    }

    private fun decayLevels() {
        for (i in levels.indices) levels[i] *= 0.82f
    }

    fun togglePlay() = _state.update { it.copy(isPlaying = !it.isPlaying) }

    fun next() {
        val order = ActivityState.entries
        val current = _state.value.activity
        val nextActivity = order[(current.ordinal + 1) % order.size]
        selectActivity(nextActivity, keepAuto = true)
        _state.update { it.copy(positionMs = 0L) }
    }

    fun previous() = _state.update { it.copy(positionMs = 0L) }

    fun seekTo(fraction: Float) = _state.update {
        it.copy(positionMs = (fraction.coerceIn(0f, 1f) * it.durationMs).toLong())
    }

    /** Manually choose the activity (turns off auto-detect unless [keepAuto]). */
    fun selectActivity(activity: ActivityState, keepAuto: Boolean = false) = _state.update {
        it.copy(
            activity = activity,
            track = trackFor(activity),
            autoSync = if (keepAuto) it.autoSync else false,
            positionMs = if (activity == it.activity) it.positionMs else 0L,
        )
    }

    fun setAutoSync(enabled: Boolean) = _state.update { it.copy(autoSync = enabled) }
}
