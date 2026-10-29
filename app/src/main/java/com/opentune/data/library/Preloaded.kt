package com.opentune.data.library

import android.content.Context
import androidx.core.content.edit
import com.opentune.data.DebugLog as Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Playlists built into the app: the personal edition carries Tej's
 * (app/src/personal/assets), the standard edition none.
 *
 * Each one missing from the library is added, once, so a deleted one
 * stays deleted. One already there under the same name, however it's
 * written ("english wynklist" for EnglishWynkList), is kept rather than
 * added again. The first time, they're named as built in and put at the
 * top in the built-in order; after that, renaming or moving them is left
 * to the user.
 */
object Preloaded {
    private const val ASSET = "preloaded_playlists.json"
    private const val PREFS = "preloaded"
    private const val ARRANGED = "arranged"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    internal data class Playlist(val name: String, val songs: List<SongRef>)

    /** Adds the built-in playlists not added before. Call after [LibraryStore.init]. */
    fun addMissing(context: Context) {
        val text = runCatching { context.assets.open(ASSET).use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        val builtIn = runCatching { parse(text) }.onFailure { Log.w("Preloaded", "Couldn't read $ASSET", it) }.getOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arranged = prefs.getBoolean(ARRANGED, false)
        val ids = builtIn.mapNotNull { p ->
            val key = key(p.name)
            val existing = LibraryStore.playlists.value.firstOrNull { key(it.name) == key }
            val id = when {
                existing != null -> existing.id.also { if (!arranged) LibraryStore.renamePlaylist(it, p.name) }
                prefs.getBoolean(key, false) || p.songs.isEmpty() -> null
                else -> LibraryStore.createPlaylist(p.name).also { LibraryStore.addAllToPlaylist(it, p.songs.map(SongRef::toSong)) }
            }
            prefs.edit { putBoolean(key, true) }
            id
        }
        if (!arranged) {
            LibraryStore.putFirst(ids)
            prefs.edit { putBoolean(ARRANGED, true) }
        }
    }

    /** "English Wynklist", "english wynklist" and "EnglishWynkList" are the same playlist. */
    internal fun key(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

    internal fun parse(text: String): List<Playlist> = json.decodeFromString(ListSerializer(Playlist.serializer()), text)
}
