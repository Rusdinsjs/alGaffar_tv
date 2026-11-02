package id.algaffar.jwstv.domain.engine

import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.IdulFitriSettings
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.RamadhanSettings
import id.algaffar.jwstv.domain.model.ScreenSaverSchedule
import id.algaffar.jwstv.domain.model.ScreenSaverSettings
import id.algaffar.jwstv.domain.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class JwsStateMachineTest {

    private val prayerCalculator = PrayerTimeCalculator()
    private val stateMachine = JwsStateMachine(prayerCalculator)
    private val makassarZone = ZoneId.of("Asia/Makassar")

    @Test
    fun `5-time prayer sequence transitions cleanly from Tartil to Tarhim to Adzan to Iqomah to Sholat to Standby`() {
        val settings = AppStateSettings()
        // Pick a Tuesday (non-Friday): 2026-03-10
        val sampleDay = ZonedDateTime.of(2026, 3, 10, 9, 0, 0, 0, makassarZone)
        val schedule = prayerCalculator.calculateSchedule(sampleDay, settings.prayerSettings)
        val dhuhrTime = schedule.timeFor(PrayerId.DHUHR)

        // Default Dhuhr config: tartil=10m, tarhim=5m, adzan=4m, iqomah=10m, sholat=15m
        // 1) 8 minutes before Dhuhr -> TARTIL
        val tickTartil = stateMachine.evaluateTick(dhuhrTime.minusMinutes(8), settings, schedule)
        assertEquals(AppState.TARTIL, tickTartil.state)
        assertEquals(PrayerId.DHUHR, tickTartil.activePrayerId)
        assertFalse(tickTartil.shouldTriggerBeep)

        // 2) 3 minutes before Dhuhr -> TARHIM
        val tickTarhim = stateMachine.evaluateTick(dhuhrTime.minusMinutes(3), settings, schedule)
        assertEquals(AppState.TARHIM, tickTarhim.state)
        assertFalse(tickTarhim.shouldTriggerBeep)

        // 3) 2 seconds before Dhuhr -> TARHIM with 3x Beep & Volume FadeOut active
        val tickTarhimBeep = stateMachine.evaluateTick(dhuhrTime.minusSeconds(2), settings, schedule)
        assertEquals(AppState.TARHIM, tickTarhimBeep.state)
        assertTrue(tickTarhimBeep.shouldTriggerBeep)
        assertTrue(tickTarhimBeep.shouldFadeOutAudio)

        // 4) Exact Dhuhr time -> ADZAN
        val tickAdzan = stateMachine.evaluateTick(dhuhrTime, settings, schedule)
        assertEquals(AppState.ADZAN, tickAdzan.state)
        assertEquals(4 * 60L, tickAdzan.countdownSeconds)

        // 5) Crash/Reboot Recovery: TV boots 6 minutes after Dhuhr (2 mins into 10-min Iqomah)
        val tickIqomahRecovery = stateMachine.evaluateTick(dhuhrTime.plusMinutes(6), settings, schedule)
        assertEquals(AppState.MENUNGGU_IQOMAH, tickIqomahRecovery.state)
        assertEquals(8 * 60L, tickIqomahRecovery.countdownSeconds)
        assertFalse(tickIqomahRecovery.isNoSunnahWarningActive)

        // 6) 90 seconds left in Iqomah -> Red Warning Banner ("HARAP TIDAK MEMULAI SHOLAT SUNNAT") active
        val tickIqomahWarning = stateMachine.evaluateTick(
            dhuhrTime.plusMinutes(4).plusSeconds(10 * 60L - 90L),
            settings,
            schedule
        )
        assertEquals(AppState.MENUNGGU_IQOMAH, tickIqomahWarning.state)
        assertTrue(tickIqomahWarning.isNoSunnahWarningActive)

        // 7) 4 seconds left in Iqomah -> 5x Countdown Beep active
        val tickIqomahBeep = stateMachine.evaluateTick(
            dhuhrTime.plusMinutes(4).plusSeconds(10 * 60L - 4L),
            settings,
            schedule
        )
        assertEquals(AppState.MENUNGGU_IQOMAH, tickIqomahBeep.state)
        assertTrue(tickIqomahBeep.shouldTriggerBeep)

        // 8) 15 minutes after Dhuhr (inside 15-min Pelaksanaan Sholat) -> PELAKSANAAN_SHOLAT
        val tickSholat = stateMachine.evaluateTick(dhuhrTime.plusMinutes(15), settings, schedule)
        assertEquals(AppState.PELAKSANAAN_SHOLAT, tickSholat.state)
        assertEquals(14 * 60L, tickSholat.countdownSeconds)

        // 9) 30 minutes after Dhuhr (after 4m Adzan + 10m Iqomah + 15m Sholat = 29m) -> STANDBY
        val tickStandbyAfter = stateMachine.evaluateTick(dhuhrTime.plusMinutes(30), settings, schedule)
        assertEquals(AppState.STANDBY, tickStandbyAfter.state)
    }

    @Test
    fun `Tarhim and Adzan durations automatically sync with actual MP3 file length when provided`() {
        val settings = AppStateSettings()
        val sampleDay = ZonedDateTime.of(2026, 3, 10, 9, 0, 0, 0, makassarZone)
        val schedule = prayerCalculator.calculateSchedule(sampleDay, settings.prayerSettings)
        val asrTime = schedule.timeFor(PrayerId.ASR)

        // Actual MP3 durations: Tarhim = 318s (5m 18s), Adzan = 195s (3m 15s)
        val audioDurations = AudioFileDurations(
            tarhimSecondsByPrayer = mapOf(PrayerId.ASR to 318L),
            adzanSecondsByPrayer = mapOf(PrayerId.ASR to 195L)
        )

        // At 310 seconds before Asr -> Already in TARHIM because audio is 318s long
        val tickTarhim = stateMachine.evaluateTick(
            asrTime.minusSeconds(310),
            settings,
            schedule,
            audioDurations
        )
        assertEquals(AppState.TARHIM, tickTarhim.state)
        assertEquals(310L, tickTarhim.countdownSeconds)

        // At 200 seconds after Asr -> Already in MENUNGGU_IQOMAH because Adzan audio finished at 195s
        val tickIqomah = stateMachine.evaluateTick(
            asrTime.plusSeconds(200),
            settings,
            schedule,
            audioDurations
        )
        assertEquals(AppState.MENUNGGU_IQOMAH, tickIqomah.state)
    }

    @Test
    fun `Friday Dhuhr transitions through WAKTU_JUMAT, KHUTBAH, and PELAKSANAAN_JUMAT`() {
        val settings = AppStateSettings()
        // 2026-03-13 is a Friday
        val friday = ZonedDateTime.of(2026, 3, 13, 9, 0, 0, 0, makassarZone)
        val schedule = prayerCalculator.calculateSchedule(friday, settings.prayerSettings)
        val jumatTime = schedule.timeFor(PrayerId.DHUHR)

        // Default: adzan=4m, waktuJumat=12m (ends at +16m), khutbah=30m (ends at +46m), sholatJumat=20m (ends at +66m)
        val tickWaktuJumat = stateMachine.evaluateTick(jumatTime.plusMinutes(5), settings, schedule)
        assertEquals(AppState.WAKTU_JUMAT, tickWaktuJumat.state)

        val tickKhutbah = stateMachine.evaluateTick(jumatTime.plusMinutes(20), settings, schedule)
        assertEquals(AppState.KHUTBAH, tickKhutbah.state)

        val tickPelaksanaanJumat = stateMachine.evaluateTick(jumatTime.plusMinutes(50), settings, schedule)
        assertEquals(AppState.PELAKSANAAN_JUMAT, tickPelaksanaanJumat.state)

        val tickDone = stateMachine.evaluateTick(jumatTime.plusMinutes(67), settings, schedule)
        assertEquals(AppState.STANDBY, tickDone.state)
    }

    @Test
    fun `Ramadhan mode appends TARAWIH and SHOLAT_TARAWIH right after Isha prayer finishes`() {
        val settings = AppStateSettings(
            ramadhan = RamadhanSettings(
                isRamadhan = true,
                tarawihDuration = 15,
                sholatTarawihDuration = 45
            )
        )
        val day = ZonedDateTime.of(2026, 3, 10, 19, 0, 0, 0, makassarZone)
        val schedule = prayerCalculator.calculateSchedule(day, settings.prayerSettings)
        val ishaTime = schedule.timeFor(PrayerId.ISHA)

        // Isha: adzan=4m + iqomah=10m + sholat=15m = 29m.
        // At +30m -> TARAWIH (kultum/info for 15m, ends at +44m)
        val tickTarawih = stateMachine.evaluateTick(ishaTime.plusMinutes(30), settings, schedule)
        assertEquals(AppState.TARAWIH, tickTarawih.state)

        // At +50m -> SHOLAT_TARAWIH (45m, ends at +89m)
        val tickSholatTarawih = stateMachine.evaluateTick(ishaTime.plusMinutes(50), settings, schedule)
        assertEquals(AppState.SHOLAT_TARAWIH, tickSholatTarawih.state)
    }

    @Test
    fun `Idul Fitri timeSlot takes top priority and transitions to Sholat and Khutbah Idul Fitri after slot ends`() {
        val settings = AppStateSettings(
            idulFitri = IdulFitriSettings(
                enabled = true,
                timeSlots = listOf(TimeSlot(id = "1", startTime = "06:00", endTime = "07:00", enabled = true)),
                fullscreenDuration = 30,
                standbyDuration = 30,
                sholatDuration = 45,
                khutbahDuration = 30
            )
        )
        val at0600 = ZonedDateTime.of(2026, 3, 20, 6, 0, 10, 0, makassarZone)
        val tickFitri = stateMachine.evaluateTick(at0600, settings)
        assertEquals(AppState.IDUL_FITRI, tickFitri.state)

        val at0600_40 = ZonedDateTime.of(2026, 3, 20, 6, 0, 40, 0, makassarZone)
        val tickFitriStandby = stateMachine.evaluateTick(at0600_40, settings)
        assertEquals(AppState.IDUL_FITRI_STANDBY, tickFitriStandby.state)

        // Right after 07:00 -> SHOLAT_IDUL_FITRI (for 45m)
        val at0710 = ZonedDateTime.of(2026, 3, 20, 7, 10, 0, 0, makassarZone)
        val tickSholatEid = stateMachine.evaluateTick(at0710, settings)
        assertEquals(AppState.SHOLAT_IDUL_FITRI, tickSholatEid.state)

        // At 07:50 -> KHUTBAH_IDUL_FITRI (for 30m)
        val at0750 = ZonedDateTime.of(2026, 3, 20, 7, 50, 0, 0, makassarZone)
        val tickKhutbahEid = stateMachine.evaluateTick(at0750, settings)
        assertEquals(AppState.KHUTBAH_IDUL_FITRI, tickKhutbahEid.state)
    }

    @Test
    fun `Overnight ScreenSaver schedule activates at 23_00 and 02_00 and exits at 03_01`() {
        val settings = AppStateSettings(
            screensaver = ScreenSaverSettings(
                enabled = true,
                schedules = listOf(ScreenSaverSchedule(startTime = "22:00", endTime = "03:00", enabled = true))
            )
        )
        val at2300 = ZonedDateTime.of(2026, 3, 10, 23, 0, 0, 0, makassarZone)
        assertEquals(AppState.SCREENSAVER, stateMachine.evaluateTick(at2300, settings).state)

        val at0200 = ZonedDateTime.of(2026, 3, 11, 2, 0, 0, 0, makassarZone)
        assertEquals(AppState.SCREENSAVER, stateMachine.evaluateTick(at0200, settings).state)

        val at0305 = ZonedDateTime.of(2026, 3, 11, 3, 5, 0, 0, makassarZone)
        assertEquals(AppState.STANDBY, stateMachine.evaluateTick(at0305, settings).state)
    }

    @Test
    fun `findEffectivePetugasJumatDate picks upcoming Friday within H-6 window and legacy path migration cleans URLs`() {
        val records = mapOf(
            "2026-03-06" to JumatPetugasRecord(khotib = "Ust. A"),
            "2026-03-13" to JumatPetugasRecord(khotib = "Ust. B"),
            "2026-03-20" to JumatPetugasRecord(khotib = "Ust. C")
        )
        // On Monday 2026-03-09 (4 days before Friday 2026-03-13), effective date is 2026-03-13
        val effectiveKey = JwsStateMachine.findEffectivePetugasJumatDate(records, LocalDate.of(2026, 3, 9))
        assertEquals("2026-03-13", effectiveKey)

        // Legacy Tauri asset URL migration test
        val legacyJson = """{"logo":"https://asset.localhost/C%3A/Users/dkm/AppData/jws-assets/images/logo%20masjid.png"}"""
        val migrated = SettingsRepository.migrateLegacyJsonPaths(legacyJson)
        assertEquals("""{"logo":"jws-assets/images/logo masjid.png"}""", migrated)
    }
}
