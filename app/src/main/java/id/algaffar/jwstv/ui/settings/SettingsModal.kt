package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.JumatJournalEntry
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.domain.model.ScaleMode
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.ThemeEngine
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private enum class SettingsTab(
    val id: String,
    val title: String,
    val roleCategory: String
) {
    BRAND("brand", "1. Identitas & Jam Masjid", "brand"),
    PRAYER("prayer", "2. Kalkulasi Waktu & Hijriah", "prayer"),
    STAGES("stages", "3. Durasi 5 Waktu Sholat", "stages"),
    JUMAT("jumat", "4. Mode Hari Jum'at & Kas", "jumat"),
    RAMADHAN("ramadhan", "5. Mode Ramadhan & Ta'jil", "ramadhan"),
    HOLIDAY("holiday", "6. Hari Raya & Info Qurban", "holiday_qurban"),
    OVERLAY_SLIDES("overlay", "7. Running Text & Mode Malam", "slides_overlay"),
    THEME_LAYOUT("theme", "8. Tema, Layout & Skala Layar", "theme"),
    HARDWARE_MEDIA("hardware", "9. Saklar Relay, CCTV & Live", "hardware"),
    BACKUP_AUTH("backup", "10. Backup (.zip) & Akun PIN", "users_security")
}

/**
 * Full-Featured Hybrid (TV D-Pad + Touchscreen) Settings Modal.
 * Protected by Multi-Role PIN Authentication ([PinLoginGuardScreen]).
 * Mirrors `src/components/Settings/SettingsModal.tsx` and all sub-forms.
 */
