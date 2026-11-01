package com.dinz.animein.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.R
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.ui.theme.ColorAccentPrimary

@Composable
fun CuplixAvatarItem(
    item: CuplixItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val squircleShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .size(62.dp)
            .clip(squircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(squircleShape)
                .border(width = 1.dp, color = Color(0x33FF5722), shape = squircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.getEffectiveThumbnail())
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder_yuranime)
                    .error(R.drawable.placeholder_yuranime)
                    .build(),
                contentDescription = item.anime ?: item.caption ?: "Cuplix",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(squircleShape)
            )
        }
    }
}

