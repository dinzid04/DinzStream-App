package com.yuranime.dinz.rpg.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.RpgCatalog

/**
 * Serializer JSON penuh untuk menyimpan dan memulihkan progres Sakura Vale RPG
 * ke database Firestore milik akun yang login (kompatibel mundur dengan save v2).
 */
object RpgSaveSerializer {
    private val gson: Gson = GsonBuilder().create()

    fun toJson(bundle: RpgSaveBundle): String = gson.toJson(bundle)

    fun fromJson(json: String, expectedUserUid: String): RpgSaveBundle? {
        if (json.isBlank() || expectedUserUid.isBlank()) return null
        return runCatching {
            val raw = gson.fromJson(json, RpgSaveBundle::class.java) ?: return null
            val rp = raw.player ?: return null
            val validPreset = if (!rp.presetId.isNullOrBlank() && RpgCatalog.presetById.containsKey(rp.presetId)) {
                rp.presetId
            } else {
                "princess"
            }
            val validMapId = MapId.fromId(rp.currentMapId).id
            val safeTint = if (rp.outfitTintHex == 0L) 0xFFFFFFFFL else rp.outfitTintHex
            val safeAccent = if (rp.accentHex == 0L) 0xFFC84B31L else rp.accentHex
            val safeRegions = rp.discoveredRegionsCsv.takeIf { !it.isNullOrBlank() }
                ?: "homestead,farm_fields,village_plaza"

            val safePlayer = rp.copy(
                userUid = expectedUserUid,
                currentMapId = validMapId,
                facing = rp.facing.takeIf { !it.isNullOrBlank() } ?: "down",
                level = rp.level.coerceIn(1, 500),
                exp = rp.exp.coerceAtLeast(0),
                coins = rp.coins.coerceIn(0, 9_999_999),
                energy = (if (rp.energy <= 0) 100 else rp.energy).coerceIn(1, 100),
                fishingLevel = rp.fishingLevel.coerceIn(1, 200),
                fishingExp = rp.fishingExp.coerceAtLeast(0),
                farmingLevel = rp.farmingLevel.coerceIn(1, 200),
                farmingExp = rp.farmingExp.coerceAtLeast(0),
                gatheringLevel = rp.gatheringLevel.coerceIn(1, 200),
                gatheringExp = rp.gatheringExp.coerceAtLeast(0),
                miningLevel = rp.miningLevel.coerceIn(1, 200),
                miningExp = rp.miningExp.coerceAtLeast(0),
                cookingLevel = rp.cookingLevel.coerceIn(1, 200),
                cookingExp = rp.cookingExp.coerceAtLeast(0),
                craftingLevel = rp.craftingLevel.coerceIn(1, 200),
                craftingExp = rp.craftingExp.coerceAtLeast(0),
                houseTier = rp.houseTier.coerceIn(1, 3),
                gameTimeHours = rp.gameTimeHours.coerceIn(0f, 23.99f),
                dayCount = rp.dayCount.coerceAtLeast(1),
                selectedToolId = rp.selectedToolId.takeIf { !it.isNullOrBlank() } ?: "tool_rod_basic",
                selectedSeedId = rp.selectedSeedId.takeIf { !it.isNullOrBlank() } ?: "seed_carrot",
                presetId = validPreset,
                outfitTintHex = safeTint,
                accentHex = safeAccent,
                discoveredRegionsCsv = safeRegions,
                openedChestsCsv = rp.openedChestsCsv ?: ""
            )
            RpgSaveBundle(
                player = safePlayer,
                inventory = (raw.inventory ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                fishCollection = (raw.fishCollection ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                trees = (raw.trees ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                crops = (raw.crops ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                placedObjects = (raw.placedObjects ?: emptyList()).map {
                    it.copy(
                        userUid = expectedUserUid,
                        mapId = MapId.fromId(it.mapId).id
                    )
                },
                quests = (raw.quests ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                achievements = (raw.achievements ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                npcFriendships = (raw.npcFriendships ?: emptyList()).map { it.copy(userUid = expectedUserUid) }
            )
        }.getOrNull()
    }

    fun buildSummaryMap(bundle: RpgSaveBundle): Map<String, Any> {
        val p = bundle.player
        return mapOf(
            "userUid" to p.userUid,
            "level" to p.level.coerceIn(1, 500),
            "exp" to p.exp.coerceAtLeast(0),
            "coins" to p.coins.coerceIn(0, 9_999_999),
            "houseTier" to p.houseTier.coerceIn(1, 3),
            "fishSpeciesCaught" to bundle.fishCollection.size,
            "dayCount" to p.dayCount.coerceAtLeast(1),
            "updatedAtMs" to p.updatedAtMs
        )
    }
}
