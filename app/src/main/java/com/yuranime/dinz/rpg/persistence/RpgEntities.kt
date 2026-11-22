package com.yuranime.dinz.rpg.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rpg_player_state")
data class PlayerStateEntity(
    @PrimaryKey val userUid: String,
    val currentMapId: String = "overworld",
    val posX: Float = 14f * 16f + 8f,
    val posY: Float = 43f * 16f + 8f,
    val facing: String = "down",
    val level: Int = 1,
    val exp: Int = 0,
    val coins: Int = 150,
    val energy: Int = 100,
    val fishingLevel: Int = 1,
    val fishingExp: Int = 0,
    val farmingLevel: Int = 1,
    val farmingExp: Int = 0,
    val gatheringLevel: Int = 1,
    val gatheringExp: Int = 0,
    val miningLevel: Int = 1,
    val miningExp: Int = 0,
    val cookingLevel: Int = 1,
    val cookingExp: Int = 0,
    val craftingLevel: Int = 1,
    val craftingExp: Int = 0,
    val houseTier: Int = 1,
    val gameTimeHours: Float = 8.0f,
    val dayCount: Int = 1,
    val selectedToolId: String = "tool_rod_basic",
    val selectedSeedId: String = "seed_carrot",
    val presetId: String = "princess",
    val outfitTintHex: Long = 0xFFFFFFFF,
    val accentHex: Long = 0xFFC84B31,
    val discoveredRegionsCsv: String = "homestead,farm_fields,village_plaza",
    val openedChestsCsv: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "rpg_inventory", primaryKeys = ["userUid", "itemId"])
data class InventoryItemEntity(
    val userUid: String,
    val itemId: String,
    val quantity: Int
)

@Entity(tableName = "rpg_fish_collection", primaryKeys = ["userUid", "fishId"])
data class FishCollectionEntity(
    val userUid: String,
    val fishId: String,
    val caughtCount: Int,
    val firstCaughtAtMs: Long
)

@Entity(tableName = "rpg_trees", primaryKeys = ["userUid", "treeId"])
data class TreeStateEntity(
    val userUid: String,
    val treeId: String,
    val tileX: Int,
    val tileY: Int,
    val treeType: String,
    val health: Int,
    val state: String,
    val regrowTimer: Float
)

@Entity(tableName = "rpg_crops", primaryKeys = ["userUid", "plotKey"])
data class CropPlotEntity(
    val userUid: String,
    val plotKey: String,
    val tileX: Int,
    val tileY: Int,
    val isTilled: Boolean,
    val isWatered: Boolean,
    val cropType: String?,
    val stage: Int,
    val growthProgress: Float
)

@Entity(tableName = "rpg_placed_objects", primaryKeys = ["userUid", "objectId"])
data class PlacedObjectEntity(
    val userUid: String,
    val objectId: String,
    val mapId: String = "overworld",
    val itemId: String,
    val tileX: Int,
    val tileY: Int,
    val rotation: Int
)

@Entity(tableName = "rpg_quests", primaryKeys = ["userUid", "questId"])
data class QuestProgressEntity(
    val userUid: String,
    val questId: String,
    val progress: Int,
    val claimed: Boolean
)

@Entity(tableName = "rpg_achievements", primaryKeys = ["userUid", "achievementId"])
data class AchievementEntity(
    val userUid: String,
    val achievementId: String,
    val progress: Int,
    val unlocked: Boolean
)

@Entity(tableName = "rpg_npc_friendship", primaryKeys = ["userUid", "npcId"])
data class NpcFriendshipEntity(
    val userUid: String,
    val npcId: String,
    val points: Int,
    val talkedToday: Boolean,
    val giftedToday: Boolean
)
