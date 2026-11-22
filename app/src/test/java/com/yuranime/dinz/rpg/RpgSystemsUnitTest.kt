package com.yuranime.dinz.rpg

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader
import com.badlogic.gdx.utils.XmlReader
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
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.FishRarity
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.OreNode
import com.yuranime.dinz.rpg.model.OreType
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.model.WaterHabitat
import com.yuranime.dinz.rpg.model.WorldRegion
import com.yuranime.dinz.rpg.npc.NpcSystem
import com.yuranime.dinz.rpg.persistence.AchievementEntity
import com.yuranime.dinz.rpg.persistence.CropPlotEntity
import com.yuranime.dinz.rpg.persistence.FishCollectionEntity
import com.yuranime.dinz.rpg.persistence.InventoryItemEntity
import com.yuranime.dinz.rpg.persistence.NpcFriendshipEntity
import com.yuranime.dinz.rpg.persistence.PlacedObjectEntity
import com.yuranime.dinz.rpg.persistence.PlayerStateEntity
import com.yuranime.dinz.rpg.persistence.QuestProgressEntity
import com.yuranime.dinz.rpg.persistence.RpgSaveBundle
import com.yuranime.dinz.rpg.persistence.RpgSaveSerializer
import com.yuranime.dinz.rpg.persistence.TreeStateEntity
import com.yuranime.dinz.rpg.quests.ProgressionSystem
import com.yuranime.dinz.rpg.world.HomesteadWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RpgSystemsUnitTest {

    @Test
    fun catalogHasCompleteSakuraValeContent() {
        assertEquals(8, RpgCatalog.protagonistPresets.size)
        assertEquals(8, RpgCatalog.npcs.size)
        assertEquals(16, RpgCatalog.fishSpecies.size)
        assertEquals(6, CropType.entries.size)
        assertEquals(6, OreType.entries.size)
        assertEquals(3, RpgCatalog.houseUpgrades.size)
        assertEquals(7, MapId.entries.size)
        assertEquals(13, WorldRegion.entries.size)
        assertTrue("Expected at least 14 buildable items", RpgCatalog.buildableItems.size >= 14)
        assertTrue("Expected at least 20 crafting & cooking recipes", RpgCatalog.recipes.size >= 20)
        assertTrue("Expected at least 12 quests", RpgCatalog.quests.size >= 12)
    }

    @Test
    fun aStarPathfinderAvoidsObstaclesAndFindsAdjacentTargets() {
        // 10x10 grid with a vertical wall at x=5, y=1..8 (gap at y=0 and y=9)
        val walkable: (Int, Int) -> Boolean = { x, y -> !(x == 5 && y in 1..8) }
        val path = AStarPathfinder.findPath(2, 5, 8, 5, 10, 10, isWalkable = walkable)
        assertTrue("A* should find detour around wall", path.isNotEmpty())
        assertEquals(8 to 5, path.last())
        assertTrue("No waypoint may step on blocked wall", path.all { (x, y) -> walkable(x, y) })

        // Completely enclosed target returns empty
        val boxedWalkable: (Int, Int) -> Boolean = { x, y -> !(x in 7..9 && y in 7..9 && !(x == 8 && y == 8)) }
        val impossible = AStarPathfinder.findPath(1, 1, 8, 8, 10, 10, isWalkable = boxedWalkable)
        assertTrue(impossible.isEmpty())

        // Adjacent pathing stops next to solid target at (5,5)
        val adjPath = AStarPathfinder.findPathToAdjacent(2, 5, 5, 5, 10, 10, isWalkable = walkable)
        assertNotNull(adjPath)
        assertEquals(4 to 5, adjPath!!.last())
    }

    @Test
    fun inventoryStackingToolsAndSortingWorks() {
        val inv = InventorySystem()
        inv.initStarterKit()
        assertTrue(inv.hasTool("tool_rod_basic"))
        assertTrue(inv.hasTool("tool_axe"))
        assertTrue(inv.hasTool("tool_pickaxe"))
        assertTrue(inv.hasTool("tool_hoe"))
        assertTrue(inv.hasTool("tool_watering_can"))
        assertTrue(inv.hasTool("tool_hammer"))

        val before = inv.getQuantity("res_wood")
        val added = inv.addItem("res_wood", 15)
        assertEquals(15, added)
        assertEquals(before + 15, inv.getQuantity("res_wood"))
        assertTrue(inv.removeItem("res_wood", 5))
        assertFalse(inv.removeItem("res_wood", 999))

        val sortedByQty = inv.getStacks(InventorySortMode.QUANTITY)
        assertTrue(sortedByQty.first().quantity >= sortedByQty.last().quantity)
    }

    @Test
    fun economyBuyAndSellUpdatesInventoryAndCoins() {
        val inv = InventorySystem()
        inv.initStarterKit()
        val eco = EconomySystem(inv)
        eco.coins = 300

        val buyRes = eco.buyItem("seed_pumpkin", 2)
        assertTrue(buyRes.success)
        assertEquals(300 - 88, eco.coins)
        assertEquals(2, inv.getQuantity("seed_pumpkin"))

        inv.addItem("fish_crimson_koi", 2)
        val sellRes = eco.sellAllInCategory(ItemCategory.FISH)
        assertTrue(sellRes.success)
        assertEquals(230, sellRes.coinsDelta)
        assertEquals(0, inv.getQuantity("fish_crimson_koi"))
    }

    @Test
    fun farmingAndOvernightSleepGrowthFlow() {
        val world = HomesteadWorld()
        val inv = InventorySystem()
        inv.initStarterKit()
        val farming = FarmingSystem(world, inv)

        // Sakura Vale Agricultural Fields: FARM_X_RANGE=7..21, FARM_Y_RANGE=25..33
        val tillRes = farming.tillSoil(15, 29)
        assertTrue(tillRes.success)

        val plantRes = farming.plantSeed(15, 29, "seed_strawberry")
        assertTrue(plantRes.success)

        val waterRes = farming.waterPlot(15, 29)
        assertTrue(waterRes.success)

        // Sleep overnight grows watered crop by +1 stage and increments day
        val dayBefore = world.dayCount
        world.sleepToNextMorning()
        assertEquals(dayBefore + 1, world.dayCount)
        assertEquals(1, world.cropPlots["15_29"]!!.stage)

        // Continue real-time growth to stage 4 harvestable
        farming.waterPlot(15, 29)
        repeat(45) { farming.update(2.5f) }
        val plot = world.cropPlots["15_29"]
        assertNotNull(plot)
        assertTrue("Crop should reach harvestable stage 4", plot!!.isHarvestable)

        val harvestRes = farming.harvestCrop(15, 29)
        assertTrue(harvestRes.success)
        assertTrue(inv.getQuantity("crop_strawberry") >= 1)
    }

    @Test
    fun miningAndCraftingCookingSystemsWork() {
        val inv = InventorySystem()
        inv.initStarterKit()
        val mining = MiningSystem(inv)
        val crafting = CraftingSystem(inv)

        // Mine Gold ore rock with Golden Pickaxe
        val goldNode = OreNode("test_gold", MapId.MINING_CAVE, 20, 15, OreType.GOLD)
        var shattered = false
        for (hit in 0 until 10) {
            val res = mining.mineNode(goldNode, pickaxeDamage = 2, hasGoldPickaxe = true)
            if (res.shattered) {
                shattered = true
                break
            }
        }
        assertTrue("Gold ore node should shatter", shattered)
        assertTrue("Player should receive Gold Ore", inv.getQuantity("ore_gold") >= 2)

        // Cook Onigiri from Wheat + Forest Berry
        inv.addItem("crop_wheat", 2)
        inv.addItem("forage_berry", 2)
        val beforeOnigiri = inv.getQuantity("food_onigiri")
        val cookRes = crafting.craft("cook_onigiri")
        assertTrue(cookRes.success)
        assertEquals(beforeOnigiri + 1, inv.getQuantity("food_onigiri"))

        // Craft Wooden Table
        val craftRes = crafting.craft("rec_table")
        assertTrue(craftRes.success)
        assertTrue(inv.hasItem("wooden_table", 1))
    }

    @Test
    fun livingNpcSchedulesDialogueAndGiftingWork() {
        val npcSys = NpcSystem()
        assertEquals(8, npcSys.npcs.size)
        assertEquals(5, npcSys.animals.size)

        // Hana is in General Store at 10:00 and Overworld Plaza at 18:30
        val hanaDef = RpgCatalog.npcById["npc_hana"]!!
        assertEquals(MapId.GENERAL_STORE, npcSys.findSlotForHour(hanaDef, 10.0f).mapId)
        assertEquals(MapId.OVERWORLD, npcSys.findSlotForHour(hanaDef, 18.5f).mapId)

        // Talk to Hana awards friendship & returns dialogue
        val talk = npcSys.talkToNpc("npc_hana", 160f, 160f, 10.0f)
        assertNotNull(talk)
        assertTrue(talk!!.first.active)
        assertTrue(talk.second)

        // Give Hana a loved gift (Berry Tart -> +35 points!)
        val gift1 = npcSys.giveGift("npc_hana", "food_berry_tart")
        assertNotNull(gift1)
        assertTrue(gift1!!.accepted)
        assertEquals(35, gift1.pointsAdded)

        // Second gift same day is politely declined until next morning
        val gift2 = npcSys.giveGift("npc_hana", "crop_strawberry")
        assertFalse(gift2!!.accepted)

        npcSys.onNewDay()
        val giftNextDay = npcSys.giveGift("npc_hana", "crop_strawberry")
        assertTrue(giftNextDay!!.accepted)
    }

    @Test
    fun houseUpgradeAndPlacementWork() {
        val world = HomesteadWorld()
        val inv = InventorySystem()
        inv.initStarterKit()
        val eco = EconomySystem(inv)
        val building = BuildingSystem(world, inv, eco)

        eco.coins = 600
        inv.addItem("res_wood", 30)
        inv.addItem("res_stone", 20)
        inv.addItem("ore_iron", 5)

        val up2 = building.upgradeHouse()
        assertTrue(up2.success)
        assertEquals(2, world.houseTier)

        building.startBuildMode("bench_wood", 15, 42)
        building.rotatePreview()
        assertEquals(90, building.rotation)
        val placeRes = building.confirmPlacement()
        assertTrue("Bench should place cleanly", placeRes.success)
        assertEquals(1, world.placedObjects.size)

        val removeRes = building.removeObjectAt(15, 42)
        assertTrue(removeRes.success)
        assertEquals(0, world.placedObjects.size)
        assertEquals(1, inv.getQuantity("bench_wood"))
    }

    @Test
    fun fishingStateMachineWorksAcrossAllWaterHabitats() {
        for (habitat in listOf(WaterHabitat.LAKE, WaterHabitat.RIVER, WaterHabitat.OCEAN, WaterHabitat.CAVE)) {
            val fishing = FishingSystem()
            fishing.startCasting(120f, 80f, habitat, "tool_rod_golden", 3, true)
            assertEquals(FishingPhase.CASTING, fishing.phase)
            var reachedBite = false
            for (step in 0 until 200) {
                fishing.update(0.05f)
                if (fishing.phase == FishingPhase.BITE) {
                    reachedBite = true
                    break
                }
            }
            assertTrue("Fishing in $habitat should reach BITE", reachedBite)
            fishing.onTimingTap()
            assertEquals(FishingPhase.MINIGAME, fishing.phase)
            assertNotNull(fishing.toUiState().targetFish)
        }
    }

    @Test
    fun progressionTracksSixSkillsQuestsAndAchievements() {
        val prog = ProgressionSystem()
        repeat(3) { prog.recordFishCaught("fish_pond_carp", FishRarity.COMMON) }
        repeat(2) { prog.recordDishCooked(30) }
        repeat(6) { prog.recordOreMined(25) }

        val fishQuest = prog.getQuestStates().first { it.def.id == "q_catch_3_fish" }
        assertTrue(fishQuest.isCompleted)
        assertNotNull(prog.claimQuest("q_catch_3_fish"))

        val cookQuest = prog.getQuestStates().first { it.def.id == "m_chef_lesson" }
        assertTrue(cookQuest.isCompleted)
        val mineQuest = prog.getQuestStates().first { it.def.id == "m_crystal_miner" }
        assertTrue(mineQuest.isCompleted)

        // Daily quest resets on new morning while Main quest remains
        prog.resetDailyQuests()
        assertEquals(0, prog.getQuestStates().first { it.def.id == "q_catch_3_fish" }.progress)
        assertTrue(prog.getQuestStates().first { it.def.id == "m_chef_lesson" }.isCompleted)
    }

    @Test
    fun saveSerializerRoundTripBackwardCompatibilityAndUidIsolation() {
        val uidA = "g_user_alpha_example_com"
        val uidB = "g_user_beta_example_com"

        val bundle = RpgSaveBundle(
            player = PlayerStateEntity(
                userUid = uidA,
                currentMapId = MapId.PLAYER_HOUSE.id,
                posX = 160f,
                posY = 96f,
                facing = "up",
                level = 8,
                exp = 45,
                coins = 1850,
                energy = 78,
                fishingLevel = 5,
                farmingLevel = 4,
                gatheringLevel = 3,
                miningLevel = 4,
                cookingLevel = 3,
                craftingLevel = 2,
                houseTier = 2,
                gameTimeHours = 16.25f,
                dayCount = 11,
                presetId = "samurai",
                discoveredRegionsCsv = "homestead,farm_fields,village_plaza,ocean_beach,crystal_cave",
                openedChestsCsv = "chest_beach",
                updatedAtMs = 1_760_000_000_000L
            ),
            inventory = listOf(
                InventoryItemEntity(uidA, "tool_rod_golden", 1),
                InventoryItemEntity(uidA, "fish_crimson_koi", 4)
            ),
            fishCollection = listOf(
                FishCollectionEntity(uidA, "fish_crimson_koi", 4, 1_759_999_000_000L)
            ),
            trees = listOf(
                TreeStateEntity(uidA, "tree_0", 8, 49, TreeType.SAKURA.id, 2, TreeHealthState.DAMAGED.name, 0f)
            ),
            crops = listOf(
                CropPlotEntity(uidA, "10_31", 10, 31, true, true, CropType.PUMPKIN.id, 3, 0.5f)
            ),
            placedObjects = listOf(
                PlacedObjectEntity(uidA, "obj_1", MapId.PLAYER_HOUSE.id, "bookshelf", 8, 9, 0)
            ),
            quests = listOf(
                QuestProgressEntity(uidA, "q_catch_3_fish", 3, true)
            ),
            achievements = listOf(
                AchievementEntity(uidA, "a_first_fish", 1, true)
            ),
            npcFriendships = listOf(
                NpcFriendshipEntity(uidA, "npc_hana", 85, true, false)
            )
        )

        val json = RpgSaveSerializer.toJson(bundle)
        val restoredA = RpgSaveSerializer.fromJson(json, uidA)
        assertNotNull(restoredA)
        assertEquals(uidA, restoredA!!.player.userUid)
        assertEquals("samurai", restoredA.player.presetId)
        assertEquals(MapId.PLAYER_HOUSE.id, restoredA.player.currentMapId)
        assertEquals(78, restoredA.player.energy)
        assertEquals(1, restoredA.npcFriendships.size)
        assertEquals(85, restoredA.npcFriendships.first().points)

        // UID isolation check
        val reboundB = RpgSaveSerializer.fromJson(json, uidB)
        assertNotNull(reboundB)
        assertEquals(uidB, reboundB!!.player.userUid)
        assertTrue(reboundB.npcFriendships.all { it.userUid == uidB })
        assertTrue(reboundB.placedObjects.all { it.userUid == uidB })

        // Backward compatibility with legacy v2 JSON (missing presetId, energy, npcFriendships, currentMapId)
        val legacyV2Json = """{"player":{"userUid":"old","posX":144,"posY":224,"facing":"down","level":3,"exp":20,"coins":420,"houseTier":2,"gameTimeHours":10.5,"dayCount":4},"inventory":[{"userUid":"old","itemId":"res_wood","quantity":12}],"fishCollection":[],"trees":[],"crops":[],"placedObjects":[],"quests":[],"achievements":[]}"""
        val migrated = RpgSaveSerializer.fromJson(legacyV2Json, uidA)
        assertNotNull("Legacy v2 save must migrate cleanly", migrated)
        assertEquals("overworld", migrated!!.player.currentMapId)
        assertEquals("princess", migrated.player.presetId)
        assertEquals(100, migrated.player.energy)
        assertEquals(420, migrated.player.coins)

        // Corrupted JSON never crashes
        assertNull(RpgSaveSerializer.fromJson("{broken", uidA))
        assertNull(RpgSaveSerializer.fromJson("", uidA))
    }

    @Test
    fun allSevenTmxMapsAndBundledCc0AssetsAreValidAndComplete() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
        assertNotNull("Assets directory must exist in test workspace", assetsRoot)

        val xml = XmlReader()
        // 1. Validate EVERY SINGLE ONE of the 7 TMX maps using real LibGDX BaseTmxMapLoader.getTileIds()!
        for (mapId in MapId.entries) {
            val tmxFile = File(assetsRoot, mapId.tmxPath)
            assertTrue("Missing TMX map: ${mapId.tmxPath}", tmxFile.isFile)
            val mapElem = xml.parse(FileHandle(tmxFile))
            val w = mapElem.getIntAttribute("width")
            val h = mapElem.getIntAttribute("height")
            assertTrue("Invalid map size for ${mapId.id}: ${w}x${h}", w >= 18 && h >= 14)

            // Verify every inline tileset image source resolves to an existing PNG!
            val tilesets = mapElem.getChildrenByName("tileset")
            assertTrue("Map ${mapId.id} must have inline tilesets", tilesets.size > 0)
            for (i in 0 until tilesets.size) {
                val imgSrc = tilesets.get(i).getChildByName("image").getAttribute("source")
                val resolved = File(tmxFile.parentFile, imgSrc).canonicalFile
                assertTrue("Missing tileset PNG for ${mapId.id}: $resolved", resolved.isFile)
            }

            // Verify every layer's CSV data parses with zero NumberFormatException in LibGDX!
            val layers = mapElem.getChildrenByName("layer")
            assertEquals("Map ${mapId.id} should have 8 layers", 8, layers.size)
            for (i in 0 until layers.size) {
                val layer = layers.get(i)
                val ids = BaseTmxMapLoader.getTileIds(layer, w, h)
                assertEquals("Layer ${layer.getAttribute("name")} in ${mapId.id} size mismatch", w * h, ids.size)
            }
        }

        // 2. Verify all 8 Protagonists, 8 NPCs & 5 Animals
        for (p in RpgCatalog.protagonistPresets) {
            assertTrue("Missing player sheet: ${p.sheetPath}", File(assetsRoot, p.sheetPath).isFile)
            assertTrue("Missing player face: ${p.facePath}", File(assetsRoot, p.facePath).isFile)
        }
        for (n in RpgCatalog.npcs) {
            assertTrue("Missing NPC sheet: ${n.sheetPath}", File(assetsRoot, n.sheetPath).isFile)
            assertTrue("Missing NPC face: ${n.facePath}", File(assetsRoot, n.facePath).isFile)
        }
        for (a in listOf("cat", "dog", "chicken", "cow", "frog")) {
            assertTrue("Missing animal sheet: $a", File(assetsRoot, "rpg/characters/animals/${a}_sheet.png").isFile)
        }

        // 3. Verify all catalog items, crops, trees, rocks, houses, FX, UI & Audio!
        for (item in RpgCatalog.items) {
            assertTrue("Missing item sprite: ${item.spritePath}", File(assetsRoot, item.spritePath).isFile)
        }
        for (crop in CropType.entries) {
            for (st in 0..4) {
                assertTrue("Missing crop stage: ${crop.id} $st", File(assetsRoot, "rpg/items/crops/crop_${crop.id}_stage$st.png").isFile)
            }
        }
        for (tree in TreeType.entries) {
            assertTrue("Missing tree sprite: ${tree.spritePath}", File(assetsRoot, tree.spritePath).isFile)
        }
        assertTrue(File(assetsRoot, "rpg/objects/trees/tree_stump.png").isFile)
        for (ore in OreType.entries) {
            assertTrue("Missing ore rock: ${ore.rockSpritePath}", File(assetsRoot, ore.rockSpritePath).isFile)
        }
        for (tier in 1..3) {
            assertTrue(File(assetsRoot, "rpg/objects/houses/house_tier$tier.png").isFile)
        }
        for (uiPath in listOf(
            "rpg/ui/dialog_box_faceset.png", "rpg/ui/faceset_box.png", "rpg/ui/heart_full.png",
            "rpg/ui/heart_empty.png", "rpg/ui/icon_coin.png", "rpg/ui/icon_chest.png",
            "rpg/ui/icon_quest.png", "rpg/ui/icon_map.png", "rpg/ui/icon_craft.png"
        )) {
            assertTrue("Missing UI asset: $uiPath", File(assetsRoot, uiPath).isFile)
        }
        for ((_, bgmPath) in RpgAudioManager.BGM_TRACKS) {
            assertTrue("Missing BGM OGG: $bgmPath", File(assetsRoot, bgmPath).isFile)
        }
        for (sfx in RpgSfx.entries) {
            assertTrue("Missing SFX WAV: ${sfx.filePath}", File(assetsRoot, sfx.filePath).isFile)
        }
    }
}
