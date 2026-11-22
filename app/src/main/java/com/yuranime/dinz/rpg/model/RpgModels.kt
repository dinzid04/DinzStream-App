package com.yuranime.dinz.rpg.model

enum class Direction(val key: String) {
    DOWN("down"),
    UP("up"),
    LEFT("left"),
    RIGHT("right")
}

enum class PlayerAction {
    IDLE, WALK, RUN,
    FISH_CAST, FISH_WAIT, FISH_REEL,
    AXE, HOE, WATER, HARVEST, INTERACT
}

enum class ItemCategory(val label: String) {
    TOOLS("Alat"),
    FISH("Ikan"),
    SEEDS("Bibit"),
    CROPS("Panen"),
    WOOD("Kayu"),
    STONE("Batu"),
    FURNITURE("Furnitur"),
    DECORATIONS("Dekorasi")
}

enum class FishRarity(val label: String, val colorHex: Long, val weight: Float, val expReward: Int) {
    COMMON("Common", 0xFF94A3B8, 48f, 12),
    UNCOMMON("Uncommon", 0xFF4ADE80, 28f, 22),
    RARE("Rare", 0xFF38BDF8, 15f, 40),
    EPIC("Epic", 0xFFC084FC, 7f, 75),
    LEGENDARY("Legendary", 0xFFFACC15, 2f, 150)
}

enum class WaterHabitat(val label: String) {
    POND("Kolam Homestead"),
    RIVER("Sungai Willow"),
    BOTH("Kolam & Sungai")
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
    val difficulty: Float = 1.0f, // 0.8 (easy) .. 2.0 (legendary fast bar)
    val nightBonus: Boolean = false
)

