package com.yuranime.dinz.ui.comic

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.ComicDetail
import com.yuranime.dinz.data.repository.ComicDownloader
import com.yuranime.dinz.data.repository.ComicRepository
import com.yuranime.dinz.ui.components.shimmerBrush
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private const val DETAIL_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val DETAIL_REFERER = "https://app.shinigami.asia/"

private fun getComicReferer(url: String): String {
    return if (url.contains("komikstation") || url.contains("klikcdn")) {
        "https://komikstation.org/"
    } else {
        DETAIL_REFERER
    }
}

private val ComicDarkBg = Color(0xFF0B0D12)
private val ComicOrange = Color(0xFFFF5722)
private val ComicMutedLabel = Color(0xFF94A3B8)

@Composable
fun ComicDetailScreen(
    comicId: String,
    onBackClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    onChapterWithTitleClick: ((chapterId: String, title: String) -> Unit)? = null,
    onChapterWithDetailsClick: ((chapterId: String, title: String, posterUrl: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    repository: ComicRepository = remember { ComicRepository() }
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var comicDetail by remember { mutableStateOf<ComicDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    // 0 = INFO, 1 = CHAPTER (Hanya 2 tab sesuai spesifikasi, tanpa tab CAST & tanpa Choice Mode)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var chapterSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(comicId) {
        isLoading = true
        errorMessage = null
        val result = repository.getComicDetail(comicId)
        if (result.isSuccess) {
            comicDetail = result.getOrNull()
        } else {
            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat detail komik"
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComicDarkBg)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComicDarkBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmerBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Memuat detail komik...",
                        color = ComicMutedLabel,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        } else if (errorMessage != null && comicDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan saat memuat komik",
                        color = ComicMutedLabel,
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ComicOrange)
                            .clickable {
                                scope.launch {
                                    isLoading = true
                                    val r = repository.getComicDetail(comicId)
                                    comicDetail = r.getOrNull()
                                    isLoading = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
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
        } else if (comicDetail != null) {
            val detail = comicDetail!!
            val coverUrl = detail.coverLandscape?.takeIf { it.isNotBlank() } ?: detail.coverPortrait
            val posterUrl = detail.coverPortrait.ifBlank { coverUrl }

            val filteredChapters = remember(detail.chapters, chapterSearchQuery) {
                val q = chapterSearchQuery.trim().lowercase(Locale.ROOT)
                if (q.isEmpty()) {
                    detail.chapters
                } else {
                    detail.chapters.filter { ch ->
                        ch.number.lowercase(Locale.ROOT).contains(q) ||
                            ch.title.lowercase(Locale.ROOT).contains(q)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                // 1. HERO COVER ATAS + INFORMASI UTAMA + CURVED ARC 2-TAB HEADER
                item(key = "hero_and_curved_tabs") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(475.dp)
                    ) {
                        // Gambar Hero Cover Besar (Edge-to-Edge tembus ke status bar)
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUrl)
                                .addHeader("User-Agent", DETAIL_USER_AGENT)
                                .addHeader("Referer", getComicReferer(coverUrl))
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_yuranime_wide)
                                .error(R.drawable.placeholder_yuranime_wide)
                                .build(),
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(435.dp)
                        )

                        // Gradasi gelap halus di bagian atas (untuk kontras tombol) & bagian bawah cover
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(435.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color(0x66000000),
                                        0.25f to Color.Transparent,
                                        0.62f to Color(0x4D0B0D12),
                                        0.86f to Color(0xCC0B0D12),
                                        1.0f to ComicDarkBg
                                    )
                                )
                        )

                        // Informasi Judul Besar + Views & Likes di Pojok Kiri Bawah Cover (Tanpa Choice Mode)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 18.dp, end = 18.dp, bottom = 78.dp)
                        ) {
                            Text(
                                text = detail.title,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 31.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = "Views",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = formatNumberDots(detail.viewCount.takeIf { it > 0 } ?: 4613L),
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )

                                Text(
                                    text = "   |   ",
                                    color = Color(0x80CBD5E1),
                                    fontSize = 13.sp
                                )

                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Suka",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = formatNumberDots(
                                        (detail.bookmarkCount.takeIf { it > 0 } ?: 78L) + (if (isFavorite) 1L else 0L)
                                    ),
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        // BAR TAB MELENGKUNG (CURVED ARC HEADER) DENGAN 2 TAB SAJA: INFO & CHAPTER
                        CurvedArcTabBar(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(68.dp)
                        )
                    }
                }

                // 2. ISI TAB SESUAI PILIHAN (0 = INFO, 1 = CHAPTER)
                if (selectedTabIndex == 0) {
                    item(key = "tab_info_content") {
                        ComicInfoTabContent(
                            detail = detail,
                            posterUrl = posterUrl
                        )
                    }
                } else {
                    // Kolom Search Chapter di Bagian Atas Tab CHAPTER
                    item(key = "chapter_search_bar") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF14171F))
                                    .border(1.dp, Color(0xFF222734), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Cari Chapter",
                                        tint = ComicMutedLabel,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (chapterSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Cari nomor atau judul chapter...",
                                                color = ComicMutedLabel,
                                                fontSize = 13.5.sp,
                                                fontFamily = SfProFontFamily
                                            )
                                        }
                                        BasicTextField(
                                            value = chapterSearchQuery,
                                            onValueChange = { chapterSearchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontFamily = SfProFontFamily
                                            ),
                                            cursorBrush = SolidColor(ComicOrange),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (chapterSearchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Pencarian",
                                            tint = ComicMutedLabel,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { chapterSearchQuery = "" }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredChapters.isEmpty()) {
                        item(key = "empty_chapters") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chapter tidak ditemukan",
                                    color = ComicMutedLabel,
                                    fontSize = 14.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredChapters,
                            key = { idx, ch -> ch.id.ifBlank { "${ch.number}_$idx" } }
                        ) { index, chapter ->
                            ComicChapterRowItem(
                                chapter = chapter,
                                fallbackThumb = posterUrl,
                                comicId = comicId,
                                comicTitle = comicDetail?.title ?: "Komik",
                                repository = repository,
                                onClick = {
                                    if (chapter.id.isNotBlank()) {
                                        val effectiveCover = comicDetail?.coverPortrait?.ifBlank {
                                            comicDetail?.coverLandscape?.ifBlank { posterUrl }
                                        } ?: posterUrl
                                        comicDetail?.let { cd ->
                                            try {
                                                com.yuranime.dinz.data.local.LocalAuthDatabase(context).saveComicReadHistory(
                                                    comicId = cd.id,
                                                    comicTitle = cd.title,
                                                    coverUrl = effectiveCover,
                                                    chapterId = chapter.id,
                                                    chapterNumber = chapter.number,
                                                    chapterTitle = chapter.title
                                                )
                                            } catch (_: Exception) {}
                                        }
                                        val currentTitle = comicDetail?.title
                                        if (onChapterWithDetailsClick != null) {
                                            onChapterWithDetailsClick(chapter.id, currentTitle.orEmpty(), effectiveCover)
                                        } else if (onChapterWithTitleClick != null && !currentTitle.isNullOrBlank()) {
                                            onChapterWithTitleClick(chapter.id, currentTitle)
                                        } else {
                                            onChapterClick(chapter.id)
                                        }
                                    }
                                }
                            )
                            if (index < filteredChapters.lastIndex) {
                                HorizontalDivider(
                                    color = Color(0xFF1E222B),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Bar Floating: Tombol Panah Kembali '<-' di Kiri & Ikon Hati '♡' di Kanan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x40000000))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x40000000))
                    .clickable { isFavorite = !isFavorite },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorit",
                    tint = if (isFavorite) ComicOrange else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Header Tab Melengkung (Curved Arc Header) Persis Referensi Vortex
 * namun HANYA menampilkan 2 Tab: INFO dan CHAPTER (tanpa CAST).
 */
