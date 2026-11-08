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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.SfProFontFamily
import kotlin.math.roundToInt

data class CoachmarkStep(
    val title: String,
    val description: String,
    // Relative target bounds on screen (fractions 0f..1f: left, top, width, height)
    val relativeLeft: Float = 0.05f,
    val relativeTop: Float = 0.1f,
    val relativeWidth: Float = 0.9f,
    val relativeHeight: Float = 0.2f,
    val cornerRadiusDp: Float = 14f,
    val isBottomNavTarget: Boolean = false
)

@Composable
fun SpotlightCoachmarkOverlay(
    steps: List<CoachmarkStep>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible || steps.isEmpty()) return

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(currentStepIndex) ?: return
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear
                .pointerInput(Unit) {
                    detectTapGestures { /* Intercept all touches behind the overlay */ }
                }
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()

            // Calculate target rect in pixels
            val targetLeftPx = step.relativeLeft * screenWidthPx
            val targetTopPx = step.relativeTop * screenHeightPx
            val targetWidthPx = step.relativeWidth * screenWidthPx
            val targetHeightPx = step.relativeHeight * screenHeightPx
            val cornerRadiusPx = with(density) { step.cornerRadiusDp.dp.toPx() }

            // 1. Canvas drawing Spotlight Cutout with glowing red/orange border
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Semi-transparent dark background (Color(0xCC000000))
                drawRect(color = Color(0xCC000000))

                // Cutout with BlendMode.Clear
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetLeftPx, targetTopPx),
                    size = Size(targetWidthPx, targetHeightPx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    blendMode = BlendMode.Clear
                )

                // Glowing outer stroke (#FF5722 with 0.35 alpha)
                drawRoundRect(
                    color = Color(0xFFFF5722).copy(alpha = 0.35f),
                    topLeft = Offset(targetLeftPx - with(density) { 3.dp.toPx() }, targetTopPx - with(density) { 3.dp.toPx() }),
                    size = Size(targetWidthPx + with(density) { 6.dp.toPx() }, targetHeightPx + with(density) { 6.dp.toPx() }),
                    cornerRadius = CornerRadius(cornerRadiusPx + with(density) { 2.dp.toPx() }, cornerRadiusPx + with(density) { 2.dp.toPx() }),
                    style = Stroke(width = with(density) { 3.dp.toPx() })
                )

                // Crisp primary neon border (#EF4444, 2.dp)
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(targetLeftPx, targetTopPx),
                    size = Size(targetWidthPx, targetHeightPx),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
            }

            // 2. Position the Coachmark Card dynamically above or below the target
            val isTargetInBottomHalf = (step.relativeTop + step.relativeHeight / 2f) > 0.5f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = if (isTargetInBottomHalf) Alignment.TopCenter else Alignment.BottomCenter
            ) {
                // Coachmark Tooltip Card (exact reference matching)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16181D))
                        .border(1.dp, Color(0xFF2A2E37), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Title
                    Text(
                        text = step.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SfProFontFamily
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = step.description,
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = SfProFontFamily,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progress Dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { idx, _ ->
                            if (idx == currentStepIndex) {
                                // Active capsule
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            } else {
                                // Inactive small dot
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4B5563))
                                )
                            }
                            if (idx < steps.size - 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Button Row: Skip on Left, Previous & Next/Selesai on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Text Button
                        Text(
                            text = "Skip",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SfProFontFamily,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Button (shown if step > 0)
                            if (currentStepIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF22252D))
                                        .border(1.dp, Color(0xFF374151), RoundedCornerShape(20.dp))
                                        .clickable {
                                            currentStepIndex--
                                        }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Previous",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = SfProFontFamily
                                    )
                                }
                            }

                            // Next / Selesai Button
                            val isLastStep = currentStepIndex == steps.size - 1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFEF4444), Color(0xFFFF5722))
                                        )
                                    )
                                    .border(1.dp, Color(0xFFFF8A65), RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (isLastStep) {
                                            onDismiss()
                                        } else {
                                            currentStepIndex++
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isLastStep) "Selesai" else "Next",
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
