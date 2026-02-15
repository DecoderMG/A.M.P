package com.dmgproductions.amp.service;

import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import org.junit.Test;

import static org.junit.Assert.*;

public class MusicPlaybackServiceTest {

    // --- buildMediaItem ---

    @Test
    public void testBuildMediaItemSetsMediaId() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                42, "Test Song", "Artist", "Album", "/music/test.mp3", 180000);

        MediaItem item = MusicPlaybackService.buildMediaItem(song);

        assertEquals("42", item.mediaId);
    }

    @Test
    public void testBuildMediaItemSetsUri() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                1, "Song", "Artist", "Album", "/music/song.mp3", 120000);

        MediaItem item = MusicPlaybackService.buildMediaItem(song);

        assertNotNull(item.localConfiguration);
        assertEquals(Uri.parse("/music/song.mp3"), item.localConfiguration.uri);
    }

    @Test
    public void testBuildMediaItemSetsMetadata() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                1, "My Song", "My Artist", "My Album", "/path", 60000);

        MediaItem item = MusicPlaybackService.buildMediaItem(song);
        MediaMetadata metadata = item.mediaMetadata;

        assertEquals("My Song", metadata.title.toString());
        assertEquals("My Artist", metadata.artist.toString());
        assertEquals("My Album", metadata.albumTitle.toString());
    }

    // --- PlaybackState enum ---

    @Test
    public void testPlaybackStateValues() {
        MusicPlaybackService.PlaybackState[] states = MusicPlaybackService.PlaybackState.values();
        assertEquals(4, states.length);
        assertNotNull(MusicPlaybackService.PlaybackState.valueOf("PLAYING"));
        assertNotNull(MusicPlaybackService.PlaybackState.valueOf("PAUSED"));
        assertNotNull(MusicPlaybackService.PlaybackState.valueOf("STOPPED"));
        assertNotNull(MusicPlaybackService.PlaybackState.valueOf("BUFFERING"));
    }

    // --- SongInfo BPM constructor ---

    @Test
    public void testSongInfoBPMConstructor() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                1, "Fast Song", "Artist", "Album", "/path", 200000, 150f);

        assertEquals(150f, song.bpm, 0.01f);
        assertEquals("Fast Song", song.title);
    }

    @Test
    public void testSongInfoDefaultBPMIsZero() {
        MusicPlaybackService.SongInfo song = new MusicPlaybackService.SongInfo(
                1, "Song", "Artist", "Album", "/path", 200000);

        assertEquals(0f, song.bpm, 0.01f);
    }

    // --- Action constants ---

    @Test
    public void testActionConstants() {
        assertNotNull(MusicPlaybackService.ACTION_PLAY);
        assertNotNull(MusicPlaybackService.ACTION_PAUSE);
        assertNotNull(MusicPlaybackService.ACTION_NEXT);
        assertNotNull(MusicPlaybackService.ACTION_PREVIOUS);
        assertNotNull(MusicPlaybackService.ACTION_STOP);
        assertNotNull(MusicPlaybackService.ACTION_SET_TEMPO);
        assertNotNull(MusicPlaybackService.EXTRA_TARGET_BPM);

        // Verify they're all unique
        String[] actions = {
                MusicPlaybackService.ACTION_PLAY,
                MusicPlaybackService.ACTION_PAUSE,
                MusicPlaybackService.ACTION_NEXT,
                MusicPlaybackService.ACTION_PREVIOUS,
                MusicPlaybackService.ACTION_STOP,
                MusicPlaybackService.ACTION_SET_TEMPO
        };
        for (int i = 0; i < actions.length; i++) {
            for (int j = i + 1; j < actions.length; j++) {
                assertNotEquals("Action constants must be unique",
                        actions[i], actions[j]);
            }
        }
    }
}
