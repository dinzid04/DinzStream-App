package com.yuranime.dinz.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.yuranime.dinz.data.model.AnimeSoundtrackTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Implementasi native Kotlin untuk pencarian lagu penuh & ekstraksi audio stream
 * YouTube Music berbasis metode Innertube WEB_REMIX + VISIONOS player (`ytmusic.mjs`)
 * tanpa memerlukan API Key atau autentikasi.
 */
object YtMusicRepository {

    private const val TAG = "YtMusicRepo"
    private const val BASE_URL = "https://music.youtube.com"
    private const val FILTER_SONGS_PARAM = "EgWKAQIIAWoSEAUQAxAJEAoQERAQEAQQDhAV"
    private const val CONFIG_TTL_MS = 30L * 60L * 1000L
    private const val STREAM_CACHE_TTL_MS = 90L * 60L * 1000L

    private const val CHROME_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val VISIONOS_UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val configMutex = Mutex()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private data class InnertubeConfig(
        val apiKey: String,
        val clientVersion: String,
        val visitorData: String?,
        val fetchedAtMs: Long
    )

    data class YtMusicSearchItem(
        val title: String,
        val videoId: String,
        val artistText: String,
        val albumTitle: String?,
        val durationText: String?,
        val durationSec: Int?,
        val thumbnailUrl: String?,
        val isSong: Boolean
    )

    data class YtAudioStreamResult(
        val videoId: String,
        val streamUrl: String,
        val mimeType: String,
        val fileExtension: String,
        val contentLength: Long,
        val durationSeconds: Int,
        val fetchedAtMs: Long = System.currentTimeMillis()
    )

    @Volatile
    private var cachedConfig: InnertubeConfig? = null

    private val streamCache = ConcurrentHashMap<String, YtAudioStreamResult>()

    private suspend fun getInnertubeConfig(): InnertubeConfig? = configMutex.withLock {
        val current = cachedConfig
        if (current != null && (System.currentTimeMillis() - current.fetchedAtMs) < CONFIG_TTL_MS) {
            return current
        }
        return withContext(Dispatchers.IO) {
            try {
                val pageReq = Request.Builder()
                    .url(BASE_URL)
                    .header("User-Agent", CHROME_UA)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()

                val html = httpClient.newCall(pageReq).execute().use { resp ->
                    if (!resp.isSuccessful) "" else resp.body?.string().orEmpty()
                }
                val apiKey = Regex("\"INNERTUBE_API_KEY\":\\s*\"([^\"]+)\"")
                    .find(html)?.groupValues?.getOrNull(1)
                    ?: return@withContext null
                val clientVer = Regex("\"INNERTUBE_CLIENT_VERSION\":\\s*\"([^\"]+)\"")
                    .find(html)?.groupValues?.getOrNull(1)
                    ?: "1.20261011.16.00"

                val visitorData = runCatching {
                    val swReq = Request.Builder()
                        .url("https://www.youtube.com/sw.js_data")
                        .header("User-Agent", CHROME_UA)
                        .build()
                    httpClient.newCall(swReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@runCatching null
                        val raw = resp.body?.string().orEmpty()
                        if (raw.length <= 5) return@runCatching null
                        val parsedFromJson = runCatching {
                            val arr = JsonParser.parseString(raw.substring(5)).asJsonArray
                            arr[0].asJsonArray[2].asJsonArray[0].asJsonArray[0].asJsonArray[13].asString
                        }.getOrNull()?.takeIf { it.isNotBlank() }
                        parsedFromJson ?: Regex("(Cgt[a-zA-Z0-9_%-]{10,})")
                            .find(raw)?.groupValues?.getOrNull(1)
                            ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
                    }
                }.getOrNull()

                val resolved = InnertubeConfig(
                    apiKey = apiKey,
                    clientVersion = clientVer,
                    visitorData = visitorData,
                    fetchedAtMs = System.currentTimeMillis()
                )
                cachedConfig = resolved
                resolved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Gagal memuat Innertube config: ${e.message}")
                null
            }
        }
    }

