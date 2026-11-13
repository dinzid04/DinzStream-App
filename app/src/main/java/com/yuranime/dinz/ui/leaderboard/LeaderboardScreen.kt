package com.yuranime.dinz.ui.leaderboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.LeaderboardUser
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * TAHAP 3: Halaman Papan Peringkat Real-Time (`LeaderboardScreen`)
 * - 100% Data Asli dari koleksi `users` di Firebase Firestore (TANPA BOT / PLACEHOLDER PALSU).
 * - Podium Top 1, Top 2, Top 3 menggunakan Border Khusus Presisi di Tengah Avatar:
 *   Top 1: https://url.dinzid.my.id/edTfBfx
 *   Top 2: https://url.dinzid.my.id/85MXtu1
 *   Top 3: https://url.dinzid.my.id/1KCdAyA
 * - Header tabel "Peringkat" bebas terpotong (`maxLines = 1`, `softWrap = false`).
 * - Baris 4+ bersih dan modern tanpa siku kuning kaku.
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
        leaderboardUsers.find { it.uid == currentUid }
    }

    if (showXpInfoModal) {
        XpInfoModal(onDismiss = { showXpInfoModal = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
    ) {
        // Cahaya atmosferik halus di belakang podium atas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24E11D48),
                            Color(0x12FF5722),
                            Color(0xFF0B0B0B)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Header Bar
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

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Papan Peringkat",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Sinkronisasi Real-Time Komunitas Yuranime",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }

                IconButton(
                    onClick = { showXpInfoModal = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14161E))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Cara Memperoleh EXP",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
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
                            text = "Memuat data peringkat asli dari database...",
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
                            text = "Jadilah member pertama yang menempati Podium Top 1 dengan menonton episode anime, membaca komik, atau klaim Check-In harian.",
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
                    contentPadding = PaddingValues(bottom = 112.dp)
                ) {
                    // 2. Podium TOP 1, TOP 2, TOP 3 dengan Border Khusus Presisi
                    item {
                        TopThreeLuxuryPodium(
                            top1 = top1User,
                            top2 = top2User,
                            top3 = top3User,
                            currentUid = currentUid
                        )
                    }

                    // 3. Header Kolom Tabel (Peringkat | Pengguna | Total EXP) - Anti Terpotong!
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF13151D))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Peringkat",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.width(68.dp)
                                )
                                Text(
                                    text = "Pengguna",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Total EXP",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // 4. Daftar Peringkat 4 ke Bawah (Bersih, Tanpa Siku Kuning Kaku)
                    if (remainingUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Menampilkan seluruh ${leaderboardUsers.size} pengguna aktif yang terdaftar di database saat ini.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.5.sp,
                                    fontFamily = SfProFontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(
                            items = remainingUsers,
                            key = { "${it.rank}_${it.uid}" }
                        ) { user ->
                            ModernLeaderboardRowItem(
                                user = user,
                                isCurrentUser = user.uid == currentUid
                            )
                        }
                    }
                }
            }
        }

        // 5. Floating Bar Bawah "Posisi Peringkat Kamu"
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
 * TAHAP 3: Podium TOP 1, TOP 2, dan TOP 3 dengan Border Khusus Presisi Konsentris
 */
