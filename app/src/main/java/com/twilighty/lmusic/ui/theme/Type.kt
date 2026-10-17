package com.twilighty.lmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.twilighty.lmusic.R


private val montserrat = GoogleFont("Montserrat")
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

val MontserratFamily = FontFamily(
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Black),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
)

val ExpTitleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        //textGeometricTransform = TextGeometricTransform(scaleX = 1f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)

// LMusic (Yuyu design system) type: Figtree for UI text, Bricolage Grotesque for
// headings. The app references `GoogleSansRounded` in hundreds of places, so that
// name is kept and now maps light-to-semibold weights to Figtree and bold-and-up
// weights to Bricolage Grotesque — every bold title picks up the Yuyu display face.
@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = androidx.compose.ui.text.font.Font(
    resId = R.font.figtree_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: FontWeight, opticalSize: Float) = androidx.compose.ui.text.font.Font(
    resId = R.font.bricolage_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(
        // Bricolage Grotesque's weight axis tops out at 800.
        FontVariation.weight(weight.weight.coerceAtMost(800)),
        FontVariation.Setting("opsz", opticalSize)
    )
)

/** Figtree only — Yuyu body/UI text. */
val YuyuBody = FontFamily(
    figtree(FontWeight.Light),
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)

/** Bricolage Grotesque only — Yuyu display/headings. */
val YuyuDisplay = FontFamily(
    bricolage(FontWeight.Normal, 24f),
    bricolage(FontWeight.Medium, 24f),
    bricolage(FontWeight.SemiBold, 24f),
    bricolage(FontWeight.Bold, 36f),
    bricolage(FontWeight.ExtraBold, 48f),
    bricolage(FontWeight.Black, 48f),
)

val GoogleSansRounded = FontFamily(
    figtree(FontWeight.Light),
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    bricolage(FontWeight.Bold, 24f),
    bricolage(FontWeight.ExtraBold, 36f),
    bricolage(FontWeight.Black, 48f),
)

// Yuyu type scale: Bricolage Grotesque headings (800, tight tracking), Figtree text.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.03).em
    ),
    displayMedium = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).em
    ),
    displaySmall = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.02).em
    ),
    headlineLarge = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.01).em
    ),
    headlineSmall = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = YuyuDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = YuyuBody,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
