package com.yuranime.dinz.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.yuranime.dinz.data.firebase.FirebaseManager
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlinx.coroutines.isActive
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Palet turunan untuk efek Style Nama, diturunkan dari warna pilihan pengguna
 * mengikuti rumus tonal highlight & deep shadow pada renderer referensi.
 */
private data class NameStylePalette(
    val accent: Color,
    val deep: Color,
    val bright: Color,
    val specular: Color,
    val twilightCompanion: Color
)

private data class SparkleAnchor(
    val xRatio: Float,
    val yRatio: Float,
    val periodX: Int,
    val periodY: Int,
    val twinklePeriod: Int,
    val sizeScale: Float,
    val phaseOffset: Float
)

private val STAR_SPARKLE_ANCHORS = listOf(
    SparkleAnchor(0.10f, 0.18f, 7300, 5100, 2600, 0.95f, 0.12f),
    SparkleAnchor(0.32f, 0.78f, 9100, 6200, 3100, 0.80f, 0.43f),
    SparkleAnchor(0.56f, 0.14f, 6400, 7700, 2300, 1.05f, 0.68f),
    SparkleAnchor(0.79f, 0.80f, 8200, 5600, 3400, 0.88f, 0.27f),
    SparkleAnchor(0.94f, 0.25f, 10100, 6900, 2800, 0.92f, 0.81f)
)

/**
 * Mengurai hex warna Style Nama secara aman dengan fallback default style.
 */
fun parseNameStyleColor(colorHex: String?, styleId: String?): Color {
    val cleaned = colorHex?.trim().orEmpty()
    if (cleaned.isNotEmpty()) {
        val parsed = runCatching {
            val normalized = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
            Color(android.graphics.Color.parseColor(normalized))
        }.getOrNull()
        if (parsed != null) return parsed
    }
    val def = FirebaseManager.resolveNameStyleDefinition(styleId)
    val fallbackHex = def?.defaultColorHex ?: "#FFFFFF"
    return runCatching { Color(android.graphics.Color.parseColor(fallbackHex)) }
        .getOrDefault(Color.White)
}

private fun buildNameStylePalette(accent: Color): NameStylePalette {
    val deep = lerp(accent, Color(0xFF090B12), 0.44f)
    val bright = lerp(accent, Color.White, 0.34f)
    val specular = lerp(accent, Color.White, 0.86f)
    // Geser komplementer lembut agar Twilight terasa kosmik selaras warna pilihan user
    val twilightCompanion = Color(
        red = (accent.red * 0.45f + accent.blue * 0.45f).coerceIn(0.12f, 0.95f),
        green = (accent.green * 0.35f + accent.red * 0.20f).coerceIn(0.10f, 0.85f),
        blue = (accent.blue * 0.65f + 0.30f).coerceIn(0.25f, 1.0f),
        alpha = 1f
    )
    return NameStylePalette(
        accent = accent,
        deep = deep,
        bright = bright,
        specular = specular,
        twilightCompanion = twilightCompanion
    )
}

/**
 * Hash deterministik Murmur-style (sesuai `Lgh/i0;->b/d`) agar partikel & glitch stabil.
 */
private fun hashUnit(a: Int, b: Int): Float {
    var h = (a * 0x466f45d + 7) xor (b * 0x127409f)
    h = ((h ushr 16) xor h) * 0x45d9f3b
    h = ((h ushr 16) xor h) * 0x45d9f3b
    h = (h ushr 16) xor h
    return ((h.toLong() and 0xffffffffL).toDouble() / 4294967296.0).toFloat()
}

private fun cycle01(phaseOffset: Float, periodMs: Int, timeMs: Long): Float {
    val raw = timeMs.toDouble() / periodMs.toDouble() + phaseOffset.toDouble()
    return (raw - floor(raw)).toFloat().coerceIn(0f, 1f)
}

