package com.yuranime.dinz.rpg.world

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.yuranime.dinz.rpg.model.CropPlot
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.PlacedObject
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.model.WaterHabitat
import com.yuranime.dinz.rpg.model.WorldTree
import com.yuranime.dinz.rpg.persistence.CropPlotEntity
import com.yuranime.dinz.rpg.persistence.PlacedObjectEntity
import com.yuranime.dinz.rpg.persistence.TreeStateEntity
import kotlin.random.Random

class HomesteadWorld {
    companion object {
        const val TILE_SIZE = 16f
        const val MAP_TILES_W = 30
        const val MAP_TILES_H = 24
        const val WORLD_W = MAP_TILES_W * TILE_SIZE
        const val WORLD_H = MAP_TILES_H * TILE_SIZE

        // Landmarks in LibGDX bottom-up tile coordinates (gdxY = 23 - tiledY)
        const val HOUSE_TILE_X = 7
        const val HOUSE_TILE_Y = 17
        const val HOUSE_DOOR_X = 9
        const val HOUSE_DOOR_Y = 16

        const val STORAGE_TILE_X = 5
        const val STORAGE_TILE_Y = 17

        const val SELL_BIN_TILE_X = 12
        const val SELL_BIN_TILE_Y = 17

        const val MERCHANT_TILE_X = 20
        const val MERCHANT_TILE_Y = 18

        // Farmable area: Tiled x=14..20, ty=14..18 -> LibGDX x=14..20, y=5..9
        val FARM_X_RANGE = 14..20
        val FARM_Y_RANGE = 5..9
    }

    lateinit var tiledMap: TiledMap
        private set
    private var collisionLayer: TiledMapTileLayer? = null
    private var waterLayer: TiledMapTileLayer? = null
    private var groundLayer: TiledMapTileLayer? = null
    private var decorLayer: TiledMapTileLayer? = null
    private var treesObjLayer: TiledMapTileLayer? = null

    val trees = mutableListOf<WorldTree>()
    val cropPlots = linkedMapOf<String, CropPlot>()
    val placedObjects = mutableListOf<PlacedObject>()

    // Dynamic animated tile coordinates cached from TMX for live water/grass/flower animation
    val waterTiles = mutableListOf<Pair<Int, Int>>()
    val flowerTiles = mutableListOf<Pair<Int, Int>>()
    val swayGrassTiles = mutableListOf<Pair<Int, Int>>()

    var houseTier: Int = 1
    var timeHours: Float = 8.5f // 0..24
    var dayCount: Int = 1
    var isRaining: Boolean = false
    var weatherEnabled: Boolean = true
    private var weatherTimer: Float = 95f

    fun loadMap() {
        tiledMap = TmxMapLoader().load("rpg/maps/starter_homestead.tmx")
        groundLayer = tiledMap.layers.get("ground") as? TiledMapTileLayer
        decorLayer = tiledMap.layers.get("ground_decor") as? TiledMapTileLayer
        waterLayer = tiledMap.layers.get("water") as? TiledMapTileLayer
        treesObjLayer = tiledMap.layers.get("trees_objects") as? TiledMapTileLayer
        collisionLayer = tiledMap.layers.get("collision") as? TiledMapTileLayer

        // Cache animated water, flowers, and grass tuft coordinates
        waterTiles.clear()
        flowerTiles.clear()
        swayGrassTiles.clear()
        for (y in 0 until MAP_TILES_H) {
            for (x in 0 until MAP_TILES_W) {
                val wCell = waterLayer?.getCell(x, y)
                val objCell = treesObjLayer?.getCell(x, y)
                if (wCell?.tile != null && wCell.tile.id == 17 && objCell == null) {
                    waterTiles.add(x to y)
                }
                val dCell = decorLayer?.getCell(x, y)
                if (dCell?.tile != null) {
                    val tid = dCell.tile.id
                    if (tid in 33..36) flowerTiles.add(x to y)
                    if (tid == 39) swayGrassTiles.add(x to y)
                }
            }
        }

        if (trees.isEmpty()) {
            initDefaultTrees()
        }
        initDefaultFarmPlots()
    }

    fun initDefaultTrees() {
        trees.clear()
        val spots = listOf(
            Triple(3, 21, TreeType.OAK),
            Triple(5, 21, TreeType.PINE),
            Triple(14, 21, TreeType.SAKURA),
            Triple(18, 21, TreeType.PINE),
            Triple(22, 21, TreeType.OAK),
            Triple(2, 15, TreeType.OAK),
            Triple(2, 11, TreeType.SAKURA),
            Triple(13, 18, TreeType.OAK),
            Triple(22, 10, TreeType.SAKURA),
            Triple(22, 4, TreeType.OAK),
            Triple(15, 2, TreeType.PINE),
            Triple(19, 2, TreeType.OAK),
            Triple(28, 19, TreeType.PINE),
            Triple(28, 6, TreeType.OAK)
        )
        spots.forEachIndexed { idx, (tx, ty, type) ->
            if (!isMapSolidTile(tx, ty) && !isWaterTile(tx, ty)) {
                trees.add(WorldTree(id = "tree_$idx", tileX = tx, tileY = ty, type = type))
            }
        }
    }

