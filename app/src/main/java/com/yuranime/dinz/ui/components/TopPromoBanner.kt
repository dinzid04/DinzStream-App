package com.yuranime.dinz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily
import kotlinx.coroutines.delay

/**
 * YuranimeHeroSlider: Slider Home Sinematik Yuranime (TAHAP 2)
 *
 * Mengikuti spesifikasi:
 * 1. Background Cover dengan overlay gelap tingkat sedang (ilustrasi cover tetap jelas terlihat).
 * 2. Poster vertikal (aspectRatio 2:3, rounded 10.dp, lebar 110.dp) di pojok kiri bawah.
 * 3. Kolom teks kanan dibatasi agar tidak pernah melewati batas atas poster (Modifier.heightIn(max = 165.dp)).
 * 4. Judul anime menggunakan font Yuranime-logo-font.ttf (Calistoga-Regular), 1-2 baris.
 * 5. Sinopsis ringkas warna abu-abu terang.
 * 6. Baris bawah berisi Tombol Kapsul Merah "TONTON SEKARANG" (klik -> onAnimeClick) dan
 *    indikator pagination dots sejajar di sisi kanan.
 */
@Composable
fun YuranimeHeroSlider(
    items: List<AnimeItem>,
    modifier: Modifier = Modifier,
    onAnimeClick: (id: String) -> Unit = {}
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-scroll loop every 5 seconds
    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(5000)
            if (pagerState.pageCount > 1) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val posterWidth = 110.dp
    val posterHeight = 165.dp // 110 * 3 / 2 = 165.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(385.dp)
            .background(ColorBackgroundPrimary)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val anime = items[page]
            val coverUrl = anime.getEffectiveCover()
            val posterUrl = anime.getEffectivePoster()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { anime.id?.let { onAnimeClick(it) } }
            ) {
                // 1. Background Cover Anime
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime_wide)
                        .error(R.drawable.placeholder_yuranime_wide)
                        .build(),
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Overlay Gelap Tingkat Sedang (Cover tetap terlihat jelas)
                // Top vignette for logo & status bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xC0000000),
                                    Color(0x60000000),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Medium subtle radial/center shade & bottom blend to #0B0B0B
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x20000000),
                                    Color(0x700B0B0B),
                                    Color(0xD90B0B0B),
                                    ColorBackgroundPrimary
                                )
                            )
                        )
                )

                // 3. Foreground Row: Poster di Kiri Bawah + Kolom Info di Kanan
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Kartu Poster Vertikal (2:3 aspect ratio, rounded 10.dp)
                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .height(posterHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { anime.id?.let { onAnimeClick(it) } }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(posterUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_yuranime)
                                .error(R.drawable.placeholder_yuranime)
                                .build(),
                            contentDescription = anime.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(13.dp))

                    // Kolom Informasi Sebelah Kanan Poster
                    // Dibatasi dengan heightIn(max = posterHeight) agar tidak pernah melewati batas atas poster!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = posterHeight)
                            .padding(bottom = 2.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Judul Anime menggunakan Yuranime-logo-font.ttf (Calistoga-Regular)
                        val rawTitle = (anime.title ?: "-").uppercase()
                        val titleFontSize = if (rawTitle.length > 20) 20.sp else 23.sp
                        val titleLineHeight = if (rawTitle.length > 20) 24.sp else 27.sp

                        Text(
                            text = rawTitle,
                            color = Color.White,
                            fontSize = titleFontSize,
                            fontFamily = YuranimeLogoFontFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = titleLineHeight
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Sinopsis Singkat
                        val synText = anime.synopsis
                            ?: "Tonton streaming anime terupdate dengan kualitas terbaik dan subtitle Indonesia resmi di platform Yuranime."
                        Text(
                            text = synText,
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Baris Bawah: Tombol TONTON SEKARANG di kiri & Pagination Dots di kanan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tombol Kapsul Merah "TONTON SEKARANG"
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48))
                                    .clickable { anime.id?.let { onAnimeClick(it) } }
                                    .padding(horizontal = 13.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TONTON SEKARANG",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Indikator Titik Slide (Pagination Dots)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val count = items.size.coerceAtMost(6)
                                for (i in 0 until count) {
                                    val isSelected = (pagerState.currentPage % count) == i
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(4.5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x66CBD5E1))
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

// Backward-compatibility alias
@Composable
fun PiuPiuHeroSlider(
    items: List<AnimeItem>,
    modifier: Modifier = Modifier,
    onAnimeClick: (id: String) -> Unit = {}
) {
    YuranimeHeroSlider(
        items = items,
        modifier = modifier,
        onAnimeClick = onAnimeClick
    )
}
