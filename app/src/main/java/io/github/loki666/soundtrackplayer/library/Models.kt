package io.github.loki666.soundtrackplayer.library

import kotlinx.serialization.Serializable

/** A soundtrack the account owns, as described by Steam (available before downloading). */
@Serializable
data class Album(
    val appId: Int,
    val name: String,
    val artist: String?,
    val parentAppId: Int?,
    /** Square cover from the album metadata, when Steam has one. */
    val coverUrl: String?,
    /** Wide store header image; always available. */
    val headerUrl: String,
    val tracks: List<CatalogTrack>,
    /** Approximate download size in bytes, summed over the app's depots. */
    val sizeBytes: Long?,
) {
    val artworkUrl: String get() = coverUrl ?: headerUrl
    val storeUrl: String get() = "https://store.steampowered.com/app/$appId/"
    val totalSeconds: Int get() = tracks.sumOf { it.seconds }
}

@Serializable
data class CatalogTrack(
    val title: String,
    val disc: Int = 1,
    val number: Int,
    val seconds: Int = 0,
)

/** What's actually on the phone for a downloaded album. */
@Serializable
data class LocalAlbum(
    val appId: Int,
    val format: String,
    val coverPath: String?,
    val tracks: List<LocalTrack>,
)

@Serializable
data class LocalTrack(
    val path: String,
    val title: String,
    val disc: Int,
    val number: Int,
    val durationMs: Long,
)

sealed interface DownloadStatus {
    data object NotDownloaded : DownloadStatus
    /** Waiting its turn. [format] is null when the worker will apply the default quality. */
    data class Queued(val format: FormatOption? = null, val waitingFor: String? = null) : DownloadStatus
    data class Downloading(val progress: Float) : DownloadStatus
    data class Failed(val message: String) : DownloadStatus
    data class Downloaded(val local: LocalAlbum) : DownloadStatus
}

data class AlbumWithStatus(val album: Album, val status: DownloadStatus)
