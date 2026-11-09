package com.yuranime.dinz.ui.comic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.repository.ComicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComicHomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = ComicRepository()
    private val localDb = LocalAuthDatabase(application)

    private val _uiState = MutableStateFlow(ComicHomeUiState())
    val uiState: StateFlow<ComicHomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            // Muat riwayat baca dari database lokal
            val history = try {
                localDb.getComicReadHistory()
            } catch (_: Exception) {
                emptyList()
            }

            coroutineScope {
                val bannersDeferred = async { repository.getBanners(forceRefresh) }
                val trendingDeferred = async { repository.getTrendingComics(page = 1, pageSize = 12, forceRefresh = forceRefresh) }
                val latestDeferred = async { repository.getLatestComics(page = 1, pageSize = 12, forceRefresh = forceRefresh) }
                val popularDeferred = async { repository.getPopularAllTimeComics(page = 1, pageSize = 15, forceRefresh = forceRefresh) }

                val bannersRes = bannersDeferred.await()
                val trendingRes = trendingDeferred.await()
                val latestRes = latestDeferred.await()
                val popularRes = popularDeferred.await()

                _uiState.update {
                    it.copy(
                        banners = bannersRes.getOrDefault(emptyList()),
                        continueReading = history,
                        trendingComics = trendingRes.getOrDefault(emptyList()),
                        latestComics = latestRes.getOrDefault(emptyList()),
                        popularComics = popularRes.getOrDefault(emptyList()),
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (bannersRes.isFailure && latestRes.isFailure) "Gagal memuat konten komik" else null
                    )
                }
            }
        }
    }

    fun refreshHistory() {
        viewModelScope.launch {
            try {
                val history = localDb.getComicReadHistory()
                _uiState.update { it.copy(continueReading = history) }
            } catch (_: Exception) {}
        }
    }

    fun openSearchDialog() {
        _uiState.update { it.copy(isSearchDialogVisible = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearchDialog() {
        _uiState.update { it.copy(isSearchDialogVisible = false, searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length >= 2) {
            executeSearch(query.trim())
        } else if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun executeSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val res = repository.searchComics(query)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchResults = res.getOrDefault(emptyList())
                )
            }
        }
    }
}
