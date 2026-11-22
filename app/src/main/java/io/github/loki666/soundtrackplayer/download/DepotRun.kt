package io.github.loki666.soundtrackplayer.download

import io.github.loki666.soundtrackplayer.steam.SteamSession
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.future.await

private const val PROGRESS_INTERVAL_MS = 500L

/**
 * Runs one JavaSteam DepotDownloader pass over [appId]'s depots into [dir] and waits for it.
 * [manifestOnly] fetches just the manifests (and writes their `manifest_*.txt` dumps); otherwise
 * files are downloaded, limited to [filter] when given.
 */
internal suspend fun runDepotDownloader(
    steam: SteamSession,
    licenses: List<License>,
    appId: Int,
    dir: File,
    manifestOnly: Boolean,
    filter: List<Regex>? = null,
    onProgress: ((Float) -> Unit)? = null,
) {
    val depotProgress = ConcurrentHashMap<Int, Float>()
    var failure: Throwable? = null
    var lastReported = 0L
    DepotDownloader(steam.client, licenses, maxDownloads = 8).use { downloader ->
        if (filter != null) DepotFiles.enableFileFilter(downloader, filter)
        downloader.addListener(object : IDownloadListener {
            override fun onChunkCompleted(depotId: Int, depotPercentComplete: Float, compressedBytes: Long, uncompressedBytes: Long) {
                onProgress ?: return
                depotProgress[depotId] = depotPercentComplete
                val now = System.currentTimeMillis()
                if (now - lastReported > PROGRESS_INTERVAL_MS) {
                    lastReported = now
                    onProgress(depotProgress.values.average().toFloat().coerceIn(0f, 1f))
                }
            }

            override fun onDownloadFailed(item: DownloadItem, error: Throwable) {
                failure = error
            }
        })
        downloader.add(
            AppItem(
                appId = appId,
                installDirectory = dir.absolutePath,
                // Soundtrack depots are often tagged for a desktop OS; we want them all.
                downloadAllPlatforms = true,
                downloadAllLanguages = true,
                downloadManifestOnly = manifestOnly,
            )
        )
        downloader.finishAdding()
        downloader.getCompletion().await()
    }
    failure?.let { throw it }
}
