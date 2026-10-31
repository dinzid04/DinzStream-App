package com.dinz.animein.ui.cuplix

import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.EpisodeCommentItem

data class CuplixUiState(
    val isLoading: Boolean = false,
    val cuplixList: List<CuplixItem> = emptyList(),
    val currentIndex: Int = 0,
    val likedItemIds: Set<String> = emptySet(),
    val likesCountMap: Map<String, Int> = emptyMap(),
    val streamUrlMap: Map<String, String> = emptyMap(), // idEpisode -> direct stream mp4
    val activeCommentsFypId: String? = null,
    val commentsList: List<EpisodeCommentItem> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isPostingComment: Boolean = false,
    val errorMessage: String? = null
)
