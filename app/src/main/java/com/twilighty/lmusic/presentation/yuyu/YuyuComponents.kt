package com.twilighty.lmusic.presentation.yuyu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.size.Size
import com.twilighty.lmusic.presentation.components.SmartImage
import com.twilighty.lmusic.ui.theme.YuyuBody
import com.twilighty.lmusic.ui.theme.YuyuDisplay

// =====================================================================================
// LMusic on the Yuyu design system — the Compose versions of the parts on the
// "LMusic — Yuyu screens" canvas. Colours always come from MaterialTheme (the Yuyu
// scheme maps: surface = canvas, surfaceContainerLow = subtle, surfaceContainer = muted,
// surfaceContainerHigh = emphasis, outlineVariant = border, outline = border-strong,
// primary = ink, tertiary = accent). Album-tinted areas pass their own accent.
// =====================================================================================

object YuyuType {
    fun display(size: TextUnit = 36.sp) = TextStyle(
        fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = size,
        lineHeight = size * 1.02f, letterSpacing = (-0.03).em
    )
    val h1 = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.02).em)
    val h2 = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em)
    val h3 = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 25.sp)
    val title = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp)
    val body = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val bodyLarge = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 27.sp)
    val meta = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp)
    val label = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val overline = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
    val stat = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em)
}

object YuyuShapes {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
    val xxl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(999.dp)
}

/** Colours read from the active scheme, named the way the Yuyu tokens are. */
object Yuyu {
    val canvas: Color @Composable get() = MaterialTheme.colorScheme.surface
    val subtle: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow
    val muted: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainer
    val emphasis: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val borderStrong: Color @Composable get() = MaterialTheme.colorScheme.outline
    val text: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val textMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val ink: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onInk: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val accent: Color @Composable get() = MaterialTheme.colorScheme.tertiaryFixed
    val onAccent: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryFixed
    val accentText: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val accentSoft: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
    val danger: Color @Composable get() = MaterialTheme.colorScheme.error
    val dangerSoft: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
    val successSoft: Color @Composable get() = if (isDark) Color(0xFF13301F) else Color(0xFFDCFCE7)
    val successText: Color @Composable get() = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
    val warningSoft: Color @Composable get() = if (isDark) Color(0xFF33270B) else Color(0xFFFEF3C7)
    val warningText: Color @Composable get() = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
    val tintMint: Color @Composable get() = if (isDark) Color(0xFF10261E) else Color(0xFFE9FBF3)
    val tintLavender: Color @Composable get() = if (isDark) Color(0xFF1C1A33) else Color(0xFFF1EFFF)
    val tintPeach: Color @Composable get() = if (isDark) Color(0xFF2A1F10) else Color(0xFFFFF4E5)
    val highlightMint: Color @Composable get() = if (isDark) Color(0xFF1F4D3D) else Color(0xFFBDF3DF)
    val highlightPeach: Color @Composable get() = if (isDark) Color(0xFF4A3418) else Color(0xFFFDE7C8)
    private val isDark: Boolean @Composable get() = MaterialTheme.colorScheme.surface.red < 0.5f
}

// ---------------------------------------------------------------- Buttons

enum class YuyuButtonStyle { Primary, Accent, Secondary, Quiet, Danger, DangerQuiet, Inverse }
enum class YuyuButtonSize(val height: Dp, val hPad: Dp, val fontSize: TextUnit) {
    Small(36.dp, 14.dp, 14.sp), Medium(44.dp, 18.dp, 15.sp), Large(52.dp, 24.dp, 17.sp)
}