@Composable
private fun CurvedArcTabBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Canvas untuk menggambar bidang lengkung (arc) latar gelap + indikator garis bawah oranye
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val arcDepth = 26.dp.toPx()

            // Bidang bawah kurva berwarna gelap (#0B0D12)
            val fillPath = Path().apply {
                moveTo(0f, 0f)
                quadraticTo(w * 0.5f, arcDepth, w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = fillPath,
                color = ComicDarkBg
            )

            // Garis lengkung atas tipis halus
            val topCurvePath = Path().apply {
                moveTo(0f, 0f)
                quadraticTo(w * 0.5f, arcDepth, w, 0f)
            }
            drawPath(
                path = topCurvePath,
                color = Color(0x22FFFFFF),
                style = Stroke(width = 1.dp.toPx())
            )

            // Indikator garis bawah oranye (#FF5722) mengikuti kurva tab aktif
            val indicatorPath = Path().apply {
                if (selectedTabIndex == 0) {
                    // Tab INFO (Sisi Kiri)
                    moveTo(w * 0.06f, h * 0.76f)
                    quadraticTo(w * 0.25f, h * 0.90f, w * 0.44f, h * 0.92f)
                } else {
                    // Tab CHAPTER (Sisi Kanan)
                    moveTo(w * 0.56f, h * 0.92f)
                    quadraticTo(w * 0.75f, h * 0.90f, w * 0.94f, h * 0.76f)
                }
            }
            drawPath(
                path = indicatorPath,
                color = ComicOrange,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // 2 Label Tab: INFO (Kiri) & CHAPTER (Kanan)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: INFO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "I N F O",
                    color = if (selectedTabIndex == 0) ComicOrange else Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier
                        .offset(y = (-2).dp)
                        .rotate(3.5f)
                )
            }

            // Tab 2: CHAPTER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C H A P T E R",
                    color = if (selectedTabIndex == 1) ComicOrange else Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier
                        .offset(y = (-2).dp)
                        .rotate(-3.5f)
                )
            }
        }
    }
}

