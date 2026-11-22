package io.github.loki666.soundtrackplayer.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.loki666.soundtrackplayer.library.DownloadQuality
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How albums that ship several audio formats are downloaded (Account › Downloads). */
@Singleton
class DownloadPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)

    private val _quality = MutableStateFlow(
        prefs.getString(KEY_QUALITY, null)
            ?.let { saved -> DownloadQuality.entries.firstOrNull { it.name == saved } }
            ?: DownloadQuality.Best
    )
    /** Which format to preselect (or use directly, when not asking). */
    val quality: StateFlow<DownloadQuality> = _quality.asStateFlow()

    private val _askEveryTime = MutableStateFlow(prefs.getBoolean(KEY_ASK, true))
    /** Show the format sheet before downloading an album that offers a choice. */
    val askEveryTime: StateFlow<Boolean> = _askEveryTime.asStateFlow()

    fun setQuality(value: DownloadQuality) {
        prefs.edit().putString(KEY_QUALITY, value.name).apply()
        _quality.value = value
    }

    fun setAskEveryTime(value: Boolean) {
        prefs.edit().putBoolean(KEY_ASK, value).apply()
        _askEveryTime.value = value
    }

    private companion object {
        const val KEY_QUALITY = "quality"
        const val KEY_ASK = "ask_every_time"
    }
}
