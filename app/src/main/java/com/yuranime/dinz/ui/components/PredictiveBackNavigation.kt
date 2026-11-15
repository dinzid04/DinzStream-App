package com.yuranime.dinz.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Spesifikasi transisi Shared Axis X + Spring lembut 60/120fps untuk perpindahan halaman Yuranime.
 */
object YuranimeTransitions {
    fun sharedAxisXEnter(durationMs: Int = 310): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> (fullWidth * 0.28f).roundToInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = (durationMs * 0.78f).roundToInt(), easing = LinearOutSlowInEasing)
        ) + scaleIn(
            animationSpec = spring(dampingRatio = 0.90f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.96f
        )

    fun sharedAxisXExit(durationMs: Int = 250): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = durationMs, easing = FastOutLinearInEasing),
            targetOffsetX = { fullWidth -> (fullWidth * 0.34f).roundToInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = (durationMs * 0.76f).roundToInt())
        ) + scaleOut(
            animationSpec = tween(durationMillis = durationMs, easing = FastOutLinearInEasing),
            targetScale = 0.93f
        )

    fun subScreenEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            initialOffsetX = { fullWidth -> (fullWidth * 0.24f).roundToInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 230, easing = LinearOutSlowInEasing)
        ) + scaleIn(
            animationSpec = spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.97f
        )

    fun subScreenPopExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing),
            targetOffsetX = { fullWidth -> (fullWidth * 0.28f).roundToInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 190)
        ) + scaleOut(
            animationSpec = tween(durationMillis = 240, easing = FastOutLinearInEasing),
            targetScale = 0.94f
        )
}

/**
 * Permukaan GPU-accelerated (graphicsLayer) yang merespons Predictive Back Gesture secara real-time:
 * - Mengikuti progres jari 0f..1f tanpa recomposition per-frame.
 * - Mengecilkan skala (1.0f -> 0.90f), membulatkan sudut (0.dp -> 28.dp),
 *   dan menggeser paralaks sesuai sisi tepi usapan (kiri / kanan).
 * - Membuka celah scrim redup di belakang agar halaman sebelumnya tampak langsung di bawahnya.
 */
@Composable
fun PredictiveBackSurface(
    enabled: Boolean,
    isActiveVisible: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val swipeProgress = remember { Animatable(0f) }
    val touchYOffset = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var initialTouchY by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(isActiveVisible) {
        if (isActiveVisible) {
            swipeProgress.snapTo(0f)
            touchYOffset.snapTo(0f)
            initialTouchY = -1f
        }
    }

    PredictiveBackHandler(enabled = enabled && isActiveVisible) { progressFlow: Flow<BackEventCompat> ->
        initialTouchY = -1f
        try {
            progressFlow.collect { event ->
                if (initialTouchY < 0f) {
                    initialTouchY = event.touchY
                }
                swipeEdge = event.swipeEdge
                val p = event.progress.coerceIn(0f, 1f)
                swipeProgress.snapTo(p)
                val deltaY = ((event.touchY - initialTouchY) * 0.18f).coerceIn(-44f, 44f)
                touchYOffset.snapTo(deltaY)
            }
            // Jangan snapTo(0f) sebelum exit animasi dimulai agar tidak kedip 1 frame saat dilepas
            onBack()
        } catch (_: CancellationException) {
            coroutineScope {
                launch {
                    swipeProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    touchYOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    val p = swipeProgress.value
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim redup tipis pada layar sebelumnya di belakang: menipis real-time saat diusap
        if (p > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (0.44f * (1f - p)).coerceIn(0f, 0.44f)))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = swipeProgress.value
                    val scale = 1f - (0.10f * progress)
                    val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                    scaleX = scale
                    scaleY = scale
                    translationX = direction * (size.width * 0.10f * progress)
                    translationY = touchYOffset.value * progress
                    if (progress > 0.002f) {
                        clip = true
                        shape = RoundedCornerShape((28f * progress).dp)
                        shadowElevation = 24.dp.toPx() * (1f - progress * 0.4f)
                    } else {
                        clip = false
                        shadowElevation = 0f
                    }
                }
                .background(Color(0xFF0B0B0B))
        ) {
            content()
        }
    }
}

/**
 * Pembungkus overlay bernilai nullable (seperti detail anime, reader komik, player streaming)
 * yang mempertahankan instance terakhir selama animasi exit & Predictive Back berlangsung.
 */
@Composable
fun <T> PredictiveBackItemOverlay(
    targetItem: T?,
    isTopmost: Boolean,
    onBack: () -> Unit,
    exitDurationMs: Int = 250,
    content: @Composable (T) -> Unit
) {
    val visibleState = remember { MutableTransitionState(targetItem != null) }
    visibleState.targetState = targetItem != null

    var retainedItem by remember { mutableStateOf(targetItem) }
    if (targetItem != null) {
        retainedItem = targetItem
    } else if (!visibleState.currentState && !visibleState.targetState) {
        retainedItem = null
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = YuranimeTransitions.sharedAxisXEnter(),
        exit = YuranimeTransitions.sharedAxisXExit(durationMs = exitDurationMs)
    ) {
        val activeItem = targetItem ?: retainedItem
        if (activeItem != null) {
            PredictiveBackSurface(
                enabled = isTopmost,
                isActiveVisible = targetItem != null,
                onBack = onBack
            ) {
                key(activeItem) {
                    content(activeItem)
                }
            }
        }
    }
}

/**
 * Pembungkus overlay bertipe boolean (seperti GlobalChatScreen / ComicViewAllScreen)
 * dengan dukungan penuh Predictive Back real-time + transisi Shared Axis X.
 */
@Composable
fun PredictiveBackVisibilityOverlay(
    visible: Boolean,
    isTopmost: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = YuranimeTransitions.sharedAxisXEnter(),
        exit = YuranimeTransitions.sharedAxisXExit()
    ) {
        PredictiveBackSurface(
            enabled = isTopmost,
            isActiveVisible = visible,
            onBack = onBack
        ) {
            content()
        }
    }
}
