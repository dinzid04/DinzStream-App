package com.yuranime.dinz.ui.streaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.ServerItem
import com.yuranime.dinz.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StreamingViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamingUiState())
    val uiState: StateFlow<StreamingUiState> = _uiState.asStateFlow()

    private var cachedMovieId: String? = null
    private val episodesPageCache = mutableMapOf<Int, List<EpisodeItem>>()

    fun loadStreaming(episodeId: String, movieId: String) {
        if (_uiState.value.episodeId == episodeId && !_uiState.value.isLoading && _uiState.value.selectedServer != null) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                episodeId = episodeId,
                movieId = movieId,
                currentPositionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                isServerPickerOpen = false,
                isLoveItDialogOpen = false,
                isGallerySheetOpen = false,
                activeCommentForReplies = null
            )
        }

        viewModelScope.launch {
            val streamResult = repository.getEpisodeStream(episodeId)
            if (streamResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = streamResult.exceptionOrNull()?.message ?: "Gagal memuat stream episode"
                    )
                }
                return@launch
            }

            val streamData = streamResult.getOrNull()
            val servers = streamData?.servers ?: emptyList()
            // Default select 720p or highest available resolution
            val defaultServer = servers.firstOrNull { it.quality == "720p" }
                ?: servers.firstOrNull { it.quality == "480p" }
                ?: servers.firstOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    episode = streamData?.episode,
                    episodeNext = streamData?.episodeNext,
                    servers = servers,
                    selectedServer = defaultServer
                )
            }

            // Fetch movie detail, comments, covers/posters, and episodes in parallel
            val effectiveMovieId = movieId.ifBlank { streamData?.episode?.idMovie ?: "" }
            if (effectiveMovieId.isNotBlank() && effectiveMovieId != cachedMovieId) {
                cachedMovieId = effectiveMovieId
                episodesPageCache.clear()
            }

            val movieDetailDeferred = async {
                if (effectiveMovieId.isNotBlank()) repository.getMovieDetail(effectiveMovieId)
                else Result.success(null)
            }
            val commentsDeferred = async { repository.getEpisodeComments(episodeId, page = 0, sort = "new") }
            val postersDeferred = async { if (effectiveMovieId.isNotBlank()) repository.getMoviePosters(effectiveMovieId) else Result.success(emptyList()) }
            val coversDeferred = async { if (effectiveMovieId.isNotBlank()) repository.getMovieCovers(effectiveMovieId) else Result.success(emptyList()) }
            val episodesPage0Deferred = async {
                if (effectiveMovieId.isNotBlank() && !episodesPageCache.containsKey(0)) {
                    repository.getMovieEpisodes(effectiveMovieId, page = 0)
                } else {
                    Result.success(episodesPageCache[0] ?: emptyList())
                }
            }

            val movieDetail = movieDetailDeferred.await().getOrNull()
            val comments = commentsDeferred.await().getOrDefault(emptyList())
            val posters = postersDeferred.await().getOrDefault(emptyList())
            val covers = coversDeferred.await().getOrDefault(emptyList())
            val page0Episodes = episodesPage0Deferred.await().getOrDefault(emptyList())
            if (page0Episodes.isNotEmpty()) {
                episodesPageCache[0] = page0Episodes
            }

            val currentEpNum = streamData?.episode?.index?.toIntOrNull() ?: 1
            val maxEpNum = maxOf(
                page0Episodes.mapNotNull { it.index?.toIntOrNull() }.maxOrNull() ?: 0,
                currentEpNum
            )

            if (maxEpNum <= 50) {
                if (page0Episodes.size >= 30 && !episodesPageCache.containsKey(1)) {
                    val p1Result = repository.getMovieEpisodes(effectiveMovieId, page = 1)
                    val p1 = p1Result.getOrDefault(emptyList())
                    if (p1.isNotEmpty()) {
                        episodesPageCache[1] = p1
                    }
                }
                val allEpisodes = episodesPageCache.values.flatten()
                    .distinctBy { it.id }
                    .sortedBy { it.index?.toIntOrNull() ?: 0 }

                _uiState.update {
                    it.copy(
                        movieDetail = movieDetail,
                        comments = comments,
                        posters = posters,
                        covers = covers,
                        episodes = allEpisodes,
                        totalEpisodesCount = maxOf(maxEpNum, allEpisodes.size),
                        selectedRange = null
                    )
                }
            } else {
                val chunkStart = (((currentEpNum - 1) / 100) * 100 + 1).coerceAtLeast(1)
                val chunkEnd = minOf(chunkStart + 99, maxEpNum)

                ensureRangeEpisodesLoaded(effectiveMovieId, chunkStart, chunkEnd, maxEpNum)

                _uiState.update {
                    it.copy(
                        movieDetail = movieDetail,
                        comments = comments,
                        posters = posters,
                        covers = covers,
                        totalEpisodesCount = maxEpNum,
                        selectedRange = Pair(chunkStart, chunkEnd)
                    )
                }
            }
        }
    }

    fun selectServer(server: ServerItem) {
        // Duration synchronization: preserves currentPositionMs!
        _uiState.update {
            it.copy(
                selectedServer = server,
                isServerPickerOpen = false
            )
        }
    }

    fun updatePosition(positionMs: Long, durationMs: Long) {
        _uiState.update {
            it.copy(
                currentPositionMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else it.durationMs
            )
        }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun cycleSpeed() {
        val nextSpeed = when (_uiState.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _uiState.update { it.copy(playbackSpeed = nextSpeed) }
    }

    fun toggleAutoNext() {
        _uiState.update { it.copy(autoNextEpisode = !it.autoNextEpisode) }
    }

    fun toggleLock() {
        if (_uiState.value.isFullScreen) {
            _uiState.update { it.copy(isLocked = !it.isLocked) }
        }
    }

    fun toggleFullScreen() {
        _uiState.update {
            val willBeFullScreen = !it.isFullScreen
            it.copy(
                isFullScreen = willBeFullScreen,
                isLocked = if (!willBeFullScreen) false else it.isLocked
            )
        }
    }

    fun toggleServerPicker() {
        _uiState.update { it.copy(isServerPickerOpen = !it.isServerPickerOpen) }
    }

    fun toggleLoveItDialog() {
        _uiState.update { it.copy(isLoveItDialogOpen = !it.isLoveItDialogOpen) }
    }

    fun setUserRating(stars: Int) {
        _uiState.update { it.copy(userRating = stars) }
    }

    fun setSettingsSubMenu(subMenu: SettingsSubMenu) {
        _uiState.update { it.copy(settingsSubMenu = subMenu) }
    }

    fun toggleSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = !it.isSettingsOpen,
                settingsSubMenu = SettingsSubMenu.MAIN
            )
        }
    }

    fun toggleGallerySheet(tab: Int = 0) {
        _uiState.update {
            it.copy(
                isGallerySheetOpen = !it.isGallerySheetOpen,
                galleryTab = tab
            )
        }
    }

    fun setGalleryTab(tab: Int) {
        _uiState.update { it.copy(galleryTab = tab) }
    }

    fun openReplies(comment: EpisodeCommentItem) {
        val cid = comment.id ?: return
        _uiState.update {
            it.copy(
                activeCommentForReplies = comment,
                isLoadingReplies = true,
                repliesList = emptyList()
            )
        }

        viewModelScope.launch {
            val result = repository.getCommentReplies(cid, page = 1)
            val replies = result.getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoadingReplies = false,
                    repliesList = replies
                )
            }
        }
    }

    fun closeReplies() {
        _uiState.update {
            it.copy(
                activeCommentForReplies = null,
                repliesList = emptyList(),
                isLoadingReplies = false
            )
        }
    }

    fun openFullScreenImage(url: String) {
        _uiState.update { it.copy(fullScreenPreviewUrl = url) }
    }

    fun closeFullScreenImage() {
        _uiState.update { it.copy(fullScreenPreviewUrl = null) }
    }

    fun toggleCommentInput() {
        _uiState.update { it.copy(isCommentInputOpen = !it.isCommentInputOpen) }
    }

    fun triggerLoginPrompt() {
        _uiState.update { it.copy(showLoginPrompt = true) }
    }

    fun dismissLoginPrompt() {
        _uiState.update { it.copy(showLoginPrompt = false) }
    }

    fun addLocalComment(comment: EpisodeCommentItem) {
        _uiState.update {
            it.copy(
                comments = listOf(comment) + it.comments,
                isCommentInputOpen = false
            )
        }
    }

    fun addLocalReply(reply: EpisodeCommentItem) {
        _uiState.update {
            it.copy(repliesList = listOf(reply) + it.repliesList)
        }
    }

    private suspend fun ensureRangeEpisodesLoaded(movieId: String, startEp: Int, endEp: Int, maxEp: Int) {
        if (movieId.isBlank() || maxEp <= 0) return
        val startPage = (maxEp - endEp).coerceAtLeast(0) / 30
        val endPage = (maxEp - startEp).coerceAtLeast(0) / 30

        for (p in startPage..endPage) {
            if (!episodesPageCache.containsKey(p)) {
                val result = repository.getMovieEpisodes(movieId, page = p)
                val eps = result.getOrDefault(emptyList())
                if (eps.isNotEmpty()) {
                    episodesPageCache[p] = eps
                }
            }
        }

        val rangeEpisodes = episodesPageCache.values.flatten()
            .distinctBy { it.id }
            .filter { ep ->
                val idx = ep.index?.toIntOrNull() ?: 0
                idx in startEp..endEp
            }
            .sortedBy { it.index?.toIntOrNull() ?: 0 }

        _uiState.update {
            it.copy(
                episodes = rangeEpisodes,
                selectedRange = Pair(startEp, endEp),
                totalEpisodesCount = maxEp
            )
        }
    }

    fun selectEpisodeRange(start: Int, end: Int) {
        val movieId = _uiState.value.movieId.ifBlank { _uiState.value.episode?.idMovie ?: "" }
        val maxEp = _uiState.value.totalEpisodesCount
        _uiState.update { it.copy(selectedRange = Pair(start, end), isLoadingEpisodes = true) }
        viewModelScope.launch {
            ensureRangeEpisodesLoaded(movieId, start, end, maxEp)
            _uiState.update { it.copy(isLoadingEpisodes = false) }
        }
    }

    fun setEpisodeSearchQuery(query: String) {
        _uiState.update { it.copy(episodeSearchQuery = query) }
        val targetNum = query.trim().toIntOrNull()
        val maxEp = _uiState.value.totalEpisodesCount
        val movieId = _uiState.value.movieId.ifBlank { _uiState.value.episode?.idMovie ?: "" }
        if (targetNum != null && targetNum in 1..maxEp && movieId.isNotBlank()) {
            val targetPage = (maxEp - targetNum).coerceAtLeast(0) / 30
            if (!episodesPageCache.containsKey(targetPage)) {
                viewModelScope.launch {
                    val result = repository.getMovieEpisodes(movieId, page = targetPage)
                    val newEpisodes = result.getOrDefault(emptyList())
                    if (newEpisodes.isNotEmpty()) {
                        episodesPageCache[targetPage] = newEpisodes
                        _uiState.update { state ->
                            val updated = (state.episodes + newEpisodes)
                                .distinctBy { it.id }
                                .sortedBy { it.index?.toIntOrNull() ?: 0 }
                            state.copy(episodes = updated)
                        }
                    }
                }
            }
        }
    }

    fun playEpisode(newEpisodeId: String) {
        val currentMovieId = _uiState.value.movieId.ifBlank { _uiState.value.episode?.idMovie ?: "" }
        loadStreaming(episodeId = newEpisodeId, movieId = currentMovieId)
    }
}
