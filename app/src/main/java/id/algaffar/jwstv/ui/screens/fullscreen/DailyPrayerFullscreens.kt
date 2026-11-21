package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.screens.standby.HeaderClockCard
import id.algaffar.jwstv.ui.screens.standby.LocalSlideVideoPlayer
import id.algaffar.jwstv.ui.screens.standby.MosqueLogoBadge
import id.algaffar.jwstv.ui.screens.standby.PrayerTimesCarousel
import id.algaffar.jwstv.ui.settings.ResolvedMediaPreview
import id.algaffar.jwstv.ui.settings.resolveMediaPreview
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1. TARTIL & TARHIM FULLSCREEN (`TartilTarhimFullscreen.tsx`)
 * Features:
 * - Full-bleed background (multi-slide images with Ken Burns + crossfade, or video tilawah, or dark gradient)
 * - Atmospheric floating particles, Tarhim amber glowing orb, and top/bottom gradient vignettes
 * - Top-Left: Mosque Brand (clickable Logo + Name + Address)
 * - Top-Right: Active Clock Card (`HeaderClockCard` using checked `settings.brand.clockStyle`)
 * - Open center showcase for background media
 * - Row above carousel (mirrored between Tartil & Tarhim):
 *   - Status card (`AudioVisualizer` + `"Tartilul Qur'an"` / `"Tarhim"` + `"Menuju <Prayer>"`)
 *   - Opposite side: `CompactNextPrayerCard` (`MENUJU <PRAYER>` + countdown `MM:SS`)
 * - Bottom horizontal 4-item `PrayerTimesCarousel`
 * - QR Card is strictly hidden during Tartil & Tarhim
 */
