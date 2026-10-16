package com.theveloper.pixelplay.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.theveloper.pixelplay.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.unit.dp

val LocalPixelPlayDarkTheme = staticCompositionLocalOf { false }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PixelPlayStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val colorArgb = color.toArgb()
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        window.statusBarColor = colorArgb
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
    }
}

val DarkColorScheme = darkColorScheme(
    primary = PixelPlayPurplePrimary,
    secondary = PixelPlayPink,
    tertiary = PixelPlayOrange,
    background = PixelPlayPurpleDark,
    surface = PixelPlaySurface,
    onPrimary = PixelPlayWhite,
    onSecondary = PixelPlayWhite,
    onTertiary = PixelPlayWhite,
    onBackground = PixelPlayWhite,
    onSurface = PixelPlayLightPurple, // Texto sobre superficies
    error = Color(0xFFFF5252),
    onError = PixelPlayWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = PixelPlayWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = PixelPlayPink,
    onSecondary = PixelPlayWhite,
    secondaryContainer = PixelPlayPink.copy(alpha = 0.15f),
    onSecondaryContainer = PixelPlayPink.copy(alpha = 0.85f),
    tertiary = PixelPlayOrange,
    onTertiary = PixelPlayBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = PixelPlayWhite
)

/** Yuyu light scheme — ink primary, paper surfaces, Ember accent as tertiary. */
val YuyuLightColorScheme = lightColorScheme(
    primary = YuyuColors.Text,
    onPrimary = YuyuColors.BgCanvas,
    primaryContainer = YuyuColors.BgEmphasis,
    onPrimaryContainer = YuyuColors.Text,
    inversePrimary = YuyuColors.DarkText,
    secondary = YuyuColors.TextSecondary,
    onSecondary = YuyuColors.BgCanvas,
    secondaryContainer = YuyuColors.BgMuted,
    onSecondaryContainer = YuyuColors.Text,
    tertiary = YuyuColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = YuyuColors.AccentSoft,
    onTertiaryContainer = YuyuColors.AccentText,
    background = YuyuColors.BgCanvas,
    onBackground = YuyuColors.Text,
    surface = YuyuColors.BgCanvas,
    onSurface = YuyuColors.Text,
    surfaceVariant = YuyuColors.BgMuted,
    onSurfaceVariant = YuyuColors.TextMuted,
    surfaceTint = YuyuColors.Text,
    inverseSurface = YuyuColors.Text,
    inverseOnSurface = YuyuColors.DarkText,
    error = YuyuColors.DangerFg,
    onError = Color.White,
    errorContainer = YuyuColors.DangerBg,
    onErrorContainer = YuyuColors.DangerFg,
    outline = YuyuColors.BorderStrong,
    outlineVariant = YuyuColors.Border,
    scrim = Color.Black,
    surfaceBright = YuyuColors.BgCanvas,
    surfaceDim = YuyuColors.BgMuted,
    surfaceContainerLowest = YuyuColors.BgCanvas,
    surfaceContainerLow = YuyuColors.BgSubtle,
    surfaceContainer = YuyuColors.BgMuted,
    surfaceContainerHigh = YuyuColors.BgEmphasis,
    surfaceContainerHighest = YuyuColors.Border,
)

/** Yuyu dark scheme — inverted ink, near-black surfaces, lightened Ember for text. */
val YuyuDarkColorScheme = darkColorScheme(
    primary = YuyuColors.DarkText,
    onPrimary = YuyuColors.Text,
    primaryContainer = YuyuColors.DarkBgEmphasis,
    onPrimaryContainer = YuyuColors.DarkText,
    inversePrimary = YuyuColors.Text,
    secondary = YuyuColors.DarkTextSecondary,
    onSecondary = YuyuColors.Text,
    secondaryContainer = YuyuColors.DarkBgMuted,
    onSecondaryContainer = YuyuColors.DarkText,
    tertiary = YuyuColors.DarkAccentText,
    onTertiary = YuyuColors.Text,
    tertiaryContainer = YuyuColors.DarkAccentSoft,
    onTertiaryContainer = YuyuColors.DarkAccentText,
    background = YuyuColors.DarkBgCanvas,
    onBackground = YuyuColors.DarkText,
    surface = YuyuColors.DarkBgCanvas,
    onSurface = YuyuColors.DarkText,
    surfaceVariant = YuyuColors.DarkBgMuted,
    onSurfaceVariant = YuyuColors.DarkTextMuted,
    surfaceTint = YuyuColors.DarkText,
    inverseSurface = YuyuColors.DarkText,
    inverseOnSurface = YuyuColors.Text,
    error = YuyuColors.DarkDangerFg,
    onError = YuyuColors.Text,
    errorContainer = YuyuColors.DarkDangerBg,
    onErrorContainer = YuyuColors.DarkDangerFg,
    outline = YuyuColors.DarkBorderStrong,
    outlineVariant = YuyuColors.DarkBgEmphasis,
    scrim = Color.Black,
    surfaceBright = YuyuColors.DarkBgEmphasis,
    surfaceDim = YuyuColors.DarkBgCanvas,
    surfaceContainerLowest = Color(0xFF0C0C0E),
    surfaceContainerLow = YuyuColors.DarkBgSubtle,
    surfaceContainer = YuyuColors.DarkBgInverse,
    surfaceContainerHigh = YuyuColors.DarkBgMuted,
    surfaceContainerHighest = YuyuColors.DarkBgEmphasis,
)

@Composable
fun PixelPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    useYuyuColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val finalColorScheme = when {
        // LMusic default: the Yuyu palette app-wide. The now-playing screen still takes its
        // colors from the album art through colorSchemePairOverride.
        colorSchemePairOverride == null && useYuyuColors -> {
            if (darkTheme) YuyuDarkColorScheme else YuyuLightColorScheme
        }
        colorSchemePairOverride == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Tema dinámico del sistema como prioridad si no hay override
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                // Fallback a los defaults si dynamic colors falla (raro, pero posible en algunos dispositivos)
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        colorSchemePairOverride != null -> {
            // Usar el esquema del álbum si se proporciona
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        // Fallback final a los defaults si no hay override ni dynamic colors aplicables
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val statusBarElevation = if (darkTheme) 4.dp else 12.dp
    val elevatedSurface = finalColorScheme.surfaceColorAtElevation(statusBarElevation)
    val defaultStatusBarColor = Color(
        ColorUtils.blendARGB(
            finalColorScheme.background.toArgb(),
            elevatedSurface.toArgb(),
            0.35f
        )
    )

    PixelPlayStatusBarStyle(color = defaultStatusBarColor)

    CompositionLocalProvider(LocalPixelPlayDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
