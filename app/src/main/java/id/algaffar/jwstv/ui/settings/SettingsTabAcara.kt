package id.algaffar.jwstv.ui.settings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import id.algaffar.jwstv.data.auth.UserRole
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.JumatJournalEntry
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.QurbanGroup
import id.algaffar.jwstv.domain.model.RamadhanJadwalItem
import id.algaffar.jwstv.domain.model.ScreenSaverSchedule
import id.algaffar.jwstv.domain.model.TimeSlot
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Category 4: ACARA (`jumat`, `ramadhan`, `idulFitri`, `idulAdha`, `state`, `live`, `switches`).
 */

@Composable
fun JumatSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val jumat = settings.jumat
    val today = LocalDate.now()
    val nearestFridayStr = remember(today) { JwsStateMachine.snapToNearestFriday(today).toString() }
    var targetFridayDate by remember { mutableStateOf(jumat.petugas.selectedDate.ifBlank { nearestFridayStr }) }

    val currentPetugas = jumat.petugas.records[targetFridayDate] ?: JumatPetugasRecord()
    val currentKeuangan = jumat.keuangan.records[targetFridayDate] ?: JumatKeuanganRecord(saldoKasLalu = jumat.keuangan.saldoKasLalu)

    var journalDesc by remember { mutableStateOf("") }
    var journalAmountStr by remember { mutableStateOf("") }
    var journalType by remember { mutableStateOf("in") }

    val rupiah = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    fun savePetugas(updated: JumatPetugasRecord) {
        actions.onUpdateSettings { prev ->
            val newMap = prev.jumat.petugas.records.toMutableMap()
            newMap[targetFridayDate] = updated
            prev.copy(
                jumat = prev.jumat.copy(
                    petugas = prev.jumat.petugas.copy(selectedDate = targetFridayDate, records = newMap)
                )
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Pengaturan Mode Hari Jum'at & Petugas",
            subtitle = "Tanggal otomatis di-snap ke Hari Jum'at terdekat dan aktif tampil di TV mulai H-6"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Khusus Hari Jum'at (Menggantikan Dzuhur)",
                checked = jumat.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(enabled = v)) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Waktu Jum'at",
                    value = jumat.waktuJumatDuration,
                    min = 5,
                    max = 45,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(waktuJumatDuration = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Khutbah Jum'at",
                    value = jumat.khutbahDuration,
                    min = 10,
                    max = 60,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(khutbahDuration = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Sholat Jum'at",
                    value = jumat.sholatJumatDuration,
                    min = 10,
                    max = 45,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(sholatJumatDuration = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Pesan Layar Khutbah",
                    value = jumat.khutbahMessage,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(khutbahMessage = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Pesan Layar Sholat Jum'at",
                    value = jumat.sholatMessage,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(sholatMessage = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Tanggal Jum'at (YYYY-MM-DD)",
                    value = targetFridayDate,
                    onValueChange = { raw ->
                        targetFridayDate = runCatching {
                            JwsStateMachine.snapToNearestFriday(LocalDate.parse(raw.trim())).toString()
                        }.getOrDefault(raw)
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "Snap ke Jum'at Terdekat ($nearestFridayStr)",
                    onClick = { targetFridayDate = nearestFridayStr }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Nama Khotib", currentPetugas.khotib, { savePetugas(currentPetugas.copy(khotib = it)) }, modifier = Modifier.weight(1f))
                HybridTextInput("Nama Imam", currentPetugas.imam, { savePetugas(currentPetugas.copy(imam = it)) }, modifier = Modifier.weight(1f))
                HybridTextInput("Nama Muadzin / Bilal", currentPetugas.muadzin, { savePetugas(currentPetugas.copy(muadzin = it)) }, modifier = Modifier.weight(1f))
            }
        }

        HybridSectionCard(
            title = "Buku Kas & Jurnal Transaksi Jum'at ($targetFridayDate)",
            subtitle = "Saldo Akhir = ${rupiah.format(currentKeuangan.saldoAkhir)} (Masuk: ${rupiah.format(currentKeuangan.pemasukan)} • Keluar: ${rupiah.format(currentKeuangan.pengeluaran)})"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Judul Laporan Kas Jum'at",
                    value = jumat.keuangan.customTitle,
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(keuangan = it.jumat.keuangan.copy(customTitle = v))) }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Saldo Kas Pekan Lalu (Rp)",
                    value = currentKeuangan.saldoKasLalu.toString(),
                    onValueChange = { v ->
                        val saldo = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                        actions.onUpdateSettings { prev ->
                            val newMap = prev.jumat.keuangan.records.toMutableMap()
                            newMap[targetFridayDate] = currentKeuangan.copy(saldoKasLalu = saldo)
                            prev.copy(jumat = prev.jumat.copy(keuangan = prev.jumat.keuangan.copy(saldoKasLalu = saldo, records = newMap)))
                        }
                    },
                    modifier = Modifier.weight(0.8f)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SUMBER DATA KAS JUM'AT", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HybridSegmentedRow(
                    options = listOf("manual" to "Manual Lokal", "online" to "Google Sheets Online"),
                    selectedId = jumat.keuangan.source,
                    onSelect = { src ->
                        actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(keuangan = it.jumat.keuangan.copy(source = src))) }
                    }
                )
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Uraian Transaksi", journalDesc, { journalDesc = it }, "Infaq Kotak Amal Jum'at", modifier = Modifier.weight(1.3f))
                HybridTextInput("Nominal (Rp)", journalAmountStr, { journalAmountStr = it.filter { c -> c.isDigit() } }, "500000", modifier = Modifier.weight(0.8f))
                HybridButton(
                    label = if (journalType == "in") "MASUK (+)" else "KELUAR (-)",
                    isPrimary = (journalType == "in"),
                    isDanger = (journalType == "out"),
                    onClick = { journalType = if (journalType == "in") "out" else "in" }
                )
                HybridButton(
                    label = "+ Tambah",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = {
                        val amt = journalAmountStr.toLongOrNull() ?: 0L
                        if (journalDesc.isNotBlank() && amt > 0L) {
                            val newEntry = JumatJournalEntry(System.currentTimeMillis().toString(), journalDesc.trim(), amt, journalType)
                            val updatedEntries = currentKeuangan.entries + newEntry
                            val totalIn = updatedEntries.filter { it.type == "in" }.sumOf { it.amount }
                            val totalOut = updatedEntries.filter { it.type == "out" }.sumOf { it.amount }
                            actions.onUpdateSettings { prev ->
                                val newMap = prev.jumat.keuangan.records.toMutableMap()
                                newMap[targetFridayDate] = currentKeuangan.copy(pemasukan = totalIn, pengeluaran = totalOut, entries = updatedEntries)
                                prev.copy(jumat = prev.jumat.copy(keuangan = prev.jumat.keuangan.copy(records = newMap)))
                            }
                            journalDesc = ""
                            journalAmountStr = ""
                        }
                    }
                )
            }

            if (currentKeuangan.entries.isNotEmpty()) {
                currentKeuangan.entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.8f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.description, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            text = (if (entry.type == "in") "+ " else "- ") + rupiah.format(entry.amount),
                            color = if (entry.type == "in") Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        HybridButton(
                            label = "Hapus",
                            icon = Icons.Default.Delete,
                            isDanger = true,
                            onClick = {
                                val remaining = currentKeuangan.entries.filterNot { it.id == entry.id }
                                val totalIn = remaining.filter { it.type == "in" }.sumOf { it.amount }
                                val totalOut = remaining.filter { it.type == "out" }.sumOf { it.amount }
                                actions.onUpdateSettings { prev ->
                                    val newMap = prev.jumat.keuangan.records.toMutableMap()
                                    newMap[targetFridayDate] = currentKeuangan.copy(pemasukan = totalIn, pengeluaran = totalOut, entries = remaining)
                                    prev.copy(jumat = prev.jumat.copy(keuangan = prev.jumat.keuangan.copy(records = newMap)))
                                }
                            },
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RamadhanSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val ramadhan = settings.ramadhan
    val nightIdx = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val currentPetugas = ramadhan.petugas.getOrNull(nightIdx)
    val currentKeuangan = ramadhan.keuangan.getOrNull(nightIdx)
    val currentTakjilList = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()
    var newDonorName by remember { mutableStateOf("") }
    var newJadwalLabel by remember { mutableStateOf("") }
    var newJadwalSource by remember { mutableStateOf("custom") }
    var newJadwalPrayerId by remember { mutableStateOf("imsak") }
    var newJadwalCustomTime by remember { mutableStateOf("03:00") }
    var newJadwalHighlighted by remember { mutableStateOf(false) }

    val prayerSourceOptions = remember {
        listOf(
            HybridSelectOption("imsak", "Otomatis: Imsak"),
            HybridSelectOption("fajr", "Otomatis: Subuh"),
            HybridSelectOption("sunrise", "Otomatis: Syuruq"),
            HybridSelectOption("dhuha", "Otomatis: Dhuha"),
            HybridSelectOption("dhuhr", "Otomatis: Dzuhur"),
            HybridSelectOption("asr", "Otomatis: Ashar"),
            HybridSelectOption("maghrib", "Otomatis: Maghrib"),
            HybridSelectOption("isha", "Otomatis: Isya")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Ramadhan, Durasi Tarawih & Petugas Malam Ke-${ramadhan.currentNight}",
            subtitle = "Otomatis menampilkan sesi Tarawih, Ceramah & Sholat Tarawih setelah Sholat Isya"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Bulan Suci Ramadhan",
                checked = ramadhan.isRamadhan,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(isRamadhan = v)) } }
            )
            HybridToggleRow(
                label = "Sinkronkan Otomatis Malam Ramadhan dari Kalender Hijriah",
                checked = ramadhan.autoSyncNightFromHijri,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(autoSyncNightFromHijri = v)) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Malam / Hari Ramadhan Aktif",
                    value = ramadhan.currentNight,
                    min = 1,
                    max = 30,
                    unit = "Ramadhan",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(currentNight = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Tahun Hijriah",
                    value = ramadhan.hijriYear,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(hijriYear = v)) } },
                    modifier = Modifier.weight(0.6f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Slide Tarawih", ramadhan.tarawihDuration, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(tarawihDuration = v)) } }, 1, 60, modifier = Modifier.weight(1f))
                HybridNumberStepper("Ceramah Tarawih", ramadhan.ceramahDuration, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(ceramahDuration = v)) } }, 0, 60, modifier = Modifier.weight(1f))
                HybridNumberStepper("Sholat Tarawih", ramadhan.sholatTarawihDuration, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sholatTarawihDuration = v)) } }, 10, 90, modifier = Modifier.weight(1f))
            }

            if (currentPetugas != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HybridTextInput(
                        label = "Penceramah Malam #${ramadhan.currentNight}",
                        value = currentPetugas.penceramah,
                        onValueChange = { v ->
                            actions.onUpdateSettings { prev ->
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
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.petugas.toMutableList()
                                list[nightIdx] = currentPetugas.copy(imamTarawih = v)
                                prev.copy(ramadhan = prev.ramadhan.copy(petugas = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridTextInput(
                        label = "Imam Witir",
                        value = currentPetugas.imamWitir,
                        onValueChange = { v ->
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.petugas.toMutableList()
                                list[nightIdx] = currentPetugas.copy(imamWitir = v)
                                prev.copy(ramadhan = prev.ramadhan.copy(petugas = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (currentKeuangan != null) {
            HybridSectionCard(
                title = "Laporan Kas Ramadhan Malam Ke-${ramadhan.currentNight}",
                subtitle = "Saldo Akhir = Rp ${String.format(Locale("id", "ID"), "%,d", currentKeuangan.saldoAkhir)}"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HybridTextInput(
                        label = "Saldo Awal (Rp)",
                        value = currentKeuangan.saldoAwal.toString(),
                        onValueChange = { v ->
                            val num = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.keuangan.toMutableList()
                                list[nightIdx] = currentKeuangan.copy(saldoAwal = num)
                                prev.copy(ramadhan = prev.ramadhan.copy(keuangan = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridTextInput(
                        label = "Infaq Pria (Rp)",
                        value = currentKeuangan.penerimaanPria.toString(),
                        onValueChange = { v ->
                            val num = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.keuangan.toMutableList()
                                list[nightIdx] = currentKeuangan.copy(penerimaanPria = num)
                                prev.copy(ramadhan = prev.ramadhan.copy(keuangan = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridTextInput(
                        label = "Infaq Wanita (Rp)",
                        value = currentKeuangan.penerimaanWanita.toString(),
                        onValueChange = { v ->
                            val num = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.keuangan.toMutableList()
                                list[nightIdx] = currentKeuangan.copy(penerimaanWanita = num)
                                prev.copy(ramadhan = prev.ramadhan.copy(keuangan = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridTextInput(
                        label = "Pengeluaran (Rp)",
                        value = currentKeuangan.pengeluaran.toString(),
                        onValueChange = { v ->
                            val num = v.filter { it.isDigit() }.toLongOrNull() ?: 0L
                            actions.onUpdateSettings { prev ->
                                val list = prev.ramadhan.keuangan.toMutableList()
                                list[nightIdx] = currentKeuangan.copy(pengeluaran = num)
                                prev.copy(ramadhan = prev.ramadhan.copy(keuangan = list))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HybridSectionCard(
            title = "Sirine Ramadhan (Berbuka Maghrib, Sahur & Imsak)",
            subtitle = "Memutar file audio sirine atau sirine sintetis otomatis tepat pada waktunya"
        ) {
            val sir = ramadhan.sirine
            HybridToggleRow(
                label = "Aktifkan Sirine Ramadhan",
                checked = sir.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(enabled = v))) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridToggleRow("Saat Maghrib (Berbuka)", sir.playAtMaghrib, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(playAtMaghrib = v))) } }, modifier = Modifier.weight(1f))
                HybridToggleRow("Saat Imsak", sir.playAtImsak, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(playAtImsak = v))) } }, modifier = Modifier.weight(1f))
                HybridToggleRow("Saat Sahur", sir.playAtSahur, { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(playAtSahur = v))) } }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridTextInput(
                    label = "Jam Sirine Sahur (HH:mm)",
                    value = sir.sahurTime,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(sahurTime = v))) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Jumlah Pengulangan Sirine",
                    value = sir.repeatCount,
                    min = 1,
                    max = 5,
                    unit = "kali",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = sir.copy(repeatCount = v))) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HybridSectionCard(
            title = "Jadwal Penyumbang Ta'jil 30 Hari & Import Excel (.xlsx)",
            subtitle = "Hari ke-${ramadhan.currentNight}: ${currentTakjilList.joinToString(", ").ifBlank { "Belum ada donatur" }}"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridButton("Download Template Excel (.xlsx)", onClick = actions.onExportTakjilTemplate)
                HybridButton("Import Jadwal Ta'jil dari Excel (.xlsx)", isPrimary = true, onClick = actions.onImportTakjilExcel)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Tambah Donatur Ta'jil Hari Ke-${ramadhan.currentNight}", newDonorName, { newDonorName = it }, "Keluarga H. Ahmad", modifier = Modifier.weight(1f))
                HybridButton(
                    label = "+ Tambah Donatur",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = {
                        if (newDonorName.isNotBlank()) {
                            val key = ramadhan.currentNight.toString()
                            actions.onUpdateSettings { prev ->
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

        // ==================== JADWAL IMSAKIYAH & RAMADHAN DINAMIS ====================
        val activeJadwalCount = ramadhan.jadwalImsakiyah.count { it.enabled }
        HybridSectionCard(
            title = "Jadwal Imsakiyah & Ramadhan Dinamis ($activeJadwalCount Aktif)",
            subtitle = "Atur item jadwal yang tampil pada Slide Jadwal Ramadhan & layar Tarawih (Otomatis dari Hisab Sholat atau Jam Manual)",
            headerAction = {
                HybridButton(
                    label = "Reset Default",
                    onClick = {
                        actions.onUpdateSettings { prev ->
                            prev.copy(
                                ramadhan = prev.ramadhan.copy(
                                    jadwalTitle = "JADWAL IMSAKIYAH & BERBUKA RAMADHAN",
                                    jadwalImsakiyah = RamadhanJadwalItem.defaultImsakiyahItems()
                                )
                            )
                        }
                    }
                )
            }
        ) {
            HybridTextInput(
                label = "Judul Slide Jadwal Imsakiyah",
                value = ramadhan.jadwalTitle,
                placeholder = "JADWAL IMSAKIYAH & BERBUKA RAMADHAN",
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(jadwalTitle = v)) }
                }
            )

            // Toggle tampilan slide yang ikut berputar di layar Tarawih / Mode Ramadhan
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridToggleRow(
                    label = "Tampil Petugas",
                    checked = ramadhan.slideshow.showPetugas,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(slideshow = it.ramadhan.slideshow.copy(showPetugas = v))) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Tampil Keuangan",
                    checked = ramadhan.slideshow.showKeuangan,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(slideshow = it.ramadhan.slideshow.copy(showKeuangan = v))) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridToggleRow(
                    label = "Tampil Jadwal Imsakiyah",
                    checked = ramadhan.slideshow.showJadwal,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(slideshow = it.ramadhan.slideshow.copy(showJadwal = v))) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Tampil Daftar Ta'jil",
                    checked = ramadhan.slideshow.showTakjil,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(slideshow = it.ramadhan.slideshow.copy(showTakjil = v))) } },
                    modifier = Modifier.weight(1f)
                )
            }

            // Daftar Item Jadwal Dinamis (Edit Label, Sumber Otomatis/Manual, Sorotan, Urutan, Hapus)
            ramadhan.jadwalImsakiyah.forEachIndexed { idx, item ->
                fun updateItem(transform: (RamadhanJadwalItem) -> RamadhanJadwalItem) {
                    actions.onUpdateSettings { prev ->
                        val list = prev.ramadhan.jadwalImsakiyah.toMutableList()
                        if (idx in list.indices) {
                            list[idx] = transform(list[idx])
                        }
                        prev.copy(ramadhan = prev.ramadhan.copy(jadwalImsakiyah = list))
                    }
                }
                fun moveItem(delta: Int) {
                    actions.onUpdateSettings { prev ->
                        val list = prev.ramadhan.jadwalImsakiyah.toMutableList()
                        val target = idx + delta
                        if (idx in list.indices && target in list.indices) {
                            val moved = list.removeAt(idx)
                            list.add(target, moved)
                        }
                        prev.copy(ramadhan = prev.ramadhan.copy(jadwalImsakiyah = list))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = if (item.enabled) 0.85f else 0.45f))
                        .border(
                            1.dp,
                            if (item.enabled && item.highlighted) palette.accentSecondary.copy(alpha = 0.65f) else palette.border,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        HybridTextInput(
                            label = "Label #${idx + 1}",
                            value = item.label,
                            placeholder = "IMSAK / SAHUR / BUKA PUASA",
                            onValueChange = { v -> updateItem { it.copy(label = v) } },
                            modifier = Modifier.weight(1.2f)
                        )
                        if (item.sourceType.equals("custom", ignoreCase = true)) {
                            HybridTextInput(
                                label = "Jam Manual (HH:mm)",
                                value = item.customTime,
                                placeholder = "03:00",
                                onValueChange = { v -> updateItem { it.copy(customTime = v) } },
                                modifier = Modifier.weight(0.9f)
                            )
                        } else {
                            HybridSelectDropdown(
                                label = "Sumber Hisab",
                                selectedId = item.prayerId,
                                options = prayerSourceOptions,
                                onSelect = { pid -> updateItem { it.copy(prayerId = pid) } },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HybridButton(
                            label = if (item.enabled) "TAMPIL" else "NONAKTIF",
                            isPrimary = item.enabled,
                            onClick = { updateItem { it.copy(enabled = !it.enabled) } }
                        )
                        HybridButton(
                            label = if (item.sourceType.equals("custom", ignoreCase = true)) "Mode: Manual" else "Mode: Hisab",
                            onClick = {
                                updateItem {
                                    val nextSource = if (it.sourceType.equals("custom", ignoreCase = true)) "prayer" else "custom"
                                    it.copy(
                                        sourceType = nextSource,
                                        customTime = if (nextSource == "custom" && it.customTime.isBlank()) "03:00" else it.customTime
                                    )
                                }
                            }
                        )
                        HybridButton(
                            label = if (item.highlighted) "★ Disorot" else "Sorot",
                            isPrimary = item.highlighted,
                            onClick = { updateItem { it.copy(highlighted = !it.highlighted) } }
                        )
                        HybridButton(
                            label = "⬆",
                            enabled = idx > 0,
                            onClick = { moveItem(-1) }
                        )
                        HybridButton(
                            label = "⬇",
                            enabled = idx < ramadhan.jadwalImsakiyah.lastIndex,
                            onClick = { moveItem(1) }
                        )
                        HybridButton(
                            label = "Hapus",
                            icon = Icons.Default.Delete,
                            isDanger = true,
                            onClick = {
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        ramadhan = prev.ramadhan.copy(
                                            jadwalImsakiyah = prev.ramadhan.jadwalImsakiyah.filterIndexed { i, _ -> i != idx }
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Form Tambah Item Jadwal Baru (Hisab Otomatis atau Manual HH:mm)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgPrimary.copy(alpha = 0.65f))
                    .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "+ TAMBAH ITEM JADWAL IMSAKIYAH / RAMADHAN BARU",
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HybridTextInput(
                        label = "Nama Jadwal",
                        value = newJadwalLabel,
                        placeholder = "Mis: SAHUR / KAJIAN / QIYAMUL LAIL",
                        onValueChange = { newJadwalLabel = it },
                        modifier = Modifier.weight(1.2f)
                    )
                    if (newJadwalSource == "custom") {
                        HybridTextInput(
                            label = "Jam (HH:mm)",
                            value = newJadwalCustomTime,
                            placeholder = "03:00",
                            onValueChange = { newJadwalCustomTime = it },
                            modifier = Modifier.weight(0.8f)
                        )
                    } else {
                        HybridSelectDropdown(
                            label = "Sumber Hisab",
                            selectedId = newJadwalPrayerId,
                            options = prayerSourceOptions,
                            onSelect = { newJadwalPrayerId = it },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HybridButton(
                        label = if (newJadwalSource == "custom") "Sumber: Jam Manual" else "Sumber: Otomatis Hisab",
                        onClick = { newJadwalSource = if (newJadwalSource == "custom") "prayer" else "custom" }
                    )
                    HybridButton(
                        label = if (newJadwalHighlighted) "★ Sorot Aktif" else "Sorot Kartu",
                        isPrimary = newJadwalHighlighted,
                        onClick = { newJadwalHighlighted = !newJadwalHighlighted }
                    )
                    HybridButton(
                        label = "+ Tambah Jadwal",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = {
                            if (newJadwalLabel.isNotBlank()) {
                                val added = RamadhanJadwalItem(
                                    id = "im_${System.currentTimeMillis()}",
                                    label = newJadwalLabel.trim(),
                                    sourceType = newJadwalSource,
                                    prayerId = newJadwalPrayerId,
                                    customTime = if (newJadwalSource == "custom") newJadwalCustomTime.trim() else "",
                                    highlighted = newJadwalHighlighted,
                                    enabled = true
                                )
                                actions.onUpdateSettings { prev ->
                                    prev.copy(ramadhan = prev.ramadhan.copy(jadwalImsakiyah = prev.ramadhan.jadwalImsakiyah + added))
                                }
                                newJadwalLabel = ""
                                newJadwalHighlighted = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IdulFitriSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val fitri = settings.idulFitri
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Hari Raya Idul Fitri",
            subtitle = "Jadwal tayang bergantian Fullscreen/Standby, lalu otomatis masuk Sholat & Khutbah Idul Fitri"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Hari Raya Idul Fitri",
                checked = fitri.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(enabled = v)) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper("Durasi Sholat Ied", fitri.sholatDuration, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(sholatDuration = v)) } }, 10, 90, modifier = Modifier.weight(1f))
                HybridNumberStepper("Durasi Khutbah Ied", fitri.khutbahDuration, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(khutbahDuration = v)) } }, 10, 60, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Khotib Idul Fitri", fitri.petugas.khotib, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(petugas = fitri.petugas.copy(khotib = v))) } }, modifier = Modifier.weight(1f))
                HybridTextInput("Imam Idul Fitri", fitri.petugas.imam, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(petugas = fitri.petugas.copy(imam = v))) } }, modifier = Modifier.weight(1f))
                HybridTextInput("Bilal / Muadzin", fitri.petugas.bilal, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(petugas = fitri.petugas.copy(bilal = v))) } }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Pesan Sholat Ied", fitri.sholatMessage, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(sholatMessage = v)) } }, modifier = Modifier.weight(1f))
                HybridTextInput("Pesan Khutbah Ied", fitri.khutbahMessage, { v -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(khutbahMessage = v)) } }, modifier = Modifier.weight(1f))
            }
        }

        TimeSlotListCard(
            title = "Slot Jam Tayang Idul Fitri (${fitri.timeSlots.size} Slot)",
            slots = fitri.timeSlots,
            onUpdateSlots = { updated -> actions.onUpdateSettings { it.copy(idulFitri = fitri.copy(timeSlots = updated)) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdulAdhaQurbanSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val adha = settings.idulAdha
    val groups = adha.qurbanGroups

    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id) }
    val activeGroupIndex = groups.indexOfFirst { it.id == selectedGroupId }
        .let { if (it >= 0) it else if (groups.isNotEmpty()) 0 else -1 }
    val activeGroup = groups.getOrNull(activeGroupIndex)

    fun addNewGroupTab(animalType: String = QurbanGroup.ANIMAL_SAPI) {
        val newId = "qgrp_${System.currentTimeMillis()}_${groups.size + 1}"
        actions.onUpdateSettings { prev ->
            val (updatedGroups, _) = QurbanGroup.addGroup(
                groups = prev.idulAdha.qurbanGroups,
                animalType = animalType,
                id = newId
            )
            prev.copy(idulAdha = prev.idulAdha.copy(qurbanGroups = updatedGroups))
        }
        selectedGroupId = newId
    }

    fun deleteGroupTab(groupId: String) {
        val delIdx = groups.indexOfFirst { it.id == groupId }
        val remaining = groups.filterNot { it.id == groupId }
        selectedGroupId = if (remaining.isEmpty()) {
            null
        } else {
            remaining[delIdx.coerceIn(0, remaining.lastIndex)].id
        }
        actions.onUpdateSettings { prev ->
            prev.copy(
                idulAdha = prev.idulAdha.copy(
                    qurbanGroups = QurbanGroup.deleteGroup(prev.idulAdha.qurbanGroups, groupId)
                )
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Hari Raya Idul Adha & Sosialisasi Info Qurban",
            subtitle = "Kelola jadwal Hari Raya Idul Adha, petugas, serta daftar kelompok peserta Qurban"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridToggleRow(
                    label = "Aktifkan Mode Idul Adha",
                    checked = adha.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(enabled = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Aktifkan Mode Info Qurban",
                    checked = settings.infoQurban.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { prev -> prev.copy(infoQurban = prev.infoQurban.copy(enabled = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput(
                    label = "Khotib Idul Adha",
                    value = adha.petugas.khotib,
                    onValueChange = { v -> actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(petugas = prev.idulAdha.petugas.copy(khotib = v))) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Imam Idul Adha",
                    value = adha.petugas.imam,
                    onValueChange = { v -> actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(petugas = prev.idulAdha.petugas.copy(imam = v))) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Bilal / Muadzin",
                    value = adha.petugas.bilal,
                    onValueChange = { v -> actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(petugas = prev.idulAdha.petugas.copy(bilal = v))) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HybridSectionCard(
            title = "Kelompok Peserta Qurban (${groups.size} Kelompok)",
            subtitle = "Kelola setiap kelompok melalui tab di bawah — maksimal ${QurbanGroup.MAX_MEMBERS} anggota (mudhohi) dalam 1 kelompok",
            headerAction = {
                HybridButton(
                    label = "+ Tambah Kelompok",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = { addNewGroupTab() }
                )
            }
        ) {
            if (groups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.65f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Belum ada Kelompok Qurban",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Tambahkan tab kelompok baru untuk mengisi daftar peserta Qurban (maksimal ${QurbanGroup.MAX_MEMBERS} anggota per kelompok).",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                    HybridButton(
                        label = "+ Tambah Kelompok Pertama",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = { addNewGroupTab() }
                    )
                }
            } else {
                // ==================== TAB BAR KELOMPOK QURBAN ====================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.6f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TAB KELOMPOK QURBAN (${groups.size} TAB • PILIH TAB UNTUK CRUD ANGGOTA)",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groups.forEachIndexed { idx, grp ->
                            QurbanGroupTabPill(
                                group = grp,
                                index = idx,
                                isSelected = activeGroup?.id == grp.id,
                                onSelect = { selectedGroupId = grp.id }
                            )
                        }
                        HybridButton(
                            label = "+ Tab Kelompok",
                            icon = Icons.Default.Add,
                            isPrimary = false,
                            onClick = { addNewGroupTab() }
                        )
                    }
                }

                // ==================== PANEL CRUD TAB AKTIF ====================
                if (activeGroup != null) {
                    key(activeGroup.id) {
                        QurbanGroupTabEditor(
                            group = activeGroup,
                            groupIndex = activeGroupIndex,
                            totalGroups = groups.size,
                            onUpdateGroup = { newName, newAnimal ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.updateGroup(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                groupName = newName,
                                                animalType = newAnimal
                                            )
                                        )
                                    )
                                }
                            },
                            onAddMember = { memberName ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.addMember(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                memberName = memberName
                                            )
                                        )
                                    )
                                }
                            },
                            onUpdateMember = { memberIdx, memberName ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.updateMember(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                memberIndex = memberIdx,
                                                memberName = memberName
                                            )
                                        )
                                    )
                                }
                            },
                            onDeleteMember = { memberIdx ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.deleteMember(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                memberIndex = memberIdx
                                            )
                                        )
                                    )
                                }
                            },
                            onMoveMember = { memberIdx, delta ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.moveMember(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                memberIndex = memberIdx,
                                                delta = delta
                                            )
                                        )
                                    )
                                }
                            },
                            onMoveGroup = { delta ->
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        idulAdha = prev.idulAdha.copy(
                                            qurbanGroups = QurbanGroup.moveGroup(
                                                groups = prev.idulAdha.qurbanGroups,
                                                groupId = activeGroup.id,
                                                delta = delta
                                            )
                                        )
                                    )
                                }
                            },
                            onDeleteGroup = { deleteGroupTab(activeGroup.id) },
                            onAddNewGroupTab = { addNewGroupTab(activeGroup.animalType) }
                        )
                    }
                }
            }
        }

        TimeSlotListCard(
            title = "Slot Jam Tayang Idul Adha (${adha.timeSlots.size} Slot)",
            slots = adha.timeSlots,
            onUpdateSlots = { updated -> actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(timeSlots = updated)) } }
        )
    }
}

