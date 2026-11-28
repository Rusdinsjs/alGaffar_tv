package id.algaffar.jwstv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole

/**
 * Single Source of Truth for the unified Two-Pane Master-Detail + Modular Sub-Tab
 * settings hierarchy shared 1:1 with the Remote PWA (`remote_html.html`).
 */
data class SettingsSubTabSpec(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector
)

data class SettingsCategorySpec(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tabs: List<SettingsSubTabSpec>
)

enum class SettingsFocusZone {
    MASTER_PANE,
    DETAIL_SUBTABS,
    DETAIL_CONTENT,
    OVERLAY_DRAWER
}

object SettingsNavigationModel {

    val MENU_TREE: List<SettingsCategorySpec> = listOf(
        SettingsCategorySpec(
            id = "masjid",
            label = "Masjid",
            description = "Identitas masjid, logo, QR infaq digital & kas online",
            icon = Icons.Default.Home,
            accentColor = Color(0xFF10B981), // Emerald
            tabs = listOf(
                SettingsSubTabSpec(
                    id = "brand",
                    label = "Identitas & Logo",
                    subtitle = "Nama masjid, alamat, telepon, ukuran font header, dan logo resmi",
                    icon = Icons.Default.Home
                ),
                SettingsSubTabSpec(
                    id = "masjidQr",
                    label = "QR Infaq Digital",
                    subtitle = "Payload QRIS infaq, banner melayang, dan informasi rekening bank",
                    icon = Icons.Default.QrCode2
                ),
                SettingsSubTabSpec(
                    id = "onlineFinance",
                    label = "Keuangan Online",
                    subtitle = "Sinkronisasi Google Sheets CSV/JSON & QR laporan kas masjid",
                    icon = Icons.Default.AccountBalanceWallet
                )
            )
        ),
        SettingsCategorySpec(
            id = "sholat",
            label = "Sholat & Audio",
            description = "Lokasi hisab, tahapan 5 waktu, audio sholat & murattal kustom",
            icon = Icons.Default.Book,
            accentColor = Color(0xFF38BDF8), // Sky Blue
            tabs = listOf(
                SettingsSubTabSpec(
                    id = "prayerLocation",
                    label = "Lokasi & Hisab",
                    subtitle = "Kota/kabupaten Indonesia, koordinat, metode hisab, ihtiyati & koreksi",
                    icon = Icons.Default.LocationOn
                ),
                SettingsSubTabSpec(
                    id = "prayerStages",
                    label = "Tahapan Sholat",
                    subtitle = "Pengaturan Tartil, Tarhim, Adzan, Iqomah, Sholat & petugas per waktu",
                    icon = Icons.Default.Schedule
                ),
                SettingsSubTabSpec(
                    id = "audio",
                    label = "Audio Sholat",
                    subtitle = "Volume master Media3, paket audio 1-klik & slot audio per sholat",
                    icon = Icons.Default.VolumeUp
                ),
                SettingsSubTabSpec(
                    id = "customAudio",
                    label = "Audio Kustom",
                    subtitle = "Perpustakaan audio kustom, siaran langsung & jadwal rutin otomatis",
                    icon = Icons.Default.PlayArrow
                )
            )
        ),
        SettingsCategorySpec(
            id = "tampilan",
            label = "Tampilan",
            description = "Tema visual, tata letak, jam, overlay, slideshow & galeri media",
            icon = Icons.Default.Palette,
            accentColor = Color(0xFFC084FC), // Purple
            tabs = listOf(
                SettingsSubTabSpec(
                    id = "visualStyle",
                    label = "Tema, Layout & Jam",
                    subtitle = "17 tema warna, 3 tata letak, skala 1080p, rotasi otomatis & 8 gaya jam",
                    icon = Icons.Default.Palette
                ),
                SettingsSubTabSpec(
                    id = "overlayInfo",
                    label = "Overlay & Info/PHBI",
                    subtitle = "Floating overlay statis, kartu informasi kegiatan & agenda PHBI",
                    icon = Icons.Default.Info
                ),
                SettingsSubTabSpec(
                    id = "slides",
                    label = "Slideshow Info",
                    subtitle = "Kelola grup slideshow, poster, video, slide teks & slide sistem",
                    icon = Icons.Default.Slideshow
                ),
                SettingsSubTabSpec(
                    id = "mediaStandby",
                    label = "Media & Standby",
                    subtitle = "Mode fullscreen standby, latar layar ibadah & paket gambar 1-klik",
                    icon = Icons.Default.Monitor
                )
            )
        ),
        SettingsCategorySpec(
            id = "acara",
            label = "Acara & Perangkat",
            description = "Jum'at, Ramadhan, Hari Raya, Qurban, Live/CCTV & saklar relay",
            icon = Icons.Default.Star,
            accentColor = Color(0xFFFBBF24), // Amber
            tabs = listOf(
                SettingsSubTabSpec(
                    id = "jumatRamadhan",
                    label = "Jum'at & Ramadhan",
                    subtitle = "Petugas & buku kas Jum'at, jadwal Ramadhan, Ta'jil Excel & sirine",
                    icon = Icons.Default.CalendarMonth
                ),
                SettingsSubTabSpec(
                    id = "hariRayaQurban",
                    label = "Idul Fitri & Adha",
                    subtitle = "Jadwal & petugas Hari Raya serta kelompok peserta Qurban (maks 7/grup)",
                    icon = Icons.Default.Event
                ),
                SettingsSubTabSpec(
                    id = "liveScreensaver",
                    label = "Live & Screensaver",
                    subtitle = "Preset Live Makkah/Madinah/HLS, CCTV RTSP & mode malam anti burn-in",
                    icon = Icons.Default.LiveTv
                ),
                SettingsSubTabSpec(
                    id = "switches",
                    label = "Hardware Saklar",
                    subtitle = "Otomasi 8 channel saklar relay WiFi HTTP & USB Serial OTG",
                    icon = Icons.Default.PowerSettingsNew
                )
            )
        ),
        SettingsCategorySpec(
            id = "sistem",
            label = "Sistem",
            description = "Remote PWA, akses admin/keyboard, backup/OTA & log perangkat",
            icon = Icons.Default.Build,
            accentColor = Color(0xFF2DD4BF), // Teal
            tabs = listOf(
                SettingsSubTabSpec(
                    id = "network",
                    label = "Remote & Jaringan",
                    subtitle = "QR Pairing Remote PWA (Port 1421), token akses & jaringan WiFi",
                    icon = Icons.Default.Wifi
                ),
                SettingsSubTabSpec(
                    id = "accessInput",
                    label = "Akses & Input",
                    subtitle = "Mode keyboard TV/PC, kelola akun admin/PIN & kontrol kiosk",
                    icon = Icons.Default.AdminPanelSettings
                ),
                SettingsSubTabSpec(
                    id = "backupUpdate",
                    label = "Backup & Update",
                    subtitle = "Memori TV, backup/restore ZIP, sync USB & pembaruan aplikasi OTA",
                    icon = Icons.Default.Download
                ),
                SettingsSubTabSpec(
                    id = "logsDevice",
                    label = "Log & Perangkat",
                    subtitle = "Simulasi state (Superadmin), log aktivitas real-time, panduan remote & kendali STB",
                    icon = Icons.Default.Terminal
                )
            )
        )
    )

