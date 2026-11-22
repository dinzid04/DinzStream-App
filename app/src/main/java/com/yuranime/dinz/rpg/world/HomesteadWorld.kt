package com.yuranime.dinz.rpg.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.CropPlot
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.ForageNode
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.OreNode
import com.yuranime.dinz.rpg.model.OreType
import com.yuranime.dinz.rpg.model.PlacedObject
import com.yuranime.dinz.rpg.model.TreasureChestNode
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.model.WaterHabitat
import com.yuranime.dinz.rpg.model.WorldRegion
import com.yuranime.dinz.rpg.model.WorldTree
import com.yuranime.dinz.rpg.persistence.CropPlotEntity
import com.yuranime.dinz.rpg.persistence.PlacedObjectEntity
import com.yuranime.dinz.rpg.persistence.TreeStateEntity
import kotlin.random.Random

data class MapPortal(
    val sourceMap: MapId,
    val tileX: Int,
    val tileY: Int,
    val targetMap: MapId,
    val targetTileX: Int,
    val targetTileY: Int,
    val label: String
)

data class StationSpot(
    val mapId: MapId,
    val xRange: IntRange,
    val yRange: IntRange,
    val actionType: ContextActionType,
    val label: String
)

class HomesteadWorld {
    companion object {
        const val TILE_SIZE = 16f
        const val OVERWORLD_W = 80
        const val OVERWORLD_H = 64

        // Player Homestead House footprint on Overworld (LibGDX bottom-up Y = 63 - tiledY)
        const val HOUSE_TILE_X = 12
        const val HOUSE_TILE_Y = 45
        const val HOUSE_DOOR_X = 13
        const val HOUSE_DOOR_Y = 44

        // Agricultural Fields Farmable Zone (Tiled x=7..21, ty=30..38 -> LibGDX y=25..33)
        val FARM_X_RANGE = 7..21
        val FARM_Y_RANGE = 25..33

        // Animated Overworld Landmarks (LibGDX coords)
        const val WINDMILL_TILE_X = 19
        const val WINDMILL_TILE_Y = 36
        const val WATERFALL_TILE_X = 54
        const val WATERFALL_TOP_Y = 58
        const val WATERMILL_TILE_X = 50
        const val WATERMILL_TILE_Y = 30
    }

    var currentMapId: MapId = MapId.OVERWORLD
        private set

    private val loadedMaps = mutableMapOf<MapId, TiledMap>()
    var activeTiledMap: TiledMap? = null
        private set
    private var collisionLayer: TiledMapTileLayer? = null
    private var waterLayer: TiledMapTileLayer? = null
    private var objectsLayer: TiledMapTileLayer? = null

    var mapWidthTiles: Int = OVERWORLD_W
        private set
    var mapHeightTiles: Int = OVERWORLD_H
        private set
    val worldW: Float get() = mapWidthTiles * TILE_SIZE
    val worldH: Float get() = mapHeightTiles * TILE_SIZE

    val trees = mutableListOf<WorldTree>()
    val oreNodes = mutableListOf<OreNode>()
    val forageNodes = mutableListOf<ForageNode>()
    val treasureChests = mutableListOf<TreasureChestNode>()
    val cropPlots = linkedMapOf<String, CropPlot>()
    val placedObjects = mutableListOf<PlacedObject>()
    val discoveredRegions = linkedSetOf(
        WorldRegion.HOMESTEAD.id,
        WorldRegion.FARM_FIELDS.id,
        WorldRegion.VILLAGE_PLAZA.id
    )