private fun smoothStep01(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun extractGraphemeRanges(text: String): List<IntRange> {
    if (text.isEmpty()) return emptyList()
    val ranges = ArrayList<IntRange>(text.length)
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(text)
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        if (end > start) {
            ranges.add(start until end)
        }
        start = end
        end = iterator.next()
    }
    return ranges
}

private fun createFourPointStarPath(radius: Float): Path {
    val inner = radius * 0.16f
    return Path().apply {
        moveTo(0f, -radius)
        quadraticTo(inner, -inner, radius, 0f)
        quadraticTo(inner, inner, 0f, radius)
        quadraticTo(-inner, inner, -radius, 0f)
        quadraticTo(-inner, -inner, 0f, -radius)
        close()
    }
}

/**
 * Komponen reusable YuraStyle (`StyledUsernameText`) untuk merender nama pengguna
 * dengan ke-8 Style Nama (`Star`, `Twilight`, `Slash`, `Glitch`, `Magma`, `Sonar`, `Ruby`, `Wave`).
 *
 * Seluruh animasi dijalankan STRICT pada fase Draw (`drawWithCache` + `onDrawWithContent`),
 * mendukung `timeMillisProvider` tunggal agar Global Chat berjalan 60/120 FPS tanpa recomposition.
 */
@Composable
fun StyledUsernameText(
    name: String,
    styleId: String?,
    colorHex: String?,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    defaultColor: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily? = SfProFontFamily,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign = TextAlign.Start,
    animate: Boolean = true,
    timeMillisProvider: (() -> Long)? = null
) {
    val safeName = name.ifBlank { "Member" }
    val normalizedStyle = styleId?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val styleDef = remember(normalizedStyle) {
        FirebaseManager.resolveNameStyleDefinition(normalizedStyle)
    }

    if (styleDef == null) {
        Text(
            text = safeName,
            color = defaultColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            modifier = modifier
        )
        return
    }

    val accentColor = remember(colorHex, styleDef.id) {
        parseNameStyleColor(colorHex, styleDef.id)
    }
    val palette = remember(accentColor) { buildNameStylePalette(accentColor) }
    val resolvedFontStyle = if (styleDef.id == "glitch") FontStyle.Italic else FontStyle.Normal

    val resolvedTextStyle = remember(fontSize, fontWeight, fontFamily, resolvedFontStyle, textAlign) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            fontStyle = resolvedFontStyle,
            textAlign = textAlign,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    }

    // Jam animasi lokal ringan hanya dibuat jika parent belum menyediakan shared clock
    val localClockMs = remember { mutableLongStateOf(420L) }
    if (timeMillisProvider == null && animate) {
        LaunchedEffect(styleDef.id) {
            while (isActive) {
                withFrameMillis { frameMs ->
                    localClockMs.longValue = frameMs
                }
            }
        }
    }
    val readTimeMs: () -> Long = remember(timeMillisProvider, animate) {
        when {
            timeMillisProvider != null -> timeMillisProvider
            animate -> { -> localClockMs.longValue }
            else -> { -> 480L }
        }
    }

    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }
    val textMeasurer = rememberTextMeasurer()

    // Khusus efek Wave: siapkan range cluster huruf & TextLayoutResult per huruf sekali saat nama berubah
    val graphemeRanges = remember(safeName, styleDef.id) {
        if (styleDef.id == "wave") extractGraphemeRanges(safeName) else emptyList()
    }
    val waveClusterLayouts = remember(safeName, styleDef.id, resolvedTextStyle) {
        if (styleDef.id == "wave" && graphemeRanges.size <= 42) {
            graphemeRanges.map { range ->
                val slice = safeName.substring(range.first, range.last + 1)
                textMeasurer.measure(
                    text = AnnotatedString(slice),
                    style = resolvedTextStyle,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            emptyList()
        }
    }

    var measuredLayout by remember(safeName, resolvedTextStyle, maxLines, overflow) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Text(
        text = safeName,
        color = Color.Transparent,
        style = resolvedTextStyle,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { measuredLayout = it },
        modifier = modifier.drawWithCache {
            val starPathSmall = createFourPointStarPath(max(3.dp.toPx(), fontSizePx * 0.22f))
            val starPathLarge = createFourPointStarPath(max(4.5.dp.toPx(), fontSizePx * 0.34f))
            val phaseSeed = (safeName.hashCode() and 0x7FFF) * 0.00017f

            onDrawWithContent {
                val layout = measuredLayout
                if (layout == null || size.width <= 1f || size.height <= 1f) {
                    drawContent()
                    return@onDrawWithContent
                }
                val nowMs = readTimeMs() + (phaseSeed * 2500f).toLong()

                when (styleDef.id) {
                    "ruby" -> drawRubyStyle(layout, palette, nowMs, size)
                    "wave" -> drawWaveStyle(
                        layout = layout,
                        ranges = graphemeRanges,
                        clusterLayouts = waveClusterLayouts,
                        palette = palette,
                        fontSizePx = fontSizePx,
                        timeMs = nowMs,
                        maxAmpPx = 4.2.dp.toPx(),
                        canvasSize = size
                    )
                    "star" -> drawStarStyle(layout, palette, nowMs, size, starPathSmall, starPathLarge)
                    "twilight" -> drawTwilightStyle(layout, palette, nowMs, size)
                    "slash" -> drawSlashStyle(layout, palette, nowMs, size)
                    "glitch" -> drawGlitchStyle(layout, palette, nowMs, size)
                    "magma" -> drawMagmaStyle(layout, palette, nowMs, size)
                    "sonar" -> drawSonarStyle(layout, palette, nowMs, size)
                    else -> drawText(textLayoutResult = layout, color = palette.accent, alpha = 1f)
                }
            }
        }
    )
}

private fun DrawScope.drawRubyStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val phase = cycle01(0f, 2200, timeMs)
    val bandWidth = max(canvasSize.width * 0.55f, 140.dp.toPx())
    val travel = canvasSize.width + bandWidth * 2f
    val startX = -bandWidth + phase * travel

    val brush = Brush.linearGradient(
        colors = listOf(
            palette.deep,
            palette.accent,
            palette.bright,
            palette.specular,
            palette.bright,
            palette.accent,
            palette.deep
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + bandWidth, canvasSize.height * 0.85f),
        tileMode = TileMode.Clamp
    )
    drawText(textLayoutResult = layout, brush = brush, alpha = 1f)
}

