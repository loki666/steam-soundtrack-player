package io.github.loki666.soundtrackplayer.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.loki666.soundtrackplayer.library.LibraryRepository
import io.github.loki666.soundtrackplayer.playback.PlayerConnection
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.steam.SteamSession
import javax.inject.Inject
import io.github.loki666.soundtrackplayer.library.AlbumWithStatus
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import io.github.loki666.soundtrackplayer.playback.NowPlaying
import io.github.loki666.soundtrackplayer.steam.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter { All, Downloaded }

data class LibraryUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.All,
    val albums: List<AlbumWithStatus> = emptyList(),
    val totalCount: Int = 0,
    val downloadedCount: Int = 0,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val player: PlayerConnection,
    private val steam: SteamSession,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(LibraryFilter.All)
    private val refresh = MutableStateFlow(RefreshState())

    private data class RefreshState(val running: Boolean = false, val error: String? = null, val done: Boolean = false)

    val nowPlaying: StateFlow<NowPlaying?> = player.state
    val auth: StateFlow<AuthState> = steam.auth

    val state: StateFlow<LibraryUiState> =
        combine(library.library, query, filter, refresh) { albums, q, f, r ->
            val all = albums.orEmpty()
            val downloaded = all.count { it.status is DownloadStatus.Downloaded }
            val visible = all
                .filter { f == LibraryFilter.All || it.status is DownloadStatus.Downloaded }
                .filter { q.isBlank() || it.album.name.contains(q.trim(), ignoreCase = true) || it.album.artist?.contains(q.trim(), ignoreCase = true) == true }
            LibraryUiState(
                // Show a spinner only on the very first sync, when there's nothing cached yet.
                loading = albums == null || (all.isEmpty() && r.running),
                refreshing = r.running,
                error = r.error,
                query = q,
                filter = f,
                albums = visible,
                totalCount = all.size,
                downloadedCount = downloaded,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (refresh.value.running) return
        refresh.update { it.copy(running = true, error = null) }
        viewModelScope.launch {
            val error = try {
                library.refresh()
                null
            } catch (e: SessionExpiredException) {
                null // The auth state flips to signed-out and navigation takes over.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Library refresh failed", e)
                "Couldn't update your library from Steam."
            }
            refresh.value = RefreshState(running = false, error = error, done = true)
        }
    }

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: LibraryFilter) { filter.value = value }
    fun togglePlay() = player.togglePlayPause()

    fun signOut() {
        viewModelScope.launch {
            library.clearCatalog()
            steam.signOut()
        }
    }

    private companion object {
        const val TAG = "LibraryViewModel"
    }
}
