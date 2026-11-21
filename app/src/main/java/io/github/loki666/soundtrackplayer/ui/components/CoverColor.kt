package io.github.loki666.soundtrackplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.loki666.soundtrackplayer.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A dark background tint taken from the album art, so each album and the player get their own
 * colour (as in the designs). Falls back to the app background.
 */
@Composable
fun rememberCoverTint(url: String?): State<Color> {
    val context = LocalContext.current
    var target by remember { mutableStateOf(AppColors.Background) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(128).build()
        val bitmap = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            ?: return@LaunchedEffect
        val swatch = withContext(Dispatchers.Default) {
            Palette.from(bitmap).generate().let { it.darkVibrantSwatch ?: it.darkMutedSwatch ?: it.dominantSwatch }
        } ?: return@LaunchedEffect
        // Keep it dark enough for light text to stay readable.
        target = lerp(Color(swatch.rgb), Color.Black, 0.55f)
    }
    return animateColorAsState(target, label = "coverTint")
}
