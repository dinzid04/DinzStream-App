package com.yuranime.dinz.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.data.local.AppPreferences
import com.yuranime.dinz.ui.theme.SfProFontFamily
import org.intellij.lang.annotations.Language

/**
 * Tab navigasi utama aplikasi Yuranime.
 * Mendukung ikon solid (aktif) dan outline (tidak aktif)
 * dengan properti label dan backward compatibility untuk [title] dan [icon].
 */
enum class NavTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    JADWAL("Jadwal", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    CARI("Cari", Icons.Filled.Search, Icons.Outlined.Search),
    CHAT("Chat", Icons.AutoMirrored.Filled.Send, Icons.AutoMirrored.Outlined.Send),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person);

    val title: String get() = label
    val icon: ImageVector get() = filledIcon
}

/**
 * Modifier untuk merekam konten halaman ke dalam [GraphicsLayer] agar piksel
 * yang berada tepat di bawah [BottomNavBar] dapat dibiaskan secara real-time
 * oleh lensa Clear Liquid Glass.
 */
fun Modifier.liquidGlassBackdrop(layer: GraphicsLayer, enabled: Boolean = true): Modifier = if (enabled) {
    this.drawWithContent {
        layer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(layer)
    }
} else {
    this
}

/**
 * AGSL Shader fisika optik Clear Liquid Glass (diadaptasi dari amarnath3003/quickLiquid):
 * - Clear Center: bagian tengah kapsul bening transparan sehingga konten di bawahnya terlihat jelas.
 * - Refractive Edge (Lensing): tepi lengkungan kapsul membelokkan/membiaskan piksel latar
 *   dengan kurva busur cembung (Snell refraction) + chromatic dispersion (R/G/B).
 * - Fresnel Specular Rim: pantulan cahaya kaca putih tajam di sepanjang bibir kapsul.
 */
@Language("AGSL")
private const val QUICK_LIQUID_AGSL_SHADER = """
    uniform shader content;
    uniform float2 resolution;

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 fragCoord) {
        float2 center = resolution * 0.5;
        float radius = min(center.x, center.y);
        float2 p = fragCoord - center;

        // Signed distance dari tepi kapsul (negatif di dalam kapsul)
        float dist = sdRoundedBox(p, center, radius);
        float d = max(-dist, 0.0);

        // Lebar bezel pembiasan tepi kaca (22px)
        float bezel = min(22.0, radius * 0.68);
        float s = clamp(d / bezel, 0.0, 1.0);

        // Profil lengkungan kaca cembung: 0 di bagian tengah (clear center), kuat di tepi
        float edgeCurve = pow(1.0 - s, 2.15);

        // Vektor normal permukaan menuju pusat kapsul
        float2 normal = (length(p) > 0.001) ? normalize(p) : float2(0.0, -1.0);

        // Kekuatan pembiasan (lensing magnification + edge bending)
        float shiftStrength = edgeCurve * 18.0;
        float2 delta = -normal * shiftStrength;

        // Chromatic dispersion halus pada kanal R, G, B
        float2 uvR = clamp(fragCoord + delta * 0.88, float2(1.0), resolution - 1.0);
        float2 uvG = clamp(fragCoord + delta * 1.00, float2(1.0), resolution - 1.0);
        float2 uvB = clamp(fragCoord + delta * 1.14, float2(1.0), resolution - 1.0);

        half4 colR = content.eval(uvR);
        half4 colG = content.eval(uvG);
        half4 colB = content.eval(uvB);
        half3 refracted = half3(colR.r, colG.g, colB.b);

        // Lapisan kaca bening (clear glass) dengan sedikit penyeimbang kontras agar ikon putih tajam
        half3 glassTint = mix(refracted * 0.82, half3(0.09, 0.11, 0.15), 0.22);

        // Fresnel specular highlight pada bibir kapsul (atas-kiri & bawah-kanan)
        float lightDir = clamp((-normal.y * 0.72 - normal.x * 0.38), 0.0, 1.0);
        float oppLight = clamp((normal.y * 0.55 + normal.x * 0.35), 0.0, 1.0);
        float fresnelRim = pow(1.0 - s, 4.2) * (0.42 * lightDir + 0.22 * oppLight);
        float innerCaustic = pow(1.0 - s, 2.4) * 0.12;

        half3 finalRgb = clamp(glassTint + half3(fresnelRim + innerCaustic), half3(0.0), half3(1.0));
        return half4(finalRgb, 1.0);
    }
"""

/**
 * Floating Bottom Navigation Bar dengan gaya optik "Clear Liquid Glass" (quickLiquid):
 * - Mengambang murni 100% di atas konten halaman tanpa blok hitam di bawahnya.
 * - Clear Center & Refractive Edge: tengah kapsul bening transparan, tepinya membiaskan konten di bawahnya.
 * - Tab Aktif: Lensa kaca bening putih (tanpa warna coklat/merah keruh) + Ikon Putih Solid + Teks Judul Putih SF Bold.
 * - Tab Tidak Aktif: Hanya Ikon Outline tanpa teks.
 */
