package io.github.loki666.soundtrackplayer.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.loki666.soundtrackplayer.R
import androidx.compose.ui.unit.sp

object AppColors {
    val Background = Color(0xFF0E141B)
    val Surface = Color(0xFF17202A)
    val SurfaceRaised = Color(0xFF1D2935)
    val Outline = Color(0xFF2A3846)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF9FB0C2)
    val TextMuted = Color(0xFF8496A9)
    val Accent = Color(0xFF5FB4E8)
    val OnAccent = Color(0xFF0E141B)
    val Success = Color(0xFF8FD694)
}

private val colors = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.OnAccent,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Background,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Surface,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.SurfaceRaised,
    surfaceContainerHighest = AppColors.SurfaceRaised,
    outline = AppColors.Outline,
    outlineVariant = AppColors.Outline,
    error = Color(0xFFFF8A80),
)

/** The design's pairing: Space Grotesk for titles and cover text, Inter for everything else. */
object AppFonts {
    val Heading = FontFamily(
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )
    val Body = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )
}

private val base = Typography()
private fun TextStyle.body() = copy(fontFamily = AppFonts.Body)
private fun TextStyle.heading() = copy(fontFamily = AppFonts.Heading)
private val typography = Typography(
    displayLarge = base.displayLarge.heading(),
    displayMedium = base.displayMedium.heading(),
    displaySmall = base.displaySmall.heading(),
    headlineLarge = base.headlineLarge.heading().copy(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = base.headlineMedium.heading().copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = base.headlineSmall.heading().copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = base.titleLarge.body(),
    titleMedium = base.titleMedium.body(),
    titleSmall = base.titleSmall.body(),
    bodyLarge = base.bodyLarge.body(),
    bodyMedium = base.bodyMedium.body(),
    bodySmall = base.bodySmall.body(),
    labelLarge = base.labelLarge.body(),
    labelMedium = base.labelMedium.body(),
    labelSmall = TextStyle(fontFamily = AppFonts.Body, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
)

@Composable
fun SoundtrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography) {
        // Screens draw their own backgrounds instead of sitting in a Surface, so nothing else sets the
        // content color: without this, uncolored Text falls back to black. Designs use TextPrimary.
        CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
    }
}
