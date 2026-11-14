package com.yuranime.dinz.ui.profile.subscreens

import android.widget.ImageView
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.yuranime.dinz.ui.components.ApngAnimationView
import com.yuranime.dinz.ui.components.ApngStackedEffectView
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

/**
 * ShopScreen:
 * Halaman Shop Kosmetik Discord Resmi Yuranime sesuai referensi presisi (1000127784_2.png).
 * Menampilkan seluruh 396+ Profile Effects & 985+ Avatar Decoration Discord tanpa batasan,
 * layout satu kesatuan permukaan (single surface) tanpa overlay kotak ganda,
 * dan preview animasi 60 FPS hardware-accelerated.
 */
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

    // State untuk Dialog Preview
    var previewBorderItem by remember { mutableStateOf<BorderDefinition?>(null) }
    var previewEffectItem by remember { mutableStateOf<ProfileEffectDefinition?>(null) }
    var isProcessingAction by remember { mutableStateOf(false) }

    // Memuat seluruh 396+ Profile Effect & 985+ Avatar Decoration TANPA BATAS!
    val allEffects = remember(context) { DiscordCosmeticsRepository.getAllProfileEffects(context) }
    val allBorders = remember(context) { DiscordCosmeticsRepository.getAllBorders(context) }

    // Filter daftar Profile Effect
    val filteredEffects = remember(allEffects, searchQuery) {
        if (searchQuery.isBlank()) {
            allEffects
        } else {
            allEffects.filter { effect ->
                effect.name.contains(searchQuery, ignoreCase = true) ||
                    effect.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Filter daftar Avatar Decoration
    val filteredBorders = remember(allBorders, searchQuery) {
        if (searchQuery.isBlank()) {
            allBorders
        } else {
            allBorders.filter { border ->
                border.name.contains(searchQuery, ignoreCase = true) ||
                    border.category.contains(searchQuery, ignoreCase = true)
            }
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
        // 1. HEADER ATAS (Persis Desain 1000127784_2.png)
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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

            // Sisi Kanan: Kapsul Poin Pengguna + Siluet Logo App Transparan Lembut & Crimson Glow
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Sapuan cahaya merah crimson lembut di latar belakang
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .offset(x = 24.dp, y = (-12).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE11D48).copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Siluet Logo Aplikasi Monochrome (size 130.dp, alpha 0.12f, miring -15f)
                Icon(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier
                        .size(130.dp)
                        .offset(x = 20.dp, y = (-8).dp)
                        .rotate(-15f)
                        .alpha(0.12f)
                )

                // Kapsul Poin Pengguna dengan PrimoPointIcon 24.dp & Poin 15.sp Bold
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141722))
                        .border(1.dp, Color(0xFF262A38), RoundedCornerShape(22.dp))
                        .clickable(onClick = onNavigateToRewards)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimoPointIcon(size = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FirebaseManager.formatNumber(points),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ========================================================
        // 2. 3 TAB KATEGORI KAPSUL DENGAN SPACING LEGA
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tabs = listOf(
                "Profile Effect" to allEffects.size,
                "Avatar Decoration" to allBorders.size,
                "Milestone & Title" to FirebaseManager.ALL_TITLES.size
            )

            tabs.forEachIndexed { index, (title, _) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFFE11D48) else Color(0xFF13151F))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFFF4D6D) else Color(0xFF212534),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ========================================================
        // 3. SEARCH BAR VERTICALLY CENTERED (Bebas Teks Terpotong)
        // ========================================================
        if (selectedTab != 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(Color(0xFF12141C), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF1F2330), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontFamily = SfProFontFamily
                        ),
                        cursorBrush = SolidColor(Color(0xFFE11D48)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (selectedTab == 0) "Cari dari 396 efek profil..." else "Cari dari 985 border avatar...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // ========================================================
        // 4. KONTEN TAB UTAMA
        // ========================================================
        when (selectedTab) {
            // TAB 0: PROFILE EFFECT (396 EFEK ASLI DISCORD)
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
                        val isOwned = unlockedEffects.contains(effect.id) || isEquipped
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
                        val isOwned = unlockedBorders.contains(border.id) || isEquipped
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

            // TAB 2: MILESTONE & TITLE (TAMPILKAN KOMPONEN TITLE LANGSUNG)
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF12131A))
                                .border(1.dp, Color(0xFF1F2330), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                            // Tombol Aksi (Crimson Red / Permukaan Gelap untuk Terpasang)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isEquipped -> Color(0xFF1E202B)
                                            isOwned -> Color(0xFFE11D48)
                                            isLevelLocked -> Color(0xFF181B26)
                                            else -> Color(0xFFE11D48)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isEquipped) Color(0xFFE11D48).copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
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
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isEquipped -> {
                                        Text(text = "Terpasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isOwned -> {
                                        Text(text = "Pasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isLevelLocked -> {
                                        Text(text = "🔒 Lv. ${titleDef.requiredLevel}", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    else -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            PrimoPointIcon(size = 14.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "${titleDef.pointCost}", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
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
    // 5. PREVIEW PROFILE EFFECT (DENGAN APNG ANIMATION & LOADING SPINNER)
    // ========================================================
    previewEffectItem?.let { effect ->
        val isOwned = unlockedEffects.contains(effect.id) || effect.animationUrl == equippedProfileEffectUrl
        val isEquipped = effect.animationUrl == equippedProfileEffectUrl
        val isLevelLocked = effect.tier == "B" && level < effect.requiredLevel
        var isEffectLoading by remember { mutableStateOf(true) }

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
                // Header Atas: Tombol Tutup & Info Efek
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
                }

                // Area Tengah: APNG Animation Penuh 60 FPS + Avatar User + Loading Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    // Animasi Profile Effect 60 FPS menggunakan ApngAnimationView / ApngStackedEffectView
                    val layers = effect.layerUrls.ifEmpty { listOf(effect.animationUrl) }
                    ApngStackedEffectView(
                        layerUrls = layers,
                        modifier = Modifier.size(320.dp),
                        scaleType = ImageView.ScaleType.FIT_CENTER,
                        onLoadingChanged = { isEffectLoading = it }
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
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    // Shimmer / Spinner Indikator saat animasi sedang dimuat
                    AnimatedVisibility(
                        visible = isEffectLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xCC090A0F))
                                .padding(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFE11D48),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat animasi efek...",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                // Area Bawah: Tombol Aksi & Harga Poin (Crimson Red)
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
                                    isEquipped -> Color(0xFF1E202B)
                                    isOwned -> Color(0xFFE11D48)
                                    isLevelLocked -> Color(0xFF1E2232)
                                    effect.tier == "C" -> Color(0xFF1E202B)
                                    else -> Color(0xFFE11D48)
                                }
                            )
                            .border(
                                1.dp,
                                if (isEquipped) Color(0xFFE11D48).copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(26.dp)
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
    // 6. PREVIEW AVATAR DECORATION
    // ========================================================
    previewBorderItem?.let { border ->
        val isOwned = unlockedBorders.contains(border.id) || border.imageUrl == equippedBorderUrl
        val isEquipped = border.imageUrl == equippedBorderUrl
        val isLevelLocked = border.tier == "B" && level < border.requiredLevel
        var isBorderLoading by remember { mutableStateOf(true) }

        Dialog(
            onDismissRequest = { previewBorderItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0D13))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Atas: Tombol Tutup & Nama Border
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { previewBorderItem = null },
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
                                text = "Preview Decoration",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = border.name,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Area Tengah: Live Preview Avatar + Animasi Border Discord 60 FPS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    // Foto Profil Pengguna Bulat 110.dp
                    Box(
                        modifier = Modifier
                            .size(110.dp)
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
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    // Border Avatar Discord 60 FPS 134.dp
                    ApngAnimationView(
                        url = border.imageUrl,
                        modifier = Modifier.size(134.dp),
                        scaleType = ImageView.ScaleType.FIT_CENTER,
                        onLoadingChanged = { isBorderLoading = it }
                    )

                    AnimatedVisibility(
                        visible = isBorderLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFE11D48),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Area Bawah: Tombol Aksi & Harga Poin (Crimson Red)
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
                                    isEquipped -> Color(0xFF1E202B)
                                    isOwned -> Color(0xFFE11D48)
                                    isLevelLocked -> Color(0xFF1E2232)
                                    border.tier == "C" -> Color(0xFF1E202B)
                                    else -> Color(0xFFE11D48)
                                }
                            )
                            .border(
                                1.dp,
                                if (isEquipped) Color(0xFFE11D48).copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(26.dp)
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
                                                Toast.makeText(context, "Border ini eksklusif dari Kode Redeem!", Toast.LENGTH_SHORT).show()
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
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    isEquipped -> {
                                        Text(text = "Sedang Terpasang", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isOwned -> {
                                        Text(text = "Pasang Border Ini", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    isLevelLocked -> {
                                        Text(text = "🔒 Terkunci (Minimal Level ${border.requiredLevel})", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    border.tier == "C" -> {
                                        Text(text = "🔒 Eksklusif Kode Redeem", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                                    }
                                    else -> {
                                        PrimoPointIcon(size = 20.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tukar & Pasang • ${FirebaseManager.formatNumber(border.pointCost)} Poin",
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
}

/**
 * Kartu Grid 2 Kolom untuk Profile Effect:
 * Mockup HP Vertikal 9:16 (145.dp x 82.dp) di atas single surface card (Color 0xFF12131A, rounded 20.dp).
 * Bebas dari badge Starter biru / overlay ganda.
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
    val staticUrl = remember(effect) {
        effect.staticImageUrl.ifBlank { effect.animationUrl }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF12131A))
            .border(
                1.dp,
                if (isEquipped) Color(0xFFE11D48) else Color(0xFF1F2330),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Frame Mockup HP Vertikal 9:16 (82.dp x 145.dp)
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(145.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0C14))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Gambar Efek Statis Memenuhi Mockup
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(staticUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = effect.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Island / Speaker bar minimalis di atas
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(width = 24.dp, height = 3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2F40))
            )

            // Avatar Mini Pengguna di Tengah Mockup
            Box(
                modifier = Modifier
                    .size(32.dp)
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
                    Text(
                        text = "YU",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nama Efek Profil
        Text(
            text = effect.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol Aksi (Crimson Red / Permukaan Gelap untuk Terpasang)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFF1E202B)
                        isOwned -> Color(0xFFE11D48)
                        isLevelLocked -> Color(0xFF181A24)
                        effect.tier == "C" -> Color(0xFF1E202B)
                        else -> Color(0xFFE11D48)
                    }
                )
                .border(
                    1.dp,
                    if (isEquipped) Color(0xFFE11D48).copy(alpha = 0.5f) else Color.Transparent,
                    RoundedCornerShape(10.dp)
                )
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isEquipped -> {
                    Text(text = "Terpasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                isOwned -> {
                    Text(text = "Pasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                isLevelLocked -> {
                    Text(text = "🔒 Lv. ${effect.requiredLevel}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                effect.tier == "C" -> {
                    Text(text = "🔒 Redeem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimoPointIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FirebaseManager.formatNumber(effect.pointCost),
                            color = Color.White,
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

/**
 * Kartu Grid 2 Kolom untuk Avatar Decoration:
 * Single surface clean card (Color 0xFF12131A, rounded 20.dp, padding 14.dp).
 * Lingkaran foto 76.dp dengan border 94.dp konsentris pas di tengah tanpa kotak hitam tambahan.
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
    val staticUrl = remember(border) {
        border.staticImageUrl.ifBlank { border.imageUrl }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF12131A))
            .border(
                1.dp,
                if (isEquipped) Color(0xFFE11D48) else Color(0xFF1F2330),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kontainer Avatar 76.dp + Border 94.dp Konsentris di Tengah
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp),
            contentAlignment = Alignment.Center
        ) {
            // Foto Profil Bulat Bersih 76.dp
            Box(
                modifier = Modifier
                    .size(76.dp)
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
                    Text(
                        text = "YU",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Layer Border Avatar Asli Discord 94.dp Tepat di Tengah
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(staticUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = border.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(94.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nama Border Avatar
        Text(
            text = border.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol Aksi (Crimson Red / Permukaan Gelap untuk Terpasang)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFF1E202B)
                        isOwned -> Color(0xFFE11D48)
                        isLevelLocked -> Color(0xFF181A24)
                        border.tier == "C" -> Color(0xFF1E202B)
                        else -> Color(0xFFE11D48)
                    }
                )
                .border(
                    1.dp,
                    if (isEquipped) Color(0xFFE11D48).copy(alpha = 0.5f) else Color.Transparent,
                    RoundedCornerShape(10.dp)
                )
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isEquipped -> {
                    Text(text = "Terpasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                isOwned -> {
                    Text(text = "Pasang", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                isLevelLocked -> {
                    Text(text = "🔒 Lv. ${border.requiredLevel}", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                border.tier == "C" -> {
                    Text(text = "🔒 Redeem", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SfProFontFamily)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PrimoPointIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = FirebaseManager.formatNumber(border.pointCost),
                            color = Color.White,
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
