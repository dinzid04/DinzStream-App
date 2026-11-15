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

    private fun isFamilySafeCuplix(item: CuplixItem): Boolean {
        val animeTitle = item.anime.orEmpty().lowercase(java.util.Locale.ROOT)
        val caption = item.caption.orEmpty().lowercase(java.util.Locale.ROOT)
        val combined = "$animeTitle $caption"

        val blockedAnimeKeywords = listOf(
            "kanojo mo kanojo", "isekai maou", "domestic na kanojo", "tejina-senpai",
            "tejina senpai", "tawawa", "high school dxd", "highschool dxd", "redo of healer",
            "kaifuku jutsushi", "interspecies", "ishuzoku", "overflow", "kiss x sis",
            "kissxsis", "shinmai maou", "valkyrie drive", "masou gakuen", "to love-ru",
            "toloveru", "prison school", "seikon no qwaser", "peter grill", "shuumatsu no harem",
            "world's end harem", "megami-ryou", "yuragi-sou", "isekai meikyuu",
            "harem in the labyrinth", "futoku no guild", "nande koko ni sensei",
            "yosuga no sora", "monster musume", "okusan", "oku-sama ga seitokaichou",
            "hxeros", "cyclops shoujo", "mankitsu", "gushing over magical girls",
            "mahou shoujo ni akogarete", "chichi", "boku no pico", "highschool of the dead",
            "high school of the dead", "strike the blood", "trinity seven"
        )
        if (blockedAnimeKeywords.any { animeTitle.contains(it) }) return false

        val blockedCaptionKeywords = listOf(
            "18+", "nsfw", "ecchi", "hentai", "oppai", "toket", "nenen", "sange",
            "bokep", "mesum", "telanjang", "cabul", "coli", "colmek", "desah",
            "celana dalam", "sempak", "jav", "tobrut", "boobs", "tits", "pussy",
            "panty", "panties", "paizuri", "lewds", "lewd", "bikini seksi"
        )
        val words = combined.split(Regex("[\\s,.;:!?/()\\[\\]\"']+"))
        for (kw in blockedCaptionKeywords) {
            if (kw.contains(" ") || kw.contains("+")) {
                if (combined.contains(kw)) return false
            } else if (words.any { it == kw || (kw.length >= 5 && it.contains(kw)) }) {
                return false
            }
        }
        return true
    }

    private fun diversifyCuplixItems(items: List<CuplixItem>): List<CuplixItem> {
        val safe = items.filter { !it.id.isNullOrBlank() && isFamilySafeCuplix(it) }
            .distinctBy { it.id }
        val perAnimeCounts = HashMap<String, Int>()
        val primary = ArrayList<CuplixItem>(safe.size)
        val overflow = ArrayList<CuplixItem>()
        for (item in safe) {
            val key = item.anime?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty().ifBlank { item.id.orEmpty() }
            val c = perAnimeCounts[key] ?: 0
            if (c < 2) {
                perAnimeCounts[key] = c + 1
                primary.add(item)
            } else {
                overflow.add(item)
            }
        }
        return primary.shuffled() + overflow.shuffled()
    }

    private suspend fun fetchCuplixPage(
        limit: Int = 30,
        sort: String = "new",
        cursorNewId: String? = null,
        cursorNewTime: String? = null
    ): Pair<List<CuplixItem>, com.yuranime.dinz.data.model.CuplixCursor?> {
        val response = api.getCuplixFeed(
            limit = limit,
            sort = sort,
            cursorNewId = cursorNewId,
            cursorNewTime = cursorNewTime
        )
        return (response.data?.fypList ?: emptyList()) to response.data?.cursor
    }

    suspend fun getCuplixFeed(
        limit: Int = 30,
        sort: String = "new",
        cursorNewId: String? = null,
        cursorNewTime: String? = null
    ): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val resolvedSort = if (sort == "views") "new" else sort
            val (items, _) = fetchCuplixPage(
                limit = limit,
                sort = resolvedSort,
                cursorNewId = cursorNewId,
                cursorNewTime = cursorNewTime
            )
            Result.success(items.filter { isFamilySafeCuplix(it) }.ifEmpty { items })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    enum class CuplixPageSource {
        HOME, JADWAL, SEARCH, FEED
    }

    /**
     * Mengambil feed Cuplix terbaru (`sort=new`) lintas beberapa halaman cursor agar bervariasi,
     * segar, dan bebas dominasi klip 18+/ecchi lama untuk setiap halaman.
     */
    suspend fun getRandomizedCuplixFeed(
        limit: Int = 30,
        source: CuplixPageSource = CuplixPageSource.FEED
    ): Result<List<CuplixItem>> = withContext(Dispatchers.IO) {
        try {
            val targetPages = when (source) {
                CuplixPageSource.HOME -> 3
                CuplixPageSource.JADWAL -> 3
                CuplixPageSource.SEARCH -> 4
                CuplixPageSource.FEED -> 4
            }
            val collected = ArrayList<CuplixItem>()
            var nextId: String? = null
            var nextTime: String? = null

            for (page in 0 until targetPages) {
                val (pageItems, cursor) = fetchCuplixPage(
                    limit = 25,
                    sort = "new",
                    cursorNewId = nextId,
                    cursorNewTime = nextTime
                )
                collected.addAll(pageItems)
                val cid = cursor?.cursorNewId?.takeIf { it.isNotBlank() }
                val ctime = cursor?.cursorNewTime?.takeIf { it.isNotBlank() }
                if (cid == null || ctime == null || pageItems.isEmpty()) break
                nextId = cid
                nextTime = ctime
            }

            // Pilih segmen berbeda per sumber halaman supaya Home/Jadwal/Search tidak terasa identik
            val filtered = diversifyCuplixItems(collected)
            val result = when (source) {
                CuplixPageSource.HOME -> filtered.take(limit)
                CuplixPageSource.JADWAL -> filtered.drop((filtered.size / 6).coerceAtMost(8)).plus(filtered.take(8)).distinctBy { it.id }.take(limit)
                CuplixPageSource.SEARCH -> filtered.shuffled().take(limit)
                CuplixPageSource.FEED -> filtered
            }
            Result.success(result.ifEmpty { collected.distinctBy { it.id }.take(limit) })
        } catch (e: Exception) {
            getCuplixFeed(limit = limit, sort = "new")
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
