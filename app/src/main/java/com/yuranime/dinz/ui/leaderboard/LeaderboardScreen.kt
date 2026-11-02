package com.yuranime.dinz.ui.leaderboard

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.LeaderboardUser
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.text.NumberFormat
import java.util.Locale

/**
 * Shape perisai / panji runcing ke bawah (persis referensi TOP 1, TOP 2, TOP 3)
 */
val ShieldBannerShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.76f)
    lineTo(size.width * 0.5f, size.height)
    lineTo(0f, size.height * 0.76f)
    close()
}

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val leaderboardUsers by FirebaseManager.leaderboard.collectAsState()
    val currentUserProfile by FirebaseManager.userProfile.collectAsState()
    var showXpInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseManager.startListeningLeaderboard()
    }

    val top1 = leaderboardUsers.getOrNull(0)
    val top2 = leaderboardUsers.getOrNull(1)
    val top3 = leaderboardUsers.getOrNull(2)
    val ranks4to100 = remember(leaderboardUsers) {
        if (leaderboardUsers.size > 3) leaderboardUsers.drop(3) else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090616),
                        Color(0xFF0D0A22),
                        Color(0xFF070512)
                    )
                )
            )
    ) {
        // Efek Bintang / Nebula Lembut di Latar Belakang
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x337C3AED), Color(0x114C1D95), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.22f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Papan Peringkat",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.weight(1f)
                )

                // Info Dialog Button
                IconButton(onClick = { showXpInfoDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Info XP",
                        tint = Color(0xFFFDE68A)
                    )
                }
            }

            // Scrollable Area (Podium + Table Header + LazyList)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 1. Area Podium TOP 1, TOP 2, TOP 3
                item {
                    PodiumView(
                        top1 = top1,
                        top2 = top2,
                        top3 = top3
                    )
                }

                // 2. Baris Header Tabel
                item {
                    TableHeaderRow(onHelpClick = { showXpInfoDialog = true })
                }

                // 3. Daftar Kartu Peringkat 4 s/d 100
                items(ranks4to100, key = { it.uid + it.rank }) { user ->
                    LeaderboardCardItem(user = user)
                }

                if (ranks4to100.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sedang memuat data peringkat...",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 4. Sticky Bottom Bar: Peringkat Pengguna Saat Ini
        StickyCurrentUserBar(
            currentUser = currentUserProfile,
            leaderboard = leaderboardUsers,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Dialog Panduan Perolehan XP
        if (showXpInfoDialog) {
            XpInfoModal(onDismiss = { showXpInfoDialog = false })
        }
    }
}

/**
 * Komponen Podium Atas: TOP 2 (Kiri), TOP 1 (Tengah Atas), TOP 3 (Kanan)
 * Persis mengacu tata letak gambar referensi!
 */
@Composable
private fun PodiumView(
    top1: LeaderboardUser?,
    top2: LeaderboardUser?,
    top3: LeaderboardUser?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .height(260.dp)
    ) {
        // TOP 2 (Kiri)
        if (top2 != null) {
            PodiumPillarItem(
                user = top2,
                rank = 2,
                avatarSize = 68.dp,
                badgeText = "TOP2",
                wingColor = Color(0xFFA5B4FC),
                haloColor = Color(0xFF818CF8),
                bannerGradient = listOf(Color(0xFFE0E7FF), Color(0xFFA5B4FC), Color(0xFF818CF8)),
                textColor = Color(0xFF1E1B4B),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = 4.dp)
                    .width(104.dp)
            )
        }

        // TOP 1 (Tengah - Paling Tinggi & Besar)
        if (top1 != null) {
            PodiumPillarItem(
                user = top1,
                rank = 1,
                avatarSize = 84.dp,
                badgeText = "TOP1",
                wingColor = Color(0xFFFDE047),
                haloColor = Color(0xFFF59E0B),
                bannerGradient = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                textColor = Color(0xFF451A03),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(124.dp)
            )
        }

        // TOP 3 (Kanan - Sejajar TOP 2)
        if (top3 != null) {
            PodiumPillarItem(
                user = top3,
                rank = 3,
                avatarSize = 68.dp,
                badgeText = "TOP3",
                wingColor = Color(0xFFFDA4AF),
                haloColor = Color(0xFFFB7185),
                bannerGradient = listOf(Color(0xFFFFF1F2), Color(0xFFFECDD3), Color(0xFFFB7185), Color(0xFFE11D48)),
                textColor = Color(0xFF4C0519),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 4.dp)
                    .width(104.dp)
            )
        }
    }
}

/**
 * Satu Pilar Podium (Avatar Bulat + Ornamen Sayap + Badge Pelat + Panji Perisai Runcing)
 */
