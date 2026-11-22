package io.github.loki666.soundtrackplayer.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.loki666.soundtrackplayer.library.LibraryRepository
import io.github.loki666.soundtrackplayer.playback.PlayerConnection
import io.github.loki666.soundtrackplayer.library.AlbumWithStatus
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import android.util.Log
import io.github.loki666.soundtrackplayer.download.FormatProbe
import io.github.loki666.soundtrackplayer.library.DownloadQuality
import io.github.loki666.soundtrackplayer.library.FormatOption
import io.github.loki666.soundtrackplayer.library.LibraryStore
import io.github.loki666.soundtrackplayer.playback.NowPlaying
import io.github.loki666.soundtrackplayer.settings.DownloadPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    private val library: LibraryRepository,
    private val player: PlayerConnection,
    private val probe: FormatProbe,
    private val preferences: DownloadPreferences,
    private val store: LibraryStore,
    @Assisted val appId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(appId: Int): AlbumViewModel
    }

    val album: StateFlow<AlbumWithStatus?> =
        library.album(appId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val nowPlaying: StateFlow<NowPlaying?> = player.state

    fun play(startIndex: Int = 0, shuffle: Boolean = false) {
        val item = album.value ?: return
        val local = (item.status as? DownloadStatus.Downloaded)?.local ?: return
        player.playAlbum(item.album, local, startIndex, shuffle)
    }

    fun togglePlay() = player.togglePlayPause()
    private val _formats = MutableStateFlow<AlbumFormats>(AlbumFormats.Loading)
    /** Formats and sizes this album can be downloaded in; probed only while it isn't downloaded. */
    internal val formats: StateFlow<AlbumFormats> = _formats.asStateFlow()
    val quality: StateFlow<DownloadQuality> = preferences.quality
    val askEveryTime: StateFlow<Boolean> = preferences.askEveryTime

    init {
        viewModelScope.launch {
            if (album.filterNotNull().first().status is DownloadStatus.Downloaded) return@launch
            _formats.value = try {
                AlbumFormats.Ready(probe.formats(appId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't list formats for $appId", e)
                AlbumFormats.Unavailable
            }
        }
    }

    /** [option] null lets the download use the default quality. */
    fun download(option: FormatOption?) = library.download(appId, option)

    fun freeBytes(): Long = store.freeBytes()
    fun cancelDownload() = library.cancelDownload(appId)

    fun remove() {
        player.stopIfPlaying(appId)
        viewModelScope.launch { library.removeDownload(appId) }
    }

    private companion object {
        const val TAG = "AlbumViewModel"
    }
}
