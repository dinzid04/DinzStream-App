package com.dinz.animein.ui.search

import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.ExploreCharacterItem
import com.dinz.animein.data.model.ExploreGenreItem
import com.dinz.animein.data.model.ExploreYearItem

data class SearchUiState(
    val query: String = "",
    val isSearchActive: Boolean = false,
    val selectedGenre: String? = null,
    val selectedType: String? = null,
    val selectedStudio: String? = null,
    val isGridLayout: Boolean = false,
    val sortOrder: String = "views",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Explore sections
    val genres: List<ExploreGenreItem> = emptyList(),
    val years: List<ExploreYearItem> = emptyList(),
    val characters: List<ExploreCharacterItem> = emptyList(),
    val cuplixList: List<CuplixItem> = emptyList(),
    // Search Results
    val searchResults: List<AnimeItem> = emptyList()
)
