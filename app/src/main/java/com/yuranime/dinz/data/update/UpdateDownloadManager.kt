package com.yuranime.dinz.data.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.yuranime.dinz.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()

    data class Downloading(
        val version: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progressPercent: Int
    ) : UpdateDownloadState()

    data class Completed(
        val version: String,
        val apkFilePath: String
    ) : UpdateDownloadState()

    data class Failed(
        val version: String,
        val message: String,
        val downloadUrl: String,
        val htmlUrl: String
    ) : UpdateDownloadState()
}

object UpdateDownloadManager {

    const val UPDATES_SUBDIR = "updates"

    private const val CHANNEL_PROGRESS_ID = "yuranime_app_update_progress"
    private const val CHANNEL_COMPLETE_ID = "yuranime_app_update_complete"
    private const val NOTIFICATION_PROGRESS_ID = 4201
    private const val NOTIFICATION_COMPLETE_ID = 4202
    private const val MIN_VALID_APK_BYTES = 512L * 1024L

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    @Volatile
    private var activeDownloadId: Long = -1L

    @Volatile
    private var pendingInstallApkPath: String? = null

    @Volatile
    private var waitingInstallPermissionResult: Boolean = false

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    private val _activeUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val activeUpdateInfo: StateFlow<AppUpdateInfo?> = _activeUpdateInfo.asStateFlow()

    fun startDownload(context: Context, updateInfo: AppUpdateInfo) {
        val appCtx = context.applicationContext
        val rawUrl = updateInfo.downloadUrl.trim().ifBlank { updateInfo.htmlUrl.trim() }
        val secureUrl = when {
            rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
            rawUrl.startsWith("http://", ignoreCase = true) -> "https://" + rawUrl.removePrefix("http://")
            else -> ""
        }
        val version = updateInfo.latestVersion.ifBlank { "Terbaru" }

        val isHtmlReleaseFallback = secureUrl == updateInfo.htmlUrl.trim() &&
            !secureUrl.contains(".apk", ignoreCase = true)

        var finalDownloadUrl = secureUrl
        if (finalDownloadUrl.isBlank() || isHtmlReleaseFallback) {
            val directApk = UpdateChecker.resolveDirectApkUrl(updateInfo.latestVersion, finalDownloadUrl)
            if (directApk.contains(".apk", ignoreCase = true)) {
                finalDownloadUrl = directApk
            } else {
                _activeUpdateInfo.value = updateInfo
                _isMinimized.value = false
                _downloadState.value = UpdateDownloadState.Failed(
                    version = version,
                    message = "Tautan file APK belum tersedia pada rilis ini.",
                    downloadUrl = secureUrl,
                    htmlUrl = updateInfo.htmlUrl
                )
                return
            }
        }

        val current = _downloadState.value
        if (current is UpdateDownloadState.Downloading) {
            _isMinimized.value = false
            return
        }

        cancelOngoingJobs(appCtx)
        clearOldUpdateApks(appCtx)
        ensureNotificationChannels(appCtx)

        _activeUpdateInfo.value = updateInfo
        _isMinimized.value = false
        _downloadState.value = UpdateDownloadState.Downloading(
            version = version,
            downloadedBytes = 0L,
            totalBytes = 0L,
            progressPercent = 0
        )

        val safeTag = version.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "Yuranime-$safeTag.apk"

        downloadJob = managerScope.launch {
            val dmSucceeded = downloadViaSystemDownloadManager(
                appCtx = appCtx,
                version = version,
                secureUrl = finalDownloadUrl,
                fileName = fileName
            )
            if (!dmSucceeded && isActive) {
                executeOkHttpDownload(
                    appCtx = appCtx,
                    updateInfo = updateInfo.copy(downloadUrl = finalDownloadUrl),
                    version = version,
                    secureUrl = finalDownloadUrl,
                    fileName = fileName
                )
            }
        }
    }

