package com.typezero.seraph.pcloud

import com.typezero.seraph.data.model.AudioFile
import com.typezero.seraph.data.tagging.Tagger
import com.typezero.seraph.storage.FolderListing
import com.typezero.seraph.storage.FolderNode
import com.typezero.seraph.storage.StorageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Minimal pCloud HTTP API client (JSON protocol). Auth is via the `auth` token
 * captured from a logged-in pCloud web session ([resolveHost] binds it to a region).
 * Endpoints used: userinfo, listfolder, getfilelink (+ host/path download),
 * uploadfile, renamefile — all verified against docs.pcloud.com.
 */
class PCloudClient(private val session: PCloudSession) {

    private fun base() = "https://${session.apiHost}"
    private fun token() = session.token ?: throw IOException("Not signed in to pCloud")

    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")

    /**
     * Bind a token captured from a logged-in pCloud web session to its region by
     * calling userinfo on each host. Returns the working host, or null if neither
     * accepts the token. (pCloud accounts are US or EU; the token only works on one.)
     */
    suspend fun resolveHost(token: String): String? = withContext(Dispatchers.IO) {
        for (host in listOf("api.pcloud.com", "eapi.pcloud.com")) {
            val ok = runCatching {
                getJson("https://$host/userinfo?auth=${enc(token)}"); true
            }.getOrDefault(false)
            if (ok) return@withContext host
        }
        null
    }

    suspend fun listAudioFiles(paths: List<String>): List<AudioFile> = withContext(Dispatchers.IO) {
        val out = ArrayList<AudioFile>()
        for (path in paths) {
            val url = "${base()}/listfolder?path=${enc(path)}&recursive=1&auth=${enc(token())}"
            // Skip a configured folder that doesn't exist on this account rather than failing the whole scan.
            val json = runCatching { getJson(url) }.getOrNull() ?: continue
            json.optJSONObject("metadata")?.let { collect(it, it.optString("name", path), out) }
        }
        out.sortedBy { it.displayName.lowercase() }
    }

    private fun collect(folder: JSONObject, folderName: String, out: MutableList<AudioFile>) {
        val contents = folder.optJSONArray("contents") ?: return
        for (i in 0 until contents.length()) {
            val item = contents.getJSONObject(i)
            if (item.optBoolean("isfolder")) {
                collect(item, item.optString("name", folderName), out)
            } else {
                val name = item.optString("name")
                if (name.substringAfterLast('.', "").lowercase() in Tagger.SUPPORTED_EXTENSIONS) {
                    out += AudioFile(
                        sourceId = StorageSource.PCLOUD,
                        id = item.optLong("fileid").toString(),
                        displayName = name,
                        mimeType = item.optString("contenttype", "audio/*"),
                        sizeBytes = item.optLong("size"),
                        parentId = folder.optLong("folderid").toString(),
                        parentName = folderName,
                    )
                }
            }
        }
    }

    /** Resolve each configured scan path to a top-level browsable folder. */
    suspend fun listRoots(paths: List<String>): List<FolderNode> = withContext(Dispatchers.IO) {
        paths.mapNotNull { path ->
            val url = "${base()}/listfolder?path=${enc(path)}&nofiles=1&auth=${enc(token())}"
            val md = runCatching { getJson(url) }.getOrNull()?.optJSONObject("metadata")
                ?: return@mapNotNull null
            FolderNode(md.optLong("folderid").toString(), md.optString("name", path.trim('/')))
        }
    }

