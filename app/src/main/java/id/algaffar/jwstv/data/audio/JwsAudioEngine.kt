package id.algaffar.jwstv.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import id.algaffar.jwstv.domain.engine.AudioFileDurations
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.StateMachineTickResult
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.PrayerId
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Media3 [ExoPlayer] Audio Orchestrator for JWS TV Masjid Al-Gaffar.
 * Handles:
 * 1. Folder hierarchy playlist resolution (`audio/{category}/{prayerId}/[files]` -> `{prayerId}.mp3` -> `default/` -> `default.mp3`)
 * 2. Exact MP3 duration scanning for Tarhim & Adzan auto-sync (`AudioFileDurations`)
 * 3. Smooth 3-second volume fade-out before state transitions
 * 4. 1000Hz countdown beeps via [BeepGenerator] (3x before Adzan, 5x before Iqomah)
 * 5. Ramadhan Sirine playback (Maghrib, Sahur, Imsak)
 * 6. Live & Scheduled Custom Audio playback
 * Mirrors `src/components/Audio/AudioPlayer.tsx` and `src/hooks/useSirine.ts`.
 */
class JwsAudioEngine(
    private val context: Context,
    private val assetsRootDir: File,
    val beepGenerator: BeepGenerator = BeepGenerator()
) {
    private var exoPlayer: ExoPlayer? = null
    private var activePlaybackKey: String? = null
    private var lastSirineTriggerMinuteKey: String? = null
    private var lastScheduledAudioMinuteKey: String? = null

    private val hhMmFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also {
            exoPlayer = it
        }
    }

    /**
     * Scans `jws-assets/audio/tarhim` and `jws-assets/audio/adzan` to measure exact audio durations
     * in seconds so [id.algaffar.jwstv.domain.engine.JwsStateMachine] can sync `TARHIM` and `ADZAN` timers.
     */
    fun scanAudioFileDurations(): AudioFileDurations {
        val tarhimMap = mutableMapOf<PrayerId, Long>()
        val adzanMap = mutableMapOf<PrayerId, Long>()

        for (prayerId in PrayerId.FIVE_FARDHU) {
            val tarhimFiles = resolvePlaylistFiles("tarhim", prayerId, assetsRootDir)
            if (tarhimFiles.isNotEmpty()) {
                val totalSec = tarhimFiles.sumOf { readAudioDurationSeconds(it) }
                if (totalSec > 5L) {
                    tarhimMap[prayerId] = totalSec
                }
            }

            val adzanFiles = resolvePlaylistFiles("adzan", prayerId, assetsRootDir)
            if (adzanFiles.isNotEmpty()) {
                val totalSec = adzanFiles.sumOf { readAudioDurationSeconds(it) }
                if (totalSec > 5L) {
                    adzanMap[prayerId] = totalSec
                }
            }
        }

        return AudioFileDurations(
            tarhimSecondsByPrayer = tarhimMap,
            adzanSecondsByPrayer = adzanMap
        )
    }

    /**
     * Called on every 1Hz tick on the Main thread to synchronize audio playback, fade-outs, beeps, and sirens.
     */
    fun onTick(
        now: ZonedDateTime,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule,
        tickResult: StateMachineTickResult
    ) {
        // 1. Trigger 1000Hz Beep if requested by StateMachine (3x before Adzan, 5x before Iqomah)
        if (tickResult.shouldTriggerBeep) {
            beepGenerator.triggerCountdownBeepOncePerSecond(now.toEpochSecond())
        }

        // 2. Check Ramadhan Sirine triggers (Maghrib, Sahur, Imsak)
        checkAndTriggerRamadhanSirine(now, settings, schedule)

        // 3. Determine target audio playlist based on active AppState
        val state = tickResult.state
        val prayerId = tickResult.activePrayerId ?: PrayerId.DHUHR

        val desiredCategory: String? = when (state) {
            AppState.TARTIL -> "tartil"
            AppState.TARHIM -> "tarhim"
            AppState.ADZAN -> {
                val stageCfg = settings.prayers.forPrayer(prayerId)
                if (stageCfg.isAdzan) "adzan" else null
            }
            else -> null
        }

        if (desiredCategory != null) {
            val key = "$desiredCategory:${prayerId.id}"
            if (activePlaybackKey != key) {
                val files = resolvePlaylistFiles(desiredCategory, prayerId, assetsRootDir)
                if (files.isNotEmpty()) {
                    startPlaylist(
                        key = key,
                        files = files,
                        loop = (desiredCategory == "tartil")
                    )
                } else {
                    stopPlayback()
                }
            }

            // Apply smooth 3-second volume fade-out if nearing state transition
            val player = exoPlayer
            if (player != null) {
                if (tickResult.shouldFadeOutAudio) {
                    val targetVolume = when (tickResult.countdownSeconds) {
                        3L -> 0.72f
                        2L -> 0.42f
                        1L -> 0.16f
                        else -> 1.0f
                    }
                    player.volume = targetVolume
                } else if (player.volume < 1.0f) {
                    player.volume = 1.0f
                }
            }
            return
        }

        // 4. If in STANDBY, check Live or Scheduled Custom Audio
        if (state == AppState.STANDBY) {
            if (settings.customAudio.isPlayingLive && settings.customAudio.liveAudioFile.isNotBlank()) {
                val file = resolveSingleAssetFile(settings.customAudio.liveAudioFile)
                val key = "custom_live:${settings.customAudio.liveAudioFile}"
                if (file != null && activePlaybackKey != key) {
                    startPlaylist(key, listOf(file), loop = false)
                }
                return
            }

            checkAndTriggerScheduledCustomAudio(now, settings)
            if (activePlaybackKey?.startsWith("tartil:") == true ||
                activePlaybackKey?.startsWith("tarhim:") == true ||
                activePlaybackKey?.startsWith("adzan:") == true
            ) {
                stopPlayback()
            }
        } else {
            // Any quiet prayer execution state immediately stops background audio
            if (state.isQuietPrayerExecution || state == AppState.MENUNGGU_IQOMAH) {
                stopPlayback()
            }
        }
    }

    private fun checkAndTriggerRamadhanSirine(
        now: ZonedDateTime,
        settings: AppStateSettings,
        schedule: DailyPrayerSchedule
    ) {
        val ramadhan = settings.ramadhan
        val sirine = ramadhan.sirine
        if (!ramadhan.isRamadhan || !sirine.enabled) return

        val currentHhMm = now.format(hhMmFormatter)
        val minuteKey = "${now.toLocalDate()}_$currentHhMm"
        if (lastSirineTriggerMinuteKey == minuteKey) return

        val maghribHhMm = schedule.formattedFor(PrayerId.MAGHRIB)
        val imsakHhMm = schedule.formattedFor(PrayerId.IMSAK)
        val sahurHhMm = sirine.sahurTime.trim()

        val shouldFire = (sirine.playAtMaghrib && currentHhMm == maghribHhMm) ||
            (sirine.playAtImsak && currentHhMm == imsakHhMm) ||
            (sirine.playAtSahur && currentHhMm == sahurHhMm)

        if (shouldFire && now.second in 0..5) {
            lastSirineTriggerMinuteKey = minuteKey
            val sirineFile = resolveSingleAssetFile(sirine.audioFile)
            if (sirineFile != null) {
                val repeatedFiles = List(sirine.repeatCount.coerceIn(1, 5)) { sirineFile }
                startPlaylist("sirine:$minuteKey", repeatedFiles, loop = false)
            } else {
                beepGenerator.playSynthesizedSirenSweep()
            }
        }
    }

    private fun checkAndTriggerScheduledCustomAudio(
        now: ZonedDateTime,
        settings: AppStateSettings
    ) {
        val currentHhMm = now.format(hhMmFormatter)
        val todayStr = now.toLocalDate().toString()
        // Convert Java DayOfWeek (1=Mon..7=Sun) to JS 0=Sun..6=Sat
        val jsDayOfWeek = now.dayOfWeek.value % 7
        val minuteKey = "${todayStr}_$currentHhMm"
        if (lastScheduledAudioMinuteKey == minuteKey) return

        for (item in settings.customAudio.schedules.filter { it.enabled }) {
            val matchesDay = if (item.scheduleType.equals("once", ignoreCase = true)) {
                item.date == todayStr
            } else {
                item.dayOfWeek == jsDayOfWeek
            }
            if (matchesDay && item.time.trim() == currentHhMm) {
                lastScheduledAudioMinuteKey = minuteKey
                val file = resolveSingleAssetFile(item.audioFile)
                if (file != null) {
                    startPlaylist("scheduled:${item.id}:$minuteKey", listOf(file), loop = false)
                }
                break
            }
        }
    }

    private fun startPlaylist(key: String, files: List<File>, loop: Boolean) {
        runCatching {
            val player = getOrCreatePlayer()
            player.stop()
            player.clearMediaItems()
            files.forEach { file ->
                player.addMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            }
            player.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            player.volume = 1.0f
            player.prepare()
            player.play()
            activePlaybackKey = key
        }
    }

    fun stopPlayback() {
        activePlaybackKey = null
        runCatching {
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()
        }
    }

    fun release() {
        activePlaybackKey = null
        runCatching {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    private fun resolveSingleAssetFile(path: String): File? {
        if (path.isBlank()) return null
        val clean = path.removePrefix("jws-assets/").removePrefix("/")
        return File(assetsRootDir, clean).takeIf { it.exists() && it.isFile }
            ?: File(path).takeIf { it.exists() && it.isFile }
    }

    private fun readAudioDurationSeconds(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationMsStr?.toLongOrNull() ?: 0L
            (durationMs / 1000L).coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "ogg", "aac", "flac")

        /**
         * Resolves the audio playlist hierarchy for a given [category] (`tartil`, `tarhim`, `adzan`)
         * and [prayerId] (`fajr`, `dhuhr`, `asr`, `maghrib`, `isha`):
         * 1. `audio/{category}/{prayerId}/[tracks]`
         * 2. `audio/{category}/{prayerId}.mp3`
         * 3. `audio/{category}/default/[tracks]`
         * 4. `audio/{category}/default.mp3`
         */
        fun resolvePlaylistFiles(
            category: String,
            prayerId: PrayerId,
            assetsRootDir: File
        ): List<File> {
            val categoryDir = File(assetsRootDir, "audio/$category")
            if (!categoryDir.exists()) return emptyList()

            // 1. Folder `audio/{category}/{prayerId}/`
            val prayerFolder = File(categoryDir, prayerId.id)
            if (prayerFolder.exists() && prayerFolder.isDirectory) {
                val list = prayerFolder.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in SUPPORTED_AUDIO_EXTENSIONS }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                if (list.isNotEmpty()) return list
            }

            // 2. Single file `audio/{category}/{prayerId}.{ext}`
            for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                val singleFile = File(categoryDir, "${prayerId.id}.$ext")
                if (singleFile.exists() && singleFile.isFile) {
                    return listOf(singleFile)
                }
            }

            // 3. Default folder `audio/{category}/default/`
            val defaultFolder = File(categoryDir, "default")
            if (defaultFolder.exists() && defaultFolder.isDirectory) {
                val list = defaultFolder.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in SUPPORTED_AUDIO_EXTENSIONS }
                    ?.sortedBy { it.name.lowercase() }
                    .orEmpty()
                if (list.isNotEmpty()) return list
            }

            // 4. Single default file `audio/{category}/default.{ext}`
            for (ext in SUPPORTED_AUDIO_EXTENSIONS) {
                val defaultFile = File(categoryDir, "default.$ext")
                if (defaultFile.exists() && defaultFile.isFile) {
                    return listOf(defaultFile)
                }
            }

            return emptyList()
        }
    }
}
