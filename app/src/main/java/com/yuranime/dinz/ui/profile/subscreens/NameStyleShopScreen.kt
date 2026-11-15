package com.yuranime.dinz.ui.profile.subscreens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.NameStyleColorSwatch
import com.yuranime.dinz.data.firebase.NameStyleDefinition
import com.yuranime.dinz.data.firebase.NameStyleTier
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.components.StyledUsernameText
import com.yuranime.dinz.ui.components.parseNameStyleColor
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

/**
 * Halaman YuraStyle / Style Nama (`NameStyleShopScreen`)
 * Mengikuti struktur WibiShop yang diadaptasi ke identitas obsidian & Primogem Yuranime:
 * - Diakses khusus melalui Edit Profil
 * - Panah kembali `<` bersih tanpa bulatan/bayangan overlay
 * - Watermark logo `ic_yuranime_monochrome` `alpha = 0.07f` menyatu ke background `#0B0B0E`
 * - Hanya 1 kapsul Primo Poin di kanan atas (tanpa koin)
 * - 8 pilihan warna bebas & 8 style (6 Mythic @ 4.500 + 2 Legendary @ 2.000)
 */
@Composable
fun NameStyleShopScreen(
    onBackClick: () -> Unit,
    onNavigateToRewards: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profile by FirebaseManager.userProfile.collectAsState()
    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: "Member Yuranime"
    val userPoints = profile?.points ?: 0L
    val equippedStyleId = profile?.equippedNameStyle.orEmpty()
    val equippedColorHex = profile?.equippedNameColor.orEmpty()
    val unlockedStyleIds = profile?.unlockedNameStyles.orEmpty()

    val mythicStyles = remember {
        FirebaseManager.NAME_STYLE_CATALOG.filter { it.tier == NameStyleTier.MYTHIC }
    }
    val legendaryStyles = remember {
        FirebaseManager.NAME_STYLE_CATALOG.filter { it.tier == NameStyleTier.LEGENDARY }
    }

    var selectedStyleId by remember {
        mutableStateOf(equippedStyleId.ifBlank { "ruby" })
    }
    var selectedColorHex by remember {
        mutableStateOf(
            equippedColorHex.ifBlank {
                FirebaseManager.NAME_STYLE_SWATCHES.first().hex
            }
        )
    }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(equippedStyleId, equippedColorHex) {
        if (equippedStyleId.isNotBlank() && selectedStyleId.isBlank()) {
            selectedStyleId = equippedStyleId
        }
        if (equippedColorHex.isNotBlank() && selectedColorHex.isBlank()) {
            selectedColorHex = equippedColorHex
        }
    }

    val selectedStyle = remember(selectedStyleId) {
        FirebaseManager.resolveNameStyleDefinition(selectedStyleId)
            ?: FirebaseManager.NAME_STYLE_CATALOG.first()
    }

    val ownedCount = remember(unlockedStyleIds) {
        FirebaseManager.NAME_STYLE_CATALOG.count { def ->
            unlockedStyleIds.any { it.equals(def.id, ignoreCase = true) }
        }
    }
    val isSelectedOwned = remember(selectedStyle.id, unlockedStyleIds) {
        unlockedStyleIds.any { it.equals(selectedStyle.id, ignoreCase = true) }
    }
    val isSelectedEquipped = selectedStyle.id.equals(equippedStyleId, ignoreCase = true)
    val canAfford = userPoints >= selectedStyle.pointCost
    val shortfall = (selectedStyle.pointCost - userPoints).coerceAtLeast(0L)
    val selectedColor = remember(selectedColorHex, selectedStyle.id) {
        parseNameStyleColor(selectedColorHex, selectedStyle.id)
    }

    Scaffold(
        containerColor = Color(0xFF0B0B0E),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1A1D28),
                    contentColor = Color.White,
                    actionColor = Color(0xFFFF5722),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        bottomBar = {
            NameStyleBottomActionBar(
                style = selectedStyle,
                isOwned = isSelectedOwned,
                isEquipped = isSelectedEquipped,
                canAfford = canAfford,
                shortfall = shortfall,
                userPoints = userPoints,
                isProcessing = isProcessing,
                onNavigateToRewards = onNavigateToRewards,
                onPrimaryClick = {
                    if (isProcessing) return@NameStyleBottomActionBar
                    if (!isSelectedOwned) {
                        if (!canAfford) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Poin kurang ${FirebaseManager.formatNumber(shortfall)}. Kumpulkan lewat misi Rewards!"
                                )
                            }
                            return@NameStyleBottomActionBar
                        }
                        isProcessing = true
                        FirebaseManager.purchaseAndEquipNameStyle(
                            context = context,
                            styleDefinition = selectedStyle,
                            colorHex = selectedColorHex
                        ) { _, msg ->
                            isProcessing = false
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    } else if (isSelectedEquipped) {
                        isProcessing = true
                        FirebaseManager.equipNameStyle(
                            context = context,
                            styleId = "",
                            colorHex = selectedColorHex
                        ) { _, msg ->
                            isProcessing = false
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    } else {
                        isProcessing = true
                        FirebaseManager.equipNameStyle(
                            context = context,
                            styleId = selectedStyle.id,
                            colorHex = selectedColorHex
                        ) { _, msg ->
                            isProcessing = false
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0E))
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Watermark logo `ic_yuranime_monochrome` tipis menyatu ke background (tanpa kotak/bayangan)
            Image(
                painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .size(188.dp)
                    .offset(x = 28.dp, y = (-12).dp)
                    .rotate(-14f)
                    .alpha(0.07f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // 1. HEADER ATAS: `<` bersih tanpa bulatan overlay + Judul + 1 Kapsul Primo Poin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBackClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "YuraStyle",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.weight(1f)
                    )

                    // Hanya 1 kapsul saldo Poin Primogem (tanpa koin)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141720))
                            .border(1.dp, Color(0xFF232838), RoundedCornerShape(20.dp))
                            .clickable(onClick = onNavigateToRewards)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimoPointIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = FirebaseManager.formatNumber(userPoints),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // 2. AREA LIVE PREVIEW NAMA & PEMILIH 8 WARNA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge MILIKMU + [Owned] dari 8
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE11D48))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "MILIKMU",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                                fontFamily = SfProFontFamily,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$ownedCount dari ${FirebaseManager.NAME_STYLE_CATALOG.size}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )
                    }

                    // Live Preview Nama Asli 34.sp Bold 60fps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 10.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            selectedColor.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = size.maxDimension * 0.42f
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        StyledUsernameText(
                            name = displayName,
                            styleId = selectedStyle.id,
                            colorHex = selectedColorHex,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Nama style + Badge Tier
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = selectedStyle.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NameStyleTierPill(tier = selectedStyle.tier)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Deretan 8 Lingkaran Pemilih Warna (bebas dipilih setelah style dibeli/dipilih)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FirebaseManager.NAME_STYLE_SWATCHES.forEach { swatch ->
                            val isColorSelected = selectedColorHex.equals(swatch.hex, ignoreCase = true)
                            ColorSwatchCircle(
                                swatch = swatch,
                                isSelected = isColorSelected,
                                onClick = {
                                    selectedColorHex = swatch.hex
                                    if (isSelectedEquipped) {
                                        FirebaseManager.equipNameStyle(
                                            context = context,
                                            styleId = selectedStyle.id,
                                            colorHex = swatch.hex
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = Color(0xFF1A1D28)
                )

                // 3. DAFTAR 8 STYLE NAMA (MYTHIC 6 + LEGENDARY 2)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp)
                ) {
                    item(key = "header_mythic") {
                        NameStyleSectionHeader(
                            title = "MYTHIC",
                            count = mythicStyles.size,
                            accentColor = Color(0xFFE11D48)
                        )
                    }

                    items(items = mythicStyles, key = { it.id }) { style ->
                        val isOwned = unlockedStyleIds.any { it.equals(style.id, ignoreCase = true) }
                        val isEquipped = style.id.equals(equippedStyleId, ignoreCase = true)
                        val isSelected = style.id.equals(selectedStyle.id, ignoreCase = true)
                        NameStyleCatalogRow(
                            style = style,
                            displayName = displayName,
                            selectedColorHex = selectedColorHex,
                            isOwned = isOwned,
                            isEquipped = isEquipped,
                            isSelected = isSelected,
                            onClick = { selectedStyleId = style.id }
                        )
                    }

                    item(key = "header_legendary") {
                        Spacer(modifier = Modifier.height(8.dp))
                        NameStyleSectionHeader(
                            title = "LEGENDARY",
                            count = legendaryStyles.size,
                            accentColor = Color(0xFFFACC15)
                        )
                    }

                    items(items = legendaryStyles, key = { it.id }) { style ->
                        val isOwned = unlockedStyleIds.any { it.equals(style.id, ignoreCase = true) }
                        val isEquipped = style.id.equals(equippedStyleId, ignoreCase = true)
                        val isSelected = style.id.equals(selectedStyle.id, ignoreCase = true)
                        NameStyleCatalogRow(
                            style = style,
                            displayName = displayName,
                            selectedColorHex = selectedColorHex,
                            isOwned = isOwned,
                            isEquipped = isEquipped,
                            isSelected = isSelected,
                            onClick = { selectedStyleId = style.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameStyleTierPill(tier: NameStyleTier) {
    val bgBrush = when (tier) {
        NameStyleTier.MYTHIC -> Brush.horizontalGradient(
            colors = listOf(Color(0xFF9333EA), Color(0xFFE11D48))
        )
        NameStyleTier.LEGENDARY -> Brush.horizontalGradient(
            colors = listOf(Color(0xFFE11D48), Color(0xFFF59E0B))
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgBrush)
            .padding(horizontal = 8.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = tier.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

@Composable
private fun ColorSwatchCircle(
    swatch: NameStyleColorSwatch,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Warna ${swatch.label}" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(swatch.colorValue))
                    .border(
                        width = if (swatch.hex.equals("#FFFFFF", ignoreCase = true) && !isSelected) 0.75.dp else 0.dp,
                        color = Color(0x44FFFFFF),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun NameStyleSectionHeader(
    title: String,
    count: Int,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            fontFamily = SfProFontFamily
        )
        Text(
            text = count.toString(),
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun NameStyleCatalogRow(
    style: NameStyleDefinition,
    displayName: String,
    selectedColorHex: String,
    isOwned: Boolean,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tierAccent = if (style.tier == NameStyleTier.MYTHIC) {
        Color(0xFFE11D48)
    } else {
        Color(0xFFFACC15)
    }
    val rowBg by animateColorAsState(
        targetValue = if (isSelected) tierAccent.copy(alpha = 0.13f) else Color.Transparent,
        label = "nameStyleRowBg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Garis aksen vertikal tier di kiri
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isSelected) tierAccent else tierAccent.copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Nama Style tetap 80.dp agar kolom live preview rata presisi
            Text(
                text = style.name,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(80.dp)
            )

            // Live Animated Preview Nama Pengguna
            StyledUsernameText(
                name = displayName,
                styleId = style.id,
                colorHex = selectedColorHex,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )

            // Kanan: Terpasang / Dimiliki / Harga Primo Poin (tanpa Koin)
            when {
                isEquipped -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x28E11D48))
                            .border(0.75.dp, Color(0xFFE11D48), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = "Terpasang",
                            color = Color(0xFFFDA4AF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
                isOwned -> {
                    Text(
                        text = "Dimiliki",
                        color = Color(0xFF4ADE80),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimoPointIcon(size = 15.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FirebaseManager.formatNumber(style.pointCost),
                            color = Color(0xFFFACC15),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFF161923),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun NameStyleBottomActionBar(
    style: NameStyleDefinition,
    isOwned: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    shortfall: Long,
    userPoints: Long,
    isProcessing: Boolean,
    onNavigateToRewards: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1016))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.75.dp, color = Color(0xFF1B1F2C))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val buttonModifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))

            val buttonBackground: Modifier = when {
                isOwned && isEquipped -> {
                    buttonModifier
                        .background(Color(0xFF1A1E29))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(25.dp))
                }
                isOwned || canAfford -> {
                    buttonModifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE11D48), Color(0xFFFF5722))
                        )
                    )
                }
                else -> {
                    buttonModifier
                        .background(Color(0xFF1C202B))
                        .border(1.dp, Color(0xFF2B3142), RoundedCornerShape(25.dp))
                }
            }

            Box(
                modifier = buttonBackground.clickable(enabled = !isProcessing, onClick = onPrimaryClick),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (!isOwned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Beli",
                            color = if (canAfford) Color.White else Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PrimoPointIcon(size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = FirebaseManager.formatNumber(style.pointCost),
                            color = if (canAfford) Color.White else Color(0xFFFACC15),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                } else {
                    Text(
                        text = if (isEquipped) "Lepas Style Nama" else "Pasang Style Nama",
                        color = if (isEquipped) Color(0xFFE2E8F0) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            if (!isOwned && !canAfford) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kurang ${FirebaseManager.formatNumber(shortfall)} Poin • Dapatkan di Rewards",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.clickable(onClick = onNavigateToRewards)
                )
            } else if (!isOwned && canAfford) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saldo saat ini: ${FirebaseManager.formatNumber(userPoints)} Primo Poin",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}
