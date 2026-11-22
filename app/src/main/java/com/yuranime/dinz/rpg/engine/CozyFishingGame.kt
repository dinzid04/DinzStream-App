package com.yuranime.dinz.rpg.engine

import android.content.Context
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.MathUtils
import com.yuranime.dinz.rpg.animation.RpgAssetManager
import com.yuranime.dinz.rpg.audio.RpgAudioManager
import com.yuranime.dinz.rpg.audio.RpgSfx
import com.yuranime.dinz.rpg.building.BuildingSystem
import com.yuranime.dinz.rpg.economy.EconomySystem
import com.yuranime.dinz.rpg.farming.FarmingSystem
import com.yuranime.dinz.rpg.fishing.FishingSystem
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.persistence.PlayerStateEntity
import com.yuranime.dinz.rpg.persistence.RpgSaveBundle
import com.yuranime.dinz.rpg.persistence.RpgSaveRepository
import com.yuranime.dinz.rpg.player.PlayerController
import com.yuranime.dinz.rpg.quests.ProgressionSystem
import com.yuranime.dinz.rpg.world.HomesteadWorld
import com.yuranime.dinz.rpg.world.ParticleSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class RpgModalRequest {
    NONE, MERCHANT, SELL_BIN, STORAGE, HOUSE_UPGRADE
}