    /** List one folder's immediate subfolders + audio files (non-recursive). */
    suspend fun listChildren(folderId: String): FolderListing = withContext(Dispatchers.IO) {
        val json = getJson("${base()}/listfolder?folderid=$folderId&auth=${enc(token())}")
        val md = json.optJSONObject("metadata")
        val folderName = md?.optString("name", "").orEmpty()
        val folders = ArrayList<FolderNode>()
        val files = ArrayList<AudioFile>()
        md?.optJSONArray("contents")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                if (item.optBoolean("isfolder")) {
                    folders += FolderNode(item.optLong("folderid").toString(), item.optString("name"))
                } else {
                    val name = item.optString("name")
                    if (name.substringAfterLast('.', "").lowercase() in Tagger.SUPPORTED_EXTENSIONS) {
                        files += AudioFile(
                            sourceId = StorageSource.PCLOUD,
                            id = item.optLong("fileid").toString(),
                            displayName = name,
                            mimeType = item.optString("contenttype", "audio/*"),
                            sizeBytes = item.optLong("size"),
                            parentId = folderId,
                            parentName = folderName,
                        )
                    }
                }
            }
        }
        FolderListing(
            folders.sortedBy { it.name.lowercase() },
            files.sortedBy { it.displayName.lowercase() },
        )
    }

    suspend fun downloadToCache(fileId: String, displayName: String, cacheDir: File): File =
        withContext(Dispatchers.IO) {
            val linkJson = getJson("${base()}/getfilelink?fileid=$fileId&auth=${enc(token())}")
            val hosts = linkJson.optJSONArray("hosts") ?: throw IOException("No download host")
            val path = linkJson.optString("path")
            if (hosts.length() == 0 || path.isBlank()) throw IOException("Bad file link")
            val downloadUrl = "https://${hosts.getString(0)}$path"

            val ext = displayName.substringAfterLast('.', "tmp")
            val cache = File(cacheDir, "pc_${System.nanoTime()}.$ext")
            (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 60_000
            }.inputStream.use { input ->
                cache.outputStream().use { input.copyTo(it, DEFAULT_BUFFER_SIZE) }
            }
            cache
        }

    /**
     * Replace an existing pCloud file without creating a same-name duplicate.
     *
     * pCloud's uploadfile endpoint can create an additional file when a same-name
     * object already exists. For tag writes, that is wrong: metadata edits should
     * keep the visible filename unless the user runs Rename.
     *
     * Safe replace flow:
     * 1. upload edited bytes with a unique temporary name;
     * 2. rename the original fileid to a backup name;
     * 3. rename the uploaded temp file to the original visible name;
     * 4. delete the backup.
     *
     * If anything fails before step 3, the original is restored/kept. The returned
     * AudioFile has the new pCloud fileid, which matters for a later rename pass.
     */
    suspend fun replaceFile(original: AudioFile, edited: File): AudioFile = withContext(Dispatchers.IO) {
        val ext = original.displayName.substringAfterLast('.', "")
        val suffix = if (ext.isBlank()) "" else ".$ext"
        val stamp = System.nanoTime()
        val tempName = "seraph_upload_tmp_${stamp}$suffix"
        val backupName = "seraph_backup_${stamp}_${original.displayName}"

        val uploaded = uploadNewFile(original.parentId, tempName, edited)
        var originalMoved = false
        try {
            renameFile(original.id, backupName)
            originalMoved = true
            renameFile(uploaded.id, original.displayName)
            runCatching { deleteFile(original.id) }
            uploaded.copy(displayName = original.displayName)
        } catch (e: Exception) {
            if (originalMoved) runCatching { renameFile(original.id, original.displayName) }
            runCatching { deleteFile(uploaded.id) }
            throw e
        }
    }

    /** Uploads [file] into [folderId] as a new unique [filename]. */
    private fun uploadNewFile(folderId: String, filename: String, file: File): AudioFile {
        val url = "${base()}/uploadfile?folderid=$folderId&filename=${enc(filename)}" +
            "&nopartial=1&auth=${enc(token())}"
        val boundary = "----att${System.nanoTime()}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000; readTimeout = 120_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        conn.outputStream.use { os ->
            val header = ("--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n").toByteArray()
            os.write(header)
            file.inputStream().use { it.copyTo(os, DEFAULT_BUFFER_SIZE) }
            os.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val result = json.optInt("result", -1)
        if (result != 0) throw IOException("pCloud upload failed (result=$result): ${json.optString("error")}")

        val metadata = json.optJSONArray("metadata")?.optJSONObject(0)
            ?: json.optJSONObject("metadata")
            ?: throw IOException("pCloud upload did not return file metadata")
        return AudioFile(
            sourceId = StorageSource.PCLOUD,
            id = metadata.optLong("fileid").toString(),
            displayName = metadata.optString("name", filename),
            mimeType = metadata.optString("contenttype", "audio/*"),
            sizeBytes = metadata.optLong("size", file.length()),
            parentId = folderId,
            parentName = "",
        )
    }

    /** Server-side delete. */
    suspend fun deleteFile(fileId: String): Unit = withContext(Dispatchers.IO) {
        val url = "${base()}/deletefile?fileid=$fileId&auth=${enc(token())}"
        val result = getJson(url).optInt("result", -1)
        if (result != 0) throw IOException("pCloud delete failed (result=$result)")
    }

    /** Server-side rename — no download needed. fileid is preserved. */
    suspend fun renameFile(fileId: String, newName: String): Unit = withContext(Dispatchers.IO) {
        val url = "${base()}/renamefile?fileid=$fileId&toname=${enc(newName)}&auth=${enc(token())}"
        val result = getJson(url).optInt("result", -1)
        if (result != 0) throw IOException("pCloud rename failed (result=$result)")
    }

    private fun getJson(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000; readTimeout = 30_000
        }
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val result = json.optInt("result", 0)
        if (result != 0) throw IOException("pCloud error ${result}: ${json.optString("error")}")
        return json
    }
}
