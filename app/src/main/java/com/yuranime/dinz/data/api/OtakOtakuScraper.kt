package com.yuranime.dinz.data.api

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class BirthdayTodayData(
    val title: String = "Yang Berulang Tahun Hari Ini",
    val dateId: String = "",
    val dateEn: String = "",
    val dateJp: String = "",
    val items: List<BirthdayCharacterItem> = emptyList()
)

data class BirthdayCharacterItem(
    val id: Int,
    val slug: String,
    val type: String,
    val name: String,
    val nameKanji: String,
    val nameEn: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val detailUrl: String
) {
    val isSeiyuu: Boolean
        get() = type == "seiyuu_staff" || detailUrl.contains("/people/view/")
}

data class BirthdayProfileAttribute(
    val label: String,
    val value: String
)

data class BirthdayVoiceActorItem(
    val name: String,
    val language: String,
    val imageUrl: String,
    val detailUrl: String
)

data class BirthdayAnimeAppearanceItem(
    val title: String,
    val role: String,
    val status: String,
    val imageUrl: String,
    val animeUrl: String
)

data class BirthdayVoicedRoleItem(
    val characterName: String,
    val characterUrl: String,
    val characterImageUrl: String,
    val role: String,
    val animeTitle: String,
    val animeUrl: String,
    val animeImageUrl: String
)

data class BirthdayCharacterDetail(
    val url: String,
    val type: String,
    val name: String,
    val nameKanji: String,
    val alias: String,
    val imageUrl: String,
    val attributes: List<BirthdayProfileAttribute>,
    val description: String,
    val voiceActors: List<BirthdayVoiceActorItem>,
    val animeList: List<BirthdayAnimeAppearanceItem>,
    val voicedRoles: List<BirthdayVoicedRoleItem>
)

object OtakOtakuScraper {

    private const val HOME_URL = "https://otakotaku.com/id"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Volatile
    private var cachedToday: BirthdayTodayData? = null
    private var cachedTimestampMs: Long = 0L
    private val detailCache = mutableMapOf<String, BirthdayCharacterDetail>()

    private fun cleanHtmlText(raw: String): String {
        if (raw.isBlank()) return ""
        val decoded = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded.replace(Regex("\\s+"), " ").trim()
    }

