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
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideTextStyle
import id.algaffar.jwstv.ui.theme.LocalJwsTheme

private data class SlideTypeMeta(
    val type: String,
    val label: String,
    val shortBadge: String,
    val accent: Color,
    val icon: ImageVector,
    val systemInfoText: String = ""
)

private val SLIDE_TYPE_CATALOG: List<SlideTypeMeta> = listOf(
    SlideTypeMeta("text", "Slide Teks", "TEKS", Color(0xFF38BDF8), Icons.Default.TextFields),
    SlideTypeMeta("image", "Slide Gambar", "GAMBAR", Color(0xFF10B981), Icons.Default.Image),
    SlideTypeMeta("video", "Slide Video", "VIDEO", Color(0xFFC084FC), Icons.Default.Videocam),
    SlideTypeMeta(
        "welcome", "Sambutan Masjid", "GREETING", Color(0xFFFACC15), Icons.Default.AutoAwesome
    ),
    SlideTypeMeta(
        "analog_clock", "Jam Analog", "JAM ANALOG", Color(0xFFF59E0B), Icons.Default.AccessTime,
        "Menampilkan Jam Analog Besar berornamen emas & hijau zamrud secara real-time."
    ),
    SlideTypeMeta(
        "jumat_petugas", "Petugas Jum'at", "JUM'AT", Color(0xFF22D3EE), Icons.Default.Groups,
        "Otomatis mengambil jadwal Khotib, Imam, dan Muadzin dari menu Acara → Jum'at."
    ),
    SlideTypeMeta(
        "jumat_keuangan", "Kas Infaq Jum'at", "KAS JUM'AT", Color(0xFF22D3EE), Icons.Default.AccountBalanceWallet,
        "Otomatis menampilkan laporan pemasukan, pengeluaran, dan saldo kas dari menu Acara → Jum'at."
    ),
    SlideTypeMeta(
        "ramadhan_petugas", "Petugas Ramadhan", "RAMADHAN", Color(0xFF10B981), Icons.Default.Groups,
        "Otomatis menampilkan Penceramah, Imam Tarawih & Imam Witir malam aktif dari menu Acara → Ramadhan."
    ),
    SlideTypeMeta(
        "ramadhan_keuangan", "Keuangan Ramadhan", "KAS RAMADHAN", Color(0xFF10B981), Icons.Default.AccountBalanceWallet,
        "Otomatis menampilkan penerimaan infaq jamaah pria/wanita dan saldo kas dari menu Acara → Ramadhan."
    ),
    SlideTypeMeta(
        "ramadhan_takjil", "Daftar Ta'jil", "TA'JIL", Color(0xFF10B981), Icons.Default.Restaurant,
        "Otomatis menampilkan daftar penyumbang Ta'jil buka puasa hari aktif dari menu Acara → Ramadhan."
    ),
    SlideTypeMeta(
        "ramadhan_jadwal", "Jadwal Ramadhan", "JADWAL RM", Color(0xFF10B981), Icons.Default.AccessTime,
        "Otomatis menampilkan jadwal Imsak, Subuh, dan Maghrib berbuka puasa hari ini."
    ),
    SlideTypeMeta(
        "ramadhan_laporan_akhir", "Laporan Akhir Ramadhan", "LAP. AKHIR", Color(0xFF10B981), Icons.Default.AccountBalanceWallet,
        "Otomatis menampilkan rekapitulasi akhir kas Ramadhan dari menu Acara → Ramadhan."
    ),
    SlideTypeMeta(
        "idulfitri_petugas", "Petugas Idul Fitri", "IDUL FITRI", Color(0xFF818CF8), Icons.Default.Groups,
        "Otomatis menampilkan Khotib, Imam, dan Bilal dari menu Acara → Idul Fitri."
    ),
    SlideTypeMeta(
        "iduladha_petugas", "Petugas Idul Adha", "IDUL ADHA", Color(0xFFFBBF24), Icons.Default.Groups,
        "Otomatis menampilkan Khotib, Imam, dan Bilal dari menu Acara → Idul Adha & Qurban."
    ),
    SlideTypeMeta(
        "iduladha_qurban", "Daftar Qurban", "QURBAN", Color(0xFFFBBF24), Icons.Default.Book,
        "Otomatis menampilkan kelompok & peserta hewan Qurban dari menu Acara → Idul Adha & Qurban."
    ),
    SlideTypeMeta(
        "online_finance", "Kas Umum (Online)", "KAS ONLINE", Color(0xFF34D399), Icons.AutoMirrored.Filled.TrendingUp,
        "Otomatis menyinkronkan ringkasan kas masjid dari Google Sheets pada menu Masjid → Keuangan Online."
    ),
    SlideTypeMeta(
        "qr_infaq", "QR Code Infaq", "QR INFAQ", Color(0xFF60A5FA), Icons.Default.QrCode2,
        "Menampilkan QRIS / tautan Infaq Digital dari menu Masjid → QR Infaq Digital."
    ),
    SlideTypeMeta(
        "qr_finance", "QR Kas Online", "QR KAS", Color(0xFF60A5FA), Icons.Default.QrCode2,
        "Menampilkan QR Code tautan laporan detail keuangan dari menu Masjid → Keuangan Online."
    )
)

