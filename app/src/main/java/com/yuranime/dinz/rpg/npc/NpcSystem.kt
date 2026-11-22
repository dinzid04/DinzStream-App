package com.yuranime.dinz.rpg.npc

import com.yuranime.dinz.rpg.controls.AStarPathfinder
import com.yuranime.dinz.rpg.model.Direction
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.NpcDef
import com.yuranime.dinz.rpg.model.NpcDialogueUiState
import com.yuranime.dinz.rpg.model.NpcScheduleSlot
import com.yuranime.dinz.rpg.model.NpcSummaryUi
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.persistence.NpcFriendshipEntity
import com.yuranime.dinz.rpg.world.HomesteadWorld
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

data class NpcRuntime(
    val def: NpcDef,
    var mapId: MapId,
    var x: Float,
    var y: Float,
    var direction: Direction = Direction.DOWN,
    var isWalking: Boolean = false,
    var stateTime: Float = 0f,
    val path: MutableList<Pair<Int, Int>> = mutableListOf(),
    var friendshipPoints: Int = 0,
    var talkedToday: Boolean = false,
    var giftedToday: Boolean = false,
    var emoteKey: String? = null,
    var emoteTimer: Float = 0f,
    var activityLabel: String = "",
    var wanderTimer: Float = 3f,
    var lineIndex: Int = 0
) {
    val tileX: Int get() = (x / HomesteadWorld.TILE_SIZE).toInt()
    val tileY: Int get() = (y / HomesteadWorld.TILE_SIZE).toInt()
    val hearts: Int get() = (friendshipPoints / 25).coerceIn(0, 10)
}

data class VillageAnimal(
    val id: String,
    val species: String,
    val mapId: MapId,
    val homeX: Float,
    val homeY: Float,
    var x: Float,
    var y: Float,
    var targetX: Float,
    var targetY: Float,
    var facingLeft: Boolean = false,
    var pauseTimer: Float = 2f,
    var animTime: Float = 0f
)

data class GiftReactionResult(
    val accepted: Boolean,
    val pointsAdded: Int,
    val dialogue: NpcDialogueUiState,
    val toast: String
)

class NpcSystem {
    val npcs: MutableList<NpcRuntime> = mutableListOf()
    val animals: MutableList<VillageAnimal> = mutableListOf()
    private var scheduleCheckTimer: Float = 0f

    init {
        initNpcs(8.0f)
        initAnimals()
    }

    private fun initNpcs(startHour: Float) {
        npcs.clear()
        for (def in RpgCatalog.npcs) {
            val slot = findSlotForHour(def, startHour)
            npcs.add(
                NpcRuntime(
                    def = def,
                    mapId = slot.mapId,
                    x = slot.tileX * HomesteadWorld.TILE_SIZE + 8f,
                    y = slot.tileY * HomesteadWorld.TILE_SIZE + 8f,
                    activityLabel = slot.activityLabel,
                    wanderTimer = 2f + Random.nextFloat() * 4f
                )
            )
        }
    }

    private fun initAnimals() {
        animals.clear()
        val spawns = listOf(
            Triple("animal_cat_1", "cat", 15 to 41),
            Triple("animal_dog_1", "dog", 38 to 35),
            Triple("animal_chicken_1", "chicken", 16 to 35),
            Triple("animal_cow_1", "cow", 24 to 33),
            Triple("animal_frog_1", "frog", 36 to 23)
        )
        spawns.forEach { (id, sp, pos) ->
            val wx = pos.first * HomesteadWorld.TILE_SIZE + 8f
            val wy = pos.second * HomesteadWorld.TILE_SIZE + 8f
            animals.add(VillageAnimal(id, sp, MapId.OVERWORLD, wx, wy, wx, wy, wx, wy, pauseTimer = 1f + Random.nextFloat() * 3f))
        }
    }

    fun loadFriendships(entities: List<NpcFriendshipEntity>) {
        val byId = entities.associateBy { it.npcId }
        for (npc in npcs) {
            val saved = byId[npc.def.id]
            if (saved != null) {
                npc.friendshipPoints = saved.points.coerceIn(0, 250)
                npc.talkedToday = saved.talkedToday
                npc.giftedToday = saved.giftedToday
            }
        }
    }

    fun toEntities(userUid: String): List<NpcFriendshipEntity> = npcs.map {
        NpcFriendshipEntity(userUid, it.def.id, it.friendshipPoints, it.talkedToday, it.giftedToday)
    }

    fun onNewDay() {
        for (npc in npcs) {
            npc.talkedToday = false
            npc.giftedToday = false
        }
    }

    fun findSlotForHour(def: NpcDef, hour: Float): NpcScheduleSlot {
        val h = hour.coerceIn(0f, 23.99f)
        return def.schedule.firstOrNull { h >= it.startHour && h < it.endHour } ?: def.schedule.first()
    }

