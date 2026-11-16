package com.yuranime.dinz.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuranime.dinz.data.local.AnimeExternalCacheStore
import com.yuranime.dinz.data.model.ExternalAnimeMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Resolver otomatis lintas bahasa (Romaji / Inggris / Sinonim / Indonesia fansub tags)
 * yang mencocokkan judul anime lokal ke ID standar internasional:
 * AniList ID, MyAnimeList ID, TMDB ID + Season, AniDB ID, dan Kitsu ID.
 */
object AnimeIdentityResolver {

    private const val ANILIST_ENDPOINT = "https://graphql.anilist.co"
    private const val JIKAN_SEARCH_ENDPOINT = "https://api.jikan.moe/v4/anime"
    private const val YUNA_RELATIONS_ENDPOINT = "https://relations.yuna.moe/api/v2/ids"
    private const val ANIZIP_MAPPINGS_ENDPOINT = "https://api.ani.zip/mappings"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    @Volatile private var jikanSearchCooldownUntilMs: Long = 0L

    val externalHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private data class CandidateAnime(
        val anilistId: Int?,
        val malId: Int?,
        val titles: List<String>,
        val displayTitle: String,
        val format: String?,
        val year: Int?,
        val episodes: Int?,
        val popularity: Int
    )

    private val SEARCH_ANILIST_QUERY = """
        query (${'$'}search: String) {
          Page(page: 1, perPage: 12) {
            media(search: ${'$'}search, type: ANIME, sort: SEARCH_MATCH) {
              id
              idMal
              title {
                romaji
                english
                native
              }
              synonyms
              format
              seasonYear
              startDate { year }
              episodes
              popularity
            }
          }
        }
    """.trimIndent()

