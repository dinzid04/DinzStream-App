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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.components.TitleIconCatalog.drawTitleVectorIcon
import com.yuranime.dinz.ui.theme.JakartaSansFontFamily
import com.yuranime.dinz.ui.theme.JetBrainsMonoFontFamily
import com.yuranime.dinz.ui.theme.OutfitDisplayFontFamily
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
    val textTint: Color
)

private fun resolveTierPalette(tierLevel: Int): BadgeTierPalette {
    return when {
        tierLevel >= 35 -> BadgeTierPalette(
            primaryAccent = Color(0xFFF59E0B),
            secondaryAccent = Color(0xFFEF4444),
            wingDarkStart = Color(0xFF28120F),
            wingDarkEnd = Color(0xFF13090B),
            hexInnerDark = Color(0xFF1A0C0B),
            highlightLine = Color(0xFFFDE68A),
            textTint = Color(0xFFFFFBEB)
        )
        tierLevel in 20..34 -> BadgeTierPalette(
            primaryAccent = Color(0xFFA855F7),
            secondaryAccent = Color(0xFFEC4899),
            wingDarkStart = Color(0xFF20102D),
            wingDarkEnd = Color(0xFF100A19),
            hexInnerDark = Color(0xFF150A21),
            highlightLine = Color(0xFFE9D5FF),
            textTint = Color(0xFFFAF5FF)
        )
        tierLevel in 10..19 -> BadgeTierPalette(
            primaryAccent = Color(0xFF06B6D4),
            secondaryAccent = Color(0xFF3B82F6),
            wingDarkStart = Color(0xFF0D1D2A),
            wingDarkEnd = Color(0xFF091019),
            hexInnerDark = Color(0xFF0A1520),
            highlightLine = Color(0xFFA5F3FC),
            textTint = Color(0xFFECFEFF)
        )
        tierLevel in 5..9 -> BadgeTierPalette(
            primaryAccent = Color(0xFFFF5722),
            secondaryAccent = Color(0xFFE11D48),
            wingDarkStart = Color(0xFF261014),
            wingDarkEnd = Color(0xFF12090C),
            hexInnerDark = Color(0xFF190A0D),
            highlightLine = Color(0xFFFFCCBC),
            textTint = Color(0xFFFFF1F2)
        )
        else -> BadgeTierPalette(
            primaryAccent = Color(0xFFF43F5E),
            secondaryAccent = Color(0xFF9F1239),
            wingDarkStart = Color(0xFF210F16),
            wingDarkEnd = Color(0xFF10090E),
            hexInnerDark = Color(0xFF170A10),
            highlightLine = Color(0xFFFDA4AF),
            textTint = Color(0xFFFFF1F2)
        )
    }
}

/**
 * TAHAP 2: Komponen Utama Custom Title Badge (`GeometricTitleBadge`)
 * - Geometri Dinamis (`wrapContentWidth` + `drawBehind`): Sayap poligon kanan memanjang otomatis
 *   mengikuti panjang teks dengan ruang aman minimal 16.dp sebelum garis miring kanan (100% ANTI-KEPOTONG!).
 * - 160+ Ikon Vektor Unik (`TitleIconCatalog`) di dalam emblem heksagon kiri.
 * - 8 Variasi Animasi Berbeda (`TitleAnimationType`).
 */
