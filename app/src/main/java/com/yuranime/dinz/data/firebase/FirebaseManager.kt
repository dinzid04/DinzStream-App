package com.yuranime.dinz.data.firebase

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.local.RemoteWatchHistoryBundle
import com.yuranime.dinz.data.local.SavedGoogleUserSession
import com.yuranime.dinz.data.local.WatchHistoryItem
import com.yuranime.dinz.data.local.WatchedEpisodeRecord
import com.yuranime.dinz.ui.components.TitleVfxTier
import com.yuranime.dinz.ui.components.VERIFIED_BADGE_DEFAULT_HEX
import com.yuranime.dinz.util.GlobalCrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class TitleUnlockType {
    MILESTONE,
    POINTS_SHOP,
    REDEEM_ONLY
}

data class TitleDefinition(
    val id: String,
    val titleName: String,
    val tierLevel: Int,
    val styleVariant: Int,
    val unlockType: TitleUnlockType,
    val requiredLevel: Int = 1,
    val pointCost: Long = 0L,
    val description: String = "",
    val vfxTier: TitleVfxTier = TitleVfxTier.SUBTLE_SHIMMER
)

data class BorderDefinition(
    val id: String,
    val name: String,
    val imageUrl: String,
    val pointCost: Long,
    val description: String,
    val staticImageUrl: String = "",
    val tier: String = "A", // "A" (Starter), "B" (Exclusive Lv.35+), "C" (Redeem Only)
    val requiredLevel: Int = 1,
    val category: String = "General"
)

data class ProfileEffectLayer(
    val src: String,
    val loop: Boolean = true,
    val start: Long = 0L,
    val duration: Long = 3000L,
    val loopDelay: Long = 0L,
    val positionY: Int = 0,
    val zIndex: Int = 0
)

data class ProfileEffectDefinition(
    val id: String,
    val name: String,
    val animationUrl: String,
    val pointCost: Long,
    val description: String,
    val tier: String = "A", // "A", "B", "C"
    val requiredLevel: Int = 1,
    val category: String = "General",
    val staticImageUrl: String = "",
    val layerUrls: List<String> = emptyList(),
    val layers: List<ProfileEffectLayer> = emptyList()
)

enum class NameStyleTier(val label: String, val pointCost: Long) {
    MYTHIC("Mythic", 4500L),
    LEGENDARY("Legendary", 2000L)
}

data class NameStyleDefinition(
    val id: String,
    val name: String,
    val tier: NameStyleTier,
    val pointCost: Long = tier.pointCost,
    val defaultColorHex: String = "#FFFFFF",
    val description: String = ""
)

data class NameStyleColorSwatch(
    val id: String,
    val label: String,
    val hex: String,
    val colorValue: Long
)

data class UserProfile(
    val uid: String = "",
    val name: String = "Member Yuranime",
    val username: String = "member",
    val userTag: String = "1001",
    val email: String = "",
    val avatar: String = "",
    val banner: String = "",
    val bio: String = "Penikmat anime & komik di Yuranime.",
    val role: String = "Member",
    val joinDate: String = "",
    val exp: Long = 0L,
    val points: Long = 0L,
    val level: Int = 1,
    val rankTitle: String = "Wibu Biasa",
    val equippedTitle: String = "",
    val equippedTitleTier: Int = 1,
    val equippedTitleVariant: Int = 0,
    val equippedNameStyle: String = "",
    val equippedNameColor: String = "",
    val equippedBorderUrl: String = "",
    val equippedProfileEffectUrl: String = "",
    val cosmeticAnimate: Boolean = true,
    val showMiniTitle: Boolean = true,
    val showCustomTitle: Boolean = true,
    val showRole: Boolean = true,
    val unlockedTitles: List<String> = emptyList(),
    val unlockedNameStyles: List<String> = emptyList(),
    val unlockedBorders: List<String> = emptyList(),
    val unlockedEffects: List<String> = emptyList(),
    val claimedRedeemCodes: List<String> = emptyList(),
    val claimedMissions: Map<String, String> = emptyMap(),
    val lastCheckInDate: String = "",
    val checkInStreak: Int = 0,
    val nextLevelTargetExp: Long = 500L,
    val expProgress: Float = 0f,
    val isVerified: Boolean = false,
    val verifiedColor: String = VERIFIED_BADGE_DEFAULT_HEX,
    val rpgLevel: Int = 0,
    val rpgCoins: Int = 0,
    val rpgHouseTier: Int = 1
) {
    val expToNextLevel: Long
        get() = (nextLevelTargetExp - exp).coerceAtLeast(0L)
}

data class LeaderboardUser(
    val rank: Int = 1,
    val uid: String = "",
    val name: String = "Pengguna",
    val username: String = "@user",
    val userTag: String = "1001",
    val avatar: String = "",
    val exp: Long = 0L,
    val points: Long = 0L,
    val level: Int = 1,
    val role: String = "Member",
    val rankTitle: String = "Wibu Biasa",
    val equippedTitle: String = "",
    val equippedTitleTier: Int = 1,
    val equippedTitleVariant: Int = 0,
    val equippedNameStyle: String = "",
    val equippedNameColor: String = "",
    val equippedBorderUrl: String = "",
    val equippedProfileEffectUrl: String = "",
    val isVerified: Boolean = false,
    val verifiedColor: String = VERIFIED_BADGE_DEFAULT_HEX
)

data class GoogleSessionInfo(
    val uid: String,
    val name: String,
    val email: String,
    val avatar: String
)

data class PromoPopupConfig(
    val isActive: Boolean = false,
    val promoId: String = "",
    val imageUrl: String = "",
    val targetUrl: String = "",
    val intervalHours: Float = 8f,
    val maxImpressions: Int = 0,
    val updatedAt: Long = 0L
) {
    val enabled: Boolean get() = isActive
}

