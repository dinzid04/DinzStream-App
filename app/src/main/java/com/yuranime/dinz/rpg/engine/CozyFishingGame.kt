package com.yuranime.dinz.rpg.engine

import android.content.Context
import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.yuranime.dinz.rpg.animation.RpgAssetManager
import com.yuranime.dinz.rpg.audio.RpgAudioManager
import com.yuranime.dinz.rpg.audio.RpgSfx
import com.yuranime.dinz.rpg.building.BuildingSystem
import com.yuranime.dinz.rpg.controls.AStarPathfinder
import com.yuranime.dinz.rpg.crafting.CraftingSystem
import com.yuranime.dinz.rpg.economy.EconomySystem
import com.yuranime.dinz.rpg.farming.FarmingSystem
import com.yuranime.dinz.rpg.fishing.FishingSystem
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.mining.MiningSystem
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.ContextualAction
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.NpcDialogueUiState
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.RecipeType
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.model.WorldRegion
import com.yuranime.dinz.rpg.npc.NpcSystem
import com.yuranime.dinz.rpg.persistence.PlayerStateEntity
import com.yuranime.dinz.rpg.persistence.RpgSaveBundle
import com.yuranime.dinz.rpg.persistence.RpgSaveRepository
import com.yuranime.dinz.rpg.player.PlayerController
import com.yuranime.dinz.rpg.quests.ProgressionSystem
import com.yuranime.dinz.rpg.world.HomesteadWorld
import com.yuranime.dinz.rpg.world.MapPortal
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
    NONE, MERCHANT, SELL_BIN, STORAGE, HOUSE_UPGRADE, COOKING, CRAFTING
}

