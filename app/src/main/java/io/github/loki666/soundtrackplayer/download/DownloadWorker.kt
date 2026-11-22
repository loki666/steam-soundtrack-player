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
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.loki666.soundtrackplayer.library.LibraryRepository
import io.github.loki666.soundtrackplayer.library.LibraryStore
import io.github.loki666.soundtrackplayer.steam.SteamSession
import io.github.loki666.soundtrackplayer.library.AudioFormats
import io.github.loki666.soundtrackplayer.library.FormatOptions
import io.github.loki666.soundtrackplayer.settings.DownloadPreferences
import io.github.loki666.soundtrackplayer.library.TrackScanner
import io.github.loki666.soundtrackplayer.steam.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Downloads one soundtrack's depots with JavaSteam's DepotDownloader, then scans the files into
 * an ordered track list. When the soundtrack ships several audio formats, only one is downloaded:
 * the one the user picked ([KEY_FORMAT]), else their default quality. Runs as a foreground worker so large FLAC albums survive the app closing.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val steam: SteamSession,
    private val library: LibraryRepository,
    private val libraryStore: LibraryStore,
    private val preferences: DownloadPreferences,
) : CoroutineWorker(context, params) {

    private val appId = inputData.getInt(KEY_APP_ID, 0)
    private val albumName by lazy {
        library.albumName(appId) ?: "Soundtrack"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(null)

    override suspend fun doWork(): Result {
        if (appId == 0) return Result.failure()
        startForeground()

        // One album at a time: each DepotDownloader can hold 100+ MB of chunk buffers, so two at
        // once ran out of heap. Later albums wait here and show as queued.
        if (downloadLock.isLocked) setProgress(workDataOf(KEY_WAITING to true))
        return downloadLock.withLock {
            setProgress(workDataOf(KEY_PROGRESS to 0f))
            download()
        }
    }

    /**
     * Android 12+ refuses a foreground service when the job is restarted in the background (e.g.
     * after the process died). Keep going as regular background work rather than failing.
     */
    private suspend fun startForeground() {
        try {
            setForeground(foregroundInfo(null))
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Couldn't start the download as a foreground service; continuing in the background", e)
        }
    }

    private suspend fun download(): Result {
        val store = libraryStore
        val dir = store.albumDir(appId).apply { mkdirs() }
        return try {
            val licenses = steam.licenses()

            // 1. Manifest-only pass: learn which files the soundtrack ships, without downloading them.
            runDepotDownloader(steam, licenses, appId, dir, manifestOnly = true)
            val options = FormatOptions.from(DepotFiles.readManifestDumps(dir))
            val requested = inputData.getString(KEY_FORMAT)
            val format = options.firstOrNull { it.format == requested }
                ?: FormatOptions.preferred(options, preferences.quality.value)

            // 2. Download only that audio format (plus cover art and other extras).
            runDepotDownloader(
                steam, licenses, appId, dir,
                manifestOnly = false,
                filter = format?.format?.let(AudioFormats::downloadFilter),
                onProgress = { p ->
                    setProgressAsync(workDataOf(KEY_PROGRESS to p))
                    notify(p)
                },
            )

            val local = TrackScanner.scan(appId, dir, downloadedFormat = format?.format)
                ?: return Result.failure(workDataOf(KEY_ERROR to "No audio files were found in this soundtrack."))
            // Safety net if the filter couldn't be applied: drop the formats we don't play.
            TrackScanner.deleteOtherFormats(dir, local.format)
            store.saveLocal(local)
            library.onDownloadFinished(appId)
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
        /** Audio format (file extension) to download; absent = the user's default quality. */
        const val KEY_FORMAT = "format"
        const val KEY_PROGRESS = "progress"
        /** Progress flag: the worker is waiting for another album's download to finish. */
        const val KEY_WAITING = "waiting"
        const val KEY_ERROR = "error"
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_BASE = 10_000
        private const val TAG = "DownloadWorker"

        /** Process-wide: WorkManager runs workers concurrently, so queueing happens here. */
        private val downloadLock = Mutex()
    }
}
