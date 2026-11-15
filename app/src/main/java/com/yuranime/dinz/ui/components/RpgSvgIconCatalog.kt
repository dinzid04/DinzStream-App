package com.yuranime.dinz.ui.components

import androidx.annotation.DrawableRes
import com.yuranime.dinz.R
import java.util.Locale

/**
 * Bentuk bingkai lower-third untuk Custom Title Badge:
 * - [ROYAL_HEX]: Pelat heksagon berlapis klasik (Starter / Prestige)
 * - [CIRCUIT_TECH]: Pelat tech circuit dengan garis node `○───` dan rivet (Referensi DnaoiKY)
 * - [FACETED_SHARD]: Bilah obsidian bersudut poligon faset + sub-shard (Referensi vheY3Ze)
 * - [DIAGONAL_SLASH]: Lower-third miring kontras tinggi + strip `//` (Referensi G9NqcmK)
 * - [MECHA_HUD]: Bingkai armor mecha asimetris + ventilasi `||||` (Referensi UGKkbof)
 */
enum class TitleShapeStyle {
    ROYAL_HEX,
    CIRCUIT_TECH,
    FACETED_SHARD,
    DIAGONAL_SLASH,
    MECHA_HUD;

    companion object {
        fun fromVariant(variant: Int): TitleShapeStyle {
            val all = entries
            return all[(variant.coerceAtLeast(0)) % all.size]
        }
    }
}

/**
 * Tingkat efek visual (VFX) untuk membedakan title pemula/gratis vs title mewah/berbayar.
 */
enum class TitleVfxTier {
    NONE,           // Polos rapi (starter gratis)
    SUBTLE_SHIMMER, // Kilau halus tunggal (rank awal / shop murah)
    PULSE_SCAN,     // Radar cyber scanline + pulse glow
    EMBER_SPARK,    // Partikel bara api hangat + tepi menyala
    FROST_PRISM,    // Kristal es dingin + refraksi diagonal
    HOLO_PRISM,     // Kilau prismatik holografik multi-spektrum
    VOID_NEBULA,    // Gelombang nebula gelap + kilatan plasma crimson/ungu
    LIGHTNING_STORM,// Sambaran petir bercabang aktif melintasi bingkai
    BLAZING_INFERNO,// Kobaran lidah api aktif + percikan bara membubung
    SOLAR_FLARE     // Korona cahaya emas & debu bintang orbital
}

/**
 * Definisi Role komunitas & staf beserta ikon SVG unik dari `RPG ICON TITLE` / `RPG ICON V2`.
 */
data class RoleBadgeSpec(
    val key: String,
    val label: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val accentHex: Long,
    val isExclusiveStaff: Boolean = false
)

/**
 * Definisi Milestone Level beserta ikon level SVG unik, Rank Mini-Title, dan Custom Title hadiah.
 */
data class LevelMilestoneSpec(
    val level: Int,
    val tierLabel: String,
    val rankTitle: String,
    @DrawableRes val levelIconRes: Int,
    val rewardCustomTitle: String? = null,
    val accentHex: Long
)

/**
 * Katalog terpusat pemetaan ikon vektor SVG asli dari folder `RPG ICON TITLE` & `RPG ICON V2`
 * menjadi Resource Drawable Android (`res/drawable/ic_rpg_*.xml`).
 *
 * Seluruh pemetaan dibuat 1-ke-1 agar tidak terjadi ikon ganda antara Custom Title,
 * Rank Mini-Title, Role Komunitas, dan Ikon Level Roadmap.
 */
object RpgSvgIconCatalog {

    @DrawableRes
    val DEFAULT_LEVEL_ICON: Int = R.drawable.ic_rpg_v17_attack_up_01