private fun slideMetaFor(type: String): SlideTypeMeta =
    SLIDE_TYPE_CATALOG.firstOrNull { it.type == type }
        ?: SlideTypeMeta(type, type.replace('_', ' '), type.uppercase(), Color(0xFF94A3B8), Icons.Default.TextFields)

private val TEXT_COLOR_PRESETS: List<Pair<String, Color>> = listOf(
    "#FFFFFF" to Color(0xFFFFFFFF),
    "#FDE047" to Color(0xFFFDE047),
    "#34D399" to Color(0xFF34D399),
    "#38BDF8" to Color(0xFF38BDF8),
    "#F472B6" to Color(0xFFF472B6),
    "#FB923C" to Color(0xFFFB923C)
)

private val FONT_SIZE_PRESETS: List<Pair<String, String>> = listOf(
    "lg" to "Kecil",
    "2xl" to "Sedang",
    "3xl" to "Besar",
    "5xl" to "Jumbo"
)

internal fun parseHexColorSafe(hex: String, fallback: Color = Color.White): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color((0xFF000000L or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> fallback
        }
    }.getOrDefault(fallback)
}

internal fun slideFontSizeToSp(sizeKey: String, isSmallPreview: Boolean = false): TextUnit {
    val baseSp = when (sizeKey.lowercase()) {
        "base" -> 22f
        "lg" -> 26f
        "xl" -> 30f
        "2xl" -> 34f
        "3xl" -> 40f
        "4xl" -> 48f
        "5xl" -> 56f
        "6xl" -> 64f
        else -> 36f
    }
    return (if (isSmallPreview) baseSp * 0.55f else baseSp).sp
}

internal fun slideTextAlignOf(align: String): TextAlign = when (align.lowercase()) {
    "left" -> TextAlign.Left
    "right" -> TextAlign.Right
    else -> TextAlign.Center
}

