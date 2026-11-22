package io.github.loki666.soundtrackplayer.download

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.loki666.soundtrackplayer.library.FormatOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DownloadScheduler @Inject constructor(@ApplicationContext context: Context) {
    private val work = WorkManager.getInstance(context)

    /** Queues [appId]. [option] is the format the user picked; null lets the worker apply their default. */
    fun download(appId: Int, option: FormatOption?) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_APP_ID to appId, DownloadWorker.KEY_FORMAT to option?.format))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .addTag(tagFor(appId))
            .apply { option?.let { addTag("$FORMAT_TAG_PREFIX${it.format}:${it.bytes}") } }
            .build()
        work.enqueueUniqueWork(nameFor(appId), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(appId: Int) {
        work.cancelUniqueWork(nameFor(appId))
    }

    /** Latest work state per app id, for downloads that are queued, running or just failed. */
    val active: Flow<Map<Int, WorkInfo>> = work.getWorkInfosByTagFlow(TAG).map { infos ->
        infos.mapNotNull { info ->
            val id = info.tags.firstNotNullOfOrNull { it.removePrefix(APP_TAG_PREFIX).takeIf { s -> s != it }?.toIntOrNull() }
            id?.let { it to info }
        }
            // Several runs can exist for one album (e.g. a failure then a retry); keep the unfinished one.
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.firstOrNull { !it.state.isFinished } ?: list.last() }
    }

    private companion object {
        const val TAG = "album-download"
        const val APP_TAG_PREFIX = "app:"
        const val FORMAT_TAG_PREFIX = "format:"
        fun tagFor(appId: Int) = "$APP_TAG_PREFIX$appId"
        fun nameFor(appId: Int) = "download-$appId"
    }

    /** The format a queued download was started with, when the user picked one. */
    fun formatOf(info: WorkInfo): FormatOption? = info.tags.firstNotNullOfOrNull { tag ->
        val (format, bytes) = tag.removePrefix(FORMAT_TAG_PREFIX).takeIf { it != tag }?.split(':')
            ?.takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
        bytes.toLongOrNull()?.let { FormatOption(format, it) }
    }
}
