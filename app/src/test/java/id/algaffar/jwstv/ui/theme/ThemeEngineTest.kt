package id.algaffar.jwstv.ui.theme

import id.algaffar.jwstv.domain.model.CustomThemeItem
import id.algaffar.jwstv.domain.model.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ThemeEngineTest {

    private val makassarZone = ZoneId.of("Asia/Makassar")

    @Test
    fun `all 17 built-in themes are registered and accessible by ID`() {
        val expectedIds = listOf(
            "default",
            "emerald",
            "midnight",
            "sunset",
            "ocean",
            "earth",
            "luxury",
            "minimal",
            "royal-gold",
            "deep-ocean",
            "obsidian-emerald",
            "purple-rain",
            "aurora-night",
            "islamic-marble",
            "midnight-forest",
            "royal-desert",
            "cyber-silk"
        )
        assertEquals(17, ThemeEngine.BuiltInThemes.size)
        val actualIds = ThemeEngine.BuiltInThemes.map { it.id }
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun `resolveActivePalette resolves custom themes and supports 24-hour auto-theme rotation`() {
        val customTheme = CustomThemeItem(
            id = "custom-masjid",
            name = "Hijau Kubah",
            bgPrimary = "#012A1F",
            bgSecondary = "#034732",
            accent = "#00FFAA",
            textPrimary = "#FFFFFF",
            textSecondary = "#B2F5EA"
        )
        val settingsWithCustom = ThemeSettings(
            currentThemeId = "custom-masjid",
            autoThemeEnabled = false,
            customThemes = listOf(customTheme)
        )
        val now = ZonedDateTime.of(2026, 3, 10, 10, 0, 0, 0, makassarZone)
        val resolved = ThemeEngine.resolveActivePalette(settingsWithCustom, now)
        assertEquals("custom-masjid", resolved.id)
        assertEquals("Hijau Kubah", resolved.name)

        // Verify auto-theme rotates across consecutive hours
        val autoSettings = ThemeSettings(autoThemeEnabled = true)
        val paletteHour10 = ThemeEngine.resolveActivePalette(autoSettings, now)
        val paletteHour11 = ThemeEngine.resolveActivePalette(autoSettings, now.plusHours(1))
        assertNotEquals(paletteHour10.id, paletteHour11.id)
        assertTrue(ThemeEngine.BuiltInThemes.any { it.id == paletteHour10.id })
    }
}
