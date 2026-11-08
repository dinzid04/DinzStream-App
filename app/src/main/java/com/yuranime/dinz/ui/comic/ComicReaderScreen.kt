package com.yuranime.dinz.ui.comic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.data.model.ComicChapterDetail
import com.yuranime.dinz.data.repository.ComicRepository
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

@Composable
fun ComicReaderScreen(
    chapterId: String,
    comicId: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    repository: ComicRepository = remember { ComicRepository() }
) {
    BackHandler { onBackClick() }

    var currentChapterId by remember { mutableStateOf(chapterId) }
    var chapterDetail by remember { mutableStateOf<ComicChapterDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var areControlsVisible by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun loadChapter(id: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            currentChapterId = id
            val result = repository.getChapterDetail(id)
            if (result.isSuccess) {
                chapterDetail = result.getOrNull()
                listState.scrollToItem(0)
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Gagal memuat halaman baca komik"
            }
            isLoading = false
        }
    }

    LaunchedEffect(chapterId) {
        loadChapter(chapterId)
    }

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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ColorAccentPrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Memuat halaman komik...",
                        color = ColorTextSecondary,
                        fontSize = 13.sp,
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
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan",
                        color = ColorTextSecondary,
                        fontSize = 14.sp,
                        fontFamily = SfProFontFamily
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ColorAccentPrimary)
                            .clickable { loadChapter(currentChapterId) }
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
        } else if (chapterDetail != null) {
            val detail = chapterDetail!!

            // Webtoon Vertical Scroller
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                itemsIndexed(
                    items = detail.pages,
                    key = { idx, url -> "$idx-$url" }
                ) { index, imageUrl ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .addHeader("Referer", "https://shinigami.io")
                            .build(),
                        contentDescription = "Halaman ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(Color(0xFF111216)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ColorAccentPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFF14151A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Gagal memuat halaman ${index + 1}",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    )
                }

                // End of Chapter Footer
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ColorAccentPrimary)
                                    .clickable { loadChapter(detail.nextChapterId) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
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
        }

        // Top Floating Bar (Back + Title)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC000000))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (chapterDetail != null) "Chapter ${chapterDetail!!.chapterNumber}" else "Membaca Komik",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )
                }
            }
        }

        // Bottom Floating Nav Bar (Prev / Next Chapter)
        AnimatedVisibility(
            visible = areControlsVisible && chapterDetail != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            val detail = chapterDetail ?: return@AnimatedVisibility

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE614161C))
                    .border(1.dp, Color(0xFF2A2E37), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Prev Chapter Button
                    val hasPrev = !detail.prevChapterId.isNullOrBlank()
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = hasPrev) {
                                detail.prevChapterId?.let { loadChapter(it) }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Chapter Sebelumnya",
                            tint = if (hasPrev) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Prev",
                            color = if (hasPrev) Color.White else Color(0xFF4B5563),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Text(
                        text = "Ch. ${detail.chapterNumber}",
                        color = ColorAccentPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    // Next Chapter Button
                    val hasNext = !detail.nextChapterId.isNullOrBlank()
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = hasNext) {
                                detail.nextChapterId?.let { loadChapter(it) }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next",
                            color = if (hasNext) Color.White else Color(0xFF4B5563),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Chapter Berikutnya",
                            tint = if (hasNext) Color.White else Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
