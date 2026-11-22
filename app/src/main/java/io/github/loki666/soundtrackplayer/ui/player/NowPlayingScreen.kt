package io.github.loki666.soundtrackplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import io.github.loki666.soundtrackplayer.container
import io.github.loki666.soundtrackplayer.ui.components.AlbumArt
import io.github.loki666.soundtrackplayer.ui.components.formatDuration
import io.github.loki666.soundtrackplayer.ui.components.rememberCoverTint
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

@Composable
fun NowPlayingScreen(onClose: () -> Unit, onOpenAlbum: (Int) -> Unit) {
    val player = LocalContext.current.container.player
    val state = player.state.collectAsStateWithLifecycle().value

    // Playback ended or was cleared while this screen was open.
    LaunchedEffect(state == null) { if (state == null) onClose() }
    state ?: return

    val tint by rememberCoverTint(state.artworkUri)
    val secondary = Color.White.copy(alpha = 0.72f)

    Column(
        Modifier
            .fillMaxSize()
            .background(tint)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close player") }
            Column(
                Modifier.weight(1f).clickable { onOpenAlbum(state.albumAppId) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("PLAYING FROM ALBUM", style = MaterialTheme.typography.labelSmall, color = secondary)
                Text(state.albumName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.weight(0.6f))
        AlbumArt(
            urls = listOf(state.artworkUri),
            name = state.albumName,
            corner = 20.dp,
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp).aspectRatio(1f).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.weight(0.6f))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull("Track ${state.trackIndex + 1} of ${state.trackCount}", state.artist).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            state.format?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary,
                    modifier = Modifier.border(1.dp, secondary.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // Seek bar: follow the player unless the user is dragging.
        var dragging by remember { mutableStateOf<Float?>(null) }
        val duration = state.durationMs.coerceAtLeast(1L)
        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = AppColors.Accent,
            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
        )
        // Only the SliderState overload takes a custom track (needed to drop the end-of-track dot).
        val sliderState = remember { SliderState() }
        val playerFraction = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
        LaunchedEffect(playerFraction, dragging) {
            if (dragging == null) sliderState.value = playerFraction
        }
        Slider(
            state = sliderState,
            onValueChange = {
                sliderState.value = it
                dragging = it
            },
            onValueChangeFinished = {
                dragging?.let { player.seekTo((it * duration).toLong()) }
                dragging = null
            },
            colors = sliderColors,
            track = {
                // No Material 3 end-of-track dot: the design's seek bar is a plain track.
                SliderDefaults.Track(sliderState = it, colors = sliderColors, drawStopIndicator = null)
            },
            modifier = Modifier.padding(top = 16.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragging?.let { (it * duration).toLong() } ?: state.positionMs
            Text(formatDuration(shown), style = MaterialTheme.typography.bodySmall, color = secondary)
            Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall, color = secondary)
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = player::toggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffle) AppColors.Accent else secondary,
                )
            }
            IconButton(onClick = player::previous, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track", modifier = Modifier.size(34.dp))
            }
            FilledIconButton(
                onClick = player::togglePlayPause,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
            IconButton(onClick = player::next, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = player::cycleRepeat) {
                Icon(
                    if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ALL -> "Repeat album"
                        Player.REPEAT_MODE_ONE -> "Repeat track"
                        else -> "Repeat off"
                    },
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) AppColors.Accent else secondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
