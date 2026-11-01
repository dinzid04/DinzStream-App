package com.yuranime.dinz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Pilihan Tema Efek Masuk Profil (Profile Entrance Effect)
 */
enum class ProfileEntranceTheme(
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    KITSUNE_BURST(
        primaryColor = Color(0xFFFF5722),
        secondaryColor = Color(0xFFFF9800),
        glowColor = Color(0xFFFFD54F)
    ),
    CELESTIAL_NEBULA(
        primaryColor = Color(0xFFFBBF24),
        secondaryColor = Color(0xFF8B5CF6),
        glowColor = Color(0xFFFFFBEB)
    ),
    CYBER_OVERDRIVE(
        primaryColor = Color(0xFF00F0FF),
        secondaryColor = Color(0xFFD946EF),
        glowColor = Color(0xFF38BDF8)
    )
}

/**
 * ProfileEntranceEffect: Efek visual sinematik seperti video saat profil dibuka
 * Menggabungkan shockwave gelombang energi, pendaran cahaya, dan partikel melayang.
 */
@Composable
fun ProfileEntranceEffect(
    isActive: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    theme: ProfileEntranceTheme = ProfileEntranceTheme.KITSUNE_BURST,
    durationMs: Int = 2200
) {
    val shockwaveProgress1 = remember { Animatable(0f) }
    val shockwaveProgress2 = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            flashAlpha.animateTo(0.6f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            flashAlpha.animateTo(0f, animationSpec = tween(600, easing = LinearEasing))
            shockwaveProgress1.animateTo(1f, animationSpec = tween(durationMs, easing = FastOutSlowInEasing))
            onFinished()
        } else {
            shockwaveProgress1.snapTo(0f)
            shockwaveProgress2.snapTo(0f)
            flashAlpha.snapTo(0f)
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            delay(350)
            shockwaveProgress2.animateTo(1f, animationSpec = tween(durationMs - 350, easing = FastOutSlowInEasing))
        }
    }

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.28f)
            val maxRadius = size.width * 1.1f

            // 1. Central Ambient Flash
            if (flashAlpha.value > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.glowColor.copy(alpha = flashAlpha.value), Color.Transparent),
                        center = center,
                        radius = maxRadius * 0.7f
                    ),
                    center = center,
                    radius = maxRadius * 0.7f
                )
            }

            // 2. Shockwave 1 (Primary Ring)
            if (shockwaveProgress1.value > 0f && shockwaveProgress1.value < 1f) {
                val p = shockwaveProgress1.value
                val currentRadius = p * maxRadius
                val alpha = (1f - p).coerceIn(0f, 1f)

                drawCircle(
                    color = theme.primaryColor.copy(alpha = alpha * 0.8f),
                    center = center,
                    radius = currentRadius,
                    style = Stroke(width = (6.dp.toPx() * (1f - p * 0.5f)))
                )
            }

            // 3. Shockwave 2 (Secondary Ring with Delay)
            if (shockwaveProgress2.value > 0f && shockwaveProgress2.value < 1f) {
                val p = shockwaveProgress2.value
                val currentRadius = p * maxRadius * 0.85f
                val alpha = (1f - p).coerceIn(0f, 1f)

                drawCircle(
                    color = theme.secondaryColor.copy(alpha = alpha * 0.75f),
                    center = center,
                    radius = currentRadius,
                    style = Stroke(width = (4.dp.toPx() * (1f - p * 0.5f)))
                )
            }
        }
    }
}
