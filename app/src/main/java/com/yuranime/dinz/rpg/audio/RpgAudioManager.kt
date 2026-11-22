package com.yuranime.dinz.rpg.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

enum class RpgSfx(val filePath: String) {
    STEP("rpg/audio/sfx_step.wav"),
    CAST("rpg/audio/sfx_cast.wav"),
    BITE("rpg/audio/sfx_bite.wav"),
    CATCH("rpg/audio/sfx_catch.wav"),
    AXE("rpg/audio/sfx_axe.wav"),
    TREE_FALL("rpg/audio/sfx_tree_fall.wav"),
    WOOD_PICKUP("rpg/audio/sfx_wood_pickup.wav"),
    PLANT("rpg/audio/sfx_plant.wav"),
    WATER("rpg/audio/sfx_water.wav"),
    HARVEST("rpg/audio/sfx_harvest.wav"),
    BUILD("rpg/audio/sfx_build.wav"),
    UI("rpg/audio/sfx_ui.wav"),
    COIN("rpg/audio/sfx_coin.wav")
}

class RpgAudioManager {
    private val sounds = mutableMapOf<RpgSfx, Sound>()
    private var bgm: Music? = null

    @Volatile
    var sfxVolume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    @Volatile
    var bgmVolume: Float = 0.45f
        set(value) {
            field = value.coerceIn(0f, 1f)
            try {
                bgm?.volume = field
                if (field <= 0.001f) {
                    if (bgm?.isPlaying == true) bgm?.pause()
                } else {
                    if (bgm != null && bgm?.isPlaying == false) bgm?.play()
                }
            } catch (_: Exception) {}
        }

    fun load() {
        RpgSfx.entries.forEach { sfx ->
            runCatching {
                val fh = Gdx.files.internal(sfx.filePath)
                if (fh.exists()) {
                    sounds[sfx] = Gdx.audio.newSound(fh)
                }
            }
        }
        runCatching {
            val bgmHandle = Gdx.files.internal("rpg/audio/bgm_homestead.wav")
            if (bgmHandle.exists()) {
                bgm = Gdx.audio.newMusic(bgmHandle).apply {
                    isLooping = true
                    volume = bgmVolume
                    if (bgmVolume > 0.001f) play()
                }
            }
        }
    }

    fun play(sfx: RpgSfx, pitch: Float = 1.0f) {
        if (sfxVolume <= 0.01f) return
        val s = sounds[sfx] ?: return
        runCatching {
            s.play(sfxVolume, pitch, 0f)
        }
    }

    fun pauseBgm() {
        runCatching { bgm?.pause() }
    }

    fun resumeBgm() {
        runCatching {
            if (bgmVolume > 0.01f && bgm?.isPlaying == false) {
                bgm?.volume = bgmVolume
                bgm?.play()
            }
        }
    }

    fun dispose() {
        sounds.values.forEach { runCatching { it.dispose() } }
        sounds.clear()
        runCatching { bgm?.stop(); bgm?.dispose() }
        bgm = null
    }
}
