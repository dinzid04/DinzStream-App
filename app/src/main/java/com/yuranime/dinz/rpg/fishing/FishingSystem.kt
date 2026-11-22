package com.yuranime.dinz.rpg.fishing

import com.yuranime.dinz.rpg.model.FishRarity
import com.yuranime.dinz.rpg.model.FishSpecies
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.FishingUiState
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.WaterHabitat
import kotlin.math.abs
import kotlin.random.Random

data class FishingEventResult(
    val biteTriggered: Boolean = false,
    val caughtFish: FishSpecies? = null,
    val escaped: Boolean = false,
    val splashTriggered: Boolean = false
)

class FishingSystem {
    var phase: FishingPhase = FishingPhase.IDLE
        private set
    var bobberX: Float = 0f
        private set
    var bobberY: Float = 0f
        private set
    var splashTimer: Float = 0f
        private set

    private var phaseTimer: Float = 0f
    private var waitTargetDuration: Float = 2.4f
    private var activeFish: FishSpecies? = null
    private var indicatorPos: Float = 0.5f
    private var indicatorVel: Float = 0.72f
    private var sweetCenter: Float = 0.5f
    private var sweetHalfW: Float = 0.145f
    private var catchProgress: Float = 0.28f
    private var tensionTimeLeft: Float = 11f
    private var lastMessage: String = ""
    private var activeHabitat: WaterHabitat = WaterHabitat.LAKE
    private var isNight: Boolean = false
    private var rodId: String = "tool_rod_basic"
    private var fishingLevel: Int = 1

    fun isActive(): Boolean = phase != FishingPhase.IDLE

    fun startCasting(
        targetWorldX: Float,
        targetWorldY: Float,
        habitat: WaterHabitat,
        bestRodId: String,
        fishingLvl: Int,
        nightTime: Boolean
    ) {
        bobberX = targetWorldX
        bobberY = targetWorldY
        activeHabitat = habitat
        rodId = bestRodId
        fishingLevel = fishingLvl
        isNight = nightTime
        phase = FishingPhase.CASTING
        phaseTimer = 0f
        splashTimer = 0f
        lastMessage = ""
        val speedFactor = when (bestRodId) {
            "tool_rod_golden" -> 0.68f
            "tool_rod_bamboo" -> 0.84f
            else -> 1.0f
        }
        waitTargetDuration = (1.5f + Random.nextFloat() * 2.1f) * speedFactor
    }

    fun cancel() {
        phase = FishingPhase.IDLE
        activeFish = null
        splashTimer = 0f
    }

    fun onTimingTap(): FishingEventResult {
        when (phase) {
            FishingPhase.WAITING -> {
                lastMessage = "Terlalu cepat! Tunggu pelampung bergetar (!)."
                phase = FishingPhase.ESCAPED
                phaseTimer = 0f
                return FishingEventResult(escaped = true)
            }
            FishingPhase.BITE -> {
                startMinigame()
                return FishingEventResult(splashTriggered = true)
            }
            FishingPhase.MINIGAME -> {
                val dist = abs(indicatorPos - sweetCenter)
                return if (dist <= sweetHalfW) {
                    catchProgress = (catchProgress + 0.30f).coerceAtMost(1f)
                    splashTimer = 0.35f
                    sweetCenter = (0.20f + Random.nextFloat() * 0.60f)
                    if (catchProgress >= 1f) {
                        val fish = activeFish
                        phase = FishingPhase.CAUGHT
                        phaseTimer = 0f
                        lastMessage = fish?.let { "Berhasil menangkap ${it.name} (${it.rarity.label})!" } ?: "Ikan tertangkap!"
                        FishingEventResult(caughtFish = fish, splashTriggered = true)
                    } else {
                        FishingEventResult(splashTriggered = true)
                    }
                } else {
                    catchProgress = (catchProgress - 0.14f).coerceAtLeast(0f)
                    if (catchProgress <= 0f) {
                        phase = FishingPhase.ESCAPED
                        phaseTimer = 0f
                        lastMessage = "Ikannya lepas! Tekan saat garis masuk zona hijau."
                        FishingEventResult(escaped = true)
                    } else {
                        FishingEventResult()
                    }
                }
            }
            else -> return FishingEventResult()
        }
    }

    private fun startMinigame() {
        val fish = rollFishSpecies()
        activeFish = fish
        phase = FishingPhase.MINIGAME
        phaseTimer = 0f
        indicatorPos = 0.15f
        indicatorVel = 0.66f * fish.difficulty
        val rodBonus = when (rodId) {
            "tool_rod_golden" -> 0.06f
            "tool_rod_bamboo" -> 0.03f
            else -> 0f
        }
        val levelBonus = (fishingLevel - 1).coerceAtMost(10) * 0.005f
        sweetHalfW = (0.145f - (fish.difficulty - 1f) * 0.03f + rodBonus + levelBonus).coerceIn(0.088f, 0.22f)
        sweetCenter = 0.35f + Random.nextFloat() * 0.3f
        catchProgress = 0.26f
        tensionTimeLeft = 11.5f
        splashTimer = 0.45f
    }

