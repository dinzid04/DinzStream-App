package com.yuranime.dinz.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.data.firebase.UserProfile
import com.yuranime.dinz.ui.components.GeometricTitleBadge
import com.yuranime.dinz.ui.components.RpgSvgIconCatalog
import com.yuranime.dinz.ui.components.TitleVfxTier
import com.yuranime.dinz.ui.components.VerifiedBadgeIcon
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlin.math.ceil

/**
 * Dialog Progres Level, Estimator Target EXP (mis. menuju Lv. 85),
 * Katalog Ikon Level SVG, & Daftar Role Resmi Yuranime (`Owner`, `Admin`, `Perintis Yuranime`, `Member`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelProgressDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onOpenShop: () -> Unit = {}
) {
    val currentLevel = (profile?.level ?: 1).coerceAtLeast(1)
    val currentExp = (profile?.exp ?: 0L).coerceAtLeast(0L)
    val nextLevelExp = FirebaseManager.computeNextLevelTargetExp(currentLevel)
    val levelStartExp = ((currentLevel - 1).coerceAtLeast(0) * 500L)
    val levelSpan = (nextLevelExp - levelStartExp).coerceAtLeast(1L)
    val inLevelProgress = ((currentExp - levelStartExp).toFloat() / levelSpan.toFloat()).coerceIn(0f, 1f)
    val expToNext = (nextLevelExp - currentExp).coerceAtLeast(0L)

    val currentRole = profile?.role?.takeIf { it.isNotBlank() } ?: "Member"
    val roleSpec = remember(currentRole) { RpgSvgIconCatalog.resolveRoleSpec(currentRole) }
    val currentRankTitle = FirebaseManager.computeRankTitle(currentLevel)
    val currentLvIconRes = remember(currentLevel) { RpgSvgIconCatalog.resolveLevelIcon(currentLevel) }
    val currentMiniIconRes = remember(currentRankTitle, currentLevel) {
        RpgSvgIconCatalog.resolveMiniTitleIcon(currentRankTitle, currentLevel)
    }

    // Default target: jika masih < 85 arahkan milestone menarik terdekat atau +5 / Lv. 85
    val defaultTarget = remember(currentLevel) {
        when {
            currentLevel < 15 -> 15
            currentLevel < 30 -> 30
            currentLevel < 50 -> 50
            currentLevel < 85 -> 85
            else -> currentLevel + 10
        }
    }
    var targetLevelInput by remember(currentLevel) { mutableStateOf(defaultTarget.toString()) }
    val parsedTargetLevel = (targetLevelInput.toIntOrNull() ?: defaultTarget).coerceIn(1, 999)
    val targetTotalExp = ((parsedTargetLevel - 1).coerceAtLeast(0) * 500L)
    val remainingTargetExp = (targetTotalExp - currentExp).coerceAtLeast(0L)
    val targetRankPreview = remember(parsedTargetLevel) { FirebaseManager.computeRankTitle(parsedTargetLevel) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Progres & Estimator, 1 = Role & Lencana

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0E1016))
                .border(1.dp, Color(0xFF232838), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            // Header + Accessible 44dp Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1F38BDF8))
                            .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = currentLvIconRes),
                            contentDescription = "Ikon Level $currentLevel",
                            colorFilter = ColorFilter.tint(Color(0xFF38BDF8)),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Progres Level • LV. $currentLevel",
                                color = Color.White,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile?.isVerified == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                VerifiedBadgeIcon(
                                    isVerified = true,
                                    colorHex = profile.verifiedColor,
                                    size = 15.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = currentMiniIconRes),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color(0xFFF59E0B)),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = currentRankTitle,
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = " • ${roleSpec.label}",
                                color = Color(roleSpec.accentHex),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF171B26))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ringkasan EXP saat ini -> Level berikutnya
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141822))
                    .border(1.dp, Color(0xFF212636), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXP Saat Ini: ${FirebaseManager.formatNumber(currentExp)}",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                    Text(
                        text = "Butuh +${FirebaseManager.formatNumber(expToNext)} ke Lv.${currentLevel + 1}",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1E2433))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(inLevelProgress.coerceAtLeast(if (currentExp > 0) 0.04f else 0f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF5722), Color(0xFFE11D48), Color(0xFFF59E0B))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Tab Navigation (44dp minimum height!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141720))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Level & Estimator", "Role & Lencana").forEachIndexed { index, label ->
                    val isActive = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isActive) Color(0xFFE11D48) else Color.Transparent)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clipToBounds(),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KALKULATOR / ESTIMATOR TARGET LEVEL
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0xFF283044), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Kalkulator Target Level (EXP Estimator)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Preset cepat Level (termasuk Lv. 85 sesuai contoh kebutuhan)
                            val presets = listOf(currentLevel + 1, 25, 50, 85, 100).distinct().sorted()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { presetLv ->
                                    val selected = parsedTargetLevel == presetLv
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) Color(0x33F59E0B) else Color(0xFF1A1F2E))
                                            .border(
                                                1.dp,
                                                if (selected) Color(0xFFF59E0B) else Color(0xFF282F42),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { targetLevelInput = presetLv.toString() }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Lv. $presetLv",
                                            color = if (selected) Color(0xFFFDE047) else Color(0xFFCBD5E1),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stepper - [Input] +
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1C2130))
                                        .clickable {
                                            targetLevelInput = (parsedTargetLevel - 1).coerceAtLeast(1).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "Kurangi", tint = Color.White, modifier = Modifier.size(18.dp))
                                }

                                OutlinedTextField(
                                    value = targetLevelInput,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(3)
                                        targetLevelInput = digits
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    ),
                                    label = {
                                        Text("Target Level", fontSize = 10.5.sp, fontFamily = SfProFontFamily)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF2B3246),
                                        focusedLabelColor = Color(0xFFF59E0B),
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        cursorColor = Color(0xFFF59E0B)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1C2130))
                                        .clickable {
                                            targetLevelInput = (parsedTargetLevel + 1).coerceAtMost(999).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hasil Estimasi EXP
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0D1017))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (parsedTargetLevel <= currentLevel) {
                                    Text(
                                        text = "Target Lv. $parsedTargetLevel sudah tercapai! (Total syarat: ${FirebaseManager.formatNumber(targetTotalExp)} EXP)",
                                        color = Color(0xFF34D399),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                } else {
                                    Text(
                                        text = "Kurang ${FirebaseManager.formatNumber(remainingTargetExp)} EXP lagi untuk menembus Level $parsedTargetLevel",
                                        color = Color(0xFFFDE047),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    val checkInDaysEst = ceil(remainingTargetExp / 65.0).toInt().coerceAtLeast(1)
                                    val missionsEst = ceil(remainingTargetExp / 150.0).toInt().coerceAtLeast(1)
                                    Text(
                                        text = "Rank pada Lv.$parsedTargetLevel: $targetRankPreview • Perkiraan ~$missionsEst misi / ~$checkInDaysEst check-in harian (50–100 EXP/hari)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // JUDUL ROADMAP
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Roadmap Hadiah Ikon Level & Title",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Text(
                                text = "Lv.1 – Lv.100",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    items(RpgSvgIconCatalog.LEVEL_ROADMAP, key = { it.level }) { step ->
                        val isReached = currentLevel >= step.level
                        val reqExp = ((step.level - 1).coerceAtLeast(0) * 500L)
                        val titleDef = step.rewardCustomTitle?.let { FirebaseManager.resolveTitleDefinition(it, step.level) }
                        val stepAccent = Color(step.accentHex)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isReached) Color(0xFF141A24) else Color(0xFF10131C))
                                .border(
                                    1.dp,
                                    if (isReached) stepAccent.copy(alpha = 0.55f) else Color(0xFF1E2332),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(stepAccent.copy(alpha = if (isReached) 0.18f else 0.08f))
                                        .border(1.dp, stepAccent.copy(alpha = if (isReached) 0.6f else 0.25f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = step.levelIconRes),
                                        contentDescription = "Ikon Level ${step.level}",
                                        colorFilter = ColorFilter.tint(if (isReached) stepAccent else Color(0xFF64748B)),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LV. ${step.level} • ${step.tierLabel}",
                                        color = if (isReached) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Mini-Title: ${step.rankTitle} • Syarat ${FirebaseManager.formatNumber(reqExp)} EXP",
                                        color = if (isReached) stepAccent else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = if (isReached) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                    contentDescription = if (isReached) "Terbuka" else "Terkunci",
                                    tint = if (isReached) Color(0xFF34D399) else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (titleDef != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Bonus Custom Title:",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.5.sp,
                                        fontFamily = SfProFontFamily,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                    GeometricTitleBadge(
                                        titleName = titleDef.titleName,
                                        tierLevel = titleDef.tierLevel,
                                        styleVariant = titleDef.styleVariant,
                                        vfxTier = titleDef.vfxTier,
                                        titleId = titleDef.id,
                                        compact = true,
                                        isAnimated = isReached
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: DAFTAR ROLE KOMUNITAS (Owner, Admin, Moderator, Perintis Yuranime, Partner VIP, Member)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clipToBounds(),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Daftar Role & Lencana Khusus Komunitas Yuranime",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                    items(RpgSvgIconCatalog.ROLE_SPECS, key = { it.key }) { spec ->
                        val isCurrentRole = roleSpec.key == spec.key
                        val specColor = Color(spec.accentHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isCurrentRole) Color(0xFF191522) else Color(0xFF11141D))
                                .border(
                                    1.dp,
                                    if (isCurrentRole) specColor else Color(0xFF1F2433),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(specColor.copy(alpha = 0.16f))
                                    .border(1.dp, specColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = spec.iconRes),
                                    contentDescription = spec.label,
                                    colorFilter = ColorFilter.tint(specColor),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = spec.label,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    if (isCurrentRole) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ROLE KAMU",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(specColor)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = spec.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1F2433))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF1A1E2B))
                        .clickable {
                            onDismiss()
                            onOpenShop()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Buka Toko Title",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFE11D48))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tutup",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}
