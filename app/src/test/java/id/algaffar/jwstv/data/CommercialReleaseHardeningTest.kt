package id.algaffar.jwstv.data

import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.OfflinePinRecoveryEngine
import id.algaffar.jwstv.data.auth.PinLockoutTracker
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.location.IndonesiaCityRepository
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.data.server.RemoteTokenStore
import id.algaffar.jwstv.data.update.OtaUpdateManager
import id.algaffar.jwstv.domain.engine.ClockIntegrityGuard
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CommercialReleaseHardeningTest {

    @Test
    fun `AuthManager PBKDF2 produces unique salted hashes, verifies correctly, and flags legacy hashes for upgrade`() {
        val hash1 = AuthManager.hashPinPbkdf2("849201")
        val hash2 = AuthManager.hashPinPbkdf2("849201")

        assertTrue(hash1.startsWith("pbkdf2$"))
        assertNotEquals("Salted PBKDF2 hashes must differ for identical PINs", hash1, hash2)
        assertTrue(AuthManager.verifyPin("849201", hash1))
        assertTrue(AuthManager.verifyPin("849201", hash2))
        assertFalse(AuthManager.verifyPin("111111", hash1))
        assertFalse(AuthManager.needsHashUpgrade(hash1))

        val legacySha256 = AuthManager.sha256Hex("654321")
        assertTrue(AuthManager.verifyPin("654321", legacySha256))
        assertTrue(AuthManager.needsHashUpgrade(legacySha256))
    }

    @Test
    fun `PinLockoutTracker enforces 60s lockout after 5 consecutive failures and resets on success`() {
        val tracker = PinLockoutTracker(maxFailedAttempts = 5, lockoutDurationMs = 60_000L)
        val t0 = 1_700_000_000_000L

        assertEquals(4, tracker.recordFailure(t0))
        assertEquals(3, tracker.recordFailure(t0 + 1_000L))
        assertEquals(2, tracker.recordFailure(t0 + 2_000L))
        assertEquals(1, tracker.recordFailure(t0 + 3_000L))
        assertFalse(tracker.isLocked(t0 + 3_500L))

        assertEquals(0, tracker.recordFailure(t0 + 4_000L))
        assertTrue(tracker.isLocked(t0 + 5_000L))
        assertTrue(tracker.remainingLockoutSeconds(t0 + 5_000L) in 58L..60L)

        // Unlocked after 60 seconds
        assertFalse(tracker.isLocked(t0 + 65_000L))
        assertEquals(0L, tracker.remainingLockoutSeconds(t0 + 65_000L))

        // Record failure then success clears counter
        tracker.recordFailure(t0 + 70_000L)
        tracker.recordSuccess()
        assertEquals(0, tracker.currentFailedAttempts())
    }

    @Test
    fun `OfflinePinRecoveryEngine verifies HMAC challenge-response codes deterministically and rejects mismatches`() {
        val date = LocalDate.of(2026, 11, 23)
        val deviceId = "K7P4M9X2"
        val challenge = "R5T8N3"

        val validCode = OfflinePinRecoveryEngine.computeRecoveryCode(deviceId, challenge, date)
        assertEquals(6, validCode.length)
        assertTrue(validCode.all { it.isDigit() })

        assertTrue(OfflinePinRecoveryEngine.verifyRecoveryCode(deviceId, challenge, validCode, date))
        // +/- 1 day midnight tolerance
        assertTrue(OfflinePinRecoveryEngine.verifyRecoveryCode(deviceId, challenge, validCode, date.plusDays(1)))
        // Reject wrong code, wrong device, wrong date (>1 day), or wrong challenge
        assertFalse(OfflinePinRecoveryEngine.verifyRecoveryCode(deviceId, challenge, "000000", date))
        assertFalse(OfflinePinRecoveryEngine.verifyRecoveryCode("OTHER123", challenge, validCode, date))
        assertFalse(OfflinePinRecoveryEngine.verifyRecoveryCode(deviceId, "XXXXXX", validCode, date))
        assertFalse(OfflinePinRecoveryEngine.verifyRecoveryCode(deviceId, challenge, validCode, date.plusDays(3)))
    }

    @Test
    fun `RemoteTokenStore persists 32-char token across restarts and supports token regeneration`() {
        val tempDir = Files.createTempDirectory("jws_token_store_test").toFile()
        try {
            val store1 = RemoteTokenStore(tempDir)
            val token1 = store1.getOrCreateRemoteToken()
            val dev1 = store1.getOrCreateDeviceCode()

            assertEquals(32, token1.length)
            assertEquals(8, dev1.length)

            // Re-instantiate store (simulating TV reboot)
            val store2 = RemoteTokenStore(tempDir)
            assertEquals(token1, store2.getOrCreateRemoteToken())
            assertEquals(dev1, store2.getOrCreateDeviceCode())

            // Regenerate disconnects previous phones
            val token2 = store2.regenerateRemoteToken()
            assertEquals(32, token2.length)
            assertNotEquals(token1, token2)
            assertEquals(token2, RemoteTokenStore(tempDir).getOrCreateRemoteToken())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `KtorRemoteServer enforces strict LAN CORS, RBAC roles, and rejects backdoor 732911`() {
        // Strict LAN CORS checks
        assertTrue(KtorRemoteServer.isAllowedLanOrigin(null))
        assertTrue(KtorRemoteServer.isAllowedLanOrigin("http://192.168.1.50:1421"))
        assertTrue(KtorRemoteServer.isAllowedLanOrigin("http://10.0.0.12:1421"))
        assertTrue(KtorRemoteServer.isAllowedLanOrigin("http://localhost:1421"))
        assertFalse(KtorRemoteServer.isAllowedLanOrigin("https://evil.example.com"))
        assertFalse(KtorRemoteServer.isAllowedLanOrigin("http://8.8.8.8:1421"))

        // RBAC enforcement
        assertTrue(KtorRemoteServer.isActionAllowedForRole("update_admin_pin", UserRole.SUPERADMIN))
        assertFalse(KtorRemoteServer.isActionAllowedForRole("update_admin_pin", UserRole.ADMIN))
        assertFalse(KtorRemoteServer.isActionAllowedForRole("regenerate_remote_token", UserRole.ADMIN))
        assertFalse(KtorRemoteServer.isActionAllowedForRole("clear_logs", UserRole.ADMIN))
        assertTrue(KtorRemoteServer.isActionAllowedForRole("add_overlay_text", UserRole.ADMIN))
        assertFalse(KtorRemoteServer.isActionAllowedForRole("add_overlay_text", UserRole.USER))

        // Backdoor 732911 rejected once custom Superadmin PIN is configured
        val customSettings = AppStateSettings(
            users = listOf(
                UserAccount("sa-1", "admin", AuthManager.hashPinPbkdf2("987654"), "superadmin"),
                UserAccount("adm-1", "dkm", AuthManager.hashPinPbkdf2("456789"), "admin")
            )
        )
        assertEquals(null, KtorRemoteServer.resolveAuthorizedRole("Bearer 732911", "LONGTOKEN32CHARS1234567890123456", customSettings))
        assertEquals(UserRole.SUPERADMIN, KtorRemoteServer.resolveAuthorizedRole("Bearer 987654", "LONGTOKEN32CHARS1234567890123456", customSettings))
        assertEquals(UserRole.ADMIN, KtorRemoteServer.resolveAuthorizedRole("Bearer 456789", "LONGTOKEN32CHARS1234567890123456", customSettings))
        assertEquals(UserRole.SUPERADMIN, KtorRemoteServer.resolveAuthorizedRole("Bearer LONGTOKEN32CHARS1234567890123456", "LONGTOKEN32CHARS1234567890123456", customSettings))
    }

    @Test
    fun `SettingsRepository automatically recovers from dual bak file when primary JSON is corrupted`() = runBlocking {
        val tempDir = Files.createTempDirectory("jws_settings_bak_test").toFile()
        try {
            val primaryFile = tempDir.resolve("jws_settings.json")
            val repo1 = SettingsRepository(primaryFile)
            repo1.updateSettings { prev ->
                prev.copy(
                    onboardingCompleted = true,
                    brand = prev.brand.copy(name = "MASJID AL-IKHLAS NUSANTARA")
                )
            }

            // Simulate sudden power outage truncating primary JSON
            primaryFile.writeText("{ \"brand\": { \"name\": \"CORRUPT", Charsets.UTF_8)

            // Re-open repository: must recover from jws_settings.json.bak!
            val repo2 = SettingsRepository(primaryFile)
            assertEquals("MASJID AL-IKHLAS NUSANTARA", repo2.currentSettings.brand.name)
            assertTrue(repo2.currentSettings.onboardingCompleted)

            // Verify corrupted file was quarantined for diagnostics
            val corruptBackups = tempDir.listFiles()?.filter { it.name.contains(".corrupt.") }.orEmpty()
            assertTrue("Expected corrupted file quarantine copy", corruptBackups.isNotEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `ClockIntegrityGuard detects No-RTC 2015 clock, applies HP sync offset, and auto-heals on NTP recovery`() {
        val tempDir = Files.createTempDirectory("jws_clock_guard_test").toFile()
        try {
            val anchorFile = tempDir.resolve(".jws_clock_anchor")
            val guard = ClockIntegrityGuard(anchorFile)
            val zone = ZoneId.of("Asia/Makassar")

            val broken2015Ms = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
            val real2026Ms = ZonedDateTime.of(2026, 11, 23, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

            assertTrue(guard.isRawClockSuspect(broken2015Ms))
            assertTrue(guard.isEffectiveClockSuspect(broken2015Ms))

            // Caretaker syncs clock from smartphone
            val syncRes = guard.syncFromTrustedReference(real2026Ms, broken2015Ms)
            assertTrue(syncRes.isSuccess)
            assertFalse(guard.isEffectiveClockSuspect(broken2015Ms))
            assertEquals(2026, guard.effectiveNow(zone, broken2015Ms).year)

            // Later, WiFi reconnects and Android NTP restores real 2026 system clock -> offset auto-clears!
            val afterNtp = guard.effectiveNow(zone, real2026Ms + 60_000L)
            assertEquals(2026, afterNtp.year)
            assertEquals(0L, guard.currentOffsetMs)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `IndonesiaCityRepository parses all 38 Indonesian provinces with valid coordinates and timezones`() {
        val candidates = listOf(
            File("src/main/assets/data/indonesia_cities.json"),
            File("app/src/main/assets/data/indonesia_cities.json")
        )
        val jsonFile = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("indonesia_cities.json not found in test working directory")

        val cities = FileInputStream(jsonFile).use { IndonesiaCityRepository.parseFromStream(it) }
        val provinces = IndonesiaCityRepository.getProvinces(cities)

        assertTrue("Expected at least 38 Indonesian provinces + ASEAN, got ${provinces.size}", provinces.size >= 38)
        assertTrue("Expected > 200 city/regency entries, got ${cities.size}", cities.size > 200)

        val validTimezones = setOf("Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", "Asia/Singapore", "Asia/Kuala_Lumpur")
        for (city in cities) {
            assertTrue("Invalid lat for ${city.city}", city.latitude in -11.5..6.5)
            assertTrue("Invalid lng for ${city.city}", city.longitude in 94.5..142.0)
            assertTrue("Invalid tz '${city.timezone}' for ${city.city}", city.timezone in validTimezones)
        }
    }

    @Test
    fun `OtaUpdateManager verifies SHA-256 checksums accurately`() {
        val tempFile = Files.createTempFile("ota_test_", ".apk").toFile()
        try {
            tempFile.writeText("ALGAFFAR-TV-TEST-PAYLOAD", Charsets.UTF_8)
            val hash = OtaUpdateManager.computeFileSha256(tempFile)
            assertEquals(64, hash.length)
            assertTrue(OtaUpdateManager.verifySha256(hash, hash.uppercase()))
            assertFalse(OtaUpdateManager.verifySha256(hash, "0".repeat(64)))
        } finally {
            tempFile.delete()
        }
    }
}
