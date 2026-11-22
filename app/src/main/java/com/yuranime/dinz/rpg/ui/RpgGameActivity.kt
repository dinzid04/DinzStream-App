package com.yuranime.dinz.rpg.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.badlogic.gdx.backends.android.SafeAndroidFragmentApplication
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.rpg.engine.CozyFishingGame
import com.yuranime.dinz.rpg.engine.RpgModalRequest
import com.yuranime.dinz.rpg.persistence.RpgAudioVideoSettings
import com.yuranime.dinz.rpg.persistence.RpgSettingsStore
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

enum class ActiveRpgModal {
    NONE,
    INVENTORY,
    MERCHANT,
    CONSTRUCTION,
    QUESTS_ALMANAC,
    CUSTOMIZATION,
    SETTINGS
}

private val RpgDarkColorScheme = darkColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    background = Color(0xFF101512),
    onBackground = Color(0xFFF6F1E7),
    surface = Color(0xFF1E1A16),
    onSurface = Color(0xFFF6F1E7)
)

class RpgGameActivity : FragmentActivity(), SafeAndroidFragmentApplication.Callbacks {

    companion object {
        private const val TAG = "RpgGameActivity"
        const val EXTRA_USER_UID = "extra_rpg_user_uid"
        const val EXTRA_USER_NAME = "extra_rpg_user_name"

        fun createIntent(context: Context, userUid: String, userName: String): Intent {
            return Intent(context, RpgGameActivity::class.java).apply {
                putExtra(EXTRA_USER_UID, userUid)
                putExtra(EXTRA_USER_NAME, userName)
            }
        }
    }

    var isSessionAuthorized: Boolean = false
        private set

    private var activeUserUid: String = ""
    private var activeUserName: String = ""
    private var gameInstance: CozyFishingGame? = null
    private lateinit var settingsStore: RpgSettingsStore
    private val pendingModalState = mutableStateOf(ActiveRpgModal.NONE)
    private val engineErrorState = mutableStateOf<String?>(null)

    fun getOrCreateGame(): CozyFishingGame? {
        if (!isSessionAuthorized || activeUserUid.isBlank()) return null
        val existing = gameInstance
        if (existing != null) return existing
        return CozyFishingGame(
            appContext = applicationContext,
            userUid = activeUserUid,
            userDisplayName = activeUserName
        ) { req ->
            runOnUiThread {
                pendingModalState.value = when (req) {
                    RpgModalRequest.MERCHANT, RpgModalRequest.SELL_BIN -> ActiveRpgModal.MERCHANT
                    RpgModalRequest.STORAGE -> ActiveRpgModal.INVENTORY
                    RpgModalRequest.HOUSE_UPGRADE -> ActiveRpgModal.CONSTRUCTION
                    RpgModalRequest.NONE -> ActiveRpgModal.NONE
                }
            }
        }.also { gameInstance = it }
    }

