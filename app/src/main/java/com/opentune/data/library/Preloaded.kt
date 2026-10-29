package com.opentune.data.library

import android.content.Context
import androidx.core.content.edit
import com.opentune.data.DebugLog as Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Playlists built into the app, added to the library on first start: the
 * personal edition carries Tej's (app/src/personal/assets), the standard
 * edition none. A playlist already in the library under the same name is
 * left alone, so nothing is added twice; each is added once, so deleting
 * one keeps it deleted.
 */
object Preloaded {
    private const val ASSET = "preloaded_playlists.json"
    private const val PREFS = "preloaded"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    internal data class Playlist(val name: String, val songs: List<SongRef>)

    /** Adds the built-in playlists not added before. Call after [LibraryStore.init]. */
    fun addMissing(context: Context) {
        val text = runCatching { context.assets.open(ASSET).use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        val playlists = runCatching { parse(text) }.onFailure { Log.w("Preloaded", "Couldn't read $ASSET", it) }.getOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val have = LibraryStore.playlists.value.mapTo(HashSet()) { it.name.trim().lowercase() }
        playlists.forEach { p ->
            if (prefs.getBoolean(p.name, false)) return@forEach
            if (p.name.trim().lowercase() !in have && p.songs.isNotEmpty()) {
                val id = LibraryStore.createPlaylist(p.name)
                LibraryStore.addAllToPlaylist(id, p.songs.map { it.toSong() })
            }
            prefs.edit { putBoolean(p.name, true) }
        }
    }

    internal fun parse(text: String): List<Playlist> = json.decodeFromString(ListSerializer(Playlist.serializer()), text)
}
