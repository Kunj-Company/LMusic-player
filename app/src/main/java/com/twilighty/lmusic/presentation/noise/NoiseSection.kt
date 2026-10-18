package com.twilighty.lmusic.presentation.noise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.noise.BinauralMode
import com.twilighty.lmusic.data.noise.NoiseGroup
import com.twilighty.lmusic.data.noise.NoiseKind
import com.twilighty.lmusic.data.noise.NoisePlayState
import com.twilighty.lmusic.data.noise.NoiseSettings
import com.twilighty.lmusic.data.noise.dsp.NoiseMixer
import com.twilighty.lmusic.presentation.lofi.LofiSlider
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonSize
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuChip
import com.twilighty.lmusic.presentation.yuyu.YuyuDialog
import com.twilighty.lmusic.presentation.yuyu.YuyuDivider
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuSegmented
import com.twilighty.lmusic.presentation.yuyu.YuyuSettingRow
import com.twilighty.lmusic.presentation.yuyu.YuyuSwitch
import com.twilighty.lmusic.presentation.yuyu.YuyuTextField
import com.twilighty.lmusic.presentation.yuyu.YuyuType

/** "Playing · Deep work", "Under music", "Off". */
fun NoisePlayState.label(settings: NoiseSettings): String = when {
    underMusic && sounding -> "Playing softly under your music"
    underMusic -> "On · sounds while music plays"
    sounding -> "Playing · ${settings.presetName ?: settings.summary}"
    else -> "Off"
}