@Composable
fun YuyuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: YuyuButtonStyle = YuyuButtonStyle.Primary,
    size: YuyuButtonSize = YuyuButtonSize.Medium,
    enabled: Boolean = true,
    icon: Painter? = null,
    accent: Color? = null,
    onAccent: Color? = null,
) {
    val accentFill = accent ?: Yuyu.accent
    val accentOn = onAccent ?: Yuyu.onAccent
    val haptics = com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics.current
    val (bg, fg, borderColor) = when {
        !enabled -> Triple(Yuyu.emphasis, Yuyu.textMuted, Color.Transparent)
        style == YuyuButtonStyle.Primary -> Triple(Yuyu.ink, Yuyu.onInk, Color.Transparent)
        style == YuyuButtonStyle.Accent -> Triple(accentFill, accentOn, Color.Transparent)
        style == YuyuButtonStyle.Secondary -> Triple(Yuyu.canvas, Yuyu.text, Yuyu.borderStrong)
        style == YuyuButtonStyle.Quiet -> Triple(Color.Transparent, Yuyu.textSecondary, Color.Transparent)
        style == YuyuButtonStyle.Danger -> Triple(Yuyu.canvas, Yuyu.danger, Yuyu.danger)
        style == YuyuButtonStyle.DangerQuiet -> Triple(Color.Transparent, Yuyu.danger, Color.Transparent)
        else -> Triple(Color.White, Color(0xFF18181B), Color.Transparent)
    }
    Row(
        modifier = modifier
            .heightIn(min = size.height)
            .clip(YuyuShapes.pill)
            .background(bg)
            .then(if (borderColor != Color.Transparent) Modifier.border(if (style == YuyuButtonStyle.Danger) 1.5.dp else 1.dp, borderColor, YuyuShapes.pill) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = { haptics.tap(); onClick() })
            .padding(horizontal = if (style == YuyuButtonStyle.Quiet || style == YuyuButtonStyle.DangerQuiet) 8.dp else size.hPad),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (size == YuyuButtonSize.Small) 16.dp else 18.dp))
        }
        Text(
            text = text,
            color = fg,
            style = TextStyle(fontFamily = YuyuBody, fontWeight = if (style == YuyuButtonStyle.Secondary || style == YuyuButtonStyle.Quiet) FontWeight.SemiBold else FontWeight.Bold, fontSize = size.fontSize),
            maxLines = 1
        )
    }
}

enum class YuyuIconButtonStyle { Plain, Tonal, Outline, Filled, Accent }

@Composable
fun YuyuIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: YuyuIconButtonStyle = YuyuIconButtonStyle.Plain,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val haptics = com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics.current
    val bg = when (style) {
        YuyuIconButtonStyle.Tonal -> Yuyu.muted
        YuyuIconButtonStyle.Filled -> Yuyu.ink
        YuyuIconButtonStyle.Accent -> Yuyu.accent
        else -> Color.Transparent
    }
    val fg = tint ?: when (style) {
        YuyuIconButtonStyle.Filled -> Yuyu.onInk
        YuyuIconButtonStyle.Accent -> Yuyu.onAccent
        else -> Yuyu.text
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(if (style == YuyuIconButtonStyle.Outline) Modifier.border(1.dp, Yuyu.borderStrong, CircleShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = { haptics.tap(); onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter, contentDescription = contentDescription, tint = if (enabled) fg else Yuyu.textMuted, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun YuyuIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: YuyuIconButtonStyle = YuyuIconButtonStyle.Plain,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
    enabled: Boolean = true,
) = YuyuIconButton(
    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(imageVector),
    contentDescription = contentDescription, onClick = onClick, modifier = modifier,
    style = style, size = size, iconSize = iconSize, tint = tint, enabled = enabled
)

// ---------------------------------------------------------------- Chips

@Composable
fun YuyuChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
) {
    val bg = if (selected) Yuyu.ink else Yuyu.canvas
    val fg = if (selected) Yuyu.onInk else Yuyu.text
    val haptics = com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics.current
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(YuyuShapes.pill)
            .background(bg)
            .then(if (!selected) Modifier.border(1.dp, Yuyu.borderStrong, YuyuShapes.pill) else Modifier)
            .clickable(role = Role.Tab, onClick = { haptics.selection(); onClick() })
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, color = fg, style = YuyuType.label, maxLines = 1)
    }
}

enum class YuyuStatus { Success, Warning, Danger, Neutral, Accent }

@Composable
fun YuyuStatusChip(text: String, status: YuyuStatus, modifier: Modifier = Modifier, icon: Painter? = null) {
    val (bg, fg) = when (status) {
        YuyuStatus.Success -> Yuyu.successSoft to Yuyu.successText
        YuyuStatus.Warning -> Yuyu.warningSoft to Yuyu.warningText
        YuyuStatus.Danger -> Yuyu.dangerSoft to Yuyu.danger
        YuyuStatus.Neutral -> Yuyu.muted to Yuyu.textSecondary
        YuyuStatus.Accent -> Yuyu.accentSoft to Yuyu.accentText
    }
    Row(
        modifier = modifier
            .clip(YuyuShapes.pill)
            .background(bg)
            .padding(horizontal = 11.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, color = fg, style = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Bold, fontSize = 13.sp), maxLines = 1)
    }
}

