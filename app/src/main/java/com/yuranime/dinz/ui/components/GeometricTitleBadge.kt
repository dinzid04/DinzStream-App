package com.yuranime.dinz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.JakartaSansFontFamily
import com.yuranime.dinz.ui.theme.JetBrainsMonoFontFamily
import com.yuranime.dinz.ui.theme.OutfitDisplayFontFamily
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.abs
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

private fun resolveTierPalette(tierLevel: Int, vfxTier: TitleVfxTier): BadgeTierPalette {
    return when {
        vfxTier == TitleVfxTier.BLAZING_INFERNO -> BadgeTierPalette(
            primaryAccent = Color(0xFFFF6D00),
            secondaryAccent = Color(0xFFE11D48),
            wingDarkStart = Color(0xFF280D0B),
            wingDarkEnd = Color(0xFF140709),
            hexInnerDark = Color(0xFF1B0907),
            highlightLine = Color(0xFFFDE047),
            textTint = Color(0xFFFFFBEB)
        )
        vfxTier == TitleVfxTier.LIGHTNING_STORM -> BadgeTierPalette(
            primaryAccent = Color(0xFF38BDF8),
            secondaryAccent = Color(0xFF6366F1),
            wingDarkStart = Color(0xFF0A162C),
            wingDarkEnd = Color(0xFF080D1C),
            hexInnerDark = Color(0xFF0B1124),
            highlightLine = Color(0xFFBAE6FD),
            textTint = Color(0xFFF0F9FF)
        )
        vfxTier == TitleVfxTier.SOLAR_FLARE -> BadgeTierPalette(
            primaryAccent = Color(0xFFFBBF24),
            secondaryAccent = Color(0xFFF97316),
            wingDarkStart = Color(0xFF271907),
            wingDarkEnd = Color(0xFF140C07),
            hexInnerDark = Color(0xFF1E1306),
            highlightLine = Color(0xFFFEF08A),
            textTint = Color(0xFFFFFBEB)
        )
        vfxTier == TitleVfxTier.FROST_PRISM -> BadgeTierPalette(
            primaryAccent = Color(0xFF22D3EE),
            secondaryAccent = Color(0xFF0EA5E9),
            wingDarkStart = Color(0xFF0B1F29),
            wingDarkEnd = Color(0xFF08111B),
            hexInnerDark = Color(0xFF091722),
            highlightLine = Color(0xFFCFFAFE),
            textTint = Color(0xFFECFEFF)
        )
        vfxTier == TitleVfxTier.VOID_NEBULA -> BadgeTierPalette(
            primaryAccent = Color(0xFFC084FC),
            secondaryAccent = Color(0xFFE11D48),
            wingDarkStart = Color(0xFF1D0D2B),
            wingDarkEnd = Color(0xFF0E0718),
            hexInnerDark = Color(0xFF140921),
            highlightLine = Color(0xFFF3E8FF),
            textTint = Color(0xFFFAF5FF)
        )
        vfxTier == TitleVfxTier.HOLO_PRISM -> BadgeTierPalette(
            primaryAccent = Color(0xFFF472B6),
            secondaryAccent = Color(0xFF2DD4BF),
            wingDarkStart = Color(0xFF19132B),
            wingDarkEnd = Color(0xFF0D111C),
            hexInnerDark = Color(0xFF130F21),
            highlightLine = Color(0xFFFCE7F3),
            textTint = Color(0xFFFFFFFF)
        )
        vfxTier == TitleVfxTier.PULSE_SCAN -> BadgeTierPalette(
            primaryAccent = Color(0xFF10B981),
            secondaryAccent = Color(0xFF06B6D4),
            wingDarkStart = Color(0xFF0B1D1B),
            wingDarkEnd = Color(0xFF081114),
            hexInnerDark = Color(0xFF0A1716),
            highlightLine = Color(0xFFA7F3D0),
            textTint = Color(0xFFECFDF5)
        )
        tierLevel >= 35 -> BadgeTierPalette(
            primaryAccent = Color(0xFFF59E0B),
            secondaryAccent = Color(0xFFEF4444),
            wingDarkStart = Color(0xFF28120F),
            wingDarkEnd = Color(0xFF13090B),
            hexInnerDark = Color(0xFF1A0C0B),
            highlightLine = Color(0xFFFDE68A),
            textTint = Color(0xFFFFFBEB)
        )
        tierLevel in 10..34 -> BadgeTierPalette(
            primaryAccent = Color(0xFFFF5722),
            secondaryAccent = Color(0xFFE11D48),
            wingDarkStart = Color(0xFF241016),
            wingDarkEnd = Color(0xFF11090D),
            hexInnerDark = Color(0xFF190A0E),
            highlightLine = Color(0xFFFECDD3),
            textTint = Color(0xFFFFF1F2)
        )
        else -> BadgeTierPalette(
            primaryAccent = Color(0xFF64748B),
            secondaryAccent = Color(0xFF475569),
            wingDarkStart = Color(0xFF171A23),
            wingDarkEnd = Color(0xFF0F1118),
            hexInnerDark = Color(0xFF13161E),
            highlightLine = Color(0xFFCBD5E1),
            textTint = Color(0xFFE2E8F0)
        )
    }
}

