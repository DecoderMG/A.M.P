package com.dmgproductions.amp.service;

import com.dmgproductions.amp.service.MusicPlaybackService.SongInfo;

import org.junit.Test;

import static org.junit.Assert.*;

public class SongInfoTest {

    @Test
    public void testSongInfoConstructor() {
        SongInfo song = new SongInfo(42, "Faint", "Linkin Park", "Meteora",
                "/music/faint.mp3", 163000);

        assertEquals(42, song.id);
        assertEquals("Faint", song.title);
        assertEquals("Linkin Park", song.artist);
        assertEquals("Meteora", song.album);
        assertEquals("/music/faint.mp3", song.path);
        assertEquals(163000, song.duration);
    }

    @Test
    public void testSongInfoWithNullValues() {
        SongInfo song = new SongInfo(0, null, null, null, null, 0);

        assertEquals(0, song.id);
        assertNull(song.title);
        assertNull(song.artist);
        assertNull(song.album);
        assertNull(song.path);
        assertEquals(0, song.duration);
    }

    @Test
    public void testSongInfoWithEmptyStrings() {
        SongInfo song = new SongInfo(1, "", "", "", "", 0);

        assertEquals("", song.title);
        assertEquals("", song.artist);
    }

    @Test
    public void testSongInfoDefaultBPMIsZero() {
        SongInfo song = new SongInfo(1, "Test", "Artist", "Album", "/path", 180000);
        assertEquals(0f, song.bpm, 0.01f);
    }

    @Test
    public void testSongInfoBPMConstructor() {
        SongInfo song = new SongInfo(1, "Test", "Artist", "Album", "/path", 180000, 128f);
        assertEquals(128f, song.bpm, 0.01f);
    }

    @Test
    public void testSongInfoMutableBPM() {
        SongInfo song = new SongInfo(1, "Test", "Artist", "Album", "/path", 180000);
        assertEquals(0f, song.bpm, 0.01f);
        song.bpm = 145f;
        assertEquals(145f, song.bpm, 0.01f);
    }
}
