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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yuranime.dinz.data.firebase.BorderDefinition
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import com.yuranime.dinz.data.firebase.TitleDefinition
import com.yuranime.dinz.data.firebase.TitleUnlockType
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.SleekHexChevronLevelTag
import com.yuranime.dinz.ui.theme.JakartaSansFontFamily
import com.yuranime.dinz.ui.theme.JetBrainsMonoFontFamily
import com.yuranime.dinz.ui.theme.OutfitDisplayFontFamily
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

private data class PublicEventCode(
    val code: String,
    val title: String,
    val rewards: String,
    val exp: Long,
    val points: Long
)

/**
 * Halaman Points & Rewards, Misi Harian/Mingguan, Milestone Level Title, Katalog Tukar Poin & Kode Redeem
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
    val equippedProfileEffectUrl = currentProfile?.equippedProfileEffectUrl.orEmpty()
    val cosmeticAnimate = currentProfile?.cosmeticAnimate ?: true
    val unlockedTitles = currentProfile?.unlockedTitles ?: emptyList()
    val unlockedBorders = currentProfile?.unlockedBorders ?: emptyList()
    val unlockedEffects = currentProfile?.unlockedEffects ?: emptyList()
    val claimedRedeemCodes = currentProfile?.claimedRedeemCodes ?: emptyList()
    val claimedMissions = currentProfile?.claimedMissions ?: emptyMap()

    val todayStr = FirebaseManager.getTodayDateString()
    val weekKey = FirebaseManager.getCurrentWeekKey()
    val isCheckedInToday = currentProfile?.lastCheckInDate == todayStr
    val currentStreak = currentProfile?.checkInStreak ?: 0

    // Hitung progres aktivitas nyata pengguna
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
    val shopProfileEffects = remember { FirebaseManager.SHOP_PROFILE_EFFECTS }

    // 4 Segmented Tabs: 0 = Misi & Check-In, 1 = Milestone, 2 = Shop & Kosmetik, 3 = Kode Redeem
    var selectedTab by remember { mutableIntStateOf(0) }
    // Sub-tab di dalam Shop & Kosmetik: 0 = Titles, 1 = Avatar Borders, 2 = Profile Effects
    var cosmeticSubTab by remember { mutableIntStateOf(0) }

    // Kode Redeem input state
    var redeemCodeInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitDisplayFontFamily
                )
                Text(
                    text = "Misi Harian, Kosmetik Discord & Kode Redeem",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = JakartaSansFontFamily
                )
            }

            // Badge Poin Saat Ini (JetBrains Mono)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF14161F))
                    .border(0.5.dp, Color(0xFF232738), RoundedCornerShape(50))
                .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${FirebaseManager.formatNumber(points)} Poin",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ringkasan Progres Level & Title Aktif
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF11131B))
                        .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SleekHexChevronLevelTag(level = level)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentProfile?.rankTitle ?: "Wibu Biasa",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = JakartaSansFontFamily
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
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = JakartaSansFontFamily,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                FirebaseManager.equipTitle(context, null) { _, msg ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Belum memasang Custom Title. Pilih di tab Milestone atau Shop.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    fontFamily = JakartaSansFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXP Progres",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = JakartaSansFontFamily
                        )
                        Text(
                            text = "${FirebaseManager.formatNumber(exp)} / ${FirebaseManager.formatNumber(nextTargetExp)}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1E2230))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedExpProgress.coerceAtLeast(0.02f))
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

            // 4 Segmented Pill Tabs (Bukan blok merah kaku!)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF11131B))
                        .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val tabs = listOf("Misi & Check-In", "Milestone", "Shop & Kosmetik", "Kode Redeem")
                    tabs.forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (active) Color(0xFF1C202C) else Color.Transparent
                                )
                                .border(
                                    width = if (active) 0.75.dp else 0.dp,
                                    color = if (active) Color(0xFF334155) else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = OutfitDisplayFontFamily,
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily,
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
                                    "Selesaikan misi (${mission.currentProgress}/${mission.targetProgress}) terlebih dahulu",
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitDisplayFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Capai level target untuk membuka Custom Title gratis dan pasang langsung ke profil Anda.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = JakartaSansFontFamily
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
                        currentLevel = level,
                        onEquipToggle = {
                            val target = if (isEquipped) null else titleDef
                            FirebaseManager.equipTitle(context, target) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // TAB 2: SHOP & KOSMETIK (DISCORD ASSETS INTEGRATION)
            if (selectedTab == 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Toggle Animasi Kosmetik APNG
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF11131B))
                                .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Animasi Kosmetik (APNG)",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = JakartaSansFontFamily
                                )
                            }
                            Switch(
                                checked = cosmeticAnimate,
                                onCheckedChange = { checked ->
                                    FirebaseManager.setCosmeticAnimate(context, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFE11D48)
                                )
                            )
                        }

                        // Sub-Tab Filter: Title | Avatar Border | Profile Effect
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF11131B))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val subTabs = listOf("Custom Title", "Avatar Border", "Profile Effect")
                            subTabs.forEachIndexed { sIdx, sLabel ->
                                val sActive = cosmeticSubTab == sIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sActive) Color(0xFF1F2433) else Color.Transparent)
                                        .clickable { cosmeticSubTab = sIdx }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sLabel,
                                        color = if (sActive) Color.White else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp,
                                        fontWeight = if (sActive) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = OutfitDisplayFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub-Tab 0: Custom Titles
                if (cosmeticSubTab == 0) {
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
                }

                // Sub-Tab 1: Avatar Borders (Discord Catalog)
                if (cosmeticSubTab == 1) {
                    items(shopBorders, key = { it.id }) { borderDef ->
                        val isUnlocked = unlockedBorders.contains(borderDef.id)
                        val isEquipped = equippedBorderUrl == borderDef.imageUrl

                        ShopBorderItemRow(
                            borderDef = borderDef,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            userPoints = points,
                            userLevel = level,
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

                // Sub-Tab 2: Profile Effects (Discord Catalog)
                if (cosmeticSubTab == 2) {
                    items(shopProfileEffects, key = { it.id }) { effectDef ->
                        val isUnlocked = unlockedEffects.contains(effectDef.id)
                        val isEquipped = equippedProfileEffectUrl == effectDef.animationUrl

                        ShopProfileEffectItemRow(
                            effectDef = effectDef,
                            isUnlocked = isUnlocked,
                            isEquipped = isEquipped,
                            userPoints = points,
                            userLevel = level,
                            onActionClick = {
                                if (isEquipped) {
                                    FirebaseManager.unlockOrEquipProfileEffect(context, null) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    FirebaseManager.unlockOrEquipProfileEffect(context, effectDef) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // TAB 3: KODE REDEEM REAL-TIME
            if (selectedTab == 3) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF11131B))
                            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Klaim Kode Redeem",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitDisplayFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Masukkan kode event komunitas untuk membuka ribuan EXP, Poin, Title langka & Kosmetik Limited.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = JakartaSansFontFamily
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = redeemCodeInput,
                                onValueChange = { redeemCodeInput = it.uppercase() },
                                placeholder = {
                                    Text(
                                        text = "Ketik kode (misal: DINZMYTHIC)",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (redeemCodeInput.isNotBlank() && !isRedeeming) {
                                            isRedeeming = true
                                            FirebaseManager.claimRedeemCode(context, redeemCodeInput) { ok, msg ->
                                                isRedeeming = false
                                                if (ok) redeemCodeInput = ""
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE11D48),
                                    unfocusedBorderColor = Color(0xFF232838),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFE11D48)
                                ),
                                shape = RoundedCornerShape(11.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFF5722), Color(0xFFE11D48))
                                        )
                                    )
                                    .clickable(enabled = redeemCodeInput.isNotBlank() && !isRedeeming) {
                                        isRedeeming = true
                                        FirebaseManager.claimRedeemCode(context, redeemCodeInput) { ok, msg ->
                                            isRedeeming = false
                                            if (ok) redeemCodeInput = ""
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRedeeming) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "KLAIM",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = OutfitDisplayFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Kode Event Terverifikasi",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val publicCodes = listOf(
                    PublicEventCode("DINZMYTHIC", "Paket Mythic Sovereign", "+25,000 EXP • +15,000 Poin • Mythic Emperor Border • Mythic Aurora", 25000L, 15000L),
                    PublicEventCode("YURANIMEVIP", "Paket VIP Yuranime Emas", "+10,000 EXP • +5,000 Poin • Sovereign Gold Border • Magic Hearts", 10000L, 5000L),
                    PublicEventCode("VENOMSYMBIOTE", "Edisi Venom Symbiote", "+12,000 EXP • +6,000 Poin • Avatar Border Venom • Profile Effect Venom", 12000L, 6000L),
                    PublicEventCode("SEPUH2026", "Gelar Sepuh Komunitas", "+5,000 EXP • +2,500 Poin • Title SEPUH YURANIME • Bronze Frame", 5000L, 2500L),
                    PublicEventCode("SOVEREIGN99", "Sayap Perak Sovereign", "+15,000 EXP • +8,000 Poin • Title KHALIFAH ANIME • Silver Wings", 15000L, 8000L),
                    PublicEventCode("ANIMEKING", "Bangsawan Komik", "+8,000 EXP • +4,000 Poin • Title BANGSAWAN KOMIK • Cosmic Twilight", 8000L, 4000L)
                )

                items(publicCodes, key = { it.code }) { itemCode ->
                    val isClaimed = claimedRedeemCodes.contains(itemCode.code)

                    PublicCodeCard(
                        item = itemCode,
                        isClaimed = isClaimed,
                        onUseCode = {
                            redeemCodeInput = itemCode.code
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Komponen Streak Check-In Harian
 */
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
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Streak Check-In Harian",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily
                    )
                    Text(
                        text = "Hari ke-7 bonus +100 EXP & +50 Poin",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = JakartaSansFontFamily
                    )
                }
            }

            Text(
                text = "$currentStreak/7 Hari",
                color = Color(0xFFFF5722),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            for (day in 1..7) {
                val isClaimedDay = day <= currentStreak
                val isBonusDay = day == 7
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isClaimedDay) Color(0x22E11D48) else Color(0xFF141722)
                        )
                        .border(
                            width = if (isClaimedDay) 1.dp else 0.5.dp,
                            color = if (isClaimedDay) Color(0xFFE11D48) else Color(0xFF1F2433),
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
                        fontFamily = JakartaSansFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isClaimedDay) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        val iconVector = when {
                            day == 7 -> Icons.Default.EmojiEvents
                            day >= 5 -> Icons.Default.Diamond
                            else -> Icons.Default.AutoAwesome
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (isBonusDay) Color(0xFFF59E0B) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBonusDay) "+50" else "+20",
                        color = if (isBonusDay) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isCheckedInToday) {
                        Brush.horizontalGradient(listOf(Color(0xFF1A1D28), Color(0xFF1A1D28)))
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitDisplayFontFamily
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
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1D2B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = mission.icon,
                contentDescription = null,
                tint = if (isClaimed) Color(0xFF64748B) else Color(0xFFFF5722),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mission.title,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OutfitDisplayFontFamily
                )
                if (mission.isWeekly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MINGGUAN",
                        color = Color(0xFFF59E0B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mission.subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = JakartaSansFontFamily
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${mission.expReward} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✦ +${mission.pointsReward}",
                    color = Color(0xFFF43F5E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${mission.currentProgress}/${mission.targetProgress})",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isClaimed -> Color(0xFF191C26)
                        isCompleted -> Color(0xFFE11D48)
                        else -> Color(0xFF191C26)
                    }
                )
                .clickable(enabled = !isClaimed) { onClaimClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
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
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitDisplayFontFamily
            )
        }
    }
}

