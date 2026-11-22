package com.yuranime.dinz.rpg.player

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.ContextualAction
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.npc.NpcSystem
import com.yuranime.dinz.rpg.world.HomesteadWorld
import kotlin.math.abs
import kotlin.math.hypot

class PlayerController {
    var x: Float = 14f * HomesteadWorld.TILE_SIZE + 8f
    var y: Float = 43f * HomesteadWorld.TILE_SIZE + 8f
    var direction: Direction = Direction.DOWN
    var action: PlayerAction = PlayerAction.IDLE
    var stateTime: Float = 0f
    var isRunning: Boolean = false
    var energy: Int = 100
    val maxEnergy: Int = 100
    var selectedToolId: String = "tool_rod_basic"
    var selectedSeedId: String = "seed_carrot"
    var customization: PlayerCustomization = PlayerCustomization()

    private var actionLockTimer: Float = 0f
    private var stepSoundTimer: Float = 0f

    // Joystick input vector (-1..1) set from Compose touch UI thread
    @Volatile
    var joyX: Float = 0f
    @Volatile
    var joyY: Float = 0f
    @Volatile
    var sensitivity: Float = 1.0f

    // Tap-to-Move A* Pathfinding state
    val autoPath: MutableList<Pair<Int, Int>> = mutableListOf()
    var pendingTapAction: ContextualAction? = null
        private set
    var tapMarkerTile: Pair<Int, Int>? = null
        private set
    var tapMarkerTimer: Float = 0f
        private set
    private var stuckTimer: Float = 0f

    val isAutoWalking: Boolean get() = autoPath.isNotEmpty()

    fun consumeEnergy(cost: Int): Boolean {
        if (cost <= 0) return true
        energy = (energy - cost).coerceAtLeast(0)
        return true
    }

    fun restoreEnergy(amount: Int) {
        if (amount <= 0) return
        energy = (energy + amount).coerceAtMost(maxEnergy)
    }

    fun setAutoPath(
        path: List<Pair<Int, Int>>,
        pendingAction: ContextualAction? = null,
        markerTile: Pair<Int, Int>? = null
    ) {
        autoPath.clear()
        autoPath.addAll(path)
        pendingTapAction = pendingAction
        tapMarkerTile = markerTile ?: path.lastOrNull()
        tapMarkerTimer = 1.6f
        stuckTimer = 0f
    }

    fun cancelAutoPath() {
        autoPath.clear()
        pendingTapAction = null
        stuckTimer = 0f
    }

    fun triggerActionAnimation(newAction: PlayerAction, duration: Float) {
        action = newAction
        stateTime = 0f
        actionLockTimer = duration
    }

    fun setFishingAnimation(fishAction: PlayerAction) {
        if (action != fishAction) {
            action = fishAction
            stateTime = 0f
        }
        actionLockTimer = 0f
    }

    fun clearAction() {
        action = PlayerAction.IDLE
        actionLockTimer = 0f
    }

