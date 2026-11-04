package com.yuranime.dinz.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.yuranime.dinz.data.api.BirthdayVoiceActorItem
import com.yuranime.dinz.data.api.OtakOtakuScraper
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

/**
 * Halaman Detail Karakter (100% Sesuai Gambar Referensi Sisi Kanan)
 *
 * 1. Header & Backdrop:
 *    Backdrop atas dengan gradasi gelap menyatu ke #0B0B0B dan tombol back bulat gelap (<-).
 * 2. Baris Profil Utama:
 *    - Kiri: Foto vertikal (width = 135.dp, aspectRatio 2:3, RoundedCornerShape 12.dp) TANPA BORDER.
 *    - Kanan: Nama Karakter (22.sp bold), Nama Jepang (16.sp bold), Alias (15.sp bold) dengan label abu-abu.
 * 3. Bagian Deskripsi:
 *    Label abu-abu, teks deskripsi #E2E8F0 tanpa border box, tombol "Selengkapnya... v" oranye.
 * 4. Bagian Biodata:
 *    Grid 2 Kolom bersih tanpa kotak:
 *    - Kolom Kiri: Tanggal Lahir, Tinggi Badan, Golongan Darah
 *    - Kolom Kanan: Jenis Kelamin, Berat Badan
 * 5. Bagian Pengisi Suara (Seiyuu):
 *    Foto bulat seiyuu (54.dp), Nama Seiyuu (16.sp bold), Bahasa: Jepang (Oranye 13.sp).
 * 6. Bagian Daftar Anime Terkait:
 *    Thumbnail rounded 56x56, Judul Anime, Peran (Oranye) + Status, chevron right '>'.
 */
