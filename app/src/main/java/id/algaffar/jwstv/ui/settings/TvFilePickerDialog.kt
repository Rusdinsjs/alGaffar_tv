package id.algaffar.jwstv.ui.settings

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.util.Locale

data class TvFilePickerRequest(
    val title: String,
    val subtitle: String,
    val allowedExtensions: Set<String>,
    val mimeTypes: Array<String>,
    val onFileSelected: (File) -> Unit = {},
    val onOpenSystemPicker: (() -> Unit)? = null,
    val pickDirectoryMode: Boolean = false,
    val onDirectorySelected: ((File) -> Unit)? = null
)

data class StorageRootEntry(
    val label: String,
    val dir: File
)

/**
 * Discovers accessible internal folders, mounted USB OTG flash drives / SD cards,
 * and the app's own `jws-assets` directory so Android TV / STB devices without DocumentsUI
 * can browse and import logos, audio, video, slides, Excel, and ZIP backups effortlessly.
 */
fun discoverTvStorageRoots(context: Context, assetsRootDir: File?): List<StorageRootEntry> {
    val roots = mutableListOf<StorageRootEntry>()
    val seenPaths = mutableSetOf<String>()

    fun tryAdd(label: String, dir: File?) {
        if (dir == null) return
        val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
        if (dir.exists() && dir.isDirectory && seenPaths.add(canonical)) {
            roots.add(StorageRootEntry(label, dir))
        }
    }

    val extStorage = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
    if (extStorage != null && extStorage.exists()) {
        tryAdd("Download", File(extStorage, "Download"))
        tryAdd("Pictures", File(extStorage, "Pictures"))
        tryAdd("Music", File(extStorage, "Music"))
        tryAdd("Movies", File(extStorage, "Movies"))
        tryAdd("Documents", File(extStorage, "Documents"))
        tryAdd("Memori Internal", extStorage)
    }

    // Scan mounted USB OTG / SD cards under /storage and /mnt
    listOf(File("/storage"), File("/mnt/media_rw"), File("/mnt/usb"), File("/mnt")).forEach { mountBase ->
        runCatching {
            mountBase.listFiles()?.forEach { child ->
                val name = child.name.lowercase(Locale.US)
                if (child.isDirectory &&
                    name != "emulated" &&
                    name != "self" &&
                    name != "runtime" &&
                    name != "secure" &&
                    name != "asec" &&
                    name != "obb" &&
                    name != "vendor" &&
                    name != "media_rw" &&
                    child.canRead()
                ) {
                    tryAdd("USB / SD (${child.name})", child)
                }
            }
        }
    }

    // Check getExternalFilesDirs for secondary volumes (Android TV USB mounts)
    runCatching {
        context.getExternalFilesDirs(null).drop(1).forEachIndexed { idx, appExtDir ->
            if (appExtDir != null) {
                // Walk up from /storage/XXXX-XXXX/Android/data/<pkg>/files -> /storage/XXXX-XXXX
                val volRoot = appExtDir.parentFile?.parentFile?.parentFile?.parentFile
                if (volRoot != null && volRoot.exists() && volRoot.canRead()) {
                    tryAdd("USB Drive #${idx + 1} (${volRoot.name})", volRoot)
                } else {
                    tryAdd("App USB #${idx + 1}", appExtDir)
                }
            }
        }
    }

    // App's own assets & external downloads folder
    tryAdd("Folder JWS Assets", assetsRootDir)
    runCatching { context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) }
        .getOrNull()
        ?.let { tryAdd("JWS Downloads", it) }

    return roots
}