    val portals: List<MapPortal> = listOf(
        // Overworld -> Interiors & Mining Cave
        MapPortal(MapId.OVERWORLD, 13, 44, MapId.PLAYER_HOUSE, 10, 3, "Masuk Rumah"),
        MapPortal(MapId.OVERWORLD, 14, 44, MapId.PLAYER_HOUSE, 10, 3, "Masuk Rumah"),
        MapPortal(MapId.OVERWORLD, 34, 31, MapId.GENERAL_STORE, 10, 3, "Masuk Toko Hana"),
        MapPortal(MapId.OVERWORLD, 34, 32, MapId.GENERAL_STORE, 10, 3, "Masuk Toko Hana"),
        MapPortal(MapId.OVERWORLD, 39, 31, MapId.TEAHOUSE, 11, 3, "Masuk Kedai Teh"),
        MapPortal(MapId.OVERWORLD, 39, 32, MapId.TEAHOUSE, 11, 3, "Masuk Kedai Teh"),
        MapPortal(MapId.OVERWORLD, 46, 31, MapId.BLACKSMITH, 10, 3, "Masuk Bengkel Kenji"),
        MapPortal(MapId.OVERWORLD, 46, 32, MapId.BLACKSMITH, 10, 3, "Masuk Bengkel Kenji"),
        MapPortal(MapId.OVERWORLD, 62, 41, MapId.ELDER_HOUSE, 9, 3, "Masuk Pendopo Haru"),
        MapPortal(MapId.OVERWORLD, 62, 42, MapId.ELDER_HOUSE, 9, 3, "Masuk Pendopo Haru"),
        MapPortal(MapId.OVERWORLD, 67, 55, MapId.MINING_CAVE, 16, 3, "Masuk Gua Kristal"),
        MapPortal(MapId.OVERWORLD, 67, 56, MapId.MINING_CAVE, 16, 3, "Masuk Gua Kristal"),

        // Interiors & Cave -> Overworld
        MapPortal(MapId.PLAYER_HOUSE, 9, 1, MapId.OVERWORLD, 13, 43, "Keluar ke Halaman"),
        MapPortal(MapId.PLAYER_HOUSE, 10, 1, MapId.OVERWORLD, 13, 43, "Keluar ke Halaman"),
        MapPortal(MapId.PLAYER_HOUSE, 9, 0, MapId.OVERWORLD, 13, 43, "Keluar ke Halaman"),
        MapPortal(MapId.PLAYER_HOUSE, 10, 0, MapId.OVERWORLD, 13, 43, "Keluar ke Halaman"),

        MapPortal(MapId.GENERAL_STORE, 9, 1, MapId.OVERWORLD, 34, 30, "Keluar ke Pasar"),
        MapPortal(MapId.GENERAL_STORE, 10, 1, MapId.OVERWORLD, 34, 30, "Keluar ke Pasar"),
        MapPortal(MapId.GENERAL_STORE, 9, 0, MapId.OVERWORLD, 34, 30, "Keluar ke Pasar"),
        MapPortal(MapId.GENERAL_STORE, 10, 0, MapId.OVERWORLD, 34, 30, "Keluar ke Pasar"),

        MapPortal(MapId.TEAHOUSE, 10, 1, MapId.OVERWORLD, 39, 30, "Keluar ke Pasar"),
        MapPortal(MapId.TEAHOUSE, 11, 1, MapId.OVERWORLD, 39, 30, "Keluar ke Pasar"),
        MapPortal(MapId.TEAHOUSE, 10, 0, MapId.OVERWORLD, 39, 30, "Keluar ke Pasar"),
        MapPortal(MapId.TEAHOUSE, 11, 0, MapId.OVERWORLD, 39, 30, "Keluar ke Pasar"),

        MapPortal(MapId.BLACKSMITH, 9, 1, MapId.OVERWORLD, 46, 30, "Keluar ke Pasar"),
        MapPortal(MapId.BLACKSMITH, 10, 1, MapId.OVERWORLD, 46, 30, "Keluar ke Pasar"),
        MapPortal(MapId.BLACKSMITH, 9, 0, MapId.OVERWORLD, 46, 30, "Keluar ke Pasar"),
        MapPortal(MapId.BLACKSMITH, 10, 0, MapId.OVERWORLD, 46, 30, "Keluar ke Pasar"),

        MapPortal(MapId.ELDER_HOUSE, 8, 1, MapId.OVERWORLD, 62, 40, "Keluar ke Desa"),
        MapPortal(MapId.ELDER_HOUSE, 9, 1, MapId.OVERWORLD, 62, 40, "Keluar ke Desa"),
        MapPortal(MapId.ELDER_HOUSE, 8, 0, MapId.OVERWORLD, 62, 40, "Keluar ke Desa"),
        MapPortal(MapId.ELDER_HOUSE, 9, 0, MapId.OVERWORLD, 62, 40, "Keluar ke Desa"),

        MapPortal(MapId.MINING_CAVE, 15, 1, MapId.OVERWORLD, 67, 54, "Keluar ke Pegunungan"),
        MapPortal(MapId.MINING_CAVE, 16, 1, MapId.OVERWORLD, 67, 54, "Keluar ke Pegunungan"),
        MapPortal(MapId.MINING_CAVE, 15, 0, MapId.OVERWORLD, 67, 54, "Keluar ke Pegunungan"),
        MapPortal(MapId.MINING_CAVE, 16, 0, MapId.OVERWORLD, 67, 54, "Keluar ke Pegunungan")
    )

