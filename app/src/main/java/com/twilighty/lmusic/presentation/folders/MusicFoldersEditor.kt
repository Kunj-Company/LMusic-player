package com.twilighty.lmusic.presentation.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.twilighty.lmusic.presentation.components.FileExplorerDialog
import com.twilighty.lmusic.presentation.screens.SettingsItem
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuChip
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuTopBar
import com.twilighty.lmusic.presentation.yuyu.YuyuType
import java.io.File

/**
 * Include-list editor: quick-add chips, chosen folders with remove buttons, "Add folder"
 * (opens the folder explorer in pick mode) and the "Only these folders are scanned" note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicFoldersEditor(
    viewModel: MusicFoldersViewModel,
    modifier: Modifier = Modifier,
    canOpenPicker: Boolean = true,
    /** Settings keeps at least one folder: an empty list would mean "scan everything". */
    allowRemovingLast: Boolean = false,
) {
    val included by viewModel.includedFolders.collectAsStateWithLifecycle()
    val includedSet by viewModel.includedFolderSet.collectAsStateWithLifecycle()
    val quickAdd = remember { viewModel.quickAddFolders() }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(canOpenPicker) {
        if (canOpenPicker) viewModel.primeExplorer()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val availableQuickAdd = quickAdd.filter { qa -> included.none { it.equals(qa.path, ignoreCase = true) } }
        if (availableQuickAdd.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                YuyuOverline("Quick add")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableQuickAdd.forEach { folder ->
                        YuyuChip(
                            text = folder.label,
                            selected = false,
                            onClick = { viewModel.addFolder(folder.path) },
                            icon = rememberVectorPainter(Icons.Rounded.Add)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            YuyuOverline(if (included.isEmpty()) "No folders yet" else "Your music folders")
            if (included.isEmpty()) {
                Text(
                    "Add at least one folder to continue.",
                    style = YuyuType.meta,
                    color = Yuyu.textMuted
                )
            }
            included.forEach { path ->
                MusicFolderRow(
                    path = path,
                    removable = allowRemovingLast || included.size > 1,
                    onRemove = { viewModel.removeFolder(path) }
                )
            }
        }

        YuyuButton(
            text = "Add folder",
            onClick = {
                showPicker = true
                viewModel.openExplorer()
            },
            style = YuyuButtonStyle.Secondary,
            enabled = canOpenPicker,
            icon = rememberVectorPainter(Icons.Rounded.Add),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = Yuyu.textMuted, modifier = Modifier.size(16.dp))
            Text("Only these folders are scanned", style = YuyuType.caption, color = Yuyu.textMuted)
        }
    }

    MusicFolderPicker(
        viewModel = viewModel,
        visible = showPicker,
        pickedPaths = includedSet,
        onClose = {
            showPicker = false
            viewModel.onPickerClosed()
        }
    )
}

@Composable
private fun MusicFolderRow(path: String, removable: Boolean, onRemove: () -> Unit) {
    val name = remember(path) { File(path).name.ifEmpty { path } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Yuyu.border, YuyuShapes.lg)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Yuyu.accentSoft, YuyuShapes.md),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Yuyu.accentText, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = YuyuType.title, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                path,
                style = YuyuType.caption,
                color = Yuyu.textMuted,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis
            )
        }
        YuyuIconButton(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove $name",
            onClick = onRemove,
            enabled = removable
        )
    }
}

@Composable
private fun MusicFolderPicker(
    viewModel: MusicFoldersViewModel,
    visible: Boolean,
    pickedPaths: Set<String>,
    onClose: () -> Unit,
) {
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val children by viewModel.currentDirectoryChildren.collectAsStateWithLifecycle()
    val storages by viewModel.availableStorages.collectAsStateWithLifecycle()
    val storageIndex by viewModel.selectedStorageIndex.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDirectories.collectAsStateWithLifecycle()
    val isPriming by viewModel.isExplorerPriming.collectAsStateWithLifecycle()
    val isReady by viewModel.isExplorerReady.collectAsStateWithLifecycle()
    val isResolved by viewModel.isCurrentDirectoryResolved.collectAsStateWithLifecycle()
    val root = viewModel.explorerRoot()

    FileExplorerDialog(
        visible = visible,
        currentPath = currentPath,
        directoryChildren = children,
        availableStorages = storages,
        selectedStorageIndex = storageIndex,
        isLoading = isLoading,
        isPriming = isPriming,
        isReady = isReady,
        isCurrentDirectoryResolved = isResolved,
        isAtRoot = viewModel.isAtRoot(),
        rootDirectory = root,
        onNavigateTo = viewModel::loadDirectory,
        onNavigateUp = viewModel::navigateUp,
        onNavigateHome = { viewModel.loadDirectory(root) },
        onToggleAllowed = viewModel::togglePickedFolder,
        onRefresh = viewModel::refreshCurrentDirectory,
        onStorageSelected = viewModel::selectStorage,
        onDone = onClose,
        onDismiss = onClose,
        title = "Add music folders",
        hint = "Tap the circle to add a folder. Open a folder to pick one inside it.",
        pickSelectedPaths = pickedPaths
    )
}

/** Setup page: "Which folders have your music?" in the Yuyu welcome style. */
@Composable
fun MusicFoldersSetupPage(
    viewModel: MusicFoldersViewModel,
    hasMediaPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(viewModel) {
        viewModel.rescanOnChange = false
        onDispose { }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Yuyu.canvas)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                buildAnnotatedString {
                    append("Which folders have ")
                    withStyle(SpanStyle(background = Yuyu.highlightMint)) { append("your music?") }
                },
                style = YuyuType.display(40.sp),
                color = Yuyu.text
            )
            Text(
                "Pick the folders LMusic should look in. Voice notes, ringtones and other audio elsewhere stay out of your library.",
                style = YuyuType.bodyLarge,
                color = Yuyu.textSecondary
            )
        }
        MusicFoldersEditor(viewModel = viewModel, canOpenPicker = hasMediaPermission, allowRemovingLast = true)
        Spacer(Modifier.size(8.dp))
    }
}

/** Full-screen "Music folders" editor opened from Settings › Library. Changes trigger a rescan. */
@Composable
fun MusicFoldersSettingsDialog(
    visible: Boolean,
    viewModel: MusicFoldersViewModel,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    DisposableEffect(viewModel) {
        viewModel.rescanOnChange = true
        onDispose { }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Yuyu.canvas) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                YuyuTopBar(
                    title = "Music folders",
                    titleStyle = YuyuType.h2,
                    navigationIcon = {
                        YuyuIconButton(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            onClick = onDismiss
                        )
                    }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "LMusic only scans these folders. Your library updates after each change.",
                        style = YuyuType.body,
                        color = Yuyu.textSecondary
                    )
                    MusicFoldersEditor(viewModel = viewModel)
                }
            }
        }
    }
}

/** "Music folders" row for Settings › Library; opens the include-list editor. */
@Composable
fun MusicFoldersSettingsItem(viewModel: MusicFoldersViewModel = hiltViewModel()) {
    val included by viewModel.includedFolders.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val subtitle = when (included.size) {
        0 -> "All folders are scanned. Tap to choose."
        1 -> File(included.first()).name.ifEmpty { included.first() }
        else -> "${included.size} folders"
    }
    SettingsItem(
        title = "Music folders",
        subtitle = subtitle,
        leadingIcon = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.secondary) },
        trailingIcon = { Icon(Icons.Rounded.ChevronRight, "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = { open = true }
    )
    MusicFoldersSettingsDialog(visible = open, viewModel = viewModel, onDismiss = { open = false })
}