/**
 * The noise mixer: presets, up to three sounds with their own volume, tone, swell, fade-in,
 * under-music mode and optional binaural tones. Used on Focus and in the Sleep space.
 * [showFocusOptions] adds the focus-session behaviour switches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoiseSection(
    modifier: Modifier = Modifier,
    showFocusOptions: Boolean = false,
    showPlayButton: Boolean = true,
    viewModel: NoiseViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val focusOptions by viewModel.focusOptions.collectAsStateWithLifecycle()
    var more by rememberSaveable { mutableStateOf(false) }
    var saveOpen by remember { mutableStateOf(false) }

    YuyuCard(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                YuyuOverline("Noise")
                Text(state.label(settings), style = YuyuType.meta, color = Yuyu.textMuted, maxLines = 1)
            }
            if (showPlayButton) {
                YuyuButton(
                    text = if (state.active) "Pause" else "Play",
                    onClick = viewModel::togglePlay,
                    style = if (state.active) YuyuButtonStyle.Primary else YuyuButtonStyle.Accent,
                    size = YuyuButtonSize.Small,
                    icon = rememberVectorPainter(if (state.active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow),
                    enabled = state.active || settings.layers.isNotEmpty(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Presets", style = YuyuType.label, color = Yuyu.text)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { p ->
                YuyuChip(text = p.name, selected = settings.presetName == p.name, onClick = { viewModel.applyPreset(p) })
            }
            YuyuChip(text = "Save as preset", selected = false, onClick = { saveOpen = true })
        }
        val userPreset = presets.firstOrNull { !it.builtIn && it.name == settings.presetName }
        if (userPreset != null) {
            YuyuButton(
                text = "Delete “${userPreset.name}”",
                onClick = { viewModel.deletePreset(userPreset.name) },
                style = YuyuButtonStyle.Quiet,
                size = YuyuButtonSize.Small,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("Sounds · up to ${NoiseMixer.MAX_LAYERS}", style = YuyuType.label, color = Yuyu.text)
        NoiseGroup.entries.forEach { group ->
            Spacer(Modifier.height(10.dp))
            Text(group.label, style = YuyuType.caption, color = Yuyu.textMuted)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoiseKind.entries.filter { it.group == group }.forEach { kind ->
                    YuyuChip(
                        text = kind.label,
                        selected = settings.layers.any { it.kind == kind },
                        onClick = { viewModel.toggleLayer(kind) }
                    )
                }
            }
        }
        if (settings.layers.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            settings.layers.forEach { layer ->
                LofiSlider(
                    label = layer.kind.label,
                    value = layer.volume,
                    onValueChange = { viewModel.setLayerVolume(layer.kind, it) },
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Pick a sound to start", style = YuyuType.caption, color = Yuyu.textMuted)
        }
        LofiSlider(
            label = "Tone",
            value = settings.tone,
            onValueChange = { viewModel.setTone(it) },
            valueRange = -1f..1f,
            valueText = when {
                settings.tone < -0.15f -> "Darker"
                settings.tone > 0.15f -> "Brighter"
                else -> "Neutral"
            },
        )
        LofiSlider(label = "Volume", value = settings.masterVolume, onValueChange = { viewModel.setMaster(it) })
        YuyuSettingRow(
            title = "Natural movement",
            subtitle = "A gentle, slow swell",
            trailing = { YuyuSwitch(checked = settings.swell, onCheckedChange = { viewModel.setSwell(it) }) },
        )

        YuyuDivider()
        YuyuSettingRow(
            title = if (more) "Fewer options" else "More options",
            subtitle = "Fade-in, under music, binaural tones",
            icon = rememberVectorPainter(if (more) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore),
            onClick = { more = !more },
        )
        if (more) {
            Text("Fade in", style = YuyuType.label, color = Yuyu.text)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoiseSettings.FADE_IN_CHOICES.forEach { s ->
                    YuyuChip(
                        text = if (s == 0) "Off" else "$s s",
                        selected = settings.fadeInSeconds == s,
                        onClick = { viewModel.setFadeIn(s) }
                    )
                }
            }
            YuyuSettingRow(
                title = "Play under music",
                subtitle = "Softly, only while your music plays",
                trailing = { YuyuSwitch(checked = settings.underMusic, onCheckedChange = { viewModel.setUnderMusic(it) }) },
            )
            YuyuDivider()
            Spacer(Modifier.height(12.dp))
            Text("Binaural tones (headphones)", style = YuyuType.label, color = Yuyu.text)
            Text(
                "Some people find these help; research is mixed. Use headphones.",
                style = YuyuType.caption,
                color = Yuyu.textMuted,
            )
            Spacer(Modifier.height(10.dp))
            YuyuSegmented(
                options = BinauralMode.entries.map { it.label },
                selectedIndex = settings.binaural.ordinal,
                onSelect = { viewModel.setBinaural(BinauralMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (settings.binaural != BinauralMode.Off) {
                Spacer(Modifier.height(8.dp))
                Text("${settings.binaural.blurb} · carrier tone", style = YuyuType.caption, color = Yuyu.textMuted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoiseSettings.CARRIERS.forEach { hz ->
                        YuyuChip(text = "$hz Hz", selected = settings.carrierHz == hz, onClick = { viewModel.setCarrier(hz) })
                    }
                }
            }
        }
        if (showFocusOptions) {
            Spacer(Modifier.height(8.dp))
            YuyuDivider()
            YuyuSettingRow(
                title = "Stop at end of session",
                subtitle = "Fades out when the last round ends",
                trailing = {
                    YuyuSwitch(
                        checked = focusOptions.stopAtSessionEnd,
                        onCheckedChange = { viewModel.setFocusOptions(focusOptions.copy(stopAtSessionEnd = it)) }
                    )
                },
            )
            YuyuSettingRow(
                title = "Keep playing through breaks",
                subtitle = if (focusOptions.keepThroughBreaks) "Noise carries on during breaks" else "Pauses during breaks",
                trailing = {
                    YuyuSwitch(
                        checked = focusOptions.keepThroughBreaks,
                        onCheckedChange = { viewModel.setFocusOptions(focusOptions.copy(keepThroughBreaks = it)) }
                    )
                },
            )
        }
    }

    if (saveOpen) {
        SavePresetDialog(
            initial = settings.presetName ?: settings.summary,
            onDismiss = { saveOpen = false },
            onSave = { name -> viewModel.savePreset(name); saveOpen = false },
        )
    }
}

@Composable
private fun SavePresetDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    YuyuDialog(
        onDismissRequest = onDismiss,
        title = "Save preset",
        actions = {
            YuyuButton(text = "Cancel", onClick = onDismiss, style = YuyuButtonStyle.Quiet)
            YuyuButton(text = "Save", onClick = { onSave(name) }, enabled = name.isNotBlank())
        }
    ) {
        YuyuTextField(value = name, onValueChange = { name = it.take(40) }, label = "Name")
    }
}
