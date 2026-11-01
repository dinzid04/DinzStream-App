package com.yuranime.dinz.ui.update

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuranime.dinz.data.update.AppUpdateInfo
import com.yuranime.dinz.ui.components.MarkdownRenderer
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Popup Dialog Update Otomatis bergaya iOS Frosted Dark Glassmorphism:
 * 1. Overlay kontainer semi-transparan efek buram (#CC1C1C1E / #D9242426, RoundedCornerShape(22.dp), border 0.5.dp #26FFFFFF).
 * 2. Tipografi SF SemiBold dan SF Bold Putih (#FFFFFF).
 * 3. Render gambar, simbol, divider, blockquote & URL biru via MarkdownRenderer.
 * 4. Tombol aksi penuh "Update Sekarang" langsung mengunduh/membuka APK dari GitHub Releases.
 */
@Composable
fun UpdateNoticeDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
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

                        // Tombol Tutup Pojok Kanan Atas
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onDismiss() },
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

                    // 3. Tombol Aksi Bawah Bergaya iOS: "Update Sekarang"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val targetUrl = updateInfo.downloadUrl.ifBlank { updateInfo.htmlUrl }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
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
                                text = "Update Sekarang",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
