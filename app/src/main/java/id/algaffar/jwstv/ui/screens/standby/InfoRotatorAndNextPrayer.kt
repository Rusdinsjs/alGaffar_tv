package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.FastingInfoSummary
import id.algaffar.jwstv.domain.engine.IslamicEventItem
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.ui.theme.JwsThemePalette
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import kotlinx.coroutines.delay
import java.util.Locale

private enum class RotatorBadgeTone {
    CYAN,
    EMERALD,
    AMBER,
    BLUE,
    PURPLE,
    GRAY
}

private data class RotatorSlideUi(
    val key: String,
    val title: String,
    val content: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badgeTone: RotatorBadgeTone
)

private data class BadgeColorSpec(
    val background: Color,
    val text: Color,
    val border: Color
)

private fun badgeColorsFor(tone: RotatorBadgeTone, isLight: Boolean): BadgeColorSpec {
    return when (tone) {
        RotatorBadgeTone.CYAN -> BadgeColorSpec(
            background = Color(0xFF06B6D4).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFF0E7490) else Color(0xFF67E8F9),
            border = Color(0xFF06B6D4).copy(alpha = 0.35f)
        )
        RotatorBadgeTone.EMERALD -> BadgeColorSpec(
            background = Color(0xFF10B981).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFF047857) else Color(0xFF6EE7B7),
            border = Color(0xFF10B981).copy(alpha = 0.35f)
        )
        RotatorBadgeTone.AMBER -> BadgeColorSpec(
            background = Color(0xFFF59E0B).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFFB45309) else Color(0xFFFCD34D),
            border = Color(0xFFF59E0B).copy(alpha = 0.35f)
        )
        RotatorBadgeTone.BLUE -> BadgeColorSpec(
            background = Color(0xFF3B82F6).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFF1D4ED8) else Color(0xFF93C5FD),
            border = Color(0xFF3B82F6).copy(alpha = 0.35f)
        )
        RotatorBadgeTone.PURPLE -> BadgeColorSpec(
            background = Color(0xFFA855F7).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFF7E22CE) else Color(0xFFD8B4FE),
            border = Color(0xFFA855F7).copy(alpha = 0.35f)
        )
        RotatorBadgeTone.GRAY -> BadgeColorSpec(
            background = Color(0xFF6B7280).copy(alpha = if (isLight) 0.16f else 0.20f),
            text = if (isLight) Color(0xFF374151) else Color(0xFFD1D5DB),
            border = Color(0xFF6B7280).copy(alpha = 0.35f)
        )
    }
}

private data class ContentTypographySpec(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val maxLines: Int
)

/**
 * Auto-scaling font size helper mirroring `getFontSizeClass` in `InfoRotator.tsx`
 * for the `480 x 225 dp` card slot on the `1920 x 1080` logical canvas.
 */
private fun resolveContentTypography(text: String): ContentTypographySpec {
    val len = text.length
    return when {
        len > 55 -> ContentTypographySpec(fontSize = 18.sp, lineHeight = 23.sp, maxLines = 3)
        len > 40 -> ContentTypographySpec(fontSize = 21.sp, lineHeight = 26.sp, maxLines = 3)
        len > 35 -> ContentTypographySpec(fontSize = 24.sp, lineHeight = 29.sp, maxLines = 2)
        len > 20 -> ContentTypographySpec(fontSize = 30.sp, lineHeight = 35.sp, maxLines = 2)
        len > 15 -> ContentTypographySpec(fontSize = 36.sp, lineHeight = 41.sp, maxLines = 2)
        else -> ContentTypographySpec(fontSize = 44.sp, lineHeight = 48.sp, maxLines = 2)
    }
}

/**
 * Shared Card Shell & Animated Content Rotator mirroring
 * `jws-android-algaffar/src/components/Screens/Main/InfoRotator.tsx`.
 */