    val stations: List<StationSpot> = listOf(
        // Overworld Homestead Stations
        StationSpot(MapId.OVERWORLD, 10..10, 45..45, ContextActionType.OPEN_STORAGE, "Peti Penyimpanan"),
        StationSpot(MapId.OVERWORLD, 17..17, 45..45, ContextActionType.OPEN_SELL_BIN, "Kotak Jual Cepat"),
        // Player House Interior (20x16 -> gdxY = 15 - tiledY)
        StationSpot(MapId.PLAYER_HOUSE, 3..3, 11..12, ContextActionType.SLEEP_BED, "Tidur Hingga Pagi"),
        StationSpot(MapId.PLAYER_HOUSE, 14..16, 11..12, ContextActionType.OPEN_STOVE, "Masak di Dapur"),
        StationSpot(MapId.PLAYER_HOUSE, 15..16, 8..8, ContextActionType.OPEN_WORKBENCH, "Meja Kerajinan"),
        StationSpot(MapId.PLAYER_HOUSE, 3..3, 7..7, ContextActionType.OPEN_STORAGE, "Peti Rumah"),
        // General Store Interior (20x16)
        StationSpot(MapId.GENERAL_STORE, 7..12, 10..10, ContextActionType.OPEN_MERCHANT, "Toko Serba Ada Hana"),
        // Teahouse Interior (22x16)
        StationSpot(MapId.TEAHOUSE, 6..14, 9..10, ContextActionType.OPEN_STOVE, "Dapur Kedai Teh"),
        // Blacksmith Interior (20x16)
        StationSpot(MapId.BLACKSMITH, 8..10, 9..9, ContextActionType.OPEN_HOUSE_UPGRADE, "Upgrade Rumah"),
        StationSpot(MapId.BLACKSMITH, 11..14, 9..9, ContextActionType.OPEN_WORKBENCH, "Tempa & Kerajinan")
    )

    // Animated water sparkle coordinates cached for the currently active map
    val waterSparkleTiles = mutableListOf<Pair<Int, Int>>()

    var houseTier: Int = 1
    var timeHours: Float = 8.0f
    var dayCount: Int = 1
    var isRaining: Boolean = false
    var weatherEnabled: Boolean = true
    var raylightEnabled: Boolean = true
    private var weatherTimer: Float = 105f

    init {
        initDefaultWorldContent()
    }

    fun initDefaultWorldContent() {
        if (trees.isEmpty()) initDefaultTrees()
        if (oreNodes.isEmpty()) initDefaultOreNodes()
        if (forageNodes.isEmpty()) initDefaultForageNodes()
        if (treasureChests.isEmpty()) initDefaultTreasureChests()
        if (cropPlots.isEmpty()) initDefaultFarmPlots()
    }

    fun loadMap(mapId: MapId = MapId.OVERWORLD): TiledMap {
        val map = loadedMaps.getOrPut(mapId) {
            val params = TmxMapLoader.Parameters().apply {
                textureMinFilter = Texture.TextureFilter.Nearest
                textureMagFilter = Texture.TextureFilter.Nearest
            }
            TmxMapLoader().load(mapId.tmxPath, params)
        }
        currentMapId = mapId
        activeTiledMap = map
        val ground = map.layers.get("ground") as? TiledMapTileLayer
        mapWidthTiles = ground?.width ?: OVERWORLD_W
        mapHeightTiles = ground?.height ?: OVERWORLD_H
        collisionLayer = map.layers.get("collision") as? TiledMapTileLayer
        waterLayer = map.layers.get("water") as? TiledMapTileLayer
        objectsLayer = map.layers.get("objects") as? TiledMapTileLayer

        waterSparkleTiles.clear()
        for (y in 0 until mapHeightTiles) {
            for (x in 0 until mapWidthTiles) {
                val wCell = waterLayer?.getCell(x, y)
                val objCell = objectsLayer?.getCell(x, y)
                if (wCell?.tile != null && objCell == null && (x * 7 + y * 13) % 9 == 0) {
                    waterSparkleTiles.add(x to y)
                }
            }
        }
        return map
    }

