package com.yuranime.dinz.data.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuranime.dinz.data.local.AnimeExternalCacheStore
import com.yuranime.dinz.data.model.EpisodeSkipData
import com.yuranime.dinz.data.model.SkipSegment
import com.yuranime.dinz.data.model.SkipSegmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Mengambil timestamp Opening (Intro) dan Ending (Outro) terverifikasi API eksternal:
 * Prioritas 1: AniSkip API v2 (berdasarkan MyAnimeList ID & nomor episode)
 * Prioritas 2: TheIntroDB API v3 (berdasarkan TMDB ID, season & nomor episode)
 * Prioritas 3: Open Anime Timestamps (berdasarkan AniDB ID, hanya jika start & end eksplisit)
 *
 * ATURAN MUTLAK: Tidak pernah menebak atau melakukan hardcode (+85s / +90s).
 * Jika seluruh sumber mengembalikan kosong / 404, segment bernilai null dan tombol disembunyikan.
 */
object SkipTimestampsRepository {

    private const val ANISKIP_BASE = "https://api.aniskip.com/v2/skip-times"
    private const val THEINTRODB_BASE = "https://api.theintrodb.org/v3/media"
    private const val OAT_RAW_URL =
        "https://raw.githubusercontent.com/jonbarrow/open-anime-timestamps/master/timestamps.json"

    private const val MIN_SEGMENT_DURATION_MS = 10_000L
    private const val MAX_SEGMENT_DURATION_MS = 240_000L

    suspend fun getSkipTimestamps(
        title: String,
        synonymsRaw: String? = null,
        yearHint: String? = null,
        airedStartHint: String? = null,
        expectedEpisodeCount: Int? = null,
        formatHint: String? = null,
        movieId: String? = null,
        episodeNumberRaw: String?,
        episodeDurationMs: Long = 0L
    ): EpisodeSkipData? = withContext(Dispatchers.IO) {
        val epNumber = extractPositiveEpisodeNumber(episodeNumberRaw) ?: return@withContext null
        val cleanedTitle = AnimeIdentityResolver.cleanAnimeTitle(title)
        if (cleanedTitle.isBlank()) return@withContext null

        val skipCacheKey = buildSkipCacheKey(movieId, cleanedTitle, epNumber)
        AnimeExternalCacheStore.getSkipData(skipCacheKey)?.let { cached ->
            return@withContext cached
        }

        val mapping = AnimeIdentityResolver.resolveMapping(
            title = title,
            synonymsRaw = synonymsRaw,
            yearHint = yearHint,
            airedStartHint = airedStartHint,
            expectedEpisodeCount = expectedEpisodeCount,
            formatHint = formatHint,
            movieId = movieId
        )

        if (mapping == null) {
            return@withContext null
        }

        var intro: SkipSegment? = null
        var outro: SkipSegment? = null
        val usedSources = linkedSetOf<String>()

        // PRIORITAS 1: AniSkip v2 (berbasis MyAnimeList ID + Episode)
        val malId = mapping.malId
        if (malId != null && malId > 0) {
            val epLenSec = if (episodeDurationMs > 30_000L) (episodeDurationMs / 1000L) else 0L
            val aniSkipResult = fetchFromAniSkip(malId, epNumber, epLenSec)
            if (aniSkipResult.first != null) {
                intro = aniSkipResult.first
                usedSources.add("AniSkip")
            }
            if (aniSkipResult.second != null) {
                outro = aniSkipResult.second
                usedSources.add("AniSkip")
            }
        }

        // PRIORITAS 2 FALLBACK: TheIntroDB v3 (berbasis TMDB ID + Season + Episode)
        val tmdbId = mapping.tmdbId
        if ((intro == null || outro == null) && tmdbId != null && tmdbId > 0) {
            val season = mapping.tmdbSeason ?: mapping.seasonHint ?: 1
            val introDbResult = fetchFromTheIntroDb(tmdbId, season, epNumber)
            if (intro == null && introDbResult.first != null) {
                intro = introDbResult.first
                usedSources.add("TheIntroDB")
            }
            if (outro == null && introDbResult.second != null) {
                outro = introDbResult.second
                usedSources.add("TheIntroDB")
            }
        }

        // PRIORITAS 3 FALLBACK: Open Anime Timestamps (hanya jika rentang start & end eksplisit ada)
        val anidbId = mapping.anidbId
        if ((intro == null || outro == null) && anidbId != null && anidbId > 0) {
            val oatResult = fetchFromOpenAnimeTimestamps(anidbId, epNumber)
            if (intro == null && oatResult.first != null) {
                intro = oatResult.first
                usedSources.add("OpenAnimeTimestamps")
            }
            if (outro == null && oatResult.second != null) {
                outro = oatResult.second
                usedSources.add("OpenAnimeTimestamps")
            }
        }

        val finalData = EpisodeSkipData(
            cacheKey = skipCacheKey,
            intro = validateAgainstDuration(intro, episodeDurationMs),
            outro = validateAgainstDuration(outro, episodeDurationMs),
            sourceSummary = usedSources.takeIf { it.isNotEmpty() }?.joinToString(" + "),
            mappingUsed = mapping
        )

        AnimeExternalCacheStore.putSkipData(finalData)
        finalData
    }

