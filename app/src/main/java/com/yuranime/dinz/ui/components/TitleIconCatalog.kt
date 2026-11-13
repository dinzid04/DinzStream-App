package com.yuranime.dinz.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * 8 Variasi Animasi Berbeda untuk Custom Title Badge (TAHAP 2)
 */
enum class TitleAnimationType {
    PLASMA_BORDER_FLOW,
    HOLOGRAPHIC_FOIL,
    CRIMSON_EMBER_DRIFT,
    LIGHTNING_ARC_SWEEP,
    CELESTIAL_ORBIT,
    LIQUID_GOLD_WAVE,
    CYBER_PULSE_GLOW,
    STARLIGHT_TWINKLE;

    companion object {
        fun fromIndex(index: Int): TitleAnimationType {
            val values = entries
            return values[(index.coerceAtLeast(0)) % values.size]
        }

        fun resolveForTitle(titleName: String, tierLevel: Int, variant: Int = 0): TitleAnimationType {
            val upper = titleName.uppercase(Locale.ROOT)
            return when {
                upper.contains("SOVEREIGN") || upper.contains("RAJA") || upper.contains("KING") || upper.contains("KAISAR") -> LIQUID_GOLD_WAVE
                upper.contains("CRIMSON") || upper.contains("API") || upper.contains("FLAME") || upper.contains("PHOENIX") || upper.contains("IBLIS") -> CRIMSON_EMBER_DRIFT
                upper.contains("PETIR") || upper.contains("THUNDER") || upper.contains("LIGHTNING") || upper.contains("RAIJIN") || upper.contains("VANGUARD") -> LIGHTNING_ARC_SWEEP
                upper.contains("CYBER") || upper.contains("MECHA") || upper.contains("NEON") || upper.contains("GLITCH") || upper.contains("MATRIX") -> CYBER_PULSE_GLOW
                upper.contains("DIMENSI") || upper.contains("KOSMIK") || upper.contains("GALAXY") || upper.contains("ASTRAL") || upper.contains("ORBIT") -> CELESTIAL_ORBIT
                upper.contains("BANGSAWAN") || upper.contains("ROYAL") || upper.contains("AMETHYST") || upper.contains("MYTHIC") || upper.contains("HOLO") -> HOLOGRAPHIC_FOIL
                upper.contains("BINTANG") || upper.contains("STAR") || upper.contains("MALAM") || upper.contains("SAKURA") || upper.contains("KOMIK") -> STARLIGHT_TWINKLE
                else -> fromIndex((upper.hashCode() and 0x7FFFFFFF) + tierLevel + variant)
            }
        }
    }
}

/**
 * Katalog 160+ Ikon Vektor Unik untuk Custom Title Badge (`TitleIconCatalog.kt`).
 * Menggambar bentuk vektor presisi (Senjata, Mitologi, Sihir, Kosmik, Mecha, Kerajaan)
 * langsung di dalam emblem heksagon kiri agar tidak ada deretan title dengan ikon kembar.
 */
object TitleIconCatalog {

    const val TOTAL_UNIQUE_ICONS = 160

