package com.yuranime.dinz.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlin.math.roundToInt

/**
 * Model data langkah petunjuk pemula (Coachmark Step).
 * Menggunakan [id] target nyata yang diukur via Modifier.onboardingTarget,
 * BUKAN koordinat persentase tebakan!
 */
data class CoachmarkStep(
    val id: String,
    val title: String,
    val description: String,
    val cornerRadiusDp: Float = 14f,
    val scrollToIndex: Int? = null,
    // Fallback bounds jika elemen belum ter-attach (0f..1f)
    val fallbackTopRatio: Float = 0.2f
)

/**
 * Modifier extension untuk mendaftarkan posisi fisik komponen target secara real-time.
 */
fun Modifier.onboardingTarget(
    id: String,
    targetBounds: MutableMap<String, Rect>
): Modifier = this.onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        targetBounds[id] = coordinates.boundsInRoot()
    }
}

/**
 * Overlay panduan pemula interaktif (Spotlight Coachmark).
 * Menyorot komponen target dengan cutout transparan + border neon menyala (#EF4444 / #FF5722),
 * serta memposisikan kartu petunjuk secara dinamis di atas/bawah elemen target.
 */
@Composable
fun SpotlightCoachmarkOverlay(
    steps: List<CoachmarkStep>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    targetBounds: Map<String, Rect>,
    modifier: Modifier = Modifier,
    onStepChanged: ((CoachmarkStep) -> Unit)? = null
) {
    if (!isVisible || steps.isEmpty()) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(currentStepIndex) ?: return
    val density = LocalDensity.current

    LaunchedEffect(currentStepIndex, isVisible) {
        if (isVisible) {
            onStepChanged?.invoke(step)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Wajib untuk BlendMode.Clear pada hardware canvas
                .pointerInput(Unit) {
                    detectTapGestures { /* Intercept all touches behind overlay */ }
                }
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()

            // Dapatkan koordinat fisik asli dari targetBounds
            val measuredRect = targetBounds[step.id]
            val paddingPx = with(density) { 6.dp.toPx() }

            val targetRect = measuredRect?.let { r ->
                Rect(
                    left = (r.left - paddingPx).coerceAtLeast(0f),
                    top = (r.top - paddingPx).coerceAtLeast(0f),
                    right = (r.right + paddingPx).coerceAtMost(screenWidthPx),
                    bottom = (r.bottom + paddingPx).coerceAtMost(screenHeightPx)
                )
            } ?: Rect(
                left = with(density) { 16.dp.toPx() },
                top = screenHeightPx * step.fallbackTopRatio,
                right = screenWidthPx - with(density) { 16.dp.toPx() },
                bottom = screenHeightPx * step.fallbackTopRatio + with(density) { 100.dp.toPx() }
            )

            val cornerRadiusPx = with(density) { step.cornerRadiusDp.dp.toPx() }

            // 1. Gambar Spotlight Cutout & Glowing Border
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Overlay gelap semi-transparan (#CC000000)
                drawRect(color = Color(0xCC000000))

                // Cutout transparan untuk elemen target
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    blendMode = BlendMode.Clear
                )

                // Glowing outer stroke halus (#FF5722)
                drawRoundRect(
                    color = Color(0xFFFF5722).copy(alpha = 0.35f),
                    topLeft = Offset(targetRect.left - with(density) { 3.dp.toPx() }, targetRect.top - with(density) { 3.dp.toPx() }),
                    size = Size(targetRect.width + with(density) { 6.dp.toPx() }, targetRect.height + with(density) { 6.dp.toPx() }),
                    cornerRadius = CornerRadius(cornerRadiusPx + with(density) { 2.dp.toPx() }, cornerRadiusPx + with(density) { 2.dp.toPx() }),
                    style = Stroke(width = with(density) { 3.dp.toPx() })
                )

                // Crisp primary neon border (#EF4444 tebal 2.dp)
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = Size(targetRect.width, targetRect.height),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
            }

            // 2. Posisi Kartu Petunjuk (Tooltip Card) Dinamis di Atas atau di Bawah
            val isTargetInBottomHalf = targetRect.center.y > (screenHeightPx / 2f)

            var cardHeightPx by remember { mutableIntStateOf(0) }

            val cardOffsetY = if (isTargetInBottomHalf) {
                // Elemen di bawah -> Kartu diletakkan tepat DI ATAS elemen
                val candidateY = targetRect.top - cardHeightPx - with(density) { 16.dp.toPx() }
                candidateY.coerceAtLeast(with(density) { 40.dp.toPx() })
            } else {
                // Elemen di atas -> Kartu diletakkan tepat DI BAWAH elemen
                val candidateY = targetRect.bottom + with(density) { 16.dp.toPx() }
                val maxAllowedY = screenHeightPx - cardHeightPx - with(density) { 40.dp.toPx() }
                candidateY.coerceAtMost(maxAllowedY.coerceAtLeast(0f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, cardOffsetY.roundToInt()) }
                    .padding(horizontal = 20.dp)
                    .onGloballyPositioned { coords ->
                        cardHeightPx = coords.size.height
                    }
            ) {
                // Kartu Petunjuk (Coachmark Card)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16181D))
                        .border(1.dp, Color(0xFF2A2E37), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    // Judul Panduan
                    Text(
                        text = step.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Deskripsi Penjelasan Ramah Pemula
                    Text(
                        text = step.description,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = SfProFontFamily,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Baris Tombol Aksi & Indikator Titik
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tombol Skip (Kiri)
                        Text(
                            text = "Skip",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        )

                        // Indikator Progres Dots (Tengah)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            steps.indices.forEach { index ->
                                if (index == currentStepIndex) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 20.dp, height = 6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4B5563))
                                    )
                                }
                            }
                        }

                        // Tombol Aksi (Kanan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Tombol Previous (jika bukan langkah pertama)
                            if (currentStepIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF22252D))
                                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
                                        .clickable { currentStepIndex-- }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Previous",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            // Tombol Next / Selesai
                            val isLast = currentStepIndex == steps.size - 1
                            Box(
                                modifier = Modifier
                                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x66EF4444))
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFEF4444))
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFFF8A80), Color(0xFFEF4444))
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        if (isLast) {
                                            onDismiss()
                                        } else {
                                            currentStepIndex++
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isLast) "Selesai" else "Next",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SfProFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
