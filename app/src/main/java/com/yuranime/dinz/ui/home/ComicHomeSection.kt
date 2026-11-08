package com.yuranime.dinz.ui.home

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

@Composable
fun ComicLatestSection(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comics.isEmpty()) return

    // Chunk into 2 items per column for a neat 2-row compact horizontal pager
    val comicPairs = comics.chunked(2)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(comicPairs, key = { pair -> pair.first().id }) { pair ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { comic ->
                    ComicHomeCard(
                        comic = comic,
                        onComicClick = { onComicClick(comic.id) },
                        onChapterClick = onChapterClick
                    )
                }
            }
        }
    }
}

@Composable
fun ComicHomeCard(
    comic: ComicItem,
    onComicClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(295.dp)
            .height(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141418))
            .border(1.dp, Color(0xFF222632), RoundedCornerShape(12.dp))
            .clickable(onClick = onComicClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sisi Kiri: Poster Vertikal (aspectRatio 2:3, rounded 8.dp)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2430))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comic.coverPortrait)
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder_yuranime)
                        .error(R.drawable.placeholder_yuranime)
                        .build(),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sisi Kanan: Judul, Bendera, dan 3 Chapter Terbaru (Rapat & Presisi)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Sisi Kanan Atas: Judul & Bendera
                Column {
                    // Judul Komik: SF Bold putih, max 2 baris
                    Text(
                        text = comic.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Ikon Bendera Negara (20x14dp, rounded 3dp)
                    AsyncImage(
                        model = comic.flagUrl,
                        contentDescription = comic.format,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 20.dp, height = 14.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                // Sisi Kanan Bawah: 3 Chapter Terbaru (Rapat 6-8dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayChapters = comic.chapters.take(3)
                    displayChapters.forEach { chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    if (chapter.id.isNotBlank()) {
                                        onChapterClick(chapter.id)
                                    } else {
                                        onComicClick()
                                    }
                                }
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Titik bulat oranye/merah (#FF5722)
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5722))
                            )

                            Spacer(modifier = Modifier.width(5.dp))

                            // CH. [Nomor] (SF Bold putih 12.sp)
                            Text(
                                text = "CH. ${chapter.number}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Waktu rilis chapter (SF Medium 11.sp abu-abu #64748B)
                            Text(
                                text = chapter.releaseTime.ifBlank { "Just now" },
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SfProFontFamily,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
