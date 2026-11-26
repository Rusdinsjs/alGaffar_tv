package id.algaffar.jwstv.data.media

import id.algaffar.jwstv.SharedHttpClient
import id.algaffar.jwstv.config.AppEndpoints
import id.algaffar.jwstv.data.update.OtaUpdateManager
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideshowGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

@Serializable
data class ImagePackAsset(
    val target: String = ImagePackDownloader.TARGET_SLIDE_IMAGE,
    val title: String = "",
    val fileName: String = "",
    val url: String = "",
    val sha256: String = "",
    val duration: Int = 12,
    val overwrite: Boolean = true
)

@Serializable
data class ImagePackItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val sizeLabel: String = "",
    val replaceExistingLists: Boolean = false,
    val assets: List<ImagePackAsset> = emptyList()
)

@Serializable
data class ImagePacksCatalog(
    val packs: List<ImagePackItem> = emptyList()
)

data class AppliedImagePackAsset(
    val target: String,
    val title: String,
    val relativePath: String,
    val duration: Int = 12,
    val overwrite: Boolean = true,
    val savedBytes: Long = 0L
)

data class ImagePackDownloadResult(
    val packId: String,
    val packTitle: String,
    val appliedAssets: List<AppliedImagePackAsset>,
    val skippedCount: Int = 0,
    val totalSavedBytes: Long = 0L
) {
    val downloadedCount: Int
        get() = appliedAssets.size
}

/**
 * 1-Click CDN Image Pack downloader for Al-Gaffar TV.
 * Downloads, SHA-256 verifies, auto-optimizes (1080p / 512px logo), and maps all mosque
 * image slots in one click:
 * - Mosque Logo (`logo`) & Custom QRIS (`masjid_qr`)
 * - Slideshow Poster Slides (`slide_image`)
 * - Tartil & Tarhim Multi-Slide Backgrounds (`tartil_image`, `tarhim_image`)
 * - Do'a Setelah Adzan (`doa_adzan`)
 * - 4 Khutbah & Ceramah Backgrounds (`khutbah_jumat`, `khutbah_idul_fitri`, `khutbah_idul_adha`, `ceramah_tarawih`)
 * - 5 Pelaksanaan Sholat Backgrounds (`sholat_5_waktu`, `sholat_jumat`, `sholat_idul_fitri`, `sholat_idul_adha`, `sholat_tarawih_witir`)
 */