    fun initDefaultTrees() {
        trees.clear()
        // LibGDX coordinates (gdxY = 63 - tiledY) on clean open grass in Homestead, Forest, Hills & Hidden Forest
        val spots = listOf(
            // Homestead & Northern Meadow
            Triple(8, 49, TreeType.SAKURA),
            Triple(19, 49, TreeType.OAK),
            Triple(23, 46, TreeType.SAKURA),
            Triple(7, 41, TreeType.OAK),
            Triple(26, 49, TreeType.AUTUMN),
            Triple(31, 52, TreeType.OAK),
            Triple(44, 52, TreeType.SAKURA),
            Triple(48, 46, TreeType.PINE),
            // Dense Western Woodland Forest (tx=6..23, gdxY=11..22)
            Triple(7, 21, TreeType.OAK),
            Triple(10, 19, TreeType.PINE),
            Triple(19, 21, TreeType.OAK),
            Triple(22, 18, TreeType.PINE),
            Triple(8, 15, TreeType.PINE),
            Triple(11, 13, TreeType.OAK),
            Triple(18, 15, TreeType.AUTUMN),
            Triple(22, 12, TreeType.OAK),
            Triple(7, 11, TreeType.SAKURA),
            Triple(19, 11, TreeType.PINE),
            // Lake & Residential Scenery
            Triple(29, 25, TreeType.SAKURA),
            Triple(47, 25, TreeType.OAK),
            Triple(60, 48, TreeType.AUTUMN),
            Triple(73, 44, TreeType.PINE),
            Triple(72, 31, TreeType.OAK),
            // Hidden Sakura Forest (Lv 2 North-West)
            Triple(7, 56, TreeType.SAKURA),
            Triple(16, 56, TreeType.SAKURA),
            Triple(20, 58, TreeType.SAKURA),
            // Ancient Ruins Glade (Lv 3 South-East)
            Triple(61, 18, TreeType.AUTUMN),
            Triple(72, 19, TreeType.PINE)
        )
        spots.forEachIndexed { idx, (tx, ty, type) ->
            trees.add(WorldTree(id = "tree_$idx", tileX = tx, tileY = ty, type = type))
        }
    }

    fun initDefaultOreNodes() {
        oreNodes.clear()
        val nodes = listOf(
            // Overworld Mountain Highland (gdxY = 63 - tiledY, tiledY 9..14 -> gdxY 49..54)
            Triple(MapId.OVERWORLD, 60 to 53, OreType.STONE),
            Triple(MapId.OVERWORLD, 64 to 50, OreType.COPPER),
            Triple(MapId.OVERWORLD, 70 to 53, OreType.STONE),
            Triple(MapId.OVERWORLD, 73 to 52, OreType.COPPER),
            Triple(MapId.OVERWORLD, 71 to 49, OreType.IRON),
            Triple(MapId.OVERWORLD, 75 to 50, OreType.STONE),
            // Crystal Mining Cave (32x26 -> gdxY = 25 - tiledY)
            Triple(MapId.MINING_CAVE, 10 to 5, OreType.STONE),
            Triple(MapId.MINING_CAVE, 21 to 5, OreType.COPPER),
            Triple(MapId.MINING_CAVE, 24 to 7, OreType.COPPER),
            Triple(MapId.MINING_CAVE, 8 to 8, OreType.STONE),
            Triple(MapId.MINING_CAVE, 15 to 9, OreType.IRON),
            Triple(MapId.MINING_CAVE, 19 to 10, OreType.IRON),
            Triple(MapId.MINING_CAVE, 26 to 11, OreType.GOLD),
            Triple(MapId.MINING_CAVE, 14 to 15, OreType.COPPER),
            Triple(MapId.MINING_CAVE, 20 to 16, OreType.IRON),
            Triple(MapId.MINING_CAVE, 25 to 15, OreType.GOLD),
            Triple(MapId.MINING_CAVE, 19 to 20, OreType.AMETHYST),
            Triple(MapId.MINING_CAVE, 23 to 20, OreType.EMERALD)
        )
        nodes.forEachIndexed { idx, (mId, pos, type) ->
            oreNodes.add(OreNode("ore_$idx", mId, pos.first, pos.second, type))
        }
    }

