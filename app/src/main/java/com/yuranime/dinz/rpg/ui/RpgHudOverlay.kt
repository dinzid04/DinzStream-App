package com.yuranime.dinz.rpg.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.rpg.model.ContextActionType
import com.yuranime.dinz.rpg.model.FishingPhase
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.hypot

// Cozy Timber & Parchment HUD Palette (High Contrast WCAG AA+, Zero Generic Gradients)
internal val RpgPanelDark = Color(0xD91A1613)
internal val RpgPanelSolid = Color(0xFF1E1A16)
internal val RpgPanelElevated = Color(0xFF28221D)
internal val RpgBorderWarm = Color(0xFF473B30)
internal val RpgCreamText = Color(0xFFF6F1E7)
internal val RpgMutedText = Color(0xFFB5A999)
internal val RpgGoldAccent = Color(0xFFEAB308)
internal val RpgLeafAccent = Color(0xFF4ADE80)
internal val RpgWaterAccent = Color(0xFF38BDF8)
internal val RpgEmberAccent = Color(0xFFEA580C)

@Composable
fun RpgAssetPixelImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
fun RpgHudOverlay(
    hud: RpgHudSnapshot,
    activeModal: ActiveRpgModal,
    onJoystickChange: (Float, Float) -> Unit,
    onToggleSprint: () -> Unit,
    onContextAction: () -> Unit,
    onFishingTap: () -> Unit,
    onCancelFishing: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onRotateBuild: () -> Unit,
    onConfirmBuild: () -> Unit,
    onCancelBuild: () -> Unit,
    onOpenModal: (ActiveRpgModal) -> Unit
) {
    var visibleToast by remember { mutableStateOf("") }
    LaunchedEffect(hud.toastId) {
        if (hud.toastMessage.isNotBlank()) {
            visibleToast = hud.toastMessage
            delay(2600L)
            visibleToast = ""
        }
    }

    val unClaimedQuests = remember(hud.quests) {
        hud.quests.count { it.isCompleted && !it.claimed }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ==================== TOP-LEFT: STATUS BAR (COINS, LEVEL, EXP, CLOCK) ====================
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgPanelDark)
                    .border(1.dp, RpgBorderWarm, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Level & EXP Bar
                Column(modifier = Modifier.width(94.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LV.${hud.playerLevel}",
                            color = RpgCreamText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "${hud.exp}/${hud.nextLevelExp}",
                            color = RpgMutedText,
                            fontSize = 9.5.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    val expFrac = (hud.exp.toFloat() / hud.nextLevelExp.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF332B24))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(expFrac)
                                .fillMaxHeight()
                                .background(RpgLeafAccent)
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(RpgBorderWarm)
                )

                // Coin Counter with Pixel Coin Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RpgAssetPixelImage(
                        assetPath = "rpg/ui/icon_coin.png",
                        contentDescription = "Koin",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = hud.coins.toString(),
                        color = RpgGoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(RpgBorderWarm)
                )

                // Day & Time Clock
                val hr = hud.timeHours.toInt().coerceIn(0, 23)
                val min = ((hud.timeHours - hr) * 60).toInt().coerceIn(0, 59)
                val timeStr = String.format(Locale.US, "%02d:%02d", hr, min)
                val weatherTag = if (hud.isRaining) "Hujan" else if (hr >= 19 || hr < 5) "Malam" else if (hr >= 17) "Senja" else "Cerah"
                Column {
                    Text(
                        text = "Hari ${hud.dayCount} • $timeStr",
                        color = RpgCreamText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = weatherTag,
                        color = if (hud.isRaining) RpgWaterAccent else RpgMutedText,
                        fontSize = 9.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        // ==================== TOP-RIGHT: COMPACT MENU SHORTCUTS ====================
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HudShortcutButton(
                icon = Icons.Outlined.Backpack,
                label = "Tas",
                onClick = { onOpenModal(ActiveRpgModal.INVENTORY) }
            )
            HudShortcutButton(
                icon = Icons.Outlined.Construction,
                label = "Bangun",
                onClick = { onOpenModal(ActiveRpgModal.CONSTRUCTION) }
            )
            HudShortcutButton(
                icon = Icons.Outlined.EmojiEvents,
                label = "Misi",
                badgeCount = unClaimedQuests,
                onClick = { onOpenModal(ActiveRpgModal.QUESTS_ALMANAC) }
            )
            HudShortcutButton(
                icon = Icons.Outlined.Checkroom,
                label = "Gaya",
                onClick = { onOpenModal(ActiveRpgModal.CUSTOMIZATION) }
            )
            HudShortcutButton(
                icon = Icons.Outlined.Settings,
                label = "Menu",
                onClick = { onOpenModal(ActiveRpgModal.SETTINGS) }
            )
        }

        // ==================== TOP-CENTER TOAST FEEDBACK ====================
        AnimatedVisibility(
            visible = visibleToast.isNotBlank() && activeModal == ActiveRpgModal.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgPanelDark)
                    .border(1.dp, RpgBorderWarm, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = visibleToast,
                    color = RpgCreamText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ==================== FISHING MINI-GAME OVERLAY ====================
        if (hud.fishingState.phase != FishingPhase.IDLE && activeModal == ActiveRpgModal.NONE) {
            FishingOverlayPanel(
                hud = hud,
                onTap = onFishingTap,
                onCancel = onCancelFishing,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }

        // ==================== CONSTRUCTION MODE BAR ====================
        if (hud.buildPreview.active && activeModal == ActiveRpgModal.NONE) {
            ConstructionOverlayBar(
                hud = hud,
                onRotate = onRotateBuild,
                onConfirm = onConfirmBuild,
                onCancel = onCancelBuild,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        // ==================== BOTTOM-LEFT: VIRTUAL JOYSTICK + SPRINT ====================
        if (activeModal == ActiveRpgModal.NONE && hud.fishingState.phase == FishingPhase.IDLE) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VirtualJoystick(
                    onMove = onJoystickChange
                )

                // Sprint toggle (48dp touch target)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (hud.isRunning) RpgEmberAccent else RpgPanelDark)
                        .border(1.5.dp, if (hud.isRunning) RpgGoldAccent else RpgBorderWarm, CircleShape)
                        .clickable { onToggleSprint() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = if (hud.isRunning) "Berlari Aktif" else "Aktifkan Lari",
                        tint = RpgCreamText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ==================== BOTTOM-RIGHT: CONTEXTUAL ACTION BUTTON + SEED SWITCHER ====================
        if (activeModal == ActiveRpgModal.NONE && hud.fishingState.phase == FishingPhase.IDLE && !hud.buildPreview.active) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show compact seed selector chip when near farmland or holding a sapling
                val seedsInBag = remember(hud.inventory) {
                    hud.inventory.filter { RpgCatalog.itemById[it.itemId]?.category == ItemCategory.SEEDS }
                }
                if (seedsInBag.isNotEmpty() && (
                        hud.contextAction.type == ContextActionType.PLANT_SEED ||
                            hud.contextAction.type == ContextActionType.TILL_SOIL ||
                            hud.contextAction.type == ContextActionType.PLANT_SAPLING
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RpgPanelDark)
                            .border(1.dp, RpgBorderWarm, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        seedsInBag.take(4).forEach { st ->
                            val def = RpgCatalog.itemById[st.itemId] ?: return@forEach
                            val selected = hud.selectedSeedId == st.itemId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF3A3026) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (selected) RpgLeafAccent else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectSeed(st.itemId) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RpgAssetPixelImage(
                                    assetPath = def.spritePath,
                                    contentDescription = def.name,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${st.quantity}",
                                    color = RpgCreamText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }

                // Dynamic Contextual Action Button (transforms based on nearby object!)
                val ctx = hud.contextAction
                val isActionAvailable = ctx.type != ContextActionType.NONE
                val btnBg = when (ctx.type) {
                    ContextActionType.FISH -> Color(0xFF0E7490)
                    ContextActionType.CHOP_TREE -> Color(0xFFB45309)
                    ContextActionType.HARVEST_CROP -> Color(0xFF15803D)
                    ContextActionType.TILL_SOIL, ContextActionType.PLANT_SEED, ContextActionType.WATER_CROP, ContextActionType.PLANT_SAPLING -> Color(0xFF166534)
                    ContextActionType.OPEN_MERCHANT, ContextActionType.OPEN_SELL_BIN -> Color(0xFFA16207)
                    ContextActionType.OPEN_HOUSE_UPGRADE, ContextActionType.OPEN_STORAGE -> Color(0xFF7C2D12)
                    ContextActionType.INTERACT_DECOR -> Color(0xFF475569)
                    ContextActionType.NONE -> RpgPanelDark
                }
                val iconPath = when (ctx.type) {
                    ContextActionType.FISH -> "rpg/objects/tool_rod_basic.png"
                    ContextActionType.CHOP_TREE -> "rpg/objects/tool_axe.png"
                    ContextActionType.TILL_SOIL, ContextActionType.PLANT_SEED, ContextActionType.PLANT_SAPLING -> "rpg/objects/tool_hoe.png"
                    ContextActionType.WATER_CROP -> "rpg/objects/tool_watering_can.png"
                    ContextActionType.HARVEST_CROP -> "rpg/objects/crop_carrot.png"
                    ContextActionType.OPEN_MERCHANT, ContextActionType.OPEN_SELL_BIN -> "rpg/ui/icon_shop.png"
                    ContextActionType.OPEN_HOUSE_UPGRADE -> "rpg/ui/icon_build.png"
                    ContextActionType.OPEN_STORAGE -> "rpg/ui/icon_bag.png"
                    else -> "rpg/ui/icon_bag.png"
                }

                Row(
                    modifier = Modifier
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(btnBg)
                        .border(
                            width = 1.5.dp,
                            color = if (isActionAvailable) RpgCreamText.copy(alpha = 0.5f) else RpgBorderWarm,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onContextAction() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isActionAvailable) {
                        RpgAssetPixelImage(
                            assetPath = iconPath,
                            contentDescription = ctx.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = if (isActionAvailable) ctx.label else "Aksi",
                        color = if (isActionAvailable) Color.White else RpgMutedText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun HudShortcutButton(
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Box {
        Column(
            modifier = Modifier
                .size(width = 48.dp, height = 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RpgPanelDark)
                .border(1.dp, RpgBorderWarm, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = RpgCreamText,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = RpgMutedText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(RpgEmberAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit
) {
    val maxRadiusPx = 92f
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(116.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = start.x - cx
                        val dy = start.y - cy
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                        val clamped = dist.coerceAtMost(maxRadiusPx)
                        val ox = (dx / dist) * clamped
                        val oy = (dy / dist) * clamped
                        knobOffset = Offset(ox, oy)
                        // Note: LibGDX Y points UP, screen Y points DOWN -> invert oy!
                        onMove(ox / maxRadiusPx, -oy / maxRadiusPx)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = knobOffset.x + dragAmount.x
                        val ny = knobOffset.y + dragAmount.y
                        val dist = hypot(nx.toDouble(), ny.toDouble()).toFloat().coerceAtLeast(1f)
                        val clamped = dist.coerceAtMost(maxRadiusPx)
                        val ox = (nx / dist) * clamped
                        val oy = (ny / dist) * clamped
                        knobOffset = Offset(ox, oy)
                        onMove(ox / maxRadiusPx, -oy / maxRadiusPx)
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = size.minDimension * 0.44f
            drawCircle(
                color = Color(0x881A1613),
                radius = baseR,
                center = center
            )
            drawCircle(
                color = Color(0x99A68B6A),
                radius = baseR,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            val thumbCenter = Offset(
                center.x + knobOffset.x * 0.48f,
                center.y + knobOffset.y * 0.48f
            )
            drawCircle(
                color = Color(0xE6F6F1E7),
                radius = baseR * 0.42f,
                center = thumbCenter
            )
        }
    }
}

@Composable
private fun FishingOverlayPanel(
    hud: RpgHudSnapshot,
    onTap: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fs = hud.fishingState
    Column(
        modifier = modifier
            .width(340.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RpgPanelDark)
            .border(1.5.dp, if (fs.phase == FishingPhase.BITE) RpgGoldAccent else RpgBorderWarm, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (fs.phase) {
            FishingPhase.CASTING, FishingPhase.WAITING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (fs.phase == FishingPhase.CASTING) "Melempar kail..." else "Menunggu ikan menggigit...",
                        color = RpgCreamText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Batal",
                        color = RpgMutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RpgPanelElevated)
                            .clickable { onCancel() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            FishingPhase.BITE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IKAN MENGGIGIT! (!)",
                        color = RpgGoldAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgEmberAccent)
                            .clickable { onTap() }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TARIK!",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            FishingPhase.MINIGAME -> {
                val rarityLabel = fs.targetFish?.rarity?.label ?: "Ikan"
                val rarityColor = fs.targetFish?.rarity?.colorHex?.let { Color(it) } ?: RpgWaterAccent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tekan TARIK saat penanda di zona hijau",
                        color = RpgCreamText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = rarityLabel,
                        color = rarityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timing Bar with Green Sweet Spot + Moving Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, RpgBorderWarm, RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val zoneStart = ((fs.sweetZoneCenter - fs.sweetZoneHalfWidth).coerceIn(0f, 1f)) * w
                        val zoneEnd = ((fs.sweetZoneCenter + fs.sweetZoneHalfWidth).coerceIn(0f, 1f)) * w
                        drawRect(
                            color = Color(0xFF22C55E),
                            topLeft = Offset(zoneStart, 0f),
                            size = androidx.compose.ui.geometry.Size(zoneEnd - zoneStart, h)
                        )
                        val markerX = fs.indicatorPos.coerceIn(0f, 1f) * w
                        drawRect(
                            color = Color.White,
                            topLeft = Offset((markerX - 4f).coerceIn(0f, w - 8f), 0f),
                            size = androidx.compose.ui.geometry.Size(8f, h)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Catch progress bar + Tap button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Progres Tangkap ${(fs.catchProgress * 100).toInt()}%",
                            color = RpgMutedText,
                            fontSize = 10.sp,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2A241E))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fs.catchProgress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(RpgWaterAccent)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E7490))
                            .border(1.dp, RpgWaterAccent, RoundedCornerShape(10.dp))
                            .clickable { onTap() }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TARIK!",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            FishingPhase.CAUGHT -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    fs.targetFish?.let { fish ->
                        RpgAssetPixelImage(
                            assetPath = fish.spritePath,
                            contentDescription = fish.name,
                            modifier = Modifier.size(width = 42.dp, height = 28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = fs.targetFish?.name ?: "Tangkapan Berhasil!",
                            color = RpgGoldAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = fs.lastResultMessage,
                            color = RpgCreamText,
                            fontSize = 11.5.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            FishingPhase.ESCAPED -> {
                Text(
                    text = fs.lastResultMessage.ifBlank { "Ikan lolos!" },
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun ConstructionOverlayBar(
    hud: RpgHudSnapshot,
    onRotate: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val def = hud.buildPreview.selectedItemId?.let { RpgCatalog.itemById[it] } ?: return
    val ownedCount = hud.inventory.find { it.itemId == def.id }?.quantity ?: 0
    val costText = if (ownedCount > 0) {
        "Stok Tas: $ownedCount"
    } else {
        "Bahan: ${def.woodCost} Kayu, ${def.stoneCost} Batu"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RpgPanelDark)
            .border(1.5.dp, if (hud.buildPreview.isValid) RpgLeafAccent else Color(0xFFEF4444), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RpgAssetPixelImage(
            assetPath = def.spritePath,
            contentDescription = def.name,
            modifier = Modifier.size(28.dp)
        )
        Column {
            Text(
                text = "Pasang ${def.name}",
                color = RpgCreamText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
            Text(
                text = "$costText • ${if (hud.buildPreview.isValid) "Lokasi Siap" else "Terhalang"}",
                color = if (hud.buildPreview.isValid) RpgLeafAccent else Color(0xFFFCA5A5),
                fontSize = 10.5.sp,
                fontFamily = SfProFontFamily
            )
        }

        if (def.rotatable) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgPanelElevated)
                    .clickable { onRotate() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.RotateRight,
                    contentDescription = "Putar",
                    tint = RpgCreamText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hud.buildPreview.isValid) Color(0xFF15803D) else Color(0xFF3F3A36))
                .clickable(enabled = hud.buildPreview.isValid) { onConfirm() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pasang",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF7F1D1D))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Batal",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
