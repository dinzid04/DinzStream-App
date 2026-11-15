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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
import com.yuranime.dinz.ui.theme.PiedraFontFamily
import com.yuranime.dinz.ui.theme.PlusJakartaFontFamily
import com.yuranime.dinz.ui.theme.SfProFontFamily
import com.yuranime.dinz.ui.theme.TourneyItalicFontFamily
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
    // Bintang utama jelas di sudut kanan-atas tepi huruf (tanpa menutupi huruf)
    SparkleAnchor(1.01f, 0.14f, 6800, 4900, 2600, 1.15f, 0.10f),
    // Kilau aksen kecil di kiri atas, bawah tengah-kiri, dan kanan bawah
    SparkleAnchor(-0.02f, 0.22f, 8200, 5600, 3100, 0.64f, 0.44f),
    SparkleAnchor(0.28f, 0.94f, 7400, 6200, 2900, 0.54f, 0.72f),
    SparkleAnchor(0.76f, 0.04f, 9100, 5400, 3300, 0.58f, 0.29f),
    SparkleAnchor(0.95f, 0.88f, 8700, 6500, 2700, 0.68f, 0.58f)
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
    val resolvedFontFamily = remember(styleDef.id, fontFamily) {
        when (styleDef.id) {
            "glitch" -> TourneyItalicFontFamily
            "magma" -> PiedraFontFamily
            "ruby", "wave", "star", "twilight", "slash", "sonar" -> PlusJakartaFontFamily
            else -> fontFamily ?: SfProFontFamily
        }
    }
    val resolvedFontStyle = if (styleDef.id == "glitch") FontStyle.Italic else FontStyle.Normal

    val resolvedTextStyle = remember(fontSize, fontWeight, resolvedFontFamily, resolvedFontStyle, textAlign) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = resolvedFontFamily,
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

private fun lineBoundsX(layout: TextLayoutResult, canvasSize: Size): Pair<Float, Float> {
    if (layout.lineCount <= 0) return 0f to canvasSize.width.coerceAtLeast(1f)
    val left = layout.getLineLeft(0).coerceIn(0f, canvasSize.width)
    val right = layout.getLineRight(0).coerceIn(left + 1f, canvasSize.width.coerceAtLeast(left + 1f))
    return left to right
}

