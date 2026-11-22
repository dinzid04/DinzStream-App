package com.yuranime.dinz.rpg.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rpg_player_state")
data class PlayerStateEntity(
    @PrimaryKey val id: Int = 1,
    val posX: Float = 144f,
    val posY: Float = 224f,
    val facing: String = "down",
    val level: Int = 1,
    val exp: Int = 0,
    val coins: Int = 120,
    val fishingLevel: Int = 1,
    val fishingExp: Int = 0,
    val farmingLevel: Int = 1,
    val farmingExp: Int = 0,
    val gatheringLevel: Int = 1,
    val gatheringExp: Int = 0,
    val houseTier: Int = 1,
    val gameTimeHours: Float = 8.5f,
    val dayCount: Int = 1,
    val selectedToolId: String = "tool_rod_basic",
    val selectedSeedId: String = "seed_carrot",
    val shirtColorHex: Long = 0xFF48A298,
    val hatColorHex: Long = 0xFFE2B65E,
    val ribbonColorHex: Long = 0xFFA84238,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "rpg_inventory")
data class InventoryItemEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int
)

@Entity(tableName = "rpg_fish_collection")
data class FishCollectionEntity(
    @PrimaryKey val fishId: String,
    val caughtCount: Int,
    val firstCaughtAtMs: Long
)

@Entity(tableName = "rpg_trees")
data class TreeStateEntity(
    @PrimaryKey val treeId: String,
    val tileX: Int,
    val tileY: Int,
    val treeType: String,
    val health: Int,
    val state: String,
    val regrowTimer: Float
)

@Entity(tableName = "rpg_crops")
data class CropPlotEntity(
    @PrimaryKey val plotKey: String,
    val tileX: Int,
    val tileY: Int,
    val isTilled: Boolean,
    val isWatered: Boolean,
    val cropType: String?,
    val stage: Int,
    val growthProgress: Float
)

@Entity(tableName = "rpg_placed_objects")
data class PlacedObjectEntity(
    @PrimaryKey val objectId: String,
    val itemId: String,
    val tileX: Int,
    val tileY: Int,
    val rotation: Int
)

@Entity(tableName = "rpg_quests")
data class QuestProgressEntity(
    @PrimaryKey val questId: String,
    val progress: Int,
    val claimed: Boolean
)

@Entity(tableName = "rpg_achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val progress: Int,
    val unlocked: Boolean
)
