package com.dinz.animein.data.repository

import com.dinz.animein.data.api.ApiClient
import com.dinz.animein.data.api.AnimeInApiService
import com.dinz.animein.data.model.AnimeHomeData
import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.data.model.ContinueWatchingItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.SliderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnimeRepository(
    private val api: AnimeInApiService = ApiClient.apiService
) {

    private fun getCurrentDayName(): String {
        val sdf = SimpleDateFormat("EEEE", Locale("id", "ID"))
        return when (sdf.format(Date()).uppercase(Locale.ROOT)) {
            "SENIN" -> "SENIN"
            "SELASA" -> "SELASA"
            "RABU" -> "RABU"
            "KAMIS" -> "KAMIS"
            "JUMAT" -> "JUMAT"
            "SABTU" -> "SABTU"
            "MINGGU" -> "MINGGU"
            else -> "SENIN"
        }
    }

    suspend fun getHomeData(): Result<AnimeHomeData> = withContext(Dispatchers.IO) {
        try {
            val day = getCurrentDayName()
            val response = api.getHomeData(day = day)
            val data = response.data
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Data respon kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCuplixFeed(): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCuplixFeed(limit = 15, sort = "new")
            val items = response.data?.fypList ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getContinueWatchingList(): List<ContinueWatchingItem> {
        // Sample cached / local continue watching items
        return listOf(
            ContinueWatchingItem(
                id = "1",
                idMovie = "6642",
                title = "Soul Land 2: The Peerless Tang Clan",
                episodeText = "Episode 176",
                imageUrl = "https://xyz-api.animein.net/assets_xyz/images/movie/poster/ec95a08b5097987985d4a596d6d5d4b0.jpg",
                progressFraction = 0.65f
            ),
            ContinueWatchingItem(
                id = "2",
                idMovie = "1755",
                title = "Detective Conan",
                episodeText = "Episode 429",
                imageUrl = "https://api.animein.net/assets/images/movie/poster/6afc662198d6c50af1f6e5a669b268cd.jpg",
                progressFraction = 0.30f
            )
        )
    }

    fun getManreList(): List<AnimeItem> {
        return listOf(
            AnimeItem(
                id = "m1",
                title = "Di Sela-Sela Kuliah",
                genre = "Comedy",
                imagePoster = "https://cf1.dinzid.my.id/?url=https%3A%2F%2Fxyz-api.animein.net%2Fassets_xyz%2Fimages%2Fcomic%2F1.jpg",
                views = "285"
            ),
            AnimeItem(
                id = "m2",
                title = "Kakak Rental PS",
                genre = "Romance",
                imagePoster = "https://cf1.dinzid.my.id/?url=https%3A%2F%2Fxyz-api.animein.net%2Fassets_xyz%2Fimages%2Fcomic%2F2.jpg",
                views = "19.360"
            ),
            AnimeItem(
                id = "m3",
                title = "Beyond Talent",
                genre = "Action",
                imagePoster = "https://cf1.dinzid.my.id/?url=https%3A%2F%2Fxyz-api.animein.net%2Fassets_xyz%2Fimages%2Fcomic%2F3.jpg",
                views = "826"
            ),
            AnimeItem(
                id = "m4",
                title = "Vortex",
                genre = "Action",
                imagePoster = "https://cf1.dinzid.my.id/?url=https%3A%2F%2Fxyz-api.animein.net%2Fassets_xyz%2Fimages%2Fcomic%2F4.jpg",
                views = "1.424"
            )
        )
    }
}
