package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.domain.model.RamadhanJadwalItem
import id.algaffar.jwstv.domain.model.RamadhanSettings
import id.algaffar.jwstv.domain.model.SlideshowGroup
import id.algaffar.jwstv.domain.model.SlideshowTargetState
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.BigAnalogClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.screens.standby.FinanceMetricBox
import id.algaffar.jwstv.ui.screens.standby.GroupedSlideshowPlayer
import id.algaffar.jwstv.ui.screens.standby.HeaderBar
import id.algaffar.jwstv.ui.screens.standby.HeaderClockCard
import id.algaffar.jwstv.ui.screens.standby.JumatKeuanganSlideView
import id.algaffar.jwstv.ui.screens.standby.JumatPetugasSlideView
import id.algaffar.jwstv.ui.screens.standby.LocalSlideVideoPlayer
import id.algaffar.jwstv.ui.screens.standby.NextPrayerCountdownCard
import id.algaffar.jwstv.ui.screens.standby.OfficerRoleRowCard
import id.algaffar.jwstv.ui.screens.standby.WelcomeMasjidSlideView
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import kotlinx.coroutines.delay
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 1. WAKTU JUM'AT FULLSCREEN (`WaktuJumatContent.tsx`)
 * Rotates Petugas Jum'at, Keuangan Jum'at, and Friday announcements before Khutbah starts.
 */