    suspend fun resolveMapping(
        title: String,
        synonymsRaw: String? = null,
        yearHint: String? = null,
        airedStartHint: String? = null,
        expectedEpisodeCount: Int? = null,
        formatHint: String? = null,
        movieId: String? = null,
        forceRefresh: Boolean = false
    ): ExternalAnimeMapping? = withContext(Dispatchers.IO) {
        val cleanedTitle = cleanAnimeTitle(title)
        if (cleanedTitle.isBlank()) return@withContext null

        val localYear = extractYear(yearHint, airedStartHint)
        val seasonHint = extractSeasonNumber(title)
        val cacheKey = buildCacheKey(movieId, cleanedTitle, seasonHint, localYear)

        if (!forceRefresh) {
            AnimeExternalCacheStore.getMapping(cacheKey)?.let { cached ->
                if (cached.anilistId != null || cached.malId != null || cached.tmdbId != null) {
                    return@withContext cached
                }
                return@withContext null
            }
        }

        val localSynonyms = parseLocalSynonyms(synonymsRaw)
        val searchQueries = buildSearchQueries(cleanedTitle, seasonHint, localSynonyms)

        val candidatePool = mutableListOf<CandidateAnime>()
        for (q in searchQueries) {
            val found = searchAniList(q)
            candidatePool.addAll(found)
            val topPreviewScore = found.maxOfOrNull {
                scoreCandidate(
                    candidate = it,
                    localTitle = cleanedTitle,
                    localSynonyms = localSynonyms,
                    localYear = localYear,
                    localEpisodeCount = expectedEpisodeCount,
                    localFormatHint = formatHint,
                    localSeasonHint = seasonHint
                )
            } ?: 0f
            if (topPreviewScore >= 0.80f) break
        }

        var bestCandidate: CandidateAnime? = null
        var bestScore = 0f

        for (candidate in candidatePool.distinctBy { "${it.anilistId ?: 0}_${it.malId ?: 0}" }) {
            val score = scoreCandidate(
                candidate = candidate,
                localTitle = cleanedTitle,
                localSynonyms = localSynonyms,
                localYear = localYear,
                localEpisodeCount = expectedEpisodeCount,
                localFormatHint = formatHint,
                localSeasonHint = seasonHint
            )
            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        // Fallback ke Jikan v4 jika AniList belum memberi kecocokan tinggi
        if (bestScore < 0.72f) {
            for (q in searchQueries.take(2)) {
                val jikanCandidates = searchJikan(q)
                for (candidate in jikanCandidates) {
                    val score = scoreCandidate(
                        candidate = candidate,
                        localTitle = cleanedTitle,
                        localSynonyms = localSynonyms,
                        localYear = localYear,
                        localEpisodeCount = expectedEpisodeCount,
                        localFormatHint = formatHint,
                        localSeasonHint = seasonHint
                    )
                    if (score > bestScore) {
                        bestScore = score
                        bestCandidate = candidate
                    }
                }
                if (bestScore >= 0.80f) break
            }
        }

        if (bestCandidate == null || bestScore < 0.44f) {
            val missEntry = ExternalAnimeMapping(
                cacheKey = cacheKey,
                localTitle = cleanedTitle,
                matchedTitle = "",
                seasonHint = seasonHint,
                confidence = bestScore
            )
            AnimeExternalCacheStore.putMapping(missEntry)
            return@withContext null
        }

        var finalMapping = ExternalAnimeMapping(
            cacheKey = cacheKey,
            localTitle = cleanedTitle,
            matchedTitle = bestCandidate.displayTitle,
            anilistId = bestCandidate.anilistId,
            malId = bestCandidate.malId,
            seasonHint = seasonHint,
            confidence = bestScore.coerceIn(0f, 1f)
        )

        finalMapping = enrichCrossIds(finalMapping)
        AnimeExternalCacheStore.putMapping(finalMapping)
        finalMapping
    }

    private suspend fun searchAniList(query: String): List<CandidateAnime> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val payload = JsonObject().apply {
            addProperty("query", SEARCH_ANILIST_QUERY)
            add("variables", JsonObject().apply { addProperty("search", query) })
        }
        val body = gson.toJson(payload).toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url(ANILIST_ENDPOINT)
            .post(body)
            .header("Accept", "application/json")
            .build()

        runCatching {
            externalHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return@use emptyList()
                val root = JsonParser.parseString(text).asJsonObject
                val mediaArr = root.getAsJsonObject("data")
                    ?.getAsJsonObject("Page")
                    ?.getAsJsonArray("media")
                    ?: return@use emptyList()

                mediaArr.mapNotNull { elem ->
                    parseAniListMediaCandidate(elem.asJsonObject)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun parseAniListMediaCandidate(obj: JsonObject): CandidateAnime? {
        val id = obj.intOrNull("id") ?: return null
        val malId = obj.intOrNull("idMal")
        val titleObj = obj.objOrNull("title")
        val romaji = titleObj?.strOrNull("romaji")
        val english = titleObj?.strOrNull("english")
        val native = titleObj?.strOrNull("native")
        val synonyms = obj.arrOrNull("synonyms")
            ?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString?.trim() }
            .orEmpty()

        val titles = listOfNotNull(romaji, english, native).plus(synonyms)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val displayTitle = romaji?.takeIf { it.isNotBlank() }
            ?: english?.takeIf { it.isNotBlank() }
            ?: native
            ?: return null

        val year = obj.intOrNull("seasonYear")
            ?: obj.objOrNull("startDate")?.intOrNull("year")

        return CandidateAnime(
            anilistId = id,
            malId = malId,
            titles = titles,
            displayTitle = displayTitle,
            format = obj.strOrNull("format"),
            year = year,
            episodes = obj.intOrNull("episodes"),
            popularity = obj.intOrNull("popularity") ?: 0
        )
    }

    private fun searchJikan(query: String): List<CandidateAnime> {
        if (query.isBlank() || System.currentTimeMillis() < jikanSearchCooldownUntilMs) return emptyList()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$JIKAN_SEARCH_ENDPOINT?q=$encoded&limit=8"
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        return try {
            externalHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (resp.code >= 500 || resp.code == 429) {
                        jikanSearchCooldownUntilMs = System.currentTimeMillis() + 5L * 60L * 1000L
                    }
                    return emptyList()
                }
                val text = resp.body?.string().orEmpty()
                if (text.isBlank()) return emptyList()
                val root = JsonParser.parseString(text).asJsonObject
                val dataArr = root.getAsJsonArray("data") ?: return emptyList()

                dataArr.mapNotNull { elem ->
                    val obj = elem.asJsonObject
                    val malId = obj.intOrNull("mal_id") ?: return@mapNotNull null
                    val defaultTitle = obj.strOrNull("title")
                    val english = obj.strOrNull("title_english")
                    val japanese = obj.strOrNull("title_japanese")
                    val synonyms = obj.arrOrNull("title_synonyms")
                        ?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString?.trim() }
                        .orEmpty()

                    val titles = listOfNotNull(defaultTitle, english, japanese).plus(synonyms)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val display = defaultTitle ?: english ?: return@mapNotNull null
                    val year = obj.intOrNull("year")
                        ?: obj.objOrNull("aired")?.objOrNull("prop")?.objOrNull("from")?.intOrNull("year")

                    CandidateAnime(
                        anilistId = null,
                        malId = malId,
                        titles = titles,
                        displayTitle = display,
                        format = obj.strOrNull("type"),
                        year = year,
                        episodes = obj.intOrNull("episodes"),
                        popularity = obj.intOrNull("members") ?: 0
                    )
                }
            }
        } catch (_: Exception) {
            jikanSearchCooldownUntilMs = System.currentTimeMillis() + 5L * 60L * 1000L
            emptyList()
        }
    }

    private fun enrichCrossIds(initial: ExternalAnimeMapping): ExternalAnimeMapping {
        var current = initial

        // 1. Yuna Relations v2 (AniList <-> MAL <-> TMDB + Season <-> AniDB)
        val yunaUrl = when {
            current.anilistId != null -> "$YUNA_RELATIONS_ENDPOINT?source=anilist&id=${current.anilistId}"
            current.malId != null -> "$YUNA_RELATIONS_ENDPOINT?source=myanimelist&id=${current.malId}"
            else -> null
        }
        if (yunaUrl != null) {
            runCatching {
                val req = Request.Builder().url(yunaUrl).get().header("Accept", "application/json").build()
                externalHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val text = resp.body?.string().orEmpty()
                        if (text.isNotBlank()) {
                            val obj = JsonParser.parseString(text).asJsonObject
                            val tmdbSeason = parseFirstPositiveInt(obj.get("themoviedb-season"))
                            current = current.copy(
                                anilistId = current.anilistId ?: obj.intOrNull("anilist"),
                                malId = current.malId ?: obj.intOrNull("myanimelist"),
                                tmdbId = current.tmdbId ?: obj.intOrNull("themoviedb"),
                                tmdbSeason = current.tmdbSeason ?: tmdbSeason,
                                anidbId = current.anidbId ?: obj.intOrNull("anidb"),
                                kitsuId = current.kitsuId ?: obj.intOrNull("kitsu")
                            )
                        }
                    }
                }
            }
        }

        // 2. api.ani.zip fallback bila masih ada ID yang kurang
        if (current.malId == null || current.tmdbId == null || current.anidbId == null || current.anilistId == null) {
            val anizipUrl = when {
                current.anilistId != null -> "$ANIZIP_MAPPINGS_ENDPOINT?anilist_id=${current.anilistId}"
                current.malId != null -> "$ANIZIP_MAPPINGS_ENDPOINT?mal_id=${current.malId}"
                else -> null
            }
            if (anizipUrl != null) {
                runCatching {
                    val req = Request.Builder().url(anizipUrl).get().header("Accept", "application/json").build()
                    externalHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val text = resp.body?.string().orEmpty()
                            if (text.isNotBlank()) {
                                val root = JsonParser.parseString(text).asJsonObject
                                val maps = root.objOrNull("mappings") ?: return@use
                                current = current.copy(
                                    anilistId = current.anilistId ?: maps.intOrNull("anilist_id"),
                                    malId = current.malId ?: maps.intOrNull("mal_id"),
                                    tmdbId = current.tmdbId ?: parseFirstPositiveInt(maps.get("themoviedb_id")),
                                    anidbId = current.anidbId ?: maps.intOrNull("anidb_id"),
                                    kitsuId = current.kitsuId ?: maps.intOrNull("kitsu_id")
                                )
                            }
                        }
                    }
                }
            }
        }

        return current
    }

    private fun scoreCandidate(
        candidate: CandidateAnime,
        localTitle: String,
        localSynonyms: List<String>,
        localYear: Int?,
        localEpisodeCount: Int?,
        localFormatHint: String?,
        localSeasonHint: Int?
    ): Float {
        val localForms = listOf(localTitle).plus(localSynonyms)
            .map { cleanAnimeTitle(it) }
            .filter { it.isNotBlank() }
            .distinct()

        var bestTextScore = 0f
        for (lf in localForms) {
            val lfNorm = normalizeForCompare(lf)
            val lfFold = foldHepburn(lfNorm)
            val lfTokens = tokenize(lfNorm)

            for (candTitle in candidate.titles) {
                val cfNorm = normalizeForCompare(cleanAnimeTitle(candTitle))
                if (cfNorm.isBlank()) continue
                val cfFold = foldHepburn(cfNorm)

                val s = when {
                    lfNorm == cfNorm -> 0.80f
                    lfFold == cfFold -> 0.77f
                    else -> {
                        val cfTokens = tokenize(cfNorm)
                        val jaccard = tokenJaccard(lfTokens, cfTokens)
                        val containment = if (
                            (lfNorm.length >= 6 && cfNorm.contains(lfNorm)) ||
                            (cfNorm.length >= 6 && lfNorm.contains(cfNorm))
                        ) 0.16f else 0f
                        val levenshteinBonus = normalizedLevenshteinSimilarity(lfFold, cfFold) * 0.24f
                        (jaccard * 0.52f + containment + levenshteinBonus).coerceAtMost(0.74f)
                    }
                }
                if (s > bestTextScore) bestTextScore = s
            }
        }

        if (bestTextScore < 0.20f) return bestTextScore

        var score = bestTextScore

        // Year verification
        if (localYear != null && candidate.year != null) {
            val diff = abs(localYear - candidate.year)
            score += when {
                diff == 0 -> 0.11f
                diff == 1 -> 0.05f
                diff >= 3 -> -0.16f
                else -> -0.05f
            }
        }

        // Episode count verification
        if (localEpisodeCount != null && localEpisodeCount > 0 && candidate.episodes != null && candidate.episodes > 0) {
            val epDiff = abs(localEpisodeCount - candidate.episodes)
            score += when {
                epDiff == 0 -> 0.10f
                epDiff <= 2 -> 0.05f
                localEpisodeCount <= 2 && candidate.episodes >= 10 -> -0.18f
                localEpisodeCount >= 10 && candidate.episodes <= 2 -> -0.18f
                else -> -0.02f
            }
        }

        // Season parity verification (hindari Season 2 nyasar ke Season 1, tapi dukung sub-judul resmi seperti Yuukaku-hen)
        if (localSeasonHint != null && localSeasonHint >= 2) {
            val candSeasonMatches = candidate.titles.any {
                extractSeasonNumber(it) == localSeasonHint
            }
            val candHasExplicitOtherSeason = candidate.titles.any {
                val s = extractSeasonNumber(it)
                s != null && s != localSeasonHint
            }
            val yearSupportsSequel = localYear != null && candidate.year != null && abs(localYear - candidate.year) <= 1
            score += when {
                candSeasonMatches -> 0.14f
                candHasExplicitOtherSeason -> -0.25f
                yearSupportsSequel -> 0.04f
                else -> -0.14f
            }
        } else {
            // Jika query lokal TIDAK menyebut Season >= 2, penalti kandidat yang jelas-jelas Season 2/3/OVA jika ada Season 1
            val candHighestSeason = candidate.titles.mapNotNull { extractSeasonNumber(it) }.firstOrNull()
            if (candHighestSeason != null && candHighestSeason >= 2) {
                score -= 0.08f
            }
        }

        // Format alignment (Movie vs Series)
        val localFmt = localFormatHint?.uppercase(Locale.ROOT).orEmpty()
        val candFmt = candidate.format?.uppercase(Locale.ROOT).orEmpty()
        if (localFmt.isNotBlank() && candFmt.isNotBlank()) {
            val localMovie = localFmt.contains("MOVIE") || localFmt.contains("FILM")
            val candMovie = candFmt.contains("MOVIE")
            if (localMovie && candMovie) score += 0.05f
            else if (localMovie != candMovie && (localFmt.contains("SERIES") || localFmt.contains("TV") || localMovie)) {
                score -= 0.08f
            }
        }

        // Popularity micro-tiebreaker (maks 0.03f agar tidak mengalahkan kecocokan judul/season)
        if (candidate.popularity > 10) {
            score += (log10(candidate.popularity.toDouble()).toFloat() / 220f).coerceIn(0f, 0.03f)
        }

        return score.coerceIn(0f, 1.15f)
    }

    private fun buildSearchQueries(
        cleanedTitle: String,
        seasonHint: Int?,
        synonyms: List<String>
    ): List<String> {
        val list = mutableListOf<String>()
        list.add(cleanedTitle)
        if (seasonHint != null) {
            val strippedSeason = cleanedTitle
                .replace(Regex("(?i)\\b(season\\s*\\d+|s\\d+|\\d+(st|nd|rd|th)\\s*season|part\\s*\\d+)\\b"), "")
                .replace(Regex("\\s{2,}"), " ")
                .trim()
            if (strippedSeason.length >= 3 && !strippedSeason.equals(cleanedTitle, ignoreCase = true)) {
                list.add("$strippedSeason $seasonHint")
                list.add(strippedSeason)
            }
        }
        synonyms
            .map { cleanAnimeTitle(it) }
            .filter { it.length >= 3 && it.any { ch -> ch in 'a'..'z' || ch in 'A'..'Z' } }
            .take(2)
            .forEach { list.add(it) }
        return list.distinct()
    }

    private fun buildCacheKey(
        movieId: String?,
        cleanedTitle: String,
        seasonHint: Int?,
        year: Int?
    ): String {
        val normTitle = normalizeForCompare(cleanedTitle).take(64)
        val moviePart = movieId?.trim()?.takeIf { it.isNotBlank() } ?: "no_id"
        return "$moviePart|$normTitle|s${seasonHint ?: 1}|y${year ?: 0}"
    }

    fun cleanAnimeTitle(raw: String): String {
        return raw
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("(?i)\\((sub\\s*indo|subtitle\\s*indonesia|dub|bd|uncensored|batch|tamat|tv|ona|ova)\\)"), " ")
            .replace(Regex("(?i)\\b(sub\\s*indo|subtitle\\s*indonesia|batch|tamat)\\b"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun parseLocalSynonyms(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(',', ';', '|', '/')
            .map { it.trim() }
            .filter { it.length >= 3 && it != "-" }
            .distinct()
    }

    private fun extractYear(vararg candidates: String?): Int? {
        val yearRegex = Regex("(19\\d{2}|20[0-3]\\d)")
        for (c in candidates) {
            if (c.isNullOrBlank()) continue
            val match = yearRegex.find(c)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (match != null) return match
        }
        return null
    }

    fun extractSeasonNumber(title: String): Int? {
        val patterns = listOf(
            Regex("(?i)\\bseason\\s*(\\d{1,2})\\b"),
            Regex("(?i)\\b(\\d{1,2})(?:st|nd|rd|th)\\s*season\\b"),
            Regex("(?i)\\bs(\\d{1,2})\\b")
        )
        for (regex in patterns) {
            val found = regex.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (found != null && found in 1..20) return found
        }
        val romanTrailing = Regex("\\s+(II|III|IV|V)\\s*$", RegexOption.IGNORE_CASE).find(title)
            ?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT)
        return when (romanTrailing) {
            "II" -> 2
            "III" -> 3
            "IV" -> 4
            "V" -> 5
            else -> null
        }
    }

    private fun normalizeForCompare(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun foldHepburn(normalized: String): String {
        return normalized
            .replace("ou", "o")
            .replace("uu", "u")
            .replace("oo", "o")
    }

    private fun tokenize(normalized: String): Set<String> {
        val stopWords = setOf("the", "of", "no", "wa", "ni", "wo", "ga", "to", "a", "an", "in")
        val tokens = normalized.split(' ').filter { it.isNotBlank() }
        val filtered = tokens.filter { it !in stopWords }.toSet()
        return filtered.ifEmpty { tokens.toSet() }
    }

    private fun tokenJaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return if (union <= 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun normalizedLevenshteinSimilarity(a: String, b: String): Float {
        if (a == b) return 1f
        val maxLen = max(a.length, b.length)
        if (maxLen == 0) return 1f
        if (abs(a.length - b.length) > maxLen * 0.6f) return 0f
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            curr.copyInto(prev)
        }
        return (1f - prev[b.length].toFloat() / maxLen.toFloat()).coerceIn(0f, 1f)
    }

    private fun parseFirstPositiveInt(elem: JsonElement?): Int? {
        if (elem == null || elem.isJsonNull) return null
        if (elem.isJsonPrimitive) {
            val prim = elem.asJsonPrimitive
            if (prim.isNumber) return prim.asInt.takeIf { it > 0 }
            if (prim.isString) {
                return Regex("\\d+").find(prim.asString)?.value?.toIntOrNull()?.takeIf { it > 0 }
            }
        }
        return null
    }

    private fun JsonObject.strOrNull(key: String): String? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive) runCatching { e.asString }.getOrNull() else null
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val e = get(key) ?: return null
        return if (e.isJsonPrimitive && e.asJsonPrimitive.isNumber) runCatching { e.asInt }.getOrNull() else null
    }

    private fun JsonObject.objOrNull(key: String): JsonObject? {
        val e = get(key) ?: return null
        return if (e.isJsonObject) e.asJsonObject else null
    }

    private fun JsonObject.arrOrNull(key: String): JsonArray? {
        val e = get(key) ?: return null
        return if (e.isJsonArray) e.asJsonArray else null
    }
}
