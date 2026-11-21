package io.github.loki666.soundtrackplayer.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadScheduler(context: Context) {
    private val work = WorkManager.getInstance(context)

    fun download(appId: Int) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_APP_ID to appId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG)
            .addTag(tagFor(appId))
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
        fun tagFor(appId: Int) = "$APP_TAG_PREFIX$appId"
        fun nameFor(appId: Int) = "download-$appId"
    }
}
