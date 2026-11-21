package id.algaffar.jwstv.domain.engine

import id.algaffar.jwstv.domain.model.CustomIslamicEventItem
import id.algaffar.jwstv.domain.model.InfoDisplaySettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.math.floor

data class HijriDateInfo(
    val day: Int,
    val month: Int, // 1..12
    val monthName: String,
    val year: Int,
    val isAfterMaghribRollover: Boolean
) {
    val formatted: String
        get() = "$day $monthName $year H"
}

data class FastingDayStatus(
    val isFasting: Boolean,
    val isForbidden: Boolean,
    val title: String,
    val description: String,
    val isSunnah: Boolean = isFasting
)

data class FastingInfoSummary(
    val today: FastingDayStatus,
    val tomorrow: FastingDayStatus
)

data class IslamicEventItem(
    val name: String,
    val hijriMonth: Int,
    val hijriDay: Int,
    val gregorianDate: LocalDate,
    val daysUntil: Int,
    val customSubtitle: String = "",
    val type: String = "hijri"
)

/**
 * Hijri Calendar, Sunnah Fasting & PHBI (Hari Besar Islam) Calculator.
 * Mirrors `src/utils/hijri.ts` and `src/components/Screens/Header/Tanggal.tsx`.
 */
class HijriCalculator {

    companion object {
        val HIJRI_MONTH_NAMES = listOf(
            "Muharram",
            "Safar",
            "Rabiul Awal",
            "Rabiul Akhir",
            "Jumadil Awal",
            "Jumadil Akhir",
            "Rajab",
            "Sya'ban",
            "Ramadhan",
            "Syawal",
            "Dzulqa'dah",
            "Dzulhijjah"
        )

        private val ISO_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Converts [now] into Hijri date, applying:
     * 1. Automatic +1 day rollover once [now] >= [maghribTime] (Islamic day starts at Maghrib).
     * 2. User manual [hijriAdjustment] (-3..+3 days).
     */
    fun calculateHijriDate(
        now: ZonedDateTime,
        maghribTime: ZonedDateTime?,
        hijriAdjustment: Int = 0
    ): HijriDateInfo {
        val isAfterMaghrib = maghribTime != null && !now.isBefore(maghribTime)
        val effectiveLocalDate = now.toLocalDate()
            .plusDays(if (isAfterMaghrib) 1L else 0L)

        return convertLocalDateToHijri(
            localDate = effectiveLocalDate,
            hijriAdjustment = hijriAdjustment,
            isAfterMaghribRollover = isAfterMaghrib
        )
    }

    fun convertLocalDateToHijri(
        localDate: LocalDate,
        hijriAdjustment: Int = 0,
        isAfterMaghribRollover: Boolean = false
    ): HijriDateInfo {
        val adjustedDate = localDate.plusDays(hijriAdjustment.toLong())
        return try {
            val hijrahDate = HijrahChronology.INSTANCE.date(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)
            HijriDateInfo(
                day = day,
                month = month,
                monthName = HIJRI_MONTH_NAMES.getOrElse(month - 1) { "Muharram" },
                year = year,
                isAfterMaghribRollover = isAfterMaghribRollover
            )
        } catch (_: Exception) {
            kuwaitiFallback(adjustedDate, isAfterMaghribRollover)
        }
    }

    /**
     * Evaluates Sunnah / Wajib / Forbidden fasting status for today and tomorrow.
     */
    fun getFastingInfo(
        now: ZonedDateTime,
        maghribTime: ZonedDateTime?,
        hijriAdjustment: Int = 0
    ): FastingInfoSummary {
        val isAfterMaghrib = maghribTime != null && !now.isBefore(maghribTime)
        val baseDate = now.toLocalDate().plusDays(if (isAfterMaghrib) 1L else 0L)
        val tomorrowDate = baseDate.plusDays(1L)

        return FastingInfoSummary(
            today = evaluateSingleDayFasting(baseDate, hijriAdjustment),
            tomorrow = evaluateSingleDayFasting(tomorrowDate, hijriAdjustment)
        )
    }

    private fun evaluateSingleDayFasting(
        date: LocalDate,
        hijriAdjustment: Int
    ): FastingDayStatus {
        val hijri = convertLocalDateToHijri(date, hijriAdjustment)
        val hDay = hijri.day
        val hMonth = hijri.month
        val dayOfWeek = date.dayOfWeek

        // 1. Forbidden fasting days (Haram berpuasa)
        if (hMonth == 10 && hDay == 1) {
            return FastingDayStatus(
                isFasting = false,
                isForbidden = true,
                title = "Hari Raya Idul Fitri",
                description = "Diharamkan berpuasa pada 1 Syawal (Hari Raya Idul Fitri).",
                isSunnah = false
            )
        }
        if (hMonth == 12 && hDay == 10) {
            return FastingDayStatus(
                isFasting = false,
                isForbidden = true,
                title = "Hari Raya Idul Adha",
                description = "Diharamkan berpuasa pada 10 Dzulhijjah (Hari Raya Idul Adha).",
                isSunnah = false
            )
        }
        if (hMonth == 12 && hDay in 11..13) {
            return FastingDayStatus(
                isFasting = false,
                isForbidden = true,
                title = "Hari Tasyrik ($hDay Dzulhijjah)",
                description = "Diharamkan berpuasa pada Hari Tasyrik (11, 12, 13 Dzulhijjah).",
                isSunnah = false
            )
        }

        // 2. Ramadhan Wajib Fasting
        if (hMonth == 9) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa Ramadhan (Hari ke-$hDay)",
                description = "Ibadah Puasa Wajib Bulan Suci Ramadhan ${hijri.year} H.",
                isSunnah = false
            )
        }

