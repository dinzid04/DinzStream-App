package com.yuranime.dinz.data.repository

import com.yuranime.dinz.data.api.ApiClient
import com.yuranime.dinz.data.api.AnimeInApiService
import com.yuranime.dinz.data.model.AnimeHomeData
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.CommunityMediaItem
import com.yuranime.dinz.data.model.ContinueWatchingItem
import com.yuranime.dinz.data.model.ContributorItem
import com.yuranime.dinz.data.model.CuplixItem
import com.yuranime.dinz.data.model.DiscussionItem
import com.yuranime.dinz.data.model.EpisodeCommentItem
import com.yuranime.dinz.data.model.EpisodeItem
import com.yuranime.dinz.data.model.ExploreCharacterItem
import com.yuranime.dinz.data.model.ExploreGenreItem
import com.yuranime.dinz.data.model.ExploreYearItem
import com.yuranime.dinz.data.model.MovieDetailData
import com.yuranime.dinz.data.model.StreamData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnimeRepository(
    private val api: AnimeInApiService = ApiClient.apiService
) {

    companion object {
        @Volatile
        private var memoryCachedHomeData: AnimeHomeData? = null
    }

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

    fun getFallbackHomeData(): AnimeHomeData? = memoryCachedHomeData

    suspend fun getHomeData(): Result<AnimeHomeData> = withContext(Dispatchers.IO) {
        try {
            val day = getCurrentDayName()
            val response = api.getHomeData(day = day)
            val data = response.data
            if (data != null && (!data.hotAnime.isNullOrEmpty() || !data.newEpisodes.isNullOrEmpty() || !data.popularAnime.isNullOrEmpty())) {
                memoryCachedHomeData = data
                Result.success(data)
            } else {
                val fallback = memoryCachedHomeData
                if (fallback != null) {
                    Result.success(fallback)
                } else if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("Data respon kosong"))
                }
            }
        } catch (e: Exception) {
            val fallback = memoryCachedHomeData
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getCuplixFeed(
        limit: Int = 30,
        sort: String = "views",
        cursorNewId: String? = null,
        cursorNewTime: String? = null
    ): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCuplixFeed(
                limit = limit,
                sort = sort,
                cursorNewId = cursorNewId,
                cursorNewTime = cursorNewTime
            )
            val items = response.data?.fypList ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    enum class CuplixPageSource {
        HOME, JADWAL, SEARCH, FEED
    }

    /**
     * Mengambil feed Cuplix acak berkualitas tinggi dengan variasi batch independen untuk
     * setiap halaman (Home, Jadwal, Search, Cuplix Feed) serta pengacakan dinamis (shuffled).
     */
    suspend fun getRandomizedCuplixFeed(
        limit: Int = 30,
        source: CuplixPageSource = CuplixPageSource.FEED
    ): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        coroutineScope {
            try {
                val items = when (source) {
                    CuplixPageSource.HOME -> {
                        val popular = async { getCuplixFeed(limit = 35, sort = "views").getOrDefault(emptyList()) }
                        val latest = async { getCuplixFeed(limit = 35, sort = "new").getOrDefault(emptyList()) }
                        val merged = (popular.await() + latest.await())
                            .filter { !it.id.isNullOrBlank() }
                            .distinctBy { it.id }
                            .shuffled()
                        merged.take(limit)
                    }
                    CuplixPageSource.JADWAL -> {
                        val latest = async { getCuplixFeed(limit = 40, sort = "new").getOrDefault(emptyList()) }
                        val popular = async { getCuplixFeed(limit = 20, sort = "views").getOrDefault(emptyList()) }
                        val merged = (latest.await().shuffled() + popular.await())
                            .filter { !it.id.isNullOrBlank() }
                            .distinctBy { it.id }
                            .shuffled()
                        merged.take(limit)
                    }
                    CuplixPageSource.SEARCH -> {
                        val popular = async { getCuplixFeed(limit = 45, sort = "views").getOrDefault(emptyList()) }
                        val latest = async { getCuplixFeed(limit = 25, sort = "new").getOrDefault(emptyList()) }
                        val list = popular.await().shuffled() + latest.await().shuffled()
                        list.filter { !it.id.isNullOrBlank() }
                            .distinctBy { it.id }
                            .shuffled()
                            .take(limit)
                    }
                    CuplixPageSource.FEED -> {
                        val popular = async { getCuplixFeed(limit = 40, sort = "views").getOrDefault(emptyList()) }
                        val latest = async { getCuplixFeed(limit = 40, sort = "new").getOrDefault(emptyList()) }
                        val merged = (popular.await() + latest.await())
                            .filter { !it.id.isNullOrBlank() }
                            .distinctBy { it.id }
                            .shuffled()
                        merged
                    }
                }
                Result.success(items)
            } catch (e: Exception) {
                getCuplixFeed(limit = limit, sort = "views")
            }
        }
    }

    suspend fun getCuplixComments(idFyp: String): Result<List<EpisodeCommentItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCuplixComments(idFyp = idFyp)
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
                imagePoster = "https://xyz-api.animein.net/assets_xyz/images/comic/1.jpg",
                views = "285"
            ),
            AnimeItem(
                id = "m2",
                title = "Kakak Rental PS",
                genre = "Romance",
                imagePoster = "https://xyz-api.animein.net/assets_xyz/images/comic/2.jpg",
                views = "19.360"
            ),
            AnimeItem(
                id = "m3",
                title = "Beyond Talent",
                genre = "Action",
                imagePoster = "https://xyz-api.animein.net/assets_xyz/images/comic/3.jpg",
                views = "826"
            ),
            AnimeItem(
                id = "m4",
                title = "Vortex",
                genre = "Action",
                imagePoster = "https://xyz-api.animein.net/assets_xyz/images/comic/4.jpg",
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

    suspend fun exploreMovieGenre(
        idGenre: String? = null,
        genreIn: String? = null,
        sort: String? = null,
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreMovieGenre(
                idGenre = idGenre?.ifBlank { null },
                genreIn = genreIn?.ifBlank { null },
                sort = sort?.ifBlank { null },
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

    suspend fun exploreMovieStudio(
        studio: String,
        sort: String? = null,
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreMovieStudio(
                studio = studio,
                sort = sort?.ifBlank { null },
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

    suspend fun exploreMovieType(
        type: String,
        sort: String? = null,
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreMovieType(
                type = type,
                sort = sort?.ifBlank { null },
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

    suspend fun exploreMovieYear(
        year: String,
        season: String? = null,
        sort: String? = null,
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExploreMovieYear(
                year = year,
                season = season?.ifBlank { null },
                sort = sort?.ifBlank { null },
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
