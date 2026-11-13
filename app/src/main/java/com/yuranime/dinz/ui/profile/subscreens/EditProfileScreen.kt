package com.yuranime.dinz.ui.profile.subscreens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.EmptyTitleSlotBadge
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * TAHAP 1 & TAHAP 4: Halaman Edit Profil Pengguna
 * Mengatur Nama, Username, Bio, Foto Profil (WebP upload ke Cloudflare R2 / Dinz CDN),
 * Cover Banner (WebP upload ke Cloudflare R2 / Dinz CDN), serta Pilih Custom Title.
 */
@Composable
fun EditProfileScreen(
    authDb: LocalAuthDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by FirebaseManager.userProfile.collectAsState()

    var nameInput by remember { mutableStateOf(currentProfile?.name ?: authDb.getUsername()) }
    var usernameInput by remember { mutableStateOf(currentProfile?.username ?: "member") }
    var bioInput by remember { mutableStateOf(currentProfile?.bio ?: "Penikmat anime & komik di Yuranime.") }
    var selectedAvatarUrl by remember { mutableStateOf(currentProfile?.avatar ?: authDb.getAvatarUrl()) }
    var selectedBannerUrl by remember { mutableStateOf(currentProfile?.banner ?: authDb.getCoverUrl()) }
    var selectedTitle by remember { mutableStateOf(currentProfile?.equippedTitle.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }

    val userLevel = currentProfile?.level ?: 1
    val unlockedTitles = currentProfile?.unlockedTitles ?: listOf("WARGA YURANIME")

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
            if (usernameInput.isBlank()) usernameInput = profile.username
            if (selectedAvatarUrl.isBlank()) selectedAvatarUrl = profile.avatar
            if (selectedBannerUrl.isBlank()) selectedBannerUrl = profile.banner
            if (selectedTitle.isBlank() && profile.equippedTitle.isNotBlank()) {
                selectedTitle = profile.equippedTitle
            }
        }
    }

    val presetAvatars = remember {
        listOf(
            "https://api.dicebear.com/9.x/adventurer/png?seed=YuraCrimson&backgroundColor=b6e3f4",
            "https://api.dicebear.com/9.x/adventurer/png?seed=DinzShadow&backgroundColor=c0aede",
            "https://api.dicebear.com/9.x/adventurer/png?seed=KitsuneBlaze&backgroundColor=ffdfbf",
            "https://api.dicebear.com/9.x/adventurer/png?seed=SakuraBlade&backgroundColor=ffd5dc",
            "https://api.dicebear.com/9.x/adventurer/png?seed=Ryuujin&backgroundColor=d1d4f9",
            "https://api.dicebear.com/9.x/adventurer/png?seed=Akatsuki&backgroundColor=b6e3f4"
        )
    }

    val presetBanners = remember {
        listOf(
            "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1000&q=80",
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&q=80",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&q=80",
            "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000&q=80"
        )
    }

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
                    text = "Foto profil & banner otomatis dikompresi WebP ke CDN",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        // Preview Header Banner & Avatar Kiri (Sesuai Tata Letak Baru ProfileScreen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x220B0B0B),
                                    Color(0x880B0B0B),
                                    Color(0xFF0B0B0B)
                                )
                            )
                        )
                )

                // Tombol Unggah Cover Banner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xB30B0B0B))
                        .clickable { bannerGalleryLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Ubah Cover",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
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

            // Avatar Kiri Menumpuk Separuh di Bawah Banner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp)
                    .size(86.dp)
                    .clickable { avatarGalleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161820))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFFF5722), Color(0xFFE11D48), Color(0xFFFF5722))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedAvatarUrl,
                            contentDescription = "Avatar Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
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

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Ganti Foto Profil",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pilih Cepat Avatar & Banner
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foto Profil (Galeri atau Preset)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Pilih dari Galeri",
                        color = Color(0xFFFF5722),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.clickable { avatarGalleryLauncher.launch("image/*") }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(presetAvatars) { avatarUrl ->
                        val isSelected = selectedAvatarUrl == avatarUrl
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14161E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFE11D48) else Color(0xFF1F2433),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatarUrl = avatarUrl }
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Preset Cover Banner",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(presetBanners) { bannerUrl ->
                        val isSelected = selectedBannerUrl == bannerUrl
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14161E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFE11D48) else Color(0xFF1F2433),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedBannerUrl = bannerUrl }
                        ) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x66000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pilih Custom Title Badge
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Title Badge",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    if (selectedTitle.isNotBlank()) {
                        Text(
                            text = "Kosongkan Title",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.clickable { selectedTitle = "" }
                        )
                    }
                }

                // Pratinjau Title yang dipilih
                if (selectedTitle.isNotBlank()) {
                    val def = FirebaseManager.resolveTitleDefinition(selectedTitle, userLevel)
                    GeometricTitleBadge(
                        titleName = selectedTitle,
                        tierLevel = def?.tierLevel ?: userLevel,
                        styleVariant = def?.styleVariant ?: 0
                    )
                } else {
                    EmptyTitleSlotBadge(onClick = {})
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(FirebaseManager.ALL_TITLES, key = { it.id }) { titleDef ->
                        val isUnlocked = (titleDef.unlockType == com.yuranime.dinz.data.firebase.TitleUnlockType.MILESTONE &&
                            userLevel >= titleDef.requiredLevel) ||
                            unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                        val isSelected = selectedTitle.equals(titleDef.titleName, ignoreCase = true)

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0x26E11D48) else Color(0xFF14161E)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFE11D48) else Color(0xFF1F2433),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isUnlocked) {
                                    selectedTitle = if (isSelected) "" else titleDef.titleName
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            GeometricTitleBadge(
                                titleName = titleDef.titleName,
                                tierLevel = titleDef.tierLevel,
                                styleVariant = titleDef.styleVariant,
                                compact = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = when {
                                        isSelected -> "Dipilih"
                                        isUnlocked -> "Tersedia"
                                        titleDef.pointCost > 0 -> "Tukar ${titleDef.pointCost} Poin"
                                        else -> "Terbuka Lv. ${titleDef.requiredLevel}"
                                    },
                                    color = if (isSelected) Color(0xFFFF5722) else Color(0xFF64748B),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // Form Input Nama, Username, Bio
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE11D48),
                unfocusedBorderColor = Color(0xFF1F2433),
                focusedContainerColor = Color(0xFF14161E),
                unfocusedContainerColor = Color(0xFF14161E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE11D48)
            )

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
                    onValueChange = { usernameInput = it.lowercase().replace(" ", "") },
                    singleLine = true,
                    prefix = { Text("@", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold) },
                    placeholder = { Text("username", color = Color(0xFF64748B)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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

            Spacer(modifier = Modifier.height(6.dp))

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
                        authDb.setUsername(nameInput.trim())
                        authDb.setAvatarUrl(selectedAvatarUrl.trim())
                        authDb.setCoverUrl(selectedBannerUrl.trim())

                        FirebaseManager.updateProfile(
                            context = context,
                            name = nameInput,
                            username = usernameInput,
                            bio = bioInput,
                            avatarUrl = selectedAvatarUrl,
                            bannerUrl = selectedBannerUrl,
                            equippedTitle = selectedTitle
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
                            text = "Mengunggah WebP ke CDN...",
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
