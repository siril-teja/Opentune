package com.opentune.data.download

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.opentune.data.Ringtones
import com.opentune.data.library.named
import com.opentune.data.model.Song
import java.io.File

/**
 * A copy of each finished download in Music/OpenTune, when turned on in
 * Settings: it stays if the app is uninstalled, other music apps can play
 * it, and this app finds it again under Local music. Android 10 and newer,
 * which let an app add files there without the storage permission.
 */
object MusicFolder {
    private val PATH = "${Environment.DIRECTORY_MUSIC}/OpenTune/"

    val supported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Copies [file], the download of [song], under the name the song has in the app; the copy already there if there is one. */
    fun save(context: Context, song: Song, file: File): Uri? {
        if (!supported) return null
        val shown = song.named()
        val ext = file.extension.ifEmpty { "m4a" }
        val name = Ringtones.fileName(shown, ext)
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        existing(context, collection, name)?.let { return it }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.TITLE, shown.title)
            put(MediaStore.Audio.Media.ARTIST, shown.artist)
            shown.albumName?.let { put(MediaStore.Audio.Media.ALBUM, it) }
            put(MediaStore.Audio.Media.MIME_TYPE, if (ext == "m4a") "audio/mp4" else "audio/webm")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.RELATIVE_PATH, PATH)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: error("Couldn't add the song to Music")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: error("Couldn't write the song to Music")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    @android.annotation.SuppressLint("Recycle") // closed by use below; lint doesn't follow the elvis
    private fun existing(context: Context, collection: Uri, name: String): Uri? {
        val cursor = context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?",
            arrayOf(name, PATH),
            null,
        ) ?: return null
        return cursor.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null }
    }
}
