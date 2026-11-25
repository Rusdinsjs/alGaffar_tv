package id.algaffar.jwstv.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.SlideItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

data class StorageHealthSnapshot(
    val usableInternalBytes: Long = 0L,
    val totalInternalBytes: Long = 0L,
    val jwsAssetsUsedBytes: Long = 0L,
    val imagesBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val totalFileCount: Int = 0,
    val orphanFileCount: Int = 0,
    val orphanBytes: Long = 0L,
    val minSafeFreeBytes: Long = MediaStorageManager.MIN_SAFE_FREE_BYTES
) {
    val isLowStorage: Boolean
        get() = usableInternalBytes in 1 until minSafeFreeBytes

    val internalUsedPercent: Int
        get() = if (totalInternalBytes > 0L) {
            (((totalInternalBytes - usableInternalBytes).coerceAtLeast(0L) * 100L) / totalInternalBytes)
                .toInt()
                .coerceIn(0, 100)
        } else {
            0
        }
}

data class OptimizedImageResult(
    val relativePath: String,
    val savedFile: File,
    val originalBytes: Long,
    val savedBytes: Long,
    val width: Int,
    val height: Int,
    val wasOptimized: Boolean
)

data class StreamSaveResult(
    val relativePath: String,
    val savedFile: File,
    val savedBytes: Long
)

data class OrphanCleanResult(
    val deletedCount: Int,
    val freedBytes: Long
)

data class BulkSyncSummary(
    val importedImages: Int = 0,
    val importedVideos: Int = 0,
    val importedAudio: Int = 0,
    val skippedFiles: Int = 0,
    val totalBytesCopied: Long = 0L,
    val createdSlides: List<SlideItem> = emptyList(),
    val updatedLogoPath: String? = null,
    val warnings: List<String> = emptyList()
) {
    val totalImported: Int
        get() = importedImages + importedVideos + importedAudio
}

/**
 * Centralized Storage Guard, Image Auto-Optimizer, Streaming Media Importer,
 * USB Folder Bulk Sync, and Orphan Media Cleaner for `jws-assets/`.
 */
object MediaStorageManager {

    /** Minimum free internal storage required on STB/TV to keep Android OS healthy (300 MB). */
    const val MIN_SAFE_FREE_BYTES: Long = 300L * 1024L * 1024L

    const val MAX_IMAGE_UPLOAD_BYTES: Long = 25L * 1024L * 1024L
    const val MAX_AUDIO_UPLOAD_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_VIDEO_UPLOAD_BYTES: Long = 250L * 1024L * 1024L

