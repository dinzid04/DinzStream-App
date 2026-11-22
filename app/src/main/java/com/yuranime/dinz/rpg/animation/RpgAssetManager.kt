package com.yuranime.dinz.rpg.animation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType

class RpgAssetManager {
    private val ownedTextures = mutableListOf<Texture>()
    private var playerAtlas: TextureAtlas? = null

    private val playerAnimations = mutableMapOf<String, Animation<TextureRegion>>()
    lateinit var merchantAnim: Animation<TextureRegion>
        private set
    lateinit var waterAnim: Animation<TextureRegion>
        private set
    lateinit var rippleAnim: Animation<TextureRegion>
        private set
    lateinit var splashAnim: Animation<TextureRegion>
        private set
    lateinit var grassSwayAnim: Animation<TextureRegion>
        private set
    lateinit var flowerSwayAnim: Animation<TextureRegion>
        private set
    lateinit var bobberIdle: TextureRegion
        private set
    lateinit var bobberBite: TextureRegion
        private set

    val houseTextures = mutableMapOf<Int, Texture>()
    val treeStaticTextures = mutableMapOf<Pair<TreeType, TreeHealthState>, Texture>()
    val treeFallingAnims = mutableMapOf<TreeType, Animation<TextureRegion>>()
    val cropStageTextures = mutableMapOf<Pair<CropType, Int>, Texture>()
    val objectTextures = mutableMapOf<String, Texture>()
    val fishTextures = mutableMapOf<String, Texture>()
    val particleTextures = mutableMapOf<String, Texture>()
    lateinit var whitePixel: Texture
        private set
    lateinit var tilledSoilTex: TextureRegion
        private set
    lateinit var wateredSoilTex: TextureRegion
        private set

    private fun loadNearest(path: String): Texture {
        val tex = Texture(Gdx.files.internal(path)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        ownedTextures.add(tex)
        return tex
    }

    private fun sliceStrip(path: String, frameW: Int, frameH: Int, frameDuration: Float, mode: Animation.PlayMode = Animation.PlayMode.LOOP): Animation<TextureRegion> {
        val tex = loadNearest(path)
        val cols = (tex.width / frameW).coerceAtLeast(1)
        val regions = com.badlogic.gdx.utils.Array<TextureRegion>(cols)
        for (i in 0 until cols) {
            regions.add(TextureRegion(tex, i * frameW, 0, frameW, frameH))
        }
        return Animation(frameDuration, regions, mode)
    }

    fun load() {
        // 1x1 white pixel for primitives (shadows, fishing line, glows, day/night tint, placement highlight)
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(1f, 1f, 1f, 1f)
            fill()
        }
        whitePixel = Texture(pm).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        pm.dispose()
        ownedTextures.add(whitePixel)

        // Tileset regions for tilled/watered dynamic farm soil
        val tilesetTex = loadNearest("rpg/tilesets/homestead_tileset.png")
        tilledSoilTex = TextureRegion(tilesetTex, 9 * 16, 0, 16, 16)
        wateredSoilTex = TextureRegion(tilesetTex, 10 * 16, 0, 16, 16)

        // Player TextureAtlas (160 sliced frames across 11 actions x 4 directions!)
        val atlas = TextureAtlas(Gdx.files.internal("rpg/characters/player.atlas"))
        atlas.textures.forEach { it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) }
        playerAtlas = atlas

        val actionConfigs = listOf(
            PlayerAction.IDLE to Triple("idle", 0.24f, Animation.PlayMode.LOOP),
            PlayerAction.WALK to Triple("walk", 0.14f, Animation.PlayMode.LOOP),
            PlayerAction.RUN to Triple("run", 0.09f, Animation.PlayMode.LOOP),
            PlayerAction.FISH_CAST to Triple("fish_cast", 0.10f, Animation.PlayMode.NORMAL),
            PlayerAction.FISH_WAIT to Triple("fish_wait", 0.45f, Animation.PlayMode.LOOP),
            PlayerAction.FISH_REEL to Triple("fish_reel", 0.10f, Animation.PlayMode.LOOP),
            PlayerAction.AXE to Triple("axe", 0.085f, Animation.PlayMode.NORMAL),
            PlayerAction.HOE to Triple("hoe", 0.09f, Animation.PlayMode.NORMAL),
            PlayerAction.WATER to Triple("water", 0.11f, Animation.PlayMode.NORMAL),
            PlayerAction.HARVEST to Triple("harvest", 0.11f, Animation.PlayMode.NORMAL),
            PlayerAction.INTERACT to Triple("interact", 0.11f, Animation.PlayMode.NORMAL)
        )
        for ((act, triple) in actionConfigs) {
            val (prefix, dur, mode) = triple
            for (dir in Direction.entries) {
                val regionName = "${prefix}_${dir.key}"
                val found = atlas.findRegions(regionName)
                if (found != null && found.size > 0) {
                    val regions = com.badlogic.gdx.utils.Array<TextureRegion>(found.size)
                    for (i in 0 until found.size) {
                        regions.add(found.get(i))
                    }
                    playerAnimations[animKey(act, dir)] = Animation(dur, regions, mode)
                }
            }
        }

