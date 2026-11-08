package com.yuranime.dinz.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.ContinueWatchingItem
import com.yuranime.dinz.data.repository.AnimeRepository
import com.yuranime.dinz.data.repository.ComicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: AnimeRepository = AnimeRepository(),
    private val comicRepository: ComicRepository = ComicRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        loadBirthdayToday()
        loadComics()
    }

    fun loadComics(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isComicsLoading = true) }
            val result = comicRepository.getLatestComics(page = 1, pageSize = 12, forceRefresh = forceRefresh)
            val list = result.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    comicsList = list,
                    isComicsLoading = false
                )
            }
        }
    }

    fun loadBirthdayToday(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBirthdayLoading = true, birthdayError = null) }
            val result = com.yuranime.dinz.data.api.OtakOtakuScraper.getTodayBirthdays(forceRefresh = forceRefresh)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        birthdayToday = result.getOrNull(),
                        isBirthdayLoading = false,
                        birthdayError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBirthdayLoading = false,
                        birthdayError = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat ulang tahun"
                    )
                }
            }
        }
    }

    fun refreshWatchHistory(context: Context) {
        val localDb = LocalAuthDatabase(context)
        val history = localDb.getWatchHistory()
        val currentState = _uiState.value
        val allLoadedAnime = currentState.newEpisodesList +
            currentState.hotAnimeList +
            currentState.todayScheduleList +
            currentState.waitingAnimeList +
            currentState.popularAnimeList

        val list = history.map { item ->
            val fraction = if (item.durationMs > 0) {
                (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.01f, 1f)
            } else 0f
            val matchedCover = allLoadedAnime
                .firstOrNull { it.id == item.movieId }
                ?.getEffectiveCover()
                ?.takeIf { it.isNotBlank() }
            val resolvedCover = matchedCover ?: com.yuranime.dinz.data.model.normalizeImageUrl(item.posterUrl)
            ContinueWatchingItem(
                id = item.movieId,
                idMovie = item.movieId,
                title = item.animeTitle,
                episodeText = item.episodeTitle,
                imageUrl = resolvedCover,
                progressFraction = fraction,
                episodeId = item.episodeId,
                positionMs = item.positionMs
            )
        }
        _uiState.update { it.copy(continueWatchingList = list) }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val homeDeferred = async { repository.getHomeData() }
            val cuplixDeferred = async { repository.getRandomizedCuplixFeed(limit = 25, source = AnimeRepository.CuplixPageSource.HOME) }
            val newTitlesDeferred = async { repository.getHomeNewTitles() }
            val randomDeferred = async { repository.getHomeRandom() }
            val comicsDeferred = async { comicRepository.getLatestComics(page = 1, pageSize = 12) }

            val homeResult = homeDeferred.await()
            val cuplixResult = cuplixDeferred.await()
            val newTitlesResult = newTitlesDeferred.await()
            val randomResult = randomDeferred.await()
            val comicsResult = comicsDeferred.await()

            if (homeResult.isSuccess) {
                val data = homeResult.getOrNull()
                val cuplixItems = cuplixResult.getOrDefault(emptyList())
                val newTitles = newTitlesResult.getOrNull()?.ifEmpty { null } ?: data?.movies ?: emptyList()
                val randomItems = randomResult.getOrNull()?.ifEmpty { null } ?: data?.hotAnime ?: emptyList()
                val comicsItems = comicsResult.getOrDefault(emptyList())

                _uiState.update { current ->
                    val newEp = data?.newEpisodes ?: emptyList()
                    val hot = data?.hotAnime ?: emptyList()
                    val today = data?.todayAnime ?: emptyList()
                    val waiting = data?.waitingAnime ?: emptyList()
                    val popular = data?.popularAnime ?: emptyList()
                    val combinedAnime = newEp + hot + today + waiting + popular
                    val upgradedContinueWatching = current.continueWatchingList.map { cw ->
                        val matchedCover = combinedAnime
                            .firstOrNull { it.id == cw.idMovie }
                            ?.getEffectiveCover()
                            ?.takeIf { it.isNotBlank() }
                        if (matchedCover != null) cw.copy(imageUrl = matchedCover) else cw
                    }

                    current.copy(
                        isLoading = false,
                        errorMessage = null,
                        sliderItem = data?.slider?.firstOrNull(),
                        cuplixList = cuplixItems,
                        continueWatchingList = upgradedContinueWatching,
                        newEpisodesList = newEp,
                        hotAnimeList = hot,
                        newTitlesList = newTitles,
                        comicsList = if (comicsItems.isNotEmpty()) comicsItems else current.comicsList,
                        todayScheduleList = today,
                        jatForYuList = randomItems,
                        waitingAnimeList = waiting,
                        popularAnimeList = popular,
                        manreList = emptyList()
                    )
                }
            } else {
                val errorMsg = homeResult.exceptionOrNull()?.localizedMessage ?: "Gagal memuat data"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun loadMoreHorizontal(section: HomeSectionType) {
        if (_uiState.value.isLoadingSection[section] == true) return
        val currentPage = _uiState.value.horizontalPageMap[section] ?: 1
        val nextPage = currentPage + 1

        _uiState.update {
            it.copy(isLoadingSection = it.isLoadingSection + (section to true))
        }

        viewModelScope.launch {
            val result = repository.searchMovies(sort = section.sortKey, page = nextPage)
            val newItems = result.getOrDefault(emptyList())
            _uiState.update { current ->
                val updatedMap = current.horizontalPageMap + (section to nextPage)
                val updatedLoading = current.isLoadingSection + (section to false)
                when (section) {
                    HomeSectionType.HOT -> current.copy(
                        hotAnimeList = current.hotAnimeList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    HomeSectionType.NEW_EPISODES -> current.copy(
                        newEpisodesList = current.newEpisodesList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    HomeSectionType.NEW_TITLES -> current.copy(
                        newTitlesList = current.newTitlesList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    HomeSectionType.JAT_FOR_YU -> current.copy(
                        jatForYuList = current.jatForYuList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    HomeSectionType.WAITING -> current.copy(
                        waitingAnimeList = current.waitingAnimeList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    HomeSectionType.POPULAR -> current.copy(
                        popularAnimeList = current.popularAnimeList + newItems,
                        horizontalPageMap = updatedMap,
                        isLoadingSection = updatedLoading
                    )
                    else -> current.copy(isLoadingSection = updatedLoading)
                }
            }
        }
    }

    fun openSectionDetail(section: HomeSectionType) {
        val initialItems = when (section) {
            HomeSectionType.HOT -> _uiState.value.hotAnimeList
            HomeSectionType.NEW_EPISODES -> _uiState.value.newEpisodesList
            HomeSectionType.NEW_TITLES -> _uiState.value.newTitlesList
            HomeSectionType.JAT_FOR_YU -> _uiState.value.jatForYuList
            HomeSectionType.WAITING -> _uiState.value.waitingAnimeList
            HomeSectionType.POPULAR -> _uiState.value.popularAnimeList
            HomeSectionType.TODAY -> _uiState.value.todayScheduleList
        }
        _uiState.update {
            it.copy(
                activeSectionDetail = HomeSectionDetailState(
                    sectionType = section,
                    items = initialItems,
                    page = 1
                )
            )
        }
    }

    fun closeSectionDetail() {
        _uiState.update { it.copy(activeSectionDetail = null) }
    }

    fun loadMoreSectionDetail() {
        val detail = _uiState.value.activeSectionDetail ?: return
        if (detail.isLoadingMore || detail.hasReachedEnd) return

        val nextPage = detail.page + 1
        _uiState.update {
            it.copy(activeSectionDetail = detail.copy(isLoadingMore = true))
        }

        viewModelScope.launch {
            val result = repository.searchMovies(sort = detail.sectionType.sortKey, page = nextPage)
            val newItems = result.getOrDefault(emptyList())
            _uiState.update { current ->
                val currentDetail = current.activeSectionDetail
                if (currentDetail != null && currentDetail.sectionType == detail.sectionType) {
                    current.copy(
                        activeSectionDetail = currentDetail.copy(
                            items = currentDetail.items + newItems,
                            page = nextPage,
                            isLoadingMore = false,
                            hasReachedEnd = newItems.isEmpty()
                        )
                    )
                } else current
            }
        }
    }
}
