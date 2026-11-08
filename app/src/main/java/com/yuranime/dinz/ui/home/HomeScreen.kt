package com.yuranime.dinz.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.HomeSliderMode
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.ui.components.AnimeVerticalCard
import com.yuranime.dinz.ui.components.AnimeWideCard
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.CoachmarkStep
import com.yuranime.dinz.ui.components.ContinueWatchingCard
import com.yuranime.dinz.ui.components.CuplixAvatarItem
import com.yuranime.dinz.ui.components.HomeErrorState
import com.yuranime.dinz.ui.components.HomeShimmerLoading
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.PopularLeaderboardCard
import com.yuranime.dinz.ui.components.SectionHeader
import com.yuranime.dinz.ui.components.SpotlightCoachmarkOverlay
import com.yuranime.dinz.ui.components.YuranimeAnimatedLogo
import com.yuranime.dinz.ui.components.YuranimeHeroSlider
import com.yuranime.dinz.ui.components.onboardingTarget
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorMetricFavorites
import com.yuranime.dinz.ui.theme.ColorMetricViews
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    selectedTab: NavTab = NavTab.HOME,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit = { _, _ -> },
    onCuplixClick: (CuplixItem) -> Unit = {},
    onBirthdayClick: (com.yuranime.dinz.data.api.BirthdayCharacterItem) -> Unit = {},
    onComicClick: (comicId: String) -> Unit = {},
    onComicChapterClick: (chapterId: String) -> Unit = {},
    onOpenAllComics: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sliderMode by AppPreferences.sliderMode.collectAsState()
    val context = LocalContext.current

    // Refresh watch history from local database
    LaunchedEffect(Unit) {
        viewModel.refreshWatchHistory(context)
    }

    // LazyListStates for horizontal infinite scroll
    val newEpisodesListState = rememberLazyListState()
    val hotListState = rememberLazyListState()
    val newTitlesListState = rememberLazyListState()
    val jatForYuListState = rememberLazyListState()
    val waitingListState = rememberLazyListState()
    val popularListState = rememberLazyListState()

    // Horizontal scroll listeners (trigger when nearing 3 items from the end)
    ObserveHorizontalPagination(newEpisodesListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.NEW_EPISODES)
    }
    ObserveHorizontalPagination(hotListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.HOT)
    }
    ObserveHorizontalPagination(newTitlesListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.NEW_TITLES)
    }
    ObserveHorizontalPagination(jatForYuListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.JAT_FOR_YU)
    }
    ObserveHorizontalPagination(waitingListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.WAITING)
    }
    ObserveHorizontalPagination(popularListState) {
        viewModel.loadMoreHorizontal(HomeSectionType.POPULAR)
    }

    // Sub-Page: Active Full Vertical List for Chevron '>'
    if (uiState.activeSectionDetail != null) {
        BackHandler {
            viewModel.closeSectionDetail()
        }
        HomeSectionDetailSubPage(
            detailState = uiState.activeSectionDetail!!,
            onBackClick = { viewModel.closeSectionDetail() },
            onAnimeClick = onAnimeClick,
            onLoadMore = { viewModel.loadMoreSectionDetail() }
        )
        return
    }

    val targetBounds = remember { mutableStateMapOf<String, Rect>() }
    val coroutineScope = rememberCoroutineScope()
    val homeLazyListState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackgroundPrimary,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.onboardingTarget("home_navbar", targetBounds)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(ColorBackgroundPrimary)
        ) {
            when {
                uiState.isLoading && uiState.newEpisodesList.isEmpty() -> {
                    HomeShimmerLoading()
                }

                uiState.errorMessage != null && uiState.newEpisodesList.isEmpty() -> {
                    HomeErrorState(
                        errorMessage = uiState.errorMessage ?: "",
                        onRetry = { viewModel.loadHomeData() }
                    )
                }

                else -> {
                    LazyColumn(
                        state = homeLazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // 1. Top Hero Slider Gaya Yuranime (Data dari Jadwal Hari Ini)
                        item(key = "piupiu_hero_slider") {
                            val sliderAnimeList = uiState.todayScheduleList.ifEmpty { uiState.newEpisodesList }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onboardingTarget("home_slider", targetBounds)
                            ) {
                                YuranimeHeroSlider(
                                    items = sliderAnimeList,
                                    sliderMode = sliderMode,
                                    onAnimeClick = onAnimeClick
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    YuranimeAnimatedLogo()

                                    if (sliderMode == HomeSliderMode.CLEAN_COVER) {
                                        // Ikon Pencarian Bersih di Pojok Kanan Atas
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33000000))
                                                .clickable { onTabSelected(NavTab.CARI) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Cari Anime",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Section "Lanjut Nonton" (Hidden if history is empty)
                        if (uiState.continueWatchingList.isNotEmpty()) {
                            item(key = "section_continue_watching") {
                                Spacer(modifier = Modifier.height(10.dp))
                                SectionHeader(
                                    title = "Lanjut Nonton",
                                    showChevron = false
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.continueWatchingList,
                                        key = { it.id }
                                    ) { item ->
                                        ContinueWatchingCard(
                                            item = item,
                                            onClick = {
                                                if (item.episodeId.isNotBlank()) {
                                                    onEpisodeClick(item.episodeId, item.idMovie)
                                                } else {
                                                    onAnimeClick(item.idMovie)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 2.5 Section "Ulang Tahun Hari Ini" (Di atas cuplix, di bawah slider / lanjut nonton)
                        item(key = "section_birthday_today") {
                            Spacer(modifier = Modifier.height(14.dp))
                            com.yuranime.dinz.ui.components.BirthdayTodaySection(
                                birthdayData = uiState.birthdayToday,
                                isLoading = uiState.isBirthdayLoading,
                                errorMessage = uiState.birthdayError,
                                onRetry = { viewModel.loadBirthdayToday(forceRefresh = true) },
                                onItemClick = onBirthdayClick
                            )
                        }

                        // 3. Section "Cuplix" (Horizontal row of circular items)
                        if (uiState.cuplixList.isNotEmpty()) {
                            item(key = "section_cuplix") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onboardingTarget("home_cuplix", targetBounds)
                                ) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    SectionHeader(
                                        title = "Cuplix",
                                        showChevron = false
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(
                                            items = uiState.cuplixList,
                                            key = { it.id ?: it.hashCode().toString() }
                                        ) { cuplixItem ->
                                            CuplixAvatarItem(
                                                item = cuplixItem,
                                                onClick = {
                                                    onCuplixClick(cuplixItem)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Section "Episode Baru" (2:3 Vertical Cards)
                        if (uiState.newEpisodesList.isNotEmpty()) {
                            item(key = "section_new_episodes") {
                                Spacer(modifier = Modifier.height(14.dp))
                                SectionHeader(
                                    title = "Episode Baru",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.NEW_EPISODES) }
                                )
                                LazyRow(
                                    state = newEpisodesListState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.newEpisodesList,
                                        key = { "new_ep_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = anime.index?.let { "Episode $it" } ?: "Episode 1",
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Section "Sedang Hangat" (16:9 Landscape Wide Cards)
                        if (uiState.hotAnimeList.isNotEmpty()) {
                            item(key = "section_hot_anime") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Sedang Hangat",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.HOT) }
                                )
                                LazyRow(
                                    state = hotListState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.hotAnimeList,
                                        key = { "hot_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeWideCard(
                                            anime = anime,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Section Komik Rilisan Terbaru (Menggantikan Judul Baru)
                        if (uiState.comicsList.isNotEmpty()) {
                            item(key = "section_comics") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onboardingTarget("home_komik", targetBounds)
                                ) {
                                    Spacer(modifier = Modifier.height(18.dp))
                                    SectionHeader(
                                        title = "Komik Terbaru",
                                        onChevronClick = onOpenAllComics
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ComicLatestSection(
                                        comics = uiState.comicsList,
                                        onComicClick = onComicClick,
                                        onChapterClick = onComicChapterClick
                                    )
                                }
                            }
                        }

                        // 7. Section "Jadwal Hari Ini" (2:3 Vertical Cards + Purple Airing Time)
                        if (uiState.todayScheduleList.isNotEmpty()) {
                            item(key = "section_today_schedule") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Jadwal Hari Ini",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.TODAY) }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.todayScheduleList,
                                        key = { "today_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        val singleGenre = anime.getDisplayGenre().split(",").firstOrNull()?.trim() ?: "Action"
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = singleGenre,
                                            showAiringTime = true,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 8. Section "Jat For Yu" (16:9 Landscape Wide Cards)
                        if (uiState.jatForYuList.isNotEmpty()) {
                            item(key = "section_jat_for_yu") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Jat For Yu",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.JAT_FOR_YU) }
                                )
                                LazyRow(
                                    state = jatForYuListState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = uiState.jatForYuList,
                                        key = { "jat_for_yu_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        AnimeWideCard(
                                            anime = anime,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 9. Section "Paling Dinanti" (2:3 Vertical Cards + Blue Release Date)
                        if (uiState.waitingAnimeList.isNotEmpty()) {
                            item(key = "section_waiting_anime") {
                                Spacer(modifier = Modifier.height(18.dp))
                                SectionHeader(
                                    title = "Paling Dinanti",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.WAITING) }
                                )
                                LazyRow(
                                    state = waitingListState,
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        items = uiState.waitingAnimeList,
                                        key = { "waiting_" + (it.id ?: it.hashCode().toString()) }
                                    ) { anime ->
                                        val singleGenre = anime.getDisplayGenre().split(",").firstOrNull()?.trim() ?: "Action"
                                        AnimeVerticalCard(
                                            anime = anime,
                                            overrideSubtitle = singleGenre,
                                            showReleaseDate = true,
                                            onClick = { anime.id?.let { onAnimeClick(it) } }
                                        )
                                    }
                                }
                            }
                        }

                        // 10. Section "Paling Populer" (Leaderboard Ranking Cards sesuai referensi)
                        if (uiState.popularAnimeList.isNotEmpty()) {
                            item(key = "section_popular_header") {
                                Spacer(modifier = Modifier.height(20.dp))
                                SectionHeader(
                                    title = "Paling Populer",
                                    onChevronClick = { viewModel.openSectionDetail(HomeSectionType.POPULAR) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            val topRankItems = uiState.popularAnimeList.take(10)
                            itemsIndexed(
                                items = topRankItems,
                                key = { index, anime -> "pop_rank_${index + 1}_${anime.id ?: anime.hashCode()}" }
                            ) { index, anime ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    PopularLeaderboardCard(
                                        anime = anime,
                                        rank = index + 1,
                                        onClick = { anime.id?.let { onAnimeClick(it) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Spotlight Coachmark Onboarding for HomeScreen
            var showHomeOnboarding by remember {
                mutableStateOf(!AppPreferences.isHomeOnboardingCompleted(context))
            }
            val homeSteps = remember {
                listOf(
                    CoachmarkStep(
                        id = "home_slider",
                        title = "Hero Slider & Tonton Sekarang",
                        description = "Temukan anime terbaru dan terpopuler pilihan hari ini. Tekan tombol Tonton Sekarang untuk langsung mulai streaming tanpa jeda.",
                        cornerRadiusDp = 16f,
                        scrollToIndex = 0
                    ),
                    CoachmarkStep(
                        id = "home_cuplix",
                        title = "Cuplix (Video Pendek Anime)",
                        description = "Tonton cuplikan momen seru, adegan lucu, dan pertarungan epik anime favorit dalam format video pendek vertikal yang adiktif.",
                        cornerRadiusDp = 14f,
                        scrollToIndex = 2
                    ),
                    CoachmarkStep(
                        id = "home_komik",
                        title = "Komik Rilisan Terbaru",
                        description = "Baca update chapter terbaru dari ratusan judul Manga, Manhwa, dan Manhua terpopuler dengan bendera asal negara dan rilis waktu nyata.",
                        cornerRadiusDp = 14f,
                        scrollToIndex = 5
                    ),
                    CoachmarkStep(
                        id = "home_navbar",
                        title = "Menu Navigasi Cepat",
                        description = "Jelajahi jadwal rilis mingguan, cari genre & studio anime, ikuti chat komunitas, atau kelola unduhan & poin di profil kamu.",
                        cornerRadiusDp = 24f
                    )
                )
            }

            SpotlightCoachmarkOverlay(
                steps = homeSteps,
                isVisible = showHomeOnboarding && uiState.activeSectionDetail == null,
                onDismiss = {
                    showHomeOnboarding = false
                    AppPreferences.setHomeOnboardingCompleted(context, true)
                },
                targetBounds = targetBounds,
                onStepChanged = { step ->
                    step.scrollToIndex?.let { targetIdx ->
                        coroutineScope.launch {
                            homeLazyListState.animateScrollToItem(targetIdx)
                            kotlinx.coroutines.delay(150)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ObserveHorizontalPagination(
    listState: LazyListState,
    onNearEnd: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 3 && totalItems > 0
        }.collect { nearEnd ->
            if (nearEnd) {
                onNearEnd()
            }
        }
    }
}

@Composable
private fun HomeSectionDetailSubPage(
    detailState: HomeSectionDetailState,
    onBackClick: () -> Unit,
    onAnimeClick: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 3 && totalItems > 0
        }.collect { nearEnd ->
            if (nearEnd) {
                onLoadMore()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = detailState.sectionType.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Full Vertical List
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = detailState.items,
                key = { "detail_" + (it.id ?: it.hashCode().toString()) }
            ) { anime ->
                SectionAnimeCard(
                    anime = anime,
                    onClick = { anime.id?.let { onAnimeClick(it) } }
                )
            }

            if (detailState.isLoadingMore) {
                item(key = "detail_loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ColorAccentPrimary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionAnimeCard(
    anime: AnimeItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Poster (2:3 aspect ratio, width 115dp, rounded 10dp)
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

        // Info Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
        ) {
            // Genre (Orange #FF5722)
            Text(
                text = anime.getDisplayGenre(),
                color = ColorAccentPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Title (White Bold)
            Text(
                text = anime.title ?: "-",
                color = ColorTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Synopsis
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

            // Views & Favorites
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Views
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

                // Favorites
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ColorMetricFavorites,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${anime.favorites ?: "0"} favs",
                        color = ColorMetricFavorites,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
