package id.algaffar.jwstv.ui.screens.standby

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.BrandSettings
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.MasjidQrSettings
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.domain.model.RamadhanSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.components.clock.BigAnalogClock
import id.algaffar.jwstv.ui.screens.fullscreen.EidPetugasSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.FinanceQrFullscreen
import id.algaffar.jwstv.ui.screens.fullscreen.IdulFitriKeuanganSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.MasjidQrFullscreen
import id.algaffar.jwstv.ui.screens.fullscreen.OnlineFinanceCardContent
import id.algaffar.jwstv.ui.screens.fullscreen.OnlineFinanceFullscreen
import id.algaffar.jwstv.ui.screens.fullscreen.QurbanGroupSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.RamadhanJadwalSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.TarawihKeuanganSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.TarawihPetugasSlideView
import id.algaffar.jwstv.ui.screens.fullscreen.TarawihTakjilSlideView
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import kotlinx.coroutines.delay
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale

private sealed interface MainDeckSlide {
    val durationSeconds: Int

    data class CustomSlide(
        val item: SlideItem,
        override val durationSeconds: Int = item.duration.coerceIn(4, 120)
    ) : MainDeckSlide

    data class JumatPetugas(
        val fridayDate: String,
        val record: JumatPetugasRecord,
        override val durationSeconds: Int = 10
    ) : MainDeckSlide

    data class JumatKeuangan(
        val fridayDate: String,
        val record: JumatKeuanganRecord,
        override val durationSeconds: Int = 12
    ) : MainDeckSlide

    data class RamadhanOverview(
        val ramadhan: RamadhanSettings,
        override val durationSeconds: Int = 12
    ) : MainDeckSlide
}

internal data class WelcomeMasjidLines(
    val line1: String,
    val line2: String,
    val line3: String,
    val line4: String,
    val line5: String
)

private val NAMA_MASJID_PLACEHOLDER_REGEX = Regex("\\{\\{\\s*Nama Masjid\\s*\\}\\}", RegexOption.IGNORE_CASE)
private val ALAMAT_MASJID_PLACEHOLDER_REGEX = Regex("\\{\\{\\s*Alamat(?:\\s+Masjid)?\\s*\\}\\}", RegexOption.IGNORE_CASE)

/**
 * Parses multi-line Greeting template supporting `{{Nama Masjid}}` and `{{Alamat Masjid}}` placeholders,
 * mirroring `WelcomeMasjidSlide.tsx` in `jws-android-algaffar`.
 */
internal fun parseWelcomeMasjidLines(
    rawContent: String?,
    mosqueName: String,
    mosqueAddress: String
): WelcomeMasjidLines {
    val safeName = mosqueName.ifBlank { "Masjid Al-Gaffar" }
    val safeAddress = mosqueAddress.trim()

    if (!rawContent.isNullOrBlank() && rawContent.contains('\n')) {
        val rawLines = rawContent.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.size >= 5) {
            return WelcomeMasjidLines(
                line1 = NAMA_MASJID_PLACEHOLDER_REGEX.replace(rawLines[0], safeName),
                line2 = NAMA_MASJID_PLACEHOLDER_REGEX.replace(rawLines[1], safeName),
                line3 = ALAMAT_MASJID_PLACEHOLDER_REGEX.replace(rawLines[2], safeAddress),
                line4 = rawLines[3],
                line5 = rawLines[4]
            )
        }
        return WelcomeMasjidLines(
            line1 = "SELAMAT DATANG DI",
            line2 = safeName,
            line3 = safeAddress,
            line4 = rawLines.getOrNull(2) ?: "SELAMA BERADA DI DALAM MASJID",
            line5 = rawLines.getOrNull(3) ?: "HARAP TENANG DAN KHIDMAT"
        )
    }

    return WelcomeMasjidLines(
        line1 = "SELAMAT DATANG DI",
        line2 = safeName,
        line3 = safeAddress,
        line4 = "SELAMA BERADA DI DALAM MASJID",
        line5 = "HARAP TENANG DAN KHIDMAT"
    )
}

/**
 * Main Slide Showcase Container (occupies the 75% main visual area on `StandbyScreen`).
 * Automatically rotates default system slides (System Greeting `welcome` & Big Analog Clock `analog_clock`),
 * user slides, Friday Petugas & Finance reports, Ramadhan cards, and overlays the Floating Masjid QR Infaq badge.
 * Mirrors `jws-android-algaffar/src/components/Screens/Main/Slide/index.tsx`.
 */