    fun resolveIconId(titleName: String, explicitId: Int = -1): Int {
        if (explicitId in 0 until TOTAL_UNIQUE_ICONS) return explicitId
        val u = titleName.uppercase(Locale.ROOT)
        val baseArchetype = when {
            u.contains("WARGA") || u.contains("MEMBER") -> 0       // Knight Shield
            u.contains("PERINTIS") || u.contains("PIONEER") -> 1   // Thunder Arrow / Compass Spear
            u.contains("PENJELAJAH") || u.contains("KOMIK") -> 2   // Grimoire Book
            u.contains("VANGUARD") || u.contains("OTAKU") -> 3     // Crossed Katana
            u.contains("DIMENSI") || u.contains("VOID") -> 4       // Eye of Fate / Astral Portal
            u.contains("SEPUH") || u.contains("LEGENDA") -> 5      // Sovereign Crown
            u.contains("KSATRIA") || u.contains("CRIMSON") -> 6    // Greatsword
            u.contains("MARATON") || u.contains("WAKTU") -> 7      // Hourglass of Time
            u.contains("BANGSAWAN") || u.contains("ROYAL") -> 8    // Lotus / Royal Crest
            u.contains("SOVEREIGN") || u.contains("MYTHIC") -> 9   // Dragon Crest
            u.contains("VENOM") || u.contains("SYMBIOTE") -> 10    // Symbiote Fang / Spider Crest
            u.contains("SHINOBI") || u.contains("NINJA") -> 11     // Shuriken
            u.contains("REAPER") || u.contains("MAUT") -> 12       // Grim Reaper Scythe
            u.contains("KITSUNE") || u.contains("RUBAH") -> 13     // Kitsune Mask
            u.contains("IBLIS") || u.contains("DEMON") -> 14       // Demon Horns
            u.contains("MALAIKAT") || u.contains("ANGEL") -> 15    // Angel Wings
            u.contains("PHOENIX") || u.contains("API") -> 16       // Spirit Flame / Phoenix
            u.contains("SERIGALA") || u.contains("WOLF") -> 17     // Night Wolf
            u.contains("TENGKORAK") || u.contains("SKULL") -> 18   // Crowned Skull
            u.contains("KRISTAL") || u.contains("RUNE") -> 19      // Rune Crystal
            u.contains("SAKURA") || u.contains("BUNGA") -> 20      // Sakura Blossom
            u.contains("BULAN") || u.contains("MOON") -> 21        // Blood Crescent
            u.contains("GERHANA") || u.contains("ECLIPSE") -> 22   // Solar Eclipse
            u.contains("MECHA") || u.contains("ROBOT") -> 23       // Mecha Core
            u.contains("CYBER") || u.contains("NEON") -> 24        // Cyberpunk Visor
            u.contains("KOSMIK") || u.contains("SATURN") -> 25     // Saturn Ring
            u.contains("PETIR") || u.contains("RAIJIN") -> 26      // Lightning Bolt
            u.contains("KAPAK") || u.contains("VIKING") -> 27      // Viking Axe
            u.contains("KUNAI") || u.contains("ASSASSIN") -> 28    // Kunai Dagger
            u.contains("OUROBOROS") || u.contains("ULAR") -> 29    // Ouroboros Serpent
            u.contains("TRISULA") || u.contains("SAMUDRA") -> 30   // Abyss Trident
            u.contains("ALKIMIA") || u.contains("SIHIR") -> 31     // Alchemy Circle
            else -> {
                var hash = 7
                for (ch in u) {
                    hash = (hash * 31 + ch.code) and 0x7FFFFFFF
                }
                hash % TOTAL_UNIQUE_ICONS
            }
        }
        return baseArchetype % TOTAL_UNIQUE_ICONS
    }