@Composable
private fun PodiumPillarItem(
    user: LeaderboardUser,
    rank: Int,
    avatarSize: androidx.compose.ui.unit.Dp,
    badgeText: String,
    wingColor: Color,
    haloColor: Color,
    bannerGradient: List<Color>,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedExp = remember(user.exp) {
        NumberFormat.getNumberInstance(Locale.US).format(user.exp).replace(",", "")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Container Avatar & Sayap
        Box(
            modifier = Modifier.size(avatarSize + 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ornamen Sayap & Cahaya Belakang (Canvas Custom Angel Wings)
            PodiumWingsCanvas(
                wingColor = wingColor,
                haloColor = haloColor,
                modifier = Modifier.fillMaxSize()
            )

            // Foto Profil Bulat
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(2.5.dp, wingColor, CircleShape)
                    .background(Color(0xFF1E1B4B)),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatar.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.avatar)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_yuranime_monochrome)
                            .error(R.drawable.ic_yuranime_monochrome)
                            .build(),
                        contentDescription = user.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = wingColor,
                        modifier = Modifier.size(avatarSize * 0.55f)
                    )
                }
            }

            // Pelat Logam Badge "TOP 1", "TOP 2", "TOP 3" di Bawah Avatar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(wingColor, haloColor)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeText,
                    color = textColor,
                    fontSize = if (rank == 1) 10.sp else 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Panji Perisai Runcing ke Bawah (Shield Banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (rank == 1) 78.dp else 68.dp)
                .clip(ShieldBannerShape)
                .background(Brush.verticalGradient(bannerGradient))
                .border(1.dp, Color.White.copy(alpha = 0.4f), ShieldBannerShape)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = user.name,
                    color = textColor,
                    fontSize = if (rank == 1) 12.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedExp,
                    color = textColor,
                    fontSize = if (rank == 1) 13.sp else 11.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = SfProFontFamily,
                    maxLines = 1
                )
            }

            // Belah Ketupat Emas Kecil di Titik Bawah Perisai
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
                    .size(6.dp)
                    .rotate(45f)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Menggambar Sayap & Halo Bercahaya Lembut di Belakang Avatar Podium
 */
@Composable
private fun PodiumWingsCanvas(
    wingColor: Color,
    haloColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Lingkaran Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(haloColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.46f
            )
        )

        // Sayap Kiri
        val leftWingPath = Path().apply {
            moveTo(cx - 10f, cy - 20f)
            cubicTo(cx - w * 0.45f, cy - h * 0.45f, cx - w * 0.5f, cy, cx - 18f, cy + 12f)
            cubicTo(cx - w * 0.38f, cy + 6f, cx - w * 0.25f, cy - 8f, cx - 10f, cy - 20f)
            close()
        }
        drawPath(
            path = leftWingPath,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), wingColor),
                start = Offset(cx, cy - 20f),
                end = Offset(cx - w * 0.45f, cy - h * 0.4f)
            )
        )

        // Sayap Kanan
        val rightWingPath = Path().apply {
            moveTo(cx + 10f, cy - 20f)
            cubicTo(cx + w * 0.45f, cy - h * 0.45f, cx + w * 0.5f, cy, cx + 18f, cy + 12f)
            cubicTo(cx + w * 0.38f, cy + 6f, cx + w * 0.25f, cy - 8f, cx + 10f, cy - 20f)
            close()
        }
        drawPath(
            path = rightWingPath,
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), wingColor),
                start = Offset(cx, cy - 20f),
                end = Offset(cx + w * 0.45f, cy - h * 0.4f)
            )
        )
    }
}

/**
 * Baris Pembatas Tabel (Peringkat, Pengguna, Total XP ?)
 */
@Composable
private fun TableHeaderRow(
    onHelpClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label Kiri
        Text(
            text = "Peringkat",
            color = Color(0xFFA5B4FC).copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily,
            modifier = Modifier.width(72.dp)
        )

        // Label Tengah
        Text(
            text = "Pengguna",
            color = Color(0xFFA5B4FC).copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily,
            modifier = Modifier.weight(1f)
        )

        // Label Kanan + Ikon Belah Ketupat ?
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onHelpClick() }
        ) {
            Text(
                text = "Total XP",
                color = Color(0xFFA5B4FC).copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Belah Ketupat ?
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .rotate(45f)
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(2.dp))
                    .background(Color(0xFF1E1B4B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = Color(0xFFFDE68A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(-45f)
                )
            }
        }
    }
}

/**
 * Kartu Baris Peringkat 4 s/d 100
 * Ornamen Sudut Emas Tipis + Belah Ketupat Putih + Avatar Bulat + Nama + Skor Emas
 */
