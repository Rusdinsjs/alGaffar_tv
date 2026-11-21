package id.algaffar.jwstv.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HijriCalculatorTest {

    private val calculator = HijriCalculator()
    private val makassarZone = ZoneId.of("Asia/Makassar")

    @Test
    fun `Hijri date automatically increments by +1 day once Maghrib time is reached`() {
        val maghribTime = ZonedDateTime.of(2026, 5, 10, 18, 15, 0, 0, makassarZone)
        val beforeMaghrib = maghribTime.minusMinutes(1)
        val atMaghrib = maghribTime

        val hijriBefore = calculator.calculateHijriDate(beforeMaghrib, maghribTime, hijriAdjustment = 0)
        val hijriAtMaghrib = calculator.calculateHijriDate(atMaghrib, maghribTime, hijriAdjustment = 0)

        assertFalse(hijriBefore.isAfterMaghribRollover)
        assertTrue(hijriAtMaghrib.isAfterMaghribRollover)

        // Compare with explicit next calendar day
        val nextDayHijri = calculator.convertLocalDateToHijri(beforeMaghrib.toLocalDate().plusDays(1), 0)
        assertEquals(nextDayHijri.day, hijriAtMaghrib.day)
        assertEquals(nextDayHijri.month, hijriAtMaghrib.month)
        assertEquals(nextDayHijri.year, hijriAtMaghrib.year)
    }

    @Test
    fun `getFastingInfo detects Monday and Thursday sunnah fasting`() {
        // 2026-05-11 is a Monday
        val mondayMorning = ZonedDateTime.of(2026, 5, 11, 9, 0, 0, 0, makassarZone)
        val maghribTime = ZonedDateTime.of(2026, 5, 11, 18, 15, 0, 0, makassarZone)

        val fastingInfo = calculator.getFastingInfo(mondayMorning, maghribTime, hijriAdjustment = 0)
        assertTrue(
            "Expected Monday to be detected as fasting day (unless forbidden holiday), got: ${fastingInfo.today.title}",
            fastingInfo.today.isFasting || fastingInfo.today.isForbidden
        )
    }

    @Test
    fun `getUpcomingIslamicEvents returns upcoming PHBI within 60 days`() {
        val now = ZonedDateTime.of(2026, 2, 15, 9, 0, 0, 0, makassarZone)
        val maghrib = now.withHour(18).withMinute(20)
        val events = calculator.getUpcomingIslamicEvents(now, maghrib, hijriAdjustment = 0, maxDays = 60)

        assertTrue("Expected at least 1 PHBI event in Feb-Apr 2026 (Ramadhan/Eid)", events.isNotEmpty())
        assertTrue(events.all { it.daysUntil in 0..60 })
    }

    @Test
    fun `getUpcomingIslamicEvents supports configurable gregorian, custom, and disabled events`() {
        val now = ZonedDateTime.of(2026, 5, 10, 9, 0, 0, 0, makassarZone)
        val maghrib = now.withHour(18).withMinute(15)
        val customConfig = listOf(
            id.algaffar.jwstv.domain.model.CustomIslamicEventItem(
                id = "g-1",
                name = "Rapat DKM Masjid",
                type = "gregorian",
                gregorianDate = "2026-05-15",
                enabled = true
            ),
            id.algaffar.jwstv.domain.model.CustomIslamicEventItem(
                id = "c-1",
                name = "Tabligh Akbar",
                type = "custom",
                customSubtitle = "Ahad Ba'da Subuh",
                enabled = true
            ),
            id.algaffar.jwstv.domain.model.CustomIslamicEventItem(
                id = "d-1",
                name = "Agenda Nonaktif",
                type = "custom",
                customSubtitle = "Tidak Muncul",
                enabled = false
            )
        )

        val events = calculator.getUpcomingIslamicEvents(
            now = now,
            maghribTime = maghrib,
            hijriAdjustment = 0,
            maxDays = 30,
            customEvents = customConfig
        )

        val gregorianEvent = events.firstOrNull { it.name == "Rapat DKM Masjid" }
        assertTrue("Expected Gregorian event to be matched", gregorianEvent != null)
        assertEquals(5, gregorianEvent?.daysUntil)

        val customEvent = events.firstOrNull { it.name == "Tabligh Akbar" }
        assertTrue("Expected Custom event to be included", customEvent != null)
        assertEquals(-1, customEvent?.daysUntil)
        assertEquals("Ahad Ba'da Subuh", customEvent?.customSubtitle)

        assertTrue("Disabled event should be excluded", events.none { it.name == "Agenda Nonaktif" })
    }
}
