package id.algaffar.jwstv.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Zero-latency synthesized 1500Hz sine-wave beep generator (600ms tone + 400ms delay)
 * and 5-second Ramadhan siren sweep (800Hz -> 1500Hz -> 600Hz) using Android [AudioTrack]
 * backed by a single-thread executor and respecting global volume settings.
 */
class BeepGenerator {

    private val sampleRate = DEFAULT_SAMPLE_RATE
    private val beepPcm1500Hz: ShortArray = generateSineWavePcm(
        frequencyHz = BEEP_FREQUENCY_HZ,
        durationMs = BEEP_DURATION_MS,
        sampleRate = sampleRate
    )

    private val audioExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AlGaffarTv-BeepAudio").apply { isDaemon = true }
    }

    @Volatile
    private var lastBeepEpochSecond: Long = -1L

    /**
     * Triggers a single 1500Hz 600ms beep (with 400ms post-beep delay),
     * guarded so it fires at most once per epoch second.
     */
    fun triggerCountdownBeepOncePerSecond(epochSecond: Long, volume: Float = 1.0f) {
        if (epochSecond == lastBeepEpochSecond) return
        lastBeepEpochSecond = epochSecond
        playPcmAsync(beepPcm1500Hz, volume, postDelayMs = BEEP_DELAY_MS)
    }

    /**
     * Plays a single 1500Hz 600ms beep (with 400ms post-beep delay) immediately
     * (e.g. from Debug Menu / Onboarding test button).
     */
    fun playBeepImmediate(volume: Float = 1.0f) {
        playPcmAsync(beepPcm1500Hz, volume, postDelayMs = BEEP_DELAY_MS)
    }

    /**
     * Plays a synthesized 5-second Ramadhan Siren sweep (800Hz -> 1500Hz -> 600Hz)
     * if user hasn't uploaded an external siren MP3 yet.
     */
    fun playSynthesizedSirenSweep(volume: Float = 1.0f) {
        val clampedVol = volume.coerceIn(0f, 1f)
        if (clampedVol <= 0.01f) return
        audioExecutor.execute {
            runCatching {
                val pcm = generateSirenSweepPcm(
                    durationSeconds = SIREN_DURATION_SECONDS,
                    startFreqHz = SIREN_START_FREQ_HZ,
                    peakFreqHz = SIREN_PEAK_FREQ_HZ,
                    endFreqHz = SIREN_END_FREQ_HZ,
                    sampleRate = sampleRate
                )
                playPcmBlocking(pcm, clampedVol)
            }
        }
    }

    private fun playPcmAsync(pcmData: ShortArray, volume: Float, postDelayMs: Long = 0L) {
        val clampedVol = volume.coerceIn(0f, 1f)
        if (clampedVol <= 0.01f) return
        audioExecutor.execute {
            runCatching {
                playPcmBlocking(pcmData, clampedVol, postDelayMs)
            }
        }
    }

    private fun playPcmBlocking(pcmData: ShortArray, volume: Float, postDelayMs: Long = 0L) {
        val taskStartMs = System.currentTimeMillis()
        val toneDurationMs = (pcmData.size * 1000L) / sampleRate
        val byteCount = pcmData.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(byteCount)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.setVolume(volume.coerceIn(0f, 1f))
            track.write(pcmData, 0, pcmData.size)
            track.play()
            // Keep track alive slightly past tone duration so HAL flush does not clip the tail
            Thread.sleep(toneDurationMs + 40L)
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }

        if (postDelayMs > 0L) {
            val targetEndMs = taskStartMs + toneDurationMs + postDelayMs
            val remainingDelayMs = targetEndMs - System.currentTimeMillis()
            if (remainingDelayMs > 0L) {
                Thread.sleep(remainingDelayMs)
            }
        }
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44100
        const val BEEP_FREQUENCY_HZ = 1500.0
        const val BEEP_DURATION_MS = 600
        const val BEEP_DELAY_MS = 400L

        const val SIREN_DURATION_SECONDS = 5
        const val SIREN_START_FREQ_HZ = 800.0
        const val SIREN_PEAK_FREQ_HZ = 1500.0
        const val SIREN_END_FREQ_HZ = 600.0

        fun generateSineWavePcm(
            frequencyHz: Double = BEEP_FREQUENCY_HZ,
            durationMs: Int = BEEP_DURATION_MS,
            sampleRate: Int = DEFAULT_SAMPLE_RATE
        ): ShortArray {
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)
            val fadeSamples = (sampleRate * 15) / 1000 // 15ms smooth attack/release envelope
            for (i in 0 until numSamples) {
                val angle = 2.0 * PI * i * frequencyHz / sampleRate
                val envelope = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples.toDouble()
                    i > numSamples - fadeSamples -> (numSamples - i).toDouble() / fadeSamples.toDouble()
                    else -> 1.0
                }
                samples[i] = (sin(angle) * Short.MAX_VALUE * 0.85 * envelope).toInt().toShort()
            }
            return samples
        }

        fun sirenFrequencyAt(
            progress: Double,
            startFreqHz: Double = SIREN_START_FREQ_HZ,
            peakFreqHz: Double = SIREN_PEAK_FREQ_HZ,
            endFreqHz: Double = SIREN_END_FREQ_HZ
        ): Double {
            val p = progress.coerceIn(0.0, 1.0)
            return if (p < 0.5) {
                startFreqHz + (p * 2.0) * (peakFreqHz - startFreqHz)
            } else {
                peakFreqHz - ((p - 0.5) * 2.0) * (peakFreqHz - endFreqHz)
            }
        }

        fun generateSirenSweepPcm(
            durationSeconds: Int = SIREN_DURATION_SECONDS,
            startFreqHz: Double = SIREN_START_FREQ_HZ,
            peakFreqHz: Double = SIREN_PEAK_FREQ_HZ,
            endFreqHz: Double = SIREN_END_FREQ_HZ,
            sampleRate: Int = DEFAULT_SAMPLE_RATE
        ): ShortArray {
            val totalSamples = sampleRate * durationSeconds
            val pcm = ShortArray(totalSamples)
            var phase = 0.0
            val fadeSamples = minOf(2000, totalSamples / 4).coerceAtLeast(1)
            for (i in 0 until totalSamples) {
                val progress = i.toDouble() / totalSamples.toDouble()
                val freq = sirenFrequencyAt(progress, startFreqHz, peakFreqHz, endFreqHz)
                phase += 2.0 * PI * freq / sampleRate.toDouble()
                val env = when {
                    i < fadeSamples -> i / fadeSamples.toDouble()
                    i > totalSamples - fadeSamples -> (totalSamples - i) / fadeSamples.toDouble()
                    else -> 1.0
                }
                pcm[i] = (sin(phase) * Short.MAX_VALUE * 0.75 * env).toInt().toShort()
            }
            return pcm
        }
    }
}
