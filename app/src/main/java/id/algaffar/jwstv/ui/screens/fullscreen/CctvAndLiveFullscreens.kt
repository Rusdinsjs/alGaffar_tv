package id.algaffar.jwstv.ui.screens.fullscreen

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.cctv.OnlineFinanceManager
import id.algaffar.jwstv.domain.model.CctvCameraConfig
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT_HHMMSS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Multi-Camera CCTV Grid Fullscreen (`1`, `2`, `4`, or `8` cells).
 * Supports RTSP/HLS streams via Media3 [ExoPlayer] and HTTP Snapshot/MJPEG URLs via Coil.
 * Mirrors `src/components/Screens/FullScreen/CctvFullScreen.tsx` & `CctvSlide.tsx`.
 */
@Composable
fun CctvGridFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val cctvSettings = snapshot.settings.cctv
    val enabledCameras = remember(cctvSettings.cameras, cctvSettings.gridLayout) {
        val active = cctvSettings.cameras.filter { it.enabled }
        val targetCount = cctvSettings.gridLayout.coerceIn(1, 8)
        if (active.size >= targetCount) {
            active.take(targetCount)
        } else {
            // Pad up to targetCount with placeholder slots so the grid layout stays balanced
            active + List(targetCount - active.size) { idx ->
                CctvCameraConfig(
                    id = "placeholder-$idx",
                    name = "Kamera ${active.size + idx + 1} (Belum Dikonfigurasi)",
                    url = "",
                    enabled = true
                )
            }
        }
    }

    val columns = when {
        enabledCameras.size <= 1 -> 1
        enabledCameras.size <= 4 -> 2
        else -> 4
    }
    val rows = ((enabledCameras.size + columns - 1) / columns).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05080F))
            .padding(20.dp)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.bgCard)
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(30.dp)
                )
                Column {
                    Text(
                        text = "MONITORING CCTV — ${snapshot.settings.brand.name}",
                        color = palette.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Tata Letak Grid ${cctvSettings.gridLayout} Kamera • RTSP / ONVIF / HTTP Snapshot",
                        color = palette.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = snapshot.now.format(TIME_FMT_HHMMSS),
                color = palette.accent,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for (rowIdx in 0 until rows) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (colIdx in 0 until columns) {
                        val camIdx = rowIdx * columns + colIdx
                        val camera = enabledCameras.getOrNull(camIdx)
                        if (camera != null) {
                            CctvCameraCell(
                                camera = camera,
                                index = camIdx + 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun CctvCameraCell(
    camera: CctvCameraConfig,
    index: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val context = LocalContext.current
    val url = camera.url.trim()
    val isRtspOrHls = url.startsWith("rtsp://", ignoreCase = true) ||
        url.endsWith(".m3u8", ignoreCase = true) ||
        url.endsWith(".mp4", ignoreCase = true)

    var snapshotTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(url, isRtspOrHls) {
        if (url.isNotEmpty() && !isRtspOrHls) {
            while (true) {
                delay(1500L)
                snapshotTick++
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0B111E))
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
    ) {
        when {
            url.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = palette.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = camera.name,
                        color = palette.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "URL RTSP / Snapshot belum diisi di Pengaturan",
                        color = palette.textSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            isRtspOrHls -> {
                val exoPlayer = remember(url) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(url))
                        volume = 0f
                        repeatMode = Player.REPEAT_MODE_ALL
                        prepare()
                        playWhenReady = true
                    }
                }
                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                val separator = if (url.contains("?")) "&" else "?"
                val bustedUrl = "$url${separator}_t=$snapshotTick"
                AsyncImage(
                    model = bustedUrl,
                    contentDescription = camera.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Camera Badge Overlay (Top-Left)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (url.isNotEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B))
            )
            Text(
                text = "CAM $index • ${camera.name}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * YouTube Live Streaming Fullscreen (Makkah `Cm1v4bteXbI`, Madinah `9A1S0xAPVIs`, or Custom Video ID).
 * Mirrors `src/components/Screens/MainContent/Slides/LiveSlide.tsx`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLiveFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val liveSettings = snapshot.settings.live

    val resolvedVideoId = remember(liveSettings.source, liveSettings.customVideoId) {
        when (liveSettings.source.lowercase()) {
            "makkah" -> "Cm1v4bteXbI"
            "madinah" -> "9A1S0xAPVIs"
            else -> extractYouTubeVideoId(liveSettings.customVideoId).ifBlank { "Cm1v4bteXbI" }
        }
    }

    val sourceTitle = when (liveSettings.source.lowercase()) {
        "makkah" -> "SIARAN LANGSUNG MASJIDIL HARAM — MAKKAH AL-MUKARRAMAH"
        "madinah" -> "SIARAN LANGSUNG MASJID NABAWI — MADINAH AL-MUNAWWARAH"
        else -> "SIARAN LANGSUNG — ${snapshot.settings.brand.name}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                val targetUrl = "https://www.youtube-nocookie.com/embed/$resolvedVideoId?autoplay=1&mute=0&controls=0&rel=0&modestbranding=1&playsinline=1"
                if (webView.url != targetUrl) {
                    webView.loadUrl(targetUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Glassmorphic Info Overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
                .padding(horizontal = 36.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Column {
                    Text(
                        text = sourceTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${snapshot.settings.brand.name} • ${snapshot.hijriDate.formatted}",
                        color = palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = snapshot.now.format(TIME_FMT_HHMMSS),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Online Mosque Finance Fullscreen (Google Sheets CSV/JSON sync with local fallback).
 * Mirrors `src/components/Screens/MainContent/Slides/OnlineFinanceSlide.tsx`.
 */
@Composable
fun OnlineFinanceFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val financeManager = remember { OnlineFinanceManager() }
    var financialData by remember {
        mutableStateOf(
            OnlineFinanceManager.buildLocalFallback(
                keuangan = snapshot.settings.jumat.keuangan,
                fallbackDateStr = snapshot.schedule.date.toString()
            )
        )
    }

    LaunchedEffect(
        snapshot.settings.onlineFinance.enabled,
        snapshot.settings.onlineFinance.sheetUrl,
        snapshot.settings.jumat.keuangan
    ) {
        financialData = financeManager.fetchFinancialData(
            onlineFinance = snapshot.settings.onlineFinance,
            localJumatKeuangan = snapshot.settings.jumat.keuangan
        )
    }

    val idrFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.bgPrimary, palette.bgSecondary)
                )
            )
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(22.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        text = "LAPORAN KEUANGAN MASJID",
                        color = palette.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = buildString {
                            append(snapshot.settings.brand.name)
                            append(" • Pembaruan: ${financialData.lastUpdated}")
                            append(if (financialData.isLocalFallback) " (Data Buku Kas Lokal)" else " (Sinkronisasi Spreadsheet Online)")
                        },
                        color = palette.textSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            Text(
                text = snapshot.now.format(TIME_FMT_HHMMSS),
                color = palette.accent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // 3 Summary Cards (Pemasukan, Pengeluaran, Saldo Akhir)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FinanceSummaryStatCard(
                label = "TOTAL PEMASUKAN",
                amountFormatted = idrFormatter.format(financialData.income),
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryStatCard(
                label = "TOTAL PENGELUARAN",
                amountFormatted = idrFormatter.format(financialData.expense),
                accentColor = Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryStatCard(
                label = "SALDO KAS MASJID",
                amountFormatted = idrFormatter.format(financialData.balance),
                accentColor = palette.accent,
                modifier = Modifier.weight(1.2f)
            )
        }

        // Recent Transactions Table
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(palette.bgCard)
                .border(1.dp, palette.border, RoundedCornerShape(22.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "RINCIAN TRANSAKSI TERAKHIR",
                color = palette.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (financialData.recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada rincian jurnal transaksi yang tercatat pada periode ini.",
                        color = palette.textSecondary,
                        fontSize = 18.sp
                    )
                }
            } else {
                financialData.recentTransactions.forEach { tx ->
                    val isIncome = tx.type.equals("in", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint = if (isIncome) Color(0xFF10B981) else Color(0xFFF43F5E),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = tx.description.ifBlank { "Transaksi Kas Masjid" },
                                    color = palette.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (tx.date.isNotBlank()) {
                                    Text(
                                        text = tx.date,
                                        color = palette.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = (if (isIncome) "+ " else "- ") + idrFormatter.format(tx.amount),
                            color = if (isIncome) Color(0xFF34D399) else Color(0xFFFB7185),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceSummaryStatCard(
    label: String,
    amountFormatted: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(palette.bgCard)
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = amountFormatted,
            color = accentColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun extractYouTubeVideoId(rawInput: String): String {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return ""
    val watchRegex = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
    watchRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }

    val shortRegex = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""")
    shortRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }

    val embedRegex = Regex("""embed/([a-zA-Z0-9_-]{11})""")
    embedRegex.find(trimmed)?.groupValues?.get(1)?.let { return it }

    return trimmed
}
