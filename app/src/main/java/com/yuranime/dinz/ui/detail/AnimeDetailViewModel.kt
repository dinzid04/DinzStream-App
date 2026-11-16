package com.yuranime.dinz.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.model.AnimeSoundtrackResult
import com.yuranime.dinz.data.model.MovieDetail
import com.yuranime.dinz.data.repository.AniListCharacterRepository
import com.yuranime.dinz.data.repository.AnimeRepository
import com.yuranime.dinz.data.repository.AnimeSoundtrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var creditsJob: Job? = null
    private var soundtrackJob: Job? = null

    fun loadAnimeDetail(movieId: String) {
        if (currentMovieId == movieId && !_uiState.value.isLoading && _uiState.value.movieDetail != null) {
            return
        }
        currentMovieId = movieId
        creditsJob?.cancel()
        soundtrackJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                selectedTab = DetailTab.INFO,
                currentEpisodePage = 0,
                hasMoreEpisodes = true,
                episodes = emptyList(),
                isLoadingCredits = false,
                creditsError = null,
                creditsData = null,
                isLoadingSoundtrack = false,
                soundtrack = AnimeSoundtrackResult()
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

            if (movie != null) {
                launchCreditsFetch(movie = movie, forceRefresh = false)
                launchSoundtrackFetch(movie = movie, forceRefresh = false)
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

    fun selectCreditsSection(section: CreditsSectionTab) {
        _uiState.update { it.copy(selectedCreditsSection = section) }
    }

    fun selectVaLanguage(language: String) {
        _uiState.update { it.copy(selectedVaLanguage = language) }
    }

    fun retryLoadCredits(forceRefresh: Boolean = true) {
        val movie = _uiState.value.movieDetail ?: return
        launchCreditsFetch(movie = movie, forceRefresh = forceRefresh)
    }

    private fun launchCreditsFetch(movie: MovieDetail, forceRefresh: Boolean) {
        val title = movie.title?.trim().takeIf { !it.isNullOrBlank() } ?: return
        val expectedId = currentMovieId

        creditsJob?.cancel()
        _uiState.update {
            it.copy(
                isLoadingCredits = true,
                creditsError = null
            )
        }

        creditsJob = viewModelScope.launch {
            val result = AniListCharacterRepository.getCreditsForAnime(
                title = title,
                synonymsRaw = movie.synonyms,
                yearHint = movie.year,
                airedStartHint = movie.airedStart,
                expectedEpisodeCount = null,
                formatHint = movie.type,
                movieId = movie.id ?: expectedId,
                forceRefresh = forceRefresh
            )
            if (currentMovieId != expectedId) return@launch

            result.fold(
                onSuccess = { data ->
                    val preferredLang = when {
                        data.availableVaLanguages.any { it.equals("Japanese", ignoreCase = true) } ->
                            data.availableVaLanguages.first { it.equals("Japanese", ignoreCase = true) }
                        data.availableVaLanguages.isNotEmpty() ->
                            data.availableVaLanguages.first()
                        else -> "Japanese"
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingCredits = false,
                            creditsError = null,
                            creditsData = data,
                            selectedVaLanguage = preferredLang
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingCredits = false,
                            creditsError = err.message ?: "Gagal memuat Karakter & Staff dari AniList"
                        )
                    }
                }
            )
        }
    }

    private fun launchSoundtrackFetch(movie: MovieDetail, forceRefresh: Boolean) {
        val title = movie.title?.trim().takeIf { !it.isNullOrBlank() } ?: return
        val expectedId = currentMovieId

        soundtrackJob?.cancel()
        _uiState.update {
            it.copy(
                isLoadingSoundtrack = true,
                soundtrack = AnimeSoundtrackResult()
            )
        }

        soundtrackJob = viewModelScope.launch {
            try {
                AnimeSoundtrackRepository.streamSoundtracksForAnime(
                    title = title,
                    synonymsRaw = movie.synonyms,
                    yearHint = movie.year,
                    airedStartHint = movie.airedStart,
                    formatHint = movie.type,
                    movieId = movie.id ?: expectedId,
                    forceRefresh = forceRefresh
                ).collect { result ->
                    if (currentMovieId != expectedId) return@collect
                    _uiState.update { state ->
                        val safeSelectedTab = if (state.selectedTab == DetailTab.SOUNDTRACK && result.isEmpty) {
                            DetailTab.INFO
                        } else {
                            state.selectedTab
                        }
                        state.copy(
                            isLoadingSoundtrack = false,
                            soundtrack = result,
                            selectedTab = safeSelectedTab
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (currentMovieId == expectedId) {
                    _uiState.update { state ->
                        val safeTab = if (state.selectedTab == DetailTab.SOUNDTRACK) DetailTab.INFO else state.selectedTab
                        state.copy(
                            isLoadingSoundtrack = false,
                            soundtrack = AnimeSoundtrackResult(),
                            selectedTab = safeTab
                        )
                    }
                }
            }
        }
    }
}
