package id.algaffar.jwstv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import id.algaffar.jwstv.domain.model.ScaleMode
import kotlin.math.max
import kotlin.math.min

/**
 * Provides a fixed 1920x1080 dp virtual reference canvas that automatically scales
 * to any physical Android TV (720p, 1080p, 4K) or Tablet screen resolution.
 * Mirrors `src/components/ResponsiveScaler.tsx`.
 */
@Composable
fun Responsive1080pCanvas(
    scaleMode: ScaleMode,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseDensity = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val targetWidthPx = 1920f
        val targetHeightPx = 1080f

        val rawScaleX = containerWidthPx / targetWidthPx
        val rawScaleY = containerHeightPx / targetHeightPx

        val (scaleX, scaleY) = when (scaleMode) {
            ScaleMode.STRETCH -> rawScaleX to rawScaleY
            ScaleMode.CONTAIN -> {
                val uniform = min(rawScaleX, rawScaleY)
                uniform to uniform
            }
            ScaleMode.COVER -> {
                val uniform = max(rawScaleX, rawScaleY)
                uniform to uniform
            }
        }

        // Use Density(1f) inside the 1920x1080 dp box so 1.dp == 1 virtual pixel on the 1080p canvas,
        // then hardware-scale the entire layer to the physical screen dimensions.
        val virtualDensity = Density(
            density = 1f,
            fontScale = baseDensity.fontScale.coerceIn(0.85f, 1.15f)
        )

        CompositionLocalProvider(LocalDensity provides virtualDensity) {
            Box(
                modifier = Modifier
                    .requiredSize(1920.dp, 1080.dp)
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.transformOrigin = TransformOrigin.Center
                    },
                content = content
            )
        }
    }
}