        // Merchant NPC & Environment Animations
        merchantAnim = sliceStrip("rpg/characters/merchant_npc.png", 32, 32, 0.28f)
        waterAnim = sliceStrip("rpg/animations/water_anim.png", 16, 16, 0.36f)
        rippleAnim = sliceStrip("rpg/animations/water_ripple.png", 16, 16, 0.18f)
        splashAnim = sliceStrip("rpg/animations/fishing_splash.png", 24, 24, 0.10f)
        grassSwayAnim = sliceStrip("rpg/animations/grass_sway.png", 16, 16, 0.34f)
        flowerSwayAnim = sliceStrip("rpg/animations/flower_sway.png", 16, 16, 0.42f)

        val bobTex = loadNearest("rpg/animations/bobber.png")
        bobberIdle = TextureRegion(bobTex, 0, 0, 16, 16)
        bobberBite = TextureRegion(bobTex, 16, 0, 16, 16)

        // Houses (3 Tiers)
        for (tier in 1..3) {
            houseTextures[tier] = loadNearest("rpg/objects/house_tier$tier.png")
        }

        // Trees (3 species x states + falling strip)
        for (sp in TreeType.entries) {
            treeStaticTextures[sp to TreeHealthState.HEALTHY] = loadNearest("rpg/objects/tree_${sp.id}_healthy.png")
            treeStaticTextures[sp to TreeHealthState.DAMAGED] = loadNearest("rpg/objects/tree_${sp.id}_damaged.png")
            treeStaticTextures[sp to TreeHealthState.HEAVILY_DAMAGED] = loadNearest("rpg/objects/tree_${sp.id}_heavily_damaged.png")
            treeStaticTextures[sp to TreeHealthState.STUMP] = loadNearest("rpg/objects/tree_${sp.id}_stump.png")
            treeFallingAnims[sp] = sliceStrip("rpg/objects/tree_${sp.id}_falling.png", 32, 48, 0.14f, Animation.PlayMode.NORMAL)
        }

        // Crops (4 crops x 5 stages)
        for (crop in CropType.entries) {
            for (st in 0..4) {
                cropStageTextures[crop to st] = loadNearest("rpg/objects/crop_${crop.id}_stage$st.png")
            }
        }

        // Placeable Furniture & Decorations
        for (item in RpgCatalog.buildableItems) {
            objectTextures[item.id] = loadNearest(item.spritePath)
        }

        // Fish sprites
        for (fish in RpgCatalog.fishSpecies) {
            fishTextures[fish.id] = loadNearest(fish.spritePath)
        }

        // Particles
        for (pName in listOf("wood_chip", "leaf_particle", "water_drop", "sparkle_harvest", "rain_drop")) {
            particleTextures[pName] = loadNearest("rpg/animations/$pName.png")
        }
    }

    private fun animKey(action: PlayerAction, direction: Direction): String = "${action.name}_${direction.name}"

    fun getPlayerFrame(action: PlayerAction, direction: Direction, stateTime: Float): TextureRegion {
        val anim = playerAnimations[animKey(action, direction)]
            ?: playerAnimations[animKey(PlayerAction.IDLE, direction)]
            ?: playerAnimations.values.first()
        return anim.getKeyFrame(stateTime)
    }

    fun dispose() {
        ownedTextures.forEach { runCatching { it.dispose() } }
        ownedTextures.clear()
        runCatching { playerAtlas?.dispose() }
        playerAtlas = null
    }
}
