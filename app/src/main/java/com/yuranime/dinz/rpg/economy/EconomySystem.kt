package com.yuranime.dinz.rpg.economy

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RpgCatalog

data class TransactionResult(
    val success: Boolean,
    val coinsDelta: Int = 0,
    val message: String
)

class EconomySystem(private val inventory: InventorySystem) {

    var coins: Int = 150

    fun buyItem(itemId: String, quantity: Int = 1): TransactionResult {
        val safeQty = quantity.coerceAtLeast(1)
        val def = RpgCatalog.itemById[itemId]
            ?: return TransactionResult(false, 0, "Item tidak ditemukan.")
        if (def.buyPrice <= 0) {
            return TransactionResult(false, 0, "${def.name} tidak dijual di toko.")
        }
        if (def.category == ItemCategory.TOOLS && inventory.hasTool(itemId)) {
            return TransactionResult(false, 0, "Kamu sudah memiliki ${def.name}!")
        }
        val actualReq = if (def.category == ItemCategory.TOOLS) 1 else safeQty
        val totalCost = def.buyPrice * actualReq
        if (coins < totalCost) {
            return TransactionResult(false, 0, "Koin tidak cukup (butuh $totalCost koin).")
        }
        val added = inventory.addItem(itemId, actualReq)
        if (added <= 0) {
            return TransactionResult(false, 0, "Tas penuh untuk ${def.name}.")
        }
        val finalCost = def.buyPrice * added
        coins -= finalCost
        return TransactionResult(true, -finalCost, "Membeli ${added}x ${def.name} (-$finalCost koin)")
    }

    fun sellItem(itemId: String, quantity: Int = 1): TransactionResult {
        val def = RpgCatalog.itemById[itemId]
            ?: return TransactionResult(false, 0, "Item tidak ditemukan.")
        if (def.sellPrice <= 0 || def.category == ItemCategory.TOOLS) {
            return TransactionResult(false, 0, "${def.name} tidak bisa dijual.")
        }
        val available = inventory.getQuantity(itemId)
        val sellQty = quantity.coerceIn(1, available.coerceAtLeast(1)).coerceAtMost(available)
        if (sellQty <= 0) {
            return TransactionResult(false, 0, "Tidak ada ${def.name} untuk dijual.")
        }
        inventory.removeItem(itemId, sellQty)
        val earned = def.sellPrice * sellQty
        coins += earned
        return TransactionResult(true, earned, "Terjual ${sellQty}x ${def.name} (+$earned koin)")
    }

    fun sellAllInCategory(category: ItemCategory): TransactionResult {
        val stacks = inventory.getItemsByCategory(category)
        if (stacks.isEmpty()) {
            return TransactionResult(false, 0, "Tidak ada ${category.label.lowercase()} di tas.")
        }
        var totalEarned = 0
        var totalCount = 0
        for (st in stacks) {
            val def = RpgCatalog.itemById[st.itemId] ?: continue
            if (def.sellPrice > 0) {
                inventory.removeItem(st.itemId, st.quantity)
                totalEarned += def.sellPrice * st.quantity
                totalCount += st.quantity
            }
        }
        if (totalCount == 0) {
            return TransactionResult(false, 0, "Tidak ada item bernilai jual.")
        }
        coins += totalEarned
        return TransactionResult(true, totalEarned, "Terjual $totalCount ${category.label} (+$totalEarned koin!)")
    }

    fun sellAllFishAndCrops(): TransactionResult {
        val all = inventory.getItemsByCategory(ItemCategory.FISH) +
            inventory.getItemsByCategory(ItemCategory.CROPS) +
            inventory.getItemsByCategory(ItemCategory.FORAGE)
        if (all.isEmpty()) {
            return TransactionResult(false, 0, "Belum ada ikan, panen, atau hasil alam untuk dijual.")
        }
        var totalEarned = 0
        var totalItems = 0
        for (st in all) {
            val def = RpgCatalog.itemById[st.itemId] ?: continue
            if (def.sellPrice > 0) {
                inventory.removeItem(st.itemId, st.quantity)
                totalEarned += def.sellPrice * st.quantity
                totalItems += st.quantity
            }
        }
        coins += totalEarned
        return TransactionResult(true, totalEarned, "Semua hasil dijual ($totalItems item): +$totalEarned koin!")
    }
}
