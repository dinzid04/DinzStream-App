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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ComicRepository(
    private val api: ComicApiService = ComicApiService.create()
) {
    // In-memory cache for fast repeat access
    private var cachedLatestComics: List<ComicItem>? = null
    private val cachedDetails = mutableMapOf<String, ComicDetail>()

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
                        var chapters = emptyList<ComicChapterItem>()

                        // Fetch 3 latest chapters for each comic
                        if (mangaId.isNotBlank()) {
                            try {
                                val chResponse = api.getMangaChapters(
                                    mangaId = mangaId,
                                    page = 1,
                                    pageSize = 3
                                )
                                chapters = (chResponse.data ?: emptyList()).map { ch ->
                                    val numStr = formatChapterNumber(ch.chapterNumber)
                                    ComicChapterItem(
                                        id = ch.chapterId.orEmpty(),
                                        comicId = mangaId,
                                        number = numStr,
                                        title = ch.chapterTitle ?: "Chapter $numStr",
                                        releaseTime = formatRelativeTime(ch.releaseDate),
                                        releaseDate = ch.releaseDate
                                    )
                                }
                            } catch (ignored: Exception) {}
                        }

                        // Fallback if chapter list fetch is empty but latestChapter exists in DTO
                        if (chapters.isEmpty() && mangaDto.latestChapterNumber != null) {
                            val num = formatChapterNumber(mangaDto.latestChapterNumber)
                            chapters = listOf(
                                ComicChapterItem(
                                    id = mangaDto.latestChapterId.orEmpty(),
                                    comicId = mangaId,
                                    number = num,
                                    title = "Chapter $num",
                                    releaseTime = formatRelativeTime(mangaDto.latestChapterTime),
                                    releaseDate = mangaDto.latestChapterTime
                                )
                            )
                        }

                        mapToComicItem(mangaDto, chapters)
                    }
                }.map { it.await() }
            }

            if (page == 1 && comicsWithChapters.isNotEmpty()) {
                cachedLatestComics = comicsWithChapters
            }

            Result.success(comicsWithChapters)
        } catch (e: Exception) {
            // If network fails, return cached or fallback comics
            if (cachedLatestComics != null && cachedLatestComics!!.isNotEmpty()) {
                return@withContext Result.success(cachedLatestComics!!)
            }
            Result.success(getFallbackComics())
        }
    }

    suspend fun getComicDetail(mangaId: String): Result<ComicDetail> = withContext(Dispatchers.IO) {
        cachedDetails[mangaId]?.let { return@withContext Result.success(it) }

        try {
            val response = api.getMangaDetail(mangaId)
            val dto = response.data ?: throw Exception("Data komik tidak ditemukan")

            // Fetch all chapters (up to 100)
            val chResponse = api.getMangaChapters(mangaId = mangaId, page = 1, pageSize = 100)
            val chapters = (chResponse.data ?: emptyList()).map { ch ->
                val numStr = formatChapterNumber(ch.chapterNumber)
                ComicChapterItem(
                    id = ch.chapterId.orEmpty(),
                    comicId = mangaId,
                    number = numStr,
                    title = ch.chapterTitle?.ifBlank { "Chapter $numStr" } ?: "Chapter $numStr",
                    releaseTime = formatRelativeTime(ch.releaseDate),
                    releaseDate = ch.releaseDate
                )
            }

            val format = detectFormat(dto)
            val countryCode = dto.countryId?.uppercase(Locale.ROOT) ?: "JP"
            val rating = when (val r = dto.userRate) {
                is Number -> r.toFloat()
                is String -> r.toFloatOrNull()
                else -> null
            }

            val detail = ComicDetail(
                id = dto.mangaId.orEmpty(),
                title = dto.title.orEmpty(),
                alternativeTitle = dto.alternativeTitle,
                description = dto.description,
                format = format,
                countryCode = countryCode,
                coverPortrait = dto.coverPortraitUrl.orEmpty(),
                coverLandscape = dto.coverImageUrl,
                rating = rating,
                status = if (dto.status?.toString() == "1") "Berjalan (Ongoing)" else "Tamat (Completed)",
                releaseYear = dto.releaseYear,
                genres = extractGenres(dto.taxonomy),
                authors = emptyList(),
                artists = emptyList(),
                chapters = chapters
            )

            cachedDetails[mangaId] = detail
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChapterDetail(chapterId: String): Result<ComicChapterDetail> = withContext(Dispatchers.IO) {
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

    private fun mapToComicItem(dto: ShngmMangaDto, chapters: List<ComicChapterItem>): ComicItem {
        val format = detectFormat(dto)
        val country = dto.countryId?.uppercase(Locale.ROOT) ?: "JP"
        val rating = when (val r = dto.userRate) {
            is Number -> r.toFloat()
            is String -> r.toFloatOrNull()
            else -> null
        }

        return ComicItem(
            id = dto.mangaId.orEmpty(),
            title = dto.title.orEmpty(),
            alternativeTitle = dto.alternativeTitle,
            description = dto.description,
            format = format,
            countryCode = country,
            coverPortrait = dto.coverPortraitUrl.orEmpty(),
            coverLandscape = dto.coverImageUrl,
            rating = rating,
            status = if (dto.status?.toString() == "1") "Ongoing" else "Completed",
            releaseYear = dto.releaseYear,
            chapters = chapters
        )
    }

    private fun detectFormat(dto: ShngmMangaDto): String {
        val country = (dto.countryId ?: "").uppercase(Locale.ROOT)
        // Check country or taxonomy
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
    private fun extractGenres(taxonomy: Any?): List<String> {
        if (taxonomy == null) return emptyList()
        try {
            if (taxonomy is Map<*, *>) {
                val genreList = taxonomy["Genre"] as? List<Map<String, Any>>
                if (genreList != null) {
                    return genreList.mapNotNull { it["name"]?.toString() }
                }
            }
        } catch (ignored: Exception) {}
        return listOf("Action", "Adventure", "Fantasy")
    }

    private fun formatChapterNumber(raw: Any?): String {
        if (raw == null) return "0"
        return when (raw) {
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            is Float -> if (raw % 1.0f == 0.0f) raw.toLong().toString() else raw.toString()
            is Number -> raw.toLong().toString()
            else -> raw.toString()
        }
    }

    private fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleanIso = isoString.substringBefore(".").substringBefore("Z")
            val date = format.parse(cleanIso) ?: return "Baru rilis"
            val diffMs = System.currentTimeMillis() - date.time
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
        } catch (e: Exception) {
            "Baru rilis"
        }
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
                status = "Ongoing",
                chapters = listOf(
                    ComicChapterItem("c3a53439-114b-4c1b-adbb-bbeeab3cb253", "4751525f-359c-423a-9fdb-44d40ac8105d", "220", "Chapter 220", "Just now"),
                    ComicChapterItem("50a93e1f-6430-4476-82fd-54d099644169", "4751525f-359c-423a-9fdb-44d40ac8105d", "219", "Chapter 219", "7 days ago"),
                    ComicChapterItem("1ee6c967-ed07-40d0-a60d-e9754b76f5c5", "4751525f-359c-423a-9fdb-44d40ac8105d", "218", "Chapter 218", "18 days ago")
                )
            ),
            ComicItem(
                id = "022a0a28-cfef-42e4-90aa-ec364068792d",
                title = "The Chairman Of Pharmaceutical Company Is A Poison King",
                format = "Manhwa",
                countryCode = "KR",
                coverPortrait = "https://assets.shngm.id/thumbnail/cover/banner_1779466348230_0p1mg6.jpg",
                rating = 8.5f,
                status = "Ongoing",
                chapters = listOf(
                    ComicChapterItem("a3d395c1-0a5f-4ca2-b013-633f59ab2c38", "022a0a28-cfef-42e4-90aa-ec364068792d", "40", "Chapter 40", "1 hour ago"),
                    ComicChapterItem("ch-39", "022a0a28-cfef-42e4-90aa-ec364068792d", "39", "Chapter 39", "7 days ago"),
                    ComicChapterItem("ch-38", "022a0a28-cfef-42e4-90aa-ec364068792d", "38", "Chapter 38", "14 days ago")
                )
            ),
            ComicItem(
                id = "c4d08129-e853-41bb-ad68-e5a9ee356555",
                title = "Houkago Bokura Wa Uchuu Ni Madou",
                format = "Manga",
                countryCode = "JP",
                coverPortrait = "https://assets.shngm.id/thumbnail/cover/banner_1779466334721_bpfjjv.jpg",
                rating = 8.8f,
                status = "Ongoing",
                chapters = listOf(
                    ComicChapterItem("ch-28", "c4d08129-e853-41bb-ad68-e5a9ee356555", "28", "Chapter 28", "Just now"),
                    ComicChapterItem("ch-27", "c4d08129-e853-41bb-ad68-e5a9ee356555", "27", "Chapter 27", "4 minutes ago"),
                    ComicChapterItem("ch-26", "c4d08129-e853-41bb-ad68-e5a9ee356555", "26", "Chapter 26", "1 minute ago")
                )
            ),
            ComicItem(
                id = "b5a93214-9f81-4e89-bd12-bb71891a2211",
                title = "Mr. Zombie",
                format = "Manhua",
                countryCode = "CN",
                coverPortrait = "https://assets.shngm.id/thumbnail/cover/banner_1780414157788_wew5eq.jpg",
                rating = 8.2f,
                status = "Ongoing",
                chapters = listOf(
                    ComicChapterItem("ch-184", "b5a93214-9f81-4e89-bd12-bb71891a2211", "184", "Chapter 184", "1 hour ago"),
                    ComicChapterItem("ch-183", "b5a93214-9f81-4e89-bd12-bb71891a2211", "183", "Chapter 183", "1 day ago"),
                    ComicChapterItem("ch-182", "b5a93214-9f81-4e89-bd12-bb71891a2211", "182", "Chapter 182", "2 days ago")
                )
            )
        )
    }
}
