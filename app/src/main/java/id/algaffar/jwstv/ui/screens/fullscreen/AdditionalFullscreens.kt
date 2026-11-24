package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.screens.standby.HeaderClockCard
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.settings.ResolvedMediaPreview
import id.algaffar.jwstv.ui.settings.resolveMediaPreview
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import java.io.File
import java.util.Locale

/**
 * Fullscreen Do'a Setelah Adzan (`doa_adzan`).
 * Mirrors `src/components/Screens/FullScreen/DoaAdzanContent.tsx`.
 */
@Composable
fun DoaAdzanFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File?,
    modifier: Modifier = Modifier
) {
    val remSec = snapshot.tickResult.countdownSeconds.coerceAtLeast(0L)
    val formattedRem = String.format(Locale.US, "%02d:%02d", remSec / 60L, remSec % 60L)
    val customImgPath = snapshot.settings.media.doaAdzanImage.trim()
    val resolvedImg: Any? = remember(customImgPath, assetsRootDir) {
        if (customImgPath.isBlank()) null
        else if (customImgPath.startsWith("http") || customImgPath.startsWith("file://") || customImgPath.startsWith("content://")) customImgPath
        else assetsRootDir?.let { File(it, customImgPath.removePrefix("jws-assets/").removePrefix("/")).takeIf { f -> f.exists() } }
            ?: File(customImgPath).takeIf { it.exists() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF020617), Color(0xFF000000))
                )
            )
    ) {
        if (resolvedImg != null) {
            AsyncImage(
                model = resolvedImg,
                contentDescription = "Do'a Setelah Adzan",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                        .border(2.25.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 42.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(39.dp)
                    )
                    Text(
                        text = "DO'A SETELAH ADZAN",
                        color = Color(0xFF6EE7B7),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
                    color = Color(0xFFFDE68A),
                    fontSize = 84.24.sp,
                    lineHeight = 130.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 2.dp)
                        .background(Color(0xFF34D399).copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "\"Ya Allah, Tuhan Pemilik seruan yang sempurna ini dan sholat yang akan didirikan, berikanlah kepada Nabi Muhammad wasilah dan keutamaan, serta tempatkanlah beliau pada kedudukan terpuji yang telah Engkau janjikan.\"",
                    color = Color.White,
                    fontSize = 41.6.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 58.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "— HR. BUKHARI —",
                    color = Color(0xFF34D399),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Bottom Countdown Pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 44.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "SISA WAKTU",
                color = Color(0xFF6EE7B7),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = formattedRem,
                color = Color.White,
                fontSize = 30.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Shared split-screen QR template mirroring Pelaksanaan Sholat / Khutbah (`SimpleStateOverlayFullscreen`):
 * - Diagonal layered geometric split dividing left & right halves
 * - Top-left: Logo + Nama & Alamat (`FullscreenBrandHeader`)
 * - Left side: Badge, Judul, Himbauan, optional extra summary, & Rekening Card (Nama Bank, No. Rekening, Nama di Rekening)
 * - Top-right: `HeaderClockCard` (480x180dp)
 * - Right side: Large QR Code filling ~80% of the right-side area
 */
@Composable
fun QrSplitOverlayFullscreen(
    snapshot: JwsRuntimeSnapshot,
    badgeLabel: String,
    badgeIcon: ImageVector,
    title: String,
    guidance: String,
    qrPayload: String,
    customQrImage: String = "",
    bankName: String = "",
    nomorRekening: String = "",
    atasNama: String = "",
    accentColor: Color,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    isCompactSlide: Boolean = false,
    extraLeftContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val effectiveAccent = if (palette.isLight) accentColor else palette.accent
    val secondaryRibbonColor = palette.accentSecondary

    val resolvedCustomQr = remember(customQrImage, assetsRootDir) {
        if (customQrImage.isBlank()) null else resolveMediaPreview(customQrImage, assetsRootDir)
    }
    val qrBitmap = remember(qrPayload, resolvedCustomQr) {
        if (resolvedCustomQr != null) null
        else generateQrBitmap(qrPayload.ifBlank { "Infaq Digital Masjid" }, 800)
    }

    val pulseAlpha: Float = if (isPerformanceMode) {
        0.10f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "QrSplitPulse")
        val p by infiniteTransition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "QrOrbPulse"
        )
        p
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.bgPrimary, palette.bgSecondary, Color(0xFF020617))
                )
            )
    ) {
        // 1. Right-Side Subtle Ambient Background (Dot matrix + radial glow)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.58f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            effectiveAccent.copy(alpha = pulseAlpha * 1.4f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.56f, size.height * 0.54f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.56f, size.height * 0.54f)
                )
                val step = 52.dp.toPx()
                var x = step * 0.5f
                while (x < size.width) {
                    var y = step * 0.5f
                    while (y < size.height) {
                        drawCircle(
                            color = palette.textPrimary.copy(alpha = 0.08f),
                            radius = 2f,
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }
        }

        // 2. Diagonal Layered Geometric Split (Left Panel + 2 Folded Theme Ribbons)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val outerRibbon = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.545f, 0f)
                lineTo(w * 0.485f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = outerRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        secondaryRibbonColor.copy(alpha = 0.92f),
                        effectiveAccent.copy(alpha = 0.88f)
                    ),
                    start = Offset(w * 0.40f, 0f),
                    end = Offset(w * 0.52f, h)
                )
            )

            val middleRibbon = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.530f, 0f)
                lineTo(w * 0.455f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = middleRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        effectiveAccent.copy(alpha = 0.94f),
                        palette.bgSecondary.copy(alpha = 0.96f)
                    ),
                    start = Offset(w * 0.36f, 0f),
                    end = Offset(w * 0.48f, h)
                )
            )

            val primaryPanel = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.515f, 0f)
                lineTo(w * 0.425f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = primaryPanel,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.bgPrimary,
                        palette.bgSecondary,
                        palette.bgPrimary.copy(alpha = 0.98f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.48f, h)
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(effectiveAccent.copy(alpha = pulseAlpha), Color.Transparent),
                    center = Offset(w * 0.16f, h * 0.25f),
                    radius = w * 0.34f
                ),
                radius = w * 0.34f,
                center = Offset(w * 0.16f, h * 0.25f)
            )
        }

        // 3. Left Panel Content (Top-Left Brand Header, Badge, Judul, Himbauan, Rekening Card)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(if (isCompactSlide) 0.50f else 0.49f)
                .padding(
                    start = if (isCompactSlide) 32.dp else 52.dp,
                    end = if (isCompactSlide) 20.dp else 32.dp,
                    top = if (isCompactSlide) 22.dp else 28.dp,
                    bottom = if (isCompactSlide) 24.dp else 36.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top: Mosque Brand (Logo + Nama + Alamat, fullscreen only) & State Context Badge
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isCompactSlide) {
                    FullscreenBrandHeader(
                        brand = snapshot.settings.brand,
                        assetsRootDir = assetsRootDir,
                        isLogoMenuOpen = isLogoMenuOpen,
                        onLogoClick = onLogoClick
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(effectiveAccent.copy(alpha = 0.18f))
                        .border(1.dp, effectiveAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(
                            horizontal = if (isCompactSlide) 14.dp else 18.dp,
                            vertical = if (isCompactSlide) 6.dp else 7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(if (isCompactSlide) 16.dp else 18.dp)
                    )
                    Text(
                        text = badgeLabel.uppercase(),
                        color = effectiveAccent,
                        fontSize = if (isCompactSlide) 13.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle: Judul + Himbauan (+ Optional Extra Summary)
            Column(
                modifier = Modifier.fillMaxWidth(0.95f),
                verticalArrangement = Arrangement.spacedBy(if (isCompactSlide) 10.dp else 14.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    color = secondaryRibbonColor,
                    fontSize = if (isCompactSlide) 40.sp else 60.sp,
                    lineHeight = if (isCompactSlide) 46.sp else 68.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                Text(
                    text = "“$guidance”",
                    color = palette.textSecondary,
                    fontSize = if (isCompactSlide) 20.sp else 27.sp,
                    lineHeight = if (isCompactSlide) 28.sp else 36.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (isCompactSlide) 3 else 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (extraLeftContent != null) {
                    extraLeftContent()
                }
            }

            // Bottom-Left: Bank & Rekening Card (Nama Bank, No. Rekening, Nama di Rekening)
            val hasBankInfo = bankName.isNotBlank() || nomorRekening.isNotBlank() || atasNama.isNotBlank()
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (isCompactSlide) 0.90f else 0.88f)
                    .clip(RoundedCornerShape(if (isCompactSlide) 18.dp else 24.dp))
                    .background(palette.bgCard.copy(alpha = 0.92f))
                    .border(2.dp, effectiveAccent.copy(alpha = 0.55f), RoundedCornerShape(if (isCompactSlide) 18.dp else 24.dp))
                    .padding(
                        horizontal = if (isCompactSlide) 18.dp else 24.dp,
                        vertical = if (isCompactSlide) 12.dp else 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isCompactSlide) 5.dp else 8.dp)
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
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = effectiveAccent,
                            modifier = Modifier.size(if (isCompactSlide) 18.dp else 22.dp)
                        )
                        Text(
                            text = "REKENING INFAQ & TRANSFER",
                            color = effectiveAccent,
                            fontSize = if (isCompactSlide) 13.sp else 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    if (bankName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(effectiveAccent.copy(alpha = 0.20f))
                                .border(1.dp, effectiveAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = if (isCompactSlide) 10.dp else 14.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = bankName.uppercase(),
                                color = palette.textPrimary,
                                fontSize = if (isCompactSlide) 13.sp else 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (hasBankInfo) {
                    Text(
                        text = nomorRekening.ifBlank { "-" },
                        color = secondaryRibbonColor,
                        fontSize = if (isCompactSlide) 28.sp else 38.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        text = "a.n. ${atasNama.ifBlank { snapshot.settings.brand.name }}",
                        color = palette.textPrimary,
                        fontSize = if (isCompactSlide) 17.sp else 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Pembayaran Non-Tunai via QRIS",
                        color = palette.textPrimary,
                        fontSize = if (isCompactSlide) 19.sp else 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan QR Code di samping dengan aplikasi Mobile Banking / E-Wallet",
                        color = palette.textSecondary,
                        fontSize = if (isCompactSlide) 14.sp else 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Top-Right Clock Card matching Standby & Pelaksanaan Sholat (480x180dp, fullscreen only)
        if (!isCompactSlide) {
            HeaderClockCard(
                now = snapshot.now,
                clockStyle = snapshot.settings.brand.clockStyle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 32.dp)
            )
        }

        // 5. Right-Side Full QR Card (~80% of available right-side area below clock)
        Box(
            modifier = Modifier
                .align(if (isCompactSlide) Alignment.CenterEnd else Alignment.BottomEnd)
                .fillMaxWidth(0.51f)
                .fillMaxHeight()
                .padding(
                    top = if (isCompactSlide) 24.dp else 216.dp,
                    bottom = if (isCompactSlide) 24.dp else 36.dp,
                    start = if (isCompactSlide) 20.dp else 24.dp,
                    end = if (isCompactSlide) 36.dp else 48.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(if (isCompactSlide) 0.86f else 0.88f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(if (isCompactSlide) 26.dp else 36.dp))
                    .background(Color.White)
                    .border(
                        width = if (isCompactSlide) 3.dp else 4.dp,
                        color = secondaryRibbonColor.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(if (isCompactSlide) 26.dp else 36.dp)
                    )
                    .padding(if (isCompactSlide) 18.dp else 28.dp),
                contentAlignment = Alignment.Center
            ) {
                when (resolvedCustomQr) {
                    is ResolvedMediaPreview.CoilModel -> AsyncImage(
                        model = resolvedCustomQr.model,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    is ResolvedMediaPreview.DecodedBitmap -> Image(
                        bitmap = resolvedCustomQr.bitmap,
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    null -> {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(360.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen QR Code Laporan Keuangan Online (`finance_qr`).
 * Mirrors `src/components/Screens/FullScreen/shared/FinanceQrSlide.tsx`.
 */
@Composable
fun FinanceQrFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    isCompactSlide: Boolean = false,
    modifier: Modifier = Modifier
) {
    val finance = snapshot.settings.onlineFinance
    val qrCfg = snapshot.settings.masjidQr
    val qrPayload = finance.reportUrl.ifBlank { finance.sheetUrl }.ifBlank { qrCfg.link }

    QrSplitOverlayFullscreen(
        snapshot = snapshot,
        badgeLabel = "TRANSPARANSI KAS ONLINE",
        badgeIcon = Icons.Default.AccountBalanceWallet,
        title = "Laporan Keuangan Detail\n${snapshot.settings.brand.name}",
        guidance = "Pindai QR Code di samping menggunakan kamera smartphone untuk melihat rincian mutasi pemasukan, pengeluaran, dan saldo kas masjid secara real-time.",
        qrPayload = qrPayload,
        bankName = finance.bankName.ifBlank { qrCfg.bankName },
        nomorRekening = finance.nomorRekening.ifBlank { qrCfg.nomorRekening },
        atasNama = finance.atasNama.ifBlank { qrCfg.atasNama },
        accentColor = Color(0xFF10B981),
        assetsRootDir = assetsRootDir,
        isLogoMenuOpen = isLogoMenuOpen,
        onLogoClick = onLogoClick,
        isCompactSlide = isCompactSlide,
        modifier = modifier
    )
}
