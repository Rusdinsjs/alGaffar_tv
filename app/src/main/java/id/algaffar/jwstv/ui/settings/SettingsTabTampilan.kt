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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.CustomIslamicEventItem
import id.algaffar.jwstv.domain.model.FloatingOverlayItem
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.domain.model.InfoDisplaySettings
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.ScaleMode
import id.algaffar.jwstv.domain.model.ScreenTextsSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.ThemeEngine
import java.time.ZonedDateTime

/**
 * Category 3: TAMPILAN (`themes`, `layout`, `clock`, `overlayText`, `slides`, `info`, `media`, `standbySlideshow`).
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Pengaturan Tema Visual & Mode Performa",
            subtitle = "17 tema islami siap pakai + rotasi tema otomatis 24 jam"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridToggleRow(
                    label = "Rotasi Tema Otomatis 24 Jam",
                    description = "Berganti tema secara berkala tanpa pengulangan",
                    checked = settings.themeSettings.autoThemeEnabled,
                    onCheckedChange = { v ->
                        actions.onUpdateSettings { it.copy(themeSettings = it.themeSettings.copy(autoThemeEnabled = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridToggleRow(
                    label = "Mode Performa Ringan (STB / Android TV)",
                    description = "Mengurangi efek blur berat untuk operasi 24/7 stabil",
                    checked = settings.performanceMode,
                    onCheckedChange = { v ->
                        actions.onUpdateSettings { it.copy(performanceMode = v) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HybridSectionCard(
            title = "Pilih Tema Warna (17 Tema Bawaan)",
            subtitle = "Klik atau tekan OK pada tema di bawah untuk menerapkan langsung"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeEngine.BuiltInThemes.forEach { theme ->
                    val isSelected = settings.themeSettings.currentThemeId == theme.id
                    var isFocused by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .width(235.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.bgPrimary)
                            .border(
                                width = if (isFocused) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                                color = if (isFocused) TvFocusCyan else if (isSelected) theme.accent else theme.border,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                actions.onUpdateSettings {
                                    it.copy(
                                        themeSettings = it.themeSettings.copy(
                                            currentThemeId = theme.id,
                                            autoThemeEnabled = false
                                        )
                                    )
                                }
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme.name,
                                color = theme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(theme.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = theme.bgPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(theme.bgSecondary, theme.bgCard, theme.accent, theme.accentSecondary).forEach { sw ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(sw)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val layout = settings.layoutSettings
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Tata Letak, Skala Kanvas 1080p & Carousel Jadwal",
            subtitle = "Mendukung 3 posisi layout modular dan rotasi otomatis anti burn-in setiap 4 jam"
        ) {
            HybridToggleRow(
                label = "Rotasi Layout Otomatis Setiap 4 Jam (Anti Burn-in)",
                description = "Berganti otomatis antara Horizontal, Vertical Left, dan Vertical Right",
                checked = layout.autoRotateLayout,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(layoutSettings = it.layoutSettings.copy(autoRotateLayout = v)) }
                }
            )

            Text(
                text = "MODE SKALA LAYAR TV",
                color = LocalJwsTheme.current.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            HybridSegmentedRow(
                options = ScaleMode.entries.map { it.serialName to "Skala: ${it.serialName.uppercase()}" },
                selectedId = layout.scaleMode.serialName,
                onSelect = { sel ->
                    ScaleMode.entries.firstOrNull { it.serialName == sel }?.let { mode ->
                        actions.onUpdateSettings { it.copy(layoutSettings = it.layoutSettings.copy(scaleMode = mode)) }
                    }
                }
            )

            HybridNumberStepper(
                label = "Interval Geser Carousel Jadwal Sholat",
                value = settings.prayerSettings.prayerTimeScrollInterval,
                min = 2,
                max = 30,
                unit = "detik",
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(prayerTimeScrollInterval = v)) }
                }
            )
        }

        HybridSectionCard(
            title = "Pilih Posisi Layout Manual",
            subtitle = "Pilih tata letak kartu jadwal sholat sesuai preferensi ruang masjid"
        ) {
            val layoutDescriptions = mapOf(
                LayoutPosition.HORIZONTAL to "Jadwal Sholat di bawah (4 kolom horizontal)",
                LayoutPosition.VERTICAL_LEFT to "Sidebar Jadwal Sholat di sisi kiri layar",
                LayoutPosition.VERTICAL_RIGHT to "Sidebar Jadwal Sholat di sisi kanan layar"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LayoutPosition.entries.forEach { pos ->
                    val isSelected = layout.position == pos
                    val palette = LocalJwsTheme.current
                    var isFocused by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) palette.accent.copy(alpha = 0.16f) else palette.bgPrimary.copy(alpha = 0.8f))
                            .border(
                                width = if (isFocused) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                                color = if (isFocused) TvFocusCyan else if (isSelected) palette.accent else palette.border,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                actions.onUpdateSettings {
                                    it.copy(layoutSettings = it.layoutSettings.copy(position = pos, autoRotateLayout = false))
                                }
                            }
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pos.serialName.uppercase(),
                            color = if (isSelected) palette.accent else palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = layoutDescriptions[pos].orEmpty(),
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockStyleSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val previewNow = remember { ZonedDateTime.now().withHour(12).withMinute(34).withSecond(56) }
    val styles = listOf(
        Triple("classic", "Original Classic", "Tampilan asli JWS yang legendaris"),
        Triple("golden", "Golden Royal", "Kemewahan emas bernuansa Islami"),
        Triple("glass", "Glassmorphic Arctic", "Efek kaca futuristik yang jernih"),
        Triple("wood", "Vintage Wood", "Sentuhan kayu klasik yang hangat"),
        Triple("neon", "Cyber Neon", "Efek cahaya neon berdaya tarik tinggi"),
        Triple("segmented", "Retro Seven-Segment LED", "Gaya jam LED merah display masjid"),
        Triple("emerald", "Emerald Nabawi", "Nuansa hijau kubah Madinah yang teduh"),
        Triple("monochrome", "OLED Ultra Contrast", "Kontras tajam terbaca dari shaf terjauh")
    )

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "8 Gaya Jam Digital Header (Pratinjau Langsung)",
            subtitle = "Pilih gaya jam artistik untuk tampilan header utama"
        ) {
            styles.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { (id, name, desc) ->
                        val isSelected = settings.brand.clockStyle.equals(id, ignoreCase = true)
                        var isFocused by remember { mutableStateOf(false) }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) palette.accent.copy(alpha = 0.14f) else palette.bgPrimary.copy(alpha = 0.85f))
                                .border(
                                    width = if (isFocused) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isFocused) TvFocusCyan else if (isSelected) palette.accent else palette.border,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    actions.onUpdateSettings { it.copy(brand = it.brand.copy(clockStyle = id)) }
                                }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, color = if (isSelected) palette.accent else palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(desc, color = palette.textSecondary, fontSize = 12.sp)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ArtisticClock(now = previewNow, style = id, variant = ClockVariant.HEADER)
                            }
                        }
                    }
                }
            }
        }

        HybridSectionCard(
            title = "Gaya Jam Layar Fullscreen (Tartil / Tarhim)",
            subtitle = "Gaya jam berukuran besar yang tampil pada mode fullscreen"
        ) {
            HybridSegmentedRow(
                options = styles.map { it.first to it.second },
                selectedId = settings.brand.fullscreenClockStyle.lowercase(),
                onSelect = { st -> actions.onUpdateSettings { it.copy(brand = it.brand.copy(fullscreenClockStyle = st)) } }
            )
        }
    }
}

@Composable
fun OverlayTextSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    var newTitle by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }

    fun syncOverlays(updated: List<FloatingOverlayItem>) {
        val validIds = updated.map { it.id }.toSet()
        actions.onUpdateSettings { prev ->
            prev.copy(
                floatingOverlays = updated,
                overlayTextList = updated.map { it.message },
                slideGroups = prev.slideGroups.map { grp ->
                    grp.copy(overlayIds = grp.overlayIds.filter { it in validIds })
                }
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Daftar Floating Overlay (${settings.floatingOverlays.size} Overlay)",
            subtitle = "Kelola Judul & Isi Floating Overlay statis. Pemanggilan, posisi sudut, dan durasi tampil otomatis diatur di setiap Group Slideshow."
        ) {
            HybridInfoBanner(
                text = "Floating Overlay ditampilkan secara statis (bukan running text). Untuk menampilkannya di layar TV, pilih Judul Floating Overlay di dalam pengaturan Group Slideshow (menu Tampilan → Slideshow Info). Durasi tiap overlay otomatis dibagi rata = Total Durasi Slide Group ÷ Jumlah Floating Overlay.",
                accentColor = Color(0xFF38BDF8)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgPrimary.copy(alpha = 0.72f))
                    .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HybridTextInput(
                        label = "Judul Floating Overlay Baru",
                        value = newTitle,
                        placeholder = "Contoh: Adab & Ketertiban Shaf",
                        onValueChange = { newTitle = it },
                        modifier = Modifier.weight(1f)
                    )
                    HybridButton(
                        label = "+ Tambah Floating Overlay",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = {
                            val trimmedTitle = newTitle.trim()
                            val trimmedMsg = newMessage.trim()
                            if (trimmedTitle.isNotBlank() || trimmedMsg.isNotBlank()) {
                                val nextNum = settings.floatingOverlays.size + 1
                                val item = FloatingOverlayItem(
                                    id = "overlay_${System.currentTimeMillis()}",
                                    title = trimmedTitle.ifBlank { "Overlay $nextNum" },
                                    message = trimmedMsg
                                )
                                syncOverlays(settings.floatingOverlays + item)
                                newTitle = ""
                                newMessage = ""
                            }
                        }
                    )
                }
                HybridTextInput(
                    label = "Isi Pesan Floating Overlay",
                    value = newMessage,
                    placeholder = "Mohon luruskan dan rapatkan shaf serta nonaktifkan dering HP...",
                    singleLine = false,
                    onValueChange = { newMessage = it }
                )
            }

            if (settings.floatingOverlays.isEmpty()) {
                HybridInfoBanner(
                    text = "Belum ada Floating Overlay. Tambahkan judul dan pesan di atas agar dapat dipanggil oleh Group Slideshow.",
                    accentColor = Color(0xFFF59E0B)
                )
            } else {
                settings.floatingOverlays.forEachIndexed { idx, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.85f))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HybridTextInput(
                                label = "Judul Floating Overlay #${idx + 1}",
                                value = item.title,
                                placeholder = "Overlay ${idx + 1}",
                                onValueChange = { updatedTitle ->
                                    val list = settings.floatingOverlays.toMutableList()
                                    list[idx] = item.copy(title = updatedTitle)
                                    syncOverlays(list)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridButton(
                                label = "Hapus",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                onClick = {
                                    syncOverlays(settings.floatingOverlays.filterIndexed { i, _ -> i != idx })
                                }
                            )
                        }
                        HybridTextInput(
                            label = "Isi Pesan",
                            value = item.message,
                            singleLine = false,
                            onValueChange = { updatedMsg ->
                                val list = settings.floatingOverlays.toMutableList()
                                list[idx] = item.copy(message = updatedMsg)
                                syncOverlays(list)
                            }
                        )
                    }
                }
            }
        }
    }
}

// SlideSettingsTab has been moved to SettingsTabSlides.kt for clean accordion UI & full 19-type parity.

@Composable
fun InfoAndPhbiSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    var subTab by remember { mutableStateOf("umum") }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Kartu Informasi Masjid & Agenda Islami (PHBI)",
            subtitle = "Mengelola konten kartu info kegiatan & hitung mundur Hari Besar Islam"
        ) {
            HybridSegmentedRow(
                options = listOf(
                    "umum" to "Kartu Informasi Umum (${settings.mosqueInfoList.size})",
                    "islami" to "Agenda Islami & PHBI (${settings.infoSettings.islamicEvents.size})"
                ),
                selectedId = subTab,
                onSelect = { subTab = it }
            )
        }

        if (subTab == "umum") {
            HybridSectionCard(
                title = "Daftar Kartu Informasi Masjid",
                subtitle = "Tampil bergantian pada kartu INFO & KEGIATAN MASJID di layar utama",
                headerAction = {
                    HybridButton(
                        label = "+ Tambah Info",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = {
                            val newItem = InfoCardItem(
                                id = "info-${System.currentTimeMillis()}",
                                title = "INFO KEGIATAN BARU",
                                content = "Tulis detail kajian atau pengumuman masjid di sini",
                                subtitle = "Ba'da Maghrib @ Ruang Utama",
                                enabled = true
                            )
                            actions.onUpdateSettings { prev -> prev.copy(mosqueInfoList = prev.mosqueInfoList + newItem) }
                        }
                    )
                }
            ) {
                settings.mosqueInfoList.forEachIndexed { idx, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.8f))
                            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridTextInput(
                                label = "Judul / Label",
                                value = item.title,
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.mosqueInfoList.toMutableList()
                                        list[idx] = item.copy(title = v)
                                        prev.copy(mosqueInfoList = list)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridTextInput(
                                label = "Sub-Judul / Waktu",
                                value = item.subtitle,
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.mosqueInfoList.toMutableList()
                                        list[idx] = item.copy(subtitle = v)
                                        prev.copy(mosqueInfoList = list)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HybridTextInput(
                            label = "Isi Pengumuman",
                            value = item.content,
                            onValueChange = { v ->
                                actions.onUpdateSettings { prev ->
                                    val list = prev.mosqueInfoList.toMutableList()
                                    list[idx] = item.copy(content = v)
                                    prev.copy(mosqueInfoList = list)
                                }
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HybridButton(
                                label = if (item.enabled) "AKTIF" else "NONAKTIF",
                                isPrimary = item.enabled,
                                onClick = {
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.mosqueInfoList.toMutableList()
                                        list[idx] = item.copy(enabled = !item.enabled)
                                        prev.copy(mosqueInfoList = list)
                                    }
                                }
                            )
                            HybridButton(
                                label = "Hapus",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                onClick = {
                                    actions.onUpdateSettings { prev ->
                                        prev.copy(mosqueInfoList = prev.mosqueInfoList.filterIndexed { i, _ -> i != idx })
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            val infoCfg = settings.infoSettings
            HybridSectionCard(
                title = "Pengaturan Puasa Sunnah & Daftar Agenda PHBI",
                subtitle = "Hitung mundur otomatis Hari Besar Islam dan pengingat puasa sunnah",
                headerAction = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HybridButton(
                            label = "Reset Default",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                actions.onUpdateSettings { prev ->
                                    prev.copy(
                                        infoSettings = InfoDisplaySettings(
                                            enableSunnahFasting = true,
                                            lookaheadDays = 60,
                                            islamicEvents = InfoDisplaySettings.defaultIslamicEvents()
                                        )
                                    )
                                }
                            }
                        )
                        HybridButton(
                            label = "+ Tambah Agenda",
                            icon = Icons.Default.Add,
                            isPrimary = true,
                            onClick = {
                                val ev = CustomIslamicEventItem(
                                    id = "agenda-${System.currentTimeMillis()}",
                                    name = "Agenda Masjid Baru",
                                    type = "hijri",
                                    hijriMonth = 1,
                                    hijriDay = 1,
                                    enabled = true
                                )
                                actions.onUpdateSettings { prev ->
                                    prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = listOf(ev) + prev.infoSettings.islamicEvents))
                                }
                            }
                        )
                    }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HybridToggleRow(
                        label = "Info Puasa Sunnah Otomatis",
                        description = "Senin-Kamis, Ayyamul Bidh, Syawal, Arafah, Asyura",
                        checked = infoCfg.enableSunnahFasting,
                        onCheckedChange = { v ->
                            actions.onUpdateSettings { it.copy(infoSettings = it.infoSettings.copy(enableSunnahFasting = v)) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridNumberStepper(
                        label = "Rentang Deteksi Agenda",
                        value = infoCfg.lookaheadDays,
                        min = 7,
                        max = 365,
                        step = 5,
                        unit = "hari",
                        onValueChange = { v ->
                            actions.onUpdateSettings { it.copy(infoSettings = it.infoSettings.copy(lookaheadDays = v)) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                infoCfg.islamicEvents.forEachIndexed { idx, ev ->
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
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HybridTextInput(
                                label = "Nama PHBI / Agenda",
                                value = ev.name,
                                placeholder = "Contoh: Maulid Nabi SAW",
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.infoSettings.islamicEvents.toMutableList()
                                        list[idx] = ev.copy(name = v)
                                        prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridButton(
                                label = if (ev.enabled) "AKTIF" else "NONAKTIF",
                                isPrimary = ev.enabled,
                                onClick = {
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.infoSettings.islamicEvents.toMutableList()
                                        list[idx] = ev.copy(enabled = !ev.enabled)
                                        prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                    }
                                }
                            )
                            HybridButton(
                                label = "Hapus",
                                icon = Icons.Default.Delete,
                                isDanger = true,
                                onClick = {
                                    actions.onUpdateSettings { prev ->
                                        prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = prev.infoSettings.islamicEvents.filterIndexed { i, _ -> i != idx }))
                                    }
                                }
                            )
                        }

                        HybridSegmentedRow(
                            options = listOf(
                                "hijri" to "Kalender Hijriyah",
                                "gregorian" to "Tanggal Masehi",
                                "custom" to "Teks Bebas / Manual"
                            ),
                            selectedId = ev.type,
                            onSelect = { newType ->
                                actions.onUpdateSettings { prev ->
                                    val list = prev.infoSettings.islamicEvents.toMutableList()
                                    list[idx] = ev.copy(
                                        type = newType,
                                        customSubtitle = ev.customSubtitle.ifBlank { "Agenda Masjid" }
                                    )
                                    prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                }
                            }
                        )

                        when (ev.type) {
                            "gregorian" -> {
                                HybridTextInput(
                                    label = "Tanggal Masehi (YYYY-MM-DD)",
                                    value = ev.gregorianDate,
                                    placeholder = "2026-08-17",
                                    onValueChange = { v ->
                                        actions.onUpdateSettings { prev ->
                                            val list = prev.infoSettings.islamicEvents.toMutableList()
                                            list[idx] = ev.copy(gregorianDate = v)
                                            prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                        }
                                    }
                                )
                            }
                            "custom" -> {
                                HybridTextInput(
                                    label = "Keterangan Waktu / Badge",
                                    value = ev.customSubtitle,
                                    placeholder = "Contoh: Ahad Pekan Depan",
                                    onValueChange = { v ->
                                        actions.onUpdateSettings { prev ->
                                            val list = prev.infoSettings.islamicEvents.toMutableList()
                                            list[idx] = ev.copy(customSubtitle = v)
                                            prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                        }
                                    }
                                )
                            }
                            else -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HybridNumberStepper(
                                        label = "Tgl Hijriah",
                                        value = ev.hijriDay,
                                        min = 1,
                                        max = 30,
                                        unit = "",
                                        onValueChange = { v ->
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.infoSettings.islamicEvents.toMutableList()
                                                list[idx] = ev.copy(hijriDay = v)
                                                prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    HybridNumberStepper(
                                        label = "Bln Hijriah (1-12)",
                                        value = ev.hijriMonth,
                                        min = 1,
                                        max = 12,
                                        unit = "",
                                        onValueChange = { v ->
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.infoSettings.islamicEvents.toMutableList()
                                                list[idx] = ev.copy(hijriMonth = v)
                                                prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MediaGallerySettingsTab has been moved to SettingsTabMedia.kt with full background slots & thumbnail previews.

@Composable
fun StandbyModeSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val standby = settings.standbySlideshow
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Mode Fullscreen Standby Slideshow",
            subtitle = "Bergantian otomatis antara Layar Jadwal Utama dan Slideshow Layar Penuh"
        ) {
            HybridToggleRow(
                label = "Aktifkan Fullscreen Standby Slideshow",
                description = "Menampilkan slide poster/jam analog secara penuh bergantian dengan layar jadwal",
                checked = standby.enabled,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(standbySlideshow = it.standbySlideshow.copy(enabled = v)) }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Durasi Layar Utama Sebelum Slideshow",
                    value = standby.durationMain,
                    min = 15,
                    max = 600,
                    step = 15,
                    unit = "detik",
                    enabled = standby.enabled,
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(standbySlideshow = it.standbySlideshow.copy(durationMain = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Jumlah Putaran Slide",
                    value = standby.repeatCount,
                    min = 1,
                    max = 10,
                    unit = "putaran",
                    enabled = standby.enabled,
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(standbySlideshow = it.standbySlideshow.copy(repeatCount = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HybridSectionCard(
            title = "Card Next Prayer Menjelang Adzan",
            subtitle = "Tampilkan kartu hitung mundur sholat berikutnya di pojok kanan bawah saat standby fullscreen"
        ) {
            HybridToggleRow(
                label = "Tampilkan Card Next Prayer Saat Mendekati Adzan",
                checked = standby.showNextPrayerCard,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(standbySlideshow = it.standbySlideshow.copy(showNextPrayerCard = v)) }
                }
            )
            HybridNumberStepper(
                label = "Batas Waktu Muncul Sebelum Adzan",
                value = standby.nextPrayerThresholdMinutes,
                min = 5,
                max = 120,
                step = 5,
                unit = "menit",
                enabled = standby.showNextPrayerCard,
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(standbySlideshow = it.standbySlideshow.copy(nextPrayerThresholdMinutes = v)) }
                }
            )
        }
    }
}

/**
 * Modular Sub-Tab 3.1: Tema, Layout & Jam (`visualStyle`).
 * Combines inline steppers for Layout/Scale + toggles + Level-3 Side Drawers for 17 Themes & 8 Clocks.
 */
