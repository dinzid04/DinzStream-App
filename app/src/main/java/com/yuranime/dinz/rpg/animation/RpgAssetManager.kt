package com.yuranime.dinz.rpg.animation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.OreType
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeType

class RpgAssetManager {
    private val ownedTextures = mutableListOf<Texture>()

    // 8 Selectable Anime Protagonist Sheets: presetId -> (actionKey -> Animation)
    private val playerSheets = mutableMapOf<String, Map<String, Animation<TextureRegion>>>()
    // 8 Village NPC Sheets: npcId -> (actionKey -> Animation)
    private val npcSheets = mutableMapOf<String, Map<String, Animation<TextureRegion>>>()
    // 5 Living Village Animals: animalId -> 2-frame Animation
    val animalAnims = mutableMapOf<String, Animation<TextureRegion>>()

    // Animated Environment & FX
    lateinit var waterRippleAnim: Animation<TextureRegion>
        private set
    lateinit var waterfallTopAnim: Animation<TextureRegion>
        private set
    lateinit var waterfallMidAnim: Animation<TextureRegion>
        private set
    lateinit var waterfallBotAnim: Animation<TextureRegion>
        private set
    lateinit var windmillAnim: Animation<TextureRegion>
        private set
    lateinit var watermillAnim: Animation<TextureRegion>
        private set
    lateinit var flagRedAnim: Animation<TextureRegion>
        private set
    lateinit var swayPlantAnim: Animation<TextureRegion>
        private set
    lateinit var smokeAnim: Animation<TextureRegion>
        private set
    lateinit var levelupAuraAnim: Animation<TextureRegion>
        private set
    lateinit var raylightTex: Texture
        private set
    lateinit var shadowTex: Texture
        private set
    lateinit var bobberIdle: TextureRegion
        private set
    lateinit var bobberBite: TextureRegion
        private set
    lateinit var tilledSoilTex: Texture
        private set
    lateinit var wateredSoilTex: Texture
        private set
    lateinit var whitePixel: Texture
        private set
    lateinit var chestClosedTex: Texture
        private set

    // Held tools in hand during swings/actions
    val inHandTools = mutableMapOf<String, Texture>()
    // Emote icons above NPCs / player
    val emoteTextures = mutableMapOf<String, Texture>()

    // World Objects
    val houseTextures = mutableMapOf<Int, Texture>()
    val treeTextures = mutableMapOf<TreeType, Texture>()
    lateinit var treeStumpTex: Texture
        private set
    val oreRockTextures = mutableMapOf<OreType, Texture>()
    val cropStageTextures = mutableMapOf<Pair<CropType, Int>, Texture>()
    val objectTextures = mutableMapOf<String, Texture>()
    val forageTextures = mutableMapOf<String, Texture>()
    val fishTextures = mutableMapOf<String, Texture>()
    val particleRegions = mutableMapOf<String, TextureRegion>()

