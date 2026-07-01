package com.dmgproductions.amp.streaming

import android.content.Context
import com.dmgproductions.amp.player.ActivityState

/** SharedPreferences store for the streaming config (chosen source + per-activity playlists). */
class StreamingPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("amp_streaming", Context.MODE_PRIVATE)

    var source: PlaybackSource
        get() = runCatching {
            PlaybackSource.valueOf(prefs.getString(KEY_SOURCE, PlaybackSource.LOCAL.name)!!)
        }.getOrDefault(PlaybackSource.LOCAL)
        set(value) { prefs.edit().putString(KEY_SOURCE, value.name).apply() }

    fun playlist(source: PlaybackSource, activity: ActivityState): String =
        prefs.getString(key(source, activity), "").orEmpty()

    fun setPlaylist(source: PlaybackSource, activity: ActivityState, value: String) {
        prefs.edit().putString(key(source, activity), value.trim()).apply()
    }

    private fun key(source: PlaybackSource, activity: ActivityState) =
        "pl_${source.name}_${activity.name}"

    private companion object {
        const val KEY_SOURCE = "source"
    }
}
