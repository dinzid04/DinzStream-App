package com.yuranime.dinz.ui.components

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.penfeizhou.animation.apng.APNGDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private val sharedApngClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

/**
 * Memeriksa apakah file adalah APNG yang valid dengan memindai magic bytes dan chunk acTL.
 */
private fun isApngFile(file: File): Boolean {
    if (!file.exists() || file.length() < 32L) return false
    return try {
        file.inputStream().use { stream ->
            val header = ByteArray(8)
            if (stream.read(header) != 8) return false
            // Magic bytes PNG: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
            if (header[0] != 0x89.toByte() || header[1] != 0x50.toByte() ||
                header[2] != 0x4E.toByte() || header[3] != 0x47.toByte()
            ) {
                return false
            }
            // Pindai 8KB pertama untuk menemukan chunk 'acTL' (Animation Control Chunk)
            val buffer = ByteArray(8192)
            val bytesRead = stream.read(buffer)
            if (bytesRead <= 0) return false
            // 'a'=0x61, 'c'=0x63, 'T'=0x54, 'L'=0x4C
            for (i in 0 until bytesRead - 3) {
                if (buffer[i] == 0x61.toByte() &&
                    buffer[i + 1] == 0x63.toByte() &&
                    buffer[i + 2] == 0x54.toByte() &&
                    buffer[i + 3] == 0x4C.toByte()
                ) {
                    return true
                }
            }
            false
        }
    } catch (_: Throwable) {
        false
    }
}

/**
 * ApngAnimationView:
 * Komponen pemutar APNG 60 FPS hardware-accelerated anti-crash.
 * - Memeriksa signature APNG sebelum di-decode.
 * - Menangani Throwable (Exception & OutOfMemoryError) dengan fallback otomatis ke Coil AsyncImage.
 * - Touch pass-through (isClickable = false, isFocusable = false).
 * - Membersihkan resource drawable dan memory saat unmount.
 */
@Composable
fun ApngAnimationView(
    url: String,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    if (url.isBlank()) {
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current
    var apngFile by remember(url) { mutableStateOf<File?>(null) }
    var useApng by remember(url) { mutableStateOf(true) }
    var currentDrawable by remember { mutableStateOf<APNGDrawable?>(null) }
    var currentImageView by remember { mutableStateOf<ImageView?>(null) }

    LaunchedEffect(url) {
        onLoadingChanged(true)
        val file = withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "apng_cache").apply { if (!exists()) mkdirs() }
                val safeHash = url.hashCode().toString().replace("-", "n")
                val targetFile = File(cacheDir, "apng_$safeHash.png")

                if (targetFile.exists() && targetFile.length() > 100L) {
                    return@withContext targetFile
                }

                val tempFile = File(cacheDir, "apng_${safeHash}_tmp.png")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko)")
                    .build()

                sharedApngClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 100L) {
                    if (tempFile.renameTo(targetFile)) {
                        targetFile
                    } else {
                        tempFile
                    }
                } else {
                    null
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }
        }

        if (file != null && file.exists() && file.length() > 100L) {
            val isApng = withContext(Dispatchers.IO) { isApngFile(file) }
            apngFile = file
            useApng = isApng
        } else {
            useApng = false
        }
        onLoadingChanged(false)
    }

    DisposableEffect(url) {
        onDispose {
            try {
                currentDrawable?.stop()
                currentImageView?.setImageDrawable(null)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            currentDrawable = null
            currentImageView = null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val file = apngFile
        if (useApng && file != null && file.exists()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        this.scaleType = scaleType
                        this.isClickable = false
                        this.isFocusable = false
                        this.isEnabled = false
                        currentImageView = this
                    }
                },
                update = { imageView ->
                    imageView.scaleType = scaleType
                    imageView.isClickable = false
                    imageView.isFocusable = false
                    imageView.isEnabled = false
                    currentImageView = imageView
                    try {
                        val drawable = APNGDrawable.fromFile(file.absolutePath)
                        drawable.setLoopLimit(0) // 0 = Infinite loop
                        imageView.setImageDrawable(drawable)
                        drawable.start()
                        currentDrawable = drawable
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        // Fallback ke Coil jika terjadi OOM atau error decode
                        useApng = false
                        try {
                            imageView.setImageDrawable(null)
                        } catch (_: Throwable) {}
                    }
                }
            )
        } else {
            // Fallback aman ke Coil jika file bukan APNG atau decode gagal
            val contentScale = when (scaleType) {
                ImageView.ScaleType.CENTER_CROP -> ContentScale.Crop
                ImageView.ScaleType.FIT_START -> ContentScale.FillWidth
                ImageView.ScaleType.FIT_XY -> ContentScale.FillBounds
                else -> ContentScale.Fit
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * ApngStackedEffectView:
 * Merender maksimal 2 layer efek profil secara aman tanpa membebani RAM (anti-OOM).
 */
@Composable
fun ApngStackedEffectView(
    layerUrls: List<String>,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val validLayers = remember(layerUrls) {
        layerUrls.filter { it.isNotBlank() }.take(2)
    }

    if (validLayers.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        validLayers.forEachIndexed { index, layerUrl ->
            ApngAnimationView(
                url = layerUrl,
                modifier = Modifier.fillMaxSize(),
                scaleType = scaleType,
                onLoadingChanged = if (index == 0) onLoadingChanged else { _ -> }
            )
        }
    }
}