private fun DrawScope.drawWaveStyle(
    layout: TextLayoutResult,
    ranges: List<IntRange>,
    clusterLayouts: List<TextLayoutResult>,
    palette: NameStylePalette,
    fontSizePx: Float,
    timeMs: Long,
    maxAmpPx: Float,
    canvasSize: Size
) {
    val basePhase = cycle01(0f, 1800, timeMs)
    val amp = min(fontSizePx * 0.16f, maxAmpPx)
    val waveBrush = Brush.verticalGradient(
        colors = listOf(palette.specular, palette.bright, palette.accent),
        startY = -amp,
        endY = canvasSize.height + amp
    )

    val canPerGlyph = ranges.isNotEmpty() &&
        clusterLayouts.size == ranges.size &&
        !layout.isLineEllipsized(0) &&
        layout.lineCount == 1

    if (canPerGlyph) {
        val textLen = layout.layoutInput.text.length
        for (i in ranges.indices) {
            val r = ranges[i]
            if (r.first >= textLen) break
            val glyphLayout = clusterLayouts[i]
            val p = (((basePhase - i * 0.0555556f) % 1f) + 1f) % 1f
            val dy = cos(p * 2.0 * PI).toFloat() * amp
            val xPos = runCatching {
                val cursorX = layout.getHorizontalPosition(r.first, true)
                val endCursorX = if (r.last + 1 <= textLen) {
                    layout.getHorizontalPosition(r.last + 1, true)
                } else {
                    cursorX + glyphLayout.size.width
                }
                min(cursorX, endCursorX)
            }.getOrDefault(0f)

            drawText(
                textLayoutResult = glyphLayout,
                brush = waveBrush,
                topLeft = Offset(xPos, dy),
                alpha = 1f
            )
        }
        return
    }

    // Fallback aman jika teks multi-baris atau terpotong elipsis (...)
    val sliceCount = 12
    val lineRight = layout.getLineRight(0).coerceAtMost(canvasSize.width)
    val lineLeft = layout.getLineLeft(0).coerceAtLeast(0f)
    val span = (lineRight - lineLeft).coerceAtLeast(1f)
    val step = span / sliceCount
    for (s in 0 until sliceCount) {
        val p = (((basePhase - s * 0.08f) % 1f) + 1f) % 1f
        val dy = cos(p * 2.0 * PI).toFloat() * amp
        val l = lineLeft + s * step
        val r = if (s == sliceCount - 1) canvasSize.width + 2f else l + step + 0.5f
        clipRect(
            left = l,
            top = -amp - 4f,
            right = r,
            bottom = canvasSize.height + amp + 4f
        ) {
            drawText(
                textLayoutResult = layout,
                brush = waveBrush,
                topLeft = Offset(0f, dy),
                alpha = 1f
            )
        }
    }
}

