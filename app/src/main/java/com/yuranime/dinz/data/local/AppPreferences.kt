package com.yuranime.dinz.data.local

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

data class AppStorageStats(
    val totalDeviceBytes: Long,
    val availableDeviceBytes: Long,
    val downloadBytes: Long,
    val downloadItemCount: Int,
    val cacheBytes: Long
) {
    val usedOtherBytes: Long
        get() = (totalDeviceBytes - availableDeviceBytes - downloadBytes - cacheBytes).coerceAtLeast(0L)
}

data class SavedGoogleUserSession(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)

enum class HomeSliderMode(val id: Int, val title: String, val description: String) {
    DEFAULT(0, "Slider dengan Poster (Default)", "Menampilkan kartu poster vertikal di pojok kiri bawah"),
    CLEAN_COVER(1, "Slider Tanpa Poster (Clean Cover)", "Menampilkan cover anime penuh sinematik dan ikon pencarian")
}

object AppPreferences {
    private const val PREFS_NAME = "yuranime_app_prefs"
    private const val KEY_SLIDER_MODE = "key_home_slider_mode"
    private const val KEY_AUTO_NEXT = "key_auto_next_episode"
    private const val KEY_LIQUID_GLASS = "key_liquid_glass_navbar"
    private const val KEY_DEFAULT_ANIME_QUALITY = "key_default_anime_quality"
    private const val KEY_DEFAULT_PLAYBACK_SPEED = "key_default_playback_speed"
    private const val KEY_AUTO_PLAY = "key_auto_play_enabled"
    private const val KEY_AUTO_SKIP_INTRO = "key_auto_skip_intro"
    private const val KEY_PRELOAD_NEXT_EPISODE = "key_preload_next_episode"
    private const val KEY_DOUBLE_TAP_SEEK_ENABLED = "key_double_tap_seek_enabled"
    private const val KEY_DOUBLE_TAP_SEEK_SECONDS = "key_double_tap_seek_seconds"
    private const val KEY_RELEASE_NOTIFICATIONS_ENABLED = "key_release_notifications_enabled"
    private const val KEY_SHOW_MINI_TITLE = "key_show_mini_title"
    private const val KEY_SHOW_CUSTOM_TITLE = "key_show_custom_title"
    private const val KEY_SHOW_ROLE = "key_show_role"
    private const val KEY_COSMETIC_ANIMATE = "key_cosmetic_animate"

    // Onboarding Coachmark Keys
    private const val KEY_ONBOARDING_HOME = "key_onboarding_home_completed"
    private const val KEY_ONBOARDING_DETAIL = "key_onboarding_detail_completed"
    private const val KEY_ONBOARDING_SEARCH = "key_onboarding_search_completed"
    private const val KEY_ONBOARDING_STREAMING = "key_onboarding_streaming_completed"

    // Google Session Keys
    private const val KEY_GOOGLE_UID = "key_google_session_uid"
    private const val KEY_GOOGLE_NAME = "key_google_session_name"
    private const val KEY_GOOGLE_EMAIL = "key_google_session_email"
    private const val KEY_GOOGLE_AVATAR = "key_google_session_avatar"

