package com.yuranime.dinz.ui.comic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
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
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.ComicDetail
import com.yuranime.dinz.data.repository.ComicRepository
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.launch

@Composable
fun ComicDetailScreen(
    comicId: String,
    onBackClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    repository: ComicRepository = remember { ComicRepository() }
) {
    BackHandler { onBackClick() }

    var comicDetail by remember { mutableStateOf<ComicDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
            .background(ColorBackgroundPrimary)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorAccentPrimary, modifier = Modifier.size(36.dp))
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 1. Backdrop Cover & Hero Info
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        // Blurred / Landscape Backdrop
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(detail.coverLandscape ?: detail.coverPortrait)
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder_yuranime_wide)
                                .error(R.drawable.placeholder_yuranime_wide)
                                .build(),
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Smooth gradient darkening to canvas background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x66000000),
                                            Color(0xCC0B0B0B),
                                            ColorBackgroundPrimary
                                        )
                                    )
                                )
                        )

                        // Poster + Header Info Row
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Poster Card
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(detail.coverPortrait)
                                        .crossfade(true)
                                        .placeholder(R.drawable.placeholder_yuranime)
                                        .error(R.drawable.placeholder_yuranime)
                                        .build(),
                                    contentDescription = detail.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Details Column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                // Title
                                Text(
                                    text = detail.title,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Flag + Type + Status
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Country Flag
                                    AsyncImage(
                                        model = detail.flagUrl,
                                        contentDescription = detail.format,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 22.dp, height = 15.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = detail.format,
                                        color = ColorAccentPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )

                                    Text(
                                        text = " • ",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        text = detail.status ?: "Ongoing",
                                        color = ColorTextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }

                                if (detail.rating != null && detail.rating > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = String.format("%.1f", detail.rating),
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

                // 2. Action Bar (Mulai Baca + Bookmark)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Button "Mulai Baca" (Opens First / Latest Chapter)
                        val firstChapter = detail.chapters.lastOrNull() ?: detail.chapters.firstOrNull()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFFF5722))
                                    )
                                )
                                .clickable(enabled = firstChapter != null) {
                                    firstChapter?.id?.let { onChapterClick(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (firstChapter != null) "Mulai Baca (Ch. ${firstChapter.number})" else "Mulai Baca",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }

                        // Bookmark Icon Button
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2430))
                                .clickable { isBookmarked = !isBookmarked },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Simpan",
                                tint = if (isBookmarked) ColorAccentPrimary else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // 3. Genres Chips
                if (detail.genres.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detail.genres.take(5).forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1C1F26))
                                        .border(1.dp, Color(0xFF2A2E37), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = g,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Synopsis / Description
                if (!detail.description.isNullOrBlank()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .animateContentSize()
                        ) {
                            Text(
                                text = "Sinopsis",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail.description,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontFamily = SfProFontFamily,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isDescriptionExpanded) "Lebih sedikit" else "Selengkapnya...",
                                    color = ColorAccentPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = SfProFontFamily
                                )
                                Icon(
                                    imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = ColorAccentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Chapter List Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Chapter (${detail.chapters.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )
                    }
                }

                // 6. Chapter List Rows
                items(detail.chapters, key = { it.id.ifBlank { it.number } }) { chapter ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141418))
                            .border(1.dp, Color(0xFF222632), RoundedCornerShape(10.dp))
                            .clickable { onChapterClick(chapter.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
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
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ColorAccentPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Chapter ${chapter.number}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SfProFontFamily
                                    )
                                    if (chapter.releaseTime.isNotBlank()) {
                                        Text(
                                            text = chapter.releaseTime,
                                            color = Color(0xFF64748B),
                                            fontSize = 11.5.sp,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Floating Back Button
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
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
