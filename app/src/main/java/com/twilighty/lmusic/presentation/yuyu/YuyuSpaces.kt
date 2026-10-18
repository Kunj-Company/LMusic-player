package com.twilighty.lmusic.presentation.yuyu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One place on Home that opens a "space": lofi, focus, garden, party, taste match, covers. */
data class YuyuSpace(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun YuyuSpacesRow(
    onLofi: () -> Unit,
    onFocus: () -> Unit,
    onGarden: () -> Unit,
    onParty: () -> Unit,
    onTasteMatch: () -> Unit,
    onCovers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spaces = listOf(
        YuyuSpace("lofi", "Lofi", "Make a tune or tune in", Icons.Rounded.Headphones, onLofi),
        YuyuSpace("focus", "Focus", "Timer with calm music", Icons.Rounded.Timer, onFocus),
        YuyuSpace("garden", "Garden", "Grows as you listen", Icons.Rounded.Spa, onGarden),
        YuyuSpace("party", "Party", "Friends add songs", Icons.Rounded.Celebration, onParty),
        YuyuSpace("taste", "Taste match", "Scan a friend", Icons.Rounded.QrCode2, onTasteMatch),
        YuyuSpace("covers", "Cover studio", "Art for bare songs", Icons.Rounded.Palette, onCovers),
    )
    val tints = listOf(Yuyu.accentSoft, Yuyu.tintLavender, Yuyu.tintMint, Yuyu.tintPeach, Yuyu.tintLavender, Yuyu.tintMint)
    Column(modifier) {
        YuyuSectionHeader(title = "Spaces", modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(spaces.size, key = { "space_${spaces[it].id}" }) { index ->
                val space = spaces[index]
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .clip(YuyuShapes.xl)
                        .background(tints[index % tints.size])
                        .clickable(role = Role.Button, onClick = space.onClick)
                        .padding(14.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(YuyuShapes.md).background(Yuyu.canvas.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(space.icon, contentDescription = null, tint = Yuyu.text, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(space.title, style = YuyuType.title, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(space.subtitle, style = YuyuType.caption, color = Yuyu.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Suppress("unused")
private val keepColorImport = Color.Unspecified
