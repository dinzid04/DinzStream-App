package com.dinz.animein.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.model.AnimeItem
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
    private var searchExecutionJob: Job? = null

    init {
        loadExploreData()
    }

    fun refreshExploreData() {
        loadExploreData(isRefresh = true)
    }

    fun loadExploreData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

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
                    isRefreshing = false,
                    genres = genresResult.getOrDefault(emptyList()).shuffled(),
                    years = yearsResult.getOrDefault(emptyList()).shuffled(),
                    characters = charsResult.getOrDefault(emptyList()).shuffled(),
                    cuplixList = cuplixResult.getOrDefault(emptyList()),
                    recommendations = recResult.getOrDefault(emptyList()).shuffled()
                )
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update {
            it.copy(
                query = newQuery,
                isSearchActive = newQuery.isNotBlank() || it.activeFilter != null
            )
        }

        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(400)
            if (newQuery.isNotBlank() || _uiState.value.activeFilter != null) {
                executeCurrentSearch(page = 0, isInitial = true)
            }
        }
    }

    fun applyFilter(type: FilterType, value: String, label: String) {
        val filter = SearchFilter(type = type, value = value, label = label)
        _uiState.update {
            it.copy(
                activeFilter = filter,
                query = "",
                isSearchActive = true,
                selectedSubPage = null,
                currentPage = 0,
                canPaginate = true,
                searchResults = emptyList()
            )
        }
        executeCurrentSearch(page = 0, isInitial = true)
    }

    fun clearFilter() {
        val currentQuery = _uiState.value.query
        _uiState.update {
            it.copy(
                activeFilter = null,
                currentPage = 0,
                canPaginate = true
            )
        }
        if (currentQuery.isNotBlank()) {
            executeCurrentSearch(page = 0, isInitial = true)
        } else {
            _uiState.update {
                it.copy(
                    isSearchActive = false,
                    searchResults = emptyList()
                )
            }
        }
    }

    fun toggleLayout() {
        _uiState.update { it.copy(isGridLayout = !it.isGridLayout) }
    }

    fun cycleSort() {
        val nextSort = when (_uiState.value.sortOrder) {
            "views" -> "alphabet"
            "alphabet" -> "new"
            else -> "views"
        }
        _uiState.update { it.copy(sortOrder = nextSort, currentPage = 0, canPaginate = true) }
        executeCurrentSearch(page = 0, isInitial = true)
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
        searchDebounceJob?.cancel()
        searchExecutionJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                selectedSubPage = null,
                activeFilter = null,
                isSearchActive = false,
                currentPage = 0,
                canPaginate = true,
                searchResults = emptyList(),
                errorMessage = null
            )
        }
    }

    fun executeSearch() {
        executeCurrentSearch(page = 0, isInitial = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isPaginating || !state.canPaginate || !state.isSearchActive) return
        val nextPage = state.currentPage + 1
        executeCurrentSearch(page = nextPage, isInitial = false)
    }

    private fun executeCurrentSearch(page: Int, isInitial: Boolean) {
        val current = _uiState.value
        val activeFilter = current.activeFilter
        val keyword = current.query.ifBlank { null }

        // If no filter and no keyword, reset search results
        if (activeFilter == null && keyword == null) {
            _uiState.update { it.copy(isSearchActive = false, searchResults = emptyList()) }
            return
        }

        searchExecutionJob?.cancel()
        searchExecutionJob = viewModelScope.launch {
            if (isInitial) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isPaginating = true) }
            }

            val sortParam = current.sortOrder

            val result: Result<List<AnimeItem>> = when {
                activeFilter != null -> {
                    when (activeFilter.type) {
                        FilterType.GENRE -> {
                            val id = activeFilter.value.toIntOrNull()?.toString()
                            if (id != null) {
                                repository.exploreMovieGenre(idGenre = id, sort = sortParam, page = page)
                            } else {
                                repository.exploreMovieGenre(genreIn = activeFilter.value, sort = sortParam, page = page)
                            }
                        }
                        FilterType.STUDIO -> {
                            repository.exploreMovieStudio(studio = activeFilter.value, sort = sortParam, page = page)
                        }
                        FilterType.YEAR -> {
                            repository.exploreMovieYear(year = activeFilter.value, sort = sortParam, page = page)
                        }
                        FilterType.TYPE -> {
                            repository.exploreMovieType(type = activeFilter.value, sort = sortParam, page = page)
                        }
                        FilterType.CHARACTER -> {
                            repository.searchMovies(keyword = activeFilter.value, sort = sortParam, page = page)
                        }
                    }
                }
                keyword != null -> {
                    repository.searchMovies(keyword = keyword, sort = sortParam, page = page)
                }
                else -> {
                    Result.success(emptyList())
                }
            }

            if (result.isSuccess) {
                val newItems = result.getOrDefault(emptyList())
                _uiState.update { prev ->
                    val combined = if (isInitial) {
                        newItems
                    } else {
                        val existingIds = prev.searchResults.mapNotNull { it.id }.toSet()
                        val filteredNew = newItems.filter { it.id == null || !existingIds.contains(it.id) }
                        prev.searchResults + filteredNew
                    }
                    prev.copy(
                        isLoading = false,
                        isPaginating = false,
                        searchResults = combined,
                        currentPage = page,
                        canPaginate = newItems.isNotEmpty(),
                        isSearchActive = true,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        isPaginating = false,
                        errorMessage = if (isInitial) result.exceptionOrNull()?.message ?: "Gagal memuat hasil filter" else null
                    )
                }
            }
        }
    }
}
