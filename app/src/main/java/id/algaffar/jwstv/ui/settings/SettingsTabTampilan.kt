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
            title = "Siklus Tampilan & Kecepatan Overlay",
            subtitle = "Set durasi sembunyi ke 0 detik jika ingin overlay tampil terus-menerus"
        ) {
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
            HybridNumberStepper(
                label = "Kecepatan Pergantian Pesan",
                value = settings.overlaySpeed,
                min = 20,
                max = 150,
                step = 10,
                unit = "lvl",
                onValueChange = { v -> actions.onUpdateSettings { it.copy(overlaySpeed = v) } }
            )
        }
    }
}

@Composable
fun SlideSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current

    fun addSlide(type: String, defaultTitle: String, defaultContent: String = "") {
        val newSlide = SlideItem(
            id = "slide_${System.currentTimeMillis()}",
            title = defaultTitle,
            content = defaultContent,
            type = type,
            duration = 12,
            enabled = true,
            showInMain = true,
            showInSlideshow = true
        )
        actions.onUpdateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Manajemen Slideshow Informasi (${settings.slides.size} Slide Kustom)",
            subtitle = "Tambahkan slide gambar/poster, teks pengumuman, atau Jam Analog Besar ke layar utama & fullscreen"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridButton(
                    label = "+ Slide Teks",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = { addSlide("text", "Pengumuman Masjid", "Isi pesan atau informasi kegiatan masjid di sini.") }
                )
                HybridButton(
                    label = "+ Slide Gambar (Memori / USB)",
                    icon = Icons.Default.Upload,
                    isPrimary = true,
                    onClick = {
                        actions.onPickImageAsset("slides") { path ->
                            val newSlide = SlideItem(
                                id = "slide_${System.currentTimeMillis()}",
                                title = "Poster Informasi",
                                type = "image",
                                imageUrl = path,
                                duration = 12,
                                enabled = true,
                                showInMain = true,
                                showInSlideshow = true
                            )
                            actions.onUpdateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
                        }
                    }
                )
                HybridButton(
                    label = "+ Slide Jam Analog Besar",
                    icon = Icons.Default.Add,
                    onClick = { addSlide("analog_clock", "Jam Analog Besar", "") }
                )
            }

            if (settings.slides.isEmpty()) {
                HybridInfoBanner(
                    text = "Belum ada slide kustom tambahan. Saat ini layar utama menampilkan Slide Sambutan, Petugas Jum'at, dan Kas Jum'at secara otomatis.",
                    accentColor = Color(0xFF38BDF8)
                )
            } else {
                settings.slides.forEachIndexed { idx, slide ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.bgPrimary.copy(alpha = 0.82f))
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1} [${slide.type.uppercase()}] ${slide.title.ifBlank { "Tanpa Judul" }}",
                                color = palette.accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (idx > 0) {
                                    HybridButton(
                                        label = "Naik",
                                        icon = Icons.Default.ArrowUpward,
                                        onClick = {
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.slides.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx - 1, item)
                                                prev.copy(slides = list)
                                            }
                                        }
                                    )
                                }
                                if (idx < settings.slides.lastIndex) {
                                    HybridButton(
                                        label = "Turun",
                                        icon = Icons.Default.ArrowDownward,
                                        onClick = {
                                            actions.onUpdateSettings { prev ->
                                                val list = prev.slides.toMutableList()
                                                val item = list.removeAt(idx)
                                                list.add(idx + 1, item)
                                                prev.copy(slides = list)
                                            }
                                        }
                                    )
                                }
                                HybridButton(
                                    label = if (slide.enabled) "AKTIF" else "NONAKTIF",
                                    isPrimary = slide.enabled,
                                    onClick = {
                                        actions.onUpdateSettings { prev ->
                                            val list = prev.slides.toMutableList()
                                            list[idx] = slide.copy(enabled = !slide.enabled)
                                            prev.copy(slides = list)
                                        }
                                    }
                                )
                                HybridButton(
                                    label = "Hapus",
                                    icon = Icons.Default.Delete,
                                    isDanger = true,
                                    onClick = {
                                        actions.onUpdateSettings { prev ->
                                            prev.copy(slides = prev.slides.filterIndexed { i, _ -> i != idx })
                                        }
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HybridTextInput(
                                label = "Judul Slide",
                                value = slide.title,
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.slides.toMutableList()
                                        list[idx] = slide.copy(title = v)
                                        prev.copy(slides = list)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridNumberStepper(
                                label = "Durasi",
                                value = slide.duration,
                                min = 4,
                                max = 120,
                                unit = "dtk",
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.slides.toMutableList()
                                        list[idx] = slide.copy(duration = v)
                                        prev.copy(slides = list)
                                    }
                                },
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        if (slide.type == "text" || slide.type == "image") {
                            HybridTextInput(
                                label = "Konten / Keterangan Slide",
                                value = slide.content,
                                onValueChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.slides.toMutableList()
                                        list[idx] = slide.copy(content = v)
                                        prev.copy(slides = list)
                                    }
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HybridToggleRow(
                                label = "Area Utama",
                                checked = slide.showInMain,
                                onCheckedChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.slides.toMutableList()
                                        list[idx] = slide.copy(showInMain = v)
                                        prev.copy(slides = list)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            HybridToggleRow(
                                label = "Fullscreen Standby",
                                checked = slide.showInSlideshow,
                                onCheckedChange = { v ->
                                    actions.onUpdateSettings { prev ->
                                        val list = prev.slides.toMutableList()
                                        list[idx] = slide.copy(showInSlideshow = v)
                                        prev.copy(slides = list)
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
                            label = "Nama PHBI / Agenda",
                            value = ev.name,
                            onValueChange = { v ->
                                actions.onUpdateSettings { prev ->
                                    val list = prev.infoSettings.islamicEvents.toMutableList()
                                    list[idx] = ev.copy(name = v)
                                    prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        )
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
                            modifier = Modifier.weight(0.9f)
                        )
                        HybridNumberStepper(
                            label = "Bln Hijriah",
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
                            modifier = Modifier.weight(0.9f)
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
                }
            }
        }
    }
}

@Composable
fun MediaGallerySettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val media = settings.media
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        HybridSectionCard(
            title = "Layar Do'a Setelah Adzan (`doa_adzan`)",
            subtitle = "Tampilkan doa setelah adzan (teks Arab & arti HR. Bukhari atau poster gambar kustom) sebelum hitung mundur Iqomah"
        ) {
            HybridToggleRow(
                label = "Aktifkan Layar Do'a Setelah Adzan",
                description = "Otomatis tampil sesaat setelah Adzan selesai sebelum masuk Menunggu Iqomah",
                checked = media.doaAdzanEnabled,
                onCheckedChange = { v ->
                    actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanEnabled = v)) }
                }
            )
            HybridNumberStepper(
                label = "Durasi Tampil Do'a Setelah Adzan",
                value = media.doaAdzanDuration,
                min = 15,
                max = 120,
                step = 5,
                unit = "detik",
                enabled = media.doaAdzanEnabled,
                onValueChange = { v ->
                    actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanDuration = v)) }
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridButton(
                    label = if (media.doaAdzanImage.isBlank()) "Upload Gambar Do'a Adzan (Opsional)" else "Ganti Gambar Do'a Adzan",
                    icon = Icons.Default.Upload,
                    isPrimary = true,
                    onClick = {
                        actions.onPickImageAsset("media") { path ->
                            actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanImage = path)) }
                        }
                    }
                )
                if (media.doaAdzanImage.isNotBlank()) {
                    HybridButton(
                        label = "Gunakan Teks Bawaan",
                        icon = Icons.Default.Delete,
                        isDanger = true,
                        onClick = {
                            actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanImage = "")) }
                        }
                    )
                }
            }
        }

        HybridSectionCard(
            title = "Interval Slide & Latar Media Ibadah",
            subtitle = "Pengaturan media pendukung Tartil, Tarhim, Khutbah, dan Pelaksanaan Sholat"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridNumberStepper(
                    label = "Interval Slide Tartil",
                    value = media.tartilSlideInterval,
                    min = 3,
                    max = 120,
                    unit = "detik",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(media = it.media.copy(tartilSlideInterval = v)) } },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Interval Slide Tarhim",
                    value = media.tarhimSlideInterval,
                    min = 3,
                    max = 120,
                    unit = "detik",
                    onValueChange = { v -> actions.onUpdateSettings { it.copy(media = it.media.copy(tarhimSlideInterval = v)) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HybridButton(
                    label = "Pilih Latar Khutbah Jum'at",
                    icon = Icons.Default.Upload,
                    onClick = {
                        actions.onPickImageAsset("media") { path ->
                            actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahJumatImage = path)) }
                        }
                    }
                )
                HybridButton(
                    label = "Pilih Latar Ceramah Tarawih",
                    icon = Icons.Default.Upload,
                    onClick = {
                        actions.onPickImageAsset("media") { path ->
                            actions.onUpdateSettings { it.copy(media = it.media.copy(ceramahTarawihImage = path)) }
                        }
                    }
                )
                HybridButton(
                    label = "Pilih Latar Sholat 5 Waktu",
                    icon = Icons.Default.Upload,
                    onClick = {
                        actions.onPickImageAsset("media") { path ->
                            actions.onUpdateSettings { it.copy(media = it.media.copy(sholat5WaktuImage = path)) }
                        }
                    }
                )
            }
        }
    }
}

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
