package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PointsRewardsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile by FirebaseManager.userProfile.collectAsState()
    var isClaiming by remember { mutableStateOf(false) }

    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isAlreadyCheckedIn = currentProfile?.lastCheckInDate == todayDate
    val streak = currentProfile?.checkInStreak ?: 0
    val activePoints = currentProfile?.points ?: 0L

    val checkInRewards = listOf(10L, 15L, 20L, 25L, 30L, 40L, 50L)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0F12))
    ) {
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
                    text = "Points & Rewards",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                // 1. Header Saldo Poin Real-Time
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF1E1B2E),
                                        Color(0xFF281C3F),
                                        Color(0xFF1E1B2E)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBBF24).copy(alpha = 0.15f))
                                    .border(1.5.dp, Color(0xFFFBBF24), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Poin",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Total Saldo Poin",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$activePoints",
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "POIN",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Kalender Check-In 7 Hari
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Check-in Harian",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "Streak: $streak Hari",
                                color = Color(0xFFFF5722),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7 Kapsul Hari
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(checkInRewards) { index, rewardPoints ->
                                val dayNum = index + 1
                                val isClaimed = streak >= dayNum && (dayNum < streak || isAlreadyCheckedIn)
                                val isToday = (streak == index && !isAlreadyCheckedIn) || (streak == 0 && index == 0 && !isAlreadyCheckedIn)

                                DayCheckInCapsule(
                                    dayNum = dayNum,
                                    reward = rewardPoints,
                                    isClaimed = isClaimed,
                                    isToday = isToday
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tombol Klaim Check-In
                        Button(
                            onClick = {
                                if (isAlreadyCheckedIn) return@Button
                                isClaiming = true
                                val dayIndex = streak.coerceIn(0, 6)
                                val reward = checkInRewards[dayIndex]

                                FirebaseManager.checkInToday(
                                    dayIndex = dayIndex,
                                    pointsReward = reward,
                                    onSuccess = { gained ->
                                        isClaiming = false
                                        Toast.makeText(context, "Selamat! +$gained Poin & +50 XP didapatkan!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        isClaiming = false
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isAlreadyCheckedIn && !isClaiming,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722),
                                disabledContainerColor = Color(0xFF1E2024)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isAlreadyCheckedIn) "Sudah Check-In Hari Ini" else "Klaim Check-In Hari Ini (+${checkInRewards.getOrElse(streak.coerceIn(0, 6)) { 10L }} Poin)",
                                color = if (isAlreadyCheckedIn) Color(0xFF94A3B8) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                // 3. Misi Harian
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Misi Harian",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    DailyMissionCard(
                        icon = Icons.Default.Tv,
                        title = "Tonton 1 Episode",
                        reward = "+20 Poin & +20 XP",
                        desc = "Selesaikan nonton 1 episode anime apa saja",
                        isCompleted = false
                    )
                }

                item {
                    DailyMissionCard(
                        icon = Icons.Default.Stars,
                        title = "Kirim Reaksi Komentar",
                        reward = "+10 Poin & +10 XP",
                        desc = "Beri komentar atau suka pada episode favorit",
                        isCompleted = false
                    )
                }

                // 4. Tukar Poin / Hadiah
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Tukar Hadiah & Badge",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    RewardExchangeCard(
                        title = "Badge Komentar Spesial",
                        cost = 150L,
                        desc = "Badge bintang emas berkilau di samping nama saat berkomentar",
                        onExchange = {
                            Toast.makeText(context, "Fitur penukaran badge segera hadir!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    RewardExchangeCard(
                        title = "Bingkai Avatar Celestial",
                        cost = 300L,
                        desc = "Bingkai sayap emas mewah untuk foto profil kamu",
                        onExchange = {
                            Toast.makeText(context, "Fitur penukaran bingkai segera hadir!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    RewardExchangeCard(
                        title = "Title Level 'Otaku Sepuh'",
                        cost = 500L,
                        desc = "Gelar eksklusif di profil dan papan peringkat",
                        onExchange = {
                            Toast.makeText(context, "Fitur penukaran title segera hadir!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCheckInCapsule(
    dayNum: Int,
    reward: Long,
    isClaimed: Boolean,
    isToday: Boolean
) {
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isClaimed -> Color(0xFF1E293B)
                    isToday -> Color(0xFF2E1A10)
                    else -> Color(0xFF161B26)
                }
            )
            .border(
                width = if (isToday) 1.5.dp else 1.dp,
                color = when {
                    isToday -> Color(0xFFFF5722)
                    isClaimed -> Color(0xFF3B82F6)
                    else -> Color(0x22FFFFFF)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "H$dayNum",
                color = if (isToday) Color(0xFFFF5722) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )

            if (isClaimed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selesai",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isToday) Color(0xFFFBBF24) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "+$reward",
                color = if (isToday) Color.White else Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun DailyMissionCard(
    icon: ImageVector,
    title: String,
    reward: String,
    desc: String,
    isCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B26))
            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2024)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
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
                text = reward,
                color = Color(0xFFFBBF24),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun RewardExchangeCard(
    title: String,
    cost: Long,
    desc: String,
    onExchange: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161B26))
            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF281C3F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
                Text(
                    text = desc,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2D1B4E))
                    .border(1.dp, Color(0xFFFDE68A).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clickable { onExchange() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "$cost Poin",
                    color = Color(0xFFFDE68A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }
    }
}
