package com.yuranime.dinz.rpg.mining

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.OreNode
import com.yuranime.dinz.rpg.model.OreType
import com.yuranime.dinz.rpg.model.RpgCatalog
import kotlin.random.Random

data class MineResult(
    val hit: Boolean,
    val shattered: Boolean,
    val droppedItemId: String? = null,
    val droppedAmount: Int = 0,
    val bonusGemId: String? = null,
    val expReward: Int = 0,
    val message: String
)

class MiningSystem(private val inventory: InventorySystem) {

    fun mineNode(node: OreNode, pickaxeDamage: Int, hasGoldPickaxe: Boolean): MineResult {
        if (node.depleted) {
            return MineResult(false, false, message = "Batu tambang ini sudah habis.")
        }
        node.health = (node.health - pickaxeDamage.coerceAtLeast(1)).coerceAtLeast(0)
        node.shakeTimer = 0.28f

        if (node.health > 0) {
            return MineResult(
                hit = true,
                shattered = false,
                message = "${node.type.displayName} (${node.health}/${node.type.maxHealth})"
            )
        }

        node.depleted = true
        node.respawnTimer = node.type.respawnSeconds
        val extra = if (hasGoldPickaxe && Random.nextFloat() < 0.45f) 1 else 0
        val amount = node.type.dropAmount + extra
        inventory.addItem(node.type.dropItemId, amount)

        // Small chance for bonus stone or rare crystal on Gold Pickaxe!
        var bonusGem: String? = null
        if (node.type != OreType.STONE && Random.nextFloat() < (if (hasGoldPickaxe) 0.35f else 0.14f)) {
            val gemId = if (Random.nextBoolean()) "gem_amethyst" else "gem_emerald"
            inventory.addItem(gemId, 1)
            bonusGem = gemId
        }

        val itemName = RpgCatalog.itemById[node.type.dropItemId]?.name ?: node.type.displayName
        val bonusText = bonusGem?.let { " + 1 ${RpgCatalog.itemById[it]?.name ?: "Kristal"}" } ?: ""
        return MineResult(
            hit = true,
            shattered = true,
            droppedItemId = node.type.dropItemId,
            droppedAmount = amount,
            bonusGemId = bonusGem,
            expReward = node.type.expReward,
            message = "+${amount}x $itemName$bonusText!"
        )
    }
}
