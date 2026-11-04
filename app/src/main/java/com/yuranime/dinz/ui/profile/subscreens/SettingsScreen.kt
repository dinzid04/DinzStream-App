package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.HomeSliderMode
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Halaman Pengaturan (SettingsScreen)
 *
 * Mengikuti standar Anti-Slop:
 * - Permukaan gelap halus #0E0F12 dan kartu #161820 dengan border tipis 0.7.dp #22FFFFFF.
 * - Tipografi SF Pro Display bersih dan terarah.
 * - Pengaturan mode slider (Default vs Clean Cover), auto-next, bersihkan cache, dan pembaruan.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCheckUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentSliderMode by AppPreferences.sliderMode.collectAsState()
    val autoNextEnabled by AppPreferences.autoNext.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0F12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Pengaturan",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            // 2. Daftar Pengaturan
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: TAMPILAN BERANDA (SLIDER)
                item {
                    Text(
                        text = "TAMPILAN BERANDA",
                        color = Color(0xFFFF5722),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161820))
                            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    ) {
                        // Opsi 1: Default (Dengan Poster)
                        SliderModeOptionItem(
                            title = "Slider dengan Poster (Default)",
                            subtitle = "Menampilkan kartu poster anime di pojok kiri bawah slider",
                            isSelected = currentSliderMode == HomeSliderMode.DEFAULT,
                            onClick = {
                                AppPreferences.setSliderMode(context, HomeSliderMode.DEFAULT)
                                Toast.makeText(context, "Mode Slider Default diaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0x15FFFFFF),
                            thickness = 0.7.dp
                        )

                        // Opsi 2: Clean Cover (Sesuai Gambar Referensi Baru)
                        SliderModeOptionItem(
                            title = "Slider Tanpa Poster (Clean Cover)",
                            subtitle = "Menampilkan cover sinematik penuh dan ikon pencarian di kanan atas",
                            isSelected = currentSliderMode == HomeSliderMode.CLEAN_COVER,
                            onClick = {
                                AppPreferences.setSliderMode(context, HomeSliderMode.CLEAN_COVER)
                                Toast.makeText(context, "Mode Slider Clean Cover diaktifkan", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                // SECTION 2: PEMUTARAN VIDEO
                item {
                    Text(
                        text = "PEMUTARAN VIDEO",
                        color = Color(0xFFFF5722),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161820))
                            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Putar Otomatis Episode Selanjutnya",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Lanjutkan ke episode berikutnya saat video selesai diputar",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Switch(
                                checked = autoNextEnabled,
                                onCheckedChange = { isChecked ->
                                    AppPreferences.setAutoNext(context, isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF5722),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF222530)
                                )
                            )
                        }
                    }
                }

                // SECTION 3: PENYIMPANAN & DATA
                item {
                    Text(
                        text = "PENYIMPANAN & DATA",
                        color = Color(0xFFFF5722),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161820))
                            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppPreferences.clearImageCache(context)
                                    Toast.makeText(context, "Cache gambar berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222530)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Bersihkan Cache Gambar",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Kosongkan penyimpanan sementara foto cover & poster",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // SECTION 4: TENTANG APLIKASI
                item {
                    Text(
                        text = "TENTANG APLIKASI",
                        color = Color(0xFFFF5722),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF161820))
                            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                    ) {
                        // Versi Aplikasi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Versi Aplikasi",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )

                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = Color(0xFFFF5722),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color(0x15FFFFFF),
                            thickness = 0.7.dp
                        )

                        // Periksa Pembaruan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCheckUpdateClick() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF222530)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Periksa Pembaruan",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Cek rilis APK versi terbaru di GitHub",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderModeOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                fontSize = 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = SfProFontFamily,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Radio / Check Circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFFFF5722) else Color(0xFF222530))
                .border(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFFFF5722) else Color(0x44FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
