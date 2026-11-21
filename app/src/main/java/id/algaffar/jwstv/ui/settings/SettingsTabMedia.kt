package id.algaffar.jwstv.ui.settings

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File

/**
 * Resolves a stored media path (`jws-assets/...`, absolute file path, HTTP/file/content URI,
 * or legacy Tauri base64 data URI) into a Coil-compatible model or decoded [ImageBitmap].
 */
internal sealed interface ResolvedMediaPreview {
    data class CoilModel(val model: Any) : ResolvedMediaPreview
    data class DecodedBitmap(val bitmap: ImageBitmap) : ResolvedMediaPreview
}

internal fun resolveMediaPreview(rawPath: String?, assetsRootDir: File?): ResolvedMediaPreview? {
    val trimmed = rawPath?.trim().orEmpty()
    if (trimmed.isEmpty()) return null

    if (trimmed.startsWith("data:image/", ignoreCase = true) && trimmed.contains("base64,")) {
        val b64 = trimmed.substringAfter("base64,")
        val bmp = runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
        return bmp?.let { ResolvedMediaPreview.DecodedBitmap(it) }
    }

    if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("file://", ignoreCase = true) ||
        trimmed.startsWith("content://", ignoreCase = true)
    ) {
        return ResolvedMediaPreview.CoilModel(trimmed)
    }

    val clean = trimmed.replace('\\', '/').removePrefix("jws-assets/").removePrefix("/")
    val file = assetsRootDir?.let { File(it, clean).takeIf { f -> f.exists() && f.length() > 0L } }
        ?: File(trimmed).takeIf { it.exists() && it.length() > 0L }
    return file?.let { ResolvedMediaPreview.CoilModel(it) }
}