/**
 * Built-in D-Pad & Touch File Explorer Overlay for Android TV / STB / Tablet.
 * Prevents "Tidak ada aplikasi yang dapat menangani tindakan ini" on devices lacking DocumentsUI.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvFilePickerDialog(
    request: TvFilePickerRequest,
    assetsRootDir: File?,
    remoteServerUrl: String,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val storageRoots = remember(refreshTick, hasStoragePermission, assetsRootDir) {
        discoverTvStorageRoots(context, assetsRootDir)
    }

    var currentDir by remember(request) {
        val preferred = when {
            request.pickDirectoryMode ->
                storageRoots.firstOrNull { it.label.startsWith("USB") }
                    ?: storageRoots.firstOrNull { it.label == "Download" }
            request.allowedExtensions.any { it in setOf("mp4", "mkv", "webm", "mov", "m4v") } ->
                storageRoots.firstOrNull { it.label.startsWith("USB") || it.label == "Movies" || it.label == "Download" }
            request.allowedExtensions.any { it in setOf("png", "jpg", "jpeg", "webp") } ->
                storageRoots.firstOrNull { it.label == "Pictures" || it.label == "Download" }
            request.allowedExtensions.any { it in setOf("mp3", "wav", "ogg", "m4a") } ->
                storageRoots.firstOrNull { it.label == "Music" || it.label == "Download" }
            else -> storageRoots.firstOrNull { it.label == "Download" }
        }
        mutableStateOf(preferred?.dir ?: storageRoots.firstOrNull()?.dir ?: context.filesDir)
    }

    val entries: List<File> = remember(currentDir, refreshTick, hasStoragePermission, request.allowedExtensions, request.pickDirectoryMode) {
        val children = runCatching { currentDir.listFiles()?.toList() }.getOrNull().orEmpty()
        val dirs = children
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase(Locale.US) }
        val files = children
            .filter { f ->
                if (!f.isFile || f.name.startsWith(".")) return@filter false
                if (request.allowedExtensions.isEmpty()) true
                else f.extension.lowercase(Locale.US) in request.allowedExtensions
            }
            .sortedBy { it.name.lowercase(Locale.US) }
        dirs + files
    }

    val firstItemFocus = remember { FocusRequester() }
    val closeButtonFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (entries.isNotEmpty()) {
            runCatching { firstItemFocus.requestFocus() }
        } else {
            runCatching { closeButtonFocus.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .size(width = 1360.dp, height = 880.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.bgPrimary)
                    .border(2.dp, palette.accent, RoundedCornerShape(24.dp))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .focusGroup()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TvFocusAmber,
                        modifier = Modifier.size(34.dp)
                    )
                    Column {
                        Text(
                            text = request.title,
                            color = palette.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = request.subtitle,
                            color = palette.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasStoragePermission) {
                        HybridButton(
                            label = "Izinkan Akses Storage",
                            icon = Icons.Default.Security,
                            isPrimary = true,
                            onClick = {
                                onRequestStoragePermission()
                                refreshTick++
                            }
                        )
                    }
                    if (request.onOpenSystemPicker != null) {
                        HybridButton(
                            label = "Picker Sistem",
                            icon = Icons.Default.SdStorage,
                            onClick = { request.onOpenSystemPicker.invoke() }
                        )
                    }
                    HybridButton(
                        label = "Refresh USB",
                        icon = Icons.Default.Refresh,
                        onClick = { refreshTick++ }
                    )
                    HybridButton(
                        label = "Tutup",
                        icon = Icons.Default.Close,
                        isDanger = true,
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(closeButtonFocus)
                    )
                }
            }

            // Quick Storage Roots (Internal, Download, Pictures, Music, Mounted USB Drives)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                storageRoots.forEach { root ->
                    val isSelected = currentDir.absolutePath == root.dir.absolutePath
                    HybridButton(
                        label = root.label,
                        icon = Icons.Default.SdStorage,
                        isPrimary = isSelected,
                        onClick = { currentDir = root.dir }
                    )
                }
            }

            // Current Directory Bar + Up Button + Optional Folder Sync Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bgSecondary)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val parentDir = currentDir.parentFile
                if (parentDir != null && parentDir.canRead()) {
                    HybridButton(
                        label = "Naik (..)",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { currentDir = parentDir }
                    )
                }
                Text(
                    text = "Lokasi: ${currentDir.absolutePath}",
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${entries.size} item",
                    color = palette.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (request.pickDirectoryMode && request.onDirectorySelected != null) {
                    HybridButton(
                        label = "Sinkronkan Folder Ini ✓",
                        icon = Icons.Default.Check,
                        isPrimary = true,
                        onClick = {
                            request.onDirectorySelected.invoke(currentDir)
                            onDismiss()
                        }
                    )
                }
            }

            // File & Folder List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.bgSecondary.copy(alpha = 0.65f))
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                if (entries.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (!hasStoragePermission) {
                                "Izin penyimpanan belum diberikan atau folder kosong."
                            } else {
                                "Tidak ada file (${request.allowedExtensions.joinToString(", ") { ".$it" }}) di folder ini."
                            },
                            color = palette.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Colokkan Flashdisk USB lalu klik 'Refresh USB', atau unggah file langsung dari HP/PC melalui Remote Control PWA di bawah.",
                            color = palette.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(entries, key = { _, f -> f.absolutePath }) { idx, file ->
                            TvFileRowItem(
                                file = file,
                                isDirectoryPickMode = request.pickDirectoryMode,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentDir = file
                                    } else if (request.pickDirectoryMode && request.onDirectorySelected != null) {
                                        request.onDirectorySelected.invoke(currentDir)
                                        onDismiss()
                                    } else {
                                        request.onFileSelected(file)
                                        onDismiss()
                                    }
                                },
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocus) else Modifier
                            )
                        }
                    }
                }
            }

            // Wireless Upload Hint Banner via Remote Control PWA
            HybridInfoBanner(
                text = if (request.pickDirectoryMode) {
                    "Mode Sync Massal USB: Buka folder berisi gambar/audio/video di Flashdisk, lalu klik 'Sinkronkan Folder Ini ✓'. Semua file akan disalin ke memori internal TV & gambar otomatis dioptimasi."
                } else {
                    "Tips Upload Mudah Tanpa Flashdisk: Buka $remoteServerUrl di browser HP/PC (satu Wi-Fi), lalu unggah Logo, Poster, Video, atau Audio MP3 langsung ke TV!"
                },
                accentColor = Color(0xFF10B981)
            )
            }
        }
    }
}

@Composable
private fun TvFileRowItem(
    file: File,
    isDirectoryPickMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val ext = file.extension.lowercase(Locale.US)
    val icon = when {
        file.isDirectory -> Icons.Default.Folder
        ext in setOf("png", "jpg", "jpeg", "webp", "bmp") -> Icons.Default.Image
        ext in setOf("mp4", "mkv", "webm", "mov", "m4v") -> Icons.Default.Movie
        ext in setOf("mp3", "wav", "ogg", "m4a", "aac") -> Icons.Default.AudioFile
        else -> Icons.Default.Description
    }
    val iconColor = when {
        file.isDirectory -> TvFocusAmber
        ext in setOf("png", "jpg", "jpeg", "webp", "bmp") -> Color(0xFF38BDF8)
        ext in setOf("mp4", "mkv", "webm", "mov", "m4v") -> Color(0xFFC084FC)
        ext in setOf("mp3", "wav", "ogg", "m4a", "aac") -> Color(0xFF34D399)
        else -> palette.accent
    }

    val sizeLabel = remember(file.absolutePath) {
        if (file.isDirectory) {
            val count = runCatching { file.listFiles()?.size ?: 0 }.getOrDefault(0)
            "Folder • $count item"
        } else {
            val bytes = file.length()
            when {
                bytes >= 1_048_576L -> String.format(Locale.US, "%.2f MB", bytes / 1_048_576.0)
                bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) palette.accent.copy(alpha = 0.25f)
                else palette.bgPrimary.copy(alpha = 0.85f)
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusAmber else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = if (isFocused) Color.White else palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sizeLabel,
                color = palette.textSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (file.isDirectory) "BUKA ›" else "PILIH ✓",
            color = if (isFocused) TvFocusAmber else palette.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
