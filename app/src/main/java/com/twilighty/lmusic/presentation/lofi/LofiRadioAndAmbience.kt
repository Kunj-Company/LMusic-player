package com.twilighty.lmusic.presentation.lofi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.lofi.AmbienceKind
import com.twilighty.lmusic.data.lofi.LofiSource
import com.twilighty.lmusic.data.lofi.LofiState
import com.twilighty.lmusic.data.lofi.RadioStation
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuActionRow
import com.twilighty.lmusic.presentation.yuyu.YuyuArt
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonSize
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuChip
import com.twilighty.lmusic.presentation.yuyu.YuyuEmptyState
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuSectionHeader
import com.twilighty.lmusic.presentation.yuyu.YuyuSheetHandle
import com.twilighty.lmusic.presentation.yuyu.YuyuSheetShape
import com.twilighty.lmusic.presentation.yuyu.YuyuSongRow
import com.twilighty.lmusic.presentation.yuyu.YuyuStatus
import com.twilighty.lmusic.presentation.yuyu.YuyuStatusChip
import com.twilighty.lmusic.presentation.yuyu.YuyuTextField
import com.twilighty.lmusic.presentation.yuyu.YuyuType

// ==================================================================== Radio

internal fun LazyListScope.radioItems(
    state: LofiState,
    browse: RadioBrowseState,
    favourites: List<RadioStation>,
    vm: LofiViewModel,
) {
    val favIds = favourites.mapTo(HashSet()) { it.id }
    item(key = "radio_loader") { LaunchedEffect(Unit) { vm.ensureRadioLoaded() } }
    if (state.source == LofiSource.Radio) {
        item(key = "radio_now") { RadioNowPlaying(state, vm) }
    }
    item(key = "radio_search") { RadioSearch(vm) }
    if (favourites.isNotEmpty()) {
        item(key = "radio_favs") { StationSection("Favourites", favourites, favIds, state, vm) }
    }
    item(key = "radio_picks") { StationSection("Picks", vm.curatedStations, favIds, state, vm) }
    item(key = "radio_browse") {
        Column(Modifier.padding(horizontal = 16.dp)) {
            YuyuSectionHeader(
                title = "Tagged “${browse.tag}”",
                actionLabel = if (browse.loaded) "Refresh" else null,
                onAction = { vm.loadTag(browse.tag, force = true) }
            )
            Spacer(Modifier.height(4.dp))
            when {
                browse.loading -> Text("Loading stations…", style = YuyuType.meta, color = Yuyu.textMuted, modifier = Modifier.padding(vertical = 12.dp))
                browse.error != null -> YuyuEmptyState(
                    title = "No connection",
                    body = browse.error,
                    icon = rememberVectorPainter(Icons.Rounded.Radio),
                    actions = { YuyuButton("Try again", onClick = { vm.loadTag(browse.tag, force = true) }, style = YuyuButtonStyle.Secondary, size = YuyuButtonSize.Small) }
                )
                browse.loaded && browse.stations.isEmpty() -> YuyuEmptyState(
                    title = "Nothing here",
                    body = "No working stations use this tag. Try lofi or chillhop.",
                    icon = rememberVectorPainter(Icons.Rounded.Radio)
                )
            }
        }
    }
    if (!browse.loading && browse.error == null) {
        items(browse.stations, key = { "rb_${it.id}" }) { st ->
            StationRow(st, st.id in favIds, state, vm, Modifier.padding(horizontal = 16.dp))
        }
    }
    item(key = "radio_credit") {
        Text(
            "Station directory by radio-browser.info. Streams come from each station.",
            style = YuyuType.caption, color = Yuyu.textMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RadioNowPlaying(state: LofiState, vm: LofiViewModel) {
    val st = state.station ?: return
    YuyuCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), background = Yuyu.subtle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            YuyuArt(model = st.favicon, title = st.name, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", style = YuyuType.overline, color = Yuyu.accentText)
                Text(st.name, style = YuyuType.title, color = Yuyu.text, maxLines = 1)
                Text(
                    when {
                        state.isBuffering -> "Connecting…"
                        state.nowPlaying != null -> state.nowPlaying
                        state.isPlaying -> "Live"
                        else -> "Paused"
                    },
                    style = YuyuType.meta, color = Yuyu.textMuted, maxLines = 2
                )
            }
            YuyuIconButton(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                onClick = vm::toggleRadioPlayback,
                style = YuyuIconButtonStyle.Filled
            )
            YuyuIconButton(imageVector = Icons.Rounded.Stop, contentDescription = "Stop", onClick = vm::stop, style = YuyuIconButtonStyle.Tonal)
        }
    }
}

@Composable
private fun RadioSearch(vm: LofiViewModel) {
    val browse by vm.browse.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        YuyuTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search by tag, like jazzhop",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.loadTag(query); focus.clearFocus() }),
            leading = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Yuyu.textMuted, modifier = Modifier.size(20.dp)) }
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (tag in vm.suggestedTags) {
                YuyuChip(text = tag, selected = browse.tag == tag, onClick = { query = ""; vm.loadTag(tag) })
            }
        }
    }
}

