package com.yuranime.dinz.ui.streaming

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.InlineStickerCodec
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.CommunityMediaItem
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.model.ServerItem
import com.yuranime.dinz.data.model.SkipSegment
import com.yuranime.dinz.data.model.SkipSegmentType
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.chat.ChatStickerPanel
import com.yuranime.dinz.ui.chat.InlineStickerComposerState
import com.yuranime.dinz.ui.chat.InlineStickerInputField
import com.yuranime.dinz.ui.chat.InlineStickerText
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.CoachmarkStep
import com.yuranime.dinz.ui.components.SpotlightCoachmarkOverlay
import com.yuranime.dinz.ui.components.onboardingTarget
import java.util.Locale
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorMetricFavorites
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ColorStreamAccent = Color.White
private val LARGE_COMMENT_STICKER_REGEX = Regex("^\\[sticker:(https?://[^\\]\\s]+)]$")

@OptIn(UnstableApi::class)
@Composable
fun StreamingScreen(
    episodeId: String,
    movieId: String,
    onBackClick: () -> Unit,
    onEpisodeChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: StreamingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefQuality by AppPreferences.defaultAnimeQuality.collectAsState()
    val prefSpeed by AppPreferences.defaultPlaybackSpeed.collectAsState()
    val prefAutoPlay by AppPreferences.autoPlayEnabled.collectAsState()
    val prefAutoSkipIntro by AppPreferences.autoSkipIntro.collectAsState()
    val prefPreloadNext by AppPreferences.preloadNextEpisode.collectAsState()
    val prefDoubleTapEnabled by AppPreferences.doubleTapSeekEnabled.collectAsState()
    val prefDoubleTapSeconds by AppPreferences.doubleTapSeekSeconds.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(
        prefQuality,
        prefSpeed,
        prefAutoPlay,
        prefAutoSkipIntro,
        prefPreloadNext,
        prefDoubleTapEnabled,
        prefDoubleTapSeconds
    ) {
        viewModel.syncPlaybackPreferences(
            preferredQuality = prefQuality,
            playbackSpeed = prefSpeed,
            autoPlayEnabled = prefAutoPlay,
            autoSkipIntro = prefAutoSkipIntro,
            preloadNextEpisode = prefPreloadNext,
            doubleTapSeekEnabled = prefDoubleTapEnabled,
            doubleTapSeekSeconds = prefDoubleTapSeconds
        )
    }
    val activity = context as? ComponentActivity
    val episodeRowState = rememberLazyListState()
    val targetBounds = remember { mutableStateMapOf<String, Rect>() }
    val commentComposerState = remember { InlineStickerComposerState() }
    val commentInputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    var showCommentStickerPanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.attachContext(context)
    }

    val prefs = remember { context.getSharedPreferences("yuranime_ui_prefs", android.content.Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        val savedMode = prefs.getString("episode_view_mode", null)
            ?: prefs.getString("episode_selector_view_mode", EpisodeViewMode.THUMBNAIL.name)
        val mode = try {
            EpisodeViewMode.valueOf(savedMode ?: EpisodeViewMode.THUMBNAIL.name)
        } catch (e: Exception) {
            EpisodeViewMode.THUMBNAIL
        }
        viewModel.setEpisodeViewMode(mode)
    }

    LaunchedEffect(episodeId, movieId) {
        viewModel.loadStreaming(episodeId = episodeId, movieId = movieId)
    }

    // Hanya cegat tombol back saat sub-sheet/dialog/fullscreen/panel stiker terbuka agar Predictive Back berjalan saat normal
    val hasInnerModal = uiState.fullScreenPreviewUrl != null ||
        showCommentStickerPanel ||
        uiState.activeCommentForReplies != null ||
        uiState.isGallerySheetOpen ||
        uiState.isServerPickerOpen ||
        uiState.isFullScreen
    BackHandler(enabled = hasInnerModal) {
        when {
            uiState.fullScreenPreviewUrl != null -> viewModel.closeFullScreenImage()
            showCommentStickerPanel -> showCommentStickerPanel = false
            uiState.activeCommentForReplies != null -> viewModel.closeReplies()
            uiState.isGallerySheetOpen -> viewModel.toggleGallerySheet()
            uiState.isServerPickerOpen -> viewModel.toggleServerPicker()
            uiState.isFullScreen -> {
                viewModel.toggleFullScreen()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Fullscreen screen orientation control & immersive system bars handling
    LaunchedEffect(uiState.isFullScreen) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (uiState.isFullScreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val localDb = remember { LocalAuthDatabase(context) }

    val persistWatchProgress: (StreamingUiState, Boolean, Boolean) -> Unit = remember(localDb, context) {
        { state, markCompleted, pushCloud ->
            val mId = state.movieId.ifBlank { state.episode?.idMovie ?: "" }
            val epId = state.episodeId
            if (mId.isNotBlank() && epId.isNotBlank() && (state.currentPositionMs > 3000L || markCompleted)) {
                val horizontalCoverUrl = state.movieDetail?.movie?.getEffectiveCover()?.takeIf { it.isNotBlank() }
                    ?: state.episode?.getEffectiveImage()?.takeIf { it.isNotBlank() }
                    ?: state.movieDetail?.movie?.getEffectivePoster()
                    ?: ""
                val epNum = state.episode?.index?.ifBlank { "1" } ?: "1"
                val savedItem = localDb.saveWatchProgress(
                    movieId = mId,
                    animeTitle = state.movieDetail?.movie?.title ?: state.episode?.title?.substringBefore(" Episode") ?: "Anime",
                    episodeId = epId,
                    episodeTitle = "Episode $epNum",
                    posterUrl = horizontalCoverUrl,
                    positionMs = state.currentPositionMs,
                    durationMs = state.durationMs,
                    episodeNumber = epNum,
                    isCompleted = markCompleted
                )
                if (savedItem != null) {
                    FirebaseManager.notifyWatchHistoryChanged()
                    if (pushCloud) {
                        FirebaseManager.syncWatchHistoryItem(context, savedItem)
                    }
                }
            }
        }
    }

    // Sinkronisasi berkala setiap 12 detik saat video sedang diputar (lokal selalu, Firebase khusus pengguna login)
    LaunchedEffect(uiState.isPlaying, uiState.episodeId) {
        if (uiState.isPlaying) {
            while (true) {
                delay(12_000L)
                val snapshot = viewModel.uiState.value
                if (snapshot.isPlaying && snapshot.currentPositionMs > 3000L) {
                    persistWatchProgress(snapshot, false, true)
                }
            }
        }
    }

    // Sinkronisasi segera saat video dijeda (Pause)
    var wasPlayingBefore by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isPlaying) {
        if (wasPlayingBefore && !uiState.isPlaying) {
            val snapshot = viewModel.uiState.value
            if (snapshot.currentPositionMs > 3000L) {
                persistWatchProgress(snapshot, false, true)
            }
        }
        wasPlayingBefore = uiState.isPlaying
    }

    // Sinkronisasi saat episode mencapai akhir (>= 85% atau sisa <= 30 dtk)
    var completedSyncedEpId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.episodeId, uiState.currentPositionMs, uiState.durationMs) {
        val dur = uiState.durationMs
        val pos = uiState.currentPositionMs
        val epId = uiState.episodeId
        if (epId.isNotBlank() &&
            epId != completedSyncedEpId &&
            dur > 30_000L &&
            (pos.toFloat() / dur.toFloat() >= 0.85f || (dur - pos) <= 30_000L)
        ) {
            completedSyncedEpId = epId
            persistWatchProgress(viewModel.uiState.value, true, true)
        }
    }

    // Sinkronisasi saat keluar dari halaman player
    DisposableEffect(uiState.episodeId, uiState.movieId) {
        onDispose {
            val snapshot = viewModel.uiState.value
            persistWatchProgress(snapshot, false, true)
        }
    }

    val firebaseProfile by FirebaseManager.userProfile.collectAsState()
    val isUserSignedIn = localDb.isLoggedIn() || FirebaseManager.isUserLoggedIn()
    val currentUserName = firebaseProfile?.name?.takeIf { it.isNotBlank() } ?: localDb.getUsername()
    val currentUserAvatar = firebaseProfile?.avatar?.takeIf { it.isNotBlank() } ?: localDb.getAvatarUrl()
    val currentUserBorderUrl = firebaseProfile?.equippedBorderUrl.orEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        val isInitialLoading = uiState.isLoading && uiState.selectedServer == null && uiState.episodes.isEmpty()
        val isInitialError = uiState.errorMessage != null && uiState.selectedServer == null && uiState.episodes.isEmpty()

        if (isInitialLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorStreamAccent, modifier = Modifier.size(40.dp))
            }
        } else if (isInitialError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = uiState.errorMessage ?: "Terjadi kesalahan", color = ColorTextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2028))
                        .clickable { viewModel.loadStreaming(episodeId, movieId) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = "Coba Lagi", color = ColorTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Custom Video Player (Media3 / ExoPlayer 16:9)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (uiState.isFullScreen) Modifier.fillMaxSize()
                            else Modifier.aspectRatio(16f / 9f)
                        )
                        .background(Color.Black)
                        .onboardingTarget("stream_player_settings", targetBounds)
                ) {
                    CustomExoPlayerView(
                        server = uiState.selectedServer,
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackClick = {
                            if (uiState.isFullScreen) {
                                viewModel.toggleFullScreen()
                            } else {
                                onBackClick()
                            }
                        },
                        onEpisodeChange = onEpisodeChange
                    )
                }

                // If not in full screen, render episode details and comments
                if (!uiState.isFullScreen) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.activeCommentForReplies != null -> {
                                uiState.activeCommentForReplies?.let { parentComment ->
                                    // Sub-View: Balasan Komentar
                                    CommentRepliesView(
                                        parentComment = parentComment,
                                        replies = uiState.repliesList,
                                        isLoading = uiState.isLoadingReplies,
                                        onBackClick = { viewModel.closeReplies() },
                                        onRequireLogin = { viewModel.triggerLoginPrompt() },
                                        onDeleteLocalReply = { replyId -> viewModel.deleteLocalComment(replyId) },
                                        onSendReply = { text ->
                                            val item = localDb.addLocalComment(
                                                episodeId = uiState.episodeId,
                                                text = text,
                                                parentId = uiState.activeCommentForReplies?.id,
                                                customUserName = currentUserName,
                                                customAvatarUrl = currentUserAvatar,
                                                customBorderUrl = currentUserBorderUrl
                                            )
                                            viewModel.addLocalReply(item)
                                        }
                                    )
                                }
                            }
                            uiState.isGallerySheetOpen -> {
                                // Full Gallery Sheet
                                FullGallerySheet(
                                    selectedTab = uiState.galleryTab,
                                    posters = uiState.posters,
                                    covers = uiState.covers,
                                    onTabSelected = { viewModel.setGalleryTab(it) },
                                    onBackClick = { viewModel.toggleGallerySheet() },
                                    onImageClick = { viewModel.openFullScreenImage(it) }
                                )
                            }
                            else -> {
                                // Main Content Column
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    // Anime Info langsung menyatu dengan latar hitam pekat (tanpa kotak/card abu-abu gelap)
                                    item(key = "episode_header") {
                                        var isSynopsisExpanded by remember(uiState.episodeId, uiState.movieId) {
                                            mutableStateOf(false)
                                        }
                                        val posterUrl = uiState.movieDetail?.movie?.getEffectivePoster()?.ifBlank { null }
                                            ?: uiState.episode?.getEffectiveImage().orEmpty()
                                        val realTitle = uiState.movieDetail?.movie?.title
                                            ?: uiState.episode?.title?.substringBefore(" Episode")
                                            ?: "Anime Streaming"
                                        val episodeNumber = uiState.episode?.index ?: "1"
                                        val shortViews = formatShortViews(
                                            uiState.episode?.views?.ifBlank { null }
                                                ?: uiState.movieDetail?.movie?.views
                                        )
                                        val formattedAiredStart = formatAiredStartDate(
                                            uiState.movieDetail?.movie?.airedStart
                                        )
                                        val realSynopsis = uiState.movieDetail?.movie?.synopsis?.ifBlank { null }
                                            ?: "Streaming anime episode terlengkap dengan kualitas jernih dan subtitle Indonesia resmi di platform Yuranime."

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            // Top Row: Vertical Poster on Left + Title & Metadata on Right
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(posterUrl.ifBlank { null })
                                                        .crossfade(true)
                                                        .placeholder(R.drawable.placeholder_yuranime)
                                                        .error(R.drawable.placeholder_yuranime)
                                                        .build(),
                                                    contentDescription = realTitle,
                                                    modifier = Modifier
                                                        .width(74.dp)
                                                        .aspectRatio(2f / 3f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF14161D)),
                                                    contentScale = ContentScale.Crop
                                                )

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = realTitle,
                                                        color = Color.White,
                                                        fontSize = 19.sp,
                                                        lineHeight = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Episode $episodeNumber",
                                                            color = Color(0xFFB8BEC9),
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )

                                                        Text(
                                                            text = "  •  ",
                                                            color = Color(0xFF6B7280),
                                                            fontSize = 12.5.sp
                                                        )

                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = "Views",
                                                            tint = Color(0xFFB8BEC9),
                                                            modifier = Modifier.size(13.5.dp)
                                                        )

                                                        Spacer(modifier = Modifier.width(4.dp))

                                                        Text(
                                                            text = shortViews,
                                                            color = Color(0xFFB8BEC9),
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )

                                                        if (formattedAiredStart != null) {
                                                            Text(
                                                                text = "  •  ",
                                                                color = Color(0xFF6B7280),
                                                                fontSize = 12.5.sp
                                                            )
                                                            Icon(
                                                                imageVector = Icons.Outlined.DateRange,
                                                                contentDescription = "Tanggal Rilis",
                                                                tint = Color(0xFFB8BEC9),
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = formattedAiredStart,
                                                                color = Color(0xFFB8BEC9),
                                                                fontSize = 12.5.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Bottom Section: Synopsis + Expand/Collapse "Selengkapnya >" (Putih Bersih)
                                            Text(
                                                text = realSynopsis,
                                                color = Color(0xFFD1D5DB),
                                                fontSize = 13.5.sp,
                                                lineHeight = 19.sp,
                                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isSynopsisExpanded) "Sembunyikan" else "Selengkapnya",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = if (isSynopsisExpanded) {
                                                        Icons.Default.KeyboardArrowUp
                                                    } else {
                                                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                                                    },
                                                    contentDescription = if (isSynopsisExpanded) {
                                                        "Sembunyikan sinopsis"
                                                    } else {
                                                        "Selengkapnya sinopsis"
                                                    },
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = Color(0x14FFFFFF),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }

                                    // Section: EPISODE SELECTOR & SEARCH INLINE
                                    item(key = "episode_selector_section") {
                                        val displayedEpisodes = remember(uiState.episodes, uiState.episodeSearchQuery) {
                                            val q = uiState.episodeSearchQuery.trim()
                                            if (q.isNotEmpty()) {
                                                uiState.episodes.filter { ep ->
                                                    (ep.index ?: "").contains(q) || (ep.title ?: "").contains(q, ignoreCase = true)
                                                }
                                            } else {
                                                uiState.episodes
                                            }
                                        }

                                        val activeEpId = uiState.episodeId.ifBlank { uiState.episode?.id ?: episodeId }
                                        val activeEpIndex = uiState.episode?.index
                                        val currentViewMode = uiState.episodeViewMode

                                        LaunchedEffect(activeEpId, activeEpIndex, displayedEpisodes.size, currentViewMode) {
                                            val activeIndex = displayedEpisodes.indexOfFirst {
                                                it.id == activeEpId || (it.index != null && it.index == activeEpIndex)
                                            }
                                            if (activeIndex >= 0) {
                                                episodeRowState.animateScrollToItem(activeIndex)
                                            }
                                        }

                                        LaunchedEffect(uiState.episodeSearchQuery, displayedEpisodes.size) {
                                            val q = uiState.episodeSearchQuery.trim()
                                            if (q.isNotEmpty()) {
                                                val matchIndex = displayedEpisodes.indexOfFirst { it.index == q }
                                                if (matchIndex >= 0) {
                                                    episodeRowState.animateScrollToItem(matchIndex)
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onboardingTarget("stream_episodes", targetBounds)
                                                .padding(top = 10.dp, bottom = 4.dp)
                                        ) {
                                            // Header Bar: EPISODE • Total + Inline Search Pill + Toggle View Mode
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "EPISODE",
                                                        color = ColorTextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.4.sp
                                                    )
                                                    val epCount = if (uiState.totalEpisodesCount > 0) uiState.totalEpisodesCount else uiState.episodes.size
                                                    if (epCount > 0) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "• $epCount Ep",
                                                            color = Color(0xFF9CA3AF),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .height(30.dp)
                                                            .width(115.dp)
                                                            .clip(RoundedCornerShape(15.dp))
                                                            .background(Color(0xFF1A1C22))
                                                            .padding(horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = "Cari episode",
                                                            tint = Color(0xFF8E929B),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        BasicTextField(
                                                            value = uiState.episodeSearchQuery,
                                                            onValueChange = { viewModel.setEpisodeSearchQuery(it) },
                                                            modifier = Modifier.weight(1f),
                                                            textStyle = TextStyle(
                                                                color = Color.White,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Medium
                                                            ),
                                                            singleLine = true,
                                                            cursorBrush = SolidColor(ColorStreamAccent),
                                                            decorationBox = { innerTextField ->
                                                                if (uiState.episodeSearchQuery.isEmpty()) {
                                                                    Text(
                                                                        text = "Cari ep.",
                                                                        color = Color(0xFF8E929B),
                                                                        fontSize = 11.5.sp
                                                                    )
                                                                }
                                                                innerTextField()
                                                            }
                                                        )
                                                        if (uiState.episodeSearchQuery.isNotEmpty()) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Hapus pencarian",
                                                                tint = Color(0xFF9CA3AF),
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .clickable { viewModel.setEpisodeSearchQuery("") }
                                                            )
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                val newMode = if (currentViewMode == EpisodeViewMode.THUMBNAIL) {
                                                                    EpisodeViewMode.COMPACT
                                                                } else {
                                                                    EpisodeViewMode.THUMBNAIL
                                                                }
                                                                viewModel.setEpisodeViewMode(newMode)
                                                                prefs.edit()
                                                                    .putString("episode_view_mode", newMode.name)
                                                                    .putString("episode_selector_view_mode", newMode.name)
                                                                    .apply()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (currentViewMode == EpisodeViewMode.THUMBNAIL) {
                                                                Icons.AutoMirrored.Filled.FormatListBulleted
                                                            } else {
                                                                Icons.Default.Image
                                                            },
                                                            contentDescription = if (currentViewMode == EpisodeViewMode.THUMBNAIL) {
                                                                "Beralih ke Mode Nomor"
                                                            } else {
                                                                "Beralih ke Mode Gambar"
                                                            },
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Range Selector untuk anime panjang (> 50 episode)
                                            if (uiState.totalEpisodesCount > 50) {
                                                val chunks = remember(uiState.totalEpisodesCount) {
                                                    val list = mutableListOf<Pair<Int, Int>>()
                                                    var start = 1
                                                    while (start <= uiState.totalEpisodesCount) {
                                                        val end = minOf(start + 99, uiState.totalEpisodesCount)
                                                        list.add(Pair(start, end))
                                                        start += 100
                                                    }
                                                    list
                                                }

                                                LazyRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(chunks) { chunk ->
                                                        val isSelected = uiState.selectedRange?.first == chunk.first &&
                                                                uiState.selectedRange?.second == chunk.second

                                                        Box(
                                                            modifier = Modifier
                                                                .height(26.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSelected) Color(0xFF262932) else Color(0xFF14161D))
                                                                .then(
                                                                    if (isSelected) {
                                                                        Modifier.border(1.dp, Color.White, RoundedCornerShape(6.dp))
                                                                    } else Modifier
                                                                )
                                                                .clickable {
                                                                    viewModel.selectEpisodeRange(chunk.first, chunk.second)
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${chunk.first} - ${chunk.second}",
                                                                color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Daftar Episode Horizontal LazyRow
                                            if (uiState.isLoadingEpisodes && displayedEpisodes.isEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(if (currentViewMode == EpisodeViewMode.THUMBNAIL) 84.dp else 40.dp)
                                                        .padding(horizontal = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        strokeWidth = 2.dp,
                                                        color = ColorStreamAccent
                                                    )
                                                }
                                            } else if (displayedEpisodes.isEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(if (currentViewMode == EpisodeViewMode.THUMBNAIL) 84.dp else 40.dp)
                                                        .padding(horizontal = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = if (uiState.episodeSearchQuery.isNotEmpty()) "Episode tidak ditemukan" else "Memuat episode...",
                                                        color = Color(0xFF9CA3AF),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            } else if (currentViewMode == EpisodeViewMode.THUMBNAIL) {
                                                // Mode 1: Thumbnail Card Row dengan border tipis PUTIH (#FFFFFF) untuk episode aktif
                                                LazyRow(
                                                    state = episodeRowState,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(
                                                        items = displayedEpisodes,
                                                        key = { it.id ?: it.index ?: "" }
                                                    ) { epItem ->
                                                        val isActive = epItem.id == activeEpId ||
                                                                (epItem.index != null && epItem.index == activeEpIndex)

                                                        val thumbnailModel = epItem.getEffectiveImage().ifBlank { null }
                                                            ?: uiState.movieDetail?.movie?.getEffectiveCover()?.ifBlank { null }
                                                            ?: uiState.movieDetail?.movie?.getEffectivePoster()?.ifBlank { null }

                                                        Box(
                                                            modifier = Modifier
                                                                .width(138.dp)
                                                                .aspectRatio(16f / 9f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(Color(0xFF14171F))
                                                                .then(
                                                                    if (isActive) {
                                                                        Modifier.border(
                                                                            width = 1.5.dp,
                                                                            color = Color.White,
                                                                            shape = RoundedCornerShape(10.dp)
                                                                        )
                                                                    } else Modifier
                                                                )
                                                                .clickable {
                                                                    if (!isActive) {
                                                                        val epId = epItem.id ?: ""
                                                                        val epIndex = epItem.index
                                                                        viewModel.selectEpisode(epId, epIndex)
                                                                        onEpisodeChange?.invoke(epId)
                                                                    }
                                                                }
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(thumbnailModel)
                                                                    .crossfade(true)
                                                                    .placeholder(R.drawable.placeholder_yuranime_wide)
                                                                    .error(R.drawable.placeholder_yuranime_wide)
                                                                    .build(),
                                                                contentDescription = "Episode ${epItem.index}",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )

                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(36.dp)
                                                                    .align(Alignment.BottomCenter)
                                                                    .background(
                                                                        Brush.verticalGradient(
                                                                            colors = listOf(
                                                                                Color.Transparent,
                                                                                Color(0x99000000),
                                                                                Color(0xDE000000)
                                                                            )
                                                                        )
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                                                contentAlignment = Alignment.BottomStart
                                                            ) {
                                                                Text(
                                                                    text = "Ep ${epItem.index ?: ""}",
                                                                    color = Color.White,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Mode 2: Compact Number Pill Row dengan border PUTIH (#FFFFFF) untuk episode aktif
                                                LazyRow(
                                                    state = episodeRowState,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(
                                                        items = displayedEpisodes,
                                                        key = { it.id ?: it.index ?: "" }
                                                    ) { epItem ->
                                                        val isActive = epItem.id == activeEpId ||
                                                                (epItem.index != null && epItem.index == activeEpIndex)

                                                        Box(
                                                            modifier = Modifier
                                                                .height(40.dp)
                                                                .defaultMinSize(minWidth = 48.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isActive) Color(0xFF22252E)
                                                                    else Color(0xFF14161D)
                                                                )
                                                                .then(
                                                                    if (isActive) {
                                                                        Modifier.border(
                                                                            width = 1.5.dp,
                                                                            color = Color.White,
                                                                            shape = RoundedCornerShape(8.dp)
                                                                        )
                                                                    } else Modifier
                                                                )
                                                                .clickable {
                                                                    if (!isActive) {
                                                                        val epId = epItem.id ?: ""
                                                                        val epIndex = epItem.index
                                                                        viewModel.selectEpisode(epId, epIndex)
                                                                        onEpisodeChange?.invoke(epId)
                                                                    }
                                                                }
                                                                .padding(horizontal = 12.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = epItem.index ?: "•",
                                                                color = if (isActive) Color.White else Color(0xFFCBD5E1),
                                                                fontSize = 13.sp,
                                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Section: COVER & POSTER >
                                    item(key = "cover_poster_section") {
                                        val interleavedMedia = remember(uiState.posters, uiState.covers) {
                                            val list = mutableListOf<Pair<CommunityMediaItem, Boolean>>()
                                            val maxLen = maxOf(uiState.posters.size, uiState.covers.size)
                                            for (i in 0 until maxLen) {
                                                if (i < uiState.posters.size) list.add(uiState.posters[i] to true)
                                                if (i < uiState.covers.size) list.add(uiState.covers[i] to false)
                                            }
                                            list
                                        }

                                        if (interleavedMedia.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable { viewModel.toggleGallerySheet(0) }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "COVER & POSTER",
                                                        color = ColorTextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = "Lihat Semua Cover & Poster",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.toggleGallerySheet(0) },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Menu Galeri",
                                                        tint = ColorTextSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(interleavedMedia) { (media, isPoster) ->
                                                    val mediaImg = media.getEffectiveImage()
                                                    val placeholderRes = if (isPoster) R.drawable.placeholder_yuranime else R.drawable.placeholder_yuranime_wide
                                                    Box(
                                                        modifier = Modifier
                                                            .height(176.dp)
                                                            .aspectRatio(if (isPoster) 2f / 3f else 16f / 9f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFF181A22))
                                                            .clickable {
                                                                if (mediaImg.isNotBlank()) viewModel.openFullScreenImage(mediaImg)
                                                                else viewModel.toggleGallerySheet(if (isPoster) 0 else 1)
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(mediaImg.ifBlank { null })
                                                                .crossfade(true)
                                                                .placeholder(placeholderRes)
                                                                .error(placeholderRes)
                                                                .build(),
                                                            contentDescription = if (isPoster) "Poster" else "Cover",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Section: COMMENTS (ALL)
                                    item(key = "comments_header") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onboardingTarget("stream_comments", targetBounds)
                                        ) {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "COMMENTS (${uiState.comments.size})",
                                                        color = ColorTextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (uiState.isLoadingMoreComments) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        CircularProgressIndicator(
                                                            color = Color.White,
                                                            strokeWidth = 1.5.dp,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            viewModel.toggleCommentSort()
                                                            val sortLabel = if (uiState.commentSort == "new") "Terpopuler" else "Terbaru"
                                                            Toast.makeText(context, "Urutkan: $sortLabel", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (uiState.commentSort == "popular") "Top" else "Terbaru",
                                                        color = Color(0xFF9CA3AF),
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Sort,
                                                        contentDescription = "Urutkan komentar",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    if (uiState.isLoadingComments && uiState.comments.isEmpty()) {
                                        item(key = "comments_loading") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    } else if (uiState.comments.isEmpty()) {
                                        item(key = "comments_empty") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Belum ada komentar di episode ini. Jadilah yang pertama berkomentar!",
                                                    color = Color(0xFF9CA3AF),
                                                    fontSize = 12.5.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    } else {
                                        items(
                                            items = uiState.comments,
                                            key = { it.id ?: "${it.userName}_${it.time}_${it.hashCode()}" }
                                        ) { comment ->
                                            CommentItemRow(
                                                comment = comment,
                                                onReplyClick = { viewModel.openReplies(comment) },
                                                onCopyText = { textToCopy ->
                                                    val clean = InlineStickerCodec.toReadableCopyText(textToCopy)
                                                    if (clean.isNotBlank()) {
                                                        clipboardManager.setText(AnnotatedString(clean))
                                                        Toast.makeText(context, "Komentar disalin", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onDeleteLocal = { commentId ->
                                                    viewModel.deleteLocalComment(commentId)
                                                    Toast.makeText(context, "Komentar dihapus", Toast.LENGTH_SHORT).show()
                                                },
                                                onRequireLogin = { viewModel.triggerLoginPrompt() }
                                            )
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = Color(0x14FFFFFF),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sticky Bottom Comment Input Bar + Sticker Picker (menggantikan FAB Chat Oranye)
                    if (uiState.activeCommentForReplies == null && !uiState.isGallerySheetOpen) {
                        val canSendComment = commentComposerState.hasSendableContent()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0B0B0B))
                                .navigationBarsPadding()
                                .imePadding()
                        ) {
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0x1AFFFFFF))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User Avatar + Avatar Decoration statis di kiri (foto profil tetap mendukung animasi)
                                StaticCommentAvatarWithBorder(
                                    avatarUrl = currentUserAvatar,
                                    userName = currentUserName,
                                    borderUrl = if (isUserSignedIn) currentUserBorderUrl else "",
                                    isCustomPhoto = currentUserAvatar.isNotBlank(),
                                    size = 34.dp,
                                    showPersonPlaceholder = !isUserSignedIn && currentUserAvatar.isBlank()
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Kolom input bulat gelap + ikon Emoji/Stiker + ikon Kirim di kanan
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color(0xFF1A1C22))
                                        .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        InlineStickerInputField(
                                            state = commentComposerState,
                                            placeholder = "Tulis komentar...",
                                            cursorColor = Color.White,
                                            placeholderColor = Color(0xFF8E929B),
                                            modifier = Modifier.focusRequester(commentInputFocusRequester),
                                            onFocused = {
                                                if (!isUserSignedIn) {
                                                    viewModel.triggerLoginPrompt()
                                                } else {
                                                    showCommentStickerPanel = false
                                                }
                                            }
                                        )
                                    }

                                    // Ikon Emoji / Stiker
                                    IconButton(
                                        onClick = {
                                            if (!isUserSignedIn) {
                                                viewModel.triggerLoginPrompt()
                                            } else if (showCommentStickerPanel) {
                                                showCommentStickerPanel = false
                                                commentInputFocusRequester.requestFocus()
                                                keyboardController?.show()
                                            } else {
                                                keyboardController?.hide()
                                                showCommentStickerPanel = true
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showCommentStickerPanel) {
                                                Icons.Rounded.Keyboard
                                            } else {
                                                Icons.Outlined.SentimentSatisfied
                                            },
                                            contentDescription = if (showCommentStickerPanel) "Tampilkan keyboard" else "Pilih stiker atau emoji",
                                            tint = if (showCommentStickerPanel) Color.White else Color(0xFF9CA3AF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Ikon Kirim
                                    IconButton(
                                        onClick = {
                                            if (!isUserSignedIn) {
                                                viewModel.triggerLoginPrompt()
                                            } else if (canSendComment) {
                                                val encoded = commentComposerState.exportEncodedText()
                                                if (encoded.isNotBlank()) {
                                                    val newComment = localDb.addLocalComment(
                                                        episodeId = uiState.episodeId,
                                                        text = encoded,
                                                        customUserName = currentUserName,
                                                        customAvatarUrl = currentUserAvatar,
                                                        customBorderUrl = currentUserBorderUrl
                                                    )
                                                    viewModel.addLocalComment(newComment)
                                                    commentComposerState.clear()
                                                    showCommentStickerPanel = false
                                                    keyboardController?.hide()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Kirim komentar",
                                            tint = if (canSendComment) Color.White else Color(0xFF6B7280),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Panel Stiker 2 Mode (Inline Emoji-Sticker & Stiker Besar) dari Global Chat
                            AnimatedVisibility(
                                visible = showCommentStickerPanel && isUserSignedIn,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                ChatStickerPanel(
                                    onInsertInlineSticker = { sticker ->
                                        commentComposerState.insertInlineSticker(sticker.url)
                                    },
                                    onSendLargeSticker = { sticker ->
                                        if (sticker.url.isNotBlank()) {
                                            val newComment = localDb.addLocalComment(
                                                episodeId = uiState.episodeId,
                                                text = "[sticker:${sticker.url}]",
                                                customUserName = currentUserName,
                                                customAvatarUrl = currentUserAvatar,
                                                customBorderUrl = currentUserBorderUrl
                                            )
                                            viewModel.addLocalComment(newComment)
                                            showCommentStickerPanel = false
                                        }
                                    },
                                    onBackspace = { commentComposerState.backspace() },
                                    accentColor = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Server Picker Overlay Panel
        if (uiState.isServerPickerOpen) {
            ServerPickerOverlay(
                servers = uiState.servers,
                selectedServer = uiState.selectedServer,
                onServerSelect = { viewModel.selectServer(it) },
                onClose = { viewModel.toggleServerPicker() }
            )
        }

        // Love It Rating Dialog
        if (uiState.isLoveItDialogOpen) {
            LoveItRatingDialog(
                currentRating = uiState.userRating,
                onRatingSelected = { viewModel.setUserRating(it) },
                onDismiss = { viewModel.toggleLoveItDialog() }
            )
        }

        // Login Required Alert Dialog
        if (uiState.showLoginPrompt) {
            LoginRequiredDialog(
                onDismiss = { viewModel.dismissLoginPrompt() },
                onSimulateLogin = {
                    localDb.setLoggedIn(true, "Member Yuranime")
                    viewModel.dismissLoginPrompt()
                    Toast.makeText(context, "Berhasil masuk untuk berkomentar", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Full Screen Image Preview
        uiState.fullScreenPreviewUrl?.let { url ->
            Dialog(
                onDismissRequest = { viewModel.closeFullScreenImage() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.closeFullScreenImage() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(url)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Spotlight Coachmark Onboarding for StreamingScreen
        var showStreamingOnboarding by remember {
            mutableStateOf(!com.yuranime.dinz.data.local.AppPreferences.isStreamingOnboardingCompleted(context))
        }
        val streamingSteps = remember {
            listOf(
                CoachmarkStep(
                    id = "stream_player_settings",
                    title = "Pengaturan Pemutar Video",
                    description = "Sentuh ikon gerigi untuk mengganti resolusi video (360p s/d 1080p), kecepatan pemutaran, server cadangan, atau auto-next.",
                    cornerRadiusDp = 12f
                ),
                CoachmarkStep(
                    id = "stream_episodes",
                    title = "Daftar Episode Tayang",
                    description = "Pindah ke episode berikutnya atau episode sebelumnya dengan cepat tanpa harus keluar dari layar pemutar video.",
                    cornerRadiusDp = 14f
                ),
                CoachmarkStep(
                    id = "stream_comments",
                    title = "Ruang Komentar & Reaksi",
                    description = "Tulis komentar, berikan reaksi, dan diskusikan jalan cerita episode ini bersama sesama pecinta anime di seluruh Indonesia.",
                    cornerRadiusDp = 14f
                )
            )
        }

        SpotlightCoachmarkOverlay(
            steps = streamingSteps,
            isVisible = showStreamingOnboarding && !uiState.isFullScreen,
            targetBounds = targetBounds,
            onDismiss = {
                showStreamingOnboarding = false
                com.yuranime.dinz.data.local.AppPreferences.setStreamingOnboardingCompleted(context, true)
            }
        )
    }
}

// ==================== 1. CUSTOM EXOPLAYER VIEW ====================

@OptIn(UnstableApi::class)
@Composable
private fun CustomExoPlayerView(
    server: ServerItem?,
    uiState: StreamingUiState,
    viewModel: StreamingViewModel,
    onBackClick: () -> Unit,
    onEpisodeChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var showControls by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableLongStateOf(0L) }
    val seekPreviewManager = remember { SeekPreviewManager(maxFrames = 18) }
    var previewFrameBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingPreviewFrame by remember { mutableStateOf(false) }
    var controlsRowWidthPx by remember { mutableIntStateOf(0) }
    var seekbarStartXPx by remember { mutableFloatStateOf(0f) }
    var seekbarWidthPx by remember { mutableFloatStateOf(1f) }
    var autoSkippedIntroForEp by remember { mutableStateOf<String?>(null) }
    var playerBoxWidthPx by remember { mutableIntStateOf(0) }
    var doubleTapSide by remember { mutableIntStateOf(0) } // -1 = kiri (mundur), 1 = kanan (maju)
    var doubleTapCumulativeSec by remember { mutableIntStateOf(0) }
    var doubleTapEventToken by remember { mutableIntStateOf(0) }
    var pendingSeekBasePosMs by remember { mutableLongStateOf(-1L) }
    var pendingSeekTargetPosMs by remember { mutableLongStateOf(-1L) }
    val gestureScope = rememberCoroutineScope()
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastSingleTapUptimeMs by remember { mutableLongStateOf(0L) }
    var lastSingleTapSide by remember { mutableIntStateOf(0) }
    var lastSkipTapUptimeMs by remember { mutableLongStateOf(0L) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestOnEpisodeChange by rememberUpdatedState(onEpisodeChange)
    val latestPlayerBoxWidthPx by rememberUpdatedState(playerBoxWidthPx)

    val exoPlayer = remember(context) {
        StreamingPlayerHolder.getOrCreatePlayer(context)
    }

    LaunchedEffect(doubleTapEventToken) {
        if (doubleTapEventToken > 0) {
            delay(270L)
            val targetPos = pendingSeekTargetPosMs
            if (targetPos >= 0L) {
                exoPlayer.seekTo(targetPos)
                val dur = exoPlayer.duration
                viewModel.updatePosition(targetPos, dur, exoPlayer.bufferedPosition)
                StreamingPlayerHolder.updatePlaybackProgress(targetPos, dur, exoPlayer.isPlaying)
                pendingSeekTargetPosMs = -1L
                pendingSeekBasePosMs = -1L
            }
            delay(500L)
            doubleTapSide = 0
            doubleTapCumulativeSec = 0
        }
    }

    // Gunakan sesi ExoPlayer bersama dari StreamingPlayerHolder agar mini-player & PiP berjalan tanpa mengulang video
    val isInSystemPip by StreamingPlayerHolder.isInSystemPip.collectAsStateWithLifecycle()

    // Bind playback listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
                StreamingPlayerHolder.updatePlaybackProgress(
                    exoPlayer.currentPosition,
                    exoPlayer.duration,
                    isPlaying
                )
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val activeState = latestUiState
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    val pos = exoPlayer.currentPosition
                    viewModel.updatePosition(pos, duration, exoPlayer.bufferedPosition)
                    StreamingPlayerHolder.updatePlaybackProgress(pos, duration, exoPlayer.isPlaying)
                } else if (playbackState == Player.STATE_ENDED) {
                    val mIdEnded = activeState.movieId.ifBlank { activeState.episode?.idMovie ?: "" }
                    if (mIdEnded.isNotBlank() && activeState.episodeId.isNotBlank()) {
                        val endDur = exoPlayer.duration.takeIf { it > 0L } ?: activeState.durationMs
                        val epNum = activeState.episode?.index?.ifBlank { "1" } ?: "1"
                        val horizontalCoverUrl = activeState.movieDetail?.movie?.getEffectiveCover()?.takeIf { it.isNotBlank() }
                            ?: activeState.episode?.getEffectiveImage()?.takeIf { it.isNotBlank() }
                            ?: activeState.movieDetail?.movie?.getEffectivePoster()
                            ?: ""
                        val savedEnd = LocalAuthDatabase(context).saveWatchProgress(
                            movieId = mIdEnded,
                            animeTitle = activeState.movieDetail?.movie?.title ?: activeState.episode?.title?.substringBefore(" Episode") ?: "Anime",
                            episodeId = activeState.episodeId,
                            episodeTitle = "Episode $epNum",
                            posterUrl = horizontalCoverUrl,
                            positionMs = endDur,
                            durationMs = endDur,
                            episodeNumber = epNum,
                            isCompleted = true
                        )
                        if (savedEnd != null) {
                            FirebaseManager.notifyWatchHistoryChanged()
                            FirebaseManager.syncWatchHistoryItem(context, savedEnd)
                        }
                    }
                    if (activeState.autoNextEpisode) {
                        val nextEp = activeState.episodeNext ?: run {
                            val currentIdx = activeState.episode?.index?.toIntOrNull()
                            if (currentIdx != null) {
                                activeState.episodes.firstOrNull { it.index == (currentIdx + 1).toString() }
                            } else null
                        }
                        val nextId = nextEp?.id
                        if (!nextId.isNullOrBlank()) {
                            exoPlayer.stop()
                            exoPlayer.clearMediaItems()
                            viewModel.selectEpisode(nextId, nextEp.index)
                            latestOnEpisodeChange?.invoke(nextId)
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Memisahkan Logika "Ganti Episode" vs "Ganti Resolusi" vs "Kembali dari Mini-Player/PiP"
    var lastLoadedEpisodeId by remember { mutableStateOf<String?>(null) }
    val currentEpisodeId = uiState.episodeId
    val currentStreamUrl = remember(server?.link) {
        val raw = server?.link?.trim().orEmpty()
        if (raw.startsWith("http://", ignoreCase = true)) {
            "https://" + raw.removePrefix("http://")
        } else {
            raw
        }
    }
    val currentAnimeTitle = uiState.movieDetail?.movie?.title
        ?: uiState.episode?.title?.substringBefore(" Episode")
        ?: ""
    val currentEpNum = uiState.episode?.index ?: "1"
    val currentThumbUrl = uiState.movieDetail?.movie?.getEffectiveCover()?.ifBlank { null }
        ?: uiState.episode?.getEffectiveImage()?.ifBlank { null }
        ?: uiState.movieDetail?.movie?.getEffectivePoster()
        ?: ""

    LaunchedEffect(currentEpisodeId, currentStreamUrl, currentAnimeTitle, currentEpNum, currentThumbUrl) {
        if (currentStreamUrl.isNotBlank()) {
            StreamingPlayerHolder.updateActiveMedia(
                episodeId = currentEpisodeId,
                movieId = uiState.movieId.ifBlank { uiState.episode?.idMovie ?: "" },
                streamUrl = currentStreamUrl,
                animeTitle = currentAnimeTitle,
                episodeNumber = currentEpNum,
                thumbnailUrl = currentThumbUrl
            )
        }
    }

    LaunchedEffect(currentEpisodeId, currentStreamUrl) {
        if (currentStreamUrl.isNotBlank()) {
            if (StreamingPlayerHolder.isAlreadyLoaded(currentEpisodeId, currentStreamUrl)) {
                // Melanjutkan sesi pemutaran yang sudah berjalan dari Mini-Player / PiP tanpa mengulang dari awal
                lastLoadedEpisodeId = currentEpisodeId
                viewModel.setPlaying(exoPlayer.isPlaying)
                viewModel.updatePosition(exoPlayer.currentPosition, exoPlayer.duration, exoPlayer.bufferedPosition)
                return@LaunchedEffect
            }

            val isResolutionChangeOnly = (lastLoadedEpisodeId != null && lastLoadedEpisodeId == currentEpisodeId)
            val savedPosition = if (isResolutionChangeOnly) {
                exoPlayer.currentPosition
            } else {
                val mIdForResume = uiState.movieId.ifBlank { uiState.episode?.idMovie ?: "" }
                if (mIdForResume.isNotBlank() && currentEpisodeId.isNotBlank()) {
                    LocalAuthDatabase(context).getEpisodeResumePositionMs(mIdForResume, currentEpisodeId)
                } else {
                    0L
                }
            }

            StreamingPlayerHolder.updateActiveMedia(
                episodeId = currentEpisodeId,
                movieId = uiState.movieId.ifBlank { uiState.episode?.idMovie ?: "" },
                streamUrl = currentStreamUrl,
                animeTitle = currentAnimeTitle,
                episodeNumber = currentEpNum,
                thumbnailUrl = currentThumbUrl
            )

            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(currentStreamUrl))
            exoPlayer.prepare()
            exoPlayer.seekTo(savedPosition.coerceAtLeast(0L))
            exoPlayer.playWhenReady = if (isResolutionChangeOnly) exoPlayer.playWhenReady else uiState.autoPlayEnabled

            lastLoadedEpisodeId = currentEpisodeId
        } else {
            if (lastLoadedEpisodeId != null && lastLoadedEpisodeId != currentEpisodeId &&
                !StreamingPlayerHolder.isAlreadyLoaded(currentEpisodeId, currentStreamUrl)
            ) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                viewModel.updatePosition(0L, 0L)
            }
        }
    }

    // Auto-clear cache frame preview RAM & state skip setiap ganti episode atau keluar dari player
    LaunchedEffect(currentEpisodeId) {
        previewFrameBitmap = null
        isLoadingPreviewFrame = false
        seekPreviewManager.clearAll()
        pendingSingleTapJob?.cancel()
        pendingSeekBasePosMs = -1L
        pendingSeekTargetPosMs = -1L
        doubleTapSide = 0
        doubleTapCumulativeSec = 0
        lastSingleTapUptimeMs = 0L
        lastSingleTapSide = 0
        lastSkipTapUptimeMs = 0L
    }

    DisposableEffect(seekPreviewManager) {
        onDispose {
            previewFrameBitmap = null
            seekPreviewManager.clearAll()
        }
    }

    val seekBucketKey = if (isSeeking) seekPreviewManager.bucketKeyFor(sliderPosition) else -1L
    LaunchedEffect(isSeeking, seekBucketKey, currentStreamUrl, currentEpisodeId) {
        if (!isSeeking || currentStreamUrl.isBlank()) {
            isLoadingPreviewFrame = false
            return@LaunchedEffect
        }
        val cached = seekPreviewManager.getCachedFrame(sliderPosition)
        if (cached != null) {
            previewFrameBitmap = cached
            isLoadingPreviewFrame = false
            return@LaunchedEffect
        }
        isLoadingPreviewFrame = true
        delay(85L)
        val extracted = seekPreviewManager.getOrExtractFrame(
            streamUrl = currentStreamUrl,
            episodeId = currentEpisodeId,
            positionMs = sliderPosition
        )
        if (isSeeking) {
            if (extracted != null) {
                previewFrameBitmap = extracted
            }
            isLoadingPreviewFrame = false
        }
    }

    // Sync speed changes
    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(uiState.playbackSpeed)
    }

    // Periodic time tracking (real-time 250ms untuk seekbar, buffer, & tombol Skip Intro / Skip Outro akurat)
    LaunchedEffect(uiState.isPlaying) {
        while (true) {
            if (!isSeeking && pendingSeekTargetPosMs < 0L) {
                viewModel.updatePosition(
                    positionMs = exoPlayer.currentPosition,
                    durationMs = exoPlayer.duration,
                    bufferedMs = exoPlayer.bufferedPosition
                )
            }
            delay(250)
        }
    }

    // Auto-skip Intro HANYA jika timestamp API terverifikasi tersedia
    LaunchedEffect(
        uiState.episodeId,
        uiState.introSegment,
        uiState.autoSkipIntro,
        uiState.currentPositionMs,
        uiState.isPlaying
    ) {
        val epIdNow = uiState.episodeId
        val intro = uiState.introSegment ?: return@LaunchedEffect
        if (!uiState.autoSkipIntro || autoSkippedIntroForEp == epIdNow || !uiState.isPlaying) {
            return@LaunchedEffect
        }
        val pos = uiState.currentPositionMs
        val dur = uiState.durationMs
        if (dur > 30_000L && intro.startMs >= dur - 4_000L) return@LaunchedEffect
        if (intro.containsPosition(pos)) {
            autoSkippedIntroForEp = epIdNow
            val targetMs = if (dur > 2_000L) {
                intro.endMs.coerceAtMost((dur - 1_000L).coerceAtLeast(intro.startMs))
            } else {
                intro.endMs
            }
            if (targetMs > pos + 1_000L) {
                exoPlayer.seekTo(targetMs)
                viewModel.updatePosition(targetMs, exoPlayer.duration)
                Toast.makeText(
                    context,
                    "Intro dilewati otomatis (${intro.source})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Auto hide controls after 4 seconds (jeda saat user sedang menggeser seekbar)
    LaunchedEffect(showControls, isSeeking) {
        if (showControls && !uiState.isLocked && !isSeeking) {
            delay(4000)
            showControls = false
        }
    }

    val triggerInstantSkip: (Int) -> Unit = { tapSide ->
        val activeState = latestUiState
        val stepSec = activeState.doubleTapSeekSeconds.coerceIn(5, 30)
        val rawDur = exoPlayer.duration.takeIf { it > 0L } ?: activeState.durationMs
        val maxPos = if (rawDur > 0L) rawDur else Long.MAX_VALUE

        val nextSec = if (doubleTapSide == tapSide && doubleTapCumulativeSec > 0) {
            doubleTapCumulativeSec + stepSec
        } else {
            pendingSeekBasePosMs = if (pendingSeekTargetPosMs >= 0L) {
                pendingSeekTargetPosMs
            } else {
                exoPlayer.currentPosition
            }
            stepSec
        }
        val basePos = if (pendingSeekBasePosMs >= 0L) pendingSeekBasePosMs else exoPlayer.currentPosition
        val targetPos = (basePos + (tapSide * nextSec * 1000L)).coerceIn(0L, maxPos)

        pendingSeekBasePosMs = basePos
        pendingSeekTargetPosMs = targetPos
        doubleTapSide = tapSide
        doubleTapCumulativeSec = nextSec
        doubleTapEventToken++

        // Perbarui UI seekbar & waktu secara instan sementara seekTo() didebounce 270ms
        viewModel.updatePosition(targetPos, exoPlayer.duration, exoPlayer.bufferedPosition)
        StreamingPlayerHolder.updatePlaybackProgress(targetPos, exoPlayer.duration, exoPlayer.isPlaying)
    }
    val latestTriggerInstantSkip by rememberUpdatedState(triggerInstantSkip)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { playerBoxWidthPx = it.width }
            .pointerInput(exoPlayer, currentEpisodeId) {
                val touchSlopPx = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    if (up.isConsumed) return@awaitEachGesture
                    if ((up.position - down.position).getDistance() > touchSlopPx) {
                        return@awaitEachGesture
                    }
                    up.consume()

                    val now = SystemClock.uptimeMillis()
                    val width = latestPlayerBoxWidthPx
                    val activeState = latestUiState
                    val tapSide = if (width > 0 && up.position.x < width * 0.5f) -1 else 1

                    if (!activeState.isLocked && activeState.doubleTapSeekEnabled && width > 0) {
                        val inActiveSkipChain = doubleTapSide == tapSide && (now - lastSkipTapUptimeMs) <= 750L
                        val isSecondTapOfDouble = lastSingleTapSide == tapSide && (now - lastSingleTapUptimeMs) <= 320L

                        when {
                            inActiveSkipChain -> {
                                // Ketukan ke-3, ke-4, dst: akumulasi instan tanpa tunggu apa pun
                                pendingSingleTapJob?.cancel()
                                lastSkipTapUptimeMs = now
                                latestTriggerInstantSkip(tapSide)
                            }
                            isSecondTapOfDouble -> {
                                // Ketukan ke-2 cepat: batalkan toggle kontrol & langsung skip
                                pendingSingleTapJob?.cancel()
                                lastSingleTapUptimeMs = 0L
                                lastSingleTapSide = 0
                                lastSkipTapUptimeMs = now
                                latestTriggerInstantSkip(tapSide)
                            }
                            else -> {
                                // Ketukan pertama: tunggu singkat 210ms untuk mendeteksi double-tap sebelum toggle kontrol
                                lastSingleTapUptimeMs = now
                                lastSingleTapSide = tapSide
                                pendingSingleTapJob?.cancel()
                                pendingSingleTapJob = gestureScope.launch {
                                    delay(210L)
                                    if (!latestUiState.isLocked) {
                                        showControls = !showControls
                                        if (!showControls && latestUiState.isSettingsOpen) {
                                            viewModel.toggleSettings()
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!activeState.isLocked) {
                        showControls = !showControls
                        if (!showControls && activeState.isSettingsOpen) {
                            viewModel.toggleSettings()
                        }
                    }
                }
            }
    ) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { pv ->
                if (isInSystemPip) {
                    if (pv.player != null) pv.player = null
                } else {
                    if (pv.player !== exoPlayer) {
                        pv.player = exoPlayer
                    }
                }
            },
            onRelease = { pv ->
                pv.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Animasi Indikator Double-Tap Kiri (Mundur) & Kanan (Maju)
        DoubleTapSeekOverlay(
            side = -1,
            visible = doubleTapSide == -1,
            seconds = doubleTapCumulativeSec,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 26.dp)
        )
        DoubleTapSeekOverlay(
            side = 1,
            visible = doubleTapSide == 1,
            seconds = doubleTapCumulativeSec,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 26.dp)
        )

        // Lock button floating when screen is locked in fullscreen
        if (uiState.isLocked && uiState.isFullScreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(24.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC181A22))
                    .clickable { viewModel.toggleLock() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Kunci Layar",
                    tint = ColorStreamAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Custom Player Controls Overlay
        AnimatedVisibility(
            visible = showControls && !uiState.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99000000),
                                Color.Transparent,
                                Color(0xCC000000)
                            )
                        )
                    )
            ) {
                // Top Control Bar in Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (uiState.isFullScreen) {
                                Modifier.padding(start = 42.dp, end = 42.dp, top = 16.dp)
                            } else {
                                Modifier
                                    .statusBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Settings Gear Button (Clean white icon, no glassmorphism box)
                    IconButton(
                        onClick = { viewModel.toggleSettings() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center Rewind, Clean White Play/Pause, Forward (tanpa lingkaran gelap/transparan di belakang tombol)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                            viewModel.updatePosition(newPos, exoPlayer.duration)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Mundur 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Center Play / Pause Icon Langsung di Atas Video (Putih Bersih)
                    if (uiState.isLoading || server == null) {
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration.coerceAtLeast(0))
                            exoPlayer.seekTo(newPos)
                            viewModel.updatePosition(newPos, exoPlayer.duration)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Maju 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Bottom Control Bar (Preview Saat Geser + Progress + Time + Fullscreen)
                val totalDuration = if (uiState.durationMs > 0) uiState.durationMs else 1L
                val currentPos = if (isSeeking) sliderPosition else uiState.currentPositionMs
                val sliderFraction = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                val bufferedFraction = (uiState.bufferedPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .then(
                            if (uiState.isFullScreen) {
                                Modifier.padding(start = 42.dp, end = 42.dp, bottom = 14.dp)
                            } else {
                                Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            }
                        )
                ) {
                    // Kotak Preview Frame Video + Waktu Melayang di Atas Posisi Jari saat Seekbar Digeser
                    if (isSeeking && controlsRowWidthPx > 0) {
                        val previewWidthDp = 128.dp
                        val previewHeightDp = 72.dp
                        val previewWidthPx = with(density) { previewWidthDp.toPx() }
                        val fingerXPx = seekbarStartXPx + (sliderFraction * seekbarWidthPx)
                        val clampedOffsetX = (fingerXPx - previewWidthPx * 0.5f)
                            .coerceIn(0f, (controlsRowWidthPx - previewWidthPx).coerceAtLeast(0f))
                            .roundToInt()
                        val fallbackThumbUrl = uiState.episode?.getEffectiveImage()?.ifBlank { null }
                            ?: uiState.movieDetail?.movie?.getEffectiveCover()?.ifBlank { null }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(clampedOffsetX, 0) }
                                    .width(previewWidthDp)
                                    .height(previewHeightDp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0E1015))
                                    .border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(8.dp))
                            ) {
                                val frame = previewFrameBitmap
                                if (frame != null) {
                                    Image(
                                        bitmap = frame,
                                        contentDescription = "Preview ${formatTime(sliderPosition)}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (!fallbackThumbUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(fallbackThumbUrl)
                                            .size(160, 90)
                                            .crossfade(false)
                                            .build(),
                                        contentDescription = "Preview ${formatTime(sliderPosition)}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xCC000000))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatTime(sliderPosition),
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isLoadingPreviewFrame && frame == null) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 1.5.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Time and Progress Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { controlsRowWidthPx = it.width },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Time
                        Text(
                            text = formatTime(if (isSeeking) sliderPosition else uiState.currentPositionMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        ModernPlayerSeekbar(
                            progressFraction = sliderFraction,
                            bufferedFraction = bufferedFraction,
                            totalDurationMs = totalDuration,
                            introSegment = uiState.introSegment,
                            outroSegment = uiState.outroSegment,
                            isDragging = isSeeking,
                            onSeekChange = { frac ->
                                isSeeking = true
                                val nextPos = (frac * totalDuration).toLong()
                                sliderPosition = nextPos
                                seekPreviewManager.getCachedFrame(nextPos)?.let { cached ->
                                    previewFrameBitmap = cached
                                }
                            },
                            onSeekCommit = { frac ->
                                val targetMs = (frac * totalDuration).toLong()
                                sliderPosition = targetMs
                                exoPlayer.seekTo(targetMs)
                                viewModel.updatePosition(targetMs, exoPlayer.duration, exoPlayer.bufferedPosition)
                                isSeeking = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    seekbarStartXPx = coords.positionInParent().x
                                    seekbarWidthPx = coords.size.width.toFloat().coerceAtLeast(1f)
                                }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Duration Time
                        Text(
                            text = formatTime(uiState.durationMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Full Screen Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleFullScreen() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Full Screen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Skip Intro / Skip Outro Floating Pill (murni dari data API AniSkip/TheIntroDB/OAT)
        val introSeg = uiState.introSegment
        val outroSeg = uiState.outroSegment
        val activeSkipSegment: SkipSegment? = when {
            uiState.isLocked -> null
            introSeg != null &&
                !(uiState.durationMs > 30_000L && introSeg.startMs >= uiState.durationMs - 4_000L) &&
                introSeg.containsPosition(uiState.currentPositionMs) -> introSeg
            outroSeg != null &&
                !(uiState.durationMs > 30_000L && outroSeg.startMs >= uiState.durationMs - 4_000L) &&
                outroSeg.containsPosition(uiState.currentPositionMs) -> outroSeg
            else -> null
        }
        AnimatedVisibility(
            visible = activeSkipSegment != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(
                    if (uiState.isFullScreen) {
                        Modifier.padding(end = 36.dp, bottom = 58.dp)
                    } else {
                        Modifier.padding(end = 14.dp, bottom = 54.dp)
                    }
                )
        ) {
            val seg = activeSkipSegment ?: return@AnimatedVisibility
            val buttonLabel = if (seg.type == SkipSegmentType.INTRO) "Skip Intro" else "Skip Outro"
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD911131A))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                    .clickable {
                        val maxSeek = if (uiState.durationMs > 2_000L) {
                            (uiState.durationMs - 1_000L).coerceAtLeast(seg.startMs)
                        } else {
                            seg.endMs
                        }
                        val targetMs = seg.endMs.coerceAtMost(maxSeek)
                        if (seg.type == SkipSegmentType.INTRO) {
                            autoSkippedIntroForEp = uiState.episodeId
                        }
                        exoPlayer.seekTo(targetMs)
                        viewModel.updatePosition(targetMs, exoPlayer.duration)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = buttonLabel,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buttonLabel,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Floating In-Place Settings Menu (PiuPiu Style)
        if (uiState.isSettingsOpen && !uiState.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.toggleSettings() }
            )

            FloatingSettingsMenu(
                uiState = uiState,
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .then(
                        if (uiState.isFullScreen) {
                            Modifier.padding(top = 56.dp, end = 42.dp)
                        } else {
                            Modifier
                                .statusBarsPadding()
                                .padding(top = 48.dp, end = 12.dp)
                        }
                    )
            )
        }
    }
}

// ==================== 2. ACTION BUTTONS ====================

@Composable
private fun ActionButtonItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161922))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ColorStreamAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                color = ColorTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== 3. SERVER PICKER OVERLAY ====================

@Composable
private fun ServerPickerOverlay(
    servers: List<ServerItem>,
    selectedServer: ServerItem?,
    onServerSelect: (ServerItem) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF012141C))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pilih Server",
                    color = ColorTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers) { srv ->
                    val isSelected = srv.id == selectedServer?.id
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(145.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) ColorStreamAccent else Color(0x33FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color(0xFF1E222D))
                            .clickable { onServerSelect(srv) }
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Uploader Name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ColorTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = srv.username ?: "Shizukana",
                                    color = ColorTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Play Button Indicator
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, if (isSelected) ColorStreamAccent else Color(0x88FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isSelected) ColorStreamAccent else Color(0xCCFFFFFF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Server Name & Resolution
                            Column {
                                Text(
                                    text = srv.name ?: "RAPSODI",
                                    color = ColorTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${srv.quality ?: "720p"} ${srv.type ?: "direct"}",
                                    color = if (isSelected) ColorStreamAccent else Color(0xFFD1D5DB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 4. LOVE IT RATING DIALOG ====================

@Composable
private fun LoveItRatingDialog(
    currentRating: Int,
    onRatingSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181A22))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Suka dengan episode ini?",
                    color = ColorTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Rating teman akan sangat membantu kami maaci!",
                    color = ColorTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 5 Star Rating Row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { onRatingSelected(i) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "$i Bintang",
                                tint = if (i <= currentRating) ColorMetricFavorites else ColorTextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Kuy bantu Yuranime dapatkan bintang terbaik agar lebih semangat lagi hehe",
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Submit Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorStreamAccent)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kirim Rating",
                        color = Color(0xFF0B0B0B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== 5. FLOATING SETTINGS MENU (PIUPIU STYLE) ====================

@Composable
private fun DoubleTapSeekOverlay(
    side: Int,
    visible: Boolean,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    val tapPulseScale = remember { Animatable(1f) }
    LaunchedEffect(seconds) {
        if (seconds > 0) {
            tapPulseScale.snapTo(1.16f)
            tapPulseScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 130)
            )
        }
    }

    AnimatedVisibility(
        visible = visible && seconds > 0,
        enter = fadeIn(tween(90)) + scaleIn(initialScale = 0.9f, animationSpec = tween(110)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.94f, animationSpec = tween(180)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val s = tapPulseScale.value
                    scaleX = s
                    scaleY = s
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (side < 0) Icons.Default.FastRewind else Icons.Default.FastForward,
                contentDescription = if (side < 0) "Mundur $seconds detik" else "Maju $seconds detik",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (side < 0) "-${seconds}s" else "+${seconds}s",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModernPlayerSeekbar(
    progressFraction: Float,
    bufferedFraction: Float,
    totalDurationMs: Long,
    introSegment: SkipSegment?,
    outroSegment: SkipSegment?,
    isDragging: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekCommit: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 3.dp,
        animationSpec = tween(140),
        label = "seekbarTrackHeight"
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 8.5.dp else 5.5.dp,
        animationSpec = tween(140),
        label = "seekbarThumbRadius"
    )
    var barWidthPx by remember { mutableIntStateOf(1) }
    var lastDragFrac by remember { mutableStateOf(progressFraction) }
    val latestOnSeekChange by rememberUpdatedState(onSeekChange)
    val latestOnSeekCommit by rememberUpdatedState(onSeekCommit)

    Box(
        modifier = modifier
            .height(30.dp)
            .onSizeChanged { barWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                    latestOnSeekChange(frac)
                    latestOnSeekCommit(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                        lastDragFrac = frac
                        latestOnSeekChange(frac)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / barWidthPx.toFloat()).coerceIn(0f, 1f)
                        lastDragFrac = frac
                        latestOnSeekChange(frac)
                    },
                    onDragEnd = {
                        latestOnSeekCommit(lastDragFrac)
                    },
                    onDragCancel = {
                        latestOnSeekCommit(lastDragFrac)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val th = trackHeight.toPx()
            val topY = (h - th) * 0.5f
            val corner = CornerRadius(th * 0.5f, th * 0.5f)

            // 1. Track dasar transparan
            drawRoundRect(
                color = Color(0x45FFFFFF),
                topLeft = Offset(0f, topY),
                size = Size(w, th),
                cornerRadius = corner
            )

            // 2. Buffer progress abu-abu transparan
            val bufW = (w * bufferedFraction.coerceIn(0f, 1f))
            if (bufW > 0f) {
                drawRoundRect(
                    color = Color(0x78CBD5E1),
                    topLeft = Offset(0f, topY),
                    size = Size(bufW, th),
                    cornerRadius = corner
                )
            }

            // 3. Progres yang sudah berjalan (Putih Bersih #FFFFFF)
            val progW = (w * progressFraction.coerceIn(0f, 1f))
            if (progW > 0f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, topY),
                    size = Size(progW, th),
                    cornerRadius = corner
                )
            }

            // 4. Penanda warna kuning/amber kontras untuk segmen Intro & Outro (langsung tampil dari awal)
            if (totalDurationMs > 1000L) {
                val markerColor = Color(0xFFFACC15)
                val minSegPx = 4.dp.toPx()
                listOfNotNull(introSegment, outroSegment).forEach { seg ->
                    if (seg.endMs > seg.startMs) {
                        val startFrac = (seg.startMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        val endFrac = (seg.endMs.toFloat() / totalDurationMs.toFloat()).coerceIn(startFrac, 1f)
                        val startX = startFrac * w
                        val segW = maxOf((endFrac - startFrac) * w, minSegPx).coerceAtMost((w - startX).coerceAtLeast(0f))
                        if (segW > 0.5f) {
                            drawRoundRect(
                                color = markerColor,
                                topLeft = Offset(startX, topY - 0.5.dp.toPx()),
                                size = Size(segW, th + 1.dp.toPx()),
                                cornerRadius = corner
                            )
                        }
                    }
                }
            }

            // 5. Thumb lingkaran kecil presisi berwarna putih bersih (#FFFFFF)
            val r = thumbRadius.toPx()
            val cx = progW.coerceIn(r, (w - r).coerceAtLeast(r))
            val cy = h * 0.5f
            if (isDragging) {
                drawCircle(
                    color = Color(0x40FFFFFF),
                    radius = r * 1.7f,
                    center = Offset(cx, cy)
                )
            }
            drawCircle(
                color = Color.White,
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun FloatingSettingsMenu(
    uiState: StreamingUiState,
    viewModel: StreamingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .widthIn(min = 225.dp, max = 248.dp)
            .heightIn(max = 275.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6141414))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume taps */ }
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        when (uiState.settingsSubMenu) {
            SettingsSubMenu.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Quality / Server
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.RESOLUTION) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Kualitas",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kualitas",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.selectedServer?.quality ?: "720p",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Pilih Kualitas",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    // Playback Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.SPEED) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Kecepatan",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kecepatan",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.playbackSpeed == 1.0f) "Normal" else "${uiState.playbackSpeed}x",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Pilih Kecepatan",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    // Double-Tap Skip Toggle (ON / OFF)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Double-Tap Skip",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = uiState.doubleTapSeekEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setDoubleTapSeekEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0B0B0B),
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color(0xFF9CA3AF),
                                uncheckedTrackColor = Color(0xFF2A2A2A)
                            )
                        )
                    }

                    // Durasi Skip Kustom (5, 10, 15, 30 detik)
                    if (uiState.doubleTapSeekEnabled) {
                        HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.DOUBLE_TAP_DURATION) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Durasi Skip",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.doubleTapSeekSeconds} dtk",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.5.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Pilih Durasi Skip",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    // Auto Next Episode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto Next",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = uiState.autoNextEpisode,
                            onCheckedChange = { viewModel.toggleAutoNext() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF0B0B0B),
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color(0xFF9CA3AF),
                                uncheckedTrackColor = Color(0xFF2A2A2A)
                            )
                        )
                    }

                    // Lock Screen (if Full Screen)
                    if (uiState.isFullScreen) {
                        HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kunci Layar",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = uiState.isLocked,
                                onCheckedChange = { viewModel.toggleLock() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0B0B0B),
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color(0xFF9CA3AF),
                                    uncheckedTrackColor = Color(0xFF2A2A2A)
                                )
                            )
                        }
                    }
                }
            }

            SettingsSubMenu.RESOLUTION -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Menu Utama",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kualitas Video",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    uiState.servers.forEach { server ->
                        val isSelected = server.id == uiState.selectedServer?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectServer(server)
                                    viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN)
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${server.quality ?: "720p"} • ${server.name ?: "Server"}",
                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            SettingsSubMenu.SPEED -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Menu Utama",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kecepatan Putar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                    speeds.forEach { speed ->
                        val isSelected = uiState.playbackSpeed == speed
                        val speedLabel = if (speed == 1.0f) "Normal" else "${speed}x"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN)
                                }
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = speedLabel,
                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            SettingsSubMenu.DOUBLE_TAP_DURATION -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Menu Utama",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Durasi Double-Tap Skip",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 0.5.dp)

                    val skipOptions = listOf(5, 10, 15, 30)
                    skipOptions.forEach { sec ->
                        val isSelected = uiState.doubleTapSeekSeconds == sec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDoubleTapSeekSeconds(context, sec)
                                    viewModel.setSettingsSubMenu(SettingsSubMenu.MAIN)
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$sec detik",
                                color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 6. COMMENT ITEM ROW & STICKER BODY ====================

@Composable
private fun CommentTextOrStickerBody(rawText: String) {
    val context = LocalContext.current
    val trimmed = rawText.trim()
    val stickerMatch = LARGE_COMMENT_STICKER_REGEX.find(trimmed)
    if (stickerMatch != null) {
        val rawUrl = stickerMatch.groupValues.getOrNull(1).orEmpty()
        var resolvedUrl by remember(rawUrl) { mutableStateOf(rawUrl) }
        val request = remember(context, resolvedUrl) {
            ImageRequest.Builder(context)
                .data(resolvedUrl)
                .size(220, 220)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .listener(
                    onError = { _, _ ->
                        if (resolvedUrl.contains("stickerly.pstatic.net", ignoreCase = true) &&
                            resolvedUrl.contains(".webp", ignoreCase = true)
                        ) {
                            resolvedUrl = resolvedUrl.replace(
                                Regex("""\.webp(\?.*)?$""", RegexOption.IGNORE_CASE)
                            ) { ".png${it.groupValues.getOrNull(1).orEmpty()}" }
                        }
                    }
                )
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = "Stiker komentar",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(88.dp)
        )
    } else if (rawText.contains("[s:")) {
        InlineStickerText(
            rawText = rawText,
            textColor = Color(0xFFE5E7EB),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            stickerSizeSp = 22.sp
        )
    } else {
        Text(
            text = rawText,
            color = Color(0xFFE5E7EB),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StaticCommentAvatarWithBorder(
    avatarUrl: String,
    userName: String,
    borderUrl: String,
    isCustomPhoto: Boolean,
    size: Dp = 34.dp,
    showPersonPlaceholder: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val frameSize = size * 1.24f
    val staticBorderUrl = remember(borderUrl) {
        if (borderUrl.isNotBlank()) {
            DiscordCosmeticsRepository.getOptimizedUrl(borderUrl, size = 128, animated = false)
        } else {
            ""
        }
    }

    Box(
        modifier = modifier.size(frameSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF1A1C22)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .decoderFactory(
                            if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory()
                            else GifDecoder.Factory()
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar ${userName.ifBlank { "Pengguna" }}",
                    contentScale = if (isCustomPhoto) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else if (!showPersonPlaceholder && userName.isNotBlank()) {
                Text(
                    text = userName.take(1).uppercase(Locale.ROOT),
                    color = Color.White,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }

        if (staticBorderUrl.isNotBlank()) {
            AnimatedCosmeticImage(
                url = staticBorderUrl,
                isAnimated = false,
                contentDescription = null,
                modifier = Modifier.size(frameSize)
            )
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: EpisodeCommentItem,
    onReplyClick: () -> Unit,
    onCopyText: (String) -> Unit = {},
    onDeleteLocal: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    val activeProfile by FirebaseManager.userProfile.collectAsState()
    val isUserSignedIn = localDb.isLoggedIn() || FirebaseManager.isUserLoggedIn()
    var isLiked by remember(comment.id) { mutableStateOf(localDb.getReaction(comment.id ?: "") == "like") }
    var isDisliked by remember(comment.id) { mutableStateOf(localDb.getReaction(comment.id ?: "") == "dislike") }
    var showMenu by remember { mutableStateOf(false) }

    val effectiveAvatar = comment.getEffectiveAvatarUrl()
    val hasCustomPhotoAvatar = !comment.avatarUrl.isNullOrBlank()
    val effectiveBorderUrl = comment.borderUrl?.takeIf { it.isNotBlank() }
        ?: if (comment.isLocal) activeProfile?.equippedBorderUrl.orEmpty() else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar bulat di kiri + Avatar Decoration statis (jika terpasang)
        StaticCommentAvatarWithBorder(
            avatarUrl = effectiveAvatar,
            userName = comment.userName ?: "Pengguna",
            borderUrl = effectiveBorderUrl,
            isCustomPhoto = hasCustomPhotoAvatar,
            size = 34.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Username (tebal putih) + titik pemisah + waktu relatif (abu-abu) + menu titik tiga di kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName ?: "Pengguna",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "•", color = Color(0xFF9CA3AF), fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = comment.time ?: "",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi komentar",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = Color(0xFF181A22)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Balas", color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onReplyClick()
                            }
                        )
                        val rawBody = comment.text.orEmpty()
                        if (rawBody.isNotBlank() && !LARGE_COMMENT_STICKER_REGEX.matches(rawBody.trim())) {
                            DropdownMenuItem(
                                text = { Text("Salin teks", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onCopyText(rawBody)
                                }
                            )
                        }
                        if (comment.isLocal && !comment.id.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("Hapus komentar", color = Color(0xFFF43F5E), fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onDeleteLocal(comment.id)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Teks komentar & stiker
            CommentTextOrStickerBody(rawText = comment.text ?: "")

            Spacer(modifier = Modifier.height(10.dp))

            // Baris aksi berurutan: Like + angka like, Dislike, dan ikon Chat/Reply + "Reply"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Like
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            if (!isUserSignedIn) {
                                onRequireLogin()
                            } else {
                                isLiked = !isLiked
                                if (isLiked) {
                                    isDisliked = false
                                    localDb.saveReaction(comment.id ?: "", "like")
                                } else {
                                    localDb.saveReaction(comment.id ?: "", "")
                                }
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val baseLikes = comment.like?.toIntOrNull() ?: 0
                    Text(
                        text = (baseLikes + if (isLiked) 1 else 0).toString(),
                        color = if (isLiked) Color.White else Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }

                // Dislike
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            if (!isUserSignedIn) {
                                onRequireLogin()
                            } else {
                                isDisliked = !isDisliked
                                if (isDisliked) {
                                    isLiked = false
                                    localDb.saveReaction(comment.id ?: "", "dislike")
                                } else {
                                    localDb.saveReaction(comment.id ?: "", "")
                                }
                            }
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Reply
                val replayCount = comment.replay?.toIntOrNull() ?: 0
                val replayText = if (replayCount > 0) "Reply ($replayCount)" else "Reply"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onReplyClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Reply",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.5.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = replayText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== 7. COMMENT REPLIES VIEW & REPLY ITEM ROW ====================

@Composable
private fun CommentRepliesView(
    parentComment: EpisodeCommentItem,
    replies: List<EpisodeCommentItem>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onRequireLogin: () -> Unit = {},
    onDeleteLocalReply: (String) -> Unit = {},
    onSendReply: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    val isUserSignedIn = localDb.isLoggedIn() || FirebaseManager.isUserLoggedIn()
    val replyComposerState = remember { InlineStickerComposerState() }
    val replyFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showReplyStickerPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Balasan",
                color = ColorTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color(0x22FFFFFF))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Parent Comment Highlight
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141720))
                ) {
                    CommentItemRow(
                        comment = parentComment,
                        onReplyClick = {},
                        onRequireLogin = onRequireLogin
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color(0x22FFFFFF))
            }

            if (isLoading && replies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorStreamAccent, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (replies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Belum ada balasan.", color = ColorTextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                items(replies, key = { it.id ?: it.hashCode().toString() }) { rep ->
                    CommentReplyItemRow(
                        reply = rep,
                        onRequireLogin = onRequireLogin,
                        onDeleteLocal = onDeleteLocalReply
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color(0x12FFFFFF),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Reply Input Bar + Sticker Support
        HorizontalDivider(thickness = 0.5.dp, color = Color(0x1AFFFFFF))
        val canSendReply = replyComposerState.hasSendableContent()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0B))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1A1C22))
                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    InlineStickerInputField(
                        state = replyComposerState,
                        placeholder = "Tulis balasan...",
                        cursorColor = Color.White,
                        placeholderColor = Color(0xFF8E929B),
                        modifier = Modifier.focusRequester(replyFocusRequester),
                        onFocused = { showReplyStickerPanel = false }
                    )
                }
                IconButton(
                    onClick = {
                        if (!isUserSignedIn) {
                            onRequireLogin()
                        } else if (showReplyStickerPanel) {
                            showReplyStickerPanel = false
                            replyFocusRequester.requestFocus()
                            keyboardController?.show()
                        } else {
                            keyboardController?.hide()
                            showReplyStickerPanel = true
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (showReplyStickerPanel) Icons.Rounded.Keyboard else Icons.Outlined.SentimentSatisfied,
                        contentDescription = "Stiker balasan",
                        tint = if (showReplyStickerPanel) Color.White else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (!isUserSignedIn) {
                            onRequireLogin()
                        } else if (canSendReply) {
                            val encoded = replyComposerState.exportEncodedText()
                            if (encoded.isNotBlank()) {
                                onSendReply(encoded)
                                replyComposerState.clear()
                                showReplyStickerPanel = false
                                keyboardController?.hide()
                            }
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim balasan",
                        tint = if (canSendReply) Color.White else Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showReplyStickerPanel && isUserSignedIn,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ChatStickerPanel(
                onInsertInlineSticker = { sticker ->
                    replyComposerState.insertInlineSticker(sticker.url)
                },
                onSendLargeSticker = { sticker ->
                    if (sticker.url.isNotBlank()) {
                        onSendReply("[sticker:${sticker.url}]")
                        showReplyStickerPanel = false
                    }
                },
                onBackspace = { replyComposerState.backspace() },
                accentColor = Color.White
            )
        }
    }
}

// Sub-Item Balasan
@Composable
private fun CommentReplyItemRow(
    reply: EpisodeCommentItem,
    onRequireLogin: () -> Unit = {},
    onDeleteLocal: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    val activeProfile by FirebaseManager.userProfile.collectAsState()
    val isUserSignedIn = localDb.isLoggedIn() || FirebaseManager.isUserLoggedIn()
    var isLiked by remember(reply.id) { mutableStateOf(localDb.getReaction(reply.id ?: "") == "like") }
    var isDisliked by remember(reply.id) { mutableStateOf(localDb.getReaction(reply.id ?: "") == "dislike") }
    val effectiveAvatar = reply.getEffectiveAvatarUrl()
    val hasCustomPhotoAvatar = !reply.avatarUrl.isNullOrBlank()
    val effectiveBorderUrl = reply.borderUrl?.takeIf { it.isNotBlank() }
        ?: if (reply.isLocal) activeProfile?.equippedBorderUrl.orEmpty() else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        StaticCommentAvatarWithBorder(
            avatarUrl = effectiveAvatar,
            userName = reply.userName ?: "Pengguna",
            borderUrl = effectiveBorderUrl,
            isCustomPhoto = hasCustomPhotoAvatar,
            size = 32.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reply.userName ?: "Pengguna",
                        color = ColorTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "•", color = ColorTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reply.time ?: "",
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }
                if (reply.isLocal && !reply.id.isNullOrBlank()) {
                    Text(
                        text = "Hapus",
                        color = Color(0xFFF43F5E),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onDeleteLocal(reply.id) }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            CommentTextOrStickerBody(rawText = reply.text ?: "")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!isUserSignedIn) onRequireLogin()
                        else {
                            isLiked = !isLiked
                            if (isLiked) {
                                isDisliked = false
                                localDb.saveReaction(reply.id ?: "", "like")
                            } else {
                                localDb.saveReaction(reply.id ?: "", "")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.White else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ((reply.like?.toIntOrNull() ?: 0) + if (isLiked) 1 else 0).toString(),
                        color = if (isLiked) Color.White else ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!isUserSignedIn) onRequireLogin()
                        else {
                            isDisliked = !isDisliked
                            if (isDisliked) {
                                isLiked = false
                                localDb.saveReaction(reply.id ?: "", "dislike")
                            } else {
                                localDb.saveReaction(reply.id ?: "", "")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) Color.White else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ==================== 7B. LOGIN REQUIRED DIALOG ====================

@Composable
private fun LoginRequiredDialog(
    onDismiss: () -> Unit,
    onSimulateLogin: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181A22))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Wajib Login Terlebih Dahulu",
                    color = ColorTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fitur ini memerlukan akun aktif. Silakan masuk terlebih dahulu untuk memberikan like, dislike, atau komentar.",
                    color = ColorTextSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262A36))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Tutup", color = ColorTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ColorStreamAccent)
                            .clickable { onSimulateLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Masuk Akun", color = Color(0xFF0B0B0B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== 8. FULL GALLERY SHEET ====================

@Composable
private fun FullGallerySheet(
    selectedTab: Int,
    posters: List<CommunityMediaItem>,
    covers: List<CommunityMediaItem>,
    onTabSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedTab == 0) "POSTER" else "COVER",
                color = ColorTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tab Selector: POSTER / COVER
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ColorBackgroundPrimary,
            contentColor = ColorStreamAccent,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(2.5.dp)
                        .background(ColorStreamAccent)
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = {
                    Text(
                        text = "POSTER",
                        color = if (selectedTab == 0) ColorStreamAccent else ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = {
                    Text(
                        text = "COVER",
                        color = if (selectedTab == 1) ColorStreamAccent else ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        // Gallery Items (Pure images, tap opens full screen)
        if (selectedTab == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posters) { item ->
                    val img = item.getEffectiveImage()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181A22))
                            .clickable { if (img.isNotBlank()) onImageClick(img) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(img.ifBlank { null })
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
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(covers) { item ->
                    val img = item.getEffectiveImage()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181A22))
                            .clickable { if (img.isNotBlank()) onImageClick(img) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(img.ifBlank { null })
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
    }
}

// Format duration helper
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatShortViews(raw: String?): String {
    if (raw.isNullOrBlank()) return "0"
    val trimmed = raw.trim()
    if (trimmed.endsWith("K", ignoreCase = true) || trimmed.endsWith("M", ignoreCase = true)) {
        return trimmed
    }
    val digitsOnly = trimmed.replace(".", "").replace(",", "").trim()
    val number = digitsOnly.toLongOrNull() ?: return trimmed
    return when {
        number >= 1_000_000L -> {
            val m = number / 1_000_000.0
            String.format(Locale.US, "%.1fM", m).replace(".0M", "M")
        }
        number >= 1_000L -> {
            val k = number / 1_000.0
            String.format(Locale.US, "%.1fK", k).replace(".0K", "K")
        }
        else -> number.toString()
    }
}

private fun formatAiredStartDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.trim()
    if (cleaned == "-" || cleaned.equals("null", ignoreCase = true) || cleaned.startsWith("0000") || cleaned.contains("?")) {
        return null
    }
    val parts = cleaned.split("-")
    if (parts.size == 3 && parts[0].length == 4) {
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull()
        val dayNum = parts[2].take(2).toIntOrNull()
        if (monthNum != null && monthNum in 1..12 && dayNum != null && dayNum in 1..31) {
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            return "$dayNum ${months[monthNum - 1]} $year"
        }
    }
    return cleaned
}