        // 3. Major Annual Sunnah Fasts
        if (hMonth == 12 && hDay == 9) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa Arafah",
                description = "9 Dzulhijjah — Menghapus dosa setahun yang lalu dan setahun yang akan datang.",
                isSunnah = true
            )
        }
        if (hMonth == 12 && hDay == 8) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa Tarwiyah",
                description = "8 Dzulhijjah — Amalan sunnah menjelang Hari Arafah.",
                isSunnah = true
            )
        }
        if (hMonth == 1 && hDay == 9) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa Tasu'a",
                description = "9 Muharram — Puasa sunnah mengiringi Hari Asyura.",
                isSunnah = true
            )
        }
        if (hMonth == 1 && hDay == 10) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa Asyura",
                description = "10 Muharram — Menghapus dosa setahun yang telah lalu.",
                isSunnah = true
            )
        }

        // 4. Syawal (2..7 Syawal) - matches jws-android-algaffar priority
        if (hMonth == 10 && hDay in 2..7) {
            return FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = "Puasa 6 Hari Syawal",
                description = "Sunnah Syawal — ${hijri.formatted}",
                isSunnah = true
            )
        }

        // 5. Senin - Kamis & Ayyamul Bidh (13, 14, 15 of every Hijri month except Tasyrik above)
        val reasons = mutableListOf<String>()
        if (dayOfWeek == DayOfWeek.MONDAY) {
            reasons.add("Puasa Sunnah Senin")
        } else if (dayOfWeek == DayOfWeek.THURSDAY) {
            reasons.add("Puasa Sunnah Kamis")
        }
        if (hDay in 13..15) {
            reasons.add("Puasa Ayyamul Bidh")
        }

        return if (reasons.isNotEmpty()) {
            FastingDayStatus(
                isFasting = true,
                isForbidden = false,
                title = reasons.joinToString(" & "),
                description = "${hijri.day} ${hijri.monthName} — Sunnah Muakkad",
                isSunnah = true
            )
        } else {
            FastingDayStatus(
                isFasting = false,
                isForbidden = false,
                title = "Tidak Ada Puasa Sunnah Khusus",
                description = "Tanggal ${hijri.formatted}",
                isSunnah = false
            )
        }
    }

    /**
     * Scans the next [maxDays] days to find upcoming Islamic holidays (PHBI) and agendas.
     * Supports configurable Hijri, Gregorian, and Custom text Islamic agendas,
     * mirroring `getUpcomingIslamicEvents` in `jws-android-algaffar/src/utils/hijri.ts`.
     */
    fun getUpcomingIslamicEvents(
        now: ZonedDateTime,
        maghribTime: ZonedDateTime?,
        hijriAdjustment: Int = 0,
        maxDays: Int = 60,
        customEvents: List<CustomIslamicEventItem>? = null
    ): List<IslamicEventItem> {
        val isAfterMaghrib = maghribTime != null && !now.isBefore(maghribTime)
        val startDate = now.toLocalDate().plusDays(if (isAfterMaghrib) 1L else 0L)
        val activeEvents = customEvents ?: InfoDisplaySettings.defaultIslamicEvents()
        val safeMaxDays = maxDays.coerceAtLeast(1)
        val results = mutableListOf<IslamicEventItem>()
        val seenEventNames = mutableSetOf<String>()

        // 1. Date-based events (Hijri & Gregorian) across 0..safeMaxDays
        for (offset in 0..safeMaxDays) {
            val candidateDate = startDate.plusDays(offset.toLong())
            val hijri = convertLocalDateToHijri(candidateDate, hijriAdjustment)
            val dateStr = candidateDate.format(ISO_DATE_FORMATTER)

            for (ev in activeEvents) {
                if (!ev.enabled || ev.name.isBlank()) continue

                val isHijriMatch = ev.type == "hijri" &&
                    ev.hijriMonth == hijri.month &&
                    ev.hijriDay == hijri.day
                val isGregorianMatch = ev.type == "gregorian" &&
                    ev.gregorianDate.trim() == dateStr

                if (isHijriMatch || isGregorianMatch) {
                    if (seenEventNames.add(ev.name)) {
                        results.add(
                            IslamicEventItem(
                                name = ev.name,
                                hijriMonth = hijri.month,
                                hijriDay = hijri.day,
                                gregorianDate = candidateDate,
                                daysUntil = offset,
                                type = ev.type
                            )
                        )
                    }
                }
            }
        }

        // 2. Custom text-based Islamic agendas (always shown when enabled)
        for (ev in activeEvents) {
            if (!ev.enabled || ev.type != "custom" || ev.name.isBlank()) continue
            val todayHijri = convertLocalDateToHijri(startDate, hijriAdjustment)
            results.add(
                IslamicEventItem(
                    name = ev.name,
                    hijriMonth = todayHijri.month,
                    hijriDay = todayHijri.day,
                    gregorianDate = startDate,
                    daysUntil = -1,
                    customSubtitle = ev.customSubtitle.ifBlank { "Agenda Masjid" },
                    type = "custom"
                )
            )
        }

        return results
    }

    /**
     * Mathematical Kuwaiti algorithm fallback in case a date falls outside Umm al-Qura range.
     */
    private fun kuwaitiFallback(
        date: LocalDate,
        isAfterMaghribRollover: Boolean
    ): HijriDateInfo {
        val jd = date.toEpochDay() + 2440588L
        val l = jd - 1948440L + 10632L
        val n = floor((l - 1).toDouble() / 10631.0).toLong()
        val l2 = l - 10631L * n + 354L
        val j = (floor((10985L - l2).toDouble() / 5316.0) * floor((50L * l2).toDouble() / 17719.0) +
            floor(l2.toDouble() / 5670.0) * floor((43L * l2).toDouble() / 15238.0)).toLong()
        val l3 = l2 - (floor((30L - j).toDouble() / 15.0) * floor((17719L * j).toDouble() / 50.0) +
            floor(j.toDouble() / 16.0) * floor((15238L * j).toDouble() / 43.0)).toLong() + 29L
        val month = floor((24.0 * l3) / 709.0).toInt().coerceIn(1, 12)
        val day = (l3 - floor((709.0 * month) / 24.0).toLong()).toInt().coerceIn(1, 30)
        val year = (30L * n + j - 30L).toInt()

        return HijriDateInfo(
            day = day,
            month = month,
            monthName = HIJRI_MONTH_NAMES.getOrElse(month - 1) { "Muharram" },
            year = year,
            isAfterMaghribRollover = isAfterMaghribRollover
        )
    }
}