@Composable
fun VisualStyleSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "themes" | "clocks"
    val activeThemeName = remember(settings.themeSettings.currentThemeId) {
        ThemeEngine.BuiltInThemes.firstOrNull { it.id == settings.themeSettings.currentThemeId }?.name
            ?: settings.themeSettings.currentThemeId
    }
    val layout = settings.layoutSettings

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Tema Visual & Gaya Jam Digital",
            subtitle = "Pilih baris di bawah (OK / ▶) untuk membuka galeri pratinjau langsung"
        ) {
            HybridNavigationRow(
                label = "Tema Warna Display (17 Tema)",
                description = "Palet islami kontras tinggi dengan pratinjau warna langsung",
                valueSummary = activeThemeName,
                icon = Icons.Default.Palette,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "themes" }
            )
            HybridNavigationRow(
                label = "Gaya Jam Header & Fullscreen (8 Gaya)",
                description = "Header: ${settings.brand.clockStyle.uppercase()}  •  Fullscreen: ${settings.brand.fullscreenClockStyle.uppercase()}",
                valueSummary = settings.brand.clockStyle.uppercase(),
                icon = Icons.Default.AccessTime,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "clocks" }
            )
            HybridToggleRow(
                label = "Rotasi Tema Otomatis 24 Jam",
                description = "Berganti tema secara berkala tanpa pengulangan",
                checked = settings.themeSettings.autoThemeEnabled,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(themeSettings = it.themeSettings.copy(autoThemeEnabled = v)) }
                }
            )
            HybridToggleRow(
                label = "Mode Performa Ringan (STB / Android TV)",
                description = "Mengurangi efek blur berat untuk stabilitas operasi 24/7",
                checked = settings.performanceMode,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(performanceMode = v) }
                }
            )
        }

        HybridSectionCard(
            title = "Tata Letak, Skala 1080p & Carousel Jadwal",
            subtitle = "Tekan ◀ ▶ pada baris stepper untuk mengubah tata letak atau skala seketika"
        ) {
            HybridInlineOptionStepper(
                label = "Posisi Tata Letak Jadwal Sholat",
                description = "Pilih letak panel jadwal sholat pada layar utama",
                options = listOf(
                    LayoutPosition.HORIZONTAL.serialName to "Horizontal (Bawah)",
                    LayoutPosition.VERTICAL_LEFT.serialName to "Vertikal Kiri",
                    LayoutPosition.VERTICAL_RIGHT.serialName to "Vertikal Kanan"
                ),
                selectedId = layout.position.serialName,
                icon = Icons.Default.ViewQuilt,
                onSelect = { sel ->
                    LayoutPosition.entries.firstOrNull { it.serialName == sel }?.let { pos ->
                        actions.onUpdateSettings {
                            it.copy(layoutSettings = it.layoutSettings.copy(position = pos, autoRotateLayout = false))
                        }
                    }
                }
            )
            HybridInlineOptionStepper(
                label = "Mode Skala Kanvas 1080p",
                description = "Menyesuaikan rasio gambar terhadap layar TV / monitor",
                options = ScaleMode.entries.map { it.serialName to it.serialName.uppercase() },
                selectedId = layout.scaleMode.serialName,
                icon = Icons.Default.AspectRatio,
                onSelect = { sel ->
                    ScaleMode.entries.firstOrNull { it.serialName == sel }?.let { mode ->
                        actions.onUpdateSettings { it.copy(layoutSettings = it.layoutSettings.copy(scaleMode = mode)) }
                    }
                }
            )
            HybridToggleRow(
                label = "Rotasi Layout Otomatis Setiap 4 Jam (Anti Burn-in)",
                description = "Berganti otomatis antara Horizontal, Vertikal Kiri & Vertikal Kanan",
                checked = layout.autoRotateLayout,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(layoutSettings = it.layoutSettings.copy(autoRotateLayout = v)) }
                }
            )
            HybridNumberStepper(
                label = "Interval Geser Carousel Jadwal Sholat",
                value = settings.prayerSettings.prayerTimeScrollInterval,
                min = 2,
                max = 30,
                unit = "detik",
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(prayerSettings = it.prayerSettings.copy(prayerTimeScrollInterval = v)) }
                }
            )
        }
    }

    when (activeDrawer) {
        "themes" -> {
            HybridSideDrawer(
                title = "Galeri 17 Tema Warna",
                subtitle = "Klik atau tekan OK pada kartu tema untuk menerapkan langsung",
                accentColor = Color(0xFFC084FC),
                onDismiss = { activeDrawer = null }
            ) {
                ThemeSettingsTab(settings, actions)
            }
        }
        "clocks" -> {
            HybridSideDrawer(
                title = "Galeri 8 Gaya Jam Digital",
                subtitle = "Pratinjau langsung jam header & jam layar penuh",
                accentColor = Color(0xFFC084FC),
                onDismiss = { activeDrawer = null }
            ) {
                ClockStyleSettingsTab(settings, actions)
            }
        }
    }
}

