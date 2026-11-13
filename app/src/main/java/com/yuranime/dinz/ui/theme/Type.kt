package com.yuranime.dinz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R

// Display & Header: Outfit (SemiBold & Bold, tracking -0.02em)
val OutfitDisplayFontFamily = FontFamily(
    Font(R.font.outfit_semibold, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_semibold, FontWeight.Bold),
    Font(R.font.outfit_semibold, FontWeight.ExtraBold)
)

// Body & UI Labels: Plus Jakarta Sans (Regular, Medium, SemiBold, Bold)
val JakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Bold)
)

// Angka & Metrik Tabular: JetBrains Mono (Medium, SemiBold, Bold)
val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_semibold, FontWeight.Normal),
    Font(R.font.jetbrains_mono_semibold, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_semibold, FontWeight.Bold)
)

// SF Pro Display (Dipertahankan untuk kompatibilitas komponen eksisting)
val SfProFontFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold)
)

// Calistoga Regular untuk wordmark Yuranime Splash Screen
val YuranimeLogoFontFamily = FontFamily(
    Font(R.font.yuranime_logo_font, FontWeight.Normal)
)

val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = OutfitDisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
        color = ColorTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.3).sp,
        color = ColorTextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = JakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = ColorTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = JakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = ColorTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = JakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = ColorTextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum",
        color = ColorTextSecondary
    )
)
