package com.dinz.animein.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.repository.AnimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadExploreData()
    }

    fun loadExploreData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val genresDeferred = async { repository.getExploreGenres() }
            val yearsDeferred = async { repository.getExploreYears() }
            val charsDeferred = async { repository.getExploreCharacters() }
            val cuplixDeferred = async { repository.getCuplixFeed(limit = 15) }
            val recommendationsDeferred = async { repository.searchMovies(sort = "popular") }

            val genresResult = genresDeferred.await()
            val yearsResult = yearsDeferred.await()
            val charsResult = charsDeferred.await()
            val cuplixResult = cuplixDeferred.await()
            val recResult = recommendationsDeferred.await()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    genres = genresResult.getOrDefault(emptyList()),
                    years = yearsResult.getOrDefault(emptyList()),
                    characters = charsResult.getOrDefault(emptyList()),
                    cuplixList = cuplixResult.getOrDefault(emptyList()),
                    recommendations = recResult.getOrDefault(emptyList())
                )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update {
            it.copy(
                query = newQuery,
                isSearchActive = newQuery.isNotBlank() || it.selectedGenre != null
            )
        }

        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(400)
            if (newQuery.isNotBlank() || _uiState.value.selectedGenre != null) {
                executeSearch()
            }
        }
    }

    fun selectGenre(genreName: String?) {
        val nextGenre = if (_uiState.value.selectedGenre == genreName) null else genreName
        _uiState.update {
            it.copy(
                selectedGenre = nextGenre,
                isSearchActive = nextGenre != null || it.query.isNotBlank()
            )
        }
        executeSearch()
    }

    fun selectType(type: String?) {
        val nextType = if (_uiState.value.selectedType == type) null else type
        _uiState.update {
            it.copy(
                selectedType = nextType,
                isSearchActive = nextType != null || it.query.isNotBlank()
            )
        }
        executeSearch()
    }

    fun selectStudio(studio: String?) {
        val nextStudio = if (_uiState.value.selectedStudio == studio) null else studio
        _uiState.update {
            it.copy(
                selectedStudio = nextStudio,
                query = nextStudio ?: it.query,
                isSearchActive = true
            )
        }
        executeSearch()
    }

    fun toggleLayout() {
        _uiState.update { it.copy(isGridLayout = !it.isGridLayout) }
    }

    fun cycleSort() {
        val nextSort = when (_uiState.value.sortOrder) {
            "views" -> "popular"
            "popular" -> "new"
            else -> "views"
        }
        _uiState.update { it.copy(sortOrder = nextSort) }
        executeSearch()
    }

    fun openSubPage(page: ExploreSubPage) {
        _uiState.update { it.copy(selectedSubPage = page) }
    }

    fun closeSubPage() {
        _uiState.update { it.copy(selectedSubPage = null) }
    }

    fun activateSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                query = "",
                selectedSubPage = null,
                selectedGenre = null,
                selectedType = null,
                selectedStudio = null,
                isSearchActive = false,
                searchResults = emptyList()
            )
        }
    }

    fun executeSearch() {
        val current = _uiState.value
        val keyword = current.query.ifBlank { null }
        val genre = current.selectedGenre
        val type = current.selectedType

        if (keyword == null && genre == null && type == null) {
            _uiState.update { it.copy(isSearchActive = false, searchResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.searchMovies(
                keyword = keyword,
                genreIn = genre,
                type = type,
                sort = current.sortOrder
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        searchResults = result.getOrDefault(emptyList()),
                        isSearchActive = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gagal memuat hasil pencarian"
                    )
                }
            }
        }
    }
}
