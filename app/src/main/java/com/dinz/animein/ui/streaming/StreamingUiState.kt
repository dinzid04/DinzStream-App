package com.dinz.animein.ui.streaming

import com.dinz.animein.data.model.CommunityMediaItem
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.data.model.EpisodeItem
import com.dinz.animein.data.model.ServerItem

data class StreamingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val episodeId: String = "",
    val movieId: String = "",
    val episode: EpisodeItem? = null,
    val episodeNext: EpisodeItem? = null,
    val servers: List<ServerItem> = emptyList(),
    val selectedServer: ServerItem? = null,
    val comments: List<EpisodeCommentItem> = emptyList(),
    val posters: List<CommunityMediaItem> = emptyList(),
    val covers: List<CommunityMediaItem> = emptyList(),
    // Player controls
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFullScreen: Boolean = false,
    val isLocked: Boolean = false,
    val autoNextEpisode: Boolean = true,
    // Overlays / Sheets
    val isServerPickerOpen: Boolean = false,
    val isLoveItDialogOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isGallerySheetOpen: Boolean = false,
    val galleryTab: Int = 0, // 0 = POSTER, 1 = COVER
    val activeCommentForReplies: EpisodeCommentItem? = null,
    val repliesList: List<EpisodeCommentItem> = emptyList(),
    val isLoadingReplies: Boolean = false,
    val fullScreenPreviewUrl: String? = null,
    val userRating: Int = 0,
    val showLoginPrompt: Boolean = false,
    val isCommentInputOpen: Boolean = false
)
