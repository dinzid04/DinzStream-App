package com.yuranime.dinz.ui.cuplix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.repository.AnimeRepository
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

    fun initialize(initialItem: com.yuranime.dinz.data.model.CuplixItem? = null, initialCuplixId: String? = initialItem?.id) {
        val targetId = initialItem?.id ?: initialCuplixId

        // When a specific item was clicked, always put it at index 0 so it plays immediately
        if (initialItem != null) {
            val existingList = _uiState.value.cuplixList.filter { it.id != initialItem.id }
            val combinedList = listOf(initialItem) + existingList
            val likesMap = _uiState.value.likesCountMap + (initialItem.id.orEmpty() to (initialItem.countLikes?.toIntOrNull() ?: 0))
            val commentsMap = _uiState.value.commentsCountMap + (initialItem.id.orEmpty() to (initialItem.countComments?.toIntOrNull() ?: 0))
            val likedIds = if (initialItem.isLike == "1") _uiState.value.likedItemIds + initialItem.id.orEmpty() else _uiState.value.likedItemIds

            _uiState.update {
                it.copy(
                    isLoading = false,
                    cuplixList = combinedList,
                    currentIndex = 0,
                    likesCountMap = likesMap,
                    commentsCountMap = commentsMap,
                    likedItemIds = likedIds,
                    activeCommentsFypId = null,
                    commentsList = emptyList()
                )
            }

            initialItem.idEpisode?.let { epId -> loadStreamForEpisode(epId) }
            initialItem.id?.let { fypId -> syncCommentsForCuplix(fypId) }

            if (combinedList.size < 5) {
                loadPopularFeedInBackground(excludeId = initialItem.id)
            }
            return
        }

        if (!targetId.isNullOrBlank()) {
            val foundIdx = _uiState.value.cuplixList.indexOfFirst { it.id == targetId }
            if (foundIdx >= 0) {
                val target = _uiState.value.cuplixList[foundIdx]
                val reordered = listOf(target) + _uiState.value.cuplixList.filter { it.id != targetId }
                _uiState.update { it.copy(cuplixList = reordered, currentIndex = 0) }
                target.idEpisode?.let { epId -> loadStreamForEpisode(epId) }
                target.id?.let { fypId -> syncCommentsForCuplix(fypId) }
                return
            }
        }

        loadFeed(targetId)
    }

    private fun loadPopularFeedInBackground(excludeId: String?) {
        viewModelScope.launch {
            val result = repository.getRandomizedCuplixFeed(limit = 35)
            result.onSuccess { items ->
                val filtered = items.filter { it.id != excludeId && !_uiState.value.cuplixList.any { ex -> ex.id == it.id } }
                _uiState.update { current ->
                    val combined = current.cuplixList + filtered
                    val likesMap = current.likesCountMap + filtered.associate { item ->
                        (item.id ?: "") to (item.countLikes?.toIntOrNull() ?: 0)
                    }
                    val commentsMap = current.commentsCountMap + filtered.associate { item ->
                        (item.id ?: "") to (item.countComments?.toIntOrNull() ?: 0)
                    }
                    current.copy(
                        cuplixList = combined,
                        likesCountMap = likesMap,
                        commentsCountMap = commentsMap
                    )
                }
            }
        }
    }

    fun loadFeed(targetId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getRandomizedCuplixFeed(limit = 40)
            result.onSuccess { items ->
                var targetIdx = -1
                if (!targetId.isNullOrBlank()) {
                    targetIdx = items.indexOfFirst { it.id == targetId }
                }

                val orderedItems = if (targetIdx >= 0) {
                    val target = items[targetIdx]
                    val others = items.filterIndexed { idx, _ -> idx != targetIdx }.shuffled()
                    listOf(target) + others
                } else {
                    items.shuffled()
                }

                val likesMap = orderedItems.associate { item ->
                    (item.id ?: "") to (item.countLikes?.toIntOrNull() ?: 0)
                }
                val commentsMap = orderedItems.associate { item ->
                    (item.id ?: "") to (item.countComments?.toIntOrNull() ?: 0)
                }
                val likedIds = orderedItems.filter { it.isLike == "1" }.mapNotNull { it.id }.toSet()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cuplixList = orderedItems,
                        currentIndex = 0,
                        likesCountMap = likesMap,
                        commentsCountMap = commentsMap,
                        likedItemIds = likedIds
                    )
                }

                val firstItem = orderedItems.firstOrNull()
                firstItem?.let { item ->
                    item.idEpisode?.let { epId -> loadStreamForEpisode(epId) }
                    item.id?.let { fypId -> syncCommentsForCuplix(fypId) }
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
            val target = _uiState.value.cuplixList[idx]
            val reordered = listOf(target) + _uiState.value.cuplixList.filter { it.id != id }
            _uiState.update { it.copy(cuplixList = reordered, currentIndex = 0) }
            target.idEpisode?.let { epId -> loadStreamForEpisode(epId) }
            target.id?.let { fypId -> syncCommentsForCuplix(fypId) }
        }
    }

    fun onPageChanged(newIndex: Int) {
        _uiState.update { it.copy(currentIndex = newIndex) }
        val item = _uiState.value.cuplixList.getOrNull(newIndex)
        item?.let {
            it.idEpisode?.let { epId -> loadStreamForEpisode(epId) }
            it.id?.let { fypId -> syncCommentsForCuplix(fypId) }
        }
    }

    private fun syncCommentsForCuplix(fypId: String) {
        viewModelScope.launch {
            val fypResult = repository.getCuplixComments(fypId)
            val comments = fypResult.getOrNull().orEmpty()
            if (comments.isNotEmpty()) {
                _uiState.update { current ->
                    val existing = current.commentsCountMap[fypId] ?: 0
                    current.copy(
                        commentsCountMap = current.commentsCountMap + (fypId to maxOf(existing, comments.size))
                    )
                }
            }
        }
    }

    fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
        val currentItem = _uiState.value.cuplixList.getOrNull(_uiState.value.currentIndex)
        val epId = currentItem?.idEpisode ?: return

        viewModelScope.launch {
            val result = repository.getEpisodeStream(epId)
            result.onSuccess { streamData ->
                val matchedServer = streamData.servers?.firstOrNull {
                    it.quality.equals(quality, ignoreCase = true) || it.name?.contains(quality, ignoreCase = true) == true
                } ?: streamData.servers?.firstOrNull()

                val directLink = matchedServer?.link
                if (!directLink.isNullOrBlank()) {
                    _uiState.update { current ->
                        current.copy(
                            streamUrlMap = current.streamUrlMap + (epId to directLink)
                        )
                    }
                }
            }
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

    fun openComments(fypId: String, episodeId: String? = null) {
        _uiState.update {
            it.copy(
                activeCommentsFypId = fypId,
                isLoadingComments = true,
                commentsList = emptyList()
            )
        }

        viewModelScope.launch {
            val fypResult = repository.getCuplixComments(fypId)
            val comments = fypResult.getOrNull().orEmpty()

            _uiState.update { current ->
                val existing = current.commentsCountMap[fypId] ?: 0
                current.copy(
                    isLoadingComments = false,
                    commentsList = comments,
                    commentsCountMap = current.commentsCountMap + (fypId to maxOf(existing, comments.size))
                )
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
            val updatedList = listOf(newComment) + current.commentsList
            val updatedCount = (current.commentsCountMap[currentFypId] ?: current.commentsList.size) + 1
            current.copy(
                commentsList = updatedList,
                commentsCountMap = current.commentsCountMap + (currentFypId to updatedCount)
            )
        }
    }
}
