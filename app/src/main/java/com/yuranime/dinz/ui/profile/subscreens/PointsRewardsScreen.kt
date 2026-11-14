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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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

/**
 * Halaman Points & Rewards (100% Mengacu Desain Referensi `1000127705_2.jpg`)
 * - Latar Belakang Gradasi Atas Lembut: #3B0D16 -> #1A0C11 -> #0B0B0E
 * - Top Bar Bersih: Tombol Kembali '<' minimalis, Judul Tengah "Rewards", Kanan kosong
 * - Area Saldo Poin Terbuka (Direct On Gradient): Saldo Poin 40.sp ExtraBold + PrimoPointIcon besar 76.dp tanpa kotak
 * - Kartu Check-in 7 Hari: 1 kontainer rapi #13151C, teks "Kamu sudah check-in X Hari", kapsul hari aktif beraksen #E11D48
 * - Inline Kode Redeem Bar: 1 baris bersih 52.dp tanpa bocoran kode event
 * - Earn Rewards Section: 1 kontainer rapi #13151C berisi seluruh misi harian & mingguan dengan divider bersih
 */
@Composable
fun PointsRewardsScreen(
    authDb: LocalAuthDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by FirebaseManager.userProfile.collectAsState()

    val points = currentProfile?.points ?: 0L
    val currentStreak = currentProfile?.checkInStreak ?: 0
    val lastCheckInDate = currentProfile?.lastCheckInDate.orEmpty()
    val todayDate = remember { FirebaseManager.getTodayDateString() }
    val isCheckedInToday = lastCheckInDate == todayDate
    val claimedMissions = currentProfile?.claimedMissions ?: emptyMap()
    val weekKey = remember { FirebaseManager.getCurrentWeekKey() }
    val watchHistory = remember { authDb.getWatchHistory() }

    var redeemInput by remember { mutableStateOf("") }
    var isRedeeming by remember { mutableStateOf(false) }

    val missions = remember(watchHistory.size, isCheckedInToday, todayDate, weekKey) {
        listOf(
            RewardMissionItem(
                id = "daily_checkin",
                title = "Check-In Harian",
                subtitle = "Buka aplikasi dan klaim hadiah harian",
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B0D16),
                        Color(0xFF1A0C11),
                        Color(0xFF0B0B0E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP APP BAR: Tombol Kembali '<' minimalis, Judul Tengah "Rewards"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Rewards",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Penyeimbang tombol kembali agar judul tetap tepat di tengah
            Spacer(modifier = Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO BALANCE (LANGSUNG DI ATAS GRADASI, TANPA KARTU PEMBUNGKUS)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saldo Poin",
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FirebaseManager.formatNumber(points),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = SfProFontFamily
                        )
                    }

                    // Ikon Primogem Besar 76.dp langsung di atas gradasi tanpa lingkaran overlay
                    PrimoPointIcon(size = 76.dp)
                }
            }

            // 2. KARTU CHECK-IN 7 HARI (1 KONTAINER RAPI #13151C)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF13151C))
                        .border(0.5.dp, Color(0xFF1E222D), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    // Header Card
                    Text(
                        text = "Kamu sudah check-in $currentStreak Hari",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 7 Hari Row
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrentTarget) Color(0x28E11D48) else Color.Transparent
                                    )
                                    .border(
                                        width = if (isCurrentTarget) 1.dp else 0.dp,
                                        color = if (isCurrentTarget) Color(0xFFE11D48) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    text = "$rewardAmount",
                                    color = when {
                                        isClaimed -> Color(0xFF64748B)
                                        isCurrentTarget -> Color(0xFFE11D48)
                                        else -> Color(0xFFCBD5E1)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tombol Check In Kapsul Full Width
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isCheckedInToday) Color(0xFF1C1F2B) else Color(0xFFE11D48)
                            )
                            .clickable(enabled = !isCheckedInToday) {
                                FirebaseManager.checkInToday(context) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCheckedInToday) "Sudah Check-In Hari Ini" else "Check in",
                            color = if (isCheckedInToday) Color(0xFF64748B) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            // 3. INLINE KODE REDEEM BAR (1 BARIS BERSIH 52.dp TANPA KODE BOCOR)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF13151C))
                        .border(0.5.dp, Color(0xFF1E222D), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = redeemInput,
                        onValueChange = { redeemInput = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color(0xFFE11D48)),
                        decorationBox = { innerTextField ->
                            if (redeemInput.isEmpty()) {
                                Text(
                                    text = "Masukkan kode redeem...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp,
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (redeemInput.isNotBlank()) Color(0xFFE11D48) else Color(0xFF1E2230)
                            )
                            .clickable(enabled = redeemInput.isNotBlank() && !isRedeeming) {
                                isRedeeming = true
                                FirebaseManager.claimRedeemCode(context, redeemInput.trim()) { success, msg ->
                                    isRedeeming = false
                                    if (success) redeemInput = ""
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }

            // 4. EARN REWARDS SECTION (1 KONTAINER RAPI #13151C)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF13151C))
                        .border(0.5.dp, Color(0xFF1E222D), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Earn Rewards",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    missions.forEachIndexed { index, mission ->
                        val isClaimed = claimedMissions[mission.id] == mission.periodKey ||
                            (mission.id == "daily_checkin" && isCheckedInToday)
                        val isCompleted = mission.currentProgress >= mission.targetProgress

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1D28)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mission.icon,
                                    contentDescription = null,
                                    tint = if (isClaimed) Color(0xFF64748B) else Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mission.title,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PrimoPointIcon(size = 14.dp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "+${mission.pointsReward}",
                                        color = Color(0xFFE11D48),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = mission.subtitle,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isClaimed -> Color(0xFF1A1D28)
                                            isCompleted -> Color(0xFFE11D48)
                                            else -> Color(0xFF1E2230)
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
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isClaimed) "Claimed" else if (isCompleted) "Claim" else "Go",
                                    color = if (isClaimed) Color(0xFF475569) else if (isCompleted) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        if (index < missions.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFF1A1D27),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Spacer agar tidak tertutup navbar
            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}
