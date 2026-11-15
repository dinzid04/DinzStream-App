package com.yuranime.dinz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Daftar preset warna centang biru (Verified Badge) yang disinkronkan dengan Web Admin Console.
 */
val VERIFIED_BADGE_DEFAULT_HEX = "#1D9BF0"

/**
 * Mengurai string hex (`#1D9BF0`, `#F59E0B`, `#E11D48`, dll.) menjadi [Color] secara aman.
 */
fun parseVerifiedBadgeColor(hexColor: String?): Color {
    val cleaned = hexColor?.trim()?.removePrefix("#").orEmpty()
    if (cleaned.length != 6 && cleaned.length != 8) {
        return Color(0xFF1D9BF0)
    }
    val parsed = cleaned.toLongOrNull(16) ?: return Color(0xFF1D9BF0)
    val argb = if (cleaned.length == 6) (0xFF000000L or parsed) else parsed
    return Color(argb.toInt())
}

/**
 * Menghitung luminance relatif agar tanda centang di tengah gear selalu memiliki kontras tinggi.
 */
private fun Color.relativeLuminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

/**
 * Segel Verifikasi "Centang Biru" berbentuk **Gear / Rosette Presisi 10-Gigi** dengan
 * tanda centang di bagian tengah serta dukungan warna kustom real-time dari Firestore.
 */
@Composable
fun VerifiedBadgeIcon(
    isVerified: Boolean,
    colorHex: String = VERIFIED_BADGE_DEFAULT_HEX,
    size: Dp = 15.dp,
    modifier: Modifier = Modifier,
    label: String? = "Akun Terverifikasi"
) {
    if (!isVerified) return

    val baseColor = remember(colorHex) { parseVerifiedBadgeColor(colorHex) }
    val lightTint = remember(baseColor) {
        Color(
            red = (baseColor.red + (1f - baseColor.red) * 0.34f).coerceIn(0f, 1f),
            green = (baseColor.green + (1f - baseColor.green) * 0.34f).coerceIn(0f, 1f),
            blue = (baseColor.blue + (1f - baseColor.blue) * 0.34f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
    val deepTint = remember(baseColor) {
        Color(
            red = (baseColor.red * 0.72f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.72f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 0.72f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
    val checkColor = remember(baseColor) {
        if (baseColor.relativeLuminance() > 0.64f) Color(0xFF0B0F19) else Color.White
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                if (label != null) {
                    contentDescription = label
                }
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val outerR = minOf(w, h) * 0.485f
        val innerR = minOf(w, h) * 0.375f

        // 1. Siluet Gear 10-gigi halus melingkar
        val teethCount = 10
        val gearPath = Path()
        val steps = teethCount * 4
        for (i in 0..steps) {
            val phase = i % 4
            val r = when (phase) {
                0, 1 -> outerR
                else -> innerR
            }
            val angleOffset = when (phase) {
                0 -> -0.14
                1 -> 0.14
                2 -> 0.32
                else -> 0.68
            }
            val toothIndex = (i / 4).toDouble()
            val angle = ((toothIndex + angleOffset) / teethCount) * (2.0 * PI) - (PI / 2.0)
            val px = cx + (cos(angle) * r).toFloat()
            val py = cy + (sin(angle) * r).toFloat()
            if (i == 0) {
                gearPath.moveTo(px, py)
            } else {
                gearPath.lineTo(px, py)
            }
        }
        gearPath.close()

        // Isi gradasi badan gear
        drawPath(
            path = gearPath,
            brush = Brush.linearGradient(
                colors = listOf(lightTint, baseColor, deepTint),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Border luar tipis untuk separasi pada latar belakang gelap/terang
        drawPath(
            path = gearPath,
            color = lightTint.copy(alpha = 0.65f),
            style = Stroke(width = (w * 0.045f).coerceAtLeast(0.8f), join = StrokeJoin.Round)
        )

        // Cincin hub gear tipis di bagian dalam
        drawCircle(
            color = checkColor.copy(alpha = 0.22f),
            radius = minOf(w, h) * 0.295f,
            center = Offset(cx, cy),
            style = Stroke(width = (w * 0.04f).coerceAtLeast(0.75f))
        )

        // 2. Tanda Centang presisi di tengah gear
        val checkPath = Path().apply {
            moveTo(cx - w * 0.18f, cy + h * 0.01f)
            lineTo(cx - w * 0.045f, cy + h * 0.145f)
            lineTo(cx + w * 0.20f, cy - h * 0.125f)
        }
        drawPath(
            path = checkPath,
            color = checkColor,
            style = Stroke(
                width = (w * 0.135f).coerceAtLeast(1.8f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