// ---------------------------------------------------------------- Album art

private val ArtPalette = listOf(
    Color(0xFFC2410C), Color(0xFF4B3FA8), Color(0xFF0F766E), Color(0xFF27272A),
    Color(0xFF4D6B2F), Color(0xFF7E2F5E), Color(0xFF1D4E89), Color(0xFF8A4B2A)
)

/** Stable placeholder colour for a title/album, from the Yuyu cover palette. */
fun yuyuArtColor(key: String): Color = ArtPalette[(key.hashCode() and 0x7fffffff) % ArtPalette.size]

fun yuyuInitials(text: String): String {
    val words = text.split(' ', '-', '_', '/', '.').filter { it.isNotBlank() && it.first().isLetterOrDigit() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }.uppercase()
}

/**
 * Album art the Yuyu way: real artwork when there is some, otherwise the cover
 * placeholder — a solid colour with a dot grid and the album's initials.
 */
@Composable
fun YuyuArt(
    model: Any?,
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape((size.value * 0.21f).coerceIn(8f, 24f).dp),
    contentDescription: String? = null,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val color = remember(title) { yuyuArtColor(title) }
    Box(modifier = modifier.size(size).clip(shape).background(color)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 14.dp.toPx()
            val r = 1.4.dp.toPx()
            var y = step / 2
            while (y < this.size.height) {
                var x = step / 2
                while (x < this.size.width) {
                    drawCircle(Color.White.copy(alpha = 0.17f), radius = r, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        Text(
            text = yuyuInitials(title),
            color = Color.White,
            style = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.28f).sp, letterSpacing = (-0.03).em, lineHeight = (size.value * 0.28f).sp),
            modifier = Modifier.align(Alignment.BottomStart).padding(size * 0.1f),
            maxLines = 1
        )
        if (model != null && (model !is String || model.isNotBlank())) {
            SmartImage(
                model = model,
                contentDescription = contentDescription,
                shape = shape,
                targetSize = Size(sizePx, sizePx),
                placeHolderBackgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ---------------------------------------------------------------- Layout pieces

@Composable
fun YuyuTopBar(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    titleStyle: TextStyle = YuyuType.h1,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = if (navigationIcon != null) 4.dp else 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        navigationIcon?.invoke()
        Column(Modifier.weight(1f)) {
            if (kicker != null) Text(kicker, style = YuyuType.caption, color = Yuyu.textMuted)
            Text(title, style = titleStyle, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun YuyuSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = YuyuType.h3, color = Yuyu.text, modifier = Modifier.weight(1f, fill = false))
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = YuyuType.label,
                color = Yuyu.textSecondary,
                modifier = Modifier
                    .clip(YuyuShapes.pill)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun YuyuOverline(text: String, modifier: Modifier = Modifier, color: Color = Yuyu.textMuted) {
    Text(text.uppercase(), style = YuyuType.overline, color = color, modifier = modifier)
}

@Composable
fun YuyuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Yuyu.border,
    background: Color = Yuyu.canvas,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(YuyuShapes.lg)
            .background(background)
            .border(1.dp, borderColor, YuyuShapes.lg)
            .then(if (onClick != null) Modifier.clickable(onClick = { com.twilighty.lmusic.presentation.haptics.LMusicHaptics.get().tap(); onClick() }) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun YuyuTile(
    background: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(YuyuShapes.xl)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = { com.twilighty.lmusic.presentation.haptics.LMusicHaptics.get().tap(); onClick() }) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun YuyuStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingColor: Color = Yuyu.textMuted,
    onClick: (() -> Unit)? = null,
) {
    YuyuCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, style = YuyuType.meta, color = Yuyu.textMuted, maxLines = 1)
        Text(value, style = YuyuType.stat, color = Yuyu.text, maxLines = 1)
        if (supporting != null) Text(supporting, style = YuyuType.caption.copy(fontWeight = FontWeight.SemiBold), color = supportingColor, maxLines = 1)
    }
}

@Composable
fun YuyuMeter(progress: Float, modifier: Modifier = Modifier, color: Color = Yuyu.accent, track: Color = Yuyu.emphasis, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(YuyuShapes.pill).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(YuyuShapes.pill).background(color))
    }
}

/** Yuyu's date block, used for album years and listening-history days. */
@Composable
fun YuyuYearBlock(top: String, number: String, modifier: Modifier = Modifier, accent: Color = Yuyu.accent, onAccent: Color = Yuyu.onAccent) {
    Column(
        modifier = modifier
            .width(52.dp)
            .clip(YuyuShapes.md)
            .border(1.dp, Yuyu.border, YuyuShapes.md)
            .background(Yuyu.canvas),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            top.uppercase(),
            style = TextStyle(fontFamily = YuyuBody, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.em),
            color = onAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(accent).padding(vertical = 3.dp)
        )
        Text(number, style = TextStyle(fontFamily = YuyuDisplay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 28.sp), color = Yuyu.text)
    }
}

// ---------------------------------------------------------------- Rows

@Composable
fun YuyuSongRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    artModel: Any? = null,
    artKey: String = title,
    isCurrent: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(YuyuShapes.lg)
            .then(if (onClick != null) Modifier.clickable(onClick = { com.twilighty.lmusic.presentation.haptics.LMusicHaptics.get().tap(); onClick() }) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (leading != null) leading() else YuyuArt(model = artModel, title = artKey, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = YuyuType.title, color = if (isCurrent) Yuyu.accentText else Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = YuyuType.meta, color = Yuyu.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailingText != null) Text(trailingText, style = YuyuType.caption, color = Yuyu.textMuted)
        trailing?.invoke(this)
    }
}

@Composable
fun YuyuSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = { com.twilighty.lmusic.presentation.haptics.LMusicHaptics.get().tap(); onClick() }) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(YuyuShapes.md).background(Yuyu.muted), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Yuyu.text, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = YuyuType.title, color = Yuyu.text)
            if (subtitle != null) Text(subtitle, style = YuyuType.meta, color = Yuyu.textMuted)
        }
        trailing?.invoke()
    }
}

