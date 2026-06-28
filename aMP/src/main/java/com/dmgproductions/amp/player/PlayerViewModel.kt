package com.dmgproductions.amp.player

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmgproductions.amp.gestures.GestureServiceClient
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Drives Now Playing with real audio. A [MediaPlayer] plays the bundled track
 * for the current activity (switching files as the activity changes); the
 * scrubber follows real playback position. When RECORD_AUDIO is granted a real
 * [Visualizer] FFT tap feeds the spectrum, otherwise it falls back to a
 * synthesized envelope. Detected activity from the gesture service drives the
 * player while auto-sync is on.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val gesture = GestureServiceClient.get(app)

    private var player: MediaPlayer? = null
    private var loadedRawResName: String? = null

    private var visualizer: Visualizer? = null
    @Volatile private var audioGranted = false

    private val levels = FloatArray(VISUALIZER_BARS)
    private var phase = 0f

    init {
        viewModelScope.launch { runLoop() }
        // Apply motion-detected activity while auto-sync is enabled.
        viewModelScope.launch {
            gesture.detected.collect { activity ->
                if (activity != null && _state.value.autoSync) selectActivity(activity, keepAuto = true)
            }
        }
        // React to the audio permission becoming available.
        viewModelScope.launch {
            gesture.audioGranted.collect { granted ->
                audioGranted = granted
                if (granted) {
                    if (_state.value.autoSync) gesture.startClassification()
                    if (_state.value.isPlaying) attachVisualizer()
                }
            }
        }
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

    fun setAutoSync(enabled: Boolean) {
        _state.update { it.copy(autoSync = enabled) }
        if (enabled) gesture.startClassification() else gesture.stopClassification()
    }

    // ── MediaPlayer + Visualizer lifecycle ────────────────────────────────────

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
        attachVisualizer()
    }

    private fun pausePlayer() {
        runCatching { player?.pause() }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun releasePlayer() {
        releaseVisualizer()
        runCatching { player?.release() }
        player = null
        loadedRawResName = null
    }

    private fun attachVisualizer() {
        val mp = player ?: return
        if (!audioGranted || visualizer != null) return
        runCatching {
            val v = Visualizer(mp.audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vz: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(vz: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) onFftData(fft)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                /* waveform = */ false,
                /* fft = */ true,
            )
            v.enabled = true
            visualizer = v
        }
    }

    private fun releaseVisualizer() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
    }

    private fun rawResId(name: String): Int {
        val context = getApplication<Application>()
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    override fun onCleared() {
        releasePlayer()
        gesture.stopClassification()
        super.onCleared()
    }

    // ── Spectrum ──────────────────────────────────────────────────────────────

    /** Real FFT path: bucket the spectrum into bars (log-spaced toward lows). */
    private fun onFftData(fft: ByteArray) {
        val bins = fft.size / 2
        if (bins <= 1) return
        for (bar in levels.indices) {
            val frac = (bar + 1).toFloat() / levels.size
            val idx = (frac * frac * (bins - 1)).toInt().coerceIn(1, bins - 1)
            val re = fft[2 * idx].toFloat()
            val im = fft[2 * idx + 1].toFloat()
            val norm = (hypot(re, im) / 80f).coerceIn(0f, 1f)
            levels[bar] += (norm - levels[bar]) * 0.5f
        }
        _state.update { it.copy(levels = levels.toList()) }
    }

    private suspend fun runLoop() {
        val frameMs = 33L
        while (currentCoroutineContext().isActive) {
            delay(frameMs)
            val s = _state.value
            val mp = player
            if (s.isPlaying && mp != null) {
                val pos = runCatching { mp.currentPosition.toLong() }.getOrDefault(s.positionMs)
                if (visualizer == null) {
                    // Synthesized fallback spectrum.
                    advanceLevels(s.activity)
                    _state.update { it.copy(positionMs = pos, levels = levels.toList()) }
                } else {
                    // Real FFT owns the levels; just track position here.
                    _state.update { it.copy(positionMs = pos) }
                }
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
