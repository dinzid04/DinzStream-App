package com.dinz.animein.ui.home

import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.data.model.ContinueWatchingItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.SliderItem

data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sliderItem: SliderItem? = null,
    val continueWatchingList: List<ContinueWatchingItem> = emptyList(),
    val cuplixList: List<CuplixItem> = emptyList(),
    val newEpisodesList: List<AnimeItem> = emptyList(),
    val hotAnimeList: List<AnimeItem> = emptyList(),
    val newTitlesList: List<AnimeItem> = emptyList(),
    val todayScheduleList: List<AnimeItem> = emptyList(),
    val jatForYuList: List<AnimeItem> = emptyList(),
    val waitingAnimeList: List<AnimeItem> = emptyList(),
    val popularAnimeList: List<AnimeItem> = emptyList(),
    val manreList: List<AnimeItem> = emptyList()
)
