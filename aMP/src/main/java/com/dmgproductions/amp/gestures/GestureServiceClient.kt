package com.dmgproductions.amp.gestures

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.dmgproductions.amp.classifier.Distribution
import com.dmgproductions.amp.player.ActivityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide client for the AIDL-bound [GestureRecognitionService]. It owns a
 * single binding shared by the player (classification) and the training screen
 * (learn mode), arbitrating the service's single mode between them.
 *
 * Every service call is wrapped defensively: if the service can't bind, or the
 * RECORD_AUDIO permission is missing, calls become no-ops and the UI falls back
 * to manual control. The training set name is fixed to [SET].
 */
class GestureServiceClient private constructor(context: Context) {

    private val appContext = context.applicationContext

    private var service: IGestureRecognitionService? = null
    private var bound = false
    private var classificationWanted = false

    private val _detected = MutableStateFlow<ActivityState?>(null)
    /** Latest activity inferred from motion, or null when unknown. */
    val detected: StateFlow<ActivityState?> = _detected.asStateFlow()

    private val _audioGranted = MutableStateFlow(false)
    val audioGranted: StateFlow<Boolean> = _audioGranted.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    fun setAudioPermissionGranted(granted: Boolean) {
        _audioGranted.value = granted
    }

    private val listener = object : IGestureRecognitionListener.Stub() {
        override fun onGestureRecognized(distribution: Distribution?) {
            val name = distribution?.bestMatch ?: return
            _detected.value = activityFromName(name)
        }

        override fun onGestureLearned(gestureName: String?) {}
        override fun onTrainingSetDeleted(trainingSet: String?) {}
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IGestureRecognitionService.Stub.asInterface(binder)
            _serviceConnected.value = true
            runCatching { service?.registerListener(listener) }
            if (classificationWanted) runCatching { service?.startClassificationMode(SET) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _serviceConnected.value = false
        }
    }

    private fun ensureBound() {
        if (bound) return
        bound = runCatching {
            appContext.bindService(
                Intent(appContext, GestureRecognitionService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
    }

    // ── Classification (player auto-sync) ─────────────────────────────────────

    fun startClassification() {
        classificationWanted = true
        ensureBound()
        runCatching { service?.startClassificationMode(SET) }
    }

    fun stopClassification() {
        classificationWanted = false
        runCatching { service?.stopClassificationMode() }
    }

    // ── Learn mode (training screen) ──────────────────────────────────────────

    fun startLearn(activity: ActivityState) {
        ensureBound()
        // Learn and classification are mutually exclusive on the service.
        runCatching { service?.stopClassificationMode() }
        runCatching { service?.startLearnMode(SET, nameFor(activity)) }
    }

    fun stopLearn() {
        runCatching { service?.stopLearnMode() }
        if (classificationWanted) runCatching { service?.startClassificationMode(SET) }
    }

    fun deleteAll() {
        ensureBound()
        runCatching { service?.deleteTrainingSet(SET) }
        _detected.value = null
    }

    private fun nameFor(activity: ActivityState) = activity.name.lowercase()

    private fun activityFromName(name: String): ActivityState = when (name.lowercase()) {
        "walking", "walk" -> ActivityState.WALKING
        "running", "run" -> ActivityState.RUNNING
        else -> ActivityState.IDLE
    }

    companion object {
        private const val SET = "amp"

        @Volatile
        private var instance: GestureServiceClient? = null

        fun get(context: Context): GestureServiceClient =
            instance ?: synchronized(this) {
                instance ?: GestureServiceClient(context).also { instance = it }
            }
    }
}
