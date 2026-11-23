package id.algaffar.jwstv.data.repository

import id.algaffar.jwstv.data.logging.AppLogger
import id.algaffar.jwstv.domain.model.AppStateSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Atomic JSON-backed repository for [AppStateSettings] with dual-file `.bak` power-loss resilience.
 * Directly supports importing/restoring legacy Tauri `settings.json` files.
 */
class SettingsRepository(
    private val storageFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()
    private val backupFile: File = File(storageFile.parentFile, "${storageFile.name}.bak")

    val jsonSerializer: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        prettyPrint = true
        isLenient = true
    }

    private val _settingsFlow = MutableStateFlow(loadInitialFromDisk())
    val settingsFlow: StateFlow<AppStateSettings> = _settingsFlow.asStateFlow()

    val currentSettings: AppStateSettings
        get() = _settingsFlow.value

    private fun loadInitialFromDisk(): AppStateSettings {
        val primary = tryReadSettingsFile(storageFile)
        if (primary != null) return primary

        // Preserve corrupted primary file for diagnostics before falling back to `.bak`
        if (storageFile.exists() && storageFile.length() > 0L) {
            runCatching {
                val quarantine = File(
                    storageFile.parentFile,
                    "${storageFile.name}.corrupt.${System.currentTimeMillis()}.json"
                )
                storageFile.copyTo(quarantine, overwrite = true)
            }
            AppLogger.error("STORAGE", "jws_settings.json rusak saat boot — mencoba pemulihan otomatis dari .bak")
        }

        val recovered = tryReadSettingsFile(backupFile)
        if (recovered != null) {
            AppLogger.warn("STORAGE", "Pengaturan masjid berhasil dipulihkan dari salinan cadangan (.bak)!")
            runCatching { writeAtomicWithSync(storageFile, jsonSerializer.encodeToString(recovered)) }
            return recovered
        }

        return AppStateSettings()
    }

    private fun tryReadSettingsFile(file: File): AppStateSettings? {
        if (!file.exists() || file.length() == 0L) return null
        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            val parsed = jsonSerializer.decodeFromString<AppStateSettings>(migrateLegacyJsonPaths(raw))
            migrateSettingsSchema(parsed, fromExistingDisk = true)
        }.getOrNull()
    }

    private fun migrateSettingsSchema(
        settings: AppStateSettings,
        fromExistingDisk: Boolean = true
    ): AppStateSettings {
        var current = settings
        if (current.schemaVersion < 3) {
            val hasSystemSlides = current.slides.any { it.type == "welcome" || it.type == "analog_clock" }
            val migratedSlides = if (hasSystemSlides) {
                current.slides
            } else {
                id.algaffar.jwstv.domain.model.SlideItem.defaultSystemSlides() + current.slides
            }
            current = current.copy(
                schemaVersion = 3,
                slides = migratedSlides
            )
        }
        if (current.schemaVersion < 4) {
            // Existing installations upgraded from v<=3 already configured their mosque;
            // mark onboardingCompleted = true so they don't get interrupted on upgrade.
            current = current.copy(
                schemaVersion = 4,
                onboardingCompleted = if (fromExistingDisk) true else current.onboardingCompleted
            )
        }
        return current
    }

    suspend fun updateSettings(transform: (AppStateSettings) -> AppStateSettings): AppStateSettings {
        return mutex.withLock {
            val updated = transform(_settingsFlow.value)
            _settingsFlow.value = updated
            persistToDisk(updated)
            updated
        }
    }

    suspend fun replaceSettings(newSettings: AppStateSettings) {
        mutex.withLock {
            _settingsFlow.value = newSettings
            persistToDisk(newSettings)
        }
    }

    /**
     * Imports a raw JSON string from a backup file (including legacy Tauri `settings.json`).
     * Automatically normalizes legacy `asset://localhost/` paths to local `jws-assets/` relative paths.
     */
    suspend fun importBackupJson(rawJson: String): Result<AppStateSettings> {
        return runCatching {
            val normalizedJson = migrateLegacyJsonPaths(rawJson)
            val parsed = migrateSettingsSchema(
                jsonSerializer.decodeFromString<AppStateSettings>(normalizedJson),
                fromExistingDisk = true
            )
            replaceSettings(parsed)
            parsed
        }
    }

    fun exportBackupJson(): String {
        return jsonSerializer.encodeToString(_settingsFlow.value)
    }

    private suspend fun persistToDisk(settings: AppStateSettings) = withContext(ioDispatcher) {
        val content = jsonSerializer.encodeToString(settings)
        writeAtomicWithSync(storageFile, content)
        // Maintain verified `.bak` mirror for automatic recovery after sudden power loss
        runCatching { writeAtomicWithSync(backupFile, content) }
    }

    private fun writeAtomicWithSync(targetFile: File, content: String) {
        targetFile.parentFile?.mkdirs()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        FileOutputStream(tempFile).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            runCatching { fos.fd.sync() }
        }
        if (!tempFile.renameTo(targetFile)) {
            FileOutputStream(targetFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            tempFile.delete()
        }
    }

    companion object {
        /**
         * Normalizes Tauri WebView asset protocol URLs (e.g. `https://asset.localhost/.../jws-assets/...`
         * or `asset://localhost/.../jws-assets/...`) into clean relative `jws-assets/...` paths.
         */
        fun migrateLegacyJsonPaths(rawJson: String): String {
            var cleaned = rawJson
            val regex = Regex("""(?:https?://asset\.localhost/|asset://localhost/)[^"]+""")
            cleaned = regex.replace(cleaned) { matchResult ->
                val decoded = matchResult.value
                    .replace("%5C", "/")
                    .replace("%5c", "/")
                    .replace("%2F", "/")
                    .replace("%2f", "/")
                    .replace("%20", " ")
                    .replace("\\\\", "/")
                val markerIdx = decoded.indexOf("jws-assets/", ignoreCase = true)
                if (markerIdx >= 0) {
                    decoded.substring(markerIdx)
                } else {
                    matchResult.value
                }
            }
            return cleaned
        }
    }
}
