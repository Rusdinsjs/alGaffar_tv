package id.algaffar.jwstv.domain.engine

import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Optional measured durations (in seconds) of actual local audio files for Tarhim & Adzan.
 * When present, `TARHIM` and `ADZAN` state durations automatically sync to the exact audio length.
 */
data class AudioFileDurations(
    val tarhimSecondsByPrayer: Map<PrayerId, Long> = emptyMap(),
    val adzanSecondsByPrayer: Map<PrayerId, Long> = emptyMap()
) {
    fun tarhimSecondsFor(prayerId: PrayerId): Long? = tarhimSecondsByPrayer[prayerId]
    fun adzanSecondsFor(prayerId: PrayerId): Long? = adzanSecondsByPrayer[prayerId]
}

/**
 * Snapshot produced on every 1-second tick of [JwsStateMachine].
 */
data class StateMachineTickResult(
    val state: AppState,
    val activePrayerId: PrayerId?,
    val countdownSeconds: Long,
    val totalStageSeconds: Long,
    val secondsUntilAdzan: Long?,
    val shouldTriggerBeep: Boolean,
    val shouldFadeOutAudio: Boolean,
    val isNoSunnahWarningActive: Boolean,
    val isManualOverride: Boolean = false
)

/**
 * Deterministic 27-State Machine for JWS TV Masjid Al-Gaffar.
 * Ports `stateMachine.ts`, `coreTransitions.ts`, `specialTransitions.ts`, `holidayTransitions.ts`,
 * and `helpers.ts` from the reference codebase.
 */
