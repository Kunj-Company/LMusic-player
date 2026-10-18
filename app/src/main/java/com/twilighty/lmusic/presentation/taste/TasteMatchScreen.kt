package com.twilighty.lmusic.presentation.taste

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.taste.TasteMatchHistoryEntry
import com.twilighty.lmusic.presentation.haptics.LocalLMusicHaptics
import com.twilighty.lmusic.presentation.viewmodel.PlayerViewModel
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonSize
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuCallout
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuChip
import com.twilighty.lmusic.presentation.yuyu.YuyuEmptyState
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuSectionHeader
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuSongRow
import com.twilighty.lmusic.presentation.yuyu.YuyuTextField
import com.twilighty.lmusic.presentation.yuyu.YuyuTile
import com.twilighty.lmusic.presentation.yuyu.YuyuTopBar
import com.twilighty.lmusic.presentation.yuyu.YuyuType
import java.text.DateFormat
import java.util.Date

@Composable
fun TasteMatchScreen(
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel,
    bottomInset: Dp = 0.dp,
    viewModel: TasteMatchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.mode != TasteMode.Home) { viewModel.backToHome() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Yuyu.canvas)
    ) {
        when (state.mode) {
            TasteMode.Home -> HomeContent(state, viewModel, onBack, bottomInset)
            TasteMode.Scan -> ScanContent(state, viewModel, bottomInset)
            TasteMode.Result -> state.result?.let { ResultContent(it, viewModel, playerViewModel, bottomInset) }
                ?: HomeContent(state, viewModel, onBack, bottomInset)
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
private fun HomeContent(state: TasteUiState, viewModel: TasteMatchViewModel, onBack: () -> Unit, bottomInset: Dp) {
    val context = LocalContext.current
    var editingName by rememberSaveable { mutableStateOf(false) }
    var pasteOpen by rememberSaveable { mutableStateOf(false) }
    var pasteText by rememberSaveable { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.openScanner()
        else Toast.makeText(context, "Camera access is off. You can paste a code instead.", Toast.LENGTH_SHORT).show()
    }
    val onScan = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.openScanner()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomInset + 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "top") {
            YuyuTopBar(
                title = "Taste match",
                kicker = "Scan a friend, see how your music lines up",
                navigationIcon = {
                    YuyuIconButton(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack)
                }
            )
        }
        if (state.loading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Yuyu.accent)
                }
            }
            return@LazyColumn
        }

        if (!state.nameConfirmed || editingName) {
            item(key = "name") {
                NameCard(
                    initial = state.name,
                    onSave = { viewModel.setName(it); editingName = false },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (!state.fromListening && !state.isEmpty) {
            item(key = "library-note") {
                YuyuCallout(
                    boldText = "Based on your library.",
                    text = "Play some music and your code will reflect what you actually listen to.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "mine") {
            if (state.isEmpty) {
                YuyuEmptyState(
                    title = "Nothing to match yet",
                    body = "Add some music and play a few songs, then come back for your taste code.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                MyTasteCard(
                    state = state,
                    onShare = { state.myShareText?.let { shareCode(context, state.name, it) } },
                    onEditName = { editingName = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "actions") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                YuyuButton(
                    text = "Scan a friend",
                    onClick = onScan,
                    style = YuyuButtonStyle.Accent,
                    size = YuyuButtonSize.Large,
                    icon = rememberVectorPainter(Icons.Rounded.QrCodeScanner),
                    enabled = !state.isEmpty,
                    modifier = Modifier.fillMaxWidth()
                )
                YuyuButton(
                    text = if (pasteOpen) "Hide paste box" else "Paste code",
                    onClick = { pasteOpen = !pasteOpen },
                    style = YuyuButtonStyle.Secondary,
                    enabled = !state.isEmpty,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (pasteOpen) {
            item(key = "paste") {
                PasteCard(
                    value = pasteText,
                    onValueChange = { pasteText = it; viewModel.clearError() },
                    error = state.error,
                    busy = state.matching,
                    onMatch = { if (viewModel.onCode(pasteText)) pasteText = "" },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else if (state.error != null) {
            item(key = "error") {
                YuyuCallout(boldText = "Heads up.", text = state.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        if (state.history.isNotEmpty()) {
            item(key = "history-h") {
                YuyuSectionHeader("Past matches", modifier = Modifier.padding(horizontal = 16.dp))
            }
            items(state.history, key = { "h" + it.timestamp + it.name }) { entry ->
                HistoryRow(entry, Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun MyTasteCard(state: TasteUiState, onShare: () -> Unit, onEditName: () -> Unit, modifier: Modifier = Modifier) {
    YuyuCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            YuyuOverline("My taste")
            Spacer(Modifier.height(12.dp))
            state.qr?.let { qr ->
                Box(
                    Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(YuyuShapes.lg)
                        .background(Color.White)
                        .padding(6.dp)
                ) {
                    Image(
                        bitmap = qr,
                        contentDescription = "Your taste code",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.name,
                    style = YuyuType.h2,
                    color = Yuyu.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                YuyuIconButton(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit name",
                    onClick = onEditName,
                    size = 36.dp,
                    iconSize = 18.dp
                )
            }
            if (state.topArtists.isNotEmpty()) {
                Text(
                    state.topArtists.joinToString(" · "),
                    style = YuyuType.meta,
                    color = Yuyu.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            YuyuButton(
                text = "Share code as text",
                onClick = onShare,
                style = YuyuButtonStyle.Secondary,
                size = YuyuButtonSize.Small,
                icon = rememberVectorPainter(Icons.Rounded.Share)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Works offline. Only artist names, song titles and your name are in the code.",
                style = YuyuType.caption,
                color = Yuyu.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NameCard(initial: String, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var name by rememberSaveable { mutableStateOf(initial) }
    YuyuCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text("What should friends see?", style = YuyuType.title, color = Yuyu.text)
        Spacer(Modifier.height(10.dp))
        YuyuTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            placeholder = "Your name",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        YuyuButton(text = "Save", onClick = { onSave(name) }, size = YuyuButtonSize.Small)
    }
}

@Composable
private fun PasteCard(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    busy: Boolean,
    onMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YuyuCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        YuyuTextField(
            value = value,
            onValueChange = onValueChange,
            label = "Friend's code",
            placeholder = "LMT1:…",
            helper = error ?: "Paste the message your friend sent. The code starts with LMT1:",
            isError = error != null,
            singleLine = false,
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        YuyuButton(
            text = if (busy) "Matching…" else "Match",
            onClick = onMatch,
            enabled = value.isNotBlank() && !busy,
            size = YuyuButtonSize.Small
        )
    }
}

@Composable
private fun HistoryRow(entry: TasteMatchHistoryEntry, modifier: Modifier = Modifier) {
    val date = remember(entry.timestamp) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestamp)) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(YuyuShapes.md)
            .border(1.dp, Yuyu.border, YuyuShapes.md)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = YuyuType.title, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$date · ${com.twilighty.lmusic.data.taste.TasteMatcher.labelFor(entry.percent)}", style = YuyuType.caption, color = Yuyu.textMuted)
        }
        Text("${entry.percent}%", style = YuyuType.h3, color = Yuyu.text)
    }
}

// ---------------------------------------------------------------- Scan

@Composable
private fun ScanContent(state: TasteUiState, viewModel: TasteMatchViewModel, bottomInset: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TasteQrScanner(onCode = viewModel::onCode, modifier = Modifier.fillMaxSize())

        // Rounded viewfinder
        Box(
            Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(3.dp, Color.White, YuyuShapes.xxl)
        )

        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YuyuIconButton(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close scanner",
                onClick = viewModel::backToHome,
                style = YuyuIconButtonStyle.Tonal
            )
            Spacer(Modifier.weight(1f))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = bottomInset)
                .padding(24.dp)
                .clip(YuyuShapes.xl)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.matching) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(8.dp))
                Text("Matching…", style = YuyuType.title, color = Color.White)
            } else {
                Text("Point at a friend's taste code", style = YuyuType.title, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    state.error ?: "On their phone: Taste match → My taste",
                    style = YuyuType.caption,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Result

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultContent(
    result: TasteResultUi,
    viewModel: TasteMatchViewModel,
    playerViewModel: PlayerViewModel,
    bottomInset: Dp,
) {
    val context = LocalContext.current
    val haptics = LocalLMusicHaptics.current
    val count = remember(result) { Animatable(0f) }
    LaunchedEffect(result) {
        count.snapTo(0f)
        count.animateTo(result.percent.toFloat(), tween(durationMillis = 1400, easing = FastOutSlowInEasing))
        haptics.celebration()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomInset + 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "top") {
            YuyuTopBar(
                title = "Taste match",
                kicker = "You & ${result.friendName}",
                navigationIcon = {
                    YuyuIconButton(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = viewModel::backToHome)
                }
            )
        }
        item(key = "score") {
            YuyuTile(background = Yuyu.accentSoft, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    YuyuOverline("Match", color = Yuyu.accentText)
                    Text(
                        "${count.value.toInt()}%",
                        style = YuyuType.display(96.sp),
                        color = Yuyu.text
                    )
                    Text(result.label, style = YuyuType.h2, color = Yuyu.accentText, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (result.usedSmart) "Based on artists, genres and moods" else "Based on the artists you both play",
                        style = YuyuType.caption,
                        color = Yuyu.textMuted
                    )
                }
            }
        }

        item(key = "shared") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                YuyuSectionHeader("Artists you share")
                if (result.sharedArtists.isEmpty()) {
                    Text(
                        if (result.friendTopArtists.isEmpty()) "No artists in common yet."
                        else "None yet. ${result.friendName} is into ${result.friendTopArtists.take(3).joinToString(", ")}.",
                        style = YuyuType.meta,
                        color = Yuyu.textMuted
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.sharedArtists.take(15).forEachIndexed { i, a ->
                            YuyuChip(text = a.name, selected = i < 3, onClick = {})
                        }
                    }
                }
            }
        }

        loveSection(result, playerViewModel)

        if (result.songsMissing.isNotEmpty()) {
            item(key = "missing-h") {
                YuyuSectionHeader(
                    "Find these",
                    actionLabel = "Copy list",
                    onAction = { copyMissing(context, result) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "missing-sub") {
                Text(
                    "${result.friendName}'s favourites that aren't in your library",
                    style = YuyuType.meta,
                    color = Yuyu.textMuted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            itemsIndexed(result.songsMissing, key = { i, s -> "m$i${s.title}" }) { _, s ->
                YuyuSongRow(
                    title = s.title,
                    subtitle = s.artist,
                    artKey = s.artist.ifBlank { s.title },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "footer") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Songs ${result.friendName} would love from you show up on their phone when they scan your code.",
                    style = YuyuType.caption,
                    color = Yuyu.textMuted
                )
                YuyuButton(
                    text = "Scan another friend",
                    onClick = viewModel::openScanner,
                    style = YuyuButtonStyle.Secondary,
                    icon = rememberVectorPainter(Icons.Rounded.QrCodeScanner),
                    modifier = Modifier.fillMaxWidth()
                )
                YuyuButton(text = "Done", onClick = viewModel::backToHome, style = YuyuButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun LazyListScope.loveSection(result: TasteResultUi, playerViewModel: PlayerViewModel) {
    if (result.songsToPlay.isEmpty()) return
    item(key = "love-h") {
        Column(Modifier.padding(horizontal = 16.dp)) {
            YuyuSectionHeader("Songs you'd love from them")
            Text(
                "${result.friendName}'s favourites that you already have",
                style = YuyuType.meta,
                color = Yuyu.textMuted
            )
            Spacer(Modifier.height(10.dp))
            YuyuButton(
                text = "Play all",
                onClick = {
                    playerViewModel.playSongs(result.songsToPlay, result.songsToPlay.first(), "Taste match · ${result.friendName}")
                },
                style = YuyuButtonStyle.Primary,
                size = YuyuButtonSize.Small,
                icon = rememberVectorPainter(Icons.Rounded.PlayArrow)
            )
        }
    }
    items(result.songsToPlay, key = { "p" + it.id }) { song ->
        YuyuSongRow(
            title = song.title,
            subtitle = song.displayArtist,
            artModel = song.albumArtUriString,
            artKey = song.album.ifBlank { song.title },
            onClick = { playerViewModel.playSongs(result.songsToPlay, song, "Taste match · ${result.friendName}") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun shareCode(context: Context, name: String, code: String) {
    val text = "Let's compare music taste on LMusic. Open Taste match, tap Paste code and paste this:\n\n$code"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "$name's LMusic taste code")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Share taste code").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun copyMissing(context: Context, result: TasteResultUi) {
    val text = buildString {
        append("Songs from ").append(result.friendName).append(" to find:\n")
        result.songsMissing.forEach { append("• ").append(it.title).append(" — ").append(it.artist).append('\n') }
    }.trimEnd()
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Songs to find", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}
