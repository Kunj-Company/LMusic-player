package com.twilighty.lmusic.presentation.smart

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.smart.IndexProgress
import com.twilighty.lmusic.presentation.components.MiniPlayerHeight
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuDivider
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuMeter
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuSettingRow
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuStatus
import com.twilighty.lmusic.presentation.yuyu.YuyuStatusChip
import com.twilighty.lmusic.presentation.yuyu.YuyuSwitch
import com.twilighty.lmusic.presentation.yuyu.YuyuTile
import com.twilighty.lmusic.presentation.yuyu.YuyuTopBar
import com.twilighty.lmusic.presentation.yuyu.YuyuType

const val SMART_CONDITIONS_TEXT = "Indexes while charging, idle and above 50% battery"

/** Settings → Smart features (experimental). */
@Composable
fun SmartSettingsScreen(
    onNavigationIconClick: () -> Unit,
    viewModel: SmartSettingsViewModel = hiltViewModel()
) {
    val enabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    var showEnableDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val bottomPad = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Yuyu.canvas)
            .statusBarsPadding()
    ) {
        YuyuTopBar(
            title = "Smart features",
            kicker = "Experimental",
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
            YuyuTile(background = Yuyu.tintLavender) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Yuyu.text, modifier = Modifier.size(28.dp))
                Text(
                    "Your library, understood on this phone",
                    style = YuyuType.h3,
                    color = Yuyu.text,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "LMusic listens to a few seconds of each song to learn its mood, genre, energy and tempo. " +
                        "That powers song radio, daily mixes and mood insights.",
                    style = YuyuType.body,
                    color = Yuyu.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            YuyuCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                YuyuSettingRow(
                    title = "Smart features",
                    subtitle = if (enabled) "On · works offline" else "Off",
                    icon = rememberVectorPainter(Icons.Rounded.GraphicEq),
                    trailing = {
                        YuyuSwitch(
                            checked = enabled,
                            onCheckedChange = { on ->
                                if (on) showEnableDialog = true else viewModel.setEnabled(false)
                            }
                        )
                    },
                    onClick = { if (enabled) viewModel.setEnabled(false) else showEnableDialog = true }
                )
            }

            if (enabled || progress.analyzed > 0) {
                IndexProgressCard(progress = progress, enabled = enabled)
            }

            YuyuCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                YuyuOverline("What you get", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                FeatureRow(Icons.Rounded.Radio, "Song radio", "Play similar songs from any song's menu")
                YuyuDivider()
                FeatureRow(Icons.AutoMirrored.Rounded.QueueMusic, "Mixes made for you", "Daily mixes, Calm, Focus, Workout, Late night")
                YuyuDivider()
                FeatureRow(Icons.Rounded.Insights, "Mood insights", "Your mood mix, energy by hour and taste drift in Stats")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(YuyuShapes.lg)
                    .background(Yuyu.subtle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Yuyu.textSecondary, modifier = Modifier.size(18.dp))
                Text(
                    "Everything stays on this phone. No audio or data is uploaded.",
                    style = YuyuType.meta,
                    color = Yuyu.textSecondary
                )
            }

            if (progress.analyzed > 0 || progress.failed > 0) {
                YuyuButton(
                    text = if (isDeleting) "Deleting…" else "Delete smart data",
                    onClick = { showDeleteDialog = true },
                    style = YuyuButtonStyle.Danger,
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showEnableDialog) {
        SmartEnableDialog(
            onConfirm = {
                showEnableDialog = false
                viewModel.setEnabled(true)
            },
            onDismiss = { showEnableDialog = false }
        )
    }
    if (showDeleteDialog) {
        YuyuConfirmDialog(
            title = "Delete smart data?",
            body = "This removes the mood, genre and energy data for every song. " +
                if (enabled) "Indexing will start again while charging." else "You can index again later.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteSmartData()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun IndexProgressCard(progress: IndexProgress, enabled: Boolean) {
    YuyuCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YuyuOverline("Library index", modifier = Modifier.weight(1f))
            val (label, status) = when {
                !enabled -> "Paused" to YuyuStatus.Neutral
                progress.running -> "Indexing now" to YuyuStatus.Accent
                progress.total > 0 && progress.analyzed + progress.failed >= progress.total -> "Up to date" to YuyuStatus.Success
                else -> "Waiting to charge" to YuyuStatus.Warning
            }
            YuyuStatusChip(text = label, status = status)
        }
        Text(
            "${progress.analyzed} of ${progress.total} songs",
            style = YuyuType.h3,
            color = Yuyu.text,
            modifier = Modifier.padding(top = 10.dp)
        )
        YuyuMeter(progress = progress.fraction, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        val last = if (progress.lastIndexedAt > 0L) {
            "Last run " + DateUtils.getRelativeTimeSpanString(
                progress.lastIndexedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ).toString().lowercase()
        } else {
            "Not run yet"
        }
        val failed = if (progress.failed > 0) " · ${progress.failed} couldn't be read" else ""
        Text(last + failed, style = YuyuType.meta, color = Yuyu.textMuted, modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = Yuyu.textSecondary, modifier = Modifier.size(16.dp))
            Text(SMART_CONDITIONS_TEXT, style = YuyuType.caption, color = Yuyu.textSecondary)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    YuyuSettingRow(title = title, subtitle = subtitle, icon = rememberVectorPainter(icon))
}

@Composable
fun SmartEnableDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(YuyuShapes.xxl)
                .background(Yuyu.canvas)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(YuyuShapes.md).background(Yuyu.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Yuyu.accentText, modifier = Modifier.size(24.dp))
            }
            Text("Turn on smart features?", style = YuyuType.h3, color = Yuyu.text)
            DialogPoint("What it does", "Listens to three short clips of each song to tag mood, genre, energy and tempo.")
            DialogPoint("On-device only", "A 4 MB model runs on this phone. Nothing is uploaded.")
            DialogPoint("Battery friendly", "$SMART_CONDITIONS_TEXT. It may take a few nights for a big library.")
            DialogPoint("Experimental", "Results can be off. You can turn it off any time.")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                YuyuButton(text = "Not now", onClick = onDismiss, style = YuyuButtonStyle.Quiet)
                YuyuButton(text = "Turn on", onClick = onConfirm, style = YuyuButtonStyle.Primary)
            }
        }
    }
}

@Composable
private fun DialogPoint(title: String, body: String) {
    Column {
        Text(title, style = YuyuType.label, color = Yuyu.text)
        Text(body, style = YuyuType.meta, color = Yuyu.textSecondary)
    }
}

@Composable
private fun YuyuConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(YuyuShapes.xxl)
                .background(Yuyu.canvas)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = YuyuType.h3, color = Yuyu.text)
            Text(body, style = YuyuType.body, color = Yuyu.textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                YuyuButton(text = "Cancel", onClick = onDismiss, style = YuyuButtonStyle.Quiet)
                YuyuButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    style = if (danger) YuyuButtonStyle.Danger else YuyuButtonStyle.Primary
                )
            }
        }
    }
}
