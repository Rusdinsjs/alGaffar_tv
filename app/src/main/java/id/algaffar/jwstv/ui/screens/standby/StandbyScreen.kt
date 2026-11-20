package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import java.io.File

/**
 * Main Standby Dashboard Screen (`1920 x 1080 dp` modular grid) supporting all 3 Layout Positions:
 * - Top Row (`1920 x 120 dp`):
 *   - `HORIZONTAL` & `VERTICAL_RIGHT`: Left `HeaderBar` (`1440 x 120 dp`: Identitas -> Tanggal) + Right `HeaderClockCard` (`480 x 120 dp`)
 *   - `VERTICAL_LEFT`: Left `HeaderClockCard` (`480 x 120 dp`) + Right `HeaderBar` (`1440 x 120 dp`: Tanggal -> Identitas)
 * - `MAIN SLIDE CONTAINER`: `1440 x 720 dp` (with floating `OverlayRunningText` at bottom-left)
 * - `KARTU INFO 1`, `KARTU INFO 2`, `NEXT PRAYER`, and each `KARTU PRAYER TIMES`: `480 x 240 dp`
 */
@Composable
fun StandbyScreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    onCycleTheme: () -> Unit,
    onCycleLayout: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onOpenDebugModal: () -> Unit,
    onOpenSettingsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = snapshot.settings
    val effectiveLayout = remember(
        settings.layoutSettings.position,
        settings.layoutSettings.autoRotateLayout,
        snapshot.now.hour
    ) {
        if (settings.layoutSettings.autoRotateLayout) {
            val block = (snapshot.now.hour / 4) % 3
            LayoutPosition.entries[block]
        } else {
            settings.layoutSettings.position
        }
    }

    val isVerticalLeft = (effectiveLayout == LayoutPosition.VERTICAL_LEFT)
    var isLogoMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.size(width = 1920.dp, height = 1080.dp)
    ) {
        Column(
            modifier = Modifier.size(width = 1920.dp, height = 1080.dp)
        ) {
            // 1. Top Area Row (1920 x 120 dp): Header (1440x120) + Clock Card (480x120)
            Row(
                modifier = Modifier.size(width = 1920.dp, height = 120.dp)
            ) {
                if (isVerticalLeft) {
                    // Vertical Left: 1. Jam/Clock (480dp) -> 2. Header (Tanggal, Identitas {Nama/Alamat, Logo}) (1440dp)
                    HeaderClockCard(
                        now = snapshot.now,
                        clockStyle = settings.brand.clockStyle,
                        modifier = Modifier.size(width = 480.dp, height = 120.dp)
                    )
                    HeaderBar(
                        now = snapshot.now,
                        brand = settings.brand,
                        hijriDate = snapshot.hijriDate,
                        assetsRootDir = assetsRootDir,
                        isReversed = true,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = { isLogoMenuOpen = !isLogoMenuOpen },
                        modifier = Modifier.size(width = 1440.dp, height = 120.dp)
                    )
                } else {
                    // Horizontal & Vertical Right: 1. Header (Identitas {Logo, Nama/Alamat}, Tanggal) (1440dp) -> 2. Jam/Clock (480dp)
                    HeaderBar(
                        now = snapshot.now,
                        brand = settings.brand,
                        hijriDate = snapshot.hijriDate,
                        assetsRootDir = assetsRootDir,
                        isReversed = false,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = { isLogoMenuOpen = !isLogoMenuOpen },
                        modifier = Modifier.size(width = 1440.dp, height = 120.dp)
                    )
                    HeaderClockCard(
                        now = snapshot.now,
                        clockStyle = settings.brand.clockStyle,
                        modifier = Modifier.size(width = 480.dp, height = 120.dp)
                    )
                }
            }

            // 2. Main Body Area (1920 x 960 dp) according to LayoutPosition
            when (effectiveLayout) {
                LayoutPosition.HORIZONTAL -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 720.dp)
                    ) {
                        MainSlideContainer(
                            today = snapshot.schedule.date,
                            settings = settings,
                            assetsRootDir = assetsRootDir,
                            modifier = Modifier.size(width = 1440.dp, height = 720.dp)
                        )

                        Column(
                            modifier = Modifier.size(width = 480.dp, height = 720.dp)
                        ) {
                            MosqueInfoRotatorCard(
                                infoItems = settings.mosqueInfoList,
                                modifier = Modifier.size(width = 480.dp, height = 240.dp)
                            )
                            IslamicFastingAndPhbiRotatorCard(
                                fastingInfo = snapshot.fastingInfo,
                                upcomingEvents = snapshot.upcomingEvents,
                                modifier = Modifier.size(width = 480.dp, height = 240.dp)
                            )
                            NextPrayerCountdownCard(
                                schedule = snapshot.schedule,
                                modifier = Modifier.size(width = 480.dp, height = 240.dp)
                            )
                        }
                    }

                    PrayerTimesCarousel(
                        schedule = snapshot.schedule,
                        scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                        isVertical = false,
                        modifier = Modifier.size(width = 1920.dp, height = 240.dp)
                    )
                }

                LayoutPosition.VERTICAL_RIGHT -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 960.dp)
                    ) {
                        Column(
                            modifier = Modifier.size(width = 1440.dp, height = 960.dp)
                        ) {
                            MainSlideContainer(
                                today = snapshot.schedule.date,
                                settings = settings,
                                assetsRootDir = assetsRootDir,
                                modifier = Modifier.size(width = 1440.dp, height = 720.dp)
                            )

                            Row(
                                modifier = Modifier.size(width = 1440.dp, height = 240.dp)
                            ) {
                                MosqueInfoRotatorCard(
                                    infoItems = settings.mosqueInfoList,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                                IslamicFastingAndPhbiRotatorCard(
                                    fastingInfo = snapshot.fastingInfo,
                                    upcomingEvents = snapshot.upcomingEvents,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                                NextPrayerCountdownCard(
                                    schedule = snapshot.schedule,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                            }
                        }

                        PrayerTimesCarousel(
                            schedule = snapshot.schedule,
                            scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                            isVertical = true,
                            modifier = Modifier.size(width = 480.dp, height = 960.dp)
                        )
                    }
                }

                LayoutPosition.VERTICAL_LEFT -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 960.dp)
                    ) {
                        PrayerTimesCarousel(
                            schedule = snapshot.schedule,
                            scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                            isVertical = true,
                            modifier = Modifier.size(width = 480.dp, height = 960.dp)
                        )

                        Column(
                            modifier = Modifier.size(width = 1440.dp, height = 960.dp)
                        ) {
                            MainSlideContainer(
                                today = snapshot.schedule.date,
                                settings = settings,
                                assetsRootDir = assetsRootDir,
                                modifier = Modifier.size(width = 1440.dp, height = 720.dp)
                            )

                            Row(
                                modifier = Modifier.size(width = 1440.dp, height = 240.dp)
                            ) {
                                MosqueInfoRotatorCard(
                                    infoItems = settings.mosqueInfoList,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                                IslamicFastingAndPhbiRotatorCard(
                                    fastingInfo = snapshot.fastingInfo,
                                    upcomingEvents = snapshot.upcomingEvents,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                                NextPrayerCountdownCard(
                                    schedule = snapshot.schedule,
                                    modifier = Modifier.size(width = 480.dp, height = 240.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Quick Action Menu triggered by clicking the Mosque Logo
        if (isLogoMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isLogoMenuOpen = false }
            )

            LogoQuickMenuCard(
                onCycleTheme = onCycleTheme,
                onCycleLayout = onCycleLayout,
                onCycleClockStyle = onCycleClockStyle,
                onOpenDebugModal = onOpenDebugModal,
                onOpenSettingsModal = onOpenSettingsModal,
                onDismiss = { isLogoMenuOpen = false },
                modifier = Modifier
                    .align(if (isVerticalLeft) Alignment.TopEnd else Alignment.TopStart)
                    .padding(
                        top = 122.dp,
                        start = if (isVerticalLeft) 0.dp else 18.dp,
                        end = if (isVerticalLeft) 18.dp else 0.dp
                    )
            )
        }
    }
}
