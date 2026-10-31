package com.dinz.animein.ui.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.R
import com.dinz.animein.data.model.CommunityMediaItem
import com.dinz.animein.data.model.ContributorItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.DiscussionItem
import com.dinz.animein.data.model.EpisodeItem
import com.dinz.animein.data.model.MovieDetail
import com.dinz.animein.data.model.SeasonItem
import com.dinz.animein.ui.theme.ColorAccentPrimary
import com.dinz.animein.ui.theme.ColorBackgroundPrimary
import com.dinz.animein.ui.theme.ColorMetricFavorites
import com.dinz.animein.ui.theme.ColorMetricViews
import com.dinz.animein.ui.theme.ColorTextPrimary
import com.dinz.animein.ui.theme.ColorTextSecondary

@Composable
fun AnimeDetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAnimeClick: (String) -> Unit = {},
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
                    // 1. Hero Cover Header
                    HeroCoverHeader(
                        movie = movie,
                        latestEpisode = uiState.latestEpisode,
                        isFavorite = uiState.isFavorite,
                        onBackClick = onBackClick,
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onShareClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Nonton ${movie.title} di AnimeIn!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan anime"))
                        },
                        onPlayClick = {
                            Toast.makeText(context, "Memutar Episode ${uiState.latestEpisode?.index ?: "1"}", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // 2. Scrollable TabBar
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
                        DetailTab.values().forEach { tab ->
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
                                episodes = uiState.episodes,
                                onEpisodeClick = { ep ->
                                    Toast.makeText(context, "Putar ${ep.title ?: "Episode " + ep.index}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DetailTab.SEASON -> TabSeasonContent(
                                seasons = uiState.seasons,
                                onSeasonClick = { seasonId ->
                                    onAnimeClick(seasonId)
                                }
                            )
                            DetailTab.DISCUSSION -> TabDiscussionContent(discussions = uiState.discussions)
                            DetailTab.CUPLIX -> TabCuplixContent(cuplixList = uiState.cuplixList)
                            DetailTab.POSTER -> TabPosterContent(posters = uiState.posters)
                            DetailTab.COVER -> TabCoverContent(covers = uiState.covers)
                            DetailTab.CONTRIBUTOR -> TabContributorContent(contributors = uiState.contributors)
                        }
                    }
                }
            }
        }
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
            .height(230.dp)
    ) {
        // Landscape Cover Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.getEffectiveCover())
                .crossfade(true)
                .build(),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Overlay (Matte dark integration)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99000000),
                            Color(0x33000000),
                            Color(0xCC0B0B0B),
                            ColorBackgroundPrimary
                        )
                    )
                )
        )

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Right: Badges & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = ColorTextPrimary,
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
                        color = ColorTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Favorite Icon
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorit",
                            tint = if (isFavorite) ColorMetricFavorites else ColorTextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Share Icon
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
            // Left: Title + Views + Favorites
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = movie.title ?: "",
                    color = ColorTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Views
                    Text(
                        text = "👁 ${(movie.views ?: "0")} views",
                        color = ColorMetricViews,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    // Favorites
                    Text(
                        text = "★ ${(movie.favorites ?: "0")} favorites",
                        color = ColorMetricFavorites,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Play Button with Episode text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayClick() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.8.dp, ColorAccentPrimary, CircleShape)
                        .background(Color(0x40FF5722)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val epText = "ep " + (latestEpisode?.index ?: "1")
                Text(
                    text = epText,
                    color = ColorTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==================== 2. TAB INFO ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabInfoContent(movie: MovieDetail) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
        ) {
            // Header Poster & Titles
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Poster vertical (2:3)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(movie.getEffectivePoster())
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title,
                        modifier = Modifier
                            .width(105.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181920)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Judul",
                            color = ColorTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = movie.title ?: "-",
                            color = ColorTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Judul Alternatif",
                            color = ColorTextSecondary,
                            fontSize = 11.sp
                        )
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

            // Synopsis
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sinopsis",
                    color = ColorTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movie.synopsis?.ifBlank { "Belum ada sinopsis tersedia." } ?: "Belum ada sinopsis tersedia.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }

            // Key-Value Metadata
            item {
                Spacer(modifier = Modifier.height(18.dp))

                MetaRow(label = "Tipe", value = movie.type ?: "TV")
                MetaRow(label = "Aired Start", value = movie.airedStart ?: "-")
                MetaRow(label = "Aired End", value = movie.airedEnd ?: "?")
                MetaRow(label = "Studios", value = movie.studio ?: "-")

                // Genres
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kategori",
                    color = ColorTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val genres = movie.getGenreList()
                if (genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        genres.forEach { g ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E2028))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = g,
                                    color = Color(0xFFE5E7EB),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    Text(text = "-", color = ColorTextPrimary, fontSize = 12.sp)
                }
            }
        }

        // Floating Action Button Edit
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF232530))
                .clickable { /* Edit metadata action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Info",
                tint = ColorTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = ColorTextSecondary, fontSize = 11.sp)
        Text(text = value, color = ColorTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ==================== 3. TAB EPISODE ====================

@Composable
private fun TabEpisodeContent(
    episodes: List<EpisodeItem>,
    onEpisodeClick: (EpisodeItem) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada daftar episode.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(episodes, key = { it.id ?: it.index.toString() }) { ep ->
            Box(
                modifier = Modifier
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181920))
                    .clickable { onEpisodeClick(ep) }
            ) {
                val imageUrl = ep.getEffectiveImage()
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = ep.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Silhouette wolf placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wolf_logo),
                            contentDescription = "AnimeIn Wolf Logo",
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
                            .clip(RoundedCornerShape(bottomEnd = 4.dp))
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

                // Episode Index Pill (bottom-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ep.index ?: "1",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181920))
                    .clickable { season.id?.let { onSeasonClick(it) } }
            ) {
                // Cover image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(season.getEffectiveCover())
                        .crossfade(true)
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
                                    Color(0x80000000),
                                    Color(0xFA0B0B0B)
                                )
                            )
                        )
                )

                // Season Title & Metrics
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = season.season?.ifBlank { season.title } ?: season.title ?: "",
                        color = ColorTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "👁 ${(season.views ?: "0")} views",
                            color = ColorMetricViews,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "★ ${(season.favorites ?: "0")} favorites",
                            color = ColorMetricFavorites,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ==================== 5. TAB DISCUSSION ====================

@Composable
private fun TabDiscussionContent(discussions: List<DiscussionItem>) {
    if (discussions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada diskusi untuk anime ini.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(discussions, key = { it.id ?: it.hashCode().toString() }) { disc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14161E))
                    .padding(12.dp)
            ) {
                // User Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2D3C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (disc.userName?.take(1) ?: "U").uppercase(),
                        color = ColorTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = disc.userName ?: "Pengguna",
                            color = ColorTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = disc.time ?: "",
                            color = ColorTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = disc.text ?: "",
                        color = Color(0xFFE5E7EB),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ==================== 6. TAB CUPLIX ====================

@Composable
private fun TabCuplixContent(cuplixList: List<CuplixItem>) {
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 14f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF181920))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.getEffectiveThumbnail())
                            .crossfade(true)
                            .build(),
                        contentDescription = item.caption,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Likes overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.countLikes ?: "0"} ♡",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!item.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.caption,
                        color = ColorTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==================== 7. TAB POSTER & COVER ====================

@Composable
private fun TabPosterContent(posters: List<CommunityMediaItem>) {
    if (posters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada poster kiriman komunitas.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(posters, key = { it.id ?: it.hashCode().toString() }) { item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181920))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.getEffectiveImage())
                            .crossfade(true)
                            .build(),
                        contentDescription = "Poster ${item.username}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Point & Upvote
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${item.point ?: "0"} P",
                            color = ColorMetricFavorites,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Upvote",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.username ?: "Pengguna",
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TabCoverContent(covers: List<CommunityMediaItem>) {
    if (covers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada cover kiriman komunitas.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(covers, key = { it.id ?: it.hashCode().toString() }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181920))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.getEffectiveImage())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover ${item.username}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top user pill
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.username ?: "User",
                        color = ColorTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${item.point ?: "0"} P",
                        color = ColorMetricFavorites,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== 8. TAB CONTRIBUTOR ====================

@Composable
private fun TabContributorContent(contributors: List<ContributorItem>) {
    if (contributors.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Belum ada kontributor terdaftar.", color = ColorTextSecondary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(contributors, key = { it.id ?: it.hashCode().toString() }) { contrib ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14161E))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF232530)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (contrib.username?.take(1) ?: "K").uppercase(),
                            color = ColorTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = contrib.username ?: "-",
                            color = ColorTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Views: ${contrib.views ?: "0"} • Likes: ${contrib.likes ?: "0"}",
                            color = ColorTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Points badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x33FFB300))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${contrib.point ?: "0"} P",
                        color = ColorMetricFavorites,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
