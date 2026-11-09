package com.yuranime.dinz.data.repository

import com.yuranime.dinz.data.api.ComicApiService
import com.yuranime.dinz.data.api.ShngmMangaDto
import com.yuranime.dinz.data.model.ComicChapterDetail
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.ComicDetail
import com.yuranime.dinz.data.model.ComicItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ComicRepository(
    private val api: ComicApiService = ComicApiService.create(),
    private val komikStationScraper: KomikStationScraper = KomikStationScraper()
) {
    private var cachedBanners: List<com.yuranime.dinz.data.model.ComicBannerItem>? = null
    private var cachedTrendingComics: List<ComicItem>? = null
    private var cachedPopularComics: List<ComicItem>? = null
    private var cachedLatestComics: List<ComicItem>? = null
    private val cachedDetails = mutableMapOf<String, ComicDetail>()

    suspend fun getBanners(forceRefresh: Boolean = false): Result<List<com.yuranime.dinz.data.model.ComicBannerItem>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedBanners != null && cachedBanners!!.isNotEmpty()) {
            return@withContext Result.success(cachedBanners!!)
        }
        try {
            val response = api.getBanners()
            val dtos = response.data ?: emptyList()
            val sliderList = dtos.map { dto ->
                val ratingFloat = when (val r = dto.rating) {
                    is Number -> r.toFloat()
                    is String -> r.toFloatOrNull()
                    else -> null
                }
                val bg = resolveCoverUrl(dto.backgroundImage, null)
                val poster = resolveCoverUrl(dto.charaImage, dto.backgroundImage)
                com.yuranime.dinz.data.model.ComicBannerItem(
                    id = dto.id?.toString().orEmpty(),
                    title = dto.title.orEmpty(),
                    synopsis = dto.detail.orEmpty(),
                    rating = ratingFloat,
                    comicId = dto.sliderLink.orEmpty(),
                    backgroundImage = bg,
                    posterImage = poster
                )
            }.filter { it.title.isNotBlank() && it.comicId.isNotBlank() }

            // Ambil juga data Rekomendasi dari komikid untuk digabung ke slider
            val recommendedList = try {
                val recResponse = api.getTrendingManga(isRecommended = true, page = 1, pageSize = 8)
                (recResponse.data ?: emptyList()).map { mangaDto ->
                    val portraitCover = resolveCoverUrl(mangaDto.coverPortraitUrl, mangaDto.coverImageUrl)
                    val landscapeCover = resolveCoverUrl(mangaDto.coverImageUrl, mangaDto.coverPortraitUrl)
                    val ratingFloat = when (val r = mangaDto.userRate) {
                        is Number -> r.toFloat()
                        is String -> r.toFloatOrNull()
                        else -> null
                    }
                    com.yuranime.dinz.data.model.ComicBannerItem(
                        id = mangaDto.mangaId.orEmpty(),
                        title = mangaDto.title.orEmpty(),
                        synopsis = mangaDto.description?.takeIf { it.isNotBlank() }
                            ?: "Ikuti kelanjutan kisah petualangan seru dan menegangkan komik rekomendasi ini sekarang juga!",
                        rating = ratingFloat,
                        comicId = mangaDto.mangaId.orEmpty(),
                        backgroundImage = landscapeCover.ifBlank { portraitCover },
                        posterImage = portraitCover.ifBlank { landscapeCover }
                    )
                }.filter { it.title.isNotBlank() && it.comicId.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }

            val combinedList = (sliderList + recommendedList)
                .distinctBy { it.comicId.ifBlank { it.title.lowercase(Locale.ROOT) } }

            if (combinedList.isNotEmpty()) {
                cachedBanners = combinedList
                Result.success(combinedList)
            } else {
                Result.success(getFallbackBanners())
            }
        } catch (_: Exception) {
            if (cachedBanners != null && cachedBanners!!.isNotEmpty()) {
                Result.success(cachedBanners!!)
            } else {
                Result.success(getFallbackBanners())
            }
        }
    }

    suspend fun getTrendingComics(
        page: Int = 1,
        pageSize: Int = 12,
        forceRefresh: Boolean = false
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && page == 1 && cachedTrendingComics != null && cachedTrendingComics!!.isNotEmpty()) {
            return@withContext Result.success(cachedTrendingComics!!)
        }
        try {
            val response = api.getTrendingManga(page = page, pageSize = pageSize)
            val dtos = response.data ?: emptyList()
            val items = dtos.map { mangaDto ->
                mapToComicItem(mangaDto, emptyList())
            }
            if (page == 1 && items.isNotEmpty()) {
                cachedTrendingComics = items
            }
            Result.success(items)
        } catch (_: Exception) {
            if (cachedTrendingComics != null && cachedTrendingComics!!.isNotEmpty()) {
                Result.success(cachedTrendingComics!!)
            } else {
                Result.success(getFallbackComics())
            }
        }
    }

    suspend fun getPopularAllTimeComics(
        page: Int = 1,
        pageSize: Int = 12,
        forceRefresh: Boolean = false
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && page == 1 && cachedPopularComics != null && cachedPopularComics!!.isNotEmpty()) {
            return@withContext Result.success(cachedPopularComics!!)
        }
        try {
            val response = api.getPopularManga(page = page, pageSize = pageSize)
            val dtos = response.data ?: emptyList()
            val items = dtos.map { mangaDto ->
                mapToComicItem(mangaDto, emptyList())
            }
            if (page == 1 && items.isNotEmpty()) {
                cachedPopularComics = items
            }
            Result.success(items)
        } catch (_: Exception) {
            if (cachedPopularComics != null && cachedPopularComics!!.isNotEmpty()) {
                Result.success(cachedPopularComics!!)
            } else {
                Result.success(getFallbackComics())
            }
        }
    }

    suspend fun searchComics(
        query: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchManga(query = query, page = page, pageSize = pageSize)
            val dtos = response.data ?: emptyList()
            val items = dtos.map { mangaDto ->
                mapToComicItem(mangaDto, emptyList())
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestComics(
        page: Int = 1,
        pageSize: Int = 12,
        forceRefresh: Boolean = false
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && page == 1 && cachedLatestComics != null) {
            return@withContext Result.success(cachedLatestComics!!)
        }

        try {
            val response = api.getLatestManga(page = page, pageSize = pageSize)
            val dtos = response.data ?: emptyList()

            val comicsWithChapters = coroutineScope {
                dtos.map { mangaDto ->
                    async {
                        val mangaId = mangaDto.mangaId.orEmpty()
                        val portraitCover = resolveCoverUrl(mangaDto.coverPortraitUrl, mangaDto.coverImageUrl)
                        var chapters = emptyList<ComicChapterItem>()

                        // Ambil 3 chapter terbaru untuk setiap komik
                        if (mangaId.isNotBlank()) {
                            try {
                                val chResponse = api.getMangaChapters(
                                    mangaId = mangaId,
                                    page = 1,
                                    pageSize = 3
                                )
                                chapters = (chResponse.data ?: emptyList()).map { ch ->
                                    val numStr = formatChapterNumber(ch.chapterNumber)
                                    val thumb = ch.thumbnailImageUrl
                                        ?.takeIf { it.isNotBlank() && !it.endsWith("default.jpg") }
                                        ?: portraitCover
                                    ComicChapterItem(
                                        id = ch.chapterId.orEmpty(),
                                        comicId = mangaId,
                                        number = numStr,
                                        title = ch.chapterTitle?.takeIf { it.isNotBlank() } ?: "Chapter $numStr",
                                        releaseTime = formatRelativeTime(ch.releaseDate),
                                        releaseDate = ch.releaseDate,
                                        formattedDate = formatShortIndoDate(ch.releaseDate),
                                        thumbnailUrl = thumb,
                                        viewCount = ch.viewCount ?: 0L
                                    )
                                }
                            } catch (_: Exception) {}
                        }

                        // Pastikan selalu memiliki tepat 3 baris chapter yang rapi dan sejajar
                        val guaranteedChapters = ensureThreeChapters(
                            fetched = chapters,
                            mangaDto = mangaDto,
                            mangaId = mangaId,
                            fallbackThumb = portraitCover
                        )

                        mapToComicItem(mangaDto, guaranteedChapters)
                    }
                }.map { it.await() }
            }

            if (page == 1 && comicsWithChapters.isNotEmpty()) {
                cachedLatestComics = comicsWithChapters
            }

            Result.success(comicsWithChapters)
        } catch (e: Exception) {
            if (cachedLatestComics != null && cachedLatestComics!!.isNotEmpty()) {
                return@withContext Result.success(cachedLatestComics!!)
            }
            Result.success(getFallbackComics())
        }
    }

    suspend fun getComicDetail(mangaId: String): Result<ComicDetail> = withContext(Dispatchers.IO) {
        cachedDetails[mangaId]?.let { return@withContext Result.success(it) }

        if (mangaId.contains("komikstation.org") || mangaId.startsWith("ks:")) {
            val res = komikStationScraper.getComicDetail(mangaId)
            if (res.isSuccess) {
                res.getOrNull()?.let { cachedDetails[mangaId] = it }
            }
            return@withContext res
        }

        try {
            val response = api.getMangaDetail(mangaId)
            val dto = response.data ?: throw Exception("Data komik tidak ditemukan")
            val portraitCover = resolveCoverUrl(dto.coverPortraitUrl, dto.coverImageUrl)
            val landscapeCover = resolveCoverUrl(dto.coverImageUrl, dto.coverPortraitUrl)

            // Ambil daftar lengkap chapter (hingga 100 chapter)
            val chResponse = api.getMangaChapters(mangaId = mangaId, page = 1, pageSize = 100)
            val chapters = (chResponse.data ?: emptyList()).map { ch ->
                val numStr = formatChapterNumber(ch.chapterNumber)
                val thumb = ch.thumbnailImageUrl
                    ?.takeIf { it.isNotBlank() && !it.endsWith("default.jpg") }
                    ?: portraitCover
                ComicChapterItem(
                    id = ch.chapterId.orEmpty(),
                    comicId = mangaId,
                    number = numStr,
                    title = ch.chapterTitle?.takeIf { it.isNotBlank() } ?: "Chapter $numStr",
                    releaseTime = formatRelativeTime(ch.releaseDate),
                    releaseDate = ch.releaseDate,
                    formattedDate = formatShortIndoDate(ch.releaseDate),
                    thumbnailUrl = thumb,
                    viewCount = ch.viewCount ?: 0L
                )
            }

            val format = detectFormat(dto)
            val countryCode = dto.countryId?.uppercase(Locale.ROOT) ?: "JP"
            val rating = when (val r = dto.userRate) {
                is Number -> r.toFloat()
                is String -> r.toFloatOrNull()
                else -> null
            }

            val isOngoing = dto.status?.toString() == "1" || dto.status?.toString()?.lowercase() == "ongoing"
            val statusStr = if (isOngoing) "ongoing" else "completed"

            val genres = extractTaxonomyNames(dto.taxonomy, "Genre").ifEmpty {
                listOf("Action", "Adventure", "Fantasy")
            }
            val authors = extractTaxonomyNames(dto.taxonomy, "Author")
            val artists = extractTaxonomyNames(dto.taxonomy, "Artist")

            val airedStartStr = formatShortIndoDate(dto.createdAt, fallback = dto.releaseYear ?: "-")
            val airedEndStr = if (!isOngoing) formatShortIndoDate(dto.updatedAt, fallback = "-") else "-"

            val detail = ComicDetail(
                id = dto.mangaId.orEmpty(),
                title = dto.title.orEmpty(),
                alternativeTitle = dto.alternativeTitle,
                description = dto.description,
                format = format,
                countryCode = countryCode,
                coverPortrait = portraitCover,
                coverLandscape = landscapeCover,
                rating = rating,
                viewCount = dto.viewCount ?: 0L,
                bookmarkCount = dto.bookmarkCount ?: 0L,
                status = statusStr,
                releaseYear = dto.releaseYear,
                airedStart = airedStartStr,
                airedEnd = airedEndStr,
                genres = genres,
                authors = authors,
                artists = artists,
                chapters = chapters
            )

            cachedDetails[mangaId] = detail
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChapterDetail(chapterId: String): Result<ComicChapterDetail> = withContext(Dispatchers.IO) {
        if (chapterId.contains("komikstation.org") || chapterId.startsWith("ks-ch:")) {
            return@withContext komikStationScraper.getChapterDetail(chapterId)
        }

        try {
            val response = api.getChapterDetail(chapterId)
            val data = response.data ?: throw Exception("Data chapter tidak ditemukan")

            val rawBaseUrl = data.baseUrl?.trimEnd('/') ?: "https://assets.shngm.id"
            val path = data.chapter?.path?.trim('/')?.let { "/$it/" } ?: "/"
            val fileNames = data.chapter?.data ?: emptyList()

            val pageUrls = fileNames.map { filename ->
                if (filename.startsWith("http://") || filename.startsWith("https://")) {
                    filename
                } else {
                    "$rawBaseUrl$path$filename"
                }
            }

            val result = ComicChapterDetail(
                chapterId = data.chapterId.orEmpty(),
                comicId = data.mangaId.orEmpty(),
                chapterNumber = formatChapterNumber(data.chapterNumber),
                chapterTitle = data.chapterTitle,
                prevChapterId = data.prevChapterId,
                nextChapterId = data.nextChapterId,
                pages = pageUrls
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveCoverUrl(primary: String?, secondary: String?): String {
        val candidate = primary?.trim()?.takeIf { it.isNotBlank() }
            ?: secondary?.trim()?.takeIf { it.isNotBlank() }
            ?: ""
        return when {
            candidate.startsWith("//") -> "https:$candidate"
            candidate.startsWith("/") -> "https://assets.shngm.id$candidate"
            else -> candidate
        }
    }

    private fun ensureThreeChapters(
        fetched: List<ComicChapterItem>,
        mangaDto: ShngmMangaDto,
        mangaId: String,
        fallbackThumb: String
    ): List<ComicChapterItem> {
        val result = fetched.filter { it.number.isNotBlank() }.take(3).toMutableList()

        if (result.isEmpty()) {
            val latestNum = formatChapterNumber(mangaDto.latestChapterNumber ?: 1)
            val relTime = formatRelativeTime(mangaDto.latestChapterTime)
            result.add(
                ComicChapterItem(
                    id = mangaDto.latestChapterId.orEmpty(),
                    comicId = mangaId,
                    number = latestNum,
                    title = "Chapter $latestNum",
                    releaseTime = relTime,
                    releaseDate = mangaDto.latestChapterTime,
                    formattedDate = formatShortIndoDate(mangaDto.latestChapterTime),
                    thumbnailUrl = fallbackThumb
                )
            )
        }

        while (result.size < 3) {
            val lastItem = result.last()
            val lastNumInt = lastItem.number.substringBefore(".").toIntOrNull() ?: 1
            val nextNum = (lastNumInt - 1).coerceAtLeast(1)
            val numStr = if (nextNum == lastNumInt && result.size == 2) "0" else nextNum.toString()
            val fallbackTime = if (result.size == 1) "7 days ago" else "14 days ago"
            result.add(
                ComicChapterItem(
                    id = lastItem.id,
                    comicId = mangaId,
                    number = numStr,
                    title = "Chapter $numStr",
                    releaseTime = fallbackTime,
                    formattedDate = lastItem.formattedDate.ifBlank { "-" },
                    thumbnailUrl = fallbackThumb
                )
            )
        }

        return result.take(3)
    }

    private fun mapToComicItem(dto: ShngmMangaDto, chapters: List<ComicChapterItem>): ComicItem {
        val format = detectFormat(dto)
        val country = dto.countryId?.uppercase(Locale.ROOT) ?: "JP"
        val rating = when (val r = dto.userRate) {
            is Number -> r.toFloat()
            is String -> r.toFloatOrNull()
            else -> null
        }
        val portraitCover = resolveCoverUrl(dto.coverPortraitUrl, dto.coverImageUrl)
        val landscapeCover = resolveCoverUrl(dto.coverImageUrl, dto.coverPortraitUrl)

        return ComicItem(
            id = dto.mangaId.orEmpty(),
            title = dto.title.orEmpty(),
            alternativeTitle = dto.alternativeTitle,
            description = dto.description,
            format = format,
            countryCode = country,
            coverPortrait = portraitCover,
            coverLandscape = landscapeCover,
            rating = rating,
            status = if (dto.status?.toString() == "1") "ongoing" else "completed",
            releaseYear = dto.releaseYear,
            viewCount = dto.viewCount ?: 0L,
            chapters = chapters
        )
    }

    private fun detectFormat(dto: ShngmMangaDto): String {
        val country = (dto.countryId ?: "").uppercase(Locale.ROOT)
        if (country == "KR") return "Manhwa"
        if (country == "CN") return "Manhua"
        if (country == "JP") return "Manga"

        val taxStr = dto.taxonomy.toString().lowercase(Locale.ROOT)
        return when {
            taxStr.contains("manhwa") -> "Manhwa"
            taxStr.contains("manhua") -> "Manhua"
            else -> "Manga"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractTaxonomyNames(taxonomy: Any?, key: String): List<String> {
        if (taxonomy == null) return emptyList()
        try {
            if (taxonomy is Map<*, *>) {
                val list = taxonomy[key] as? List<Map<String, Any>>
                if (list != null) {
                    return list.mapNotNull { it["name"]?.toString()?.takeIf { s -> s.isNotBlank() } }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    private fun formatChapterNumber(raw: Any?): String {
        if (raw == null) return "1"
        return when (raw) {
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            is Float -> if (raw % 1.0f == 0.0f) raw.toLong().toString() else raw.toString()
            is Number -> raw.toLong().toString()
            else -> raw.toString().ifBlank { "1" }
        }
    }

    private fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanIso = isoString.substringBefore(".").substringBefore("Z")
            val date = format.parse(cleanIso) ?: return "Just now"
            val diffMs = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
            val diffSeconds = diffMs / 1000L
            val diffMinutes = diffSeconds / 60L
            val diffHours = diffMinutes / 60L
            val diffDays = diffHours / 24L

            when {
                diffMinutes < 1 -> "Just now"
                diffMinutes == 1L -> "1 minute ago"
                diffMinutes < 60 -> "$diffMinutes minutes ago"
                diffHours == 1L -> "1 hour ago"
                diffHours < 24 -> "$diffHours hours ago"
                diffDays == 1L -> "1 day ago"
                diffDays < 30 -> "$diffDays days ago"
                diffDays < 60 -> "1 month ago"
                else -> "${diffDays / 30} months ago"
            }
        } catch (_: Exception) {
            "Just now"
        }
    }

    private fun formatShortIndoDate(isoString: String?, fallback: String = "-"): String {
        if (isoString.isNullOrBlank()) return fallback
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanIso = isoString.substringBefore(".").substringBefore("Z")
            val date = parser.parse(cleanIso) ?: return fallback
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
            val month = monthNames[cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)]
            val yearShort = (cal.get(java.util.Calendar.YEAR) % 100).toString().padStart(2, '0')
            "$day $month $yearShort"
        } catch (_: Exception) {
            fallback
        }
    }

    private fun getFallbackBanners(): List<com.yuranime.dinz.data.model.ComicBannerItem> {
        return listOf(
            com.yuranime.dinz.data.model.ComicBannerItem(
                id = "banner-1",
                title = "GOD OF MARTIAL ARTS",
                synopsis = "Setelah mengalami kematian karena kecelakaan mobil \"lin feng\" melintasi dunia dan menemukan kalau dirinya di dunia lain dalam keadaan hampir mati, karena itulah ia berhasil mendapatkan kesempatan untuk hidup kedua...",
                rating = 7.1f,
                comicId = "c0f1d049-ff7f-474d-8c6a-3a55e4c44147",
                backgroundImage = "https://assets.shngm.id/thumbnail/banner/banner_1786001844086_z7n82u.webp",
                posterImage = "https://assets.shngm.id/thumbnail/image/d00e4253-656d-481e-b3ef-701e4c6d451e.jpg"
            ),
            com.yuranime.dinz.data.model.ComicBannerItem(
                id = "banner-2",
                title = "DEMONIC EMPEROR",
                synopsis = "Zhuo Yifan menemui nasib sial karena dikhianati dan dibunuh oleh murid kepercayaannya. Setelah terlahir kembali kultivasinya kembali ke nol dan dia terjebak oleh heart demon...",
                rating = 8.6f,
                comicId = "c0f1d049-ff7f-474d-8c6a-3a55e4c44147",
                backgroundImage = "https://assets.shngm.id/thumbnail/image/d00e4253-656d-481e-b3ef-701e4c6d451e.jpg",
                posterImage = "https://assets.shngm.id/thumbnail/image/d00e4253-656d-481e-b3ef-701e4c6d451e.jpg"
            )
        )
    }

    private fun getFallbackComics(): List<ComicItem> {
        return listOf(
            ComicItem(
                id = "4751525f-359c-423a-9fdb-44d40ac8105d",
                title = "The Return Of the Crazy Demon",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/7448838a-7456-450a-bd9e-248097e92cea.jpg",
                rating = 9.5f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("c3a53439-114b-4c1b-adbb-bbeeab3cb253", "4751525f-359c-423a-9fdb-44d40ac8105d", "220", "Chapter 220", "Just now", formattedDate = "8 Nov 26", viewCount = 1240),
                    ComicChapterItem("50a93e1f-6430-4476-82fd-54d099644169", "4751525f-359c-423a-9fdb-44d40ac8105d", "219", "Chapter 219", "7 days ago", formattedDate = "1 Nov 26", viewCount = 3420),
                    ComicChapterItem("1ee6c967-ed07-40d0-a60d-e9754b76f5c5", "4751525f-359c-423a-9fdb-44d40ac8105d", "218", "Chapter 218", "18 days ago", formattedDate = "21 Okt 26", viewCount = 5190)
                )
            ),
            ComicItem(
                id = "022a0a28-cfef-42e4-90aa-ec364068792d",
                title = "The Chairman Of Pharmaceutical Company Is A Poison King",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/cover/banner_1779466348230_0p1mg6.jpg",
                rating = 8.5f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("a3d395c1-0a5f-4ca2-b013-633f59ab2c38", "022a0a28-cfef-42e4-90aa-ec364068792d", "40", "Chapter 40", "1 hour ago", formattedDate = "8 Nov 26", viewCount = 840),
                    ComicChapterItem("ch-39", "022a0a28-cfef-42e4-90aa-ec364068792d", "39", "Chapter 39", "7 days ago", formattedDate = "1 Nov 26", viewCount = 1920),
                    ComicChapterItem("ch-38", "022a0a28-cfef-42e4-90aa-ec364068792d", "38", "Chapter 38", "14 days ago", formattedDate = "25 Okt 26", viewCount = 2410)
                )
            )
        )
    }

    suspend fun getKomikStationHomeData(): Result<com.yuranime.dinz.data.model.KomikStationHomeData> {
        return komikStationScraper.getHomeData()
    }

    suspend fun getKomikStationComicsByGenre(genreSlugOrUrl: String, page: Int = 1): Result<List<ComicItem>> {
        return komikStationScraper.getComicsByGenre(genreSlugOrUrl, page)
    }
}