@Composable
private fun InfoRotatorCardFrame(
    slides: List<RotatorSlideUi>,
    palette: JwsThemePalette,
    transitionLabel: String,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(slides.size) {
        if (currentIndex >= slides.size) currentIndex = 0
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            currentIndex = (currentIndex + 1) % slides.size
        }
    }

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 225.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
    ) {
        // Subtle bottom accent ambient glow matching `bg-gradient-to-t from-[var(--color-accent)]/10 to-transparent`
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            palette.accent.copy(alpha = 0.14f)
                        )
                    )
                )
        )

        if (slides.isNotEmpty()) {
            val safeSlide = slides[currentIndex % slides.size]
            val isPerformanceMode = LocalPerformanceMode.current

            AnimatedContent(
                targetState = safeSlide,
                transitionSpec = {
                    if (isPerformanceMode) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (fadeIn(tween(500, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(500, easing = FastOutSlowInEasing)) { 14 } +
                            scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.95f))
                            .togetherWith(
                                fadeOut(tween(400, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { -14 } +
                                    scaleOut(tween(400, easing = FastOutSlowInEasing), targetScale = 0.95f)
                            )
                    }
                },
                label = transitionLabel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) { item ->
                val badgeColors = badgeColorsFor(item.badgeTone, palette.isLight)
                val typography = resolveContentTypography(item.content)
                val contentTextColor = if (palette.isLight) palette.textPrimary else Color.White

                Box(modifier = Modifier.fillMaxSize()) {
                    // Row 1: Top-Left Icon Badge (`w-12 h-12 rounded-xl bg-white/5 border border-white/10`)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .background(item.iconColor.copy(alpha = 0.20f))
                            .border(
                                width = 1.dp,
                                color = if (palette.isLight) item.iconColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Centered 3-Tier Hierarchy: Header Label -> Main Headline -> Subtitle Pill Badge
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Row 2: Header Label (Centered, clear of top-left icon box)
                        Text(
                            text = item.title.uppercase(Locale.getDefault()),
                            color = palette.textSecondary.copy(alpha = 0.85f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 54.dp, end = 54.dp)
                        )

                        // Row 3: Main Content Headline (Centered, auto-scaling font size)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.content,
                                color = contentTextColor,
                                fontSize = typography.fontSize,
                                lineHeight = typography.lineHeight,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp,
                                textAlign = TextAlign.Center,
                                maxLines = typography.maxLines,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    shadow = if (palette.isLight) null else Shadow(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 10f
                                    ),
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }

                        // Row 4: Subtitle / Countdown Pill Badge (Centered)
                        if (item.subtitle.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(badgeColors.background)
                                    .border(1.dp, badgeColors.border, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.subtitle.uppercase(Locale.getDefault()),
                                    color = badgeColors.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
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
 * Card 1: Rotates Mosque Announcements & Program Information (`mosqueInfoList`).
 * Mirrors `InfoRotator` (`type="settings"`) in `jws-android-algaffar/src/components/Screens/Main/InfoRotator.tsx`.
 */
@Composable
fun MosqueInfoRotatorCard(
    infoItems: List<InfoCardItem>,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val slides = remember(infoItems) {
        infoItems.filter { it.enabled }.map { item ->
            RotatorSlideUi(
                key = "info-${item.id}-${item.title}-${item.content}",
                title = item.title.ifBlank { "INFO MASJID" },
                content = item.content,
                subtitle = item.subtitle,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconColor = Color(0xFF22D3EE), // Tailwind cyan-400
                badgeTone = RotatorBadgeTone.CYAN
            )
        }
    }

    InfoRotatorCardFrame(
        slides = slides,
        palette = palette,
        transitionLabel = "MosqueInfoRotatorTransition",
        modifier = modifier
    )
}

/**
 * Card 2: Rotates between Sunnah Fasting Reminders (Today or Tomorrow only when active)
 * and Upcoming Islamic Holidays / Custom Agendas (PHBI).
 * Mirrors `InfoRotator` (`type="islamic"`) in `jws-android-algaffar/src/components/Screens/Main/InfoRotator.tsx`.
 */
@Composable
fun IslamicFastingAndPhbiRotatorCard(
    fastingInfo: FastingInfoSummary,
    upcomingEvents: List<IslamicEventItem>,
    enableSunnahFasting: Boolean = true,
    isRamadhanMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    val slides = remember(fastingInfo, upcomingEvents, enableSunnahFasting, isRamadhanMode) {
        buildList {
            // 1. Sunnah Fasting Info (Today takes priority over Tomorrow; never shows "Tidak ada puasa")
            if (enableSunnahFasting) {
                val activeFasting: Pair<Boolean, String>? = when {
                    fastingInfo.today.isFasting && fastingInfo.today.isSunnah -> true to fastingInfo.today.title
                    fastingInfo.tomorrow.isFasting && fastingInfo.tomorrow.isSunnah -> false to fastingInfo.tomorrow.title
                    else -> null
                }

                if (activeFasting != null) {
                    val (isToday, fastingName) = activeFasting
                    val forceShowSyawal = fastingName.contains("Syawal", ignoreCase = true)
                    if (!isRamadhanMode || forceShowSyawal) {
                        add(
                            RotatorSlideUi(
                                key = "fasting-${if (isToday) "today" else "tomorrow"}-$fastingName",
                                title = if (isToday) "PUASA HARI INI" else "PUASA BESOK",
                                content = fastingName,
                                subtitle = if (isToday) "Sedang Berlangsung" else "Besok",
                                icon = Icons.Default.NightsStay,
                                iconColor = Color(0xFF34D399), // Tailwind emerald-400
                                badgeTone = if (isToday) RotatorBadgeTone.EMERALD else RotatorBadgeTone.AMBER
                            )
                        )
                    }
                }
            }

            // 2. Upcoming Islamic Events & Custom Agendas
            upcomingEvents.forEach { event ->
                if (event.name.contains("Ramadhan Hari ke-", ignoreCase = true)) return@forEach

                val (subtitle, badgeTone) = when {
                    event.daysUntil == -1 -> event.customSubtitle.ifBlank { "Agenda Masjid" } to RotatorBadgeTone.PURPLE
                    event.daysUntil == 0 -> "Hari Ini" to RotatorBadgeTone.EMERALD
                    event.daysUntil == 1 -> "Besok" to RotatorBadgeTone.AMBER
                    else -> "${event.daysUntil} Hari Lagi" to RotatorBadgeTone.BLUE
                }

                add(
                    RotatorSlideUi(
                        key = "event-${event.name}-${event.daysUntil}-${event.customSubtitle}",
                        title = if (event.daysUntil == 0) "INFO TERKINI" else "AGENDA ISLAMI",
                        content = event.name,
                        subtitle = subtitle,
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFBBF24), // Tailwind amber-400
                        badgeTone = badgeTone
                    )
                )
            }

            // 3. Fallback so the Islamic card is never blank (matching InfoRotator.tsx)
            if (isEmpty()) {
                add(
                    RotatorSlideUi(
                        key = "fallback-no-agenda",
                        title = "AGENDA ISLAMI",
                        content = "Tidak ada agenda terdekat",
                        subtitle = "Bulan ini",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFF9CA3AF), // Tailwind gray-400
                        badgeTone = RotatorBadgeTone.GRAY
                    )
                )
            }
        }
    }

    InfoRotatorCardFrame(
        slides = slides,
        palette = palette,
        transitionLabel = "IslamicRotatorTransition",
        modifier = modifier
    )
}

/**
 * Formats remaining seconds to match `PrayerTimeContext.tsx` in `jws-android-algaffar`:
 * - `diff > 0` and `h > 0` -> `H:MM:SS` (e.g. `2:15:09`, `10:05:30`)
 * - `diff > 0` and `h == 0` -> `MM:SS` (e.g. `45:09`, `05:30`)
 * - `diff <= 0` -> `00:00`
 */
internal fun formatNextPrayerCountdown(secondsRemaining: Long): String {
    if (secondsRemaining <= 0L) return "00:00"
    val hours = secondsRemaining / 3600L
    val minutes = (secondsRemaining % 3600L) / 60L
    val seconds = secondsRemaining % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Card 3: Real-time Countdown to Next Obligatory Prayer.
 * Mirrors `jws-android-algaffar/src/components/Screens/Main/NextPrayer.tsx`.
 */
@Composable
fun NextPrayerCountdownCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val nextPrayer = schedule.nextPrayer
    val formattedCountdown = formatNextPrayerCountdown(schedule.secondsToNextPrayer)
    val countdownTextColor = if (palette.isLight) palette.textPrimary else Color.White

    // Dynamically tune jumbo countdown font size so 8-char ("11:59:59") fits cleanly in 440dp width
    val countdownFontSize = when {
        formattedCountdown.length >= 8 -> 78.sp
        formattedCountdown.length == 7 -> 84.sp
        else -> 92.sp
    }

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 225.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Centered Ambient Glow (`bg-[var(--color-accent)]/15 blur-[60px] rounded-full w-56 h-56`)
        if (!isPerformanceMode) {
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.22f),
                                palette.accent.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Top Label (`MENUJU WAKTU`)
            Text(
                text = "MENUJU WAKTU",
                color = palette.textSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            // Middle: Next Prayer Name (`SUBUH` / `DZUHUR` / `JUM'AT` / `ASHAR` / `MAGHRIB` / `ISYA`)
            Text(
                text = nextPrayer.label.uppercase(Locale.getDefault()),
                color = palette.accent,
                fontSize = 52.sp,
                lineHeight = 54.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = if (palette.isLight || isPerformanceMode) null else Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        offset = Offset(0f, 3f),
                        blurRadius = 10f
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            // Bottom: Jumbo Countdown Timer (`H:MM:SS` or `MM:SS`, tabular numbers)
            Text(
                text = formattedCountdown,
                color = countdownTextColor,
                fontSize = countdownFontSize,
                lineHeight = countdownFontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFeatureSettings = "tnum",
                    shadow = if (palette.isLight || isPerformanceMode) null else Shadow(
                        color = Color.Black.copy(alpha = 0.60f),
                        offset = Offset(0f, 4f),
                        blurRadius = 14f
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}
