package com.dmgproductions.amp.service;

import com.dmgproductions.amp.service.ActivityRecognitionManager.UserActivity;

import org.junit.Test;

import static org.junit.Assert.*;

public class ActivityRecognitionManagerTest {

    @Test
    public void testCalculateMagnitudeAtRest() {
        // At rest, accelerometer reads approximately (0, 0, 9.81) = gravity
        float[] restValues = {0f, 0f, 9.81f};
        float magnitude = ActivityRecognitionManager.calculateMagnitude(restValues);
        // Should be close to 0 after subtracting gravity
        assertEquals(0f, magnitude, 0.1f);
    }

    @Test
    public void testCalculateMagnitudeWithMotion() {
        // Walking produces acceleration above gravity
        float[] walkingValues = {2.0f, 0f, 12.0f};
        float magnitude = ActivityRecognitionManager.calculateMagnitude(walkingValues);
        assertTrue("Magnitude during motion should be positive", magnitude > 0);
    }

    @Test
    public void testClassifyIdleActivity() {
        // Low magnitude = idle
        UserActivity activity = ActivityRecognitionManager.classifyActivity(0.5f);
        assertEquals(UserActivity.IDLE, activity);
    }

    @Test
    public void testClassifyWalkingActivity() {
        // Medium magnitude = walking
        UserActivity activity = ActivityRecognitionManager.classifyActivity(3.0f);
        assertEquals(UserActivity.WALKING, activity);
    }

    @Test
    public void testClassifyRunningActivity() {
        // High magnitude = running
        UserActivity activity = ActivityRecognitionManager.classifyActivity(8.0f);
        assertEquals(UserActivity.RUNNING, activity);
    }

    @Test
    public void testClassifyNegativeMagnitudeAsWalking() {
        // Negative magnitude (deceleration) above walking threshold
        UserActivity activity = ActivityRecognitionManager.classifyActivity(-3.0f);
        assertEquals(UserActivity.WALKING, activity);
    }

    @Test
    public void testClassifyNegativeMagnitudeAsRunning() {
        UserActivity activity = ActivityRecognitionManager.classifyActivity(-8.0f);
        assertEquals(UserActivity.RUNNING, activity);
    }

    @Test
    public void testClassifyAtExactWalkingThreshold() {
        UserActivity activity = ActivityRecognitionManager.classifyActivity(2.0f);
        assertEquals(UserActivity.WALKING, activity);
    }

    @Test
    public void testClassifyBelowWalkingThreshold() {
        UserActivity activity = ActivityRecognitionManager.classifyActivity(1.9f);
        assertEquals(UserActivity.IDLE, activity);
    }

    @Test
    public void testClassifyAtExactRunningThreshold() {
        UserActivity activity = ActivityRecognitionManager.classifyActivity(6.0f);
        assertEquals(UserActivity.RUNNING, activity);
    }

    @Test
    public void testMagnitudeCalculationAllAxes() {
        // Test that all three axes contribute to magnitude
        float[] xOnly = {5.0f, 0f, 0f};
        float[] yOnly = {0f, 5.0f, 0f};
        float[] zOnly = {0f, 0f, 5.0f};

        float magX = ActivityRecognitionManager.calculateMagnitude(xOnly);
        float magY = ActivityRecognitionManager.calculateMagnitude(yOnly);
        float magZ = ActivityRecognitionManager.calculateMagnitude(zOnly);

        // All should produce the same base magnitude (before gravity subtraction)
        assertEquals(magX, magY, 0.001f);
        assertEquals(magY, magZ, 0.001f);
    }
}
