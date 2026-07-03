package com.typezero.seraph.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.typezero.seraph.data.album.AlbumMatchService
import com.typezero.seraph.data.album.AlbumPlanRow
import com.typezero.seraph.data.model.AudioFile
import com.typezero.seraph.data.model.ReleaseCandidate
import com.typezero.seraph.data.model.ReleaseDetail
import com.typezero.seraph.data.musicbrainz.MusicBrainzClient
import com.typezero.seraph.data.rename.FilenameTemplate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AlbumStage { Searching, Pick, Review, Applying }

data class AlbumPreviewSummary(
    val files: Int = 0,
    val tagWrites: Int = 0,
    val artworkWrites: Int = 0,
    val renames: Int = 0,
    val changedTagFields: Int = 0,
)

data class AlbumUiState(
    val folderName: String = "",
    val query: String = "",
    val stage: AlbumStage = AlbumStage.Searching,
    val candidates: List<ReleaseCandidate> = emptyList(),
    val release: ReleaseDetail? = null,
    val rows: List<AlbumPlanRow> = emptyList(),
    val rename: Boolean = false,
    val embedArt: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
    val showApplyConfirm: Boolean = false,
    val preview: AlbumPreviewSummary = AlbumPreviewSummary(),
)

class AlbumMatchViewModel(
    private val service: AlbumMatchService,
    private val mb: MusicBrainzClient,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumUiState())
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    // Signals the host to leave the screen and rescan once changes land.
    private val _applied = Channel<Unit>(Channel.BUFFERED)
    val applied = _applied.receiveAsFlow()

    private var files: List<AudioFile> = emptyList()
    private val template = FilenameTemplate.DEFAULT

    fun start(folderName: String, folderFiles: List<AudioFile>) {
        files = folderFiles
        _state.value = AlbumUiState(folderName = folderName, query = folderName, busy = true)
        search(folderName)
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }

    fun search(q: String = _state.value.query) {
        _state.update { it.copy(stage = AlbumStage.Searching, busy = true, message = null) }
        viewModelScope.launch {
            val results = runCatching { mb.searchReleases(q) }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    stage = AlbumStage.Pick,
                    busy = false,
                    candidates = results,
                    message = if (results.isEmpty()) "No releases found — try a different search." else null,
                )
            }
        }
    }

    fun pick(candidate: ReleaseCandidate) {
        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            val release = runCatching { mb.getRelease(candidate.mbid) }.getOrNull()
            if (release == null) {
                _state.update { it.copy(busy = false, message = "Couldn't load that release — try another.") }
                return@launch
            }
            val planResult = runCatching { service.plan(files, release, template) }
            val rows = planResult.getOrDefault(emptyList())
            val preview = buildPreview(rows, rename = _state.value.rename, embedArt = _state.value.embedArt)
            val err = planResult.exceptionOrNull()
            _state.update {
                it.copy(
                    stage = AlbumStage.Review,
                    busy = false,
                    release = release,
                    rows = rows,
                    preview = preview,
                    message = when {
                        err != null -> "Match error: ${err.javaClass.simpleName}: ${err.message}"
                        rows.isEmpty() -> "No tracks matched (${files.size} files, ${release.total} tracks)."
                        else -> null
                    },
                )
            }
        }
    }

    fun backToPick() = _state.update {
        it.copy(stage = AlbumStage.Pick, release = null, rows = emptyList(), message = null)
    }

    fun toggleRename() = _state.update {
        val nextRename = !it.rename
        it.copy(rename = nextRename, preview = buildPreview(it.rows, nextRename, it.embedArt))
    }

    fun toggleArt() = _state.update {
        val nextArt = !it.embedArt
        it.copy(embedArt = nextArt, preview = buildPreview(it.rows, it.rename, nextArt))
    }

    fun requestApply() = _state.update { it.copy(showApplyConfirm = true) }

    fun cancelApply() = _state.update { it.copy(showApplyConfirm = false) }

    private fun buildPreview(
        rows: List<AlbumPlanRow>,
        rename: Boolean,
        embedArt: Boolean,
    ): AlbumPreviewSummary {
        val tagFieldsPerRow = 6 // title, artist, album, album artist, track number, track total/year bucket
        return AlbumPreviewSummary(
            files = rows.size,
            tagWrites = rows.size,
            artworkWrites = if (embedArt) rows.size else 0,
            renames = if (rename) rows.count { it.file.displayName != it.proposedName } else 0,
            changedTagFields = rows.size * tagFieldsPerRow,
        )
    }

    fun apply() {
        val st = _state.value
        val release = st.release ?: return
        if (st.rows.isEmpty()) return
        _state.update { it.copy(stage = AlbumStage.Applying, busy = true, message = null, showApplyConfirm = false) }
        viewModelScope.launch {
            val res = runCatching {
                service.apply(st.rows, release, st.folderName, st.rename, st.embedArt)
            }.getOrElse { com.typezero.seraph.data.album.AlbumApplyResult(0, 0, st.rows.size) }
            if (res.failed == 0) {
                _applied.send(Unit)
            } else {
                _state.update {
                    it.copy(
                        stage = AlbumStage.Review,
                        busy = false,
                        message = "Tagged ${res.tagged}, renamed ${res.renamed}, ${res.failed} failed.",
                    )
                }
            }
        }
    }
}
