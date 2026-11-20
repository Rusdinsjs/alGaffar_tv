package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.logging.AppLogger
import id.algaffar.jwstv.data.logging.LogLevel
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme

/**
 * Category 5: SISTEM (`network`, `userManagement`, `backup`, `logs`, `remote_help`).
 */

@Composable
fun RemoteNetworkSettingsTab(actions: SettingsActions) {
    val palette = LocalJwsTheme.current
    val qrBmp = remember(actions.remoteQrPairingUrl) {
        if (actions.remoteQrPairingUrl.isNotBlank()) generateQrBitmap(actions.remoteQrPairingUrl, 340) else null
    }

    HybridSectionCard(
        title = "Web Remote Control PWA (Ktor Server Port 1421)",
        subtitle = "Scan QR Code dengan kamera smartphone di jaringan WiFi yang sama untuk kendali penuh tanpa mengetik token"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (qrBmp != null) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(3.dp, palette.accent, RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBmp.asImageBitmap(),
                        contentDescription = "QR Remote Server",
                        modifier = Modifier.size(182.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.85f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text("ALAMAT URL REMOTE HP", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = actions.remoteServerUrl,
                        color = palette.accent,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.85f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text("TOKEN AKSES / PIN ADMIN", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = actions.remoteAuthToken.ifBlank { "732911" },
                        color = palette.accentSecondary,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.14f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                    Text(
                        text = "HP dan TV wajib terhubung dalam satu jaringan WiFi / LAN yang sama. Mendukung instalasi PWA (Add to Home Screen).",
                        color = Color(0xFFD1FAE5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun UserManagementSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    var newUsername by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.ADMIN) }
    val superadminCount = settings.users.count { it.role.equals("superadmin", ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Daftar Akun Pengurus DKM (${settings.users.size} Akun)",
            subtitle = "Sistem autentikasi PIN SHA-256 dengan 3 tingkat hak akses (Super Admin, Admin DKM, Operator)"
        ) {
            settings.users.forEach { acc ->
                val isSuperadmin = acc.role.equals("superadmin", ignoreCase = true)
                val roleColor = when {
                    isSuperadmin -> Color(0xFFFBBF24)
                    acc.role.equals("admin", ignoreCase = true) -> Color(0xFF38BDF8)
                    else -> Color(0xFF34D399)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.85f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(acc.username, color = palette.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(roleColor.copy(alpha = 0.18f))
                                .border(1.dp, roleColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(UserRole.fromId(acc.role).displayLabel, color = roleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!isSuperadmin || superadminCount > 1) {
                        HybridButton(
                            label = "Hapus Akun",
                            icon = Icons.Default.Delete,
                            isDanger = true,
                            onClick = {
                                actions.onUpdateSettings { prev ->
                                    prev.copy(users = prev.users.filterNot { it.id == acc.id })
                                }
                            }
                        )
                    } else {
                        Text("Akun Utama (Dilindungi)", color = palette.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        HybridSectionCard(
            title = "Tambah atau Perbarui Akun PIN Baru",
            subtitle = "Jika username sudah ada, PIN & perannya akan diperbarui"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Username",
                    value = newUsername,
                    placeholder = "Contoh: marbot / bendahara",
                    onValueChange = { newUsername = it },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "PIN Akses (4–8 Digit Angka)",
                    value = newPin,
                    placeholder = "123456",
                    onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(8) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserRole.entries.forEach { role ->
                    HybridButton(
                        label = role.displayLabel,
                        isPrimary = (newRole == role),
                        onClick = { newRole = role }
                    )
                }
                HybridButton(
                    label = "+ Simpan Akun",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    enabled = newUsername.isNotBlank() && newPin.length >= 4,
                    onClick = {
                        val acc = UserAccount(
                            id = "user-${System.currentTimeMillis()}",
                            username = newUsername.trim(),
                            pinHash = AuthManager.sha256Hex(newPin),
                            role = newRole.id
                        )
                        actions.onUpdateSettings { prev ->
                            prev.copy(
                                users = prev.users.filterNot { it.username.equals(acc.username, ignoreCase = true) } + acc
                            )
                        }
                        newUsername = ""
                        newPin = ""
                    }
                )
            }
        }
    }
}

@Composable
fun BackupRestoreSettingsTab(actions: SettingsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Backup & Restore Lengkap (.zip) — Kompatibel Tauri Lama",
            subtitle = "Cadangkan atau pulihkan seluruh konfigurasi JSON + seluruh aset media (logo, slide, audio) dalam 1 file ZIP"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridButton(
                    label = "Export Full Backup (.zip)",
                    icon = Icons.Default.Download,
                    isPrimary = true,
                    onClick = actions.onExportBackupZip
                )
                HybridButton(
                    label = "Restore / Import Backup (.zip)",
                    icon = Icons.Default.Upload,
                    isPrimary = true,
                    onClick = actions.onImportBackupZip
                )
            }
            HybridInfoBanner(
                text = "Direktori aset aktif: ${actions.assetsRootDir?.absolutePath ?: "jws-assets/"}. Restore otomatis memigrasikan path asset.localhost dari aplikasi Tauri versi desktop/lama.",
                accentColor = Color(0xFF14B8A6)
            )
        }
    }
}

@Composable
fun LogViewerSettingsTab() {
    val palette = LocalJwsTheme.current
    val allLogs by AppLogger.logsFlow.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf("all") }

    val filtered = remember(allLogs, searchQuery, levelFilter) {
        allLogs.reversed().filter { entry ->
            val matchLevel = levelFilter == "all" || entry.level.id == levelFilter
            val matchQuery = searchQuery.isBlank() ||
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.tag.contains(searchQuery, ignoreCase = true)
            matchLevel && matchQuery
        }
    }

    HybridSectionCard(
        title = "Log Aktivitas Sistem Real-Time (${filtered.size} / ${allLogs.size})",
        subtitle = "Memantau transisi state machine, audio, saklar relay, remote server, dan sinkronisasi online",
        headerAction = {
            HybridButton(
                label = "Bersihkan Log",
                icon = Icons.Default.Delete,
                isDanger = true,
                onClick = { AppLogger.clear() }
            )
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            HybridTextInput(
                label = "Cari Kata Kunci / Tag",
                value = searchQuery,
                placeholder = "STATE, AUDIO, LIVE, RELAY...",
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f)
            )
        }

        HybridSegmentedRow(
            options = listOf(
                "all" to "Semua Level",
                "info" to "INFO",
                "warn" to "WARN",
                "error" to "ERROR",
                "debug" to "DEBUG"
            ),
            selectedId = levelFilter,
            onSelect = { levelFilter = it }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF090F1E))
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = "Belum ada log yang cocok dengan filter saat ini.",
                    color = palette.textSecondary,
                    fontSize = 14.sp
                )
            } else {
                filtered.take(80).forEach { log ->
                    val lvlColor = when (log.level) {
                        LogLevel.ERROR -> Color(0xFFF87171)
                        LogLevel.WARN -> Color(0xFFFBBF24)
                        LogLevel.DEBUG -> Color(0xFF38BDF8)
                        LogLevel.INFO -> Color(0xFF34D399)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(log.timestamp, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = log.level.label,
                            color = lvlColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("[${log.tag}]", color = palette.accentSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(log.message, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteHelpSettingsTab() {
    val palette = LocalJwsTheme.current
    val shortcuts = listOf(
        "⬆ ⬇ ⬅ ➡ D-Pad" to "Navigasi antar Kategori, Sub-Menu & Form Pengaturan",
        "OK / Enter" to "Pilih / Aktifkan Tombol, Toggle, atau Stepper Angka",
        "Back / ESC" to "Kembali atau Tutup Modal Pengaturan",
        "Bar Kategori Bawah" to "5 Kategori Utama: Masjid, Sholat, Tampilan, Acara, Sistem",
        "Bar Sub-Menu" to "Sub-menu horizontal untuk setiap kategori dengan highlight fokus kuning",
        "Panel Simulasi" to "Uji langsung seluruh 31 State (Adzan, Doa Adzan, Jum'at, Tarawih, CCTV, Live)"
    )

    HybridSectionCard(
        title = "Panduan Navigasi Remote Android TV & Touchscreen",
        subtitle = "Dirancang khusus agar nyaman dioperasikan dengan Remote TV D-Pad maupun layar sentuh tablet/monitor"
    ) {
        shortcuts.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { (key, desc) ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141E33))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, TvFocusAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(key, color = TvFocusAmber, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                        }
                        Text(desc, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
