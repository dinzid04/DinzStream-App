package com.yuranime.dinz.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

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

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    private val _activeUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val activeUpdateInfo: StateFlow<AppUpdateInfo?> = _activeUpdateInfo.asStateFlow()

    @Volatile
    private var pendingInstallApkPath: String? = null

    fun startDownload(context: Context, updateInfo: AppUpdateInfo) {
        val url = updateInfo.downloadUrl.trim()
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            _activeUpdateInfo.value = updateInfo
            _isMinimized.value = false
            _downloadState.value = UpdateDownloadState.Failed(
                version = updateInfo.latestVersion,
                message = "Tautan file APK tidak valid atau tidak tersedia pada rilis ini.",
                downloadUrl = updateInfo.downloadUrl,
                htmlUrl = updateInfo.htmlUrl
            )
            return
        }

        val current = _downloadState.value
        if (current is UpdateDownloadState.Downloading) {
            _isMinimized.value = false
            return
        }

        _activeUpdateInfo.value = updateInfo
        _isMinimized.value = false
        clearOldUpdateApks(context)

        _downloadState.value = UpdateDownloadState.Downloading(
            version = updateInfo.latestVersion,
            downloadedBytes = 0L,
            totalBytes = 0L,
            progressPercent = 0
        )

        val intent = Intent(context.applicationContext, UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_START_DOWNLOAD
            putExtra(UpdateDownloadService.EXTRA_VERSION, updateInfo.latestVersion)
            putExtra(UpdateDownloadService.EXTRA_DOWNLOAD_URL, url)
            putExtra(UpdateDownloadService.EXTRA_HTML_URL, updateInfo.htmlUrl)
        }
        ContextCompat.startForegroundService(context.applicationContext, intent)
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
    }

    internal fun updateProgress(version: String, downloadedBytes: Long, totalBytes: Long, percent: Int) {
        _downloadState.value = UpdateDownloadState.Downloading(
            version = version,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progressPercent = percent.coerceIn(0, 100)
        )
    }

    internal fun markCompleted(version: String, apkFilePath: String) {
        _downloadState.value = UpdateDownloadState.Completed(
            version = version,
            apkFilePath = apkFilePath
        )
    }

    internal fun markFailed(version: String, message: String, downloadUrl: String, htmlUrl: String) {
        _downloadState.value = UpdateDownloadState.Failed(
            version = version,
            message = message,
            downloadUrl = downloadUrl,
            htmlUrl = htmlUrl
        )
    }

    fun getUpdateDirectory(context: Context): File {
        val dir = File(context.applicationContext.cacheDir, UPDATES_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun clearOldUpdateApks(context: Context) {
        runCatching {
            val cacheUpdates = File(context.applicationContext.cacheDir, UPDATES_SUBDIR)
            cacheUpdates.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
            }
            val filesUpdates = File(context.applicationContext.filesDir, UPDATES_SUBDIR)
            filesUpdates.listFiles()?.forEach { file ->
                if (file.isFile) file.delete()
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
        val file = File(apkFilePath)
        if (!file.exists() || file.length() <= 1024L) {
            Toast.makeText(context, "File APK pembaruan tidak ditemukan atau rusak.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallApkPath = apkFilePath
            Toast.makeText(
                context,
                "Izinkan instalasi dari sumber ini untuk memasang pembaruan.",
                Toast.LENGTH_LONG
            ).show()
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(settingsIntent) }
            return false
        }

        pendingInstallApkPath = null
        return runCatching {
            val installIntent = buildInstallIntent(context, file)
            context.startActivity(installIntent)
            true
        }.getOrElse { err ->
            Toast.makeText(
                context,
                "Gagal membuka pemasang paket: ${err.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    fun consumePendingInstallIfGranted(context: Context) {
        val path = pendingInstallApkPath ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallApkPath = null
            triggerApkInstall(context, path)
        }
    }

    fun formatBytesMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb.coerceAtLeast(0.0))
    }
}
