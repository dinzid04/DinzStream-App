package com.yuranime.dinz.rpg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.rpg.inventory.InventorySortMode
import com.yuranime.dinz.rpg.model.ItemCategory
import com.yuranime.dinz.rpg.model.PlayerCustomization
import com.yuranime.dinz.rpg.model.RpgCatalog
import com.yuranime.dinz.rpg.model.RpgHudSnapshot
import com.yuranime.dinz.rpg.persistence.RpgAudioVideoSettings
import com.yuranime.dinz.ui.theme.SfProFontFamily

@Composable
fun RpgModalRouter(
    activeModal: ActiveRpgModal,
    hud: RpgHudSnapshot,
    settings: RpgAudioVideoSettings,
    onDismiss: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onSelectTool: (String) -> Unit,
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
    onManualSave: () -> Unit,
    onExitGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA0A0908))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(14.dp))
                .background(RpgPanelSolid)
                .border(1.5.dp, RpgBorderWarm, RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
                .padding(14.dp)
        ) {
            when (activeModal) {
                ActiveRpgModal.INVENTORY -> InventoryModalContent(
                    hud = hud,
                    onDismiss = onDismiss,
                    onSelectSeed = onSelectSeed,
                    onSelectTool = onSelectTool,
                    onStartBuild = onStartBuild,
                    onDiscardItem = onDiscardItem,
                    onSortChange = onSortChange
                )
                ActiveRpgModal.MERCHANT -> MerchantModalContent(
                    hud = hud,
                    onDismiss = onDismiss,
                    onBuyItem = onBuyItem,
                    onSellItem = onSellItem,
                    onSellAllFish = onSellAllFish,
                    onSellAllAll = onSellAllAll
                )
                ActiveRpgModal.CONSTRUCTION -> ConstructionModalContent(
                    hud = hud,
                    onDismiss = onDismiss,
                    onStartBuild = onStartBuild,
                    onUpgradeHouse = onUpgradeHouse
                )
                ActiveRpgModal.QUESTS_ALMANAC -> QuestsAndAlmanacContent(
                    hud = hud,
                    onDismiss = onDismiss,
                    onClaimQuest = onClaimQuest
                )
                ActiveRpgModal.CUSTOMIZATION -> CustomizationModalContent(
                    hud = hud,
                    onDismiss = onDismiss,
                    onCustomize = onCustomize
                )
                ActiveRpgModal.SETTINGS -> SettingsModalContent(
                    hud = hud,
                    settings = settings,
                    onDismiss = onDismiss,
                    onSfxChange = onSfxChange,
                    onBgmChange = onBgmChange,
                    onZoomChange = onZoomChange,
                    onWeatherToggle = onWeatherToggle,
                    onManualSave = onManualSave,
                    onExitGame = onExitGame
                )
                ActiveRpgModal.NONE -> Unit
            }
        }
    }
}

@Composable
private fun ModalHeader(
    title: String,
    subtitle: String,
    coins: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = RpgCreamText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
            Text(
                text = subtitle,
                color = RpgMutedText,
                fontSize = 11.sp,
                fontFamily = SfProFontFamily
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgPanelElevated)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RpgAssetPixelImage("rpg/ui/icon_coin.png", "Koin", Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$coins",
                    color = RpgGoldAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgPanelElevated)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, "Tutup", tint = RpgCreamText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RpgEmberAccent else RpgPanelElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else RpgMutedText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
    }
}

// ==================== 1. INVENTORY MODAL ====================

