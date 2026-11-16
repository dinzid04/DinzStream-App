package com.yuranime.dinz

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.update.AppUpdateInfo
import com.yuranime.dinz.data.update.UpdateChecker
import com.yuranime.dinz.data.update.UpdateDownloadManager
import com.yuranime.dinz.data.update.UpdateDownloadState
import com.yuranime.dinz.ui.comic.ComicDetailScreen
import com.yuranime.dinz.ui.comic.ComicHomeScreen
import com.yuranime.dinz.ui.chat.GlobalChatScreen
import com.yuranime.dinz.ui.comic.ComicReaderScreen
import com.yuranime.dinz.ui.comic.ComicSearchScreen
import com.yuranime.dinz.ui.comic.ComicViewAllScreen
import com.yuranime.dinz.ui.components.ComicNavTab
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.PredictiveBackItemOverlay
import com.yuranime.dinz.ui.components.PredictiveBackVisibilityOverlay
import com.yuranime.dinz.ui.cuplix.CuplixViewScreen
import com.yuranime.dinz.ui.detail.AnimeDetailScreen
import com.yuranime.dinz.ui.home.HomeScreen
import com.yuranime.dinz.ui.home.HomeViewModel
import com.yuranime.dinz.ui.jadwal.JadwalScreen
import com.yuranime.dinz.ui.jadwal.JadwalViewModel
import com.yuranime.dinz.ui.profile.LoginProfileScreen
import com.yuranime.dinz.ui.search.SearchScreen
import com.yuranime.dinz.ui.search.SearchViewModel
import com.yuranime.dinz.ui.splash.YuranimeSplashScreen
import com.yuranime.dinz.ui.streaming.StreamingScreen
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.YuranimeTheme
import com.yuranime.dinz.ui.update.UpdateMiniProgressBanner
import com.yuranime.dinz.ui.update.UpdateNoticeDialog
import com.yuranime.dinz.ui.components.PromoPopupDialog
import androidx.compose.runtime.DisposableEffect
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.PromoPopupConfig
import com.yuranime.dinz.data.local.AppPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            YuranimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBackgroundPrimary
                ) {
                    // Preload Home, Jadwal, dan Search data in the background while the motion splash screen runs
                    val homeViewModel: HomeViewModel = viewModel()
                    val jadwalViewModel: JadwalViewModel = viewModel()
                    val searchViewModel: SearchViewModel = viewModel()
                    val scope = rememberCoroutineScope()
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    var selectedAnimeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingEpisodeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingMovieId by remember { mutableStateOf<String?>(null) }
                    var selectedCuplixItem by remember { mutableStateOf<CuplixItem?>(null) }
                    var selectedBirthdayItem by remember { mutableStateOf<com.yuranime.dinz.data.api.BirthdayCharacterItem?>(null) }
                    var selectedComicChapterId by remember { mutableStateOf<String?>(null) }
                    var selectedComicId by remember { mutableStateOf<String?>(null) }
                    var selectedComicTitle by remember { mutableStateOf<String?>(null) }
                    var selectedComicPosterUrl by remember { mutableStateOf<String?>(null) }
                    var showAllComics by remember { mutableStateOf(false) }
                    var currentTab by remember { mutableStateOf(NavTab.HOME) }
                    var isComicMode by rememberSaveable { mutableStateOf(false) }
                    var selectedComicTab by rememberSaveable { mutableStateOf(ComicNavTab.HOME) }
                    var chatReturnTab by remember { mutableStateOf(NavTab.HOME) }
                    var chatReturnComicMode by rememberSaveable { mutableStateOf(false) }
                    var chatReturnComicTab by rememberSaveable { mutableStateOf(ComicNavTab.HOME) }
                    var activeComicGenreFilter by remember { mutableStateOf<String?>(null) }

                    fun openGlobalChat() {
                        if (currentTab != NavTab.CHAT) {
                            chatReturnTab = currentTab
                            chatReturnComicMode = isComicMode
                            chatReturnComicTab = selectedComicTab
                        }
                        isComicMode = false
                        currentTab = NavTab.CHAT
                    }

                    fun closeGlobalChat() {
                        isComicMode = chatReturnComicMode
                        selectedComicTab = chatReturnComicTab
                        currentTab = if (chatReturnTab == NavTab.CHAT) NavTab.HOME else chatReturnTab
                    }
                    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

                    // Real-time Promo Popup State
                    val promoConfig by FirebaseManager.promoPopupConfig.collectAsStateWithLifecycle()
                    var activePromoToShow by remember { mutableStateOf<PromoPopupConfig?>(null) }

                    LaunchedEffect(promoConfig, showSplash) {
                        if (!showSplash) {
                            val cfg = promoConfig
                            if (cfg != null && AppPreferences.shouldShowPromoPopup(this@MainActivity, cfg)) {
                                activePromoToShow = cfg
                            }
                        }
                    }

                    // Pengecekan update otomatis saat startup dinonaktifkan sementara untuk rilis bertahap ini
                    // agar pengguna terinstall tidak terus mendapat popup pembaruan di awal buka aplikasi.
                    // Pengecekan manual lewat Profil -> Periksa Pembaruan tetap tersedia.

                    val isChatOpen = !isComicMode && currentTab == NavTab.CHAT
                    val showComicBase = if (isChatOpen) chatReturnComicMode else isComicMode
                    val activeBaseTab = if (isChatOpen) {
                        if (chatReturnTab == NavTab.CHAT) NavTab.HOME else chatReturnTab
                    } else {
                        currentTab
                    }
                    val activeComicBaseTab = if (isChatOpen) chatReturnComicTab else selectedComicTab

                    val topmostOverlay = when {
                        selectedComicChapterId != null -> "COMIC_READER"
                        selectedComicId != null -> "COMIC_DETAIL"
                        showAllComics -> "COMIC_VIEW_ALL"
                        selectedBirthdayItem != null -> "BIRTHDAY_DETAIL"
                        selectedCuplixItem != null -> "CUPLIX_VIEW"
                        selectedStreamingEpisodeId != null -> "STREAMING"
                        selectedAnimeId != null -> "ANIME_DETAIL"
                        isChatOpen -> "GLOBAL_CHAT"
                        else -> null
                    }

                    val onAnimeTabSelected: (NavTab) -> Unit = { tab ->
                        if (tab == NavTab.CHAT) {
                            openGlobalChat()
                        } else if (tab == NavTab.COMIC) {
                            isComicMode = true
                            selectedComicTab = ComicNavTab.HOME
                        } else {
                            currentTab = tab
                        }
                    }

                    val onComicTabSelected: (ComicNavTab) -> Unit = { tab ->
                        when (tab) {
                            ComicNavTab.ANIME -> {
                                isComicMode = false
                                currentTab = NavTab.HOME
                            }
                            ComicNavTab.CHAT -> {
                                openGlobalChat()
                            }
                            ComicNavTab.CARI -> {
                                activeComicGenreFilter = null
                                selectedComicTab = ComicNavTab.CARI
                            }
                            else -> {
                                selectedComicTab = tab
                            }
                        }
                    }

                    val onComicProfileTabSelected: (NavTab) -> Unit = { tab ->
                        if (tab == NavTab.CHAT) {
                            openGlobalChat()
                        } else if (tab == NavTab.COMIC) {
                            selectedComicTab = ComicNavTab.HOME
                        } else {
                            isComicMode = false
                            currentTab = tab
                        }
                    }

                    val onManualUpdateCheck: () -> Unit = {
                        scope.launch {
                            Toast.makeText(this@MainActivity, "Memeriksa pembaruan...", Toast.LENGTH_SHORT).show()
                            val info = UpdateChecker.checkUpdate(BuildConfig.VERSION_NAME, forceCheck = false)
                            if (info != null && info.isUpdateAvailable) {
                                pendingUpdateInfo = info
                            } else if (info != null) {
                                Toast.makeText(this@MainActivity, "Aplikasi sudah dalam versi terbaru (${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Gagal memeriksa pembaruan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    val activeBaseKey = remember(showComicBase, activeComicBaseTab, activeBaseTab) {
                        if (showComicBase) {
                            when (activeComicBaseTab) {
                                ComicNavTab.HOME, ComicNavTab.CARI, ComicNavTab.PROFILE -> "COMIC_${activeComicBaseTab.name}"
                                else -> "COMIC_HOME"
                            }
                        } else {
                            when (activeBaseTab) {
                                NavTab.HOME, NavTab.JADWAL, NavTab.CARI, NavTab.COMIC, NavTab.PROFILE -> "ANIME_${activeBaseTab.name}"
                                else -> "ANIME_HOME"
                            }
                        }
                    }
                    val visitedBaseTabs = remember { mutableStateMapOf(activeBaseKey to true) }
                    var settledBaseKey by remember { mutableStateOf(activeBaseKey) }
                    var exitingBaseKey by remember { mutableStateOf<String?>(null) }
                    val enteringBaseAlpha = remember { Animatable(1f) }

                    // Pre-stage tab utama di latar belakang setelah splash selesai agar perpindahan pertama bebas jeda
                    LaunchedEffect(showSplash) {
                        if (!showSplash) {
                            delay(350L)
                            if (!visitedBaseTabs.containsKey("ANIME_JADWAL")) visitedBaseTabs["ANIME_JADWAL"] = true
                            delay(200L)
                            if (!visitedBaseTabs.containsKey("ANIME_CARI")) visitedBaseTabs["ANIME_CARI"] = true
                            delay(200L)
                            if (!visitedBaseTabs.containsKey("ANIME_PROFILE")) visitedBaseTabs["ANIME_PROFILE"] = true
                        }
                    }

                    LaunchedEffect(activeBaseKey) {
                        visitedBaseTabs[activeBaseKey] = true
                        if (activeBaseKey != settledBaseKey) {
                            exitingBaseKey = settledBaseKey
                            settledBaseKey = activeBaseKey
                            enteringBaseAlpha.snapTo(0f)
                            enteringBaseAlpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                            )
                            exitingBaseKey = null
                        }
                    }

                    val orderedBaseKeys = remember {
                        listOf(
                            "ANIME_HOME",
                            "ANIME_JADWAL",
                            "ANIME_CARI",
                            "ANIME_COMIC",
                            "ANIME_PROFILE",
                            "COMIC_HOME",
                            "COMIC_CARI",
                            "COMIC_PROFILE"
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // LAYER 0: Host tab ter-cache tanpa crossfade black-dip; halaman lama tetap 100% opak di bawah halaman baru
                        Box(modifier = Modifier.fillMaxSize()) {
                            orderedBaseKeys.forEach { tabKey ->
                                if (visitedBaseTabs[tabKey] == true) {
                                    key(tabKey) {
                                        val isCurrent = tabKey == settledBaseKey
                                        val isExiting = tabKey == exitingBaseKey && !isCurrent
                                        val isVisibleTab = isCurrent || isExiting
                                        val isTabInteractive = isCurrent && topmostOverlay == null

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .zIndex(
                                                    when {
                                                        isCurrent -> 2f
                                                        isExiting -> 1f
                                                        else -> -5f
                                                    }
                                                )
                                                .graphicsLayer {
                                                    alpha = when {
                                                        isCurrent -> enteringBaseAlpha.value
                                                        isExiting -> 1f
                                                        else -> 0f
                                                    }
                                                }
                                                .drawWithContent {
                                                    if (isVisibleTab) {
                                                        drawContent()
                                                    }
                                                }
                                                .then(
                                                    if (!isTabInteractive) {
                                                        Modifier.pointerInput(Unit) {
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                                    event.changes.forEach { it.consume() }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        ) {
                                            when (tabKey) {
                                                "COMIC_HOME" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        isComicMode = false
                                                        currentTab = NavTab.HOME
                                                    }
                                                    ComicHomeScreen(
                                                        selectedComicTab = selectedComicTab,
                                                        onComicTabSelected = onComicTabSelected,
                                                        onComicClick = { cId -> selectedComicId = cId },
                                                        onChapterClick = { chId -> selectedComicChapterId = chId },
                                                        onComicChapterWithInfoClick = { chId, cId, cTitle, cPoster ->
                                                            selectedComicChapterId = chId
                                                            if (cId != null) selectedComicId = cId
                                                            if (cTitle != null) selectedComicTitle = cTitle
                                                            if (cPoster != null) selectedComicPosterUrl = cPoster
                                                        },
                                                        onOpenAllComics = { showAllComics = true },
                                                        onGenreClick = { genreName ->
                                                            activeComicGenreFilter = genreName
                                                            selectedComicTab = ComicNavTab.CARI
                                                        }
                                                    )
                                                }
                                                "COMIC_CARI" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        selectedComicTab = ComicNavTab.HOME
                                                    }
                                                    ComicSearchScreen(
                                                        initialGenre = activeComicGenreFilter,
                                                        onComicClick = { cId -> selectedComicId = cId },
                                                        onBackClick = { selectedComicTab = ComicNavTab.HOME },
                                                        selectedComicTab = selectedComicTab,
                                                        onComicTabSelected = { tab ->
                                                            when (tab) {
                                                                ComicNavTab.ANIME -> {
                                                                    isComicMode = false
                                                                    currentTab = NavTab.HOME
                                                                }
                                                                ComicNavTab.CHAT -> openGlobalChat()
                                                                else -> selectedComicTab = tab
                                                            }
                                                        },
                                                        isTabActive = isTabInteractive
                                                    )
                                                }
                                                "COMIC_PROFILE" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        selectedComicTab = ComicNavTab.HOME
                                                    }
                                                    LoginProfileScreen(
                                                        selectedTab = activeBaseTab,
                                                        onTabSelected = onComicProfileTabSelected,
                                                        onAnimeClick = { animeId -> selectedAnimeId = animeId },
                                                        onEpisodeClick = { epId, mId ->
                                                            selectedStreamingMovieId = mId
                                                            selectedStreamingEpisodeId = epId
                                                        },
                                                        onComicChapterClick = { chId -> selectedComicChapterId = chId },
                                                        onCheckUpdateClick = onManualUpdateCheck,
                                                        isTabActive = isTabInteractive
                                                    )
                                                }
                                                "ANIME_HOME" -> {
                                                    HomeScreen(
                                                        selectedTab = NavTab.HOME,
                                                        onTabSelected = onAnimeTabSelected,
                                                        onAnimeClick = { animeId -> selectedAnimeId = animeId },
                                                        onEpisodeClick = { epId, mId ->
                                                            selectedStreamingMovieId = mId
                                                            selectedStreamingEpisodeId = epId
                                                        },
                                                        onCuplixClick = { cuplixItem -> selectedCuplixItem = cuplixItem },
                                                        onBirthdayClick = { bItem -> selectedBirthdayItem = bItem },
                                                        onComicClick = { cId -> selectedComicId = cId },
                                                        onComicChapterClick = { chId -> selectedComicChapterId = chId },
                                                        onOpenAllComics = { showAllComics = true },
                                                        isTabActive = isTabInteractive,
                                                        viewModel = homeViewModel
                                                    )
                                                }
                                                "ANIME_JADWAL" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        currentTab = NavTab.HOME
                                                    }
                                                    JadwalScreen(
                                                        selectedTab = NavTab.JADWAL,
                                                        onTabSelected = onAnimeTabSelected,
                                                        onAnimeClick = { animeId -> selectedAnimeId = animeId },
                                                        onCuplixClick = { cuplixItem -> selectedCuplixItem = cuplixItem },
                                                        viewModel = jadwalViewModel
                                                    )
                                                }
                                                "ANIME_CARI" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        currentTab = NavTab.HOME
                                                    }
                                                    SearchScreen(
                                                        selectedTab = NavTab.CARI,
                                                        onTabSelected = onAnimeTabSelected,
                                                        onAnimeClick = { animeId -> selectedAnimeId = animeId },
                                                        onCuplixClick = { cuplixItem -> selectedCuplixItem = cuplixItem },
                                                        isTabActive = isTabInteractive,
                                                        viewModel = searchViewModel
                                                    )
                                                }
                                                "ANIME_COMIC" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        currentTab = NavTab.HOME
                                                    }
                                                    ComicHomeScreen(
                                                        selectedComicTab = selectedComicTab,
                                                        onComicTabSelected = onComicTabSelected,
                                                        onComicClick = { cId -> selectedComicId = cId },
                                                        onChapterClick = { chId -> selectedComicChapterId = chId },
                                                        onComicChapterWithInfoClick = { chId, cId, cTitle, cPoster ->
                                                            selectedComicChapterId = chId
                                                            if (cId != null) selectedComicId = cId
                                                            if (cTitle != null) selectedComicTitle = cTitle
                                                            if (cPoster != null) selectedComicPosterUrl = cPoster
                                                        },
                                                        onOpenAllComics = { showAllComics = true },
                                                        onGenreClick = { genreName ->
                                                            activeComicGenreFilter = genreName
                                                            selectedComicTab = ComicNavTab.CARI
                                                        }
                                                    )
                                                }
                                                "ANIME_PROFILE" -> {
                                                    BackHandler(enabled = isTabInteractive) {
                                                        currentTab = NavTab.HOME
                                                    }
                                                    LoginProfileScreen(
                                                        selectedTab = NavTab.PROFILE,
                                                        onTabSelected = onAnimeTabSelected,
                                                        onAnimeClick = { animeId -> selectedAnimeId = animeId },
                                                        onEpisodeClick = { epId, mId ->
                                                            selectedStreamingMovieId = mId
                                                            selectedStreamingEpisodeId = epId
                                                        },
                                                        onComicChapterClick = { chId -> selectedComicChapterId = chId },
                                                        onCheckUpdateClick = onManualUpdateCheck,
                                                        isTabActive = isTabInteractive
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // LAYER 1..8: Tumpukan Overlay dengan Shared Axis X + Spring & Predictive Back Interaktif
                        PredictiveBackVisibilityOverlay(
                            visible = isChatOpen,
                            isTopmost = topmostOverlay == "GLOBAL_CHAT",
                            onBack = { closeGlobalChat() }
                        ) {
                            GlobalChatScreen(
                                onBackClick = { closeGlobalChat() },
                                onOpenLoginProfile = {
                                    isComicMode = false
                                    currentTab = NavTab.PROFILE
                                }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedAnimeId,
                            isTopmost = topmostOverlay == "ANIME_DETAIL",
                            onBack = { selectedAnimeId = null }
                        ) { animeId ->
                            AnimeDetailScreen(
                                movieId = animeId,
                                onBackClick = { selectedAnimeId = null },
                                onAnimeClick = { newId -> selectedAnimeId = newId },
                                onEpisodeClick = { epId ->
                                    selectedStreamingMovieId = animeId
                                    selectedStreamingEpisodeId = epId
                                },
                                onCuplixClick = { cuplixItem ->
                                    selectedCuplixItem = cuplixItem
                                }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedStreamingEpisodeId,
                            isTopmost = topmostOverlay == "STREAMING",
                            onBack = { selectedStreamingEpisodeId = null },
                            exitDurationMs = 190
                        ) { episodeId ->
                            StreamingScreen(
                                episodeId = episodeId,
                                movieId = selectedStreamingMovieId ?: selectedAnimeId ?: "",
                                onBackClick = { selectedStreamingEpisodeId = null },
                                onEpisodeChange = { newEpId ->
                                    selectedStreamingEpisodeId = newEpId
                                }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedCuplixItem,
                            isTopmost = topmostOverlay == "CUPLIX_VIEW",
                            onBack = { selectedCuplixItem = null }
                        ) { cuplix ->
                            CuplixViewScreen(
                                initialCuplix = cuplix,
                                onBackClick = { selectedCuplixItem = null },
                                onAnimeClick = { animeId ->
                                    selectedCuplixItem = null
                                    selectedAnimeId = animeId
                                },
                                onEpisodeClick = { epId, mId ->
                                    selectedCuplixItem = null
                                    selectedStreamingMovieId = mId
                                    selectedStreamingEpisodeId = epId
                                }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedBirthdayItem,
                            isTopmost = topmostOverlay == "BIRTHDAY_DETAIL",
                            onBack = { selectedBirthdayItem = null }
                        ) { bItem ->
                            com.yuranime.dinz.ui.detail.BirthdayDetailScreen(
                                item = bItem,
                                onBackClick = { selectedBirthdayItem = null },
                                onNavigateToDetailUrl = { newUrl ->
                                    val idMatch = Regex("""/(character|people)/view/(\d+)/([^/?#\s]+)""").find(newUrl)
                                    val kind = if (newUrl.contains("/people/view/")) "seiyuu_staff" else "character"
                                    val id = idMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                                    val slug = idMatch?.groupValues?.getOrNull(3).orEmpty()
                                    selectedBirthdayItem = com.yuranime.dinz.data.api.BirthdayCharacterItem(
                                        id = id,
                                        slug = slug,
                                        type = kind,
                                        name = slug.replace("-", " "),
                                        nameKanji = "",
                                        nameEn = "",
                                        imageUrl = "",
                                        thumbnailUrl = "",
                                        detailUrl = newUrl
                                    )
                                },
                                onAnimeClick = { animeTitleOrId ->
                                    selectedAnimeId = animeTitleOrId
                                    selectedBirthdayItem = null
                                }
                            )
                        }

                        PredictiveBackVisibilityOverlay(
                            visible = showAllComics,
                            isTopmost = topmostOverlay == "COMIC_VIEW_ALL",
                            onBack = { showAllComics = false }
                        ) {
                            com.yuranime.dinz.ui.comic.ComicViewAllScreen(
                                onBackClick = { showAllComics = false },
                                onComicClick = { cId -> selectedComicId = cId }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedComicId,
                            isTopmost = topmostOverlay == "COMIC_DETAIL",
                            onBack = { selectedComicId = null }
                        ) { cId ->
                            com.yuranime.dinz.ui.comic.ComicDetailScreen(
                                comicId = cId,
                                onBackClick = { selectedComicId = null },
                                onChapterClick = { chId -> selectedComicChapterId = chId },
                                onChapterWithTitleClick = { chId, title ->
                                    selectedComicTitle = title
                                    selectedComicChapterId = chId
                                },
                                onChapterWithDetailsClick = { chId, title, poster ->
                                    selectedComicTitle = title
                                    selectedComicPosterUrl = poster
                                    selectedComicChapterId = chId
                                }
                            )
                        }

                        PredictiveBackItemOverlay(
                            targetItem = selectedComicChapterId,
                            isTopmost = topmostOverlay == "COMIC_READER",
                            onBack = { selectedComicChapterId = null }
                        ) { chapterId ->
                            com.yuranime.dinz.ui.comic.ComicReaderScreen(
                                chapterId = chapterId,
                                comicId = selectedComicId,
                                comicTitle = selectedComicTitle,
                                posterUrl = selectedComicPosterUrl,
                                onBackClick = { selectedComicChapterId = null }
                            )
                        }

                        // Dialog Pembaruan Otomatis Bergaya iOS Frosted Glassmorphism + Background Download
                        val lifecycleOwner = LocalLifecycleOwner.current
                        val updateDownloadState by UpdateDownloadManager.downloadState.collectAsStateWithLifecycle()
                        val isUpdateBannerMinimized by UpdateDownloadManager.isMinimized.collectAsStateWithLifecycle()

                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    UpdateDownloadManager.consumePendingInstallIfGranted(this@MainActivity)
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                            }
                        }

                        pendingUpdateInfo?.let { updateInfo ->
                            UpdateNoticeDialog(
                                updateInfo = updateInfo,
                                onDismiss = {
                                    if (updateDownloadState is UpdateDownloadState.Downloading) {
                                        UpdateDownloadManager.minimizeBanner()
                                    }
                                    pendingUpdateInfo = null
                                }
                            )
                        }

                        // Mini status bar putih non-blocking ketika dialog di-minimize atau unduhan berjalan di latar
                        if (pendingUpdateInfo == null &&
                            (isUpdateBannerMinimized || updateDownloadState is UpdateDownloadState.Downloading || updateDownloadState is UpdateDownloadState.Completed)
                        ) {
                            UpdateMiniProgressBanner(
                                downloadState = updateDownloadState,
                                onExpandDialog = {
                                    pendingUpdateInfo = UpdateDownloadManager.activeUpdateInfo.value
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .zIndex(90f)
                            )
                        }

                        // Dialog Notifikasi Promosi Dinamis (Sesuai Referensi 1002208488.jpg & Apple Design)
                        val promoToShow = activePromoToShow
                        if (!showSplash && promoToShow != null) {
                            PromoPopupDialog(
                                config = promoToShow,
                                onDismiss = {
                                    AppPreferences.recordPromoPopupShown(
                                        context = this@MainActivity,
                                        promoId = promoToShow.promoId
                                    )
                                    activePromoToShow = null
                                }
                            )
                        }

                        if (showSplash) {
                            YuranimeSplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
