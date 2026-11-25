package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.model.FloatingOverlayItem
import id.algaffar.jwstv.domain.model.MasjidQrSettings
import id.algaffar.jwstv.domain.model.OnlineFinanceSettings
import id.algaffar.jwstv.domain.model.SlideshowGroup
import id.algaffar.jwstv.ui.settings.ResolvedMediaPreview
import id.algaffar.jwstv.ui.settings.resolveMediaPreview
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import kotlinx.coroutines.delay
import java.io.File

/**
 * Maps corner position key (`top-left`, `bottom-left`, `top-right`, `bottom-right`)
 * to Compose [Alignment].
 */
fun cornerAlignmentOf(position: String): Alignment = when (SlideshowGroup.normalizeCornerPosition(position)) {
    SlideshowGroup.POS_TOP_LEFT -> Alignment.TopStart
    SlideshowGroup.POS_TOP_RIGHT -> Alignment.TopEnd
    SlideshowGroup.POS_BOTTOM_RIGHT -> Alignment.BottomEnd
    else -> Alignment.BottomStart
}

/**
 * Static (non-running-text) Floating Overlay Banner controlled by the active [SlideshowGroup].
 * Uses the original sleek accent pill design (flush on the screen edge, full rounded inner end,
 * pulsing indicator dot, uppercase bold message) without rendering title/index badges on TV.
 */
@Composable
fun StaticFloatingOverlayBanner(
    overlay: FloatingOverlayItem?,
    position: String,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val normalizedPos = SlideshowGroup.normalizeCornerPosition(position)
    val isRightSide = normalizedPos == SlideshowGroup.POS_TOP_RIGHT || normalizedPos == SlideshowGroup.POS_BOTTOM_RIGHT
    val messageText = overlay?.message?.trim().orEmpty()
    val isVisible = messageText.isNotBlank()

    val dotAlpha: Float = if (isPerformanceMode) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "OverlayPulseTransition")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OverlayPulseDotAlpha"
        )
        animated
    }

    // Flush flat on the outer edge (left or right), full rounded on the inner end — matching the original Overlay UI
    val overlayShape = if (isRightSide) {
        RoundedCornerShape(
            topStartPercent = 100,
            bottomStartPercent = 100,
            topEndPercent = 0,
            bottomEndPercent = 0
        )
    } else {
        RoundedCornerShape(
            topStartPercent = 0,
            bottomStartPercent = 0,
            topEndPercent = 100,
            bottomEndPercent = 100
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (isPerformanceMode) {
            EnterTransition.None
        } else {
            slideInHorizontally(tween(450)) { if (isRightSide) it else -it } + fadeIn(tween(400))
        },
        exit = if (isPerformanceMode) {
            ExitTransition.None
        } else {
            slideOutHorizontally(tween(350)) { if (isRightSide) it else -it } + fadeOut(tween(300))
        },
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        AnimatedContent(
            targetState = messageText,
            transitionSpec = {
                if (isPerformanceMode) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (slideInHorizontally(tween(450)) { if (isRightSide) it / 2 else -it / 2 } + fadeIn(tween(400)))
                        .togetherWith(slideOutHorizontally(tween(350)) { if (isRightSide) it / 2 else -it / 2 } + fadeOut(tween(250)))
                }
            },
            label = "StaticFloatingOverlayTransition"
        ) { textToDisplay ->
            Row(
                modifier = Modifier
                    .widthIn(
                        min = 360.dp,
                        max = if (isFullscreen) 1200.dp else 940.dp
                    )
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
                    .padding(
                        start = if (isRightSide) 48.dp else 28.dp,
                        end = if (isRightSide) 28.dp else 48.dp,
                        top = 14.dp,
                        bottom = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRightSide) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(palette.bgPrimary)
                    )
                }

                Text(
                    text = textToDisplay.uppercase(),
                    color = palette.bgPrimary,
                    fontSize = if (isFullscreen) 28.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = if (isFullscreen) 34.sp else 30.sp,
                    textAlign = if (isRightSide) TextAlign.End else TextAlign.Start,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (!isRightSide) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(palette.bgPrimary)
                    )
                }
            }
        }
    }
}

private data class RotatingQrCardModel(
    val id: String,
    val badgeTitle: String,
    val label: String,
    val subtitle: String,
    val qrPayload: String,
    val customQrImage: String,
    val isFinance: Boolean
)

/**
 * Floating Card QR Infaq & QR Kas Online that alternates automatically whenever both are enabled.
 * Always positioned horizontally opposite to the active [SlideshowGroup]'s Floating Overlay position.
 */
