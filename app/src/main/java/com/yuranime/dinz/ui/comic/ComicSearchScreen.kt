package com.yuranime.dinz.ui.comic

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.model.ComicItem
import com.yuranime.dinz.data.repository.ComicRepository
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.ComicNavTab
import com.yuranime.dinz.ui.components.liquidGlassBackdrop
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val SEARCH_IMAGE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val SEARCH_IMAGE_REFERER = "https://app.shinigami.asia/"

private val SearchScreenBg = Color(0xFF0B0D12)
private val SearchCardBg = Color(0xFF141418)
private val SearchRedAccent = Color(0xFFE11D48)
private val SearchMutedText = Color(0xFF94A3B8)
private val SearchPillInactiveBg = Color(0xFF181B22)
private val SearchPillActiveBg = Color(0xFF262A35)

private val ALL_GENRES = listOf(
    "Action", "Adaptation", "Adult", "Adventure", "Comedy", "Cooking", "Crime", "Demon", "Demons",
    "Drama", "Ecchi", "Fantasy", "Fight", "Fighting", "Game", "Gender Bender", "Harem", "Historical",
    "Horror", "Isekai", "Josei", "Latest", "Love", "Magic", "Martial Arts", "Mature", "Mecha",
    "Medical", "Murim", "Mystery", "Philosophical", "Psychological", "Regression", "Reincarnation",
    "Revenge", "Romance", "School Life", "Sci-fi", "Seinen", "Shoujo", "Shounen", "Slice of Life",
    "Smut", "Sports", "Supernatural", "Supranatural", "Thriller", "Tragedy", "Violence", "Wuxia"
)