    fun DrawScope.drawTitleVectorIcon(
        iconId: Int,
        center: Offset,
        radius: Float,
        primaryColor: Color,
        secondaryColor: Color,
         isAnimatedPulse: Float = 0.5f
    ) {
        val archetype = (iconId.coerceAtLeast(0)) % 32
        val ornamentTier = ((iconId.coerceAtLeast(0)) / 32) % 5
        val r = radius * 0.78f
        val stroke = (radius * 0.14f).coerceAtLeast(1.6f)
        val thinStroke = (stroke * 0.72f).coerceAtLeast(1.1f)

        // Ornamen latar mikro (membedakan hingga 160 variasi unik: 32 bentuk utama x 5 cincin/aksen)
        when (ornamentTier) {
            1 -> {
                // 4 titik kardinal mikro
                val d = r * 1.05f
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = stroke * 0.45f, center = Offset(center.x, center.y - d))
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = stroke * 0.45f, center = Offset(center.x, center.y + d))
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = stroke * 0.45f, center = Offset(center.x - d, center.y))
                drawCircle(secondaryColor.copy(alpha = 0.7f), radius = stroke * 0.45f, center = Offset(center.x + d, center.y))
            }
            2 -> {
                // Cincin orbit putus-putus halus
                drawArc(
                    color = secondaryColor.copy(alpha = 0.55f),
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.02f, center.y - r * 1.02f),
                    size = Size(r * 2.04f, r * 2.04f),
                    style = Stroke(width = thinStroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = secondaryColor.copy(alpha = 0.55f),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 1.02f, center.y - r * 1.02f),
                    size = Size(r * 2.04f, r * 2.04f),
                    style = Stroke(width = thinStroke, cap = StrokeCap.Round)
                )
            }
            3 -> {
                // Belah ketupat luar tipis
                val dm = Path().apply {
                    moveTo(center.x, center.y - r * 1.08f)
                    lineTo(center.x + r * 1.08f, center.y)
                    lineTo(center.x, center.y + r * 1.08f)
                    lineTo(center.x - r * 1.08f, center.y)
                    close()
                }
                drawPath(dm, secondaryColor.copy(alpha = 0.4f), style = Stroke(width = thinStroke))
            }
            4 -> {
                // Dua sayap bracket samping
                drawLine(secondaryColor.copy(alpha = 0.65f), Offset(center.x - r, center.y - r * 0.4f), Offset(center.x - r * 1.12f, center.y), thinStroke, StrokeCap.Round)
                drawLine(secondaryColor.copy(alpha = 0.65f), Offset(center.x - r * 1.12f, center.y), Offset(center.x - r, center.y + r * 0.4f), thinStroke, StrokeCap.Round)
                drawLine(secondaryColor.copy(alpha = 0.65f), Offset(center.x + r, center.y - r * 0.4f), Offset(center.x + r * 1.12f, center.y), thinStroke, StrokeCap.Round)
                drawLine(secondaryColor.copy(alpha = 0.65f), Offset(center.x + r * 1.12f, center.y), Offset(center.x + r, center.y + r * 0.4f), thinStroke, StrokeCap.Round)
            }
        }

        when (archetype) {
            0 -> {
                // 0: KNIGHT SHIELD (Perisai Ksatria)
                val p = Path().apply {
                    moveTo(center.x, center.y - r * 0.92f)
                    lineTo(center.x + r * 0.78f, center.y - r * 0.55f)
                    lineTo(center.x + r * 0.65f, center.y + r * 0.25f)
                    lineTo(center.x, center.y + r * 0.95f)
                    lineTo(center.x - r * 0.65f, center.y + r * 0.25f)
                    lineTo(center.x - r * 0.78f, center.y - r * 0.55f)
                    close()
                }
                drawPath(p, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawLine(primaryColor, Offset(center.x, center.y - r * 0.55f), Offset(center.x, center.y + r * 0.55f), thinStroke, StrokeCap.Round)
                drawLine(primaryColor, Offset(center.x - r * 0.38f, center.y - r * 0.1f), Offset(center.x + r * 0.38f, center.y - r * 0.1f), thinStroke, StrokeCap.Round)
            }
            1 -> {
                // 1: THUNDER ARROW / SPEARHEAD (Tombak Panah Petir)
                val p = Path().apply {
                    moveTo(center.x, center.y - r * 0.98f)
                    lineTo(center.x + r * 0.65f, center.y + r * 0.25f)
                    lineTo(center.x + r * 0.18f, center.y + r * 0.05f)
                    lineTo(center.x + r * 0.32f, center.y + r * 0.92f)
                    lineTo(center.x, center.y + r * 0.55f)
                    lineTo(center.x - r * 0.32f, center.y + r * 0.92f)
                    lineTo(center.x - r * 0.18f, center.y + r * 0.05f)
                    lineTo(center.x - r * 0.65f, center.y + r * 0.25f)
                    close()
                }
                drawPath(p, primaryColor)
            }
            2 -> {
                // 2: GRIMOIRE BOOK (Buku Sihir Terbuka)
                val p = Path().apply {
                    moveTo(center.x, center.y - r * 0.45f)
                    lineTo(center.x + r * 0.85f, center.y - r * 0.72f)
                    lineTo(center.x + r * 0.85f, center.y + r * 0.55f)
                    lineTo(center.x, center.y + r * 0.82f)
                    lineTo(center.x - r * 0.85f, center.y + r * 0.55f)
                    lineTo(center.x - r * 0.85f, center.y - r * 0.72f)
                    close()
                }
                drawPath(p, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawLine(primaryColor, Offset(center.x, center.y - r * 0.45f), Offset(center.x, center.y + r * 0.82f), thinStroke)
                drawCircle(secondaryColor, radius = r * 0.22f, center = Offset(center.x, center.y - r * 0.78f))
            }
            3 -> {
                // 3: CROSSED KATANA (Katana Silang)
                drawLine(primaryColor, Offset(center.x - r * 0.82f, center.y + r * 0.82f), Offset(center.x + r * 0.82f, center.y - r * 0.82f), stroke, StrokeCap.Round)
                drawLine(primaryColor, Offset(center.x - r * 0.28f, center.y + r * 0.58f), Offset(center.x - r * 0.58f, center.y + r * 0.28f), stroke, StrokeCap.Round)
                drawLine(primaryColor, Offset(center.x + r * 0.82f, center.y + r * 0.82f), Offset(center.x - r * 0.82f, center.y - r * 0.82f), stroke, StrokeCap.Round)
                drawLine(primaryColor, Offset(center.x + r * 0.28f, center.y + r * 0.58f), Offset(center.x + r * 0.58f, center.y + r * 0.28f), stroke, StrokeCap.Round)
                drawCircle(secondaryColor, radius = r * 0.2f, center = center)
            }
            4 -> {
                // 4: EYE OF FATE (Mata Takdir)
                val eye = Path().apply {
                    moveTo(center.x - r * 0.95f, center.y)
                    quadraticTo(center.x, center.y - r * 0.82f, center.x + r * 0.95f, center.y)
                    quadraticTo(center.x, center.y + r * 0.82f, center.x - r * 0.95f, center.y)
                    close()
                }
                drawPath(eye, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawCircle(primaryColor, radius = r * 0.34f, center = center)
                drawCircle(Color(0xFF0E1015), radius = r * 0.14f, center = center)
            }
            5 -> {
                // 5: SOVEREIGN CROWN (Mahkota Raja 3 Puncak)
                val crown = Path().apply {
                    moveTo(center.x - r * 0.82f, center.y + r * 0.58f)
                    lineTo(center.x - r * 0.92f, center.y - r * 0.38f)
                    lineTo(center.x - r * 0.42f, center.y + r * 0.08f)
                    lineTo(center.x, center.y - r * 0.78f)
                    lineTo(center.x + r * 0.42f, center.y + r * 0.08f)
                    lineTo(center.x + r * 0.92f, center.y - r * 0.38f)
                    lineTo(center.x + r * 0.82f, center.y + r * 0.58f)
                    close()
                }
                drawPath(crown, primaryColor)
                drawCircle(secondaryColor, radius = stroke * 0.65f, center = Offset(center.x, center.y - r * 0.88f))
            }
            6 -> {
                // 6: GREATSWORD (Pedang Besar Vertikal)
                val blade = Path().apply {
                    moveTo(center.x, center.y - r * 0.98f)
                    lineTo(center.x + r * 0.28f, center.y - r * 0.45f)
                    lineTo(center.x + r * 0.22f, center.y + r * 0.28f)
                    lineTo(center.x - r * 0.22f, center.y + r * 0.28f)
                    lineTo(center.x - r * 0.28f, center.y - r * 0.45f)
                    close()
                }
                drawPath(blade, primaryColor)
                drawLine(primaryColor, Offset(center.x - r * 0.68f, center.y + r * 0.32f), Offset(center.x + r * 0.68f, center.y + r * 0.32f), stroke * 1.15f, StrokeCap.Round)
                drawLine(primaryColor, Offset(center.x, center.y + r * 0.32f), Offset(center.x, center.y + r * 0.92f), stroke, StrokeCap.Round)
            }
            7 -> {
                // 7: HOURGLASS OF TIME (Jam Pasir Waktu)
                val hg = Path().apply {
                    moveTo(center.x - r * 0.68f, center.y - r * 0.82f)
                    lineTo(center.x + r * 0.68f, center.y - r * 0.82f)
                    lineTo(center.x - r * 0.68f, center.y + r * 0.82f)
                    lineTo(center.x + r * 0.68f, center.y + r * 0.82f)
                    close()
                }
                drawPath(hg, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawCircle(secondaryColor, radius = r * 0.18f, center = center)
            }
            8 -> {
                // 8: LOTUS BLOSSOM (Bunga Teratai)
                val centerPetal = Path().apply {
                    moveTo(center.x, center.y - r * 0.92f)
                    quadraticTo(center.x + r * 0.48f, center.y - r * 0.15f, center.x, center.y + r * 0.65f)
                    quadraticTo(center.x - r * 0.48f, center.y - r * 0.15f, center.x, center.y - r * 0.92f)
                }
                drawPath(centerPetal, primaryColor, style = Stroke(width = stroke))
                val sidePetals = Path().apply {
                    moveTo(center.x, center.y + r * 0.65f)
                    quadraticTo(center.x + r * 0.95f, center.y + r * 0.25f, center.x + r * 0.82f, center.y - r * 0.42f)
                    moveTo(center.x, center.y + r * 0.65f)
                    quadraticTo(center.x - r * 0.95f, center.y + r * 0.25f, center.x - r * 0.82f, center.y - r * 0.42f)
                }
                drawPath(sidePetals, primaryColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
            9 -> {
                // 9: DRAGON CREST (Lambang Naga Bersayap)
                val dr = Path().apply {
                    moveTo(center.x, center.y - r * 0.95f)
                    lineTo(center.x + r * 0.35f, center.y - r * 0.35f)
                    lineTo(center.x + r * 0.95f, center.y - r * 0.58f)
                    lineTo(center.x + r * 0.58f, center.y + r * 0.22f)
                    lineTo(center.x, center.y + r * 0.95f)
                    lineTo(center.x - r * 0.58f, center.y + r * 0.22f)
                    lineTo(center.x - r * 0.95f, center.y - r * 0.58f)
                    lineTo(center.x - r * 0.35f, center.y - r * 0.35f)
                    close()
                }
                drawPath(dr, primaryColor)
            }
            10 -> {
                // 10: SYMBIOTE VENOM / SPIDER CREST (Taring & Laba-Laba Symbiote)
                val sp = Path().apply {
                    moveTo(center.x, center.y - r * 0.85f)
                    lineTo(center.x + r * 0.32f, center.y)
                    lineTo(center.x, center.y + r * 0.92f)
                    lineTo(center.x - r * 0.32f, center.y)
                    close()
                }
                drawPath(sp, primaryColor)
                for (dir in listOf(-1f, 1f)) {
                    drawLine(primaryColor, Offset(center.x, center.y - r * 0.15f), Offset(center.x + dir * r * 0.92f, center.y - r * 0.72f), thinStroke, StrokeCap.Round)
                    drawLine(primaryColor, Offset(center.x, center.y + r * 0.05f), Offset(center.x + dir * r * 0.98f, center.y), thinStroke, StrokeCap.Round)
                    drawLine(primaryColor, Offset(center.x, center.y + r * 0.25f), Offset(center.x + dir * r * 0.88f, center.y + r * 0.78f), thinStroke, StrokeCap.Round)
                }
            }
            11 -> {
                // 11: SHURIKEN 4-BLADE (Shuriken Ninja)
                rotate(degrees = 15f, pivot = center) {
                    val sh = Path().apply {
                        moveTo(center.x, center.y - r * 0.98f)
                        lineTo(center.x + r * 0.26f, center.y - r * 0.26f)
                        lineTo(center.x + r * 0.98f, center.y)
                        lineTo(center.x + r * 0.26f, center.y + r * 0.26f)
                        lineTo(center.x, center.y + r * 0.98f)
                        lineTo(center.x - r * 0.26f, center.y + r * 0.26f)
                        lineTo(center.x - r * 0.98f, center.y)
                        lineTo(center.x - r * 0.26f, center.y - r * 0.26f)
                        close()
                    }
                    drawPath(sh, primaryColor)
                    drawCircle(Color(0xFF0E1015), radius = r * 0.22f, center = center)
                }
            }
            12 -> {
                // 12: GRIM REAPER SCYTHE (Sabit Maut)
                drawLine(primaryColor, Offset(center.x - r * 0.55f, center.y + r * 0.88f), Offset(center.x + r * 0.35f, center.y - r * 0.82f), stroke, StrokeCap.Round)
                val blade = Path().apply {
                    moveTo(center.x + r * 0.35f, center.y - r * 0.82f)
                    quadraticTo(center.x - r * 0.65f, center.y - r * 0.92f, center.x - r * 0.92f, center.y - r * 0.05f)
                    quadraticTo(center.x - r * 0.35f, center.y - r * 0.45f, center.x + r * 0.22f, center.y - r * 0.45f)
                    close()
                }
                drawPath(blade, primaryColor)
            }
            13 -> {
                // 13: KITSUNE MASK (Topeng Rubah Kitsune)
                val mask = Path().apply {
                    moveTo(center.x - r * 0.78f, center.y - r * 0.88f)
                    lineTo(center.x - r * 0.25f, center.y - r * 0.35f)
                    lineTo(center.x + r * 0.25f, center.y - r * 0.35f)
                    lineTo(center.x + r * 0.78f, center.y - r * 0.88f)
                    lineTo(center.x + r * 0.72f, center.y + r * 0.15f)
                    lineTo(center.x, center.y + r * 0.92f)
                    lineTo(center.x - r * 0.72f, center.y + r * 0.15f)
                    close()
                }
                drawPath(mask, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawLine(secondaryColor, Offset(center.x - r * 0.42f, center.y + r * 0.05f), Offset(center.x - r * 0.15f, center.y + r * 0.18f), thinStroke, StrokeCap.Round)
                drawLine(secondaryColor, Offset(center.x + r * 0.42f, center.y + r * 0.05f), Offset(center.x + r * 0.15f, center.y + r * 0.18f), thinStroke, StrokeCap.Round)
            }
            14 -> {
                // 14: DEMON HORNS (Tanduk Iblis Kembar)
                val horns = Path().apply {
                    moveTo(center.x - r * 0.22f, center.y + r * 0.65f)
                    quadraticTo(center.x - r * 0.98f, center.y + r * 0.25f, center.x - r * 0.72f, center.y - r * 0.88f)
                    quadraticTo(center.x - r * 0.42f, center.y - r * 0.15f, center.x, center.y + r * 0.22f)
                    quadraticTo(center.x + r * 0.42f, center.y - r * 0.15f, center.x + r * 0.72f, center.y - r * 0.88f)
                    quadraticTo(center.x + r * 0.98f, center.y + r * 0.25f, center.x + r * 0.22f, center.y + r * 0.65f)
                    close()
                }
                drawPath(horns, primaryColor)
            }
            15 -> {
                // 15: ANGEL / VALKYRIE WINGS (Sayap Kembar)
                for (dir in listOf(-1f, 1f)) {
                    val wing = Path().apply {
                        moveTo(center.x + dir * r * 0.12f, center.y + r * 0.45f)
                        quadraticTo(center.x + dir * r * 0.98f, center.y - r * 0.15f, center.x + dir * r * 0.88f, center.y - r * 0.85f)
                        quadraticTo(center.x + dir * r * 0.35f, center.y - r * 0.45f, center.x + dir * r * 0.12f, center.y - r * 0.05f)
                        close()
                    }
                    drawPath(wing, primaryColor)
                }
                drawCircle(secondaryColor, radius = r * 0.2f, center = Offset(center.x, center.y - r * 0.55f))
            }
            16 -> {
                // 16: SPIRIT FLAME / PHOENIX (Api Roh)
                val flame = Path().apply {
                    moveTo(center.x, center.y - r * 0.95f)
                    quadraticTo(center.x + r * 0.88f, center.y - r * 0.12f, center.x + r * 0.45f, center.y + r * 0.68f)
                    quadraticTo(center.x, center.y + r * 0.98f, center.x - r * 0.45f, center.y + r * 0.68f)
                    quadraticTo(center.x - r * 0.88f, center.y - r * 0.12f, center.x, center.y - r * 0.95f)
                    close()
                }
                drawPath(flame, primaryColor)
                drawCircle(Color(0xFF0E1015), radius = r * 0.28f, center = Offset(center.x, center.y + r * 0.28f))
            }
            17 -> {
                // 17: NIGHT WOLF (Serigala Malam)
                val wolf = Path().apply {
                    moveTo(center.x - r * 0.65f, center.y - r * 0.88f)
                    lineTo(center.x - r * 0.25f, center.y - r * 0.38f)
                    lineTo(center.x + r * 0.25f, center.y - r * 0.38f)
                    lineTo(center.x + r * 0.65f, center.y - r * 0.88f)
                    lineTo(center.x + r * 0.82f, center.y + r * 0.05f)
                    lineTo(center.x, center.y + r * 0.92f)
                    lineTo(center.x - r * 0.82f, center.y + r * 0.05f)
                    close()
                }
                drawPath(wolf, primaryColor)
            }
            18 -> {
                // 18: CROWNED SKULL (Tengkorak Mahkota)
                drawCircle(primaryColor, radius = r * 0.62f, center = Offset(center.x, center.y - r * 0.12f), style = Stroke(width = stroke))
                drawCircle(primaryColor, radius = r * 0.15f, center = Offset(center.x - r * 0.24f, center.y - r * 0.15f))
                drawCircle(primaryColor, radius = r * 0.15f, center = Offset(center.x + r * 0.24f, center.y - r * 0.15f))
                drawLine(primaryColor, Offset(center.x - r * 0.32f, center.y + r * 0.62f), Offset(center.x + r * 0.32f, center.y + r * 0.62f), stroke, StrokeCap.Round)
            }
            19 -> {
                // 19: RUNE CRYSTAL (Kristal Heksagonal Meruncing)
                val cr = Path().apply {
                    moveTo(center.x, center.y - r * 0.96f)
                    lineTo(center.x + r * 0.56f, center.y - r * 0.32f)
                    lineTo(center.x + r * 0.56f, center.y + r * 0.32f)
                    lineTo(center.x, center.y + r * 0.96f)
                    lineTo(center.x - r * 0.56f, center.y + r * 0.32f)
                    lineTo(center.x - r * 0.56f, center.y - r * 0.32f)
                    close()
                }
                drawPath(cr, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawLine(secondaryColor, Offset(center.x, center.y - r * 0.92f), Offset(center.x, center.y + r * 0.92f), thinStroke)
            }
            20 -> {
                // 20: SAKURA BLOSSOM (5 Kelopak Sakura)
                for (i in 0 until 5) {
                    val angle = Math.toRadians((i * 72 - 90).toDouble())
                    val px = center.x + (r * 0.52f * cos(angle)).toFloat()
                    val py = center.y + (r * 0.52f * sin(angle)).toFloat()
                    drawCircle(primaryColor, radius = r * 0.32f, center = Offset(px, py))
                }
                drawCircle(Color(0xFF0E1015), radius = r * 0.22f, center = center)
            }
            21 -> {
                // 21: BLOOD CRESCENT (Bulan Sabit)
                val moon = Path().apply {
                    addArc(Rect(center.x - r * 0.85f, center.y - r * 0.85f, center.x + r * 0.85f, center.y + r * 0.85f), 35f, 290f)
                }
                drawPath(moon, primaryColor, style = Stroke(width = stroke * 1.25f, cap = StrokeCap.Round))
                drawCircle(secondaryColor, radius = r * 0.22f, center = Offset(center.x + r * 0.38f, center.y - r * 0.05f))
            }
            22 -> {
                // 22: SOLAR ECLIPSE (Gerhana Matahari)
                drawCircle(primaryColor, radius = r * 0.65f, center = center, style = Stroke(width = stroke))
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    val x0 = center.x + (r * 0.72f * cos(a)).toFloat()
                    val y0 = center.y + (r * 0.72f * sin(a)).toFloat()
                    val x1 = center.x + (r * 0.98f * cos(a)).toFloat()
                    val y1 = center.y + (r * 0.98f * sin(a)).toFloat()
                    drawLine(primaryColor, Offset(x0, y0), Offset(x1, y1), thinStroke, StrokeCap.Round)
                }
            }
            23 -> {
                // 23: MECHA CORE (Inti Reaktor Mecha Segitiga)
                val tri = Path().apply {
                    moveTo(center.x, center.y + r * 0.88f)
                    lineTo(center.x + r * 0.85f, center.y - r * 0.58f)
                    lineTo(center.x - r * 0.85f, center.y - r * 0.58f)
                    close()
                }
                drawPath(tri, primaryColor, style = Stroke(width = stroke, join = StrokeJoin.Round))
                drawCircle(secondaryColor, radius = r * 0.25f, center = Offset(center.x, center.y - r * 0.1f))
            }
            24 -> {
                // 24: CYBERPUNK VISOR (Visor V-Tech)
                val visor = Path().apply {
                    moveTo(center.x - r * 0.92f, center.y - r * 0.35f)
                    lineTo(center.x, center.y + r * 0.25f)
                    lineTo(center.x + r * 0.92f, center.y - r * 0.35f)
                    lineTo(center.x + r * 0.72f, center.y + r * 0.45f)
                    lineTo(center.x, center.y + r * 0.82f)
                    lineTo(center.x - r * 0.72f, center.y + r * 0.45f)
                    close()
                }
                drawPath(visor, primaryColor)
            }
            25 -> {
                // 25: SATURN RING (Planet Bercincin)
                drawCircle(primaryColor, radius = r * 0.48f, center = center)
                rotate(degrees = -25f, pivot = center) {
                    drawOval(
                        color = secondaryColor,
                        topLeft = Offset(center.x - r * 0.98f, center.y - r * 0.28f),
                        size = Size(r * 1.96f, r * 0.56f),
                        style = Stroke(width = stroke)
                    )
                }
            }
            26 -> {
                // 26: LIGHTNING BOLT (Sambaran Petir Tajam)
                val bolt = Path().apply {
                    moveTo(center.x + r * 0.22f, center.y - r * 0.98f)
                    lineTo(center.x - r * 0.62f, center.y + r * 0.08f)
                    lineTo(center.x + r * 0.05f, center.y + r * 0.08f)
                    lineTo(center.x - r * 0.25f, center.y + r * 0.98f)
                    lineTo(center.x + r * 0.65f, center.y - r * 0.12f)
                    lineTo(center.x - r * 0.05f, center.y - r * 0.12f)
                    close()
                }
                drawPath(bolt, primaryColor)
            }
            27 -> {
                // 27: VIKING BATTLE AXE (Kapak Kembar)
                drawLine(primaryColor, Offset(center.x, center.y - r * 0.92f), Offset(center.x, center.y + r * 0.92f), stroke, StrokeCap.Round)
                val axe = Path().apply {
                    moveTo(center.x, center.y - r * 0.25f)
                    quadraticTo(center.x + r * 0.88f, center.y - r * 0.78f, center.x + r * 0.82f, center.y + r * 0.35f)
                    quadraticTo(center.x + r * 0.45f, center.y + r * 0.05f, center.x, center.y + r * 0.15f)
                    quadraticTo(center.x - r * 0.45f, center.y + r * 0.05f, center.x - r * 0.82f, center.y + r * 0.35f)
                    quadraticTo(center.x - r * 0.88f, center.y - r * 0.78f, center.x, center.y - r * 0.25f)
                    close()
                }
                drawPath(axe, primaryColor)
            }
            28 -> {
                // 28: KUNAI DAGGER (Kunai Ninja)
                val kunai = Path().apply {
                    moveTo(center.x, center.y - r * 0.98f)
                    lineTo(center.x + r * 0.48f, center.y + r * 0.05f)
                    lineTo(center.x, center.y + r * 0.32f)
                    lineTo(center.x - r * 0.48f, center.y + r * 0.05f)
                    close()
                }
                drawPath(kunai, primaryColor)
                drawCircle(primaryColor, radius = r * 0.24f, center = Offset(center.x, center.y + r * 0.68f), style = Stroke(width = stroke))
            }
            29 -> {
                // 29: OUROBOROS SERPENT (Cincin Naga Melingkar)
                drawArc(
                    color = primaryColor,
                    startAngle = 30f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 0.78f, center.y - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    style = Stroke(width = stroke * 1.2f, cap = StrokeCap.Round)
                )
                drawCircle(secondaryColor, radius = r * 0.24f, center = Offset(center.x + r * 0.68f, center.y - r * 0.12f))
            }
            30 -> {
                // 30: ABYSS TRIDENT (Trisula Samudra)
                drawLine(primaryColor, Offset(center.x, center.y - r * 0.92f), Offset(center.x, center.y + r * 0.92f), stroke, StrokeCap.Round)
                val prongs = Path().apply {
                    moveTo(center.x - r * 0.65f, center.y - r * 0.68f)
                    lineTo(center.x - r * 0.65f, center.y - r * 0.05f)
                    lineTo(center.x + r * 0.65f, center.y - r * 0.05f)
                    lineTo(center.x + r * 0.65f, center.y - r * 0.68f)
                }
                drawPath(prongs, primaryColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            else -> {
                // 31: ALCHEMY HEXAGRAM (Bintang Alkimia Enam Sudut)
                val t1 = Path().apply {
                    moveTo(center.x, center.y - r * 0.92f)
                    lineTo(center.x + r * 0.8f, center.y + r * 0.46f)
                    lineTo(center.x - r * 0.8f, center.y + r * 0.46f)
                    close()
                }
                val t2 = Path().apply {
                    moveTo(center.x, center.y + r * 0.92f)
                    lineTo(center.x + r * 0.8f, center.y - r * 0.46f)
                    lineTo(center.x - r * 0.8f, center.y - r * 0.46f)
                    close()
                }
                drawPath(t1, primaryColor, style = Stroke(width = thinStroke, join = StrokeJoin.Round))
                drawPath(t2, primaryColor, style = Stroke(width = thinStroke, join = StrokeJoin.Round))
            }
        }
    }
}