@Composable
fun SettingsModal(
    settings: AppStateSettings,
    statusBannerMessage: String,
    takjilImportPreview: List<TakjilImportPreviewRow>?,
    onConfirmTakjilImport: () -> Unit,
    onDismissTakjilImport: () -> Unit,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit,
    onRequestExportTakjilTemplate: () -> Unit,
    onRequestImportTakjilExcel: () -> Unit,
    onRequestExportBackupZip: () -> Unit,
    onRequestImportBackupZip: () -> Unit,
    onOpenDebugSimulator: () -> Unit,
    onClose: () -> Unit,
    remoteServerUrl: String = "http://127.0.0.1:1421/remote",
    remoteQrPairingUrl: String = "http://127.0.0.1:1421/remote",
    remoteAuthToken: String = "",
    relayPinStates: Map<Int, Boolean> = emptyMap(),
    onToggleManualRelay: (Int, Boolean) -> Unit = { _, _ -> },
    onDiscoverOnvifCameras: () -> Unit = {}
) {
    val palette = LocalJwsTheme.current
    var authenticatedUser by remember { mutableStateOf<UserAccount?>(null) }

    val currentUser = authenticatedUser
    if (currentUser == null) {
        PinLoginGuardScreen(
            users = settings.users,
            onAuthenticated = { authenticatedUser = it },
            onCancel = onClose
        )
        return
    }

    val activeRole = remember(currentUser.role) { UserRole.fromId(currentUser.role) }
    val accessibleTabs = remember(activeRole) {
        SettingsTab.entries.filter { AuthManager.canAccessTab(activeRole, it.roleCategory) }
    }

    var selectedTab by remember { mutableStateOf(accessibleTabs.firstOrNull() ?: SettingsTab.BRAND) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(26.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accent, RoundedCornerShape(26.dp))
        ) {
            // 1. Left Sidebar Navigation (D-Pad & Touch)
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(palette.bgSecondary.copy(alpha = 0.8f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PENGATURAN JWS TV",
                        color = palette.accent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Login: ${currentUser.username} (${activeRole.displayLabel})",
                        color = palette.accentSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    accessibleTabs.forEach { tab ->
                        val isSelected = (tab == selectedTab)
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> palette.accent
                                        isFocused -> palette.accent.copy(alpha = 0.28f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isFocused) 2.dp else 0.dp,
                                    color = if (isFocused) palette.accentSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) palette.bgPrimary else palette.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected || isFocused) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = "Panel Simulasi 27 State",
                        onClick = onOpenDebugSimulator,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HybridButton(
                        label = "Selesai & Tutup [ESC]",
                        isPrimary = true,
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Right Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (statusBannerMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accent.copy(alpha = 0.2f))
                            .border(1.5.dp, palette.accent, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = statusBannerMessage,
                            color = palette.accentSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when (selectedTab) {
                    SettingsTab.BRAND -> BrandSettingsTabContent(settings, onUpdateSettings)
                    SettingsTab.PRAYER -> PrayerSettingsTabContent(settings, onUpdateSettings)
                    SettingsTab.STAGES -> FivePrayerStagesTabContent(settings, onUpdateSettings)
                    SettingsTab.JUMAT -> JumatSettingsTabContent(settings, onUpdateSettings)
                    SettingsTab.RAMADHAN -> RamadhanSettingsTabContent(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        onExportExcelTemplate = onRequestExportTakjilTemplate,
                        onImportExcel = onRequestImportTakjilExcel
                    )
                    SettingsTab.HOLIDAY -> HolidayAndQurbanTabContent(settings, onUpdateSettings)
                    SettingsTab.OVERLAY_SLIDES -> OverlayAndNightModeTabContent(settings, onUpdateSettings)
                    SettingsTab.THEME_LAYOUT -> ThemeAndLayoutTabContent(settings, onUpdateSettings)
                    SettingsTab.HARDWARE_MEDIA -> HardwareAndMediaTabContent(
                        settings = settings,
                        relayPinStates = relayPinStates,
                        onToggleManualRelay = onToggleManualRelay,
                        onDiscoverOnvifCameras = onDiscoverOnvifCameras,
                        onUpdateSettings = onUpdateSettings
                    )
                    SettingsTab.BACKUP_AUTH -> BackupAndAuthTabContent(
                        settings = settings,
                        remoteServerUrl = remoteServerUrl,
                        remoteQrPairingUrl = remoteQrPairingUrl,
                        remoteAuthToken = remoteAuthToken,
                        onUpdateSettings = onUpdateSettings,
                        onExportBackupZip = onRequestExportBackupZip,
                        onImportBackupZip = onRequestImportBackupZip
                    )
                }
            }
        }

        // Ta'jil Excel Import Preview Modal
        if (takjilImportPreview != null) {
            TakjilExcelPreviewDialog(
                previewRows = takjilImportPreview,
                onConfirm = onConfirmTakjilImport,
                onCancel = onDismissTakjilImport
            )
        }
    }
}

@Composable
private fun BrandSettingsTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    val brand = settings.brand
    val clockStyles = listOf("classic", "golden", "glass", "wood", "neon")

    HybridSectionCard(
        title = "Identitas Masjid & Tampilan Jam",
        subtitle = "Ubah nama masjid, alamat, ukuran teks header, dan gaya jam artistik"
    ) {
        HybridTextInput(
            label = "Nama Masjid",
            value = brand.name,
            onValueChange = { v -> onUpdateSettings { it.copy(brand = it.brand.copy(name = v)) } }
        )
        HybridTextInput(
            label = "Alamat Lengkap",
            value = brand.address,
            onValueChange = { v -> onUpdateSettings { it.copy(brand = it.brand.copy(address = v)) } }
        )
        HybridTextInput(
            label = "Nomor Telepon / DKM",
            value = brand.phone,
            onValueChange = { v -> onUpdateSettings { it.copy(brand = it.brand.copy(phone = v)) } }
        )
        HybridNumberStepper(
            label = "Ukuran Font Nama Masjid",
            value = brand.titleFontSize,
            min = 24,
            max = 52,
            unit = "sp",
            onValueChange = { v -> onUpdateSettings { it.copy(brand = it.brand.copy(titleFontSize = v)) } }
        )
        HybridToggleRow(
            label = "Tampilkan Tanggal Hijriah di Header",
            checked = brand.showHijriDate,
            onCheckedChange = { v -> onUpdateSettings { it.copy(brand = it.brand.copy(showHijriDate = v)) } }
        )

        Text(
            text = "GAYA JAM HEADER (${brand.clockStyle.uppercase()})",
            color = LocalJwsTheme.current.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            clockStyles.forEach { st ->
                HybridButton(
                    label = st.uppercase(),
                    isPrimary = brand.clockStyle.equals(st, ignoreCase = true),
                    onClick = { onUpdateSettings { it.copy(brand = it.brand.copy(clockStyle = st)) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerSettingsTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    val p = settings.prayerSettings
    val methods = listOf(
        "Singapore", "MuslimWorldLeague", "Egyptian", "Karachi", "UmmAlQura",
        "Dubai", "MoonsightingCommittee", "NorthAmerica", "Kuwait", "Qatar", "Tehran", "Turkey"
    )
    val timezones = listOf(
        "Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", "Asia/Singapore", "Asia/Kuala_Lumpur"
    )

    HybridSectionCard(
        title = "Koordinat, Zona Waktu & Metode Kalkulasi",
        subtitle = "Mendukung 12 metode standar internasional & Kemenag (Singapore) serta koreksi Ihtiyati"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HybridTextInput(
                label = "Latitude (Lintang)",
                value = p.latitude.toString(),
                onValueChange = { v ->
                    v.toDoubleOrNull()?.let { lat ->
                        onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(latitude = lat)) }
                    }
                },
                modifier = Modifier.weight(1f)
            )
            HybridTextInput(
                label = "Longitude (Bujur)",
                value = p.longitude.toString(),
                onValueChange = { v ->
                    v.toDoubleOrNull()?.let { lon ->
                        onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(longitude = lon)) }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "ZONA WAKTU (${p.timezone})",
            color = LocalJwsTheme.current.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            timezones.forEach { tz ->
                HybridButton(
                    label = tz,
                    isPrimary = (p.timezone == tz),
                    onClick = { onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(timezone = tz)) } }
                )
            }
        }

        Text(
            text = "METODE KALKULASI (${p.calculationMethod})",
            color = LocalJwsTheme.current.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEach { m ->
                HybridButton(
                    label = m,
                    isPrimary = (p.calculationMethod == m),
                    onClick = { onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(calculationMethod = m)) } }
                )
            }
        }

        HybridNumberStepper(
            label = "Pengaman Global (Ihtiyati)",
            value = p.ihtiyati,
            min = -5,
            max = 5,
            unit = "menit",
            onValueChange = { v -> onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(ihtiyati = v)) } }
        )
        HybridNumberStepper(
            label = "Penyesuaian Kalender Hijriah",
            value = p.hijriAdjustment,
            min = -3,
            max = 3,
            unit = "hari",
            onValueChange = { v -> onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(hijriAdjustment = v)) } }
        )
        HybridNumberStepper(
            label = "Interval Geser Carousel Jadwal Sholat",
            value = p.prayerTimeScrollInterval,
            min = 2,
            max = 20,
            unit = "detik",
            onValueChange = { v ->
                onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(prayerTimeScrollInterval = v)) }
            }
        )
    }
}

