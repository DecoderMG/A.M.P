# YouTube Music source

A.M.P drives the **installed YouTube Music app** through Android's media-session
APIs — it never streams the catalog itself, so it stays within YouTube's Terms
of Service. This is fully implemented in `streaming/YouTubeMusicSource.kt`.

## What it does

- **Observe** — reads the currently playing title/artist and play state from
  YouTube Music's active `MediaController`.
- **Control** — play / pause / skip via the media session's transport controls.
- **Switch by activity** — when your activity changes, A.M.P deep-links YouTube
  Music to the playlist you configured for that activity.

## Setup (on device)

1. Install YouTube Music.
2. In A.M.P → **Settings → Audio sources**, choose **YouTube Music**.
3. Tap **Grant notification access** and enable A.M.P in the system list. This is
   required for Android to expose the active media session to A.M.P. (A.M.P reads
   only media sessions, not notification content.)
4. Paste a **YouTube Music playlist link** for each activity
   (`https://music.youtube.com/playlist?list=…`).

## How it works

- Media sessions are read via `MediaSessionManager.getActiveSessions(...)`, which
  requires an enabled `NotificationListenerService`
  (`AmpNotificationListenerService`) — hence the notification-access grant.
- Playlist switching uses an `ACTION_VIEW` intent targeting
  `com.google.android.apps.youtube.music`.

## Limits

- A.M.P controls YouTube Music's playback; it can't crossfade its audio (that's a
  local-only feature). Activity changes become playlist switches.
- Background/foreground behavior depends on the YouTube Music app itself.
