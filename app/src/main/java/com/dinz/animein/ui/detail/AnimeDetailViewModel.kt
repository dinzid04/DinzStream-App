package com.dinz.animein.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnimeDetailViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = _uiState.asStateFlow()

    private var currentMovieId: String? = null

    fun loadAnimeDetail(movieId: String) {
        if (currentMovieId == movieId && !_uiState.value.isLoading && _uiState.value.movieDetail != null) {
            return
        }
        currentMovieId = movieId
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                currentEpisodePage = 0,
                hasMoreEpisodes = true,
                episodes = emptyList()
            )
        }

        viewModelScope.launch {
            val detailResult = repository.getMovieDetail(movieId)
            if (detailResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = detailResult.exceptionOrNull()?.message ?: "Gagal memuat detail anime"
                    )
                }
                return@launch
            }

            val data = detailResult.getOrNull()
            val movie = data?.movie
            val latestEp = data?.latestEpisode
            val seasons = data?.seasons ?: emptyList()
            val isFav = movie?.isFav == "1"

            _uiState.update {
                it.copy(
                    isLoading = false,
                    movieDetail = movie,
                    latestEpisode = latestEp,
                    seasons = seasons,
                    isFavorite = isFav
                )
            }

            // Fetch secondary tabs concurrently
            val episodesDeferred = async { repository.getMovieEpisodes(movieId, page = 0) }
            val cuplixDeferred = async { repository.getMovieCuplix(movieId) }
            val postersDeferred = async { repository.getMoviePosters(movieId) }
            val coversDeferred = async { repository.getMovieCovers(movieId) }

            val episodes = episodesDeferred.await().getOrDefault(emptyList())
            val cuplix = cuplixDeferred.await().getOrDefault(emptyList())
            val posters = postersDeferred.await().getOrDefault(emptyList())
            val covers = coversDeferred.await().getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    episodes = episodes,
                    cuplixList = cuplix,
                    posters = posters,
                    covers = covers,
                    hasMoreEpisodes = episodes.isNotEmpty()
                )
            }
        }
    }

    fun loadMoreEpisodes() {
        val movieId = currentMovieId ?: return
        val currentState = _uiState.value
        if (currentState.isLoadingMoreEpisodes || !currentState.hasMoreEpisodes) return

        val nextPage = currentState.currentEpisodePage + 1
        _uiState.update { it.copy(isLoadingMoreEpisodes = true) }

        viewModelScope.launch {
            val result = repository.getMovieEpisodes(movieId, page = nextPage)
            val newEpisodes = result.getOrDefault(emptyList())
            if (newEpisodes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingMoreEpisodes = false,
                        hasMoreEpisodes = false
                    )
                }
            } else {
                _uiState.update { state ->
                    val existingIds = state.episodes.mapNotNull { it.id }.toSet()
                    val distinctNew = newEpisodes.filter { (it.id ?: "") !in existingIds }
                    state.copy(
                        episodes = state.episodes + distinctNew,
                        currentEpisodePage = nextPage,
                        isLoadingMoreEpisodes = false,
                        hasMoreEpisodes = distinctNew.isNotEmpty()
                    )
                }
            }
        }
    }

    fun selectTab(tab: DetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun setEpisodeSearchQuery(query: String) {
        _uiState.update { it.copy(episodeSearchQuery = query) }
    }

    fun toggleEpisodeSort() {
        _uiState.update { it.copy(isEpisodeSortAscending = !it.isEpisodeSortAscending) }
    }

    fun openFullScreenImage(url: String) {
        _uiState.update { it.copy(fullScreenPreviewUrl = url) }
    }

    fun closeFullScreenImage() {
        _uiState.update { it.copy(fullScreenPreviewUrl = null) }
    }
}