    private fun isLiquidGlassDefault(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    private val _sliderMode = MutableStateFlow(HomeSliderMode.DEFAULT)
    val sliderMode: StateFlow<HomeSliderMode> = _sliderMode.asStateFlow()

    private val _autoNext = MutableStateFlow(true)
    val autoNext: StateFlow<Boolean> = _autoNext.asStateFlow()

    private val _liquidGlassEnabled = MutableStateFlow(isLiquidGlassDefault())
    val liquidGlassEnabled: StateFlow<Boolean> = _liquidGlassEnabled.asStateFlow()

    private val _defaultAnimeQuality = MutableStateFlow("Auto")
    val defaultAnimeQuality: StateFlow<String> = _defaultAnimeQuality.asStateFlow()

    private val _defaultPlaybackSpeed = MutableStateFlow(1.0f)
    val defaultPlaybackSpeed: StateFlow<Float> = _defaultPlaybackSpeed.asStateFlow()

    private val _autoPlayEnabled = MutableStateFlow(true)
    val autoPlayEnabled: StateFlow<Boolean> = _autoPlayEnabled.asStateFlow()

    private val _autoSkipIntro = MutableStateFlow(false)
    val autoSkipIntro: StateFlow<Boolean> = _autoSkipIntro.asStateFlow()

    private val _preloadNextEpisode = MutableStateFlow(true)
    val preloadNextEpisode: StateFlow<Boolean> = _preloadNextEpisode.asStateFlow()

    private val _showMiniTitle = MutableStateFlow(true)
    val showMiniTitle: StateFlow<Boolean> = _showMiniTitle.asStateFlow()

    private val _showCustomTitle = MutableStateFlow(true)
    val showCustomTitle: StateFlow<Boolean> = _showCustomTitle.asStateFlow()

    private val _showRole = MutableStateFlow(true)
    val showRole: StateFlow<Boolean> = _showRole.asStateFlow()

    private val _cosmeticAnimate = MutableStateFlow(true)
    val cosmeticAnimate: StateFlow<Boolean> = _cosmeticAnimate.asStateFlow()

    private val _doubleTapSeekEnabled = MutableStateFlow(true)
    val doubleTapSeekEnabled: StateFlow<Boolean> = _doubleTapSeekEnabled.asStateFlow()

    private val _doubleTapSeekSeconds = MutableStateFlow(10)
    val doubleTapSeekSeconds: StateFlow<Int> = _doubleTapSeekSeconds.asStateFlow()

    private val _releaseNotificationsEnabled = MutableStateFlow(true)
    val releaseNotificationsEnabled: StateFlow<Boolean> = _releaseNotificationsEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeId = prefs.getInt(KEY_SLIDER_MODE, HomeSliderMode.DEFAULT.id)
        _sliderMode.value = if (modeId == HomeSliderMode.CLEAN_COVER.id) HomeSliderMode.CLEAN_COVER else HomeSliderMode.DEFAULT
        _autoNext.value = prefs.getBoolean(KEY_AUTO_NEXT, true)
        _liquidGlassEnabled.value = prefs.getBoolean(KEY_LIQUID_GLASS, isLiquidGlassDefault())
        _defaultAnimeQuality.value = prefs.getString(KEY_DEFAULT_ANIME_QUALITY, "Auto") ?: "Auto"
        _defaultPlaybackSpeed.value = prefs.getFloat(KEY_DEFAULT_PLAYBACK_SPEED, 1.0f).coerceIn(0.5f, 2.0f)
        _autoPlayEnabled.value = prefs.getBoolean(KEY_AUTO_PLAY, true)
        _autoSkipIntro.value = prefs.getBoolean(KEY_AUTO_SKIP_INTRO, false)
        _preloadNextEpisode.value = prefs.getBoolean(KEY_PRELOAD_NEXT_EPISODE, true)
        _showMiniTitle.value = prefs.getBoolean(KEY_SHOW_MINI_TITLE, true)
        _showCustomTitle.value = prefs.getBoolean(KEY_SHOW_CUSTOM_TITLE, true)
        _showRole.value = prefs.getBoolean(KEY_SHOW_ROLE, true)
        _cosmeticAnimate.value = prefs.getBoolean(KEY_COSMETIC_ANIMATE, true)
        _doubleTapSeekEnabled.value = prefs.getBoolean(KEY_DOUBLE_TAP_SEEK_ENABLED, true)
        val rawSkipSec = prefs.getInt(KEY_DOUBLE_TAP_SEEK_SECONDS, 10)
        _doubleTapSeekSeconds.value = if (rawSkipSec in setOf(5, 10, 15, 30)) rawSkipSec else 10
        _releaseNotificationsEnabled.value = prefs.getBoolean(KEY_RELEASE_NOTIFICATIONS_ENABLED, true)
    }