enum class TreeType(
    val id: String,
    val displayName: String,
    val maxHealth: Int,
    val woodYield: Int,
    val hardwoodChance: Float,
    val regrowSeconds: Float,
    val expReward: Int
) {
    OAK("oak", "Pohon Ek", 4, 4, 0.2f, 90f, 16),
    PINE("pine", "Pohon Pinus", 5, 5, 0.35f, 110f, 20),
    SAKURA("sakura", "Pohon Sakura", 3, 3, 0.5f, 100f, 24)
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

enum class CropType(
    val id: String,
    val displayName: String,
    val seedItemId: String,
    val harvestItemId: String,
    val stageDurationSeconds: Float, // Per growth stage (4 transitions total)
    val sellPrice: Int,
    val expReward: Int
) {
    CARROT("carrot", "Wortel", "seed_carrot", "crop_carrot", 14f, 28, 14),
    POTATO("potato", "Kentang", "seed_potato", "crop_potato", 18f, 38, 18),
    TOMATO("tomato", "Tomat", "seed_tomato", "crop_tomato", 22f, 52, 24),
    PUMPKIN("pumpkin", "Labu", "seed_pumpkin", "crop_pumpkin", 28f, 88, 36)
}

data class CropPlot(
    val tileX: Int,
    val tileY: Int,
    var isTilled: Boolean = false,
    var isWatered: Boolean = false,
    var cropType: CropType? = null,
    var stage: Int = 0, // 0=seed, 1=sprout, 2=growing, 3=mature, 4=harvestable
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
    val stoneCost: Int = 0
)

data class InventoryStack(
    val itemId: String,
    val quantity: Int
)

data class PlacedObject(
    val id: String,
    val itemId: String,
    val tileX: Int,
    val tileY: Int,
    val rotation: Int = 0 // 0, 90, 180, 270
)

data class HouseUpgradeSpec(
    val tier: Int,
    val name: String,
    val coinCost: Int,
    val woodCost: Int,
    val stoneCost: Int,
    val description: String
)

enum class ContextActionType {
    NONE,
    FISH,
    CHOP_TREE,
    TILL_SOIL,
    PLANT_SEED,
    WATER_CROP,
    HARVEST_CROP,
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
    val habitat: WaterHabitat = WaterHabitat.POND
)

enum class FishingPhase {
    IDLE, CASTING, WAITING, BITE, MINIGAME, CAUGHT, ESCAPED
}

data class FishingUiState(
    val phase: FishingPhase = FishingPhase.IDLE,
    val targetFish: FishSpecies? = null,
    val indicatorPos: Float = 0.5f,     // 0f..1f moving marker
    val sweetZoneCenter: Float = 0.5f,  // 0f..1f sweet spot center
    val sweetZoneHalfWidth: Float = 0.14f,
    val catchProgress: Float = 0.25f,   // 0f..1f (reach 1f to catch!)
    val tensionTimeLeft: Float = 10f,
    val lastResultMessage: String = ""
)

data class QuestDef(
    val id: String,
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

data class PlayerCustomization(
    val shirtColorHex: Long = 0xFF48A298,
    val hatColorHex: Long = 0xFFE2B65E,
    val ribbonColorHex: Long = 0xFFA84238
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
    val coins: Int = 120,
    val playerLevel: Int = 1,
    val exp: Int = 0,
    val nextLevelExp: Int = 100,
    val fishingLevel: Int = 1,
    val farmingLevel: Int = 1,
    val gatheringLevel: Int = 1,
    val houseTier: Int = 1,
    val timeHours: Float = 8.5f,
    val dayCount: Int = 1,
    val isRaining: Boolean = false,
    val isRunning: Boolean = false,
    val selectedToolId: String = "tool_rod_basic",
    val selectedSeedId: String = "seed_carrot",
    val contextAction: ContextualAction = ContextualAction(),
    val fishingState: FishingUiState = FishingUiState(),
    val buildPreview: BuildPreviewState = BuildPreviewState(),
    val inventory: List<InventoryStack> = emptyList(),
    val caughtFishCounts: Map<String, Int> = emptyMap(),
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
    val fishSpecies: List<FishSpecies> = listOf(
        FishSpecies("fish_river_minnow", "Silver Minnow", FishRarity.COMMON, 15, 0.24f, WaterHabitat.BOTH, "rpg/fish/fish_river_minnow.png", "Ikan kecil perak yang lincah di air jernih.", 0.85f),
        FishSpecies("fish_pond_carp", "Sunlit Carp", FishRarity.COMMON, 22, 0.22f, WaterHabitat.POND, "rpg/fish/fish_pond_carp.png", "Ikan mas oranye hangat penghuni kolam rumah.", 0.9f),
        FishSpecies("fish_moss_perch", "Moss Perch", FishRarity.COMMON, 25, 0.20f, WaterHabitat.BOTH, "rpg/fish/fish_moss_perch.png", "Suka bersembunyi di dekat daun teratai.", 0.95f),
        FishSpecies("fish_brook_trout", "Speckled Trout", FishRarity.UNCOMMON, 45, 0.14f, WaterHabitat.RIVER, "rpg/fish/fish_brook_trout.png", "Trout berbintik dari arus Sungai Willow.", 1.12f),
        FishSpecies("fish_amber_catfish", "Whisker Catfish", FishRarity.UNCOMMON, 55, 0.13f, WaterHabitat.POND, "rpg/fish/fish_amber_catfish.png", "Lele berkumis panjang yang tenang di dasar kolam.", 1.15f),
        FishSpecies("fish_jade_bream", "Jade Bream", FishRarity.UNCOMMON, 60, 0.12f, WaterHabitat.RIVER, "rpg/fish/fish_jade_bream.png", "Sisik hijau giok yang berkilau kena matahari.", 1.2f),
        FishSpecies("fish_crimson_koi", "Crimson Koi", FishRarity.RARE, 110, 0.08f, WaterHabitat.POND, "rpg/fish/fish_crimson_koi.png", "Koi merah-putih pembawa keberuntungan desa.", 1.38f),
        FishSpecies("fish_azure_pike", "Azure Pike", FishRarity.RARE, 130, 0.07f, WaterHabitat.RIVER, "rpg/fish/fish_azure_pike.png", "Predator sungai biru yang gesit saat ditarik.", 1.45f),
        FishSpecies("fish_moon_eel", "Moonlight Eel", FishRarity.EPIC, 220, 0.04f, WaterHabitat.RIVER, "rpg/fish/fish_moon_eel.png", "Belut ungu bercahaya yang aktif menjelang malam.", 1.65f, nightBonus = true),
        FishSpecies("fish_sakura_betta", "Sakura Veilfish", FishRarity.EPIC, 250, 0.035f, WaterHabitat.POND, "rpg/fish/fish_sakura_betta.png", "Sirip lembut merah muda menyerupai kelopak sakura.", 1.7f),
        FishSpecies("fish_golden_arowana", "Gilded Sovereign", FishRarity.LEGENDARY, 480, 0.015f, WaterHabitat.RIVER, "rpg/fish/fish_golden_arowana.png", "Ikan legenda bersisik emas murni dari hulu sungai.", 1.9f),
        FishSpecies("fish_starlight_sturgeon", "Starlight Sturgeon", FishRarity.LEGENDARY, 550, 0.012f, WaterHabitat.POND, "rpg/fish/fish_starlight_sturgeon.png", "Penguasa misterius kolam kuno di bawah cahaya bintang.", 2.0f, nightBonus = true)
    )

    val fishById: Map<String, FishSpecies> = fishSpecies.associateBy { it.id }

    val items: List<ItemDef> = listOf(
        // Tools
        ItemDef("tool_rod_basic", "Joran Kayu", ItemCategory.TOOLS, 1, 0, 0, "rpg/objects/tool_rod_basic.png", "Joran pemula untuk memancing di kolam dan sungai."),
        ItemDef("tool_rod_bamboo", "Joran Bambu", ItemCategory.TOOLS, 1, 80, 200, "rpg/objects/tool_rod_bamboo.png", "Memperlebar zona tangkap & peluang ikan langka +25%."),
        ItemDef("tool_rod_golden", "Joran Emas", ItemCategory.TOOLS, 1, 250, 650, "rpg/objects/tool_rod_golden.png", "Joran terbaik: gigitan lebih cepat & peluang Epic/Legendary +60%."),
        ItemDef("tool_axe", "Kapak Kayu", ItemCategory.TOOLS, 1, 0, 0, "rpg/objects/tool_axe.png", "Menebang pohon Ek, Pinus, dan Sakura."),
        ItemDef("tool_axe_iron", "Kapak Besi", ItemCategory.TOOLS, 1, 110, 260, "rpg/objects/tool_axe_iron.png", "Menebang pohon 2x lebih cepat dengan ekstra kayu."),
        ItemDef("tool_hoe", "Cangkul", ItemCategory.TOOLS, 1, 0, 0, "rpg/objects/tool_hoe.png", "Menggemburkan tanah kebun untuk menanam bibit."),
        ItemDef("tool_watering_can", "Gembor Air", ItemCategory.TOOLS, 1, 0, 0, "rpg/objects/tool_watering_can.png", "Menyiram tanaman agar tumbuh 2x lebih cepat."),
        // Resources
        ItemDef("res_wood", "Kayu", ItemCategory.WOOD, 99, 6, 15, "rpg/objects/res_wood.png", "Bahan utama membangun furnitur dan upgrade rumah."),
        ItemDef("res_hardwood", "Kayu Keras", ItemCategory.WOOD, 99, 18, 40, "rpg/objects/res_hardwood.png", "Kayu padat berkualitas tinggi dari pohon tua."),
        ItemDef("res_stone", "Batu", ItemCategory.STONE, 99, 8, 18, "rpg/objects/res_stone.png", "Batu kokoh untuk fondasi rumah dan lentera taman."),
        // Seeds
        ItemDef("seed_carrot", "Bibit Wortel", ItemCategory.SEEDS, 99, 4, 10, "rpg/objects/seed_carrot.png", "Tumbuh cepat (56 detik). Cocok untuk pemula."),
        ItemDef("seed_potato", "Bibit Kentang", ItemCategory.SEEDS, 99, 6, 15, "rpg/objects/seed_potato.png", "Hasil panen stabil dengan harga jual menarik."),
        ItemDef("seed_tomato", "Bibit Tomat", ItemCategory.SEEDS, 99, 10, 24, "rpg/objects/seed_tomato.png", "Tomat merah segar yang digemari warga desa."),
        ItemDef("seed_pumpkin", "Bibit Labu", ItemCategory.SEEDS, 99, 18, 42, "rpg/objects/seed_pumpkin.png", "Tanaman premium bernilai jual paling tinggi."),
        ItemDef("seed_oak", "Tunas Pohon Ek", ItemCategory.SEEDS, 50, 8, 20, "rpg/objects/seed_oak.png", "Tanam di rumput untuk menumbuhkan Pohon Ek baru."),
        ItemDef("seed_sakura", "Tunas Sakura", ItemCategory.SEEDS, 50, 14, 35, "rpg/objects/seed_sakura.png", "Tanam di rumput untuk menumbuhkan Pohon Sakura."),
        // Crops
        ItemDef("crop_carrot", "Wortel Segar", ItemCategory.CROPS, 99, 28, 0, "rpg/objects/crop_carrot.png", "Wortel renyah hasil kebun homestead."),
        ItemDef("crop_potato", "Kentang Emas", ItemCategory.CROPS, 99, 38, 0, "rpg/objects/crop_potato.png", "Kentang lembut hasil panen kebun."),
        ItemDef("crop_tomato", "Tomat Matang", ItemCategory.CROPS, 99, 52, 0, "rpg/objects/crop_tomato.png", "Tomat manis merah menyala."),
        ItemDef("crop_pumpkin", "Labu Besar", ItemCategory.CROPS, 99, 88, 0, "rpg/objects/crop_pumpkin.png", "Labu raksasa bernilai tinggi di pasar."),
        // Furniture (placeable + craftable in Construction Mode!)
        ItemDef("fence_wood", "Pagar Kayu", ItemCategory.FURNITURE, 50, 8, 20, "rpg/objects/fence_wood.png", "Pagar pembatas halaman yang hangat.", placeable = true, rotatable = true, woodCost = 2, stoneCost = 0),
        ItemDef("bench_wood", "Bangku Taman", ItemCategory.FURNITURE, 20, 18, 45, "rpg/objects/bench_wood.png", "Bangku kayu santai menghadap kolam.", placeable = true, rotatable = true, woodCost = 5, stoneCost = 0),
        ItemDef("wooden_table", "Meja Piknik", ItemCategory.FURNITURE, 20, 25, 60, "rpg/objects/wooden_table.png", "Meja kayu hangat untuk bersantai.", placeable = true, rotatable = true, woodCost = 6, stoneCost = 0),
        ItemDef("storage_chest", "Peti Penyimpanan", ItemCategory.FURNITURE, 10, 30, 75, "rpg/objects/storage_chest.png", "Peti ekstra untuk menyimpan hasil alam.", placeable = true, rotatable = false, woodCost = 8, stoneCost = 2),
        // Decorations (placeable + craftable!)
        ItemDef("lamp_post", "Lampu Taman", ItemCategory.DECORATIONS, 30, 22, 55, "rpg/objects/lamp_post.png", "Menyala hangat otomatis saat senja & malam.", placeable = true, rotatable = false, woodCost = 3, stoneCost = 2),
        ItemDef("flower_pot", "Pot Bunga", ItemCategory.DECORATIONS, 30, 12, 30, "rpg/objects/flower_pot.png", "Pot bunga warna-warni pemanis pekarangan.", placeable = true, rotatable = false, woodCost = 2, stoneCost = 1),
        ItemDef("sign_post", "Papan Penunjuk", ItemCategory.DECORATIONS, 20, 10, 25, "rpg/objects/sign_post.png", "Papan kayu penanda sudut homestead.", placeable = true, rotatable = true, woodCost = 3, stoneCost = 0),
        ItemDef("campfire", "Api Unggun", ItemCategory.DECORATIONS, 15, 28, 70, "rpg/objects/campfire.png", "Api unggun hangat di tepi kolam.", placeable = true, rotatable = false, woodCost = 4, stoneCost = 4),
        ItemDef("lantern_stone", "Lentera Batu", ItemCategory.DECORATIONS, 20, 35, 90, "rpg/objects/lantern_stone.png", "Lentera taman klasik bercahaya lembut.", placeable = true, rotatable = false, woodCost = 1, stoneCost = 6),
        ItemDef("scarecrow", "Orang-Orangan Sawah", ItemCategory.DECORATIONS, 10, 30, 80, "rpg/objects/scarecrow.png", "Penjaga ramah untuk kebun sayurmu.", placeable = true, rotatable = true, woodCost = 5, stoneCost = 1),
        ItemDef("fish_trophy", "Plakat Trofi Ikan", ItemCategory.DECORATIONS, 10, 60, 150, "rpg/objects/fish_trophy.png", "Penghargaan pemancing sejati Willowbrook.", placeable = true, rotatable = false, woodCost = 6, stoneCost = 2),
        ItemDef("well_stone", "Sumur Batu", ItemCategory.DECORATIONS, 5, 80, 200, "rpg/objects/well_stone.png", "Sumur air jernih pengisi halaman.", placeable = true, rotatable = false, woodCost = 4, stoneCost = 10)
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

    val houseUpgrades: List<HouseUpgradeSpec> = listOf(
        HouseUpgradeSpec(1, "Kabin Kayu Pemula", 0, 0, 0, "Rumah kayu hangat untuk memulai hidup tenang di Willowbrook."),
        HouseUpgradeSpec(2, "Pondok Cerobong Asap", 220, 18, 6, "Menambah teras kayu, cerobong asap, dan kotak bunga jendela."),
        HouseUpgradeSpec(3, "Vila Tepi Danau", 550, 36, 16, "Atap biru slate, jendela loteng, dan lentera teras bercahaya.")
    )

    val dailyQuests: List<QuestDef> = listOf(
        QuestDef("q_catch_3_fish", "Pemancing Pagi", "Tangkap 3 ekor ikan di kolam atau sungai", 3, 75, 45),
        QuestDef("q_collect_10_wood", "Pengumpul Kayu", "Kumpulkan 10 kayu dari menebang pohon", 10, 65, 40),
        QuestDef("q_plant_5_crops", "Tangan Hijau", "Tanam 5 bibit sayuran di lahan kebun", 5, 60, 35),
        QuestDef("q_harvest_3_crops", "Panen Segar", "Panen 3 sayuran matang dari kebun", 3, 95, 55),
        QuestDef("q_earn_150_coins", "Pedagang Desa", "Dapatkan 150 koin dari menjual ikan atau hasil panen", 150, 100, 60),
        QuestDef("q_build_2_items", "Arsitek Homestead", "Pasang 2 furnitur atau dekorasi di halaman", 2, 110, 65)
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("a_first_fish", "Tangkapan Pertama", "Tangkap ikan pertamamu di Willowbrook", 1, 50, 30),
        AchievementDef("a_fish_15", "Pemancing Andal", "Tangkap total 15 ikan", 15, 180, 120),
        AchievementDef("a_rare_fish", "Pemburu Langka", "Tangkap ikan Rare, Epic, atau Legendary", 1, 200, 140),
        AchievementDef("a_chop_10", "Penebang Terampil", "Tebang 10 pohon di sekitar homestead", 10, 140, 90),
        AchievementDef("a_harvest_10", "Petani Makmur", "Panen 10 tanaman dari kebun", 10, 160, 110),
        AchievementDef("a_house_tier2", "Rumah Impian", "Upgrade rumah ke Pondok Cerobong Asap (Tier 2)", 1, 250, 160),
        AchievementDef("a_house_tier3", "Tuan Tanah Willowbrook", "Upgrade rumah ke Vila Tepi Danau (Tier 3)", 1, 500, 300),
        AchievementDef("a_collector_6", "Ensiklopedia Ikan", "Koleksi 6 spesies ikan berbeda", 6, 300, 200)
    )

    fun expRequiredForLevel(level: Int): Int = 100 + (level - 1) * 65
}
