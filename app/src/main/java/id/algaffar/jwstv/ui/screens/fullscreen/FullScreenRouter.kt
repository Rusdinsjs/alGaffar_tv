package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.screens.standby.JumatKeuanganSlideView
import id.algaffar.jwstv.ui.screens.standby.StandbyScreen
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File

/**
 * Master 27-State Router (`src/components/Screens/FullScreen/index.tsx`).
 * Routes the current [AppState] to either [StandbyScreen] or the corresponding Fullscreen view.
 */
@Composable
fun FullScreenRouter(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    onCycleTheme: () -> Unit,
    onCycleLayout: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onOpenDebugModal: () -> Unit,
    onOpenSettingsModal: () -> Unit,
    onExitManualSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeState = snapshot.tickResult.state
    val activePrayer = snapshot.tickResult.activePrayerId ?: PrayerId.DHUHR
    val prayerLabel = activePrayer.displayName(isFriday = snapshot.schedule.isFriday)
    val stageCfg = snapshot.settings.prayers.forPrayer(activePrayer)

    val today = snapshot.schedule.date
    val effJumatPetugasDate = remember(snapshot.settings.jumat.petugas.records, today) {
        JwsStateMachine.findEffectivePetugasJumatDate(snapshot.settings.jumat.petugas.records, today)
    }
    val jumatPetugas = effJumatPetugasDate?.let { snapshot.settings.jumat.petugas.records[it] }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeState,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
            label = "Master27StateTransition"
        ) { state ->
            when (state) {
                AppState.STANDBY,
                AppState.IDUL_FITRI_STANDBY,
                AppState.IDUL_ADHA_STANDBY,
                AppState.INFO_QURBAN_STANDBY -> {
                    StandbyScreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        onCycleTheme = onCycleTheme,
                        onCycleLayout = onCycleLayout,
                        onCycleClockStyle = onCycleClockStyle,
                        onOpenDebugModal = onOpenDebugModal,
                        onOpenSettingsModal = onOpenSettingsModal
                    )
                }

                AppState.TARTIL -> {
                    TartilTarhimFullscreen(
                        snapshot = snapshot,
                        isTarhim = false
                    )
                }

                AppState.TARHIM -> {
                    TartilTarhimFullscreen(
                        snapshot = snapshot,
                        isTarhim = true
                    )
                }

                AppState.ADZAN -> {
                    AdzanFullscreen(snapshot = snapshot)
                }

                AppState.MENUNGGU_IQOMAH -> {
                    IqomahFullscreen(snapshot = snapshot)
                }

                AppState.PELAKSANAAN_SHOLAT -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN SHOLAT ${prayerLabel.uppercase()}",
                        stageSubtitle = "Harap tenang, sholat berjamaah sedang berlangsung.",
                        officerRoleLabel = "IMAM SHOLAT",
                        officerName = stageCfg.imam
                    )
                }

                AppState.WAKTU_JUMAT -> {
                    WaktuJumatFullscreen(snapshot = snapshot)
                }

                AppState.KHUTBAH -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN KHUTBAH JUM'AT",
                        stageSubtitle = "“Jika engkau berkata kepada sahabatmu pada hari Jum'at: ‘Diamlah!’ sewaktu imam berkhotbah, maka sungguh engkau telah sia-sia.” (HR. Bukhari & Muslim)",
                        officerRoleLabel = "KHOTIB JUM'AT",
                        officerName = jumatPetugas?.khotib.orEmpty()
                    )
                }

                AppState.PELAKSANAAN_JUMAT -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN SHOLAT JUM'AT",
                        stageSubtitle = "Luruskan dan rapatkan shaf untuk kesempurnaan sholat berjamaah.",
                        officerRoleLabel = "IMAM SHOLAT JUM'AT",
                        officerName = jumatPetugas?.imam.orEmpty()
                    )
                }

                AppState.TARAWIH -> {
                    TarawihFullscreen(snapshot = snapshot)
                }

                AppState.SHOLAT_TARAWIH -> {
                    val nightIdx = (snapshot.settings.ramadhan.currentNight - 1).coerceIn(0, 29)
                    val ramadhanPetugas = snapshot.settings.ramadhan.petugas.getOrNull(nightIdx)
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN SHOLAT TARAWIH & WITIR",
                        stageSubtitle = "Malam Ramadhan Ke-${snapshot.settings.ramadhan.currentNight} • ${snapshot.settings.brand.name}",
                        officerRoleLabel = "IMAM TARAWIH & WITIR",
                        officerName = "${ramadhanPetugas?.imamTarawih ?: "-"} / ${ramadhanPetugas?.imamWitir ?: "-"}"
                    )
                }

                AppState.IDUL_FITRI -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = true,
                        isInfoQurbanOnly = false
                    )
                }

                AppState.SHOLAT_IDUL_FITRI -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN SHOLAT IDUL FITRI",
                        stageSubtitle = "Allahu Akbar, Allahu Akbar, Laa Ilaaha Illallahu Wallahu Akbar",
                        officerRoleLabel = "IMAM SHOLAT IED",
                        officerName = snapshot.settings.idulFitri.petugas.imam
                    )
                }

                AppState.KHUTBAH_IDUL_FITRI -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "KHUTBAH HARI RAYA IDUL FITRI",
                        stageSubtitle = "Mohon menyimak khutbah Idul Fitri dengan khusyuk.",
                        officerRoleLabel = "KHOTIB IDUL FITRI",
                        officerName = snapshot.settings.idulFitri.petugas.khotib
                    )
                }

                AppState.IDUL_ADHA -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = false,
                        isInfoQurbanOnly = false
                    )
                }

                AppState.SHOLAT_IDUL_ADHA -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "PELAKSANAAN SHOLAT IDUL ADHA",
                        stageSubtitle = "Allahu Akbar, Allahu Akbar, Laa Ilaaha Illallahu Wallahu Akbar",
                        officerRoleLabel = "IMAM SHOLAT IED",
                        officerName = snapshot.settings.idulAdha.petugas.imam
                    )
                }

                AppState.KHUTBAH_IDUL_ADHA -> {
                    CctvStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "KHUTBAH HARI RAYA IDUL ADHA",
                        stageSubtitle = "Mohon menyimak khutbah Idul Adha dengan khusyuk.",
                        officerRoleLabel = "KHOTIB IDUL ADHA",
                        officerName = snapshot.settings.idulAdha.petugas.khotib
                    )
                }

                AppState.INFO_QURBAN -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = false,
                        isInfoQurbanOnly = true
                    )
                }

                AppState.STANDBY_SLIDESHOW -> {
                    StandbySlideshowFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir
                    )
                }

                AppState.SCREENSAVER -> {
                    ScreenSaverFullscreen(snapshot = snapshot)
                }

                AppState.MASJID_QR -> {
                    MasjidQrFullscreen(snapshot = snapshot)
                }

                AppState.ONLINE_FINANCE -> {
                    OnlineFinanceFullscreen(snapshot = snapshot)
                }

                AppState.LIVE -> {
                    YouTubeLiveFullscreen(snapshot = snapshot)
                }

                AppState.CCTV -> {
                    CctvGridFullscreen(snapshot = snapshot)
                }
            }
        }

        // Top-Center Floating Control Pill whenever in Fullscreen or Manual Simulation
        if (activeState.isFullScreen || snapshot.tickResult.isManualOverride) {
            val palette = LocalJwsTheme.current
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "State: ${activeState.serialName}",
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accent.copy(alpha = 0.25f))
                        .focusable()
                        .clickable { onOpenDebugModal() }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Panel Simulasi",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (snapshot.tickResult.isManualOverride) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFDC2626))
                            .focusable()
                            .clickable { onExitManualSimulation() }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Kembali ke Standby",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
