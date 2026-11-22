package io.github.loki666.soundtrackplayer.library

import javax.inject.Inject
import javax.inject.Singleton
import io.github.loki666.soundtrackplayer.di.ApplicationScope
import androidx.work.WorkInfo
import io.github.loki666.soundtrackplayer.download.DownloadScheduler
import io.github.loki666.soundtrackplayer.download.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for the library screen: the Steam catalog, merged with what's on the
 * phone and with in-flight downloads.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val store: LibraryStore,
    private val catalog: SteamCatalog,
    private val downloads: DownloadScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val albums = MutableStateFlow<List<Album>?>(null)
    private val local = MutableStateFlow<Map<Int, LocalAlbum>>(emptyMap())

    init {
        scope.launch {
            albums.value = store.loadCatalog()
            local.value = store.loadAllLocal()
        }
    }

    /** Null until the on-disk cache has been read. */
    val library: StateFlow<List<AlbumWithStatus>?> =
        combine(albums, local, downloads.active.onStart { emit(emptyMap()) }) { albums, local, work ->
            albums?.map { AlbumWithStatus(it, statusOf(it.appId, local, work)) }
        }.stateIn(scope, SharingStarted.Eagerly, null)

    fun album(appId: Int): Flow<AlbumWithStatus?> = library.map { list -> list?.firstOrNull { it.album.appId == appId } }

    fun albumName(appId: Int): String? = albums.value?.firstOrNull { it.appId == appId }?.name

    /** Re-fetches the soundtrack list from Steam. Throws on network / session errors. */
    suspend fun refresh() {
        val fresh = catalog.fetchSoundtracks()
        store.saveCatalog(fresh)
        albums.value = fresh
    }

    fun download(appId: Int) = downloads.download(appId)

    fun cancelDownload(appId: Int) = downloads.cancel(appId)

    suspend fun removeDownload(appId: Int) {
        downloads.cancel(appId)
        store.deleteAlbum(appId)
        local.update { it - appId }
    }

    suspend fun onDownloadFinished(appId: Int) {
        store.loadLocal(appId)?.let { found -> local.update { it + (appId to found) } }
    }

    /** Forgets the account's catalog; downloaded files stay so signing back in doesn't re-download. */
    suspend fun clearCatalog() {
        store.clearCatalog()
        albums.value = emptyList()
    }

    private fun statusOf(appId: Int, local: Map<Int, LocalAlbum>, work: Map<Int, WorkInfo>): DownloadStatus {
        local[appId]?.let { return DownloadStatus.Downloaded(it) }
        val info = work[appId] ?: return DownloadStatus.NotDownloaded
        return when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadStatus.Queued
            WorkInfo.State.RUNNING ->
                if (info.progress.getBoolean(DownloadWorker.KEY_WAITING, false)) DownloadStatus.Queued
                else DownloadStatus.Downloading(info.progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f))
            WorkInfo.State.FAILED -> DownloadStatus.Failed(
                info.outputData.getString(DownloadWorker.KEY_ERROR) ?: "Download failed."
            )
            // Succeeded but not yet rescanned, or cancelled.
            WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED -> DownloadStatus.NotDownloaded
        }
    }
}
