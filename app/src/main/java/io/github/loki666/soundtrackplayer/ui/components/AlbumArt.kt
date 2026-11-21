package io.github.loki666.soundtrackplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

/**
 * Square album art. Tries each url in order (album cover, then the wide store header, cropped),
 * and falls back to a tile with the album name if none load.
 */
@Composable
fun AlbumArt(
    urls: List<String?>,
    name: String,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    val candidates = remember(urls) { urls.filterNotNull().distinct() }
    var index by remember(candidates) { mutableStateOf(0) }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(AppColors.SurfaceRaised),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (index < candidates.size) {
            AsyncImage(
                model = candidates[index],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { index++ },
            )
        } else {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
