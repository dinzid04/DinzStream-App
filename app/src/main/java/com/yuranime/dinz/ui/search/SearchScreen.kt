package com.yuranime.dinz.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.ExploreCharacterItem
import com.yuranime.dinz.data.model.ExploreGenreItem
import com.yuranime.dinz.data.model.ExploreYearItem
import com.yuranime.dinz.ui.components.AnimeVerticalCard
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.CuplixAvatarItem
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorMetricFavorites
import com.yuranime.dinz.ui.theme.ColorMetricViews
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily

private val StudioList = listOf("MAPPA", "Ufotable", "Kyoto Animation", "Bones", "A-1 Pictures", "Toei Animation", "Madhouse", "Wit Studio", "CloverWorks")
private val TypeList = listOf("MOVIE", "ONA", "OVA", "LIVE ACTION", "SPECIAL", "TV")

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
    selectedTab: NavTab = NavTab.CARI,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onCuplixClick: (CuplixItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = uiState.selectedSubPage != null || uiState.isSearchActive) {
        if (uiState.selectedSubPage != null) {
            viewModel.closeSubPage()
        } else {
            viewModel.clearSearch()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackgroundPrimary,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ColorBackgroundPrimary)
        ) {
            if (uiState.selectedSubPage != null) {
                // Sub-Page Full List for KATEGORI, TAHUN, KARAKTER
                ExploreSubPageContent(
                    subPage = uiState.selectedSubPage!!,
                    uiState = uiState,
                    onClose = { viewModel.closeSubPage() },
                    onGenreClick = { id, name ->
                        viewModel.applyFilter(FilterType.GENRE, id, name)
                        viewModel.closeSubPage()
                    },
                    onYearClick = { year ->
                        viewModel.applyFilter(FilterType.YEAR, year, year)
                        viewModel.closeSubPage()
                    },
                    onCharacterClick = { charName ->
                        viewModel.applyFilter(FilterType.CHARACTER, charName, charName)
                        viewModel.closeSubPage()
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isSearchActive) {
                            IconButton(
                                onClick = { viewModel.clearSearch() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = ColorTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Pill input box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF161922))
                                .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(22.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Cari",
                                    tint = ColorTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = uiState.query,
                                    onValueChange = { viewModel.onQueryChange(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = ColorTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(ColorAccentPrimary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        focusManager.clearFocus()
                                        viewModel.executeSearch()
                                    }),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (uiState.query.isEmpty()) {
                                            Text(
                                                text = "Cari Anime..",
                                                color = ColorTextSecondary,
                                                fontSize = 13.5.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onQueryChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Hapus",
                                            tint = ColorTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isSearchActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.executeSearch()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim",
                                    tint = ColorAccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Main Content: Default Explore Mode vs Active Search Mode
                    if (!uiState.isSearchActive) {
                        PullToRefreshBox(
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refreshExploreData() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ExploreDefaultContent(
                                genres = uiState.genres,
                                years = uiState.years,
                                characters = uiState.characters,
                                onOpenSubPage = { viewModel.openSubPage(it) },
                                onGenreClick = { id, name -> viewModel.applyFilter(FilterType.GENRE, id, name) },
                                onStudioClick = { studio -> viewModel.applyFilter(FilterType.STUDIO, studio, studio) },
                                onYearClick = { year -> viewModel.applyFilter(FilterType.YEAR, year, year) },
                                onTypeClick = { type -> viewModel.applyFilter(FilterType.TYPE, type, type) },
                                onCharacterClick = { charName -> viewModel.applyFilter(FilterType.CHARACTER, charName, charName) }
                            )
                        }
                    } else {
                        SearchResultsContent(
                            uiState = uiState,
                            onAnimeClick = onAnimeClick,
                            onCuplixClick = onCuplixClick,
                            onClearFilter = { viewModel.clearFilter() },
                            onLoadNextPage = { viewModel.loadNextPage() }
                        )
                    }
                }
            }

            // Floating Buttons in Active Search Mode (Sort with Green Arrow + Layout Toggle)
            val hasItems = if (uiState.activeFilter != null || uiState.query.isNotBlank()) {
                uiState.searchResults.isNotEmpty()
            } else {
                uiState.recommendations.isNotEmpty()
            }
            if (uiState.isSearchActive && hasItems && uiState.selectedSubPage == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sort order button with Emerald Green Arrow
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xE61E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { viewModel.cycleSort() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Urutkan",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Layout toggle button (Grid vs List)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xE61E293B))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { viewModel.toggleLayout() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Ganti Tampilan",
                            tint = ColorAccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 1. DEFAULT EXPLORE CONTENT ====================

@Composable
private fun ExploreDefaultContent(
    genres: List<ExploreGenreItem>,
    years: List<ExploreYearItem>,
    characters: List<ExploreCharacterItem>,
    onOpenSubPage: (ExploreSubPage) -> Unit,
    onGenreClick: (id: String, name: String) -> Unit,
    onStudioClick: (String) -> Unit,
    onYearClick: (String) -> Unit,
    onTypeClick: (String) -> Unit,
    onCharacterClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. KATEGORI (White Cards with Right Illustration)
        item {
            ExploreHeaderRow(
                title = "KATEGORI",
                onClick = { onOpenSubPage(ExploreSubPage.KATEGORI) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val sampleGenres = if (genres.isNotEmpty()) genres.take(3) else listOf(
                    ExploreGenreItem("1", "Magic", "", "Theme"),
                    ExploreGenreItem("2", "Music", "", "Genre"),
                    ExploreGenreItem("3", "Shoujo", "", "Demographic")
                )
                sampleGenres.forEach { g ->
                    WhiteCategoryCard(
                        group = g.group ?: "Genre",
                        name = g.name ?: "-",
                        imageUrl = g.getEffectiveImage(),
                        onClick = { onGenreClick(g.id ?: g.name ?: "", g.name ?: "-") }
                    )
                }
            }
        }

        // 2. STUDIO (Dark Pills)
        item {
            ExploreHeaderRow(title = "STUDIO", showChevron = false)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(StudioList) { studio ->
                    DarkPillChip(label = studio, onClick = { onStudioClick(studio) })
                }
            }
        }

        // 3. TAHUN (White Cards with Left Illustration)
        item {
            ExploreHeaderRow(
                title = "TAHUN",
                onClick = { onOpenSubPage(ExploreSubPage.TAHUN) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val sampleYears = if (years.isNotEmpty()) years.take(3) else listOf(
                    ExploreYearItem("2019", ""),
                    ExploreYearItem("2024", ""),
                    ExploreYearItem("2025", "")
                )
                sampleYears.forEach { y ->
                    WhiteYearCard(
                        year = y.name ?: "-",
                        imageUrl = y.getEffectiveImage(),
                        onClick = { y.name?.let { onYearClick(it) } }
                    )
                }
            }
        }

        // 4. TIPE (Dark Pills)
        item {
            ExploreHeaderRow(title = "TIPE", showChevron = false)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(TypeList) { type ->
                    DarkPillChip(label = type, onClick = { onTypeClick(type) })
                }
            }
        }

        // 5. KARAKTER (Dark Blue-Gray Cards)
        item {
            ExploreHeaderRow(
                title = "KARAKTER",
                onClick = { onOpenSubPage(ExploreSubPage.KARAKTER) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val sampleChars = if (characters.isNotEmpty()) characters.take(3) else listOf(
                    ExploreCharacterItem("1", "Kurisu", image = ""),
                    ExploreCharacterItem("2", "Amane", image = ""),
                    ExploreCharacterItem("3", "Shiori", image = "")
                )
                sampleChars.forEach { c ->
                    DarkCharacterCard(
                        name = c.name ?: "-",
                        imageUrl = c.getEffectiveImage(),
                        onClick = { c.name?.let { onCharacterClick(it) } }
                    )
                }
            }
        }
    }
}

// ==================== 2. ACTIVE SEARCH RESULTS CONTENT ====================
// Note: NO genre capsules above Cuplix per specification!
// Cuplix is directly under search bar, followed by Recommendations / Search Results.

@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    onAnimeClick: (String) -> Unit,
    onCuplixClick: (CuplixItem) -> Unit,
    onClearFilter: () -> Unit,
    onLoadNextPage: () -> Unit
) {
    val isFilterActive = uiState.activeFilter != null
    val isQueryActive = uiState.query.isNotBlank()
    val displayedList = if (isFilterActive || isQueryActive) {
        uiState.searchResults
    } else {
        uiState.recommendations
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val shouldLoadMoreGrid = remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    val shouldLoadMoreList = remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMoreGrid.value) {
        if (shouldLoadMoreGrid.value && uiState.canPaginate && !uiState.isLoading && !uiState.isPaginating) {
            onLoadNextPage()
        }
    }

    LaunchedEffect(shouldLoadMoreList.value) {
        if (shouldLoadMoreList.value && uiState.canPaginate && !uiState.isLoading && !uiState.isPaginating) {
            onLoadNextPage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cuplix Bar directly under the search bar
        if (uiState.cuplixList.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.cuplixList, key = { it.id ?: it.hashCode().toString() }) { cuplixItem ->
                    CuplixAvatarItem(
                        item = cuplixItem,
                        onClick = { onCuplixClick(cuplixItem) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Active Filter Banner / Chip (Emerald accent, clean clear button)
        if (uiState.activeFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, ColorAccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearFilter)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val typeLabel = when (uiState.activeFilter.type) {
                        FilterType.GENRE -> "Kategori"
                        FilterType.STUDIO -> "Studio"
                        FilterType.YEAR -> "Tahun"
                        FilterType.TYPE -> "Tipe"
                        FilterType.CHARACTER -> "Karakter"
                    }
                    Text(
                        text = "$typeLabel: ${uiState.activeFilter.label}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus Filter",
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = "${displayedList.size} anime",
                    color = ColorTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Section Title: "Hasil Filter", "Hasil Pencarian", or "Rekomendasi Anime Populer"
        val headerTitle = when {
            uiState.activeFilter != null -> "Hasil Filter \"${uiState.activeFilter.label}\""
            uiState.query.isNotBlank() -> "Hasil Pencarian \"${uiState.query}\""
            else -> "Rekomendasi Anime Populer"
        }
        Text(
            text = headerTitle,
            color = ColorTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Results List or Grid
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && displayedList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorAccentPrimary, modifier = Modifier.size(36.dp))
                    }
                }

                displayedList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.activeFilter != null || uiState.query.isNotBlank()) {
                                "Tidak ada anime ditemukan."
                            } else {
                                "Belum ada rekomendasi."
                            },
                            color = ColorTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                uiState.isGridLayout -> {
                    // Grid 3 Columns
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(displayedList, key = { it.id ?: it.hashCode().toString() }) { anime ->
                            AnimeVerticalCard(
                                anime = anime,
                                onClick = { anime.id?.let { onAnimeClick(it) } }
                            )
                        }
                        if (uiState.isPaginating) {
                            item(span = { GridItemSpan(3) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ColorAccentPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Wide Cards List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayedList, key = { it.id ?: it.hashCode().toString() }) { anime ->
                            SearchResultWideCard(
                                anime = anime,
                                onClick = { anime.id?.let { onAnimeClick(it) } }
                            )
                        }
                        if (uiState.isPaginating) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ColorAccentPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
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

// ==================== 2B. SUB-PAGE FULL LIST CONTENT ====================

@Composable
private fun ExploreSubPageContent(
    subPage: ExploreSubPage,
    uiState: SearchUiState,
    onClose: () -> Unit,
    onGenreClick: (id: String, name: String) -> Unit,
    onYearClick: (String) -> Unit,
    onCharacterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        // Sub-page Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = ColorTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val title = when (subPage) {
                ExploreSubPage.KATEGORI -> "KATEGORI"
                ExploreSubPage.TAHUN -> "TAHUN"
                ExploreSubPage.KARAKTER -> "KARAKTER"
            }
            Text(
                text = title,
                color = ColorTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (subPage) {
                ExploreSubPage.KATEGORI -> {
                    items(uiState.genres) { g ->
                        WhiteCategoryCard(
                            group = g.group ?: "Genre",
                            name = g.name ?: "-",
                            imageUrl = g.getEffectiveImage(),
                            onClick = { onGenreClick(g.id ?: g.name ?: "", g.name ?: "-") }
                        )
                    }
                }
                ExploreSubPage.TAHUN -> {
                    items(uiState.years) { y ->
                        WhiteYearCard(
                            year = y.name ?: "-",
                            imageUrl = y.getEffectiveImage(),
                            onClick = {
                                y.name?.let {
                                    onYearClick(it)
                                }
                            }
                        )
                    }
                }
                ExploreSubPage.KARAKTER -> {
                    items(uiState.characters) { c ->
                        DarkCharacterCard(
                            name = c.name ?: "-",
                            imageUrl = c.getEffectiveImage(),
                            onClick = {
                                c.name?.let {
                                    onCharacterClick(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== 3. COMPONENT HELPERS ====================

@Composable
private fun ExploreHeaderRow(
    title: String,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ColorTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        if (showChevron) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2430)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WhiteCategoryCard(
    group: String,
    name: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(114.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Full background image from API (TopCenter to prevent cropping character heads)
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime_wide)
                    .error(R.drawable.placeholder_yuranime_wide)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Text on top (Left aligned)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
        ) {
            Text(
                text = group,
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                color = Color(0xFF222222),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun WhiteYearCard(
    year: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(114.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Full background image from API (TopCenter alignment)
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime_wide)
                    .error(R.drawable.placeholder_yuranime_wide)
                    .build(),
                contentDescription = year,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // Text on top (Right aligned)
        Text(
            text = year,
            color = Color(0xFF222222),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        )
    }
}

@Composable
private fun DarkCharacterCard(
    name: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    val cardBg = Color(0xFF18202C)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(114.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
    ) {
        // Character image on right with horizontal gradient fade mask
        if (imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(165.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient mask overlay: solid #18202C on left to transparent on right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    cardBg,
                                    cardBg.copy(alpha = 0.85f),
                                    cardBg.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Name on left
        Text(
            text = name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
        )
    }
}

@Composable
private fun DarkPillChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF18202C))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchResultWideCard(
    anime: AnimeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(anime.getEffectivePoster())
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime)
                .error(R.drawable.placeholder_yuranime)
                .build(),
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(115.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141720))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = anime.getDisplayGenre(),
                color = ColorAccentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = anime.title ?: "-",
                color = ColorTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (!anime.synopsis.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = anime.synopsis,
                    color = ColorTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = ColorMetricViews,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${anime.views ?: "0"} views",
                        color = ColorMetricViews,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ColorMetricFavorites,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${anime.favorites ?: "0"} favorites",
                        color = ColorMetricFavorites,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
