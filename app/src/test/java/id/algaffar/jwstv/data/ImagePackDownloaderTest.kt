package id.algaffar.jwstv.data

import id.algaffar.jwstv.data.media.AppliedImagePackAsset
import id.algaffar.jwstv.data.media.ImagePackAsset
import id.algaffar.jwstv.data.media.ImagePackDownloadResult
import id.algaffar.jwstv.data.media.ImagePackDownloader
import id.algaffar.jwstv.data.media.ImagePackItem
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.SlideItem
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ImagePackDownloaderTest {

    @Test
    fun `example image-packs manifest parses cleanly with all targets valid`() {
        val candidates = listOf(
            File("docs/image-packs.example.json"),
            File("../docs/image-packs.example.json")
        )
        val manifestFile = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("docs/image-packs.example.json not found")

        val downloader = ImagePackDownloader()
        val packs = downloader.parseCatalog(manifestFile.readText(Charsets.UTF_8))

        assertTrue("Expected at least 2 packs in example manifest", packs.size >= 2)
        assertNotNull(packs.firstOrNull { it.id == "standard_image_pack" })
        assertNotNull(packs.firstOrNull { it.id == "haramain_image_pack" })

        for (pack in packs) {
            assertTrue("Pack ${pack.id} must have assets", pack.assets.isNotEmpty())
            for (asset in pack.assets) {
                assertNotNull(
                    "Invalid target '${asset.target}' in pack '${pack.id}'",
                    ImagePackDownloader.normalizeTarget(asset.target)
                )
                assertTrue(
                    "Asset URL must be HTTPS in pack '${pack.id}': ${asset.url}",
                    asset.url.startsWith("https://")
                )
            }
        }
    }

    @Test
    fun `applyToSettings populates every image upload slot in AppStateSettings`() {
        val pack = ImagePackItem(id = "standard_image_pack", title = "Paket Standar")
        val applied = listOf(
            AppliedImagePackAsset(ImagePackDownloader.TARGET_LOGO, "Logo", "jws-assets/images/logo/logo.png"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_MASJID_QR, "QRIS", "jws-assets/images/qr/qris.png"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SLIDE_IMAGE, "Poster 1", "jws-assets/images/slides/p1.jpg", duration = 15),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SLIDE_IMAGE, "Poster 2", "jws-assets/images/slides/p2.jpg", duration = 20),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_TARTIL_IMAGE, "Tartil 1", "jws-assets/images/tartil/t1.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_TARHIM_IMAGE, "Tarhim 1", "jws-assets/images/tarhim/th1.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_DOA_ADZAN, "Doa", "jws-assets/images/media/doa.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_KHUTBAH_JUMAT, "KJ", "jws-assets/images/media/kj.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_KHUTBAH_IDUL_FITRI, "KIF", "jws-assets/images/media/kif.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_KHUTBAH_IDUL_ADHA, "KIA", "jws-assets/images/media/kia.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_CERAMAH_TARAWIH, "CT", "jws-assets/images/media/ct.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SHOLAT_5_WAKTU, "S5", "jws-assets/images/media/s5.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SHOLAT_JUMAT, "SJ", "jws-assets/images/media/sj.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SHOLAT_IDUL_FITRI, "SIF", "jws-assets/images/media/sif.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SHOLAT_IDUL_ADHA, "SIA", "jws-assets/images/media/sia.jpg"),
            AppliedImagePackAsset(ImagePackDownloader.TARGET_SHOLAT_TARAWIH_WITIR, "STW", "jws-assets/images/media/stw.jpg")
        )
        val result = ImagePackDownloadResult(
            packId = pack.id,
            packTitle = pack.title,
            appliedAssets = applied,
            totalSavedBytes = 1024L * 1024L
        )

        val updated = ImagePackDownloader.applyToSettings(AppStateSettings(), pack, result)

        assertEquals("jws-assets/images/logo/logo.png", updated.brand.logo)
        assertEquals("jws-assets/images/qr/qris.png", updated.masjidQr.customQrImage)
        assertEquals(listOf("jws-assets/images/tartil/t1.jpg"), updated.media.tartilImages)
        assertEquals("image", updated.media.tartilMediaType)
        assertEquals(listOf("jws-assets/images/tarhim/th1.jpg"), updated.media.tarhimImages)
        assertEquals("image", updated.media.tarhimMediaType)
        assertEquals("jws-assets/images/media/doa.jpg", updated.media.doaAdzanImage)
        assertTrue(updated.media.doaAdzanEnabled)
        assertEquals("jws-assets/images/media/kj.jpg", updated.media.khutbahJumatImage)
        assertEquals("jws-assets/images/media/kif.jpg", updated.media.khutbahIdulFitriImage)
        assertEquals("jws-assets/images/media/kia.jpg", updated.media.khutbahIdulAdhaImage)
        assertEquals("jws-assets/images/media/ct.jpg", updated.media.ceramahTarawihImage)
        assertEquals("jws-assets/images/media/s5.jpg", updated.media.sholat5WaktuImage)
        assertEquals("jws-assets/images/media/sj.jpg", updated.media.sholatJumatImage)
        assertEquals("jws-assets/images/media/sif.jpg", updated.media.sholatIdulFitriImage)
        assertEquals("jws-assets/images/media/sia.jpg", updated.media.sholatIdulAdhaImage)
        assertEquals("jws-assets/images/media/stw.jpg", updated.media.sholatTarawihWitirImage)

        val packSlides = updated.slideGroups.first().slides.filter { it.id.startsWith("slide_pack_standard_image_pack_") }
        assertEquals(2, packSlides.size)
        assertEquals("Poster 1", packSlides[0].title)
        assertEquals(15, packSlides[0].duration)
        assertEquals("Poster 2", packSlides[1].title)
        assertEquals(20, packSlides[1].duration)
    }

    @Test
    fun `applyToSettings is idempotent for same pack, preserves manual slides, and respects overwrite=false`() {
        val manualSlide = SlideItem(
            id = "manual_mosque_slide",
            title = "Pengumuman Kajian",
            type = "image",
            imageUrl = "jws-assets/images/slides/manual.jpg"
        )
        val initial = AppStateSettings().let { base ->
            val grp0 = base.slideGroups.first().copy(slides = base.slideGroups.first().slides + manualSlide)
            base.copy(
                brand = base.brand.copy(logo = "jws-assets/images/logo/custom_mosque_logo.png"),
                slideGroups = listOf(grp0),
                slides = grp0.slides,
                media = base.media.copy(tartilImages = listOf("jws-assets/images/tartil/old.jpg"))
            )
        }

        val pack = ImagePackItem(id = "haramain_image_pack", title = "Haramain", replaceExistingLists = true)
        val res1 = ImagePackDownloadResult(
            packId = pack.id,
            packTitle = pack.title,
            appliedAssets = listOf(
                AppliedImagePackAsset(ImagePackDownloader.TARGET_LOGO, "Default Logo", "jws-assets/images/logo/pack.png", overwrite = false),
                AppliedImagePackAsset(ImagePackDownloader.TARGET_SLIDE_IMAGE, "Makkah", "jws-assets/images/slides/m1.jpg"),
                AppliedImagePackAsset(ImagePackDownloader.TARGET_TARTIL_IMAGE, "Tartil New", "jws-assets/images/tartil/new.jpg")
            )
        )

        val afterFirst = ImagePackDownloader.applyToSettings(initial, pack, res1)
        val afterSecond = ImagePackDownloader.applyToSettings(afterFirst, pack, res1)

        // Existing custom mosque logo must NOT be overwritten when overwrite = false
        assertEquals("jws-assets/images/logo/custom_mosque_logo.png", afterSecond.brand.logo)
        // replaceExistingLists = true replaces old tartil list
        assertEquals(listOf("jws-assets/images/tartil/new.jpg"), afterSecond.media.tartilImages)
        // Manual slide preserved, and pack slide NOT duplicated after second apply
        val slides = afterSecond.slideGroups.first().slides
        assertTrue(slides.any { it.id == "manual_mosque_slide" })
        assertEquals(1, slides.count { it.id.startsWith("slide_pack_haramain_image_pack_") })
    }

    @Test
    fun `downloadPack downloads via HTTPS, verifies SHA-256, saves assets, and rejects HTTP or bad checksum`() = runBlocking {
        val tempDir = Files.createTempDirectory("jws_image_pack_test").toFile()
        try {
            val payloadA = "FAKE-JPEG-PAYLOAD-A-1234567890".toByteArray(Charsets.UTF_8)
            val payloadB = "FAKE-PNG-PAYLOAD-B-0987654321".toByteArray(Charsets.UTF_8)
            val hashA = ImagePackDownloader.computeBytesSha256(payloadA)
            val hashB = ImagePackDownloader.computeBytesSha256(payloadB)

            val mockClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val url = chain.request().url.toString()
                    val bytes = when {
                        url.endsWith("sholat.jpg") -> payloadA
                        url.endsWith("poster.png") -> payloadB
                        else -> null
                    }
                    if (bytes != null) {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(bytes.toResponseBody("application/octet-stream".toMediaType()))
                            .build()
                    } else {
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("".toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                }
                .build()

            val downloader = ImagePackDownloader(httpClient = mockClient)

            // 1. Valid HTTPS + matching SHA-256 + unknown future target gracefully skipped
            val validPack = ImagePackItem(
                id = "test_pack",
                title = "Test Pack",
                assets = listOf(
                    ImagePackAsset(
                        target = "sholat_5_waktu",
                        fileName = "sholat.jpg",
                        url = "https://cdn.example.com/sholat.jpg",
                        sha256 = hashA
                    ),
                    ImagePackAsset(
                        target = "slide_image",
                        title = "Poster Uji",
                        fileName = "poster.png",
                        url = "https://cdn.example.com/poster.png",
                        sha256 = hashB
                    ),
                    ImagePackAsset(
                        target = "future_unknown_slot",
                        url = "https://cdn.example.com/future.jpg"
                    )
                )
            )
            val okRes = downloader.downloadPack(validPack, tempDir)
            assertTrue("Expected downloadPack to succeed, got: ${okRes.exceptionOrNull()}", okRes.isSuccess)
            val resVal = okRes.getOrThrow()
            assertEquals(2, resVal.downloadedCount)
            assertEquals(1, resVal.skippedCount)
            for (applied in resVal.appliedAssets) {
                val rel = applied.relativePath.removePrefix("jws-assets/")
                assertTrue("Expected saved file at $rel", File(tempDir, rel).exists())
            }

            // 2. Reject insecure HTTP URL
            val httpPack = ImagePackItem(
                id = "http_pack",
                title = "Insecure Pack",
                assets = listOf(
                    ImagePackAsset(target = "sholat_5_waktu", url = "http://cdn.example.com/sholat.jpg")
                )
            )
            val httpRes = downloader.downloadPack(httpPack, tempDir)
            assertTrue("Expected non-HTTPS URL to fail", httpRes.isFailure)

            // 3. Reject mismatched SHA-256 checksum
            val badHashPack = ImagePackItem(
                id = "bad_hash_pack",
                title = "Corrupt Pack",
                assets = listOf(
                    ImagePackAsset(
                        target = "sholat_5_waktu",
                        url = "https://cdn.example.com/sholat.jpg",
                        sha256 = "0".repeat(64)
                    )
                )
            )
            val badHashRes = downloader.downloadPack(badHashPack, tempDir)
            assertTrue("Expected SHA-256 mismatch to fail", badHashRes.isFailure)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
