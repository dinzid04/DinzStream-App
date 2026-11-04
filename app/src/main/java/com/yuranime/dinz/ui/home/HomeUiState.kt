package com.yuranime.dinz.ui.home

import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.ContinueWatchingItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.SliderItem

enum class HomeSectionType(val title: String, val sortKey: String) {
    HOT("Sedang Hangat", "hot"),
    NEW_EPISODES("Episode Baru", "new"),
    NEW_TITLES("Judul Baru", "id"),
    JAT_FOR_YU("Jat For Yu", "random"),
    WAITING("Paling Dinanti", "waiting"),
    POPULAR("Paling Populer", "views"),
    TODAY("Jadwal Hari Ini", "today")
}

data class HomeSectionDetailState(
    val sectionType: HomeSectionType,
    val items: List<AnimeItem> = emptyList(),
    val page: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasReachedEnd: Boolean = false
)

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
    val manreList: List<AnimeItem> = emptyList(),
    // Birthday Today Section (Otakotaku Scraper)
    val birthdayToday: com.yuranime.dinz.data.api.BirthdayTodayData? = null,
    val isBirthdayLoading: Boolean = false,
    val birthdayError: String? = null,
    // Horizontal row pagination state
    val horizontalPageMap: Map<HomeSectionType, Int> = emptyMap(),
    val isLoadingSection: Map<HomeSectionType, Boolean> = emptyMap(),
    // Active full vertical sub-page state for chevron '>'
    val activeSectionDetail: HomeSectionDetailState? = null
)
