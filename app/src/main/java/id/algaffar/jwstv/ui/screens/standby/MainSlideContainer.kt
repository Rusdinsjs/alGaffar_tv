package id.algaffar.jwstv.ui.screens.standby

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
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
import id.algaffar.jwstv.domain.model.RamadhanSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.ui.components.clock.BigAnalogClock
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
    now: ZonedDateTime,
    today: LocalDate,
    settings: AppStateSettings,
    assetsRootDir: File?,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    val deck: List<MainDeckSlide> = remember(today, settings) {
        buildList {
            // 1. Configured & Default System Slides
            val configuredMainSlides = settings.slides.filter { it.enabled && it.showInMain }
            val configuredTypes = configuredMainSlides.map { it.type }.toSet()
            configuredMainSlides.forEach { slide ->
                add(MainDeckSlide.CustomSlide(slide))
            }

            // 2. Ramadhan Overview slide when Ramadhan mode is active and no explicit ramadhan slide added
            val hasExplicitRamadhanSlide = configuredTypes.any { it.startsWith("ramadhan_") }
            if (settings.ramadhan.isRamadhan && !hasExplicitRamadhanSlide) {
                add(
                    MainDeckSlide.RamadhanOverview(
                        ramadhan = settings.ramadhan,
                        durationSeconds = settings.ramadhan.slideshow.slideDuration
                    )
                )
            }

            // 4. Fallback so main container is never blank if user disabled all slides
            if (isEmpty()) {
                SlideItem.defaultSystemSlides().forEach { add(MainDeckSlide.CustomSlide(it)) }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.mod(deck.size.coerceAtLeast(1))
    val activeSlide = deck[safeIndex]

    val isSystemDefaultSlide = activeSlide is MainDeckSlide.CustomSlide &&
        (activeSlide.item.type == "welcome" || activeSlide.item.type == "analog_clock")

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
                    settings = settings,
                    today = today,
                    now = now,
                    assetsRootDir = assetsRootDir
                )
                is MainDeckSlide.JumatPetugas -> JumatPetugasSlideView(
                    fridayDate = slide.fridayDate,
                    record = slide.record
                )
                is MainDeckSlide.JumatKeuangan -> JumatKeuanganSlideView(
                    fridayDate = slide.fridayDate,
                    record = slide.record
                )
                is MainDeckSlide.RamadhanOverview -> RamadhanOverviewSlideView(
                    ramadhan = slide.ramadhan
                )
            }
        }

        // Floating Masjid QR Infaq Badge (Bottom-Right)
        if (settings.masjidQr.enabled && settings.masjidQr.showFloatingOnMain) {
            FloatingMasjidQrBadge(
                masjidQr = settings.masjidQr,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }

        // Floating Overlay Running Text (hidden on default Greeting & Analog Clock slides, matching Slide/index.tsx)
        if (!isSystemDefaultSlide) {
            OverlayRunningText(
                messages = settings.overlayTextList,
                visibleDurationSeconds = settings.overlayVisibleDuration,
                hiddenDurationSeconds = settings.overlayHiddenDuration,
                speed = settings.overlaySpeed,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

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
 * Renders system data slides (`jumat_*`, `ramadhan_*`, `idulfitri_*`, `iduladha_*`, `online_finance`, `qr_*`).
 * Returns true if the slide type was handled as a system data slide.
 */
@Composable
fun RenderSystemDataSlideIfApplicable(
    slide: SlideItem,
    settings: AppStateSettings,
    today: LocalDate,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
): Boolean {
    val palette = LocalJwsTheme.current
    val rupiah = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    when (slide.type) {
        "jumat_petugas" -> {
            val effDate = JwsStateMachine.findEffectivePetugasJumatDate(settings.jumat.petugas.records, today)
            val rec = effDate?.let { settings.jumat.petugas.records[it] } ?: JumatPetugasRecord()
            JumatPetugasSlideView(
                fridayDate = effDate ?: JwsStateMachine.snapToNearestFriday(today).toString(),
                record = rec
            )
            return true
        }
        "jumat_keuangan" -> {
            val effDate = JwsStateMachine.findEffectiveJumatKeuanganDate(settings.jumat.keuangan.records, today)
            val rec = effDate?.let { settings.jumat.keuangan.records[it] }
                ?: JumatKeuanganRecord(saldoKasLalu = settings.jumat.keuangan.saldoKasLalu)
            JumatKeuanganSlideView(fridayDate = effDate ?: today.toString(), record = rec, isExpanded = isExpanded)
            return true
        }
        "qr_jumat" -> {
            val effDate = JwsStateMachine.findEffectiveJumatKeuanganDate(settings.jumat.keuangan.records, today) ?: today.toString()
            val rec = settings.jumat.keuangan.records[effDate] ?: JumatKeuanganRecord(saldoKasLalu = settings.jumat.keuangan.saldoKasLalu)
            val payload = if (settings.jumat.keuangan.qrType == "custom_url" && settings.jumat.keuangan.qrCustomUrl.isNotBlank()) {
                settings.jumat.keuangan.qrCustomUrl
            } else {
                "${settings.jumat.keuangan.customTitle}\n${settings.brand.name}\nTgl: $effDate\nSaldo Akhir: ${rupiah.format(rec.saldoAkhir)}"
            }
            CompactSystemQrSlideCard(
                badge = "QR KAS JUM'AT",
                title = settings.jumat.keuangan.customTitle.ifBlank { "Laporan Infaq Kas Jum'at" },
                subtitle = "Periode $effDate • Saldo Akhir: ${rupiah.format(rec.saldoAkhir)}",
                qrPayload = payload,
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "ramadhan_petugas", "ramadhan_keuangan", "ramadhan_jadwal" -> {
            RamadhanOverviewSlideView(ramadhan = settings.ramadhan, isExpanded = isExpanded)
            return true
        }
        "ramadhan_takjil" -> {
            val night = settings.ramadhan.currentNight.coerceIn(1, 30)
            val donors = settings.ramadhan.takjil[night.toString()].orEmpty()
            CompactSystemInfoSlideCard(
                badge = "RAMADHAN HARI KE-$night",
                title = "DAFTAR PENYUMBANG TA'JIL BUKA PUASA",
                rows = listOf(
                    "Donatur Hari Ini" to if (donors.isEmpty()) "Jazakumullahu Khairan kepada seluruh Muhsinin" else donors.joinToString(" • ")
                ),
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "ramadhan_laporan_akhir" -> {
            RamadhanLaporanAkhirSlideView(
                ramadhan = settings.ramadhan,
                brandName = settings.brand.name,
                isExpanded = isExpanded
            )
            return true
        }
        "idulfitri_petugas" -> {
            val p = settings.idulFitri.petugas
            CompactSystemInfoSlideCard(
                badge = "IDUL FITRI ${settings.idulFitri.hijriYear} H",
                title = "PETUGAS SHOLAT HARI RAYA IDUL FITRI",
                rows = listOf("Khotib" to p.khotib, "Imam" to p.imam, "Bilal" to p.bilal),
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "iduladha_petugas" -> {
            val p = settings.idulAdha.petugas
            CompactSystemInfoSlideCard(
                badge = "IDUL ADHA ${settings.idulAdha.hijriYear} H",
                title = "PETUGAS SHOLAT HARI RAYA IDUL ADHA",
                rows = listOf("Khotib" to p.khotib, "Imam" to p.imam, "Bilal" to p.bilal),
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "iduladha_qurban" -> {
            val groups = settings.idulAdha.qurbanGroups
            val summaryRows = if (groups.isEmpty()) {
                listOf("Status Qurban" to "Pendaftaran & Data Qurban Masjid ${settings.brand.name}")
            } else {
                groups.take(4).map { g ->
                    "${g.groupName} (${g.animalType})" to g.members.joinToString(", ").ifBlank { "${g.members.size} Peserta" }
                }
            }
            CompactSystemInfoSlideCard(
                badge = "IBADAH QURBAN ${settings.idulAdha.hijriYear} H",
                title = "DAFTAR PESERTA & KELOMPOK QURBAN",
                rows = summaryRows,
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "online_finance" -> {
            val fin = settings.onlineFinance
            CompactSystemInfoSlideCard(
                badge = "TRANSPARANSI KAS ONLINE",
                title = "KAS UMUM ${settings.brand.name.uppercase()}",
                rows = listOf(
                    "Rekening Infaq" to listOf(fin.bankName, fin.nomorRekening).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Lihat Laporan Kas Online" },
                    "Atas Nama" to fin.atasNama.ifBlank { settings.brand.name }
                ),
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "qr_infaq" -> {
            val qr = settings.masjidQr
            CompactSystemQrSlideCard(
                badge = "INFAQ & SEDEKAH DIGITAL",
                title = qr.label.ifBlank { "Infaq Digital ${settings.brand.name}" },
                subtitle = listOf(qr.bankName, qr.nomorRekening, qr.atasNama).filter { it.isNotBlank() }.joinToString(" • ")
                    .ifBlank { "Pindai QR Code menggunakan aplikasi pembayaran / m-banking Anda" },
                qrPayload = qr.link.ifBlank { "Infaq Digital ${settings.brand.name}" },
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        "qr_finance" -> {
            val fin = settings.onlineFinance
            val payload = fin.reportUrl.ifBlank { fin.sheetUrl }.ifBlank { settings.masjidQr.link }
            CompactSystemQrSlideCard(
                badge = "QR LAPORAN KAS ONLINE",
                title = "TRANSPARANSI KEUANGAN ${settings.brand.name.uppercase()}",
                subtitle = "Pindai QR Code untuk melihat rincian mutasi kas masjid secara real-time",
                qrPayload = payload.ifBlank { "Laporan Kas ${settings.brand.name}" },
                isExpanded = isExpanded,
                modifier = modifier
            )
            return true
        }
        else -> return false
    }
}

@Composable
private fun CompactSystemInfoSlideCard(
    badge: String,
    title: String,
    rows: List<Pair<String, String>>,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.bgPrimary, palette.bgSecondary)))
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 36.dp else 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = badge,
            color = palette.accent,
            fontSize = if (isExpanded) 34.sp else 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = palette.textPrimary,
            fontSize = if (isExpanded) 56.sp else 48.sp,
            lineHeight = if (isExpanded) 64.sp else 56.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(if (isExpanded) 28.dp else 20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(0.92f),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 18.dp else 14.dp)
        ) {
            rows.forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.bgCard)
                        .border(1.5.dp, palette.border, RoundedCornerShape(16.dp))
                        .padding(
                            horizontal = if (isExpanded) 32.dp else 26.dp,
                            vertical = if (isExpanded) 22.dp else 18.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = k,
                        color = palette.accentSecondary,
                        fontSize = if (isExpanded) 36.sp else 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = v.ifBlank { "-" },
                        color = palette.textPrimary,
                        fontSize = if (isExpanded) 46.sp else 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSystemQrSlideCard(
    badge: String,
    title: String,
    subtitle: String,
    qrPayload: String,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val qrBmp = remember(qrPayload) { generateQrBitmap(qrPayload, 360) }
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.bgPrimary, palette.bgSecondary)))
            .padding(if (isExpanded) 56.dp else 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 48.dp else 36.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = badge,
                color = palette.accent,
                fontSize = if (isExpanded) 34.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = title,
                color = palette.textPrimary,
                fontSize = if (isExpanded) 62.sp else 52.sp,
                lineHeight = if (isExpanded) 70.sp else 60.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = subtitle,
                color = palette.textSecondary,
                fontSize = if (isExpanded) 34.sp else 28.sp,
                lineHeight = if (isExpanded) 44.sp else 38.sp
            )
        }
        Box(
            modifier = Modifier
                .size(if (isExpanded) 400.dp else 320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(3.dp, palette.accent, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBmp != null) {
                Image(bitmap = qrBmp.asImageBitmap(), contentDescription = title, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black, modifier = Modifier.size(160.dp))
            }
        }
    }
}

@Composable
private fun CustomSlideView(
    slide: SlideItem,
    settings: AppStateSettings,
    today: LocalDate,
    now: ZonedDateTime,
    assetsRootDir: File?
) {
    val palette = LocalJwsTheme.current
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
            now = now,
            isSmall = true,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (RenderSystemDataSlideIfApplicable(slide = slide, settings = settings, today = today)) {
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

        if (slide.title.isNotBlank() || slide.content.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, palette.bgPrimary.copy(alpha = 0.92f))
                        )
                    )
                    .padding(horizontal = 36.dp, vertical = 32.dp)
            ) {
                if (slide.title.isNotBlank()) {
                    Text(
                        text = slide.title,
                        color = palette.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (slide.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = slide.content,
                        color = palette.textSecondary,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    )
                }
            }
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
    isRowLayout: Boolean = false
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isRowLayout) 28.dp else 36.dp),
        verticalArrangement = if (isRowLayout) Arrangement.spacedBy(20.dp) else Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isRowLayout) 16.dp else 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(if (isRowLayout) 44.dp else 34.dp)
                )
                Column {
                    Text(
                        text = "PETUGAS IBADAH SHOLAT JUM'AT",
                        color = palette.textPrimary,
                        fontSize = if (isRowLayout) 44.sp else 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Jadwal Efektif Hari Jum'at: $fridayDate",
                        color = palette.accent,
                        fontSize = if (isRowLayout) 28.sp else 22.sp,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OfficerRoleRowCard(
                    roleTitle = "KHOTIB JUM'AT",
                    officerName = record.khotib,
                    accentColor = palette.accent,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleRowCard(
                    roleTitle = "IMAM SHOLAT",
                    officerName = record.imam,
                    accentColor = palette.accentSecondary,
                    modifier = Modifier.weight(1f)
                )
                OfficerRoleRowCard(
                    roleTitle = "MUADZIN / BILAL",
                    officerName = record.muadzin,
                    accentColor = palette.accent,
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
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val displayName = officerName.ifBlank { "-" }
    val isLongName = displayName.length > 30
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgPrimary.copy(alpha = 0.65f))
            .border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = roleTitle,
                color = accentColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = displayName,
            color = palette.textPrimary,
            fontSize = if (isLongName) 60.sp else 84.sp,
            lineHeight = if (isLongName) 66.sp else 90.sp,
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
private fun RamadhanOverviewSlideView(
    ramadhan: RamadhanSettings,
    isExpanded: Boolean = false
) {
    val palette = LocalJwsTheme.current
    val nightIndex = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val petugasTonight = ramadhan.petugas.getOrNull(nightIndex)
    val donorsTonight = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()

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
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 38.dp)
            )
            Column {
                Text(
                    text = "INFORMASI IBADAH RAMADHAN — MALAM KE-${ramadhan.currentNight}",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 44.sp else 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Petugas Sholat Tarawih, Kultum & Daftar Penyumbang Ta'jil",
                    color = palette.accent,
                    fontSize = if (isExpanded) 28.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OfficerRoleCard(
                roleTitle = "PENCERAMAH KULTUM",
                officerName = petugasTonight?.penceramah ?: "-",
                accentColor = palette.accentSecondary,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleCard(
                roleTitle = "IMAM TARAWIH & WITIR",
                officerName = "${petugasTonight?.imamTarawih ?: "-"} / ${petugasTonight?.imamWitir ?: "-"}",
                accentColor = palette.accent,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleCard(
                roleTitle = "DONATUR TA'JIL HARI INI",
                officerName = if (donorsTonight.isEmpty()) "Jamaah Masjid Al-Gaffar" else donorsTonight.joinToString(", "),
                accentColor = palette.accentSecondary,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
        }
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
