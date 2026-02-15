package com.dmgproductions.amp.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class TempoMatcherTest {

    // --- getTempoRangeForActivity ---

    @Test
    public void testIdleTempoRange() {
        TempoMatcher.TempoRange range = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.IDLE);
        assertEquals(60f, range.minBPM, 0.01f);
        assertEquals(100f, range.maxBPM, 0.01f);
        assertEquals(80f, range.targetBPM, 0.01f);
    }

    @Test
    public void testWalkingTempoRange() {
        TempoMatcher.TempoRange range = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.WALKING);
        assertEquals(95f, range.minBPM, 0.01f);
        assertEquals(135f, range.maxBPM, 0.01f);
        assertEquals(115f, range.targetBPM, 0.01f);
    }

    @Test
    public void testRunningTempoRange() {
        TempoMatcher.TempoRange range = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.RUNNING);
        assertEquals(130f, range.minBPM, 0.01f);
        assertEquals(200f, range.maxBPM, 0.01f);
        assertEquals(160f, range.targetBPM, 0.01f);
    }

    @Test
    public void testUnknownActivityDefaultsToIdle() {
        TempoMatcher.TempoRange range = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.UNKNOWN);
        assertEquals(60f, range.minBPM, 0.01f);
    }

    // --- TempoRange.contains ---

    @Test
    public void testTempoRangeContains() {
        TempoMatcher.TempoRange range = new TempoMatcher.TempoRange(100, 140, 120);
        assertTrue(range.contains(120));
        assertTrue(range.contains(100));
        assertTrue(range.contains(140));
        assertFalse(range.contains(99));
        assertFalse(range.contains(141));
    }

    // --- matchMusicTempo ---

    @Test
    public void testDirectMatchInNormalRange() {
        assertEquals(120f, TempoMatcher.matchMusicTempo(120f), 0.01f);
        assertEquals(80f, TempoMatcher.matchMusicTempo(80f), 0.01f);
        assertEquals(200f, TempoMatcher.matchMusicTempo(200f), 0.01f);
    }

    @Test
    public void testDoubleTimeForSlowCadence() {
        // 60 steps/min -> double to 120 BPM for music
        assertEquals(120f, TempoMatcher.matchMusicTempo(60f), 0.01f);
        assertEquals(80f, TempoMatcher.matchMusicTempo(40f), 0.01f);
    }

    @Test
    public void testHalfTimeForFastCadence() {
        // 300 steps/min -> halve to 150 BPM for music
        assertEquals(150f, TempoMatcher.matchMusicTempo(300f), 0.01f);
        assertEquals(200f, TempoMatcher.matchMusicTempo(400f), 0.01f);
    }

    @Test
    public void testZeroCadenceReturnsZero() {
        assertEquals(0f, TempoMatcher.matchMusicTempo(0f), 0.01f);
        assertEquals(0f, TempoMatcher.matchMusicTempo(-10f), 0.01f);
    }

    // --- scoreSongMatch ---

    @Test
    public void testPerfectMatchScoresOne() {
        float score = TempoMatcher.scoreSongMatch(120f, 120f, 10f);
        assertEquals(1.0f, score, 0.01f);
    }

    @Test
    public void testNoMatchScoresZero() {
        float score = TempoMatcher.scoreSongMatch(60f, 180f, 10f);
        assertEquals(0f, score, 0.01f);
    }

    @Test
    public void testPartialMatchScoresMiddle() {
        float score = TempoMatcher.scoreSongMatch(125f, 120f, 10f);
        assertTrue(score > 0f && score < 1.0f);
    }

    @Test
    public void testHalfTimeMatchScoresWithPenalty() {
        // Song at 60 BPM, target at 120 BPM -> half-time match with 0.7 penalty
        float score = TempoMatcher.scoreSongMatch(60f, 120f, 10f);
        assertTrue(score > 0);
        assertTrue(score <= 0.7f);
    }

    @Test
    public void testZeroInputScoresZero() {
        assertEquals(0f, TempoMatcher.scoreSongMatch(0f, 120f, 10f), 0.01f);
        assertEquals(0f, TempoMatcher.scoreSongMatch(120f, 0f, 10f), 0.01f);
    }

    // --- calculatePlaybackSpeed ---

    @Test
    public void testNoAdjustmentNeeded() {
        float speed = TempoMatcher.calculatePlaybackSpeed(120f, 120f, 0.15f);
        assertEquals(1.0f, speed, 0.01f);
    }

    @Test
    public void testSpeedUpForSlowerSong() {
        float speed = TempoMatcher.calculatePlaybackSpeed(110f, 120f, 0.15f);
        assertTrue(speed > 1.0f);
        assertEquals(120f / 110f, speed, 0.01f);
    }

    @Test
    public void testSlowDownForFasterSong() {
        float speed = TempoMatcher.calculatePlaybackSpeed(130f, 120f, 0.15f);
        assertTrue(speed < 1.0f);
        assertEquals(120f / 130f, speed, 0.01f);
    }

    @Test
    public void testClampToMaxAdjustment() {
        // Song at 100, target at 200 -> ratio 2.0, should be clamped to 1.15
        float speed = TempoMatcher.calculatePlaybackSpeed(100f, 200f, 0.15f);
        assertEquals(1.15f, speed, 0.01f);
    }

    @Test
    public void testHarmonicMatchForPlaybackSpeed() {
        // Song at 120 BPM, target at 240 BPM -> half-time ratio = 1.0 (best match)
        float speed = TempoMatcher.calculatePlaybackSpeed(120f, 240f, 0.15f);
        // halfRatio = 120/120 = 1.0 which is closer to 1.0 than 240/120 = 2.0
        assertEquals(1.0f, speed, 0.01f);
    }

    @Test
    public void testZeroInputReturnsOne() {
        assertEquals(1.0f, TempoMatcher.calculatePlaybackSpeed(0f, 120f, 0.15f), 0.01f);
        assertEquals(1.0f, TempoMatcher.calculatePlaybackSpeed(120f, 0f, 0.15f), 0.01f);
    }
}
