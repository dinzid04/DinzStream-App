package com.yuranime.dinz.rpg.model

enum class Direction(val key: String, val sheetCol: Int) {
    DOWN("down", 0),
    UP("up", 1),
    LEFT("left", 2),
    RIGHT("right", 3)
}

enum class PlayerAction {
    IDLE, WALK, RUN,
    FISH_CAST, FISH_WAIT, FISH_REEL,
    AXE, PICKAXE, HOE, WATER, HARVEST, INTERACT, CELEBRATE, CRAFT
}

enum class MapId(
    val id: String,
    val displayName: String,
    val tmxPath: String,
    val bgmTrack: String,
    val isIndoor: Boolean
) {
    OVERWORLD("overworld", "Sakura Vale", "rpg/maps/sakura_vale_overworld.tmx", "village", false),
    PLAYER_HOUSE("interior_player_house", "Rumah Petualang", "rpg/maps/interior_player_house.tmx", "homestead", true),
    GENERAL_STORE("interior_general_store", "Toko Serba Ada Hana", "rpg/maps/interior_general_store.tmx", "village", true),
    TEAHOUSE("interior_teahouse", "Kedai Teh Sakura", "rpg/maps/interior_teahouse.tmx", "village", true),
    BLACKSMITH("interior_blacksmith", "Bengkel Pandai Besi Kenji", "rpg/maps/interior_blacksmith.tmx", "homestead", true),
    ELDER_HOUSE("interior_elder_house", "Pendopo Tetua Haru", "rpg/maps/interior_elder_house.tmx", "homestead", true),
    MINING_CAVE("mining_cave", "Gua Kristal Utara", "rpg/maps/mining_cave.tmx", "cave", true);

    companion object {
        fun fromId(id: String?): MapId = entries.find { it.id == id } ?: OVERWORLD
    }
}

enum class WorldRegion(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val requiredLevel: Int,
    val mapNormX: Float,
    val mapNormY: Float,
    val bgmTrack: String
) {
    HOMESTEAD("homestead", "Rumah & Pekarangan", "Tempat tinggal, peti simpan & lahan dekorasi", 1, 0.17f, 0.28f, "homestead"),
    FARM_FIELDS("farm_fields", "Lahan Pertanian", "Ladang subur berpagar untuk 6 jenis tanaman", 1, 0.17f, 0.54f, "homestead"),
    VILLAGE_PLAZA("village_plaza", "Alun-Alun Sakura", "Pusat desa, sumur warga & papan misi", 1, 0.49f, 0.39f, "village"),
    MARKETPLACE("marketplace", "Pasar & Pertokoan", "Toko Hana, Kedai Teh Daiki & Bengkel Kenji", 1, 0.50f, 0.53f, "village"),
    RESIDENTIAL("residential", "Permukiman Warga", "Pondok Tetua Haru dan rumah penduduk timur", 1, 0.82f, 0.37f, "village"),
    WHISPERING_LAKE("whispering_lake", "Danau Bisikan", "Dermaga kayu & spot pancing ikan air tawar", 1, 0.47f, 0.72f, "water"),
    RIVER_VALLEY("river_valley", "Lembah Sungai", "Sungai mengalir dari air terjun hingga samudra", 1, 0.68f, 0.45f, "water"),
    DENSE_FOREST("dense_forest", "Hutan Rimba Barat", "Kayu Ek, Pinus, beri liar & kacang hutan", 1, 0.16f, 0.74f, "homestead"),
    OCEAN_BEACH("ocean_beach", "Pantai Pasir Emas", "Dermaga laut & spesies ikan samudra langka", 1, 0.42f, 0.92f, "water"),
    MOUNTAIN_HIGHLAND("mountain_highland", "Pegunungan Utara", "Tebing batu, air terjun & pintu Gua Kristal", 1, 0.84f, 0.14f, "ruins"),
    CRYSTAL_CAVE("crystal_cave", "Gua Kristal", "Tambang bijih emas, permata & kolam gua", 1, 0.85f, 0.08f, "cave"),
    HIDDEN_SAKURA("hidden_sakura", "Hutan Sakura Rahasia", "Kuil sakura kuno (Terbuka di Lv. 2)", 2, 0.14f, 0.09f, "ruins"),
    ANCIENT_RUINS("ancient_ruins", "Reruntuhan Kuno", "Patung roh penjaga & peti harta (Terbuka di Lv. 3)", 3, 0.85f, 0.69f, "ruins")
}

enum class ControlMode(val id: String, val label: String, val description: String) {
    HYBRID("hybrid", "Hybrid (Joystick + Ketuk)", "Gunakan joystick analog atau ketuk langsung di dunia"),
    JOYSTICK_ONLY("joystick", "Joystick Saja", "Kendali analog penuh tanpa ketuk jalan"),
    TAP_ONLY("tap", "Ketuk-Jalan Saja", "Ketuk tanah, pohon, batu, air, NPC atau pintu untuk bergerak otomatis")
}

data class ControlSettings(
    val controlMode: ControlMode = ControlMode.HYBRID,
    val floatingJoystick: Boolean = false,
    val joystickOnRight: Boolean = false,
    val joystickScale: Float = 1.0f,     // 0.75f .. 1.35f
    val buttonScale: Float = 1.0f,       // 0.80f .. 1.30f
    val controlOpacity: Float = 0.88f,   // 0.45f .. 1.0f
    val controlSensitivity: Float = 1.0f,// 0.70f .. 1.40f
    val uiScale: Float = 1.0f            // 0.85f .. 1.15f
)

data class ProtagonistPreset(
    val id: String,
    val name: String,
    val genderTag: String,
    val sheetPath: String,
    val facePath: String,
    val tagline: String
)

data class PlayerCustomization(
    val presetId: String = "princess",
    val outfitTintHex: Long = 0xFFFFFFFF,
    val accentHex: Long = 0xFFC84B31
)

enum class ItemCategory(val label: String) {
    TOOLS("Alat"),
    FISH("Ikan"),
    SEEDS("Bibit"),
    CROPS("Panen"),
    MINERALS("Bahan"),
    FOOD("Masakan"),
    FORAGE("Alam"),
    FURNITURE("Furnitur"),
    DECORATIONS("Dekorasi")
}

enum class FishRarity(val label: String, val colorHex: Long, val weight: Float, val expReward: Int) {
    COMMON("Biasa", 0xFF78716C, 46f, 12),
    UNCOMMON("Tidak Umum", 0xFF2F855A, 28f, 22),
    RARE("Langka", 0xFF2563EB, 16f, 40),
    EPIC("Epik", 0xFF9333EA, 7.5f, 75),
    LEGENDARY("Legendaris", 0xFFD97706, 2.5f, 150)
}

enum class WaterHabitat(val label: String) {
    LAKE("Danau Bisikan"),
    RIVER("Lembah Sungai"),
    OCEAN("Pantai Samudra"),
    CAVE("Kolam Gua Kristal"),
    BOTH("Danau & Sungai")
}

data class FishSpecies(
    val id: String,
    val name: String,
    val rarity: FishRarity,
    val sellPrice: Int,
    val catchProbability: Float,
    val habitat: WaterHabitat,
    val spritePath: String,
    val description: String,
    val difficulty: Float = 1.0f,
    val nightBonus: Boolean = false
)

enum class TreeType(
    val id: String,
    val displayName: String,
    val maxHealth: Int,
    val woodYield: Int,
    val hardwoodChance: Float,
    val regrowSeconds: Float,
    val expReward: Int,
    val spritePath: String,
    val drawSizePx: Float
) {
    OAK("oak", "Pohon Ek", 4, 4, 0.22f, 95f, 16, "rpg/objects/trees/tree_oak.png", 44f),
    PINE("pine", "Pohon Pinus", 5, 5, 0.35f, 110f, 20, "rpg/objects/trees/tree_pine.png", 34f),
    SAKURA("sakura", "Pohon Sakura", 3, 4, 0.55f, 100f, 26, "rpg/objects/trees/tree_sakura.png", 44f),
    AUTUMN("autumn", "Pohon Emas", 4, 5, 0.45f, 105f, 24, "rpg/objects/trees/tree_autumn.png", 44f)
}

enum class TreeHealthState {
    HEALTHY, DAMAGED, HEAVILY_DAMAGED, FALLING, STUMP
}

data class WorldTree(
    val id: String,
    val tileX: Int,
    val tileY: Int,
    val type: TreeType,
    var health: Int = type.maxHealth,
    var state: TreeHealthState = TreeHealthState.HEALTHY,
    var shakeTimer: Float = 0f,
    var fallTimer: Float = 0f,
    var regrowTimer: Float = 0f
)

