package com.yuranime.dinz.rpg.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rpgSettingsDataStore by preferencesDataStore(name = "willowbrook_rpg_settings")

data class RpgAudioVideoSettings(
    val sfxVolume: Float = 0.8f,
    val bgmVolume: Float = 0.45f,
    val cameraZoom: Float = 1.0f,
    val weatherEnabled: Boolean = true
)

class RpgSettingsStore(private val context: Context) {
    private val keySfx = floatPreferencesKey("sfx_volume")
    private val keyBgm = floatPreferencesKey("bgm_volume")
    private val keyZoom = floatPreferencesKey("camera_zoom")
    private val keyWeather = booleanPreferencesKey("weather_enabled")

    val settingsFlow: Flow<RpgAudioVideoSettings> = context.rpgSettingsDataStore.data.map { prefs ->
        RpgAudioVideoSettings(
            sfxVolume = (prefs[keySfx] ?: 0.8f).coerceIn(0f, 1f),
            bgmVolume = (prefs[keyBgm] ?: 0.45f).coerceIn(0f, 1f),
            cameraZoom = (prefs[keyZoom] ?: 1.0f).coerceIn(0.75f, 1.45f),
            weatherEnabled = prefs[keyWeather] ?: true
        )
    }

    suspend fun updateSfxVolume(volume: Float) {
        context.rpgSettingsDataStore.edit { it[keySfx] = volume.coerceIn(0f, 1f) }
    }

    suspend fun updateBgmVolume(volume: Float) {
        context.rpgSettingsDataStore.edit { it[keyBgm] = volume.coerceIn(0f, 1f) }
    }

    suspend fun updateCameraZoom(zoom: Float) {
        context.rpgSettingsDataStore.edit { it[keyZoom] = zoom.coerceIn(0.75f, 1.45f) }
    }

    suspend fun updateWeatherEnabled(enabled: Boolean) {
        context.rpgSettingsDataStore.edit { it[keyWeather] = enabled }
    }
}
