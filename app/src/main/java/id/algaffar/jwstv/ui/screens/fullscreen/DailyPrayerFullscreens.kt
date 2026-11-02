package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.screens.standby.PrayerTimesCarousel
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.util.Locale
import kotlin.math.sin

/**
 * 1. TARTIL & TARHIM FULLSCREEN (`TartilTarhimFullscreen.tsx`)
 * Features:
 * - Animated audio visualizer bars (`AudioVisualizer`)
 * - Large centerpiece `ArtisticClock` (`brand.fullscreenClockStyle`, `ClockVariant.MAIN`)
 * - Countdown to Tarhim / Countdown to Adzan
 * - Bottom horizontal 4-item `PrayerTimesCarousel`
 */
@Composable
fun TartilTarhimFullscreen(
    snapshot: JwsRuntimeSnapshot,
    isTarhim: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val tick = snapshot.tickResult
    val activePrayer = tick.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)

    val stageSeconds = tick.countdownSeconds.coerceAtLeast(0L)
    val adzanSeconds = (tick.secondsUntilAdzan ?: stageSeconds).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = settings.brand.name,
                    color = palette.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = settings.brand.address,
                    color = palette.textSecondary,
                    fontSize = 15.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.accent.copy(alpha = 0.2f))
                    .border(1.5.dp, palette.accent, RoundedCornerShape(50))
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isTarhim) "SHALAWAT TARHIM MENJELANG ADZAN" else "TARTIL AL-QUR'AN MENJELANG SHOLAT",
                    color = palette.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Center Showcase: Artistic Clock + Visualizer + Dual Countdowns
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ArtisticClock(
                now = snapshot.now,
                style = settings.brand.fullscreenClockStyle,
                variant = ClockVariant.MAIN
            )

            AnimatedAudioVisualizerBars(
                accentColor = if (isTarhim) palette.accentSecondary else palette.accent
            )

            Text(
                text = if (isTarhim) {
                    "Persiapan Menuju Kumandang Adzan $prayerLabel"
                } else {
                    "Menjelang Waktu Sholat $prayerLabel (${snapshot.schedule.formattedFor(activePrayer)})"
                },
                color = palette.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isTarhim) {
                    CountdownMetricPill(
                        label = "MENUJU TARHIM",
                        seconds = stageSeconds,
                        highlightColor = palette.accent
                    )
                }
                CountdownMetricPill(
                    label = "MENUJU ADZAN ${prayerLabel.uppercase()}",
                    seconds = adzanSeconds,
                    highlightColor = palette.accentSecondary
                )
            }
        }

        // Bottom Prayer Times Carousel
        PrayerTimesCarousel(
            schedule = snapshot.schedule,
            scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
            isVertical = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnimatedAudioVisualizerBars(
    accentColor: Color,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VisualizerPhase"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(48.dp)
    ) {
        for (i in 0 until barCount) {
            val wave = ((sin(phase + i * 0.45f) + 1f) / 2f).coerceIn(0.18f, 1f)
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .fillMaxHeight(wave)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.35f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun CountdownMetricPill(
    label: String,
    seconds: Long,
    highlightColor: Color
) {
    val palette = LocalJwsTheme.current
    val mins = seconds / 60L
    val secs = seconds % 60L
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgCard)
            .border(2.dp, highlightColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = highlightColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatted,
            color = palette.textPrimary,
            fontSize = 48.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * 2. ADZAN FULLSCREEN (`AdzanContent.tsx`)
 * Features:
 * - Dynamic sky palette & Mosque Silhouette Horizon per prayer (`Subuh`, `Dzuhur`, `Ashar`, `Maghrib`, `Isya`)
 * - Pulsing Adzan callout badge & Arabic calligraphy
 * - Post-Adzan Du'a / Sunnah reminder
 */
@Composable
fun AdzanFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val activePrayer = snapshot.tickResult.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)
    val remainingSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remainingSec / 60L, remainingSec % 60L)

    val skyColors = when (activePrayer) {
        PrayerId.FAJR -> listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF4C1D95))
        PrayerId.DHUHR -> listOf(Color(0xFF022C22), Color(0xFF065F46), Color(0xFF0D9488))
        PrayerId.ASR -> listOf(Color(0xFF1C1917), Color(0xFF78350F), Color(0xFFD97706))
        PrayerId.MAGHRIB -> listOf(Color(0xFF1E1B4B), Color(0xFF7C2D12), Color(0xFFEA580C))
        else -> listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AdzanPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AdzanPulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(skyColors))
    ) {
        // Mosque Horizon Vector Silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE047).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.38f
                ),
                radius = w * 0.38f,
                center = Offset(w * 0.5f, h * 0.42f)
            )

            val horizonColor = Color(0xFF020617).copy(alpha = 0.88f)
            drawRect(
                color = horizonColor,
                topLeft = Offset(0f, h * 0.82f),
                size = Size(w, h * 0.18f)
            )

            val domePath = Path().apply {
                moveTo(w * 0.32f, h * 0.82f)
                cubicTo(w * 0.32f, h * 0.54f, w * 0.50f, h * 0.42f, w * 0.50f, h * 0.42f)
                cubicTo(w * 0.50f, h * 0.42f, w * 0.68f, h * 0.54f, w * 0.68f, h * 0.82f)
                close()
            }
            drawPath(domePath, color = horizonColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snapshot.settings.brand.name,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                ArtisticClock(
                    now = snapshot.now,
                    style = snapshot.settings.brand.clockStyle,
                    variant = ClockVariant.HEADER
                )
            }

            // Center Adzan Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .alpha(pulseAlpha)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                        .border(2.dp, Color(0xFFFDE047), RoundedCornerShape(50))
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "SAATNYA KUMANDANG ADZAN",
                        color = Color(0xFFFDE047),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "WAKTU SHOLAT ${prayerLabel.uppercase()}",
                    color = Color.White,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Pukul ${snapshot.schedule.formattedFor(activePrayer)} • ${activePrayer.arabicName}",
                    color = Color(0xFFFDE68A),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 36.dp, vertical = 20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "“Barangsiapa ketika mendengar adzan mengucapkan doa setelah adzan, maka ia berhak mendapatkan syafa'atku pada hari kiamat.” (HR. Bukhari)",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Durasi Adzan tersisa: $formattedRem",
                            color = palette.accent,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 3. MENUNGGU IQOMAH FULLSCREEN (`IqomahContent.tsx`)
 * Features:
 * - Massive digital countdown (`MM:SS`)
 * - Linear progress bar
 * - Red Warning Banner when `<= 120 seconds` (`isNoSunnahWarningActive`):
 *   `"Iqomah segera tiba... HARAP TIDAK MEMULAI SHOLAT SUNNAT"`
 */