private fun DrawScope.drawStarStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size,
    smallStar: Path,
    largeStar: Path
) {
    val shimmer = cycle01(0f, 2500, timeMs)
    val sweepX = shimmer * (canvasSize.width + 140.dp.toPx()) - 70.dp.toPx()
    val textBrush = Brush.linearGradient(
        colors = listOf(palette.accent, palette.bright, palette.specular, palette.accent),
        start = Offset(sweepX, 0f),
        end = Offset(sweepX + 120.dp.toPx(), canvasSize.height)
    )
    drawText(textLayoutResult = layout, brush = textBrush, alpha = 1f)

    // 5 bintang 4-sudut (✦) berkedip & berputar sesuai Lgh/l0;->d,e
    STAR_SPARKLE_ANCHORS.forEachIndexed { idx, anchor ->
        val twinkle = cycle01(anchor.phaseOffset, anchor.twinklePeriod, timeMs)
        val alpha: Float
        val scale: Float
        val angleDeg: Float
        if (twinkle < 0.45f) {
            val u = smoothStep01(twinkle / 0.45f)
            alpha = u
            scale = 0.3f + 0.7f * u
            angleDeg = 45f * u
        } else if (twinkle < 0.70f) {
            val u = (twinkle - 0.45f) / 0.25f
            alpha = 1f - 0.15f * u
            scale = 1f - 0.1f * u
            angleDeg = 45f + 20f * u
        } else {
            val u = smoothStep01((twinkle - 0.70f) / 0.30f)
            alpha = (1f - u) * 0.85f
            scale = 0.9f * (1f - u * 0.65f)
            angleDeg = 65f + 25f * u
        }
        if (alpha <= 0.04f) return@forEachIndexed

        val driftX = sin(cycle01(anchor.phaseOffset, anchor.periodX, timeMs) * 2f * PI.toFloat()) * 3.dp.toPx()
        val driftY = cos(cycle01(anchor.phaseOffset * 1.3f, anchor.periodY, timeMs) * 2f * PI.toFloat()) * 2.dp.toPx()
        val cx = (canvasSize.width * anchor.xRatio + driftX).coerceIn(2f, canvasSize.width - 2f)
        val cy = (canvasSize.height * anchor.yRatio + driftY).coerceIn(1f, canvasSize.height - 1f)

        val starPath = if (idx % 2 == 0) largeStar else smallStar
        val glowRadius = (if (idx % 2 == 0) 6.5.dp.toPx() else 4.5.dp.toPx()) * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.specular.copy(alpha = 0.55f * alpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowRadius.coerceAtLeast(1f)
            ),
            radius = glowRadius.coerceAtLeast(1f),
            center = Offset(cx, cy)
        )
        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
            translate(left = cx, top = cy) {
                drawPath(path = starPath, color = Color.White.copy(alpha = alpha * 0.96f))
            }
        }
    }
}

