package com.yuranime.dinz.ui.streaming

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * In-App Floating Mini-Player (ala YouTube Premium):
 * - Dapat digeser bebas (draggable) ke sudut layar mana saja dan otomatis menempel halus ke sudut terdekat.
 * - Menyediakan kontrol Play/Pause, tombol X untuk menutup & menghentikan video, serta tap area video untuk expand.
 */
@OptIn(UnstableApi::class)
@Composable
fun InAppFloatingMiniPlayer(
    onExpand: (episodeId: String, movieId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta by StreamingPlayerHolder.metadata.collectAsStateWithLifecycle()
    val player = StreamingPlayerHolder.getPlayerOrNull() ?: return
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var livePosMs by remember { mutableLongStateOf(meta.positionMs) }
    var liveDurMs by remember { mutableLongStateOf(meta.durationMs) }

    LaunchedEffect(meta.episodeId, meta.isPlaying) {
        while (true) {
            livePosMs = player.currentPosition.coerceAtLeast(0L)
            val d = player.duration
            if (d > 0L) liveDurMs = d
            StreamingPlayerHolder.updatePlaybackProgress(livePosMs, liveDurMs, player.isPlaying)
            delay(350L)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val miniWidthDp = 188.dp
        val miniHeightDp = 106.dp
        val marginPx = with(density) { 12.dp.toPx() }
        val bottomReservedPx = with(density) { 82.dp.toPx() }
        val miniWidthPx = with(density) { miniWidthDp.toPx() }
        val miniHeightPx = with(density) { miniHeightDp.toPx() }

        val minX = marginPx
        val maxX = (constraints.maxWidth.toFloat() - miniWidthPx - marginPx).coerceAtLeast(minX)
        val minY = marginPx
        val maxY = (constraints.maxHeight.toFloat() - miniHeightPx - bottomReservedPx).coerceAtLeast(minY)

        val offsetX = remember { Animatable(maxX) }
        val offsetY = remember { Animatable(maxY) }

        LaunchedEffect(maxX, maxY) {
            // Pastikan posisi tetap di dalam batas aman bila ukuran layar berubah
            val clampedX = offsetX.value.coerceIn(minX, maxX)
            val clampedY = offsetY.value.coerceIn(minY, maxY)
            if (clampedX != offsetX.value) offsetX.snapTo(clampedX)
            if (clampedY != offsetY.value) offsetY.snapTo(clampedY)
        }

        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(width = miniWidthDp, height = miniHeightDp)
                .shadow(elevation = 12.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(Color.Black)
                .border(1.dp, Color(0x3DFFFFFF), shape)
                .pointerInput(minX, maxX, minY, maxY) {
                    detectTapGestures(
                        onTap = {
                            onExpand(meta.episodeId, meta.movieId)
                        }
                    )
                }
                .pointerInput(minX, maxX, minY, maxY) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount.x).coerceIn(minX, maxX))
                                offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(minY, maxY))
                            }
                        },
                        onDragEnd = {
                            val midX = (minX + maxX) * 0.5f
                            val midY = (minY + maxY) * 0.5f
                            val targetX = if (offsetX.value < midX) minX else maxX
                            val targetY = if (offsetY.value < midY) minY else maxY
                            scope.launch {
                                launch {
                                    offsetX.animateTo(
                                        targetValue = targetX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        targetValue = targetY,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            // Surface Video Aktif
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv ->
                    if (pv.player !== player) {
                        pv.player = player
                    }
                },
                onRelease = { pv ->
                    pv.player = null
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scrim tipis di atas & bawah agar ikon kontrol kontras
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x66000000),
                                Color.Transparent,
                                Color(0xAA000000)
                            )
                        )
                    )
            )

            // Label episode singkat di pojok kiri atas
            if (meta.animeTitle.isNotBlank() || meta.episodeNumber.isNotBlank()) {
                Text(
                    text = if (meta.episodeNumber.isNotBlank()) "Ep ${meta.episodeNumber}" else meta.animeTitle,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 7.dp, end = 34.dp)
                )
            }

            // Tombol "X" untuk menutup & menghentikan pemutaran sepenuhnya
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Mini Player",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Tombol Play / Pause di tengah
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .clickable { StreamingPlayerHolder.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (meta.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (meta.isPlaying) "Jeda" else "Putar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Progress bar putih tipis di tepi bawah mini-player
            val progressFrac = if (liveDurMs > 0L) (livePosMs.toFloat() / liveDurMs.toFloat()).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFrac)
                        .height(2.5.dp)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Tampilan khusus saat Activity masuk ke mode Picture-in-Picture (PiP) sistem Android.
 * Menampilkan surface video bersih penuh tanpa UI kontrol Compose karena kontrol disediakan langsung oleh sistem PiP Android.
 */
@OptIn(UnstableApi::class)
@Composable
fun SystemPipPlayerOverlay(modifier: Modifier = Modifier) {
    val player = StreamingPlayerHolder.getPlayerOrNull()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv ->
                    if (pv.player !== player) {
                        pv.player = player
                    }
                },
                onRelease = { pv ->
                    pv.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