    private fun rollFishSpecies(): FishSpecies {
        val candidates = RpgCatalog.fishSpecies.filter { fish ->
            when (activeHabitat) {
                WaterHabitat.LAKE -> fish.habitat == WaterHabitat.LAKE || fish.habitat == WaterHabitat.BOTH
                WaterHabitat.RIVER -> fish.habitat == WaterHabitat.RIVER || fish.habitat == WaterHabitat.BOTH
                WaterHabitat.OCEAN -> fish.habitat == WaterHabitat.OCEAN
                WaterHabitat.CAVE -> fish.habitat == WaterHabitat.CAVE || fish.id == "fish_moon_eel"
                WaterHabitat.BOTH -> fish.habitat == WaterHabitat.LAKE || fish.habitat == WaterHabitat.RIVER || fish.habitat == WaterHabitat.BOTH
            }
        }.ifEmpty { RpgCatalog.fishSpecies }

        val rodRareBoost = when (rodId) {
            "tool_rod_golden" -> 1.65f
            "tool_rod_bamboo" -> 1.25f
            else -> 1.0f
        }
        val weighted = candidates.map { fish ->
            var w = fish.catchProbability * 100f
            if (fish.rarity == FishRarity.RARE || fish.rarity == FishRarity.EPIC || fish.rarity == FishRarity.LEGENDARY) {
                w *= rodRareBoost * (1f + (fishingLevel - 1) * 0.05f)
            }
            if (fish.nightBonus && isNight) {
                w *= 2.1f
            }
            fish to w
        }
        val total = weighted.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.01f)
        var roll = Random.nextFloat() * total
        for ((fish, weight) in weighted) {
            roll -= weight
            if (roll <= 0f) return fish
        }
        return candidates.first()
    }

    fun update(dt: Float): FishingEventResult {
        if (splashTimer > 0f) {
            splashTimer = (splashTimer - dt).coerceAtLeast(0f)
        }
        if (phase == FishingPhase.IDLE) return FishingEventResult()

        phaseTimer += dt
        when (phase) {
            FishingPhase.CASTING -> {
                if (phaseTimer >= 0.38f) {
                    phase = FishingPhase.WAITING
                    phaseTimer = 0f
                    splashTimer = 0.38f
                    return FishingEventResult(splashTriggered = true)
                }
            }
            FishingPhase.WAITING -> {
                if (phaseTimer >= waitTargetDuration) {
                    phase = FishingPhase.BITE
                    phaseTimer = 0f
                    splashTimer = 0.55f
                    return FishingEventResult(biteTriggered = true, splashTriggered = true)
                }
            }
            FishingPhase.BITE -> {
                if (phaseTimer >= 2.3f) {
                    phase = FishingPhase.ESCAPED
                    phaseTimer = 0f
                    lastMessage = "Ikan melepas umpan! Cepat tekan saat tanda (!) muncul."
                    return FishingEventResult(escaped = true)
                }
            }
            FishingPhase.MINIGAME -> {
                indicatorPos += indicatorVel * dt
                if (indicatorPos >= 1f) {
                    indicatorPos = 1f
                    indicatorVel = -abs(indicatorVel)
                } else if (indicatorPos <= 0f) {
                    indicatorPos = 0f
                    indicatorVel = abs(indicatorVel)
                }
                catchProgress = (catchProgress - dt * 0.030f).coerceAtLeast(0f)
                tensionTimeLeft = (tensionTimeLeft - dt).coerceAtLeast(0f)
                if (tensionTimeLeft <= 0f || catchProgress <= 0f) {
                    phase = FishingPhase.ESCAPED
                    phaseTimer = 0f
                    lastMessage = "Senar mengendur dan ikan kabur!"
                    return FishingEventResult(escaped = true)
                }
            }
            FishingPhase.CAUGHT, FishingPhase.ESCAPED -> {
                if (phaseTimer >= 1.50f) {
                    phase = FishingPhase.IDLE
                    activeFish = null
                }
            }
            else -> Unit
        }
        return FishingEventResult()
    }

    fun toUiState(): FishingUiState = FishingUiState(
        phase = phase,
        targetFish = activeFish,
        indicatorPos = indicatorPos,
        sweetZoneCenter = sweetCenter,
        sweetZoneHalfWidth = sweetHalfW,
        catchProgress = catchProgress,
        tensionTimeLeft = tensionTimeLeft,
        lastResultMessage = lastMessage
    )
}
