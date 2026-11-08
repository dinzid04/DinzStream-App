package com.yuranime.dinz.ui.jadwal

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.CuplixAvatarItem
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.liquidGlassBackdrop
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorMetricFavorites
import com.yuranime.dinz.ui.theme.ColorMetricTime
import com.yuranime.dinz.ui.theme.ColorMetricViews
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary

@Composable
fun JadwalScreen(
    modifier: Modifier = Modifier,
    viewModel: JadwalViewModel = viewModel(),
    selectedTab: NavTab = NavTab.JADWAL,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onCuplixClick: (CuplixItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backdropLayer = rememberGraphicsLayer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdrop(backdropLayer)
                .background(ColorBackgroundPrimary)
                .statusBarsPadding()
        ) {
            // Days Tab Row (SEN, SEL, RAB, KAM, JUM, SAB, MIN)
            val days = ScheduleDay.entries
            val selectedIndex = days.indexOf(uiState.selectedDay).coerceAtLeast(0)

            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = ColorBackgroundPrimary,
                contentColor = ColorAccentPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedIndex < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                .height(2.5.dp)
                                .background(ColorAccentPrimary)
                        )
                    }
                },
                divider = {}
            ) {
                days.forEachIndexed { index, day ->
                    val isSelected = index == selectedIndex
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectDay(day) },
                        text = {
                            Text(
                                text = day.label,
                                color = if (isSelected) ColorAccentPrimary else ColorTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cuplix Bar
            if (uiState.cuplixList.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(uiState.cuplixList, key = { it.id ?: it.hashCode().toString() }) { cuplixItem ->
                        CuplixAvatarItem(
                            item = cuplixItem,
                            onClick = { onCuplixClick(cuplixItem) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Scheduled Anime List
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.scheduledAnimeList.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ColorAccentPrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    uiState.scheduledAnimeList.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada jadwal anime untuk hari ${uiState.selectedDay.label}.",
                                color = ColorTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 115.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.scheduledAnimeList,
                                key = { it.id ?: it.hashCode().toString() }
                            ) { anime ->
                                ScheduledAnimeCard(
                                    anime = anime,
                                    onClick = { anime.id?.let { onAnimeClick(it) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            backdropLayer = backdropLayer,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ScheduledAnimeCard(
    anime: AnimeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Poster (2:3 aspect ratio)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.getEffectivePoster())
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime)
                .error(R.drawable.placeholder_yuranime)
                .build(),
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(115.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141720))
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Info Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
        ) {
            // Genre (Orange #FF5722)
            Text(
                text = anime.getDisplayGenre(),
                color = ColorAccentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Title (White Bold)
            Text(
                text = anime.title ?: "-",
                color = ColorTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Synopsis
            if (!anime.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = anime.synopsis,
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Views & Favorites
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Views
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = ColorMetricViews,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${anime.views ?: "0"} views",
                        color = ColorMetricViews,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Favorites
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ColorMetricFavorites,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${anime.favorites ?: "0"} favorites",
                        color = ColorMetricFavorites,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Airing Countdown (Purple #A855F7)
            if (!anime.time.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = ColorMetricTime,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = anime.time,
                        color = ColorMetricTime,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
