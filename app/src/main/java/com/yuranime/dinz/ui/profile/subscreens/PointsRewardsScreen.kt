package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.ui.components.PrimoPointIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily

private data class RewardMissionItem(
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

private data class RedeemPresetCode(
    val code: String,
    val label: String,
    val rewardText: String
)

/**
 * TAHAP 6: Redesign Halaman Points & Rewards (100% Mengacu Referensi `1000127705.jpg`)
 * - Hero Balance: Saldo Poin Kamu (ExtraBold 34.sp) + Ilustrasi koin besar 72.dp (ic_primo.png)
 * - 7-Day Streak Check-In Card: 7 hari kapsul dengan ikon primo.png + Full-width Check in capsule
 * - Inline Kode Redeem Bar: Tepat di bawah kartu check-in
 * - Earn Rewards: Daftar misi vertikal bersih dengan ikon, PrimoPointIcon, dan tombol kapsul Go/Claim
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
    val rankTitle = currentProfile?.rankTitle ?: FirebaseManager.computeRankTitle(level)
    val currentStreak = currentProfile?.checkInStreak ?: 0
    val lastCheckInDate = currentProfile?.lastCheckInDate.orEmpty()
    val todayDate = remember { FirebaseManager.getTodayDateString() }
    val isCheckedInToday = lastCheckInDate == todayDate
    val claimedMissions = currentProfile?.claimedMissions ?: emptyMap()
    val weekKey = remember { FirebaseManager.getCurrentWeekKey() }
    val watchHistory = remember { authDb.getWatchHistory() }

    var redeemInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    val presetCodes = remember {
        listOf(
            RedeemPresetCode("YURAVENOM", "Venom Event", "+200 EXP & +100 Poin"),
            RedeemPresetCode("MYTHIC2026", "Mythic 2026", "+500 EXP & +500 Poin"),
            RedeemPresetCode("YURAUPDATE", "Server Update", "+150 EXP & +100 Poin")
        )
    }

    val missions = remember(watchHistory.size, isCheckedInToday, todayDate, weekKey) {
        listOf(
            RewardMissionItem(
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
            RewardMissionItem(
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
            RewardMissionItem(
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
            RewardMissionItem(
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
            RewardMissionItem(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR: Tombol Kembali + Judul + Kapsul Poin Pengguna
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
                    text = "Check-In Harian, Misi & Kode Redeem",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            // Kapsul Poin Kanan Atas
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF14161F))
                    .border(0.5.dp, Color(0xFF232738), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimoPointIcon(size = 15.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${FirebaseManager.formatNumber(points)} Poin",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO BALANCE (Referensi 1000127705.jpg)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF171A27), Color(0xFF10121C), Color(0xFF0D0F17))
                            )
                        )
                        .border(0.75.dp, Color(0xFF262C3D), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Saldo Poin Kamu",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FirebaseManager.formatNumber(points),
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF1F2436))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Level $level • $rankTitle",
                                    color = Color(0xFFFF5722),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        // Ilustrasi Koin Primo Besar 72.dp di Kanan
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AE11D48)),
                            contentAlignment = Alignment.Center
                        ) {
                            PrimoPointIcon(size = 72.dp)
                        }
                    }
                }
            }

            // 2. KARTU CHECK-IN 7 HARI (Referensi 1000127705.jpg)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF11131B))
                        .border(0.5.dp, Color(0xFF1E2230), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Header Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Check-in 7 Hari",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1A1D2A))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$currentStreak Hari Berturut",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 7 Hari Kapsul
                    val dailyRewards = listOf(10, 15, 20, 25, 30, 40, 50)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dailyRewards.forEachIndexed { idx, rewardAmount ->
                            val isClaimed = idx < currentStreak && (isCheckedInToday || idx < currentStreak - 1)
                            val isCurrentTarget = !isCheckedInToday && idx == (currentStreak % 7)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isClaimed -> Color(0xFF151822)
                                            isCurrentTarget -> Color(0xFF1E1F30)
                                            else -> Color(0xFF12141C)
                                        }
                                    )
                                    .border(
                                        width = if (isCurrentTarget) 1.dp else 0.5.dp,
                                        color = when {
                                            isCurrentTarget -> Color(0xFFE11D48)
                                            isClaimed -> Color(0xFF262D3F)
                                            else -> Color(0xFF1B1E29)
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Hari ${idx + 1}",
                                    color = if (isClaimed) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily
                                )

                                if (isClaimed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sudah Diklaim",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    PrimoPointIcon(size = 18.dp)
                                }

                                Text(
                                    text = "+$rewardAmount",
                                    color = when {
                                        isClaimed -> Color(0xFF64748B)
                                        isCurrentTarget -> Color(0xFFFF5722)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tombol Check In Full Width Capsule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCheckedInToday) {
                                    Brush.horizontalGradient(listOf(Color(0xFF181B26), Color(0xFF181B26)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFFFF5722), Color(0xFFE11D48)))
                                }
                            )
                            .clickable(enabled = !isCheckedInToday) {
                                FirebaseManager.checkInToday(context) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCheckedInToday) "Sudah Check-In Hari Ini" else "Check in Sekarang (+50 EXP & +20 Poin)",
                            color = if (isCheckedInToday) Color(0xFF64748B) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            // 3. INLINE KODE REDEEM BAR (Tepat di bawah check-in)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF11131B))
                        .border(0.5.dp, Color(0xFF1E2230), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Kode Redeem Hadiah",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Bar + Tombol Klaim
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161924))
                            .border(0.5.dp, Color(0xFF252B3D), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = redeemInput,
                            onValueChange = { redeemInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(Color(0xFFE11D48)),
                            decorationBox = { innerTextField ->
                                if (redeemInput.isEmpty()) {
                                    Text(
                                        text = "Masukkan kode redeem...",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.5.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (redeemInput.isNotBlank()) Color(0xFFE11D48) else Color(0xFF1F2433)
                                )
                                .clickable(enabled = redeemInput.isNotBlank() && !isRedeeming) {
                                    isRedeeming = true
                                    FirebaseManager.claimRedeemCode(context, redeemInput.trim()) { success, msg ->
                                        isRedeeming = false
                                        if (success) redeemInput = ""
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRedeeming) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = "Klaim",
                                    color = if (redeemInput.isNotBlank()) Color.White else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chip Kode Event Cepat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetCodes.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161822))
                                    .border(0.5.dp, Color(0xFF222838), RoundedCornerShape(8.dp))
                                    .clickable { redeemInput = preset.code }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preset.code,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = preset.label,
                                        color = Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. DAFTAR MISI: EARN REWARDS (Referensi 1000127705.jpg)
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Earn Rewards",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Selesaikan misi harian & mingguan untuk meraih EXP dan Poin",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }

            items(missions, key = { it.id }) { mission ->
                val isClaimed = claimedMissions[mission.id] == mission.periodKey ||
                    (mission.id == "daily_checkin" && isCheckedInToday)
                val isCompleted = mission.currentProgress >= mission.targetProgress

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
                            PrimoPointIcon(size = 13.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+${mission.pointsReward} Poin",
                                color = Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${mission.expReward} EXP",
                                color = Color(0xFFFF5722),
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
                            .clickable(enabled = !isClaimed) {
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
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isClaimed) "Klaim" else if (isCompleted) "Klaim" else "Go",
                            color = if (isClaimed) Color(0xFF475569) else if (isCompleted) Color.White else Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(115.dp))
            }
        }
    }
}
