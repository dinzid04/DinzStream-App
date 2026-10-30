package com.dinz.animein.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val homeDeferred = async { repository.getHomeData() }
            val cuplixDeferred = async { repository.getCuplixFeed() }

            val homeResult = homeDeferred.await()
            val cuplixResult = cuplixDeferred.await()

            if (homeResult.isSuccess) {
                val data = homeResult.getOrNull()
                val cuplixItems = cuplixResult.getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        sliderItem = data?.slider?.firstOrNull(),
                        continueWatchingList = repository.getContinueWatchingList(),
                        cuplixList = cuplixItems,
                        newEpisodesList = data?.newEpisodes ?: emptyList(),
                        hotAnimeList = data?.hotAnime ?: emptyList(),
                        newTitlesList = data?.movies ?: data?.newEpisodes ?: emptyList(),
                        todayScheduleList = data?.todayAnime ?: emptyList(),
                        jatForYuList = data?.fypAnime ?: data?.hotAnime ?: emptyList(),
                        waitingAnimeList = data?.waitingAnime ?: emptyList(),
                        popularAnimeList = data?.popularAnime ?: emptyList(),
                        manreList = repository.getManreList()
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
}
