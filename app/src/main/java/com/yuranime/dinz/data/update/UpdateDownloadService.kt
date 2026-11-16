package com.yuranime.dinz.data.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yuranime.dinz.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateDownloadService : Service() {

    companion object {
        private const val TAG = "UpdateDownloadService"
        const val ACTION_START_DOWNLOAD = "com.yuranime.dinz.action.START_UPDATE_DOWNLOAD"
        const val EXTRA_VERSION = "extra_version"
        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_HTML_URL = "extra_html_url"

        private const val CHANNEL_PROGRESS_ID = "yuranime_app_update_progress"
        private const val CHANNEL_COMPLETE_ID = "yuranime_app_update_complete"
        private const val NOTIFICATION_PROGRESS_ID = 4201
        private const val NOTIFICATION_COMPLETE_ID = 4202
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START_DOWNLOAD) {
            stopSelf()
            return START_NOT_STICKY
        }

        val version = intent.getStringExtra(EXTRA_VERSION).orEmpty().ifBlank { "Terbaru" }
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL).orEmpty()
        val htmlUrl = intent.getStringExtra(EXTRA_HTML_URL).orEmpty()

        val initialNotification = buildProgressNotification(
            version = version,
            percent = 0,
            indeterminate = true,
            downloadedBytes = 0L,
            totalBytes = 0L
        )
        startForegroundSafely(initialNotification)

        if (downloadUrl.isBlank()) {
            UpdateDownloadManager.markFailed(
                version = version,
                message = "Tautan file APK tidak ditemukan.",
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl
            )
            stopSelf()
            return START_NOT_STICKY
        }

        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            executeApkDownload(
                version = version,
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl
            )
        }

        return START_NOT_STICKY
    }

    private fun executeApkDownload(
        version: String,
        downloadUrl: String,
        htmlUrl: String
    ) {
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val updateDir = UpdateDownloadManager.getUpdateDirectory(this)
        val safeVersionTag = version.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val tempFile = File(updateDir, "Yuranime-$safeVersionTag.apk.part")
        val finalFile = File(updateDir, "Yuranime-$safeVersionTag.apk")

        try {
            UpdateDownloadManager.clearOldUpdateApks(this)
            if (tempFile.exists()) tempFile.delete()
            if (finalFile.exists()) finalFile.delete()

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Yuranime-Updater/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server mengembalikan status HTTP ${response.code}")
                }
                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.startsWith("text/html", ignoreCase = true)) {
                    throw IllegalStateException("Tautan unduhan bukan berkas APK langsung.")
                }
                val body = response.body ?: throw IllegalStateException("Respons unduhan kosong")
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                var downloadedBytes = 0L
                var lastNotifTimeMs = 0L
                var lastNotifPercent = -1

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val percent = if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            UpdateDownloadManager.updateProgress(
                                version = version,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                percent = percent
                            )

                            val now = System.currentTimeMillis()
                            if (now - lastNotifTimeMs >= 350L || (totalBytes > 0L && percent > lastNotifPercent && percent % 2 == 0)) {
                                lastNotifTimeMs = now
                                lastNotifPercent = percent
                                val progNotif = buildProgressNotification(
                                    version = version,
                                    percent = percent,
                                    indeterminate = totalBytes <= 0L,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                                notifManager?.notify(NOTIFICATION_PROGRESS_ID, progNotif)
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 1024L) {
                throw IllegalStateException("File APK yang diunduh tidak valid.")
            }

            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            UpdateDownloadManager.markCompleted(
                version = version,
                apkFilePath = finalFile.absolutePath
            )

            // Hentikan progress ongoing & tampilkan notifikasi siap pasang (ketuk untuk install)
            stopForegroundRemove()
            val completedNotif = buildCompletedNotification(version, finalFile)
            notifManager?.notify(NOTIFICATION_COMPLETE_ID, completedNotif)

            // Pemicu otomatis pemasang paket APK di main thread
            Handler(Looper.getMainLooper()).post {
                UpdateDownloadManager.triggerApkInstall(applicationContext, finalFile.absolutePath)
            }
        } catch (e: CancellationException) {
            tempFile.delete()
            stopForegroundRemove()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunduh update APK: ${e.message}", e)
            tempFile.delete()
            stopForegroundRemove()
            UpdateDownloadManager.markFailed(
                version = version,
                message = e.message ?: "Unduhan gagal. Periksa koneksi internet Anda.",
                downloadUrl = downloadUrl,
                htmlUrl = htmlUrl
            )
            val failNotif = NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Yuranime")
                .setContentText("Gagal mengunduh pembaruan v$version")
                .setAutoCancel(true)
                .build()
            notifManager?.notify(NOTIFICATION_COMPLETE_ID, failNotif)
        } finally {
            stopSelf()
        }
    }

    private fun startForegroundSafely(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_PROGRESS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_PROGRESS_ID, notification)
        }
    }

    private fun stopForegroundRemove() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Unduhan Pembaruan Aplikasi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progres pengunduhan pembaruan Yuranime"
                setShowBadge(false)
            }
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Pembaruan Siap Dipasang",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat file APK pembaruan siap diinstal"
            }
            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(completeChannel)
        }
    }

    private fun buildProgressNotification(
        version: String,
        percent: Int,
        indeterminate: Boolean,
        downloadedBytes: Long,
        totalBytes: Long
    ): Notification {
        val statusText = if (totalBytes > 0L) {
            "Mengunduh pembaruan v$version ($percent%)"
        } else if (downloadedBytes > 0L) {
            "Mengunduh pembaruan v$version (${UpdateDownloadManager.formatBytesMb(downloadedBytes)})"
        } else {
            "Mengunduh pembaruan..."
        }
        return NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Yuranime")
            .setContentText(statusText)
            .setSubText(if (totalBytes > 0L) "$percent%" else null)
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildCompletedNotification(version: String, apkFile: File): Notification {
        val installPendingIntent = UpdateDownloadManager.buildInstallPendingIntent(this, apkFile)
        return NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Yuranime v$version")
            .setContentText("Pembaruan siap dipasang. Ketuk untuk menginstal")
            .setContentIntent(installPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