@Composable
private fun TopThreeLuxuryPodium(
    top1: LeaderboardUser?,
    top2: LeaderboardUser?,
    top3: LeaderboardUser?,
    currentUid: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Posisi Kiri: TOP 2
        PodiumSpotColumn(
            rank = 2,
            user = top2,
            borderUrl = FirebaseManager.TOP_2_BORDER_URL,
            avatarSize = 58.dp,
            accentColor = Color(0xFFCBD5E1),
            pedestalHeight = 126.dp,
            isCurrentUser = top2?.uid != null && top2.uid == currentUid,
            modifier = Modifier.weight(1f)
        )

        // Posisi Tengah: TOP 1 (Lebih Tinggi & Menonjol)
        PodiumSpotColumn(
            rank = 1,
            user = top1,
            borderUrl = FirebaseManager.TOP_1_BORDER_URL,
            avatarSize = 72.dp,
            accentColor = Color(0xFFF59E0B),
            pedestalHeight = 148.dp,
            isCurrentUser = top1?.uid != null && top1.uid == currentUid,
            modifier = Modifier.weight(1.12f)
        )

        // Posisi Kanan: TOP 3
        PodiumSpotColumn(
            rank = 3,
            user = top3,
            borderUrl = FirebaseManager.TOP_3_BORDER_URL,
            avatarSize = 58.dp,
            accentColor = Color(0xFFFB923C),
            pedestalHeight = 118.dp,
            isCurrentUser = top3?.uid != null && top3.uid == currentUid,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PodiumSpotColumn(
    rank: Int,
    user: LeaderboardUser?,
    borderUrl: String,
    avatarSize: Dp,
    accentColor: Color,
    pedestalHeight: Dp,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val borderFrameSize = avatarSize * 1.38f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar + Custom Podium Border (100% Konsentris di Tengah)
        Box(
            modifier = Modifier.size(borderFrameSize),
            contentAlignment = Alignment.Center
        ) {
            // Layer Dalam: Foto Profil Bulat Pengguna
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(Color(0xFF161820)),
                contentAlignment = Alignment.Center
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
                    Text(
                        text = user?.name?.take(1)?.uppercase() ?: "-",
                        color = if (user != null) Color.White else Color(0xFF475569),
                        fontSize = if (rank == 1) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Layer Luar: Gambar Border Khusus Top 1 / Top 2 / Top 3 (Alignment.Center, ContentScale.Fit)
            AsyncImage(
                model = borderUrl,
                contentDescription = "Border Top $rank",
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.size(borderFrameSize)
            )

            // Badge Nomor Peringkat di Bawah Frame
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF13151D))
                    .border(1.dp, accentColor, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TOP $rank",
                    color = accentColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Kartu Pedestal Podium Bersih (Tanpa Pita Bertumpuk yang Memotong Angka)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (rank == 1) {
                            listOf(Color(0xFF221518), Color(0xFF13151D))
                        } else {
                            listOf(Color(0xFF171A24), Color(0xFF11131A))
                        }
                    )
                )
                .border(
                    width = if (isCurrentUser || rank == 1) 1.dp else 0.5.dp,
                    color = when {
                        isCurrentUser -> Color(0xFFE11D48)
                        rank == 1 -> Color(0x66F59E0B)
                        else -> Color(0xFF1F2433)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (user != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = user.name,
                        color = Color.White,
                        fontSize = if (rank == 1) 13.5.sp else 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Lv. ${user.level} • #${user.userTag}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily,
                        maxLines = 1
                    )
                }

                // Title Badge Ringkas atau Gelar Level
                if (user.equippedTitle.isNotBlank()) {
                    GeometricTitleBadge(
                        titleName = user.equippedTitle,
                        tierLevel = maxOf(user.equippedTitleTier, if (rank == 1) 31 else user.level),
                        styleVariant = user.equippedTitleVariant,
                        compact = true
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1C202E))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = user.rankTitle,
                            color = Color(0xFFCBD5E1),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Angka EXP Utuh dalam SF Bold (Tidak Terpotong)
                Text(
                    text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                    color = accentColor,
                    fontSize = if (rank == 1) 13.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kosong",
                        color = Color(0xFF475569),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

/**
 * TAHAP 3: Baris Peringkat 4 ke Bawah (Bersih, Modern, Tanpa Siku Kuning Kaku)
 */
@Composable
private fun ModernLeaderboardRowItem(
    user: LeaderboardUser,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrentUser) Color(0xFF1E1319) else Color(0xFF13151D))
            .border(
                width = if (isCurrentUser) 1.dp else 0.5.dp,
                color = if (isCurrentUser) Color(0xFFE11D48) else Color(0xFF1B1F2C),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kiri: Nomor Peringkat
        Box(
            modifier = Modifier.width(46.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "#${user.rank}",
                color = if (isCurrentUser) Color(0xFFFF5722) else Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                softWrap = false
            )
        }

        // Tengah: Avatar + Nama + Badge Level/Title
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C202E)),
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            if (user.equippedBorderUrl.isNotBlank()) {
                AsyncImage(
                    model = user.equippedBorderUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "#${user.userTag}",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1C202E))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Lv. ${user.level}",
                        color = Color(0xFFFF5722),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                if (user.equippedTitle.isNotBlank()) {
                    GeometricTitleBadge(
                        titleName = user.equippedTitle,
                        tierLevel = user.equippedTitleTier,
                        styleVariant = user.equippedTitleVariant,
                        compact = true
                    )
                } else {
                    Text(
                        text = user.rankTitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Kanan: Total EXP
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "✦ ${FirebaseManager.formatNumber(user.points)}",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * TAHAP 3: Floating Bar Bawah "Posisi Peringkat Kamu" yang Bersih & Sinkron Database
 */
@Composable
private fun CurrentUserRankBottomBar(
    currentUserProfile: UserProfile?,
    myRankEntry: LeaderboardUser?,
    totalUsers: Int,
    modifier: Modifier = Modifier
) {
    val rankLabel = when {
        myRankEntry != null -> "#${myRankEntry.rank}"
        currentUserProfile != null && totalUsers > 0 -> "#${totalUsers + 1}"
        else -> "-"
    }
    val displayName = currentUserProfile?.name ?: myRankEntry?.name ?: "Kamu"
    val userTag = currentUserProfile?.userTag ?: myRankEntry?.userTag ?: "1001"
    val avatarUrl = currentUserProfile?.avatar ?: myRankEntry?.avatar ?: ""
    val exp = currentUserProfile?.exp ?: myRankEntry?.exp ?: 0L
    val level = currentUserProfile?.level ?: myRankEntry?.level ?: 1
    val rankTitle = currentUserProfile?.rankTitle ?: FirebaseManager.computeRankTitle(level)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14161E))
            .border(1.dp, Color(0x40E11D48), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Posisi Peringkat Kamu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rankLabel,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Foto Profil
            Box(
                modifier = Modifier
                    .size(40.dp)
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

            Spacer(modifier = Modifier.width(11.dp))

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
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "#$userTag",
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
                Text(
                    text = "Posisi Peringkat Kamu • $rankTitle",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${FirebaseManager.formatNumber(exp)} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}

@Composable
private fun XpInfoModal(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF14161E))
                .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
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
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            XpSourceRow(
                title = "Check-In Harian",
                xp = "+50 EXP",
                desc = "Klaim kehadiran setiap hari di halaman Points & Rewards"
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1F2433))
            Spacer(modifier = Modifier.height(10.dp))
            XpSourceRow(
                title = "Tonton Episode Anime",
                xp = "+100 EXP",
                desc = "Tonton episode anime dan klaim misi harianmu"
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1F2433))
            Spacer(modifier = Modifier.height(10.dp))
            XpSourceRow(
                title = "Baca Chapter Komik",
                xp = "+100 EXP",
                desc = "Baca chapter manga, manhwa, atau manhua favorit"
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1F2433))
            Spacer(modifier = Modifier.height(10.dp))
            XpSourceRow(
                title = "Kirim Komentar & Diskusi",
                xp = "+50 EXP",
                desc = "Berinteraksi aktif di kolom komentar episode"
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                        )
                    )
                    .clickable { onDismiss() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mengerti",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = SfProFontFamily
                )
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = xp,
            color = Color(0xFFFF5722),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
    }
}