@Composable
private fun MilestoneTitleItemRow(
    titleDef: TitleDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    currentLevel: Int,
    onEquipToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(14.dp))
            .padding(14.dp)
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
                            else -> Color(0xFF181B26)
                        }
                    )
                    .clickable(enabled = isUnlocked) { onEquipToggle() }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = if (isEquipped) "Terpasang" else "Pasang",
                        color = if (isEquipped) Color(0xFFCBD5E1) else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitDisplayFontFamily
                    )
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
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = titleDef.description,
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontFamily = JakartaSansFontFamily
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
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(14.dp))
            .padding(14.dp)
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
                            else -> Color(0xFF181B26)
                        }
                    )
                    .clickable { onActionClick() }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isEquipped -> "Terpasang"
                        isUnlocked -> "Pasang"
                        else -> "✦ ${titleDef.pointCost}"
                    },
                    color = if (!isUnlocked && !canAfford) Color(0xFF64748B) else Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = titleDef.description,
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontFamily = JakartaSansFontFamily
        )
    }
}

@Composable
private fun ShopBorderItemRow(
    borderDef: BorderDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userPoints: Long,
    userLevel: Int,
    onActionClick: () -> Unit
) {
    val isLevelLocked = borderDef.tier == "B" && userLevel < borderDef.requiredLevel
    val isRedeemOnly = borderDef.tier == "C"
    val canAfford = userPoints >= borderDef.pointCost && !isLevelLocked && !isRedeemOnly

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview Frame
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1D2B))
            )
            AsyncImage(
                model = borderDef.imageUrl,
                contentDescription = borderDef.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = borderDef.name,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OutfitDisplayFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Tag Tier
                val tierLabel = when (borderDef.tier) {
                    "B" -> "Lv.${borderDef.requiredLevel}+"
                    "C" -> "REDEEM ONLY"
                    else -> "STARTER"
                }
                val tierColor = when (borderDef.tier) {
                    "B" -> Color(0xFFF59E0B)
                    "C" -> Color(0xFFE11D48)
                    else -> Color(0xFF38BDF8)
                }
                Text(
                    text = tierLabel,
                    color = tierColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = borderDef.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = JakartaSansFontFamily
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
                        else -> Color(0xFF181B26)
                    }
                )
                .clickable(enabled = isUnlocked || canAfford) { onActionClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isEquipped -> "Terpasang"
                    isUnlocked -> "Pasang"
                    isRedeemOnly -> "KODE REDEEM"
                    isLevelLocked -> "Lv. ${borderDef.requiredLevel}"
                    else -> "✦ ${borderDef.pointCost}"
                },
                color = if (!isUnlocked && !canAfford) Color(0xFF64748B) else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
