package com.dmgproductions.amp.service;

import com.google.android.gms.location.DetectedActivity;

import org.junit.Test;

import static org.junit.Assert.*;

public class GoogleActivityRecognitionSourceTest {

    @Test
    public void testMapRunning() {
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.RUNNING));
    }

    @Test
    public void testMapOnBicycle() {
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.ON_BICYCLE));
    }

    @Test
    public void testMapWalking() {
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.WALKING));
    }

    @Test
    public void testMapOnFoot() {
        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.ON_FOOT));
    }

    @Test
    public void testMapStill() {
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.STILL));
    }

    @Test
    public void testMapTilting() {
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.TILTING));
    }

    @Test
    public void testMapUnknown() {
        assertEquals(ActivityRecognitionManager.UserActivity.UNKNOWN,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.UNKNOWN));
    }

    @Test
    public void testMapInVehicle() {
        assertEquals(ActivityRecognitionManager.UserActivity.UNKNOWN,
                GoogleActivityRecognitionSource.mapGoogleActivity(DetectedActivity.IN_VEHICLE));
    }
}
