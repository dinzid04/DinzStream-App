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
import kotlinx.coroutines.delay

/**
 * YuranimeHeroSlider: Slider Home Sinematik Yuranime
 * Mengacu pada desain referensi: poster anime di sisi kiri, judul tebal, deskripsi singkat,
 * tombol CTA "TONTON SEKARANG" merah brand Yuranime, dan pagination indicator.
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(365.dp)
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
                // 1. Full Backdrop Landscape Cover Image
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

                // 2. Dual Gradient Overlays:
                // Top gradient (for status bar & header logo readability)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE6000000),
                                    Color(0x88000000),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom gradient (smoothly blends into app surface)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x220B0B0B),
                                    Color(0x990B0B0B),
                                    Color(0xF00B0B0B),
                                    ColorBackgroundPrimary
                                )
                            )
                        )
                )

                // 3. Foreground Content: Poster Card on Left + Title, Synopsis & CTA on Right
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Vertical Anime Poster Card (2:3 aspect ratio)
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
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

                    // Right Metadata & Action Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 2.dp)
                    ) {
                        // Title (Bold Uppercase)
                        Text(
                            text = (anime.title ?: "-").uppercase(),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 23.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Synopsis
                        val synText = anime.synopsis
                            ?: "Tonton streaming anime terupdate dengan kualitas terbaik dan subtitle Indonesia resmi di platform Yuranime."
                        Text(
                            text = synText,
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontFamily = SfProFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // CTA Button "TONTON SEKARANG"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE23441))
                                .clickable { anime.id?.let { onAnimeClick(it) } }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TONTON SEKARANG",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pagination Dots at bottom right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = items.size.coerceAtMost(6)
            for (i in 0 until count) {
                val isSelected = (pagerState.currentPage % count) == i
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0x66FFFFFF))
                    )
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
