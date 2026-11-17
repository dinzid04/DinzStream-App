package com.yuranime.dinz.ui.components

import android.graphics.BitmapFactory
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.yuranime.dinz.data.firebase.ProfileEffectDefinition
import com.yuranime.dinz.data.firebase.ProfileEffectLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

private fun isValidPngSignature(file: File): Boolean {
    if (!file.exists() || file.length() < 32L) return false
    return try {
        file.inputStream().use { stream ->
            val header = ByteArray(8)
            if (stream.read(header) != 8) return false
            header[0] == 0x89.toByte() &&
                header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() &&
                header[3] == 0x47.toByte()
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

    val sanitizedUrl = remember(url) {
        val t = url.trim()
        if (t.startsWith("http://", ignoreCase = true)) {
            "https://" + t.removePrefix("http://")
        } else {
            t
        }
    }
    if (!sanitizedUrl.startsWith("https://", ignoreCase = true)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = when (scaleType) {
                ImageView.ScaleType.CENTER_CROP -> ContentScale.Crop
                ImageView.ScaleType.FIT_START -> ContentScale.FillWidth
                ImageView.ScaleType.FIT_XY -> ContentScale.FillBounds
                else -> ContentScale.Fit
            },
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    var apngFile by remember(sanitizedUrl) { mutableStateOf<File?>(null) }
    var useApng by remember(sanitizedUrl) { mutableStateOf(true) }
    val viewRef = remember(sanitizedUrl) {
        object {
            var drawable: APNGDrawable? = null
            var imageView: ImageView? = null
        }
    }

    LaunchedEffect(sanitizedUrl) {
        onLoadingChanged(true)
        val file = withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "apng_cache").apply { if (!exists()) mkdirs() }
                val safeHash = sanitizedUrl.hashCode().toString().replace("-", "n")
                val targetFile = File(cacheDir, "apng_$safeHash.png")

                if (targetFile.exists() && targetFile.length() >= 128L) {
                    return@withContext targetFile
                }

                val tempFile = File(cacheDir, "apng_${safeHash}_${System.currentTimeMillis()}_tmp.png")
                val request = Request.Builder()
                    .url(sanitizedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko)")
                    .build()

                sharedApngClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() >= 128L) {
                    if (targetFile.exists()) {
                        try { targetFile.delete() } catch (_: Throwable) {}
                    }
                    if (tempFile.renameTo(targetFile)) {
                        targetFile
                    } else {
                        try {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                            targetFile
                        } catch (_: Throwable) {
                            tempFile
                        }
                    }
                } else {
                    try { tempFile.delete() } catch (_: Throwable) {}
                    null
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }
        }

        if (file != null && file.exists() && file.length() >= 128L) {
            val isApng = withContext(Dispatchers.IO) { isApngFile(file) }
            apngFile = file
            useApng = isApng
        } else {
            useApng = false
        }
        onLoadingChanged(false)
    }

    DisposableEffect(sanitizedUrl) {
        onDispose {
            try {
                viewRef.drawable?.stop()
                viewRef.imageView?.setImageDrawable(null)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            viewRef.drawable = null
            viewRef.imageView = null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val file = apngFile
        if (useApng && file != null && file.exists() && file.length() >= 128L) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        this.scaleType = scaleType
                        this.isClickable = false
                        this.isFocusable = false
                        this.isEnabled = false
                        viewRef.imageView = this
                    }
                },
                update = { imageView ->
                    imageView.scaleType = scaleType
                    imageView.isClickable = false
                    imageView.isFocusable = false
                    imageView.isEnabled = false
                    viewRef.imageView = imageView
                    val targetPath = file.absolutePath
                    if (imageView.tag == targetPath && viewRef.drawable != null) {
                        return@AndroidView
                    }
                    try {
                        if (file.exists() && file.length() >= 128L) {
                            runCatching { viewRef.drawable?.stop() }
                            val drawable = APNGDrawable.fromFile(targetPath)
                            drawable.setLoopLimit(0) // 0 = Infinite loop
                            imageView.tag = targetPath
                            imageView.setImageDrawable(drawable)
                            drawable.start()
                            viewRef.drawable = drawable
                        } else {
                            imageView.tag = null
                            imageView.setImageDrawable(null)
                            imageView.post { useApng = false }
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        imageView.tag = null
                        try {
                            imageView.setImageDrawable(null)
                        } catch (_: Throwable) {}
                        // Fallback ke Coil di frame berikutnya agar tidak mutasi state saat update
                        imageView.post { useApng = false }
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
                    .data(sanitizedUrl)
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
 * MultiLayerProfileEffectView:
 * Engine pemutar animasi Profile Effect multi-layer 60 FPS sesuai skenario Anibi.
 * - Membaca seluruh layer dari ProfileEffectDefinition (intro character + ambient loop).
 * - Cache file unik per layer: "effect_${effectId}_layer_${layerIndex}_${hash}.png".
 * - Timeline Controller: memutar intro karakter (loop=false), lalu ambient loop (loop=true),
 *   dan mengulang siklus secara periodik (while(isActive)) agar karakter utama (Venom, Lord of the Dead, dll.)
 *   selalu muncul hidup dan tidak pernah hilang/mati!
 */
private class TopWidthCropImageView(context: android.content.Context) : ImageView(context) {
    private var lastScaledVw = -1
    private var lastScaledDw = -1

    init {
        super.setScaleType(ScaleType.MATRIX)
    }

    override fun setScaleType(scaleType: ScaleType?) {
        // Pertahankan ScaleType.MATRIX agar efek profil selalu 100% full-width dan menempel di atas (y = 0)
        if (this.scaleType != ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX)
        }
        applyTopWidthMatrix()
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        lastScaledDw = -1
        super.setImageDrawable(drawable)
        applyTopWidthMatrix()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        applyTopWidthMatrix()
        return changed
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTopWidthMatrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyTopWidthMatrix()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            applyTopWidthMatrix()
        }
    }

    fun refreshTopWidthMatrix() {
        lastScaledVw = -1
        applyTopWidthMatrix()
    }

    private fun applyTopWidthMatrix() {
        runCatching {
            val d = drawable ?: return
            val vw = width - paddingLeft - paddingRight
            val dw = d.intrinsicWidth.takeIf { it > 0 } ?: d.bounds.width()
            if (vw <= 0 || dw <= 0) {
                return
            }
            if (vw == lastScaledVw && dw == lastScaledDw) return
            lastScaledVw = vw
            lastScaledDw = dw
            val scale = vw.toFloat() / dw.toFloat()
            val m = android.graphics.Matrix()
            m.setScale(scale, scale)
            imageMatrix = m
        }
    }
}

@Composable
fun MultiLayerProfileEffectView(
    layers: List<ProfileEffectLayer>,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    cropTopWidth: Boolean = false,
    cycleCooldownMs: Long = 2500L,
    effectId: String = "effect",
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val sortedLayers = remember(layers) { layers.sortedBy { it.zIndex } }
    if (sortedLayers.isEmpty()) {
        LaunchedEffect(Unit) { onLoadingChanged(false) }
        Box(modifier = modifier)
        return
    }

    val context = LocalContext.current
    var isReady by remember(sortedLayers) { mutableStateOf(false) }
    var cachedFiles by remember(sortedLayers) { mutableStateOf<List<File?>>(emptyList()) }

    // Download & Cache setiap layer dengan nama unik & validasi PNG header
    LaunchedEffect(sortedLayers) {
        onLoadingChanged(true)
        isReady = false
        val files = withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "apng_effects_cache").apply { if (!exists()) mkdirs() }
            val safeEffectId = effectId.replace(Regex("[^a-zA-Z0-9_]"), "_")

            sortedLayers.mapIndexed { layerIndex, layer ->
                try {
                    val rawSrc = layer.src.trim()
                    val safeSrc = if (rawSrc.startsWith("http://", ignoreCase = true)) {
                        "https://" + rawSrc.removePrefix("http://")
                    } else {
                        rawSrc
                    }
                    if (!safeSrc.startsWith("https://", ignoreCase = true)) return@mapIndexed null
                    val safeHash = safeSrc.hashCode().toString().replace("-", "n")
                    val targetFile = File(cacheDir, "effect_${safeEffectId}_layer_${layerIndex}_$safeHash.png")

                    if (targetFile.exists() && targetFile.length() >= 128L && isValidPngSignature(targetFile)) {
                        return@mapIndexed targetFile
                    }
                    if (targetFile.exists()) {
                        try { targetFile.delete() } catch (_: Throwable) {}
                    }

                    val tempFile = File(cacheDir, "effect_${safeEffectId}_layer_${layerIndex}_${safeHash}_tmp.png")
                    val request = Request.Builder()
                        .url(safeSrc)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36")
                        .build()

                    sharedApngClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@mapIndexed null
                        val body = response.body ?: return@mapIndexed null
                        FileOutputStream(tempFile).use { out ->
                            body.byteStream().copyTo(out)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() >= 128L && isValidPngSignature(tempFile)) {
                        try {
                            tempFile.copyTo(targetFile, overwrite = true)
                            tempFile.delete()
                            targetFile
                        } catch (_: Throwable) {
                            tempFile
                        }
                    } else {
                        try { tempFile.delete() } catch (_: Throwable) {}
                        null
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    null
                }
            }
        }

        cachedFiles = files
        isReady = files.any { it != null && it.exists() && it.length() >= 128L }
        onLoadingChanged(false)
    }

    if (!isReady || cachedFiles.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    // AndroidView dengan FrameLayout yang menampung ImageView per layer sesuai urutan zIndex
    val imageViews = remember(sortedLayers, cachedFiles) { mutableListOf<ImageView>() }
    val drawables = remember(sortedLayers, cachedFiles) { mutableListOf<APNGDrawable?>() }
    var isViewsAttached by remember(sortedLayers, cachedFiles) { mutableStateOf(false) }

    DisposableEffect(sortedLayers, cachedFiles) {
        onDispose {
            isViewsAttached = false
            drawables.forEach {
                try { it?.stop() } catch (_: Throwable) {}
            }
            imageViews.forEach {
                try { it.setImageDrawable(null) } catch (_: Throwable) {}
            }
            drawables.clear()
            imageViews.clear()
        }
    }

    // Timeline Controller: menunggu isReady dan isViewsAttached agar Cycle 0 selalu berjalan
    LaunchedEffect(isReady, isViewsAttached, sortedLayers, cachedFiles) {
        if (!isReady || !isViewsAttached || imageViews.isEmpty()) return@LaunchedEffect

        var cycleCount = 0
        while (isActive) {
            if (cycleCount > 0) {
                sortedLayers.forEachIndexed { index, _ ->
                    imageViews.getOrNull(index)?.visibility = View.INVISIBLE
                    try {
                        drawables.getOrNull(index)?.stop()
                    } catch (_: Throwable) {}
                }
            }

            val maxDuration = sortedLayers.maxOfOrNull { it.start + it.duration }?.coerceAtLeast(3500L) ?: 4000L

            coroutineScope {
                sortedLayers.forEachIndexed { index, layer ->
                    launch {
                        val iv = imageViews.getOrNull(index) ?: return@launch
                        val d = drawables.getOrNull(index)

                        if (layer.start > 0L) {
                            delay(layer.start)
                        }
                        if (!isActive) return@launch

                        iv.visibility = View.VISIBLE
                        if (iv is TopWidthCropImageView) {
                            iv.refreshTopWidthMatrix()
                        }
                        if (d != null) {
                            try {
                                d.setLoopLimit(if (layer.loop) 0 else 1)
                                if (cycleCount > 0) {
                                    d.reset()
                                }
                                d.start()
                            } catch (_: Throwable) {}
                        }

                        if (!layer.loop) {
                            delay(layer.duration.coerceAtLeast(1000L))
                            iv.visibility = View.INVISIBLE
                            try { d?.stop() } catch (_: Throwable) {}
                        }
                    }
                }

                if (cycleCooldownMs >= 5000L) {
                    delay(maxDuration)
                    imageViews.forEachIndexed { idx, view ->
                        view.visibility = View.INVISIBLE
                        try {
                            drawables.getOrNull(idx)?.stop()
                        } catch (_: Throwable) {}
                    }
                    delay(cycleCooldownMs)
                } else {
                    // Waktu tunggu ambient sebelum karakter utama muncul kembali
                    delay(maxDuration + cycleCooldownMs)
                }
            }
            cycleCount++
        }
    }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.TopCenter
    ) {
        key(cropTopWidth, sortedLayers, cachedFiles) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        isClickable = false
                        isFocusable = false
                        clipChildren = true
                        clipToPadding = true
                        imageViews.clear()
                        drawables.clear()

                        sortedLayers.forEachIndexed { index, layer ->
                            val file = cachedFiles.getOrNull(index)
                            val iv = (if (cropTopWidth) TopWidthCropImageView(ctx) else ImageView(ctx)).apply {
                                if (!cropTopWidth) {
                                    this.scaleType = scaleType
                                }
                                this.isClickable = false
                                this.isFocusable = false
                                this.isEnabled = false
                                this.visibility = View.INVISIBLE
                            }
                            addView(iv, FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ))
                            imageViews.add(iv)

                            var d: APNGDrawable? = null
                            if (file != null && file.exists() && file.length() >= 128L) {
                                try {
                                    val candidate = APNGDrawable.fromFile(file.absolutePath)
                                    if (candidate.intrinsicWidth > 0 && candidate.intrinsicHeight > 0) {
                                        candidate.setLoopLimit(if (layer.loop) 0 else 1)
                                        iv.setImageDrawable(candidate)
                                        d = candidate
                                    }
                                } catch (_: Throwable) {
                                    d = null
                                }
                                if (d == null) {
                                    try {
                                        BitmapFactory.decodeFile(file.absolutePath)?.let { bmp ->
                                            iv.setImageBitmap(bmp)
                                        }
                                    } catch (_: Throwable) {}
                                }
                            }
                            drawables.add(d)
                        }
                        post { isViewsAttached = true }
                    }
                },
                update = { frameLayout ->
                    frameLayout.isClickable = false
                    frameLayout.isFocusable = false
                    frameLayout.clipChildren = true
                    frameLayout.clipToPadding = true
                    // Pertahankan ScaleType.MATRIX pada TopWidthCropImageView agar tidak tertimpa FIT_CENTER
                    for (i in 0 until frameLayout.childCount) {
                        val child = frameLayout.getChildAt(i)
                        if (child is TopWidthCropImageView) {
                            child.refreshTopWidthMatrix()
                            child.isClickable = false
                            child.isFocusable = false
                        } else if (child is ImageView) {
                            child.scaleType = scaleType
                            child.isClickable = false
                            child.isFocusable = false
                        }
                    }
                }
            )
        }
    }
}

