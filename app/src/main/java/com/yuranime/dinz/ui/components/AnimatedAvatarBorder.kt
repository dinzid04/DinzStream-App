package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yuranime.dinz.R

/**
 * Pilihan Preset Tema Border Bergerak (Avatar Decoration)
 */
enum class AvatarBorderTheme(
    val title: String,
    val colors: List<Color>,
    val durationMs: Int
) {
    KITSUNE_FLAME(
        title = "Kitsune Flame",
        colors = listOf(Color(0xFFFF5722), Color(0xFFFF9800), Color(0xFFFFD54F), Color(0xFFFF3D00), Color(0xFFFF5722)),
        durationMs = 3500
    ),
    NEON_CYBERPUNK(
        title = "Neon Cyberpunk",
        colors = listOf(Color(0xFF00F0FF), Color(0xFF3B82F6), Color(0xFFD946EF), Color(0xFF00F0FF)),
        durationMs = 2800
    ),
    CELESTIAL_GOLD(
        title = "Celestial Gold",
        colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFFFDE68A)),
        durationMs = 4500
    ),
    SAKURA_BLOOM(
        title = "Sakura Bloom",
        colors = listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFFFF1F2), Color(0xFFFDA4AF)),
        durationMs = 5000
    ),
    VOID_ABYSS(
        title = "Void Abyss",
        colors = listOf(Color(0xFFC084FC), Color(0xFF8B5CF6), Color(0xFF4C1D95), Color(0xFF38BDF8), Color(0xFFC084FC)),
        durationMs = 3200
    )
}

/**
 * AnimatedAvatarBorder: Komponen Avatar dengan Border Bergerak Murni Native Compose
 * Memberikan animasi rotasi sweep gradient 60-120fps tanpa dependensi video/file berat.
 */
@Composable
fun AnimatedAvatarBorder(
    avatarUrl: Any,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 68.dp,
    borderWidth: Dp = 3.5.dp,
    borderTheme: AvatarBorderTheme = AvatarBorderTheme.KITSUNE_FLAME,
    onClick: (() -> Unit)? = null
) {
    val totalSize = avatarSize + (borderWidth * 2) + 8.dp

    val infiniteTransition = rememberInfiniteTransition(label = "avatarBorderRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = borderTheme.durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )

    Box(
        modifier = modifier
            .size(totalSize)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1. Native Rotating Sweep Gradient Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .rotate(rotationAngle)
                .drawWithContent {
                    drawCircle(
                        brush = Brush.sweepGradient(borderTheme.colors),
                        style = Stroke(width = borderWidth.toPx())
                    )
                }
        )

        // 2. Avatar Image in Center
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime)
                    .error(R.drawable.placeholder_yuranime)
                    .build(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