    fun initDefaultForageNodes() {
        forageNodes.clear()
        val items = listOf(
            Triple(MapId.OVERWORLD, 9 to 20, "forage_berry"),
            Triple(MapId.OVERWORLD, 17 to 18, "forage_chestnut"),
            Triple(MapId.OVERWORLD, 21 to 14, "forage_berry"),
            Triple(MapId.OVERWORLD, 9 to 12, "forage_chestnut"),
            Triple(MapId.OVERWORLD, 28 to 48, "forage_berry"),
            Triple(MapId.OVERWORLD, 46 to 49, "forage_tea_leaf"),
            Triple(MapId.OVERWORLD, 61 to 49, "forage_tea_leaf"),
            Triple(MapId.OVERWORLD, 69 to 50, "forage_tea_leaf"),
            Triple(MapId.OVERWORLD, 11 to 55, "forage_tea_leaf"),
            Triple(MapId.OVERWORLD, 18 to 55, "forage_berry"),
            Triple(MapId.OVERWORLD, 61 to 22, "forage_chestnut"),
            Triple(MapId.OVERWORLD, 71 to 14, "forage_tea_leaf")
        )
        items.forEachIndexed { idx, (mId, pos, itemId) ->
            forageNodes.add(ForageNode("forage_$idx", mId, pos.first, pos.second, itemId))
        }
    }

    fun initDefaultTreasureChests() {
        treasureChests.clear()
        treasureChests.addAll(
            listOf(
                TreasureChestNode(
                    id = "chest_beach",
                    mapId = MapId.OVERWORLD,
                    tileX = 48,
                    tileY = 8,
                    coinReward = 140,
                    itemRewards = listOf("seed_strawberry" to 3, "food_tempura" to 1),
                    label = "Peti Harta Pantai"
                ),
                TreasureChestNode(
                    id = "chest_sakura",
                    mapId = MapId.OVERWORLD,
                    tileX = 14,
                    tileY = 58,
                    coinReward = 220,
                    itemRewards = listOf("seed_sakura" to 3, "food_honey_tea" to 2),
                    label = "Peti Kuil Sakura"
                ),
                TreasureChestNode(
                    id = "chest_ruins",
                    mapId = MapId.OVERWORLD,
                    tileX = 68,
                    tileY = 14,
                    coinReward = 320,
                    itemRewards = listOf("gem_emerald" to 1, "ore_gold" to 2),
                    label = "Peti Reruntuhan Kuno"
                ),
                TreasureChestNode(
                    id = "chest_cave",
                    mapId = MapId.MINING_CAVE,
                    tileX = 26,
                    tileY = 21,
                    coinReward = 260,
                    itemRewards = listOf("gem_amethyst" to 1, "ore_gold" to 2, "food_bento" to 1),
                    label = "Peti Harta Gua Kristal"
                )
            )
        )
    }

    private fun initDefaultFarmPlots() {
        val starterA = CropPlot(10, 31, isTilled = true, isWatered = true, cropType = CropType.CARROT, stage = 2, growthProgress = 0.5f)
        val starterB = CropPlot(11, 31, isTilled = true, isWatered = true, cropType = CropType.TOMATO, stage = 1, growthProgress = 0.3f)
        val starterC = CropPlot(12, 31, isTilled = true, isWatered = true, cropType = CropType.STRAWBERRY, stage = 2, growthProgress = 0.4f)
        val starterD = CropPlot(13, 31, isTilled = true, isWatered = false, cropType = null, stage = 0, growthProgress = 0f)
        listOf(starterA, starterB, starterC, starterD).forEach { cropPlots[it.key] = it }
    }

