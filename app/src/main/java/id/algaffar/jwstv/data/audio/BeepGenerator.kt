package id.algaffar.jwstv.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Zero-latency synthesized 1000Hz sine-wave beep generator using Android [AudioTrack].
 * Used for:
 * - 3x Beep before Adzan (`TARHIM` at 3s, 2s, 1s remaining)
 * - 5x Beep before Iqomah (`MENUNGGU_IQOMAH` at 5s, 4s, 3s, 2s, 1s remaining)
 * - Synthesized dual-tone siren fallback when no custom Ramadhan siren MP3 file is provided.
 * Mirrors `play_beep` in `src-tauri/src/audio.rs`.
 */
class BeepGenerator {

    private val sampleRate = 44100
    private val beepPcm1000Hz: ShortArray = generateSineWavePcm(
        frequencyHz = 1000.0,
        durationMs = 280
    )

    private var lastBeepEpochSecond: Long = -1L

    /**
     * Triggers a single 1000Hz 280ms beep, guarded so it fires at most once per epoch second.
     */
    fun triggerCountdownBeepOncePerSecond(epochSecond: Long) {
        if (epochSecond == lastBeepEpochSecond) return
        lastBeepEpochSecond = epochSecond
        playPcmAsync(beepPcm1000Hz)
    }

    /**
     * Plays a single 1000Hz beep immediately (e.g. from Debug Menu test button).
     */
    fun playBeepImmediate() {
        playPcmAsync(beepPcm1000Hz)
    }

    /**
     * Plays a synthesized Ramadhan Siren sweep if user hasn't uploaded an external siren MP3 yet.
     */
    fun playSynthesizedSirenSweep() {
        Thread {
            runCatching {
                val totalSamples = sampleRate * 3 // 3 seconds sweep
                val pcm = ShortArray(totalSamples)
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples.toDouble()
                    // Sweep 440Hz -> 880Hz -> 440Hz
                    val freq = if (progress < 0.5) {
                        440.0 + (progress * 2.0) * 440.0
                    } else {
                        880.0 - ((progress - 0.5) * 2.0) * 440.0
                    }
                    phase += 2.0 * PI * freq / sampleRate.toDouble()
                    val env = when {
                        i < 2000 -> i / 2000.0
                        i > totalSamples - 2000 -> (totalSamples - i) / 2000.0
                        else -> 1.0
                    }
                    pcm[i] = (sin(phase) * Short.MAX_VALUE * 0.75 * env).toInt().toShort()
                }
                playPcmBlocking(pcm)
            }
        }.start()
    }

    private fun playPcmAsync(pcmData: ShortArray) {
        Thread {
            runCatching {
                playPcmBlocking(pcmData)
            }
        }.start()
    }

    private fun playPcmBlocking(pcmData: ShortArray) {
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
            track.write(pcmData, 0, pcmData.size)
            track.play()
            val durationMs = (pcmData.size * 1000L) / sampleRate + 60L
            Thread.sleep(durationMs)
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    companion object {
        fun generateSineWavePcm(frequencyHz: Double, durationMs: Int, sampleRate: Int = 44100): ShortArray {
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
    }
}
