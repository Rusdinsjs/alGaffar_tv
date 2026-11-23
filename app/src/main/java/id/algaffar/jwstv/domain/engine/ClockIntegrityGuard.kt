package id.algaffar.jwstv.domain.engine

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Protects low-cost Android STBs without a CMOS/RTC battery against clock resets
 * (e.g. jumping back to 1970-01-01 or 2015-01-01 after an offline power outage).
 *
 * Supports:
 * 1. Detecting suspect system clocks (`year < 2025` or backward jump > 1 hour from last valid anchor).
 * 2. Applying a temporary `softwareClockOffsetMs` synced from a caretaker's smartphone via Remote PWA (`POST /api/sync_time`).
 * 3. Automatically clearing the software offset once Android NTP restores the real system clock.
 */
class ClockIntegrityGuard(
    private val anchorFile: File? = null
) {
    @Volatile
    private var softwareClockOffsetMs: Long = 0L

    @Volatile
    private var lastValidAnchorEpochMs: Long = loadAnchorFromDisk()

    @Volatile
    private var lastAnchorPersistMs: Long = 0L

    val currentOffsetMs: Long
        get() = softwareClockOffsetMs

    fun isRawClockSuspect(rawSystemEpochMs: Long = System.currentTimeMillis()): Boolean {
        val rawYear = Instant.ofEpochMilli(rawSystemEpochMs).atZone(ZoneId.of("UTC")).year
        if (rawYear < MIN_VALID_YEAR) return true
        if (lastValidAnchorEpochMs > 0L && rawSystemEpochMs < lastValidAnchorEpochMs - BACKWARD_JUMP_TOLERANCE_MS) {
            return true
        }
        return false
    }

    fun isEffectiveClockSuspect(rawSystemEpochMs: Long = System.currentTimeMillis()): Boolean {
        val effectiveMs = rawSystemEpochMs + softwareClockOffsetMs
        val year = Instant.ofEpochMilli(effectiveMs).atZone(ZoneId.of("UTC")).year
        if (year < MIN_VALID_YEAR) return true
        if (lastValidAnchorEpochMs > 0L && effectiveMs < lastValidAnchorEpochMs - BACKWARD_JUMP_TOLERANCE_MS) {
            return true
        }
        return false
    }

    @Synchronized
    fun syncFromTrustedReference(
        trustedEpochMs: Long,
        rawSystemEpochMs: Long = System.currentTimeMillis()
    ): Result<Long> {
        val trustedYear = Instant.ofEpochMilli(trustedEpochMs).atZone(ZoneId.of("UTC")).year
        if (trustedYear !in MIN_VALID_YEAR..2100) {
            return Result.failure(IllegalArgumentException("Waktu referensi tidak valid (tahun $trustedYear)"))
        }
        val offset = trustedEpochMs - rawSystemEpochMs
        softwareClockOffsetMs = if (kotlin.math.abs(offset) < 3_000L) 0L else offset
        if (trustedEpochMs > lastValidAnchorEpochMs) {
            lastValidAnchorEpochMs = trustedEpochMs
            persistAnchorToDisk(trustedEpochMs)
        }
        return Result.success(softwareClockOffsetMs)
    }

    @Synchronized
    fun effectiveNow(
        zoneId: ZoneId = ZoneId.systemDefault(),
        rawSystemEpochMs: Long = System.currentTimeMillis()
    ): ZonedDateTime {
        // Auto-heal: if we had a large software offset due to No-RTC boot (e.g. 1970/2015)
        // and Android NTP later updated the OS clock back to a valid modern timestamp, clear the offset!
        if (softwareClockOffsetMs != 0L && !isRawClockSuspect(rawSystemEpochMs)) {
            if (kotlin.math.abs(softwareClockOffsetMs) > NTP_AUTO_CLEAR_THRESHOLD_MS) {
                softwareClockOffsetMs = 0L
            }
        }

        val effectiveMs = rawSystemEpochMs + softwareClockOffsetMs
        if (!isEffectiveClockSuspect(rawSystemEpochMs)) {
            if (effectiveMs > lastValidAnchorEpochMs && (rawSystemEpochMs - lastAnchorPersistMs) >= ANCHOR_SAVE_INTERVAL_MS) {
                lastValidAnchorEpochMs = effectiveMs
                lastAnchorPersistMs = rawSystemEpochMs
                persistAnchorToDisk(effectiveMs)
            }
        }

        return Instant.ofEpochMilli(effectiveMs).atZone(zoneId)
    }

    private fun loadAnchorFromDisk(): Long {
        val f = anchorFile ?: return 0L
        return runCatching {
            if (f.exists()) f.readText(Charsets.UTF_8).trim().toLongOrNull() ?: 0L else 0L
        }.getOrDefault(0L)
    }

    private fun persistAnchorToDisk(epochMs: Long) {
        val f = anchorFile ?: return
        runCatching {
            f.parentFile?.mkdirs()
            f.writeText(epochMs.toString(), Charsets.UTF_8)
        }
    }

    companion object {
        const val MIN_VALID_YEAR = 2025
        private const val BACKWARD_JUMP_TOLERANCE_MS = 3_600_000L // 1 hour
        private const val NTP_AUTO_CLEAR_THRESHOLD_MS = 3_600_000L // 1 hour
        private const val ANCHOR_SAVE_INTERVAL_MS = 3_600_000L // persist anchor at most once per hour
    }
}