    fun filterForRole(role: UserRole): List<SettingsCategorySpec> =
        MENU_TREE.map { cat ->
            cat.copy(tabs = cat.tabs.filter { AuthManager.canAccessTab(role, it.id) })
        }.filter { it.tabs.isNotEmpty() }

    fun footerHintForZone(zone: SettingsFocusZone): String = when (zone) {
        SettingsFocusZone.MASTER_PANE ->
            "▲▼ Ganti Kategori (Pratinjau Langsung)  •  ▶ / OK Masuk Pengaturan  •  BACK Tutup"
        SettingsFocusZone.DETAIL_SUBTABS ->
            "◀▶ Pilih Sub-Menu  •  ▼ Masuk Form  •  BACK Kembali ke Kategori"
        SettingsFocusZone.DETAIL_CONTENT ->
            "▲▼ Pindah Opsi  •  ◀▶ Ubah Stepper/Slider  •  OK Pilih/Buka Laci  •  BACK ke Kategori"
        SettingsFocusZone.OVERLAY_DRAWER ->
            "▲▼ Navigasi Laci  •  OK Pilih/Simpan  •  BACK / ESC Tutup Laci"
    }

    /** Clamped step arithmetic for single-focus Inline Stepper & Horizontal Slider controls. */
    fun computeSteppedInt(
        current: Int,
        deltaSteps: Int,
        min: Int,
        max: Int,
        step: Int = 1
    ): Int {
        val safeStep = step.coerceAtLeast(1)
        val safeMin = minOf(min, max)
        val safeMax = maxOf(min, max)
        return (current + deltaSteps * safeStep).coerceIn(safeMin, safeMax)
    }

    /** Cyclic option stepping for 2–4 item Inline Option Steppers (`< Opsi >`). */
    fun computeSteppedOptionIndex(
        currentIndex: Int,
        delta: Int,
        optionCount: Int
    ): Int {
        if (optionCount <= 0) return 0
        val normalized = ((currentIndex % optionCount) + optionCount) % optionCount
        return ((normalized + delta) % optionCount + optionCount) % optionCount
    }
}
