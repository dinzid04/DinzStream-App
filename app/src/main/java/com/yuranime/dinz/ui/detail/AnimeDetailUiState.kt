package com.yuranime.dinz.ui.detail

import com.yuranime.dinz.data.model.CommunityMediaItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.MovieDetail
import com.yuranime.dinz.data.model.SeasonItem

enum class DetailTab(val label: String) {
    INFO("INFO"),
    EPISODE("EPISODE"),
    SEASON("SEASON"),
    CUPLIX("CUPLIX"),
    POSTER("POSTER"),
    COVER("COVER")
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
    val selectedTab: DetailTab = DetailTab.INFO,
    val isFavorite: Boolean = false,
    // Pagination & Search
    val episodeSearchQuery: String = "",
    val isEpisodeSortAscending: Boolean = false, // false = newest first, true = oldest first
    val currentEpisodePage: Int = 0,
    val isLoadingMoreEpisodes: Boolean = false,
    val hasMoreEpisodes: Boolean = true,
    // Full screen image preview
    val fullScreenPreviewUrl: String? = null
) {
    val filteredEpisodes: List<EpisodeItem>
        get() {
            var list = episodes
            if (episodeSearchQuery.isNotBlank()) {
                val q = episodeSearchQuery.trim()
                list = list.filter {
                    (it.index?.contains(q, ignoreCase = true) == true) ||
                    (it.title?.contains(q, ignoreCase = true) == true)
                }
            }
            return if (isEpisodeSortAscending) {
                list.sortedBy { it.index?.toIntOrNull() ?: 0 }
            } else {
                list.sortedByDescending { it.index?.toIntOrNull() ?: 0 }
            }
        }
}