@Composable
private fun QurbanGroupTabPill(
    group: QurbanGroup,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val memberCount = group.members.size
    val isFull = memberCount >= QurbanGroup.MAX_MEMBERS
    val isOverflow = memberCount > QurbanGroup.MAX_MEMBERS

    val bgColor = when {
        isSelected && isFocused -> palette.accentSecondary
        isSelected -> palette.accent
        isFocused -> palette.accent.copy(alpha = 0.28f)
        else -> palette.bgSecondary.copy(alpha = 0.9f)
    }
    val titleColor = if (isSelected) palette.bgPrimary else palette.textPrimary
    val badgeBg = when {
        isSelected -> palette.bgPrimary.copy(alpha = 0.22f)
        isOverflow -> Color(0xFFF59E0B).copy(alpha = 0.22f)
        isFull -> Color(0xFF10B981).copy(alpha = 0.22f)
        else -> palette.bgPrimary.copy(alpha = 0.85f)
    }
    val badgeTextColor = when {
        isSelected -> palette.bgPrimary
        isOverflow -> Color(0xFFFBBF24)
        isFull -> Color(0xFF34D399)
        else -> palette.accentSecondary
    }
    val borderColor = when {
        isFocused -> TvFocusAmber
        isSelected -> palette.accentSecondary
        else -> palette.border
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.groupName.ifBlank { "Kelompok ${index + 1}" },
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBg)
                .padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${group.animalType} • $memberCount/${QurbanGroup.MAX_MEMBERS}",
                color = badgeTextColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
            if (memberCount == QurbanGroup.MAX_MEMBERS) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = badgeTextColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun QurbanGroupTabEditor(
    group: QurbanGroup,
    groupIndex: Int,
    totalGroups: Int,
    onUpdateGroup: (groupName: String?, animalType: String?) -> Unit,
    onAddMember: (String) -> Unit,
    onUpdateMember: (index: Int, name: String) -> Unit,
    onDeleteMember: (index: Int) -> Unit,
    onMoveMember: (index: Int, delta: Int) -> Unit,
    onMoveGroup: (delta: Int) -> Unit,
    onDeleteGroup: () -> Unit,
    onAddNewGroupTab: () -> Unit
) {
    val palette = LocalJwsTheme.current

    var nextSlotUid by remember(group.id) { mutableLongStateOf(1L) }
    val slotUids = remember(group.id) {
        mutableStateListOf<Long>().apply {
            repeat(group.members.size) { add(nextSlotUid++) }
        }
    }
    val deletedUids = remember(group.id) { mutableSetOf<Long>() }
    var isDeletingGroup by remember(group.id) { mutableStateOf(false) }
    var confirmDeleteGroup by remember(group.id) { mutableStateOf(false) }

    var newMemberInput by remember(group.id) { mutableStateOf("") }
    var addResetCounter by remember(group.id) { mutableIntStateOf(0) }

    // Reconcile local stable row keys if members were added/removed externally (e.g., Remote PWA)
    while (slotUids.size < group.members.size) {
        slotUids.add(nextSlotUid++)
    }
    while (slotUids.size > group.members.size) {
        slotUids.removeAt(slotUids.lastIndex)
    }

    val memberCount = group.members.size
    val isFull = memberCount >= QurbanGroup.MAX_MEMBERS
    val isOverflow = memberCount > QurbanGroup.MAX_MEMBERS
    val remainingSlots = (QurbanGroup.MAX_MEMBERS - memberCount).coerceAtLeast(0)

    fun commitAddMember() {
        val clean = newMemberInput.trim()
        if (clean.isBlank() || group.members.size >= QurbanGroup.MAX_MEMBERS) return
        slotUids.add(nextSlotUid++)
        onAddMember(clean)
        newMemberInput = ""
        addResetCounter++
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.bgPrimary.copy(alpha = 0.85f))
            .border(1.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==================== HEADER TAB AKTIF & AKSI KELOMPOK ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accent.copy(alpha = 0.2f))
                        .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "TAB #${groupIndex + 1} / $totalGroups",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                val statusColor = when {
                    isOverflow -> Color(0xFFF59E0B)
                    isFull -> Color(0xFF10B981)
                    else -> palette.accentSecondary
                }
                val statusLabel = when {
                    isOverflow -> "$memberCount/${QurbanGroup.MAX_MEMBERS} Anggota (Melebihi Batas)"
                    isFull -> "$memberCount/${QurbanGroup.MAX_MEMBERS} Anggota • PENUH"
                    else -> "$memberCount/${QurbanGroup.MAX_MEMBERS} Anggota • Sisa $remainingSlots slot"
                }
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HybridButton(
                    label = "⬅",
                    enabled = groupIndex > 0,
                    onClick = { onMoveGroup(-1) }
                )
                HybridButton(
                    label = "➡",
                    enabled = groupIndex < totalGroups - 1,
                    onClick = { onMoveGroup(1) }
                )
                HybridButton(
                    label = "Hapus Kelompok",
                    icon = Icons.Default.Delete,
                    isDanger = true,
                    onClick = {
                        if (group.activeMembers.isEmpty()) {
                            isDeletingGroup = true
                            onDeleteGroup()
                        } else {
                            confirmDeleteGroup = true
                        }
                    }
                )
            }
        }

        if (confirmDeleteGroup) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF991B1B).copy(alpha = 0.28f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yakin hapus '${group.groupName.ifBlank { "Kelompok ${groupIndex + 1}" }}' beserta ${group.activeMembers.size} anggota?",
                    color = Color(0xFFFECACA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = "Batal",
                        onClick = { confirmDeleteGroup = false }
                    )
                    HybridButton(
                        label = "Ya, Hapus",
                        icon = Icons.Default.Delete,
                        isDanger = true,
                        onClick = {
                            isDeletingGroup = true
                            confirmDeleteGroup = false
                            onDeleteGroup()
                        }
                    )
                }
            }
        }

        // ==================== IDENTITAS KELOMPOK (NAMA & JENIS HEWAN) ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            HybridTextInput(
                label = "Nama Kelompok",
                value = group.groupName,
                placeholder = "Contoh: Kelompok 1 / Sapi RT 01",
                onValueChange = { updatedName ->
                    if (!isDeletingGroup) onUpdateGroup(updatedName, null)
                },
                modifier = Modifier.weight(1.35f)
            )
            Column(
                modifier = Modifier.weight(0.85f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "JENIS HEWAN",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = QurbanGroup.ANIMAL_SAPI,
                        isPrimary = group.animalType.equals(QurbanGroup.ANIMAL_SAPI, ignoreCase = true),
                        onClick = { onUpdateGroup(null, QurbanGroup.ANIMAL_SAPI) },
                        modifier = Modifier.weight(1f)
                    )
                    HybridButton(
                        label = QurbanGroup.ANIMAL_KAMBING,
                        isPrimary = group.animalType.equals(QurbanGroup.ANIMAL_KAMBING, ignoreCase = true),
                        onClick = { onUpdateGroup(null, QurbanGroup.ANIMAL_KAMBING) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==================== DAFTAR ANGGOTA (MAKSIMAL 7 ANGGOTA) ====================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAFTAR ANGGOTA / MUDHOHI ($memberCount/${QurbanGroup.MAX_MEMBERS})",
                    color = palette.accentSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Edit langsung pada nama, urutkan (⬆/⬇), atau hapus",
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }

            if (isOverflow) {
                HybridInfoBanner(
                    text = "Kelompok ini memiliki $memberCount anggota (melebihi batas ${QurbanGroup.MAX_MEMBERS}). Hanya ${QurbanGroup.MAX_MEMBERS} anggota pertama yang tampil di layar TV. Hapus kelebihan anggota atau pindahkan ke tab kelompok baru.",
                    accentColor = Color(0xFFF59E0B)
                )
            }

            if (group.members.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bgSecondary.copy(alpha = 0.55f))
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Belum ada anggota pada kelompok ini. Ketik nama peserta pada kolom 'Tambah Anggota' di bawah (maksimal ${QurbanGroup.MAX_MEMBERS} anggota).",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                slotUids.forEachIndexed { idx, uid ->
                    val memberValue = group.members.getOrElse(idx) { "" }
                    val isExcessRow = idx >= QurbanGroup.MAX_MEMBERS
                    key(uid) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isExcessRow) Color(0xFFF59E0B).copy(alpha = 0.1f)
                                    else palette.bgSecondary.copy(alpha = 0.65f)
                                )
                                .border(
                                    1.dp,
                                    if (isExcessRow) Color(0xFFF59E0B).copy(alpha = 0.55f) else palette.border,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isExcessRow) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                        else palette.accent.copy(alpha = 0.18f)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    color = if (isExcessRow) Color(0xFFFBBF24) else palette.accent,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            HybridTextInput(
                                label = "",
                                value = memberValue,
                                placeholder = "Nama anggota #${idx + 1}",
                                onValueChange = { updatedText ->
                                    if (isDeletingGroup || uid in deletedUids) return@HybridTextInput
                                    val liveIdx = slotUids.indexOf(uid)
                                    if (liveIdx >= 0) {
                                        onUpdateMember(liveIdx, updatedText)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            HybridButton(
                                label = "⬆",
                                enabled = idx > 0,
                                onClick = {
                                    val liveIdx = slotUids.indexOf(uid)
                                    val targetIdx = liveIdx - 1
                                    if (liveIdx > 0 && targetIdx in slotUids.indices) {
                                        val moved = slotUids.removeAt(liveIdx)
                                        slotUids.add(targetIdx, moved)
                                        onMoveMember(liveIdx, -1)
                                    }
                                }
                            )
                            HybridButton(
                                label = "⬇",
                                enabled = idx < group.members.lastIndex,
                                onClick = {
                                    val liveIdx = slotUids.indexOf(uid)
                                    val targetIdx = liveIdx + 1
                                    if (liveIdx >= 0 && targetIdx in slotUids.indices) {
                                        val moved = slotUids.removeAt(liveIdx)
                                        slotUids.add(targetIdx, moved)
                                        onMoveMember(liveIdx, 1)
                                    }
                                }
                            )
                            HybridButton(
                                label = "Hapus",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                onClick = {
                                    val liveIdx = slotUids.indexOf(uid)
                                    if (liveIdx >= 0) {
                                        deletedUids.add(uid)
                                        slotUids.removeAt(liveIdx)
                                        onDeleteMember(liveIdx)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ==================== FORM TAMBAH ANGGOTA / INFO KUOTA PENUH ====================
            if (!isFull) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bgSecondary.copy(alpha = 0.5f))
                        .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        key(addResetCounter) {
                            HybridTextInput(
                                label = "Tambah Anggota #${memberCount + 1} (Sisa $remainingSlots dari ${QurbanGroup.MAX_MEMBERS} slot)",
                                value = newMemberInput,
                                placeholder = "Contoh: H. Ahmad beserta keluarga",
                                onValueChange = { newMemberInput = it },
                                debounceMs = 0L,
                                onSubmit = { commitAddMember() }
                            )
                        }
                    }
                    HybridButton(
                        label = "+ Tambah Anggota",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        enabled = newMemberInput.isNotBlank(),
                        onClick = { commitAddMember() }
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HybridInfoBanner(
                        text = "Kuota maksimal ${QurbanGroup.MAX_MEMBERS} anggota dalam 1 kelompok sudah terpenuhi ($memberCount/${QurbanGroup.MAX_MEMBERS}). Tambah tab kelompok baru untuk memasukkan peserta berikutnya.",
                        accentColor = Color(0xFF10B981)
                    )
                    HybridButton(
                        label = "+ Tambah Tab Kelompok Baru",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = onAddNewGroupTab
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlotListCard(
    title: String,
    slots: List<TimeSlot>,
    onUpdateSlots: (List<TimeSlot>) -> Unit
) {
    val palette = LocalJwsTheme.current
    HybridSectionCard(
        title = title,
        subtitle = "Setelah jam selesai terlewati, TV otomatis masuk tahap Sholat Ied -> Khutbah Ied",
        headerAction = {
            HybridButton(
                label = "+ Tambah Slot",
                icon = Icons.Default.Add,
                isPrimary = true,
                onClick = {
                    onUpdateSlots(slots + TimeSlot(System.currentTimeMillis().toString(), "06:00", "08:00", true))
                }
            )
        }
    ) {
        slots.forEachIndexed { idx, slot ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bgPrimary.copy(alpha = 0.8f))
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HybridTextInput("Jam Mulai (HH:mm)", slot.startTime, { v ->
                    val list = slots.toMutableList(); list[idx] = slot.copy(startTime = v); onUpdateSlots(list)
                }, modifier = Modifier.weight(1f))
                HybridTextInput("Jam Selesai (HH:mm)", slot.endTime, { v ->
                    val list = slots.toMutableList(); list[idx] = slot.copy(endTime = v); onUpdateSlots(list)
                }, modifier = Modifier.weight(1f))
                HybridButton(if (slot.enabled) "AKTIF" else "OFF", isPrimary = slot.enabled, onClick = {
                    val list = slots.toMutableList(); list[idx] = slot.copy(enabled = !slot.enabled); onUpdateSlots(list)
                })
                HybridButton("Hapus", icon = Icons.Default.Delete, isDanger = true, onClick = {
                    onUpdateSlots(slots.filterIndexed { i, _ -> i != idx })
                })
            }
        }
    }
}

@Composable
fun ScreensaverStateSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val ss = settings.screensaver

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Malam / Screensaver Anti Burn-In (${ss.schedules.size} Jadwal)",
            subtitle = "Layar hitam hemat daya dengan jam melayang pada jam istirahat malam",
            headerAction = {
                HybridButton(
                    label = "+ Tambah Jadwal",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = {
                        val item = ScreenSaverSchedule("ss-${System.currentTimeMillis()}", "22:00", "03:00", true)
                        actions.onUpdateSettings { it.copy(screensaver = ss.copy(schedules = ss.schedules + item)) }
                    }
                )
            }
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Malam (Screensaver Otomatis)",
                checked = ss.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(screensaver = ss.copy(enabled = v)) } }
            )
            HybridTextInput(
                label = "Pesan Tampilan Screensaver",
                value = ss.message,
                onValueChange = { v -> actions.onUpdateSettings { it.copy(screensaver = ss.copy(message = v)) } }
            )

            ss.schedules.forEachIndexed { idx, sched ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.8f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HybridTextInput("Mulai (HH:mm)", sched.startTime, { v ->
                        val list = ss.schedules.toMutableList(); list[idx] = sched.copy(startTime = v)
                        actions.onUpdateSettings { it.copy(screensaver = ss.copy(schedules = list)) }
                    }, modifier = Modifier.weight(1f))
                    HybridTextInput("Selesai (HH:mm)", sched.endTime, { v ->
                        val list = ss.schedules.toMutableList(); list[idx] = sched.copy(endTime = v)
                        actions.onUpdateSettings { it.copy(screensaver = ss.copy(schedules = list)) }
                    }, modifier = Modifier.weight(1f))
                    HybridButton(if (sched.enabled) "AKTIF" else "OFF", isPrimary = sched.enabled, onClick = {
                        val list = ss.schedules.toMutableList(); list[idx] = sched.copy(enabled = !sched.enabled)
                        actions.onUpdateSettings { it.copy(screensaver = ss.copy(schedules = list)) }
                    })
                    HybridButton("Hapus", icon = Icons.Default.Delete, isDanger = true, onClick = {
                        actions.onUpdateSettings { it.copy(screensaver = ss.copy(schedules = ss.schedules.filterIndexed { i, _ -> i != idx })) }
                    })
                }
            }
        }

        HybridSectionCard(
            title = "Panel Simulasi 31-State & Uji Audio",
            subtitle = "Uji langsung semua layar state (Adzan, Do'a Adzan, Iqomah, Jum'at, Tarawih, QR, Live, CCTV, Beep, Sirine)"
        ) {
            HybridButton(
                label = "Buka Panel Simulasi 31-State",
                icon = Icons.Default.PlayArrow,
                isPrimary = true,
                onClick = actions.onOpenDebugSimulator
            )
        }
    }
}

