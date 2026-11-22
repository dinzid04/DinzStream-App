package com.yuranime.dinz.rpg.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Serializer JSON penuh untuk menyimpan dan memulihkan seluruh progres Willowbrook Cove RPG
 * ke database akun pengguna yang sudah login di Firebase Firestore.
 */
object RpgSaveSerializer {
    private val gson: Gson = GsonBuilder().create()

    fun toJson(bundle: RpgSaveBundle): String {
        return gson.toJson(bundle)
    }

    fun fromJson(json: String, expectedUserUid: String): RpgSaveBundle? {
        if (json.isBlank() || expectedUserUid.isBlank()) return null
        return runCatching {
            val raw = gson.fromJson(json, RpgSaveBundle::class.java) ?: return null
            val safePlayer = raw.player.copy(
                userUid = expectedUserUid,
                level = raw.player.level.coerceIn(1, 500),
                exp = raw.player.exp.coerceAtLeast(0),
                coins = raw.player.coins.coerceIn(0, 9_999_999),
                fishingLevel = raw.player.fishingLevel.coerceIn(1, 200),
                fishingExp = raw.player.fishingExp.coerceAtLeast(0),
                farmingLevel = raw.player.farmingLevel.coerceIn(1, 200),
                farmingExp = raw.player.farmingExp.coerceAtLeast(0),
                gatheringLevel = raw.player.gatheringLevel.coerceIn(1, 200),
                gatheringExp = raw.player.gatheringExp.coerceAtLeast(0),
                houseTier = raw.player.houseTier.coerceIn(1, 3),
                gameTimeHours = raw.player.gameTimeHours.coerceIn(0f, 23.99f),
                dayCount = raw.player.dayCount.coerceAtLeast(1)
            )
            RpgSaveBundle(
                player = safePlayer,
                inventory = (raw.inventory ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                fishCollection = (raw.fishCollection ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                trees = (raw.trees ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                crops = (raw.crops ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                placedObjects = (raw.placedObjects ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                quests = (raw.quests ?: emptyList()).map { it.copy(userUid = expectedUserUid) },
                achievements = (raw.achievements ?: emptyList()).map { it.copy(userUid = expectedUserUid) }
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
