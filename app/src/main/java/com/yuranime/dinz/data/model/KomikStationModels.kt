package com.yuranime.dinz.data.model

import com.yuranime.dinz.R
import java.util.Locale
import kotlin.math.roundToInt

data class KomikStationCardItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val format: String = "Manga",
    val chapter: String = "",
    val score: Float = 0f
) {
    val flagDrawableRes: Int
        get() = when (format.trim().lowercase(Locale.ROOT)) {
            "manhwa" -> R.drawable.kr
            "manhua" -> R.drawable.cn
            else -> R.drawable.jp
        }

    val filledStars: Int
        get() = (score / 2f).roundToInt().coerceIn(0, 5)
}

data class KomikStationTab(
    val id: String,
    val title: String,
    val comics: List<KomikStationCardItem> = emptyList()
)

data class KomikStationGenre(
    val name: String,
    val url: String,
    val slug: String
)

data class KomikStationSeriesItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val genres: List<String> = emptyList(),
    val year: String = "",
    val score: Float? = null,
    val rank: Int? = null
)

data class KomikStationHomeData(
    val tabs: List<KomikStationTab> = emptyList(),
    val genres: List<KomikStationGenre> = emptyList(),
    val seriesTerbaru: List<KomikStationSeriesItem> = emptyList(),
    val seriesTerpopuler: List<KomikStationSeriesItem> = emptyList()
)
