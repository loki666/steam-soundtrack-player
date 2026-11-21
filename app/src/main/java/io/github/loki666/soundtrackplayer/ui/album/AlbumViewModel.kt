package io.github.loki666.soundtrackplayer.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.loki666.soundtrackplayer.AppContainer
import io.github.loki666.soundtrackplayer.library.AlbumWithStatus
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import io.github.loki666.soundtrackplayer.playback.NowPlaying
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumViewModel(private val app: AppContainer, val appId: Int) : ViewModel() {
    val album: StateFlow<AlbumWithStatus?> =
        app.library.album(appId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val nowPlaying: StateFlow<NowPlaying?> = app.player.state

    fun play(startIndex: Int = 0, shuffle: Boolean = false) {
        val item = album.value ?: return
        val local = (item.status as? DownloadStatus.Downloaded)?.local ?: return
        app.player.playAlbum(item.album, local, startIndex, shuffle)
    }

    fun togglePlay() = app.player.togglePlayPause()
    fun download() = app.library.download(appId)
    fun cancelDownload() = app.library.cancelDownload(appId)

    fun remove() {
        app.player.stopIfPlaying(appId)
        viewModelScope.launch { app.library.removeDownload(appId) }
    }
}