@Composable
fun WaktuJumatFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val jumat = snapshot.settings.jumat
    val today = snapshot.schedule.date
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)

    val effPetugasDate = remember(jumat.petugas.records, today) {
        JwsStateMachine.findEffectivePetugasJumatDate(jumat.petugas.records, today)
            ?: JwsStateMachine.snapToNearestFriday(today).toString()
    }
    val petugasRec = jumat.petugas.records[effPetugasDate] ?: JumatPetugasRecord(
        khotib = "Ust. Khotib (Atur di Pengaturan)",
        imam = "Ust. Imam (Atur di Pengaturan)",
        muadzin = "Sdr. Muadzin"
    )

    val effKeuDate = remember(jumat.keuangan.records, today) {
        JwsStateMachine.findEffectiveJumatKeuanganDate(jumat.keuangan.records, today)
            ?: today.toString()
    }
    val keuRec = jumat.keuangan.records[effKeuDate] ?: JumatKeuanganRecord(
        saldoKasLalu = jumat.keuangan.saldoKasLalu.takeIf { it > 0 } ?: 15_450_000L,
        pemasukan = 4_850_000L,
        pengeluaran = 1_650_000L
    )

    var slidePage by remember { mutableIntStateOf(0) }
    LaunchedEffect(jumat.slideshow.slideDuration) {
        while (true) {
            delay(jumat.slideshow.slideDuration.coerceIn(5, 60) * 1000L)
            slidePage = (slidePage + 1) % 2
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Standby HeaderBar + Standby-sized HeaderClockCard (480x180dp)
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
                isLogoMenuOpen = isLogoMenuOpen,
                onLogoClick = onLogoClick,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )

            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle
            )
        }

        // Main Rotating Slide Area
        val jumatGroups = remember(snapshot.settings) {
            snapshot.settings.resolveGroupsForState(SlideshowTargetState.JUMAT)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
            if (jumatGroups.isNotEmpty()) {
                GroupedSlideshowPlayer(
                    groups = jumatGroups,
                    snapshot = snapshot,
                    assetsRootDir = assetsRootDir,
                    isExpanded = true,
                    wrapSystemSlideWithHeader = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = slidePage,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                    label = "WaktuJumatSlide"
                ) { page ->
                    if (page == 0) {
                        JumatPetugasSlideView(
                            fridayDate = effPetugasDate,
                            record = petugasRec,
                            isRowLayout = true
                        )
                    } else {
                        JumatKeuanganSlideView(
                            fridayDate = effKeuDate,
                            record = keuRec,
                            isExpanded = true
                        )
                    }
                }
            }
        }

        // Bottom Row: Left = Card Menuju Khutbah, Right = Quoted Reminder Text (2x font size)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgCard)
                    .border(2.dp, palette.accent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent.copy(alpha = 0.18f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Column {
                    Text(
                        text = "MENUJU KHUTBAH",
                        color = palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formattedRem,
                        color = palette.accentSecondary,
                        fontSize = 46.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            val jumatReminder = snapshot.settings.screenTexts.waktuJumatReminder.trim()
            if (jumatReminder.isNotEmpty()) {
                Text(
                    text = "“$jumatReminder”",
                    color = palette.textSecondary,
                    fontSize = 40.sp,
                    lineHeight = 48.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 2. TARAWIH RAMADHAN FULLSCREEN (`TarawihContent.tsx`)
 * Mengacu pada UI/UX `WaktuJumatFullscreen`:
 * - Top Header: `HeaderBar` + `HeaderClockCard`
 * - Main Rotating Slide Area:
 *   - Page 0: Petugas Kultum, Tarawih & Witir (Row cards ala `JumatPetugasSlideView`)
 *   - Page 1: Laporan Infaq Malam Ramadhan (2x2 Grid ala `JumatKeuanganSlideView`, termasuk "Jumlah Infaq Malam Ini")
 *   - Page 2: Daftar Penyumbang Ta'jil Buka Puasa
 * - Bottom Row: Countdown Card (`MENUJU SHOLAT TARAWIH`) + Quoted Reminder Text
 */
@Composable
fun TarawihFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val ramadhan = snapshot.settings.ramadhan
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)

    val nightIdx = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val petugas = ramadhan.petugas.getOrNull(nightIdx)
    val keuangan = ramadhan.keuangan.getOrNull(nightIdx)
    val takjilDonors = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()
    val takjilPages = remember(takjilDonors) {
        if (takjilDonors.isEmpty()) listOf(emptyList()) else takjilDonors.chunked(10)
    }

    val slideKinds: List<Pair<String, Int>> = remember(ramadhan.slideshow, takjilPages.size) {
        buildList {
            if (ramadhan.slideshow.showPetugas) add("petugas" to 0)
            if (ramadhan.slideshow.showKeuangan) add("keuangan" to 0)
            if (ramadhan.slideshow.showJadwal) add("jadwal" to 0)
            if (ramadhan.slideshow.showTakjil) {
                takjilPages.indices.forEach { add("takjil" to it) }
            }
            if (isEmpty()) add("petugas" to 0)
        }
    }
    val totalPages = slideKinds.size

    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(ramadhan.slideshow.slideDuration, totalPages) {
        if (page >= totalPages) page = 0
        while (true) {
            delay(ramadhan.slideshow.slideDuration.coerceIn(5, 60) * 1000L)
            page = (page + 1) % totalPages
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Standby HeaderBar + Standby-sized HeaderClockCard (480x180dp)
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
                isLogoMenuOpen = isLogoMenuOpen,
                onLogoClick = onLogoClick,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )

            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle
            )
        }

        // Main Rotating Slide Area
        val ramadhanGroups = remember(snapshot.settings) {
            snapshot.settings.resolveGroupsForState(SlideshowTargetState.RAMADHAN)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
            if (ramadhanGroups.isNotEmpty()) {
                GroupedSlideshowPlayer(
                    groups = ramadhanGroups,
                    snapshot = snapshot,
                    assetsRootDir = assetsRootDir,
                    isExpanded = true,
                    wrapSystemSlideWithHeader = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = slideKinds[page.coerceIn(0, slideKinds.lastIndex)],
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                    label = "TarawihSlide"
                ) { (kind, subIdx) ->
                    when (kind) {
                        "petugas" -> TarawihPetugasSlideView(
                            night = ramadhan.currentNight,
                            hijriYear = ramadhan.hijriYear,
                            penceramah = petugas?.penceramah ?: "-",
                            imamTarawih = petugas?.imamTarawih ?: "-",
                            imamWitir = petugas?.imamWitir ?: "-"
                        )
                        "keuangan" -> TarawihKeuanganSlideView(
                            night = ramadhan.currentNight,
                            hijriYear = ramadhan.hijriYear,
                            penerimaanPria = keuangan?.penerimaanPria ?: 0L,
                            penerimaanWanita = keuangan?.penerimaanWanita ?: 0L,
                            jumlahInfaqMalamIni = keuangan?.totalPenerimaan ?: 0L,
                            saldoAkhir = keuangan?.saldoAkhir ?: 0L
                        )
                        "jadwal" -> RamadhanJadwalSlideView(
                            ramadhan = ramadhan,
                            schedule = snapshot.schedule,
                            isExpanded = true
                        )
                        else -> {
                            val takjilPageIdx = subIdx.coerceIn(0, takjilPages.lastIndex)
                            TarawihTakjilSlideView(
                                night = ramadhan.currentNight,
                                hijriYear = ramadhan.hijriYear,
                                donors = takjilPages[takjilPageIdx],
                                startNumber = takjilPageIdx * 10 + 1,
                                pageNumber = takjilPageIdx + 1,
                                totalTakjilPages = takjilPages.size
                            )
                        }
                    }
                }
            }
        }

        // Bottom Row: Left = Card Menuju Sholat Tarawih, Right = Quoted Reminder Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgCard)
                    .border(2.dp, palette.accent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent.copy(alpha = 0.18f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Column {
                    Text(
                        text = "MENUJU SHOLAT TARAWIH",
                        color = palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = formattedRem,
                        color = palette.accentSecondary,
                        fontSize = 46.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            val tarawihReminder = snapshot.settings.screenTexts.tarawihReminder.trim()
            if (tarawihReminder.isNotEmpty()) {
                Text(
                    text = "“$tarawihReminder”",
                    color = palette.textSecondary,
                    fontSize = 40.sp,
                    lineHeight = 48.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun TarawihPetugasSlideView(
    night: Int,
    hijriYear: String,
    penceramah: String,
    imamTarawih: String,
    imamWitir: String,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
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
                    text = "PETUGAS SHOLAT TARAWIH & WITIR",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ibadah Malam Ramadhan Ke-$night ($hijriYear H)",
                    color = palette.accent,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            OfficerRoleRowCard(
                roleTitle = "PENCERAMAH KULTUM",
                officerName = penceramah,
                accentColor = palette.accent,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT TARAWIH",
                officerName = imamTarawih,
                accentColor = palette.accentSecondary,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT WITIR",
                officerName = imamWitir,
                accentColor = palette.accent,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun TarawihKeuanganSlideView(
    night: Int,
    hijriYear: String,
    penerimaanPria: Long,
    penerimaanWanita: Long,
    jumlahInfaqMalamIni: Long,
    saldoAkhir: Long,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 36.dp)
            )
            Column {
                Text(
                    text = "LAPORAN INFAQ MALAM RAMADHAN KE-$night",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Periode Laporan: Malam Ke-$night Ramadhan $hijriYear H",
                    color = palette.textSecondary,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
            ) {
                FinanceMetricBox(
                    label = "INFAQ JAMAAH PRIA",
                    amount = penerimaanPria,
                    valueColor = Color(0xFF34D399),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "INFAQ JAMAAH WANITA",
                    amount = penerimaanWanita,
                    valueColor = Color(0xFF34D399),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
            ) {
                FinanceMetricBox(
                    label = "JUMLAH INFAQ MALAM INI",
                    amount = jumlahInfaqMalamIni,
                    valueColor = palette.accent,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "SALDO AKHIR KAS RAMADHAN",
                    amount = saldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun TarawihTakjilSlideView(
    night: Int,
    hijriYear: String,
    donors: List<String>,
    startNumber: Int = 1,
    pageNumber: Int = 1,
    totalTakjilPages: Int = 1,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current
    val subtitleText = if (totalTakjilPages > 1) {
        "Hari Ke-$night Ramadhan $hijriYear H  •  Halaman $pageNumber / $totalTakjilPages"
    } else {
        "Hari Ke-$night Ramadhan $hijriYear H"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 36.dp)
            )
            Column {
                Text(
                    text = "DAFTAR PENYUMBANG TA'JIL BUKA PUASA",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitleText,
                    color = palette.accent,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        val displayDonors = if (donors.isEmpty()) {
            (0 until 10).map { "Nama ${startNumber + it}" }
        } else {
            donors
        }
        val leftCol = displayDonors.take(5)
        val rightCol = displayDonors.drop(5).take(5)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
        ) {
            TakjilDonorColumn(
                items = leftCol,
                startNumber = startNumber,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            TakjilDonorColumn(
                items = rightCol,
                startNumber = startNumber + 5,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TakjilDonorColumn(
    items: List<String>,
    startNumber: Int,
    isExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 12.dp else 9.dp)
    ) {
        for (i in 0 until 5) {
            val donor = items.getOrNull(i)
            if (donor != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(if (isExpanded) 16.dp else 13.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.65f))
                        .border(1.5.dp, palette.border, RoundedCornerShape(if (isExpanded) 16.dp else 13.dp))
                        .padding(horizontal = if (isExpanded) 20.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isExpanded) 64.dp else 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accent.copy(alpha = 0.18f))
                            .padding(vertical = if (isExpanded) 8.dp else 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${startNumber + i}.",
                            color = palette.accent,
                            fontSize = if (isExpanded) 24.sp else 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = donor.ifBlank { "Nama ${startNumber + i}" },
                        color = palette.textPrimary,
                        fontSize = if (isExpanded) 30.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun RamadhanJadwalSlideView(
    ramadhan: RamadhanSettings,
    schedule: DailyPrayerSchedule,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current
    val timeFmt = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm") }
    val activeItems = remember(ramadhan.jadwalImsakiyah) {
        ramadhan.jadwalImsakiyah
            .filter { it.enabled && it.label.isNotBlank() }
            .ifEmpty { RamadhanJadwalItem.defaultImsakiyahItems().filter { it.enabled } }
    }
    val cols = when {
        activeItems.size <= 3 -> activeItems.size.coerceAtLeast(1)
        activeItems.size == 4 -> 2
        activeItems.size <= 6 -> 3
        else -> 4
    }
    val rows = remember(activeItems, cols) { activeItems.chunked(cols) }

    val (labelSp, timeSp) = when {
        rows.size == 1 -> if (isExpanded) 30.sp to 112.sp else 24.sp to 84.sp
        rows.size == 2 && cols == 2 -> if (isExpanded) 28.sp to 98.sp else 22.sp to 74.sp
        rows.size == 2 && cols == 3 -> if (isExpanded) 24.sp to 84.sp else 19.sp to 62.sp
        rows.size == 2 -> if (isExpanded) 20.sp to 68.sp else 16.sp to 50.sp
        else -> if (isExpanded) 18.sp to 52.sp else 14.sp to 38.sp
    }

    fun resolveTime(item: RamadhanJadwalItem): String {
        return if (item.sourceType.equals("custom", ignoreCase = true)) {
            item.customTime.trim().ifBlank { "--:--" }
        } else {
            val pid = PrayerId.fromId(item.prayerId)
            pid?.let { schedule.timeFor(it)?.format(timeFmt) }
                ?: item.customTime.trim().ifBlank { "--:--" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 36.dp)
            )
            Column {
                Text(
                    text = ramadhan.jadwalTitle.ifBlank { "JADWAL IMSAKIYAH & BERBUKA RAMADHAN" }.uppercase(),
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Hari Ke-${ramadhan.currentNight} Ramadhan ${ramadhan.hijriYear} H • ${schedule.date}",
                    color = palette.accent,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 18.dp else 12.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 18.dp else 12.dp)
                ) {
                    rowItems.forEach { item ->
                        val isMaghribHighlight = item.highlighted &&
                            !item.sourceType.equals("custom", ignoreCase = true) &&
                            item.prayerId.equals("maghrib", ignoreCase = true)
                        val valueColor = when {
                            isMaghribHighlight -> Color(0xFF34D399)
                            item.highlighted -> palette.accentSecondary
                            else -> palette.textPrimary
                        }
                        RamadhanTimeBox(
                            label = item.label.uppercase(),
                            timeText = resolveTime(item),
                            valueColor = valueColor,
                            isHighlighted = item.highlighted,
                            labelFontSize = labelSp,
                            timeFontSize = timeSp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(cols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RamadhanTimeBox(
    label: String,
    timeText: String,
    valueColor: Color,
    isHighlighted: Boolean = false,
    labelFontSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    timeFontSize: androidx.compose.ui.unit.TextUnit = 84.sp,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (isHighlighted) palette.accentSecondary else palette.textSecondary,
            fontSize = labelFontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = timeText,
            color = valueColor,
            fontSize = timeFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )
    }
}

/**
 * 3. IDUL FITRI, IDUL ADHA & INFO QURBAN FULLSCREEN (`IdulFitriContent.tsx`, `IdulAdhaContent.tsx`)
 * Mengacu pada UI/UX `WaktuJumatFullscreen` & `TarawihFullscreen`:
 * - Top Header: `HeaderBar` + `HeaderClockCard`
 * - Main Rotating Slide Area:
 *   - Idul Fitri: Page 0 = Petugas (`OfficerRoleRowCard`), Page 1 = Keuangan Kas (`FinanceMetricBox` 2x2)
 *   - Idul Adha: Page 0 = Petugas (`OfficerRoleRowCard`), Page 1..N = Kelompok Qurban
 *   - Info Qurban: Page 0..N-1 = Kelompok Qurban
 * - Bottom Row: Countdown / Event Badge Card + Quoted Reminder Text
 */
@Composable
fun HolidayAndQurbanFullscreen(
    snapshot: JwsRuntimeSnapshot,
    isIdulFitri: Boolean,
    isInfoQurbanOnly: Boolean,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)

    val qurbanGroups: List<QurbanGroup> = settings.idulAdha.qurbanGroups.ifEmpty {
        listOf(
            QurbanGroup(
                id = "q-1",
                animalType = "Sapi",
                groupName = "Kelompok Qurban 1",
                members = listOf(
                    "1. H. Abdullah beserta keluarga",
                    "2. H. Sulaiman beserta keluarga",
                    "3. Bpk. Ahmad Fauzi",
                    "4. Bpk. Muhammad Ridwan",
                    "5. Ibu Hj. Siti Aminah",
                    "6. Bpk. Hasan Basri",
                    "7. Hamba Allah"
                )
            )
        )
    }

    val totalPages = when {
        isInfoQurbanOnly -> qurbanGroups.size.coerceAtLeast(1)
        isIdulFitri -> 2
        else -> 1 + qurbanGroups.size.coerceAtLeast(1)
    }
    val slideDurationSec = when {
        isInfoQurbanOnly -> settings.infoQurban.slideDuration
        isIdulFitri -> settings.idulFitri.slideDuration
        else -> settings.idulAdha.slideDuration
    }.coerceIn(5, 60)

    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(slideDurationSec, totalPages) {
        if (page >= totalPages) page = 0
        while (true) {
            delay(slideDurationSec * 1000L)
            page = (page + 1) % totalPages
        }
    }

    val petugas = if (isIdulFitri) settings.idulFitri.petugas else settings.idulAdha.petugas

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Standby HeaderBar + Standby-sized HeaderClockCard (480x180dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderBar(
                now = snapshot.now,
                brand = settings.brand,
                hijriDate = snapshot.hijriDate,
                assetsRootDir = assetsRootDir,
                isReversed = false,
                isLogoMenuOpen = isLogoMenuOpen,
                onLogoClick = onLogoClick,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )

            HeaderClockCard(
                now = snapshot.now,
                clockStyle = settings.brand.clockStyle
            )
        }

        // Main Rotating Slide Area
        val holidayTargetState = when {
            isInfoQurbanOnly -> null
            isIdulFitri -> SlideshowTargetState.IDUL_FITRI
            else -> SlideshowTargetState.IDUL_ADHA
        }
        val holidayGroups = remember(settings, holidayTargetState) {
            holidayTargetState?.let { settings.resolveGroupsForState(it) }.orEmpty()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
            if (holidayGroups.isNotEmpty()) {
                GroupedSlideshowPlayer(
                    groups = holidayGroups,
                    snapshot = snapshot,
                    assetsRootDir = assetsRootDir,
                    isExpanded = true,
                    wrapSystemSlideWithHeader = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                    label = "HolidayQurbanSlide"
                ) { idx ->
                    when {
                        isInfoQurbanOnly -> {
                            val gIdx = idx.coerceIn(0, qurbanGroups.lastIndex)
                            QurbanGroupSlideView(
                                group = qurbanGroups[gIdx],
                                groupNumber = gIdx + 1,
                                totalGroups = qurbanGroups.size
                            )
                        }
                        isIdulFitri && idx == 0 -> {
                            EidPetugasSlideView(
                                title = "PETUGAS SHOLAT HARI RAYA IDUL FITRI",
                                subtitle = "1 Syawal ${settings.idulFitri.hijriYear} H • ${settings.brand.name}",
                                khotib = petugas.khotib.ifBlank { "Ust. H. Ahmad Fauzi, Lc." },
                                imam = petugas.imam.ifBlank { "Ust. Muhammad Ridwan" },
                                bilal = petugas.bilal.ifBlank { "Sdr. Abdullah" }
                            )
                        }
                        isIdulFitri -> {
                            IdulFitriKeuanganSlideView(
                                hijriYear = settings.idulFitri.hijriYear,
                                brandName = settings.brand.name,
                                ramadhan = settings.ramadhan,
                                fallbackEidKeuangan = settings.idulFitri.keuangan
                            )
                        }
                        idx == 0 -> {
                            EidPetugasSlideView(
                                title = "PETUGAS SHOLAT HARI RAYA IDUL ADHA",
                                subtitle = "10 Dzulhijjah ${settings.idulAdha.hijriYear} H • ${settings.brand.name}",
                                khotib = petugas.khotib.ifBlank { "Ust. H. Ahmad Fauzi, Lc." },
                                imam = petugas.imam.ifBlank { "Ust. Muhammad Ridwan" },
                                bilal = petugas.bilal.ifBlank { "Sdr. Abdullah" }
                            )
                        }
                        else -> {
                            val gIdx = (idx - 1).coerceIn(0, qurbanGroups.lastIndex)
                            QurbanGroupSlideView(
                                group = qurbanGroups[gIdx],
                                groupNumber = gIdx + 1,
                                totalGroups = qurbanGroups.size
                            )
                        }
                    }
                }
            }
        }

        // Bottom Row: Left = Event / Countdown Card, Right = Quoted Reminder Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgCard)
                    .border(2.dp, palette.accent.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accent.copy(alpha = 0.18f))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (remSec > 0) Icons.Default.AccessTime else Icons.Default.Celebration,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Column {
                    Text(
                        text = when {
                            isInfoQurbanOnly -> "INFORMASI IBADAH"
                            remSec > 0 -> "MENUJU SHOLAT IED"
                            else -> "HARI RAYA"
                        },
                        color = palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = when {
                            remSec > 0 -> formattedRem
                            isInfoQurbanOnly -> "QURBAN"
                            isIdulFitri -> "IDUL FITRI"
                            else -> "IDUL ADHA"
                        },
                        color = palette.accentSecondary,
                        fontSize = 46.sp,
                        fontFamily = if (remSec > 0) FontFamily.Monospace else FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            val holidayReminder = when {
                isInfoQurbanOnly -> settings.screenTexts.infoQurbanReminder.trim()
                isIdulFitri -> settings.screenTexts.idulFitriReminder.trim()
                else -> settings.screenTexts.idulAdhaReminder.trim()
            }
            if (holidayReminder.isNotEmpty()) {
                Text(
                    text = "“$holidayReminder”",
                    color = palette.textSecondary,
                    fontSize = 40.sp,
                    lineHeight = 48.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun EidPetugasSlideView(
    title: String,
    subtitle: String,
    khotib: String,
    imam: String,
    bilal: String,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
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
                    text = title,
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    color = palette.accent,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            OfficerRoleRowCard(
                roleTitle = "KHOTIB SHOLAT IED",
                officerName = khotib,
                accentColor = palette.accent,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT IED",
                officerName = imam,
                accentColor = palette.accentSecondary,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "BILAL / MUADZIN",
                officerName = bilal,
                accentColor = palette.accent,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun IdulFitriKeuanganSlideView(
    hijriYear: String,
    brandName: String,
    ramadhan: id.algaffar.jwstv.domain.model.RamadhanSettings,
    fallbackEidKeuangan: id.algaffar.jwstv.domain.model.EidKeuangan,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current

    val hasNightlyData = remember(ramadhan.keuangan) {
        ramadhan.keuangan.any {
            it.saldoAwal != 0L || it.penerimaanPria != 0L || it.penerimaanWanita != 0L || it.pengeluaran != 0L
        }
    }
    val hasLaporanAkhir = remember(ramadhan.laporanAkhir) {
        ramadhan.laporanAkhir.totalPenerimaan != 0L ||
            ramadhan.laporanAkhir.totalPengeluaran != 0L ||
            ramadhan.laporanAkhir.saldoAkhir != 0L
    }

    val (saldoAwal, totalPemasukan, totalPengeluaran, totalSaldoAkhir) = remember(
        ramadhan.keuangan,
        ramadhan.laporanAkhir,
        fallbackEidKeuangan,
        hasNightlyData,
        hasLaporanAkhir
    ) {
        when {
            hasNightlyData -> {
                val awal = ramadhan.keuangan.firstOrNull { it.saldoAwal != 0L }?.saldoAwal ?: 0L
                val masuk = ramadhan.keuangan.sumOf { it.totalPenerimaan }
                val keluar = ramadhan.keuangan.sumOf { it.pengeluaran }
                listOf(awal, masuk, keluar, awal + masuk - keluar)
            }
            hasLaporanAkhir -> {
                val lap = ramadhan.laporanAkhir
                listOf(0L, lap.totalPenerimaan, lap.totalPengeluaran, lap.saldoAkhir)
            }
            else -> {
                listOf(
                    fallbackEidKeuangan.saldoAwal,
                    fallbackEidKeuangan.pemasukan,
                    fallbackEidKeuangan.pengeluaran,
                    fallbackEidKeuangan.saldoAkhir
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 28.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 44.dp else 36.dp)
            )
            Column {
                Text(
                    text = "LAPORAN KEUANGAN AKHIR RAMADHAN",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 32.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ramadhan ${ramadhan.hijriYear} H • 1 Syawal $hijriYear H • $brandName",
                    color = palette.textSecondary,
                    fontSize = if (isExpanded) 20.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
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
                    valueColor = Color(0xFF34D399),
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
            ) {
                FinanceMetricBox(
                    label = "TOTAL PENGELUARAN RAMADHAN (-)",
                    amount = totalPengeluaran,
                    valueColor = Color(0xFFF87171),
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
    }
}

@Composable
internal fun QurbanGroupSlideView(
    group: QurbanGroup,
    groupNumber: Int,
    totalGroups: Int,
    isExpanded: Boolean = true
) {
    val palette = LocalJwsTheme.current
    val members = group.activeMembers
    val (nameFontSize, badgeFontSize, badgeWidth) = if (isExpanded) {
        when {
            members.size <= 3 -> Triple(54.sp, 32.sp, 84.dp)
            members.size <= 5 -> Triple(46.sp, 28.sp, 76.dp)
            else -> Triple(40.sp, 26.sp, 72.dp)
        }
    } else {
        when {
            members.size <= 3 -> Triple(42.sp, 26.sp, 72.dp)
            members.size <= 5 -> Triple(34.sp, 22.sp, 64.dp)
            else -> Triple(28.sp, 20.sp, 58.dp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = if (isExpanded) 24.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 16.dp else 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(if (isExpanded) 48.dp else 36.dp)
            )
            Column {
                Text(
                    text = "${group.groupName.ifBlank { "Kelompok $groupNumber" }.uppercase()} (HEWAN: ${group.animalType.uppercase()})",
                    color = palette.textPrimary,
                    fontSize = if (isExpanded) 36.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Daftar Peserta Ibadah Qurban • Kelompok $groupNumber dari $totalGroups",
                    color = palette.accent,
                    fontSize = if (isExpanded) 22.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 10.dp else 7.dp)
        ) {
            if (members.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(if (isExpanded) 16.dp else 12.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.55f))
                        .border(1.5.dp, palette.border, RoundedCornerShape(if (isExpanded) 16.dp else 12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada peserta pada kelompok ini",
                        color = palette.textSecondary,
                        fontSize = if (isExpanded) 32.sp else 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            members.forEachIndexed { idx, rawMember ->
                // Strip leading "1. " if already numbered in settings so badge doesn't duplicate number
                val cleanName = rawMember.trim().replace(Regex("^\\d+\\.\\s*"), "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(if (isExpanded) 16.dp else 12.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.65f))
                        .border(1.5.dp, palette.border, RoundedCornerShape(if (isExpanded) 16.dp else 12.dp))
                        .padding(horizontal = if (isExpanded) 24.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(badgeWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accent.copy(alpha = 0.18f))
                            .padding(vertical = if (isExpanded) 8.dp else 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            color = palette.accent,
                            fontSize = badgeFontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                    Text(
                        text = cleanName,
                        color = palette.textPrimary,
                        fontSize = nameFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 4. STANDBY SLIDESHOW FULLSCREEN & NIGHT SCREENSAVER (`StandbySlideshowContent.tsx`, `ScreenSaver.tsx`)
 */
@Composable
fun StandbySlideshowFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    modifier: Modifier = Modifier
) {
    val fullscreenGroups = remember(snapshot.settings) {
        snapshot.settings.resolveGroupsForState(SlideshowTargetState.FULLSCREEN)
    }
    val standbyCfg = snapshot.settings.standbySlideshow
    val thresholdSec = standbyCfg.nextPrayerThresholdMinutes.coerceIn(5, 120) * 60L
    val showNextPrayerCard = standbyCfg.showNextPrayerCard &&
        snapshot.schedule.secondsToNextPrayer in 1L..thresholdSec

    Box(modifier = modifier.fillMaxSize()) {
        if (fullscreenGroups.isNotEmpty()) {
            GroupedSlideshowPlayer(
                groups = fullscreenGroups,
                snapshot = snapshot,
                assetsRootDir = assetsRootDir,
                isExpanded = true,
                wrapSystemSlideWithHeader = true,
                hideOverlayAndQr = showNextPrayerCard,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WelcomeMasjidSlideView(
                slide = null,
                brand = snapshot.settings.brand,
                isSmall = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating Next Prayer Card when nearing Adzan (Overlay & QR Card are hidden to give it space)
        if (showNextPrayerCard) {
            NextPrayerCountdownCard(
                schedule = snapshot.schedule,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }
}

/**
 * Night Mode ScreenSaver with anti-burn-in floating clock on pure black canvas.
 */
@Composable
fun ScreenSaverFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPerformanceMode = LocalPerformanceMode.current
    val offsetX: Float
    val offsetY: Float

    if (isPerformanceMode) {
        var stepIdx by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(12_000L)
                stepIdx = (stepIdx + 1) % 6
            }
        }
        val driftOffsets = remember {
            listOf(
                -180f to -100f,
                160f to -110f,
                190f to 90f,
                -160f to 110f,
                0f to -120f,
                0f to 100f
            )
        }
        val pair = driftOffsets[stepIdx % driftOffsets.size]
        offsetX = pair.first
        offsetY = pair.second
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "ScreenSaverFloat")
        val animX by infiniteTransition.animateFloat(
            initialValue = -220f,
            targetValue = 220f,
            animationSpec = infiniteRepeatable(
                animation = tween(18_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "FloatX"
        )
        val animY by infiniteTransition.animateFloat(
            initialValue = -140f,
            targetValue = 140f,
            animationSpec = infiniteRepeatable(
                animation = tween(23_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "FloatY"
        )
        offsetX = animX
        offsetY = animY
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        FullscreenBrandHeader(
            brand = snapshot.settings.brand,
            assetsRootDir = assetsRootDir,
            isLogoMenuOpen = isLogoMenuOpen,
            onLogoClick = onLogoClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 52.dp, top = 28.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle
            )
        }
    }
}

/**
 * 5. MASJID QR FULLSCREEN (`masjid_qr`)
 */
@Composable
fun MasjidQrFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    isCompactSlide: Boolean = false,
    modifier: Modifier = Modifier
) {
    val qrCfg = snapshot.settings.masjidQr
    val finance = snapshot.settings.onlineFinance

    QrSplitOverlayFullscreen(
        snapshot = snapshot,
        badgeLabel = "INFAQ & QRIS MASJID",
        badgeIcon = Icons.Default.QrCode2,
        title = qrCfg.label.ifBlank { "Infaq & Sedekah Digital ${snapshot.settings.brand.name}" },
        guidance = snapshot.settings.screenTexts.masjidQrGuidance.trim(),
        qrPayload = qrCfg.link.ifBlank { "Infaq Digital ${snapshot.settings.brand.name}" },
        customQrImage = qrCfg.customQrImage,
        bankName = qrCfg.bankName.ifBlank { finance.bankName },
        nomorRekening = qrCfg.nomorRekening.ifBlank { finance.nomorRekening },
        atasNama = qrCfg.atasNama.ifBlank { finance.atasNama },
        accentColor = Color(0xFF10B981),
        assetsRootDir = assetsRootDir,
        isLogoMenuOpen = isLogoMenuOpen,
        onLogoClick = onLogoClick,
        isCompactSlide = isCompactSlide,
        modifier = modifier
    )
}