@Composable
fun LiveAndCctvSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val live = settings.live

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // 1. Live YouTube On-Demand & Presets
        HybridSectionCard(
            title = "Mode Live YouTube (Makkah / Madinah / Kustom — Putar Langsung)",
            subtitle = if (actions.isLiveActiveNow) "STATUS: SIARAN LANGSUNG SEDANG TAYANG" else "Putar siaran langsung seketika dengan preset durasi"
        ) {
            HybridSegmentedRow(
                options = listOf("makkah" to "Makkah Live", "madinah" to "Madinah Live", "custom" to "YouTube Kustom"),
                selectedId = live.source,
                onSelect = { src -> actions.onUpdateSettings { it.copy(live = live.copy(source = src)) } }
            )

            if (live.source == "custom") {
                HybridTextInput(
                    label = "URL atau Video ID YouTube Kustom",
                    value = live.customVideoId,
                    placeholder = "https://www.youtube.com/watch?v=... atau Video ID",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(customVideoId = v)) } }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput(
                    label = "Preset ID Makkah Live (YouTube)",
                    value = live.makkahVideoId,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(makkahVideoId = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Preset ID Madinah Live (YouTube)",
                    value = live.madinahVideoId,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(madinahVideoId = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            HybridToggleRow(
                label = "Utamakan Native ExoPlayer HLS (.m3u8) — Lebih Hemat RAM di STB 2GB",
                checked = live.preferNativeHls,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(preferNativeHls = v)) } }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput(
                    label = "URL HLS (.m3u8) Makkah (Opsional)",
                    value = live.makkahHlsUrl,
                    placeholder = "https://.../makkah.m3u8",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(makkahHlsUrl = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "URL HLS (.m3u8) Madinah (Opsional)",
                    value = live.madinahHlsUrl,
                    placeholder = "https://.../madinah.m3u8",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(madinahHlsUrl = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }

            if (live.source == "custom") {
                HybridTextInput(
                    label = "URL Stream HLS Kustom (.m3u8) — Opsional",
                    value = live.customStreamUrl,
                    placeholder = "https://.../stream.m3u8 (kosongkan jika pakai YouTube ID)",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(customStreamUrl = v)) } }
                )
            }

            Text("DURASI PUTAR LANGSUNG", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            HybridSegmentedRow(
                options = listOf("15" to "15 Menit", "30" to "30 Menit", "60" to "60 Menit", "0" to "Non-Stop"),
                selectedId = live.directLiveDurationMinutes.toString(),
                onSelect = { d ->
                    d.toIntOrNull()?.let { mins ->
                        actions.onUpdateSettings { it.copy(live = live.copy(directLiveDurationMinutes = mins)) }
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (actions.isLiveActiveNow || live.directLiveEnabled) {
                    HybridButton(
                        label = "Hentikan Siaran Live",
                        icon = Icons.Default.Stop,
                        isDanger = true,
                        onClick = actions.onStopDirectLive
                    )
                } else {
                    HybridButton(
                        label = "Mulai Putar Live Sekarang",
                        icon = Icons.Default.PlayArrow,
                        isPrimary = true,
                        onClick = { actions.onStartDirectLive(live.source, live.customVideoId, live.directLiveDurationMinutes) }
                    )
                }
                HybridToggleRow(
                    label = "Bisukan Suara (Mute)",
                    checked = live.muted,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(muted = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwitchHardwareSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val sw = settings.switchSettings
    var selectedId by remember { mutableStateOf(sw.switches.firstOrNull()?.id ?: "hardware") }
    val selectedIdx = sw.switches.indexOfFirst { it.id == selectedId }
    val selectedChannel = sw.switches.getOrNull(selectedIdx)

    val prayerLabels = remember {
        listOf(
            "fajr" to "Subuh",
            "dhuhr" to "Dzuhur",
            "asr" to "Ashar",
            "maghrib" to "Maghrib",
            "isha" to "Isya"
        )
    }

    val statePresets = remember {
        listOf(
            "tartil" to "Tartil",
            "tarhim" to "Tarhim",
            "adzan" to "Adzan",
            "menunggu_iqomah" to "Iqomah",
            "pelaksanaan_sholat" to "Sholat",
            "standby" to "Standby"
        )
    }

    fun updateChannel(idx: Int, transform: (id.algaffar.jwstv.domain.model.SwitchChannelConfig) -> id.algaffar.jwstv.domain.model.SwitchChannelConfig) {
        actions.onUpdateSettings { prev ->
            val list = prev.switchSettings.switches.toMutableList()
            val cur = list.getOrNull(idx) ?: return@onUpdateSettings prev
            list[idx] = transform(cur)
            prev.copy(switchSettings = prev.switchSettings.copy(switches = list))
        }
    }

    val maxPins = sw.maxRelayPins()
    val visibleSwitches = sw.switches.filter { it.pin in 1..maxPins }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Profil Board & Jumlah Pin Relay ($maxPins Pin Aktif)",
            subtitle = "Pilih tipe modul relay (USB Relay 2-Channel HID Plug & Play tanpa mikrokontroler, ESP32 8-Pin, atau Wemos D1 Mini 4/2-Pin)"
        ) {
            HybridSegmentedRow(
                options = listOf(
                    "usb_relay_2" to "USB Relay 2-Ch (HID Plug & Play)",
                    "esp32_8" to "ESP32 (8 Pin)",
                    "wemos_4" to "Wemos D1 (4 Pin)",
                    "wemos_2" to "Wemos D1 (2 Pin)"
                ),
                selectedId = sw.boardProfile,
                onSelect = { profile ->
                    val newMax = when (profile) {
                        "usb_relay_1" -> 1
                        "wemos_2", "usb_relay_2" -> 2
                        "wemos_4" -> 4
                        else -> 8
                    }
                    if (selectedChannel != null && selectedChannel.pin > newMax) {
                        selectedId = "hardware"
                    }
                    actions.onUpdateSettings { prev ->
                        val nextConn = if (profile == "usb_relay_2" || profile == "usb_relay_1") {
                            "usb"
                        } else {
                            prev.switchSettings.connectionType
                        }
                        prev.copy(
                            switchSettings = prev.switchSettings.copy(
                                boardProfile = profile,
                                connectionType = nextConn
                            )
                        )
                    }
                }
            )
        }

        HybridSectionCard(
            title = "Pilih Konfigurasi / Channel Saklar ($maxPins Pin)",
            subtitle = "Pilih channel untuk mengatur jadwal otomatis berdasarkan waktu sholat atau transisi state layar"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HybridButton(
                    label = "⚙ Konfigurasi Hardware",
                    isPrimary = selectedId == "hardware",
                    onClick = { selectedId = "hardware" }
                )
                visibleSwitches.forEach { ch ->
                    val isPinOn = actions.relayPinStates[ch.pin] == true
                    val activeTag = if (ch.enabled) "•" else ""
                    HybridButton(
                        label = "PIN ${ch.pin}: ${ch.name} $activeTag".trim(),
                        isPrimary = selectedId == ch.id || isPinOn,
                        onClick = { selectedId = ch.id }
                    )
                }
            }
        }

        if (selectedId == "hardware" || selectedChannel == null || selectedChannel.pin > maxPins) {
            HybridSectionCard(
                title = "Konfigurasi Hardware Relay (WiFi HTTP, USB HID & USB Serial OTG)",
                subtitle = "Atur mode koneksi ke modul relay ($maxPins channel) dan aktifkan otomasi saklar"
            ) {
                HybridToggleRow(
                    label = "Aktifkan Otomasi Saklar Relay ($maxPins Pin)",
                    checked = sw.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(enabled = v)) } }
                )
                HybridSegmentedRow(
                    options = listOf(
                        "wifi" to "Mode Koneksi: WiFi HTTP Relay",
                        "usb" to "Mode Koneksi: USB OTG (HID / Serial)"
                    ),
                    selectedId = sw.connectionType,
                    onSelect = { ct -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(connectionType = ct)) } }
                )
                if (sw.connectionType == "wifi") {
                    Text(
                        text = "MODE JARINGAN WIFI RELAY (ONLINE VS OFFLINE)",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HybridSegmentedRow(
                        options = listOf(
                            "online" to "ONLINE (Gabung WiFi Masjid • Fitur Live Aktif)",
                            "offline_ap" to "OFFLINE (Hotspot AP Mandiri 192.168.4.1)"
                        ),
                        selectedId = sw.wifiMode.ifBlank { "online" },
                        onSelect = { mode ->
                            actions.onUpdateSettings { prev ->
                                val nextIp = if (mode == "offline_ap") "192.168.4.1" else prev.switchSettings.wifiIp
                                prev.copy(
                                    switchSettings = prev.switchSettings.copy(
                                        wifiMode = mode,
                                        wifiIp = nextIp
                                    )
                                )
                            }
                        }
                    )

                    HybridTextInput(
                        label = "IP Address Modul WiFi Relay (LAN Privat / 192.168.4.1 untuk Mode Offline AP)",
                        value = sw.wifiIp,
                        placeholder = if (sw.wifiMode == "offline_ap") "192.168.4.1" else "192.168.1.250",
                        onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(wifiIp = v)) } }
                    )

                    if (sw.wifiMode != "offline_ap") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridTextInput(
                                label = "Nama WiFi Masjid / Hotspot TV Box (SSID)",
                                value = sw.targetWifiSsid,
                                placeholder = "Contoh: WiFi_Masjid_AlGaffar",
                                onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(targetWifiSsid = v)) } },
                                modifier = Modifier.weight(1f)
                            )
                            HybridTextInput(
                                label = "Password WiFi Masjid",
                                value = sw.targetWifiPassword,
                                placeholder = "Password WiFi Masjid",
                                onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(targetWifiPassword = v)) } },
                                modifier = Modifier.weight(1f)
                            )
                            HybridTextInput(
                                label = "Kunci Akhiran IP (.250)",
                                value = sw.staticIpLastOctet.toString(),
                                placeholder = "250",
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 254) ?: 250
                                    actions.onUpdateSettings { it.copy(switchSettings = sw.copy(staticIpLastOctet = digits)) }
                                },
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HybridButton(
                            label = "Kirim Pengaturan WiFi ke ESP32/Wemos (via USB/WiFi)",
                            isPrimary = true,
                            onClick = actions.onProvisionRelayWifiConfig
                        )
                        if (sw.wifiMode == "offline_ap" && sw.wifiIp != "192.168.4.1") {
                            HybridButton(
                                label = "Set IP ke 192.168.4.1",
                                onClick = {
                                    actions.onUpdateSettings { it.copy(switchSettings = sw.copy(wifiIp = "192.168.4.1")) }
                                }
                            )
                        }
                    }
                    Text(
                        text = "Tip: Anda juga bisa mengubah Nama WiFi & Password masjid kapan saja dari HP dengan menyambung ke Hotspot 'JWS_Relay_AP' (sandi: algaffar123) lalu buka http://192.168.4.1 di browser.",
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    HybridButton(
                        label = "Deteksi & Minta Izin Modul USB Relay (USB HID / Serial OTG)",
                        isPrimary = true,
                        onClick = actions.onRequestUsbPermission
                    )
                    if (sw.isPlugAndPlayUsbRelay()) {
                        Text(
                            text = "Mode Plug & Play Aktif (USB-Relay-2 HID / LCUS): Cukup colok kabel USB dari modul relay 2-channel ke port USB TV/STB lalu klik tombol 'Deteksi & Minta Izin' di atas. Tanpa perlu coding Arduino/ESP32.",
                            color = Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HybridTextInput(
                        label = "Device USB (Opsional, kosongkan / biarkan default untuk deteksi otomatis)",
                        value = sw.usbPort,
                        placeholder = "/dev/ttyUSB0",
                        onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(usbPort = v)) } }
                    )
                    Text(
                        text = "Baud Rate Serial USB (bps — khusus CH340/ESP32/Wemos, diabaikan untuk USB HID)",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    HybridSegmentedRow(
                        options = listOf(
                            "9600" to "9600",
                            "19200" to "19200",
                            "38400" to "38400",
                            "57600" to "57600",
                            "115200" to "115200"
                        ),
                        selectedId = sw.baudRate.toString(),
                        onSelect = { bStr ->
                            bStr.toIntOrNull()?.let { b ->
                                actions.onUpdateSettings { it.copy(switchSettings = sw.copy(baudRate = b)) }
                            }
                        }
                    )
                }
            }
        } else {
            val isPinOn = actions.relayPinStates[selectedChannel.pin] == true
            HybridSectionCard(
                title = "Saklar PIN ${selectedChannel.pin}: ${selectedChannel.name}",
                subtitle = "Atur nama, status aktif, uji manual, dan jadwal ON/OFF per waktu sholat atau status aplikasi"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HybridTextInput(
                        label = "Nama Saklar",
                        value = selectedChannel.name,
                        onValueChange = { v -> updateChannel(selectedIdx) { it.copy(name = v.uppercase()) } },
                        modifier = Modifier.weight(1f)
                    )
                    HybridButton(
                        label = if (selectedChannel.enabled) "Aktif: ON" else "Aktif: OFF",
                        isPrimary = selectedChannel.enabled,
                        onClick = { updateChannel(selectedIdx) { it.copy(enabled = !it.enabled) } }
                    )
                    HybridButton(
                        label = if (isPinOn) "Test Relay: ON" else "Test Relay: OFF",
                        isPrimary = isPinOn,
                        onClick = { actions.onToggleManualRelay(selectedChannel.pin, !isPinOn) }
                    )
                }

                HybridSegmentedRow(
                    options = listOf(
                        "prayer" to "Berdasarkan Waktu Sholat",
                        "state" to "Berdasarkan Status Aplikasi"
                    ),
                    selectedId = selectedChannel.triggerMode,
                    onSelect = { mode -> updateChannel(selectedIdx) { it.copy(triggerMode = mode) } }
                )

                if (selectedChannel.triggerMode == "prayer") {
                    Text(
                        text = "Kosongkan kolom jika tidak dipakai (contoh: Maghrib ON 30, OFF kosong; Isya ON kosong, OFF 60).",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                    prayerLabels.forEach { (prayerId, label) ->
                        val curSched = selectedChannel.schedules[prayerId] ?: id.algaffar.jwstv.domain.model.PrayerSwitchSchedule()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.bgPrimary.copy(alpha = 0.78f))
                                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label.uppercase(),
                                color = palette.accentSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(0.35f)
                            )
                            HybridTextInput(
                                label = "Menyala Sebelum (Menit)",
                                value = curSched.onOffset?.toString().orEmpty(),
                                placeholder = "Kosong = tidak dipakai",
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    val parsed = digits.toIntOrNull()
                                    updateChannel(selectedIdx) { ch ->
                                        val nextMap = ch.schedules.toMutableMap()
                                        nextMap[prayerId] = curSched.copy(onOffset = parsed)
                                        ch.copy(schedules = nextMap)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridTextInput(
                                label = "Mati Sesudah (Menit)",
                                value = curSched.offOffset?.toString().orEmpty(),
                                placeholder = "Kosong = tidak dipakai",
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    val parsed = digits.toIntOrNull()
                                    updateChannel(selectedIdx) { ch ->
                                        val nextMap = ch.schedules.toMutableMap()
                                        nextMap[prayerId] = curSched.copy(offOffset = parsed)
                                        ch.copy(schedules = nextMap)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Saklar Otomatis ON/OFF berdasarkan transisi status layar TV (misal Audio Luar / Dalam).",
                        color = palette.textSecondary,
                        fontSize = 13.sp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.78f))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("KONDISI MENYALA (ON)", color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            statePresets.forEach { (stId, stLabel) ->
                                HybridButton(
                                    label = stLabel,
                                    isPrimary = selectedChannel.onState == stId,
                                    onClick = { updateChannel(selectedIdx) { it.copy(onState = stId) } }
                                )
                            }
                        }
                        HybridNumberStepper(
                            label = "Offset Waktu ON (Menit)",
                            value = selectedChannel.onStateOffset,
                            min = -30,
                            max = 60,
                            unit = "mnt",
                            onValueChange = { v -> updateChannel(selectedIdx) { it.copy(onStateOffset = v) } }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.78f))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("KONDISI MATI (OFF)", color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            statePresets.forEach { (stId, stLabel) ->
                                HybridButton(
                                    label = stLabel,
                                    isPrimary = selectedChannel.offState == stId,
                                    onClick = { updateChannel(selectedIdx) { it.copy(offState = stId) } }
                                )
                            }
                        }
                        HybridNumberStepper(
                            label = "Offset Waktu OFF (Menit)",
                            value = selectedChannel.offStateOffset,
                            min = -30,
                            max = 60,
                            unit = "mnt",
                            onValueChange = { v -> updateChannel(selectedIdx) { it.copy(offStateOffset = v) } }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modular Sub-Tab 4.1: Jum'at & Ramadhan (`jumatRamadhan`).
 * Keeps quick toggles & durations right on the detail pane, opening Level-3 Slide-Over Drawers
 * for detailed Jum'at Kas/Petugas & Ramadhan Schedules/Ta'jil/Sirine.
 */
@Composable
fun JumatAndRamadhanSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "jumat" | "ramadhan"
    val jumat = settings.jumat
    val ram = settings.ramadhan

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Mode Sholat Jum'at",
            subtitle = "Pengaturan cepat mode Jum'at + laci lengkap petugas & jurnal buku kas"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Khusus Hari Jum'at (Menggantikan Dzuhur)",
                checked = jumat.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(enabled = v)) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Waktu Jum'at",
                    value = jumat.waktuJumatDuration,
                    min = 5,
                    max = 45,
                    unit = "mnt",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(waktuJumatDuration = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Khutbah Jum'at",
                    value = jumat.khutbahDuration,
                    min = 10,
                    max = 60,
                    unit = "mnt",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(khutbahDuration = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
            HybridNavigationRow(
                label = "Petugas Khotib/Imam & Buku Kas Jum'at",
                description = "Kelola jadwal petugas Jum'at, saldo kas pekan lalu, dan jurnal transaksi masuk/keluar",
                valueSummary = "Buka Editor Jum'at",
                icon = Icons.Default.CalendarMonth,
                accentBadgeColor = Color(0xFFFBBF24),
                onClick = { activeDrawer = "jumat" }
            )
        }

        HybridSectionCard(
            title = "Mode Bulan Suci Ramadhan",
            subtitle = "Jadwal Tarawih, Penceramah, Kas Infaq, Ta'jil Excel 30 Hari & Sirine Imsak/Maghrib"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Khusus Ramadhan",
                description = "Malam aktif: Malam ke-${ram.currentNight} • Tahun ${ram.hijriYear} H",
                checked = ram.isRamadhan,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(isRamadhan = v)) } }
            )
            HybridToggleRow(
                label = "Aktifkan Sirine Ramadhan (Sahur / Imsak / Maghrib)",
                checked = ram.sirine.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(ramadhan = it.ramadhan.copy(sirine = it.ramadhan.sirine.copy(enabled = v))) } }
            )
            HybridNavigationRow(
                label = "Petugas, Kas, Ta'jil Excel & Sirine Ramadhan",
                description = "Import/export Excel Ta'jil 30 hari, petugas tarawih/witir, keuangan & jadwal sirine",
                valueSummary = "Malam ke-${ram.currentNight}",
                icon = Icons.Default.NightsStay,
                accentBadgeColor = Color(0xFFFBBF24),
                onClick = { activeDrawer = "ramadhan" }
            )
        }
    }

    when (activeDrawer) {
        "jumat" -> {
            HybridSideDrawer(
                title = "Pengaturan Lengkap Hari Jum'at",
                subtitle = "Petugas, durasi, pesan layar khutbah & jurnal buku kas Jum'at",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                JumatSettingsTab(settings, actions)
            }
        }
        "ramadhan" -> {
            HybridSideDrawer(
                title = "Pengaturan Lengkap Ramadhan",
                subtitle = "Jadwal Tarawih, Kas, Import/Export Ta'jil Excel & Sirine Ramadhan",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                RamadhanSettingsTab(settings, actions)
            }
        }
    }
}

