package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.PrayerTimeItem
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay

private val SmoothCarouselEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Displays 4 visible prayer cards at a time out of the 8 daily prayer times
 * (`Imsak`, `Subuh`, `Syuruq`, `Dhuha`, `Dzuhur/Jum'at`, `Ashar`, `Maghrib`, `Isya`)
 * in a seamless GPU-accelerated circular auto-scrolling carousel.
 *
 * Always animates smoothly regardless of `performanceMode`.
 */
@Composable
fun PrayerTimesCarousel(
    schedule: DailyPrayerSchedule,
    scrollIntervalSeconds: Int,
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    val items = schedule.items
    val totalCount = items.size.coerceAtLeast(1)
    val visibleCount = 4

    var startIndex by remember { mutableIntStateOf(0) }
    val slideProgress = remember { Animatable(0f) }
    val intervalMs = (scrollIntervalSeconds.coerceIn(2, 30) * 1000L)

    LaunchedEffect(intervalMs, totalCount) {
        slideProgress.snapTo(0f)
        if (totalCount <= visibleCount) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = SmoothCarouselEasing)
            )
            startIndex = (startIndex + 1) % totalCount
            slideProgress.snapTo(0f)
        }
    }

    // Render 5 cards (4 visible + 1 incoming) so the strip glides smoothly by 1 full card slot
    val windowItems = remember(items, startIndex) {
        List(visibleCount + 1) { offset ->
            items[(startIndex + offset) % totalCount]
        }
    }

    val density = LocalDensity.current
    if (isVertical) {
        val cardHeightPx = with(density) { 225.dp.toPx() }
        Box(
            modifier = modifier
                .size(width = 480.dp, height = 900.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .graphicsLayer {
                        translationY = -slideProgress.value * cardHeightPx
                    }
            ) {
                windowItems.forEach { prayerItem ->
                    PrayerTimeCard(
                        item = prayerItem,
                        modifier = Modifier.size(width = 480.dp, height = 225.dp)
                    )
                }
            }
        }
    } else {
        val cardWidthPx = with(density) { 480.dp.toPx() }
        Box(
            modifier = modifier
                .size(width = 1920.dp, height = 225.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                modifier = Modifier
                    .height(225.dp)
                    .wrapContentWidth(align = Alignment.Start, unbounded = true)
                    .graphicsLayer {
                        translationX = -slideProgress.value * cardWidthPx
                    }
            ) {
                windowItems.forEach { prayerItem ->
                    PrayerTimeCard(
                        item = prayerItem,
                        modifier = Modifier.size(width = 480.dp, height = 225.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimeCard(
    item: PrayerTimeItem,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isNext = item.isNext

    val cardBgBrush = if (isNext) {
        Brush.linearGradient(
            colors = listOf(
                palette.accent.copy(alpha = 0.92f),
                palette.accent.copy(alpha = 0.72f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                palette.bgCard,
                palette.bgPrimary.copy(alpha = 0.85f)
            )
        )
    }

    val borderColor = if (isNext) palette.accentSecondary else palette.border
    val borderWidth = if (isNext) 2.5.dp else 1.5.dp
    val titleColor = if (isNext) palette.bgPrimary else palette.textPrimary
    val subColor = if (isNext) palette.bgPrimary.copy(alpha = 0.8f) else palette.textSecondary
    val timeColor = if (isNext) palette.bgPrimary else palette.accent

    Box(
        modifier = modifier
            .size(width = 480.dp, height = 225.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBgBrush)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.label.uppercase(),
                    color = titleColor,
                    fontSize = 56.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isNext) {
                        NextPrayerPillBadge(
                            bgColor = palette.bgPrimary,
                            textColor = palette.accentSecondary
                        )
                    }

                    Text(
                        text = item.id.arabicName,
                        color = subColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.formattedTime,
                    color = timeColor,
                    fontSize = 112.sp,
                    lineHeight = 112.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFeatureSettings = "tnum",
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun NextPrayerPillBadge(
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "BERIKUTNYA",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
