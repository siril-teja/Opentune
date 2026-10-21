package com.opentune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmbedInfoTest {
    @Test fun namesAnUploadFromItsTitleAndChannel() {
        val song = songFromEmbedInfo(
            "4NRXx6U8ABQ",
            """{"title":"The Weeknd - Blinding Lights (Official Video)","author_name":"TheWeekndVEVO","thumbnail_url":"https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"}""",
        )!!
        assertEquals("Blinding Lights", song.title)
        assertEquals("The Weeknd", song.artist)
        assertEquals("https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg", song.thumbnailUrl)
        assertEquals("4NRXx6U8ABQ", song.videoId)
    }

    @Test fun keepsFeaturedArtistsAndAPlainTitle() {
        val song = songFromEmbedInfo("abc", """{"title":"Stay (feat. Justin Bieber) [Official Audio]","author_name":"The Kid LAROI"}""")!!
        assertEquals("Stay (feat. Justin Bieber)", song.title)
        assertEquals("The Kid LAROI", song.artist)
        val plain = songFromEmbedInfo("abc", """{"title":"Kesariya Lo-fi Mix","author_name":"Lofi Beats"}""")!!
        assertEquals("Kesariya Lo-fi Mix", plain.title)
        assertEquals("Lofi Beats", plain.artist)
    }

    @Test fun noTitleNoSong() {
        assertNull(songFromEmbedInfo("abc", """{"author_name":"Someone"}"""))
        assertNull(songFromEmbedInfo("abc", "Unauthorized"))
    }
}
