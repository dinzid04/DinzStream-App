package com.yuranime.dinz.rpg.farming

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.CropPlot
import com.yuranime.dinz.rpg.model.CropType
import com.yuranime.dinz.rpg.world.HomesteadWorld
import kotlin.random.Random

data class FarmActionResult(
    val success: Boolean,
    val message: String,
    val harvestedCrop: CropType? = null,
    val planted: Boolean = false,
    val watered: Boolean = false,
    val tilled: Boolean = false
)

class FarmingSystem(
    private val world: HomesteadWorld,
    private val inventory: InventorySystem
) {
    fun tillSoil(tileX: Int, tileY: Int): FarmActionResult {
        if (!world.isFarmableTile(tileX, tileY)) {
            return FarmActionResult(false, "Hanya lahan kebun berpagar yang bisa dicangkul.")
        }
        val key = "${tileX}_${tileY}"
        val existing = world.cropPlots[key]
        if (existing != null && existing.isTilled) {
            return FarmActionResult(false, "Tanah sudah digemburkan.")
        }
        world.cropPlots[key] = CropPlot(tileX = tileX, tileY = tileY, isTilled = true, isWatered = world.isRaining)
        return FarmActionResult(true, "Tanah digemburkan! Siap ditanami bibit.", tilled = true)
    }

    fun plantSeed(tileX: Int, tileY: Int, seedItemId: String): FarmActionResult {
        if (!world.isFarmableTile(tileX, tileY)) {
            return FarmActionResult(false, "Tanam bibit di petak lahan kebun.")
        }
        val cropType = CropType.entries.find { it.seedItemId == seedItemId }
            ?: return FarmActionResult(false, "Pilih bibit sayuran terlebih dahulu.")
        if (!inventory.hasItem(seedItemId, 1)) {
            return FarmActionResult(false, "Stok bibit ${cropType.displayName} habis! Beli di Merchant.")
        }
        val key = "${tileX}_${tileY}"
        val plot = world.cropPlots[key] ?: CropPlot(tileX, tileY, isTilled = true).also {
            world.cropPlots[key] = it
        }
        if (!plot.isTilled) {
            plot.isTilled = true
        }
        if (plot.cropType != null) {
            return FarmActionResult(false, "Petak ini sudah ada tanamannya.")
        }
        inventory.removeItem(seedItemId, 1)
        plot.cropType = cropType
        plot.stage = 0
        plot.growthProgress = 0f
        if (world.isRaining) plot.isWatered = true
        return FarmActionResult(true, "Menanam ${cropType.displayName}!", planted = true)
    }

    fun waterPlot(tileX: Int, tileY: Int): FarmActionResult {
        val key = "${tileX}_${tileY}"
        val plot = world.cropPlots[key]
            ?: return FarmActionResult(false, "Gemburkan tanah dulu sebelum menyiram.")
        if (!plot.isTilled) {
            return FarmActionResult(false, "Tanah belum dicangkul.")
        }
        plot.isWatered = true
        return FarmActionResult(true, "Tanaman disiram! Pertumbuhan 2x lebih cepat.", watered = true)
    }

    fun harvestCrop(tileX: Int, tileY: Int): FarmActionResult {
        val key = "${tileX}_${tileY}"
        val plot = world.cropPlots[key]
            ?: return FarmActionResult(false, "Tidak ada tanaman di petak ini.")
        val crop = plot.cropType
        if (crop == null || !plot.isHarvestable) {
            return FarmActionResult(false, "Tanaman belum matang untuk dipanen.")
        }
        val qty = if (Random.nextFloat() < 0.30f) 2 else 1
        inventory.addItem(crop.harvestItemId, qty)
        // Small chance of bonus seed
        if (Random.nextFloat() < 0.35f) {
            inventory.addItem(crop.seedItemId, 1)
        }
        plot.cropType = null
        plot.stage = 0
        plot.growthProgress = 0f
        plot.isWatered = world.isRaining
        return FarmActionResult(
            success = true,
            message = "Panen ${qty}x ${crop.displayName}!",
            harvestedCrop = crop
        )
    }

    fun update(dt: Float) {
        for (plot in world.cropPlots.values) {
            if (world.isRaining && plot.isTilled) {
                plot.isWatered = true
            }
            val crop = plot.cropType ?: continue
            if (plot.stage >= 4) continue
            // Watered crops grow 2.2x faster than dry crops!
            val multiplier = if (plot.isWatered) 2.2f else 0.75f
            plot.growthProgress += (dt * multiplier) / crop.stageDurationSeconds
            while (plot.growthProgress >= 1f && plot.stage < 4) {
                plot.growthProgress -= 1f
                plot.stage++
                // Soil dries out each stage unless raining
                if (!world.isRaining && plot.stage < 4) {
                    plot.isWatered = false
                }
            }
            if (plot.stage >= 4) {
                plot.growthProgress = 1f
            }
        }
    }
}
