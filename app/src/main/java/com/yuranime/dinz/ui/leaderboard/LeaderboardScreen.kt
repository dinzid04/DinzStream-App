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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.LeaderboardUser
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Halaman Papan Peringkat Real-Time 100% Persis Referensi 1000127571.png
 * - Aurora wave gradasi merah marun lembut di latar atas
 * - Ornamen Mahkota Merah ♛ dan garis hiasan simetris
 * - Podium Top 1, Top 2, Top 3 dengan PodiumLaurelCrownFrame & Curved Horizon Pedestal Arch
 * - Bar Header # Pengguna & Total EXP dengan pendaran neon merah
 * - Baris 4+ bersih dengan PrimoPointIcon
 * - Sticky Bottom Bar Posisi Peringkat Anda ber-border merah menyala
 */
@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    val leaderboardUsers by FirebaseManager.leaderboard.collectAsState()
    val isLoading by FirebaseManager.isLeaderboardLoading.collectAsState()
    val currentUserProfile by FirebaseManager.userProfile.collectAsState()
    val currentUid = FirebaseManager.getCurrentUid()

    var showXpInfoModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
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

    val myRankEntry = remember(leaderboardUsers, currentUid) {
        if (!currentUid.isNullOrBlank()) {
            leaderboardUsers.firstOrNull { it.uid == currentUid }
        } else {
            null
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
            // 1. HEADER ATAS (Persis 1000127571.png)
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Tombol Bulat Gelap Kiri (Panah <-)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14161E))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Bagian Tengah: Ikon Mahkota Merah ♛ + Judul + Subteks + Ornamen Simetris
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "♛",
                        color = Color(0xFFE11D48),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(1.dp))

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

                    Spacer(modifier = Modifier.height(4.dp))

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

                // Tombol Bulat Gelap Kanan (Tanda Tanya ?)
                IconButton(
                    onClick = { showXpInfoModal = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14161E))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Bantuan EXP",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(19.dp)
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
                    // 2. PODIUM TOP 1, 2, 3 (MAHKOTA + SAYAP DAUN LAUREL + PEDESTAL MELENGKUNG)
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
                    // 3. BAR HEADER # PENGGUNA & TOTAL EXP
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
                                    Text(
                                        text = "#",
                                        color = Color(0xFFE11D48),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pengguna",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📊",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
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
 * Komponen Vektor Bingkai Mahkota + Sayap Daun Laurel Wreath + Plakat Ribbon
 * 100% Mengikuti Gambar Referensi 1000127571.png
 */
@Composable
private fun PodiumLaurelCrownFrame(
    rank: Int,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    avatarContent: @Composable () -> Unit
) {
    val crownColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFFD97706)
    }
    val laurelColor = when (rank) {
        1 -> Color(0xFFFBBF24)
        2 -> Color(0xFFA5B4FC)
        else -> Color(0xFFEA580C)
    }
    val plaqueText = "TOP $rank"
    val plaqueBg = when (rank) {
        1 -> listOf(Color(0xFFF59E0B), Color(0xFFB45309))
        2 -> listOf(Color(0xFF94A3B8), Color(0xFF475569))
        else -> listOf(Color(0xFFD97706), Color(0xFF7C2D12))
    }

    Box(
        modifier = modifier.size(avatarSize + 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = (avatarSize.toPx() / 2f) + 2.dp.toPx()

            // 1. Cincin Ganda Memeluk Avatar
            drawCircle(
                color = crownColor,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = crownColor.copy(alpha = 0.35f),
                radius = r + 2.5.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Mahkota 3 Sudut di Puncak Atas
            val crownTopY = cy - r - 12.dp.toPx()
            val crownBaseY = cy - r - 1.dp.toPx()
            val crownHalfW = 12.dp.toPx()

            val crownPath = Path().apply {
                moveTo(cx - crownHalfW, crownBaseY)
                lineTo(cx - crownHalfW, crownTopY + 3.dp.toPx())
                lineTo(cx - crownHalfW * 0.45f, crownBaseY - 4.dp.toPx())
                lineTo(cx, crownTopY)
                lineTo(cx + crownHalfW * 0.45f, crownBaseY - 4.dp.toPx())
                lineTo(cx + crownHalfW, crownTopY + 3.dp.toPx())
                lineTo(cx + crownHalfW, crownBaseY)
                close()
            }
            drawPath(crownPath, color = crownColor)

            // Permata Ruby Merah di Mahkota Top 1
            if (rank == 1) {
                drawCircle(
                    color = Color(0xFFE11D48),
                    radius = 2.dp.toPx(),
                    center = Offset(cx, crownBaseY - 3.5.dp.toPx())
                )
            }

            // 3. Sayap Daun Laurel Wreath Melengkung di Kiri dan Kanan Cincin
            val leafRadiusX = 3.5.dp.toPx()
            val leafRadiusY = 1.8.dp.toPx()
            val leafAngles = listOf(-60f, -40f, -20f, 0f, 20f, 40f)

            // Daun Sisi Kiri
            for (ang in leafAngles) {
                val rad = Math.toRadians((180f + ang).toDouble())
                val lx = cx + (r + 4.dp.toPx()) * Math.cos(rad).toFloat()
                val ly = cy + (r + 4.dp.toPx()) * Math.sin(rad).toFloat()
                drawOval(
                    color = laurelColor.copy(alpha = 0.85f),
                    topLeft = Offset(lx - leafRadiusX, ly - leafRadiusY),
                    size = androidx.compose.ui.geometry.Size(leafRadiusX * 2, leafRadiusY * 2)
                )
            }

            // Daun Sisi Kanan
            for (ang in leafAngles) {
                val rad = Math.toRadians((0f - ang).toDouble())
                val rx = cx + (r + 4.dp.toPx()) * Math.cos(rad).toFloat()
                val ry = cy + (r + 4.dp.toPx()) * Math.sin(rad).toFloat()
                drawOval(
                    color = laurelColor.copy(alpha = 0.85f),
                    topLeft = Offset(rx - leafRadiusX, ry - leafRadiusY),
                    size = androidx.compose.ui.geometry.Size(leafRadiusX * 2, leafRadiusY * 2)
                )
            }
        }

        // Foto Profil Bulat di Tengah Cincin
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            avatarContent()
        }

        // 4. Plakat Pita Heksagonal Membulat di Bawah (TOP 1 / TOP 2 / TOP 3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-2).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(plaqueBg))
                .border(0.75.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = plaqueText,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = SfProFontFamily,
                letterSpacing = 0.5.sp
            )
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
            .height(28.dp)
    ) {
        val w = size.width
        val h = size.height

        val archPath = Path().apply {
            moveTo(w * 0.05f, h * 0.9f)
            quadraticTo(w * 0.5f, 2.dp.toPx(), w * 0.95f, h * 0.9f)
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
            moveTo(w * 0.05f, h * 0.9f)
            quadraticTo(w * 0.5f, 2.dp.toPx(), w * 0.95f, h * 0.9f)
            lineTo(w * 0.95f, h)
            lineTo(w * 0.05f, h)
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
 */
@Composable
private fun EditorialPodiumSpot(
    rank: Int,
    user: LeaderboardUser?,
    avatarSize: Dp,
    accentColor: Color,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PodiumLaurelCrownFrame(
            rank = rank,
            avatarSize = avatarSize
        ) {
            if (user != null && user.avatar.isNotBlank()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF161820)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.name?.take(1)?.uppercase() ?: "-",
                        color = Color.White,
                        fontSize = if (rank == 1) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Pedestal Melengkung Terbuka
        CurvedHorizonPedestalArch(rank = rank)

        Spacer(modifier = Modifier.height(4.dp))

        if (user != null) {
            // 1. Username
            Text(
                text = user.name,
                color = Color.White,
                fontSize = if (rank == 1) 16.sp else 14.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 2. Kapsul Level Ramping (^ LV. X)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF161922))
                    .border(0.75.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▲",
                    color = Color(0xFFFF5722),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "LV. ${user.level}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }

            // 3. Custom Title Badge (Jika Ada)
            if (user.equippedTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                GeometricTitleBadge(
                    title = user.equippedTitle,
                    tier = user.equippedTitleTier,
                    variant = user.equippedTitleVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Total EXP
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = if (rank == 1) Color(0xFFF59E0B) else Color.White,
                fontSize = if (rank == 1) 15.sp else 13.sp,
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
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // TOP 2 (Kiri) - avatarSize = 70.dp
        EditorialPodiumSpot(
            rank = 2,
            user = top2,
            avatarSize = 70.dp,
            accentColor = Color(0xFF94A3B8),
            isCurrentUser = top2?.uid != null && top2.uid == currentUid,
            modifier = Modifier.weight(1f)
        )

        // TOP 1 (Tengah - Lebih Tinggi & Besar) - avatarSize = 86.dp
        EditorialPodiumSpot(
            rank = 1,
            user = top1,
            avatarSize = 86.dp,
            accentColor = Color(0xFFF59E0B),
            isCurrentUser = top1?.uid != null && top1.uid == currentUid,
            modifier = Modifier.weight(1.18f)
        )

        // TOP 3 (Kanan) - avatarSize = 70.dp
        EditorialPodiumSpot(
            rank = 3,
            user = top3,
            avatarSize = 70.dp,
            accentColor = Color(0xFFD97706),
            isCurrentUser = top3?.uid != null && top3.uid == currentUid,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Baris Kartu Peringkat #4 ke Bawah (Bersih, Rapi & Elegan)
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank #
        Text(
            text = "#${user.rank}",
            color = Color(0xFF64748B),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Foto Profil Bulat 44.dp
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2230)),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatar.isNotBlank()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Tengah: Baris atas nama + #tag; Baris bawah kapsul level + title
        Column(modifier = Modifier.weight(1f)) {
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
                    text = "#${user.userTag.removePrefix("@")}",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Kapsul border merah tipis ^ LV. 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(0.75.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "▲ LV. ${user.level}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

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

        // Kanan: Baris atas EXP, Baris bawah PrimoPointIcon + poin
        Column(horizontalAlignment = Alignment.End) {
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
 * Sticky Bottom Bar "Posisi Peringkat Anda" (Persis 1000127571.png)
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Foto Profil Bulat 42.dp
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2230)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Info Tengah
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "#${userTag.removePrefix("@")}",
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Posisi Peringkat Anda • $rankTitle",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Kanan: EXP & Level
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${FirebaseManager.formatNumber(exp)} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Lv. $level",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}

/**
 * Modal Dialog Penjelasan EXP & Poin
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
                    "Menonton Episode Anime" to "+20 EXP & +10 Poin",
                    "Membaca Chapter Komik" to "+15 EXP & +8 Poin",
                    "Check-In Harian" to "+50 EXP & +10-50 Poin",
                    "Menyelesaikan Misi Komunitas" to "+100-300 EXP"
                )

                rules.forEach { (activity, reward) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activity,
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = reward,
                            color = Color(0xFFF59E0B),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF202538))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE11D48))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
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
