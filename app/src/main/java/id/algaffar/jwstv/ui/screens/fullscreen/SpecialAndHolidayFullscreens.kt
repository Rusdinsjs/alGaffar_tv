package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
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
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.screens.standby.JumatKeuanganSlideView
import id.algaffar.jwstv.ui.screens.standby.JumatPetugasSlideView
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
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
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header with Countdown to Khutbah
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
                    text = "MENJELANG KHUTBAH JUM'AT — ${snapshot.settings.brand.name}",
                    color = palette.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Mohon tenang dan dengarkan pengumuman serta laporan kas masjid sebelum Khotib naik mimbar.",
                    color = palette.textSecondary,
                    fontSize = 15.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MENUJU KHUTBAH",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formattedRem,
                        color = palette.accentSecondary,
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                ArtisticClock(
                    now = snapshot.now,
                    style = snapshot.settings.brand.clockStyle,
                    variant = ClockVariant.HEADER
                )
            }
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
                    JumatPetugasSlideView(fridayDate = effPetugasDate, record = petugasRec)
                } else {
                    JumatKeuanganSlideView(fridayDate = effKeuDate, record = keuRec)
                }
            }
        }
    }
}

/**
 * 2. TARAWIH RAMADHAN FULLSCREEN (`TarawihContent.tsx`)
 * Rotates:
 * - Page 0: Petugas Tarawih & Kultum Malam Ke-N
 * - Page 1: Laporan Kas Ramadhan (Penerimaan Jamaah Pria & Wanita)
 * - Page 2: Daftar Penyumbang Ta'jil (30 Hari)
 */
