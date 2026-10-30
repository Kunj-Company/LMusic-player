package com.twilighty.lmusic.presentation.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.twilighty.lmusic.R
import com.twilighty.lmusic.presentation.components.MiniPlayerHeight
import com.twilighty.lmusic.presentation.navigation.Screen
import com.twilighty.lmusic.presentation.navigation.navigateSafely
import com.twilighty.lmusic.presentation.viewmodel.PlayerViewModel
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuDivider
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuSettingRow
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuStatus
import com.twilighty.lmusic.presentation.yuyu.YuyuStatusChip
import com.twilighty.lmusic.presentation.yuyu.YuyuTile
import com.twilighty.lmusic.presentation.yuyu.YuyuTopBar
import com.twilighty.lmusic.presentation.yuyu.YuyuType

private const val DeveloperName = "Abhiram NJ"
private const val RepoUrl = "https://github.com/twilighty-abhi/lmusic-player"
private const val ReleasesUrl = "$RepoUrl/releases"
private const val IssuesUrl = "$RepoUrl/issues"

@OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun AboutScreen(
    navController: NavController,
    viewModel: PlayerViewModel,
    onNavigationIconClick: () -> Unit,
) {
    val context = LocalContext.current
    val versionName: String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
    } catch (_: Exception) {
        "—"
    }
    val bottomPad = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Yuyu.canvas)
            .statusBarsPadding()
    ) {
        YuyuTopBar(
            title = "About",
            titleStyle = YuyuType.h2,
            navigationIcon = {
                YuyuIconButton(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onNavigationIconClick
                )
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPad),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AboutHero(
                versionName = versionName,
                onVersionLongPress = { navController.navigateSafely(Screen.EasterEgg.route) }
            )

            YuyuCard {
                YuyuOverline("Developers")
                Spacer(Modifier.height(6.dp))
                DeveloperRow(
                    name = DeveloperName,
                    handle = "Twilight Abhi · @twilighty-abhi",
                    avatar = R.drawable.avatar_abhiram_nj,
                    onClick = { openUrl(context, "https://github.com/twilighty-abhi") }
                )
                DeveloperRow(
                    name = "Shaan Shoukath",
                    handle = "@Shaan-Shoukath",
                    avatar = R.drawable.avatar_shaan_shoukath,
                    onClick = { openUrl(context, "https://github.com/Shaan-Shoukath") }
                )
            }

            YuyuCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                AboutLinkRow(title = "Source code", subtitle = "github.com/twilighty-abhi/lmusic-player") { openUrl(context, RepoUrl) }
                YuyuDivider()
                AboutLinkRow(title = "Updates", subtitle = "Every release, with the APK to install") { openUrl(context, ReleasesUrl) }
                YuyuDivider()
                AboutLinkRow(title = "Report a problem", subtitle = "Open an issue on GitHub") { openUrl(context, IssuesUrl) }
            }

            YuyuTile(background = Yuyu.subtle) {
                Text("Made for listening, not for engagement.", style = YuyuType.title, color = Yuyu.text)
                Spacer(Modifier.height(6.dp))
                Text(
                    "LMusic plays the music on your phone. No accounts, no ads, no tracking — your listening stats stay on this device.",
                    style = YuyuType.body,
                    color = Yuyu.textSecondary
                )
            }

            Text(
                "© $DeveloperName. Released under the MIT License.",
                style = YuyuType.caption,
                color = Yuyu.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutHero(versionName: String, onVersionLongPress: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    YuyuTile(background = Yuyu.accentSoft, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(64.dp).clip(YuyuShapes.xl).background(Yuyu.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.lmusic_base_monochrome),
                contentDescription = null,
                tint = Yuyu.onAccent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("LMusic", style = YuyuType.display(40.sp()), color = Yuyu.text)
        Spacer(Modifier.height(4.dp))
        Text("Your music, on your phone.", style = YuyuType.bodyLarge, color = Yuyu.textSecondary)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .clip(YuyuShapes.pill)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        com.twilighty.lmusic.presentation.haptics.LMusicHaptics.get().longPress()
                        onVersionLongPress()
                    }
                )
        ) {
            YuyuStatusChip(text = "Version $versionName", status = YuyuStatus.Accent)
        }
    }
}

@Composable
private fun DeveloperRow(name: String, handle: String, @DrawableRes avatar: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(YuyuShapes.md)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = painterResource(avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(YuyuShapes.pill)
        )
        Column(Modifier.weight(1f)) {
            Text(name, style = YuyuType.h3, color = Yuyu.text)
            Text(handle, style = YuyuType.meta, color = Yuyu.textMuted)
        }
        Icon(
            painter = painterResource(R.drawable.github),
            contentDescription = "GitHub profile",
            tint = Yuyu.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AboutLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    YuyuSettingRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Yuyu.textMuted
            )
        }
    )
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}
