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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
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
import id.algaffar.jwstv.domain.model.FloatingOverlayItem
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideTextStyle
import id.algaffar.jwstv.domain.model.SlideshowGroup
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.util.Locale

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

private fun formatDurationSeconds(ms: Long): String {
    val sec = ms / 1000.0
    return if (ms % 1000L == 0L) {
        "${ms / 1000L} dtk"
    } else {
        String.format(Locale.US, "%.1f dtk", sec)
    }
}

/**
 * Group-based Slideshow Info Settings Tab (`slides`).
 * Supports creating multiple named Slideshow Groups (`Group 1`, `Group 2`, ...), where each group:
 * - Contains 1 or more Content/System slides with a live Total Slide Duration badge
 * - Selects 1 or more Floating Overlays by Title (displayed statically with equal duration = Total / Count)
 * - Configures Floating Overlay corner position (Kiri Atas / Kiri Bawah / Kanan Atas / Kanan Bawah)
 * - Toggles Card QR Infaq & QR Kas Online (rotating automatically on the horizontally opposite corner)
 * - Selects 1 or more target states (`MainSlideContainer`, `Slide Fullscreen`, `Jum'at`, `Ramadhan`, `Idul Fitri`, `Idul Adha`)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlideSettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val slideConfig = settings.slideConfig
    val groups = settings.slideGroups

    var expandedGroupId by remember { mutableStateOf<String?>(groups.firstOrNull()?.id) }

    fun saveGroups(updatedGroups: List<SlideshowGroup>) {
        actions.onUpdateSettings { prev ->
            prev.copy(
                slideGroups = updatedGroups,
                slides = updatedGroups.flatMap { it.slides }
            )
        }
    }

    fun updateGroupAt(index: Int, updated: SlideshowGroup) {
        if (index !in groups.indices) return
        val list = groups.toMutableList()
        list[index] = updated
        saveGroups(list)
    }

    fun addNewGroup() {
        val nextName = SlideshowGroup.nextDefaultGroupName(groups)
        val newId = "group_${System.currentTimeMillis()}"
        val newGroup = SlideshowGroup(
            id = newId,
            name = nextName,
            enabled = true,
            slides = emptyList(),
            overlayIds = emptyList(),
            overlayPosition = SlideshowGroup.POS_BOTTOM_LEFT,
            showQrCard = true,
            showInMain = true,
            showInSlideshow = true,
            showInJumat = false,
            showInRamadhan = false,
            showInIdulFitri = false,
            showInIdulAdha = false
        )
        saveGroups(groups + newGroup)
        expandedGroupId = newId
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ==================== 1. PENGATURAN UMUM SLIDESHOW ====================
        HybridSectionCard(
            title = "Pengaturan Umum Slideshow",
            subtitle = "Atur auto-play, durasi default slide baru, dan batas resolusi video",
            headerAction = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = "Reset Default",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            val defaults = SlideshowGroup.defaultGroups()
                            expandedGroupId = defaults.firstOrNull()?.id
                            saveGroups(defaults)
                        }
                    )
                    HybridButton(
                        label = "+ Tambah Group",
                        icon = Icons.Default.Add,
                        isPrimary = true,
                        onClick = { addNewGroup() }
                    )
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HybridToggleRow(
                    label = "Auto-Play Slideshow",
                    description = "Putar group & slide bergantian otomatis sesuai durasi",
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

        // ==================== 2. DAFTAR GROUP SLIDESHOW ====================
        HybridSectionCard(
            title = "Group Slideshow (${groups.size} Group)",
            subtitle = "Setiap Group berisi 1+ slide, badge total durasi otomatis, pemilihan Judul Floating Overlay statis, posisi sudut, toggle Card QR berlawanan, dan pilihan State tayang"
        ) {
            if (groups.isEmpty()) {
                HybridInfoBanner(
                    text = "Belum ada Group Slideshow. Tekan '+ Tambah Group' di atas untuk membuat Group 1 atau tekan 'Reset Default'.",
                    accentColor = Color(0xFF38BDF8)
                )
            } else {
                groups.forEachIndexed { idx, group ->
                    val isExpanded = expandedGroupId == group.id
                    SlideshowGroupAccordionCard(
                        groupIndex = idx,
                        totalGroups = groups.size,
                        group = group,
                        allOverlays = settings.floatingOverlays,
                        defaultSlideDuration = slideConfig.defaultDuration.coerceIn(4, 120),
                        isExpanded = isExpanded,
                        assetsRootDir = actions.assetsRootDir,
                        actions = actions,
                        onToggleExpand = {
                            expandedGroupId = if (isExpanded) null else group.id
                        },
                        onMoveGroup = { delta ->
                            val target = idx + delta
                            if (target in groups.indices) {
                                val list = groups.toMutableList()
                                val moved = list.removeAt(idx)
                                list.add(target, moved)
                                saveGroups(list)
                            }
                        },
                        onUpdateGroup = { updated -> updateGroupAt(idx, updated) },
                        onDeleteGroup = {
                            if (expandedGroupId == group.id) expandedGroupId = null
                            saveGroups(groups.filterIndexed { i, _ -> i != idx })
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlideshowGroupAccordionCard(
    groupIndex: Int,
    totalGroups: Int,
    group: SlideshowGroup,
    allOverlays: List<FloatingOverlayItem>,
    defaultSlideDuration: Int,
    isExpanded: Boolean,
    assetsRootDir: File?,
    actions: SettingsActions,
    onToggleExpand: () -> Unit,
    onMoveGroup: (Int) -> Unit,
    onUpdateGroup: (SlideshowGroup) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var expandedSlideId by remember(group.id) { mutableStateOf<String?>(null) }

    val totalDurationSec = group.totalDurationSeconds
    val activeOverlays = remember(group.overlayIds, allOverlays) {
        group.resolveActiveOverlays(allOverlays)
    }
    val overlayPosLabel = SlideshowGroup.cornerPositionLabel(group.normalizedOverlayPosition)
    val qrPosLabel = SlideshowGroup.cornerPositionLabel(group.qrCardPosition)

    val stateBadges = remember(group) {
        buildList {
            if (group.showInMain) add("MainSlideContainer")
            if (group.showInSlideshow) add("Slide Fullscreen")
            if (group.showInJumat) add("Jum'at")
            if (group.showInRamadhan) add("Ramadhan")
            if (group.showInIdulFitri) add("Idul Fitri")
            if (group.showInIdulAdha) add("Idul Adha")
        }
    }

    fun addSlideToGroup(type: String) {
        val meta = slideMetaFor(type)
        val newId = "slide_${System.currentTimeMillis()}_$type"
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
            duration = if (type == "analog_clock") 20 else defaultSlideDuration,
            enabled = true,
            textStyle = SlideTextStyle()
        )
        onUpdateGroup(group.copy(slides = group.slides + newSlide))
        if (type == "text" || type == "welcome") {
            expandedSlideId = newId
        }
    }

    fun updateSlideInGroup(slideIdx: Int, updated: SlideItem) {
        if (slideIdx !in group.slides.indices) return
        val list = group.slides.toMutableList()
        list[slideIdx] = updated
        onUpdateGroup(group.copy(slides = list))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isExpanded) palette.bgPrimary else palette.bgPrimary.copy(alpha = 0.80f))
            .border(
                width = if (isExpanded) 2.dp else 1.5.dp,
                color = if (isExpanded) palette.accent else palette.border,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        // ==================== GROUP SUMMARY HEADER ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.accent.copy(alpha = 0.18f))
                    .border(1.5.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${groupIndex + 1}",
                        color = palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = group.name.ifBlank { "Group ${groupIndex + 1}" },
                        color = if (group.enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.55f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Prominent Total Slide Duration Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Total: $totalDurationSec dtk",
                            color = Color(0xFF6EE7B7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                val overlaySummary = if (activeOverlays.isEmpty()) {
                    "Overlay: Tidak dipilih"
                } else {
                    val perDur = formatDurationSeconds(group.overlayDurationMillis(activeOverlays.size))
                    "Overlay: ${activeOverlays.size} ($perDur/overlay • $overlayPosLabel)"
                }
                val qrSummary = if (group.showQrCard) "QR: Tampil ($qrPosLabel)" else "QR: Sembunyi"
                val stateSummary = if (stateBadges.isEmpty()) "Belum ada state dipilih" else stateBadges.joinToString(", ")

                Text(
                    text = "${group.slides.size} Slide • $overlaySummary • $qrSummary",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "State: $stateSummary",
                    color = palette.accentSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (groupIndex > 0) {
                    HybridButton(label = "", icon = Icons.Default.ArrowUpward, onClick = { onMoveGroup(-1) })
                }
                if (groupIndex < totalGroups - 1) {
                    HybridButton(label = "", icon = Icons.Default.ArrowDownward, onClick = { onMoveGroup(1) })
                }
                HybridButton(
                    label = if (group.enabled) "ON" else "OFF",
                    isPrimary = group.enabled,
                    onClick = { onUpdateGroup(group.copy(enabled = !group.enabled)) }
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
                    onClick = onDeleteGroup
                )
            }
        }

        // ==================== GROUP EXPANDED EDITOR ====================
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.bgSecondary.copy(alpha = 0.42f))
                    .border(1.dp, palette.border.copy(alpha = 0.6f))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- A. Nama Group & Badge Total Durasi ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    HybridTextInput(
                        label = "Nama Group Slideshow",
                        value = group.name,
                        placeholder = "Group ${groupIndex + 1}",
                        onValueChange = { onUpdateGroup(group.copy(name = it)) },
                        modifier = Modifier.weight(1.3f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF064E3B).copy(alpha = 0.55f))
                            .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "TOTAL DURASI SLIDE AKTIF",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "$totalDurationSec Detik (${group.activeSlides.size}/${group.slides.size} Slide ON)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (activeOverlays.isNotEmpty() && totalDurationSec > 0) {
                            val perOverlayStr = formatDurationSeconds(group.overlayDurationMillis(activeOverlays.size))
                            Text(
                                text = "Durasi/Overlay: $perOverlayStr ($totalDurationSec dtk ÷ ${activeOverlays.size})",
                                color = Color(0xFF6EE7B7),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // --- B. Pilih State Penayangan Group (Multi-Select) ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TAMPILKAN GROUP INI DI STATE (BISA LEBIH DARI 1 STATE)",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SlideModeToggleChip(
                            label = "MainSlideContainer",
                            checked = group.showInMain,
                            accent = Color(0xFF38BDF8),
                            onToggle = { onUpdateGroup(group.copy(showInMain = !group.showInMain)) }
                        )
                        SlideModeToggleChip(
                            label = "Slide Fullscreen",
                            checked = group.showInSlideshow,
                            accent = Color(0xFFC084FC),
                            onToggle = { onUpdateGroup(group.copy(showInSlideshow = !group.showInSlideshow)) }
                        )
                        SlideModeToggleChip(
                            label = "Jum'at",
                            checked = group.showInJumat,
                            accent = Color(0xFF22D3EE),
                            onToggle = { onUpdateGroup(group.copy(showInJumat = !group.showInJumat)) }
                        )
                        SlideModeToggleChip(
                            label = "Ramadhan",
                            checked = group.showInRamadhan,
                            accent = Color(0xFF10B981),
                            onToggle = { onUpdateGroup(group.copy(showInRamadhan = !group.showInRamadhan)) }
                        )
                        SlideModeToggleChip(
                            label = "Idul Fitri",
                            checked = group.showInIdulFitri,
                            accent = Color(0xFF818CF8),
                            onToggle = { onUpdateGroup(group.copy(showInIdulFitri = !group.showInIdulFitri)) }
                        )
                        SlideModeToggleChip(
                            label = "Idul Adha",
                            checked = group.showInIdulAdha,
                            accent = Color(0xFFFBBF24),
                            onToggle = { onUpdateGroup(group.copy(showInIdulAdha = !group.showInIdulAdha)) }
                        )
                    }
                }

                // --- C. Pengaturan Floating Overlay & Card QR Infaq/Kas Online ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.72f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "FLOATING OVERLAY STATIS & CARD QR INFAQ / KAS ONLINE",
                            color = palette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // C1. Multi-select Judul Floating Overlay
                    Text(
                        text = "PILIH JUDUL FLOATING OVERLAY (STATIS • BISA 1 ATAU LEBIH)",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (allOverlays.isEmpty()) {
                        HybridInfoBanner(
                            text = "Belum ada data Floating Overlay. Tambahkan Judul & Pesan terlebih dahulu di menu Tampilan → Overlay Text.",
                            accentColor = Color(0xFFF59E0B)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allOverlays.forEachIndexed { ovIdx, ov ->
                                val isSelected = ov.id in group.overlayIds
                                val orderNum = if (isSelected) group.overlayIds.indexOf(ov.id) + 1 else null
                                val chipLabel = buildString {
                                    if (orderNum != null) append("#$orderNum ")
                                    append(ov.title.ifBlank { "Overlay ${ovIdx + 1}" })
                                }
                                SlideModeToggleChip(
                                    label = chipLabel,
                                    checked = isSelected,
                                    accent = Color(0xFF38BDF8),
                                    onToggle = {
                                        val nextIds = if (isSelected) {
                                            group.overlayIds.filterNot { it == ov.id }
                                        } else {
                                            group.overlayIds + ov.id
                                        }
                                        onUpdateGroup(group.copy(overlayIds = nextIds))
                                    }
                                )
                            }
                        }
                    }

                    // C2. Posisi Floating Overlay (4 Sudut)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "POSISI FLOATING OVERLAY DITAMPILKAN",
                            color = palette.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HybridSegmentedRow(
                            options = listOf(
                                SlideshowGroup.POS_TOP_LEFT to "Kiri Atas",
                                SlideshowGroup.POS_BOTTOM_LEFT to "Kiri Bawah",
                                SlideshowGroup.POS_TOP_RIGHT to "Kanan Atas",
                                SlideshowGroup.POS_BOTTOM_RIGHT to "Kanan Bawah"
                            ),
                            selectedId = group.normalizedOverlayPosition,
                            onSelect = { pos ->
                                onUpdateGroup(group.copy(overlayPosition = pos))
                            }
                        )
                    }

                    // C3. Hide / Unhide Card QR Infaq & QR Kas Online (Berlawanan Kiri/Kanan)
                    HybridToggleRow(
                        label = "Tampilkan Card QR Infaq & QR Kas Online (Bergiliran)",
                        description = if (group.showQrCard) {
                            "Posisi Card QR otomatis di $qrPosLabel (berlawanan kiri/kanan dengan Floating Overlay di $overlayPosLabel)"
                        } else {
                            "Card QR Infaq & QR Kas Online disembunyikan saat group ini tayang"
                        },
                        checked = group.showQrCard,
                        onCheckedChange = { show ->
                            onUpdateGroup(group.copy(showQrCard = show))
                        }
                    )
                }

                // --- D. Tambah Slide ke Dalam Group Ini ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgPrimary.copy(alpha = 0.72f))
                        .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TAMBAH SLIDE KE ${group.name.ifBlank { "GROUP ${groupIndex + 1}" }.uppercase()}",
                        color = palette.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
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
                            onClick = { addSlideToGroup("text") },
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
                                        duration = defaultSlideDuration,
                                        enabled = true
                                    )
                                    onUpdateGroup(group.copy(slides = group.slides + newSlide))
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
                                        enabled = true
                                    )
                                    onUpdateGroup(group.copy(slides = group.slides + newSlide))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "SLIDE SYSTEM OTOMATIS",
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
                                onClick = { addSlideToGroup(meta.type) }
                            )
                        }
                    }
                }

                // --- E. Daftar Slide di Dalam Group Ini ---
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DAFTAR SLIDE DALAM GROUP (${group.slides.size} SLIDE • TOTAL $totalDurationSec DTK)",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (group.slides.isEmpty()) {
                        HybridInfoBanner(
                            text = "Group ini belum memiliki slide. Tambahkan minimal 1 Slide Konten atau Slide System di atas agar group dapat ditayangkan.",
                            accentColor = Color(0xFFF59E0B)
                        )
                    } else {
                        group.slides.forEachIndexed { slideIdx, slide ->
                            val isSlideExpanded = expandedSlideId == slide.id
                            SlideAccordionItem(
                                index = slideIdx,
                                totalCount = group.slides.size,
                                slide = slide,
                                isExpanded = isSlideExpanded,
                                assetsRootDir = assetsRootDir,
                                onToggleExpand = {
                                    expandedSlideId = if (isSlideExpanded) null else slide.id
                                },
                                onMove = { delta ->
                                    val target = slideIdx + delta
                                    if (target in group.slides.indices) {
                                        val list = group.slides.toMutableList()
                                        val moved = list.removeAt(slideIdx)
                                        list.add(target, moved)
                                        onUpdateGroup(group.copy(slides = list))
                                    }
                                },
                                onUpdate = { updated -> updateSlideInGroup(slideIdx, updated) },
                                onDelete = {
                                    if (expandedSlideId == slide.id) expandedSlideId = null
                                    onUpdateGroup(
                                        group.copy(slides = group.slides.filterIndexed { i, _ -> i != slideIdx })
                                    )
                                },
                                onReplaceImage = {
                                    actions.onPickImageAsset("slides") { path ->
                                        updateSlideInGroup(slideIdx, slide.copy(imageUrl = path))
                                    }
                                },
                                onReplaceVideo = {
                                    actions.onPickVideoAsset { path, title ->
                                        updateSlideInGroup(
                                            slideIdx,
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

@Composable
private fun SlideAccordionItem(
    index: Int,
    totalCount: Int,
    slide: SlideItem,
    isExpanded: Boolean,
    assetsRootDir: File?,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isExpanded) palette.bgPrimary else palette.bgPrimary.copy(alpha = 0.82f))
            .border(
                width = if (isExpanded) 2.dp else 1.dp,
                color = if (isExpanded) meta.accent.copy(alpha = 0.8f) else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        // Compact Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (slide.type == "image" && slide.imageUrl.isNotBlank()) {
                MediaPreviewImageBox(
                    rawPath = slide.imageUrl,
                    assetsRootDir = assetsRootDir,
                    contentDescription = displayTitle,
                    emptyLabel = "IMG",
                    modifier = Modifier
                        .width(72.dp)
                        .height(42.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(meta.accent.copy(alpha = 0.18f))
                        .border(1.dp, meta.accent.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = meta.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    text = "Durasi: ${slide.duration} dtk • ${if (slide.enabled) "Aktif" else "Nonaktif"}",
                    color = palette.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    HybridButton(label = "", icon = Icons.Default.ArrowUpward, onClick = { onMove(-1) })
                }
                if (index < totalCount - 1) {
                    HybridButton(label = "", icon = Icons.Default.ArrowDownward, onClick = { onMove(1) })
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
                HybridButton(label = "", icon = Icons.Default.Delete, isDanger = true, onClick = onDelete)
            }
        }

        // Expanded Slide Editor
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.bgSecondary.copy(alpha = 0.35f))
                    .border(1.dp, palette.border.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
