package com.yuranime.dinz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
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

// Font family resmi WibiShop / YuraStyle untuk paritas 100% tampilan Style Nama
val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

@OptIn(ExperimentalTextApi::class)
val TourneyItalicFontFamily = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.width(100f)
            )
        ),
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.Medium,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500),
                FontVariation.width(100f)
            )
        ),
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(600),
                FontVariation.width(100f)
            )
        ),
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.Bold,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.width(100f)
            )
        ),
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800),
                FontVariation.width(100f)
            )
        ),
        Font(
            resId = R.font.tourney_italic_variable,
            weight = FontWeight.Black,
            style = FontStyle.Italic,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(900),
                FontVariation.width(100f)
            )
        )
    )
} else {
    FontFamily(
        Font(R.font.tourney_italic_variable, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.tourney_italic_variable, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.tourney_italic_variable, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.tourney_italic_variable, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.tourney_italic_variable, FontWeight.ExtraBold, FontStyle.Italic),
        Font(R.font.tourney_italic_variable, FontWeight.Black, FontStyle.Italic)
    )
}

val PiedraFontFamily = FontFamily(
    Font(R.font.piedra_regular, FontWeight.Normal),
    Font(R.font.piedra_regular, FontWeight.Medium),
    Font(R.font.piedra_regular, FontWeight.SemiBold),
    Font(R.font.piedra_regular, FontWeight.Bold),
    Font(R.font.piedra_regular, FontWeight.ExtraBold),
    Font(R.font.piedra_regular, FontWeight.Black)
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
