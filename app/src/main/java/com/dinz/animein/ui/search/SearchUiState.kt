package com.dinz.animein.ui.search

import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.ExploreCharacterItem
import com.dinz.animein.data.model.ExploreGenreItem
import com.dinz.animein.data.model.ExploreYearItem

enum class ExploreSubPage {
    KATEGORI,
    TAHUN,
    KARAKTER
}

data class SearchUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val selectedSubPage: ExploreSubPage? = null,
    val selectedGenre: String? = null,
    val selectedType: String? = null,
    val selectedStudio: String? = null,
    val selectedYear: String? = null,
    val isGridLayout: Boolean = false,
    val sortOrder: String = "views",
    val isLoading: Boolean = false,
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