    private fun fetchFromAniSkip(
        malId: Int,
        epNumber: Int,
        episodeLengthSec: Long
    ): Pair<SkipSegment?, SkipSegment?> {
        val url = "$ANISKIP_BASE/$malId/$epNumber?types=op&types=mixed-op&types=ed&types=mixed-ed&episodeLength=$episodeLengthSec"
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        return runCatching {
            AnimeIdentityResolver.externalHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null to null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@use null to null

                val root = JsonParser.parseString(raw).asJsonObject
                val found = root.get("found")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
                if (!found) return@use null to null
                val results = root.getAsJsonArray("results") ?: return@use null to null

                var bestOp: SkipSegment? = null
                var isBestOpExact = false
                var bestEd: SkipSegment? = null
                var isBestEdExact = false

                for (elem in results) {
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val skipType = obj.get("skipType")?.takeIf { it.isJsonPrimitive }?.asString
                        ?.lowercase(Locale.ROOT) ?: continue
                    val interval = obj.getAsJsonObject("interval") ?: continue
                    val startSec = interval.doubleOrNull("startTime") ?: continue
                    val endSec = interval.doubleOrNull("endTime") ?: continue

                    val startMs = (startSec * 1000.0).roundToLong()
                    val endMs = (endSec * 1000.0).roundToLong()
                    val durMs = endMs - startMs
                    if (startMs < 0L || durMs !in MIN_SEGMENT_DURATION_MS..MAX_SEGMENT_DURATION_MS) {
                        continue
                    }

                    when (skipType) {
                        "op", "mixed-op" -> {
                            val isExact = skipType == "op"
                            if (bestOp == null || (isExact && !isBestOpExact)) {
                                bestOp = SkipSegment(SkipSegmentType.INTRO, startMs, endMs, "AniSkip")
                                isBestOpExact = isExact
                            }
                        }
                        "ed", "mixed-ed" -> {
                            val isExact = skipType == "ed"
                            if (bestEd == null || (isExact && !isBestEdExact)) {
                                bestEd = SkipSegment(SkipSegmentType.OUTRO, startMs, endMs, "AniSkip")
                                isBestEdExact = isExact
                            }
                        }
                    }
                }
                bestOp to bestEd
            }
        }.getOrDefault(null to null)
    }

    private fun fetchFromTheIntroDb(
        tmdbId: Int,
        season: Int,
        epNumber: Int
    ): Pair<SkipSegment?, SkipSegment?> {
        val url = "$THEINTRODB_BASE?tmdb_id=$tmdbId&season=$season&episode=$epNumber"
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        return runCatching {
            AnimeIdentityResolver.externalHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null to null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@use null to null
                val root = JsonParser.parseString(raw).asJsonObject

                val introSeg = parseFirstValidTheIntroDbItem(root, "intro", SkipSegmentType.INTRO)
                val outroSeg = parseFirstValidTheIntroDbItem(root, "credits", SkipSegmentType.OUTRO)
                introSeg to outroSeg
            }
        }.getOrDefault(null to null)
    }

    private fun parseFirstValidTheIntroDbItem(
        root: JsonObject,
        arrayKey: String,
        type: SkipSegmentType
    ): SkipSegment? {
        val arr = root.get(arrayKey)?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        for (elem in arr) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val startMs = obj.longOrNull("start_ms") ?: continue
            val endMs = obj.longOrNull("end_ms") ?: continue
            val durMs = endMs - startMs
            if (startMs >= 0L && durMs in MIN_SEGMENT_DURATION_MS..MAX_SEGMENT_DURATION_MS) {
                return SkipSegment(type, startMs, endMs, "TheIntroDB")
            }
        }
        return null
    }

    private fun fetchFromOpenAnimeTimestamps(
        anidbId: Int,
        epNumber: Int
    ): Pair<SkipSegment?, SkipSegment?> {
        val rawJson = AnimeExternalCacheStore.getCachedOpenAnimeTimestampsRaw() ?: run {
            val req = Request.Builder()
                .url(OAT_RAW_URL)
                .get()
                .header("Accept", "application/json")
                .build()
            val downloaded = runCatching {
                AnimeIdentityResolver.externalHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()?.takeIf { it.isNotBlank() }
                }
            }.getOrNull() ?: return null to null
            AnimeExternalCacheStore.putCachedOpenAnimeTimestampsRaw(downloaded)
            downloaded
        }

        return runCatching {
            val root = JsonParser.parseString(rawJson).asJsonObject
            val epList = root.get(anidbId.toString())?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return@runCatching null to null

            var introSeg: SkipSegment? = null
            var outroSeg: SkipSegment? = null

            for (elem in epList) {
                if (!elem.isJsonObject) continue
                val obj = elem.asJsonObject
                val num = obj.longOrNull("episode_number")?.toInt() ?: continue
                if (num != epNumber) continue

                // Hanya gunakan segmen bila titik awal & akhir eksplisit tersedia di database
                val opStart = obj.longOrNull("opening_start") ?: -1L
                val opEnd = obj.longOrNull("opening_end") ?: -1L
                if (opStart >= 0L && opEnd > opStart && (opEnd - opStart) in 10L..240L) {
                    introSeg = SkipSegment(
                        SkipSegmentType.INTRO,
                        opStart * 1000L,
                        opEnd * 1000L,
                        "OpenAnimeTimestamps"
                    )
                }

                val edStart = obj.longOrNull("ending_start") ?: -1L
                val edExplicitEnd = obj.longOrNull("ending_end") ?: -1L
                val previewStart = obj.longOrNull("preview_start") ?: -1L
                val edEnd = when {
                    edExplicitEnd > edStart -> edExplicitEnd
                    previewStart > edStart -> previewStart
                    else -> -1L
                }
                if (edStart >= 0L && edEnd > edStart && (edEnd - edStart) in 10L..240L) {
                    outroSeg = SkipSegment(
                        SkipSegmentType.OUTRO,
                        edStart * 1000L,
                        edEnd * 1000L,
                        "OpenAnimeTimestamps"
                    )
                }
                break
            }
            introSeg to outroSeg
        }.getOrDefault(null to null)
    }

    private fun validateAgainstDuration(segment: SkipSegment?, durationMs: Long): SkipSegment? {
        if (segment == null) return null
        if (segment.endMs <= segment.startMs) return null
        if (durationMs > 30_000L) {
            if (segment.startMs >= durationMs - 4_000L) return null
            val clampedEnd = segment.endMs.coerceAtMost(durationMs)
            if (clampedEnd - segment.startMs < MIN_SEGMENT_DURATION_MS) return null
            return segment.copy(endMs = clampedEnd)
        }
        return segment
    }

    fun extractPositiveEpisodeNumber(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return Regex("\\d+").find(raw.trim())?.value?.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun buildSkipCacheKey(movieId: String?, cleanedTitle: String, epNumber: Int): String {
        val normTitle = cleanedTitle.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
            .take(48)
        val moviePart = movieId?.trim()?.takeIf { it.isNotBlank() } ?: "noid"
        return "${moviePart}_${normTitle}_ep${epNumber}"
    }

    private fun JsonObject.doubleOrNull(key: String): Double? {
        val elem: JsonElement = get(key) ?: return null
        return if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
            runCatching { elem.asDouble }.getOrNull()
        } else null
    }

    private fun JsonObject.longOrNull(key: String): Long? {
        val elem: JsonElement = get(key) ?: return null
        return if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
            runCatching { elem.asLong }.getOrNull()
        } else null
    }
}
