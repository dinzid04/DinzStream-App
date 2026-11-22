package com.yuranime.dinz.rpg.inventory

import com.yuranime.dinz.rpg.model.InventoryStack
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.persistence.InventoryItemEntity

enum class InventorySortMode(val label: String) {
    CATEGORY("Kategori"),
    NAME("Nama"),
    VALUE("Harga"),
    QUANTITY("Jumlah")
}

class InventorySystem {
    private val items = linkedMapOf<String, Int>()

    private val coreStarterTools = listOf(
        "tool_rod_basic",
        "tool_axe",
        "tool_pickaxe",
        "tool_hoe",
        "tool_watering_can",
        "tool_hammer"
    )

    fun initStarterKit() {
        items.clear()
        coreStarterTools.forEach { items[it] = 1 }
        // Starter seeds, food & building supplies so player can immediately enjoy every RPG loop
        items["seed_carrot"] = 6
        items["seed_potato"] = 4
        items["seed_tomato"] = 3
        items["seed_strawberry"] = 2
        items["seed_wheat"] = 3
        items["seed_oak"] = 2
        items["seed_sakura"] = 2
        items["food_onigiri"] = 3
        items["food_honey_tea"] = 2
        items["res_wood"] = 12
        items["res_stone"] = 8
        items["fence_wood"] = 4
        items["lamp_post"] = 2
        items["flower_pot"] = 2
    }

    fun loadFromEntities(entities: List<InventoryItemEntity>) {
        if (entities.isEmpty()) {
            initStarterKit()
            return
        }
        items.clear()
        entities.forEach { e ->
            if (e.quantity > 0 && RpgCatalog.itemById.containsKey(e.itemId)) {
                items[e.itemId] = e.quantity
            }
        }
        coreStarterTools.forEach { toolId ->
            if ((items[toolId] ?: 0) <= 0) items[toolId] = 1
        }
    }

    fun toEntities(userUid: String): List<InventoryItemEntity> {
        return items.filter { it.value > 0 }.map { InventoryItemEntity(userUid, it.key, it.value) }
    }

    fun addItem(itemId: String, amount: Int): Int {
        if (amount <= 0) return 0
        val def = RpgCatalog.itemById[itemId] ?: return 0
        val current = items[itemId] ?: 0
        val newQty = (current + amount).coerceAtMost(def.maxStack)
        val actuallyAdded = newQty - current
        if (newQty > 0) items[itemId] = newQty
        return actuallyAdded
    }

    fun removeItem(itemId: String, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = items[itemId] ?: 0
        if (current < amount) return false
        val remaining = current - amount
        if (remaining <= 0) {
            items.remove(itemId)
        } else {
            items[itemId] = remaining
        }
        return true
    }

    fun getQuantity(itemId: String): Int = items[itemId] ?: 0

    fun hasItem(itemId: String, amount: Int = 1): Boolean = getQuantity(itemId) >= amount

    fun hasTool(toolId: String): Boolean = hasItem(toolId, 1)

    fun getBestRodId(): String = when {
        hasTool("tool_rod_golden") -> "tool_rod_golden"
        hasTool("tool_rod_bamboo") -> "tool_rod_bamboo"
        else -> "tool_rod_basic"
    }

    fun getBestAxeDamage(): Int = if (hasTool("tool_axe_iron")) 2 else 1

    fun getBestPickaxeDamage(): Int = if (hasTool("tool_pickaxe_gold")) 2 else 1

    fun getStacks(sortMode: InventorySortMode = InventorySortMode.CATEGORY): List<InventoryStack> {
        val list = items.filter { it.value > 0 }.map { InventoryStack(it.key, it.value) }
        return when (sortMode) {
            InventorySortMode.CATEGORY -> list.sortedWith(
                compareBy<InventoryStack> { RpgCatalog.itemById[it.itemId]?.category?.ordinal ?: 99 }
                    .thenBy { RpgCatalog.itemById[it.itemId]?.name ?: it.itemId }
            )
            InventorySortMode.NAME -> list.sortedBy { RpgCatalog.itemById[it.itemId]?.name ?: it.itemId }
            InventorySortMode.VALUE -> list.sortedByDescending { RpgCatalog.itemById[it.itemId]?.sellPrice ?: 0 }
            InventorySortMode.QUANTITY -> list.sortedByDescending { it.quantity }
        }
    }

    fun getItemsByCategory(category: ItemCategory): List<InventoryStack> {
        return getStacks().filter { RpgCatalog.itemById[it.itemId]?.category == category }
    }

    fun getGiftableItems(): List<InventoryStack> {
        return getStacks(InventorySortMode.VALUE).filter { stack ->
            val cat = RpgCatalog.itemById[stack.itemId]?.category
            cat != null && cat != ItemCategory.TOOLS
        }
    }
}
