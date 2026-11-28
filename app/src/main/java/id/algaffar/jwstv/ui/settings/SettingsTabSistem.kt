package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
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
import id.algaffar.jwstv.data.media.MediaStorageManager
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TOKEN AKSES PERSISTEN (32 KARAKTER)", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = actions.remoteAuthToken.ifBlank { "(Belum tersedia)" },
                                color = palette.accentSecondary,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        }
                        HybridButton(
                            label = "Regenerate Token",
                            icon = Icons.Default.Refresh,
                            isDanger = true,
                            onClick = actions.onRegenerateRemoteToken
                        )
                    }
                    if (actions.deviceCode.isNotBlank()) {
                        Text(
                            text = "Device ID STB (Pemulihan CS): ${actions.deviceCode}",
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            subtitle = "Sistem autentikasi PIN PBKDF2-SHA256 (salted) dengan 3 tingkat hak akses (Super Admin, Admin DKM, Operator)"
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
                            pinHash = AuthManager.hashPinPbkdf2(newPin),
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
fun BackupRestoreSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val health = actions.storageHealth
    val statusColor = if (health.isLowStorage) Color(0xFFEF4444) else Color(0xFF10B981)
    val usedFraction = (health.internalUsedPercent / 100f).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 1. Storage Guard & Orphan Media Cleaner
        HybridSectionCard(
            title = "Pengaman Memori Internal (Storage Guard)",
            subtitle = "Pantau sisa ruang STB/TV, rincian aset lokal `jws-assets/`, dan bersihkan gambar/video lama yang sudah tidak dipakai",
            headerAction = {
                HybridButton(
                    label = "Refresh Storage",
                    icon = Icons.Default.Refresh,
                    onClick = actions.onRefreshStorageHealth
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StorageStatBox(
                    title = "SISA MEMORI INTERNAL TV",
                    value = MediaStorageManager.formatByteSize(health.usableInternalBytes),
                    sub = if (health.isLowStorage) "MEMORI RENDAH (< 300 MB)" else "AMAN • Batas minimal 300 MB",
                    accentColor = statusColor,
                    modifier = Modifier.weight(1.2f)
                )
                StorageStatBox(
                    title = "TOTAL ASET JWS (${health.totalFileCount} FILE)",
                    value = MediaStorageManager.formatByteSize(health.jwsAssetsUsedBytes),
                    sub = "Img ${MediaStorageManager.formatByteSize(health.imagesBytes)} • Aud ${MediaStorageManager.formatByteSize(health.audioBytes)} • Vid ${MediaStorageManager.formatByteSize(health.videosBytes)}",
                    accentColor = palette.accent,
                    modifier = Modifier.weight(1.5f)
                )
                StorageStatBox(
                    title = "FILE TIDAK TERPAKAI (YATIM)",
                    value = "${health.orphanFileCount} File",
                    sub = if (health.orphanFileCount > 0) "Bisa dihemat ${MediaStorageManager.formatByteSize(health.orphanBytes)}" else "Penyimpanan bersih",
                    accentColor = if (health.orphanFileCount > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
                    modifier = Modifier.weight(1f)
                )
            }

            // Internal Storage Usage Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pemakaian Partisi Internal (${health.internalUsedPercent}%)",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total ${MediaStorageManager.formatByteSize(health.totalInternalBytes)}",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.bgPrimary)
                        .border(1.dp, palette.border, RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(usedFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(statusColor)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton(
                    label = if (health.orphanFileCount > 0) {
                        "Bersihkan ${health.orphanFileCount} File Tidak Terpakai (${MediaStorageManager.formatByteSize(health.orphanBytes)})"
                    } else {
                        "Bersihkan File Tidak Terpakai"
                    },
                    icon = Icons.Default.CleaningServices,
                    isDanger = health.orphanFileCount > 0,
                    onClick = actions.onCleanOrphanMedia
                )
            }

            HybridInfoBanner(
                text = "Auto-Optimasi Aktif: Semua poster/gambar yang diunggah otomatis diperkecil ke resolusi 1920×1080 (Logo 512×512). Upload/impor otomatis ditolak bila sisa memori internal < 300 MB.",
                accentColor = statusColor
            )
        }

        // 2. USB Bulk Folder Sync
        HybridSectionCard(
            title = "Import / Sync Massal 1 Folder dari Flashdisk USB",
            subtitle = "Salin banyak poster gambar, video (.mp4), dan audio (Tartil/Tarhim/Adzan/Kustom) sekaligus ke gudang internal — setelah selesai Flashdisk aman dicabut"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HybridButton(
                    label = "Sync 1 Folder dari Flashdisk USB",
                    icon = Icons.Default.FolderOpen,
                    isPrimary = true,
                    onClick = actions.onSyncUsbFolder
                )
            }
            HybridInfoBanner(
                text = "Mendukung struktur folder `jws-assets/` (audio/tarhim, audio/adzan, images/slides, videos) MAUPUN folder datar berisi campuran gambar, MP3 (`tarhim_fajr.mp3`, `adzan_default.mp3`, dll.), dan video.",
                accentColor = Color(0xFF38BDF8)
            )
        }

        // 3. Full Backup & Restore ZIP
        HybridSectionCard(
            title = "Backup & Restore Lengkap (.zip) — Kompatibel Tauri Lama",
            subtitle = "Cadangkan atau pulihkan seluruh konfigurasi JSON + seluruh aset media (logo, slide, audio, video) dalam 1 file ZIP"
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

        // 4. Pembaruan Aplikasi (OTA Online & Offline USB) + Onboarding Wizard
        HybridSectionCard(
            title = "Pembaruan Al-Gaffar TV (OTA) & Setup Perdana",
            subtitle = "Perbarui aplikasi via internet (verifikasi SHA-256) atau offline dari file APK di Flashdisk USB / Remote HP"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton(
                    label = "Cek Pembaruan Online (OTA)",
                    icon = Icons.Default.Download,
                    isPrimary = true,
                    onClick = actions.onCheckOtaUpdate
                )
                HybridButton(
                    label = "Update APK dari Flashdisk USB",
                    icon = Icons.Default.Upload,
                    onClick = actions.onInstallOtaApkFromUsb
                )
                HybridButton(
                    label = "Buka Ulang Wizard Setup Perdana",
                    icon = Icons.Default.Refresh,
                    onClick = actions.onReopenOnboarding
                )
            }
            HybridInfoBanner(
                text = "Versi terpasang: Al-Gaffar TV v${id.algaffar.jwstv.BuildConfig.VERSION_NAME} (Build ${id.algaffar.jwstv.BuildConfig.VERSION_CODE}) • Device ID: ${actions.deviceCode.ifBlank { "-" }}. Untuk STB tanpa internet, Anda juga dapat mengunggah file .apk melalui Remote PWA di HP.",
                accentColor = Color(0xFF38BDF8)
            )
        }

        // 5. Mode Kiosk 24/7, Maintenance & Jadwal Restart Otomatis
        val kioskCfg = settings.kioskMaintenance
        val kioskStatusLabel = when {
            !actions.isDeviceOwner -> "Mode Launcher / Non-Device Owner"
            actions.isKioskMaintenanceUnlocked -> "TERBUKA SEMENTARA (Maintenance)"
            else -> "TERKUNCI PENUH (Device Owner Kiosk)"
        }
        HybridSectionCard(
            title = "Mode Kiosk 24/7, Maintenance & Jadwal Restart",
            subtitle = "Status Kiosk: $kioskStatusLabel — Keluar sementara untuk atur Wi-Fi STB atau jadwalkan restart rutin agar RAM selalu segar"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (actions.isDeviceOwner) {
                    HybridButton(
                        label = if (actions.isKioskMaintenanceUnlocked) {
                            "Kunci Kembali Mode Kiosk"
                        } else {
                            "Buka Kunci Kiosk (Maintenance)"
                        },
                        isPrimary = actions.isKioskMaintenanceUnlocked,
                        isDanger = !actions.isKioskMaintenanceUnlocked,
                        onClick = actions.onToggleKioskMaintenance
                    )
                }
                HybridButton(
                    label = "Setelan TV Box (Android TV)",
                    isPrimary = true,
                    onClick = actions.onOpenAndroidSettings
                )
                HybridButton(
                    label = "Pengaturan Wi-Fi",
                    icon = Icons.Default.Wifi,
                    onClick = actions.onOpenWifiSettings
                )
                HybridButton(
                    label = "Restart Aplikasi Sekarang",
                    icon = Icons.Default.Refresh,
                    onClick = actions.onTriggerAppRestartNow
                )
                if (actions.isDeviceOwner) {
                    HybridButton(
                        label = "Reboot TV Box Sekarang",
                        icon = Icons.Default.Refresh,
                        isDanger = true,
                        onClick = actions.onTriggerDeviceRebootNow
                    )
                }
            }

            Text(
                text = "JADWAL RESTART OTOMATIS (SAAT STANDBY)",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            HybridSegmentedRow(
                options = listOf(
                    "off" to "Nonaktif",
                    "daily" to "Setiap Hari",
                    "weekly" to "Mingguan"
                ),
                selectedId = kioskCfg.autoRestartMode,
                onSelect = { mode ->
                    actions.onUpdateSettings { prev ->
                        prev.copy(kioskMaintenance = prev.kioskMaintenance.copy(autoRestartMode = mode))
                    }
                }
            )

            if (kioskCfg.autoRestartMode != "off") {
                HybridSegmentedRow(
                    options = listOf(
                        "app" to "Restart Aplikasi (Aman untuk semua TV Box)",
                        "reboot_device" to "Reboot OS TV Box (Khusus Device Owner)"
                    ),
                    selectedId = kioskCfg.autoRestartAction,
                    onSelect = { act ->
                        actions.onUpdateSettings { prev ->
                            prev.copy(kioskMaintenance = prev.kioskMaintenance.copy(autoRestartAction = act))
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HybridNumberStepper(
                        label = "Jam Restart (WIB/Lokal)",
                        value = kioskCfg.autoRestartHour,
                        min = 0,
                        max = 23,
                        unit = "jam",
                        onValueChange = { h ->
                            actions.onUpdateSettings { prev ->
                                prev.copy(kioskMaintenance = prev.kioskMaintenance.copy(autoRestartHour = h))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridNumberStepper(
                        label = "Menit Restart",
                        value = kioskCfg.autoRestartMinute,
                        min = 0,
                        max = 59,
                        step = 5,
                        unit = "menit",
                        onValueChange = { m ->
                            actions.onUpdateSettings { prev ->
                                prev.copy(kioskMaintenance = prev.kioskMaintenance.copy(autoRestartMinute = m))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (kioskCfg.autoRestartMode == "weekly") {
                        HybridNumberStepper(
                            label = "Hari (1=Senin .. 7=Minggu)",
                            value = kioskCfg.autoRestartDayOfWeek,
                            min = 1,
                            max = 7,
                            unit = "hari",
                            onValueChange = { d ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(kioskMaintenance = prev.kioskMaintenance.copy(autoRestartDayOfWeek = d))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HybridInfoBanner(
                text = "Restart terjadwal hanya dieksekusi saat aplikasi berada di mode Standby (tidak pernah memotong Adzan, Iqomah, Sholat, Khutbah, atau Tarawih). Jika terjadi crash mendadak, aplikasi otomatis pulih dalam ~2 detik.",
                accentColor = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun StorageStatBox(
    title: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgPrimary.copy(alpha = 0.85f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )
        Text(
            text = sub,
            color = palette.textSecondary,
            fontSize = 11.sp
        )
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
fun KeyboardInputSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val vkPolicy = LocalVirtualKeyboardPolicy.current
    var testInput by remember { mutableStateOf("") }

    val statusColor = if (vkPolicy.allowVirtualKeyboard) Color(0xFF38BDF8) else Color(0xFF10B981)
    val hwColor = if (vkPolicy.hasPhysicalKeyboard) Color(0xFF10B981) else Color(0xFFF59E0B)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Pengaturan Keyboard Virtual (On-Screen Keyboard)",
            subtitle = "Nonaktifkan keyboard virtual secara otomatis saat menggunakan keyboard fisik / PC, atau atur secara manual"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hardware detection status card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(hwColor.copy(alpha = 0.12f))
                        .border(1.dp, hwColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DETEKSI KEYBOARD FISIK / PC",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (vkPolicy.hasPhysicalKeyboard) "TERDETEKSI" else "TIDAK TERDETEKSI",
                        color = hwColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (vkPolicy.detectedKeyboardNames.isNotEmpty()) {
                            vkPolicy.detectedKeyboardNames.joinToString(" • ")
                        } else {
                            "Tidak ada keyboard USB / Bluetooth / PC terhubung"
                        },
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }

                // Effective virtual keyboard status card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "STATUS KEYBOARD VIRTUAL SAAT INI",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (vkPolicy.allowVirtualKeyboard) "AKTIF (MUNCUL)" else "NONAKTIF (DISEMBUNYIKAN)",
                        color = statusColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when (vkPolicy.normalizedMode) {
                            VirtualKeyboardPolicy.MODE_ENABLED -> "Dipaksa aktif oleh pengaturan manual"
                            VirtualKeyboardPolicy.MODE_DISABLED -> "Dimatikan secara manual (hanya ketik keyboard fisik)"
                            else -> if (vkPolicy.hasPhysicalKeyboard) {
                                "Otomatis nonaktif karena keyboard fisik/PC terdeteksi"
                            } else {
                                "Otomatis aktif karena tidak ada keyboard fisik"
                            }
                        },
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = "PILIH MODE KEYBOARD VIRTUAL",
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            HybridSegmentedRow(
                options = listOf(
                    VirtualKeyboardPolicy.MODE_AUTO to "Otomatis (Nonaktif jika Keyboard PC Terdeteksi)",
                    VirtualKeyboardPolicy.MODE_DISABLED to "Nonaktif (Matikan Keyboard Virtual)",
                    VirtualKeyboardPolicy.MODE_ENABLED to "Aktif (Selalu Tampilkan Keyboard Virtual)"
                ),
                selectedId = vkPolicy.normalizedMode,
                onSelect = { selected ->
                    actions.onUpdateSettings { it.copy(virtualKeyboardMode = selected) }
                }
            )

            HybridInfoBanner(
                text = "Pada mode 'Otomatis' atau 'Nonaktif', kotak teks tetap bisa menerima ketikan langsung dari keyboard USB/Bluetooth/PC tanpa memunculkan popup keyboard virtual yang menutupi layar.",
                accentColor = Color(0xFF10B981)
            )
        }

        HybridSectionCard(
            title = "Uji Coba Input Teks",
            subtitle = "Fokuskan kotak di bawah lalu ketik dengan keyboard PC / Remote untuk memastikan perilaku keyboard virtual"
        ) {
            HybridTextInput(
                label = "Kotak Uji Coba Pengetikan",
                value = testInput,
                placeholder = "Klik / tekan OK di sini lalu coba ketik...",
                onValueChange = { testInput = it }
            )
        }
    }
}

@Composable
fun RemoteHelpSettingsTab() {
    val palette = LocalJwsTheme.current
    val shortcuts = listOf(
        "Panel Kiri (▲ ▼)" to "Ganti kategori utama (pratinjau panel kanan langsung berubah tanpa OK)",
        "Masuk / Kembali" to "Tekan ▶ atau OK untuk masuk Panel Kanan • Tekan ◀ atau BACK untuk kembali ke Kategori",
        "Stepper & Slider" to "Fokus 1 baris lalu tekan ◀ ▶ untuk ubah angka/opsi/volume langsung di tempat",
        "Laci Samping (Level 3)" to "Opsi lanjutan (Tahapan Sholat, Qurban, Saklar) terbuka di laci kanan dengan focus trap",
        "Mouse + Keyboard" to "Hover mouse otomatis memindahkan fokus D-Pad agar transisi mouse ↔ remote mulus",
        "Remote PWA (Port 1421)" to "Struktur menu 100% sama dengan TV; layar lebar memakai Two-Pane, HP memakai Bottom Nav"
    )

    HybridSectionCard(
        title = "Panduan Navigasi Two-Pane (D-Pad, Mouse+Keyboard & Remote PWA)",
        subtitle = "Dirancang khusus agar nyaman dioperasikan dengan Remote TV, Mouse+Keyboard, maupun Smartphone"
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
                            .background(TvSurfaceRow)
                            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TvSurfaceCanvas)
                                .border(1.dp, TvFocusCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(key, color = TvFocusCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                        }
                        Text(desc, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Modular Sub-Tab 5.2: Akses & Input (`accessInput`).
 * Combines Keyboard Policy inline stepper + Level-3 Side Drawer for Admin PIN Management (Superadmin only).
 */
@Composable
fun AccessAndInputSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions,
    activeRole: UserRole
) {
    val vkPolicy = LocalVirtualKeyboardPolicy.current
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "keyboard" | "users"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Pengaturan Keyboard Virtual & Perangkat Input",
            subtitle = if (vkPolicy.hasPhysicalKeyboard) {
                "Keyboard fisik terdeteksi: ${vkPolicy.detectedKeyboardNames.joinToString(", ")}"
            } else {
                "Tidak ada keyboard fisik terdeteksi — menggunakan Remote D-Pad / Touchscreen"
            }
        ) {
            HybridInlineOptionStepper(
                label = "Mode Keyboard Virtual (On-Screen Keyboard)",
                description = "Tekan ◀ ▶ untuk beralih antara Otomatis, Nonaktif, atau Selalu Aktif",
                options = listOf(
                    VirtualKeyboardPolicy.MODE_AUTO to "Otomatis (Ikuti Keyboard PC)",
                    VirtualKeyboardPolicy.MODE_DISABLED to "Nonaktif (Keyboard PC Saja)",
                    VirtualKeyboardPolicy.MODE_ENABLED to "Aktif (Selalu Muncul)"
                ),
                selectedId = vkPolicy.normalizedMode,
                icon = Icons.Default.Keyboard,
                onSelect = { selected ->
                    actions.onUpdateSettings { it.copy(virtualKeyboardMode = selected) }
                }
            )
            HybridNavigationRow(
                label = "Detail Status Hardware & Uji Ketik",
                description = "Periksa daftar perangkat keyboard terdeteksi dan uji kotak pengetikan",
                valueSummary = if (vkPolicy.allowVirtualKeyboard) "OSK Aktif" else "OSK Tersembunyi",
                icon = Icons.Default.Keyboard,
                accentBadgeColor = Color(0xFF2DD4BF),
                onClick = { activeDrawer = "keyboard" }
            )
        }

        if (activeRole == UserRole.SUPERADMIN) {
            HybridSectionCard(
                title = "Manajemen Akun Admin & Keamanan PIN (Khusus Superadmin)",
                subtitle = "Kelola akun pengurus DKM (Superadmin, Admin, Operator) beserta PIN PBKDF2"
            ) {
                HybridNavigationRow(
                    label = "Kelola Akun Pengurus & PIN Akses",
                    description = "Tambah, ubah PIN, atau hapus akun pengurus masjid dengan proteksi brute-force",
                    valueSummary = "${settings.users.size.coerceAtLeast(1)} Akun",
                    icon = Icons.Default.AdminPanelSettings,
                    accentBadgeColor = Color(0xFF2DD4BF),
                    onClick = { activeDrawer = "users" }
                )
            }
        }
    }

    when (activeDrawer) {
        "keyboard" -> {
            HybridSideDrawer(
                title = "Detail Keyboard & Uji Ketik",
                subtitle = "Konfigurasi lengkap perangkat input TV/STB",
                accentColor = Color(0xFF2DD4BF),
                onDismiss = { activeDrawer = null }
            ) {
                KeyboardInputSettingsTab(settings, actions)
            }
        }
        "users" -> {
            HybridSideDrawer(
                title = "Kelola Admin & PIN Akses",
                subtitle = "Autentikasi multi-role (Superadmin / Admin / Operator)",
                accentColor = Color(0xFF2DD4BF),
                onDismiss = { activeDrawer = null }
            ) {
                UserManagementSettingsTab(settings, actions)
            }
        }
    }
}

/**
 * Modular Sub-Tab 5.3: Backup & Update (`backupUpdate`).
 */
@Composable
fun BackupAndUpdateSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    BackupRestoreSettingsTab(settings, actions)
}

/**
 * Modular Sub-Tab 5.4: Log & Perangkat (`logsDevice`).
 * Houses the Superadmin-only State Simulation & Audio Test launcher alongside device controls and logs.
 */
@Composable
fun LogsAndDeviceSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions,
    activeRole: UserRole = UserRole.ADMIN
) {
    var showHelpDrawer by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (activeRole == UserRole.SUPERADMIN) {
            HybridSectionCard(
                title = "Panel Simulasi State & Uji Audio (Khusus Superadmin)",
                subtitle = "Uji coba lompat ke 31 state layar ibadah (Adzan, Iqomah, Khutbah, Hari Raya, Qurban) serta uji bunyi Beep & Sirine"
            ) {
                HybridNavigationRow(
                    label = "Buka Panel Simulasi 31-State & Uji Audio",
                    description = "Menutup jendela pengaturan dan menampilkan kontrol simulasi langsung di atas layar TV",
                    valueSummary = "Simulasi TV",
                    icon = Icons.Default.PlayArrow,
                    accentBadgeColor = Color(0xFF2DD4BF),
                    onClick = actions.onOpenDebugSimulator
                )
            }
        }

        HybridSectionCard(
            title = "Panduan Navigasi & Kendali Perangkat STB",
            subtitle = "Pintasan tombol Remote TV / Keyboard serta tombol muat ulang aplikasi"
        ) {
            HybridNavigationRow(
                label = "Panduan Navigasi Remote TV, Mouse & Remote PWA",
                description = "Pelajari alur navigasi Two-Pane, Stepper, Slider, dan Laci Samping",
                valueSummary = "Buka Panduan",
                icon = Icons.Default.Tv,
                accentBadgeColor = Color(0xFF2DD4BF),
                onClick = { showHelpDrawer = true }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton(
                    label = "Restart Aplikasi Sekarang",
                    icon = Icons.Default.Refresh,
                    onClick = actions.onTriggerAppRestartNow,
                    modifier = Modifier.weight(1f)
                )
                if (actions.isDeviceOwner) {
                    HybridButton(
                        label = "Reboot STB Android",
                        icon = Icons.Default.PowerSettingsNew,
                        isDanger = true,
                        onClick = actions.onTriggerDeviceRebootNow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LogViewerSettingsTab()
    }

    if (showHelpDrawer) {
        HybridSideDrawer(
            title = "Panduan Navigasi Pengaturan",
            subtitle = "Remote TV D-Pad, Mouse+Keyboard, dan Remote PWA",
            accentColor = Color(0xFF2DD4BF),
            onDismiss = { showHelpDrawer = false }
        ) {
            RemoteHelpSettingsTab()
        }
    }
}
