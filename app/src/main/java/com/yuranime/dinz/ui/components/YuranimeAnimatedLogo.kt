package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * YuranimeAnimatedLogo: Logo Header Yuranime dengan Animasi Motion Graphics Berkala
 *
 * Mengikuti spesifikasi TAHAP 2:
 * 1. Siklus Awal: Teks Yuranime ngeslide keluar dari balik ikon rubah (translationX + alpha + clipToBounds)
 *    disertai efek kilau cahaya (glossy linear gradient shimmer sweep). Jeda tenang ~4–5 detik.
 * 2. Siklus Berkala:
 *    a. Sembunyikan/tarik masuk teks Yuranime terlebih dahulu (alpha -> 0, translationX -> masuk).
 *    b. Motion graphics pada Ikon Rubah & Busur Merah (busur berputar 360° sweep, siluet rubah pulse/scale).
 *    c. Teks Yuranime ngeslide keluar ke arah kanan dari balik ikon rubah, langsung disusul sapuan kilau shimmer sweep.
 *    d. Kembali ke jeda tenang selama ~4–5 detik.
 */
@Composable
fun YuranimeAnimatedLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 36.dp,
    showWordmark: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // State animasi ikon
    val arcRotation = remember { Animatable(0f) }
    val wolfScale = remember { Animatable(1f) }

    // State animasi teks
    val textSlideX = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(1f) }
    val shimmerProgress = remember { Animatable(0f) }

    // Loop siklus motion graphics berkala
    LaunchedEffect(Unit) {
        // Siklus Awal: Teks slide out dari balik ikon + shimmer sweep
        textSlideX.snapTo(-28f)
        textAlpha.snapTo(0f)
        shimmerProgress.snapTo(0f)

        launch { textAlpha.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing)) }
        textSlideX.animateTo(0f, animationSpec = tween(450, easing = FastOutSlowInEasing))

        // Glossy shimmer sweep melintas di permukaan teks
        shimmerProgress.animateTo(1f, animationSpec = tween(750, easing = LinearEasing))

        // Jeda tenang pertama
        delay(4500)

        // Siklus Berkala berikutnya
        while (isActive) {
            // 1. Sembunyikan / tarik masuk teks Yuranime terlebih dahulu
            launch { textAlpha.animateTo(0f, animationSpec = tween(280, easing = FastOutSlowInEasing)) }
            textSlideX.animateTo(-24f, animationSpec = tween(280, easing = FastOutSlowInEasing))

            delay(150)

            // 2. Jalankan animasi pada Ikon Rubah & Busur Merah
            arcRotation.snapTo(0f)
            launch {
                wolfScale.animateTo(1.10f, animationSpec = tween(320, easing = FastOutSlowInEasing))
                wolfScale.animateTo(1.0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
            }
            arcRotation.animateTo(360f, animationSpec = tween(700, easing = FastOutSlowInEasing))

            delay(100)

            // 3. Tepat setelah ikon selesai, teks ngeslide keluar ke arah kanan dari balik ikon
            shimmerProgress.snapTo(0f)
            launch { textAlpha.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing)) }
            textSlideX.animateTo(0f, animationSpec = tween(450, easing = FastOutSlowInEasing))

            // Langsung disusul sapuan kilau cahaya glossy shimmer
            shimmerProgress.animateTo(1f, animationSpec = tween(750, easing = LinearEasing))

            // 4. Jeda tenang ~4–5 detik
            delay(4800)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Micro-interaction manual saat di-tap pengguna
                coroutineScope.launch {
                    wolfScale.animateTo(0.92f, animationSpec = tween(80))
                    wolfScale.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                }
                coroutineScope.launch {
                    arcRotation.animateTo(arcRotation.value + 180f, animationSpec = tween(450, easing = FastOutSlowInEasing))
                    shimmerProgress.snapTo(0f)
                    shimmerProgress.animateTo(1f, animationSpec = tween(600, easing = LinearEasing))
                }
                onClick?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Ikon Serigala/Rubah + Busur Merah
        Box(
            modifier = Modifier
                .size(emblemSize)
                .clip(CircleShape)
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            // Busur Merah Luar Kiri & Kanan (Sweep Rotation)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = arcRotation.value }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_red_arc_left),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_red_arc_right),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Siluet Serigala Putih (Pulse Scale)
            Image(
                painter = painterResource(id = R.drawable.ic_splash_wolf_white),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.86f)
                    .scale(wolfScale.value)
            )

            // Segitiga Play Putih Inti
            Image(
                painter = painterResource(id = R.drawable.ic_splash_play_triangle),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.24f)
            )
        }

        // 2. Teks Yuranime (Slide dari balik ikon + Glossy Shimmer Sweep)
        if (showWordmark) {
            Box(
                modifier = Modifier
                    .clipToBounds()
                    .padding(start = 8.dp)
            ) {
                val p = shimmerProgress.value
                val hasActiveShimmer = p > 0f && p < 1f

                Text(
                    text = "Yuranime",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontFamily = YuranimeLogoFontFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset { IntOffset(x = textSlideX.value.dp.roundToPx(), y = 0) }
                        .graphicsLayer { alpha = textAlpha.value }
                        .then(
                            if (hasActiveShimmer) {
                                Modifier.drawWithContent {
                                    drawContent()
                                    // Sapuan berkilau glossy shimmer gradient yang melintas di permukaan teks
                                    val sweepX = size.width * (p * 2.2f - 0.6f)
                                    val shimmerWidth = size.width * 0.45f
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0x99FFFFFF),
                                                Color(0xEEFFFFFF),
                                                Color(0x99FFFFFF),
                                                Color.Transparent
                                            ),
                                            start = Offset(sweepX - shimmerWidth, 0f),
                                            end = Offset(sweepX + shimmerWidth, size.height)
                                        ),
                                        blendMode = BlendMode.SrcAtop
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}