/**
 * Modular Sub-Tab 4.2: Idul Fitri & Adha (`hariRayaQurban`).
 */
@Composable
fun HariRayaAndQurbanSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "fitri" | "adha"
    val fitri = settings.idulFitri
    val adha = settings.idulAdha

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Mode Hari Raya Idul Fitri",
            subtitle = "Slot jam tayang, petugas sholat Ied & pesan khutbah"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Hari Raya Idul Fitri",
                checked = fitri.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(idulFitri = it.idulFitri.copy(enabled = v)) } }
            )
            HybridNavigationRow(
                label = "Petugas, Durasi & Slot Jam Idul Fitri",
                description = "Khotib: ${fitri.petugas.khotib.ifBlank { "-" }}  •  ${fitri.timeSlots.size} Slot Jam",
                valueSummary = "Kelola Idul Fitri",
                icon = Icons.Default.Event,
                accentBadgeColor = Color(0xFFFBBF24),
                onClick = { activeDrawer = "fitri" }
            )
        }

        HybridSectionCard(
            title = "Mode Hari Raya Idul Adha & Peserta Qurban",
            subtitle = "Kelola petugas Idul Adha & kelompok peserta Qurban (maksimal 7 anggota per kelompok)"
        ) {
            HybridToggleRow(
                label = "Aktifkan Mode Hari Raya Idul Adha",
                checked = adha.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(idulAdha = it.idulAdha.copy(enabled = v)) } }
            )
            HybridToggleRow(
                label = "Aktifkan Mode Sosialisasi Info Qurban",
                checked = settings.infoQurban.enabled,
                onCheckedChange = { v -> actions.onUpdateSettings { it.copy(infoQurban = it.infoQurban.copy(enabled = v)) } }
            )
            HybridNavigationRow(
                label = "Petugas Idul Adha & Kelompok Qurban",
                description = "CRUD kelompok peserta qurban (Sapi/Kambing), maksimal ${QurbanGroup.MAX_MEMBERS} anggota per kelompok",
                valueSummary = "${adha.qurbanGroups.size} Kelompok",
                icon = Icons.Default.Event,
                accentBadgeColor = Color(0xFFFBBF24),
                onClick = { activeDrawer = "adha" }
            )
        }
    }

    when (activeDrawer) {
        "fitri" -> {
            HybridSideDrawer(
                title = "Pengaturan Idul Fitri",
                subtitle = "Petugas, durasi Sholat/Khutbah Ied, pesan layar & slot jam",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                IdulFitriSettingsTab(settings, actions)
            }
        }
        "adha" -> {
            HybridSideDrawer(
                title = "Idul Adha & Kelompok Qurban",
                subtitle = "Petugas Idul Adha & CRUD Kelompok Peserta Qurban (${adha.qurbanGroups.size} Kelompok)",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                IdulAdhaQurbanSettingsTab(settings, actions)
            }
        }
    }
}

