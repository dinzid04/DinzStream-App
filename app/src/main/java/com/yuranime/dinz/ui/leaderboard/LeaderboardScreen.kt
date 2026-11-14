package com.yuranime.dinz.ui.leaderboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.LeaderboardUser
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Halaman Papan Peringkat Real-Time (URUTAN 1):
 * - Header bersih tanpa bulatan overlay pada tombol <- dan ?, dilengkapi Vector Mahkota Royal 3-Puncak
 * - Podium Top 1, 2, 3 & Daftar #4+ menampilkan Avatar Decoration milik masing-masing user (Versi STATIS / Non-Animasi)
 * - Bar Header menggunakan Vector Icon Grup Pengguna & Vector Icon Grafik 3-Bar (tanpa simbol '#' atau emoji)
 * - Indikator Level menggunakan Vector Icon Rank Chevron tanpa segitiga teks dan tanpa kapsul overlay
 * - Sticky Bottom Bar presisi di tengah vertikal & Modal Cara Memperoleh EXP rapi
 */
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val leaderboardUsers by FirebaseManager.leaderboard.collectAsState()
    val isLoading by FirebaseManager.isLeaderboardLoading.collectAsState()
    val currentUserProfile by FirebaseManager.userProfile.collectAsState()
    val currentUid = FirebaseManager.getCurrentUid()

    var showXpInfoModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseManager.deduplicateUserAccounts(context)
        FirebaseManager.startListeningLeaderboard()
    }

    val top1User = leaderboardUsers.getOrNull(0)
    val top2User = leaderboardUsers.getOrNull(1)
    val top3User = leaderboardUsers.getOrNull(2)
    val remainingUsers = if (leaderboardUsers.size > 3) {
        leaderboardUsers.subList(3, leaderboardUsers.size)
    } else {
        emptyList()
    }

    val myRankEntry = remember(leaderboardUsers, currentUid, currentUserProfile) {
        val profileName = currentUserProfile?.name?.trim().orEmpty()
        leaderboardUsers.firstOrNull {
            (!currentUid.isNullOrBlank() && it.uid == currentUid) ||
                (profileName.isNotBlank() && it.name.equals(profileName, ignoreCase = true))
        }
    }

    if (showXpInfoModal) {
        XpInfoModal(onDismiss = { showXpInfoModal = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
    ) {
        // Latar Atas: Sapuan Gelombang Gradasi Merah Marun Gelap (Dark Crimson Aurora Wave)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D0A14),
                            Color(0xFF1A070E),
                            Color(0x00090A0E)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ========================================================
            // 1. HEADER ATAS (Tanpa Bulatan Overlay + Vector Mahkota Royal 3-Puncak)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Tombol Kiri (Panah <- bersih tanpa lingkaran overlay)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Bagian Tengah: Vector Mahkota Royal 3-Puncak + Judul + Subteks + Ornamen Simetris
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoyalThreePeakCrownIcon(
                        size = 20.dp,
                        primaryColor = Color(0xFFE11D48),
                        accentColor = Color(0xFFF59E0B)
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Papan Peringkat",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Text(
                        text = "Sinkronisasi Real-Time Komunitas Yuranime",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Garis Hiasan Simetris Merah Tipis
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.width(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFFE11D48))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFE11D48), Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                // Tombol Kanan (Ikon Bantuan ? bersih tanpa lingkaran overlay)
                IconButton(
                    onClick = { showXpInfoModal = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Bantuan EXP",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            if (isLoading && leaderboardUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFE11D48),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Memuat data peringkat real-time...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            } else if (leaderboardUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum Ada Data Peringkat",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jadilah member pertama yang menempati Podium Top 1 dengan aktif menonton anime dan membaca komik.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontFamily = SfProFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // ========================================================
                    // 2. PODIUM TOP 1, 2, 3 (AVATAR DECORATION STATIS MILIK USER + PEDESTAL)
                    // ========================================================
                    item {
                        TopThreeEditorialPodium(
                            top1 = top1User,
                            top2 = top2User,
                            top3 = top3User,
                            currentUid = currentUid
                        )
                    }

                    // ========================================================
                    // 3. BAR HEADER PENGGUNA & TOTAL EXP (VECTOR ICON ASLI)
                    // ========================================================
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0E1016))
                                .border(1.dp, Color(0xFF1C202B), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pendaran merah neon di sudut kiri bawah
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(width = 68.dp, height = 2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFE11D48), Color.Transparent)
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserGroupVectorIcon(
                                        size = 16.dp,
                                        color = Color(0xFFE11D48)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "Pengguna",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SparkBarChartVectorIcon(
                                        size = 14.dp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total EXP",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // ========================================================
                    // 4. KARTU DAFTAR PERINGKAT #4 KE BAWAH
                    // ========================================================
                    if (remainingUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Menampilkan ${leaderboardUsers.size} pengguna teratas saat ini.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    } else {
                        items(remainingUsers, key = { it.uid.ifBlank { "rank_${it.rank}" } }) { user ->
                            ModernLeaderboardRowItem(
                                user = user,
                                isCurrentUser = user.uid.isNotBlank() && user.uid == currentUid
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // 5. STICKY BOTTOM BAR "POSISI PERINGKAT ANDA"
        // ========================================================
        CurrentUserRankBottomBar(
            currentUserProfile = currentUserProfile,
            myRankEntry = myRankEntry,
            totalUsers = leaderboardUsers.size,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/**
 * Vector Icon Mahkota Royal 3-Puncak yang lancip, simetris, dan proporsional
 */
@Composable
private fun RoyalThreePeakCrownIcon(
    size: Dp,
    primaryColor: Color = Color(0xFFE11D48),
    accentColor: Color = Color(0xFFF59E0B),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = size * 1.25f, height = size)) {
        val w = this.size.width
        val h = this.size.height

        val crownPath = Path().apply {
            moveTo(w * 0.14f, h * 0.78f)
            lineTo(w * 0.05f, h * 0.28f)
            lineTo(w * 0.31f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.69f, h * 0.52f)
            lineTo(w * 0.95f, h * 0.28f)
            lineTo(w * 0.86f, h * 0.78f)
            close()
        }

        drawPath(
            path = crownPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor, Color(0xFFBE123C))
            )
        )

        // Base band mahkota
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(w * 0.14f, h * 0.83f),
            size = Size(w * 0.72f, h * 0.11f),
            cornerRadius = CornerRadius(h * 0.05f, h * 0.05f)
        )

        // 3 titik permata di ujung puncak mahkota
        val jewelRadius = h * 0.065f
        drawCircle(
            color = accentColor,
            radius = jewelRadius,
            center = Offset(w * 0.50f, h * 0.09f)
        )
        drawCircle(
            color = primaryColor,
            radius = jewelRadius * 0.85f,
            center = Offset(w * 0.05f, h * 0.24f)
        )
        drawCircle(
            color = primaryColor,
            radius = jewelRadius * 0.85f,
            center = Offset(w * 0.95f, h * 0.24f)
        )
    }
}

/**
 * Vector Icon Grup Pengguna untuk kolom kiri Header Papan Peringkat
 */
@Composable
private fun UserGroupVectorIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // User utama (depan kiri)
        drawCircle(
            color = color,
            radius = w * 0.19f,
            center = Offset(w * 0.38f, h * 0.30f)
        )
        val frontBody = Path().apply {
            moveTo(w * 0.10f, h * 0.86f)
            quadraticTo(w * 0.10f, h * 0.56f, w * 0.38f, h * 0.56f)
            quadraticTo(w * 0.66f, h * 0.56f, w * 0.66f, h * 0.86f)
            close()
        }
        drawPath(path = frontBody, color = color)

        // User kedua (belakang kanan)
        drawCircle(
            color = color.copy(alpha = 0.65f),
            radius = w * 0.15f,
            center = Offset(w * 0.72f, h * 0.34f)
        )
        val backBody = Path().apply {
            moveTo(w * 0.56f, h * 0.84f)
            quadraticTo(w * 0.58f, h * 0.60f, w * 0.74f, h * 0.60f)
            quadraticTo(w * 0.93f, h * 0.60f, w * 0.93f, h * 0.84f)
            close()
        }
        drawPath(path = backBody, color = color.copy(alpha = 0.65f))
    }
}

/**
 * Vector Icon Grafik 3-Bar Monokrom Bersih untuk kolom kanan Total EXP
 */
@Composable
private fun SparkBarChartVectorIcon(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * 0.21f
        val radius = CornerRadius(barWidth * 0.45f, barWidth * 0.45f)

        // Bar 1 (kiri - pendek)
        drawRoundRect(
            color = color.copy(alpha = 0.65f),
            topLeft = Offset(w * 0.08f, h * 0.52f),
            size = Size(barWidth, h * 0.40f),
            cornerRadius = radius
        )
        // Bar 2 (tengah - sedang)
        drawRoundRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.395f, h * 0.30f),
            size = Size(barWidth, h * 0.62f),
            cornerRadius = radius
        )
        // Bar 3 (kanan - tinggi)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.71f, h * 0.10f),
            size = Size(barWidth, h * 0.82f),
            cornerRadius = radius
        )
    }
}