private fun DrawScope.drawTwilightStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val shift = cycle01(0f, 3600, timeMs)
    val pan = shift * canvasSize.width
    val textBrush = Brush.linearGradient(
        colors = listOf(
            palette.twilightCompanion,
            palette.accent,
            palette.specular,
            palette.bright,
            palette.twilightCompanion
        ),
        start = Offset(-canvasSize.width * 0.5f + pan, 0f),
        end = Offset(canvasSize.width * 1.1f + pan, canvasSize.height),
        tileMode = TileMode.Mirror
    )
    drawText(textLayoutResult = layout, brush = textBrush, alpha = 1f)

    // Kunang-kunang / partikel debu bintang senja (Lgh/i0;)
    val particleCount = 8
    for (i in 0 until particleCount) {
        val u1 = hashUnit(i, 101)
        val u2 = hashUnit(i, 202)
        val u3 = hashUnit(i, 303)
        val pulse = 0.12f + 0.76f * (sin(cycle01(u3, 2300 + i * 210, timeMs) * 2f * PI.toFloat()) * 0.5f + 0.5f)
        val x = canvasSize.width * ((u1 + sin(cycle01(u2, 5400, timeMs) * 2f * PI.toFloat()) * 0.06f).coerceIn(0.03f, 0.97f))
        val y = canvasSize.height * ((u2 + cos(cycle01(u1, 4300, timeMs) * 2f * PI.toFloat()) * 0.12f).coerceIn(0.08f, 0.92f))
        val r = (1.6f + u3 * 1.4f).dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.specular.copy(alpha = 0.75f * pulse), Color.Transparent),
                center = Offset(x, y),
                radius = r * 2.2f
            ),
            radius = r * 2.2f,
            center = Offset(x, y)
        )
        drawCircle(
            color = palette.specular.copy(alpha = pulse),
            radius = r * 0.45f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSlashStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val cycle = cycle01(0f, 2400, timeMs)
    val baseBrush = Brush.linearGradient(
        colors = listOf(palette.bright, palette.specular, palette.accent),
        start = Offset(0f, 0f),
        end = Offset(canvasSize.width, canvasSize.height)
    )

    val slashActive = cycle in 0.06f..0.44f
    if (!slashActive) {
        drawText(textLayoutResult = layout, brush = baseBrush, alpha = 1f)
        return
    }

    val p = smoothStep01((cycle - 0.06f) / 0.38f)
    val impactFactor = sin(p * PI.toFloat()).coerceIn(0f, 1f)
    val splitShift = impactFactor * 2.2.dp.toPx()
    val midY = canvasSize.height * 0.52f

    // Split top & bottom along impact
    if (impactFactor > 0.15f) {
        clipRect(0f, -4f, canvasSize.width + 8f, midY) {
            drawText(
                textLayoutResult = layout,
                color = Color(0xFF19E6F5).copy(alpha = 0.55f * impactFactor),
                topLeft = Offset(-splitShift, -0.5.dp.toPx())
            )
            drawText(
                textLayoutResult = layout,
                brush = baseBrush,
                topLeft = Offset(splitShift * 0.45f, 0f)
            )
        }
        clipRect(-8f, midY, canvasSize.width + 8f, canvasSize.height + 4f) {
            drawText(
                textLayoutResult = layout,
                color = Color(0xFFFF2FD6).copy(alpha = 0.52f * impactFactor),
                topLeft = Offset(splitShift, 0.5.dp.toPx())
            )
            drawText(
                textLayoutResult = layout,
                brush = baseBrush,
                topLeft = Offset(-splitShift * 0.45f, 0f)
            )
        }
    } else {
        drawText(textLayoutResult = layout, brush = baseBrush, alpha = 1f)
    }

    // Tebasan pedang katana diagonal tajam menyapu kiri -> kanan
    val beamCenterX = lerpFloat(-0.15f * canvasSize.width, 1.15f * canvasSize.width, p)
    val dx = canvasSize.height * 0.55f
    val beamStart = Offset(beamCenterX - dx, -canvasSize.height * 0.15f)
    val beamEnd = Offset(beamCenterX + dx, canvasSize.height * 1.15f)
    val slashBrush = Brush.linearGradient(
        0.0f to Color.Transparent,
        0.35f to Color(0xFF19E6F5).copy(alpha = 0.85f * impactFactor),
        0.5f to Color.White.copy(alpha = impactFactor),
        0.65f to palette.accent.copy(alpha = 0.90f * impactFactor),
        1.0f to Color.Transparent,
        start = beamStart,
        end = beamEnd
    )
    drawLine(
        brush = slashBrush,
        start = beamStart,
        end = beamEnd,
        strokeWidth = 2.4.dp.toPx()
    )
    drawLine(
        color = Color.White.copy(alpha = 0.95f * impactFactor),
        start = Offset(beamCenterX - dx * 0.45f, canvasSize.height * 0.12f),
        end = Offset(beamCenterX + dx * 0.45f, canvasSize.height * 0.88f),
        strokeWidth = 1.1.dp.toPx()
    )
}

