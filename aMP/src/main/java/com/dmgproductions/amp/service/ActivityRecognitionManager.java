package com.dmgproductions.amp.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.lifecycle.MutableLiveData;

public class ActivityRecognitionManager implements SensorEventListener {

    public enum UserActivity {
        IDLE, WALKING, RUNNING, UNKNOWN
    }

    public interface ActivityChangeListener {
        void onActivityChanged(UserActivity newActivity, float confidence);
    }

    private static final float WALKING_THRESHOLD = 2.0f;
    private static final float RUNNING_THRESHOLD = 6.0f;
    private static final int CONFIRMATION_COUNT = 3;
    private static final int STEP_WINDOW_SIZE = 50;

    private final SensorManager sensorManager;
    private final MutableLiveData<UserActivity> currentActivity = new MutableLiveData<>(UserActivity.IDLE);
    private ActivityChangeListener listener;

    private UserActivity pendingActivity = UserActivity.IDLE;
    private int confirmationCounter = 0;
    private float[] accelerometerHistory = new float[STEP_WINDOW_SIZE];
    private int historyIndex = 0;
    private int stepCount = 0;
    private long lastStepTime = 0;

    public ActivityRecognitionManager(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void start() {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setActivityChangeListener(ActivityChangeListener listener) {
        this.listener = listener;
    }

    public MutableLiveData<UserActivity> getCurrentActivity() {
        return currentActivity;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float magnitude = calculateMagnitude(event.values);
        accelerometerHistory[historyIndex % STEP_WINDOW_SIZE] = magnitude;
        historyIndex++;

        UserActivity detected = classifyActivity(magnitude);

        if (detected == pendingActivity) {
            confirmationCounter++;
        } else {
            pendingActivity = detected;
            confirmationCounter = 1;
        }

        if (confirmationCounter >= CONFIRMATION_COUNT) {
            UserActivity previous = currentActivity.getValue();
            if (previous != detected) {
                currentActivity.postValue(detected);
                if (listener != null) {
                    listener.onActivityChanged(detected, calculateConfidence());
                }
            }
        }

        detectStep(magnitude);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

    static float calculateMagnitude(float[] values) {
        return (float) Math.sqrt(
                values[0] * values[0] +
                values[1] * values[1] +
                values[2] * values[2]
        ) - SensorManager.GRAVITY_EARTH;
    }

    static UserActivity classifyActivity(float magnitude) {
        float absMagnitude = Math.abs(magnitude);
        if (absMagnitude >= RUNNING_THRESHOLD) {
            return UserActivity.RUNNING;
        } else if (absMagnitude >= WALKING_THRESHOLD) {
            return UserActivity.WALKING;
        } else {
            return UserActivity.IDLE;
        }
    }

    private float calculateConfidence() {
        if (historyIndex < STEP_WINDOW_SIZE) return 0.5f;

        int matchCount = 0;
        UserActivity current = pendingActivity;
        for (int i = 0; i < STEP_WINDOW_SIZE; i++) {
            if (classifyActivity(accelerometerHistory[i]) == current) {
                matchCount++;
            }
        }
        return (float) matchCount / STEP_WINDOW_SIZE;
    }

    private void detectStep(float magnitude) {
        long now = System.currentTimeMillis();
        if (Math.abs(magnitude) > WALKING_THRESHOLD && (now - lastStepTime) > 200) {
            stepCount++;
            lastStepTime = now;
        }
    }

    public int getStepCount() {
        return stepCount;
    }

    public float getEstimatedBPM() {
        if (historyIndex < STEP_WINDOW_SIZE) return 0;
        // Count threshold crossings in the window to estimate cadence
        int crossings = 0;
        boolean wasAbove = false;
        for (int i = 0; i < STEP_WINDOW_SIZE; i++) {
            boolean isAbove = Math.abs(accelerometerHistory[i]) > WALKING_THRESHOLD;
            if (isAbove && !wasAbove) {
                crossings++;
            }
            wasAbove = isAbove;
        }
        // Approximate BPM based on sensor rate (~50Hz game delay) and crossings
        float windowDurationSeconds = STEP_WINDOW_SIZE / 50.0f;
        return (crossings / windowDurationSeconds) * 60.0f;
    }
}