    private fun initDefaultFarmPlots() {
        // Starter cozy garden: 2 pre-tilled sprouts so the farm looks alive immediately!
        if (cropPlots.isEmpty()) {
            val starterA = CropPlot(15, 8, isTilled = true, isWatered = true, cropType = CropType.CARROT, stage = 2, growthProgress = 0.5f)
            val starterB = CropPlot(16, 8, isTilled = true, isWatered = true, cropType = CropType.TOMATO, stage = 1, growthProgress = 0.2f)
            val starterC = CropPlot(17, 8, isTilled = true, isWatered = false, cropType = null, stage = 0, growthProgress = 0f)
            cropPlots[starterA.key] = starterA
            cropPlots[starterB.key] = starterB
            cropPlots[starterC.key] = starterC
        }
    }

    fun loadSavedWorld(
        savedHouseTier: Int,
        savedTimeHours: Float,
        savedDayCount: Int,
        treeEntities: List<TreeStateEntity>,
        cropEntities: List<CropPlotEntity>,
        placedEntities: List<PlacedObjectEntity>
    ) {
        houseTier = savedHouseTier.coerceIn(1, 3)
        timeHours = savedTimeHours.coerceIn(0f, 23.99f)
        dayCount = savedDayCount.coerceAtLeast(1)

        if (treeEntities.isNotEmpty()) {
            trees.clear()
            treeEntities.forEach { e ->
                val type = TreeType.entries.find { it.id == e.treeType } ?: TreeType.OAK
                val state = runCatching { TreeHealthState.valueOf(e.state) }.getOrDefault(TreeHealthState.HEALTHY)
                trees.add(
                    WorldTree(
                        id = e.treeId,
                        tileX = e.tileX,
                        tileY = e.tileY,
                        type = type,
                        health = e.health.coerceIn(0, type.maxHealth),
                        state = if (state == TreeHealthState.FALLING) TreeHealthState.STUMP else state,
                        regrowTimer = e.regrowTimer
                    )
                )
            }
        }

        if (cropEntities.isNotEmpty()) {
            cropPlots.clear()
            cropEntities.forEach { e ->
                val ct = e.cropType?.let { cid -> CropType.entries.find { it.id == cid } }
                val plot = CropPlot(
                    tileX = e.tileX,
                    tileY = e.tileY,
                    isTilled = e.isTilled,
                    isWatered = e.isWatered,
                    cropType = ct,
                    stage = e.stage.coerceIn(0, 4),
                    growthProgress = e.growthProgress
                )
                cropPlots[plot.key] = plot
            }
        }

        placedObjects.clear()
        placedEntities.forEach { e ->
            placedObjects.add(PlacedObject(e.objectId, e.itemId, e.tileX, e.tileY, e.rotation))
        }
    }

    fun update(dt: Float, onTreeRegrown: (WorldTree) -> Unit = {}) {
        // Configurable cozy in-game clock: 1 real minute ≈ 2 in-game hours (12 min full day/night)
        timeHours += dt * (2f / 60f)
        if (timeHours >= 24f) {
            timeHours -= 24f
            dayCount++
        }

        // Dynamic cozy rain weather cycle
        if (weatherEnabled) {
            weatherTimer -= dt
            if (weatherTimer <= 0f) {
                isRaining = !isRaining
                weatherTimer = if (isRaining) 32f + Random.nextFloat() * 20f else 90f + Random.nextFloat() * 60f
            }
        } else {
            isRaining = false
        }

        // Tree shake, falling, and natural regeneration
        for (tree in trees) {
            if (tree.shakeTimer > 0f) {
                tree.shakeTimer = (tree.shakeTimer - dt).coerceAtLeast(0f)
            }
            if (tree.state == TreeHealthState.FALLING) {
                tree.fallTimer += dt
                if (tree.fallTimer >= 0.58f) {
                    tree.state = TreeHealthState.STUMP
                    tree.regrowTimer = tree.type.regrowSeconds
                }
            } else if (tree.state == TreeHealthState.STUMP) {
                tree.regrowTimer -= dt
                if (tree.regrowTimer <= 0f) {
                    tree.state = TreeHealthState.HEALTHY
                    tree.health = tree.type.maxHealth
                    tree.regrowTimer = 0f
                    onTreeRegrown(tree)
                }
            }
        }
    }

