package com.yuranime.dinz.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily

@Composable
fun LoginProfileScreen(
    modifier: Modifier = Modifier,
    selectedTab: NavTab = NavTab.PROFILE,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit = { _, _ -> },
    onCheckUpdateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authDb = remember { LocalAuthDatabase(context) }
    var isLoggedIn by remember { mutableStateOf(authDb.isLoggedIn()) }
    var username by remember { mutableStateOf(authDb.getUsername()) }
    var avatarUrl by remember { mutableStateOf(authDb.getAvatarUrl()) }
    var coverUrl by remember { mutableStateOf(authDb.getCoverUrl()) }
    var userCoins by remember { mutableIntStateOf(authDb.getUserCoins()) }

    // Dialog States
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showRewardsDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Handler tombol Back: selalu kembalikan ke Beranda Utama (HomeScreen)
    BackHandler {
        onTabSelected(NavTab.HOME)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0E0F12),
        bottomBar = {
            // TAHAP 2 Point 1: SEMBUNYIKAN SEPENUHNYA Bottom Navigation Bar saat di Layar Login!
            // Hanya tampilkan jika pengguna sudah login.
            if (isLoggedIn) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isLoggedIn) innerPadding.calculateBottomPadding() else 0.dp)
                .background(Color(0xFF0E0F12))
        ) {
            if (!isLoggedIn) {
                // TAHAP 2: Halaman Login (Poster Bergerak Halus & Rapat, Logo Transparan, Panah Pojok Kanan Atas)
                CinematicLoginView(
                    onBackToHome = { onTabSelected(NavTab.HOME) },
                    onGoogleLoginClick = {
                        authDb.setLoggedIn(true, "Gading Kencana")
                        username = "Gading Kencana"
                        isLoggedIn = true
                        Toast.makeText(context, "Berhasil masuk dengan Akun Google", Toast.LENGTH_SHORT).show()
                    },
                    onGuestLoginClick = {
                        authDb.setLoggedIn(true, "Pengguna Yuranime")
                        username = "Pengguna Yuranime"
                        isLoggedIn = true
                        Toast.makeText(context, "Melanjutkan sesi sebagai Pengguna Yuranime", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // TAHAP 3: Halaman Profile Setelah Login (Sesuai Referensi Screenshot_20261101_220757.jpg)
                UserProfileView(
                    username = username,
                    avatarUrl = avatarUrl,
                    coverUrl = coverUrl,
                    coins = userCoins,
                    onEditProfileClick = { showEditProfileDialog = true },
                    onDownloadMenuClick = { showDownloadDialog = true },
                    onLeaderboardMenuClick = { showLeaderboardDialog = true },
                    onRewardsMenuClick = { showRewardsDialog = true },
                    onInfoMenuClick = { showInfoDialog = true },
                    onLogout = {
                        authDb.setLoggedIn(false)
                        isLoggedIn = false
                        Toast.makeText(context, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Dialog Edit Profil Interaktif
            if (showEditProfileDialog) {
                EditProfileDialog(
                    currentName = username,
                    currentAvatarUrl = avatarUrl,
                    currentCoverUrl = coverUrl,
                    onDismiss = { showEditProfileDialog = false },
                    onSave = { newName, newAvatar, newCover ->
                        authDb.setUsername(newName)
                        authDb.setAvatarUrl(newAvatar)
                        authDb.setCoverUrl(newCover)
                        username = newName
                        avatarUrl = newAvatar
                        coverUrl = newCover
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Dialog Unduh / Download Manager
            if (showDownloadDialog) {
                DownloadManagerDialog(
                    onDismiss = { showDownloadDialog = false }
                )
            }

            // Dialog Papan Peringkat (Leaderboard)
            if (showLeaderboardDialog) {
                LeaderboardDialog(
                    currentUsername = username,
                    onDismiss = { showLeaderboardDialog = false }
                )
            }

            // Dialog Points & Rewards
            if (showRewardsDialog) {
                PointsRewardsDialog(
                    currentCoins = userCoins,
                    isCheckedIn = authDb.isDailyCheckedIn(),
                    onCheckInClick = {
                        val success = authDb.performDailyCheckIn()
                        if (success) {
                            userCoins = authDb.getUserCoins()
                            Toast.makeText(context, "Check-in harian sukses! +10 Poin didapatkan", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Kamu sudah check-in hari ini!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showRewardsDialog = false }
                )
            }

            // Dialog Info & Cerita Kami (+ Cek Pembaruan)
            if (showInfoDialog) {
                AppInfoDialog(
                    onCheckUpdate = {
                        showInfoDialog = false
                        onCheckUpdateClick()
                    },
                    onDismiss = { showInfoDialog = false }
                )
            }
        }
    }
}

/**
 * TAHAP 2: Halaman Login Sinematik
 * 1. Bottom Navigation Bar disembunyikan.
 * 2. Tombol Ikon Panah Kembali ke Beranda di Pojok Kanan Atas.
 * 3. Ikon Rubah putih berlatar belakang 100% transparan (tanpa bulatan hitam), ukuran lebih besar, dan digeser ke posisi 28%-32% dari atas.
 * 4. Kolase Poster bergerak lebih lambat, rapat (Arrangement.spacedBy(8.dp)), dan True Seamless Infinite Loop anti-glitch.
 */
@Composable
private fun CinematicLoginView(
    onBackToHome: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Kolase Poster Bergerak Seamless Infinite Loop
        SeamlessTiltedPosterBackground()

        // 2. Gradient Overlay Gelap (Menyatukan latar belakang secara sinematik)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC000000),
                            Color(0x66000000),
                            Color(0x99000000),
                            Color(0xEE0E0F12),
                            Color(0xFF0E0F12)
                        )
                    )
                )
        )

        // 3. Tombol Panah Kembali ke Beranda di Pojok Kanan Atas (Jarak Aman Status Bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, end = 18.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x661C1C1E))
                    .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onBackToHome() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Kembali ke Beranda",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. Komposisi Tengah & Bawah
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer dengan bobot 0.40f menempatkan logo rubah di posisi ~28%-32% dari atas layar
            Spacer(modifier = Modifier.weight(0.40f))

            // Grup Ikon Rubah + Teks YURANIME (Background 100% Transparan)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = "Logo Yuranime",
                    modifier = Modifier.size(86.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "YURANIME",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontFamily = YuranimeLogoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tempat Nonton Anime Subtitle Indonesia",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.5.sp,
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1.0f))

            // Tombol Google Kapsul Penuh (RoundedCornerShape(50), Latar Putih #FFFFFF, Tinggi 52.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onGoogleLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Masuk dengan Google",
                        color = Color(0xFF111827),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol "Login Tanpa Akun" (SF Medium 14.sp, warna #9CA3AF)
            Text(
                text = "Login Tanpa Akun",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily,
                modifier = Modifier
                    .clickable { onGuestLoginClick() }
                    .padding(vertical = 4.dp, horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teks Persetujuan Layanan menggunakan buildAnnotatedString
            val agreementText = remember {
                buildAnnotatedString {
                    append("Dengan melanjutkan, kamu menyetujui ")
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("Ketentuan Layanan")
                    }
                    append(" dan ")
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("Kebijakan Privasi")
                    }
                }
            }

            Text(
                text = agreementText,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Teks Versi Aplikasi
            Text(
                text = "Versi 1.9.0",
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * 3 Kolom Grid Poster dengan Sudut Kemiringan Halus (-12°)
 * - Jarak antar poster rapat: Arrangement.spacedBy(8.dp)
 * - Pergerakan diperlambat secara elegan (40-44 detik per siklus)
 * - True Seamless Infinite Loop: offset siklus tepat modulo tinggi daftar poster
 */
@Composable
private fun SeamlessTiltedPosterBackground() {
    val col1Posters = listOf(
        R.drawable.poster_login_01,
        R.drawable.poster_login_04,
        R.drawable.poster_login_07,
        R.drawable.poster_login_10
    )
    val col2Posters = listOf(
        R.drawable.poster_login_02,
        R.drawable.poster_login_05,
        R.drawable.poster_login_08,
        R.drawable.poster_login_11
    )
    val col3Posters = listOf(
        R.drawable.poster_login_03,
        R.drawable.poster_login_06,
        R.drawable.poster_login_09,
        R.drawable.poster_login_12
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = -12f
                scaleX = 1.3f
                scaleY = 1.3f
            }
    ) {
        val density = LocalDensity.current
        val colWidth = (maxWidth - 16.dp) / 3
        val posterHeight = colWidth * 1.5f
        val itemHeightWithSpacing = posterHeight + 8.dp
        val cycleHeightPx = with(density) { (itemHeightWithSpacing * 4).toPx() }

        val transition = rememberInfiniteTransition(label = "seamlessPosterTransition")

        // Kolom 1 (Bergerak Perlahan ke ATAS)
        val progressCol1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(42000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol1"
        )

        // Kolom 2 (Bergerak Perlahan ke BAWAH)
        val progressCol2 by transition.animateFloat(
            initialValue = -1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(38000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol2"
        )

        // Kolom 3 (Bergerak Perlahan ke ATAS)
        val progressCol3 by transition.animateFloat(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(45000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progressCol3"
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SeamlessMarqueeColumn(
                posters = col1Posters,
                offsetY = progressCol1 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
            SeamlessMarqueeColumn(
                posters = col2Posters,
                offsetY = progressCol2 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
            SeamlessMarqueeColumn(
                posters = col3Posters,
                offsetY = progressCol3 * cycleHeightPx,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SeamlessMarqueeColumn(
    posters: List<Int>,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    // Gandakan daftar poster 3x agar siklus loop selalu mulus tanpa pernah terputus
    val repeatedPosters = remember(posters) { posters + posters + posters }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .offset { IntOffset(0, offsetY.toInt()) },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeatedPosters.forEach { resId ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
            ) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * TAHAP 3: Halaman Profile Setelah Login (Persis Mengacu Screenshot_20261101_220757.jpg)
 */
@Composable
private fun UserProfileView(
    username: String,
    avatarUrl: String,
    coverUrl: String,
    coins: Int,
    onEditProfileClick: () -> Unit,
    onDownloadMenuClick: () -> Unit,
    onLeaderboardMenuClick: () -> Unit,
    onRewardsMenuClick: () -> Unit,
    onInfoMenuClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header Banner Landscape Anime dengan Avatar & Nama
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                // Background Cover / Banner Anime
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Default Cover Anime yang Indah
                    Image(
                        painter = painterResource(id = R.drawable.poster_login_01),
                        contentDescription = "Cover Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradasi Gelap Halus ke Warna Latar Utama (#0E0F12)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x330E0F12),
                                    Color(0x880E0F12),
                                    Color(0xFF0E0F12)
                                )
                            )
                        )
                )

                // Avatar, Nama & Level di Kiri Bawah Banner
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 12.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foto Profil Bulat dengan Border Oranye & Badge Kamera Biru
                    Box(
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2024))
                                .border(2.dp, Color(0xFFF97316), CircleShape)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUrl)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_yuranime_monochrome)
                                        .build(),
                                    contentDescription = "Foto Profil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                    contentDescription = "Foto Profil",
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        // Badge Ikon Kamera Biru Kecil di Pojok Kanan Bawah
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6))
                                .border(1.5.dp, Color(0xFF0E0F12), CircleShape)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ubah Foto",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Sisi Kanan Avatar: Nama Pengguna + Ikon Pensil Edit + Lv. 1
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = username,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Nama",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onEditProfileClick() }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Lv. 1",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }

        // 2. Baris Tombol Media Sosial (Scrollable Row)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // WhatsApp
                item {
                    SocialMediaPillButton(
                        label = "WhatsApp",
                        color = Color(0xFF25D366),
                        onClick = {
                            openExternalUrl(context, "https://chat.whatsapp.com/invite")
                        }
                    )
                }
                // Telegram
                item {
                    SocialMediaPillButton(
                        label = "Telegram",
                        color = Color(0xFF38BDF8),
                        onClick = {
                            openExternalUrl(context, "https://t.me/yuranime_official")
                        }
                    )
                }
                // Instagram
                item {
                    SocialMediaPillButton(
                        label = "Instagram",
                        color = Color(0xFFE1306C),
                        onClick = {
                            openExternalUrl(context, "https://instagram.com/yuranime")
                        }
                    )
                }
                // TikTok
                item {
                    SocialMediaPillButton(
                        label = "TikTok",
                        color = Color(0xFFFFFFFF),
                        onClick = {
                            openExternalUrl(context, "https://tiktok.com/@yuranime")
                        }
                    )
                }
                // Discord
                item {
                    SocialMediaPillButton(
                        label = "Discord",
                        color = Color(0xFF5865F2),
                        onClick = {
                            openExternalUrl(context, "https://discord.gg/yuranime")
                        }
                    )
                }
            }
        }

        // 3. Daftar Menu Profile (HANYA 5 MENU UTAMA + LOGOUT!)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Edit Profil - Sub-teks: "Ganti Foto atau Cover"
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    iconTint = Color(0xFF94A3B8),
                    title = "Edit Profil",
                    subtitle = "Ganti Foto atau Cover",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onEditProfileClick
                )

                // 2. Unduh - Sub-teks: "Kelola konten yang diunduh"
                ProfileMenuItem(
                    icon = Icons.Default.FileDownload,
                    iconTint = Color(0xFF94A3B8),
                    title = "Unduh",
                    subtitle = "Kelola konten yang diunduh",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onDownloadMenuClick
                )

                // 3. Papan Peringkat - Sub-teks: "Lihat peringkat XP bulanan" (badge: TOP 100)
                ProfileMenuItem(
                    icon = Icons.Default.EmojiEvents,
                    iconTint = Color(0xFF94A3B8),
                    title = "Papan Peringkat",
                    subtitle = "Lihat peringkat XP bulanan",
                    trailingType = TrailingType.BADGE_BLUE,
                    badgeText = "TOP 100",
                    onClick = onLeaderboardMenuClick
                )

                // 4. Points & Rewards - Sub-teks: "Check-in harian & tukar poin" (badge: ★ 60)
                ProfileMenuItem(
                    icon = Icons.Default.Star,
                    iconTint = Color(0xFF94A3B8),
                    title = "Points & Rewards",
                    subtitle = "Check-in harian & tukar poin",
                    trailingType = TrailingType.BADGE_GOLD,
                    badgeText = "★ $coins",
                    onClick = onRewardsMenuClick
                )

                // 5. Info - Sub-teks: "Simak Cerita Kami"
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFF94A3B8),
                    title = "Info",
                    subtitle = "Simak Cerita Kami",
                    trailingType = TrailingType.CHEVRON,
                    onClick = onInfoMenuClick
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Opsi Keluar / Logout Bersih
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1C20))
                        .border(0.5.dp, Color(0x33EF4444), RoundedCornerShape(14.dp))
                        .clickable { onLogout() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22EF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keluar dari Akun",
                                color = Color(0xFFEF4444),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kembali ke halaman login",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen Tombol Kapsul Media Sosial
 */
@Composable
private fun SocialMediaPillButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF161B22))
            .border(0.75.dp, Color(0x26FFFFFF), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

private enum class TrailingType {
    CHEVRON,
    BADGE_BLUE,
    BADGE_GOLD
}

/**
 * Komponen Item Menu Profil
 */
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingType: TrailingType,
    badgeText: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1C20))
            .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikon Kotak Gelap Membulat (#1A1C20 / #23272F)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF24272E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Judul SF SemiBold Putih + Deskripsi Abu-abu
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }

            // Indikator di Kanan
            when (trailingType) {
                TrailingType.CHEVRON -> {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                TrailingType.BADGE_BLUE -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF2563EB))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
                TrailingType.BADGE_GOLD -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF59E0B))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color(0xFF1E1B4B),
                            fontSize = 11.sp,
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
 * Dialog Edit Profil Interaktif: Mengubah Nama, Foto Profil, atau Cover Banner
 */
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentAvatarUrl: String,
    currentCoverUrl: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newAvatarUrl: String, newCoverUrl: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatarUrl) }
    var selectedCover by remember { mutableStateOf(currentCoverUrl) }

    val presetAvatars = listOf(
        "", // Default white fox
        "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=200",
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=200",
        "https://images.unsplash.com/photo-1563089145-599997674d42?w=200"
    )

    val presetCovers = listOf(
        "", // Default Frieren Meadow
        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C20))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profil",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Nama
                Text(
                    text = "Nama Pengguna",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF0A84FF),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedContainerColor = Color(0xFF121418),
                        unfocusedContainerColor = Color(0xFF121418)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pilihan Foto Profil
                Text(
                    text = "Pilih Preset Foto Profil",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetAvatars.forEachIndexed { index, url ->
                        val isSelected = selectedAvatar == url
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF24272E))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFFF97316) else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = url },
                            contentAlignment = Alignment.Center
                        ) {
                            if (url.isBlank()) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pilihan Cover Banner
                Text(
                    text = "Pilih Preset Cover Banner",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetCovers.forEachIndexed { index, url ->
                        val isSelected = selectedCover == url
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF24272E))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFF0A84FF) else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCover = url },
                            contentAlignment = Alignment.Center
                        ) {
                            if (url.isBlank()) {
                                Image(
                                    painter = painterResource(id = R.drawable.poster_login_01),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val validName = nameInput.trim().ifBlank { "Pengguna Yuranime" }
                        onSave(validName, selectedAvatar, selectedCover)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Simpan Perubahan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Dialog Unduh / Download Manager
 */
@Composable
private fun DownloadManagerDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C20))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Konten Unduhan",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0x2238BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Belum Ada Konten Terunduh",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Kamu bisa mengunduh episode anime favoritmu untuk ditonton secara offline kapan saja.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF24272E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Tutup", fontFamily = SfProFontFamily)
                }
            }
        }
    }
}

