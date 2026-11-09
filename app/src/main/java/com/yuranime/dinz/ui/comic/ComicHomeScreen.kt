package com.yuranime.dinz.ui.comic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.model.ComicBannerItem
import com.yuranime.dinz.data.model.ComicItem
import com.yuranime.dinz.data.model.ComicReadHistoryItem
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.liquidGlassBackdrop
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.util.Locale

private const val COMIC_IMAGE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val COMIC_IMAGE_REFERER = "https://app.shinigami.asia/"

private val ComicScreenBg = Color(0xFF0B0D12)
private val ComicCardBg = Color(0xFF141418)
private val ComicRedAccent = Color(0xFFE11D48)
private val ComicGoldRating = Color(0xFFFACC15)
private val ComicMutedText = Color(0xFF94A3B8)
private val ComicLightGray = Color(0xFFCBD5E1)

@Composable
fun ComicHomeScreen(
    selectedTab: NavTab = NavTab.COMIC,
    onTabSelected: (NavTab) -> Unit,
    onComicClick: (comicId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    onOpenAllComics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComicHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backdropLayer = rememberGraphicsLayer()
    val isLiquidGlass = AppPreferences.liquidGlassEnabled.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.refreshHistory()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComicScreenBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdrop(backdropLayer, enabled = isLiquidGlass)
        ) {
            if (uiState.isLoading && uiState.banners.isEmpty() && uiState.latestComics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ComicRedAccent, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // 1. HERO SLIDER KOMIK
                    if (uiState.banners.isNotEmpty()) {
                        item(key = "comic_hero_slider") {
                            ComicHeroSlider(
                                banners = uiState.banners,
                                onReadClick = { banner ->
                                    onComicClick(banner.comicId)
                                },
                                onDetailClick = { banner ->
                                    onComicClick(banner.comicId)
                                }
                            )
                        }
                    }

                    // 2. SECTION LANJUTKAN MEMBACA (Kondisional Riwayat Baca)
                    if (uiState.continueReading.isNotEmpty()) {
                        item(key = "comic_continue_reading") {
                            Spacer(modifier = Modifier.height(24.dp))
                            ComicContinueReadingSection(
                                historyList = uiState.continueReading,
                                onContinueClick = { item ->
                                    if (item.chapterId.isNotBlank()) {
                                        onChapterClick(item.chapterId)
                                    } else {
                                        onComicClick(item.comicId)
                                    }
                                }
                            )
                        }
                    }

                    // 3. SECTION TRENDING & POPULER
                    if (uiState.trendingComics.isNotEmpty()) {
                        item(key = "comic_trending_populer") {
                            Spacer(modifier = Modifier.height(24.dp))
                            ComicSectionHeaderWithAccent(
                                title = "TRENDING & POPULER",
                                subtitle = "Explore multiple options for maximum fun"
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ComicTrendingRow(
                                comics = uiState.trendingComics,
                                onComicClick = onComicClick
                            )
                        }
                    }

                    // 4. SECTION RILISAN TERBARU (Grid 2 Kolom x 6 Baris = 12 Item)
                    if (uiState.latestComics.isNotEmpty()) {
                        item(key = "comic_rilisan_terbaru") {
                            Spacer(modifier = Modifier.height(28.dp))
                            ComicSectionHeaderWithAccent(
                                title = "RILISAN TERBARU",
                                subtitle = "Explore multiple options for maximum fun",
                                actionText = "LIHAT SEMUA >",
                                onActionClick = onOpenAllComics
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ComicLatestGridSection(
                                comics = uiState.latestComics.take(12),
                                onComicClick = onComicClick,
                                onChapterClick = onChapterClick
                            )
                        }
                    }

                    // 5. SECTION TERPOPULER SEPANJANG MASA
                    if (uiState.popularComics.isNotEmpty()) {
                        item(key = "comic_terpopuler_sepanjang_masa") {
                            Spacer(modifier = Modifier.height(28.dp))
                            ComicSectionHeaderWithAccent(
                                title = "TERPOPULER SEPANJANG MASA",
                                subtitle = "Paling banyak dibaca sepanjang masa",
                                actionText = "LIHAT SEMUA >",
                                onActionClick = onOpenAllComics
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ComicAllTimePopularRow(
                                comics = uiState.popularComics,
                                onComicClick = onComicClick
                            )
                        }
                    }
                }
            }

            // Tombol Pencarian Komik di Pojok Kanan Atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                        ) { viewModel.openSearchDialog() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari Komik",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Clear Liquid Glass Navbar
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            backdropLayer = backdropLayer,
            isLiquidGlassEnabled = isLiquidGlass,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Dialog Pencarian Komik Real-Time
        if (uiState.isSearchDialogVisible) {
            ComicSearchDialog(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onDismiss = { viewModel.closeSearchDialog() },
                results = uiState.searchResults,
                isSearching = uiState.isSearching,
                onComicClick = { comicId ->
                    viewModel.closeSearchDialog()
                    onComicClick(comicId)
                }
            )
        }
    }
}

// ==================== 1. HERO SLIDER KOMIK ====================

@Composable
private fun ComicHeroSlider(
    banners: List<ComicBannerItem>,
    onReadClick: (ComicBannerItem) -> Unit,
    onDetailClick: (ComicBannerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) { page ->
            val banner = banners[page]

            Box(modifier = Modifier.fillMaxSize()) {
                // Background cover yang diperbesar dengan vignette/dark blur
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(banner.backgroundImage.ifBlank { banner.posterImage })
                        .crossfade(true)
                        .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                        .addHeader("Referer", COMIC_IMAGE_REFERER)
                        .build(),
                    contentDescription = banner.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Lapisan overlay gelap bertingkat untuk keterbacaan teks
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x990B0D12),
                                    Color(0xCC0B0D12),
                                    ComicScreenBg
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xE60B0D12),
                                    Color(0x800B0D12),
                                    Color(0x330B0D12)
                                )
                            )
                        )
                )

                // Baris Konten: Sisi Kiri (Teks & 2 Tombol), Sisi Kanan (Poster)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SISI KIRI (weight 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Judul Komik: UPPERCASE, SF Bold putih 22-24sp
                        Text(
                            text = banner.title.uppercase(Locale.ROOT),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sinopsis Singkat
                        Text(
                            text = banner.synopsis.ifBlank { "Ikuti kelanjutan kisah petualangan seru dan menegangkan komik ini sekarang juga!" },
                            color = ComicLightGray,
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2 Tombol Aksi Berdampingan
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tombol Kiri: Kapsul Merah Menyala
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ComicRedAccent)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White)
                                    ) { onReadClick(banner) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "▶ BACA SEKARANG",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            // Tombol Kanan: Kapsul Gelap Semi-Transparan
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x33FFFFFF))
                                    .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White)
                                    ) { onDetailClick(banner) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "DETAIL",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }

                    // SISI KANAN (Poster Komik Vertikal 110.dp x 2:3)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .shadow(12.dp, RoundedCornerShape(10.dp), clip = false)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            .background(ComicCardBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { onDetailClick(banner) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(banner.posterImage.ifBlank { banner.backgroundImage })
                                .crossfade(true)
                                .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                                .addHeader("Referer", COMIC_IMAGE_REFERER)
                                .build(),
                            contentDescription = banner.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Titik Pagination Dots di Bagian Bawah Tengah Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.indices.forEach { index ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) ComicRedAccent else Color(0x4DFFFFFF))
                )
            }
        }
    }
}

