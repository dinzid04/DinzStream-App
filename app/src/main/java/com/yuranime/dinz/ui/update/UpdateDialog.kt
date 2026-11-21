package com.yuranime.dinz.ui.update

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.yuranime.dinz.data.update.AppUpdateInfo
import com.yuranime.dinz.data.update.UpdateDownloadManager
import com.yuranime.dinz.data.update.UpdateDownloadState
import com.yuranime.dinz.ui.components.MarkdownRenderer
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Popup Dialog Update Otomatis bergaya iOS Frosted Dark Glassmorphism:
 * 1. Overlay kontainer semi-transparan efek buram (#CC1C1C1E / #D9242426, RoundedCornerShape(22.dp), border 0.5.dp #26FFFFFF).
 * 2. Tipografi SF SemiBold dan SF Bold Putih (#FFFFFF).
 * 3. Render gambar, simbol, divider, blockquote & URL biru via MarkdownRenderer.
 * 4. Tombol aksi penuh "Update Sekarang" langsung mengunduh & memasang APK di dalam aplikasi.
 */
@Composable
fun UpdateNoticeDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by UpdateDownloadManager.downloadState.collectAsState()

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        UpdateDownloadManager.startDownload(context, updateInfo)
    }

    val startBackgroundUpdate: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            UpdateDownloadManager.startDownload(context, updateInfo)
        }
    }

    val handleMinimizeOrDismiss: () -> Unit = {
        if (downloadState is UpdateDownloadState.Downloading) {
            UpdateDownloadManager.minimizeBanner()
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = handleMinimizeOrDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect {
            val dialogWindow = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            if (dialogWindow != null) {
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                dialogWindow.clearFlags(
                    android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                        android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                )
                dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
                dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    dialogWindow.isStatusBarContrastEnforced = false
                    dialogWindow.isNavigationBarContrastEnforced = false
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { /* Mencegah dismiss sengaja di area luar */ }
                .padding(horizontal = 24.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            // Kontainer Kartu Dialog iOS Dark Glassmorphism
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xD9242426))
                    .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Header Kartu Bergaya Ilustrasi Update
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E3A8A).copy(alpha = 0.6f),
                                        Color(0xFF0F172A).copy(alpha = 0.8f),
                                        Color(0xD9242426)
                                    )
                                )
                            )
                            .padding(top = 18.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Ikon Roket / Update Launch
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x330A84FF))
                                    .border(1.dp, Color(0x550A84FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = "Pembaruan Tersedia",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Update Notice",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0A84FF))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Versi ${updateInfo.latestVersion}",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }

                        // Tombol Tutup / Sembunyikan Pojok Kanan Atas
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { handleMinimizeOrDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color(0xFFE5E5EA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0x26FFFFFF))

                    // 2. Area Konten yang Bisa Di-scroll (Markdown & README Renderer)
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        MarkdownRenderer(
                            markdown = updateInfo.releaseNotes.ifBlank {
                                "### Pembaruan Baru Siap Dipasang!\n\n" +
                                "- Peningkatan performa & stabilitas aplikasi Yuranime.\n" +
                                "- Perbaikan bug dan optimalisasi pemutaran video anime.\n" +
                                "- Desain antarmuka baru yang lebih elegan dan responsif."
                            },
                            textColor = Color(0xFFE5E5EA),
                            fontSize = 13.5.sp
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0x26FFFFFF))

                    // 3. Area Aksi Bawah In-App Background Update + Status Bar Putih (#FFFFFF)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (val state = downloadState) {
                            is UpdateDownloadState.Idle -> {
                                Button(
                                    onClick = startBackgroundUpdate,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0A84FF),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Update Sekarang",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            is UpdateDownloadState.Downloading -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mengunduh pembaruan...",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Text(
                                        text = "${state.progressPercent}%",
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                if (state.totalBytes > 0L) {
                                    LinearProgressIndicator(
                                        progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                        color = Color.White,
                                        trackColor = Color(0x33FFFFFF),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        color = Color.White,
                                        trackColor = Color(0x33FFFFFF),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }

                                val sizeText = if (state.totalBytes > 0L) {
                                    "${UpdateDownloadManager.formatBytesMb(state.downloadedBytes)} / ${UpdateDownloadManager.formatBytesMb(state.totalBytes)}"
                                } else {
                                    "${UpdateDownloadManager.formatBytesMb(state.downloadedBytes)} diunduh"
                                }
                                Text(
                                    text = sizeText,
                                    color = Color(0xFFE5E5EA),
                                    fontSize = 11.5.sp,
                                    fontFamily = SfProFontFamily
                                )

                                OutlinedButton(
                                    onClick = { handleMinimizeOrDismiss() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Text(
                                        text = "Sembunyikan (Unduh di Latar Belakang)",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            is UpdateDownloadState.Completed -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Pembaruan v${state.version} siap dipasang.",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Button(
                                    onClick = {
                                        UpdateDownloadManager.triggerApkInstall(context, state.apkFilePath)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0A84FF),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "Pasang Sekarang",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "🛡️ Tips: Jika muncul konfirmasi Google Play Protect ('Aplikasi tidak dikenal'), ketuk 'Rincian lebih lanjut' lalu pilih 'Tetap instal'. Pembaruan Yuranime 100% aman dan diverifikasi resmi.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = SfProFontFamily,
                                    lineHeight = 15.sp
                                )
                            }

                            is UpdateDownloadState.Failed -> {
                                Text(
                                    text = state.message,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )

                                Button(
                                    onClick = startBackgroundUpdate,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0A84FF),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Text(
                                        text = "Coba Lagi",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                val browserUrl = state.downloadUrl.ifBlank { state.htmlUrl }
                                if (browserUrl.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            runCatching {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(browserUrl))
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = "Buka di Browser",
                                            color = Color(0xFF93C5FD),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mini banner progres mengambang agar pengguna tetap bisa menonton/menjelajah aplikasi
 * saat pengunduhan update berjalan di latar belakang, dengan status bar putih murni (#FFFFFF).
 */
@Composable
fun UpdateMiniProgressBanner(
    downloadState: UpdateDownloadState,
    onExpandDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (downloadState !is UpdateDownloadState.Downloading &&
        downloadState !is UpdateDownloadState.Completed
    ) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEB141720))
            .border(0.75.dp, Color(0x38FFFFFF), RoundedCornerShape(14.dp))
            .clickable {
                if (downloadState is UpdateDownloadState.Completed) {
                    UpdateDownloadManager.triggerApkInstall(context, downloadState.apkFilePath)
                } else {
                    UpdateDownloadManager.expandDialog()
                    onExpandDialog()
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        when (downloadState) {
            is UpdateDownloadState.Downloading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sizePart = if (downloadState.totalBytes > 0L) {
                            " (${UpdateDownloadManager.formatBytesMb(downloadState.downloadedBytes)} / ${UpdateDownloadManager.formatBytesMb(downloadState.totalBytes)})"
                        } else if (downloadState.downloadedBytes > 0L) {
                            " (${UpdateDownloadManager.formatBytesMb(downloadState.downloadedBytes)})"
                        } else ""

                        Text(
                            text = "Mengunduh Yuranime v${downloadState.version}$sizePart",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${downloadState.progressPercent}%",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    if (downloadState.totalBytes > 0L) {
                        LinearProgressIndicator(
                            progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                            color = Color.White,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    } else {
                        LinearProgressIndicator(
                            color = Color.White,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            is UpdateDownloadState.Completed -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pembaruan v${downloadState.version} siap dipasang",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pasang",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .clickable { UpdateDownloadManager.resetToIdle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup notifikasi unduhan",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}
