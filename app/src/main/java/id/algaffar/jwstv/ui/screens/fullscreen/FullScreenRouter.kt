package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import java.io.File

/**
 * Master 27-State Router (`src/components/Screens/FullScreen/index.tsx`).
 * Routes the current [AppState] to either [StandbyScreen] or the corresponding Fullscreen view.
 */
@Composable
fun FullScreenRouter(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    isLogoMenuOpen: Boolean,
    onToggleLogoMenu: () -> Unit,
    onOpenDebugModal: () -> Unit,
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

    val isPerformanceMode = LocalPerformanceMode.current
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeState,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (isPerformanceMode) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(400))
                }
            },
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
                        isLogoMenuOpen = isLogoMenuOpen,
                        onToggleLogoMenu = onToggleLogoMenu
                    )
                }

                AppState.TARTIL -> {
                    TartilTarhimFullscreen(
                        snapshot = snapshot,
                        isTarhim = false,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.TARHIM -> {
                    TartilTarhimFullscreen(
                        snapshot = snapshot,
                        isTarhim = true,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.ADZAN -> {
                    AdzanFullscreen(snapshot = snapshot)
                }

                AppState.DOA_ADZAN -> {
                    DoaAdzanFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir
                    )
                }

                AppState.MENUNGGU_IQOMAH -> {
                    IqomahFullscreen(snapshot = snapshot)
                }

                AppState.PELAKSANAAN_SHOLAT -> {
                    val isMaghrib = activePrayer == PrayerId.MAGHRIB
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Pelaksanaan Sholat\n$prayerLabel",
                        countdownLabel = "Pelaksanaan Sholat $prayerLabel",
                        stageContextBadge = "SHOLAT FARDU",
                        stageMessage = stageCfg.sholatMessage.ifBlank { "Rapatkan Shaf dan Matikan Gadget" },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Imam",
                                name = stageCfg.imam.trim().ifBlank { "Ust. Fulan Imam" },
                                icon = Icons.Default.Person
                            )
                        ),
                        stageIcon = Icons.Default.Schedule,
                        accentColor = Color(0xFF10B981),
                        showTopRightClock = true,
                        backgroundImagePath = snapshot.settings.media.sholat5WaktuImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu,
                        mainTitleFontSize = if (isMaghrib) 148.sp else null,
                        mainTitleMaxLines = if (isMaghrib) 1 else 2
                    )
                }

                AppState.WAKTU_JUMAT -> {
                    WaktuJumatFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.KHUTBAH -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Khutbah Jum'at",
                        stageMessage = snapshot.settings.jumat.khutbahMessage.ifBlank {
                            "Harap Tenang dan Menyimak Khutbah"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Khotib",
                                name = jumatPetugas?.khotib?.trim().orEmpty().ifBlank { "Ust. Fulan Khotib" },
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                        ),
                        stageIcon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = Color(0xFFF59E0B),
                        backgroundImagePath = snapshot.settings.media.khutbahJumatImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.PELAKSANAAN_JUMAT -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Sholat Jum'at",
                        stageMessage = snapshot.settings.jumat.sholatMessage.ifBlank {
                            "Rapatkan Shaf dan Matikan Gadget"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Imam",
                                name = jumatPetugas?.imam?.trim().orEmpty().ifBlank { "Ust. Fulan Imam" },
                                icon = Icons.Default.Person
                            )
                        ),
                        stageIcon = Icons.Default.Person,
                        accentColor = Color(0xFF10B981),
                        backgroundImagePath = snapshot.settings.media.sholatJumatImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.TARAWIH -> {
                    TarawihFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.CERAMAH_TARAWIH -> {
                    val nightIdx = (snapshot.settings.ramadhan.currentNight - 1).coerceIn(0, 29)
                    val ramadhanPetugas = snapshot.settings.ramadhan.petugas.getOrNull(nightIdx)
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Ceramah Tarawih",
                        stageContextBadge = "MALAM RAMADHAN KE-${snapshot.settings.ramadhan.currentNight}",
                        stageMessage = snapshot.settings.ramadhan.ceramahMessage.ifBlank {
                            "Harap Tenang dan Menyimak Ceramah"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Penceramah",
                                name = ramadhanPetugas?.penceramah?.trim().orEmpty().ifBlank { "Ust. Fulan" },
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                        ),
                        stageIcon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = Color(0xFFF59E0B),
                        backgroundImagePath = snapshot.settings.media.ceramahTarawihImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu,
                        mainTitleFontSize = 144.sp,
                        mainTitleMaxLines = 1
                    )
                }

                AppState.SHOLAT_TARAWIH -> {
                    val nightIdx = (snapshot.settings.ramadhan.currentNight - 1).coerceIn(0, 29)
                    val ramadhanPetugas = snapshot.settings.ramadhan.petugas.getOrNull(nightIdx)
                    val imamTarawih = ramadhanPetugas?.imamTarawih?.trim().orEmpty().ifBlank { "Ust. Fulan Imam" }
                    val imamWitir = ramadhanPetugas?.imamWitir?.trim().orEmpty()
                    val hasSeparateWitir = imamWitir.isNotBlank() && imamWitir != "-" && imamWitir != imamTarawih
                    val tarawihOfficers = buildList {
                        add(
                            OverlayOfficerInfo(
                                role = if (hasSeparateWitir) "Imam Tarawih" else "Imam Tarawih & Witir",
                                name = imamTarawih,
                                icon = Icons.Default.Person
                            )
                        )
                        if (hasSeparateWitir) {
                            add(OverlayOfficerInfo("Imam Witir", imamWitir, Icons.Default.Person))
                        }
                    }
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "SHOLAT\nTARAWIH & WITIR",
                        countdownLabel = "Sholat Tarawih & Witir",
                        stageContextBadge = "MALAM RAMADHAN KE-${snapshot.settings.ramadhan.currentNight}",
                        stageMessage = snapshot.settings.ramadhan.sholatMessage.ifBlank {
                            "Khusyuk dalam Melaksanakan Sholat"
                        },
                        officers = tarawihOfficers,
                        stageIcon = Icons.Default.NightsStay,
                        accentColor = Color(0xFF818CF8),
                        backgroundImagePath = snapshot.settings.media.sholatTarawihWitirImage.ifBlank {
                            snapshot.settings.media.ceramahTarawihImage
                        },
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu,
                        mainTitleFontSize = 112.sp,
                        mainTitleMaxLines = 2
                    )
                }

                AppState.IDUL_FITRI -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = true,
                        isInfoQurbanOnly = false,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.SHOLAT_IDUL_FITRI -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Sholat Idul Fitri",
                        stageMessage = snapshot.settings.idulFitri.sholatMessage.ifBlank {
                            "Gema Idul Fitri - Raih Kemenangan"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Imam",
                                name = snapshot.settings.idulFitri.petugas.imam.trim().ifBlank { "Ust. Fulan Imam" },
                                icon = Icons.Default.Person
                            )
                        ),
                        stageIcon = Icons.Default.AutoAwesome,
                        accentColor = Color(0xFF10B981),
                        backgroundImagePath = snapshot.settings.media.sholatIdulFitriImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.KHUTBAH_IDUL_FITRI -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Khutbah Idul Fitri",
                        stageMessage = snapshot.settings.idulFitri.khutbahMessage.ifBlank {
                            "Menyimak Khutbah Hari Raya"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Khotib",
                                name = snapshot.settings.idulFitri.petugas.khotib.trim().ifBlank { "Ust. Fulan Khotib" },
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                        ),
                        stageIcon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = Color(0xFFF59E0B),
                        backgroundImagePath = snapshot.settings.media.khutbahIdulFitriImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.IDUL_ADHA -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = false,
                        isInfoQurbanOnly = false,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.SHOLAT_IDUL_ADHA -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Sholat Idul Adha",
                        stageMessage = snapshot.settings.idulAdha.sholatMessage.ifBlank {
                            "Semangat Qurban dan Ketaatan"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Imam",
                                name = snapshot.settings.idulAdha.petugas.imam.trim().ifBlank { "Ust. Fulan Imam" },
                                icon = Icons.Default.Person
                            )
                        ),
                        stageIcon = Icons.Default.AutoAwesome,
                        accentColor = Color(0xFF10B981),
                        backgroundImagePath = snapshot.settings.media.sholatIdulAdhaImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.KHUTBAH_IDUL_ADHA -> {
                    SimpleStateOverlayFullscreen(
                        snapshot = snapshot,
                        stageTitle = "Khutbah Idul Adha",
                        stageMessage = snapshot.settings.idulAdha.khutbahMessage.ifBlank {
                            "Menyimak Khutbah Hari Raya"
                        },
                        officers = listOf(
                            OverlayOfficerInfo(
                                role = "Khotib",
                                name = snapshot.settings.idulAdha.petugas.khotib.trim().ifBlank { "Ust. Fulan Khotib" },
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                        ),
                        stageIcon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = Color(0xFFF59E0B),
                        backgroundImagePath = snapshot.settings.media.khutbahIdulAdhaImage,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.INFO_QURBAN -> {
                    HolidayAndQurbanFullscreen(
                        snapshot = snapshot,
                        isIdulFitri = false,
                        isInfoQurbanOnly = true,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.STANDBY_SLIDESHOW -> {
                    StandbySlideshowFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir
                    )
                }

                AppState.SCREENSAVER -> {
                    ScreenSaverFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.MASJID_QR -> {
                    MasjidQrFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.FINANCE_QR -> {
                    FinanceQrFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.ONLINE_FINANCE -> {
                    OnlineFinanceFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }

                AppState.LIVE -> {
                    YouTubeLiveFullscreen(
                        snapshot = snapshot,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu
                    )
                }
            }
        }

        // Top-Center Floating Control Pill only when Manual Simulation is active (hidden in normal Fullscreen)
        if (snapshot.tickResult.isManualOverride) {
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
