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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
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
import com.dinz.animein.ui.theme.ColorMetricReleaseDate
import com.dinz.animein.ui.theme.ColorMetricTime
import com.dinz.animein.ui.theme.ColorMetricViews
import com.dinz.animein.ui.theme.ColorTextPrimary

@Composable
fun AnimeVerticalCard(
    anime: AnimeItem,
    modifier: Modifier = Modifier,
    overrideSubtitle: String? = null,
    showAiringTime: Boolean = false,
    showReleaseDate: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(115.dp)
            .clickable(onClick = onClick)
    ) {
        // Poster 2:3 aspect ratio with 8dp rounded corner
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.getEffectivePoster())
                .crossfade(true)
                .build(),
            contentDescription = anime.title ?: "Poster Anime",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Episode or Genre in Orange (#FF5722)
        val subtitle = overrideSubtitle ?: anime.index?.let { "Episode $it" } ?: anime.getDisplayGenre()
        Text(
            text = subtitle,
            color = ColorAccentPrimary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(1.dp))

        // Anime Title (Bold White, max 2 lines, ellipsis)
        Text(
            text = anime.title ?: "-",
            color = ColorTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        if (showReleaseDate) {
            // Optional Release Date in Blue (#3B82F6) - Paling Dinanti only shows Release Date & Favorites
            val dateText = anime.airedStart ?: "2026-10-??"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = ColorMetricReleaseDate,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = dateText,
                    color = ColorMetricReleaseDate,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Views row (Red #EF4444)
            val viewsCount = anime.views ?: "0"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = ColorMetricViews,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$viewsCount views",
                    color = ColorMetricViews,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Favorites row (Yellow #FACC15)
        val favoritesCount = anime.favorites ?: "0"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = ColorMetricFavorites,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$favoritesCount favorites",
                color = ColorMetricFavorites,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Optional Airing Time in Purple (#A855F7) - Jadwal Hari Ini shows purple clock at bottom
        if (showAiringTime) {
            Spacer(modifier = Modifier.height(2.dp))
            val timeText = if (!anime.time.isNullOrBlank()) anime.time else "new !!"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = ColorMetricTime,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = timeText,
                    color = ColorMetricTime,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
