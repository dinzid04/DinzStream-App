package com.yuranime.dinz.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.yuranime.dinz.data.repository.StickerItem
import com.yuranime.dinz.data.repository.StickerPack
import com.yuranime.dinz.data.repository.StickerRepository
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StickerPanelMode {
    INLINE_EMOJI,
    LARGE_STICKER
}

private sealed interface StickerGridEntry {
    val sectionKey: String

    data class Header(
        override val sectionKey: String,
        val title: String,
        val subtitle: String = ""
    ) : StickerGridEntry

    data class Cell(
        override val sectionKey: String,
        val item: StickerItem,
        val keySuffix: Int
    ) : StickerGridEntry
}

/**
 * Panel Stiker 2 Mode (Inline Emoji-Sticker 8-kolom 36.dp & Stiker Besar 4-kolom 76.dp),
 * Pencarian Sticker.ly, dan Bar Thumbnail Bawah dengan Sinkronisasi Scroll 2 Arah.
 */
@Composable
fun ChatStickerPanel(
    onInsertInlineSticker: (StickerItem) -> Unit,
    onSendLargeSticker: (StickerItem) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF5722)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val packs by StickerRepository.packs.collectAsState()
    val recentStickers by StickerRepository.recentStickers.collectAsState()
    val isLoadingDefaults by StickerRepository.isLoadingDefaults.collectAsState()

    var mode by remember { mutableStateOf(StickerPanelMode.INLINE_EMOJI) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var isSearchingRemote by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Tahap 2 Pencarian: klik thumbnail paket -> buka preview detail paket & render stiker malas-malasan (lazy)
    var selectedSearchPack by remember { mutableStateOf<StickerPack?>(null) }
    var selectedSearchDetail by remember { mutableStateOf<StickerPack?>(null) }
    var isLoadingPackDetail by remember { mutableStateOf(false) }
    var packDetailError by remember { mutableStateOf<String?>(null) }

    val gridState = rememberLazyGridState()
    val thumbRowState = rememberLazyListState()
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        StickerRepository.initializeAndPreload(context)
    }

    BackHandler(enabled = isSearchMode) {
        if (selectedSearchPack != null) {
            selectedSearchPack = null
            selectedSearchDetail = null
            packDetailError = null
        } else {
            isSearchMode = false
            searchQuery = ""
        }
    }

    LaunchedEffect(searchQuery, isSearchMode) {
        if (!isSearchMode) return@LaunchedEffect
        val q = searchQuery.trim()
        selectedSearchPack = null
        selectedSearchDetail = null
        isLoadingPackDetail = false
        packDetailError = null

        if (q.isEmpty()) {
            searchResults = emptyList()
            isSearchingRemote = false
            searchError = null
            return@LaunchedEffect
        }
        delay(350L)
        isSearchingRemote = true
        searchError = null
        val found = runCatching { StickerRepository.searchPackThumbnails(q) }.getOrElse {
            searchError = "Gagal mencari paket stiker untuk \"$q\"."
            emptyList()
        }
        searchResults = found
        if (found.isEmpty() && searchError == null) {
            searchError = "Paket stiker \"$q\" tidak ditemukan."
        }
        isSearchingRemote = false
    }

    LaunchedEffect(selectedSearchPack) {
        val pack = selectedSearchPack ?: return@LaunchedEffect
        isLoadingPackDetail = true
        packDetailError = null
        selectedSearchDetail = null
        val loaded = runCatching {
            StickerRepository.fetchPackDetail(
                packUrl = pack.sourceUrl,
                fallbackName = pack.name,
                fallbackAuthor = pack.author,
                fallbackThumb = pack.thumbnailUrl
            )
        }.getOrNull()
        if (loaded != null && loaded.stickers.isNotEmpty()) {
            selectedSearchDetail = loaded
        } else {
            packDetailError = "Isi stiker pada paket \"${pack.name}\" gagal dimuat."
        }
        isLoadingPackDetail = false
    }

    val allPreloadedStickers = remember(packs) {
        packs.flatMap { it.stickers }.distinctBy { it.url }
    }

    val gridEntries = remember(
        isSearchMode,
        searchQuery,
        selectedSearchDetail,
        recentStickers,
        packs,
        allPreloadedStickers
    ) {
        val list = mutableListOf<StickerGridEntry>()
        if (isSearchMode) {
            val q = searchQuery.trim()
            if (q.isEmpty()) {
                if (allPreloadedStickers.isNotEmpty()) {
                    list.add(StickerGridEntry.Header("all", "Semua stiker"))
                    allPreloadedStickers.forEachIndexed { idx, stk ->
                        list.add(StickerGridEntry.Cell("all", stk, idx))
                    }
                }
            } else {
                val detail = selectedSearchDetail
                if (detail != null && detail.stickers.isNotEmpty()) {
                    detail.stickers.forEachIndexed { idx, stk ->
                        list.add(StickerGridEntry.Cell("detail_${detail.id}", stk, idx))
                    }
                }
            }
        } else {
            if (recentStickers.isNotEmpty()) {
                list.add(StickerGridEntry.Header("recent", "Terbaru"))
                recentStickers.forEachIndexed { idx, stk ->
                    list.add(StickerGridEntry.Cell("recent", stk, idx))
                }
            }
            packs.forEach { pack ->
                if (pack.stickers.isNotEmpty()) {
                    list.add(
                        StickerGridEntry.Header(
                            sectionKey = pack.id,
                            title = pack.name,
                            subtitle = pack.author
                        )
                    )
                    pack.stickers.forEachIndexed { idx, stk ->
                        list.add(StickerGridEntry.Cell(pack.id, stk, idx))
                    }
                }
            }
        }
        list
    }

    val sectionStartIndexMap = remember(gridEntries) {
        val map = LinkedHashMap<String, Int>()
        gridEntries.forEachIndexed { idx, entry ->
            if (!map.containsKey(entry.sectionKey)) {
                map[entry.sectionKey] = idx
            }
        }
        map
    }

    val visibleSectionKey by remember(gridEntries) {
        derivedStateOf {
            val idx = gridState.firstVisibleItemIndex.coerceIn(0, (gridEntries.size - 1).coerceAtLeast(0))
            gridEntries.getOrNull(idx)?.sectionKey ?: if (recentStickers.isNotEmpty()) "recent" else packs.firstOrNull()?.id.orEmpty()
        }
    }

    val packIndexById = remember(packs) {
        packs.mapIndexed { idx, pack -> pack.id to idx }.toMap()
    }

    // Arah 1 Sinkronisasi: scroll grid otomatis menyorot & menggulir bar thumbnail paket di bawah
    LaunchedEffect(visibleSectionKey, isSearchMode, isProgrammaticScroll) {
        if (!isSearchMode && !isProgrammaticScroll) {
            val packIdx = packIndexById[visibleSectionKey]
            if (packIdx != null && packIdx >= 0 && packIdx < packs.size) {
                thumbRowState.animateScrollToItem(packIdx)
            }
        }
    }

    val columnCount = if (mode == StickerPanelMode.INLINE_EMOJI) 8 else 4
    val itemSize = if (mode == StickerPanelMode.INLINE_EMOJI) 36.dp else 76.dp
    val gridSpacing = if (mode == StickerPanelMode.INLINE_EMOJI) 6.dp else 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(328.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xFF0F1118))
    ) {
        // Handle bar atas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2D3242))
            )
        }

        // Header Panel Stiker: Mode Normal vs Mode Pencarian / Preview Detail Paket
        val activePreviewPack = selectedSearchPack
        if (isSearchMode && activePreviewPack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Kembali ke daftar paket",
                    tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selectedSearchPack = null
                                selectedSearchDetail = null
                                packDetailError = null
                            }
                        )
                        .padding(5.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                StickerImageWithPngFallback(
                    url = activePreviewPack.thumbnailUrl,
                    contentDescription = activePreviewPack.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF191C26))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePreviewPack.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val countText = selectedSearchDetail?.stickers?.size?.let { "$it stiker" } ?: "Memuat isi..."
                    Text(
                        text = "@${activePreviewPack.author} • $countText",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Segmented Toggle tetap tersedia saat membuka detail paket agar bisa kirim besar atau sisipkan inline
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF171A23))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (mode == StickerPanelMode.INLINE_EMOJI) Color(0xFF282D3C) else Color.Transparent)
                            .clickable { mode = StickerPanelMode.INLINE_EMOJI }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentSatisfiedAlt,
                            contentDescription = "Inline",
                            tint = if (mode == StickerPanelMode.INLINE_EMOJI) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (mode == StickerPanelMode.LARGE_STICKER) Color(0xFF282D3C) else Color.Transparent)
                            .clickable { mode = StickerPanelMode.LARGE_STICKER }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        FoldedStickerIcon(
                            tint = if (mode == StickerPanelMode.LARGE_STICKER) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else if (isSearchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Panah kembali murni tanpa lingkaran/kotak overlay
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Tutup pencarian stiker",
                    tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selectedSearchPack = null
                                selectedSearchDetail = null
                                isSearchMode = false
                                searchQuery = ""
                            }
                        )
                        .padding(5.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF191C26))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = SfProFontFamily
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari stiker atau paket",
                                        color = Color(0xFF64748B),
                                        fontSize = 14.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = searchQuery.dropLast(1)
                        } else {
                            onBackspace()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Hapus",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isSearchMode = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari stiker",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Segmented Toggle Tengah: Emoji-Sticker Inline vs Stiker Besar
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF171A23))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val inlineBg by animateColorAsState(
                        targetValue = if (mode == StickerPanelMode.INLINE_EMOJI) Color(0xFF282D3C) else Color.Transparent,
                        label = "inlineTabBg"
                    )
                    val largeBg by animateColorAsState(
                        targetValue = if (mode == StickerPanelMode.LARGE_STICKER) Color(0xFF282D3C) else Color.Transparent,
                        label = "largeTabBg"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(inlineBg)
                            .clickable { mode = StickerPanelMode.INLINE_EMOJI }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SentimentSatisfiedAlt,
                            contentDescription = "Mode Emoji Stiker Inline",
                            tint = if (mode == StickerPanelMode.INLINE_EMOJI) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(largeBg)
                            .clickable { mode = StickerPanelMode.LARGE_STICKER }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FoldedStickerIcon(
                            tint = if (mode == StickerPanelMode.LARGE_STICKER) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Hapus karakter",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Area Grid Stiker / Daftar Thumbnail Paket Pencarian
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val activePreview = selectedSearchPack
            when {
                isSearchMode && searchQuery.isNotBlank() && activePreview != null -> {
                    when {
                        isLoadingPackDetail -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Memuat stiker paket \"${activePreview.name}\"...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.5.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        packDetailError != null && gridEntries.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = packDetailError.orEmpty(),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                        else -> {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(columnCount),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = gridEntries,
                                    key = { idx, entry ->
                                        when (entry) {
                                            is StickerGridEntry.Header -> "hdr_${entry.sectionKey}_$idx"
                                            is StickerGridEntry.Cell -> "stk_${entry.sectionKey}_${entry.keySuffix}_${entry.item.url}"
                                        }
                                    },
                                    contentType = { _, entry ->
                                        when (entry) {
                                            is StickerGridEntry.Header -> "header"
                                            is StickerGridEntry.Cell -> "sticker_cell"
                                        }
                                    }
                                ) { _, entry ->
                                    if (entry is StickerGridEntry.Cell) {
                                        Box(
                                            modifier = Modifier
                                                .size(itemSize)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    StickerRepository.recordRecentSticker(context, entry.item)
                                                    if (mode == StickerPanelMode.INLINE_EMOJI) {
                                                        onInsertInlineSticker(entry.item)
                                                    } else {
                                                        onSendLargeSticker(entry.item)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            StickerImageWithPngFallback(
                                                url = entry.item.url,
                                                contentDescription = "Stiker",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                isSearchMode && isSearchingRemote -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Mencari paket stiker \"${searchQuery.trim()}\"...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
                isSearchMode && searchQuery.isNotBlank() && searchError != null && searchResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = searchError.orEmpty(),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.5.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
                isSearchMode && searchQuery.isNotBlank() -> {
                    // Tampilkan kartu THUMBNAIL paket saja dulu agar ringan & tidak render semua stiker sekaligus
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = searchResults,
                            key = { _, pack -> pack.id },
                            contentType = { _, _ -> "search_pack" }
                        ) { _, pack ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161923))
                                    .border(0.75.dp, Color(0xFF242938), RoundedCornerShape(12.dp))
                                    .clickable { selectedSearchPack = pack }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF101219)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pack.thumbnailUrl.isNotBlank()) {
                                        StickerImageWithPngFallback(
                                            url = pack.thumbnailUrl,
                                            contentDescription = pack.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = pack.name.take(1).uppercase(),
                                            color = accentColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pack.name,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${pack.author}",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                gridEntries.isEmpty() && isLoadingDefaults -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Menyiapkan stiker...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
                gridEntries.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Belum ada stiker tersedia. Coba cari dengan kata kunci lain.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columnCount),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = gridEntries,
                            key = { idx, entry ->
                                when (entry) {
                                    is StickerGridEntry.Header -> "hdr_${entry.sectionKey}_$idx"
                                    is StickerGridEntry.Cell -> "stk_${entry.sectionKey}_${entry.keySuffix}_${entry.item.url}"
                                }
                            },
                            contentType = { _, entry ->
                                when (entry) {
                                    is StickerGridEntry.Header -> "header"
                                    is StickerGridEntry.Cell -> "sticker_cell"
                                }
                            },
                            span = { _, entry ->
                                when (entry) {
                                    is StickerGridEntry.Header -> GridItemSpan(maxLineSpan)
                                    is StickerGridEntry.Cell -> GridItemSpan(1)
                                }
                            }
                        ) { _, entry ->
                            when (entry) {
                                is StickerGridEntry.Header -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = entry.title,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = SfProFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (entry.subtitle.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = entry.subtitle,
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontFamily = SfProFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                is StickerGridEntry.Cell -> {
                                    Box(
                                        modifier = Modifier
                                            .size(itemSize)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                StickerRepository.recordRecentSticker(context, entry.item)
                                                if (mode == StickerPanelMode.INLINE_EMOJI) {
                                                    onInsertInlineSticker(entry.item)
                                                } else {
                                                    onSendLargeSticker(entry.item)
                                                }
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            StickerImageWithPngFallback(
                                                url = entry.item.url,
                                                contentDescription = "Stiker",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bar Thumbnail Paket Stiker Bawah (48.dp, #11131A) + Sinkronisasi 2 Arah
        if (!isSearchMode) {
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF1E222E))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF11131A))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val recentSelected = visibleSectionKey == "recent"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (recentSelected) Color(0xFF232736) else Color.Transparent)
                        .clickable(enabled = recentStickers.isNotEmpty()) {
                            val idx = sectionStartIndexMap["recent"] ?: return@clickable
                            scope.launch {
                                isProgrammaticScroll = true
                                gridState.animateScrollToItem(idx)
                                delay(300L)
                                isProgrammaticScroll = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Stiker terbaru",
                        tint = if (recentSelected) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color(0xFF232736))
                )

                LazyRow(
                    state = thumbRowState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    itemsIndexed(
                        items = packs,
                        key = { _, pack -> pack.id }
                    ) { _, pack ->
                        val isSelected = visibleSectionKey == pack.id
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Color(0xFF232736) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable {
                                    val targetIdx = sectionStartIndexMap[pack.id] ?: return@clickable
                                    scope.launch {
                                        isProgrammaticScroll = true
                                        gridState.animateScrollToItem(targetIdx)
                                        delay(300L)
                                        isProgrammaticScroll = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StickerImageWithPngFallback(
                                url = pack.thumbnailUrl,
                                contentDescription = pack.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerImageWithPngFallback(
    url: String,
    contentDescription: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    sizePx: Int = 120
) {
    val context = LocalContext.current
    var resolvedUrl by remember(url) { mutableStateOf(url) }
    val request = remember(context, resolvedUrl, sizePx) {
        ImageRequest.Builder(context)
            .data(resolvedUrl)
            .size(sizePx, sizePx)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .listener(
                onError = { _, _ ->
                    if (resolvedUrl.contains("stickerly.pstatic.net", ignoreCase = true) &&
                        resolvedUrl.contains(".webp", ignoreCase = true)
                    ) {
                        resolvedUrl = resolvedUrl.replace(
                            Regex("""\.webp(\?.*)?$""", RegexOption.IGNORE_CASE)
                        ) { ".png${it.groupValues.getOrNull(1).orEmpty()}" }
                    }
                }
            )
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
private fun FoldedStickerIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.2f
        val fold = w * 0.34f
        val stroke = 1.6.dp.toPx()

        val outline = Path().apply {
            moveTo(r, stroke)
            lineTo(w - r, stroke)
            quadraticTo(w - stroke, stroke, w - stroke, r)
            lineTo(w - stroke, h - fold)
            lineTo(w - fold, h - stroke)
            lineTo(r, h - stroke)
            quadraticTo(stroke, h - stroke, stroke, h - r)
            lineTo(stroke, r)
            quadraticTo(stroke, stroke, r, stroke)
            close()
        }
        drawPath(outline, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val corner = Path().apply {
            moveTo(w - fold, h - stroke)
            lineTo(w - fold, h - fold)
            lineTo(w - stroke, h - fold)
        }
        drawPath(corner, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
