package id.algaffar.jwstv.ui.screens.fullscreen

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.cctv.OnlineFinanceManager
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.screens.standby.HeaderBar
import id.algaffar.jwstv.ui.screens.standby.HeaderClockCard
import id.algaffar.jwstv.ui.screens.standby.PrayerTimesCarousel
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FMT_HHMMSS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * YouTube Live Streaming Fullscreen (Makkah `Cm1v4bteXbI`, Madinah `9A1S0xAPVIs`, or Custom Video ID).
 * Mirrors `src/components/Screens/MainContent/Slides/LiveSlide.tsx`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLiveFullscreen(
    snapshot: JwsRuntimeSnapshot,
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val liveSettings = settings.live

    val resolvedVideoId = remember(
        liveSettings.source,
        liveSettings.makkahVideoId,
        liveSettings.madinahVideoId,
        liveSettings.customVideoId
    ) {
        when (liveSettings.source.lowercase()) {
            "makkah" -> extractYouTubeVideoId(liveSettings.makkahVideoId).ifBlank { "Cm1v4bteXbI" }
            "madinah" -> extractYouTubeVideoId(liveSettings.madinahVideoId).ifBlank { "9A1S0xAPVIs" }
            else -> extractYouTubeVideoId(liveSettings.customVideoId).ifBlank {
                extractYouTubeVideoId(liveSettings.makkahVideoId).ifBlank { "Cm1v4bteXbI" }
            }
        }
    }

    val resolvedHlsUrl = remember(
        liveSettings.preferNativeHls,
        liveSettings.source,
        liveSettings.makkahHlsUrl,
        liveSettings.madinahHlsUrl,
        liveSettings.customStreamUrl
    ) {
        if (liveSettings.preferNativeHls) liveSettings.activeHlsUrl else ""
    }

    val sourceTitle = when (liveSettings.source.lowercase()) {
        "makkah" -> "SIARAN LANGSUNG MASJIDIL HARAM — MAKKAH AL-MUKARRAMAH"
        "madinah" -> "SIARAN LANGSUNG MASJID NABAWI — MADINAH AL-MUNAWWARAH"
        else -> "SIARAN LANGSUNG — ${settings.brand.name}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Fullscreen Live Video Layer (1920 x 1080): Native Media3 HLS when available, WebView fallback for YouTube IDs
        if (resolvedHlsUrl.isNotBlank()) {
            NativeHlsLivePlayer(
                hlsUrl = resolvedHlsUrl,
                muted = liveSettings.muted,
                modifier = Modifier.fillMaxSize()
            )
        } else {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.settings.javaScriptEnabled = true
                    this.settings.domStorageEnabled = true
                    this.settings.mediaPlaybackRequiresUserGesture = false
                    this.settings.cacheMode = WebSettings.LOAD_DEFAULT
                    this.settings.loadWithOverviewMode = true
                    this.settings.useWideViewPort = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                val muteParam = if (liveSettings.muted) 1 else 0
                val desiredKey = "$resolvedVideoId|$muteParam"
                if (webView.tag != desiredKey) {
                    webView.tag = desiredKey
                    val embedSrc = "https://www.youtube-nocookie.com/embed/$resolvedVideoId" +
                        "?autoplay=1&mute=$muteParam&controls=0&rel=0&modestbranding=1&playsinline=1" +
                        "&enablejsapi=1&origin=https://www.youtube-nocookie.com"
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                                iframe { width: 100%; height: 100%; border: 0; display: block; }
                            </style>
                        </head>
                        <body>
                            <iframe
                                src="$embedSrc"
                                allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                                referrerpolicy="strict-origin-when-cross-origin"
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            onRelease = { webView ->
                runCatching {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.onPause()
                    webView.destroy()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        }

        // 2. Top Overlay: Logo + Nama Masjid (Tanpa Alamat & Tanpa Tanggal) di kiri, Card Jam di kanan
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    id.algaffar.jwstv.ui.screens.standby.MosqueLogoBadge(
                        logoPath = settings.brand.logo,
                        assetsRootDir = assetsRootDir,
                        isMenuOpen = isLogoMenuOpen,
                        onClick = onLogoClick
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = settings.brand.name.ifBlank { "MASJID ANDA" },
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = sourceTitle,
                                color = palette.accentSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                HeaderClockCard(
                    now = snapshot.now,
                    clockStyle = settings.brand.clockStyle,
                    modifier = Modifier.size(width = 440.dp, height = 160.dp)
                )
            }
        }

        // 3. Bottom Overlay: Horizontal Prayer Times Carousel (full 1920 x 225 dp so all 4 cards render unclipped)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        ) {
            PrayerTimesCarousel(
                schedule = snapshot.schedule,
                scrollIntervalSeconds = settings.prayerSettings.prayerTimeScrollInterval,
                isVertical = false,
                modifier = Modifier.size(width = 1920.dp, height = 225.dp)
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
    assetsRootDir: File? = null,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val finCfg = settings.onlineFinance
    val qrCfg = settings.masjidQr
    val bankName = finCfg.bankName.ifBlank { qrCfg.bankName }
    val nomorRekening = finCfg.nomorRekening.ifBlank { qrCfg.nomorRekening }
    val atasNama = finCfg.atasNama.ifBlank { qrCfg.atasNama }
    val hasBankInfo = bankName.isNotBlank() || nomorRekening.isNotBlank() || atasNama.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.bgPrimary, palette.bgSecondary)
                )
            )
            .padding(horizontal = 36.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Standby HeaderBar + Standby-sized HeaderClockCard (480x180dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderBar(
                now = snapshot.now,
                brand = settings.brand,
                hijriDate = snapshot.hijriDate,
                assetsRootDir = assetsRootDir,
                isReversed = false,
                isLogoMenuOpen = isLogoMenuOpen,
                onLogoClick = onLogoClick,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            )

            HeaderClockCard(
                now = snapshot.now,
                clockStyle = settings.brand.clockStyle
            )
        }

        // 2. Main Finance Content Card (Shared with `online_finance` System Slide)
        OnlineFinanceCardContent(
            snapshot = snapshot,
            isExpanded = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgCard)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp))
        )

        // 3. Bottom Row: Left = Rekening / Transparansi Badge, Right = Quoted Guidance Text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgCard)
                    .border(1.5.dp, palette.accentSecondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = if (hasBankInfo) "REKENING INFAQ • ${bankName.ifBlank { "MASJID" }.uppercase()}" else "TRANSPARANSI KAS ONLINE",
                        color = palette.accentSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasBankInfo) {
                            listOf(nomorRekening, atasNama.takeIf { it.isNotBlank() }?.let { "a.n. $it" })
                                .filterNotNull()
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                                .ifBlank { settings.brand.name }
                        } else {
                            "Laporan Keuangan ${settings.brand.name}"
                        },
                        color = palette.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgCard)
                    .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val footerQuote = settings.screenTexts.onlineFinanceFooterQuote.trim()
                    .replace("{{Nama Masjid}}", settings.brand.name, ignoreCase = true)
                if (footerQuote.isNotEmpty()) {
                    Text(
                        text = "“$footerQuote”",
                        color = palette.textSecondary,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Reusable Online Finance Card Content shared between [OnlineFinanceFullscreen]
 * and the `online_finance` System Slide inside Main Standby (`1440 x 675 dp`).
 */
@Composable
fun OnlineFinanceCardContent(
    snapshot: JwsRuntimeSnapshot,
    isExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings
    val financeManager = remember { OnlineFinanceManager() }
    var financialData by remember {
        mutableStateOf(
            OnlineFinanceManager.buildLocalFallback(
                keuangan = settings.jumat.keuangan,
                fallbackDateStr = snapshot.schedule.date.toString()
            )
        )
    }

    LaunchedEffect(
        settings.onlineFinance.enabled,
        settings.onlineFinance.sheetUrl,
        settings.jumat.keuangan
    ) {
        financialData = financeManager.fetchFinancialData(
            onlineFinance = settings.onlineFinance,
            localJumatKeuangan = settings.jumat.keuangan
        )
    }

    val idrFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val maxTxRows = if (isExpanded) 5 else 3

    Column(
        modifier = modifier.padding(
            horizontal = if (isExpanded) 32.dp else 28.dp,
            vertical = if (isExpanded) 24.dp else 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isExpanded) 18.dp else 14.dp)
    ) {
        // Title & Sync Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(if (isExpanded) 40.dp else 34.dp)
                )
                Column {
                    Text(
                        text = "LAPORAN KEUANGAN KAS MASJID",
                        color = palette.textPrimary,
                        fontSize = if (isExpanded) 30.sp else 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Pembaruan: ${financialData.lastUpdated} • ${settings.brand.name}",
                        color = palette.textSecondary,
                        fontSize = if (isExpanded) 18.sp else 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (financialData.isLocalFallback) Color(0xFFF59E0B).copy(alpha = 0.16f)
                        else Color(0xFF10B981).copy(alpha = 0.18f)
                    )
                    .border(
                        1.dp,
                        if (financialData.isLocalFallback) Color(0xFFF59E0B).copy(alpha = 0.5f)
                        else Color(0xFF10B981).copy(alpha = 0.55f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (financialData.isLocalFallback) "DATA BUKU KAS LOKAL" else "SINKRONISASI SPREADSHEET ONLINE",
                    color = if (financialData.isLocalFallback) Color(0xFFFBBF24) else Color(0xFF34D399),
                    fontSize = if (isExpanded) 14.sp else 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 3 Summary Cards (Pemasukan, Pengeluaran, Saldo Akhir)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 20.dp else 14.dp)
        ) {
            FinanceSummaryStatCard(
                label = "TOTAL PEMASUKAN (+)",
                amountFormatted = idrFormatter.format(financialData.income),
                accentColor = Color(0xFF10B981),
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryStatCard(
                label = "TOTAL PENGELUARAN (-)",
                amountFormatted = idrFormatter.format(financialData.expense),
                accentColor = Color(0xFFF43F5E),
                isExpanded = isExpanded,
                modifier = Modifier.weight(1f)
            )
            FinanceSummaryStatCard(
                label = "SALDO AKHIR KAS MASJID",
                amountFormatted = idrFormatter.format(financialData.balance),
                accentColor = palette.accentSecondary,
                isExpanded = isExpanded,
                modifier = Modifier.weight(1.2f)
            )
        }

        // Recent Transactions Table
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.bgPrimary.copy(alpha = 0.45f))
                .border(1.dp, palette.border.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = if (isExpanded) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 10.dp else 8.dp)
        ) {
            Text(
                text = "RINCIAN TRANSAKSI TERAKHIR",
                color = palette.accentSecondary,
                fontSize = if (isExpanded) 17.sp else 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            if (financialData.recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada rincian jurnal transaksi yang tercatat pada periode ini.",
                        color = palette.textSecondary,
                        fontSize = if (isExpanded) 18.sp else 16.sp
                    )
                }
            } else {
                financialData.recentTransactions.take(maxTxRows).forEach { tx ->
                    val isIncome = tx.type.equals("in", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 16.dp, vertical = if (isExpanded) 10.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                tint = if (isIncome) Color(0xFF10B981) else Color(0xFFF43F5E),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = tx.description.ifBlank { "Transaksi Kas Masjid" },
                                    color = palette.textPrimary,
                                    fontSize = if (isExpanded) 17.sp else 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tx.date.isNotBlank()) {
                                    Text(
                                        text = tx.date,
                                        color = palette.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = (if (isIncome) "+ " else "- ") + idrFormatter.format(tx.amount),
                            color = if (isIncome) Color(0xFF34D399) else Color(0xFFFB7185),
                            fontSize = if (isExpanded) 19.sp else 17.sp,
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
    isExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(if (isExpanded) 22.dp else 18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(if (isExpanded) 22.dp else 18.dp))
            .padding(
                horizontal = if (isExpanded) 24.dp else 18.dp,
                vertical = if (isExpanded) 20.dp else 14.dp
            )
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = if (isExpanded) 14.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(if (isExpanded) 8.dp else 6.dp))
        Text(
            text = amountFormatted,
            color = accentColor,
            fontSize = if (isExpanded) 32.sp else 28.sp,
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

@Composable
private fun NativeHlsLivePlayer(
    hlsUrl: String,
    muted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    LaunchedEffect(hlsUrl, muted) {
        player.volume = if (muted) 0f else 1f
        player.setMediaItem(MediaItem.fromUri(hlsUrl))
        player.prepare()
        player.play()
    }

    DisposableEffect(player) {
        onDispose {
            runCatching {
                player.stop()
                player.release()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
        },
        modifier = modifier
    )
}
