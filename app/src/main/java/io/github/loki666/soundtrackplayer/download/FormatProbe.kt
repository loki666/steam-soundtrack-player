package io.github.loki666.soundtrackplayer.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.loki666.soundtrackplayer.library.FormatOption
import io.github.loki666.soundtrackplayer.library.FormatOptions
import io.github.loki666.soundtrackplayer.steam.SteamSession
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Finds which audio formats an album can be downloaded in, and how big each is, before anything
 * is downloaded: a manifest-only pass lists every file with its size. Results are kept for the
 * life of the process.
 */
@Singleton
class FormatProbe @Inject constructor(
    @ApplicationContext context: Context,
    private val steam: SteamSession,
) {
    private val root = File(context.cacheDir, "format-probe")
    private val cache = ConcurrentHashMap<Int, List<FormatOption>>()
    private val lock = Mutex()

    suspend fun formats(appId: Int): List<FormatOption> {
        cache[appId]?.let { return it }
        return lock.withLock {
            cache[appId] ?: withContext(Dispatchers.IO) { probe(appId) }.also { cache[appId] = it }
        }
    }

    private suspend fun probe(appId: Int): List<FormatOption> {
        val dir = File(root, appId.toString())
        dir.deleteRecursively()
        dir.mkdirs()
        try {
            runDepotDownloader(steam, steam.licenses(), appId, dir, manifestOnly = true)
            return FormatOptions.from(DepotFiles.readManifestDumps(dir))
        } finally {
            dir.deleteRecursively()
        }
    }
}
