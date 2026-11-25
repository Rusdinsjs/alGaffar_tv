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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
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
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.domain.model.InfoDisplaySettings
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.ScaleMode
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
                                color = if (isFocused) TvFocusAmber else if (isSelected) theme.accent else theme.border,
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
                                color = if (isFocused) TvFocusAmber else if (isSelected) palette.accent else palette.border,
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
                                    color = if (isFocused) TvFocusAmber else if (isSelected) palette.accent else palette.border,
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
    var newText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Daftar Pesan Overlay / Running Text (${settings.overlayTextList.size} Pesan)",
            subtitle = "Pesan pengumuman yang tampil bergantian di pojok kiri bawah slide utama"
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HybridTextInput(
                    label = "Tambah Pesan Pengumuman Baru",
                    value = newText,
                    placeholder = "Mohon luruskan dan rapatkan shaf...",
                    onValueChange = { newText = it },
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "+ Tambah Pesan",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = {
                        if (newText.isNotBlank()) {
                            actions.onUpdateSettings { prev ->
                                prev.copy(overlayTextList = prev.overlayTextList + newText.trim())
                            }
                            newText = ""
                        }
                    }
                )
            }

            settings.overlayTextList.forEachIndexed { idx, msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.8f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HybridTextInput(
                        label = "Pesan #${idx + 1}",
                        value = msg,
                        onValueChange = { updated ->
                            actions.onUpdateSettings { prev ->
                                val list = prev.overlayTextList.toMutableList()
                                list[idx] = updated
                                prev.copy(overlayTextList = list)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HybridButton(
                        label = "Hapus",
                        icon = Icons.Default.Delete,
                        isDanger = true,
                        onClick = {
                            actions.onUpdateSettings { prev ->
                                prev.copy(overlayTextList = prev.overlayTextList.filterIndexed { i, _ -> i != idx })
                            }
                        }
                    )
                }
            }
        }

        HybridSectionCard(
            title = "Posisi, Siklus Tampilan & Kecepatan Overlay",
            subtitle = "Pilih posisi melayang, jeda bersih awal slide, dan durasi tampil/sembunyi"
        ) {
            HybridSegmentedRow(
                options = listOf(
                    "bottom" to "Bawah (Kiri-Bawah)",
                    "top" to "Atas (Kiri-Atas)"
                ),
                selectedId = settings.overlayPosition,
                onSelect = { pos -> actions.onUpdateSettings { it.copy(overlayPosition = pos) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Durasi Tampil",
                    value = settings.overlayVisibleDuration,
                    min = 5,
                    max = 300,
                    step = 5,
                    unit = "detik",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(overlayVisibleDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Durasi Sembunyi",
                    value = settings.overlayHiddenDuration,
                    min = 0,
                    max = 300,
                    step = 5,
                    unit = "detik",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(overlayHiddenDuration = v) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Jeda Bersih Awal Slide",
                    value = settings.overlaySlideClearSeconds,
                    min = 0,
                    max = 30,
                    step = 1,
                    unit = "detik",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(overlaySlideClearSeconds = v) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Kecepatan Pergantian & Marquee",
                    value = settings.overlaySpeed,
                    min = 20,
                    max = 150,
                    step = 10,
                    unit = "lvl",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(overlaySpeed = v) } },
                    modifier = Modifier.weight(1f)
                )
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
