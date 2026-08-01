package com.typezero.seraph.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.typezero.seraph.data.model.MusicBrainzResult
import com.typezero.seraph.data.model.Tags
import com.typezero.seraph.ui.components.Artwork
import com.typezero.seraph.ui.components.TagField
import com.typezero.seraph.ui.theme.Graphite850
import com.typezero.seraph.ui.theme.Graphite900
import com.typezero.seraph.ui.theme.Ink
import com.typezero.seraph.ui.theme.Teal
import com.typezero.seraph.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onEdit: ((Tags) -> Tags) -> Unit,
    onLookup: () -> Unit,
    onApply: (MusicBrainzResult) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                title = {
                    Column {
                        Text("Tag Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            state.fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = state.dirty && !state.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Violet,
                            contentColor = Ink,
                        ),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Ink)
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.size(7.dp))
                        Text(if (state.dirty) "Save" else "Saved", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { pad ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal)
            }
            return@Scaffold
        }

        val tags = state.tags
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(
                    Brush.verticalGradient(
                        listOf(Ink, Graphite900.copy(alpha = 0.64f), Ink),
                    ),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorHero(tags = tags, isSearching = state.isSearching, onLookup = onLookup)

            if (state.candidates.isNotEmpty()) {
                SectionLabel("MusicBrainz matches", "Tap a result to apply its metadata")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.candidates.forEach { candidate ->
                        CandidateCard(candidate, onApply = { onApply(candidate) })
                    }
                }
            }

            SectionLabel("Core metadata", "The fields most players display first")
            PremiumCard {
                TagField("Title", tags.title, { value -> onEdit { it.copy(title = value) } }, Modifier.fillMaxWidth())
                TagField("Artist", tags.artist, { value -> onEdit { it.copy(artist = value) } }, Modifier.fillMaxWidth())
                TagField("Album", tags.album, { value -> onEdit { it.copy(album = value) } }, Modifier.fillMaxWidth())
                TagField("Album artist", tags.albumArtist, { value -> onEdit { it.copy(albumArtist = value) } }, Modifier.fillMaxWidth())
            }

            SectionLabel("Track details", "Sequence, release year, and genre")
            PremiumCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TagField("Track", tags.trackNumber, { value -> onEdit { it.copy(trackNumber = value) } }, Modifier.weight(1f), numeric = true)
                    TagField("Total", tags.trackTotal, { value -> onEdit { it.copy(trackTotal = value) } }, Modifier.weight(1f), numeric = true)
                    TagField("Disc", tags.discNumber, { value -> onEdit { it.copy(discNumber = value) } }, Modifier.weight(1f), numeric = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TagField("Year", tags.year, { value -> onEdit { it.copy(year = value) } }, Modifier.weight(1f), numeric = true)
                    TagField("Genre", tags.genre, { value -> onEdit { it.copy(genre = value) } }, Modifier.weight(2f))
                }
                TagField("Comment", tags.comment, { value -> onEdit { it.copy(comment = value) } }, Modifier.fillMaxWidth())
            }

            ArtworkCard(tags.artwork)

            state.message?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = state.dirty && !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                    Spacer(Modifier.size(8.dp))
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (state.dirty) "Save metadata" else "All changes saved", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EditorHero(tags: Tags, isSearching: Boolean, onLookup: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Graphite900,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box {
                Artwork(tags.artwork, size = 112.dp)
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(30.dp),
                    shape = CircleShape,
                    color = Violet,
                    contentColor = Ink,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, contentDescription = null, Modifier.size(17.dp))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tags.title.ifBlank { "Untitled track" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tags.artist.ifBlank { "Unknown artist" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedButton(
                    onClick = onLookup,
                    enabled = !isSearching,
                    border = BorderStroke(1.dp, Teal.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Teal)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, Modifier.size(18.dp), tint = Teal)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text("Find metadata", color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Teal)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Graphite900,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ArtworkCard(artwork: ByteArray?) {
    SectionLabel("Embedded artwork", "Cover image stored inside the audio file")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Graphite900,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Artwork(artwork, size = 72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (artwork == null) "No embedded artwork" else "Embedded cover",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (artwork == null) "A MusicBrainz match can add cover art." else formatArtworkSize(artwork.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.Image, contentDescription = null, tint = Violet)
        }
    }
}

private fun formatArtworkSize(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB artwork".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "%.0f KB artwork".format(bytes / 1024f)
    else -> "$bytes B artwork"
}

@Composable
private fun CandidateCard(candidate: MusicBrainzResult, onApply: () -> Unit) {
    val highConfidence = candidate.score >= 90
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onApply),
        color = if (highConfidence) Teal.copy(alpha = 0.08f) else Graphite850,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (highConfidence) Teal.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = scoreColor(candidate.score).copy(alpha = 0.16f)) {
                    Text(
                        "${candidate.score}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor(candidate.score),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        candidate.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
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
                }
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Teal)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                buildString {
                    if (candidate.album.isNotBlank()) append(candidate.album)
                    if (candidate.year.isNotBlank()) {
                        if (isNotEmpty()) append("  •  ")
                        append(candidate.year)
                    }
                    if (isEmpty()) append("Recording match")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun scoreColor(score: Int) = when {
    score >= 90 -> Teal
    score >= 70 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
