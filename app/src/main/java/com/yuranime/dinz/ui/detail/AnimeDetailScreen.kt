package com.yuranime.dinz.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.AniListCharacterEntry
import com.yuranime.dinz.data.model.AniListCreditsData
import com.yuranime.dinz.data.model.AniListStaffEntry
import com.yuranime.dinz.data.model.AnimeSoundtrackResult
import com.yuranime.dinz.data.model.AnimeSoundtrackTrack
import com.yuranime.dinz.data.model.CommunityMediaItem
import kotlinx.coroutines.delay
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.MovieDetail
import com.yuranime.dinz.data.model.SeasonItem
import com.yuranime.dinz.ui.components.CoachmarkStep
import com.yuranime.dinz.ui.components.SpotlightCoachmarkOverlay
import com.yuranime.dinz.ui.components.onboardingTarget
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorMetricFavorites
import com.yuranime.dinz.ui.theme.ColorMetricViews
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary

@Composable
fun AnimeDetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAnimeClick: (String) -> Unit = {},
    onEpisodeClick: (String) -> Unit = {},
    onCuplixClick: (CuplixItem) -> Unit = {},
    viewModel: AnimeDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val targetBounds = remember { mutableStateMapOf<String, Rect>() }
    val watchSyncVer by FirebaseManager.watchHistorySyncVersion.collectAsState()
    var watchedEpisodeIds by remember(movieId) {
        mutableStateOf(LocalAuthDatabase(context).getWatchedEpisodeIds(movieId))
    }

    LaunchedEffect(movieId, watchSyncVer) {
        watchedEpisodeIds = LocalAuthDatabase(context).getWatchedEpisodeIds(movieId)
    }

    // Single-instance audio preview player untuk tab SOUNDTRACK (otomatis berhenti saat pindah tab / keluar halaman)
    var activeTrackId by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var isAudioBuffering by remember { mutableStateOf(false) }
    var audioPositionMs by remember { mutableLongStateOf(0L) }
    var audioDurationMs by remember { mutableLongStateOf(30_000L) }

    val audioPreviewPlayer = remember(context) {
        ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
    }

    val stopAudioPreview: () -> Unit = remember(audioPreviewPlayer) {
        {
            runCatching {
                audioPreviewPlayer.pause()
                audioPreviewPlayer.stop()
                audioPreviewPlayer.clearMediaItems()
            }
            activeTrackId = null
            isAudioPlaying = false
            isAudioBuffering = false
            audioPositionMs = 0L
        }
    }

    val toggleAudioPreview: (AnimeSoundtrackTrack) -> Unit = remember(audioPreviewPlayer) {
        { track ->
            val previewUrl = track.previewUrl?.takeIf { it.startsWith("http") }
            if (previewUrl != null) {
                if (activeTrackId == track.id) {
                    if (isAudioPlaying) {
                        runCatching { audioPreviewPlayer.pause() }
                        isAudioPlaying = false
                    } else {
                        runCatching {
                            if (audioPreviewPlayer.playbackState == Player.STATE_ENDED) {
                                audioPreviewPlayer.seekTo(0L)
                            }
                            audioPreviewPlayer.playWhenReady = true
                            audioPreviewPlayer.play()
                        }
                        isAudioPlaying = true
                    }
                } else {
                    runCatching {
                        audioPreviewPlayer.stop()
                        audioPreviewPlayer.clearMediaItems()
                        audioPreviewPlayer.setMediaItem(MediaItem.fromUri(previewUrl))
                        audioPreviewPlayer.prepare()
                        audioPreviewPlayer.playWhenReady = true
                        audioPreviewPlayer.play()
                        activeTrackId = track.id
                        isAudioPlaying = true
                        isAudioBuffering = true
                        audioPositionMs = 0L
                        audioDurationMs = ((track.durationSec ?: 30) * 1000L).coerceAtLeast(1000L)
                    }.onFailure {
                        stopAudioPreview()
                    }
                }
            }
        }
    }

    DisposableEffect(audioPreviewPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isAudioBuffering = true
                    Player.STATE_READY -> {
                        isAudioBuffering = false
                        val dur = audioPreviewPlayer.duration
                        if (dur > 0L) audioDurationMs = dur
                    }
                    Player.STATE_ENDED -> {
                        isAudioPlaying = false
                        isAudioBuffering = false
                        audioPositionMs = 0L
                        runCatching {
                            audioPreviewPlayer.seekTo(0L)
                            audioPreviewPlayer.pause()
                        }
                    }
                    else -> isAudioBuffering = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isAudioPlaying = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                stopAudioPreview()
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                if (activeTrackId != null) {
                    runCatching { audioPreviewPlayer.pause() }
                    isAudioPlaying = false
                }
            }
        }
        audioPreviewPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            audioPreviewPlayer.removeListener(listener)
            runCatching {
                audioPreviewPlayer.stop()
                audioPreviewPlayer.release()
            }
        }
    }

    LaunchedEffect(activeTrackId, isAudioPlaying) {
        while (activeTrackId != null && isAudioPlaying) {
            val pos = runCatching { audioPreviewPlayer.currentPosition }.getOrDefault(0L).coerceAtLeast(0L)
            val dur = runCatching { audioPreviewPlayer.duration }.getOrDefault(0L)
            audioPositionMs = pos
            if (dur > 0L) audioDurationMs = dur
            delay(150L)
        }
    }

    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab != DetailTab.SOUNDTRACK && activeTrackId != null) {
            stopAudioPreview()
        }
    }

    LaunchedEffect(movieId) {
        stopAudioPreview()
        viewModel.loadAnimeDetail(movieId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        when {
            uiState.isLoading && uiState.movieDetail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorAccentPrimary,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            uiState.errorMessage != null && uiState.movieDetail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Terjadi kesalahan",
                        color = ColorTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2028))
                            .clickable { viewModel.loadAnimeDetail(movieId) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = ColorTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Coba Lagi",
                                color = ColorTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            else -> {
                uiState.movieDetail?.let { movie ->
                    val visibleTabs = remember(uiState.isLoadingSoundtrack, uiState.soundtrack) {
                        DetailTab.entries.filter { tab ->
                            if (tab == DetailTab.SOUNDTRACK) {
                                !uiState.isLoadingSoundtrack && !uiState.soundtrack.isEmpty
                            } else {
                                true
                            }
                        }
                    }
                    val activeTabIndex = visibleTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)

                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Hero Cover Header with Edge-to-Edge Status Bar Handling
                        HeroCoverHeader(
                            movie = movie,
                            latestEpisode = uiState.latestEpisode,
                            isFavorite = uiState.isFavorite,
                            targetBounds = targetBounds,
                            onBackClick = {
                                stopAudioPreview()
                                onBackClick()
                            },
                            onFavoriteClick = { viewModel.toggleFavorite() },
                            onShareClick = {
                                runCatching {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Nonton ${movie.title} di Yuranime!")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan anime"))
                                }
                            },
                            onPlayClick = {
                                val targetEpId = uiState.latestEpisode?.id ?: uiState.episodes.firstOrNull()?.id
                                if (targetEpId != null) {
                                    stopAudioPreview()
                                    onEpisodeClick(targetEpId)
                                }
                            }
                        )

                        // 2. Scrollable TabBar (SOUNDTRACK disembunyikan otomatis jika lagu OP & ED kosong)
                        ScrollableTabRow(
                            selectedTabIndex = activeTabIndex,
                            containerColor = ColorBackgroundPrimary,
                            contentColor = ColorAccentPrimary,
                            edgePadding = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onboardingTarget("detail_tabs", targetBounds),
                            indicator = { tabPositions ->
                                if (activeTabIndex < tabPositions.size) {
                                    Box(
                                        modifier = Modifier
                                            .tabIndicatorOffset(tabPositions[activeTabIndex])
                                            .height(2.5.dp)
                                            .background(ColorAccentPrimary)
                                    )
                                }
                            },
                            divider = {}
                        ) {
                            visibleTabs.forEach { tab ->
                                val isSelected = uiState.selectedTab == tab
                                Tab(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(tab) },
                                    text = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) ColorAccentPrimary else ColorTextSecondary
                                        )
                                    },
                                    modifier = Modifier.height(48.dp)
                                )
                            }
                        }

                        // 3. Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.selectedTab) {
                                DetailTab.INFO -> TabInfoContent(
                                    movie = movie,
                                    creditsData = uiState.creditsData,
                                    onOpenCreditsTab = { viewModel.selectTab(DetailTab.CREDITS) }
                                )
                                DetailTab.EPISODE -> TabEpisodeContent(
                                    episodes = uiState.filteredEpisodes,
                                    watchedEpisodeIds = watchedEpisodeIds,
                                    searchQuery = uiState.episodeSearchQuery,
                                    onSearchQueryChange = { viewModel.setEpisodeSearchQuery(it) },
                                    isSortAscending = uiState.isEpisodeSortAscending,
                                    onToggleSort = { viewModel.toggleEpisodeSort() },
                                    isLoadingMore = uiState.isLoadingMoreEpisodes,
                                    onLoadMore = { viewModel.loadMoreEpisodes() },
                                    onEpisodeClick = { ep ->
                                        ep.id?.let {
                                            stopAudioPreview()
                                            onEpisodeClick(it)
                                        }
                                    }
                                )
                                DetailTab.CREDITS -> TabCharactersAndStaffContent(
                                    isLoading = uiState.isLoadingCredits,
                                    errorMessage = uiState.creditsError,
                                    creditsData = uiState.creditsData,
                                    selectedSection = uiState.selectedCreditsSection,
                                    selectedVaLanguage = uiState.selectedVaLanguage,
                                    onSelectSection = { viewModel.selectCreditsSection(it) },
                                    onSelectVaLanguage = { viewModel.selectVaLanguage(it) },
                                    onRetry = { viewModel.retryLoadCredits(forceRefresh = true) },
                                    onImageClick = { viewModel.openFullScreenImage(it) }
                                )
                                DetailTab.SOUNDTRACK -> TabSoundtrackContent(
                                    soundtrack = uiState.soundtrack,
                                    activeTrackId = activeTrackId,
                                    isPlaying = isAudioPlaying,
                                    isBuffering = isAudioBuffering,
                                    currentPosMs = audioPositionMs,
                                    durationMs = audioDurationMs,
                                    onTogglePlay = toggleAudioPreview
                                )
                                DetailTab.SEASON -> TabSeasonContent(
                                    seasons = uiState.seasons,
                                    onSeasonClick = { seasonId ->
                                        stopAudioPreview()
                                        onAnimeClick(seasonId)
                                    }
                                )
                                DetailTab.CUPLIX -> TabCuplixContent(
                                    cuplixList = uiState.cuplixList,
                                    onCuplixClick = {
                                        stopAudioPreview()
                                        onCuplixClick(it)
                                    }
                                )
                                DetailTab.POSTER -> TabPosterContent(
                                    posters = uiState.posters,
                                    onImageClick = { viewModel.openFullScreenImage(it) }
                                )
                                DetailTab.COVER -> TabCoverContent(
                                    covers = uiState.covers,
                                    onImageClick = { viewModel.openFullScreenImage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Full Screen Image Preview Modal
        uiState.fullScreenPreviewUrl?.let { previewUrl ->
            FullScreenImagePreview(
                imageUrl = previewUrl,
                onDismiss = { viewModel.closeFullScreenImage() }
            )
        }

        // 5. Spotlight Coachmark Onboarding for AnimeDetailScreen
        var showDetailOnboarding by remember {
            mutableStateOf(!com.yuranime.dinz.data.local.AppPreferences.isDetailOnboardingCompleted(context))
        }
        val detailSteps = remember {
            listOf(
                CoachmarkStep(
                    id = "detail_play_fav",
                    title = "Tombol Putar Cepat & Favorit",
                    description = "Mulai streaming instan dari episode terakhir yang kamu tonton atau bookmark ke daftar favoritmu.",
                    cornerRadiusDp = 14f
                ),
                CoachmarkStep(
                    id = "detail_tabs",
                    title = "Informasi & Tab Lengkap",
                    description = "Pilih tab INFO untuk sinopsis, EPISODE untuk daftar episode tayang, atau tab SEASON, CUPLIX, dan POSTER untuk konten lainnya.",
                    cornerRadiusDp = 12f
                )
            )
        }

        SpotlightCoachmarkOverlay(
            steps = detailSteps,
            isVisible = showDetailOnboarding && uiState.movieDetail != null,
            targetBounds = targetBounds,
            onDismiss = {
                showDetailOnboarding = false
                com.yuranime.dinz.data.local.AppPreferences.setDetailOnboardingCompleted(context, true)
            }
        )
    }
}

