package com.yuranime.dinz.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pengelola unggahan Avatar & Banner ke CDN Telegram + Cloudflare Worker (`yuranime-tg-cdn`).
 * Mendukung foto statis (dikompresi ke WebP) maupun media bergerak (GIF, WebP animasi, APNG,
 * serta konversi otomatis Video ke Animated GIF).
 */
object R2CdnUploader {
    private const val TAG = "R2CdnUploader"

    private const val TG_CDN_BASE = "https://yuranime-tg-cdn.gadingkencana04.workers.dev"
    private const val TG_UPLOAD_URL = "$TG_CDN_BASE/upload"
    private const val TG_UPLOAD_KEY = "yrn_tg_a86e4f8a6563a37e1afb996e93850e97"

    private const val MAX_ANIMATED_UPLOAD_BYTES = 18 * 1024 * 1024
    private const val MAX_CHAT_IMAGE_BYTES = 10 * 1024 * 1024

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()

    private data class PreparedUpload(
        val bytes: ByteArray,
        val mimeType: String,
        val ext: String
    )

    private data class WorkerUploadResponse(
        val success: Boolean = false,
        val url: String? = null,
        val error: String? = null
    )

    /**
     * Menyiapkan preview media sebelum upload. Jika pengguna memilih file video,
     * mengonversinya menjadi file GIF lokal di cache agar preview di layar Edit Profil langsung bergerak.
     */
    suspend fun prepareMediaForPreview(
        context: Context,
        uri: Uri,
        isBanner: Boolean
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!VideoToGifConverter.isVideoUri(context, uri)) {
                return@withContext Result.success(uri)
            }
            val gifFile = VideoToGifConverter.convertVideoToGif(context, uri, isBanner = isBanner)
                ?: return@withContext Result.failure(
                    Exception("Video gagal dikonversi ke GIF. Gunakan video pendek (maks 5 detik).")
                )
            Result.success(Uri.fromFile(gifFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Memproses media dari URI lokal (foto statis, GIF/WebP animasi, atau video)
     * lalu mengunggahnya ke CDN Telegram permanen.
     */
    suspend fun compressAndUploadImage(
        context: Context,
        uriString: String,
        uid: String,
        isBanner: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                return@withContext Result.success(uriString)
            }

            val uri = Uri.parse(uriString)
            val prepared = prepareBytesForUpload(context, uri, isBanner)
                ?: return@withContext Result.failure(
                    Exception("Gagal memproses file media. Pastikan file valid dan ukuran tidak terlalu besar.")
                )

            uploadToTelegramCdn(
                bytes = prepared.bytes,
                mimeType = prepared.mimeType,
                ext = prepared.ext,
                uid = uid,
                type = if (isBanner) "banner" else "avatar"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memproses/upload media: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Memproses foto/GIF chat (maksimal 10 MB) lalu mengunggah ke Telegram CDN. */
    suspend fun compressAndUploadChatImage(
        context: Context,
        uriString: String,
        uid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                return@withContext Result.success(uriString)
            }
            val uri = Uri.parse(uriString)
            if (VideoToGifConverter.isVideoUri(context, uri)) {
                return@withContext Result.failure(
                    Exception("Pilih file gambar atau GIF animasi (maks 10 MB).")
                )
            }
            val rawBytes = readBytesBounded(context, uri, MAX_CHAT_IMAGE_BYTES + 1)
            val prepared = if (rawBytes != null && rawBytes.size <= MAX_CHAT_IMAGE_BYTES &&
                VideoToGifConverter.isAnimatedImageBytes(rawBytes)
            ) {
                val (mime, ext) = VideoToGifConverter.detectImageMimeAndExt(rawBytes)
                PreparedUpload(bytes = rawBytes, mimeType = mime, ext = ext)
            } else if (rawBytes == null) {
                val head = readBytesBounded(context, uri, 32 * 1024)
                val mimeHint = context.contentResolver.getType(uri).orEmpty().lowercase()
                if (mimeHint.contains("gif") || (head != null && VideoToGifConverter.isAnimatedImageBytes(head))) {
                    return@withContext Result.failure(
                        Exception("GIF/WebP animasi melebihi batas maksimal 10 MB.")
                    )
                }
                val webpBytes = compressStaticToWebP(
                    context = context,
                    uri = uri,
                    maxWidth = 1280,
                    targetMaxBytes = 420 * 1024
                ) ?: return@withContext Result.failure(
                    Exception("Ukuran media melebihi batas maksimal 10 MB.")
                )
                PreparedUpload(bytes = webpBytes, mimeType = "image/webp", ext = "webp")
            } else {
                val webpBytes = compressStaticToWebP(
                    context = context,
                    uri = uri,
                    maxWidth = 1280,
                    targetMaxBytes = 420 * 1024
                ) ?: return@withContext Result.failure(
                    Exception("Gagal memproses gambar chat.")
                )
                PreparedUpload(bytes = webpBytes, mimeType = "image/webp", ext = "webp")
            }

            if (prepared.bytes.size > MAX_CHAT_IMAGE_BYTES) {
                return@withContext Result.failure(
                    Exception("Ukuran media melebihi batas maksimal 10 MB.")
                )
            }

            uploadToTelegramCdn(
                bytes = prepared.bytes,
                mimeType = prepared.mimeType,
                ext = prepared.ext,
                uid = uid,
                type = "chat"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal upload gambar chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun prepareBytesForUpload(
        context: Context,
        uri: Uri,
        isBanner: Boolean
    ): PreparedUpload? {
        // 1. Jika input berupa video -> konversi ke Animated GIF
        if (VideoToGifConverter.isVideoUri(context, uri)) {
            val gifFile = VideoToGifConverter.convertVideoToGif(context, uri, isBanner = isBanner)
                ?: return null
            val bytes = gifFile.readBytes()
            runCatching { gifFile.delete() }
            if (bytes.isEmpty() || bytes.size > MAX_ANIMATED_UPLOAD_BYTES) return null
            return PreparedUpload(bytes = bytes, mimeType = "image/gif", ext = "gif")
        }

        // 2. Baca header & raw bytes untuk deteksi GIF / WebP animasi / APNG tanpa merusak animasinya
        val rawBytes = readBytesBounded(context, uri, MAX_ANIMATED_UPLOAD_BYTES + 1)
        if (rawBytes != null && rawBytes.size <= MAX_ANIMATED_UPLOAD_BYTES &&
            VideoToGifConverter.isAnimatedImageBytes(rawBytes)
        ) {
            val (mime, ext) = VideoToGifConverter.detectImageMimeAndExt(rawBytes)
            Log.d(TAG, "Mempertahankan media animasi asli ($mime, ${rawBytes.size / 1024} KB)")
            return PreparedUpload(bytes = rawBytes, mimeType = mime, ext = ext)
        }

        // Jika GIF/WebP animasi terlalu besar > 18 MB, jangan diratakan ke statis secara diam-diam
        if (rawBytes != null && rawBytes.size > MAX_ANIMATED_UPLOAD_BYTES) {
            return null
        }

        // 3. Gambar statis -> resize + kompres ke WebP ringan
        val webpBytes = compressStaticToWebP(
            context = context,
            uri = uri,
            maxWidth = if (isBanner) 1200 else 512,
            targetMaxBytes = if (isBanner) 250 * 1024 else 160 * 1024
        ) ?: return null

        return PreparedUpload(bytes = webpBytes, mimeType = "image/webp", ext = "webp")
    }

    private fun uploadToTelegramCdn(
        bytes: ByteArray,
        mimeType: String,
        ext: String,
        uid: String,
        type: String
    ): Result<String> {
        return try {
            val fileName = "${type}_${uid.take(12)}_${System.currentTimeMillis()}.$ext"
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uid", uid)
                .addFormDataPart("type", type)
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(TG_UPLOAD_URL)
                .header("User-Agent", "YuranimeApp/2.8 (Android)")
                .header("X-Upload-Key", TG_UPLOAD_KEY)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errDetail = runCatching {
                        gson.fromJson(rawBody, WorkerUploadResponse::class.java)?.error
                    }.getOrNull()
                    return Result.failure(
                        Exception(errDetail ?: "Upload CDN gagal (HTTP ${response.code})")
                    )
                }

                val parsed = runCatching {
                    gson.fromJson(rawBody, WorkerUploadResponse::class.java)
                }.getOrNull()

                val uploadedUrl = parsed?.url
                if (parsed?.success == true && !uploadedUrl.isNullOrBlank()) {
                    Result.success(uploadedUrl)
                } else {
                    Result.failure(
                        Exception(parsed?.error ?: "Respons upload CDN tidak valid.")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upload ke Telegram CDN: ${e.message}", e)
            Result.failure(Exception("Koneksi ke server CDN gagal: ${e.message}"))
        }
    }

    private fun openStream(context: Context, uri: Uri): InputStream? {
        val filePath = uri.path
        return if (uri.scheme == "file" && !filePath.isNullOrBlank()) {
            FileInputStream(File(filePath))
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    private fun readBytesBounded(context: Context, uri: Uri, maxCap: Int): ByteArray? {
        return try {
            openStream(context, uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buf)
                    if (read == -1) break
                    total += read
                    if (total > maxCap) return null
                    out.write(buf, 0, read)
                }
                out.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun compressStaticToWebP(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        targetMaxBytes: Int
    ): ByteArray? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sampleSize = 1
            while (srcW / sampleSize > maxWidth * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = openStream(context, uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            if (bitmap.width > maxWidth) {
                val scale = maxWidth.toFloat() / bitmap.width.toFloat()
                val targetH = max(1, (bitmap.height * scale).roundToInt())
                val resized = Bitmap.createScaledBitmap(bitmap, maxWidth, targetH, true)
                if (resized !== bitmap) bitmap.recycle()
                bitmap = resized
            }

            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            val stream = ByteArrayOutputStream()
            var quality = 88
            bitmap.compress(compressFormat, quality, stream)

            while (stream.size() > targetMaxBytes && quality > 45) {
                stream.reset()
                quality -= 10
                bitmap.compress(compressFormat, quality, stream)
            }

            val finalBytes = stream.toByteArray()
            bitmap.recycle()
            finalBytes
        } catch (e: Exception) {
            Log.e(TAG, "Error kompresi WebP statis: ${e.message}", e)
            null
        }
    }
}
