package com.yuranime.dinz.rpg.player

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.ContextualAction
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.PlayerAction
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.world.HomesteadWorld
import kotlin.math.abs
import kotlin.math.hypot

class PlayerController {
    var x: Float = 144f + 8f
    var y: Float = 224f + 8f
    var direction: Direction = Direction.DOWN
    var action: PlayerAction = PlayerAction.IDLE
    var stateTime: Float = 0f
    var isRunning: Boolean = false
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

    fun update(dt: Float, world: HomesteadWorld, isFishingActive: Boolean, onFootstep: () -> Unit) {
        stateTime += dt

        if (isFishingActive) return

        if (actionLockTimer > 0f) {
            actionLockTimer = (actionLockTimer - dt).coerceAtLeast(0f)
            if (actionLockTimer <= 0f) {
                action = PlayerAction.IDLE
                stateTime = 0f
            }
            return
        }

        val ix = joyX
        val iy = joyY
        val mag = hypot(ix.toDouble(), iy.toDouble()).toFloat()

        if (mag > 0.12f) {
            val nx = ix / mag
            val ny = iy / mag
            val clampedMag = mag.coerceAtMost(1f)
            val speed = (if (isRunning) 88f else 56f) * clampedMag

            direction = if (abs(nx) > abs(ny)) {
                if (nx > 0f) Direction.RIGHT else Direction.LEFT
            } else {
                if (ny > 0f) Direction.UP else Direction.DOWN
            }

            // Axis-separated AABB collision sliding for butter-smooth movement along walls/fences/water!
            val nextX = (x + nx * speed * dt).coerceIn(20f, HomesteadWorld.WORLD_W - 20f)
            if (!world.isBlockedAtWorldPos(nextX, y)) {
                x = nextX
            }
            val nextY = (y + ny * speed * dt).coerceIn(20f, HomesteadWorld.WORLD_H - 20f)
            if (!world.isBlockedAtWorldPos(x, nextY)) {
                y = nextY
            }

            val moveAction = if (isRunning) PlayerAction.RUN else PlayerAction.WALK
            if (action != moveAction) {
                action = moveAction
                stateTime = 0f
            }

            stepSoundTimer -= dt
            if (stepSoundTimer <= 0f) {
                onFootstep()
                stepSoundTimer = if (isRunning) 0.24f else 0.36f
            }
        } else {
            if (action != PlayerAction.IDLE) {
                action = PlayerAction.IDLE
                stateTime = 0f
            }
            stepSoundTimer = 0.08f
        }
    }

    /**
     * Scans facing tile and adjacent 1-tile ring to provide intelligent, dynamic contextual actions.
     */
    fun detectContextAction(world: HomesteadWorld, inventory: InventorySystem): ContextualAction {
        val (fx, fy) = getFacingTile()
        val px = getTileX()
        val py = getTileY()

        // Candidate tiles prioritized: facing tile first, then current tile, then orthogonal neighbors
        val candidates = listOf(
            fx to fy,
            px to py,
            px to (py + 1),
            px to (py - 1),
            (px - 1) to py,
            (px + 1) to py
        ).distinct()

        // 1. Check nearby NPC / Station landmarks (Merchant, Selling Bin, Storage Chest, House Door)
        for ((cx, cy) in candidates) {
            if (cx in (HomesteadWorld.MERCHANT_TILE_X - 1)..(HomesteadWorld.MERCHANT_TILE_X + 2) &&
                cy in (HomesteadWorld.MERCHANT_TILE_Y - 1)..(HomesteadWorld.MERCHANT_TILE_Y + 1)
            ) {
                return ContextualAction(ContextActionType.OPEN_MERCHANT, "Toko Merchant", cx, cy)
            }
            if (cx == HomesteadWorld.SELL_BIN_TILE_X && cy == HomesteadWorld.SELL_BIN_TILE_Y) {
                return ContextualAction(ContextActionType.OPEN_SELL_BIN, "Jual Hasil", cx, cy)
            }
            if (cx == HomesteadWorld.STORAGE_TILE_X && cy == HomesteadWorld.STORAGE_TILE_Y) {
                return ContextualAction(ContextActionType.OPEN_STORAGE, "Peti Simpan", cx, cy)
            }
            if (cx in (HomesteadWorld.HOUSE_DOOR_X - 1)..(HomesteadWorld.HOUSE_DOOR_X + 1) &&
                cy in HomesteadWorld.HOUSE_DOOR_Y..(HomesteadWorld.HOUSE_DOOR_Y + 1)
            ) {
                return ContextualAction(ContextActionType.OPEN_HOUSE_UPGRADE, "Upgrade Rumah", cx, cy)
            }
        }

        // 2. Check facing tile first for Water (Fishing), Trees (Chopping), Crops/Soil (Farming)
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

        // Check farm tiles (facing first, then standing tile)
        for ((cx, cy) in listOf(fx to fy, px to py)) {
            if (world.isFarmableTile(cx, cy)) {
                val plot = world.cropPlots["${cx}_${cy}"]
                return when {
                    plot == null || !plot.isTilled -> ContextualAction(ContextActionType.TILL_SOIL, "Cangkul Tanah", cx, cy)
                    plot.isHarvestable -> ContextualAction(ContextActionType.HARVEST_CROP, "Panen ${plot.cropType?.displayName ?: "Sayur"}", cx, cy)
                    plot.cropType == null -> {
                        val seedName = RpgCatalog.itemById[selectedSeedId]?.name?.removePrefix("Bibit ") ?: "Bibit"
                        ContextualAction(ContextActionType.PLANT_SEED, "Tanam $seedName", cx, cy)
                    }
                    !plot.isWatered -> ContextualAction(ContextActionType.WATER_CROP, "Siram Tanaman", cx, cy)
                    else -> ContextualAction(ContextActionType.WATER_CROP, "Tumbuh (${plot.stage}/4)", cx, cy)
                }
            }
        }

        // 3. Check adjacent tiles for water / tree if player isn't facing directly at it
        for ((cx, cy) in candidates) {
            if (world.isWaterTile(cx, cy)) {
                return ContextualAction(ContextActionType.FISH, "Pancing", cx, cy, habitat = world.getWaterHabitat(cx, cy))
            }
            val adjTree = world.getTreeAt(cx, cy)
            if (adjTree != null && adjTree.state != TreeHealthState.STUMP && adjTree.state != TreeHealthState.FALLING) {
                return ContextualAction(ContextActionType.CHOP_TREE, "Tebang ${adjTree.type.displayName}", cx, cy, adjTree.id)
            }
        }

        // 4. Check placed objects (pick up / interact)
        val placed = world.getPlacedObjectAt(fx, fy)
        if (placed != null) {
            val name = RpgCatalog.itemById[placed.itemId]?.name ?: "Dekorasi"
            return ContextualAction(ContextActionType.INTERACT_DECOR, "Ambil $name", fx, fy, placed.id)
        }

        // 5. If player holds a tree sapling (Oak/Sakura) on open grass, offer Plant Tree!
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