@Composable
fun TartilTarhimFullscreen(
    snapshot: JwsRuntimeSnapshot,
    isTarhim: Boolean,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val settings = snapshot.settings
    val tick = snapshot.tickResult
    val activePrayer = tick.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)

    val stageSeconds = tick.countdownSeconds.coerceAtLeast(0L)
    val adzanSeconds = (tick.secondsUntilAdzan ?: stageSeconds).coerceAtLeast(0L)
    val isTarhimEnabledForPrayer = settings.prayers.forPrayer(activePrayer).isTarhim

    val media = settings.media
    val mediaType = if (isTarhim) media.tarhimMediaType else media.tartilMediaType
    val slideImages = if (isTarhim) media.tarhimImages else media.tartilImages
    val slideIntervalSec = (if (isTarhim) media.tarhimSlideInterval else media.tartilSlideInterval).coerceIn(3, 120)
    val videoPath = if (isTarhim) media.tarhimVideo else media.tartilVideo

    var bgSlideIdx by remember(isTarhim, slideImages.size) { mutableIntStateOf(0) }
    LaunchedEffect(isTarhim, slideImages.size, slideIntervalSec) {
        if (slideImages.size <= 1) return@LaunchedEffect
        while (true) {
            delay(slideIntervalSec * 1000L)
            bgSlideIdx = (bgSlideIdx + 1) % slideImages.size
        }
    }

    val activeImagePath = slideImages.getOrNull(bgSlideIdx % slideImages.size.coerceAtLeast(1))
    val resolvedVideoUri = remember(videoPath, assetsRootDir, mediaType) {
        if (!mediaType.equals("video", ignoreCase = true) || videoPath.isBlank()) {
            null
        } else if (videoPath.startsWith("http://") || videoPath.startsWith("https://") || videoPath.startsWith("file://") || videoPath.startsWith("content://")) {
            android.net.Uri.parse(videoPath)
        } else {
            val clean = videoPath.replace('\\', '/').removePrefix("jws-assets/").removePrefix("/")
            val file = assetsRootDir?.let { File(it, clean).takeIf { f -> f.exists() } }
                ?: File(videoPath).takeIf { it.exists() }
            file?.let { android.net.Uri.fromFile(it) }
        }
    }

    // Subtle Ken Burns zoom on background slide images
    val kenBurnsScale = if (isPerformanceMode) {
        1.0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "TartilKenBurns")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "KenBurnsScale"
        )
        scale
    }

    val tarhimAmber = Color(0xFFFBBF24)
    val particleColor = if (isTarhim) tarhimAmber else palette.accent

    Box(modifier = modifier.fillMaxSize()) {
        // ─── 1. Fullscreen Background Layer (Video / Multi-Slide Image / Fallback Gradient) ───
        if (resolvedVideoUri != null) {
            LocalSlideVideoPlayer(
                videoUri = resolvedVideoUri,
                modifier = Modifier.fillMaxSize()
            )
        } else if (activeImagePath != null) {
            Crossfade(
                targetState = activeImagePath,
                animationSpec = tween(durationMillis = if (isPerformanceMode) 0 else 1000),
                label = "TartilTarhimSlideCrossfade",
                modifier = Modifier.fillMaxSize()
            ) { imagePath ->
                val resolvedBg = remember(imagePath, assetsRootDir) {
                    resolveMediaPreview(imagePath, assetsRootDir)
                }
                val imgModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenBurnsScale
                        scaleY = kenBurnsScale
                    }
                when (resolvedBg) {
                    is ResolvedMediaPreview.CoilModel -> AsyncImage(
                        model = resolvedBg.model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imgModifier
                    )
                    is ResolvedMediaPreview.DecodedBitmap -> androidx.compose.foundation.Image(
                        bitmap = resolvedBg.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imgModifier
                    )
                    null -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color.Black, Color(0xFF111827), Color.Black)
                                )
                            )
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Black, Color(0xFF111827), Color.Black)
                        )
                    )
            )
        }

        // ─── 2. Atmospheric Effects (Particles, Tarhim Orb, Top/Bottom Vignettes) ───
        if (!isPerformanceMode) {
            TartilTarhimFloatingParticles(color = particleColor)
        }

        if (isTarhim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tarhimAmber.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(960f, 280f),
                            radius = 700f
                        )
                    )
            )
        }

        // Top vignette for header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.32f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.58f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom vignette for status card, countdown & prayer carousel readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // ─── 3. Content Overlay ───
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Row: Brand (Top-Left) — Active Clock Card (Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                FullscreenBrandHeader(
                    brand = settings.brand,
                    assetsRootDir = assetsRootDir,
                    isLogoMenuOpen = isLogoMenuOpen,
                    onLogoClick = onLogoClick,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Active Clock Card in Top-Right corner using the checked Style Card Jam
                HeaderClockCard(
                    now = snapshot.now,
                    clockStyle = settings.brand.clockStyle,
                    modifier = Modifier.size(width = 480.dp, height = 180.dp)
                )
            }

            // Open middle space to showcase background image/video
            Spacer(modifier = Modifier.weight(1f))

            // Row above carousel: Status+Visualizer+Countdown (left for Tartil, right for Tarhim, 4.dp margin matching PrayerTimesCarousel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = if (!isTarhim) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                TartilTarhimStatusCard(
                    title = if (isTarhim) "Tarhim" else "Tartilul Qur'an",
                    subtitle = "Menuju $prayerLabel",
                    countdownSeconds = adzanSeconds,
                    isTarhim = isTarhim,
                    accentColor = if (isTarhim) tarhimAmber else palette.accent
                )
            }

            // Bottom Prayer Times Carousel (No QR Card displayed during Tartil & Tarhim)
            PrayerTimesCarousel(
                schedule = snapshot.schedule,
                scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                isVertical = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FullscreenBrandHeader(
    brand: id.algaffar.jwstv.domain.model.BrandSettings,
    assetsRootDir: File?,
    isLogoMenuOpen: Boolean,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleFontSp = (brand.titleFontSize * 1.25f).roundToInt().coerceIn(28, 54)
    val addressFontSp = (brand.addressFontSize * 1.15f).roundToInt().coerceIn(16, 28)

    Row(
        modifier = modifier
            .widthIn(max = 1100.dp)
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        MosqueLogoBadge(
            logoPath = brand.logo,
            assetsRootDir = assetsRootDir,
            isMenuOpen = isLogoMenuOpen,
            onClick = onLogoClick
        )

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = brand.name.ifBlank { "MASJID AL-GAFFAR" },
                color = Color.White,
                fontSize = titleFontSp.sp,
                lineHeight = (titleFontSp + 4).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            if (brand.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = brand.address,
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = addressFontSp.sp,
                        lineHeight = (addressFontSp + 4).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TartilTarhimFloatingParticles(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "TartilParticles")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticleProgress"
    )

    val seeds = remember {
        listOf(
            Triple(0.10f, 0.0f, 4.5f),
            Triple(0.24f, 0.35f, 3.5f),
            Triple(0.38f, 0.70f, 5.0f),
            Triple(0.52f, 0.18f, 3.0f),
            Triple(0.66f, 0.55f, 4.5f),
            Triple(0.79f, 0.82f, 3.5f),
            Triple(0.90f, 0.28f, 5.5f),
            Triple(0.16f, 0.62f, 4.0f)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        seeds.forEach { (xFrac, phaseOffset, radiusDp) ->
            val t = (progress + phaseOffset) % 1f
            val y = size.height * (1f - t)
            val x = size.width * xFrac + sin(t * 6.28318f) * 24f
            val alpha = (sin(t * 3.14159f) * 0.55f).coerceIn(0f, 0.55f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radiusDp.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun TartilTarhimStatusCard(
    title: String,
    subtitle: String,
    countdownSeconds: Long,
    isTarhim: Boolean,
    accentColor: Color
) {
    val borderColor = if (isTarhim) {
        Color(0xFFFBBF24).copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }
    val subtitleColor = if (isTarhim) {
        Color(0xFFFDE68A).copy(alpha = 0.80f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }

    val safeSec = countdownSeconds.coerceAtLeast(0L)
    val hrs = safeSec / 3600L
    val mins = (safeSec % 3600L) / 60L
    val secs = safeSec % 60L
    val formattedCountdown = if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AnimatedAudioVisualizerBars(accentColor = accentColor)

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle.uppercase(Locale("id", "ID")),
                color = subtitleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(64.dp)
                .background(borderColor)
        )

        Text(
            text = formattedCountdown,
            color = Color.White,
            fontSize = 79.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                fontFeatureSettings = "tnum",
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
private fun AnimatedAudioVisualizerBars(
    accentColor: Color,
    barCount: Int = 20
) {
    val isPerformanceMode = LocalPerformanceMode.current
    val phase: Float = if (isPerformanceMode) {
        0.8f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "VisualizerTransition")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.28318f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "VisualizerPhase"
        )
        animated
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(46.dp)
    ) {
        for (i in 0 until barCount) {
            val wave = ((sin(phase + i * 0.45f) + 1f) / 2f).coerceIn(0.18f, 1f)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(wave)
                    .clip(CircleShape)
                    .background(
                        if (isPerformanceMode) {
                            Brush.verticalGradient(listOf(accentColor, accentColor))
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.35f))
                            )
                        }
                    )
            )
        }
    }
}


/**
 * 2. ADZAN FULLSCREEN (`AdzanContent.tsx`)
 * Features:
 * - Dynamic sky palette & Mosque Silhouette Horizon per prayer (`Subuh`, `Dzuhur`, `Ashar`, `Maghrib`, `Isya`)
 * - Pulsing Adzan callout badge & Arabic calligraphy
 * - Post-Adzan Du'a / Sunnah reminder
 */
private data class AdzanHorizonTheme(
    val baseColor: Color,
    val horizonStops: List<Pair<Float, Color>>,
    val glowColor: Color,
    val accentStart: Color,
    val accentEnd: Color,
    val taglineColor: Color,
    val particleColor: Color,
    val silhouetteColor: Color,
    val defaultDesc: String
)

@Composable
fun AdzanFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val activePrayer = snapshot.tickResult.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)
    val stageCfg = snapshot.settings.prayers.forPrayer(activePrayer)
    val remainingSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remainingSec / 60L, remainingSec % 60L)

    val theme = remember(activePrayer) {
        when (activePrayer) {
            PrayerId.FAJR -> AdzanHorizonTheme(
                baseColor = Color(0xFF020617),
                horizonStops = listOf(
                    0.0f to Color(0xFF7DD3FC),
                    0.25f to Color(0xFF38BDF8),
                    0.60f to Color(0xFF1E40AF),
                    1.0f to Color(0xFF020617)
                ),
                glowColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                accentStart = Color(0xFFCFFAFE),
                accentEnd = Color(0xFF60A5FA),
                taglineColor = Color(0xFF67E8F9).copy(alpha = 0.85f),
                particleColor = Color(0xFFCFFAFE).copy(alpha = 0.45f),
                silhouetteColor = Color(0xFF020617),
                defaultDesc = "Fajar Menyingsing, Saatnya Bersujud"
            )
            PrayerId.DHUHR -> AdzanHorizonTheme(
                baseColor = Color(0xFF0F172A),
                horizonStops = listOf(
                    0.0f to Color(0xFFFEF08A),
                    0.25f to Color(0xFFEAB308),
                    0.60f to Color(0xFF3B82F6),
                    1.0f to Color(0xFF0F172A)
                ),
                glowColor = Color(0xFFEAB308).copy(alpha = 0.25f),
                accentStart = Color(0xFFFEF3C7),
                accentEnd = Color(0xFFF59E0B),
                taglineColor = Color(0xFFFDE68A).copy(alpha = 0.85f),
                particleColor = Color.White.copy(alpha = 0.45f),
                silhouetteColor = Color(0xFF0F172A),
                defaultDesc = "Cahaya Terang, Hati Tenang Dalam Shalat"
            )
            PrayerId.ASR -> AdzanHorizonTheme(
                baseColor = Color(0xFF0F172A),
                horizonStops = listOf(
                    0.0f to Color(0xFFFDBA74),
                    0.25f to Color(0xFFF97316),
                    0.60f to Color(0xFF1E3A8A),
                    1.0f to Color(0xFF0F172A)
                ),
                glowColor = Color(0xFFF97316).copy(alpha = 0.25f),
                accentStart = Color(0xFFFFEDD5),
                accentEnd = Color(0xFFF43F5E),
                taglineColor = Color(0xFFFED7AA).copy(alpha = 0.85f),
                particleColor = Color(0xFFFFEDD5).copy(alpha = 0.45f),
                silhouetteColor = Color(0xFF0F172A),
                defaultDesc = "Sore Berkah, Langkah Menuju Kemenangan"
            )
            PrayerId.MAGHRIB -> AdzanHorizonTheme(
                baseColor = Color(0xFF020617),
                horizonStops = listOf(
                    0.0f to Color(0xFFFBBF24),
                    0.25f to Color(0xFFEF4444),
                    0.60f to Color(0xFF4C1D95),
                    1.0f to Color(0xFF020617)
                ),
                glowColor = Color(0xFFFBBF24).copy(alpha = 0.25f),
                accentStart = Color(0xFFFEF3C7),
                accentEnd = Color(0xFFFB923C),
                taglineColor = Color(0xFFFCD34D).copy(alpha = 0.85f),
                particleColor = Color(0xFFFEF3C7).copy(alpha = 0.45f),
                silhouetteColor = Color(0xFF020617),
                defaultDesc = "Tenggelam Mentari, Terbitlah Kedamaian Hati"
            )
            else -> AdzanHorizonTheme(
                baseColor = Color(0xFF000000),
                horizonStops = listOf(
                    0.0f to Color(0xFF1E293B),
                    0.25f to Color(0xFF111827),
                    0.60f to Color(0xFF020617),
                    1.0f to Color(0xFF000000)
                ),
                glowColor = Color(0xFF818CF8).copy(alpha = 0.25f),
                accentStart = Color(0xFFE0E7FF),
                accentEnd = Color(0xFF60A5FA),
                taglineColor = Color(0xFFC7D2FE).copy(alpha = 0.70f),
                particleColor = Color(0xFFE0E7FF).copy(alpha = 0.60f),
                silhouetteColor = Color(0xFF000000),
                defaultDesc = "Malam Menjemput, Mengadu Di Depan Sang Pencipta"
            )
        }
    }

    val displayTagline = stageCfg.adzanTagline.trim().ifBlank { "Panggilan Suci" }
    val displayMessage = stageCfg.adzanMessage.trim().ifBlank { theme.defaultDesc }
    val isPerformanceMode = LocalPerformanceMode.current

    val pulseAlpha: Float = if (isPerformanceMode) {
        0.40f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "AdzanGlowPulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.52f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "AdzanGlowAlpha"
        )
        animated
    }

    val particles = remember(activePrayer) {
        val count = if (activePrayer == PrayerId.ISHA) 36 else 18
        val rng = java.util.Random(activePrayer.ordinal.toLong() + 101L)
        List(count) {
            Triple(rng.nextFloat(), rng.nextFloat(), 2f + rng.nextFloat() * 4f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.baseColor)
    ) {
        // Horizon Radial Gradient + Particles + Ottoman Mosque Silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.radialGradient(
                    colorStops = theme.horizonStops.toTypedArray(),
                    center = Offset(w * 0.5f, h * 1.02f),
                    radius = maxOf(w, h) * 0.92f
                ),
                alpha = 0.85f
            )

            if (!isPerformanceMode) {
                particles.forEach { (px, py, r) ->
                    drawCircle(
                        color = theme.particleColor,
                        radius = r,
                        center = Offset(w * px, h * py)
                    )
                }

                // Soft radial glow behind heading
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.glowColor.copy(alpha = pulseAlpha), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.42f),
                        radius = w * 0.34f
                    ),
                    radius = w * 0.34f,
                    center = Offset(w * 0.5f, h * 0.42f)
                )
            }

            // Ottoman Classic Mosque Silhouette at bottom center (opacity 0.25)
            val silW = w * 0.70f
            val silLeft = (w - silW) / 2f
            val silH = silW * 0.30f
            val silTop = h - silH
            fun sx(v: Float) = silLeft + (v / 1000f) * silW
            fun sy(v: Float) = silTop + (v / 300f) * silH

            val silColor = theme.silhouetteColor.copy(alpha = 0.45f)

            // Main Central Dome
            val mainDome = Path().apply {
                moveTo(sx(400f), sy(280f))
                cubicTo(sx(400f), sy(160f), sx(600f), sy(160f), sx(600f), sy(280f))
                lineTo(sx(600f), sy(300f))
                lineTo(sx(400f), sy(300f))
                close()
            }
            drawPath(mainDome, color = silColor)

            // Side Domes
            val leftDome = Path().apply {
                moveTo(sx(300f), sy(280f))
                cubicTo(sx(300f), sy(220f), sx(400f), sy(220f), sx(400f), sy(280f))
                close()
            }
            drawPath(leftDome, color = silColor)

            val rightDome = Path().apply {
                moveTo(sx(600f), sy(280f))
                cubicTo(sx(600f), sy(220f), sx(700f), sy(220f), sx(700f), sy(280f))
                close()
            }
            drawPath(rightDome, color = silColor)

            // Base structure
            drawRect(
                color = silColor,
                topLeft = Offset(sx(280f), sy(280f)),
                size = Size(sx(720f) - sx(280f), sy(300f) - sy(280f))
            )

            // Outer Minarets
            listOf(Triple(240f, 247f, 255f), Triple(745f, 752f, 760f)).forEach { (l, mid, r) ->
                val minaret = Path().apply {
                    moveTo(sx(l), sy(300f))
                    lineTo(sx(r), sy(300f))
                    lineTo(sx(r), sy(80f))
                    lineTo(sx(mid), sy(50f))
                    lineTo(sx(l), sy(80f))
                    close()
                }
                drawPath(minaret, color = silColor)
            }

            // Inner Minarets
            listOf(Triple(320f, 326f, 332f), Triple(668f, 674f, 680f)).forEach { (l, mid, r) ->
                val minaret = Path().apply {
                    moveTo(sx(l), sy(280f))
                    lineTo(sx(r), sy(280f))
                    lineTo(sx(r), sy(120f))
                    lineTo(sx(mid), sy(90f))
                    lineTo(sx(l), sy(120f))
                    close()
                }
                drawPath(minaret, color = silColor)
            }
        }

        // Center Content (matching AdzanContent.tsx + HR. Bukhari Hadith)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tagline Row with flanking gradient lines
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
                Text(
                    text = displayTagline.uppercase(),
                    color = theme.taglineColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 7.sp
                )
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Heading: ADZAN [SHOLAT]
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ADZAN",
                    color = Color.White,
                    fontSize = 208.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-4).sp,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = prayerLabel.uppercase(),
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(theme.accentStart, theme.accentEnd)),
                        fontSize = 208.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-4).sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quote / Message framed by horizontal gradient dividers
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.widthIn(max = 960.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.40f), Color.Transparent)
                            )
                        )
                )
                Text(
                    text = "“$displayMessage”",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.40f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preserved Hadith (HR. Bukhari) Card
            Box(
                modifier = Modifier
                    .widthIn(max = 1480.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 44.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "“Barangsiapa ketika mendengar adzan mengucapkan doa setelah adzan, maka ia berhak mendapatkan syafa'atku pada hari kiamat.” (HR. Bukhari)",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 38.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 54.sp
                )
            }
        }

        // Bottom-Right CountdownBadge ("DURASI ADZAN", matching CountdownBadge.tsx)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.78f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.accentEnd.copy(alpha = 0.20f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = theme.accentStart,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column {
                Text(
                    text = "DURASI ADZAN",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
                Text(
                    text = formattedRem,
                    color = Color.White,
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

data class OverlayOfficerInfo(
    val role: String,
    val name: String,
    val icon: ImageVector = Icons.Default.Person
)

/**
 * Unified Simple State Overlay (`SimpleStateOverlay.tsx` + `FullscreenShell.tsx`).
 * Used for all 9 serene ibadah/lecture states:
 * - `pelaksanaan_sholat`
 * - `khutbah`, `pelaksanaan_jumat`
 * - `ceramah_tarawih`, `sholat_tarawih`
 * - `sholat_idul_fitri`, `khutbah_idul_fitri`
 * - `sholat_idul_adha`, `khutbah_idul_adha`
 */
@Composable
fun SimpleStateOverlayFullscreen(
    snapshot: JwsRuntimeSnapshot,
    stageTitle: String,
    stageMessage: String,
    officers: List<OverlayOfficerInfo>,
    countdownLabel: String = stageTitle.replace("\n", " "),
    stageContextBadge: String? = null,
    stageIcon: ImageVector = Icons.Default.Schedule,
    accentColor: Color = Color(0xFF10B981),
    showTopRightClock: Boolean = true,
    backgroundImagePath: String = "",
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val totalSec = snapshot.tickResult.totalStageSeconds.coerceAtLeast(1L)
    val progress = (remSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)
    val resolvedBg = remember(backgroundImagePath, assetsRootDir) {
        resolveMediaPreview(backgroundImagePath, assetsRootDir)
    }

    val effectiveAccent = if (palette.isLight) accentColor else palette.accent
    val secondaryRibbonColor = palette.accentSecondary

    // Split stageTitle into prefix ("PELAKSANAAN SHOLAT" / "KHUTBAH") and main name ("DZUHUR" / "JUM'AT")
    val (prefixTitle, mainNameTitle) = remember(stageTitle) {
        val trimmed = stageTitle.trim()
        if (trimmed.contains('\n')) {
            val parts = trimmed.split('\n', limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            val prefixes = listOf("Pelaksanaan Sholat ", "Khutbah ", "Sholat ", "Ceramah ")
            val matched = prefixes.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            if (matched != null) {
                trimmed.substring(0, matched.length).trim() to trimmed.substring(matched.length).trim()
            } else {
                "" to trimmed
            }
        }
    }

    val pulseAlpha: Float = if (isPerformanceMode) {
        0.10f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "SimpleStatePulse")
        val p by infiniteTransition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbPulse"
        )
        p
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgPrimary, palette.bgSecondary, Color(0xFF020617))
                )
            )
    ) {
        // 1. Right-Side Media Gallery Area (occupies right ~64% behind the diagonal split)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.64f)
        ) {
            if (resolvedBg != null) {
                when (resolvedBg) {
                    is ResolvedMediaPreview.CoilModel -> AsyncImage(
                        model = resolvedBg.model,
                        contentDescription = countdownLabel,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    is ResolvedMediaPreview.DecodedBitmap -> androidx.compose.foundation.Image(
                        bitmap = resolvedBg.bitmap,
                        contentDescription = countdownLabel,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    palette.bgPrimary.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                effectiveAccent.copy(alpha = pulseAlpha * 1.4f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.62f, size.height * 0.48f),
                            radius = size.width * 0.55f
                        ),
                        radius = size.width * 0.55f,
                        center = Offset(size.width * 0.62f, size.height * 0.48f)
                    )
                    val step = 52.dp.toPx()
                    var x = step * 0.5f
                    while (x < size.width) {
                        var y = step * 0.5f
                        while (y < size.height) {
                            drawCircle(
                                color = palette.textPrimary.copy(alpha = 0.08f),
                                radius = 2f,
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }
                }
            }
        }

        // 2. Diagonal Layered Geometric Split (Left Panel + 2 Folded Theme Ribbons)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val outerRibbon = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.575f, 0f)
                lineTo(w * 0.495f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = outerRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        secondaryRibbonColor.copy(alpha = 0.92f),
                        effectiveAccent.copy(alpha = 0.88f)
                    ),
                    start = Offset(w * 0.40f, 0f),
                    end = Offset(w * 0.55f, h)
                )
            )

            val middleRibbon = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.562f, 0f)
                lineTo(w * 0.445f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = middleRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        effectiveAccent.copy(alpha = 0.94f),
                        palette.bgSecondary.copy(alpha = 0.96f)
                    ),
                    start = Offset(w * 0.36f, 0f),
                    end = Offset(w * 0.50f, h)
                )
            )

            val primaryPanel = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.545f, 0f)
                lineTo(w * 0.395f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = primaryPanel,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.bgPrimary,
                        palette.bgSecondary,
                        palette.bgPrimary.copy(alpha = 0.98f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.50f, h)
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(effectiveAccent.copy(alpha = pulseAlpha), Color.Transparent),
                    center = Offset(w * 0.16f, h * 0.25f),
                    radius = w * 0.34f
                ),
                radius = w * 0.34f,
                center = Offset(w * 0.16f, h * 0.25f)
            )
        }

        // 3. Left Panel Content (Brand, Badge, Title, Message, Countdown Pill)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.52f)
                .padding(start = 52.dp, end = 36.dp, top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top: Mosque Brand + State Context Badge
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FullscreenBrandHeader(
                    brand = snapshot.settings.brand,
                    assetsRootDir = assetsRootDir,
                    isLogoMenuOpen = isLogoMenuOpen,
                    onLogoClick = onLogoClick
                )

                val badgeText = stageContextBadge?.takeIf { it.isNotBlank() }
                    ?: countdownLabel
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(effectiveAccent.copy(alpha = 0.18f))
                        .border(1.dp, effectiveAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = stageIcon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = badgeText.uppercase(),
                        color = effectiveAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle: Prefix + 2x Prayer/Khutbah Name (Distinct Font & Theme Color) + 2x Adab Message
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (prefixTitle.isNotBlank()) {
                    Text(
                        text = prefixTitle.uppercase(),
                        color = palette.textPrimary,
                        fontSize = 36.sp,
                        lineHeight = 42.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = mainNameTitle.uppercase(),
                    color = secondaryRibbonColor,
                    fontSize = 104.sp,
                    lineHeight = 108.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                Text(
                    text = "“$stageMessage”",
                    color = palette.textSecondary,
                    fontSize = 48.sp,
                    lineHeight = 56.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 4.dp)
                )
            }

            // Bottom-Left: Compact Bold Countdown Pill (1.5x font, no underline, tighter padding)
            val pillBg = if (palette.isLight) palette.bgPrimary else Color.White
            val pillText = if (palette.isLight) Color.White else palette.bgPrimary
            val pillAccent = if (palette.isLight) palette.accent else palette.bgSecondary

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(pillBg)
                    .border(2.dp, secondaryRibbonColor.copy(alpha = 0.55f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(effectiveAccent.copy(alpha = 0.18f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = stageIcon,
                        contentDescription = null,
                        tint = pillAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "SISA WAKTU",
                        color = pillText.copy(alpha = 0.72f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formattedRem,
                        color = pillText,
                        fontSize = 54.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }

        // 4. Top-Right Clock Card matching Standby size (480x180dp) & selected clockStyle
        if (showTopRightClock) {
            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 32.dp)
            )
        }

        // 5. Bottom-Right Officer Card(s) (Imam / Khotib / Penceramah) - Right-Aligned, 2.5x Font
        if (officers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 36.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                officers.forEach { officer ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.bgPrimary.copy(alpha = 0.88f))
                            .border(2.dp, secondaryRibbonColor.copy(alpha = 0.60f), RoundedCornerShape(50))
                            .padding(horizontal = 36.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 860.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = officer.role.uppercase(),
                                color = secondaryRibbonColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.5.sp,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = officer.name.trim().ifBlank { "Ust. Fulan" },
                                color = palette.textPrimary,
                                fontSize = 65.sp,
                                lineHeight = 72.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(secondaryRibbonColor.copy(alpha = 0.18f))
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = officer.icon,
                                contentDescription = null,
                                tint = secondaryRibbonColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
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
private data class IqomahThemeConfig(
    val gradientStops: List<Color>,
    val glowColor: Color,
    val accentStart: Color,
    val accentEnd: Color
)

@Composable
fun IqomahFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val tick = snapshot.tickResult
    val activePrayer = tick.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)

    val remSec = tick.countdownSeconds.coerceAtLeast(0L)

    val mins = String.format(Locale.US, "%02d", remSec / 60L)
    val secs = String.format(Locale.US, "%02d", remSec % 60L)

    val isWarning = tick.isNoSunnahWarningActive
    val isPerformanceMode = LocalPerformanceMode.current

    val theme = remember(activePrayer) {
        when (activePrayer) {
            PrayerId.FAJR -> IqomahThemeConfig(
                gradientStops = listOf(Color(0xFF020617), Color(0xFF083344), Color(0xFF164E63)),
                glowColor = Color(0xFF22D3EE).copy(alpha = 0.30f),
                accentStart = Color(0xFF22D3EE),
                accentEnd = Color(0xFF60A5FA)
            )
            PrayerId.DHUHR -> IqomahThemeConfig(
                gradientStops = listOf(Color(0xFF020617), Color(0xFF422006), Color(0xFF713F12)),
                glowColor = Color(0xFFF59E0B).copy(alpha = 0.30f),
                accentStart = Color(0xFFFBBF24),
                accentEnd = Color(0xFFFACC15)
            )
            PrayerId.ASR -> IqomahThemeConfig(
                gradientStops = listOf(Color(0xFF020617), Color(0xFF431407), Color(0xFF7C2D12)),
                glowColor = Color(0xFFF97316).copy(alpha = 0.30f),
                accentStart = Color(0xFFFB923C),
                accentEnd = Color(0xFFFB7185)
            )
            PrayerId.MAGHRIB -> IqomahThemeConfig(
                gradientStops = listOf(Color(0xFF020617), Color(0xFF4C0519), Color(0xFF881337)),
                glowColor = Color(0xFFF43F5E).copy(alpha = 0.30f),
                accentStart = Color(0xFFFB7185),
                accentEnd = Color(0xFFF472B6)
            )
            else -> IqomahThemeConfig(
                gradientStops = listOf(Color(0xFF020617), Color(0xFF1E1B4B), Color(0xFF312E81)),
                glowColor = Color(0xFF6366F1).copy(alpha = 0.30f),
                accentStart = Color(0xFF818CF8),
                accentEnd = Color(0xFF60A5FA)
            )
        }
    }

    val warningPulseScale: Float = if (isWarning && !isPerformanceMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "IqomahPulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "IqomahScale"
        )
        animated
    } else {
        1.0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(theme.gradientStops))
    ) {
        // Subtle radial ambient blurs matching IqomahContent.tsx
        if (!isPerformanceMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.accentStart.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.25f),
                        radius = size.width * 0.40f
                    ),
                    radius = size.width * 0.40f,
                    center = Offset(size.width * 0.22f, size.height * 0.25f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.accentEnd.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.78f, size.height * 0.72f),
                        radius = size.width * 0.36f
                    ),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.78f, size.height * 0.72f)
                )
            }
        }

        // Center Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Label: — [Timer] MENUNGGU —
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, theme.accentStart.copy(alpha = 0.6f))))
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = theme.accentStart,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "MENUNGGU",
                    color = theme.accentStart.copy(alpha = 0.85f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 10.sp
                )
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(theme.accentStart.copy(alpha = 0.6f), Color.Transparent)))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Title: IQOMAH [SHOLAT]
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IQOMAH",
                    color = Color.White,
                    fontSize = 172.sp,
                    lineHeight = 136.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-4).sp,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
                Text(
                    text = prayerLabel.uppercase(),
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(theme.accentStart, theme.accentEnd)),
                        fontSize = 172.sp,
                        lineHeight = 136.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-4).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Massive Countdown Timer (MM : SS) — 80% of 690.sp = 552.sp
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = warningPulseScale
                        scaleY = warningPulseScale
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = mins,
                    color = Color.White,
                    fontSize = 552.sp,
                    lineHeight = 410.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        ),
                        fontFeatureSettings = "tnum"
                    )
                )
                Text(
                    text = ":",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 504.sp,
                    lineHeight = 410.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
                Text(
                    text = secs,
                    color = Color.White,
                    fontSize = 552.sp,
                    lineHeight = 410.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    softWrap = false,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        ),
                        fontFeatureSettings = "tnum"
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preserved Hadith (HR. Abu Dawud & Tirmidzi) replaced by Sunnah Warning when countdown < 2 minutes
            if (isWarning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.88f))
                        .border(2.dp, Color(0xFFEF4444).copy(alpha = 0.60f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 40.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Iqomah segera tiba... HARAP TIDAK MEMULAI SHOLAT SUNNAT.",
                        color = Color(0xFFFECACA),
                        fontSize = 44.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.40f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 56.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "“Doa di antara adzan dan iqomah tidak tertolak.” (HR. Abu Dawud & Tirmidzi)",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 48.sp,
                        lineHeight = 64.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
