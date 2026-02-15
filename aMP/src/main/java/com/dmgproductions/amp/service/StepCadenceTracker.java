package com.dmgproductions.amp.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.lifecycle.MutableLiveData;

/**
 * Tracks step cadence using the hardware step detector sensor (TYPE_STEP_DETECTOR)
 * and calculates real-time BPM (beats per minute) from walking/running cadence.
 *
 * Uses a rolling window of step timestamps to provide smooth, accurate BPM
 * that can be used for music tempo matching.
 */
public class StepCadenceTracker implements SensorEventListener {

    private static final int MAX_STEP_HISTORY = 20;
    private static final long STEP_TIMEOUT_MS = 3000;
    private static final float MIN_BPM = 40f;
    private static final float MAX_BPM = 220f;

    private final SensorManager sensorManager;
    private final long[] stepTimestamps = new long[MAX_STEP_HISTORY];
    private int stepIndex = 0;
    private int totalSteps = 0;
    private boolean hasStepDetector = false;

    private final MutableLiveData<Float> currentBPM = new MutableLiveData<>(0f);
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);

    public StepCadenceTracker(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void start() {
        Sensor stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);
            hasStepDetector = true;
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_DETECTOR) return;

        long now = System.currentTimeMillis();
        stepTimestamps[stepIndex % MAX_STEP_HISTORY] = now;
        stepIndex++;
        totalSteps++;
        stepCount.postValue(totalSteps);

        float bpm = calculateBPM(now);
        if (bpm >= MIN_BPM && bpm <= MAX_BPM) {
            currentBPM.postValue(bpm);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

    /**
     * Calculate BPM from the rolling window of step timestamps.
     * Uses the average interval between steps to produce a smooth BPM reading.
     */
    float calculateBPM(long currentTime) {
        int availableSteps = Math.min(stepIndex, MAX_STEP_HISTORY);
        if (availableSteps < 2) return 0;

        // Find the oldest valid step in our window
        int oldest = -1;
        long oldestTime = Long.MAX_VALUE;
        for (int i = 0; i < availableSteps; i++) {
            long ts = stepTimestamps[i];
            if (ts > 0 && (currentTime - ts) <= STEP_TIMEOUT_MS && ts < oldestTime) {
                oldestTime = ts;
                oldest = i;
            }
        }
        if (oldest == -1) return 0;

        // Count steps within the timeout window
        int validSteps = 0;
        long earliest = Long.MAX_VALUE;
        long latest = Long.MIN_VALUE;
        for (int i = 0; i < availableSteps; i++) {
            long ts = stepTimestamps[i];
            if (ts > 0 && (currentTime - ts) <= STEP_TIMEOUT_MS) {
                validSteps++;
                if (ts < earliest) earliest = ts;
                if (ts > latest) latest = ts;
            }
        }

        if (validSteps < 2 || latest <= earliest) return 0;

        float intervalMs = (float) (latest - earliest) / (validSteps - 1);
        if (intervalMs <= 0) return 0;

        return 60000f / intervalMs;
    }

    /**
     * Estimate BPM from raw accelerometer data when step detector is unavailable.
     * Counts zero-crossings (rising edges above threshold) to approximate step rate.
     */
    static float estimateBPMFromAccelerometer(float[] magnitudeHistory, int historySize,
                                               float threshold, float sensorRateHz) {
        if (historySize < 10) return 0;

        int crossings = 0;
        boolean wasAbove = false;
        for (int i = 0; i < historySize; i++) {
            boolean isAbove = Math.abs(magnitudeHistory[i]) > threshold;
            if (isAbove && !wasAbove) {
                crossings++;
            }
            wasAbove = isAbove;
        }

        float windowDurationSeconds = historySize / sensorRateHz;
        if (windowDurationSeconds <= 0) return 0;

        float bpm = (crossings / windowDurationSeconds) * 60f;
        return (bpm >= MIN_BPM && bpm <= MAX_BPM) ? bpm : 0;
    }

    public boolean hasStepDetector() {
        return hasStepDetector;
    }

    public MutableLiveData<Float> getCurrentBPM() {
        return currentBPM;
    }

    public MutableLiveData<Integer> getStepCount() {
        return stepCount;
    }

    public int getTotalSteps() {
        return totalSteps;
    }
}
