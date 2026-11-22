package com.yuranime.dinz.rpg.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.yuranime.dinz.rpg.engine.CozyFishingGame
import com.yuranime.dinz.rpg.engine.RpgModalRequest
import com.yuranime.dinz.rpg.persistence.RpgAudioVideoSettings
import com.yuranime.dinz.rpg.persistence.RpgSettingsStore
import com.yuranime.dinz.ui.theme.YuranimeTheme
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

class RpgGameActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {

    private var gameInstance: CozyFishingGame? = null
    private lateinit var settingsStore: RpgSettingsStore
    private val pendingModalState = mutableStateOf(ActiveRpgModal.NONE)

    fun getOrCreateGame(): CozyFishingGame {
        val existing = gameInstance
        if (existing != null) return existing
        return CozyFishingGame(applicationContext) { req ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = RpgSettingsStore(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#101512"))
        }

        val gameContainerId = View.generateViewId()
        val fragmentContainer = FragmentContainerView(this).apply {
            id = gameContainerId
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(fragmentContainer)

        val game = getOrCreateGame()
        val composeHud = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setContent {
                YuranimeTheme {
                    val hud by game.hudState.collectAsState()
                    val settings by settingsStore.settingsFlow.collectAsState(initial = RpgAudioVideoSettings())
                    var activeModal by remember { pendingModalState }
                    val scope = rememberCoroutineScope()

                    // Push DataStore audio/video settings into LibGDX engine live
                    androidx.compose.runtime.LaunchedEffect(settings) {
                        game.applySettings(
                            sfxVol = settings.sfxVolume,
                            bgmVol = settings.bgmVolume,
                            zoom = settings.cameraZoom,
                            weatherEnabled = settings.weatherEnabled
                        )
                    }
                    androidx.compose.runtime.LaunchedEffect(activeModal) {
                        game.setMenuPaused(activeModal != ActiveRpgModal.NONE)
                    }

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
        root.addView(composeHud)
        setContentView(root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(gameContainerId, RpgGameFragment())
                .commit()
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun exit() {
        gameInstance?.saveGameAsync(silent = true)
        finish()
    }
}
