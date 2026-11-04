package com.yuranime.dinz.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.yuranime.dinz.data.api.BirthdayAnimeAppearanceItem
import com.yuranime.dinz.data.api.BirthdayCharacterDetail
import com.yuranime.dinz.data.api.BirthdayCharacterItem
import com.yuranime.dinz.data.api.BirthdayProfileAttribute
import com.yuranime.dinz.data.api.BirthdayVoiceActorItem
import com.yuranime.dinz.data.api.BirthdayVoicedRoleItem
import com.yuranime.dinz.data.api.OtakOtakuScraper
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorControlCircle
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BirthdayDetailScreen(
    item: BirthdayCharacterItem,
    onBackClick: () -> Unit,
    onNavigateToDetailUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    var detailData by remember { mutableStateOf<BirthdayCharacterDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDescExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun fetchDetail() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = OtakOtakuScraper.getBirthdayDetail(item.detailUrl)
            if (result.isSuccess) {
                detailData = result.getOrNull()
                isLoading = false
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat detail"
                isLoading = false
            }
        }
    }

    LaunchedEffect(item.detailUrl) {
        fetchDetail()
    }

    val context = LocalContext.current
    val effectiveDetail = detailData
    val displayName = effectiveDetail?.name ?: item.name
    val displayKanji = effectiveDetail?.nameKanji ?: item.nameKanji
    val displayImage = effectiveDetail?.imageUrl?.ifBlank { null } ?: item.imageUrl.ifBlank { item.thumbnailUrl }
    val isSeiyuu = item.isSeiyuu || (effectiveDetail?.type == "seiyuu_staff")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Header Banner & Portrait Poster Karakter
            item(key = "header_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    // Blurred Backdrop Cover
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayImage)
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_yuranime)
                            .error(R.drawable.placeholder_yuranime)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    // Dark Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x880B0B0B),
                                        Color(0xD90B0B0B),
                                        ColorBackgroundPrimary
                                    )
                                )
                            )
                    )

                    // Content Container (Poster + Name + Badges)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Poster Portrait
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.2.dp, ColorAccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .background(Color(0xFF14161F))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(displayImage)
                                    .crossfade(true)
                                    .placeholder(R.drawable.placeholder_yuranime)
                                    .error(R.drawable.placeholder_yuranime)
                                    .build(),
                                contentDescription = displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title, Kanji & Badges
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 4.dp)
                        ) {
                            // Category Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSeiyuu) Color(0xFF332711) else Color(0xFF2E1511))
                                        .border(
                                            0.8.dp,
                                            if (isSeiyuu) Color(0xFFFACC15) else ColorAccentPrimary,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSeiyuu) "SEIYUU / STAF" else "KARAKTER ANIME",
                                        color = if (isSeiyuu) Color(0xFFFDE68A) else ColorAccentPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                if (effectiveDetail?.alias?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = effectiveDetail.alias,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Main Name
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                lineHeight = 26.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (displayKanji.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = displayKanji,
                                    color = ColorAccentPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // 2. Loading State
            if (isLoading && effectiveDetail == null) {
                item(key = "loading_indicator") {
                    BirthdayDetailShimmer()
                }
            }

            // 3. Error State with Retry
            if (errorMessage != null && effectiveDetail == null) {
                item(key = "error_retry") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF14161D))
                            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage ?: "Gagal mengambil rincian karakter",
                                color = ColorTextSecondary,
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ColorAccentPrimary)
                                    .clickable { fetchDetail() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Coba Lagi",
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

            // 4. Data Detail Attributes (Kompak, Rapat, Tidak Ada Ruang Kosong Menganga)
            if (effectiveDetail != null && effectiveDetail.attributes.isNotEmpty()) {
                item(key = "section_attributes") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cake,
                                contentDescription = null,
                                tint = ColorAccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Biodata & Karakteristik",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            effectiveDetail.attributes.forEach { attr ->
                                AttributeChip(attr = attr)
                            }
                        }
                    }
                }
            }

            // 5. Deskripsi Karakter / Biografi
            if (effectiveDetail != null && effectiveDetail.description.isNotBlank()) {
                item(key = "section_description") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Deskripsi",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF14161D))
                                .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = effectiveDetail.description,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.sp,
                                    fontFamily = SfProFontFamily,
                                    lineHeight = 19.sp,
                                    maxLines = if (isDescExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (effectiveDetail.description.length > 180) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isDescExpanded) "Sembunyikan" else "Baca Selengkapnya...",
                                        color = ColorAccentPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily,
                                        modifier = Modifier
                                            .clickable { isDescExpanded = !isDescExpanded }
                                            .padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Pengisi Suara (Seiyu)
            if (effectiveDetail != null && effectiveDetail.voiceActors.isNotEmpty()) {
                item(key = "section_voice_actors_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pengisi Suara (Seiyu)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                items(
                    items = effectiveDetail.voiceActors,
                    key = { "va_${it.name}_${it.detailUrl}" }
                ) { va ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        VoiceActorCard(
                            va = va,
                            onClick = {
                                if (va.detailUrl.isNotBlank()) {
                                    onNavigateToDetailUrl(va.detailUrl)
                                }
                            }
                        )
                    }
                }
            }

            // 7. Jika Tokoh ini adalah Seiyuu: Tampilkan Karakter yang Disuarakan
            if (effectiveDetail != null && effectiveDetail.voicedRoles.isNotEmpty()) {
                item(key = "section_voiced_roles_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Karakter yang Pernah Disuarakan",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                items(
                    items = effectiveDetail.voicedRoles,
                    key = { "role_${it.characterName}_${it.animeTitle}" }
                ) { role ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        VoicedRoleCard(role = role)
                    }
                }
            }

            // 8. Daftar Anime Terkait (Dengan Poster, Judul, Peran, dan Status)
            if (effectiveDetail != null && effectiveDetail.animeList.isNotEmpty()) {
                item(key = "section_anime_list_header") {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = ColorAccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daftar Anime Terkait",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF161820))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${effectiveDetail.animeList.size} Judul",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }

                items(
                    items = effectiveDetail.animeList,
                    key = { "anime_${it.title}_${it.animeUrl}" }
                ) { anime ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        AnimeAppearanceCard(anime = anime)
                    }
                }
            }
        }

        // Top Navigation Floating Bar (Back Button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp) // Touch target >= 48dp (R-03 / R-39)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0E0F14))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Beranda",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributeChip(attr: BirthdayProfileAttribute) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF14161D))
            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${attr.label}: ",
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
        Text(
            text = attr.value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun VoiceActorCard(
    va: BirthdayVoiceActorItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161D))
            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(va.imageUrl)
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime)
                .error(R.drawable.placeholder_yuranime)
                .build(),
            contentDescription = va.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B1E28))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = va.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Bahasa: ${va.language}",
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = SfProFontFamily
            )
        }
    }
}

@Composable
private fun AnimeAppearanceCard(
    anime: BirthdayAnimeAppearanceItem
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161D))
            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(anime.imageUrl)
                .crossfade(true)
                .placeholder(R.drawable.placeholder_yuranime)
                .error(R.drawable.placeholder_yuranime)
                .build(),
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(48.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1B1E28))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = anime.title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (anime.role.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorAccentPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = anime.role,
                            color = ColorAccentPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                if (anime.status.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = anime.status,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicedRoleCard(
    role: BirthdayVoicedRoleItem
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14161D))
            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (role.characterImageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(role.characterImageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime)
                    .error(R.drawable.placeholder_yuranime)
                    .build(),
                contentDescription = role.characterName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(44.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B1E28))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role.characterName,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily
            )
            if (role.role.isNotBlank()) {
                Text(
                    text = role.role,
                    color = ColorAccentPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
            }
            if (role.animeTitle.isNotBlank()) {
                Text(
                    text = "Anime: ${role.animeTitle}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BirthdayDetailShimmer() {
    val transition = rememberInfiniteTransition(label = "detailShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "detailShimmerOffset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF15171F), Color(0xFF222531), Color(0xFF15171F)),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 100f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
        )
    }
}