class JwsStateMachine(
    private val prayerTimeCalculator: PrayerTimeCalculator = PrayerTimeCalculator()
) {

    private var manualOverrideState: AppState? = null
    private var manualOverrideUntilEpochSec: Long? = null

    fun setManualOverride(state: AppState?, durationSeconds: Long? = null, nowEpochSec: Long = System.currentTimeMillis() / 1000L) {
        manualOverrideState = state
        manualOverrideUntilEpochSec = if (state != null && durationSeconds != null && durationSeconds > 0) {
            nowEpochSec + durationSeconds
        } else {
            null
        }
    }

    fun clearManualOverride() {
        manualOverrideState = null
        manualOverrideUntilEpochSec = null
    }

    /**
     * Evaluates the exact [AppState] and countdown timers for [now].
     */
    fun evaluateTick(
        now: ZonedDateTime,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule = prayerTimeCalculator.calculateSchedule(now, settings.prayerSettings),
        audioDurations: AudioFileDurations = AudioFileDurations()
    ): StateMachineTickResult {
        val zoneId = schedule.zoneId
        val zonedNow = now.withZoneSameInstant(zoneId)
        val nowEpochSec = zonedNow.toEpochSecond()

        // 1. Check Manual / Debug Override first
        val overrideState = manualOverrideState
        if (overrideState != null) {
            val expiry = manualOverrideUntilEpochSec
            if (expiry == null || nowEpochSec < expiry) {
                val remaining = if (expiry != null) (expiry - nowEpochSec).coerceAtLeast(0L) else 0L
                return StateMachineTickResult(
                    state = overrideState,
                    activePrayerId = schedule.nextPrayer.id.takeIf { it.isFiveTimeFardhu } ?: PrayerId.DHUHR,
                    countdownSeconds = remaining,
                    totalStageSeconds = remaining,
                    secondsUntilAdzan = null,
                    shouldTriggerBeep = false,
                    shouldFadeOutAudio = false,
                    isManualOverride = true,
                    isNoSunnahWarningActive = (overrideState == AppState.MENUNGGU_IQOMAH && remaining in 1L..120L)
                )
            } else {
                clearManualOverride()
            }
        }

        // 2. Priority: Holiday Modes (Idul Fitri, Idul Adha, Info Qurban)
        evaluateHolidayTransitions(zonedNow, settings)?.let { return it }

        // 3. Priority: 5-Time Prayer Cycle, Friday (Jum'at), Ramadhan (Tarawih) & Past-Prayer Recovery
        evaluatePrayerCycleAndRecovery(zonedNow, settings, schedule, audioDurations)?.let { return it }

        // 4. Priority: Scheduled Special Modes (CCTV, YouTube Live, Night ScreenSaver)
        evaluateScheduledSpecialModes(zonedNow, settings, schedule)?.let { return it }

        // 5. Fallback: Idle Alternation (`STANDBY` <-> `STANDBY_SLIDESHOW`)
        return evaluateIdleStandby(zonedNow, settings)
    }

    // ==================== 2. HOLIDAY & QURBAN TRANSITIONS ====================

    private fun evaluateHolidayTransitions(
        now: ZonedDateTime,
        settings: AppStateSettings
    ): StateMachineTickResult? {
        // 2A. Idul Fitri
        if (settings.idulFitri.enabled) {
            val fitri = settings.idulFitri
            for (slot in fitri.timeSlots.filter { it.enabled }) {
                val window = resolveTimeSlotWindow(now, slot) ?: continue
                val (start, end) = window
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return computeAlternatingHolidayState(
                        now = now,
                        slotStart = start,
                        fullscreenState = AppState.IDUL_FITRI,
                        standbyState = AppState.IDUL_FITRI_STANDBY,
                        fullscreenSec = fitri.fullscreenDuration.toLong().coerceAtLeast(5L),
                        standbySec = fitri.standbyDuration.toLong().coerceAtLeast(5L)
                    )
                }
                // Post-slot automatic Sholat Idul Fitri -> Khutbah Idul Fitri
                val elapsedAfterEnd = ChronoUnit.SECONDS.between(end, now)
                val sholatSec = fitri.sholatDuration.toLong().coerceAtLeast(1L) * 60L
                val khutbahSec = fitri.khutbahDuration.toLong().coerceAtLeast(1L) * 60L
                if (elapsedAfterEnd in 0L until sholatSec) {
                    val rem = sholatSec - elapsedAfterEnd
                    return StateMachineTickResult(
                        state = AppState.SHOLAT_IDUL_FITRI,
                        activePrayerId = null,
                        countdownSeconds = rem,
                        totalStageSeconds = sholatSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                } else if (elapsedAfterEnd in sholatSec until (sholatSec + khutbahSec)) {
                    val rem = (sholatSec + khutbahSec) - elapsedAfterEnd
                    return StateMachineTickResult(
                        state = AppState.KHUTBAH_IDUL_FITRI,
                        activePrayerId = null,
                        countdownSeconds = rem,
                        totalStageSeconds = khutbahSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                }
            }
        }

        // 2B. Idul Adha
        if (settings.idulAdha.enabled) {
            val adha = settings.idulAdha
            for (slot in adha.timeSlots.filter { it.enabled }) {
                val window = resolveTimeSlotWindow(now, slot) ?: continue
                val (start, end) = window
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return computeAlternatingHolidayState(
                        now = now,
                        slotStart = start,
                        fullscreenState = AppState.IDUL_ADHA,
                        standbyState = AppState.IDUL_ADHA_STANDBY,
                        fullscreenSec = adha.fullscreenDuration.toLong().coerceAtLeast(5L),
                        standbySec = adha.standbyDuration.toLong().coerceAtLeast(5L)
                    )
                }
                val elapsedAfterEnd = ChronoUnit.SECONDS.between(end, now)
                val sholatSec = adha.sholatDuration.toLong().coerceAtLeast(1L) * 60L
                val khutbahSec = adha.khutbahDuration.toLong().coerceAtLeast(1L) * 60L
                if (elapsedAfterEnd in 0L until sholatSec) {
                    val rem = sholatSec - elapsedAfterEnd
                    return StateMachineTickResult(
                        state = AppState.SHOLAT_IDUL_ADHA,
                        activePrayerId = null,
                        countdownSeconds = rem,
                        totalStageSeconds = sholatSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                } else if (elapsedAfterEnd in sholatSec until (sholatSec + khutbahSec)) {
                    val rem = (sholatSec + khutbahSec) - elapsedAfterEnd
                    return StateMachineTickResult(
                        state = AppState.KHUTBAH_IDUL_ADHA,
                        activePrayerId = null,
                        countdownSeconds = rem,
                        totalStageSeconds = khutbahSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                }
            }
        }

        // 2C. Info Qurban
        if (settings.infoQurban.enabled) {
            val qurban = settings.infoQurban
            for (slot in qurban.timeSlots.filter { it.enabled }) {
                val window = resolveTimeSlotWindow(now, slot) ?: continue
                val (start, end) = window
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return computeAlternatingHolidayState(
                        now = now,
                        slotStart = start,
                        fullscreenState = AppState.INFO_QURBAN,
                        standbyState = AppState.INFO_QURBAN_STANDBY,
                        fullscreenSec = qurban.fullscreenDuration.toLong().coerceAtLeast(5L),
                        standbySec = qurban.standbyDuration.toLong().coerceAtLeast(5L)
                    )
                }
            }
        }

        return null
    }

    private fun computeAlternatingHolidayState(
        now: ZonedDateTime,
        slotStart: ZonedDateTime,
        fullscreenState: AppState,
        standbyState: AppState,
        fullscreenSec: Long,
        standbySec: Long
    ): StateMachineTickResult {
        val cycleSec = fullscreenSec + standbySec
        val elapsedInSlot = ChronoUnit.SECONDS.between(slotStart, now).coerceAtLeast(0L)
        val posInCycle = elapsedInSlot % cycleSec
        return if (posInCycle < fullscreenSec) {
            StateMachineTickResult(
                state = fullscreenState,
                activePrayerId = null,
                countdownSeconds = fullscreenSec - posInCycle,
                totalStageSeconds = fullscreenSec,
                secondsUntilAdzan = null,
                shouldTriggerBeep = false,
                shouldFadeOutAudio = false,
                isNoSunnahWarningActive = false
            )
        } else {
            val rem = cycleSec - posInCycle
            StateMachineTickResult(
                state = standbyState,
                activePrayerId = null,
                countdownSeconds = rem,
                totalStageSeconds = standbySec,
                secondsUntilAdzan = null,
                shouldTriggerBeep = false,
                shouldFadeOutAudio = false,
                isNoSunnahWarningActive = false
            )
        }
    }

    // ==================== 3. PRAYER CYCLE & CRASH RECOVERY ====================

    private fun evaluatePrayerCycleAndRecovery(
        now: ZonedDateTime,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule,
        audioDurations: AudioFileDurations
    ): StateMachineTickResult? {
        // Check today's 5 fardhu prayers (and yesterday's Isha if near midnight for late Tarawih recovery)
        val candidates = mutableListOf<Pair<PrayerId, ZonedDateTime>>()

        // Add yesterday's Isha first so late-night Tarawih crossing 00:00 recovers cleanly
        val yesterdayDate = schedule.date.minusDays(1)
        val yesterdayTimes = prayerTimeCalculator.computeEightTimesForDate(
            yesterdayDate,
            schedule.zoneId,
            settings.prayerSettings
        )
        yesterdayTimes[PrayerId.ISHA]?.let { candidates.add(PrayerId.ISHA to it) }

        // Add today's 5 fardhu prayers
        for (prayerId in PrayerId.FIVE_FARDHU) {
            candidates.add(prayerId to schedule.timeFor(prayerId))
        }

        for ((prayerId, prayerTime) in candidates) {
            val stageConfig = settings.prayers.forPrayer(prayerId)
            val isFridayDhuhr = (prayerId == PrayerId.DHUHR &&
                prayerTime.dayOfWeek == DayOfWeek.FRIDAY &&
                settings.jumat.enabled)

            val tarhimSec = (audioDurations.tarhimSecondsFor(prayerId)
                ?: (stageConfig.tarhimDuration.toLong().coerceAtLeast(0L) * 60L))
            val tartilWindowSec = stageConfig.tartilDuration.toLong().coerceAtLeast(0L) * 60L
            // Pre-Adzan total window starts at maxOf(tartilWindowSec + tarhimSec, tartilWindowSec)
            // Note: In reference app, tartilDuration is minutes before Adzan (or before Tarhim if tartilDuration > tarhimDuration).
            // If tartilDuration = 10m and tarhimDuration = 5m, Tartil runs from -10m to -5m, and Tarhim runs from -5m to 0m.
            // If tarhim audio is longer than tartilDuration, total pre-adzan window starts at tarhimSec.
            val preAdzanStartSec = if (tartilWindowSec > tarhimSec) {
                tartilWindowSec
            } else {
                tartilWindowSec + tarhimSec
            }

            val secondsUntilAdzan = ChronoUnit.SECONDS.between(now, prayerTime)

            // A. Pre-Adzan: Tartil & Tarhim
            if (secondsUntilAdzan in 1L..preAdzanStartSec) {
                if (tarhimSec > 0L && secondsUntilAdzan <= tarhimSec) {
                    // Inside TARHIM
                    return StateMachineTickResult(
                        state = AppState.TARHIM,
                        activePrayerId = prayerId,
                        countdownSeconds = secondsUntilAdzan,
                        totalStageSeconds = tarhimSec,
                        secondsUntilAdzan = secondsUntilAdzan,
                        shouldTriggerBeep = (secondsUntilAdzan in 1L..3L),
                        shouldFadeOutAudio = (secondsUntilAdzan in 1L..3L),
                        isNoSunnahWarningActive = false
                    )
                } else if (tartilWindowSec > 0L) {
                    // Inside TARTIL
                    val remainingInTartil = (secondsUntilAdzan - tarhimSec).coerceAtLeast(1L)
                    val totalTartilSec = (preAdzanStartSec - tarhimSec).coerceAtLeast(1L)
                    return StateMachineTickResult(
                        state = AppState.TARTIL,
                        activePrayerId = prayerId,
                        countdownSeconds = remainingInTartil,
                        totalStageSeconds = totalTartilSec,
                        secondsUntilAdzan = secondsUntilAdzan,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = (remainingInTartil in 1L..3L),
                        isNoSunnahWarningActive = false
                    )
                }
            }

            // B. At & Post-Adzan (elapsed >= 0) — Includes Crash/Reboot Recovery
            val elapsedSec = ChronoUnit.SECONDS.between(prayerTime, now)
            if (elapsedSec >= 0L) {
                val adzanSec = if (stageConfig.isAdzan) {
                    audioDurations.adzanSecondsFor(prayerId)
                        ?: (stageConfig.adzanDuration.toLong().coerceAtLeast(1L) * 60L)
                } else {
                    stageConfig.adzanDuration.toLong().coerceAtLeast(1L) * 60L
                }

                // 1) ADZAN phase
                if (elapsedSec < adzanSec) {
                    val rem = adzanSec - elapsedSec
                    return StateMachineTickResult(
                        state = AppState.ADZAN,
                        activePrayerId = prayerId,
                        countdownSeconds = rem,
                        totalStageSeconds = adzanSec,
                        secondsUntilAdzan = 0L,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = (rem in 1L..3L),
                        isNoSunnahWarningActive = false
                    )
                }

                // 2) Friday (Jum'at) sequence: WAKTU_JUMAT -> KHUTBAH -> PELAKSANAAN_JUMAT
                if (isFridayDhuhr) {
                    val waktuJumatSec = settings.jumat.waktuJumatDuration.toLong().coerceAtLeast(1L) * 60L
                    val khutbahSec = settings.jumat.khutbahDuration.toLong().coerceAtLeast(1L) * 60L
                    val sholatJumatSec = settings.jumat.sholatJumatDuration.toLong().coerceAtLeast(1L) * 60L

                    val endWaktuJumat = adzanSec + waktuJumatSec
                    val endKhutbah = endWaktuJumat + khutbahSec
                    val endSholatJumat = endKhutbah + sholatJumatSec

                    when (elapsedSec) {
                        in adzanSec until endWaktuJumat -> {
                            val rem = endWaktuJumat - elapsedSec
                            return StateMachineTickResult(
                                state = AppState.WAKTU_JUMAT,
                                activePrayerId = PrayerId.DHUHR,
                                countdownSeconds = rem,
                                totalStageSeconds = waktuJumatSec,
                                secondsUntilAdzan = null,
                                shouldTriggerBeep = false,
                                shouldFadeOutAudio = false,
                                isNoSunnahWarningActive = false
                            )
                        }
                        in endWaktuJumat until endKhutbah -> {
                            val rem = endKhutbah - elapsedSec
                            return StateMachineTickResult(
                                state = AppState.KHUTBAH,
                                activePrayerId = PrayerId.DHUHR,
                                countdownSeconds = rem,
                                totalStageSeconds = khutbahSec,
                                secondsUntilAdzan = null,
                                shouldTriggerBeep = false,
                                shouldFadeOutAudio = false,
                                isNoSunnahWarningActive = false
                            )
                        }
                        in endKhutbah until endSholatJumat -> {
                            val rem = endSholatJumat - elapsedSec
                            return StateMachineTickResult(
                                state = AppState.PELAKSANAAN_JUMAT,
                                activePrayerId = PrayerId.DHUHR,
                                countdownSeconds = rem,
                                totalStageSeconds = sholatJumatSec,
                                secondsUntilAdzan = null,
                                shouldTriggerBeep = false,
                                shouldFadeOutAudio = false,
                                isNoSunnahWarningActive = false
                            )
                        }
                    }
                } else {
                    // 3) Regular 5-time prayer sequence: MENUNGGU_IQOMAH -> PELAKSANAAN_SHOLAT
                    val iqomahSec = stageConfig.iqomahDuration.toLong().coerceAtLeast(1L) * 60L
                    val sholatSec = stageConfig.sholatDuration.toLong().coerceAtLeast(1L) * 60L

                    val endIqomah = adzanSec + iqomahSec
                    val endSholat = endIqomah + sholatSec

                    if (elapsedSec in adzanSec until endIqomah) {
                        val rem = endIqomah - elapsedSec
                        return StateMachineTickResult(
                            state = AppState.MENUNGGU_IQOMAH,
                            activePrayerId = prayerId,
                            countdownSeconds = rem,
                            totalStageSeconds = iqomahSec,
                            secondsUntilAdzan = null,
                            shouldTriggerBeep = (rem in 1L..5L),
                            shouldFadeOutAudio = false,
                            isNoSunnahWarningActive = (rem in 1L..120L)
                        )
                    }

                    if (elapsedSec in endIqomah until endSholat) {
                        val rem = endSholat - elapsedSec
                        return StateMachineTickResult(
                            state = AppState.PELAKSANAAN_SHOLAT,
                            activePrayerId = prayerId,
                            countdownSeconds = rem,
                            totalStageSeconds = sholatSec,
                            secondsUntilAdzan = null,
                            shouldTriggerBeep = false,
                            shouldFadeOutAudio = false,
                            isNoSunnahWarningActive = false
                        )
                    }

                    // 4) Ramadhan Post-Isha sequence: TARAWIH -> SHOLAT_TARAWIH
                    if (prayerId == PrayerId.ISHA && settings.ramadhan.isRamadhan) {
                        val tarawihSec = settings.ramadhan.tarawihDuration.toLong().coerceAtLeast(1L) * 60L
                        val sholatTarawihSec = settings.ramadhan.sholatTarawihDuration.toLong().coerceAtLeast(1L) * 60L

                        val endTarawihInfo = endSholat + tarawihSec
                        val endSholatTarawih = endTarawihInfo + sholatTarawihSec

                        if (elapsedSec in endSholat until endTarawihInfo) {
                            val rem = endTarawihInfo - elapsedSec
                            return StateMachineTickResult(
                                state = AppState.TARAWIH,
                                activePrayerId = PrayerId.ISHA,
                                countdownSeconds = rem,
                                totalStageSeconds = tarawihSec,
                                secondsUntilAdzan = null,
                                shouldTriggerBeep = false,
                                shouldFadeOutAudio = false,
                                isNoSunnahWarningActive = false
                            )
                        }

                        if (elapsedSec in endTarawihInfo until endSholatTarawih) {
                            val rem = endSholatTarawih - elapsedSec
                            return StateMachineTickResult(
                                state = AppState.SHOLAT_TARAWIH,
                                activePrayerId = PrayerId.ISHA,
                                countdownSeconds = rem,
                                totalStageSeconds = sholatTarawihSec,
                                secondsUntilAdzan = null,
                                shouldTriggerBeep = false,
                                shouldFadeOutAudio = false,
                                isNoSunnahWarningActive = false
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    // ==================== 4. SCHEDULED SPECIAL MODES ====================

    private fun evaluateScheduledSpecialModes(
        now: ZonedDateTime,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule
    ): StateMachineTickResult? {
        // 4A. Scheduled CCTV
        if (settings.cctv.enabled && settings.cctv.cameras.any { it.enabled }) {
            for (trigger in settings.cctv.schedules.filter { it.enabled }) {
                val prayerId = PrayerId.fromId(trigger.prayerId) ?: continue
                if (!prayerId.isFiveTimeFardhu) continue
                val refTime = resolveTriggerReferenceTime(prayerId, trigger.referencePoint, settings, schedule)
                val startTime = refTime.plusMinutes(trigger.startOffsetMinutes.toLong())
                val durationSec = trigger.durationMinutes.toLong().coerceAtLeast(1L) * 60L
                val endTime = startTime.plusSeconds(durationSec)

                if (!now.isBefore(startTime) && now.isBefore(endTime)) {
                    val rem = ChronoUnit.SECONDS.between(now, endTime)
                    return StateMachineTickResult(
                        state = AppState.CCTV,
                        activePrayerId = prayerId,
                        countdownSeconds = rem,
                        totalStageSeconds = durationSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                }
            }
        }

        // 4B. Scheduled YouTube Live
        if (settings.live.enabled) {
            for (trigger in settings.live.schedules.filter { it.enabled }) {
                val prayerId = PrayerId.fromId(trigger.prayerId) ?: continue
                if (!prayerId.isFiveTimeFardhu) continue
                val refTime = resolveTriggerReferenceTime(prayerId, trigger.referencePoint, settings, schedule)
                val startTime = refTime.plusMinutes(trigger.startOffsetMinutes.toLong())
                val durationSec = trigger.durationMinutes.toLong().coerceAtLeast(1L) * 60L
                val endTime = startTime.plusSeconds(durationSec)

                if (!now.isBefore(startTime) && now.isBefore(endTime)) {
                    val rem = ChronoUnit.SECONDS.between(now, endTime)
                    return StateMachineTickResult(
                        state = AppState.LIVE,
                        activePrayerId = prayerId,
                        countdownSeconds = rem,
                        totalStageSeconds = durationSec,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                }
            }
        }

        // 4C. Night ScreenSaver Schedule (Supports overnight spans e.g. 22:00 -> 03:00)
        if (settings.screensaver.enabled) {
            val currentLocalTime = now.toLocalTime()
            for (sched in settings.screensaver.schedules.filter { it.enabled }) {
                val start = parseLocalTimeOrNull(sched.startTime) ?: continue
                val end = parseLocalTimeOrNull(sched.endTime) ?: continue
                val isInWindow = if (start.isBefore(end)) {
                    !currentLocalTime.isBefore(start) && currentLocalTime.isBefore(end)
                } else {
                    // Overnight window (e.g. 22:00 to 03:00)
                    !currentLocalTime.isBefore(start) || currentLocalTime.isBefore(end)
                }
                if (isInWindow) {
                    return StateMachineTickResult(
                        state = AppState.SCREENSAVER,
                        activePrayerId = null,
                        countdownSeconds = schedule.secondsToNextPrayer,
                        totalStageSeconds = schedule.secondsToNextPrayer,
                        secondsUntilAdzan = null,
                        shouldTriggerBeep = false,
                        shouldFadeOutAudio = false,
                        isNoSunnahWarningActive = false
                    )
                }
            }
        }

        return null
    }

    private fun resolveTriggerReferenceTime(
        prayerId: PrayerId,
        referencePoint: String,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule
    ): ZonedDateTime {
        val adzanTime = schedule.timeFor(prayerId)
        val stageConfig = settings.prayers.forPrayer(prayerId)
        return if (referencePoint.equals("sholat", ignoreCase = true)) {
            val totalAfterAdzanMin = stageConfig.adzanDuration +
                stageConfig.iqomahDuration +
                stageConfig.sholatDuration
            adzanTime.plusMinutes(totalAfterAdzanMin.toLong())
        } else {
            // Default: relative to start of Tartil
            adzanTime.minusMinutes(stageConfig.tartilDuration.toLong())
        }
    }

    // ==================== 5. IDLE STANDBY & SLIDESHOW ALTERNATION ====================

    private fun evaluateIdleStandby(
        now: ZonedDateTime,
        settings: AppStateSettings
    ): StateMachineTickResult {
        val slideshowSettings = settings.standbySlideshow
        val slideshowSlides = settings.slides.filter { it.enabled && it.showInSlideshow }

        if (slideshowSettings.enabled && slideshowSlides.isNotEmpty()) {
            val mainSec = slideshowSettings.durationMain.toLong().coerceAtLeast(10L)
            val singleCycleSec = slideshowSlides.sumOf { it.duration.toLong().coerceAtLeast(3L) }
            val slideshowSec = (singleCycleSec * slideshowSettings.repeatCount.toLong().coerceAtLeast(1L))
                .coerceAtLeast(5L)

            val totalCycle = mainSec + slideshowSec
            val secOfDay = now.toLocalTime().toSecondOfDay().toLong()
            val pos = secOfDay % totalCycle

            return if (pos < mainSec) {
                StateMachineTickResult(
                    state = AppState.STANDBY,
                    activePrayerId = null,
                    countdownSeconds = mainSec - pos,
                    totalStageSeconds = mainSec,
                    secondsUntilAdzan = null,
                    shouldTriggerBeep = false,
                    shouldFadeOutAudio = false,
                    isNoSunnahWarningActive = false
                )
            } else {
                val rem = totalCycle - pos
                StateMachineTickResult(
                    state = AppState.STANDBY_SLIDESHOW,
                    activePrayerId = null,
                    countdownSeconds = rem,
                    totalStageSeconds = slideshowSec,
                    secondsUntilAdzan = null,
                    shouldTriggerBeep = false,
                    shouldFadeOutAudio = false,
                    isNoSunnahWarningActive = false
                )
            }
        }

        return StateMachineTickResult(
            state = AppState.STANDBY,
            activePrayerId = null,
            countdownSeconds = 0L,
            totalStageSeconds = 0L,
            secondsUntilAdzan = null,
            shouldTriggerBeep = false,
            shouldFadeOutAudio = false,
            isNoSunnahWarningActive = false
        )
    }

    private fun resolveTimeSlotWindow(
        now: ZonedDateTime,
        slot: TimeSlot
    ): Pair<ZonedDateTime, ZonedDateTime>? {
        val startLocal = parseLocalTimeOrNull(slot.startTime) ?: return null
        val endLocal = parseLocalTimeOrNull(slot.endTime) ?: return null
        val today = now.toLocalDate()
        val zone = now.zone
        val start = ZonedDateTime.of(today, startLocal, zone)
        val end = if (endLocal.isAfter(startLocal)) {
            ZonedDateTime.of(today, endLocal, zone)
        } else {
            ZonedDateTime.of(today.plusDays(1), endLocal, zone)
        }
        return start to end
    }

    private fun parseLocalTimeOrNull(hhMm: String): LocalTime? {
        return try {
            LocalTime.parse(hhMm.trim(), DateTimeFormatter.ofPattern("H:mm"))
        } catch (_: Exception) {
            try {
                LocalTime.parse(hhMm.trim())
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        /**
         * Snaps any [date] forward to the nearest Friday (if already Friday, returns [date]).
         * Mirrors `snapToNearestFriday` in `JumatSettingsForm.tsx`.
         */
        fun snapToNearestFriday(date: LocalDate): LocalDate {
            val currentDow = date.dayOfWeek.value // 1=Mon..5=Fri..7=Sun
            val daysToAdd = when {
                currentDow <= DayOfWeek.FRIDAY.value -> (DayOfWeek.FRIDAY.value - currentDow).toLong()
                else -> (7 - currentDow + DayOfWeek.FRIDAY.value).toLong()
            }
            return date.plusDays(daysToAdd)
        }

        /**
         * Finds the effective Friday Petugas date key (`YYYY-MM-DD`) to display on TV.
         * Valid window: From H-6 (Saturday after previous Friday) up to and including the target Friday.
         * Mirrors `findEffectivePetugasJumatDate` in `src/contexts/appState/helpers.ts`.
         */
        fun findEffectivePetugasJumatDate(
            records: Map<String, *>,
            today: LocalDate
        ): String? {
            if (records.isEmpty()) return null
            val parsedDates = records.keys.mapNotNull { key ->
                runCatching { LocalDate.parse(key) to key }.getOrNull()
            }.sortedBy { it.first }

            // 1. Look for a record whose Friday is >= today and within 6 days (H-6 to H)
            for ((recordDate, key) in parsedDates) {
                val diffDays = ChronoUnit.DAYS.between(today, recordDate)
                if (diffDays in 0L..6L) {
                    return key
                }
            }

            // 2. Fallback: nearest upcoming date, or most recent past date
            return parsedDates.firstOrNull { !it.first.isBefore(today) }?.second
                ?: parsedDates.lastOrNull()?.second
        }

        /**
         * Finds the effective Friday Finance (`Keuangan Jum'at`) date key (`YYYY-MM-DD`) to display on TV.
         * Prefers the most recent Friday report `<= today`, or falls back to the latest available key.
         */
        fun findEffectiveJumatKeuanganDate(
            records: Map<String, *>,
            today: LocalDate
        ): String? {
            if (records.isEmpty()) return null
            val parsedDates = records.keys.mapNotNull { key ->
                runCatching { LocalDate.parse(key) to key }.getOrNull()
            }.sortedBy { it.first }

            return parsedDates.lastOrNull { !it.first.isAfter(today) }?.second
                ?: parsedDates.firstOrNull()?.second
        }
    }
}
