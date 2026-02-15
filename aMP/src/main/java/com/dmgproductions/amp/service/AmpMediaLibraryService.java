package com.dmgproductions.amp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;

import com.dmgproductions.amp.MainAmpActivity;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * MediaLibraryService provides a browsable media tree for external clients
 * (Android Auto, WearOS, Bluetooth headsets, Google Assistant, lock screen).
 *
 * This service exposes the music library in a tree structure:
 *   Root
 *   ├── All Songs
 *   ├── Walking Tempo
 *   └── Running Tempo
 *
 * The MusicPlaybackService handles the main app playback; this service
 * enables external media browser clients to discover and play content.
 */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class AmpMediaLibraryService extends MediaLibraryService {

    private static final String TAG = "AmpMediaLibraryService";
    private static final String CHANNEL_ID = "amp_media_library_channel";

    private static final String ROOT_ID = "amp_root";
    private static final String ALL_SONGS_ID = "all_songs";
    private static final String WALKING_TEMPO_ID = "walking_tempo";
    private static final String RUNNING_TEMPO_ID = "running_tempo";

    private ExoPlayer player;
    private MediaLibrarySession mediaLibrarySession;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initPlayer();
        initSession();
        Log.d(TAG, "AmpMediaLibraryService created");
    }

    private void initPlayer() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();
    }

    private void initSession() {
        Intent intent = new Intent(this, MainAmpActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        mediaLibrarySession = new MediaLibrarySession.Builder(this, player, new LibraryCallback())
                .setSessionActivity(pendingIntent)
                .build();
    }

    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaLibrarySession;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AMP Media Browser",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Media browser service for external clients");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (mediaLibrarySession != null) {
            mediaLibrarySession.release();
        }
        if (player != null) {
            player.release();
        }
        super.onDestroy();
    }

    /**
     * Callback that handles media browsing requests from external clients.
     */
    private class LibraryCallback implements MediaLibrarySession.Callback {

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @Nullable LibraryParams params) {

            MediaMetadata rootMetadata = new MediaMetadata.Builder()
                    .setTitle("A.M.P")
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build();

            MediaItem rootItem = new MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(rootMetadata)
                    .build();

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @NonNull String parentId,
                int page, int pageSize,
                @Nullable LibraryParams params) {

            List<MediaItem> children = new ArrayList<>();

            switch (parentId) {
                case ROOT_ID:
                    children.add(buildBrowsableItem(ALL_SONGS_ID, "All Songs",
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS));
                    children.add(buildBrowsableItem(WALKING_TEMPO_ID, "Walking Tempo (95-135 BPM)",
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS));
                    children.add(buildBrowsableItem(RUNNING_TEMPO_ID, "Running Tempo (130-200 BPM)",
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS));
                    break;

                case ALL_SONGS_ID:
                case WALKING_TEMPO_ID:
                case RUNNING_TEMPO_ID:
                    // These would be populated from the song library in a full implementation
                    // For now return empty — the library will be populated when
                    // MusicPlaybackService loads songs and shares them via a shared repository
                    break;
            }

            return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(children), params));
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @NonNull String mediaId) {

            // Return browsable folder items by ID
            switch (mediaId) {
                case ALL_SONGS_ID:
                    return Futures.immediateFuture(LibraryResult.ofItem(
                            buildBrowsableItem(ALL_SONGS_ID, "All Songs",
                                    MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null));
                case WALKING_TEMPO_ID:
                    return Futures.immediateFuture(LibraryResult.ofItem(
                            buildBrowsableItem(WALKING_TEMPO_ID, "Walking Tempo",
                                    MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null));
                case RUNNING_TEMPO_ID:
                    return Futures.immediateFuture(LibraryResult.ofItem(
                            buildBrowsableItem(RUNNING_TEMPO_ID, "Running Tempo",
                                    MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS), null));
                default:
                    return Futures.immediateFuture(
                            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE));
            }
        }

        private MediaItem buildBrowsableItem(String id, String title, int mediaType) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(mediaType)
                    .build();

            return new MediaItem.Builder()
                    .setMediaId(id)
                    .setMediaMetadata(metadata)
                    .build();
        }
    }
}
