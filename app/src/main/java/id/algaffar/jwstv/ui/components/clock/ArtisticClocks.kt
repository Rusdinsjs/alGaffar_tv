package id.algaffar.jwstv.ui.components.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ClockVariant {
    HEADER,
    MAIN
}

/**
 * 5 Artistic Clock styles (`classic`, `golden`, `glass`, `wood`, `neon`)
 * with `HEADER` and `MAIN` size variants.
 * Mirrors `src/components/Utils/ArtisticClocks.tsx`.
 */
@Composable
fun ArtisticClock(
    now: ZonedDateTime,
    style: String,
    variant: ClockVariant = ClockVariant.HEADER,
    modifier: Modifier = Modifier
) {
    val hmFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val secFormatter = remember { DateTimeFormatter.ofPattern("ss") }

    val hmText = now.format(hmFormatter)
    val secText = now.format(secFormatter)

    when (style.lowercase().trim()) {
        "golden" -> GoldenArtisticClock(hmText, secText, variant, modifier)
        "glass" -> GlassArtisticClock(hmText, secText, variant, modifier)
        "wood" -> WoodArtisticClock(hmText, secText, variant, modifier)
        "neon" -> NeonArtisticClock(hmText, secText, variant, modifier)
        else -> ClassicArtisticClock(hmText, secText, variant, modifier)
    }
}

@Composable
private fun ClassicArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val mainFontSize = if (variant == ClockVariant.MAIN) 86.sp else 48.sp
    val secFontSize = if (variant == ClockVariant.MAIN) 32.sp else 20.sp
    val padH = if (variant == ClockVariant.MAIN) 32.dp else 20.dp
    val padV = if (variant == ClockVariant.MAIN) 16.dp else 8.dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.bgPrimary.copy(alpha = 0.75f))
            .border(1.5.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hmText,
            color = palette.textPrimary,
            fontSize = mainFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .padding(bottom = if (variant == ClockVariant.MAIN) 12.dp else 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.accent.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = secText,
                color = palette.accent,
                fontSize = secFontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoldenArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val mainFontSize = if (variant == ClockVariant.MAIN) 88.sp else 48.sp
    val secFontSize = if (variant == ClockVariant.MAIN) 32.sp else 20.sp
    val padH = if (variant == ClockVariant.MAIN) 34.dp else 22.dp
    val padV = if (variant == ClockVariant.MAIN) 16.dp else 8.dp

    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFDE047), Color(0xFFD97706), Color(0xFFFEF08A))
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C1408), Color(0xFF2E1F0B))
                )
            )
            .border(2.5.dp, goldBrush, RoundedCornerShape(18.dp))
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hmText,
            color = Color(0xFFFEF08A),
            fontSize = mainFontSize,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .padding(bottom = if (variant == ClockVariant.MAIN) 12.dp else 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFD97706).copy(alpha = 0.35f))
                .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = secText,
                color = Color(0xFFFDE047),
                fontSize = secFontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GlassArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val mainFontSize = if (variant == ClockVariant.MAIN) 86.sp else 48.sp
    val secFontSize = if (variant == ClockVariant.MAIN) 32.sp else 20.sp
    val padH = if (variant == ClockVariant.MAIN) 32.dp else 20.dp
    val padV = if (variant == ClockVariant.MAIN) 16.dp else 8.dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hmText,
            color = Color.White,
            fontSize = mainFontSize,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = secText,
            color = palette.accent,
            fontSize = secFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = if (variant == ClockVariant.MAIN) 12.dp else 6.dp)
        )
    }
}

@Composable
private fun WoodArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val mainFontSize = if (variant == ClockVariant.MAIN) 86.sp else 48.sp
    val secFontSize = if (variant == ClockVariant.MAIN) 32.sp else 20.sp
    val padH = if (variant == ClockVariant.MAIN) 32.dp else 20.dp
    val padV = if (variant == ClockVariant.MAIN) 16.dp else 8.dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF3E2723))
                )
            )
            .border(3.dp, Color(0xFF8D6E63), RoundedCornerShape(16.dp))
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hmText,
            color = Color(0xFFFFF8E1),
            fontSize = mainFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = secText,
            color = Color(0xFFFFCC80),
            fontSize = secFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = if (variant == ClockVariant.MAIN) 12.dp else 6.dp)
        )
    }
}

@Composable
private fun NeonArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val neonColor = palette.accent
    val mainFontSize = if (variant == ClockVariant.MAIN) 86.sp else 48.sp
    val secFontSize = if (variant == ClockVariant.MAIN) 32.sp else 20.sp
    val padH = if (variant == ClockVariant.MAIN) 32.dp else 20.dp
    val padV = if (variant == ClockVariant.MAIN) 16.dp else 8.dp

    Row(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = neonColor.copy(alpha = 0.25f),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF050814).copy(alpha = 0.9f))
            .border(2.dp, neonColor, RoundedCornerShape(18.dp))
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = hmText,
            color = neonColor,
            fontSize = mainFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = secText,
            color = Color.White,
            fontSize = secFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = if (variant == ClockVariant.MAIN) 12.dp else 6.dp)
        )
    }
}
