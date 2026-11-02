package id.algaffar.jwstv.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import id.algaffar.jwstv.domain.model.CustomThemeItem
import id.algaffar.jwstv.domain.model.ThemeSettings
import java.time.ZonedDateTime

@Immutable
data class JwsThemePalette(
    val id: String,
    val name: String,
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgCard: Color,
    val accent: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val isLight: Boolean = false
)

val LocalJwsTheme = staticCompositionLocalOf { ThemeEngine.BuiltInThemes.first { it.id == "emerald" } }

/**
 * Manages the 17 built-in themes + custom user themes + 24-hour Auto-Theme rotation.
 * Mirrors `src/types/themes.ts` and `src/contexts/ThemeContext.tsx`.
 */
object ThemeEngine {

    val BuiltInThemes: List<JwsThemePalette> = listOf(
        JwsThemePalette(
            id = "default",
            name = "Default Slate",
            bgPrimary = Color(0xFF0F172A),
            bgSecondary = Color(0xFF1E293B),
            bgCard = Color(0xCC1E293B),
            accent = Color(0xFF38BDF8),
            accentSecondary = Color(0xFFF59E0B),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            border = Color(0x3338BDF8)
        ),
        JwsThemePalette(
            id = "emerald",
            name = "Emerald Al-Gaffar",
            bgPrimary = Color(0xFF022C22),
            bgSecondary = Color(0xFF064E3B),
            bgCard = Color(0xCC064E3B),
            accent = Color(0xFF10B981),
            accentSecondary = Color(0xFFFBBF24),
            textPrimary = Color(0xFFECFDF5),
            textSecondary = Color(0xFFA7F3D0),
            border = Color(0x4010B981)
        ),
        JwsThemePalette(
            id = "midnight",
            name = "Midnight Indigo",
            bgPrimary = Color(0xFF090D16),
            bgSecondary = Color(0xFF1E1B4B),
            bgCard = Color(0xCC1E1B4B),
            accent = Color(0xFF818CF8),
            accentSecondary = Color(0xFF38BDF8),
            textPrimary = Color(0xFFEEF2FF),
            textSecondary = Color(0xFFA5B4FC),
            border = Color(0x40818CF8)
        ),
        JwsThemePalette(
            id = "sunset",
            name = "Warm Sunset",
            bgPrimary = Color(0xFF2A0800),
            bgSecondary = Color(0xFF431407),
            bgCard = Color(0xCC431407),
            accent = Color(0xFFF97316),
            accentSecondary = Color(0xFFFACC15),
            textPrimary = Color(0xFFFFF7ED),
            textSecondary = Color(0xFFFDBA74),
            border = Color(0x40F97316)
        ),
        JwsThemePalette(
            id = "ocean",
            name = "Ocean Breeze",
            bgPrimary = Color(0xFF042F2E),
            bgSecondary = Color(0xFF134E4A),
            bgCard = Color(0xCC134E4A),
            accent = Color(0xFF2DD4BF),
            accentSecondary = Color(0xFF38BDF8),
            textPrimary = Color(0xFFF0FDFA),
            textSecondary = Color(0xFF99F6E4),
            border = Color(0x402DD4BF)
        ),
        JwsThemePalette(
            id = "earth",
            name = "Terracotta Earth",
            bgPrimary = Color(0xFF1C1917),
            bgSecondary = Color(0xFF292524),
            bgCard = Color(0xCC292524),
            accent = Color(0xFFD97706),
            accentSecondary = Color(0xFFA8A29E),
            textPrimary = Color(0xFFFAFAF9),
            textSecondary = Color(0xFFD6D3D1),
            border = Color(0x40D97706)
        ),
        JwsThemePalette(
            id = "luxury",
            name = "Obsidian Luxury",
            bgPrimary = Color(0xFF09090B),
            bgSecondary = Color(0xFF18181B),
            bgCard = Color(0xCC18181B),
            accent = Color(0xFFEAB308),
            accentSecondary = Color(0xFFFDE047),
            textPrimary = Color(0xFFFAFAFA),
            textSecondary = Color(0xFFD4D4D8),
            border = Color(0x4DEAB308)
        ),
        JwsThemePalette(
            id = "minimal",
            name = "Minimal Pearl",
            bgPrimary = Color(0xFFF8FAFC),
            bgSecondary = Color(0xFFE2E8F0),
            bgCard = Color(0xE6FFFFFF),
            accent = Color(0xFF059669),
            accentSecondary = Color(0xFF0284C7),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            border = Color(0x33059669),
            isLight = true
        ),
        JwsThemePalette(
            id = "royal-gold",
            name = "Royal Gold",
            bgPrimary = Color(0xFF1A120B),
            bgSecondary = Color(0xFF3C2A21),
            bgCard = Color(0xCC3C2A21),
            accent = Color(0xFFFFD700),
            accentSecondary = Color(0xFFF59E0B),
            textPrimary = Color(0xFFFFFBEB),
            textSecondary = Color(0xFFFDE68A),
            border = Color(0x4DFFD700)
        ),
        JwsThemePalette(
            id = "deep-ocean",
            name = "Deep Ocean",
            bgPrimary = Color(0xFF020617),
            bgSecondary = Color(0xFF0F172A),
            bgCard = Color(0xCC0F172A),
            accent = Color(0xFF0EA5E9),
            accentSecondary = Color(0xFF22D3EE),
            textPrimary = Color(0xFFF0F9FF),
            textSecondary = Color(0xFF7DD3FC),
            border = Color(0x400EA5E9)
        ),
        JwsThemePalette(
            id = "obsidian-emerald",
            name = "Obsidian Emerald",
            bgPrimary = Color(0xFF050A08),
            bgSecondary = Color(0xFF0D1F18),
            bgCard = Color(0xCC0D1F18),
            accent = Color(0xFF00FF9D),
            accentSecondary = Color(0xFF10B981),
            textPrimary = Color(0xFFECFDF5),
            textSecondary = Color(0xFF6EE7B7),
            border = Color(0x4000FF9D)
        ),
        JwsThemePalette(
            id = "purple-rain",
            name = "Purple Rain",
            bgPrimary = Color(0xFF1E0533),
            bgSecondary = Color(0xFF3B0764),
            bgCard = Color(0xCC3B0764),
            accent = Color(0xFFC084FC),
            accentSecondary = Color(0xFFF472B6),
            textPrimary = Color(0xFFFAF5FF),
            textSecondary = Color(0xFFE9D5FF),
            border = Color(0x40C084FC)
        ),
        JwsThemePalette(
            id = "aurora-night",
            name = "Aurora Night",
            bgPrimary = Color(0xFF041C2C),
            bgSecondary = Color(0xFF0B3142),
            bgCard = Color(0xCC0B3142),
            accent = Color(0xFF4ADE80),
            accentSecondary = Color(0xFF818CF8),
            textPrimary = Color(0xFFF0FDF4),
            textSecondary = Color(0xFFBBF7D0),
            border = Color(0x404ADE80)
        ),
        JwsThemePalette(
            id = "islamic-marble",
            name = "Islamic Marble",
            bgPrimary = Color(0xFFFDFBF7),
            bgSecondary = Color(0xFFF3EFE6),
            bgCard = Color(0xE6FFFFFF),
            accent = Color(0xFF0D9488),
            accentSecondary = Color(0xFFB45309),
            textPrimary = Color(0xFF1C1917),
            textSecondary = Color(0xFF57534E),
            border = Color(0x330D9488),
            isLight = true
        ),
        JwsThemePalette(
            id = "midnight-forest",
            name = "Midnight Forest",
            bgPrimary = Color(0xFF051911),
            bgSecondary = Color(0xFF0A2E1F),
            bgCard = Color(0xCC0A2E1F),
            accent = Color(0xFF84CC16),
            accentSecondary = Color(0xFF10B981),
            textPrimary = Color(0xFFF7FEE7),
            textSecondary = Color(0xFFBEF264),
            border = Color(0x4084CC16)
        ),
        JwsThemePalette(
            id = "royal-desert",
            name = "Royal Desert",
            bgPrimary = Color(0xFF271810),
            bgSecondary = Color(0xFF452B1F),
            bgCard = Color(0xCC452B1F),
            accent = Color(0xFFF59E0B),
            accentSecondary = Color(0xFFD97706),
            textPrimary = Color(0xFFFFFBEB),
            textSecondary = Color(0xFFFDE68A),
            border = Color(0x40F59E0B)
        ),
        JwsThemePalette(
            id = "cyber-silk",
            name = "Cyber Silk",
            bgPrimary = Color(0xFF070B19),
            bgSecondary = Color(0xFF111827),
            bgCard = Color(0xCC111827),
            accent = Color(0xFF06B6D4),
            accentSecondary = Color(0xFFEC4899),
            textPrimary = Color(0xFFECFEFF),
            textSecondary = Color(0xFF67E8F9),
            border = Color(0x4006B6D4)
        )
    )