private fun resolveVfxForTitle(titleName: String, tierLevel: Int): TitleVfxTier {
    val u = titleName.trim().uppercase(Locale.ROOT)
    return when {
        u == "WARGA YURANIME" || u == "PEMBURU KOLEKSI" -> TitleVfxTier.NONE
        u == "PERINTIS ANIME" || u == "PENJELAJAH KOMIK" || u.contains("RAMEN") ||
            u.contains("PUMPKIN") || u.contains("PEACEKEEPER") || u.contains("ALCHEMIST") ||
            u.contains("ELIXIR") -> TitleVfxTier.SUBTLE_SHIMMER
        u.contains("INFERNO") || u.contains("SEPUH") || u.contains("SHOGUN") ||
            u.contains("METEOR") || u.contains("MYTHIC") || u.contains("PHOENIX") ||
            u.contains("DRAGON") -> TitleVfxTier.BLAZING_INFERNO
        u.contains("RAIJIN") || u.contains("THUNDER") || u.contains("STORM") ||
            u.contains("TEMPEST") || u.contains("EMPEROR OF OTAKU") -> TitleVfxTier.LIGHTNING_STORM
        u.contains("SOLAR") || u.contains("AKASHIC") || u.contains("KHALIFAH") ||
            u.contains("STARFALL") || u.contains("CELESTIAL") -> TitleVfxTier.SOLAR_FLARE
        u.contains("FROST") || u.contains("GLACIAL") -> TitleVfxTier.FROST_PRISM
        u.contains("VOID") || u.contains("DIMENSI") || u.contains("REAPER") ||
            u.contains("OBSIDIAN") || u.contains("LUNAR") || u.contains("SHINOBI") ||
            u.contains("SKULL") || u.contains("CURSE") || u.contains("JESTER") -> TitleVfxTier.VOID_NEBULA
        u.contains("VIP") || u.contains("BANGSAWAN") || u.contains("GRANDMASTER") ||
            u.contains("SORCERER") || u.contains("WARDEN") -> TitleVfxTier.HOLO_PRISM
        u.contains("NETRUNNER") || u.contains("MARATON") || u.contains("VANGUARD") ||
            u.contains("DRUID") || u.contains("FORGE") || u.contains("BRAWLER") ||
            u.contains("RIDER") -> TitleVfxTier.PULSE_SCAN
        u.contains("CRIMSON") || u.contains("ISEKAI") || u.contains("GOBLIN") ||
            u.contains("CLEAVER") || u.contains("BOMBARDIER") || u.contains("BEAST") ||
            u.contains("KATANA") || u.contains("PYRO") -> TitleVfxTier.EMBER_SPARK
        tierLevel >= 50 -> TitleVfxTier.BLAZING_INFERNO
        tierLevel >= 35 -> TitleVfxTier.HOLO_PRISM
        tierLevel >= 18 -> TitleVfxTier.PULSE_SCAN
        else -> TitleVfxTier.SUBTLE_SHIMMER
    }
}

