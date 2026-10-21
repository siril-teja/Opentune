package com.opentune.data.library

import com.opentune.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SongNamesTest {
    private val shared = Song("abc", "YouTube video", "", null)

    @Before fun fresh() {
        LibraryStore.init(RuntimeEnvironment.getApplication())
        LibraryStore.forgetName(shared.videoId)
    }

    @Test fun aRenameIsShownButNotStored() {
        LibraryStore.renameSong(shared, "  Kesariya ", "Arijit Singh")
        val shown = shared.named()
        assertEquals("Kesariya", shown.title)
        assertEquals("Arijit Singh", shown.artist)
        // Liked, queued, downloaded or played under the new name, it's kept under the old one.
        val ref = SongRef.of(shown)
        assertEquals("YouTube video", ref.title)
        assertEquals("", ref.artist)
        assertEquals(shared, shown.unnamed())
    }

    @Test fun renamingAgainKeepsTheOriginalName() {
        LibraryStore.renameSong(shared, "Kesariya", "Arijit Singh")
        LibraryStore.renameSong(shared.named(), "Kesariya (Lo-fi)", "Arijit Singh")
        val name = LibraryStore.renamed.value.getValue("abc")
        assertEquals("Kesariya (Lo-fi)", name.title)
        assertEquals("YouTube video", name.originalTitle)
    }

    @Test fun aBlankFieldKeepsWhatItCameWith() {
        LibraryStore.renameSong(shared, "Kesariya", "  ")
        assertEquals("", shared.named().artist)
        LibraryStore.renameSong(Song("xyz", "Tum Hi Ho", "Arijit Singh", null), "", "Arijit")
        assertEquals("Tum Hi Ho", Song("xyz", "Tum Hi Ho", "Arijit Singh", null).named().title)
        assertEquals("Arijit", Song("xyz", "Tum Hi Ho", "Arijit Singh", null).named().artist)
        LibraryStore.forgetName("xyz")
    }

    @Test fun goingBackToTheOriginalForgetsTheRename() {
        LibraryStore.renameSong(shared, "Kesariya", "Arijit Singh")
        LibraryStore.renameSong(shared.named(), "YouTube video", "")
        assertNull(LibraryStore.renamed.value["abc"])
        LibraryStore.renameSong(shared, "Kesariya", "Arijit Singh")
        LibraryStore.forgetName("abc")
        assertEquals(shared, shared.named())
    }

    @Test fun renamesAreBackedUp() {
        LibraryStore.renameSong(shared, "Kesariya", "Arijit Singh")
        val exported = LibraryStore.exportJson().toString()
        assertTrue(exported.contains("Kesariya"))
        LibraryStore.forgetName("abc")
        assertFalse(LibraryStore.renamed.value.containsKey("abc"))
        LibraryStore.importJson(kotlinx.serialization.json.Json.parseToJsonElement(exported))
        assertEquals("Kesariya", shared.named().title)
    }
}
