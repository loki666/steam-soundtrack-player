package io.github.loki666.soundtrackplayer.download

import io.github.loki666.soundtrackplayer.library.AudioFormats
import io.github.loki666.soundtrackplayer.library.ManifestFile
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DepotFilesTest {
    @Test
    fun `parses file names from a manifest dump`() {
        val dump = """
            Content Manifest for Depot 1235

            Manifest ID / date     : 42 / Tue Nov 18 10:00:00 UTC 2026
            Total number of files  : 3
            Total number of chunks : 9
            Total bytes on disk    : 123
            Total bytes compressed : 100


                      Size Chunks File SHA                                 Flags Name
                  52428800      5 0123456789abcdef0123456789abcdef01234567     0 WAV/01 - Tidewake.wav
                   5242880      1 89abcdef0123456789abcdef0123456789abcdef     0 MP3/01 - Tidewake.mp3
                         0      0 0000000000000000000000000000000000000000    40 WAV
        """.trimIndent().lines()

        assertEquals(
            listOf(
                ManifestFile("WAV/01 - Tidewake.wav", 52_428_800),
                ManifestFile("MP3/01 - Tidewake.mp3", 5_242_880),
                ManifestFile("WAV", 0),
            ),
            DepotFiles.parseManifestDump(dump),
        )
    }

    /**
     * Guards the reflection into JavaSteam: if an upgrade renames DepotDownloader's internal config,
     * this fails instead of the app silently downloading every format.
     */
    @Test
    fun `file filter is applied to DepotDownloader`() {
        val downloader = DepotDownloader(SteamClient(), emptyList())
        try {
            assertTrue(DepotFiles.enableFileFilter(downloader, AudioFormats.downloadFilter("wav")))

            val isIncluded = DepotDownloader::class.java.getDeclaredMethod("testIsFileIncluded", String::class.java)
                .apply { isAccessible = true }
            fun included(name: String) = isIncluded.invoke(downloader, name) as Boolean

            assertTrue(included("WAV\\01 - Tidewake.wav"))
            assertTrue(included("cover.png"))
            assertFalse(included("MP3\\01 - Tidewake.mp3"))
        } finally {
            downloader.close()
        }
    }
}
