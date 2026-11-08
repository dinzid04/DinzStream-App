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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.model.CommunityMediaItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.MovieDetail
import com.yuranime.dinz.data.model.SeasonItem
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

    LaunchedEffect(movieId) {
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

            uiState.movieDetail != null -> {
                val movie = uiState.movieDetail!!
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Hero Cover Header with Edge-to-Edge Status Bar Handling
                    HeroCoverHeader(
                        movie = movie,
                        latestEpisode = uiState.latestEpisode,
                        isFavorite = uiState.isFavorite,
                        onBackClick = onBackClick,
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onShareClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Nonton ${movie.title} di Yuranime!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan anime"))
                        },
                        onPlayClick = {
                            val targetEpId = uiState.latestEpisode?.id ?: uiState.episodes.firstOrNull()?.id
                            if (targetEpId != null) {
                                onEpisodeClick(targetEpId)
                            }
                        }
                    )

                    // 2. Scrollable TabBar (Clean 6 Tabs)
                    ScrollableTabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = ColorBackgroundPrimary,
                        contentColor = ColorAccentPrimary,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            if (uiState.selectedTab.ordinal < tabPositions.size) {
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal])
                                        .height(2.5.dp)
                                        .background(ColorAccentPrimary)
                                )
                            }
                        },
                        divider = {}
                    ) {
                        DetailTab.entries.forEach { tab ->
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
                            DetailTab.INFO -> TabInfoContent(movie = movie)
                            DetailTab.EPISODE -> TabEpisodeContent(
                                episodes = uiState.filteredEpisodes,
                                searchQuery = uiState.episodeSearchQuery,
                                onSearchQueryChange = { viewModel.setEpisodeSearchQuery(it) },
                                isSortAscending = uiState.isEpisodeSortAscending,
                                onToggleSort = { viewModel.toggleEpisodeSort() },
                                isLoadingMore = uiState.isLoadingMoreEpisodes,
                                onLoadMore = { viewModel.loadMoreEpisodes() },
                                onEpisodeClick = { ep ->
                                    ep.id?.let { onEpisodeClick(it) }
                                }
                            )
                            DetailTab.SEASON -> TabSeasonContent(
                                seasons = uiState.seasons,
                                onSeasonClick = { seasonId ->
                                    onAnimeClick(seasonId)
                                }
                            )
                            DetailTab.CUPLIX -> TabCuplixContent(
                                cuplixList = uiState.cuplixList,
                                onCuplixClick = onCuplixClick
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
                com.yuranime.dinz.ui.components.CoachmarkStep(
                    title = "Informasi & Daftar Episode",
                    description = "Lihat sinopsis lengkap, skor rating, genre, dan pilih nomor episode yang ingin kamu tonton langsung.",
                    relativeLeft = 0.04f,
                    relativeTop = 0.38f,
                    relativeWidth = 0.92f,
                    relativeHeight = 0.25f,
                    cornerRadiusDp = 14f
                ),
                com.yuranime.dinz.ui.components.CoachmarkStep(
                    title = "Tombol Putar Cepat & Favorit",
                    description = "Mulai streaming instan dari episode terakhir yang kamu tonton atau bookmark ke daftar favoritmu.",
                    relativeLeft = 0.04f,
                    relativeTop = 0.22f,
                    relativeWidth = 0.92f,
                    relativeHeight = 0.12f,
                    cornerRadiusDp = 14f
                ),
                com.yuranime.dinz.ui.components.CoachmarkStep(
                    title = "Tab Season, Cuplix & Poster",
                    description = "Ganti musim tayang, tonton video pendek cuplix dari anime ini, atau unduh poster resolusi tinggi.",
                    relativeLeft = 0.04f,
                    relativeTop = 0.65f,
                    relativeWidth = 0.92f,
                    relativeHeight = 0.22f,
                    cornerRadiusDp = 14f
                )
            )
        }

        com.yuranime.dinz.ui.components.SpotlightCoachmarkOverlay(
            steps = detailSteps,
            isVisible = showDetailOnboarding && uiState.movieDetail != null,
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
    onPlayClick: () -> Unit
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
private fun TabInfoContent(movie: MovieDetail) {
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
