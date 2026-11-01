package com.dinz.animein

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.ui.components.NavTab
import com.dinz.animein.ui.cuplix.CuplixViewScreen
import com.dinz.animein.ui.detail.AnimeDetailScreen
import com.dinz.animein.ui.home.HomeScreen
import com.dinz.animein.ui.jadwal.JadwalScreen
import com.dinz.animein.ui.search.SearchScreen
import com.dinz.animein.ui.streaming.StreamingScreen
import com.dinz.animein.ui.theme.AnimeInTheme
import com.dinz.animein.ui.theme.ColorBackgroundPrimary

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
            AnimeInTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBackgroundPrimary
                ) {
                    var selectedAnimeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingEpisodeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingMovieId by remember { mutableStateOf<String?>(null) }
                    var selectedCuplixItem by remember { mutableStateOf<CuplixItem?>(null) }
                    var currentTab by remember { mutableStateOf(NavTab.HOME) }

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
                                            if (tab == NavTab.CHAT || tab == NavTab.PROFILE) {
                                                Toast.makeText(this, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
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
                                        }
                                    )
                                }
                                NavTab.JADWAL -> {
                                    BackHandler {
                                        currentTab = NavTab.HOME
                                    }
                                    JadwalScreen(
                                        selectedTab = currentTab,
                                        onTabSelected = { tab ->
                                            if (tab == NavTab.CHAT || tab == NavTab.PROFILE) {
                                                Toast.makeText(this, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
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
                                            if (tab == NavTab.CHAT || tab == NavTab.PROFILE) {
                                                Toast.makeText(this, "Fitur ${tab.title} segera hadir", Toast.LENGTH_SHORT).show()
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
                                else -> {
                                    currentTab = NavTab.HOME
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
