package com.dmgproductions.amp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.dmgproductions.amp.MainAmpActivity;
import com.dmgproductions.amp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicPlaybackService extends LifecycleService {

    private static final String TAG = "MusicPlaybackService";
    private static final String CHANNEL_ID = "amp_playback_channel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_PLAY = "com.dmgproductions.amp.action.PLAY";
    public static final String ACTION_PAUSE = "com.dmgproductions.amp.action.PAUSE";
    public static final String ACTION_NEXT = "com.dmgproductions.amp.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.dmgproductions.amp.action.PREVIOUS";
    public static final String ACTION_STOP = "com.dmgproductions.amp.action.STOP";
    public static final String ACTION_SET_TEMPO = "com.dmgproductions.amp.action.SET_TEMPO";
    public static final String EXTRA_TARGET_BPM = "target_bpm";

    private ExoPlayer exoPlayer;
    private MediaSession mediaSession;
    private final IBinder binder = new LocalBinder();
    private final Random random = new Random();

    private final MutableLiveData<PlaybackState> playbackState =
            new MutableLiveData<>(PlaybackState.STOPPED);
    private final MutableLiveData<SongInfo> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Integer> playbackPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Float> currentPlaybackSpeed = new MutableLiveData<>(1.0f);

    private List<SongInfo> songLibrary = new ArrayList<>();
    private List<SongInfo> currentQueue = new ArrayList<>();
    private int currentQueueIndex = -1;

    // Tempo-aware playback
    private float targetBPM = 0f;
    private float currentSongBPM = 0f;
    private boolean tempoMatchingEnabled = true;
    private static final float MAX_SPEED_ADJUSTMENT = 0.15f; // +/-15%

    public enum PlaybackState {
        PLAYING, PAUSED, STOPPED, BUFFERING
    }

    public static class SongInfo {
        public final long id;
        public final String title;
        public final String artist;
        public final String album;
        public final String path;
        public final long duration;
        public float bpm; // Estimated BPM, 0 if unknown

        public SongInfo(long id, String title, String artist, String album,
                        String path, long duration) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.path = path;
            this.duration = duration;
            this.bpm = 0f;
        }

        public SongInfo(long id, String title, String artist, String album,
                        String path, long duration, float bpm) {
            this(id, title, artist, album, path, duration);
            this.bpm = bpm;
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
        initExoPlayer();
        initMediaSession();
        Log.d(TAG, "MusicPlaybackService created with Media3 ExoPlayer");
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
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
                case ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case ACTION_STOP:
                    stop();
                    break;
                case ACTION_SET_TEMPO:
                    float bpm = intent.getFloatExtra(EXTRA_TARGET_BPM, 0f);
                    setTargetBPM(bpm);
                    break;
            }
        }
        return START_STICKY;
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    private void initExoPlayer() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // true = handle audio focus
                .setHandleAudioBecomingNoisy(true) // pause on headphone disconnect
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_READY:
                        if (exoPlayer.getPlayWhenReady()) {
                            playbackState.postValue(PlaybackState.PLAYING);
                        } else {
                            playbackState.postValue(PlaybackState.PAUSED);
                        }
                        break;
                    case Player.STATE_BUFFERING:
                        playbackState.postValue(PlaybackState.BUFFERING);
                        break;
                    case Player.STATE_ENDED:
                        onSongCompleted();
                        break;
                    case Player.STATE_IDLE:
                        playbackState.postValue(PlaybackState.STOPPED);
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    playbackState.postValue(PlaybackState.PLAYING);
                    SongInfo song = currentSong.getValue();
                    if (song != null) showNotification(song);
                } else if (exoPlayer.getPlaybackState() == Player.STATE_READY) {
                    playbackState.postValue(PlaybackState.PAUSED);
                    SongInfo song = currentSong.getValue();
                    if (song != null) showNotification(song);
                }
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters params) {
                currentPlaybackSpeed.postValue(params.speed);
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem,
                                               int reason) {
                if (mediaItem != null) {
                    updateCurrentSongFromQueue();
                }
            }
        });
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    private void initMediaSession() {
        Intent intent = new Intent(this, MainAmpActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        mediaSession = new MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(pendingIntent)
                .build();

        Log.d(TAG, "MediaSession initialized");
    }

    // --- Music Library ---

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

        try (Cursor cursor = resolver.query(musicUri, projection, selection,
                null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String title = cursor.getString(1);
                    String artist = cursor.getString(2);
                    String album = cursor.getString(3);
                    String path = cursor.getString(4);
                    long duration = cursor.getLong(5);
                    songLibrary.add(new SongInfo(id, title, artist, album,
                            path, duration));
                }
            }
        }
        Log.d(TAG, "Loaded " + songLibrary.size() + " songs from library");
    }

    // --- Queue Management ---

    /**
     * Build a playback queue sorted by how well each song's BPM
     * matches the target cadence. Best matches play first.
     */
    public void buildTempoMatchedQueue(float targetBPM, float tolerance) {
        if (songLibrary.isEmpty()) return;

        this.targetBPM = targetBPM;
        List<SongInfo> scored = new ArrayList<>(songLibrary);

        if (targetBPM > 0) {
            Collections.sort(scored, (a, b) -> {
                float scoreA = TempoMatcher.scoreSongMatch(a.bpm, targetBPM, tolerance);
                float scoreB = TempoMatcher.scoreSongMatch(b.bpm, targetBPM, tolerance);
                return Float.compare(scoreB, scoreA); // descending
            });
        } else {
            Collections.shuffle(scored, random);
        }

        currentQueue.clear();
        currentQueue.addAll(scored);
        currentQueueIndex = -1;

        loadQueueIntoPlayer();
    }

    /**
     * Build a shuffled queue from the full library.
     */
    public void buildShuffledQueue() {
        currentQueue.clear();
        currentQueue.addAll(songLibrary);
        Collections.shuffle(currentQueue, random);
        currentQueueIndex = -1;
        loadQueueIntoPlayer();
    }

    private void loadQueueIntoPlayer() {
        exoPlayer.clearMediaItems();
        for (SongInfo song : currentQueue) {
            MediaItem mediaItem = buildMediaItem(song);
            exoPlayer.addMediaItem(mediaItem);
        }
        exoPlayer.prepare();
    }

    static MediaItem buildMediaItem(SongInfo song) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .build();

        return new MediaItem.Builder()
                .setUri(Uri.parse(song.path))
                .setMediaMetadata(metadata)
                .setMediaId(String.valueOf(song.id))
                .build();
    }

    private void updateCurrentSongFromQueue() {
        int index = exoPlayer.getCurrentMediaItemIndex();
        if (index >= 0 && index < currentQueue.size()) {
            currentQueueIndex = index;
            SongInfo song = currentQueue.get(index);
            currentSong.postValue(song);
            currentSongBPM = song.bpm;
            applyTempoAdjustment();
            showNotification(song);
        }
    }

    private void onSongCompleted() {
        if (!exoPlayer.hasNextMediaItem()) {
            playbackState.postValue(PlaybackState.STOPPED);
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
    }

    // --- Playback Controls ---

    public void playSong(int libraryIndex) {
        if (libraryIndex < 0 || libraryIndex >= songLibrary.size()) return;

        SongInfo song = songLibrary.get(libraryIndex);
        exoPlayer.clearMediaItems();
        currentQueue.clear();
        currentQueue.add(song);
        currentQueueIndex = 0;

        exoPlayer.setMediaItem(buildMediaItem(song));
        exoPlayer.prepare();
        exoPlayer.play();

        currentSong.postValue(song);
        currentSongBPM = song.bpm;
        applyTempoAdjustment();
    }

    public void play() {
        if (currentQueue.isEmpty() && !songLibrary.isEmpty()) {
            buildShuffledQueue();
            exoPlayer.seekTo(0, 0);
        }
        exoPlayer.play();
    }

    public void pause() {
        exoPlayer.pause();
    }

    public void stop() {
        exoPlayer.stop();
        playbackState.postValue(PlaybackState.STOPPED);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    public void playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem();
        } else if (!currentQueue.isEmpty()) {
            exoPlayer.seekTo(0, 0);
        }
    }

    public void playPrevious() {
        if (exoPlayer.getCurrentPosition() > 3000) {
            exoPlayer.seekTo(0);
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem();
        }
    }

    public void seekTo(int position) {
        exoPlayer.seekTo(position);
        playbackPosition.postValue(position);
    }

    // --- Tempo-Aware Playback ---

    /**
     * Set the target BPM for tempo matching. The service will adjust
     * playback speed to match the user's cadence within the max adjustment.
     */
    public void setTargetBPM(float bpm) {
        this.targetBPM = bpm;
        applyTempoAdjustment();
    }

    public void setTempoMatchingEnabled(boolean enabled) {
        this.tempoMatchingEnabled = enabled;
        if (!enabled) {
            exoPlayer.setPlaybackParameters(new PlaybackParameters(1.0f));
            currentPlaybackSpeed.postValue(1.0f);
        } else {
            applyTempoAdjustment();
        }
    }

    /**
     * Apply tempo adjustment based on current song BPM and target cadence BPM.
     * Uses TempoMatcher to find the best harmonic match and clamps adjustment.
     */
    void applyTempoAdjustment() {
        if (!tempoMatchingEnabled || targetBPM <= 0 || currentSongBPM <= 0) {
            return;
        }

        float speed = TempoMatcher.calculatePlaybackSpeed(
                currentSongBPM, targetBPM, MAX_SPEED_ADJUSTMENT);

        if (Math.abs(speed - 1.0f) > 0.01f) {
            exoPlayer.setPlaybackParameters(new PlaybackParameters(speed));
            currentPlaybackSpeed.postValue(speed);
            Log.d(TAG, String.format("Tempo adjusted: song=%.0f BPM, target=%.0f BPM, speed=%.2fx",
                    currentSongBPM, targetBPM, speed));
        }
    }

    /**
     * Update the BPM for a song in the library.
     */
    public void setSongBPM(long songId, float bpm) {
        for (SongInfo song : songLibrary) {
            if (song.id == songId) {
                song.bpm = bpm;
                break;
            }
        }
        SongInfo current = currentSong.getValue();
        if (current != null && current.id == songId) {
            currentSongBPM = bpm;
            applyTempoAdjustment();
        }
    }

    // --- State Accessors ---

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    public int getDuration() {
        long duration = exoPlayer.getDuration();
        return duration != C.TIME_UNSET ? (int) duration : 0;
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public ExoPlayer getPlayer() {
        return exoPlayer;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
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

    public MutableLiveData<Float> getCurrentPlaybackSpeed() {
        return currentPlaybackSpeed;
    }

    public List<SongInfo> getSongLibrary() {
        return songLibrary;
    }

    public List<SongInfo> getCurrentQueue() {
        return currentQueue;
    }

    public float getTargetBPM() {
        return targetBPM;
    }

    public boolean isTempoMatchingEnabled() {
        return tempoMatchingEnabled;
    }

    // --- Notification ---

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

        boolean playing = exoPlayer.isPlaying();

        Intent prevIntent = new Intent(this, MusicPlaybackService.class);
        prevIntent.setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getService(
                this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MusicPlaybackService.class);
        playPauseIntent.setAction(playing ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent playPausePending = PendingIntent.getService(
                this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicPlaybackService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(
                this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        String subtitle = song.artist + " - " + song.album;
        Float speed = currentPlaybackSpeed.getValue();
        if (speed != null && Math.abs(speed - 1.0f) > 0.01f) {
            subtitle += String.format(" (%.0f%%)", speed * 100);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.title)
                .setContentText(subtitle)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
                .addAction(playing ? android.R.drawable.ic_media_pause
                                : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play", playPausePending)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(playing)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        super.onDestroy();
    }
}