/**
 * Modular Sub-Tab 4.3: Live & Screensaver (`liveScreensaver`).
 */
@Composable
fun LiveAndScreensaverSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions,
    activeRole: UserRole
) {
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "live" | "screensaver"
    val ss = settings.screensaver

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Siaran Live Makkah / Madinah / Kustom & CCTV",
            subtitle = if (actions.isLiveActiveNow) "STATUS: SIARAN LANGSUNG SEDANG TAYANG" else "Putar siaran langsung seketika atau atur preset HLS & kamera CCTV RTSP"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridButton(
                    label = "Putar Makkah Live",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = { actions.onStartDirectLive("makkah", "", 60) },
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "Putar Madinah Live",
                    icon = Icons.Default.PlayArrow,
                    onClick = { actions.onStartDirectLive("madinah", "", 60) },
                    modifier = Modifier.weight(1f)
                )
                if (actions.isLiveActiveNow) {
                    HybridButton(
                        label = "Hentikan Live",
                        icon = Icons.Default.Stop,
                        isDanger = true,
                        onClick = actions.onStopDirectLive
                    )
                }
            }
            HybridNavigationRow(
                label = "Pengaturan Lengkap Live YouTube, HLS & CCTV",
                description = "Ubah ID YouTube, URL HLS .m3u8 prioritas native, dan daftar kamera CCTV ONVIF/RTSP",
                valueSummary = "Sumber: ${settings.live.source.uppercase()}",
                icon = Icons.Default.LiveTv,
                accentBadgeColor = Color(0xFFFBBF24),
                onClick = { activeDrawer = "live" }
            )
        }

        if (activeRole != UserRole.USER) {
            HybridSectionCard(
                title = "Mode Malam / Screensaver Anti Burn-In",
                subtitle = "Layar hitam hemat daya dengan jam melayang pada jam istirahat malam"
            ) {
                HybridToggleRow(
                    label = "Aktifkan Mode Malam (Screensaver Otomatis)",
                    description = "${ss.schedules.size} jadwal aktif",
                    checked = ss.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(screensaver = it.screensaver.copy(enabled = v)) } }
                )
                HybridNavigationRow(
                    label = "Kelola Jadwal Screensaver & Simulasi State",
                    description = "Atur jam mulai/selesai mode malam, pesan screensaver & akses simulator 30-State",
                    valueSummary = "${ss.schedules.size} Jadwal",
                    icon = Icons.Default.AutoAwesome,
                    accentBadgeColor = Color(0xFFFBBF24),
                    onClick = { activeDrawer = "screensaver" }
                )
            }
        }
    }

    when (activeDrawer) {
        "live" -> {
            HybridSideDrawer(
                title = "Pengaturan Siaran Live & CCTV",
                subtitle = "YouTube Live, HLS Native (.m3u8) & kamera CCTV RTSP/ONVIF",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                LiveAndCctvSettingsTab(settings, actions)
            }
        }
        "screensaver" -> {
            HybridSideDrawer(
                title = "Screensaver & Simulasi State",
                subtitle = "Jadwal mode malam anti burn-in & panel pengujian",
                accentColor = Color(0xFFFBBF24),
                onDismiss = { activeDrawer = null }
            ) {
                ScreensaverStateSettingsTab(settings, actions)
            }
        }
    }
}
