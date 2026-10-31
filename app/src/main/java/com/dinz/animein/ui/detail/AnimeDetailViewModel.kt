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
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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
            val episodesDeferred = async { repository.getMovieEpisodes(movieId) }
            val cuplixDeferred = async { repository.getMovieCuplix(movieId) }
            val postersDeferred = async { repository.getMoviePosters(movieId) }
            val coversDeferred = async { repository.getMovieCovers(movieId) }
            val discussionsDeferred = async { repository.getMovieDiscussions(movieId) }
            val contributorsDeferred = async { repository.getMovieContributors(movieId) }

            val episodes = episodesDeferred.await().getOrDefault(emptyList())
            val cuplix = cuplixDeferred.await().getOrDefault(emptyList())
            val posters = postersDeferred.await().getOrDefault(emptyList())
            val covers = coversDeferred.await().getOrDefault(emptyList())
            val discussions = discussionsDeferred.await().getOrDefault(emptyList())
            val contributors = contributorsDeferred.await().getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    episodes = episodes,
                    cuplixList = cuplix,
                    posters = posters,
                    covers = covers,
                    discussions = discussions,
                    contributors = contributors
                )
            }
        }
    }

    fun selectTab(tab: DetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }
}
