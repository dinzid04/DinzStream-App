package com.yuranime.dinz.rpg.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.model.ControlMode
import com.yuranime.dinz.rpg.model.ControlSettings
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.MapId
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.QuestType
import com.yuranime.dinz.rpg.model.RecipeType
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.rpg.model.WorldRegion
import com.yuranime.dinz.rpg.persistence.RpgAudioVideoSettings
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlin.math.roundToInt

@Composable
fun RpgModalRouter(
    activeModal: ActiveRpgModal,
    craftingInitialTab: Int,
    hud: RpgHudSnapshot,
    settings: RpgAudioVideoSettings,
    onDismiss: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onSelectTool: (String) -> Unit,
    onEatFood: (String) -> Unit,
    onCraftRecipe: (String) -> Unit,
    onStartBuild: (String) -> Unit,
    onDiscardItem: (String) -> Unit,
    onSortChange: (InventorySortMode) -> Unit,
    onBuyItem: (String, Int) -> Unit,
    onSellItem: (String, Int) -> Unit,
    onSellAllFish: () -> Unit,
    onSellAllAll: () -> Unit,
    onUpgradeHouse: () -> Unit,
    onClaimQuest: (String) -> Unit,
    onCustomize: (PlayerCustomization) -> Unit,
    onSfxChange: (Float) -> Unit,
    onBgmChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onRaylightToggle: (Boolean) -> Unit,
    onControlsChange: (ControlSettings) -> Unit,
    onResetControls: () -> Unit,
    onManualSave: () -> Unit,
    onExitGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x9A1B100A))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
        ) {
            when (activeModal) {
                ActiveRpgModal.INVENTORY -> InventoryRpgModal(
                    hud = hud,
                    onClose = onDismiss,
                    onSelectSeed = onSelectSeed,
                    onSelectTool = onSelectTool,
                    onEatFood = onEatFood,
                    onStartBuild = onStartBuild,
                    onDiscardItem = onDiscardItem,
                    onSortChange = onSortChange
                )
                ActiveRpgModal.CHARACTER -> CharacterRpgModal(
                    hud = hud,
                    onClose = onDismiss,
                    onCustomize = onCustomize
                )
                ActiveRpgModal.CRAFTING -> CraftingRpgModal(
                    hud = hud,
                    initialTab = craftingInitialTab,
                    onClose = onDismiss,
                    onCraftRecipe = onCraftRecipe,
                    onStartBuild = onStartBuild,
                    onUpgradeHouse = onUpgradeHouse
                )
                ActiveRpgModal.MERCHANT -> MerchantRpgModal(
                    hud = hud,
                    onClose = onDismiss,
                    onBuyItem = onBuyItem,
                    onSellItem = onSellItem,
                    onSellAllFish = onSellAllFish,
                    onSellAllAll = onSellAllAll
                )
                ActiveRpgModal.QUESTS_ALMANAC -> JournalAlmanacModal(
                    hud = hud,
                    onClose = onDismiss,
                    onClaimQuest = onClaimQuest
                )
                ActiveRpgModal.WORLD_MAP -> WorldMapModal(
                    hud = hud,
                    onClose = onDismiss
                )
                ActiveRpgModal.SETTINGS -> SettingsRpgModal(
                    hud = hud,
                    settings = settings,
                    onClose = onDismiss,
                    onSfxChange = onSfxChange,
                    onBgmChange = onBgmChange,
                    onZoomChange = onZoomChange,
                    onWeatherToggle = onWeatherToggle,
                    onRaylightToggle = onRaylightToggle,
                    onControlsChange = onControlsChange,
                    onResetControls = onResetControls,
                    onManualSave = onManualSave,
                    onExitGame = onExitGame
                )
                ActiveRpgModal.NONE -> Unit
            }
        }
    }
}

// ==================== 1. RPG INVENTORY & INSPECTOR ====================