// ==================== 1. HERO COVER HEADER ====================

@Composable
private fun HeroCoverHeader(
    movie: MovieDetail,
    latestEpisode: EpisodeItem?,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onPlayClick: () -> Unit,
    targetBounds: MutableMap<String, Rect>? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp) // Proportional enlarged cover
    ) {
        // Landscape Cover Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.getEffectiveCover())
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime_wide)
                .error(R.drawable.placeholder_yuranime_wide)
                .build(),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Smooth Dark Gradient (Matte clean fade)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88000000),
                            Color(0x33000000),
                            Color(0xCC0B0B0B),
                            ColorBackgroundPrimary
                        )
                    )
                )
        )

        // Top Control Bar with statusBarsPadding (Transparent floating icons without black circles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back button (Thin transparent arrow)
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Right: Badges & Outline Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status Pill (e.g. ONGOING / FINISHED)
                val statusText = (movie.status ?: "ONGOING").uppercase()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Type Pill (e.g. SERIES / MOVIE) - Teal
                val typeText = (movie.type ?: "SERIES").uppercase()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D9488))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = typeText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Favorite Icon (Outline white, gold when active)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) ColorMetricFavorites else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share Icon (Outline white)
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom Info Row
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title + Views + Favorites (Using Vector Icons, No emojis)
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = movie.title ?: "",
                    color = ColorTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Views with clean Vector Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = ColorMetricViews,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${movie.views ?: "0"} views",
                            color = ColorMetricViews,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Favorites with clean Vector Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = ColorMetricFavorites,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${movie.favorites ?: "0"} favorites",
                            color = ColorMetricFavorites,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right: Play Button (Thin white outline circle with white play triangle and 'ep 1')
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (targetBounds != null) Modifier.onboardingTarget("detail_play_fav", targetBounds)
                        else Modifier
                    )
                    .clickable { onPlayClick() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xCCFFFFFF), CircleShape)
                        .background(Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                val epText = "ep " + (latestEpisode?.index ?: "1")
                Text(
                    text = epText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==================== 2. TAB INFO ====================

@Composable
private fun TabInfoContent(
    movie: MovieDetail,
    creditsData: AniListCreditsData? = null,
    onOpenCreditsTab: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
        ) {
            // Header Poster (~44% width) & Titles
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Larger vertical poster (2:3 aspect ratio, ~44% screen width)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(movie.getEffectivePoster())
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_yuranime)
                            .error(R.drawable.placeholder_yuranime)
                            .build(),
                        contentDescription = movie.title,
                        modifier = Modifier
                            .fillMaxWidth(0.44f)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF181920)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Judul",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = movie.title ?: "-",
                            color = ColorTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Judul Alternatif",
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = movie.synonyms?.ifBlank { "-" } ?: "-",
                            color = ColorTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Left-Aligned Vertical Metadata Stack (No space-between, no chips)
            item {
                Spacer(modifier = Modifier.height(18.dp))

                // Sinopsis
                Text(
                    text = "Sinopsis",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.synopsis?.ifBlank { "Belum ada sinopsis tersedia." } ?: "Belum ada sinopsis tersedia.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tipe
                Text(text = "Tipe", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = movie.type ?: "SERIES", color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                // Aired Start
                Text(text = "Aired Start", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = movie.airedStart ?: "-", color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                // Aired End
                Text(text = "Aired End", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = movie.airedEnd ?: "-", color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                // Studios
                Text(text = "Studios", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = movie.studio ?: "-", color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(14.dp))

                // Kategori (Plain comma-separated text, NO chip boxes)
                Text(text = "Kategori", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = movie.getDisplayGenre(),
                    color = ColorTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Akses Cepat ke Karakter & Staff (AniList)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF11131A))
                        .border(1.dp, Color(0xFF1E222E), RoundedCornerShape(12.dp))
                        .clickable { onOpenCreditsTab() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Karakter, Seiyuu & Staff",
                            color = ColorTextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitle = if (creditsData != null) {
                            "${creditsData.characters.size} Karakter • ${creditsData.staff.size} Staff (AniList)"
                        } else {
                            "Lihat profil karakter, pengisi suara & kreator di AniList"
                        }
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Buka Karakter & Staff",
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Floating Action Button Edit (Bottom Right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2028))
                .clickable { /* Edit metadata action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Info",
                tint = ColorAccentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== 3. TAB EPISODE (With Search, Sort & Infinite Scroll) ====================

@Composable
private fun TabEpisodeContent(
    episodes: List<EpisodeItem>,
    watchedEpisodeIds: Set<String> = emptySet(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSortAscending: Boolean,
    onToggleSort: () -> Unit,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit
) {
    val gridState = rememberLazyGridState()

    // Trigger infinite scroll when near end
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Episode Search & Sort Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181A22))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ColorTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = ColorTextPrimary, fontSize = 12.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(ColorAccentPrimary),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(text = "Cari nomor episode...", color = Color(0xFF6B7280), fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sort Toggle Button (Terbaru / Terlama)
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181A22))
                    .clickable { onToggleSort() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isSortAscending) "Terlama" else "Terbaru",
                        color = ColorTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Episodes Grid
        if (episodes.isEmpty() && !isLoadingMore) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Tidak ada episode ditemukan.", color = ColorTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes, key = { it.id ?: (it.index.toString() + "_" + it.hashCode()) }) { ep ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f) // Smooth 1:1 rounded square
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181920))
                            .clickable { onEpisodeClick(ep) }
                    ) {
                        val imageUrl = ep.getEffectiveImage()
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.placeholder_yuranime_wide)
                                    .error(R.drawable.placeholder_yuranime_wide)
                                    .build(),
                                contentDescription = ep.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Clean wolf silhouette placeholder
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                    contentDescription = "Yuranime Kitsune Logo",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Red "NEW" ribbon
                        if (ep.isNew == "1") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .clip(RoundedCornerShape(bottomEnd = 6.dp))
                                    .background(Color(0xFFEF4444))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Tanda episode sudah ditonton (tersinkron lintas perangkat untuk user login)
                        val isWatched = !ep.id.isNullOrBlank() && ep.id in watchedEpisodeIds
                        if (isWatched) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xD90B0B0E))
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Sudah ditonton",
                                    tint = ColorAccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Episode Number Badge (Bottom-Right, crisp white bold)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ep.index ?: "1",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Loading indicator item at bottom
                if (isLoadingMore) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ColorAccentPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 4. TAB SEASON ====================

@Composable
private fun TabSeasonContent(
    seasons: List<SeasonItem>,
    onSeasonClick: (String) -> Unit
) {
    if (seasons.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Tidak ada relasi season lain.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(seasons, key = { it.id ?: it.hashCode().toString() }) { season ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF181920))
                    .clickable { season.id?.let { onSeasonClick(it) } }
            ) {
                // Cover image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(season.getEffectiveCover())
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime_wide)
                        .error(R.drawable.placeholder_yuranime_wide)
                        .build(),
                    contentDescription = season.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x70000000),
                                    Color(0xF50B0B0B)
                                )
                            )
                        )
                )

                // Season Title (Bold 23sp) + Stacked Views/Favorites to the RIGHT
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Big Season Title
                    val titleText = season.season?.ifBlank { season.title } ?: season.title ?: ""
                    Text(
                        text = titleText,
                        color = ColorTextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Stacked 2 lines to the RIGHT of Title
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Views row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ColorMetricViews,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${season.views ?: "0"} views",
                                color = ColorMetricViews,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Favorites row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = ColorMetricFavorites,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${season.favorites ?: "0"} favorites",
                                color = ColorMetricFavorites,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 5. TAB CUPLIX (Pure Clean Images) ====================

@Composable
private fun TabCuplixContent(
    cuplixList: List<CuplixItem>,
    onCuplixClick: (CuplixItem) -> Unit = {}
) {
    if (cuplixList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada Cuplix untuk anime ini.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cuplixList, key = { it.id ?: it.hashCode().toString() }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181920))
                    .clickable {
                        onCuplixClick(item)
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.getEffectiveThumbnail())
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime_wide)
                        .error(R.drawable.placeholder_yuranime_wide)
                        .build(),
                    contentDescription = item.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// ==================== 6. TAB POSTER & COVER (100% Pure Images + Full Screen Preview) ====================

@Composable
private fun TabPosterContent(
    posters: List<CommunityMediaItem>,
    onImageClick: (String) -> Unit
) {
    if (posters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada poster tersedia.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posters, key = { it.id ?: it.hashCode().toString() }) { item ->
            val imageUrl = item.getEffectiveImage()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181920))
                    .clickable { if (imageUrl.isNotBlank()) onImageClick(imageUrl) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = "Poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun TabCoverContent(
    covers: List<CommunityMediaItem>,
    onImageClick: (String) -> Unit
) {
    if (covers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada cover tersedia.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(covers, key = { it.id ?: it.hashCode().toString() }) { item ->
            val imageUrl = item.getEffectiveImage()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181920))
                    .clickable { if (imageUrl.isNotBlank()) onImageClick(imageUrl) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime_wide)
                        .error(R.drawable.placeholder_yuranime_wide)
                        .build(),
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

// ==================== 6B. TAB KARAKTER & STAFF (ANILIST AUTO-DETECT) ====================

@Composable
private fun TabCharactersAndStaffContent(
    isLoading: Boolean,
    errorMessage: String?,
    creditsData: AniListCreditsData?,
    selectedSection: CreditsSectionTab,
    selectedVaLanguage: String,
    onSelectSection: (CreditsSectionTab) -> Unit,
    onSelectVaLanguage: (String) -> Unit,
    onRetry: () -> Unit,
    onImageClick: (String) -> Unit
) {
    if (isLoading && creditsData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = ColorAccentPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mendeteksi anime & memuat Karakter/Staff AniList...",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (creditsData == null && !errorMessage.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A1C24))
                        .border(1.dp, Color(0xFF2B3040), RoundedCornerShape(10.dp))
                        .clickable { onRetry() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat ulang",
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Coba Muat Ulang",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    val data = creditsData
    val languages = data?.availableVaLanguages.orEmpty()
    val characters = data?.characters.orEmpty()
    val staffList = data?.staff.orEmpty()

    var isLangMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Kontrol: Segmented Karakter / Staff + Filter Bahasa Seiyuu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF11131A))
                    .border(1.dp, Color(0xFF1E222E), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CreditsSectionTab.entries.forEach { section ->
                    val active = selectedSection == section
                    val count = if (section == CreditsSectionTab.CHARACTERS) characters.size else staffList.size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) ColorAccentPrimary else Color.Transparent)
                            .clickable { onSelectSection(section) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${section.label} ($count)",
                            color = if (active) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedSection == CreditsSectionTab.CHARACTERS && languages.isNotEmpty()) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF11131A))
                            .border(1.dp, Color(0xFF1E222E), RoundedCornerShape(10.dp))
                            .clickable { isLangMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedVaLanguage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Pilih Bahasa Seiyuu",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isLangMenuExpanded,
                        onDismissRequest = { isLangMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF161922))
                            .border(1.dp, Color(0xFF232838), RoundedCornerShape(8.dp))
                    ) {
                        languages.forEach { lang ->
                            val isLangSelected = lang.equals(selectedVaLanguage, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang,
                                        color = if (isLangSelected) ColorAccentPrimary else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isLangSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectVaLanguage(lang)
                                    isLangMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Caption kecocokan AniList + tombol refresh manual
        if (data != null && data.matchedTitle.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AniList: ${data.matchedTitle}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Segarkan data AniList",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onRetry() }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedSection == CreditsSectionTab.CHARACTERS) {
            if (characters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data karakter di AniList untuk judul ini.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(characters, key = { it.id }) { character ->
                        val matchedVa = character.voiceActors.firstOrNull {
                            it.language.equals(selectedVaLanguage, ignoreCase = true)
                        }
                        PairedCharacterCard(
                            character = character,
                            voiceActorName = matchedVa?.name,
                            voiceActorLanguage = matchedVa?.language,
                            voiceActorImage = matchedVa?.imageUrl,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        } else {
            if (staffList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data staff produksi di AniList untuk judul ini.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(staffList, key = { it.id }) { staff ->
                        StaffCreditCard(
                            staff = staff,
                            onImageClick = onImageClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairedCharacterCard(
    character: AniListCharacterEntry,
    voiceActorName: String?,
    voiceActorLanguage: String?,
    voiceActorImage: String?,
    onImageClick: (String) -> Unit
) {
    val isMain = character.role.equals("MAIN", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF11131A))
            .border(1.dp, Color(0xFF1E222E), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kiri: Karakter
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF181922))
                    .clickable(enabled = !character.imageUrl.isNullOrBlank()) {
                        character.imageUrl?.let(onImageClick)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!character.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(character.imageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_yuranime)
                            .error(R.drawable.placeholder_yuranime)
                            .build(),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = character.name,
                    color = ColorTextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = character.formattedRole(),
                    color = if (isMain) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = if (isMain) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        // Pemisah vertikal halus
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E222E))
        )

        // Kanan: Voice Actor / Seiyuu
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = voiceActorName ?: "-",
                    color = if (voiceActorName != null) Color(0xFFE2E8F0) else Color(0xFF64748B),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = voiceActorLanguage ?: "Seiyuu tidak tersedia",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF181922))
                    .clickable(enabled = !voiceActorImage.isNullOrBlank()) {
                        voiceActorImage?.let(onImageClick)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!voiceActorImage.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(voiceActorImage)
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_yuranime)
                            .error(R.drawable.placeholder_yuranime)
                            .build(),
                        contentDescription = voiceActorName ?: "Seiyuu",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffCreditCard(
    staff: AniListStaffEntry,
    onImageClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF11131A))
            .border(1.dp, Color(0xFF1E222E), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(Color(0xFF181922))
                .clickable(enabled = !staff.imageUrl.isNullOrBlank()) {
                    staff.imageUrl?.let(onImageClick)
                },
            contentAlignment = Alignment.Center
        ) {
            if (!staff.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(staff.imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = staff.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = staff.name,
                color = ColorTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!staff.nativeName.isNullOrBlank()) {
                Text(
                    text = staff.nativeName,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = staff.role,
                color = Color(0xFFF87171),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== TAB SOUNDTRACK (OPENING & ENDING - CLEAN FLAT UI) ====================

@Composable
private fun TabSoundtrackContent(
    soundtrack: AnimeSoundtrackResult,
    activeTrackId: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    onTogglePlay: (AnimeSoundtrackTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
    ) {
        if (soundtrack.openings.isNotEmpty()) {
            item(key = "header_opening") {
                SoundtrackSectionHeader(
                    title = "OPENING",
                    count = soundtrack.openings.size
                )
            }
            items(
                items = soundtrack.openings,
                key = { it.id }
            ) { track ->
                val isCurrent = activeTrackId == track.id
                SoundtrackTrackRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    isBuffering = isCurrent && isBuffering,
                    currentPosMs = if (isCurrent) currentPosMs else 0L,
                    durationMs = if (isCurrent) durationMs else 30_000L,
                    onTogglePlay = { onTogglePlay(track) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.75.dp,
                    color = Color(0xFF1A1D26)
                )
            }
        }

        if (soundtrack.openings.isNotEmpty() && soundtrack.endings.isNotEmpty()) {
            item(key = "section_gap") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (soundtrack.endings.isNotEmpty()) {
            item(key = "header_ending") {
                SoundtrackSectionHeader(
                    title = "ENDING",
                    count = soundtrack.endings.size
                )
            }
            items(
                items = soundtrack.endings,
                key = { it.id }
            ) { track ->
                val isCurrent = activeTrackId == track.id
                SoundtrackTrackRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    isBuffering = isCurrent && isBuffering,
                    currentPosMs = if (isCurrent) currentPosMs else 0L,
                    durationMs = if (isCurrent) durationMs else 30_000L,
                    onTogglePlay = { onTogglePlay(track) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.75.dp,
                    color = Color(0xFF1A1D26)
                )
            }
        }
    }
}

@Composable
private fun SoundtrackSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = ColorTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp
        )
        Text(
            text = "$count lagu",
            color = Color(0xFF9CA3AF),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SoundtrackTrackRow(
    track: AnimeSoundtrackTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit
) {
    val hasPreview = !track.previewUrl.isNullOrBlank()
    val context = LocalContext.current
    var coverFailed by remember(track.id, track.coverUrl) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) Color(0xFF12141B) else ColorBackgroundPrimary)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Album / Fallback Ikon Musik Minimalis
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161922))
                    .border(
                        width = 1.dp,
                        color = if (isCurrent) ColorAccentPrimary.copy(alpha = 0.55f) else Color(0xFF232736),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) ColorAccentPrimary else Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
                val cover = track.coverUrl
                if (!cover.isNullOrBlank() && !coverFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(cover)
                            .crossfade(true)
                            .listener(
                                onError = { _, _ -> coverFailed = true }
                            )
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata Lagu (Badge OP/ED + Judul + Artis + Album/Episode)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x24FF5722))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.badgeLabel,
                            color = Color(0xFFFF6D3F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }

                    if (!track.episodeNote.isNullOrBlank()) {
                        Text(
                            text = track.episodeNote,
                            color = Color(0xFF9CA3AF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = track.title,
                    color = if (isCurrent) Color(0xFFFF6D3F) else ColorTextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.artist,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!track.album.isNullOrBlank()) {
                    Text(
                        text = track.album,
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tombol Play/Pause Preview Audio hanya jika URL preview tersedia
            if (hasPreview) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) ColorAccentPrimary else Color(0xFF1A1D26)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isCurrent) ColorAccentPrimary else Color(0xFF2B3040),
                            shape = CircleShape
                        )
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isBuffering -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        isPlaying -> Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = "Jeda Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Indikator Progres Sederhana untuk Preview Aktif
        if (isCurrent && hasPreview) {
            Spacer(modifier = Modifier.height(10.dp))
            val safeDuration = durationMs.coerceAtLeast(1000L)
            val progressFraction = (currentPosMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ColorAccentPrimary,
                    trackColor = Color(0xFF232736)
                )
                Text(
                    text = "${formatAudioSec(currentPosMs)} / ${formatAudioSec(safeDuration)}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatAudioSec(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(java.util.Locale.US, "%d:%02d", min, sec)
}

// ==================== 7. FULL SCREEN IMAGE PREVIEW MODAL ====================

@Composable
private fun FullScreenImagePreview(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full Screen Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