@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    backdropLayer: GraphicsLayer? = null,
    isLiquidGlassEnabled: Boolean = AppPreferences.liquidGlassEnabled.collectAsState().value
) {
    var pillOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var pillSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        val outerPillModifier = if (isLiquidGlassEnabled) {
            Modifier
                .onGloballyPositioned { coords ->
                    pillOffsetInRoot = coords.positionInRoot()
                    pillSize = coords.size
                }
                .shadow(16.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color(0x3310131A))
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val r = size.height / 2f

                    // Lapisan kilau dalam kaca (Clear Center + Subtle Refractive Edge Highlight)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x38FFFFFF),
                                Color(0x0AFFFFFF),
                                Color(0x05FFFFFF),
                                Color(0x24FFFFFF)
                            )
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )

                    // Chromatic dispersion tipis di bagian dalam tepi lensa
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x337DD3FC), // Cyan refraction edge
                                Color(0x08FFFFFF),
                                Color(0x26F472B6)  // Warm spectrum edge
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        topLeft = Offset(strokeWidth * 1.5f, strokeWidth * 1.5f),
                        size = Size(size.width - strokeWidth * 3f, size.height - strokeWidth * 3f),
                        cornerRadius = CornerRadius(r - strokeWidth, r - strokeWidth),
                        style = Stroke(width = strokeWidth * 1.4f)
                    )

                    // Fresnel Specular Rim putih tajam (1.dp) di bibir kapsul
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xB3FFFFFF),
                                Color(0x33FFFFFF),
                                Color(0x1AFFFFFF),
                                Color(0x66FFFFFF)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width * 0.85f, size.height)
                        ),
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = strokeWidth)
                    )
                }
        } else {
            Modifier
                .shadow(12.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color(0xD912141A))
                .border(
                    width = 1.dp,
                    color = Color(0x33FFFFFF),
                    shape = CircleShape
                )
        }

        Box(modifier = outerPillModifier) {
            // Live Refracted Backdrop Layer (Android 13+ AGSL RuntimeShader / Clear Glass Backdrop)
            if (isLiquidGlassEnabled && backdropLayer != null && pillSize.width > 0 && pillSize.height > 0) {
                val agslRenderEffect = remember(pillSize) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            val shader = RuntimeShader(QUICK_LIQUID_AGSL_SHADER).apply {
                                setFloatUniform(
                                    "resolution",
                                    pillSize.width.toFloat(),
                                    pillSize.height.toFloat()
                                )
                            }
                            RenderEffect.createRuntimeShaderEffect(shader, "content")
                                .asComposeRenderEffect()
                        } catch (_: Throwable) {
                            null
                        }
                    } else {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            if (agslRenderEffect != null) {
                                renderEffect = agslRenderEffect
                            }
                            alpha = 0.96f
                        }
                        .drawBehind {
                            translate(
                                left = -pillOffsetInRoot.x,
                                top = -pillOffsetInRoot.y
                            ) {
                                drawLayer(backdropLayer)
                            }
                        }
                        // Rim Fresnel di atas hasil bias AGSL
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xB3FFFFFF),
                                    Color(0x2EFFFFFF),
                                    Color(0x1AFFFFFF),
                                    Color(0x66FFFFFF)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(400f, 140f)
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Baris Item Navigasi di Dalam Kapsul Kaca
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember { MutableInteractionSource() }

                    if (isSelected) {
                        val activeTabModifier = Modifier
                            .height(44.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .then(
                                if (isLiquidGlassEnabled) {
                                    Modifier
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x38FFFFFF),
                                                    Color(0x18FFFFFF),
                                                    Color(0x28FFFFFF)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xA6FFFFFF),
                                                    Color(0x33FFFFFF),
                                                    Color(0x66FFFFFF)
                                                ),
                                                start = Offset(0f, 0f),
                                                end = Offset(160f, 90f)
                                            ),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                } else {
                                    Modifier
                                        .background(Color(0x26FFFFFF))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true, color = Color.White)
                            ) { onTabSelected(tab) }
                            .padding(horizontal = 15.dp, vertical = 9.dp)

                        Box(
                            modifier = activeTabModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.filledIcon,
                                    contentDescription = tab.label,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.label,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SfProFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB TIDAK AKTIF: Hanya Ikon Outline Saja (Tanpa Teks Judul)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = false, radius = 22.dp, color = Color.White)
                                ) { onTabSelected(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.outlinedIcon,
                                contentDescription = tab.label,
                                tint = Color(0xD9FFFFFF),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
