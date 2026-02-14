package com.dmgproductions.amp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.dmgproductions.amp.MainAmpActivity;
import com.dmgproductions.amp.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicPlaybackService extends LifecycleService {

    private static final String CHANNEL_ID = "amp_playback_channel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_PLAY = "com.dmgproductions.amp.action.PLAY";
    public static final String ACTION_PAUSE = "com.dmgproductions.amp.action.PAUSE";
    public static final String ACTION_NEXT = "com.dmgproductions.amp.action.NEXT";
    public static final String ACTION_STOP = "com.dmgproductions.amp.action.STOP";

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private final IBinder binder = new LocalBinder();
    private final Random random = new Random();

    private final MutableLiveData<PlaybackState> playbackState = new MutableLiveData<>(PlaybackState.STOPPED);
    private final MutableLiveData<SongInfo> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);

    private List<SongInfo> songLibrary = new ArrayList<>();
    private int currentSongIndex = -1;

    public enum PlaybackState {
        PLAYING, PAUSED, STOPPED
    }

    public static class SongInfo {
        public final long id;
        public final String title;
        public final String artist;
        public final String album;
        public final String path;
        public final long duration;

        public SongInfo(long id, String title, String artist, String album, String path, long duration) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.path = path;
            this.duration = duration;
        }
    }

    public class LocalBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaSession();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> playNext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_STOP:
                    stop();
                    break;
            }
        }
        return START_STICKY;
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "AMP_MediaSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onStop() {
                stop();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });
        mediaSession.setActive(true);
    }

    public void loadMusicLibrary() {
        songLibrary.clear();
        ContentResolver resolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION
        };

        try (Cursor cursor = resolver.query(musicUri, projection, selection, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String album = cursor.getString(3);
                    String path = cursor.getString(4);
                    long duration = cursor.getLong(5);
                    songLibrary.add(new SongInfo(id, title, artist, album, path, duration));
                }
            }
        }
    }

    public void playSong(int index) {
        if (index < 0 || index >= songLibrary.size()) return;
        currentSongIndex = index;
        SongInfo song = songLibrary.get(index);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            playbackState.postValue(PlaybackState.PLAYING);
            currentSong.postValue(song);
            updateMediaSessionMetadata(song);
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            showNotification(song);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (mediaPlayer != null) {
            if (currentSongIndex == -1 && !songLibrary.isEmpty()) {
                playSong(random.nextInt(songLibrary.size()));
            } else if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                playbackState.postValue(PlaybackState.PLAYING);
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                SongInfo song = currentSong.getValue();
                if (song != null) {
                    showNotification(song);
                }
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playbackState.postValue(PlaybackState.PAUSED);
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            SongInfo song = currentSong.getValue();
            if (song != null) {
                showNotification(song);
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            playbackState.postValue(PlaybackState.STOPPED);
            updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    public void playNext() {
        if (songLibrary.isEmpty()) return;
        int nextIndex = random.nextInt(songLibrary.size());
        playSong(nextIndex);
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
            playbackPosition.postValue(position);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null && mediaPlayer.isPlaying() ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public MutableLiveData<PlaybackState> getPlaybackState() {
        return playbackState;
    }

    public MutableLiveData<SongInfo> getCurrentSong() {
        return currentSong;
    }

    public MutableLiveData<Integer> getPlaybackPosition() {
        return playbackPosition;
    }

    public List<SongInfo> getSongLibrary() {
        return songLibrary;
    }

    private void updateMediaSessionMetadata(SongInfo song) {
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build();
        mediaSession.setMetadata(metadata);
    }

    private void updateMediaSessionPlaybackState(int state) {
        PlaybackStateCompat playbackStateCompat = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0, 1.0f)
                .build();
        mediaSession.setPlaybackState(playbackStateCompat);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AMP Music Playback",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Controls for music playback");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void showNotification(SongInfo song) {
        Intent contentIntent = new Intent(this, MainAmpActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE);

        boolean isPlaying = mediaPlayer != null && mediaPlayer.isPlaying();

        Intent playPauseIntent = new Intent(this, MusicPlaybackService.class);
        playPauseIntent.setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePending = PendingIntent.getService(
                this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicPlaybackService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(
                this, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(song.artist + " - " + song.album)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentPendingIntent)
                .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        isPlaying ? "Pause" : "Play", playPausePending)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1))
                .setOngoing(isPlaying)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
