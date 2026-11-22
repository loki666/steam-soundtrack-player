package io.github.loki666.soundtrackplayer.library

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * On-disk state:
 * - `files/catalog.json`: the last soundtrack list fetched from Steam (so the library opens offline).
 * - `<music dir>/<appId>/`: one folder per downloaded album, holding the depot files, plus
 *   `album.json` once the download has completed and been scanned.
 */
@Singleton
class LibraryStore @Inject constructor(@ApplicationContext context: Context) {
    private val catalogFile = File(context.filesDir, "catalog.json")
    val musicDir: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "albums")

    fun albumDir(appId: Int) = File(musicDir, appId.toString())

    suspend fun loadCatalog(): List<Album> = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<List<Album>>(catalogFile.readText()) }.getOrDefault(emptyList())
    }

    suspend fun saveCatalog(albums: List<Album>) = withContext(Dispatchers.IO) {
        catalogFile.writeText(json.encodeToString(albums))
    }

    suspend fun clearCatalog() = withContext(Dispatchers.IO) { catalogFile.delete() }

    suspend fun loadLocal(appId: Int): LocalAlbum? = withContext(Dispatchers.IO) {
        val file = File(albumDir(appId), LOCAL_FILE)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<LocalAlbum>(file.readText()) }.getOrNull()
            ?.takeIf { local -> local.tracks.isNotEmpty() && local.tracks.all { File(it.path).exists() } }
    }

    suspend fun loadAllLocal(): Map<Int, LocalAlbum> = withContext(Dispatchers.IO) {
        musicDir.listFiles().orEmpty()
            .mapNotNull { it.name.toIntOrNull() }
            .mapNotNull { id -> loadLocal(id)?.let { id to it } }
            .toMap()
    }

    suspend fun saveLocal(local: LocalAlbum) = withContext(Dispatchers.IO) {
        File(albumDir(local.appId), LOCAL_FILE).writeText(json.encodeToString(local))
    }

    suspend fun deleteAlbum(appId: Int) = withContext(Dispatchers.IO) { albumDir(appId).deleteRecursively() }

    private companion object {
        const val LOCAL_FILE = "album.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
