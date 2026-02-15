package com.dmgproductions.amp.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class StepCadenceTrackerTest {

    // --- estimateBPMFromAccelerometer (static, no sensor needed) ---

    @Test
    public void testEstimateBPMWithRegularSteps() {
        // Simulate 50Hz sensor, 50 samples = 1 second window
        // Create a pattern with clear crossings above threshold
        float[] history = new float[50];
        float threshold = 2.0f;

        // Create a regular walking pattern: ~2 crossings per second = 120 BPM
        for (int i = 0; i < 50; i++) {
            // Sine wave at 2Hz (2 steps per second)
            history[i] = (float) (3.0 * Math.sin(2 * Math.PI * 2.0 * i / 50.0));
        }

        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 50, threshold, 50f);
        assertTrue("BPM should be positive for walking pattern", bpm > 0);
        // ~120 BPM expected (2 crossings/sec * 60)
        assertTrue("BPM should be in walking range", bpm >= 40 && bpm <= 220);
    }

    @Test
    public void testEstimateBPMWithNoMotion() {
        float[] history = new float[50];
        // All values near zero — no steps
        for (int i = 0; i < 50; i++) {
            history[i] = 0.1f;
        }

        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 50, 2.0f, 50f);
        assertEquals("BPM should be 0 for no motion", 0f, bpm, 0.01f);
    }

    @Test
    public void testEstimateBPMTooFewSamples() {
        float[] history = new float[5];
        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 5, 2.0f, 50f);
        assertEquals("BPM should be 0 with too few samples", 0f, bpm, 0.01f);
    }

    @Test
    public void testEstimateBPMZeroSensorRate() {
        float[] history = new float[50];
        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 50, 2.0f, 0f);
        assertEquals("BPM should be 0 with zero sensor rate", 0f, bpm, 0.01f);
    }

    @Test
    public void testEstimateBPMOutOfRangeReturnsZero() {
        // Create extremely fast crossings that would exceed MAX_BPM
        float[] history = new float[50];
        float threshold = 0.01f; // Very low threshold
        for (int i = 0; i < 50; i++) {
            // Oscillate rapidly every sample
            history[i] = (i % 2 == 0) ? 1.0f : -1.0f;
        }

        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 50, threshold, 50f);
        // At 50Hz with every-other-sample crossing, that's 25 crossings/sec = 1500 BPM
        // Should be clamped to 0 since it exceeds MAX_BPM
        assertEquals("Extreme BPM should return 0", 0f, bpm, 0.01f);
    }

    @Test
    public void testEstimateBPMRunningCadence() {
        // Simulate 50Hz sensor, 100 samples = 2 seconds
        // Running at ~170 steps/min = ~2.83 crossings/sec
        float[] history = new float[100];
        float threshold = 2.0f;
        double freq = 170.0 / 60.0; // Hz

        for (int i = 0; i < 100; i++) {
            history[i] = (float) (5.0 * Math.sin(2 * Math.PI * freq * i / 50.0));
        }

        float bpm = StepCadenceTracker.estimateBPMFromAccelerometer(history, 100, threshold, 50f);
        assertTrue("BPM should be positive for running", bpm > 0);
        assertTrue("BPM should be in reasonable range", bpm >= 40 && bpm <= 220);
    }
}
