package com.yuranime.dinz.rpg.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

enum class RpgSfx(val filePath: String) {
    STEP("rpg/audio/sfx/sfx_step.wav"),
    CAST("rpg/audio/sfx/sfx_cast.wav"),
    BITE("rpg/audio/sfx/sfx_bite.wav"),
    CATCH("rpg/audio/sfx/sfx_catch.wav"),
    AXE("rpg/audio/sfx/sfx_axe.wav"),
    MINE("rpg/audio/sfx/sfx_mine.wav"),
    TREE_FALL("rpg/audio/sfx/sfx_tree_fall.wav"),
    WOOD_PICKUP("rpg/audio/sfx/sfx_pickup.wav"),
    PLANT("rpg/audio/sfx/sfx_plant.wav"),
    WATER("rpg/audio/sfx/sfx_water.wav"),
    HARVEST("rpg/audio/sfx/sfx_harvest.wav"),
    BUILD("rpg/audio/sfx/sfx_levelup.wav"),
    UI("rpg/audio/sfx/sfx_ui.wav"),
    CANCEL("rpg/audio/sfx/sfx_cancel.wav"),
    COIN("rpg/audio/sfx/sfx_coin.wav"),
    TALK("rpg/audio/sfx/sfx_talk.wav"),
    DOOR("rpg/audio/sfx/sfx_door.wav")
}

class RpgAudioManager {
    companion object {
        val BGM_TRACKS = mapOf(
            "village" to "rpg/audio/music/bgm_village.ogg",
            "homestead" to "rpg/audio/music/bgm_homestead.ogg",
            "water" to "rpg/audio/music/bgm_water.ogg",
            "cave" to "rpg/audio/music/bgm_cave.ogg",
            "ruins" to "rpg/audio/music/bgm_ruins.ogg"
        )
    }

    private val sounds = mutableMapOf<RpgSfx, Sound>()
    private var bgm: Music? = null
    private var currentTrackKey: String = ""

    @Volatile
    var sfxVolume: Float = 0.8f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    @Volatile
    var bgmVolume: Float = 0.45f
        set(value) {
            field = value.coerceIn(0f, 1f)
            runCatching {
                bgm?.volume = field
                if (field <= 0.005f) {
                    if (bgm?.isPlaying == true) bgm?.pause()
                } else {
                    if (bgm != null && bgm?.isPlaying == false) bgm?.play()
                }
            }
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
        switchBgm("homestead")
    }

    fun switchBgm(trackKey: String) {
        if (trackKey == currentTrackKey && bgm != null) return
        val path = BGM_TRACKS[trackKey] ?: BGM_TRACKS["homestead"] ?: return
        runCatching {
            bgm?.stop()
            bgm?.dispose()
            bgm = null
            val fh = Gdx.files.internal(path)
            if (fh.exists()) {
                currentTrackKey = trackKey
                bgm = Gdx.audio.newMusic(fh).apply {
                    isLooping = true
                    volume = bgmVolume
                    if (bgmVolume > 0.005f) play()
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
        currentTrackKey = ""
    }
}
