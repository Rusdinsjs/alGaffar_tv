package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay

/**
 * Floating Overlay Running Text positioned at the bottom-left corner (`Alignment.BottomStart`)
 * of `MainSlideContainer`. Width and height dynamically wrap/adapt to the active text content.
 * Mirrors `src/components/Screens/OverlayText/OverlayText.tsx` (`variant = "overlay"`).
 */
@Composable
fun OverlayRunningText(
    messages: List<String>,
    visibleDurationSeconds: Int,
    hiddenDurationSeconds: Int,
    speed: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val activeMessages = remember(messages) {
        messages.map { it.trim() }.filter { it.isNotBlank() }
    }

    if (activeMessages.isEmpty()) return

    var isVisible by remember { mutableStateOf(true) }
    val visMs = visibleDurationSeconds.coerceIn(5, 600) * 1000L
    val hidMs = hiddenDurationSeconds.coerceIn(0, 300) * 1000L

    LaunchedEffect(visMs, hidMs) {
        if (hidMs <= 0L) {
            isVisible = true
            return@LaunchedEffect
        }
        while (true) {
            isVisible = true
            delay(visMs)
            isVisible = false
            delay(hidMs)
        }
    }

    // Cycle through messages; display duration scales with speed setting (higher speed = faster rotation)
    var currentIndex by remember { mutableIntStateOf(0) }
    val slideIntervalMs = remember(speed) {
        val normalizedSpeed = speed.coerceIn(20, 150)
        // Map speed 20..150 -> 9000ms..3500ms per message
        (9500L - (normalizedSpeed * 40L)).coerceIn(3500L, 10000L)
    }

    LaunchedEffect(activeMessages.size, slideIntervalMs) {
        if (activeMessages.size <= 1) {
            currentIndex = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(slideIntervalMs)
            currentIndex = (currentIndex + 1) % activeMessages.size
        }
    }

    val currentMessage = activeMessages[currentIndex % activeMessages.size]

    val infiniteTransition = rememberInfiniteTransition(label = "OverlayPulseTransition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OverlayPulseDotAlpha"
    )

    // Quarter-circle top-right curve, flush flat on left and bottom edges (matches diagram)
    val overlayShape = RoundedCornerShape(
        topStartPercent = 0,
        bottomStartPercent = 0,
        bottomEndPercent = 0,
        topEndPercent = 100
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(tween(450)) { -it } + fadeIn(tween(400)),
        exit = slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(300)),
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        AnimatedContent(
            targetState = currentMessage,
            transitionSpec = {
                (slideInHorizontally(tween(450)) { -it / 2 } + fadeIn(tween(400)))
                    .togetherWith(slideOutHorizontally(tween(350)) { -it / 2 } + fadeOut(tween(250)))
            },
            label = "FloatingOverlayMessageTransition"
        ) { messageText ->
            Row(
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 1120.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .shadow(elevation = 12.dp, shape = overlayShape)
                    .clip(overlayShape)
                    .background(palette.accent)
                    .border(
                        width = 1.5.dp,
                        color = palette.accentSecondary.copy(alpha = 0.65f),
                        shape = overlayShape
                    )
                    .padding(start = 28.dp, end = 64.dp, top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = messageText.uppercase(),
                    color = palette.bgPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .wrapContentWidth()
                        .wrapContentHeight()
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(palette.bgPrimary)
                )
            }
        }
    }
}

