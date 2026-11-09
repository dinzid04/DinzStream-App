package com.yuranime.dinz.ui.profile.subscreens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.DownloadedComicChapter
import com.yuranime.dinz.data.repository.ComicDownloader
import com.yuranime.dinz.ui.theme.SfProFontFamily

data class DownloadedEpisode(
    val id: String,
    val title: String,
    val episodeNumber: String,
    val thumbnail: String,
    val fileSizeMb: String,
    val localFilePath: String = ""
)

@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    onPlayOffline: (DownloadedEpisode) -> Unit = {},
    onReadComicOffline: (DownloadedComicChapter) -> Unit = {}
) {
    val context = LocalContext.current
    val localDb = remember { LocalAuthDatabase(context) }

    // 0: Semua, 1: Anime, 2: Komik
    var selectedCategoryTab by remember { mutableIntStateOf(0) }

    val downloadedEpisodes = remember { mutableStateListOf<DownloadedEpisode>() }
    val downloadedComics = remember { mutableStateListOf<DownloadedComicChapter>() }

    fun refreshData() {
        downloadedComics.clear()
        downloadedComics.addAll(localDb.getDownloadedComicChapters())
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val totalComicBytes = remember(downloadedComics.size) {
        downloadedComics.sumOf { it.totalBytes }
    }
    val totalComicMb = totalComicBytes / (1024f * 1024f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0F12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Unduh",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    modifier = Modifier.weight(1f)
                )
            }

            // 1. Indikator Penggunaan Penyimpanan Minimalis
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B26))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Memori Unduhan Internal",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProFontFamily
                        )
                        Text(
                            text = "Terpakai %.1f MB".format(totalComicMb),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = SfProFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val storageFraction = (totalComicMb / 1000f).coerceIn(0.02f, 1f)
                    LinearProgressIndicator(
                        progress = { storageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFF5722),
                        trackColor = Color(0xFF1E2024),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Tab Filter ("Semua", "Anime", "Komik")
            val totalItemsCount = downloadedEpisodes.size + downloadedComics.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadTabPill(
                    label = "Semua ($totalItemsCount)",
                    isSelected = selectedCategoryTab == 0,
                    onClick = { selectedCategoryTab = 0 }
                )
                DownloadTabPill(
                    label = "Komik (${downloadedComics.size})",
                    isSelected = selectedCategoryTab == 2,
                    onClick = { selectedCategoryTab = 2 }
                )
                DownloadTabPill(
                    label = "Anime (${downloadedEpisodes.size})",
                    isSelected = selectedCategoryTab == 1,
                    onClick = { selectedCategoryTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val showComics = (selectedCategoryTab == 0 || selectedCategoryTab == 2) && downloadedComics.isNotEmpty()
            val showEpisodes = (selectedCategoryTab == 0 || selectedCategoryTab == 1) && downloadedEpisodes.isNotEmpty()

            if (showComics || showEpisodes) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showComics) {
                        items(downloadedComics, key = { "comic_${it.chapterId}" }) { comicItem ->
                            DownloadedComicItemCard(
                                item = comicItem,
                                onRead = { onReadComicOffline(comicItem) },
                                onDelete = {
                                    ComicDownloader.deleteDownloadedChapter(context, comicItem.chapterId, comicItem.comicId)
                                    refreshData()
                                    Toast.makeText(context, "Unduhan komik berhasil dihapus", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    if (showEpisodes) {
                        items(downloadedEpisodes, key = { "ep_${it.id}" }) { item ->
                            DownloadItemCard(
                                item = item,
                                onPlay = { onPlayOffline(item) },
                                onDelete = {
                                    downloadedEpisodes.remove(item)
                                    Toast.makeText(context, "Unduhan berhasil dihapus", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // Empty State Bersih
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF161B26)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Belum Ada Konten Diunduh",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SfProFontFamily
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Unduh chapter komik atau episode anime favoritmu untuk dinikmati kapan saja tanpa koneksi internet.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = SfProFontFamily,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFFF5722) else Color(0xFF161B26))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = SfProFontFamily
        )
    }
}

@Composable
private fun DownloadedComicItemCard(
    item: DownloadedComicChapter,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = {
                Text(
                    text = "Hapus Unduhan Komik?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            },
            text = {
                Text(
                    text = "Hapus ${item.comicTitle} - Ch. ${item.chapterNumber} dari memori perangkat?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    fontFamily = SfProFontFamily
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Hapus", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E222A)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B26))
            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Poster 3:4
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(78.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2024))
            ) {
                if (item.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.coverUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_yuranime_monochrome)
                            .error(R.drawable.ic_yuranime_monochrome)
                            .build(),
                        contentDescription = item.comicTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Komik & Chapter
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.comicTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ch. ${item.chapterNumber} • ${item.chapterTitle}",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val sizeMb = item.totalBytes / (1024f * 1024f)
                Text(
                    text = "${item.pageCount} Halaman • %.1f MB".format(sizeMb),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Baca Offline & Hapus
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRead, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = "Baca",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { showConfirmDelete = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadedEpisode,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B26))
            .border(0.7.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail 16:9
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2024))
            ) {
                if (item.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.thumbnail)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_yuranime_monochrome)
                            .error(R.drawable.ic_yuranime_monochrome)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_yuranime_monochrome),
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Anime & Episode
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Episode ${item.episodeNumber}",
                    color = Color(0xFFFF5722),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SfProFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.fileSizeMb,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: Play & Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
