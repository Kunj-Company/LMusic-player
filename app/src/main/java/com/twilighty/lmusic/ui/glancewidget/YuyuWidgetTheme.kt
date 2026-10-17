package com.twilighty.lmusic.ui.glancewidget

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders
import com.twilighty.lmusic.ui.theme.YuyuColors

/**
 * Yuyu colours for the home-screen widgets. Flat canvas (white / #111113), ink text,
 * muted secondary text, and the Ember accent for the play button. Roles are mapped onto
 * Material names so every widget layout picks them up through [androidx.glance.GlanceTheme].
 */
object YuyuWidgetPalette {
    val Ember = YuyuColors.Accent
    val OnEmber = Color.White

    val LightCanvas = YuyuColors.BgCanvas
    val LightMuted = YuyuColors.BgMuted
    val LightText = YuyuColors.Text
    val LightTextMuted = YuyuColors.TextMuted
    val LightBorder = YuyuColors.Border

    val DarkCanvas = YuyuColors.DarkBgCanvas
    val DarkMuted = YuyuColors.DarkBgMuted
    val DarkText = YuyuColors.DarkText
    val DarkTextMuted = YuyuColors.DarkTextMuted
    val DarkBorder = YuyuColors.DarkBorderStrong
}

val YuyuGlanceColors = ColorProviders(
    light = lightColorScheme(
        primary = YuyuWidgetPalette.Ember,
        onPrimary = YuyuWidgetPalette.OnEmber,
        primaryContainer = YuyuWidgetPalette.Ember,
        onPrimaryContainer = YuyuWidgetPalette.OnEmber,
        secondary = YuyuWidgetPalette.LightText,
        onSecondary = YuyuWidgetPalette.LightCanvas,
        secondaryContainer = YuyuWidgetPalette.LightMuted,
        onSecondaryContainer = YuyuWidgetPalette.LightText,
        tertiary = YuyuWidgetPalette.Ember,
        onTertiary = YuyuWidgetPalette.OnEmber,
        tertiaryContainer = YuyuColors.AccentSoft,
        onTertiaryContainer = YuyuColors.AccentText,
        background = YuyuWidgetPalette.LightCanvas,
        onBackground = YuyuWidgetPalette.LightText,
        surface = YuyuWidgetPalette.LightCanvas,
        onSurface = YuyuWidgetPalette.LightText,
        surfaceVariant = YuyuWidgetPalette.LightMuted,
        onSurfaceVariant = YuyuWidgetPalette.LightTextMuted,
        outline = YuyuWidgetPalette.LightBorder,
    ),
    dark = darkColorScheme(
        primary = YuyuWidgetPalette.Ember,
        onPrimary = YuyuWidgetPalette.OnEmber,
        primaryContainer = YuyuWidgetPalette.Ember,
        onPrimaryContainer = YuyuWidgetPalette.OnEmber,
        secondary = YuyuWidgetPalette.DarkText,
        onSecondary = YuyuWidgetPalette.DarkCanvas,
        secondaryContainer = YuyuWidgetPalette.DarkMuted,
        onSecondaryContainer = YuyuWidgetPalette.DarkText,
        tertiary = YuyuWidgetPalette.Ember,
        onTertiary = YuyuWidgetPalette.OnEmber,
        tertiaryContainer = YuyuColors.DarkAccentSoft,
        onTertiaryContainer = YuyuColors.DarkAccentText,
        background = YuyuWidgetPalette.DarkCanvas,
        onBackground = YuyuWidgetPalette.DarkText,
        surface = YuyuWidgetPalette.DarkCanvas,
        onSurface = YuyuWidgetPalette.DarkText,
        surfaceVariant = YuyuWidgetPalette.DarkMuted,
        onSurfaceVariant = YuyuWidgetPalette.DarkTextMuted,
        outline = YuyuWidgetPalette.DarkBorder,
    ),
)