@Composable
fun GeometricTitleBadge(
    titleName: String,
    tierLevel: Int,
    modifier: Modifier = Modifier,
    styleVariant: Int = 0,
    iconId: Int = -1,
    animationType: TitleAnimationType? = null,
    isAnimated: Boolean = true,
    compact: Boolean = false,
    scaleFactor: Float = 1f,
    onClick: (() -> Unit)? = null
) {
    val palette = remember(tierLevel) { resolveTierPalette(tierLevel) }
    val resolvedIconId = remember(titleName, iconId) {
        TitleIconCatalog.resolveIconId(titleName, iconId)
    }
    val resolvedAnimType = remember(titleName, tierLevel, styleVariant, animationType) {
        animationType ?: TitleAnimationType.resolveForTitle(titleName, tierLevel, styleVariant)
    }

    val heightDp: Dp = if (compact) 23.dp else 28.dp
    val fontSize: TextUnit = if (compact) 9.sp else 10.5.sp
    val startTextPad: Dp = if (compact) 28.dp else 34.dp
    val endSafePad: Dp = if (compact) 18.dp else 24.dp

    val phase = if (isAnimated) {
        val transition = rememberInfiniteTransition(label = "titleBadgeAnim")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "titlePhase"
        )
        anim
    } else {
        0.35f
    }

    val baseMod = if (scaleFactor != 1f) {
        modifier.graphicsLayer(
            scaleX = scaleFactor,
            scaleY = scaleFactor
        )
    } else {
        modifier
    }

    val clickMod = if (onClick != null) {
        baseMod.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        baseMod
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = clickMod
            .wrapContentWidth()
            .height(heightDp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cy = h / 2f
                val hexRadius = h * 0.47f
                val hexCenterX = hexRadius * 1.05f

                val wingTopY = h * 0.13f
                val wingBottomY = h * 0.87f
                val wingHeight = wingBottomY - wingTopY
                val cutSize = wingHeight * 0.42f
                val wingStartX = hexCenterX + hexRadius * 0.32f
                val wingEndX = w - 1.5.dp.toPx()

                val wingPath = Path().apply {
                    when (styleVariant % 4) {
                        0 -> {
                            moveTo(wingStartX, wingTopY)
                            lineTo(wingEndX - cutSize * 0.42f, wingTopY)
                            lineTo(wingEndX, cy)
                            lineTo(wingEndX - cutSize * 0.78f, wingBottomY)
                            lineTo(wingStartX, wingBottomY)
                            close()
                        }
                        1 -> {
                            moveTo(wingStartX, wingTopY)
                            lineTo(wingEndX - cutSize * 0.8f, wingTopY)
                            lineTo(wingEndX, wingTopY + cutSize * 0.62f)
                            lineTo(wingEndX - cutSize * 0.32f, wingBottomY)
                            lineTo(wingStartX, wingBottomY)
                            close()
                        }
                        2 -> {
                            moveTo(wingStartX, wingTopY)
                            lineTo(wingEndX - cutSize * 0.65f, wingTopY)
                            lineTo(wingEndX, cy)
                            lineTo(wingEndX - cutSize * 0.65f, wingBottomY)
                            lineTo(wingStartX, wingBottomY)
                            close()
                        }
                        else -> {
                            moveTo(wingStartX, wingTopY)
                            lineTo(wingEndX, wingTopY)
                            lineTo(wingEndX - cutSize * 0.78f, wingBottomY)
                            lineTo(wingStartX, wingBottomY)
                            close()
                        }
                    }
                }

                val accentBrush = Brush.linearGradient(
                    colors = listOf(palette.primaryAccent, palette.secondaryAccent),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                val wingFillBrush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.wingDarkStart,
                        palette.wingDarkEnd,
                        palette.secondaryAccent.copy(alpha = 0.25f)
                    ),
                    startX = hexCenterX,
                    endX = w
                )

                drawPath(path = wingPath, brush = wingFillBrush)

                // Render 8 Variasi Animasi di Latar Sayap (Terkunci Sempurna di Dalam clipPath)
                clipPath(wingPath) {
                    when (resolvedAnimType) {
                        TitleAnimationType.HOLOGRAPHIC_FOIL -> {
                            val sweepX = wingStartX + (wingEndX - wingStartX + 40.dp.toPx()) * phase - 20.dp.toPx()
                            drawLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x66A5F3FC),
                                        Color(0x66F472B6),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(sweepX, wingTopY),
                                end = Offset(sweepX - 10.dp.toPx(), wingBottomY),
                                strokeWidth = 10.dp.toPx()
                            )
                        }
                        TitleAnimationType.LIQUID_GOLD_WAVE -> {
                            val waveX = wingStartX + (wingEndX - wingStartX) * phase
                            drawLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        palette.highlightLine.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(waveX, wingTopY),
                                end = Offset(waveX - 7.dp.toPx(), wingBottomY),
                                strokeWidth = 7.dp.toPx()
                            )
                        }
                        TitleAnimationType.CRIMSON_EMBER_DRIFT -> {
                            for (i in 0..3) {
                                val pLocal = (phase + i * 0.25f) % 1f
                                val ex = wingStartX + (wingEndX - wingStartX - 18.dp.toPx()) * ((i * 0.27f + 0.12f) % 1f)
                                val ey = wingBottomY - wingHeight * pLocal
                                drawCircle(
                                    color = palette.highlightLine.copy(alpha = (1f - pLocal) * 0.65f),
                                    radius = 1.3.dp.toPx(),
                                    center = Offset(ex, ey)
                                )
                            }
                        }
                        TitleAnimationType.STARLIGHT_TWINKLE -> {
                            for (i in 0..3) {
                                val alpha = (0.2f + 0.7f * sin((phase * 2 * Math.PI + i * 1.5).toFloat()).let { (it + 1f) / 2f })
                                val sx = wingStartX + (wingEndX - wingStartX - 16.dp.toPx()) * (0.18f + i * 0.22f)
                                val sy = if (i % 2 == 0) wingTopY + wingHeight * 0.28f else wingBottomY - wingHeight * 0.28f
                                drawCircle(
                                    color = palette.highlightLine.copy(alpha = alpha * 0.75f),
                                    radius = 1.2.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                        else -> Unit
                    }

                    if (resolvedAnimType == TitleAnimationType.LIGHTNING_ARC_SWEEP && phase < 0.35f) {
                        val lx = wingStartX + (wingEndX - wingStartX) * (phase / 0.35f)
                        drawLine(
                            color = palette.highlightLine.copy(alpha = 0.85f),
                            start = Offset(lx, wingTopY),
                            end = Offset(lx - 4.dp.toPx(), cy),
                            strokeWidth = 1.3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = palette.highlightLine.copy(alpha = 0.85f),
                            start = Offset(lx - 4.dp.toPx(), cy),
                            end = Offset(lx + 2.dp.toPx(), wingBottomY),
                            strokeWidth = 1.3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                val borderAlpha = if (resolvedAnimType == TitleAnimationType.CYBER_PULSE_GLOW) {
                    0.55f + 0.45f * sin(phase * 2 * Math.PI).toFloat().let { (it + 1f) / 2f }
                } else {
                    0.95f
                }

                drawPath(
                    path = wingPath,
                    brush = if (resolvedAnimType == TitleAnimationType.PLASMA_BORDER_FLOW) {
                        val shift = w * phase
                        Brush.linearGradient(
                            colors = listOf(palette.primaryAccent, palette.highlightLine, palette.secondaryAccent),
                            start = Offset(shift - w * 0.4f, 0f),
                            end = Offset(shift + w * 0.4f, h)
                        )
                    } else {
                        accentBrush
                    },
                    alpha = borderAlpha,
                    style = Stroke(width = 1.25.dp.toPx(), join = StrokeJoin.Miter)
                )

                // Garis aksen paralel atas & bawah + chevron di ujung kanan (aman di belakang endSafePad)
                val innerInset = 2.4.dp.toPx()
                drawLine(
                    color = palette.highlightLine.copy(alpha = 0.38f),
                    start = Offset(wingStartX + hexRadius * 0.5f, wingTopY + innerInset),
                    end = Offset(wingEndX - cutSize * 1.15f, wingTopY + innerInset),
                    strokeWidth = 0.75.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val chevronBaseX = wingEndX - cutSize * 0.9f
                drawLine(
                    color = palette.primaryAccent.copy(alpha = 0.55f),
                    start = Offset(chevronBaseX, wingTopY + 3.dp.toPx()),
                    end = Offset(chevronBaseX - 3.dp.toPx(), wingBottomY - 3.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 2. Emblem Heksagon Kiri Berlapis
                fun buildHexagonPath(cx: Float, centerY: Float, radius: Float): Path {
                    return Path().apply {
                        for (i in 0 until 6) {
                            val angleRad = Math.toRadians(60.0 * i - 30.0)
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

                drawPath(path = outerHexPath, brush = accentBrush)
                drawPath(path = midHexPath, color = palette.hexInnerDark)
                drawPath(
                    path = innerHexPath,
                    color = palette.highlightLine.copy(alpha = 0.65f),
                    style = Stroke(width = 0.9.dp.toPx(), join = StrokeJoin.Round)
                )
                drawPath(
                    path = outerHexPath,
                    color = palette.highlightLine.copy(alpha = 0.85f),
                    style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
                )

                if (resolvedAnimType == TitleAnimationType.CELESTIAL_ORBIT) {
                    val orbitAngle = Math.toRadians((phase * 360.0))
                    val ox = hexCenterX + (hexRadius * 0.92f * cos(orbitAngle)).toFloat()
                    val oy = cy + (hexRadius * 0.92f * sin(orbitAngle)).toFloat()
                    drawCircle(
                        color = palette.highlightLine,
                        radius = 1.8.dp.toPx(),
                        center = Offset(ox, oy)
                    )
                }

                // Gambar Ikon Vektor Unik dari 160+ Katalog Ikon di Tengah Heksagon Kiri
                drawTitleVectorIcon(
                    iconId = resolvedIconId,
                    center = Offset(hexCenterX, cy),
                    radius = hexRadius * 0.46f,
                    primaryColor = palette.highlightLine,
                    secondaryColor = palette.primaryAccent,
                    isAnimatedPulse = phase
                )
            }
            .padding(start = startTextPad, end = endSafePad)
    ) {
        Text(
            text = titleName.uppercase(Locale.ROOT),
            color = palette.textTint,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = OutfitDisplayFontFamily,
            letterSpacing = if (compact) 0.6.sp else 0.9.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * TAHAP 1 Item 2: Komponen "Micro-Title Tags" (`StatusMicroTitleTagRow`)
 * Menggantikan kapsul abu-abu biasa di bawah bio pada `ProfileScreen` dengan 3 lencana mikro
 * berbentuk Sleek Chamfered Tag (sudut potong miring 4.dp di kiri-atas & kanan-bawah, border 0.75.dp):
 * 1. Role (`MEMBER` / `ADMIN`): Ikon Perisai/Mahkota Vektor Mikro + teks kapital 10.sp.
 * 2. Level (`LV. X`): Lencana Segienam Memanjang + Ikon Rank Chevron Militer (1..4 garis pangkat) + angka monospasi.
 * 3. Level Class (`PERINTIS ANIME`, dll.): Pita tipis bergradasi halus dengan ikon vektor kelas.
 */
@Composable
fun StatusMicroTitleTagRow(
    role: String,
    level: Int,
    rankClassTitle: String = "",
    rankTitle: String = rankClassTitle,
    modifier: Modifier = Modifier
) {
    val resolvedTitle = rankClassTitle.ifBlank { rankTitle }.ifBlank { "Wibu Biasa" }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tag 1: Role (Sleek Chamfered Tag dengan Ikon Perisai/Mahkota)
        val isAdmin = role.equals("Admin", ignoreCase = true) || role.equals("Moderator", ignoreCase = true)
        val roleAccent = if (isAdmin) Color(0xFFF43F5E) else Color(0xFF94A3B8)
        SleekChamferedMicroTag(
            text = role.uppercase(Locale.ROOT),
            borderColor = roleAccent.copy(alpha = 0.55f),
            backgroundColor = Color(0xFF101219),
            textColor = if (isAdmin) Color(0xFFFDA4AF) else Color(0xFFE2E8F0),
            useMonospace = false
        ) {
            Canvas(modifier = Modifier.size(11.dp)) {
                val w = size.width
                val h = size.height
                if (isAdmin) {
                    // Mahkota Mikro
                    val p = Path().apply {
                        moveTo(w * 0.1f, h * 0.82f)
                        lineTo(w * 0.05f, h * 0.28f)
                        lineTo(w * 0.35f, h * 0.55f)
                        lineTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.65f, h * 0.55f)
                        lineTo(w * 0.95f, h * 0.28f)
                        lineTo(w * 0.9f, h * 0.82f)
                        close()
                    }
                    drawPath(p, Color(0xFFF43F5E))
                } else {
                    // Perisai Ksatria Mikro
                    val p = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.26f)
                        lineTo(w * 0.8f, h * 0.65f)
                        lineTo(w * 0.5f, h * 0.92f)
                        lineTo(w * 0.2f, h * 0.65f)
                        lineTo(w * 0.12f, h * 0.26f)
                        close()
                    }
                    drawPath(p, Color(0xFF94A3B8), style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round))
                }
            }
        }

        // Tag 2: Level (Elongated Hexagonal Tag dengan Ikon Rank Chevron Militer)
        val lvAccent = when {
            level >= 35 -> Color(0xFFF3C663)
            level >= 15 -> Color(0xFFA855F7)
            else -> Color(0xFFFF5722)
        }
        SleekHexChevronLevelTag(
            level = level,
            accentColor = lvAccent
        )

        // Tag 3: Level Class (Pita Tipis Bergradasi Halus)
        val classAccent = when {
            level >= 35 -> Color(0xFFF59E0B)
            level >= 15 -> Color(0xFFEC4899)
            else -> Color(0xFFE11D48)
        }
        SleekChamferedMicroTag(
            text = resolvedTitle.uppercase(Locale.ROOT),
            borderColor = classAccent.copy(alpha = 0.55f),
            backgroundColor = Color(0xFF141016),
            textColor = Color(0xFFFDA4AF),
            useMonospace = false
        ) {
            Canvas(modifier = Modifier.size(10.dp)) {
                val w = size.width
                val h = size.height
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.92f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.92f)
                    lineTo(w * 0.08f, h * 0.5f)
                    close()
                }
                drawPath(p, classAccent, style = Stroke(width = 1.2.dp.toPx()))
                drawCircle(classAccent, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}

@Composable
fun SleekChamferedMicroTag(
    text: String,
    borderColor: Color,
    backgroundColor: Color,
    textColor: Color,
    useMonospace: Boolean = false,
    leadingIcon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(22.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cut = 4.dp.toPx()
                val path = Path().apply {
                    moveTo(cut, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h - cut)
                    lineTo(w - cut, h)
                    lineTo(0f, h)
                    lineTo(0f, cut)
                    close()
                }
                drawPath(path = path, color = backgroundColor)
                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(width = 0.75.dp.toPx(), join = StrokeJoin.Miter)
                )
            }
            .padding(horizontal = 8.dp)
    ) {
        leadingIcon()
        Spacer(modifier = Modifier.width(4.5.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (useMonospace) JetBrainsMonoFontFamily else JakartaSansFontFamily,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun SleekHexChevronLevelTag(
    level: Int,
    accentColor: Color = when {
        level >= 35 -> Color(0xFFF3C663)
        level >= 15 -> Color(0xFFA855F7)
        else -> Color(0xFFFF5722)
    }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(22.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cy = h / 2f
                val point = 5.dp.toPx()
                val path = Path().apply {
                    moveTo(point, 0f)
                    lineTo(w - point, 0f)
                    lineTo(w, cy)
                    lineTo(w - point, h)
                    lineTo(point, h)
                    lineTo(0f, cy)
                    close()
                }
                drawPath(path = path, color = Color(0xFF12141D))
                drawPath(
                    path = path,
                    color = accentColor.copy(alpha = 0.65f),
                    style = Stroke(width = 0.75.dp.toPx(), join = StrokeJoin.Round)
                )
            }
            .padding(horizontal = 9.dp)
    ) {
        // Ikon Rank Chevron (Garis Pangkat Militer yang berubah sesuai Level)
        Canvas(modifier = Modifier.size(11.dp)) {
            val w = size.width
            val h = size.height
            val chevronCount = when {
                level >= 15 -> 3
                level >= 5 -> 2
                else -> 1
            }
            val spacing = h * 0.25f
            val baseStartY = when (chevronCount) {
                3 -> h * 0.78f
                2 -> h * 0.68f
                else -> h * 0.58f
            }
            for (i in 0 until chevronCount) {
                val y = baseStartY - i * spacing
                val chev = Path().apply {
                    moveTo(w * 0.15f, y)
                    lineTo(w * 0.5f, y - h * 0.24f)
                    lineTo(w * 0.85f, y)
                }
                drawPath(
                    path = chev,
                    color = accentColor,
                    style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (level >= 15) {
                drawCircle(accentColor, radius = w * 0.11f, center = Offset(w * 0.5f, h * 0.14f))
            }
        }
        Spacer(modifier = Modifier.width(4.5.dp))
        Text(
            text = "LV. $level",
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = JetBrainsMonoFontFamily,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Kerangka Heksagonal + Kapsul Bergaris Putus-Putus (`dashed outline #334155`, `height = 28.dp`)
 * ketika pengguna belum memasang Title khusus.
 */
@Composable
fun EmptyTitleSlotBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strokeColor = Color(0xFF334155)
    Box(
        modifier = modifier
            .height(28.dp)
            .width(156.dp)
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
            val hexRadius = h * 0.45f
            val hexCenterX = hexRadius * 1.08f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            val wingTopY = h * 0.16f
            val wingBottomY = h * 0.84f
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
                    width = 1.15.dp.toPx(),
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

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
                    width = 1.15.dp.toPx(),
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawCircle(
                color = strokeColor,
                radius = hexRadius * 0.38f,
                center = Offset(hexCenterX, cy),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 34.dp, end = 12.dp)
        ) {
            Text(
                text = "Pasang Title",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = JakartaSansFontFamily
            )
        }
    }
}
