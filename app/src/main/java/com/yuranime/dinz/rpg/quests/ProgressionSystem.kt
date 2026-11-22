package com.yuranime.dinz.rpg.quests

import com.yuranime.dinz.rpg.model.AchievementState
import com.yuranime.dinz.rpg.model.FishRarity
import com.yuranime.dinz.rpg.model.QuestState
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.persistence.AchievementEntity
import com.yuranime.dinz.rpg.persistence.FishCollectionEntity
import com.yuranime.dinz.rpg.persistence.QuestProgressEntity

class ProgressionSystem {
    var playerLevel: Int = 1
    var exp: Int = 0
    var fishingLevel: Int = 1
    var fishingExp: Int = 0
    var farmingLevel: Int = 1
    var farmingExp: Int = 0
    var gatheringLevel: Int = 1
    var gatheringExp: Int = 0

    private val fishCollection = linkedMapOf<String, Pair<Int, Long>>() // fishId -> (count, firstMs)
    private val questProgress = linkedMapOf<String, Pair<Int, Boolean>>() // id -> (progress, claimed)
    private val achievementProgress = linkedMapOf<String, Pair<Int, Boolean>>() // id -> (progress, unlocked)

    init {
        resetDefaults()
    }

    fun resetDefaults() {
        questProgress.clear()
        RpgCatalog.dailyQuests.forEach { questProgress[it.id] = 0 to false }
        achievementProgress.clear()
        RpgCatalog.achievements.forEach { achievementProgress[it.id] = 0 to false }
    }

    fun loadSaved(
        fishEntities: List<FishCollectionEntity>,
        questEntities: List<QuestProgressEntity>,
        achievementEntities: List<AchievementEntity>
    ) {
        fishCollection.clear()
        fishEntities.forEach { fishCollection[it.fishId] = it.caughtCount to it.firstCaughtAtMs }
        resetDefaults()
        questEntities.forEach { q ->
            if (questProgress.containsKey(q.questId)) {
                questProgress[q.questId] = q.progress to q.claimed
            }
        }
        achievementEntities.forEach { a ->
            if (achievementProgress.containsKey(a.achievementId)) {
                achievementProgress[a.achievementId] = a.progress to a.unlocked
            }
        }
    }

    fun addPlayerExp(amount: Int): Boolean {
        if (amount <= 0) return false
        exp += amount
        var leveledUp = false
        while (exp >= RpgCatalog.expRequiredForLevel(playerLevel)) {
            exp -= RpgCatalog.expRequiredForLevel(playerLevel)
            playerLevel++
            leveledUp = true
        }
        return leveledUp
    }

    fun addFishingExp(amount: Int) {
        fishingExp += amount
        while (fishingExp >= 60 + (fishingLevel - 1) * 40) {
            fishingExp -= 60 + (fishingLevel - 1) * 40
            fishingLevel++
        }
    }

    fun addFarmingExp(amount: Int) {
        farmingExp += amount
        while (farmingExp >= 60 + (farmingLevel - 1) * 40) {
            farmingExp -= 60 + (farmingLevel - 1) * 40
            farmingLevel++
        }
    }

    fun addGatheringExp(amount: Int) {
        gatheringExp += amount
        while (gatheringExp >= 60 + (gatheringLevel - 1) * 40) {
            gatheringExp -= 60 + (gatheringLevel - 1) * 40
            gatheringLevel++
        }
    }

    fun recordFishCaught(fishId: String, rarity: FishRarity): List<String> {
        val notices = mutableListOf<String>()
        val prev = fishCollection[fishId]
        fishCollection[fishId] = ((prev?.first ?: 0) + 1) to (prev?.second ?: System.currentTimeMillis())
        addFishingExp(rarity.expReward)
        if (addPlayerExp(rarity.expReward)) notices.add("Naik ke Level $playerLevel!")

        incrementQuest("q_catch_3_fish", 1)
        incrementAchievement("a_first_fish", 1)?.let { notices.add(it) }
        incrementAchievement("a_fish_15", 1)?.let { notices.add(it) }
        if (rarity == FishRarity.RARE || rarity == FishRarity.EPIC || rarity == FishRarity.LEGENDARY) {
            incrementAchievement("a_rare_fish", 1)?.let { notices.add(it) }
        }
        setAchievementProgress("a_collector_6", fishCollection.size)?.let { notices.add(it) }
        return notices
    }