    private fun toFullResImage(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.replace(Regex("/thumb/\\d+x\\d+"), "")
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            return response.body?.string().orEmpty()
        }
    }

    suspend fun getTodayBirthdays(forceRefresh: Boolean = false): Result<BirthdayTodayData> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val existing = cachedToday
                if (!forceRefresh && existing != null && existing.items.isNotEmpty() && (now - cachedTimestampMs) < 15 * 60 * 1000L) {
                    return@withContext Result.success(existing)
                }

                val html = fetchHtml(HOME_URL)
                val parsed = parseHomeBirthdays(html)
                if (parsed.items.isNotEmpty()) {
                    cachedToday = parsed
                    cachedTimestampMs = now
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Data ulang tahun hari ini belum tersedia"))
                }
            } catch (e: Exception) {
                cachedToday?.let { return@withContext Result.success(it) }
                Result.failure(e)
            }
        }

    suspend fun getBirthdayDetail(url: String): Result<BirthdayCharacterDetail> =
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = url.trim()
                detailCache[cleanUrl]?.let { return@withContext Result.success(it) }

                val html = fetchHtml(cleanUrl)
                val detail = parseCharacterOrPeopleDetail(cleanUrl, html)
                detailCache[cleanUrl] = detail
                Result.success(detail)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseHomeBirthdays(html: String): BirthdayTodayData {
        val headlineRegex = Regex(
            """<h4[^>]*class="[^"]*headline[^"]*akai[^"]*"[^>]*>\s*(Yang Berulang Tahun Hari ini\s*\(([^)]+)\))""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val headlineMatch = headlineRegex.find(html)
        val rawTitle = headlineMatch?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }
            ?: "Yang Berulang Tahun Hari Ini"
        val fallbackDateId = headlineMatch?.groupValues?.getOrNull(2)?.let { cleanHtmlText(it) }.orEmpty()

        val ultahStart = html.indexOf("""id="ultah-list"""")
        val carouselStart = html.indexOf("""id="myCarousel"""")
        val ultahSection = if (ultahStart != -1 && carouselStart != -1 && carouselStart > ultahStart) {
            html.substring(ultahStart, carouselStart)
        } else {
            html
        }

        val dateId = Regex("""<div[^>]*class="left lang-s"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            .find(ultahSection)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }
            ?.ifBlank { fallbackDateId } ?: fallbackDateId

        val dateEn = Regex("""<div[^>]*class="left lang-e"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            .find(ultahSection)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }.orEmpty()

        val dateJp = Regex("""<div[^>]*class="left lang-k"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            .find(ultahSection)?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) }.orEmpty()

        data class LangEntry(val romaji: String, val kanji: String, val english: String, val fullImage: String)

        val ultahGridRegex = Regex(
            """<div class="anime-grid"[^>]*>\s*<div class="img-cover">\s*<img[^>]*data-src="([^"]+)"[^>]*>\s*</div>\s*<small class="lang-s">(.*?)</small>\s*<small class="lang-k"[^>]*>(.*?)</small>\s*<small class="lang-e"[^>]*>(.*?)</small>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val langEntries = ultahGridRegex.findAll(ultahSection).map { match ->
            LangEntry(
                fullImage = toFullResImage(match.groupValues[1]),
                romaji = cleanHtmlText(match.groupValues[2]),
                kanji = cleanHtmlText(match.groupValues[3]),
                english = cleanHtmlText(match.groupValues[4])
            )
        }.toList()

        val carouselEnd = if (carouselStart != -1) {
            val endMarker = html.indexOf("<!--/myCarousel-->", carouselStart)
            if (endMarker != -1) endMarker else (carouselStart + 25000).coerceAtMost(html.length)
        } else html.length

        val carouselSection = if (carouselStart != -1) {
            html.substring(carouselStart, carouselEnd)
        } else html

        val itemRegex = Regex(
            """<div class="anime-grid"[^>]*>\s*<a\s+href="([^"]+)"\s+title="([^"]*)"[^>]*>\s*<div class="img-cover">\s*<img[^>]*data-src="([^"]+)"[^>]*>\s*</div>\s*<small>(.*?)</small>\s*</a>\s*</div>""",
            RegexOption.DOT_MATCHES_ALL
        )

        val items = mutableListOf<BirthdayCharacterItem>()
        itemRegex.findAll(carouselSection).forEachIndexed { index, match ->
            val link = match.groupValues[1].trim()
            val titleAttr = cleanHtmlText(match.groupValues[2])
            val thumb = match.groupValues[3].trim()
            val smallText = cleanHtmlText(match.groupValues[4])

            val lang = langEntries.getOrNull(index)
            val idMatch = Regex("""/(character|people)/view/(\d+)/([^/?#\s]+)""").find(link)
            val kind = if (link.contains("/people/view/")) "seiyuu_staff" else "character"
            val parsedId = idMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: (index + 1)
            val parsedSlug = idMatch?.groupValues?.getOrNull(3).orEmpty()

            val displayName = lang?.romaji?.ifBlank { null } ?: titleAttr.ifBlank { smallText }
            val kanjiName = lang?.kanji.orEmpty()
            val enName = lang?.english?.ifBlank { null } ?: displayName
            val fullImg = lang?.fullImage?.ifBlank { null } ?: toFullResImage(thumb)

            items.add(
                BirthdayCharacterItem(
                    id = parsedId,
                    slug = parsedSlug,
                    type = kind,
                    name = displayName,
                    nameKanji = kanjiName,
                    nameEn = enName,
                    imageUrl = fullImg,
                    thumbnailUrl = thumb,
                    detailUrl = link
                )
            )
        }

        return BirthdayTodayData(
            title = rawTitle,
            dateId = dateId,
            dateEn = dateEn,
            dateJp = dateJp,
            items = items
        )
    }

    private fun parseCharacterOrPeopleDetail(url: String, html: String): BirthdayCharacterDetail {
        val isPeople = url.contains("/people/view/")

        val nameMatch = Regex("""<h3[^>]*>(.*?)</h3>""", RegexOption.DOT_MATCHES_ALL).find(html)
        val mainName = nameMatch?.groupValues?.getOrNull(1)?.let { cleanHtmlText(it) } ?: "Karakter"

        val coverRegex = Regex(
            """<div class="(?:cover-content|content-main-cover)"[^>]*>.*?<img[^>]*src="([^"]+)"[^>]*>""",
            RegexOption.DOT_MATCHES_ALL
        )
        val mainImage = coverRegex.find(html)?.groupValues?.getOrNull(1)?.let { toFullResImage(it) }.orEmpty()

        val attributes = mutableListOf<BirthdayProfileAttribute>()
        val attrMap = mutableMapOf<String, String>()

        val tableRegex = Regex("""<table class="table-detail"[^>]*>(.*?)</table>""", RegexOption.DOT_MATCHES_ALL)
        val rowRegex = Regex("""<tr[^>]*>\s*<td[^>]*>(.*?)</td>\s*<td[^>]*>(.*?)</td>\s*</tr>""", RegexOption.DOT_MATCHES_ALL)

        tableRegex.findAll(html).forEach { tableMatch ->
            val tableBody = tableMatch.groupValues[1]
            rowRegex.findAll(tableBody).forEach { rowMatch ->
                val rawLabel = rowMatch.groupValues[1]
                val rawValue = rowMatch.groupValues[2]
                if (!rawLabel.contains("Website", ignoreCase = true)) {
                    val label = cleanHtmlText(rawLabel)
                    val value = cleanHtmlText(rawValue)
                    if (label.isNotBlank() && value.isNotBlank() && value != "-") {
                        attrMap[label] = value
                        if (attributes.none { it.label.equals(label, ignoreCase = true) }) {
                            attributes.add(BirthdayProfileAttribute(label = label, value = value))
                        }
                    }
                }
            }
        }

        val desCharMatch = Regex("""<div id="des_char"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL).find(html)
        val rawDescHtml = desCharMatch?.groupValues?.getOrNull(1)
            ?: Regex("""<h4 id="h4_deskripsi"[^>]*>.*?</h4>\s*<div[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.getOrNull(1)
            ?: ""

        val cleanedDesc = if (rawDescHtml.isNotBlank()) {
            val withNewlines = rawDescHtml
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            val text = Html.fromHtml(withNewlines, Html.FROM_HTML_MODE_LEGACY).toString()
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("Tulis Deskripsi", ignoreCase = true) }
                .joinToString("\n\n")
            if (text.contains("Deskripsi belum tersedia", ignoreCase = true)) "" else text
        } else ""

        val voiceActors = mutableListOf<BirthdayVoiceActorItem>()
        val animeList = mutableListOf<BirthdayAnimeAppearanceItem>()
        val voicedRoles = mutableListOf<BirthdayVoicedRoleItem>()

        if (!isPeople) {
            val seiyuuSectionRegex = Regex(
                """<h4 class="content-header">\s*<small>\s*Pengisi Suara[^<]*</small>\s*</h4>(.*?)</div>\s*<div class="clear">""",
                RegexOption.DOT_MATCHES_ALL
            )
            val seiyuuHtml = seiyuuSectionRegex.find(html)?.groupValues?.getOrNull(1).orEmpty()
            val vaItemRegex = Regex(
                """<div class="anime-char-list"[^>]*>.*?<div class="anime-img">\s*<a href="([^"]+)"[^>]*>\s*<img src="([^"]+)"[^>]*>.*?<div class="anime-title">\s*<a[^>]*>(.*?)</a>.*?<small>(.*?)</small>""",
                RegexOption.DOT_MATCHES_ALL
            )
            vaItemRegex.findAll(seiyuuHtml).forEach { m ->
                val vaUrl = m.groupValues[1].trim()
                val vaImg = toFullResImage(m.groupValues[2])
                val vaName = cleanHtmlText(m.groupValues[3])
                val vaLang = cleanHtmlText(m.groupValues[4]).removePrefix("Bahasa:").trim().ifBlank { "Jepang" }
                if (vaName.isNotBlank()) {
                    voiceActors.add(
                        BirthdayVoiceActorItem(
                            name = vaName,
                            language = vaLang,
                            imageUrl = vaImg,
                            detailUrl = vaUrl
                        )
                    )
                }
            }

            val animeSectionRegex = Regex(
                """<h4 class="content-header">\s*<small>\s*Daftar Anime\s*</small>\s*</h4>(.*?)<h4 class="content-header">""",
                RegexOption.DOT_MATCHES_ALL
            )
            val animeSectionHtml = animeSectionRegex.find(html)?.groupValues?.getOrNull(1)
                ?: html.substringAfter("Daftar Anime", "")

            val animeItemRegex = Regex(
                """<div class="anime-char-list"[^>]*>\s*<div class="anime-img">\s*<a href="([^"]+)"[^>]*>\s*<img src="([^"]+)"[^>]*>\s*</a>\s*</div>\s*<div class="anime-detail">\s*<div class="anime-title">\s*<a[^>]*>(.*?)</a>\s*</div>\s*<div class="char-jenis-karakter">\s*<small>(.*?)</small>\s*</div>(?:\s*<div class="char-status-tayang">\s*<small>(.*?)</small>\s*</div>)?""",
                RegexOption.DOT_MATCHES_ALL
            )
            animeItemRegex.findAll(animeSectionHtml).forEach { m ->
                val aUrl = m.groupValues[1].trim()
                val aImg = toFullResImage(m.groupValues[2])
                val aTitle = cleanHtmlText(m.groupValues[3])
                val aRole = cleanHtmlText(m.groupValues[4]).ifBlank { "Karakter" }
                val aStatus = cleanHtmlText(m.groupValues.getOrElse(5) { "" })
                if (aTitle.isNotBlank()) {
                    animeList.add(
                        BirthdayAnimeAppearanceItem(
                            title = aTitle,
                            role = aRole,
                            status = aStatus,
                            imageUrl = aImg,
                            animeUrl = aUrl
                        )
                    )
                }
            }
        } else {
            val rolesSectionHtml = html.substringAfter("id=\"h4_seiyuu_karakter\"", "")
                .substringBefore("id=\"h4_peran_bersama\"", html.substringAfter("id=\"h4_seiyuu_karakter\"", ""))

            val roleRegex = Regex(
                """<div class="anime-char-list"[^>]*>\s*<div class="char-img"[^>]*>\s*<a href="([^"]+)"[^>]*>\s*<img src="([^"]+)"[^>]*>\s*</a>\s*</div>\s*<div class="char-img"[^>]*>\s*<a href="([^"]+)"[^>]*>\s*<img src="([^"]+)"[^>]*>\s*</a>\s*</div>\s*<div class="char-detail">\s*<div class="char-name">\s*<a[^>]*>(.*?)</a>\s*</div>\s*<div class="char-jenis-karakter">\s*<small[^>]*>(.*?)</small>\s*</div>\s*<div>\s*<a[^>]*>(.*?)</a>""",
                RegexOption.DOT_MATCHES_ALL
            )
            roleRegex.findAll(rolesSectionHtml).forEach { m ->
                val aUrl = m.groupValues[1].trim()
                val aImg = toFullResImage(m.groupValues[2])
                val cUrl = m.groupValues[3].trim()
                val cImg = toFullResImage(m.groupValues[4])
                val cName = cleanHtmlText(m.groupValues[5])
                val cRole = cleanHtmlText(m.groupValues[6])
                val aTitle = cleanHtmlText(m.groupValues[7])
                if (cName.isNotBlank() || aTitle.isNotBlank()) {
                    voicedRoles.add(
                        BirthdayVoicedRoleItem(
                            characterName = cName,
                            characterUrl = cUrl,
                            characterImageUrl = cImg,
                            role = cRole,
                            animeTitle = aTitle,
                            animeUrl = aUrl,
                            animeImageUrl = aImg
                        )
                    )
                    if (aTitle.isNotBlank() && animeList.none { it.title == aTitle }) {
                        animeList.add(
                            BirthdayAnimeAppearanceItem(
                                title = aTitle,
                                role = if (cName.isNotBlank()) "Seiyuu: $cName ($cRole)" else cRole,
                                status = "",
                                imageUrl = aImg,
                                animeUrl = aUrl
                            )
                        )
                    }
                }
            }
        }

        return BirthdayCharacterDetail(
            url = url,
            type = if (isPeople) "seiyuu_staff" else "character",
            name = mainName,
            nameKanji = attrMap["Jepang"].orEmpty(),
            alias = attrMap["Alias"].orEmpty(),
            imageUrl = mainImage,
            attributes = attributes,
            description = cleanedDesc,
            voiceActors = voiceActors,
            animeList = animeList,
            voicedRoles = voicedRoles
        )
    }
}