@Composable
private fun LeaderboardCardItem(
    user: LeaderboardUser
) {
    val context = LocalContext.current
    val formattedExp = remember(user.exp) {
        NumberFormat.getNumberInstance(Locale.US).format(user.exp).replace(",", "")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.5.dp)
    ) {
        // Kartu Utama
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141026))
                .border(0.7.dp, Color(0xFF382A6E), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Belah Ketupat Putih Rotasi 45° dengan Angka Peringkat
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(45f)
                        .background(Color.White)
                        .shadow(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.rank.toString(),
                        color = Color(0xFF0F0B1E),
                        fontSize = if (user.rank > 99) 11.sp else 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.rotate(-45f)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 2. Avatar Pengguna Bulat (46.dp)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF818CF8).copy(alpha = 0.6f), CircleShape)
                        .background(Color(0xFF1F1A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(user.avatar)
                                .crossfade(true)
                                .placeholder(R.drawable.ic_yuranime_monochrome)
                                .error(R.drawable.ic_yuranime_monochrome)
                                .build(),
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                            contentDescription = null,
                            tint = Color(0xFFC7D2FE),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 3. Nama Pengguna (SF Bold Putih 15.sp)
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 4. Skor XP Emas Terang (SF Bold #FDE68A)
                Text(
                    text = formattedExp,
                    color = Color(0xFFFDE68A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        // Aksen Ornamen Sudut Emas di 4 Pojok Kartu (Persis Referensi Gambar)
        GildedCornersCanvas(
            modifier = Modifier.matchParentSize()
        )
    }
}

/**
 * Garis Aksen Sudut Emas Tipis pada Keempat Pojok Kartu
 */
@Composable
private fun GildedCornersCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerLen = 14.dp.toPx()
        val goldColor = Color(0xFFFACC15)
        val strokeWidth = 1.3.dp.toPx()

        // Pojok Kiri Atas
        drawLine(goldColor, Offset(0f, cornerLen), Offset(0f, 0f), strokeWidth)
        drawLine(goldColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth)

        // Pojok Kanan Atas
        drawLine(goldColor, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeWidth)
        drawLine(goldColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth)

        // Pojok Kiri Bawah
        drawLine(goldColor, Offset(0f, h - cornerLen), Offset(0f, h), strokeWidth)
        drawLine(goldColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth)

        // Pojok Kanan Bawah
        drawLine(goldColor, Offset(w - cornerLen, h), Offset(w, h), strokeWidth)
        drawLine(goldColor, Offset(w, h), Offset(w, h - cornerLen), strokeWidth)
    }
}

/**
 * Sticky Bottom Bar: Peringkat Pengguna Saat Ini
 */
@Composable
private fun StickyCurrentUserBar(
    currentUser: com.yuranime.dinz.data.firebase.UserProfile?,
    leaderboard: List<LeaderboardUser>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUid = FirebaseManager.getCurrentUid()

    val myRank = remember(currentUser, leaderboard) {
        if (currentUid == null) null
        else leaderboard.indexOfFirst { it.uid == currentUid }.takeIf { it >= 0 }?.plus(1) ?: 99
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF24164A),
                        Color(0xFF3B1D78),
                        Color(0xFF24164A)
                    )
                )
            )
            .border(1.dp, Color(0xFFFDE68A).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Belah Ketupat Peringkat Pengguna
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .rotate(45f)
                    .background(Color(0xFFFDE68A)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${myRank ?: "-"}",
                    color = Color(0xFF3B1D78),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.rotate(-45f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar Pengguna
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color(0xFFFDE68A), CircleShape)
                    .background(Color(0xFF1E1B4B)),
                contentAlignment = Alignment.Center
            ) {
                if (!currentUser?.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentUser?.avatar)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_yuranime_monochrome)
                            .error(R.drawable.ic_yuranime_monochrome)
                            .build(),
                        contentDescription = "Avatar Pengguna",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.name ?: "Kamu",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Posisi Peringkat Kamu",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
            }

            // Total XP Pengguna
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currentUser?.exp ?: 0L} XP",
                    color = Color(0xFFFDE68A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Lv. ${currentUser?.level ?: 1}",
                    color = Color(0xFFA5B4FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}

/**
 * Modal Dialog Penjelasan Cara Mendapatkan XP
 */
@Composable
private fun XpInfoModal(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF17132B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C3682))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cara Memperoleh XP",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                XpSourceRow(title = "Nonton 1 Episode", xp = "+20 XP", desc = "Dapatkan XP setiap kali menyelesaikan tontonan")
                Spacer(modifier = Modifier.height(10.dp))
                XpSourceRow(title = "Check-in Harian", xp = "+50 XP", desc = "Klaim check-in setiap hari di halaman Points & Rewards")
                Spacer(modifier = Modifier.height(10.dp))
                XpSourceRow(title = "Beri Komentar & Reaksi", xp = "+10 XP", desc = "Berinteraksi aktif di kolom diskusi episode")

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF251B47))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mengerti",
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun XpSourceRow(
    title: String,
    xp: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2B1F57)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = null,
                tint = Color(0xFFFDE68A),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily
            )
        }

        Text(
            text = xp,
            color = Color(0xFFFDE68A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
    }
}
