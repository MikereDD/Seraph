package com.typezero.seraph.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.typezero.seraph.data.model.AudioFile
import com.typezero.seraph.storage.FolderNode
import com.typezero.seraph.storage.StorageSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onPickDevice: () -> Unit,
    onSelectPCloud: () -> Unit,
    onOpenFolder: (FolderNode) -> Unit,
    onUp: () -> Unit,
    onOpen: (AudioFile) -> Unit,
    onRename: () -> Unit,
    onAlbumMatch: () -> Unit,
    onToggleSelect: (AudioFile) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onAbout: () -> Unit,
) {
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (state.selectionMode) "${state.selection.size} selected" else "Seraph",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            if (state.selectionMode) "Choose an action" else state.currentName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (state.selectionMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Clear selection")
                        }
                    } else if (!state.atRoot) {
                        IconButton(onClick = onUp) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Up a folder")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onAbout) {
                        Icon(Icons.Rounded.Info, contentDescription = "About Seraph")
                    }
                },
                colors = barColors,
            )
        },
        bottomBar = {
            if (state.selectionMode) {
                SelectionBar(
                    count = state.selection.size,
                    totalBytes = state.files.filter { it.id in state.selection }.sumOf { it.sizeBytes },
                    allSelected = state.allSelected,
                    onSelectAll = onSelectAll,
                    onAlbumMatch = onAlbumMatch,
                    onRename = onRename,
                )
            }
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
        ) {
            SourceSelector(
                activeSourceId = state.activeSourceId,
                pcloudSignedIn = state.pcloudSignedIn,
                onPickDevice = onPickDevice,
                onSelectPCloud = onSelectPCloud,
            )

            when {
                state.isLoading -> Centered {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Loading your library", style = MaterialTheme.typography.titleSmall)
                }

                !state.hasSource -> EmptyLibrary()

                else -> LibraryContent(
                    state = state,
                    onOpenFolder = onOpenFolder,
                    onOpen = onOpen,
                    onToggleSelect = onToggleSelect,
                )
            }
        }
    }
}

@Composable
private fun SourceSelector(
    activeSourceId: String,
    pcloudSignedIn: Boolean,
    onPickDevice: () -> Unit,
    onSelectPCloud: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)) {
        SectionLabel("MUSIC SOURCE")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SourceCard(
                title = "Device",
                subtitle = "Local music",
                selected = activeSourceId == StorageSource.DEVICE,
                icon = { Icon(Icons.Rounded.PhoneAndroid, null) },
                onClick = onPickDevice,
                modifier = Modifier.weight(1f),
            )
            SourceCard(
                title = "pCloud",
                subtitle = if (pcloudSignedIn) "Connected" else "Connect cloud",
                selected = activeSourceId == StorageSource.PCLOUD,
                icon = { Icon(Icons.Rounded.Cloud, null) },
                onClick = onSelectPCloud,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    onOpenFolder: (FolderNode) -> Unit,
    onOpen: (AudioFile) -> Unit,
    onToggleSelect: (AudioFile) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        item {
            BreadcrumbRow(state)
        }

        state.error?.let { message -> item { Notice(message) } }

        if (state.folders.isNotEmpty()) {
            item { SectionHeader("FOLDERS", "${state.folders.size}") }
            items(state.folders.chunked(2), key = { pair -> pair.joinToString("|") { it.id } }) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { folder ->
                        FolderCard(
                            name = folder.name,
                            onClick = { onOpenFolder(folder) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (state.files.isNotEmpty()) {
            item { SectionHeader("FILES", "${state.files.size}") }
            items(state.files, key = { "f:${it.id}" }) { file ->
                FileRow(
                    file = file,
                    selected = file.id in state.selection,
                    selectionMode = state.selectionMode,
                    onOpen = { onOpen(file) },
                    onToggle = { onToggleSelect(file) },
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(state: LibraryUiState) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (state.activeSourceId == StorageSource.PCLOUD) "pCloud" else "Device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            state.breadcrumb.takeLast(3).forEach { crumb ->
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    crumb.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            if (selected) 1.25.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 5.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                ) { icon() }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun FolderCard(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            ) {
                Icon(
                    Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    file: AudioFile,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggle() else onOpen() },
                onLongClick = onToggle,
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            }
        ),
        border = BorderStroke(
            if (selected) 1.25.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        file.parentName.takeIf { it.isNotBlank() },
                        file.extension.uppercase().takeIf { it.isNotBlank() },
                        humanSize(file.sizeBytes),
                    ).joinToString("  •  "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    totalBytes: Long,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onAlbumMatch: () -> Unit,
    onRename: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Text("$count selected", style = MaterialTheme.typography.titleSmall)
                    Text(
                        humanSize(totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Rounded.DoneAll,
                        contentDescription = if (allSelected) "Deselect all" else "Select all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAlbumMatch) {
                    Icon(
                        Icons.Rounded.Album,
                        contentDescription = "Match album",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                IconButton(onClick = onRename) {
                    Icon(
                        Icons.Rounded.DriveFileRenameOutline,
                        contentDescription = "Rename selected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Centered {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).size(30.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Your library, beautifully organized", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(7.dp))
        Text(
            "Choose a device folder or connect pCloud to browse music, edit tags, and match albums.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 1.dp, start = 3.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            count,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Notice(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)),
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}
