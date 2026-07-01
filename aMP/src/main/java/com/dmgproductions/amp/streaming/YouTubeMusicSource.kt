package com.dmgproductions.amp.streaming

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.dmgproductions.amp.player.ActivityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controls/observes the installed YouTube Music app via the platform media
 * session APIs — ToS-friendly (no stream extraction). Reading sessions requires
 * the user to grant Notification access (see [hasNotificationAccess]).
 *
 * On an activity change it deep-links YouTube Music to the playlist configured
 * for that activity; transport controls and "now playing" come from the app's
 * active [MediaController].
 */
class YouTubeMusicSource(
    context: Context,
    private val prefs: StreamingPrefs,
) : StreamingSource {

    private val appContext = context.applicationContext
    override val source = PlaybackSource.YOUTUBE_MUSIC

    private val _nowPlaying = MutableStateFlow(ExternalNowPlaying())
    override val nowPlaying: StateFlow<ExternalNowPlaying> = _nowPlaying.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val listenerComponent = ComponentName(appContext, AmpNotificationListenerService::class.java)

    private var controller: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() {
            controller = null
            publish()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> bindController(controllers) }

    override fun start() {
        val manager = sessionManager ?: return
        runCatching {
            manager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent, handler)
            bindController(manager.getActiveSessions(listenerComponent))
        }
    }

    override fun stop() {
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        _nowPlaying.value = ExternalNowPlaying()
    }

    private fun bindController(controllers: List<MediaController>?) {
        val match = controllers?.firstOrNull { it.packageName == YT_MUSIC_PKG }
        if (match?.sessionToken == controller?.sessionToken) {
            publish()
            return
        }
        controller?.unregisterCallback(controllerCallback)
        controller = match
        controller?.registerCallback(controllerCallback, handler)
        publish()
    }

    private fun publish() {
        val active = controller
        if (active == null) {
            _nowPlaying.value = ExternalNowPlaying(available = false)
            return
        }
        val metadata = active.metadata
        _nowPlaying.value = ExternalNowPlaying(
            available = true,
            isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
        )
    }

    override fun onActivity(activity: ActivityState) {
        val url = prefs.playlist(PlaybackSource.YOUTUBE_MUSIC, activity)
        if (url.isBlank()) return
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val base = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(Intent(base).setPackage(YT_MUSIC_PKG)) }
            .recoverCatching { appContext.startActivity(base) }
    }

    override fun playPause() {
        val active = controller ?: return
        if (active.playbackState?.state == PlaybackState.STATE_PLAYING) {
            active.transportControls.pause()
        } else {
            active.transportControls.play()
        }
    }

    override fun next() {
        controller?.transportControls?.skipToNext()
    }

    override fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    companion object {
        const val YT_MUSIC_PKG = "com.google.android.apps.youtube.music"

        /** Whether A.M.P currently holds Notification access (required to read sessions). */
        fun hasNotificationAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners",
            ) ?: return false
            val me = context.packageName
            return enabled.split(":").any { it.contains(me) }
        }

        fun notificationAccessIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
