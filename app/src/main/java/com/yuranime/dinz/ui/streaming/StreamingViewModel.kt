package com.yuranime.dinz.ui.streaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.MovieDetailData
import com.yuranime.dinz.data.model.ServerItem
import com.yuranime.dinz.data.model.StreamData
import com.yuranime.dinz.data.repository.AnimeRepository
import com.yuranime.dinz.data.repository.SkipTimestampsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StreamingViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StreamingUiState(
            preferredQuality = AppPreferences.defaultAnimeQuality.value,
            playbackSpeed = AppPreferences.defaultPlaybackSpeed.value,
            autoPlayEnabled = AppPreferences.autoPlayEnabled.value,
            autoSkipIntro = AppPreferences.autoSkipIntro.value,
            preloadNextEpisode = AppPreferences.preloadNextEpisode.value,
            autoNextEpisode = AppPreferences.autoNext.value
        )
    )
    val uiState: StateFlow<StreamingUiState> = _uiState.asStateFlow()

    private var cachedMovieId: String? = null
    private val episodesPageCache = mutableMapOf<Int, List<EpisodeItem>>()
    private val preloadedStreams = mutableMapOf<String, StreamData>()
    private var episodeJob: Job? = null
    private var preloadJob: Job? = null
    private var skipJob: Job? = null
    private var manualServerSelected: Boolean = false

    fun syncPlaybackPreferences(
        preferredQuality: String,
        playbackSpeed: Float,
        autoPlayEnabled: Boolean,
        autoSkipIntro: Boolean,
        preloadNextEpisode: Boolean
    ) {
        _uiState.update { state ->
            val updatedServer = if (!manualServerSelected && state.servers.isNotEmpty()) {
                resolvePreferredServer(state.servers, preferredQuality) ?: state.selectedServer
            } else {
                state.selectedServer
            }
            state.copy(
                preferredQuality = preferredQuality,
                playbackSpeed = playbackSpeed,
                autoPlayEnabled = autoPlayEnabled,
                autoSkipIntro = autoSkipIntro,
                preloadNextEpisode = preloadNextEpisode,
                selectedServer = updatedServer
            )
        }
        if (preloadNextEpisode) {
            schedulePreloadNextEpisode(null, _uiState.value.episodeId)
        } else {
            preloadJob?.cancel()
            preloadedStreams.clear()
        }
    }

    private fun resolvePreferredServer(servers: List<ServerItem>, preferredQuality: String): ServerItem? {
        if (servers.isEmpty()) return null
        val cleanPref = preferredQuality.trim()
        if (cleanPref.equals("Auto", ignoreCase = true) || cleanPref.isBlank()) {
            return servers.firstOrNull { it.quality?.contains("720", ignoreCase = true) == true }
                ?: servers.firstOrNull { it.quality?.contains("480", ignoreCase = true) == true }
                ?: servers.firstOrNull()
        }
        val numToken = cleanPref.removeSuffix("p")
        servers.firstOrNull {
            it.quality?.equals(cleanPref, ignoreCase = true) == true ||
                it.quality?.contains(numToken, ignoreCase = true) == true
        }?.let { return it }

        val fallbackOrder = when {
            cleanPref.contains("1080") -> listOf("1080", "720", "480", "360")
            cleanPref.contains("720") -> listOf("720", "480", "1080", "360")
            cleanPref.contains("480") -> listOf("480", "360", "720", "1080")
            cleanPref.contains("360") -> listOf("360", "480", "720", "1080")
            else -> listOf("720", "480", "360", "1080")
        }
        for (q in fallbackOrder) {
            servers.firstOrNull { it.quality?.contains(q, ignoreCase = true) == true }?.let { return it }
        }
        return servers.firstOrNull()
    }

    private fun schedulePreloadNextEpisode(streamData: StreamData?, currentEpisodeId: String) {
        if (!_uiState.value.preloadNextEpisode) return
        val nextId = streamData?.episodeNext?.id
            ?: _uiState.value.episodeNext?.id
            ?: run {
                val curIdx = _uiState.value.episode?.index?.toIntOrNull()
                if (curIdx != null) {
                    _uiState.value.episodes.firstOrNull { it.index == (curIdx + 1).toString() }?.id
                } else null
            }
        if (nextId.isNullOrBlank() || nextId == currentEpisodeId || preloadedStreams.containsKey(nextId)) return
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch {
            val result = repository.getEpisodeStream(nextId)
            result.getOrNull()?.let { prefetched ->
                if (preloadedStreams.size >= 4) {
                    preloadedStreams.keys.firstOrNull()?.let { preloadedStreams.remove(it) }
                }
                preloadedStreams[nextId] = prefetched
            }
        }
    }

    private fun launchSkipTimestampsFetch(
        forEpisodeId: String,
        episodeNumberRaw: String?,
        movieDetailData: MovieDetailData?,
        expectedEpisodes: Int?
    ) {
        val movie = movieDetailData?.movie ?: return
        val title = movie.title?.trim()?.takeIf { it.isNotBlank() } ?: return
        val epNum = episodeNumberRaw?.trim()?.takeIf { it.isNotBlank() } ?: return

        skipJob?.cancel()
        skipJob = viewModelScope.launch {
            val result = SkipTimestampsRepository.getSkipTimestamps(
                title = title,
                synonymsRaw = movie.synonyms,
                yearHint = movie.year,
                airedStartHint = movie.airedStart,
                expectedEpisodeCount = expectedEpisodes?.takeIf { it > 0 },
                formatHint = movie.type,
                movieId = movie.id ?: _uiState.value.movieId,
                episodeNumberRaw = epNum,
                episodeDurationMs = _uiState.value.durationMs
            )
            if (_uiState.value.episodeId == forEpisodeId) {
                _uiState.update {
                    it.copy(
                        introSegment = result?.intro,
                        outroSegment = result?.outro,
                        skipSourceLabel = result?.sourceSummary
                    )
                }
            }
        }
    }

    fun loadStreaming(episodeId: String, movieId: String) {
        if (_uiState.value.episodeId == episodeId && (_uiState.value.selectedServer != null || _uiState.value.isLoading)) {
            return
        }

        val effectiveMovieId = movieId.ifBlank { _uiState.value.movieId.ifBlank { _uiState.value.episode?.idMovie ?: "" } }
        if (effectiveMovieId.isNotBlank() && effectiveMovieId == cachedMovieId && _uiState.value.episodes.isNotEmpty()) {
            selectEpisode(newEpisodeId = episodeId)
            return
        }

        episodeJob?.cancel()
        skipJob?.cancel()
        manualServerSelected = false

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                episodeId = episodeId,
                movieId = movieId,
                currentPositionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                introSegment = null,
                outroSegment = null,
                skipSourceLabel = null,
                isServerPickerOpen = false,
                isLoveItDialogOpen = false,
                isGallerySheetOpen = false,
                activeCommentForReplies = null
            )
        }

        episodeJob = viewModelScope.launch {
            val streamResult = preloadedStreams.remove(episodeId)?.let { Result.success(it) }
                ?: repository.getEpisodeStream(episodeId)
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
            val defaultServer = resolvePreferredServer(servers, _uiState.value.preferredQuality)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    episode = streamData?.episode,
                    episodeNext = streamData?.episodeNext,
                    servers = servers,
                    selectedServer = defaultServer
                )
            }
            schedulePreloadNextEpisode(streamData, episodeId)

            // Fetch movie detail, comments, covers/posters, and episodes in parallel
            val resolvedMovieId = effectiveMovieId.ifBlank { streamData?.episode?.idMovie ?: "" }
            if (resolvedMovieId.isNotBlank() && resolvedMovieId != cachedMovieId) {
                cachedMovieId = resolvedMovieId
                episodesPageCache.clear()
            }

            val movieDetailDeferred = async {
                if (resolvedMovieId.isNotBlank()) repository.getMovieDetail(resolvedMovieId)
                else Result.success(null)
            }
            val commentsDeferred = async { repository.getEpisodeComments(episodeId, page = 0, sort = "new") }
            val postersDeferred = async { if (resolvedMovieId.isNotBlank()) repository.getMoviePosters(resolvedMovieId) else Result.success(emptyList()) }
            val coversDeferred = async { if (resolvedMovieId.isNotBlank()) repository.getMovieCovers(resolvedMovieId) else Result.success(emptyList()) }
            val episodesPage0Deferred = async {
                if (resolvedMovieId.isNotBlank() && !episodesPageCache.containsKey(0)) {
                    repository.getMovieEpisodes(resolvedMovieId, page = 0)
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
                    val p1Result = repository.getMovieEpisodes(resolvedMovieId, page = 1)
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

                ensureRangeEpisodesLoaded(resolvedMovieId, chunkStart, chunkEnd, maxEpNum)

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

            launchSkipTimestampsFetch(
                forEpisodeId = episodeId,
                episodeNumberRaw = streamData?.episode?.index ?: _uiState.value.episode?.index,
                movieDetailData = movieDetail,
                expectedEpisodes = maxOf(maxEpNum, _uiState.value.episodes.size)
            )
        }
    }

    /**
     * Reaktif memilih episode baru:
     * 1. Mengubah state currentEpisodeId dan currentEpisodeNumber secara instan.
     * 2. Mereset posisi durasi ke 0L dan mengosongkan selectedServer agar player tidak memutar stream lama.
     * 3. Memanggil ulang API Stream/Server Resolusi untuk newEpisodeId.
     * 4. Memanggil ulang API Komentar dan views untuk newEpisodeId.
     */
    fun selectEpisode(newEpisodeId: String, newEpisodeNumber: String? = null) {
        if (_uiState.value.episodeId == newEpisodeId && !_uiState.value.isLoading && _uiState.value.selectedServer != null) {
            return
        }

        episodeJob?.cancel()
        skipJob?.cancel()
        manualServerSelected = false

        val existingEp = _uiState.value.episodes.firstOrNull { it.id == newEpisodeId }
            ?: _uiState.value.episodes.firstOrNull { newEpisodeNumber != null && it.index == newEpisodeNumber }

        val targetEpNumber = newEpisodeNumber ?: existingEp?.index ?: _uiState.value.episode?.index

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                episodeId = newEpisodeId,
                episode = existingEp ?: EpisodeItem(
                    id = newEpisodeId,
                    index = targetEpNumber,
                    title = existingEp?.title ?: "Episode ${targetEpNumber ?: ""}",
                    views = existingEp?.views,
                    idMovie = it.movieId
                ),
                selectedServer = null, // Segera hentikan pemutaran stream lama!
                servers = emptyList(),
                currentPositionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                introSegment = null,
                outroSegment = null,
                skipSourceLabel = null,
                comments = emptyList(),
                activeCommentForReplies = null
            )
        }

        val currentMovieDetail = _uiState.value.movieDetail
        val currentTotalEps = _uiState.value.totalEpisodesCount
        if (currentMovieDetail != null && !targetEpNumber.isNullOrBlank()) {
            launchSkipTimestampsFetch(
                forEpisodeId = newEpisodeId,
                episodeNumberRaw = targetEpNumber,
                movieDetailData = currentMovieDetail,
                expectedEpisodes = currentTotalEps.takeIf { it > 0 } ?: _uiState.value.episodes.size
            )
        }

        episodeJob = viewModelScope.launch {
            val streamDeferred = async {
                preloadedStreams.remove(newEpisodeId)?.let { Result.success(it) }
                    ?: repository.getEpisodeStream(newEpisodeId)
            }
            val commentsDeferred = async { repository.getEpisodeComments(newEpisodeId, page = 0, sort = "new") }

            val streamResult = streamDeferred.await()
            val commentsResult = commentsDeferred.await()

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
            val defaultServer = resolvePreferredServer(servers, _uiState.value.preferredQuality)

            val comments = commentsResult.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    episode = streamData?.episode ?: existingEp ?: it.episode,
                    episodeNext = streamData?.episodeNext,
                    servers = servers,
                    selectedServer = defaultServer,
                    comments = comments,
                    currentPositionMs = 0L,
                    durationMs = 0L
                )
            }
            schedulePreloadNextEpisode(streamData, newEpisodeId)

            val finalEpNum = streamData?.episode?.index ?: targetEpNumber
            val mDetail = _uiState.value.movieDetail
            if (mDetail != null && !finalEpNum.isNullOrBlank() && finalEpNum != targetEpNumber) {
                launchSkipTimestampsFetch(
                    forEpisodeId = newEpisodeId,
                    episodeNumberRaw = finalEpNum,
                    movieDetailData = mDetail,
                    expectedEpisodes = _uiState.value.totalEpisodesCount.takeIf { it > 0 } ?: _uiState.value.episodes.size
                )
            }
        }
    }

    fun setEpisodeViewMode(mode: EpisodeViewMode) {
        _uiState.update { it.copy(episodeViewMode = mode) }
    }

    fun selectServer(server: ServerItem) {
        manualServerSelected = true
        // Duration synchronization: preserves currentPositionMs!
        _uiState.update {
            it.copy(
                selectedServer = server,
                isServerPickerOpen = false
            )
        }
    }

    fun updatePosition(positionMs: Long, durationMs: Long) {
        if (_uiState.value.isLoading || _uiState.value.selectedServer == null) {
            return
        }
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
        val existingEp = _uiState.value.episodes.firstOrNull { it.id == newEpisodeId }
        selectEpisode(newEpisodeId = newEpisodeId, newEpisodeNumber = existingEp?.index)
    }
}
