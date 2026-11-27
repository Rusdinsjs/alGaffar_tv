package id.algaffar.jwstv.data.audio

import id.algaffar.jwstv.domain.model.PrayerId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class JwsAudioEngineTest {

    @Test
    fun `resolvePlaylistFiles follows exact 4-level folder and default fallback hierarchy`() {
        val tempRoot = Files.createTempDirectory("jws_audio_test").toFile()
        try {
            // 1. Setup default fallback file: audio/tartil/default.mp3
            val tartilDir = File(tempRoot, "audio/tartil").apply { mkdirs() }
            val defaultFile = File(tartilDir, "default.mp3").apply { writeText("dummy") }

            val resolvedLevel4 = JwsAudioEngine.resolvePlaylistFiles("tartil", PrayerId.FAJR, tempRoot)
            assertEquals(listOf(defaultFile), resolvedLevel4)

            // 2. Setup default folder: audio/tartil/default/01.mp3, 02.mp3
            val defaultFolder = File(tartilDir, "default").apply { mkdirs() }
            val defTrack1 = File(defaultFolder, "01_ar_rahman.mp3").apply { writeText("dummy") }
            val defTrack2 = File(defaultFolder, "02_al_waqiah.mp3").apply { writeText("dummy") }

            val resolvedLevel3 = JwsAudioEngine.resolvePlaylistFiles("tartil", PrayerId.FAJR, tempRoot)
            assertEquals(listOf(defTrack1, defTrack2), resolvedLevel3)

            // 3. Setup single prayer file: audio/tartil/fajr.mp3
            val singleFajr = File(tartilDir, "fajr.mp3").apply { writeText("dummy") }
            val resolvedLevel2 = JwsAudioEngine.resolvePlaylistFiles("tartil", PrayerId.FAJR, tempRoot)
            assertEquals(listOf(singleFajr), resolvedLevel2)

            // 4. Setup prayer-specific folder: audio/tartil/fajr/surah_qaf.mp3
            val fajrFolder = File(tartilDir, "fajr").apply { mkdirs() }
            val fajrTrack = File(fajrFolder, "surah_qaf.mp3").apply { writeText("dummy") }
            val resolvedLevel1 = JwsAudioEngine.resolvePlaylistFiles("tartil", PrayerId.FAJR, tempRoot)
            assertEquals(listOf(fajrTrack), resolvedLevel1)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `resolvePlaylistFiles prioritizes jumat slot over dhuhr on Friday and falls back cleanly`() {
        val tempRoot = Files.createTempDirectory("jws_audio_jumat_test").toFile()
        try {
            val tarhimDir = File(tempRoot, "audio/tarhim").apply { mkdirs() }
            val dhuhrFile = File(tarhimDir, "dhuhr.mp3").apply { writeText("dhuhr") }

            // Before jumat.mp3 exists, Friday falls back to dhuhr.mp3
            assertEquals(
                listOf(dhuhrFile),
                JwsAudioEngine.resolvePlaylistFiles("tarhim", PrayerId.DHUHR, tempRoot, isFridayJumat = true)
            )

            // Once jumat.mp3 exists, Friday uses jumat.mp3 while non-Friday still uses dhuhr.mp3
            val jumatFile = File(tarhimDir, "jumat.mp3").apply { writeText("jumat") }
            assertEquals(
                listOf(jumatFile),
                JwsAudioEngine.resolvePlaylistFiles("tarhim", PrayerId.DHUHR, tempRoot, isFridayJumat = true)
            )
            assertEquals(
                listOf(dhuhrFile),
                JwsAudioEngine.resolvePlaylistFiles("tarhim", PrayerId.DHUHR, tempRoot, isFridayJumat = false)
            )

            // Folder audio/tarhim/jumat/ takes top priority over single jumat.mp3 on Friday
            val jumatFolder = File(tarhimDir, "jumat").apply { mkdirs() }
            val jumatFolderTrack = File(jumatFolder, "01_khusus_jumat.mp3").apply { writeText("jumat_folder") }
            assertEquals(
                listOf(jumatFolderTrack),
                JwsAudioEngine.resolvePlaylistFiles("tarhim", PrayerId.DHUHR, tempRoot, isFridayJumat = true)
            )
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `generateSineWavePcm produces 1500Hz 600ms PCM samples with smooth envelope and 400ms delay config`() {
        assertEquals(1500.0, BeepGenerator.BEEP_FREQUENCY_HZ, 0.001)
        assertEquals(600, BeepGenerator.BEEP_DURATION_MS)
        assertEquals(400L, BeepGenerator.BEEP_DELAY_MS)

        val sampleRate = BeepGenerator.DEFAULT_SAMPLE_RATE
        val pcm = BeepGenerator.generateSineWavePcm(
            frequencyHz = BeepGenerator.BEEP_FREQUENCY_HZ,
            durationMs = BeepGenerator.BEEP_DURATION_MS,
            sampleRate = sampleRate
        )
        assertEquals((sampleRate * 600) / 1000, pcm.size)
        // First sample is 0 due to attack envelope, middle samples have high amplitude
        assertEquals(0, pcm.first().toInt())
        val maxAmp = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Expected peak amplitude > 20000, got $maxAmp", maxAmp > 20000)

        // Verify measured frequency in middle steady-state window via positive zero-crossings (~1500Hz)
        val measuredHz = estimateFrequencyHz(pcm, startIdx = 4410, endIdx = 22050, sampleRate = sampleRate)
        assertEquals(1500.0, measuredHz, 15.0)
    }

    @Test
    fun `generateSirenSweepPcm produces 5s sweep from 800Hz to 1500Hz and down to 600Hz`() {
        assertEquals(5, BeepGenerator.SIREN_DURATION_SECONDS)
        assertEquals(800.0, BeepGenerator.sirenFrequencyAt(0.0), 0.001)
        assertEquals(1500.0, BeepGenerator.sirenFrequencyAt(0.5), 0.001)
        assertEquals(600.0, BeepGenerator.sirenFrequencyAt(1.0), 0.001)

        val sampleRate = BeepGenerator.DEFAULT_SAMPLE_RATE
        val pcm = BeepGenerator.generateSirenSweepPcm()
        assertEquals(sampleRate * 5, pcm.size)
        assertEquals(0, pcm.first().toInt())
        val maxAmp = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Expected peak amplitude > 20000, got $maxAmp", maxAmp > 20000)

        // Verify actual synthesized waveform frequency rises 800 -> 1500 Hz then drops to 600 Hz
        val earlyHz = estimateFrequencyHz(pcm, startIdx = 2500, endIdx = 6910, sampleRate = sampleRate)
        val midHz = estimateFrequencyHz(pcm, startIdx = (pcm.size / 2) - 2205, endIdx = (pcm.size / 2) + 2205, sampleRate = sampleRate)
        val lateHz = estimateFrequencyHz(pcm, startIdx = pcm.size - 6910, endIdx = pcm.size - 2500, sampleRate = sampleRate)

        assertTrue("Expected early sweep freq in 800..900Hz, got $earlyHz", earlyHz in 790.0..920.0)
        assertTrue("Expected peak sweep freq near 1500Hz, got $midHz", midHz in 1430.0..1510.0)
        assertTrue("Expected late sweep freq in 600..700Hz, got $lateHz", lateHz in 590.0..710.0)
    }

    private fun estimateFrequencyHz(pcm: ShortArray, startIdx: Int, endIdx: Int, sampleRate: Int): Double {
        var positiveCrossings = 0
        for (i in (startIdx + 1) until endIdx) {
            if (pcm[i - 1] <= 0 && pcm[i] > 0) {
                positiveCrossings++
            }
        }
        val windowSec = (endIdx - startIdx).toDouble() / sampleRate.toDouble()
        return positiveCrossings / windowSec
    }
}
