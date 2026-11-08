package com.yuranime.dinz.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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

private const val COMIC_IMAGE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val COMIC_IMAGE_REFERER = "https://app.shinigami.asia/"

/**
 * Section Komik Terbaru di HomeScreen:
 * - 10 komik terbaru (5 baris ke bawah x 2 kolom berdampingan).
 * - TANPA kotak/card overlay abu-abu (#141418) dan TANPA border di belakang item komik.
 * - Judul dikunci tepat 2 baris (minLines = 2, maxLines = 2) agar bendera dan 3 chapter
 *   di kolom kiri dan kanan 100% sejajar rata horizontal.
 */
@Composable
fun ComicLatestSection(
    comics: List<ComicItem>,
    onComicClick: (comicId: String) -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comics.isEmpty()) return

    // Ambil 10 komik terbaru dan susun menjadi 5 baris x 2 kolom
    val displayComics = comics.take(10)
    val rows = displayComics.chunked(2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
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
 * Item Komik Bersih Tanpa Kotak/Border Overlay:
 * - Poster vertikal di kiri langsung di atas latar utama (#0B0B0B).
 * - Judul komik 2 baris rata (minLines = 2, maxLines = 2) + bendera negara + 3 chapter sejajar.
 */
@Composable
fun ComicGridCard(
    comic: ComicItem,
    onComicClick: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveCover = comic.coverPortrait.ifBlank { comic.coverLandscape.orEmpty() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onComicClick),
        verticalAlignment = Alignment.Top
    ) {
        // SISI KIRI: Poster Komik Vertikal Murni (Tanpa kotak overlay di belakangnya)
        Box(
            modifier = Modifier
                .width(68.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(effectiveCover)
                    .addHeader("User-Agent", COMIC_IMAGE_USER_AGENT)
                    .addHeader("Referer", COMIC_IMAGE_REFERER)
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

        // SISI KANAN: Judul (2 baris tetap), Bendera Negara, dan 3 Chapter Terbaru Sejajar
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = comic.title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Ikon Bendera Negara (China / Jepang / Korea)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(comic.flagUrl)
                    .crossfade(false)
                    .build(),
                contentDescription = comic.format,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 18.dp, height = 12.dp)
                    .clip(RoundedCornerShape(2.5.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3 Chapter Terbaru yang Rapat & Sejajar Rata Horizontal
            Column(
                verticalArrangement = Arrangement.spacedBy(3.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                comic.chapters.take(3).forEach { chapter ->
                    val chNum = chapter.number.ifBlank { "1" }
                    val timeStr = chapter.releaseTime.ifBlank { chapter.releaseDate ?: "Just now" }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5722))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CH. $chNum",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = timeStr,
                            color = Color(0xFF64748B),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
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
