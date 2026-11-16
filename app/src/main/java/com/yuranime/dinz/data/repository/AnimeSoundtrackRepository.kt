package com.yuranime.dinz.data.repository

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuranime.dinz.data.local.AnimeExternalCacheStore
import com.yuranime.dinz.data.model.AnimeSoundtrackResult
import com.yuranime.dinz.data.model.AnimeSoundtrackTrack
import com.yuranime.dinz.data.model.SoundtrackCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Locale

/**
 * Repository Soundtrack Anime (Opening & Ending) 100% gratis, publik, dan tanpa autentikasi/API Key.
 *
 * Alur fallback berurutan:
 * 1. Metadata OP/ED:
 *    - Utama: AnimeThemes API (api.animethemes.moe)
 *    - Fallback 1: Jikan API v4 (api.jikan.moe/v4/anime/{mal_id}/themes)
 *    - Fallback 2: Kitsu API (kitsu.io/api/edge) / AniList Public GraphQL API (graphql.anilist.co) untuk resolusi ID & judul alternatif lalu query ulang metadata lagu.
 * 2. Detail Cover & Audio Preview (per lagu yang belum lengkap):
 *    - Utama: Deezer Public Search API (api.deezer.com) -> .mp3 preview 30 dtk, cover XL, album
 *    - Fallback 1: iTunes Search API (itunes.apple.com) -> .m4a previewUrl, artwork 600px, album
 *    - Fallback 2: MusicBrainz + Cover Art Archive API -> cover art & info rilis
 */
object AnimeSoundtrackRepository {

    private const val TAG = "AnimeSoundtrackRepo"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient get() = AnimeIdentityResolver.externalHttpClient

    private data class TrackSeed(
        val id: String,
        val category: SoundtrackCategory,
        val badgeLabel: String,
        val title: String,
        val artist: String,
        val album: String? = null,
        val episodeNote: String? = null,
        val coverUrl: String? = null,
        val previewUrl: String? = null,
        val metadataSource: String
    )

    private data class EnrichmentInfo(
        val coverUrl: String? = null,
        val previewUrl: String? = null,
        val album: String? = null,
        val durationSec: Int? = null,
        val source: String = ""
    )

    suspend fun getSoundtracksForAnime(
        title: String,
        synonymsRaw: String? = null,
        yearHint: String? = null,
        airedStartHint: String? = null,
        formatHint: String? = null,
        movieId: String? = null,
        forceRefresh: Boolean = false
    ): AnimeSoundtrackResult = withContext(Dispatchers.IO) {
        val cleanedTitle = AnimeIdentityResolver.cleanAnimeTitle(title)
        if (cleanedTitle.isBlank()) return@withContext AnimeSoundtrackResult()

        val cacheKey = "${movieId ?: "no_id"}_${cleanedTitle.lowercase(Locale.ROOT)}"
        if (!forceRefresh) {
            AnimeExternalCacheStore.getSoundtrack(cacheKey)?.let { cached ->
                if (!cached.isEmpty) return@withContext cached
            }
        }

        val mapping = runCatching {
            AnimeIdentityResolver.resolveMapping(
                title = cleanedTitle,
                synonymsRaw = synonymsRaw,
                yearHint = yearHint,
                airedStartHint = airedStartHint,
                expectedEpisodeCount = null,
                formatHint = formatHint,
                movieId = movieId,
                forceRefresh = false
            )
        }.getOrNull()

        var malId = mapping?.malId
        val anilistId = mapping?.anilistId
        var kitsuId: String? = mapping?.kitsuId?.takeIf { it > 0 }?.toString()

        // 1A. Utama: AnimeThemes API (by MAL -> AniList -> Name search)
        var rawTracks = fetchFromAnimeThemes(
            malId = malId,
            anilistId = anilistId,
            searchTitle = cleanedTitle
        )

        // 1B. Fallback 1: Jikan API v4 (api.jikan.moe/v4/anime/{mal_id}/themes)
        if (rawTracks.isEmpty()) {
            if (malId == null || malId <= 0) {
                malId = resolveMalIdViaJikanSearch(cleanedTitle)
            }
            if (malId != null && malId > 0) {
                rawTracks = fetchFromJikanThemes(malId)
            }
        }

        // 1C. Fallback 2: Kitsu API + AniList Public GraphQL API untuk menemukan relasi/judul alternatif & MAL/Kitsu ID
        if (rawTracks.isEmpty()) {
            val altResolution = resolveViaAniListAndKitsu(cleanedTitle, anilistId, kitsuId)
            if (altResolution != null) {
                val resolvedMal = altResolution.malId ?: malId
                val altTitles = altResolution.altTitles
                kitsuId = kitsuId ?: altResolution.kitsuId

                // Retry AnimeThemes menggunakan AniList/MAL ID atau judul romaji/native baru
                for (candidateTitle in altTitles.distinct().take(2)) {
                    rawTracks = fetchFromAnimeThemes(
                        malId = resolvedMal,
                        anilistId = altResolution.anilistId ?: anilistId,
                        searchTitle = candidateTitle
                    )
                    if (rawTracks.isNotEmpty()) break
                }

                // Retry Jikan jika akhirnya malId ditemukan dari AniList/Kitsu
                if (rawTracks.isEmpty() && resolvedMal != null && resolvedMal > 0 && resolvedMal != malId) {
                    rawTracks = fetchFromJikanThemes(resolvedMal)
                }
            }
        }

        if (rawTracks.isEmpty()) {
            return@withContext AnimeSoundtrackResult(animeTitle = cleanedTitle)
        }

        // 2. Perkaya Cover Thumbnail, Album, & Audio Preview secara berurutan (Deezer -> iTunes -> MusicBrainz/CoverArtArchive)
        val enrichSemaphore = Semaphore(3)
        val enrichedTracks = coroutineScope {
            rawTracks.map { seed ->
                async {
                    enrichSemaphore.withPermit {
                        enrichTrackMedia(seed)
                    }
                }
            }.awaitAll()
        }

        val openings = enrichedTracks.filter { it.category == SoundtrackCategory.OPENING }
        val endings = enrichedTracks.filter { it.category == SoundtrackCategory.ENDING }

        val result = AnimeSoundtrackResult(
            animeTitle = cleanedTitle,
            openings = openings,
            endings = endings
        )
        if (!result.isEmpty) {
            AnimeExternalCacheStore.putSoundtrack(cacheKey, result)
        }
        result
    }