private fun DrawScope.drawGlitchStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    // Kuantisasi bucket 46ms sesuai Lgh/j0;->x Case 3
    val bucket = (timeMs / 46L).toInt()
    val burstRoll = hashUnit(bucket / 3, 71)
    val isBurst = burstRoll > 0.58f
    val stepRoll = hashUnit(bucket, 19)

    val mainBrush = Brush.horizontalGradient(
        colors = listOf(palette.bright, palette.accent, palette.specular)
    )

    if (!isBurst) {
        drawText(textLayoutResult = layout, brush = mainBrush, alpha = 1f)
        return
    }

    val jitterX = (stepRoll - 0.5f) * 5.2.dp.toPx()
    val jitterY = (hashUnit(bucket, 37) - 0.5f) * 1.4.dp.toPx()

    // RGB chromatic ghosts
    drawText(
        textLayoutResult = layout,
        color = Color(0xFF19E6F5).copy(alpha = 0.85f),
        topLeft = Offset(-jitterX.coerceAtLeast(1.4.dp.toPx()), jitterY)
    )
    drawText(
        textLayoutResult = layout,
        color = Color(0xFFFF2FD6).copy(alpha = 0.80f),
        topLeft = Offset(jitterX.coerceAtLeast(1.4.dp.toPx()), -jitterY)
    )

    // Horizontal slice displacement (2 pita terpotong)
    val cut1 = canvasSize.height * (0.25f + hashUnit(bucket, 53) * 0.25f)
    val cut2 = canvasSize.height * (0.58f + hashUnit(bucket, 61) * 0.22f)
    val sliceDx = (hashUnit(bucket, 83) - 0.5f) * 7.5.dp.toPx()

    clipRect(-10f, -2f, canvasSize.width + 10f, cut1) {
        drawText(textLayoutResult = layout, brush = mainBrush, topLeft = Offset(0f, 0f))
    }
    clipRect(-12f, cut1, canvasSize.width + 12f, cut2) {
        drawText(textLayoutResult = layout, brush = mainBrush, topLeft = Offset(sliceDx, 0f))
    }
    clipRect(-10f, cut2, canvasSize.width + 10f, canvasSize.height + 2f) {
        drawText(textLayoutResult = layout, brush = mainBrush, topLeft = Offset(-sliceDx * 0.4f, 0f))
    }
}