private fun ShopProfileEffectItemRow(
    effectDef: ProfileEffectDefinition,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    userPoints: Long,
    userLevel: Int,
    onActionClick: () -> Unit
) {
    val isLevelLocked = effectDef.tier == "B" && userLevel < effectDef.requiredLevel
    val isRedeemOnly = effectDef.tier == "C"
    val canAfford = userPoints >= effectDef.pointCost && !isLevelLocked && !isRedeemOnly

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF11131B))
            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Animasi
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181A25)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = effectDef.animationUrl,
                contentDescription = effectDef.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = effectDef.name,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OutfitDisplayFontFamily
                )
                Spacer(modifier = Modifier.width(6.dp))
                val tierLabel = when (effectDef.tier) {
                    "B" -> "Lv.${effectDef.requiredLevel}+"
                    "C" -> "REDEEM ONLY"
                    else -> "STARTER"
                }
                val tierColor = when (effectDef.tier) {
                    "B" -> Color(0xFFF59E0B)
                    "C" -> Color(0xFFE11D48)
                    else -> Color(0xFF38BDF8)
                }
                Text(
                    text = tierLabel,
                    color = tierColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = effectDef.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = JakartaSansFontFamily
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
                        else -> Color(0xFF181B26)
                    }
                )
                .clickable(enabled = isUnlocked || canAfford) { onActionClick() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isEquipped -> "Terpasang"
                    isUnlocked -> "Pasang"
                    isRedeemOnly -> "KODE REDEEM"
                    isLevelLocked -> "Lv. ${effectDef.requiredLevel}"
                    else -> "✦ ${effectDef.pointCost}"
                },
                color = if (!isUnlocked && !canAfford) Color(0xFF64748B) else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
    }
}

@Composable
private fun PublicCodeCard(
    item: PublicEventCode,
    isClaimed: Boolean,
    onUseCode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF11131B))
            .border(
                width = if (isClaimed) 0.5.dp else 1.dp,
                color = if (isClaimed) Color(0xFF1F2433) else Color(0x55E11D48),
                shape = RoundedCornerShape(13.dp)
            )
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.code,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.title,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    fontFamily = JakartaSansFontFamily
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.rewards,
                color = Color(0xFFFF5722),
                fontSize = 11.sp,
                fontFamily = JakartaSansFontFamily
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isClaimed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF151824))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DIKLAIM",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F2435))
                    .clickable(onClick = onUseCode)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GUNAKAN",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }
    }
}
