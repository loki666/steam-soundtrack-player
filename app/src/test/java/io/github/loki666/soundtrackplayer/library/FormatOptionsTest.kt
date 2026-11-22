package io.github.loki666.soundtrackplayer.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatOptionsTest {
    private val files = listOf(
        ManifestFile("WAV/01 - Tidewake.wav", 50_000_000),
        ManifestFile("WAV/02 - Harbor.wav", 40_000_000),
        ManifestFile("MP3/01 - Tidewake.mp3", 5_000_000),
        ManifestFile("MP3/02 - Harbor.mp3", 4_000_000),
        ManifestFile("cover.jpg", 1_000_000),
        ManifestFile("WAV", 0),
    )

    @Test
    fun `one option per audio format, each including the extras, best quality first`() {
        val options = FormatOptions.from(files)

        assertEquals(
            listOf(FormatOption("wav", 91_000_000), FormatOption("mp3", 10_000_000)),
            options,
        )
    }

    @Test
    fun `best quality picks lossless and smaller files picks the smallest`() {
        val options = FormatOptions.from(files)

        assertEquals("wav", FormatOptions.preferred(options, DownloadQuality.Best)?.format)
        assertEquals("mp3", FormatOptions.preferred(options, DownloadQuality.Smaller)?.format)
    }

    @Test
    fun `flac outranks wav for best quality`() {
        val options = FormatOptions.from(
            listOf(ManifestFile("a.wav", 90), ManifestFile("a.flac", 50), ManifestFile("a.mp3", 10)),
        )

        assertEquals(listOf("flac", "wav", "mp3"), options.map { it.format })
        assertEquals("flac", FormatOptions.preferred(options, DownloadQuality.Best)?.format)
    }

    @Test
    fun `lossless formats are flagged`() {
        assertTrue(FormatOption("flac", 1).lossless)
        assertTrue(FormatOption("wav", 1).lossless)
        assertFalse(FormatOption("mp3", 1).lossless)
        assertEquals("MP3", FormatOption("mp3", 1).label)
    }

    @Test
    fun `no audio files means no options`() {
        val options = FormatOptions.from(listOf(ManifestFile("booklet.pdf", 10)))

        assertEquals(emptyList<FormatOption>(), options)
        assertNull(FormatOptions.preferred(options, DownloadQuality.Best))
    }
}