    /**
     * Peta ikon unik per ID Custom Title (masing-masing memakai SVG tersendiri).
     */
    private val titleIdIconMap: Map<String, Int> = mapOf(
        // A. Milestone Level Titles (RPG ICON TITLE)
        "title_lv1_starter" to R.drawable.ic_rpg_v20_cottage_01,
        "title_lv5_pioneer" to R.drawable.ic_rpg_v1_quest_scroll_12,
        "title_lv10_explorer" to R.drawable.ic_rpg_v7_spell_book_173,
        "title_lv15_vanguard" to R.drawable.ic_rpg_v13_warrior_emblem_crossed_swords_and_helmet_01,
        "title_lv25_dimension" to R.drawable.ic_rpg_v6_nebula_planet_300,
        "title_lv35_knight" to R.drawable.ic_rpg_v1_knight_helmet_04,
        "title_lv50_sepuh" to R.drawable.ic_rpg_v12_dragon_head_roaring_front_view_257,
        "title_lv65_grandmaster" to R.drawable.ic_rpg_v7_eagle_scepter_199,
        "title_lv80_emperor" to R.drawable.ic_rpg_v10_trophy_132,
        "title_lv100_akashic" to R.drawable.ic_rpg_v5_phoenix_179,

        // B. Points Shop — Entry / Casual (RPG ICON TITLE)
        "shop_vault_hunter" to R.drawable.ic_rpg_v18_inventory_bag_225,
        "shop_ramen_chef" to R.drawable.ic_rpg_v11_cooking_pot_over_campfire_33,
        "shop_alchemist_heart" to R.drawable.ic_rpg_v19_round_flask_with_heart_symbol_01,
        "shop_iron_brawler" to R.drawable.ic_rpg_v2_fist_punch_50,
        "shop_forge_master" to R.drawable.ic_rpg_v11_blacksmith_anvil_with_hammer_01,
        "shop_cozy_pumpkin" to R.drawable.ic_rpg_v19_carved_pumpkin_225,
        "shop_peace_keeper" to R.drawable.ic_rpg_v5_peace_dove_168,
        "shop_love_potion" to R.drawable.ic_rpg_v5_heart_potion_01,

        // C. Points Shop — Rare / Epic (RPG ICON TITLE + RPG ICON V2)
        "shop_crimson_knight" to R.drawable.ic_rpg_v2_sword_clash_56,
        "shop_marathon_king" to R.drawable.ic_rpg_v7_hourglass_207,
        "shop_amethyst_lord" to R.drawable.ic_rpg_v14_castle_01,
        "shop_druid_sanctuary" to R.drawable.ic_rpg_v8_root_staff_99,
        "shop_glacial_monarch" to R.drawable.ic_rpg_v15_frost_bolt_66,
        "shop_cyber_netrunner" to R.drawable.ic_rpg_v18_settings_gear_257,
        "shop_shadow_shinobi" to R.drawable.ic_rpg_v2_ninja_star,
        "shop_ronin_katana" to R.drawable.ic_rpg_v2_katana,
        "shop_berserker_cleaver" to R.drawable.ic_rpg_v8_cleaver_01,
        "shop_bombardier" to R.drawable.ic_rpg_v4_exploding_bomb_40,
        "shop_goblin_slayer" to R.drawable.ic_rpg_v12_goblin_head_01,
        "shop_beast_tamer" to R.drawable.ic_rpg_v15_shapeshift_bear_97,
        "shop_fuse_demolition" to R.drawable.ic_rpg_v19_round_fuse_bomb_97,
        "shop_skull_warlord" to R.drawable.ic_rpg_v12_skeleton_warrior_skull_with_helmet_33,
        "shop_curse_collar" to R.drawable.ic_rpg_v9_skull_collar_258,
        "shop_treasure_warden" to R.drawable.ic_rpg_v10_latch_chest_262,
        "shop_rider_nomad" to R.drawable.ic_rpg_v16_saddle_49,
        "shop_dizzy_trickster" to R.drawable.ic_rpg_v17_stunned_49,

        // D. Points Shop — Luxury / Mythic (BLAZING INFERNO / LIGHTNING / SOLAR FX)
        "shop_inferno_emperor" to R.drawable.ic_rpg_v17_fire_aura_145,
        "shop_raijin_thunder" to R.drawable.ic_rpg_v2_chain_lightning,
        "shop_crimson_shogun" to R.drawable.ic_rpg_v2_samurai_helmet,
        "shop_astral_sorcerer" to R.drawable.ic_rpg_v3_spirit_vortex_55,
        "shop_abyssal_reaper" to R.drawable.ic_rpg_v2_reaper_scythe,
        "shop_storm_breaker" to R.drawable.ic_rpg_v2_thunder_blade,
        "shop_meteor_destroyer" to R.drawable.ic_rpg_v2_burning_meteor,
        "shop_celestial_archon" to R.drawable.ic_rpg_v2_spiked_halo,
        "shop_obsidian_eye" to R.drawable.ic_rpg_v3_evil_eye_12,
        "shop_solar_valkyrie" to R.drawable.ic_rpg_v16_griffin_mount_17,
        "shop_mythic_sovereign" to R.drawable.ic_rpg_v9_fire_gem_131,

        // E. Exclusive Redeem-Only Titles
        "redeem_vip_yuranime" to R.drawable.ic_rpg_v1_open_treasure_chest_69,
        "redeem_khalifah_anime" to R.drawable.ic_rpg_v10_mystic_cross_128,
        "redeem_tempest_overlord" to R.drawable.ic_rpg_v2_focused_lightning,
        "redeem_eternal_phoenix" to R.drawable.ic_rpg_v3_burning_scroll_56,
        "redeem_lunar_predator" to R.drawable.ic_rpg_v2_moon_claws,
        "redeem_dragon_emperor" to R.drawable.ic_rpg_v2_dragon_shield,
        "redeem_starfall_god" to R.drawable.ic_rpg_v6_shooting_stars_314
    )

