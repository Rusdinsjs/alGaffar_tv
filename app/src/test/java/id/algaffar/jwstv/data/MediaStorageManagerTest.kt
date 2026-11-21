package id.algaffar.jwstv.data

import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.BrandSettings
import id.algaffar.jwstv.domain.model.MediaDisplaySettings
import id.algaffar.jwstv.domain.model.SlideItem
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class MediaStorageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `Storage Guard rejects writes when usable space falls below 300 MB threshold`() {
        val assetsDir = tempFolder.newFolder("jws-assets")

        val ok = MediaStorageManager.ensureSpaceAvailable(
            assetsRootDir = assetsDir,
            incomingBytes = 50L * 1024 * 1024,
            minFreeBytes = 300L * 1024 * 1024,
            usableSpaceProvider = { 500L * 1024 * 1024 }
        )
        assertTrue(ok.isSuccess)

        val low = MediaStorageManager.ensureSpaceAvailable(
            assetsRootDir = assetsDir,
            incomingBytes = 50L * 1024 * 1024,
            minFreeBytes = 300L * 1024 * 1024,
            usableSpaceProvider = { 320L * 1024 * 1024 }
        )
        assertTrue(low.isFailure)
        assertTrue(low.exceptionOrNull()?.message.orEmpty().contains("Memori internal TV hampir penuh"))
    }

    @Test
    fun `saveStreamWithGuard streams atomically and enforces max size cap without leaving tmp files`() {
        val assetsDir = tempFolder.newFolder("jws-assets")
        val payload = ByteArray(1024) { 42 }

        val saved = MediaStorageManager.saveStreamWithGuard(
            assetsRootDir = assetsDir,
            targetRelativePath = "videos/profil_masjid.mp4",
            inputStream = ByteArrayInputStream(payload),
            maxBytes = 2048L,
            usableSpaceProvider = { 1_000_000_000L }
        )
        assertTrue(saved.isSuccess)
        assertEquals("jws-assets/videos/profil_masjid.mp4", saved.getOrThrow().relativePath)
        assertEquals(1024L, saved.getOrThrow().savedBytes)
        assertTrue(File(assetsDir, "videos/profil_masjid.mp4").exists())

        // Exceeding maxBytes should fail and leave zero .tmp files behind
        val oversized = MediaStorageManager.saveStreamWithGuard(
            assetsRootDir = assetsDir,
            targetRelativePath = "videos/too_big.mp4",
            inputStream = ByteArrayInputStream(ByteArray(4096) { 1 }),
            maxBytes = 2048L,
            usableSpaceProvider = { 1_000_000_000L }
        )
        assertTrue(oversized.isFailure)
        assertFalse(File(assetsDir, "videos/too_big.mp4").exists())
        val leftoverTmps = File(assetsDir, "videos").listFiles().orEmpty().filter { it.name.endsWith(".tmp") }
        assertTrue("Leftover .tmp files must be cleaned", leftoverTmps.isEmpty())
    }

    @Test
    fun `Orphan Media Cleaner removes unreferenced images and videos while preserving active assets and all audio`() {
        val assetsDir = tempFolder.newFolder("jws-assets")

        val activeLogo = File(assetsDir, "images/logo/active_logo.png").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(500))
        }
        val oldLogo = File(assetsDir, "images/logo/old_logo.png").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(700))
        }
        val activeSlide = File(assetsDir, "images/slides/poster_kajian.jpg").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(1200))
        }
        val orphanSlide = File(assetsDir, "images/slides/deleted_poster.jpg").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(1500))
        }
        val activeBg = File(assetsDir, "images/media/khutbah_bg.jpg").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(800))
        }
        val activeVideo = File(assetsDir, "videos/profil.mp4").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(2000))
        }
        val orphanVideo = File(assetsDir, "videos/deleted_video.mp4").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(3000))
        }
        val prayerAudio = File(assetsDir, "audio/adzan/fajr.mp3").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(2500))
        }
        val customAudio = File(assetsDir, "audio/custom/murottal.mp3").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(2500))
        }

        val settings = AppStateSettings(
            brand = BrandSettings(logo = "jws-assets/images/logo/active_logo.png"),
            slides = listOf(
                SlideItem(id = "s1", type = "image", imageUrl = "jws-assets/images/slides/poster_kajian.jpg"),
                SlideItem(id = "s2", type = "video", videoUrl = "jws-assets/videos/profil.mp4")
            ),
            media = MediaDisplaySettings(khutbahJumatImage = "jws-assets/images/media/khutbah_bg.jpg")
        )

        val healthBefore = MediaStorageManager.inspectStorage(assetsDir, settings)
        assertEquals(3, healthBefore.orphanFileCount)
        assertEquals(700L + 1500L + 3000L, healthBefore.orphanBytes)

        val cleaned = MediaStorageManager.cleanOrphanMedia(assetsDir, settings)
        assertEquals(3, cleaned.deletedCount)
        assertEquals(700L + 1500L + 3000L, cleaned.freedBytes)

        // Active files & ALL audio files must remain intact
        assertTrue(activeLogo.exists())
        assertTrue(activeSlide.exists())
        assertTrue(activeBg.exists())
        assertTrue(activeVideo.exists())
        assertTrue(prayerAudio.exists())
        assertTrue(customAudio.exists())

        // Orphans must be gone
        assertFalse(oldLogo.exists())
        assertFalse(orphanSlide.exists())
        assertFalse(orphanVideo.exists())

        val healthAfter = MediaStorageManager.inspectStorage(assetsDir, settings)
        assertEquals(0, healthAfter.orphanFileCount)
        assertEquals(0L, healthAfter.orphanBytes)
    }

    @Test
    fun `syncFromExternalFolder bulk imports images, videos, and audio from both flat and structured USB folders`() {
        val assetsDir = tempFolder.newFolder("jws-assets")
        val usbDir = tempFolder.newFolder("usb-drive")

        // Mixed flat + structured media inside USB drive
        File(usbDir, "logo.png").writeBytes(ByteArray(300) { 1 })
        File(usbDir, "poster_ramadhan.jpg").writeBytes(ByteArray(600) { 2 })
        File(usbDir, "profil_masjid.mp4").writeBytes(ByteArray(1000) { 3 })
        File(usbDir, "tarhim_fajr.mp3").writeBytes(ByteArray(500) { 4 })
        File(usbDir, "murottal_al_kahfi.mp3").writeBytes(ByteArray(700) { 5 })
        File(usbDir, "audio/adzan/maghrib.mp3").apply {
            parentFile?.mkdirs(); writeBytes(ByteArray(800) { 6 })
        }

        val syncRes = MediaStorageManager.syncFromExternalFolder(
            sourceDir = usbDir,
            assetsRootDir = assetsDir,
            usableSpaceProvider = { 2_000_000_000L }
        )
        assertTrue(syncRes.isSuccess)
        val summary = syncRes.getOrThrow()

        assertEquals(2, summary.importedImages) // logo.png + poster_ramadhan.jpg
        assertEquals(1, summary.importedVideos) // profil_masjid.mp4
        assertEquals(3, summary.importedAudio)  // tarhim_fajr + murottal + adzan/maghrib
        assertNotNull(summary.updatedLogoPath)
        assertTrue(summary.updatedLogoPath!!.startsWith("jws-assets/images/logo/"))

        assertEquals(2, summary.createdSlides.size)
        assertTrue(summary.createdSlides.any { it.type == "image" && it.imageUrl.startsWith("jws-assets/images/slides/") })
        assertTrue(summary.createdSlides.any { it.type == "video" && it.videoUrl.startsWith("jws-assets/videos/") })

        assertTrue(File(assetsDir, "audio/tarhim/fajr.mp3").exists())
        assertTrue(File(assetsDir, "audio/adzan/maghrib.mp3").exists())
        assertTrue(File(assetsDir, "audio/custom/murottal_al_kahfi.mp3").exists())

        // Guard: syncing from assetsRootDir itself must be rejected
        val selfSync = MediaStorageManager.syncFromExternalFolder(assetsDir, assetsDir)
        assertTrue(selfSync.isFailure)
    }

    @Test
    fun `KtorRemoteServer exposes storage telemetry in status JSON and enforces category upload caps`() {
        assertEquals(MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES, KtorRemoteServer.maxUploadBytesForCategory("slide_video"))
        assertEquals(MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES, KtorRemoteServer.maxUploadBytesForCategory("tarhim"))
        assertEquals(MediaStorageManager.MAX_IMAGE_UPLOAD_BYTES, KtorRemoteServer.maxUploadBytesForCategory("slide_image"))

        val server = KtorRemoteServer(
            authToken = "TEST1234",
            storageHealthProvider = {
                StorageHealthSnapshot(
                    usableInternalBytes = 200L * 1024 * 1024, // < 300 MB => low storage
                    totalInternalBytes = 8L * 1024 * 1024 * 1024,
                    jwsAssetsUsedBytes = 42L * 1024 * 1024,
                    totalFileCount = 14,
                    orphanFileCount = 2,
                    orphanBytes = 4L * 1024 * 1024
                )
            }
        )

        val status = server.buildStatusJson()
        val storageJson = status["system"]?.jsonObject?.get("storage")?.jsonObject
        assertNotNull(storageJson)
        assertEquals(true, storageJson?.get("isLowStorage")?.jsonPrimitive?.booleanOrNull)
        assertEquals(2, storageJson?.get("orphanCount")?.jsonPrimitive?.intOrNull)
        assertEquals(14, storageJson?.get("totalFiles")?.jsonPrimitive?.intOrNull)
    }
}