    private suspend fun downloadViaSystemDownloadManager(
        appCtx: Context,
        version: String,
        secureUrl: String,
        fileName: String
    ): Boolean {
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return false
        val downloadsDir = runCatching { appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) }.getOrNull()
            ?: return false
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) return false

        val expectedFile = File(downloadsDir, fileName)
        if (expectedFile.exists()) expectedFile.delete()

        val downloadId = runCatching {
            val request = DownloadManager.Request(Uri.parse(secureUrl))
                .setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setTitle("Yuranime v$version")
                .setDescription("Mengunduh pembaruan aplikasi")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(appCtx, Environment.DIRECTORY_DOWNLOADS, fileName)
                .addRequestHeader("User-Agent", "Yuranime-App")
            dm.enqueue(request)
        }.getOrElse {
            return false
        }

        if (downloadId <= 0L) return false
        activeDownloadId = downloadId

        val startAtMs = System.currentTimeMillis()
        var lastBytes = 0L
        var lastProgressAtMs = startAtMs

        try {
            while (downloadJob?.isActive == true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                var status = -1
                var bytesSoFar = 0L
                var totalBytes = 0L
                var localUri: String? = null
                var found = false

                dm.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        found = true
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val soFarIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                        if (statusIdx >= 0) status = cursor.getInt(statusIdx)
                        if (soFarIdx >= 0) bytesSoFar = cursor.getLong(soFarIdx).coerceAtLeast(0L)
                        if (totalIdx >= 0) totalBytes = cursor.getLong(totalIdx).coerceAtLeast(0L)
                        if (uriIdx >= 0) localUri = cursor.getString(uriIdx)
                    }
                }

                if (!found) {
                    return false
                }

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val resolvedFile = resolveLocalFile(localUri, expectedFile)
                        if (isValidApkFile(resolvedFile)) {
                            activeDownloadId = -1L
                            onApkReady(appCtx, version, resolvedFile)
                            return true
                        } else {
                            runCatching { resolvedFile.delete() }
                            runCatching { dm.remove(downloadId) }
                            activeDownloadId = -1L
                            return false
                        }
                    }

                    DownloadManager.STATUS_FAILED -> {
                        runCatching { dm.remove(downloadId) }
                        activeDownloadId = -1L
                        return false
                    }

                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_PAUSED -> {
                        val now = System.currentTimeMillis()
                        if (bytesSoFar > lastBytes) {
                            lastBytes = bytesSoFar
                            lastProgressAtMs = now
                        }
                        if (bytesSoFar == 0L && now - startAtMs > 18_000L) {
                            // Sistem DownloadManager tertahan atau nonaktif; alihkan ke unduhan langsung OkHttp
                            runCatching { dm.remove(downloadId) }
                            activeDownloadId = -1L
                            return false
                        }
                        if (bytesSoFar > 0L && status == DownloadManager.STATUS_PAUSED && now - lastProgressAtMs > 25_000L) {
                            runCatching { dm.remove(downloadId) }
                            activeDownloadId = -1L
                            return false
                        }

                        val pct = if (totalBytes > 0L) {
                            ((bytesSoFar * 100L) / totalBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        _downloadState.value = UpdateDownloadState.Downloading(
                            version = version,
                            downloadedBytes = bytesSoFar,
                            totalBytes = totalBytes,
                            progressPercent = pct
                        )
                    }
                }
                delay(300L)
            }
            return false
        } catch (e: CancellationException) {
            runCatching { dm.remove(downloadId) }
            activeDownloadId = -1L
            throw e
        } catch (_: Throwable) {
            runCatching { dm.remove(downloadId) }
            activeDownloadId = -1L
            return false
        }
    }

    private fun executeOkHttpDownload(
        appCtx: Context,
        updateInfo: AppUpdateInfo,
        version: String,
        secureUrl: String,
        fileName: String
    ) {
        val notifManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val updateDir = getUpdateDirectory(appCtx)
        val tempFile = File(updateDir, "$fileName.part")
        val finalFile = File(updateDir, fileName)

        try {
            if (tempFile.exists()) tempFile.delete()
            if (finalFile.exists()) finalFile.delete()

            val request = Request.Builder()
                .url(secureUrl)
                .header("User-Agent", "Yuranime-App")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Server mengembalikan status HTTP ${response.code}")
                }
                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.startsWith("text/html", ignoreCase = true)) {
                    throw IllegalStateException("Tautan tidak mengarah ke file APK langsung.")
                }

                val body = response.body ?: throw IllegalStateException("Respons unduhan kosong.")
                val totalBytes = body.contentLength().coerceAtLeast(0L)
                var downloadedBytes = 0L
                var lastUiUpdateMs = 0L
                var lastNotifPct = -5

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (downloadJob?.isActive != false) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            val pct = if (totalBytes > 0L) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                            if (now - lastUiUpdateMs >= 150L || pct - lastNotifPct >= 2) {
                                lastUiUpdateMs = now
                                lastNotifPct = pct
                                _downloadState.value = UpdateDownloadState.Downloading(
                                    version = version,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    progressPercent = pct
                                )
                                postProgressNotificationSafely(
                                    notifManager = notifManager,
                                    appCtx = appCtx,
                                    version = version,
                                    percent = pct,
                                    indeterminate = totalBytes <= 0L,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            if (!isValidApkFile(finalFile)) {
                finalFile.delete()
                throw IllegalStateException("File APK yang diunduh tidak valid atau terpotong.")
            }

            notifManager?.cancel(NOTIFICATION_PROGRESS_ID)
            onApkReady(appCtx, version, finalFile)
        } catch (e: CancellationException) {
            tempFile.delete()
            notifManager?.cancel(NOTIFICATION_PROGRESS_ID)
            throw e
        } catch (e: Exception) {
            tempFile.delete()
            notifManager?.cancel(NOTIFICATION_PROGRESS_ID)
            _downloadState.value = UpdateDownloadState.Failed(
                version = version,
                message = e.message ?: "Unduhan gagal. Periksa koneksi internet Anda.",
                downloadUrl = updateInfo.downloadUrl,
                htmlUrl = updateInfo.htmlUrl
            )
            runCatching {
                val failNotif = NotificationCompat.Builder(appCtx, CHANNEL_COMPLETE_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Yuranime")
                    .setContentText("Gagal mengunduh pembaruan v$version")
                    .setAutoCancel(true)
                    .build()
                notifManager?.notify(NOTIFICATION_COMPLETE_ID, failNotif)
            }
        }
    }

    private fun onApkReady(appCtx: Context, version: String, finalFile: File) {
        _downloadState.value = UpdateDownloadState.Completed(
            version = version,
            apkFilePath = finalFile.absolutePath
        )

        val notifManager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        runCatching {
            val completedNotif = NotificationCompat.Builder(appCtx, CHANNEL_COMPLETE_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Yuranime v$version")
                .setContentText("Pembaruan siap dipasang. Ketuk untuk menginstal")
                .setContentIntent(buildInstallPendingIntent(appCtx, finalFile))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notifManager?.notify(NOTIFICATION_COMPLETE_ID, completedNotif)
        }

        Handler(Looper.getMainLooper()).post {
            triggerApkInstall(appCtx, finalFile.absolutePath)
        }
    }

    private fun resolveLocalFile(localUri: String?, fallback: File): File {
        if (!localUri.isNullOrBlank()) {
            val parsed = runCatching { Uri.parse(localUri).path }.getOrNull()
            if (!parsed.isNullOrBlank()) {
                val candidate = File(parsed)
                if (candidate.exists()) return candidate
            }
        }
        return fallback
    }

    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < MIN_VALID_APK_BYTES) {
            return false
        }
        return runCatching {
            val header = ByteArray(4)
            file.inputStream().use { input ->
                if (input.read(header) < 4) return@runCatching false
            }
            header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        }.getOrDefault(false)
    }

    private fun cancelOngoingJobs(appCtx: Context) {
        downloadJob?.cancel()
        downloadJob = null
        val dmId = activeDownloadId
        if (dmId > 0L) {
            activeDownloadId = -1L
            val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            runCatching { dm?.remove(dmId) }
        }
    }

    fun minimizeBanner() {
        _isMinimized.value = true
    }

    fun expandDialog() {
        _isMinimized.value = false
    }

    fun resetToIdle() {
        _downloadState.value = UpdateDownloadState.Idle
        _isMinimized.value = false
        pendingInstallApkPath = null
        waitingInstallPermissionResult = false
    }

    fun getUpdateDirectory(context: Context): File {
        val dir = File(context.applicationContext.cacheDir, UPDATES_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun clearOldUpdateApks(context: Context) {
        val appCtx = context.applicationContext
        runCatching {
            val cacheUpdates = File(appCtx.cacheDir, UPDATES_SUBDIR)
            cacheUpdates.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
            val filesUpdates = File(appCtx.filesDir, UPDATES_SUBDIR)
            filesUpdates.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
            val extDownloads = appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            extDownloads?.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".apk") || file.name.endsWith(".part"))) {
                    file.delete()
                }
            }
        }
    }

    fun buildInstallIntent(context: Context, apkFile: File): Intent {
        val appCtx = context.applicationContext
        val uri = FileProvider.getUriForFile(
            appCtx,
            "${appCtx.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, appCtx.packageName)
        }
    }

    fun buildInstallPendingIntent(context: Context, apkFile: File): PendingIntent {
        val intent = buildInstallIntent(context, apkFile)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context.applicationContext, 7701, intent, flags)
    }

    fun triggerApkInstall(context: Context, apkFilePath: String): Boolean {
        val appCtx = context.applicationContext
        val file = File(apkFilePath)
        if (!isValidApkFile(file)) {
            Toast.makeText(appCtx, "File APK pembaruan tidak ditemukan atau rusak.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appCtx.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallApkPath = apkFilePath
            waitingInstallPermissionResult = true
            Toast.makeText(
                appCtx,
                "Izinkan instalasi dari sumber ini untuk memasang pembaruan.",
                Toast.LENGTH_LONG
            ).show()
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appCtx.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(settingsIntent) }
            return false
        }

        pendingInstallApkPath = null
        waitingInstallPermissionResult = false
        return runCatching {
            val installIntent = buildInstallIntent(appCtx, file)
            context.startActivity(installIntent)
            true
        }.getOrElse { err ->
            Toast.makeText(
                appCtx,
                "Gagal membuka pemasang paket: ${err.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    fun consumePendingInstallIfGranted(context: Context) {
        if (!waitingInstallPermissionResult) return
        val path = pendingInstallApkPath ?: run {
            waitingInstallPermissionResult = false
            return
        }
        waitingInstallPermissionResult = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallApkPath = null
            triggerApkInstall(context, path)
        }
    }

    private fun ensureNotificationChannels(appCtx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
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

    private fun postProgressNotificationSafely(
        notifManager: NotificationManager?,
        appCtx: Context,
        version: String,
        percent: Int,
        indeterminate: Boolean,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        if (notifManager == null) return
        val statusText = if (totalBytes > 0L) {
            "Mengunduh pembaruan v$version ($percent%)"
        } else if (downloadedBytes > 0L) {
            "Mengunduh pembaruan v$version (${formatBytesMb(downloadedBytes)})"
        } else {
            "Mengunduh pembaruan..."
        }
        runCatching {
            val notif = NotificationCompat.Builder(appCtx, CHANNEL_PROGRESS_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Yuranime")
                .setContentText(statusText)
                .setSubText(if (totalBytes > 0L) "$percent%" else null)
                .setProgress(100, percent, indeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            notifManager.notify(NOTIFICATION_PROGRESS_ID, notif)
        }
    }

    fun formatBytesMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb.coerceAtLeast(0.0))
    }
}
