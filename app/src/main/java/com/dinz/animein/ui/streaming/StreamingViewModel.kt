package com.dinz.animein.ui.streaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.data.model.ServerItem
import com.dinz.animein.data.repository.AnimeRepository
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

            // Fetch comments and covers/posters in parallel
            val commentsDeferred = async { repository.getEpisodeComments(episodeId, page = 0, sort = "new") }
            val postersDeferred = async { if (movieId.isNotBlank()) repository.getMoviePosters(movieId) else Result.success(emptyList()) }
            val coversDeferred = async { if (movieId.isNotBlank()) repository.getMovieCovers(movieId) else Result.success(emptyList()) }

            val comments = commentsDeferred.await().getOrDefault(emptyList())
            val posters = postersDeferred.await().getOrDefault(emptyList())
            val covers = coversDeferred.await().getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    comments = comments,
                    posters = posters,
                    covers = covers
                )
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

    fun toggleSettings() {
        _uiState.update { it.copy(isSettingsOpen = !it.isSettingsOpen) }
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
}
