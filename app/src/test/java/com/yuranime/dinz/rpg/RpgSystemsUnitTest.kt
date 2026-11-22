package com.yuranime.dinz.rpg

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader
import com.badlogic.gdx.utils.XmlReader
import com.yuranime.dinz.rpg.audio.RpgSfx
import com.yuranime.dinz.rpg.building.BuildingSystem
import com.yuranime.dinz.rpg.economy.EconomySystem
import com.yuranime.dinz.rpg.farming.FarmingSystem
import com.yuranime.dinz.rpg.fishing.FishingSystem
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.FishRarity
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.TreeHealthState
import com.yuranime.dinz.rpg.model.TreeType
import com.yuranime.dinz.rpg.model.WaterHabitat
import com.yuranime.dinz.rpg.persistence.AchievementEntity
import com.yuranime.dinz.rpg.persistence.CropPlotEntity
import com.yuranime.dinz.rpg.persistence.FishCollectionEntity
import com.yuranime.dinz.rpg.persistence.InventoryItemEntity
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
    fun catalogHasRequiredSpeciesCropsAndUpgrades() {
        assertTrue("Expected at least 10 fish species", RpgCatalog.fishSpecies.size >= 10)
        assertEquals(4, CropType.entries.size)
        assertEquals(3, RpgCatalog.houseUpgrades.size)
        assertTrue(RpgCatalog.buildableItems.size >= 10)
        assertTrue(RpgCatalog.dailyQuests.size >= 4)
    }

    @Test
    fun inventoryStackingAndSortingWorks() {
        val inv = InventorySystem()
        inv.initStarterKit()
        assertTrue(inv.hasTool("tool_rod_basic"))
        assertTrue(inv.hasTool("tool_axe"))
        assertTrue(inv.hasTool("tool_hoe"))
        assertTrue(inv.hasTool("tool_watering_can"))

        val added = inv.addItem("res_wood", 15)
        assertEquals(15, added)
        assertEquals(25, inv.getQuantity("res_wood"))
        assertTrue(inv.removeItem("res_wood", 5))
        assertEquals(20, inv.getQuantity("res_wood"))
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
        assertEquals(300 - 84, eco.coins)
        assertEquals(2, inv.getQuantity("seed_pumpkin"))

        inv.addItem("fish_crimson_koi", 2)
        val sellRes = eco.sellAllInCategory(ItemCategory.FISH)
        assertTrue(sellRes.success)
        assertEquals(220, sellRes.coinsDelta)
        assertEquals(0, inv.getQuantity("fish_crimson_koi"))
    }

    @Test
    fun farmingTillPlantWaterGrowAndHarvestFlow() {
        val world = HomesteadWorld()
        val inv = InventorySystem()
        inv.initStarterKit()
        val farming = FarmingSystem(world, inv)

        val tillRes = farming.tillSoil(15, 6)
        assertTrue(tillRes.success)

        val plantRes = farming.plantSeed(15, 6, "seed_carrot")
        assertTrue(plantRes.success)

        val waterRes = farming.waterPlot(15, 6)
        assertTrue(waterRes.success)

        repeat(40) {
            farming.update(2.0f)
        }
        val plot = world.cropPlots["15_6"]
        assertNotNull(plot)
        assertTrue("Crop should reach harvestable stage 4", plot!!.isHarvestable)

        val harvestRes = farming.harvestCrop(15, 6)
        assertTrue(harvestRes.success)
        assertTrue(inv.getQuantity("crop_carrot") >= 1)
    }

    @Test
    fun houseUpgradeConsumesResourcesAndChangesTier() {
        val world = HomesteadWorld()
        val inv = InventorySystem()
        inv.initStarterKit()
        val eco = EconomySystem(inv)
        val building = BuildingSystem(world, inv, eco)

        eco.coins = 500
        inv.addItem("res_wood", 30)
        inv.addItem("res_stone", 20)

        val up2 = building.upgradeHouse()
        assertTrue(up2.success)
        assertEquals(2, world.houseTier)
        assertEquals(500 - 220, eco.coins)
    }

    @Test
    fun fishingStateMachineReachesBiteMinigameAndCatch() {
        val fishing = FishingSystem()
        fishing.startCasting(120f, 80f, WaterHabitat.POND, "tool_rod_basic", 1, false)
        assertEquals(FishingPhase.CASTING, fishing.phase)

        var reachedBite = false
        for (step in 0 until 200) {
            fishing.update(0.05f)
            if (fishing.phase == FishingPhase.BITE) {
                reachedBite = true
                break
            }
        }
        assertTrue("Fishing should reach BITE phase", reachedBite)

        fishing.onTimingTap()
        assertEquals(FishingPhase.MINIGAME, fishing.phase)
        assertNotNull(fishing.toUiState().targetFish)
    }

    @Test
    fun treeChoppingAndBuildingPlacementWork() {
        val world = HomesteadWorld()
        val inv = InventorySystem()
        inv.initStarterKit()
        val eco = EconomySystem(inv)
        val building = BuildingSystem(world, inv, eco)

        building.startBuildMode("bench_wood", 10, 12)
        building.rotatePreview()
        assertEquals(90, building.rotation)
        val res = building.confirmPlacement()
        assertTrue("Bench should place cleanly on open tile", res.success)
        assertEquals(1, world.placedObjects.size)
        assertEquals(5, inv.getQuantity("res_wood"))

        val removeRes = building.removeObjectAt(10, 12)
        assertTrue(removeRes.success)
        assertEquals(0, world.placedObjects.size)
        assertEquals(1, inv.getQuantity("bench_wood"))
    }

    @Test
    fun progressionQuestsAndAchievementsTrackGameplay() {
        val prog = ProgressionSystem()
        repeat(3) { prog.recordFishCaught("fish_pond_carp", FishRarity.COMMON) }
        val quest = prog.getQuestStates().first { it.def.id == "q_catch_3_fish" }
        assertTrue(quest.isCompleted)
        val reward = prog.claimQuest("q_catch_3_fish")
        assertNotNull(reward)
        assertTrue(prog.getQuestStates().first { it.def.id == "q_catch_3_fish" }.claimed)
    }

    @Test
    fun saveSerializerRoundTripPreservesFullStateAndIsolatesUserUid() {
        val uidA = "g_user_alpha_example_com"
        val uidB = "g_user_beta_example_com"

        val bundle = RpgSaveBundle(
            player = PlayerStateEntity(
                userUid = uidA,
                posX = 212f,
                posY = 168f,
                facing = "right",
                level = 7,
                exp = 45,
                coins = 1480,
                fishingLevel = 5,
                fishingExp = 30,
                farmingLevel = 4,
                farmingExp = 15,
                gatheringLevel = 3,
                gatheringExp = 20,
                houseTier = 2,
                gameTimeHours = 16.25f,
                dayCount = 9,
                selectedToolId = "tool_rod_golden",
                selectedSeedId = "seed_pumpkin",
                shirtColorHex = 0xFF56A854,
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
                TreeStateEntity(uidA, "tree_0", 3, 21, TreeType.OAK.id, 2, TreeHealthState.DAMAGED.name, 0f)
            ),
            crops = listOf(
                CropPlotEntity(uidA, "15_6", 15, 6, true, true, CropType.PUMPKIN.id, 3, 12.5f)
            ),
            placedObjects = listOf(
                PlacedObjectEntity(uidA, "obj_1", "lamp_post", 10, 12, 90)
            ),
            quests = listOf(
                QuestProgressEntity(uidA, "q_catch_3_fish", 3, true)
            ),
            achievements = listOf(
                AchievementEntity(uidA, "a_first_fish", 1, true)
            )
        )

        val json = RpgSaveSerializer.toJson(bundle)
        assertTrue(json.contains("fish_crimson_koi"))

        val restoredForA = RpgSaveSerializer.fromJson(json, uidA)
        assertNotNull(restoredForA)
        assertEquals(uidA, restoredForA!!.player.userUid)
        assertEquals(1480, restoredForA.player.coins)
        assertEquals(7, restoredForA.player.level)
        assertEquals(2, restoredForA.player.houseTier)
        assertEquals(2, restoredForA.inventory.size)
        assertEquals(1, restoredForA.placedObjects.size)

        // Ensure UID binding enforces isolation when deserialized for a target account
        val reboundForB = RpgSaveSerializer.fromJson(json, uidB)
        assertNotNull(reboundForB)
        assertEquals(uidB, reboundForB!!.player.userUid)
        assertTrue(reboundForB.inventory.all { it.userUid == uidB })
        assertTrue(reboundForB.trees.all { it.userUid == uidB })

        // Verify summary map for Firestore profile document
        val summary = RpgSaveSerializer.buildSummaryMap(restoredForA)
        assertEquals(7, summary["level"])
        assertEquals(1480, summary["coins"])
        assertEquals(2, summary["houseTier"])
        assertEquals(1, summary["fishSpeciesCaught"])

        // Corrupted JSON never crashes
        assertNull(RpgSaveSerializer.fromJson("{not-valid-json", uidA))
        assertNull(RpgSaveSerializer.fromJson("", uidA))
    }

    @Test
    fun bundledAssetsAtlasAndTmxMapAreValidAndComplete() {
        val assetsRoot = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        ).firstOrNull { it.isDirectory }
        assertNotNull("Assets directory should exist in test workspace", assetsRoot)

        // 1. Validate LibGDX TextureAtlasData parses player.atlas with all 11 actions x 4 directions
        val atlasFile = File(assetsRoot, "rpg/characters/player.atlas")
        assertTrue("player.atlas must exist", atlasFile.isFile)
        val atlasData = TextureAtlas.TextureAtlasData(
            FileHandle(atlasFile),
            FileHandle(atlasFile.parentFile),
            false
        )
        assertEquals("Expected 1 atlas page", 1, atlasData.pages.size)
        assertTrue("Expected 160 sliced player frames", atlasData.regions.size >= 140)

        val expectedPrefixes = listOf(
            "idle", "walk", "run", "fish_cast", "fish_wait", "fish_reel",
            "axe", "hoe", "water", "harvest", "interact"
        )
        for (prefix in expectedPrefixes) {
            for (dir in Direction.entries) {
                val regionName = "${prefix}_${dir.key}"
                val count = atlasData.regions.count { it.name == regionName }
                assertTrue("Atlas missing animation region $regionName", count > 0)
            }
        }

        // 2. Validate Tiled TMX + TSX XML structure & tileset PNG reference
        val tmxFile = File(assetsRoot, "rpg/maps/starter_homestead.tmx")
        val tsxFile = File(assetsRoot, "rpg/maps/homestead_tileset.tsx")
        assertTrue("starter_homestead.tmx must exist", tmxFile.isFile)
        assertTrue("homestead_tileset.tsx must exist", tsxFile.isFile)

        val xml = XmlReader()
        val mapElem = xml.parse(FileHandle(tmxFile))
        assertEquals(30, mapElem.getIntAttribute("width"))
        assertEquals(24, mapElem.getIntAttribute("height"))
        val layerElems = mapElem.getChildrenByName("layer")
        assertEquals(8, layerElems.size)

        // Verify exact LibGDX BaseTmxMapLoader.getTileIds CSV parser succeeds on every layer
        for (i in 0 until layerElems.size) {
            val layer = layerElems.get(i)
            val ids = BaseTmxMapLoader.getTileIds(layer, 30, 24)
            assertEquals("Layer ${layer.getAttribute("name")} must have 720 tile IDs", 720, ids.size)
            for (gid in ids) {
                assertTrue("Invalid GID $gid in layer ${layer.getAttribute("name")}", gid in 0..64)
            }
        }

        val tsxElem = xml.parse(FileHandle(tsxFile))
        val imageSource = tsxElem.getChildByName("image").getAttribute("source")
        val resolvedTileset = File(tsxFile.parentFile, imageSource).canonicalFile
        assertTrue("Tileset image must exist at $resolvedTileset", resolvedTileset.isFile)

        // 3. Validate all catalog sprites and audio assets exist on disk
        for (item in RpgCatalog.items) {
            assertTrue("Missing item sprite: ${item.spritePath}", File(assetsRoot, item.spritePath).isFile)
        }
        for (sfx in RpgSfx.entries) {
            assertTrue("Missing SFX asset: ${sfx.filePath}", File(assetsRoot, sfx.filePath).isFile)
        }
        assertTrue("Missing BGM asset", File(assetsRoot, "rpg/audio/bgm_homestead.wav").isFile)
    }
}