private fun DrawScope.drawRubyStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val phase = cycle01(0f, 2200, timeMs)
    val bandWidth = max(textWidth * 0.55f, 110.dp.toPx())
    val travel = textWidth + bandWidth * 2f
    val startX = textLeft - bandWidth + phase * travel

    val brush = Brush.linearGradient(
        colors = listOf(
            palette.deep,
            palette.accent,
            palette.bright,
            palette.specular,
            Color.White.copy(alpha = 0.96f),
            palette.specular,
            palette.bright,
            palette.accent,
            palette.deep
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + bandWidth, canvasSize.height * 0.88f),
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
    val amp = min(fontSizePx * 0.145f, maxAmpPx)
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val sheenX = lerpFloat(textLeft - 60f, textRight + 60f, basePhase)
    val waveBrush = Brush.linearGradient(
        colors = listOf(palette.accent, palette.bright, palette.specular, palette.bright, palette.accent),
        start = Offset(sheenX - 80f, -amp),
        end = Offset(sheenX + 80f, canvasSize.height + amp),
        tileMode = TileMode.Mirror
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
            val waveFactor = cos(p * 2.0 * PI).toFloat()
            val dy = waveFactor * amp
            val xPos = runCatching {
                val cursorX = layout.getHorizontalPosition(r.first, true)
                val endCursorX = if (r.last + 1 <= textLen) {
                    layout.getHorizontalPosition(r.last + 1, true)
                } else {
                    cursorX + glyphLayout.size.width
                }
                min(cursorX, endCursorX)
            }.getOrDefault(0f)

            val crestColor = lerp(
                palette.accent,
                palette.specular,
                ((waveFactor * 0.5f + 0.5f) * 0.85f).coerceIn(0f, 1f)
            )
            drawText(
                textLayoutResult = glyphLayout,
                brush = Brush.verticalGradient(
                    colors = listOf(crestColor, palette.bright, palette.accent),
                    startY = 0f,
                    endY = glyphLayout.size.height.toFloat().coerceAtLeast(1f)
                ),
                topLeft = Offset(xPos, dy),
                alpha = 1f
            )
        }
        return
    }

    // Fallback aman jika teks multi-baris atau terpotong elipsis (...)
    val sliceCount = 12
    val span = (textRight - textLeft).coerceAtLeast(1f)
    val step = span / sliceCount
    for (s in 0 until sliceCount) {
        val p = (((basePhase - s * 0.08f) % 1f) + 1f) % 1f
        val dy = cos(p * 2.0 * PI).toFloat() * amp
        val l = textLeft + s * step
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
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val shimmer = cycle01(0f, 3400, timeMs)
    val sweepX = textLeft + shimmer * (textWidth + 120.dp.toPx()) - 60.dp.toPx()
    val textBrush = Brush.linearGradient(
        colors = listOf(palette.accent, palette.bright, palette.specular, palette.accent),
        start = Offset(sweepX, 0f),
        end = Offset(sweepX + 96.dp.toPx(), canvasSize.height)
    )
    drawText(textLayoutResult = layout, brush = textBrush, alpha = 1f)

    // Bintang utama di sudut kanan atas & kilau halus di tepi huruf agar keterbacaan tetap tajam
    STAR_SPARKLE_ANCHORS.forEachIndexed { idx, anchor ->
        val twinkle = cycle01(anchor.phaseOffset, anchor.twinklePeriod, timeMs)
        val alpha: Float
        val scaleFactor: Float
        val angleDeg: Float
        if (twinkle < 0.45f) {
            val u = smoothStep01(twinkle / 0.45f)
            alpha = u
            scaleFactor = (0.35f + 0.72f * u) * anchor.sizeScale
            angleDeg = 45f * u
        } else if (twinkle < 0.70f) {
            val u = (twinkle - 0.45f) / 0.25f
            alpha = 1f - 0.15f * u
            scaleFactor = (1.07f - 0.12f * u) * anchor.sizeScale
            angleDeg = 45f + 22f * u
        } else {
            val u = smoothStep01((twinkle - 0.70f) / 0.30f)
            alpha = (1f - u) * 0.85f
            scaleFactor = (0.95f * (1f - u * 0.65f)) * anchor.sizeScale
            angleDeg = 67f + 23f * u
        }
        if (alpha <= 0.04f) return@forEachIndexed

        val driftX = sin(cycle01(anchor.phaseOffset, anchor.periodX, timeMs) * 2f * PI.toFloat()) * 2.dp.toPx()
        val driftY = cos(cycle01(anchor.phaseOffset * 1.3f, anchor.periodY, timeMs) * 2f * PI.toFloat()) * 1.6.dp.toPx()
        val rawX = textLeft + textWidth * anchor.xRatio + driftX + if (idx == 0) 3.dp.toPx() else 0f
        val cx = rawX.coerceIn(-4.dp.toPx(), canvasSize.width - 1f)
        val cy = (canvasSize.height * anchor.yRatio + driftY).coerceIn(1f, canvasSize.height - 1f)

        val starPath = if (idx == 0) largeStar else smallStar
        val glowRadius = (if (idx == 0) 6.5.dp.toPx() else 3.8.dp.toPx()) * scaleFactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.specular.copy(alpha = 0.50f * alpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowRadius.coerceAtLeast(1f)
            ),
            radius = glowRadius.coerceAtLeast(1f),
            center = Offset(cx, cy)
        )
        rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
            scale(scale = scaleFactor.coerceIn(0.25f, 1.35f), pivot = Offset(cx, cy)) {
                translate(left = cx, top = cy) {
                    drawPath(path = starPath, color = Color.White.copy(alpha = alpha * 0.96f))
                }
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
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val shiftA = cycle01(0f, 9000, timeMs)
    val shiftB = cycle01(0.35f, 13000, timeMs)
    val pan = (shiftA * 0.7f + sin(shiftB * 2f * PI.toFloat()) * 0.15f) * textWidth

    val duskDeep = lerp(palette.twilightCompanion, Color(0xFF3B2C7A), 0.32f)
    val textBrush = Brush.linearGradient(
        colors = listOf(
            duskDeep,
            palette.twilightCompanion,
            palette.accent,
            palette.specular,
            palette.bright,
            palette.twilightCompanion
        ),
        start = Offset(textLeft - textWidth * 0.45f + pan, 0f),
        end = Offset(textLeft + textWidth * 1.15f + pan, canvasSize.height),
        tileMode = TileMode.Mirror
    )
    drawText(textLayoutResult = layout, brush = textBrush, alpha = 1f)

    // Kunang-kunang senja lembut mengorbit tepi atas/bawah nama
    val particleCount = 7
    for (i in 0 until particleCount) {
        val u1 = hashUnit(i, 101)
        val u2 = hashUnit(i, 202)
        val u3 = hashUnit(i, 303)
        val pulse = 0.15f + 0.80f * (sin(cycle01(u3, 2400 + i * 240, timeMs) * 2f * PI.toFloat()) * 0.5f + 0.5f)
        val x = textLeft + textWidth * ((u1 + sin(cycle01(u2, 5400, timeMs) * 2f * PI.toFloat()) * 0.05f).coerceIn(0.02f, 0.98f))
        val bandY = if (i % 2 == 0) 0.14f + u2 * 0.20f else 0.72f + u2 * 0.22f
        val y = canvasSize.height * ((bandY + cos(cycle01(u1, 4300, timeMs) * 2f * PI.toFloat()) * 0.08f).coerceIn(0.06f, 0.94f))
        val orbColor = if (i % 3 == 0) Color(0xFFFDE68A) else palette.specular
        val r = (1.3f + u3 * 1.1f).dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orbColor.copy(alpha = 0.72f * pulse), Color.Transparent),
                center = Offset(x, y),
                radius = r * 2.4f
            ),
            radius = r * 2.4f,
            center = Offset(x, y)
        )
        drawCircle(
            color = orbColor.copy(alpha = pulse * 0.95f),
            radius = r * 0.42f,
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
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val cycle = cycle01(0f, 2400, timeMs)
    val baseBrush = Brush.linearGradient(
        colors = listOf(palette.bright, palette.specular, palette.accent),
        start = Offset(textLeft, 0f),
        end = Offset(textRight, canvasSize.height)
    )

    val slashActive = cycle in 0.08f..0.46f
    if (!slashActive) {
        drawText(textLayoutResult = layout, brush = baseBrush, alpha = 1f)
        return
    }

    val p = ((cycle - 0.08f) / 0.38f).coerceIn(0f, 1f)
    val cutX = textLeft + textWidth * 0.52f
    val slopeOffset = canvasSize.height * 0.46f
    val topCutX = cutX + slopeOffset
    val botCutX = cutX - slopeOffset

    // Geser dua belahan teks sepanjang bidang tebasan diagonal katana (Lgh/o0;->b/c)
    val impactEnvelope = if (p < 0.25f) {
        smoothStep01(p / 0.25f)
    } else {
        1f - smoothStep01((p - 0.25f) / 0.75f)
    }
    val shearDist = impactEnvelope * 3.8.dp.toPx()
    val shearDx = shearDist * 0.88f
    val shearDy = shearDist * 0.42f

    if (impactEnvelope > 0.03f) {
        val leftHalfPath = Path().apply {
            moveTo(-24f, -8f)
            lineTo(topCutX, -8f)
            lineTo(botCutX, canvasSize.height + 8f)
            lineTo(-24f, canvasSize.height + 8f)
            close()
        }
        val rightHalfPath = Path().apply {
            moveTo(topCutX, -8f)
            lineTo(canvasSize.width + 24f, -8f)
            lineTo(canvasSize.width + 24f, canvasSize.height + 8f)
            lineTo(botCutX, canvasSize.height + 8f)
            close()
        }

        clipPath(leftHalfPath) {
            drawText(
                textLayoutResult = layout,
                brush = baseBrush,
                topLeft = Offset(-shearDx, -shearDy),
                alpha = 1f
            )
        }
        clipPath(rightHalfPath) {
            drawText(
                textLayoutResult = layout,
                brush = baseBrush,
                topLeft = Offset(shearDx, shearDy),
                alpha = 1f
            )
        }
    } else {
        drawText(textLayoutResult = layout, brush = baseBrush, alpha = 1f)
    }

    // Kilat sabetan katana diagonal sejajar bidang belah
    val bladeFlash = if (p < 0.38f) sin((p / 0.38f) * PI.toFloat()).coerceIn(0f, 1f) else 0f
    if (bladeFlash > 0.02f) {
        val bladeStart = Offset(topCutX + 10.dp.toPx(), -canvasSize.height * 0.14f)
        val bladeEnd = Offset(botCutX - 10.dp.toPx(), canvasSize.height * 1.14f)
        val auraBrush = Brush.linearGradient(
            0.0f to Color.Transparent,
            0.30f to palette.accent.copy(alpha = 0.88f * bladeFlash),
            0.50f to Color.White.copy(alpha = bladeFlash),
            0.70f to palette.specular.copy(alpha = 0.90f * bladeFlash),
            1.0f to Color.Transparent,
            start = bladeStart,
            end = bladeEnd
        )
        drawLine(
            brush = auraBrush,
            start = bladeStart,
            end = bladeEnd,
            strokeWidth = 3.0.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.98f * bladeFlash),
            start = Offset(topCutX + 4.dp.toPx(), 0f),
            end = Offset(botCutX - 4.dp.toPx(), canvasSize.height),
            strokeWidth = 1.25.dp.toPx()
        )

        // Percikan bilah katana tegak lurus garis tebasan
        for (k in 0 until 4) {
            val h = hashUnit(k, 91)
            val frac = 0.22f + k * 0.19f
            val sx = lerpFloat(bladeStart.x, bladeEnd.x, frac)
            val sy = lerpFloat(bladeStart.y, bladeEnd.y, frac)
            val side = if (k % 2 == 0) 1f else -1f
            val len = (5f + h * 7f).dp.toPx() * bladeFlash
            drawLine(
                color = lerp(palette.specular, Color.White, 0.7f).copy(alpha = bladeFlash * 0.9f),
                start = Offset(sx, sy),
                end = Offset(sx + side * len * 0.85f, sy - side * len * 0.48f),
                strokeWidth = 1.1.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawGlitchStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    // Kuantisasi bucket 46ms sesuai Lgh/j0;->x Case 3 pada font Tourney Italic
    val bucket = (timeMs / 46L).toInt()
    val burstRoll = hashUnit(bucket / 3, 71)
    val isBurst = burstRoll > 0.55f
    val stepRoll = hashUnit(bucket, 19)

    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val mainBrush = Brush.horizontalGradient(
        colors = listOf(palette.specular, palette.bright, palette.accent),
        startX = textLeft,
        endX = textRight
    )

    if (!isBurst) {
        drawText(textLayoutResult = layout, brush = mainBrush, alpha = 1f)
        return
    }

    val jitterX = (stepRoll - 0.5f) * 5.6.dp.toPx()
    val jitterY = (hashUnit(bucket, 37) - 0.5f) * 1.4.dp.toPx()
    val ghostShift = max(jitterX.let { if (it < 0f) -it else it }, 1.6.dp.toPx())

    // Jejak kromatik siber kontras tinggi (Cyan & Hot Pink/Crimson)
    drawText(
        textLayoutResult = layout,
        color = Color(0xFF00F5D4).copy(alpha = 0.88f),
        topLeft = Offset(-ghostShift, jitterY)
    )
    drawText(
        textLayoutResult = layout,
        color = Color(0xFFFF2A6D).copy(alpha = 0.84f),
        topLeft = Offset(ghostShift, -jitterY)
    )

    // Pergeseran 3 pita horizontal tajam
    val cut1 = canvasSize.height * (0.24f + hashUnit(bucket, 53) * 0.24f)
    val cut2 = canvasSize.height * (0.58f + hashUnit(bucket, 61) * 0.22f)
    val sliceDx = (hashUnit(bucket, 83) - 0.5f) * 8.0.dp.toPx()

    clipRect(-14f, -2f, canvasSize.width + 14f, cut1) {
        drawText(textLayoutResult = layout, brush = mainBrush, topLeft = Offset(0f, 0f))
    }
    clipRect(-14f, cut1, canvasSize.width + 14f, cut2) {
        val midFlash = if (hashUnit(bucket, 97) > 0.72f) Color.White else palette.specular
        drawText(
            textLayoutResult = layout,
            color = midFlash,
            topLeft = Offset(sliceDx, 0f)
        )
    }
    clipRect(-14f, cut2, canvasSize.width + 14f, canvasSize.height + 2f) {
        drawText(textLayoutResult = layout, brush = mainBrush, topLeft = Offset(-sliceDx * 0.45f, 0f))
    }
}

private fun DrawScope.drawMagmaStyle(
    layout: TextLayoutResult,
    palette: NameStylePalette,
    timeMs: Long,
    canvasSize: Size
) {
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val flow = cycle01(0f, 3400, timeMs)
    val flare = sin(cycle01(0f, 5200, timeMs) * 2f * PI.toFloat()) * 0.5f + 0.5f
    val yShift = flow * canvasSize.height * 2.1f

    // Gradien lava pijar di atas siluet batu Piedra (3400ms + 5200ms)
    val charred = lerp(palette.deep, Color(0xFF2B0600), 0.35f)
    val emberHot = lerp(palette.accent, Color(0xFFFF6A00), 0.48f)
    val moltenCore = lerp(emberHot, Color(0xFFFFF1B8), 0.75f + flare * 0.16f)
    val magmaBrush = Brush.linearGradient(
        colors = listOf(charred, emberHot, moltenCore, palette.accent, charred),
        start = Offset(textLeft, canvasSize.height + yShift),
        end = Offset(textLeft + canvasSize.height * 0.34f, -canvasSize.height + yShift),
        tileMode = TileMode.Mirror
    )
    drawText(textLayoutResult = layout, brush = magmaBrush, alpha = 1f)

    // Percikan bara api naik halus dari dasar huruf (Lgh/e0;->k)
    val emberCount = 8
    for (i in 0 until emberCount) {
        val h1 = hashUnit(i, 411)
        val h2 = hashUnit(i, 523)
        val life = cycle01(h1, 1800 + i * 190, timeMs)
        val alpha = if (life < 0.25f) life / 0.25f else (1f - life) / 0.75f
        val x = textLeft + textWidth * (0.04f + h2 * 0.92f) +
            sin(life * 4f * PI.toFloat() + h1 * 6f) * 2.2.dp.toPx()
        val y = canvasSize.height * (0.94f - life * 1.12f)
        val radius = ((1.1f + h1 * 1.2f) * (1f - life * 0.5f)).dp.toPx().coerceAtLeast(0.75f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(emberHot.copy(alpha = 0.65f * alpha), Color.Transparent),
                center = Offset(x, y),
                radius = radius * 2.3f
            ),
            radius = radius * 2.3f,
            center = Offset(x, y)
        )
        drawCircle(
            color = moltenCore.copy(alpha = alpha),
            radius = radius * 0.52f,
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
    val (textLeft, textRight) = lineBoundsX(layout, canvasSize)
    val textWidth = (textRight - textLeft).coerceAtLeast(1f)
    val center = Offset((textLeft + textRight) * 0.5f, canvasSize.height * 0.5f)
    val maxRadius = max(textWidth * 0.62f, canvasSize.height * 1.15f).coerceAtLeast(12f)

    // 1. Sapuan ping radial menyebar dari pusat nama (Lgh/j0;->x case 0 & Lm1/r;->j)
    val primaryPhase = cycle01(0f, 3400, timeMs)
    val waveRadius = (maxRadius * (0.14f + primaryPhase * 0.94f)).coerceAtLeast(6f)
    val innerStop = ((waveRadius - maxRadius * 0.24f) / waveRadius).coerceIn(0.02f, 0.72f)
    val crestStop = (innerStop + 0.14f).coerceIn(innerStop + 0.02f, 0.88f)
    val trailStop = (crestStop + 0.08f).coerceIn(crestStop + 0.02f, 0.98f)
    val baseMuted = lerp(palette.deep, palette.accent, 0.62f)

    val radialTextBrush = Brush.radialGradient(
        0.0f to baseMuted,
        innerStop to palette.accent,
        crestStop to palette.specular,
        trailStop to palette.bright,
        1.0f to baseMuted,
        center = center,
        radius = waveRadius
    )
    drawText(textLayoutResult = layout, brush = radialTextBrush, alpha = 1f)

    // 2. Tiga cincin radar konsentris memancar keluar berurutan (3400ms)
    val ringOffsets = floatArrayOf(0f, 0.33f, 0.66f)
    for (offset in ringOffsets) {
        val p = cycle01(offset, 3400, timeMs)
        val fade = (1f - p) * (1f - p * 0.35f)
        if (fade <= 0.02f) continue
        val r = 4.dp.toPx() + p * maxRadius

        drawCircle(
            brush = Brush.radialGradient(
                0.72f to Color.Transparent,
                0.96f to palette.accent.copy(alpha = fade * 0.14f),
                1.0f to Color.Transparent,
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
        drawCircle(
            color = lerp(palette.accent, palette.specular, (1f - p) * 0.65f).copy(alpha = fade * 0.72f),
            radius = r,
            center = center,
            style = Stroke(width = 1.2.dp.toPx()),
            blendMode = BlendMode.SrcOver
        )
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
