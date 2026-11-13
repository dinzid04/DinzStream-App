package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private data class BadgeTierPalette(
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val wingDarkStart: Color,
    val wingDarkEnd: Color,
    val hexInnerDark: Color,
    val highlightLine: Color,
    val textTint: Color,
    val isMythic: Boolean = false
)

private fun resolveTierPalette(tierLevel: Int): BadgeTierPalette {
    return when {
        // Level 31+ / Top Leaderboard (Mythic Sovereign Tier): Gold-Crimson (#F59E0B & #EF4444)
        tierLevel >= 31 -> BadgeTierPalette(
            primaryAccent = Color(0xFFF59E0B),
            secondaryAccent = Color(0xFFEF4444),
            wingDarkStart = Color(0xFF2B1210),
            wingDarkEnd = Color(0xFF170B0D),
            hexInnerDark = Color(0xFF1E0D0C),
            highlightLine = Color(0xFFFDE68A),
            textTint = Color(0xFFFFFBEB),
            isMythic = true
        )
        // Level 16–30 (Royal Amethyst Tier): Neon Violet-Crimson (#A855F7 & #EC4899)
        tierLevel in 16..30 -> BadgeTierPalette(
            primaryAccent = Color(0xFFA855F7),
            secondaryAccent = Color(0xFFEC4899),
            wingDarkStart = Color(0xFF221030),
            wingDarkEnd = Color(0xFF120B1D),
            hexInnerDark = Color(0xFF170B24),
            highlightLine = Color(0xFFE9D5FF),
            textTint = Color(0xFFFAF5FF)
        )
        // Level 6–15 (Vanguard Tier): Blaze Orange-Red (#FF5722 & #E11D48)
        tierLevel in 6..15 -> BadgeTierPalette(
            primaryAccent = Color(0xFFFF5722),
            secondaryAccent = Color(0xFFE11D48),
            wingDarkStart = Color(0xFF2A1015),
            wingDarkEnd = Color(0xFF140A0D),
            hexInnerDark = Color(0xFF1C0B0F),
            highlightLine = Color(0xFFFFCCBC),
            textTint = Color(0xFFFFF1F2)
        )
        // Level 1–5 (Starter Tier): Crimson Slate (#F43F5E & #881337)
        else -> BadgeTierPalette(
            primaryAccent = Color(0xFFF43F5E),
            secondaryAccent = Color(0xFF881337),
            wingDarkStart = Color(0xFF241018),
            wingDarkEnd = Color(0xFF110B10),
            hexInnerDark = Color(0xFF190B11),
            highlightLine = Color(0xFFFDA4AF),
            textTint = Color(0xFFFFF1F2)
        )
    }
}

/**
 * TAHAP 2: Komponen Vektor Custom Title Badge (`1002213249.jpg` `set:nAFID5xQYdM`)
 * Menggambar Emblem Heksagon berlapis di sisi kiri yang menyatu dengan sayap banner
 * poligon meruncing (`chevron/hexagonal tech banner`) di sisi kanan.
 */
