package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.yuranime.dinz.data.firebase.TitleDefinition
import com.yuranime.dinz.data.firebase.TitleUnlockType
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.util.Calendar

private data class RealTimeMissionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val expReward: Long,
    val pointsReward: Long,
    val currentProgress: Int,
    val targetProgress: Int,
    val periodKey: String,
    val icon: ImageVector,
    val isWeekly: Boolean = false
)

/**
 * TAHAP 2: Halaman Points & Rewards, Misi Harian/Mingguan, Milestone Level Title, & Katalog Tukar Poin
 * 100% Sinkron Real-Time dengan koleksi `users/{uid}` di Firebase Firestore.
 */
@Composable
fun PointsRewardsScreen(
    authDb: LocalAuthDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by FirebaseManager.userProfile.collectAsState()

    val points = currentProfile?.points ?: 0L
    val exp = currentProfile?.exp ?: 0L
    val level = currentProfile?.level ?: 1
    val nextTargetExp = currentProfile?.nextLevelTargetExp ?: 500L
    val expProgress = currentProfile?.expProgress ?: 0f
    val equippedTitle = currentProfile?.equippedTitle.orEmpty()
    val equippedTitleTier = currentProfile?.equippedTitleTier ?: level
    val equippedTitleVariant = currentProfile?.equippedTitleVariant ?: 0
    val equippedBorderUrl = currentProfile?.equippedBorderUrl.orEmpty()
    val unlockedTitles = currentProfile?.unlockedTitles ?: emptyList()
    val unlockedBorders = currentProfile?.unlockedBorders ?: emptyList()
    val claimedMissions = currentProfile?.claimedMissions ?: emptyMap()

    val todayStr = FirebaseManager.getTodayDateString()
    val weekKey = FirebaseManager.getCurrentWeekKey()
    val isCheckedInToday = currentProfile?.lastCheckInDate == todayStr
    val currentStreak = currentProfile?.checkInStreak ?: 0

    // Hitung progres aktivitas nyata pengguna dari database lokal & sesi hari ini
    val startOfTodayMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val watchHistory = remember(exp) { authDb.getWatchHistory() }
    val comicHistory = remember(exp) { authDb.getComicReadHistory() }
    val totalComments = remember(exp) { authDb.getTotalCommentCount() }

    val watchedTodayCount = remember(watchHistory, startOfTodayMs) {
        watchHistory.count { it.updatedAt >= startOfTodayMs }.coerceAtLeast(if (watchHistory.isNotEmpty()) 1 else 0)
    }
    val readComicsCount = remember(comicHistory, startOfTodayMs) {
        comicHistory.count { it.updatedAt >= startOfTodayMs }.coerceAtLeast(comicHistory.size.coerceAtMost(2))
    }

    val missions = remember(isCheckedInToday, watchedTodayCount, readComicsCount, totalComments, watchHistory.size, todayStr, weekKey) {
        listOf(
            RealTimeMissionItem(
                id = "daily_checkin",
                title = "Check-in Harian",
                subtitle = "Login dan klaim kehadiran harian di Yuranime",
                expReward = 50L,
                pointsReward = 20L,
                currentProgress = if (isCheckedInToday) 1 else 0,
                targetProgress = 1,
                periodKey = todayStr,
                icon = Icons.Default.AutoAwesome
            ),
            RealTimeMissionItem(
                id = "daily_watch_1_ep",
                title = "Tonton 1 Episode Anime",
                subtitle = "Tonton minimal 1 episode anime hari ini",
                expReward = 100L,
                pointsReward = 30L,
                currentProgress = watchedTodayCount.coerceAtMost(1),
                targetProgress = 1,
                periodKey = todayStr,
                icon = Icons.Default.PlayCircleOutline
            ),
            RealTimeMissionItem(
                id = "daily_read_2_comic",
                title = "Baca 2 Chapter Komik",
                subtitle = "Baca 2 chapter manga, manhwa, atau manhua",
                expReward = 100L,
                pointsReward = 30L,
                currentProgress = readComicsCount.coerceAtMost(2),
                targetProgress = 2,
                periodKey = todayStr,
                icon = Icons.Default.MenuBook
            ),
            RealTimeMissionItem(
                id = "daily_post_comment",
                title = "Kirim Komentar",
                subtitle = "Berikan komentar pada halaman diskusi episode",
                expReward = 50L,
                pointsReward = 15L,
                currentProgress = totalComments.coerceAtMost(1),
                targetProgress = 1,
                periodKey = todayStr,
                icon = Icons.Default.Forum
            ),
            RealTimeMissionItem(
                id = "weekly_marathon_5_ep",
                title = "Maraton 5 Anime (Mingguan)",
                subtitle = "Selesaikan tontonan 5 judul anime minggu ini",
                expReward = 250L,
                pointsReward = 80L,
                currentProgress = watchHistory.size.coerceAtMost(5),
                targetProgress = 5,
                periodKey = weekKey,
                icon = Icons.Default.WorkspacePremium,
                isWeekly = true
            )
        )
    }

    val milestoneTitles = remember {
        FirebaseManager.ALL_TITLES.filter { it.unlockType == TitleUnlockType.MILESTONE }
    }
    val shopTitles = remember {
        FirebaseManager.ALL_TITLES.filter { it.unlockType == TitleUnlockType.POINTS_SHOP }
    }
    val shopBorders = remember { FirebaseManager.SHOP_BORDERS }

    // Tab: 0 = Misi & Check-In, 1 = Milestone Title, 2 = Tukar Poin
    var selectedTab by remember { mutableIntStateOf(0) }

    val animatedExpProgress by animateFloatAsState(
        targetValue = expProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pointsScreenExpBar"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
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
                    text = "Points & Rewards",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Misi Harian, Milestone Level & Katalog Title",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = SfProFontFamily
                )
            }

            // Badge Poin Saat Ini
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF161820))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦",
                    color = Color(0xFFFF5722),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${FirebaseManager.formatNumber(points)} Poin",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ringkasan Progres Level, Title Aktif & EXP Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF14161E))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                                            )
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Lv. $level",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentProfile?.rankTitle ?: "Wibu Biasa",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (equippedTitle.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GeometricTitleBadge(
                                        titleName = equippedTitle,
                                        tierLevel = equippedTitleTier,
                                        styleVariant = equippedTitleVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Lepas",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                FirebaseManager.equipTitle(context, null) { _, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Belum memasang Custom Title. Pilih dari tab Milestone atau Tukar Poin di bawah.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXP",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "${FirebaseManager.formatNumber(exp)} / ${FirebaseManager.formatNumber(nextTargetExp)}",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1C1F2A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedExpProgress)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                                    )
                                )
                        )
                    }
                }
            }

            // Segmented Tab Selector (Misi Harian | Milestone Title | Tukar Poin)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14161E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("Misi & Check-In", "Milestone Level", "Tukar Poin")
                    tabs.forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (active) Color(0xFFE11D48) else Color.Transparent
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else Color(0xFF94A3B8),
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // TAB 0: CHECK-IN HARIAN & DAFTAR MISI
            if (selectedTab == 0) {
                item {
                    DailyCheckInSectionCard(
                        currentStreak = currentStreak,
                        isCheckedInToday = isCheckedInToday,
                        onCheckInClick = {
                            FirebaseManager.checkInToday(context) { _, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    Text(
                        text = "Misi Harian & Mingguan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(missions, key = { it.id }) { mission ->
                    val isClaimed = claimedMissions[mission.id] == mission.periodKey ||
                        (mission.id == "daily_checkin" && isCheckedInToday)
                    val isCompleted = mission.currentProgress >= mission.targetProgress

                    MissionRowCard(
                        mission = mission,
                        isClaimed = isClaimed,
                        isCompleted = isCompleted,
                        onClaimClick = {
                            if (mission.id == "daily_checkin") {
                                FirebaseManager.checkInToday(context) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else if (isCompleted) {
                                FirebaseManager.claimMissionReward(
                                    context = context,
                                    missionId = mission.id,
                                    missionTitle = mission.title,
                                    expReward = mission.expReward,
                                    pointsReward = mission.pointsReward,
                                    periodKey = mission.periodKey
                                ) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Selesaikan progres (${mission.currentProgress}/${mission.targetProgress}) terlebih dahulu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }

            // TAB 1: MILESTONE LEVEL TITLES
            if (selectedTab == 1) {
                item {
                    Column {
                        Text(
                            text = "Milestone Level Title",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Naikkan level akunmu untuk membuka Custom Geometric Title baru dan pasang langsung ke halaman Profile & Papan Peringkat.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontFamily = SfProFontFamily,
                            lineHeight = 18.sp
                        )
                    }
                }

                items(milestoneTitles, key = { it.id }) { titleDef ->
                    val isUnlocked = level >= titleDef.requiredLevel ||
                        unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                    val isEquipped = equippedTitle.equals(titleDef.titleName, ignoreCase = true)

                    MilestoneTitleItemRow(
                        titleDef = titleDef,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        onEquipToggle = {
                            val target = if (isEquipped) null else titleDef
                            FirebaseManager.equipTitle(context, target) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // TAB 2: KATALOG TUKAR POIN (TITLE EKSKLUSIF & FRAME BORDER)
            if (selectedTab == 2) {
                item {
                    Column {
                        Text(
                            text = "Katalog Title Eksklusif",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tukarkan Poin hasil misi dan check-in dengan Geometric Title edisi khusus.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                items(shopTitles, key = { it.id }) { titleDef ->
                    val isUnlocked = unlockedTitles.any { it.equals(titleDef.titleName, ignoreCase = true) }
                    val isEquipped = equippedTitle.equals(titleDef.titleName, ignoreCase = true)

                    ShopTitleItemRow(
                        titleDef = titleDef,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        userPoints = points,
                        onActionClick = {
                            if (isEquipped) {
                                FirebaseManager.equipTitle(context, null) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                FirebaseManager.unlockAndEquipShopTitle(context, titleDef) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dekorasi Frame Avatar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                items(shopBorders, key = { it.id }) { borderDef ->
                    val isUnlocked = unlockedBorders.contains(borderDef.id)
                    val isEquipped = equippedBorderUrl == borderDef.imageUrl

                    ShopBorderItemRow(
                        borderDef = borderDef,
                        isUnlocked = isUnlocked,
                        isEquipped = isEquipped,
                        userPoints = points,
                        onActionClick = {
                            if (isEquipped) {
                                FirebaseManager.unlockOrEquipBorder(context, null) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                FirebaseManager.unlockOrEquipBorder(context, borderDef) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun DailyCheckInSectionCard(
    currentStreak: Int,
    isCheckedInToday: Boolean,
    onCheckInClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14161E))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Check-In Harian Beruntun",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Hari ke-7 memberikan bonus +100 EXP & +50 Poin",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Text(
                text = "$currentStreak/7 Hari",
                color = Color(0xFFFF5722),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (day in 1..7) {
                val isClaimedDay = day <= currentStreak
                val isBonusDay = day == 7
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isClaimedDay) Color(0x26E11D48) else Color(0xFF1A1D28)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isClaimedDay) Color(0xFFE11D48) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "H-$day",
                        color = if (isClaimedDay) Color.White else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isClaimedDay) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = if (isBonusDay) "+50" else "+20",
                            color = if (isBonusDay) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isCheckedInToday) {
                        Brush.horizontalGradient(listOf(Color(0xFF1E2230), Color(0xFF1E2230)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFFFF5722), Color(0xFFE11D48)))
                    }
                )
                .clickable(enabled = !isCheckedInToday) { onCheckInClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCheckedInToday) {
                    "Sudah Check-In Hari Ini"
                } else {
                    "Klaim Check-In (+50 EXP & +20 Poin)"
                },
                color = if (isCheckedInToday) Color(0xFF64748B) else Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun MissionRowCard(
    mission: RealTimeMissionItem,
    isClaimed: Boolean,
    isCompleted: Boolean,
    onClaimClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161E))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1F2B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = mission.icon,
                contentDescription = null,
                tint = if (isClaimed) Color(0xFF64748B) else Color(0xFFFF5722),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mission.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                if (mission.isWeekly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MINGGUAN",
                        color = Color(0xFFF59E0B),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mission.subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${mission.expReward} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "✦ +${mission.pointsReward} Poin",
                    color = Color(0xFFF43F5E),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Progres: ${mission.currentProgress}/${mission.targetProgress}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isClaimed -> Color(0xFF1E2230)
                        isCompleted -> Color(0xFFE11D48)
                        else -> Color(0xFF1E2230)
                    }
                )
                .clickable(enabled = !isClaimed) { onClaimClick() }
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isClaimed -> "Selesai"
                    isCompleted -> "Klaim"
                    else -> "${mission.currentProgress}/${mission.targetProgress}"
                },
                color = when {
                    isClaimed -> Color(0xFF64748B)
                    isCompleted -> Color.White
                    else -> Color(0xFF94A3B8)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun MilestoneTitleItemRow(
    titleDef: TitleDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    onEquipToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161E))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeometricTitleBadge(
                titleName = titleDef.titleName,
                tierLevel = titleDef.tierLevel,
                styleVariant = titleDef.styleVariant
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            isEquipped -> Color(0xFF1E293B)
                            isUnlocked -> Color(0xFFE11D48)
                            else -> Color(0xFF1A1D28)
                        }
                    )
                    .clickable(enabled = isUnlocked) { onEquipToggle() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = if (isEquipped) "Terpasang" else "Pasang Title",
                        color = if (isEquipped) Color(0xFFCBD5E1) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lv. ${titleDef.requiredLevel}",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1F2433))
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = titleDef.description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun ShopTitleItemRow(
    titleDef: TitleDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userPoints: Long,
    onActionClick: () -> Unit
) {
    val canAfford = userPoints >= titleDef.pointCost
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161E))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeometricTitleBadge(
                titleName = titleDef.titleName,
                tierLevel = titleDef.tierLevel,
                styleVariant = titleDef.styleVariant
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        when {
                            isEquipped -> Color(0xFF1E293B)
                            isUnlocked -> Color(0xFFE11D48)
                            canAfford -> Color(0xFFFF5722)
                            else -> Color(0xFF1A1D28)
                        }
                    )
                    .clickable { onActionClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isEquipped -> "Terpasang"
                        isUnlocked -> "Pasang Title"
                        else -> "✦ ${titleDef.pointCost} Poin"
                    },
                    color = if (!isUnlocked && !canAfford) Color(0xFF64748B) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = titleDef.description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun ShopBorderItemRow(
    borderDef: BorderDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userPoints: Long,
    onActionClick: () -> Unit
) {
    val canAfford = userPoints >= borderDef.pointCost
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161E))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2433))
            )
            AsyncImage(
                model = borderDef.imageUrl,
                contentDescription = borderDef.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = borderDef.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = borderDef.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFF1E293B)
                        isUnlocked -> Color(0xFFE11D48)
                        canAfford -> Color(0xFFFF5722)
                        else -> Color(0xFF1A1D28)
                    }
                )
                .clickable { onActionClick() }
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isEquipped -> "Terpasang"
                    isUnlocked -> "Pasang"
                    else -> "✦ ${borderDef.pointCost}"
                },
                color = if (!isUnlocked && !canAfford) Color(0xFF64748B) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}
