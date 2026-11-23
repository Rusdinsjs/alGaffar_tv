package id.algaffar.jwstv.data.media

import id.algaffar.jwstv.SharedHttpClient
import id.algaffar.jwstv.config.AppEndpoints
import id.algaffar.jwstv.data.update.OtaUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@Serializable
data class AudioPackTrack(
    val category: String = "adzan", // "tartil" | "tarhim" | "adzan" | "custom"
    val slot: String = "default",   // "fajr" | "dhuhr" | "asr" | "maghrib" | "isha" | "default" | filename
    val url: String = "",
    val sha256: String = ""
)

@Serializable
data class AudioPackItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val sizeLabel: String = "",
    val tracks: List<AudioPackTrack> = emptyList()
)

@Serializable
data class AudioPacksCatalog(
    val packs: List<AudioPackItem> = emptyList()
)

/**
 * 1-Click CDN Audio Pack downloader for Al-Gaffar TV.
 * Keeps the base APK lean while allowing mosque caretakers or technicians to fetch
 * royalty-free Tartil, Tarhim, Adzan, and Sirine audio packs directly into `jws-assets/audio/`.
 */
class AudioPackDownloader(
    private val httpClient: OkHttpClient = SharedHttpClient.baseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchCatalog(
        manifestUrl: String = AppEndpoints.AUDIO_PACKS_MANIFEST_URL
    ): Result<List<AudioPackItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(manifestUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Katalog paket audio tidak dapat dijangkau (HTTP ${resp.code})")
                }
                val body = resp.body?.string()?.trim().orEmpty()
                json.decodeFromString<AudioPacksCatalog>(body).packs
            }
        }
    }

    suspend fun downloadPack(
        pack: AudioPackItem,
        assetsRootDir: File,
        onTrackProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(pack.tracks.isNotEmpty()) { "Paket audio '${pack.title}' tidak memiliki daftar trek." }
            var downloadedCount = 0
            val total = pack.tracks.size
            for (track in pack.tracks) {
                require(track.url.startsWith("https://", ignoreCase = true)) {
                    "URL trek audio wajib HTTPS: ${track.url}"
                }
                val safeCategory = track.category.lowercase()
                val relPath = when (safeCategory) {
                    "tartil", "tarhim", "adzan" -> {
                        val slot = track.slot.lowercase()
                            .takeIf { it in setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumat", "default") }
                            ?: "default"
                        "audio/$safeCategory/$slot.mp3"
                    }
                    else -> {
                        val clean = MediaStorageManager.sanitizeFileName(track.slot.ifBlank { "track_${downloadedCount + 1}.mp3" })
                        val finalName = if (clean.contains('.')) clean else "$clean.mp3"
                        "audio/custom/$finalName"
                    }
                }
                val req = Request.Builder().url(track.url).get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Gagal mengunduh ${track.slot} (HTTP ${resp.code})")
                    }
                    val stream = resp.body?.byteStream()
                        ?: throw IllegalStateException("Stream audio kosong untuk ${track.slot}")
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = relPath,
                        inputStream = stream,
                        maxBytes = MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
                    ).getOrThrow()

                    if (track.sha256.isNotBlank()) {
                        val actual = OtaUpdateManager.computeFileSha256(saved.savedFile)
                        if (!OtaUpdateManager.verifySha256(actual, track.sha256)) {
                            saved.savedFile.delete()
                            throw SecurityException("Checksum SHA-256 trek ${track.slot} tidak cocok.")
                        }
                    }
                }
                downloadedCount++
                onTrackProgress(downloadedCount, total)
            }
            downloadedCount
        }
    }
}
