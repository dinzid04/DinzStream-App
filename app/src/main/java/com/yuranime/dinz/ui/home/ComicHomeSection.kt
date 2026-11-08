package com.yuranime.dinz.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.yuranime.dinz.data.model.ComicItem
import com.yuranime.dinz.ui.theme.SfProFontFamily

/**
 * Section Komik Terbaru di HomeScreen:
 * - 2 kolom grid ke bawah (total 8 poster, 4 baris ke bawah).
 * - Tidak menggunakan slider / LazyRow yang memotong kartu kanan.
 * - Sisi kiri poster murni tanpa overlay gelap di belakang poster.
 */
@Composable
fun ComicLatestSection(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comics.isEmpty()) return

    // Ambil maksimal 8 komik terbaru dan bagi menjadi 4 pasang (2 kolom per baris)
    val displayComics = comics.take(8)
    val rows = displayComics.chunked(2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kartu Kolom Kiri
                val first = pair.getOrNull(0)
                if (first != null) {
                    ComicGridCard(
                        comic = first,
                        onComicClick = { onComicClick(first.id) },
                        onChapterClick = onChapterClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Kartu Kolom Kanan
                val second = pair.getOrNull(1)
                if (second != null) {
                    ComicGridCard(
                        comic = second,
                        onComicClick = { onComicClick(second.id) },
                        onChapterClick = onChapterClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Kartu Komik 2 Kolom Padat & Rapi:
 * - Poster 62.dp vertikal 2:3 tanpa overlay gelap di belakang poster.
 * - Judul komik SF Bold putih 12.sp max 2 baris.
 * - Bendera negara 16.dp x 11.dp.
 * - 3 chapter terbaru dengan titik oranye + CH. [Nomor] + waktu rilis singkat.
 */
@Composable
fun ComicGridCard(
    comic: ComicItem,
    onComicClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF222632), RoundedCornerShape(10.dp))
            .clickable(onClick = onComicClick)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SISI KIRI: Poster Murni (width = 62.dp, ratio 2:3, TANPA overlay gelap di belakang poster)
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comic.coverPortrait)
                        .crossfade(false)
                        .size(coil.size.Size.ORIGINAL)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // SISI KANAN: Judul, Bendera, dan 3 Chapter Terbaru
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Judul & Bendera
                Column {
                    Text(
                        text = comic.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Bendera Negara (16x11dp, rounded 2dp)
                    AsyncImage(
                        model = comic.flagUrl,
                        contentDescription = comic.format,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 16.dp, height = 11.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }

                // 3 Chapter Terbaru (Jarak rapat 4.dp di bawah bendera)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayChapters = comic.chapters.take(3)
                    displayChapters.forEach { chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .clickable {
                                    if (chapter.id.isNotBlank()) {
                                        onChapterClick(chapter.id)
                                    } else {
                                        onComicClick()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Kiri: Titik bulat oranye + CH. [Nomor]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5722))
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (chapter.number.isNotBlank()) "CH. ${chapter.number}" else "CH. -",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Kanan: Waktu rilis relatif
                            val timeStr = chapter.timeAgo.ifBlank { "Baru" }
                            Text(
                                text = timeStr,
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
