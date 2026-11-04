package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.api.BirthdayCharacterItem
import com.yuranime.dinz.data.api.BirthdayTodayData
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Section Ulang Tahun Hari Ini di Halaman Home (100% Sesuai Gambar Referensi Sisi Kiri)
 *
 * Header:
 * - Teks "Ulang Tahun Hari Ini" (SF Bold putih 18.sp) diikuti " • [Tanggal]" (#94A3B8, 14.sp)
 * - Tombol lingkaran gelap di kanan dengan chevron panah '>'
 *
 * Kartu Karakter (LazyRow):
 * - Kartu potret vertikal (width = 115.dp, aspectRatio 2:3, RoundedCornerShape 12.dp) TANPA BORDER.
 * - Foto karakter memenuhi kartu dengan bottom gradient overlay halus.
 * - Baris 1: Tanggal ulang tahun oranye (#FF5722), font SF SemiBold 11.sp.
 * - Baris 2: Nama karakter putih bersih, font SF Bold 13.sp, 1 baris ellipsis.
 */
@Composable
fun BirthdayTodaySection(
    birthdayData: BirthdayTodayData?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onItemClick: (BirthdayCharacterItem) -> Unit,
    onChevronClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rawDate = birthdayData?.dateId?.takeIf { it.isNotBlank() } ?: "4 Nov"
    val formattedDate = if (rawDate.contains("November", ignoreCase = true)) {
        rawDate.replace("November", "Nov", ignoreCase = true)
    } else {
        rawDate
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Header Section Sesuai Gambar Referensi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Ulang Tahun Hari Ini",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "• $formattedDate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily,
                    color = Color(0xFF94A3B8)
                )
            }

            // Tombol Bulat Gelap Chevron Kanan '>'
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1D26))
                    .clickable { onChevronClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Lihat Semua",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Daftar Kartu Karakter / Loading / Error
        when {
            isLoading && (birthdayData == null || birthdayData.items.isEmpty()) -> {
                BirthdayShimmerRow()
            }

            errorMessage != null && (birthdayData == null || birthdayData.items.isEmpty()) -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14161D))
                        .clickable { onRetry() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gagal memuat ulang tahun hari ini",
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Muat ulang",
                            tint = ColorAccentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Coba Lagi",
                            color = ColorAccentPrimary,
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            birthdayData != null && birthdayData.items.isNotEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = birthdayData.items,
                        key = { "ultah_${it.type}_${it.id}_${it.slug}" }
                    ) { item ->
                        BirthdayCharacterCard(
                            item = item,
                            defaultDate = formattedDate,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Karakter Vertikal Sesuai Sisi Kiri Gambar Referensi:
 * - width = 115.dp, aspectRatio(2f/3f), RoundedCornerShape(12.dp), TANPA BORDER.
 * - Foto memenuhi seluruh kartu.
 * - Bottom gradient overlay hitam halus.
 * - Tanggal ulang tahun oranye (#FF5722) 11.sp di atas nama karakter putih 13.sp.
 */
@Composable
private fun BirthdayCharacterCard(
    item: BirthdayCharacterItem,
    defaultDate: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val birthDateText = item.birthDate.ifBlank { defaultDate }

    Box(
        modifier = Modifier
            .width(115.dp)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1E28))
            .clickable(onClick = onClick)
    ) {
        // Foto Karakter Memenuhi Seluruh Kartu
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.imageUrl.ifBlank { item.thumbnailUrl })
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime)
                .error(R.drawable.placeholder_yuranime)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Gradient Overlay Halus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x10000000),
                            Color(0x99000000),
                            Color(0xF0000000)
                        ),
                        startY = 90f
                    )
                )
        )

        // Overlay Teks di Pojok Kiri Bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp)
        ) {
            Text(
                text = birthDateText,
                color = Color(0xFFFF5722),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BirthdayShimmerRow() {
    val transition = rememberInfiniteTransition(label = "ultahShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ultahShimmerOffset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF15171F), Color(0xFF222531), Color(0xFF15171F)),
        start = Offset(translateAnim - 250f, 0f),
        end = Offset(translateAnim, 180f)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}
