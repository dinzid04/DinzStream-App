package com.dinz.animein.ui.cuplix

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.R
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CuplixViewScreen(
    initialCuplix: CuplixItem? = null,
    initialCuplixId: String? = initialCuplix?.id,
    viewModel: CuplixViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAnimeClick: (idMovie: String) -> Unit,
    onEpisodeClick: (idEpisode: String, idMovie: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialCuplix, initialCuplixId) {
        viewModel.initialize(initialCuplix, initialCuplixId)
    }

    BackHandler {
        if (uiState.activeCommentsFypId != null) {
            viewModel.closeComments()
        } else {
            onBackClick()
        }
    }

    if (uiState.isLoading && uiState.cuplixList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF5722))
        }
        return
    }

    val cuplixList = uiState.cuplixList
    if (cuplixList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.errorMessage ?: "Tidak ada tayangan Cuplix",
                color = Color.White,
                fontFamily = SfProFontFamily
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { cuplixList.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = cuplixList[page]
            val isCurrentPage = page == pagerState.currentPage
            val streamUrl = item.idEpisode?.let { uiState.streamUrlMap[it] }

            CuplixVideoPage(
                item = item,
                streamUrl = streamUrl,
                isCurrentPage = isCurrentPage,
                isLiked = uiState.likedItemIds.contains(item.id ?: ""),
                likesCount = uiState.likesCountMap[item.id ?: ""] ?: (item.countLikes?.toIntOrNull() ?: 0),
                commentsCount = uiState.commentsCountMap[item.id ?: ""] ?: (item.countComments?.toIntOrNull() ?: 0),
                onToggleLike = { item.id?.let { viewModel.toggleLike(it) } },
                onCommentClick = {
                    item.id?.let { fypId ->
                        viewModel.openComments(fypId)
                    }
                },
                onAnimeClick = { item.idMovie?.let { onAnimeClick(it) } },
                onEpisodeClick = {
                    val epId = item.idEpisode.orEmpty()
                    val mId = item.idMovie.orEmpty()
                    if (epId.isNotBlank()) onEpisodeClick(epId, mId)
                }
            )
        }

        // Top Navigation Header (Back + Resolution Menu ⋮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            var showResolutionMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showResolutionMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Pilihan Resolusi",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                DropdownMenu(
                    expanded = showResolutionMenu,
                    onDismissRequest = { showResolutionMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF161B26))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Kualitas Video Cuplix",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    val resolutions = listOf("360p", "480p", "720p", "1080p")
                    resolutions.forEach { res ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = res,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (uiState.selectedQuality == res) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = SfProFontFamily
                                    )
                                    if (uiState.selectedQuality == res) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Terpilih",
                                            tint = Color(0xFFFF5722),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectQuality(res)
                                showResolutionMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Comments Bottom Sheet
        if (uiState.activeCommentsFypId != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeComments() },
                sheetState = sheetState,
                containerColor = Color(0xFF0D111A),
                contentColor = Color.White
            ) {
                CuplixCommentBottomSheetContent(
                    comments = uiState.commentsList,
                    isLoading = uiState.isLoadingComments,
                    onSendComment = { text -> viewModel.postComment(text) }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun CuplixVideoPage(
    item: CuplixItem,
    streamUrl: String?,
    isCurrentPage: Boolean,
    isLiked: Boolean,
    likesCount: Int,
    commentsCount: Int,
    onToggleLike: () -> Unit,
    onCommentClick: () -> Unit,
    onAnimeClick: () -> Unit,
    onEpisodeClick: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember(isCurrentPage, streamUrl, item.id) { mutableStateOf(true) }
    var isFirstFrameRendered by remember(isCurrentPage, streamUrl, item.id) { mutableStateOf(false) }
    var exoPlayer by remember(item.id) { mutableStateOf<ExoPlayer?>(null) }

    val startMs = remember(item) { item.getTimeStartMs() }
    val endMs = remember(item) { item.getTimeEndMs() }

    DisposableEffect(isCurrentPage, streamUrl, item.id) {
        if (isCurrentPage && !streamUrl.isNullOrBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(streamUrl)
                setMediaItem(mediaItem)
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
                if (startMs > 0) {
                    seekTo(startMs)
                }
                prepare()
                play()
            }
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                    isPlaying = true
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        player.play()
                    }
                }
            }
            player.addListener(listener)
            exoPlayer = player
        } else {
            isFirstFrameRendered = false
            exoPlayer?.release()
            exoPlayer = null
        }

        onDispose {
            isFirstFrameRendered = false
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    // Loop interval between timeStart and timeEnd
    LaunchedEffect(exoPlayer, startMs, endMs) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            delay(200)
            if (player.isPlaying) {
                val curPos = player.currentPosition
                if (endMs > startMs && curPos >= endMs) {
                    player.seekTo(startMs)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.play()
                        isPlaying = true
                    }
                }
            }
    ) {
        // 1. Center Video View (16:9 / 4:3 fit)
        if (isCurrentPage && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Thumbnail poster while buffering / until first video frame renders (NO BLACK SCREEN FLASH)
        AnimatedVisibility(
            visible = !isFirstFrameRendered || !isCurrentPage || exoPlayer == null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.getEffectiveThumbnail())
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime_wide)
                        .error(R.drawable.placeholder_yuranime_wide)
                        .build(),
                    contentDescription = item.anime,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Pause Indicator Overlay
        AnimatedVisibility(
            visible = !isPlaying && isCurrentPage,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // 2. Right Action Sidebar (Heart, Comment, Anime Poster)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onToggleLike)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF244D) else Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = likesCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }

            // Comment Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onCommentClick)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "Comments",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = commentsCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }

            // Anime Poster Card
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAnimeClick)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.getEffectivePoster())
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = item.anime,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Bottom Left Info Overlay (Creator, Caption, Pills)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Creator Row (Avatar + Username)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.getPokemonAvatarUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = item.username,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFFF5722), CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.username ?: "Yuranime Creator",
                    color = Color(0xFFFF5722),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            // Caption
            if (!item.caption.isNullOrBlank()) {
                Text(
                    text = item.caption.trim(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            // Anime Title Pill (Orange)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF5722))
                    .clickable(onClick = onAnimeClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${item.anime ?: "Lihat Anime"}   >",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Episode Pill (Dark translucent)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x80222222))
                    .clickable(onClick = onEpisodeClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${item.episode ?: "Episode 1"}   >",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}

@Composable
private fun CuplixCommentBottomSheetContent(
    comments: List<EpisodeCommentItem>,
    isLoading: Boolean,
    onSendComment: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Input Bar at top matching Screenshot_20261031_211753_ANIMEIN.jpg
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Tulis komentar..",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF18202C)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendComment(inputText)
                        inputText = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Comment List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF5722))
            }
        } else if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada komentar. Jadilah yang pertama!",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontFamily = SfProFontFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = comments,
                    key = { it.id ?: it.hashCode().toString() }
                ) { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(c.getPokemonAvatarUrl())
                                .crossfade(true)
                                .build(),
                            contentDescription = c.userName,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = c.userName ?: "User",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                                Text(
                                    text = " • ${c.time ?: "beberapa waktu lalu"}",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = c.text ?: "",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
