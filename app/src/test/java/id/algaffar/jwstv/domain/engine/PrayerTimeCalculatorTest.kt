package id.algaffar.jwstv.domain.engine

import id.algaffar.jwstv.domain.model.PrayerCorrections
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PrayerTimeCalculatorTest {

    private val calculator = PrayerTimeCalculator()
    private val makassarZone = ZoneId.of("Asia/Makassar")

    @Test
    fun `8 daily prayer times are strictly chronological and follow Imsak and Dhuha offsets`() {
        val settings = PrayerSettings(
            latitude = -1.2379,
            longitude = 116.8529,
            timezone = "Asia/Makassar",
            calculationMethod = "Singapore",
            madhab = "Shafi",
            ihtiyati = 2
        )
        val now = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, makassarZone)
        val schedule = calculator.calculateSchedule(now, settings)

        assertEquals(8, schedule.items.size)
        for (i in 0 until schedule.items.size - 1) {
            val current = schedule.items[i]
            val next = schedule.items[i + 1]
            assertTrue(
                "Expected ${current.id} (${current.time}) to be before ${next.id} (${next.time})",
                current.time.isBefore(next.time)
            )
        }

        // Verify Imsak is exactly 10 minutes before Fajr when corrections are 0
        val imsakDiff = ChronoUnit.MINUTES.between(
            schedule.timeFor(PrayerId.IMSAK),
            schedule.timeFor(PrayerId.FAJR)
        )
        assertEquals(10L, imsakDiff)

        // Verify Dhuha is exactly 20 minutes after Sunrise when corrections are 0
        val dhuhaDiff = ChronoUnit.MINUTES.between(
            schedule.timeFor(PrayerId.SUNRISE),
            schedule.timeFor(PrayerId.DHUHA)
        )
        assertEquals(20L, dhuhaDiff)
    }

    @Test
    fun `Dhuhr label changes to Jumat on Fridays and stays Dzuhur on other days`() {
        val settings = PrayerSettings()
        // 2026-03-13 is a Friday
        val fridayNow = ZonedDateTime.of(2026, 3, 13, 9, 0, 0, 0, makassarZone)
        val fridaySchedule = calculator.calculateSchedule(fridayNow, settings)
        assertTrue(fridaySchedule.isFriday)
        val fridayDhuhr = fridaySchedule.items.first { it.id == PrayerId.DHUHR }
        assertEquals("Jum'at", fridayDhuhr.label)

        // 2026-03-14 is a Saturday
        val saturdayNow = ZonedDateTime.of(2026, 3, 14, 9, 0, 0, 0, makassarZone)
        val saturdaySchedule = calculator.calculateSchedule(saturdayNow, settings)
        val saturdayDhuhr = saturdaySchedule.items.first { it.id == PrayerId.DHUHR }
        assertEquals("Dzuhur", saturdayDhuhr.label)
    }

    @Test
    fun `ihtiyati applies to Imsak and 5 Fardhu prayers but not Sunrise or Dhuha`() {
        val baseSettings = PrayerSettings(ihtiyati = 0, corrections = PrayerCorrections())
        val plusThreeSettings = PrayerSettings(ihtiyati = 3, corrections = PrayerCorrections())
        val now = ZonedDateTime.of(2026, 3, 10, 6, 0, 0, 0, makassarZone)

        val baseSchedule = calculator.calculateSchedule(now, baseSettings)
        val plusSchedule = calculator.calculateSchedule(now, plusThreeSettings)

        assertEquals(
            3L,
            ChronoUnit.MINUTES.between(
                baseSchedule.timeFor(PrayerId.IMSAK),
                plusSchedule.timeFor(PrayerId.IMSAK)
            )
        )
        assertEquals(
            3L,
            ChronoUnit.MINUTES.between(
                baseSchedule.timeFor(PrayerId.FAJR),
                plusSchedule.timeFor(PrayerId.FAJR)
            )
        )
        assertEquals(
            0L,
            ChronoUnit.MINUTES.between(
                baseSchedule.timeFor(PrayerId.SUNRISE),
                plusSchedule.timeFor(PrayerId.SUNRISE)
            )
        )
        assertEquals(
            0L,
            ChronoUnit.MINUTES.between(
                baseSchedule.timeFor(PrayerId.DHUHA),
                plusSchedule.timeFor(PrayerId.DHUHA)
            )
        )
        assertEquals(
            3L,
            ChronoUnit.MINUTES.between(
                baseSchedule.timeFor(PrayerId.MAGHRIB),
                plusSchedule.timeFor(PrayerId.MAGHRIB)
            )
        )
    }

    @Test
    fun `after Isha has passed, nextPrayer rolls over to tomorrow Imsak`() {
        val settings = PrayerSettings()
        val lateNight = ZonedDateTime.of(2026, 3, 10, 22, 30, 0, 0, makassarZone)
        val schedule = calculator.calculateSchedule(lateNight, settings)

        assertEquals(PrayerId.IMSAK, schedule.nextPrayer.id)
        assertEquals(lateNight.toLocalDate().plusDays(1), schedule.nextPrayer.time.toLocalDate())
        assertTrue(schedule.secondsToNextPrayer > 3600L)
    }
}
