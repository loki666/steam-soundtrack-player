package io.github.loki666.soundtrackplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

/**
 * The account's Steam avatar in a circle. The initial is drawn underneath, so it shows while the
 * image loads and stays when there is no avatar or it fails to load.
 */
@Composable
fun AccountAvatar(
    avatarUrl: String?,
    account: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    letterSize: TextUnit = 15.sp,
) {
    Box(
        modifier.size(size).clip(CircleShape).background(AppColors.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Text(account?.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.SemiBold, fontSize = letterSize)
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
