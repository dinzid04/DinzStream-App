package com.yuranime.dinz.data.repository

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class StickerItem(
    val url: String,
    val isAnimated: Boolean = false
)

data class StickerPack(
    val id: String,
    val name: String,
    val author: String,
    val thumbnailUrl: String,
    val sourceUrl: String,
    val stickers: List<StickerItem>,
    val isAdminManaged: Boolean = false
)

/** Mengelola katalog stiker Sticker.ly, listener Firestore Admin, cache lokal 0ms, dan pre-warm Coil. */
object StickerRepository {
    private const val TAG = "StickerRepository"

    private const val SEARCH_API = "https://api.snowping.cfd/api/search/stikerly?query="
    private const val DOWNLOAD_API = "https://api.snowping.cfd/api/downloader/stikerly?url="
    private const val FALLBACK_SEARCH_API = "https://api-faa.my.id/faa/stickerly?q="

    private const val CACHE_FILE_NAME = "yuranime_stickers_cache_v1.json"
    private const val PREFS_NAME = "yuranime_sticker_prefs"
    private const val KEY_RECENT_JSON = "recent_stickers_json"
    private const val CACHE_TTL_MS = 12L * 60 * 60 * 1000L

    private val DEFAULT_QUERIES = listOf(
        "kucing",
        "bubu dudu",
        "jomok",
        "meme",
        "anime",
        "pentol"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _packs = MutableStateFlow<List<StickerPack>>(emptyList())
    val packs: StateFlow<List<StickerPack>> = _packs.asStateFlow()

    private val _recentStickers = MutableStateFlow<List<StickerItem>>(emptyList())
    val recentStickers: StateFlow<List<StickerItem>> = _recentStickers.asStateFlow()

    private val _isLoadingDefaults = MutableStateFlow(false)
    val isLoadingDefaults: StateFlow<Boolean> = _isLoadingDefaults.asStateFlow()

    private var adminPacksCache: List<StickerPack> = emptyList()
    private var defaultPacksCache: List<StickerPack> = emptyList()
    private var adminListener: ListenerRegistration? = null
    private var initialized = false

    private val searchMemoryCache = ConcurrentHashMap<String, List<StickerPack>>()
    private val packDetailMemoryCache = ConcurrentHashMap<String, StickerPack>()

    fun initializeAndPreload(context: Context) {
        val appContext = context.applicationContext
        loadRecentFromPrefs(appContext)
        loadCachedDefaultsFromDisk(appContext)

        if (!initialized) {
            initialized = true
            startListeningGlobalStickerPacks(appContext)
        }

        scope.launch {
            val cacheFile = File(appContext.filesDir, CACHE_FILE_NAME)
            val needsNetworkRefresh = defaultPacksCache.isEmpty() ||
                !cacheFile.exists() ||
                (System.currentTimeMillis() - cacheFile.lastModified() > CACHE_TTL_MS)

            if (needsNetworkRefresh) {
                refreshDefaultPacks(appContext)
            } else {
                prewarmStickerImages(appContext, _packs.value)
            }
        }
    }

    fun recordRecentSticker(context: Context, item: StickerItem) {
        val appContext = context.applicationContext
        val updated = (listOf(item) + _recentStickers.value.filterNot { it.url == item.url }).take(32)
        _recentStickers.value = updated

        val arr = JSONArray()
        updated.forEach { st ->
            arr.put(JSONObject().apply {
                put("url", st.url)
                put("anim", st.isAnimated)
            })
        }
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT_JSON, arr.toString())
            .apply()
    }

