package io.github.loki666.soundtrackplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.loki666.soundtrackplayer.library.AlbumWithStatus
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import io.github.loki666.soundtrackplayer.steam.AuthState
import io.github.loki666.soundtrackplayer.ui.components.AlbumArt
import io.github.loki666.soundtrackplayer.ui.components.MiniPlayer
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenAlbum: (Int) -> Unit, onOpenPlayer: () -> Unit) {
    val vm: LibraryViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
    val account = (vm.auth.collectAsStateWithLifecycle().value as? AuthState.SignedIn)?.accountName
    var showAccount by remember { mutableStateOf(false) }
    if (showAccount) {
        LaunchedEffect(Unit) { vm.loadStorage() }
        AccountSheet(
            account = account,
            quality = vm.quality.collectAsStateWithLifecycle().value,
            askEveryTime = vm.askEveryTime.collectAsStateWithLifecycle().value,
            storage = vm.storage.collectAsStateWithLifecycle().value,
            onQuality = vm::setQuality,
            onAskEveryTime = vm::setAskEveryTime,
            onSignOut = { showAccount = false; vm.signOut() },
            onDismiss = { showAccount = false },
        )
    }

    Box(Modifier.fillMaxSize().background(AppColors.Background)) {
        PullToRefreshBox(
            isRefreshing = state.refreshing && !state.loading,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = if (nowPlaying != null) 104.dp else 32.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Header(state, account, onOpenAccount = { showAccount = true })
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchAndFilters(state, vm::setQuery, vm::setFilter)
                }
                state.error?.let { error ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = vm::refresh) { Text("Retry") }
                        }
                    }
                }
                when {
                    state.loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Loading your soundtracks…", color = AppColors.TextSecondary, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                    state.albums.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            when {
                                state.query.isNotBlank() -> "No soundtracks match “${state.query}”."
                                state.filter == LibraryFilter.Downloaded -> "Nothing downloaded yet. Open an album to download it."
                                else -> "No soundtracks on this account yet."
                            },
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        )
                    }
                    else -> items(state.albums, key = { it.album.appId }) { item ->
                        AlbumTile(item, onClick = { onOpenAlbum(item.album.appId) })
                    }
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
private fun Header(state: LibraryUiState, account: String?, onOpenAccount: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Library", style = MaterialTheme.typography.headlineMedium)
            if (!state.loading) {
                Text(
                    "${state.totalCount} soundtracks · ${state.downloadedCount} downloaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
        }
        Surface(
            onClick = onOpenAccount,
            shape = CircleShape,
            color = AppColors.SurfaceRaised,
            modifier = Modifier.size(44.dp).semantics { contentDescription = "Account and settings" },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(account?.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchAndFilters(state: LibraryUiState, onQuery: (String) -> Unit, onFilter: (LibraryFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = { Text("Search soundtracks") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = AppColors.Surface,
                focusedContainerColor = AppColors.Surface,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { onFilter(f) },
                    label = { Text(f.name) },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Accent,
                        selectedLabelColor = AppColors.OnAccent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AlbumTile(item: AlbumWithStatus, onClick: () -> Unit) {
    val status = item.status
    val downloaded = status is DownloadStatus.Downloaded
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                AlbumArt(
                    urls = listOf(item.album.coverUrl, item.album.headerUrl),
                    name = item.album.name,
                    modifier = Modifier.fillMaxSize().alpha(if (downloaded) 1f else 0.55f),
                )
                when (status) {
                    DownloadStatus.NotDownloaded, is DownloadStatus.Failed -> Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(AppColors.Background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Not downloaded", modifier = Modifier.size(16.dp))
                    }
                    is DownloadStatus.Queued -> Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).background(AppColors.Background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = "Waiting to download", modifier = Modifier.size(16.dp))
                    }
                    is DownloadStatus.Downloading -> LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp),
                        trackColor = AppColors.Background.copy(alpha = 0.6f),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                    is DownloadStatus.Downloaded -> Unit
                }
            }
            Column {
                Text(
                    item.album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun subtitle(item: AlbumWithStatus): String = when (val s = item.status) {
    is DownloadStatus.Downloaded -> "${s.local.tracks.size} tracks · ${s.local.format}"
    is DownloadStatus.Downloading -> "Downloading · ${(s.progress * 100).toInt()}%"
    is DownloadStatus.Queued -> "Waiting to download"
    is DownloadStatus.Failed -> "Download failed"
    DownloadStatus.NotDownloaded -> item.album.tracks.size.takeIf { it > 0 }?.let { "$it tracks" } ?: (item.album.artist ?: "Soundtrack")
}
