package com.dinz.animein.data.repository

import com.dinz.animein.data.api.ApiClient
import com.dinz.animein.data.api.AnimeInApiService
import com.dinz.animein.data.model.AnimeHomeData
import com.dinz.animein.data.model.AnimeItem
import com.dinz.animein.data.model.CommunityMediaItem
import com.dinz.animein.data.model.ContinueWatchingItem
import com.dinz.animein.data.model.ContributorItem
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.data.model.DiscussionItem
import com.dinz.animein.data.model.EpisodeCommentItem
import com.dinz.animein.data.model.EpisodeItem
import com.dinz.animein.data.model.ExploreCharacterItem
import com.dinz.animein.data.model.ExploreGenreItem
import com.dinz.animein.data.model.ExploreYearItem
import com.dinz.animein.data.model.MovieDetailData
import com.dinz.animein.data.model.StreamData
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

    suspend fun getCuplixFeed(limit: Int = 15): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCuplixFeed(limit = limit, sort = "new")
            val items = response.data?.fypList ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCuplixComments(idFyp: String, page: Int = 1): Result<List<EpisodeCommentItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCuplixComments(idFyp = idFyp, page = page)
            val comments = response.data?.comments ?: emptyList()
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHomeNewTitles(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHomeNewTitles()
            val list = response.data?.movies ?: response.data?.newEpisodes ?: emptyList()
            Result.success(list.take(16))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHomeRandom(): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHomeRandom()
            val list = response.data?.movies ?: response.data?.newEpisodes ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DETAIL REPOSITORY METHODS ====================

    suspend fun getMovieDetail(id: String): Result<MovieDetailData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieDetail(id)
            val data = response.data
            if (data?.movie != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Informasi anime tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieEpisodes(id: String, page: Int = 0): Result<List<EpisodeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieEpisodes(id, page = page)
            val episodes = response.data?.episodes ?: emptyList()
            Result.success(episodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieCuplix(idMovie: String, page: Int = 1): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieCuplix(idMovie = idMovie, page = page)
            val items = response.data?.fypList ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMoviePosters(idMovie: String): Result<List<CommunityMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMoviePosters(idMovie = idMovie)
            val items = response.data?.posters ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieCovers(idMovie: String): Result<List<CommunityMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieCovers(idMovie = idMovie)
            val items = response.data?.covers ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDiscussions(idMovie: String, page: Int = 1): Result<List<DiscussionItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieDiscussions(idMovie = idMovie, page = page)
            val items = response.data?.discussions ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieContributors(idMovie: String, page: Int = 1): Result<List<ContributorItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMovieContributors(idMovie = idMovie, page = page)
            val items = response.data?.contributors ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== STREAMING & COMMENTS ====================

    suspend fun getEpisodeStream(idEpisode: String): Result<StreamData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEpisodeStream(idEpisode)
            val data = response.data
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Data stream kosong"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodeComments(idEpisode: String, page: Int = 0, sort: String = "new"): Result<List<EpisodeCommentItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEpisodeComments(idEpisode = idEpisode, page = page, sort = sort)
            val comments = response.data?.comments ?: emptyList()
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentReplies(idComment: String, page: Int = 1): Result<List<EpisodeCommentItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCommentReplies(idComment = idComment, page = page)
            val replies = response.data?.replies ?: emptyList()
            Result.success(replies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun getContinueWatchingList(): List<ContinueWatchingItem> {
        return emptyList()
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

    // ==================== JADWAL REPOSITORY ====================

    suspend fun getSchedule(day: String): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSchedule(day = day.uppercase(Locale.ROOT))
            val movies = response.data?.movies ?: emptyList()
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== EXPLORE / SEARCH REPOSITORY ====================

    suspend fun getExploreGenres(): Result<List<ExploreGenreItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreGenres()
            val genres = response.data?.genres ?: emptyList()
            Result.success(genres)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExploreYears(): Result<List<ExploreYearItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreYears()
            val years = response.data?.years ?: emptyList()
            Result.success(years)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExploreCharacters(): Result<List<ExploreCharacterItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreCharacters()
            val chars = response.data?.characters ?: emptyList()
            Result.success(chars)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMovies(
        keyword: String? = null,
        genreIn: String? = null,
        type: String? = null,
        sort: String = "views",
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchMovies(
                keyword = keyword?.ifBlank { null },
                genreIn = genreIn?.ifBlank { null },
                type = type?.ifBlank { null },
                sort = sort,
                page = page
            )
            val movies = response.data?.movies
                ?: response.data?.hotAnime
                ?: response.data?.newEpisodes
                ?: emptyList()
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
