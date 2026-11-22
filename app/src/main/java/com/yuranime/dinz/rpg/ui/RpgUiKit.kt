package com.yuranime.dinz.rpg.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.SfProFontFamily
import java.util.concurrent.ConcurrentHashMap

// ==================== WARM CEDAR-WOOD & CREAM PARCHMENT ANIME JRPG PALETTE ====================
val RpgParchment = Color(0xFFF6ECD8)
val RpgParchmentSoft = Color(0xFFFDF8ED)
val RpgLinen = Color(0xFFE7D6B8)
val RpgLinenSelected = Color(0xFFF3DFB2)
val RpgCedarDark = Color(0xFF3E261B)
val RpgCedarMid = Color(0xFF6B4631)
val RpgCedarLight = Color(0xFFA87A54)
val RpgGoldTrim = Color(0xFFD9A648)
val RpgInkDark = Color(0xFF281911)
val RpgInkMuted = Color(0xFF624533)
val RpgTerracotta = Color(0xFFC4452B)
val RpgForestGreen = Color(0xFF246B40)
val RpgSkyBlue = Color(0xFF2B6CB0)
val RpgBerryRed = Color(0xFF9A2020)
val RpgHudBg = Color(0xEA362217)
val RpgHudCard = Color(0xF04A3021)
val RpgCreamOnWood = Color(0xFFFDF6E7)
val RpgMutedOnWood = Color(0xFFD8C2A6)

private object RpgBitmapCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(context: android.content.Context, assetPath: String): ImageBitmap? {
        cache[assetPath]?.let { return it }
        return runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()?.also { bmp ->
            cache[assetPath] = bmp
        }
    }
}

@Composable
fun rememberRpgPixelBitmap(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetPath) {
        if (assetPath.isBlank()) null else RpgBitmapCache.get(context, assetPath)
    }
}

@Composable
fun RpgPixelSprite(
    assetPath: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val bmp = rememberRpgPixelBitmap(assetPath)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(RpgLinen)
        )
    }
}

@Composable
fun RpgPortraitFrame(
    facePath: String,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val frameBmp = rememberRpgPixelBitmap("rpg/ui/faceset_box.png")
    val faceBmp = rememberRpgPixelBitmap(facePath)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(RpgCedarMid)
            .border(2.dp, RpgCedarDark, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (frameBmp != null) {
            Image(
                bitmap = frameBmp,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (faceBmp != null) {
            Image(
                bitmap = faceBmp,
                contentDescription = null,
                filterQuality = FilterQuality.None,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding((size * 0.12f).coerceAtLeast(4.dp))
            )
        }
    }
}

@Composable
fun RpgWoodWindow(
    title: String,
    subtitle: String? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    headerExtra: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val outerShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(outerShape)
            .background(RpgCedarDark)
            .border(2.5.dp, RpgGoldTrim, outerShape)
            .padding(5.dp)
            .clip(innerShape)
            .background(RpgParchment)
            .border(1.5.dp, RpgCedarLight, innerShape)
    ) {
        // Warm Cedar Wood Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RpgCedarMid)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = RpgCreamOnWood,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = RpgMutedOnWood,
                        fontSize = 11.sp,
                        fontFamily = SfProFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (headerExtra != null) {
                headerExtra()
                Spacer(Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RpgTerracotta)
                    .border(1.5.dp, RpgCedarDark, RoundedCornerShape(8.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tutup",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SfProFontFamily
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(RpgCedarDark)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            content = content
        )
    }
}

@Composable
fun RpgActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = RpgTerracotta,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 44.dp
) {
    val bg = if (enabled) containerColor else RpgLinen
    val fg = if (enabled) contentColor else RpgInkMuted
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.5.dp, if (enabled) RpgCedarDark else RpgCedarLight, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SfProFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RpgTabStrip(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) RpgCedarMid else RpgLinen)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) RpgCedarDark else RpgCedarLight,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) RpgCreamOnWood else RpgInkDark,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontFamily = SfProFontFamily,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RpgHeartMeter(
    hearts: Int,
    maxHearts: Int = 5,
    heartSize: Dp = 14.dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..maxHearts) {
            RpgPixelSprite(
                assetPath = if (i <= hearts) "rpg/ui/heart_full.png" else "rpg/ui/heart_empty.png",
                size = heartSize
            )
        }
    }
}

@Composable
fun RpgCoinBadge(coins: Int, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (compact) RpgHudCard else RpgCedarDark)
            .border(1.2.dp, RpgGoldTrim, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        RpgPixelSprite("rpg/ui/icon_coin.png", size = 14.dp)
        Text(
            text = "$coins",
            color = Color(0xFFFFE082),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = SfProFontFamily
        )
    }
}

fun formatRpgClock(timeHours: Float): String {
    val h = timeHours.toInt().coerceIn(0, 23)
    val m = ((timeHours - h) * 60f).toInt().coerceIn(0, 59)
    return String.format("%02d:%02d", h, m)
}