    fun faceTile(tx: Int, ty: Int) {
        val wx = tx * HomesteadWorld.TILE_SIZE + 8f
        val wy = ty * HomesteadWorld.TILE_SIZE + 8f
        val dx = wx - x
        val dy = wy - y
        if (abs(dx) < 0.5f && abs(dy) < 0.5f) return
        direction = if (abs(dx) > abs(dy)) {
            if (dx > 0f) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0f) Direction.UP else Direction.DOWN
        }
    }

    fun getTileX(): Int = (x / HomesteadWorld.TILE_SIZE).toInt()
    fun getTileY(): Int = (y / HomesteadWorld.TILE_SIZE).toInt()

    fun getFacingTile(): Pair<Int, Int> {
        val tx = getTileX()
        val ty = getTileY()
        return when (direction) {
            Direction.UP -> tx to (ty + 1)
            Direction.DOWN -> tx to (ty - 1)
            Direction.LEFT -> (tx - 1) to ty
            Direction.RIGHT -> (tx + 1) to ty
        }
    }

    fun update(
        dt: Float,
        world: HomesteadWorld,
        playerLevel: Int,
        isFishingActive: Boolean,
        isDialogueActive: Boolean,
        onFootstep: () -> Unit,
        onBlockedRegion: (String) -> Unit,
        onPendingActionReached: (ContextualAction) -> Unit
    ) {
        stateTime += dt
        if (tapMarkerTimer > 0f) {
            tapMarkerTimer = (tapMarkerTimer - dt).coerceAtLeast(0f)
            if (tapMarkerTimer <= 0f && autoPath.isEmpty()) tapMarkerTile = null
        }

        if (isFishingActive || isDialogueActive) {
            cancelAutoPath()
            return
        }

        if (actionLockTimer > 0f) {
            actionLockTimer = (actionLockTimer - dt).coerceAtLeast(0f)
            if (actionLockTimer <= 0f) {
                action = PlayerAction.IDLE
                stateTime = 0f
            }
            return
        }

        val ix = joyX * sensitivity
        val iy = joyY * sensitivity
        val mag = hypot(ix.toDouble(), iy.toDouble()).toFloat()

        if (mag > 0.12f) {
            // Manual joystick overrides any active A* tap-to-move path immediately!
            if (autoPath.isNotEmpty() || pendingTapAction != null) cancelAutoPath()
            val nx = ix / mag
            val ny = iy / mag
            val clampedMag = mag.coerceAtMost(1f)
            val speed = (if (isRunning) 94f else 62f) * clampedMag

            direction = if (abs(nx) > abs(ny)) {
                if (nx > 0f) Direction.RIGHT else Direction.LEFT
            } else {
                if (ny > 0f) Direction.UP else Direction.DOWN
            }

            moveWithCollision(nx * speed * dt, ny * speed * dt, world, playerLevel, onBlockedRegion)
            setMovingAnim(dt, onFootstep)
            return
        }

        // Follow A* Tap-to-Move Path
        if (autoPath.isNotEmpty()) {
            val (targetTx, targetTy) = autoPath.first()
            val targetWx = targetTx * HomesteadWorld.TILE_SIZE + 8f
            val targetWy = targetTy * HomesteadWorld.TILE_SIZE + 8f
            val dx = targetWx - x
            val dy = targetWy - y
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (dist <= 2.2f) {
                x = targetWx
                y = targetWy
                autoPath.removeAt(0)
                stuckTimer = 0f
                if (autoPath.isEmpty()) {
                    val act = pendingTapAction
                    pendingTapAction = null
                    tapMarkerTile = null
                    action = PlayerAction.IDLE
                    stateTime = 0f
                    if (act != null) {
                        faceTile(act.targetTileX, act.targetTileY)
                        onPendingActionReached(act)
                    }
                }
            } else {
                val nx = dx / dist
                val ny = dy / dist
                val speed = if (isRunning) 94f else 68f
                val step = (speed * dt).coerceAtMost(dist)
                direction = if (abs(nx) > abs(ny)) {
                    if (nx > 0f) Direction.RIGHT else Direction.LEFT
                } else {
                    if (ny > 0f) Direction.UP else Direction.DOWN
                }
                val oldX = x
                val oldY = y
                moveWithCollision(nx * step, ny * step, world, playerLevel, onBlockedRegion)
                if (hypot((x - oldX).toDouble(), (y - oldY).toDouble()) < 0.05) {
                    stuckTimer += dt
                    if (stuckTimer > 0.35f) cancelAutoPath()
                } else {
                    stuckTimer = 0f
                }
                setMovingAnim(dt, onFootstep)
            }
            return
        }

        // If player tapped an adjacent target with zero walk path, execute right away!
        val immediate = pendingTapAction
        if (immediate != null) {
            pendingTapAction = null
            faceTile(immediate.targetTileX, immediate.targetTileY)
            onPendingActionReached(immediate)
            return
        }

        if (action != PlayerAction.IDLE) {
            action = PlayerAction.IDLE
            stateTime = 0f
        }
        stepSoundTimer = 0.08f
    }

    private fun setMovingAnim(dt: Float, onFootstep: () -> Unit) {
        val moveAction = if (isRunning) PlayerAction.RUN else PlayerAction.WALK
        if (action != moveAction) {
            action = moveAction
            stateTime = 0f
        }
        stepSoundTimer -= dt
        if (stepSoundTimer <= 0f) {
            onFootstep()
            stepSoundTimer = if (isRunning) 0.23f else 0.34f
        }
    }

    private fun moveWithCollision(
        dx: Float,
        dy: Float,
        world: HomesteadWorld,
        playerLevel: Int,
        onBlockedRegion: (String) -> Unit
    ) {
        val minX = 16f
        val maxX = (world.worldW - 16f).coerceAtLeast(minX + 1f)
        val minY = 6f
        val maxY = (world.worldH - 16f).coerceAtLeast(minY + 1f)

        val nextX = (x + dx).coerceIn(minX, maxX)
        if (!world.isBlockedAtWorldPos(nextX, y, playerLevel)) {
            x = nextX
        } else {
            checkLockedRegionHint((nextX / HomesteadWorld.TILE_SIZE).toInt(), getTileY(), world, playerLevel, onBlockedRegion)
        }

        val nextY = (y + dy).coerceIn(minY, maxY)
        if (!world.isBlockedAtWorldPos(x, nextY, playerLevel)) {
            y = nextY
        } else {
            checkLockedRegionHint(getTileX(), (nextY / HomesteadWorld.TILE_SIZE).toInt(), world, playerLevel, onBlockedRegion)
        }
    }

    private fun checkLockedRegionHint(
        tx: Int,
        ty: Int,
        world: HomesteadWorld,
        playerLevel: Int,
        onBlockedRegion: (String) -> Unit
    ) {
        if (world.currentMapId.isIndoor) return
        val reg = world.getRegionAt(world.currentMapId, tx, ty)
        if (!world.isRegionUnlocked(reg, playerLevel)) {
            onBlockedRegion("Butuh Level ${reg.requiredLevel} untuk membuka ${reg.displayName}!")
        }
    }

    fun detectContextAction(
        world: HomesteadWorld,
        inventory: InventorySystem,
        npcSystem: NpcSystem
    ): ContextualAction {
        val (fx, fy) = getFacingTile()
        val px = getTileX()
        val py = getTileY()

        val candidates = listOf(
            fx to fy,
            px to py,
            px to (py + 1),
            px to (py - 1),
            (px - 1) to py,
            (px + 1) to py
        ).distinct()

        // 1. Living Village NPC nearby
        val nearNpc = npcSystem.findNpcAtOrNearTile(world.currentMapId, fx, fy, 1)
            ?: npcSystem.findNpcAtOrNearTile(world.currentMapId, px, py, 1)
        if (nearNpc != null) {
            return ContextualAction(
                type = ContextActionType.TALK_NPC,
                label = "Bicara ${nearNpc.def.name}",
                targetTileX = nearNpc.tileX,
                targetTileY = nearNpc.tileY,
                targetId = nearNpc.def.id
            )
        }

        // 2. Doors & Cave Portals
        for ((cx, cy) in candidates) {
            val portal = world.getPortalAt(cx, cy)
            if (portal != null) {
                return ContextualAction(
                    type = ContextActionType.ENTER_PORTAL,
                    label = portal.label,
                    targetTileX = cx,
                    targetTileY = cy,
                    targetId = portal.targetMap.id
                )
            }
        }

        // 3. Interactive Building/Homestead Stations (Bed, Stove, Workbench, Storage, Sell Bin, Shop, Blacksmith)
        for ((cx, cy) in candidates) {
            val st = world.getStationAt(cx, cy)
            if (st != null) {
                return ContextualAction(st.actionType, st.label, cx, cy)
            }
        }

        // 4. Treasure Chests & Wild Forageables
        for ((cx, cy) in candidates) {
            val chest = world.getChestAt(cx, cy)
            if (chest != null) {
                return ContextualAction(ContextActionType.OPEN_CHEST, "Buka ${chest.label}", cx, cy, chest.id)
            }
            val forage = world.getForageAt(cx, cy)
            if (forage != null) {
                val fname = RpgCatalog.itemById[forage.itemId]?.name ?: "Hasil Alam"
                return ContextualAction(ContextActionType.GATHER_FORAGE, "Ambil $fname", cx, cy, forage.id)
            }
        }

        // 5. Facing Water (Fishing), Trees (Chopping), Ore Rocks (Mining)
        if (world.isWaterTile(fx, fy)) {
            return ContextualAction(
                type = ContextActionType.FISH,
                label = "Pancing",
                targetTileX = fx,
                targetTileY = fy,
                habitat = world.getWaterHabitat(fx, fy)
            )
        }

        val facingTree = world.getTreeAt(fx, fy)
        if (facingTree != null && facingTree.state != TreeHealthState.STUMP && facingTree.state != TreeHealthState.FALLING) {
            return ContextualAction(ContextActionType.CHOP_TREE, "Tebang ${facingTree.type.displayName}", fx, fy, facingTree.id)
        }

        val facingOre = world.getOreAt(fx, fy)
        if (facingOre != null) {
            return ContextualAction(ContextActionType.MINE_ORE, "Tambang ${facingOre.type.displayName}", fx, fy, facingOre.id)
        }

        // 6. Farmable Tiles
        for ((cx, cy) in listOf(fx to fy, px to py)) {
            if (world.isFarmableTile(cx, cy)) {
                val plot = world.cropPlots["${cx}_${cy}"]
                return when {
                    plot == null || !plot.isTilled -> ContextualAction(ContextActionType.TILL_SOIL, "Cangkul Tanah", cx, cy)
                    plot.isHarvestable -> ContextualAction(ContextActionType.HARVEST_CROP, "Panen ${plot.cropType?.displayName ?: "Tanaman"}", cx, cy)
                    plot.cropType == null -> {
                        val seedName = RpgCatalog.itemById[selectedSeedId]?.name?.removePrefix("Bibit ") ?: "Bibit"
                        ContextualAction(ContextActionType.PLANT_SEED, "Tanam $seedName", cx, cy)
                    }
                    !plot.isWatered -> ContextualAction(ContextActionType.WATER_CROP, "Siram Tanaman", cx, cy)
                    else -> ContextualAction(ContextActionType.WATER_CROP, "Tumbuh (${plot.stage}/4)", cx, cy)
                }
            }
        }

        // 7. Adjacent Water, Trees, or Ores
        for ((cx, cy) in candidates) {
            if (world.isWaterTile(cx, cy)) {
                return ContextualAction(ContextActionType.FISH, "Pancing", cx, cy, habitat = world.getWaterHabitat(cx, cy))
            }
            val adjTree = world.getTreeAt(cx, cy)
            if (adjTree != null && adjTree.state != TreeHealthState.STUMP && adjTree.state != TreeHealthState.FALLING) {
                return ContextualAction(ContextActionType.CHOP_TREE, "Tebang ${adjTree.type.displayName}", cx, cy, adjTree.id)
            }
            val adjOre = world.getOreAt(cx, cy)
            if (adjOre != null) {
                return ContextualAction(ContextActionType.MINE_ORE, "Tambang ${adjOre.type.displayName}", cx, cy, adjOre.id)
            }
        }

        // 8. Placed Decor / Furniture Pickup
        val placed = world.getPlacedObjectAt(fx, fy)
        if (placed != null) {
            val name = RpgCatalog.itemById[placed.itemId]?.name ?: "Furnitur"
            return ContextualAction(ContextActionType.INTERACT_DECOR, "Ambil $name", fx, fy, placed.id)
        }

        // 9. Tree Sapling Planting on open grass
        if ((selectedSeedId == "seed_oak" || selectedSeedId == "seed_sakura") &&
            inventory.hasItem(selectedSeedId, 1) &&
            world.isGrassPlantableForTree(fx, fy)
        ) {
            val name = if (selectedSeedId == "seed_sakura") "Sakura" else "Ek"
            return ContextualAction(ContextActionType.PLANT_SAPLING, "Tanam Pohon $name", fx, fy)
        }

        return ContextualAction(ContextActionType.NONE, "")
    }
}