    fun onEngineInitFailure(error: Throwable) {
        val reason = error.localizedMessage ?: error.javaClass.simpleName
        runOnUiThread {
            engineErrorState.value = "Gagal memuat grafis game: $reason"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Selalu pass null ke super agar FragmentManager tidak pernah me-restore RpgGameFragment lama
        // yang punya GL context kadaluarsa atau ID view berbeda saat rotasi/recreation.
        super.onCreate(null)

        try {
            FirebaseManager.initSession(applicationContext)
        } catch (t: Throwable) {
            Log.w(TAG, "initSession gagal: ${t.message}")
        }

        val loggedIn = FirebaseManager.isUserLoggedIn()
        val resolvedUid = FirebaseManager.getCurrentUid()?.takeIf { it.isNotBlank() }
            ?: intent?.getStringExtra(EXTRA_USER_UID)?.takeIf { it.isNotBlank() && loggedIn }
            ?: ""

        if (!loggedIn || resolvedUid.isBlank()) {
            isSessionAuthorized = false
            Toast.makeText(
                this,
                "Willowbrook Cove RPG hanya untuk pengguna yang sudah login.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        activeUserUid = resolvedUid
        activeUserName = FirebaseManager.userProfile.value?.name?.takeIf { it.isNotBlank() }
            ?: intent?.getStringExtra(EXTRA_USER_NAME)?.takeIf { it.isNotBlank() }
            ?: "Petualang"
        isSessionAuthorized = true

        try {
            settingsStore = RpgSettingsStore(applicationContext)
            val game = getOrCreateGame() ?: run {
                finish()
                return
            }

            val root = FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.parseColor("#101512"))
            }

            // ID statis compile-time dari res/values/ids.xml (anti crash View.generateViewId mismatch)
            val fragmentContainer = FragmentContainerView(this).apply {
                id = R.id.rpg_game_container
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            root.addView(fragmentContainer)

            val composeHud = ComposeView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setContent {
                    // Pakai MaterialTheme langsung tanpa SideEffect mutasi window (hindari bentrok 60Hz dengan GLSurfaceView)
                    MaterialTheme(colorScheme = RpgDarkColorScheme) {
                        val hud by game.hudState.collectAsState()
                        val settings by settingsStore.settingsFlow.collectAsState(initial = RpgAudioVideoSettings())
                        var activeModal by remember { pendingModalState }
                        val initError by remember { engineErrorState }
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(settings) {
                            game.applySettings(
                                sfxVol = settings.sfxVolume,
                                bgmVol = settings.bgmVolume,
                                zoom = settings.cameraZoom,
                                weatherEnabled = settings.weatherEnabled
                            )
                        }
                        LaunchedEffect(activeModal) {
                            game.setMenuPaused(activeModal != ActiveRpgModal.NONE)
                        }

                        val activeError = initError ?: hud.fatalErrorMessage
                        if (activeError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xF0101512))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 420.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(RpgPanelSolid)
                                        .border(1.5.dp, RpgBorderWarm, RoundedCornerShape(14.dp))
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Tidak Dapat Memuat Game",
                                        color = RpgCreamText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Text(
                                        text = activeError,
                                        color = RpgMutedText,
                                        fontSize = 12.sp,
                                        fontFamily = SfProFontFamily,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(RpgEmberAccent)
                                            .clickable { finish() }
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Kembali ke Profil",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        } else {
                            RpgHudOverlay(
                                hud = hud,
                                activeModal = activeModal,
                                onJoystickChange = { x, y -> game.setJoystick(x, y) },
                                onToggleSprint = { game.toggleSprint() },
                                onContextAction = { game.onContextualActionPressed() },
                                onFishingTap = { game.onFishingTap() },
                                onCancelFishing = { game.cancelFishing() },
                                onSelectSeed = { game.selectSeed(it) },
                                onRotateBuild = { game.rotateConstruction() },
                                onConfirmBuild = { game.confirmBuildPlacement() },
                                onCancelBuild = { game.cancelConstruction() },
                                onOpenModal = { modal -> activeModal = modal }
                            )

                            if (activeModal != ActiveRpgModal.NONE) {
                                RpgModalRouter(
                                    activeModal = activeModal,
                                    hud = hud,
                                    settings = settings,
                                    onDismiss = { activeModal = ActiveRpgModal.NONE },
                                    onSelectSeed = { game.selectSeed(it) },
                                    onSelectTool = { game.selectTool(it) },
                                    onStartBuild = { itemId ->
                                        activeModal = ActiveRpgModal.NONE
                                        game.startConstruction(itemId)
                                    },
                                    onDiscardItem = { id -> game.discardItem(id, 1) },
                                    onSortChange = { mode -> game.setInventorySort(mode) },
                                    onBuyItem = { id, qty -> game.buyShopItem(id, qty) },
                                    onSellItem = { id, qty -> game.sellInventoryItem(id, qty) },
                                    onSellAllFish = { game.sellAllFish() },
                                    onSellAllAll = { game.sellAllFishAndCrops() },
                                    onUpgradeHouse = { game.upgradeHouse() },
                                    onClaimQuest = { id -> game.claimQuestReward(id) },
                                    onCustomize = { custom -> game.updateCustomization(custom) },
                                    onSfxChange = { v -> scope.launch { settingsStore.updateSfxVolume(v) } },
                                    onBgmChange = { v -> scope.launch { settingsStore.updateBgmVolume(v) } },
                                    onZoomChange = { z -> scope.launch { settingsStore.updateCameraZoom(z) } },
                                    onWeatherToggle = { w -> scope.launch { settingsStore.updateWeatherEnabled(w) } },
                                    onManualSave = { game.saveGameAsync(silent = false) },
                                    onExitGame = {
                                        game.saveGameAsync(silent = true)
                                        finish()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            root.addView(composeHud)
            setContentView(root)

            // Terapkan immersive fullscreen SETELAH setContentView selesai terpasang
            window.decorView.post {
                applyFullscreenSystemBars()
            }

            if (supportFragmentManager.findFragmentById(R.id.rpg_game_container) == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.rpg_game_container, RpgGameFragment())
                    .commitAllowingStateLoss()
            }

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (pendingModalState.value != ActiveRpgModal.NONE) {
                        pendingModalState.value = ActiveRpgModal.NONE
                    } else {
                        gameInstance?.saveGameAsync(silent = true)
                        finish()
                    }
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal membuka RpgGameActivity: ${t.message}", t)
            Toast.makeText(
                this,
                "Gagal membuka Willowbrook Cove RPG: ${t.localizedMessage ?: "Kesalahan grafis"}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun applyFullscreenSystemBars() {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isSessionAuthorized) {
            applyFullscreenSystemBars()
        }
    }

    override fun exit() {
        gameInstance?.saveGameAsync(silent = true)
        finish()
    }
}
