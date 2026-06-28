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
 * Drives Now Playing with a real two-deck crossfade. One [MediaPlayer] per
 * distinct bundled track plays simultaneously and looping; their volumes
 * crossfade so the deck for the current activity is heard while the others fade
 * to silence. Switching activity is therefore seamless (no reload), and decks
 * that back the same file just relabel.
 *
 * The spectrum is driven by a real [Visualizer] on the global output mix when
 * RECORD_AUDIO is granted, falling back to a synthesized envelope otherwise.
 * Detected activity from the gesture service drives the player while auto-sync
 * is on.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val gesture = GestureServiceClient.get(app)

    /** One deck per distinct raw track, keyed by raw resource name. */
    private val decks = LinkedHashMap<String, MediaPlayer>()
    private val deckVolume = HashMap<String, Float>()
    private val fileDuration = HashMap<String, Long>()

    private var visualizer: Visualizer? = null
    @Volatile private var audioGranted = false

    private val levels = FloatArray(VISUALIZER_BARS)
    private var phase = 0f

    init {
        viewModelScope.launch { runLoop() }
        viewModelScope.launch {
            gesture.detected.collect { activity ->
                if (activity != null && _state.value.autoSync) selectActivity(activity, keepAuto = true)
            }
        }
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
        if (decks.isEmpty()) setupDecks()
        if (_state.value.isPlaying) pauseAll() else startAll()
    }

    fun next() {
        val order = ActivityState.entries
        val nextActivity = order[(_state.value.activity.ordinal + 1) % order.size]
        selectActivity(nextActivity, keepAuto = true)
    }

    fun previous() {
        runCatching { activeDeck()?.seekTo(0) }
        _state.update { it.copy(positionMs = 0L) }
    }

    fun seekTo(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * _state.value.durationMs).toLong()
        runCatching { activeDeck()?.seekTo(target.toInt()) }
        _state.update { it.copy(positionMs = target) }
    }

    /** Switch activity. Decks keep running; the crossfade handles the audio. */
    fun selectActivity(activity: ActivityState, keepAuto: Boolean = false) {
        val previousDeck = activeDeck()
        val track = trackFor(activity)
        val duration = fileDuration[track.rawResName] ?: track.durationMs
        _state.update {
            it.copy(
                activity = activity,
                autoSync = if (keepAuto) it.autoSync else false,
                track = track.copy(durationMs = duration),
            )
        }
        // Carry the playhead across a different deck so the crossfade lines up.
        val newDeck = decks[track.rawResName]
        if (newDeck != null && previousDeck != null && newDeck !== previousDeck) {
            runCatching { newDeck.seekTo(previousDeck.currentPosition) }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        _state.update { it.copy(autoSync = enabled) }
        if (enabled) gesture.startClassification() else gesture.stopClassification()
    }

    // ── Deck lifecycle ────────────────────────────────────────────────────────

    private fun setupDecks() {
        val files = ActivityState.entries.map { trackFor(it).rawResName }.distinct()
        for (name in files) {
            val resId = rawResId(name)
            if (resId == 0) continue
            val mp = MediaPlayer.create(getApplication<Application>(), resId) ?: continue
            mp.isLooping = true
            runCatching { mp.setVolume(0f, 0f) }
            decks[name] = mp
            deckVolume[name] = 0f
            fileDuration[name] = mp.duration.toLong().takeIf { it > 0 } ?: trackFor(_state.value.activity).durationMs
        }
        // Adopt the real duration for the current track.
        val current = _state.value.track
        fileDuration[current.rawResName]?.let { dur ->
            _state.update { it.copy(track = current.copy(durationMs = dur)) }
        }
    }

    private fun activeDeck(): MediaPlayer? = decks[trackFor(_state.value.activity).rawResName]

    private fun startAll() {
        decks.values.forEach { runCatching { it.start() } }
        _state.update { it.copy(isPlaying = true) }
        attachVisualizer()
    }

    private fun pauseAll() {
        decks.values.forEach { runCatching { it.pause() } }
        _state.update { it.copy(isPlaying = false) }
    }

    private fun releaseAll() {
        releaseVisualizer()
        decks.values.forEach { runCatching { it.release() } }
        decks.clear()
        deckVolume.clear()
    }

    private fun rawResId(name: String): Int {
        val context = getApplication<Application>()
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    override fun onCleared() {
        releaseAll()
        gesture.stopClassification()
        super.onCleared()
    }

    // ── Visualizer ────────────────────────────────────────────────────────────

    private fun attachVisualizer() {
        if (!audioGranted || visualizer != null) return
        runCatching {
            // Session 0 = global output mix, so the spectrum reflects the
            // crossfaded result of all decks combined.
            val v = Visualizer(0)
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

    // ── Frame loop: crossfade volumes, track position, synth fallback ─────────

    private suspend fun runLoop() {
        val frameMs = 33L
        while (currentCoroutineContext().isActive) {
            delay(frameMs)
            val s = _state.value
            crossfadeVolumes(activeFile = trackFor(s.activity).rawResName)

            if (s.isPlaying && decks.isNotEmpty()) {
                val active = activeDeck()
                val pos = active?.let { runCatching { it.currentPosition.toLong() }.getOrDefault(s.positionMs) }
                    ?: s.positionMs
                if (visualizer == null) {
                    advanceLevels(s.activity)
                    _state.update { it.copy(positionMs = pos, levels = levels.toList()) }
                } else {
                    _state.update { it.copy(positionMs = pos) }
                }
            } else {
                decayLevels()
                _state.update { it.copy(levels = levels.toList()) }
            }
        }
    }

    private fun crossfadeVolumes(activeFile: String) {
        if (decks.isEmpty()) return
        for ((name, mp) in decks) {
            val target = if (name == activeFile) 1f else 0f
            val current = deckVolume[name] ?: 0f
            val next = current + (target - current) * CROSSFADE_RATE
            deckVolume[name] = next
            runCatching { mp.setVolume(next, next) }
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

    private companion object {
        /** Per-frame volume approach factor (~0.8s crossfade at 33ms frames). */
        const val CROSSFADE_RATE = 0.08f
    }
}