/**
 * Dialog Papan Peringkat (Leaderboard)
 */
@Composable
private fun LeaderboardDialog(
    currentUsername: String,
    onDismiss: () -> Unit
) {
    val topUsers = listOf(
        Pair("Kurogane", "3,450 XP"),
        Pair("Rimuru_Tempest", "3,120 XP"),
        Pair("Mikasa_A", "2,980 XP"),
        Pair("Gojo_Satoru", "2,740 XP"),
        Pair(currentUsername, "1,200 XP")
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C20))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Papan Peringkat XP",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                topUsers.forEachIndexed { index, pair ->
                    val isCurrentUser = pair.first == currentUsername
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrentUser) Color(0x332563EB) else Color(0x11FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${if (isCurrentUser) 98 else index + 1}",
                            color = if (index < 3) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            text = pair.first,
                            color = if (isCurrentUser) Color(0xFF38BDF8) else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = pair.second,
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Tutup", fontFamily = SfProFontFamily)
                }
            }
        }
    }
}

/**
 * Dialog Points & Rewards: Check-in Harian & Tukar Poin
 */
@Composable
private fun PointsRewardsDialog(
    currentCoins: Int,
    isCheckedIn: Boolean,
    onCheckInClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C20))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Points & Rewards",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0x22F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "★ $currentCoins Poin",
                    color = Color(0xFFF59E0B),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Kumpulkan poin dari check-in harian dan tukarkan dengan berbagai badge avatar eksklusif!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onCheckInClick,
                    enabled = !isCheckedIn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckedIn) Color(0xFF24272E) else Color(0xFFF59E0B),
                        contentColor = if (isCheckedIn) Color(0xFF94A3B8) else Color(0xFF1E1B4B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isCheckedIn) "Sudah Check-In Hari Ini ✓" else "Check-in Harian (+10 Poin)",
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Dialog Info Cerita Kami (+ Tombol Cek Pembaruan)
 */
@Composable
private fun AppInfoDialog(
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1C20))
                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tentang Yuranime",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Yuranime adalah platform streaming anime mobile subtitle Indonesia yang dibangun dengan dedikasi penuh untuk para penonton anime tanah air. Menghadirkan katalog anime terlengkap, cuplikan video pendek Cuplix, serta antarmuka modern yang cepat, responsif, dan bebas hambatan.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Versi Terpasang: 1.9.0 (Build 18)",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Tombol Cek Pembaruan Aplikasi
                Button(
                    onClick = onCheckUpdate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cek Pembaruan Aplikasi",
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Membuka tautan: $url", Toast.LENGTH_SHORT).show()
    }
}