@Composable
private fun StationSection(title: String, stations: List<RadioStation>, favIds: Set<String>, state: LofiState, vm: LofiViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        YuyuSectionHeader(title)
        Spacer(Modifier.height(4.dp))
        for (st in stations) StationRow(st, st.id in favIds, state, vm)
    }
}

@Composable
private fun StationRow(station: RadioStation, isFav: Boolean, state: LofiState, vm: LofiViewModel, modifier: Modifier = Modifier) {
    val isCurrent = state.source == LofiSource.Radio && state.station?.id == station.id
    val subtitle = buildList {
        if (station.bitrate > 0) add("${station.bitrate} kbps")
        station.country?.let { add(it) }
        station.tags?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }.joinToString(" · ").ifEmpty { "Live radio" }
    YuyuSongRow(
        title = station.name,
        subtitle = if (isCurrent && state.isPlaying) "Playing · $subtitle" else subtitle,
        artModel = station.favicon,
        artKey = station.name,
        isCurrent = isCurrent,
        modifier = modifier,
        onClick = { vm.playStation(station) },
        trailing = {
            if (isCurrent && state.isPlaying) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Yuyu.accentText, modifier = Modifier.size(20.dp))
            }
            YuyuIconButton(
                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFav) "Remove from favourites" else "Add to favourites",
                onClick = { vm.toggleFavourite(station) },
                tint = if (isFav) Yuyu.accentText else null,
                size = 40.dp,
                iconSize = 20.dp
            )
        }
    )
}

// ==================================================================== Ambience

internal fun LazyListScope.ambienceItems(vm: LofiViewModel) {
    item(key = "amb_intro") {
        val sounding by vm.ambienceSounding.collectAsStateWithLifecycle()
        val layers by vm.ambienceLayers.collectAsStateWithLifecycle()
        val anyOn = layers.values.any { it.enabled }
        YuyuCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), background = Yuyu.subtle) {
            Text("Ambience over your music", style = YuyuType.h3, color = Yuyu.text)
            Spacer(Modifier.height(4.dp))
            Text(
                "Mix rain, café, fireplace or waves over any song or lofi radio. It plays only while music plays.",
                style = YuyuType.meta, color = Yuyu.textMuted
            )
            Spacer(Modifier.height(10.dp))
            YuyuStatusChip(
                text = when {
                    !anyOn -> "Off"
                    sounding -> "Playing over your music"
                    else -> "Waiting for music"
                },
                status = when {
                    !anyOn -> YuyuStatus.Neutral
                    sounding -> YuyuStatus.Success
                    else -> YuyuStatus.Warning
                }
            )
        }
    }
    item(key = "amb_layers") {
        val layers by vm.ambienceLayers.collectAsStateWithLifecycle()
        Column(Modifier.padding(horizontal = 16.dp)) {
            AmbienceLayerControls(layers, vm::setAmbienceEnabled, vm::setAmbienceVolume)
            if (layers.values.any { it.enabled }) {
                Spacer(Modifier.height(8.dp))
                YuyuButton("Turn all off", onClick = vm::ambienceAllOff, style = YuyuButtonStyle.Secondary, size = YuyuButtonSize.Small)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "The generator has its own rain and café sliders under Generate › Texture.",
                style = YuyuType.caption, color = Yuyu.textMuted
            )
        }
    }
}

/**
 * "Ambience" row for the song options sheet; opens a small sheet with the four layers.
 * Self-contained, so the host sheet only needs one line.
 */
@Composable
fun LofiSongActions(modifier: Modifier = Modifier, viewModel: AmbienceViewModel = hiltViewModel()) {
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val on = AmbienceKind.entries.filter { layers[it]?.enabled == true }
    YuyuActionRow(
        text = "Ambience",
        subtitle = if (on.isEmpty()) "Rain, café, fireplace or waves over your music" else on.joinToString(", ") { it.label },
        icon = rememberVectorPainter(Icons.Rounded.Waves),
        modifier = modifier,
        trailing = if (on.isNotEmpty()) ({ YuyuStatusChip(text = "On", status = YuyuStatus.Accent) }) else null,
        onClick = { open = true }
    )
    if (open) AmbienceSheet(viewModel = viewModel, onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbienceSheet(onDismiss: () -> Unit, viewModel: AmbienceViewModel = hiltViewModel()) {
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val sounding by viewModel.sounding.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = YuyuSheetShape,
        containerColor = Yuyu.canvas,
        dragHandle = { YuyuSheetHandle() },
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ambience", style = YuyuType.h2, color = Yuyu.text)
                    Text(
                        if (sounding) "Playing over your music" else "Plays while music plays",
                        style = YuyuType.meta, color = Yuyu.textMuted
                    )
                }
                if (layers.values.any { it.enabled }) {
                    YuyuButton("All off", onClick = viewModel::allOff, style = YuyuButtonStyle.Quiet, size = YuyuButtonSize.Small)
                }
            }
            Spacer(Modifier.height(8.dp))
            AmbienceLayerControls(layers, viewModel::setEnabled, viewModel::setVolume)
        }
    }
}
