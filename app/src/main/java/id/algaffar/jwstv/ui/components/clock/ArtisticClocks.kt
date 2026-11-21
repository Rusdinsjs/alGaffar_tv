package id.algaffar.jwstv.ui.components.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
        cornerRadius = 30.dp,
        secBottomPad = 6.dp,
        secBadgePadH = 8.dp,
        secBadgePadV = 2.dp
    )
    ClockVariant.HEADER_CARD -> ClockMetrics(
        mainFontSize = 122.sp,
        secFontSize = 52.sp,
        padH = 12.dp,
        padV = 6.dp,
        spacing = 12.dp,
        cornerRadius = 44.dp,
        secBottomPad = 10.dp,
        secBadgePadH = 12.dp,
        secBadgePadV = 4.dp
    )
    ClockVariant.MAIN -> ClockMetrics(
        mainFontSize = 88.sp,
        secFontSize = 32.sp,
        padH = 32.dp,
        padV = 16.dp,
        spacing = 10.dp,
        cornerRadius = 44.dp,
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
 * with `HEADER`, `HEADER_CARD` (full 480x180 card), and `MAIN` size variants.
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
 * Mirrors `jws-android-algaffar/src/components/Screens/Shared/BigAnalogClock.tsx`
 * across all 10 SVG layers (scaled cleanly from the 500x500 reference coordinate space).
 */
@Composable
fun BigAnalogClock(
    now: ZonedDateTime,
    isSmall: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hours = now.hour % 12
    val minutes = now.minute
    val seconds = now.second

    val secondDeg = seconds * 6f
    val minuteDeg = minutes * 6f + seconds * 0.1f
    val hourDeg = hours * 30f + minutes * 0.5f

    val numberPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFFFFEA79).toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
    }

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
                .fillMaxHeight(if (isSmall) 0.94f else 0.95f)
                .aspectRatio(1f)
        ) {
            val s = size.minDimension / 500f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)

            fun pt(x: Float, y: Float): Offset = Offset(cx + (x - 250f) * s, cy + (y - 250f) * s)

            // Layer 1: Outer Metallic Gold Bezel (5 concentric rings)
            drawCircle(color = Color(0xFFD4AF37), radius = 246f * s, center = center)
            drawCircle(color = Color(0xFF1A150A), radius = 238f * s, center = center)
            drawCircle(color = Color(0xFFB38728), radius = 232f * s, center = center)
            drawCircle(color = Color(0xFFFFD700), radius = 228f * s, center = center)
            drawCircle(color = Color(0xFF14261D), radius = 225f * s, center = center)

            // Layer 2: Dial Face
            drawCircle(color = Color(0xFF041A12), radius = 222f * s, center = center)
            drawCircle(
                color = Color(0xFF0E3F2C),
                radius = 222f * s,
                center = center,
                style = Stroke(width = 2f * s)
            )

            // Layer 3: Inner Arabesque Motif (Subtle Luxury Watermark)
            val arabesqueColor = Color(0xFFFFD700).copy(alpha = 0.12f)
            val arabesqueStroke = Stroke(width = 1.5f * s)
            val squareTopLeft = pt(170f, 170f)
            val squareSize = Size(160f * s, 160f * s)
            val squareCorner = CornerRadius(8f * s, 8f * s)
            drawRoundRect(
                color = arabesqueColor,
                topLeft = squareTopLeft,
                size = squareSize,
                cornerRadius = squareCorner,
                style = arabesqueStroke
            )
            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = arabesqueColor,
                    topLeft = squareTopLeft,
                    size = squareSize,
                    cornerRadius = squareCorner,
                    style = arabesqueStroke
                )
            }
            drawCircle(color = arabesqueColor, radius = 105f * s, center = center, style = arabesqueStroke)
            drawCircle(color = arabesqueColor, radius = 145f * s, center = center, style = arabesqueStroke)

            // Layer 4: 60 Ticks (12 Gold Hour Ticks + 48 Emerald Minute Ticks)
            for (i in 0 until 60) {
                val isHour = i % 5 == 0
                val rad = (i * 6.0) * PI / 180.0
                val r1 = (if (isHour) 194f else 204f) * s
                val r2 = 214f * s
                val x1 = cx + (r1 * sin(rad)).toFloat()
                val y1 = cy - (r1 * cos(rad)).toFloat()
                val x2 = cx + (r2 * sin(rad)).toFloat()
                val y2 = cy - (r2 * cos(rad)).toFloat()
                drawLine(
                    color = if (isHour) Color(0xFFFFD700) else Color(0xFF4E9F76),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = (if (isHour) 4.5f else 1.8f) * s,
                    cap = StrokeCap.Round
                )
            }

            // Layer 5: Numbers (1 - 12)
            numberPaint.textSize = 36f * s
            val fm = numberPaint.fontMetrics
            val baselineShift = -(fm.ascent + fm.descent) / 2f
            for (n in 1..12) {
                val angleDeg = (n % 12) * 30.0
                val rad = angleDeg * PI / 180.0
                val r = 166f * s
                val x = cx + (r * sin(rad)).toFloat()
                val y = cy - (r * cos(rad)).toFloat()
                drawContext.canvas.nativeCanvas.drawText(
                    n.toString(),
                    x,
                    y + baselineShift,
                    numberPaint
                )
            }

            // Layer 6: Golden Mosque Dome Emblem in Upper Dial (centered at 250, 142, scale 0.9)
            val domeCx = cx
            val domeCy = cy + (142f - 250f) * s
            val ds = 0.9f * s
            val domePath = Path().apply {
                moveTo(domeCx, domeCy - 24f * ds)
                cubicTo(
                    domeCx - 9f * ds, domeCy - 17f * ds,
                    domeCx - 15f * ds, domeCy - 6f * ds,
                    domeCx - 15f * ds, domeCy + 7f * ds
                )
                lineTo(domeCx + 15f * ds, domeCy + 7f * ds)
                cubicTo(
                    domeCx + 15f * ds, domeCy - 6f * ds,
                    domeCx + 9f * ds, domeCy - 17f * ds,
                    domeCx, domeCy - 24f * ds
                )
                close()
            }
            drawPath(path = domePath, color = Color(0xFFFFD700).copy(alpha = 0.75f))
            drawCircle(
                color = Color(0xFFFFF2A3),
                radius = 3f * ds,
                center = Offset(domeCx, domeCy - 28f * ds)
            )
        }

        // Moving Hands Layer (only this lightweight Canvas redraws on 1Hz ticks)
        Canvas(
            modifier = Modifier
                .fillMaxHeight(if (isSmall) 0.94f else 0.95f)
                .aspectRatio(1f)
        ) {
            val s = size.minDimension / 500f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)

            fun pt(x: Float, y: Float): Offset = Offset(cx + (x - 250f) * s, cy + (y - 250f) * s)

            // Layer 7: Hour Hand (Gold Sword)
            rotate(degrees = hourDeg, pivot = center) {
                val hourBase = Path().apply {
                    val p0 = pt(243f, 265f)
                    val p1 = pt(257f, 265f)
                    val p2 = pt(254f, 130f)
                    val p3 = pt(250f, 110f)
                    val p4 = pt(246f, 130f)
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                    close()
                }
                drawPath(hourBase, color = Color(0xFFD4AF37))

                val hourFacet = Path().apply {
                    val p0 = pt(250f, 260f)
                    val p1 = pt(254f, 260f)
                    val p2 = pt(252f, 135f)
                    val p3 = pt(250f, 115f)
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(hourFacet, color = Color(0xFFFFF8DC))
            }

            // Layer 8: Minute Hand (Long Gold Sword)
            rotate(degrees = minuteDeg, pivot = center) {
                val minBase = Path().apply {
                    val p0 = pt(244f, 275f)
                    val p1 = pt(256f, 275f)
                    val p2 = pt(253f, 65f)
                    val p3 = pt(250f, 45f)
                    val p4 = pt(247f, 65f)
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    lineTo(p4.x, p4.y)
                    close()
                }
                drawPath(minBase, color = Color(0xFFFFD700))

                val minFacet = Path().apply {
                    val p0 = pt(250f, 270f)
                    val p1 = pt(253f, 270f)
                    val p2 = pt(252f, 70f)
                    val p3 = pt(250f, 50f)
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }
                drawPath(minFacet, color = Color(0xFFFFFFF0))
            }

            // Layer 9: Second Hand (Sport Red Needle)
            rotate(degrees = secondDeg, pivot = center) {
                drawCircle(color = Color(0xFFEF4444), radius = 7f * s, center = pt(250f, 285f))
                drawLine(
                    color = Color(0xFFEF4444),
                    start = pt(250f, 295f),
                    end = pt(250f, 35f),
                    strokeWidth = 3f * s,
                    cap = StrokeCap.Round
                )
                val arrowTip = Path().apply {
                    val p0 = pt(246f, 45f)
                    val p1 = pt(254f, 45f)
                    val p2 = pt(250f, 25f)
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(arrowTip, color = Color(0xFFEF4444))
            }

            // Layer 10: Center Hub Nut
            drawCircle(color = Color(0xFFD4AF37), radius = 14f * s, center = center)
            drawCircle(
                color = Color(0xFFFFF2A3),
                radius = 14f * s,
                center = center,
                style = Stroke(width = 2f * s)
            )
            drawCircle(color = Color(0xFFEF4444), radius = 7f * s, center = center)
        }
    }
}