    // =========================================================================
    // 1A. ANIME THEMES API (UTAMA)
    // =========================================================================

    private fun fetchFromAnimeThemes(
        malId: Int?,
        anilistId: Int?,
        searchTitle: String
    ): List<TrackSeed> {
        val candidateUrls = buildList {
            if (malId != null && malId > 0) {
                add(
                    "https://api.animethemes.moe/anime?filter[has]=resources&filter[site]=MyAnimeList&filter[external_id]=$malId&include=animethemes.song.artists,animethemes.animethemeentries"
                )
            }
            if (anilistId != null && anilistId > 0) {
                add(
                    "https://api.animethemes.moe/anime?filter[has]=resources&filter[site]=AniList&filter[external_id]=$anilistId&include=animethemes.song.artists,animethemes.animethemeentries"
                )
            }
            if (searchTitle.isNotBlank()) {
                val encoded = encodeQuery(searchTitle)
                add(
                    "https://api.animethemes.moe/anime?filter[name]=$encoded&page[size]=5&include=animethemes.song.artists,animethemes.animethemeentries"
                )
            }
        }

        for ((idx, url) in candidateUrls.withIndex()) {
            val root = getJsonObject(url) ?: continue
            val animeArray = root.arrOrNull("anime") ?: continue
            if (animeArray.size() == 0) continue

            val matchedAnime = if (idx < candidateUrls.lastIndex || (malId != null && idx == 0) || (anilistId != null && idx <= 1)) {
                animeArray.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
            } else {
                pickBestAnimeThemesMatch(animeArray, searchTitle)
            } ?: continue

            val parsed = parseAnimeThemesObject(matchedAnime)
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }

    private fun pickBestAnimeThemesMatch(animeArray: JsonArray, targetTitle: String): JsonObject? {
        val normalizedTarget = normalizeForCompare(targetTitle)
        var bestObj: JsonObject? = null
        var bestScore = -1f
        for (elem in animeArray) {
            val obj = elem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val name = obj.strOrNull("name").orEmpty()
            val normName = normalizeForCompare(name)
            val score = when {
                normName == normalizedTarget -> 1.0f
                normName.contains(normalizedTarget) || normalizedTarget.contains(normName) -> 0.75f
                else -> tokenOverlap(normName, normalizedTarget)
            }
            if (score > bestScore) {
                bestScore = score
                bestObj = obj
            }
        }
        return if (bestScore >= 0.45f) bestObj else null
    }

    private fun parseAnimeThemesObject(animeObj: JsonObject): List<TrackSeed> {
        val themes = animeObj.arrOrNull("animethemes") ?: return emptyList()
        val seeds = mutableListOf<TrackSeed>()
        var opCounter = 0
        var edCounter = 0

        for (elem in themes) {
            val theme = elem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val typeStr = theme.strOrNull("type")?.uppercase(Locale.ROOT).orEmpty()
            val category = when {
                typeStr.startsWith("OP") -> SoundtrackCategory.OPENING
                typeStr.startsWith("ED") -> SoundtrackCategory.ENDING
                else -> continue
            }

            val songObj = theme.objOrNull("song") ?: continue
            val songTitle = songObj.strOrNull("title")?.trim().orEmpty()
            if (songTitle.isBlank()) continue

            val artistsArr = songObj.arrOrNull("artists")
            val artistNames = buildList {
                if (artistsArr != null) {
                    for (artistElem in artistsArr) {
                        val aName = artistElem.takeIf { it.isJsonObject }?.asJsonObject?.strOrNull("name")?.trim()
                        if (!aName.isNullOrBlank()) add(aName)
                    }
                }
            }
            val artistDisplay = artistNames.distinct().joinToString(", ").ifBlank { "Artis Tidak Diketahui" }

            val seqNum = theme.intOrNull("sequence")?.takeIf { it > 0 } ?: run {
                if (category == SoundtrackCategory.OPENING) ++opCounter else ++edCounter
            }
            if (category == SoundtrackCategory.OPENING) opCounter = maxOf(opCounter, seqNum)
            else edCounter = maxOf(edCounter, seqNum)

            val badge = if (category == SoundtrackCategory.OPENING) "OP $seqNum" else "ED $seqNum"

            val episodeNote = theme.arrOrNull("animethemeentries")
                ?.firstOrNull()
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.strOrNull("episodes")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { rawEp -> if (rawEp.startsWith("ep", ignoreCase = true)) rawEp else "Ep $rawEp" }

            val uniqueId = "at_${category.name}_${seqNum}_${songTitle.hashCode()}"
            seeds.add(
                TrackSeed(
                    id = uniqueId,
                    category = category,
                    badgeLabel = badge,
                    title = songTitle,
                    artist = artistDisplay,
                    episodeNote = episodeNote,
                    metadataSource = "AnimeThemes"
                )
            )
        }

        // Urutkan OP dulu lalu ED sesuai urutan nomor tema
        return seeds
            .distinctBy { "${it.category.name}|${normalizeForCompare(it.title)}|${it.badgeLabel}" }
            .sortedWith(compareBy<TrackSeed> { it.category.ordinal }.thenBy { extractBadgeNumber(it.badgeLabel) })
    }

    // =========================================================================
    // 1B. JIKAN API V4 (FALLBACK 1)
    // =========================================================================

    private fun resolveMalIdViaJikanSearch(title: String): Int? {
        val url = "https://api.jikan.moe/v4/anime?q=${encodeQuery(title)}&limit=3"
        val root = getJsonObject(url) ?: return null
        val data = root.arrOrNull("data") ?: return null
        val normTarget = normalizeForCompare(title)
        var fallbackId: Int? = null

        for (elem in data) {
            val obj = elem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val malId = obj.intOrNull("mal_id") ?: continue
            if (fallbackId == null) fallbackId = malId
            val tDefault = obj.strOrNull("title").orEmpty()
            val tEng = obj.strOrNull("title_english").orEmpty()
            if (tokenOverlap(normalizeForCompare(tDefault), normTarget) >= 0.55f ||
                tokenOverlap(normalizeForCompare(tEng), normTarget) >= 0.55f
            ) {
                return malId
            }
        }
        return fallbackId
    }

    private fun fetchFromJikanThemes(malId: Int): List<TrackSeed> {
        val url = "https://api.jikan.moe/v4/anime/$malId/themes"
        val root = getJsonObject(url) ?: return emptyList()
        val data = root.objOrNull("data") ?: return emptyList()

        val seeds = mutableListOf<TrackSeed>()
        parseJikanThemeArray(data.arrOrNull("openings"), SoundtrackCategory.OPENING, seeds)
        parseJikanThemeArray(data.arrOrNull("endings"), SoundtrackCategory.ENDING, seeds)
        return seeds
    }

    private fun parseJikanThemeArray(
        arr: JsonArray?,
        category: SoundtrackCategory,
        out: MutableList<TrackSeed>
    ) {
        if (arr == null) return
        val prefix = if (category == SoundtrackCategory.OPENING) "OP" else "ED"
        arr.forEachIndexed { idx, elem ->
            val raw = runCatching { elem.asString }.getOrNull()?.trim().orEmpty()
            if (raw.isBlank()) return@forEachIndexed
            val parsed = parseJikanThemeString(raw, fallbackIndex = idx + 1) ?: return@forEachIndexed
            val badge = "$prefix ${parsed.index}"
            out.add(
                TrackSeed(
                    id = "jk_${category.name}_${parsed.index}_${parsed.title.hashCode()}",
                    category = category,
                    badgeLabel = badge,
                    title = parsed.title,
                    artist = parsed.artist,
                    episodeNote = parsed.episodes,
                    metadataSource = "Jikan"
                )
            )
        }
    }

    private data class ParsedJikanLine(
        val index: Int,
        val title: String,
        val artist: String,
        val episodes: String?
    )

    private fun parseJikanThemeString(raw: String, fallbackIndex: Int): ParsedJikanLine? {
        var remaining = raw.trim()
        var index = fallbackIndex

        // Opsional awalan "1: " atau "#1: "
        val indexMatch = Regex("^#?(\\d{1,3})\\s*[:.]\\s*(.+)$").find(remaining)
        if (indexMatch != null) {
            index = indexMatch.groupValues[1].toIntOrNull() ?: fallbackIndex
            remaining = indexMatch.groupValues[2].trim()
        }

        // Opsional catatan episode di belakang "(eps 1-12)" atau "(ep 5)"
        var episodes: String? = null
        val epRegex = Regex("\\(eps?\\.?\\s*([^)]+)\\)", RegexOption.IGNORE_CASE)
        val epMatch = epRegex.find(remaining)
        if (epMatch != null) {
            episodes = "Ep ${epMatch.groupValues[1].trim()}"
            remaining = remaining.removeRange(epMatch.range).trim()
        }
        // Hapus tag bracket seperti "[TV Broadcast]" atau "(TV Broadcast)" jika bukan judul
        remaining = remaining.replace(Regex("\\[[^\\]]*]"), "").trim()

        // Pola baku Jikan: "\"Song Title\" by Artist Name"
        val quotedMatch = Regex("^\"([^\"]+)\"\\s+(?:by\\s+)?(.*)$", RegexOption.IGNORE_CASE).find(remaining)
        val songTitle: String
        val artistName: String
        if (quotedMatch != null) {
            songTitle = quotedMatch.groupValues[1].trim()
            artistName = quotedMatch.groupValues[2]
                .replace(Regex("^(?:performed\\s+by|by)\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "Artis Tidak Diketahui" }
        } else {
            val bySplit = remaining.split(Regex("\\s+by\\s+", RegexOption.IGNORE_CASE), limit = 2)
            songTitle = bySplit[0].trim().trim('"')
            artistName = bySplit.getOrNull(1)?.trim().orEmpty().ifBlank { "Artis Tidak Diketahui" }
        }

        if (songTitle.isBlank()) return null
        return ParsedJikanLine(
            index = index,
            title = songTitle,
            artist = artistName,
            episodes = episodes
        )
    }

    // =========================================================================
    // 1C. KITSU + ANILIST FALLBACK 2
    // =========================================================================

    private data class AltAnimeResolution(
        val anilistId: Int?,
        val malId: Int?,
        val kitsuId: String?,
        val altTitles: List<String>
    )

    private fun resolveViaAniListAndKitsu(
        title: String,
        existingAniListId: Int?,
        existingKitsuId: String?
    ): AltAnimeResolution? {
        val altTitles = mutableListOf<String>()
        var foundAniListId = existingAniListId
        var foundMalId: Int? = null
        var foundKitsuId = existingKitsuId

        // Query AniList Public GraphQL API
        try {
            val query = if (existingAniListId != null && existingAniListId > 0) {
                """query(${'$'}id:Int){Media(id:${'$'}id,type:ANIME){id idMal title{romaji english native} synonyms}}"""
            } else {
                """query(${'$'}search:String){Media(search:${'$'}search,type:ANIME,sort:POPULARITY_DESC){id idMal title{romaji english native} synonyms}}"""
            }
            val variables = JsonObject().apply {
                if (existingAniListId != null && existingAniListId > 0) {
                    addProperty("id", existingAniListId)
                } else {
                    addProperty("search", title)
                }
            }
            val payload = JsonObject().apply {
                addProperty("query", query)
                add("variables", variables)
            }
            val media = postGraphQl("https://graphql.anilist.co", payload.toString())
                ?.objOrNull("data")
                ?.objOrNull("Media")
            if (media != null) {
                foundAniListId = foundAniListId ?: media.intOrNull("id")
                foundMalId = foundMalId ?: media.intOrNull("idMal")
                val titleObj = media.objOrNull("title")
                titleObj?.strOrNull("romaji")?.let { if (it.isNotBlank()) altTitles.add(it) }
                titleObj?.strOrNull("english")?.let { if (it.isNotBlank()) altTitles.add(it) }
                titleObj?.strOrNull("native")?.let { if (it.isNotBlank()) altTitles.add(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // Query Kitsu API (kitsu.io/api/edge) sebagai cadangan mapping & judul
        try {
            val kitsuSearchUrl = if (!foundKitsuId.isNullOrBlank()) {
                "https://kitsu.io/api/edge/anime/$foundKitsuId?include=mappings"
            } else {
                "https://kitsu.io/api/edge/anime?filter[text]=${encodeQuery(title)}&page[limit]=2&include=mappings"
            }
            val kitsuRoot = getJsonObject(
                url = kitsuSearchUrl,
                headers = mapOf("Accept" to "application/vnd.api+json")
            )
            if (kitsuRoot != null) {
                val dataNode = kitsuRoot.get("data")
                val firstObj = when {
                    dataNode != null && dataNode.isJsonArray && dataNode.asJsonArray.size() > 0 ->
                        dataNode.asJsonArray[0].takeIf { it.isJsonObject }?.asJsonObject
                    dataNode != null && dataNode.isJsonObject -> dataNode.asJsonObject
                    else -> null
                }
                if (firstObj != null) {
                    foundKitsuId = foundKitsuId ?: firstObj.strOrNull("id")
                    val attrs = firstObj.objOrNull("attributes")
                    attrs?.strOrNull("canonicalTitle")?.let { if (it.isNotBlank()) altTitles.add(it) }
                    val titlesObj = attrs?.objOrNull("titles")
                    titlesObj?.strOrNull("en_jp")?.let { if (it.isNotBlank()) altTitles.add(it) }
                    titlesObj?.strOrNull("en")?.let { if (it.isNotBlank()) altTitles.add(it) }
                }
                val included = kitsuRoot.arrOrNull("included")
                if (included != null && foundMalId == null) {
                    for (inc in included) {
                        val incObj = inc.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                        if (incObj.strOrNull("type") == "mappings") {
                            val attr = incObj.objOrNull("attributes") ?: continue
                            val site = attr.strOrNull("externalSite").orEmpty()
                            val extId = attr.strOrNull("externalId")?.toIntOrNull()
                            if (site.contains("myanimelist", ignoreCase = true) && extId != null && extId > 0) {
                                foundMalId = extId
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        return if (foundMalId != null || foundAniListId != null || altTitles.isNotEmpty()) {
            AltAnimeResolution(
                anilistId = foundAniListId,
                malId = foundMalId,
                kitsuId = foundKitsuId,
                altTitles = altTitles.distinct()
            )
        } else {
            null
        }
    }

    // =========================================================================
    // 2. ENRICHMENT: DEEZER -> ITUNES -> MUSICBRAINZ & COVER ART ARCHIVE
    // =========================================================================

    private fun enrichTrackMedia(seed: TrackSeed): AnimeSoundtrackTrack {
        val cleanTitle = cleanSongTitleForSearch(seed.title)
        val cleanArtist = cleanArtistForSearch(seed.artist)
        val queryWithArtist = listOf(cleanTitle, cleanArtist).filter { it.isNotBlank() }.joinToString(" ")
        val queriesToTry = listOf(queryWithArtist, cleanTitle).filter { it.isNotBlank() }.distinct()

        var coverUrl = seed.coverUrl
        var previewUrl = seed.previewUrl
        var album = seed.album
        var durationSec: Int? = null
        val sources = mutableListOf<String>()

        // 2A. Deezer Public Search API (Preview .mp3 30s + Cover High-Res + Album)
        if (previewUrl.isNullOrBlank() || coverUrl.isNullOrBlank() || album.isNullOrBlank()) {
            for (q in queriesToTry) {
                val dz = searchDeezer(q, cleanTitle, cleanArtist)
                if (dz != null) {
                    if (previewUrl.isNullOrBlank() && !dz.previewUrl.isNullOrBlank()) previewUrl = dz.previewUrl
                    if (coverUrl.isNullOrBlank() && !dz.coverUrl.isNullOrBlank()) coverUrl = dz.coverUrl
                    if (album.isNullOrBlank() && !dz.album.isNullOrBlank()) album = dz.album
                    if (durationSec == null) durationSec = dz.durationSec
                    sources.add("Deezer")
                    break
                }
            }
        }

        // 2B. iTunes Search API (Preview .m4a + Artwork 600px + Album jika Deezer belum lengkap)
        if (previewUrl.isNullOrBlank() || coverUrl.isNullOrBlank() || album.isNullOrBlank()) {
            for (q in queriesToTry) {
                val itunes = searchItunes(q, cleanTitle, cleanArtist)
                if (itunes != null) {
                    if (previewUrl.isNullOrBlank() && !itunes.previewUrl.isNullOrBlank()) previewUrl = itunes.previewUrl
                    if (coverUrl.isNullOrBlank() && !itunes.coverUrl.isNullOrBlank()) coverUrl = itunes.coverUrl
                    if (album.isNullOrBlank() && !itunes.album.isNullOrBlank()) album = itunes.album
                    if (durationSec == null) durationSec = itunes.durationSec
                    sources.add("iTunes")
                    break
                }
            }
        }

        // 2C. MusicBrainz & Cover Art Archive API (Khusus cadangan Cover Art & Rilis Album)
        if (coverUrl.isNullOrBlank()) {
            val mb = searchMusicBrainzAndCoverArt(cleanTitle, cleanArtist)
            if (mb != null) {
                if (!mb.coverUrl.isNullOrBlank()) coverUrl = mb.coverUrl
                if (album.isNullOrBlank() && !mb.album.isNullOrBlank()) album = mb.album
                sources.add("MusicBrainz")
            }
        }

        return AnimeSoundtrackTrack(
            id = seed.id,
            category = seed.category,
            badgeLabel = seed.badgeLabel,
            title = seed.title,
            artist = seed.artist,
            album = album?.takeIf { it.isNotBlank() },
            episodeNote = seed.episodeNote?.takeIf { it.isNotBlank() },
            coverUrl = coverUrl?.takeIf { it.startsWith("http") },
            previewUrl = previewUrl?.takeIf { it.startsWith("http") },
            durationSec = durationSec,
            metadataSource = seed.metadataSource,
            enrichmentSource = sources.distinct().joinToString("+")
        )
    }

    private fun searchDeezer(query: String, targetTitle: String, targetArtist: String): EnrichmentInfo? {
        val url = "https://api.deezer.com/search?q=${encodeQuery(query)}&limit=5"
        val root = getJsonObject(url) ?: return null
        val data = root.arrOrNull("data") ?: return null
        if (data.size() == 0) return null

        var bestObj: JsonObject? = null
        var bestScore = -1f

        for (elem in data) {
            val item = elem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val itemTitle = item.strOrNull("title").orEmpty()
            val itemArtist = item.objOrNull("artist")?.strOrNull("name").orEmpty()
            val preview = item.strOrNull("preview").orEmpty()
            val score = scoreAudioMatch(
                candTitle = itemTitle,
                candArtist = itemArtist,
                targetTitle = targetTitle,
                targetArtist = targetArtist,
                hasPreview = preview.startsWith("http")
            )
            if (score > bestScore) {
                bestScore = score
                bestObj = item
            }
        }

        val chosen = bestObj?.takeIf { bestScore >= 0.42f } ?: return null
        val previewUrl = chosen.strOrNull("preview")?.normalizeHttps()
        val albumObj = chosen.objOrNull("album")
        val coverUrl = (
            albumObj?.strOrNull("cover_xl")
                ?: albumObj?.strOrNull("cover_big")
                ?: albumObj?.strOrNull("cover_medium")
            )?.normalizeHttps()
        val albumTitle = albumObj?.strOrNull("title")?.trim()
        val duration = chosen.intOrNull("duration")?.takeIf { it > 0 }

        return EnrichmentInfo(
            coverUrl = coverUrl,
            previewUrl = previewUrl,
            album = albumTitle,
            durationSec = duration,
            source = "Deezer"
        )
    }

    private fun searchItunes(query: String, targetTitle: String, targetArtist: String): EnrichmentInfo? {
        val url = "https://itunes.apple.com/search?term=${encodeQuery(query)}&media=music&entity=song&limit=5"
        val root = getJsonObject(url) ?: return null
        val results = root.arrOrNull("results") ?: return null
        if (results.size() == 0) return null

        var bestObj: JsonObject? = null
        var bestScore = -1f

        for (elem in results) {
            val item = elem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val itemTitle = item.strOrNull("trackName").orEmpty()
            val itemArtist = item.strOrNull("artistName").orEmpty()
            val preview = item.strOrNull("previewUrl").orEmpty()
            val score = scoreAudioMatch(
                candTitle = itemTitle,
                candArtist = itemArtist,
                targetTitle = targetTitle,
                targetArtist = targetArtist,
                hasPreview = preview.startsWith("http")
            )
            if (score > bestScore) {
                bestScore = score
                bestObj = item
            }
        }

        val chosen = bestObj?.takeIf { bestScore >= 0.42f } ?: return null
        val previewUrl = chosen.strOrNull("previewUrl")?.normalizeHttps()
        val rawArtwork = chosen.strOrNull("artworkUrl100") ?: chosen.strOrNull("artworkUrl60")
        val coverUrl = rawArtwork
            ?.replace("100x100bb", "600x600bb")
            ?.replace("60x60bb", "600x600bb")
            ?.normalizeHttps()
        val albumTitle = chosen.strOrNull("collectionName")?.trim()
        val duration = chosen.intOrNull("trackTimeMillis")?.let { (it / 1000).takeIf { sec -> sec > 0 } }

        return EnrichmentInfo(
            coverUrl = coverUrl,
            previewUrl = previewUrl,
            album = albumTitle,
            durationSec = duration,
            source = "iTunes"
        )
    }

    private fun searchMusicBrainzAndCoverArt(title: String, artist: String): EnrichmentInfo? {
        val mbQuery = buildString {
            append("recording:\"")
            append(title.replace("\"", ""))
            append("\"")
            if (artist.isNotBlank()) {
                append(" AND artist:\"")
                append(artist.replace("\"", ""))
                append("\"")
            }
        }
        val mbUrl = "https://musicbrainz.org/ws/2/recording/?query=${encodeQuery(mbQuery)}&fmt=json&limit=3"
        val mbRoot = getJsonObject(
            url = mbUrl,
            headers = mapOf(
                "User-Agent" to "YuranimeApp/2.8 (https://github.com/dinzid04/DinzStream-App)",
                "Accept" to "application/json"
            )
        ) ?: return null

        val recordings = mbRoot.arrOrNull("recordings") ?: return null
        val candidateReleaseIds = mutableListOf<Pair<String, String?>>()

        for (recElem in recordings) {
            val recObj = recElem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            val releases = recObj.arrOrNull("releases") ?: continue
            for (relElem in releases) {
                val relObj = relElem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val relId = relObj.strOrNull("id")?.takeIf { it.isNotBlank() } ?: continue
                val relTitle = relObj.strOrNull("title")?.trim()
                candidateReleaseIds.add(relId to relTitle)
                if (candidateReleaseIds.size >= 3) break
            }
            if (candidateReleaseIds.size >= 3) break
        }

        for ((releaseId, releaseTitle) in candidateReleaseIds) {
            val caaUrl = "https://coverartarchive.org/release/$releaseId"
            val caaRoot = getJsonObject(
                url = caaUrl,
                headers = mapOf("Accept" to "application/json")
            ) ?: continue
            val images = caaRoot.arrOrNull("images") ?: continue
            for (imgElem in images) {
                val imgObj = imgElem.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val thumbs = imgObj.objOrNull("thumbnails")
                val cover = (
                    thumbs?.strOrNull("500")
                        ?: thumbs?.strOrNull("large")
                        ?: thumbs?.strOrNull("250")
                        ?: imgObj.strOrNull("image")
                    )?.normalizeHttps()
                if (!cover.isNullOrBlank()) {
                    return EnrichmentInfo(
                        coverUrl = cover,
                        album = releaseTitle,
                        source = "MusicBrainz"
                    )
                }
            }
        }
        return null
    }

    // =========================================================================
    // SCORING & NETWORK UTILITIES
    // =========================================================================

    private fun scoreAudioMatch(
        candTitle: String,
        candArtist: String,
        targetTitle: String,
        targetArtist: String,
        hasPreview: Boolean
    ): Float {
        val normCandTitle = normalizeForCompare(candTitle)
        val normTargetTitle = normalizeForCompare(targetTitle)
        if (normCandTitle.isBlank() || normTargetTitle.isBlank()) return 0f

        var titleScore = when {
            normCandTitle == normTargetTitle -> 1.0f
            normCandTitle.contains(normTargetTitle) || normTargetTitle.contains(normCandTitle) -> 0.82f
            else -> tokenOverlap(normCandTitle, normTargetTitle)
        }
        if (titleScore < 0.35f) return 0f

        val normCandArtist = normalizeForCompare(candArtist)
        val normTargetArtist = normalizeForCompare(targetArtist)
        val artistScore = if (normTargetArtist.isBlank() || normTargetArtist.contains("tidak diketahui")) {
            0.5f
        } else {
            when {
                normCandArtist == normTargetArtist -> 1.0f
                normCandArtist.contains(normTargetArtist) || normTargetArtist.contains(normCandArtist) -> 0.85f
                else -> tokenOverlap(normCandArtist, normTargetArtist)
            }
        }

        // Penalti ringan untuk remix/instrumental/nightcore jika judul target bukan versi tersebut
        val lowerCand = candTitle.lowercase(Locale.ROOT)
        val lowerTarget = targetTitle.lowercase(Locale.ROOT)
        if ((lowerCand.contains("karaoke") || lowerCand.contains("instrumental") || lowerCand.contains("off vocal")) &&
            !(lowerTarget.contains("karaoke") || lowerTarget.contains("instrumental"))
        ) {
            titleScore -= 0.45f
        }

        return (titleScore * 0.65f) + (artistScore * 0.25f) + (if (hasPreview) 0.10f else 0f)
    }

    private fun cleanSongTitleForSearch(raw: String): String {
        return raw
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .ifBlank { raw.trim() }
    }

    private fun cleanArtistForSearch(raw: String): String {
        if (raw.contains("tidak diketahui", ignoreCase = true)) return ""
        return raw
            .substringBefore(",")
            .substringBefore(" feat")
            .substringBefore(" Feat")
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun extractBadgeNumber(badge: String): Int {
        return Regex("\\d+").find(badge)?.value?.toIntOrNull() ?: 999
    }

    private fun getJsonObject(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): JsonObject? {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
            headers.forEach { (k, v) -> builder.header(k, v) }
            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return null
                val parsed = JsonParser.parseString(body)
                if (parsed.isJsonObject) parsed.asJsonObject else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "GET $url gagal: ${e.message}")
            null
        }
    }

    private fun postGraphQl(url: String, jsonBody: String): JsonObject? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return null
                val parsed = JsonParser.parseString(body)
                if (parsed.isJsonObject) parsed.asJsonObject else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "POST $url gagal: ${e.message}")
            null
        }
    }

    private fun encodeQuery(q: String): String {
        return runCatching { URLEncoder.encode(q.trim(), "UTF-8") }.getOrDefault(q.trim())
    }

    private fun normalizeForCompare(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenOverlap(a: String, b: String): Float {
        val setA = a.split(' ').filter { it.isNotBlank() }.toSet()
        val setB = b.split(' ').filter { it.isNotBlank() }.toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0f
        val intersection = setA.count { it in setB }
        val union = setA.size + setB.size - intersection
        return if (union <= 0) 0f else intersection.toFloat() / union.toFloat()
    }

    private fun String.normalizeHttps(): String? {
        val t = trim()
        if (t.isBlank()) return null
        return when {
            t.startsWith("https://", ignoreCase = true) -> t
            t.startsWith("http://", ignoreCase = true) -> "https://" + t.removePrefix("http://")
            t.startsWith("//") -> "https:$t"
            else -> null
        }
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
