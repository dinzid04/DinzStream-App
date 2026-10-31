package com.dinz.animein.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.R
import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.ui.theme.SfProFontFamily

@Composable
fun PopularLeaderboardCard(
    anime: AnimeItem,
    rank: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(14.dp)
    val cardBg = Color(0xFF0F1420)

    // Border styling based on ranking
    val borderModifier = when (rank) {
        1 -> Modifier.border(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24), Color(0x66F59E0B))
            ),
            shape = cardShape
        )
        2 -> Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0x4094A3B8))
            ),
            shape = cardShape
        )
        3 -> Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0x40D97706))
            ),
            shape = cardShape
        )
        else -> Modifier.border(
            width = 0.8.dp,
            color = Color(0xFF1E2536),
            shape = cardShape
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .then(borderModifier)
            .clip(cardShape)
            .background(cardBg)
            .clickable(onClick = onClick)
    ) {
        // 1. Right-side Anime Cover Artwork with Smooth Horizontal Gradient Fade
        val coverUrl = anime.imageCover ?: anime.getEffectiveCover()
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .align(Alignment.CenterEnd)
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

            // Horizontal gradient overlay fading cover into card background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                cardBg,
                                cardBg.copy(alpha = 0.95f),
                                cardBg.copy(alpha = 0.60f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // 2. Foreground Content (Rank Numeral, Title & Stars Rating)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized Rank Numeral on the left
            Box(
                modifier = Modifier.width(42.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val (numColor, numSize) = when (rank) {
                    1 -> Color(0xFFFDE68A) to 34.sp
                    2 -> Color(0xFFFEF3C7) to 34.sp
                    3 -> Color(0xFFFCD34D) to 34.sp
                    else -> Color(0xFF334155) to 30.sp
                }

                Text(
                    text = rank.toString(),
                    color = numColor,
                    fontSize = numSize,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Metadata Column (Title in All-Caps + 5 Gold Stars + Favorites Count)
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.60f), // prevent text from overlapping too far into bright artwork
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = (anime.title ?: "-").uppercase(),
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★★★★★",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val favCount = anime.favorites?.takeIf { it.isNotBlank() } ?: "0"
                    Text(
                        text = favCount,
                        color = Color(0xFFFCD34D),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}