/**
 * Custom Title Badge (`GeometricTitleBadge`) bergaya Lower-Third Futuristik / Fantasi RPG:
 * - 5 Gaya Geometris (`ROYAL_HEX`, `CIRCUIT_TECH`, `FACETED_SHARD`, `DIAGONAL_SLASH`, `MECHA_HUD`).
 * - Menggunakan ikon vektor SVG asli dari folder `RPG ICON TITLE` & `RPG ICON V2` di dalam emblem kiri.
 * - Membedakan jelas title sederhana/starter (`NONE`/`SUBTLE_SHIMMER`) dengan title berbayar/mewah
 *   berupa efek kobaran api nyata, petir bercabang, korona surya, prisma holo, dan radar scan.
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
    vfxTier: TitleVfxTier? = null,
    titleId: String = "",
    timeMillisProvider: (() -> Long)? = null,
    onClick: (() -> Unit)? = null
) {
    val resolvedVfx = remember(titleName, tierLevel, vfxTier) {
        vfxTier ?: resolveVfxForTitle(titleName, tierLevel)
    }
    val shapeStyle = remember(styleVariant) { TitleShapeStyle.fromVariant(styleVariant) }
    val palette = remember(tierLevel, resolvedVfx) { resolveTierPalette(tierLevel, resolvedVfx) }
    val iconRes = remember(titleName, titleId) {
        RpgSvgIconCatalog.resolveCustomTitleIcon(titleName, titleId)
    }

    val isLongCompact = compact && titleName.length > 12
    val heightDp: Dp = if (compact) 23.dp else 32.dp
    val fontSize: TextUnit = when {
        isLongCompact -> 7.2.sp
        compact -> 8.2.sp
        else -> 10.5.sp
    }

    val iconBoxSize: Dp = when (shapeStyle) {
        TitleShapeStyle.MECHA_HUD -> if (compact) 15.dp else 20.dp
        TitleShapeStyle.FACETED_SHARD -> if (compact) 14.5.dp else 19.dp
        else -> if (compact) 13.5.dp else 18.dp
    }
    val startTextPad: Dp = if (compact) 7.dp else 10.dp
    val endSafePad: Dp = when (shapeStyle) {
        TitleShapeStyle.CIRCUIT_TECH -> if (compact) 20.dp else 28.dp
        TitleShapeStyle.DIAGONAL_SLASH -> if (compact) 20.dp else 28.dp
        else -> if (compact) 16.dp else 24.dp
    }

    val shouldRunLoop = isAnimated && resolvedVfx != TitleVfxTier.NONE
    val periodMs = remember(resolvedVfx) {
        when (resolvedVfx) {
            TitleVfxTier.LIGHTNING_STORM -> 1900L
            TitleVfxTier.BLAZING_INFERNO -> 1600L
            TitleVfxTier.SOLAR_FLARE -> 2800L
            else -> 2600L
        }
    }
    // Clock lokal ringan hanya dibuat bila parent tidak menyediakan Shared Clock
    val localClockMs = remember { mutableLongStateOf(0L) }
    if (shouldRunLoop && timeMillisProvider == null) {
        LaunchedEffect(resolvedVfx) {
            while (isActive) {
                withFrameMillis { frameMs ->
                    localClockMs.longValue = frameMs
                }
            }
        }
    }
    val readTimeMs: () -> Long = remember(timeMillisProvider, shouldRunLoop) {
        when {
            !shouldRunLoop -> { -> 0L }
            timeMillisProvider != null -> timeMillisProvider
            else -> { -> localClockMs.longValue }
        }
    }

    // Dibaca STRICTLY di dalam fase Draw agar 0 recomposition saat animasi/scroll
    val baseMod = modifier.graphicsLayer {
        scaleX = scaleFactor
        scaleY = scaleFactor
        compositingStrategy = CompositingStrategy.Offscreen
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
                val phase = if (shouldRunLoop) {
                    val nowMs = readTimeMs()
                    ((nowMs % periodMs).toFloat() / periodMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0.25f
                }
                drawBadgeFrameGeometry(
                    shapeStyle = shapeStyle,
                    vfxTier = resolvedVfx,
                    palette = palette,
                    phase = phase,
                    isAnimated = shouldRunLoop,
                    compact = compact
                )
            }
            .drawWithContent {
                drawContent()
                if (shouldRunLoop) {
                    val nowMs = readTimeMs()
                    val phase = ((nowMs % periodMs).toFloat() / periodMs.toFloat()).coerceIn(0f, 1f)
                    drawForegroundRichFx(
                        vfxTier = resolvedVfx,
                        palette = palette,
                        phase = phase,
                        compact = compact
                    )
                }
            }
            .padding(start = startTextPad, end = endSafePad)
    ) {
        Box(
            modifier = Modifier.size(iconBoxSize),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = titleName,
                colorFilter = ColorFilter.tint(palette.highlightLine),
                modifier = Modifier.size(iconBoxSize * 0.86f)
            )
        }

        Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))

        Text(
            text = titleName.uppercase(Locale.ROOT),
            color = palette.textTint,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = OutfitDisplayFontFamily,
            letterSpacing = when {
                isLongCompact -> 0.2.sp
                compact -> 0.45.sp
                else -> 0.85.sp
            },
            style = centeredBadgeTextStyle(fontSize),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun DrawScope.drawBadgeFrameGeometry(
    shapeStyle: TitleShapeStyle,
    vfxTier: TitleVfxTier,
    palette: BadgeTierPalette,
    phase: Float,
    isAnimated: Boolean,
    compact: Boolean
) {
    val w = size.width
    val h = size.height
    val cy = h / 2f
    val padY = h * 0.10f
    val bodyTop = padY
    val bodyBottom = h - padY
    val bodyH = bodyBottom - bodyTop
    val cut = bodyH * 0.36f
    val iconAreaW = if (compact) 23.dp.toPx() else 31.dp.toPx()

    val bodyPath = Path().apply {
        when (shapeStyle) {
            TitleShapeStyle.CIRCUIT_TECH -> {
                val traceOffset = if (compact) 4.dp.toPx() else 6.dp.toPx()
                val sx = traceOffset
                val ex = w - traceOffset
                moveTo(sx + cut * 0.8f, bodyTop)
                lineTo(ex - cut * 0.8f, bodyTop)
                lineTo(ex, bodyTop + cut * 0.7f)
                lineTo(ex, bodyBottom - cut * 0.7f)
                lineTo(ex - cut * 0.8f, bodyBottom)
                lineTo(sx + cut * 0.8f, bodyBottom)
                lineTo(sx, bodyBottom - cut * 0.7f)
                lineTo(sx, bodyTop + cut * 0.7f)
                close()
            }
            TitleShapeStyle.FACETED_SHARD -> {
                moveTo(cut * 0.55f, bodyTop)
                lineTo(w - cut * 1.1f, bodyTop)
                lineTo(w - 2.dp.toPx(), bodyTop + bodyH * 0.42f)
                lineTo(w - cut * 0.75f, bodyBottom)
                lineTo(0f, bodyBottom)
                lineTo(0f, bodyTop + cut * 0.65f)
                close()
            }
            TitleShapeStyle.DIAGONAL_SLASH -> {
                val skew = bodyH * 0.42f
                moveTo(skew, bodyTop)
                lineTo(w - 1.dp.toPx(), bodyTop)
                lineTo(w - skew - 4.dp.toPx(), bodyBottom)
                lineTo(0f, bodyBottom)
                close()
            }
            TitleShapeStyle.MECHA_HUD -> {
                val chamfer = bodyH * 0.32f
                moveTo(chamfer, bodyTop - h * 0.04f)
                lineTo(iconAreaW + chamfer * 0.5f, bodyTop - h * 0.04f)
                lineTo(iconAreaW + chamfer, bodyTop + h * 0.04f)
                lineTo(w - chamfer, bodyTop + h * 0.04f)
                lineTo(w - 1.5.dp.toPx(), bodyTop + chamfer)
                lineTo(w - 1.5.dp.toPx(), bodyBottom - chamfer * 0.7f)
                lineTo(w - chamfer * 1.1f, bodyBottom)
                lineTo(chamfer * 0.6f, bodyBottom)
                lineTo(0f, bodyBottom - chamfer * 0.6f)
                lineTo(0f, bodyTop + chamfer * 0.7f)
                close()
            }
            TitleShapeStyle.ROYAL_HEX -> {
                moveTo(cut * 0.6f, bodyTop)
                lineTo(w - cut * 0.65f, bodyTop)
                lineTo(w - 1.5.dp.toPx(), cy)
                lineTo(w - cut * 0.65f, bodyBottom)
                lineTo(cut * 0.6f, bodyBottom)
                lineTo(0f, cy)
                close()
            }
        }
    }

    val accentBrush = Brush.linearGradient(
        colors = listOf(palette.primaryAccent, palette.secondaryAccent),
        start = Offset(0f, 0f),
        end = Offset(w, h)
    )
    val darkWingBrush = Brush.horizontalGradient(
        colors = listOf(
            palette.wingDarkStart,
            palette.wingDarkEnd,
            palette.secondaryAccent.copy(alpha = if (vfxTier == TitleVfxTier.NONE) 0.14f else 0.34f)
        )
    )

    // 1. Sub-shard / Fin / Trace dekoratif sesuai gaya referensi
    when (shapeStyle) {
        TitleShapeStyle.CIRCUIT_TECH -> {
            // Referensi DnaoiKY: garis trace sirkuit dengan node lingkaran kecil di kiri & kanan
            val lineColor = palette.primaryAccent.copy(alpha = 0.75f)
            val nodeR = if (compact) 1.5.dp.toPx() else 2.dp.toPx()
            drawLine(lineColor, Offset(nodeR, cy), Offset(8.dp.toPx(), cy), strokeWidth = 1.dp.toPx())
            drawCircle(palette.highlightLine, radius = nodeR, center = Offset(nodeR, cy))
            drawLine(lineColor, Offset(w - 8.dp.toPx(), cy), Offset(w - nodeR, cy), strokeWidth = 1.dp.toPx())
            drawCircle(palette.highlightLine, radius = nodeR, center = Offset(w - nodeR, cy))
        }
        TitleShapeStyle.FACETED_SHARD -> {
            // Referensi vheY3Ze: sub-shard bawah runcing
            val subShard = Path().apply {
                moveTo(iconAreaW * 0.72f, bodyBottom - 1f)
                lineTo(iconAreaW + 22.dp.toPx(), bodyBottom - 1f)
                lineTo(iconAreaW + 15.dp.toPx(), h)
                lineTo(iconAreaW * 0.85f, h)
                close()
            }
            drawPath(subShard, palette.primaryAccent.copy(alpha = 0.85f))
        }
        TitleShapeStyle.MECHA_HUD -> {
            // Referensi UGKkbof: 4 ventilasi HUD bawah + fin atas
            val topFin = Path().apply {
                moveTo(iconAreaW * 0.25f, 0f)
                lineTo(iconAreaW * 0.9f, 0f)
                lineTo(iconAreaW + 4.dp.toPx(), bodyTop)
                lineTo(iconAreaW * 0.15f, bodyTop)
                close()
            }
            drawPath(topFin, palette.primaryAccent)
            val slitStart = iconAreaW + (w - iconAreaW) * 0.45f
            for (i in 0..3) {
                val sx = slitStart + i * (if (compact) 4.5.dp.toPx() else 6.dp.toPx())
                if (sx < w - 10.dp.toPx()) {
                    drawLine(
                        color = palette.primaryAccent.copy(alpha = 0.8f),
                        start = Offset(sx, bodyBottom - 0.5f),
                        end = Offset(sx - 2.dp.toPx(), h),
                        strokeWidth = (if (compact) 1.6.dp.toPx() else 2.2.dp.toPx()),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        else -> Unit
    }

    // 2. Isi utama pelat title
    drawPath(path = bodyPath, brush = darkWingBrush)

    // 3. Layer interior & animasi dalam pelat (clipped ke bodyPath)
    clipPath(bodyPath) {
        // Low-poly facets untuk FACETED_SHARD (referensi vheY3Ze)
        if (shapeStyle == TitleShapeStyle.FACETED_SHARD) {
            val facet1 = Path().apply {
                moveTo(iconAreaW, bodyTop)
                lineTo(iconAreaW + (w - iconAreaW) * 0.48f, bodyBottom)
                lineTo(iconAreaW, bodyBottom)
                close()
            }
            val facet2 = Path().apply {
                moveTo(w, bodyTop)
                lineTo(w - (w - iconAreaW) * 0.35f, bodyTop)
                lineTo(w, bodyBottom)
                close()
            }
            drawPath(facet1, palette.primaryAccent.copy(alpha = 0.14f))
            drawPath(facet2, palette.secondaryAccent.copy(alpha = 0.18f))
        }

        // Left angled emblem backing untuk SVG icon
        val emblemCap = Path().apply {
            moveTo(0f, 0f)
            lineTo(iconAreaW, 0f)
            if (shapeStyle == TitleShapeStyle.DIAGONAL_SLASH) {
                lineTo(iconAreaW - cut * 0.65f, h)
            } else {
                lineTo(iconAreaW + cut * 0.25f, cy)
                lineTo(iconAreaW - cut * 0.2f, h)
            }
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = emblemCap,
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.primaryAccent.copy(alpha = if (vfxTier == TitleVfxTier.NONE) 0.30f else 0.72f),
                    palette.hexInnerDark
                )
            )
        )

        // Strip miring `//` di ekor kanan untuk DIAGONAL_SLASH (referensi G9NqcmK) & CIRCUIT_TECH bar
        if (shapeStyle == TitleShapeStyle.DIAGONAL_SLASH) {
            val stripeW = if (compact) 3.dp.toPx() else 4.5.dp.toPx()
            val gap = if (compact) 2.5.dp.toPx() else 3.5.dp.toPx()
            for (s in 0..1) {
                val rx = w - (if (compact) 11.dp.toPx() else 16.dp.toPx()) - s * (stripeW + gap)
                val stripe = Path().apply {
                    moveTo(rx + cut * 0.55f, bodyTop)
                    lineTo(rx + cut * 0.55f + stripeW, bodyTop)
                    lineTo(rx + stripeW, bodyBottom)
                    lineTo(rx, bodyBottom)
                    close()
                }
                drawPath(stripe, if (s == 0) palette.highlightLine else palette.primaryAccent)
            }
        } else if (shapeStyle == TitleShapeStyle.CIRCUIT_TECH) {
            // Dot / slash bars kanan ala referensi DnaoiKY
            val startBar = w - (if (compact) 16.dp.toPx() else 22.dp.toPx())
            for (b in 0..2) {
                val bx = startBar + b * (if (compact) 3.5.dp.toPx() else 4.5.dp.toPx())
                drawLine(
                    color = palette.highlightLine.copy(alpha = 0.75f),
                    start = Offset(bx, bodyTop + bodyH * 0.30f),
                    end = Offset(bx - 2.dp.toPx(), bodyTop + bodyH * 0.70f),
                    strokeWidth = if (compact) 1.4.dp.toPx() else 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Render VFX interior pelat sesuai tier (Starter NONE tetap bersih tanpa efek berlebihan)
        if (isAnimated && vfxTier != TitleVfxTier.NONE) {
            drawInteriorVfx(
                vfxTier = vfxTier,
                palette = palette,
                phase = phase,
                startX = iconAreaW * 0.7f,
                endX = w,
                topY = bodyTop,
                bottomY = bodyBottom
            )
        }
    }

    // 4. Outline bevel luar
    drawPath(
        path = bodyPath,
        brush = accentBrush,
        style = Stroke(
            width = if (vfxTier == TitleVfxTier.NONE) 0.9.dp.toPx() else 1.35.dp.toPx(),
            join = StrokeJoin.Round
        )
    )

    // Rivet sudut halus untuk CIRCUIT_TECH & MECHA_HUD
    if (shapeStyle == TitleShapeStyle.CIRCUIT_TECH || shapeStyle == TitleShapeStyle.MECHA_HUD) {
        val rivetR = if (compact) 0.9.dp.toPx() else 1.2.dp.toPx()
        drawCircle(palette.highlightLine.copy(alpha = 0.75f), rivetR, Offset(iconAreaW * 0.35f, bodyTop + 2.5.dp.toPx()))
        drawCircle(palette.highlightLine.copy(alpha = 0.75f), rivetR, Offset(w - 10.dp.toPx(), bodyBottom - 2.5.dp.toPx()))
    }
}

private fun DrawScope.drawInteriorVfx(
    vfxTier: TitleVfxTier,
    palette: BadgeTierPalette,
    phase: Float,
    startX: Float,
    endX: Float,
    topY: Float,
    bottomY: Float
) {
    val span = (endX - startX).coerceAtLeast(1f)
    val sheenX = startX + (span + 40.dp.toPx()) * phase - 20.dp.toPx()

    // Kilau sapuan dasar untuk selain NONE
    val sheenAlpha = when (vfxTier) {
        TitleVfxTier.SUBTLE_SHIMMER -> 0.14f
        TitleVfxTier.HOLO_PRISM -> 0.36f
        TitleVfxTier.SOLAR_FLARE, TitleVfxTier.BLAZING_INFERNO -> 0.28f
        else -> 0.22f
    }
    drawLine(
        brush = Brush.horizontalGradient(
            colors = if (vfxTier == TitleVfxTier.HOLO_PRISM) {
                listOf(
                    Color.Transparent,
                    Color(0xFF22D3EE).copy(alpha = 0.30f),
                    Color(0xFFF472B6).copy(alpha = 0.36f),
                    Color(0xFFFACC15).copy(alpha = 0.30f),
                    Color.Transparent
                )
            } else {
                listOf(
                    Color.Transparent,
                    palette.highlightLine.copy(alpha = sheenAlpha),
                    Color.White.copy(alpha = sheenAlpha * 1.15f),
                    Color.Transparent
                )
            },
            startX = sheenX - 18.dp.toPx(),
            endX = sheenX + 18.dp.toPx()
        ),
        start = Offset(sheenX, topY - 4f),
        end = Offset(sheenX - 14.dp.toPx(), bottomY + 4f),
        strokeWidth = 22.dp.toPx()
    )

    when (vfxTier) {
        TitleVfxTier.PULSE_SCAN -> {
            val scanX = startX + span * phase
            drawLine(
                color = palette.highlightLine.copy(alpha = 0.65f),
                start = Offset(scanX, topY),
                end = Offset(scanX, bottomY),
                strokeWidth = 1.6.dp.toPx()
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, palette.primaryAccent.copy(alpha = 0.30f), Color.Transparent),
                    startX = scanX - 14.dp.toPx(),
                    endX = scanX + 14.dp.toPx()
                ),
                start = Offset(scanX, topY),
                end = Offset(scanX, bottomY),
                strokeWidth = 14.dp.toPx()
            )
        }
        TitleVfxTier.EMBER_SPARK, TitleVfxTier.BLAZING_INFERNO -> {
            val count = if (vfxTier == TitleVfxTier.BLAZING_INFERNO) 9 else 4
            for (i in 0 until count) {
                val p = (phase + i * (1f / count)) % 1f
                val ex = startX + span * (((i * 0.27f) + p * 0.35f) % 1f)
                val ey = bottomY - (bottomY - topY) * p
                val r = (1.6f - p * 0.7f).coerceAtLeast(0.7f).dp.toPx()
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFFFDE047) else Color(0xFFFF6D00),
                    radius = r * (1f - p * 0.3f),
                    center = Offset(ex, ey),
                    alpha = (1f - p) * 0.9f
                )
            }
        }
        TitleVfxTier.FROST_PRISM -> {
            for (i in 0..4) {
                val p = (phase + i * 0.2f) % 1f
                val fx = startX + span * (0.12f + i * 0.19f)
                val fy = topY + (bottomY - topY) * (0.3f + 0.4f * sin(p * Math.PI * 2.0).toFloat())
                val arm = 2.5.dp.toPx()
                val alpha = abs(sin(p * Math.PI)).toFloat() * 0.85f
                drawLine(Color(0xFFE0F2FE), Offset(fx - arm, fy), Offset(fx + arm, fy), 1.dp.toPx(), alpha = alpha)
                drawLine(Color(0xFFE0F2FE), Offset(fx, fy - arm), Offset(fx, fy + arm), 1.dp.toPx(), alpha = alpha)
            }
        }
        TitleVfxTier.VOID_NEBULA -> {
            val pulse = 0.5f + 0.5f * sin(phase * Math.PI * 2.0).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC084FC).copy(alpha = 0.38f * pulse), Color.Transparent),
                    center = Offset(startX + span * (0.3f + 0.4f * phase), (topY + bottomY) * 0.5f),
                    radius = span * 0.42f
                ),
                radius = span * 0.42f,
                center = Offset(startX + span * (0.3f + 0.4f * phase), (topY + bottomY) * 0.5f)
            )
        }
        TitleVfxTier.SOLAR_FLARE -> {
            val rayX = startX + span * phase
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(rayX, (topY + bottomY) * 0.5f),
                    radius = (bottomY - topY) * 1.15f
                ),
                radius = (bottomY - topY) * 1.15f,
                center = Offset(rayX, (topY + bottomY) * 0.5f)
            )
        }
        else -> Unit
    }
}

/**
 * Efek meriah foreground di atas tepi bingkai khusus Title berbayar/mewah:
 * - [TitleVfxTier.BLAZING_INFERNO]: Lidah api menari & percikan bara di atas pinggir badge.
 * - [TitleVfxTier.LIGHTNING_STORM]: Sambaran petir zigzag bercabang melintasi sisi kiri/kanan.
 * - [TitleVfxTier.SOLAR_FLARE]: Korona partikel bintang orbital di pinggir badge.
 */
