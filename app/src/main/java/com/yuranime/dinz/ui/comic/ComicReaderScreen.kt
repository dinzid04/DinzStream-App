package com.yuranime.dinz.ui.comic

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.ComicChapterDetail
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.ComicDetail
import com.yuranime.dinz.data.repository.ComicDownloader
import com.yuranime.dinz.data.repository.ComicRepository
import com.yuranime.dinz.ui.components.shimmerBrush
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val ReaderDarkOverlay = Color(0xD9121316)
private val ReaderBtnDarkBg = Color(0xD916181D)
private val ReaderBorderColor = Color(0x26FFFFFF)
private val ReaderAccentOrange = Color(0xFFFF5722)
private val ReaderSheetBg = Color(0xFF121316)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicReaderScreen(
    chapterId: String,
    comicId: String? = null,
    comicTitle: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    repository: ComicRepository = remember { ComicRepository() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDb = remember { LocalAuthDatabase(context) }

    var currentChapterId by remember { mutableStateOf(chapterId) }
    var effectiveComicId by remember { mutableStateOf(comicId.orEmpty()) }
    var effectiveTitle by remember { mutableStateOf(comicTitle.orEmpty()) }

    var chapterDetail by remember { mutableStateOf<ComicChapterDetail?>(null) }
    var fullComicDetail by remember { mutableStateOf<ComicDetail?>(null) }
    var offlinePageFiles by remember { mutableStateOf<List<File>?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }

    // Bottom Sheets State
    var showChapterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Settings
    var readMode by remember { mutableStateOf("webtoon") } // "webtoon" | "page"
    var imageQuality by remember { mutableStateOf("tinggi") } // "tinggi" | "rendah"
    var screenBrightness by remember { mutableFloatStateOf(0.7f) }
    var isAutoScrollActive by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableStateOf("1x") } // 1x, 1.5x, 2x, 3x, 4x, 5x

    val listState = rememberLazyListState()
    val totalPagesCount = remember(chapterDetail, offlinePageFiles) {
        offlinePageFiles?.size ?: chapterDetail?.pages?.size ?: 0
    }
    val pagerState = rememberPagerState(pageCount = { totalPagesCount.coerceAtLeast(1) })

    // Restore Brightness on Exit
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        onDispose {
            activity?.window?.let { w ->
                val lp = w.attributes
                lp.screenBrightness = originalBrightness
                w.attributes = lp
            }
        }
    }

    // Apply Screen Brightness Realtime
    LaunchedEffect(screenBrightness) {
        val activity = context as? Activity
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness = screenBrightness.coerceIn(0.05f, 1.0f)
            w.attributes = lp
        }
    }

    // Auto-Scroll Worker (Webtoon Mode Only)
    LaunchedEffect(isAutoScrollActive, autoScrollSpeed, readMode) {
        if (isAutoScrollActive && readMode == "webtoon") {
            val delta = when (autoScrollSpeed) {
                "1x" -> 2.5f
                "1.5x" -> 4f
                "2x" -> 6f
                "3x" -> 9f
                "4x" -> 13f
                "5x" -> 18f
                else -> 2.5f
            }
            while (isAutoScrollActive && readMode == "webtoon") {
                listState.scrollBy(delta)
                delay(16L) // ~60 FPS
            }
        }
    }

    // Load Chapter Function (Online or Offline)
    fun loadChapter(targetId: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            currentChapterId = targetId
            isAutoScrollActive = false

            // 1. Cek Offline Files
            val offline = ComicDownloader.getOfflinePages(context, effectiveComicId, targetId)
            if (!offline.isNullOrEmpty()) {
                offlinePageFiles = offline
                val dummyDetail = ComicChapterDetail(
                    chapterId = targetId,
                    comicId = effectiveComicId,
                    chapterNumber = targetId.substringAfterLast("-").ifBlank { "1" },
                    chapterTitle = "Chapter ${targetId.substringAfterLast("-")}",
                    prevChapterId = null,
                    nextChapterId = null,
                    pages = offline.map { it.absolutePath }
                )
                chapterDetail = dummyDetail
                listState.scrollToItem(0)
                pagerState.scrollToPage(0)
                isLoading = false
            } else {
                offlinePageFiles = null
                val result = repository.getChapterDetail(targetId)
                if (result.isSuccess) {
                    val detail = result.getOrNull()
                    chapterDetail = detail
                    if (detail != null) {
                        if (effectiveComicId.isBlank()) {
                            effectiveComicId = detail.comicId
                        }
                    }
                    listState.scrollToItem(0)
                    pagerState.scrollToPage(0)
                } else {
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat halaman baca komik"
                }
                isLoading = false
            }

            // Muat comic metadata untuk daftar chapter sheet
            if (effectiveComicId.isNotBlank() && fullComicDetail == null) {
                val detailResult = repository.getComicDetail(effectiveComicId)
                if (detailResult.isSuccess) {
                    val cd = detailResult.getOrNull()
                    fullComicDetail = cd
                    if (effectiveTitle.isBlank() && cd != null) {
                        effectiveTitle = cd.title
                    }
                }
            }
        }
    }

    LaunchedEffect(chapterId) {
        loadChapter(chapterId)
    }

    // Auto-Save Reading History Realtime
    val currentReadingPage by remember(readMode, listState.firstVisibleItemIndex, pagerState.currentPage) {
        derivedStateOf {
            if (readMode == "webtoon") {
                (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPagesCount.coerceAtLeast(1))
            } else {
                (pagerState.currentPage + 1).coerceAtMost(totalPagesCount.coerceAtLeast(1))
            }
        }
    }

    LaunchedEffect(currentChapterId, currentReadingPage) {
        if (effectiveComicId.isNotBlank() && chapterDetail != null) {
            val cd = chapterDetail!!
            localDb.saveComicReadHistory(
                comicId = effectiveComicId,
                comicTitle = effectiveTitle.ifBlank { "Komik" },
                coverUrl = fullComicDetail?.coverPortrait ?: "",
                chapterId = currentChapterId,
                chapterNumber = cd.chapterNumber,
                chapterTitle = cd.chapterTitle.orEmpty(),
                pageIndex = currentReadingPage - 1,
                scrollOffset = if (readMode == "webtoon") listState.firstVisibleItemScrollOffset else 0
            )
        }
    }

    BackHandler { onBackClick() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    areControlsVisible = !areControlsVisible
                }
            }
    ) {
        // ==================== KONTEN BACA (WEBTOON / PER HALAMAN) ====================
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0D12)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmerBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Memuat halaman komik...",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        } else if (errorMessage != null && chapterDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ReaderAccentOrange)
                            .clickable { loadChapter(currentChapterId) }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
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
        } else if (chapterDetail != null) {
            val detail = chapterDetail!!
            val pages = if (!offlinePageFiles.isNullOrEmpty()) {
                offlinePageFiles!!.map { it.absolutePath }
            } else {
                detail.pages
            }

            if (readMode == "webtoon") {
                // Mode 1: Webtoon Vertical LazyColumn Tanpa Sela
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(
                        items = pages,
                        key = { idx, url -> "$idx-$url" }
                    ) { index, pageSource ->
                        ComicImageItem(
                            pageSource = pageSource,
                            pageIndex = index,
                            imageQuality = imageQuality
                        )
                    }

                    // Footer Akhir Chapter
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Akhir Chapter ${detail.chapterNumber}",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily
                            )

                            if (!detail.nextChapterId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(ReaderAccentOrange)
                                        .clickable { loadChapter(detail.nextChapterId) }
                                        .padding(horizontal = 22.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Lanjut Chapter Berikutnya ▶",
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
            } else {
                // Mode 2: Per Halaman (HorizontalPager dengan Pinch-to-Zoom)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pageSource = pages.getOrNull(pageIndex).orEmpty()
                    PinchZoomablePage(
                        pageSource = pageSource,
                        pageIndex = pageIndex,
                        imageQuality = imageQuality
                    )
                }
            }
        }

        // ==================== TOP HEADER BAR ====================
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ReaderDarkOverlay)
                        .border(0.8.dp, ReaderBorderColor, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tombol Kembali '<-' & Judul Komik
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White)
                                ) { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val displayTitle = effectiveTitle.ifBlank {
                            chapterDetail?.chapterTitle ?: "Baca Komik"
                        }
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Kapsul Kanan: Ch. [Nomor] ⌄
                    val currentChNumber = chapterDetail?.chapterNumber ?: "1"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x26FFFFFF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { showChapterSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Ch. $currentChNumber ⌄",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
            }
        }

        // ==================== BOTTOM CONTROL AREA ====================
        AnimatedVisibility(
            visible = areControlsVisible && chapterDetail != null,
            enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            val detail = chapterDetail ?: return@AnimatedVisibility

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. BARIS ATAS: HANYA 3 TOMBOL BULAT (Pengaturan, Auto-Scroll, Daftar Chapter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tombol Kiri (Pengaturan Pembaca '⊶')
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ReaderBtnDarkBg)
                            .border(0.8.dp, ReaderBorderColor, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { showSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Pengaturan Pembaca",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Tombol Tengah (Play / Pause Auto-Scroll Langsung)
                    val isAutoScrollModeActive = isAutoScrollActive && readMode == "webtoon"
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isAutoScrollModeActive) Color.White else ReaderBtnDarkBg)
                            .border(
                                0.8.dp,
                                if (isAutoScrollModeActive) Color.White else ReaderBorderColor,
                                CircleShape
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                if (readMode == "webtoon") {
                                    isAutoScrollActive = !isAutoScrollActive
                                } else {
                                    readMode = "webtoon"
                                    isAutoScrollActive = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAutoScrollModeActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Auto Scroll",
                            tint = if (isAutoScrollModeActive) Color.Black else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    // Tombol Kanan (Daftar Chapter '☰')
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ReaderBtnDarkBg)
                            .border(0.8.dp, ReaderBorderColor, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) { showChapterSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Daftar Chapter",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. BARIS BAWAH: NAVIGASI PREV + SLIDER HALAMAN + NEXT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Bulat '<' (Chapter Sebelumnya)
                    val hasPrev = !detail.prevChapterId.isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ReaderBtnDarkBg)
                            .border(0.8.dp, ReaderBorderColor, CircleShape)
                            .clickable(
                                enabled = hasPrev,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                detail.prevChapterId?.let { loadChapter(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Chapter Sebelumnya",
                            tint = if (hasPrev) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Kapsul Tengah Panjang (Nomor Halaman + Slider + Total)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ReaderBtnDarkBg)
                            .border(0.8.dp, ReaderBorderColor, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$currentReadingPage",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = SfProFontFamily,
                                modifier = Modifier.width(24.dp)
                            )

                            val maxPages = totalPagesCount.coerceAtLeast(1)
                            Slider(
                                value = currentReadingPage.toFloat().coerceIn(1f, maxPages.toFloat()),
                                onValueChange = { newVal ->
                                    val targetPage = newVal.toInt().coerceIn(1, maxPages)
                                    scope.launch {
                                        if (readMode == "webtoon") {
                                            listState.scrollToItem(targetPage - 1)
                                        } else {
                                            pagerState.scrollToPage(targetPage - 1)
                                        }
                                    }
                                },
                                valueRange = 1f..maxPages.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            )

                            Text(
                                text = "$maxPages",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = SfProFontFamily,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }

                    // Tombol Bulat '>' (Chapter Selanjutnya)
                    val hasNext = !detail.nextChapterId.isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ReaderBtnDarkBg)
                            .border(0.8.dp, ReaderBorderColor, CircleShape)
                            .clickable(
                                enabled = hasNext,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                detail.nextChapterId?.let { loadChapter(it) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Chapter Selanjutnya",
                            tint = if (hasNext) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ==================== BOTTOM SHEET 1: DAFTAR CHAPTER ====================
        if (showChapterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = ReaderSheetBg,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF374151))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Chapter",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val chapters = fullComicDetail?.chapters ?: emptyList()
                    if (chapters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Memuat daftar chapter...",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontFamily = SfProFontFamily
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(chapters, key = { it.id }) { ch ->
                                val isCurrent = ch.id == currentChapterId || ch.number == chapterDetail?.chapterNumber
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) Color(0x26FF5722) else Color(0x0FFFFFFF))
                                        .clickable {
                                            showChapterSheet = false
                                            loadChapter(ch.id)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail Rounded 48.dp x 48.dp
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E222B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val thumb = ch.thumbnailUrl.ifBlank { fullComicDetail?.coverPortrait.orEmpty() }
                                        if (thumb.isNotBlank()) {
                                            SubcomposeAsyncImage(
                                                model = thumb,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize().background(shimmerBrush()))
                                                },
                                                error = {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.2f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ch. ${ch.number}",
                                            color = if (isCurrent) ReaderAccentOrange else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val dateLabel = ch.formattedDate.ifBlank { ch.releaseTime.ifBlank { "Baru" } }
                                        Text(
                                            text = "$dateLabel • ${ch.viewCount.coerceAtLeast(124)} dilihat",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==================== BOTTOM SHEET 2: PENGATURAN PEMBACA ====================
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = ReaderSheetBg,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF374151))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Pengaturan pembaca",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    // 1. Mode Baca (Webtoon vs Per halaman)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Mode baca",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E222A))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Webtoon Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (readMode == "webtoon") Color(0xFF383C45) else Color.Transparent)
                                    .clickable {
                                        readMode = "webtoon"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.ViewStream,
                                        contentDescription = null,
                                        tint = if (readMode == "webtoon") Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Webtoon",
                                        color = if (readMode == "webtoon") Color.White else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            // Per Halaman Pill
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (readMode == "page") Color(0xFF383C45) else Color.Transparent)
                                    .clickable {
                                        readMode = "page"
                                        isAutoScrollActive = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = if (readMode == "page") Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Per halaman",
                                        color = if (readMode == "page") Color.White else Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }

                    // 2. Kualitas Gambar (Tinggi vs Rendah)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Kualitas gambar",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E222A))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (imageQuality == "tinggi") Color(0xFF383C45) else Color.Transparent)
                                    .clickable { imageQuality = "tinggi" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tinggi",
                                    color = if (imageQuality == "tinggi") Color.White else Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (imageQuality == "rendah") Color(0xFF383C45) else Color.Transparent)
                                    .clickable { imageQuality = "rendah" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Rendah",
                                    color = if (imageQuality == "rendah") Color.White else Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        Text(
                            text = "Kualitas rendah lebih cepat dimuat dan hemat kuota.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
                        )
                    }

                    // 3. Kecerahan (Sinkron Layar HP)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Kecerahan",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = "Redup",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )

                            Slider(
                                value = screenBrightness,
                                onValueChange = { screenBrightness = it },
                                valueRange = 0.05f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            )

                            Icon(
                                imageVector = Icons.Outlined.LightMode,
                                contentDescription = "Terang",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 4. Kecepatan Gulir Otomatis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E222A))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kecepatan gulir otomatis",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                                Text(
                                    text = "Hanya di mode webtoon",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        // Tombol Kapsul 1x, 1.5x, 2x, 3x, 4x, 5x
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x33FFFFFF))
                                .clickable {
                                    autoScrollSpeed = when (autoScrollSpeed) {
                                        "1x" -> "1.5x"
                                        "1.5x" -> "2x"
                                        "2x" -> "3x"
                                        "3x" -> "4x"
                                        "4x" -> "5x"
                                        else -> "1x"
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = autoScrollSpeed,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Item Gambar Webtoon dengan Skeleton Shimmer & Tanpa Circular Loading
 */
@Composable
private fun ComicImageItem(
    pageSource: String,
    pageIndex: Int,
    imageQuality: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLocalFile = pageSource.startsWith("/") || pageSource.startsWith("file://")

    val imageModel = remember(pageSource, imageQuality) {
        val req = ImageRequest.Builder(context)
            .data(if (isLocalFile) File(pageSource) else pageSource)
            .crossfade(true)

        if (imageQuality == "tinggi") {
            req.size(Size.ORIGINAL)
        } else {
            req.size(800)
        }

        if (!isLocalFile) {
            val referer = if (pageSource.contains("komikstation") || pageSource.contains("klikcdn")) {
                "https://komikstation.org/"
            } else {
                "https://app.shinigami.asia/"
            }
            req.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            req.addHeader("Referer", referer)
        }
        req.build()
    }

    SubcomposeAsyncImage(
        model = imageModel,
        contentDescription = "Halaman ${pageIndex + 1}",
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
        loading = {
            // Skeleton Shimmer + Icon Monokrom (Sesuai R-27 & Anti-Slop, DILARANG Loading Bulat!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(shimmerBrush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(54.dp)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF14151A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gagal memuat halaman ${pageIndex + 1}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }
    )
}

/**
 * Halaman Komik dengan Dukungan Pinch-to-Zoom (Mode Per Halaman)
 */
@Composable
private fun PinchZoomablePage(
    pageSource: String,
    pageIndex: Int,
    imageQuality: String
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 3.5f)
                    if (scale > 1f) {
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.2f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            ComicImageItem(
                pageSource = pageSource,
                pageIndex = pageIndex,
                imageQuality = imageQuality
            )
        }
    }
}
