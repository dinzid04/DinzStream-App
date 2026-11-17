package com.yuranime.dinz.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yuranime.dinz.MainActivity
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.model.AnimeItem
import com.yuranime.dinz.data.model.normalizeImageUrl
import com.yuranime.dinz.data.repository.AnimeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class ScheduledAnimeAlarm(
    val movieId: String,
    val title: String,
    val coverUrl: String,
    val posterUrl: String,
    val episodeNumber: String,
    val dayApiName: String,
    val timeRaw: String,
    val triggerAtMillis: Long
)

object AnimeReleaseScheduler {
    const val CHANNEL_ID = "yuranime_anime_release_channel"
    const val EXTRA_OPEN_ANIME_ID = "extra_open_anime_id"

    internal const val EXTRA_MOVIE_ID = "extra_movie_id"
    internal const val EXTRA_TITLE = "extra_title"
    internal const val EXTRA_COVER_URL = "extra_cover_url"
    internal const val EXTRA_POSTER_URL = "extra_poster_url"
    internal const val EXTRA_EPISODE_NUM = "extra_episode_num"
    internal const val EXTRA_DAY_API = "extra_day_api"
    internal const val EXTRA_TIME_RAW = "extra_time_raw"

    private const val PREFS_NAME = "yuranime_release_alarms_v1"
    private const val KEY_ALARMS_JSON = "scheduled_alarms_json"
    private const val KEY_LAST_SYNC_MS = "last_week_sync_ms"
    private const val SYNC_THROTTLE_MS = 3 * 60 * 60 * 1000L // 3 jam

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val RELEASE_CAPTIONS = listOf(
        "Episode terbaru baru saja rilis! Tonton sekarang sebelum terkena spoiler.",
        "Waktunya maraton! Episode teranyar sudah tayang dan siap diputar.",
        "Jangan sampai ketinggalan, episode baru anime ini baru saja mendarat!",
        "Notifikasi tayang berbunyi! Yuk cek kelanjutan ceritanya sekarang.",
        "Siapkan camilan favoritmu, episode baru sudah tersedia di Yuranime.",
        "Episode yang ditunggu-tunggu akhirnya rilis hari ini!",
        "Update anime favoritmu sudah tiba! Ketuk untuk membuka detail anime.",
        "Baru rilis sesuai jadwal tayang! Langsung simak episodenya sekarang.",
        "Keseruan berlanjut! Episode baru telah hadir dan siap kamu tonton."
    )

    fun pickRandomCaption(): String {
        return RELEASE_CAPTIONS[Random.nextInt(RELEASE_CAPTIONS.size)]
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Jadwal Rilis Anime",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi otomatis saat episode anime baru rilis sesuai jadwal."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun syncFullWeekScheduleInBackground(context: Context, force: Boolean = false) {
        val appCtx = context.applicationContext
        if (!AppPreferences.releaseNotificationsEnabled.value) return
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
        val now = System.currentTimeMillis()
        if (!force && (now - lastSync) < SYNC_THROTTLE_MS) {
            rescheduleSavedAlarms(appCtx)
            return
        }

        syncScope.launch {
            runCatching {
                val repo = AnimeRepository()
                val days = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")
                val collected = mutableListOf<ScheduledAnimeAlarm>()
                val nowMs = System.currentTimeMillis()

                for (day in days) {
                    val items = repo.getSchedule(day).getOrDefault(emptyList())
                    for (item in items) {
                        buildAlarmEntry(item, day, nowMs)?.let { collected.add(it) }
                    }
                }

                if (collected.isNotEmpty()) {
                    val distinct = collected.distinctBy { it.movieId }
                    saveAlarms(appCtx, distinct)
                    distinct.forEach { scheduleSingleAlarm(appCtx, it) }
                    prefs.edit().putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis()).apply()
                }
            }
        }
    }

    fun scheduleFromScheduleItems(
        context: Context,
        dayApiName: String,
        items: List<AnimeItem>
    ) {
        val appCtx = context.applicationContext
        if (!AppPreferences.releaseNotificationsEnabled.value || items.isEmpty()) return

        syncScope.launch {
            runCatching {
                val nowMs = System.currentTimeMillis()
                val existingMap = loadSavedAlarms(appCtx).associateBy { it.movieId }.toMutableMap()
                for (item in items) {
                    val built = buildAlarmEntry(item, dayApiName, nowMs) ?: continue
                    existingMap[built.movieId] = built
                    scheduleSingleAlarm(appCtx, built)
                }
                saveAlarms(appCtx, existingMap.values.toList())
            }
        }
    }

