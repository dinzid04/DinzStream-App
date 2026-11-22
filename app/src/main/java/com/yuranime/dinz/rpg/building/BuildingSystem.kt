package com.yuranime.dinz.rpg.building

import com.yuranime.dinz.rpg.economy.EconomySystem
import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.BuildPreviewState
import com.yuranime.dinz.rpg.model.PlacedObject
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.world.HomesteadWorld

data class BuildResult(
    val success: Boolean,
    val message: String,
    val newHouseTier: Int? = null
)

class BuildingSystem(
    private val world: HomesteadWorld,
    private val inventory: InventorySystem,
    private val economy: EconomySystem
) {
    var isBuildMode: Boolean = false
        private set
    var selectedItemId: String? = null
        private set
    var previewTileX: Int = 9
        private set
    var previewTileY: Int = 13
        private set
    var rotation: Int = 0
        private set

    fun startBuildMode(itemId: String, tileX: Int, tileY: Int) {
        val def = RpgCatalog.itemById[itemId] ?: return
        if (!def.placeable) return
        isBuildMode = true
        selectedItemId = itemId
        previewTileX = tileX
        previewTileY = tileY
        rotation = 0
    }

    fun cancelBuildMode() {
        isBuildMode = false
        selectedItemId = null
        rotation = 0
    }

    fun updatePreviewTile(tileX: Int, tileY: Int) {
        if (!isBuildMode) return
        previewTileX = tileX.coerceIn(1, HomesteadWorld.MAP_TILES_W - 2)
        previewTileY = tileY.coerceIn(1, HomesteadWorld.MAP_TILES_H - 2)
    }

    fun rotatePreview() {
        val def = selectedItemId?.let { RpgCatalog.itemById[it] } ?: return
        if (!def.rotatable) return
        rotation = (rotation + 90) % 360
    }

    fun canPlaceAt(tileX: Int, tileY: Int): Boolean {
        if (!world.isInBounds(tileX, tileY)) return false
        if (world.isMapSolidTile(tileX, tileY) || world.isWaterTile(tileX, tileY)) return false
        if (world.isFarmableTile(tileX, tileY)) return false
        if (world.getTreeAt(tileX, tileY) != null) return false
        if (world.getPlacedObjectAt(tileX, tileY) != null) return false
        // Keep house door & spawn clear
        if (tileX == HomesteadWorld.HOUSE_DOOR_X && tileY == HomesteadWorld.HOUSE_DOOR_Y) return false
        return true
    }

    fun confirmPlacement(): BuildResult {
        val itemId = selectedItemId ?: return BuildResult(false, "Pilih objek konstruksi dulu.")
        val def = RpgCatalog.itemById[itemId] ?: return BuildResult(false, "Objek tidak valid.")
        if (!canPlaceAt(previewTileX, previewTileY)) {
            return BuildResult(false, "Lokasi terhalang! Pindahkan karakter ke rumput kosong.")
        }

        // First check if player already owns the item in inventory; otherwise craft from Wood & Stone!
        if (inventory.hasItem(itemId, 1)) {
            inventory.removeItem(itemId, 1)
        } else {
            val needWood = def.woodCost
            val needStone = def.stoneCost
            if (!inventory.hasItem("res_wood", needWood) || !inventory.hasItem("res_stone", needStone)) {
                return BuildResult(false, "Butuh $needWood Kayu & $needStone Batu untuk membuat ${def.name}.")
            }
            if (needWood > 0) inventory.removeItem("res_wood", needWood)
            if (needStone > 0) inventory.removeItem("res_stone", needStone)
        }

        val obj = PlacedObject(
            id = "obj_${System.currentTimeMillis()}_${previewTileX}_${previewTileY}",
            itemId = itemId,
            tileX = previewTileX,
            tileY = previewTileY,
            rotation = rotation
        )
        world.placedObjects.add(obj)
        return BuildResult(true, "${def.name} berhasil dipasang!")
    }

    fun removeObjectAt(tileX: Int, tileY: Int): BuildResult {
        val existing = world.getPlacedObjectAt(tileX, tileY)
            ?: return BuildResult(false, "Tidak ada objek di petak ini.")
        world.placedObjects.remove(existing)
        inventory.addItem(existing.itemId, 1)
        val name = RpgCatalog.itemById[existing.itemId]?.name ?: "Objek"
        return BuildResult(true, "$name disimpan kembali ke tas.")
    }

    fun upgradeHouse(): BuildResult {
        val nextTier = world.houseTier + 1
        val spec = RpgCatalog.houseUpgrades.find { it.tier == nextTier }
            ?: return BuildResult(false, "Rumah sudah mencapai level maksimal (Tier 3)!")
        if (economy.coins < spec.coinCost) {
            return BuildResult(false, "Butuh ${spec.coinCost} koin untuk ${spec.name}.")
        }
        if (!inventory.hasItem("res_wood", spec.woodCost)) {
            return BuildResult(false, "Butuh ${spec.woodCost} Kayu untuk ${spec.name}.")
        }
        if (!inventory.hasItem("res_stone", spec.stoneCost)) {
            return BuildResult(false, "Butuh ${spec.stoneCost} Batu untuk ${spec.name}.")
        }
        economy.coins -= spec.coinCost
        inventory.removeItem("res_wood", spec.woodCost)
        inventory.removeItem("res_stone", spec.stoneCost)
        world.houseTier = nextTier
        return BuildResult(true, "Rumah di-upgrade ke ${spec.name}!", newHouseTier = nextTier)
    }

    fun toPreviewState(): BuildPreviewState = BuildPreviewState(
        active = isBuildMode,
        selectedItemId = selectedItemId,
        tileX = previewTileX,
        tileY = previewTileY,
        rotation = rotation,
        isValid = isBuildMode && canPlaceAt(previewTileX, previewTileY)
    )
}
