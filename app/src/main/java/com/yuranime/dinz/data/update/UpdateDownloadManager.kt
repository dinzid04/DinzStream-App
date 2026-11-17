package com.yuranime.dinz.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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

    fun startDownload(context: Context, updateInfo: AppUpdateInfo) {
        val rawUrl = updateInfo.downloadUrl.trim().ifBlank { updateInfo.htmlUrl.trim() }
        val secureUrl = when {
            rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
            rawUrl.startsWith("http://", ignoreCase = true) -> "https://" + rawUrl.removePrefix("http://")
            else -> ""
        }
        if (secureUrl.isBlank()) {
            _activeUpdateInfo.value = updateInfo
            _isMinimized.value = false
            _downloadState.value = UpdateDownloadState.Failed(
                version = updateInfo.latestVersion,
                message = "Tautan rilis tidak tersedia.",
                downloadUrl = updateInfo.downloadUrl,
                htmlUrl = updateInfo.htmlUrl
            )
            return
        }

        clearOldUpdateApks(context)
        _activeUpdateInfo.value = updateInfo
        _downloadState.value = UpdateDownloadState.Idle
        _isMinimized.value = false

        val opened = runCatching {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(secureUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        }.getOrDefault(false)

        if (!opened) {
            Toast.makeText(
                context,
                "Tidak dapat membuka browser untuk mengunduh pembaruan.",
                Toast.LENGTH_SHORT
            ).show()
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

    fun triggerApkInstall(context: Context, apkFilePath: String): Boolean {
        val targetUrl = _activeUpdateInfo.value?.let {
            it.downloadUrl.trim().ifBlank { it.htmlUrl.trim() }
        }.orEmpty()
        if (targetUrl.startsWith("https://", ignoreCase = true)) {
            return runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        return false
    }

    fun consumePendingInstallIfGranted(context: Context) {
        // Pembaruan diarahkan ke browser sistem secara aman
    }

    fun formatBytesMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb.coerceAtLeast(0.0))
    }
}
