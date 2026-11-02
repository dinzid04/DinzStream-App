package com.yuranime.dinz

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuranime.dinz.BuildConfig
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.update.AppUpdateInfo
import com.yuranime.dinz.data.update.UpdateChecker
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.cuplix.CuplixViewScreen
import com.yuranime.dinz.ui.detail.AnimeDetailScreen
import com.yuranime.dinz.ui.home.HomeScreen
import com.yuranime.dinz.ui.home.HomeViewModel
import com.yuranime.dinz.ui.jadwal.JadwalScreen
import com.yuranime.dinz.ui.profile.LoginProfileScreen
import com.yuranime.dinz.ui.search.SearchScreen
import com.yuranime.dinz.ui.splash.YuranimeSplashScreen
import com.yuranime.dinz.ui.streaming.StreamingScreen
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.YuranimeTheme
import com.yuranime.dinz.ui.update.UpdateNoticeDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
        }

        setContent {
            YuranimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBackgroundPrimary
                ) {
                    // Preload Home data in the background while the motion splash screen runs
                    val homeViewModel: HomeViewModel = viewModel()
                    val scope = rememberCoroutineScope()
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    var selectedAnimeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingEpisodeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingMovieId by remember { mutableStateOf<String?>(null) }
                    var selectedCuplixItem by remember { mutableStateOf<CuplixItem?>(null) }
                    var currentTab by remember { mutableStateOf(NavTab.HOME) }
                    var pendingUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

                    // TAHAP 1: Pengecekan versi SemVer otomatis saat aplikasi dibuka
                    LaunchedEffect(Unit) {
                        try {
                            val info = UpdateChecker.checkUpdate(BuildConfig.VERSION_NAME)
                            if (info != null && info.isUpdateAvailable) {
                                pendingUpdateInfo = info
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            selectedCuplixItem != null -> {
                                key(selectedCuplixItem?.id) {
                                    CuplixViewScreen(
                                        initialCuplix = selectedCuplixItem,
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
                            }
                            selectedStreamingEpisodeId != null -> {
                                StreamingScreen(
                                    episodeId = selectedStreamingEpisodeId!!,
                                    movieId = selectedStreamingMovieId ?: selectedAnimeId ?: "",
                                    onBackClick = {
                                        selectedStreamingEpisodeId = null
                                    },
                                    onEpisodeChange = { newEpId ->
                                        selectedStreamingEpisodeId = newEpId
                                    }
                                )
                            }
                            selectedAnimeId != null -> {
                                BackHandler {
                                    selectedAnimeId = null
                                }
                                AnimeDetailScreen(
                                    movieId = selectedAnimeId!!,
                                    onBackClick = {
                                        selectedAnimeId = null
                                    },
                                    onAnimeClick = { newId ->
                                        selectedAnimeId = newId
                                    },
                                    onEpisodeClick = { epId ->
                                        selectedStreamingMovieId = selectedAnimeId
                                        selectedStreamingEpisodeId = epId
                                    },
                                    onCuplixClick = { cuplixItem ->
                                        selectedCuplixItem = cuplixItem
                                    }
                                )
                            }
                            else -> {
                                when (currentTab) {
                                    NavTab.HOME -> {
                                        HomeScreen(
                                            selectedTab = currentTab,
                                            onTabSelected = { tab ->
                                                if (tab == NavTab.CHAT) {
                                                    Toast.makeText(this@MainActivity, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    currentTab = tab
                                                }
                                            },
                                            onAnimeClick = { animeId ->
                                                selectedAnimeId = animeId
                                            },
                                            onEpisodeClick = { epId, mId ->
                                                selectedStreamingMovieId = mId
                                                selectedStreamingEpisodeId = epId
                                            },
                                            onCuplixClick = { cuplixItem ->
                                                selectedCuplixItem = cuplixItem
                                            },
                                            viewModel = homeViewModel
                                        )
                                    }
                                    NavTab.JADWAL -> {
                                        BackHandler {
                                            currentTab = NavTab.HOME
                                        }
                                        JadwalScreen(
                                            selectedTab = currentTab,
                                            onTabSelected = { tab ->
                                                if (tab == NavTab.CHAT) {
                                                    Toast.makeText(this@MainActivity, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    currentTab = tab
                                                }
                                            },
                                            onAnimeClick = { animeId ->
                                                selectedAnimeId = animeId
                                            },
                                            onCuplixClick = { cuplixItem ->
                                                selectedCuplixItem = cuplixItem
                                            }
                                        )
                                    }
                                    NavTab.CARI -> {
                                        BackHandler {
                                            currentTab = NavTab.HOME
                                        }
                                        SearchScreen(
                                            selectedTab = currentTab,
                                            onTabSelected = { tab ->
                                                if (tab == NavTab.CHAT) {
                                                    Toast.makeText(this@MainActivity, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    currentTab = tab
                                                }
                                            },
                                            onAnimeClick = { animeId ->
                                                selectedAnimeId = animeId
                                            },
                                            onCuplixClick = { cuplixItem ->
                                                selectedCuplixItem = cuplixItem
                                            }
                                        )
                                    }
                                    NavTab.PROFILE -> {
                                        BackHandler {
                                            currentTab = NavTab.HOME
                                        }
                                        LoginProfileScreen(
                                            selectedTab = currentTab,
                                            onTabSelected = { tab ->
                                                if (tab == NavTab.CHAT) {
                                                    Toast.makeText(this@MainActivity, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    currentTab = tab
                                                }
                                            },
                                            onAnimeClick = { animeId ->
                                                selectedAnimeId = animeId
                                            },
                                            onEpisodeClick = { epId, mId ->
                                                selectedStreamingMovieId = mId
                                                selectedStreamingEpisodeId = epId
                                            },
                                            onCheckUpdateClick = {
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
                                        )
                                    }
                                    else -> {
                                        currentTab = NavTab.HOME
                                    }
                                }
                            }
                        }

                        // Dialog Pembaruan Otomatis Bergaya iOS Frosted Glassmorphism
                        if (pendingUpdateInfo != null) {
                            UpdateNoticeDialog(
                                updateInfo = pendingUpdateInfo!!,
                                onDismiss = { pendingUpdateInfo = null }
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