private fun DrawScope.drawForegroundRichFx(
    vfxTier: TitleVfxTier,
    palette: BadgeTierPalette,
    phase: Float,
    compact: Boolean
) {
    val w = size.width
    val h = size.height
    when (vfxTier) {
        TitleVfxTier.BLAZING_INFERNO -> {
            val flameCount = if (compact) 7 else 10
            val baseY = h * 0.16f
            val maxFlameH = if (compact) 5.5.dp.toPx() else 8.5.dp.toPx()
            for (i in 0 until flameCount) {
                val norm = (i + 0.5f) / flameCount
                val fx = w * (0.14f + norm * 0.78f)
                val wave = sin((phase * 2.0 * Math.PI) + i * 1.3).toFloat()
                val heightF = maxFlameH * (0.55f + 0.45f * abs(wave))
                val sway = wave * (if (compact) 1.8.dp.toPx() else 2.8.dp.toPx())
                val tongue = Path().apply {
                    moveTo(fx - (if (compact) 3.dp.toPx() else 4.2.dp.toPx()), baseY + 1.5.dp.toPx())
                    quadraticTo(fx - 1.5.dp.toPx(), baseY - heightF * 0.55f, fx + sway, baseY - heightF)
                    quadraticTo(fx + 2.dp.toPx(), baseY - heightF * 0.45f, fx + (if (compact) 3.dp.toPx() else 4.2.dp.toPx()), baseY + 1.5.dp.toPx())
                    close()
                }
                drawPath(
                    path = tongue,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = 0.85f),
                            Color(0xFFFF6D00).copy(alpha = 0.72f),
                            Color(0xFFE11D48).copy(alpha = 0.25f)
                        ),
                        startY = baseY - heightF,
                        endY = baseY + 2.dp.toPx()
                    )
                )
            }
        }
        TitleVfxTier.LIGHTNING_STORM -> {
            // 2 busur petir bercabang di sisi kiri & kanan bingkai
            val flashWindow = ((phase * 3f) % 1f)
            val intensity = if (flashWindow < 0.75f) 0.95f else 0.35f

            val bolt1Start = w * (0.18f + 0.52f * phase)
            val boltPath1 = Path().apply {
                moveTo(bolt1Start, h * 0.05f)
                lineTo(bolt1Start + 5.dp.toPx(), h * 0.36f)
                lineTo(bolt1Start - 2.dp.toPx(), h * 0.56f)
                lineTo(bolt1Start + 7.dp.toPx(), h * 0.94f)
            }
            val branch1 = Path().apply {
                moveTo(bolt1Start - 2.dp.toPx(), h * 0.56f)
                lineTo(bolt1Start - 7.dp.toPx(), h * 0.82f)
            }
            drawPath(
                path = boltPath1,
                color = Color(0xFF38BDF8).copy(alpha = 0.50f * intensity),
                style = Stroke(width = if (compact) 2.4.dp.toPx() else 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Miter)
            )
            drawPath(
                path = boltPath1,
                color = Color(0xFFE0F2FE).copy(alpha = intensity),
                style = Stroke(width = if (compact) 1.dp.toPx() else 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Miter)
            )
            drawPath(
                path = branch1,
                color = Color(0xFFBAE6FD).copy(alpha = 0.85f * intensity),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        TitleVfxTier.SOLAR_FLARE -> {
            for (i in 0..3) {
                val p = (phase + i * 0.25f) % 1f
                val sx = w * p
                val sy = if (i % 2 == 0) h * 0.11f else h * 0.89f
                val sparkR = (if (compact) 1.8.dp.toPx() else 2.5.dp.toPx()) * abs(sin(p * Math.PI)).toFloat()
                if (sparkR > 0.2f) {
                    drawCircle(Color(0xFFFEF08A), radius = sparkR, center = Offset(sx, sy), alpha = 0.9f)
                }
            }
        }
        else -> Unit
    }
}

