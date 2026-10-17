package com.twilighty.lmusic.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.twilighty.lmusic.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.ui.unit.sp
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonSize
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuChip
import com.twilighty.lmusic.presentation.yuyu.YuyuDialog
import com.twilighty.lmusic.presentation.yuyu.YuyuDivider
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuRadioRow
import com.twilighty.lmusic.presentation.yuyu.YuyuSegmented
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuSheetHandle
import com.twilighty.lmusic.presentation.yuyu.YuyuSheetShape
import com.twilighty.lmusic.presentation.yuyu.YuyuType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.twilighty.lmusic.R

val predefinedTimes = listOf(0, 5, 10, 15, 20, 30, 45, 60) // 0 represents 'Off'

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TimerOptionsBottomSheet(
    onPlayCounter: (count: Int) -> Unit,
    activeTimerValueDisplay: String?,
    activeTimerDurationMinutes: Int?,
    playCount: Float,
    isEndOfTrackTimerActive: Boolean,
    onDismiss: () -> Unit,
    onCancelCountedPlay: () -> Unit,
    onSetPredefinedTimer: (minutes: Int) -> Unit,
    onSetEndOfTrackTimer: (enable: Boolean) -> Unit,
    onOpenCustomTimePicker: () -> Unit,
    onCancelTimer: () -> Unit
) {
    var showCustomTimePicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var timerSliderPosition by remember { mutableStateOf(0f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSwitchEnabled = isEndOfTrackTimerActive

    var counterSliderPosition by remember { mutableStateOf(1f) }
    var isTimerMode by remember { mutableStateOf(true) } // true = timer mode, false = counter mode

    val timerPresets = remember { predefinedTimes.filter { it > 0 } }
    val countPresets = remember { (1..10).toList() }

    LaunchedEffect(activeTimerDurationMinutes, activeTimerValueDisplay, playCount) {
        timerSliderPosition = when {
            activeTimerDurationMinutes != null -> {
                val index = predefinedTimes.indexOf(activeTimerDurationMinutes)
                if (index != -1) index.toFloat() else 0f
            }
            else -> 0f
        }
        counterSliderPosition = playCount
        // Restore counter mode if play count was previously set
        if (playCount > 1f) {
            isTimerMode = false
        }
    }

    val isCounterActive = playCount > 1f
    val hasActiveTimer = activeTimerValueDisplay != null || isCounterActive

    com.twilighty.lmusic.presentation.haptics.HapticBottomSheetEffect(sheetState)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Yuyu.canvas,
        tonalElevation = 0.dp,
        shape = YuyuSheetShape,
        dragHandle = { YuyuSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = Yuyu.text)
                Text(
                    text = stringResource(R.string.sleep_timer_ui_title),
                    style = YuyuType.h2,
                    color = Yuyu.text,
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasActiveTimer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(YuyuShapes.lg)
                        .background(Yuyu.accentSoft)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isCounterActive && activeTimerValueDisplay == null) "Music stops after" else "Music stops in",
                            style = YuyuType.label,
                            color = Yuyu.accentText
                        )
                        Text(
                            text = when {
                                activeTimerValueDisplay != null -> activeTimerValueDisplay
                                else -> pluralStringResource(R.plurals.sleep_timer_play_count_times, playCount.toInt(), playCount.toInt())
                            },
                            style = YuyuType.display(30.sp),
                            color = Yuyu.accentText,
                            maxLines = 1
                        )
                    }
                    YuyuButton(
                        text = "Turn off",
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                            onCancelCountedPlay()
                        },
                        style = YuyuButtonStyle.DangerQuiet,
                        size = YuyuButtonSize.Small
                    )
                }
            }

            YuyuSegmented(
                options = listOf(stringResource(R.string.sleep_timer_ui_slider_label_timer), "Song count"),
                selectedIndex = if (isTimerMode) 0 else 1,
                onSelect = { isTimerMode = it == 0 }
            )

            if (isTimerMode) {
                YuyuOverline("Stop after")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timerPresets.forEach { minutes ->
                        YuyuChip(
                            text = if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60} h" else "$minutes min",
                            selected = activeTimerDurationMinutes == minutes && !isEndOfTrackTimerActive,
                            onClick = {
                                if (activeTimerDurationMinutes == minutes) {
                                    onCancelTimer()
                                } else {
                                    onCancelCountedPlay()
                                    onSetPredefinedTimer(minutes)
                                }
                            }
                        )
                    }
                }
                Column {
                    YuyuRadioRow(
                        title = stringResource(R.string.sleep_timer_ui_end_of_current_track),
                        selected = isEndOfTrackTimerActive,
                        onClick = { onSetEndOfTrackTimer(!isEndOfTrackTimerActive) }
                    )
                    YuyuDivider()
                    YuyuRadioRow(
                        title = stringResource(R.string.sleep_timer_ui_custom_time),
                        selected = false,
                        onClick = { showCustomTimePicker = true },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Yuyu.textMuted
                            )
                        }
                    )
                }
            } else {
                val currentPlayCount = counterSliderPosition.toInt().coerceAtLeast(1)
                val timesPart = if (currentPlayCount == 1) {
                    stringResource(R.string.sleep_timer_ui_play_count_one_time)
                } else {
                    pluralStringResource(
                        R.plurals.sleep_timer_play_count_times,
                        currentPlayCount,
                        currentPlayCount
                    )
                }
                YuyuOverline(stringResource(R.string.sleep_timer_ui_play_count_label, timesPart))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    countPresets.forEach { count ->
                        YuyuChip(
                            text = count.toString(),
                            selected = isCounterActive && playCount.toInt() == count,
                            onClick = {
                                counterSliderPosition = count.toFloat()
                                onCancelTimer()
                                onSetEndOfTrackTimer(false)
                                onPlayCounter(count)
                            },
                            modifier = Modifier.widthIn(min = 48.dp)
                        )
                    }
                }
                Text(
                    "Playback stops after this many songs.",
                    style = YuyuType.caption,
                    color = Yuyu.textMuted
                )
            }

            YuyuButton(
                text = "Done",
                onClick = onDismiss,
                style = YuyuButtonStyle.Accent,
                size = YuyuButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showCustomTimePicker) {
        val initialHour = 0    // Default to 0 hours for a duration
        val initialMinute = 15 // Default to 15 minutes for a duration

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        YuyuDialog(
            onDismissRequest = { showCustomTimePicker = false },
            title = stringResource(R.string.sleep_timer_ui_set_custom_duration),
            actions = {
                YuyuButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showCustomTimePicker = false },
                    style = YuyuButtonStyle.Secondary
                )
                YuyuButton(
                    text = "Start timer",
                    onClick = {
                        val totalMinutes = timePickerState.hour * 60 + timePickerState.minute
                        if (totalMinutes > 0) {
                            onCancelCountedPlay()
                            onSetPredefinedTimer(totalMinutes)
                        }
                        showCustomTimePicker = false
                        onDismiss()
                    },
                    style = YuyuButtonStyle.Accent
                )
            }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Yuyu.muted,
                    selectorColor = Yuyu.accent,
                    containerColor = Yuyu.canvas,
                    timeSelectorSelectedContainerColor = Yuyu.accentSoft,
                    timeSelectorSelectedContentColor = Yuyu.accentText,
                    timeSelectorUnselectedContainerColor = Yuyu.muted,
                    timeSelectorUnselectedContentColor = Yuyu.text,
                )
            )
        }
    }
}
