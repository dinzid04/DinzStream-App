package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.BorderDefinition
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import com.yuranime.dinz.data.firebase.TitleDefinition
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onNavigateToRewards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authDb = remember { LocalAuthDatabase(context) }
    val currentProfile by FirebaseManager.userProfile.collectAsState()

    val level = currentProfile?.level ?: 1
    val points = currentProfile?.points ?: 0L
    val unlockedTitles = currentProfile?.unlockedTitles ?: listOf("WARGA YURANIME")
    val equippedTitle = currentProfile?.equippedTitle.orEmpty()
    val equippedBorderUrl = currentProfile?.equippedBorderUrl.orEmpty()
    val equippedProfileEffectUrl = currentProfile?.equippedProfileEffectUrl.orEmpty()
    val unlockedBorders = currentProfile?.unlockedBorders ?: emptyList()
    val unlockedEffects = currentProfile?.unlockedEffects ?: emptyList()
    val userAvatarUrl = currentProfile?.avatar ?: authDb.getAvatarUrl()

    // 3 Tab: 0 = Profile Effect, 1 = Avatar Decoration, 2 = Milestone & Title
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf("Semua") } // Semua, Koleksiku, Starter, Lv. 35+, Limited Redeem

    // State untuk Sheet / Dialog Preview
    var previewBorderItem by remember { mutableStateOf<BorderDefinition?>(null) }
    var previewEffectItem by remember { mutableStateOf<ProfileEffectDefinition?>(null) }
    var isProcessingAction by remember { mutableStateOf(false) }

    // Memuat seluruh 396+ Profile Effect & 985+ Avatar Decoration TANPA BATAS 60!
    val allEffects = remember(context) { DiscordCosmeticsRepository.getAllProfileEffects(context) }
    val allBorders = remember(context) { DiscordCosmeticsRepository.getAllBorders(context) }

    // Filter daftar Profile Effect
    val filteredEffects = remember(allEffects, unlockedEffects, equippedProfileEffectUrl, searchQuery, selectedTierFilter) {
        allEffects.filter { effect ->
            val matchSearch = searchQuery.isBlank() ||
                effect.name.contains(searchQuery, ignoreCase = true) ||
                effect.category.contains(searchQuery, ignoreCase = true)
            val isOwned = unlockedEffects.contains(effect.id) || effect.animationUrl == equippedProfileEffectUrl
            val matchFilter = when (selectedTierFilter) {
                "Koleksiku" -> isOwned
                "Starter" -> effect.tier == "A"
                "Lv. 35+" -> effect.tier == "B"
                "Limited Redeem" -> effect.tier == "C"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    // Filter daftar Avatar Decoration
    val filteredBorders = remember(allBorders, unlockedBorders, equippedBorderUrl, searchQuery, selectedTierFilter) {
        allBorders.filter { border ->
            val matchSearch = searchQuery.isBlank() ||
                border.name.contains(searchQuery, ignoreCase = true) ||
                border.category.contains(searchQuery, ignoreCase = true)
            val isOwned = unlockedBorders.contains(border.id) || border.imageUrl == equippedBorderUrl
            val matchFilter = when (selectedTierFilter) {
                "Koleksiku" -> isOwned
                "Starter" -> border.tier == "A"
                "Lv. 35+" -> border.tier == "B"
                "Limited Redeem" -> border.tier == "C"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ========================================================
        // 1. HEADER ATAS (Persis 1000127784.png)
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Sisi Kiri: Ikon Panah Bersih (Tanpa Bulatan Background) + Judul & Subteks
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "Shop",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Customize your profile",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Sisi Kanan: Kapsul Poin Pengguna + Siluet Logo App Transparan Lembut
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Siluet Logo Aplikasi dengan opasitas rendah & pendaran lembut
                Icon(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier
                        .size(68.dp)
                        .offset(x = 12.dp, y = (-2).dp)
                        .alpha(0.14f)
                )

                // Kapsul Poin Pengguna dengan PrimoPointIcon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141722))
                        .border(1.dp, Color(0xFF262A38), RoundedCornerShape(20.dp))
                        .clickable(onClick = onNavigateToRewards)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimoPointIcon(size = 18.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${FirebaseManager.formatNumber(points)} Poin",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        // ========================================================
        // 2. 3 TAB KATEGORI KAPSUL
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                "Profile Effect" to allEffects.size,
                "Avatar Decoration" to allBorders.size,
                "Milestone & Title" to FirebaseManager.ALL_TITLES.size
            )

            tabs.forEachIndexed { index, (title, count) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFFE11D48) else Color(0xFF13151F))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFFF4D6D) else Color(0xFF212534),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ========================================================
        // 3. SEARCH BAR & FILTER TIER CHIPS (Untuk Tab 0 & 1)
        // ========================================================
        if (selectedTab != 2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "Cari dari 396 efek profil..." else "Cari dari 985 border avatar...",
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp,
                            fontFamily = SfProFontFamily
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF10121B),
                        unfocusedContainerColor = Color(0xFF0D0F16),
                        focusedBorderColor = Color(0xFFE11D48),
                        unfocusedBorderColor = Color(0xFF1E2232)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterChips = listOf("Semua", "Koleksiku", "Starter", "Lv. 35+", "Limited Redeem")
                    items(filterChips) { chip ->
                        val isSelected = selectedTierFilter == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFF12141E))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFE11D48) else Color(0xFF222636),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTierFilter = chip }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = chip,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // 4. KONTEN TAB UTAMA
        // ========================================================
        when (selectedTab) {
            // TAB 0: PROFILE EFFECT (396 EFEK — MOCKUP HP MINIMALIS DI GRID)
            0 -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredEffects,
                        key = { it.id }
                    ) { effect ->
                        val isEquipped = effect.animationUrl == equippedProfileEffectUrl
                        val isOwned = unlockedEffects.contains(effect.id)
                        val isLevelLocked = effect.tier == "B" && level < effect.requiredLevel

                        ShopProfileEffectMockupCard(
                            effect = effect,
                            userAvatarUrl = userAvatarUrl,
                            isEquipped = isEquipped,
                            isOwned = isOwned,
                            isLevelLocked = isLevelLocked,
                            onClick = { previewEffectItem = effect }
                        )
                    }
                }
            }

            // TAB 1: AVATAR DECORATION (985 BORDER DISCORD LENGKAP TANPA BATAS)
            1 -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = filteredBorders,
                        key = { it.id }
                    ) { border ->
                        val isEquipped = border.imageUrl == equippedBorderUrl
                        val isOwned = unlockedBorders.contains(border.id)
                        val isLevelLocked = border.tier == "B" && level < border.requiredLevel

                        ShopAvatarBorderCard(
                            border = border,
                            userAvatarUrl = userAvatarUrl,
                            isEquipped = isEquipped,
                            isOwned = isOwned,
                            isLevelLocked = isLevelLocked,
                            onClick = { previewBorderItem = border }
                        )
                    }
                }
            }

            // TAB 2: MILESTONE & TITLE (TAMPILKAN KOMPONEN TITLE LANGSUNG TANPA KOTAK SHADOW)
            2 -> {
                val allTitles = remember { FirebaseManager.ALL_TITLES }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Koleksi Custom Title Geometris",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "Buka gelar kasta rank dari pencapaian level atau tukarkan dengan poin.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(allTitles, key = { it.id }) { titleDef ->
                        val isOwned = unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                        val isEquipped = equippedTitle.equals(titleDef.titleName, ignoreCase = true)
                        val isLevelLocked = level < titleDef.requiredLevel

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10121B))
                                .border(1.dp, Color(0xFF1E2230), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tampilkan langsung komponen GeometricTitleBadge tanpa card shadow
                            Box(modifier = Modifier.weight(1f)) {
                                Column {
                                    GeometricTitleBadge(
                                        titleName = titleDef.titleName,
                                        tierLevel = titleDef.tierLevel,
                                        styleVariant = titleDef.styleVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = titleDef.description,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Tombol Aksi
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isEquipped -> Color(0xFFE11D48)
                                            isOwned -> Color(0xFF10B981)
                                            isLevelLocked -> Color(0xFF181B26)
                                            else -> Color(0xFFFF5722)
                                        }
                                    )
                                    .clickable {
                                        if (isEquipped) return@clickable
                                        if (isOwned) {
                                            FirebaseManager.equipTitle(context, titleDef) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else if (isLevelLocked) {
                                            Toast.makeText(context, "Capai Level ${titleDef.requiredLevel} untuk membuka Title ini!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            FirebaseManager.unlockAndEquipShopTitle(context, titleDef) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isEquipped -> {
                                        Text(text = "Terpasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    isOwned -> {
                                        Text(text = "Pasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    isLevelLocked -> {
                                        Text(text = "🔒 Lv. ${titleDef.requiredLevel}", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    else -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PrimoPointIcon(size = 13.dp)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(text = "${titleDef.pointCost}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================
    // 5. PREVIEW PROFILE EFFECT (LAYAR PENUH PERSIS 1000127821.jpg)
    // ========================================================
    previewEffectItem?.let { effect ->
        val isOwned = unlockedEffects.contains(effect.id)
        val isEquipped = effect.animationUrl == equippedProfileEffectUrl
        val isLevelLocked = effect.tier == "B" && level < effect.requiredLevel

        Dialog(
            onDismissRequest = { previewEffectItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0D13))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Atas: Tombol ✕ + Preview Effect Title & Tier Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { previewEffectItem = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Preview Effect",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = effect.name,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Badge Tier
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (effect.tier) {
                                    "C" -> Color(0xFFE11D48)
                                    "B" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF3B82F6)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (effect.tier) {
                                "C" -> "Limited"
                                "B" -> "Lv. 35+"
                                else -> "Starter"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // Area Tengah: Animasi Penuh Profile Effect 60 FPS + Avatar User
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    // Animasi Profile Effect 60 FPS di background avatar
                    AnimatedCosmeticImage(
                        url = effect.animationUrl,
                        isAnimated = true,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(320.dp)
                    )

                    // Avatar Bulat Pengguna di Tengah
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = "Avatar Pengguna",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = "YU",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Area Bawah: Kapsul Aksi & Harga Poin
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                when {
                                    isEquipped -> Color(0xFFE11D48)
                                    isOwned -> Color(0xFF10B981)
                                    isLevelLocked -> Color(0xFF1E2232)
                                    effect.tier == "C" -> Color(0xFF7C3AED)
                                    else -> Color(0xFFE11D48)
                                }
                            )
                            .clickable {
                                if (isProcessingAction) return@clickable
                                isProcessingAction = true
                                coroutineScope.launch {
                                    try {
                                        when {
                                            isEquipped -> {
                                                Toast.makeText(context, "Efek ini sudah terpasang!", Toast.LENGTH_SHORT).show()
                                            }
                                            isLevelLocked -> {
                                                Toast.makeText(context, "Capai Level ${effect.requiredLevel} terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                            }
                                            effect.tier == "C" && !isOwned -> {
                                                Toast.makeText(context, "Efek ini eksklusif dari Kode Redeem!", Toast.LENGTH_SHORT).show()
                                                onNavigateToRewards()
                                                previewEffectItem = null
                                            }
                                            else -> {
                                                FirebaseManager.unlockOrEquipProfileEffect(context, effect) { _, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    previewEffectItem = null
                                                }
                                            }
                                        }
                                    } finally {
                                        isProcessingAction = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessingAction) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    isEquipped -> {
                                        Text(text = "Sedang Terpasang", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isOwned -> {
                                        Text(text = "Pasang Efek Ini", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isLevelLocked -> {
                                        Text(text = "🔒 Terkunci (Minimal Level ${effect.requiredLevel})", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    effect.tier == "C" -> {
                                        Text(text = "🔒 Eksklusif Kode Redeem", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    else -> {
                                        PrimoPointIcon(size = 20.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tukar & Pasang • ${FirebaseManager.formatNumber(effect.pointCost)} Poin",
                                            color = Color.White,
                                            fontSize = 15.sp,
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
        }
    }

    // ========================================================
    // 6. PREVIEW AVATAR DECORATION (BOTTOM SHEET PERSIS 1000127813.jpg)
    // ========================================================
    previewBorderItem?.let { border ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isOwned = unlockedBorders.contains(border.id)
        val isEquipped = border.imageUrl == equippedBorderUrl
        val isLevelLocked = border.tier == "B" && level < border.requiredLevel

        ModalBottomSheet(
            onDismissRequest = { previewBorderItem = null },
            sheetState = sheetState,
            containerColor = Color(0xFF10121A),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle baris kecil
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2C3246))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tier Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (border.tier) {
                                "C" -> Color(0xFFE11D48)
                                "B" -> Color(0xFFF59E0B)
                                else -> Color(0xFF3B82F6)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (border.tier) {
                            "C" -> "LIMITED REDEEM"
                            "B" -> "LEVEL 35+ EXCLUSIVE"
                            else -> "STARTER TIER"
                        },
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = border.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Preview di profil kamu",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Preview Avatar Bulat + Animasi Live Avatar Decoration APNG 60 FPS
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Foto Profil Bulat Pengguna (86.dp)
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = "Avatar Pengguna",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(text = "YU", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Border Avatar Decoration APNG Beranimasi 60 FPS (108.dp)
                    AnimatedCosmeticImage(
                        url = border.imageUrl,
                        isAnimated = true,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(108.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Aksi Kapsul
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(
                            when {
                                isEquipped -> Color(0xFFE11D48)
                                isOwned -> Color(0xFF10B981)
                                isLevelLocked -> Color(0xFF1E2232)
                                border.tier == "C" -> Color(0xFF7C3AED)
                                else -> Color(0xFFE11D48)
                            }
                        )
                        .clickable {
                            if (isProcessingAction) return@clickable
                            isProcessingAction = true
                            coroutineScope.launch {
                                try {
                                    when {
                                        isEquipped -> {
                                            Toast.makeText(context, "Border ini sudah terpasang!", Toast.LENGTH_SHORT).show()
                                        }
                                        isLevelLocked -> {
                                            Toast.makeText(context, "Capai Level ${border.requiredLevel} terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                        }
                                        border.tier == "C" && !isOwned -> {
                                            Toast.makeText(context, "Border ini khusus dari Kode Redeem!", Toast.LENGTH_SHORT).show()
                                            onNavigateToRewards()
                                            previewBorderItem = null
                                        }
                                        else -> {
                                            FirebaseManager.unlockOrEquipBorder(context, border) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                previewBorderItem = null
                                            }
                                        }
                                    }
                                } finally {
                                    isProcessingAction = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessingAction) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when {
                                isEquipped -> {
                                    Text(text = "Sedang Terpasang", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                }
                                isOwned -> {
                                    Text(text = "Pasang Sekarang", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                }
                                isLevelLocked -> {
                                    Text(text = "🔒 Terkunci (Minimal Level ${border.requiredLevel})", color = Color(0xFF94A3B8), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                }
                                border.tier == "C" -> {
                                    Text(text = "🔒 Eksklusif Kode Redeem", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                }
                                else -> {
                                    PrimoPointIcon(size = 18.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tukar & Pasang • ${FirebaseManager.formatNumber(border.pointCost)} Poin",
                                        color = Color.White,
                                        fontSize = 14.sp,
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
    }
}

/**
 * Kartu Grid 2 Kolom untuk Profile Effect dalam Mockup HP Minimalis (Scroll Instan 60 FPS!)
 */
@Composable
private fun ShopProfileEffectMockupCard(
    effect: ProfileEffectDefinition,
    userAvatarUrl: String,
    isEquipped: Boolean,
    isOwned: Boolean,
    isLevelLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val staticUrl = remember(effect.animationUrl) {
        DiscordCosmeticsRepository.getOptimizedUrl(effect.animationUrl, size = 160, animated = false)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10121B))
            .border(
                1.dp,
                if (isEquipped) Color(0xFFE11D48) else Color(0xFF1E2232),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mockup HP Minimalis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0B10))
                .border(1.dp, Color(0xFF232736), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Speaker bar di atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .size(width = 28.dp, height = 3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF262A38))
            )

            // Preview Frame Pertama Profile Effect (Ringan & Cepat!)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(staticUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = effect.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(110.dp)
            )

            // Avatar Mini di dalam mockup
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF181B26)),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(text = "YU", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tier Badge Kecil di Pojok Kiri Atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (effect.tier) {
                            "C" -> Color(0xFFE11D48)
                            "B" -> Color(0xFFF59E0B)
                            else -> Color(0xFF3B82F6)
                        }
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (effect.tier) {
                        "C" -> "Limited"
                        "B" -> "Lv. 35+"
                        else -> "Starter"
                    },
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nama Efek Profil
        Text(
            text = effect.name,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tombol Aksi Harga / Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFFE11D48)
                        isOwned -> Color(0xFF10B981)
                        isLevelLocked -> Color(0xFF181A24)
                        effect.tier == "C" -> Color(0xFF6D28D9)
                        else -> Color(0xFF1F2436)
                    }
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isEquipped -> {
                    Text(text = "Terpasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                isOwned -> {
                    Text(text = "Pasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                isLevelLocked -> {
                    Text(text = "🔒 Lv. ${effect.requiredLevel}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                effect.tier == "C" -> {
                    Text(text = "🔒 Redeem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimoPointIcon(size = 13.dp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${FirebaseManager.formatNumber(effect.pointCost)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Grid 2 Kolom untuk Avatar Decoration (Scroll Instan 60 FPS!)
 */
@Composable
private fun ShopAvatarBorderCard(
    border: BorderDefinition,
    userAvatarUrl: String,
    isEquipped: Boolean,
    isOwned: Boolean,
    isLevelLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val staticUrl = remember(border.imageUrl) {
        DiscordCosmeticsRepository.getOptimizedUrl(border.imageUrl, size = 160, animated = false)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF10121B))
            .border(
                1.dp,
                if (isEquipped) Color(0xFFE11D48) else Color(0xFF1E2232),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kontainer Avatar + Border Statis Cepat
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0B10))
                .border(1.dp, Color(0xFF232736), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Tier Badge Kecil di Pojok Kiri Atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (border.tier) {
                            "C" -> Color(0xFFE11D48)
                            "B" -> Color(0xFFF59E0B)
                            else -> Color(0xFF3B82F6)
                        }
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = when (border.tier) {
                        "C" -> "Limited"
                        "B" -> "Lv. 35+"
                        else -> "Starter"
                    },
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            // Foto Profil Bulat
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF181B26)),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(text = "YU", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Border Statis (Frame 1)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(staticUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = border.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(78.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nama Border
        Text(
            text = border.name,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tombol Aksi Harga / Status
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFFE11D48)
                        isOwned -> Color(0xFF10B981)
                        isLevelLocked -> Color(0xFF181A24)
                        border.tier == "C" -> Color(0xFF6D28D9)
                        else -> Color(0xFF1F2436)
                    }
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isEquipped -> {
                    Text(text = "Terpasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                isOwned -> {
                    Text(text = "Pasang", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                isLevelLocked -> {
                    Text(text = "🔒 Lv. ${border.requiredLevel}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                border.tier == "C" -> {
                    Text(text = "🔒 Redeem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimoPointIcon(size = 13.dp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${FirebaseManager.formatNumber(border.pointCost)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
