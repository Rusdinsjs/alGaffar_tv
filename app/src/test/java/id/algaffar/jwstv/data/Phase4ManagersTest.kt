package id.algaffar.jwstv.data

import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.backup.BackupRestoreManager
import id.algaffar.jwstv.data.excel.TakjilExcelManager
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.domain.model.UserAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Phase4ManagersTest {

    @Test
    fun `AuthManager authenticates default superadmin and SHA-256 hashed PINs`() {
        // Default fallback when user list is empty
        val defaultAuth = AuthManager.authenticate("admin", "732911", emptyList())
        assertNotNull(defaultAuth)
        assertEquals("superadmin", defaultAuth?.role)

        // Wrong PIN returns null
        assertNull(AuthManager.authenticate("admin", "000000", emptyList()))

        // Custom accounts with SHA-256 hash
        val accounts = listOf(
            UserAccount(
                id = "op-1",
                username = "marbot",
                pinHash = AuthManager.sha256Hex("123456"),
                role = "user"
            )
        )
        val matched = AuthManager.authenticate("marbot", "123456", accounts)
        assertNotNull(matched)
        assertEquals("op-1", matched?.id)

        // Blank username fast PIN-only keypad login
        val fastLogin = AuthManager.authenticate("", "123456", accounts)
        assertNotNull(fastLogin)
        assertEquals("marbot", fastLogin?.username)

        // Role tab access check
        assertTrue(AuthManager.canAccessTab(UserRole.SUPERADMIN, "users_security"))
        assertFalse(AuthManager.canAccessTab(UserRole.ADMIN, "users_security"))
        assertTrue(AuthManager.canAccessTab(UserRole.ADMIN, "audio_hardware"))
        assertFalse(AuthManager.canAccessTab(UserRole.USER, "audio_hardware"))
        assertTrue(AuthManager.canAccessTab(UserRole.USER, "ramadhan"))
    }

    @Test
    fun `TakjilExcelManager exports 30-day xlsx and parses it back accurately`() {
        val sampleSchedule = mapOf(
            "1" to listOf("H. Sulaiman", "Hj. Aminah"),
            "15" to listOf("Keluarga Besar Bpk. Hasan", "Ibu Fatimah"),
            "27" to listOf("Remaja Masjid Al-Gaffar")
        )

        val baos = ByteArrayOutputStream()
        TakjilExcelManager.exportTemplateExcel(baos, sampleSchedule)
        val excelBytes = baos.toByteArray()
        assertTrue("Generated XLSX should not be empty", excelBytes.isNotEmpty())

        // Parse full 30-day table
        val parsedRows = TakjilExcelManager.parseTakjilExcel(
            inputStream = ByteArrayInputStream(excelBytes),
            targetDay = null
        )
        assertEquals(3, parsedRows.size)
        val day1 = parsedRows.first { it.day == 1 }
        assertEquals(listOf("H. Sulaiman", "Hj. Aminah"), day1.names)

        val day15 = parsedRows.first { it.day == 15 }
        assertEquals(listOf("Keluarga Besar Bpk. Hasan", "Ibu Fatimah"), day15.names)

        // Parse single target day mode
        val singleDayParsed = TakjilExcelManager.parseTakjilExcel(
            inputStream = ByteArrayInputStream(excelBytes),
            targetDay = 10
        )
        assertEquals(1, singleDayParsed.size)
        assertEquals(10, singleDayParsed.first().day)
        assertTrue(singleDayParsed.first().names.contains("H. Sulaiman"))
    }

    @Test
    fun `BackupRestoreManager restores legacy Tauri zip with asset localhost URLs and jws-assets`() = runBlocking {
        val tempDir = Files.createTempDirectory("jws_backup_test").toFile()
        try {
            val settingsFile = tempDir.resolve("datastore/jws_settings.json")
            val assetsDir = tempDir.resolve("jws-assets")
            val repo = SettingsRepository(storageFile = settingsFile)

            // Build a simulated legacy Tauri backup .zip in memory
            val legacySettingsJson = """
                {
                  "brand": {
                    "name": "Masjid Raya Al-Gaffar Tauri",
                    "address": "Jl. Kemakmuran No. 99",
                    "logo": "https://asset.localhost/C%3A%5CUsers%5CDKM%5CAppData%5CRoaming%5Cjws-assets%5Cimages%5Clogo_masjid.png"
                  }
                }
            """.trimIndent()

            val zipBaos = ByteArrayOutputStream()
            ZipOutputStream(zipBaos).use { zos ->
                zos.putNextEntry(ZipEntry("settings.json"))
                zos.write(legacySettingsJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("jws-assets/images/logo_masjid.png"))
                zos.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("jws-assets/audio/adzan/makkah.mp3"))
                zos.write(byteArrayOf(0x49, 0x44, 0x33))
                zos.closeEntry()
            }

            val restoreResult = BackupRestoreManager.restoreFromBackupZip(
                inputStream = ByteArrayInputStream(zipBaos.toByteArray()),
                settingsRepository = repo,
                assetsRootDir = assetsDir
            )
            assertTrue(restoreResult.isSuccess)
            val summary = restoreResult.getOrThrow()
            assertTrue(summary.settingsRestored)
            assertEquals(2, summary.extractedAssetFilesCount)
            assertEquals("Masjid Raya Al-Gaffar Tauri", summary.restoredMosqueName)

            // Verify legacy URL was migrated to clean relative jws-assets path and files exist on disk
            val expectedLogoFile = assetsDir.resolve("images/logo_masjid.png")
            assertTrue(expectedLogoFile.exists())
            assertTrue(assetsDir.resolve("audio/adzan/makkah.mp3").exists())
            assertEquals(
                "jws-assets/images/logo_masjid.png",
                repo.currentSettings.brand.logo
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
