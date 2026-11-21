package io.github.loki666.soundtrackplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.loki666.soundtrackplayer.library.Album
import io.github.loki666.soundtrackplayer.library.LocalAlbum
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class NowPlaying(
    val albumAppId: Int,
    val albumName: String,
    val artist: String?,
    val title: String,
    val artworkUri: String?,
    val trackIndex: Int,
    val trackCount: Int,
    val format: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

/** App-wide handle on the playback service, exposed as state for the UI. */
class PlayerConnection(private val context: Context, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private var controller: MediaController? = null
    private val lock = Mutex()
    private var ticker: Job? = null

    init {
        // Reconnect at startup so the mini player reflects a session that outlived the activity.
        scope.launch { runCatching { controller() } }
    }

    private suspend fun controller(): MediaController = lock.withLock {
        controller?.takeIf { it.isConnected }?.let { return it }
        withContext(Dispatchers.Main) {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, token).buildAsync().await().also { c ->
                c.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = publish(player)
                })
                controller = c
                publish(c)
            }
        }
    }

    fun playAlbum(album: Album, local: LocalAlbum, startIndex: Int = 0, shuffle: Boolean = false) {
        scope.launch {
            val c = controller()
            withContext(Dispatchers.Main) {
                val artwork = local.coverPath?.let { Uri.fromFile(File(it)) } ?: Uri.parse(album.artworkUrl)
                val items = local.tracks.mapIndexed { i, track ->
                    MediaItem.Builder()
                        .setMediaId("${album.appId}/$i")
                        .setUri(Uri.fromFile(File(track.path)))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(album.artist)
                                .setAlbumTitle(album.name)
                                .setArtworkUri(artwork)
                                .setTrackNumber(i + 1)
                                .setTotalTrackCount(local.tracks.size)
                                .setExtras(bundleOf(EXTRA_APP_ID to album.appId, EXTRA_FORMAT to local.format))
                                .build()
                        )
                        .build()
                }
                c.shuffleModeEnabled = shuffle
                c.setMediaItems(items, if (shuffle) (items.indices).random() else startIndex, 0L)
                c.prepare()
                c.play()
            }
        }
    }

    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Stops playback if it's playing this album (e.g. before deleting its files). */
    fun stopIfPlaying(appId: Int) = withController {
        if (_state.value?.albumAppId == appId) {
            it.stop()
            it.clearMediaItems()
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch {
            val c = controller()
            withContext(Dispatchers.Main) { block(c) }
        }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        if (item == null) {
            _state.value = null
            ticker?.cancel()
            return
        }
        val meta = item.mediaMetadata
        _state.value = NowPlaying(
            albumAppId = meta.extras?.getInt(EXTRA_APP_ID) ?: item.mediaId.substringBefore('/').toIntOrNull() ?: 0,
            albumName = meta.albumTitle?.toString().orEmpty(),
            artist = meta.artist?.toString(),
            title = meta.title?.toString().orEmpty(),
            artworkUri = meta.artworkUri?.toString(),
            trackIndex = player.currentMediaItemIndex,
            trackCount = player.mediaItemCount,
            format = meta.extras?.getString(EXTRA_FORMAT),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
        if (player.isPlaying) startTicker(player) else ticker?.cancel()
    }

    private fun startTicker(player: Player) {
        if (ticker?.isActive == true) return
        ticker = scope.launch(Dispatchers.Main) {
            while (isActive) {
                _state.value = _state.value?.copy(
                    positionMs = player.currentPosition,
                    durationMs = player.duration.coerceAtLeast(0L),
                )
                delay(500)
            }
        }
    }

    private companion object {
        const val EXTRA_APP_ID = "appId"
        const val EXTRA_FORMAT = "format"
    }
}