@Composable
private fun InventoryModalContent(
    hud: RpgHudSnapshot,
    onDismiss: () -> Unit,
    onSelectSeed: (String) -> Unit,
    onSelectTool: (String) -> Unit,
    onStartBuild: (String) -> Unit,
    onDiscardItem: (String) -> Unit,
    onSortChange: (InventorySortMode) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var currentSort by remember { mutableStateOf(InventorySortMode.CATEGORY) }
    val filtered = remember(hud.inventory, selectedCategory) {
        if (selectedCategory == null) hud.inventory
        else hud.inventory.filter { RpgCatalog.itemById[it.itemId]?.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader("Tas & Inventaris", "Kelola alat, ikan, kayu, bibit, dan furnitur", hud.coins, onDismiss)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { TabChip("Semua", selectedCategory == null) { selectedCategory = null } }
                items(ItemCategory.entries) { cat ->
                    TabChip(cat.label, selectedCategory == cat) { selectedCategory = cat }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgPanelElevated)
                    .border(1.dp, RpgBorderWarm, RoundedCornerShape(8.dp))
                    .clickable {
                        val next = InventorySortMode.entries[(currentSort.ordinal + 1) % InventorySortMode.entries.size]
                        currentSort = next
                        onSortChange(next)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Urut: ${currentSort.label}",
                    color = RpgCreamText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = RpgBorderWarm, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Belum ada item di kategori ini. Memancing, menebang, atau berkebun untuk mengisinya.",
                    color = RpgMutedText,
                    fontSize = 12.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(filtered, key = { it.itemId }) { stack ->
                    val def = RpgCatalog.itemById[stack.itemId] ?: return@items
                    val isEquipped = hud.selectedSeedId == stack.itemId || hud.selectedToolId == stack.itemId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .border(1.dp, if (isEquipped) RpgLeafAccent else RpgBorderWarm, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgAssetPixelImage(def.spritePath, def.name, Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${def.name}  x${stack.quantity}",
                                color = RpgCreamText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = def.description,
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SfProFontFamily
                            )
                        }

                        if (def.category == ItemCategory.SEEDS) {
                            SmallActionBtn(
                                text = if (hud.selectedSeedId == def.id) "Dipakai" else "Pilih Bibit",
                                bg = if (hud.selectedSeedId == def.id) Color(0xFF166534) else Color(0xFF0E7490)
                            ) { onSelectSeed(def.id) }
                        } else if (def.category == ItemCategory.TOOLS) {
                            SmallActionBtn(
                                text = if (hud.selectedToolId == def.id) "Aktif" else "Pakai",
                                bg = if (hud.selectedToolId == def.id) Color(0xFF166534) else Color(0xFF334155)
                            ) { onSelectTool(def.id) }
                        } else if (def.placeable) {
                            SmallActionBtn("Pasang", Color(0xFF15803D)) { onStartBuild(def.id) }
                        }

                        if (def.category != ItemCategory.TOOLS) {
                            SmallActionBtn("Buang", Color(0xFF7F1D1D)) { onDiscardItem(def.id) }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 2. MERCHANT & SELLING STATION MODAL ====================

@Composable
private fun MerchantModalContent(
    hud: RpgHudSnapshot,
    onDismiss: () -> Unit,
    onBuyItem: (String, Int) -> Unit,
    onSellItem: (String, Int) -> Unit,
    onSellAllFish: () -> Unit,
    onSellAllAll: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) } // 0 = Jual Hasil, 1 = Beli Toko

    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader("Toko & Stasiun Jual Willowbrook", "Jual tangkapan ikan & panen atau beli perlengkapan baru", hud.coins, onDismiss)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("Jual Hasil", tabIndex == 0) { tabIndex = 0 }
                TabChip("Beli Perlengkapan", tabIndex == 1) { tabIndex = 1 }
            }
            if (tabIndex == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallActionBtn("Jual Semua Ikan", Color(0xFF0E7490)) { onSellAllFish() }
                    SmallActionBtn("Jual Semua Hasil", Color(0xFFA16207)) { onSellAllAll() }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = RpgBorderWarm, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        if (tabIndex == 0) {
            val sellable = remember(hud.inventory) {
                hud.inventory.filter {
                    val d = RpgCatalog.itemById[it.itemId]
                    d != null && d.sellPrice > 0 && d.category != ItemCategory.TOOLS
                }
            }
            if (sellable.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada ikan, sayur, atau material yang siap dijual.",
                        color = RpgMutedText,
                        fontSize = 12.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sellable, key = { it.itemId }) { st ->
                        val def = RpgCatalog.itemById[st.itemId] ?: return@items
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RpgPanelElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RpgAssetPixelImage(def.spritePath, def.name, Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${def.name} x${st.quantity}",
                                    color = RpgCreamText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                                Text(
                                    text = "Harga satuan: ${def.sellPrice} koin (Total: ${def.sellPrice * st.quantity} koin)",
                                    color = RpgGoldAccent,
                                    fontSize = 11.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            SmallActionBtn("Jual 1", Color(0xFF15803D)) { onSellItem(def.id, 1) }
                            if (st.quantity > 1) {
                                SmallActionBtn("Jual Semua", Color(0xFFA16207)) { onSellItem(def.id, st.quantity) }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(RpgCatalog.shopItems, key = { it.id }) { def ->
                    val owned = hud.inventory.find { it.itemId == def.id }?.quantity ?: 0
                    val alreadyOwnedTool = def.category == ItemCategory.TOOLS && owned > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgAssetPixelImage(def.spritePath, def.name, Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = def.name + if (owned > 0) " (Tas: $owned)" else "",
                                color = RpgCreamText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = def.description,
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SfProFontFamily
                            )
                        }
                        if (alreadyOwnedTool) {
                            SmallActionBtn("Dimiliki", Color(0xFF3F3A36)) {}
                        } else {
                            val canAfford = hud.coins >= def.buyPrice
                            SmallActionBtn(
                                text = "${def.buyPrice} Koin",
                                bg = if (canAfford) RpgEmberAccent else Color(0xFF3F3A36)
                            ) { onBuyItem(def.id, 1) }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. CONSTRUCTION & HOUSE UPGRADE MODAL ====================

@Composable
private fun ConstructionModalContent(
    hud: RpgHudSnapshot,
    onDismiss: () -> Unit,
    onStartBuild: (String) -> Unit,
    onUpgradeHouse: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val woodQty = hud.inventory.find { it.itemId == "res_wood" }?.quantity ?: 0
    val stoneQty = hud.inventory.find { it.itemId == "res_stone" }?.quantity ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader("Konstruksi & Upgrade Rumah", "Kayu: $woodQty  •  Batu: $stoneQty", hud.coins, onDismiss)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip("Pasang Furnitur & Dekor", tabIndex == 0) { tabIndex = 0 }
            TabChip("Upgrade Rumah (Tier ${hud.houseTier}/3)", tabIndex == 1) { tabIndex = 1 }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = RpgBorderWarm, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        if (tabIndex == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(RpgCatalog.buildableItems, key = { it.id }) { def ->
                    val inBag = hud.inventory.find { it.itemId == def.id }?.quantity ?: 0
                    val canCraft = woodQty >= def.woodCost && stoneQty >= def.stoneCost
                    val ready = inBag > 0 || canCraft
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgAssetPixelImage(def.spritePath, def.name, Modifier.size(30.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = def.name,
                                color = RpgCreamText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = if (inBag > 0) "Tersedia di tas: $inBag (Gratis pasang)"
                                else "Bahan: ${def.woodCost} Kayu, ${def.stoneCost} Batu • ${def.description}",
                                color = if (ready) RpgMutedText else Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SfProFontFamily
                            )
                        }
                        SmallActionBtn(
                            text = "Pratinjau",
                            bg = if (ready) Color(0xFF15803D) else Color(0xFF3F3A36)
                        ) { onStartBuild(def.id) }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RpgCatalog.houseUpgrades, key = { it.tier }) { spec ->
                    val isCurrent = hud.houseTier == spec.tier
                    val isOwned = hud.houseTier >= spec.tier
                    val isNext = hud.houseTier + 1 == spec.tier
                    val canAfford = hud.coins >= spec.coinCost && woodQty >= spec.woodCost && stoneQty >= spec.stoneCost

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .border(1.dp, if (isCurrent) RpgGoldAccent else RpgBorderWarm, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RpgAssetPixelImage(
                            assetPath = "rpg/objects/house_tier${spec.tier}.png",
                            contentDescription = spec.name,
                            modifier = Modifier.size(64.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tier ${spec.tier}: ${spec.name}",
                                color = if (isCurrent) RpgGoldAccent else RpgCreamText,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = spec.description,
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                            if (!isOwned) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Biaya: ${spec.coinCost} Koin • ${spec.woodCost} Kayu • ${spec.stoneCost} Batu",
                                    color = RpgGoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                        when {
                            isCurrent -> SmallActionBtn("Aktif", Color(0xFF166534)) {}
                            isOwned -> SmallActionBtn("Selesai", Color(0xFF3F3A36)) {}
                            isNext -> SmallActionBtn(
                                text = "Upgrade",
                                bg = if (canAfford) RpgEmberAccent else Color(0xFF3F3A36)
                            ) { onUpgradeHouse() }
                            else -> SmallActionBtn("Terkunci", Color(0xFF2D2722)) {}
                        }
                    }
                }
            }
        }
    }
}

// ==================== 4. QUESTS, ACHIEVEMENTS & FISH ALMANAC ====================

@Composable
private fun QuestsAndAlmanacContent(
    hud: RpgHudSnapshot,
    onDismiss: () -> Unit,
    onClaimQuest: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader(
            title = "Misi, Pencapaian & Koleksi Ikan",
            subtitle = "Pancing Lv.${hud.fishingLevel} • Tani Lv.${hud.farmingLevel} • Kumpul Lv.${hud.gatheringLevel}",
            coins = hud.coins,
            onDismiss = onDismiss
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabChip("Misi Harian", tabIndex == 0) { tabIndex = 0 }
            TabChip("Koleksi Ikan (${hud.caughtFishCounts.size}/${RpgCatalog.fishSpecies.size})", tabIndex == 1) { tabIndex = 1 }
            TabChip("Pencapaian", tabIndex == 2) { tabIndex = 2 }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = RpgBorderWarm, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        when (tabIndex) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hud.quests, key = { it.def.id }) { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${q.def.title} (${q.progress}/${q.def.targetCount})",
                                color = RpgCreamText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "${q.def.description} • Hadiah: ${q.def.coinReward} Koin + ${q.def.expReward} EXP",
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                        when {
                            q.claimed -> SmallActionBtn("Diklaim", Color(0xFF3F3A36)) {}
                            q.isCompleted -> SmallActionBtn("Klaim", Color(0xFF15803D)) { onClaimQuest(q.def.id) }
                            else -> SmallActionBtn("${(q.progress * 100 / q.def.targetCount.coerceAtLeast(1))}%", Color(0xFF2D2722)) {}
                        }
                    }
                }
            }

            1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(RpgCatalog.fishSpecies, key = { it.id }) { fish ->
                    val caught = hud.caughtFishCounts[fish.id] ?: 0
                    val discovered = caught > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgAssetPixelImage(
                            assetPath = fish.spritePath,
                            contentDescription = fish.name,
                            modifier = Modifier
                                .size(width = 36.dp, height = 24.dp)
                                .alpha(if (discovered) 1f else 0.25f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (discovered) fish.name else "??? (${fish.habitat.label})",
                                    color = RpgCreamText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                                Text(
                                    text = fish.rarity.label,
                                    color = Color(fish.rarity.colorHex),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            Text(
                                text = if (discovered) "${fish.description} • ${fish.sellPrice} koin" else "Belum tertangkap. Pancing di ${fish.habitat.label}.",
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = SfProFontFamily
                            )
                        }
                        Text(
                            text = if (discovered) "x$caught" else "-",
                            color = if (discovered) RpgGoldAccent else RpgMutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hud.achievements, key = { it.def.id }) { ach ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RpgPanelElevated)
                            .border(1.dp, if (ach.unlocked) RpgGoldAccent else RpgBorderWarm, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.def.title,
                                color = if (ach.unlocked) RpgGoldAccent else RpgCreamText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "${ach.def.description} (${ach.progress}/${ach.def.targetCount})",
                                color = RpgMutedText,
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                        Text(
                            text = if (ach.unlocked) "Terbuka" else "+${ach.def.coinReward}k",
                            color = if (ach.unlocked) RpgLeafAccent else RpgMutedText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }
    }
}

// ==================== 5. CHARACTER CUSTOMIZATION MODAL ====================

@Composable
private fun CustomizationModalContent(
    hud: RpgHudSnapshot,
    onDismiss: () -> Unit,
    onCustomize: (PlayerCustomization) -> Unit
) {
    val swatches = listOf(
        "Teal Nelayan" to 0xFF48A298L,
        "Hijau Hutan" to 0xFF56A854L,
        "Beri Senja" to 0xFFD95B66L,
        "Biru Sungai" to 0xFF4B8CD9L,
        "Emas Panen" to 0xFFE6AD4CL,
        "Ungu Lavender" to 0xFF9E74D1L
    )
    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader("Kustomisasi Karakter", "Pilih nuansa warna pakaian karakter Willowbrook", hud.coins, onDismiss)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RpgPanelElevated)
                    .border(1.5.dp, Color(hud.customization.shirtColorHex), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                RpgAssetPixelImage(
                    assetPath = "rpg/characters/frames/idle_down_0.png",
                    contentDescription = "Karakter",
                    modifier = Modifier.size(64.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih Warna Nuansa Baju",
                    color = RpgCreamText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(swatches) { (name, hex) ->
                        val selected = hud.customization.shirtColorHex == hex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                onCustomize(hud.customization.copy(shirtColorHex = hex))
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else RpgBorderWarm, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = if (selected) RpgCreamText else RpgMutedText,
                                fontSize = 10.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 6. SETTINGS & PAUSE MODAL ====================

@Composable
private fun SettingsModalContent(
    hud: RpgHudSnapshot,
    settings: RpgAudioVideoSettings,
    onDismiss: () -> Unit,
    onSfxChange: (Float) -> Unit,
    onBgmChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onManualSave: () -> Unit,
    onExitGame: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ModalHeader("Pengaturan & Jeda", "Willowbrook Cove • Hari ${hud.dayCount}", hud.coins, onDismiss)
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = RpgBorderWarm, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingSliderRow("Volume Efek Suara (SFX)", settings.sfxVolume, 0f..1f, onSfxChange)
            }
            item {
                SettingSliderRow("Volume Musik Latar (BGM)", settings.bgmVolume, 0f..1f, onBgmChange)
            }
            item {
                SettingSliderRow("Zoom Kamera", settings.cameraZoom, 0.75f..1.45f, onZoomChange)
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RpgPanelElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Efek Cuaca Hujan Dinamis",
                        color = RpgCreamText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Switch(
                        checked = settings.weatherEnabled,
                        onCheckedChange = onWeatherToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = RpgLeafAccent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF15803D))
                    .clickable { onManualSave() },
                contentAlignment = Alignment.Center
            ) {
                Text("Simpan Progres", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF991B1B))
                    .clickable { onExitGame() },
                contentAlignment = Alignment.Center
            ) {
                Text("Kembali ke Yuranime", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RpgPanelElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = RpgCreamText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = SfProFontFamily)
            Text("${(value * 100).toInt()}%", color = RpgGoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = RpgGoldAccent, activeTrackColor = RpgEmberAccent)
        )
    }
}

@Composable
private fun SmallActionBtn(text: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
    }
}