private val FORMAT_TABS = listOf("Semua", "Manhwa", "Manhua", "Manga")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicSearchScreen(
    initialGenre: String? = null,
    onComicClick: (comicId: String) -> Unit,
    onBackClick: () -> Unit,
    selectedComicTab: ComicNavTab = ComicNavTab.CARI,
    onComicTabSelected: (ComicNavTab) -> Unit,
    modifier: Modifier = Modifier,
    repository: ComicRepository = remember { ComicRepository() }
) {
    BackHandler { onBackClick() }

    val scope = rememberCoroutineScope()
    val backdropLayer = rememberGraphicsLayer()
    val isLiquidGlass = AppPreferences.liquidGlassEnabled.collectAsState().value

    // State Filter & Pencarian
    var searchQuery by remember { mutableStateOf("") }
    var selectedFormatTab by remember { mutableStateOf("Semua") }
    var selectedGenre by remember { mutableStateOf(initialGenre.orEmpty()) }
    var selectedSort by remember { mutableStateOf("Pembaruan terbaru") } // "Pembaruan terbaru" atau "Populer"
    var selectedStatus by remember { mutableStateOf("") } // "Berlanjut", "Tamat", "Hiatus"

    var isFilterSheetVisible by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(true) }
    var searchResults by remember { mutableStateOf<List<ComicItem>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            isLoading = true
            val formatParam = if (selectedFormatTab == "Semua") null else selectedFormatTab
            val genreParam = if (selectedGenre.isBlank() || selectedGenre.equals("Semua", true)) null else selectedGenre
            val sortParam = if (selectedSort == "Populer") "popular" else "latest"
            val statusParam = when (selectedStatus) {
                "Berlanjut" -> "ongoing"
                "Tamat" -> "completed"
                else -> null
            }

            val result = repository.getFilteredComics(
                query = searchQuery.trim(),
                format = formatParam,
                genre = genreParam,
                status = statusParam,
                sort = sortParam,
                page = 1,
                pageSize = 30
            )

            searchResults = result.getOrDefault(emptyList())
            isLoading = false
        }
    }

    // Trigger pencarian saat query, format, genre, status, atau sort berubah
    LaunchedEffect(selectedFormatTab, selectedGenre, selectedSort, selectedStatus) {
        executeSearch()
    }

    LaunchedEffect(searchQuery) {
        delay(350)
        executeSearch()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SearchScreenBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdrop(backdropLayer, enabled = isLiquidGlass)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ==================== 1. HEADER & SEARCH BAR ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0xFF161922))
                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(23.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari manga, manhwa, manhua...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.5.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontFamily = SfProFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(SearchRedAccent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // ==================== 2. TAB SEGMENTASI FORMAT (Semua, Manhwa, Manhua, Manga) ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FORMAT_TABS.forEach { tab ->
                        val isSelected = tab == selectedFormatTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) SearchPillActiveBg else SearchPillInactiveBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0x59FFFFFF) else Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(19.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White)
                                ) { selectedFormatTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else SearchMutedText,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== 3. BARIS TOMBOL FILTER & QUICK GENRE PILLS ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Filter Modal
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xFF1E222B))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(17.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { isFilterSheetVisible = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Filter",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Horizontal Scrollable Genre Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ALL_GENRES) { genre ->
                            val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (isSelected) SearchRedAccent else SearchPillInactiveBg)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) SearchRedAccent else Color(0x1FFFFFFF),
                                        shape = RoundedCornerShape(17.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = Color.White)
                                    ) {
                                        selectedGenre = if (isSelected) "" else genre
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = genre,
                                    color = if (isSelected) Color.White else SearchMutedText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==================== 4. SUB-HEADER INFORMASI & PENGURUTAN ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        selectedGenre.isNotBlank() -> "Genre: $selectedGenre"
                        selectedFormatTab != "Semua" -> selectedFormatTab
                        else -> "Semua tipe"
                    }
                    Text(
                        text = statusText,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )

                    // Tombol Toggle Pengurutan
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                selectedSort = if (selectedSort == "Pembaruan terbaru") "Populer" else "Pembaruan terbaru"
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Urutkan",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedSort,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== 5. GRID 3 KOLOM KOMIK BERSIH ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SearchRedAccent, strokeWidth = 3.dp)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Komik tidak ditemukan",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Coba gunakan kata kunci atau filter genre lainnya.",
                                    color = SearchMutedText,
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 120.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(searchResults, key = { it.id.ifBlank { it.title } }) { comic ->
                                ComicGridItem(
                                    comic = comic,
                                    onClick = { onComicClick(comic.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Clear Liquid Glass Navbar (Mode Komik: Tab 2 = Cari Aktif)
        BottomNavBar(
            isComicMode = true,
            selectedComicTab = selectedComicTab,
            onComicTabSelected = onComicTabSelected,
            backdropLayer = backdropLayer,
            isLiquidGlassEnabled = isLiquidGlass,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ==================== 6. MODAL BOTTOM SHEET FILTER KOMIK ====================
        if (isFilterSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var tempSort by remember { mutableStateOf(selectedSort) }
            var tempStatus by remember { mutableStateOf(selectedStatus) }
            var tempFormat by remember { mutableStateOf(selectedFormatTab) }
            var tempGenre by remember { mutableStateOf(selectedGenre) }

            ModalBottomSheet(
                onDismissRequest = { isFilterSheetVisible = false },
                sheetState = sheetState,
                containerColor = Color(0xFF14171E),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Header Filter & Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )

                        Text(
                            text = "Reset",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    tempSort = "Pembaruan terbaru"
                                    tempStatus = ""
                                    tempFormat = "Semua"
                                    tempGenre = ""
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Section 1: Urutkan
                        item {
                            FilterSectionTitle("Urutkan")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChipPill(
                                    text = "Terkini",
                                    isSelected = tempSort == "Pembaruan terbaru",
                                    onClick = { tempSort = "Pembaruan terbaru" }
                                )
                                FilterChipPill(
                                    text = "Populer",
                                    isSelected = tempSort == "Populer",
                                    onClick = { tempSort = "Populer" }
                                )
                            }
                        }

                        // Section 2: Status
                        item {
                            FilterSectionTitle("Status")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Berlanjut", "Tamat", "Hiatus").forEach { st ->
                                    FilterChipPill(
                                        text = st,
                                        isSelected = tempStatus == st,
                                        onClick = { tempStatus = if (tempStatus == st) "" else st }
                                    )
                                }
                            }
                        }

                        // Section 3: Format
                        item {
                            FilterSectionTitle("Format")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Manhwa", "Manhua", "Manga").forEach { fmt ->
                                    FilterChipPill(
                                        text = fmt,
                                        isSelected = tempFormat == fmt,
                                        onClick = { tempFormat = if (tempFormat == fmt) "Semua" else fmt }
                                    )
                                }
                            }
                        }

                        // Section 4: Genre
                        item {
                            FilterSectionTitle("Genre")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Judul dengan salah satu genre pilihanmu ditampilkan.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ALL_GENRES.forEach { g ->
                                    FilterChipPill(
                                        text = g,
                                        isSelected = tempGenre.equals(g, ignoreCase = true),
                                        onClick = { tempGenre = if (tempGenre.equals(g, ignoreCase = true)) "" else g }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Bar (Batal & Tampilkan hasil)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tombol Batal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF1E222B))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp))
                                .clickable { isFilterSheetVisible = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Batal",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        // Tombol Tampilkan Hasil
                        Box(
                            modifier = Modifier
                                .weight(1.8f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color.White)
                                .clickable {
                                    selectedSort = tempSort
                                    selectedStatus = tempStatus
                                    selectedFormatTab = tempFormat
                                    selectedGenre = tempGenre
                                    isFilterSheetVisible = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tampilkan hasil",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== KOMPONEN KARTU GRID 3 KOLOM ====================

@Composable
private fun ComicGridItem(
    comic: ComicItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chText = remember(comic.chapters) {
        val firstCh = comic.chapters.firstOrNull()?.number?.trim().orEmpty()
        if (firstCh.isNotBlank()) {
            if (firstCh.startsWith("ch", ignoreCase = true)) firstCh else "Ch. $firstCh"
        } else {
            "Ch. 1"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 70.dp, color = Color.White)
            ) { onClick() }
    ) {
        // Poster Komik Murni (Rasio 3:4, Tanpa Badge Tipe/BARU di atas gambar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(SearchCardBg)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(comic.coverPortrait.ifBlank { comic.coverLandscape })
                    .crossfade(true)
                    .addHeader("User-Agent", SEARCH_IMAGE_USER_AGENT)
                    .addHeader("Referer", SEARCH_IMAGE_REFERER)
                    .build(),
                contentDescription = comic.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Judul Komik: SF Bold putih 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
        Text(
            text = comic.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Baris Bawah: Nomor Chapter di Kiri & Bendera Negara di Kanan secara Sejajar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chText,
                color = SearchMutedText,
                fontSize = 11.sp,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Ikon Bendera Negara
            AsyncImage(
                model = comic.flagUrl,
                contentDescription = comic.format,
                modifier = Modifier
                    .size(width = 16.dp, height = 11.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = SfProFontFamily
    )
}

@Composable
private fun FilterChipPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (isSelected) SearchPillActiveBg else SearchPillInactiveBg)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.White else Color(0x1FFFFFFF),
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White)
            ) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else SearchMutedText,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
    }
}
