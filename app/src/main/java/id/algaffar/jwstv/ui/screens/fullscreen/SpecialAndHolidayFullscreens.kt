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
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.BigAnalogClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.screens.standby.FinanceMetricBox
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
        khotib = "Ust. H. Ahmad Fauzi, Lc., M.Ag.",
        imam = "Ust. Muhammad Ridwan, Al-Hafizh",
        muadzin = "Sdr. Abdullah Al-Gaffar"
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
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

            Text(
                text = "“Mohon tenang dan dengarkan pengumuman serta laporan kas masjid sebelum Khotib naik mimbar.”",
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
    val totalPages = 2 + takjilPages.size

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                label = "TarawihSlide"
            ) { idx ->
                when (idx) {
                    0 -> TarawihPetugasSlideView(
                        night = ramadhan.currentNight,
                        hijriYear = ramadhan.hijriYear,
                        penceramah = petugas?.penceramah ?: "-",
                        imamTarawih = petugas?.imamTarawih ?: "-",
                        imamWitir = petugas?.imamWitir ?: "-"
                    )
                    1 -> TarawihKeuanganSlideView(
                        night = ramadhan.currentNight,
                        hijriYear = ramadhan.hijriYear,
                        penerimaanPria = keuangan?.penerimaanPria ?: 0L,
                        penerimaanWanita = keuangan?.penerimaanWanita ?: 0L,
                        jumlahInfaqMalamIni = keuangan?.totalPenerimaan ?: 0L,
                        saldoAkhir = keuangan?.saldoAkhir ?: 0L
                    )
                    else -> {
                        val takjilPageIdx = (idx - 2).coerceIn(0, takjilPages.lastIndex)
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

            Text(
                text = "“Mohon tenang dan bersiap melaksanakan ibadah Sholat Tarawih & Witir berjamaah.”",
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

@Composable
private fun TarawihPetugasSlideView(
    night: Int,
    hijriYear: String,
    penceramah: String,
    imamTarawih: String,
    imamWitir: String
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = "PETUGAS SHOLAT TARAWIH & WITIR",
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ibadah Malam Ramadhan Ke-$night ($hijriYear H)",
                    color = palette.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OfficerRoleRowCard(
                roleTitle = "PENCERAMAH KULTUM",
                officerName = penceramah,
                accentColor = palette.accent,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT TARAWIH",
                officerName = imamTarawih,
                accentColor = palette.accentSecondary,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT WITIR",
                officerName = imamWitir,
                accentColor = palette.accent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TarawihKeuanganSlideView(
    night: Int,
    hijriYear: String,
    penerimaanPria: Long,
    penerimaanWanita: Long,
    jumlahInfaqMalamIni: Long,
    saldoAkhir: Long
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = "LAPORAN INFAQ MALAM RAMADHAN KE-$night",
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Periode Laporan: Malam Ke-$night Ramadhan $hijriYear H",
                    color = palette.textSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinanceMetricBox(
                    label = "INFAQ JAMAAH PRIA",
                    amount = penerimaanPria,
                    valueColor = Color(0xFF34D399),
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "INFAQ JAMAAH WANITA",
                    amount = penerimaanWanita,
                    valueColor = Color(0xFF34D399),
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinanceMetricBox(
                    label = "JUMLAH INFAQ MALAM INI",
                    amount = jumlahInfaqMalamIni,
                    valueColor = palette.accent,
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "SALDO AKHIR KAS RAMADHAN",
                    amount = saldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TarawihTakjilSlideView(
    night: Int,
    hijriYear: String,
    donors: List<String>,
    startNumber: Int = 1,
    pageNumber: Int = 1,
    totalTakjilPages: Int = 1
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
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = "DAFTAR PENYUMBANG TA'JIL BUKA PUASA",
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitleText,
                    color = palette.accent,
                    fontSize = 20.sp,
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
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TakjilDonorColumn(
                items = leftCol,
                startNumber = startNumber,
                modifier = Modifier.weight(1f)
            )
            TakjilDonorColumn(
                items = rightCol,
                startNumber = startNumber + 5,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TakjilDonorColumn(
    items: List<String>,
    startNumber: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (i in 0 until 5) {
            val donor = items.getOrNull(i)
            if (donor != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.65f))
                        .border(1.5.dp, palette.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accent.copy(alpha = 0.18f))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${startNumber + i}.",
                            color = palette.accent,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = donor.ifBlank { "Nama ${startNumber + i}" },
                        color = palette.textPrimary,
                        fontSize = 30.sp,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        ) {
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

            Text(
                text = when {
                    isInfoQurbanOnly -> "“Semoga Allah menerima amal ibadah Qurban kita semua dan melimpahkan keberkahan.”"
                    isIdulFitri -> "“Taqabbalallahu Minna Wa Minkum • Mohon tenang dan bersiap melaksanakan Sholat Idul Fitri.”"
                    else -> "“Allahu Akbar, Allahu Akbar • Mari bersiap melaksanakan Sholat Idul Adha & Ibadah Qurban.”"
                },
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

@Composable
private fun EidPetugasSlideView(
    title: String,
    subtitle: String,
    khotib: String,
    imam: String,
    bilal: String
) {
    val palette = LocalJwsTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = subtitle,
                    color = palette.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OfficerRoleRowCard(
                roleTitle = "KHOTIB SHOLAT IED",
                officerName = khotib,
                accentColor = palette.accent,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "IMAM SHOLAT IED",
                officerName = imam,
                accentColor = palette.accentSecondary,
                modifier = Modifier.weight(1f)
            )
            OfficerRoleRowCard(
                roleTitle = "BILAL / MUADZIN",
                officerName = bilal,
                accentColor = palette.accent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IdulFitriKeuanganSlideView(
    hijriYear: String,
    brandName: String,
    ramadhan: id.algaffar.jwstv.domain.model.RamadhanSettings,
    fallbackEidKeuangan: id.algaffar.jwstv.domain.model.EidKeuangan
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
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = "LAPORAN KEUANGAN AKHIR RAMADHAN",
                    color = palette.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ramadhan ${ramadhan.hijriYear} H • 1 Syawal $hijriYear H • $brandName",
                    color = palette.textSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinanceMetricBox(
                    label = "SALDO AWAL RAMADHAN",
                    amount = saldoAwal,
                    valueColor = palette.textPrimary,
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL PEMASUKAN RAMADHAN (+)",
                    amount = totalPemasukan,
                    valueColor = Color(0xFF34D399),
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FinanceMetricBox(
                    label = "TOTAL PENGELUARAN RAMADHAN (-)",
                    amount = totalPengeluaran,
                    valueColor = Color(0xFFF87171),
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricBox(
                    label = "TOTAL SALDO AKHIR RAMADHAN",
                    amount = totalSaldoAkhir,
                    valueColor = palette.accentSecondary,
                    isHighlighted = true,
                    isExpanded = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QurbanGroupSlideView(
    group: QurbanGroup,
    groupNumber: Int,
    totalGroups: Int
) {
    val palette = LocalJwsTheme.current
    val members = group.members.take(7)
    val (nameFontSize, badgeFontSize, badgeWidth) = when {
        members.size <= 3 -> Triple(54.sp, 32.sp, 84.dp)
        members.size <= 5 -> Triple(46.sp, 28.sp, 76.dp)
        else -> Triple(40.sp, 26.sp, 72.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    text = "${group.groupName.uppercase()} (HEWAN: ${group.animalType.uppercase()})",
                    color = palette.textPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Daftar Peserta Ibadah Qurban • Kelompok $groupNumber dari $totalGroups",
                    color = palette.accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            members.forEachIndexed { idx, rawMember ->
                // Strip leading "1. " if already numbered in settings so badge doesn't duplicate number
                val cleanName = rawMember.trim().replace(Regex("^\\d+\\.\\s*"), "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.65f))
                        .border(1.5.dp, palette.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(badgeWidth)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.accent.copy(alpha = 0.18f))
                            .padding(vertical = 8.dp),
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
    val palette = LocalJwsTheme.current
    val slides = snapshot.settings.slides.filter { it.enabled && it.showInSlideshow }

    var idx by remember { mutableIntStateOf(0) }
    val activeSlide = slides.getOrNull(idx % slides.size.coerceAtLeast(1))

    LaunchedEffect(activeSlide?.id, slides.size, snapshot.settings.slideConfig.autoPlay) {
        if (!snapshot.settings.slideConfig.autoPlay || slides.size <= 1) return@LaunchedEffect
        val dur = (activeSlide?.duration ?: 10).coerceIn(3, 120) * 1000L
        delay(dur)
        if (slides.isNotEmpty()) {
            idx = (idx + 1) % slides.size
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val isCustomFullCanvasSlide = activeSlide == null ||
            activeSlide.type !in setOf("image", "video")

        when {
            activeSlide?.type == "welcome" -> {
                WelcomeMasjidSlideView(
                    slide = activeSlide,
                    brand = snapshot.settings.brand,
                    isSmall = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            activeSlide?.type == "analog_clock" -> {
                BigAnalogClock(
                    now = snapshot.now,
                    isSmall = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
            activeSlide?.type == "text" -> {
                id.algaffar.jwstv.ui.screens.standby.StyledTextSlideView(
                    slide = activeSlide,
                    modifier = Modifier.fillMaxSize()
                )
            }
            activeSlide?.type == "video" && activeSlide.videoUrl.isNotBlank() -> {
                val clean = activeSlide.videoUrl.removePrefix("jws-assets/").removePrefix("/")
                val resolvedUri = remember(activeSlide.videoUrl, assetsRootDir) {
                    if (activeSlide.videoUrl.startsWith("http://") ||
                        activeSlide.videoUrl.startsWith("https://") ||
                        activeSlide.videoUrl.startsWith("file://") ||
                        activeSlide.videoUrl.startsWith("content://")
                    ) {
                        android.net.Uri.parse(activeSlide.videoUrl)
                    } else {
                        assetsRootDir?.let { File(it, clean).takeIf { f -> f.exists() } }
                            ?.let { android.net.Uri.fromFile(it) }
                    }
                }
                if (resolvedUri != null) {
                    LocalSlideVideoPlayer(
                        videoUri = resolvedUri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            activeSlide != null && activeSlide.type == "image" && activeSlide.imageUrl.isNotBlank() -> {
                val clean = activeSlide.imageUrl.removePrefix("jws-assets/").removePrefix("/")
                val fileModel = assetsRootDir?.let { File(it, clean).takeIf { f -> f.exists() } }
                    ?: activeSlide.imageUrl
                AsyncImage(
                    model = fileModel,
                    contentDescription = activeSlide.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            activeSlide != null -> {
                val handled = id.algaffar.jwstv.ui.screens.standby.RenderSystemDataSlideIfApplicable(
                    slide = activeSlide,
                    settings = snapshot.settings,
                    today = snapshot.schedule.date,
                    modifier = Modifier.fillMaxSize()
                )
                if (!handled) {
                    WelcomeMasjidSlideView(
                        slide = null,
                        brand = snapshot.settings.brand,
                        isSmall = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                WelcomeMasjidSlideView(
                    slide = null,
                    brand = snapshot.settings.brand,
                    isSmall = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (activeSlide != null && !isCustomFullCanvasSlide && (activeSlide.title.isNotBlank() || activeSlide.content.isNotBlank())) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(palette.bgPrimary.copy(alpha = 0.85f))
                    .padding(36.dp)
            ) {
                if (activeSlide.title.isNotBlank()) {
                    Text(
                        text = activeSlide.title,
                        color = palette.textPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (activeSlide.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeSlide.content,
                        color = palette.textSecondary,
                        fontSize = 20.sp
                    )
                }
            }
        }

        // Optional floating Next Prayer Card in bottom-right when nearing Adzan
        val standbyCfg = snapshot.settings.standbySlideshow
        val thresholdSec = standbyCfg.nextPrayerThresholdMinutes.coerceIn(5, 120) * 60L
        if (standbyCfg.showNextPrayerCard && snapshot.schedule.secondsToNextPrayer in 1L..thresholdSec) {
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
    modifier: Modifier = Modifier
) {
    val qrCfg = snapshot.settings.masjidQr
    val finance = snapshot.settings.onlineFinance

    QrSplitOverlayFullscreen(
        snapshot = snapshot,
        badgeLabel = "INFAQ & QRIS MASJID",
        badgeIcon = Icons.Default.QrCode2,
        title = qrCfg.label.ifBlank { "Infaq & Sedekah Digital Masjid Al-Gaffar" },
        guidance = "Pindai QR Code di samping menggunakan aplikasi Mobile Banking atau E-Wallet (QRIS) Anda untuk menyalurkan infaq & sedekah terbaik.",
        qrPayload = qrCfg.link.ifBlank { "https://masjidalgaffar.id/infaq" },
        customQrImage = qrCfg.customQrImage,
        bankName = qrCfg.bankName.ifBlank { finance.bankName },
        nomorRekening = qrCfg.nomorRekening.ifBlank { finance.nomorRekening },
        atasNama = qrCfg.atasNama.ifBlank { finance.atasNama },
        accentColor = Color(0xFF10B981),
        assetsRootDir = assetsRootDir,
        isLogoMenuOpen = isLogoMenuOpen,
        onLogoClick = onLogoClick,
        modifier = modifier
    )
}
