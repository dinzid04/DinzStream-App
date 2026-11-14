package com.yuranime.dinz.ui.profile.subscreens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yuranime.dinz.data.firebase.BorderDefinition
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.EmptyTitleSlotBadge
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.util.Locale

/**
 * Halaman Edit Profil Pengguna
 * - Mengatur Nama, Username (Bebas Bug '@@'), Bio, Foto Profil, Cover Banner
 * - Katalog Lengkap Border Avatar (seluruh 985 item) & Profile Effects (seluruh 396 item)
 * - Tampilan Item Dimiliki di urutan teratas, Item Terkunci bertanda 🔒 dengan Live Preview instan
 * - Pemilihan Custom Title bergaya poligon geometris
 */
@Composable
fun EditProfileScreen(
    authDb: LocalAuthDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by FirebaseManager.userProfile.collectAsState()

    var nameInput by remember { mutableStateOf(currentProfile?.name ?: authDb.getUsername()) }
    var usernameInput by remember {
        mutableStateOf((currentProfile?.username ?: "member").removePrefix("@"))
    }
    var bioInput by remember { mutableStateOf(currentProfile?.bio ?: "Penikmat anime & komik di Yuranime.") }
    var selectedAvatarUrl by remember { mutableStateOf(currentProfile?.avatar ?: authDb.getAvatarUrl()) }
    var selectedBannerUrl by remember { mutableStateOf(currentProfile?.banner ?: authDb.getCoverUrl()) }
    var selectedTitle by remember { mutableStateOf(currentProfile?.equippedTitle.orEmpty()) }
    var selectedBorderUrl by remember { mutableStateOf(currentProfile?.equippedBorderUrl.orEmpty()) }
    var selectedProfileEffectUrl by remember { mutableStateOf(currentProfile?.equippedProfileEffectUrl.orEmpty()) }

    var lockedNoticeText by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val userLevel = currentProfile?.level ?: 1
    val userPoints = currentProfile?.points ?: 0L
    val unlockedTitles = currentProfile?.unlockedTitles ?: listOf("WARGA YURANIME")
    val unlockedBorders = currentProfile?.unlockedBorders ?: emptyList()
    val unlockedEffects = currentProfile?.unlockedEffects ?: emptyList()

    // Tab Kosmetik: 0 = Border Avatar (985), 1 = Profile Effect (396), 2 = Custom Title
    var cosmeticTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTier by remember { mutableStateOf("Semua") } // Semua, Koleksiku, Starter, Lv. 35+, Limited Redeem

    val avatarGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUrl = uri.toString()
        }
    }

    val bannerGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedBannerUrl = uri.toString()
        }
    }

    LaunchedEffect(currentProfile) {
        currentProfile?.let { profile ->
            if (nameInput.isBlank()) nameInput = profile.name
            if (usernameInput.isBlank()) usernameInput = profile.username.removePrefix("@")
            if (selectedAvatarUrl.isBlank()) selectedAvatarUrl = profile.avatar
            if (selectedBannerUrl.isBlank()) selectedBannerUrl = profile.banner
            if (selectedTitle.isBlank() && profile.equippedTitle.isNotBlank()) {
                selectedTitle = profile.equippedTitle
            }
            if (selectedBorderUrl.isBlank() && profile.equippedBorderUrl.isNotBlank()) {
                selectedBorderUrl = profile.equippedBorderUrl
            }
            if (selectedProfileEffectUrl.isBlank() && profile.equippedProfileEffectUrl.isNotBlank()) {
                selectedProfileEffectUrl = profile.equippedProfileEffectUrl
            }
        }
    }

    // Seluruh 985 Border Avatar Asli Discord
    val allBorders = remember(context) { DiscordCosmeticsRepository.getAllBorders(context) }
    // Seluruh 396 Profile Effects Asli Discord
    val allProfileEffects = remember(context) { DiscordCosmeticsRepository.getAllProfileEffects(context) }

    // Urutkan item dimiliki di urutan paling depan
    val sortedBorders = remember(allBorders, unlockedBorders, selectedBorderUrl, searchQuery, filterTier) {
        allBorders
            .filter { border ->
                val matchSearch = searchQuery.isBlank() ||
                    border.name.contains(searchQuery, ignoreCase = true) ||
                    border.category.contains(searchQuery, ignoreCase = true)
                val isOwned = unlockedBorders.contains(border.id) || border.imageUrl == selectedBorderUrl
                val matchFilter = when (filterTier) {
                    "Koleksiku" -> isOwned
                    "Starter" -> border.tier == "A"
                    "Lv. 35+" -> border.tier == "B"
                    "Limited Redeem" -> border.tier == "C"
                    else -> true
                }
                matchSearch && matchFilter
            }
            .sortedWith(
                compareByDescending<BorderDefinition> { it.imageUrl == selectedBorderUrl }
                    .thenByDescending { unlockedBorders.contains(it.id) }
            )
    }

    val sortedEffects = remember(allProfileEffects, unlockedEffects, selectedProfileEffectUrl, searchQuery, filterTier) {
        allProfileEffects
            .filter { effect ->
                val matchSearch = searchQuery.isBlank() ||
                    effect.name.contains(searchQuery, ignoreCase = true) ||
                    effect.category.contains(searchQuery, ignoreCase = true)
                val isOwned = unlockedEffects.contains(effect.id) || effect.animationUrl == selectedProfileEffectUrl
                val matchFilter = when (filterTier) {
                    "Koleksiku" -> isOwned
                    "Starter" -> effect.tier == "A"
                    "Lv. 35+" -> effect.tier == "B"
                    "Limited Redeem" -> effect.tier == "C"
                    else -> true
                }
                matchSearch && matchFilter
            }
            .sortedWith(
                compareByDescending<ProfileEffectDefinition> { it.animationUrl == selectedProfileEffectUrl }
                    .thenByDescending { unlockedEffects.contains(it.id) }
            )
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color(0xFFE2E8F0),
        focusedContainerColor = Color(0xFF14161E),
        unfocusedContainerColor = Color(0xFF11131B),
        focusedBorderColor = Color(0xFFFF5722),
        unfocusedBorderColor = Color(0xFF232738),
        cursorColor = Color(0xFFFF5722)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14161E))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Edit Profil",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Live Preview Border & Profile Effect Discord",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        // 1. LIVE PREVIEW BANNER & AVATAR BORDER (Presisi 82.dp + 100.dp Border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
        ) {
            // Background Cover Banner + Effect Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .background(Color(0xFF14161E))
                    .clickable { bannerGalleryLauncher.launch("image/*") }
            ) {
                if (selectedBannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = selectedBannerUrl,
                        contentDescription = "Banner Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF281018), Color(0xFF14161E), Color(0xFF0B0B0B))
                                )
                            )
                    )
                }

                // Render Live Preview Profile Effect di Atas Banner
                if (selectedProfileEffectUrl.isNotBlank()) {
                    AnimatedCosmeticImage(
                        url = selectedProfileEffectUrl,
                        isAnimated = true,
                        contentDescription = "Live Preview Profile Effect",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x220B0B0B),
                                    Color(0x770B0B0B),
                                    Color(0xFF0B0B0B)
                                )
                            )
                        )
                )

                // Tombol Ganti Cover Banner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xB30B0B0B))
                        .clickable { bannerGalleryLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload Banner",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upload Banner",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Live Preview Avatar Kiri: Circle 82.dp + Discord Border 100.dp (Presisi 100% Pas)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp)
                    .size(100.dp)
                    .clickable { avatarGalleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                // Layer Foto Profil Bulat: 82.dp
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161820)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedAvatarUrl,
                            contentDescription = "Avatar Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = nameInput.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // Layer Live Preview Border Avatar (discord_assets): 100.dp tepat konsentris
                if (selectedBorderUrl.isNotBlank()) {
                    AnimatedCosmeticImage(
                        url = DiscordCosmeticsRepository.getOptimizedUrl(selectedBorderUrl, size = 240, animated = true),
                        isAnimated = true,
                        contentDescription = "Live Preview Border Avatar",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
        }

        // Notifikasi Live Preview Item Terkunci
        AnimatedVisibility(visible = lockedNoticeText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x33F59E0B))
                    .border(0.75.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lockedNoticeText.orEmpty(),
                        color = Color(0xFFFDE68A),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
                IconButton(
                    onClick = { lockedNoticeText = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Input Nama
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Nama Tampilan",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    placeholder = { Text("Masukkan nama tampilan", color = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Input Username (Bebas Bug '@@')
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Username",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        usernameInput = it.lowercase(Locale.ROOT).replace(" ", "").removePrefix("@")
                    },
                    singleLine = true,
                    prefix = { Text("@", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold) },
                    placeholder = { Text("username", color = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Input Bio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Bio Profil",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { if (it.length <= 160) bioInput = it },
                    minLines = 3,
                    maxLines = 4,
                    placeholder = { Text("Tulis bio singkat tentang anime atau komik favoritmu...", color = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // TAB KATALOG KOSMETIK (Border Avatar, Profile Effect, Custom Title)
            Text(
                text = "Koleksi Kosmetik & Dekorasi Profil",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )

            // Segmented Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF11131B))
                    .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val tabs = listOf("Border Avatar (${allBorders.size})", "Effect (${allProfileEffects.size})", "Custom Title")
                tabs.forEachIndexed { idx, label ->
                    val active = cosmeticTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (active) Color(0xFF1C202C) else Color.Transparent)
                            .border(
                                width = if (active) 0.75.dp else 0.dp,
                                color = if (active) Color(0xFF334155) else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { cosmeticTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // FILTER & PENCARIAN (Untuk Tab 0 & Tab 1)
            if (cosmeticTab == 0 || cosmeticTab == 1) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (cosmeticTab == 0) "Cari dari 985 Border Avatar..." else "Cari dari 396 Profile Effect...",
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips Filter Kasta
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterChips = listOf("Semua", "Koleksiku", "Starter", "Lv. 35+", "Limited Redeem")
                    items(filterChips) { chip ->
                        val selected = filterTier == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) Color(0xFFFF5722) else Color(0xFF141722))
                                .border(
                                    width = 0.5.dp,
                                    color = if (selected) Color(0xFFFF5722) else Color(0xFF222738),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { filterTier = chip }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip,
                                color = if (selected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }

            // KONTEN TAB 0: SEMUA 985 BORDER AVATAR
            if (cosmeticTab == 0) {
                // Tombol Copot Border
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141722))
                        .clickable {
                            selectedBorderUrl = ""
                            lockedNoticeText = null
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tanpa Border (Polos)",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    if (selectedBorderUrl.isBlank()) {
                        Text(
                            text = "Aktif",
                            color = Color(0xFFFF5722),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // Grid 2 Kolom untuk Border Avatar (Tampilkan seluruh item)
                val displayList = sortedBorders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = displayList.chunked(2)
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { border ->
                                val isEquipped = border.imageUrl == selectedBorderUrl
                                val isOwned = unlockedBorders.contains(border.id)

                                EditCosmeticCard(
                                    name = border.name,
                                    imageUrl = border.imageUrl,
                                    isBorder = true,
                                    userAvatarUrl = selectedAvatarUrl,
                                    isEquipped = isEquipped,
                                    isOwned = isOwned,
                                    tier = border.tier,
                                    requiredLevel = border.requiredLevel,
                                    pointCost = border.pointCost,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedBorderUrl = border.imageUrl
                                        if (!isOwned) {
                                            lockedNoticeText = when (border.tier) {
                                                "C" -> "🔒 \"${border.name}\" adalah Item Eksklusif Kode Redeem!"
                                                "B" -> "🔒 \"${border.name}\" butuh Level 35 & ${border.pointCost} Poin di Shop!"
                                                else -> "🔒 \"${border.name}\" butuh ${border.pointCost} Poin di Shop!"
                                            }
                                        } else {
                                            lockedNoticeText = null
                                        }
                                    }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (sortedBorders.size > 60) {
                    Text(
                        text = "Menampilkan 60 dari ${sortedBorders.size} Border. Gunakan kolom pencarian di atas untuk menemukan spesifik nama.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }

            // KONTEN TAB 1: SEMUA 396 PROFILE EFFECT
            if (cosmeticTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141722))
                        .clickable {
                            selectedProfileEffectUrl = ""
                            lockedNoticeText = null
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tanpa Efek Profil",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    if (selectedProfileEffectUrl.isBlank()) {
                        Text(
                            text = "Aktif",
                            color = Color(0xFFFF5722),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                val displayList = sortedEffects
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = displayList.chunked(2)
                    chunked.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { effect ->
                                val isEquipped = effect.animationUrl == selectedProfileEffectUrl
                                val isOwned = unlockedEffects.contains(effect.id)

                                EditCosmeticCard(
                                    name = effect.name,
                                    imageUrl = effect.animationUrl,
                                    isBorder = false,
                                    userAvatarUrl = selectedAvatarUrl,
                                    isEquipped = isEquipped,
                                    isOwned = isOwned,
                                    tier = effect.tier,
                                    requiredLevel = effect.requiredLevel,
                                    pointCost = effect.pointCost,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedProfileEffectUrl = effect.animationUrl
                                        if (!isOwned) {
                                            lockedNoticeText = when (effect.tier) {
                                                "C" -> "🔒 \"${effect.name}\" adalah Efek Eksklusif Kode Redeem!"
                                                "B" -> "🔒 \"${effect.name}\" butuh Level 35 & ${effect.pointCost} Poin di Shop!"
                                                else -> "🔒 \"${effect.name}\" butuh ${effect.pointCost} Poin di Shop!"
                                            }
                                        } else {
                                            lockedNoticeText = null
                                        }
                                    }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (sortedEffects.size > 60) {
                    Text(
                        text = "Menampilkan 60 dari ${sortedEffects.size} Effect. Gunakan kolom pencarian di atas untuk menemukan spesifik nama.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }

            // KONTEN TAB 2: CUSTOM TITLE SELECTION
            if (cosmeticTab == 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val allTitles = remember { FirebaseManager.ALL_TITLES }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141722))
                            .clickable { selectedTitle = "" }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tanpa Custom Title",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                        if (selectedTitle.isBlank()) {
                            Text(
                                text = "Aktif",
                                color = Color(0xFFFF5722),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    allTitles.forEach { titleDef ->
                        val isOwned = userLevel >= titleDef.requiredLevel ||
                            unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                        val isEquipped = selectedTitle.equals(titleDef.titleName, ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isEquipped) Color(0x22E11D48) else Color(0xFF11131B))
                                .border(
                                    width = if (isEquipped) 1.dp else 0.5.dp,
                                    color = if (isEquipped) Color(0xFFE11D48) else Color(0xFF1D212E),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isOwned) {
                                        selectedTitle = titleDef.titleName
                                        lockedNoticeText = null
                                    } else {
                                        lockedNoticeText = "🔒 Title \"${titleDef.titleName}\" butuh Level ${titleDef.requiredLevel}!"
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GeometricTitleBadge(
                                titleName = titleDef.titleName,
                                tierLevel = titleDef.tierLevel,
                                styleVariant = titleDef.styleVariant
                            )

                            if (isOwned) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isEquipped) Color(0xFFE11D48) else Color(0xFF1E293B))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isEquipped) "Terpasang" else "Pasang",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lv. ${titleDef.requiredLevel}",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol Simpan Perubahan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                        )
                    )
                    .clickable(enabled = !isSaving) {
                        isSaving = true
                        val cleanUsername = usernameInput.trim().lowercase(Locale.ROOT).replace(" ", "").removePrefix("@")
                        authDb.setUsername(nameInput.trim())
                        authDb.setAvatarUrl(selectedAvatarUrl.trim())
                        authDb.setCoverUrl(selectedBannerUrl.trim())

                        FirebaseManager.updateProfile(
                            context = context,
                            name = nameInput,
                            username = cleanUsername,
                            bio = bioInput,
                            avatarUrl = selectedAvatarUrl,
                            bannerUrl = selectedBannerUrl,
                            equippedTitle = selectedTitle,
                            equippedBorderUrl = selectedBorderUrl,
                            equippedProfileEffectUrl = selectedProfileEffectUrl
                        ) { _, msg ->
                            isSaving = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Menyimpan Perubahan...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                } else {
                    Text(
                        text = "Simpan Perubahan",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Kartu Item Kosmetik di Halaman Edit Profil (2 Kolom)
 */
@Composable
private fun EditCosmeticCard(
    name: String,
    imageUrl: String,
    isBorder: Boolean,
    userAvatarUrl: String,
    isEquipped: Boolean,
    isOwned: Boolean,
    tier: String,
    requiredLevel: Int,
    pointCost: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isEquipped -> Color(0x33E11D48)
                    isOwned -> Color(0xFF141722)
                    else -> Color(0xFF0F1118)
                }
            )
            .border(
                width = if (isEquipped) 1.dp else 0.5.dp,
                color = when {
                    isEquipped -> Color(0xFFE11D48)
                    isOwned -> Color(0xFF283046)
                    else -> Color(0xFF1D212E)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Baris Atas: Badge Status / Kasta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tierLabel = when (tier) {
                "C" -> "Limited"
                "B" -> "Lv. $requiredLevel"
                else -> "Starter"
            }
            val tierColor = when (tier) {
                "C" -> Color(0xFFE11D48)
                "B" -> Color(0xFFF59E0B)
                else -> Color(0xFF38BDF8)
            }
            Text(
                text = tierLabel,
                color = tierColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )

            if (isOwned) {
                Text(
                    text = "Milikmu",
                    color = Color(0xFF10B981),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Terkunci",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Preview
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBorder) {
                // Border mengelilingi foto profil pengguna
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C202C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
                AnimatedCosmeticImage(
                    url = DiscordCosmeticsRepository.getOptimizedUrl(imageUrl, size = 160, animated = true),
                    isAnimated = true,
                    contentDescription = name,
                    modifier = Modifier.size(72.dp)
                )
            } else {
                // Profile Effect di atas kartu mini
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161824)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCosmeticImage(
                        url = imageUrl,
                        isAnimated = true,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nama Item
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status Tombol Aksi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFFE11D48)
                        isOwned -> Color(0xFF1E293B)
                        else -> Color(0xFF161822)
                    }
                )
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isEquipped -> "Terpasang"
                    isOwned -> "Pasang"
                    tier == "C" -> "🔒 Redeem"
                    tier == "B" -> "🔒 Lv. $requiredLevel"
                    else -> "🔒 $pointCost Poin"
                },
                color = when {
                    isEquipped -> Color.White
                    isOwned -> Color(0xFFE2E8F0)
                    else -> Color(0xFF64748B)
                },
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}
