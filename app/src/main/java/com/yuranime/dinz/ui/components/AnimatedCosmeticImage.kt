package com.yuranime.dinz.ui.components

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.data.repository.DiscordCosmeticsRepository

/**
 * AnimatedCosmeticImage:
 * Komponen render dekorasi kosmetik Discord (Border Avatar & Profile Effects).
 * - Ketika isAnimated == true:
 *   Menggunakan ApngAnimationView (APNGDrawable native 60 FPS hardware accelerated).
 * - Ketika isAnimated == false:
 *   Merender frame statis pertama saja menggunakan Coil secara hemat daya dan ringan.
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
        // Mode Animasi 60 FPS menggunakan ApngAnimationView
        val scaleType = when (contentScale) {
            ContentScale.Crop -> ImageView.ScaleType.CENTER_CROP
            ContentScale.FillBounds -> ImageView.ScaleType.FIT_XY
            else -> ImageView.ScaleType.FIT_CENTER
        }

        ApngAnimationView(
            url = url,
            modifier = modifier,
            scaleType = scaleType
        )
    }
}