    suspend fun searchTracks(query: String, songsOnly: Boolean = true): List<YtMusicSearchItem> =
        withContext(Dispatchers.IO) {
            val cleanQ = query.trim()
            if (cleanQ.isBlank()) return@withContext emptyList()
            val cfg = getInnertubeConfig() ?: return@withContext emptyList()

            try {
                val clientObj = JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", cfg.clientVersion)
                    addProperty("hl", "en")
                    addProperty("gl", "US")
                    if (!cfg.visitorData.isNullOrBlank()) {
                        addProperty("visitorData", cfg.visitorData)
                    }
                }
                val contextObj = JsonObject().apply { add("client", clientObj) }
                val payload = JsonObject().apply {
                    add("context", contextObj)
                    addProperty("query", cleanQ)
                    if (songsOnly) addProperty("params", FILTER_SONGS_PARAM)
                }

                val req = Request.Builder()
                    .url("$BASE_URL/youtubei/v1/search?key=${cfg.apiKey}")
                    .header("Content-Type", "application/json")
                    .header("Origin", BASE_URL)
                    .header("Referer", "$BASE_URL/")
                    .header("User-Agent", CHROME_UA)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                val root = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyList()
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) return@withContext emptyList()
                    runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
                } ?: return@withContext emptyList()

                parseSearchResults(root, songsOnly)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Search YT Music gagal untuk '$cleanQ': ${e.message}")
                emptyList()
            }
        }

    private fun parseSearchResults(root: JsonObject, songsOnly: Boolean): List<YtMusicSearchItem> {
        val sections = root.obj("contents")
            ?.obj("tabbedSearchResultsRenderer")
            ?.arr("tabs")
            ?.firstOrNull()
            ?.asJsonObjectOrNull()
            ?.obj("tabRenderer")
            ?.obj("content")
            ?.obj("sectionListRenderer")
            ?.arr("contents")
            ?: return emptyList()

        val out = mutableListOf<YtMusicSearchItem>()

        if (songsOnly) {
            val shelfContents = sections
                .mapNotNull { it.asJsonObjectOrNull()?.obj("musicShelfRenderer")?.arr("contents") }
                .firstOrNull()
            if (shelfContents != null) {
                for (elem in shelfContents) {
                    parseResponsiveItem(elem)?.let { out.add(it) }
                }
            }
            return out
        }

        for (secElem in sections) {
            val sec = secElem.asJsonObjectOrNull() ?: continue
            sec.obj("musicCardShelfRenderer")?.let { card ->
                val title = card.obj("title")?.arr("runs")?.firstOrNull()?.asJsonObjectOrNull()?.str("text").orEmpty()
                val subtitle = card.obj("subtitle")?.arr("runs")
                    ?.mapNotNull { it.asJsonObjectOrNull()?.str("text") }
                    ?.joinToString("")
                    .orEmpty()
                val videoId = card.obj("onTap")?.obj("watchEndpoint")?.str("videoId")
                    ?: card.arr("buttons")?.firstOrNull()?.asJsonObjectOrNull()
                        ?.obj("buttonRenderer")?.obj("command")?.obj("watchEndpoint")?.str("videoId")
                val thumbs = card.obj("thumbnail")?.obj("musicThumbnailRenderer")
                    ?.obj("thumbnail")?.arr("thumbnails")
                val thumbUrl = upgradeYtThumbnail(
                    thumbs?.lastOrNull()?.asJsonObjectOrNull()?.str("url")
                )
                if (!videoId.isNullOrBlank() && title.isNotBlank()) {
                    out.add(
                        YtMusicSearchItem(
                            title = title,
                            videoId = videoId,
                            artistText = subtitle,
                            albumTitle = null,
                            durationText = null,
                            durationSec = null,
                            thumbnailUrl = thumbUrl,
                            isSong = !subtitle.contains("video", ignoreCase = true)
                        )
                    )
                }
            }

            val shelfItems = sec.obj("musicShelfRenderer")?.arr("contents")
                ?: sec.obj("itemSectionRenderer")?.arr("contents")
            if (shelfItems != null) {
                for (elem in shelfItems) {
                    parseResponsiveItem(elem)?.let { out.add(it) }
                }
            }
        }
        return out
    }

    private fun parseResponsiveItem(elem: JsonElement): YtMusicSearchItem? {
        val ir = elem.asJsonObjectOrNull()?.obj("musicResponsiveListItemRenderer") ?: return null
        val col0Runs = parseFlexColumn(ir, 0)
        val col1Runs = parseFlexColumn(ir, 1)
        val col2Runs = parseFlexColumn(ir, 2)

        val title = col0Runs.firstOrNull()?.str("text")?.trim().orEmpty()
        if (title.isBlank()) return null

        val videoId = ir.obj("playlistItemData")?.str("videoId")
            ?: ir.obj("overlay")
                ?.obj("musicItemThumbnailOverlayRenderer")
                ?.obj("content")
                ?.obj("musicPlayButtonRenderer")
                ?.obj("playNavigationEndpoint")
                ?.obj("watchEndpoint")
                ?.str("videoId")
            ?: col0Runs.firstOrNull()?.obj("navigationEndpoint")?.obj("watchEndpoint")?.str("videoId")
            ?: return null

        val artistRuns = col1Runs.filter { run ->
            val bid = run.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("browseId").orEmpty()
            bid.startsWith("UC")
        }.mapNotNull { it.str("text")?.trim() }

        val firstCol1 = col1Runs.firstOrNull()?.str("text").orEmpty()
        val fallbackArtist = if (artistRuns.isEmpty()) {
            col1Runs.mapNotNull { it.str("text")?.trim() }
                .filter { t ->
                    t != "•" &&
                        !t.equals("Song", ignoreCase = true) &&
                        !t.equals("Video", ignoreCase = true) &&
                        !Regex("^\\d+:\\d+$").matches(t) &&
                        !t.contains("plays", ignoreCase = true) &&
                        !t.contains("views", ignoreCase = true)
                }
                .firstOrNull()
                .orEmpty()
        } else {
            artistRuns.joinToString(", ")
        }

        val albumRun = (col1Runs + col2Runs).firstOrNull { run ->
            val bid = run.obj("navigationEndpoint")?.obj("browseEndpoint")?.str("browseId").orEmpty()
            bid.startsWith("MPREb_")
        }
        val albumTitle = albumRun?.str("text")?.trim()

        val durationText = (col1Runs + col2Runs)
            .mapNotNull { it.str("text")?.trim() }
            .firstOrNull { Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$").matches(it) }
            ?: ir.arr("fixedColumns")?.firstOrNull()?.asJsonObjectOrNull()
                ?.obj("musicResponsiveListItemFixedColumnRenderer")
                ?.obj("text")?.arr("runs")?.firstOrNull()?.asJsonObjectOrNull()?.str("text")

        val durationSec = durationText?.let { parseDurationTextToSec(it) }

        val rawThumb = ir.obj("thumbnail")
            ?.obj("musicThumbnailRenderer")
            ?.obj("thumbnail")
            ?.arr("thumbnails")
            ?.lastOrNull()
            ?.asJsonObjectOrNull()
            ?.str("url")
        val thumbUrl = upgradeYtThumbnail(rawThumb) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val isSong = firstCol1.contains("song", ignoreCase = true) ||
            !firstCol1.contains("video", ignoreCase = true)

        return YtMusicSearchItem(
            title = title,
            videoId = videoId,
            artistText = fallbackArtist,
            albumTitle = albumTitle,
            durationText = durationText,
            durationSec = durationSec,
            thumbnailUrl = thumbUrl,
            isSong = isSong
        )
    }

    /**
     * Mengambil direct stream audio lengkap (M4A itag 140 / WebM itag 251) memakai client VISIONOS.
     */
    suspend fun getAudioStream(videoId: String): YtAudioStreamResult? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        val cached = streamCache[videoId]
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAtMs) < STREAM_CACHE_TTL_MS) {
            return@withContext cached
        }

        val cfg = getInnertubeConfig()
        try {
            val clientObj = JsonObject().apply {
                addProperty("clientName", "VISIONOS")
                addProperty("clientVersion", "1.02")
                addProperty("deviceMake", "Apple")
                addProperty("deviceModel", "RealityDevice17,1")
                addProperty("osName", "visionOS")
                addProperty("osVersion", "26.5.23O471")
                if (!cfg?.visitorData.isNullOrBlank()) {
                    addProperty("visitorData", cfg?.visitorData)
                }
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            val payload = JsonObject().apply {
                add("context", JsonObject().apply { add("client", clientObj) })
                addProperty("videoId", videoId)
            }

            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("Content-Type", "application/json")
                .header("User-Agent", VISIONOS_UA)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val data = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
            } ?: return@withContext null

            val status = data.obj("playabilityStatus")?.str("status")
            if (status != "OK") return@withContext null

            val adaptive = data.obj("streamingData")?.arr("adaptiveFormats") ?: return@withContext null
            val formats = adaptive.mapNotNull { it.asJsonObjectOrNull() }

            val chosen = formats.firstOrNull { it.int("itag") == 140 && !it.str("url").isNullOrBlank() }
                ?: formats.firstOrNull { it.int("itag") == 251 && !it.str("url").isNullOrBlank() }
                ?: formats.firstOrNull {
                    it.str("mimeType")?.contains("audio", ignoreCase = true) == true &&
                        !it.str("url").isNullOrBlank()
                }
                ?: return@withContext null

            val baseUrl = chosen.str("url") ?: return@withContext null
            val contentLength = chosen.str("contentLength")?.toLongOrNull()
                ?: chosen.long("contentLength")
                ?: 0L
            val fullUrl = if (contentLength > 0L) "$baseUrl&range=0-${contentLength - 1L}" else baseUrl
            val rawMime = chosen.str("mimeType").orEmpty().substringBefore(";").ifBlank { "audio/mp4" }
            val ext = if (rawMime.contains("webm", ignoreCase = true) || rawMime.contains("opus", ignoreCase = true)) {
                "webm"
            } else {
                "m4a"
            }
            val lengthSeconds = data.obj("videoDetails")?.str("lengthSeconds")?.toIntOrNull() ?: 0

            val result = YtAudioStreamResult(
                videoId = videoId,
                streamUrl = fullUrl,
                mimeType = rawMime,
                fileExtension = ext,
                contentLength = contentLength,
                durationSeconds = lengthSeconds
            )
            streamCache[videoId] = result
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Gagal ambil stream YT Music untuk $videoId: ${e.message}")
            null
        }
    }

    /**
     * Mengunduh lagu soundtrack (audio penuh YT Music .m4a/.webm atau fallback preview .mp3)
     * langsung ke folder Music/Yuranime atau penyimpanan lokal aplikasi tanpa perlu izin storage tambahan.
     */
    suspend fun downloadSoundtrackToDevice(
        context: Context,
        track: AnimeSoundtrackTrack,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var targetUrl: String? = null
            var mimeType = "audio/mp4"
            var ext = "m4a"

            val vid = track.ytVideoId
            if (!vid.isNullOrBlank()) {
                val stream = getAudioStream(vid)
                if (stream != null) {
                    targetUrl = stream.streamUrl
                    mimeType = stream.mimeType
                    ext = stream.fileExtension
                }
            }

            if (targetUrl.isNullOrBlank()) {
                val preview = track.previewUrl?.takeIf { it.startsWith("http") }
                if (!preview.isNullOrBlank()) {
                    targetUrl = preview
                    if (preview.contains(".mp3", ignoreCase = true)) {
                        mimeType = "audio/mpeg"
                        ext = "mp3"
                    } else {
                        mimeType = "audio/mp4"
                        ext = "m4a"
                    }
                }
            }

            val resolvedUrl = targetUrl
                ?: return@withContext Result.failure(IllegalStateException("Tautan unduhan audio tidak tersedia"))

            val safeArtist = sanitizeFileName(track.artist).ifBlank { "Artist" }
            val safeTitle = sanitizeFileName(track.title).ifBlank { "Soundtrack" }
            val fileName = "$safeArtist - $safeTitle.$ext"

            val req = Request.Builder()
                .url(resolvedUrl)
                .header(
                    "User-Agent",
                    if (resolvedUrl.contains("googlevideo.com", ignoreCase = true)) VISIONOS_UA else CHROME_UA
                )
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Server audio mengembalikan HTTP ${resp.code}"))
                }
                val body = resp.body
                    ?: return@withContext Result.failure(IllegalStateException("Data audio kosong"))
                val totalLen = body.contentLength().coerceAtLeast(0L)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.TITLE, track.title)
                        put(MediaStore.Audio.Media.ARTIST, track.artist)
                        if (!track.album.isNullOrBlank()) {
                            put(MediaStore.Audio.Media.ALBUM, track.album)
                        }
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Yuranime")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }
                    val resolver = context.applicationContext.contentResolver
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure(IllegalStateException("Gagal membuat file di folder Music"))

                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            copyWithProgress(body.byteStream(), out, totalLen, onProgress)
                        } ?: throw IllegalStateException("Gagal membuka output stream")

                        val doneValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, doneValues, null, null)
                        return@withContext Result.success("Music/Yuranime/$fileName")
                    } catch (e: Exception) {
                        runCatching { resolver.delete(uri, null, null) }
                        throw e
                    }
                } else {
                    val musicDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        ?: File(context.applicationContext.filesDir, "music")
                    val targetDir = File(musicDir, "Yuranime").apply { if (!exists()) mkdirs() }
                    val targetFile = File(targetDir, fileName)
                    FileOutputStream(targetFile).use { out ->
                        copyWithProgress(body.byteStream(), out, totalLen, onProgress)
                    }
                    return@withContext Result.success("Music/Yuranime/$fileName")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunduh soundtrack ${track.title}: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun copyWithProgress(
        input: java.io.InputStream,
        output: java.io.OutputStream,
        totalBytes: Long,
        onProgress: (Float) -> Unit
    ) {
        val buf = ByteArray(16 * 1024)
        var written = 0L
        var lastEmit = -1
        while (true) {
            val read = input.read(buf)
            if (read == -1) break
            output.write(buf, 0, read)
            written += read
            if (totalBytes > 0L) {
                val pct = ((written * 100L) / totalBytes).toInt().coerceIn(0, 100)
                if (pct != lastEmit) {
                    lastEmit = pct
                    onProgress(pct / 100f)
                }
            }
        }
        output.flush()
        onProgress(1f)
    }

    fun upgradeYtThumbnail(rawUrl: String?): String? {
        val url = rawUrl?.trim()?.takeIf { it.startsWith("http") } ?: return null
        return if (url.contains("googleusercontent.com")) {
            url.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
        } else {
            url
        }
    }

    private fun parseDurationTextToSec(text: String): Int? {
        val parts = text.split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[/\\\\?%*:|\"<>]"), "_").trim().take(60)
    }

    private fun parseFlexColumn(ir: JsonObject, index: Int): List<JsonObject> {
        return ir.arr("flexColumns")
            ?.getOrNull(index)
            ?.asJsonObjectOrNull()
            ?.obj("musicResponsiveListItemFlexColumnRenderer")
            ?.obj("text")
            ?.arr("runs")
            ?.mapNotNull { it.asJsonObjectOrNull() }
            ?: emptyList()
    }

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonArray.getOrNull(idx: Int): JsonElement? =
        if (idx in 0 until size()) get(idx) else null

    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arr(key: String): JsonArray? =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asString }.getOrNull() }

    private fun JsonObject.int(key: String): Int? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.let { runCatching { it.asInt }.getOrNull() }

    private fun JsonObject.long(key: String): Long? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.let { runCatching { it.asLong }.getOrNull() }
}
