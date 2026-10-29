package com.opentune.data.library

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreloadedTest {
    @Test fun thePersonalEditionsPlaylistsRead() {
        val playlists = Preloaded.parse(File("src/personal/assets/preloaded_playlists.json").readText())
        assertEquals(listOf("wynklist", "english wynklist", "tej's"), playlists.map { it.name })
        assertTrue(playlists.all { p -> p.songs.isNotEmpty() && p.songs.all { it.videoId.isNotBlank() && it.title.isNotBlank() } })
    }
}
