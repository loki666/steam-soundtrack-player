package io.github.loki666.soundtrackplayer.steam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SteamAvatarTest {
    @Test
    fun `builds the full-size avatar url from the hash`() {
        val hash = byteArrayOf(0x0f, 0xa1.toByte(), 0x00, 0x7f, 0xff.toByte())

        assertEquals("https://avatars.steamstatic.com/0fa1007fff_full.jpg", steamAvatarUrl(hash))
    }

    @Test
    fun `no avatar for a missing or all-zero hash`() {
        assertNull(steamAvatarUrl(null))
        assertNull(steamAvatarUrl(ByteArray(0)))
        assertNull(steamAvatarUrl(ByteArray(20)))
    }
}