class CozyFishingGame(
    private val appContext: Context,
    private val onOpenModalRequest: (RpgModalRequest) -> Unit
) : ApplicationAdapter() {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveRepo = RpgSaveRepository(appContext)

    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var mapRenderer: OrthogonalTiledMapRenderer

    private val assets = RpgAssetManager()
    private val audio = RpgAudioManager()
    private val world = HomesteadWorld()
    private val particles = ParticleSystem()
    private val inventory = InventorySystem()
    private val economy = EconomySystem(inventory)
    private val progression = ProgressionSystem()
    private val player = PlayerController()
    private val fishing = FishingSystem()
    private val farming = FarmingSystem(world, inventory)
    private val building = BuildingSystem(world, inventory, economy)

    private val _hudState = MutableStateFlow(RpgHudSnapshot())
    val hudState: StateFlow<RpgHudSnapshot> = _hudState.asStateFlow()

    @Volatile
    var cameraZoom: Float = 1.0f
    @Volatile
    private var isPausedByMenu: Boolean = false
    @Volatile
    private var sortMode: InventorySortMode = InventorySortMode.CATEGORY

    private var globalAnimTime: Float = 0f
    private var uiSyncTimer: Float = 0f
    private var autoSaveTimer: Float = 0f
    private var toastMessage: String = ""
    private var toastCounter: Long = 0L
    private var isInitialized: Boolean = false

    // Ground & below-player layers vs foreground layer indices in starter_homestead.tmx
    private val baseLayerIndices = intArrayOf(0, 1, 2, 3, 4)
    private val foregroundLayerIndices = intArrayOf(7)

    override fun create() {
        camera = OrthographicCamera()
        batch = SpriteBatch()

        assets.load()
        audio.load()
        world.loadMap()
        inventory.initStarterKit()
        mapRenderer = OrthogonalTiledMapRenderer(world.tiledMap, 1f)

        // Load saved Room state asynchronously and apply safely on GL thread
        ioScope.launch {
            val saved = runCatching { saveRepo.loadGame() }.getOrNull()
            Gdx.app.postRunnable {
                if (saved != null) {
                    applySaveBundle(saved)
                    showToast("Selamat datang kembali di Willowbrook! (Hari ${world.dayCount})")
                } else {
                    showToast("Selamat datang di Willowbrook Cove! Dekati kolam untuk memancing.")
                }
                isInitialized = true
                pushHudSnapshot()
            }
        }
        isInitialized = true
        pushHudSnapshot()
    }

    private fun applySaveBundle(bundle: RpgSaveBundle) {
        val p = bundle.player
        player.x = p.posX.coerceIn(24f, HomesteadWorld.WORLD_W - 24f)
        player.y = p.posY.coerceIn(24f, HomesteadWorld.WORLD_H - 24f)
        player.direction = Direction.entries.find { it.key == p.facing } ?: Direction.DOWN
        player.selectedToolId = p.selectedToolId
        player.selectedSeedId = p.selectedSeedId
        player.customization = PlayerCustomization(p.shirtColorHex, p.hatColorHex, p.ribbonColorHex)

        economy.coins = p.coins.coerceAtLeast(0)
        progression.playerLevel = p.level.coerceAtLeast(1)
        progression.exp = p.exp.coerceAtLeast(0)
        progression.fishingLevel = p.fishingLevel.coerceAtLeast(1)
        progression.fishingExp = p.fishingExp.coerceAtLeast(0)
        progression.farmingLevel = p.farmingLevel.coerceAtLeast(1)
        progression.farmingExp = p.farmingExp.coerceAtLeast(0)
        progression.gatheringLevel = p.gatheringLevel.coerceAtLeast(1)
        progression.gatheringExp = p.gatheringExp.coerceAtLeast(0)

        inventory.loadFromEntities(bundle.inventory)
        progression.loadSaved(bundle.fishCollection, bundle.quests, bundle.achievements)
        world.loadSavedWorld(
            savedHouseTier = p.houseTier,
            savedTimeHours = p.gameTimeHours,
            savedDayCount = p.dayCount,
            treeEntities = bundle.trees,
            cropEntities = bundle.crops,
            placedEntities = bundle.placedObjects
        )
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val aspect = width.toFloat() / height.toFloat()
        // Crisp pixel-art viewport showing ~11 tiles tall (176 world units) -> lush & readable on phone!
        val viewH = 180f
        val viewW = viewH * aspect
        camera.setToOrtho(false, viewW, viewH)
        camera.position.set(player.x, player.y, 0f)
        camera.update()
    }

    override fun render() {
        val rawDt = Gdx.graphics.deltaTime.coerceIn(0f, 0.05f)
        val dt = if (isPausedByMenu) 0f else rawDt
        globalAnimTime += rawDt

        updateGame(dt)
        renderWorld()
    }

    private fun updateGame(dt: Float) {
        if (dt > 0f) {
            world.update(dt) { regrownTree ->
                particles.spawnLeaves(
                    regrownTree.tileX * HomesteadWorld.TILE_SIZE + 8f,
                    regrownTree.tileY * HomesteadWorld.TILE_SIZE + 16f,
                    count = 8,
                    isSakura = regrownTree.type == TreeType.SAKURA
                )
            }
            farming.update(dt)

            // Fishing update
            val fishRes = fishing.update(dt)
            if (fishRes.biteTriggered) {
                audio.play(RpgSfx.BITE)
                particles.spawnWaterDrops(fishing.bobberX, fishing.bobberY, 10)
            } else if (fishRes.splashTriggered) {
                particles.spawnWaterDrops(fishing.bobberX, fishing.bobberY, 6)
            }
            if (fishRes.escaped) {
                showToast(fishing.toUiState().lastResultMessage)
                player.clearAction()
            }

            // Update player animation while fishing
            when (fishing.phase) {
                FishingPhase.CASTING -> player.setFishingAnimation(PlayerAction.FISH_CAST)
                FishingPhase.WAITING, FishingPhase.BITE -> player.setFishingAnimation(PlayerAction.FISH_WAIT)
                FishingPhase.MINIGAME -> player.setFishingAnimation(PlayerAction.FISH_REEL)
                FishingPhase.CAUGHT -> player.setFishingAnimation(PlayerAction.HARVEST)
                else -> Unit
            }

            // Player movement & collision
            player.update(dt, world, fishing.isActive()) {
                audio.play(RpgSfx.STEP, 0.92f + Random.nextFloat() * 0.16f)
            }

            // Keep construction placement ghost right in front of the player
            if (building.isBuildMode) {
                val (fx, fy) = player.getFacingTile()
                building.updatePreviewTile(fx, fy)
            }

            // Ambient rain particles
            if (world.isRaining) {
                particles.spawnRain(camera.position.x, camera.position.y, camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom)
            }
            particles.update(dt)

            // Auto-save every 20 seconds
            autoSaveTimer += dt
            if (autoSaveTimer >= 20f) {
                autoSaveTimer = 0f
                saveGameAsync(silent = true)
            }
        }

        // Smooth camera follow clamped neatly to Starter Homestead boundaries
        camera.zoom = MathUtils.lerp(camera.zoom, cameraZoom.coerceIn(0.75f, 1.45f), 0.12f)
        val halfW = (camera.viewportWidth * camera.zoom) * 0.5f
        val halfH = (camera.viewportHeight * camera.zoom) * 0.5f
        val targetX = if (HomesteadWorld.WORLD_W > halfW * 2f) {
            player.x.coerceIn(halfW, HomesteadWorld.WORLD_W - halfW)
        } else {
            HomesteadWorld.WORLD_W * 0.5f
        }
        val targetY = if (HomesteadWorld.WORLD_H > halfH * 2f) {
            player.y.coerceIn(halfH, HomesteadWorld.WORLD_H - halfH)
        } else {
            HomesteadWorld.WORLD_H * 0.5f
        }
        camera.position.x = MathUtils.lerp(camera.position.x, targetX, 0.14f)
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, 0.14f)
        camera.update()

        // Sync HUD snapshot smoothly (~18 Hz during normal play, 60 Hz during fishing minigame!)
        uiSyncTimer += Gdx.graphics.deltaTime
        val syncInterval = if (fishing.phase == FishingPhase.MINIGAME || fishing.phase == FishingPhase.BITE) 0.016f else 0.06f
        if (uiSyncTimer >= syncInterval) {
            uiSyncTimer = 0f
            pushHudSnapshot()
        }
    }

    private fun renderWorld() {
        Gdx.gl.glClearColor(0.18f, 0.32f, 0.22f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // 1. Render base TMX layers (ground, decor, water, buildings, docks/bridges/fences)
        mapRenderer.setView(camera)
        mapRenderer.render(baseLayerIndices)

        batch.projectionMatrix = camera.combined
        batch.begin()

        // 2. Render Animated Water Shimmer over water tiles
        val waterFrame = assets.waterAnim.getKeyFrame(globalAnimTime)
        for ((wx, wy) in world.waterTiles) {
            batch.draw(waterFrame, wx * HomesteadWorld.TILE_SIZE, wy * HomesteadWorld.TILE_SIZE, 16f, 16f)
        }

        // 3. Render Ambient Swaying Grass & Flowers
        val grassFrame = assets.grassSwayAnim.getKeyFrame(globalAnimTime)
        for ((gx, gy) in world.swayGrassTiles) {
            batch.draw(grassFrame, gx * HomesteadWorld.TILE_SIZE, gy * HomesteadWorld.TILE_SIZE, 16f, 16f)
        }
        val flowerFrame = assets.flowerSwayAnim.getKeyFrame(globalAnimTime)
        for ((fx, fy) in world.flowerTiles) {
            batch.draw(flowerFrame, fx * HomesteadWorld.TILE_SIZE, fy * HomesteadWorld.TILE_SIZE, 16f, 16f)
        }

        // 4. Render Dynamic Tilled / Watered Soil Plots
        for (plot in world.cropPlots.values) {
            if (!plot.isTilled) continue
            val soilTex = if (plot.isWatered) assets.wateredSoilTex else assets.tilledSoilTex
            batch.draw(soilTex, plot.tileX * HomesteadWorld.TILE_SIZE, plot.tileY * HomesteadWorld.TILE_SIZE, 16f, 16f)
        }

        // 5. Render Subtle Contextual Target Tile Indicator on ground
        val ctx = player.detectContextAction(world, inventory)
        if (!building.isBuildMode && !fishing.isActive() && ctx.type != ContextActionType.NONE) {
            val pulse = 0.22f + 0.12f * sin(globalAnimTime * 4f)
            batch.setColor(1f, 0.92f, 0.45f, pulse)
            batch.draw(
                assets.whitePixel,
                ctx.targetTileX * HomesteadWorld.TILE_SIZE,
                ctx.targetTileY * HomesteadWorld.TILE_SIZE,
                16f, 16f
            )
            batch.setColor(1f, 1f, 1f, 1f)
        }

        // 6. Y-Sorted Dynamic Entities (House, Crops, Trees, Placed Furniture, Merchant NPC, Player!)
        val drawables = mutableListOf<Pair<Float, () -> Unit>>()

        // Upgradeable Starter House (80x80 at tile (7, 17))
        val houseY = HomesteadWorld.HOUSE_TILE_Y * HomesteadWorld.TILE_SIZE
        drawables.add(houseY to {
            val houseTex = assets.houseTextures[world.houseTier] ?: assets.houseTextures[1]
            if (houseTex != null) {
                batch.draw(houseTex, HomesteadWorld.HOUSE_TILE_X * HomesteadWorld.TILE_SIZE, houseY - 6f, 80f, 80f)
            }
        })

        // Merchant NPC beside stall
        val merchantY = HomesteadWorld.MERCHANT_TILE_Y * HomesteadWorld.TILE_SIZE - 2f
        drawables.add(merchantY to {
            val mFrame = assets.merchantAnim.getKeyFrame(globalAnimTime)
            batch.draw(mFrame, HomesteadWorld.MERCHANT_TILE_X * HomesteadWorld.TILE_SIZE, merchantY - 4f, 32f, 32f)
        })

        // Crops (with gentle breeze sway!)
        for (plot in world.cropPlots.values) {
            val crop = plot.cropType ?: continue
            val cy = plot.tileY * HomesteadWorld.TILE_SIZE
            drawables.add(cy to {
                val tex = assets.cropStageTextures[crop to plot.stage]
                if (tex != null) {
                    val swayX = if (plot.stage >= 2) sin(globalAnimTime * 2.5f + plot.tileX) * 0.6f else 0f
                    batch.draw(tex, plot.tileX * HomesteadWorld.TILE_SIZE + swayX, cy, 16f, 24f)
                }
            })
        }

        // Trees (with hit-shake, falling animation, and stumps!)
        for (tree in world.trees) {
            val ty = tree.tileY * HomesteadWorld.TILE_SIZE
            drawables.add(ty to {
                val shakeOffset = if (tree.shakeTimer > 0f) sin(tree.shakeTimer * 52f) * 2.4f else 0f
                val dx = tree.tileX * HomesteadWorld.TILE_SIZE - 8f + shakeOffset
                val dy = ty - 2f
                if (tree.state == TreeHealthState.FALLING) {
                    val fAnim = assets.treeFallingAnims[tree.type]
                    val frame = fAnim?.getKeyFrame(tree.fallTimer)
                    if (frame != null) batch.draw(frame, dx, dy, 32f, 48f)
                } else {
                    val tex = assets.treeStaticTextures[tree.type to tree.state]
                    if (tex != null) batch.draw(tex, dx, dy, 32f, 48f)
                }
            })
        }

        // Placed Furniture & Decorations
        for (obj in world.placedObjects) {
            val oy = obj.tileY * HomesteadWorld.TILE_SIZE
            drawables.add(oy to {
                val tex = assets.objectTextures[obj.itemId]
                if (tex != null) {
                    val ox = obj.tileX * HomesteadWorld.TILE_SIZE
                    batch.draw(
                        tex,
                        ox, oy,
                        8f, 8f,
                        16f, 16f,
                        1f, 1f,
                        obj.rotation.toFloat(),
                        0, 0, tex.width, tex.height,
                        false, false
                    )
                }
            })
        }

        // Player Character (with subtle customization tint modulation!)
        drawables.add(player.y to {
            val pFrame = assets.getPlayerFrame(player.action, player.direction, player.stateTime)
            // Subtle warm tint blend based on customization shirt choice
            val tintR = ((player.customization.shirtColorHex shr 16) and 0xFF) / 255f
            val tintG = ((player.customization.shirtColorHex shr 8) and 0xFF) / 255f
            val tintB = (player.customization.shirtColorHex and 0xFF) / 255f
            // Blend 82% true sprite + 18% custom tint so skin/hat stay natural while outfit shifts noticeably
            batch.setColor(0.82f + tintR * 0.18f, 0.82f + tintG * 0.18f, 0.82f + tintB * 0.18f, 1f)
            batch.draw(pFrame, player.x - 16f, player.y - 6f, 32f, 32f)
            batch.setColor(1f, 1f, 1f, 1f)
        })

        // Sort descending by Y (higher Y drawn behind lower Y in top-down RPGs)
        drawables.sortByDescending { it.first }
        for ((_, drawFn) in drawables) {
            drawFn()
        }

        // 7. Fishing Line, Animated Bobber, Water Ripples & Splash FX
        if (fishing.isActive()) {
            val rodTipX = player.x + when (player.direction) {
                Direction.RIGHT -> 12f
                Direction.LEFT -> -12f
                else -> 4f
            }
            val rodTipY = player.y + when (player.direction) {
                Direction.UP -> 18f
                Direction.DOWN -> 4f
                else -> 12f
            }
            // Draw crisp fishing line segments
            drawPixelLine(batch, rodTipX, rodTipY, fishing.bobberX, fishing.bobberY + 2f)

            // Animated ripple rings around bobber
            val ripFrame = assets.rippleAnim.getKeyFrame(globalAnimTime)
            batch.draw(ripFrame, fishing.bobberX - 8f, fishing.bobberY - 8f, 16f, 16f)

            // Bobber sprite (bobs gently or plunges on BITE!)
            val bobOffset = if (fishing.phase == FishingPhase.BITE) -2.5f else sin(globalAnimTime * 4f) * 1.1f
            val bobTex = if (fishing.phase == FishingPhase.BITE || fishing.phase == FishingPhase.MINIGAME) {
                assets.bobberBite
            } else {
                assets.bobberIdle
            }
            batch.draw(bobTex, fishing.bobberX - 8f, fishing.bobberY - 6f + bobOffset, 16f, 16f)

            // Fishing splash animation on cast/bite/reel
            if (fishing.splashTimer > 0f) {
                val spFrame = assets.splashAnim.getKeyFrame(0.55f - fishing.splashTimer)
                batch.draw(spFrame, fishing.bobberX - 12f, fishing.bobberY - 8f, 24f, 24f)
            }
        }

        // 8. Construction Mode Placement Preview (Green valid / Red blocked!)
        if (building.isBuildMode) {
            val px = building.previewTileX * HomesteadWorld.TILE_SIZE
            val py = building.previewTileY * HomesteadWorld.TILE_SIZE
            val valid = building.canPlaceAt(building.previewTileX, building.previewTileY)
            batch.setColor(if (valid) 0.25f else 0.95f, if (valid) 0.92f else 0.25f, 0.35f, 0.38f)
            batch.draw(assets.whitePixel, px, py, 16f, 16f)
            val objTex = building.selectedItemId?.let { assets.objectTextures[it] }
            if (objTex != null) {
                batch.setColor(if (valid) 0.85f else 1f, if (valid) 1f else 0.55f, if (valid) 0.85f else 0.55f, 0.78f)
                batch.draw(
                    objTex,
                    px, py,
                    8f, 8f,
                    16f, 16f,
                    1f, 1f,
                    building.rotation.toFloat(),
                    0, 0, objTex.width, objTex.height,
                    false, false
                )
            }
            batch.setColor(1f, 1f, 1f, 1f)
        }

        // 9. Particles (wood chips, leaves, water splashes, harvest sparkles, rain)
        particles.render(batch, assets)

        batch.end()

        // 10. Foreground TMX layer
        mapRenderer.render(foregroundLayerIndices)

        // 11. Day & Night Cozy Lighting Tint + Warm Lamp Glows at Dusk/Night
        renderDayNightOverlay()
    }

    private fun drawPixelLine(batch: SpriteBatch, x0: Float, y0: Float, x1: Float, y1: Float) {
        val steps = 18
        batch.setColor(0.92f, 0.98f, 1f, 0.85f)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val sag = sin(t * Math.PI).toFloat() * -4f
            val lx = x0 + (x1 - x0) * t
            val ly = y0 + (y1 - y0) * t + sag
            batch.draw(assets.whitePixel, lx, ly, 1f, 1f)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun renderDayNightOverlay() {
        val h = world.timeHours
        val tint: Color? = when {
            h in 5.5f..7.5f -> {
                val k = 1f - (h - 5.5f) / 2f
                Color(0.95f, 0.62f, 0.38f, 0.16f * k) // Golden dawn
            }
            h in 17.5f..20.0f -> {
                val k = (h - 17.5f) / 2.5f
                Color(0.85f - 0.72f * k, 0.42f - 0.28f * k, 0.25f + 0.18f * k, 0.18f + 0.28f * k) // Sunset -> twilight
            }
            h >= 20.0f || h < 5.5f -> {
                Color(0.08f, 0.12f, 0.28f, 0.44f) // Cozy starlit blue night
            }
            world.isRaining -> {
                Color(0.16f, 0.24f, 0.34f, 0.16f) // Gentle rainy tint
            }
            else -> null
        }

        if (tint != null) {
            batch.projectionMatrix = camera.combined
            batch.begin()
            batch.setColor(tint)
            val left = camera.position.x - camera.viewportWidth * camera.zoom * 0.6f
            val bottom = camera.position.y - camera.viewportHeight * camera.zoom * 0.6f
            batch.draw(
                assets.whitePixel,
                left,
                bottom,
                camera.viewportWidth * camera.zoom * 1.2f,
                camera.viewportHeight * camera.zoom * 1.2f
            )
            // Warm glowing pools around street lamps, campfires, lanterns & house windows at dusk/night!
            if (world.isNightOrDusk()) {
                val glowSpots = mutableListOf(
                    (6 * 16f + 8f) to (14 * 16f + 10f),
                    (18 * 16f + 8f) to (14 * 16f + 10f),
                    (HomesteadWorld.HOUSE_TILE_X * 16f + 24f) to (HomesteadWorld.HOUSE_TILE_Y * 16f + 22f),
                    (HomesteadWorld.HOUSE_TILE_X * 16f + 56f) to (HomesteadWorld.HOUSE_TILE_Y * 16f + 22f)
                )
                world.placedObjects.forEach { obj ->
                    if (obj.itemId in listOf("lamp_post", "campfire", "lantern_stone")) {
                        glowSpots.add((obj.tileX * 16f + 8f) to (obj.tileY * 16f + 8f))
                    }
                }
                batch.setColor(1f, 0.86f, 0.45f, 0.22f)
                for ((gx, gy) in glowSpots) {
                    batch.draw(assets.whitePixel, gx - 18f, gy - 18f, 36f, 36f)
                }
            }
            batch.setColor(1f, 1f, 1f, 1f)
            batch.end()
        }
    }

    // ==================== PUBLIC GAMEPLAY ACTIONS (CALLED FROM COMPOSE HUD THREAD) ====================

    fun setJoystick(x: Float, y: Float) {
        player.joyX = x.coerceIn(-1f, 1f)
        player.joyY = y.coerceIn(-1f, 1f)
    }

    fun toggleSprint() {
        Gdx.app.postRunnable {
            player.isRunning = !player.isRunning
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun setMenuPaused(paused: Boolean) {
        isPausedByMenu = paused
        if (paused) {
            player.joyX = 0f
            player.joyY = 0f
        }
    }

    fun selectSeed(seedId: String) {
        Gdx.app.postRunnable {
            player.selectedSeedId = seedId
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun selectTool(toolId: String) {
        Gdx.app.postRunnable {
            player.selectedToolId = toolId
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun setInventorySort(mode: InventorySortMode) {
        sortMode = mode
        Gdx.app.postRunnable {
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun onContextualActionPressed() {
        Gdx.app.postRunnable {
            if (building.isBuildMode) {
                confirmBuildPlacementInternal()
                return@postRunnable
            }
            if (fishing.isActive()) {
                handleFishingTapInternal()
                return@postRunnable
            }

            val ctx = player.detectContextAction(world, inventory)
            when (ctx.type) {
                ContextActionType.FISH -> {
                    val tx = ctx.targetTileX * HomesteadWorld.TILE_SIZE + 8f
                    val ty = ctx.targetTileY * HomesteadWorld.TILE_SIZE + 8f
                    // Face water tile cleanly
                    val dx = tx - player.x
                    val dy = ty - player.y
                    player.direction = if (abs(dx) > abs(dy)) {
                        if (dx > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0) Direction.UP else Direction.DOWN
                    }
                    fishing.startCasting(
                        targetWorldX = tx,
                        targetWorldY = ty,
                        habitat = ctx.habitat,
                        bestRodId = inventory.getBestRodId(),
                        fishingLvl = progression.fishingLevel,
                        nightTime = world.isNightOrDusk()
                    )
                    audio.play(RpgSfx.CAST)
                    showToast("Melempar kail di ${ctx.habitat.label}...")
                }

                ContextActionType.CHOP_TREE -> {
                    val tree = world.trees.find { it.id == ctx.targetId } ?: world.getTreeAt(ctx.targetTileX, ctx.targetTileY)
                    if (tree != null && tree.state != TreeHealthState.STUMP && tree.state != TreeHealthState.FALLING) {
                        player.triggerActionAnimation(PlayerAction.AXE, 0.34f)
                        audio.play(RpgSfx.AXE)
                        val dmg = inventory.getBestAxeDamage()
                        tree.health = (tree.health - dmg).coerceAtLeast(0)
                        tree.shakeTimer = 0.28f
                        val wx = tree.tileX * HomesteadWorld.TILE_SIZE + 8f
                        val wy = tree.tileY * HomesteadWorld.TILE_SIZE + 12f
                        particles.spawnWoodChips(wx, wy, 7)
                        particles.spawnLeaves(wx, wy + 14f, 6, tree.type == TreeType.SAKURA)

                        if (tree.health <= 0) {
                            tree.state = TreeHealthState.FALLING
                            tree.fallTimer = 0f
                            audio.play(RpgSfx.TREE_FALL)
                            val bonusWood = if (inventory.hasTool("tool_axe_iron")) 2 else 0
                            val gainedWood = tree.type.woodYield + bonusWood
                            inventory.addItem("res_wood", gainedWood)
                            val gotHardwood = Random.nextFloat() < tree.type.hardwoodChance
                            if (gotHardwood) inventory.addItem("res_hardwood", 1)
                            // Occasional stone drop from roots
                            if (Random.nextFloat() < 0.35f) inventory.addItem("res_stone", 1)
                            audio.play(RpgSfx.WOOD_PICKUP)
                            val notices = progression.recordTreeChopped(gainedWood, tree.type.expReward)
                            val extra = if (gotHardwood) " + 1 Kayu Keras" else ""
                            showToast(notices.firstOrNull() ?: "+$gainedWood Kayu$extra dari ${tree.type.displayName}!")
                            saveGameAsync(silent = true)
                        } else {
                            tree.state = if (tree.health <= 1) TreeHealthState.HEAVILY_DAMAGED else TreeHealthState.DAMAGED
                            showToast("${tree.type.displayName} (${tree.health}/${tree.type.maxHealth})")
                        }
                    }
                }

                ContextActionType.TILL_SOIL -> {
                    player.triggerActionAnimation(PlayerAction.HOE, 0.35f)
                    val res = farming.tillSoil(ctx.targetTileX, ctx.targetTileY)
                    if (res.success) {
                        audio.play(RpgSfx.PLANT)
                        particles.spawnWoodChips(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 8f, 5)
                    }
                    showToast(res.message)
                }

                ContextActionType.PLANT_SEED -> {
                    player.triggerActionAnimation(PlayerAction.HOE, 0.32f)
                    // Auto-pick an available crop seed if selected seed is empty or sapling
                    val seedToUse = if (player.selectedSeedId.startsWith("seed_") &&
                        player.selectedSeedId !in listOf("seed_oak", "seed_sakura") &&
                        inventory.hasItem(player.selectedSeedId, 1)
                    ) {
                        player.selectedSeedId
                    } else {
                        listOf("seed_carrot", "seed_potato", "seed_tomato", "seed_pumpkin")
                            .firstOrNull { inventory.hasItem(it, 1) }
                            ?: player.selectedSeedId
                    }
                    player.selectedSeedId = seedToUse
                    val res = farming.plantSeed(ctx.targetTileX, ctx.targetTileY, seedToUse)
                    if (res.success) {
                        audio.play(RpgSfx.PLANT)
                        val notices = progression.recordCropPlanted()
                        showToast(notices.firstOrNull() ?: res.message)
                    } else {
                        showToast(res.message)
                    }
                }

                ContextActionType.WATER_CROP -> {
                    player.triggerActionAnimation(PlayerAction.WATER, 0.42f)
                    val res = farming.waterPlot(ctx.targetTileX, ctx.targetTileY)
                    if (res.success) {
                        audio.play(RpgSfx.WATER)
                        particles.spawnWaterDrops(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 8f, 10)
                    }
                    showToast(res.message)
                }

                ContextActionType.HARVEST_CROP -> {
                    player.triggerActionAnimation(PlayerAction.HARVEST, 0.36f)
                    val res = farming.harvestCrop(ctx.targetTileX, ctx.targetTileY)
                    if (res.success && res.harvestedCrop != null) {
                        audio.play(RpgSfx.HARVEST)
                        particles.spawnHarvestSparkles(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 12f, 12)
                        val notices = progression.recordCropHarvested(res.harvestedCrop.expReward)
                        showToast(notices.firstOrNull() ?: res.message)
                        saveGameAsync(silent = true)
                    } else {
                        showToast(res.message)
                    }
                }

                ContextActionType.PLANT_SAPLING -> {
                    player.triggerActionAnimation(PlayerAction.HOE, 0.35f)
                    val treeType = if (player.selectedSeedId == "seed_sakura") TreeType.SAKURA else TreeType.OAK
                    if (inventory.hasItem(player.selectedSeedId, 1) && world.plantTreeSapling(ctx.targetTileX, ctx.targetTileY, treeType)) {
                        inventory.removeItem(player.selectedSeedId, 1)
                        audio.play(RpgSfx.PLANT)
                        particles.spawnLeaves(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 12f, 10, treeType == TreeType.SAKURA)
                        showToast("Berhasil menanam ${treeType.displayName} baru!")
                    } else {
                        showToast("Tidak bisa menanam pohon di sini.")
                    }
                }

                ContextActionType.INTERACT_DECOR -> {
                    player.triggerActionAnimation(PlayerAction.INTERACT, 0.28f)
                    val res = building.removeObjectAt(ctx.targetTileX, ctx.targetTileY)
                    audio.play(RpgSfx.UI)
                    showToast(res.message)
                }

                ContextActionType.OPEN_MERCHANT -> {
                    player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                    audio.play(RpgSfx.UI)
                    onOpenModalRequest(RpgModalRequest.MERCHANT)
                }

                ContextActionType.OPEN_SELL_BIN -> {
                    player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                    audio.play(RpgSfx.UI)
                    onOpenModalRequest(RpgModalRequest.SELL_BIN)
                }

                ContextActionType.OPEN_STORAGE -> {
                    player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                    audio.play(RpgSfx.UI)
                    onOpenModalRequest(RpgModalRequest.STORAGE)
                }

                ContextActionType.OPEN_HOUSE_UPGRADE -> {
                    player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                    audio.play(RpgSfx.UI)
                    onOpenModalRequest(RpgModalRequest.HOUSE_UPGRADE)
                }

                ContextActionType.NONE -> {
                    showToast("Dekati air, pohon, lahan kebun, atau toko untuk berinteraksi.")
                }
            }
            pushHudSnapshot()
        }
    }

    fun onFishingTap() {
        Gdx.app.postRunnable { handleFishingTapInternal() }
    }

    private fun handleFishingTapInternal() {
        val res = fishing.onTimingTap()
        if (res.splashTriggered) {
            particles.spawnWaterDrops(fishing.bobberX, fishing.bobberY, 8)
            audio.play(RpgSfx.WATER, 1.15f)
        }
        val caught = res.caughtFish
        if (caught != null) {
            inventory.addItem(caught.id, 1)
            audio.play(RpgSfx.CATCH)
            particles.spawnHarvestSparkles(player.x, player.y + 16f, 14)
            val notices = progression.recordFishCaught(caught.id, caught.rarity)
            // Award achievement coins automatically if any achievement unlocked
            for (ach in progression.getAchievementStates()) {
                if (ach.unlocked && notices.any { it.contains(ach.def.title) }) {
                    economy.coins += ach.def.coinReward
                }
            }
            showToast(notices.firstOrNull() ?: "Dapat ${caught.name} (${caught.rarity.label}, ${caught.sellPrice} koin)!")
            saveGameAsync(silent = true)
        } else if (res.escaped) {
            audio.play(RpgSfx.UI, 0.7f)
            showToast(fishing.toUiState().lastResultMessage)
        }
        pushHudSnapshot()
    }

    fun cancelFishing() {
        Gdx.app.postRunnable {
            fishing.cancel()
            player.clearAction()
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    // Construction actions
    fun startConstruction(itemId: String) {
        Gdx.app.postRunnable {
            val (fx, fy) = player.getFacingTile()
            building.startBuildMode(itemId, fx, fy)
            audio.play(RpgSfx.UI)
            showToast("Arahkan karakter ke rumput kosong lalu tekan Pasang.")
            pushHudSnapshot()
        }
    }

    fun rotateConstruction() {
        Gdx.app.postRunnable {
            building.rotatePreview()
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun cancelConstruction() {
        Gdx.app.postRunnable {
            building.cancelBuildMode()
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun confirmBuildPlacement() {
        Gdx.app.postRunnable { confirmBuildPlacementInternal() }
    }

    private fun confirmBuildPlacementInternal() {
        val res = building.confirmPlacement()
        if (res.success) {
            audio.play(RpgSfx.BUILD)
            particles.spawnHarvestSparkles(building.previewTileX * 16f + 8f, building.previewTileY * 16f + 8f, 10)
            val notices = progression.recordObjectBuilt()
            showToast(notices.firstOrNull() ?: res.message)
            building.cancelBuildMode()
            saveGameAsync(silent = true)
        } else {
            audio.play(RpgSfx.UI, 0.7f)
            showToast(res.message)
        }
        pushHudSnapshot()
    }

    fun upgradeHouse() {
        Gdx.app.postRunnable {
            val res = building.upgradeHouse()
            if (res.success && res.newHouseTier != null) {
                audio.play(RpgSfx.BUILD)
                audio.play(RpgSfx.CATCH)
                particles.spawnHarvestSparkles(
                    HomesteadWorld.HOUSE_TILE_X * 16f + 40f,
                    HomesteadWorld.HOUSE_TILE_Y * 16f + 32f,
                    22
                )
                val notices = progression.recordHouseUpgraded(res.newHouseTier)
                for (ach in progression.getAchievementStates()) {
                    if (ach.unlocked && notices.any { it.contains(ach.def.title) }) {
                        economy.coins += ach.def.coinReward
                    }
                }
                showToast(notices.firstOrNull() ?: res.message)
                saveGameAsync(silent = true)
            } else {
                showToast(res.message)
            }
            pushHudSnapshot()
        }
    }

    // Shop & Inventory actions
    fun buyShopItem(itemId: String, quantity: Int = 1) {
        Gdx.app.postRunnable {
            val res = economy.buyItem(itemId, quantity)
            audio.play(if (res.success) RpgSfx.COIN else RpgSfx.UI)
            showToast(res.message)
            if (res.success) saveGameAsync(silent = true)
            pushHudSnapshot()
        }
    }

    fun sellInventoryItem(itemId: String, quantity: Int = 1) {
        Gdx.app.postRunnable {
            val res = economy.sellItem(itemId, quantity)
            if (res.success) {
                audio.play(RpgSfx.COIN)
                progression.recordCoinsEarned(res.coinsDelta)
                saveGameAsync(silent = true)
            }
            showToast(res.message)
            pushHudSnapshot()
        }
    }

    fun sellAllFish() {
        Gdx.app.postRunnable {
            val res = economy.sellAllInCategory(ItemCategory.FISH)
            if (res.success) {
                audio.play(RpgSfx.COIN)
                progression.recordCoinsEarned(res.coinsDelta)
                saveGameAsync(silent = true)
            }
            showToast(res.message)
            pushHudSnapshot()
        }
    }

    fun sellAllFishAndCrops() {
        Gdx.app.postRunnable {
            val res = economy.sellAllFishAndCrops()
            if (res.success) {
                audio.play(RpgSfx.COIN)
                progression.recordCoinsEarned(res.coinsDelta)
                saveGameAsync(silent = true)
            }
            showToast(res.message)
            pushHudSnapshot()
        }
    }

    fun discardItem(itemId: String, quantity: Int = 1) {
        Gdx.app.postRunnable {
            val def = RpgCatalog.itemById[itemId]
            if (def?.category == ItemCategory.TOOLS) {
                showToast("Alat utama tidak bisa dibuang.")
            } else if (inventory.removeItem(itemId, quantity)) {
                audio.play(RpgSfx.UI)
                showToast("Membuang ${quantity}x ${def?.name ?: itemId}.")
            }
            pushHudSnapshot()
        }
    }

    fun claimQuestReward(questId: String) {
        Gdx.app.postRunnable {
            val reward = progression.claimQuest(questId)
            if (reward != null) {
                economy.coins += reward.first
                audio.play(RpgSfx.CATCH)
                showToast("Hadiah misi diklaim: +${reward.first} koin & +${reward.second} EXP!")
                saveGameAsync(silent = true)
            }
            pushHudSnapshot()
        }
    }

    fun updateCustomization(custom: PlayerCustomization) {
        Gdx.app.postRunnable {
            player.customization = custom
            audio.play(RpgSfx.UI)
            saveGameAsync(silent = true)
            pushHudSnapshot()
        }
    }

    fun applySettings(sfxVol: Float, bgmVol: Float, zoom: Float, weatherEnabled: Boolean) {
        audio.sfxVolume = sfxVol
        audio.bgmVolume = bgmVol
        cameraZoom = zoom
        world.weatherEnabled = weatherEnabled
        if (!weatherEnabled) world.isRaining = false
    }

    fun saveGameAsync(silent: Boolean = false) {
        if (!isInitialized) return
        val bundle = RpgSaveBundle(
            player = PlayerStateEntity(
                id = 1,
                posX = player.x,
                posY = player.y,
                facing = player.direction.key,
                level = progression.playerLevel,
                exp = progression.exp,
                coins = economy.coins,
                fishingLevel = progression.fishingLevel,
                fishingExp = progression.fishingExp,
                farmingLevel = progression.farmingLevel,
                farmingExp = progression.farmingExp,
                gatheringLevel = progression.gatheringLevel,
                gatheringExp = progression.gatheringExp,
                houseTier = world.houseTier,
                gameTimeHours = world.timeHours,
                dayCount = world.dayCount,
                selectedToolId = player.selectedToolId,
                selectedSeedId = player.selectedSeedId,
                shirtColorHex = player.customization.shirtColorHex,
                hatColorHex = player.customization.hatColorHex,
                ribbonColorHex = player.customization.ribbonColorHex,
                updatedAtMs = System.currentTimeMillis()
            ),
            inventory = inventory.toEntities(),
            fishCollection = progression.toFishEntities(),
            trees = world.toTreeEntities(),
            crops = world.toCropEntities(),
            placedObjects = world.toPlacedEntities(),
            quests = progression.toQuestEntities(),
            achievements = progression.toAchievementEntities()
        )
        ioScope.launch {
            runCatching { saveRepo.saveGame(bundle) }
            if (!silent) {
                Gdx.app.postRunnable {
                    audio.play(RpgSfx.UI)
                    showToast("Progres permainan berhasil disimpan!")
                    pushHudSnapshot()
                }
            }
        }
    }

    private fun showToast(msg: String) {
        if (msg.isBlank()) return
        toastMessage = msg
        toastCounter++
    }

    private fun pushHudSnapshot() {
        _hudState.value = RpgHudSnapshot(
            coins = economy.coins,
            playerLevel = progression.playerLevel,
            exp = progression.exp,
            nextLevelExp = RpgCatalog.expRequiredForLevel(progression.playerLevel),
            fishingLevel = progression.fishingLevel,
            farmingLevel = progression.farmingLevel,
            gatheringLevel = progression.gatheringLevel,
            houseTier = world.houseTier,
            timeHours = world.timeHours,
            dayCount = world.dayCount,
            isRaining = world.isRaining,
            isRunning = player.isRunning,
            selectedToolId = player.selectedToolId,
            selectedSeedId = player.selectedSeedId,
            contextAction = player.detectContextAction(world, inventory),
            fishingState = fishing.toUiState(),
            buildPreview = building.toPreviewState(),
            inventory = inventory.getStacks(sortMode),
            caughtFishCounts = progression.getCaughtFishCounts(),
            quests = progression.getQuestStates(),
            achievements = progression.getAchievementStates(),
            customization = player.customization,
            toastMessage = toastMessage,
            toastId = toastCounter
        )
    }

    override fun pause() {
        audio.pauseBgm()
        saveGameAsync(silent = true)
    }

    override fun resume() {
        audio.resumeBgm()
    }

    override fun dispose() {
        saveGameAsync(silent = true)
        runCatching { mapRenderer.dispose() }
        runCatching { batch.dispose() }
        world.dispose()
        assets.dispose()
        audio.dispose()
        ioScope.cancel()
    }
}
