package com.yuranime.dinz.ui.components

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
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
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * AnimatedCosmeticImage:
 * Komponen render dekorasi kosmetik Discord (Border Avatar & Profile Effects).
 * - Ketika isAnimated == true:
 *   Menggunakan APNGDrawable (untuk file APNG .png Discord) atau AnimatedImageDrawable (WebP/GIF)
 *   pada 60 FPS mulus dan otomatis memanggil drawable.start()!
 * - Ketika isAnimated == false:
 *   Merender frame statis pertama saja secara hemat daya dan ringan.
 * - Menggunakan disk cache instan (cosmetics_cache) dan optimasi parameter ?size=160.
 */
@Composable
fun AnimatedCosmeticImage(
    url: String,
    isAnimated: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (url.isBlank()) {
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val isApngUrl = remember(url) {
        url.contains("avatar-decoration", ignoreCase = true) ||
            url.contains(".png", ignoreCase = true)
    }

    if (!isAnimated) {
        // Mode Statis: Gunakan frame pertama statis
        val staticUrl = remember(url) {
            DiscordCosmeticsRepository.getOptimizedUrl(url, size = 160, animated = false)
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(staticUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        // Mode Animasi 60 FPS: APNGDrawable untuk APNG / Coil Animatable untuk WebP/GIF
        var currentAnimatable by remember { mutableStateOf<Animatable?>(null) }
        var currentApngDrawable by remember { mutableStateOf<APNGDrawable?>(null) }
        var apngFile by remember(url) { mutableStateOf<File?>(null) }

        // Prefetch & Cache APNG File ke local cache directory untuk pemutaran 60fps tanpa lag
        LaunchedEffect(url) {
            if (isApngUrl) {
                withContext(Dispatchers.IO) {
                    try {
                        val optimizedUrl = DiscordCosmeticsRepository.getOptimizedUrl(url, size = 160, animated = true)
                        val cacheDir = context.cacheDir.resolve("cosmetics_cache")
                        if (!cacheDir.exists()) cacheDir.mkdirs()

                        // Hash filename untuk disk cache instan
                        val hash = optimizedUrl.hashCode().toString().replace("-", "n")
                        val targetFile = cacheDir.resolve("apng_$hash.png")

                        if (!targetFile.exists() || targetFile.length() == 0L) {
                            val tempFile = cacheDir.resolve("apng_${hash}_tmp.png")
                            URL(optimizedUrl).openStream().use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            tempFile.renameTo(targetFile)
                        }

                        if (targetFile.exists() && targetFile.length() > 0L) {
                            withContext(Dispatchers.Main) {
                                apngFile = targetFile
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        DisposableEffect(url) {
            onDispose {
                try {
                    currentAnimatable?.stop()
                    currentApngDrawable?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                currentAnimatable = null
                currentApngDrawable = null
            }
        }

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            if (isApngUrl && apngFile != null) {
                // APNG Native Decoder View (60 FPS Hardware Accelerated)
                AndroidView(
                    modifier = modifier,
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = when (contentScale) {
                                ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
                                ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
                                else -> ImageView.ScaleType.FIT_CENTER
                            }
                        }
                    },
                    update = { imageView ->
                        try {
                            val file = apngFile
                            if (file != null && file.exists()) {
                                val drawable = APNGDrawable.fromFile(file.absolutePath)
                                drawable.setLoopLimit(0) // Infinite 60 FPS loop
                                imageView.setImageDrawable(drawable)
                                drawable.start()
                                currentApngDrawable = drawable
                                if (drawable is Animatable) {
                                    currentAnimatable = drawable
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            } else {
                // Coil Fallback / Animated WebP Profile Effects
                val optimizedUrl = remember(url) {
                    DiscordCosmeticsRepository.getOptimizedUrl(url, size = 160, animated = true)
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(optimizedUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    onSuccess = { success ->
                        val drawable = success.result.drawable
                        if (drawable is Animatable) {
                            drawable.start()
                            currentAnimatable = drawable
                        }
                    },
                    modifier = modifier
                )
            }
        }
    }
}
