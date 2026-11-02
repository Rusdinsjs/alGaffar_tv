package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.PrayerTimeItem
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay

/**
 * Displays 4 visible prayer cards at a time out of the 8 daily prayer times
 * (`Imsak`, `Subuh`, `Syuruq`, `Dhuha`, `Dzuhur/Jum'at`, `Ashar`, `Maghrib`, `Isya`)
 * in a seamless circular auto-scrolling carousel.
 *
 * Supports both Horizontal Bar (bottom of `HORIZONTAL` layout) and Vertical Sidebar
 * (left/right of `VERTICAL_LEFT` and `VERTICAL_RIGHT` layouts).
 * Mirrors `src/components/Screens/PrayerTimes/PrayerTimesList.tsx`.
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
    val intervalMs = (scrollIntervalSeconds.coerceIn(2, 30) * 1000L)

    LaunchedEffect(intervalMs, totalCount) {
        while (true) {
            delay(intervalMs)
            startIndex = (startIndex + 1) % totalCount
        }
    }

    val visibleItems = remember(items, startIndex) {
        List(visibleCount) { offset ->
            items[(startIndex + offset) % totalCount]
        }
    }

    if (isVertical) {
        Box(
            modifier = modifier.size(width = 480.dp, height = 960.dp)
        ) {
            AnimatedContent(
                targetState = startIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (slideInVertically(tween(550)) { h -> h / 3 } + fadeIn(tween(450)))
                        .togetherWith(slideOutVertically(tween(550)) { h -> -h / 3 } + fadeOut(tween(350)))
                        .using(SizeTransform(clip = false))
                },
                label = "VerticalPrayerCarousel"
            ) { idx ->
                val currentWindow = List(visibleCount) { offset ->
                    items[(idx + offset) % totalCount]
                }
                Column(
                    modifier = Modifier.size(width = 480.dp, height = 960.dp)
                ) {
                    currentWindow.forEach { prayerItem ->
                        PrayerTimeCard(
                            item = prayerItem,
                            isVerticalLayout = true,
                            modifier = Modifier.size(width = 480.dp, height = 240.dp)
                        )
                    }
                }
            }

            // Carousel pagination dots (8 dots) overlaid at bottom-center
            CarouselPaginationIndicator(
                totalCount = totalCount,
                activeIndex = startIndex,
                onSelectIndex = { startIndex = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }
    } else {
        Box(
            modifier = modifier.size(width = 1920.dp, height = 240.dp)
        ) {
            AnimatedContent(
                targetState = startIndex,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (slideInHorizontally(tween(550)) { w -> w / 4 } + fadeIn(tween(450)))
                        .togetherWith(slideOutHorizontally(tween(550)) { w -> -w / 4 } + fadeOut(tween(350)))
                        .using(SizeTransform(clip = false))
                },
                label = "HorizontalPrayerCarousel"
            ) { idx ->
                val currentWindow = List(visibleCount) { offset ->
                    items[(idx + offset) % totalCount]
                }
                Row(
                    modifier = Modifier.size(width = 1920.dp, height = 240.dp)
                ) {
                    currentWindow.forEach { prayerItem ->
                        PrayerTimeCard(
                            item = prayerItem,
                            isVerticalLayout = false,
                            modifier = Modifier.size(width = 480.dp, height = 240.dp)
                        )
                    }
                }
            }

            CarouselPaginationIndicator(
                totalCount = totalCount,
                activeIndex = startIndex,
                onSelectIndex = { startIndex = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }
    }
}

@Composable
private fun PrayerTimeCard(
    item: PrayerTimeItem,
    isVerticalLayout: Boolean,
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
            .size(width = 480.dp, height = 240.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBgBrush)
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (isVerticalLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    if (isNext) {
                        NextPrayerPillBadge(bgColor = palette.bgPrimary, textColor = palette.accentSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = item.label.uppercase(),
                        color = titleColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.id.arabicName,
                        color = subColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = item.formattedTime,
                    color = timeColor,
                    fontSize = 52.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
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
                        text = item.label.uppercase(),
                        color = titleColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (isNext) {
                        NextPrayerPillBadge(bgColor = palette.bgPrimary, textColor = palette.accentSecondary)
                    } else {
                        Text(
                            text = item.id.arabicName,
                            color = subColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = item.formattedTime,
                        color = timeColor,
                        fontSize = 56.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (isNext) {
                        Text(
                            text = item.id.arabicName,
                            color = subColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

@Composable
private fun CarouselPaginationIndicator(
    totalCount: Int,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalCount) {
            val isVisibleInWindow = (0 until 4).any { offset -> (activeIndex + offset) % totalCount == i }
            val isPrimaryHead = (i == activeIndex)
            Box(
                modifier = Modifier
                    .width(if (isPrimaryHead) 22.dp else if (isVisibleInWindow) 12.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPrimaryHead -> palette.accent
                            isVisibleInWindow -> palette.accent.copy(alpha = 0.45f)
                            else -> palette.textSecondary.copy(alpha = 0.25f)
                        }
                    )
                    .focusable()
                    .clickable { onSelectIndex(i) }
            )
        }
    }
}
