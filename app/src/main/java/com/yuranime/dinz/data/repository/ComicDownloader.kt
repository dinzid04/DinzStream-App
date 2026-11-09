package com.yuranime.dinz.data.repository

import android.content.Context
import com.yuranime.dinz.data.local.LocalAuthDatabase
import com.yuranime.dinz.data.model.ComicChapterItem
import com.yuranime.dinz.data.model.DownloadedComicChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object ComicDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Set<String>>(emptySet())
    val activeDownloads: StateFlow<Set<String>> = _activeDownloads.asStateFlow()

    fun getOfflineDir(context: Context, comicId: String, chapterId: String): File {
        val safeComicId = comicId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val safeChapterId = chapterId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(context.filesDir, "offline_comics/$safeComicId/$safeChapterId")
    }

    fun isChapterDownloaded(context: Context, chapterId: String): Boolean {
        return LocalAuthDatabase(context).isChapterDownloaded(chapterId)
    }

    fun getOfflinePages(context: Context, comicId: String, chapterId: String): List<File>? {
        val dir = getOfflineDir(context, comicId, chapterId)
        if (!dir.exists() || !dir.isDirectory) {
            // Coba periksa direct child jika safeComicId berbeda
            val parent = File(context.filesDir, "offline_comics")
            if (parent.exists()) {
                val found = parent.walkTopDown().firstOrNull { it.isDirectory && it.name == chapterId.replace(Regex("[^a-zA-Z0-9_-]"), "_") }
                if (found != null && found.isDirectory) {
                    val files = found.listFiles { f -> f.isFile && f.length() > 0 }?.sortedBy { it.name }
                    if (!files.isNullOrEmpty()) return files
                }
            }
            return null
        }
        val files = dir.listFiles { f -> f.isFile && f.length() > 0 }?.sortedBy { it.name }
        return if (!files.isNullOrEmpty()) files else null
    }

    suspend fun downloadChapter(
        context: Context,
        comicId: String,
        comicTitle: String,
        chapter: ComicChapterItem,
        coverUrl: String,
        repository: ComicRepository
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val chapterId = chapter.id
        if (chapterId.isBlank()) {
            return@withContext Result.failure(Exception("ID chapter tidak valid"))
        }

        if (isChapterDownloaded(context, chapterId)) {
            return@withContext Result.success(true)
        }

        if (_activeDownloads.value.contains(chapterId)) {
            return@withContext Result.failure(Exception("Chapter sedang diunduh"))
        }

        _activeDownloads.update { it + chapterId }
        _downloadProgress.update { it + (chapterId to 0.05f) }

        try {
            val detailResult = repository.getChapterDetail(chapterId)
            val detail = detailResult.getOrNull()
                ?: throw (detailResult.exceptionOrNull() ?: Exception("Gagal memuat daftar halaman chapter"))

            val pages = detail.pages
            if (pages.isEmpty()) {
                throw Exception("Tidak ada halaman pada chapter ini")
            }

            val targetDir = getOfflineDir(context, comicId, chapterId)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            var totalBytes = 0L
            val totalPages = pages.size

            for ((index, pageUrl) in pages.withIndex()) {
                val referer = if (pageUrl.contains("komikstation") || pageUrl.contains("klikcdn")) {
                    "https://komikstation.org/"
                } else {
                    "https://app.shinigami.asia/"
                }

                val request = Request.Builder()
                    .url(pageUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Referer", referer)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()

                val call = client.newCall(request)
                val response = call.execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code} saat mengunduh halaman ${index + 1}")
                }

                val bytes = response.body?.bytes()
                    ?: throw Exception("Data kosong pada halaman ${index + 1}")

                val pageFile = File(targetDir, "page_%03d.webp".format(index + 1))
                pageFile.writeBytes(bytes)
                totalBytes += bytes.size

                val progress = 0.05f + (0.90f * (index + 1).toFloat() / totalPages.toFloat())
                _downloadProgress.update { it + (chapterId to progress) }
            }

            // Simpan ke database lokal
            val downloadedChapter = DownloadedComicChapter(
                chapterId = chapterId,
                comicId = comicId,
                comicTitle = comicTitle,
                chapterNumber = chapter.number,
                chapterTitle = chapter.title.ifBlank { "Chapter ${chapter.number}" },
                coverUrl = coverUrl,
                pageCount = totalPages,
                localDirPath = targetDir.absolutePath,
                totalBytes = totalBytes,
                downloadedAt = System.currentTimeMillis()
            )

            LocalAuthDatabase(context).saveDownloadedComicChapter(downloadedChapter)
            _downloadProgress.update { it + (chapterId to 1.0f) }

            Result.success(true)
        } catch (e: Exception) {
            val targetDir = getOfflineDir(context, comicId, chapterId)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            Result.failure(e)
        } finally {
            _activeDownloads.update { it - chapterId }
            _downloadProgress.update { it - chapterId }
        }
    }

    fun deleteDownloadedChapter(context: Context, chapterId: String, comicId: String? = null) {
        try {
            if (comicId != null) {
                val dir = getOfflineDir(context, comicId, chapterId)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            } else {
                val parent = File(context.filesDir, "offline_comics")
                if (parent.exists()) {
                    val safeChapterId = chapterId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    parent.walkTopDown().filter { it.isDirectory && it.name == safeChapterId }.forEach {
                        it.deleteRecursively()
                    }
                }
            }
            LocalAuthDatabase(context).deleteDownloadedComicChapter(chapterId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