/**
 * Clean, simple, accordion-based Slide Info Settings Tab (`slides`).
 * Feature-parity with `SlideSettingsForm.tsx` in `jws-android-algaffar`:
 * - Global slideshow controls (Auto-Play, Default Duration, Max Video Resolution)
 * - Add Basic Slides (Text, Image, Video) & 15 System Slides
 * - Compact single-row accordion list with thumbnail previews & single-open editor
 * - Mode visibility toggles (Area Utama, Fullscreen, Jum'at, Ramadhan, Idul Fitri, Idul Adha)
 * - Rich Text formatting toolbar + Live Preview for text slides
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlideSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val slideConfig = settings.slideConfig
    var expandedSlideId by remember { mutableStateOf<String?>(null) }

    fun addSystemOrTextSlide(type: String) {
        val meta = slideMetaFor(type)
        val isUniversalDefault = type == "welcome" || type == "analog_clock"
        val newId = "slide_${System.currentTimeMillis()}_${type}"
        val newSlide = SlideItem(
            id = newId,
            title = meta.label,
            content = when (type) {
                "welcome" -> SlideItem.DEFAULT_WELCOME_TEMPLATE
                "analog_clock" -> "Jam Analog Besar"
                "text" -> "Tulis informasi atau pengumuman masjid di sini."
                else -> ""
            },
            type = type,
            duration = if (type == "analog_clock") 20 else slideConfig.defaultDuration.coerceIn(4, 120),
            enabled = true,
            showInMain = true,
            showInSlideshow = true,
            showInJumat = isUniversalDefault || type.startsWith("jumat_"),
            showInRamadhan = isUniversalDefault || type.startsWith("ramadhan_"),
            showInIdulFitri = isUniversalDefault || type == "idulfitri_petugas" || type == "ramadhan_laporan_akhir",
            showInIdulAdha = isUniversalDefault || type.startsWith("iduladha_"),
            textStyle = SlideTextStyle()
        )
        actions.onUpdateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
        if (type == "text" || type == "welcome") {
            expandedSlideId = newId
        }
    }

    fun updateSlideAt(index: Int, updated: SlideItem) {
        actions.onUpdateSettings { prev ->
            if (index !in prev.slides.indices) return@onUpdateSettings prev
            val list = prev.slides.toMutableList()
            list[index] = updated
            prev.copy(slides = list)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ==================== 1. PENGATURAN UMUM SLIDESHOW ====================
        HybridSectionCard(
            title = "Pengaturan Umum Slideshow",
            subtitle = "Atur auto-play, durasi default slide baru, dan batas resolusi video",
            headerAction = {
                HybridButton(
                    label = "Reset Default",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        expandedSlideId = null
                        actions.onUpdateSettings { prev ->
                            prev.copy(slides = SlideItem.defaultSystemSlides())
                        }
                    }
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HybridToggleRow(
                    label = "Auto-Play Slideshow",
                    description = "Putar bergantian otomatis sesuai durasi tiap slide",
                    checked = slideConfig.autoPlay,
                    onCheckedChange = { v ->
                        actions.onUpdateSettings { it.copy(slideConfig = it.slideConfig.copy(autoPlay = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Durasi Default Slide",
                    value = slideConfig.defaultDuration,
                    min = 4,
                    max = 120,
                    unit = "dtk",
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(slideConfig = it.slideConfig.copy(defaultDuration = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "BATAS RESOLUSI VIDEO SLIDE",
                    color = palette.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                HybridSegmentedRow(
                    options = listOf(
                        "720p" to "HD (720p)",
                        "1080p" to "Full HD (1080p)",
                        "4k" to "Ultra HD (4K)",
                        "unlimited" to "Tanpa Batas"
                    ),
                    selectedId = slideConfig.maxVideoResolution,
                    onSelect = { res ->
                        actions.onUpdateSettings { it.copy(slideConfig = it.slideConfig.copy(maxVideoResolution = res)) }
                    }
                )
            }
        }

        // ==================== 2. TAMBAH SLIDE (DASAR & SISTEM) ====================
        HybridSectionCard(
            title = "Tambah Slide Baru",
            subtitle = "Pilih slide konten (Teks / Gambar / Video) atau slide sistem otomatis dari data masjid"
        ) {
            Text(
                text = "SLIDE KONTEN",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HybridButton(
                    label = "+ Slide Teks",
                    icon = Icons.Default.TextFields,
                    isPrimary = true,
                    onClick = { addSystemOrTextSlide("text") },
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "+ Slide Gambar",
                    icon = Icons.Default.Image,
                    isPrimary = true,
                    onClick = {
                        actions.onPickImageAsset("slides") { path ->
                            val newId = "slide_img_${System.currentTimeMillis()}"
                            val fileNameTitle = path.substringAfterLast('/')
                                .substringAfter('_')
                                .substringBeforeLast('.')
                                .replace('_', ' ')
                                .ifBlank { "Poster Informasi" }
                            val newSlide = SlideItem(
                                id = newId,
                                title = fileNameTitle,
                                type = "image",
                                imageUrl = path,
                                duration = slideConfig.defaultDuration.coerceIn(4, 120),
                                enabled = true,
                                showInMain = true,
                                showInSlideshow = true
                            )
                            actions.onUpdateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "+ Slide Video",
                    icon = Icons.Default.Videocam,
                    isPrimary = true,
                    onClick = {
                        actions.onPickVideoAsset { path, title ->
                            val newId = "slide_vid_${System.currentTimeMillis()}"
                            val newSlide = SlideItem(
                                id = newId,
                                title = title.ifBlank { "Video Masjid" },
                                type = "video",
                                videoUrl = path,
                                duration = 30,
                                enabled = true,
                                showInMain = true,
                                showInSlideshow = true
                            )
                            actions.onUpdateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SLIDE SISTEM OTOMATIS",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            val systemTypes = SLIDE_TYPE_CATALOG.filter { it.type !in setOf("text", "image", "video") }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                systemTypes.forEach { meta ->
                    SlideQuickAddChip(
                        meta = meta,
                        onClick = { addSystemOrTextSlide(meta.type) }
                    )
                }
            }
        }

        // ==================== 3. DAFTAR SLIDE (ACCORDION RINGKAS) ====================
        HybridSectionCard(
            title = "Daftar Slide Aktif (${settings.slides.size} Slide)",
            subtitle = "Tekan 'Edit' pada salah satu slide untuk mengatur durasi, konten, format teks, dan mode tayang"
        ) {
            if (settings.slides.isEmpty()) {
                HybridInfoBanner(
                    text = "Belum ada slide di daftar. Tambahkan slide baru di atas atau tekan 'Reset Default' untuk memulihkan Sambutan Masjid & Jam Analog.",
                    accentColor = Color(0xFF38BDF8)
                )
            } else {
                settings.slides.forEachIndexed { idx, slide ->
                    val isExpanded = expandedSlideId == slide.id
                    SlideAccordionItem(
                        index = idx,
                        totalCount = settings.slides.size,
                        slide = slide,
                        isExpanded = isExpanded,
                        assetsRootDir = actions.assetsRootDir,
                        onToggleExpand = {
                            expandedSlideId = if (isExpanded) null else slide.id
                        },
                        onMove = { delta ->
                            actions.onUpdateSettings { prev ->
                                val list = prev.slides.toMutableList()
                                val target = idx + delta
                                if (target in list.indices) {
                                    val moved = list.removeAt(idx)
                                    list.add(target, moved)
                                }
                                prev.copy(slides = list)
                            }
                        },
                        onUpdate = { updated -> updateSlideAt(idx, updated) },
                        onDelete = {
                            if (expandedSlideId == slide.id) expandedSlideId = null
                            actions.onUpdateSettings { prev ->
                                prev.copy(slides = prev.slides.filterIndexed { i, _ -> i != idx })
                            }
                        },
                        onReplaceImage = {
                            actions.onPickImageAsset("slides") { path ->
                                updateSlideAt(idx, slide.copy(imageUrl = path))
                            }
                        },
                        onReplaceVideo = {
                            actions.onPickVideoAsset { path, title ->
                                updateSlideAt(
                                    idx,
                                    slide.copy(
                                        videoUrl = path,
                                        title = slide.title.ifBlank { title }
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideQuickAddChip(
    meta: SlideTypeMeta,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFocused) meta.accent.copy(alpha = 0.30f) else meta.accent.copy(alpha = 0.12f)
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusAmber else meta.accent.copy(alpha = 0.38f),
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = meta.icon,
            contentDescription = null,
            tint = meta.accent,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = "+ ${meta.label}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideAccordionItem(
    index: Int,
    totalCount: Int,
    slide: SlideItem,
    isExpanded: Boolean,
    assetsRootDir: java.io.File?,
    onToggleExpand: () -> Unit,
    onMove: (Int) -> Unit,
    onUpdate: (SlideItem) -> Unit,
    onDelete: () -> Unit,
    onReplaceImage: () -> Unit,
    onReplaceVideo: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val meta = slideMetaFor(slide.type)
    val displayTitle = remember(slide.title, slide.content, slide.type) {
        when {
            slide.type == "text" && slide.title.isBlank() -> slide.content.lineSequence().firstOrNull()?.take(48)?.ifBlank { "Slide Teks Kosong" } ?: "Slide Teks Kosong"
            slide.title.isNotBlank() -> slide.title
            else -> meta.label
        }
    }
    val placementSummary = remember(slide) {
        buildList {
            add("${slide.duration} dtk")
            add(if (slide.enabled) "Aktif" else "Nonaktif")
            if (slide.showInMain) add("Area Utama")
            if (slide.showInSlideshow) add("Fullscreen")
            if (slide.showInJumat) add("Jum'at")
            if (slide.showInRamadhan) add("Ramadhan")
            if (slide.showInIdulFitri) add("Idul Fitri")
            if (slide.showInIdulAdha) add("Idul Adha")
        }.joinToString(" • ")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isExpanded) palette.bgPrimary else palette.bgPrimary.copy(alpha = 0.78f)
            )
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = if (isExpanded) meta.accent.copy(alpha = 0.8f) else palette.border,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // ==================== COMPACT SUMMARY HEADER ROW ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading Thumbnail (image) or Colored Type Icon Badge
            if (slide.type == "image" && slide.imageUrl.isNotBlank()) {
                MediaPreviewImageBox(
                    rawPath = slide.imageUrl,
                    assetsRootDir = assetsRootDir,
                    contentDescription = displayTitle,
                    emptyLabel = "IMG",
                    modifier = Modifier
                        .width(76.dp)
                        .height(44.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(meta.accent.copy(alpha = 0.18f))
                        .border(1.dp, meta.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = meta.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Title + Badge + Compact Metadata Line
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(meta.accent.copy(alpha = 0.18f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = meta.shortBadge,
                            color = meta.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = displayTitle,
                        color = if (slide.enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.55f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = placementSummary,
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Compact Action Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    HybridButton(
                        label = "",
                        icon = Icons.Default.ArrowUpward,
                        onClick = { onMove(-1) }
                    )
                }
                if (index < totalCount - 1) {
                    HybridButton(
                        label = "",
                        icon = Icons.Default.ArrowDownward,
                        onClick = { onMove(1) }
                    )
                }
                HybridButton(
                    label = if (slide.enabled) "ON" else "OFF",
                    isPrimary = slide.enabled,
                    onClick = { onUpdate(slide.copy(enabled = !slide.enabled)) }
                )
                HybridButton(
                    label = if (isExpanded) "Tutup" else "Edit",
                    icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                    isPrimary = isExpanded,
                    onClick = onToggleExpand
                )
                HybridButton(
                    label = "",
                    icon = Icons.Default.Delete,
                    isDanger = true,
                    onClick = onDelete
                )
            }
        }

        // ==================== EXPANDED DETAIL EDITOR ====================
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.bgSecondary.copy(alpha = 0.38f))
                    .border(1.dp, palette.border.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Row 1: Title + Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HybridTextInput(
                        label = "Judul Slide",
                        value = slide.title,
                        placeholder = meta.label,
                        onValueChange = { onUpdate(slide.copy(title = it)) },
                        modifier = Modifier.weight(1.2f)
                    )
                    HybridNumberStepper(
                        label = "Durasi Tampil",
                        value = slide.duration,
                        min = 4,
                        max = 120,
                        unit = "dtk",
                        onValueChange = { onUpdate(slide.copy(duration = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Where to display (Area Utama, Fullscreen, Mode Khusus)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TAMPILKAN DI AREA & MODE",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlideModeToggleChip(
                            label = "Area Utama",
                            checked = slide.showInMain,
                            accent = Color(0xFF38BDF8),
                            onToggle = { onUpdate(slide.copy(showInMain = !slide.showInMain)) }
                        )
                        SlideModeToggleChip(
                            label = "Fullscreen Standby",
                            checked = slide.showInSlideshow,
                            accent = Color(0xFFC084FC),
                            onToggle = { onUpdate(slide.copy(showInSlideshow = !slide.showInSlideshow)) }
                        )
                        if (supportsJumatMode(slide.type)) {
                            SlideModeToggleChip(
                                label = "Mode Jum'at",
                                checked = slide.showInJumat,
                                accent = Color(0xFF22D3EE),
                                onToggle = { onUpdate(slide.copy(showInJumat = !slide.showInJumat)) }
                            )
                        }
                        if (supportsRamadhanMode(slide.type)) {
                            SlideModeToggleChip(
                                label = "Mode Ramadhan",
                                checked = slide.showInRamadhan,
                                accent = Color(0xFF10B981),
                                onToggle = { onUpdate(slide.copy(showInRamadhan = !slide.showInRamadhan)) }
                            )
                        }
                        if (supportsIdulFitriMode(slide.type)) {
                            SlideModeToggleChip(
                                label = "Mode Idul Fitri",
                                checked = slide.showInIdulFitri,
                                accent = Color(0xFF818CF8),
                                onToggle = { onUpdate(slide.copy(showInIdulFitri = !slide.showInIdulFitri)) }
                            )
                        }
                        if (supportsIdulAdhaMode(slide.type)) {
                            SlideModeToggleChip(
                                label = "Mode Idul Adha",
                                checked = slide.showInIdulAdha,
                                accent = Color(0xFFFBBF24),
                                onToggle = { onUpdate(slide.copy(showInIdulAdha = !slide.showInIdulAdha)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SMART OVERLAY & QR DI SLIDE INI",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlideModeToggleChip(
                            label = "Sembunyikan Overlay Text",
                            checked = slide.hideOverlayText,
                            accent = Color(0xFFF97316),
                            onToggle = { onUpdate(slide.copy(hideOverlayText = !slide.hideOverlayText)) }
                        )
                        SlideModeToggleChip(
                            label = "Sembunyikan Card QR Code",
                            checked = slide.hideQrCard,
                            accent = Color(0xFFEC4899),
                            onToggle = { onUpdate(slide.copy(hideQrCard = !slide.hideQrCard)) }
                        )
                    }
                }

                // Row 3: Type-specific Content Editor
                when (slide.type) {
                    "welcome" -> {
                        HybridTextInput(
                            label = "Teks Sambutan (5 baris • dukung {{Nama Masjid}} & {{Alamat Masjid}})",
                            value = slide.content,
                            placeholder = SlideItem.DEFAULT_WELCOME_TEMPLATE,
                            singleLine = false,
                            onValueChange = { onUpdate(slide.copy(content = it)) }
                        )
                        HybridInfoBanner(
                            text = "Tips: Baris 1 = Sapaan, Baris 2 = {{Nama Masjid}}, Baris 3 = {{Alamat Masjid}}, Baris 4 & 5 = Pesan ketertiban masjid.",
                            accentColor = Color(0xFF10B981)
                        )
                    }

                    "text" -> {
                        TextSlideContentEditor(
                            slide = slide,
                            onUpdate = onUpdate
                        )
                    }

                    "image" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MediaPreviewImageBox(
                                rawPath = slide.imageUrl,
                                assetsRootDir = assetsRootDir,
                                contentDescription = displayTitle,
                                modifier = Modifier
                                    .width(240.dp)
                                    .height(135.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (slide.imageUrl.isNotBlank()) {
                                        "File aktif: ${slide.imageUrl.substringAfterLast('/')}"
                                    } else {
                                        "Belum ada file gambar dipilih."
                                    },
                                    color = palette.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                HybridButton(
                                    label = if (slide.imageUrl.isBlank()) "Pilih Gambar" else "Ganti Gambar",
                                    icon = Icons.Default.Upload,
                                    isPrimary = true,
                                    onClick = onReplaceImage
                                )
                                HybridTextInput(
                                    label = "Keterangan Bawah Gambar (Opsional)",
                                    value = slide.content,
                                    placeholder = "Kosongkan jika poster sudah berisi teks lengkap",
                                    onValueChange = { onUpdate(slide.copy(content = it)) }
                                )
                            }
                        }
                    }

                    "video" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HybridInfoBanner(
                                text = "Video Aktif: ${slide.videoUrl.substringAfterLast('/').ifBlank { "(Belum dipilih)" }} — diputar otomatis tanpa suara (loop).",
                                accentColor = Color(0xFFC084FC),
                                modifier = Modifier.weight(1f)
                            )
                            HybridButton(
                                label = "Ganti Video",
                                icon = Icons.Default.Upload,
                                isPrimary = true,
                                onClick = onReplaceVideo
                            )
                        }
                        HybridTextInput(
                            label = "Keterangan Bawah Video (Opsional)",
                            value = slide.content,
                            onValueChange = { onUpdate(slide.copy(content = it)) }
                        )
                    }

                    else -> {
                        HybridInfoBanner(
                            text = meta.systemInfoText.ifBlank {
                                "Slide sistem otomatis ini menampilkan data langsung dari pengaturan terkait."
                            },
                            accentColor = meta.accent
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextSlideContentEditor(
    slide: SlideItem,
    onUpdate: (SlideItem) -> Unit
) {
    val palette = LocalJwsTheme.current
    val style = slide.textStyle

    fun updateStyle(transform: (SlideTextStyle) -> SlideTextStyle) {
        onUpdate(slide.copy(textStyle = transform(style)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HybridTextInput(
            label = "Isi Teks Pengumuman",
            value = slide.content,
            placeholder = "Tulis teks slide pengumuman di sini...",
            singleLine = false,
            onValueChange = { onUpdate(slide.copy(content = it)) }
        )

        // Formatting Toolbar (Bold, Italic, Align, Font Size, Color)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgPrimary.copy(alpha = 0.85f))
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "FORMAT & GAYA TEKS",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HybridButton(
                    label = "Bold",
                    icon = Icons.Default.FormatBold,
                    isPrimary = style.bold,
                    onClick = { updateStyle { it.copy(bold = !it.bold) } }
                )
                HybridButton(
                    label = "Italic",
                    icon = Icons.Default.FormatItalic,
                    isPrimary = style.italic,
                    onClick = { updateStyle { it.copy(italic = !it.italic) } }
                )
                HybridButton(
                    label = "Kiri",
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    isPrimary = style.textAlign == "left",
                    onClick = { updateStyle { it.copy(textAlign = "left") } }
                )
                HybridButton(
                    label = "Tengah",
                    icon = Icons.Default.FormatAlignCenter,
                    isPrimary = style.textAlign == "center",
                    onClick = { updateStyle { it.copy(textAlign = "center") } }
                )
                HybridButton(
                    label = "Kanan",
                    icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                    isPrimary = style.textAlign == "right",
                    onClick = { updateStyle { it.copy(textAlign = "right") } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ukuran:", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    FONT_SIZE_PRESETS.forEach { (key, lbl) ->
                        HybridButton(
                            label = lbl,
                            isPrimary = style.fontSize == key,
                            onClick = { updateStyle { it.copy(fontSize = key) } }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Warna:", color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TEXT_COLOR_PRESETS.forEach { (hex, color) ->
                        val isPicked = style.fontColor.equals(hex, ignoreCase = true)
                        var focused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (focused) 3.dp else if (isPicked) 2.5.dp else 1.dp,
                                    color = if (focused) TvFocusAmber else if (isPicked) Color.White else Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .onFocusChanged { focused = it.isFocused }
                                .focusable()
                                .clickable { updateStyle { it.copy(fontColor = hex) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Preview Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF060D1C))
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "PRATINJAU TAMPILAN SLIDE",
                color = palette.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            if (slide.title.isNotBlank()) {
                Text(
                    text = slide.title,
                    color = palette.accentSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = slideTextAlignOf(style.textAlign),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = slide.content.ifBlank { "Teks pratinjau slide..." },
                color = parseHexColorSafe(style.fontColor, palette.textPrimary),
                fontSize = slideFontSizeToSp(style.fontSize, isSmallPreview = true),
                fontWeight = if (style.bold) FontWeight.ExtraBold else FontWeight.Medium,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                textAlign = slideTextAlignOf(style.textAlign),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SlideModeToggleChip(
    label: String,
    checked: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (checked) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = when {
                    isFocused -> TvFocusAmber
                    checked -> accent
                    else -> Color(0xFF475569)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = if (checked) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun supportsRamadhanMode(type: String): Boolean =
    type in setOf("text", "image", "video", "welcome", "analog_clock", "online_finance", "qr_infaq", "qr_finance") ||
        type.startsWith("ramadhan_")

private fun supportsIdulFitriMode(type: String): Boolean =
    type in setOf("text", "image", "video", "welcome", "analog_clock", "idulfitri_petugas", "ramadhan_laporan_akhir", "online_finance", "qr_infaq", "qr_finance")

private fun supportsIdulAdhaMode(type: String): Boolean =
    type in setOf("text", "image", "video", "welcome", "analog_clock", "iduladha_petugas", "iduladha_qurban", "online_finance", "qr_infaq", "qr_finance")

private fun supportsJumatMode(type: String): Boolean =
    type in setOf("text", "image", "video", "welcome", "analog_clock", "jumat_petugas", "jumat_keuangan", "online_finance", "qr_infaq", "qr_finance")
