package com.typezero.seraph.ui.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.typezero.seraph.data.album.AlbumPlanRow
import com.typezero.seraph.data.model.ReleaseCandidate
import com.typezero.seraph.data.model.ReleaseDetail
import com.typezero.seraph.ui.theme.Graphite850
import com.typezero.seraph.ui.theme.Graphite900
import com.typezero.seraph.ui.theme.Ink
import com.typezero.seraph.ui.theme.Teal
import com.typezero.seraph.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumMatchScreen(
    state: AlbumUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (ReleaseCandidate) -> Unit,
    onToggleRename: () -> Unit,
    onToggleArt: () -> Unit,
    onRequestApply: () -> Unit,
    onConfirmApply: () -> Unit,
    onCancelApply: () -> Unit,
    onBackToPick: () -> Unit,
    onFinishResult: () -> Unit,
    onBack: () -> Unit,
) {
    if (state.showApplyConfirm) {
        ApplyConfirmDialog(state, onConfirmApply, onCancelApply)
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                title = {
                    Column {
                        Text("Match Album", fontWeight = FontWeight.Bold)
                        if (state.folderName.isNotBlank()) {
                            Text(
                                state.folderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(Brush.verticalGradient(listOf(Ink, Graphite900.copy(alpha = 0.72f), Ink))),
        ) {
            when (state.stage) {
                AlbumStage.Searching -> Busy("Searching MusicBrainz…")
                AlbumStage.Applying -> ApplyingStage(state)
                AlbumStage.Result -> ResultStage(state, onFinishResult)
                AlbumStage.Pick -> PickStage(state, onQueryChange, onSearch, onPick)
                AlbumStage.Review -> ReviewStage(
                    state,
                    onToggleRename,
                    onToggleArt,
                    onRequestApply,
                    onBackToPick,
                )
            }
        }
    }
}

@Composable
private fun ApplyingStage(state: AlbumUiState) {
    val total = state.progressTotal.coerceAtLeast(1)
    val progress = state.progressCompleted.toFloat() / total.toFloat()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(progress = { progress }, color = Teal, strokeWidth = 5.dp, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Applying metadata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${state.progressCompleted} of ${state.progressTotal} files complete",
            color = Teal,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.progressFile.isNotBlank()) {
            Text(
                state.progressFile,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ResultStage(state: AlbumUiState, onFinish: () -> Unit) {
    val result = state.result ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (result.failed == 0) Teal.copy(alpha = 0.10f) else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, if (result.failed == 0) Teal else MaterialTheme.colorScheme.error),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (result.failed == 0) Teal else MaterialTheme.colorScheme.error)
                    Text(
                        if (result.failed == 0) "Album tagging complete" else "Album tagging completed with issues",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(if (result.failed == 0) "All planned changes were verified." else "Review the failures below before trying again.")
                }
            }
        }
        item {
            Surface(shape = MaterialTheme.shapes.large, color = Graphite900, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultRow("Files tagged", result.tagged.toString())
                    ResultRow("Artwork writes", result.artworkWrites.toString())
                    ResultRow("Files renamed", result.renamed.toString())
                    ResultRow("Failed", result.failed.toString())
                }
            }
        }
        if (result.errors.isNotEmpty()) {
            item { Text("DETAILS", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            items(result.errors) { error ->
                Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) {
                Text("Return to library", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Busy(label: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = Teal.copy(alpha = 0.12f)) {
            CircularProgressIndicator(
                modifier = Modifier.padding(18.dp).size(34.dp),
                color = Teal,
                strokeWidth = 3.dp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Seraph is building the safest match plan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PickStage(
    state: AlbumUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (ReleaseCandidate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CurrentFolderCard(state.folderName, state.candidates.size)
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Album or artist") },
                placeholder = { Text("Search MusicBrainz") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Teal)
                    }
                },
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.large,
            )
        }
        state.message?.let { message -> item { NoticeCard(message) } }

        if (state.candidates.isNotEmpty()) {
            item { SectionLabel("Recommended match", "Best result based on the folder name") }
            item {
                RecommendedReleaseCard(state.candidates.first(), onPick)
            }
            if (state.candidates.size > 1) {
                item { SectionLabel("Other results", "Choose another edition or release") }
                items(state.candidates.drop(1)) { candidate ->
                    ReleaseResultCard(candidate, onPick)
                }
            }
        }

        item {
            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Search online", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CurrentFolderCard(folderName: String, resultCount: Int) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Graphite900,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = Violet.copy(alpha = 0.14f)) {
                Icon(
                    Icons.Rounded.Album,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.padding(14.dp).size(30.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Current folder", style = MaterialTheme.typography.labelMedium, color = Teal)
                Text(
                    folderName.ifBlank { "Untitled album folder" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (resultCount == 0) "Ready to search for a release" else "$resultCount release candidates found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendedReleaseCard(candidate: ReleaseCandidate, onPick: (ReleaseCandidate) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onPick(candidate) },
        shape = MaterialTheme.shapes.large,
        color = Teal.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Teal.copy(alpha = 0.75f)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AlbumPlaceholder(Teal)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    candidate.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.artist.ifBlank { "Unknown artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(releaseMeta(candidate), style = MaterialTheme.typography.bodySmall, color = Teal)
            }
            Surface(shape = CircleShape, color = Teal) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Recommended",
                    tint = Ink,
                    modifier = Modifier.padding(6.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ReleaseResultCard(candidate: ReleaseCandidate, onPick: (ReleaseCandidate) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onPick(candidate) },
        shape = MaterialTheme.shapes.medium,
        color = Graphite850,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlbumPlaceholder(Violet, compact = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    candidate.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    candidate.artist.ifBlank { "Unknown artist" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(releaseMeta(candidate), style = MaterialTheme.typography.labelSmall, color = Violet)
            }
            Surface(
                modifier = Modifier.size(22.dp),
                shape = CircleShape,
                color = Graphite900,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {}
        }
    }
}

@Composable
private fun AlbumPlaceholder(accent: androidx.compose.ui.graphics.Color, compact: Boolean = false) {
    val size = if (compact) 58.dp else 76.dp
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Album, contentDescription = null, tint = accent, modifier = Modifier.size(if (compact) 28.dp else 36.dp))
        }
    }
}

private fun releaseMeta(candidate: ReleaseCandidate): String = buildString {
    if (candidate.year.isNotBlank()) append(candidate.year) else append("Unknown year")
    if (candidate.trackCount > 0) append("  •  ${candidate.trackCount} tracks")
}

@Composable
private fun ReviewStage(
    state: AlbumUiState,
    onToggleRename: () -> Unit,
    onToggleArt: () -> Unit,
    onApply: () -> Unit,
    onBackToPick: () -> Unit,
) {
    val release = state.release
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectedReleaseHero(release, state.rows.size, onBackToPick) }
        item { PreviewCard(state) }
        item {
            OptionCard(
                icon = Icons.Rounded.Image,
                title = "Embed album cover art",
                subtitle = "Store the release artwork inside every matched file.",
                checked = state.embedArt,
                accent = Violet,
                onToggle = onToggleArt,
            )
        }
        item {
            OptionCard(
                icon = Icons.Rounded.DriveFileRenameOutline,
                title = "Rename files after tagging",
                subtitle = "Apply Seraph's filename template after metadata is written.",
                checked = state.rename,
                accent = Teal,
                onToggle = onToggleRename,
            )
        }
        state.message?.let { message -> item { NoticeCard(message) } }
        if (state.rows.isNotEmpty()) {
            item { SectionLabel("Track plan", "Review each matched file before applying") }
            itemsIndexed(state.rows) { _, row -> TrackPlanCard(row, state.rename) }
        }
        item {
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.rows.isNotEmpty() && !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.rename) "Review & apply tags + rename" else "Review & apply tags",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            Text(
                "Dry-run preview only. Nothing is written until you confirm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelectedReleaseHero(release: ReleaseDetail?, matched: Int, onBackToPick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Teal.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Teal.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AlbumPlaceholder(Teal)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SELECTED RELEASE", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold)
                    Text(
                        release?.title.orEmpty().ifBlank { "Unknown release" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            append(release?.artist.orEmpty().ifBlank { "Unknown artist" })
                            release?.year?.takeIf { it.isNotBlank() }?.let { append("  •  $it") }
                            append("  •  matched $matched of ${release?.total ?: 0}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onBackToPick, modifier = Modifier.align(Alignment.End)) {
                Text("Choose a different release", color = Teal)
            }
        }
    }
}

@Composable
private fun PreviewCard(state: AlbumUiState) {
    val release = state.release
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Graphite900,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Teal)
                Text("Dry run preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreviewLine("Album", release?.title.orEmpty().ifBlank { "—" })
            PreviewLine("Album artist", release?.artist.orEmpty().ifBlank { "—" })
            PreviewLine("Year", release?.year.orEmpty().ifBlank { "—" })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreviewLine("Files to tag", state.preview.tagWrites.toString())
            PreviewLine("Tag fields planned", state.preview.changedTagFields.toString())
            PreviewLine("Artwork writes", if (state.embedArt) state.preview.artworkWrites.toString() else "Off")
            PreviewLine("Renames", if (state.rename) state.preview.renames.toString() else "Off")
        }
    }
}

@Composable
private fun PreviewLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onToggle() },
        shape = MaterialTheme.shapes.medium,
        color = Graphite850,
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.13f)) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun TrackPlanCard(row: AlbumPlanRow, showRename: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Graphite850,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = Teal.copy(alpha = 0.13f)) {
                Text(
                    row.position.toString().padStart(2, '0'),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Teal,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    row.title.ifBlank { "(untitled)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${row.artist}  •  ${row.tags.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "File: ${row.file.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showRename) {
                    Text(
                        "Rename → ${row.proposedName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Teal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplyConfirmDialog(state: AlbumUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Graphite900,
        title = { Text("Apply these changes?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Seraph will update ${state.preview.files} files.")
                PreviewLine("Tags", "${state.preview.tagWrites} files")
                PreviewLine("Artwork", if (state.embedArt) "${state.preview.artworkWrites} files" else "Off")
                PreviewLine("Rename", if (state.rename) "${state.preview.renames} files" else "Off")
                Text(
                    "Tagging replaces the original file only after the new copy is ready. Renaming runs only when enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink)) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = Teal, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoticeCard(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Violet.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Violet.copy(alpha = 0.35f)),
    ) {
        Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = Violet)
    }
}