    private fun loadRecentFromPrefs(context: Context) {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_JSON, null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            val list = ArrayList<StickerItem>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val url = obj.optString("url")
                if (url.isNotBlank()) {
                    list.add(StickerItem(url = url, isAnimated = obj.optBoolean("anim", false)))
                }
            }
            _recentStickers.value = list
        }
    }

    private fun startListeningGlobalStickerPacks(appContext: Context) {
        try {
            adminListener?.remove()
            adminListener = FirebaseFirestore.getInstance()
                .collection("global_sticker_packs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listener global_sticker_packs gagal: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val mapped = snapshot.documents.mapNotNull { doc ->
                            val name = doc.getString("name")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("title")?.takeIf { it.isNotBlank() }
                                ?: "Paket Stiker"
                            val author = doc.getString("author")?.takeIf { it.isNotBlank() } ?: "Yuranime"
                            val thumb = doc.getString("thumbnailUrl")?.ifBlank { null }
                                ?: doc.getString("thumbnail").orEmpty()
                            val sourceUrl = doc.getString("sourceUrl")?.ifBlank { null }
                                ?: doc.getString("url").orEmpty()
                            val rawStickers = doc.get("stickers") as? List<*>
                            val stickers = rawStickers?.mapNotNull { entry ->
                                when (entry) {
                                    is String -> StickerItem(url = entry, isAnimated = guessAnimated(entry))
                                    is Map<*, *> -> {
                                        val u = entry["url"]?.toString()?.trim().orEmpty()
                                        if (u.isBlank()) null
                                        else StickerItem(
                                            url = u,
                                            isAnimated = (entry["isAnimated"] as? Boolean) ?: guessAnimated(u)
                                        )
                                    }
                                    else -> null
                                }
                            }.orEmpty()
                            if (stickers.isEmpty()) return@mapNotNull null

                            StickerPack(
                                id = doc.id,
                                name = name,
                                author = author,
                                thumbnailUrl = thumb.ifBlank { stickers.first().url },
                                sourceUrl = sourceUrl,
                                stickers = stickers,
                                isAdminManaged = true
                            )
                        }
                        adminPacksCache = mapped
                        publishMergedPacks()
                        prewarmStickerImages(appContext, mapped)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memasang listener stiker Firestore: ${e.message}")
        }
    }

    private suspend fun refreshDefaultPacks(appContext: Context) = coroutineScope {
        _isLoadingDefaults.value = true
        try {
            val resolved = DEFAULT_QUERIES.map { query ->
                async {
                    runCatching {
                        val candidates = searchPackSummaries(query).take(3)
                        for (candidate in candidates) {
                            val detail = fetchPackDetail(
                                packUrl = candidate.sourceUrl,
                                fallbackName = candidate.name,
                                fallbackAuthor = candidate.author,
                                fallbackThumb = candidate.thumbnailUrl
                            )
                            if (detail != null && detail.stickers.isNotEmpty()) {
                                return@async detail
                            }
                        }
                        null
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()

            if (resolved.isNotEmpty()) {
                defaultPacksCache = resolved
                publishMergedPacks()
                saveDefaultsToDisk(appContext, resolved)
                prewarmStickerImages(appContext, resolved)
            }
        } finally {
            _isLoadingDefaults.value = false
        }
    }

    private fun publishMergedPacks() {
        val seenIds = HashSet<String>()
        val merged = mutableListOf<StickerPack>()
        (adminPacksCache + defaultPacksCache).forEach { pack ->
            val dedupKey = pack.sourceUrl.ifBlank { pack.id }
            if (pack.stickers.isNotEmpty() && seenIds.add(dedupKey)) {
                merged.add(pack)
            }
        }
        _packs.value = merged
    }

    suspend fun searchStickerPacks(query: String): List<StickerPack> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext _packs.value

        val cacheKey = cleanQuery.lowercase(Locale.ROOT)
        searchMemoryCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val summaries = searchPackSummaries(cleanQuery).take(6)
        val details = coroutineScope {
            summaries.map { summary ->
                async {
                    fetchPackDetail(
                        packUrl = summary.sourceUrl,
                        fallbackName = summary.name,
                        fallbackAuthor = summary.author,
                        fallbackThumb = summary.thumbnailUrl
                    )
                }
            }.awaitAll().filterNotNull().filter { it.stickers.isNotEmpty() }
        }

        if (details.isNotEmpty()) {
            searchMemoryCache[cacheKey] = details
        }
        details
    }

    private fun searchPackSummaries(query: String): List<PackSummary> {
        val encoded = URLEncoder.encode(query, "UTF-8")

        // Sumber utama sesuai spesifikasi api.snowping.cfd, otomatis fallback saat diblokir proxy/server.
        val primaryRaw = httpGetText("$SEARCH_API$encoded")
        if (!primaryRaw.isNullOrBlank()) {
            val parsedPrimary = parseSearchSummariesJson(primaryRaw)
            if (parsedPrimary.isNotEmpty()) return parsedPrimary
        }

        val fallbackRaw = httpGetText("$FALLBACK_SEARCH_API$encoded")
        if (!fallbackRaw.isNullOrBlank()) {
            val parsedFallback = parseSearchSummariesJson(fallbackRaw)
            if (parsedFallback.isNotEmpty()) return parsedFallback
        }
        return emptyList()
    }

    private data class PackSummary(
        val name: String,
        val author: String,
        val thumbnailUrl: String,
        val sourceUrl: String
    )

    private fun parseSearchSummariesJson(raw: String): List<PackSummary> {
        return runCatching {
            val trimmed = raw.trim()
            val arr = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    if (obj.optInt("status", 200) == 403 || obj.optBoolean("status", true) == false && !obj.has("results") && !obj.has("data") && !obj.has("result")) {
                        return emptyList()
                    }
                    extractFirstArray(obj, listOf("results", "data", "result", "packs", "items"))
                }
                else -> null
            } ?: return emptyList()

            val list = ArrayList<PackSummary>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val url = firstNonBlank(item, "url", "packUrl", "link", "sticker_url")
                if (url.isBlank() || !url.contains("sticker.ly", ignoreCase = true)) continue
                val name = firstNonBlank(item, "title", "name", "packName", "pack_name").ifBlank { "Sticker Pack" }
                val author = firstNonBlank(item, "author", "creator", "publisher", "owner").ifBlank { "Sticker.ly" }
                val thumb = firstNonBlank(item, "thumbnail", "thumb", "cover", "image", "icon")
                list.add(PackSummary(name = name, author = author, thumbnailUrl = thumb, sourceUrl = url))
            }
            list
        }.getOrDefault(emptyList())
    }

    suspend fun fetchPackDetail(
        packUrl: String,
        fallbackName: String = "",
        fallbackAuthor: String = "",
        fallbackThumb: String = ""
    ): StickerPack? = withContext(Dispatchers.IO) {
        val cleanUrl = packUrl.trim()
        if (cleanUrl.isBlank()) return@withContext null

        packDetailMemoryCache[cleanUrl]?.let { return@withContext it }

        val encoded = URLEncoder.encode(cleanUrl, "UTF-8")
        val apiRaw = httpGetText("$DOWNLOAD_API$encoded")
        if (!apiRaw.isNullOrBlank()) {
            val fromApi = parseDownloaderJson(apiRaw, cleanUrl, fallbackName, fallbackAuthor, fallbackThumb)
            if (fromApi != null && fromApi.stickers.isNotEmpty()) {
                packDetailMemoryCache[cleanUrl] = fromApi
                return@withContext fromApi
            }
        }

        // Fallback langsung ke halaman publik sticker.ly/s/... jika endpoint API sedang 403/gangguan.
        val htmlRaw = httpGetText(cleanUrl)
        if (!htmlRaw.isNullOrBlank()) {
            val fromHtml = parseStickerLyHtml(htmlRaw, cleanUrl, fallbackName, fallbackAuthor, fallbackThumb)
            if (fromHtml != null && fromHtml.stickers.isNotEmpty()) {
                packDetailMemoryCache[cleanUrl] = fromHtml
                return@withContext fromHtml
            }
        }
        null
    }

    private fun parseDownloaderJson(
        raw: String,
        sourceUrl: String,
        fallbackName: String,
        fallbackAuthor: String,
        fallbackThumb: String
    ): StickerPack? {
        return runCatching {
            val root = JSONObject(raw.trim())
            if (root.optInt("status", 200) == 403) return null

            val payload = root.optJSONObject("result")
                ?: root.optJSONObject("data")
                ?: root
            val stickerArr = extractFirstArray(
                payload,
                listOf("stickers", "sticker", "images", "items", "result", "data")
            ) ?: extractFirstArray(root, listOf("stickers", "data", "result")) ?: return null

            val stickers = ArrayList<StickerItem>(stickerArr.length())
            for (i in 0 until stickerArr.length()) {
                val elem = stickerArr.opt(i) ?: continue
                when (elem) {
                    is String -> if (elem.startsWith("http")) {
                        stickers.add(StickerItem(url = elem, isAnimated = guessAnimated(elem)))
                    }
                    is JSONObject -> {
                        val u = firstNonBlank(elem, "url", "imageUrl", "image_url", "image", "src", "stickerUrl")
                        if (u.startsWith("http")) {
                            val animFlag = if (elem.has("isAnimated")) {
                                elem.optBoolean("isAnimated", false)
                            } else if (elem.has("animated")) {
                                elem.optBoolean("animated", false)
                            } else {
                                guessAnimated(u)
                            }
                            stickers.add(StickerItem(url = u, isAnimated = animFlag))
                        }
                    }
                }
            }
            if (stickers.isEmpty()) return null

            val packName = firstNonBlank(payload, "packName", "title", "name", "pack_name")
                .ifBlank { fallbackName.ifBlank { "Sticker Pack" } }
            val author = firstNonBlank(payload, "author", "creator", "publisher", "owner")
                .ifBlank { fallbackAuthor.ifBlank { "Sticker.ly" } }
            val thumb = firstNonBlank(payload, "thumbnail", "thumb", "cover", "icon", "trayImageUrl")
                .ifBlank { fallbackThumb.ifBlank { stickers.first().url } }

            StickerPack(
                id = extractPackId(sourceUrl),
                name = packName,
                author = author,
                thumbnailUrl = thumb,
                sourceUrl = sourceUrl,
                stickers = stickers,
                isAdminManaged = false
            )
        }.getOrNull()
    }

    private fun parseStickerLyHtml(
        html: String,
        sourceUrl: String,
        fallbackName: String,
        fallbackAuthor: String,
        fallbackThumb: String
    ): StickerPack? {
        val listBlock = Regex("""<ul[^>]*id=["']content_images["'][^>]*>([\s\S]*?)</ul>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1) ?: html

        val urls = Regex("""<img[^>]+src=["'](https://stickerly\.pstatic\.net/sticker_pack/[^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(listBlock)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        if (urls.isEmpty()) return null

        val parsedTitle = Regex("""property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
            ?: Regex("""class=["']sticker_name["'][\s\S]*?<span>([^<]+)</span>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
            ?: fallbackName

        val parsedAuthor = Regex("""class=["']sticker_author["'][^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.trim()
            ?: fallbackAuthor

        val parsedThumb = Regex("""property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
            ?: fallbackThumb

        val stickers = urls.map { u -> StickerItem(url = u, isAnimated = guessAnimated(u)) }
        return StickerPack(
            id = extractPackId(sourceUrl),
            name = parsedTitle.ifBlank { "Sticker Pack" },
            author = parsedAuthor.ifBlank { "Sticker.ly" },
            thumbnailUrl = parsedThumb.ifBlank { stickers.first().url },
            sourceUrl = sourceUrl,
            stickers = stickers,
            isAdminManaged = false
        )
    }

    private fun prewarmStickerImages(context: Context, packs: List<StickerPack>) {
        if (packs.isEmpty()) return
        val imageLoader = Coil.imageLoader(context)
        val queued = HashSet<String>()

        packs.forEach { pack ->
            if (pack.thumbnailUrl.isNotBlank() && queued.add(pack.thumbnailUrl)) {
                enqueueImageWarmup(context, imageLoader, pack.thumbnailUrl)
            }
            // Preload 16 stiker pertama per paket agar tampilan panel instan tanpa membebani jaringan awal.
            pack.stickers.take(16).forEach { sticker ->
                if (queued.add(sticker.url)) {
                    enqueueImageWarmup(context, imageLoader, sticker.url)
                }
            }
        }
    }

    private fun enqueueImageWarmup(context: Context, imageLoader: coil.ImageLoader, url: String) {
        val req = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        imageLoader.enqueue(req)
    }

    private fun loadCachedDefaultsFromDisk(context: Context) {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        if (!cacheFile.exists()) return
        runCatching {
            val arr = JSONArray(cacheFile.readText())
            val list = ArrayList<StickerPack>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val stickersArr = obj.optJSONArray("stickers") ?: continue
                val stickers = ArrayList<StickerItem>(stickersArr.length())
                for (j in 0 until stickersArr.length()) {
                    val sObj = stickersArr.optJSONObject(j) ?: continue
                    val u = sObj.optString("url")
                    if (u.isNotBlank()) {
                        stickers.add(StickerItem(url = u, isAnimated = sObj.optBoolean("anim", false)))
                    }
                }
                if (stickers.isNotEmpty()) {
                    list.add(
                        StickerPack(
                            id = obj.optString("id"),
                            name = obj.optString("name", "Sticker Pack"),
                            author = obj.optString("author", "Sticker.ly"),
                            thumbnailUrl = obj.optString("thumb", stickers.first().url),
                            sourceUrl = obj.optString("sourceUrl"),
                            stickers = stickers,
                            isAdminManaged = false
                        )
                    )
                }
            }
            if (list.isNotEmpty()) {
                defaultPacksCache = list
                publishMergedPacks()
            }
        }
    }

    private fun saveDefaultsToDisk(context: Context, packs: List<StickerPack>) {
        runCatching {
            val arr = JSONArray()
            packs.forEach { pack ->
                val obj = JSONObject().apply {
                    put("id", pack.id)
                    put("name", pack.name)
                    put("author", pack.author)
                    put("thumb", pack.thumbnailUrl)
                    put("sourceUrl", pack.sourceUrl)
                    val sArr = JSONArray()
                    pack.stickers.forEach { st ->
                        sArr.put(JSONObject().apply {
                            put("url", st.url)
                            put("anim", st.isAnimated)
                        })
                    }
                    put("stickers", sArr)
                }
                arr.put(obj)
            }
            File(context.filesDir, CACHE_FILE_NAME).writeText(arr.toString())
        }
    }

    private fun httpGetText(url: String): String? {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        }.getOrNull()
    }

    private fun extractFirstArray(obj: JSONObject, keys: List<String>): JSONArray? {
        for (key in keys) {
            val found = obj.optJSONArray(key)
            if (found != null) return found
        }
        return null
    }

    private fun firstNonBlank(obj: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj.optString(k, "").trim()
            if (v.isNotBlank() && v != "null") return v
        }
        return ""
    }

    private fun extractPackId(url: String): String {
        return url.trimEnd('/').substringAfterLast('/').ifBlank { url.hashCode().toString() }
    }

    private fun guessAnimated(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return lower.endsWith(".gif") || lower.contains(".gif?") ||
            lower.endsWith(".webp") || lower.contains(".webp?")
    }
}