    fun rescheduleSavedAlarms(context: Context) {
        val appCtx = context.applicationContext
        if (!AppPreferences.releaseNotificationsEnabled.value) return
        val nowMs = System.currentTimeMillis()
        val updated = loadSavedAlarms(appCtx).map { alarm ->
            if (alarm.triggerAtMillis <= nowMs - 60_000L) {
                // Hitung ulang jadwal minggu berikutnya bila sudah lewat
                val nextTrigger = parseNextReleaseMillis(alarm.dayApiName, alarm.timeRaw, nowMs)
                    ?: (alarm.triggerAtMillis + 7L * 24L * 60L * 60L * 1000L)
                alarm.copy(triggerAtMillis = nextTrigger)
            } else {
                alarm
            }
        }
        saveAlarms(appCtx, updated)
        updated.forEach { scheduleSingleAlarm(appCtx, it) }
    }

    fun cancelAllScheduledAlarms(context: Context) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val saved = loadSavedAlarms(appCtx)
        if (alarmManager != null) {
            saved.forEach { alarm ->
                val pi = buildBroadcastPendingIntent(appCtx, alarm, PendingIntent.FLAG_NO_CREATE)
                if (pi != null) {
                    alarmManager.cancel(pi)
                    pi.cancel()
                }
            }
        }
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ALARMS_JSON)
            .remove(KEY_LAST_SYNC_MS)
            .apply()
    }

    internal fun scheduleSingleAlarm(context: Context, alarm: ScheduledAnimeAlarm) {
        val appCtx = context.applicationContext
        ensureNotificationChannel(appCtx)
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildBroadcastPendingIntent(
            appCtx,
            alarm,
            PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Throwable) {
            }
        } catch (_: Throwable) {
        }
    }

    internal fun buildAlarmEntry(
        item: AnimeItem,
        dayApiName: String,
        nowMs: Long
    ): ScheduledAnimeAlarm? {
        val movieId = item.id?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val title = item.title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val timeRaw = item.time?.trim().orEmpty()
        if (timeRaw.isBlank()) return null

        val triggerAt = parseNextReleaseMillis(dayApiName, timeRaw, nowMs) ?: return null
        val rawCover = item.imageCover?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeImageUrl(it) }.orEmpty()
        val rawPoster = item.imagePoster?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeImageUrl(it) }
            ?: item.poster?.trim()?.takeIf { it.isNotBlank() }?.let { normalizeImageUrl(it) }
            ?: ""

        // Hanya simpan coverUrl jika benar-benar berbeda dari poster vertikal
        val distinctHorizontalCover = if (rawCover.isNotBlank() && rawCover != rawPoster) rawCover else ""

        return ScheduledAnimeAlarm(
            movieId = movieId,
            title = title,
            coverUrl = distinctHorizontalCover,
            posterUrl = rawPoster,
            episodeNumber = item.index?.trim().orEmpty(),
            dayApiName = dayApiName.uppercase(Locale.ROOT),
            timeRaw = timeRaw,
            triggerAtMillis = triggerAt
        )
    }

    internal fun parseNextReleaseMillis(dayApiName: String, timeRaw: String, nowMs: Long): Long? {
        val clean = timeRaw.trim()
        if (clean.isBlank()) return null

        // 1. Format jam absolut "HH:mm" atau "HH.mm"
        val hhmmMatch = Regex("""\b(\d{1,2})[:.](\d{2})\b""").find(clean)
        if (hhmmMatch != null) {
            val hour = hhmmMatch.groupValues[1].toIntOrNull()
            val minute = hhmmMatch.groupValues[2].toIntOrNull()
            if (hour != null && hour in 0..23 && minute != null && minute in 0..59) {
                val targetDow = dayApiToCalendarDay(dayApiName)
                val tz = TimeZone.getTimeZone("Asia/Jakarta")
                val cal = Calendar.getInstance(tz).apply {
                    timeInMillis = nowMs
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (targetDow != null) {
                    val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                    var daysDiff = targetDow - currentDow
                    if (daysDiff < 0) daysDiff += 7
                    cal.add(Calendar.DAY_OF_YEAR, daysDiff)
                }
                // Jika jadwal hari ini sudah lewat lebih dari 2 menit, jadwalkan ke minggu depan
                if (cal.timeInMillis <= nowMs - 120_000L) {
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                }
                return cal.timeInMillis
            }
        }

        // 2. Format countdown relatif (contoh: "2 jam 30 menit", "1j 15m", "45 menit")
        val hours = Regex("""(\d+)\s*(?:jam|j|hour|hours|hr|h)\b""", RegexOption.IGNORE_CASE)
            .find(clean)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val mins = Regex("""(\d+)\s*(?:menit|mnt|min|mins|m)\b""", RegexOption.IGNORE_CASE)
            .find(clean)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        if (hours > 0L || mins > 0L) {
            val deltaMs = (hours * 3600_000L) + (mins * 60_000L)
            if (deltaMs >= 60_000L) {
                return nowMs + deltaMs
            }
        }

        return null
    }

    private fun dayApiToCalendarDay(dayApiName: String): Int? {
        return when (dayApiName.trim().uppercase(Locale.ROOT)) {
            "SENIN" -> Calendar.MONDAY
            "SELASA" -> Calendar.TUESDAY
            "RABU" -> Calendar.WEDNESDAY
            "KAMIS" -> Calendar.THURSDAY
            "JUMAT" -> Calendar.FRIDAY
            "SABTU" -> Calendar.SATURDAY
            "MINGGU" -> Calendar.SUNDAY
            else -> null
        }
    }

    private fun buildBroadcastPendingIntent(
        context: Context,
        alarm: ScheduledAnimeAlarm,
        extraFlag: Int
    ): PendingIntent? {
        val intent = Intent(context, AnimeReleaseReceiver::class.java).apply {
            putExtra(EXTRA_MOVIE_ID, alarm.movieId)
            putExtra(EXTRA_TITLE, alarm.title)
            putExtra(EXTRA_COVER_URL, alarm.coverUrl)
            putExtra(EXTRA_POSTER_URL, alarm.posterUrl)
            putExtra(EXTRA_EPISODE_NUM, alarm.episodeNumber)
            putExtra(EXTRA_DAY_API, alarm.dayApiName)
            putExtra(EXTRA_TIME_RAW, alarm.timeRaw)
        }
        val flags = extraFlag or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            alarm.movieId.hashCode(),
            intent,
            flags
        )
    }

    private fun saveAlarms(context: Context, list: List<ScheduledAnimeAlarm>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("movieId", a.movieId)
                    put("title", a.title)
                    put("coverUrl", a.coverUrl)
                    put("posterUrl", a.posterUrl)
                    put("episodeNumber", a.episodeNumber)
                    put("dayApiName", a.dayApiName)
                    put("timeRaw", a.timeRaw)
                    put("triggerAtMillis", a.triggerAtMillis)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARMS_JSON, arr.toString())
            .apply()
    }

    internal fun loadSavedAlarms(context: Context): List<ScheduledAnimeAlarm> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALARMS_JSON, null)
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<ScheduledAnimeAlarm>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val mId = obj.optString("movieId")
                val t = obj.optString("title")
                if (mId.isBlank() || t.isBlank()) continue
                out.add(
                    ScheduledAnimeAlarm(
                        movieId = mId,
                        title = t,
                        coverUrl = obj.optString("coverUrl"),
                        posterUrl = obj.optString("posterUrl"),
                        episodeNumber = obj.optString("episodeNumber"),
                        dayApiName = obj.optString("dayApiName"),
                        timeRaw = obj.optString("timeRaw"),
                        triggerAtMillis = obj.optLong("triggerAtMillis")
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * Receiver notifikasi jadwal rilis anime.
 * Wajib mengambil gambar Cover/Banner HORIZONTAL (mengambil detail anime jika data jadwal hanya punya poster)
 * lalu menampilkan notifikasi BigPictureStyle dengan judul + episode + caption acak.
 */
class AnimeReleaseReceiver : BroadcastReceiver() {

    companion object {
        private val imageClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val movieId = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_MOVIE_ID).orEmpty()
        if (movieId.isBlank()) return

        val initialTitle = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_TITLE).orEmpty().ifBlank { "Anime Baru" }
        val initialCoverUrl = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_COVER_URL).orEmpty()
        val initialPosterUrl = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_POSTER_URL).orEmpty()
        val initialEpNum = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_EPISODE_NUM).orEmpty()
        val dayApi = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_DAY_API).orEmpty()
        val timeRaw = intent.getStringExtra(AnimeReleaseScheduler.EXTRA_TIME_RAW).orEmpty()

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (!AppPreferences.releaseNotificationsEnabled.value) return@launch
                if (!AnimeReleaseScheduler.hasNotificationPermission(appCtx)) return@launch

                val repository = AnimeRepository()
                val detail = repository.getMovieDetail(movieId).getOrNull()
                val movie = detail?.movie

                val finalTitle = movie?.title?.takeIf { it.isNotBlank() } ?: initialTitle
                val finalEpNum = detail?.latestEpisode?.index?.takeIf { it.isNotBlank() } ?: initialEpNum

                // Kumpulkan kandidat gambar horizontal (cover/banner & thumbnail episode 16:9), BUKAN poster vertikal
                val candidates = LinkedHashSet<String>()
                movie?.imageCover?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                    val normalized = normalizeImageUrl(raw)
                    val posterNorm = movie.imagePoster?.let { normalizeImageUrl(it) }.orEmpty()
                    if (normalized.isNotBlank() && normalized != posterNorm) {
                        candidates.add(normalized)
                    }
                }
                if (initialCoverUrl.isNotBlank() && initialCoverUrl != initialPosterUrl) {
                    candidates.add(initialCoverUrl)
                }

                // Jika cover utama masih kosong/sama dengan poster, ambil dari endpoint cover komunitas
                if (candidates.isEmpty()) {
                    val covers = repository.getMovieCovers(movieId).getOrDefault(emptyList())
                    covers.forEach { cov ->
                        val img = cov.getEffectiveImage()
                        if (img.isNotBlank()) candidates.add(img)
                    }
                }

                // Fallback horizontal 16:9 dari thumbnail episode terbaru
                detail?.latestEpisode?.getEffectiveImage()?.takeIf { it.isNotBlank() }?.let {
                    candidates.add(it)
                }

                // Cari bitmap yang benar-benar berorientasi horizontal (width > height)
                var landscapeBitmap: Bitmap? = null
                for (url in candidates) {
                    val bmp = downloadBitmapSafely(url) ?: continue
                    if (bmp.width > bmp.height) {
                        landscapeBitmap = bmp
                        break
                    } else {
                        bmp.recycle()
                    }
                }

                // Fallback darurat bila hanya tersedia gambar tunggal: crop rasio 16:9 horizontal
                if (landscapeBitmap == null) {
                    val fallbackUrl = candidates.firstOrNull()
                        ?: movie?.getEffectiveCover()?.takeIf { it.isNotBlank() }
                        ?: initialCoverUrl
                    if (!fallbackUrl.isNullOrBlank()) {
                        downloadBitmapSafely(fallbackUrl)?.let { rawBmp ->
                            landscapeBitmap = ensureLandscapeCrop(rawBmp)
                        }
                    }
                }

                showReleaseNotification(
                    context = appCtx,
                    movieId = movieId,
                    animeTitle = finalTitle,
                    episodeNum = finalEpNum,
                    horizontalCover = landscapeBitmap
                )

                // Jadwalkan ulang untuk minggu depan bila jam tayang harian/mingguan terdeteksi
                if (dayApi.isNotBlank() && timeRaw.isNotBlank()) {
                    val nextWeekTrigger = AnimeReleaseScheduler.parseNextReleaseMillis(
                        dayApiName = dayApi,
                        timeRaw = timeRaw,
                        nowMs = System.currentTimeMillis() + 180_000L
                    )
                    if (nextWeekTrigger != null) {
                        AnimeReleaseScheduler.scheduleSingleAlarm(
                            appCtx,
                            ScheduledAnimeAlarm(
                                movieId = movieId,
                                title = finalTitle,
                                coverUrl = candidates.firstOrNull() ?: initialCoverUrl,
                                posterUrl = initialPosterUrl,
                                episodeNumber = finalEpNum,
                                dayApiName = dayApi,
                                timeRaw = timeRaw,
                                triggerAtMillis = nextWeekTrigger
                            )
                        )
                    }
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showReleaseNotification(
        context: Context,
        movieId: String,
        animeTitle: String,
        episodeNum: String,
        horizontalCover: Bitmap?
    ) {
        if (!AnimeReleaseScheduler.hasNotificationPermission(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        AnimeReleaseScheduler.ensureNotificationChannel(context)

        val notifTitle = if (episodeNum.isNotBlank()) {
            "$animeTitle - Episode $episodeNum"
        } else {
            "$animeTitle - Episode Baru"
        }
        val caption = AnimeReleaseScheduler.pickRandomCaption()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AnimeReleaseScheduler.EXTRA_OPEN_ANIME_ID, movieId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            movieId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, AnimeReleaseScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_yuranime_monochrome)
            .setContentTitle(notifTitle)
            .setContentText(caption)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (horizontalCover != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(horizontalCover)
                    .setBigContentTitle(notifTitle)
                    .setSummaryText(caption)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(caption)
                    .setBigContentTitle(notifTitle)
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(movieId.hashCode(), builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun downloadBitmapSafely(url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .build()
            imageClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1080) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun ensureLandscapeCrop(source: Bitmap): Bitmap {
        if (source.width > source.height) return source
        val targetHeight = (source.width * 9f / 16f).toInt().coerceIn(1, source.height)
        val top = ((source.height - targetHeight) / 3).coerceAtLeast(0)
        return try {
            val cropped = Bitmap.createBitmap(source, 0, top, source.width, targetHeight)
            if (cropped !== source) source.recycle()
            cropped
        } catch (_: Throwable) {
            source
        }
    }
}
