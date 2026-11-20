package id.algaffar.jwstv.ui.screens.fullscreen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.text.NumberFormat
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
                    .padding(horizontal = 96.dp, vertical = 64.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                        .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "DO'A SETELAH ADZAN",
                        color = Color(0xFF6EE7B7),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
                    color = Color(0xFFFDE68A),
                    fontSize = 52.sp,
                    lineHeight = 84.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 2.dp)
                        .background(Color(0xFF34D399).copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "\"Allāhumma rabba hāżihid-da'watit-tāmmati, waṣ-ṣalātil-qā'imah, āti muḥammadanil-wasīlata wal-faḍīlah, wab'aṡhu maqāmam maḥmūdanil-lażī wa'adtah.\"",
                    color = Color(0xFFD1FAE5).copy(alpha = 0.85f),
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "\"Ya Allah, Tuhan Pemilik seruan yang sempurna ini dan sholat yang akan didirikan, berikanlah kepada Nabi Muhammad wasilah dan keutamaan, serta tempatkanlah beliau pada kedudukan terpuji yang telah Engkau janjikan.\"",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 44.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MENUJU WAKTU MENUNGGU IQOMAH • ${snapshot.settings.brand.name}",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
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
}

/**
 * Fullscreen QR Code Laporan Keuangan Online (`finance_qr`).
 * Mirrors `src/components/Screens/FullScreen/shared/FinanceQrSlide.tsx`.
 */
@Composable
fun FinanceQrFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val finance = snapshot.settings.onlineFinance
    val qrPayload = finance.reportUrl.ifBlank { finance.sheetUrl }.ifBlank { snapshot.settings.masjidQr.link }
    val qrBmp = remember(qrPayload) { generateQrBitmap(qrPayload, 460) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp)
    ) {
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "TRANSPARANSI KAS ONLINE",
                    color = palette.accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = "LAPORAN KEUANGAN DETAIL\n${snapshot.settings.brand.name.uppercase()}",
                color = palette.textPrimary,
                fontSize = 44.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Pindai QR Code di samping menggunakan kamera smartphone untuk melihat rincian mutasi pemasukan, pengeluaran, dan saldo kas masjid secara real-time.",
                color = palette.textSecondary,
                fontSize = 22.sp,
                lineHeight = 32.sp
            )

            if (finance.bankName.isNotBlank() || finance.nomorRekening.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.bgCard)
                        .border(1.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "REKENING INFAQ & KAS MASJID",
                        color = palette.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${finance.bankName.uppercase()} • ${finance.nomorRekening}",
                        color = palette.accentSecondary,
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                    if (finance.atasNama.isNotBlank()) {
                        Text(
                            text = "a.n. ${finance.atasNama}",
                            color = palette.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(4.dp, palette.accent, RoundedCornerShape(32.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBmp != null) {
                Image(
                    bitmap = qrBmp.asImageBitmap(),
                    contentDescription = "QR Laporan Keuangan",
                    modifier = Modifier.size(420.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(320.dp)
                )
            }
        }
    }
}

/**
 * Fullscreen QR Code Ringkasan Kas Jum'at (`jumat_qr`).
 * Mirrors `src/components/Screens/FullScreen/shared/JumatQrSlide.tsx`.
 */
@Composable
fun JumatQrFullscreen(
    snapshot: JwsRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val jumat = snapshot.settings.jumat
    val today = snapshot.schedule.date
    val effDate = remember(jumat.keuangan.records, today) {
        JwsStateMachine.findEffectiveJumatKeuanganDate(jumat.keuangan.records, today) ?: today.toString()
    }
    val record = jumat.keuangan.records[effDate] ?: JumatKeuanganRecord(saldoKasLalu = jumat.keuangan.saldoKasLalu)
    val rupiah = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    val qrPayload = remember(jumat.keuangan, record, effDate, snapshot.settings.brand.name) {
        if (jumat.keuangan.qrType == "custom_url" && jumat.keuangan.qrCustomUrl.isNotBlank()) {
            jumat.keuangan.qrCustomUrl
        } else {
            buildString {
                appendLine(jumat.keuangan.customTitle.ifBlank { "Laporan Infaq Kas Jum'at" })
                appendLine(snapshot.settings.brand.name)
                appendLine("Tanggal: $effDate")
                appendLine("Saldo Lalu: ${rupiah.format(record.saldoKasLalu)}")
                appendLine("Pemasukan: ${rupiah.format(record.pemasukan)}")
                appendLine("Pengeluaran: ${rupiah.format(record.pengeluaran)}")
                appendLine("Saldo Akhir: ${rupiah.format(record.saldoAkhir)}")
            }
        }
    }
    val qrBmp = remember(qrPayload) { generateQrBitmap(qrPayload, 460) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp)
    ) {
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "QR LAPORAN KAS JUM'AT",
                color = palette.accentSecondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = jumat.keuangan.customTitle.ifBlank { "Laporan Infaq Kas Jum'at" },
                color = palette.textPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Periode: $effDate • ${snapshot.settings.brand.name}",
                color = palette.textSecondary,
                fontSize = 22.sp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.bgCard)
                    .border(1.5.dp, palette.border, RoundedCornerShape(22.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Saldo Pekan Lalu: ${rupiah.format(record.saldoKasLalu)}", color = palette.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Pemasukan (+): ${rupiah.format(record.pemasukan)}", color = Color(0xFF34D399), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Pengeluaran (-): ${rupiah.format(record.pengeluaran)}", color = Color(0xFFF87171), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Saldo Akhir: ${rupiah.format(record.saldoAkhir)}", color = palette.accentSecondary, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(4.dp, palette.accentSecondary, RoundedCornerShape(32.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBmp != null) {
                Image(
                    bitmap = qrBmp.asImageBitmap(),
                    contentDescription = "QR Kas Jum'at",
                    modifier = Modifier.size(420.dp)
                )
            }
        }
    }
}
