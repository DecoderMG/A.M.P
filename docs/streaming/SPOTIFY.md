# Activating Spotify (App Remote)

A.M.P ships a Spotify **scaffold** (`streaming/SpotifySource.kt`) that already
stores per-activity playlist URIs and implements the `StreamingSource` contract.
The actual playback calls are stubbed because Spotify's **App Remote SDK is a
manual `.aar` with no Maven artifact**, so it can't live in this repo's CI. Here
is how to make it real.

## 1. Add the SDK

1. Download the **Spotify Android SDK** (App Remote) from the Spotify developer
   site and copy `spotify-app-remote-release-x.y.z.aar` into `aMP/libs/`.
2. In `aMP/build.gradle`:

   ```groovy
   android {
       // ...
   }
   dependencies {
       implementation fileTree(dir: 'libs', include: ['*.aar'])
       implementation 'com.spotify.android:auth:2.1.1' // optional: auth flow
       // ...
   }
   ```

## 2. Register your app

- Create an app in the Spotify Developer Dashboard to get a **Client ID**.
- Add a **Redirect URI** (e.g. `amp://spotify-callback`) both in the dashboard
  and to your app.
- Make sure the Spotify app is installed and logged in on the device.

## 3. Wire the calls

Fill in the `TODO(spotify)` markers in `SpotifySource.kt`:

```kotlin
private var appRemote: SpotifyAppRemote? = null

override fun start() {
    val params = ConnectionParams.Builder(CLIENT_ID)
        .setRedirectUri(REDIRECT_URI)
        .showAuthView(true)
        .build()
    SpotifyAppRemote.connect(appContext, params, object : Connector.ConnectionListener {
        override fun onConnected(remote: SpotifyAppRemote) {
            appRemote = remote
            remote.playerApi.subscribeToPlayerState().setEventCallback { state ->
                _nowPlaying.value = ExternalNowPlaying(
                    available = true,
                    isPlaying = !state.isPaused,
                    title = state.track?.name,
                    artist = state.track?.artist?.name,
                )
            }
        }
        override fun onFailure(error: Throwable) {
            _nowPlaying.value = ExternalNowPlaying(available = false)
        }
    })
}

override fun stop() {
    appRemote?.let { SpotifyAppRemote.disconnect(it) }
    appRemote = null
    _nowPlaying.value = ExternalNowPlaying()
}

override fun onActivity(activity: ActivityState) {
    val uri = prefs.playlist(PlaybackSource.SPOTIFY, activity)
    if (uri.isNotBlank()) appRemote?.playerApi?.play(uri)   // "spotify:playlist:…"
}

override fun playPause() { appRemote?.playerApi?.let { /* toggle via last state */ } }
override fun next() { appRemote?.playerApi?.skipNext() }
override fun previous() { appRemote?.playerApi?.skipPrevious() }
```

Constants:

```kotlin
private companion object {
    const val CLIENT_ID = "<your-client-id>"
    const val REDIRECT_URI = "amp://spotify-callback"
}
```

## 4. Use it

In A.M.P → **Settings → Audio sources**, pick **Spotify** and paste a
`spotify:playlist:…` URI for each activity. A.M.P will call
`playerApi.play(uri)` whenever that activity is detected.

> Note: review Spotify's Developer Terms before redistributing their `.aar` in a
> public repo.
