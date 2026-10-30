package com.dinz.animein.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dinz.animein.data.model.CuplixItem
import com.dinz.animein.ui.theme.ColorAccentPrimary

@Composable
fun CuplixAvatarItem(
    item: CuplixItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .size(68.dp) // Total container allowing full touch target
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(width = 1.5.dp, color = ColorAccentPrimary, shape = CircleShape)
                .padding(2.5.dp) // Ring padding
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.getEffectiveThumbnail())
                    .crossfade(true)
                    .build(),
                contentDescription = item.anime ?: item.caption ?: "Cuplix",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }
    }
}
