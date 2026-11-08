package com.yuranime.dinz.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _sliderMode = MutableStateFlow(HomeSliderMode.DEFAULT)
    val sliderMode: StateFlow<HomeSliderMode> = _sliderMode.asStateFlow()

    private val _autoNext = MutableStateFlow(true)
    val autoNext: StateFlow<Boolean> = _autoNext.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeId = prefs.getInt(KEY_SLIDER_MODE, HomeSliderMode.DEFAULT.id)
        _sliderMode.value = if (modeId == HomeSliderMode.CLEAN_COVER.id) HomeSliderMode.CLEAN_COVER else HomeSliderMode.DEFAULT
        _autoNext.value = prefs.getBoolean(KEY_AUTO_NEXT, true)
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

    fun clearImageCache(context: Context) {
        try {
            coil.Coil.imageLoader(context).diskCache?.clear()
            coil.Coil.imageLoader(context).memoryCache?.clear()
        } catch (ignored: Exception) {}
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
}