/**
 * Pengelola utama autentikasi Google, profil pengguna, sistem level/title/misi,
 * dan papan peringkat real-time dari Firebase Firestore (100% TANPA DATA PALSU/PLACEHOLDER).
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    const val WEB_CLIENT_ID = "683241181647-gp00tdtgh2bkn8bojm0pl1hf0eriduq1.apps.googleusercontent.com"
    private const val PREFS_NAME = "yuranime_google_session_prefs"

    // Border khusus podium Top 1, Top 2, dan Top 3
    const val TOP_1_BORDER_URL = "https://yuranime-tg-cdn.gadingkencana04.workers.dev/f/BQACAgUAAyEGAAMBCfs22gADBWr42b3FJPD7cmmoFeKiMPzulw5nAAJQKgAC6K7IV6gYJVEAAb5wBT0E/top_1_border_url.png"
    const val TOP_2_BORDER_URL = "https://yuranime-tg-cdn.gadingkencana04.workers.dev/f/BQACAgUAAyEGAAMBCfs22gADBmr42cstH_akk1CXWyCItkIXZJ6mAAJRKgAC6K7IV6NESEpi9FYxPQQ/top_2_border_url.png"
    const val TOP_3_BORDER_URL = "https://yuranime-tg-cdn.gadingkencana04.workers.dev/f/BQACAgUAAyEGAAMBCfs22gADB2r42dDemUCiG2gxsx0FrPdq8WzNAAJTKgAC6K7IV0VQ5hQdaw9mPQQ/top_3_border_url.png"

    // 8 Pilihan Warna Bebas Style Nama (YuraStyle)
    val NAME_STYLE_SWATCHES: List<NameStyleColorSwatch> = listOf(
        NameStyleColorSwatch("emerald", "Emerald Green", "#4ADE80", 0xFF4ADE80),
        NameStyleColorSwatch("gold", "Amber Gold", "#FACC15", 0xFFFACC15),
        NameStyleColorSwatch("cyan", "Sky Cyan", "#38BDF8", 0xFF38BDF8),
        NameStyleColorSwatch("purple", "Royal Purple", "#A855F7", 0xFFA855F7),
        NameStyleColorSwatch("crimson", "Crimson Ruby", "#F43F5E", 0xFFF43F5E),
        NameStyleColorSwatch("silver", "Silver Mist", "#94A3B8", 0xFF94A3B8),
        NameStyleColorSwatch("white", "Pure White", "#FFFFFF", 0xFFFFFFFF),
        NameStyleColorSwatch("teal", "Mint Teal", "#2DD4BF", 0xFF2DD4BF)
    )

    // Katalog 8 Style Nama (6 Mythic + 2 Legendary hasil reverse engineering WibuTime)
    val NAME_STYLE_CATALOG: List<NameStyleDefinition> = listOf(
        NameStyleDefinition(
            id = "star",
            name = "Star",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#FACC15",
            description = "Partikel bintang 4-sudut berkilau halus di sekitar huruf nama"
        ),
        NameStyleDefinition(
            id = "twilight",
            name = "Twilight",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#A855F7",
            description = "Sapuan gradasi kosmik dengan taburan debu bintang senja"
        ),
        NameStyleDefinition(
            id = "slash",
            name = "Slash",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#38BDF8",
            description = "Tebasan pedang katana diagonal dengan kilatan cahaya tajam"
        ),
        NameStyleDefinition(
            id = "glitch",
            name = "Glitch",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#38BDF8",
            description = "Teks miring distorsi siber dengan slice horizontal & RGB chromatic"
        ),
        NameStyleDefinition(
            id = "magma",
            name = "Magma",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#F43F5E",
            description = "Aliran magma pijar dengan percikan bara api naik dari dasar nama"
        ),
        NameStyleDefinition(
            id = "sonar",
            name = "Sonar",
            tier = NameStyleTier.MYTHIC,
            pointCost = 4500L,
            defaultColorHex = "#2DD4BF",
            description = "Garis pemindai gelombang sonar vertikal menyapu bolak-balik"
        ),
        NameStyleDefinition(
            id = "ruby",
            name = "Ruby",
            tier = NameStyleTier.LEGENDARY,
            pointCost = 2000L,
            defaultColorHex = "#F43F5E",
            description = "Kilau permata merah dengan pantulan cahaya diagonal elegan"
        ),
        NameStyleDefinition(
            id = "wave",
            name = "Wave",
            tier = NameStyleTier.LEGENDARY,
            pointCost = 2000L,
            defaultColorHex = "#38BDF8",
            description = "Setiap huruf bergerak naik-turun membentuk gelombang sinus mulus"
        )
    )

    fun resolveNameStyleDefinition(styleId: String?): NameStyleDefinition? {
        val clean = styleId?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (clean.isBlank()) return null
        return NAME_STYLE_CATALOG.firstOrNull { it.id == clean || it.name.equals(clean, ignoreCase = true) }
    }

    // Katalog Title Resmi (Milestone Level, Shop Bertingkat, & Redeem Eksklusif)
    val ALL_TITLES: List<TitleDefinition> = listOf(
        // === A. MILESTONE LEVEL (Gratis Starter -> Meriah di Level Tinggi) ===
        TitleDefinition(
            id = "title_lv1_starter",
            titleName = "WARGA YURANIME",
            tierLevel = 1,
            styleVariant = 0,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 1,
            description = "Pelat pemula sederhana untuk anggota baru Yuranime",
            vfxTier = TitleVfxTier.NONE
        ),
        TitleDefinition(
            id = "title_lv5_pioneer",
            titleName = "PERINTIS ANIME",
            tierLevel = 6,
            styleVariant = 1,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 5,
            description = "Capai Level 5 lewat misi menonton & check-in harian",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "title_lv10_explorer",
            titleName = "PENJELAJAH KOMIK",
            tierLevel = 10,
            styleVariant = 3,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 10,
            description = "Capai Level 10 dengan aktif menjelajahi anime & komik",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "title_lv15_vanguard",
            titleName = "VANGUARD OTAKU",
            tierLevel = 15,
            styleVariant = 4,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 15,
            description = "Capai Level 15 — Bingkai HUD Mecha dengan sapuan radar",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "title_lv25_dimension",
            titleName = "PENGUASA DIMENSI",
            tierLevel = 25,
            styleVariant = 1,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 25,
            description = "Capai Level 25 — Sirkuit antariksa dengan aura nebula",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "title_lv35_knight",
            titleName = "KSATRIA ISEKAI",
            tierLevel = 35,
            styleVariant = 2,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 35,
            description = "Capai Level 35 — Bilah faset poligon dengan bara api hangat",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "title_lv50_sepuh",
            titleName = "SEPUH YURANIME",
            tierLevel = 50,
            styleVariant = 3,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 50,
            description = "Capai Level 50 — Gelar naga legendaris berkobar api aktif",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "title_lv65_grandmaster",
            titleName = "GRANDMASTER LORE",
            tierLevel = 65,
            styleVariant = 4,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 65,
            description = "Capai Level 65 — Pelat mecha berlapis foil prismatik",
            vfxTier = TitleVfxTier.HOLO_PRISM
        ),
        TitleDefinition(
            id = "title_lv80_emperor",
            titleName = "EMPEROR OF OTAKU",
            tierLevel = 80,
            styleVariant = 2,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 80,
            description = "Capai Level 80 — Trofi kaisar dengan sambaran petir bercabang",
            vfxTier = TitleVfxTier.LIGHTNING_STORM
        ),
        TitleDefinition(
            id = "title_lv100_akashic",
            titleName = "AKASHIC SOVEREIGN",
            tierLevel = 100,
            styleVariant = 1,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 100,
            description = "Capai Level 100 — Takhta Phoenix tertinggi dengan korona surya",
            vfxTier = TitleVfxTier.SOLAR_FLARE
        ),

        // === B. POINTS SHOP — ENTRY / CASUAL (Terjangkau, Gaya Bersih & Sederhana) ===
        TitleDefinition(
            id = "shop_vault_hunter",
            titleName = "PEMBURU KOLEKSI",
            tierLevel = 4,
            styleVariant = 0,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 1,
            pointCost = 450L,
            description = "Badge tas petualang klasik untuk kolektor awal",
            vfxTier = TitleVfxTier.NONE
        ),
        TitleDefinition(
            id = "shop_ramen_chef",
            titleName = "PENJAGA RAMEN",
            tierLevel = 5,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 1,
            pointCost = 650L,
            description = "Pot hangat bergaya diagonal simpel untuk penikmat slice-of-life",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "shop_cozy_pumpkin",
            titleName = "PUMPKIN PHANTOM",
            tierLevel = 6,
            styleVariant = 0,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 1,
            pointCost = 800L,
            description = "Lentera labu klasik dengan efek kilau tipis",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "shop_peace_keeper",
            titleName = "PEACEKEEPER",
            tierLevel = 7,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 1,
            pointCost = 950L,
            description = "Simbol merpati damai bagi anggota ramah Global Chat",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "shop_alchemist_heart",
            titleName = "ALCHEMIST HEART",
            tierLevel = 9,
            styleVariant = 0,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 3,
            pointCost = 1100L,
            description = "Labu alkimia romantis dengan sapuan cahaya lembut",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),
        TitleDefinition(
            id = "shop_iron_brawler",
            titleName = "IRON BRAWLER",
            tierLevel = 10,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 5,
            pointCost = 1350L,
            description = "Tinju besi faset obsidian untuk fans genre shonen tarung",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_forge_master",
            titleName = "FORGE MASTER",
            tierLevel = 11,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 5,
            pointCost = 1500L,
            description = "Paron tempa mekanis bergaya bingkai HUD metalik",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_love_potion",
            titleName = "CRIMSON ELIXIR",
            tierLevel = 12,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 5,
            pointCost = 1600L,
            description = "Ramuan vitalitas bergaya potongan diagonal tajam",
            vfxTier = TitleVfxTier.SUBTLE_SHIMMER
        ),

        // === C. POINTS SHOP — RARE / EPIC (Efek Cyber, Frost, Holo & Ember) ===
        TitleDefinition(
            id = "shop_crimson_knight",
            titleName = "KSATRIA CRIMSON",
            tierLevel = 14,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 8,
            pointCost = 2000L,
            description = "Dua pedang bersilang dengan percikan bara merah menyala",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_marathon_king",
            titleName = "RAJA MARATON",
            tierLevel = 16,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 10,
            pointCost = 2400L,
            description = "Jam pasir penakluk anime dengan trace sirkuit & radar pulse",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_goblin_slayer",
            titleName = "GOBLIN SLAYER",
            tierLevel = 17,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 10,
            pointCost = 2600L,
            description = "Pelat mecha pemburu dungeon berbalut kilau bara merah",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_amethyst_lord",
            titleName = "BANGSAWAN KOMIK",
            tierLevel = 20,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 12,
            pointCost = 3000L,
            description = "Bilah faset istana kristal dengan spektrum holografik",
            vfxTier = TitleVfxTier.HOLO_PRISM
        ),
        TitleDefinition(
            id = "shop_druid_sanctuary",
            titleName = "DRUID SANCTUARY",
            tierLevel = 21,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 12,
            pointCost = 3200L,
            description = "Tongkat alam zamrud dengan denyut sirkuit hijau neon",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_treasure_warden",
            titleName = "VAULT WARDEN",
            tierLevel = 22,
            styleVariant = 0,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 15,
            pointCost = 3500L,
            description = "Penjaga peti pusaka emas dengan efek kilau prismatik",
            vfxTier = TitleVfxTier.HOLO_PRISM
        ),
        TitleDefinition(
            id = "shop_rider_nomad",
            titleName = "NOMAD RIDER",
            tierLevel = 23,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 15,
            pointCost = 3600L,
            description = "Pelana pengembara dunia paralel dengan strip diagonal",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_glacial_monarch",
            titleName = "GLACIAL MONARCH",
            tierLevel = 25,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 15,
            pointCost = 4200L,
            description = "Faset kristal es biru membeku dengan refraksi prisma",
            vfxTier = TitleVfxTier.FROST_PRISM
        ),
        TitleDefinition(
            id = "shop_cyber_netrunner",
            titleName = "CYBER NETRUNNER",
            tierLevel = 27,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 18,
            pointCost = 4800L,
            description = "Roda gigi cyber dengan node sirkuit & scanline aktif",
            vfxTier = TitleVfxTier.PULSE_SCAN
        ),
        TitleDefinition(
            id = "shop_dizzy_trickster",
            titleName = "PHANTOM JESTER",
            tierLevel = 28,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 18,
            pointCost = 5000L,
            description = "Gelar ilusionis kosmik dengan gelombang nebula ungu",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "shop_berserker_cleaver",
            titleName = "BERSERKER CLEAVER",
            tierLevel = 30,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 20,
            pointCost = 5400L,
            description = "Kapak raksasa dengan letupan partikel bara darah",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_bombardier",
            titleName = "ARCANE BOMBARDIER",
            tierLevel = 31,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 20,
            pointCost = 5600L,
            description = "Meriam mecha berenergi ledak & kilau percikan api",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_beast_tamer",
            titleName = "BEAST TAMER",
            tierLevel = 32,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 20,
            pointCost = 5800L,
            description = "Lambang beruang buas di pelat sirkuit bertenaga",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_shadow_shinobi",
            titleName = "SHADOW SHINOBI",
            tierLevel = 34,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 22,
            pointCost = 6200L,
            description = "Shuriken bayangan dengan kabut void ungu & biru tua",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "shop_ronin_katana",
            titleName = "RONIN KATANA",
            tierLevel = 35,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 22,
            pointCost = 6800L,
            description = "Bilah katana mecha tajam dengan percikan api baja",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_fuse_demolition",
            titleName = "PYRO TECHNICIAN",
            tierLevel = 36,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 25,
            pointCost = 7200L,
            description = "Bom sumbu klasik dibalut kristal faset membara",
            vfxTier = TitleVfxTier.EMBER_SPARK
        ),
        TitleDefinition(
            id = "shop_skull_warlord",
            titleName = "SKULL WARLORD",
            tierLevel = 37,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 25,
            pointCost = 7500L,
            description = "Tengkorak berhelm perang dengan aura void mematikan",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "shop_curse_collar",
            titleName = "CURSE BEARER",
            tierLevel = 38,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 25,
            pointCost = 7800L,
            description = "Kalung kutukan kuno dalam bingkai slash bermuatan nebula",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),

        // === D. POINTS SHOP — LUXURY / MYTHIC (Kobaran Api, Petir Bercabang, & Korona Surya!) ===
        TitleDefinition(
            id = "shop_inferno_emperor",
            titleName = "INFERNO EMPEROR",
            tierLevel = 45,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 30,
            pointCost = 9500L,
            description = "Aura api neraka berkobar nyata di atas pelat faset gelap",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "shop_raijin_thunder",
            titleName = "RAIJIN THUNDER",
            tierLevel = 48,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 30,
            pointCost = 10500L,
            description = "Kilat bercabang menyambar aktif melintasi bingkai HUD mecha",
            vfxTier = TitleVfxTier.LIGHTNING_STORM
        ),
        TitleDefinition(
            id = "shop_crimson_shogun",
            titleName = "CRIMSON SHOGUN",
            tierLevel = 52,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 35,
            pointCost = 12000L,
            description = "Kabuto samurai agung diselimuti lidah api crimson membara",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "shop_astral_sorcerer",
            titleName = "ASTRAL SORCERER",
            tierLevel = 55,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 35,
            pointCost = 13500L,
            description = "Pusaran roh arwah pada sirkuit futuristik berkilau prismatik",
            vfxTier = TitleVfxTier.HOLO_PRISM
        ),
        TitleDefinition(
            id = "shop_abyssal_reaper",
            titleName = "ABYSSAL REAPER",
            tierLevel = 58,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 40,
            pointCost = 15000L,
            description = "Sabit maut berbalut pusaran energi void gelap nan megah",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "shop_storm_breaker",
            titleName = "STORM BREAKER",
            tierLevel = 62,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 40,
            pointCost = 16500L,
            description = "Pedang halilintar bersambaran petir biru elektrik tanpa henti",
            vfxTier = TitleVfxTier.LIGHTNING_STORM
        ),
        TitleDefinition(
            id = "shop_obsidian_eye",
            titleName = "OBSIDIAN EYE",
            tierLevel = 64,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 40,
            pointCost = 17500L,
            description = "Mata iblis abadi dalam bilah obsidian bercahaya plasma ungu",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "shop_meteor_destroyer",
            titleName = "METEOR DESTROYER",
            tierLevel = 70,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 45,
            pointCost = 19500L,
            description = "Meteor jatuh berkecepatan penuh dibakar api berkobar dahsyat",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "shop_celestial_archon",
            titleName = "CELESTIAL ARCHON",
            tierLevel = 75,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 50,
            pointCost = 22000L,
            description = "Halo bertaji agung bermandikan korona surya emas berkilau",
            vfxTier = TitleVfxTier.SOLAR_FLARE
        ),
        TitleDefinition(
            id = "shop_solar_valkyrie",
            titleName = "SOLAR VALKYRIE",
            tierLevel = 80,
            styleVariant = 4,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 55,
            pointCost = 24000L,
            description = "Griffin suci pada bingkai mecha bertenaga badai cahaya matahari",
            vfxTier = TitleVfxTier.SOLAR_FLARE
        ),
        TitleDefinition(
            id = "shop_mythic_sovereign",
            titleName = "MYTHIC SOVEREIGN",
            tierLevel = 90,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            requiredLevel = 60,
            pointCost = 28000L,
            description = "Permata api kasta tertinggi dengan kobaran inferno & aura emas",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),

        // === E. EXCLUSIVE REDEEM ONLY (Hanya Via Kode Redeem Resmi / Event Web Admin!) ===
        TitleDefinition(
            id = "redeem_vip_yuranime",
            titleName = "VIP YURANIME",
            tierLevel = 70,
            styleVariant = 1,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Pelat Sirkuit VIP berkilau holografik",
            vfxTier = TitleVfxTier.HOLO_PRISM
        ),
        TitleDefinition(
            id = "redeem_khalifah_anime",
            titleName = "KHALIFAH ANIME",
            tierLevel = 82,
            styleVariant = 2,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Faset emas suci berkorona bintang surya",
            vfxTier = TitleVfxTier.SOLAR_FLARE
        ),
        TitleDefinition(
            id = "redeem_tempest_overlord",
            titleName = "TEMPEST OVERLORD",
            tierLevel = 85,
            styleVariant = 4,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Badai petir terfokus bersambaran hebat",
            vfxTier = TitleVfxTier.LIGHTNING_STORM
        ),
        TitleDefinition(
            id = "redeem_eternal_phoenix",
            titleName = "ETERNAL PHOENIX",
            tierLevel = 88,
            styleVariant = 3,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Gulungan kuno terbakar kobaran api abadi",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "redeem_lunar_predator",
            titleName = "LUNAR PREDATOR",
            tierLevel = 90,
            styleVariant = 2,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Cakar bulan sabit diselimuti nebula malam",
            vfxTier = TitleVfxTier.VOID_NEBULA
        ),
        TitleDefinition(
            id = "redeem_dragon_emperor",
            titleName = "DRAGON EMPEROR",
            tierLevel = 95,
            styleVariant = 4,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Perisai naga mecha dengan lidah api naga",
            vfxTier = TitleVfxTier.BLAZING_INFERNO
        ),
        TitleDefinition(
            id = "redeem_starfall_god",
            titleName = "STARFALL GOD",
            tierLevel = 99,
            styleVariant = 1,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            requiredLevel = 1,
            pointCost = 0L,
            description = "Eksklusif Redeem Code — Bintang jatuh ilahi bermandikan cahaya surya",
            vfxTier = TitleVfxTier.SOLAR_FLARE
        )
    )

    // Seluruh kosmetik dibaca langsung dari katalog Discord Asli (985 Border + 396 Profile Effect)
    val SHOP_BORDERS: List<BorderDefinition> = emptyList()
    val SHOP_PROFILE_EFFECTS: List<ProfileEffectDefinition> = emptyList()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + GlobalCrashHandler.coroutineExceptionHandler)

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            try {
                val app = FirebaseApp.getInstance()
                FirebaseFirestore.getInstance(app)
            } catch (fallbackEx: Exception) {
                try {
                    val app = FirebaseApp.getInstance()
                    FirebaseFirestore.getInstance(app, "ai-studio-eff2e4c3-4396-44bb-b0c9-0936b4c631a4")
                } catch (ignored: Exception) {
                    FirebaseFirestore.getInstance()
                }
            }
        }
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _googleSession = MutableStateFlow<SavedGoogleUserSession?>(null)
    val googleSession: StateFlow<SavedGoogleUserSession?> = _googleSession.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _watchHistorySyncVersion = MutableStateFlow(0L)
    val watchHistorySyncVersion: StateFlow<Long> = _watchHistorySyncVersion.asStateFlow()

    @Volatile
    private var lastSyncedWatchHistoryUid: String? = null

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(true)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _promoPopupConfig = MutableStateFlow<PromoPopupConfig?>(null)
    val promoPopupConfig: StateFlow<PromoPopupConfig?> = _promoPopupConfig.asStateFlow()

    private var userDocListener: ListenerRegistration? = null
    private var leaderboardListener: ListenerRegistration? = null
    private var promoListener: ListenerRegistration? = null

    init {
        try {
            startListeningPromoConfig()
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { fbAuth ->
                _currentUser.value = fbAuth.currentUser
                if (fbAuth.currentUser != null) {
                    startListeningCurrentUser()
                } else if (_googleSession.value == null) {
                    _userProfile.value = null
                    userDocListener?.remove()
                    userDocListener = null
                }
            }
        } catch (ignored: Exception) {}
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null || _googleSession.value != null
    }

    fun isGoogleSessionActive(): Boolean {
        return _googleSession.value != null
    }

    fun resolveCanonicalUserId(email: String?, fallbackUid: String? = null): String {
        val cleanEmail = email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (cleanEmail.isNotBlank() && cleanEmail.contains("@")) {
            return "g_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")
        }
        return fallbackUid?.trim().orEmpty()
    }

    fun getActiveUid(): String? {
        val email = _googleSession.value?.email?.takeIf { it.isNotBlank() }
            ?: try { auth.currentUser?.email?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
            ?: _userProfile.value?.email?.takeIf { it.isNotBlank() }
        if (!email.isNullOrBlank()) {
            return resolveCanonicalUserId(email, _googleSession.value?.id ?: auth.currentUser?.uid)
        }
        return _googleSession.value?.id ?: try { auth.currentUser?.uid } catch (_: Exception) { null } ?: _userProfile.value?.uid
    }

    fun getCurrentUid(): String? = getActiveUid()

    fun computeLevelFromExp(exp: Long): Int {
        val safeExp = exp.coerceAtLeast(0L)
        return ((safeExp / 500L).toInt() + 1).coerceAtLeast(1)
    }

    fun computeNextLevelTargetExp(level: Int): Long {
        return (level.coerceAtLeast(1) * 500L)
    }

    fun computeExpProgress(exp: Long, nextLevelTargetExp: Long): Float {
        if (nextLevelTargetExp <= 0L) return 0f
        return (exp.coerceAtLeast(0L).toFloat() / nextLevelTargetExp.toFloat()).coerceIn(0f, 1f)
    }

    fun computeRankTitle(level: Int): String {
        return when {
            level >= 100 -> "Celestial Sovereign"
            level >= 85 -> "Grandmaster Akashic"
            level >= 75 -> "Monarch Dimensi"
            level >= 60 -> "Overlord Yuranime"
            level >= 45 -> "Archmage Isekai"
            level >= 30 -> "Sepuh Yuranime"
            level >= 20 -> "Vanguard Otaku"
            level >= 10 -> "Penjelajah Komik"
            level >= 5 -> "Perintis Anime"
            else -> "Wibu Biasa"
        }
    }

    fun computeUserTag(uid: String, existingTag: String? = null): String {
        val cleaned = existingTag?.trim()?.removePrefix("#").orEmpty()
        if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
            return cleaned
        }
        if (uid.isBlank()) return "1001"
        val hash = abs(uid.hashCode().toLong())
        val fourDigits = (hash % 9000L) + 1000L
        return fourDigits.toString()
    }

    fun formatNumber(value: Long): String {
        return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(value)
    }

    fun resolveTitleDefinition(titleName: String, fallbackLevel: Int = 1): TitleDefinition? {
        if (titleName.isBlank()) return null
        val matched = ALL_TITLES.find { it.titleName.equals(titleName.trim(), ignoreCase = true) }
        if (matched != null) return matched
        val inferredVfx = when {
            fallbackLevel >= 50 -> TitleVfxTier.BLAZING_INFERNO
            fallbackLevel >= 35 -> TitleVfxTier.HOLO_PRISM
            fallbackLevel >= 20 -> TitleVfxTier.PULSE_SCAN
            else -> TitleVfxTier.SUBTLE_SHIMMER
        }
        return TitleDefinition(
            id = "custom_${titleName.lowercase(Locale.ROOT)}",
            titleName = titleName.uppercase(Locale.ROOT),
            tierLevel = fallbackLevel,
            styleVariant = fallbackLevel % 5,
            unlockType = TitleUnlockType.REDEEM_ONLY,
            vfxTier = inferredVfx
        )
    }

    private fun formatJoinDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestampMs))
    }

    private fun extractJoinDateFromSnapshot(snapshot: DocumentSnapshot?, fallbackAuthTimestamp: Long?): String {
        if (snapshot != null && snapshot.exists()) {
            val directStr = snapshot.getString("joinDate")?.trim()
            if (!directStr.isNullOrBlank()) return directStr

            val createdAtAny = snapshot.get("createdAt") ?: snapshot.get("registeredAt")
            when (createdAtAny) {
                is Timestamp -> return formatJoinDate(createdAtAny.toDate().time)
                is Long -> if (createdAtAny > 1_000_000_000_000L) return formatJoinDate(createdAtAny)
                is Number -> {
                    val ms = createdAtAny.toLong()
                    if (ms > 1_000_000_000_000L) return formatJoinDate(ms)
                }
                is Date -> return formatJoinDate(createdAtAny.time)
            }
        }
        if (fallbackAuthTimestamp != null && fallbackAuthTimestamp > 1_000_000_000_000L) {
            return formatJoinDate(fallbackAuthTimestamp)
        }
        return formatJoinDate(System.currentTimeMillis())
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initSession(context: Context) {
        val fbUser = try { auth.currentUser } catch (_: Exception) { null }
        _currentUser.value = fbUser

        val savedGoogle = AppPreferences.getSavedGoogleSession(context)
        val prefs = getPrefs(context)
        val savedUid = savedGoogle?.id ?: prefs.getString("uid", null)

        if (fbUser != null) {
            val resolvedEmail = fbUser.email ?: savedGoogle?.email ?: prefs.getString("email", "") ?: ""
            val canonicalId = resolveCanonicalUserId(resolvedEmail, fbUser.uid)
            val session = SavedGoogleUserSession(
                id = canonicalId,
                name = savedGoogle?.name ?: fbUser.displayName ?: prefs.getString("name", "Member Yuranime") ?: "Member Yuranime",
                email = resolvedEmail,
                photoUrl = savedGoogle?.photoUrl ?: fbUser.photoUrl?.toString() ?: prefs.getString("avatar", "")
            )
            _googleSession.value = session
            loadCachedProfile(prefs, session)
            startListeningCurrentUser(context)
        } else if (savedGoogle != null) {
            val canonicalId = resolveCanonicalUserId(savedGoogle.email, savedGoogle.id)
            val normalizedSession = savedGoogle.copy(id = canonicalId)
            _googleSession.value = normalizedSession
            loadCachedProfile(prefs, normalizedSession)
            startListeningCurrentUser(context)
        } else if (!savedUid.isNullOrBlank()) {
            val resolvedEmail = prefs.getString("email", "") ?: ""
            val canonicalId = resolveCanonicalUserId(resolvedEmail, savedUid)
            val session = SavedGoogleUserSession(
                id = canonicalId,
                name = prefs.getString("name", "Member Yuranime") ?: "Member Yuranime",
                email = resolvedEmail,
                photoUrl = prefs.getString("avatar", "")
            )
            _googleSession.value = session
            loadCachedProfile(prefs, session)
            startListeningCurrentUser(context)
        }
        deduplicateUserAccounts(context)
        startListeningLeaderboard()
    }

    private fun loadCachedProfile(prefs: SharedPreferences, session: SavedGoogleUserSession) {
        val exp = prefs.getLong("exp_${session.id}", 0L)
        val points = prefs.getLong("points_${session.id}", 100L)
        val level = computeLevelFromExp(exp)
        val nextTarget = computeNextLevelTargetExp(level)
        val progress = computeExpProgress(exp, nextTarget)
        val cachedName = prefs.getString("profile_name_${session.id}", session.name) ?: session.name
        val cachedUsername = prefs.getString(
            "profile_username_${session.id}",
            cachedName.lowercase(Locale.ROOT).replace(" ", "")
        ) ?: "member"
        val cachedAvatar = prefs.getString("profile_avatar_${session.id}", session.photoUrl ?: "") ?: (session.photoUrl ?: "")
        val cachedBanner = prefs.getString("profile_banner_${session.id}", "") ?: ""
        val cachedBio = prefs.getString("profile_bio_${session.id}", "Penikmat anime & komik di Yuranime.")
            ?: "Penikmat anime & komik di Yuranime."
        val cachedRole = prefs.getString("profile_role_${session.id}", "Member") ?: "Member"
        val cachedTag = computeUserTag(session.id, prefs.getString("profile_tag_${session.id}", null))
        val cachedJoinDate = prefs.getString("profile_joindate_${session.id}", null)
            ?: formatJoinDate(System.currentTimeMillis())
        val cachedEquippedTitle = prefs.getString("profile_title_${session.id}", "") ?: ""
        val cachedTitleDef = resolveTitleDefinition(cachedEquippedTitle, level)
        val cachedNameStyle = prefs.getString("profile_name_style_${session.id}", "") ?: ""
        val cachedNameColor = prefs.getString("profile_name_color_${session.id}", "") ?: ""
        val cachedBorderUrl = prefs.getString("profile_border_${session.id}", "") ?: ""
        val cachedEffectUrl = prefs.getString("profile_effect_${session.id}", "") ?: ""
        val cachedCosmeticAnimate = prefs.getBoolean("cosmetic_animate_${session.id}", true)
        val cachedShowMiniTitle = prefs.getBoolean("show_mini_title_${session.id}", true)
        val cachedShowCustomTitle = prefs.getBoolean("show_custom_title_${session.id}", true)
        val cachedShowRole = prefs.getBoolean("show_role_${session.id}", true)
        val cachedUnlockedTitles = prefs.getStringSet("profile_unlocked_titles_${session.id}", setOf("WARGA YURANIME"))
            ?.toList() ?: listOf("WARGA YURANIME")
        val cachedUnlockedNameStyles = prefs.getStringSet("profile_unlocked_name_styles_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val cachedUnlockedBorders = prefs.getStringSet("profile_unlocked_borders_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val cachedUnlockedEffects = prefs.getStringSet("profile_unlocked_effects_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val cachedClaimedCodes = prefs.getStringSet("profile_claimed_codes_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val lastCheckIn = prefs.getString("last_checkin_${session.id}", "") ?: ""
        val streak = prefs.getInt("streak_${session.id}", 0)
        val cachedVerified = prefs.getBoolean("profile_verified_${session.id}", false)
        val cachedVerifiedColor = prefs.getString("profile_verified_color_${session.id}", VERIFIED_BADGE_DEFAULT_HEX)
            ?: VERIFIED_BADGE_DEFAULT_HEX

        _userProfile.value = UserProfile(
            uid = session.id,
            name = cachedName,
            username = cachedUsername,
            userTag = cachedTag,
            email = session.email,
            avatar = cachedAvatar,
            banner = cachedBanner,
            bio = cachedBio,
            role = cachedRole,
            joinDate = cachedJoinDate,
            exp = exp,
            points = points,
            level = level,
            rankTitle = computeRankTitle(level),
            equippedTitle = cachedEquippedTitle,
            equippedTitleTier = cachedTitleDef?.tierLevel ?: level,
            equippedTitleVariant = cachedTitleDef?.styleVariant ?: 0,
            equippedNameStyle = cachedNameStyle,
            equippedNameColor = cachedNameColor,
            equippedBorderUrl = cachedBorderUrl,
            equippedProfileEffectUrl = cachedEffectUrl,
            cosmeticAnimate = cachedCosmeticAnimate,
            showMiniTitle = cachedShowMiniTitle,
            showCustomTitle = cachedShowCustomTitle,
            showRole = cachedShowRole,
            unlockedTitles = cachedUnlockedTitles,
            unlockedNameStyles = cachedUnlockedNameStyles,
            unlockedBorders = cachedUnlockedBorders,
            unlockedEffects = cachedUnlockedEffects,
            claimedRedeemCodes = cachedClaimedCodes,
            lastCheckInDate = lastCheckIn,
            checkInStreak = streak,
            nextLevelTargetExp = nextTarget,
            expProgress = progress,
            isVerified = cachedVerified,
            verifiedColor = cachedVerifiedColor
        )
    }

    private fun saveProfileToPrefs(context: Context, profile: UserProfile) {
        getPrefs(context).edit()
            .putString("uid", profile.uid)
            .putString("name", profile.name)
            .putString("email", profile.email)
            .putString("avatar", profile.avatar)
            .putString("profile_name_${profile.uid}", profile.name)
            .putString("profile_username_${profile.uid}", profile.username)
            .putString("profile_tag_${profile.uid}", profile.userTag)
            .putString("profile_avatar_${profile.uid}", profile.avatar)
            .putString("profile_banner_${profile.uid}", profile.banner)
            .putString("profile_bio_${profile.uid}", profile.bio)
            .putString("profile_role_${profile.uid}", profile.role)
            .putString("profile_joindate_${profile.uid}", profile.joinDate)
            .putString("profile_title_${profile.uid}", profile.equippedTitle)
            .putString("profile_name_style_${profile.uid}", profile.equippedNameStyle)
            .putString("profile_name_color_${profile.uid}", profile.equippedNameColor)
            .putString("profile_border_${profile.uid}", profile.equippedBorderUrl)
            .putString("profile_effect_${profile.uid}", profile.equippedProfileEffectUrl)
            .putBoolean("cosmetic_animate_${profile.uid}", profile.cosmeticAnimate)
            .putBoolean("show_mini_title_${profile.uid}", profile.showMiniTitle)
            .putBoolean("show_custom_title_${profile.uid}", profile.showCustomTitle)
            .putBoolean("show_role_${profile.uid}", profile.showRole)
            .putStringSet("profile_unlocked_titles_${profile.uid}", profile.unlockedTitles.toSet())
            .putStringSet("profile_unlocked_name_styles_${profile.uid}", profile.unlockedNameStyles.toSet())
            .putStringSet("profile_unlocked_borders_${profile.uid}", profile.unlockedBorders.toSet())
            .putStringSet("profile_unlocked_effects_${profile.uid}", profile.unlockedEffects.toSet())
            .putStringSet("profile_claimed_codes_${profile.uid}", profile.claimedRedeemCodes.toSet())
            .putLong("exp_${profile.uid}", profile.exp)
            .putLong("points_${profile.uid}", profile.points)
            .putString("last_checkin_${profile.uid}", profile.lastCheckInDate)
            .putInt("streak_${profile.uid}", profile.checkInStreak)
            .putBoolean("profile_verified_${profile.uid}", profile.isVerified)
            .putString("profile_verified_color_${profile.uid}", profile.verifiedColor)
            .apply()
    }

    private fun decodeJwtPayload(idToken: String): JSONObject? {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val decodedBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
                JSONObject(String(decodedBytes, Charsets.UTF_8))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun syncLocalAuthDatabase(context: Context?, profile: UserProfile) {
        if (context == null) return
        val db = LocalAuthDatabase(context)
        db.setLoggedIn(true, profile.name)
        db.setAvatarUrl(profile.avatar)
        db.setCoverUrl(profile.banner)
    }

    fun onGoogleAccountSelected(
        uid: String,
        name: String,
        email: String,
        avatar: String?,
        idToken: String? = null,
        context: Context? = null
    ) {
        scope.launch {
            onGoogleAccountSelectedAwait(
                uid = uid,
                name = name,
                email = email,
                avatar = avatar,
                idToken = idToken,
                context = context
            )
        }
    }

    /** Memeriksa dokumen Firestore dulu sebelum menyentuh AppPreferences agar profil editan tidak tertimpa. */
    suspend fun onGoogleAccountSelectedAwait(
        uid: String,
        name: String,
        email: String,
        avatar: String?,
        idToken: String? = null,
        context: Context? = null
    ): UserProfile {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        if (!idToken.isNullOrBlank()) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                _currentUser.value = authResult.user ?: auth.currentUser
            } catch (_: Exception) {}
        }

        val canonicalUid = resolveCanonicalUserId(cleanEmail, uid)
        val profile = hydrateOrCreateGoogleUser(
            context = context,
            canonicalUid = canonicalUid,
            googleName = name,
            cleanEmail = cleanEmail,
            googlePhotoUrl = avatar
        )
        deduplicateUserAccounts(context)
        startListeningCurrentUser(context)
        startListeningLeaderboard()
        return profile
    }

    suspend fun onGoogleAccountSelected(
        context: Context,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): Result<GoogleSessionInfo> {
        return try {
            val cleanEmail = email.trim().lowercase(Locale.ROOT)
            val deterministicUid = resolveCanonicalUserId(cleanEmail, "g_${cleanEmail.hashCode()}")
            val fallbackName = displayName?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val profile = hydrateOrCreateGoogleUser(
                context = context,
                canonicalUid = deterministicUid,
                googleName = fallbackName,
                cleanEmail = cleanEmail,
                googlePhotoUrl = photoUrl
            )
            deduplicateUserAccounts(context)
            startListeningCurrentUser(context)
            startListeningLeaderboard()

            Result.success(
                GoogleSessionInfo(
                    uid = profile.uid,
                    name = profile.name,
                    email = profile.email,
                    avatar = profile.avatar
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context): Result<GoogleSessionInfo> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val jwtPayload = decodeJwtPayload(idToken)
                val subUid = jwtPayload?.optString("sub")?.takeIf { it.isNotBlank() }
                    ?: "google_${googleIdTokenCredential.id.hashCode()}"
                val displayName = googleIdTokenCredential.displayName
                    ?: jwtPayload?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: googleIdTokenCredential.id.substringBefore("@")
                val email = jwtPayload?.optString("email")?.takeIf { it.isNotBlank() }
                    ?: googleIdTokenCredential.id
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    ?: jwtPayload?.optString("picture")
                    ?: ""

                var googleName = displayName
                var finalEmail = email.trim().lowercase(Locale.ROOT)
                var googlePhoto = photoUrl

                try {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        _currentUser.value = fbUser
                        googleName = fbUser.displayName ?: displayName
                        finalEmail = (fbUser.email ?: email).trim().lowercase(Locale.ROOT)
                        googlePhoto = fbUser.photoUrl?.toString() ?: photoUrl
                    }
                } catch (firebaseErr: Exception) {
                    Log.w(TAG, "Menggunakan sesi Google terverifikasi: ${firebaseErr.message}")
                }

                val finalUid = resolveCanonicalUserId(finalEmail, subUid)
                val profile = hydrateOrCreateGoogleUser(
                    context = context,
                    canonicalUid = finalUid,
                    googleName = googleName,
                    cleanEmail = finalEmail,
                    googlePhotoUrl = googlePhoto
                )
                deduplicateUserAccounts(context)
                startListeningCurrentUser(context)
                startListeningLeaderboard()
                Result.success(
                    GoogleSessionInfo(
                        uid = profile.uid,
                        name = profile.name,
                        email = profile.email,
                        avatar = profile.avatar
                    )
                )
            } else {
                Result.failure(Exception("Tipe kredensial Google tidak dikenali"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun hydrateOrCreateGoogleUser(
        context: Context?,
        canonicalUid: String,
        googleName: String,
        cleanEmail: String,
        googlePhotoUrl: String?
    ): UserProfile {
        val prefs = context?.let { getPrefs(it) }
        val docRef = firestore.collection("users").document(canonicalUid)

        var existingSnapshot: DocumentSnapshot? = null
        var fetchFailed = false
        try {
            val directSnap = docRef.get().await()
            if (directSnap.exists()) {
                existingSnapshot = directSnap
            } else if (cleanEmail.isNotBlank()) {
                val byEmail = firestore.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .limit(1)
                    .get()
                    .await()
                existingSnapshot = byEmail.documents.firstOrNull { it.exists() }
            }
        } catch (e: Exception) {
            fetchFailed = true
            Log.w(TAG, "Cek profil Firestore gagal saat login: ${e.message}")
        }

        val fallbackSession = SavedGoogleUserSession(
            id = canonicalUid,
            name = googleName,
            email = cleanEmail,
            photoUrl = googlePhotoUrl
        )

        if (existingSnapshot != null && existingSnapshot.exists()) {
            // Dokumen user sudah ada di Firestore: pertahankan seluruh profil hasil edit user.
            val parsed = parseUserProfileFromSnapshot(
                snapshot = existingSnapshot,
                fallbackUid = canonicalUid,
                fallbackSession = fallbackSession
            ).copy(uid = canonicalUid)

            val sessionPhoto = parsed.avatar.ifBlank { googlePhotoUrl.orEmpty() }
            val session = SavedGoogleUserSession(
                id = canonicalUid,
                name = parsed.name,
                email = parsed.email.ifBlank { cleanEmail },
                photoUrl = sessionPhoto.ifBlank { null }
            )
            _googleSession.value = session
            _userProfile.value = parsed

            if (context != null) {
                AppPreferences.saveGoogleSession(
                    context = context,
                    id = canonicalUid,
                    name = parsed.name,
                    email = session.email,
                    photoUrl = sessionPhoto
                )
                saveProfileToPrefs(context, parsed)
                syncLocalAuthDatabase(context, parsed)
            }

            try {
                val existingExp = maxOf(
                    existingSnapshot.getLong("exp") ?: 0L,
                    existingSnapshot.getLong("xp") ?: 0L
                )
                val patchMap = mutableMapOf<String, Any>(
                    "uid" to canonicalUid,
                    "exp" to existingExp,
                    "xp" to existingExp,
                    "userTag" to parsed.userTag,
                    "joinDate" to parsed.joinDate,
                    "lastActive" to FieldValue.serverTimestamp()
                )
                if (existingSnapshot.getString("email").isNullOrBlank() && cleanEmail.isNotBlank()) {
                    patchMap["email"] = cleanEmail
                }
                docRef.set(patchMap, SetOptions.merge()).await()
            } catch (_: Exception) {}

            return parsed
        }

        // Jika Firestore gagal diakses sementara saat login, utamakan cache lokal sebelum memakai data Google.
        val cachedName = prefs?.getString("profile_name_$canonicalUid", null)?.takeIf { it.isNotBlank() }
        val cachedAvatar = prefs?.getString("profile_avatar_$canonicalUid", null)?.takeIf { it.isNotBlank() }

        val localExp = prefs?.getLong("exp_$canonicalUid", 0L) ?: 0L
        val localPoints = prefs?.getLong("points_$canonicalUid", 100L) ?: 100L
        val level = computeLevelFromExp(localExp)
        val nextTarget = computeNextLevelTargetExp(level)
        val progress = computeExpProgress(localExp, nextTarget)
        val resolvedName = cachedName ?: googleName
        val defaultUsername = resolvedName.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "")
            .ifBlank { "member" }
        val userTag = computeUserTag(canonicalUid, prefs?.getString("profile_tag_$canonicalUid", null))
        val defaultJoinDate = formatJoinDate(
            try { auth.currentUser?.metadata?.creationTimestamp } catch (_: Exception) { null }
                ?: System.currentTimeMillis()
        )
        val resolvedAvatar = cachedAvatar ?: googlePhotoUrl.orEmpty()

        val initialProfile = UserProfile(
            uid = canonicalUid,
            name = resolvedName,
            username = prefs?.getString("profile_username_$canonicalUid", defaultUsername) ?: defaultUsername,
            userTag = userTag,
            email = cleanEmail,
            avatar = resolvedAvatar,
            banner = prefs?.getString("profile_banner_$canonicalUid", "") ?: "",
            bio = prefs?.getString("profile_bio_$canonicalUid", "Penikmat anime & komik di Yuranime.")
                ?: "Penikmat anime & komik di Yuranime.",
            role = prefs?.getString("profile_role_$canonicalUid", "Member") ?: "Member",
            joinDate = prefs?.getString("profile_joindate_$canonicalUid", defaultJoinDate) ?: defaultJoinDate,
            exp = localExp,
            points = localPoints,
            level = level,
            rankTitle = computeRankTitle(level),
            equippedTitle = prefs?.getString("profile_title_$canonicalUid", "") ?: "",
            equippedNameStyle = prefs?.getString("profile_name_style_$canonicalUid", "") ?: "",
            equippedNameColor = prefs?.getString("profile_name_color_$canonicalUid", "") ?: "",
            equippedBorderUrl = prefs?.getString("profile_border_$canonicalUid", "") ?: "",
            equippedProfileEffectUrl = prefs?.getString("profile_effect_$canonicalUid", "") ?: "",
            cosmeticAnimate = prefs?.getBoolean("cosmetic_animate_$canonicalUid", true) ?: true,
            showMiniTitle = prefs?.getBoolean("show_mini_title_$canonicalUid", AppPreferences.showMiniTitle.value)
                ?: AppPreferences.showMiniTitle.value,
            showCustomTitle = prefs?.getBoolean("show_custom_title_$canonicalUid", AppPreferences.showCustomTitle.value)
                ?: AppPreferences.showCustomTitle.value,
            showRole = prefs?.getBoolean("show_role_$canonicalUid", AppPreferences.showRole.value)
                ?: AppPreferences.showRole.value,
            unlockedTitles = listOf("WARGA YURANIME"),
            unlockedNameStyles = prefs?.getStringSet("profile_unlocked_name_styles_$canonicalUid", emptySet())?.toList()
                ?: emptyList(),
            unlockedBorders = prefs?.getStringSet("profile_unlocked_borders_$canonicalUid", emptySet())?.toList()
                ?: emptyList(),
            unlockedEffects = prefs?.getStringSet("profile_unlocked_effects_$canonicalUid", emptySet())?.toList()
                ?: emptyList(),
            claimedRedeemCodes = prefs?.getStringSet("profile_claimed_codes_$canonicalUid", emptySet())?.toList()
                ?: emptyList(),
            lastCheckInDate = prefs?.getString("last_checkin_$canonicalUid", "") ?: "",
            checkInStreak = prefs?.getInt("streak_$canonicalUid", 0) ?: 0,
            nextLevelTargetExp = nextTarget,
            expProgress = progress
        )

        val session = SavedGoogleUserSession(
            id = canonicalUid,
            name = initialProfile.name,
            email = cleanEmail,
            photoUrl = initialProfile.avatar.ifBlank { null }
        )
        _googleSession.value = session
        _userProfile.value = initialProfile

        if (context != null) {
            AppPreferences.saveGoogleSession(
                context = context,
                id = canonicalUid,
                name = initialProfile.name,
                email = cleanEmail,
                photoUrl = initialProfile.avatar
            )
            saveProfileToPrefs(context, initialProfile)
            syncLocalAuthDatabase(context, initialProfile)
        }

        if (!fetchFailed) {
            try {
                val newProfileMap = hashMapOf(
                    "uid" to canonicalUid,
                    "name" to initialProfile.name,
                    "displayName" to initialProfile.name,
                    "username" to initialProfile.username,
                    "userTag" to initialProfile.userTag,
                    "email" to initialProfile.email,
                    "avatar" to initialProfile.avatar,
                    "photoUrl" to initialProfile.avatar,
                    "banner" to initialProfile.banner,
                    "bannerUrl" to initialProfile.banner,
                    "bio" to initialProfile.bio,
                    "role" to initialProfile.role,
                    "joinDate" to initialProfile.joinDate,
                    "exp" to initialProfile.exp,
                    "xp" to initialProfile.exp,
                    "points" to initialProfile.points,
                    "level" to initialProfile.level,
                    "rankTitle" to initialProfile.rankTitle,
                    "equippedTitle" to initialProfile.equippedTitle,
                    "equippedNameStyle" to initialProfile.equippedNameStyle,
                    "equippedNameColor" to initialProfile.equippedNameColor,
                    "equippedBorderUrl" to initialProfile.equippedBorderUrl,
                    "equippedAvatarDecoration" to initialProfile.equippedBorderUrl,
                    "equippedProfileEffectUrl" to initialProfile.equippedProfileEffectUrl,
                    "equippedProfileEffect" to initialProfile.equippedProfileEffectUrl,
                    "cosmeticAnimate" to initialProfile.cosmeticAnimate,
                    "showMiniTitle" to initialProfile.showMiniTitle,
                    "showCustomTitle" to initialProfile.showCustomTitle,
                    "showRole" to initialProfile.showRole,
                    "unlockedTitles" to initialProfile.unlockedTitles,
                    "unlockedNameStyles" to initialProfile.unlockedNameStyles,
                    "unlockedBorders" to initialProfile.unlockedBorders,
                    "unlockedEffects" to initialProfile.unlockedEffects,
                    "claimedRedeemCodes" to initialProfile.claimedRedeemCodes,
                    "lastCheckInDate" to initialProfile.lastCheckInDate,
                    "checkInStreak" to initialProfile.checkInStreak,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastActive" to FieldValue.serverTimestamp()
                )
                docRef.set(newProfileMap, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menulis profil baru ke Firestore: ${e.message}")
            }
        }
        return initialProfile
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserProfileFromSnapshot(
        snapshot: DocumentSnapshot,
        fallbackUid: String,
        fallbackSession: SavedGoogleUserSession? = null,
        allowCurrentAuthTimestampFallback: Boolean = true
    ): UserProfile {
        val uid = snapshot.getString("uid")?.takeIf { it.isNotBlank() } ?: snapshot.id.ifBlank { fallbackUid }
        val exp = maxOf(
            readSnapshotLong(snapshot, "exp") ?: 0L,
            readSnapshotLong(snapshot, "xp") ?: 0L
        )
        val points = readSnapshotLong(snapshot, "points") ?: 100L
        val computedLevel = computeLevelFromExp(exp)
        val nextTarget = computeNextLevelTargetExp(computedLevel)
        val expProgress = computeExpProgress(exp, nextTarget)

        val name = snapshot.getString("name")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: fallbackSession?.name
            ?: "Member Yuranime"
        val username = snapshot.getString("username")?.takeIf { it.isNotBlank() }
            ?: name.lowercase(Locale.ROOT).replace(" ", "")
        val userTag = computeUserTag(uid, snapshot.getString("userTag"))
        val email = snapshot.getString("email")?.takeIf { it.isNotBlank() }
            ?: fallbackSession?.email
            ?: ""
        val rawAvatarField = snapshot.getString("avatar")?.trim().orEmpty()
        val rawPhotoUrlField = snapshot.getString("photoUrl")?.trim().orEmpty()
        val avatar = when {
            rawPhotoUrlField.isNotBlank() && !rawPhotoUrlField.contains("googleusercontent.com") -> rawPhotoUrlField
            rawAvatarField.isNotBlank() && !rawAvatarField.contains("googleusercontent.com") -> rawAvatarField
            rawPhotoUrlField.isNotBlank() -> rawPhotoUrlField
            rawAvatarField.isNotBlank() -> rawAvatarField
            else -> fallbackSession?.photoUrl.orEmpty()
        }.trim()
        val banner = (snapshot.getString("bannerUrl")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("banner")?.takeIf { it.isNotBlank() }
            ?: "").trim()
        val bio = snapshot.getString("bio")?.takeIf { it.isNotBlank() }
            ?: "Penikmat anime & komik di Yuranime."
        val role = snapshot.getString("role")?.takeIf { it.isNotBlank() } ?: "Member"
        val joinDate = extractJoinDateFromSnapshot(
            snapshot,
            if (allowCurrentAuthTimestampFallback) {
                try { auth.currentUser?.metadata?.creationTimestamp } catch (_: Exception) { null }
            } else null
        )

        val equippedTitle = snapshot.getString("equippedTitle")?.trim().orEmpty()
        val titleDef = resolveTitleDefinition(equippedTitle, computedLevel)
        val equippedTitleTier = readSnapshotLong(snapshot, "equippedTitleTier")?.toInt()
            ?: titleDef?.tierLevel
            ?: computedLevel
        val equippedTitleVariant = readSnapshotLong(snapshot, "equippedTitleVariant")?.toInt()
            ?: titleDef?.styleVariant
            ?: 0
        val equippedNameStyle = snapshot.getString("equippedNameStyle")?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val equippedNameColor = snapshot.getString("equippedNameColor")?.trim().orEmpty()
        val equippedBorderUrl = (snapshot.getString("equippedBorderUrl")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("equippedAvatarDecoration")?.takeIf { it.isNotBlank() }
            ?: "").trim()
        val equippedProfileEffectUrl = (snapshot.getString("equippedProfileEffectUrl")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("equippedProfileEffect")?.takeIf { it.isNotBlank() }
            ?: "").trim()
        val cosmeticAnimate = snapshot.getBoolean("cosmeticAnimate") ?: true
        val showMiniTitle = snapshot.getBoolean("showMiniTitle") ?: true
        val showCustomTitle = snapshot.getBoolean("showCustomTitle") ?: true
        val showRole = snapshot.getBoolean("showRole") ?: true

        val rawUnlockedTitles = (snapshot.get("unlockedTitles") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val autoMilestoneTitles = ALL_TITLES
            .filter { it.unlockType == TitleUnlockType.MILESTONE && computedLevel >= it.requiredLevel }
            .map { it.titleName }
        val unlockedTitles = (rawUnlockedTitles + autoMilestoneTitles).distinct()

        val unlockedNameStyles = (snapshot.get("unlockedNameStyles") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim()?.lowercase(Locale.ROOT) }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
            .distinct()

        val unlockedBorders = (snapshot.get("unlockedBorders") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val unlockedEffects = (snapshot.get("unlockedEffects") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val claimedRedeemCodes = (snapshot.get("claimedRedeemCodes") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim()?.uppercase(Locale.ROOT) }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val rawClaimedMissions = (snapshot.get("claimedMissions") as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val keyStr = k?.toString()
                val valStr = v?.toString()
                if (!keyStr.isNullOrBlank() && !valStr.isNullOrBlank()) keyStr to valStr else null
            }?.toMap()
            ?: emptyMap()

        val verifiedBool = snapshot.getBoolean("isVerified") ?: false
        val verifiedHex = snapshot.getString("verifiedColor")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: VERIFIED_BADGE_DEFAULT_HEX

        val rpgSummaryMap = snapshot.get("rpgSummary") as? Map<*, *>
        val rpgLevel = ((rpgSummaryMap?.get("level") as? Number)?.toInt() ?: 0).coerceAtLeast(0)
        val rpgCoins = ((rpgSummaryMap?.get("coins") as? Number)?.toInt() ?: 0).coerceAtLeast(0)
        val rpgHouseTier = ((rpgSummaryMap?.get("houseTier") as? Number)?.toInt() ?: 1).coerceIn(1, 3)

        return UserProfile(
            uid = uid,
            name = name,
            username = username,
            userTag = userTag,
            email = email,
            avatar = avatar,
            banner = banner,
            bio = bio,
            role = role,
            joinDate = joinDate,
            exp = exp,
            points = points,
            level = computedLevel,
            rankTitle = computeRankTitle(computedLevel),
            equippedTitle = equippedTitle,
            equippedTitleTier = equippedTitleTier,
            equippedTitleVariant = equippedTitleVariant,
            equippedNameStyle = equippedNameStyle,
            equippedNameColor = equippedNameColor,
            equippedBorderUrl = equippedBorderUrl,
            equippedProfileEffectUrl = equippedProfileEffectUrl,
            cosmeticAnimate = cosmeticAnimate,
            showMiniTitle = showMiniTitle,
            showCustomTitle = showCustomTitle,
            showRole = showRole,
            unlockedTitles = unlockedTitles,
            unlockedNameStyles = unlockedNameStyles,
            unlockedBorders = unlockedBorders,
            unlockedEffects = unlockedEffects,
            claimedRedeemCodes = claimedRedeemCodes,
            claimedMissions = rawClaimedMissions,
            lastCheckInDate = snapshot.getString("lastCheckInDate") ?: "",
            checkInStreak = (readSnapshotLong(snapshot, "checkInStreak") ?: 0L).toInt(),
            nextLevelTargetExp = nextTarget,
            expProgress = expProgress,
            isVerified = verifiedBool,
            verifiedColor = verifiedHex,
            rpgLevel = rpgLevel,
            rpgCoins = rpgCoins,
            rpgHouseTier = rpgHouseTier
        )
    }

    fun startListeningCurrentUser(context: Context? = null) {
        val uid = getActiveUid() ?: return
        userDocListener?.remove()

        if (context != null && isUserLoggedIn() && lastSyncedWatchHistoryUid != uid) {
            lastSyncedWatchHistoryUid = uid
            syncWatchHistoryFromCloud(context)
        }

        userDocListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listener profil error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = parseUserProfileFromSnapshot(
                        snapshot = snapshot,
                        fallbackUid = uid,
                        fallbackSession = _googleSession.value
                    )
                    _userProfile.value = profile
                    if (context != null) {
                        saveProfileToPrefs(context, profile)
                        AppPreferences.saveGoogleSession(
                            context = context,
                            id = profile.uid,
                            name = profile.name,
                            email = profile.email,
                            photoUrl = profile.avatar.ifBlank { _googleSession.value?.photoUrl }
                        )
                        syncLocalAuthDatabase(context, profile)
                    }
                    refreshLeaderboardWithCurrentUser(profile)
                }
            }
    }

    /**
     * URUTAN 1 (Poin 6): Bersihkan & gabungkan dokumen akun ganda di koleksi `users` Firestore
     * (contoh: duplikat saat ganti nama seperti Erinah -> jmk, DinzID -> Gading Kencana, atau
     * duplikat akibat perbedaan ID dokumen `g_email` vs Firebase Auth UID).
     * Menyimpan nama hasil edit terbaru, EXP tertinggi, Poin tertinggi, dan seluruh item kosmetik
     * ke 1 dokumen kanonik, lalu menghapus dokumen duplikat dari Firestore.
     */
    fun deduplicateUserAccounts(context: Context? = null) {
        scope.launch {
            try {
                val snapshot = firestore.collection("users").get().await()
                if (snapshot.isEmpty) return@launch

                val activeEmail = (
                    auth.currentUser?.email
                        ?: _googleSession.value?.email
                        ?: _userProfile.value?.email
                        ?: ""
                    ).trim().lowercase(Locale.ROOT)
                val activeFbUid = auth.currentUser?.uid.orEmpty()
                val activeCanonUid = getActiveUid().orEmpty()

                // Pertama, petakan email untuk setiap grup yang terhubung
                fun resolveIdentityGroup(doc: com.google.firebase.firestore.DocumentSnapshot): String {
                    val docId = doc.id
                    val rawEmail = doc.getString("email")?.trim()?.lowercase(Locale.ROOT).orEmpty()
                    val docName = (doc.getString("name") ?: doc.getString("displayName") ?: "")
                        .trim().lowercase(Locale.ROOT)
                    val docUser = (doc.getString("username") ?: "")
                        .trim().lowercase(Locale.ROOT).removePrefix("@")

                    // Pasangan akun duplikat yang dilaporkan pengguna
                    if (docName in setOf("erinah", "jmk") || docUser in setOf("erinah", "jmk")) {
                        return "legacy_pair_erinah_jmk"
                    }
                    if (docName in setOf("dinzid", "gading kencana", "gadingkencana") ||
                        docUser in setOf("dinzid", "gadingkencana", "gading_kencana")
                    ) {
                        return "legacy_pair_dinzid_gading"
                    }

                    if (rawEmail.isNotBlank()) {
                        return resolveCanonicalUserId(rawEmail, docId)
                    }
                    if (activeEmail.isNotBlank() && (docId == activeFbUid || docId == activeCanonUid)) {
                        return resolveCanonicalUserId(activeEmail, docId)
                    }
                    if (docId.startsWith("g_")) {
                        return docId
                    }
                    return "uid_$docId"
                }

                // Kedua, jika ada dokumen dengan email yang cocok dengan grup legacy, satukan grupnya
                val rawGroups = snapshot.documents.groupBy { resolveIdentityGroup(it) }.toMutableMap()

                // Satukan grup jika ada dokumen yang memiliki canonical ID dari email dokumen di legacy_pair
                val finalGroups = mutableMapOf<String, MutableList<com.google.firebase.firestore.DocumentSnapshot>>()
                for ((key, docs) in rawGroups) {
                    val groupEmail = docs.firstNotNullOfOrNull {
                        it.getString("email")?.trim()?.lowercase(Locale.ROOT)?.takeIf { e -> e.isNotBlank() }
                    } ?: if (docs.any { it.id == activeFbUid || it.id == activeCanonUid }) activeEmail else ""

                    val canonicalKey = if (groupEmail.isNotBlank()) {
                        resolveCanonicalUserId(groupEmail, key)
                    } else {
                        key
                    }
                    finalGroups.getOrPut(canonicalKey) { mutableListOf() }.addAll(docs)
                }

                var anyDeleted = false
                for ((groupKey, docs) in finalGroups) {
                    if (docs.size <= 1) continue

                    val groupEmail = docs.firstNotNullOfOrNull {
                        it.getString("email")?.trim()?.lowercase(Locale.ROOT)?.takeIf { e -> e.isNotBlank() }
                    } ?: if (docs.any { it.id == activeFbUid || it.id == activeCanonUid }) activeEmail else ""

                    val targetDocId = when {
                        groupEmail.isNotBlank() -> resolveCanonicalUserId(groupEmail, null)
                        docs.any { it.id == activeCanonUid && activeCanonUid.isNotBlank() } -> activeCanonUid
                        docs.any { it.id.startsWith("g_") } -> docs.first { it.id.startsWith("g_") }.id
                        else -> docs.maxByOrNull { maxOf(it.getLong("exp") ?: 0L, it.getLong("xp") ?: 0L) }?.id
                            ?: docs.first().id
                    }

                    // Pilih dokumen yang memiliki nama hasil edit terbaru (bukan nama default lama)
                    val preferredNameDoc = docs.sortedWith(
                        compareByDescending<com.google.firebase.firestore.DocumentSnapshot> { d ->
                            val n = (d.getString("name") ?: d.getString("displayName") ?: "").trim()
                            when {
                                n.equals("jmk", ignoreCase = true) -> 100
                                n.equals("Gading Kencana", ignoreCase = true) -> 100
                                n.equals("Erinah", ignoreCase = true) -> 10
                                n.equals("DinzID", ignoreCase = true) -> 10
                                d.id == activeCanonUid -> 80
                                else -> 50
                            }
                        }.thenByDescending { d ->
                            (d.getString("equippedBorderUrl")?.isNotBlank() == true) ||
                                (d.getString("equippedTitle")?.isNotBlank() == true)
                        }.thenByDescending { d ->
                            maxOf(d.getLong("exp") ?: 0L, d.getLong("xp") ?: 0L)
                        }
                    ).first()

                    val mergedName = (preferredNameDoc.getString("name")
                        ?: preferredNameDoc.getString("displayName")
                        ?: "User Yuranime").trim()
                    val mergedUsername = preferredNameDoc.getString("username")?.takeIf { it.isNotBlank() }
                        ?: "@${mergedName.lowercase(Locale.ROOT).replace(" ", "")}"
                    val mergedBio = docs.mapNotNull { it.getString("bio")?.trim() }
                        .firstOrNull { it.isNotBlank() && it != "Penikmat anime & komik di Yuranime." }
                        ?: preferredNameDoc.getString("bio")?.takeIf { it.isNotBlank() }
                        ?: "Penikmat anime & komik di Yuranime."
                    val mergedAvatar = (preferredNameDoc.getString("avatar")?.takeIf { it.isNotBlank() }
                        ?: preferredNameDoc.getString("photoUrl")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull {
                            (it.getString("avatar") ?: it.getString("photoUrl"))?.trim()?.takeIf { a -> a.isNotBlank() }
                        }).orEmpty()
                    val mergedBanner = (preferredNameDoc.getString("banner")?.takeIf { it.isNotBlank() }
                        ?: preferredNameDoc.getString("bannerUrl")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull {
                            (it.getString("banner") ?: it.getString("bannerUrl"))?.trim()?.takeIf { b -> b.isNotBlank() }
                        }).orEmpty()

                    val mergedExp = docs.maxOfOrNull { maxOf(it.getLong("exp") ?: 0L, it.getLong("xp") ?: 0L) } ?: 0L
                    val mergedPoints = docs.maxOfOrNull { it.getLong("points") ?: 0L } ?: 0L
                    val mergedLevel = computeLevelFromExp(mergedExp)
                    val mergedRole = docs.mapNotNull { it.getString("role")?.trim() }
                        .firstOrNull { it.isNotBlank() && !it.equals("Member", ignoreCase = true) }
                        ?: "Member"
                    val mergedTitle = (preferredNameDoc.getString("equippedTitle")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("equippedTitle")?.trim()?.takeIf { t -> t.isNotBlank() } })
                        .orEmpty()
                    val titleDef = resolveTitleDefinition(mergedTitle, mergedLevel)
                    val mergedTitleTier = (preferredNameDoc.getLong("equippedTitleTier")?.toInt())
                        ?: titleDef?.tierLevel
                        ?: mergedLevel
                    val mergedTitleVariant = (preferredNameDoc.getLong("equippedTitleVariant")?.toInt())
                        ?: titleDef?.styleVariant
                        ?: 0
                    val mergedNameStyle = (preferredNameDoc.getString("equippedNameStyle")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("equippedNameStyle")?.trim()?.takeIf { s -> s.isNotBlank() } })
                        .orEmpty()
                    val mergedNameColor = (preferredNameDoc.getString("equippedNameColor")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("equippedNameColor")?.trim()?.takeIf { c -> c.isNotBlank() } })
                        .orEmpty()
                    val mergedBorder = (preferredNameDoc.getString("equippedBorderUrl")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("equippedBorderUrl")?.trim()?.takeIf { b -> b.isNotBlank() } })
                        .orEmpty()
                    val mergedEffect = (preferredNameDoc.getString("equippedProfileEffectUrl")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("equippedProfileEffectUrl")?.trim()?.takeIf { e -> e.isNotBlank() } })
                        .orEmpty()

                    val mergedUnlockedTitles = docs.flatMap {
                        (it.get("unlockedTitles") as? List<*>)?.mapNotNull { x -> x?.toString() } ?: emptyList()
                    }.distinct()
                    val mergedUnlockedNameStyles = docs.flatMap {
                        (it.get("unlockedNameStyles") as? List<*>)?.mapNotNull { x -> x?.toString()?.lowercase(Locale.ROOT) }
                            ?: emptyList()
                    }.distinct()
                    val mergedUnlockedBorders = docs.flatMap {
                        (it.get("unlockedBorders") as? List<*>)?.mapNotNull { x -> x?.toString() } ?: emptyList()
                    }.distinct()
                    val mergedUnlockedEffects = docs.flatMap {
                        (it.get("unlockedEffects") as? List<*>)?.mapNotNull { x -> x?.toString() } ?: emptyList()
                    }.distinct()
                    val mergedClaimedCodes = docs.flatMap {
                        (it.get("claimedRedeemCodes") as? List<*>)?.mapNotNull { x -> x?.toString() } ?: emptyList()
                    }.distinct()
                    val mergedClaimedMissions = docs.flatMap {
                        (it.get("claimedMissions") as? List<*>)?.mapNotNull { x -> x?.toString() } ?: emptyList()
                    }.distinct()

                    val mergedVerified = docs.any { it.getBoolean("isVerified") == true }
                    val mergedVerifiedColor = (preferredNameDoc.getString("verifiedColor")?.takeIf { it.isNotBlank() }
                        ?: docs.firstNotNullOfOrNull { it.getString("verifiedColor")?.trim()?.takeIf { c -> c.isNotBlank() } }
                        ?: VERIFIED_BADGE_DEFAULT_HEX)

                    val mergedPayload = mutableMapOf<String, Any>(
                        "uid" to targetDocId,
                        "name" to mergedName,
                        "username" to mergedUsername,
                        "userTag" to computeUserTag(targetDocId, preferredNameDoc.getString("userTag")),
                        "bio" to mergedBio,
                        "avatar" to mergedAvatar,
                        "photoUrl" to mergedAvatar,
                        "banner" to mergedBanner,
                        "bannerUrl" to mergedBanner,
                        "exp" to mergedExp,
                        "points" to mergedPoints,
                        "level" to mergedLevel,
                        "role" to mergedRole,
                        "isVerified" to mergedVerified,
                        "verifiedColor" to mergedVerifiedColor,
                        "equippedTitle" to mergedTitle,
                        "equippedTitleTier" to mergedTitleTier,
                        "equippedTitleVariant" to mergedTitleVariant,
                        "equippedNameStyle" to mergedNameStyle,
                        "equippedNameColor" to mergedNameColor,
                        "equippedBorderUrl" to mergedBorder,
                        "equippedProfileEffectUrl" to mergedEffect,
                        "unlockedTitles" to mergedUnlockedTitles,
                        "unlockedNameStyles" to mergedUnlockedNameStyles,
                        "unlockedBorders" to mergedUnlockedBorders,
                        "unlockedEffects" to mergedUnlockedEffects,
                        "claimedRedeemCodes" to mergedClaimedCodes,
                        "claimedMissions" to mergedClaimedMissions
                    )
                    if (groupEmail.isNotBlank()) {
                        mergedPayload["email"] = groupEmail
                    }

                    firestore.collection("users").document(targetDocId)
                        .set(mergedPayload, SetOptions.merge())
                        .await()

                    for (doc in docs) {
                        if (doc.id != targetDocId) {
                            try {
                                firestore.collection("users").document(doc.id).delete().await()
                                anyDeleted = true
                            } catch (e: Exception) {
                                Log.w(TAG, "Gagal menghapus dokumen duplikat ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }

                if (anyDeleted && context != null) {
                    startListeningCurrentUser(context)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Deduplikasi akun dilewati sementara: ${e.message}")
            }
        }
    }

    /**
     * TAHAP 3: Query Papan Peringkat 100% MURNI dari koleksi `users` di Firebase Firestore
     * diurutkan berdasarkan XP/EXP tertinggi (limit 100), TANPA bot atau placeholder palsu,
     * serta otomatis menggabungkan entri duplikat agar 1 akun hanya tampil 1 kali!
     */
    fun startListeningLeaderboard() {
        leaderboardListener?.remove()
        _isLeaderboardLoading.value = true
        deduplicateUserAccounts()

        leaderboardListener = firestore.collection("users")
            .limit(150)
            .addSnapshotListener { querySnapshot, error ->
                _isLeaderboardLoading.value = false
                if (error != null) {
                    Log.w(TAG, "Leaderboard listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val activeEmail = (
                        auth.currentUser?.email
                            ?: _googleSession.value?.email
                            ?: _userProfile.value?.email
                            ?: ""
                        ).trim().lowercase(Locale.ROOT)
                    val activeFbUid = auth.currentUser?.uid.orEmpty()
                    val activeCanonUid = getActiveUid().orEmpty()

                    data class RawEntry(
                        val identityKey: String,
                        val priorityScore: Int,
                        val user: LeaderboardUser
                    )

                    val rawEntries = querySnapshot.documents.mapNotNull { doc ->
                        val uid = doc.getString("uid")?.takeIf { it.isNotBlank() } ?: doc.id
                        val name = doc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("username")?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null

                        val exp = maxOf(doc.getLong("exp") ?: 0L, doc.getLong("xp") ?: 0L)
                        val points = doc.getLong("points") ?: 0L
                        val level = computeLevelFromExp(exp)
                        val username = doc.getString("username")?.takeIf { it.isNotBlank() }
                            ?: "@${name.lowercase(Locale.ROOT).replace(" ", "")}"
                        val avatar = (doc.getString("avatar")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("photoUrl")?.takeIf { it.isNotBlank() }
                            ?: "").trim()
                        val role = doc.getString("role")?.takeIf { it.isNotBlank() } ?: "Member"
                        val equippedTitle = doc.getString("equippedTitle")?.trim().orEmpty()
                        val titleDef = resolveTitleDefinition(equippedTitle, level)
                        val equippedTitleTier = (doc.getLong("equippedTitleTier")?.toInt())
                            ?: titleDef?.tierLevel
                            ?: level
                        val equippedTitleVariant = (doc.getLong("equippedTitleVariant")?.toInt())
                            ?: titleDef?.styleVariant
                            ?: 0
                        val equippedNameStyle = doc.getString("equippedNameStyle")?.trim()?.lowercase(Locale.ROOT).orEmpty()
                        val equippedNameColor = doc.getString("equippedNameColor")?.trim().orEmpty()
                        val equippedBorderUrl = doc.getString("equippedBorderUrl")?.trim().orEmpty()
                        val equippedProfileEffectUrl = doc.getString("equippedProfileEffectUrl")?.trim().orEmpty()
                        val isVerified = doc.getBoolean("isVerified") ?: false
                        val verifiedColor = doc.getString("verifiedColor")?.trim()?.takeIf { it.isNotBlank() }
                            ?: VERIFIED_BADGE_DEFAULT_HEX

                        val rawEmail = doc.getString("email")?.trim()?.lowercase(Locale.ROOT).orEmpty()
                        val lowerName = name.trim().lowercase(Locale.ROOT)
                        val lowerUser = username.trim().lowercase(Locale.ROOT).removePrefix("@")

                        val identityKey = when {
                            lowerName in setOf("erinah", "jmk") || lowerUser in setOf("erinah", "jmk") ->
                                "pair_erinah_jmk"
                            lowerName in setOf("dinzid", "gading kencana", "gadingkencana") ||
                                lowerUser in setOf("dinzid", "gadingkencana", "gading_kencana") ->
                                "pair_dinzid_gading"
                            rawEmail.isNotBlank() -> resolveCanonicalUserId(rawEmail, uid)
                            activeEmail.isNotBlank() && (doc.id == activeFbUid || doc.id == activeCanonUid || uid == activeCanonUid) ->
                                resolveCanonicalUserId(activeEmail, uid)
                            else -> uid
                        }

                        val priorityScore = when {
                            name.equals("jmk", ignoreCase = true) -> 100
                            name.equals("Gading Kencana", ignoreCase = true) -> 100
                            name.equals("Erinah", ignoreCase = true) -> 10
                            name.equals("DinzID", ignoreCase = true) -> 10
                            doc.id == activeCanonUid || uid == activeCanonUid -> 80
                            else -> 50
                        }

                        RawEntry(
                            identityKey = identityKey,
                            priorityScore = priorityScore,
                            user = LeaderboardUser(
                                rank = 0,
                                uid = if (doc.id == activeFbUid && activeCanonUid.isNotBlank()) activeCanonUid else uid,
                                name = name,
                                username = username,
                                userTag = computeUserTag(uid, doc.getString("userTag")),
                                avatar = avatar,
                                exp = exp,
                                points = points,
                                level = level,
                                role = role,
                                rankTitle = computeRankTitle(level),
                                equippedTitle = equippedTitle,
                                equippedTitleTier = equippedTitleTier,
                                equippedTitleVariant = equippedTitleVariant,
                                equippedNameStyle = equippedNameStyle,
                                equippedNameColor = equippedNameColor,
                                equippedBorderUrl = equippedBorderUrl,
                                equippedProfileEffectUrl = equippedProfileEffectUrl,
                                isVerified = isVerified,
                                verifiedColor = verifiedColor
                            )
                        )
                    }

                    val deduplicatedUsers = rawEntries
                        .groupBy { it.identityKey }
                        .values
                        .mapNotNull { group ->
                            val primary = group.maxWithOrNull(
                                compareBy<RawEntry> { it.priorityScore }
                                    .thenBy { it.user.equippedBorderUrl.isNotBlank() || it.user.equippedTitle.isNotBlank() }
                                    .thenBy { it.user.exp }
                            )?.user ?: return@mapNotNull null
                            val maxExp = group.maxOf { it.user.exp }
                            val maxPoints = group.maxOf { it.user.points }
                            val bestLevel = computeLevelFromExp(maxExp)
                            val bestBorder = primary.equippedBorderUrl.ifBlank {
                                group.firstNotNullOfOrNull { it.user.equippedBorderUrl.takeIf { b -> b.isNotBlank() } }.orEmpty()
                            }
                            val bestEffect = primary.equippedProfileEffectUrl.ifBlank {
                                group.firstNotNullOfOrNull { it.user.equippedProfileEffectUrl.takeIf { e -> e.isNotBlank() } }.orEmpty()
                            }
                            val bestTitle = primary.equippedTitle.ifBlank {
                                group.firstNotNullOfOrNull { it.user.equippedTitle.takeIf { t -> t.isNotBlank() } }.orEmpty()
                            }
                            val bestNameStyle = primary.equippedNameStyle.ifBlank {
                                group.firstNotNullOfOrNull { it.user.equippedNameStyle.takeIf { s -> s.isNotBlank() } }.orEmpty()
                            }
                            val bestNameColor = primary.equippedNameColor.ifBlank {
                                group.firstNotNullOfOrNull { it.user.equippedNameColor.takeIf { c -> c.isNotBlank() } }.orEmpty()
                            }
                            val bestVerified = group.any { it.user.isVerified }
                            val bestVerifiedColor = primary.verifiedColor.takeIf { it.isNotBlank() }
                                ?: group.firstNotNullOfOrNull { it.user.verifiedColor.takeIf { c -> c.isNotBlank() } }
                                ?: VERIFIED_BADGE_DEFAULT_HEX
                            primary.copy(
                                exp = maxExp,
                                points = maxPoints,
                                level = bestLevel,
                                rankTitle = computeRankTitle(bestLevel),
                                equippedBorderUrl = bestBorder,
                                equippedProfileEffectUrl = bestEffect,
                                equippedTitle = bestTitle,
                                equippedNameStyle = bestNameStyle,
                                equippedNameColor = bestNameColor,
                                isVerified = bestVerified,
                                verifiedColor = bestVerifiedColor
                            )
                        }
                        .sortedWith(
                            compareByDescending<LeaderboardUser> { it.exp }
                                .thenByDescending { it.points }
                                .thenBy { it.name.lowercase(Locale.ROOT) }
                        )
                        .take(100)
                        .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

                    _leaderboard.value = deduplicatedUsers
                }
            }
    }

    private fun refreshLeaderboardWithCurrentUser(profile: UserProfile) {
        val currentList = _leaderboard.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.uid == profile.uid }
        val updatedEntry = LeaderboardUser(
            rank = 0,
            uid = profile.uid,
            name = profile.name,
            username = profile.username,
            userTag = profile.userTag,
            avatar = profile.avatar,
            exp = profile.exp,
            points = profile.points,
            level = profile.level,
            role = profile.role,
            rankTitle = profile.rankTitle,
            equippedTitle = profile.equippedTitle,
            equippedTitleTier = profile.equippedTitleTier,
            equippedTitleVariant = profile.equippedTitleVariant,
            equippedNameStyle = profile.equippedNameStyle,
            equippedNameColor = profile.equippedNameColor,
            equippedBorderUrl = profile.equippedBorderUrl,
            equippedProfileEffectUrl = profile.equippedProfileEffectUrl,
            isVerified = profile.isVerified,
            verifiedColor = profile.verifiedColor
        )
        if (existingIdx >= 0) {
            currentList[existingIdx] = updatedEntry
        } else if (profile.uid.isNotBlank()) {
            currentList.add(updatedEntry)
        }
        val resorted = currentList
            .sortedWith(
                compareByDescending<LeaderboardUser> { it.exp }
                    .thenByDescending { it.points }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            .take(100)
            .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
        _leaderboard.value = resorted
    }

    /**
     * TAHAP 1 & 4: Update Profil Lengkap (termasuk Upload Avatar & Banner WebP ke Cloudflare R2 / Dinz CDN
     * serta pemilihan Custom Title) dan sinkronisasi real-time ke koleksi `users/{uid}` di Firestore.
     */
    fun updateProfile(
        context: Context? = null,
        name: String,
        username: String = "",
        bio: String,
        avatarUrl: String,
        bannerUrl: String,
        equippedTitle: String? = null,
        equippedBorderUrl: String? = null,
        equippedProfileEffectUrl: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }

        scope.launch {
            try {
                var finalAvatarUrl = avatarUrl.trim()
                var finalBannerUrl = bannerUrl.trim()

                if (context != null) {
                    if (finalAvatarUrl.isNotEmpty() && !finalAvatarUrl.startsWith("http")) {
                        val uploadRes = R2CdnUploader.compressAndUploadImage(
                            context = context,
                            uriString = finalAvatarUrl,
                            uid = uid,
                            isBanner = false
                        )
                        uploadRes.onSuccess { cdnUrl ->
                            finalAvatarUrl = cdnUrl
                        }.onFailure { err ->
                            CoroutineScope(Dispatchers.Main).launch {
                                onComplete(false, "Gagal mengunggah foto profil ke CDN: ${err.message}")
                            }
                            return@launch
                        }
                    }

                    if (finalBannerUrl.isNotEmpty() && !finalBannerUrl.startsWith("http")) {
                        val uploadRes = R2CdnUploader.compressAndUploadImage(
                            context = context,
                            uriString = finalBannerUrl,
                            uid = uid,
                            isBanner = true
                        )
                        uploadRes.onSuccess { cdnUrl ->
                            finalBannerUrl = cdnUrl
                        }.onFailure { err ->
                            CoroutineScope(Dispatchers.Main).launch {
                                onComplete(false, "Gagal mengunggah cover banner ke CDN: ${err.message}")
                            }
                            return@launch
                        }
                    }
                }

                val current = _userProfile.value ?: UserProfile(uid = uid)
                val cleanName = name.trim().ifBlank { current.name }
                val cleanUsername = username.trim().lowercase(Locale.ROOT).replace(" ", "")
                    .removePrefix("@")
                    .ifBlank { current.username.removePrefix("@") }
                val cleanBio = bio.trim().ifBlank { "Penikmat anime & komik di Yuranime." }
                val finalTitle = equippedTitle?.trim() ?: current.equippedTitle
                val titleDef = resolveTitleDefinition(finalTitle, current.level)
                val finalTitleTier = titleDef?.tierLevel ?: current.level
                val finalTitleVariant = titleDef?.styleVariant ?: 0
                val finalBorder = equippedBorderUrl?.trim() ?: current.equippedBorderUrl
                val finalEffect = equippedProfileEffectUrl?.trim() ?: current.equippedProfileEffectUrl

                val updated = current.copy(
                    name = cleanName,
                    username = cleanUsername,
                    bio = cleanBio,
                    avatar = finalAvatarUrl,
                    banner = finalBannerUrl,
                    equippedTitle = finalTitle,
                    equippedTitleTier = finalTitleTier,
                    equippedTitleVariant = finalTitleVariant,
                    equippedBorderUrl = finalBorder,
                    equippedProfileEffectUrl = finalEffect
                )
                _userProfile.value = updated
                if (context != null) {
                    saveProfileToPrefs(context, updated)
                    AppPreferences.saveGoogleSession(
                        context = context,
                        id = uid,
                        name = updated.name,
                        email = updated.email,
                        photoUrl = updated.avatar
                    )
                }
                refreshLeaderboardWithCurrentUser(updated)

                try {
                    auth.currentUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(updated.name)
                            .build()
                    )
                } catch (_: Exception) {}

                val effectiveEmail = updated.email.ifBlank {
                    auth.currentUser?.email ?: _googleSession.value?.email ?: ""
                }
                val updates = mapOf(
                    "uid" to uid,
                    "email" to effectiveEmail,
                    "name" to updated.name,
                    "displayName" to updated.name,
                    "username" to updated.username,
                    "userTag" to updated.userTag,
                    "bio" to updated.bio,
                    "avatar" to updated.avatar,
                    "photoUrl" to updated.avatar,
                    "banner" to updated.banner,
                    "bannerUrl" to updated.banner,
                    "equippedTitle" to updated.equippedTitle,
                    "equippedTitleTier" to updated.equippedTitleTier,
                    "equippedTitleVariant" to updated.equippedTitleVariant,
                    "equippedNameStyle" to updated.equippedNameStyle,
                    "equippedNameColor" to updated.equippedNameColor,
                    "unlockedNameStyles" to updated.unlockedNameStyles,
                    "equippedBorderUrl" to updated.equippedBorderUrl,
                    "equippedAvatarDecoration" to updated.equippedBorderUrl,
                    "equippedProfileEffectUrl" to updated.equippedProfileEffectUrl,
                    "equippedProfileEffect" to updated.equippedProfileEffectUrl
                )

                try {
                    firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
                    deduplicateUserAccounts(context)
                    CoroutineScope(Dispatchers.Main).launch {
                        onComplete(true, "Profil berhasil disimpan & disinkronkan ke server!")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Update profil tersimpan lokal, menunggu koneksi Firestore: ${e.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        onComplete(true, "Profil berhasil diperbarui!")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error tak terduga saat updateProfile: ${t.message}", t)
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(false, t.message ?: "Terjadi kesalahan saat menyimpan profil")
                }
            }
        }
    }

    fun equipTitle(
        context: Context? = null,
        titleDefinition: TitleDefinition?,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }
        val newTitleName = titleDefinition?.titleName.orEmpty()
        val newTier = titleDefinition?.tierLevel ?: current.level
        val newVariant = titleDefinition?.styleVariant ?: 0

        val updated = current.copy(
            equippedTitle = newTitleName,
            equippedTitleTier = newTier,
            equippedTitleVariant = newVariant
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "equippedTitle" to newTitleName,
                        "equippedTitleTier" to newTier,
                        "equippedTitleVariant" to newVariant
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    val msg = if (newTitleName.isBlank()) {
                        "Title dilepas dari profil"
                    } else {
                        "Title \"$newTitleName\" berhasil dipasang!"
                    }
                    onComplete(true, msg)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Title berhasil dipasang!")
                }
            }
        }
    }

    fun unlockAndEquipShopTitle(
        context: Context? = null,
        titleDefinition: TitleDefinition,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }

        if (current.unlockedTitles.any { it.equals(titleDefinition.titleName, ignoreCase = true) }) {
            equipTitle(context, titleDefinition, onComplete)
            return
        }

        if (titleDefinition.unlockType == TitleUnlockType.REDEEM_ONLY) {
            onComplete(false, "Title eksklusif ini hanya bisa dibuka lewat Kode Redeem atau Event!")
            return
        }

        if (titleDefinition.unlockType == TitleUnlockType.MILESTONE) {
            if (current.level < titleDefinition.requiredLevel) {
                onComplete(false, "Capai Level ${titleDefinition.requiredLevel} untuk membuka Title ini!")
            } else {
                equipTitle(context, titleDefinition, onComplete)
            }
            return
        }

        if (current.level < titleDefinition.requiredLevel) {
            onComplete(false, "Butuh minimal Level ${titleDefinition.requiredLevel} untuk membeli Title ini!")
            return
        }

        if (current.points < titleDefinition.pointCost) {
            onComplete(false, "Poin belum cukup! Butuh ${titleDefinition.pointCost} Poin.")
            return
        }

        val newPoints = (current.points - titleDefinition.pointCost).coerceAtLeast(0L)
        val newUnlocked = (current.unlockedTitles + titleDefinition.titleName).distinct()
        val updated = current.copy(
            points = newPoints,
            unlockedTitles = newUnlocked,
            equippedTitle = titleDefinition.titleName,
            equippedTitleTier = titleDefinition.tierLevel,
            equippedTitleVariant = titleDefinition.styleVariant
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "points" to newPoints,
                        "unlockedTitles" to newUnlocked,
                        "equippedTitle" to titleDefinition.titleName,
                        "equippedTitleTier" to titleDefinition.tierLevel,
                        "equippedTitleVariant" to titleDefinition.styleVariant
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Title \"${titleDefinition.titleName}\" berhasil ditukar & dipasang!")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Title \"${titleDefinition.titleName}\" berhasil dipasang!")
                }
            }
        }
    }

    /**
     * Memasang atau melepas Style Nama (`equippedNameStyle`) beserta warna pilihan bebas (`equippedNameColor`)
     * dan menyinkronkannya real-time ke Firestore sehingga langsung terlihat di Chat Global, Profil, & Leaderboard.
     */
    fun equipNameStyle(
        context: Context? = null,
        styleId: String,
        colorHex: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }
        val cleanId = styleId.trim().lowercase(Locale.ROOT)
        val cleanColor = colorHex.trim().ifBlank { "#FFFFFF" }

        if (cleanId.isNotEmpty() && !current.unlockedNameStyles.any { it.equals(cleanId, ignoreCase = true) }) {
            onComplete(false, "Beli Style Nama ini terlebih dahulu sebelum memasang.")
            return
        }

        val updated = current.copy(
            equippedNameStyle = cleanId,
            equippedNameColor = cleanColor
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        val def = resolveNameStyleDefinition(cleanId)
        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "equippedNameStyle" to cleanId,
                        "equippedNameColor" to cleanColor
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    val msg = if (cleanId.isBlank()) {
                        "Style Nama dilepas dari profil"
                    } else {
                        "Style Nama \"${def?.name ?: cleanId}\" berhasil dipasang!"
                    }
                    onComplete(true, msg)
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    val msg = if (cleanId.isBlank()) {
                        "Style Nama dilepas dari profil"
                    } else {
                        "Style Nama \"${def?.name ?: cleanId}\" berhasil dipasang!"
                    }
                    onComplete(true, msg)
                }
            }
        }
    }

    /**
     * Membeli Style Nama menggunakan Primo Poin (Mythic 4.500 / Legendary 2.000), langsung membuka kunci,
     * dan memasangnya beserta warna yang dipilih ke akun pengguna.
     */
    fun purchaseAndEquipNameStyle(
        context: Context? = null,
        styleDefinition: NameStyleDefinition,
        colorHex: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }
        val cleanColor = colorHex.trim().ifBlank { styleDefinition.defaultColorHex }

        if (current.unlockedNameStyles.any { it.equals(styleDefinition.id, ignoreCase = true) }) {
            equipNameStyle(context, styleDefinition.id, cleanColor, onComplete)
            return
        }

        if (current.points < styleDefinition.pointCost) {
            onComplete(false, "Poin belum cukup! Butuh ${formatNumber(styleDefinition.pointCost)} Poin.")
            return
        }

        val newPoints = (current.points - styleDefinition.pointCost).coerceAtLeast(0L)
        val newUnlocked = (current.unlockedNameStyles + styleDefinition.id).distinct()
        val updated = current.copy(
            points = newPoints,
            unlockedNameStyles = newUnlocked,
            equippedNameStyle = styleDefinition.id,
            equippedNameColor = cleanColor
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "points" to newPoints,
                        "unlockedNameStyles" to newUnlocked,
                        "equippedNameStyle" to styleDefinition.id,
                        "equippedNameColor" to cleanColor
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Style Nama \"${styleDefinition.name}\" berhasil dibeli & dipasang!")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Style Nama \"${styleDefinition.name}\" berhasil dipasang!")
                }
            }
        }
    }

    fun unlockOrEquipBorder(
        context: Context? = null,
        border: BorderDefinition?,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }

        if (border == null) {
            val updated = current.copy(equippedBorderUrl = "")
            _userProfile.value = updated
            if (context != null) saveProfileToPrefs(context, updated)
            refreshLeaderboardWithCurrentUser(updated)
            scope.launch {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf("equippedBorderUrl" to ""),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Frame border avatar dilepas")
                }
            }
            return
        }

        val alreadyUnlocked = current.unlockedBorders.contains(border.id)
        if (border.tier == "B" && current.level < border.requiredLevel && !alreadyUnlocked) {
            onComplete(false, "Frame ini butuh Level ${border.requiredLevel}! Tingkatkan level Anda terlebih dahulu.")
            return
        }
        if (border.tier == "C" && !alreadyUnlocked) {
            onComplete(false, "Item Limited ini hanya bisa didapatkan melalui Kode Redeem!")
            return
        }
        if (!alreadyUnlocked && current.points < border.pointCost) {
            onComplete(false, "Poin belum cukup! Butuh ${border.pointCost} Poin.")
            return
        }

        val newPoints = if (alreadyUnlocked) current.points else (current.points - border.pointCost).coerceAtLeast(0L)
        val newUnlockedBorders = (current.unlockedBorders + border.id).distinct()
        val updated = current.copy(
            points = newPoints,
            unlockedBorders = newUnlockedBorders,
            equippedBorderUrl = border.imageUrl
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "points" to newPoints,
                        "unlockedBorders" to newUnlockedBorders,
                        "equippedBorderUrl" to border.imageUrl
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Frame \"${border.name}\" berhasil dipasang!")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Frame \"${border.name}\" berhasil dipasang!")
                }
            }
        }
    }

    fun unlockOrEquipProfileEffect(
        context: Context? = null,
        effect: ProfileEffectDefinition?,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }

        if (effect == null) {
            val updated = current.copy(equippedProfileEffectUrl = "")
            _userProfile.value = updated
            if (context != null) saveProfileToPrefs(context, updated)
            refreshLeaderboardWithCurrentUser(updated)
            scope.launch {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf("equippedProfileEffectUrl" to ""),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Profile Effect dilepas")
                }
            }
            return
        }

        val alreadyUnlocked = current.unlockedEffects.contains(effect.id)
        if (effect.tier == "B" && current.level < effect.requiredLevel && !alreadyUnlocked) {
            onComplete(false, "Efek ini butuh Level ${effect.requiredLevel}! Tingkatkan level Anda terlebih dahulu.")
            return
        }
        if (effect.tier == "C" && !alreadyUnlocked) {
            onComplete(false, "Efek Limited ini hanya bisa didapatkan melalui Kode Redeem!")
            return
        }
        if (!alreadyUnlocked && current.points < effect.pointCost) {
            onComplete(false, "Poin belum cukup! Butuh ${effect.pointCost} Poin.")
            return
        }

        val newPoints = if (alreadyUnlocked) current.points else (current.points - effect.pointCost).coerceAtLeast(0L)
        val newUnlockedEffects = (current.unlockedEffects + effect.id).distinct()
        val updated = current.copy(
            points = newPoints,
            unlockedEffects = newUnlockedEffects,
            equippedProfileEffectUrl = effect.animationUrl
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "points" to newPoints,
                        "unlockedEffects" to newUnlockedEffects,
                        "equippedProfileEffectUrl" to effect.animationUrl
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Profile Effect \"${effect.name}\" berhasil dipasang!")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Profile Effect \"${effect.name}\" berhasil dipasang!")
                }
            }
        }
    }

    fun setCosmeticAnimate(
        context: Context? = null,
        enabled: Boolean,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (context != null) AppPreferences.setCosmeticAnimatePref(context, enabled)
        val uid = getActiveUid()
        val current = _userProfile.value
        if (current == null) {
            onComplete?.invoke(true)
            return
        }
        val updated = current.copy(cosmeticAnimate = enabled)
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        if (uid != null) {
            scope.launch {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf("cosmeticAnimate" to enabled),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}
                onComplete?.let { cb ->
                    CoroutineScope(Dispatchers.Main).launch { cb(true) }
                }
            }
        } else {
            onComplete?.invoke(true)
        }
    }

    fun setDisplayVisibility(
        context: Context? = null,
        showMiniTitle: Boolean? = null,
        showCustomTitle: Boolean? = null,
        showRole: Boolean? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (context != null) {
            if (showMiniTitle != null) AppPreferences.setShowMiniTitle(context, showMiniTitle)
            if (showCustomTitle != null) AppPreferences.setShowCustomTitle(context, showCustomTitle)
            if (showRole != null) AppPreferences.setShowRole(context, showRole)
        }
        val uid = getActiveUid()
        val current = _userProfile.value
        if (current == null) {
            onComplete?.invoke(true)
            return
        }
        val resolvedMini = showMiniTitle ?: current.showMiniTitle
        val resolvedCustom = showCustomTitle ?: current.showCustomTitle
        val resolvedRole = showRole ?: current.showRole
        val updated = current.copy(
            showMiniTitle = resolvedMini,
            showCustomTitle = resolvedCustom,
            showRole = resolvedRole
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        if (uid != null) {
            val patch = mutableMapOf<String, Any>()
            if (showMiniTitle != null) patch["showMiniTitle"] = resolvedMini
            if (showCustomTitle != null) patch["showCustomTitle"] = resolvedCustom
            if (showRole != null) patch["showRole"] = resolvedRole
            if (patch.isNotEmpty()) {
                scope.launch {
                    try {
                        firestore.collection("users").document(uid).set(
                            patch,
                            SetOptions.merge()
                        ).await()
                    } catch (_: Exception) {}
                    onComplete?.let { cb ->
                        CoroutineScope(Dispatchers.Main).launch { cb(true) }
                    }
                }
            } else {
                onComplete?.invoke(true)
            }
        } else {
            onComplete?.invoke(true)
        }
    }

    fun claimRedeemCode(
        context: Context? = null,
        codeInput: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: run {
            onComplete(false, "Profil pengguna belum dimuat, silakan coba lagi")
            return
        }
        val cleanCode = codeInput.trim().uppercase(Locale.ROOT)
        if (cleanCode.isBlank()) {
            onComplete(false, "Masukkan kode redeem yang valid")
            return
        }

        if (current.claimedRedeemCodes.contains(cleanCode)) {
            onComplete(false, "Kode \"$cleanCode\" sudah pernah Anda klaim di akun ini!")
            return
        }

        class CodeReward(
            val exp: Long,
            val points: Long,
            val title: String? = null,
            val borderId: String? = null,
            val effectId: String? = null,
            val message: String
        )

        val builtInRewards = mapOf(
            "YURANIMEVIP" to CodeReward(
                exp = 10000L,
                points = 5000L,
                title = "VIP YURANIME",
                borderId = "border_sovereign_gold",
                effectId = "effect_magic_hearts_gold",
                message = "Selamat! Kode VIP Yuranime aktif: +10,000 EXP, +5,000 Poin, & Kosmetik Emas!"
            ),
            "DINZMYTHIC" to CodeReward(
                exp = 25000L,
                points = 15000L,
                title = "MYTHIC SOVEREIGN",
                borderId = "border_mythic_emperor",
                effectId = "effect_mythic_aurora",
                message = "Luar biasa! Paket Mythic Sovereign aktif: +25,000 EXP, +15,000 Poin & Mahkota Mythic!"
            ),
            "SEPUH2026" to CodeReward(
                exp = 5000L,
                points = 2500L,
                title = "SEPUH YURANIME",
                borderId = "border_bronze_crest",
                message = "Kode Sepuh 2026 aktif: +5,000 EXP, +2,500 Poin & Title Sepuh!"
            ),
            "SOVEREIGN99" to CodeReward(
                exp = 15000L,
                points = 8000L,
                title = "KHALIFAH ANIME",
                borderId = "border_silver_wings",
                message = "Kode Sovereign aktif: +15,000 EXP, +8,000 Poin & Sayap Perak!"
            ),
            "ANIMEKING" to CodeReward(
                exp = 8000L,
                points = 4000L,
                title = "BANGSAWAN KOMIK",
                effectId = "effect_cosmic_twilight",
                message = "Kode Raja Anime aktif: +8,000 EXP, +4,000 Poin & Efek Cosmic Twilight!"
            ),
            "VENOMSYMBIOTE" to CodeReward(
                exp = 12000L,
                points = 6000L,
                borderId = "border_venom_logo",
                effectId = "effect_venom_symbiote",
                message = "We Are Venom! +12,000 EXP, +6,000 Poin, Avatar Border & Profile Effect Venom didapatkan!"
            )
        )

        fun applyReward(reward: CodeReward) {
            val newExp = current.exp + reward.exp
            val newPoints = current.points + reward.points
            val newLevel = computeLevelFromExp(newExp)
            val nextTarget = computeNextLevelTargetExp(newLevel)
            val newProgress = computeExpProgress(newExp, nextTarget)

            val newUnlockedTitles = (current.unlockedTitles + listOfNotNull(reward.title)).distinct()
            val newUnlockedBorders = (current.unlockedBorders + listOfNotNull(reward.borderId)).distinct()
            val newUnlockedEffects = (current.unlockedEffects + listOfNotNull(reward.effectId)).distinct()
            val newClaimedCodes = (current.claimedRedeemCodes + cleanCode).distinct()

            val updated = current.copy(
                exp = newExp,
                points = newPoints,
                level = newLevel,
                rankTitle = computeRankTitle(newLevel),
                unlockedTitles = newUnlockedTitles,
                unlockedBorders = newUnlockedBorders,
                unlockedEffects = newUnlockedEffects,
                claimedRedeemCodes = newClaimedCodes,
                nextLevelTargetExp = nextTarget,
                expProgress = newProgress
            )
            _userProfile.value = updated
            if (context != null) saveProfileToPrefs(context, updated)
            refreshLeaderboardWithCurrentUser(updated)

            scope.launch {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "exp" to newExp,
                            "xp" to newExp,
                            "points" to newPoints,
                            "level" to newLevel,
                            "rankTitle" to updated.rankTitle,
                            "unlockedTitles" to newUnlockedTitles,
                            "unlockedBorders" to newUnlockedBorders,
                            "unlockedEffects" to newUnlockedEffects,
                            "claimedRedeemCodes" to newClaimedCodes
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (_: Exception) {}
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, reward.message)
                }
            }
        }

        val builtIn = builtInRewards[cleanCode]
        if (builtIn != null) {
            applyReward(builtIn)
            return
        }

        scope.launch {
            try {
                val doc = firestore.collection("redeem_codes").document(cleanCode).get().await()
                if (doc.exists() && (doc.getBoolean("active") ?: true)) {
                    val exp = doc.getLong("exp") ?: 0L
                    val points = doc.getLong("points") ?: 0L
                    val title = doc.getString("title")
                    val borderId = doc.getString("borderId")
                    val effectId = doc.getString("effectId")
                    val msg = doc.getString("message") ?: "Kode \"$cleanCode\" berhasil diklaim!"
                    applyReward(CodeReward(exp, points, title, borderId, effectId, msg))
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        onComplete(false, "Kode \"$cleanCode\" tidak valid atau sudah kedaluwarsa.")
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(false, "Kode \"$cleanCode\" tidak ditemukan.")
                }
            }
        }
    }

    fun claimMissionReward(
        context: Context? = null,
        missionId: String,
        missionTitle: String,
        expReward: Long,
        pointsReward: Long,
        periodKey: String = getTodayDateString(),
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }
        val current = _userProfile.value ?: UserProfile(uid = uid)
        if (current.claimedMissions[missionId] == periodKey) {
            onComplete(false, "Misi \"$missionTitle\" sudah diklaim untuk periode ini!")
            return
        }

        val newExp = current.exp + expReward
        val newPoints = current.points + pointsReward
        val newLevel = computeLevelFromExp(newExp)
        val nextTarget = computeNextLevelTargetExp(newLevel)
        val newProgress = computeExpProgress(newExp, nextTarget)
        val newClaimed = current.claimedMissions.toMutableMap().apply {
            put(missionId, periodKey)
        }
        val autoMilestoneTitles = ALL_TITLES
            .filter { it.unlockType == TitleUnlockType.MILESTONE && newLevel >= it.requiredLevel }
            .map { it.titleName }
        val newUnlockedTitles = (current.unlockedTitles + autoMilestoneTitles).distinct()

        val updated = current.copy(
            exp = newExp,
            points = newPoints,
            level = newLevel,
            rankTitle = computeRankTitle(newLevel),
            unlockedTitles = newUnlockedTitles,
            claimedMissions = newClaimed,
            nextLevelTargetExp = nextTarget,
            expProgress = newProgress
        )
        _userProfile.value = updated
        if (context != null) saveProfileToPrefs(context, updated)
        refreshLeaderboardWithCurrentUser(updated)

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "exp" to newExp,
                        "xp" to newExp,
                        "points" to newPoints,
                        "level" to newLevel,
                        "rankTitle" to updated.rankTitle,
                        "unlockedTitles" to newUnlockedTitles,
                        "claimedMissions" to newClaimed
                    ),
                    SetOptions.merge()
                ).await()
                CoroutineScope(Dispatchers.Main).launch {
                    val rewardText = buildString {
                        append("+$expReward EXP")
                        if (pointsReward > 0) append(" & +$pointsReward Poin")
                    }
                    onComplete(true, "Misi selesai! Kamu mendapatkan $rewardText")
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Misi berhasil diklaim! +$expReward EXP")
                }
            }
        }
    }

    fun checkInToday(context: Context? = null, onComplete: (Boolean, String) -> Unit) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }

        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()
        val current = _userProfile.value ?: UserProfile(uid = uid)

        if (current.lastCheckInDate == todayStr) {
            onComplete(false, "Kamu sudah klaim Check-In hari ini!")
            return
        }

        val newStreak = if (current.lastCheckInDate == yesterdayStr) {
            (current.checkInStreak % 7) + 1
        } else {
            1
        }

        val rewardExp = if (newStreak == 7) 100L else 50L
        val rewardPoints = if (newStreak == 7) 50L else 20L
        val newExp = current.exp + rewardExp
        val newPoints = current.points + rewardPoints
        val newLevel = computeLevelFromExp(newExp)
        val nextTarget = computeNextLevelTargetExp(newLevel)
        val newProgress = computeExpProgress(newExp, nextTarget)

        val newClaimed = current.claimedMissions.toMutableMap().apply {
            put("daily_checkin", todayStr)
        }
        val autoMilestoneTitles = ALL_TITLES
            .filter { it.unlockType == TitleUnlockType.MILESTONE && newLevel >= it.requiredLevel }
            .map { it.titleName }
        val newUnlockedTitles = (current.unlockedTitles + autoMilestoneTitles).distinct()

        val updated = current.copy(
            exp = newExp,
            points = newPoints,
            level = newLevel,
            rankTitle = computeRankTitle(newLevel),
            unlockedTitles = newUnlockedTitles,
            claimedMissions = newClaimed,
            lastCheckInDate = todayStr,
            checkInStreak = newStreak,
            nextLevelTargetExp = nextTarget,
            expProgress = newProgress
        )
        _userProfile.value = updated
        if (context != null) {
            saveProfileToPrefs(context, updated)
        }
        refreshLeaderboardWithCurrentUser(updated)

        val updates = mapOf(
            "uid" to uid,
            "name" to updated.name,
            "avatar" to updated.avatar,
            "photoUrl" to updated.avatar,
            "exp" to newExp,
            "xp" to newExp,
            "points" to newPoints,
            "level" to newLevel,
            "rankTitle" to updated.rankTitle,
            "unlockedTitles" to newUnlockedTitles,
            "claimedMissions" to newClaimed,
            "lastCheckInDate" to todayStr,
            "checkInStreak" to newStreak
        )

        scope.launch {
            try {
                firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(
                        true,
                        "Check-In Hari ke-$newStreak berhasil! +$rewardExp EXP & +$rewardPoints Poin"
                    )
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(
                        true,
                        "Check-In Hari ke-$newStreak berhasil! +$rewardExp EXP & +$rewardPoints Poin"
                    )
                }
            }
        }
    }

    fun addExpAndPoints(expDelta: Long, pointsDelta: Long, context: Context? = null) {
        val uid = getActiveUid() ?: return
        val current = _userProfile.value
        if (current != null) {
            val newExp = (current.exp + expDelta).coerceAtLeast(0L)
            val newPoints = (current.points + pointsDelta).coerceAtLeast(0L)
            val newLevel = computeLevelFromExp(newExp)
            val nextTarget = computeNextLevelTargetExp(newLevel)
            val newProgress = computeExpProgress(newExp, nextTarget)

            val autoMilestoneTitles = ALL_TITLES
                .filter { it.unlockType == TitleUnlockType.MILESTONE && newLevel >= it.requiredLevel }
                .map { it.titleName }
            val newUnlockedTitles = (current.unlockedTitles + autoMilestoneTitles).distinct()

            val updated = current.copy(
                exp = newExp,
                points = newPoints,
                level = newLevel,
                rankTitle = computeRankTitle(newLevel),
                unlockedTitles = newUnlockedTitles,
                nextLevelTargetExp = nextTarget,
                expProgress = newProgress
            )
            _userProfile.value = updated
            if (context != null) {
                saveProfileToPrefs(context, updated)
            }
            refreshLeaderboardWithCurrentUser(updated)

            scope.launch {
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "exp" to newExp,
                            "xp" to newExp,
                            "points" to newPoints,
                            "level" to newLevel,
                            "rankTitle" to updated.rankTitle,
                            "unlockedTitles" to newUnlockedTitles
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal sinkronisasi addExpAndPoints ke Firestore: ${e.message}")
                }
            }
        } else {
            val docRef = firestore.collection("users").document(uid)
            docRef.set(
                mapOf(
                    "exp" to FieldValue.increment(expDelta),
                    "xp" to FieldValue.increment(expDelta),
                    "points" to FieldValue.increment(pointsDelta)
                ),
                SetOptions.merge()
            )
        }
    }

    /**
     * Listener Real-Time Konfigurasi Promo Popup Dinamis (`app_config/promo_popup`)
     */
    fun startListeningPromoConfig() {
        if (promoListener != null) return
        fetchLivePromoConfigFromRest()

        try {
            promoListener = firestore.collection("app_config").document("promo_popup")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        fetchLivePromoConfigFromRest()
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        _promoPopupConfig.value = null
                        return@addSnapshotListener
                    }
                    val isActive = snapshot.getBoolean("isActive")
                        ?: snapshot.getBoolean("enabled")
                        ?: false
                    val promoId = snapshot.getString("promoId").orEmpty()
                    val imageUrl = snapshot.getString("imageUrl")
                        ?: snapshot.getString("posterUrl")
                        ?: ""
                    val targetUrl = snapshot.getString("targetUrl")
                        ?: snapshot.getString("linkUrl")
                        ?: ""
                    val interval = snapshot.getDouble("intervalHours")
                        ?: snapshot.getLong("intervalHours")?.toDouble()
                        ?: 8.0
                    val maxImp = snapshot.getLong("maxImpressions")?.toInt() ?: 0
                    val updatedAt = snapshot.getLong("updatedAt") ?: 0L

                    if (isActive && imageUrl.isNotBlank()) {
                        _promoPopupConfig.value = PromoPopupConfig(
                            isActive = true,
                            promoId = promoId,
                            imageUrl = imageUrl,
                            targetUrl = targetUrl,
                            intervalHours = interval.toFloat(),
                            maxImpressions = maxImp,
                            updatedAt = updatedAt
                        )
                    } else {
                        _promoPopupConfig.value = null
                    }
                }
        } catch (ignored: Exception) {
            fetchLivePromoConfigFromRest()
        }
    }

    private fun fetchLivePromoConfigFromRest() {
        scope.launch {
            try {
                val url = "https://firestore.googleapis.com/v1/projects/dinz-62119/databases/(default)/documents/app_config/promo_popup"
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val req = Request.Builder().url(url).build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val fields = json.optJSONObject("fields") ?: return@use
                            val isActive = fields.optJSONObject("isActive")?.optBoolean("booleanValue", false) ?: false
                            val promoId = fields.optJSONObject("promoId")?.optString("stringValue", "") ?: ""
                            val imageUrl = fields.optJSONObject("imageUrl")?.optString("stringValue", "") ?: ""
                            val targetUrl = fields.optJSONObject("targetUrl")?.optString("stringValue", "") ?: ""
                            val interval = (fields.optJSONObject("intervalHours")?.optDouble("doubleValue", 8.0)
                                ?: fields.optJSONObject("intervalHours")?.optLong("integerValue", 8L)?.toDouble()
                                ?: 8.0).toFloat()
                            val maxImp = fields.optJSONObject("maxImpressions")?.optInt("integerValue", 0) ?: 0
                            val updatedAt = fields.optJSONObject("updatedAt")?.optLong("integerValue", 0L) ?: 0L

                            if (isActive && imageUrl.isNotBlank()) {
                                _promoPopupConfig.value = PromoPopupConfig(
                                    isActive = true,
                                    promoId = promoId,
                                    imageUrl = imageUrl,
                                    targetUrl = targetUrl,
                                    intervalHours = interval,
                                    maxImpressions = maxImp,
                                    updatedAt = updatedAt
                                )
                            } else {
                                _promoPopupConfig.value = null
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    fun signOut(context: Context? = null) {
        userDocListener?.remove()
        userDocListener = null
        try {
            auth.signOut()
        } catch (_: Exception) {}
        if (context != null) {
            AppPreferences.clearGoogleSession(context)
            getPrefs(context).edit().remove("uid").apply()
            LocalAuthDatabase(context).apply {
                setLoggedIn(false, "Member Yuranime")
                setAvatarUrl("")
                setCoverUrl("")
            }
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}
            }
        }
        _currentUser.value = null
        _googleSession.value = null
        _userProfile.value = null
        lastSyncedWatchHistoryUid = null
        _watchHistorySyncVersion.value += 1L
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getCurrentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun notifyWatchHistoryChanged() {
        _watchHistorySyncVersion.value += 1L
    }

    /**
     * Sinkronkan satu item histori tontonan & daftar episode ditonton ke Firestore (khusus pengguna login).
     * Untuk tamu (guest), fungsi ini langsung berhenti tanpa membaca/menulis Firebase sama sekali.
     */
    fun syncWatchHistoryItem(context: Context, item: WatchHistoryItem) {
        if (!isUserLoggedIn() || item.movieId.isBlank()) return
        val uid = getActiveUid() ?: return

        scope.launch {
            try {
                val localDb = LocalAuthDatabase(context.applicationContext)
                val watchedEps = localDb.getWatchedEpisodesForAnime(item.movieId)
                val watchedEpsMap = HashMap<String, Map<String, Any>>()
                watchedEps.forEach { ep ->
                    if (ep.episodeId.isNotBlank()) {
                        watchedEpsMap[ep.episodeId] = mapOf(
                            "episodeId" to ep.episodeId,
                            "episodeNumber" to ep.episodeNumber,
                            "positionMs" to ep.positionMs,
                            "durationMs" to ep.durationMs,
                            "isCompleted" to ep.isCompleted,
                            "updatedAt" to ep.updatedAt
                        )
                    }
                }

                val payload = mapOf(
                    "animeId" to item.movieId,
                    "animeTitle" to item.animeTitle,
                    "posterUrl" to item.posterUrl,
                    "episodeId" to item.episodeId,
                    "episodeNumber" to item.episodeNumber,
                    "episodeTitle" to item.episodeTitle,
                    "lastPosition" to (item.positionMs / 1000L).coerceAtLeast(0L),
                    "lastPositionMs" to item.positionMs.coerceAtLeast(0L),
                    "totalDuration" to (item.durationMs / 1000L).coerceAtLeast(0L),
                    "totalDurationMs" to item.durationMs.coerceAtLeast(0L),
                    "isCompleted" to item.isCompleted,
                    "updatedAt" to item.updatedAt,
                    "watchedEpisodes" to watchedEpsMap
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("watch_history")
                    .document(item.movieId)
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sinkronisasi watch_history ${item.movieId}: ${e.message}")
            }
        }
    }

    /**
     * Tarik & gabungkan histori tontonan dari Firebase ke database lokal (prioritas updatedAt terbaru).
     * Otomatis dilewati bila pengguna belum login (guest).
     */
    fun syncWatchHistoryFromCloud(context: Context) {
        if (!isUserLoggedIn()) return
        val uid = getActiveUid() ?: return

        scope.launch {
            try {
                val snap = firestore.collection("users")
                    .document(uid)
                    .collection("watch_history")
                    .get()
                    .await()

                val bundles = mutableListOf<RemoteWatchHistoryBundle>()
                for (doc in snap.documents) {
                    val animeId = doc.getString("animeId")?.takeIf { it.isNotBlank() } ?: doc.id
                    if (animeId.isBlank()) continue

                    val animeTitle = doc.getString("animeTitle").orEmpty()
                    val posterUrl = doc.getString("posterUrl").orEmpty()
                    val episodeId = doc.getString("episodeId").orEmpty()
                    val episodeNumber = doc.getString("episodeNumber").orEmpty()
                    val episodeTitle = doc.getString("episodeTitle").orEmpty()

                    val lastPosMs = doc.getLong("lastPositionMs")
                        ?: ((doc.getLong("lastPosition") ?: 0L) * 1000L)
                    val totalDurMs = doc.getLong("totalDurationMs")
                        ?: ((doc.getLong("totalDuration") ?: 0L) * 1000L)
                    val isCompleted = doc.getBoolean("isCompleted") ?: false
                    val updatedAt = doc.getLong("updatedAt") ?: 0L

                    val watchedList = mutableListOf<WatchedEpisodeRecord>()
                    val rawWatched = doc.get("watchedEpisodes") as? Map<*, *>
                    rawWatched?.forEach { (k, v) ->
                        val epKey = k?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                        val map = v as? Map<*, *> ?: return@forEach
                        val epNum = map["episodeNumber"]?.toString().orEmpty()
                        val posMs = (map["positionMs"] as? Number)?.toLong() ?: 0L
                        val durMs = (map["durationMs"] as? Number)?.toLong() ?: 0L
                        val done = (map["isCompleted"] as? Boolean) ?: false
                        val epUpdated = (map["updatedAt"] as? Number)?.toLong() ?: updatedAt
                        watchedList.add(
                            WatchedEpisodeRecord(
                                movieId = animeId,
                                episodeId = epKey,
                                episodeNumber = epNum,
                                positionMs = posMs,
                                durationMs = durMs,
                                isCompleted = done,
                                updatedAt = epUpdated
                            )
                        )
                    }
                    if (episodeId.isNotBlank() && watchedList.none { it.episodeId == episodeId }) {
                        watchedList.add(
                            WatchedEpisodeRecord(
                                movieId = animeId,
                                episodeId = episodeId,
                                episodeNumber = episodeNumber,
                                positionMs = lastPosMs,
                                durationMs = totalDurMs,
                                isCompleted = isCompleted,
                                updatedAt = updatedAt
                            )
                        )
                    }

                    bundles.add(
                        RemoteWatchHistoryBundle(
                            item = WatchHistoryItem(
                                movieId = animeId,
                                animeTitle = animeTitle,
                                episodeId = episodeId,
                                episodeTitle = episodeTitle.ifBlank { "Episode ${episodeNumber.ifBlank { "1" }}" },
                                posterUrl = posterUrl,
                                positionMs = lastPosMs,
                                durationMs = totalDurMs,
                                updatedAt = updatedAt,
                                episodeNumber = episodeNumber,
                                isCompleted = isCompleted
                            ),
                            watchedEpisodes = watchedList
                        )
                    )
                }

                if (bundles.isNotEmpty()) {
                    LocalAuthDatabase(context.applicationContext).mergeRemoteWatchHistory(bundles)
                    _watchHistorySyncVersion.value += 1L
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menarik watch_history dari Firebase: ${e.message}")
            }
        }
    }

    /**
     * Menghapus satu item histori tontonan di Firebase saat pengguna login menghapus riwayat lokalnya.
     */
    fun deleteWatchHistoryFromCloud(context: Context, animeId: String) {
        _watchHistorySyncVersion.value += 1L
        if (!isUserLoggedIn() || animeId.isBlank()) return
        val uid = getActiveUid() ?: return

        scope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("watch_history")
                    .document(animeId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menghapus watch_history $animeId di Firebase: ${e.message}")
            }
        }
    }

    /** Mengubah snapshot dokumen `users` publik menjadi [UserProfile] dengan parser tunggal yang konsisten. */
    internal fun parsePublicProfileSnapshot(snapshot: DocumentSnapshot): UserProfile {
        return parseUserProfileFromSnapshot(
            snapshot = snapshot,
            fallbackUid = snapshot.id,
            fallbackSession = null,
            allowCurrentAuthTimestampFallback = false
        )
    }

    private fun readSnapshotLong(snapshot: DocumentSnapshot, field: String): Long? {
        return try {
            when (val raw = snapshot.get(field)) {
                is Number -> raw.toLong()
                is String -> raw.trim().toLongOrNull()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** Mengambil profil publik user lain berdasarkan UID dari Firestore (untuk preview profil chat). */
    suspend fun fetchUserProfileByUid(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val snap = firestore.collection("users").document(uid).get().await()
            if (snap.exists()) {
                parseUserProfileFromSnapshot(
                    snapshot = snap,
                    fallbackUid = uid,
                    fallbackSession = null,
                    allowCurrentAuthTimestampFallback = (uid == getActiveUid())
                )
            } else {
                val query = firestore.collection("users")
                    .whereEqualTo("uid", uid)
                    .limit(1)
                    .get()
                    .await()
                val first = query.documents.firstOrNull()
                if (first != null && first.exists()) {
                    parseUserProfileFromSnapshot(
                        snapshot = first,
                        fallbackUid = uid,
                        fallbackSession = null,
                        allowCurrentAuthTimestampFallback = (uid == getActiveUid())
                    )
                } else null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Gagal mengambil profil peer $uid: ${t.message}")
            null
        }
    }

    /**
     * Menyimpan seluruh progres game Willowbrook Cove RPG ke database Firestore milik akun
     * pengguna yang sedang login (`users/{uid}` dan subkoleksi `users/{uid}/rpg_saves/starter_homestead`).
     */
    suspend fun saveRpgStateToCloud(
        uid: String,
        saveJson: String,
        summary: Map<String, Any>,
        updatedAtMs: Long
    ): Boolean {
        if (!isUserLoggedIn() || uid.isBlank() || saveJson.isBlank()) return false
        var syncedAny = false

        val userDocPayload = mapOf(
            "uid" to uid,
            "rpgSaveJson" to saveJson,
            "rpgSummary" to summary,
            "rpgUpdatedAt" to updatedAtMs
        )
        try {
            firestore.collection("users")
                .document(uid)
                .set(userDocPayload, SetOptions.merge())
                .await()
            syncedAny = true
        } catch (e: Exception) {
            Log.w(TAG, "Gagal simpan rpgSaveJson pada dokumen users/$uid: ${e.message}")
        }

        val subDocPayload = HashMap<String, Any>(summary).apply {
            put("uid", uid)
            put("saveJson", saveJson)
            put("updatedAtMs", updatedAtMs)
        }
        try {
            firestore.collection("users")
                .document(uid)
                .collection("rpg_saves")
                .document("starter_homestead")
                .set(subDocPayload, SetOptions.merge())
                .await()
            syncedAny = true
        } catch (e: Exception) {
            Log.w(TAG, "Subkoleksi rpg_saves/$uid dilewati: ${e.message}")
        }

        return syncedAny
    }

    /**
     * Memuat progres game Willowbrook Cove RPG terbaru dari database Firestore milik akun login.
     * Mengecek baik `users/{uid}/rpg_saves/starter_homestead` maupun field `rpgSaveJson` pada `users/{uid}`,
     * lalu memilih versi dengan timestamp `updatedAtMs` paling baru.
     */
    suspend fun loadRpgStateFromCloud(uid: String): Pair<String, Long>? {
        if (!isUserLoggedIn() || uid.isBlank()) return null

        var bestJson: String? = null
        var bestTs = 0L

        try {
            val subSnap = firestore.collection("users")
                .document(uid)
                .collection("rpg_saves")
                .document("starter_homestead")
                .get()
                .await()
            if (subSnap.exists()) {
                val json = subSnap.getString("saveJson")?.takeIf { it.isNotBlank() }
                val ts = (subSnap.getLong("updatedAtMs") ?: subSnap.getLong("updatedAt") ?: 0L)
                if (json != null && ts >= bestTs) {
                    bestJson = json
                    bestTs = ts
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal baca subkoleksi rpg_saves untuk $uid: ${e.message}")
        }

        try {
            val userSnap = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            if (userSnap.exists()) {
                val json = userSnap.getString("rpgSaveJson")?.takeIf { it.isNotBlank() }
                val ts = userSnap.getLong("rpgUpdatedAt") ?: 0L
                if (json != null && (bestJson == null || ts > bestTs)) {
                    bestJson = json
                    bestTs = ts
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal baca rpgSaveJson pada users/$uid: ${e.message}")
        }

        return bestJson?.let { it to bestTs }
    }
}