    private fun loadNearest(path: String): Texture {
        val tex = Texture(Gdx.files.internal(path)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        ownedTextures.add(tex)
        return tex
    }

    private fun sliceHorizontal(
        path: String,
        frameW: Int,
        frameH: Int,
        frameDuration: Float,
        mode: Animation.PlayMode = Animation.PlayMode.LOOP
    ): Animation<TextureRegion> {
        val tex = loadNearest(path)
        val cols = (tex.width / frameW).coerceAtLeast(1)
        val regions = com.badlogic.gdx.utils.Array<TextureRegion>(cols)
        for (i in 0 until cols) {
            regions.add(TextureRegion(tex, i * frameW, 0, frameW, frameH.coerceAtMost(tex.height)))
        }
        return Animation(frameDuration, regions, mode)
    }

    /**
     * Slices Ninja Adventure top-down character sheets (64x112 = 4 cols x 7 rows of 16x16,
     * or compact 64x32 = 4 cols x 2 rows like OldWoman).
     * Col 0 = DOWN, Col 1 = UP, Col 2 = LEFT, Col 3 = RIGHT.
     * Rows 0..3 = Walk cycle, Row 4 = Tool swing, Row 5 = Jump/Harvest, Row 6 = Special poses.
     */
    private fun sliceCharacterSheet(path: String): Map<String, Animation<TextureRegion>> {
        val tex = loadNearest(path)
        val fw = 16
        val fh = 16
        val rows = (tex.height / fh).coerceAtLeast(1)
        val cols = (tex.width / fw).coerceAtLeast(1)

        fun cell(col: Int, row: Int): TextureRegion {
            val c = col.coerceIn(0, cols - 1)
            val r = row.coerceIn(0, rows - 1)
            return TextureRegion(tex, c * fw, r * fh, fw, fh)
        }

        fun makeAnim(dur: Float, mode: Animation.PlayMode, vararg frames: TextureRegion): Animation<TextureRegion> {
            val arr = com.badlogic.gdx.utils.Array<TextureRegion>(frames.size)
            for (f in frames) arr.add(f)
            return Animation(dur, arr, mode)
        }

        val map = HashMap<String, Animation<TextureRegion>>()
        for (dir in Direction.entries) {
            val col = dir.sheetCol
            val idleFrame = cell(col, 0)
            val walkFrames = if (rows >= 4) {
                arrayOf(cell(col, 0), cell(col, 1), cell(col, 2), cell(col, 3))
            } else if (rows >= 2) {
                arrayOf(cell(col, 0), cell(col, 1))
            } else {
                arrayOf(idleFrame)
            }
            val swingRow = if (rows >= 5) 4 else (rows - 1)
            val harvestRow = if (rows >= 6) 5 else (rows - 1)
            val celebrateFrame = if (rows >= 7) cell(1, 6) else idleFrame
            val craftFrame = if (rows >= 7) cell(2, 6) else cell(col, swingRow)

            map[animKey(PlayerAction.IDLE, dir)] = makeAnim(0.45f, Animation.PlayMode.LOOP, idleFrame)
            map[animKey(PlayerAction.WALK, dir)] = makeAnim(0.14f, Animation.PlayMode.LOOP, *walkFrames)
            map[animKey(PlayerAction.RUN, dir)] = makeAnim(0.09f, Animation.PlayMode.LOOP, *walkFrames)
            map[animKey(PlayerAction.FISH_CAST, dir)] = makeAnim(0.11f, Animation.PlayMode.NORMAL, cell(col, swingRow), idleFrame)
            map[animKey(PlayerAction.FISH_WAIT, dir)] = makeAnim(0.50f, Animation.PlayMode.LOOP, idleFrame)
            map[animKey(PlayerAction.FISH_REEL, dir)] = makeAnim(0.12f, Animation.PlayMode.LOOP, cell(col, swingRow), cell(col, 1.coerceAtMost(rows - 1)))
            map[animKey(PlayerAction.AXE, dir)] = makeAnim(0.09f, Animation.PlayMode.NORMAL, cell(col, swingRow), cell(col, 1.coerceAtMost(rows - 1)), idleFrame)
            map[animKey(PlayerAction.PICKAXE, dir)] = makeAnim(0.09f, Animation.PlayMode.NORMAL, cell(col, swingRow), cell(col, 1.coerceAtMost(rows - 1)), idleFrame)
            map[animKey(PlayerAction.HOE, dir)] = makeAnim(0.10f, Animation.PlayMode.NORMAL, cell(col, swingRow), idleFrame)
            map[animKey(PlayerAction.WATER, dir)] = makeAnim(0.12f, Animation.PlayMode.NORMAL, cell(col, swingRow), idleFrame)
            map[animKey(PlayerAction.HARVEST, dir)] = makeAnim(0.11f, Animation.PlayMode.NORMAL, cell(col, harvestRow), idleFrame)
            map[animKey(PlayerAction.INTERACT, dir)] = makeAnim(0.12f, Animation.PlayMode.NORMAL, cell(col, swingRow), idleFrame)
            map[animKey(PlayerAction.CELEBRATE, dir)] = makeAnim(0.35f, Animation.PlayMode.LOOP, celebrateFrame)
            map[animKey(PlayerAction.CRAFT, dir)] = makeAnim(0.22f, Animation.PlayMode.LOOP, craftFrame, cell(col, swingRow))
        }
        return map
    }

    fun load() {
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(1f, 1f, 1f, 1f)
            fill()
        }
        whitePixel = Texture(pm).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        pm.dispose()
        ownedTextures.add(whitePixel)

        // 8 Protagonist Presets
        for (preset in RpgCatalog.protagonistPresets) {
            playerSheets[preset.id] = sliceCharacterSheet(preset.sheetPath)
        }

        // 8 Village NPCs
        for (npc in RpgCatalog.npcs) {
            npcSheets[npc.id] = sliceCharacterSheet(npc.sheetPath)
        }

        // 5 Living Village Animals (each 32x16 -> 2 frames of 16x16)
        for (animalId in listOf("cat", "dog", "chicken", "cow", "frog")) {
            val path = "rpg/characters/animals/${animalId}_sheet.png"
            if (Gdx.files.internal(path).exists()) {
                animalAnims[animalId] = sliceHorizontal(path, 16, 16, 0.28f)
            }
        }

        // Animated Environment Strips & FX
        waterRippleAnim = sliceHorizontal("rpg/fx/water_ripples.png", 16, 16, 0.24f)
        waterfallTopAnim = sliceHorizontal("rpg/fx/waterfall_top.png", 16, 16, 0.14f)
        waterfallMidAnim = sliceHorizontal("rpg/fx/waterfall_mid.png", 16, 16, 0.14f)
        waterfallBotAnim = sliceHorizontal("rpg/fx/waterfall_bot.png", 16, 16, 0.16f)
        windmillAnim = sliceHorizontal("rpg/fx/windmill_prop.png", 64, 64, 0.22f)
        watermillAnim = sliceHorizontal("rpg/fx/watermill.png", 34, 36, 0.22f)
        flagRedAnim = sliceHorizontal("rpg/fx/flag_red.png", 16, 16, 0.20f)
        swayPlantAnim = sliceHorizontal("rpg/fx/sway_plant.png", 16, 16, 0.34f)
        smokeAnim = sliceHorizontal("rpg/fx/smoke.png", 32, 32, 0.24f)
        levelupAuraAnim = sliceHorizontal("rpg/fx/levelup_aura.png", 16, 32, 0.10f)
        raylightTex = loadNearest("rpg/fx/raylight.png")
        shadowTex = loadNearest("rpg/fx/shadow.png")
        tilledSoilTex = loadNearest("rpg/fx/soil_tilled.png")
        wateredSoilTex = loadNearest("rpg/fx/soil_watered.png")
        chestClosedTex = loadNearest("rpg/ui/icon_chest.png")

        val bobTex = loadNearest("rpg/fx/bobber.png")
        bobberIdle = TextureRegion(bobTex, 0, 0, 16, 16)
        bobberBite = TextureRegion(bobTex, 16, 0, 16, 16)

        // Held Tool Sprites
        inHandTools["axe"] = loadNearest("rpg/items/tools/inhand_axe.png")
        inHandTools["pickaxe"] = loadNearest("rpg/items/tools/inhand_pickaxe.png")
        inHandTools["hoe"] = loadNearest("rpg/items/tools/inhand_hoe.png")
        inHandTools["hammer"] = loadNearest("rpg/items/tools/inhand_hammer.png")
        inHandTools["rod"] = loadNearest("rpg/items/tools/tool_rod_basic.png")
        inHandTools["water"] = loadNearest("rpg/items/tools/tool_watering_can.png")

        // Emote Bubbles
        for (e in listOf("exclaim", "question", "happy", "music", "sleep", "heart")) {
            emoteTextures[e] = loadNearest("rpg/ui/emotes/emote_$e.png")
        }

        // Upgradeable Player Houses (3 Tiers)
        for (tier in 1..3) {
            houseTextures[tier] = loadNearest("rpg/objects/houses/house_tier$tier.png")
        }

        // Interactive Trees & Stump
        for (t in TreeType.entries) {
            treeTextures[t] = loadNearest(t.spritePath)
        }
        treeStumpTex = loadNearest("rpg/objects/trees/tree_stump.png")

        // Mineable Ore Rocks
        for (ore in OreType.entries) {
            oreRockTextures[ore] = loadNearest(ore.rockSpritePath)
        }

        // Crops (6 crops x 5 growth stages)
        for (crop in CropType.entries) {
            for (st in 0..4) {
                cropStageTextures[crop to st] = loadNearest("rpg/items/crops/crop_${crop.id}_stage$st.png")
            }
        }

        // Placeable Furniture & Decorations
        for (item in RpgCatalog.buildableItems) {
            objectTextures[item.id] = loadNearest(item.spritePath)
        }

        // Wild Forageables
        for (fid in listOf("forage_berry", "forage_chestnut", "forage_tea_leaf")) {
            val def = RpgCatalog.itemById[fid]
            if (def != null) forageTextures[fid] = loadNearest(def.spritePath)
        }

        // Fish Sprites
        for (fish in RpgCatalog.fishSpecies) {
            fishTextures[fish.id] = loadNearest(fish.spritePath)
        }

        // Particle Regions (extract crisp first frame of each Ninja Adventure particle strip!)
        particleRegions["wood_chip"] = firstFrame("rpg/fx/particle_wood.png", 16, 16)
        particleRegions["rock_chip"] = firstFrame("rpg/fx/particle_rock.png", 16, 16)
        particleRegions["leaf_particle"] = firstFrame("rpg/fx/particle_leaf.png", 12, 7)
        particleRegions["sakura_petal"] = firstFrame("rpg/fx/particle_sakura.png", 12, 7)
        particleRegions["sparkle_harvest"] = firstFrame("rpg/fx/particle_spark.png", 10, 8)
        particleRegions["rain_drop"] = firstFrame("rpg/fx/particle_rain.png", 8, 8)
        particleRegions["water_drop"] = firstFrame("rpg/fx/particle_rain_splash.png", 8, 8)
    }

