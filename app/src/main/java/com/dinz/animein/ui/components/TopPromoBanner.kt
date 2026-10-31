package com.dinz.animein.ui.components

import androidx.compose.foundation.background
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
import com.dinz.animein.R
import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.ui.theme.ColorBackgroundPrimary
import com.dinz.animein.ui.theme.ColorTextPrimary
import com.dinz.animein.ui.theme.ColorTextSecondary
import com.dinz.animein.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay

@Composable
fun PiuPiuHeroSlider(
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
            .aspectRatio(16f / 11f) // Full width header aspect ratio
            .background(ColorBackgroundPrimary)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val anime = items[page]
            val coverUrl = anime.getEffectiveCover()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { anime.id?.let { onAnimeClick(it) } }
            ) {
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

                // Smooth gradient overlay at the bottom fading to background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0x990B0B0B),
                                    Color(0xF00B0B0B),
                                    ColorBackgroundPrimary
                                )
                            )
                        )
                )

                // Title and Synopsis
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = anime.title ?: "-",
                        color = ColorTextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val synText = anime.synopsis
                        ?: "Tonton streaming anime terupdate dengan kualitas terbaik dan subtitle Indonesia resmi di platform Yuranime."
                    Text(
                        text = synText,
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }
        }

        // Pagination Dots at bottom right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp),
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
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0x66FFFFFF))
                    )
                }
            }
        }
    }
}