    fun resolveActivePalette(
        themeSettings: ThemeSettings,
        now: ZonedDateTime
    ): JwsThemePalette {
        val allPalettes = BuiltInThemes + themeSettings.customThemes.map { it.toPalette() }

        if (themeSettings.autoThemeEnabled && allPalettes.isNotEmpty()) {
            // Deterministic 24-hour rotation seeded by day-of-year so themes don't repeat consecutively
            val seed = now.dayOfYear * 31 + now.year
            val index = (now.hour + seed).mod(allPalettes.size)
            return allPalettes[index]
        }

        return allPalettes.firstOrNull { it.id == themeSettings.currentThemeId }
            ?: BuiltInThemes.first { it.id == "emerald" }
    }

    private fun CustomThemeItem.toPalette(): JwsThemePalette {
        val primary = parseHexColor(bgPrimary, Color(0xFF064E3B))
        val secondary = parseHexColor(bgSecondary, Color(0xFF047857))
        val acc = parseHexColor(accent, Color(0xFF10B981))
        val txtPrimary = parseHexColor(textPrimary, Color.White)
        val txtSecondary = parseHexColor(textSecondary, Color(0xFFA7F3D0))

        return JwsThemePalette(
            id = id.ifBlank { "custom-1" },
            name = name.ifBlank { "Tema Kustom" },
            bgPrimary = primary,
            bgSecondary = secondary,
            bgCard = secondary.copy(alpha = 0.82f),
            accent = acc,
            accentSecondary = acc,
            textPrimary = txtPrimary,
            textSecondary = txtSecondary,
            border = acc.copy(alpha = 0.3f)
        )
    }

