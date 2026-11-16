package com.yuranime.dinz.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yuranime.dinz.data.model.AniListCreditsData
import com.yuranime.dinz.data.model.AnimeSoundtrackResult
import com.yuranime.dinz.data.model.EpisodeSkipData
import com.yuranime.dinz.data.model.ExternalAnimeMapping
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache disk & memori persisten untuk pemetaan ID anime eksternal (AniList/MAL/TMDB/AniDB),
 * timestamp Skip Intro & Skip Outro per episode, dan data Karakter & Staff AniList.
 */
object AnimeExternalCacheStore {

    private const val CACHE_DIR_NAME = "yuranime_media_cache"
    private const val MAPPINGS_FILE = "mappings_cache_v1.json"
    private const val SKIPS_FILE = "skip_timestamps_cache_v1.json"
    private const val OAT_DB_FILE = "open_anime_timestamps_v1.json"

    private const val MAPPING_HIT_TTL_MS = 30L * 24L * 60L * 60L * 1000L
    private const val MAPPING_MISS_TTL_MS = 6L * 60L * 60L * 1000L
    private const val SKIP_HIT_TTL_MS = 14L * 24L * 60L * 60L * 1000L
    private const val SKIP_MISS_TTL_MS = 2L * 60L * 60L * 1000L
    private const val CREDITS_TTL_MS = 7L * 24L * 60L * 60L * 1000L
    private const val OAT_DB_TTL_MS = 7L * 24L * 60L * 60L * 1000L

    private const val MAX_MAPPINGS = 400
    private const val MAX_SKIPS = 800

    private val gson = Gson()
    private val ioLock = Any()

    @Volatile
    private var cacheDir: File? = null

    @Volatile
    private var loadedFromDisk = false

    private val mappingsMemory = ConcurrentHashMap<String, ExternalAnimeMapping>()
    private val skipsMemory = ConcurrentHashMap<String, EpisodeSkipData>()
    private val creditsMemory = ConcurrentHashMap<Int, AniListCreditsData>()
    private val soundtrackMemory = ConcurrentHashMap<String, AnimeSoundtrackResult>()