/**
 * Modular Sub-Tab 3.2: Overlay & Info/PHBI (`overlayInfo`).
 * Replaces nested sub-tabs with clean navigation rows that open Level-3 Side Drawers.
 */
@Composable
fun OverlayAndInfoSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    var activeDrawer by remember { mutableStateOf<String?>(null) } // "overlay" | "info" | "phbi"
    val infoCfg = settings.infoSettings

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Pengaturan Cepat Puasa Sunnah & Agenda PHBI",
            subtitle = "Pengingat puasa sunnah dan rentang hitung mundur Hari Besar Islam"
        ) {
            HybridToggleRow(
                label = "Info Puasa Sunnah Otomatis",
                description = "Senin-Kamis, Ayyamul Bidh, Syawal, Arafah, Tasu'a & Asyura",
                checked = infoCfg.enableSunnahFasting,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(infoSettings = it.infoSettings.copy(enableSunnahFasting = v)) }
                }
            )
            HybridNumberStepper(
                label = "Rentang Deteksi Agenda PHBI",
                value = infoCfg.lookaheadDays,
                min = 7,
                max = 365,
                step = 5,
                unit = "hari",
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(infoSettings = it.infoSettings.copy(lookaheadDays = v)) }
                }
            )
        }

        HybridSectionCard(
            title = "Kelola Konten Overlay, Kartu Info & Agenda PHBI",
            subtitle = "Pilih baris di bawah (OK / ▶) untuk menambah, mengubah, atau menghapus item"
        ) {
            HybridNavigationRow(
                label = "Floating Overlay Statis",
                description = "Pesan singkat statis yang dipanggil oleh masing-masing Group Slideshow",
                valueSummary = "${settings.floatingOverlays.size} Overlay",
                icon = Icons.Default.TextFields,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "overlay" }
            )
            HybridNavigationRow(
                label = "Kartu Informasi & Kegiatan Masjid",
                description = "Tampil bergantian pada kartu INFO & KEGIATAN MASJID di layar utama",
                valueSummary = "${settings.mosqueInfoList.size} Kartu",
                icon = Icons.Default.Info,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "info" }
            )
            HybridNavigationRow(
                label = "Daftar Agenda Islami & PHBI",
                description = "Hitung mundur otomatis kalender Hijriyah, Masehi & agenda manual",
                valueSummary = "${infoCfg.islamicEvents.size} Agenda",
                icon = Icons.Default.CalendarMonth,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "phbi" }
            )
            HybridNavigationRow(
                label = "Teks Hadits & Pesan Layar",
                description = "Ubah atau kosongkan teks Hadits (Adzan & Iqomah) serta kalimat pengingat layar Jum'at, Tarawih, Hari Raya, Qurban, dan QR",
                valueSummary = "11 Slot Teks",
                icon = Icons.Default.TextFields,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { activeDrawer = "screenTexts" }
            )
        }
    }

    when (activeDrawer) {
        "overlay" -> {
            HybridSideDrawer(
                title = "Kelola Floating Overlay Statis",
                subtitle = "${settings.floatingOverlays.size} item tersimpan",
                accentColor = Color(0xFFC084FC),
                onDismiss = { activeDrawer = null }
            ) {
                OverlayTextSettingsTab(settings, actions)
            }
        }
        "info", "phbi" -> {
            HybridSideDrawer(
                title = if (activeDrawer == "info") "Kartu Informasi Masjid" else "Agenda Islami & PHBI",
                subtitle = "Kelola daftar informasi & hitung mundur Hari Besar Islam",
                accentColor = Color(0xFFC084FC),
                onDismiss = { activeDrawer = null }
            ) {
                InfoAndPhbiSettingsTab(settings, actions)
            }
        }
        "screenTexts" -> {
            HybridSideDrawer(
                title = "Teks Hadits & Pesan Layar",
                subtitle = "Kosongkan kotak bila ingin menyembunyikan teks/hadits pada layar terkait",
                accentColor = Color(0xFFC084FC),
                onDismiss = { activeDrawer = null },
                headerAction = {
                    HybridButton(
                        label = "Reset Default",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            actions.onUpdateSettings { it.copy(screenTexts = ScreenTextsSettings()) }
                        }
                    )
                }
            ) {
                ScreenTextsSettingsEditor(settings, actions)
            }
        }
    }
}