    private val titleNameIconMap: Map<String, Int> = mapOf(
        "WARGA YURANIME" to R.drawable.ic_rpg_v20_cottage_01,
        "PERINTIS ANIME" to R.drawable.ic_rpg_v1_quest_scroll_12,
        "PENJELAJAH KOMIK" to R.drawable.ic_rpg_v7_spell_book_173,
        "VANGUARD OTAKU" to R.drawable.ic_rpg_v13_warrior_emblem_crossed_swords_and_helmet_01,
        "PENGUASA DIMENSI" to R.drawable.ic_rpg_v6_nebula_planet_300,
        "KSATRIA ISEKAI" to R.drawable.ic_rpg_v1_knight_helmet_04,
        "SEPUH YURANIME" to R.drawable.ic_rpg_v12_dragon_head_roaring_front_view_257,
        "GRANDMASTER LORE" to R.drawable.ic_rpg_v7_eagle_scepter_199,
        "EMPEROR OF OTAKU" to R.drawable.ic_rpg_v10_trophy_132,
        "AKASHIC SOVEREIGN" to R.drawable.ic_rpg_v5_phoenix_179,

        "PEMBURU KOLEKSI" to R.drawable.ic_rpg_v18_inventory_bag_225,
        "PENJAGA RAMEN" to R.drawable.ic_rpg_v11_cooking_pot_over_campfire_33,
        "ALCHEMIST HEART" to R.drawable.ic_rpg_v19_round_flask_with_heart_symbol_01,
        "IRON BRAWLER" to R.drawable.ic_rpg_v2_fist_punch_50,
        "FORGE MASTER" to R.drawable.ic_rpg_v11_blacksmith_anvil_with_hammer_01,
        "PUMPKIN PHANTOM" to R.drawable.ic_rpg_v19_carved_pumpkin_225,
        "PEACEKEEPER" to R.drawable.ic_rpg_v5_peace_dove_168,
        "CRIMSON ELIXIR" to R.drawable.ic_rpg_v5_heart_potion_01,

        "KSATRIA CRIMSON" to R.drawable.ic_rpg_v2_sword_clash_56,
        "RAJA MARATON" to R.drawable.ic_rpg_v7_hourglass_207,
        "BANGSAWAN KOMIK" to R.drawable.ic_rpg_v14_castle_01,
        "DRUID SANCTUARY" to R.drawable.ic_rpg_v8_root_staff_99,
        "GLACIAL MONARCH" to R.drawable.ic_rpg_v15_frost_bolt_66,
        "CYBER NETRUNNER" to R.drawable.ic_rpg_v18_settings_gear_257,
        "SHADOW SHINOBI" to R.drawable.ic_rpg_v2_ninja_star,
        "RONIN KATANA" to R.drawable.ic_rpg_v2_katana,
        "BERSERKER CLEAVER" to R.drawable.ic_rpg_v8_cleaver_01,
        "ARCANE BOMBARDIER" to R.drawable.ic_rpg_v4_exploding_bomb_40,
        "GOBLIN SLAYER" to R.drawable.ic_rpg_v12_goblin_head_01,
        "BEAST TAMER" to R.drawable.ic_rpg_v15_shapeshift_bear_97,
        "PYRO TECHNICIAN" to R.drawable.ic_rpg_v19_round_fuse_bomb_97,
        "SKULL WARLORD" to R.drawable.ic_rpg_v12_skeleton_warrior_skull_with_helmet_33,
        "CURSE BEARER" to R.drawable.ic_rpg_v9_skull_collar_258,
        "VAULT WARDEN" to R.drawable.ic_rpg_v10_latch_chest_262,
        "NOMAD RIDER" to R.drawable.ic_rpg_v16_saddle_49,
        "PHANTOM JESTER" to R.drawable.ic_rpg_v17_stunned_49,

        "INFERNO EMPEROR" to R.drawable.ic_rpg_v17_fire_aura_145,
        "RAIJIN THUNDER" to R.drawable.ic_rpg_v2_chain_lightning,
        "CRIMSON SHOGUN" to R.drawable.ic_rpg_v2_samurai_helmet,
        "ASTRAL SORCERER" to R.drawable.ic_rpg_v3_spirit_vortex_55,
        "ABYSSAL REAPER" to R.drawable.ic_rpg_v2_reaper_scythe,
        "STORM BREAKER" to R.drawable.ic_rpg_v2_thunder_blade,
        "METEOR DESTROYER" to R.drawable.ic_rpg_v2_burning_meteor,
        "CELESTIAL ARCHON" to R.drawable.ic_rpg_v2_spiked_halo,
        "OBSIDIAN EYE" to R.drawable.ic_rpg_v3_evil_eye_12,
        "SOLAR VALKYRIE" to R.drawable.ic_rpg_v16_griffin_mount_17,
        "MYTHIC SOVEREIGN" to R.drawable.ic_rpg_v9_fire_gem_131,

        "VIP YURANIME" to R.drawable.ic_rpg_v1_open_treasure_chest_69,
        "KHALIFAH ANIME" to R.drawable.ic_rpg_v10_mystic_cross_128,
        "TEMPEST OVERLORD" to R.drawable.ic_rpg_v2_focused_lightning,
        "ETERNAL PHOENIX" to R.drawable.ic_rpg_v3_burning_scroll_56,
        "LUNAR PREDATOR" to R.drawable.ic_rpg_v2_moon_claws,
        "DRAGON EMPEROR" to R.drawable.ic_rpg_v2_dragon_shield,
        "STARFALL GOD" to R.drawable.ic_rpg_v6_shooting_stars_314
    )

