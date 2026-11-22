package io.github.loki666.soundtrackplayer.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.loki666.soundtrackplayer.library.DownloadQuality
import io.github.loki666.soundtrackplayer.library.DownloadStatus
import io.github.loki666.soundtrackplayer.library.FormatOption
import io.github.loki666.soundtrackplayer.library.FormatOptions
import io.github.loki666.soundtrackplayer.ui.components.formatSize
import io.github.loki666.soundtrackplayer.ui.theme.AppColors
import io.github.loki666.soundtrackplayer.ui.theme.AppFonts
import kotlin.math.roundToInt

private val SubtleText = AppColors.TextPrimary.copy(alpha = 0.75f)
private const val MIN_RATIO_TO_SHOW = 2

/**
 * The "Download album" action for an album that isn't on the phone yet (or failed). With one
 * format it downloads straight away; with several it either opens the format sheet or, when the
 * user turned "Ask before each download" off, uses their default and offers "Change format".
 */
@Composable
internal fun DownloadSection(
    formats: AlbumFormats,
    failure: String?,
    fallbackSize: Long?,
    quality: DownloadQuality,
    askEveryTime: Boolean,
    onDownload: (FormatOption?) -> Unit,
    onChooseFormat: () -> Unit,
) {
    val options = (formats as? AlbumFormats.Ready)?.options.orEmpty()
    val several = options.size > 1
    val preferred = FormatOptions.preferred(options, quality)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = { if (several && askEveryTime) onChooseFormat() else onDownload(preferred) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (failure != null) "Retry download" else "Download album", style = MaterialTheme.typography.titleMedium)
        }
        when {
            failure != null -> Text(
                failure,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            several && !askEveryTime && preferred != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                val which = if (quality == DownloadQuality.Best) "best quality" else "smallest"
                Text("${preferred.label} · ${formatSize(preferred.bytes)} · $which", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                TextButton(onClick = onChooseFormat) { Text("Change format", fontWeight = FontWeight.SemiBold) }
            }
            else -> Text(
                availabilityLine(options, fallbackSize),
                style = MaterialTheme.typography.bodySmall,
                color = SubtleText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun availabilityLine(options: List<FormatOption>, fallbackSize: Long?): String {
    if (options.size <= 1) {
        val single = options.singleOrNull()
        return listOfNotNull(
            "Download to play",
            single?.label,
            single?.let { formatSize(it.bytes) } ?: fallbackSize?.let { "up to ${formatSize(it)}" },
        ).joinToString(" · ")
    }
    val smallest = options.minBy { it.bytes }
    val largest = options.maxBy { it.bytes }
    return if (options.size == 2) {
        "Available as ${smallest.label} · ${formatSize(smallest.bytes)} or ${largest.label} · ${formatSize(largest.bytes)}"
    } else {
        "${options.size} formats · from ${formatSize(smallest.bytes)} (${smallest.label}) to ${formatSize(largest.bytes)} (${largest.label})"
    }
}

/** "Waiting to download": shown while another album downloads first. */
@Composable
internal fun QueuedSection(status: DownloadStatus.Queued, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Waiting to download", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedIconButton(onClick = onCancel, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel download")
            }
        }
        val details = listOfNotNull(
            status.format?.label,
            status.format?.let { formatSize(it.bytes) },
            status.waitingFor?.let { "starts after $it" },
        )
        if (details.isNotEmpty()) {
            Text(
                details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = SubtleText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Bottom sheet listing each format with its size, preselected from the user's default. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormatSheet(
    options: List<FormatOption>,
    trackCount: Int,
    quality: DownloadQuality,
    freeBytes: Long,
    onDismiss: () -> Unit,
    onDownload: (FormatOption) -> Unit,
) {
    val preselected = FormatOptions.preferred(options, quality) ?: return
    var picked by remember(options) { mutableStateOf(preselected) }
    val best = options.first()
    val smallest = options.minBy { it.bytes }
    val largest = options.maxBy { it.bytes }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Choose a format",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = AppFonts.Heading,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "The same ${if (trackCount > 0) "$trackCount " else ""}tracks in each. Only the file size and sound quality change.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }

            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    FormatRow(
                        option = option,
                        selected = option == picked,
                        isBest = option == best && option.lossless,
                        isSmallest = option == smallest,
                        fraction = option.bytes.toFloat() / largest.bytes,
                        detail = detailFor(option, smallest, largest),
                        onSelect = { picked = option },
                    )
                }
            }

            Button(onClick = { onDownload(picked) }, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download ${picked.label} · ${formatSize(picked.bytes)}", style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${formatSize(freeBytes)} free on this device", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Text(
                    "Preselected from your default: ${if (quality == DownloadQuality.Best) "Best quality" else "Smaller files"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun FormatRow(
    option: FormatOption,
    selected: Boolean,
    isBest: Boolean,
    isSmallest: Boolean,
    fraction: Float,
    detail: String,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .border(if (selected) 2.dp else 1.dp, if (selected) AppColors.Accent else AppColors.Outline, shape)
            .background(if (selected) AppColors.SurfaceRaised else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Accent),
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(option.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (isBest) Badge("Best quality", AppColors.Accent.copy(alpha = 0.16f), AppColors.Accent)
                if (isSmallest) Badge("Smallest", AppColors.Outline, AppColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(formatSize(option.bytes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.fillMaxWidth().height(6.dp).background(AppColors.Background, CircleShape)) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                        .height(6.dp)
                        .background(if (selected) AppColors.Accent else AppColors.TextMuted, CircleShape)
                )
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun Badge(text: String, background: Color, content: Color) {
    Box(
        Modifier.heightIn(min = 22.dp).background(background, RoundedCornerShape(11.dp)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content)
    }
}

private fun detailFor(option: FormatOption, smallest: FormatOption, largest: FormatOption): String {
    if (!option.lossless) {
        val ratio = (largest.bytes.toDouble() / option.bytes).roundToInt()
        return "Compressed (lossy)" + if (ratio >= MIN_RATIO_TO_SHOW) " · ${ratio}× smaller than ${largest.label}" else ""
    }
    val kind = if (option.format == "flac") "Lossless, compressed" else "Lossless, uncompressed"
    val ratio = (option.bytes.toDouble() / smallest.bytes).roundToInt()
    return kind + if (ratio >= MIN_RATIO_TO_SHOW) " · ${ratio}× the size of ${smallest.label}" else ""
}

/** What the album screen knows about an album's formats before it is downloaded. */
internal sealed interface AlbumFormats {
    data object Loading : AlbumFormats
    data class Ready(val options: List<FormatOption>) : AlbumFormats
    data object Unavailable : AlbumFormats
}