@Composable
fun FloatingRotatingQrCard(
    masjidQr: MasjidQrSettings,
    onlineFinance: OnlineFinanceSettings,
    brandName: String,
    assetsRootDir: File?,
    groupTotalDurationMs: Long,
    groupTurnKey: Any,
    visible: Boolean,
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current

    val cards = remember(masjidQr, onlineFinance, brandName) {
        buildList {
            if (masjidQr.enabled) {
                val bankSummary = listOf(masjidQr.bankName, masjidQr.nomorRekening)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                add(
                    RotatingQrCardModel(
                        id = "qr_infaq",
                        badgeTitle = "QR INFAQ MASJID",
                        label = masjidQr.label.ifBlank { "Infaq & Sedekah Digital" },
                        subtitle = bankSummary.ifBlank { "Scan QRIS / E-Wallet" },
                        qrPayload = masjidQr.link.ifBlank { "https://masjidalgaffar.id/infaq" },
                        customQrImage = masjidQr.customQrImage,
                        isFinance = false
                    )
                )
            }
            if (onlineFinance.enabled) {
                val financePayload = onlineFinance.reportUrl.ifBlank { onlineFinance.sheetUrl }
                    .ifBlank { masjidQr.link }
                    .ifBlank { "https://masjidalgaffar.id/keuangan" }
                add(
                    RotatingQrCardModel(
                        id = "qr_kas_online",
                        badgeTitle = "QR KAS ONLINE",
                        label = "Laporan Keuangan Online",
                        subtitle = "Scan untuk mutasi kas ${brandName.ifBlank { "Masjid" }}",
                        qrPayload = financePayload,
                        customQrImage = "",
                        isFinance = true
                    )
                )
            }
        }
    }

    var activeCardIdx by remember(groupTurnKey, cards.size) { mutableIntStateOf(0) }

    LaunchedEffect(groupTurnKey, cards.size, groupTotalDurationMs, visible) {
        activeCardIdx = 0
        if (!visible || cards.size <= 1) return@LaunchedEffect
        val perCardMs = (groupTotalDurationMs / cards.size).coerceIn(5_000L, 15_000L)
        while (true) {
            delay(perCardMs)
            activeCardIdx = (activeCardIdx + 1) % cards.size
        }
    }

    val activeCard = cards.getOrNull(activeCardIdx % cards.size.coerceAtLeast(1))

    AnimatedVisibility(
        visible = visible && activeCard != null,
        enter = if (isPerformanceMode) EnterTransition.None else fadeIn(tween(350)),
        exit = if (isPerformanceMode) ExitTransition.None else fadeOut(tween(250)),
        modifier = modifier.padding(if (isFullscreen) 24.dp else 18.dp)
    ) {
        if (activeCard != null) {
            AnimatedContent(
                targetState = activeCard,
                transitionSpec = {
                    if (isPerformanceMode) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(380)) togetherWith fadeOut(tween(280))
                    }
                },
                label = "FloatingRotatingQrTransition"
            ) { card ->
                val resolvedCustomQr = remember(card.customQrImage, assetsRootDir) {
                    if (card.customQrImage.isBlank()) null else resolveMediaPreview(card.customQrImage, assetsRootDir)
                }
                val qrBitmap = remember(card.qrPayload, resolvedCustomQr) {
                    if (resolvedCustomQr != null) null else generateQrBitmap(card.qrPayload, 240)
                }
                val accentColor = if (card.isFinance) palette.accent else palette.accentSecondary
                val qrBoxSize = if (isFullscreen) 92.dp else 78.dp

                Row(
                    modifier = Modifier
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.93f))
                        .border(2.dp, accentColor, RoundedCornerShape(18.dp))
                        .padding(if (isFullscreen) 14.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(qrBoxSize)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (resolvedCustomQr) {
                            is ResolvedMediaPreview.CoilModel -> AsyncImage(
                                model = resolvedCustomQr.model,
                                contentDescription = card.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(qrBoxSize)
                            )
                            is ResolvedMediaPreview.DecodedBitmap -> Image(
                                bitmap = resolvedCustomQr.bitmap,
                                contentDescription = card.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(qrBoxSize)
                            )
                            null -> {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = card.label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(qrBoxSize)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (card.isFinance) Icons.Default.AccountBalanceWallet else Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.width(if (isFullscreen) 205.dp else 178.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = card.badgeTitle,
                                color = accentColor,
                                fontSize = if (isFullscreen) 13.sp else 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (cards.size > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    cards.indices.forEach { dotIdx ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (dotIdx == activeCardIdx % cards.size) 7.dp else 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (dotIdx == activeCardIdx % cards.size) accentColor
                                                    else palette.textSecondary.copy(alpha = 0.35f)
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = card.label,
                            color = palette.textPrimary,
                            fontSize = if (isFullscreen) 15.sp else 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = card.subtitle,
                            color = palette.textSecondary,
                            fontSize = if (isFullscreen) 12.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
