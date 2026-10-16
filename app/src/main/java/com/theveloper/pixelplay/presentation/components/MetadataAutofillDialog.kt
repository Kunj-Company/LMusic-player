package com.theveloper.pixelplay.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.autofill.AutofillCandidate
import com.theveloper.pixelplay.data.autofill.AutofillField
import com.theveloper.pixelplay.data.autofill.AutofillSource
import com.theveloper.pixelplay.data.autofill.CoverArt
import com.theveloper.pixelplay.data.autofill.MetadataFields
import com.theveloper.pixelplay.data.autofill.MusicBrainzAutofill
import com.theveloper.pixelplay.data.autofill.OfflineMetadataGuesser
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.repository.MusicRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MetadataAutofillEntryPoint {
    fun musicRepository(): MusicRepository
}

/** What the edit sheet receives when the user taps "Fill fields". */
data class AutofillResult(
    val fields: MetadataFields,
    val selected: Set<AutofillField>,
    val coverArt: CoverArt?,
    val candidate: AutofillCandidate,
)

private sealed interface OnlineState {
    data object Idle : OnlineState
    data object Loading : OnlineState
    data class Loaded(val candidates: List<AutofillCandidate>) : OnlineState
    data class Failed(val offline: Boolean) : OnlineState
}

private val fieldLabels = mapOf(
    AutofillField.TITLE to R.string.song_field_title,
    AutofillField.ARTIST to R.string.song_field_artist,
    AutofillField.ALBUM to R.string.song_field_album,
    AutofillField.ALBUM_ARTIST to R.string.lmusic_field_album_artist,
    AutofillField.YEAR to R.string.lmusic_field_year,
    AutofillField.GENRE to R.string.song_field_genre,
    AutofillField.TRACK to R.string.song_field_track_number,
    AutofillField.DISC to R.string.song_field_disc_number,
    AutofillField.COMPOSER to R.string.lmusic_field_composer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataAutofillDialog(
    song: Song,
    current: MetadataFields,
    onDismiss: () -> Unit,
    onApply: (AutofillResult) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var queryTitle by remember { mutableStateOf(current.title) }
        var queryArtist by remember { mutableStateOf(current.artist) }
        var queryAlbum by remember { mutableStateOf(current.album) }

        var offlineCandidate by remember { mutableStateOf<AutofillCandidate?>(null) }
        var online by remember { mutableStateOf<OnlineState>(OnlineState.Idle) }
        var searchJob by remember { mutableStateOf<Job?>(null) }
        var selected by remember { mutableStateOf<AutofillCandidate?>(null) }

        fun runSearch() {
            searchJob?.cancel()
            searchJob = scope.launch {
                online = OnlineState.Loading
                online = try {
                    OnlineState.Loaded(
                        MusicBrainzAutofill.search(
                            title = queryTitle,
                            artist = queryArtist,
                            album = queryAlbum,
                            durationMs = song.duration.takeIf { it > 0 }
                        )
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Autofill search failed")
                    OnlineState.Failed(offline = e is java.io.IOException)
                }
            }
        }

        LaunchedEffect(song.id) {
            offlineCandidate = withContext(Dispatchers.IO) {
                runCatching { OfflineMetadataGuesser.guess(song, loadFolderMates(context, song)) }
                    .onFailure { Timber.w(it, "Offline autofill failed") }
                    .getOrNull()
            }
            // Untagged files: search with what the filename and folder suggest.
            offlineCandidate?.fields?.let { guess ->
                if (queryTitle.isBlank()) queryTitle = guess.title
                if (queryArtist.isBlank()) queryArtist = guess.artist
                if (queryAlbum.isBlank()) queryAlbum = guess.album
            }
            if (queryTitle.isNotBlank()) runSearch() else online = OnlineState.Loaded(emptyList())
        }

        val chosen = selected
        if (chosen != null) {
            BackHandler { selected = null }
            CandidateDetail(
                candidate = chosen,
                current = current,
                songHasArt = !song.albumArtUriString.isNullOrBlank(),
                onBack = { selected = null },
                onApply = { result ->
                    onApply(result)
                    onDismiss()
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.lmusic_autofill_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.lmusic_autofill_close))
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = WindowInsets.statusBars
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 4.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.lmusic_autofill_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = queryTitle,
                                onValueChange = { queryTitle = it },
                                label = { Text(stringResource(R.string.lmusic_autofill_search_title)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = queryArtist,
                                onValueChange = { queryArtist = it },
                                label = { Text(stringResource(R.string.lmusic_autofill_search_artist)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = queryAlbum,
                                onValueChange = { queryAlbum = it },
                                label = { Text(stringResource(R.string.lmusic_autofill_search_album)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = { runSearch() },
                                enabled = queryTitle.isNotBlank() && online !is OnlineState.Loading,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.lmusic_autofill_search))
                            }
                        }
                    }

                    offlineCandidate?.let { guess ->
                        item {
                            SectionHeader(
                                title = stringResource(R.string.lmusic_autofill_from_files),
                                subtitle = stringResource(R.string.lmusic_autofill_from_files_desc)
                            )
                        }
                        item {
                            CandidateRow(candidate = guess, onClick = { selected = guess })
                        }
                    }

                    item {
                        SectionHeader(
                            title = stringResource(R.string.lmusic_autofill_online),
                            subtitle = stringResource(R.string.lmusic_autofill_online_desc)
                        )
                    }
                    when (val state = online) {
                        OnlineState.Idle, OnlineState.Loading -> item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = stringResource(R.string.lmusic_autofill_searching),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is OnlineState.Failed -> item {
                            Text(
                                text = stringResource(R.string.lmusic_autofill_offline_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        is OnlineState.Loaded -> {
                            if (state.candidates.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.lmusic_autofill_no_results),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            } else {
                                items(state.candidates, key = { (it.recordingId ?: "") + (it.releaseId ?: "") }) { candidate ->
                                    CandidateRow(candidate = candidate, onClick = { selected = candidate })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CandidateRow(candidate: AutofillCandidate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (candidate.source == AutofillSource.FILES) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                candidate.thumbnailUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = candidate.fields.title.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = candidate.fields.artist.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val meta = listOfNotNull(
                candidate.fields.album.ifBlank { null },
                candidate.fields.year.ifBlank { null },
                candidate.releaseType,
                candidate.durationMs?.let { formatDuration(it) }
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (candidate.source == AutofillSource.MUSICBRAINZ) {
            ConfidenceChip(candidate.confidence)
        }
    }
}

@Composable
private fun ConfidenceChip(confidence: Int) {
    val strong = confidence >= 80
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (strong) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.lmusic_autofill_match, confidence),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (strong) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CandidateDetail(
    candidate: AutofillCandidate,
    current: MetadataFields,
    songHasArt: Boolean,
    onBack: () -> Unit,
    onApply: (AutofillResult) -> Unit,
) {
    var detailed by remember(candidate) { mutableStateOf(if (candidate.detailed) candidate else null) }
    var coverArt by remember(candidate) { mutableStateOf<CoverArt?>(null) }
    var coverBitmap by remember(candidate) { mutableStateOf<ImageBitmap?>(null) }
    var coverLoading by remember(candidate) { mutableStateOf(candidate.source == AutofillSource.MUSICBRAINZ) }
    var useCover by remember(candidate) { mutableStateOf(true) }
    var chosenFields by remember(candidate) { mutableStateOf<Set<AutofillField>?>(null) }

    LaunchedEffect(candidate) {
        if (candidate.source == AutofillSource.MUSICBRAINZ) {
            launch {
                val art = runCatching { MusicBrainzAutofill.loadCoverArt(candidate) }.getOrNull()
                coverArt = art
                coverBitmap = art?.let { cover ->
                    withContext(Dispatchers.Default) {
                        runCatching {
                            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeByteArray(cover.bytes, 0, cover.bytes.size, options)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
                coverLoading = false
                useCover = art != null
            }
            detailed = runCatching { MusicBrainzAutofill.loadDetails(candidate) }.getOrElse { candidate }
        }
    }

    val details = detailed
    val proposals = details?.fields
    val changedFields = remember(proposals, current) {
        if (proposals == null) emptyList() else AutofillField.entries.filter { field ->
            val newValue = field.valueOf(proposals).trim()
            newValue.isNotEmpty() && !newValue.equals(field.valueOf(current).trim(), ignoreCase = false)
        }
    }
    LaunchedEffect(changedFields) {
        if (chosenFields == null && proposals != null) {
            // Pre-tick everything that's new, but don't overwrite an existing title/artist
            // from a weak match.
            chosenFields = changedFields.filter { field ->
                val hasCurrent = field.valueOf(current).isNotBlank()
                !(hasCurrent && candidate.confidence < 70 && field in setOf(AutofillField.TITLE, AutofillField.ARTIST))
            }.toSet()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = details?.fields?.title ?: candidate.fields.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.lmusic_autofill_back))
                    }
                }
            )
        },
        bottomBar = {
            val selectedSet = chosenFields.orEmpty()
            val applyCover = useCover && coverArt != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp
                    )
            ) {
                Button(
                    onClick = {
                        val finalCandidate = details ?: candidate
                        onApply(
                            AutofillResult(
                                fields = finalCandidate.fields,
                                selected = selectedSet,
                                coverArt = if (applyCover) coverArt else null,
                                candidate = finalCandidate
                            )
                        )
                    },
                    enabled = details != null && (selectedSet.isNotEmpty() || applyCover),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.lmusic_autofill_apply))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (candidate.source == AutofillSource.MUSICBRAINZ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable(enabled = coverArt != null) { useCover = !useCover }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = coverBitmap
                            when {
                                bitmap != null -> Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                coverLoading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                else -> Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.lmusic_autofill_cover_art),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    coverLoading -> stringResource(R.string.lmusic_autofill_cover_loading)
                                    coverArt == null -> stringResource(R.string.lmusic_autofill_cover_none)
                                    else -> stringResource(if (songHasArt) R.string.lmusic_autofill_cover_replace else R.string.lmusic_autofill_cover_add)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = useCover && coverArt != null,
                            enabled = coverArt != null,
                            onCheckedChange = { useCover = it }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.lmusic_autofill_choose_fields),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (proposals == null) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.lmusic_autofill_loading_details),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (changedFields.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lmusic_autofill_nothing_new),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(changedFields, key = { it.name }) { field ->
                    val isChecked = chosenFields.orEmpty().contains(field)
                    FieldChangeRow(
                        label = stringResource(fieldLabels.getValue(field)),
                        currentValue = field.valueOf(current),
                        newValue = field.valueOf(proposals),
                        checked = isChecked,
                        onToggle = {
                            val set = chosenFields.orEmpty()
                            chosenFields = if (isChecked) set - field else set + field
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldChangeRow(
    label: String,
    currentValue: String,
    newValue: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(R.string.lmusic_autofill_current)}: ${currentValue.ifBlank { stringResource(R.string.lmusic_autofill_empty) }}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(R.string.lmusic_autofill_new)}: $newValue",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private suspend fun loadFolderMates(context: Context, song: Song): List<Song> {
    val folder = File(song.path).parent ?: return emptyList()
    return runCatching {
        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, MetadataAutofillEntryPoint::class.java)
            .musicRepository()
        repository.getAllSongsOnce().filter { File(it.path).parent == folder }
    }.getOrElse { emptyList() }
}
