package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.location.IndonesiaCityRepository
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerStageConfig
import id.algaffar.jwstv.domain.model.ScheduledAudioItem
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlin.math.roundToInt

/**
 * Category 2: SHOLAT (`prayer`, `audio`, `customAudio`).
 * Mirrors `PrayerSettingsForm.tsx`, `AudioSettingsForm.tsx`, and `CustomAudioSettingsForm.tsx`.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerAndStagesSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val p = settings.prayerSettings
    var selectedPrayer by remember { mutableStateOf(PrayerId.FAJR) }
    val currentCfg = settings.prayers.forPrayer(selectedPrayer)

    val methods = listOf(
        "Singapore" to "Kemenag RI / MABIMS",
        "MuslimWorldLeague" to "MWL",
        "Egyptian" to "Egyptian",
        "Karachi" to "Karachi",
        "UmmAlQura" to "Umm al-Qura",
        "Dubai" to "Dubai",
        "MoonsightingCommittee" to "Moonsighting",
        "NorthAmerica" to "ISNA",
        "Kuwait" to "Kuwait",
        "Qatar" to "Qatar",
        "Tehran" to "Tehran",
        "Turkey" to "Turkey"
    )
    val timezones = listOf(
        "Asia/Jakarta" to "WIB (Jakarta)",
        "Asia/Makassar" to "WITA (Makassar/Kaltim)",
        "Asia/Jayapura" to "WIT (Jayapura)",
        "Asia/Singapore" to "Singapore",
        "Asia/Kuala_Lumpur" to "Malaysia"
    )

    fun updateStage(pid: PrayerId, transform: (PrayerStageConfig) -> PrayerStageConfig) {
        actions.onUpdateSettings { prev ->
            val updated = transform(prev.prayers.forPrayer(pid))
            val next = when (pid) {
                PrayerId.FAJR -> prev.prayers.copy(fajr = updated)
                PrayerId.DHUHR -> prev.prayers.copy(dhuhr = updated)
                PrayerId.ASR -> prev.prayers.copy(asr = updated)
                PrayerId.MAGHRIB -> prev.prayers.copy(maghrib = updated)
                PrayerId.ISHA -> prev.prayers.copy(isha = updated)
                else -> prev.prayers
            }
            prev.copy(prayers = next)
        }
    }

    val context = LocalContext.current
    val allCities = remember(context) { IndonesiaCityRepository.loadCities(context) }
    val provinces = remember(allCities) { IndonesiaCityRepository.getProvinces(allCities) }
    var selectedProvince by remember(p.provinceName) {
        mutableStateOf(p.provinceName.ifBlank { provinces.firstOrNull() ?: "DKI Jakarta" })
    }
    val citiesInProvince = remember(allCities, selectedProvince) {
        IndonesiaCityRepository.getCitiesInProvince(allCities, selectedProvince)
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 0. Pemilih Cepat 38 Provinsi & Kota/Kabupaten Indonesia (Offline)
        val provinceOptions = remember(provinces) {
            provinces.map { HybridSelectOption(id = it, label = it) }
        }
        val cityOptions = remember(citiesInProvince) {
            citiesInProvince.map { city ->
                HybridSelectOption(
                    id = city.city,
                    label = city.city,
                    subtitle = "${city.timezone} • Lat ${city.latitude}, Lng ${city.longitude}"
                )
            }
        }
        val activeCityId = if (p.provinceName == selectedProvince) p.cityName else ""

        HybridSectionCard(
            title = "Pemilih Cepat Kota / Kabupaten Indonesia (Offline)",
            subtitle = if (p.cityName.isNotBlank()) {
                "Kota aktif: ${p.cityName}, ${p.provinceName} (${p.timezone})"
            } else {
                "Pilih provinsi & kota untuk mengisi otomatis Latitude, Longitude, dan Zona Waktu tanpa mengetik manual"
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HybridSelectDropdown(
                    label = "Provinsi",
                    selectedId = selectedProvince,
                    options = provinceOptions,
                    onSelect = { prov -> selectedProvince = prov },
                    placeholder = "Pilih Provinsi...",
                    dialogTitle = "Pilih Provinsi (38 Provinsi & ASEAN)",
                    modifier = Modifier.weight(1f)
                )
                HybridSelectDropdown(
                    label = "Kabupaten / Kota",
                    selectedId = activeCityId,
                    options = cityOptions,
                    onSelect = { cityName ->
                        val city = citiesInProvince.firstOrNull { it.city == cityName } ?: return@HybridSelectDropdown
                        actions.onUpdateSettings { prev ->
                            prev.copy(
                                prayerSettings = prev.prayerSettings.copy(
                                    provinceName = city.province,
                                    cityName = city.city,
                                    latitude = city.latitude,
                                    longitude = city.longitude,
                                    timezone = city.timezone
                                )
                            )
                        }
                    },
                    placeholder = "Pilih Kabupaten / Kota...",
                    dialogTitle = "Pilih Kabupaten / Kota ($selectedProvince)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 1. Lokasi, Metode, Mazhab, Zona Waktu
        HybridSectionCard(
            title = "Lokasi, Metode Hisab, Mazhab & Koreksi Global",
            subtitle = "Koordinat geografis masjid menentukan presisi jadwal sholat harian"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridCoordinateStepper(
                    label = "Latitude (Lintang)",
                    value = p.latitude,
                    min = -90.0,
                    max = 90.0,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(latitude = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridCoordinateStepper(
                    label = "Longitude (Bujur)",
                    value = p.longitude,
                    min = -180.0,
                    max = 180.0,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(longitude = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("ZONA WAKTU", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            HybridSegmentedRow(
                options = timezones,
                selectedId = p.timezone,
                onSelect = { tz -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(timezone = tz)) } }
            )

            Text("MAZHAB (PERHITUNGAN ASHAR)", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            HybridSegmentedRow(
                options = listOf("Shafi" to "Syafi'i / Maliki / Hambali", "Hanafi" to "Hanafi"),
                selectedId = p.madhab,
                onSelect = { m -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(madhab = m)) } }
            )

            Text("METODE KALKULASI (${p.calculationMethod})", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            HybridSegmentedRow(
                options = methods,
                selectedId = p.calculationMethod,
                onSelect = { m -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(calculationMethod = m)) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Pengaman Global (Ihtiyati)",
                    value = p.ihtiyati,
                    min = -5,
                    max = 5,
                    unit = "menit",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(ihtiyati = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Penyesuaian Tanggal Hijriah",
                    value = p.hijriAdjustment,
                    min = -3,
                    max = 3,
                    unit = "hari",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(hijriAdjustment = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Koreksi Per-Waktu Sholat (8 waktu)
        HybridSectionCard(
            title = "Koreksi Manual Per Waktu Sholat (-15 s/d +15 Menit)",
            subtitle = "Fine-tuning masing-masing jadwal bila ada selisih dengan jadwal lokal Kemenag"
        ) {
            val c = p.corrections
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Imsak", c.imsak, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(imsak = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
                HybridNumberStepper("Subuh", c.fajr, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(fajr = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Syuruq", c.sunrise, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(sunrise = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
                HybridNumberStepper("Dhuha", c.dhuha, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(dhuha = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Dzuhur", c.dhuhr, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(dhuhr = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
                HybridNumberStepper("Ashar", c.asr, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(asr = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Maghrib", c.maghrib, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(maghrib = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
                HybridNumberStepper("Isya", c.isha, { v -> actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(corrections = c.copy(isha = v))) } }, -15, 15, unit = "mnt", modifier = Modifier.weight(1f))
            }
        }

        // 3. Pengaturan Tahapan 5 Waktu Sholat
        HybridSectionCard(
            title = "Pengaturan Tahapan Sholat 5 Waktu (Tartil, Tarhim, Adzan, Iqomah, Sholat)",
            subtitle = "Kelola toggle, durasi, petugas Imam/Muadzin, serta pesan khusus per waktu sholat",
            headerAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = "Aktifkan Semua",
                        isPrimary = true,
                        onClick = {
                            PrayerId.FIVE_FARDHU.forEach { pid ->
                                updateStage(pid) { it.copy(isTartil = true, isTarhim = true, isAdzan = true) }
                            }
                        }
                    )
                    HybridButton(
                        label = "Nonaktifkan Semua",
                        isDanger = true,
                        onClick = {
                            PrayerId.FIVE_FARDHU.forEach { pid ->
                                updateStage(pid) { it.copy(isTartil = false, isTarhim = false, isAdzan = false) }
                            }
                        }
                    )
                }
            }
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrayerId.FIVE_FARDHU.forEach { pid ->
                    HybridButton(
                        label = pid.defaultIndonesianName.uppercase(),
                        isPrimary = (pid == selectedPrayer),
                        onClick = { selectedPrayer = pid }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridToggleRow(
                    label = "Aktifkan Tartil",
                    checked = currentCfg.isTartil,
                    onCheckedChange = { v -> updateStage(selectedPrayer) { it.copy(isTartil = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Aktifkan Tarhim",
                    checked = currentCfg.isTarhim,
                    onCheckedChange = { v -> updateStage(selectedPrayer) { it.copy(isTarhim = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Putar Audio Adzan",
                    checked = currentCfg.isAdzan,
                    onCheckedChange = { v -> updateStage(selectedPrayer) { it.copy(isAdzan = v) } },
                    modifier = Modifier.weight(1f)
                )
            }

            HybridInfoBanner(
                text = "Aturan jws-android-algaffar: Durasi Tartil dihitung mundur dari waktu Adzan (tidak ditambah durasi Tarhim). Durasi Tarhim (default 5 mnt) & Adzan otomatis mengikuti panjang file MP3/Video jika aktif.",
                accentColor = Color(0xFF10B981)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Durasi Tartil (Sebelum Adzan)",
                    value = currentCfg.tartilDuration,
                    min = 5,
                    max = 60,
                    enabled = currentCfg.isTartil,
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(tartilDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Durasi Menunggu Iqomah",
                    value = currentCfg.iqomahDuration,
                    min = 3,
                    max = 15,
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(iqomahDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Durasi Pelaksanaan Sholat",
                    value = currentCfg.sholatDuration,
                    min = 5,
                    max = 30,
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(sholatDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Durasi Adzan (Manual)",
                    value = currentCfg.adzanDuration,
                    min = 1,
                    max = 15,
                    enabled = !currentCfg.isAdzan,
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(adzanDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Nama Imam (${selectedPrayer.defaultIndonesianName})",
                    value = currentCfg.imam,
                    placeholder = "Ust. Fulan",
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(imam = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Nama Muadzin (${selectedPrayer.defaultIndonesianName})",
                    value = currentCfg.muadzin,
                    placeholder = "Sdr. Fulan",
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(muadzin = v) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Tagline Layar Adzan",
                    value = currentCfg.adzanTagline,
                    placeholder = "Panggilan Suci",
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(adzanTagline = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Pesan / Kutipan Adzan",
                    value = currentCfg.adzanMessage,
                    placeholder = "Fajar Menyingsing, Saatnya Bersujud",
                    onValueChange = { v -> updateStage(selectedPrayer) { it.copy(adzanMessage = v) } },
                    modifier = Modifier.weight(1f)
                )
            }
            HybridTextInput(
                label = "Pesan Layar Pelaksanaan Sholat",
                value = currentCfg.sholatMessage,
                placeholder = "Rapatkan Shaf dan Matikan Gadget",
                onValueChange = { v -> updateStage(selectedPrayer) { it.copy(sholatMessage = v) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioSholatSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    var selectedSlot by remember { mutableStateOf("fajr") }
    val prayerSlots = listOf(
        "fajr" to "Subuh",
        "dhuhr" to "Dzuhur",
        "asr" to "Ashar",
        "maghrib" to "Maghrib",
        "isha" to "Isya",
        "jumat" to "Jum'at"
    )
    val categories = listOf(
        "tartil" to "Tartil Al-Qur'an",
        "tarhim" to "Shalawat Tarhim",
        "adzan" to "Kumandang Adzan"
    )
    val volumePct = (settings.globalVolume * 100f).roundToInt().coerceIn(0, 100)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Volume Global & Engine Audio Media3",
            subtitle = "Mengatur tingkat volume pemutaran Tartil, Tarhim, Adzan, Sirine, dan Audio Kustom"
        ) {
            HybridNumberStepper(
                label = "Volume Global Pemutaran Audio",
                value = volumePct,
                min = 0,
                max = 100,
                step = 5,
                unit = "%",
                onValueChange = { pct ->
                    actions.onUpdateSettings { it.copy(globalVolume = pct / 100f) }
                }
            )
            HybridInfoBanner(
                text = "Aplikasi menggunakan ExoPlayer Media3 Native Android. File audio disimpan di jws-assets/audio/{tartil|tarhim|adzan}/ dan durasi MP3 otomatis tersinkron dengan hitung mundur Tarhim & Adzan.",
                accentColor = Color(0xFF38BDF8)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton(
                    label = "Unduh Paket Audio Standar (1-Klik)",
                    icon = Icons.Default.Download,
                    isPrimary = true,
                    onClick = { actions.onDownloadAudioPack("standard_pack") }
                )
                HybridButton(
                    label = "Unduh Paket Adzan Makkah & Madinah",
                    icon = Icons.Default.Download,
                    onClick = { actions.onDownloadAudioPack("haramain_pack") }
                )
            }
        }

        HybridSectionCard(
            title = "Manajemen File Audio Per Waktu Sholat",
            subtitle = "Pilih waktu sholat lalu unggah, pratinjau, atau hapus file audio MP3/WAV/M4A"
        ) {
            HybridSegmentedRow(
                options = prayerSlots,
                selectedId = selectedSlot,
                onSelect = { selectedSlot = it }
            )

            // Re-read inventory off the Main thread whenever slot or audioInventoryTick changes
            val tick = actions.audioInventoryTick
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                categories.forEach { (catId, catLabel) ->
                    val slotInfo by androidx.compose.runtime.produceState(
                        initialValue = false to "",
                        key1 = catId,
                        key2 = selectedSlot,
                        key3 = tick
                    ) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            actions.onFindPrayerAudioSlot(catId, selectedSlot)
                        }
                    }
                    val (exists, duration) = slotInfo
                    val isPlaying = actions.previewingAudioKey == "$catId:$selectedSlot"

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.85f))
                            .border(
                                1.5.dp,
                                if (exists) Color(0xFF10B981).copy(alpha = 0.55f) else palette.border,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(catLabel, color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (exists) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (exists) "READY ($duration)" else "KOSONG",
                                    color = if (exists) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        HybridButton(
                            label = if (exists) "Ganti Audio (Memori / USB)" else "Pilih Audio (Memori / USB)",
                            icon = Icons.Default.Upload,
                            isPrimary = !exists,
                            onClick = { actions.onPickPrayerAudio(catId, selectedSlot) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HybridButton(
                                label = if (isPlaying) "STOP" else "PLAY",
                                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                isPrimary = isPlaying,
                                enabled = exists,
                                onClick = { actions.onTogglePreviewPrayerAudio(catId, selectedSlot) },
                                modifier = Modifier.weight(1f)
                            )
                            HybridButton(
                                label = "Hapus",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                enabled = exists,
                                onClick = { actions.onDeletePrayerAudio(catId, selectedSlot) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAudioSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    var subTab by remember { mutableStateOf("library") }
    val customAudio = settings.customAudio
    val files by androidx.compose.runtime.produceState(
        initialValue = emptyList<Pair<String, String>>(),
        key1 = actions.audioInventoryTick
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            actions.onListCustomAudioFiles()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Audio Kustom Terpusat (Library, Siaran Langsung & Jadwal Rutin)",
            subtitle = "Putar murattal, pengumuman, atau shalawat kustom secara instan atau terjadwal (${files.size} file tersimpan)"
        ) {
            HybridSegmentedRow(
                options = listOf(
                    "library" to "1. PERPUSTAKAAN (${files.size})",
                    "live" to "2. SIARAN LANGSUNG (ON-AIR)",
                    "schedule" to "3. JADWAL OTOMATIS (${customAudio.schedules.size})"
                ),
                selectedId = subTab,
                onSelect = { subTab = it }
            )
        }

        when (subTab) {
            "library" -> {
                HybridSectionCard(
                    title = "Perpustakaan Audio Kustom",
                    subtitle = "Impor file audio MP3/WAV/M4A dari Memori Internal TV atau Flashdisk USB",
                    headerAction = {
                        HybridButton(
                            label = "+ Impor Audio (Memori / USB)",
                            icon = Icons.Default.Upload,
                            isPrimary = true,
                            onClick = actions.onPickCustomAudio
                        )
                    }
                ) {
                    if (files.isEmpty()) {
                        HybridInfoBanner(
                            text = "Belum ada file audio kustom di jws-assets/audio/custom/. Klik tombol '+ Impor Audio' di atas untuk menambahkan file.",
                            accentColor = Color(0xFFF59E0B)
                        )
                    } else {
                        files.forEach { (name, dur) ->
                            val isPlaying = actions.previewingAudioKey == "custom:$name"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.bgPrimary.copy(alpha = 0.8f))
                                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = palette.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Durasi: $dur", color = palette.textSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HybridButton(
                                        label = if (isPlaying) "Stop" else "Pratinjau",
                                        icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        isPrimary = isPlaying,
                                        onClick = { actions.onTogglePreviewCustomAudio(name) }
                                    )
                                    HybridButton(
                                        label = "Pilih u/ Live",
                                        onClick = {
                                            actions.onUpdateSettings { prev ->
                                                prev.copy(customAudio = prev.customAudio.copy(liveAudioFile = "audio/custom/$name"))
                                            }
                                            subTab = "live"
                                        }
                                    )
                                    HybridButton(
                                        label = "Hapus",
                                        icon = Icons.Default.Delete,
                                        isDanger = true,
                                        onClick = { actions.onDeleteCustomAudio(name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "live" -> {
                HybridSectionCard(
                    title = "Siaran Audio Langsung (On-Demand)",
                    subtitle = if (customAudio.isPlayingLive) "SEDANG ON-AIR: ${customAudio.liveAudioFile}" else "Putar satu file audio seketika ke speaker masjid"
                ) {
                    HybridTextInput(
                        label = "File Audio Aktif",
                        value = customAudio.liveAudioFile,
                        placeholder = "audio/custom/nama_file.mp3 (pilih dari tab Perpustakaan)",
                        onValueChange = { v ->
                            actions.onUpdateSettings { it.copy(customAudio = it.customAudio.copy(liveAudioFile = v)) }
                        }
                    )
                    HybridNumberStepper(
                        label = "Durasi Maksimal Siaran",
                        value = customAudio.liveDurationMinutes,
                        min = 1,
                        max = 120,
                        unit = "menit",
                        onValueChange = { v ->
                            actions.onUpdateSettings { it.copy(customAudio = it.customAudio.copy(liveDurationMinutes = v)) }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HybridButton(
                            label = if (customAudio.isPlayingLive) "HENTIKAN SIARAN LANGSUNG" else "MULAI SIARAN LANGSUNG",
                            icon = if (customAudio.isPlayingLive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            isPrimary = !customAudio.isPlayingLive,
                            isDanger = customAudio.isPlayingLive,
                            enabled = customAudio.liveAudioFile.isNotBlank() || customAudio.isPlayingLive,
                            onClick = {
                                actions.onUpdateSettings { prev ->
                                    prev.copy(customAudio = prev.customAudio.copy(isPlayingLive = !prev.customAudio.isPlayingLive))
                                }
                            }
                        )
                    }
                }
            }

            "schedule" -> {
                val dayNames = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
                HybridSectionCard(
                    title = "Penjadwalan Audio Kustom Otomatis",
                    subtitle = "Jadwalkan pemutaran rutin mingguan (misal pengajian pagi Jum'at) atau satu kali",
                    headerAction = {
                        HybridButton(
                            label = "+ Tambah Jadwal",
                            icon = Icons.Default.Add,
                            isPrimary = true,
                            onClick = {
                                val firstFile = files.firstOrNull()?.first?.let { "audio/custom/$it" }.orEmpty()
                                val item = ScheduledAudioItem(
                                    id = "sched-${System.currentTimeMillis()}",
                                    name = "Jadwal Audio #${customAudio.schedules.size + 1}",
                                    audioFile = firstFile,
                                    scheduleType = "weekly",
                                    dayOfWeek = 5,
                                    time = "11:00",
                                    durationMinutes = 15,
                                    enabled = true
                                )
                                actions.onUpdateSettings { prev ->
                                    prev.copy(customAudio = prev.customAudio.copy(schedules = prev.customAudio.schedules + item))
                                }
                            }
                        )
                    }
                ) {
                    if (customAudio.schedules.isEmpty()) {
                        HybridInfoBanner(
                            text = "Belum ada jadwal audio otomatis. Klik '+ Tambah Jadwal' untuk membuat jadwal baru.",
                            accentColor = Color(0xFFF59E0B)
                        )
                    } else {
                        customAudio.schedules.forEachIndexed { idx, sched ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.bgPrimary.copy(alpha = 0.8f))
                                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${sched.name.ifBlank { "Jadwal #${idx + 1}" }} • ${sched.time} (${dayNames.getOrElse(sched.dayOfWeek) { "Jum" }})",
                                        color = palette.accentSecondary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        HybridButton(
                                            label = if (sched.enabled) "AKTIF" else "NONAKTIF",
                                            isPrimary = sched.enabled,
                                            onClick = {
                                                actions.onUpdateSettings { prev ->
                                                    val list = prev.customAudio.schedules.toMutableList()
                                                    list[idx] = sched.copy(enabled = !sched.enabled)
                                                    prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                                                }
                                            }
                                        )
                                        HybridButton(
                                            label = "Hapus",
                                            icon = Icons.Default.Delete,
                                            isDanger = true,
                                            onClick = {
                                                actions.onUpdateSettings { prev ->
                                                    prev.copy(customAudio = prev.customAudio.copy(schedules = prev.customAudio.schedules.filterIndexed { i, _ -> i != idx }))
                                                }
                                            }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HybridTextInput(
                                        label = "Jam (HH:mm)",
                                        value = sched.time,
                                        onValueChange = { v ->
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.customAudio.schedules.toMutableList()
                                                list[idx] = sched.copy(time = v)
                                                prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                                            }
                                        },
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    HybridTextInput(
                                        label = "File Audio",
                                        value = sched.audioFile,
                                        onValueChange = { v ->
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.customAudio.schedules.toMutableList()
                                                list[idx] = sched.copy(audioFile = v)
                                                prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                                            }
                                        },
                                        modifier = Modifier.weight(1.4f)
                                    )
                                }
                                HybridSegmentedRow(
                                    options = dayNames.mapIndexed { dIdx, dName -> dIdx.toString() to dName },
                                    selectedId = sched.dayOfWeek.toString(),
                                    onSelect = { dStr ->
                                        dStr.toIntOrNull()?.let { d ->
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.customAudio.schedules.toMutableList()
                                                list[idx] = sched.copy(dayOfWeek = d)
                                                prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
