package com.dmgproductions.amp.service;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

/**
 * Unified activity detection bridge that combines:
 * 1. Google Play Services Activity Recognition (high-level, system-optimized)
 * 2. Accelerometer-based activity classification (local, real-time)
 * 3. Step cadence tracker (precise BPM from hardware step detector)
 *
 * Produces a single, fused activity signal and cadence BPM for music tempo matching.
 * Google API results are preferred when available (higher accuracy, lower power),
 * with accelerometer fallback for devices without Play Services.
 */
public class ActivityBridge implements
        GoogleActivityRecognitionSource.GoogleActivityListener,
        ActivityRecognitionManager.ActivityChangeListener {

    private static final String TAG = "ActivityBridge";

    /** Weight given to Google API vs accelerometer when both are available */
    private static final float GOOGLE_WEIGHT = 0.7f;
    private static final float ACCEL_WEIGHT = 0.3f;

    public interface ActivityBridgeListener {
        void onUnifiedActivityChanged(ActivityRecognitionManager.UserActivity activity,
                                       float cadenceBPM, float confidence);
    }

    private final GoogleActivityRecognitionSource googleSource;
    private final ActivityRecognitionManager accelSource;
    private final StepCadenceTracker cadenceTracker;

    private ActivityBridgeListener listener;

    private ActivityRecognitionManager.UserActivity googleActivity =
            ActivityRecognitionManager.UserActivity.IDLE;
    private ActivityRecognitionManager.UserActivity accelActivity =
            ActivityRecognitionManager.UserActivity.IDLE;
    private float googleConfidence = 0f;
    private float accelConfidence = 0f;

    // Fused output
    private final MutableLiveData<ActivityRecognitionManager.UserActivity> fusedActivity =
            new MutableLiveData<>(ActivityRecognitionManager.UserActivity.IDLE);
    private final MutableLiveData<Float> fusedBPM = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> fusedConfidence = new MutableLiveData<>(0f);
    private final MutableLiveData<TempoMatcher.TempoRange> currentTempoRange =
            new MutableLiveData<>(TempoMatcher.getTempoRangeForActivity(
                    ActivityRecognitionManager.UserActivity.IDLE));

    public ActivityBridge(Context context) {
        googleSource = new GoogleActivityRecognitionSource(context);
        accelSource = new ActivityRecognitionManager(context);
        cadenceTracker = new StepCadenceTracker(context);
    }

    /** Constructor for testing with injectable sources */
    ActivityBridge(GoogleActivityRecognitionSource googleSource,
                   ActivityRecognitionManager accelSource,
                   StepCadenceTracker cadenceTracker) {
        this.googleSource = googleSource;
        this.accelSource = accelSource;
        this.cadenceTracker = cadenceTracker;
    }

    public void start() {
        googleSource.setListener(this);
        accelSource.setActivityChangeListener(this);

        googleSource.start();
        accelSource.start();
        cadenceTracker.start();

        Log.d(TAG, "ActivityBridge started - Google API + Accelerometer + Step Cadence");
    }

    public void stop() {
        googleSource.stop();
        accelSource.stop();
        cadenceTracker.stop();

        Log.d(TAG, "ActivityBridge stopped");
    }

    public void setListener(ActivityBridgeListener listener) {
        this.listener = listener;
    }

    // --- Google API callback ---
    @Override
    public void onActivityDetected(ActivityRecognitionManager.UserActivity activity, int confidence) {
        googleActivity = activity;
        googleConfidence = confidence / 100f;
        fuseAndNotify();
    }

    // --- Accelerometer callback ---
    @Override
    public void onActivityChanged(ActivityRecognitionManager.UserActivity newActivity, float confidence) {
        accelActivity = newActivity;
        accelConfidence = confidence;
        fuseAndNotify();
    }

    /**
     * Fuses signals from all sources to produce a single activity + BPM output.
     *
     * Priority logic:
     * - If Google confidence is high (>70%), trust it primarily
     * - If both agree, boost confidence
     * - If they disagree, weighted blend favoring Google
     * - BPM from step detector is always preferred; accelerometer BPM is fallback
     */
    void fuseAndNotify() {
        ActivityRecognitionManager.UserActivity resolvedActivity;
        float resolvedConfidence;

        if (googleConfidence > 0.7f) {
            // High-confidence Google detection — trust it
            resolvedActivity = googleActivity;
            resolvedConfidence = googleConfidence;
        } else if (googleActivity == accelActivity) {
            // Both sources agree — boost confidence
            resolvedActivity = googleActivity;
            resolvedConfidence = Math.min(1.0f, googleConfidence * GOOGLE_WEIGHT
                    + accelConfidence * ACCEL_WEIGHT + 0.2f);
        } else if (googleConfidence > 0) {
            // Disagree but Google has some data — weighted blend
            float googleScore = activityToScore(googleActivity) * GOOGLE_WEIGHT * googleConfidence;
            float accelScore = activityToScore(accelActivity) * ACCEL_WEIGHT * accelConfidence;
            resolvedActivity = scoreToActivity(googleScore + accelScore);
            resolvedConfidence = googleConfidence * GOOGLE_WEIGHT + accelConfidence * ACCEL_WEIGHT;
        } else {
            // No Google data — use accelerometer only
            resolvedActivity = accelActivity;
            resolvedConfidence = accelConfidence;
        }

        // Get BPM from step detector if available, otherwise accelerometer estimate
        float bpm = 0f;
        Float stepBPM = cadenceTracker.getCurrentBPM().getValue();
        if (stepBPM != null && stepBPM > 0) {
            bpm = stepBPM;
        } else {
            bpm = accelSource.getEstimatedBPM();
        }

        // If we have BPM, use it to refine activity (BPM is ground truth for cadence)
        if (bpm > 0) {
            resolvedActivity = refineActivityFromBPM(resolvedActivity, bpm);
        }

        // Update LiveData
        fusedActivity.postValue(resolvedActivity);
        fusedBPM.postValue(bpm);
        fusedConfidence.postValue(resolvedConfidence);
        currentTempoRange.postValue(TempoMatcher.getTempoRangeForActivity(resolvedActivity));

        // Notify listener
        if (listener != null) {
            listener.onUnifiedActivityChanged(resolvedActivity, bpm, resolvedConfidence);
        }
    }

    /**
     * If we have reliable BPM data, use it to correct the activity classification.
     * Step cadence is a strong indicator of activity type.
     */
    static ActivityRecognitionManager.UserActivity refineActivityFromBPM(
            ActivityRecognitionManager.UserActivity detected, float bpm) {
        if (bpm < 60) {
            return ActivityRecognitionManager.UserActivity.IDLE;
        } else if (bpm >= 60 && bpm < 130) {
            // Could be walking — only override if we detected something very different
            if (detected == ActivityRecognitionManager.UserActivity.RUNNING) {
                return ActivityRecognitionManager.UserActivity.WALKING;
            }
            return detected == ActivityRecognitionManager.UserActivity.IDLE ?
                    ActivityRecognitionManager.UserActivity.WALKING : detected;
        } else if (bpm >= 130) {
            if (detected == ActivityRecognitionManager.UserActivity.IDLE) {
                return ActivityRecognitionManager.UserActivity.RUNNING;
            }
            return detected;
        }
        return detected;
    }

    /** Convert activity to a numeric score for weighted averaging */
    static float activityToScore(ActivityRecognitionManager.UserActivity activity) {
        switch (activity) {
            case IDLE: return 0f;
            case WALKING: return 1f;
            case RUNNING: return 2f;
            default: return 0.5f;
        }
    }

    /** Convert a numeric score back to the nearest activity */
    static ActivityRecognitionManager.UserActivity scoreToActivity(float score) {
        if (score < 0.5f) return ActivityRecognitionManager.UserActivity.IDLE;
        if (score < 1.5f) return ActivityRecognitionManager.UserActivity.WALKING;
        return ActivityRecognitionManager.UserActivity.RUNNING;
    }

    // --- LiveData accessors ---

    public MutableLiveData<ActivityRecognitionManager.UserActivity> getFusedActivity() {
        return fusedActivity;
    }

    public MutableLiveData<Float> getFusedBPM() {
        return fusedBPM;
    }

    public MutableLiveData<Float> getFusedConfidence() {
        return fusedConfidence;
    }

    public MutableLiveData<TempoMatcher.TempoRange> getCurrentTempoRange() {
        return currentTempoRange;
    }

    public StepCadenceTracker getCadenceTracker() {
        return cadenceTracker;
    }

    public GoogleActivityRecognitionSource getGoogleSource() {
        return googleSource;
    }

    public ActivityRecognitionManager getAccelSource() {
        return accelSource;
    }
}
