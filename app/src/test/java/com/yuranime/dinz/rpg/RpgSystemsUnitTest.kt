package com.yuranime.dinz.rpg

import com.yuranime.dinz.rpg.building.BuildingSystem
import com.yuranime.dinz.rpg.economy.EconomySystem
import com.yuranime.dinz.rpg.farming.FarmingSystem
import com.yuranime.dinz.rpg.fishing.FishingSystem
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.model.FishRarity
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.WaterHabitat
import com.yuranime.dinz.rpg.quests.ProgressionSystem
import com.yuranime.dinz.rpg.world.HomesteadWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

        // Advance time until harvestable (stage 4)
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
    fun fishingStateMachineReachesBiteAndMinigame() {
        val fishing = FishingSystem()
        fishing.startCasting(120f, 80f, WaterHabitat.POND, "tool_rod_basic", 1, false)
        assertEquals(FishingPhase.CASTING, fishing.phase)

        var reachedBite = false
        repeat(200) {
            fishing.update(0.05f)
            if (fishing.phase == FishingPhase.BITE) {
                reachedBite = true
                return@repeat
            }
        }
        assertTrue("Fishing should reach BITE phase", reachedBite)

        fishing.onTimingTap()
        assertEquals(FishingPhase.MINIGAME, fishing.phase)
        assertNotNull(fishing.toUiState().targetFish)
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
}