private fun DrawScope.drawMagmaStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val flow = cycle01(0f, 3400, timeMs)
    val flare = sin(cycle01(0f, 5200, timeMs) * 2f * PI.toFloat()) * 0.5f + 0.5f
    val yShift = flow * canvasSize.height * 2f

    // Gradien magma pijar bergerak 16° sesuai Lgh/j0;->x Case 2
    val emberHot = lerp(palette.accent, Color(0xFFFF8A00), 0.35f)
    val moltenCore = lerp(emberHot, Color(0xFFFFF1C1), 0.72f + flare * 0.15f)
    val magmaBrush = Brush.linearGradient(
        colors = listOf(palette.deep, emberHot, moltenCore, palette.accent, palette.deep),
        start = Offset(0f, canvasSize.height + yShift),
        end = Offset(canvasSize.height * 0.30f, -canvasSize.height + yShift),
        tileMode = TileMode.Mirror
    )
    drawText(textLayoutResult = layout, brush = magmaBrush, alpha = 1f)

    // Percikan bara api naik dari bagian bawah huruf (Lgh/e0;->k)
    val emberCount = 8
    for (i in 0 until emberCount) {
        val h1 = hashUnit(i, 411)
        val h2 = hashUnit(i, 523)
        val life = cycle01(h1, 1800 + i * 190, timeMs)
        val alpha = if (life < 0.25f) life / 0.25f else (1f - life) / 0.75f
        val x = canvasSize.width * (0.06f + h2 * 0.88f) +
            sin(life * 4f * PI.toFloat() + h1 * 6f) * 2.5.dp.toPx()
        val y = canvasSize.height * (0.95f - life * 1.15f)
        val radius = ((1.2f + h1 * 1.3f) * (1f - life * 0.5f)).dp.toPx().coerceAtLeast(0.8f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(emberHot.copy(alpha = 0.65f * alpha), Color.Transparent),
                center = Offset(x, y),
                radius = radius * 2.4f
            ),
            radius = radius * 2.4f,
            center = Offset(x, y)
        )
        drawCircle(
            color = moltenCore.copy(alpha = alpha),
            radius = radius * 0.55f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSonarStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    // Base muted body so scan beam pops with high contrast
    val baseBrush = Brush.horizontalGradient(
        colors = listOf(
            lerp(palette.deep, palette.accent, 0.55f),
            palette.accent.copy(alpha = 0.85f),
            lerp(palette.deep, palette.accent, 0.55f)
        )
    )
    drawText(textLayoutResult = layout, brush = baseBrush, alpha = 1f)

    // Gelombang sonar vertikal bolak-balik (ping-pong smooth)
    val t = cycle01(0f, 2500, timeMs)
    val pingPong = if (t <= 0.5f) smoothStep01(t * 2f) else smoothStep01((1f - t) * 2f)
    val scanX = lerpFloat(-0.08f * canvasSize.width, 1.08f * canvasSize.width, pingPong)
    val bandHalf = max(canvasSize.width * 0.14f, 16.dp.toPx())

    clipRect(
        left = (scanX - bandHalf).coerceAtLeast(0f),
        top = -2f,
        right = (scanX + bandHalf).coerceAtMost(canvasSize.width),
        bottom = canvasSize.height + 2f
    ) {
        val beamTextBrush = Brush.horizontalGradient(
            0.0f to palette.accent,
            0.35f to palette.bright,
            0.5f to palette.specular,
            0.65f to palette.bright,
            1.0f to palette.accent,
            startX = scanX - bandHalf,
            endX = scanX + bandHalf
        )
        drawText(textLayoutResult = layout, brush = beamTextBrush, alpha = 1f)
    }

    // Garis pemindai sonar tipis & lingkaran riak gelombang halus
    val lineTop = canvasSize.height * 0.04f
    val lineBottom = canvasSize.height * 0.96f
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, palette.specular, Color.Transparent),
            startY = lineTop,
            endY = lineBottom
        ),
        start = Offset(scanX, lineTop),
        end = Offset(scanX, lineBottom),
        strokeWidth = 1.8.dp.toPx()
    )
    val ripplePhase = cycle01(0f, 1250, timeMs)
    val rippleR = (canvasSize.height * 0.45f) + ripplePhase * canvasSize.height * 0.45f
    drawCircle(
        color = palette.accent.copy(alpha = (1f - ripplePhase) * 0.35f),
        radius = rippleR,
        center = Offset(scanX, canvasSize.height * 0.5f),
        style = Stroke(width = 1.dp.toPx()),
        blendMode = BlendMode.SrcOver
    )
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
