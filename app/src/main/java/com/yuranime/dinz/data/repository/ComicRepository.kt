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

            val ksBannerList = if (recommendedList.isEmpty()) {
                komikStationScraper.getBannersFromTrending(limit = 6, forceRefresh = forceRefresh).getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val combinedList = (sliderList + recommendedList + ksBannerList)
                .distinctBy { it.comicId.ifBlank { it.title.lowercase(Locale.ROOT) } }

            if (combinedList.isNotEmpty()) {
                cachedBanners = combinedList
                Result.success(combinedList)
            } else {
                Result.success(getFallbackBanners())
            }
        } catch (_: Exception) {
            val ksFallback = komikStationScraper.getBannersFromTrending(limit = 6, forceRefresh = forceRefresh).getOrDefault(emptyList())
            if (ksFallback.isNotEmpty()) {
                cachedBanners = ksFallback
                return@withContext Result.success(ksFallback)
            }
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
            }.filter { it.coverPortrait.isNotBlank() }
            if (items.isNotEmpty()) {
                if (page == 1) cachedTrendingComics = items
                return@withContext Result.success(items)
            }
        } catch (_: Exception) {}

        val ksItems = komikStationScraper.getTrendingComics(limit = pageSize, forceRefresh = forceRefresh).getOrDefault(emptyList())
        if (ksItems.isNotEmpty()) {
            if (page == 1) cachedTrendingComics = ksItems
            return@withContext Result.success(ksItems)
        }

        val fallbackSnapshot = cachedTrendingComics
        if (!fallbackSnapshot.isNullOrEmpty()) {
            Result.success(fallbackSnapshot)
        } else {
            Result.success(getFallbackComics())
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
            }.filter { it.coverPortrait.isNotBlank() }
            if (items.isNotEmpty()) {
                if (page == 1) cachedPopularComics = items
                return@withContext Result.success(items)
            }
        } catch (_: Exception) {}

        val ksItems = komikStationScraper.getPopularComics(limit = pageSize, forceRefresh = forceRefresh).getOrDefault(emptyList())
        if (ksItems.isNotEmpty()) {
            if (page == 1) cachedPopularComics = ksItems
            return@withContext Result.success(ksItems)
        }

        val fallbackSnapshot = cachedPopularComics
        if (!fallbackSnapshot.isNullOrEmpty()) {
            Result.success(fallbackSnapshot)
        } else {
            Result.success(getFallbackComics())
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
            }.filter { it.coverPortrait.isNotBlank() }
            if (items.isNotEmpty()) {
                return@withContext Result.success(items)
            }
        } catch (_: Exception) {}

        val ksRes = komikStationScraper.searchComics(query = query, page = page)
        if (ksRes.isSuccess) {
            return@withContext ksRes
        }
        Result.success(emptyList())
    }

    suspend fun getLatestComics(
        page: Int = 1,
        pageSize: Int = 12,
        forceRefresh: Boolean = false
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        val snapshot = cachedLatestComics
        if (!forceRefresh && page == 1 && !snapshot.isNullOrEmpty() && snapshot.size >= pageSize.coerceAtMost(12)) {
            return@withContext Result.success(snapshot.take(pageSize))
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

        // Sumber Live Mirror: KomikStation Rilisan Terbaru (30 komik dengan 3 chapter & cover 210x300)
        val ksLatest = komikStationScraper.getLatestComics(
            limit = pageSize.coerceAtLeast(12),
            forceRefresh = forceRefresh
        ).getOrDefault(emptyList())
        if (ksLatest.isNotEmpty()) {
            val normalized = ksLatest.map { item ->
                if (item.chapters.size >= 3) {
                    item
                } else {
                    val padded = item.chapters.toMutableList()
                    while (padded.size < 3) {
                        val lastNum = padded.lastOrNull()?.number?.substringBefore(".")?.toIntOrNull() ?: 1
                        val nextNum = (lastNum - 1).coerceAtLeast(1)
                        padded.add(
                            ComicChapterItem(
                                id = padded.lastOrNull()?.id.orEmpty(),
                                comicId = item.id,
                                number = nextNum.toString(),
                                title = "Chapter $nextNum",
                                releaseTime = if (padded.size == 1) "1 hari lalu" else "3 hari lalu",
                                thumbnailUrl = item.coverPortrait
                            )
                        )
                    }
                    item.copy(chapters = padded.take(3))
                }
            }
            if (page == 1) {
                cachedLatestComics = normalized
            }
            return@withContext Result.success(normalized.take(pageSize))
        }

        val fallbackLatest = cachedLatestComics
        if (!fallbackLatest.isNullOrEmpty() && fallbackLatest.size >= 12) {
            return@withContext Result.success(fallbackLatest.take(pageSize))
        }
        Result.success(getFallbackComics().take(pageSize))
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
                id = "https://komikstation.org/manga/overgeared-2020/",
                title = "Overgeared (2020)",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2020/04/Overgeared-2020-222x300.jpg",
                rating = 8.9f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/overgeared-2020/", "349", "Chapter 349", "Baru saja"),
                    ComicChapterItem("", "https://komikstation.org/manga/overgeared-2020/", "348", "Chapter 348", "7 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/overgeared-2020/", "347", "Chapter 347", "14 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/yuan-zun/",
                title = "Yuan Zun",
                format = "Manhua",
                countryCode = "CN",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2018/03/Yuan-Zun-225x300.png",
                rating = 8.6f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/yuan-zun/", "652.5", "Chapter 652.5", "1 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/yuan-zun/", "652", "Chapter 652", "3 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/yuan-zun/", "651", "Chapter 651", "6 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/fairy-tail-100-years-quest/",
                title = "Fairy Tail: 100 Years Quest",
                format = "Manga",
                countryCode = "JP",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2018/04/Fairy-Tail-100-Years-Quest-201x300.jpg",
                rating = 8.7f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/fairy-tail-100-years-quest/", "223", "Chapter 223", "2 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/fairy-tail-100-years-quest/", "222", "Chapter 222", "14 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/fairy-tail-100-years-quest/", "221", "Chapter 221", "28 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/hitojichi-tsuma-wa-rikon-shitai/",
                title = "Hitojichi Tsuma wa Rikon Shitai",
                format = "Manga",
                countryCode = "JP",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/Hitojichi-Tsuma-wa-Rikon-Shitai-212x300.png",
                rating = 8.2f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/hitojichi-tsuma-wa-rikon-shitai/", "9", "Chapter 9", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/hitojichi-tsuma-wa-rikon-shitai/", "8", "Chapter 8", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/hitojichi-tsuma-wa-rikon-shitai/", "7", "Chapter 7", "4 jam lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/the-bored-duke-and-the-iron-divorcee/",
                title = "The Bored Duke and the Iron Divorcee",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/The-Bored-Duke-and-the-Iron-Divorcee-207x300.jpg",
                rating = 8.4f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/the-bored-duke-and-the-iron-divorcee/", "6", "Chapter 6", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/the-bored-duke-and-the-iron-divorcee/", "5", "Chapter 5", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/the-bored-duke-and-the-iron-divorcee/", "4", "Chapter 4", "4 jam lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/the-baby-demon-king-has-too-many-dads/",
                title = "The Baby Demon King Has Too Many Dads",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/The-Baby-Demon-King-Has-Too-Many-Dads-210x300.jpg",
                rating = 8.8f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/the-baby-demon-king-has-too-many-dads/", "25", "Chapter 25", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/the-baby-demon-king-has-too-many-dads/", "24", "Chapter 24", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/the-baby-demon-king-has-too-many-dads/", "23", "Chapter 23", "4 jam lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/ao-ashi/",
                title = "Ao Ashi",
                format = "Manga",
                countryCode = "JP",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2023/03/Ao-Ashi-212x300.jpg",
                rating = 8.4f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/ao-ashi/", "390", "Chapter 390", "1 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/ao-ashi/", "389", "Chapter 389", "7 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/ao-ashi/", "388", "Chapter 388", "14 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/a-king-is-born-on-the-road/",
                title = "A King Is Born on the Road",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/A-King-Is-Born-on-the-Road-210x300.jpg",
                rating = 8.5f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/a-king-is-born-on-the-road/", "18", "Chapter 18", "1 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/a-king-is-born-on-the-road/", "17", "Chapter 17", "5 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/a-king-is-born-on-the-road/", "16", "Chapter 16", "10 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/cyberpunk-swordsman/",
                title = "Cyberpunk Swordsman",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/Cyberpunk-Swordsman-207x300.jpg",
                rating = 8.6f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/cyberpunk-swordsman/", "24", "Chapter 24", "1 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/cyberpunk-swordsman/", "23", "Chapter 23", "7 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/cyberpunk-swordsman/", "22", "Chapter 22", "14 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/reincarnated-as-the-archdukes-son/",
                title = "Reincarnated as the Archduke's Son",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2025/12/Reincarnated-as-the-Archdukes-Son-231x300.jpg",
                rating = 8.7f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/reincarnated-as-the-archdukes-son/", "45", "Chapter 45", "2 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/reincarnated-as-the-archdukes-son/", "44", "Chapter 44", "9 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/reincarnated-as-the-archdukes-son/", "43", "Chapter 43", "16 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/money-keeps-piling-up-no-matter-how-much-i-spend/",
                title = "Money Keeps Piling Up No Matter How Much I Spend",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/06/Money-Keeps-Piling-Up-No-Matter-How-Much-I-Spend-207x300.jpg",
                rating = 7.8f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/money-keeps-piling-up-no-matter-how-much-i-spend/", "52", "Chapter 52", "2 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/money-keeps-piling-up-no-matter-how-much-i-spend/", "51", "Chapter 51", "8 hari lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/money-keeps-piling-up-no-matter-how-much-i-spend/", "50", "Chapter 50", "15 hari lalu")
                )
            ),
            ComicItem(
                id = "https://komikstation.org/manga/surviving-as-the-devils-child/",
                title = "Surviving as the Devil's Child",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://komikstation.org/wp-content/uploads/2026/11/Surviving-as-the-Devils-Child-210x300.jpg",
                rating = 8.5f,
                status = "ongoing",
                chapters = listOf(
                    ComicChapterItem("", "https://komikstation.org/manga/surviving-as-the-devils-child/", "10", "Chapter 10", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/surviving-as-the-devils-child/", "9", "Chapter 9", "4 jam lalu"),
                    ComicChapterItem("", "https://komikstation.org/manga/surviving-as-the-devils-child/", "8", "Chapter 8", "4 jam lalu")
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
        val mappedFormat = when (format?.lowercase(Locale.ROOT)) {
            "manhwa" -> "manhwa"
            "manhua" -> "manhua"
            "manga" -> "manga"
            else -> null
        }
        val mappedStatus = when (status?.lowercase(Locale.ROOT)) {
            "berlanjut", "ongoing" -> "ongoing"
            "tamat", "completed" -> "completed"
            "hiatus" -> "hiatus"
            else -> null
        }
        val mappedSort = when (sort?.lowercase(Locale.ROOT)) {
            "populer", "popular" -> "popular"
            else -> "latest"
        }

        val comicidItems = try {
            val response = api.getMangaList(
                query = query?.takeIf { it.isNotBlank() },
                sort = mappedSort,
                status = mappedStatus,
                format = mappedFormat,
                genre = genre?.takeIf { it.isNotBlank() && !it.equals("semua", true) },
                page = page,
                pageSize = pageSize
            )
            (response.data ?: emptyList())
                .map { mangaDto -> mapToComicItem(mangaDto, emptyList()) }
                .filter { it.coverPortrait.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }

        val ksItems = komikStationScraper.getFilteredComics(
            query = query?.takeIf { it.isNotBlank() },
            format = mappedFormat,
            genre = genre?.takeIf { it.isNotBlank() && !it.equals("semua", true) },
            status = mappedStatus,
            sort = mappedSort,
            page = page
        ).getOrDefault(emptyList())

        val combined = (comicidItems + ksItems)
            .distinctBy { it.title.trim().lowercase(Locale.ROOT) }

        if (combined.isNotEmpty()) {
            Result.success(combined)
        } else {
            val fallbackCombined = cachedLatestComics?.takeIf { it.isNotEmpty() } ?: getFallbackComics()
            Result.success(fallbackCombined)
        }
    }
}