/**
 * Overload MultiLayerProfileEffectView yang menerima ProfileEffectDefinition langsung
 */
@Composable
fun MultiLayerProfileEffectView(
    effect: ProfileEffectDefinition?,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    cropTopWidth: Boolean = false,
    cycleCooldownMs: Long = 2500L,
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    if (effect == null) {
        Box(modifier = modifier)
        return
    }

    val layers = remember(effect) {
        effect.layers.ifEmpty {
            val layerUrls = effect.layerUrls.ifEmpty { listOf(effect.animationUrl) }
            layerUrls.filter { it.isNotBlank() }.mapIndexed { idx, url ->
                ProfileEffectLayer(
                    src = url,
                    loop = true,
                    start = 0L,
                    duration = 3800L,
                    zIndex = 100 + idx
                )
            }
        }
    }

    MultiLayerProfileEffectView(
        layers = layers,
        modifier = modifier,
        scaleType = scaleType,
        cropTopWidth = cropTopWidth,
        cycleCooldownMs = cycleCooldownMs,
        effectId = effect.id,
        onLoadingChanged = onLoadingChanged
    )
}

/**
 * ApngStackedEffectView:
 * Adapter kompatibilitas yang mendelegasikan ke MultiLayerProfileEffectView.
 */
@Composable
fun ApngStackedEffectView(
    layerUrls: List<String>,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER,
    cropTopWidth: Boolean = false,
    cycleCooldownMs: Long = 2500L,
    effectId: String = "stacked_effect",
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val validUrls = remember(layerUrls) { layerUrls.filter { it.isNotBlank() } }
    if (validUrls.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val layers = remember(validUrls) {
        validUrls.mapIndexed { idx, url ->
            ProfileEffectLayer(
                src = url,
                loop = true,
                start = 0L,
                duration = 3800L,
                zIndex = 100 + idx
            )
        }
    }

    MultiLayerProfileEffectView(
        layers = layers,
        modifier = modifier,
        scaleType = scaleType,
        cropTopWidth = cropTopWidth,
        cycleCooldownMs = cycleCooldownMs,
        effectId = effectId,
        onLoadingChanged = onLoadingChanged
    )
}
