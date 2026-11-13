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
import com.yuranime.dinz.data.local.SavedGoogleUserSession
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
    POINTS_SHOP
}

data class TitleDefinition(
    val id: String,
    val titleName: String,
    val tierLevel: Int,
    val styleVariant: Int,
    val unlockType: TitleUnlockType,
    val requiredLevel: Int = 1,
    val pointCost: Long = 0L,
    val description: String = ""
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

data class ProfileEffectDefinition(
    val id: String,
    val name: String,
    val animationUrl: String,
    val pointCost: Long,
    val description: String,
    val tier: String = "A", // "A", "B", "C"
    val requiredLevel: Int = 1,
    val category: String = "General"
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
    val equippedBorderUrl: String = "",
    val equippedProfileEffectUrl: String = "",
    val cosmeticAnimate: Boolean = true,
    val unlockedTitles: List<String> = emptyList(),
    val unlockedBorders: List<String> = emptyList(),
    val unlockedEffects: List<String> = emptyList(),
    val claimedRedeemCodes: List<String> = emptyList(),
    val claimedMissions: Map<String, String> = emptyMap(),
    val lastCheckInDate: String = "",
    val checkInStreak: Int = 0,
    val nextLevelTargetExp: Long = 500L,
    val expProgress: Float = 0f
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
    val equippedBorderUrl: String = "",
    val equippedProfileEffectUrl: String = ""
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
    const val TOP_1_BORDER_URL = "https://url.dinzid.my.id/edTfBfx"
    const val TOP_2_BORDER_URL = "https://url.dinzid.my.id/85MXtu1"
    const val TOP_3_BORDER_URL = "https://url.dinzid.my.id/1KCdAyA"

    // Katalog Title Resmi (Milestone Level & Tukar Poin)
    val ALL_TITLES: List<TitleDefinition> = listOf(
        TitleDefinition(
            id = "title_lv1_starter",
            titleName = "WARGA YURANIME",
            tierLevel = 1,
            styleVariant = 0,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 1,
            description = "Terbuka otomatis saat bergabung sebagai keluarga Yuranime"
        ),
        TitleDefinition(
            id = "title_lv5_pioneer",
            titleName = "PERINTIS ANIME",
            tierLevel = 6,
            styleVariant = 1,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 5,
            description = "Capai Level 5 melalui misi menonton dan check-in harian"
        ),
        TitleDefinition(
            id = "title_lv10_explorer",
            titleName = "PENJELAJAH KOMIK",
            tierLevel = 10,
            styleVariant = 2,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 10,
            description = "Capai Level 10 dengan aktif menjelajahi anime & komik"
        ),
        TitleDefinition(
            id = "title_lv15_vanguard",
            titleName = "VANGUARD OTAKU",
            tierLevel = 15,
            styleVariant = 3,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 15,
            description = "Capai Level 15 sebagai garda depan komunitas Yuranime"
        ),
        TitleDefinition(
            id = "title_lv25_dimension",
            titleName = "PENGUASA DIMENSI",
            tierLevel = 25,
            styleVariant = 1,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 25,
            description = "Capai Level 25 dengan dedikasi tinggi di Yuranime"
        ),
        TitleDefinition(
            id = "title_lv50_sepuh",
            titleName = "SEPUH YURANIME",
            tierLevel = 50,
            styleVariant = 3,
            unlockType = TitleUnlockType.MILESTONE,
            requiredLevel = 50,
            description = "Gelar kehormatan tertinggi untuk legenda Level 50+"
        ),
        TitleDefinition(
            id = "shop_crimson_knight",
            titleName = "KSATRIA CRIMSON",
            tierLevel = 8,
            styleVariant = 0,
            unlockType = TitleUnlockType.POINTS_SHOP,
            pointCost = 100L,
            description = "Badge geometris eksklusif bertema prajurit Crimson"
        ),
        TitleDefinition(
            id = "shop_marathon_king",
            titleName = "RAJA MARATON",
            tierLevel = 14,
            styleVariant = 2,
            unlockType = TitleUnlockType.POINTS_SHOP,
            pointCost = 200L,
            description = "Simbol penakluk ratusan episode anime tanpa henti"
        ),
        TitleDefinition(
            id = "shop_amethyst_lord",
            titleName = "BANGSAWAN KOMIK",
            tierLevel = 20,
            styleVariant = 1,
            unlockType = TitleUnlockType.POINTS_SHOP,
            pointCost = 350L,
            description = "Gelar Royal Amethyst untuk pembaca setia manhwa & manga"
        ),
        TitleDefinition(
            id = "shop_mythic_sovereign",
            titleName = "MYTHIC SOVEREIGN",
            tierLevel = 35,
            styleVariant = 3,
            unlockType = TitleUnlockType.POINTS_SHOP,
            pointCost = 600L,
            description = "Badge kasta tertinggi Gold-Crimson berefek kilau mewah"
        )
    )

    val SHOP_BORDERS: List<BorderDefinition> = listOf(
        // Podium Classics
        BorderDefinition(
            id = "border_bronze_crest",
            name = "Vanguard Bronze Frame",
            imageUrl = TOP_3_BORDER_URL,
            pointCost = 250L,
            description = "Frame avatar bergaya mecha perunggu",
            tier = "A",
            category = "Podium"
        ),
        BorderDefinition(
            id = "border_silver_wings",
            name = "Valkyrie Silver Frame",
            imageUrl = TOP_2_BORDER_URL,
            pointCost = 450L,
            description = "Frame avatar perak bersayap presisi",
            tier = "A",
            category = "Podium"
        ),
        BorderDefinition(
            id = "border_sovereign_gold",
            name = "Sovereign Gold Frame",
            imageUrl = TOP_1_BORDER_URL,
            pointCost = 750L,
            description = "Frame avatar mahkota emas kasta tertinggi",
            tier = "A",
            category = "Podium"
        ),
        // Tier A (Starter: 300-800 Poin)
        BorderDefinition(
            id = "border_rage_red",
            name = "Rage Crimson Aura",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_e1b0849ce324835695662bb368dca622.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_e1b0849ce324835695662bb368dca622.png?size=4096&passthrough=false",
            pointCost = 500L,
            description = "Aura api merah berkobar penuh amarah petarung",
            tier = "A",
            category = "Anime"
        ),
        BorderDefinition(
            id = "border_rage_purple",
            name = "Rage Amethyst Aura",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_8171a822bd51b5c00e6203cf65d08fc3.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_8171a822bd51b5c00e6203cf65d08fc3.png?size=4096&passthrough=false",
            pointCost = 550L,
            description = "Aura petir ungu misterius kekuatan tersembunyi",
            tier = "A",
            category = "Anime"
        ),
        BorderDefinition(
            id = "border_nova_surge",
            name = "Nova Surge Cosmic",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_4de86ce972cebc31f9e2b17f6505708a.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_4de86ce972cebc31f9e2b17f6505708a.png?size=4096&passthrough=false",
            pointCost = 650L,
            description = "Pusaran energi supernova luar angkasa",
            tier = "A",
            category = "Cosmos"
        ),
        BorderDefinition(
            id = "border_constellations",
            name = "Constellations Dusk",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_1fe04b503e91456565158a74c10a30b5.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_1fe04b503e91456565158a74c10a30b5.png?size=4096&passthrough=false",
            pointCost = 600L,
            description = "Rasi bintang langit senja memukau",
            tier = "A",
            category = "Cosmos"
        ),
        BorderDefinition(
            id = "border_spirit_embers",
            name = "Spirit Embers",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_10058ec0ffad8e4693b2a2656ca8018e.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_10058ec0ffad8e4693b2a2656ca8018e.png?size=4096&passthrough=false",
            pointCost = 450L,
            description = "Percikan roh magis api suci melayang",
            tier = "A",
            category = "Fantasy"
        ),
        // Tier B (Exclusive: Lv. 35+ & 3,500 - 25,000 Poin)
        BorderDefinition(
            id = "border_arcane_sigil",
            name = "Arcane Sigil",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_ef8d91c10d3f25c2ceb90b8f58d0c0fc.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_ef8d91c10d3f25c2ceb90b8f58d0c0fc.png?size=4096&passthrough=false",
            pointCost = 5000L,
            description = "Segel sihir kuno terlarang bagi penyihir agung",
            tier = "B",
            requiredLevel = 35,
            category = "Dark Fantasy"
        ),
        BorderDefinition(
            id = "border_cybernetic",
            name = "Cybernetic Neural",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_c6b3b5550a6e7889a9c40330dc372f51.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_c6b3b5550a6e7889a9c40330dc372f51.png?size=4096&passthrough=false",
            pointCost = 7500L,
            description = "Implan neural sibernetik berkecepatan cahaya",
            tier = "B",
            requiredLevel = 35,
            category = "Cyberpunk"
        ),
        BorderDefinition(
            id = "border_digital_sunrise",
            name = "Digital Sunrise",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_cc83e95e86a51d957d47e447b966c5d1.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_cc83e95e86a51d957d47e447b966c5d1.png?size=4096&passthrough=false",
            pointCost = 12000L,
            description = "Spektrum neon fajar neo-Tokyo abad 22",
            tier = "B",
            requiredLevel = 40,
            category = "Cyberpunk"
        ),
        BorderDefinition(
            id = "border_miyabi",
            name = "Hoshimi Miyabi Blade",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_c8092128806507a66067c174c7ec1fb7.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_c8092128806507a66067c174c7ec1fb7.png?size=4096&passthrough=false",
            pointCost = 20000L,
            description = "Aura pedang void es pembelah Hollow Zenless",
            tier = "B",
            requiredLevel = 50,
            category = "Zenless Zone Zero"
        ),
        BorderDefinition(
            id = "border_ellen_joe",
            name = "Ellen Joe Shark Hunter",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_46317d74da81ae77c2e391bb7b607061.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_46317d74da81ae77c2e391bb7b607061.png?size=4096&passthrough=false",
            pointCost = 22500L,
            description = "Gigi hiu predator maid Victoria Housekeeping",
            tier = "B",
            requiredLevel = 50,
            category = "Zenless Zone Zero"
        ),
        // Tier C (Limited: Redeem Only)
        BorderDefinition(
            id = "border_venom_logo",
            name = "Venom Symbiote Crest",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_537adf38d27721432f062cc9d913e2af.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_537adf38d27721432f062cc9d913e2af.png?size=4096&passthrough=false",
            pointCost = 0L,
            description = "Tendril simbiotik hitam lambang laba-laba putih legendaris",
            tier = "C",
            category = "Limited"
        ),
        BorderDefinition(
            id = "border_venom_nologo",
            name = "Venom Tendrils",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_a7e2ab61c12d84c8b538573f28d58ae0.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_a7e2ab61c12d84c8b538573f28d58ae0.png?size=4096&passthrough=false",
            pointCost = 0L,
            description = "Sulur simbiotik hitam merayap mengelilingi bingkai avatar",
            tier = "C",
            category = "Limited"
        ),
        BorderDefinition(
            id = "border_mythic_emperor",
            name = "Mythic Emperor Crest",
            imageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_ef8d91c10d3f25c2ceb90b8f58d0c0fc.png?size=4096&passthrough=true",
            staticImageUrl = "https://cdn.discordapp.com/avatar-decoration-presets/a_ef8d91c10d3f25c2ceb90b8f58d0c0fc.png?size=4096&passthrough=false",
            pointCost = 0L,
            description = "Mahkota keemasan penguasa mutlak kasta tertinggi",
            tier = "C",
            category = "Limited"
        )
    )

    val SHOP_PROFILE_EFFECTS: List<ProfileEffectDefinition> = listOf(
        // Tier A (Starter: 600-800 Poin)
        ProfileEffectDefinition(
            id = "effect_magic_hearts_gold",
            name = "Magic Hearts Gold",
            animationUrl = "https://cdn.discordapp.com/assets/content/2ecd952da0b1c00559d3e8e2b85e053f1469e3a6cbe70295db2ee3fa3ff55883",
            pointCost = 600L,
            description = "Hujan hati emas bersinar dengan aura magis",
            tier = "A",
            category = "Anime"
        ),
        ProfileEffectDefinition(
            id = "effect_magic_hearts_blue",
            name = "Magic Hearts Sapphire",
            animationUrl = "https://cdn.discordapp.com/assets/content/8b756deca4bb1a5744fa1284d720b08db6faefc3a91e5cfb34d284e3e3b12361",
            pointCost = 650L,
            description = "Kilau partikel safir biru berkilau di kartu profil",
            tier = "A",
            category = "Anime"
        ),
        ProfileEffectDefinition(
            id = "effect_starlight_wand",
            name = "Starlight Sparkle",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/9cf28ca2b03623fa06b4725355a1532189d533dd04cfa886ef299a9a06144e5b",
            pointCost = 750L,
            description = "Pusaran tongkat bintang ajaib berkilau lembut",
            tier = "A",
            category = "Starlight Magic"
        ),
        // Tier B (Exclusive: Lv. 35+ & 3,500 - 25,000 Poin)
        ProfileEffectDefinition(
            id = "effect_cosmic_twilight",
            name = "Cosmic Twilight",
            animationUrl = "https://cdn.discordapp.com/assets/content/fcfa4926b8731b4c000f576e2c9efca1b27236b567d162bb6b8ec013be82bc72",
            pointCost = 6500L,
            description = "Aura kosmik nebula kecubung ungu yang megah",
            tier = "B",
            requiredLevel = 35,
            category = "Cosmos"
        ),
        ProfileEffectDefinition(
            id = "effect_cyberspace",
            name = "Cyberspace Matrix",
            animationUrl = "https://cdn.discordapp.com/assets/content/31fe6e737fc1773afa54c0db46235cebaea6989467d51ee2bc3a44d8b9ba58ea",
            pointCost = 9500L,
            description = "Aliran data biner matriks hijau neon berkecepatan tinggi",
            tier = "B",
            requiredLevel = 40,
            category = "DISXCORE"
        ),
        ProfileEffectDefinition(
            id = "effect_cosmic_guardian",
            name = "Cosmic Guardian Nova",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/0350f55562d987e91d516248aa0c36b69512356598ea1b0b54060851ec690b2d",
            pointCost = 15000L,
            description = "Pancaran perisai pelindung nova penjaga galaksi",
            tier = "B",
            requiredLevel = 45,
            category = "Starlight Magic"
        ),
        ProfileEffectDefinition(
            id = "effect_shadow_strike",
            name = "Shadow Strike Blade",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/819dcfaf0dd0a6723e7b1c3127dd3fa65239e088a85f81f1e0cfbaab4db3f677",
            pointCost = 20000L,
            description = "Tebasan pedang bayangan hitam membelah kartu profil",
            tier = "B",
            requiredLevel = 50,
            category = "Zen Protocol"
        ),
        ProfileEffectDefinition(
            id = "effect_miyabi_slash",
            name = "Hoshimi Miyabi Slash",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/8d0841804c183f6bb5ee61befdb3c37c84413d33bcb4eed903841bea8878c665",
            pointCost = 25000L,
            description = "Tebasan pedang es klan Hoshimi pembelah waktu",
            tier = "B",
            requiredLevel = 50,
            category = "Zenless Zone Zero"
        ),
        ProfileEffectDefinition(
            id = "effect_ellen_chomp",
            name = "Ellen Joe Shark Chomp",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/9d2f22c608f619b02213796d5b08ec8ae2376993ce615c829da49386c99c43d0",
            pointCost = 25000L,
            description = "Gigitan rahang hiu laut dalam mematikan",
            tier = "B",
            requiredLevel = 50,
            category = "Zenless Zone Zero"
        ),
        // Tier C (Limited: Redeem Only)
        ProfileEffectDefinition(
            id = "effect_venom_symbiote",
            name = "Venom Symbiote Surge",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/d272f4fc37857c23e9ea7217c35d202a363f8c585679dd2e87e9aa66388bdec2",
            pointCost = 0L,
            description = "Venom menerjang layar dengan raungan sulur simbiotik hitam liar",
            tier = "C",
            category = "Limited"
        ),
        ProfileEffectDefinition(
            id = "effect_mythic_aurora",
            name = "Mythic Sovereign Aurora",
            animationUrl = "https://cdn.discordapp.com/media/v1/collectibles-shop/0350f55562d987e91d516248aa0c36b69512356598ea1b0b54060851ec690b2d",
            pointCost = 0L,
            description = "Gelombang aurora keemasan berkilau lambang penguasa",
            tier = "C",
            category = "Limited"
        )
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    fun getActiveUid(): String? {
        return auth.currentUser?.uid ?: _googleSession.value?.id ?: _userProfile.value?.uid
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
        return TitleDefinition(
            id = "custom_${titleName.lowercase(Locale.ROOT)}",
            titleName = titleName.uppercase(Locale.ROOT),
            tierLevel = fallbackLevel,
            styleVariant = fallbackLevel % 4,
            unlockType = TitleUnlockType.MILESTONE
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
            val session = SavedGoogleUserSession(
                id = fbUser.uid,
                name = fbUser.displayName ?: savedGoogle?.name ?: prefs.getString("name", "Member Yuranime") ?: "Member Yuranime",
                email = fbUser.email ?: savedGoogle?.email ?: prefs.getString("email", "") ?: "",
                photoUrl = fbUser.photoUrl?.toString() ?: savedGoogle?.photoUrl ?: prefs.getString("avatar", "")
            )
            _googleSession.value = session
            loadCachedProfile(prefs, session)
            startListeningCurrentUser(context)
        } else if (savedGoogle != null) {
            _googleSession.value = savedGoogle
            loadCachedProfile(prefs, savedGoogle)
            startListeningCurrentUser(context)
        } else if (!savedUid.isNullOrBlank()) {
            val session = SavedGoogleUserSession(
                id = savedUid,
                name = prefs.getString("name", "Member Yuranime") ?: "Member Yuranime",
                email = prefs.getString("email", "") ?: "",
                photoUrl = prefs.getString("avatar", "")
            )
            _googleSession.value = session
            loadCachedProfile(prefs, session)
            startListeningCurrentUser(context)
        }
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
        val cachedBorderUrl = prefs.getString("profile_border_${session.id}", "") ?: ""
        val cachedEffectUrl = prefs.getString("profile_effect_${session.id}", "") ?: ""
        val cachedCosmeticAnimate = prefs.getBoolean("cosmetic_animate_${session.id}", true)
        val cachedUnlockedTitles = prefs.getStringSet("profile_unlocked_titles_${session.id}", setOf("WARGA YURANIME"))
            ?.toList() ?: listOf("WARGA YURANIME")
        val cachedUnlockedBorders = prefs.getStringSet("profile_unlocked_borders_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val cachedUnlockedEffects = prefs.getStringSet("profile_unlocked_effects_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val cachedClaimedCodes = prefs.getStringSet("profile_claimed_codes_${session.id}", emptySet())
            ?.toList() ?: emptyList()
        val lastCheckIn = prefs.getString("last_checkin_${session.id}", "") ?: ""
        val streak = prefs.getInt("streak_${session.id}", 0)

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
            equippedBorderUrl = cachedBorderUrl,
            equippedProfileEffectUrl = cachedEffectUrl,
            cosmeticAnimate = cachedCosmeticAnimate,
            unlockedTitles = cachedUnlockedTitles,
            unlockedBorders = cachedUnlockedBorders,
            unlockedEffects = cachedUnlockedEffects,
            claimedRedeemCodes = cachedClaimedCodes,
            lastCheckInDate = lastCheckIn,
            checkInStreak = streak,
            nextLevelTargetExp = nextTarget,
            expProgress = progress
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
            .putString("profile_border_${profile.uid}", profile.equippedBorderUrl)
            .putString("profile_effect_${profile.uid}", profile.equippedProfileEffectUrl)
            .putBoolean("cosmetic_animate_${profile.uid}", profile.cosmeticAnimate)
            .putStringSet("profile_unlocked_titles_${profile.uid}", profile.unlockedTitles.toSet())
            .putStringSet("profile_unlocked_borders_${profile.uid}", profile.unlockedBorders.toSet())
            .putStringSet("profile_unlocked_effects_${profile.uid}", profile.unlockedEffects.toSet())
            .putStringSet("profile_claimed_codes_${profile.uid}", profile.claimedRedeemCodes.toSet())
            .putLong("exp_${profile.uid}", profile.exp)
            .putLong("points_${profile.uid}", profile.points)
            .putString("last_checkin_${profile.uid}", profile.lastCheckInDate)
            .putInt("streak_${profile.uid}", profile.checkInStreak)
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

    fun onGoogleAccountSelected(
        uid: String,
        name: String,
        email: String,
        avatar: String?,
        idToken: String? = null,
        context: Context? = null
    ) {
        val session = SavedGoogleUserSession(
            id = uid,
            name = name,
            email = email,
            photoUrl = avatar
        )
        _googleSession.value = session
        if (context != null) {
            AppPreferences.saveGoogleSession(context, uid, name, email, avatar)
        }

        if (!idToken.isNullOrBlank()) {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        _currentUser.value = auth.currentUser
                    }
            } catch (ignored: Exception) {}
        }

        scope.launch {
            ensureUserDocumentExists(context, session)
            startListeningCurrentUser(context)
            startListeningLeaderboard()
        }
    }

    suspend fun onGoogleAccountSelected(
        context: Context,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): Result<GoogleSessionInfo> {
        return try {
            val cleanEmail = email.trim().lowercase(Locale.ROOT)
            val deterministicUid = "g_" + cleanEmail.replace(Regex("[^a-z0-9]"), "_")
            val finalName = displayName?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

            val savedSession = SavedGoogleUserSession(
                id = deterministicUid,
                name = finalName,
                email = cleanEmail,
                photoUrl = photoUrl
            )
            _googleSession.value = savedSession
            AppPreferences.saveGoogleSession(context, deterministicUid, finalName, cleanEmail, photoUrl)

            ensureUserDocumentExists(context, savedSession)
            startListeningCurrentUser(context)
            startListeningLeaderboard()

            Result.success(
                GoogleSessionInfo(
                    uid = deterministicUid,
                    name = finalName,
                    email = cleanEmail,
                    avatar = photoUrl.orEmpty()
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

                var finalUid = subUid
                var finalName = displayName
                var finalEmail = email
                var finalPhoto = photoUrl

                try {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        _currentUser.value = fbUser
                        finalUid = fbUser.uid
                        finalName = fbUser.displayName ?: displayName
                        finalEmail = fbUser.email ?: email
                        finalPhoto = fbUser.photoUrl?.toString() ?: photoUrl
                    }
                } catch (firebaseErr: Exception) {
                    Log.w(TAG, "Menggunakan sesi Google terverifikasi: ${firebaseErr.message}")
                }

                val savedSession = SavedGoogleUserSession(
                    id = finalUid,
                    name = finalName,
                    email = finalEmail,
                    photoUrl = finalPhoto
                )
                _googleSession.value = savedSession
                AppPreferences.saveGoogleSession(context, finalUid, finalName, finalEmail, finalPhoto)

                ensureUserDocumentExists(context, savedSession)
                startListeningCurrentUser(context)
                startListeningLeaderboard()
                Result.success(
                    GoogleSessionInfo(
                        uid = finalUid,
                        name = finalName,
                        email = finalEmail,
                        avatar = finalPhoto
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

    private suspend fun ensureUserDocumentExists(context: Context?, session: SavedGoogleUserSession) {
        val uid = session.id
        val prefs = context?.let { getPrefs(it) }
        val localExp = prefs?.getLong("exp_$uid", 0L) ?: 0L
        val localPoints = prefs?.getLong("points_$uid", 100L) ?: 100L
        val level = computeLevelFromExp(localExp)
        val nextTarget = computeNextLevelTargetExp(level)
        val progress = computeExpProgress(localExp, nextTarget)
        val defaultUsername = session.name.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "")
            .ifBlank { "member" }
        val userTag = computeUserTag(uid, prefs?.getString("profile_tag_$uid", null))
        val defaultJoinDate = formatJoinDate(
            try { auth.currentUser?.metadata?.creationTimestamp } catch (_: Exception) { null }
                ?: System.currentTimeMillis()
        )

        val initialProfile = UserProfile(
            uid = uid,
            name = prefs?.getString("profile_name_$uid", session.name) ?: session.name,
            username = prefs?.getString("profile_username_$uid", defaultUsername) ?: defaultUsername,
            userTag = userTag,
            email = session.email,
            avatar = prefs?.getString("profile_avatar_$uid", session.photoUrl ?: "") ?: (session.photoUrl ?: ""),
            banner = prefs?.getString("profile_banner_$uid", "") ?: "",
            bio = prefs?.getString("profile_bio_$uid", "Penikmat anime & komik di Yuranime.")
                ?: "Penikmat anime & komik di Yuranime.",
            role = prefs?.getString("profile_role_$uid", "Member") ?: "Member",
            joinDate = prefs?.getString("profile_joindate_$uid", defaultJoinDate) ?: defaultJoinDate,
            exp = localExp,
            points = localPoints,
            level = level,
            rankTitle = computeRankTitle(level),
            equippedTitle = prefs?.getString("profile_title_$uid", "") ?: "",
            equippedBorderUrl = prefs?.getString("profile_border_$uid", "") ?: "",
            equippedProfileEffectUrl = prefs?.getString("profile_effect_$uid", "") ?: "",
            cosmeticAnimate = prefs?.getBoolean("cosmetic_animate_$uid", true) ?: true,
            unlockedTitles = listOf("WARGA YURANIME"),
            unlockedBorders = prefs?.getStringSet("profile_unlocked_borders_$uid", emptySet())?.toList() ?: emptyList(),
            unlockedEffects = prefs?.getStringSet("profile_unlocked_effects_$uid", emptySet())?.toList() ?: emptyList(),
            claimedRedeemCodes = prefs?.getStringSet("profile_claimed_codes_$uid", emptySet())?.toList() ?: emptyList(),
            lastCheckInDate = prefs?.getString("last_checkin_$uid", "") ?: "",
            checkInStreak = prefs?.getInt("streak_$uid", 0) ?: 0,
            nextLevelTargetExp = nextTarget,
            expProgress = progress
        )
        if (_userProfile.value == null) {
            _userProfile.value = initialProfile
        }
        if (context != null) {
            saveProfileToPrefs(context, initialProfile)
        }

        try {
            val docRef = firestore.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val newProfileMap = hashMapOf(
                    "uid" to uid,
                    "name" to initialProfile.name,
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
                    "equippedBorderUrl" to initialProfile.equippedBorderUrl,
                    "equippedProfileEffectUrl" to initialProfile.equippedProfileEffectUrl,
                    "cosmeticAnimate" to initialProfile.cosmeticAnimate,
                    "unlockedTitles" to initialProfile.unlockedTitles,
                    "unlockedBorders" to initialProfile.unlockedBorders,
                    "unlockedEffects" to initialProfile.unlockedEffects,
                    "claimedRedeemCodes" to initialProfile.claimedRedeemCodes,
                    "lastCheckInDate" to initialProfile.lastCheckInDate,
                    "checkInStreak" to initialProfile.checkInStreak,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastActive" to FieldValue.serverTimestamp()
                )
                docRef.set(newProfileMap, SetOptions.merge()).await()
            } else {
                val existingExp = maxOf(snapshot.getLong("exp") ?: 0L, snapshot.getLong("xp") ?: 0L)
                val resolvedJoinDate = extractJoinDateFromSnapshot(
                    snapshot,
                    try { auth.currentUser?.metadata?.creationTimestamp } catch (_: Exception) { null }
                )
                val resolvedTag = computeUserTag(uid, snapshot.getString("userTag"))
                val patchMap = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "exp" to existingExp,
                    "xp" to existingExp,
                    "userTag" to resolvedTag,
                    "joinDate" to resolvedJoinDate,
                    "lastActive" to FieldValue.serverTimestamp()
                )
                if (snapshot.getString("email").isNullOrBlank() && session.email.isNotBlank()) {
                    patchMap["email"] = session.email
                }
                docRef.set(patchMap, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync tertunda: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseUserProfileFromSnapshot(
        snapshot: DocumentSnapshot,
        fallbackUid: String,
        fallbackSession: SavedGoogleUserSession? = null
    ): UserProfile {
        val uid = snapshot.getString("uid")?.takeIf { it.isNotBlank() } ?: snapshot.id.ifBlank { fallbackUid }
        val exp = maxOf(snapshot.getLong("exp") ?: 0L, snapshot.getLong("xp") ?: 0L)
        val points = snapshot.getLong("points") ?: 100L
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
        val avatar = (snapshot.getString("avatar")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() }
            ?: fallbackSession?.photoUrl
            ?: "").trim()
        val banner = (snapshot.getString("banner")?.takeIf { it.isNotBlank() }
            ?: snapshot.getString("bannerUrl")?.takeIf { it.isNotBlank() }
            ?: "").trim()
        val bio = snapshot.getString("bio")?.takeIf { it.isNotBlank() }
            ?: "Penikmat anime & komik di Yuranime."
        val role = snapshot.getString("role")?.takeIf { it.isNotBlank() } ?: "Member"
        val joinDate = extractJoinDateFromSnapshot(
            snapshot,
            try { auth.currentUser?.metadata?.creationTimestamp } catch (_: Exception) { null }
        )

        val equippedTitle = snapshot.getString("equippedTitle")?.trim().orEmpty()
        val titleDef = resolveTitleDefinition(equippedTitle, computedLevel)
        val equippedTitleTier = (snapshot.getLong("equippedTitleTier")?.toInt())
            ?: titleDef?.tierLevel
            ?: computedLevel
        val equippedTitleVariant = (snapshot.getLong("equippedTitleVariant")?.toInt())
            ?: titleDef?.styleVariant
            ?: 0
        val equippedBorderUrl = snapshot.getString("equippedBorderUrl")?.trim().orEmpty()
        val equippedProfileEffectUrl = snapshot.getString("equippedProfileEffectUrl")?.trim().orEmpty()
        val cosmeticAnimate = snapshot.getBoolean("cosmeticAnimate") ?: true

        val rawUnlockedTitles = (snapshot.get("unlockedTitles") as? List<*>)
            ?.mapNotNull { it?.toString()?.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val autoMilestoneTitles = ALL_TITLES
            .filter { it.unlockType == TitleUnlockType.MILESTONE && computedLevel >= it.requiredLevel }
            .map { it.titleName }
        val unlockedTitles = (rawUnlockedTitles + autoMilestoneTitles).distinct()

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
            equippedBorderUrl = equippedBorderUrl,
            equippedProfileEffectUrl = equippedProfileEffectUrl,
            cosmeticAnimate = cosmeticAnimate,
            unlockedTitles = unlockedTitles,
            unlockedBorders = unlockedBorders,
            unlockedEffects = unlockedEffects,
            claimedRedeemCodes = claimedRedeemCodes,
            claimedMissions = rawClaimedMissions,
            lastCheckInDate = snapshot.getString("lastCheckInDate") ?: "",
            checkInStreak = (snapshot.getLong("checkInStreak") ?: 0L).toInt(),
            nextLevelTargetExp = nextTarget,
            expProgress = expProgress
        )
    }

    fun startListeningCurrentUser(context: Context? = null) {
        val uid = getActiveUid() ?: return
        userDocListener?.remove()

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
                    }
                    refreshLeaderboardWithCurrentUser(profile)
                }
            }
    }

    /**
     * TAHAP 3: Query Papan Peringkat 100% MURNI dari koleksi `users` di Firebase Firestore
     * diurutkan berdasarkan XP/EXP tertinggi (limit 100), TANPA bot atau placeholder palsu!
     */
    fun startListeningLeaderboard() {
        leaderboardListener?.remove()
        _isLeaderboardLoading.value = true

        leaderboardListener = firestore.collection("users")
            .limit(150)
            .addSnapshotListener { querySnapshot, error ->
                _isLeaderboardLoading.value = false
                if (error != null) {
                    Log.w(TAG, "Leaderboard listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val parsedUsers = querySnapshot.documents.mapNotNull { doc ->
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
                        val equippedBorderUrl = doc.getString("equippedBorderUrl")?.trim().orEmpty()
                        val equippedProfileEffectUrl = doc.getString("equippedProfileEffectUrl")?.trim().orEmpty()

                        LeaderboardUser(
                            rank = 0,
                            uid = uid,
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
                            equippedBorderUrl = equippedBorderUrl,
                            equippedProfileEffectUrl = equippedProfileEffectUrl
                        )
                    }
                        .sortedWith(
                            compareByDescending<LeaderboardUser> { it.exp }
                                .thenByDescending { it.points }
                                .thenBy { it.name.lowercase(Locale.ROOT) }
                        )
                        .take(100)
                        .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

                    _leaderboard.value = parsedUsers
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
            equippedBorderUrl = profile.equippedBorderUrl,
            equippedProfileEffectUrl = profile.equippedProfileEffectUrl
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
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = getActiveUid()
        if (uid == null) {
            onComplete(false, "Silakan login terlebih dahulu")
            return
        }

        scope.launch {
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
                .ifBlank { current.username }
            val cleanBio = bio.trim().ifBlank { "Penikmat anime & komik di Yuranime." }
            val finalTitle = equippedTitle?.trim() ?: current.equippedTitle
            val titleDef = resolveTitleDefinition(finalTitle, current.level)
            val finalTitleTier = titleDef?.tierLevel ?: current.level
            val finalTitleVariant = titleDef?.styleVariant ?: 0
            val finalBorder = equippedBorderUrl?.trim() ?: current.equippedBorderUrl

            val updated = current.copy(
                name = cleanName,
                username = cleanUsername,
                bio = cleanBio,
                avatar = finalAvatarUrl,
                banner = finalBannerUrl,
                equippedTitle = finalTitle,
                equippedTitleTier = finalTitleTier,
                equippedTitleVariant = finalTitleVariant,
                equippedBorderUrl = finalBorder
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

            val updates = mapOf(
                "uid" to uid,
                "name" to updated.name,
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
                "equippedBorderUrl" to updated.equippedBorderUrl
            )

            try {
                firestore.collection("users").document(uid).set(updates, SetOptions.merge()).await()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Profil berhasil disimpan & disinkronkan ke server!")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update profil tersimpan lokal, menunggu koneksi Firestore: ${e.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete(true, "Profil berhasil diperbarui!")
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
        val current = _userProfile.value ?: return
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
        val current = _userProfile.value ?: return

        if (current.unlockedTitles.any { it.equals(titleDefinition.titleName, ignoreCase = true) }) {
            equipTitle(context, titleDefinition, onComplete)
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
        val current = _userProfile.value ?: return

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
        val current = _userProfile.value ?: return

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
        val uid = getActiveUid()
        val current = _userProfile.value ?: return
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
        val current = _userProfile.value ?: return
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
}
