package com.dinz.animein.ui.cuplix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CuplixViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CuplixUiState())
    val uiState: StateFlow<CuplixUiState> = _uiState.asStateFlow()

    fun initialize(initialCuplixId: String? = null) {
        if (_uiState.value.cuplixList.isNotEmpty()) {
            if (initialCuplixId != null) {
                jumpToId(initialCuplixId)
            }
            return
        }
        loadFeed(initialCuplixId)
    }

    fun loadFeed(initialCuplixId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getCuplixFeed(limit = 30)
            result.onSuccess { items ->
                val likesMap = items.associate { item ->
                    (item.id ?: "") to (item.countLikes?.toIntOrNull() ?: 0)
                }
                var targetIdx = 0
                if (initialCuplixId != null) {
                    val foundIdx = items.indexOfFirst { it.id == initialCuplixId }
                    if (foundIdx >= 0) targetIdx = foundIdx
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cuplixList = items,
                        currentIndex = targetIdx,
                        likesCountMap = likesMap
                    )
                }

                // Preload stream for initial target item
                val targetItem = items.getOrNull(targetIdx)
                targetItem?.idEpisode?.let { epId ->
                    loadStreamForEpisode(epId)
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Gagal memuat tayangan Cuplix"
                    )
                }
            }
        }
    }

    fun jumpToId(id: String) {
        val idx = _uiState.value.cuplixList.indexOfFirst { it.id == id }
        if (idx >= 0) {
            onPageChanged(idx)
        }
    }

    fun onPageChanged(newIndex: Int) {
        _uiState.update { it.copy(currentIndex = newIndex) }
        val item = _uiState.value.cuplixList.getOrNull(newIndex)
        item?.idEpisode?.let { epId ->
            loadStreamForEpisode(epId)
        }
    }

    fun loadStreamForEpisode(idEpisode: String) {
        if (_uiState.value.streamUrlMap.containsKey(idEpisode)) return

        viewModelScope.launch {
            val result = repository.getEpisodeStream(idEpisode)
            result.onSuccess { streamData ->
                val directLink = streamData.servers?.firstOrNull {
                    it.link?.endsWith(".mp4", ignoreCase = true) == true
                }?.link ?: streamData.servers?.firstOrNull()?.link

                if (!directLink.isNullOrBlank()) {
                    _uiState.update { current ->
                        current.copy(
                            streamUrlMap = current.streamUrlMap + (idEpisode to directLink)
                        )
                    }
                }
            }
        }
    }

    fun toggleLike(cuplixId: String) {
        _uiState.update { current ->
            val isLiked = current.likedItemIds.contains(cuplixId)
            val currentLikes = current.likesCountMap[cuplixId] ?: 0
            val newLikedIds = if (isLiked) {
                current.likedItemIds - cuplixId
            } else {
                current.likedItemIds + cuplixId
            }
            val newCount = if (isLiked) (currentLikes - 1).coerceAtLeast(0) else currentLikes + 1

            current.copy(
                likedItemIds = newLikedIds,
                likesCountMap = current.likesCountMap + (cuplixId to newCount)
            )
        }
    }

    fun openComments(fypId: String, episodeId: String?) {
        _uiState.update {
            it.copy(
                activeCommentsFypId = fypId,
                isLoadingComments = true,
                commentsList = emptyList()
            )
        }

        viewModelScope.launch {
            // First try Cuplix comments endpoint
            val fypResult = repository.getCuplixComments(fypId)
            val comments = fypResult.getOrNull().orEmpty()

            if (comments.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        commentsList = comments
                    )
                }
            } else if (!episodeId.isNullOrBlank()) {
                // Fallback to episode comments so user always sees active discussion
                val epResult = repository.getEpisodeComments(idEpisode = episodeId, page = 0)
                val epComments = epResult.getOrNull().orEmpty()
                _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        commentsList = epComments
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    fun closeComments() {
        _uiState.update { it.copy(activeCommentsFypId = null) }
    }

    fun postComment(text: String) {
        if (text.isBlank()) return
        val currentFypId = _uiState.value.activeCommentsFypId ?: return

        val newComment = EpisodeCommentItem(
            id = System.currentTimeMillis().toString(),
            userId = "user_me",
            userName = "Yuranime Viewer",
            userPokemon = "25",
            text = text,
            time = "baru saja"
        )

        _uiState.update { current ->
            current.copy(
                commentsList = listOf(newComment) + current.commentsList
            )
        }
    }
}
