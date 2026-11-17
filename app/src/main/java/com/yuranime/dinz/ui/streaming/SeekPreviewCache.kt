package com.yuranime.dinz.ui.streaming

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pengelola cache thumbnail preview seekbar khusus di RAM (LruCache, maks 18 frame).
 * Tidak pernah menulis file ke penyimpanan internal perangkat agar aplikasi tetap ringan,
 * dan menyediakan pembersihan instan saat ganti episode atau keluar dari pemutar video.
 */
class SeekPreviewManager(
    maxFrames: Int = 18
) {
    companion object {
        private const val THUMB_TARGET_WIDTH = 160
        private const val THUMB_TARGET_HEIGHT = 90
        private const val BUCKET_INTERVAL_MS = 2000L
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    private val ramCache = LruCache<Long, ImageBitmap>(maxFrames.coerceIn(15, 20))
    private val retrieverMutex = Mutex()
    private var activeSourceKey: String? = null
    private var retriever: MediaMetadataRetriever? = null
    private var sourceFailed: Boolean = false

    fun bucketKeyFor(positionMs: Long): Long {
        return (positionMs.coerceAtLeast(0L) / BUCKET_INTERVAL_MS) * BUCKET_INTERVAL_MS
    }

    fun getCachedFrame(positionMs: Long): ImageBitmap? {
        val key = bucketKeyFor(positionMs)
        return synchronized(ramCache) { ramCache.get(key) }
    }

    suspend fun getOrExtractFrame(
        streamUrl: String,
        episodeId: String,
        positionMs: Long
    ): ImageBitmap? {
        if (streamUrl.isBlank()) return null
        val key = bucketKeyFor(positionMs)

        synchronized(ramCache) {
            ramCache.get(key)?.let { return it }
        }

        val sourceKey = "$episodeId|$streamUrl"
        return withContext(Dispatchers.IO) {
            retrieverMutex.withLock {
                // Cek ulang cache di dalam lock setelah menunggu request sebelumnya
                synchronized(ramCache) {
                    ramCache.get(key)?.let { return@withContext it }
                }

                if (activeSourceKey != sourceKey) {
                    releaseRetrieverLocked()
                    activeSourceKey = sourceKey
                    sourceFailed = false
                }

                if (sourceFailed) {
                    return@withContext null
                }

                // Lewati URL manifest HLS karena MediaMetadataRetriever tidak mendukung frame range HLS
                val lowerUrl = streamUrl.lowercase()
                if (lowerUrl.contains(".m3u8")) {
                    sourceFailed = true
                    return@withContext null
                }

                try {
                    val activeRetriever = retriever ?: MediaMetadataRetriever().also { mmr ->
                        if (streamUrl.startsWith("http://", ignoreCase = true) ||
                            streamUrl.startsWith("https://", ignoreCase = true)
                        ) {
                            mmr.setDataSource(
                                streamUrl,
                                mapOf("User-Agent" to DEFAULT_USER_AGENT)
                            )
                        } else {
                            mmr.setDataSource(streamUrl)
                        }
                        retriever = mmr
                    }

                    val timeUs = key * 1000L
                    val rawBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        activeRetriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            THUMB_TARGET_WIDTH,
                            THUMB_TARGET_HEIGHT
                        )
                    } else {
                        activeRetriever.getFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )?.let { decoded ->
                            val aspectHeight = if (decoded.width > 0) {
                                ((decoded.height.toFloat() / decoded.width.toFloat()) * THUMB_TARGET_WIDTH)
                                    .toInt()
                                    .coerceIn(60, 140)
                            } else {
                                THUMB_TARGET_HEIGHT
                            }
                            if (decoded.width > THUMB_TARGET_WIDTH) {
                                val scaled = Bitmap.createScaledBitmap(
                                    decoded,
                                    THUMB_TARGET_WIDTH,
                                    aspectHeight,
                                    true
                                )
                                if (scaled !== decoded) decoded.recycle()
                                scaled
                            } else {
                                decoded
                            }
                        }
                    }

                    val imageBitmap = rawBitmap?.asImageBitmap()
                    if (imageBitmap != null) {
                        synchronized(ramCache) {
                            ramCache.put(key, imageBitmap)
                        }
                    }
                    imageBitmap
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    sourceFailed = true
                    releaseRetrieverLocked()
                    null
                }
            }
        }
    }

    fun clearAll() {
        synchronized(ramCache) {
            ramCache.evictAll()
        }
        activeSourceKey = null
        sourceFailed = false
        val toRelease = retriever
        retriever = null
        if (toRelease != null) {
            try {
                toRelease.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun releaseRetrieverLocked() {
        val toRelease = retriever
        retriever = null
        if (toRelease != null) {
            try {
                toRelease.release()
            } catch (_: Throwable) {
            }
        }
    }
}