@Composable
fun GeometricTitleBadge(
    titleName: String,
    tierLevel: Int,
    modifier: Modifier = Modifier,
    styleVariant: Int = 0,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = remember(tierLevel) { resolveTierPalette(tierLevel) }
    val heightDp: Dp = if (compact) 22.dp else 30.dp
    val hexWidthDp: Dp = if (compact) 24.dp else 32.dp
    val fontSize: TextUnit = if (compact) 9.sp else 11.sp
    val hexFontSize: TextUnit = if (compact) 7.5.sp else 9.sp

    // Shimmer halus khusus untuk Tier 31+ (Mythic Sovereign Tier)
    val shimmerProgress = if (palette.isMythic) {
        val transition = rememberInfiniteTransition(label = "mythicTitleShimmer")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "mythicShimmerOffset"
        )
        anim
    } else {
        0.5f
    }

    val clickableModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        modifier
    }

    Box(
        modifier = clickableModifier
            .height(heightDp)
            .defaultMinSize(minWidth = if (compact) 104.dp else 148.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val hexRadius = h * 0.48f
            val hexCenterX = hexRadius * 1.05f

            val accentBrush = Brush.linearGradient(
                colors = listOf(palette.primaryAccent, palette.secondaryAccent),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val wingFillBrush = Brush.horizontalGradient(
                colors = listOf(
                    palette.wingDarkStart,
                    palette.wingDarkEnd,
                    palette.secondaryAccent.copy(alpha = 0.28f)
                ),
                startX = hexCenterX,
                endX = w
            )

            // 1. Sayap Banner Poligon Kanan (Menyatu dari belakang heksagon kiri)
            val wingTopY = h * 0.14f
            val wingBottomY = h * 0.86f
            val wingHeight = wingBottomY - wingTopY
            val cutSize = wingHeight * 0.46f
            val wingStartX = hexCenterX + hexRadius * 0.35f
            val wingEndX = w - 2.dp.toPx()

            val wingPath = Path().apply {
                when (styleVariant % 4) {
                    0 -> {
                        // Varian 0: Bevel tajam kanan-bawah + bahu atas meruncing (seperti baris 1 referensi)
                        moveTo(wingStartX, wingTopY)
                        lineTo(wingEndX - cutSize * 0.45f, wingTopY)
                        lineTo(wingEndX, cy)
                        lineTo(wingEndX - cutSize * 0.85f, wingBottomY)
                        lineTo(wingStartX, wingBottomY)
                        close()
                    }
                    1 -> {
                        // Varian 1: Sayap trapesium mecha dengan takik sudut atas & bawah (baris 2 referensi)
                        moveTo(wingStartX, wingTopY)
                        lineTo(wingEndX - cutSize * 0.9f, wingTopY)
                        lineTo(wingEndX, wingTopY + cutSize * 0.65f)
                        lineTo(wingEndX - cutSize * 0.35f, wingBottomY)
                        lineTo(wingStartX, wingBottomY)
                        close()
                    }
                    2 -> {
                        // Varian 2: Sayap heksagonal simetris meruncing tajam (baris 3 referensi)
                        moveTo(wingStartX, wingTopY)
                        lineTo(wingEndX - cutSize * 0.7f, wingTopY)
                        lineTo(wingEndX, cy)
                        lineTo(wingEndX - cutSize * 0.7f, wingBottomY)
                        lineTo(wingStartX, wingBottomY)
                        close()
                    }
                    else -> {
                        // Varian 3: Sayap miring agresif bergaya chevron (baris 4 referensi)
                        moveTo(wingStartX, wingTopY)
                        lineTo(wingEndX, wingTopY)
                        lineTo(wingEndX - cutSize * 0.85f, wingBottomY)
                        lineTo(wingStartX, wingBottomY)
                        close()
                    }
                }
            }

            drawPath(path = wingPath, brush = wingFillBrush)
            drawPath(
                path = wingPath,
                brush = accentBrush,
                style = Stroke(width = 1.35.dp.toPx(), join = StrokeJoin.Miter)
            )

            // Garis aksen paralel di bagian atas & bawah dalam sayap
            val innerInset = 2.6.dp.toPx()
            drawLine(
                color = palette.highlightLine.copy(alpha = 0.42f),
                start = Offset(wingStartX + hexRadius * 0.55f, wingTopY + innerInset),
                end = Offset(wingEndX - cutSize * 1.15f, wingTopY + innerInset),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = palette.primaryAccent.copy(alpha = 0.35f),
                start = Offset(wingStartX + hexRadius * 0.55f, wingBottomY - innerInset),
                end = Offset(wingEndX - cutSize * 1.35f, wingBottomY - innerInset),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Aksen garis miring/chevron di ujung kanan sayap (khas set:nAFID5xQYdM)
            val chevronBaseX = wingEndX - cutSize * 1.05f
            for (i in 0..1) {
                val offsetX = i * 3.8.dp.toPx()
                drawLine(
                    color = palette.primaryAccent.copy(alpha = 0.65f - i * 0.2f),
                    start = Offset(chevronBaseX - offsetX, wingTopY + 3.dp.toPx()),
                    end = Offset(chevronBaseX - offsetX - 3.dp.toPx(), wingBottomY - 3.dp.toPx()),
                    strokeWidth = 1.1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Shimmer kilau lembut khusus Mythic Sovereign Tier (Lv. 31+)
            if (palette.isMythic) {
                val shimmerX = wingStartX + (wingEndX - wingStartX) * shimmerProgress
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.highlightLine.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(shimmerX, wingTopY),
                    end = Offset(shimmerX - 8.dp.toPx(), wingBottomY),
                    strokeWidth = 6.dp.toPx()
                )
            }

            // 2. Emblem Heksagon Berlapis di Sisi Kiri
            fun buildHexagonPath(cx: Float, centerY: Float, radius: Float): Path {
                return Path().apply {
                    for (i in 0 until 6) {
                        val angleDeg = 60.0 * i - 30.0
                        val angleRad = Math.toRadians(angleDeg)
                        val px = cx + (radius * cos(angleRad)).toFloat()
                        val py = centerY + (radius * sin(angleRad)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
            }

            val outerHexPath = buildHexagonPath(hexCenterX, cy, hexRadius)
            val midHexPath = buildHexagonPath(hexCenterX, cy, hexRadius * 0.80f)
            val innerHexPath = buildHexagonPath(hexCenterX, cy, hexRadius * 0.62f)

            // Layer luar heksagon dengan gradasi warna tier
            drawPath(path = outerHexPath, brush = accentBrush)
            // Layer tengah heksagon gelap
            drawPath(path = midHexPath, color = palette.hexInnerDark)
            // Garis kristal heksagon dalam
            drawPath(
                path = innerHexPath,
                color = palette.highlightLine.copy(alpha = 0.78f),
                style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
            )
            // Border luar heksagon tegas
            drawPath(
                path = outerHexPath,
                color = palette.highlightLine.copy(alpha = 0.85f),
                style = Stroke(width = 1.1.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // Konten Teks: Simbol/Angka Tier di Heksagon Kiri + Nama Title di Sayap Kanan
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = if (compact) 12.dp else 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(hexWidthDp)
                    .height(heightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (palette.isMythic) "★" else "✦",
                    color = palette.highlightLine,
                    fontSize = hexFontSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = SfProFontFamily
                )
            }

            Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))

            Text(
                text = titleName.uppercase(Locale.ROOT),
                color = palette.textTint,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = SfProFontFamily,
                letterSpacing = if (compact) 0.7.sp else 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * TAHAP 1: Kerangka Heksagonal + Kapsul Bergaris Putus-Putus (`dashed outline #334155`, `height = 30.dp`)
 * seperti pada referensi `1002213252.jpg` ketika pengguna belum memasang Title khusus.
 */
@Composable
fun EmptyTitleSlotBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strokeColor = Color(0xFF334155)
    Box(
        modifier = modifier
            .height(30.dp)
            .width(168.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val hexRadius = h * 0.46f
            val hexCenterX = hexRadius * 1.08f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            // Sayap kapsul/poligon kanan bergaris putus-putus
            val wingTopY = h * 0.17f
            val wingBottomY = h * 0.83f
            val wingStartX = hexCenterX + hexRadius * 0.68f
            val wingEndX = w - 2.dp.toPx()
            val cornerRadius = (wingBottomY - wingTopY) / 2f

            val capsulePath = Path().apply {
                moveTo(wingStartX, wingTopY)
                lineTo(wingEndX - cornerRadius, wingTopY)
                quadraticTo(wingEndX, wingTopY, wingEndX, cy)
                quadraticTo(wingEndX, wingBottomY, wingEndX - cornerRadius, wingBottomY)
                lineTo(wingStartX, wingBottomY)
            }

            drawPath(
                path = capsulePath,
                color = strokeColor,
                style = Stroke(
                    width = 1.25.dp.toPx(),
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Heksagon kiri bergaris putus-putus + lingkaran kecil di tengah (persis referensi 1002213252.jpg)
            val hexPath = Path().apply {
                for (i in 0 until 6) {
                    val angleRad = Math.toRadians(60.0 * i - 30.0)
                    val px = hexCenterX + (hexRadius * cos(angleRad)).toFloat()
                    val py = cy + (hexRadius * sin(angleRad)).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }

            drawPath(
                path = hexPath,
                color = strokeColor,
                style = Stroke(
                    width = 1.25.dp.toPx(),
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawCircle(
                color = strokeColor,
                radius = hexRadius * 0.42f,
                center = Offset(hexCenterX, cy),
                style = Stroke(
                    width = 1.1.dp.toPx(),
                    pathEffect = dashEffect
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 36.dp, end = 12.dp)
        ) {
            Text(
                text = "Pasang Title",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SfProFontFamily
            )
        }
    }
}
