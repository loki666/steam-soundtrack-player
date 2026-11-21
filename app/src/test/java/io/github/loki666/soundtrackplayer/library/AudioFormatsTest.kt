package io.github.loki666.soundtrackplayer.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatsTest {
    @Test
    fun `prefers lossless over mp3`() {
        assertEquals("wav", AudioFormats.best(listOf("MP3/01 Intro.mp3", "WAV/01 Intro.WAV", "cover.jpg")))
        assertEquals("flac", AudioFormats.best(listOf("mp3/a.mp3", "flac/a.flac", "wav/a.wav")))
        assertEquals("mp3", AudioFormats.best(listOf("a.mp3", "booklet.pdf")))
        assertNull(AudioFormats.best(listOf("readme.txt")))
    }

    @Test
    fun `download filter keeps the chosen format and non-audio files only`() {
        val filter = AudioFormats.downloadFilter("wav")
        fun included(name: String) = filter.any { it.matches(name) }

        assertTrue(included("WAV/01 - Tidewake.wav"))
        assertTrue(included("WAV/02 - Harbor.WAV"))
        assertTrue(included("cover.jpg"))
        assertTrue(included("Digital Booklet.pdf"))
        assertFalse(included("MP3/01 - Tidewake.mp3"))
        assertFalse(included("FLAC/01 - Tidewake.flac"))
    }
}
