package io.github.loki666.soundtrackplayer.ui.album

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.loki666.soundtrackplayer.library.AlbumWithStatus
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import io.github.loki666.soundtrackplayer.library.FormatOption
import io.github.loki666.soundtrackplayer.ui.components.AlbumArt
import io.github.loki666.soundtrackplayer.ui.components.MiniPlayer
import io.github.loki666.soundtrackplayer.ui.components.formatDuration
import io.github.loki666.soundtrackplayer.ui.components.formatLongDuration
import io.github.loki666.soundtrackplayer.ui.components.formatSize
import io.github.loki666.soundtrackplayer.ui.components.rememberCoverTint
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

private data class TrackRow(val title: String, val length: String)

@Composable
fun AlbumScreen(appId: Int, onBack: () -> Unit, onOpenPlayer: () -> Unit) {
    val vm = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(key = "album-$appId") { it.create(appId) }
    val item by vm.album.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val current = item ?: return Box(Modifier.fillMaxSize().background(AppColors.Background))

    val album = current.album
    val status = current.status
    val local = (status as? DownloadStatus.Downloaded)?.local
    val tint by rememberCoverTint(album.coverUrl ?: album.headerUrl)
    val playingIndex = nowPlaying?.takeIf { it.albumAppId == appId }?.trackIndex

    val rows = local?.tracks?.map { TrackRow(it.title, formatDuration(it.durationMs)) }
        ?: album.tracks.map { TrackRow(it.title, if (it.seconds > 0) formatDuration(it.seconds * 1000L) else "") }

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = if (nowPlaying != null) 104.dp else 32.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Header(current, tint, onBack, vm)
            }
            if (rows.isEmpty()) {
                item {
                    Text(
                        "Steam doesn't list the tracks for this soundtrack. Download it to see them.",
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            itemsIndexed(rows) { index, row ->
                val playing = index == playingIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .background(if (playing) AppColors.Surface else Color.Transparent)
                        .then(if (local != null) Modifier.clickable { vm.play(index) } else Modifier)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
                        if (playing) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = "Now playing", tint = AppColors.Accent, modifier = Modifier.size(18.dp))
                        } else {
                            Text("${index + 1}", color = AppColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        row.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            playing -> AppColors.Accent
                            local == null -> AppColors.TextPrimary.copy(alpha = 0.6f)
                            else -> AppColors.TextPrimary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(row.length, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        nowPlaying?.let { np ->
            MiniPlayer(
                nowPlaying = np,
                onClick = onOpenPlayer,
                onTogglePlay = vm::togglePlay,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
            )
        }
    }
}

@Composable
private fun Header(item: AlbumWithStatus, tint: Color, onBack: () -> Unit, vm: AlbumViewModel) {
    val context = LocalContext.current
    val album = item.album
    val status = item.status
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    val formats by vm.formats.collectAsStateWithLifecycle()
    val quality by vm.quality.collectAsStateWithLifecycle()
    val askEveryTime by vm.askEveryTime.collectAsStateWithLifecycle()
    var chooseFormat by remember { mutableStateOf(false) }

    // The notification permission prompt comes first on Android 13+; remember the format meanwhile.
    var pendingFormat by remember { mutableStateOf<FormatOption?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.download(pendingFormat)
    }
    val startDownload: (FormatOption?) -> Unit = { option ->
        pendingFormat = option
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.download(option)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(tint)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to library") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (status is DownloadStatus.Downloaded) {
                        DropdownMenuItem(text = { Text("Remove from phone") }, onClick = { menu = false; confirmRemove = true })
                    }
                    DropdownMenuItem(
                        text = { Text("Open in Steam store") },
                        onClick = {
                            menu = false
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(album.storeUrl))) }
                        },
                    )
                }
            }
        }

        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            AlbumArt(
                urls = listOf((status as? DownloadStatus.Downloaded)?.local?.coverPath?.let { "file://$it" }, album.coverUrl, album.headerUrl),
                name = album.name,
                modifier = Modifier.size(128.dp),
                corner = 14.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SOUNDTRACK", style = MaterialTheme.typography.labelSmall, color = AppColors.TextPrimary.copy(alpha = 0.75f))
                Text(album.name, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        album.artist,
                        trackCount(item).takeIf { it > 0 }?.let { "$it tracks" },
                        album.totalSeconds.takeIf { it > 0 }?.let(::formatLongDuration),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextPrimary.copy(alpha = 0.75f),
                )
            }
        }

        Box(Modifier.padding(horizontal = 12.dp)) {
            when (status) {
                is DownloadStatus.Downloaded -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.play() }, shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedIconButton(onClick = { vm.play(shuffle = true) }, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle")
                    }
                }
                DownloadStatus.NotDownloaded, is DownloadStatus.Failed -> DownloadSection(
                    formats = formats,
                    failure = (status as? DownloadStatus.Failed)?.message,
                    fallbackSize = album.sizeBytes,
                    quality = quality,
                    askEveryTime = askEveryTime,
                    onDownload = startDownload,
                    onChooseFormat = { chooseFormat = true },
                )
                is DownloadStatus.Queued -> QueuedSection(status, onCancel = vm::cancelDownload)
                is DownloadStatus.Downloading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Downloading · ${(status.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            drawStopIndicator = {},
                            gapSize = 0.dp,
                        )
                    }
                    OutlinedIconButton(onClick = vm::cancelDownload, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel download")
                    }
                }
            }
        }
    }

    val options = (formats as? AlbumFormats.Ready)?.options
    if (chooseFormat && options != null && options.size > 1) {
        FormatSheet(
            options = options,
            trackCount = trackCount(item),
            quality = quality,
            freeBytes = vm.freeBytes(),
            onDismiss = { chooseFormat = false },
            onDownload = { chooseFormat = false; startDownload(it) },
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove from phone?") },
            text = { Text("The files for “${album.name}” will be deleted. You can download it again any time.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; vm.remove() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
}

private fun trackCount(item: AlbumWithStatus): Int =
    (item.status as? DownloadStatus.Downloaded)?.local?.tracks?.size ?: item.album.tracks.size
