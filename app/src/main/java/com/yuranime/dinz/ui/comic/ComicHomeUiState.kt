package com.yuranime.dinz.ui.comic

import com.yuranime.dinz.data.model.ComicBannerItem
import com.yuranime.dinz.data.model.ComicItem
import com.yuranime.dinz.data.model.ComicReadHistoryItem

data class ComicHomeUiState(
    val banners: List<ComicBannerItem> = emptyList(),
    val continueReading: List<ComicReadHistoryItem> = emptyList(),
    val trendingComics: List<ComicItem> = emptyList(),
    val latestComics: List<ComicItem> = emptyList(),
    val popularComics: List<ComicItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isSearchDialogVisible: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<ComicItem> = emptyList()
)