    private val titleIconPool: List<Int> = titleNameIconMap.values.toList()

    /**
     * 10 Ikon RPG SVG unik (dari `RPG ICON TITLE` + `RPG ICON V2`) untuk Rank Mini-Title.
     * Tidak dipakai ulang oleh Custom Title manapun.
     */
    private val rankMiniTitleIconMap: Map<String, Int> = mapOf(
        "WIBU BIASA" to R.drawable.ic_rpg_v20_reading_chair_145,
        "PERINTIS ANIME" to R.drawable.ic_rpg_v14_world_map_scroll_113,
        "PENJELAJAH KOMIK" to R.drawable.ic_rpg_v6_owl_309,
        "VANGUARD OTAKU" to R.drawable.ic_rpg_v15_shield_bash_01,
        "SEPUH YURANIME" to R.drawable.ic_rpg_v13_mage_head_beard_hat_34,
        "ARCHMAGE ISEKAI" to R.drawable.ic_rpg_v4_magic_arrow_83,
        "OVERLORD YURANIME" to R.drawable.ic_rpg_v9_skeletal_armor_335,
        "MONARCH DIMENSI" to R.drawable.ic_rpg_v14_underground_city_97,
        "GRANDMASTER AKASHIC" to R.drawable.ic_rpg_v2_hammer_smash_48,
        "CELESTIAL SOVEREIGN" to R.drawable.ic_rpg_v2_flame_spin
    )