@Composable
private fun InventoryRpgModal(
    hud: RpgHudSnapshot,
    onClose: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onSelectTool: (String) -> Unit,
    onEatFood: (String) -> Unit,
    onStartBuild: (String) -> Unit,
    onDiscardItem: (String) -> Unit,
    onSortChange: (InventorySortMode) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var selectedItemId by remember { mutableStateOf(hud.inventory.firstOrNull()?.itemId) }
    var sortMode by remember { mutableStateOf(InventorySortMode.CATEGORY) }

    val filtered = remember(hud.inventory, selectedCategory) {
        if (selectedCategory == null) hud.inventory
        else hud.inventory.filter { RpgCatalog.itemById[it.itemId]?.category == selectedCategory }
    }
    val inspectedStack = filtered.find { it.itemId == selectedItemId } ?: filtered.firstOrNull()
    val inspectedDef = inspectedStack?.let { RpgCatalog.itemById[it.itemId] }

    RpgWoodWindow(
        title = "Tas Petualang Sakura Vale",
        subtitle = "${hud.inventory.size} jenis barang • Energi ${hud.energy}/${hud.maxEnergy}",
        onClose = onClose,
        headerExtra = { RpgCoinBadge(hud.coins) },
        modifier = Modifier.fillMaxSize()
    ) {
        // Category Filter + Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryPill("Semua", selectedCategory == null) { selectedCategory = null }
            ItemCategory.entries.forEach { cat ->
                CategoryPill(cat.label, selectedCategory == cat) { selectedCategory = cat }
            }
            Spacer(Modifier.width(8.dp))
            CategoryPill("Urut: ${sortMode.label}", false, bgColor = RpgCedarMid, textColor = RpgCreamOnWood) {
                val next = InventorySortMode.entries[(sortMode.ordinal + 1) % InventorySortMode.entries.size]
                sortMode = next
                onSortChange(next)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Pane: RPG Grid of Linen Item Slots
            Box(
                modifier = Modifier
                    .weight(1.45f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgLinen)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    Text(
                        text = "Belum ada barang di kategori ini.",
                        color = RpgInkMuted,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(66.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.itemId }) { stack ->
                            val def = RpgCatalog.itemById[stack.itemId] ?: return@items
                            val isSelected = stack.itemId == inspectedStack?.itemId
                            val isEquipped = stack.itemId == hud.selectedToolId || stack.itemId == hud.selectedSeedId
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) RpgLinenSelected else RpgParchmentSoft)
                                    .border(
                                        width = if (isSelected) 2.2.dp else 1.2.dp,
                                        color = if (isSelected) RpgTerracotta else if (isEquipped) RpgForestGreen else RpgCedarLight,
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                    .clickable { selectedItemId = stack.itemId }
                                    .padding(4.dp)
                            ) {
                                RpgPixelSprite(
                                    assetPath = def.spritePath,
                                    size = 34.dp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                if (stack.quantity > 1) {
                                    Text(
                                        text = "${stack.quantity}",
                                        color = RpgInkDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = SfProFontFamily,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RpgParchment)
                                            .padding(horizontal = 3.dp)
                                    )
                                }
                                if (isEquipped) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(RpgForestGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane: Detailed Item Inspector & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgParchmentSoft)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (inspectedStack != null && inspectedDef != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpgLinen)
                                .border(1.5.dp, RpgCedarMid, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            RpgPixelSprite(inspectedDef.spritePath, 38.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inspectedDef.name,
                                color = RpgInkDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "${inspectedDef.category.label} • Stok: ${inspectedStack.quantity}",
                                color = RpgInkMuted,
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    Text(
                        text = inspectedDef.description,
                        color = RpgInkDark,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        lineHeight = 16.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (inspectedDef.sellPrice > 0) {
                            StatPill("Harga Jual: ${inspectedDef.sellPrice} Koin", RpgLinen, RpgInkDark)
                        }
                        if (inspectedDef.energyRestore > 0) {
                            StatPill("+${inspectedDef.energyRestore} Energi", Color(0xFFD8F3DC), RpgForestGreen)
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (inspectedDef.energyRestore > 0) {
                        RpgActionButton(
                            text = "Makan (+${inspectedDef.energyRestore} Energi)",
                            onClick = { onEatFood(inspectedDef.id) },
                            containerColor = RpgForestGreen,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (inspectedDef.category == ItemCategory.TOOLS) {
                        RpgActionButton(
                            text = if (hud.selectedToolId == inspectedDef.id) "Alat Sedang Dipilih" else "Gunakan Alat",
                            onClick = { onSelectTool(inspectedDef.id) },
                            containerColor = RpgSkyBlue,
                            enabled = hud.selectedToolId != inspectedDef.id,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (inspectedDef.category == ItemCategory.SEEDS) {
                        RpgActionButton(
                            text = if (hud.selectedSeedId == inspectedDef.id) "Bibit Aktif" else "Pilih Bibit Ini",
                            onClick = { onSelectSeed(inspectedDef.id) },
                            containerColor = RpgForestGreen,
                            enabled = hud.selectedSeedId != inspectedDef.id,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (inspectedDef.placeable) {
                        RpgActionButton(
                            text = "Pasang di Dunia / Rumah",
                            onClick = { onStartBuild(inspectedDef.id) },
                            containerColor = RpgTerracotta,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (inspectedDef.category != ItemCategory.TOOLS) {
                        RpgActionButton(
                            text = "Buang 1x",
                            onClick = { onDiscardItem(inspectedDef.id) },
                            containerColor = RpgLinen,
                            contentColor = RpgBerryRed,
                            height = 38.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "Pilih item di sebelah kiri untuk melihat detail & aksi.",
                        color = RpgInkMuted,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

// ==================== 2. CHARACTER STATUS & ANIME CUSTOMIZATION ====================

@Composable
private fun CharacterRpgModal(
    hud: RpgHudSnapshot,
    onClose: () -> Unit,
    onCustomize: (PlayerCustomization) -> Unit
) {
    val activePreset = RpgCatalog.presetById[hud.customization.presetId] ?: RpgCatalog.protagonistPresets.first()
    val tintOptions = listOf(
        0xFFFFFFFFL to "Alami",
        0xFFFFD6D6L to "Sakura",
        0xFFD5E8FFL to "Langit",
        0xFFD8F3DCL to "Rimba",
        0xFFFFECC0L to "Senja",
        0xFFEBD8FFL to "Kristal"
    )

    RpgWoodWindow(
        title = "Karakter & Gaya Anime",
        subtitle = "${hud.accountName} • ${activePreset.name} (${activePreset.genderTag})",
        onClose = onClose,
        headerExtra = { RpgCoinBadge(hud.coins) },
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Pane: Portrait, Stats & 6 RPG Skills
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgParchmentSoft)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RpgPortraitFrame(facePath = activePreset.facePath, size = 64.dp)
                    Column {
                        Text(
                            text = hud.accountName.ifBlank { activePreset.name },
                            color = RpgInkDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "Level ${hud.playerLevel} (${hud.exp}/${hud.nextLevelExp} EXP)",
                            color = RpgTerracotta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "Energi ${hud.energy}/${hud.maxEnergy} • Rumah Tier ${hud.houseTier}",
                            color = RpgInkMuted,
                            fontSize = 11.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                Text(
                    text = "Keahlian Hidup Sakura Vale",
                    color = RpgInkDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily
                )

                val skills = listOf(
                    "Memancing" to hud.fishingLevel,
                    "Bertani" to hud.farmingLevel,
                    "Menebang & Alam" to hud.gatheringLevel,
                    "Menambang" to hud.miningLevel,
                    "Memasak" to hud.cookingLevel,
                    "Kerajinan" to hud.craftingLevel
                )
                skills.forEach { (label, lvl) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(RpgLinen)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = RpgInkDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = SfProFontFamily)
                        Text("Lv. $lvl", color = RpgForestGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                    }
                }
            }

            // Right Pane: 8 Anime Protagonist Presets + Color Tint Swatches
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgLinen)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pilih Karakter Anime (8 Gaya)",
                    color = RpgInkDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(RpgCatalog.protagonistPresets, key = { it.id }) { preset ->
                        val selected = preset.id == hud.customization.presetId
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) RpgLinenSelected else RpgParchmentSoft)
                                .border(
                                    if (selected) 2.2.dp else 1.2.dp,
                                    if (selected) RpgTerracotta else RpgCedarLight,
                                    RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    onCustomize(hud.customization.copy(presetId = preset.id))
                                }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RpgPortraitFrame(preset.facePath, size = 38.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    color = RpgInkDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1
                                )
                                Text(
                                    text = preset.genderTag,
                                    color = RpgInkMuted,
                                    fontSize = 9.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Nuansa Warna Pakaian",
                    color = RpgInkDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tintOptions.forEach { (hex, label) ->
                        val sel = hud.customization.outfitTintHex == hex
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) RpgCedarMid else RpgParchmentSoft)
                                .border(1.4.dp, if (sel) RpgGoldTrim else RpgCedarLight, RoundedCornerShape(8.dp))
                                .clickable { onCustomize(hud.customization.copy(outfitTintHex = hex)) }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(1.dp, RpgCedarDark, CircleShape)
                            )
                            Text(
                                text = label,
                                color = if (sel) RpgCreamOnWood else RpgInkDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. CRAFTING, COOKING & HOUSE CONSTRUCTION ====================

@Composable
private fun CraftingRpgModal(
    hud: RpgHudSnapshot,
    initialTab: Int,
    onClose: () -> Unit,
    onCraftRecipe: (String) -> Unit,
    onStartBuild: (String) -> Unit,
    onUpgradeHouse: () -> Unit
) {
    var tab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val qtyMap = remember(hud.inventory) { hud.inventory.associate { it.itemId to it.quantity } }

    RpgWoodWindow(
        title = "Karya, Dapur & Konstruksi",
        subtitle = "Masak hidangan penambah energi, buat alat/furnitur & bangun rumah",
        onClose = onClose,
        headerExtra = { RpgCoinBadge(hud.coins) },
        modifier = Modifier.fillMaxSize()
    ) {
        RpgTabStrip(
            tabs = listOf("Dapur Memasak", "Meja Kerajinan", "Dekorasi & Rumah"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(10.dp))

        when (tab) {
            0, 1 -> {
                val targetType = if (tab == 0) RecipeType.COOKING else RecipeType.CRAFTING
                val list = remember(targetType) { RpgCatalog.recipes.filter { it.type == targetType } }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list, key = { it.id }) { rec ->
                        val outDef = RpgCatalog.itemById[rec.outputItemId] ?: return@items
                        val alreadyOwnsTool = outDef.category == ItemCategory.TOOLS && (qtyMap[outDef.id] ?: 0) > 0
                        val hasAll = !alreadyOwnsTool && rec.ingredients.all { (iid, req) -> (qtyMap[iid] ?: 0) >= req }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpgParchmentSoft)
                                .border(1.4.dp, if (hasAll) RpgForestGreen else RpgCedarLight, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RpgLinen)
                                    .border(1.dp, RpgCedarMid, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                RpgPixelSprite(outDef.spritePath, 32.dp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = outDef.name,
                                        color = RpgInkDark,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = SfProFontFamily
                                    )
                                    if (outDef.energyRestore > 0) {
                                        StatPill("+${outDef.energyRestore} Energi", Color(0xFFD8F3DC), RpgForestGreen)
                                    }
                                }
                                Text(
                                    text = rec.description,
                                    color = RpgInkMuted,
                                    fontSize = 11.sp,
                                    fontFamily = SfProFontFamily
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    rec.ingredients.forEach { (iid, req) ->
                                        val owned = qtyMap[iid] ?: 0
                                        val ingName = RpgCatalog.itemById[iid]?.name ?: iid
                                        val ok = owned >= req
                                        StatPill(
                                            text = "$ingName $owned/$req",
                                            bg = if (ok) Color(0xFFDCEEE2) else Color(0xFFF6D9D5),
                                            fg = if (ok) RpgForestGreen else RpgBerryRed
                                        )
                                    }
                                }
                            }
                            RpgActionButton(
                                text = when {
                                    alreadyOwnsTool -> "Dimiliki"
                                    tab == 0 -> "Masak"
                                    else -> "Buat"
                                },
                                onClick = { onCraftRecipe(rec.id) },
                                containerColor = if (tab == 0) RpgTerracotta else RpgForestGreen,
                                enabled = hasAll,
                                height = 40.dp
                            )
                        }
                    }
                }
            }
            2 -> {
                val nextSpec = RpgCatalog.houseUpgrades.find { it.tier == hud.houseTier + 1 }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpgLinenSelected)
                                .border(2.dp, RpgGoldTrim, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RpgPixelSprite("rpg/objects/houses/house_tier${hud.houseTier.coerceIn(1, 3)}.png", 58.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val curSpec = RpgCatalog.houseUpgrades.find { it.tier == hud.houseTier }
                                Text(
                                    text = "Rumah Saat Ini: ${curSpec?.name ?: "Tier ${hud.houseTier}"} (Tier ${hud.houseTier}/3)",
                                    color = RpgInkDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SfProFontFamily
                                )
                                if (nextSpec != null) {
                                    Text(
                                        text = "Berikutnya: ${nextSpec.name} (${nextSpec.coinCost} Koin, ${nextSpec.woodCost} Kayu, ${nextSpec.stoneCost} Batu, ${nextSpec.ironCost} Besi)",
                                        color = RpgInkMuted,
                                        fontSize = 11.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                } else {
                                    Text(
                                        text = "Rumahmu sudah mencapai tingkat Vila Sakura maksimal!",
                                        color = RpgForestGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                            if (nextSpec != null) {
                                RpgActionButton(
                                    text = "Upgrade",
                                    onClick = onUpgradeHouse,
                                    containerColor = RpgTerracotta,
                                    height = 42.dp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Pasang Furnitur & Dekorasi (Bisa di Luar & Di Dalam Rumahmu!)",
                            color = RpgInkDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    items(RpgCatalog.buildableItems, key = { it.id }) { item ->
                        val owned = qtyMap[item.id] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(RpgParchmentSoft)
                                .border(1.2.dp, RpgCedarLight, RoundedCornerShape(9.dp))
                                .padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RpgPixelSprite(item.spritePath, 32.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = RpgInkDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                                Text(
                                    text = if (owned > 0) "Stok di tas: $owned (Gratis pasang)" else "Atau buat: ${item.woodCost} Kayu, ${item.stoneCost} Batu",
                                    color = if (owned > 0) RpgForestGreen else RpgInkMuted,
                                    fontSize = 11.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            RpgActionButton(
                                text = "Pasang",
                                onClick = { onStartBuild(item.id) },
                                containerColor = RpgForestGreen,
                                height = 38.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 4. MERCHANT SHOP (TOKO HANA) ====================

@Composable
private fun MerchantRpgModal(
    hud: RpgHudSnapshot,
    onClose: () -> Unit,
    onBuyItem: (String, Int) -> Unit,
    onSellItem: (String, Int) -> Unit,
    onSellAllFish: () -> Unit,
    onSellAllAll: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var buyQty by remember { mutableIntStateOf(1) }

    RpgWoodWindow(
        title = "Toko Serba Ada Hana",
        subtitle = "Bibit musim semi, alat pemancing/penambang, makanan & jual hasil alam",
        onClose = onClose,
        headerExtra = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RpgPortraitFrame("rpg/characters/npc/npc_hana_face.png", size = 34.dp)
                RpgCoinBadge(hud.coins)
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryPill("Beli Barang", tab == 0) { tab = 0 }
                CategoryPill("Jual Hasil", tab == 1) { tab = 1 }
            }
            if (tab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(1, 5, 10).forEach { q ->
                        CategoryPill("x$q", buyQty == q) { buyQty = q }
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RpgActionButton("Jual Semua Ikan", onSellAllFish, containerColor = RpgSkyBlue, height = 36.dp)
                    RpgActionButton("Jual Ikan & Panen", onSellAllAll, containerColor = RpgForestGreen, height = 36.dp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (tab == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                items(RpgCatalog.shopItems, key = { it.id }) { item ->
                    val qty = if (item.category == ItemCategory.TOOLS) 1 else buyQty
                    val total = item.buyPrice * qty
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(RpgParchmentSoft)
                            .border(1.2.dp, RpgCedarLight, RoundedCornerShape(9.dp))
                            .padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgPixelSprite(item.spritePath, 32.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = RpgInkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                            Text(item.description, color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        RpgActionButton(
                            text = "Beli ${qty}x ($total)",
                            onClick = { onBuyItem(item.id, qty) },
                            containerColor = RpgTerracotta,
                            enabled = hud.coins >= total,
                            height = 38.dp
                        )
                    }
                }
            }
        } else {
            val sellable = hud.inventory.filter { st ->
                val def = RpgCatalog.itemById[st.itemId]
                def != null && def.sellPrice > 0 && def.category != ItemCategory.TOOLS
            }
            if (sellable.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada barang untuk dijual di tasmu.", color = RpgInkMuted, fontSize = 13.sp, fontFamily = SfProFontFamily)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                    items(sellable, key = { it.itemId }) { st ->
                        val def = RpgCatalog.itemById[st.itemId] ?: return@items
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(RpgParchmentSoft)
                                .border(1.2.dp, RpgCedarLight, RoundedCornerShape(9.dp))
                                .padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RpgPixelSprite(def.spritePath, 32.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${def.name} (x${st.quantity})", color = RpgInkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                                Text("${def.sellPrice} Koin/buah • Total ${def.sellPrice * st.quantity} Koin", color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RpgActionButton("Jual 1", { onSellItem(st.itemId, 1) }, containerColor = RpgCedarMid, height = 38.dp)
                                if (st.quantity > 1) {
                                    RpgActionButton("Semua", { onSellItem(st.itemId, st.quantity) }, containerColor = RpgForestGreen, height = 38.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 5. QUEST JOURNAL, 8 VILLAGERS & 16-FISH ENCYCLOPEDIA ====================

@Composable
private fun JournalAlmanacModal(
    hud: RpgHudSnapshot,
    onClose: () -> Unit,
    onClaimQuest: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }

    RpgWoodWindow(
        title = "Jurnal & Ensiklopedia Sakura Vale",
        subtitle = "Misi Cerita & Harian • 8 Warga Desa • ${hud.caughtFishCounts.size}/${RpgCatalog.fishSpecies.size} Spesies Ikan",
        onClose = onClose,
        modifier = Modifier.fillMaxSize()
    ) {
        RpgTabStrip(
            tabs = listOf("Misi (${hud.quests.count { !it.claimed }})", "Warga Desa (8)", "Ikan (${hud.caughtFishCounts.size}/16)", "Pencapaian"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(10.dp))

        when (tab) {
            0 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                    items(hud.quests, key = { it.def.id }) { q ->
                        val typeBg = when (q.def.type) {
                            QuestType.MAIN -> RpgTerracotta
                            QuestType.SIDE -> RpgSkyBlue
                            QuestType.DAILY -> RpgForestGreen
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (q.claimed) RpgLinen else RpgParchmentSoft)
                                .border(1.3.dp, if (q.isCompleted && !q.claimed) RpgGoldTrim else RpgCedarLight, RoundedCornerShape(9.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatPill(q.def.type.label, typeBg, Color.White)
                                    Text(q.def.title, color = RpgInkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                                }
                                Text(q.def.description, color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                                Text(
                                    text = "Progres: ${q.progress}/${q.def.targetCount} • Hadiah: +${q.def.coinReward} Koin, +${q.def.expReward} EXP",
                                    color = RpgInkDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            RpgActionButton(
                                text = when {
                                    q.claimed -> "Selesai"
                                    q.isCompleted -> "Klaim!"
                                    else -> "${q.progress}/${q.def.targetCount}"
                                },
                                onClick = { onClaimQuest(q.def.id) },
                                containerColor = if (q.isCompleted && !q.claimed) RpgForestGreen else RpgLinen,
                                contentColor = if (q.isCompleted && !q.claimed) Color.White else RpgInkMuted,
                                enabled = q.isCompleted && !q.claimed,
                                height = 38.dp
                            )
                        }
                    }
                }
            }
            1 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                    items(hud.npcSummaries, key = { it.id }) { npc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpgParchmentSoft)
                                .border(1.3.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                                .padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RpgPortraitFrame(npc.facePath, size = 48.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${npc.name} (${npc.roleTitle})", color = RpgInkDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                                Text("Lokasi: ${npc.currentMapId.displayName} • ${npc.activityLabel}", color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RpgHeartMeter(hearts = npc.hearts.coerceAtMost(5), maxHearts = 5, heartSize = 13.dp)
                                    Text("${npc.hearts}/10 Hati", color = RpgTerracotta, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    if (npc.talkedToday) StatPill("Sudah Disapa", Color(0xFFDCEEE2), RpgForestGreen)
                                    if (npc.giftedToday) StatPill("Sudah Diberi Hadiah", RpgLinenSelected, RpgInkDark)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(185.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(RpgCatalog.fishSpecies, key = { it.id }) { fish ->
                        val count = hud.caughtFishCounts[fish.id] ?: 0
                        val caught = count > 0
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (caught) RpgParchmentSoft else RpgLinen)
                                .border(1.3.dp, if (caught) Color(fish.rarity.colorHex) else RpgCedarLight, RoundedCornerShape(9.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.alpha(if (caught) 1f else 0.35f)) {
                                RpgPixelSprite(fish.spritePath, 34.dp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (caught) fish.name else "??? (${fish.rarity.label})",
                                    color = RpgInkDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${fish.habitat.label} • ${fish.sellPrice} Koin",
                                    color = RpgInkMuted,
                                    fontSize = 10.sp,
                                    fontFamily = SfProFontFamily
                                )
                                Text(
                                    text = if (caught) "Tertangkap: ${count}x" else "Belum ditemukan",
                                    color = if (caught) RpgForestGreen else RpgInkMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }
            3 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxSize()) {
                    items(hud.achievements, key = { it.def.id }) { ach ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (ach.unlocked) RpgLinenSelected else RpgParchmentSoft)
                                .border(1.3.dp, if (ach.unlocked) RpgGoldTrim else RpgCedarLight, RoundedCornerShape(9.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ach.def.title, color = RpgInkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                                Text(ach.def.description, color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                            }
                            StatPill(
                                text = if (ach.unlocked) "Terbuka!" else "${ach.progress}/${ach.def.targetCount}",
                                bg = if (ach.unlocked) RpgForestGreen else RpgLinen,
                                fg = if (ach.unlocked) Color.White else RpgInkDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 6. INTERACTIVE WORLD MAP (13 REGIONS + LIVE PLAYER & NPC MARKERS) ====================

@Composable
private fun WorldMapModal(
    hud: RpgHudSnapshot,
    onClose: () -> Unit
) {
    var selectedRegion by remember { mutableStateOf(hud.currentRegion) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    RpgWoodWindow(
        title = "Peta Dunia Sakura Vale",
        subtitle = "Wilayah Ditemukan: ${hud.discoveredRegions.size}/${WorldRegion.entries.size} • Ketuk pin untuk detail",
        onClose = onClose,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Illustrated Stylized Parchment Map with Live Player & NPC Pins!
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF9AC26B))
                    .border(2.dp, RpgCedarDark, RoundedCornerShape(10.dp))
                    .onSizeChanged { mapSize = it }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Hidden Sakura Forest (NW)
                    drawRoundRect(Color(0xFFF6C4CF), Offset(0f, 0f), Size(w * 0.32f, h * 0.20f), CornerRadius(12f))
                    // Mountain Highland (NE)
                    drawRoundRect(Color(0xFFB4A392), Offset(w * 0.65f, 0f), Size(w * 0.35f, h * 0.25f), CornerRadius(12f))
                    // Dense Forest (SW)
                    drawRoundRect(Color(0xFF5D964B), Offset(0f, h * 0.60f), Size(w * 0.32f, h * 0.26f), CornerRadius(12f))
                    // Ancient Ruins (SE)
                    drawRoundRect(Color(0xFF6B8F5E), Offset(w * 0.72f, h * 0.50f), Size(w * 0.28f, h * 0.36f), CornerRadius(12f))
                    // Golden Beach & Ocean (South)
                    drawRect(Color(0xFFF3CA87), Offset(0f, h * 0.84f), Size(w, h * 0.08f))
                    drawRect(Color(0xFF62B6DC), Offset(0f, h * 0.90f), Size(w, h * 0.10f))
                    // Winding River Valley
                    drawRect(Color(0xFF6CC4E8), Offset(w * 0.66f, h * 0.10f), Size(w * 0.055f, h * 0.82f))
                    // Whispering Lake
                    drawOval(Color(0xFF6CC4E8), Offset(w * 0.33f, h * 0.64f), Size(w * 0.28f, h * 0.20f))
                    // Village Dirt Boulevard & Plaza
                    drawRect(Color(0xFFD5A978), Offset(w * 0.12f, h * 0.38f), Size(w * 0.74f, h * 0.05f))
                    drawRoundRect(Color(0xFFD5A978), Offset(w * 0.43f, h * 0.34f), Size(w * 0.14f, h * 0.23f), CornerRadius(8f))
                }

                // 13 Region Pins
                if (mapSize.width > 0 && mapSize.height > 0) {
                    WorldRegion.entries.forEach { reg ->
                        val discovered = reg.id in hud.discoveredRegions
                        val unlocked = hud.playerLevel >= reg.requiredLevel
                        val isSel = selectedRegion == reg
                        val px = (reg.mapNormX * mapSize.width).roundToInt() - with(density) { 14.dp.roundToPx() }
                        val py = (reg.mapNormY * mapSize.height).roundToInt() - with(density) { 14.dp.roundToPx() }
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(px.coerceIn(4, mapSize.width - 36), py.coerceIn(4, mapSize.height - 36)) }
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !unlocked -> Color(0xFF7B4B3A)
                                        isSel -> RpgTerracotta
                                        discovered -> RpgGoldTrim
                                        else -> RpgCedarMid
                                    }
                                )
                                .border(2.dp, if (isSel) Color.White else RpgCedarDark, CircleShape)
                                .clickable { selectedRegion = reg },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!unlocked) "L${reg.requiredLevel}" else if (discovered) "★" else "?",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Live Player Dot on Overworld (or active region)
                    val pNormX = if (hud.currentMapId == MapId.OVERWORLD) (hud.playerTileX / 80f) else hud.currentRegion.mapNormX
                    val pNormY = if (hud.currentMapId == MapId.OVERWORLD) (1f - hud.playerTileY / 64f) else hud.currentRegion.mapNormY
                    val dotX = (pNormX * mapSize.width).roundToInt() - with(density) { 7.dp.roundToPx() }
                    val dotY = (pNormY * mapSize.height).roundToInt() - with(density) { 7.dp.roundToPx() }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dotX.coerceIn(2, mapSize.width - 18), dotY.coerceIn(2, mapSize.height - 18)) }
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            // Right: Selected Region Info & NPC Presence
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RpgParchmentSoft)
                    .border(1.5.dp, RpgCedarLight, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val unlocked = hud.playerLevel >= selectedRegion.requiredLevel
                val discovered = selectedRegion.id in hud.discoveredRegions

                Text(
                    text = selectedRegion.displayName,
                    color = RpgInkDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily
                )
                StatPill(
                    text = when {
                        !unlocked -> "Terkunci (Butuh Level ${selectedRegion.requiredLevel})"
                        discovered -> "Sudah Dijelajahi"
                        else -> "Belum Dikunjungi"
                    },
                    bg = when {
                        !unlocked -> Color(0xFFF6D9D5)
                        discovered -> Color(0xFFD8F3DC)
                        else -> RpgLinen
                    },
                    fg = when {
                        !unlocked -> RpgBerryRed
                        discovered -> RpgForestGreen
                        else -> RpgInkMuted
                    }
                )
                Text(
                    text = selectedRegion.subtitle,
                    color = RpgInkMuted,
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily,
                    lineHeight = 16.sp
                )

                Text(
                    text = "Legenda Peta",
                    color = RpgInkDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily
                )
                Text("• Titik Merah: Posisimu saat ini (${hud.currentMapId.displayName})", color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                Text("• Bintang Emas: Wilayah sudah ditemukan", color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
                Text("• L2 / L3: Wilayah rahasia yang terbuka saat naik level", color = RpgInkMuted, fontSize = 11.sp, fontFamily = SfProFontFamily)
            }
        }
    }
}

// ==================== 7. SETTINGS & FULL PRO MOBILE CONTROLS CUSTOMIZER ====================

@Composable
private fun SettingsRpgModal(
    hud: RpgHudSnapshot,
    settings: RpgAudioVideoSettings,
    onClose: () -> Unit,
    onSfxChange: (Float) -> Unit,
    onBgmChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onRaylightToggle: (Boolean) -> Unit,
    onControlsChange: (ControlSettings) -> Unit,
    onResetControls: () -> Unit,
    onManualSave: () -> Unit,
    onExitGame: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val c = settings.controls

    RpgWoodWindow(
        title = "Pengaturan & Kontrol Sakura Vale",
        subtitle = "Akun: ${hud.accountName} • ${hud.cloudSyncStatus}",
        onClose = onClose,
        modifier = Modifier.fillMaxSize()
    ) {
        RpgTabStrip(
            tabs = listOf("Kontrol & Tata Letak", "Audio, Grafis & Akun"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )
        Spacer(Modifier.height(10.dp))

        if (tab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Mode Kendali", color = RpgInkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ControlMode.entries.forEach { mode ->
                        val sel = c.controlMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (sel) RpgCedarMid else RpgParchmentSoft)
                                .border(1.5.dp, if (sel) RpgGoldTrim else RpgCedarLight, RoundedCornerShape(9.dp))
                                .clickable { onControlsChange(c.copy(controlMode = mode)) }
                                .padding(9.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (sel) RpgCreamOnWood else RpgInkDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = mode.description,
                                color = if (sel) RpgMutedOnWood else RpgInkMuted,
                                fontSize = 10.sp,
                                fontFamily = SfProFontFamily,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                SettingsSwitchRow(
                    title = "Joystick Mengambang (Floating)",
                    subtitle = "Sentuh dan geser di area layar mana saja untuk memunculkan joystick",
                    checked = c.floatingJoystick,
                    onCheckedChange = { onControlsChange(c.copy(floatingJoystick = it)) }
                )
                SettingsSwitchRow(
                    title = "Posisi Joystick di Sisi Kanan",
                    subtitle = "Tukar posisi joystick dan tombol aksi sesuai kenyamanan tangan",
                    checked = c.joystickOnRight,
                    onCheckedChange = { onControlsChange(c.copy(joystickOnRight = it)) }
                )

                SettingsSliderRow("Ukuran Joystick", "${(c.joystickScale * 100).roundToInt()}%", c.joystickScale, 0.75f..1.35f) {
                    onControlsChange(c.copy(joystickScale = it))
                }
                SettingsSliderRow("Ukuran Tombol Aksi", "${(c.buttonScale * 100).roundToInt()}%", c.buttonScale, 0.80f..1.30f) {
                    onControlsChange(c.copy(buttonScale = it))
                }
                SettingsSliderRow("Transparansi Kontrol", "${(c.controlOpacity * 100).roundToInt()}%", c.controlOpacity, 0.45f..1.0f) {
                    onControlsChange(c.copy(controlOpacity = it))
                }
                SettingsSliderRow("Sensitivitas Analog", "${(c.controlSensitivity * 100).roundToInt()}%", c.controlSensitivity, 0.70f..1.40f) {
                    onControlsChange(c.copy(controlSensitivity = it))
                }

                RpgActionButton(
                    text = "Reset Tata Letak Kontrol ke Default",
                    onClick = onResetControls,
                    containerColor = RpgLinen,
                    contentColor = RpgInkDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSliderRow("Volume Musik (BGM)", "${(settings.bgmVolume * 100).roundToInt()}%", settings.bgmVolume, 0f..1f, onBgmChange)
                SettingsSliderRow("Volume Efek Suara (SFX)", "${(settings.sfxVolume * 100).roundToInt()}%", settings.sfxVolume, 0f..1f, onSfxChange)
                SettingsSliderRow("Zoom Kamera", "${String.format("%.2f", settings.cameraZoom)}x", settings.cameraZoom, 0.75f..1.45f, onZoomChange)

                SettingsSwitchRow(
                    title = "Cuaca Hujan Dinamis",
                    subtitle = "Hujan otomatis menyiram kebun secara alami",
                    checked = settings.weatherEnabled,
                    onCheckedChange = onWeatherToggle
                )
                SettingsSwitchRow(
                    title = "Sinar Matahari Pagi (Raylight)",
                    subtitle = "Efek berkas cahaya hangat di pepohonan & desa",
                    checked = settings.raylightEnabled,
                    onCheckedChange = onRaylightToggle
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RpgActionButton(
                        text = "Simpan ke Database Akun",
                        onClick = onManualSave,
                        containerColor = RpgForestGreen,
                        modifier = Modifier.weight(1f)
                    )
                    RpgActionButton(
                        text = "Kembali ke Yuranime",
                        onClick = onExitGame,
                        containerColor = RpgTerracotta,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    bgColor: Color? = null,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    val bg = bgColor ?: if (selected) RpgTerracotta else RpgLinen
    val fg = textColor ?: if (selected) Color.White else RpgInkDark
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.2.dp, if (selected) RpgCedarDark else RpgCedarLight, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily, maxLines = 1)
    }
}

@Composable
private fun StatPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily, maxLines = 1)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(RpgParchmentSoft)
            .border(1.2.dp, RpgCedarLight, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = RpgInkDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
            Text(subtitle, color = RpgInkMuted, fontSize = 10.sp, fontFamily = SfProFontFamily)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RpgForestGreen,
                uncheckedThumbColor = RpgParchment,
                uncheckedTrackColor = RpgCedarLight
            )
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(RpgParchmentSoft)
            .border(1.2.dp, RpgCedarLight, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = RpgInkDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
            Text(valueText, color = RpgTerracotta, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SfProFontFamily)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = RpgTerracotta,
                activeTrackColor = RpgTerracotta,
                inactiveTrackColor = RpgLinen
            )
        )
    }
}
