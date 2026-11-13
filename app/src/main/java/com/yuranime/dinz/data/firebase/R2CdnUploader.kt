package com.yuranime.dinz.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

/**
 * TAHAP 4: Utilitas Kompresi WebP & Upload Media ke Cloudflare R2 / Dinz CDN
 * Mengompresi gambar galeri menjadi WebP tajam (< 150KB) lalu mengunggahnya ke
 * jaringan CDN Dinz (cdn.dinzid.my.id / url.dinzid.my.id) agar tersimpan sebagai URL publik permanen.
 */
object R2CdnUploader {
    private const val TAG = "R2CdnUploader"
    private const val MAX_BYTES = 148 * 1024 // < 150 KB

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Memproses URI gambar dari galeri (atau mengembalikan URL langsung jika sudah berupa https://),
     * mengompresi ke WebP, dan mengunggahnya ke Cloudflare R2 / Dinz CDN.
     */
    suspend fun compressAndUploadImage(
        context: Context,
        uriString: String,
        uid: String,
        isBanner: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmed = uriString.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.success("")
            }
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return@withContext Result.success(trimmed)
            }

            val uri = Uri.parse(trimmed)
            val webpBytes = decodeAndCompressToWebP(
                context = context,
                uri = uri,
                maxWidth = if (isBanner) 1200 else 512,
                maxHeight = if (isBanner) 600 else 512
            ) ?: return@withContext Result.failure(
                IllegalStateException("Gagal membaca atau mengompresi gambar dari galeri")
            )

            val folderPrefix = if (isBanner) "banners" else "avatars"
            val safeUid = uid.take(12).replace(Regex("[^a-zA-Z0-9_-]"), "")
            val fileName = "${folderPrefix}_${safeUid}_${System.currentTimeMillis()}.webp"

            // 1. Primary Upload: Dinz Cloudflare R2 / CDN Node (https://cdn.dinzid.my.id/api/upload)
            val primaryUrl = uploadToDinzCdn(webpBytes, fileName)
            if (!primaryUrl.isNullOrBlank()) {
                Log.d(TAG, "Berhasil upload ke Dinz CDN: $primaryUrl (${webpBytes.size / 1024} KB)")
                return@withContext Result.success(primaryUrl)
            }

            // 2. Fallback Upload: Catbox / Qu.ax + Dinz Shortener Worker (https://url.dinzid.my.id/add)
            val fallbackRawUrl = uploadToCatboxFallback(webpBytes, fileName)
            if (!fallbackRawUrl.isNullOrBlank()) {
                val shortened = shortenWithDinzWorker(fallbackRawUrl) ?: fallbackRawUrl
                Log.d(TAG, "Berhasil upload via fallback CDN: $shortened")
                return@withContext Result.success(shortened)
            }

            Result.failure(IllegalStateException("Server CDN sedang sibuk, silakan coba beberapa saat lagi"))
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunggah gambar profil/banner: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun decodeAndCompressToWebP(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int
    ): ByteArray? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var sampleSize = 1
        while (srcWidth / (sampleSize * 2) >= maxWidth && srcHeight / (sampleSize * 2) >= maxHeight) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val scale = min(
            maxWidth.toFloat() / decodedBitmap.width.toFloat(),
            maxHeight.toFloat() / decodedBitmap.height.toFloat()
        ).coerceAtMost(1f)

        val targetBitmap = if (scale < 1f) {
            val targetW = (decodedBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (decodedBitmap.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true).also {
                if (it !== decodedBitmap) decodedBitmap.recycle()
            }
        } else {
            decodedBitmap
        }

        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        var quality = 85
        var resultBytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            targetBitmap.compress(compressFormat, quality, out)
            resultBytes = out.toByteArray()
            quality -= 12
        } while (resultBytes.size > MAX_BYTES && quality >= 45)

        targetBitmap.recycle()
        return resultBytes
    }

    private fun uploadToDinzCdn(webpBytes: ByteArray, fileName: String): String? {
        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    webpBytes.toRequestBody("image/webp".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://cdn.dinzid.my.id/api/upload")
                .header("User-Agent", "YuranimeApp/1.5 (Android)")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                if (!json.optBoolean("success", false)) return null
                val dataArr = json.optJSONArray("data") ?: return null
                if (dataArr.length() == 0) return null
                val firstItem = dataArr.optJSONObject(0) ?: return null
                val resultObj = firstItem.optJSONObject("result")
                val url = resultObj?.optString("url").takeUnless { it.isNullOrBlank() }
                    ?: firstItem.optString("url").takeUnless { it.isNullOrBlank() }
                url
            }
        } catch (e: Exception) {
            Log.w(TAG, "uploadToDinzCdn gagal: ${e.message}")
            null
        }
    }

    private fun uploadToCatboxFallback(webpBytes: ByteArray, fileName: String): String? {
        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart("userhash", "")
                .addFormDataPart(
                    "fileToUpload",
                    fileName,
                    webpBytes.toRequestBody("image/webp".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .header("User-Agent", "Mozilla/5.0 (Android 13; Mobile; rv:131.0) Gecko/131.0 Firefox/131.0")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val text = response.body?.string()?.trim() ?: return null
                if (text.startsWith("https://")) text else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "uploadToCatboxFallback gagal: ${e.message}")
            null
        }
    }

    private fun shortenWithDinzWorker(originalUrl: String): String? {
        return try {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val code = (1..7).map { chars[Random.nextInt(chars.length)] }.joinToString("")
            val payload = JSONObject().apply {
                put("code", code)
                put("url", originalUrl)
            }
            val request = Request.Builder()
                .url("https://url.dinzid.my.id/add")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    "https://url.dinzid.my.id/$code"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