@Composable
fun ScreenTextsSettingsEditor(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val st = settings.screenTexts
    fun update(transform: (ScreenTextsSettings) -> ScreenTextsSettings) {
        actions.onUpdateSettings { prev -> prev.copy(screenTexts = transform(prev.screenTexts)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridInfoBanner(
            text = "Seluruh 11 teks Hadits dan kalimat himbauan layar di bawah dapat diubah bebas (atau dikosongkan untuk menyembunyikan). Dapat diedit juga melalui Remote PWA di HP/Laptop.",
            accentColor = TvFocusCyan
        )

        HybridSectionCard(
            title = "1. Layar Adzan & Menunggu Iqomah",
            subtitle = "Menggantikan teks Hadits (Bukhari, Abu Dawud, Tirmidzi) & peringatan larangan sholat sunnah"
        ) {
            HybridTextInput(
                label = "Teks Hadits / Pesan Layar Adzan",
                value = st.adzanHadith,
                singleLine = false,
                placeholder = "Kosongkan untuk menyembunyikan kotak hadits pada layar Adzan",
                onValueChange = { v -> update { it.copy(adzanHadith = v) } }
            )
            HybridTextInput(
                label = "Teks Hadits Layar Menunggu Iqomah (> 2 Menit)",
                value = st.iqomahHadith,
                singleLine = false,
                placeholder = "Kosongkan untuk menyembunyikan kotak hadits Iqomah",
                onValueChange = { v -> update { it.copy(iqomahHadith = v) } }
            )
            HybridTextInput(
                label = "Peringatan Merah Menjelang Iqomah (≤ 2 Menit)",
                value = st.iqomahWarning,
                singleLine = false,
                placeholder = "Iqomah segera tiba... HARAP TIDAK MEMULAI SHOLAT SUNNAT.",
                onValueChange = { v -> update { it.copy(iqomahWarning = v) } }
            )
        }

        HybridSectionCard(
            title = "2. Layar Waktu Jum'at & Tarawih Ramadhan",
            subtitle = "Kalimat pengingat di bagian bawah layar Waktu Jum'at dan layar Tarawih"
        ) {
            HybridTextInput(
                label = "Kalimat Pengingat Layar Waktu Jum'at",
                value = st.waktuJumatReminder,
                singleLine = false,
                onValueChange = { v -> update { it.copy(waktuJumatReminder = v) } }
            )
            HybridTextInput(
                label = "Kalimat Pengingat Layar Tarawih",
                value = st.tarawihReminder,
                singleLine = false,
                onValueChange = { v -> update { it.copy(tarawihReminder = v) } }
            )
        }

        HybridSectionCard(
            title = "3. Layar Hari Raya & Info Qurban",
            subtitle = "Kalimat pengingat di bagian bawah layar Idul Fitri, Idul Adha, dan Info Qurban"
        ) {
            HybridTextInput(
                label = "Kalimat Pengingat Layar Idul Fitri",
                value = st.idulFitriReminder,
                singleLine = false,
                onValueChange = { v -> update { it.copy(idulFitriReminder = v) } }
            )
            HybridTextInput(
                label = "Kalimat Pengingat Layar Idul Adha",
                value = st.idulAdhaReminder,
                singleLine = false,
                onValueChange = { v -> update { it.copy(idulAdhaReminder = v) } }
            )
            HybridTextInput(
                label = "Kalimat Pengingat Layar Info Qurban",
                value = st.infoQurbanReminder,
                singleLine = false,
                onValueChange = { v -> update { it.copy(infoQurbanReminder = v) } }
            )
        }

        HybridSectionCard(
            title = "4. Layar QR Infaq & Keuangan Online",
            subtitle = "Himbauan scan QR & kutipan transparansi kas (mendukung {{Nama Masjid}})"
        ) {
            HybridTextInput(
                label = "Himbauan Layar QR Infaq Masjid",
                value = st.masjidQrGuidance,
                singleLine = false,
                onValueChange = { v -> update { it.copy(masjidQrGuidance = v) } }
            )
            HybridTextInput(
                label = "Himbauan Layar QR Keuangan Online",
                value = st.financeQrGuidance,
                singleLine = false,
                onValueChange = { v -> update { it.copy(financeQrGuidance = v) } }
            )
            HybridTextInput(
                label = "Kutipan Footer Layar Keuangan Online",
                value = st.onlineFinanceFooterQuote,
                singleLine = false,
                onValueChange = { v -> update { it.copy(onlineFinanceFooterQuote = v) } }
            )
        }
    }
}

/**
 * Modular Sub-Tab 3.4: Media & Standby (`mediaStandby`).
 * Shows Fullscreen Standby Mode controls inline + Level-3 Side Drawer for full Media & Ibadah Backgrounds.
 */
@Composable
fun MediaAndStandbySettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    var showMediaDrawer by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HybridSectionCard(
            title = "Galeri Media Ibadah, Tartil/Tarhim & Paket Gambar",
            subtitle = "Kelola slide Tartil/Tarhim, Do'a Adzan, 4 latar Khutbah, dan 5 latar Pelaksanaan Sholat"
        ) {
            HybridNavigationRow(
                label = "Buka Galeri Media & Latar Ibadah",
                description = "Unggah gambar/video 1080p per layar ibadah atau unduh paket gambar 1-klik",
                valueSummary = "Kelola Slot Media",
                icon = Icons.Default.Image,
                accentBadgeColor = Color(0xFFC084FC),
                onClick = { showMediaDrawer = true }
            )
        }

        StandbyModeSettingsTab(settings, actions)
    }

    if (showMediaDrawer) {
        HybridSideDrawer(
            title = "Galeri Media & Latar Ibadah",
            subtitle = "Paket gambar 1-klik, slide Tartil/Tarhim, dan latar layar ibadah",
            accentColor = Color(0xFFC084FC),
            onDismiss = { showMediaDrawer = false }
        ) {
            MediaGallerySettingsTab(settings, actions)
        }
    }
}
