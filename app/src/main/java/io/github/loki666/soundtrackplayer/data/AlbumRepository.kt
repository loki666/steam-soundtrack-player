package io.github.loki666.soundtrackplayer.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

/** Fetches the user's soundtracks and keeps the last result on disk for instant startup. */
class AlbumRepository(context: Context, private val api: SteamApi = SteamApi()) {
    private val cacheFile = File(context.filesDir, "albums.json")
    private val json = SteamApi.DefaultJson

    suspend fun cached(): List<Album>? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<List<Album>>(cacheFile.readText()) }.getOrNull()
    }

    suspend fun refresh(): List<Album> {
        val albums = api.soundtracks(api.ownedAppIds())
        withContext(Dispatchers.IO) { cacheFile.writeText(json.encodeToString(albums)) }
        return albums
    }

    suspend fun clear() = withContext(Dispatchers.IO) { cacheFile.delete() }
}