    fun loadSavedWorld(
        savedHouseTier: Int,
        savedTimeHours: Float,
        savedDayCount: Int,
        savedDiscoveredCsv: String,
        savedOpenedChestsCsv: String,
        treeEntities: List<TreeStateEntity>,
        cropEntities: List<CropPlotEntity>,
        placedEntities: List<PlacedObjectEntity>
    ) {
        houseTier = savedHouseTier.coerceIn(1, 3)
        timeHours = savedTimeHours.coerceIn(0f, 23.99f)
        dayCount = savedDayCount.coerceAtLeast(1)

        discoveredRegions.clear()
        discoveredRegions.addAll(
            savedDiscoveredCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
        discoveredRegions.add(WorldRegion.HOMESTEAD.id)

        val openedSet = savedOpenedChestsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        treasureChests.forEach { it.opened = it.id in openedSet }

        if (treeEntities.isNotEmpty()) {
            val validSaved = treeEntities.filter { it.tileX in 2 until OVERWORLD_W - 2 && it.tileY in 2 until OVERWORLD_H - 2 }
            if (validSaved.size >= 15) {
                trees.clear()
                validSaved.forEach { e ->
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
        }

        if (cropEntities.isNotEmpty()) {
            val validCrops = cropEntities.filter { it.tileX in FARM_X_RANGE && it.tileY in FARM_Y_RANGE }
            if (validCrops.isNotEmpty()) {
                cropPlots.clear()
                validCrops.forEach { e ->
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
        }

        placedObjects.clear()
        placedEntities.forEach { e ->
            placedObjects.add(PlacedObject(e.objectId, MapId.fromId(e.mapId), e.itemId, e.tileX, e.tileY, e.rotation))
        }
    }

    fun update(dt: Float, onTreeRegrown: (WorldTree) -> Unit = {}) {
        // Cozy day/night cycle: 1 real minute = 2 in-game hours
        timeHours += dt * (2f / 60f)
        if (timeHours >= 24f) {
            timeHours -= 24f
            dayCount++
        }

        if (weatherEnabled) {
            weatherTimer -= dt
            if (weatherTimer <= 0f) {
                isRaining = !isRaining
                weatherTimer = if (isRaining) 32f + Random.nextFloat() * 22f else 95f + Random.nextFloat() * 65f
            }
        } else {
            isRaining = false
        }

        for (tree in trees) {
            if (tree.shakeTimer > 0f) tree.shakeTimer = (tree.shakeTimer - dt).coerceAtLeast(0f)
            if (tree.state == TreeHealthState.FALLING) {
                tree.fallTimer += dt
                if (tree.fallTimer >= 0.45f) {
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

        for (ore in oreNodes) {
            if (ore.shakeTimer > 0f) ore.shakeTimer = (ore.shakeTimer - dt).coerceAtLeast(0f)
            if (ore.depleted) {
                ore.respawnTimer -= dt
                if (ore.respawnTimer <= 0f) {
                    ore.depleted = false
                    ore.health = ore.type.maxHealth
                }
            }
        }

        for (forage in forageNodes) {
            if (forage.collected) {
                forage.respawnTimer -= dt
                if (forage.respawnTimer <= 0f) {
                    forage.collected = false
                }
            }
        }
    }

    fun sleepToNextMorning() {
        dayCount++
        timeHours = 6.5f
        isRaining = weatherEnabled && Random.nextFloat() < 0.25f
        // Grow every watered crop by +1 full stage overnight!
        for (plot in cropPlots.values) {
            if (plot.cropType != null && plot.stage < 4) {
                if (plot.isWatered) {
                    plot.stage = (plot.stage + 1).coerceAtMost(4)
                    plot.growthProgress = if (plot.stage >= 4) 1f else 0.1f
                }
                plot.isWatered = isRaining
            }
        }
        // Respawn ore nodes & wild forageables for the new morning!
        oreNodes.forEach {
            it.depleted = false
            it.health = it.type.maxHealth
            it.respawnTimer = 0f
        }
        forageNodes.forEach {
            it.collected = false
            it.respawnTimer = 0f
        }
    }

    fun getRegionAt(mapId: MapId, tx: Int, ty: Int): WorldRegion {
        if (mapId == MapId.MINING_CAVE) return WorldRegion.CRYSTAL_CAVE
        if (mapId == MapId.PLAYER_HOUSE) return WorldRegion.HOMESTEAD
        if (mapId == MapId.GENERAL_STORE || mapId == MapId.TEAHOUSE || mapId == MapId.BLACKSMITH) return WorldRegion.MARKETPLACE
        if (mapId == MapId.ELDER_HOUSE) return WorldRegion.RESIDENTIAL

        // Overworld LibGDX coords (ty = 63 - tiledY)
        return when {
            ty <= 9 -> WorldRegion.OCEAN_BEACH
            tx <= 25 && ty >= 53 -> WorldRegion.HIDDEN_SAKURA
            tx >= 52 && ty >= 48 -> WorldRegion.MOUNTAIN_HIGHLAND
            tx in 50..57 -> WorldRegion.RIVER_VALLEY
            tx in 26..48 && ty in 10..23 -> WorldRegion.WHISPERING_LAKE
            tx <= 25 && ty in 10..23 -> WorldRegion.DENSE_FOREST
            tx >= 58 && ty in 10..31 -> WorldRegion.ANCIENT_RUINS
            tx in 6..23 && ty in 24..35 -> WorldRegion.FARM_FIELDS
            tx in 30..49 && ty in 26..34 -> WorldRegion.MARKETPLACE
            tx in 30..49 && ty in 35..48 -> WorldRegion.VILLAGE_PLAZA
            tx >= 58 && ty in 32..47 -> WorldRegion.RESIDENTIAL
            else -> WorldRegion.HOMESTEAD
        }
    }

    fun isRegionUnlocked(region: WorldRegion, playerLevel: Int): Boolean {
        return playerLevel >= region.requiredLevel
    }

    fun discoverRegion(region: WorldRegion): Boolean {
        return discoveredRegions.add(region.id)
    }

    fun isInBounds(tileX: Int, tileY: Int): Boolean {
        return tileX in 1 until (mapWidthTiles - 1) && tileY in 0 until (mapHeightTiles - 1)
    }

    fun isMapSolidTile(tileX: Int, tileY: Int): Boolean {
        if (!isInBounds(tileX, tileY)) return true
        val cell = collisionLayer?.getCell(tileX, tileY)
        return cell?.tile != null && cell.tile.id > 0
    }

    fun isWaterTile(tileX: Int, tileY: Int): Boolean {
        if (tileX !in 0 until mapWidthTiles || tileY !in 0 until mapHeightTiles) return false
        val wCell = waterLayer?.getCell(tileX, tileY)
        if (wCell?.tile == null || wCell.tile.id <= 0) return false
        // Bridges & wooden fishing piers have collision == 0, so only solid water tiles are fishable water
        return isMapSolidTile(tileX, tileY)
    }

    fun getWaterHabitat(tileX: Int, tileY: Int): WaterHabitat {
        if (currentMapId == MapId.MINING_CAVE) return WaterHabitat.CAVE
        return when {
            tileY <= 8 -> WaterHabitat.OCEAN
            tileX >= 49 -> WaterHabitat.RIVER
            else -> WaterHabitat.LAKE
        }
    }

    fun isFarmableTile(tileX: Int, tileY: Int): Boolean {
        return currentMapId == MapId.OVERWORLD && tileX in FARM_X_RANGE && tileY in FARM_Y_RANGE
    }

    fun isGrassPlantableForTree(tileX: Int, tileY: Int): Boolean {
        if (currentMapId != MapId.OVERWORLD) return false
        if (!isInBounds(tileX, tileY)) return false
        if (isMapSolidTile(tileX, tileY) || isWaterTile(tileX, tileY) || isFarmableTile(tileX, tileY)) return false
        if (getTreeAt(tileX, tileY) != null || getPlacedObjectAt(tileX, tileY) != null || getOreAt(tileX, tileY) != null) return false
        if (tileY <= 9 || (tileX >= 52 && tileY >= 48)) return false // No trees on beach or mountain rock
        return true
    }

    fun getTreeAt(tileX: Int, tileY: Int): WorldTree? {
        if (currentMapId != MapId.OVERWORLD) return null
        return trees.find { it.tileX == tileX && it.tileY == tileY }
    }

    fun getOreAt(tileX: Int, tileY: Int): OreNode? {
        return oreNodes.find { it.mapId == currentMapId && !it.depleted && it.tileX == tileX && it.tileY == tileY }
    }

    fun getForageAt(tileX: Int, tileY: Int): ForageNode? {
        return forageNodes.find { it.mapId == currentMapId && !it.collected && it.tileX == tileX && it.tileY == tileY }
    }

    fun getChestAt(tileX: Int, tileY: Int): TreasureChestNode? {
        return treasureChests.find { it.mapId == currentMapId && !it.opened && it.tileX == tileX && it.tileY == tileY }
    }

    fun getPortalAt(tileX: Int, tileY: Int): MapPortal? {
        return portals.find { it.sourceMap == currentMapId && it.tileX == tileX && it.tileY == tileY }
    }

    fun getStationAt(tileX: Int, tileY: Int): StationSpot? {
        return stations.find { it.mapId == currentMapId && tileX in it.xRange && tileY in it.yRange }
    }

    fun getPlacedObjectAt(tileX: Int, tileY: Int): PlacedObject? {
        return placedObjects.find { it.mapId == currentMapId && it.tileX == tileX && it.tileY == tileY }
    }

    fun getPlacedObjectsInCurrentMap(): List<PlacedObject> {
        return placedObjects.filter { it.mapId == currentMapId }
    }

    fun isBlockedForWalking(tileX: Int, tileY: Int, playerLevel: Int = 99): Boolean {
        if (isMapSolidTile(tileX, tileY)) return true
        if (currentMapId == MapId.OVERWORLD) {
            val reg = getRegionAt(MapId.OVERWORLD, tileX, tileY)
            if (!isRegionUnlocked(reg, playerLevel)) return true
        }
        val tree = getTreeAt(tileX, tileY)
        if (tree != null && tree.state != TreeHealthState.STUMP) return true
        if (getOreAt(tileX, tileY) != null) return true
        if (getChestAt(tileX, tileY) != null) return true
        if (getPlacedObjectAt(tileX, tileY) != null) return true
        return false
    }

    fun isBlockedAtWorldPos(
        worldX: Float,
        worldY: Float,
        playerLevel: Int = 99,
        halfW: Float = 4.5f,
        halfH: Float = 3.5f
    ): Boolean {
        val corners = arrayOf(
            (worldX - halfW) to (worldY - halfH),
            (worldX + halfW) to (worldY - halfH),
            (worldX - halfW) to (worldY + halfH),
            (worldX + halfW) to (worldY + halfH)
        )
        for ((cx, cy) in corners) {
            val tx = (cx / TILE_SIZE).toInt()
            val ty = (cy / TILE_SIZE).toInt()
            if (isBlockedForWalking(tx, ty, playerLevel)) return true
        }
        return false
    }

    fun plantTreeSapling(tileX: Int, tileY: Int, type: TreeType): Boolean {
        if (!isGrassPlantableForTree(tileX, tileY)) return false
        val id = "tree_planted_${System.currentTimeMillis()}_${tileX}_${tileY}"
        trees.add(WorldTree(id = id, tileX = tileX, tileY = tileY, type = type))
        return true
    }

    fun isNightOrDusk(): Boolean = !currentMapId.isIndoor && (timeHours >= 18.5f || timeHours < 5.5f)

    fun toTreeEntities(userUid: String): List<TreeStateEntity> = trees.map {
        TreeStateEntity(userUid, it.id, it.tileX, it.tileY, it.type.id, it.health, it.state.name, it.regrowTimer)
    }

    fun toCropEntities(userUid: String): List<CropPlotEntity> = cropPlots.values.map {
        CropPlotEntity(userUid, it.key, it.tileX, it.tileY, it.isTilled, it.isWatered, it.cropType?.id, it.stage, it.growthProgress)
    }

    fun toPlacedEntities(userUid: String): List<PlacedObjectEntity> = placedObjects.map {
        PlacedObjectEntity(userUid, it.id, it.mapId.id, it.itemId, it.tileX, it.tileY, it.rotation)
    }

    fun getOpenedChestsCsv(): String = treasureChests.filter { it.opened }.joinToString(",") { it.id }

    fun getDiscoveredRegionsCsv(): String = discoveredRegions.joinToString(",")

    fun dispose() {
        loadedMaps.values.forEach { runCatching { it.dispose() } }
        loadedMaps.clear()
        activeTiledMap = null
    }
}