@Composable
private fun FivePrayerStagesTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    var selectedPrayer by remember { mutableStateOf(PrayerId.FAJR) }
    val currentCfg = settings.prayers.forPrayer(selectedPrayer)

    HybridSectionCard(
        title = "Durasi Tahapan Sholat 5 Waktu (Tartil, Tarhim, Adzan, Iqomah, Sholat)",
        subtitle = "Pilih waktu sholat di bawah untuk mengatur durasi dan petugas Imam/Muadzin"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrayerId.FIVE_FARDHU.forEach { pid ->
                HybridButton(
                    label = pid.defaultIndonesianName.uppercase(),
                    isPrimary = (pid == selectedPrayer),
                    onClick = { selectedPrayer = pid }
                )
            }
        }

        fun updateCurrentStage(transform: (id.algaffar.jwstv.domain.model.PrayerStageConfig) -> id.algaffar.jwstv.domain.model.PrayerStageConfig) {
            onUpdateSettings { prev ->
                val updated = transform(prev.prayers.forPrayer(selectedPrayer))
                val nextPrayers = when (selectedPrayer) {
                    PrayerId.FAJR -> prev.prayers.copy(fajr = updated)
                    PrayerId.DHUHR -> prev.prayers.copy(dhuhr = updated)
                    PrayerId.ASR -> prev.prayers.copy(asr = updated)
                    PrayerId.MAGHRIB -> prev.prayers.copy(maghrib = updated)
                    PrayerId.ISHA -> prev.prayers.copy(isha = updated)
                    else -> prev.prayers
                }
                prev.copy(prayers = nextPrayers)
            }
        }

        HybridNumberStepper(
            label = "Durasi Tartil Al-Qur'an (Sebelum Adzan)",
            value = currentCfg.tartilDuration,
            min = 0,
            max = 60,
            unit = "menit",
            onValueChange = { v -> updateCurrentStage { it.copy(tartilDuration = v) } }
        )
        HybridNumberStepper(
            label = "Durasi Shalawat Tarhim (Auto-Sync jika ada file MP3)",
            value = currentCfg.tarhimDuration,
            min = 0,
            max = 30,
            unit = "menit",
            onValueChange = { v -> updateCurrentStage { it.copy(tarhimDuration = v) } }
        )
        HybridToggleRow(
            label = "Putar Audio Adzan Otomatis Saat Waktu Tiba",
            checked = currentCfg.isAdzan,
            onCheckedChange = { v -> updateCurrentStage { it.copy(isAdzan = v) } }
        )
        HybridNumberStepper(
            label = "Durasi Hitung Mundur Menunggu Iqomah",
            value = currentCfg.iqomahDuration,
            min = 1,
            max = 30,
            unit = "menit",
            onValueChange = { v -> updateCurrentStage { it.copy(iqomahDuration = v) } }
        )
        HybridNumberStepper(
            label = "Durasi Mode Tenang Pelaksanaan Sholat",
            value = currentCfg.sholatDuration,
            min = 5,
            max = 45,
            unit = "menit",
            onValueChange = { v -> updateCurrentStage { it.copy(sholatDuration = v) } }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HybridTextInput(
                label = "Nama Imam Tetap (${selectedPrayer.defaultIndonesianName})",
                value = currentCfg.imam,
                onValueChange = { v -> updateCurrentStage { it.copy(imam = v) } },
                modifier = Modifier.weight(1f)
            )
            HybridTextInput(
                label = "Nama Muadzin (${selectedPrayer.defaultIndonesianName})",
                value = currentCfg.muadzin,
                onValueChange = { v -> updateCurrentStage { it.copy(muadzin = v) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun JumatSettingsTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    val jumat = settings.jumat
    val today = LocalDate.now()
    val nearestFridayStr = remember(today) { JwsStateMachine.snapToNearestFriday(today).toString() }

    var targetFridayDate by remember {
        mutableStateOf(jumat.petugas.selectedDate.ifBlank { nearestFridayStr })
    }
    val currentPetugas = jumat.petugas.records[targetFridayDate] ?: JumatPetugasRecord()
    val currentKeuangan = jumat.keuangan.records[targetFridayDate] ?: JumatKeuanganRecord(
        saldoKasLalu = jumat.keuangan.saldoKasLalu
    )

    var journalDesc by remember { mutableStateOf("") }
    var journalAmountStr by remember { mutableStateOf("") }
    var journalType by remember { mutableStateOf("in") }

    val rupiah = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    HybridSectionCard(
        title = "Pengaturan Mode Hari Jum'at, Petugas & Buku Kas Jurnal",
        subtitle = "Tanggal otomatis di-snap ke Hari Jum'at terdekat dan tampil aktif di TV mulai H-6"
    ) {
        HybridToggleRow(
            label = "Aktifkan Mode Khusus Hari Jum'at (Menggantikan Dzuhur)",
            checked = jumat.enabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(jumat = it.jumat.copy(enabled = v)) } }
        )
        HybridNumberStepper(
            label = "Durasi Pengumuman Sebelum Khutbah (Waktu Jum'at)",
            value = jumat.waktuJumatDuration,
            min = 5,
            max = 45,
            unit = "menit",
            onValueChange = { v -> onUpdateSettings { it.copy(jumat = it.jumat.copy(waktuJumatDuration = v)) } }
        )
        HybridNumberStepper(
            label = "Durasi Khutbah Jum'at",
            value = jumat.khutbahDuration,
            min = 10,
            max = 60,
            unit = "menit",
            onValueChange = { v -> onUpdateSettings { it.copy(jumat = it.jumat.copy(khutbahDuration = v)) } }
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HybridTextInput(
                label = "Tanggal Jum'at (YYYY-MM-DD)",
                value = targetFridayDate,
                onValueChange = { raw ->
                    val snapped = runCatching {
                        JwsStateMachine.snapToNearestFriday(LocalDate.parse(raw.trim())).toString()
                    }.getOrDefault(raw)
                    targetFridayDate = snapped
                },
                modifier = Modifier.weight(1f)
            )
            HybridButton(
                label = "Snap ke Jum'at Terdekat ($nearestFridayStr)",
                onClick = { targetFridayDate = nearestFridayStr }
            )
        }

        fun savePetugas(updated: JumatPetugasRecord) {
            onUpdateSettings { prev ->
                val newMap = prev.jumat.petugas.records.toMutableMap()
                newMap[targetFridayDate] = updated
                prev.copy(
                    jumat = prev.jumat.copy(
                        petugas = prev.jumat.petugas.copy(
                            selectedDate = targetFridayDate,
                            records = newMap
                        )
                    )
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HybridTextInput(
                label = "Nama Khotib Jum'at",
                value = currentPetugas.khotib,
                onValueChange = { v -> savePetugas(currentPetugas.copy(khotib = v)) },
                modifier = Modifier.weight(1f)
            )
            HybridTextInput(
                label = "Nama Imam Jum'at",
                value = currentPetugas.imam,
                onValueChange = { v -> savePetugas(currentPetugas.copy(imam = v)) },
                modifier = Modifier.weight(1f)
            )
            HybridTextInput(
                label = "Nama Muadzin / Bilal",
                value = currentPetugas.muadzin,
                onValueChange = { v -> savePetugas(currentPetugas.copy(muadzin = v)) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    HybridSectionCard(
        title = "Buku Kas & Jurnal Transaksi Jum'at ($targetFridayDate)",
        subtitle = "Saldo Akhir = ${rupiah.format(currentKeuangan.saldoAkhir)} (Pemasukan: ${rupiah.format(currentKeuangan.pemasukan)} • Pengeluaran: ${rupiah.format(currentKeuangan.pengeluaran)})"
    ) {
        HybridTextInput(
            label = "Saldo Kas Jum'at Pekan Lalu (Rp)",
            value = currentKeuangan.saldoKasLalu.toString(),
            onValueChange = { v ->
                val saldo = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                onUpdateSettings { prev ->
                    val newMap = prev.jumat.keuangan.records.toMutableMap()
                    newMap[targetFridayDate] = currentKeuangan.copy(saldoKasLalu = saldo)
                    prev.copy(
                        jumat = prev.jumat.copy(
                            keuangan = prev.jumat.keuangan.copy(
                                saldoKasLalu = saldo,
                                records = newMap
                            )
                        )
                    )
                }
            }
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HybridTextInput(
                label = "Uraian Transaksi Kas",
                value = journalDesc,
                placeholder = "Contoh: Infaq Kotak Amal Jum'at",
                onValueChange = { journalDesc = it },
                modifier = Modifier.weight(1.4f)
            )
            HybridTextInput(
                label = "Nominal (Rp)",
                value = journalAmountStr,
                placeholder = "500000",
                onValueChange = { journalAmountStr = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(0.9f)
            )
            HybridButton(
                label = if (journalType == "in") "Tipe: MASUK (+)" else "Tipe: KELUAR (-)",
                isPrimary = (journalType == "in"),
                isDanger = (journalType == "out"),
                onClick = { journalType = if (journalType == "in") "out" else "in" }
            )
            HybridButton(
                label = "+ Tambah Jurnal",
                isPrimary = true,
                onClick = {
                    val amt = journalAmountStr.toLongOrNull() ?: 0L
                    if (journalDesc.isNotBlank() && amt > 0L) {
                        val newEntry = JumatJournalEntry(
                            id = System.currentTimeMillis().toString(),
                            description = journalDesc.trim(),
                            amount = amt,
                            type = journalType
                        )
                        val updatedEntries = currentKeuangan.entries + newEntry
                        val totalIn = updatedEntries.filter { it.type == "in" }.sumOf { it.amount }
                        val totalOut = updatedEntries.filter { it.type == "out" }.sumOf { it.amount }

                        onUpdateSettings { prev ->
                            val newMap = prev.jumat.keuangan.records.toMutableMap()
                            newMap[targetFridayDate] = currentKeuangan.copy(
                                pemasukan = totalIn,
                                pengeluaran = totalOut,
                                entries = updatedEntries
                            )
                            prev.copy(
                                jumat = prev.jumat.copy(
                                    keuangan = prev.jumat.keuangan.copy(records = newMap)
                                )
                            )
                        }
                        journalDesc = ""
                        journalAmountStr = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun RamadhanSettingsTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit,
    onExportExcelTemplate: () -> Unit,
    onImportExcel: () -> Unit
) {
    val ramadhan = settings.ramadhan
    val nightIdx = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val currentPetugas = ramadhan.petugas.getOrNull(nightIdx)
    val currentTakjilList = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()
    var newDonorName by remember { mutableStateOf("") }

    HybridSectionCard(
        title = "Mode Ramadhan, Petugas Tarawih & Sirine Imsak/Buka",
        subtitle = "Mengatur jadwal kultum & sholat Tarawih 30 malam, daftar Ta'jil (Import Excel), dan Sirine"
    ) {
        HybridToggleRow(
            label = "Aktifkan Mode Bulan Suci Ramadhan (Tarawih Otomatis Ba'da Isya)",
            checked = ramadhan.isRamadhan,
            onCheckedChange = { v -> onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(isRamadhan = v)) } }
        )
        HybridNumberStepper(
            label = "Malam / Hari Ramadhan Aktif Saat Ini",
            value = ramadhan.currentNight,
            min = 1,
            max = 30,
            unit = "Ramadhan",
            onValueChange = { v -> onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(currentNight = v)) } }
        )
        HybridToggleRow(
            label = "Aktifkan Sirine Ramadhan (Berbuka Maghrib & Imsak)",
            checked = ramadhan.sirine.enabled,
            onCheckedChange = { v ->
                onUpdateSettings {
                    it.copy(ramadhan = it.ramadhan.copy(sirine = it.ramadhan.sirine.copy(enabled = v)))
                }
            }
        )

        if (currentPetugas != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput(
                    label = "Penceramah Malam Ke-${ramadhan.currentNight}",
                    value = currentPetugas.penceramah,
                    onValueChange = { v ->
                        onUpdateSettings { prev ->
                            val list = prev.ramadhan.petugas.toMutableList()
                            list[nightIdx] = currentPetugas.copy(penceramah = v)
                            prev.copy(ramadhan = prev.ramadhan.copy(petugas = list))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Imam Tarawih",
                    value = currentPetugas.imamTarawih,
                    onValueChange = { v ->
                        onUpdateSettings { prev ->
                            val list = prev.ramadhan.petugas.toMutableList()
                            list[nightIdx] = currentPetugas.copy(imamTarawih = v)
                            prev.copy(ramadhan = prev.ramadhan.copy(petugas = list))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    HybridSectionCard(
        title = "Jadwal Penyumbang Ta'jil 30 Hari & Import Excel (.xlsx)",
        subtitle = "Penyumbang Hari Ke-${ramadhan.currentNight}: ${currentTakjilList.joinToString(", ").ifBlank { "Belum ada" }}"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HybridButton(
                label = "Download Template Excel (.xlsx)",
                onClick = onExportExcelTemplate
            )
            HybridButton(
                label = "Import Jadwal Ta'jil dari Excel (.xlsx)",
                isPrimary = true,
                onClick = onImportExcel
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HybridTextInput(
                label = "Tambah Nama Donatur Ta'jil Hari Ke-${ramadhan.currentNight}",
                value = newDonorName,
                placeholder = "Contoh: Keluarga H. Ahmad",
                onValueChange = { newDonorName = it },
                modifier = Modifier.weight(1f)
            )
            HybridButton(
                label = "+ Tambah Donatur",
                isPrimary = true,
                onClick = {
                    if (newDonorName.isNotBlank()) {
                        val key = ramadhan.currentNight.toString()
                        onUpdateSettings { prev ->
                            val map = prev.ramadhan.takjil.toMutableMap()
                            map[key] = (map[key].orEmpty() + newDonorName.trim()).distinct()
                            prev.copy(ramadhan = prev.ramadhan.copy(takjil = map))
                        }
                        newDonorName = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun HolidayAndQurbanTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    var groupNameInput by remember { mutableStateOf("Kelompok Sapi Baru") }
    var membersCsvInput by remember { mutableStateOf("") }

    HybridSectionCard(
        title = "Mode Hari Raya (Idul Fitri, Idul Adha) & Info Qurban",
        subtitle = "Aktifkan tampilan khusus Hari Raya dan kelola daftar kelompok peserta Qurban"
    ) {
        HybridToggleRow(
            label = "Aktifkan Mode Hari Raya Idul Fitri",
            checked = settings.idulFitri.enabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(idulFitri = it.idulFitri.copy(enabled = v)) } }
        )
        HybridToggleRow(
            label = "Aktifkan Mode Hari Raya Idul Adha",
            checked = settings.idulAdha.enabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(idulAdha = it.idulAdha.copy(enabled = v)) } }
        )
        HybridToggleRow(
            label = "Aktifkan Mode Sosialisasi Info Qurban",
            checked = settings.infoQurban.enabled,
            onCheckedChange = { v -> onUpdateSettings { it.copy(infoQurban = it.infoQurban.copy(enabled = v)) } }
        )

        HybridTextInput(
            label = "Nama Kelompok Qurban Baru",
            value = groupNameInput,
            onValueChange = { groupNameInput = it }
        )
        HybridTextInput(
            label = "Daftar Nama Peserta / Mudhohi (Pisahkan dengan koma)",
            value = membersCsvInput,
            placeholder = "H. Ahmad, Bpk. Budi, Ibu Siti...",
            onValueChange = { membersCsvInput = it }
        )
        HybridButton(
            label = "+ Simpan Kelompok Qurban (${settings.idulAdha.qurbanGroups.size} Kelompok Tersimpan)",
            isPrimary = true,
            onClick = {
                val members = membersCsvInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (groupNameInput.isNotBlank() && members.isNotEmpty()) {
                    val newGroup = QurbanGroup(
                        id = System.currentTimeMillis().toString(),
                        animalType = if (members.size > 1) "Sapi" else "Kambing",
                        groupName = groupNameInput.trim(),
                        members = members
                    )
                    onUpdateSettings { prev ->
                        prev.copy(
                            idulAdha = prev.idulAdha.copy(
                                qurbanGroups = prev.idulAdha.qurbanGroups + newGroup
                            )
                        )
                    }
                    membersCsvInput = ""
                }
            }
        )
    }
}

@Composable
private fun OverlayAndNightModeTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    var newTickerText by remember { mutableStateOf("") }

    HybridSectionCard(
        title = "Running Text (Teks Berjalan) & Mode Malam (ScreenSaver)",
        subtitle = "Atur pesan pengumuman berjalan di bagian bawah layar dan jadwal hemat daya malam hari"
    ) {
        HybridNumberStepper(
            label = "Durasi Tampil Running Text",
            value = settings.overlayVisibleDuration,
            min = 10,
            max = 300,
            unit = "detik",
            onValueChange = { v -> onUpdateSettings { it.copy(overlayVisibleDuration = v) } }
        )
        HybridNumberStepper(
            label = "Durasi Sembunyi Running Text",
            value = settings.overlayHiddenDuration,
            min = 0,
            max = 120,
            unit = "detik",
            onValueChange = { v -> onUpdateSettings { it.copy(overlayHiddenDuration = v) } }
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HybridTextInput(
                label = "Tambah Pesan Running Text Baru",
                value = newTickerText,
                onValueChange = { newTickerText = it },
                modifier = Modifier.weight(1f)
            )
            HybridButton(
                label = "+ Tambah Teks",
                isPrimary = true,
                onClick = {
                    if (newTickerText.isNotBlank()) {
                        onUpdateSettings { prev ->
                            prev.copy(overlayTextList = prev.overlayTextList + newTickerText.trim())
                        }
                        newTickerText = ""
                    }
                }
            )
        }

        HybridToggleRow(
            label = "Aktifkan Mode Malam (ScreenSaver Hemat Daya 22:00 - 03:00)",
            checked = settings.screensaver.enabled,
            onCheckedChange = { v ->
                onUpdateSettings { it.copy(screensaver = it.screensaver.copy(enabled = v)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAndLayoutTabContent(
    settings: AppStateSettings,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    HybridSectionCard(
        title = "17 Tema Visual, 3 Posisi Layout & Skala Kanvas 1080p",
        subtitle = "Mendukung rotasi otomatis tema 24 jam dan rotasi layout setiap 4 jam (Anti Burn-in)"
    ) {
        HybridToggleRow(
            label = "Rotasi Tema Otomatis 24 Jam (Auto-Theme)",
            checked = settings.themeSettings.autoThemeEnabled,
            onCheckedChange = { v ->
                onUpdateSettings { it.copy(themeSettings = it.themeSettings.copy(autoThemeEnabled = v)) }
            }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeEngine.BuiltInThemes.forEach { palette ->
                HybridButton(
                    label = palette.name,
                    isPrimary = (settings.themeSettings.currentThemeId == palette.id),
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                themeSettings = it.themeSettings.copy(
                                    currentThemeId = palette.id,
                                    autoThemeEnabled = false
                                )
                            )
                        }
                    }
                )
            }
        }

        HybridToggleRow(
            label = "Rotasi Layout Otomatis Setiap 4 Jam (Anti Burn-in)",
            checked = settings.layoutSettings.autoRotateLayout,
            onCheckedChange = { v ->
                onUpdateSettings { it.copy(layoutSettings = it.layoutSettings.copy(autoRotateLayout = v)) }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayoutPosition.entries.forEach { pos ->
                HybridButton(
                    label = "Layout: ${pos.serialName.uppercase()}",
                    isPrimary = (settings.layoutSettings.position == pos),
                    onClick = {
                        onUpdateSettings {
                            it.copy(
                                layoutSettings = it.layoutSettings.copy(
                                    position = pos,
                                    autoRotateLayout = false
                                )
                            )
                        }
                    }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScaleMode.entries.forEach { mode ->
                HybridButton(
                    label = "Skala: ${mode.serialName.uppercase()}",
                    isPrimary = (settings.layoutSettings.scaleMode == mode),
                    onClick = {
                        onUpdateSettings {
                            it.copy(layoutSettings = it.layoutSettings.copy(scaleMode = mode))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HardwareAndMediaTabContent(
    settings: AppStateSettings,
    relayPinStates: Map<Int, Boolean>,
    onToggleManualRelay: (Int, Boolean) -> Unit,
    onDiscoverOnvifCameras: () -> Unit,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    val sw = settings.switchSettings
    HybridSectionCard(
        title = "Saklar Otomatis (Relay 8 Channel WiFi & USB OTG)",
        subtitle = "Kontrol lampu, AC, dan ampli Toa luar/dalam secara otomatis berdasarkan waktu sholat atau state"
    ) {
        HybridToggleRow(
            label = "Aktifkan Otomasi Saklar Relay (8 Channel)",
            checked = sw.enabled,
            onCheckedChange = { v ->
                onUpdateSettings { it.copy(switchSettings = it.switchSettings.copy(enabled = v)) }
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HybridButton(
                label = "Mode Koneksi: WiFi HTTP Relay",
                isPrimary = (sw.connectionType == "wifi"),
                onClick = {
                    onUpdateSettings { it.copy(switchSettings = it.switchSettings.copy(connectionType = "wifi")) }
                }
            )
            HybridButton(
                label = "Mode Koneksi: USB OTG Serial Relay",
                isPrimary = (sw.connectionType == "usb"),
                onClick = {
                    onUpdateSettings { it.copy(switchSettings = it.switchSettings.copy(connectionType = "usb")) }
                }
            )
        }
        HybridTextInput(
            label = "IP Address Modul WiFi Relay (Contoh: 192.168.1.100)",
            value = sw.wifiIp,
            onValueChange = { v ->
                onUpdateSettings { it.copy(switchSettings = it.switchSettings.copy(wifiIp = v)) }
            }
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sw.switches.forEach { channel ->
                val isPinOn = relayPinStates[channel.pin] == true
                HybridButton(
                    label = "Pin ${channel.pin} (${channel.name}): ${if (isPinOn) "ON" else "OFF"}",
                    isPrimary = isPinOn,
                    onClick = { onToggleManualRelay(channel.pin, !isPinOn) }
                )
            }
        }
    }

    HybridSectionCard(
        title = "Kamera CCTV (RTSP / ONVIF), YouTube Live & QR Infaq",
        subtitle = "Terdaftar ${settings.cctv.cameras.size} kamera CCTV • Tata letak ${settings.cctv.gridLayout} Grid"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HybridButton(
                label = "Pindai Kamera ONVIF Otomatis (LAN)",
                isPrimary = true,
                onClick = onDiscoverOnvifCameras
            )
            listOf(1, 2, 4, 8).forEach { gridCount ->
                HybridButton(
                    label = "Grid $gridCount Kamera",
                    isPrimary = (settings.cctv.gridLayout == gridCount),
                    onClick = {
                        onUpdateSettings { it.copy(cctv = it.cctv.copy(gridLayout = gridCount)) }
                    }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("makkah" to "Live Makkah", "madinah" to "Live Madinah", "custom" to "Live Custom").forEach { (src, label) ->
                HybridButton(
                    label = label,
                    isPrimary = settings.live.source.equals(src, ignoreCase = true),
                    onClick = {
                        onUpdateSettings { it.copy(live = it.live.copy(source = src)) }
                    }
                )
            }
        }

        HybridToggleRow(
            label = "Tampilkan Lencana QR Code Infaq Masjid di Layar Utama",
            checked = settings.masjidQr.enabled,
            onCheckedChange = { v ->
                onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(enabled = v)) }
            }
        )
        HybridTextInput(
            label = "Tautan / Payload QRIS Infaq Masjid",
            value = settings.masjidQr.link,
            onValueChange = { v ->
                onUpdateSettings { it.copy(masjidQr = it.masjidQr.copy(link = v)) }
            }
        )
        HybridTextInput(
            label = "URL Google Sheets Laporan Keuangan Online",
            value = settings.onlineFinance.sheetUrl,
            onValueChange = { v ->
                onUpdateSettings { it.copy(onlineFinance = it.onlineFinance.copy(sheetUrl = v, enabled = v.isNotBlank())) }
            }
        )
    }
}

@Composable
private fun BackupAndAuthTabContent(
    settings: AppStateSettings,
    remoteServerUrl: String,
    remoteQrPairingUrl: String,
    remoteAuthToken: String,
    onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit,
    onExportBackupZip: () -> Unit,
    onImportBackupZip: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var newUsername by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.ADMIN) }

    HybridSectionCard(
        title = "Web Remote Control PWA (Ktor Server Port 1421)",
        subtitle = "Pindai QR Code di bawah dengan kamera HP (jaringan WiFi yang sama) untuk mengontrol TV tanpa mengetik token"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (remoteQrPairingUrl.isNotBlank()) {
                val qrBmp = remember(remoteQrPairingUrl) {
                    generateQrBitmap(remoteQrPairingUrl, 280)
                }
                if (qrBmp != null) {
                    Image(
                        bitmap = qrBmp.asImageBitmap(),
                        contentDescription = "QR Remote Server",
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(6.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "URL Remote HP: $remoteServerUrl",
                    color = palette.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Token Akses / PIN Admin: ${remoteAuthToken.ifBlank { "732911" }}",
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Mendukung instalasi PWA (Add to Home Screen), pemantauan CPU/RAM/Uptime, dan kontrol penuh 27 state.",
                    color = palette.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }

    HybridSectionCard(
        title = "Backup & Restore Satu Klik (.zip) — Kompatibel dengan Backup Tauri Lama",
        subtitle = "Mencadangkan atau memulihkan seluruh pengaturan JSON + seluruh file media di folder jws-assets/"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HybridButton(
                label = "Export Full Backup (.zip)",
                isPrimary = true,
                onClick = onExportBackupZip
            )
            HybridButton(
                label = "Restore / Import Backup (.zip Baru atau Tauri Lama)",
                isPrimary = true,
                onClick = onImportBackupZip
            )
        }
    }

    HybridSectionCard(
        title = "Manajemen Akun Pengguna & PIN Multi-Role",
        subtitle = "Terdaftar ${settings.users.size} akun (${settings.users.joinToString { "${it.username} [${it.role}]" }})"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HybridTextInput(
                label = "Username Baru",
                value = newUsername,
                onValueChange = { newUsername = it },
                modifier = Modifier.weight(1f)
            )
            HybridTextInput(
                label = "PIN Baru (6 Digit)",
                value = newPin,
                onValueChange = { newPin = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UserRole.entries.forEach { r ->
                HybridButton(
                    label = r.displayLabel,
                    isPrimary = (newRole == r),
                    onClick = { newRole = r }
                )
            }
            HybridButton(
                label = "+ Simpan Akun",
                isPrimary = true,
                onClick = {
                    if (newUsername.isNotBlank() && newPin.length >= 4) {
                        val acc = UserAccount(
                            id = "user-${System.currentTimeMillis()}",
                            username = newUsername.trim(),
                            pinHash = AuthManager.sha256Hex(newPin),
                            role = newRole.id
                        )
                        onUpdateSettings { prev ->
                            prev.copy(
                                users = prev.users.filterNot {
                                    it.username.equals(acc.username, ignoreCase = true)
                                } + acc
                            )
                        }
                        newUsername = ""
                        newPin = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun TakjilExcelPreviewDialog(
    previewRows: List<TakjilImportPreviewRow>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val totalNames = previewRows.sumOf { it.names.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accentSecondary, RoundedCornerShape(22.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Preview Import Jadwal Ta'jil Excel (.xlsx)",
                color = palette.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Ditemukan ${previewRows.size} hari dengan total $totalNames nama donatur:",
                color = palette.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                previewRows.forEach { row ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.bgSecondary)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Hari ke-${row.day}: ${row.names.joinToString(", ")}",
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                HybridButton(label = "Batal", onClick = onCancel)
                Spacer(modifier = Modifier.width(12.dp))
                HybridButton(
                    label = "Terapkan ($totalNames Nama)",
                    isPrimary = true,
                    onClick = onConfirm
                )
            }
        }
    }
}
