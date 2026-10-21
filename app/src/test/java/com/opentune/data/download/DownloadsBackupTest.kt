package com.opentune.data.download

import androidx.test.core.app.ApplicationProvider
import com.opentune.data.library.SongRef
import com.opentune.data.model.Song
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DownloadsBackupTest {
    private val kept = Song("aaaaaaaaaaa", "Kesariya", "Arijit Singh", null)
    private val waiting = Song("bbbbbbbbbbb", "Tum Hi Ho", "Arijit Singh", null)

    @Test fun aBackupListsTheDownloadsAndOffersBackOnlyWhatsMissing() {
        Downloads.init(ApplicationProvider.getApplicationContext())
        Downloads.put(kept.videoId) { DownloadEntry(SongRef.of(kept), DownloadState.DONE) }
        Downloads.put(waiting.videoId) { DownloadEntry(SongRef.of(waiting), DownloadState.QUEUED) }
        val backup = Downloads.exportJson()
        val listed = Json.decodeFromJsonElement(ListSerializer(SongRef.serializer()), backup)
        assertEquals(setOf(kept.videoId, waiting.videoId), listed.map { it.videoId }.toSet())
        // On this phone one is already downloaded: only the other is offered.
        assertEquals(listOf(waiting.videoId), Downloads.missingFrom(backup).map { it.videoId })
    }

    @Test fun afterAReinstallEverySongIsOfferedWithItsName() {
        // A fresh install: nothing downloaded yet.
        Downloads.init(ApplicationProvider.getApplicationContext())
        val backup = Json.encodeToJsonElement(ListSerializer(SongRef.serializer()), listOf(SongRef.of(kept), SongRef.of(waiting), SongRef.of(kept)))
        val missing = Downloads.missingFrom(backup)
        assertEquals(listOf(kept.videoId, waiting.videoId), missing.map { it.videoId })
        assertEquals("Kesariya", missing.first().title)
        assertEquals("Arijit Singh", missing.first().artist)
    }

    @Test fun anOlderOrDamagedBackupOffersNothing() {
        Downloads.init(ApplicationProvider.getApplicationContext())
        assertEquals(emptyList<Song>(), Downloads.missingFrom(kotlinx.serialization.json.JsonPrimitive("nope")))
    }
}
