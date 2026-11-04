package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.api.BirthdayCharacterItem
import com.yuranime.dinz.data.api.BirthdayTodayData
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily

@Composable
fun BirthdayTodaySection(
    birthdayData: BirthdayTodayData?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onItemClick: (BirthdayCharacterItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF221512))
                        .border(0.8.dp, ColorAccentPrimary.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cake,
                        contentDescription = null,
                        tint = ColorAccentPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(9.dp))

                Column {
                    Text(
                        text = "Ulang Tahun Hari Ini",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        color = ColorTextPrimary
                    )
                    val dateLabel = birthdayData?.dateId?.takeIf { it.isNotBlank() }
                    if (dateLabel != null) {
                        Text(
                            text = dateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            color = ColorAccentPrimary
                        )
                    }
                }
            }

            if (birthdayData != null && birthdayData.items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF161820))
                        .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${birthdayData.items.size} Tokoh",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when {
            isLoading && (birthdayData == null || birthdayData.items.isEmpty()) -> {
                BirthdayShimmerRow()
            }

            errorMessage != null && (birthdayData == null || birthdayData.items.isEmpty()) -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14161D))
                        .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { onRetry() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gagal memuat daftar ulang tahun hari ini",
                        color = ColorTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Muat ulang",
                            tint = ColorAccentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Coba Lagi",
                            color = ColorAccentPrimary,
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            birthdayData != null && birthdayData.items.isNotEmpty() -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = birthdayData.items,
                        key = { "ultah_${it.type}_${it.id}_${it.slug}" }
                    ) { item ->
                        BirthdayCharacterCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BirthdayCharacterCard(
    item: BirthdayCharacterItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSeiyuu = item.isSeiyuu
    val badgeColor = if (isSeiyuu) Color(0xFFFACC15) else ColorAccentPrimary
    val badgeLabel = if (isSeiyuu) "SEIYUU / STAF" else "KARAKTER"

    Row(
        modifier = Modifier
            .width(192.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161D))
            .border(0.8.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .fillMaxHeight()
                .background(Color(0xFF1B1E28))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl.ifBlank { item.thumbnailUrl })
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime)
                    .error(R.drawable.placeholder_yuranime)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = badgeLabel,
                    color = badgeColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    letterSpacing = 0.3.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.nameKanji.ifBlank { "Lihat profil lengkap" },
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BirthdayShimmerRow() {
    val transition = rememberInfiniteTransition(label = "ultahShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ultahShimmerOffset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF15171F), Color(0xFF222531), Color(0xFF15171F)),
        start = Offset(translateAnim - 250f, 0f),
        end = Offset(translateAnim, 180f)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(192.dp)
                    .height(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}