class CozyFishingGame(
    private val appContext: Context,
    private val userUid: String,
    private val userDisplayName: String,
    private val onOpenModalRequest: (RpgModalRequest) -> Unit
) : ApplicationAdapter() {

    companion object {
        private const val TAG = "SakuraValeGame"
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveRepo = RpgSaveRepository(appContext)

    private var camera: OrthographicCamera? = null
    private var batch: SpriteBatch? = null
    private var mapRenderer: OrthogonalTiledMapRenderer? = null

    private val assets = RpgAssetManager()
    private val audio = RpgAudioManager()
    private val world = HomesteadWorld()
    private val npcSystem = NpcSystem()
    private val particles = ParticleSystem()
    private val inventory = InventorySystem()
    private val economy = EconomySystem(inventory)
    private val progression = ProgressionSystem()
    private val player = PlayerController()
    private val fishing = FishingSystem()
    private val farming = FarmingSystem(world, inventory)
    private val mining = MiningSystem(inventory)
    private val crafting = CraftingSystem(inventory)
    private val building = BuildingSystem(world, inventory, economy)

    private val _hudState = MutableStateFlow(
        RpgHudSnapshot(
            accountName = userDisplayName,
            cloudSyncStatus = "Menyinkronkan akun..."
        )
    )
    val hudState: StateFlow<RpgHudSnapshot> = _hudState.asStateFlow()

    @Volatile
    var cameraZoom: Float = 1.0f
    @Volatile
    private var isPausedByMenu: Boolean = false
    @Volatile
    private var sortMode: InventorySortMode = InventorySortMode.CATEGORY
    @Volatile
    private var glAssetsReady: Boolean = false
    @Volatile
    private var isDisposed: Boolean = false

    private var globalAnimTime: Float = 0f
    private var uiSyncTimer: Float = 0f
    private var autoSaveTimer: Float = 0f
    private var blockedToastCooldown: Float = 0f
    private var portalCooldown: Float = 0f
    private var levelUpAuraTimer: Float = 0f

    // Smooth black fade transition when entering/exiting buildings & cave
    private var fadeAlpha: Float = 0f
    private var fadeDir: Int = 0 // 1 = fading out, -1 = fading in
    private var pendingPortal: MapPortal? = null

    private var activeDialogue: NpcDialogueUiState = NpcDialogueUiState()
    private var sleepPromptOpen: Boolean = false
    private var toastMessage: String = ""
    private var toastCounter: Long = 0L
    private var cloudSyncStatus: String = "Menyinkronkan akun..."
    private var fatalErrorMessage: String? = null
    private var isSaveReady: Boolean = false
    private var lastRegionId: String = WorldRegion.HOMESTEAD.id

    // Layer indices in all 7 inline-tileset TMX maps:
    // 0:ground, 1:ground_detail, 2:water, 3:cliffs, 4:buildings, 5:objects, 6:collision(hidden), 7:foreground
    private val baseLayerIndices = intArrayOf(0, 1, 2, 3, 4, 5)
    private val foregroundLayerIndices = intArrayOf(7)
    private val unprojVec = Vector3()

    private inline fun postToGl(crossinline block: () -> Unit) {
        if (isDisposed) return
        val app = Gdx.app ?: return
        app.postRunnable {
            if (isDisposed) return@postRunnable
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "Safely caught GL runnable error: ${t.message}", t)
            }
        }
    }

    override fun create() {
        try {
            camera = OrthographicCamera()
            batch = SpriteBatch()

            assets.load()
            audio.load()
            val startMap = world.loadMap(MapId.OVERWORLD)
            inventory.initStarterKit()
            mapRenderer = OrthogonalTiledMapRenderer(startMap, 1f)
            npcSystem.syncSchedulesImmediately(world.timeHours)
            glAssetsReady = true

            ioScope.launch {
                val result = runCatching { saveRepo.loadLatestForUser(userUid) }.getOrNull()
                postToGl {
                    if (result?.bundle != null) {
                        applySaveBundle(result.bundle)
                        cloudSyncStatus = result.sourceLabel
                        isSaveReady = true
                        showToast("Selamat datang kembali di Sakura Vale, $userDisplayName! (Hari ${world.dayCount})")
                    } else {
                        cloudSyncStatus = result?.sourceLabel ?: "Database Akun Siap"
                        isSaveReady = true
                        showToast("Selamat datang di Sakura Vale! Ketuk tanah, warga, atau pintu untuk berpetualang.")
                        saveGameAsync(silent = true)
                    }
                    pushHudSnapshot()
                }
            }
            pushHudSnapshot()
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal inisialisasi Sakura Vale RPG: ${t.message}", t)
            glAssetsReady = false
            fatalErrorMessage = "Gagal memuat aset game: ${t.localizedMessage ?: t.javaClass.simpleName}"
            pushHudSnapshot()
        }
    }

    private fun applySaveBundle(bundle: RpgSaveBundle) {
        val p = bundle.player
        val targetMap = MapId.fromId(p.currentMapId)
        switchActiveMapImmediate(targetMap)

        val safeX = p.posX.coerceIn(24f, (world.worldW - 24f).coerceAtLeast(25f))
        val safeY = p.posY.coerceIn(24f, (world.worldH - 24f).coerceAtLeast(25f))
        // If migrating from an old v2 save where coordinates might land on a solid wall in 80x64, snap to starter homestead!
        val tileX = (safeX / HomesteadWorld.TILE_SIZE).toInt()
        val tileY = (safeY / HomesteadWorld.TILE_SIZE).toInt()
        if (world.isBlockedForWalking(tileX, tileY, p.level)) {
            switchActiveMapImmediate(MapId.OVERWORLD)
            player.x = 14f * HomesteadWorld.TILE_SIZE + 8f
            player.y = 43f * HomesteadWorld.TILE_SIZE + 8f
        } else {
            player.x = safeX
            player.y = safeY
        }

        player.direction = Direction.entries.find { it.key == p.facing } ?: Direction.DOWN
        player.energy = p.energy.coerceIn(1, player.maxEnergy)
        player.selectedToolId = p.selectedToolId
        player.selectedSeedId = p.selectedSeedId
        player.customization = PlayerCustomization(
            presetId = if (RpgCatalog.presetById.containsKey(p.presetId)) p.presetId else "princess",
            outfitTintHex = if (p.outfitTintHex == 0L) 0xFFFFFFFFL else p.outfitTintHex,
            accentHex = if (p.accentHex == 0L) 0xFFC84B31L else p.accentHex
        )

        economy.coins = p.coins.coerceAtLeast(0)
        progression.playerLevel = p.level.coerceAtLeast(1)
        progression.exp = p.exp.coerceAtLeast(0)
        progression.fishingLevel = p.fishingLevel.coerceAtLeast(1)
        progression.fishingExp = p.fishingExp.coerceAtLeast(0)
        progression.farmingLevel = p.farmingLevel.coerceAtLeast(1)
        progression.farmingExp = p.farmingExp.coerceAtLeast(0)
        progression.gatheringLevel = p.gatheringLevel.coerceAtLeast(1)
        progression.gatheringExp = p.gatheringExp.coerceAtLeast(0)
        progression.miningLevel = p.miningLevel.coerceAtLeast(1)
        progression.miningExp = p.miningExp.coerceAtLeast(0)
        progression.cookingLevel = p.cookingLevel.coerceAtLeast(1)
        progression.cookingExp = p.cookingExp.coerceAtLeast(0)
        progression.craftingLevel = p.craftingLevel.coerceAtLeast(1)
        progression.craftingExp = p.craftingExp.coerceAtLeast(0)

        inventory.loadFromEntities(bundle.inventory)
        progression.loadSaved(bundle.fishCollection, bundle.quests, bundle.achievements)
        world.loadSavedWorld(
            savedHouseTier = p.houseTier,
            savedTimeHours = p.gameTimeHours,
            savedDayCount = p.dayCount,
            savedDiscoveredCsv = p.discoveredRegionsCsv,
            savedOpenedChestsCsv = p.openedChestsCsv,
            treeEntities = bundle.trees,
            cropEntities = bundle.crops,
            placedEntities = bundle.placedObjects
        )
        npcSystem.loadFriendships(bundle.npcFriendships)
        npcSystem.syncSchedulesImmediately(world.timeHours)
        camera?.position?.set(player.x, player.y, 0f)
        camera?.update()
        updateRegionAndBgm()
    }

    private fun switchActiveMapImmediate(targetMap: MapId) {
        val loaded = world.loadMap(targetMap)
        runCatching { mapRenderer?.dispose() }
        mapRenderer = OrthogonalTiledMapRenderer(loaded, 1f)
        particles.clear()
        audio.switchBgm(targetMap.bgmTrack)
    }

    private fun beginPortalTransition(portal: MapPortal) {
        if (fadeDir != 0) return
        pendingPortal = portal
        fadeDir = 1
        player.cancelAutoPath()
        audio.play(RpgSfx.DOOR)
    }

    override fun resize(width: Int, height: Int) {
        try {
            if (width <= 0 || height <= 0) return
            val cam = camera ?: return
            val aspect = width.toFloat() / height.toFloat()
            val viewH = 184f
            val viewW = viewH * aspect
            cam.setToOrtho(false, viewW, viewH)
            cam.position.set(player.x, player.y, 0f)
            cam.update()
        } catch (t: Throwable) {
            Log.w(TAG, "resize error: ${t.message}", t)
        }
    }

    override fun render() {
        try {
            Gdx.gl.glClearColor(0.08f, 0.11f, 0.10f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            if (!glAssetsReady || isDisposed) return

            val rawDt = Gdx.graphics.deltaTime.coerceIn(0f, 0.05f)
            val dt = if (isPausedByMenu || sleepPromptOpen) 0f else rawDt
            globalAnimTime += rawDt

            updateFadeTransition(rawDt)
            updateGame(dt)
            renderWorld()
            renderFadeOverlay()
        } catch (t: Throwable) {
            Log.e(TAG, "Safely caught render loop error: ${t.message}", t)
        }
    }

    private fun updateFadeTransition(rawDt: Float) {
        if (fadeDir == 1) {
            fadeAlpha = (fadeAlpha + rawDt * 4.2f).coerceAtMost(1f)
            if (fadeAlpha >= 1f) {
                val p = pendingPortal
                pendingPortal = null
                if (p != null) {
                    switchActiveMapImmediate(p.targetMap)
                    player.x = p.targetTileX * HomesteadWorld.TILE_SIZE + 8f
                    player.y = p.targetTileY * HomesteadWorld.TILE_SIZE + 8f
                    player.direction = if (p.targetMap.isIndoor) Direction.UP else Direction.DOWN
                    portalCooldown = 0.65f
                    camera?.position?.set(player.x, player.y, 0f)
                    camera?.update()
                    updateRegionAndBgm()
                    showToast("Memasuki ${p.targetMap.displayName}")
                    pushHudSnapshot()
                }
                fadeDir = -1
            }
        } else if (fadeDir == -1) {
            fadeAlpha = (fadeAlpha - rawDt * 4.2f).coerceAtLeast(0f)
            if (fadeAlpha <= 0f) fadeDir = 0
        }
    }

    private fun updateGame(dt: Float) {
        val cam = camera ?: return
        if (dt > 0f) {
            if (blockedToastCooldown > 0f) blockedToastCooldown = (blockedToastCooldown - dt).coerceAtLeast(0f)
            if (portalCooldown > 0f) portalCooldown = (portalCooldown - dt).coerceAtLeast(0f)
            if (levelUpAuraTimer > 0f) levelUpAuraTimer = (levelUpAuraTimer - dt).coerceAtLeast(0f)

            val prevDay = world.dayCount
            world.update(dt) { regrownTree ->
                particles.spawnLeaves(
                    regrownTree.tileX * HomesteadWorld.TILE_SIZE + 8f,
                    regrownTree.tileY * HomesteadWorld.TILE_SIZE + 16f,
                    count = 8,
                    isSakura = regrownTree.type == TreeType.SAKURA
                )
            }
            if (world.dayCount > prevDay) {
                npcSystem.onNewDay()
                progression.resetDailyQuests()
            }
            farming.update(dt)
            npcSystem.update(dt, world, if (activeDialogue.active) activeDialogue.npcId else null)

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

            when (fishing.phase) {
                FishingPhase.CASTING -> player.setFishingAnimation(PlayerAction.FISH_CAST)
                FishingPhase.WAITING, FishingPhase.BITE -> player.setFishingAnimation(PlayerAction.FISH_WAIT)
                FishingPhase.MINIGAME -> player.setFishingAnimation(PlayerAction.FISH_REEL)
                FishingPhase.CAUGHT -> player.setFishingAnimation(PlayerAction.CELEBRATE)
                else -> Unit
            }

            if (fadeDir == 0) {
                player.update(
                    dt = dt,
                    world = world,
                    playerLevel = progression.playerLevel,
                    isFishingActive = fishing.isActive(),
                    isDialogueActive = activeDialogue.active,
                    onFootstep = {
                        audio.play(RpgSfx.STEP, 0.90f + Random.nextFloat() * 0.18f)
                    },
                    onBlockedRegion = { msg ->
                        if (blockedToastCooldown <= 0f) {
                            showToast(msg)
                            blockedToastCooldown = 2.2f
                        }
                    },
                    onPendingActionReached = { action ->
                        executeContextualActionInternal(action)
                    }
                )
            }

            // Automatic interior/cave exit mat portal when walking down through doorway!
            if (fadeDir == 0 && portalCooldown <= 0f && world.currentMapId.isIndoor) {
                val ptx = player.getTileX()
                val pty = player.getTileY()
                if (pty <= 1) {
                    val exitPortal = world.getPortalAt(ptx, pty)
                    if (exitPortal != null) {
                        beginPortalTransition(exitPortal)
                    }
                }
            }

            updateRegionAndBgm()

            if (building.isBuildMode) {
                val (fx, fy) = player.getFacingTile()
                building.updatePreviewTile(fx, fy)
            }

            if (!world.currentMapId.isIndoor) {
                if (world.isRaining) {
                    particles.spawnRain(cam.position.x, cam.position.y, cam.viewportWidth * cam.zoom, cam.viewportHeight * cam.zoom)
                } else {
                    val reg = world.getRegionAt(world.currentMapId, player.getTileX(), player.getTileY())
                    if (reg == WorldRegion.HIDDEN_SAKURA || reg == WorldRegion.HOMESTEAD || reg == WorldRegion.VILLAGE_PLAZA) {
                        particles.spawnAmbientSakura(cam.position.x, cam.position.y, cam.viewportWidth * cam.zoom, cam.viewportHeight * cam.zoom)
                    }
                }
            }
            particles.update(dt)

            autoSaveTimer += dt
            if (autoSaveTimer >= 22f) {
                autoSaveTimer = 0f
                saveGameAsync(silent = true)
            }
        }

        // Smooth camera tracking clamped neatly to current map dimensions!
        cam.zoom = MathUtils.lerp(cam.zoom, cameraZoom.coerceIn(0.75f, 1.45f), 0.12f)
        val halfW = (cam.viewportWidth * cam.zoom) * 0.5f
        val halfH = (cam.viewportHeight * cam.zoom) * 0.5f
        val targetX = if (world.worldW > halfW * 2f) {
            player.x.coerceIn(halfW, world.worldW - halfW)
        } else {
            world.worldW * 0.5f
        }
        val targetY = if (world.worldH > halfH * 2f) {
            player.y.coerceIn(halfH, world.worldH - halfH)
        } else {
            world.worldH * 0.5f
        }
        cam.position.x = MathUtils.lerp(cam.position.x, targetX, 0.14f)
        cam.position.y = MathUtils.lerp(cam.position.y, targetY, 0.14f)
        cam.update()

        uiSyncTimer += Gdx.graphics.deltaTime
        val syncInterval = if (fishing.phase == FishingPhase.MINIGAME || fishing.phase == FishingPhase.BITE) 0.016f else 0.06f
        if (uiSyncTimer >= syncInterval) {
            uiSyncTimer = 0f
            pushHudSnapshot()
        }
    }

    private fun updateRegionAndBgm() {
        val reg = world.getRegionAt(world.currentMapId, player.getTileX(), player.getTileY())
        if (reg.id != lastRegionId) {
            lastRegionId = reg.id
            val track = if (world.currentMapId.isIndoor) world.currentMapId.bgmTrack else reg.bgmTrack
            audio.switchBgm(track)
            if (world.discoverRegion(reg)) {
                val notices = progression.recordRegionDiscovered(world.discoveredRegions.size)
                audio.play(RpgSfx.HARVEST)
                showToast(notices.firstOrNull() ?: "Wilayah Baru Ditemukan: ${reg.displayName}!")
            }
        }
    }

    private fun renderWorld() {
        val cam = camera ?: return
        val spriteBatch = batch ?: return
        val renderer = mapRenderer ?: return

        val bg = if (world.currentMapId == MapId.MINING_CAVE) Color(0.06f, 0.06f, 0.09f, 1f)
        else if (world.currentMapId.isIndoor) Color(0.09f, 0.07f, 0.06f, 1f)
        else Color(0.16f, 0.28f, 0.20f, 1f)
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        renderer.setView(cam)
        renderer.render(baseLayerIndices)

        spriteBatch.projectionMatrix = cam.combined
        spriteBatch.begin()

        val camLeft = cam.position.x - cam.viewportWidth * cam.zoom * 0.65f
        val camRight = cam.position.x + cam.viewportWidth * cam.zoom * 0.65f
        val camBottom = cam.position.y - cam.viewportHeight * cam.zoom * 0.65f
        val camTop = cam.position.y + cam.viewportHeight * cam.zoom * 0.65f

        // 1. Animated Water Ripples (culled to camera viewport for 60 FPS!)
        val ripFrame = assets.waterRippleAnim.getKeyFrame(globalAnimTime)
        for ((wx, wy) in world.waterSparkleTiles) {
            val px = wx * HomesteadWorld.TILE_SIZE
            val py = wy * HomesteadWorld.TILE_SIZE
            if (px in camLeft..camRight && py in camBottom..camTop) {
                spriteBatch.draw(ripFrame, px, py, 16f, 16f)
            }
        }

        // 2. Animated Overworld Waterfall, River Watermill, Festival Flags & Farm Soil
        if (world.currentMapId == MapId.OVERWORLD) {
            // Northern Mountain Waterfall (3 tiles wide x 3 tiles tall cascading into river!)
            val wfTop = assets.waterfallTopAnim.getKeyFrame(globalAnimTime)
            val wfMid = assets.waterfallMidAnim.getKeyFrame(globalAnimTime)
            val wfBot = assets.waterfallBotAnim.getKeyFrame(globalAnimTime)
            for (dx in -1..1) {
                val wx = (HomesteadWorld.WATERFALL_TILE_X + dx) * 16f
                spriteBatch.draw(wfTop, wx, HomesteadWorld.WATERFALL_TOP_Y * 16f, 16f, 16f)
                spriteBatch.draw(wfMid, wx, (HomesteadWorld.WATERFALL_TOP_Y - 1) * 16f, 16f, 16f)
                spriteBatch.draw(wfBot, wx, (HomesteadWorld.WATERFALL_TOP_Y - 2) * 16f, 16f, 16f)
            }

            // Animated River Watermill wheel beside eastern bridge!
            val wmFrame = assets.watermillAnim.getKeyFrame(globalAnimTime)
            spriteBatch.draw(wmFrame, HomesteadWorld.WATERMILL_TILE_X * 16f, HomesteadWorld.WATERMILL_TILE_Y * 16f, 26f, 28f)

            // Animated Red Festival Banners flanking Village Torii & Plaza!
            val flagFrame = assets.flagRedAnim.getKeyFrame(globalAnimTime)
            spriteBatch.draw(flagFrame, 37f * 16f, 45f * 16f, 16f, 16f)
            spriteBatch.draw(flagFrame, 41f * 16f, 45f * 16f, 16f, 16f)

            // Dynamic Farmland Tilled / Watered Soil
            for (plot in world.cropPlots.values) {
                if (!plot.isTilled) continue
                val soilTex = if (plot.isWatered) assets.wateredSoilTex else assets.tilledSoilTex
                spriteBatch.draw(soilTex, plot.tileX * 16f, plot.tileY * 16f, 16f, 16f)
            }
        }

        // 3. Tap-to-Move Destination Tile Ring & Contextual Target Highlight
        val tapTile = player.tapMarkerTile
        if (tapTile != null && (player.isAutoWalking || player.tapMarkerTimer > 0f)) {
            val pulse = 0.45f + 0.30f * sin(globalAnimTime * 9f)
            val tx = tapTile.first * 16f
            val ty = tapTile.second * 16f
            spriteBatch.setColor(0.98f, 0.85f, 0.38f, pulse)
            spriteBatch.draw(assets.whitePixel, tx + 1f, ty + 1f, 14f, 1.5f)
            spriteBatch.draw(assets.whitePixel, tx + 1f, ty + 13.5f, 14f, 1.5f)
            spriteBatch.draw(assets.whitePixel, tx + 1f, ty + 1f, 1.5f, 14f)
            spriteBatch.draw(assets.whitePixel, tx + 13.5f, ty + 1f, 1.5f, 14f)
            spriteBatch.setColor(1f, 1f, 1f, 1f)
        } else {
            val ctx = player.detectContextAction(world, inventory, npcSystem)
            if (!building.isBuildMode && !fishing.isActive() && ctx.type != ContextActionType.NONE) {
                val pulse = 0.22f + 0.12f * sin(globalAnimTime * 4.5f)
                spriteBatch.setColor(1f, 0.92f, 0.48f, pulse)
                spriteBatch.draw(assets.whitePixel, ctx.targetTileX * 16f, ctx.targetTileY * 16f, 16f, 16f)
                spriteBatch.setColor(1f, 1f, 1f, 1f)
            }
        }

        // 4. Y-Sorted World Entities (House, Trees, Crops, Ores, Forageables, Chests, Furniture, Animals, NPCs, Player!)
        val drawables = mutableListOf<Pair<Float, () -> Unit>>()

        if (world.currentMapId == MapId.OVERWORLD) {
            // Upgradeable Player Homestead House (Tier 1..3) + Cozy Chimney Smoke!
            val hy = HomesteadWorld.HOUSE_TILE_Y * 16f
            drawables.add(hy to {
                val houseTex = assets.houseTextures[world.houseTier] ?: assets.houseTextures[1]
                if (houseTex != null) {
                    spriteBatch.draw(houseTex, HomesteadWorld.HOUSE_TILE_X * 16f, hy, 64f, 48f)
                    if (world.houseTier >= 2) {
                        val sm = assets.smokeAnim.getKeyFrame(globalAnimTime)
                        spriteBatch.setColor(1f, 1f, 1f, 0.72f)
                        spriteBatch.draw(sm, HomesteadWorld.HOUSE_TILE_X * 16f + 44f, hy + 42f, 18f, 18f)
                        spriteBatch.setColor(1f, 1f, 1f, 1f)
                    }
                }
            })

            // Animated Farm Windmill Blades spinning above Barn!
            val wmy = HomesteadWorld.WINDMILL_TILE_Y * 16f - 10f
            drawables.add(wmy to {
                val blades = assets.windmillAnim.getKeyFrame(globalAnimTime)
                spriteBatch.draw(blades, HomesteadWorld.WINDMILL_TILE_X * 16f - 8f, HomesteadWorld.WINDMILL_TILE_Y * 16f + 2f, 64f, 64f)
            })

            // Crops (6 species x 5 stages)
            for (plot in world.cropPlots.values) {
                val crop = plot.cropType ?: continue
                val cy = plot.tileY * 16f
                drawables.add(cy to {
                    val tex = assets.cropStageTextures[crop to plot.stage]
                    if (tex != null) {
                        val swayX = if (plot.stage >= 2) sin(globalAnimTime * 2.5f + plot.tileX) * 0.6f else 0f
                        spriteBatch.draw(tex, plot.tileX * 16f + swayX, cy, 16f, 18f)
                    }
                })
            }

            // Interactive Chop-able Trees (28+ trees across Homestead, Forest & Hidden Sakura!)
            for (tree in world.trees) {
                val txPx = tree.tileX * 16f
                val tyPx = tree.tileY * 16f
                if (txPx !in (camLeft - 48f)..(camRight + 48f) || tyPx !in (camBottom - 48f)..(camTop + 48f)) continue
                drawables.add(tyPx to {
                    if (tree.state == TreeHealthState.STUMP) {
                        spriteBatch.draw(assets.treeStumpTex, txPx, tyPx, 16f, 16f)
                    } else {
                        val shakeX = if (tree.shakeTimer > 0f) sin(tree.shakeTimer * 52f) * 2.4f else 0f
                        val tex = assets.treeTextures[tree.type]
                        if (tex != null) {
                            val sz = tree.type.drawSizePx
                            val dx = txPx + 8f - sz * 0.5f + shakeX
                            val fallRot = if (tree.state == TreeHealthState.FALLING) (tree.fallTimer / 0.45f) * 78f else 0f
                            val alpha = if (tree.state == TreeHealthState.FALLING) (1f - tree.fallTimer / 0.45f).coerceIn(0.15f, 1f) else 1f
                            spriteBatch.setColor(1f, 1f, 1f, alpha)
                            spriteBatch.draw(
                                tex,
                                dx, tyPx - 2f,
                                sz * 0.5f, 4f,
                                sz, sz,
                                1f, 1f,
                                fallRot,
                                0, 0, tex.width, tex.height,
                                false, false
                            )
                            spriteBatch.setColor(1f, 1f, 1f, 1f)
                        }
                    }
                })
            }
        }

        // Mineable Ore Rocks (Overworld Mountain & Mining Cave)
        for (ore in world.oreNodes) {
            if (ore.mapId != world.currentMapId || ore.depleted) continue
            val oy = ore.tileY * 16f
            drawables.add(oy to {
                val tex = assets.oreRockTextures[ore.type]
                if (tex != null) {
                    val shake = if (ore.shakeTimer > 0f) sin(ore.shakeTimer * 55f) * 2.0f else 0f
                    spriteBatch.draw(tex, ore.tileX * 16f + shake, oy, 16f, 16f)
                }
            })
        }

        // Wild Forageables (Berries, Chestnuts, Mountain Tea Leaves)
        for (forage in world.forageNodes) {
            if (forage.mapId != world.currentMapId || forage.collected) continue
            val fy = forage.tileY * 16f
            drawables.add(fy to {
                val tex = assets.forageTextures[forage.itemId]
                if (tex != null) {
                    val bob = sin(globalAnimTime * 3.0f + forage.tileX) * 0.9f
                    spriteBatch.draw(tex, forage.tileX * 16f + 1f, fy + 1f + bob, 14f, 14f)
                }
            })
        }

        // Discoverable Treasure Chests
        for (chest in world.treasureChests) {
            if (chest.mapId != world.currentMapId || chest.opened) continue
            val cy = chest.tileY * 16f
            drawables.add(cy to {
                spriteBatch.draw(assets.chestClosedTex, chest.tileX * 16f, cy + 1f, 16f, 14f)
                val pulse = 0.5f + 0.5f * sin(globalAnimTime * 4f)
                val spark = assets.particleRegions["sparkle_harvest"]
                if (spark != null) {
                    spriteBatch.setColor(1f, 0.95f, 0.6f, pulse)
                    spriteBatch.draw(spark, chest.tileX * 16f + 4f, cy + 14f, 8f, 8f)
                    spriteBatch.setColor(1f, 1f, 1f, 1f)
                }
            })
        }

        // Placed Indoor & Outdoor Furniture / Decorations
        for (obj in world.getPlacedObjectsInCurrentMap()) {
            val oy = obj.tileY * 16f
            drawables.add(oy to {
                val tex = assets.objectTextures[obj.itemId]
                if (tex != null) {
                    val dw = tex.width.toFloat().coerceIn(16f, 32f)
                    val dh = tex.height.toFloat().coerceIn(16f, 32f)
                    val ox = obj.tileX * 16f + (16f - dw) * 0.5f
                    spriteBatch.draw(
                        tex,
                        ox, oy,
                        dw * 0.5f, dh * 0.5f,
                        dw, dh,
                        1f, 1f,
                        obj.rotation.toFloat(),
                        0, 0, tex.width, tex.height,
                        false, false
                    )
                }
            })
        }

        // 5 Living Village Animals (Overworld)
        if (world.currentMapId == MapId.OVERWORLD) {
            for (animal in npcSystem.animals) {
                val anim = assets.animalAnims[animal.species] ?: continue
                drawables.add(animal.y to {
                    spriteBatch.draw(assets.shadowTex, animal.x - 6f, animal.y - 4f, 12f, 5f)
                    val frame = anim.getKeyFrame(animal.animTime)
                    if (animal.facingLeft) {
                        spriteBatch.draw(frame, animal.x + 8f, animal.y - 3f, -16f, 16f)
                    } else {
                        spriteBatch.draw(frame, animal.x - 8f, animal.y - 3f, 16f, 16f)
                    }
                })
            }
        }

        // 8 Living Anime NPCs (on current map!) + Floating Emote Bubbles!
        for (npc in npcSystem.getNpcsInMap(world.currentMapId)) {
            drawables.add(npc.y to {
                spriteBatch.draw(assets.shadowTex, npc.x - 7f, npc.y - 4f, 14f, 6f)
                val frame = assets.getNpcFrame(npc.def.id, npc.isWalking, npc.direction, npc.stateTime)
                spriteBatch.draw(frame, npc.x - 9f, npc.y - 3f, 18f, 18f)
                val emKey = npc.emoteKey
                if (emKey != null) {
                    val emTex = assets.emoteTextures[emKey]
                    if (emTex != null) {
                        val floatY = sin(globalAnimTime * 6f) * 1.4f
                        spriteBatch.draw(emTex, npc.x - 6f, npc.y + 17f + floatY, 12f, 11f)
                    }
                }
            })
        }

        // Anime Protagonist + Soft Shadow + Held Tool In Hand + Level-Up Aura!
        drawables.add(player.y to {
            spriteBatch.draw(assets.shadowTex, player.x - 7f, player.y - 4f, 14f, 6f)
            val pFrame = assets.getPlayerFrame(player.customization.presetId, player.action, player.direction, player.stateTime)
            val tintR = ((player.customization.outfitTintHex shr 16) and 0xFF) / 255f
            val tintG = ((player.customization.outfitTintHex shr 8) and 0xFF) / 255f
            val tintB = (player.customization.outfitTintHex and 0xFF) / 255f
            spriteBatch.setColor(0.84f + tintR * 0.16f, 0.84f + tintG * 0.16f, 0.84f + tintB * 0.16f, 1f)
            spriteBatch.draw(pFrame, player.x - 9f, player.y - 3f, 18f, 18f)
            spriteBatch.setColor(1f, 1f, 1f, 1f)

            renderHeldToolInHand(spriteBatch)

            if (levelUpAuraTimer > 0f) {
                val aura = assets.levelupAuraAnim.getKeyFrame(globalAnimTime)
                spriteBatch.setColor(1f, 0.94f, 0.65f, (levelUpAuraTimer / 1.8f).coerceIn(0f, 0.85f))
                spriteBatch.draw(aura, player.x - 10f, player.y - 4f, 20f, 34f)
                spriteBatch.setColor(1f, 1f, 1f, 1f)
            }
        })

        drawables.sortByDescending { it.first }
        for ((_, drawFn) in drawables) drawFn()

        // Fishing Line, Bobber & Ripples
        if (fishing.isActive()) {
            val rodTipX = player.x + when (player.direction) {
                Direction.RIGHT -> 11f
                Direction.LEFT -> -11f
                else -> 4f
            }
            val rodTipY = player.y + when (player.direction) {
                Direction.UP -> 15f
                Direction.DOWN -> 3f
                else -> 10f
            }
            drawPixelLine(spriteBatch, rodTipX, rodTipY, fishing.bobberX, fishing.bobberY + 2f)
            spriteBatch.draw(ripFrame, fishing.bobberX - 8f, fishing.bobberY - 8f, 16f, 16f)
            val bobOffset = if (fishing.phase == FishingPhase.BITE) -2.5f else sin(globalAnimTime * 4f) * 1.1f
            val bobTex = if (fishing.phase == FishingPhase.BITE || fishing.phase == FishingPhase.MINIGAME) {
                assets.bobberBite
            } else {
                assets.bobberIdle
            }
            spriteBatch.draw(bobTex, fishing.bobberX - 7f, fishing.bobberY - 5f + bobOffset, 14f, 14f)
            if (fishing.phase == FishingPhase.BITE) {
                assets.emoteTextures["exclaim"]?.let {
                    spriteBatch.draw(it, player.x - 6f, player.y + 18f, 12f, 12f)
                }
            }
        }

        // Construction Placement Preview (Indoor & Outdoor!)
        if (building.isBuildMode) {
            val px = building.previewTileX * 16f
            val py = building.previewTileY * 16f
            val valid = building.canPlaceAt(building.previewTileX, building.previewTileY)
            spriteBatch.setColor(if (valid) 0.25f else 0.95f, if (valid) 0.92f else 0.25f, 0.35f, 0.38f)
            spriteBatch.draw(assets.whitePixel, px, py, 16f, 16f)
            val objTex = building.selectedItemId?.let { assets.objectTextures[it] }
            if (objTex != null) {
                val dw = objTex.width.toFloat().coerceIn(16f, 32f)
                val dh = objTex.height.toFloat().coerceIn(16f, 32f)
                spriteBatch.setColor(if (valid) 0.85f else 1f, if (valid) 1f else 0.55f, if (valid) 0.85f else 0.55f, 0.78f)
                spriteBatch.draw(
                    objTex,
                    px + (16f - dw) * 0.5f, py,
                    dw * 0.5f, dh * 0.5f,
                    dw, dh,
                    1f, 1f,
                    building.rotation.toFloat(),
                    0, 0, objTex.width, objTex.height,
                    false, false
                )
            }
            spriteBatch.setColor(1f, 1f, 1f, 1f)
        }

        particles.render(spriteBatch, assets)
        spriteBatch.end()

        renderer.render(foregroundLayerIndices)
        renderAtmosphericLighting(cam, spriteBatch)
    }

    private fun renderHeldToolInHand(spriteBatch: SpriteBatch) {
        val toolKey = when (player.action) {
            PlayerAction.AXE -> "axe"
            PlayerAction.PICKAXE -> "pickaxe"
            PlayerAction.HOE -> "hoe"
            PlayerAction.WATER -> "water"
            PlayerAction.FISH_CAST, PlayerAction.FISH_WAIT, PlayerAction.FISH_REEL -> "rod"
            PlayerAction.CRAFT -> "hammer"
            else -> return
        }
        val tex = assets.inHandTools[toolKey] ?: return
        val ox = when (player.direction) {
            Direction.LEFT -> -11f
            Direction.RIGHT -> 5f
            else -> 3f
        }
        val oy = when (player.direction) {
            Direction.UP -> 9f
            Direction.DOWN -> -2f
            else -> 3f
        }
        val swingAngle = sin(player.stateTime * 22f) * 28f
        spriteBatch.draw(
            tex,
            player.x + ox, player.y + oy,
            4f, 4f,
            10f, 10f,
            1f, 1f,
            swingAngle,
            0, 0, tex.width, tex.height,
            player.direction == Direction.LEFT, false
        )
    }

    private fun drawPixelLine(spriteBatch: SpriteBatch, x0: Float, y0: Float, x1: Float, y1: Float) {
        val steps = 18
        spriteBatch.setColor(0.95f, 0.98f, 1f, 0.88f)
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val sag = sin(t * Math.PI).toFloat() * -4f
            spriteBatch.draw(assets.whitePixel, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t + sag, 1f, 1f)
        }
        spriteBatch.setColor(1f, 1f, 1f, 1f)
    }

    private fun renderAtmosphericLighting(cam: OrthographicCamera, spriteBatch: SpriteBatch) {
        val left = cam.position.x - cam.viewportWidth * cam.zoom * 0.6f
        val bottom = cam.position.y - cam.viewportHeight * cam.zoom * 0.6f
        val spanW = cam.viewportWidth * cam.zoom * 1.2f
        val spanH = cam.viewportHeight * cam.zoom * 1.2f

        spriteBatch.projectionMatrix = cam.combined
        spriteBatch.begin()

        // Warm morning & woodland diagonal sunbeams (Raylight.png!)
        if (!world.currentMapId.isIndoor && world.raylightEnabled && !world.isRaining && world.timeHours in 6.5f..16.5f) {
            spriteBatch.setColor(1f, 0.96f, 0.80f, 0.14f)
            spriteBatch.draw(assets.raylightTex, left + spanW * 0.15f, bottom + spanH * 0.2f, spanW * 0.7f, spanH * 0.7f)
            spriteBatch.setColor(1f, 1f, 1f, 1f)
        }

        val tint: Color? = when {
            world.currentMapId == MapId.MINING_CAVE -> Color(0.08f, 0.10f, 0.22f, 0.34f)
            world.currentMapId.isIndoor -> Color(1.0f, 0.86f, 0.65f, 0.06f)
            world.timeHours in 5.5f..7.5f -> {
                val k = 1f - (world.timeHours - 5.5f) / 2f
                Color(0.95f, 0.64f, 0.40f, 0.15f * k)
            }
            world.timeHours in 17.5f..20.0f -> {
                val k = (world.timeHours - 17.5f) / 2.5f
                Color(0.85f - 0.72f * k, 0.44f - 0.28f * k, 0.28f + 0.18f * k, 0.16f + 0.26f * k)
            }
            world.timeHours >= 20.0f || world.timeHours < 5.5f -> Color(0.08f, 0.12f, 0.28f, 0.42f)
            world.isRaining -> Color(0.16f, 0.24f, 0.34f, 0.16f)
            else -> null
        }

        if (tint != null) {
            spriteBatch.setColor(tint)
            spriteBatch.draw(assets.whitePixel, left, bottom, spanW, spanH)

            // Warm golden lantern & crystal lights in night or cave!
            if (world.isNightOrDusk() || world.currentMapId == MapId.MINING_CAVE) {
                val glowSpots = mutableListOf<Pair<Float, Float>>()
                if (world.currentMapId == MapId.OVERWORLD) {
                    listOf(
                        11 to 44, 24 to 39, 32 to 39, 47 to 39, 58 to 39, 35 to 32, 42 to 32, 60 to 40,
                        13 to 46, 14 to 46, 39 to 33
                    ).forEach { (lx, ly) -> glowSpots.add((lx * 16f + 8f) to (ly * 16f + 8f)) }
                } else if (world.currentMapId == MapId.MINING_CAVE) {
                    listOf(12 to 5, 19 to 5, 12 to 14, 22 to 14, 7 to 14).forEach { (lx, ly) ->
                        glowSpots.add((lx * 16f + 8f) to (ly * 16f + 8f))
                    }
                }
                world.getPlacedObjectsInCurrentMap().forEach { obj ->
                    if (obj.itemId in listOf("lamp_post", "campfire", "lantern_stone")) {
                        glowSpots.add((obj.tileX * 16f + 8f) to (obj.tileY * 16f + 8f))
                    }
                }
                spriteBatch.setColor(1f, 0.86f, 0.48f, 0.22f)
                for ((gx, gy) in glowSpots) {
                    spriteBatch.draw(assets.whitePixel, gx - 20f, gy - 20f, 40f, 40f)
                }
            }
            spriteBatch.setColor(1f, 1f, 1f, 1f)
        }
        spriteBatch.end()
    }

    private fun renderFadeOverlay() {
        if (fadeAlpha <= 0.01f) return
        val cam = camera ?: return
        val spriteBatch = batch ?: return
        spriteBatch.projectionMatrix = cam.combined
        spriteBatch.begin()
        spriteBatch.setColor(0.04f, 0.05f, 0.06f, fadeAlpha)
        val left = cam.position.x - cam.viewportWidth * cam.zoom * 0.6f
        val bottom = cam.position.y - cam.viewportHeight * cam.zoom * 0.6f
        spriteBatch.draw(assets.whitePixel, left, bottom, cam.viewportWidth * cam.zoom * 1.2f, cam.viewportHeight * cam.zoom * 1.2f)
        spriteBatch.setColor(1f, 1f, 1f, 1f)
        spriteBatch.end()
    }

    // ==================== TAP-TO-MOVE & SMART CONTEXTUAL INTERACTION ====================

    fun onWorldScreenTap(screenX: Float, screenY: Float, viewW: Float, viewH: Float) {
        postToGl {
            if (!glAssetsReady || isPausedByMenu || sleepPromptOpen || activeDialogue.active || fadeDir != 0) return@postToGl
            if (fishing.isActive()) {
                handleFishingTapInternal()
                return@postToGl
            }
            val cam = camera ?: return@postToGl
            if (viewW <= 0f || viewH <= 0f) return@postToGl

            // Map Compose view pixel coordinates to LibGDX screen coordinates and unproject to world tile!
            val gdxW = Gdx.graphics.width.toFloat().coerceAtLeast(1f)
            val gdxH = Gdx.graphics.height.toFloat().coerceAtLeast(1f)
            unprojVec.set((screenX / viewW) * gdxW, (screenY / viewH) * gdxH, 0f)
            cam.unproject(unprojVec)

            val tx = (unprojVec.x / HomesteadWorld.TILE_SIZE).toInt().coerceIn(0, world.mapWidthTiles - 1)
            val ty = (unprojVec.y / HomesteadWorld.TILE_SIZE).toInt().coerceIn(0, world.mapHeightTiles - 1)

            if (building.isBuildMode) {
                building.updatePreviewTile(tx, ty)
                pushHudSnapshot()
                return@postToGl
            }

            // Locked region check
            if (!world.currentMapId.isIndoor) {
                val tappedRegion = world.getRegionAt(world.currentMapId, tx, ty)
                if (!world.isRegionUnlocked(tappedRegion, progression.playerLevel)) {
                    showToast("Butuh Level ${tappedRegion.requiredLevel} untuk memasuki ${tappedRegion.displayName}!")
                    pushHudSnapshot()
                    return@postToGl
                }
            }

            val startX = player.getTileX()
            val startY = player.getTileY()
            val pLevel = progression.playerLevel
            val walkable: (Int, Int) -> Boolean = { x, y -> !world.isBlockedForWalking(x, y, pLevel) }

            // Identify smart contextual action for the tapped tile!
            val tappedAction = resolveTappedAction(tx, ty)
            if (tappedAction != null) {
                val needDirectTile = tappedAction.type in listOf(
                    ContextActionType.ENTER_PORTAL,
                    ContextActionType.GATHER_FORAGE,
                    ContextActionType.TILL_SOIL,
                    ContextActionType.PLANT_SEED,
                    ContextActionType.WATER_CROP,
                    ContextActionType.HARVEST_CROP
                ) && walkable(tappedAction.targetTileX, tappedAction.targetTileY)

                val path = if (needDirectTile) {
                    if (startX == tappedAction.targetTileX && startY == tappedAction.targetTileY) {
                        emptyList()
                    } else {
                        AStarPathfinder.findPath(startX, startY, tappedAction.targetTileX, tappedAction.targetTileY, world.mapWidthTiles, world.mapHeightTiles, isWalkable = walkable)
                    }
                } else {
                    AStarPathfinder.findPathToAdjacent(startX, startY, tappedAction.targetTileX, tappedAction.targetTileY, world.mapWidthTiles, world.mapHeightTiles, isWalkable = walkable)
                }

                if (path != null && (path.isNotEmpty() || (abs(startX - tappedAction.targetTileX) + abs(startY - tappedAction.targetTileY) <= 1))) {
                    player.setAutoPath(path, tappedAction, tappedAction.targetTileX to tappedAction.targetTileY)
                    audio.play(RpgSfx.UI, 1.15f)
                    pushHudSnapshot()
                    return@postToGl
                }
            }

            // Otherwise plain Tap-to-Move A* pathfinding to walkable ground!
            if (walkable(tx, ty)) {
                val path = AStarPathfinder.findPath(startX, startY, tx, ty, world.mapWidthTiles, world.mapHeightTiles, isWalkable = walkable)
                if (path.isNotEmpty()) {
                    player.setAutoPath(path, null, tx to ty)
                    pushHudSnapshot()
                }
            }
        }
    }

    private fun resolveTappedAction(tx: Int, ty: Int): ContextualAction? {
        // 1. Tapped NPC
        val npc = npcSystem.findNpcAtOrNearTile(world.currentMapId, tx, ty, 1)
        if (npc != null) {
            return ContextualAction(ContextActionType.TALK_NPC, "Bicara ${npc.def.name}", npc.tileX, npc.tileY, npc.def.id)
        }
        // 2. Tapped Door / Portal (allow tapping 1 tile above door on building facade too!)
        val portal = world.getPortalAt(tx, ty) ?: world.getPortalAt(tx, ty - 1) ?: world.getPortalAt(tx, ty + 1)
        if (portal != null) {
            return ContextualAction(ContextActionType.ENTER_PORTAL, portal.label, portal.tileX, portal.tileY, portal.targetMap.id)
        }
        // 3. Tapped Station (Bed, Stove, Workbench, Storage, Sell Bin, Shop, Blacksmith)
        val station = world.getStationAt(tx, ty)
        if (station != null) {
            return ContextualAction(station.actionType, station.label, tx, ty)
        }
        // 4. Tapped Treasure Chest or Wild Forage
        world.getChestAt(tx, ty)?.let {
            return ContextualAction(ContextActionType.OPEN_CHEST, "Buka ${it.label}", tx, ty, it.id)
        }
        world.getForageAt(tx, ty)?.let {
            val nm = RpgCatalog.itemById[it.itemId]?.name ?: "Hasil Alam"
            return ContextualAction(ContextActionType.GATHER_FORAGE, "Ambil $nm", tx, ty, it.id)
        }
        // 5. Tapped Tree (allow tapping trunk OR canopy 1 tile above!)
        val tree = world.getTreeAt(tx, ty) ?: world.getTreeAt(tx, ty - 1)
        if (tree != null && tree.state != TreeHealthState.STUMP && tree.state != TreeHealthState.FALLING) {
            return ContextualAction(ContextActionType.CHOP_TREE, "Tebang ${tree.type.displayName}", tree.tileX, tree.tileY, tree.id)
        }
        // 6. Tapped Ore Rock
        val ore = world.getOreAt(tx, ty)
        if (ore != null) {
            return ContextualAction(ContextActionType.MINE_ORE, "Tambang ${ore.type.displayName}", tx, ty, ore.id)
        }
        // 7. Tapped Water -> walk to shore & cast fishing rod!
        if (world.isWaterTile(tx, ty)) {
            return ContextualAction(ContextActionType.FISH, "Pancing", tx, ty, habitat = world.getWaterHabitat(tx, ty))
        }
        // 8. Tapped Farmland Plot
        if (world.isFarmableTile(tx, ty)) {
            val plot = world.cropPlots["${tx}_${ty}"]
            return when {
                plot == null || !plot.isTilled -> ContextualAction(ContextActionType.TILL_SOIL, "Cangkul Tanah", tx, ty)
                plot.isHarvestable -> ContextualAction(ContextActionType.HARVEST_CROP, "Panen ${plot.cropType?.displayName ?: "Sayur"}", tx, ty)
                plot.cropType == null -> ContextualAction(ContextActionType.PLANT_SEED, "Tanam Bibit", tx, ty)
                !plot.isWatered -> ContextualAction(ContextActionType.WATER_CROP, "Siram Tanaman", tx, ty)
                else -> ContextualAction(ContextActionType.WATER_CROP, "Siram Tanaman", tx, ty)
            }
        }
        // 9. Tapped Placed Furniture
        world.getPlacedObjectAt(tx, ty)?.let {
            val nm = RpgCatalog.itemById[it.itemId]?.name ?: "Furnitur"
            return ContextualAction(ContextActionType.INTERACT_DECOR, "Ambil $nm", tx, ty, it.id)
        }
        return null
    }

    // ==================== PUBLIC GAMEPLAY ACTIONS (CALLED FROM COMPOSE UI THREAD) ====================

    fun setJoystick(x: Float, y: Float) {
        player.joyX = x.coerceIn(-1f, 1f)
        player.joyY = y.coerceIn(-1f, 1f)
    }

    fun toggleSprint() {
        postToGl {
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
            player.cancelAutoPath()
        }
    }

    fun selectSeed(seedId: String) {
        postToGl {
            player.selectedSeedId = seedId
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun selectTool(toolId: String) {
        postToGl {
            player.selectedToolId = toolId
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun setInventorySort(mode: InventorySortMode) {
        sortMode = mode
        postToGl {
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun onContextualActionPressed() {
        postToGl {
            if (!glAssetsReady || fadeDir != 0) return@postToGl
            if (building.isBuildMode) {
                confirmBuildPlacementInternal()
                return@postToGl
            }
            if (fishing.isActive()) {
                handleFishingTapInternal()
                return@postToGl
            }
            val ctx = player.detectContextAction(world, inventory, npcSystem)
            executeContextualActionInternal(ctx)
        }
    }

    private fun executeContextualActionInternal(ctx: ContextualAction) {
        when (ctx.type) {
            ContextActionType.TALK_NPC -> {
                val res = npcSystem.talkToNpc(ctx.targetId, player.x, player.y, world.timeHours)
                if (res != null) {
                    activeDialogue = res.first
                    audio.play(RpgSfx.TALK)
                    val notices = progression.recordNpcTalked(ctx.targetId)
                    if (notices.isNotEmpty()) {
                        levelUpAuraTimer = 1.6f
                        showToast(notices.first())
                    }
                }
            }

            ContextActionType.ENTER_PORTAL -> {
                val portal = world.getPortalAt(ctx.targetTileX, ctx.targetTileY)
                    ?: world.portals.find { it.sourceMap == world.currentMapId && it.targetMap.id == ctx.targetId }
                if (portal != null) {
                    beginPortalTransition(portal)
                }
            }

            ContextActionType.SLEEP_BED -> {
                sleepPromptOpen = true
                audio.play(RpgSfx.UI)
            }

            ContextActionType.OPEN_STOVE -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.COOKING)
            }

            ContextActionType.OPEN_WORKBENCH -> {
                player.triggerActionAnimation(PlayerAction.CRAFT, 0.25f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.CRAFTING)
            }

            ContextActionType.FISH -> {
                if (player.energy < 2) {
                    showToast("Energi habis! Makan masakan dari Tas atau tidur di Rumah.")
                    pushHudSnapshot()
                    return
                }
                player.consumeEnergy(2)
                val tx = ctx.targetTileX * HomesteadWorld.TILE_SIZE + 8f
                val ty = ctx.targetTileY * HomesteadWorld.TILE_SIZE + 8f
                player.faceTile(ctx.targetTileX, ctx.targetTileY)
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
                if (player.energy < 2) {
                    showToast("Energi terlalu rendah! Makan Onigiri atau istirahat di Rumah.")
                    pushHudSnapshot()
                    return
                }
                val tree = world.trees.find { it.id == ctx.targetId } ?: world.getTreeAt(ctx.targetTileX, ctx.targetTileY)
                if (tree != null && tree.state != TreeHealthState.STUMP && tree.state != TreeHealthState.FALLING) {
                    player.consumeEnergy(2)
                    player.faceTile(tree.tileX, tree.tileY)
                    player.triggerActionAnimation(PlayerAction.AXE, 0.32f)
                    audio.play(RpgSfx.AXE)
                    val dmg = inventory.getBestAxeDamage()
                    tree.health = (tree.health - dmg).coerceAtLeast(0)
                    tree.shakeTimer = 0.28f
                    val wx = tree.tileX * 16f + 8f
                    val wy = tree.tileY * 16f + 12f
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
                        audio.play(RpgSfx.WOOD_PICKUP)
                        val notices = progression.recordTreeChopped(gainedWood, tree.type.expReward)
                        if (notices.any { it.contains("Level") }) levelUpAuraTimer = 1.8f
                        val extra = if (gotHardwood) " + 1 Kayu Keras" else ""
                        showToast(notices.firstOrNull() ?: "+$gainedWood Kayu$extra dari ${tree.type.displayName}!")
                        saveGameAsync(silent = true)
                    } else {
                        tree.state = if (tree.health <= 1) TreeHealthState.HEAVILY_DAMAGED else TreeHealthState.DAMAGED
                        showToast("${tree.type.displayName} (${tree.health}/${tree.type.maxHealth})")
                    }
                }
            }

            ContextActionType.MINE_ORE -> {
                if (player.energy < 3) {
                    showToast("Energi terlalu rendah untuk menambang! Makan dulu atau tidur.")
                    pushHudSnapshot()
                    return
                }
                val node = world.oreNodes.find { it.id == ctx.targetId } ?: world.getOreAt(ctx.targetTileX, ctx.targetTileY)
                if (node != null && !node.depleted) {
                    player.consumeEnergy(3)
                    player.faceTile(node.tileX, node.tileY)
                    player.triggerActionAnimation(PlayerAction.PICKAXE, 0.32f)
                    audio.play(RpgSfx.MINE)
                    val wx = node.tileX * 16f + 8f
                    val wy = node.tileY * 16f + 8f
                    particles.spawnRockChips(wx, wy, 8)
                    val res = mining.mineNode(node, inventory.getBestPickaxeDamage(), inventory.hasTool("tool_pickaxe_gold"))
                    if (res.shattered) {
                        audio.play(RpgSfx.TREE_FALL, 1.15f)
                        audio.play(RpgSfx.WOOD_PICKUP)
                        particles.spawnHarvestSparkles(wx, wy + 6f, 10)
                        val notices = progression.recordOreMined(res.expReward)
                        if (notices.any { it.contains("Level") }) levelUpAuraTimer = 1.8f
                        showToast(notices.firstOrNull() ?: res.message)
                        saveGameAsync(silent = true)
                    } else {
                        showToast(res.message)
                    }
                }
            }

            ContextActionType.GATHER_FORAGE -> {
                val node = world.forageNodes.find { it.id == ctx.targetId } ?: world.getForageAt(ctx.targetTileX, ctx.targetTileY)
                if (node != null && !node.collected) {
                    node.collected = true
                    node.respawnTimer = 120f
                    inventory.addItem(node.itemId, 1)
                    player.triggerActionAnimation(PlayerAction.HARVEST, 0.28f)
                    audio.play(RpgSfx.WOOD_PICKUP)
                    particles.spawnHarvestSparkles(node.tileX * 16f + 8f, node.tileY * 16f + 8f, 8)
                    val notices = progression.recordForaged()
                    val name = RpgCatalog.itemById[node.itemId]?.name ?: "Hasil Alam"
                    showToast(notices.firstOrNull() ?: "Memetik 1x $name!")
                }
            }

            ContextActionType.OPEN_CHEST -> {
                val chest = world.treasureChests.find { it.id == ctx.targetId } ?: world.getChestAt(ctx.targetTileX, ctx.targetTileY)
                if (chest != null && !chest.opened) {
                    chest.opened = true
                    economy.coins += chest.coinReward
                    val lootParts = mutableListOf("+${chest.coinReward} Koin")
                    chest.itemRewards.forEach { (iid, qty) ->
                        inventory.addItem(iid, qty)
                        lootParts.add("${qty}x ${RpgCatalog.itemById[iid]?.name ?: iid}")
                    }
                    player.triggerActionAnimation(PlayerAction.CELEBRATE, 0.55f)
                    audio.play(RpgSfx.CATCH)
                    audio.play(RpgSfx.COIN)
                    particles.spawnHarvestSparkles(chest.tileX * 16f + 8f, chest.tileY * 16f + 12f, 18)
                    progression.addPlayerExp(45)
                    levelUpAuraTimer = 1.6f
                    showToast("${chest.label} dibuka! Dapat: ${lootParts.joinToString(", ")}")
                    saveGameAsync(silent = true)
                }
            }

            ContextActionType.TILL_SOIL -> {
                if (player.energy < 1) {
                    showToast("Energi habis! Makan atau istirahat dulu.")
                    pushHudSnapshot()
                    return
                }
                player.consumeEnergy(1)
                player.triggerActionAnimation(PlayerAction.HOE, 0.30f)
                val res = farming.tillSoil(ctx.targetTileX, ctx.targetTileY)
                if (res.success) {
                    audio.play(RpgSfx.PLANT)
                    particles.spawnWoodChips(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 8f, 5)
                }
                showToast(res.message)
            }

            ContextActionType.PLANT_SEED -> {
                player.triggerActionAnimation(PlayerAction.HOE, 0.28f)
                val seedToUse = if (player.selectedSeedId.startsWith("seed_") &&
                    player.selectedSeedId !in listOf("seed_oak", "seed_sakura") &&
                    inventory.hasItem(player.selectedSeedId, 1)
                ) {
                    player.selectedSeedId
                } else {
                    listOf("seed_carrot", "seed_potato", "seed_tomato", "seed_strawberry", "seed_wheat", "seed_pumpkin")
                        .firstOrNull { inventory.hasItem(it, 1) } ?: player.selectedSeedId
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
                player.triggerActionAnimation(PlayerAction.WATER, 0.36f)
                val res = farming.waterPlot(ctx.targetTileX, ctx.targetTileY)
                if (res.success) {
                    audio.play(RpgSfx.WATER)
                    particles.spawnWaterDrops(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 8f, 10)
                }
                showToast(res.message)
            }

            ContextActionType.HARVEST_CROP -> {
                player.triggerActionAnimation(PlayerAction.HARVEST, 0.32f)
                val res = farming.harvestCrop(ctx.targetTileX, ctx.targetTileY)
                if (res.success && res.harvestedCrop != null) {
                    audio.play(RpgSfx.HARVEST)
                    particles.spawnHarvestSparkles(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 12f, 12)
                    val notices = progression.recordCropHarvested(res.harvestedCrop.expReward)
                    if (notices.any { it.contains("Level") }) levelUpAuraTimer = 1.8f
                    showToast(notices.firstOrNull() ?: res.message)
                    saveGameAsync(silent = true)
                } else {
                    showToast(res.message)
                }
            }

            ContextActionType.PLANT_SAPLING -> {
                player.triggerActionAnimation(PlayerAction.HOE, 0.32f)
                val treeType = if (player.selectedSeedId == "seed_sakura") TreeType.SAKURA else TreeType.OAK
                if (inventory.hasItem(player.selectedSeedId, 1) && world.plantTreeSapling(ctx.targetTileX, ctx.targetTileY, treeType)) {
                    inventory.removeItem(player.selectedSeedId, 1)
                    audio.play(RpgSfx.PLANT)
                    particles.spawnLeaves(ctx.targetTileX * 16f + 8f, ctx.targetTileY * 16f + 12f, 10, treeType == TreeType.SAKURA)
                    showToast("Menanam ${treeType.displayName} baru!")
                } else {
                    showToast("Tidak bisa menanam pohon di petak ini.")
                }
            }

            ContextActionType.INTERACT_DECOR -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.25f)
                val res = building.removeObjectAt(ctx.targetTileX, ctx.targetTileY)
                audio.play(RpgSfx.UI)
                showToast(res.message)
            }

            ContextActionType.OPEN_MERCHANT -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.22f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.MERCHANT)
            }

            ContextActionType.OPEN_SELL_BIN -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.22f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.SELL_BIN)
            }

            ContextActionType.OPEN_STORAGE -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.22f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.STORAGE)
            }

            ContextActionType.OPEN_HOUSE_UPGRADE -> {
                player.triggerActionAnimation(PlayerAction.INTERACT, 0.22f)
                audio.play(RpgSfx.UI)
                onOpenModalRequest(RpgModalRequest.HOUSE_UPGRADE)
            }

            ContextActionType.NONE -> {
                showToast("Ketuk tanah untuk berjalan, atau dekati warga, air, pohon, batu & pintu.")
            }
        }
        pushHudSnapshot()
    }

    // ==================== DIALOGUE, GIFTING, SLEEPING, CRAFTING & EATING ====================

    fun closeNpcDialogue() {
        postToGl {
            activeDialogue = NpcDialogueUiState()
            audio.play(RpgSfx.CANCEL)
            pushHudSnapshot()
        }
    }

    fun talkAgainActiveNpc() {
        postToGl {
            if (!activeDialogue.active) return@postToGl
            val res = npcSystem.talkToNpc(activeDialogue.npcId, player.x, player.y, world.timeHours)
            if (res != null) {
                activeDialogue = res.first
                audio.play(RpgSfx.TALK)
                pushHudSnapshot()
            }
        }
    }

    fun giveGiftToActiveNpc(itemId: String) {
        postToGl {
            if (!activeDialogue.active) return@postToGl
            if (!inventory.hasItem(itemId, 1)) {
                showToast("Item tersebut tidak ada di tas.")
                pushHudSnapshot()
                return@postToGl
            }
            val reaction = npcSystem.giveGift(activeDialogue.npcId, itemId)
            if (reaction != null) {
                if (reaction.accepted) {
                    inventory.removeItem(itemId, 1)
                    audio.play(RpgSfx.HARVEST)
                    val notices = progression.recordNpcGifted()
                    showToast(notices.firstOrNull() ?: reaction.toast)
                    saveGameAsync(silent = true)
                } else {
                    audio.play(RpgSfx.CANCEL)
                    showToast(reaction.toast)
                }
                activeDialogue = reaction.dialogue
                pushHudSnapshot()
            }
        }
    }

    fun openActiveNpcService() {
        postToGl {
            val d = activeDialogue
            activeDialogue = NpcDialogueUiState()
            audio.play(RpgSfx.UI)
            when {
                d.opensShop -> onOpenModalRequest(RpgModalRequest.MERCHANT)
                d.opensCooking -> onOpenModalRequest(RpgModalRequest.COOKING)
                d.opensBlacksmith -> onOpenModalRequest(RpgModalRequest.HOUSE_UPGRADE)
            }
            pushHudSnapshot()
        }
    }

    fun confirmSleep() {
        postToGl {
            sleepPromptOpen = false
            world.sleepToNextMorning()
            player.energy = player.maxEnergy
            npcSystem.onNewDay()
            npcSystem.syncSchedulesImmediately(world.timeHours)
            progression.resetDailyQuests()
            audio.play(RpgSfx.HARVEST)
            levelUpAuraTimer = 1.4f
            showToast("Selamat pagi! Hari ${world.dayCount} dimulai. Energi penuh 100% & kebun bertumbuh!")
            saveGameAsync(silent = true)
            pushHudSnapshot()
        }
    }

    fun cancelSleep() {
        postToGl {
            sleepPromptOpen = false
            audio.play(RpgSfx.CANCEL)
            pushHudSnapshot()
        }
    }

    fun eatFood(itemId: String) {
        postToGl {
            val def = RpgCatalog.itemById[itemId] ?: return@postToGl
            if (def.energyRestore <= 0) {
                showToast("${def.name} tidak bisa dimakan.")
            } else if (!inventory.hasItem(itemId, 1)) {
                showToast("${def.name} habis.")
            } else {
                inventory.removeItem(itemId, 1)
                player.restoreEnergy(def.energyRestore)
                audio.play(RpgSfx.HARVEST)
                particles.spawnHarvestSparkles(player.x, player.y + 12f, 10)
                showToast("Menyantap ${def.name} (+${def.energyRestore} Energi)!")
            }
            pushHudSnapshot()
        }
    }

    fun craftRecipe(recipeId: String) {
        postToGl {
            val res = crafting.craft(recipeId)
            if (res.success && res.recipe != null) {
                audio.play(RpgSfx.HARVEST)
                particles.spawnHarvestSparkles(player.x, player.y + 12f, 12)
                val notices = if (res.recipe.type == RecipeType.COOKING) {
                    progression.recordDishCooked(res.recipe.expReward)
                } else {
                    progression.recordItemCrafted(res.recipe.expReward)
                }
                if (notices.any { it.contains("Level") }) levelUpAuraTimer = 1.8f
                showToast(notices.firstOrNull() ?: res.message)
                saveGameAsync(silent = true)
            } else {
                audio.play(RpgSfx.CANCEL)
                showToast(res.message)
            }
            pushHudSnapshot()
        }
    }

    // ==================== FISHING, BUILDING, SHOP, QUESTS & SETTINGS ====================

    fun onFishingTap() {
        postToGl { handleFishingTapInternal() }
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
            if (notices.any { it.contains("Level") }) levelUpAuraTimer = 1.8f
            for (ach in progression.getAchievementStates()) {
                if (ach.unlocked && notices.any { it.contains(ach.def.title) }) {
                    economy.coins += ach.def.coinReward
                }
            }
            showToast(notices.firstOrNull() ?: "Dapat ${caught.name} (${caught.rarity.label}, ${caught.sellPrice} koin)!")
            saveGameAsync(silent = true)
        } else if (res.escaped) {
            audio.play(RpgSfx.CANCEL)
            showToast(fishing.toUiState().lastResultMessage)
        }
        pushHudSnapshot()
    }

    fun cancelFishing() {
        postToGl {
            fishing.cancel()
            player.clearAction()
            audio.play(RpgSfx.CANCEL)
            pushHudSnapshot()
        }
    }

    fun startConstruction(itemId: String) {
        postToGl {
            val (fx, fy) = player.getFacingTile()
            building.startBuildMode(itemId, fx, fy)
            audio.play(RpgSfx.UI)
            showToast("Ketuk petak kosong atau gerakkan karakter lalu tekan Pasang.")
            pushHudSnapshot()
        }
    }

    fun rotateConstruction() {
        postToGl {
            building.rotatePreview()
            audio.play(RpgSfx.UI)
            pushHudSnapshot()
        }
    }

    fun cancelConstruction() {
        postToGl {
            building.cancelBuildMode()
            audio.play(RpgSfx.CANCEL)
            pushHudSnapshot()
        }
    }

    fun confirmBuildPlacement() {
        postToGl { confirmBuildPlacementInternal() }
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
            audio.play(RpgSfx.CANCEL)
            showToast(res.message)
        }
        pushHudSnapshot()
    }

    fun upgradeHouse() {
        postToGl {
            val res = building.upgradeHouse()
            if (res.success && res.newHouseTier != null) {
                audio.play(RpgSfx.BUILD)
                audio.play(RpgSfx.CATCH)
                levelUpAuraTimer = 2.0f
                val notices = progression.recordHouseUpgraded(res.newHouseTier)
                for (ach in progression.getAchievementStates()) {
                    if (ach.unlocked && notices.any { it.contains(ach.def.title) }) {
                        economy.coins += ach.def.coinReward
                    }
                }
                showToast(notices.firstOrNull() ?: res.message)
                saveGameAsync(silent = true)
            } else {
                audio.play(RpgSfx.CANCEL)
                showToast(res.message)
            }
            pushHudSnapshot()
        }
    }

    fun buyShopItem(itemId: String, quantity: Int = 1) {
        postToGl {
            val res = economy.buyItem(itemId, quantity)
            audio.play(if (res.success) RpgSfx.COIN else RpgSfx.CANCEL)
            showToast(res.message)
            if (res.success) saveGameAsync(silent = true)
            pushHudSnapshot()
        }
    }

    fun sellInventoryItem(itemId: String, quantity: Int = 1) {
        postToGl {
            val res = economy.sellItem(itemId, quantity)
            if (res.success) {
                audio.play(RpgSfx.COIN)
                progression.recordCoinsEarned(res.coinsDelta)
                saveGameAsync(silent = true)
            } else {
                audio.play(RpgSfx.CANCEL)
            }
            showToast(res.message)
            pushHudSnapshot()
        }
    }

    fun sellAllFish() {
        postToGl {
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
        postToGl {
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
        postToGl {
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
        postToGl {
            val reward = progression.claimQuest(questId)
            if (reward != null) {
                economy.coins += reward.first
                audio.play(RpgSfx.CATCH)
                levelUpAuraTimer = 1.6f
                showToast("Hadiah misi diklaim: +${reward.first} koin & +${reward.second} EXP!")
                saveGameAsync(silent = true)
            }
            pushHudSnapshot()
        }
    }

    fun updateCustomization(custom: PlayerCustomization) {
        postToGl {
            player.customization = custom
            audio.play(RpgSfx.UI)
            saveGameAsync(silent = true)
            pushHudSnapshot()
        }
    }

    fun applySettings(
        sfxVol: Float,
        bgmVol: Float,
        zoom: Float,
        weatherEnabled: Boolean,
        raylightEnabled: Boolean,
        sensitivity: Float
    ) {
        audio.sfxVolume = sfxVol
        audio.bgmVolume = bgmVol
        cameraZoom = zoom
        world.weatherEnabled = weatherEnabled
        world.raylightEnabled = raylightEnabled
        player.sensitivity = sensitivity.coerceIn(0.70f, 1.40f)
        if (!weatherEnabled) world.isRaining = false
    }

    fun saveGameAsync(silent: Boolean = false) {
        if (!isSaveReady || !glAssetsReady || userUid.isBlank()) return
        val bundle = RpgSaveBundle(
            player = PlayerStateEntity(
                userUid = userUid,
                currentMapId = world.currentMapId.id,
                posX = player.x,
                posY = player.y,
                facing = player.direction.key,
                level = progression.playerLevel,
                exp = progression.exp,
                coins = economy.coins,
                energy = player.energy,
                fishingLevel = progression.fishingLevel,
                fishingExp = progression.fishingExp,
                farmingLevel = progression.farmingLevel,
                farmingExp = progression.farmingExp,
                gatheringLevel = progression.gatheringLevel,
                gatheringExp = progression.gatheringExp,
                miningLevel = progression.miningLevel,
                miningExp = progression.miningExp,
                cookingLevel = progression.cookingLevel,
                cookingExp = progression.cookingExp,
                craftingLevel = progression.craftingLevel,
                craftingExp = progression.craftingExp,
                houseTier = world.houseTier,
                gameTimeHours = world.timeHours,
                dayCount = world.dayCount,
                selectedToolId = player.selectedToolId,
                selectedSeedId = player.selectedSeedId,
                presetId = player.customization.presetId,
                outfitTintHex = player.customization.outfitTintHex,
                accentHex = player.customization.accentHex,
                discoveredRegionsCsv = world.getDiscoveredRegionsCsv(),
                openedChestsCsv = world.getOpenedChestsCsv(),
                updatedAtMs = System.currentTimeMillis()
            ),
            inventory = inventory.toEntities(userUid),
            fishCollection = progression.toFishEntities(userUid),
            trees = world.toTreeEntities(userUid),
            crops = world.toCropEntities(userUid),
            placedObjects = world.toPlacedEntities(userUid),
            quests = progression.toQuestEntities(userUid),
            achievements = progression.toAchievementEntities(userUid),
            npcFriendships = npcSystem.toEntities(userUid)
        )
        ioScope.launch {
            val cloudSynced = runCatching { saveRepo.saveGame(bundle) }.getOrDefault(false)
            postToGl {
                cloudSyncStatus = if (cloudSynced) "Tersinkron ke Database Akun" else "Tersimpan Lokal (Offline)"
                if (!silent) {
                    audio.play(RpgSfx.UI)
                    showToast(
                        if (cloudSynced) "Progres berhasil disimpan ke database akun!"
                        else "Progres disimpan lokal (akan sinkron saat online)."
                    )
                }
                pushHudSnapshot()
            }
        }
    }

    private fun showToast(msg: String) {
        if (msg.isBlank()) return
        toastMessage = msg
        toastCounter++
    }

    private fun pushHudSnapshot() {
        val currentReg = world.getRegionAt(world.currentMapId, player.getTileX(), player.getTileY())
        _hudState.value = RpgHudSnapshot(
            coins = economy.coins,
            energy = player.energy,
            maxEnergy = player.maxEnergy,
            playerLevel = progression.playerLevel,
            exp = progression.exp,
            nextLevelExp = RpgCatalog.expRequiredForLevel(progression.playerLevel),
            fishingLevel = progression.fishingLevel,
            farmingLevel = progression.farmingLevel,
            gatheringLevel = progression.gatheringLevel,
            miningLevel = progression.miningLevel,
            cookingLevel = progression.cookingLevel,
            craftingLevel = progression.craftingLevel,
            houseTier = world.houseTier,
            currentMapId = world.currentMapId,
            currentRegion = currentReg,
            discoveredRegions = world.discoveredRegions.toSet(),
            playerTileX = player.getTileX(),
            playerTileY = player.getTileY(),
            timeHours = world.timeHours,
            dayCount = world.dayCount,
            isRaining = world.isRaining,
            isRunning = player.isRunning,
            isAutoWalking = player.isAutoWalking,
            selectedToolId = player.selectedToolId,
            selectedSeedId = player.selectedSeedId,
            contextAction = if (glAssetsReady) player.detectContextAction(world, inventory, npcSystem) else _hudState.value.contextAction,
            fishingState = fishing.toUiState(),
            buildPreview = building.toPreviewState(),
            dialogueState = activeDialogue,
            sleepPromptOpen = sleepPromptOpen,
            inventory = inventory.getStacks(sortMode),
            caughtFishCounts = progression.getCaughtFishCounts(),
            npcSummaries = npcSystem.getSummaries(),
            quests = progression.getQuestStates(),
            achievements = progression.getAchievementStates(),
            customization = player.customization,
            toastMessage = toastMessage,
            toastId = toastCounter,
            accountName = userDisplayName,
            cloudSyncStatus = cloudSyncStatus,
            fatalErrorMessage = fatalErrorMessage
        )
    }

    override fun pause() {
        runCatching {
            audio.pauseBgm()
            saveGameAsync(silent = true)
        }
    }

    override fun resume() {
        runCatching {
            audio.resumeBgm()
        }
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        glAssetsReady = false
        runCatching { mapRenderer?.dispose() }
        mapRenderer = null
        runCatching { batch?.dispose() }
        batch = null
        camera = null
        runCatching { world.dispose() }
        runCatching { assets.dispose() }
        runCatching { audio.dispose() }
        runCatching { ioScope.cancel() }
    }
}
