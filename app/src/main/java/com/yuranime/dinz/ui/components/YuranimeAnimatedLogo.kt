package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R
import com.yuranime.dinz.ui.theme.YuranimeLogoFontFamily
import kotlinx.coroutines.launch

/**
 * YuranimeAnimatedLogo: Komponen Logo Header Yuranime dengan Animasi Masuk & Interaksi Mikro
 * - Emblem bulat serigala dengan rotasi busur merah 60fps saat pertama kali dimuat.
 * - Efek sentuhan responsif (scale spring + rotasi putar busur) saat di-tap.
 * - Wordmark "Yuranime" menggunakan font resmi Calistoga Regular (YuranimeLogoFontFamily).
 */
@Composable
fun YuranimeAnimatedLogo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 34.dp,
    showWordmark: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val arcRotation = remember { Animatable(0f) }
    val emblemScale = remember { Animatable(0.85f) }
    val emblemAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    // Animasi Masuk saat pertama kali render
    LaunchedEffect(Unit) {
        emblemAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        emblemScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        arcRotation.animateTo(360f, animationSpec = tween(850, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                coroutineScope.launch {
                    // Tap micro-interaction: spring bounce & spin
                    emblemScale.animateTo(0.9f, animationSpec = tween(80))
                    emblemScale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                coroutineScope.launch {
                    arcRotation.animateTo(
                        arcRotation.value + 180f,
                        animationSpec = tween(500, easing = FastOutSlowInEasing)
                    )
                }
                onClick?.invoke()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emblem Container
        Box(
            modifier = Modifier
                .size(emblemSize)
                .scale(emblemScale.value)
                .graphicsLayer { alpha = emblemAlpha.value }
                .clip(CircleShape)
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            // Busur Merah Luar Kiri & Kanan (Rotated Layer)
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

            // Siluet Serigala Putih
            Image(
                painter = painterResource(id = R.drawable.ic_splash_wolf_white),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.86f)
            )

            // Segitiga Play Putih Inti
            Image(
                painter = painterResource(id = R.drawable.ic_splash_play_triangle),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.24f)
            )
        }

        if (showWordmark) {
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "Yuranime",
                color = Color.White,
                fontSize = 21.sp,
                fontFamily = YuranimeLogoFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
        }
    }
}
