package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.JumatJournalEntry
import id.algaffar.jwstv.domain.model.JumatKeuanganRecord
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.QurbanGroup
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SUMBER DATA KAS JUM'AT", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    HybridSegmentedRow(
                        options = listOf("manual" to "Manual Lokal", "online" to "Google Sheets Online"),
                        selectedId = jumat.keuangan.source,
                        onSelect = { src ->
                            actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(keuangan = it.jumat.keuangan.copy(source = src))) }
                        }
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TIPE QR CODE KAS JUM'AT", color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    HybridSegmentedRow(
                        options = listOf("text_summary" to "Ringkasan Offline", "custom_url" to "URL Kustom"),
                        selectedId = jumat.keuangan.qrType,
                        onSelect = { qt ->
                            actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(keuangan = it.jumat.keuangan.copy(qrType = qt))) }
                        }
                    )
                }
            }

            if (jumat.keuangan.qrType == "custom_url") {
                HybridTextInput(
                    label = "URL Kustom QR Kas Jum'at",
                    value = jumat.keuangan.qrCustomUrl,
                    placeholder = "https://...",
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(jumat = it.jumat.copy(keuangan = it.jumat.keuangan.copy(qrCustomUrl = v))) }
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
    val ramadhan = settings.ramadhan
    val nightIdx = (ramadhan.currentNight - 1).coerceIn(0, 29)
    val currentPetugas = ramadhan.petugas.getOrNull(nightIdx)
    val currentKeuangan = ramadhan.keuangan.getOrNull(nightIdx)
    val currentTakjilList = ramadhan.takjil[ramadhan.currentNight.toString()].orEmpty()
    var newDonorName by remember { mutableStateOf("") }

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

@Composable
fun IdulAdhaQurbanSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val adha = settings.idulAdha
    var groupNameInput by remember { mutableStateOf("Kelompok Qurban Baru") }
    var animalTypeInput by remember { mutableStateOf("Sapi") }
    var membersCsvInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Hari Raya Idul Adha & Sosialisasi Info Qurban",
            subtitle = "Kelola jadwal Hari Raya Idul Adha, petugas, serta daftar kelompok peserta Qurban"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridToggleRow("Aktifkan Mode Idul Adha", adha.enabled, { v -> actions.onUpdateSettings { it.copy(idulAdha = adha.copy(enabled = v)) } }, modifier = Modifier.weight(1f))
                HybridToggleRow("Aktifkan Mode Info Qurban", settings.infoQurban.enabled, { v -> actions.onUpdateSettings { it.copy(infoQurban = it.infoQurban.copy(enabled = v)) } }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridTextInput("Khotib Idul Adha", adha.petugas.khotib, { v -> actions.onUpdateSettings { it.copy(idulAdha = adha.copy(petugas = adha.petugas.copy(khotib = v))) } }, modifier = Modifier.weight(1f))
                HybridTextInput("Imam Idul Adha", adha.petugas.imam, { v -> actions.onUpdateSettings { it.copy(idulAdha = adha.copy(petugas = adha.petugas.copy(imam = v))) } }, modifier = Modifier.weight(1f))
                HybridTextInput("Bilal / Muadzin", adha.petugas.bilal, { v -> actions.onUpdateSettings { it.copy(idulAdha = adha.copy(petugas = adha.petugas.copy(bilal = v))) } }, modifier = Modifier.weight(1f))
            }
        }

        HybridSectionCard(
            title = "Kelompok Peserta Qurban (${adha.qurbanGroups.size} Kelompok)",
            subtitle = "Tambahkan atau hapus kelompok Sapi (7 mudhohi) / Kambing"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                HybridTextInput("Nama Kelompok", groupNameInput, { groupNameInput = it }, modifier = Modifier.weight(1f))
                HybridButton(
                    label = "Hewan: $animalTypeInput",
                    onClick = { animalTypeInput = if (animalTypeInput == "Sapi") "Kambing" else "Sapi" }
                )
            }
            HybridTextInput("Daftar Nama Peserta (pisahkan dengan koma)", membersCsvInput, { membersCsvInput = it }, "H. Ahmad, Bpk. Budi, Ibu Siti...")
            HybridButton(
                label = "+ Simpan Kelompok Qurban",
                icon = Icons.Default.Add,
                isPrimary = true,
                onClick = {
                    val members = membersCsvInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (groupNameInput.isNotBlank() && members.isNotEmpty()) {
                        val grp = QurbanGroup(System.currentTimeMillis().toString(), animalTypeInput, groupNameInput.trim(), members)
                        actions.onUpdateSettings { prev -> prev.copy(idulAdha = prev.idulAdha.copy(qurbanGroups = prev.idulAdha.qurbanGroups + grp)) }
                        membersCsvInput = ""
                    }
                }
            )

            adha.qurbanGroups.forEach { grp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.8f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${grp.groupName} (${grp.animalType})", color = palette.accentSecondary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(grp.members.joinToString(", "), color = palette.textSecondary, fontSize = 13.sp)
                    }
                    HybridButton(
                        label = "Hapus",
                        icon = Icons.Default.Delete,
                        isDanger = true,
                        onClick = {
                            actions.onUpdateSettings { prev ->
                                prev.copy(idulAdha = prev.idulAdha.copy(qurbanGroups = prev.idulAdha.qurbanGroups.filterNot { it.id == grp.id }))
                            }
                        }
                    )
                }
            }
        }

        TimeSlotListCard(
            title = "Slot Jam Tayang Idul Adha (${adha.timeSlots.size} Slot)",
            slots = adha.timeSlots,
            onUpdateSlots = { updated -> actions.onUpdateSettings { it.copy(idulAdha = adha.copy(timeSlots = updated)) } }
        )
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
                    label = "Preset ID Makkah Live",
                    value = live.makkahVideoId,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(makkahVideoId = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridTextInput(
                    label = "Preset ID Madinah Live",
                    value = live.madinahVideoId,
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(live = live.copy(madinahVideoId = v)) } },
                    modifier = Modifier.weight(1f)
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

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Pilih Konfigurasi / Channel Saklar (8 Pin)",
            subtitle = "Mengacu pada JWS TV Al-Gaffar: pilih channel untuk mengatur jadwal tiap waktu sholat atau status aplikasi"
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
                sw.switches.forEach { ch ->
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

        if (selectedId == "hardware" || selectedChannel == null) {
            HybridSectionCard(
                title = "Konfigurasi Hardware Relay (WiFi HTTP & USB Serial OTG)",
                subtitle = "Atur mode koneksi ke modul relay 8 channel dan aktifkan otomasi saklar"
            ) {
                HybridToggleRow(
                    label = "Aktifkan Otomasi Saklar Relay 8 Channel",
                    checked = sw.enabled,
                    onCheckedChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(enabled = v)) } }
                )
                HybridSegmentedRow(
                    options = listOf("wifi" to "Mode Koneksi: WiFi HTTP Relay", "usb" to "Mode Koneksi: USB OTG Serial"),
                    selectedId = sw.connectionType,
                    onSelect = { ct -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(connectionType = ct)) } }
                )
                if (sw.connectionType == "wifi") {
                    HybridTextInput(
                        label = "IP Address Modul WiFi Relay (LAN Privat)",
                        value = sw.wifiIp,
                        placeholder = "192.168.1.50",
                        onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(wifiIp = v)) } }
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HybridTextInput(
                            label = "Port USB Serial",
                            value = sw.usbPort,
                            placeholder = "/dev/ttyUSB0",
                            onValueChange = { v -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(usbPort = v)) } },
                            modifier = Modifier.weight(1f)
                        )
                        HybridNumberStepper(
                            label = "Baud Rate",
                            value = sw.baudRate,
                            min = 9600,
                            max = 115200,
                            unit = "bps",
                            onValueChange = { b -> actions.onUpdateSettings { it.copy(switchSettings = sw.copy(baudRate = b)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
