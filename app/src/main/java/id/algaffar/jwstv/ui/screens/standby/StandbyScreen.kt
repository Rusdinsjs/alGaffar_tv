package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import java.io.File

/**
 * Main Standby Dashboard Screen (`1920 x 1080 dp` modular grid) supporting all 3 Layout Positions:
 * - `HEADER`: `1920 x 120 dp`
 * - `MAIN SLIDE CONTAINER`: `1440 x 720 dp` (with floating `OverlayRunningText` at bottom-left)
 * - `KARTU INFO 1`, `KARTU INFO 2`, `NEXT PRAYER`, and each `KARTU PRAYER TIMES`: `480 x 240 dp`
 *
 * Modes:
 * - `LayoutPosition.HORIZONTAL`: Top `1440x720` Slide + Right `480x720` (3x `480x240` Info/NextPrayer), Bottom `1920x240` (4x `480x240` Prayer Cards)
 * - `LayoutPosition.VERTICAL_RIGHT`: Left `1440x960` (`1440x720` Slide + Bottom 3x `480x240` Info/NextPrayer), Right `480x960` (4x `480x240` Prayer Cards)
 * - `LayoutPosition.VERTICAL_LEFT`: Left `480x960` (4x `480x240` Prayer Cards), Right `1440x960` (`1440x720` Slide + Bottom 3x `480x240` Info/NextPrayer)
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

    Column(
        modifier = modifier.size(width = 1920.dp, height = 1080.dp)
    ) {
        // 1. Top Header Bar (1920 x 120 dp)
        HeaderBar(
            now = snapshot.now,
            brand = settings.brand,
            hijriDate = snapshot.hijriDate,
            assetsRootDir = assetsRootDir,
            onCycleTheme = onCycleTheme,
            onCycleLayout = onCycleLayout,
            onCycleClockStyle = onCycleClockStyle,
            onOpenDebugModal = onOpenDebugModal,
            onOpenSettingsModal = onOpenSettingsModal,
            modifier = Modifier.size(width = 1920.dp, height = 120.dp)
        )

        // 2. Main Body Area (1920 x 960 dp) according to LayoutPosition
        when (effectiveLayout) {
            LayoutPosition.HORIZONTAL -> {
                // Middle Row (1920 x 720 dp): Left 1440x720 Slide Container + Right 480x720 Stacked Cards
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

                // Bottom Row (1920 x 240 dp): 4 Horizontal Prayer Cards (480 x 240 dp each)
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
                    // Left Column (1440 x 960 dp): Top 1440x720 Slide + Bottom Row 1440x240 (3x 480x240 Cards)
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

                    // Right Column (480 x 960 dp): 4 Vertical Prayer Cards (480 x 240 dp each)
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
                    // Left Column (480 x 960 dp): 4 Vertical Prayer Cards (480 x 240 dp each)
                    PrayerTimesCarousel(
                        schedule = snapshot.schedule,
                        scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                        isVertical = true,
                        modifier = Modifier.size(width = 480.dp, height = 960.dp)
                    )

                    // Right Column (1440 x 960 dp): Top 1440x720 Slide + Bottom Row 1440x240 (3x 480x240 Cards)
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
}