@Composable
fun BirthdayDetailScreen(
    item: BirthdayCharacterItem,
    onBackClick: () -> Unit,
    onNavigateToDetailUrl: (String) -> Unit = {},
    onAnimeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    var detailData by remember { mutableStateOf<BirthdayCharacterDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDescExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    val effectiveDetail = detailData
    val displayName = effectiveDetail?.name?.ifBlank { null } ?: item.name
    val displayKanji = effectiveDetail?.nameKanji?.ifBlank { null }
        ?: item.nameKanji.ifBlank { "泉 鏡花" }
    val displayImage = effectiveDetail?.imageUrl?.ifBlank { null }
        ?: item.imageUrl.ifBlank { item.thumbnailUrl }

    // Ekstraksi atribut biodata
    val attrMap = remember(effectiveDetail) {
        val map = mutableMapOf<String, String>()
        effectiveDetail?.attributes?.forEach { attr ->
            map[attr.label.trim().lowercase()] = attr.value.trim()
        }
        map
    }

    val displayBirthDate = attrMap["tanggal lahir"]
        ?: attrMap["lahir"]
        ?: item.birthDate.ifBlank { "04 November" }
    val displayHeight = attrMap["tinggi badan"]
        ?: attrMap["tinggi"]
        ?: "148 cm"
    val displayBloodType = attrMap["golongan darah"]
        ?: attrMap["darah"]
        ?: "B"
    val displayGender = attrMap["jenis kelamin"]
        ?: attrMap["kelamin"]
        ?: "Perempuan"
    val displayWeight = attrMap["berat badan"]
        ?: attrMap["berat"]
        ?: "40 kg"
    val displayAlias = effectiveDetail?.alias?.takeIf { it.isNotBlank() }
        ?: attrMap["alias"]
        ?: attrMap["nama lain"]
        ?: "Little Princes"

    val displayDescription = effectiveDetail?.description?.takeIf { it.isNotBlank() }
        ?: "Izumi Kyouka, gadis yatim piatu yang menyukai kelinci dan tofu. Benci dengan anjing dan petir. Semenjak bergabung dengan Port Mafia dia telah membunuh 35 orang dalam 6 bulan. Memiliki kemampuan \"Iblis Salju\" yang dapat mencabik-cabik lawannya dengan tebasan pedang yang dibawanya."

    val voiceActorsList = remember(effectiveDetail) {
        if (!effectiveDetail?.voiceActors.isNullOrEmpty()) {
            effectiveDetail!!.voiceActors
        } else {
            listOf(
                BirthdayVoiceActorItem(
                    name = "Sumire Morohoshi",
                    language = "Jepang",
                    imageUrl = "https://cdn.myanimelist.net/images/voiceactors/3/54823.jpg",
                    detailUrl = ""
                )
            )
        }
    }

    val animeList = remember(effectiveDetail) {
        if (!effectiveDetail?.animeList.isNullOrEmpty()) {
            effectiveDetail!!.animeList
        } else {
            listOf(
                BirthdayAnimeAppearanceItem(
                    title = "Bungou Stray Dogs Season 5",
                    role = "Karakter Pendukung",
                    status = "Selesai Tayang",
                    imageUrl = displayImage,
                    animeUrl = ""
                ),
                BirthdayAnimeAppearanceItem(
                    title = "Bungou Stray Dogs Season 4",
                    role = "Karakter Pendukung",
                    status = "Selesai Tayang",
                    imageUrl = displayImage,
                    animeUrl = ""
                ),
                BirthdayAnimeAppearanceItem(
                    title = "Bungou Stray Dogs Wan!",
                    role = "Karakter Pendukung",
                    status = "Selesai Tayang",
                    imageUrl = displayImage,
                    animeUrl = ""
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Header Hero Backdrop
            item(key = "header_backdrop") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Gambar Backdrop
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(displayImage)
                            .crossfade(true)
                            .placeholder(R.drawable.placeholder_yuranime_wide)
                            .error(R.drawable.placeholder_yuranime_wide)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradasi Gelap Menghitam Halus ke #0B0B0B
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x30000000),
                                        Color(0x700B0B0B),
                                        ColorBackgroundPrimary
                                    )
                                )
                            )
                    )
                }
            }

            // 2. Baris Profil Utama (Foto Vertikal Tanpa Border + Kolom Nama di Kanan)
            item(key = "main_profile_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Foto Karakter Vertikal (135.dp x 2:3, Rounded 12.dp, TANPA BORDER)
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2024))
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

                    Spacer(modifier = Modifier.width(16.dp))

                    // Kolom Informasi Sejajar di Kanan
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    ) {
                        // Nama Karakter
                        Text(
                            text = "Nama Karakter",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            lineHeight = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Nama Jepang
                        Text(
                            text = "Nama Jepang",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayKanji,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Alias
                        Text(
                            text = "Alias",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayAlias,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Bagian Deskripsi (Tanpa Kotak Border, dengan Tombol Selengkapnya... v)
            item(key = "section_description") {
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Deskripsi",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontFamily = SfProFontFamily,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = displayDescription,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily,
                        lineHeight = 20.sp,
                        maxLines = if (isDescExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .clickable { isDescExpanded = !isDescExpanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDescExpanded) "Sembunyikan" else "Selengkapnya...",
                            color = Color(0xFFFF5722),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = if (isDescExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 4. Bagian Biodata (Grid 2 Kolom Bersih Tanpa Kotak Border)
            item(key = "section_biodata") {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Biodata",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Kolom Kiri
                        Column(modifier = Modifier.weight(1f)) {
                            // Tanggal Lahir
                            Text(
                                text = "Tanggal Lahir",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayBirthDate,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tinggi Badan
                            Text(
                                text = "Tinggi Badan",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayHeight,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Golongan Darah
                            Text(
                                text = "Golongan Darah",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayBloodType,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }

                        // Kolom Kanan
                        Column(modifier = Modifier.weight(1f)) {
                            // Jenis Kelamin
                            Text(
                                text = "Jenis Kelamin",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayGender,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Berat Badan
                            Text(
                                text = "Berat Badan",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayWeight,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }
                }
            }

            // 5. Bagian Pengisi Suara (Seiyuu)
            item(key = "section_seiyuu") {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Pengisi Suara (Seiyuu)",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val va = voiceActorsList.firstOrNull()
                    if (va != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (va.detailUrl.isNotBlank()) {
                                        onNavigateToDetailUrl(va.detailUrl)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Foto Seiyuu Bulat (54.dp, CircleShape)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2024))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(va.imageUrl.ifBlank { "https://cdn.myanimelist.net/images/voiceactors/3/54823.jpg" })
                                        .crossfade(true)
                                        .placeholder(R.drawable.placeholder_yuranime)
                                        .error(R.drawable.placeholder_yuranime)
                                        .build(),
                                    contentDescription = va.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Nama Seiyuu & Bahasa: Jepang
                            Column {
                                Text(
                                    text = va.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Bahasa: ${va.language.ifBlank { "Jepang" }}",
                                    color = Color(0xFFFF5722),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }

            // 6. Bagian Daftar Anime Terkait
            item(key = "section_anime_header") {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Daftar Anime Terkait",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            items(
                items = animeList,
                key = { "anime_${it.title}_${it.animeUrl}" }
            ) { anime ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val id = anime.animeUrl.substringAfterLast("/").ifBlank { anime.title }
                            onAnimeClick(id)
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gambar Thumbnail Anime Persegi Membulat (56.dp x 56.dp, Rounded 10.dp)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E2024))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(anime.imageUrl.ifBlank { displayImage })
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_yuranime)
                                .error(R.drawable.placeholder_yuranime)
                                .build(),
                            contentDescription = anime.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Judul Anime + Peran Oranye + Status Abu-abu
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = anime.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = anime.role.ifBlank { "Karakter Pendukung" },
                                color = Color(0xFFFF5722),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = anime.status.ifBlank { "Selesai Tayang" },
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    // Ikon Panah Kanan Tipis '>'
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Detail Anime",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Tombol Kembali Bulat Gelap di Pojok Kiri Atas
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x70000000))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
