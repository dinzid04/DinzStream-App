package com.dinz.animein.ui.streaming

import android.content.pm.ActivityInfo
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil.request.ImageRequest
import com.dinz.animein.data.local.LocalAuthDatabase
import com.dinz.animein.data.model.CommunityMediaItem
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.data.model.ServerItem
import com.dinz.animein.ui.theme.ColorAccentPrimary
import com.dinz.animein.ui.theme.ColorBackgroundPrimary
import com.dinz.animein.ui.theme.ColorMetricFavorites
import com.dinz.animein.ui.theme.ColorMetricViews
import com.dinz.animein.ui.theme.ColorTextPrimary
import com.dinz.animein.ui.theme.ColorTextSecondary
import kotlinx.coroutines.delay

private val ColorTosca = Color(0xFF26A69A)
private val ColorEmerald = Color(0xFF00C853)

@OptIn(UnstableApi::class)
@Composable
fun StreamingScreen(
    episodeId: String,
    movieId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreamingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    LaunchedEffect(episodeId, movieId) {
        viewModel.loadStreaming(episodeId = episodeId, movieId = movieId)
    }

    // Handle system back button: exits replies, gallery, or screen
    BackHandler {
        when {
            uiState.fullScreenPreviewUrl != null -> viewModel.closeFullScreenImage()
            uiState.activeCommentForReplies != null -> viewModel.closeReplies()
            uiState.isGallerySheetOpen -> viewModel.toggleGallerySheet()
            uiState.isServerPickerOpen -> viewModel.toggleServerPicker()
            uiState.isFullScreen -> {
                viewModel.toggleFullScreen()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            else -> onBackClick()
        }
    }

    // Fullscreen screen orientation control
    LaunchedEffect(uiState.isFullScreen) {
        if (uiState.isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val localDb = remember { LocalAuthDatabase(context) }

    // Periodic watch progress recording
    LaunchedEffect(uiState.isPlaying, uiState.currentPositionMs) {
        if (uiState.isPlaying && uiState.currentPositionMs > 5000L) {
            val mId = uiState.movieId.ifBlank { uiState.episode?.idMovie ?: "" }
            if (mId.isNotBlank()) {
                localDb.saveWatchProgress(
                    movieId = mId,
                    animeTitle = uiState.movieDetail?.movie?.title ?: uiState.episode?.title?.substringBefore(" Episode") ?: "Anime",
                    episodeId = uiState.episodeId,
                    episodeTitle = "Episode ${uiState.episode?.index ?: "1"}",
                    posterUrl = uiState.movieDetail?.movie?.imagePoster ?: uiState.episode?.getEffectiveImage() ?: "",
                    positionMs = uiState.currentPositionMs,
                    durationMs = uiState.durationMs
                )
            }
        }
    }

    // Save watch progress on exit
    DisposableEffect(uiState.episodeId, uiState.movieId) {
        onDispose {
            val state = viewModel.uiState.value
            val mId = state.movieId.ifBlank { state.episode?.idMovie ?: "" }
            if (mId.isNotBlank() && state.currentPositionMs > 3000L) {
                localDb.saveWatchProgress(
                    movieId = mId,
                    animeTitle = state.movieDetail?.movie?.title ?: state.episode?.title?.substringBefore(" Episode") ?: "Anime",
                    episodeId = state.episodeId,
                    episodeTitle = "Episode ${state.episode?.index ?: "1"}",
                    posterUrl = state.movieDetail?.movie?.imagePoster ?: state.episode?.getEffectiveImage() ?: "",
                    positionMs = state.currentPositionMs,
                    durationMs = state.durationMs
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        if (uiState.isLoading && uiState.selectedServer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorAccentPrimary, modifier = Modifier.size(40.dp))
            }
        } else if (uiState.errorMessage != null && uiState.selectedServer == null) {
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
                        }
                    )
                }

                // If not in full screen, render episode details and comments
                if (!uiState.isFullScreen) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.activeCommentForReplies != null -> {
                                // Sub-View: Balasan Komentar
                                CommentRepliesView(
                                    parentComment = uiState.activeCommentForReplies!!,
                                    replies = uiState.repliesList,
                                    isLoading = uiState.isLoadingReplies,
                                    onBackClick = { viewModel.closeReplies() },
                                    onRequireLogin = { viewModel.triggerLoginPrompt() },
                                    onSendReply = { text ->
                                        val localDb = LocalAuthDatabase(context)
                                        val item = localDb.addLocalComment(
                                            episodeId = uiState.episodeId,
                                            text = text,
                                            parentId = uiState.activeCommentForReplies?.id
                                        )
                                        viewModel.addLocalReply(item)
                                    }
                                )
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
                                    contentPadding = PaddingValues(bottom = 72.dp)
                                ) {
                                    // Episode Title, Anime Title & Synopsis (Matching IMG-20261031-WA0049 & DESIGN.md)
                                    item(key = "episode_header") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "Episode ${uiState.episode?.index ?: "1"}",
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            val realTitle = uiState.movieDetail?.movie?.title
                                                ?: uiState.episode?.title?.substringBefore(" Episode")
                                                ?: "Anime Streaming"
                                            Text(
                                                text = realTitle,
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val realSynopsis = uiState.movieDetail?.movie?.synopsis?.ifBlank { null }
                                                ?: "Streaming anime episode terlengkap dengan kualitas jernih dan subtitle Indonesia resmi di platform Yuranime."
                                            Text(
                                                text = realSynopsis,
                                                color = Color(0xFF9CA3AF),
                                                fontSize = 12.5.sp,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }

                                    // Section: COVER & POSTER (Equal Height 195dp, Interleaved, Horizontal Scroll)
                                    item(key = "cover_poster_section") {
                                        val interleavedMedia = remember(uiState.posters, uiState.covers) {
                                            val list = mutableListOf<Pair<CommunityMediaItem, Boolean>>() // Pair(item, isPoster)
                                            val maxLen = maxOf(uiState.posters.size, uiState.covers.size)
                                            for (i in 0 until maxLen) {
                                                if (i < uiState.posters.size) list.add(uiState.posters[i] to true)
                                                if (i < uiState.covers.size) list.add(uiState.covers[i] to false)
                                            }
                                            list
                                        }

                                        if (interleavedMedia.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "COVER & POSTER",
                                                    color = ColorTextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
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
                                                    Box(
                                                        modifier = Modifier
                                                            .height(195.dp)
                                                            .aspectRatio(if (isPoster) 2f / 3f else 16f / 9f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFF181A22))
                                                            .clickable {
                                                                if (mediaImg.isNotBlank()) viewModel.openFullScreenImage(mediaImg)
                                                                else viewModel.toggleGallerySheet(if (isPoster) 0 else 1)
                                                            }
                                                    ) {
                                                        if (mediaImg.isNotBlank()) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(mediaImg)
                                                                    .crossfade(true)
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
                                    }

                                    // Section: COMMENTS (X)
                                    item(key = "comments_header") {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "COMMENTS (${uiState.comments.size})",
                                                color = ColorTextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Sort,
                                                contentDescription = "Sort",
                                                tint = ColorTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    // Comments Items
                                    items(uiState.comments, key = { it.id ?: it.hashCode().toString() }) { comment ->
                                        CommentItemRow(
                                            comment = comment,
                                            onReplyClick = { viewModel.openReplies(comment) },
                                            onRequireLogin = { viewModel.triggerLoginPrompt() }
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Chat FAB (Bottom-Right, Orange Glassmorphism)
                        if (uiState.activeCommentForReplies == null && !uiState.isGallerySheetOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 24.dp)
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ColorAccentPrimary, Color(0xFFFF6D3F))
                                        )
                                    )
                                    .border(1.dp, Color(0x66FFFFFF), CircleShape)
                                    .clickable {
                                        val localDb = LocalAuthDatabase(context)
                                        if (!localDb.isLoggedIn()) {
                                            viewModel.triggerLoginPrompt()
                                        } else {
                                            viewModel.toggleCommentInput()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Tulis Komentar",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
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

        // Comment Input Dialog
        if (uiState.isCommentInputOpen) {
            CommentInputDialog(
                onDismiss = { viewModel.toggleCommentInput() },
                onSend = { text ->
                    val localDb = LocalAuthDatabase(context)
                    val newComment = localDb.addLocalComment(
                        episodeId = uiState.episodeId,
                        text = text
                    )
                    viewModel.addLocalComment(newComment)
                }
            )
        }

        // Login Required Alert Dialog
        if (uiState.showLoginPrompt) {
            LoginRequiredDialog(
                onDismiss = { viewModel.dismissLoginPrompt() },
                onSimulateLogin = {
                    val localDb = LocalAuthDatabase(context)
                    localDb.setLoggedIn(true, "OtakuKeren")
                    viewModel.dismissLoginPrompt()
                    Toast.makeText(context, "Berhasil masuk sebagai OtakuKeren", Toast.LENGTH_SHORT).show()
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
    }
}

// ==================== 1. CUSTOM EXOPLAYER VIEW ====================

@OptIn(UnstableApi::class)
@Composable
private fun CustomExoPlayerView(
    server: ServerItem?,
    uiState: StreamingUiState,
    viewModel: StreamingViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableLongStateOf(0L) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Bind playback listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    viewModel.updatePosition(exoPlayer.currentPosition, exoPlayer.duration)
                } else if (playbackState == Player.STATE_ENDED && uiState.autoNextEpisode) {
                    // Auto next episode if available
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load media URL on server change with position synchronization
    LaunchedEffect(server?.link) {
        val link = server?.link
        if (!link.isNullOrBlank()) {
            val preservedPos = uiState.currentPositionMs
            val mediaItem = MediaItem.fromUri(link)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (preservedPos > 0) {
                exoPlayer.seekTo(preservedPos)
            }
            exoPlayer.play()
        }
    }

    // Sync speed changes
    LaunchedEffect(uiState.playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(uiState.playbackSpeed)
    }

    // Periodic time tracking
    LaunchedEffect(uiState.isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying && !isSeeking) {
                viewModel.updatePosition(exoPlayer.currentPosition, exoPlayer.duration)
            }
            delay(500)
        }
    }

    // Auto hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls && !uiState.isLocked) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!uiState.isLocked) {
                    showControls = !showControls
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
            modifier = Modifier.fillMaxSize()
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
                    tint = ColorAccentPrimary,
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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

                // Center Rewind, Pure White Glassmorphism Play/Pause, Forward (NO GREEN)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Mundur 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Center Play / Pause Big Button with Pure White Glassmorphism (NO GREEN)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration.coerceAtLeast(0))
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Maju 10 detik",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Control Bar (Progress, Time, Fullscreen)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Time and Progress Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                        // Custom Progress Slider in Pure White
                        val totalDuration = if (uiState.durationMs > 0) uiState.durationMs else 1L
                        val currentPos = if (isSeeking) sliderPosition else uiState.currentPositionMs
                        val sliderFraction = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                        Slider(
                            value = sliderFraction,
                            onValueChange = { frac ->
                                isSeeking = true
                                sliderPosition = (frac * totalDuration).toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(sliderPosition)
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier.weight(1f)
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
                    .padding(top = 48.dp, end = 12.dp)
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
                tint = ColorAccentPrimary,
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
                                color = if (isSelected) ColorAccentPrimary else Color(0x33FFFFFF),
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
                                    .border(1.dp, if (isSelected) ColorAccentPrimary else Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isSelected) ColorAccentPrimary else Color.White,
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
                                    color = if (isSelected) ColorAccentPrimary else Color(0xFFD1D5DB),
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
                    text = "Kuy bantu ANIMEIN dapatkan bintang terbaik agar lebih semangat lagi hehe",
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
                        .background(ColorAccentPrimary)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kirim Rating",
                        color = Color.White,
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
private fun FloatingSettingsMenu(
    uiState: StreamingUiState,
    viewModel: StreamingViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(215.dp)
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
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorAccentPrimary,
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
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ColorAccentPrimary,
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
                                color = if (isSelected) ColorAccentPrimary else Color(0xFFE5E7EB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = ColorAccentPrimary,
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
                                color = if (isSelected) ColorAccentPrimary else Color(0xFFE5E7EB),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = ColorAccentPrimary,
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

// ==================== 6. COMMENT ITEM ROW ====================

@Composable
private fun CommentItemRow(
    comment: EpisodeCommentItem,
    onReplyClick: () -> Unit,
    onRequireLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    var isLiked by remember { mutableStateOf(localDb.getReaction(comment.id ?: "") == "like") }
    var isDisliked by remember { mutableStateOf(localDb.getReaction(comment.id ?: "") == "dislike") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Animated Pokemon GIF Avatar via Coil GifDecoder
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(comment.getPokemonAvatarUrl())
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory()
                    else GifDecoder.Factory()
                )
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Username and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.userName ?: "Pengguna",
                    color = ColorTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "•", color = ColorTextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = comment.time ?: "",
                    color = ColorTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Comment Body
            Text(
                text = comment.text ?: "",
                color = Color(0xFFE5E7EB),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Row: Likes, Dislikes, Balasan (Full Width, SpaceBetween, No pill border)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like (Left)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!localDb.isLoggedIn()) {
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
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) ColorAccentPrimary else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (comment.like?.toIntOrNull() ?: 0 + if (isLiked) 1 else 0).toString(),
                        color = ColorTextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                // Dislike (Center)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!localDb.isLoggedIn()) {
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
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) ColorAccentPrimary else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (comment.dislike?.toIntOrNull() ?: 0 + if (isDisliked) 1 else 0).toString(),
                        color = ColorTextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                // Balasan (Right): Orange chat icon + grey text, NO border, NO pill
                val replayCount = comment.replay?.toIntOrNull() ?: 0
                val replayText = if (replayCount > 0) "$replayCount Balasan" else "Balasan"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onReplyClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Balasan",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = replayText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.5.sp,
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
    onSendReply: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    var replyText by remember { mutableStateOf("") }

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
                text = "Balasan",
                color = ColorTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = Color(0x33FFFFFF))

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

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorAccentPrimary, modifier = Modifier.size(28.dp))
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
                        onRequireLogin = onRequireLogin
                    )
                }
            }
        }

        // Reply Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF222634))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = ColorTextPrimary, fontSize = 13.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(ColorAccentPrimary),
                    decorationBox = { innerTextField ->
                        if (replyText.isEmpty()) {
                            Text(text = "Tulis Balasan..", color = Color(0xFF6B7280), fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (!localDb.isLoggedIn()) {
                        onRequireLogin()
                    } else if (replyText.isNotBlank()) {
                        onSendReply(replyText.trim())
                        replyText = ""
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Kirim",
                    tint = ColorAccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Sub-Item Balasan: Pokemon Avatar on the RIGHT side
@Composable
private fun CommentReplyItemRow(
    reply: EpisodeCommentItem,
    onRequireLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }
    var isLiked by remember { mutableStateOf(localDb.getReaction(reply.id ?: "") == "like") }
    var isDisliked by remember { mutableStateOf(localDb.getReaction(reply.id ?: "") == "dislike") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: User info, Comment text, Like & Dislike
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = reply.text ?: "",
                color = Color(0xFFE5E7EB),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Like
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!localDb.isLoggedIn()) onRequireLogin()
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
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) ColorAccentPrimary else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (reply.like?.toIntOrNull() ?: 0 + if (isLiked) 1 else 0).toString(),
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Dislike
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (!localDb.isLoggedIn()) onRequireLogin()
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
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) ColorAccentPrimary else ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (reply.dislike?.toIntOrNull() ?: 0 + if (isDisliked) 1 else 0).toString(),
                        color = ColorTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right side: Pokemon Animated GIF Avatar (Matching Screenshot_20261031_125604_ANIMEIN)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(reply.getPokemonAvatarUrl())
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory()
                    else GifDecoder.Factory()
                )
                .crossfade(true)
                .build(),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
    }
}

// ==================== 7B. COMMENT INPUT & LOGIN REQUIRED DIALOGS ====================

@Composable
private fun CommentInputDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF181A22))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tulis Komentar",
                    color = ColorTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222634))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(color = ColorTextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(ColorAccentPrimary),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    text = "Tulis tanggapanmu mengenai anime ini...",
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Batal",
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (text.isNotBlank()) ColorAccentPrimary else Color(0x40FF5722))
                            .clickable(enabled = text.isNotBlank()) {
                                onSend(text)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Kirim",
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
                    text = "Fitur ini memerlukan autentikasi akun. Silakan login untuk memberikan like, dislike, atau komentar.",
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
                            .background(ColorAccentPrimary)
                            .clickable { onSimulateLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Masuk Akun", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            contentColor = ColorAccentPrimary,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(2.5.dp)
                        .background(ColorAccentPrimary)
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
                        color = if (selectedTab == 0) ColorAccentPrimary else ColorTextSecondary,
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
                        color = if (selectedTab == 1) ColorAccentPrimary else ColorTextSecondary,
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
                                .data(img)
                                .crossfade(true)
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
                                .data(img)
                                .crossfade(true)
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