    fun init(context: Context) {
        val dir = File(context.applicationContext.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        cacheDir = dir
        ensureLoaded()
    }

    private fun ensureLoaded() {
        val dir = cacheDir ?: return
        if (loadedFromDisk) return
        synchronized(ioLock) {
            if (loadedFromDisk) return
            runCatching {
                val mapFile = File(dir, MAPPINGS_FILE)
                if (mapFile.exists()) {
                    val type = object : TypeToken<Map<String, ExternalAnimeMapping>>() {}.type
                    val parsed: Map<String, ExternalAnimeMapping>? = gson.fromJson(mapFile.readText(), type)
                    parsed?.forEach { (k, v) -> mappingsMemory[k] = v }
                }
            }
            runCatching {
                val skipFile = File(dir, SKIPS_FILE)
                if (skipFile.exists()) {
                    val type = object : TypeToken<Map<String, EpisodeSkipData>>() {}.type
                    val parsed: Map<String, EpisodeSkipData>? = gson.fromJson(skipFile.readText(), type)
                    parsed?.forEach { (k, v) -> skipsMemory[k] = v }
                }
            }
            loadedFromDisk = true
        }
    }

    fun getMapping(cacheKey: String): ExternalAnimeMapping? {
        ensureLoaded()
        val entry = mappingsMemory[cacheKey] ?: return null
        val age = System.currentTimeMillis() - entry.resolvedAtMs
        val hasId = entry.anilistId != null || entry.malId != null || entry.tmdbId != null
        val ttl = if (hasId) MAPPING_HIT_TTL_MS else MAPPING_MISS_TTL_MS
        if (age > ttl) {
            mappingsMemory.remove(cacheKey)
            return null
        }
        return entry
    }

    fun putMapping(mapping: ExternalAnimeMapping) {
        ensureLoaded()
        mappingsMemory[mapping.cacheKey] = mapping
        val dir = cacheDir ?: return
        synchronized(ioLock) {
            if (mappingsMemory.size > MAX_MAPPINGS) {
                val trimCount = mappingsMemory.size - MAX_MAPPINGS
                mappingsMemory.entries
                    .sortedBy { it.value.resolvedAtMs }
                    .take(trimCount)
                    .forEach { mappingsMemory.remove(it.key) }
            }
            writeTextAtomic(File(dir, MAPPINGS_FILE), gson.toJson(mappingsMemory))
        }
    }

    fun getSkipData(cacheKey: String): EpisodeSkipData? {
        ensureLoaded()
        val entry = skipsMemory[cacheKey] ?: return null
        val age = System.currentTimeMillis() - entry.fetchedAtMs
        val hasSegments = entry.intro != null || entry.outro != null
        val ttl = if (hasSegments) SKIP_HIT_TTL_MS else SKIP_MISS_TTL_MS
        if (age > ttl) {
            skipsMemory.remove(cacheKey)
            return null
        }
        return entry
    }

    fun putSkipData(data: EpisodeSkipData) {
        ensureLoaded()
        skipsMemory[data.cacheKey] = data
        val dir = cacheDir ?: return
        synchronized(ioLock) {
            if (skipsMemory.size > MAX_SKIPS) {
                val trimCount = skipsMemory.size - MAX_SKIPS
                skipsMemory.entries
                    .sortedBy { it.value.fetchedAtMs }
                    .take(trimCount)
                    .forEach { skipsMemory.remove(it.key) }
            }
            writeTextAtomic(File(dir, SKIPS_FILE), gson.toJson(skipsMemory))
        }
    }

    fun getCredits(anilistId: Int): AniListCreditsData? {
        val mem = creditsMemory[anilistId]
        val now = System.currentTimeMillis()
        if (mem != null && now - mem.fetchedAtMs <= CREDITS_TTL_MS) {
            return mem
        }
        val dir = cacheDir ?: return null
        val file = File(dir, "anilist_credits_v1_${anilistId}.json")
        if (!file.exists()) return null
        val parsed = runCatching { gson.fromJson(file.readText(), AniListCreditsData::class.java) }.getOrNull()
        if (parsed != null && now - parsed.fetchedAtMs <= CREDITS_TTL_MS) {
            creditsMemory[anilistId] = parsed
            return parsed
        }
        return null
    }

    fun putCredits(data: AniListCreditsData) {
        creditsMemory[data.anilistId] = data
        val dir = cacheDir ?: return
        synchronized(ioLock) {
            writeTextAtomic(File(dir, "anilist_credits_v1_${data.anilistId}.json"), gson.toJson(data))
        }
    }

    fun getSoundtrack(cacheKey: String): AnimeSoundtrackResult? {
        if (cacheKey.isBlank()) return null
        val sanitizedKey = cacheKey.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        val mem = soundtrackMemory[sanitizedKey]
        if (mem != null && (System.currentTimeMillis() - mem.fetchedAtMs) < CREDITS_TTL_MS) {
            return mem
        }
        val dir = cacheDir ?: return null
        val file = File(dir, "soundtrack_v1_$sanitizedKey.json")
        if (!file.exists()) return null
        val parsed = runCatching {
            gson.fromJson(file.readText(), AnimeSoundtrackResult::class.java)
        }.getOrNull() ?: return null
        if ((System.currentTimeMillis() - parsed.fetchedAtMs) > CREDITS_TTL_MS) return null
        soundtrackMemory[sanitizedKey] = parsed
        return parsed
    }

    fun putSoundtrack(cacheKey: String, data: AnimeSoundtrackResult) {
        if (cacheKey.isBlank() || data.isEmpty) return
        val sanitizedKey = cacheKey.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
        soundtrackMemory[sanitizedKey] = data
        val dir = cacheDir ?: return
        synchronized(ioLock) {
            writeTextAtomic(File(dir, "soundtrack_v1_$sanitizedKey.json"), gson.toJson(data))
        }
    }

    fun getCachedOpenAnimeTimestampsRaw(): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, OAT_DB_FILE)
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > OAT_DB_TTL_MS) return null
        return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun putCachedOpenAnimeTimestampsRaw(jsonRaw: String) {
        if (jsonRaw.isBlank()) return
        val dir = cacheDir ?: return
        synchronized(ioLock) {
            writeTextAtomic(File(dir, OAT_DB_FILE), jsonRaw)
        }
    }

    private fun writeTextAtomic(target: File, content: String) {
        runCatching {
            val tmp = File(target.parentFile, "${target.name}.tmp")
            tmp.writeText(content)
            if (!tmp.renameTo(target)) {
                target.writeText(content)
                tmp.delete()
            }
        }
    }
}
