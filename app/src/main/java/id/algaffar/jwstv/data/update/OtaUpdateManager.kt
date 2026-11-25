package id.algaffar.jwstv.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import id.algaffar.jwstv.SharedHttpClient
import id.algaffar.jwstv.config.AppEndpoints
import id.algaffar.jwstv.service.JwsDeviceAdminReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

@Serializable
data class OtaManifest(
    val versionCode: Int = 1,
    val versionName: String = "2.0.0",
    val changelog: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val minAndroidSdk: Int = 26
)

data class OtaCheckResult(
    val hasUpdate: Boolean,
    val currentVersionCode: Int,
    val manifest: OtaManifest? = null,
    val message: String = ""
)

/**
 * Online (HTTPS manifest + SHA-256 verification) & Offline (USB / Remote PWA APK upload)
 * self-updater for Al-Gaffar TV STB Bundling devices.
 */
class OtaUpdateManager(
    private val context: Context? = null,
    private val httpClient: OkHttpClient = SharedHttpClient.baseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        manifestUrl: String = AppEndpoints.OTA_MANIFEST_URL
    ): Result<OtaCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(manifestUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Server OTA merespons HTTP ${resp.code}")
                }
                val body = resp.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) throw IllegalStateException("Manifest OTA kosong")
                val manifest = json.decodeFromString<OtaManifest>(body)
                val hasUpdate = manifest.versionCode > currentVersionCode && manifest.apkUrl.isNotBlank()
                val msg = if (hasUpdate) {
                    "Pembaruan tersedia: v${manifest.versionName} (Build ${manifest.versionCode})"
                } else {
                    "Al-Gaffar TV sudah menggunakan versi terbaru (Build $currentVersionCode)."
                }
                OtaCheckResult(
                    hasUpdate = hasUpdate,
                    currentVersionCode = currentVersionCode,
                    manifest = manifest,
                    message = msg
                )
            }
        }
    }

    suspend fun downloadAndVerifyApk(
        manifest: OtaManifest,
        targetDir: File,
        onProgress: (percent: Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(manifest.apkUrl.startsWith("https://", ignoreCase = true)) {
                "URL APK OTA wajib menggunakan HTTPS demi keamanan perangkat STB."
            }
            targetDir.mkdirs()
            val outFile = File(targetDir, "algaffar_tv_${manifest.versionCode}.apk")
            val tmpFile = File(targetDir, "algaffar_tv_${manifest.versionCode}.apk.tmp")

            val req = Request.Builder().url(manifest.apkUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Gagal mengunduh APK (HTTP ${resp.code})")
                }
                val body = resp.body ?: throw IllegalStateException("Body APK kosong")
                val totalBytes = body.contentLength()
                var downloaded = 0L
                val buf = ByteArray(64 * 1024)
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { fos ->
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            fos.write(buf, 0, read)
                            downloaded += read
                            if (totalBytes > 0L) {
                                onProgress(((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100))
                            }
                        }
                        fos.flush()
                    }
                }
            }

            if (manifest.sha256.isNotBlank()) {
                val actualHash = computeFileSha256(tmpFile)
                if (!verifySha256(actualHash, manifest.sha256)) {
                    tmpFile.delete()
                    throw SecurityException(
                        "Verifikasi SHA-256 APK gagal! File dibatalkan demi keamanan."
                    )
                }
            }

            if (outFile.exists()) outFile.delete()
            if (!tmpFile.renameTo(outFile)) {
                tmpFile.copyTo(outFile, overwrite = true)
                tmpFile.delete()
            }
            outFile
        }
    }

    fun stageOfflineApk(inputStream: InputStream, targetDir: File, maxBytes: Long = 180L * 1024L * 1024L): Result<File> {
        return runCatching {
            targetDir.mkdirs()
            val outFile = File(targetDir, "algaffar_tv_offline_update.apk")
            val tmpFile = File(targetDir, "algaffar_tv_offline_update.apk.tmp")
            var written = 0L
            val buf = ByteArray(64 * 1024)
            try {
                FileOutputStream(tmpFile).use { fos ->
                    var read: Int
                    while (inputStream.read(buf).also { read = it } != -1) {
                        written += read
                        if (written > maxBytes) {
                            throw IllegalArgumentException("Ukuran APK melebihi batas maksimal 180 MB")
                        }
                        fos.write(buf, 0, read)
                    }
                    fos.flush()
                }
                if (written < 100_000L) {
                    throw IllegalArgumentException("File APK terlalu kecil atau tidak valid")
                }
                // Check ZIP/APK magic header ("PK\u0003\u0004")
                val header = ByteArray(4)
                FileInputStream(tmpFile).use { it.read(header) }
                if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                    throw IllegalArgumentException("File bukan arsip APK yang valid")
                }
                if (outFile.exists()) outFile.delete()
                if (!tmpFile.renameTo(outFile)) {
                    tmpFile.copyTo(outFile, overwrite = true)
                    tmpFile.delete()
                }
                outFile
            } catch (t: Throwable) {
                runCatching { tmpFile.delete() }
                throw t
            }
        }
    }

    fun launchApkInstaller(
        apkFile: File,
        onBeforeSystemInstaller: (() -> Unit)? = null
    ): Result<Unit> = runCatching {
        val ctx = context ?: throw IllegalStateException("Android Context tidak tersedia")
        require(apkFile.exists() && apkFile.isFile) { "File APK tidak ditemukan: ${apkFile.name}" }

        if (JwsDeviceAdminReceiver.isDeviceOwner(ctx)) {
            val silentResult = runCatching { installSilentlyAsDeviceOwner(ctx, apkFile) }
            if (silentResult.isSuccess) return@runCatching
        }

        onBeforeSystemInstaller?.invoke()
        val apkUri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        }
        ctx.startActivity(intent)
    }

    private fun installSilentlyAsDeviceOwner(ctx: Context, apkFile: File) {
        val packageInstaller = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(ctx.packageName)
        val sessionId = packageInstaller.createSession(params)
        packageInstaller.openSession(sessionId).use { session ->
            session.openWrite("ota_update.apk", 0, apkFile.length()).use { out ->
                FileInputStream(apkFile).use { input ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                    }
                    session.fsync(out)
                }
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val statusIntent = Intent("id.algaffar.jwstv.OTA_INSTALL_STATUS").setPackage(ctx.packageName)
            val pendingIntent = PendingIntent.getBroadcast(ctx, sessionId, statusIntent, flags)
            session.commit(pendingIntent.intentSender)
        }
    }

    companion object {
        fun computeFileSha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(64 * 1024)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buf).also { read = it } != -1) {
                    md.update(buf, 0, read)
                }
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        fun verifySha256(actualHex: String, expectedHex: String): Boolean {
            val a = actualHex.trim().lowercase()
            val e = expectedHex.trim().lowercase()
            if (a.length != 64 || e.length != 64) return false
            return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), e.toByteArray(Charsets.UTF_8))
        }
    }
}