    fun syncSchedulesImmediately(hour: Float) {
        for (npc in npcs) {
            val slot = findSlotForHour(npc.def, hour)
            npc.mapId = slot.mapId
            npc.x = slot.tileX * HomesteadWorld.TILE_SIZE + 8f
            npc.y = slot.tileY * HomesteadWorld.TILE_SIZE + 8f
            npc.path.clear()
            npc.isWalking = false
            npc.activityLabel = slot.activityLabel
        }
    }

    fun update(dt: Float, world: HomesteadWorld, pausedNpcId: String? = null) {
        scheduleCheckTimer += dt
        val shouldCheckSchedule = scheduleCheckTimer >= 2.2f
        if (shouldCheckSchedule) scheduleCheckTimer = 0f

        for (npc in npcs) {
            npc.stateTime += dt
            if (npc.emoteTimer > 0f) {
                npc.emoteTimer = (npc.emoteTimer - dt).coerceAtLeast(0f)
                if (npc.emoteTimer <= 0f) npc.emoteKey = null
            }
            if (npc.def.id == pausedNpcId) {
                npc.isWalking = false
                continue
            }

            if (shouldCheckSchedule) {
                val slot = findSlotForHour(npc.def, world.timeHours)
                npc.activityLabel = slot.activityLabel
                if (npc.mapId != slot.mapId) {
                    // Transition NPC seamlessly to their scheduled building/region!
                    npc.mapId = slot.mapId
                    npc.x = slot.tileX * HomesteadWorld.TILE_SIZE + 8f
                    npc.y = slot.tileY * HomesteadWorld.TILE_SIZE + 8f
                    npc.path.clear()
                    npc.isWalking = false
                } else if (npc.mapId == world.currentMapId && npc.path.isEmpty()) {
                    val dist = abs(npc.tileX - slot.tileX) + abs(npc.tileY - slot.tileY)
                    if (dist > 3) {
                        val p = AStarPathfinder.findPath(
                            npc.tileX, npc.tileY, slot.tileX, slot.tileY,
                            world.mapWidthTiles, world.mapHeightTiles,
                            maxVisited = 400
                        ) { tx, ty -> !world.isBlockedForWalking(tx, ty) }
                        if (p.isNotEmpty()) npc.path.addAll(p)
                    }
                }
            }

            // Gentle cozy wander near scheduled station when on current map
            if (npc.mapId == world.currentMapId && npc.path.isEmpty()) {
                npc.wanderTimer -= dt
                if (npc.wanderTimer <= 0f) {
                    npc.wanderTimer = 3.5f + Random.nextFloat() * 4.5f
                    val slot = findSlotForHour(npc.def, world.timeHours)
                    val wx = (slot.tileX + Random.nextInt(-2, 3)).coerceIn(2, world.mapWidthTiles - 3)
                    val wy = (slot.tileY + Random.nextInt(-2, 3)).coerceIn(2, world.mapHeightTiles - 3)
                    if (!world.isBlockedForWalking(wx, wy)) {
                        val p = AStarPathfinder.findPath(
                            npc.tileX, npc.tileY, wx, wy,
                            world.mapWidthTiles, world.mapHeightTiles,
                            maxVisited = 120
                        ) { tx, ty -> !world.isBlockedForWalking(tx, ty) }
                        if (p.isNotEmpty()) npc.path.addAll(p)
                    }
                }
            }

            // Follow A* waypoints smoothly
            if (npc.mapId == world.currentMapId && npc.path.isNotEmpty()) {
                val (nxTile, nyTile) = npc.path.first()
                val tx = nxTile * HomesteadWorld.TILE_SIZE + 8f
                val ty = nyTile * HomesteadWorld.TILE_SIZE + 8f
                val dx = tx - npc.x
                val dy = ty - npc.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < 1.5f) {
                    npc.x = tx
                    npc.y = ty
                    npc.path.removeAt(0)
                    npc.isWalking = npc.path.isNotEmpty()
                } else {
                    val speed = 28f
                    val step = (speed * dt).coerceAtMost(dist)
                    npc.x += (dx / dist) * step
                    npc.y += (dy / dist) * step
                    npc.isWalking = true
                    npc.direction = if (abs(dx) > abs(dy)) {
                        if (dx > 0f) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0f) Direction.UP else Direction.DOWN
                    }
                }
            } else {
                npc.isWalking = false
            }
        }

        // Update 5 Living Village Animals on Overworld!
        if (world.currentMapId == MapId.OVERWORLD) {
            for (animal in animals) {
                animal.animTime += dt
                val dx = animal.targetX - animal.x
                val dy = animal.targetY - animal.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist > 1.5f) {
                    val speed = 18f
                    val step = (speed * dt).coerceAtMost(dist)
                    val nx = animal.x + (dx / dist) * step
                    val ny = animal.y + (dy / dist) * step
                    if (!world.isBlockedAtWorldPos(nx, ny)) {
                        animal.x = nx
                        animal.y = ny
                        animal.facingLeft = dx < 0f
                    } else {
                        animal.targetX = animal.x
                        animal.targetY = animal.y
                    }
                } else {
                    animal.pauseTimer -= dt
                    if (animal.pauseTimer <= 0f) {
                        animal.pauseTimer = 2.0f + Random.nextFloat() * 3.5f
                        val ox = (Random.nextFloat() * 56f) - 28f
                        val oy = (Random.nextFloat() * 56f) - 28f
                        val cx = (animal.homeX + ox).coerceIn(32f, world.worldW - 32f)
                        val cy = (animal.homeY + oy).coerceIn(32f, world.worldH - 32f)
                        if (!world.isBlockedAtWorldPos(cx, cy)) {
                            animal.targetX = cx
                            animal.targetY = cy
                        }
                    }
                }
            }
        }
    }

    fun getNpcsInMap(mapId: MapId): List<NpcRuntime> = npcs.filter { it.mapId == mapId }

    fun findNpcAtOrNearTile(mapId: MapId, tileX: Int, tileY: Int, radiusTiles: Int = 1): NpcRuntime? {
        return npcs.firstOrNull { npc ->
            npc.mapId == mapId && abs(npc.tileX - tileX) <= radiusTiles && abs(npc.tileY - tileY) <= radiusTiles
        }
    }

    fun talkToNpc(npcId: String, playerX: Float, playerY: Float, timeHours: Float): Pair<NpcDialogueUiState, Boolean>? {
        val npc = npcs.find { it.def.id == npcId } ?: return null
        val dx = playerX - npc.x
        val dy = playerY - npc.y
        npc.direction = if (abs(dx) > abs(dy)) {
            if (dx > 0f) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0f) Direction.UP else Direction.DOWN
        }
        npc.path.clear()
        npc.isWalking = false

        var firstTalkToday = false
        if (!npc.talkedToday) {
            npc.talkedToday = true
            npc.friendshipPoints = (npc.friendshipPoints + 8).coerceAtMost(250)
            firstTalkToday = true
        }
        npc.emoteKey = if (npc.hearts >= 3) "heart" else "happy"
        npc.emoteTimer = 2.2f

        val lines = when {
            timeHours < 12f -> npc.def.morningLines
            timeHours < 17.5f -> npc.def.afternoonLines
            else -> npc.def.eveningLines
        }
        val line = lines[npc.lineIndex % lines.size]
        npc.lineIndex++

        return buildDialogue(npc, line) to firstTalkToday
    }

    fun giveGift(npcId: String, itemId: String): GiftReactionResult? {
        val npc = npcs.find { it.def.id == npcId } ?: return null
        if (npc.giftedToday) {
            return GiftReactionResult(
                accepted = false,
                pointsAdded = 0,
                dialogue = buildDialogue(npc, "Terima kasih, tapi aku sudah menerima hadiahmu hari ini! Besok lagi ya."),
                toast = "${npc.def.name} sudah diberi hadiah hari ini."
            )
        }
        npc.giftedToday = true
        val (pts, line, emote) = when {
            itemId in npc.def.lovedItems -> Triple(35, npc.def.giftLoveLine, "heart")
            itemId in npc.def.favoriteItems -> Triple(20, npc.def.giftThankLine, "music")
            else -> Triple(10, npc.def.giftThankLine, "happy")
        }
        npc.friendshipPoints = (npc.friendshipPoints + pts).coerceAtMost(250)
        npc.emoteKey = emote
        npc.emoteTimer = 2.8f
        return GiftReactionResult(
            accepted = true,
            pointsAdded = pts,
            dialogue = buildDialogue(npc, line),
            toast = "Persahabatan ${npc.def.name} +$pts (${npc.hearts}/10 Hati)!"
        )
    }

    private fun buildDialogue(npc: NpcRuntime, text: String): NpcDialogueUiState = NpcDialogueUiState(
        active = true,
        npcId = npc.def.id,
        npcName = npc.def.name,
        npcRole = npc.def.roleTitle,
        facePath = npc.def.facePath,
        text = text,
        friendshipHearts = npc.hearts,
        friendshipPoints = npc.friendshipPoints,
        giftedToday = npc.giftedToday,
        opensShop = npc.def.opensShop,
        opensCooking = npc.def.opensCooking,
        opensBlacksmith = npc.def.opensBlacksmith
    )

    fun getSummaries(): List<NpcSummaryUi> = npcs.map {
        NpcSummaryUi(
            id = it.def.id,
            name = it.def.name,
            roleTitle = it.def.roleTitle,
            facePath = it.def.facePath,
            currentMapId = it.mapId,
            tileX = it.tileX,
            tileY = it.tileY,
            activityLabel = it.activityLabel,
            hearts = it.hearts,
            points = it.friendshipPoints,
            talkedToday = it.talkedToday,
            giftedToday = it.giftedToday
        )
    }
}
