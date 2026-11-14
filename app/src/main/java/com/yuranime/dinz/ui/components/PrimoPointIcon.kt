package com.yuranime.dinz.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuranime.dinz.R

/**
 * Komponen Reusable Ikon Poin Resmi Yuranime (Primogem)
 * Menggunakan aset resmi primo.png (ic_primo)
 */
@Composable
fun PrimoPointIcon(
    size: Dp = 18.dp,
    modifier: Modifier = Modifier,
    contentDescription: String = "Poin Primogem"
) {
    Image(
        painter = painterResource(id = R.drawable.ic_primo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
