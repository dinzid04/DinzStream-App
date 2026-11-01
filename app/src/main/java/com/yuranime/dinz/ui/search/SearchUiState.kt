package com.yuranime.dinz.ui.search

import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.ExploreCharacterItem
import com.yuranime.dinz.data.model.ExploreGenreItem
import com.yuranime.dinz.data.model.ExploreYearItem

enum class ExploreSubPage {
    KATEGORI,
    TAHUN,
    KARAKTER
}

enum class FilterType {
    GENRE,
    STUDIO,
    YEAR,
    TYPE,
    CHARACTER
}

data class SearchFilter(
    val type: FilterType,
    val value: String,
    val label: String
)

data class SearchUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val activeFilter: SearchFilter? = null,
    val selectedSubPage: ExploreSubPage? = null,
    val isGridLayout: Boolean = false,
    val sortOrder: String = "views",
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val canPaginate: Boolean = true,
    val currentPage: Int = 0,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    // Explore sections
    val genres: List<ExploreGenreItem> = emptyList(),
    val years: List<ExploreYearItem> = emptyList(),
    val characters: List<ExploreCharacterItem> = emptyList(),
    val cuplixList: List<CuplixItem> = emptyList(),
    // Search Results & Recommendations
    val searchResults: List<AnimeItem> = emptyList(),
    val recommendations: List<AnimeItem> = emptyList()
)
