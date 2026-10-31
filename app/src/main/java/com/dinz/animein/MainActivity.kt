package com.dinz.animein

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dinz.animein.ui.components.NavTab
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

        setContent {
            AnimeInTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBackgroundPrimary
                ) {
                    var selectedAnimeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingEpisodeId by remember { mutableStateOf<String?>(null) }
                    var selectedStreamingMovieId by remember { mutableStateOf<String?>(null) }
                    var currentTab by remember { mutableStateOf(NavTab.HOME) }

                    when {
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
                                        onCuplixClick = { cuplixId ->
                                            Toast.makeText(this, "Putar Cuplix #$cuplixId", Toast.LENGTH_SHORT).show()
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
                                        onCuplixClick = { cuplixId ->
                                            Toast.makeText(this, "Putar Cuplix #$cuplixId", Toast.LENGTH_SHORT).show()
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
                                        onCuplixClick = { cuplixId ->
                                            Toast.makeText(this, "Putar Cuplix #$cuplixId", Toast.LENGTH_SHORT).show()
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