    /**
     * Daftar Role Resmi Komunitas dengan sisa ikon SVG unik dari `RPG ICON V2`.
     */
    val ROLE_SPECS: List<RoleBadgeSpec> = listOf(
        RoleBadgeSpec(
            key = "owner",
            label = "Owner",
            description = "Pendiri & pemegang otoritas utama ekosistem Yuranime.",
            iconRes = R.drawable.ic_rpg_v2_throne_king,
            accentHex = 0xFFF59E0B,
            isExclusiveStaff = true
        ),
        RoleBadgeSpec(
            key = "admin",
            label = "Admin",
            description = "Pengelola server, event, dan moderasi komunitas Yuranime.",
            iconRes = R.drawable.ic_rpg_v2_police_badge,
            accentHex = 0xFFE11D48,
            isExclusiveStaff = true
        ),
        RoleBadgeSpec(
            key = "moderator",
            label = "Moderator",
            description = "Penjaga ketertiban Global Chat & kenyamanan anggota.",
            iconRes = R.drawable.ic_rpg_v2_winged_shield,
            accentHex = 0xFF38BDF8,
            isExclusiveStaff = true
        ),
        RoleBadgeSpec(
            key = "perintis",
            label = "Perintis Yuranime",
            description = "Generasi pelopor awal yang membangun Yuranime sejak rilis perdana.",
            iconRes = R.drawable.ic_rpg_v2_laurel_crown,
            accentHex = 0xFF34D399,
            isExclusiveStaff = true
        ),
        RoleBadgeSpec(
            key = "vip",
            label = "Partner & VIP",
            description = "Kontributor spesial & pendukung eksklusif komunitas Yuranime.",
            iconRes = R.drawable.ic_rpg_v2_diamond_trophy,
            accentHex = 0xFFA78BFA,
            isExclusiveStaff = true
        ),
        RoleBadgeSpec(
            key = "member",
            label = "Member",
            description = "Anggota resmi keluarga besar Yuranime.",
            iconRes = R.drawable.ic_rpg_v2_allied_star,
            accentHex = 0xFF94A3B8,
            isExclusiveStaff = false
        )
    )

