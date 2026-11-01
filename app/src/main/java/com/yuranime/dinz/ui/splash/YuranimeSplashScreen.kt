package com.yuranime.dinz.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily

/**
 * 60fps Motion Graphics Splash Screen for Yuranime (0ms - 2600ms):
 * - Stage A (0ms - 600ms): Core White Play Triangle scales up & fades in at screen center on #000000.
 * - Stage B (600ms - 1400ms): Two Red Circular Arcs sweep & rotate around center while the White Wolf
 *   silhouette scales/fades in and merges with the Play triangle into the circular Yuranime emblem.
 * - Stage C (1400ms - 2200ms): Circular emblem slides smoothly to the left while the "Yuranime" wordmark
 *   (Calistoga Regular) slides & fades in character-by-character from left to right.
 * - Stage D (2200ms - 2600ms): Smooth crossfade + slight scale transition into the preloaded Home Screen.
 */
@Composable
fun YuranimeSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current

    // Master 60fps frame clock
    LaunchedEffect(Unit) {
        val startMs = withFrameMillis { it }
        while (true) {
            val nowMs = withFrameMillis { it }
            val current = (nowMs - startMs).coerceAtLeast(0L)
            elapsedMs = current
            if (current >= 2600L) {
                break
            }
        }
        onSplashFinished()
    }

    val t = elapsedMs.toFloat()

    // Stage A (0ms - 600ms): Play Triangle scale-up + fade-in
    val stageAProgress = ((t - 0f) / 600f).coerceIn(0f, 1f)
    val stageAEased = FastOutSlowInEasing.transform(stageAProgress)

    // Play Triangle stays visible in Stage A and early Stage B, then crossfades into the Wolf's
    // negative-space Play cutout between 900ms and 1350ms
    val playFadeOutProgress = ((t - 900f) / 450f).coerceIn(0f, 1f)
    val playAlpha = stageAEased * (1f - FastOutSlowInEasing.transform(playFadeOutProgress))
    val playScale = when {
        t < 420f -> 0.25f + 0.88f * FastOutSlowInEasing.transform((t / 420f).coerceIn(0f, 1f))
        t < 600f -> 1.13f - 0.13f * LinearOutSlowInEasing.transform(((t - 420f) / 180f).coerceIn(0f, 1f))
        else -> 1.0f
    }

    // Subtle dark halo pulse behind core during Stage A & B (matching reference video frames 3-5)
    val haloAlpha = when {
        t < 200f -> 0f
        t < 650f -> 0.55f * FastOutSlowInEasing.transform(((t - 200f) / 450f).coerceIn(0f, 1f))
        t < 1350f -> 0.55f * (1f - FastOutSlowInEasing.transform(((t - 650f) / 700f).coerceIn(0f, 1f)))
        else -> 0f
    }
    val haloScale = 0.5f + 0.75f * FastOutSlowInEasing.transform(((t - 200f) / 1100f).coerceIn(0f, 1f))

    // Stage B (600ms - 1400ms): Red Arcs rotation/sweep & Wolf silhouette reveal
    val stageBProgress = ((t - 600f) / 800f).coerceIn(0f, 1f)
    val stageBEased = FastOutSlowInEasing.transform(stageBProgress)

    val arcAlpha = ((t - 580f) / 380f).coerceIn(0f, 1f)
    val leftArcRotation = -150f * (1f - stageBEased)
    val rightArcRotation = 150f * (1f - stageBEased)
    val arcScale = 0.76f + 0.24f * stageBEased

    // Wolf silhouette reveal (700ms - 1400ms)
    val wolfProgress = ((t - 700f) / 700f).coerceIn(0f, 1f)
    val wolfEased = FastOutSlowInEasing.transform(wolfProgress)
    val wolfAlpha = wolfEased
    val wolfScale = 0.74f + 0.26f * wolfEased

    // Stage C (1400ms - 2200ms): Slide emblem left & staggered reveal of "Yuranime" wordmark
    val stageCProgress = ((t - 1400f) / 750f).coerceIn(0f, 1f)
    val stageCEased = FastOutSlowInEasing.transform(stageCProgress)

    // Emblem starts at screen center (0.dp) and shifts left (-96.dp) as wordmark expands on the right
    val emblemCenterShiftPx = with(density) { (-96).dp.toPx() } * stageCEased

    // Stage D (2200ms - 2600ms): Crossfade + slight scale into Home Screen
    val stageDProgress = ((t - 2200f) / 400f).coerceIn(0f, 1f)
    val stageDEased = FastOutSlowInEasing.transform(stageDProgress)
    val containerAlpha = 1f - stageDEased
    val containerScale = 1f + 0.06f * stageDEased

    val emblemSize = 86.dp
    val brandText = "Yuranime"

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = containerAlpha
                scaleX = containerScale
                scaleY = containerScale
            }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Centered Lockup Container: Emblem + Wordmark
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Wordmark "Yuranime" (appears to the right of the shifted emblem in Stage C)
            if (t >= 1400f) {
                Row(
                    modifier = Modifier
                        .offset(x = 46.dp, y = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    brandText.forEachIndexed { index, ch ->
                        val charStartMs = 1420f + index * 52f
                        val charProgress = ((t - charStartMs) / 340f).coerceIn(0f, 1f)
                        val charEased = FastOutSlowInEasing.transform(charProgress)
                        val slideOffsetPx = with(density) { (-18).dp.toPx() } * (1f - charEased)

                        Text(
                            text = ch.toString(),
                            fontFamily = YuranimeLogoFontFamily,
                            fontSize = 40.sp,
                            color = Color.White,
                            modifier = Modifier.graphicsLayer {
                                alpha = charEased
                                translationX = slideOffsetPx
                                scaleX = 0.90f + 0.10f * charEased
                                scaleY = 0.90f + 0.10f * charEased
                            }
                        )
                    }
                }
            }

            // 2. Circular Yuranime Emblem (starts at exact screen center, slides left in Stage C)
            Box(
                modifier = Modifier
                    .size(emblemSize)
                    .graphicsLayer {
                        translationX = emblemCenterShiftPx
                    },
                contentAlignment = Alignment.Center
            ) {
                // Subtle circular halo pulse in Stage A & early Stage B
                if (haloAlpha > 0.01f) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = haloAlpha
                                scaleX = haloScale
                                scaleY = haloScale
                            }
                    ) {
                        drawCircle(
                            color = Color(0xFF1E1F26),
                            radius = size.minDimension * 0.48f
                        )
                    }
                }

                // Dynamic Red Sweep Arc Trail during Stage B (600ms - 1250ms)
                if (t in 600f..1300f) {
                    val sweepTrailAlpha = (1f - ((t - 1050f) / 250f).coerceIn(0f, 1f)) * 0.75f
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = sweepTrailAlpha }
                    ) {
                        val strokeWidth = size.minDimension * 0.055f
                        val sweepAngle = 160f * stageBEased
                        drawArc(
                            color = Color(0xFFD8353E),
                            startAngle = 110f - 120f * (1f - stageBEased),
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFFD8353E),
                            startAngle = -45f + 120f * (1f - stageBEased),
                            sweepAngle = sweepAngle * 0.65f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Layer 2a: Left Red Circular Arc
                if (t >= 580f) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_red_arc_left),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = arcAlpha
                                rotationZ = leftArcRotation
                                scaleX = arcScale
                                scaleY = arcScale
                            }
                    )

                    // Layer 2b: Right Red Circular Arc
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_red_arc_right),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = arcAlpha
                                rotationZ = rightArcRotation
                                scaleX = arcScale
                                scaleY = arcScale
                            }
                    )
                }

                // Layer 3: White Wolf/Fox Silhouette
                if (t >= 700f) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_wolf_white),
                        contentDescription = "Yuranime Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = wolfAlpha
                                scaleX = wolfScale
                                scaleY = wolfScale
                            }
                    )
                }

                // Layer 1: Core White Play Triangle (Stage A & early Stage B)
                if (playAlpha > 0.01f) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_play_triangle),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = playAlpha
                                scaleX = playScale
                                scaleY = playScale
                            }
                    )
                }
            }
        }
    }
}