class ImagePackDownloader(
    private val httpClient: OkHttpClient = SharedHttpClient.baseClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseCatalog(rawJson: String): List<ImagePackItem> =
        json.decodeFromString<ImagePacksCatalog>(rawJson).packs

    suspend fun fetchCatalog(
        manifestUrl: String = AppEndpoints.IMAGE_PACKS_MANIFEST_URL
    ): Result<List<ImagePackItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(manifestUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Katalog paket gambar tidak dapat dijangkau (HTTP ${resp.code})")
                }
                val body = resp.body?.string()?.trim().orEmpty()
                if (body.isEmpty()) {
                    throw IllegalStateException("Katalog paket gambar kosong")
                }
                parseCatalog(body)
            }
        }
    }

    suspend fun downloadPack(
        pack: ImagePackItem,
        assetsRootDir: File,
        onAssetProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<ImagePackDownloadResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(pack.assets.isNotEmpty()) {
                "Paket gambar '${pack.title}' tidak memiliki daftar aset."
            }

            val applied = mutableListOf<AppliedImagePackAsset>()
            var skipped = 0
            var totalBytes = 0L
            val total = pack.assets.size

            pack.assets.forEachIndexed { idx, asset ->
                val normalizedTarget = normalizeTarget(asset.target)
                if (normalizedTarget == null) {
                    skipped++
                    onAssetProgress(idx + 1, total)
                    return@forEachIndexed
                }

                val url = asset.url.trim()
                require(url.startsWith("https://", ignoreCase = true)) {
                    "URL gambar wajib HTTPS: $url"
                }

                val req = Request.Builder().url(url).get().build()
                val rawBytes = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Gagal mengunduh gambar '$normalizedTarget' (HTTP ${resp.code})")
                    }
                    val stream = resp.body?.byteStream()
                        ?: throw IllegalStateException("Stream gambar kosong untuk '$normalizedTarget'")
                    readBoundedBytes(stream, MediaStorageManager.MAX_IMAGE_UPLOAD_BYTES)
                }

                if (rawBytes.isEmpty()) {
                    throw IllegalStateException("File gambar '$normalizedTarget' kosong (0 byte)")
                }

                if (asset.sha256.isNotBlank()) {
                    val actualHash = computeBytesSha256(rawBytes)
                    if (!OtaUpdateManager.verifySha256(actualHash, asset.sha256)) {
                        throw SecurityException("Checksum SHA-256 gambar '$normalizedTarget' tidak cocok.")
                    }
                }

                val subFolder = subFolderForTarget(normalizedTarget)
                val maxWidth = when (normalizedTarget) {
                    TARGET_LOGO -> 512
                    TARGET_MASJID_QR -> 1080
                    else -> 1920
                }
                val maxHeight = when (normalizedTarget) {
                    TARGET_LOGO -> 512
                    TARGET_MASJID_QR -> 1080
                    else -> 1080
                }

                val derivedName = deriveFileName(asset, normalizedTarget, idx + 1)
                val saved = MediaStorageManager.optimizeAndSaveImage(
                    assetsRootDir = assetsRootDir,
                    subFolder = subFolder,
                    rawFileName = derivedName,
                    inputStream = rawBytes.inputStream(),
                    maxWidth = maxWidth,
                    maxHeight = maxHeight
                ).getOrThrow()

                totalBytes += saved.savedBytes
                applied.add(
                    AppliedImagePackAsset(
                        target = normalizedTarget,
                        title = asset.title.trim().ifBlank {
                            derivedName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
                        },
                        relativePath = saved.relativePath,
                        duration = asset.duration.coerceIn(4, 120),
                        overwrite = asset.overwrite,
                        savedBytes = saved.savedBytes
                    )
                )
                onAssetProgress(idx + 1, total)
            }

            if (applied.isEmpty()) {
                throw IllegalStateException("Tidak ada aset gambar valid yang dapat dipasang dari paket '${pack.title}'.")
            }

            ImagePackDownloadResult(
                packId = pack.id.ifBlank { "default_pack" },
                packTitle = pack.title.ifBlank { "Paket Gambar Masjid" },
                appliedAssets = applied,
                skippedCount = skipped,
                totalSavedBytes = totalBytes
            )
        }
    }

    companion object {
        const val TARGET_LOGO = "logo"
        const val TARGET_MASJID_QR = "masjid_qr"
        const val TARGET_SLIDE_IMAGE = "slide_image"
        const val TARGET_TARTIL_IMAGE = "tartil_image"
        const val TARGET_TARHIM_IMAGE = "tarhim_image"
        const val TARGET_DOA_ADZAN = "doa_adzan"
        const val TARGET_KHUTBAH_JUMAT = "khutbah_jumat"
        const val TARGET_KHUTBAH_IDUL_FITRI = "khutbah_idul_fitri"
        const val TARGET_KHUTBAH_IDUL_ADHA = "khutbah_idul_adha"
        const val TARGET_CERAMAH_TARAWIH = "ceramah_tarawih"
        const val TARGET_SHOLAT_5_WAKTU = "sholat_5_waktu"
        const val TARGET_SHOLAT_JUMAT = "sholat_jumat"
        const val TARGET_SHOLAT_IDUL_FITRI = "sholat_idul_fitri"
        const val TARGET_SHOLAT_IDUL_ADHA = "sholat_idul_adha"
        const val TARGET_SHOLAT_TARAWIH_WITIR = "sholat_tarawih_witir"

        val ALL_TARGETS: Set<String> = setOf(
            TARGET_LOGO,
            TARGET_MASJID_QR,
            TARGET_SLIDE_IMAGE,
            TARGET_TARTIL_IMAGE,
            TARGET_TARHIM_IMAGE,
            TARGET_DOA_ADZAN,
            TARGET_KHUTBAH_JUMAT,
            TARGET_KHUTBAH_IDUL_FITRI,
            TARGET_KHUTBAH_IDUL_ADHA,
            TARGET_CERAMAH_TARAWIH,
            TARGET_SHOLAT_5_WAKTU,
            TARGET_SHOLAT_JUMAT,
            TARGET_SHOLAT_IDUL_FITRI,
            TARGET_SHOLAT_IDUL_ADHA,
            TARGET_SHOLAT_TARAWIH_WITIR
        )

        fun normalizeTarget(raw: String?): String? {
            val cleaned = raw?.trim()?.lowercase(Locale.US)?.replace('-', '_') ?: return null
            val aliased = when (cleaned) {
                "slide", "poster", "slideshow" -> TARGET_SLIDE_IMAGE
                "tartil" -> TARGET_TARTIL_IMAGE
                "tarhim" -> TARGET_TARHIM_IMAGE
                "qr", "qris" -> TARGET_MASJID_QR
                else -> cleaned
            }
            return aliased.takeIf { it in ALL_TARGETS }
        }

        fun subFolderForTarget(target: String): String = when (target) {
            TARGET_LOGO -> "logo"
            TARGET_MASJID_QR -> "qr"
            TARGET_SLIDE_IMAGE -> "slides"
            TARGET_TARTIL_IMAGE -> "tartil"
            TARGET_TARHIM_IMAGE -> "tarhim"
            else -> "media"
        }

        fun computeBytesSha256(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(bytes).joinToString("") { "%02x".format(it) }
        }

        /**
         * Pure function that maps every downloaded image from [result] into its exact slot
         * inside [prev] (`brand`, `masjidQr`, `slideGroups`/`slides`, and `media`).
         */
        fun applyToSettings(
            prev: AppStateSettings,
            pack: ImagePackItem,
            result: ImagePackDownloadResult
        ): AppStateSettings {
            var brand = prev.brand
            var masjidQr = prev.masjidQr
            var media = prev.media

            val newTartilPaths = mutableListOf<String>()
            val newTarhimPaths = mutableListOf<String>()
            val newSlideAssets = mutableListOf<AppliedImagePackAsset>()

            for (item in result.appliedAssets) {
                when (item.target) {
                    TARGET_LOGO -> {
                        if (item.overwrite || brand.logo.isBlank()) {
                            brand = brand.copy(logo = item.relativePath)
                        }
                    }
                    TARGET_MASJID_QR -> {
                        if (item.overwrite || masjidQr.customQrImage.isBlank()) {
                            masjidQr = masjidQr.copy(customQrImage = item.relativePath)
                        }
                    }
                    TARGET_SLIDE_IMAGE -> newSlideAssets.add(item)
                    TARGET_TARTIL_IMAGE -> newTartilPaths.add(item.relativePath)
                    TARGET_TARHIM_IMAGE -> newTarhimPaths.add(item.relativePath)
                    TARGET_DOA_ADZAN -> {
                        if (item.overwrite || media.doaAdzanImage.isBlank()) {
                            media = media.copy(doaAdzanImage = item.relativePath, doaAdzanEnabled = true)
                        }
                    }
                    TARGET_KHUTBAH_JUMAT -> {
                        if (item.overwrite || media.khutbahJumatImage.isBlank()) {
                            media = media.copy(khutbahJumatImage = item.relativePath)
                        }
                    }
                    TARGET_KHUTBAH_IDUL_FITRI -> {
                        if (item.overwrite || media.khutbahIdulFitriImage.isBlank()) {
                            media = media.copy(khutbahIdulFitriImage = item.relativePath)
                        }
                    }
                    TARGET_KHUTBAH_IDUL_ADHA -> {
                        if (item.overwrite || media.khutbahIdulAdhaImage.isBlank()) {
                            media = media.copy(khutbahIdulAdhaImage = item.relativePath)
                        }
                    }
                    TARGET_CERAMAH_TARAWIH -> {
                        if (item.overwrite || media.ceramahTarawihImage.isBlank()) {
                            media = media.copy(ceramahTarawihImage = item.relativePath)
                        }
                    }
                    TARGET_SHOLAT_5_WAKTU -> {
                        if (item.overwrite || media.sholat5WaktuImage.isBlank()) {
                            media = media.copy(sholat5WaktuImage = item.relativePath)
                        }
                    }
                    TARGET_SHOLAT_JUMAT -> {
                        if (item.overwrite || media.sholatJumatImage.isBlank()) {
                            media = media.copy(sholatJumatImage = item.relativePath)
                        }
                    }
                    TARGET_SHOLAT_IDUL_FITRI -> {
                        if (item.overwrite || media.sholatIdulFitriImage.isBlank()) {
                            media = media.copy(sholatIdulFitriImage = item.relativePath)
                        }
                    }
                    TARGET_SHOLAT_IDUL_ADHA -> {
                        if (item.overwrite || media.sholatIdulAdhaImage.isBlank()) {
                            media = media.copy(sholatIdulAdhaImage = item.relativePath)
                        }
                    }
                    TARGET_SHOLAT_TARAWIH_WITIR -> {
                        if (item.overwrite || media.sholatTarawihWitirImage.isBlank()) {
                            media = media.copy(sholatTarawihWitirImage = item.relativePath)
                        }
                    }
                }
            }

            if (newTartilPaths.isNotEmpty()) {
                val mergedTartil = if (pack.replaceExistingLists) {
                    newTartilPaths.distinct()
                } else {
                    (media.tartilImages + newTartilPaths).distinct()
                }
                media = media.copy(tartilMediaType = "image", tartilImages = mergedTartil)
            }

            if (newTarhimPaths.isNotEmpty()) {
                val mergedTarhim = if (pack.replaceExistingLists) {
                    newTarhimPaths.distinct()
                } else {
                    (media.tarhimImages + newTarhimPaths).distinct()
                }
                media = media.copy(tarhimMediaType = "image", tarhimImages = mergedTarhim)
            }

            var updatedGroups = prev.slideGroups
            var updatedSlides = prev.slides

            if (newSlideAssets.isNotEmpty()) {
                val safePackSlug = result.packId
                    .lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9_-]"), "_")
                    .ifBlank { "default" }
                val idPrefix = "slide_pack_${safePackSlug}_"

                val packSlideItems = newSlideAssets.mapIndexed { idx, asset ->
                    SlideItem(
                        id = "${idPrefix}${idx + 1}",
                        title = asset.title.ifBlank { "Poster ${result.packTitle} #${idx + 1}" },
                        type = "image",
                        imageUrl = asset.relativePath,
                        duration = asset.duration.coerceIn(4, 120),
                        enabled = true,
                        showInMain = true,
                        showInSlideshow = true
                    )
                }

                if (updatedGroups.isEmpty()) {
                    updatedGroups = listOf(
                        SlideshowGroup(
                            id = "group-default-1",
                            name = "Group 1",
                            enabled = true,
                            slides = SlideItem.defaultSystemSlides() + packSlideItems,
                            overlayPosition = SlideshowGroup.POS_BOTTOM_LEFT,
                            showQrCard = true,
                            showInMain = true,
                            showInSlideshow = true
                        )
                    )
                } else {
                    // Replace previous slides belonging to the same pack ID in-place (idempotent)
                    // while preserving all manually added mosque slides.
                    updatedGroups = updatedGroups.mapIndexed { idx, grp ->
                        val cleaned = grp.slides.filterNot { it.id.startsWith(idPrefix) }
                        if (idx == 0) {
                            grp.copy(slides = cleaned + packSlideItems)
                        } else {
                            grp.copy(slides = cleaned)
                        }
                    }
                }
                updatedSlides = updatedGroups.flatMap { it.slides }
            }

            return prev.copy(
                brand = brand,
                masjidQr = masjidQr,
                media = media,
                slideGroups = updatedGroups,
                slides = updatedSlides
            )
        }

        private fun deriveFileName(asset: ImagePackAsset, normalizedTarget: String, index: Int): String {
            val explicit = asset.fileName.trim()
            if (explicit.isNotBlank()) return MediaStorageManager.sanitizeFileName(explicit)

            val urlTail = asset.url.substringBefore('?').substringAfterLast('/').trim()
            if (urlTail.isNotBlank() && urlTail.contains('.')) {
                return MediaStorageManager.sanitizeFileName(urlTail)
            }
            return "${normalizedTarget}_$index.jpg"
        }

        private fun readBoundedBytes(inputStream: InputStream, maxBytes: Long): ByteArray {
            val baos = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            var read: Int
            while (inputStream.read(buf).also { read = it } != -1) {
                total += read
                if (total > maxBytes) {
                    throw IllegalArgumentException(
                        "Ukuran gambar melebihi batas maksimal (${MediaStorageManager.formatByteSize(maxBytes)})"
                    )
                }
                baos.write(buf, 0, read)
            }
            return baos.toByteArray()
        }
    }
}
