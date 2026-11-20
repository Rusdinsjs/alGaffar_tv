package id.algaffar.jwstv.ui.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

/**
 * Category 1: MASJID (`brand`, `masjidQr`, `onlineFinance`).
 * Mirrors `BrandSettingsForm.tsx`, `MasjidQrSettingsForm.tsx`, and `OnlineFinanceSettingsForm.tsx`.
 */

@Composable
fun BrandSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val brand = settings.brand
    val resolvedLogo: Any? = remember(brand.logo, actions.assetsRootDir) {
        if (brand.logo.isBlank()) null
        else if (brand.logo.startsWith("http") || brand.logo.startsWith("file://") || brand.logo.startsWith("content://")) brand.logo
        else actions.assetsRootDir?.let { File(it, brand.logo.removePrefix("jws-assets/").removePrefix("/")).takeIf { f -> f.exists() } }
            ?: File(brand.logo).takeIf { it.exists() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Identitas & Profil Masjid",
            subtitle = "Nama, alamat, nomor telepon, dan konfigurasi tanggal header layar TV"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridTextInput(
                    label = "Nama Masjid",
                    value = brand.name,
                    placeholder = "Contoh: MASJID AL-GAFFAR",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(name = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Nomor Telepon / DKM",
                    value = brand.phone,
                    placeholder = "0812-3456-7890",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(phone = v)) } },
                    modifier = Modifier.weight(0.7f)
                )
            }
            HybridTextInput(
                label = "Alamat Lengkap Masjid",
                value = brand.address,
                placeholder = "Jl. Perjuangan, Balikpapan, Kalimantan Timur",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(address = v)) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridNumberStepper(
                    label = "Ukuran Font Nama Masjid",
                    value = brand.titleFontSize,
                    min = 24,
                    max = 54,
                    unit = "sp",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(titleFontSize = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Ukuran Font Alamat",
                    value = brand.addressFontSize,
                    min = 12,
                    max = 30,
                    unit = "sp",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(addressFontSize = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridToggleRow(
                    label = "Tampilkan Tanggal Hijriah",
                    checked = brand.showHijriDate,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(showHijriDate = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Tampilkan Tanggal Masehi",
                    checked = brand.showMasehiDate,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(showMasehiDate = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HybridSectionCard(
            title = "Logo Resmi Masjid",
            subtitle = "Pilih file gambar logo (PNG transparan / JPG) dari memori TV atau Flashdisk USB"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(palette.bgPrimary)
                        .border(2.dp, palette.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (resolvedLogo != null) {
                        AsyncImage(
                            model = resolvedLogo,
                            contentDescription = "Logo Masjid",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = palette.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (brand.logo.isBlank()) "Belum ada logo kustom (menggunakan ikon default)" else "Path aktif: ${brand.logo}",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HybridButton(
                            label = "Pilih Logo dari Memori / USB",
                            icon = Icons.Default.Upload,
                            isPrimary = true,
                            onClick = {
                                actions.onPickImageAsset("logo") { savedPath ->
                                    actions.onUpdateSettings { it.copy(brand = it.brand.copy(logo = savedPath)) }
                                }
                            }
                        )
                        if (brand.logo.isNotBlank()) {
                            HybridButton(
                                label = "Hapus Logo",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                onClick = {
                                    actions.onUpdateSettings { it.copy(brand = it.brand.copy(logo = "")) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasjidQrSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val qr = settings.masjidQr
    val qrBmp = remember(qr.link) {
        if (qr.link.isNotBlank()) generateQrBitmap(qr.link, 260) else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "QR Code Infaq Digital Masjid",
            subtitle = "Kelola tautan QRIS/donasi, label tampilan, dan detail rekening bank masjid"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridToggleRow(
                    label = "Aktifkan QR Infaq Masjid",
                    checked = qr.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(enabled = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Tampilkan Lencana Floating di Slide Utama",
                    checked = qr.showFloatingOnMain,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(showFloatingOnMain = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            HybridTextInput(
                label = "Tautan / Payload QRIS Infaq",
                value = qr.link,
                placeholder = "https://masjidalgaffar.id/infaq atau payload QRIS",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(link = v)) } }
            )
            HybridTextInput(
                label = "Label Teks QR Infaq",
                value = qr.label,
                placeholder = "Infaq & Sedekah Digital Masjid Al-Gaffar",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(label = v)) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Nama Bank (Opsional)",
                    value = qr.bankName,
                    placeholder = "BSI / Bank Muamalat",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(bankName = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Nomor Rekening",
                    value = qr.nomorRekening,
                    placeholder = "86776543",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(nomorRekening = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Atas Nama (a.n.)",
                    value = qr.atasNama,
                    placeholder = "DKM Masjid Al-Gaffar",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(atasNama = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live QR Preview Card
        HybridSectionCard(
            title = "Pratinjau Tampilan QR Infaq di Layar",
            subtitle = "QR Code diperbarui secara otomatis setiap kali tautan diubah"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBmp != null) {
                        Image(
                            bitmap = qrBmp.asImageBitmap(),
                            contentDescription = "Preview QR Infaq",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = qr.label.ifBlank { "Infaq Digital Masjid" },
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = qr.link.ifBlank { "Tautan belum diisi" },
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (qr.bankName.isNotBlank() || qr.nomorRekening.isNotBlank()) {
                        Text(
                            text = "${qr.bankName} • No. ${qr.nomorRekening}" +
                                if (qr.atasNama.isNotBlank()) " (a.n. ${qr.atasNama})" else "",
                            color = Color(0xFF34D399),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineFinanceSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val fin = settings.onlineFinance
    val rupiah = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }
    val qrBmp = remember(fin.reportUrl) {
        if (fin.reportUrl.isNotBlank()) generateQrBitmap(fin.reportUrl, 240) else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Keuangan Online (Google Sheets CSV / JSON)",
            subtitle = "Sinkronisasi otomatis laporan kas umum masjid & QR laporan detail web"
        ) {
            HybridToggleRow(
                label = "Aktifkan Sinkronisasi Laporan Keuangan Online",
                description = "Hubungkan TV dengan link Google Sheets yang sudah di-Publish to Web (.csv)",
                checked = fin.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(enabled = v)) } }
            )

            HybridInfoBanner(
                text = "Modul ini menampilkan Laporan Kas Umum Masjid via Google Sheets dan berdiri terpisah dari pembukuan Kas Jum'at manual (kecuali jika di tab Jum'at Anda memilih sumber Online).",
                accentColor = Color(0xFF10B981)
            )

            HybridTextInput(
                label = "URL Data Google Sheets (Publish CSV / Edit URL / JSON)",
                value = fin.sheetUrl,
                placeholder = "https://docs.google.com/spreadsheets/d/.../pub?output=csv",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(sheetUrl = v)) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton(
                    label = "Uji Data Online Sekarang",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    enabled = fin.sheetUrl.isNotBlank(),
                    onClick = actions.onTestOnlineFinance
                )
                HybridButton(
                    label = "Buat Otomatis URL Laporan Detail",
                    icon = Icons.Default.AutoAwesome,
                    enabled = fin.sheetUrl.isNotBlank(),
                    onClick = {
                        val nama = URLEncoder.encode(
                            settings.brand.name.replace(Regex("^Masjid\\s+", RegexOption.IGNORE_CASE), "").uppercase(),
                            "UTF-8"
                        )
                        val dataUrl = URLEncoder.encode(fin.sheetUrl.trim(), "UTF-8")
                        val rek = URLEncoder.encode("${fin.bankName} ${fin.nomorRekening}".trim(), "UTF-8")
                        val an = URLEncoder.encode(fin.atasNama.ifBlank { "DKM Masjid" }, "UTF-8")
                        val generated = "https://charming-lily-4e4270.netlify.app/?data=$dataUrl&nama=$nama&rek=$rek&an=$an"
                        actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(reportUrl = generated)) }
                    }
                )
            }

            // Live Test Connection Result
            val testRes = actions.onlineFinanceTestResult
            if (testRes != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (!testRes.isLocalFallback) Color(0xFF064E3B).copy(alpha = 0.55f)
                            else Color(0xFF7F1D1D).copy(alpha = 0.45f)
                        )
                        .border(
                            1.dp,
                            if (!testRes.isLocalFallback) Color(0xFF10B981) else Color(0xFFF87171),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SALDO KAS", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(rupiah.format(testRes.balance), color = Color(0xFF6EE7B7), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PEMASUKAN", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(rupiah.format(testRes.income), color = Color(0xFF34D399), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PENGELUARAN", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(rupiah.format(testRes.expense), color = Color(0xFFFDA4AF), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Nama Bank",
                    value = fin.bankName,
                    placeholder = "Bank Muamalat / BSI",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(bankName = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Nomor Rekening",
                    value = fin.nomorRekening,
                    placeholder = "86776543",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(nomorRekening = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Atas Nama (a.n.)",
                    value = fin.atasNama,
                    placeholder = "DKM Masjid Al-Gaffar",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(atasNama = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            HybridTextInput(
                label = "URL Laporan Detail (Untuk QR Code Keuangan)",
                value = fin.reportUrl,
                placeholder = "https://...",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(reportUrl = v)) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridNumberStepper(
                    label = "Interval Refresh Data",
                    value = fin.refreshIntervalMinutes,
                    min = 1,
                    max = 120,
                    unit = "menit",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(refreshIntervalMinutes = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Tampilkan di Slide Utama",
                    checked = fin.showInMainSlide,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(showInMainSlide = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (qrBmp != null) {
            HybridSectionCard(
                title = "Pratinjau QR Code Laporan Keuangan Online",
                subtitle = "Ditampilkan saat slide / mode `finance_qr` aktif"
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Image(
                        bitmap = qrBmp.asImageBitmap(),
                        contentDescription = "Preview QR Keuangan",
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LAPORAN KEUANGAN ONLINE", color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        if (fin.bankName.isNotBlank() || fin.nomorRekening.isNotBlank()) {
                            Text(
                                text = "${fin.bankName} • ${fin.nomorRekening} (a.n. ${fin.atasNama.ifBlank { "DKM" }})",
                                color = Color(0xFF34D399),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = fin.reportUrl,
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
