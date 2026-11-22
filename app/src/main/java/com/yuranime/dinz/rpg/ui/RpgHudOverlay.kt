package com.yuranime.dinz.rpg.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.ControlMode
import com.yuranime.dinz.rpg.model.ControlSettings
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.FishingUiState
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.NpcDialogueUiState
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay
import kotlin.math.hypot

@Composable
fun RpgHudOverlay(
    hud: RpgHudSnapshot,
    controls: ControlSettings,
    activeModal: ActiveRpgModal,
    onWorldTap: (Float, Float, Float, Float) -> Unit,
    onJoystickChange: (Float, Float) -> Unit,
    onToggleSprint: () -> Unit,
    onContextAction: () -> Unit,
    onFishingTap: () -> Unit,
    onCancelFishing: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onRotateBuild: () -> Unit,
    onConfirmBuild: () -> Unit,
    onCancelBuild: () -> Unit,
    onDialogueTalkAgain: () -> Unit,
    onDialogueGiveGift: (String) -> Unit,
    onDialogueService: () -> Unit,
    onDialogueClose: () -> Unit,
    onConfirmSleep: () -> Unit,
    onCancelSleep: () -> Unit,
    onOpenModal: (ActiveRpgModal) -> Unit
) {
    var visibleToast by remember { mutableStateOf("") }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(hud.toastId) {
        if (hud.toastMessage.isNotBlank()) {
            visibleToast = hud.toastMessage
            delay(3200)
            visibleToast = ""
        }
    }

    val tapEnabled = activeModal == ActiveRpgModal.NONE &&
        !hud.dialogueState.active &&
        !hud.sleepPromptOpen &&
        controls.controlMode != ControlMode.JOYSTICK_ONLY

    val showJoystick = activeModal == ActiveRpgModal.NONE &&
        !hud.dialogueState.active &&
        !hud.sleepPromptOpen &&
        hud.fishingState.phase == FishingPhase.IDLE &&
        controls.controlMode != ControlMode.TAP_ONLY

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
    ) {
        // 1. World Tap-to-Move & Contextual Tap Layer (sits underneath HUD controls!)
        if (tapEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(rootSize, tapEnabled) {
                        detectTapGestures { offset ->
                            if (rootSize.width > 0 && rootSize.height > 0) {
                                onWorldTap(offset.x, offset.y, rootSize.width.toFloat(), rootSize.height.toFloat())
                            }
                        }
                    }
            )
        }

        // 2. Floating Joystick Touch Surface (optional half-screen floating touch zone!)
        if (showJoystick && controls.floatingJoystick) {
            FloatingJoystickZone(
                onLeft = !controls.joystickOnRight,
                scale = controls.joystickScale,
                opacity = controls.controlOpacity,
                onJoystickChange = onJoystickChange,
                modifier = Modifier
                    .align(if (controls.joystickOnRight) Alignment.BottomEnd else Alignment.BottomStart)
                    .fillMaxWidth(0.45f)
                    .fillMaxHeight(0.62f)
            )
        }

        // 3. Top-Left Anime Status Bar (Portrait + Lv/EXP + Energy + Coins + Region/Time)
        if (activeModal == ActiveRpgModal.NONE) {
            val preset = RpgCatalog.presetById[hud.customization.presetId] ?: RpgCatalog.protagonistPresets.first()
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Player Portrait -> opens Character & Customization Screen
                RpgPortraitFrame(
                    facePath = preset.facePath,
                    size = 52.dp,
                    modifier = Modifier.clickable { onOpenModal(ActiveRpgModal.CHARACTER) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Row 1: Level + EXP + Energy + Coins
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(RpgHudBg)
                            .border(1.4.dp, RpgCedarLight, RoundedCornerShape(9.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Lv.${hud.playerLevel}",
                                color = Color(0xFFFFDF85),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SfProFontFamily
                            )
                            val expRatio = (hud.exp.toFloat() / hud.nextLevelExp.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF22150E))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(expRatio)
                                        .fillMaxHeight()
                                        .background(Color(0xFF4ADE80))
                                )
                            }
                        }

                        // Energy Bar
                        Column {
                            Text(
                                text = "Energi ${hud.energy}",
                                color = RpgCreamOnWood,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            val enRatio = (hud.energy.toFloat() / hud.maxEnergy.coerceAtLeast(1)).coerceIn(0f, 1f)
                            val enColor = when {
                                enRatio > 0.5f -> Color(0xFF4ADE80)
                                enRatio > 0.2f -> Color(0xFFFBBF24)
                                else -> Color(0xFFF87171)
                            }
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF22150E))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(enRatio)
                                        .fillMaxHeight()
                                        .background(enColor)
                                )
                            }
                        }

                        RpgCoinBadge(coins = hud.coins, compact = true)
                    }

                    // Row 2: Region Name + Day + Clock + Weather (tap opens World Map!)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RpgHudBg)
                            .border(1.dp, RpgCedarLight, RoundedCornerShape(8.dp))
                            .clickable { onOpenModal(ActiveRpgModal.WORLD_MAP) }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val locTitle = if (hud.currentMapId.isIndoor) hud.currentMapId.displayName else hud.currentRegion.displayName
                        Text(
                            text = locTitle,
                            color = Color(0xFFFFE7A0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "• H${hud.dayCount} ${formatRpgClock(hud.timeHours)}",
                            color = RpgCreamOnWood,
                            fontSize = 11.sp,
                            fontFamily = SfProFontFamily
                        )
                        if (hud.isRaining && !hud.currentMapId.isIndoor) {
                            Text(
                                text = "Hujan",
                                color = Color(0xFF93C5FD),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }

            // 4. Top-Right RPG Quick Icon Bar (44dp+ targets, zero text clipping!)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HudIconTile("Tas", "rpg/ui/icon_chest.png") { onOpenModal(ActiveRpgModal.INVENTORY) }
                HudIconTile("Karya", "rpg/ui/icon_craft.png") { onOpenModal(ActiveRpgModal.CRAFTING) }
                HudIconTile("Jurnal", "rpg/ui/icon_quest.png") { onOpenModal(ActiveRpgModal.QUESTS_ALMANAC) }
                HudIconTile("Peta", "rpg/ui/icon_map.png") { onOpenModal(ActiveRpgModal.WORLD_MAP) }
                HudMenuTile("Menu") { onOpenModal(ActiveRpgModal.SETTINGS) }
            }
        }

        // 5. Crisp Top-Center Toast Banner
        if (visibleToast.isNotBlank() && activeModal == ActiveRpgModal.NONE) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgParchment)
                    .border(2.dp, RpgCedarDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = visibleToast,
                    color = RpgInkDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 6. Fixed Virtual Analog Joystick (left or right side according to ControlSettings!)
        if (showJoystick && !controls.floatingJoystick) {
            FixedAnalogJoystick(
                sizeDp = (118f * controls.joystickScale).dp,
                opacity = controls.controlOpacity,
                onJoystickChange = onJoystickChange,
                modifier = Modifier
                    .align(if (controls.joystickOnRight) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            )
        }

        // 7. Bottom Contextual Action Cluster (opposite side from joystick!)
        if (activeModal == ActiveRpgModal.NONE && !hud.dialogueState.active && !hud.sleepPromptOpen) {
            val actionAlign = if (controls.joystickOnRight) Alignment.BottomStart else Alignment.BottomEnd
            when {
                hud.fishingState.phase != FishingPhase.IDLE -> {
                    FishingMinigameCard(
                        state = hud.fishingState,
                        onTap = onFishingTap,
                        onCancel = onCancelFishing,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
                    )
                }
                hud.buildPreview.active -> {
                    ConstructionActionBar(
                        hud = hud,
                        onRotate = onRotateBuild,
                        onConfirm = onConfirmBuild,
                        onCancel = onCancelBuild,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .align(actionAlign)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .alpha(controls.controlOpacity),
                        horizontalAlignment = if (controls.joystickOnRight) Alignment.Start else Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Seed Picker Strip when standing on farmable soil
                        if (hud.contextAction.type in listOf(
                                ContextActionType.TILL_SOIL,
                                ContextActionType.PLANT_SEED,
                                ContextActionType.WATER_CROP,
                                ContextActionType.HARVEST_CROP
                            )
                        ) {
                            val seeds = hud.inventory.filter {
                                RpgCatalog.itemById[it.itemId]?.category == ItemCategory.SEEDS &&
                                    it.itemId !in listOf("seed_oak", "seed_sakura")
                            }
                            if (seeds.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RpgHudBg)
                                        .border(1.2.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                                        .padding(5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    seeds.take(5).forEach { st ->
                                        val def = RpgCatalog.itemById[st.itemId] ?: return@forEach
                                        val sel = st.itemId == hud.selectedSeedId
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(if (sel) RpgTerracotta else RpgHudCard)
                                                .border(1.2.dp, if (sel) RpgGoldTrim else RpgCedarLight, RoundedCornerShape(7.dp))
                                                .clickable { onSelectSeed(st.itemId) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RpgPixelSprite(def.spritePath, 20.dp)
                                            Text(
                                                text = "${st.quantity}",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val btnScale = controls.buttonScale
                            // Sprint Toggle Pill
                            Box(
                                modifier = Modifier
                                    .size((50f * btnScale).dp)
                                    .clip(CircleShape)
                                    .background(if (hud.isRunning) RpgForestGreen else RpgHudCard)
                                    .border(2.dp, if (hud.isRunning) RpgGoldTrim else RpgCedarLight, CircleShape)
                                    .clickable(onClick = onToggleSprint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hud.isRunning) "LARI" else "JALAN",
                                    color = RpgCreamOnWood,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            // Primary Dynamic Contextual Action Button
                            val hasCtx = hud.contextAction.type != ContextActionType.NONE
                            val actionColor = when (hud.contextAction.type) {
                                ContextActionType.FISH -> RpgSkyBlue
                                ContextActionType.HARVEST_CROP, ContextActionType.GATHER_FORAGE, ContextActionType.OPEN_CHEST -> RpgForestGreen
                                ContextActionType.TALK_NPC, ContextActionType.ENTER_PORTAL, ContextActionType.SLEEP_BED -> Color(0xFFB46924)
                                ContextActionType.NONE -> RpgHudCard
                                else -> RpgTerracotta
                            }
                            Box(
                                modifier = Modifier
                                    .size((72f * btnScale).dp)
                                    .clip(CircleShape)
                                    .background(actionColor)
                                    .border(2.5.dp, if (hasCtx) RpgGoldTrim else RpgCedarLight, CircleShape)
                                    .clickable(onClick = onContextAction)
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasCtx) hud.contextAction.label else "AKSI",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SfProFontFamily,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8. Genuine Wood-Framed Anime NPC Dialogue Overlay (with Faceset Portrait, Hearts & Gifting!)
        if (hud.dialogueState.active && activeModal == ActiveRpgModal.NONE) {
            AnimeNpcDialogueBox(
                dialogue = hud.dialogueState,
                hud = hud,
                onTalkAgain = onDialogueTalkAgain,
                onGiveGift = onDialogueGiveGift,
                onService = onDialogueService,
                onClose = onDialogueClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // 9. Sleep Confirmation Modal at Player House Bed
        if (hud.sleepPromptOpen && activeModal == ActiveRpgModal.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x881A100B)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RpgCedarDark)
                        .border(2.5.dp, RpgGoldTrim, RoundedCornerShape(14.dp))
                        .padding(5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RpgParchment)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tidur Hingga Pagi?",
                        color = RpgInkDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Istirahat di rumah akan memulihkan Energi 100%, menumbuhkan tanaman yang sudah disiram, menyegarkan Gua Kristal & misi harian, serta menyimpan progresmu ke akun.",
                        color = RpgInkMuted,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        lineHeight = 17.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgActionButton(
                            text = "Nanti Saja",
                            onClick = onCancelSleep,
                            containerColor = RpgLinen,
                            contentColor = RpgInkDark,
                            modifier = Modifier.weight(1f)
                        )
                        RpgActionButton(
                            text = "Tidur Sekarang",
                            onClick = onConfirmSleep,
                            containerColor = RpgForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudIconTile(
    label: String,
    iconPath: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RpgHudBg)
            .border(1.4.dp, RpgCedarLight, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RpgPixelSprite(iconPath, 20.dp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = RpgCreamOnWood,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1
        )
    }
}

@Composable
private fun HudMenuTile(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RpgTerracotta)
            .border(1.4.dp, RpgGoldTrim, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun FixedAnalogJoystick(
    sizeDp: androidx.compose.ui.unit.Dp,
    opacity: Float,
    onJoystickChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .size(sizeDp)
            .alpha(opacity)
            .pointerInput(Unit) {
                val maxRadius = size.width * 0.36f
                detectDragGestures(
                    onDragStart = { start ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val d = start - c
                        val dist = hypot(d.x.toDouble(), d.y.toDouble()).toFloat()
                        val clamped = if (dist > maxRadius) d * (maxRadius / dist) else d
                        knobOffset = clamped
                        onJoystickChange(clamped.x / maxRadius, -clamped.y / maxRadius)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onJoystickChange(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onJoystickChange(0f, 0f)
                    }
                ) { change, _ ->
                    change.consume()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val d = change.position - c
                    val dist = hypot(d.x.toDouble(), d.y.toDouble()).toFloat()
                    val clamped = if (dist > maxRadius) d * (maxRadius / dist) else d
                    knobOffset = clamped
                    onJoystickChange(clamped.x / maxRadius, -clamped.y / maxRadius)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.45f
            val knobR = size.minDimension * 0.19f
            drawCircle(color = Color(0x993E261B), radius = outerR, center = center)
            drawCircle(color = Color(0xFFD9A648), radius = outerR, center = center, style = Stroke(width = 3.5f))
            drawCircle(color = Color(0x44FDF6E7), radius = outerR * 0.55f, center = center, style = Stroke(width = 1.5f))
            drawCircle(color = Color(0xFFF6ECD8), radius = knobR, center = center + knobOffset)
            drawCircle(color = Color(0xFF4B3022), radius = knobR, center = center + knobOffset, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun FloatingJoystickZone(
    onLeft: Boolean,
    scale: Float,
    opacity: Float,
    onJoystickChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var baseCenter by remember { mutableStateOf<Offset?>(null) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .alpha(opacity)
            .pointerInput(scale) {
                val maxR = 115f * scale
                detectDragGestures(
                    onDragStart = { start ->
                        baseCenter = start
                        knobOffset = Offset.Zero
                        onJoystickChange(0f, 0f)
                    },
                    onDragEnd = {
                        baseCenter = null
                        knobOffset = Offset.Zero
                        onJoystickChange(0f, 0f)
                    },
                    onDragCancel = {
                        baseCenter = null
                        knobOffset = Offset.Zero
                        onJoystickChange(0f, 0f)
                    }
                ) { change, _ ->
                    change.consume()
                    val base = baseCenter ?: return@detectDragGestures
                    val d = change.position - base
                    val dist = hypot(d.x.toDouble(), d.y.toDouble()).toFloat()
                    val clamped = if (dist > maxR) d * (maxR / dist) else d
                    knobOffset = clamped
                    onJoystickChange(clamped.x / maxR, -clamped.y / maxR)
                }
            }
    ) {
        val center = baseCenter
        if (center != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerR = 115f * scale
                val knobR = 46f * scale
                drawCircle(color = Color(0x883E261B), radius = outerR, center = center)
                drawCircle(color = Color(0xFFD9A648), radius = outerR, center = center, style = Stroke(width = 3.5f))
                drawCircle(color = Color(0xFFF6ECD8), radius = knobR, center = center + knobOffset)
                drawCircle(color = Color(0xFF4B3022), radius = knobR, center = center + knobOffset, style = Stroke(width = 3f))
            }
        } else {
            Text(
                text = "Geser untuk berjalan",
                color = RpgCreamOnWood.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontFamily = SfProFontFamily,
                modifier = Modifier
                    .align(if (onLeft) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(18.dp)
            )
        }
    }
}

@Composable
private fun AnimeNpcDialogueBox(
    dialogue: NpcDialogueUiState,
    hud: RpgHudSnapshot,
    onTalkAgain: () -> Unit,
    onGiveGift: (String) -> Unit,
    onService: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pickingGift by remember(dialogue.npcId) { mutableStateOf(false) }
    val giftableItems = remember(hud.inventory) {
        hud.inventory.filter {
            val cat = RpgCatalog.itemById[it.itemId]?.category
            cat != null && cat != ItemCategory.TOOLS
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RpgCedarDark)
            .border(2.5.dp, RpgGoldTrim, RoundedCornerShape(14.dp))
            .padding(5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RpgParchment)
            .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RpgPortraitFrame(facePath = dialogue.facePath, size = 62.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = dialogue.npcName,
                            color = RpgInkDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = dialogue.npcRole,
                            color = RpgInkMuted,
                            fontSize = 10.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                    RpgHeartMeter(hearts = dialogue.friendshipHearts.coerceAtMost(5), maxHearts = 5, heartSize = 13.dp)
                }
                Text(
                    text = dialogue.text,
                    color = RpgInkDark,
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily,
                    lineHeight = 16.sp
                )
            }
        }

        if (pickingGift) {
            if (giftableItems.isEmpty()) {
                Text(
                    text = "Belum ada item yang bisa dijadikan hadiah di tasmu.",
                    color = RpgInkMuted,
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    giftableItems.take(12).forEach { st ->
                        val def = RpgCatalog.itemById[st.itemId] ?: return@forEach
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RpgLinen)
                                .border(1.2.dp, RpgCedarMid, RoundedCornerShape(8.dp))
                                .clickable {
                                    pickingGift = false
                                    onGiveGift(st.itemId)
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            RpgPixelSprite(def.spritePath, 20.dp)
                            Text(
                                text = "${def.name} (${st.quantity})",
                                color = RpgInkDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RpgActionButton(
                text = "Obrolan Lagi",
                onClick = onTalkAgain,
                containerColor = RpgCedarMid,
                height = 38.dp,
                modifier = Modifier.weight(1f)
            )
            RpgActionButton(
                text = if (dialogue.giftedToday) "Sudah Diberi" else if (pickingGift) "Batal Hadiah" else "Beri Hadiah",
                onClick = { pickingGift = !pickingGift },
                containerColor = RpgForestGreen,
                enabled = !dialogue.giftedToday,
                height = 38.dp,
                modifier = Modifier.weight(1f)
            )
            if (dialogue.opensShop || dialogue.opensCooking || dialogue.opensBlacksmith) {
                val svcLabel = when {
                    dialogue.opensShop -> "Buka Toko"
                    dialogue.opensCooking -> "Masak"
                    else -> "Bengkel"
                }
                RpgActionButton(
                    text = svcLabel,
                    onClick = onService,
                    containerColor = RpgTerracotta,
                    height = 38.dp,
                    modifier = Modifier.weight(1f)
                )
            }
            RpgActionButton(
                text = "Tutup",
                onClick = onClose,
                containerColor = RpgLinen,
                contentColor = RpgInkDark,
                height = 38.dp
            )
        }
    }
}

@Composable
private fun FishingMinigameCard(
    state: FishingUiState,
    onTap: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RpgCedarDark)
            .border(2.5.dp, RpgGoldTrim, RoundedCornerShape(14.dp))
            .padding(5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RpgParchment)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val status = when (state.phase) {
                FishingPhase.CASTING -> "Melempar kail..."
                FishingPhase.WAITING -> "Menunggu gigitan ikan..."
                FishingPhase.BITE -> "ADA GIGITAN! TEKAN TARIK SEKARANG!"
                FishingPhase.MINIGAME -> "Tarik saat penanda di zona hijau!"
                FishingPhase.CAUGHT -> state.lastResultMessage
                FishingPhase.ESCAPED -> state.lastResultMessage
                else -> ""
            }
            Text(
                text = status,
                color = if (state.phase == FishingPhase.BITE) RpgTerracotta else RpgInkDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = SfProFontFamily,
                modifier = Modifier.weight(1f)
            )
            if (state.phase == FishingPhase.MINIGAME) {
                Text(
                    text = "${state.tensionTimeLeft.toInt()}d",
                    color = RpgInkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        if (state.phase == FishingPhase.MINIGAME) {
            // Timing Bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RpgCedarDark)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(6.dp))
            ) {
                val zStart = ((state.sweetZoneCenter - state.sweetZoneHalfWidth).coerceIn(0f, 1f)) * size.width
                val zWidth = (state.sweetZoneHalfWidth * 2f).coerceIn(0.05f, 1f) * size.width
                drawRect(color = RpgForestGreen, topLeft = Offset(zStart, 0f), size = androidx.compose.ui.geometry.Size(zWidth, size.height))
                val indX = state.indicatorPos.coerceIn(0f, 1f) * size.width
                drawRect(color = Color(0xFFFFF7CC), topLeft = Offset((indX - 4f).coerceAtLeast(0f), 0f), size = androidx.compose.ui.geometry.Size(8f, size.height))
            }
            // Catch Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RpgLinen)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.catchProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(RpgSkyBlue)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RpgActionButton(
                text = "Batal",
                onClick = onCancel,
                containerColor = RpgLinen,
                contentColor = RpgInkDark,
                height = 42.dp
            )
            RpgActionButton(
                text = when (state.phase) {
                    FishingPhase.BITE -> "TARIK KAIL!"
                    FishingPhase.MINIGAME -> "SENTAK!"
                    else -> "Tarik"
                },
                onClick = onTap,
                containerColor = if (state.phase == FishingPhase.BITE) RpgTerracotta else RpgForestGreen,
                height = 42.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConstructionActionBar(
    hud: RpgHudSnapshot,
    onRotate: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemName = hud.buildPreview.selectedItemId?.let { RpgCatalog.itemById[it]?.name } ?: "Furnitur"
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RpgCedarDark)
            .border(2.dp, RpgGoldTrim, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$itemName (${if (hud.buildPreview.isValid) "Siap" else "Terhalang"})",
            color = RpgCreamOnWood,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
        RpgActionButton(text = "Putar", onClick = onRotate, containerColor = RpgCedarMid, height = 38.dp)
        RpgActionButton(
            text = "Pasang",
            onClick = onConfirm,
            containerColor = RpgForestGreen,
            enabled = hud.buildPreview.isValid,
            height = 38.dp
        )
        RpgActionButton(text = "Batal", onClick = onCancel, containerColor = RpgTerracotta, height = 38.dp)
    }
}