    fun recordTreeChopped(woodGained: Int, expReward: Int): List<String> {
        val notices = mutableListOf<String>()
        addGatheringExp(expReward)
        if (addPlayerExp(expReward)) notices.add("Naik ke Level $playerLevel!")
        incrementQuest("q_collect_10_wood", woodGained)
        incrementAchievement("a_chop_10", 1)?.let { notices.add(it) }
        return notices
    }

    fun recordCropPlanted(): List<String> {
        val notices = mutableListOf<String>()
        addFarmingExp(8)
        if (addPlayerExp(8)) notices.add("Naik ke Level $playerLevel!")
        incrementQuest("q_plant_5_crops", 1)
        return notices
    }

    fun recordCropHarvested(expReward: Int): List<String> {
        val notices = mutableListOf<String>()
        addFarmingExp(expReward)
        if (addPlayerExp(expReward)) notices.add("Naik ke Level $playerLevel!")
        incrementQuest("q_harvest_3_crops", 1)
        incrementAchievement("a_harvest_10", 1)?.let { notices.add(it) }
        return notices
    }

    fun recordCoinsEarned(coinsEarned: Int) {
        if (coinsEarned > 0) incrementQuest("q_earn_150_coins", coinsEarned)
    }

    fun recordObjectBuilt(): List<String> {
        val notices = mutableListOf<String>()
        if (addPlayerExp(15)) notices.add("Naik ke Level $playerLevel!")
        incrementQuest("q_build_2_items", 1)
        return notices
    }

    fun recordHouseUpgraded(newTier: Int): List<String> {
        val notices = mutableListOf<String>()
        if (addPlayerExp(newTier * 45)) notices.add("Naik ke Level $playerLevel!")
        if (newTier >= 2) setAchievementProgress("a_house_tier2", 1)?.let { notices.add(it) }
        if (newTier >= 3) setAchievementProgress("a_house_tier3", 1)?.let { notices.add(it) }
        return notices
    }

    private fun incrementQuest(questId: String, delta: Int) {
        val def = RpgCatalog.dailyQuests.find { it.id == questId } ?: return
        val cur = questProgress[questId] ?: (0 to false)
        if (!cur.second) {
            questProgress[questId] = (cur.first + delta).coerceAtMost(def.targetCount) to false
        }
    }

    private fun incrementAchievement(achId: String, delta: Int): String? {
        val cur = achievementProgress[achId] ?: (0 to false)
        return setAchievementProgress(achId, cur.first + delta)
    }

    private fun setAchievementProgress(achId: String, newValue: Int): String? {
        val def = RpgCatalog.achievements.find { it.id == achId } ?: return null
        val cur = achievementProgress[achId] ?: (0 to false)
        if (cur.second) return null
        val clamped = newValue.coerceAtMost(def.targetCount)
        val unlockedNow = clamped >= def.targetCount
        achievementProgress[achId] = clamped to unlockedNow
        return if (unlockedNow) {
            addPlayerExp(def.expReward)
            "Pencapaian Terbuka: ${def.title} (+${def.coinReward} koin!)"
        } else null
    }

    fun claimQuest(questId: String): Pair<Int, Int>? {
        val def = RpgCatalog.dailyQuests.find { it.id == questId } ?: return null
        val cur = questProgress[questId] ?: return null
        if (cur.second || cur.first < def.targetCount) return null
        questProgress[questId] = cur.first to true
        addPlayerExp(def.expReward)
        return def.coinReward to def.expReward
    }

    fun getCaughtFishCounts(): Map<String, Int> = fishCollection.mapValues { it.value.first }

    fun getQuestStates(): List<QuestState> = RpgCatalog.dailyQuests.map { def ->
        val st = questProgress[def.id] ?: (0 to false)
        QuestState(def = def, progress = st.first, claimed = st.second)
    }

    fun getAchievementStates(): List<AchievementState> = RpgCatalog.achievements.map { def ->
        val st = achievementProgress[def.id] ?: (0 to false)
        AchievementState(def = def, progress = st.first, unlocked = st.second)
    }

    fun toFishEntities(userUid: String): List<FishCollectionEntity> = fishCollection.map { (id, pair) ->
        FishCollectionEntity(userUid, id, pair.first, pair.second)
    }

    fun toQuestEntities(userUid: String): List<QuestProgressEntity> = questProgress.map { (id, pair) ->
        QuestProgressEntity(userUid, id, pair.first, pair.second)
    }

    fun toAchievementEntities(userUid: String): List<AchievementEntity> = achievementProgress.map { (id, pair) ->
        AchievementEntity(userUid, id, pair.first, pair.second)
    }
}
