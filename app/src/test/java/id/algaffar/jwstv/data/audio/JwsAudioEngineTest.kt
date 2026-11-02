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
    fun `generateSineWavePcm produces non-zero 1000Hz PCM samples with smooth envelope`() {
        val pcm = BeepGenerator.generateSineWavePcm(frequencyHz = 1000.0, durationMs = 280, sampleRate = 44100)
        assertEquals((44100 * 280) / 1000, pcm.size)
        // First sample is 0 due to attack envelope, middle samples have high amplitude
        assertEquals(0, pcm.first().toInt())
        val maxAmp = pcm.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Expected peak amplitude > 20000, got $maxAmp", maxAmp > 20000)
    }
}
