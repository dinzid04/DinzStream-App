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
import com.yuranime.dinz.ui.components.SleekHexChevronLevelTag
import com.yuranime.dinz.ui.theme.JakartaSansFontFamily
import com.yuranime.dinz.ui.theme.JetBrainsMonoFontFamily
import com.yuranime.dinz.ui.theme.OutfitDisplayFontFamily
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Halaman Papan Peringkat Real-Time (`LeaderboardScreen`)
 * - 100% Data Asli dari koleksi `users` di Firebase Firestore (TANPA BOT / PLACEHOLDER PALSU).
 * - Podium Top 1, Top 2, Top 3 Konsentris Presisi dengan Border Khusus (Tanpa Box Kaku & Tanpa Teks Terpotong):
 *   Top 1: https://url.dinzid.my.id/edTfBfx (Hole center y=25.69%, diameter 35.32%)
 *   Top 2: https://url.dinzid.my.id/85MXtu1 (Hole center y=32.00%, diameter 49.12%)
 *   Top 3: https://url.dinzid.my.id/1KCdAyA (Hole center y=26.33%, diameter 37.72%)
 * - Tipografi 3-Layer: Outfit (Display/Header), Jakarta Sans (Body), JetBrains Mono (Metrik/Angka).
 * - Header tabel 3 kolom terpisah (# | PENGGUNA | TOTAL EXP).
 * - Baris 4+ minimalis editorial modern tanpa siku kuning kaku.
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
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x28E11D48),
                            Color(0x14FF5722),
                            Color(0x000B0B0B)
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
                        fontFamily = OutfitDisplayFontFamily
                    )
                    Text(
                        text = "Sinkronisasi Real-Time Komunitas Yuranime",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = JakartaSansFontFamily
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
                            fontFamily = JakartaSansFontFamily
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
                            fontFamily = OutfitDisplayFontFamily
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jadilah member pertama yang menempati Podium Top 1 dengan menonton anime, membaca komik, atau klaim Check-In harian.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontFamily = JakartaSansFontFamily,
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
                    // 2. Podium TOP 1, TOP 2, TOP 3 dengan Presisi Konsentris
                    item {
                        TopThreeEditorialPodium(
                            top1 = top1User,
                            top2 = top2User,
                            top3 = top3User,
                            currentUid = currentUid
                        )
                    }

                    // 3. Header Kolom Tabel (# | PENGGUNA | TOTAL EXP) - Bersih & Anti Terpotong
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF10121A))
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "PENGGUNA",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JakartaSansFontFamily,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "TOTAL EXP",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    letterSpacing = 0.8.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // 4. Daftar Peringkat 4 ke Bawah (Bersih, Minimalis Editorial)
                    if (remainingUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Menampilkan seluruh ${leaderboardUsers.size} pengguna aktif terdaftar saat ini.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.5.sp,
                                    fontFamily = JakartaSansFontFamily,
                                    textAlign = TextAlign.Center
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

        // 5. Floating Bottom Bar "Posisi Peringkat Kamu"
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
 * Podium TOP 1, TOP 2, dan TOP 3 Tanpa Kotak Kaku Pedestal
 * Avatar ditempatkan konsentris di tengah cincin dekorasi border:
 * - Top 1: hole center y = 25.69%
 * - Top 2: hole center y = 32.00%
 * - Top 3: hole center y = 26.33%
 */
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
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // TOP 2 (Kiri)
        EditorialPodiumSpot(
            rank = 2,
            user = top2,
            borderUrl = FirebaseManager.TOP_2_BORDER_URL,
            frameWidth = 110.dp,
            frameHeight = 135.dp,
            avatarSize = 50.dp,
            avatarTopOffset = 18.dp,
            accentColor = Color(0xFFE2E8F0),
            isCurrentUser = top2?.uid != null && top2.uid == currentUid,
            modifier = Modifier.weight(1f)
        )

        // TOP 1 (Tengah - Terbesar & Paling Menonjol)
        EditorialPodiumSpot(
            rank = 1,
            user = top1,
            borderUrl = FirebaseManager.TOP_1_BORDER_URL,
            frameWidth = 126.dp,
            frameHeight = 142.dp,
            avatarSize = 56.dp,
            avatarTopOffset = 12.dp,
            accentColor = Color(0xFFF59E0B),
            isCurrentUser = top1?.uid != null && top1.uid == currentUid,
            modifier = Modifier.weight(1.15f)
        )

        // TOP 3 (Kanan)
        EditorialPodiumSpot(
            rank = 3,
            user = top3,
            borderUrl = FirebaseManager.TOP_3_BORDER_URL,
            frameWidth = 110.dp,
            frameHeight = 130.dp,
            avatarSize = 50.dp,
            avatarTopOffset = 16.dp,
            accentColor = Color(0xFFFB923C),
            isCurrentUser = top3?.uid != null && top3.uid == currentUid,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditorialPodiumSpot(
    rank: Int,
    user: LeaderboardUser?,
    borderUrl: String,
    frameWidth: Dp,
    frameHeight: Dp,
    avatarSize: Dp,
    avatarTopOffset: Dp,
    accentColor: Color,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Frame Avatar Konsentris (BEBAS KOTAK PEMBUNGKUS PODIUM!)
        Box(
            modifier = Modifier
                .size(width = frameWidth, height = frameHeight),
            contentAlignment = Alignment.TopCenter
        ) {
            // Layer Belakang (zIndex = 0f): Foto Profil Bulat Pengguna pas di lubang atas
            Box(
                modifier = Modifier
                    .zIndex(0f)
                    .offset(y = avatarTopOffset)
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
                        fontSize = if (rank == 1) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            // Layer Depan (zIndex = 1f): Gambar Bingkai PNG Transparan (Sayap & Pita Membingkai Sempurna)
            AsyncImage(
                model = borderUrl,
                contentDescription = "Border Podium Top $rank",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .zIndex(1f)
                    .size(width = frameWidth, height = frameHeight)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (user != null) {
            // Nama Pengguna (Outfit Bold)
            Text(
                text = user.name,
                color = Color.White,
                fontSize = if (rank == 1) 14.sp else 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitDisplayFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Baris Tag Level & #ID
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                SleekHexChevronLevelTag(level = user.level)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "#${user.userTag.removePrefix("@")}",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            // Custom Title Badge (Jika Ada)
            if (user.equippedTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                GeometricTitleBadge(
                    titleName = user.equippedTitle,
                    tierLevel = user.equippedTitleTier,
                    styleVariant = user.equippedTitleVariant,
                    compact = true,
                    scaleFactor = if (rank == 1) 0.82f else 0.76f
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Total EXP (JetBrains Mono - Anti Terpotong!)
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = accentColor,
                fontSize = if (rank == 1) 13.5.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kosong",
                    color = Color(0xFF475569),
                    fontSize = 11.5.sp,
                    fontFamily = JakartaSansFontFamily
                )
            }
        }
    }
}

/**
 * Baris Peringkat 4 ke Bawah (Sleek Minimalis Editorial)
 */
@Composable
private fun ModernLeaderboardRowItem(
    user: LeaderboardUser,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentUser) Color(0xFF1F1218) else Color(0xFF11131B))
            .border(
                width = if (isCurrentUser) 1.dp else 0.5.dp,
                color = if (isCurrentUser) Color(0xFFE11D48) else Color(0xFF1B1E2B),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kolom 1: Nomor Peringkat (#4, #5, ...)
        Text(
            text = "#${user.rank}",
            color = if (isCurrentUser) Color(0xFFFF5722) else Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Kolom 2: Avatar + Border Kosmetik
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily
                    )
                }
            }

            if (user.equippedBorderUrl.isNotBlank()) {
                AsyncImage(
                    model = user.equippedBorderUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Kolom 3: Nama Pengguna + Title/Rank
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OutfitDisplayFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "#${user.userTag.removePrefix("@")}",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SleekHexChevronLevelTag(level = user.level)

                if (user.equippedTitle.isNotBlank()) {
                    GeometricTitleBadge(
                        titleName = user.equippedTitle,
                        tierLevel = user.equippedTitleTier,
                        styleVariant = user.equippedTitleVariant,
                        compact = true,
                        scaleFactor = 0.78f
                    )
                } else {
                    Text(
                        text = user.rankTitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = JakartaSansFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Kolom 4: Total EXP
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${FirebaseManager.formatNumber(user.exp)} EXP",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "✦ ${FirebaseManager.formatNumber(user.points)}",
                color = Color(0xFF64748B),
                fontSize = 10.5.sp,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Floating Bar Bawah "Posisi Peringkat Kamu"
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF012141D))
            .border(1.dp, Color(0x55E11D48), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Posisi Peringkat
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rankLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            // Foto Profil
            Box(
                modifier = Modifier
                    .size(38.dp)
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "#$userTag",
                        color = Color(0xFF64748B),
                        fontSize = 10.5.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Text(
                    text = "Posisi Peringkat Anda • $rankTitle",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = JakartaSansFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${FirebaseManager.formatNumber(exp)} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = JetBrainsMonoFontFamily
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
                    fontFamily = OutfitDisplayFontFamily
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
                    fontFamily = JakartaSansFontFamily
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
                fontFamily = OutfitDisplayFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = JakartaSansFontFamily
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = xp,
            color = Color(0xFFFF5722),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily
        )
    }
}
