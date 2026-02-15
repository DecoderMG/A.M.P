package com.dmgproductions.amp.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class ActivityBridgeTest {

    // --- activityToScore ---

    @Test
    public void testActivityToScoreIdle() {
        assertEquals(0f, ActivityBridge.activityToScore(
                ActivityRecognitionManager.UserActivity.IDLE), 0.01f);
    }

    @Test
    public void testActivityToScoreWalking() {
        assertEquals(1f, ActivityBridge.activityToScore(
                ActivityRecognitionManager.UserActivity.WALKING), 0.01f);
    }

    @Test
    public void testActivityToScoreRunning() {
        assertEquals(2f, ActivityBridge.activityToScore(
                ActivityRecognitionManager.UserActivity.RUNNING), 0.01f);
    }

    @Test
    public void testActivityToScoreUnknown() {
        assertEquals(0.5f, ActivityBridge.activityToScore(
                ActivityRecognitionManager.UserActivity.UNKNOWN), 0.01f);
    }

    // --- scoreToActivity ---

    @Test
    public void testScoreToActivityIdle() {
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                ActivityBridge.scoreToActivity(0f));
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                ActivityBridge.scoreToActivity(0.4f));
    }

    @Test
    public void testScoreToActivityWalking() {
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.scoreToActivity(0.5f));
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.scoreToActivity(1.0f));
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.scoreToActivity(1.4f));
    }

    @Test
    public void testScoreToActivityRunning() {
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                ActivityBridge.scoreToActivity(1.5f));
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                ActivityBridge.scoreToActivity(2.0f));
    }

    // --- refineActivityFromBPM ---

    @Test
    public void testRefineIdleLowBPM() {
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.IDLE, 30f));
    }

    @Test
    public void testRefineIdleWithWalkingBPM() {
        // BPM 100 with IDLE detection -> should be refined to WALKING
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.IDLE, 100f));
    }

    @Test
    public void testRefineRunningWithLowBPM() {
        // Detected as RUNNING but BPM only 100 -> should be refined to WALKING
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.RUNNING, 100f));
    }

    @Test
    public void testRefineIdleWithRunningBPM() {
        // Detected as IDLE but BPM is 160 -> should be refined to RUNNING
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.IDLE, 160f));
    }

    @Test
    public void testRefineWalkingWithWalkingBPM() {
        // Already walking, BPM confirms -> stays walking
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.WALKING, 110f));
    }

    @Test
    public void testRefineRunningWithRunningBPM() {
        // Already running, BPM confirms -> stays running
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                ActivityBridge.refineActivityFromBPM(
                        ActivityRecognitionManager.UserActivity.RUNNING, 170f));
    }

    // --- Round-trip score conversion ---

    @Test
    public void testScoreRoundTripIdle() {
        float score = ActivityBridge.activityToScore(ActivityRecognitionManager.UserActivity.IDLE);
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                ActivityBridge.scoreToActivity(score));
    }

    @Test
    public void testScoreRoundTripWalking() {
        float score = ActivityBridge.activityToScore(ActivityRecognitionManager.UserActivity.WALKING);
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                ActivityBridge.scoreToActivity(score));
    }

    @Test
    public void testScoreRoundTripRunning() {
        float score = ActivityBridge.activityToScore(ActivityRecognitionManager.UserActivity.RUNNING);
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                ActivityBridge.scoreToActivity(score));
    }
}
