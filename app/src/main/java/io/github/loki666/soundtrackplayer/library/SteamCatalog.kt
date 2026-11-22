package io.github.loki666.soundtrackplayer.library

import io.github.loki666.soundtrackplayer.steam.SteamSession
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.types.KeyValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the account's soundtracks over the Steam client protocol (PICS):
 * licenses → packages → app ids → app info, keeping apps whose type is "Music".
 */
class SteamCatalog(private val steam: SteamSession) {

    /** Main-safe: JavaSteam writes PICS requests to the socket on the calling thread. */
    suspend fun fetchSoundtracks(): List<Album> = withContext(Dispatchers.IO) {
        val licenses = steam.licenses()

        val packageRequests = licenses.map { PICSRequest(it.packageID, it.accessToken) }
        val appIds = packageRequests.chunked(BATCH)
            .flatMap { batch -> productInfo(apps = emptyList(), packages = batch).flatMap { it.packages.values } }
            .flatMap { pkg -> pkg.keyValues["appids"].children.map { it.asInteger() } }
            .filter { it > 0 }
            .distinct()

        val tokens = appIds.chunked(BATCH).flatMap { batch ->
            steam.apps.picsGetAccessTokens(batch, emptyList()).await().appTokens.entries
        }.associate { it.key to it.value }

        appIds.chunked(BATCH)
            .flatMap { batch ->
                productInfo(apps = batch.map { PICSRequest(it, tokens[it] ?: 0L) }, packages = emptyList())
                    .flatMap { it.apps.values }
            }
            .mapNotNull { parseSoundtrack(it.id, it.keyValues) }
            .sortedBy { it.name.lowercase() }
    }

    private suspend fun productInfo(apps: List<PICSRequest>, packages: List<PICSRequest>) =
        steam.apps.picsGetProductInfo(apps, packages).await().results

    companion object {
        private const val BATCH = 200
        private const val ASSETS = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps"
        // Album covers are community assets, not store assets: PICS gives a bare SHA-1 file name.
        private const val COMMUNITY_ASSETS = "https://shared.akamai.steamstatic.com/community_assets/images/apps"

        private fun coverUrl(appId: Int, name: String): String {
            val file = if ('.' in name) name else "$name.jpg"
            return "$COMMUNITY_ASSETS/$appId/$file"
        }

        /** Builds an [Album] from a PICS app info tree, or returns null if the app isn't a soundtrack. */
        fun parseSoundtrack(id: Int, kv: KeyValue): Album? {
            val common = kv["common"]
            if (!common["type"].value.equals("music", ignoreCase = true)) return null
            val meta = kv["albummetadata"]

            val artist = meta["metadata"]["artist"].localized()
                ?: common["associations"].children
                    .firstOrNull { it["type"].value == "developer" }?.get("name")?.value
                ?: meta["metadata"]["composer"].localized()

            val tracks = meta["tracks"].children.mapNotNull { t ->
                val title = t["originalname"].value ?: return@mapNotNull null
                CatalogTrack(
                    title = title,
                    disc = t["discnumber"].asInteger(1),
                    number = t["tracknumber"].asInteger(t.name?.toIntOrNull() ?: 0),
                    seconds = t["m"].asInteger() * 60 + t["s"].asInteger(),
                )
            }.sortedWith(compareBy({ it.disc }, { it.number }))

            val cover = meta["cdn_assets"]["album_cover"].value?.takeIf { it.isNotBlank() }

            val size = kv["depots"].children
                .filter { it.name?.toIntOrNull() != null }
                .sumOf { depot -> depot["manifests"]["public"]["size"].asLong() }
                .takeIf { it > 0 }

            return Album(
                appId = id,
                name = common["name"].value ?: "Soundtrack $id",
                artist = artist,
                parentAppId = common["parent"].asInteger().takeIf { it > 0 },
                coverUrl = cover?.let { coverUrl(id, it) },
                headerUrl = "$ASSETS/$id/header.jpg",
                tracks = tracks,
                sizeBytes = size,
            )
        }

        /** Album metadata strings are keyed by language. */
        private fun KeyValue.localized(): String? =
            (this["english"].value ?: value ?: children.firstOrNull()?.value)?.takeIf { it.isNotBlank() }
    }
}
