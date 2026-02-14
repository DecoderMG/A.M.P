package com.dmgproductions.amp.viewmodel;

import com.dmgproductions.amp.service.MusicPlaybackService;

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
}
