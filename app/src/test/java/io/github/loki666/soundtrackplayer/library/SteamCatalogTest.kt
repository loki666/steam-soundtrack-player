package io.github.loki666.soundtrackplayer.library

import `in`.dragonbra.javasteam.types.KeyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SteamCatalogTest {

    private fun kv(text: String) = requireNotNull(KeyValue.loadFromString(text))

    @Test
    fun `parses a soundtrack with album metadata`() {
        val album = SteamCatalog.parseSoundtrack(
            1234,
            kv(
                """
                "appinfo"
                {
                    "common"
                    {
                        "name"  "Ember Coast Soundtrack"
                        "type"  "Music"
                        "parent" "1000"
                    }
                    "albummetadata"
                    {
                        "metadata" { "artist" { "english" "Saltwind Studio" } }
                        "cdn_assets" { "album_cover" "cover_abc.jpg" }
                        "tracks"
                        {
                            "2" { "discnumber" "1" "tracknumber" "2" "originalname" "Harbor at Dusk" "m" "4" "s" "5" }
                            "1" { "discnumber" "1" "tracknumber" "1" "originalname" "Tidewake" "m" "3" "s" "12" }
                        }
                    }
                    "depots"
                    {
                        "1235" { "manifests" { "public" { "gid" "42" "size" "1000" } } }
                        "1236" { "manifests" { "public" { "gid" "43" "size" "500" } } }
                        "branches" { "public" { "buildid" "1" } }
                    }
                }
                """.trimIndent()
            ),
        )!!

        assertEquals("Ember Coast Soundtrack", album.name)
        assertEquals("Saltwind Studio", album.artist)
        assertEquals(1000, album.parentAppId)
        assertEquals("https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1234/cover_abc.jpg", album.coverUrl)
        assertEquals(listOf("Tidewake", "Harbor at Dusk"), album.tracks.map { it.title })
        assertEquals(192 + 245, album.totalSeconds)
        assertEquals(1500L, album.sizeBytes)
    }

    @Test
    fun `falls back to developer and header when metadata is missing`() {
        val album = SteamCatalog.parseSoundtrack(
            99,
            kv(
                """
                "appinfo"
                {
                    "common"
                    {
                        "name" "Iron Tide OST"
                        "type" "music"
                        "associations" { "0" { "type" "developer" "name" "Tide Works" } }
                    }
                }
                """.trimIndent()
            ),
        )!!

        assertEquals("Tide Works", album.artist)
        assertNull(album.coverUrl)
        assertEquals("https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/99/header.jpg", album.artworkUrl)
        assertNull(album.sizeBytes)
    }

    @Test
    fun `ignores apps that are not soundtracks`() {
        val game = kv(""" "appinfo" { "common" { "name" "Some Game" "type" "Game" } } """)
        assertNull(SteamCatalog.parseSoundtrack(1, game))
    }
}