@Composable
fun MainSlideContainer(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val today = snapshot.schedule.date

    val deck: List<MainDeckSlide> = remember(today, settings) {
        buildList {
            // 1. Configured & Default System Slides
            val configuredMainSlides = settings.slides.filter { it.enabled && it.showInMain }
            val configuredTypes = configuredMainSlides.map { it.type }.toSet()
            configuredMainSlides.forEach { slide ->
                add(MainDeckSlide.CustomSlide(slide))
            }

            // 2. Auto Ramadhan slides when Ramadhan mode is active and no explicit ramadhan slide added
            val hasExplicitRamadhanSlide = configuredTypes.any { it.startsWith("ramadhan_") }
            if (settings.ramadhan.isRamadhan && !hasExplicitRamadhanSlide) {
                val dur = settings.ramadhan.slideshow.slideDuration.coerceIn(5, 60)
                if (settings.ramadhan.slideshow.showPetugas) {
                    add(MainDeckSlide.CustomSlide(SlideItem(id = "auto-ramadhan-petugas", type = "ramadhan_petugas", duration = dur)))
                }
                if (settings.ramadhan.slideshow.showKeuangan) {
                    add(MainDeckSlide.CustomSlide(SlideItem(id = "auto-ramadhan-keuangan", type = "ramadhan_keuangan", duration = dur)))
                }
                if (settings.ramadhan.slideshow.showJadwal) {
                    add(MainDeckSlide.CustomSlide(SlideItem(id = "auto-ramadhan-jadwal", type = "ramadhan_jadwal", duration = dur)))
                }
                if (settings.ramadhan.slideshow.showTakjil) {
                    add(MainDeckSlide.CustomSlide(SlideItem(id = "auto-ramadhan-takjil", type = "ramadhan_takjil", duration = dur)))
                }
            }

            // 3. Fallback so main container is never blank if user disabled all slides
            if (isEmpty()) {
                SlideItem.defaultSystemSlides().forEach { add(MainDeckSlide.CustomSlide(it)) }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.mod(deck.size.coerceAtLeast(1))
    val activeSlide = deck[safeIndex]

    val activeCustomSlide = (activeSlide as? MainDeckSlide.CustomSlide)?.item
    val activeSlideType = when (activeSlide) {
        is MainDeckSlide.CustomSlide -> activeSlide.item.type
        is MainDeckSlide.JumatPetugas -> "jumat_petugas"
        is MainDeckSlide.JumatKeuangan -> "jumat_keuangan"
        is MainDeckSlide.RamadhanOverview -> "ramadhan_petugas"
    }
    val isDenseInfoSlide = activeSlideType in DENSE_INFO_SLIDE_TYPES
    val isSystemDefaultSlide = activeSlideType == "welcome" || activeSlideType == "analog_clock"
    val shouldHideOverlayText = isSystemDefaultSlide || isDenseInfoSlide || (activeCustomSlide?.hideOverlayText == true)
    val shouldHideQrCard = isDenseInfoSlide || (activeCustomSlide?.hideQrCard == true)

    val overlayAlignment = if (settings.overlayPosition == "top") Alignment.TopStart else Alignment.BottomStart
    val qrAlignment = when (settings.masjidQr.floatingPosition) {
        "top-left" -> Alignment.TopStart
        "bottom-left" -> Alignment.BottomStart
        "top-right" -> Alignment.TopEnd
        else -> Alignment.BottomEnd
    }

    LaunchedEffect(safeIndex, activeSlide.durationSeconds, deck.size, settings.slideConfig.autoPlay) {
        if (!settings.slideConfig.autoPlay || deck.size <= 1) return@LaunchedEffect
        delay(activeSlide.durationSeconds.coerceIn(4, 120) * 1000L)
        currentIndex = (safeIndex + 1) % deck.size
    }

    val isPerformanceMode = LocalPerformanceMode.current
    Box(
        modifier = modifier
            .size(width = 1440.dp, height = 675.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
    ) {
        AnimatedContent(
            targetState = activeSlide,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (isPerformanceMode) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(700)) togetherWith fadeOut(tween(500))
                }
            },
            label = "MainDeckSlideTransition"
        ) { slide ->
            when (slide) {
                is MainDeckSlide.CustomSlide -> CustomSlideView(
                    slide = slide.item,
                    snapshot = snapshot,
                    assetsRootDir = assetsRootDir
                )
                is MainDeckSlide.JumatPetugas -> JumatPetugasSlideView(
                    fridayDate = slide.fridayDate,
                    record = slide.record,
                    isRowLayout = true,
                    isExpanded = false
                )
                is MainDeckSlide.JumatKeuangan -> JumatKeuanganSlideView(
                    fridayDate = slide.fridayDate,
                    record = slide.record,
                    isExpanded = false
                )
                is MainDeckSlide.RamadhanOverview -> {
                    val nightIdx = (slide.ramadhan.currentNight - 1).coerceIn(0, 29)
                    val p = slide.ramadhan.petugas.getOrNull(nightIdx)
                    TarawihPetugasSlideView(
                        night = slide.ramadhan.currentNight,
                        hijriYear = slide.ramadhan.hijriYear,
                        penceramah = p?.penceramah ?: "-",
                        imamTarawih = p?.imamTarawih ?: "-",
                        imamWitir = p?.imamWitir ?: "-",
                        isExpanded = false
                    )
                }
            }
        }

        // Smart Floating Masjid QR Infaq Badge (configurable 4-corner position, auto-hidden on dense slides & per-slide)
        AnimatedVisibility(
            visible = settings.masjidQr.enabled && settings.masjidQr.showFloatingOnMain && !shouldHideQrCard,
            enter = if (isPerformanceMode) EnterTransition.None else fadeIn(tween(350)),
            exit = if (isPerformanceMode) ExitTransition.None else fadeOut(tween(250)),
            modifier = Modifier
                .align(qrAlignment)
                .padding(20.dp)
        ) {
            FloatingMasjidQrBadge(
                masjidQr = settings.masjidQr
            )
        }

        // Smart Floating Overlay Running Text (single-line marquee, Peek & Clear sync, auto-hidden on dense slides & per-slide)
        OverlayRunningText(
            messages = settings.overlayTextList,
            visibleDurationSeconds = settings.overlayVisibleDuration,
            hiddenDurationSeconds = settings.overlayHiddenDuration,
            speed = settings.overlaySpeed,
            slideTriggerKey = safeIndex,
            slideClearDelaySeconds = settings.overlaySlideClearSeconds,
            shouldHide = shouldHideOverlayText,
            modifier = Modifier.align(overlayAlignment)
        )
    }
}

private val DENSE_INFO_SLIDE_TYPES = setOf(
    "jumat_keuangan",
    "ramadhan_keuangan",
    "ramadhan_takjil",
    "ramadhan_jadwal",
    "iduladha_qurban",
    "online_finance",
    "qr_infaq",
    "qr_finance"
)

/**
 * System Greeting Slide ("Ucapan Selamat Datang di Masjid").
 * Mirrors `jws-android-algaffar/src/components/Screens/Shared/WelcomeMasjidSlide.tsx`
 * for both Main Area (`isSmall = true`) and Fullscreen Slideshow (`isSmall = false`).
 */
@Composable
fun WelcomeMasjidSlideView(
    slide: SlideItem?,
    brand: BrandSettings,
    isSmall: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lines = remember(slide?.content, brand.name, brand.address) {
        parseWelcomeMasjidLines(
            rawContent = slide?.content,
            mosqueName = brand.name,
            mosqueAddress = brand.address
        )
    }

    val heroFontSize = remember(lines.line2, isSmall) {
        val len = lines.line2.length
        if (isSmall) {
            when {
                len > 30 -> 46.sp
                len > 22 -> 54.sp
                else -> 64.sp
            }
        } else {
            when {
                len > 30 -> 70.sp
                len > 22 -> 84.sp
                else -> 98.sp
            }
        }
    }

    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val bgOuter = remember(palette) {
        lerp(
            palette.bgPrimary,
            if (palette.isLight) palette.bgSecondary else Color.Black,
            if (palette.isLight) 0.25f else 0.45f
        )
    }
    val heroGradient = remember(palette) {
        if (palette.isLight) {
            listOf(
                palette.textPrimary,
                lerp(palette.textPrimary, palette.accent, 0.45f),
                palette.accent
            )
        } else {
            listOf(
                palette.textPrimary,
                lerp(palette.textPrimary, palette.accentSecondary, 0.55f),
                palette.accentSecondary
            )
        }
    }
    val pillBg = remember(palette) {
        if (palette.isLight) {
            lerp(palette.bgCard, palette.accent, 0.10f)
        } else {
            lerp(palette.bgPrimary, palette.accent, 0.18f).copy(alpha = 0.90f)
        }
    }
    val pillTextColor = remember(palette) {
        if (palette.isLight) {
            palette.textPrimary
        } else {
            lerp(palette.textPrimary, palette.accentSecondary, 0.45f)
        }
    }
    val textShadowColor = Color.Black.copy(alpha = if (palette.isLight) 0.14f else 0.72f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to palette.bgSecondary,
                        0.6f to palette.bgPrimary,
                        1.0f to bgOuter
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Islamic Dot Motif Overlay harmonized with theme accentSecondary
        if (!isPerformanceMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 28.dp.toPx()
                val dotRadius = 1.2.dp.toPx()
                val dotColor = palette.accentSecondary.copy(alpha = if (palette.isLight) 0.09f else 0.06f)
                var y = step / 2f
                while (y < size.height) {
                    var x = step / 2f
                    while (x < size.width) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        x += step
                    }
                    y += step
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isSmall) 1280.dp else 1640.dp)
                .padding(
                    horizontal = if (isSmall) 40.dp else 64.dp,
                    vertical = if (isSmall) 24.dp else 44.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isSmall) 16.dp else 28.dp, Alignment.CenterVertically)
        ) {
            // Line 1: SELAMAT DATANG DI
            Text(
                text = lines.line1.uppercase(Locale.getDefault()),
                color = palette.accentSecondary,
                fontSize = if (isSmall) 26.sp else 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = if (isSmall) 6.sp else 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = textShadowColor,
                        offset = Offset(0f, 3f),
                        blurRadius = 8f
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            // Line 2: {{Nama Masjid}} (Theme-Harmonized Hero Title)
            Text(
                text = lines.line2,
                fontSize = heroFontSize,
                lineHeight = (heroFontSize.value * 1.12f).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    brush = Brush.horizontalGradient(colors = heroGradient),
                    shadow = Shadow(
                        color = textShadowColor,
                        offset = Offset(0f, 6f),
                        blurRadius = 22f
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            // Line 3: {{Alamat Masjid}} with Theme Accent MapPin icon
            if (lines.line3.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmall) 10.dp else 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(if (isSmall) 26.dp else 34.dp)
                    )
                    Text(
                        text = lines.line3,
                        color = palette.textSecondary,
                        fontSize = if (isSmall) 22.sp else 30.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ornamental Theme Diamond Divider
            Row(
                modifier = Modifier
                    .width(if (isSmall) 580.dp else 780.dp)
                    .padding(vertical = if (isSmall) 4.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    palette.accentSecondary.copy(alpha = 0.6f),
                                    palette.accentSecondary
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(if (isSmall) 14.dp else 16.dp)
                        .rotate(45f)
                        .background(palette.accentSecondary.copy(alpha = if (palette.isLight) 0.22f else 0.36f))
                        .border(2.dp, palette.accentSecondary)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    palette.accentSecondary,
                                    palette.accentSecondary.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Line 4: SELAMA BERADA DI DALAM MASJID
            Text(
                text = lines.line4.uppercase(Locale.getDefault()),
                color = palette.textPrimary.copy(alpha = 0.88f),
                fontSize = if (isSmall) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isSmall) 4.sp else 6.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Line 5: HARAP TENANG DAN KHIDMAT (Theme-Harmonized Pill with VolumeOff icon)
            Row(
                modifier = Modifier
                    .padding(top = if (isSmall) 2.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pillBg)
                    .border(2.dp, palette.accent.copy(alpha = 0.78f), RoundedCornerShape(50))
                    .padding(
                        horizontal = if (isSmall) 32.dp else 52.dp,
                        vertical = if (isSmall) 12.dp else 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isSmall) 14.dp else 20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(if (isSmall) 26.dp else 38.dp)
                )
                Text(
                    text = lines.line5.uppercase(Locale.getDefault()),
                    color = pillTextColor,
                    fontSize = if (isSmall) 24.sp else 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = Shadow(
                            color = textShadowColor,
                            offset = Offset(0f, 2f),
                            blurRadius = 6f
                        ),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
fun StyledTextSlideView(
    slide: SlideItem,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val style = slide.textStyle
    val textColor = remember(style.fontColor, palette.textPrimary) {
        id.algaffar.jwstv.ui.settings.parseHexColorSafe(style.fontColor, palette.textPrimary)
    }
    val align = id.algaffar.jwstv.ui.settings.slideTextAlignOf(style.textAlign)
    val contentSize = id.algaffar.jwstv.ui.settings.slideFontSizeToSp(style.fontSize, isSmallPreview = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgPrimary, palette.bgSecondary)
                )
            )
            .padding(horizontal = 64.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (slide.title.isNotBlank()) {
            Text(
                text = slide.title,
                color = palette.accentSecondary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
        if (slide.content.isNotBlank()) {
            Text(
                text = slide.content,
                color = textColor,
                fontSize = contentSize,
                lineHeight = (contentSize.value * 1.35f).sp,
                fontWeight = if (style.bold) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                textAlign = align,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Renders system data slides (`jumat_*`, `ramadhan_*`, `idulfitri_*`, `iduladha_*`, `online_finance`, `qr_*`)
 * using the exact same UI components as the Fullscreen Simulation screens (`WaktuJumatFullscreen`,
 * `TarawihFullscreen`, `HolidayAndQurbanFullscreen`, `OnlineFinanceFullscreen`, `MasjidQrFullscreen`,
 * `FinanceQrFullscreen`).
 * Returns true if the slide type was handled as a system data slide.
 */
@Composable
fun RenderSystemDataSlideIfApplicable(
    slide: SlideItem,
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
): Boolean {
    val settings = snapshot.settings
    val today = snapshot.schedule.date

    when (slide.type) {
        "online_finance" -> {
            if (isExpanded) {
                OnlineFinanceFullscreen(
                    snapshot = snapshot,
                    assetsRootDir = assetsRootDir,
                    modifier = modifier
                )
            } else {
                OnlineFinanceCardContent(
                    snapshot = snapshot,
                    isExpanded = false,
                    modifier = modifier.fillMaxSize()
                )
            }
            return true
        }
        "qr_infaq" -> {
            MasjidQrFullscreen(
                snapshot = snapshot,
                assetsRootDir = assetsRootDir,
                isCompactSlide = !isExpanded,
                modifier = modifier
            )
            return true
        }
        "qr_finance" -> {
            FinanceQrFullscreen(
                snapshot = snapshot,
                assetsRootDir = assetsRootDir,
                isCompactSlide = !isExpanded,
                modifier = modifier
            )
            return true
        }
        "jumat_petugas",
        "jumat_keuangan",
        "ramadhan_petugas",
        "ramadhan_keuangan",
        "ramadhan_takjil",
        "ramadhan_jadwal",
        "ramadhan_laporan_akhir",
        "idulfitri_petugas",
        "iduladha_petugas",
        "iduladha_qurban" -> {
            SystemDataSlideContainer(
                snapshot = snapshot,
                assetsRootDir = assetsRootDir,
                isExpanded = isExpanded,
                modifier = modifier
            ) {
                when (slide.type) {
                    "jumat_petugas" -> {
                        val effDate = JwsStateMachine.findEffectivePetugasJumatDate(settings.jumat.petugas.records, today)
                        val rec = effDate?.let { settings.jumat.petugas.records[it] } ?: JumatPetugasRecord()
                        JumatPetugasSlideView(
                            fridayDate = effDate ?: JwsStateMachine.snapToNearestFriday(today).toString(),
                            record = rec,
                            isRowLayout = true,
                            isExpanded = isExpanded
                        )
                    }
                    "jumat_keuangan" -> {
                        val effDate = JwsStateMachine.findEffectiveJumatKeuanganDate(settings.jumat.keuangan.records, today)
                        val rec = effDate?.let { settings.jumat.keuangan.records[it] }
                            ?: JumatKeuanganRecord(saldoKasLalu = settings.jumat.keuangan.saldoKasLalu)
                        JumatKeuanganSlideView(
                            fridayDate = effDate ?: today.toString(),
                            record = rec,
                            isExpanded = isExpanded
                        )
                    }
                    "ramadhan_petugas" -> {
                        val night = settings.ramadhan.currentNight.coerceIn(1, 30)
                        val petugas = settings.ramadhan.petugas.getOrNull(night - 1)
                        TarawihPetugasSlideView(
                            night = night,
                            hijriYear = settings.ramadhan.hijriYear,
                            penceramah = petugas?.penceramah ?: "-",
                            imamTarawih = petugas?.imamTarawih ?: "-",
                            imamWitir = petugas?.imamWitir ?: "-",
                            isExpanded = isExpanded
                        )
                    }
                    "ramadhan_keuangan" -> {
                        val night = settings.ramadhan.currentNight.coerceIn(1, 30)
                        val keuangan = settings.ramadhan.keuangan.getOrNull(night - 1)
                        TarawihKeuanganSlideView(
                            night = night,
                            hijriYear = settings.ramadhan.hijriYear,
                            penerimaanPria = keuangan?.penerimaanPria ?: 0L,
                            penerimaanWanita = keuangan?.penerimaanWanita ?: 0L,
                            jumlahInfaqMalamIni = keuangan?.totalPenerimaan ?: 0L,
                            saldoAkhir = keuangan?.saldoAkhir ?: 0L,
                            isExpanded = isExpanded
                        )
                    }
                    "ramadhan_takjil" -> {
                        val night = settings.ramadhan.currentNight.coerceIn(1, 30)
                        val donors = settings.ramadhan.takjil[night.toString()].orEmpty()
                        val takjilPages = remember(donors) {
                            if (donors.isEmpty()) listOf(emptyList()) else donors.chunked(10)
                        }
                        var pageIdx by remember(donors) { mutableIntStateOf(0) }
                        LaunchedEffect(takjilPages.size) {
                            if (takjilPages.size <= 1) return@LaunchedEffect
                            while (true) {
                                delay(6_000L)
                                pageIdx = (pageIdx + 1) % takjilPages.size
                            }
                        }
                        val safePage = pageIdx.coerceIn(0, takjilPages.lastIndex)
                        TarawihTakjilSlideView(
                            night = night,
                            hijriYear = settings.ramadhan.hijriYear,
                            donors = takjilPages[safePage],
                            startNumber = safePage * 10 + 1,
                            pageNumber = safePage + 1,
                            totalTakjilPages = takjilPages.size,
                            isExpanded = isExpanded
                        )
                    }
                    "ramadhan_jadwal" -> {
                        RamadhanJadwalSlideView(
                            ramadhan = settings.ramadhan,
                            schedule = snapshot.schedule,
                            isExpanded = isExpanded
                        )
                    }
                    "ramadhan_laporan_akhir" -> {
                        IdulFitriKeuanganSlideView(
                            hijriYear = settings.idulFitri.hijriYear,
                            brandName = settings.brand.name,
                            ramadhan = settings.ramadhan,
                            fallbackEidKeuangan = settings.idulFitri.keuangan,
                            isExpanded = isExpanded
                        )
                    }
                    "idulfitri_petugas" -> {
                        val p = settings.idulFitri.petugas
                        EidPetugasSlideView(
                            title = "PETUGAS SHOLAT HARI RAYA IDUL FITRI",
                            subtitle = "1 Syawal ${settings.idulFitri.hijriYear} H • ${settings.brand.name}",
                            khotib = p.khotib.ifBlank { "-" },
                            imam = p.imam.ifBlank { "-" },
                            bilal = p.bilal.ifBlank { "-" },
                            isExpanded = isExpanded
                        )
                    }
                    "iduladha_petugas" -> {
                        val p = settings.idulAdha.petugas
                        EidPetugasSlideView(
                            title = "PETUGAS SHOLAT HARI RAYA IDUL ADHA",
                            subtitle = "10 Dzulhijjah ${settings.idulAdha.hijriYear} H • ${settings.brand.name}",
                            khotib = p.khotib.ifBlank { "-" },
                            imam = p.imam.ifBlank { "-" },
                            bilal = p.bilal.ifBlank { "-" },
                            isExpanded = isExpanded
                        )
                    }
                    "iduladha_qurban" -> {
                        val groups = settings.idulAdha.qurbanGroups.ifEmpty {
                            listOf(
                                QurbanGroup(
                                    id = "q-default",
                                    animalType = "Sapi",
                                    groupName = "Kelompok Qurban 1",
                                    members = listOf("Belum ada data peserta Qurban — atur di Pengaturan Acara → Idul Adha & Qurban")
                                )
                            )
                        }
                        var groupIdx by remember(groups) { mutableIntStateOf(0) }
                        LaunchedEffect(groups.size) {
                            if (groups.size <= 1) return@LaunchedEffect
                            while (true) {
                                delay(6_000L)
                                groupIdx = (groupIdx + 1) % groups.size
                            }
                        }
                        val safeIdx = groupIdx.coerceIn(0, groups.lastIndex)
                        QurbanGroupSlideView(
                            group = groups[safeIdx],
                            groupNumber = safeIdx + 1,
                            totalGroups = groups.size,
                            isExpanded = isExpanded
                        )
                    }
                }
            }
            return true
        }
        else -> return false
    }
}

/**
 * Wraps card-based system slides with the full Simulation header (`HeaderBar` + `HeaderClockCard` + `bgCard`)
 * when rendered in Fullscreen Slideshow (`isExpanded = true`), and renders directly inside `MainSlideContainer`
 * when `isExpanded = false` (since `StandbyScreen` already renders the header & card container).
 */
@Composable
private fun SystemDataSlideContainer(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!isExpanded) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
        return
    }

    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.bgPrimary, palette.bgSecondary)))
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderBar(
                now = snapshot.now,
                brand = snapshot.settings.brand,
                hijriDate = snapshot.hijriDate,
                assetsRootDir = assetsRootDir,
                isReversed = false,
                isLogoMenuOpen = false,
                onLogoClick = {},
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )
            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun CustomSlideView(
    slide: SlideItem,
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?
) {
    val settings = snapshot.settings
    val brand = settings.brand

    if (slide.type == "welcome") {
        WelcomeMasjidSlideView(
            slide = slide,
            brand = brand,
            isSmall = true,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (slide.type == "analog_clock") {
        BigAnalogClock(
            now = snapshot.now,
            isSmall = true,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (RenderSystemDataSlideIfApplicable(
            slide = slide,
            snapshot = snapshot,
            assetsRootDir = assetsRootDir,
            isExpanded = false
        )
    ) {
        return
    }

    if (slide.type == "text" && slide.imageUrl.isBlank()) {
        StyledTextSlideView(slide = slide, modifier = Modifier.fillMaxSize())
        return
    }

    val videoUri: Uri? = remember(slide.videoUrl, assetsRootDir) {
        if (slide.videoUrl.isBlank()) {
            null
        } else if (slide.videoUrl.startsWith("http://") ||
            slide.videoUrl.startsWith("https://") ||
            slide.videoUrl.startsWith("file://") ||
            slide.videoUrl.startsWith("content://")
        ) {
            Uri.parse(slide.videoUrl)
        } else if (assetsRootDir != null) {
            val clean = slide.videoUrl.removePrefix("jws-assets/").removePrefix("/")
            val resolvedFile = File(assetsRootDir, clean).takeIf { it.exists() }
                ?: File(slide.videoUrl).takeIf { it.exists() }
            resolvedFile?.let { Uri.fromFile(it) }
        } else {
            null
        }
    }

    val imageModel: Any? = remember(slide.imageUrl, assetsRootDir) {
        if (slide.imageUrl.isBlank()) {
            null
        } else if (slide.imageUrl.startsWith("http://") || slide.imageUrl.startsWith("https://") || slide.imageUrl.startsWith("file://") || slide.imageUrl.startsWith("content://")) {
            slide.imageUrl
        } else if (assetsRootDir != null) {
            val clean = slide.imageUrl.removePrefix("jws-assets/").removePrefix("/")
            File(assetsRootDir, clean).takeIf { it.exists() } ?: File(slide.imageUrl).takeIf { it.exists() }
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (slide.type == "video" && videoUri != null) {
            LocalSlideVideoPlayer(
                videoUri = videoUri,
                modifier = Modifier.fillMaxSize()
            )
        } else if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Reusable silent-looping Media3 ExoPlayer surface for local `jws-assets/videos/` slide playback
 * across both Main Standby and Fullscreen Standby Slideshow.
 */
@OptIn(UnstableApi::class)
@Composable
fun LocalSlideVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPerformanceMode = LocalPerformanceMode.current
    val exoPlayer = remember(videoUri, isPerformanceMode) {
        val builder = ExoPlayer.Builder(context)
        if (isPerformanceMode) {
            val trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSize(1280, 720)
                        .setForceLowestBitrate(false)
                )
            }
            builder.setTrackSelector(trackSelector)
        }
        builder.build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exoPlayer
            }
        },
        modifier = modifier
    )
}

@Composable
fun JumatPetugasSlideView(
    fridayDate: String,
    record: JumatPetugasRecord,
    isRowLayout: Boolean = true,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 36.dp,
                vertical = if (isExpanded) 28.dp else 22.dp
            ),
        verticalArrangement = if (isRowLayout) {
            Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
        } else {
            Arrangement.SpaceBetween
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(if (isExpanded) 44.dp else 36.dp)
                )
                Column {
                    Text(
                        text = "PETUGAS IBADAH SHOLAT JUM'AT",
                        color = palette.textPrimary,
                        fontSize = if (isExpanded) 38.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Jadwal Efektif Hari Jum'at: $fridayDate",
                        color = palette.accent,
                        fontSize = if (isExpanded) 24.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (isRowLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
            ) {
                OfficerRoleRowCard(
                    roleTitle = "KHOTIB JUM'AT",
                    officerName = record.khotib,
                    accentColor = palette.accent,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleRowCard(
                    roleTitle = "IMAM SHOLAT",
                    officerName = record.imam,
                    accentColor = palette.accentSecondary,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleRowCard(
                    roleTitle = "MUADZIN / BILAL",
                    officerName = record.muadzin,
                    accentColor = palette.accent,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OfficerRoleCard(
                    roleTitle = "KHOTIB JUM'AT",
                    officerName = record.khotib,
                    accentColor = palette.accent,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleCard(
                    roleTitle = "IMAM SHOLAT",
                    officerName = record.imam,
                    accentColor = palette.accentSecondary,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleCard(
                    roleTitle = "MUADZIN / BILAL",
                    officerName = record.muadzin,
                    accentColor = palette.accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun OfficerRoleRowCard(
    roleTitle: String,
    officerName: String,
    accentColor: Color,
    isExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val displayName = officerName.ifBlank { "-" }
    val isLongName = displayName.length > 30
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(if (isExpanded) 20.dp else 16.dp))
            .background(palette.bgPrimary.copy(alpha = 0.65f))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(if (isExpanded) 20.dp else 16.dp))
            .padding(
                horizontal = if (isExpanded) 32.dp else 24.dp,
                vertical = if (isExpanded) 12.dp else 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 28.dp else 20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(if (isExpanded) 460.dp else 360.dp)
                .clip(RoundedCornerShape(if (isExpanded) 14.dp else 12.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .padding(
                    horizontal = if (isExpanded) 20.dp else 16.dp,
                    vertical = if (isExpanded) 14.dp else 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = roleTitle,
                color = accentColor,
                fontSize = if (isExpanded) 32.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = displayName,
            color = palette.textPrimary,
            fontSize = if (isExpanded) {
                if (isLongName) 60.sp else 84.sp
            } else {
                if (isLongName) 42.sp else 58.sp
            },
            lineHeight = if (isExpanded) {
                if (isLongName) 66.sp else 90.sp
            } else {
                if (isLongName) 48.sp else 64.sp
            },
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Start,
            maxLines = if (isLongName) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OfficerRoleCard(
    roleTitle: String,
    officerName: String,
    accentColor: Color,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgPrimary.copy(alpha = 0.65f))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = roleTitle,
                color = accentColor,
                fontSize = if (isExpanded) 32.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = officerName.ifBlank { "-" },
            color = palette.textPrimary,
            fontSize = if (isExpanded) 48.sp else 40.sp,
            lineHeight = if (isExpanded) 56.sp else 48.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun JumatKeuanganSlideView(
    fridayDate: String,
    record: JumatKeuanganRecord,
    isExpanded: Boolean = false
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 38.dp)
            )
            Column {
                Text(
                    text = "LAPORAN KEUANGAN KAS MASJID (JUM'AT)",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 44.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Periode Laporan: $fridayDate",
                    color = palette.textSecondary,
                    fontSize = if (isExpanded) 28.sp else 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
            ) {
                FinanceMetricBox(
                    label = "SALDO KAS LALU",
                    amount = record.saldoKasLalu,
                    valueColor = palette.textPrimary,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL PEMASUKAN (+)",
                    amount = record.pemasukan,
                    valueColor = if (palette.isLight) Color(0xFF059669) else Color(0xFF34D399),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
            ) {
                FinanceMetricBox(
                    label = "TOTAL PENGELUARAN (-)",
                    amount = record.pengeluaran,
                    valueColor = if (palette.isLight) Color(0xFFDC2626) else Color(0xFFF87171),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "SALDO AKHIR KAS MASJID",
                    amount = record.saldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isExpanded) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RamadhanLaporanAkhirSlideView(
    ramadhan: RamadhanSettings,
    brandName: String,
    isExpanded: Boolean = false
) {
    val palette = LocalJwsTheme.current

    val hasNightlyData = remember(ramadhan.keuangan) {
        ramadhan.keuangan.any {
            it.saldoAwal != 0L || it.penerimaanPria != 0L || it.penerimaanWanita != 0L || it.pengeluaran != 0L
        }
    }
    val (saldoAwal, totalPemasukan, totalPengeluaran, totalSaldoAkhir) = remember(ramadhan.keuangan, ramadhan.laporanAkhir, hasNightlyData) {
        if (hasNightlyData) {
            val awal = ramadhan.keuangan.firstOrNull { it.saldoAwal != 0L }?.saldoAwal ?: 0L
            val masuk = ramadhan.keuangan.sumOf { it.totalPenerimaan }
            val keluar = ramadhan.keuangan.sumOf { it.pengeluaran }
            listOf(awal, masuk, keluar, awal + masuk - keluar)
        } else {
            val lap = ramadhan.laporanAkhir
            listOf(0L, lap.totalPenerimaan, lap.totalPengeluaran, lap.saldoAkhir)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 38.dp)
            )
            Column {
                Text(
                    text = "LAPORAN KEUANGAN AKHIR RAMADHAN",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 44.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ramadhan ${ramadhan.hijriYear} H • $brandName",
                    color = palette.textSecondary,
                    fontSize = if (isExpanded) 28.sp else 24.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
            ) {
                FinanceMetricBox(
                    label = "SALDO AWAL RAMADHAN",
                    amount = saldoAwal,
                    valueColor = palette.textPrimary,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL PEMASUKAN RAMADHAN (+)",
                    amount = totalPemasukan,
                    valueColor = if (palette.isLight) Color(0xFF059669) else Color(0xFF34D399),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 16.dp)
            ) {
                FinanceMetricBox(
                    label = "TOTAL PENGELUARAN RAMADHAN (-)",
                    amount = totalPengeluaran,
                    valueColor = if (palette.isLight) Color(0xFFDC2626) else Color(0xFFF87171),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL SALDO AKHIR RAMADHAN",
                    amount = totalSaldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!isExpanded) {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FinanceMetricBox(
    label: String,
    amount: Long,
    valueColor: Color,
    isHighlighted: Boolean = false,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }
    val formattedAmount = rupiahFormat.format(amount)
    val expandedAmountSize = when {
        formattedAmount.length <= 12 -> 112.sp
        formattedAmount.length <= 14 -> 96.sp
        else -> 80.sp
    }
    val compactAmountSize = when {
        formattedAmount.length <= 12 -> 64.sp
        formattedAmount.length <= 14 -> 54.sp
        else -> 46.sp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isHighlighted) palette.accent.copy(alpha = 0.22f)
                else palette.bgPrimary.copy(alpha = 0.65f)
            )
            .border(
                width = if (isHighlighted) 2.dp else 1.5.dp,
                color = if (isHighlighted) palette.accentSecondary else palette.border,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(
                horizontal = if (isExpanded) 28.dp else 24.dp,
                vertical = if (isExpanded) 16.dp else 18.dp
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = if (isExpanded) 34.sp else 28.sp,
            lineHeight = if (isExpanded) 40.sp else 34.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (isExpanded) 1.2.sp else 0.4.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = formattedAmount,
            color = valueColor,
            fontSize = if (isExpanded) expandedAmountSize else compactAmountSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

@Composable
private fun FloatingMasjidQrBadge(
    masjidQr: MasjidQrSettings,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val qrBitmap = remember(masjidQr.link) {
        generateQrBitmap(masjidQr.link.ifBlank { "https://masjidalgaffar.id/infaq" }, 220)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgPrimary.copy(alpha = 0.9f))
            .border(2.dp, palette.accentSecondary, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = masjidQr.label,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(4.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(64.dp)
            )
        }

        Column(modifier = Modifier.width(170.dp)) {
            Text(
                text = "INFAQ & QRIS MASJID",
                color = palette.accentSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = masjidQr.label,
                color = palette.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}
