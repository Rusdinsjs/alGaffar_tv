package id.algaffar.jwstv.data.repository

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

/**
 * Atomic JSON-backed repository for [AppStateSettings].
 * Directly supports importing/restoring legacy Tauri `settings.json` files.
 */
class SettingsRepository(
    private val storageFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()

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
        return try {
            if (storageFile.exists() && storageFile.length() > 0L) {
                val raw = storageFile.readText(Charsets.UTF_8)
                jsonSerializer.decodeFromString<AppStateSettings>(migrateLegacyJsonPaths(raw))
            } else {
                AppStateSettings()
            }
        } catch (_: Exception) {
            AppStateSettings()
        }
    }

    suspend fun updateSettings(transform: (AppStateSettings) -> AppStateSettings): AppStateSettings {
        return mutex.withLock {
            val updated = transform(_settingsFlow.value)
            persistToDisk(updated)
            _settingsFlow.value = updated
            updated
        }
    }

    suspend fun replaceSettings(newSettings: AppStateSettings) {
        mutex.withLock {
            persistToDisk(newSettings)
            _settingsFlow.value = newSettings
        }
    }

    /**
     * Imports a raw JSON string from a backup file (including legacy Tauri `settings.json`).
     * Automatically normalizes legacy `asset://localhost/` paths to local `jws-assets/` relative paths.
     */
    suspend fun importBackupJson(rawJson: String): Result<AppStateSettings> {
        return runCatching {
            val normalizedJson = migrateLegacyJsonPaths(rawJson)
            val parsed = jsonSerializer.decodeFromString<AppStateSettings>(normalizedJson)
            replaceSettings(parsed)
            parsed
        }
    }

    fun exportBackupJson(): String {
        return jsonSerializer.encodeToString(_settingsFlow.value)
    }

    private suspend fun persistToDisk(settings: AppStateSettings) = withContext(ioDispatcher) {
        storageFile.parentFile?.mkdirs()
        val tempFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        val content = jsonSerializer.encodeToString(settings)
        tempFile.writeText(content, Charsets.UTF_8)
        if (!tempFile.renameTo(storageFile)) {
            storageFile.writeText(content, Charsets.UTF_8)
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
