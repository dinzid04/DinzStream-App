package com.yuranime.dinz.data.repository

import com.google.gson.JsonParser
import com.yuranime.dinz.data.model.ComicBannerItem
import com.yuranime.dinz.data.model.ComicChapterDetail
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.ComicDetail
import com.yuranime.dinz.data.model.ComicItem
import com.yuranime.dinz.data.model.KomikStationCardItem
import com.yuranime.dinz.data.model.KomikStationGenre
import com.yuranime.dinz.data.model.KomikStationHomeData
import com.yuranime.dinz.data.model.KomikStationSeriesItem
import com.yuranime.dinz.data.model.KomikStationTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Locale
import java.util.concurrent.TimeUnit

class KomikStationScraper(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val BASE_URL = "https://komikstation.org/"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        private const val HOME_CACHE_TTL_MS = 90_000L

        private val homeFetchMutex = Mutex()
        @Volatile
        private var cachedHomeHtml: String? = null
        @Volatile
        private var cachedHomeTimestamp: Long = 0L
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    }

    private suspend fun fetchHomeDocument(forceRefresh: Boolean = false): Document {
        val now = System.currentTimeMillis()
        val existing = cachedHomeHtml
        if (!forceRefresh && !existing.isNullOrBlank() && (now - cachedHomeTimestamp) < HOME_CACHE_TTL_MS) {
            return Jsoup.parse(existing, BASE_URL)
        }
        return homeFetchMutex.withLock {
            val nowLocked = System.currentTimeMillis()
            val existingLocked = cachedHomeHtml
            if (!forceRefresh && !existingLocked.isNullOrBlank() && (nowLocked - cachedHomeTimestamp) < HOME_CACHE_TTL_MS) {
                return@withLock Jsoup.parse(existingLocked, BASE_URL)
            }
            val request = newRequestBuilder(BASE_URL).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    throw Exception("HTTP ${res.code} saat membuka komikstation.org")
                }
                val html = res.body?.string().orEmpty()
                if (html.isNotBlank()) {
                    cachedHomeHtml = html
                    cachedHomeTimestamp = System.currentTimeMillis()
                }
                Jsoup.parse(html, BASE_URL)
            }
        }
    }

    private fun isValidCoverUrl(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val trimmed = raw.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("data:")) return false
        if (lower.contains("/flags/") || lower.endsWith(".svg")) return false
        if (lower.contains("placeholder") || lower.contains("blank.gif")) return false
        return lower.startsWith("http://") || lower.startsWith("https://") || trimmed.startsWith("//") || trimmed.startsWith("/")
    }

    private fun normalizeCoverUrl(raw: String): String {
        val cleaned = raw.trim().replace("\\/", "/")
        return when {
            cleaned.startsWith("//") -> "https:$cleaned"
            cleaned.startsWith("/") -> "${BASE_URL.trimEnd('/')}$cleaned"
            else -> cleaned
        }
    }

    private fun extractCoverFromImg(img: Element?): String? {
        if (img == null) return null
        val attrs = listOf("data-src", "data-lazy-src", "data-original", "src")
        for (attr in attrs) {
            val candidate = img.attr(attr).trim()
            if (isValidCoverUrl(candidate)) {
                return normalizeCoverUrl(candidate)
            }
        }
        return null
    }

    private fun extractPosterUrl(container: Element?): String {
        if (container == null) return ""
        val prioritizedImgs = container.select("img.ts-post-image, img.wp-post-image, .imgu img, .imgseries img, .thumb img, img")
        for (img in prioritizedImgs) {
            val url = extractCoverFromImg(img)
            if (url != null) return url
        }
        // Fallback: baca tag <noscript> untuk gambar lazyload
        container.select("noscript").forEach { ns ->
            val nsDoc = Jsoup.parseBodyFragment(ns.html(), BASE_URL)
            nsDoc.select("img").forEach { img ->
                val url = extractCoverFromImg(img)
                if (url != null) return url
            }
        }
        return ""
    }

    private fun detectFormatFromElement(container: Element?, fallbackClass: String = ""): Pair<String, String> {
        val raw = buildString {
            append(container?.selectFirst(".type")?.className().orEmpty())
            append(" ")
            append(container?.selectFirst(".type")?.text().orEmpty())
            append(" ")
            append(fallbackClass)
        }
        val format = when {
            raw.contains("manhwa", ignoreCase = true) -> "Manhwa"
            raw.contains("manhua", ignoreCase = true) -> "Manhua"
            else -> "Manga"
        }
        val country = when (format) {
            "Manhwa" -> "KR"
            "Manhua" -> "CN"
            else -> "JP"
        }
        return format to country
    }

    private fun parseBsxToComicItem(bsx: Element): ComicItem? {
        val linkA = bsx.selectFirst("a") ?: return null
        val href = linkA.attr("href").trim()
        if (href.isBlank()) return null
        val title = linkA.attr("title").trim().takeIf { it.isNotBlank() }
            ?: bsx.selectFirst(".tt")?.text()?.trim()
            ?: bsx.selectFirst(".titleheading h2")?.text()?.trim()
            ?: return null
        val cover = extractPosterUrl(bsx)
        val (format, countryCode) = detectFormatFromElement(bsx)
        val score = bsx.selectFirst(".numscore")?.text()?.trim()?.toFloatOrNull()
        val chText = bsx.selectFirst(".epxs")?.text()?.trim().orEmpty()
        val chNum = chText.replace("Chapter", "", ignoreCase = true)
            .replace("Ch.", "", ignoreCase = true)
            .trim()
        val chapters = if (chNum.isNotBlank()) {
            listOf(
                ComicChapterItem(
                    id = "",
                    comicId = href,
                    number = chNum,
                    title = chText.ifBlank { "Chapter $chNum" },
                    releaseTime = "Baru saja",
                    thumbnailUrl = cover
                )
            )
        } else {
            emptyList()
        }
        return ComicItem(
            id = href,
            title = title,
            format = format,
            countryCode = countryCode,
            coverPortrait = cover,
            coverLandscape = cover,
            rating = score,
            status = "ongoing",
            chapters = chapters
        )
    }

    suspend fun getLatestComics(limit: Int = 30, forceRefresh: Boolean = false): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val doc = fetchHomeDocument(forceRefresh)
            val result = mutableListOf<ComicItem>()

            doc.select(".utao").forEach { utao ->
                val linkA = utao.selectFirst(".luf a.series") ?: utao.selectFirst(".imgu a.series") ?: utao.selectFirst("a")
                val href = linkA?.attr("href")?.trim().orEmpty()
                val title = utao.selectFirst(".luf h3")?.text()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: linkA?.attr("title")?.trim().orEmpty()
                val cover = extractPosterUrl(utao)
                if (href.isBlank() || title.isBlank() || cover.isBlank()) return@forEach

                val ul = utao.selectFirst(".luf ul")
                val (format, countryCode) = detectFormatFromElement(utao, ul?.className().orEmpty())

                val chapters = mutableListOf<ComicChapterItem>()
                utao.select(".luf ul li").take(3).forEach { li ->
                    val chA = li.selectFirst("a") ?: return@forEach
                    val chHref = chA.attr("href").trim()
                    val chRawText = chA.text().trim()
                    val chNum = chRawText
                        .replace("Chapter", "", ignoreCase = true)
                        .replace("Ch.", "", ignoreCase = true)
                        .replace("Ch", "", ignoreCase = true)
                        .trim()
                        .ifBlank { "1" }
                    val chTime = li.selectFirst("span")?.text()?.trim().orEmpty().ifBlank { "Baru saja" }
                    chapters.add(
                        ComicChapterItem(
                            id = chHref,
                            comicId = href,
                            number = chNum,
                            title = chRawText.ifBlank { "Chapter $chNum" },
                            releaseTime = chTime,
                            thumbnailUrl = cover
                        )
                    )
                }

                result.add(
                    ComicItem(
                        id = href,
                        title = title,
                        format = format,
                        countryCode = countryCode,
                        coverPortrait = cover,
                        coverLandscape = cover,
                        rating = null,
                        status = "ongoing",
                        chapters = chapters
                    )
                )
            }

            if (result.isEmpty()) {
                val fallbackRes = getFilteredComics(sort = "latest", page = 1)
                return@withContext fallbackRes.map { it.take(limit) }
            }

            Result.success(result.distinctBy { it.id }.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingComics(limit: Int = 12, forceRefresh: Boolean = false): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val doc = fetchHomeDocument(forceRefresh)
            val result = mutableListOf<ComicItem>()

            // Ambil dari section "Yang Lagi Trending!" dan "Proyek Komikstation"
            doc.select(".bixbox").forEach { box ->
                val heading = box.selectFirst(".releases h2, .releases h3")?.text().orEmpty()
                if (heading.contains("Trending", ignoreCase = true) || heading.contains("Proyek", ignoreCase = true)) {
                    box.select(".bsx").forEach { bsx ->
                        parseBsxToComicItem(bsx)?.takeIf { it.coverPortrait.isNotBlank() }?.let { item ->
                            result.add(item)
                        }
                    }
                }
            }

            if (result.size < limit) {
                doc.select(".bsx").forEach { bsx ->
                    if (result.size >= limit) return@forEach
                    parseBsxToComicItem(bsx)?.takeIf { it.coverPortrait.isNotBlank() }?.let { item ->
                        result.add(item)
                    }
                }
            }

            Result.success(result.distinctBy { it.id }.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPopularComics(limit: Int = 15, forceRefresh: Boolean = false): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val doc = fetchHomeDocument(forceRefresh)
            val result = mutableListOf<ComicItem>()

            val popHeading = doc.select("h2, h3").find { it.text().contains("Series Terpopuler", ignoreCase = true) }
            val popBox = popHeading?.parents()?.find { it.hasClass("section") || it.hasClass("bixbox") }
            val popList = popBox?.select(".serieslist.pop ul li, .serieslist ul li")
                ?: doc.select(".serieslist.pop ul li")

            popList.forEach { li ->
                val left = li.selectFirst(".leftseries") ?: return@forEach
                val aTitle = left.selectFirst("h2 a") ?: return@forEach
                val title = aTitle.text().trim()
                val href = aTitle.attr("href").trim()
                val cover = extractPosterUrl(li)
                if (title.isBlank() || href.isBlank() || cover.isBlank()) return@forEach
                val score = left.selectFirst(".numscore")?.text()?.trim()?.toFloatOrNull()
                val genresText = left.select("span a").joinToString(" ") { it.text() }
                val (format, country) = detectFormatFromElement(li, genresText)

                result.add(
                    ComicItem(
                        id = href,
                        title = title,
                        format = format,
                        countryCode = country,
                        coverPortrait = cover,
                        coverLandscape = cover,
                        rating = score,
                        status = "ongoing",
                        chapters = emptyList()
                    )
                )
            }

            if (result.size < limit) {
                val extra = getFilteredComics(sort = "popular", page = 1).getOrDefault(emptyList())
                result.addAll(extra)
            }

            Result.success(result.distinctBy { it.id }.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBannersFromTrending(limit: Int = 6, forceRefresh: Boolean = false): Result<List<ComicBannerItem>> = withContext(Dispatchers.IO) {
        getTrendingComics(limit = limit, forceRefresh = forceRefresh).map { list ->
            list.filter { it.coverPortrait.isNotBlank() }.map { comic ->
                ComicBannerItem(
                    id = "ks-banner-${comic.id.hashCode()}",
                    title = comic.title,
                    synopsis = "Baca ${comic.title} (${comic.format}) bahasa Indonesia dengan update chapter terbaru.",
                    rating = comic.rating ?: 8.5f,
                    comicId = comic.id,
                    backgroundImage = comic.coverPortrait,
                    posterImage = comic.coverPortrait
                )
            }
        }
    }

    suspend fun getFilteredComics(
        query: String? = null,
        format: String? = null,
        genre: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1
    ): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            if (!query.isNullOrBlank()) {
                return@withContext searchComics(query, page)
            }
            if (!genre.isNullOrBlank() && !genre.equals("semua", ignoreCase = true)) {
                val slug = genre.trim().lowercase(Locale.ROOT).replace(" ", "-")
                return@withContext getComicsByGenre(slug, page)
            }

            val orderParam = if (sort.equals("popular", ignoreCase = true) || sort.equals("populer", ignoreCase = true)) {
                "popular"
            } else {
                "update"
            }
            val params = mutableListOf("order=$orderParam")
            if (page > 1) params.add("page=$page")
            when (format?.lowercase(Locale.ROOT)) {
                "manhwa" -> params.add("type=manhwa")
                "manhua" -> params.add("type=manhua")
                "manga" -> params.add("type=manga")
            }
            when (status?.lowercase(Locale.ROOT)) {
                "ongoing", "berlanjut" -> params.add("status=ongoing")
                "completed", "tamat" -> params.add("status=completed")
                "hiatus" -> params.add("status=hiatus")
            }

            val url = "${BASE_URL}manga/?${params.joinToString("&")}"
            val request = newRequestBuilder(url).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${res.code} saat memuat daftar komik"))
                }
                val html = res.body?.string().orEmpty()
                val doc = Jsoup.parse(html, url)
                val comics = doc.select(".bsx").mapNotNull { parseBsxToComicItem(it) }
                    .filter { it.coverPortrait.isNotBlank() }
                Result.success(comics.distinctBy { it.id })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHomeData(forceRefresh: Boolean = false): Result<KomikStationHomeData> = withContext(Dispatchers.IO) {
        try {
            val doc = fetchHomeDocument(forceRefresh)

            // 1. Yang Nggak Kalah Seru! (Tabs & Comics)
            val tabs = mutableListOf<KomikStationTab>()
            val genContainer = doc.selectFirst(".series-gen")
            if (genContainer != null) {
                val navItems = genContainer.select("ul.nav-tabs li a")
                navItems.forEach { a ->
                    val tabId = a.attr("href").removePrefix("#").trim()
                    val tabTitle = a.text().trim()
                    val pane = genContainer.selectFirst("div#$tabId")
                    val comics = mutableListOf<KomikStationCardItem>()
                    if (pane != null) {
                        pane.select(".bsx").forEach { bsx ->
                            val linkA = bsx.selectFirst("a")
                            val href = linkA?.attr("href").orEmpty()
                            val title = linkA?.attr("title")?.takeIf { it.isNotBlank() }
                                ?: bsx.selectFirst(".tt")?.text()?.trim()
                                ?: bsx.selectFirst(".titleheading h2")?.text()?.trim()
                                ?: "Komik"
                            val posterUrl = extractPosterUrl(bsx)
                            val (format, _) = detectFormatFromElement(bsx)
                            val chText = bsx.selectFirst(".epxs")?.text()?.trim() ?: ""
                            val scoreStr = bsx.selectFirst(".numscore")?.text()?.trim()
                            val score = scoreStr?.toFloatOrNull() ?: 5.0f

                            if (href.isNotBlank() && posterUrl.isNotBlank()) {
                                comics.add(
                                    KomikStationCardItem(
                                        id = href,
                                        title = title,
                                        posterUrl = posterUrl,
                                        format = format,
                                        chapter = chText,
                                        score = score
                                    )
                                )
                            }
                        }
                    }
                    if (tabTitle.isNotBlank() && tabId.isNotBlank()) {
                        tabs.add(KomikStationTab(id = tabId, title = tabTitle, comics = comics))
                    }
                }
            }

            // 2. Genre (ul.genre li a)
            val genres = mutableListOf<KomikStationGenre>()
            val genreUl = doc.selectFirst("ul.genre")
            if (genreUl != null) {
                genreUl.select("li a").forEach { a ->
                    val name = a.text().trim()
                    val href = a.attr("href").trim()
                    val slug = href.trimEnd('/').substringAfterLast('/')
                    if (name.isNotBlank()) {
                        genres.add(KomikStationGenre(name = name, url = href, slug = slug))
                    }
                }
            }

            // 3. Series Terbaru
            val seriesTerbaru = mutableListOf<KomikStationSeriesItem>()
            val headings = doc.select("h2, h3")
            val terbaruH3 = headings.find { it.text().contains("Series Terbaru", ignoreCase = true) }
            val terbaruContainer = terbaruH3?.parents()?.find { it.hasClass("releases") }?.nextElementSibling()
                ?.selectFirst(".serieslist ul")
                ?: doc.select(".serieslist ul").lastOrNull()

            terbaruContainer?.select("li")?.forEach { li ->
                val left = li.selectFirst(".leftseries")
                val aTitle = left?.selectFirst("h2 a")
                val title = aTitle?.text()?.trim().orEmpty()
                val href = aTitle?.attr("href").orEmpty()
                val posterUrl = extractPosterUrl(li)
                val genreSpans = left?.select("span")
                val genreList = genreSpans?.firstOrNull()?.select("a")?.map { it.text().trim() } ?: emptyList()
                val year = genreSpans?.lastOrNull()?.text()?.trim().orEmpty()

                if (title.isNotBlank() && posterUrl.isNotBlank()) {
                    seriesTerbaru.add(
                        KomikStationSeriesItem(
                            id = href,
                            title = title,
                            posterUrl = posterUrl,
                            genres = genreList,
                            year = year
                        )
                    )
                }
            }

            // 4. Series Terpopuler
            val seriesTerpopuler = mutableListOf<KomikStationSeriesItem>()
            val populerH3 = headings.find { it.text().contains("Series Terpopuler", ignoreCase = true) }
            val populerContainer = populerH3?.parents()?.find { it.hasClass("releases") }?.nextElementSibling()
                ?.selectFirst(".serieslist ul")

            populerContainer?.select("li")?.forEach { li ->
                val ctr = li.selectFirst(".ctr")?.text()?.trim()?.toIntOrNull()
                val left = li.selectFirst(".leftseries")
                val aTitle = left?.selectFirst("h2 a")
                val title = aTitle?.text()?.trim().orEmpty()
                val href = aTitle?.attr("href").orEmpty()
                val posterUrl = extractPosterUrl(li)
                val genreList = left?.select("span a")?.map { it.text().trim() } ?: emptyList()
                val score = left?.selectFirst(".numscore")?.text()?.trim()?.toFloatOrNull()

                if (title.isNotBlank() && posterUrl.isNotBlank()) {
                    seriesTerpopuler.add(
                        KomikStationSeriesItem(
                            id = href,
                            title = title,
                            posterUrl = posterUrl,
                            genres = genreList,
                            year = "",
                            score = score,
                            rank = ctr
                        )
                    )
                }
            }

            Result.success(
                KomikStationHomeData(
                    tabs = tabs,
                    genres = genres,
                    seriesTerbaru = seriesTerbaru,
                    seriesTerpopuler = seriesTerpopuler
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComicsByGenre(genreSlugOrUrl: String, page: Int = 1): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val url = when {
                genreSlugOrUrl.startsWith("http://") || genreSlugOrUrl.startsWith("https://") -> {
                    if (page > 1) "${genreSlugOrUrl.trimEnd('/')}/page/$page/" else genreSlugOrUrl
                }
                else -> {
                    if (page > 1) "${BASE_URL}genres/$genreSlugOrUrl/page/$page/" else "${BASE_URL}genres/$genreSlugOrUrl/"
                }
            }

            val request = newRequestBuilder(url).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${res.code} saat memuat genre komik"))
                }
                val html = res.body?.string().orEmpty()
                val doc = Jsoup.parse(html, url)
                val comics = doc.select(".bsx").mapNotNull { parseBsxToComicItem(it) }
                    .filter { it.coverPortrait.isNotBlank() }
                Result.success(comics.distinctBy { it.id })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchComics(query: String, page: Int = 1): Result<List<ComicItem>> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = if (page > 1) "${BASE_URL}page/$page/?s=$encoded" else "${BASE_URL}?s=$encoded"
            val request = newRequestBuilder(url).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${res.code} saat mencari komik"))
                }
                val html = res.body?.string().orEmpty()
                val doc = Jsoup.parse(html, url)
                val comics = doc.select(".bsx").mapNotNull { parseBsxToComicItem(it) }
                    .filter { it.coverPortrait.isNotBlank() }
                Result.success(comics.distinctBy { it.id })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComicDetail(urlOrSlug: String): Result<ComicDetail> = withContext(Dispatchers.IO) {
        try {
            val url = when {
                urlOrSlug.startsWith("http://") || urlOrSlug.startsWith("https://") -> urlOrSlug
                urlOrSlug.startsWith("ks:") -> "${BASE_URL}manga/${urlOrSlug.removePrefix("ks:")}/"
                else -> "${BASE_URL}manga/$urlOrSlug/"
            }

            val request = newRequestBuilder(url).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${res.code} saat memuat detail komik"))
                }
                val html = res.body?.string().orEmpty()
                val doc = Jsoup.parse(html, url)

                val rawTitle = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: "Detail Komik"
                val title = rawTitle.replace("Bahasa Indonesia", "").trim()

                val thumb = doc.selectFirst(".thumb")
                val posterUrl = extractPosterUrl(thumb).ifBlank { extractPosterUrl(doc) }

                val descElem = doc.selectFirst(".entry-content-single")
                    ?: doc.selectFirst("div[itemprop=description]")
                    ?: doc.selectFirst(".entry-content")
                val description = descElem?.text()?.trim()

                val genres = doc.select(".mgen a, .seriestugenre a").map { it.text().trim() }
                    .filter { it.isNotBlank() }
                    .ifEmpty { listOf("Action", "Adventure") }

                val rawType = doc.select(".tsinfo .imptdt:contains(Type) i, .spe:contains(Type)").text().trim()
                val format = when {
                    rawType.contains("Manhwa", ignoreCase = true) || genres.any { it.equals("Manhwa", true) } -> "Manhwa"
                    rawType.contains("Manhua", ignoreCase = true) || genres.any { it.equals("Manhua", true) } -> "Manhua"
                    else -> "Manga"
                }
                val countryCode = when (format) {
                    "Manhwa" -> "KR"
                    "Manhua" -> "CN"
                    else -> "JP"
                }

                val statusText = doc.select(".tsinfo .imptdt:contains(Status) i, .spe:contains(Status)").text().trim()
                val status = if (statusText.contains("Complete", ignoreCase = true)) "completed" else "ongoing"

                val ratingScore = doc.selectFirst(".numscore, .rating-prc .num")?.text()?.trim()?.toFloatOrNull()

                val altTitle = doc.select(".desktop-tit, .alter, .spe span:contains(Alternative)").text()
                    .replace("Alternative:", "", ignoreCase = true)
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" }

                val rawAuthor = doc.select(".tsinfo .imptdt:contains(Author) i, .spe span:contains(Author) i, .spe span:contains(Author)").text()
                    .replace("Author:", "", ignoreCase = true)
                    .trim()
                val authorList = if (rawAuthor.isNotBlank() && rawAuthor != "-" && !rawAuthor.equals("N/A", true)) {
                    rawAuthor.split(",", "/", "&").map { it.trim() }.filter { it.isNotBlank() }
                } else emptyList()

                val rawArtist = doc.select(".tsinfo .imptdt:contains(Artist) i, .spe span:contains(Artist) i, .spe span:contains(Artist)").text()
                    .replace("Artist:", "", ignoreCase = true)
                    .trim()
                val artistList = if (rawArtist.isNotBlank() && rawArtist != "-" && !rawArtist.equals("N/A", true)) {
                    rawArtist.split(",", "/", "&").map { it.trim() }.filter { it.isNotBlank() }
                } else emptyList()

                val serialization = doc.select(".tsinfo .imptdt:contains(Serialization) i, .spe span:contains(Serialization) i, .spe span:contains(Serialization)").text()
                    .replace("Serialization:", "", ignoreCase = true)
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" && !it.equals("N/A", true) }

                val releasedYear = doc.select(".tsinfo .imptdt:contains(Released) i, .spe span:contains(Released) i, .spe span:contains(Released)").text()
                    .replace("Released:", "", ignoreCase = true)
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" && !it.equals("N/A", true) }

                val updatedOn = doc.select(".tsinfo .imptdt:contains(Updated) i, .spe span:contains(Updated) i, .spe span:contains(Updated)").text()
                    .replace("Updated on:", "", ignoreCase = true)
                    .replace("Updated:", "", ignoreCase = true)
                    .trim()
                    .takeIf { it.isNotBlank() && it != "-" }

                val chapters = mutableListOf<ComicChapterItem>()
                val chapterElements = doc.select("#chapterlist ul li, ul.clstyle li")
                chapterElements.forEach { li ->
                    val a = li.selectFirst("a")
                    val chUrl = a?.attr("href").orEmpty()
                    val chNumText = li.selectFirst(".chapternum")?.text()?.trim()
                        ?: a?.text()?.trim()
                        ?: "Chapter"
                    val chDate = li.selectFirst(".chapterdate")?.text()?.trim() ?: ""
                    val cleanNum = chNumText.replace("Chapter", "", ignoreCase = true).trim()

                    if (chUrl.isNotBlank()) {
                        chapters.add(
                            ComicChapterItem(
                                id = chUrl,
                                comicId = url,
                                number = cleanNum.ifBlank { "1" },
                                title = chNumText,
                                releaseTime = chDate,
                                formattedDate = chDate,
                                thumbnailUrl = posterUrl
                            )
                        )
                    }
                }

                val detail = ComicDetail(
                    id = url,
                    title = title,
                    alternativeTitle = altTitle,
                    description = description,
                    format = format,
                    countryCode = countryCode,
                    coverPortrait = posterUrl,
                    coverLandscape = posterUrl,
                    rating = ratingScore,
                    status = status,
                    releaseYear = releasedYear,
                    serialization = serialization,
                    updatedOn = updatedOn,
                    genres = genres,
                    authors = authorList,
                    artists = artistList,
                    chapters = chapters
                )

                Result.success(detail)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChapterDetail(chapterUrlOrId: String): Result<ComicChapterDetail> = withContext(Dispatchers.IO) {
        try {
            val url = when {
                chapterUrlOrId.startsWith("http://") || chapterUrlOrId.startsWith("https://") -> chapterUrlOrId
                chapterUrlOrId.startsWith("ks-ch:") -> "${BASE_URL}${chapterUrlOrId.removePrefix("ks-ch:")}/"
                else -> "${BASE_URL}$chapterUrlOrId/"
            }

            val request = newRequestBuilder(url).build()
            val response = client.newCall(request).execute()
            response.use { res ->
                if (!res.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${res.code} saat membuka chapter komik"))
                }
                val html = res.body?.string().orEmpty()
                val doc = Jsoup.parse(html, url)

                val rawTitle = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: "Baca Komik"
                val chapterNum = rawTitle.substringAfter("Chapter", "").trim().substringBefore(" ")

                val images = mutableListOf<String>()
                var prevUrl: String? = null
                var nextUrl: String? = null

                val tsReaderMatch = Regex("""ts_reader\.run\s*\(\s*(\{.*?\})\s*\)""", RegexOption.DOT_MATCHES_ALL).find(html)
                if (tsReaderMatch != null) {
                    try {
                        val jsonStr = tsReaderMatch.groupValues[1]
                        val jsonObj = JsonParser.parseString(jsonStr).asJsonObject
                        prevUrl = jsonObj.get("prevUrl")?.asString?.takeIf { it.isNotBlank() }
                        nextUrl = jsonObj.get("nextUrl")?.asString?.takeIf { it.isNotBlank() }
                        val sources = jsonObj.getAsJsonArray("sources")
                        if (sources != null && sources.size() > 0) {
                            val firstSource = sources[0].asJsonObject
                            val imagesArray = firstSource.getAsJsonArray("images")
                            if (imagesArray != null) {
                                imagesArray.forEach { elem ->
                                    val u = elem.asString.replace("\\/", "/").trim()
                                    if (u.isNotBlank() && !images.contains(u)) {
                                        images.add(u)
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (images.isEmpty()) {
                    val jsVarMatch = Regex("""(?:chapter_image|var\s+images)\s*=\s*(\[.*?\]);""", RegexOption.DOT_MATCHES_ALL).find(html)
                    if (jsVarMatch != null) {
                        try {
                            val arrayJson = jsVarMatch.groupValues[1]
                            val jsonArr = JsonParser.parseString(arrayJson).asJsonArray
                            jsonArr.forEach { elem ->
                                val u = elem.asString.replace("\\/", "/").trim()
                                if (u.isNotBlank() && !images.contains(u)) {
                                    images.add(u)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (images.isEmpty()) {
                    doc.select("#readerarea img, .main-reading-area img, .chapter-content img, #chimg-auh img, .reader-area img, .rdimage img").forEach { img ->
                        val src = img.attr("data-src")
                            .ifBlank { img.attr("data-lazy-src") }
                            .ifBlank { img.attr("data-original") }
                            .ifBlank { img.attr("src") }
                            .replace("\\/", "/")
                            .trim()

                        val isIgnored = src.isBlank() ||
                            src.startsWith("data:image") ||
                            src.contains("flags/") ||
                            src.contains("readerarea.svg") ||
                            src.contains("placeholder")

                        if (!isIgnored && !images.contains(src)) {
                            images.add(src)
                        }
                    }
                }

                if (prevUrl == null) {
                    val prevA = doc.selectFirst(".nextprev a[rel=prev]") ?: doc.selectFirst("a.ch-prev-btn")
                    prevUrl = prevA?.attr("href")?.takeIf { it.isNotBlank() && !it.contains("#") }
                }
                if (nextUrl == null) {
                    val nextA = doc.selectFirst(".nextprev a[rel=next]") ?: doc.selectFirst("a.ch-next-btn")
                    nextUrl = nextA?.attr("href")?.takeIf { it.isNotBlank() && !it.contains("#") }
                }

                val mangaUrl = doc.selectFirst(".nextprev a[href*='/manga/']")?.attr("href")
                    ?: doc.selectFirst(".allc a")?.attr("href")
                    ?: doc.selectFirst(".ts-breadcrumb li:nth-child(2) a")?.attr("href")
                    ?: url.substringBeforeLast('/', "").substringBeforeLast('/')

                val chapterDetail = ComicChapterDetail(
                    chapterId = url,
                    comicId = mangaUrl,
                    chapterNumber = chapterNum.ifBlank { "1" },
                    chapterTitle = rawTitle,
                    prevChapterId = prevUrl,
                    nextChapterId = nextUrl,
                    pages = images
                )

                Result.success(chapterDetail)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