    fun isInBounds(tileX: Int, tileY: Int): Boolean {
        return tileX in 1 until (MAP_TILES_W - 1) && tileY in 1 until (MAP_TILES_H - 1)
    }

    fun isMapSolidTile(tileX: Int, tileY: Int): Boolean {
        if (!isInBounds(tileX, tileY)) return true
        val cell = collisionLayer?.getCell(tileX, tileY)
        return cell?.tile != null && cell.tile.id > 0
    }

    fun isWaterTile(tileX: Int, tileY: Int): Boolean {
        if (tileX !in 0 until MAP_TILES_W || tileY !in 0 until MAP_TILES_H) return false
        val wCell = waterLayer?.getCell(tileX, tileY)
        val bridgeOrDock = treesObjLayer?.getCell(tileX, tileY)
        // Bridges (15,16) and dock planks (13) are walkable surfaces above water
        val isWalkableDeck = bridgeOrDock?.tile?.id in listOf(13, 15, 16)
        return wCell?.tile != null && wCell.tile.id > 0 && !isWalkableDeck
    }

    fun getWaterHabitat(tileX: Int, tileY: Int): WaterHabitat {
        return if (tileX >= 22) WaterHabitat.RIVER else WaterHabitat.POND
    }

    fun isFarmableTile(tileX: Int, tileY: Int): Boolean {
        return tileX in FARM_X_RANGE && tileY in FARM_Y_RANGE
    }

    fun isGrassPlantableForTree(tileX: Int, tileY: Int): Boolean {
        if (!isInBounds(tileX, tileY)) return false
        if (isMapSolidTile(tileX, tileY) || isWaterTile(tileX, tileY) || isFarmableTile(tileX, tileY)) return false
        if (getTreeAt(tileX, tileY) != null || getPlacedObjectAt(tileX, tileY) != null) return false
        val gId = groundLayer?.getCell(tileX, tileY)?.tile?.id ?: 0
        // Grass tile IDs are 1..5
        return gId in 1..5
    }

    fun getTreeAt(tileX: Int, tileY: Int): WorldTree? {
        return trees.find { it.tileX == tileX && it.tileY == tileY }
    }

    fun getPlacedObjectAt(tileX: Int, tileY: Int): PlacedObject? {
        return placedObjects.find { it.tileX == tileX && it.tileY == tileY }
    }

    fun isBlockedForWalking(tileX: Int, tileY: Int): Boolean {
        if (isMapSolidTile(tileX, tileY)) return true
        val tree = getTreeAt(tileX, tileY)
        if (tree != null && tree.state != TreeHealthState.STUMP) return true
        if (getPlacedObjectAt(tileX, tileY) != null) return true
        return false
    }

    fun isBlockedAtWorldPos(worldX: Float, worldY: Float, halfW: Float = 4.5f, halfH: Float = 3.5f): Boolean {
        val corners = arrayOf(
            (worldX - halfW) to (worldY - halfH),
            (worldX + halfW) to (worldY - halfH),
            (worldX - halfW) to (worldY + halfH),
            (worldX + halfW) to (worldY + halfH)
        )
        for ((cx, cy) in corners) {
            val tx = (cx / TILE_SIZE).toInt()
            val ty = (cy / TILE_SIZE).toInt()
            if (isBlockedForWalking(tx, ty)) return true
        }
        return false
    }

    fun plantTreeSapling(tileX: Int, tileY: Int, type: TreeType): Boolean {
        if (!isGrassPlantableForTree(tileX, tileY)) return false
        val id = "tree_planted_${System.currentTimeMillis()}_${tileX}_${tileY}"
        trees.add(WorldTree(id = id, tileX = tileX, tileY = tileY, type = type))
        return true
    }

    fun isNightOrDusk(): Boolean = timeHours >= 18.5f || timeHours < 5.5f

    fun toTreeEntities(userUid: String): List<TreeStateEntity> = trees.map {
        TreeStateEntity(userUid, it.id, it.tileX, it.tileY, it.type.id, it.health, it.state.name, it.regrowTimer)
    }

    fun toCropEntities(userUid: String): List<CropPlotEntity> = cropPlots.values.map {
        CropPlotEntity(userUid, it.key, it.tileX, it.tileY, it.isTilled, it.isWatered, it.cropType?.id, it.stage, it.growthProgress)
    }

    fun toPlacedEntities(userUid: String): List<PlacedObjectEntity> = placedObjects.map {
        PlacedObjectEntity(userUid, it.id, it.itemId, it.tileX, it.tileY, it.rotation)
    }

    fun dispose() {
        if (::tiledMap.isInitialized) {
            runCatching { tiledMap.dispose() }
        }
    }
}
