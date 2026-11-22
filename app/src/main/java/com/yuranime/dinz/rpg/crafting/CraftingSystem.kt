package com.yuranime.dinz.rpg.crafting

import com.yuranime.dinz.rpg.inventory.InventorySystem
import com.yuranime.dinz.rpg.model.CraftingRecipe
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RecipeType
import com.yuranime.dinz.rpg.model.RpgCatalog

data class CraftResult(
    val success: Boolean,
    val message: String,
    val recipe: CraftingRecipe? = null
)

class CraftingSystem(private val inventory: InventorySystem) {

    fun canCraft(recipe: CraftingRecipe): Boolean {
        val outDef = RpgCatalog.itemById[recipe.outputItemId] ?: return false
        if (outDef.category == ItemCategory.TOOLS && inventory.hasTool(outDef.id)) {
            return false
        }
        return recipe.ingredients.all { (itemId, qty) -> inventory.hasItem(itemId, qty) }
    }

    fun craft(recipeId: String): CraftResult {
        val recipe = RpgCatalog.recipes.find { it.id == recipeId }
            ?: return CraftResult(false, "Resep tidak ditemukan.")
        val outDef = RpgCatalog.itemById[recipe.outputItemId]
            ?: return CraftResult(false, "Item hasil tidak valid.")

        if (outDef.category == ItemCategory.TOOLS && inventory.hasTool(outDef.id)) {
            return CraftResult(false, "Kamu sudah memiliki ${outDef.name}!")
        }

        for ((ingId, reqQty) in recipe.ingredients) {
            if (!inventory.hasItem(ingId, reqQty)) {
                val ingName = RpgCatalog.itemById[ingId]?.name ?: ingId
                return CraftResult(false, "Bahan kurang: butuh ${reqQty}x $ingName.")
            }
        }

        for ((ingId, reqQty) in recipe.ingredients) {
            inventory.removeItem(ingId, reqQty)
        }
        inventory.addItem(recipe.outputItemId, recipe.outputQty)

        val verb = if (recipe.type == RecipeType.COOKING) "Memasak" else "Membuat"
        return CraftResult(
            success = true,
            message = "Berhasil $verb ${recipe.outputQty}x ${outDef.name}!",
            recipe = recipe
        )
    }
}
