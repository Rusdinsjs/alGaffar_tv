package id.algaffar.jwstv.ui.settings

import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.MediaDisplaySettings
import id.algaffar.jwstv.domain.model.SlideGlobalConfig
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideTextStyle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsEnhancementsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Test
    fun `VirtualKeyboardPolicy auto mode disables virtual keyboard when physical keyboard is present`() {
        val withPcKeyboard = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_AUTO,
            hasPhysicalKeyboard = true,
            detectedKeyboardNames = listOf("USB QWERTY Keyboard")
        )
        assertFalse(withPcKeyboard.allowVirtualKeyboard)

        val remoteOrTouchOnly = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_AUTO,
            hasPhysicalKeyboard = false
        )
        assertTrue(remoteOrTouchOnly.allowVirtualKeyboard)
    }

    @Test
    fun `VirtualKeyboardPolicy manual modes override physical keyboard detection`() {
        val forcedEnabled = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_ENABLED,
            hasPhysicalKeyboard = true
        )
        assertTrue(forcedEnabled.allowVirtualKeyboard)

        val forcedDisabled = VirtualKeyboardPolicy(
            mode = VirtualKeyboardPolicy.MODE_DISABLED,
            hasPhysicalKeyboard = false
        )
        assertFalse(forcedDisabled.allowVirtualKeyboard)

        val unknownFallbackToAuto = VirtualKeyboardPolicy(
            mode = "unexpected_value",
            hasPhysicalKeyboard = true
        )
        assertEquals(VirtualKeyboardPolicy.MODE_AUTO, unknownFallbackToAuto.normalizedMode)
        assertFalse(unknownFallbackToAuto.allowVirtualKeyboard)
    }

    @Test
    fun `MediaStorageManager protects all Khutbah, Ceramah, Sholat, and Tartil-Tarhim media fields from orphan cleanup`() {
        val settings = AppStateSettings(
            media = MediaDisplaySettings(
                tartilVideo = "jws-assets/videos/tartil_recitation.mp4",
                tarhimVideo = "jws-assets/videos/tarhim_recitation.mp4",
                tartilImages = listOf("jws-assets/images/tartil/01.jpg"),
                tarhimImages = listOf("jws-assets/images/tarhim/01.jpg"),
                doaAdzanImage = "jws-assets/images/media/doa_adzan.jpg",
                khutbahJumatImage = "jws-assets/images/media/khutbah_jumat.jpg",
                khutbahIdulFitriImage = "jws-assets/images/media/khutbah_idul_fitri.jpg",
                khutbahIdulAdhaImage = "jws-assets/images/media/khutbah_idul_adha.jpg",
                ceramahTarawihImage = "jws-assets/images/media/ceramah_tarawih.jpg",
                sholat5WaktuImage = "jws-assets/images/media/sholat_5waktu.jpg",
                sholatJumatImage = "jws-assets/images/media/sholat_jumat.jpg",
                sholatIdulFitriImage = "jws-assets/images/media/sholat_idul_fitri.jpg",
                sholatIdulAdhaImage = "jws-assets/images/media/sholat_idul_adha.jpg",
                sholatTarawihWitirImage = "jws-assets/images/media/sholat_tarawih.jpg"
            )
        )

        val refs = MediaStorageManager.collectReferencedMediaPaths(settings)
        assertTrue("videos/tartil_recitation.mp4" in refs)
        assertTrue("videos/tarhim_recitation.mp4" in refs)
        assertTrue("images/tartil/01.jpg" in refs)
        assertTrue("images/tarhim/01.jpg" in refs)
        assertTrue("images/media/doa_adzan.jpg" in refs)
        assertTrue("images/media/khutbah_jumat.jpg" in refs)
        assertTrue("images/media/khutbah_idul_fitri.jpg" in refs)
        assertTrue("images/media/khutbah_idul_adha.jpg" in refs)
        assertTrue("images/media/ceramah_tarawih.jpg" in refs)
        assertTrue("images/media/sholat_5waktu.jpg" in refs)
        assertTrue("images/media/sholat_jumat.jpg" in refs)
        assertTrue("images/media/sholat_idul_fitri.jpg" in refs)
        assertTrue("images/media/sholat_idul_adha.jpg" in refs)
        assertTrue("images/media/sholat_tarawih.jpg" in refs)
    }

    @Test
    fun `AppStateSettings round-trips new SlideGlobalConfig, SlideTextStyle, virtualKeyboardMode and keeps legacy JSON compatibility`() {
        val original = AppStateSettings(
            virtualKeyboardMode = VirtualKeyboardPolicy.MODE_DISABLED,
            slideConfig = SlideGlobalConfig(
                autoPlay = false,
                defaultDuration = 20,
                maxVideoResolution = "720p"
            ),
            slides = listOf(
                SlideItem(
                    id = "s-text-1",
                    title = "Kajian Rutin",
                    content = "Ba'da Maghrib",
                    type = "text",
                    textStyle = SlideTextStyle(
                        bold = true,
                        italic = true,
                        fontSize = "5xl",
                        fontColor = "#FDE047",
                        textAlign = "left"
                    )
                ),
                SlideItem(
                    id = "s-sys-1",
                    title = "Petugas Jum'at",
                    type = "jumat_petugas",
                    showInJumat = true
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<AppStateSettings>(encoded)
        assertEquals(VirtualKeyboardPolicy.MODE_DISABLED, decoded.virtualKeyboardMode)
        assertFalse(decoded.slideConfig.autoPlay)
        assertEquals(20, decoded.slideConfig.defaultDuration)
        assertEquals("720p", decoded.slideConfig.maxVideoResolution)
        assertEquals(2, decoded.slides.size)
        assertTrue(decoded.slides[0].textStyle.bold)
        assertEquals("#FDE047", decoded.slides[0].textStyle.fontColor)
        assertEquals("jumat_petugas", decoded.slides[1].type)

        // Legacy minimal JSON without new fields must still parse cleanly with safe defaults
        val legacyParsed = json.decodeFromString<AppStateSettings>("""{"schemaVersion":3}""")
        assertEquals(VirtualKeyboardPolicy.MODE_AUTO, legacyParsed.virtualKeyboardMode)
        assertTrue(legacyParsed.slideConfig.autoPlay)
        assertEquals("1080p", legacyParsed.slideConfig.maxVideoResolution)
    }
}