    fun setSliderMode(context: Context, mode: HomeSliderMode) {
        _sliderMode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SLIDER_MODE, mode.id)
            .apply()
    }

    fun setAutoNext(context: Context, enabled: Boolean) {
        _autoNext.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_NEXT, enabled)
            .apply()
    }

    fun setLiquidGlassEnabled(context: Context, enabled: Boolean) {
        _liquidGlassEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIQUID_GLASS, enabled)
            .apply()
    }

    fun setDefaultAnimeQuality(context: Context, quality: String) {
        val clean = quality.trim().ifBlank { "Auto" }
        _defaultAnimeQuality.value = clean
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEFAULT_ANIME_QUALITY, clean)
            .apply()
    }

    fun setDefaultPlaybackSpeed(context: Context, speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _defaultPlaybackSpeed.value = clamped
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_DEFAULT_PLAYBACK_SPEED, clamped)
            .apply()
    }

    fun setAutoPlayEnabled(context: Context, enabled: Boolean) {
        _autoPlayEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_PLAY, enabled)
            .apply()
    }

    fun setAutoSkipIntro(context: Context, enabled: Boolean) {
        _autoSkipIntro.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_SKIP_INTRO, enabled)
            .apply()
    }

    fun setPreloadNextEpisode(context: Context, enabled: Boolean) {
        _preloadNextEpisode.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRELOAD_NEXT_EPISODE, enabled)
            .apply()
    }

    fun setDoubleTapSeekEnabled(context: Context, enabled: Boolean) {
        _doubleTapSeekEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DOUBLE_TAP_SEEK_ENABLED, enabled)
            .apply()
    }

    fun setDoubleTapSeekSeconds(context: Context, seconds: Int) {
        val valid = if (seconds in setOf(5, 10, 15, 30)) seconds else 10
        _doubleTapSeekSeconds.value = valid
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_DOUBLE_TAP_SEEK_SECONDS, valid)
            .apply()
    }

    fun setReleaseNotificationsEnabled(context: Context, enabled: Boolean) {
        _releaseNotificationsEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RELEASE_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    fun setShowMiniTitle(context: Context, enabled: Boolean) {
        _showMiniTitle.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_MINI_TITLE, enabled)
            .apply()
    }

    fun setShowCustomTitle(context: Context, enabled: Boolean) {
        _showCustomTitle.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_CUSTOM_TITLE, enabled)
            .apply()
    }

    fun setShowRole(context: Context, enabled: Boolean) {
        _showRole.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_ROLE, enabled)
            .apply()
    }

    fun setCosmeticAnimatePref(context: Context, enabled: Boolean) {
        _cosmeticAnimate.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COSMETIC_ANIMATE, enabled)
            .apply()
    }

    fun computeDirectoryBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += computeDirectoryBytes(child)
        }
        return total
    }

    fun readAppStorageStats(context: Context): AppStorageStats {
        val (totalDevice, availableDevice) = try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.totalBytes to stat.availableBytes
        } catch (_: Exception) {
            0L to 0L
        }
        val comics = try {
            LocalAuthDatabase(context).getDownloadedComicChapters()
        } catch (_: Exception) {
            emptyList()
        }
        val recordedComicBytes = comics.sumOf { it.totalBytes }
        val offlineComicsDisk = computeDirectoryBytes(File(context.filesDir, "offline_comics"))
        val offlineEpisodesDisk = computeDirectoryBytes(File(context.filesDir, "offline_episodes"))
        val totalDownloadBytes = maxOf(recordedComicBytes, offlineComicsDisk) + offlineEpisodesDisk
        val cacheBytes = computeDirectoryBytes(context.cacheDir) +
            computeDirectoryBytes(context.externalCacheDir)
        return AppStorageStats(
            totalDeviceBytes = totalDevice,
            availableDeviceBytes = availableDevice,
            downloadBytes = totalDownloadBytes,
            downloadItemCount = comics.size,
            cacheBytes = cacheBytes
        )
    }

    fun formatByteSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes < kb -> "$bytes B"
            bytes < mb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            bytes < gb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            else -> {
                val gbVal = bytes / gb
                if (gbVal >= 100.0 && kotlin.math.abs(gbVal - gbVal.toLong()) < 0.05) {
                    "${gbVal.toLong()} GB"
                } else {
                    String.format(Locale.US, "%.1f GB", gbVal)
                }
            }
        }
    }

    private fun deleteDirectoryContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { child ->
            try {
                if (child.isDirectory) {
                    child.deleteRecursively()
                } else {
                    child.delete()
                }
            } catch (_: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    fun clearImageCache(context: Context) {
        try {
            coil.Coil.imageLoader(context).diskCache?.clear()
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (_: Exception) {}
        deleteDirectoryContents(context.cacheDir)
        deleteDirectoryContents(context.externalCacheDir)
    }

    // ==================== ONBOARDING COACHMARK ====================

    fun isHomeOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_HOME, false)
    }

    fun setHomeOnboardingCompleted(context: Context, completed: Boolean = true) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_HOME, completed)
            .apply()
    }

    fun isDetailOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_DETAIL, false)
    }

    fun setDetailOnboardingCompleted(context: Context, completed: Boolean = true) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_DETAIL, completed)
            .apply()
    }

    fun isSearchOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SEARCH, false)
    }

    fun setSearchOnboardingCompleted(context: Context, completed: Boolean = true) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_SEARCH, completed)
            .apply()
    }

    fun isStreamingOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_STREAMING, false)
    }

    fun setStreamingOnboardingCompleted(context: Context, completed: Boolean = true) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_STREAMING, completed)
            .apply()
    }

    // ==================== GOOGLE USER SESSION PERSISTENCE ====================

    fun saveGoogleSession(
        context: Context,
        id: String,
        name: String,
        email: String,
        photoUrl: String?
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GOOGLE_UID, id)
            .putString(KEY_GOOGLE_NAME, name)
            .putString(KEY_GOOGLE_EMAIL, email)
            .putString(KEY_GOOGLE_AVATAR, photoUrl.orEmpty())
            .apply()
    }

    fun getSavedGoogleSession(context: Context): SavedGoogleUserSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_GOOGLE_UID, null) ?: return null
        val name = prefs.getString(KEY_GOOGLE_NAME, "Pengguna Google") ?: "Pengguna Google"
        val email = prefs.getString(KEY_GOOGLE_EMAIL, "") ?: ""
        val avatar = prefs.getString(KEY_GOOGLE_AVATAR, null)?.ifBlank { null }
        return SavedGoogleUserSession(
            id = uid,
            name = name,
            email = email,
            photoUrl = avatar
        )
    }

    fun clearGoogleSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_GOOGLE_UID)
            .remove(KEY_GOOGLE_NAME)
            .remove(KEY_GOOGLE_EMAIL)
            .remove(KEY_GOOGLE_AVATAR)
            .apply()
    }

    // ==================== PROMO POPUP PREFERENCES ====================

    fun shouldShowPromoPopup(context: Context, config: com.yuranime.dinz.data.firebase.PromoPopupConfig): Boolean {
        if (!config.isActive || config.imageUrl.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val promoKey = config.promoId.ifBlank { "default_promo" }
        val lastShown = prefs.getLong("promo_last_shown_$promoKey", 0L)
        val count = prefs.getInt("promo_count_$promoKey", 0)

        if (config.maxImpressions > 0 && count >= config.maxImpressions) {
            return false
        }

        val intervalMs = (config.intervalHours * 3600 * 1000L).toLong()
        val now = System.currentTimeMillis()
        return (now - lastShown) >= intervalMs
    }

    fun recordPromoPopupShown(context: Context, promoId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val promoKey = promoId.ifBlank { "default_promo" }
        val count = prefs.getInt("promo_count_$promoKey", 0)
        prefs.edit()
            .putLong("promo_last_shown_$promoKey", System.currentTimeMillis())
            .putInt("promo_count_$promoKey", count + 1)
            .apply()
    }
}