    val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp")
    val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "aac")
    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "m4v")
    private val PRAYER_SLOTS = setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "default")
    private val PRAYER_AUDIO_CATEGORIES = setOf("tartil", "tarhim", "adzan")

    fun formatByteSize(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return when {
            safe >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", safe / 1_073_741_824.0)
            safe >= 1_048_576L -> String.format(Locale.US, "%.1f MB", safe / 1_048_576.0)
            safe >= 1024L -> String.format(Locale.US, "%.0f KB", safe / 1024.0)
            else -> "$safe B"
        }
    }

    /**
     * Verifies that saving [incomingBytes] will leave at least [minFreeBytes] free on disk.
     */
    fun ensureSpaceAvailable(
        assetsRootDir: File,
        incomingBytes: Long,
        minFreeBytes: Long = MIN_SAFE_FREE_BYTES,
        usableSpaceProvider: (File) -> Long = { dir ->
            dir.mkdirs()
            dir.usableSpace
        }
    ): Result<Unit> {
        val usable = usableSpaceProvider(assetsRootDir)
        if (usable <= 0L) {
            // Filesystem could not report free space; allow safely rather than bricking import.
            return Result.success(Unit)
        }
        val remainingAfter = usable - incomingBytes.coerceAtLeast(0L)
        if (remainingAfter < minFreeBytes) {
            return Result.failure(
                IllegalStateException(
                    "Memori internal TV hampir penuh! Sisa saat ini ${formatByteSize(usable)}, " +
                        "batas aman minimal ${formatByteSize(minFreeBytes)}. " +
                        "Bersihkan file media tidak terpakai terlebih dahulu."
                )
            )
        }
        return Result.success(Unit)
    }

    /**
     * Computes internal storage usage, category breakdown, and orphan media statistics.
     */
    fun inspectStorage(
        assetsRootDir: File,
        settings: AppStateSettings,
        minFreeBytes: Long = MIN_SAFE_FREE_BYTES
    ): StorageHealthSnapshot {
        assetsRootDir.mkdirs()
        var imagesBytes = 0L
        var audioBytes = 0L
        var videosBytes = 0L
        var otherBytes = 0L
        var totalCount = 0

        if (assetsRootDir.exists() && assetsRootDir.isDirectory) {
            assetsRootDir.walkTopDown()
                .filter { it.isFile && !it.name.endsWith(".tmp") }
                .forEach { file ->
                    val rel = file.relativeTo(assetsRootDir).invariantSeparatorsPath.lowercase(Locale.US)
                    val len = file.length()
                    totalCount++
                    when {
                        rel.startsWith("images/") -> imagesBytes += len
                        rel.startsWith("audio/") -> audioBytes += len
                        rel.startsWith("videos/") -> videosBytes += len
                        else -> otherBytes += len
                    }
                }
        }

        val orphans = findOrphanMediaFiles(assetsRootDir, settings)
        val orphanBytes = orphans.sumOf { it.length() }

        return StorageHealthSnapshot(
            usableInternalBytes = assetsRootDir.usableSpace,
            totalInternalBytes = assetsRootDir.totalSpace,
            jwsAssetsUsedBytes = imagesBytes + audioBytes + videosBytes + otherBytes,
            imagesBytes = imagesBytes,
            audioBytes = audioBytes,
            videosBytes = videosBytes,
            otherBytes = otherBytes,
            totalFileCount = totalCount,
            orphanFileCount = orphans.size,
            orphanBytes = orphanBytes,
            minSafeFreeBytes = minFreeBytes
        )
    }

    /**
     * Collects all normalized relative asset paths (without `jws-assets/` prefix) actively referenced in [settings].
     */
    fun collectReferencedMediaPaths(settings: AppStateSettings): Set<String> {
        val rawPaths = mutableListOf<String>()
        rawPaths.add(settings.brand.logo)
        rawPaths.add(settings.masjidQr.customQrImage)

        settings.slides.forEach { slide ->
            rawPaths.add(slide.imageUrl)
            rawPaths.add(slide.videoUrl)
        }
        settings.slideGroups.forEach { group ->
            group.slides.forEach { slide ->
                rawPaths.add(slide.imageUrl)
                rawPaths.add(slide.videoUrl)
            }
        }

        val m = settings.media
        rawPaths.add(m.tartilVideo)
        rawPaths.add(m.tarhimVideo)
        rawPaths.addAll(m.tartilImages)
        rawPaths.addAll(m.tarhimImages)
        rawPaths.add(m.doaAdzanImage)
        rawPaths.add(m.khutbahJumatImage)
        rawPaths.add(m.khutbahIdulFitriImage)
        rawPaths.add(m.khutbahIdulAdhaImage)
        rawPaths.add(m.ceramahTarawihImage)
        rawPaths.add(m.sholat5WaktuImage)
        rawPaths.add(m.sholatJumatImage)
        rawPaths.add(m.sholatIdulFitriImage)
        rawPaths.add(m.sholatIdulAdhaImage)
        rawPaths.add(m.sholatTarawihWitirImage)

        return rawPaths.mapNotNull { normalizeAssetRelativePath(it) }.toSet()
    }

    internal fun normalizeAssetRelativePath(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://", true) ||
            trimmed.startsWith("https://", true) ||
            trimmed.startsWith("content://", true)
        ) {
            return null
        }
        val normalized = trimmed
            .replace('\\', '/')
            .removePrefix("file://")
            .let { path ->
                val marker = "jws-assets/"
                val idx = path.indexOf(marker)
                if (idx >= 0) path.substring(idx + marker.length) else path
            }
            .removePrefix("/")
        return normalized.takeIf { it.isNotBlank() }
    }

    /**
     * Finds unreferenced media files under `images/` and `videos/` (never touches `audio/`).
     */
    fun findOrphanMediaFiles(assetsRootDir: File, settings: AppStateSettings): List<File> {
        if (!assetsRootDir.exists() || !assetsRootDir.isDirectory) return emptyList()
        val referenced = collectReferencedMediaPaths(settings)
        val orphans = mutableListOf<File>()

        listOf("images", "videos").forEach { dirName ->
            val subDir = File(assetsRootDir, dirName)
            if (subDir.exists() && subDir.isDirectory) {
                subDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val rel = file.relativeTo(assetsRootDir).invariantSeparatorsPath
                        if (file.name.endsWith(".tmp") || rel !in referenced) {
                            orphans.add(file)
                        }
                    }
            }
        }
        return orphans
    }

    /**
     * Deletes unreferenced image/video files and stale `.tmp` files to reclaim STB internal space.
     */
    fun cleanOrphanMedia(assetsRootDir: File, settings: AppStateSettings): OrphanCleanResult {
        val orphans = findOrphanMediaFiles(assetsRootDir, settings)
        var deletedCount = 0
        var freedBytes = 0L
        for (file in orphans) {
            val size = file.length()
            if (runCatching { file.delete() }.getOrDefault(false)) {
                deletedCount++
                freedBytes += size
            }
        }
        return OrphanCleanResult(deletedCount = deletedCount, freedBytes = freedBytes)
    }

    /**
     * Streams an incoming binary asset directly to disk with chunked copy, size cap,
     * Storage Guard check, and atomic `.tmp` rename (zero RAM spike for large videos/audio).
     */
    fun saveStreamWithGuard(
        assetsRootDir: File,
        targetRelativePath: String,
        inputStream: InputStream,
        maxBytes: Long,
        minFreeBytes: Long = MIN_SAFE_FREE_BYTES,
        usableSpaceProvider: (File) -> Long = { dir ->
            dir.mkdirs()
            dir.usableSpace
        }
    ): Result<StreamSaveResult> {
        return runCatching {
            val safeRel = sanitizeRelativePath(targetRelativePath)
            val targetFile = File(assetsRootDir, safeRel)
            val canonicalRoot = assetsRootDir.canonicalFile
            val canonicalTarget = targetFile.canonicalFile
            if (!canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)) {
                throw SecurityException("Path tujuan tidak valid: $targetRelativePath")
            }

            // Pre-flight minimum free check before opening file
            ensureSpaceAvailable(assetsRootDir, 0L, minFreeBytes, usableSpaceProvider).getOrThrow()

            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.${System.nanoTime()}.tmp")

            var totalWritten = 0L
            try {
                FileOutputStream(tempFile).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var nextSpaceCheckAt = 2L * 1024L * 1024L // Check disk space every 2 MB
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        totalWritten += read
                        if (totalWritten > maxBytes) {
                            throw IllegalArgumentException(
                                "Ukuran file melebihi batas maksimal (${formatByteSize(maxBytes)})"
                            )
                        }
                        if (totalWritten >= nextSpaceCheckAt) {
                            ensureSpaceAvailable(assetsRootDir, 0L, minFreeBytes, usableSpaceProvider).getOrThrow()
                            nextSpaceCheckAt += 2L * 1024L * 1024L
                        }
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }

                if (totalWritten == 0L) {
                    throw IllegalArgumentException("File kosong (0 byte)")
                }

                ensureSpaceAvailable(assetsRootDir, 0L, minFreeBytes, usableSpaceProvider).getOrThrow()

                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                StreamSaveResult(
                    relativePath = "jws-assets/$safeRel",
                    savedFile = targetFile,
                    savedBytes = totalWritten
                )
            } catch (t: Throwable) {
                runCatching { tempFile.delete() }
                throw t
            }
        }
    }

    /**
     * Auto-resizes and compresses incoming images to fit 1080p canvas (or 512x512 for logos)
     * while enforcing [ensureSpaceAvailable]. Falls back cleanly to raw bytes if Android
     * [BitmapFactory] cannot decode (e.g., unit tests or exotic formats).
     */
    fun optimizeAndSaveImage(
        assetsRootDir: File,
        subFolder: String,
        rawFileName: String,
        inputStream: InputStream,
        maxWidth: Int = if (subFolder == "logo") 512 else 1920,
        maxHeight: Int = if (subFolder == "logo") 512 else 1080,
        jpegQuality: Int = 85,
        maxInputBytes: Long = MAX_IMAGE_UPLOAD_BYTES,
        minFreeBytes: Long = MIN_SAFE_FREE_BYTES,
        usableSpaceProvider: (File) -> Long = { dir ->
            dir.mkdirs()
            dir.usableSpace
        }
    ): Result<OptimizedImageResult> {
        return runCatching {
            val rawBytes = readBoundedBytes(inputStream, maxInputBytes)
            if (rawBytes.isEmpty()) {
                throw IllegalArgumentException("File gambar kosong")
            }
            ensureSpaceAvailable(assetsRootDir, rawBytes.size.toLong(), minFreeBytes, usableSpaceProvider).getOrThrow()

            val cleanSub = subFolder.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "media" }
            val safeBase = sanitizeFileName(rawFileName).substringBeforeLast('.').ifBlank { "image" }
            val originalExt = rawFileName.substringAfterLast('.', "jpg")
                .lowercase(Locale.US)
                .takeIf { it in IMAGE_EXTENSIONS } ?: "jpg"

            val optimized = tryOptimizeBitmapBytes(
                rawBytes = rawBytes,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                jpegQuality = jpegQuality,
                preferPng = (cleanSub == "logo" || originalExt == "png")
            )

            val finalBytes: ByteArray
            val finalExt: String
            val finalW: Int
            val finalH: Int
            val wasOptimized: Boolean

            if (optimized != null && (optimized.bytes.size < rawBytes.size || optimized.wasResized)) {
                finalBytes = optimized.bytes
                finalExt = optimized.ext
                finalW = optimized.width
                finalH = optimized.height
                wasOptimized = true
            } else {
                finalBytes = rawBytes
                finalExt = originalExt
                finalW = optimized?.width ?: 0
                finalH = optimized?.height ?: 0
                wasOptimized = false
            }

            ensureSpaceAvailable(assetsRootDir, finalBytes.size.toLong(), minFreeBytes, usableSpaceProvider).getOrThrow()

            val finalFileName = "${System.currentTimeMillis()}_${safeBase}.$finalExt"
            val targetDir = File(assetsRootDir, "images/$cleanSub").apply { mkdirs() }
            val targetFile = File(targetDir, finalFileName)
            val tempFile = File(targetDir, "$finalFileName.${System.nanoTime()}.tmp")
            try {
                tempFile.writeBytes(finalBytes)
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (t: Throwable) {
                runCatching { tempFile.delete() }
                throw t
            }

            OptimizedImageResult(
                relativePath = "jws-assets/images/$cleanSub/$finalFileName",
                savedFile = targetFile,
                originalBytes = rawBytes.size.toLong(),
                savedBytes = finalBytes.size.toLong(),
                width = finalW,
                height = finalH,
                wasOptimized = wasOptimized
            )
        }
    }

    /**
     * Bulk-imports media from an external Flashdisk/USB/local directory into internal `jws-assets/`.
     * Supports both:
     * 1. Structured folder layout (`audio/{tartil|tarhim|adzan|custom}/...`, `images/{logo|slides|media}/...`, `videos/...`)
     * 2. Flat/mixed folder of images (imported as poster slides), videos (imported as video slides),
     *    and audio (auto-routed by `tartil_fajr.mp3` naming or into `audio/custom/`).
     */
    fun syncFromExternalFolder(
        sourceDir: File,
        assetsRootDir: File,
        minFreeBytes: Long = MIN_SAFE_FREE_BYTES,
        usableSpaceProvider: (File) -> Long = { dir ->
            dir.mkdirs()
            dir.usableSpace
        }
    ): Result<BulkSyncSummary> {
        return runCatching {
            if (!sourceDir.exists() || !sourceDir.isDirectory || !sourceDir.canRead()) {
                throw IllegalArgumentException("Folder sumber tidak dapat dibaca: ${sourceDir.absolutePath}")
            }
            val canonicalAssets = assetsRootDir.canonicalPath
            val canonicalSource = sourceDir.canonicalPath
            if (canonicalSource == canonicalAssets || canonicalSource.startsWith("$canonicalAssets${File.separator}")) {
                throw IllegalArgumentException("Pilih folder dari Flashdisk USB atau Memori Eksternal, bukan folder internal JWS Assets.")
            }

            // If user selected a parent containing `jws-assets/`, sync from that subfolder directly
            val effectiveRoot = File(sourceDir, "jws-assets")
                .takeIf { it.exists() && it.isDirectory }
                ?: sourceDir

            var importedImages = 0
            var importedVideos = 0
            var importedAudio = 0
            var skippedFiles = 0
            var totalCopied = 0L
            var updatedLogo: String? = null
            val createdSlides = mutableListOf<SlideItem>()
            val warnings = mutableListOf<String>()
            var slideCounter = 0

            val candidateFiles = effectiveRoot.walkTopDown()
                .maxDepth(5)
                .filter { it.isFile && !it.name.startsWith(".") }
                .sortedBy { it.relativeTo(effectiveRoot).invariantSeparatorsPath.lowercase(Locale.US) }
                .toList()

            for (file in candidateFiles) {
                val ext = file.extension.lowercase(Locale.US)
                val relPath = file.relativeTo(effectiveRoot).invariantSeparatorsPath
                val segments = relPath.split('/').map { it.lowercase(Locale.US) }

                when {
                    ext in IMAGE_EXTENSIONS -> {
                        val isLogo = segments.any { it == "logo" } ||
                            file.nameWithoutExtension.equals("logo", ignoreCase = true)
                        val subFolder = when {
                            isLogo -> "logo"
                            segments.any { it == "media" || it == "backgrounds" } -> "media"
                            else -> "slides"
                        }
                        val res = runCatching {
                            FileInputStream(file).use { input ->
                                optimizeAndSaveImage(
                                    assetsRootDir = assetsRootDir,
                                    subFolder = subFolder,
                                    rawFileName = file.name,
                                    inputStream = input,
                                    minFreeBytes = minFreeBytes,
                                    usableSpaceProvider = usableSpaceProvider
                                ).getOrThrow()
                            }
                        }
                        res.onSuccess { saved ->
                            importedImages++
                            totalCopied += saved.savedBytes
                            if (isLogo) {
                                updatedLogo = saved.relativePath
                            } else if (subFolder == "slides") {
                                slideCounter++
                                val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
                                    .ifBlank { "Poster USB #$slideCounter" }
                                createdSlides.add(
                                    SlideItem(
                                        id = "slide_usb_${System.currentTimeMillis()}_$slideCounter",
                                        title = title,
                                        type = "image",
                                        imageUrl = saved.relativePath,
                                        duration = 12,
                                        enabled = true,
                                        showInMain = true,
                                        showInSlideshow = true
                                    )
                                )
                            }
                        }.onFailure { err ->
                            skippedFiles++
                            warnings.add("${file.name}: ${err.message}")
                        }
                    }

                    ext in VIDEO_EXTENSIONS -> {
                        val cleanName = "${System.currentTimeMillis()}_${sanitizeFileName(file.name)}"
                        val res = runCatching {
                            FileInputStream(file).use { input ->
                                saveStreamWithGuard(
                                    assetsRootDir = assetsRootDir,
                                    targetRelativePath = "videos/$cleanName",
                                    inputStream = input,
                                    maxBytes = MAX_VIDEO_UPLOAD_BYTES,
                                    minFreeBytes = minFreeBytes,
                                    usableSpaceProvider = usableSpaceProvider
                                ).getOrThrow()
                            }
                        }
                        res.onSuccess { saved ->
                            importedVideos++
                            totalCopied += saved.savedBytes
                            slideCounter++
                            val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ').trim()
                                .ifBlank { "Video USB #$slideCounter" }
                            createdSlides.add(
                                SlideItem(
                                    id = "slide_vid_${System.currentTimeMillis()}_$slideCounter",
                                    title = title,
                                    type = "video",
                                    videoUrl = saved.relativePath,
                                    duration = 30,
                                    enabled = true,
                                    showInMain = true,
                                    showInSlideshow = true
                                )
                            )
                        }.onFailure { err ->
                            skippedFiles++
                            warnings.add("${file.name}: ${err.message}")
                        }
                    }

                    ext in AUDIO_EXTENSIONS -> {
                        val targetRel = resolveAudioSyncTargetPath(segments, file.name)
                        val res = runCatching {
                            FileInputStream(file).use { input ->
                                saveStreamWithGuard(
                                    assetsRootDir = assetsRootDir,
                                    targetRelativePath = targetRel,
                                    inputStream = input,
                                    maxBytes = MAX_AUDIO_UPLOAD_BYTES,
                                    minFreeBytes = minFreeBytes,
                                    usableSpaceProvider = usableSpaceProvider
                                ).getOrThrow()
                            }
                        }
                        res.onSuccess { saved ->
                            importedAudio++
                            totalCopied += saved.savedBytes
                        }.onFailure { err ->
                            skippedFiles++
                            warnings.add("${file.name}: ${err.message}")
                        }
                    }

                    else -> skippedFiles++
                }
            }

            BulkSyncSummary(
                importedImages = importedImages,
                importedVideos = importedVideos,
                importedAudio = importedAudio,
                skippedFiles = skippedFiles,
                totalBytesCopied = totalCopied,
                createdSlides = createdSlides,
                updatedLogoPath = updatedLogo,
                warnings = warnings.take(5)
            )
        }
    }

    internal fun resolveAudioSyncTargetPath(lowerSegments: List<String>, rawFileName: String): String {
        val cleanName = sanitizeFileName(rawFileName)
        val ext = cleanName.substringAfterLast('.', "mp3").lowercase(Locale.US)
        val baseLower = cleanName.substringBeforeLast('.').lowercase(Locale.US)

        // 1. Structured folder: e.g. audio/tarhim/fajr/01.mp3 or tarhim/fajr.mp3
        val catIdx = lowerSegments.indexOfFirst { it in PRAYER_AUDIO_CATEGORIES }
        if (catIdx >= 0) {
            val category = lowerSegments[catIdx]
            val nextSeg = lowerSegments.getOrNull(catIdx + 1)
            return when {
                nextSeg != null && nextSeg in PRAYER_SLOTS && catIdx + 2 <= lowerSegments.lastIndex ->
                    "audio/$category/$nextSeg/$cleanName"
                baseLower in PRAYER_SLOTS ->
                    "audio/$category/$baseLower.$ext"
                else ->
                    "audio/$category/default/$cleanName"
            }
        }

        // 2. Prefix naming convention in flat folder: e.g. tarhim_fajr.mp3 or adzan-maghrib.mp3
        val prefixMatch = Regex("^(tartil|tarhim|adzan)[_-](fajr|dhuhr|asr|maghrib|isha|default)$")
            .find(baseLower)
        if (prefixMatch != null) {
            val cat = prefixMatch.groupValues[1]
            val slot = prefixMatch.groupValues[2]
            return "audio/$cat/$slot.$ext"
        }

        // 3. Default to custom audio library
        return "audio/custom/$cleanName"
    }

    private data class DecodedImageOutput(
        val bytes: ByteArray,
        val ext: String,
        val width: Int,
        val height: Int,
        val wasResized: Boolean
    )

    private fun tryOptimizeBitmapBytes(
        rawBytes: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        jpegQuality: Int,
        preferPng: Boolean
    ): DecodedImageOutput? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            while ((origW / (sampleSize * 2)) >= maxWidth && (origH / (sampleSize * 2)) >= maxHeight) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

            var working = decoded
            var wasResized = sampleSize > 1
            try {
                val curW = decoded.width
                val curH = decoded.height
                if (curW > maxWidth || curH > maxHeight) {
                    val scale = minOf(maxWidth.toFloat() / curW, maxHeight.toFloat() / curH)
                    val targetW = (curW * scale).toInt().coerceAtLeast(1)
                    val targetH = (curH * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                    if (scaled !== decoded) {
                        decoded.recycle()
                        working = scaled
                    }
                    wasResized = true
                }

                val keepPng = preferPng && working.hasAlpha()
                val outStream = ByteArrayOutputStream()
                val format = if (keepPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val quality = if (keepPng) 100 else jpegQuality.coerceIn(60, 95)
                val ok = working.compress(format, quality, outStream)
                if (!ok) return null

                DecodedImageOutput(
                    bytes = outStream.toByteArray(),
                    ext = if (keepPng) "png" else "jpg",
                    width = working.width,
                    height = working.height,
                    wasResized = wasResized
                )
            } finally {
                if (!working.isRecycled) {
                    working.recycle()
                }
            }
        }.getOrNull()
    }

    private fun readBoundedBytes(inputStream: InputStream, maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(32 * 1024)
        var total = 0L
        var read: Int
        while (inputStream.read(buf).also { read = it } != -1) {
            total += read
            if (total > maxBytes) {
                throw IllegalArgumentException(
                    "Ukuran file melebihi batas maksimal (${formatByteSize(maxBytes)})"
                )
            }
            out.write(buf, 0, read)
        }
        return out.toByteArray()
    }

    fun sanitizeFileName(raw: String): String {
        return raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trimStart('.')
            .ifBlank { "media_${System.currentTimeMillis()}" }
            .take(120)
    }

    private fun sanitizeRelativePath(rel: String): String {
        return rel.replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
            .ifBlank { throw IllegalArgumentException("Path target tidak boleh kosong") }
    }
}
