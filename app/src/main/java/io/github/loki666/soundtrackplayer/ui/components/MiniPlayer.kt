package io.github.loki666.soundtrackplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.loki666.soundtrackplayer.playback.NowPlaying
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

@Composable
fun MiniPlayer(
    nowPlaying: NowPlaying,
    onClick: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.SurfaceRaised,
        shadowElevation = 8.dp,
        onClick = onClick,
    ) {
        Box {
            Row(
                Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AlbumArt(listOf(nowPlaying.artworkUri), nowPlaying.albumName, Modifier.size(48.dp), corner = 10.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        nowPlaying.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        nowPlaying.albumName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (nowPlaying.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (nowPlaying.isPlaying) "Pause" else "Play",
                    )
                }
            }
            val progress = if (nowPlaying.durationMs > 0) nowPlaying.positionMs.toFloat() / nowPlaying.durationMs else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomStart),
                trackColor = Color.Transparent,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
        }
    }
}