    private fun firstFrame(path: String, fw: Int, fh: Int): TextureRegion {
        val tex = loadNearest(path)
        return TextureRegion(tex, 0, 0, fw.coerceAtMost(tex.width), fh.coerceAtMost(tex.height))
    }

    private fun animKey(action: PlayerAction, direction: Direction): String = "${action.name}_${direction.name}"

    fun getPlayerFrame(presetId: String, action: PlayerAction, direction: Direction, stateTime: Float): TextureRegion {
        val sheet = playerSheets[presetId] ?: playerSheets.values.first()
        val anim = sheet[animKey(action, direction)]
            ?: sheet[animKey(PlayerAction.IDLE, direction)]
            ?: sheet.values.first()
        return anim.getKeyFrame(stateTime)
    }

    fun getNpcFrame(npcId: String, isWalking: Boolean, direction: Direction, stateTime: Float): TextureRegion {
        val sheet = npcSheets[npcId] ?: npcSheets.values.first()
        val act = if (isWalking) PlayerAction.WALK else PlayerAction.IDLE
        val anim = sheet[animKey(act, direction)]
            ?: sheet[animKey(PlayerAction.IDLE, direction)]
            ?: sheet.values.first()
        return anim.getKeyFrame(stateTime)
    }

    fun dispose() {
        ownedTextures.forEach { runCatching { it.dispose() } }
        ownedTextures.clear()
        playerSheets.clear()
        npcSheets.clear()
        animalAnims.clear()
    }
}
