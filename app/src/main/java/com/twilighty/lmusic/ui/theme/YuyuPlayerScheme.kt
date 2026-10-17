package com.twilighty.lmusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** True while the app uses the Yuyu palette (Settings › Appearance › App colours). */
val LocalUseYuyuColors = staticCompositionLocalOf { true }

/** The Ember accent Yuyu uses when a song has no artwork to take a colour from. */
val YuyuDefaultAccent = Color(0xFFC2410C)

/**
 * The colours of one "album accent" in Yuyu terms (Yuyu's event accent, here driven by
 * the current song's artwork):
 *  - [fill]: solid fills — play button, seek bar, progress, chips' active state
 *  - [onFill]: text and icons on [fill]
 *  - [text]: the accent used as text on the canvas (readable in light and dark)
 *  - [soft]: the accent mixed into the canvas — the player background and soft chips
 */
data class YuyuAccent(
    val fill: Color,
    val onFill: Color,
    val text: Color,
    val soft: Color,
    val softStrong: Color,
)

fun yuyuAccentFrom(seed: Color, isDark: Boolean): YuyuAccent {
    val canvas = if (isDark) YuyuColors.DarkBgCanvas else YuyuColors.BgCanvas
    // Keep fills deep enough for white text; keep accent-as-text readable on the canvas.
    val fill = when {
        seed.luminance() > 0.32f -> lerp(seed, Color.Black, 0.45f)
        seed.luminance() < 0.02f && isDark -> lerp(seed, Color.White, 0.18f)
        else -> seed
    }
    val onFill = if (fill.luminance() > 0.4f) YuyuColors.Text else Color.White
    val text = if (isDark) lerp(fill, Color.White, 0.42f) else fill
    val soft = lerp(canvas, fill, if (isDark) 0.18f else 0.09f)
    val softStrong = lerp(canvas, fill, if (isDark) 0.30f else 0.18f)
    return YuyuAccent(fill = fill, onFill = onFill, text = text, soft = soft, softStrong = softStrong)
}

/**
 * Turns the Material scheme generated from the album art into a Yuyu scheme: Yuyu's
 * neutrals and ink text everywhere, with the artwork colour used only as the accent.
 * [albumScheme] equal to [base] means "no artwork" → the Ember default accent.
 */
fun yuyuPlayerScheme(albumScheme: ColorScheme, base: ColorScheme, isDark: Boolean): ColorScheme {
    val seed = if (albumScheme == base) {
        YuyuDefaultAccent
    } else if (isDark) {
        albumScheme.primaryContainer
    } else {
        albumScheme.primary
    }
    val accent = yuyuAccentFrom(seed, isDark)
    val ink = base.onSurface
    return base.copy(
        primary = accent.fill,
        onPrimary = accent.onFill,
        primaryContainer = accent.soft,
        onPrimaryContainer = ink,
        inversePrimary = accent.text,
        secondary = base.onSurfaceVariant,
        onSecondary = base.surface,
        secondaryContainer = accent.softStrong,
        onSecondaryContainer = ink,
        tertiary = accent.text,
        onTertiary = accent.onFill,
        tertiaryContainer = accent.softStrong,
        onTertiaryContainer = ink,
        surfaceTint = accent.fill,
        // Fixed roles drive the full player's transport buttons and toggles.
        primaryFixed = accent.fill,
        primaryFixedDim = accent.fill,
        onPrimaryFixed = accent.onFill,
        onPrimaryFixedVariant = accent.onFill,
        secondaryFixed = accent.softStrong,
        secondaryFixedDim = Color.Transparent,
        onSecondaryFixed = ink,
        onSecondaryFixedVariant = base.onSurfaceVariant,
        tertiaryFixed = accent.fill,
        tertiaryFixedDim = accent.fill,
        onTertiaryFixed = accent.onFill,
        onTertiaryFixedVariant = accent.onFill,
    )
}

/** Fixed roles for the plain Yuyu schemes (Material's defaults would be purple). */
fun ColorScheme.withYuyuFixedRoles(isDark: Boolean): ColorScheme {
    val accent = yuyuAccentFrom(YuyuDefaultAccent, isDark)
    return copy(
        primaryFixed = if (isDark) YuyuColors.DarkBgEmphasis else YuyuColors.BgEmphasis,
        primaryFixedDim = if (isDark) YuyuColors.DarkBgMuted else YuyuColors.Border,
        onPrimaryFixed = onSurface,
        onPrimaryFixedVariant = onSurfaceVariant,
        secondaryFixed = if (isDark) YuyuColors.DarkBgMuted else YuyuColors.BgMuted,
        secondaryFixedDim = if (isDark) YuyuColors.DarkBgEmphasis else YuyuColors.BgEmphasis,
        onSecondaryFixed = onSurface,
        onSecondaryFixedVariant = onSurfaceVariant,
        tertiaryFixed = accent.fill,
        tertiaryFixedDim = accent.fill,
        onTertiaryFixed = accent.onFill,
        onTertiaryFixedVariant = accent.onFill,
    )
}
