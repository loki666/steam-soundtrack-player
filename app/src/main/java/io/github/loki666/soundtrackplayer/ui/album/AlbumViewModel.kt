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
import io.github.loki666.soundtrackplayer.playback.NowPlaying
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(
    private val library: LibraryRepository,
    private val player: PlayerConnection,
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
    fun download() = library.download(appId)
    fun cancelDownload() = library.cancelDownload(appId)

    fun remove() {
        player.stopIfPlaying(appId)
        viewModelScope.launch { library.removeDownload(appId) }
    }
}