    /**
     * Peta Progresi Level & Hadiah Ikon Level (Lv.1 .. Lv.100) untuk dialog Roadmap Profil.
     * Setiap milestone menggunakan ikon SVG level unik tersendiri.
     */
    val LEVEL_ROADMAP: List<LevelMilestoneSpec> = listOf(
        LevelMilestoneSpec(1, "Novice", "Wibu Biasa", R.drawable.ic_rpg_v13_human_head_49, "WARGA YURANIME", 0xFF94A3B8),
        LevelMilestoneSpec(5, "Initiate", "Perintis Anime", R.drawable.ic_rpg_v11_pickaxe_129, "PERINTIS ANIME", 0xFF38BDF8),
        LevelMilestoneSpec(10, "Explorer", "Penjelajah Komik", R.drawable.ic_rpg_v2_compass, "PENJELAJAH KOMIK", 0xFF22D3EE),
        LevelMilestoneSpec(15, "Vanguard", "Penjelajah Komik", R.drawable.ic_rpg_v8_root_shield_93, "VANGUARD OTAKU", 0xFF34D399),
        LevelMilestoneSpec(20, "Challenger", "Vanguard Otaku", R.drawable.ic_rpg_v4_crossed_bows_arrows_63, null, 0xFF818CF8),
        LevelMilestoneSpec(25, "Rift Walker", "Vanguard Otaku", R.drawable.ic_rpg_v16_war_horse_01, "PENGUASA DIMENSI", 0xFFA855F7),
        LevelMilestoneSpec(30, "Elder Sage", "Sepuh Yuranime", R.drawable.ic_rpg_v20_iron_chandelier_193, null, 0xFFF43F5E),
        LevelMilestoneSpec(35, "Champion", "Sepuh Yuranime", R.drawable.ic_rpg_v18_strength_fist_81, "KSATRIA ISEKAI", 0xFFFB7185),
        LevelMilestoneSpec(45, "Archmage", "Archmage Isekai", R.drawable.ic_rpg_v2_rank_1, null, 0xFFC084FC),
        LevelMilestoneSpec(50, "Living Legend", "Archmage Isekai", R.drawable.ic_rpg_v2_laurels_trophy, "SEPUH YURANIME", 0xFFF59E0B),
        LevelMilestoneSpec(60, "Overlord", "Overlord Yuranime", R.drawable.ic_rpg_v2_rank_2, null, 0xFFFB923C),
        LevelMilestoneSpec(65, "High Magister", "Overlord Yuranime", R.drawable.ic_rpg_v2_medal, "GRANDMASTER LORE", 0xFFFBBF24),
        LevelMilestoneSpec(75, "Monarch", "Monarch Dimensi", R.drawable.ic_rpg_v2_king, null, 0xFFE11D48),
        LevelMilestoneSpec(80, "Otaku Emperor", "Monarch Dimensi", R.drawable.ic_rpg_v2_sport_medal, "EMPEROR OF OTAKU", 0xFF2DD4BF),
        LevelMilestoneSpec(85, "Grandmaster", "Grandmaster Akashic", R.drawable.ic_rpg_v2_rank_3, null, 0xFF38BDF8),
        LevelMilestoneSpec(90, "Ascendant", "Grandmaster Akashic", R.drawable.ic_rpg_v2_medallist, null, 0xFFFDE047),
        LevelMilestoneSpec(100, "Cosmic Deity", "Celestial Sovereign", R.drawable.ic_rpg_v2_crown, "AKASHIC SOVEREIGN", 0xFFFF5722)
    )

