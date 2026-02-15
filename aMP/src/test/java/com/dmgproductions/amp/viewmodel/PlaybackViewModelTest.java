package com.dmgproductions.amp.viewmodel;

import com.dmgproductions.amp.service.ActivityRecognitionManager;
import com.dmgproductions.amp.service.MusicPlaybackService;
import com.dmgproductions.amp.service.TempoMatcher;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlaybackViewModelTest {

    private PlaybackViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new PlaybackViewModel();
    }

    @Test
    public void testInitialPlaybackStateIsStopped() {
        assertEquals(MusicPlaybackService.PlaybackState.STOPPED,
                viewModel.getPlaybackState().getValue());
    }

    @Test
    public void testInitialSongIsNull() {
        assertNull(viewModel.getCurrentSong().getValue());
    }

    @Test
    public void testInitialPositionIsZero() {
        assertEquals(Integer.valueOf(0), viewModel.getPlaybackPosition().getValue());
    }

    @Test
    public void testInitialDurationIsZero() {
        assertEquals(Integer.valueOf(0), viewModel.getDuration().getValue());
    }

    @Test
    public void testInitialActivityIsIdle() {
        assertEquals("idle", viewModel.getCurrentActivity().getValue());
    }

    @Test
    public void testInitialServiceBoundIsFalse() {
        assertEquals(Boolean.FALSE, viewModel.getServiceBound().getValue());
    }

    @Test
    public void testSetPlaybackState() {
        viewModel.setPlaybackState(MusicPlaybackService.PlaybackState.PLAYING);
        assertEquals(MusicPlaybackService.PlaybackState.PLAYING,
                viewModel.getPlaybackState().getValue());
    }

    @Test
    public void testSetCurrentSong() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                1, "Test Song", "Test Artist", "Test Album", "/path/to/song.mp3", 180000);
        viewModel.setCurrentSong(song);

        MusicPlaybackService.SongInfo result = viewModel.getCurrentSong().getValue();
        assertNotNull(result);
        assertEquals("Test Song", result.title);
        assertEquals("Test Artist", result.artist);
    }

    @Test
    public void testSetPlaybackPosition() {
        viewModel.setPlaybackPosition(60000);
        assertEquals(Integer.valueOf(60000), viewModel.getPlaybackPosition().getValue());
    }

    @Test
    public void testSetDuration() {
        viewModel.setDuration(180000);
        assertEquals(Integer.valueOf(180000), viewModel.getDuration().getValue());
    }

    @Test
    public void testSetCurrentActivity() {
        viewModel.setCurrentActivity("running");
        assertEquals("running", viewModel.getCurrentActivity().getValue());
    }

    @Test
    public void testSetServiceBound() {
        viewModel.setServiceBound(true);
        assertEquals(Boolean.TRUE, viewModel.getServiceBound().getValue());
    }

    // --- Phase 3: Activity + Tempo tests ---

    @Test
    public void testInitialDetectedActivityIsIdle() {
        assertEquals(ActivityRecognitionManager.UserActivity.IDLE,
                viewModel.getDetectedActivity().getValue());
    }

    @Test
    public void testInitialCadenceBPMIsZero() {
        assertEquals(Float.valueOf(0f), viewModel.getCadenceBPM().getValue());
    }

    @Test
    public void testInitialPlaybackSpeedIsOne() {
        assertEquals(Float.valueOf(1.0f), viewModel.getPlaybackSpeed().getValue());
    }

    @Test
    public void testInitialStepCountIsZero() {
        assertEquals(Integer.valueOf(0), viewModel.getStepCount().getValue());
    }

    @Test
    public void testSetDetectedActivityUpdatesStringActivity() {
        viewModel.setDetectedActivity(ActivityRecognitionManager.UserActivity.RUNNING);
        assertEquals(ActivityRecognitionManager.UserActivity.RUNNING,
                viewModel.getDetectedActivity().getValue());
        assertEquals("running", viewModel.getCurrentActivity().getValue());
    }

    @Test
    public void testSetCadenceBPMComputesTargetMusicBPM() {
        viewModel.setCadenceBPM(120f);
        assertEquals(Float.valueOf(120f), viewModel.getCadenceBPM().getValue());
        // 120 is in direct range, so target should also be 120
        assertEquals(Float.valueOf(120f), viewModel.getTargetMusicBPM().getValue());
    }

    @Test
    public void testSetCadenceBPMDoubleTimeForSlow() {
        viewModel.setCadenceBPM(60f);
        // 60 is below 80, so should double to 120
        assertEquals(Float.valueOf(120f), viewModel.getTargetMusicBPM().getValue());
    }

    @Test
    public void testUpdateActivityState() {
        viewModel.updateActivityState(
                ActivityRecognitionManager.UserActivity.WALKING, 110f, 0.85f);

        assertEquals(ActivityRecognitionManager.UserActivity.WALKING,
                viewModel.getDetectedActivity().getValue());
        assertEquals(Float.valueOf(110f), viewModel.getCadenceBPM().getValue());
        assertEquals(Float.valueOf(0.85f), viewModel.getActivityConfidence().getValue());

        TempoMatcher.TempoRange range = viewModel.getTempoRange().getValue();
        assertNotNull(range);
        assertEquals(95f, range.minBPM, 0.01f);
        assertEquals(135f, range.maxBPM, 0.01f);
    }

    @Test
    public void testSetPlaybackSpeed() {
        viewModel.setPlaybackSpeed(1.1f);
        assertEquals(Float.valueOf(1.1f), viewModel.getPlaybackSpeed().getValue());
    }

    @Test
    public void testSetStepCount() {
        viewModel.setStepCount(500);
        assertEquals(Integer.valueOf(500), viewModel.getStepCount().getValue());
    }
}