enum class OreType(
    val id: String,
    val displayName: String,
    val maxHealth: Int,
    val dropItemId: String,
    val dropAmount: Int,
    val expReward: Int,
    val rockSpritePath: String,
    val respawnSeconds: Float
) {
    STONE("stone", "Batu Gunung", 3, "res_stone", 3, 12, "rpg/objects/rocks/rock_stone.png", 75f),
    COPPER("copper", "Bongkahan Tembaga", 4, "ore_copper", 2, 20, "rpg/objects/rocks/rock_copper.png", 95f),
    IRON("iron", "Bongkahan Besi", 5, "ore_iron", 2, 28, "rpg/objects/rocks/rock_iron.png", 110f),
    GOLD("gold", "Urat Emas", 6, "ore_gold", 2, 42, "rpg/objects/rocks/rock_gold.png", 140f),
    AMETHYST("amethyst", "Kristal Kecubung", 5, "gem_amethyst", 1, 55, "rpg/objects/rocks/rock_amethyst.png", 160f),
    EMERALD("emerald", "Kristal Zamrud", 6, "gem_emerald", 1, 68, "rpg/objects/rocks/rock_emerald.png", 180f)
}

data class OreNode(
    val id: String,
    val mapId: MapId,
    val tileX: Int,
    val tileY: Int,
    val type: OreType,
    var health: Int = type.maxHealth,
    var depleted: Boolean = false,
    var shakeTimer: Float = 0f,
    var respawnTimer: Float = 0f
)

data class ForageNode(
    val id: String,
    val mapId: MapId,
    val tileX: Int,
    val tileY: Int,
    val itemId: String,
    var collected: Boolean = false,
    var respawnTimer: Float = 0f
)

data class TreasureChestNode(
    val id: String,
    val mapId: MapId,
    val tileX: Int,
    val tileY: Int,
    val coinReward: Int,
    val itemRewards: List<Pair<String, Int>>,
    val label: String,
    var opened: Boolean = false
)

enum class CropType(
    val id: String,
    val displayName: String,
    val seedItemId: String,
    val harvestItemId: String,
    val stageDurationSeconds: Float,
    val sellPrice: Int,
    val expReward: Int
) {
    CARROT("carrot", "Wortel", "seed_carrot", "crop_carrot", 14f, 28, 14),
    POTATO("potato", "Kentang", "seed_potato", "crop_potato", 18f, 38, 18),
    TOMATO("tomato", "Tomat", "seed_tomato", "crop_tomato", 22f, 52, 24),
    STRAWBERRY("strawberry", "Stroberi", "seed_strawberry", "crop_strawberry", 24f, 70, 30),
    WHEAT("wheat", "Gandum Emas", "seed_wheat", "crop_wheat", 20f, 46, 22),
    PUMPKIN("pumpkin", "Labu", "seed_pumpkin", "crop_pumpkin", 28f, 92, 38)
}

data class CropPlot(
    val tileX: Int,
    val tileY: Int,
    var isTilled: Boolean = false,
    var isWatered: Boolean = false,
    var cropType: CropType? = null,
    var stage: Int = 0,
    var growthProgress: Float = 0f
) {
    val key: String get() = "${tileX}_${tileY}"
    val isHarvestable: Boolean get() = cropType != null && stage >= 4
}

data class ItemDef(
    val id: String,
    val name: String,
    val category: ItemCategory,
    val maxStack: Int,
    val sellPrice: Int,
    val buyPrice: Int,
    val spritePath: String,
    val description: String,
    val placeable: Boolean = false,
    val rotatable: Boolean = false,
    val woodCost: Int = 0,
    val stoneCost: Int = 0,
    val energyRestore: Int = 0
)

data class InventoryStack(
    val itemId: String,
    val quantity: Int
)

data class PlacedObject(
    val id: String,
    val mapId: MapId = MapId.OVERWORLD,
    val itemId: String,
    val tileX: Int,
    val tileY: Int,
    val rotation: Int = 0
)

data class HouseUpgradeSpec(
    val tier: Int,
    val name: String,
    val coinCost: Int,
    val woodCost: Int,
    val stoneCost: Int,
    val ironCost: Int = 0,
    val description: String
)

enum class RecipeType(val label: String) {
    CRAFTING("Kerajinan"),
    COOKING("Memasak")
}

data class CraftingRecipe(
    val id: String,
    val type: RecipeType,
    val outputItemId: String,
    val outputQty: Int = 1,
    val ingredients: List<Pair<String, Int>>,
    val expReward: Int,
    val description: String
)

data class NpcScheduleSlot(
    val startHour: Float,
    val endHour: Float,
    val mapId: MapId,
    val tileX: Int,
    val tileY: Int,
    val activityLabel: String
)

data class NpcDef(
    val id: String,
    val name: String,
    val roleTitle: String,
    val sheetPath: String,
    val facePath: String,
    val favoriteItems: List<String>,
    val lovedItems: List<String>,
    val morningLines: List<String>,
    val afternoonLines: List<String>,
    val eveningLines: List<String>,
    val giftThankLine: String,
    val giftLoveLine: String,
    val opensShop: Boolean = false,
    val opensCooking: Boolean = false,
    val opensBlacksmith: Boolean = false,
    val schedule: List<NpcScheduleSlot>
)

data class NpcDialogueUiState(
    val active: Boolean = false,
    val npcId: String = "",
    val npcName: String = "",
    val npcRole: String = "",
    val facePath: String = "",
    val text: String = "",
    val friendshipHearts: Int = 0,
    val friendshipPoints: Int = 0,
    val giftedToday: Boolean = false,
    val opensShop: Boolean = false,
    val opensCooking: Boolean = false,
    val opensBlacksmith: Boolean = false
)

data class NpcSummaryUi(
    val id: String,
    val name: String,
    val roleTitle: String,
    val facePath: String,
    val currentMapId: MapId,
    val tileX: Int,
    val tileY: Int,
    val activityLabel: String,
    val hearts: Int,
    val points: Int,
    val talkedToday: Boolean,
    val giftedToday: Boolean
)

enum class ContextActionType {
    NONE,
    FISH,
    CHOP_TREE,
    MINE_ORE,
    GATHER_FORAGE,
    OPEN_CHEST,
    TILL_SOIL,
    PLANT_SEED,
    WATER_CROP,
    HARVEST_CROP,
    TALK_NPC,
    ENTER_PORTAL,
    SLEEP_BED,
    OPEN_STOVE,
    OPEN_WORKBENCH,
    OPEN_MERCHANT,
    OPEN_SELL_BIN,
    OPEN_STORAGE,
    OPEN_HOUSE_UPGRADE,
    PLANT_SAPLING,
    INTERACT_DECOR
}

data class ContextualAction(
    val type: ContextActionType = ContextActionType.NONE,
    val label: String = "",
    val targetTileX: Int = 0,
    val targetTileY: Int = 0,
    val targetId: String = "",
    val habitat: WaterHabitat = WaterHabitat.LAKE
)

enum class FishingPhase {
    IDLE, CASTING, WAITING, BITE, MINIGAME, CAUGHT, ESCAPED
}

data class FishingUiState(
    val phase: FishingPhase = FishingPhase.IDLE,
    val targetFish: FishSpecies? = null,
    val indicatorPos: Float = 0.5f,
    val sweetZoneCenter: Float = 0.5f,
    val sweetZoneHalfWidth: Float = 0.14f,
    val catchProgress: Float = 0.25f,
    val tensionTimeLeft: Float = 10f,
    val lastResultMessage: String = ""
)

enum class QuestType(val label: String) {
    MAIN("Utama"),
    SIDE("Warga"),
    DAILY("Harian")
}

data class QuestDef(
    val id: String,
    val type: QuestType,
    val giverNpcId: String?,
    val title: String,
    val description: String,
    val targetCount: Int,
    val coinReward: Int,
    val expReward: Int
)

data class QuestState(
    val def: QuestDef,
    val progress: Int = 0,
    val claimed: Boolean = false
) {
    val isCompleted: Boolean get() = progress >= def.targetCount
}

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val coinReward: Int,
    val expReward: Int
)

data class AchievementState(
    val def: AchievementDef,
    val progress: Int = 0,
    val unlocked: Boolean = false
)

data class BuildPreviewState(
    val active: Boolean = false,
    val selectedItemId: String? = null,
    val tileX: Int = 0,
    val tileY: Int = 0,
    val rotation: Int = 0,
    val isValid: Boolean = false
)

