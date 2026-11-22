package io.github.loki666.soundtrackplayer.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackScannerTest {
    @Test
    fun `keeps the format that was just downloaded over leftovers of a better one`() {
        assertEquals("mp3", TrackScanner.chooseFormat(setOf("wav", "mp3"), downloaded = "mp3"))
    }

    @Test
    fun `without a downloaded format, keeps the best one on disk`() {
        assertEquals("wav", TrackScanner.chooseFormat(setOf("mp3", "wav"), downloaded = null))
    }

    @Test
    fun `falls back to the best one when the downloaded format is missing`() {
        assertEquals("flac", TrackScanner.chooseFormat(setOf("flac", "mp3"), downloaded = "wav"))
    }

    @Test
    fun `no audio means no format`() {
        assertNull(TrackScanner.chooseFormat(emptySet(), downloaded = "mp3"))
    }
}
