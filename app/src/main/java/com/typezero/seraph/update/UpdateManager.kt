package com.typezero.seraph.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Manual, user-initiated updater for sideloaded Seraph builds. */
class UpdateManager(private val context: Context) {

    data class Release(
        val versionCode: Long,
        val versionName: String,
        val apkUrl: String,
        val sha256: String,
        val notes: String,
    )

    sealed interface CheckResult {
        data class Available(val release: Release) : CheckResult
        data class Current(val versionName: String) : CheckResult
    }

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        val json = getText(MANIFEST_URL, 15_000, 30_000)
        val manifest = JSONObject(json)
        val release = Release(
            versionCode = manifest.getLong("versionCode"),
            versionName = manifest.getString("versionName"),
            apkUrl = manifest.getString("apkUrl"),
            sha256 = manifest.getString("sha256").lowercase(),
            notes = manifest.optString("releaseNotes"),
        )
        requireTrustedHttpsUrl(release.apkUrl)
        if (!release.sha256.matches(Regex("^[0-9a-f]{64}$"))) {
            throw IOException("Update manifest contains an invalid SHA-256")
        }

        val installed = installedVersionCode()
        if (release.versionCode > installed) CheckResult.Available(release)
        else CheckResult.Current(installedVersionName())
    }

    suspend fun downloadAndVerify(release: Release): File = withContext(Dispatchers.IO) {
        requireTrustedHttpsUrl(release.apkUrl)
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updateDir.listFiles()?.forEach { it.delete() }
        val apk = File(updateDir, "Seraph-${release.versionName}.apk")

        val conn = open(release.apkUrl, 20_000, 120_000)
        try {
            if (conn.responseCode !in 200..299) throw IOException("Update download failed: HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
        } finally {
            conn.disconnect()
        }

        val digest = sha256(apk)
        if (!digest.equals(release.sha256, ignoreCase = true)) {
            apk.delete()
            throw IOException("Downloaded APK failed SHA-256 verification")
        }
        verifyPackageAndSigner(apk)
        apk
    }

    fun launchInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun verifyPackageAndSigner(apk: File) {
        val pm = context.packageManager
        val archive = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
        } ?: throw IOException("Downloaded file is not a valid APK")

        if (archive.packageName != context.packageName) {
            throw IOException("Update package name does not match Seraph")
        }

        val installed = if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }

        val archiveSigners = signerDigests(archive)
        val installedSigners = signerDigests(installed)
        if (archiveSigners.isEmpty() || archiveSigners != installedSigners) {
            throw IOException("Update APK is not signed by the installed Seraph signer")
        }
    }

    private fun signerDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { bytesToHex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
    }

    private fun installedVersionCode(): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION") info.versionCode.toLong()
        }
    }

    private fun installedVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

    private fun getText(url: String, connectTimeout: Int, readTimeout: Int): String {
        val conn = open(url, connectTimeout, readTimeout)
        return try {
            if (conn.responseCode !in 200..299) throw IOException("Update check failed: HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String, connectTimeout: Int, readTimeout: Int): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
            setRequestProperty("Accept", "application/json, application/vnd.android.package-archive;q=0.9, */*;q=0.1")
            setRequestProperty("User-Agent", "Seraph-Android-Updater")
        }

    private fun requireTrustedHttpsUrl(value: String) {
        val uri = Uri.parse(value)
        val host = uri.host?.lowercase()
        if (uri.scheme != "https" || host !in TRUSTED_DOWNLOAD_HOSTS) {
            throw IOException("Update URL is not an approved HTTPS host")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return bytesToHex(digest.digest())
    }

    private fun bytesToHex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/MikereDD/It-Works-On-My-Machine/main/Android/Seraph/update-manifest.json"
        private val TRUSTED_DOWNLOAD_HOSTS = setOf(
            "raw.githubusercontent.com",
            "github.com",
            "objects.githubusercontent.com",
        )
    }
}
