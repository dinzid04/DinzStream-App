package com.dinz.animein

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dinz.animein.ui.home.HomeScreen
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
                    HomeScreen(
                        onAnimeClick = { animeId ->
                            Toast.makeText(this, "Buka Anime #$animeId", Toast.LENGTH_SHORT).show()
                        },
                        onCuplixClick = { cuplixId ->
                            Toast.makeText(this, "Putar Cuplix #$cuplixId", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
