package io.github.loki666.soundtrackplayer.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.loki666.soundtrackplayer.R
import io.github.loki666.soundtrackplayer.container
import io.github.loki666.soundtrackplayer.library.TrackScanner
import io.github.loki666.soundtrackplayer.steam.SessionExpiredException
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await

/**
 * Downloads one soundtrack's depots with JavaSteam's DepotDownloader, then scans the files into
 * an ordered track list. Runs as a foreground worker so large FLAC albums survive the app closing.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val container = context.container
    private val appId = inputData.getInt(KEY_APP_ID, 0)
    private val albumName by lazy {
        container.library.albumName(appId) ?: "Soundtrack"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(null)

    override suspend fun doWork(): Result {
        if (appId == 0) return Result.failure()
        setForeground(foregroundInfo(null))

        val store = container.libraryStore
        val dir = store.albumDir(appId).apply { mkdirs() }
        val depotProgress = ConcurrentHashMap<Int, Float>()
        var failure: Throwable? = null
        var lastReported = 0L

        return try {
            val licenses = container.steam.licenses()
            DepotDownloader(container.steam.client, licenses, maxDownloads = 8).use { downloader ->
                downloader.addListener(object : IDownloadListener {
                    override fun onChunkCompleted(depotId: Int, depotPercentComplete: Float, compressedBytes: Long, uncompressedBytes: Long) {
                        depotProgress[depotId] = depotPercentComplete
                        val now = System.currentTimeMillis()
                        if (now - lastReported > 500) {
                            lastReported = now
                            val p = depotProgress.values.average().toFloat().coerceIn(0f, 1f)
                            setProgressAsync(workDataOf(KEY_PROGRESS to p))
                            notify(p)
                        }
                    }

                    override fun onDownloadFailed(item: DownloadItem, error: Throwable) {
                        failure = error
                    }
                })
                downloader.add(
                    AppItem(
                        appId = appId,
                        installDirectory = dir.absolutePath,
                        // Soundtrack depots are often tagged for a desktop OS; we want them all.
                        downloadAllPlatforms = true,
                        downloadAllLanguages = true,
                    )
                )
                downloader.finishAdding()
                downloader.getCompletion().await()
            }
            failure?.let { throw it }

            val local = TrackScanner.scan(appId, dir)
                ?: return Result.failure(workDataOf(KEY_ERROR to "No audio files were found in this soundtrack."))
            store.saveLocal(local)
            container.library.onDownloadFinished(appId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionExpiredException) {
            Result.failure(workDataOf(KEY_ERROR to e.message))
        } catch (e: Exception) {
            Log.w(TAG, "Download of $appId failed", e)
            if (runAttemptCount < 2) Result.retry()
            else Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed.")))
        }
    }

    private fun notify(progress: Float) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        runCatching { nm.notify(notificationId, buildNotification(progress)) }
    }

    private fun foregroundInfo(progress: Float?): ForegroundInfo {
        val notification = buildNotification(progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildNotification(progress: Float?): android.app.Notification {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val cancel = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        return NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Downloading $albumName")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, ((progress ?: 0f) * 100).toInt(), progress == null)
            .addAction(0, "Cancel", cancel)
            .build()
    }

    private val notificationId get() = NOTIFICATION_BASE + appId % 100_000

    companion object {
        const val KEY_APP_ID = "appId"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_BASE = 10_000
        private const val TAG = "DownloadWorker"
    }
}