@Composable
fun TarawihFullscreen(
    snapshot: JwsRuntimeSnapshot,
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

    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(ramadhan.slideshow.slideDuration) {
        while (true) {
            delay(ramadhan.slideshow.slideDuration.coerceIn(5, 60) * 1000L)
            page = (page + 1) % 3
        }
    }

    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(38.dp)
                )
                Column {
                    Text(
                        text = "IBADAH MALAM RAMADHAN KE-${ramadhan.currentNight}",
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = snapshot.settings.brand.name,
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MENUJU SHOLAT TARAWIH",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = formattedRem,
                        color = palette.accentSecondary,
                        fontSize = 36.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                ArtisticClock(
                    now = snapshot.now,
                    style = snapshot.settings.brand.clockStyle,
                    variant = ClockVariant.HEADER
                )
            }
        }

        // Rotating Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
                .padding(36.dp)
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                label = "TarawihSlide"
            ) { idx ->
                when (idx) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PETUGAS KULTUM, TARAWIH & WITIR (MALAM KE-${ramadhan.currentNight})",
                                color = palette.accentSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                RamadhanInfoCard(
                                    title = "PENCERAMAH KULTUM",
                                    value = petugas?.penceramah ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                RamadhanInfoCard(
                                    title = "IMAM SHOLAT TARAWIH",
                                    value = petugas?.imamTarawih ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                RamadhanInfoCard(
                                    title = "IMAM SHOLAT WITIR",
                                    value = petugas?.imamWitir ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "LAPORAN INFAQ MALAM RAMADHAN KE-${ramadhan.currentNight}",
                                color = palette.accentSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RamadhanInfoCard(
                                    title = "INFAQ JAMAAH PRIA",
                                    value = rupiahFormat.format(keuangan?.penerimaanPria ?: 0L),
                                    modifier = Modifier.weight(1f)
                                )
                                RamadhanInfoCard(
                                    title = "INFAQ JAMAAH WANITA",
                                    value = rupiahFormat.format(keuangan?.penerimaanWanita ?: 0L),
                                    modifier = Modifier.weight(1f)
                                )
                                RamadhanInfoCard(
                                    title = "SALDO AKHIR KAS RAMADHAN",
                                    value = rupiahFormat.format(keuangan?.saldoAkhir ?: 0L),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DAFTAR PENYUMBANG TA'JIL BUKA PUASA (HARI KE-${ramadhan.currentNight})",
                                color = palette.accentSecondary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 20.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.bgPrimary.copy(alpha = 0.65f))
                                    .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (takjilDonors.isEmpty()) {
                                        "Jazakumullahu Khairan Katsiran kepada seluruh Muhsinin & Jamaah Masjid Al-Gaffar"
                                    } else {
                                        takjilDonors.joinToString("   •   ")
                                    },
                                    color = palette.textPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 40.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RamadhanInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgPrimary.copy(alpha = 0.68f))
            .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = palette.accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 3. IDUL FITRI, IDUL ADHA & INFO QURBAN FULLSCREEN (`IdulFitriContent.tsx`, `IdulAdhaContent.tsx`)
 */
@Composable
fun HolidayAndQurbanFullscreen(
    snapshot: JwsRuntimeSnapshot,
    isIdulFitri: Boolean,
    isInfoQurbanOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val qurbanGroups: List<QurbanGroup> = settings.idulAdha.qurbanGroups.ifEmpty {
        listOf(
            QurbanGroup(
                id = "q-1",
                animalType = "Sapi",
                groupName = "Kelompok Sapi 1",
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

    var groupIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(qurbanGroups.size) {
        while (true) {
            delay(8_000L)
            groupIdx = (groupIdx + 1) % qurbanGroups.size.coerceAtLeast(1)
        }
    }

    val petugas = if (isIdulFitri) settings.idulFitri.petugas else settings.idulAdha.petugas

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = if (isInfoQurbanOnly) Icons.Default.Groups else Icons.Default.Celebration,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(38.dp)
                )
                Column {
                    Text(
                        text = when {
                            isInfoQurbanOnly -> "DAFTAR PESERTA & KELOMPOK QURBAN — ${settings.brand.name}"
                            isIdulFitri -> "INFORMASI SHOLAT HARI RAYA IDUL FITRI — ${settings.brand.name}"
                            else -> "INFORMASI SHOLAT IDUL ADHA & IBADAH QURBAN — ${settings.brand.name}"
                        },
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Taqabbalallahu Minna Wa Minkum • Shiyamana Wa Shiyamakum",
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            ArtisticClock(
                now = snapshot.now,
                style = settings.brand.clockStyle,
                variant = ClockVariant.HEADER
            )
        }

        if (!isInfoQurbanOnly) {
            // Petugas Hari Raya Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                RamadhanInfoCard(
                    title = "KHOTIB SHOLAT IED",
                    value = petugas.khotib.ifBlank { "Ust. H. Ahmad Fauzi, Lc." },
                    modifier = Modifier.weight(1f)
                )
                RamadhanInfoCard(
                    title = "IMAM SHOLAT IED",
                    value = petugas.imam.ifBlank { "Ust. Muhammad Ridwan" },
                    modifier = Modifier.weight(1f)
                )
                RamadhanInfoCard(
                    title = "BILAL / MUADZIN",
                    value = petugas.bilal.ifBlank { "Sdr. Abdullah" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom: Kas Idul Fitri OR Qurban Group Members
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            if (isIdulFitri && !isInfoQurbanOnly) {
                val rupiah = remember {
                    NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
                }
                val keu = settings.idulFitri.keuangan
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    RamadhanInfoCard(
                        title = "SALDO AWAL",
                        value = rupiah.format(keu.saldoAwal),
                        modifier = Modifier.weight(1f)
                    )
                    RamadhanInfoCard(
                        title = "PENERIMAAN INFAQ IED",
                        value = rupiah.format(keu.pemasukan),
                        modifier = Modifier.weight(1f)
                    )
                    RamadhanInfoCard(
                        title = "SALDO AKHIR KAS",
                        value = rupiah.format(keu.saldoAkhir),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                val activeGroup = qurbanGroups[groupIdx % qurbanGroups.size]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${activeGroup.groupName.uppercase()} (HEWAN: ${activeGroup.animalType.uppercase()})",
                            color = palette.accentSecondary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Kelompok ${(groupIdx % qurbanGroups.size) + 1} dari ${qurbanGroups.size}",
                            color = palette.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        activeGroup.members.forEach { member ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.bgPrimary.copy(alpha = 0.65f))
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = member,
                                    color = palette.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
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

    LaunchedEffect(activeSlide?.id, slides.size) {
        val dur = (activeSlide?.duration ?: 10).coerceIn(3, 120) * 1000L
        delay(dur)
        if (slides.isNotEmpty()) {
            idx = (idx + 1) % slides.size
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeSlide != null && activeSlide.imageUrl.isNotBlank()) {
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(palette.bgPrimary.copy(alpha = 0.85f))
                .padding(36.dp)
        ) {
            Text(
                text = activeSlide?.title?.ifBlank { snapshot.settings.brand.name }
                    ?: snapshot.settings.brand.name,
                color = palette.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (!activeSlide?.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activeSlide?.content.orEmpty(),
                    color = palette.textSecondary,
                    fontSize = 20.sp
                )
            }
        }
    }
}

/**
 * Night Mode ScreenSaver with anti-burn-in floating clock on pure black canvas.
 */
@Composable
fun ScreenSaverFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScreenSaverFloat")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -220f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatX"
    )
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -140f,
        targetValue = 140f,
        animationSpec = infiniteRepeatable(
            animation = tween(23_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset {
                IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ArtisticClock(
                now = snapshot.now,
                style = "classic",
                variant = ClockVariant.MAIN
            )
            Text(
                text = snapshot.settings.screensaver.message.ifBlank { snapshot.settings.brand.name },
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Sholat Berikutnya: ${snapshot.schedule.nextPrayer.label} (${snapshot.schedule.nextPrayer.formattedTime})",
                color = Color(0xFF10B981).copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
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
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val qrCfg = snapshot.settings.masjidQr
    val qrBitmap = remember(qrCfg.link) {
        generateQrBitmap(qrCfg.link.ifBlank { "https://masjidalgaffar.id/infaq" }, 520)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.settings.brand.name,
                color = palette.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            ArtisticClock(
                now = snapshot.now,
                style = snapshot.settings.brand.clockStyle,
                variant = ClockVariant.HEADER
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(420.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .border(4.dp, palette.accentSecondary, RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = qrCfg.label,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(280.dp)
                    )
                }
            }

            Text(
                text = qrCfg.label.ifBlank { "Infaq & Sedekah Digital Masjid Al-Gaffar" },
                color = palette.textPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pindai QR Code di atas menggunakan aplikasi Mobile Banking atau E-Wallet (QRIS) Anda.",
                color = palette.textSecondary,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
