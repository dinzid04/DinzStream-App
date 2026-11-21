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
    @Volatile
    private var cachedBanners: List<com.yuranime.dinz.data.model.ComicBannerItem>? = null
    @Volatile
    private var cachedTrendingComics: List<ComicItem>? = null
    @Volatile
    private var cachedPopularComics: List<ComicItem>? = null
    @Volatile
    private var cachedLatestComics: List<ComicItem>? = null
    private val cachedDetails = java.util.Collections.synchronizedMap(mutableMapOf<String, ComicDetail>())

    suspend fun getBanners(forceRefresh: Boolean = false): Result<List<com.yuranime.dinz.data.model.ComicBannerItem>> = withContext(Dispatchers.IO) {
        val snapshot = cachedBanners
        if (!forceRefresh && !snapshot.isNullOrEmpty()) {
            return@withContext Result.success(snapshot)
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
            val fallbackSnapshot = cachedBanners
            if (!fallbackSnapshot.isNullOrEmpty()) {
                Result.success(fallbackSnapshot)
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
        val snapshot = cachedTrendingComics
        if (!forceRefresh && page == 1 && !snapshot.isNullOrEmpty()) {
            return@withContext Result.success(snapshot)
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
            val fallbackSnapshot = cachedTrendingComics
            if (!fallbackSnapshot.isNullOrEmpty()) {
                Result.success(fallbackSnapshot)
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
        val snapshot = cachedPopularComics
        if (!forceRefresh && page == 1 && !snapshot.isNullOrEmpty()) {
            return@withContext Result.success(snapshot)
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
            val fallbackSnapshot = cachedPopularComics
            if (!fallbackSnapshot.isNullOrEmpty()) {
                Result.success(fallbackSnapshot)
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
        val snapshot = cachedLatestComics
        if (!forceRefresh && page == 1 && !snapshot.isNullOrEmpty() && snapshot.size >= 12) {
            return@withContext Result.success(snapshot)
        }

        try {
            val response = api.getLatestManga(page = page, pageSize = pageSize)
            val dtos = response.data ?: emptyList()

            if (dtos.isNotEmpty()) {
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

                return@withContext Result.success(comicsWithChapters)
            }
        } catch (_: Exception) {}

        // Fallback 1: KomikStation Latest
        try {
            val ksRes = komikStationScraper.getHomeData()
            if (ksRes.isSuccess) {
                val ksData = ksRes.getOrNull()
                val candidateList = ksData?.latestComics?.ifEmpty { null }
                    ?: ksData?.popularToday?.ifEmpty { null }
                    ?: ksData?.seriesTabs?.firstOrNull()?.items
                if (!candidateList.isNullOrEmpty()) {
                    val converted = candidateList.take(12).map { card ->
                        ComicItem(
                            id = card.id,
                            title = card.title,
                            format = card.format.ifBlank { "Manhwa" },
                            countryCode = when (card.format.lowercase()) {
                                "manhua" -> "CN"
                                "manga" -> "JP"
                                else -> "KR"
                            },
                            coverPortrait = card.coverUrl,
                            rating = 8.5f,
                            status = "ongoing",
                            chapters = card.latestChapters.take(3).map { ch ->
                                ComicChapterItem(
                                    id = ch.chapterId,
                                    comicId = card.id,
                                    number = ch.chapterNumber,
                                    title = "Chapter ${ch.chapterNumber}",
                                    releaseTime = ch.releaseTime,
                                    releaseDate = ch.releaseDate,
                                    formattedDate = ch.releaseTime,
                                    thumbnailUrl = card.coverUrl
                                )
                            }
                        )
                    }
                    if (converted.size >= 6) {
                        return@withContext Result.success(converted)
                    }
                }
            }
        } catch (_: Exception) {}

        val fallbackLatest = cachedLatestComics
        if (!fallbackLatest.isNullOrEmpty() && fallbackLatest.size >= 12) {
            return@withContext Result.success(fallbackLatest)
        }
        Result.success(getFallbackComics())
    }

    suspend fun getComicDetail(mangaId: String): Result<ComicDetail> = withContext(Dispatchers.IO) {
        val cleanId = mangaId.trim().removeSurrounding("\"").removeSurrounding("'")
        cachedDetails[cleanId]?.let { return@withContext Result.success(it) }

        if (cleanId.contains("komikstation.org") || cleanId.startsWith("ks:") || cleanId.startsWith("http")) {
            val res = komikStationScraper.getComicDetail(cleanId)
            if (res.isSuccess) {
                res.getOrNull()?.let { cachedDetails[cleanId] = it }
                return@withContext res
            }
        }

        try {
            val response = api.getMangaDetail(cleanId)
            val dto = response.data ?: throw Exception("Data komik tidak ditemukan")
            val portraitCover = resolveCoverUrl(dto.coverPortraitUrl, dto.coverImageUrl)
            val landscapeCover = resolveCoverUrl(dto.coverImageUrl, dto.coverPortraitUrl)

            // Ambil daftar lengkap chapter (hingga 100 chapter)
            val chResponse = api.getMangaChapters(mangaId = cleanId, page = 1, pageSize = 100)
            val chapters = (chResponse.data ?: emptyList()).map { ch ->
                val numStr = formatChapterNumber(ch.chapterNumber)
                val thumb = ch.thumbnailImageUrl
                    ?.takeIf { it.isNotBlank() && !it.endsWith("default.jpg") }
                    ?: portraitCover
                ComicChapterItem(
                    id = ch.chapterId.orEmpty(),
                    comicId = cleanId,
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
            val serialization = extractTaxonomyNames(dto.taxonomy, "Serialization").firstOrNull()
                ?: extractTaxonomyNames(dto.taxonomy, "Magazine").firstOrNull()

            val releaseYear = dto.releaseYear?.takeIf { it.isNotBlank() && it != "-" }
            val updatedOnStr = formatShortIndoDate(dto.updatedAt, fallback = "").takeIf { it.isNotBlank() && it != "-" }
            val airedStartStr = formatShortIndoDate(dto.createdAt, fallback = "").takeIf { it.isNotBlank() && it != "-" }
                ?: (releaseYear ?: "")
            val airedEndStr = if (!isOngoing) formatShortIndoDate(dto.updatedAt, fallback = "") else ""

            val detail = ComicDetail(
                id = dto.mangaId.orEmpty(),
                title = dto.title.orEmpty(),
                alternativeTitle = dto.alternativeTitle?.takeIf { it.isNotBlank() && it != "-" },
                description = dto.description,
                format = format,
                countryCode = countryCode,
                coverPortrait = portraitCover,
                coverLandscape = landscapeCover,
                rating = rating,
                viewCount = dto.viewCount ?: 0L,
                bookmarkCount = dto.bookmarkCount ?: 0L,
                status = statusStr,
                releaseYear = releaseYear,
                airedStart = airedStartStr,
                airedEnd = airedEndStr,
                serialization = serialization,
                updatedOn = updatedOnStr,
                genres = genres,
                authors = authors,
                artists = artists,
                chapters = chapters
            )

            cachedDetails[cleanId] = detail
            Result.success(detail)
        } catch (e: Exception) {
            // AUTOMATIC FALLBACK MIRROR: Jika terjadi HTTP 432 / Cloudflare / 404, coba cari di KomikStation
            try {
                val searchQuery = cleanId.removePrefix("ks:").removePrefix("manga-")
                    .substringAfterLast("/")
                    .replace("-", " ")
                    .trim()
                if (searchQuery.isNotBlank()) {
                    val searchRes = komikStationScraper.searchComics(searchQuery)
                    val first = searchRes.getOrNull()?.firstOrNull()
                    if (first != null) {
                        val fbDetailRes = komikStationScraper.getComicDetail(first.id)
                        if (fbDetailRes.isSuccess) {
                            val fbDetail = fbDetailRes.getOrNull()
                            if (fbDetail != null) {
                                cachedDetails[cleanId] = fbDetail
                                return@withContext Result.success(fbDetail)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    suspend fun getChapterDetail(chapterId: String): Result<ComicChapterDetail> = withContext(Dispatchers.IO) {
        val cleanChapterId = chapterId.trim()
        if (cleanChapterId.contains("komikstation.org") || cleanChapterId.startsWith("ks-ch:") || cleanChapterId.startsWith("http")) {
            return@withContext komikStationScraper.getChapterDetail(cleanChapterId)
        }

        try {
            val response = api.getChapterDetail(cleanChapterId)
            val data = response.data ?: throw Exception("Data chapter tidak ditemukan")

            val rawBaseUrl = data.baseUrl?.trimEnd('/') ?: "https://assets.shngm.id"
            val path = data.chapter?.path?.trim('/')?.let { "/$it/" } ?: "/"
            val fileNames = data.chapter?.data ?: emptyList()

            val pageUrls = fileNames.map { filename ->
                val cleanName = filename.trim()
                when {
                    cleanName.startsWith("http://") || cleanName.startsWith("https://") -> cleanName
                    cleanName.startsWith("/") -> "$rawBaseUrl$cleanName"
                    else -> "$rawBaseUrl$path$cleanName"
                }
            }

            if (pageUrls.isEmpty()) {
                val ksFallback = komikStationScraper.getChapterDetail(cleanChapterId)
                if (ksFallback.isSuccess) {
                    return@withContext ksFallback
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
            // Fallback ke KomikStation jika terjadi error atau 432
            try {
                val ksResult = komikStationScraper.getChapterDetail(cleanChapterId)
                if (ksResult.isSuccess) {
                    return@withContext ksResult
                }
            } catch (_: Exception) {}
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
        val results = mutableListOf<String>()
        try {
            when (taxonomy) {
                is Map<*, *> -> {
                    for ((k, v) in taxonomy) {
                        val kStr = k?.toString().orEmpty()
                        if (kStr.equals(key, ignoreCase = true) || kStr.startsWith(key, ignoreCase = true)) {
                            when (v) {
                                is List<*> -> {
                                    v.forEach { item ->
                                        if (item is Map<*, *>) {
                                            val name = item["name"]?.toString()
                                                ?: item["title"]?.toString()
                                                ?: item["taxonomy_name"]?.toString()
                                            if (!name.isNullOrBlank() && name != "-" && !name.equals("N/A", true)) {
                                                results.add(name.trim())
                                            }
                                        } else if (item is String && item.isNotBlank() && item != "-") {
                                            results.add(item.trim())
                                        }
                                    }
                                }
                                is String -> {
                                    if (v.isNotBlank() && v != "-") results.add(v.trim())
                                }
                            }
                        }
                    }
                }
                is List<*> -> {
                    taxonomy.forEach { item ->
                        if (item is Map<*, *>) {
                            val taxType = item["taxonomy"]?.toString()
                                ?: item["type"]?.toString()
                                ?: item["taxonomy_type"]?.toString()
                            if (taxType.equals(key, ignoreCase = true)) {
                                val name = item["name"]?.toString() ?: item["title"]?.toString()
                                if (!name.isNullOrBlank() && name != "-") {
                                    results.add(name.trim())
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return results.distinct()
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
            ),
            ComicItem(
                id = "ks:solo-leveling-ragnarok",
                title = "Solo Leveling: Ragnarok",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/d00e4253-656d-481e-b3ef-701e4c6d451e.jpg",
                rating = 9.8f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("slr-ch-36", "ks:solo-leveling-ragnarok", "36", "Chapter 36", "2 hours ago", formattedDate = "8 Nov 26", viewCount = 5100),
                    ComicChapterItem("slr-ch-35", "ks:solo-leveling-ragnarok", "35", "Chapter 35", "7 days ago", formattedDate = "1 Nov 26", viewCount = 9200),
                    ComicChapterItem("slr-ch-34", "ks:solo-leveling-ragnarok", "34", "Chapter 34", "14 days ago", formattedDate = "25 Okt 26", viewCount = 11400)
                )
            ),
            ComicItem(
                id = "ks:magic-emperor",
                title = "Magic Emperor",
                format = "Manhua",
                countryCode = "CN",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/8ee2f1c8-2b81-420d-b4b1-912c9c7f9999.jpg",
                rating = 9.4f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("me-ch-625", "ks:magic-emperor", "625", "Chapter 625", "3 hours ago", formattedDate = "8 Nov 26", viewCount = 4200),
                    ComicChapterItem("me-ch-624", "ks:magic-emperor", "624", "Chapter 624", "3 days ago", formattedDate = "5 Nov 26", viewCount = 7600),
                    ComicChapterItem("me-ch-623", "ks:magic-emperor", "623", "Chapter 623", "7 days ago", formattedDate = "1 Nov 26", viewCount = 8900)
                )
            ),
            ComicItem(
                id = "ks:reincarnation-of-the-suicidal-battle-god",
                title = "Reincarnation of the Suicidal Battle God",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/cover/doom_breaker_banner.jpg",
                rating = 9.7f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("rsbg-ch-115", "ks:reincarnation-of-the-suicidal-battle-god", "115", "Chapter 115", "5 hours ago", formattedDate = "8 Nov 26", viewCount = 3800),
                    ComicChapterItem("rsbg-ch-114", "ks:reincarnation-of-the-suicidal-battle-god", "114", "Chapter 114", "7 days ago", formattedDate = "1 Nov 26", viewCount = 7100),
                    ComicChapterItem("rsbg-ch-113", "ks:reincarnation-of-the-suicidal-battle-god", "113", "Chapter 113", "14 days ago", formattedDate = "25 Okt 26", viewCount = 9400)
                )
            ),
            ComicItem(
                id = "ks:nano-machine",
                title = "Nano Machine",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/nano_machine_cover.jpg",
                rating = 9.6f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("nm-ch-238", "ks:nano-machine", "238", "Chapter 238", "6 hours ago", formattedDate = "8 Nov 26", viewCount = 6200),
                    ComicChapterItem("nm-ch-237", "ks:nano-machine", "237", "Chapter 237", "7 days ago", formattedDate = "1 Nov 26", viewCount = 10400),
                    ComicChapterItem("nm-ch-236", "ks:nano-machine", "236", "Chapter 236", "14 days ago", formattedDate = "25 Okt 26", viewCount = 12900)
                )
            ),
            ComicItem(
                id = "ks:omniscient-reader",
                title = "Omniscient Reader's Viewpoint",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/orv_cover_main.jpg",
                rating = 9.9f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("orv-ch-241", "ks:omniscient-reader", "241", "Chapter 241", "8 hours ago", formattedDate = "8 Nov 26", viewCount = 8900),
                    ComicChapterItem("orv-ch-240", "ks:omniscient-reader", "240", "Chapter 240", "7 days ago", formattedDate = "1 Nov 26", viewCount = 14200),
                    ComicChapterItem("orv-ch-239", "ks:omniscient-reader", "239", "Chapter 239", "14 days ago", formattedDate = "25 Okt 26", viewCount = 18100)
                )
            ),
            ComicItem(
                id = "ks:damn-reincarnation",
                title = "Damn Reincarnation",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/damn_reincarnation_cover.jpg",
                rating = 9.3f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("dr-ch-92", "ks:damn-reincarnation", "92", "Chapter 92", "12 hours ago", formattedDate = "8 Nov 26", viewCount = 3100),
                    ComicChapterItem("dr-ch-91", "ks:damn-reincarnation", "91", "Chapter 91", "7 days ago", formattedDate = "1 Nov 26", viewCount = 6800),
                    ComicChapterItem("dr-ch-90", "ks:damn-reincarnation", "90", "Chapter 90", "14 days ago", formattedDate = "25 Okt 26", viewCount = 8900)
                )
            ),
            ComicItem(
                id = "ks:the-greatest-estate-developer",
                title = "The Greatest Estate Developer",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/estate_developer_cover.jpg",
                rating = 9.9f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("ged-ch-170", "ks:the-greatest-estate-developer", "170", "Chapter 170", "1 day ago", formattedDate = "7 Nov 26", viewCount = 7800),
                    ComicChapterItem("ged-ch-169", "ks:the-greatest-estate-developer", "169", "Chapter 169", "7 days ago", formattedDate = "1 Nov 26", viewCount = 13500),
                    ComicChapterItem("ged-ch-168", "ks:the-greatest-estate-developer", "168", "Chapter 168", "14 days ago", formattedDate = "25 Okt 26", viewCount = 16200)
                )
            ),
            ComicItem(
                id = "ks:pick-me-up-infinite-gacha",
                title = "Pick Me Up, Infinite Gacha",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/pick_me_up_cover.jpg",
                rating = 9.6f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("pmu-ch-120", "ks:pick-me-up-infinite-gacha", "120", "Chapter 120", "1 day ago", formattedDate = "7 Nov 26", viewCount = 4900),
                    ComicChapterItem("pmu-ch-119", "ks:pick-me-up-infinite-gacha", "119", "Chapter 119", "7 days ago", formattedDate = "1 Nov 26", viewCount = 9100),
                    ComicChapterItem("pmu-ch-118", "ks:pick-me-up-infinite-gacha", "118", "Chapter 118", "14 days ago", formattedDate = "25 Okt 26", viewCount = 11300)
                )
            ),
            ComicItem(
                id = "ks:revenge-of-iron-blooded-sword-hound",
                title = "Revenge of the Iron-Blooded Sword Hound",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/sword_hound_cover.jpg",
                rating = 9.5f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("sh-ch-98", "ks:revenge-of-iron-blooded-sword-hound", "98", "Chapter 98", "2 days ago", formattedDate = "6 Nov 26", viewCount = 4100),
                    ComicChapterItem("sh-ch-97", "ks:revenge-of-iron-blooded-sword-hound", "97", "Chapter 97", "8 days ago", formattedDate = "31 Okt 26", viewCount = 8200),
                    ComicChapterItem("sh-ch-96", "ks:revenge-of-iron-blooded-sword-hound", "96", "Chapter 96", "15 days ago", formattedDate = "24 Okt 26", viewCount = 10100)
                )
            ),
            ComicItem(
                id = "ks:martial-peak",
                title = "Martial Peak",
                format = "Manhua",
                countryCode = "CN",
                coverPortrait = "https://assets.shngm.id/thumbnail/image/martial_peak_cover.jpg",
                rating = 9.2f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("mp-ch-3820", "ks:martial-peak", "3820", "Chapter 3820", "2 days ago", formattedDate = "6 Nov 26", viewCount = 3300),
                    ComicChapterItem("mp-ch-3819", "ks:martial-peak", "3819", "Chapter 3819", "4 days ago", formattedDate = "4 Nov 26", viewCount = 5900),
                    ComicChapterItem("mp-ch-3818", "ks:martial-peak", "3818", "Chapter 3818", "6 days ago", formattedDate = "2 Nov 26", viewCount = 7400)
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

    suspend fun getFilteredComics(
        query: String? = null,
        format: String? = null,
        genre: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1,
        pageSize: Int = 24
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val mappedFormat = when (format?.lowercase(Locale.ROOT)) {
                "manhwa" -> "manhwa"
                "manhua" -> "manhua"
                "manga" -> "manga"
                else -> null
            }
            val mappedStatus = when (status?.lowercase(Locale.ROOT)) {
                "berlanjut", "ongoing" -> "ongoing"
                "tamat", "completed" -> "completed"
                else -> null
            }
            val mappedSort = when (sort?.lowercase(Locale.ROOT)) {
                "populer", "popular" -> "popular"
                else -> "latest"
            }

            val response = api.getMangaList(
                query = query?.takeIf { it.isNotBlank() },
                sort = mappedSort,
                status = mappedStatus,
                format = mappedFormat,
                genre = genre?.takeIf { it.isNotBlank() && !it.equals("semua", true) },
                page = page,
                pageSize = pageSize
            )
            val dtos = response.data ?: emptyList()
            val comicidItems = dtos.map { mangaDto ->
                mapToComicItem(mangaDto, emptyList())
            }

            val ksItems = if (!query.isNullOrBlank()) {
                komikStationScraper.searchComics(query, page).getOrDefault(emptyList())
            } else if (!genre.isNullOrBlank() && !genre.equals("semua", true)) {
                komikStationScraper.getComicsByGenre(genre, page).getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val combined = (comicidItems + ksItems)
                .distinctBy { it.title.trim().lowercase(Locale.ROOT) }

            Result.success(combined)
        } catch (e: Exception) {
            val fallbackCombined = cachedLatestComics
            if (!fallbackCombined.isNullOrEmpty()) {
                Result.success(fallbackCombined)
            } else {
                Result.failure(e)
            }
        }
    }
}