    @DrawableRes
    fun resolveCustomTitleIcon(titleName: String, titleId: String = ""): Int {
        val byId = titleIdIconMap[titleId]
        if (byId != null) return byId

        val upper = titleName.trim().uppercase(Locale.ROOT)
        titleNameIconMap[upper]?.let { return it }

        return when {
            upper.contains("THUNDER") || upper.contains("RAIJIN") || upper.contains("PETIR") -> R.drawable.ic_rpg_v2_chain_lightning
            upper.contains("STORM") || upper.contains("LIGHTNING") -> R.drawable.ic_rpg_v2_thunder_blade
            upper.contains("TEMPEST") -> R.drawable.ic_rpg_v2_focused_lightning
            upper.contains("SAMURAI") || upper.contains("SHOGUN") -> R.drawable.ic_rpg_v2_samurai_helmet
            upper.contains("KATANA") || upper.contains("RONIN") -> R.drawable.ic_rpg_v2_katana
            upper.contains("SHINOBI") || upper.contains("NINJA") -> R.drawable.ic_rpg_v2_ninja_star
            upper.contains("REAPER") || upper.contains("MAUT") || upper.contains("ABYSS") -> R.drawable.ic_rpg_v2_reaper_scythe
            upper.contains("ARCHON") || upper.contains("HALO") -> R.drawable.ic_rpg_v2_spiked_halo
            upper.contains("METEOR") -> R.drawable.ic_rpg_v2_burning_meteor
            upper.contains("INFERNO") || upper.contains("API") || upper.contains("FLAME") -> R.drawable.ic_rpg_v17_fire_aura_145
            upper.contains("PHOENIX") -> R.drawable.ic_rpg_v5_phoenix_179
            upper.contains("DRAGON") || upper.contains("NAGA") || upper.contains("SEPUH") -> R.drawable.ic_rpg_v12_dragon_head_roaring_front_view_257
            upper.contains("SOVEREIGN") || upper.contains("MYTHIC") -> R.drawable.ic_rpg_v9_fire_gem_131
            upper.contains("FROST") || upper.contains("GLACIAL") || upper.contains("ES") -> R.drawable.ic_rpg_v15_frost_bolt_66
            upper.contains("CYBER") || upper.contains("MECHA") || upper.contains("NETRUNNER") -> R.drawable.ic_rpg_v18_settings_gear_257
            upper.contains("VIP") -> R.drawable.ic_rpg_v1_open_treasure_chest_69
            upper.contains("KHALIFAH") -> R.drawable.ic_rpg_v10_mystic_cross_128
            upper.contains("LUNAR") || upper.contains("MOON") || upper.contains("BULAN") -> R.drawable.ic_rpg_v2_moon_claws
            upper.contains("STAR") -> R.drawable.ic_rpg_v6_shooting_stars_314
            upper.contains("DIMENSI") || upper.contains("COSMIC") || upper.contains("NEBULA") -> R.drawable.ic_rpg_v6_nebula_planet_300
            upper.contains("CRIMSON") || upper.contains("KSATRIA") -> R.drawable.ic_rpg_v2_sword_clash_56
            else -> {
                val hash = (upper.hashCode() and 0x7FFFFFFF)
                titleIconPool[hash % titleIconPool.size]
            }
        }
    }

    @DrawableRes
    fun resolveMiniTitleIcon(rankTitle: String, level: Int = 1): Int {
        val upper = rankTitle.trim().uppercase(Locale.ROOT)
        rankMiniTitleIconMap[upper]?.let { return it }
        return when {
            level >= 100 -> R.drawable.ic_rpg_v2_flame_spin
            level >= 85 -> R.drawable.ic_rpg_v2_hammer_smash_48
            level >= 75 -> R.drawable.ic_rpg_v14_underground_city_97
            level >= 60 -> R.drawable.ic_rpg_v9_skeletal_armor_335
            level >= 45 -> R.drawable.ic_rpg_v4_magic_arrow_83
            level >= 30 -> R.drawable.ic_rpg_v13_mage_head_beard_hat_34
            level >= 20 -> R.drawable.ic_rpg_v15_shield_bash_01
            level >= 10 -> R.drawable.ic_rpg_v6_owl_309
            level >= 5 -> R.drawable.ic_rpg_v14_world_map_scroll_113
            else -> R.drawable.ic_rpg_v20_reading_chair_145
        }
    }

    @DrawableRes
    fun resolveRoleIcon(role: String): Int {
        return resolveRoleSpec(role).iconRes
    }

    fun resolveRoleSpec(role: String): RoleBadgeSpec {
        val lower = role.trim().lowercase(Locale.ROOT)
        return when {
            lower.contains("owner") || lower.contains("founder") -> ROLE_SPECS[0]
            lower.contains("admin") -> ROLE_SPECS[1]
            lower.contains("mod") -> ROLE_SPECS[2]
            lower.contains("perintis") || lower.contains("pioneer") -> ROLE_SPECS[3]
            lower.contains("vip") || lower.contains("partner") -> ROLE_SPECS[4]
            else -> ROLE_SPECS[5]
        }
    }

    @DrawableRes
    fun resolveLevelIcon(level: Int): Int {
        return LEVEL_ROADMAP.lastOrNull { level >= it.level }?.levelIconRes
            ?: DEFAULT_LEVEL_ICON
    }
}
