package com.yuranime.dinz.data.repository

import com.google.gson.JsonParser
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
    }

    private fun newRequestBuilder(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", BASE_URL)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    }

    suspend fun getHomeData(): Result<KomikStationHomeData> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder(BASE_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} saat membuka komikstation.org"))
            }
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)

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
                            val img = bsx.selectFirst("img.ts-post-image") ?: bsx.selectFirst("img")
                            val posterUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                                ?: img?.attr("src")?.takeIf { it.isNotBlank() }
                                ?: ""
                            val typeSpan = bsx.selectFirst(".type")
                            val rawFormat = typeSpan?.text()?.trim() ?: "Manga"
                            val format = when {
                                rawFormat.contains("Manhwa", ignoreCase = true) -> "Manhwa"
                                rawFormat.contains("Manhua", ignoreCase = true) -> "Manhua"
                                else -> "Manga"
                            }
                            val chText = bsx.selectFirst(".epxs")?.text()?.trim() ?: ""
                            val scoreStr = bsx.selectFirst(".numscore")?.text()?.trim()
                            val score = scoreStr?.toFloatOrNull() ?: 5.0f

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
            val headings = doc.select("h3")
            val terbaruH3 = headings.find { it.text().contains("Series Terbaru", ignoreCase = true) }
            val terbaruContainer = terbaruH3?.parents()?.find { it.hasClass("releases") }?.nextElementSibling()
                ?.selectFirst(".serieslist ul")
                ?: doc.selectFirst(".serieslist ul")

            terbaruContainer?.select("li")?.forEach { li ->
                val left = li.selectFirst(".leftseries")
                val aTitle = left?.selectFirst("h2 a")
                val title = aTitle?.text()?.trim().orEmpty()
                val href = aTitle?.attr("href").orEmpty()
                val img = li.selectFirst("img.ts-post-image") ?: li.selectFirst("img")
                val posterUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }
                    ?: ""
                val genreSpans = left?.select("span")
                val genreList = genreSpans?.firstOrNull()?.select("a")?.map { it.text().trim() } ?: emptyList()
                val year = genreSpans?.lastOrNull()?.text()?.trim().orEmpty()

                if (title.isNotBlank()) {
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
                val img = li.selectFirst("img.ts-post-image") ?: li.selectFirst("img")
                val posterUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }
                    ?: ""
                val genreList = left?.select("span a")?.map { it.text().trim() } ?: emptyList()
                val score = left?.selectFirst(".numscore")?.text()?.trim()?.toFloatOrNull()

                if (title.isNotBlank()) {
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
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} saat memuat genre komik"))
            }
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, url)

            val comics = mutableListOf<ComicItem>()
            doc.select(".bsx").forEach { bsx ->
                val a = bsx.selectFirst("a")
                val title = a?.attr("title")?.takeIf { it.isNotBlank() }
                    ?: bsx.selectFirst(".tt")?.text()?.trim()
                    ?: "Komik"
                val link = a?.attr("href").orEmpty()
                val img = bsx.selectFirst("img.ts-post-image") ?: bsx.selectFirst("img")
                val cover = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                    ?: img?.attr("src")?.takeIf { it.isNotBlank() }
                    ?: ""
                val typeSpan = bsx.selectFirst(".type")
                val rawFormat = typeSpan?.text()?.trim() ?: "Manga"
                val format = when {
                    rawFormat.contains("Manhwa", ignoreCase = true) -> "Manhwa"
                    rawFormat.contains("Manhua", ignoreCase = true) -> "Manhua"
                    else -> "Manga"
                }
                val countryCode = when (format) {
                    "Manhwa" -> "KR"
                    "Manhua" -> "CN"
                    else -> "JP"
                }
                val score = bsx.selectFirst(".numscore")?.text()?.trim()?.toFloatOrNull()
                val chText = bsx.selectFirst(".epxs")?.text()?.trim() ?: ""

                comics.add(
                    ComicItem(
                        id = link,
                        title = title,
                        format = format,
                        countryCode = countryCode,
                        coverPortrait = cover,
                        rating = score,
                        status = "ongoing",
                        chapters = if (chText.isNotBlank()) {
                            listOf(
                                ComicChapterItem(
                                    id = "",
                                    number = chText.replace("Chapter", "").trim(),
                                    title = chText
                                )
                            )
                        } else emptyList()
                    )
                )
            }

            Result.success(comics)
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
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} saat memuat detail komik"))
            }
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, url)

            val rawTitle = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: "Detail Komik"
            val title = rawTitle.replace("Bahasa Indonesia", "").trim()

            val thumb = doc.selectFirst(".thumb")
            val img = thumb?.selectFirst("img") ?: doc.selectFirst("img.ts-post-image")
            val posterUrl = img?.attr("data-src")?.takeIf { it.isNotBlank() }
                ?: img?.attr("src")?.takeIf { it.isNotBlank() }
                ?: ""

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
                description = description,
                format = format,
                countryCode = countryCode,
                coverPortrait = posterUrl,
                coverLandscape = posterUrl,
                rating = ratingScore,
                status = status,
                genres = genres,
                chapters = chapters
            )

            Result.success(detail)
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
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} saat membuka chapter komik"))
            }
            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, url)

            val rawTitle = doc.selectFirst("h1.entry-title")?.text()?.trim() ?: "Baca Komik"
            val chapterNum = rawTitle.substringAfter("Chapter", "").trim().substringBefore(" ")

            // Extract from ts_reader script
            val images = mutableListOf<String>()
            var prevUrl: String? = null
            var nextUrl: String? = null

            val tsReaderMatch = Regex("""ts_reader\.run\((\{.*?\})\);""", RegexOption.DOT_MATCHES_ALL).find(html)
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
                                val u = elem.asString
                                if (u.isNotBlank() && !images.contains(u)) {
                                    images.add(u)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Fallback from DOM #readerarea img
            if (images.isEmpty()) {
                doc.select("#readerarea img").forEach { img ->
                    val src = img.attr("data-src").ifBlank { img.attr("src") }.trim()
                    if (src.isNotBlank() && !src.startsWith("data:image") && !images.contains(src)) {
                        images.add(src)
                    }
                }
            }

            // Fallback for prev/next links if not found in JSON
            if (prevUrl == null) {
                val prevA = doc.selectFirst(".nextprev a[rel=prev]") ?: doc.selectFirst("a.ch-prev-btn")
                prevUrl = prevA?.attr("href")?.takeIf { it.isNotBlank() && !it.contains("#") }
            }
            if (nextUrl == null) {
                val nextA = doc.selectFirst(".nextprev a[rel=next]") ?: doc.selectFirst("a.ch-next-btn")
                nextUrl = nextA?.attr("href")?.takeIf { it.isNotBlank() && !it.contains("#") }
            }

            val chapterDetail = ComicChapterDetail(
                chapterId = url,
                comicId = url.substringBeforeLast('/', "").substringBeforeLast('/'),
                chapterNumber = chapterNum.ifBlank { "1" },
                chapterTitle = rawTitle,
                prevChapterId = prevUrl,
                nextChapterId = nextUrl,
                pages = images
            )

            Result.success(chapterDetail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
