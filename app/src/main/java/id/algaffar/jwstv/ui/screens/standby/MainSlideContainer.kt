package id.algaffar.jwstv.ui.screens.standby

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.MasjidQrSettings
import id.algaffar.jwstv.domain.model.RamadhanSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private sealed interface MainDeckSlide {
    val durationSeconds: Int

    data class WelcomeHero(
        val mosqueName: String,
        val mosqueAddress: String,
        override val durationSeconds: Int = 12
    ) : MainDeckSlide

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

/**
 * Main Slide Showcase Container (occupies the 75% main visual area on `StandbyScreen`).
 * Automatically rotates user slides, Friday Petugas & Finance reports, Ramadhan cards,
 * and overlays the Floating Masjid QR Infaq badge when enabled.
 * Mirrors `src/components/Screens/Main/Slide/index.tsx`.
 */
@Composable
fun MainSlideContainer(
    today: LocalDate,
    settings: AppStateSettings,
    assetsRootDir: File?,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    val deck: List<MainDeckSlide> = remember(today, settings) {
        buildList {
            // 1. Always include the Mosque Welcome & Verse Hero Slide
            add(
                MainDeckSlide.WelcomeHero(
                    mosqueName = settings.brand.name,
                    mosqueAddress = settings.brand.address
                )
            )

            // 2. User custom slides marked for Main screen
            settings.slides.filter { it.enabled && it.showInMain }.forEach { slide ->
                add(MainDeckSlide.CustomSlide(slide))
            }

            // 3. Friday Petugas & Keuangan slides (if enabled and records exist)
            if (settings.jumat.enabled) {
                if (settings.jumat.slideshow.showPetugas) {
                    val effPetugasDate = JwsStateMachine.findEffectivePetugasJumatDate(
                        settings.jumat.petugas.records,
                        today
                    )
                    val rec = effPetugasDate?.let { settings.jumat.petugas.records[it] }
                        ?: JumatPetugasRecord(
                            khotib = "Ust. H. Ahmad Fauzi, Lc., M.Ag.",
                            imam = "Ust. Muhammad Ridwan, Al-Hafizh",
                            muadzin = "Sdr. Abdullah Al-Gaffar"
                        )
                    add(
                        MainDeckSlide.JumatPetugas(
                            fridayDate = effPetugasDate
                                ?: JwsStateMachine.snapToNearestFriday(today).toString(),
                            record = rec,
                            durationSeconds = settings.jumat.slideshow.slideDuration
                        )
                    )
                }

                if (settings.jumat.slideshow.showKeuangan) {
                    val effKeuDate = JwsStateMachine.findEffectiveJumatKeuanganDate(
                        settings.jumat.keuangan.records,
                        today
                    )
                    val keuRec = effKeuDate?.let { settings.jumat.keuangan.records[it] }
                        ?: JumatKeuanganRecord(
                            saldoKasLalu = settings.jumat.keuangan.saldoKasLalu.takeIf { it > 0 } ?: 15_450_000L,
                            pemasukan = 4_850_000L,
                            pengeluaran = 1_650_000L
                        )
                    add(
                        MainDeckSlide.JumatKeuangan(
                            fridayDate = effKeuDate ?: today.toString(),
                            record = keuRec,
                            durationSeconds = settings.jumat.slideshow.slideDuration
                        )
                    )
                }
            }

            // 4. Ramadhan Overview slide when Ramadhan mode is active
            if (settings.ramadhan.isRamadhan) {
                add(
                    MainDeckSlide.RamadhanOverview(
                        ramadhan = settings.ramadhan,
                        durationSeconds = settings.ramadhan.slideshow.slideDuration
                    )
                )
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.mod(deck.size.coerceAtLeast(1))
    val activeSlide = deck[safeIndex]

    LaunchedEffect(safeIndex, activeSlide.durationSeconds, deck.size) {
        delay(activeSlide.durationSeconds.coerceIn(4, 120) * 1000L)
        currentIndex = (safeIndex + 1) % deck.size
    }

    Box(
        modifier = modifier
            .size(width = 1440.dp, height = 720.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
    ) {
        AnimatedContent(
            targetState = activeSlide,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(600)) togetherWith fadeOut(tween(450))
            },
            label = "MainDeckSlideTransition"
        ) { slide ->
            when (slide) {
                is MainDeckSlide.WelcomeHero -> WelcomeHeroSlideView(
                    mosqueName = slide.mosqueName,
                    mosqueAddress = slide.mosqueAddress
                )
                is MainDeckSlide.CustomSlide -> CustomSlideView(
                    slide = slide.item,
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

        // Top-Right Slide Navigation & Indicator Pill (Hybrid D-Pad + Touch)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.bgPrimary.copy(alpha = 0.72f))
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .focusable()
                    .clickable {
                        currentIndex = (safeIndex - 1 + deck.size) % deck.size
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Slide Sebelumnya",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                deck.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .width(if (idx == safeIndex) 20.dp else 7.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (idx == safeIndex) palette.accent
                                else palette.textSecondary.copy(alpha = 0.35f)
                            )
                            .clickable { currentIndex = idx }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .focusable()
                    .clickable {
                        currentIndex = (safeIndex + 1) % deck.size
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Slide Berikutnya",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(18.dp)
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

        // Floating Overlay Running Text (Flush at Bottom-Left inside MainSlideContainer)
        OverlayRunningText(
            messages = settings.overlayTextList,
            visibleDurationSeconds = settings.overlayVisibleDuration,
            hiddenDurationSeconds = settings.overlayHiddenDuration,
            speed = settings.overlaySpeed,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun WelcomeHeroSlideView(
    mosqueName: String,
    mosqueAddress: String
) {
    val palette = LocalJwsTheme.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Artistic Vector Mosque Silhouette Horizon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.bgPrimary,
                        palette.bgSecondary,
                        palette.accent.copy(alpha = 0.25f)
                    )
                )
            )

            // Sun/Moon ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentSecondary.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.28f),
                    radius = w * 0.26f
                ),
                radius = w * 0.26f,
                center = Offset(w * 0.78f, h * 0.28f)
            )

            // Mosque Silhouette along bottom horizon
            val silhouetteColor = palette.bgPrimary.copy(alpha = 0.85f)
            val baseHeight = h * 0.16f
            drawRect(
                color = silhouetteColor,
                topLeft = Offset(0f, h - baseHeight),
                size = Size(w, baseHeight)
            )

            // Central Grand Dome
            val mainDomePath = Path().apply {
                moveTo(w * 0.34f, h - baseHeight)
                cubicTo(
                    w * 0.34f, h * 0.56f,
                    w * 0.50f, h * 0.44f,
                    w * 0.50f, h * 0.44f
                )
                cubicTo(
                    w * 0.50f, h * 0.44f,
                    w * 0.66f, h * 0.56f,
                    w * 0.66f, h - baseHeight
                )
                close()
            }
            drawPath(mainDomePath, color = silhouetteColor)

            // Twin Minarets
            val minaretLeft = w * 0.24f
            val minaretRight = w * 0.73f
            listOf(minaretLeft, minaretRight).forEach { mx ->
                drawRect(
                    color = silhouetteColor,
                    topLeft = Offset(mx, h * 0.32f),
                    size = Size(w * 0.03f, h * 0.52f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.accent.copy(alpha = 0.2f))
                    .border(1.dp, palette.accent, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    color = palette.accentSecondary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "“Hanyalah yang memakmurkan masjid-masjid Allah ialah orang-orang yang beriman kepada Allah dan hari kemudian, serta tetap mendirikan sholat, menunaikan zakat dan tidak takut (kepada siapapun) selain kepada Allah.”",
                color = palette.textPrimary,
                fontSize = 26.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "— QS. At-Taubah: 18 • $mosqueName —",
                color = palette.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mosqueAddress,
                color = palette.textSecondary,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun CustomSlideView(
    slide: SlideItem,
    assetsRootDir: File?
) {
    val palette = LocalJwsTheme.current
    val imageModel: Any? = remember(slide.imageUrl, assetsRootDir) {
        if (slide.imageUrl.isBlank()) {
            null
        } else if (slide.imageUrl.startsWith("http://") || slide.imageUrl.startsWith("https://") || slide.imageUrl.startsWith("file://")) {
            slide.imageUrl
        } else if (assetsRootDir != null) {
            val clean = slide.imageUrl.removePrefix("jws-assets/").removePrefix("/")
            File(assetsRootDir, clean).takeIf { it.exists() } ?: File(slide.imageUrl).takeIf { it.exists() }
        } else {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (imageModel != null) {
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

@Composable
fun JumatPetugasSlideView(
    fridayDate: String,
    record: JumatPetugasRecord
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = "PETUGAS IBADAH SHOLAT JUM'AT",
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Jadwal Efektif Hari Jum'at: $fridayDate",
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

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

@Composable
private fun OfficerRoleCard(
    roleTitle: String,
    officerName: String,
    accentColor: Color,
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
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = roleTitle,
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = officerName.ifBlank { "-" },
            color = palette.textPrimary,
            fontSize = 26.sp,
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
    record: JumatKeuanganRecord
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(34.dp)
            )
            Column {
                Text(
                    text = "LAPORAN KEUANGAN KAS MASJID (JUM'AT)",
                    color = palette.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Periode Laporan: $fridayDate",
                    color = palette.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinanceMetricBox(
                    label = "SALDO KAS LALU",
                    amount = record.saldoKasLalu,
                    valueColor = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL PEMASUKAN (+)",
                    amount = record.pemasukan,
                    valueColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FinanceMetricBox(
                    label = "TOTAL PENGELUARAN (-)",
                    amount = record.pengeluaran,
                    valueColor = Color(0xFFF87171),
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "SALDO AKHIR KAS MASJID",
                    amount = record.saldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun FinanceMetricBox(
    label: String,
    amount: Long,
    valueColor: Color,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
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
            .padding(22.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = rupiahFormat.format(amount),
            color = valueColor,
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RamadhanOverviewSlideView(
    ramadhan: RamadhanSettings
) {
    val palette = LocalJwsTheme.current
    val nightIndex = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val petugasTonight = ramadhan.petugas.getOrNull(nightIndex)
    val donorsTonight = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(34.dp)
            )
            Column {
                Text(
                    text = "INFORMASI IBADAH RAMADHAN — MALAM KE-${ramadhan.currentNight}",
                    color = palette.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Petugas Sholat Tarawih, Kultum & Daftar Penyumbang Ta'jil",
                    color = palette.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OfficerRoleCard(
                roleTitle = "PENCERAMAH KULTUM",
                officerName = petugasTonight?.penceramah ?: "-",
                accentColor = palette.accentSecondary,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleCard(
                roleTitle = "IMAM TARAWIH & WITIR",
                officerName = "${petugasTonight?.imamTarawih ?: "-"} / ${petugasTonight?.imamWitir ?: "-"}",
                accentColor = palette.accent,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleCard(
                roleTitle = "DONATUR TA'JIL HARI INI",
                officerName = if (donorsTonight.isEmpty()) "Jamaah Masjid Al-Gaffar" else donorsTonight.joinToString(", "),
                accentColor = palette.accentSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
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
