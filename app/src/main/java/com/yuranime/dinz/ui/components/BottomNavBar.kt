package com.yuranime.dinz.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Tab navigasi utama aplikasi Yuranime.
 * Mendukung ikon solid (aktif) dan outline (tidak aktif)
 * dengan properti label dan backward compatibility untuk [title] dan [icon].
 */
enum class NavTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    JADWAL("Jadwal", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    CARI("Cari", Icons.Filled.Search, Icons.Outlined.Search),
    CHAT("Chat", Icons.AutoMirrored.Filled.Send, Icons.AutoMirrored.Outlined.Send),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person);

    // Backward compatibility getters
    val title: String get() = label
    val icon: ImageVector get() = filledIcon
}

/**
 * Floating Bottom Navigation Bar dengan gaya "Liquid Glass"
 * Terinspirasi dari quickLiquid (amarnath3003/quickLiquid):
 * - Floating pill dengan jarak aman padding horizontal dan bottom.
 * - Active tab: Lensa Liquid Glass memanjang dengan Row(Ikon Filled + Label SF Bold)
 *   didukung animasi spring physics (squash and stretch).
 * - Inactive tabs: Hanya Ikon Outline tanpa label teks sama sekali.
 * - Efek optik kaca cair: clear center, refractive rim highlight (fresnel specular),
 *   chromatic edge halus, serta bayangan lembut.
 */
@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Kapsul Utama Liquid Glass (Floating Pill)
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = Color(0x99000000),
                    ambientColor = Color(0x4D000000)
                )
                .clip(CircleShape)
                .background(Color(0xEB11141B))
                // Lapisan Pembiasan Kaca Cair (Fresnel Rim Specular + Chromatic Dispersion Edge)
                .drawBehind {
                    val strokeWidth = 1.2.dp.toPx()
                    val halfStroke = strokeWidth / 2f

                    // 1. Chromatic dispersion subtle inner rim (bias optik spektrum kaca)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x26FF5722), // Red-orange bias
                                Color(0x143B82F6), // Cyan-blue bias
                                Color(0x0DFFFFFF)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                        style = Stroke(width = strokeWidth * 1.5f)
                    )

                    // 2. Fresnel specular highlight pada bibir atas kapsul
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x59FFFFFF), // 35% specular highlight di puncak atas
                                Color(0x1AFFFFFF), // 10% di tengah
                                Color(0x08FFFFFF)  // 3% di dasar bawah
                            )
                        ),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember { MutableInteractionSource() }

                    if (isSelected) {
                        // TAB AKTIF: Kapsul Lensa Liquid Glass Memanjang (Ikon + Judul)
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = Color(0x66FF5722)
                                )
                                .clip(RoundedCornerShape(24.dp))
                                // Lensa Liquid Glass dengan aksen oranye Yuranime
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0x3DFF5722),
                                            Color(0x26EF4444),
                                            Color(0x1F141418)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(200f, 100f)
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x80FF5722),
                                            Color(0x33FF5722),
                                            Color(0x1AFFFFFF)
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true, color = ColorAccentPrimary)
                                ) { onTabSelected(tab) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.filledIcon,
                                    contentDescription = tab.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB TIDAK AKTIF: Hanya Ikon Outline Saja (Tanpa Label)
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = false, radius = 24.dp)
                                ) { onTabSelected(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.outlinedIcon,
                                contentDescription = tab.label,
                                tint = Color(0xFF8E95A5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
