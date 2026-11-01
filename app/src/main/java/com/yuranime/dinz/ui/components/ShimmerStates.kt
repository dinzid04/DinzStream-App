package com.yuranime.dinz.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.ui.theme.ColorAccentPrimary
import com.yuranime.dinz.ui.theme.ColorBackgroundPrimary
import com.yuranime.dinz.ui.theme.ColorShimmerBase
import com.yuranime.dinz.ui.theme.ColorShimmerHighlight
import com.yuranime.dinz.ui.theme.ColorTextPrimary
import com.yuranime.dinz.ui.theme.ColorTextSecondary

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerAnim"
    )

    return Brush.linearGradient(
        colors = listOf(
            ColorShimmerBase,
            ColorShimmerHighlight,
            ColorShimmerBase
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun HomeShimmerLoading(
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
            .padding(vertical = 12.dp)
    ) {
        // Top Banner Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 8f)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cuplix Avatar Skeleton Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section Title Skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(130.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2:3 Vertical Card Row Skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                Column(modifier = Modifier.width(115.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorBackgroundPrimary)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Gagal Memuat Konten",
            color = ColorTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage.ifBlank { "Terjadi gangguan saat menghubungkan ke server. Silakan coba lagi." },
            color = ColorTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .height(48.dp) // Touch target >= 48dp (R-03 / R-39)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Coba Lagi",
                color = ColorTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
