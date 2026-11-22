package io.github.loki666.soundtrackplayer.ui.library

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.loki666.soundtrackplayer.library.DownloadQuality
import io.github.loki666.soundtrackplayer.ui.components.formatSize
import io.github.loki666.soundtrackplayer.ui.theme.AppColors

/** Downloaded size and free space, shown in the account sheet. */
data class StorageInfo(val downloadedBytes: Long, val freeBytes: Long)

private data class QualityChoice(val quality: DownloadQuality, val title: String, val description: String)

private val QUALITY_CHOICES = listOf(
    QualityChoice(DownloadQuality.Best, "Best quality", "Lossless when offered: FLAC first, then WAV. Largest downloads."),
    QualityChoice(DownloadQuality.Smaller, "Smaller files", "The smallest format offered, usually MP3. Often a fraction of the size."),
)

/** Opened from the avatar: the account, default download quality, storage and sign out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSheet(
    account: String?,
    quality: DownloadQuality,
    askEveryTime: Boolean,
    storage: StorageInfo?,
    onQuality: (DownloadQuality) -> Unit,
    onAskEveryTime: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(48.dp).background(AppColors.SurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                    Text(account?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        account ?: "Steam account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("Signed in to Steam", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            }

            HorizontalDivider(color = AppColors.Outline)

            Text(
                "DOWNLOADS",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextMuted,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Default quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Used when an album comes in more than one format.", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QUALITY_CHOICES.forEach { choice ->
                        QualityRow(choice, selected = choice.quality == quality, onSelect = { onQuality(choice.quality) })
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(value = askEveryTime, role = Role.Switch, onValueChange = onAskEveryTime),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ask before each download", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Show formats and sizes for albums that offer a choice", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                Switch(
                    checked = askEveryTime,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Accent, checkedThumbColor = AppColors.OnAccent),
                )
            }

            HorizontalDivider(color = AppColors.Outline)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    storage?.let { "${formatSize(it.downloadedBytes)} downloaded · ${formatSize(it.freeBytes)} free" } ?: "…",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }

            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Sign out", style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun QualityRow(choice: QualityChoice, selected: Boolean, onSelect: () -> Unit) {
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
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(choice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(choice.description, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}
