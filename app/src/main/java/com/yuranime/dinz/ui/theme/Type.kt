package com.yuranime.dinz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yuranime.dinz.R

// SF Pro Display (Font Utama Premium: Bold, SemiBold, Medium, Regular - Bersih, Solid, Bebas Angka Nol Bergaris)
val SfProFontFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
    Font(R.font.sf_pro_display_bold, FontWeight.ExtraBold)
)

// Alias terpadu ke SF Pro Display agar seluruh komponen otomatis menggunakan font tegas sans-serif tanpa angka nol bergaris
val OutfitDisplayFontFamily = SfProFontFamily
val JakartaSansFontFamily = SfProFontFamily
val JetBrainsMonoFontFamily = SfProFontFamily

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
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = ColorTextSecondary
    )
)
