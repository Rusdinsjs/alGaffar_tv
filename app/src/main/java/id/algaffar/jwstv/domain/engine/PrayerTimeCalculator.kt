package id.algaffar.jwstv.domain.engine

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerSettings
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

data class PrayerTimeItem(
    val id: PrayerId,
    val label: String,
    val time: ZonedDateTime,
    val formattedTime: String,
    val isNext: Boolean = false
)

data class DailyPrayerSchedule(
    val date: LocalDate,
    val zoneId: ZoneId,
    val isFriday: Boolean,
    val items: List<PrayerTimeItem>,
    val nextPrayer: PrayerTimeItem,
    val secondsToNextPrayer: Long
) {
    fun timeFor(prayerId: PrayerId): ZonedDateTime =
        items.first { it.id == prayerId }.time

    fun formattedFor(prayerId: PrayerId): String =
        items.first { it.id == prayerId }.formattedTime

    val fiveFardhuItems: List<PrayerTimeItem>
        get() = items.filter { it.id.isFiveTimeFardhu }
}

class PrayerTimeCalculator {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun resolveZoneId(timezone: String): ZoneId {
        return try {
            ZoneId.of(timezone)
        } catch (_: Exception) {
            ZoneId.of("Asia/Makassar")
        }
    }

    /**
     * Calculates the 8 daily prayer times for a specific [now] and [settings].
     * Mirrors the exact calculation rules in `PrayerTimeContext.tsx`.
     */
    fun calculateSchedule(
        now: ZonedDateTime,
        settings: PrayerSettings
    ): DailyPrayerSchedule {
        val zoneId = resolveZoneId(settings.timezone)
        val zonedNow = now.withZoneSameInstant(zoneId)
        val today = zonedNow.toLocalDate()
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY

        val todayTimesMap = computeEightTimesForDate(today, zoneId, settings)

        // Determine next prayer among today's 8 times
        var nextPrayerId: PrayerId? = null
        var nextPrayerTime: ZonedDateTime? = null

        for (prayerId in PrayerId.entries) {
            val candidateTime = todayTimesMap.getValue(prayerId)
            if (candidateTime.isAfter(zonedNow)) {
                nextPrayerId = prayerId
                nextPrayerTime = candidateTime
                break
            }
        }

        // If all 8 times today have passed (after Isha), next prayer is tomorrow's Imsak
        if (nextPrayerId == null || nextPrayerTime == null) {
            val tomorrow = today.plusDays(1)
            val tomorrowTimesMap = computeEightTimesForDate(tomorrow, zoneId, settings)
            nextPrayerId = PrayerId.IMSAK
            nextPrayerTime = tomorrowTimesMap.getValue(PrayerId.IMSAK)
        }

        val secondsToNext = ChronoUnit.SECONDS.between(zonedNow, nextPrayerTime).coerceAtLeast(0L)

        val items = PrayerId.entries.map { id ->
            val t = todayTimesMap.getValue(id)
            PrayerTimeItem(
                id = id,
                label = id.displayName(isFriday = isFriday),
                time = t,
                formattedTime = t.format(timeFormatter),
                isNext = (id == nextPrayerId && t.toLocalDate() == nextPrayerTime.toLocalDate())
            )
        }

        val nextItem = PrayerTimeItem(
            id = nextPrayerId,
            label = nextPrayerId.displayName(isFriday = nextPrayerTime.dayOfWeek == DayOfWeek.FRIDAY),
            time = nextPrayerTime,
            formattedTime = nextPrayerTime.format(timeFormatter),
            isNext = true
        )

        return DailyPrayerSchedule(
            date = today,
            zoneId = zoneId,
            isFriday = isFriday,
            items = items,
            nextPrayer = nextItem,
            secondsToNextPrayer = secondsToNext
        )
    }