private fun centeredBadgeTextStyle(fontSize: TextUnit) = TextStyle(
    lineHeight = fontSize,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

/**
 * Komponen Micro-Title Tags untuk Profil & Peer Profile Sheet (tetap mempertahankan tampilan default profil).
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
    val roleSpec = remember(role) { RpgSvgIconCatalog.resolveRoleSpec(role) }
    val miniIconRes = remember(resolvedTitle, level) { RpgSvgIconCatalog.resolveMiniTitleIcon(resolvedTitle, level) }
    val lvIconRes = remember(level) { RpgSvgIconCatalog.resolveLevelIcon(level) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val roleAccent = Color(roleSpec.accentHex)
        SleekChamferedMicroTag(
            text = role.uppercase(Locale.ROOT),
            borderColor = roleAccent.copy(alpha = 0.55f),
            backgroundColor = Color(0xFF101219),
            textColor = if (roleSpec.isExclusiveStaff) roleAccent else Color(0xFFE2E8F0),
            useMonospace = false
        ) {
            Image(
                painter = painterResource(id = roleSpec.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(roleAccent),
                modifier = Modifier.size(11.dp)
            )
        }

        val lvAccent = when {
            level >= 60 -> Color(0xFFF59E0B)
            level >= 35 -> Color(0xFFF3C663)
            level >= 15 -> Color(0xFFA855F7)
            else -> Color(0xFFFF5722)
        }
        SleekHexChevronLevelTag(
            level = level,
            accentColor = lvAccent,
            levelIconRes = lvIconRes
        )

        val classAccent = when {
            level >= 50 -> Color(0xFFF59E0B)
            level >= 25 -> Color(0xFFEC4899)
            else -> Color(0xFFE11D48)
        }
        SleekChamferedMicroTag(
            text = resolvedTitle.uppercase(Locale.ROOT),
            borderColor = classAccent.copy(alpha = 0.55f),
            backgroundColor = Color(0xFF141016),
            textColor = Color(0xFFFDA4AF),
            useMonospace = false
        ) {
            Image(
                painter = painterResource(id = miniIconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(classAccent),
                modifier = Modifier.size(11.dp)
            )
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
            style = centeredBadgeTextStyle(10.sp),
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
    },
    levelIconRes: Int = RpgSvgIconCatalog.resolveLevelIcon(level)
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
        Image(
            painter = painterResource(id = levelIconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accentColor),
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.5.dp))
        Text(
            text = "LV. $level",
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = JetBrainsMonoFontFamily,
            letterSpacing = 0.4.sp,
            style = centeredBadgeTextStyle(10.5.sp),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Kerangka Heksagonal + Kapsul Bergaris Putus-Putus ketika pengguna belum memasang Title khusus.
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