data class RpgHudSnapshot(
    val coins: Int = 150,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val playerLevel: Int = 1,
    val exp: Int = 0,
    val nextLevelExp: Int = 100,
    val fishingLevel: Int = 1,
    val farmingLevel: Int = 1,
    val gatheringLevel: Int = 1,
    val miningLevel: Int = 1,
    val cookingLevel: Int = 1,
    val craftingLevel: Int = 1,
    val houseTier: Int = 1,
    val currentMapId: MapId = MapId.OVERWORLD,
    val currentRegion: WorldRegion = WorldRegion.HOMESTEAD,
    val discoveredRegions: Set<String> = setOf(WorldRegion.HOMESTEAD.id, WorldRegion.FARM_FIELDS.id, WorldRegion.VILLAGE_PLAZA.id),
    val playerTileX: Int = 14,
    val playerTileY: Int = 43,
    val timeHours: Float = 8.0f,
    val dayCount: Int = 1,
    val isRaining: Boolean = false,
    val isRunning: Boolean = false,
    val isAutoWalking: Boolean = false,
    val selectedToolId: String = "tool_rod_basic",
    val selectedSeedId: String = "seed_carrot",
    val contextAction: ContextualAction = ContextualAction(),
    val fishingState: FishingUiState = FishingUiState(),
    val buildPreview: BuildPreviewState = BuildPreviewState(),
    val dialogueState: NpcDialogueUiState = NpcDialogueUiState(),
    val sleepPromptOpen: Boolean = false,
    val inventory: List<InventoryStack> = emptyList(),
    val caughtFishCounts: Map<String, Int> = emptyMap(),
    val npcSummaries: List<NpcSummaryUi> = emptyList(),
    val quests: List<QuestState> = emptyList(),
    val achievements: List<AchievementState> = emptyList(),
    val customization: PlayerCustomization = PlayerCustomization(),
    val toastMessage: String = "",
    val toastId: Long = 0L,
    val accountName: String = "",
    val cloudSyncStatus: String = "Menyinkronkan...",
    val fatalErrorMessage: String? = null
)

object RpgCatalog {
    val protagonistPresets: List<ProtagonistPreset> = listOf(
        ProtagonistPreset("princess", "Hanae", "Perempuan", "rpg/characters/player/princess_sheet.png", "rpg/characters/player/princess_face.png", "Ceria dengan pita sakura klasik"),
        ProtagonistPreset("boy", "Haruto", "Laki-laki", "rpg/characters/player/boy_sheet.png", "rpg/characters/player/boy_face.png", "Petualang desa bersemangat tinggi"),
        ProtagonistPreset("samurai", "Renji", "Laki-laki", "rpg/characters/player/samurai_sheet.png", "rpg/characters/player/samurai_face.png", "Pengelana tenang berjubah tradisional"),
        ProtagonistPreset("woman", "Mei", "Perempuan", "rpg/characters/player/woman_sheet.png", "rpg/characters/player/woman_face.png", "Ramah dan gemar merawat kebun"),
        ProtagonistPreset("hunter", "Kaito", "Laki-laki", "rpg/characters/player/hunter_sheet.png", "rpg/characters/player/hunter_face.png", "Penjelajah hutan & pemancing ulung"),
        ProtagonistPreset("villager3", "Yuki", "Perempuan", "rpg/characters/player/villager3_sheet.png", "rpg/characters/player/villager3_face.png", "Suka memasak dan menjelajah pasar"),
        ProtagonistPreset("egg_girl", "Momo", "Perempuan", "rpg/characters/player/egg_girl_sheet.png", "rpg/characters/player/egg_girl_face.png", "Penuh rasa ingin tahu di gua kristal"),
        ProtagonistPreset("ninja_green", "Sora", "Laki-laki", "rpg/characters/player/ninja_green_sheet.png", "rpg/characters/player/ninja_green_face.png", "Gesit menelusuri rimba sakura")
    )

    val presetById: Map<String, ProtagonistPreset> = protagonistPresets.associateBy { it.id }

    val fishSpecies: List<FishSpecies> = listOf(
        FishSpecies("fish_river_minnow", "Silver Minnow", FishRarity.COMMON, 15, 0.24f, WaterHabitat.BOTH, "rpg/items/fish/fish_river_minnow.png", "Ikan perak lincah penghuni arus dangkal.", 0.84f),
        FishSpecies("fish_pond_carp", "Sunlit Carp", FishRarity.COMMON, 22, 0.22f, WaterHabitat.LAKE, "rpg/items/fish/fish_pond_carp.png", "Ikan mas hangat yang gemar berjemur di Danau Bisikan.", 0.90f),
        FishSpecies("fish_moss_perch", "Moss Perch", FishRarity.COMMON, 26, 0.20f, WaterHabitat.BOTH, "rpg/items/fish/fish_moss_perch.png", "Suka bersembunyi di bawah daun teratai hijau.", 0.94f),
        FishSpecies("fish_ocean_shrimp", "Udang Karang", FishRarity.COMMON, 30, 0.24f, WaterHabitat.OCEAN, "rpg/items/fish/fish_ocean_shrimp.png", "Udang manis segar dari Pantai Pasir Emas.", 0.92f),
        FishSpecies("fish_brook_trout", "Speckled Trout", FishRarity.UNCOMMON, 45, 0.14f, WaterHabitat.RIVER, "rpg/items/fish/fish_brook_trout.png", "Trout berbintik dari arus Lembah Sungai.", 1.12f),
        FishSpecies("fish_amber_catfish", "Whisker Catfish", FishRarity.UNCOMMON, 55, 0.13f, WaterHabitat.LAKE, "rpg/items/fish/fish_amber_catfish.png", "Lele berkumis panjang penjaga dasar danau.", 1.15f),
        FishSpecies("fish_jade_bream", "Jade Bream", FishRarity.UNCOMMON, 62, 0.12f, WaterHabitat.RIVER, "rpg/items/fish/fish_jade_bream.png", "Sisik hijau giok yang memantulkan sinar matahari.", 1.20f),
        FishSpecies("fish_reef_squid", "Cumi Karang", FishRarity.UNCOMMON, 70, 0.18f, WaterHabitat.OCEAN, "rpg/items/fish/fish_reef_squid.png", "Cumi lembut bahan utama masakan laut Daiki.", 1.22f),
        FishSpecies("fish_crimson_koi", "Crimson Koi", FishRarity.RARE, 115, 0.08f, WaterHabitat.LAKE, "rpg/items/fish/fish_crimson_koi.png", "Koi merah-putih simbol keberuntungan Sakura Vale.", 1.36f),
        FishSpecies("fish_azure_pike", "Azure Pike", FishRarity.RARE, 130, 0.075f, WaterHabitat.RIVER, "rpg/items/fish/fish_azure_pike.png", "Predator sungai biru yang cepat saat menarik kail.", 1.44f),
        FishSpecies("fish_coral_octopus", "Gurita Koral", FishRarity.RARE, 145, 0.13f, WaterHabitat.OCEAN, "rpg/items/fish/fish_coral_octopus.png", "Gurita cerdik favorit untuk membuat Takoyaki.", 1.46f),
        FishSpecies("fish_cave_glowfish", "Ikan Kristal Gua", FishRarity.RARE, 175, 0.25f, WaterHabitat.CAVE, "rpg/items/fish/fish_cave_glowfish.png", "Ikan bercahaya pirus dari kolam Gua Kristal.", 1.50f),
        FishSpecies("fish_moon_eel", "Moonlight Eel", FishRarity.EPIC, 225, 0.045f, WaterHabitat.RIVER, "rpg/items/fish/fish_moon_eel.png", "Belut ungu bercahaya yang aktif saat senja & malam.", 1.62f, nightBonus = true),
        FishSpecies("fish_sakura_betta", "Sakura Veilfish", FishRarity.EPIC, 260, 0.04f, WaterHabitat.LAKE, "rpg/items/fish/fish_sakura_betta.png", "Sirip lembut merah muda menyerupai kelopak sakura.", 1.68f),
        FishSpecies("fish_golden_arowana", "Gilded Sovereign", FishRarity.LEGENDARY, 490, 0.018f, WaterHabitat.OCEAN, "rpg/items/fish/fish_golden_arowana.png", "Ikan legenda bersisik emas dari perairan selatan.", 1.88f),
        FishSpecies("fish_starlight_sturgeon", "Starlight Sturgeon", FishRarity.LEGENDARY, 560, 0.02f, WaterHabitat.CAVE, "rpg/items/fish/fish_starlight_sturgeon.png", "Penguasa misterius kolam kristal kuno di bawah gua.", 1.96f, nightBonus = true)
    )

