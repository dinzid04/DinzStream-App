package com.dinz.animein.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dinz.animein.ui.components.AnimeVerticalCard
import com.dinz.animein.ui.components.AnimeWideCard
import com.dinz.animein.ui.components.BottomNavBar
import com.dinz.animein.ui.components.ContinueWatchingCard
import com.dinz.animein.ui.components.CuplixAvatarItem
import com.dinz.animein.ui.components.HomeErrorState
import com.dinz.animein.ui.components.HomeShimmerLoading
import com.dinz.animein.ui.components.NavTab
import com.dinz.animein.ui.components.SectionHeader
import com.dinz.animein.ui.components.TopPromoBanner
import com.dinz.animein.ui.theme.ColorBackgroundPrimary

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onAnimeClick: (id: String) -> Unit = {},
    onCuplixClick: (id: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackgroundPrimary,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ColorBackgroundPrimary)
        ) {
            when {
                uiState.isLoading && uiState.newEpisodesList.isEmpty() -> {
                    HomeShimmerLoading()
                }

                uiState.errorMessage != null && uiState.newEpisodesList.isEmpty() -> {
                    HomeErrorState(
                        errorMessage = uiState.errorMessage ?: "",
                        onRetry = { viewModel.loadHomeData() }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 1. Top Promo Banner (16:9 / 21:9 with rounded-xl)
                        item(key = "top_promo_banner") {
                            TopPromoBanner(
                                sliderItem = uiState.sliderItem,
                                onClick = {
                                    uiState.sliderItem?.idMovie?.let { onAnimeClick(it) }
                                }
                            )
                        }

                        // 2. Section "Lanjut Nonton"
                        if (uiState.continueWatchingList.isNotEmpty()) {
                            item(key = "section_continue_watching") {
                                Spacer(modifier = Modifier.height(10.dp))
                                SectionHeader(
                                    title = "Lanjut Nonton",
                                    onChevronClick = { /* Navigate to watch history */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.continueWatchingList,
                                        key = { it.id }
                                    ) { item ->
                                        ContinueWatchingCard(
                                            item = item,
                                            onClick = { onAnimeClick(item.idMovie) }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Section "Cuplix" (Horizontal row of circular items)
                        if (uiState.cuplixList.isNotEmpty()) {
                            item(key = "section_cuplix") {
                                Spacer(modifier = Modifier.height(14.dp))
                                SectionHeader(
                                    title = "Cuplix",
                                    showChevron = false
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(
                                        items = uiState.cuplixList,
                                        key = { it.id ?: it.hashCode().toString() }
                                    ) { cuplixItem ->
                                        CuplixAvatarItem(
                                            item = cuplixItem,
                                            onClick = {
                                                cuplixItem.id?.let { onCuplixClick(it) }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Section "Episode Baru" (2:3 Vertical Cards)
                        if (uiState.newEpisodesList.isNotEmpty()) {
                            item(key = "section_new_episodes") {
                                Spacer(modifier = Modifier.height(14.dp))
                                SectionHeader(
                                    title = "Episode Baru",
                                    onChevronClick = { /* See All Episode Baru */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.newEpisodesList,
                                        key = { "new_ep_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.index?.let { "Episode $it" } ?: "Episode Baru",
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Section "Sedang Hangat" (16:9 Landscape Wide Cards)
                        if (uiState.hotAnimeList.isNotEmpty()) {
                            item(key = "section_hot_anime") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Sedang Hangat",
                                    onChevronClick = { /* See All Sedang Hangat */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.hotAnimeList,
                                        key = { "hot_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeWideCard(
                                            anime = anime,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Section "Judul Baru" (2:3 Vertical Cards)
                        if (uiState.newTitlesList.isNotEmpty()) {
                            item(key = "section_new_titles") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Judul Baru",
                                    onChevronClick = { /* See All Judul Baru */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.newTitlesList,
                                        key = { "new_title_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.getDisplayGenre(),
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Section "Jadwal Hari Ini" (2:3 Vertical Cards + Purple Airing Time)
                        if (uiState.todayScheduleList.isNotEmpty()) {
                            item(key = "section_today_schedule") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Jadwal Hari Ini",
                                    onChevronClick = { /* See All Schedule */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.todayScheduleList,
                                        key = { "today_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.getDisplayGenre(),
                                            showAiringTime = true,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 8. Section "Jat For Yu" (16:9 Landscape Wide Cards)
                        if (uiState.jatForYuList.isNotEmpty()) {
                            item(key = "section_jat_for_yu") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Jat For Yu",
                                    onChevronClick = { /* See All Jat For Yu */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.jatForYuList,
                                        key = { "jat_for_yu_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeWideCard(
                                            anime = anime,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 9. Section "Paling Dinanti" (2:3 Vertical Cards + Blue Release Date)
                        if (uiState.waitingAnimeList.isNotEmpty()) {
                            item(key = "section_waiting_anime") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Paling Dinanti",
                                    onChevronClick = { /* See All Upcoming */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.waitingAnimeList,
                                        key = { "waiting_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.getDisplayGenre(),
                                            showReleaseDate = true,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 10. Section "Paling Populer" (2:3 Vertical Cards)
                        if (uiState.popularAnimeList.isNotEmpty()) {
                            item(key = "section_popular_anime") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Paling Populer",
                                    onChevronClick = { /* See All Popular */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.popularAnimeList,
                                        key = { "popular_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.getDisplayGenre(),
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 11. Section "Baca Manre" (2:3 Vertical Cards)
                        if (uiState.manreList.isNotEmpty()) {
                            item(key = "section_baca_manre") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Baca Manre",
                                    onChevronClick = { /* See All Manre */ }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.manreList,
                                        key = { "manre_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.getDisplayGenre(),
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
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
