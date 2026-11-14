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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
 * ApngAnimationView:
 * Komponen pemutar APNG 60 FPS hardware accelerated Discord menggunakan library Penfeizhou APNGDrawable.
 * Mendownload via OkHttpClient, menyimpan ke disk cache `context.cacheDir/apng_cache/`,
 * dan memutar secara mulus (setLoopLimit(0) + start()).
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
    var currentDrawable by remember { mutableStateOf<APNGDrawable?>(null) }

    LaunchedEffect(url) {
        onLoadingChanged(true)
        val file = withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "apng_cache").apply { if (!exists()) mkdirs() }
                val safeHash = url.hashCode().toString().replace("-", "n")
                val targetFile = File(cacheDir, "apng_$safeHash.png")

                if (targetFile.exists() && targetFile.length() > 0L) {
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

                if (tempFile.exists() && tempFile.length() > 0L) {
                    if (tempFile.renameTo(targetFile)) {
                        targetFile
                    } else {
                        tempFile
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        apngFile = file
        onLoadingChanged(false)
    }

    DisposableEffect(url) {
        onDispose {
            try {
                currentDrawable?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            currentDrawable = null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val file = apngFile
        if (file != null && file.exists()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        this.scaleType = scaleType
                    }
                },
                update = { imageView ->
                    imageView.scaleType = scaleType
                    try {
                        val drawable = APNGDrawable.fromFile(file.absolutePath)
                        drawable.setLoopLimit(0) // 0 = Infinite loop
                        imageView.setImageDrawable(drawable)
                        drawable.start()
                        currentDrawable = drawable
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}

/**
 * ApngStackedEffectView:
 * Merender layer APNG bertumpuk untuk efek profil multi-layer Discord.
 */
@Composable
fun ApngStackedEffectView(
    layerUrls: List<String>,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    if (layerUrls.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        layerUrls.forEach { layerUrl ->
            ApngAnimationView(
                url = layerUrl,
                modifier = Modifier.fillMaxSize(),
                scaleType = scaleType,
                onLoadingChanged = onLoadingChanged
            )
        }
    }
}
