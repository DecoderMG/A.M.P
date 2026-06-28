package com.dmgproductions.amp.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Training UI state for the "teach A.M.P your motion" flow. */
data class TrainingUiState(
    val walkingTrained: Boolean = false,
    val runningTrained: Boolean = false,
    /** The activity currently being recorded, or null when idle. */
    val recording: ActivityState? = null,
    val secondsLeft: Int = 0,
) {
    val isRecording: Boolean get() = recording != null
    fun isTrained(activity: ActivityState): Boolean = when (activity) {
        ActivityState.WALKING -> walkingTrained
        ActivityState.RUNNING -> runningTrained
        ActivityState.IDLE -> true
    }
}

/**
 * Backs the training screen. Each session records motion for [TRAIN_SECONDS]
 * before marking that activity learned. This mirrors the gesture-recognition
 * service's learn mode; Stage 6 can bind the real AIDL service here.
 */
class ActivitiesViewModel : ViewModel() {

    private val _state = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()

    private var countdown: Job? = null

    fun train(activity: ActivityState) {
        if (_state.value.isRecording) return
        countdown?.cancel()
        countdown = viewModelScope.launch {
            _state.update { it.copy(recording = activity, secondsLeft = TRAIN_SECONDS) }
            for (s in TRAIN_SECONDS downTo 1) {
                _state.update { it.copy(secondsLeft = s) }
                delay(1000)
            }
            _state.update {
                it.copy(
                    recording = null,
                    secondsLeft = 0,
                    walkingTrained = it.walkingTrained || activity == ActivityState.WALKING,
                    runningTrained = it.runningTrained || activity == ActivityState.RUNNING,
                )
            }
        }
    }

    fun cancel() {
        countdown?.cancel()
        _state.update { it.copy(recording = null, secondsLeft = 0) }
    }

    fun clearAll() {
        countdown?.cancel()
        _state.value = TrainingUiState()
    }

    companion object {
        const val TRAIN_SECONDS = 15
    }
}