@Composable
fun YuyuSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { callback -> { value: Boolean -> haptics.toggle(value); callback(value) } },
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Yuyu.ink,
            checkedThumbColor = Yuyu.onInk,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = Yuyu.borderStrong,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
        ),
        thumbContent = null
    )
}

/** Pill segmented control (Light / Dark / System). */
@Composable
fun YuyuSegmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics.current
    Row(
        modifier = modifier.clip(YuyuShapes.pill).background(Yuyu.muted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(YuyuShapes.pill)
                    .background(if (selected) Yuyu.canvas else Color.Transparent)
                    .clickable(role = Role.Tab) { if (index != selectedIndex) haptics.selection(); onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option, style = YuyuType.label, color = if (selected) Yuyu.text else Yuyu.textSecondary, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- Feedback

@Composable
fun YuyuCallout(
    boldText: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(YuyuShapes.lg)
            .background(Yuyu.warningSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Yuyu.warningText, modifier = Modifier.size(18.dp))
        Text(
            buildString { append(boldText); append(' '); append(text) },
            style = YuyuType.meta,
            color = Yuyu.warningText,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = YuyuType.label.copy(fontWeight = FontWeight.Bold),
                color = Yuyu.warningText,
                modifier = Modifier.clip(YuyuShapes.pill).clickable(role = Role.Button, onClick = onAction).padding(6.dp)
            )
        }
    }
}

@Composable
fun YuyuEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(YuyuShapes.lg)
            .dashedBorder(Yuyu.borderStrong)
            .padding(horizontal = 22.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Yuyu.muted), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Yuyu.text, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, style = YuyuType.h3, color = Yuyu.text, textAlign = TextAlign.Center)
        Text(body, style = YuyuType.body, color = Yuyu.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp), content = actions)
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = this.drawBehindDashed(color)

private fun Modifier.drawBehindDashed(color: Color): Modifier = this.drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        ),
        topLeft = Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
    )
}

/** Thin divider in Yuyu border colour. */
@Composable
fun YuyuDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Yuyu.border))
}

@Composable
fun YuyuSpacer(height: Dp) = Spacer(Modifier.height(height))