    val fishById: Map<String, FishSpecies> = fishSpecies.associateBy { it.id }

    val items: List<ItemDef> = listOf(
        // Tools
        ItemDef("tool_rod_basic", "Joran Kayu", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_rod_basic.png", "Joran pemula untuk memancing di danau, sungai, laut, dan gua."),
        ItemDef("tool_rod_bamboo", "Joran Bambu", ItemCategory.TOOLS, 1, 80, 220, "rpg/items/tools/tool_rod_bamboo.png", "Memperlebar zona tangkap & peluang ikan langka +25%."),
        ItemDef("tool_rod_golden", "Joran Emas", ItemCategory.TOOLS, 1, 260, 680, "rpg/items/tools/tool_rod_golden.png", "Joran terbaik: gigitan cepat & peluang Epic/Legendary +60%."),
        ItemDef("tool_axe", "Kapak Kayu", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_axe.png", "Menebang pohon Ek, Pinus, Sakura, dan Pohon Emas."),
        ItemDef("tool_axe_iron", "Kapak Besi", ItemCategory.TOOLS, 1, 110, 280, "rpg/items/tools/tool_axe_iron.png", "Menebang pohon 2x lebih cepat dengan ekstra kayu."),
        ItemDef("tool_pickaxe", "Beliung Besi", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_pickaxe.png", "Menambang batu, tembaga, besi, emas, dan kristal di gunung & gua."),
        ItemDef("tool_pickaxe_gold", "Beliung Emas", ItemCategory.TOOLS, 1, 180, 420, "rpg/items/tools/tool_pickaxe_gold.png", "Menambang 2x lebih cepat dengan peluang bonus permata."),
        ItemDef("tool_hoe", "Cangkul", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_hoe.png", "Menggemburkan lahan pertanian untuk menanam bibit."),
        ItemDef("tool_watering_can", "Kendi Air", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_watering_can.png", "Menyiram tanaman agar tumbuh 2x lebih cepat."),
        ItemDef("tool_hammer", "Palu Tukang", ItemCategory.TOOLS, 1, 0, 0, "rpg/items/tools/tool_hammer.png", "Menata furnitur & dekorasi di rumah maupun halaman."),

        // Minerals & Wood Materials
        ItemDef("res_wood", "Kayu", ItemCategory.MINERALS, 99, 6, 15, "rpg/items/minerals/res_wood.png", "Bahan utama membangun furnitur dan upgrade rumah."),
        ItemDef("res_hardwood", "Kayu Keras", ItemCategory.MINERALS, 99, 18, 42, "rpg/items/minerals/res_hardwood.png", "Kayu padat bermutu tinggi dari pohon tua."),
        ItemDef("res_stone", "Batu", ItemCategory.MINERALS, 99, 8, 18, "rpg/items/minerals/res_stone.png", "Batu kokoh untuk fondasi rumah, sumur, dan lentera."),
        ItemDef("ore_copper", "Bijih Tembaga", ItemCategory.MINERALS, 99, 22, 48, "rpg/items/minerals/ore_copper.png", "Bijih tembaga hangat dari tebing pegunungan."),
        ItemDef("ore_iron", "Bijih Besi", ItemCategory.MINERALS, 99, 36, 80, "rpg/items/minerals/ore_iron.png", "Bijih besi kuat untuk menempa alat & upgrade rumah."),
        ItemDef("ore_gold", "Bijih Emas", ItemCategory.MINERALS, 99, 75, 165, "rpg/items/minerals/ore_gold.png", "Emas murni langka dari pedalaman Gua Kristal."),
        ItemDef("gem_amethyst", "Permata Kecubung", ItemCategory.MINERALS, 99, 130, 0, "rpg/items/minerals/gem_amethyst.png", "Kristal ungu berkilau favorit Sora & Nenek Haru."),
        ItemDef("gem_emerald", "Permata Zamrud", ItemCategory.MINERALS, 99, 160, 0, "rpg/items/minerals/gem_emerald.png", "Kristal hijau langka bernilai jual sangat tinggi."),

        // Seeds & Saplings
        ItemDef("seed_carrot", "Bibit Wortel", ItemCategory.SEEDS, 99, 4, 10, "rpg/items/crops/seed_carrot.png", "Tumbuh cepat (56 detik). Cocok untuk pemula."),
        ItemDef("seed_potato", "Bibit Kentang", ItemCategory.SEEDS, 99, 6, 15, "rpg/items/crops/seed_potato.png", "Hasil panen stabil dan enak dimasak."),
        ItemDef("seed_tomato", "Bibit Tomat", ItemCategory.SEEDS, 99, 10, 24, "rpg/items/crops/seed_tomato.png", "Tomat merah segar digemari seluruh warga desa."),
        ItemDef("seed_wheat", "Bibit Gandum", ItemCategory.SEEDS, 99, 9, 20, "rpg/items/crops/seed_wheat.png", "Bahan dasar Ramen, Onigiri, dan Tart Desa."),
        ItemDef("seed_strawberry", "Bibit Stroberi", ItemCategory.SEEDS, 99, 14, 34, "rpg/items/crops/seed_strawberry.png", "Buah manis favorit Aoi dan Hana."),
        ItemDef("seed_pumpkin", "Bibit Labu", ItemCategory.SEEDS, 99, 18, 44, "rpg/items/crops/seed_pumpkin.png", "Tanaman premium bernilai jual paling tinggi."),
        ItemDef("seed_oak", "Tunas Pohon Ek", ItemCategory.SEEDS, 50, 8, 20, "rpg/items/crops/seed_oak.png", "Tanam di rumput untuk menumbuhkan Pohon Ek."),
        ItemDef("seed_sakura", "Tunas Sakura", ItemCategory.SEEDS, 50, 14, 35, "rpg/items/crops/seed_sakura.png", "Tanam di rumput untuk menumbuhkan Pohon Sakura."),

        // Harvested Crops
        ItemDef("crop_carrot", "Wortel Segar", ItemCategory.CROPS, 99, 28, 0, "rpg/items/crops/crop_carrot.png", "Wortel renyah hasil kebun Sakura Vale.", energyRestore = 10),
        ItemDef("crop_potato", "Kentang Emas", ItemCategory.CROPS, 99, 38, 0, "rpg/items/crops/crop_potato.png", "Kentang lembut hasil panen ladang.", energyRestore = 12),
        ItemDef("crop_tomato", "Tomat Matang", ItemCategory.CROPS, 99, 52, 0, "rpg/items/crops/crop_tomato.png", "Tomat manis merah menyala.", energyRestore = 14),
        ItemDef("crop_wheat", "Gandum Emas", ItemCategory.CROPS, 99, 46, 0, "rpg/items/crops/crop_wheat.png", "Bulir gandum harum untuk bahan dapur."),
        ItemDef("crop_strawberry", "Stroberi Manis", ItemCategory.CROPS, 99, 70, 0, "rpg/items/crops/crop_strawberry.png", "Stroberi segar penuh aroma musim semi.", energyRestore = 18),
        ItemDef("crop_pumpkin", "Labu Besar", ItemCategory.CROPS, 99, 92, 0, "rpg/items/crops/crop_pumpkin.png", "Labu raksasa bernilai tinggi di pasar."),

        // Wild Forageables
        ItemDef("forage_berry", "Beri Hutan", ItemCategory.FORAGE, 99, 16, 0, "rpg/items/food/forage_berry.png", "Beri manis liar dari Hutan Rimba Barat.", energyRestore = 15),
        ItemDef("forage_chestnut", "Kacang Kastanye", ItemCategory.FORAGE, 99, 20, 0, "rpg/items/food/forage_chestnut.png", "Kacang panggang gurih dari pepohonan.", energyRestore = 16),
        ItemDef("forage_tea_leaf", "Daun Teh Gunung", ItemCategory.FORAGE, 99, 24, 0, "rpg/items/food/forage_tea_leaf.png", "Daun teh harum dari lereng Pegunungan Utara.", energyRestore = 12),

        // Cooked Anime Dishes (Restore Energy + Great Gifts!)
        ItemDef("food_onigiri", "Onigiri Hangat", ItemCategory.FOOD, 50, 48, 65, "rpg/items/food/food_onigiri.png", "Nasi kepal tradisional isi gurih. Memulihkan +30 Energi.", energyRestore = 30),
        ItemDef("food_honey_tea", "Teh Madu Sakura", ItemCategory.FOOD, 50, 55, 75, "rpg/items/food/food_honey_tea.png", "Teh pegunungan hangat beraroma manis. Memulihkan +35 Energi.", energyRestore = 35),
        ItemDef("food_grilled_fish", "Ikan Bakar Kecap", ItemCategory.FOOD, 50, 68, 90, "rpg/items/food/food_grilled_fish.png", "Ikan segar panggang bara arang. Memulihkan +42 Energi.", energyRestore = 42),
        ItemDef("food_berry_tart", "Tart Stroberi Desa", ItemCategory.FOOD, 50, 95, 130, "rpg/items/food/food_berry_tart.png", "Kue tart renyah isi stroberi & beri. Memulihkan +48 Energi.", energyRestore = 48),
        ItemDef("food_yakitori", "Sate Yakitori", ItemCategory.FOOD, 50, 82, 110, "rpg/items/food/food_yakitori.png", "Sate panggang saus manis gurih. Memulihkan +45 Energi.", energyRestore = 45),
        ItemDef("food_sushi", "Sushi Salmon Vale", ItemCategory.FOOD, 50, 120, 160, "rpg/items/food/food_sushi.png", "Sushi ikan segar resep Chef Daiki. Memulihkan +58 Energi.", energyRestore = 58),
        ItemDef("food_tamago_sushi", "Tamago Sushi", ItemCategory.FOOD, 50, 88, 120, "rpg/items/food/food_tamago_sushi.png", "Sushi lembut manis disukai anak-anak. Memulihkan +45 Energi.", energyRestore = 45),
        ItemDef("food_tempura", "Udang Tempura", ItemCategory.FOOD, 50, 110, 150, "rpg/items/food/food_tempura.png", "Udang goreng tepung renyah keemasan. Memulihkan +55 Energi.", energyRestore = 55),
        ItemDef("food_calamari", "Cumi Goreng Renyah", ItemCategory.FOOD, 50, 125, 0, "rpg/items/food/food_calamari.png", "Cumi karang goreng gurih. Memulihkan +56 Energi.", energyRestore = 56),
        ItemDef("food_takoyaki", "Takoyaki Festival", ItemCategory.FOOD, 50, 195, 0, "rpg/items/food/food_takoyaki.png", "Bola gurita saus khas festival Sakura Vale. Memulihkan +70 Energi.", energyRestore = 70),
        ItemDef("food_ramen", "Ramen Sakura Spesial", ItemCategory.FOOD, 50, 155, 210, "rpg/items/food/food_ramen.png", "Semangkuk ramen kaldu kaya rasa. Memulihkan +75 Energi.", energyRestore = 75),
        ItemDef("food_bento", "Bento Petualang", ItemCategory.FOOD, 50, 210, 0, "rpg/items/food/food_bento.png", "Bekal lengkap penuh gizi. Memulihkan +90 Energi.", energyRestore = 90),
        ItemDef("food_herb_elixir", "Ramuan Herbal Hutan", ItemCategory.FOOD, 50, 140, 180, "rpg/items/food/food_herb_elixir.png", "Minuman penyegar dari teh & beri hutan. Memulihkan +100 Energi.", energyRestore = 100),

        // Placeable Furniture & Decorations (Indoor & Outdoor!)
        ItemDef("fence_wood", "Pagar Kayu", ItemCategory.FURNITURE, 50, 8, 20, "rpg/items/furniture/fence_wood.png", "Pagar pembatas halaman yang rapi.", placeable = true, rotatable = true, woodCost = 2),
        ItemDef("bench_wood", "Bangku Taman", ItemCategory.FURNITURE, 20, 18, 45, "rpg/items/furniture/bench_wood.png", "Bangku kayu santai untuk halaman & rumah.", placeable = true, rotatable = true, woodCost = 5),
        ItemDef("wooden_table", "Meja Kayu", ItemCategory.FURNITURE, 20, 25, 60, "rpg/items/furniture/wooden_table.png", "Meja kayu hangat untuk ruang makan atau taman.", placeable = true, rotatable = true, woodCost = 6),
        ItemDef("bookshelf", "Rak Buku Kayu", ItemCategory.FURNITURE, 15, 35, 85, "rpg/items/furniture/bookshelf.png", "Rak penuh buku cerita & catatan desa.", placeable = true, rotatable = false, woodCost = 8),
        ItemDef("cozy_bed", "Tempat Tidur Kayu", ItemCategory.FURNITURE, 5, 65, 160, "rpg/items/furniture/cozy_bed.png", "Kasur nyaman untuk dekorasi rumah.", placeable = true, rotatable = false, woodCost = 10),
        ItemDef("storage_chest", "Peti Penyimpanan", ItemCategory.FURNITURE, 10, 30, 75, "rpg/items/furniture/storage_chest.png", "Peti kayu kokoh untuk menyimpan koleksi.", placeable = true, rotatable = false, woodCost = 8, stoneCost = 2),
        ItemDef("wooden_cart", "Gerobak Panen", ItemCategory.FURNITURE, 10, 48, 120, "rpg/items/furniture/wooden_cart.png", "Gerobak kayu pedesaan penuh hasil ladang.", placeable = true, rotatable = false, woodCost = 10, stoneCost = 2),
        ItemDef("lamp_post", "Lampu Jalan Desa", ItemCategory.DECORATIONS, 30, 22, 55, "rpg/items/furniture/lamp_post.png", "Menyala hangat otomatis saat senja & malam.", placeable = true, rotatable = false, woodCost = 3, stoneCost = 2),
        ItemDef("flower_pot", "Pot Tanaman Hias", ItemCategory.DECORATIONS, 30, 12, 30, "rpg/items/furniture/flower_pot.png", "Tanaman pot hijau untuk dalam & luar rumah.", placeable = true, rotatable = false, woodCost = 2, stoneCost = 1),
        ItemDef("sign_post", "Papan Penunjuk", ItemCategory.DECORATIONS, 20, 10, 25, "rpg/items/furniture/sign_post.png", "Papan kayu penanda sudut halaman.", placeable = true, rotatable = true, woodCost = 3),
        ItemDef("campfire", "Kuali Api Unggun", ItemCategory.DECORATIONS, 15, 28, 70, "rpg/items/furniture/campfire.png", "Api unggun hangat penghangat malam.", placeable = true, rotatable = false, woodCost = 4, stoneCost = 4),
        ItemDef("lantern_stone", "Patung Lentera Batu", ItemCategory.DECORATIONS, 20, 35, 90, "rpg/items/furniture/lantern_stone.png", "Ornamen batu kuil klasik bercahaya lembut.", placeable = true, rotatable = false, woodCost = 1, stoneCost = 6),
        ItemDef("scarecrow", "Orang-Orangan Sawah", ItemCategory.DECORATIONS, 10, 30, 80, "rpg/items/furniture/scarecrow.png", "Penjaga ramah untuk kebun sayurmu.", placeable = true, rotatable = true, woodCost = 5, stoneCost = 1),
        ItemDef("well_stone", "Sumur Batu Desa", ItemCategory.DECORATIONS, 5, 80, 200, "rpg/items/furniture/well_stone.png", "Sumur air jernih penghias pekarangan.", placeable = true, rotatable = false, woodCost = 4, stoneCost = 10)
    ) + fishSpecies.map { f ->
        ItemDef(
            id = f.id,
            name = f.name,
            category = ItemCategory.FISH,
            maxStack = 99,
            sellPrice = f.sellPrice,
            buyPrice = 0,
            spritePath = f.spritePath,
            description = "${f.rarity.label} • ${f.habitat.label}: ${f.description}"
        )
    }

    val itemById: Map<String, ItemDef> = items.associateBy { it.id }
    val buildableItems: List<ItemDef> = items.filter { it.placeable }
    val shopItems: List<ItemDef> = items.filter { it.buyPrice > 0 }

    val recipes: List<CraftingRecipe> = listOf(
        // Crafting Recipes (Furniture, Decor & Upgraded Tools)
        CraftingRecipe("rec_lamp", RecipeType.CRAFTING, "lamp_post", 1, listOf("res_wood" to 3, "res_stone" to 2), 18, "Lentera jalan menyala otomatis di malam hari."),
        CraftingRecipe("rec_bench", RecipeType.CRAFTING, "bench_wood", 1, listOf("res_wood" to 5), 16, "Bangku kayu nyaman untuk pekarangan atau rumah."),
        CraftingRecipe("rec_table", RecipeType.CRAFTING, "wooden_table", 1, listOf("res_wood" to 6), 18, "Meja kayu hangat untuk ruang keluarga."),
        CraftingRecipe("rec_bookshelf", RecipeType.CRAFTING, "bookshelf", 1, listOf("res_wood" to 8), 24, "Rak buku kayu penuh koleksi desa."),
        CraftingRecipe("rec_chest", RecipeType.CRAFTING, "storage_chest", 1, listOf("res_wood" to 8, "ore_copper" to 1), 26, "Peti simpan tambahan dengan engsel tembaga."),
        CraftingRecipe("rec_campfire", RecipeType.CRAFTING, "campfire", 1, listOf("res_wood" to 4, "res_stone" to 4), 22, "Api unggun & kuali hangat di tepi danau."),
        CraftingRecipe("rec_well", RecipeType.CRAFTING, "well_stone", 1, listOf("res_wood" to 4, "res_stone" to 10), 35, "Sumur air batu klasik untuk halaman rumah."),
        CraftingRecipe("rec_cart", RecipeType.CRAFTING, "wooden_cart", 1, listOf("res_wood" to 10, "ore_iron" to 1), 38, "Gerobak panen pedesaan beroda besi."),
        CraftingRecipe("rec_axe_iron", RecipeType.CRAFTING, "tool_axe_iron", 1, listOf("res_wood" to 4, "ore_iron" to 3), 55, "Tempa Kapak Besi untuk menebang 2x lebih cepat."),
        CraftingRecipe("rec_pick_gold", RecipeType.CRAFTING, "tool_pickaxe_gold", 1, listOf("res_hardwood" to 3, "ore_gold" to 3), 75, "Tempa Beliung Emas untuk bonus permata tambang."),

        // Cooking Recipes (12 Anime Dishes!)
        CraftingRecipe("cook_onigiri", RecipeType.COOKING, "food_onigiri", 1, listOf("crop_wheat" to 1, "forage_berry" to 1), 16, "Onigiri hangat +30 Energi."),
        CraftingRecipe("cook_honey_tea", RecipeType.COOKING, "food_honey_tea", 1, listOf("forage_tea_leaf" to 1, "forage_berry" to 1), 18, "Teh madu sakura harum +35 Energi."),
        CraftingRecipe("cook_grilled_fish", RecipeType.COOKING, "food_grilled_fish", 1, listOf("fish_pond_carp" to 1), 22, "Ikan mas bakar kecap +42 Energi."),
        CraftingRecipe("cook_yakitori", RecipeType.COOKING, "food_yakitori", 1, listOf("crop_carrot" to 1, "forage_chestnut" to 1), 24, "Sate panggang gurih +45 Energi."),
        CraftingRecipe("cook_tamago", RecipeType.COOKING, "food_tamago_sushi", 1, listOf("crop_wheat" to 1, "crop_carrot" to 1), 24, "Tamago sushi manis lembut +45 Energi."),
        CraftingRecipe("cook_berry_tart", RecipeType.COOKING, "food_berry_tart", 1, listOf("crop_strawberry" to 1, "crop_wheat" to 1), 30, "Tart stroberi desa favorit Hana & Aoi (+48 Energi)."),
        CraftingRecipe("cook_tempura", RecipeType.COOKING, "food_tempura", 1, listOf("fish_ocean_shrimp" to 1, "crop_wheat" to 1), 34, "Udang tempura renyah pantai (+55 Energi)."),
        CraftingRecipe("cook_calamari", RecipeType.COOKING, "food_calamari", 1, listOf("fish_reef_squid" to 1, "crop_wheat" to 1), 36, "Cumi karang goreng gurih (+56 Energi)."),
        CraftingRecipe("cook_sushi", RecipeType.COOKING, "food_sushi", 1, listOf("fish_brook_trout" to 1, "crop_wheat" to 1), 40, "Sushi segar resep Chef Daiki (+58 Energi)."),
        CraftingRecipe("cook_takoyaki", RecipeType.COOKING, "food_takoyaki", 1, listOf("fish_coral_octopus" to 1, "crop_wheat" to 1), 48, "Takoyaki festival spesial (+70 Energi)."),
        CraftingRecipe("cook_ramen", RecipeType.COOKING, "food_ramen", 1, listOf("crop_wheat" to 1, "crop_tomato" to 1, "crop_carrot" to 1), 52, "Ramen sakura hangat kaldu kaya rasa (+75 Energi)."),
        CraftingRecipe("cook_bento", RecipeType.COOKING, "food_bento", 1, listOf("crop_potato" to 1, "crop_tomato" to 1, "fish_pond_carp" to 1), 60, "Bento petualang super lengkap (+90 Energi)."),
        CraftingRecipe("cook_elixir", RecipeType.COOKING, "food_herb_elixir", 1, listOf("forage_tea_leaf" to 2, "forage_berry" to 2), 45, "Ramuan herbal pemulih 100 Energi penuh!")
    )

    val houseUpgrades: List<HouseUpgradeSpec> = listOf(
        HouseUpgradeSpec(1, "Kabin Kayu Pemula", 0, 0, 0, 0, "Rumah kayu hangat di Sakura Vale lengkap dengan kasur, dapur & meja kerja."),
        HouseUpgradeSpec(2, "Rumah Ladang Cerobong", 240, 18, 8, 2, "Memperindah eksterior rumah bergaya pedesaan Jepang dengan atap hangat."),
        HouseUpgradeSpec(3, "Vila Atap Merah Sakura", 580, 36, 16, 5, "Vila tradisional beratap merah sakura megah di jantung desa.")
    )

    // 8 Distinct Living Village NPCs with Daily Schedules, Anime Portraits & Gift Tastes!
    // Note: Coordinates use LibGDX bottom-up Y (gdxY = mapHeight - 1 - tiledY).
    val npcs: List<NpcDef> = listOf(
        NpcDef(
            id = "npc_hana",
            name = "Hana",
            roleTitle = "Pemilik Toko Serba Ada",
            sheetPath = "rpg/characters/npc/npc_hana_sheet.png",
            facePath = "rpg/characters/npc/npc_hana_face.png",
            favoriteItems = listOf("crop_strawberry", "crop_tomato", "forage_berry"),
            lovedItems = listOf("food_berry_tart", "food_honey_tea", "fish_sakura_betta"),
            morningLines = listOf(
                "Selamat pagi! Bibit stroberi dan gandum baru saja tiba di Toko Serba Ada.",
                "Kalau tasmu penuh hasil panen atau ikan, jual saja di sini ya!"
            ),
            afternoonLines = listOf(
                "Pasar ramai sekali siang ini! Jangan lupa cek bibit labu untuk untung besar.",
                "Cuaca Sakura Vale hari ini pas sekali untuk merawat kebun."
            ),
            eveningLines = listOf(
                "Senja di Alun-Alun Sakura selalu terasa damai setelah seharian berjualan.",
                "Terima kasih sudah mampir hari ini! Istirahatlah di rumah kalau energimu mulai turun."
            ),
            giftThankLine = "Wah, terima kasih banyak! Kamu baik sekali mengingatku.",
            giftLoveLine = "Astaga, ini kesukaanku!! Hati rasanya hangat sekali menerimanya!",
            opensShop = true,
            schedule = listOf(
                NpcScheduleSlot(6f, 17f, MapId.GENERAL_STORE, 10, 11, "Menjaga Toko Serba Ada"),
                NpcScheduleSlot(17f, 21f, MapId.OVERWORLD, 38, 37, "Bersantai di Alun-Alun"),
                NpcScheduleSlot(21f, 24f, MapId.GENERAL_STORE, 9, 10, "Merapikan Toko"),
                NpcScheduleSlot(0f, 6f, MapId.GENERAL_STORE, 9, 10, "Beristirahat")
            )
        ),
        NpcDef(
            id = "npc_kenji",
            name = "Master Kenji",
            roleTitle = "Pandai Besi & Arsitek",
            sheetPath = "rpg/characters/npc/npc_kenji_sheet.png",
            facePath = "rpg/characters/npc/npc_kenji_face.png",
            favoriteItems = listOf("ore_copper", "ore_iron", "res_hardwood"),
            lovedItems = listOf("ore_gold", "food_ramen", "food_bento"),
            morningLines = listOf(
                "Tungku sudah membara! Bawa bijih besi dan emas dari Gua Kristal ke bengkelku.",
                "Ingin memperbesar rumahmu? Aku bisa membangun Vila Sakura terbaik di lembah ini."
            ),
            afternoonLines = listOf(
                "Beliung Emas bisa menghancurkan urat kristal dua kali lebih cepat!",
                "Suara palu beradu logam adalah musik terbaik di desa."
            ),
            eveningLines = listOf(
                "Kerja keras hari ini terbayar lunas. Semangkuk ramen hangat pasti nikmat sekali.",
                "Jangan lupa periksa meja kerajinan untuk membuat furnitur rumahmu."
            ),
            giftThankLine = "Haha, mantap! Bahan bagus seperti ini selalu berguna di bengkel.",
            giftLoveLine = "Luar biasa!! Ini kualitas kelas satu, terima kasih banyak sobat!",
            opensBlacksmith = true,
            schedule = listOf(
                NpcScheduleSlot(6f, 18f, MapId.BLACKSMITH, 9, 10, "Menempa di Bengkel"),
                NpcScheduleSlot(18f, 21f, MapId.OVERWORLD, 44, 31, "Mengobrol di Pasar"),
                NpcScheduleSlot(21f, 24f, MapId.BLACKSMITH, 9, 10, "Membersihkan Tungku"),
                NpcScheduleSlot(0f, 6f, MapId.BLACKSMITH, 9, 10, "Beristirahat")
            )
        ),
        NpcDef(
            id = "npc_daiki",
            name = "Chef Daiki",
            roleTitle = "Koki Kedai Teh Sakura",
            sheetPath = "rpg/characters/npc/npc_daiki_sheet.png",
            facePath = "rpg/characters/npc/npc_daiki_face.png",
            favoriteItems = listOf("crop_carrot", "crop_potato", "fish_ocean_shrimp"),
            lovedItems = listOf("fish_crimson_koi", "fish_coral_octopus", "forage_tea_leaf"),
            morningLines = listOf(
                "Selamat datang di Kedai Teh Sakura! Aroma kaldu ramen baru saja matang.",
                "Bawa ikan dan sayur hasil panenmu ke kompor dapur untuk memasak bekal energi!"
            ),
            afternoonLines = listOf(
                "Masakan hangat memulihkan energimu jauh lebih besar dibanding dimakan mentah.",
                "Cobalah membuat Takoyaki Festival jika kamu berhasil menangkap Gurita Koral di pantai!"
            ),
            eveningLines = listOf(
                "Malam hari paling nikmat menyeruput Teh Madu Sakura hangat.",
                "Pintu dapurku selalu terbuka untuk koki muda sepertimu."
            ),
            giftThankLine = "Ooh bahan segar! Aku bisa langsung mengolahnya di dapur.",
            giftLoveLine = "Sempurna! Bahan langka seperti ini impian setiap koki di Sakura Vale!",
            opensCooking = true,
            schedule = listOf(
                NpcScheduleSlot(6f, 19f, MapId.TEAHOUSE, 10, 10, "Memasak di Kedai Teh"),
                NpcScheduleSlot(19f, 22f, MapId.OVERWORLD, 40, 30, "Menikmati Angin Malam"),
                NpcScheduleSlot(22f, 24f, MapId.TEAHOUSE, 10, 10, "Menutup Dapur"),
                NpcScheduleSlot(0f, 6f, MapId.TEAHOUSE, 10, 10, "Beristirahat")
            )
        ),
        NpcDef(
            id = "npc_taro",
            name = "Kakek Taro",
            roleTitle = "Master Pemancing Lembah",
            sheetPath = "rpg/characters/npc/npc_taro_sheet.png",
            facePath = "rpg/characters/npc/npc_taro_face.png",
            favoriteItems = listOf("fish_pond_carp", "fish_brook_trout", "food_onigiri"),
            lovedItems = listOf("fish_golden_arowana", "fish_starlight_sturgeon", "food_grilled_fish"),
            morningLines = listOf(
                "Pagi yang tenang di Danau Bisikan! Ikan Sakura Veilfish sering muncul di sini.",
                "Ingat, ketuk langsung permukaan air untuk berjalan ke dermaga dan melempar kail!"
            ),
            afternoonLines = listOf(
                "Siang begini ombak Pantai Pasir Emas membawa Udang Karang dan Gurita Koral!",
                "Kalau ingin menangkap Starlight Sturgeon, cobalah memancing di kolam Gua Kristal."
            ),
            eveningLines = listOf(
                "Saat malam tiba, Moonlight Eel keluar menyusuri Lembah Sungai.",
                "Kesabaran dan ritme adalah rahasia pemancing sejati, nak."
            ),
            giftThankLine = "Ho ho, terima kasih nak! Paling pas menemani sore hari.",
            giftLoveLine = "Wahaha! Kakek sampai terharu, ini benar-benar luar biasa!",
            schedule = listOf(
                NpcScheduleSlot(6f, 13f, MapId.OVERWORLD, 39, 21, "Memancing di Dermaga Danau"),
                NpcScheduleSlot(13f, 19f, MapId.OVERWORLD, 26, 7, "Memancing di Dermaga Pantai"),
                NpcScheduleSlot(19f, 24f, MapId.OVERWORLD, 67, 41, "Bersantai di Pondok Timur"),
                NpcScheduleSlot(0f, 6f, MapId.OVERWORLD, 67, 41, "Beristirahat")
            )
        ),
        NpcDef(
            id = "npc_haru",
            name = "Nenek Haru",
            roleTitle = "Tetua Desa Sakura Vale",
            sheetPath = "rpg/characters/npc/npc_haru_sheet.png",
            facePath = "rpg/characters/npc/npc_haru_face.png",
            favoriteItems = listOf("forage_tea_leaf", "crop_pumpkin", "food_onigiri"),
            lovedItems = listOf("food_honey_tea", "gem_amethyst", "food_herb_elixir"),
            morningLines = listOf(
                "Selamat datang di Sakura Vale, nak. Lembah ini menunggu sentuhan hangatmu.",
                "Ketika kamu mencapai Level 2, gerbang Hutan Sakura Rahasia di barat laut akan menyambutmu."
            ),
            afternoonLines = listOf(
                "Konon di tenggara sana terdapat Reruntuhan Kuno yang menyimpan peti peninggalan leluhur.",
                "Rawat persahabatan dengan para warga, maka desa ini akan terasa seperti rumah sejati."
            ),
            eveningLines = listOf(
                "Lentera-lentera desa mulai menyala. Tidurlah di kasur rumahmu untuk menyambut pagi esok.",
                "Setiap hari baru menumbuhkan tanaman yang sudah kamu siram dan mengisi kembali gua kristal."
            ),
            giftThankLine = "Oh manis sekali, terima kasih nak. Semoga harimu selalu diberkahi.",
            giftLoveLine = "Ya ampun, Nenek suka sekali! Hatiku berbunga seperti musim sakura!",
            schedule = listOf(
                NpcScheduleSlot(6f, 11f, MapId.ELDER_HOUSE, 9, 8, "Merawat Altar Pendopo"),
                NpcScheduleSlot(11f, 18f, MapId.OVERWORLD, 39, 43, "Berjalan di Kuil Alun-Alun"),
                NpcScheduleSlot(18f, 24f, MapId.ELDER_HOUSE, 9, 8, "Minum Teh di Pendopo"),
                NpcScheduleSlot(0f, 6f, MapId.ELDER_HOUSE, 9, 8, "Beristirahat")
            )
        ),
        NpcDef(
            id = "npc_ren",
            name = "Ren",
            roleTitle = "Peneliti Geologi & Tambang",
            sheetPath = "rpg/characters/npc/npc_ren_sheet.png",
            facePath = "rpg/characters/npc/npc_ren_face.png",
            favoriteItems = listOf("res_stone", "ore_copper", "food_yakitori"),
            lovedItems = listOf("gem_emerald", "gem_amethyst", "food_bento"),
            morningLines = listOf(
                "Formasi batuan di Pegunungan Utara sangat kaya tembaga dan besi!",
                "Masuklah ke Gua Kristal di utara untuk menemukan Kecubung dan Zamrud langka."
            ),
            afternoonLines = listOf(
                "Tahukah kamu? Di dalam Gua Kristal ada kolam bawah tanah dengan ikan bercahaya!",
                "Bawa makanan saat menambang supaya energimu tetap penuh."
            ),
            eveningLines = listOf(
                "Aku baru selesai mencatat sampel mineral hari ini di dekat alun-alun.",
                "Kalau kamu menemukan peti harta di gua, isinya pasti sangat berharga!"
            ),
            giftThankLine = "Menarik! Aku akan mencatatnya di jurnal penelitianku.",
            giftLoveLine = "Fantastis! Ini spesimen paling menakjubkan yang pernah kulihat!",
            schedule = listOf(
                NpcScheduleSlot(6f, 12f, MapId.OVERWORLD, 65, 53, "Meneliti Tebing Gunung"),
                NpcScheduleSlot(12f, 18f, MapId.MINING_CAVE, 17, 16, "Menjelajah Gua Kristal"),
                NpcScheduleSlot(18f, 24f, MapId.OVERWORLD, 42, 37, "Beristirahat di Alun-Alun"),
                NpcScheduleSlot(0f, 6f, MapId.OVERWORLD, 65, 53, "Mengamati Bintang")
            )
        ),
        NpcDef(
            id = "npc_aoi",
            name = "Aoi",
            roleTitle = "Warga Ceria Sakura Vale",
            sheetPath = "rpg/characters/npc/npc_aoi_sheet.png",
            facePath = "rpg/characters/npc/npc_aoi_face.png",
            favoriteItems = listOf("forage_berry", "crop_carrot", "food_tamago_sushi"),
            lovedItems = listOf("crop_strawberry", "food_berry_tart", "food_takoyaki"),
            morningLines = listOf(
                "Hei! Aku lihat kucing dan anak anjing sedang bermain di dekat ladang tadi!",
                "Kincir angin di samping kebunmu berputar kencang sekali pagi ini!"
            ),
            afternoonLines = listOf(
                "Kamu sudah coba menanam stroberi? Katanya rasanya semanis permen!",
                "Ayo keliling desa! Banyak sudut rahasia dengan beri liar dan kacang kastanye."
            ),
            eveningLines = listOf(
                "Kunang-kunang dan lentera desa kelihatan keren banget waktu malam!",
                "Besok kita main lagi ya!"
            ),
            giftThankLine = "Asyiiik! Makasih banyak ya!",
            giftLoveLine = "WOOOW!! Ini makanan favoritku sedunia! Makasih banyaaak!!",
            schedule = listOf(
                NpcScheduleSlot(6f, 12f, MapId.OVERWORLD, 22, 37, "Bermain Dekat Kincir Ladang"),
                NpcScheduleSlot(12f, 18f, MapId.OVERWORLD, 37, 36, "Bermain di Alun-Alun"),
                NpcScheduleSlot(18f, 24f, MapId.OVERWORLD, 64, 37, "Pulang ke Rumah Timur"),
                NpcScheduleSlot(0f, 6f, MapId.OVERWORLD, 64, 37, "Tidur")
            )
        ),
        NpcDef(
            id = "npc_sora",
            name = "Sora",
            roleTitle = "Penjaga Roh Sakura & Reruntuhan",
            sheetPath = "rpg/characters/npc/npc_sora_sheet.png",
            facePath = "rpg/characters/npc/npc_sora_face.png",
            favoriteItems = listOf("forage_tea_leaf", "fish_moon_eel", "food_herb_elixir"),
            lovedItems = listOf("gem_amethyst", "gem_emerald", "fish_sakura_betta"),
            morningLines = listOf(
                "Angin membawa kelopak merah muda... Hutan Sakura Rahasia menyimpan ketenangan abadi.",
                "Setiap pohon yang kamu tanam membuat roh lembah tersenyum."
            ),
            afternoonLines = listOf(
                "Patung-patung tua di Reruntuhan Kuno menjaga harta bagi petualang yang bijaksana.",
                "Dengarkan suara air terjun utara; ia menyanyikan kisah Sakura Vale."
            ),
            eveningLines = listOf(
                "Di bawah sinar bulan, air danau dan kristal gua saling memantulkan cahaya.",
                "Semoga langkahmu di lembah ini selalu damai."
            ),
            giftThankLine = "Pemberian tulus dari alam... terima kasih, petualang.",
            giftLoveLine = "Cahaya murni menyelimuti pemberian ini. Roh Sakura memberkatimu!",
            schedule = listOf(
                NpcScheduleSlot(6f, 14f, MapId.OVERWORLD, 12, 57, "Menjaga Kuil Sakura"),
                NpcScheduleSlot(14f, 22f, MapId.OVERWORLD, 67, 22, "Bermeditasi di Reruntuhan"),
                NpcScheduleSlot(22f, 24f, MapId.OVERWORLD, 12, 57, "Menatap Bulan di Kuil"),
                NpcScheduleSlot(0f, 6f, MapId.OVERWORLD, 12, 57, "Menjaga Kuil Sakura")
            )
        )
    )

    val npcById: Map<String, NpcDef> = npcs.associateBy { it.id }

    val quests: List<QuestDef> = listOf(
        // Main Story Quests
        QuestDef("m_welcome_explore", QuestType.MAIN, "npc_haru", "Menjelajah Sakura Vale", "Temukan 5 wilayah berbeda di dunia Sakura Vale", 5, 160, 110),
        QuestDef("m_meet_villagers", QuestType.MAIN, "npc_haru", "Salam Persahabatan", "Berbicara dengan 4 warga desa Sakura Vale", 4, 140, 95),
        QuestDef("m_crystal_miner", QuestType.MAIN, "npc_ren", "Ekspedisi Gua Kristal", "Tambang 6 bongkahan batu atau bijih mineral", 6, 180, 125),
        QuestDef("m_chef_lesson", QuestType.MAIN, "npc_daiki", "Rahasia Dapur Sakura", "Masak 2 hidangan hangat di kompor rumah atau Kedai Teh", 2, 175, 115),
        QuestDef("m_dream_home", QuestType.MAIN, "npc_kenji", "Renovasi Rumah Impian", "Upgrade rumahmu ke Tier 2 bersama Master Kenji", 1, 300, 200),

        // Side / Villager Quests
        QuestDef("s_taro_angler", QuestType.SIDE, "npc_taro", "Tantangan Kakek Taro", "Tangkap 5 ekor ikan dari danau, sungai, laut, atau gua", 5, 150, 95),
        QuestDef("s_hana_harvest", QuestType.SIDE, "npc_hana", "Pasokan Segar Pasar", "Panen 4 tanaman matang dari lahan pertanian", 4, 145, 90),
        QuestDef("s_gift_friend", QuestType.SIDE, "npc_aoi", "Hadiah Dari Hati", "Berikan 2 hadiah kepada warga desa Sakura Vale", 2, 130, 85),

        // Daily Quests
        QuestDef("q_catch_3_fish", QuestType.DAILY, "npc_taro", "Pemancing Pagi", "Tangkap 3 ekor ikan di perairan mana saja", 3, 85, 50),
        QuestDef("q_collect_10_wood", QuestType.DAILY, "npc_kenji", "Pengumpul Kayu", "Kumpulkan 10 kayu dari menebang pohon", 10, 75, 45),
        QuestDef("q_plant_5_crops", QuestType.DAILY, "npc_hana", "Tangan Hijau", "Tanam 5 bibit sayuran atau buah di kebun", 5, 70, 40),
        QuestDef("q_earn_150_coins", QuestType.DAILY, "npc_hana", "Pedagang Lembah", "Dapatkan 150 koin dari menjual hasil alam", 150, 110, 65),
        QuestDef("q_build_2_items", QuestType.DAILY, "npc_kenji", "Dekorator Sakura", "Pasang atau buat 2 furnitur & dekorasi", 2, 120, 70)
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("a_first_fish", "Tangkapan Pertama", "Tangkap ikan pertamamu di Sakura Vale", 1, 60, 35),
        AchievementDef("a_fish_15", "Pemancing Andal", "Tangkap total 15 ikan", 15, 200, 130),
        AchievementDef("a_rare_fish", "Pemburu Langka", "Tangkap ikan Langka, Epik, atau Legendaris", 1, 220, 150),
        AchievementDef("a_collector_8", "Ensiklopedia Perairan", "Koleksi 8 spesies ikan berbeda", 8, 360, 240),
        AchievementDef("a_chop_10", "Penebang Terampil", "Tebang 10 pohon di sekitar lembah", 10, 150, 95),
        AchievementDef("a_mine_10", "Penambang Kristal", "Hancurkan 10 batu tambang atau kristal", 10, 190, 120),
        AchievementDef("a_harvest_10", "Petani Makmur", "Panen 10 tanaman dari kebun", 10, 175, 115),
        AchievementDef("a_cook_5", "Koki Lembah Sakura", "Masak 5 hidangan anime di dapur", 5, 200, 130),
        AchievementDef("a_explore_8", "Penjelajah Dunia", "Temukan 8 wilayah berbeda di Sakura Vale", 8, 280, 190),
        AchievementDef("a_house_tier2", "Rumah Ladang", "Upgrade rumah ke Tier 2", 1, 260, 165),
        AchievementDef("a_house_tier3", "Vila Sakura Megah", "Upgrade rumah ke Tier 3", 1, 550, 340)
    )

    fun expRequiredForLevel(level: Int): Int = 100 + (level - 1) * 70
}
