package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EventAvailable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.FastingInfoSummary
import id.algaffar.jwstv.domain.engine.IslamicEventItem
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Card 1: Rotates Mosque Announcements & Program Information (`mosqueInfoList`).
 * Mirrors the first `InfoRotator` in `src/components/Screens/Main/InfoRotator.tsx`.
 */
@Composable
fun MosqueInfoRotatorCard(
    infoItems: List<InfoCardItem>,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val activeItems = remember(infoItems) {
        infoItems.filter { it.enabled }.ifEmpty {
            listOf(
                InfoCardItem(
                    id = "default-1",
                    title = "Informasi Masjid Al-Gaffar",
                    content = "Mari makmurkan masjid dengan sholat fardhu berjamaah tepat pada waktunya."
                )
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(activeItems.size) {
        while (true) {
            delay(8_000L)
            currentIndex = (currentIndex + 1) % activeItems.size
        }
    }

    val safeIndex = currentIndex % activeItems.size
    val currentItem = activeItems[safeIndex]

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 240.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "INFO & KEGIATAN MASJID",
                        color = palette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    activeItems.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == safeIndex) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == safeIndex) palette.accent
                                    else palette.textSecondary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = currentItem,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "MosqueInfoTransition"
            ) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.content,
                        color = palette.textSecondary,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Card 2: Rotates between Sunnah/Wajib Fasting Reminders (Today & Tomorrow) and
 * Upcoming Islamic Holidays (PHBI within 60 days).
 * Mirrors the second `InfoRotator` in `src/components/Screens/Main/InfoRotator.tsx`.
 */
@Composable
fun IslamicFastingAndPhbiRotatorCard(
    fastingInfo: FastingInfoSummary,
    upcomingEvents: List<IslamicEventItem>,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current

    // Slide 0: Today's Fasting, Slide 1: Tomorrow's Fasting, Slide 2+: Upcoming PHBI
    val totalSlides = 2 + upcomingEvents.take(3).size
    var slideIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(totalSlides) {
        while (true) {
            delay(7_000L)
            slideIdx = (slideIdx + 1) % totalSlides
        }
    }

    val safeSlide = slideIdx % totalSlides

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 240.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (safeSlide < 2) Icons.Default.AutoAwesome else Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = palette.accentSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (safeSlide == 0) "AMALAN PUASA HARI INI"
                        else if (safeSlide == 1) "PERSIAPAN PUASA BESOK"
                        else "HARI BESAR ISLAM TERDEKAT",
                        color = palette.accentSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (idx in 0 until totalSlides) {
                        Box(
                            modifier = Modifier
                                .size(if (idx == safeSlide) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == safeSlide) palette.accentSecondary
                                    else palette.textSecondary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = safeSlide,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "IslamicInfoTransition"
            ) { idx ->
                when (idx) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = fastingInfo.today.title,
                                color = palette.textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = fastingInfo.today.description,
                                color = palette.textSecondary,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Besok: ${fastingInfo.tomorrow.title}",
                                color = palette.textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = fastingInfo.tomorrow.description,
                                color = palette.textSecondary,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    else -> {
                        val event = upcomingEvents.getOrNull(idx - 2)
                        if (event != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = event.name,
                                    color = palette.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (event.daysUntil == 0) {
                                        "Berlangsung Hari Ini!"
                                    } else {
                                        "Insya Allah ${event.daysUntil} hari lagi (${event.gregorianDate})"
                                    },
                                    color = palette.accent,
                                    fontSize = 16.sp,
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
 * Card 3: Real-time Countdown to Next Prayer (`NextPrayer.tsx`).
 */
@Composable
fun NextPrayerCountdownCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val nextPrayer = schedule.nextPrayer
    val totalSec = schedule.secondsToNextPrayer.coerceAtLeast(0L)

    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    val formattedCountdown = String.format(Locale.US, "-%02d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 240.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.bgCard,
                        palette.accent.copy(alpha = 0.22f)
                    )
                )
            )
            .border(2.dp, palette.accent.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MENUJU WAKTU ${nextPrayer.label.uppercase()}",
                        color = palette.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Pukul ${nextPrayer.formattedTime}",
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formattedCountdown,
                    color = palette.textPrimary,
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = nextPrayer.id.arabicName,
                    color = palette.accentSecondary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
