package id.algaffar.jwstv.ui.components.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ClockVariant {
    HEADER,
    HEADER_CARD,
    MAIN
}

private data class ClockMetrics(
    val mainFontSize: TextUnit,
    val secFontSize: TextUnit,
    val padH: Dp,
    val padV: Dp,
    val spacing: Dp,
    val cornerRadius: Dp,
    val secBottomPad: Dp,
    val secBadgePadH: Dp,
    val secBadgePadV: Dp
)

private fun clockMetricsFor(variant: ClockVariant): ClockMetrics = when (variant) {
    ClockVariant.HEADER -> ClockMetrics(
        mainFontSize = 48.sp,
        secFontSize = 20.sp,
        padH = 20.dp,
        padV = 8.dp,
        spacing = 8.dp,
        cornerRadius = 16.dp,
        secBottomPad = 6.dp,
        secBadgePadH = 8.dp,
        secBadgePadV = 2.dp
    )
    ClockVariant.HEADER_CARD -> ClockMetrics(
        mainFontSize = 94.sp,
        secFontSize = 42.sp,
        padH = 10.dp,
        padV = 4.dp,
        spacing = 12.dp,
        cornerRadius = 18.dp,
        secBottomPad = 6.dp,
        secBadgePadH = 10.dp,
        secBadgePadV = 3.dp
    )
    ClockVariant.MAIN -> ClockMetrics(
        mainFontSize = 88.sp,
        secFontSize = 32.sp,
        padH = 32.dp,
        padV = 16.dp,
        spacing = 10.dp,
        cornerRadius = 20.dp,
        secBottomPad = 12.dp,
        secBadgePadH = 10.dp,
        secBadgePadV = 4.dp
    )
}

private fun tightClockTextStyle(tabular: Boolean = true): TextStyle = TextStyle(
    fontFeatureSettings = if (tabular) "tnum" else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * 8 Artistic Clock styles (`classic`, `golden`, `glass`, `wood`, `neon`, `segmented`, `emerald`, `monochrome`)
 * with `HEADER`, `HEADER_CARD` (full 480x120 card), and `MAIN` size variants.
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
        "segmented" -> SegmentedArtisticClock(hmText, secText, variant, modifier)
        "emerald" -> EmeraldArtisticClock(hmText, secText, variant, modifier)
        "monochrome" -> MonochromeArtisticClock(hmText, secText, variant, modifier)
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
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = palette.textPrimary,
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Box(
                modifier = Modifier
                    .padding(bottom = m.secBottomPad)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accent.copy(alpha = 0.22f))
                    .padding(horizontal = m.secBadgePadH, vertical = m.secBadgePadV)
            ) {
                Text(
                    text = secText,
                    color = palette.accent,
                    fontSize = m.secFontSize,
                    lineHeight = m.secFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    style = tightClockTextStyle()
                )
            }
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
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)
    val goldBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFDE047), Color(0xFFD97706), Color(0xFFFEF08A))
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C1408), Color(0xFF2E1F0B))
                )
            )
            .border(2.5.dp, goldBrush, shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = Color(0xFFFEF08A),
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Box(
                modifier = Modifier
                    .padding(bottom = m.secBottomPad)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD97706).copy(alpha = 0.35f))
                    .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(10.dp))
                    .padding(horizontal = m.secBadgePadH, vertical = m.secBadgePadV)
            ) {
                Text(
                    text = secText,
                    color = Color(0xFFFDE047),
                    fontSize = m.secFontSize,
                    lineHeight = m.secFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = tightClockTextStyle()
                )
            }
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
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
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
                shape = shape
            )
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = Color.White,
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Text(
                text = secText,
                color = palette.accent,
                fontSize = m.secFontSize,
                lineHeight = m.secFontSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle(),
                modifier = Modifier.padding(bottom = m.secBottomPad)
            )
        }
    }
}

@Composable
private fun WoodArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFF3E2723))
                )
            )
            .border(2.5.dp, Color(0xFF8D6E63), shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = Color(0xFFFFF8E1),
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Text(
                text = secText,
                color = Color(0xFFFFCC80),
                fontSize = m.secFontSize,
                lineHeight = m.secFontSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle(),
                modifier = Modifier.padding(bottom = m.secBottomPad)
            )
        }
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
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = neonColor.copy(alpha = 0.25f),
                    cornerRadius = CornerRadius(m.cornerRadius.toPx()),
                    style = Stroke(width = 5.dp.toPx())
                )
            }
            .clip(shape)
            .background(Color(0xFF050814).copy(alpha = 0.92f))
            .border(2.dp, neonColor, shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = neonColor,
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Text(
                text = secText,
                color = Color.White,
                fontSize = m.secFontSize,
                lineHeight = m.secFontSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle(),
                modifier = Modifier.padding(bottom = m.secBottomPad)
            )
        }
    }
}

/**
 * 6. Retro Seven-Segment LED Clock (`segmented` in `ArtisticClocks.tsx`)
 */
@Composable
private fun SegmentedArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)
    val ledRed = Color(0xFFFF2E2E)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0B0C10))
            .border(2.5.dp, Color(0xFF1F293D), shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = ledRed,
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Box(
                modifier = Modifier
                    .padding(bottom = m.secBottomPad)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E0707))
                    .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = m.secBadgePadH, vertical = m.secBadgePadV)
            ) {
                Text(
                    text = secText,
                    color = Color(0xFFFF4D4D),
                    fontSize = m.secFontSize,
                    lineHeight = m.secFontSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    style = tightClockTextStyle()
                )
            }
        }
    }
}

