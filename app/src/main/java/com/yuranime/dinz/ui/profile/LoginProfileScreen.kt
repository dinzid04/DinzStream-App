package com.yuranime.dinz.ui.profile

import android.widget.Toast
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.local.WatchHistoryItem
import com.yuranime.dinz.ui.components.AnimatedAvatarBorder
import com.yuranime.dinz.ui.components.AvatarBorderTheme
import com.yuranime.dinz.ui.components.BottomNavBar
import com.yuranime.dinz.ui.components.NavTab
import com.yuranime.dinz.ui.components.ProfileEntranceEffect
import com.yuranime.dinz.ui.components.ProfileEntranceTheme
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily

@Composable
fun LoginProfileScreen(
    modifier: Modifier = Modifier,
    selectedTab: NavTab = NavTab.PROFILE,
    onTabSelected: (NavTab) -> Unit = {},
    onAnimeClick: (id: String) -> Unit = {},
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val authDb = remember { LocalAuthDatabase(context) }
    var isLoggedIn by remember { mutableStateOf(authDb.isLoggedIn()) }
    var username by remember { mutableStateOf(authDb.getUsername()) }
    var watchHistory by remember { mutableStateOf<List<WatchHistoryItem>>(emptyList()) }
    var selectedBorderTheme by remember { mutableStateOf(AvatarBorderTheme.KITSUNE_FLAME) }
    var showEntranceEffect by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            watchHistory = authDb.getWatchHistory()
            showEntranceEffect = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ColorBackgroundPrimary,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(ColorBackgroundPrimary)
        ) {
            if (!isLoggedIn) {
                // Tampilan Login Sinematik (TAHAP 3)
                CinematicLoginView(
                    onGoogleLoginClick = {
                        authDb.setLoggedIn(true, "User Google")
                        username = "User Google"
                        isLoggedIn = true
                        Toast.makeText(context, "Berhasil masuk dengan Akun Google", Toast.LENGTH_SHORT).show()
                    },
                    onGuestLoginClick = {
                        authDb.setLoggedIn(true, "Pengguna Tamu")
                        username = "Pengguna Tamu"
                        isLoggedIn = true
                        Toast.makeText(context, "Melanjutkan sesi sebagai Pengguna Tamu", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Tampilan Profil Akun Aktif
                UserProfileView(
                    username = username,
                    watchHistory = watchHistory,
                    selectedBorderTheme = selectedBorderTheme,
                    onBorderThemeSelected = { selectedBorderTheme = it },
                    onAnimeClick = onAnimeClick,
                    onEpisodeClick = onEpisodeClick,
                    onClearHistory = {
                        authDb.clearWatchHistory()
                        watchHistory = emptyList()
                        Toast.makeText(context, "Riwayat tontonan berhasil dibersihkan", Toast.LENGTH_SHORT).show()
                    },
                    onLogout = {
                        authDb.setLoggedIn(false)
                        isLoggedIn = false
                        Toast.makeText(context, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                    }
                )

                // Efek Masuk Sinematik Shockwave
                ProfileEntranceEffect(
                    isActive = showEntranceEffect,
                    onFinished = { showEntranceEffect = false },
                    theme = ProfileEntranceTheme.KITSUNE_BURST
                )
            }
        }
    }
}

/**
 * Tampilan Login Sinematik (TAHAP 3):
 * 1. Background kolase 3 kolom poster anime dengan sudut kemiringan diagonal halus
 *    graphicsLayer { rotationZ = -12f; scaleX = 1.25f; scaleY = 1.25f }
 *    Kolom kiri & kanan bergerak ke atas, kolom tengah bergerak ke bawah secara mulus.
 * 2. Gradient overlay gelap (semi-transparan di atas & tengah, pekat #0B0B0B di bawah).
 * 3. Logo utama di tengah-atas (yuranime-full-logo / wolf emblem + Calistoga wordmark).
 * 4. Tombol Google: kapsul penuh RoundedCornerShape(50), background #FFFFFF, tinggi 52.dp,
 *    ikon Google vector 4-warna di kiri, teks "Masuk dengan Google" di tengah.
 * 5. Tombol "Login Tanpa Akun" warna abu-abu terang #9CA3AF (SF Medium 14.sp).
 * 6. Teks persetujuan layanan dengan buildAnnotatedString (Ketentuan Layanan dan Kebijakan Privasi bergaris bawah).
 * 7. Teks Versi Aplikasi "Versi 1.3.2" di paling bawah dengan jarak aman navigation bar.
 */
@Composable
private fun CinematicLoginView(
    onGoogleLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Kolase Poster Anime Bergerak Naik-Turun (Tilted 3-Column Marquee)
        TiltedPosterBackground()

        // 2. Gradient Overlay Gelap (Semi-transparan atas/tengah, menghitam pekat #0B0B0B di bawah)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC000000),
                            Color(0x77000000),
                            Color(0xAA000000),
                            Color(0xE60B0B0B),
                            Color(0xFF0B0B0B)
                        )
                    )
                )
        )

        // 3. Foreground Content: Logo Tengah-Atas & Tombol Bawah
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.0f))

            // Logo Utama di Tengah-Atas (yuranime-full-logo)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_yuranime_logo),
                        contentDescription = "Logo Yuranime",
                        modifier = Modifier.fillMaxSize(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "YURANIME",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = YuranimeLogoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tempat Nonton Anime Subtitle Indonesia",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontFamily = SfProFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1.2f))

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
                text = "Versi 1.3.2",
                color = Color(0xFF6B7280),
                fontSize = 12.sp,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * 3 Kolom Grid Poster dengan Sudut Kemiringan Diagonal Halus (-12°)
 * - Kolom Kiri & Kanan bergerak perlahan ke ATAS
 * - Kolom Tengah bergerak perlahan ke BAWAH
 * Persis seperti pada video referensi VID-20261101-WA0035.mp4
 */
@Composable
private fun TiltedPosterBackground() {
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

    val infiniteTransition = rememberInfiniteTransition(label = "marqueeTransition")

    // Kolom Kiri & Kanan bergerak perlahan ke ATAS (negatif offsetY)
    val scrollUpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -650f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scrollUpOffset"
    )

    // Kolom Tengah bergerak perlahan ke BAWAH (positif offsetY)
    val scrollDownOffset by infiniteTransition.animateFloat(
        initialValue = -650f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scrollDownOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationZ = -12f
                scaleX = 1.25f
                scaleY = 1.25f
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Kolom 1 (Bergerak Perlahan ke ATAS)
            MarqueeColumn(
                posters = col1Posters + col1Posters + col1Posters,
                offsetY = scrollUpOffset,
                modifier = Modifier.weight(1f)
            )

            // Kolom 2 (Bergerak Perlahan ke BAWAH)
            MarqueeColumn(
                posters = col2Posters + col2Posters + col2Posters,
                offsetY = scrollDownOffset,
                modifier = Modifier.weight(1f)
            )

            // Kolom 3 (Bergerak Perlahan ke ATAS)
            MarqueeColumn(
                posters = col3Posters + col3Posters + col3Posters,
                offsetY = scrollUpOffset * 0.9f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MarqueeColumn(
    posters: List<Int>,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .offset { IntOffset(0, (offsetY % 500).toInt() - 250) },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        posters.forEach { resId ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
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
 * Tampilan Pengguna Setelah Berhasil Masuk:
 * Menampilkan Avatar Border Bergerak, Informasi Akun, Riwayat Tontonan & Tombol Keluar
 */
@Composable
private fun UserProfileView(
    username: String,
    watchHistory: List<WatchHistoryItem>,
    selectedBorderTheme: AvatarBorderTheme,
    onBorderThemeSelected: (AvatarBorderTheme) -> Unit,
    onAnimeClick: (id: String) -> Unit,
    onEpisodeClick: (episodeId: String, movieId: String) -> Unit,
    onClearHistory: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROFIL PENGGUNA",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    letterSpacing = 1.sp
                )
            }
        }

        // 2. Profile Card with Animated Border
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF12151C))
                    .border(0.75.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedAvatarBorder(
                        avatarUrl = R.drawable.ic_yuranime_monochrome,
                        avatarSize = 72.dp,
                        borderTheme = selectedBorderTheme,
                        borderWidth = 3.5.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = username,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE11D48))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "MEMBER AKTIF",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 0.75.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(title = "Riwayat Ditonton", value = "${watchHistory.size} Judul")
                        ProfileStatItem(title = "Status Akun", value = "Aktif")
                        ProfileStatItem(title = "Versi App", value = "v1.9.0")
                    }
                }
            }
        }

        // 3. Section Avatar Border Theme Selector
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Gaya Border Bergerak",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih tema animasi rotasi avatar profil kamu",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AvatarBorderTheme.entries) { theme ->
                        val isSelected = theme == selectedBorderTheme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF12151C))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.75.dp,
                                    color = if (isSelected) Color(0xFFE11D48) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onBorderThemeSelected(theme) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Brush.sweepGradient(theme.colors))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = theme.title,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Section Watch History
        item {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Ditonton",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                if (watchHistory.isNotEmpty()) {
                    Text(
                        text = "Bersihkan",
                        color = Color(0xFFE11D48),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.clickable { onClearHistory() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (watchHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF12151C))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Belum ada riwayat tontonan anime",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }

        items(watchHistory) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF12151C))
                    .clickable {
                        if (item.episodeId.isNotBlank()) {
                            onEpisodeClick(item.episodeId, item.movieId)
                        } else {
                            onAnimeClick(item.movieId)
                        }
                    }
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.posterUrl)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_yuranime)
                                .build(),
                            contentDescription = item.animeTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.animeTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (item.episodeTitle.isNotBlank()) item.episodeTitle else "Lanjut menonton",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 5. Logout Button
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1315))
                    .border(0.75.dp, Color(0x33E11D48), RoundedCornerShape(10.dp))
                    .clickable { onLogout() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Keluar dari Akun",
                        color = Color(0xFFE11D48),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontFamily = SfProFontFamily
        )
    }
}
