package com.dinz.animein.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.ui.theme.ColorAccentPrimary
import com.dinz.animein.ui.theme.ColorMetricFavorites
import com.dinz.animein.ui.theme.ColorMetricViews
import com.dinz.animein.ui.theme.ColorTextPrimary

@Composable
fun AnimeWideCard(
    anime: AnimeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(235.dp)
            .clickable(onClick = onClick)
    ) {
        // Landscape Banner (16:9) with 10dp rounded corners
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.getEffectiveCover())
                .crossfade(true)
                .build(),
            contentDescription = anime.title ?: "Banner Anime",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info area: Left thumbnail + Right metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enlarged vertical poster thumbnail on the left (50dp x 74dp) to match text height
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.getEffectivePoster())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(50.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Right side column: Genre + Title + 1-line Views & Favorites
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Genre (Orange #FF5722)
                Text(
                    text = anime.getDisplayGenre(),
                    color = ColorAccentPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Title (Bold White, 2 lines max, ellipsis)
                Text(
                    text = anime.title ?: "-",
                    color = ColorTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1 Unified Row with Views (Red) + Favorites (Gold)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = ColorMetricViews,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = anime.views ?: "0",
                        color = ColorMetricViews,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ColorMetricFavorites,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = anime.favorites ?: "0",
                        color = ColorMetricFavorites,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