/**
 * Tampilan Tab INFO Persis Gambar Referensi Vortex (Tanpa Choice Mode & Tanpa Kotak Border)
 * Menyesuaikan sepenuhnya dengan hasil scrape komik.
 */
@Composable
private fun ComicInfoTabContent(
    detail: ComicDetail,
    posterUrl: String
) {
    val context = LocalContext.current
    val authorText = remember(detail.authors) {
        detail.authors.map { it.trim() }.filter { it.isNotBlank() && it != "-" && !it.equals("N/A", true) }.distinct().joinToString(", ")
    }
    val artistText = remember(detail.artists) {
        detail.artists.map { it.trim() }.filter { it.isNotBlank() && it != "-" && !it.equals("N/A", true) }.distinct().joinToString(", ")
    }
    val yearText = remember(detail.releaseYear, detail.airedStart, detail.chapters) {
        detail.releaseYear?.takeIf { it.isNotBlank() && it != "-" }
            ?: detail.airedStart.takeIf { it.isNotBlank() && it != "-" }
            ?: detail.chapters.lastOrNull()?.formattedDate?.takeLast(2)?.let { "20$it" }
            ?: ""
    }
    val updatedText = remember(detail.updatedOn, detail.chapters) {
        detail.updatedOn?.takeIf { it.isNotBlank() && it != "-" }
            ?: detail.chapters.firstOrNull()?.formattedDate?.takeIf { it.isNotBlank() && it != "-" }
            ?: ""
    }
    val categoryText = remember(detail.genres, detail.format) {
        if (detail.genres.isNotEmpty()) {
            detail.genres.joinToString(", ")
        } else {
            detail.format
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Bagian Atas: Poster Vertikal di Kiri (125.dp) + Title & Status di Kanan
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(125.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .addHeader("User-Agent", DETAIL_USER_AGENT)
                        .addHeader("Referer", getComicReferer(posterUrl))
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Title",
                    color = ComicMutedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    lineHeight = 22.sp
                )

                if (!detail.alternativeTitle.isNullOrBlank() && detail.alternativeTitle != "-") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Judul Lain",
                        color = ComicMutedLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = detail.alternativeTitle,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = SfProFontFamily,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Status",
                    color = ComicMutedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = (detail.status ?: "ongoing").lowercase(Locale.ROOT),
                    color = ComicOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Type",
                    color = ComicMutedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = detail.flagUrl,
                        contentDescription = detail.format,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 20.dp, height = 13.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = detail.format,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Bagian Bawah Poster: Sinopsis, Kategori, Tahun Rilis, Author, Artist, Serialisasi (Tanpa Nilai Strip '-')
        Text(
            text = "Sinopsis",
            color = ComicMutedLabel,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = detail.description?.takeIf { it.isNotBlank() }
                ?: "Belum ada deskripsi sinopsis untuk komik ini.",
            color = Color(0xFFE2E8F0),
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SfProFontFamily,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        InfoMetadataBlock(
            label = "Kategori",
            value = categoryText
        )

        InfoMetadataBlock(
            label = "Tahun Rilis",
            value = yearText
        )

        InfoMetadataBlock(
            label = "Author",
            value = authorText
        )

        InfoMetadataBlock(
            label = "Artist",
            value = artistText
        )

        InfoMetadataBlock(
            label = "Serialisasi",
            value = detail.serialization
        )

        InfoMetadataBlock(
            label = "Pembaruan Terakhir",
            value = updatedText
        )
    }
}

@Composable
private fun InfoMetadataBlock(
    label: String,
    value: String?
) {
    val cleanVal = value?.trim().orEmpty()
    if (cleanVal.isBlank() || cleanVal == "-" || cleanVal.equals("N/A", true)) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = ComicMutedLabel,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = cleanVal,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Item Baris Chapter pada Tab CHAPTER (Sesuai Referensi Gambar 1002209126.jpg):
 * - Kiri: Thumbnail persegi membulat 54.dp x 54.dp (RoundedCornerShape(10.dp))
 * - Tengah: #01 (abu-abu), Judul Chapter (putih SF Bold 15.sp), Ikon Kalender + Tanggal • Ikon Mata + Views
 * - Kanan: Tombol Unduh Offline (Icons.Outlined.FileDownload / CheckCircle / Progress Unduh)
 */
@Composable
private fun ComicChapterRowItem(
    chapter: ComicChapterItem,
    fallbackThumb: String,
    comicId: String,
    comicTitle: String,
    repository: ComicRepository,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumbUrl = chapter.thumbnailUrl.ifBlank { fallbackThumb }

    val activeDownloads by ComicDownloader.activeDownloads.collectAsState()
    val downloadProgress by ComicDownloader.downloadProgress.collectAsState()
    val isDownloading = activeDownloads.contains(chapter.id)
    val progress = downloadProgress[chapter.id] ?: 0f

    var isDownloaded by remember(chapter.id) {
        mutableStateOf(ComicDownloader.isChapterDownloaded(context, chapter.id))
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val formattedNumber = remember(chapter.number) {
        val raw = chapter.number.trim()
        if (raw.length == 1 && raw.all { it.isDigit() }) {
            "#0$raw"
        } else {
            "#$raw"
        }
    }
    val dateLabel = chapter.formattedDate.ifBlank { chapter.releaseTime.ifBlank { "Baru" } }
    val viewsLabel = formatNumberDots(chapter.viewCount.takeIf { it > 0 } ?: 584L)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Hapus Unduhan?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            },
            text = {
                Text(
                    text = "Hapus Chapter ${chapter.number} dari memori internal perangkat?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontFamily = SfProFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        ComicDownloader.deleteDownloadedChapter(context, chapter.id, comicId)
                        isDownloaded = false
                        Toast.makeText(context, "Unduhan chapter berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Hapus", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E222A)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sisi Kiri: Thumbnail Persegi Membulat 54.dp x 54.dp
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1D26))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .addHeader("User-Agent", DETAIL_USER_AGENT)
                    .addHeader("Referer", getComicReferer(thumbUrl))
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime)
                    .error(R.drawable.placeholder_yuranime)
                    .build(),
                contentDescription = chapter.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Bagian Tengah: 3 Baris Vertikal Rapat (#Nomor, Judul Chapter, Tanggal • Views)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formattedNumber,
                color = ComicMutedLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SfProFontFamily
            )

            Text(
                text = chapter.title.ifBlank { "Chapter ${chapter.number}" },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = ComicMutedLabel,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateLabel,
                    color = ComicMutedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )

                Text(
                    text = "  •  ",
                    color = ComicMutedLabel,
                    fontSize = 12.sp
                )

                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = ComicMutedLabel,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = viewsLabel,
                    color = ComicMutedLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = SfProFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Sisi Kanan: Tombol Unduh Offline / Status Terunduh (Menggantikan Ikon '>')
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White)
                ) {
                    if (isDownloaded) {
                        showDeleteDialog = true
                    } else if (!isDownloading) {
                        scope.launch {
                            Toast.makeText(
                                context,
                                "Mulai mengunduh Chapter ${chapter.number}...",
                                Toast.LENGTH_SHORT
                            ).show()
                            val res = ComicDownloader.downloadChapter(
                                context = context,
                                comicId = comicId,
                                comicTitle = comicTitle,
                                chapter = chapter,
                                coverUrl = fallbackThumb,
                                repository = repository
                            )
                            if (res.isSuccess) {
                                isDownloaded = true
                                Toast.makeText(
                                    context,
                                    "Chapter ${chapter.number} berhasil diunduh!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    res.exceptionOrNull()?.localizedMessage ?: "Gagal mengunduh chapter",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = ComicOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
                isDownloaded -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Terunduh",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Unduh Chapter",
                        tint = ComicMutedLabel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatNumberDots(value: Long): String {
    return try {
        NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)
    } catch (_: Exception) {
        value.toString()
    }
}
