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
    fun `after Isha has passed, nextPrayer rolls over to tomorrow Fajr`() {
        val settings = PrayerSettings()
        val lateNight = ZonedDateTime.of(2026, 3, 10, 22, 30, 0, 0, makassarZone)
        val schedule = calculator.calculateSchedule(lateNight, settings)

        assertEquals(PrayerId.FAJR, schedule.nextPrayer.id)
        assertEquals(lateNight.toLocalDate().plusDays(1), schedule.nextPrayer.time.toLocalDate())
        assertTrue(schedule.secondsToNextPrayer > 3600L)
        assertTrue(schedule.items.first { it.id == PrayerId.FAJR }.isNext)
    }

    @Test
    fun `nextPrayer skips non-obligatory times (Syuruq and Dhuha) and targets Dhuhr after Fajr`() {
        val settings = PrayerSettings()
        // 05:30 is after Subuh (~05:00) and before Syuruq (~06:15) & Dhuha (~06:35)
        val afterSubuh = ZonedDateTime.of(2026, 3, 10, 5, 30, 0, 0, makassarZone)
        val schedule = calculator.calculateSchedule(afterSubuh, settings)

        assertEquals(PrayerId.DHUHR, schedule.nextPrayer.id)
        assertTrue(schedule.items.first { it.id == PrayerId.DHUHR }.isNext)
    }

    @Test
    fun `Dhuhr label stays Dzuhur on Friday when isJumatEnabled is false`() {
        val settings = PrayerSettings()
        val fridayNow = ZonedDateTime.of(2026, 3, 13, 9, 0, 0, 0, makassarZone)
        val schedule = calculator.calculateSchedule(fridayNow, settings, isJumatEnabled = false)

        assertEquals("Dzuhur", schedule.items.first { it.id == PrayerId.DHUHR }.label)
        assertEquals("Dzuhur", schedule.nextPrayer.label)
    }

    @Test
    fun `formatNextPrayerCountdown matches jws-android-algaffar H-MM-SS and MM-SS rules`() {
        assertEquals("00:00", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(0L))
        assertEquals("00:00", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(-5L))
        assertEquals("05:09", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(309L))
        assertEquals("59:59", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(3599L))
        assertEquals("1:00:00", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(3600L))
        assertEquals("10:05:09", id.algaffar.jwstv.ui.screens.standby.formatNextPrayerCountdown(36309L))
    }

    @Test
    fun `calculateSchedule reuses cached items across 1Hz ticks and invalidates on settings or date change`() {
        val settings = PrayerSettings(ihtiyati = 2)
        val t1 = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, makassarZone)
        val t2 = t1.plusSeconds(1)

        val s1 = calculator.calculateSchedule(t1, settings)
        val s2 = calculator.calculateSchedule(t2, settings)

        // Same day, same nextPrayer -> identical cached items reference, decremented countdown
        assertTrue(s1.items === s2.items)
        assertEquals(s1.secondsToNextPrayer - 1L, s2.secondsToNextPrayer)

        // Settings change immediately invalidates cache
        val s3 = calculator.calculateSchedule(t2, settings.copy(ihtiyati = 3))
        assertTrue(s1.items !== s3.items)
        assertEquals(
            1L,
            ChronoUnit.MINUTES.between(s1.timeFor(PrayerId.DHUHR), s3.timeFor(PrayerId.DHUHR))
        )

        // Next day immediately invalidates cache
        val sNextDay = calculator.calculateSchedule(t1.plusDays(1), settings)
        assertTrue(s1.items !== sNextDay.items)
        assertEquals(t1.toLocalDate().plusDays(1), sNextDay.date)
    }
}
