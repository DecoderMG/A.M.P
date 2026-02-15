package com.dmgproductions.amp.service;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the integration between TempoMatcher and the playback service's
 * tempo-aware queue building and speed adjustment logic.
 */
public class TempoAwarePlaybackTest {

    // --- Queue scoring for tempo matching ---

    @Test
    public void testWalkingSongsRankedByTempoMatch() {
        float walkingBPM = 120f;
        float tolerance = 15f;

        MusicPlaybackService.SongInfo perfectMatch = new MusicPlaybackService.SongInfo(
                1, "Perfect", "A", "A", "/a", 180000, 120f);
        MusicPlaybackService.SongInfo closeMatch = new MusicPlaybackService.SongInfo(
                2, "Close", "A", "A", "/b", 180000, 125f);
        MusicPlaybackService.SongInfo farMatch = new MusicPlaybackService.SongInfo(
                3, "Far", "A", "A", "/c", 180000, 160f);

        float scorePerfect = TempoMatcher.scoreSongMatch(perfectMatch.bpm, walkingBPM, tolerance);
        float scoreClose = TempoMatcher.scoreSongMatch(closeMatch.bpm, walkingBPM, tolerance);
        float scoreFar = TempoMatcher.scoreSongMatch(farMatch.bpm, walkingBPM, tolerance);

        assertTrue("Perfect match should score highest", scorePerfect > scoreClose);
        assertTrue("Close match should score higher than far", scoreClose > scoreFar);
    }

    @Test
    public void testRunningTempoSpeedAdjustment() {
        // Running at 170 BPM, song is at 160 BPM
        float speed = TempoMatcher.calculatePlaybackSpeed(160f, 170f, 0.15f);
        // 170/160 = 1.0625 — within 15% range
        assertEquals(1.0625f, speed, 0.01f);
        assertTrue("Should speed up slightly", speed > 1.0f);
    }

    @Test
    public void testIdleNormalPlaybackSpeed() {
        // Idle with no target BPM
        float speed = TempoMatcher.calculatePlaybackSpeed(120f, 0f, 0.15f);
        assertEquals(1.0f, speed, 0.01f);
    }

    @Test
    public void testDoubleTimeMatchForHalfBPMSong() {
        // Running at 170 BPM, song is at 85 BPM (half time = 1:1 match)
        float speed = TempoMatcher.calculatePlaybackSpeed(85f, 170f, 0.15f);
        // halfRatio = (170/2) / 85 = 1.0 — perfect half-time match
        assertEquals(1.0f, speed, 0.01f);
    }

    @Test
    public void testSongWithNoBPMGetsZeroScore() {
        float score = TempoMatcher.scoreSongMatch(0f, 120f, 15f);
        assertEquals(0f, score, 0.01f);
    }

    @Test
    public void testActivityToTempoRangeRoundTrip() {
        // Walking -> get tempo range -> check target BPM makes sense
        TempoMatcher.TempoRange walkingRange = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.WALKING);
        assertTrue("Walking target should be within range",
                walkingRange.contains(walkingRange.targetBPM));

        TempoMatcher.TempoRange runningRange = TempoMatcher.getTempoRangeForActivity(
                ActivityRecognitionManager.UserActivity.RUNNING);
        assertTrue("Running target should be within range",
                runningRange.contains(runningRange.targetBPM));
    }

    @Test
    public void testSpeedClampPreventsExtremeAdjustment() {
        // Song at 80 BPM, target at 170 BPM -> raw ratio = 2.125
        // But half-time ratio = (170/2)/80 = 1.0625 which is closer to 1.0
        float speed = TempoMatcher.calculatePlaybackSpeed(80f, 170f, 0.15f);
        assertTrue("Speed should be between 0.85 and 1.15",
                speed >= 0.85f && speed <= 1.15f);
    }

    @Test
    public void testTempoMatchPreservesHarmonicRelationships() {
        // Song at 130, target at 130 -> direct match score should be 1.0
        float directScore = TempoMatcher.scoreSongMatch(130f, 130f, 10f);
        assertEquals(1.0f, directScore, 0.01f);

        // Song at 65, target at 130 -> half-time match
        float halfTimeScore = TempoMatcher.scoreSongMatch(65f, 130f, 10f);
        assertTrue("Half-time should score > 0", halfTimeScore > 0f);
        assertTrue("Half-time should be penalized vs direct", halfTimeScore < directScore);
    }
}
