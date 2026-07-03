package com.typezero.seraph.data.tagging

import com.typezero.seraph.data.model.Tags
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.IOException

/** Pure jaudiotagger read/write against a local File. Storage-agnostic. */
class Tagger {

    fun read(file: File): Tags {
        val af = AudioFileIO.read(file)
        val tag = af.tagOrCreateAndSetDefault
        fun f(key: FieldKey) = runCatching { tag.getFirst(key) }.getOrDefault("")
        val art = runCatching { tag.firstArtwork?.binaryData }.getOrNull()
        return Tags(
            title = f(FieldKey.TITLE),
            artist = f(FieldKey.ARTIST),
            album = f(FieldKey.ALBUM),
            albumArtist = f(FieldKey.ALBUM_ARTIST),
            trackNumber = f(FieldKey.TRACK),
            trackTotal = f(FieldKey.TRACK_TOTAL),
            discNumber = f(FieldKey.DISC_NO),
            year = f(FieldKey.YEAR),
            genre = f(FieldKey.GENRE),
            comment = f(FieldKey.COMMENT),
            artwork = art,
        )
    }

    fun write(file: File, tags: Tags) {
        val af = AudioFileIO.read(file)
        val tag = af.tagOrCreateAndSetDefault
        fun set(key: FieldKey, value: String) {
            if (value.isBlank()) runCatching { tag.deleteField(key) } else tag.setField(key, value)
        }
        set(FieldKey.TITLE, tags.title)
        set(FieldKey.ARTIST, tags.artist)
        set(FieldKey.ALBUM, tags.album)
        set(FieldKey.ALBUM_ARTIST, tags.albumArtist)
        set(FieldKey.TRACK, tags.trackNumber)
        set(FieldKey.TRACK_TOTAL, tags.trackTotal)
        set(FieldKey.DISC_NO, tags.discNumber)
        set(FieldKey.YEAR, tags.year)
        set(FieldKey.GENRE, tags.genre)
        set(FieldKey.COMMENT, tags.comment)

        val expectedArtwork = tags.artwork
        if (expectedArtwork != null) {
            validateArtwork(expectedArtwork)
            tag.deleteArtworkField()

            // v0.4.2: jaudiotagger is much more reliable when artwork is created
            // from a real image file. AndroidArtwork(binaryData=...) can appear to
            // succeed but produce no embedded APIC/cover block on some formats.
            val coverFile = writeTempCover(file.parentFile ?: File("."), expectedArtwork)
            try {
                val art = ArtworkFactory.createArtworkFromFile(coverFile).apply {
                    pictureType = 3 // front cover
                    mimeType = sniffMime(expectedArtwork)
                }
                tag.setField(tag.createField(art))
            } finally {
                coverFile.delete()
            }
        }

        af.commit()

        if (expectedArtwork != null) {
            val saved = artworkSize(file)
            if (saved < MIN_ARTWORK_BYTES) {
                throw IOException("Tag write completed, but embedded artwork was not found when the file was re-read")
            }
        }
    }

    fun hasArtwork(file: File): Boolean = artworkSize(file) >= MIN_ARTWORK_BYTES

    fun artworkSize(file: File): Int {
        val af = AudioFileIO.read(file)
        val art = af.tagOrCreateAndSetDefault.firstArtwork?.binaryData
        return art?.size ?: 0
    }

    private fun writeTempCover(dir: File, bytes: ByteArray): File {
        val ext = when (sniffMime(bytes)) {
            "image/png" -> ".png"
            else -> ".jpg"
        }
        return File.createTempFile("seraph_cover_", ext, dir).apply {
            outputStream().use { it.write(bytes) }
        }
    }

    private fun validateArtwork(bytes: ByteArray) {
        if (bytes.size < MIN_ARTWORK_BYTES) {
            throw IOException("Artwork is too small to be a valid cover image (${bytes.size} bytes)")
        }
        val jpeg = bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val png = bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        if (!jpeg && !png) {
            throw IOException("Cover Art Archive returned bytes that are not JPEG or PNG")
        }
    }

    private fun sniffMime(bytes: ByteArray): String = when {
        bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() -> "image/png"
        else -> "image/jpeg"
    }

    companion object {
        private const val MIN_ARTWORK_BYTES = 128
        val SUPPORTED_EXTENSIONS = setOf("mp3", "flac", "m4a", "mp4", "ogg", "opus", "wav", "aac", "wma")
    }
}