/**
 * 7. Emerald Nabawi Clock (`emerald` in `ArtisticClocks.tsx`)
 */
@Composable
private fun EmeraldArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF022C22), Color(0xFF064E3B), Color(0xFF022C22))
                )
            )
            .border(2.dp, Color(0xFF10B981).copy(alpha = 0.65f), shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = Color(0xFFECFDF5),
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Box(
                modifier = Modifier
                    .padding(bottom = m.secBottomPad)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF022C22))
                    .border(1.dp, Color(0xFF34D399).copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = m.secBadgePadH, vertical = m.secBadgePadV)
            ) {
                Text(
                    text = secText,
                    color = Color(0xFF6EE7B7),
                    fontSize = m.secFontSize,
                    lineHeight = m.secFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    style = tightClockTextStyle()
                )
            }
        }
    }
}

/**
 * 8. OLED Ultra Contrast Monochrome Clock (`monochrome` in `ArtisticClocks.tsx`)
 */
@Composable
private fun MonochromeArtisticClock(
    hmText: String,
    secText: String,
    variant: ClockVariant,
    modifier: Modifier = Modifier
) {
    val m = clockMetricsFor(variant)
    val shape = RoundedCornerShape(m.cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black)
            .border(2.dp, Color(0xFF525252), shape)
            .padding(horizontal = m.padH, vertical = m.padV),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(m.spacing)
        ) {
            Text(
                text = hmText,
                color = Color.White,
                fontSize = m.mainFontSize,
                lineHeight = m.mainFontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                style = tightClockTextStyle()
            )
            Box(
                modifier = Modifier
                    .padding(bottom = m.secBottomPad)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF59E0B))
                    .padding(horizontal = m.secBadgePadH, vertical = m.secBadgePadV)
            ) {
                Text(
                    text = secText,
                    color = Color.Black,
                    fontSize = m.secFontSize,
                    lineHeight = m.secFontSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    style = tightClockTextStyle()
                )
            }
        }
    }
}

/**
 * Luxury Gold-Bezel Big Analog Clock for Slide (`analog_clock`) & Standby Fullscreen.
 * Mirrors `src/components/Screens/Shared/BigAnalogClock.tsx`.
 */
@Composable
fun BigAnalogClock(
    now: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val hours = now.hour % 12
    val minutes = now.minute
    val seconds = now.second

    val secondDeg = seconds * 6f
    val minuteDeg = minutes * 6f + seconds * 0.1f
    val hourDeg = hours * 30f + minutes * 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF05261C), Color(0xFF031710), Color(0xFF010A07))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(560.dp)
                .padding(16.dp)
        ) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = Color(0xFFD4AF37), radius = radius, center = center)
            drawCircle(color = Color(0xFF1A150A), radius = radius * 0.96f, center = center)
            drawCircle(color = Color(0xFFB38728), radius = radius * 0.93f, center = center)
            drawCircle(color = Color(0xFFFFD700), radius = radius * 0.91f, center = center)
            drawCircle(color = Color(0xFF041A12), radius = radius * 0.89f, center = center)

            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.14f),
                radius = radius * 0.60f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.12f),
                radius = radius * 0.42f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            for (i in 0 until 60) {
                val isHour = i % 5 == 0
                val rad = (i * 6.0 - 90.0) * PI / 180.0
                val innerR = radius * (if (isHour) 0.74f else 0.80f)
                val outerR = radius * 0.85f
                val start = Offset(
                    x = center.x + (innerR * cos(rad)).toFloat(),
                    y = center.y + (innerR * sin(rad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (outerR * cos(rad)).toFloat(),
                    y = center.y + (outerR * sin(rad)).toFloat()
                )
                drawLine(
                    color = if (isHour) Color(0xFFFFD700) else Color(0xFF4E9F76),
                    start = start,
                    end = end,
                    strokeWidth = if (isHour) 5.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val hourRad = (hourDeg - 90f) * PI / 180.0
            drawLine(
                color = Color(0xFFD4AF37),
                start = center,
                end = Offset(
                    x = center.x + (radius * 0.48f * cos(hourRad)).toFloat(),
                    y = center.y + (radius * 0.48f * sin(hourRad)).toFloat()
                ),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
            )

            val minRad = (minuteDeg - 90f) * PI / 180.0
            drawLine(
                color = Color(0xFFFFF8DC),
                start = center,
                end = Offset(
                    x = center.x + (radius * 0.68f * cos(minRad)).toFloat(),
                    y = center.y + (radius * 0.68f * sin(minRad)).toFloat()
                ),
                strokeWidth = 7.dp.toPx(),
                cap = StrokeCap.Round
            )

            val secRad = (secondDeg - 90f) * PI / 180.0
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(
                    x = center.x - (radius * 0.14f * cos(secRad)).toFloat(),
                    y = center.y - (radius * 0.14f * sin(secRad)).toFloat()
                ),
                end = Offset(
                    x = center.x + (radius * 0.78f * cos(secRad)).toFloat(),
                    y = center.y + (radius * 0.78f * sin(secRad)).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(color = Color(0xFFD4AF37), radius = 14.dp.toPx(), center = center)
            drawCircle(color = Color(0xFFEF4444), radius = 7.dp.toPx(), center = center)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.7f), RoundedCornerShape(50))
                .padding(horizontal = 28.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                color = Color(0xFFFFEA79),
                fontSize = 26.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}