/**
 * Vector Icon Rank Chevron (Pangkat Garis Bersusun Tajam Emas/Oranye) Tanpa Kapsul Overlay
 */
@Composable
private fun RankChevronVectorIcon(
    level: Int,
    size: Dp = 13.dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = when {
        level >= 50 -> Color(0xFFF59E0B)
        level >= 20 -> Color(0xFFFF5722)
        else -> Color(0xFFFB923C)
    }
    val secondaryColor = when {
        level >= 50 -> Color(0xFFFDE68A)
        level >= 20 -> Color(0xFFFDBA74)
        else -> Color(0xFFFED7AA)
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.16f).coerceAtLeast(1.8f)

        // Chevron Atas
        val topChevron = Path().apply {
            moveTo(w * 0.16f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.16f)
            lineTo(w * 0.84f, h * 0.46f)
        }
        drawPath(
            path = topChevron,
            brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, primaryColor)),
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Chevron Bawah
        val bottomChevron = Path().apply {
            moveTo(w * 0.16f, h * 0.82f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.82f)
        }
        drawPath(
            path = bottomChevron,
            color = primaryColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Komponen Avatar User + Avatar Decoration Milik User (Versi STATIS / Non-Animation)
 * Digunakan di Podium Top 1, 2, 3, di Daftar #4 ke bawah, dan di Sticky Bottom Bar.
 */
@Composable
private fun StaticUserAvatarWithBorder(
    avatarUrl: String,
    userName: String,
    borderUrl: String,
    avatarSize: Dp,
    rankBadgeNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val borderContainerSize = avatarSize * 1.22f
    val staticBorderUrl = remember(borderUrl) {
        if (borderUrl.isNotBlank()) {
            DiscordCosmeticsRepository.getOptimizedUrl(
                url = borderUrl.trim(),
                size = 240,
                animated = false
            )
        } else {
            ""
        }
    }

    val ringColor = when (rankBadgeNumber) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFFCBD5E1)
        3 -> Color(0xFFD97706)
        else -> Color(0xFF262B38)
    }

    Box(
        modifier = modifier.size(borderContainerSize),
        contentAlignment = Alignment.Center
    ) {
        // 1. Foto Profil Bulat di Tengah
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color(0xFF161820))
                .then(
                    if (staticBorderUrl.isBlank()) {
                        Modifier.border(
                            width = if (rankBadgeNumber != null) 2.dp else 1.dp,
                            color = ringColor,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = userName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = userName.take(1).uppercase().ifBlank { "-" },
                    color = Color.White,
                    fontSize = (avatarSize.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        // 2. Avatar Decoration Milik User (Versi STATIS / Frame Pertama ?passthrough=false)
        if (staticBorderUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(staticBorderUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(borderContainerSize)
                    .align(Alignment.Center)
            )
        }

        // 3. Lencana Angka Podium Minimalis (1, 2, 3) di Bawah Cincin Avatar
        if (rankBadgeNumber != null && rankBadgeNumber in 1..3) {
            val badgeColors = when (rankBadgeNumber) {
                1 -> listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                2 -> listOf(Color(0xFFCBD5E1), Color(0xFF475569))
                else -> listOf(Color(0xFFD97706), Color(0xFF7C2D12))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(badgeColors))
                    .border(1.dp, Color(0xFF090A0E), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    RoyalThreePeakCrownIcon(
                        size = 9.dp,
                        primaryColor = Color.White,
                        accentColor = Color.White
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$rankBadgeNumber",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = SfProFontFamily,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Garis Busur Melengkung Bercahaya di Bawah Avatar (Curved Horizon Pedestal Arch)
 */
@Composable
private fun CurvedHorizonPedestalArch(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val archColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF60A5FA)
        else -> Color(0xFFD97706)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val w = size.width
        val h = size.height

        val archPath = Path().apply {
            moveTo(w * 0.04f, h * 0.9f)
            quadraticTo(w * 0.5f, 2.dp.toPx(), w * 0.96f, h * 0.9f)
        }

        drawPath(
            path = archPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    archColor.copy(alpha = 0.9f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val glowPath = Path().apply {
            moveTo(w * 0.04f, h * 0.9f)
            quadraticTo(w * 0.5f, 2.dp.toPx(), w * 0.96f, h * 0.9f)
            lineTo(w * 0.96f, h)
            lineTo(w * 0.04f, h)
            close()
        }
        drawPath(
            path = glowPath,
            brush = Brush.verticalGradient(
                colors = listOf(archColor.copy(alpha = 0.15f), Color.Transparent)
            )
        )
    }
}

/**
 * Satu Spot Podium Lengkap (Top 1, 2, atau 3)
 * Pedestal lega agar Custom Title ("MYTHIC SOVEREIGN" dll.) tampil utuh 100% tanpa terpotong!
 */
@Composable
private fun EditorialPodiumSpot(
    rank: Int,
    user: LeaderboardUser?,
    avatarSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StaticUserAvatarWithBorder(
            avatarUrl = user?.avatar.orEmpty(),
            userName = user?.name ?: "-",
            borderUrl = user?.equippedBorderUrl.orEmpty(),
            avatarSize = avatarSize,
            rankBadgeNumber = rank
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Pedestal Melengkung Terbuka
        CurvedHorizonPedestalArch(rank = rank)

        Spacer(modifier = Modifier.height(4.dp))

        if (user != null) {
            // 1. Nama Pengguna
            Text(
                text = user.name,
                color = Color.White,
                fontSize = if (rank == 1) 15.5.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Indikator Level Bersih (Vector Rank Chevron + LV. X TANPA Kapsul Overlay & TANPA ▲)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                RankChevronVectorIcon(
                    level = user.level,
                    size = 12.dp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "LV. ${user.level}",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            // 3. Custom Title Badge (Compact = true agar MYTHIC SOVEREIGN tampil utuh 100%)
            if (user.equippedTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                GeometricTitleBadge(
                    titleName = user.equippedTitle,
                    tierLevel = user.equippedTitleTier,
                    styleVariant = user.equippedTitleVariant,
                    compact = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Total EXP
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = if (rank == 1) Color(0xFFF59E0B) else Color.White,
                fontSize = if (rank == 1) 14.5.sp else 12.5.sp,
                fontWeight = if (rank == 1) FontWeight.ExtraBold else FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Kosong",
                color = Color(0xFF475569),
                fontSize = 12.sp,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun TopThreeEditorialPodium(
    top1: LeaderboardUser?,
    top2: LeaderboardUser?,
    top3: LeaderboardUser?,
    currentUid: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // TOP 2 (Kiri) - avatarSize = 68.dp
        EditorialPodiumSpot(
            rank = 2,
            user = top2,
            avatarSize = 68.dp,
            modifier = Modifier.weight(1f)
        )

        // TOP 1 (Tengah - Lebih Tinggi & Besar) - avatarSize = 84.dp
        EditorialPodiumSpot(
            rank = 1,
            user = top1,
            avatarSize = 84.dp,
            modifier = Modifier.weight(1.12f)
        )

        // TOP 3 (Kanan) - avatarSize = 68.dp
        EditorialPodiumSpot(
            rank = 3,
            user = top3,
            avatarSize = 68.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Baris Kartu Peringkat #4 ke Bawah:
 * - Menampilkan Avatar Decoration milik user (Versi STATIS)
 * - Indikator Level menggunakan Vector Rank Chevron + LV. X + Pangkat/Title TANPA kapsul overlay
 */
@Composable
private fun ModernLeaderboardRowItem(
    user: LeaderboardUser,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrentUser) Color(0xFF19121E) else Color(0xFF11131A))
            .border(
                1.dp,
                if (isCurrentUser) Color(0xFFE11D48).copy(alpha = 0.6f) else Color(0xFF1E222D),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nomor Urut Peringkat
        Text(
            text = "${user.rank}",
            color = Color(0xFF94A3B8),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Foto Profil Bulat + Avatar Decoration Milik User (Versi STATIS)
        StaticUserAvatarWithBorder(
            avatarUrl = user.avatar,
            userName = user.name,
            borderUrl = user.equippedBorderUrl,
            avatarSize = 42.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Tengah: Baris atas Nama + Tag; Baris bawah Vector Rank Chevron + LV. X + Pangkat/Title (Tanpa Kapsul Overlay!)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "#${user.userTag.removePrefix("@").removePrefix("#")}",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RankChevronVectorIcon(
                    level = user.level,
                    size = 12.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LV. ${user.level}",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    color = Color(0xFF475569),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = user.equippedTitle.ifBlank { user.rankTitle },
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Kanan: Baris atas EXP, Baris bawah PrimoPointIcon + Poin
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrimoPointIcon(size = 12.dp)
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = FirebaseManager.formatNumber(user.points),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}

/**
 * Sticky Bottom Bar "Posisi Peringkat Anda" (URUTAN 1 Poin 5: Teks Tengah 100% Presisi Vertikal)
 */
@Composable
private fun CurrentUserRankBottomBar(
    currentUserProfile: UserProfile?,
    myRankEntry: LeaderboardUser?,
    totalUsers: Int,
    modifier: Modifier = Modifier
) {
    val rankNumber = myRankEntry?.rank ?: if (currentUserProfile != null && totalUsers > 0) totalUsers + 1 else 1
    val displayName = currentUserProfile?.name ?: myRankEntry?.name ?: "Kamu"
    val userTag = currentUserProfile?.userTag ?: myRankEntry?.userTag ?: "1001"
    val avatarUrl = currentUserProfile?.avatar ?: myRankEntry?.avatar ?: ""
    val borderUrl = currentUserProfile?.equippedBorderUrl ?: myRankEntry?.equippedBorderUrl ?: ""
    val exp = currentUserProfile?.exp ?: myRankEntry?.exp ?: 0L
    val level = currentUserProfile?.level ?: myRankEntry?.level ?: 1
    val rankTitle = currentUserProfile?.rankTitle ?: FirebaseManager.computeRankTitle(level)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1A0B10), Color(0xFF120D14))
                )
            )
            .border(1.dp, Color(0xFF9F1239), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kotak persegi merah (#E11D48, 38.dp, rounded 10.dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE11D48)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rankNumber",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Foto Profil Bulat + Border Statis Milik User
            StaticUserAvatarWithBorder(
                avatarUrl = avatarUrl,
                userName = displayName,
                borderUrl = borderUrl,
                avatarSize = 40.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Info Tengah: 100% Pas di Tengah Vertikal (verticalArrangement = Arrangement.Center)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "#${userTag.removePrefix("@").removePrefix("#")}",
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Posisi Peringkat Anda • $rankTitle",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Kanan: EXP & Level (Vector Rank Chevron + Lv. X)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${FirebaseManager.formatNumber(exp)} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RankChevronVectorIcon(
                        level = level,
                        size = 11.dp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "LV. $level",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

private data class XpRuleItem(
    val activity: String,
    val expText: String,
    val pointsText: String? = null
)

/**
 * Modal Dialog Penjelasan EXP & Poin (URUTAN 1 Poin 5: Rapi, Tidak Pernah Bertabrakan!)
 */
@Composable
private fun XpInfoModal(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF141724))
                .border(1.dp, Color(0xFF262C40), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cara Memperoleh EXP",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val rules = listOf(
                    XpRuleItem(
                        activity = "Menonton Episode Anime",
                        expText = "+20 EXP",
                        pointsText = "+10 Poin"
                    ),
                    XpRuleItem(
                        activity = "Membaca Chapter Komik",
                        expText = "+15 EXP",
                        pointsText = "+8 Poin"
                    ),
                    XpRuleItem(
                        activity = "Check-In Harian",
                        expText = "+50 EXP",
                        pointsText = "+10–50 Poin"
                    ),
                    XpRuleItem(
                        activity = "Menyelesaikan Misi Komunitas",
                        expText = "+100–300 EXP",
                        pointsText = null
                    )
                )

                rules.forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rule.activity,
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SparkBarChartVectorIcon(
                                    size = 11.dp,
                                    color = Color(0xFFF59E0B)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = rule.expText,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1
                                )
                            }
                            if (rule.pointsText != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PrimoPointIcon(size = 12.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = rule.pointsText,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF202538))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE11D48))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mengerti",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}
