package com.twilighty.lmusic.ui.glancewidget

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.twilighty.lmusic.data.model.PlayerInfo

object AlbumArtBitmapCache {
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024 // 4 MiB
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getBitmap(key: String): Bitmap? = lruCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            lruCache.put(key, bitmap)
        }
    }

    fun getKey(byteArray: ByteArray): String {
        return byteArray.contentHashCode().toString()
    }
}

data class WidgetColors(
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val artist: ColorProvider,
    val playPauseBackground: ColorProvider,
    val playPauseIcon: ColorProvider,
    val prevNextBackground: ColorProvider,
    val prevNextIcon: ColorProvider
)

/**
 * Widget colours, Yuyu style: flat canvas (white / #111113), ink title, muted artist,
 * Ember play button with a white icon, muted circles for the other controls.
 * Album colours are no longer used so every widget matches the app.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun PlayerInfo.getWidgetColors(): WidgetColors = WidgetColors(
    surface = ColorProvider(day = YuyuWidgetPalette.LightCanvas, night = YuyuWidgetPalette.DarkCanvas),
    onSurface = ColorProvider(day = YuyuWidgetPalette.LightText, night = YuyuWidgetPalette.DarkText),
    artist = ColorProvider(day = YuyuWidgetPalette.LightTextMuted, night = YuyuWidgetPalette.DarkTextMuted),
    playPauseBackground = ColorProvider(day = YuyuWidgetPalette.Ember, night = YuyuWidgetPalette.Ember),
    playPauseIcon = ColorProvider(day = YuyuWidgetPalette.OnEmber, night = YuyuWidgetPalette.OnEmber),
    prevNextBackground = ColorProvider(day = YuyuWidgetPalette.LightMuted, night = YuyuWidgetPalette.DarkMuted),
    prevNextIcon = ColorProvider(day = YuyuWidgetPalette.LightText, night = YuyuWidgetPalette.DarkText),
)
