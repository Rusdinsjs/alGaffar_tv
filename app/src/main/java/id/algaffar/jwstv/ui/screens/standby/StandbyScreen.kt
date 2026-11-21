package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.foundation.layout.Box
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
 * - Top Row (`1920 x 180 dp`, 1.5x height):
 *   - `HORIZONTAL` & `VERTICAL_RIGHT`: Left `HeaderBar` (`1440 x 180 dp`: Identitas -> Tanggal rata kanan) + Right `HeaderClockCard` (`480 x 180 dp`)
 *   - `VERTICAL_LEFT`: Left `HeaderClockCard` (`480 x 180 dp`) + Right `HeaderBar` (`1440 x 180 dp`: Tanggal rata kiri -> Identitas)
 * - `MAIN SLIDE CONTAINER`: `1440 x 675 dp` (with System Greeting & Big Analog Clock default slides)
 * - `KARTU INFO 1`, `KARTU INFO 2`, `NEXT PRAYER`, and each `KARTU PRAYER TIMES`: `480 x 225 dp`
 */
@Composable
fun StandbyScreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    isLogoMenuOpen: Boolean,
    onToggleLogoMenu: () -> Unit,
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

    Box(
        modifier = modifier.size(width = 1920.dp, height = 1080.dp)
    ) {
        Column(
            modifier = Modifier.size(width = 1920.dp, height = 1080.dp)
        ) {
            // 1. Top Area Row (1920 x 180 dp): Header (1440x180) + Clock Card (480x180)
            Row(
                modifier = Modifier.size(width = 1920.dp, height = 180.dp)
            ) {
                if (isVerticalLeft) {
                    // Vertical Left: 1. Jam/Clock (480dp) -> 2. Header (Tanggal, Identitas {Nama/Alamat, Logo}) (1440dp)
                    HeaderClockCard(
                        now = snapshot.now,
                        clockStyle = settings.brand.clockStyle,
                        modifier = Modifier.size(width = 480.dp, height = 180.dp)
                    )
                    HeaderBar(
                        now = snapshot.now,
                        brand = settings.brand,
                        hijriDate = snapshot.hijriDate,
                        assetsRootDir = assetsRootDir,
                        isReversed = true,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onToggleLogoMenu,
                        modifier = Modifier.size(width = 1440.dp, height = 180.dp)
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
                        onLogoClick = onToggleLogoMenu,
                        modifier = Modifier.size(width = 1440.dp, height = 180.dp)
                    )
                    HeaderClockCard(
                        now = snapshot.now,
                        clockStyle = settings.brand.clockStyle,
                        modifier = Modifier.size(width = 480.dp, height = 180.dp)
                    )
                }
            }

            // 2. Main Body Area (1920 x 900 dp) according to LayoutPosition
            when (effectiveLayout) {
                LayoutPosition.HORIZONTAL -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 675.dp)
                    ) {
                        MainSlideContainer(
                            now = snapshot.now,
                            today = snapshot.schedule.date,
                            settings = settings,
                            assetsRootDir = assetsRootDir,
                            modifier = Modifier.size(width = 1440.dp, height = 675.dp)
                        )

                        Column(
                            modifier = Modifier.size(width = 480.dp, height = 675.dp)
                        ) {
                            MosqueInfoRotatorCard(
                                infoItems = settings.mosqueInfoList,
                                modifier = Modifier.size(width = 480.dp, height = 225.dp)
                            )
                            IslamicFastingAndPhbiRotatorCard(
                                fastingInfo = snapshot.fastingInfo,
                                upcomingEvents = snapshot.upcomingEvents,
                                enableSunnahFasting = settings.infoSettings.enableSunnahFasting,
                                isRamadhanMode = settings.ramadhan.isRamadhan,
                                modifier = Modifier.size(width = 480.dp, height = 225.dp)
                            )
                            NextPrayerCountdownCard(
                                schedule = snapshot.schedule,
                                modifier = Modifier.size(width = 480.dp, height = 225.dp)
                            )
                        }
                    }

                    PrayerTimesCarousel(
                        schedule = snapshot.schedule,
                        scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                        isVertical = false,
                        modifier = Modifier.size(width = 1920.dp, height = 225.dp)
                    )
                }

                LayoutPosition.VERTICAL_RIGHT -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 900.dp)
                    ) {
                        Column(
                            modifier = Modifier.size(width = 1440.dp, height = 900.dp)
                        ) {
                            MainSlideContainer(
                                now = snapshot.now,
                                today = snapshot.schedule.date,
                                settings = settings,
                                assetsRootDir = assetsRootDir,
                                modifier = Modifier.size(width = 1440.dp, height = 675.dp)
                            )

                            Row(
                                modifier = Modifier.size(width = 1440.dp, height = 225.dp)
                            ) {
                                MosqueInfoRotatorCard(
                                    infoItems = settings.mosqueInfoList,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                                IslamicFastingAndPhbiRotatorCard(
                                    fastingInfo = snapshot.fastingInfo,
                                    upcomingEvents = snapshot.upcomingEvents,
                                    enableSunnahFasting = settings.infoSettings.enableSunnahFasting,
                                    isRamadhanMode = settings.ramadhan.isRamadhan,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                                NextPrayerCountdownCard(
                                    schedule = snapshot.schedule,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                            }
                        }

                        PrayerTimesCarousel(
                            schedule = snapshot.schedule,
                            scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                            isVertical = true,
                            modifier = Modifier.size(width = 480.dp, height = 900.dp)
                        )
                    }
                }

                LayoutPosition.VERTICAL_LEFT -> {
                    Row(
                        modifier = Modifier.size(width = 1920.dp, height = 900.dp)
                    ) {
                        PrayerTimesCarousel(
                            schedule = snapshot.schedule,
                            scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                            isVertical = true,
                            modifier = Modifier.size(width = 480.dp, height = 900.dp)
                        )

                        Column(
                            modifier = Modifier.size(width = 1440.dp, height = 900.dp)
                        ) {
                            MainSlideContainer(
                                now = snapshot.now,
                                today = snapshot.schedule.date,
                                settings = settings,
                                assetsRootDir = assetsRootDir,
                                modifier = Modifier.size(width = 1440.dp, height = 675.dp)
                            )

                            Row(
                                modifier = Modifier.size(width = 1440.dp, height = 225.dp)
                            ) {
                                MosqueInfoRotatorCard(
                                    infoItems = settings.mosqueInfoList,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                                IslamicFastingAndPhbiRotatorCard(
                                    fastingInfo = snapshot.fastingInfo,
                                    upcomingEvents = snapshot.upcomingEvents,
                                    enableSunnahFasting = settings.infoSettings.enableSunnahFasting,
                                    isRamadhanMode = settings.ramadhan.isRamadhan,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                                NextPrayerCountdownCard(
                                    schedule = snapshot.schedule,
                                    modifier = Modifier.size(width = 480.dp, height = 225.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
