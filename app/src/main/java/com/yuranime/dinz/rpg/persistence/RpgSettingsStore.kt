package com.yuranime.dinz.rpg.persistence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yuranime.dinz.rpg.model.ControlMode
import com.yuranime.dinz.rpg.model.ControlSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rpgSettingsDataStore by preferencesDataStore(name = "sakura_vale_rpg_settings")

data class RpgAudioVideoSettings(
    val sfxVolume: Float = 0.8f,
    val bgmVolume: Float = 0.45f,
    val cameraZoom: Float = 1.0f,
    val weatherEnabled: Boolean = true,
    val raylightEnabled: Boolean = true,
    val controls: ControlSettings = ControlSettings()
)

class RpgSettingsStore(private val context: Context) {
    private val keySfx = floatPreferencesKey("sfx_volume")
    private val keyBgm = floatPreferencesKey("bgm_volume")
    private val keyZoom = floatPreferencesKey("camera_zoom")
    private val keyWeather = booleanPreferencesKey("weather_enabled")
    private val keyRaylight = booleanPreferencesKey("raylight_enabled")

    // Pro Mobile Controls Customization Keys
    private val keyControlMode = stringPreferencesKey("control_mode")
    private val keyFloatingJoy = booleanPreferencesKey("floating_joystick")
    private val keyJoyRight = booleanPreferencesKey("joystick_on_right")
    private val keyJoyScale = floatPreferencesKey("joystick_scale")
    private val keyBtnScale = floatPreferencesKey("button_scale")
    private val keyOpacity = floatPreferencesKey("control_opacity")
    private val keySensitivity = floatPreferencesKey("control_sensitivity")
    private val keyUiScale = floatPreferencesKey("ui_scale")

    val settingsFlow: Flow<RpgAudioVideoSettings> = context.rpgSettingsDataStore.data.map { prefs ->
        val modeId = prefs[keyControlMode] ?: ControlMode.HYBRID.id
        val mode = ControlMode.entries.find { it.id == modeId } ?: ControlMode.HYBRID
        RpgAudioVideoSettings(
            sfxVolume = (prefs[keySfx] ?: 0.8f).coerceIn(0f, 1f),
            bgmVolume = (prefs[keyBgm] ?: 0.45f).coerceIn(0f, 1f),
            cameraZoom = (prefs[keyZoom] ?: 1.0f).coerceIn(0.75f, 1.45f),
            weatherEnabled = prefs[keyWeather] ?: true,
            raylightEnabled = prefs[keyRaylight] ?: true,
            controls = ControlSettings(
                controlMode = mode,
                floatingJoystick = prefs[keyFloatingJoy] ?: false,
                joystickOnRight = prefs[keyJoyRight] ?: false,
                joystickScale = (prefs[keyJoyScale] ?: 1.0f).coerceIn(0.75f, 1.35f),
                buttonScale = (prefs[keyBtnScale] ?: 1.0f).coerceIn(0.80f, 1.30f),
                controlOpacity = (prefs[keyOpacity] ?: 0.88f).coerceIn(0.45f, 1.0f),
                controlSensitivity = (prefs[keySensitivity] ?: 1.0f).coerceIn(0.70f, 1.40f),
                uiScale = (prefs[keyUiScale] ?: 1.0f).coerceIn(0.85f, 1.15f)
            )
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

    suspend fun updateRaylightEnabled(enabled: Boolean) {
        context.rpgSettingsDataStore.edit { it[keyRaylight] = enabled }
    }

    suspend fun updateControls(controls: ControlSettings) {
        context.rpgSettingsDataStore.edit {
            it[keyControlMode] = controls.controlMode.id
            it[keyFloatingJoy] = controls.floatingJoystick
            it[keyJoyRight] = controls.joystickOnRight
            it[keyJoyScale] = controls.joystickScale.coerceIn(0.75f, 1.35f)
            it[keyBtnScale] = controls.buttonScale.coerceIn(0.80f, 1.30f)
            it[keyOpacity] = controls.controlOpacity.coerceIn(0.45f, 1.0f)
            it[keySensitivity] = controls.controlSensitivity.coerceIn(0.70f, 1.40f)
            it[keyUiScale] = controls.uiScale.coerceIn(0.85f, 1.15f)
        }
    }

    suspend fun resetControlsToDefault() {
        updateControls(ControlSettings())
    }
}
