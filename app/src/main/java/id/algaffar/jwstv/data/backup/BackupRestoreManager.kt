package id.algaffar.jwstv.data.backup

import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.domain.model.AppStateSettings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupRestoreSummary(
    val settingsRestored: Boolean,
    val extractedAssetFilesCount: Int,
    val restoredMosqueName: String
)

/**
 * Full Backup & Restore `.zip` Manager.
 * Supports:
 * 1. Exporting full backup `.zip` (`settings.json` + all files under `jws-assets/`).
 * 2. Restoring from both new Android `.zip` backups AND **legacy Tauri `.zip` backups**
 *    (`settings.json` / `app-state-settings.json` / `config.json` + `jws-assets/` media folders),
 *    protected against Zip-Slip path traversal.
 * Mirrors `src-tauri/src/backup.rs`.
 */
object BackupRestoreManager {

    private val SETTINGS_JSON_FILENAMES = setOf(
        "settings.json",
        "jws_settings.json",
        "app-state-settings.json",
        "config.json"
    )

    fun exportFullBackupZip(
        outputStream: OutputStream,
        settingsRepository: SettingsRepository,
        assetsRootDir: File
    ) {
        ZipOutputStream(outputStream.buffered()).use { zos ->
            // 1. Write settings.json
            val jsonBytes = settingsRepository.exportBackupJson().toByteArray(Charsets.UTF_8)
            val settingsEntry = ZipEntry("settings.json")
            zos.putNextEntry(settingsEntry)
            zos.write(jsonBytes)
            zos.closeEntry()

            // 2. Recursively write all media files inside jws-assets/
            if (assetsRootDir.exists() && assetsRootDir.isDirectory) {
                assetsRootDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relPath = file.relativeTo(assetsRootDir).invariantSeparatorsPath
                        val zipEntryName = "jws-assets/$relPath"
                        zos.putNextEntry(ZipEntry(zipEntryName))
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
            }
            zos.finish()
        }
    }

    suspend fun restoreFromBackupZip(
        inputStream: InputStream,
        settingsRepository: SettingsRepository,
        assetsRootDir: File
    ): Result<BackupRestoreSummary> {
        return runCatching {
            assetsRootDir.mkdirs()
            val canonicalAssetsRoot = assetsRootDir.canonicalPath + File.separator

            var rawSettingsJson: String? = null
            var extractedCount = 0

            ZipInputStream(inputStream.buffered()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.replace('\\', '/').trimStart('/')
                    if (!entry.isDirectory && entryName.isNotBlank()) {
                        val baseName = entryName.substringAfterLast('/')

                        if (baseName.lowercase() in SETTINGS_JSON_FILENAMES && rawSettingsJson == null) {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            rawSettingsJson = baos.toString(Charsets.UTF_8.name())
                        } else {
                            val assetRelativePath = extractAssetRelativePath(entryName)
                            if (assetRelativePath != null) {
                                val destFile = File(assetsRootDir, assetRelativePath)
                                val canonicalDest = destFile.canonicalPath
                                // Zip-Slip security guard
                                if (!canonicalDest.startsWith(canonicalAssetsRoot)) {
                                    throw SecurityException("Invalid ZIP entry path traversal: $entryName")
                                }
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                extractedCount++
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            var restoredSettings: AppStateSettings = settingsRepository.currentSettings
            var settingsRestored = false
            val jsonToImport = rawSettingsJson
            if (!jsonToImport.isNullOrBlank()) {
                restoredSettings = settingsRepository.importBackupJson(jsonToImport).getOrThrow()
                settingsRestored = true
            }

            BackupRestoreSummary(
                settingsRestored = settingsRestored,
                extractedAssetFilesCount = extractedCount,
                restoredMosqueName = restoredSettings.brand.name
            )
        }
    }

    /**
     * Maps both `jws-assets/audio/tartil/fajr.mp3` and legacy top-level `audio/tartil/fajr.mp3`
     * or `images/slide1.jpg` inside a ZIP archive to the clean relative path inside `jws-assets/`.
     */
    internal fun extractAssetRelativePath(normalizedEntryName: String): String? {
        val jwsAssetsMarker = "jws-assets/"
        val idx = normalizedEntryName.indexOf(jwsAssetsMarker, ignoreCase = true)
        if (idx >= 0) {
            val sub = normalizedEntryName.substring(idx + jwsAssetsMarker.length).trimStart('/')
            return sub.takeIf { it.isNotBlank() }
        }

        val knownFolders = listOf("audio/", "images/", "videos/", "slides/", "backgrounds/")
        for (prefix in knownFolders) {
            if (normalizedEntryName.startsWith(prefix, ignoreCase = true)) {
                return normalizedEntryName
            }
        }
        return null
    }
}