@Composable
internal fun MediaPreviewImageBox(
    rawPath: String?,
    assetsRootDir: File?,
    contentDescription: String,
    emptyLabel: String = "BELUM ADA",
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val resolved = remember(rawPath, assetsRootDir) { resolveMediaPreview(rawPath, assetsRootDir) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090F1E))
            .border(
                width = 1.dp,
                color = if (resolved != null) Color(0xFF10B981).copy(alpha = 0.65f) else palette.border,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (resolved) {
            is ResolvedMediaPreview.CoilModel -> {
                AsyncImage(
                    model = resolved.model,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is ResolvedMediaPreview.DecodedBitmap -> {
                Image(
                    bitmap = resolved.bitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = palette.textSecondary.copy(alpha = 0.45f),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = emptyLabel,
                        color = palette.textSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Availability Status Pill on Top-Right
        if (resolved != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF065F46).copy(alpha = 0.92f))
                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF6EE7B7),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "TERSEDIA",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private data class BackgroundMediaSlotSpec(
    val title: String,
    val description: String,
    val currentPath: String,
    val onSavedPath: (String) -> Unit,
    val onClear: () -> Unit
)

/**
 * Complete Media Gallery Settings Tab (`media`), modeled after `MediaSettingsForm.tsx` in `jws-android-algaffar`.
 * Features thumbnail previews, availability indicators, Tartil/Tarhim multi-slide & video managers,
 * and all 9 Khutbah/Ceramah & Pelaksanaan Sholat background slots.
 */
@Composable
fun MediaGallerySettingsTab(
    settings: AppStateSettings,
    actions: SettingsActions
) {
    val palette = LocalJwsTheme.current
    val media = settings.media

    val khutbahSlots = listOf(
        BackgroundMediaSlotSpec(
            title = "Khutbah Jum'at",
            description = "Latar belakang saat Khutbah Jum'at berlangsung.",
            currentPath = media.khutbahJumatImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahJumatImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahJumatImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Khutbah Idul Fitri",
            description = "Latar belakang saat Khutbah Idul Fitri berlangsung.",
            currentPath = media.khutbahIdulFitriImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahIdulFitriImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahIdulFitriImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Khutbah Idul Adha",
            description = "Latar belakang saat Khutbah Idul Adha berlangsung.",
            currentPath = media.khutbahIdulAdhaImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahIdulAdhaImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(khutbahIdulAdhaImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Ceramah Tarawih / Ramadhan",
            description = "Latar belakang saat Ceramah / Kultum Ramadhan berlangsung.",
            currentPath = media.ceramahTarawihImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(ceramahTarawihImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(ceramahTarawihImage = "")) } }
        )
    )

    val sholatSlots = listOf(
        BackgroundMediaSlotSpec(
            title = "Sholat 5 Waktu",
            description = "Latar belakang saat Sholat Rawatib (Subuh–Isya) berlangsung.",
            currentPath = media.sholat5WaktuImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(sholat5WaktuImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(sholat5WaktuImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Sholat Jum'at",
            description = "Latar belakang saat pelaksanaan Sholat Jum'at berlangsung.",
            currentPath = media.sholatJumatImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(sholatJumatImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(sholatJumatImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Sholat Idul Fitri",
            description = "Latar belakang saat pelaksanaan Sholat Idul Fitri.",
            currentPath = media.sholatIdulFitriImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(sholatIdulFitriImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(sholatIdulFitriImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Sholat Idul Adha",
            description = "Latar belakang saat pelaksanaan Sholat Idul Adha.",
            currentPath = media.sholatIdulAdhaImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(sholatIdulAdhaImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(sholatIdulAdhaImage = "")) } }
        ),
        BackgroundMediaSlotSpec(
            title = "Sholat Tarawih & Witir",
            description = "Latar belakang saat pelaksanaan Sholat Tarawih & Witir.",
            currentPath = media.sholatTarawihWitirImage,
            onSavedPath = { p -> actions.onUpdateSettings { it.copy(media = it.media.copy(sholatTarawihWitirImage = p)) } },
            onClear = { actions.onUpdateSettings { it.copy(media = it.media.copy(sholatTarawihWitirImage = "")) } }
        )
    )

    val khutbahReadyCount = khutbahSlots.count { resolveMediaPreview(it.currentPath, actions.assetsRootDir) != null }
    val sholatReadyCount = sholatSlots.count { resolveMediaPreview(it.currentPath, actions.assetsRootDir) != null }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // ==================== RINGKASAN KELENGKAPAN MEDIA ====================
        HybridSectionCard(
            title = "Ringkasan Ketersediaan Galeri Media",
            subtitle = "Pantau thumbnail dan status gambar latar untuk Tartil, Tarhim, Do'a Adzan, Khutbah/Ceramah, dan Pelaksanaan Sholat"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaSummaryBadge(
                    label = "TARTIL",
                    value = if (media.tartilMediaType == "video") {
                        if (media.tartilVideo.isNotBlank()) "Video Siap" else "Belum Ada Video"
                    } else {
                        "${media.tartilImages.size} Slide Gambar"
                    },
                    isReady = if (media.tartilMediaType == "video") media.tartilVideo.isNotBlank() else media.tartilImages.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                MediaSummaryBadge(
                    label = "TARHIM",
                    value = if (media.tarhimMediaType == "video") {
                        if (media.tarhimVideo.isNotBlank()) "Video Siap" else "Belum Ada Video"
                    } else {
                        "${media.tarhimImages.size} Slide Gambar"
                    },
                    isReady = if (media.tarhimMediaType == "video") media.tarhimVideo.isNotBlank() else media.tarhimImages.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
                MediaSummaryBadge(
                    label = "KHUTBAH & CERAMAH",
                    value = "$khutbahReadyCount / ${khutbahSlots.size} Latar Siap",
                    isReady = khutbahReadyCount > 0,
                    modifier = Modifier.weight(1f)
                )
                MediaSummaryBadge(
                    label = "PELAKSANAAN SHOLAT",
                    value = "$sholatReadyCount / ${sholatSlots.size} Latar Siap",
                    isReady = sholatReadyCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ==================== 1. DO'A SETELAH ADZAN ====================
        HybridSectionCard(
            title = "Layar Do'a Setelah Adzan (`doa_adzan`)",
            subtitle = "Tampilkan doa setelah adzan (teks Arab & arti HR. Bukhari atau poster gambar kustom) sebelum hitung mundur Iqomah"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HybridToggleRow(
                    label = "Aktifkan Layar Do'a Setelah Adzan",
                    description = "Otomatis tampil sesaat setelah Adzan selesai",
                    checked = media.doaAdzanEnabled,
                    onCheckedChange = { v ->
                        actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanEnabled = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
                HybridNumberStepper(
                    label = "Durasi Tampil",
                    value = media.doaAdzanDuration,
                    min = 15,
                    max = 120,
                    step = 5,
                    unit = "detik",
                    enabled = media.doaAdzanEnabled,
                    onValueChange = { v ->
                        actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanDuration = v)) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            MediaImageSlotCard(
                title = "Gambar Do'a Setelah Adzan (Opsional)",
                description = if (media.doaAdzanImage.isNotBlank()) {
                    "Gambar kustom aktif menggantikan tampilan teks Arab bawaan."
                } else {
                    "Menggunakan desain Teks Arab + Terjemahan bawaan sistem. Unggah poster gambar jika ingin menggantinya."
                },
                imagePath = media.doaAdzanImage,
                emptyLabel = "TEKS BAWAAN",
                inactiveStatusText = "Teks Arab Bawaan Aktif",
                assetsRootDir = actions.assetsRootDir,
                onUpload = {
                    actions.onPickImageAsset("media") { path ->
                        actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanImage = path)) }
                    }
                },
                onRemove = {
                    actions.onUpdateSettings { it.copy(media = it.media.copy(doaAdzanImage = "")) }
                }
            )
        }

        // ==================== 2. MEDIA TARTIL QUR'AN ====================
        MultiSlideOrVideoMediaCard(
            title = "Media Tartil Qur'an (Menjelang Tarhim)",
            subtitle = "Pilih kumpulan slide gambar bergantian atau video tilawah saat state Tartil berlangsung",
            mediaType = media.tartilMediaType,
            images = media.tartilImages,
            interval = media.tartilSlideInterval,
            videoPath = media.tartilVideo,
            assetsRootDir = actions.assetsRootDir,
            onChangeMediaType = { mode ->
                actions.onUpdateSettings { it.copy(media = it.media.copy(tartilMediaType = mode)) }
            },
            onUpdateInterval = { v ->
                actions.onUpdateSettings { it.copy(media = it.media.copy(tartilSlideInterval = v)) }
            },
            onAddImage = {
                actions.onPickImageAsset("tartil") { path ->
                    actions.onUpdateSettings { prev ->
                        prev.copy(media = prev.media.copy(tartilImages = prev.media.tartilImages + path))
                    }
                }
            },
            onMoveImage = { idx, delta ->
                actions.onUpdateSettings { prev ->
                    val list = prev.media.tartilImages.toMutableList()
                    val target = idx + delta
                    if (target in list.indices) {
                        val item = list.removeAt(idx)
                        list.add(target, item)
                    }
                    prev.copy(media = prev.media.copy(tartilImages = list))
                }
            },
            onRemoveImage = { idx ->
                actions.onUpdateSettings { prev ->
                    prev.copy(
                        media = prev.media.copy(
                            tartilImages = prev.media.tartilImages.filterIndexed { i, _ -> i != idx }
                        )
                    )
                }
            },
            onPickVideo = {
                actions.onPickVideoAsset { path, _ ->
                    actions.onUpdateSettings { it.copy(media = it.media.copy(tartilVideo = path)) }
                }
            },
            onRemoveVideo = {
                actions.onUpdateSettings { it.copy(media = it.media.copy(tartilVideo = "")) }
            }
        )

        // ==================== 3. MEDIA TARHIM MENJELANG ADZAN ====================
        MultiSlideOrVideoMediaCard(
            title = "Media Tarhim Menjelang Adzan",
            subtitle = "Pilih kumpulan slide gambar bergantian atau video tarhim saat state Tarhim berlangsung",
            mediaType = media.tarhimMediaType,
            images = media.tarhimImages,
            interval = media.tarhimSlideInterval,
            videoPath = media.tarhimVideo,
            assetsRootDir = actions.assetsRootDir,
            onChangeMediaType = { mode ->
                actions.onUpdateSettings { it.copy(media = it.media.copy(tarhimMediaType = mode)) }
            },
            onUpdateInterval = { v ->
                actions.onUpdateSettings { it.copy(media = it.media.copy(tarhimSlideInterval = v)) }
            },
            onAddImage = {
                actions.onPickImageAsset("tarhim") { path ->
                    actions.onUpdateSettings { prev ->
                        prev.copy(media = prev.media.copy(tarhimImages = prev.media.tarhimImages + path))
                    }
                }
            },
            onMoveImage = { idx, delta ->
                actions.onUpdateSettings { prev ->
                    val list = prev.media.tarhimImages.toMutableList()
                    val target = idx + delta
                    if (target in list.indices) {
                        val item = list.removeAt(idx)
                        list.add(target, item)
                    }
                    prev.copy(media = prev.media.copy(tarhimImages = list))
                }
            },
            onRemoveImage = { idx ->
                actions.onUpdateSettings { prev ->
                    prev.copy(
                        media = prev.media.copy(
                            tarhimImages = prev.media.tarhimImages.filterIndexed { i, _ -> i != idx }
                        )
                    )
                }
            },
            onPickVideo = {
                actions.onPickVideoAsset { path, _ ->
                    actions.onUpdateSettings { it.copy(media = it.media.copy(tarhimVideo = path)) }
                }
            },
            onRemoveVideo = {
                actions.onUpdateSettings { it.copy(media = it.media.copy(tarhimVideo = "")) }
            }
        )

        // ==================== 4. LATAR KHUTBAH & CERAMAH (4 SLOT) ====================
        HybridSectionCard(
            title = "Latar Khutbah & Ceramah ($khutbahReadyCount / ${khutbahSlots.size} Tersedia)",
            subtitle = "Gambar latar fullscreen untuk Khutbah Jum'at, Khutbah Idul Fitri, Khutbah Idul Adha, dan Ceramah Tarawih"
        ) {
            khutbahSlots.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { slot ->
                        MediaImageSlotCard(
                            title = slot.title,
                            description = slot.description,
                            imagePath = slot.currentPath,
                            assetsRootDir = actions.assetsRootDir,
                            onUpload = { actions.onPickImageAsset("media", slot.onSavedPath) },
                            onRemove = slot.onClear,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ==================== 5. LATAR PELAKSANAAN SHOLAT (5 SLOT) ====================
        HybridSectionCard(
            title = "Latar Pelaksanaan Sholat ($sholatReadyCount / ${sholatSlots.size} Tersedia)",
            subtitle = "Gambar latar fullscreen untuk Sholat 5 Waktu, Sholat Jum'at, Sholat Idul Fitri, Sholat Idul Adha, dan Tarawih & Witir"
        ) {
            sholatSlots.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { slot ->
                        MediaImageSlotCard(
                            title = slot.title,
                            description = slot.description,
                            imagePath = slot.currentPath,
                            assetsRootDir = actions.assetsRootDir,
                            onUpload = { actions.onPickImageAsset("media", slot.onSavedPath) },
                            onRemove = slot.onClear,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            HybridInfoBanner(
                text = "Rekomendasi: Gunakan gambar landscape rasio 16:9 (1920×1080, format JPG/PNG/WebP). Gambar otomatis dioptimasi agar ringan dan stabil 24/7 di TV/STB.",
                accentColor = Color(0xFFFBBF24)
            )
        }
    }
}

@Composable
private fun MediaSummaryBadge(
    label: String,
    value: String,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val accent = if (isReady) Color(0xFF10B981) else palette.textSecondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgPrimary.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = palette.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = if (isReady) Color(0xFF34D399) else palette.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MediaImageSlotCard(
    title: String,
    description: String,
    imagePath: String,
    assetsRootDir: File?,
    onUpload: () -> Unit,
    onRemove: () -> Unit,
    emptyLabel: String = "BELUM ADA",
    inactiveStatusText: String = "Belum Ada (Default Tema)",
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val resolved = remember(imagePath, assetsRootDir) { resolveMediaPreview(imagePath, assetsRootDir) }
    val hasImage = resolved != null
    val displayFileName = remember(imagePath) {
        when {
            imagePath.isBlank() -> ""
            imagePath.startsWith("data:image/", true) -> "Gambar Tersimpan (Embedded)"
            else -> imagePath.replace('\\', '/').substringAfterLast('/')
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.bgPrimary.copy(alpha = 0.84f))
            .border(
                width = 1.dp,
                color = if (hasImage) Color(0xFF10B981).copy(alpha = 0.45f) else palette.border,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 16:9 Thumbnail Preview
        MediaPreviewImageBox(
            rawPath = imagePath,
            assetsRootDir = assetsRootDir,
            contentDescription = title,
            emptyLabel = emptyLabel,
            modifier = Modifier
                .width(184.dp)
                .height(104.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = description,
                color = palette.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (hasImage) {
                    "✓ File: $displayFileName"
                } else if (imagePath.isNotBlank()) {
                    "⚠ File tidak ditemukan di memori: $displayFileName"
                } else {
                    inactiveStatusText
                },
                color = when {
                    hasImage -> Color(0xFF34D399)
                    imagePath.isNotBlank() -> Color(0xFFFBBF24)
                    else -> palette.textSecondary.copy(alpha = 0.75f)
                },
                fontSize = 11.sp,
                fontFamily = if (hasImage) FontFamily.Monospace else FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HybridButton(
                    label = if (hasImage) "Ganti" else "Pilih Gambar",
                    icon = Icons.Default.Upload,
                    isPrimary = !hasImage,
                    onClick = onUpload
                )
                if (imagePath.isNotBlank()) {
                    HybridButton(
                        label = "Hapus",
                        icon = Icons.Default.Delete,
                        isDanger = true,
                        onClick = onRemove
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSlideOrVideoMediaCard(
    title: String,
    subtitle: String,
    mediaType: String,
    images: List<String>,
    interval: Int,
    videoPath: String,
    assetsRootDir: File?,
    onChangeMediaType: (String) -> Unit,
    onUpdateInterval: (Int) -> Unit,
    onAddImage: () -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onPickVideo: () -> Unit,
    onRemoveVideo: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val isVideoMode = mediaType.equals("video", ignoreCase = true)

    HybridSectionCard(
        title = title,
        subtitle = subtitle,
        headerAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HybridButton(
                    label = "Slide Gambar (${images.size})",
                    icon = Icons.Default.Image,
                    isPrimary = !isVideoMode,
                    onClick = { onChangeMediaType("image") }
                )
                HybridButton(
                    label = "Video Tilawah",
                    icon = Icons.Default.Videocam,
                    isPrimary = isVideoMode,
                    onClick = { onChangeMediaType("video") }
                )
            }
        }
    ) {
        if (!isVideoMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HybridNumberStepper(
                    label = "Interval Pergantian Slide",
                    value = interval,
                    min = 3,
                    max = 120,
                    unit = "detik",
                    onValueChange = onUpdateInterval,
                    modifier = Modifier.weight(1f)
                )
                HybridButton(
                    label = "+ Tambah Slide Gambar",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    onClick = onAddImage
                )
            }

            if (images.isEmpty()) {
                HybridInfoBanner(
                    text = "Belum ada slide gambar khusus. Layar akan menampilkan latar tema bawaan saat state aktif. Klik '+ Tambah Slide Gambar' untuk menambahkan latar 16:9.",
                    accentColor = Color(0xFF38BDF8)
                )
            } else {
                images.chunked(3).forEachIndexed { rowIdx, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { colIdx, imgPath ->
                            val actualIdx = rowIdx * 3 + colIdx
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.bgPrimary.copy(alpha = 0.85f))
                                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box {
                                    MediaPreviewImageBox(
                                        rawPath = imgPath,
                                        assetsRootDir = assetsRootDir,
                                        contentDescription = "Slide #${actualIdx + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.78f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Slide #${actualIdx + 1}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (actualIdx > 0) {
                                            HybridButton(
                                                label = "",
                                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                                onClick = { onMoveImage(actualIdx, -1) }
                                            )
                                        }
                                        if (actualIdx < images.lastIndex) {
                                            HybridButton(
                                                label = "",
                                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                                onClick = { onMoveImage(actualIdx, 1) }
                                            )
                                        }
                                    }
                                    HybridButton(
                                        label = "Hapus",
                                        icon = Icons.Default.Delete,
                                        isDanger = true,
                                        onClick = { onRemoveImage(actualIdx) }
                                    )
                                }
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            val videoExists = remember(videoPath, assetsRootDir) {
                if (videoPath.isBlank()) false
                else if (videoPath.startsWith("http") || videoPath.startsWith("file://") || videoPath.startsWith("content://")) true
                else {
                    val clean = videoPath.replace('\\', '/').removePrefix("jws-assets/").removePrefix("/")
                    assetsRootDir?.let { File(it, clean).exists() } == true || File(videoPath).exists()
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgPrimary.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = if (videoExists) Color(0xFF10B981).copy(alpha = 0.55f) else palette.border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (videoExists) Color(0xFF10B981).copy(alpha = 0.18f) else palette.bgSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (videoExists) Color(0xFF34D399) else palette.textSecondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (videoExists) "Video Tilawah Tersedia" else "Belum Ada Video Tilawah",
                            color = if (videoExists) Color(0xFF34D399) else palette.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (videoPath.isNotBlank()) {
                                "File: ${videoPath.substringAfterLast('/')}"
                            } else {
                                "Disarankan format MP4 (H.264) 720p/1080p. Diputar saat state berlangsung."
                            },
                            color = palette.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridButton(
                        label = if (videoPath.isNotBlank()) "Ganti Video" else "Pilih Video (.mp4)",
                        icon = Icons.Default.Upload,
                        isPrimary = true,
                        onClick = onPickVideo
                    )
                    if (videoPath.isNotBlank()) {
                        HybridButton(
                            label = "Hapus",
                            icon = Icons.Default.Delete,
                            isDanger = true,
                            onClick = onRemoveVideo
                        )
                    }
                }
            }
        }
    }
}