    fun parseHexColor(hex: String, fallback: Color): Color {
        return try {
            val cleaned = hex.trim().removePrefix("#")
            val longVal = cleaned.toLong(16)
            when (cleaned.length) {
                6 -> Color(0xFF000000L or longVal)
                8 -> Color(longVal)
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}

/**
 * Renders the themed ambient background with subtle aurora radial glow and
 * traditional 8-point Islamic geometric star (`Rub el Hizb`) watermark patterns.
 */
@Composable
fun IslamicAmbientBackground(
    palette: JwsThemePalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.bgPrimary, palette.bgSecondary, palette.bgPrimary),
                    start = Offset.Zero,
                    end = Offset(1920f, 1080f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-left and bottom-right ambient aurora glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.18f),
                    radius = size.width * 0.45f
                ),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.15f, size.height * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accentSecondary.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.82f),
                    radius = size.width * 0.42f
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.85f, size.height * 0.82f)
            )

            // Subtle Rub el Hizb (8-pointed Islamic star) geometric watermarks in corners
            val patternColor = palette.accent.copy(alpha = 0.06f)
            val starCenters = listOf(
                Offset(size.width * 0.92f, size.height * 0.14f),
                Offset(size.width * 0.08f, size.height * 0.86f)
            )
            starCenters.forEach { center ->
                val side = 180f
                val topLeft = Offset(center.x - side / 2f, center.y - side / 2f)
                drawRect(
                    color = patternColor,
                    topLeft = topLeft,
                    size = Size(side, side),
                    style = Stroke(width = 2f)
                )
                withTransform({
                    rotate(degrees = 45f, pivot = center)
                }) {
                    drawRect(
                        color = patternColor,
                        topLeft = topLeft,
                        size = Size(side, side),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        content()
    }
}
