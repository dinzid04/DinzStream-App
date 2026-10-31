package com.dinz.animein.ui.detail

import com.dinz.animein.data.model.CommunityMediaItem
import com.dinz.animein.data.model.ContributorItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.DiscussionItem
import com.dinz.animein.data.model.EpisodeItem
import com.dinz.animein.data.model.MovieDetail
import com.dinz.animein.data.model.SeasonItem

enum class DetailTab(val label: String) {
    INFO("INFO"),
    EPISODE("EPISODE"),
    SEASON("SEASON"),
    DISCUSSION("DISCUSSION"),
    CUPLIX("CUPLIX"),
    POSTER("POSTER"),
    COVER("COVER"),
    CONTRIBUTOR("CONTRIBUTOR")
}

data class AnimeDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val movieDetail: MovieDetail? = null,
    val latestEpisode: EpisodeItem? = null,
    val seasons: List<SeasonItem> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
    val cuplixList: List<CuplixItem> = emptyList(),
    val posters: List<CommunityMediaItem> = emptyList(),
    val covers: List<CommunityMediaItem> = emptyList(),
    val discussions: List<DiscussionItem> = emptyList(),
    val contributors: List<ContributorItem> = emptyList(),
    val selectedTab: DetailTab = DetailTab.INFO,
    val isFavorite: Boolean = false
)