// ==================== 2. SECTION LANJUTKAN MEMBACA ====================

@Composable
private fun ComicContinueReadingSection(
    historyList: List<ComicReadHistoryItem>,
    onContinueClick: (ComicReadHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        ComicSectionHeaderWithAccent(
            title = "LANJUTKAN MEMBACA",
            subtitle = "Lanjutkan membaca komik yang terakhir kamu buka"
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyList) { item ->
                Row(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComicCardBg)
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                        ) { onContinueClick(item) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Poster Kecil (56.dp, 2:3, shape 8.dp)
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.coverUrl)
                                .crossfade(true)
                                .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                                .addHeader("Referer", COMIC_IMAGE_REFERER)
                                .build(),
                            contentDescription = item.comicTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Info Judul & Kapsul Merah Lanjutkan
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.comicTitle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        val buttonText = if (item.chapterNumber.isNotBlank()) {
                            "LANJUTKAN Ch. ${item.chapterNumber}"
                        } else {
                            "LANJUTKAN BACA"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(ComicRedAccent)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = buttonText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. SECTION TRENDING & POPULER ====================

@Composable
private fun ComicTrendingRow(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(comics) { comic ->
            Column(
                modifier = Modifier
                    .width(118.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 60.dp, color = Color.White)
                    ) { onComicClick(comic.id) }
            ) {
                // Poster Komik Vertikal (aspectRatio 2:3, shape 8.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ComicCardBg)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(comic.coverPortrait.ifBlank { comic.coverLandscape })
                            .crossfade(true)
                            .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                            .addHeader("Referer", COMIC_IMAGE_REFERER)
                            .build(),
                        contentDescription = comic.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Judul Komik: UPPERCASE, SF Bold putih 12.sp, minLines = 2, maxLines = 2
                Text(
                    text = comic.title.uppercase(Locale.ROOT),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Baris Bawah: Bintang Emas + Rating di kiri, Bendera Negara di kanan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ratingStr = comic.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "7.0"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "★",
                            color = ComicGoldRating,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = ratingStr,
                            color = ComicGoldRating,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    // Ikon Bendera Negara (16.dp x 11.dp)
                    CountryFlagIcon(comic = comic)
                }
            }
        }
    }
}

// ==================== 4. SECTION RILISAN TERBARU (Grid 2 Kolom x 6 Baris = 12 Item) ====================

@Composable
private fun ComicLatestGridSection(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = comics.take(12).chunked(2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                val first = pair.getOrNull(0)
                if (first != null) {
                    ComicLatestCard(
                        comic = first,
                        onComicClick = { onComicClick(first.id) },
                        onChapterClick = onChapterClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                val second = pair.getOrNull(1)
                if (second != null) {
                    ComicLatestCard(
                        comic = second,
                        onComicClick = { onComicClick(second.id) },
                        onChapterClick = onChapterClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ComicLatestCard(
    comic: ComicItem,
    onComicClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 80.dp, color = Color.White)
            ) { onComicClick() },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Poster Kiri (width = 68.dp, 2:3, shape 8.dp)
        Box(
            modifier = Modifier
                .width(68.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(ComicCardBg)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(comic.coverPortrait.ifBlank { comic.coverLandscape })
                    .crossfade(true)
                    .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                    .addHeader("Referer", COMIC_IMAGE_REFERER)
                    .build(),
                contentDescription = comic.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Sisi Kanan: Judul, Bendera, dan 3 Chapter Terbaru
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Judul Komik: UPPERCASE, minLines = 2, maxLines = 2
            Text(
                text = comic.title.uppercase(Locale.ROOT),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            // Ikon Bendera Negara tepat di bawah judul
            CountryFlagIcon(comic = comic)

            Spacer(modifier = Modifier.height(2.dp))

            // 3 Chapter Terbaru
            val chaptersToShow = comic.chapters.take(3)
            chaptersToShow.forEach { ch ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                        ) {
                            if (ch.id.isNotBlank()) {
                                onChapterClick(ch.id)
                            } else {
                                onComicClick()
                            }
                        }
                        .padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ",
                            color = ComicRedAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CH. ${ch.number}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = ch.releaseTime.ifBlank { "Just now" },
                        color = ComicMutedText,
                        fontSize = 10.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ==================== 5. SECTION TERPOPULER SEPANJANG MASA ====================

@Composable
private fun ComicAllTimePopularRow(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Kelompokkan 3 komik per kolom (Kolom 1: 1,2,3; Kolom 2: 4,5,6, dst.)
    val columns = comics.chunked(3)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(columns) { colIndex, trio ->
            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                trio.forEachIndexed { rowIndex, comic ->
                    val rankNumber = (colIndex * 3) + rowIndex + 1
                    val isTopThree = rankNumber <= 3
                    val rankColor = if (isTopThree) Color.White else ComicMutedText

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { onComicClick(comic.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Angka Peringkat Besar di Kiri (lebar tetap 36.dp, 32.sp)
                        Box(
                            modifier = Modifier.width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rankNumber.toString(),
                                color = rankColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        // 2. Poster Komik di Tengah (width = 64.dp, 2:3, shape 8.dp)
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ComicCardBg)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(comic.coverPortrait.ifBlank { comic.coverLandscape })
                                    .crossfade(true)
                                    .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                                    .addHeader("Referer", COMIC_IMAGE_REFERER)
                                    .build(),
                                contentDescription = comic.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // 3. Informasi di Kanan (Judul + Skor & Pembaca)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = comic.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Ikon Bintang + Skor Rating
                                val ratingText = comic.rating?.let {
                                    String.format(Locale.GERMAN, "%.1f", it)
                                } ?: "8,6"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "★",
                                        color = ComicGoldRating,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = ratingText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                // Ikon Mata + Jumlah Pembaca
                                val viewsText = formatViewCount(comic.viewCount)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "👁",
                                        color = ComicMutedText,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = viewsText,
                                        color = ComicMutedText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== KOMPONEN PENDUKUNG ====================

@Composable
private fun ComicSectionHeaderWithAccent(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Aksen Bar Merah Menyala
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ComicRedAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = ComicMutedText,
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Text(
                text = actionText,
                color = ComicRedAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = ComicRedAccent)
                    ) { onActionClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CountryFlagIcon(
    comic: ComicItem,
    modifier: Modifier = Modifier
) {
    val resId = comic.flagDrawableRes
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(width = 16.dp, height = 11.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(2.dp))
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = comic.format,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(comic.flagUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = comic.format,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun formatViewCount(views: Long): String {
    return when {
        views >= 1_000_000 -> String.format(Locale.GERMAN, "%.1fjt", views / 1_000_000f)
        views >= 1_000 -> String.format(Locale.GERMAN, "%.1frb", views / 1_000f)
        views > 0 -> views.toString()
        else -> "50,1jt" // Fallback data views populer
    }
}

// ==================== DIALOG PENCARIAN KOMIK ====================

@Composable
private fun ComicSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    results: List<ComicItem>,
    isSearching: Boolean,
    onComicClick: (comicId: String) -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161A22))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cari Komik di KomikID",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kolom Input Pencarian
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1117))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ComicMutedText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = SfProFontFamily
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(ComicRedAccent),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Ketik judul komik (contoh: Solo, Martial)...",
                                    color = ComicMutedText,
                                    fontSize = 13.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ComicRedAccent, strokeWidth = 2.dp)
                    }
                } else if (results.isEmpty() && query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada komik yang cocok",
                            color = ComicMutedText,
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(results) { comic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ComicCardBg)
                                    .clickable { onComicClick(comic.id) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(comic.coverPortrait.ifBlank { comic.coverLandscape })
                                            .crossfade(true)
                                            .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                                            .addHeader("Referer", COMIC_IMAGE_REFERER)
                                            .build(),
                                        contentDescription = comic.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comic.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CountryFlagIcon(comic = comic)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = comic.format,
                                            color = ComicMutedText,
                                            fontSize = 11.sp,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
