package com.yuranime.dinz.ui.streaming

import com.yuranime.dinz.data.model.CommunityMediaItem
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.MovieDetailData
import com.yuranime.dinz.data.model.ServerItem

enum class SettingsSubMenu {
    MAIN,
    RESOLUTION,
    SPEED
}

enum class EpisodeViewMode {
    THUMBNAIL,
    COMPACT
}

data class StreamingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val episodeId: String = "",
    val movieId: String = "",
    val episode: EpisodeItem? = null,
    val episodeNext: EpisodeItem? = null,
    val movieDetail: MovieDetailData? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val totalEpisodesCount: Int = 0,
    val selectedRange: Pair<Int, Int>? = null,
    val episodeSearchQuery: String = "",
    val episodeViewMode: EpisodeViewMode = EpisodeViewMode.THUMBNAIL,
    val isLoadingEpisodes: Boolean = false,
    val settingsSubMenu: SettingsSubMenu = SettingsSubMenu.MAIN,
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
) {
    val currentEpisodeId: String get() = episodeId
    val currentEpisodeNumber: String get() = episode?.index ?: "1"
    val currentStreamUrl: String get() = selectedServer?.link.orEmpty()
}