    /**
     * Computes the exact 8 times (`imsak`, `fajr`, `sunrise`, `dhuha`, `dhuhr`, `asr`, `maghrib`, `isha`)
     * for any [localDate], applying per-prayer corrections and `ihtiyati`.
     */
    fun computeEightTimesForDate(
        localDate: LocalDate,
        zoneId: ZoneId,
        settings: PrayerSettings
    ): Map<PrayerId, ZonedDateTime> {
        val coordinates = Coordinates(settings.latitude, settings.longitude)
        val dateComponents = DateComponents(localDate.year, localDate.monthValue, localDate.dayOfMonth)
        val params = buildCalculationParameters(settings.calculationMethod, settings.madhab)

        val rawPrayerTimes = PrayerTimes(coordinates, dateComponents, params)

        val rawFajr = rawPrayerTimes.fajr.toZonedDateTime(zoneId)
        val rawSunrise = rawPrayerTimes.sunrise.toZonedDateTime(zoneId)
        val rawDhuhr = rawPrayerTimes.dhuhr.toZonedDateTime(zoneId)
        val rawAsr = rawPrayerTimes.asr.toZonedDateTime(zoneId)
        val rawMaghrib = rawPrayerTimes.maghrib.toZonedDateTime(zoneId)
        val rawIsha = rawPrayerTimes.isha.toZonedDateTime(zoneId)

        val corr = settings.corrections
        val ihtiyati = settings.ihtiyati.toLong()

        // Imsak = Fajr - 10 mins + imsak correction + ihtiyati
        val imsak = rawFajr
            .minusMinutes(10)
            .plusMinutes(corr.imsak.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        val fajr = rawFajr
            .plusMinutes(corr.fajr.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        // Sunrise & Dhuha only apply per-prayer correction (no ihtiyati, matching PrayerTimeContext.tsx)
        val sunrise = rawSunrise
            .plusMinutes(corr.sunrise.toLong())
            .truncatedTo(ChronoUnit.MINUTES)

        val dhuha = rawSunrise
            .plusMinutes(20 + corr.dhuha.toLong())
            .truncatedTo(ChronoUnit.MINUTES)

        val dhuhr = rawDhuhr
            .plusMinutes(corr.dhuhr.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        val asr = rawAsr
            .plusMinutes(corr.asr.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        val maghrib = rawMaghrib
            .plusMinutes(corr.maghrib.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        val isha = rawIsha
            .plusMinutes(corr.isha.toLong() + ihtiyati)
            .truncatedTo(ChronoUnit.MINUTES)

        return mapOf(
            PrayerId.IMSAK to imsak,
            PrayerId.FAJR to fajr,
            PrayerId.SUNRISE to sunrise,
            PrayerId.DHUHA to dhuha,
            PrayerId.DHUHR to dhuhr,
            PrayerId.ASR to asr,
            PrayerId.MAGHRIB to maghrib,
            PrayerId.ISHA to isha
        )
    }

    private fun buildCalculationParameters(
        methodName: String,
        madhabName: String
    ): CalculationParameters {
        val params = when (methodName) {
            "Singapore" -> CalculationMethod.SINGAPORE.parameters
            "MuslimWorldLeague" -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            "Egyptian" -> CalculationMethod.EGYPTIAN.parameters
            "Karachi" -> CalculationMethod.KARACHI.parameters
            "UmmAlQura" -> CalculationMethod.UMM_AL_QURA.parameters
            "Dubai" -> CalculationMethod.DUBAI.parameters
            "MoonsightingCommittee" -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
            "NorthAmerica" -> CalculationMethod.NORTH_AMERICA.parameters
            "Kuwait" -> CalculationMethod.KUWAIT.parameters
            "Qatar" -> CalculationMethod.QATAR.parameters
            "Tehran" -> CalculationParameters(17.7, 14.0)
            "Turkey" -> CalculationParameters(18.0, 17.0)
            else -> CalculationMethod.SINGAPORE.parameters
        }

        params.madhab = if (madhabName.equals("Hanafi", ignoreCase = true)) {
            Madhab.HANAFI
        } else {
            Madhab.SHAFI
        }

        return params
    }

    private fun Date.toZonedDateTime(zoneId: ZoneId): ZonedDateTime {
        return Instant.ofEpochMilli(this.time).atZone(zoneId)
    }
}