@Composable
fun IqomahFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val tick = snapshot.tickResult
    val activePrayer = tick.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)
    val stageCfg = snapshot.settings.prayers.forPrayer(activePrayer)

    val remSec = tick.countdownSeconds.coerceAtLeast(0L)
    val totalSec = tick.totalStageSeconds.coerceAtLeast(1L)
    val progress = (remSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

    val mins = remSec / 60L
    val secs = remSec % 60L
    val formattedTimer = String.format(Locale.US, "%02d:%02d", mins, secs)

    val isWarning = tick.isNoSunnahWarningActive

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = snapshot.settings.brand.name,
                    color = palette.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Persiapan Sholat Berjamaah $prayerLabel",
                    color = palette.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            ArtisticClock(
                now = snapshot.now,
                style = snapshot.settings.brand.clockStyle,
                variant = ClockVariant.HEADER
            )
        }

        // Center Massive Countdown
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "HITUNG MUNDUR MENUJU IQOMAH ${prayerLabel.uppercase()}",
                color = palette.textSecondary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (isWarning) Color(0xFF450A0A).copy(alpha = 0.9f)
                        else palette.bgCard
                    )
                    .border(
                        width = 3.dp,
                        color = if (isWarning) Color(0xFFEF4444) else palette.accent,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 72.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedTimer,
                    color = if (isWarning) Color(0xFFFCA5A5) else palette.textPrimary,
                    fontSize = 148.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                color = if (isWarning) Color(0xFFEF4444) else palette.accent,
                trackColor = palette.bgPrimary,
                modifier = Modifier
                    .width(680.dp)
                    .height(14.dp)
                    .clip(CircleShape)
            )

            // Red Warning Banner when <= 120 seconds (`isNoSunnahWarningActive`)
            if (isWarning) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFDC2626))
                        .border(2.dp, Color(0xFFFDE047), RoundedCornerShape(18.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "IQOMAH SEGERA TIBA (KURANG DARI 2 MENIT)",
                            color = Color(0xFFFDE047),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "HARAP TIDAK MEMULAI SHOLAT SUNNAT BARU — PERSIAPKAN BARISAN SHAF",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else if (stageCfg.imam.isNotBlank() || stageCfg.muadzin.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (stageCfg.imam.isNotBlank()) {
                        OfficerChip(role = "Imam Sholat", name = stageCfg.imam)
                    }
                    if (stageCfg.muadzin.isNotBlank()) {
                        OfficerChip(role = "Muadzin", name = stageCfg.muadzin)
                    }
                }
            }
        }

        // Bottom Sunnah Reminder
        Text(
            text = "“Doa di antara adzan dan iqomah tidak tertolak.” (HR. Abu Dawud & Tirmidzi)",
            color = palette.textSecondary,
            fontSize = 20.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun OfficerChip(role: String, name: String) {
    val palette = LocalJwsTheme.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgCard)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = "$role: $name",
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 4. QUIET PRAYER EXECUTION / KHUTBAH OVERLAY (`shared/CctvStateOverlay.tsx`)
 * Used for:
 * - `PELAKSANAAN_SHOLAT`
 * - `KHUTBAH` & `PELAKSANAAN_JUMAT`
 * - `SHOLAT_TARAWIH`
 * - `SHOLAT_IDUL_FITRI` & `KHUTBAH_IDUL_FITRI`
 * - `SHOLAT_IDUL_ADHA` & `KHUTBAH_IDUL_ADHA`
 */
@Composable
fun CctvStateOverlayFullscreen(
    snapshot: JwsRuntimeSnapshot,
    stageTitle: String,
    stageSubtitle: String,
    officerRoleLabel: String,
    officerName: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // Serene Dark Mosque Nightscape + Left Architectural Curved Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle ambient night sky
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.bgPrimary.copy(alpha = 0.95f),
                        Color(0xFF020617),
                        Color(0xFF050B14)
                    )
                )
            )

            // Left curved panel (`CctvStateOverlay` signature curve)
            val leftCurve = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.44f, 0f)
                cubicTo(
                    w * 0.36f, h * 0.35f,
                    w * 0.50f, h * 0.70f,
                    w * 0.38f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = leftCurve,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.bgSecondary.copy(alpha = 0.85f),
                        palette.bgPrimary.copy(alpha = 0.95f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(44.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Mosque Brand on Left, Header Clock on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.width(680.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accent.copy(alpha = 0.2f))
                            .border(1.dp, palette.accent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MODE TENANG IBADAH BERLANGSUNG",
                            color = palette.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stageTitle,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 56.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stageSubtitle,
                        color = palette.textSecondary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                ArtisticClock(
                    now = snapshot.now,
                    style = snapshot.settings.brand.clockStyle,
                    variant = ClockVariant.HEADER
                )
            }

            // Bottom Row: Left Remaining Timer, Right Imam/Khotib Card + "Rapatkan Shaf & Matikan Gadget"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Bottom-Left Remaining Duration Pill
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "ESTIMASI SISA WAKTU",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedRem,
                        color = palette.accent,
                        fontSize = 34.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Bottom-Right Imam/Khotib Card + Shaf & Gadget Reminder
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (officerName.isNotBlank() && officerName != "-") {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.bgCard)
                                .border(1.5.dp, palette.accentSecondary, RoundedCornerShape(18.dp))
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = officerRoleLabel.uppercase(),
                                    color = palette.accentSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = officerName,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF064E3B).copy(alpha = 0.9f))
                            .border(1.5.dp, palette.accent, RoundedCornerShape(18.dp))
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "Luruskan & Rapatkan Shaf  •  Matikan atau Nonaktifkan Dering Gadget",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
