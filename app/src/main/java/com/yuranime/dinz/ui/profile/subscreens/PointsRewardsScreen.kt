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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
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
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import com.yuranime.dinz.ui.components.AnimatedCosmeticImage
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.SleekHexChevronLevelTag
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

    val level = currentProfile?.level ?: 1
    val points = currentProfile?.points ?: 0L
    val exp = currentProfile?.exp ?: 0L
    val nextTargetExp = currentProfile?.nextLevelTargetExp ?: FirebaseManager.computeNextLevelTargetExp(level)
    val expProgress = currentProfile?.expProgress ?: FirebaseManager.computeExpProgress(exp, nextTargetExp)
    val currentStreak = currentProfile?.checkInStreak ?: 0
    val lastCheckInDate = currentProfile?.lastCheckInDate.orEmpty()
    val todayDate = remember { FirebaseManager.getTodayDateString() }
    val isCheckedInToday = lastCheckInDate == todayDate

    val unlockedTitles = currentProfile?.unlockedTitles ?: listOf("WARGA YURANIME")
    val equippedTitle = currentProfile?.equippedTitle.orEmpty()
    val equippedTitleTier = currentProfile?.equippedTitleTier ?: level
    val equippedTitleVariant = currentProfile?.equippedTitleVariant ?: 0

    val unlockedBorders = currentProfile?.unlockedBorders ?: emptyList()
    val equippedBorderUrl = currentProfile?.equippedBorderUrl.orEmpty()
    val unlockedEffects = currentProfile?.unlockedEffects ?: emptyList()
    val equippedProfileEffectUrl = currentProfile?.equippedProfileEffectUrl.orEmpty()
    val cosmeticAnimate = currentProfile?.cosmeticAnimate ?: true
    val claimedRedeemCodes = currentProfile?.claimedRedeemCodes ?: emptyList()
    val claimedMissions = currentProfile?.claimedMissions ?: emptyMap()
    val userAvatarUrl = currentProfile?.avatar ?: authDb.getAvatarUrl()

    val weekKey = remember { FirebaseManager.getCurrentWeekKey() }
    val watchHistory = remember { authDb.getWatchHistory() }

    val missions = remember(watchHistory.size, isCheckedInToday, todayDate, weekKey) {
        listOf(
            RealTimeMissionItem(
                id = "daily_checkin",
                title = "Check-In Harian",
                subtitle = "Buka aplikasi dan klaim hadiah harian Anda",
                expReward = 50L,
                pointsReward = 20L,
                currentProgress = if (isCheckedInToday) 1 else 0,
                targetProgress = 1,
                periodKey = todayDate,
                icon = Icons.Outlined.CheckCircle
            ),
            RealTimeMissionItem(
                id = "daily_watch_1",
                title = "Tonton 1 Episode Anime",
                subtitle = "Selesaikan tontonan 1 episode anime apa pun",
                expReward = 75L,
                pointsReward = 30L,
                currentProgress = watchHistory.size.coerceAtMost(1),
                targetProgress = 1,
                periodKey = todayDate,
                icon = Icons.Default.PlayCircleOutline
            ),
            RealTimeMissionItem(
                id = "daily_read_comic_1",
                title = "Baca 1 Chapter Komik",
                subtitle = "Baca manga/manhwa seru di Yuranime Komik",
                expReward = 60L,
                pointsReward = 25L,
                currentProgress = 1,
                targetProgress = 1,
                periodKey = todayDate,
                icon = Icons.Default.MenuBook
            ),
            RealTimeMissionItem(
                id = "daily_comment_1",
                title = "Tulis Komentar Diskusi",
                subtitle = "Kirim apresiasi atau ulasan episode ke komunitas",
                expReward = 40L,
                pointsReward = 15L,
                currentProgress = 1,
                targetProgress = 1,
                periodKey = todayDate,
                icon = Icons.Default.Forum
            ),
            RealTimeMissionItem(
                id = "weekly_watch_5",
                title = "Maraton 5 Episode Mingguan",
                subtitle = "Tonton minimal 5 episode selama minggu ini",
                expReward = 250L,
                pointsReward = 100L,
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

    // Seluruh 985 Avatar Borders & 396 Profile Effects Asli Discord
    val shopBorders = remember(context) { DiscordCosmeticsRepository.getAllBorders(context) }
    val shopProfileEffects = remember(context) { DiscordCosmeticsRepository.getAllProfileEffects(context) }

    // 4 Segmented Tabs: 0 = Misi & Check-In, 1 = Milestone, 2 = Shop & Kosmetik, 3 = Kode Redeem
    var selectedTab by remember { mutableIntStateOf(0) }
    // Sub-tab di dalam Shop & Kosmetik: 0 = Avatar Borders (985), 1 = Profile Effects (396), 2 = Custom Titles
    var cosmeticSubTab by remember { mutableIntStateOf(0) }
    var shopSearchQuery by remember { mutableStateOf("") }
    var shopFilterTier by remember { mutableStateOf("Semua") }

    // Kode Redeem input state
    var redeemCodeInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    // Filter daftar border shop
    val filteredBorders = remember(shopBorders, unlockedBorders, equippedBorderUrl, shopSearchQuery, shopFilterTier) {
        shopBorders.filter { border ->
            val matchSearch = shopSearchQuery.isBlank() ||
                border.name.contains(shopSearchQuery, ignoreCase = true) ||
                border.category.contains(shopSearchQuery, ignoreCase = true)
            val isOwned = unlockedBorders.contains(border.id) || border.imageUrl == equippedBorderUrl
            val matchFilter = when (shopFilterTier) {
                "Koleksiku" -> isOwned
                "Starter" -> border.tier == "A"
                "Lv. 35+" -> border.tier == "B"
                "Limited Redeem" -> border.tier == "C"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    // Filter daftar profile effect shop
    val filteredEffects = remember(shopProfileEffects, unlockedEffects, equippedProfileEffectUrl, shopSearchQuery, shopFilterTier) {
        shopProfileEffects.filter { effect ->
            val matchSearch = shopSearchQuery.isBlank() ||
                effect.name.contains(shopSearchQuery, ignoreCase = true) ||
                effect.category.contains(shopSearchQuery, ignoreCase = true)
            val isOwned = unlockedEffects.contains(effect.id) || effect.animationUrl == equippedProfileEffectUrl
            val matchFilter = when (shopFilterTier) {
                "Koleksiku" -> isOwned
                "Starter" -> effect.tier == "A"
                "Lv. 35+" -> effect.tier == "B"
                "Limited Redeem" -> effect.tier == "C"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    val chunkedBorders = remember(filteredBorders) { filteredBorders.take(60).chunked(2) }
    val chunkedEffects = remember(filteredEffects) { filteredEffects.take(60).chunked(2) }
    val publicEventCodes = remember {
        listOf(
            PublicEventCode("YURAVENOM", "Venom Limited Release", "Venom Symbiote Frame + Effect", 200, 100),
            PublicEventCode("MYTHIC2026", "Event Launching Sovereign", "Gold Sovereign Crest + 500 Poin", 500, 500),
            PublicEventCode("YURAUPDATE", "Kompensasi Update Server", "150 EXP & 100 Poin", 150, 100)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR: Ringkasan Ramping 1 Baris (Bebas Tumpukan Kotak Overlay)
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
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "Misi Harian, Kosmetik Discord & Kode Redeem",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            // Ringkasan Ramping: Lv. X • ✦ [Poin] Poin
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF14161F))
                    .border(0.5.dp, Color(0xFF232738), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lv. $level",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = "  •  ",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Text(
                    text = "✦ ${FirebaseManager.formatNumber(points)} Poin",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 4 Segmented Pill Tabs
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
                        fontSize = 15.sp,
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
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Capai level target untuk membuka Custom Title gratis dan pasang langsung ke profil Anda.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
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

            // TAB 2: SHOP & KOSMETIK (GAYA ANIBI GRID 2 KOLOM)
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
                                    text = "Animasi Kosmetik (APNG 60 FPS)",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily
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

                        // Sub-Tab: Avatar Border (985) | Profile Effect (396) | Custom Title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF11131B))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val subTabs = listOf(
                                "Avatar Border (${shopBorders.size})",
                                "Effect (${shopProfileEffects.size})",
                                "Title"
                            )
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
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }

                        // Kolom Pencarian & Filter Chip (Untuk Border & Effect)
                        if (cosmeticSubTab == 0 || cosmeticSubTab == 1) {
                            OutlinedTextField(
                                value = shopSearchQuery,
                                onValueChange = { shopSearchQuery = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        if (cosmeticSubTab == 0) "Cari dari 985 Border Avatar..." else "Cari dari 396 Profile Effect...",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.5.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (shopSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { shopSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFF14161E),
                                    unfocusedContainerColor = Color(0xFF11131B),
                                    focusedBorderColor = Color(0xFFFF5722),
                                    unfocusedBorderColor = Color(0xFF232738),
                                    cursorColor = Color(0xFFFF5722)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Chip Filter
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val filterChips = listOf("Semua", "Koleksiku", "Starter", "Lv. 35+", "Limited Redeem")
                                items(filterChips) { chip ->
                                    val selected = shopFilterTier == chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) Color(0xFFFF5722) else Color(0xFF141722))
                                            .border(
                                                width = 0.5.dp,
                                                color = if (selected) Color(0xFFFF5722) else Color(0xFF222738),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable { shopFilterTier = chip }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = chip,
                                            color = if (selected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 11.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SUB-TAB 0: AVATAR BORDERS (GRID 2 KOLOM ANIBI)
                if (cosmeticSubTab == 0) {
                    items(chunkedBorders) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { border ->
                                val isUnlocked = unlockedBorders.contains(border.id)
                                val isEquipped = equippedBorderUrl == border.imageUrl

                                ShopCosmeticGridCard(
                                    name = border.name,
                                    imageUrl = border.imageUrl,
                                    isBorder = true,
                                    userAvatarUrl = userAvatarUrl,
                                    isCosmeticsAnimationActive = cosmeticAnimate,
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    tier = border.tier,
                                    requiredLevel = border.requiredLevel,
                                    pointCost = border.pointCost,
                                    userPoints = points,
                                    userLevel = level,
                                    modifier = Modifier.weight(1f),
                                    onActionClick = {
                                        if (isEquipped) {
                                            FirebaseManager.unlockOrEquipBorder(context, null) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            FirebaseManager.unlockOrEquipBorder(context, border) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (filteredBorders.size > 60) {
                        item {
                            Text(
                                text = "Menampilkan 60 dari ${filteredBorders.size} Avatar Border. Gunakan kolom pencarian di atas untuk nama lainnya.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // SUB-TAB 1: PROFILE EFFECTS (GRID 2 KOLOM ANIBI)
                if (cosmeticSubTab == 1) {
                    items(chunkedEffects) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { effect ->
                                val isUnlocked = unlockedEffects.contains(effect.id)
                                val isEquipped = equippedProfileEffectUrl == effect.animationUrl

                                ShopCosmeticGridCard(
                                    name = effect.name,
                                    imageUrl = effect.animationUrl,
                                    isBorder = false,
                                    userAvatarUrl = userAvatarUrl,
                                    isCosmeticsAnimationActive = cosmeticAnimate,
                                    isUnlocked = isUnlocked,
                                    isEquipped = isEquipped,
                                    tier = effect.tier,
                                    requiredLevel = effect.requiredLevel,
                                    pointCost = effect.pointCost,
                                    userPoints = points,
                                    userLevel = level,
                                    modifier = Modifier.weight(1f),
                                    onActionClick = {
                                        if (isEquipped) {
                                            FirebaseManager.unlockOrEquipProfileEffect(context, null) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            FirebaseManager.unlockOrEquipProfileEffect(context, effect) { _, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (filteredEffects.size > 60) {
                        item {
                            Text(
                                text = "Menampilkan 60 dari ${filteredEffects.size} Profile Effect. Gunakan kolom pencarian di atas untuk nama lainnya.",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // SUB-TAB 2: CUSTOM TITLES
                if (cosmeticSubTab == 2) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Klaim Kode Redeem",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tukarkan kode event untuk membuka Cosmetic Limited, EXP & Poin gratis langsung ke akun Anda.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = redeemCodeInput,
                            onValueChange = { redeemCodeInput = it.uppercase() },
                            singleLine = true,
                            placeholder = { Text("Contoh: YURAVENOM, MYTHIC2026", color = Color(0xFF64748B)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
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
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFF14161E),
                                unfocusedContainerColor = Color(0xFF11131B),
                                focusedBorderColor = Color(0xFFFF5722),
                                unfocusedBorderColor = Color(0xFF232738),
                                cursorColor = Color(0xFFFF5722)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (redeemCodeInput.isNotBlank() && !isRedeeming) {
                                        Brush.horizontalGradient(listOf(Color(0xFFFF5722), Color(0xFFE11D48)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFF1F2433), Color(0xFF1F2433)))
                                    }
                                )
                                .clickable(enabled = redeemCodeInput.isNotBlank() && !isRedeeming) {
                                    isRedeeming = true
                                    FirebaseManager.claimRedeemCode(context, redeemCodeInput) { ok, msg ->
                                        isRedeeming = false
                                        if (ok) redeemCodeInput = ""
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRedeeming) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Memverifikasi Kode...",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            } else {
                                Text(
                                    text = "Tukarkan Kode Sekarang",
                                    color = if (redeemCodeInput.isNotBlank()) Color.White else Color(0xFF64748B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Kode Event Aktif Komunitas",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }

                items(publicEventCodes, key = { it.code }) { itemCode ->
                    val isClaimed = claimedRedeemCodes.contains(itemCode.code)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF11131B))
                            .border(0.5.dp, Color(0xFF1D212E), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isClaimed) {
                                if (!isClaimed) {
                                    redeemCodeInput = itemCode.code
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = itemCode.code,
                                    color = if (isClaimed) Color(0xFF64748B) else Color(0xFFFF5722),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isClaimed) {
                                    Text(
                                        text = "SUDAH DIKLAIM",
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                } else {
                                    Text(
                                        text = "AKTIF",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = itemCode.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hadiah: ${itemCode.rewards}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                        }

                        if (!isClaimed) {
                            IconButton(onClick = { redeemCodeInput = itemCode.code }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Kode",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/**
 * Kartu Grid 2 Kolom untuk Shop & Kosmetik (Gaya Anibi)
 */
@Composable
private fun ShopCosmeticGridCard(
    name: String,
    imageUrl: String,
    isBorder: Boolean,
    userAvatarUrl: String,
    isCosmeticsAnimationActive: Boolean,
    isUnlocked: Boolean,
    isEquipped: Boolean,
    tier: String,
    requiredLevel: Int,
    pointCost: Long,
    userPoints: Long,
    userLevel: Int,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit
) {
    val isLevelLocked = tier == "B" && userLevel < requiredLevel
    val isRedeemOnly = tier == "C"
    val canAfford = userPoints >= pointCost && !isLevelLocked && !isRedeemOnly

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF11131B))
            .border(
                width = if (isEquipped) 1.dp else 0.5.dp,
                color = if (isEquipped) Color(0xFFE11D48) else Color(0xFF1D212E),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Baris Atas: Badge Tier & Status Milik
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tierLabel = when (tier) {
                "C" -> "Limited"
                "B" -> "Lv. $requiredLevel+"
                else -> "Starter"
            }
            val tierColor = when (tier) {
                "C" -> Color(0xFFE11D48)
                "B" -> Color(0xFFF59E0B)
                else -> Color(0xFF38BDF8)
            }
            Text(
                text = tierLabel,
                color = tierColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )

            if (isUnlocked) {
                Text(
                    text = "Dimiliki",
                    color = Color(0xFF10B981),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            } else if (isRedeemOnly) {
                Text(
                    text = "Redeem",
                    color = Color(0xFFE11D48),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Animated Preview Berukuran Besar (100.dp)
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isBorder) {
                // Foto profil pengguna di tengah dilingkari border avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF181B26)),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = userAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
                AnimatedCosmeticImage(
                    url = DiscordCosmeticsRepository.getOptimizedUrl(imageUrl, size = 160, animated = isCosmeticsAnimationActive),
                    isAnimated = isCosmeticsAnimationActive,
                    contentDescription = name,
                    modifier = Modifier.size(100.dp)
                )
            } else {
                // Profile Effect di atas kartu mini
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161824)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCosmeticImage(
                        url = imageUrl,
                        isAnimated = isCosmeticsAnimationActive,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Nama Item (White Bold 13.sp, Rata Tengah)
        Text(
            text = name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tombol Aksi Penuh di Bawah
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isEquipped -> Color(0xFF1E293B)
                        isUnlocked -> Color(0xFFE11D48)
                        canAfford -> Color(0xFFFF5722)
                        else -> Color(0xFF161824)
                    }
                )
                .clickable(enabled = isUnlocked || canAfford) { onActionClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    isEquipped -> "Terpasang"
                    isUnlocked -> "Pasang"
                    isRedeemOnly -> "🔒 Eksklusif Redeem"
                    isLevelLocked -> "🔒 Lv. $requiredLevel • ✦ $pointCost"
                    else -> "✦ $pointCost Poin"
                },
                color = when {
                    isEquipped -> Color(0xFFCBD5E1)
                    isUnlocked || canAfford -> Color.White
                    else -> Color(0xFF64748B)
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

/**
 * Streak Check-In 7 Hari Bersih & Elegan
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Streak Check-In Harian",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Hari ke-7 bonus +100 EXP & +50 Poin",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Text(
                text = "$currentStreak/7 Hari",
                color = Color(0xFFFF5722),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7 Kapsul Check-In Bersih dengan Ikon Hadiah Emas/Kristal Jelas
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
                        .background(if (isClaimedDay) Color(0x22E11D48) else Color(0xFF141722))
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
                        val iconVector = when {
                            day == 7 -> Icons.Default.EmojiEvents
                            day >= 5 -> Icons.Default.Diamond
                            else -> Icons.Default.AutoAwesome
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = if (isBonusDay) Color(0xFFF59E0B) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isBonusDay) "+50" else "+20",
                        color = if (isBonusDay) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tombol Klaim yang Elegan
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
                    fontFamily = SfProFontFamily
                )
                if (mission.isWeekly) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MINGGUAN",
                        color = Color(0xFFF59E0B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mission.subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = SfProFontFamily
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${mission.expReward} EXP",
                    color = Color(0xFFFF5722),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✦ +${mission.pointsReward}",
                    color = Color(0xFFF43F5E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${mission.currentProgress}/${mission.targetProgress})",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when {
                        isClaimed -> Color(0xFF181B26)
                        isCompleted -> Color(0xFFE11D48)
                        else -> Color(0xFF141722)
                    }
                )
                .clickable(enabled = !isClaimed) { onClaimClick() }
                .padding(horizontal = 13.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isClaimed) "Klaim" else if (isCompleted) "Klaim" else "Belum",
                color = if (isClaimed) Color(0xFF475569) else if (isCompleted) Color.White else Color(0xFF64748B),
                fontSize = 11.5.sp,
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
                        fontFamily = SfProFontFamily
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
                            fontFamily = SfProFontFamily
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
                    .clickable(enabled = isUnlocked || canAfford) { onActionClick() }
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
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = titleDef.description,
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontFamily = SfProFontFamily
        )
    }
}
