package com.yuranime.dinz.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HomeSliderMode(val id: Int, val title: String, val description: String) {
    DEFAULT(0, "Slider dengan Poster (Default)", "Menampilkan kartu poster vertikal di pojok kiri bawah"),
    CLEAN_COVER(1, "Slider Tanpa Poster (Clean Cover)", "Menampilkan cover anime penuh sinematik dan ikon pencarian")
}

object AppPreferences {
    private const val PREFS_NAME = "yuranime_app_prefs"
    private const val KEY_SLIDER_MODE = "key_home_slider_mode"
    private const val KEY_AUTO_NEXT = "key_auto_next_episode"

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
}
