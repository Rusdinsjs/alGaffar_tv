package id.algaffar.jwstv.ui.screens.standby

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.engine.HijriDateInfo
import id.algaffar.jwstv.domain.model.BrandSettings
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Top Header Card (`1440 x 180 dp`, matching the width of `MainSlideContainer`)
 * displaying Mosque Brand (clickable Logo -> Quick Menu, Name & Address) and
 * 2-line enlarged Masehi & Hijri Dates.
 *
 * - When [isReversed] is `false` (`HORIZONTAL` / `VERTICAL_RIGHT`):
 *   `[ Identitas (Logo, Nama/Alamat) ]  [ Tanggal rata kanan (Masehi atas, Hijriyah bawah) ]`
 * - When [isReversed] is `true` (`VERTICAL_LEFT`):
 *   `[ Tanggal rata kiri (Masehi atas, Hijriyah bawah) ]  [ Identitas (Nama/Alamat, Logo) ]`
 */
@Composable
fun HeaderBar(
    now: ZonedDateTime,
    brand: BrandSettings,
    hijriDate: HijriDateInfo,
    assetsRootDir: File?,
    isReversed: Boolean = false,
    isLogoMenuOpen: Boolean = false,
    onLogoClick: () -> Unit,
    showAddress: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Only extract LocalDate (changes once per day, not every second) so HeaderBarInner
    // skips recomposition and GPU redraws across 1Hz clock ticks.
    HeaderBarInner(
        masehiDate = now.toLocalDate(),
        brand = brand,
        hijriDate = hijriDate,
        assetsRootDir = assetsRootDir,
        isReversed = isReversed,
        isLogoMenuOpen = isLogoMenuOpen,
        onLogoClick = onLogoClick,
        showAddress = showAddress,
        modifier = modifier
    )
}

@Composable
private fun HeaderBarInner(
    masehiDate: LocalDate,
    brand: BrandSettings,
    hijriDate: HijriDateInfo,
    assetsRootDir: File?,
    isReversed: Boolean,
    isLogoMenuOpen: Boolean,
    onLogoClick: () -> Unit,
    showAddress: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val isPerformanceMode = LocalPerformanceMode.current
    val masehiFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    }
    val masehiDateText = remember(masehiDate) { masehiDate.format(masehiFormatter) }

    Box(
        modifier = modifier
            .size(width = 1440.dp, height = 180.dp)
            .padding(4.dp)
    ) {
        if (isPerformanceMode) {
            // Single-pass feathered horizontal gradient (avoids 1440x180 Offscreen FBO + 2x DstIn blend on Mali-450 GLES 2.0)
            val perfFeatherBrush = remember(palette.bgCard) {
                Brush.horizontalGradient(
                    0.00f to Color.Transparent,
                    0.05f to palette.bgCard.copy(alpha = palette.bgCard.alpha * 0.55f),
                    0.11f to palette.bgCard,
                    0.89f to palette.bgCard,
                    0.95f to palette.bgCard.copy(alpha = palette.bgCard.alpha * 0.55f),
                    1.00f to Color.Transparent
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(perfFeatherBrush)
            )
        } else {
            // Feathered/blurred-edge background so the header blends smoothly into the screen background without a border
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawBehind {
                        drawRect(color = palette.bgCard)
                        // Horizontal feather (left & right edges fade to transparent)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.00f to Color.Transparent,
                                0.025f to Color.Black.copy(alpha = 0.25f),
                                0.06f to Color.Black.copy(alpha = 0.70f),
                                0.11f to Color.Black,
                                0.89f to Color.Black,
                                0.94f to Color.Black.copy(alpha = 0.70f),
                                0.975f to Color.Black.copy(alpha = 0.25f),
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        // Vertical feather (top & bottom edges fade to transparent)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.08f to Color.Black.copy(alpha = 0.25f),
                                0.18f to Color.Black.copy(alpha = 0.70f),
                                0.28f to Color.Black,
                                0.72f to Color.Black,
                                0.82f to Color.Black.copy(alpha = 0.70f),
                                0.92f to Color.Black.copy(alpha = 0.25f),
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
        }

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isReversed) {
                // HORIZONTAL & VERTICAL_RIGHT: 1. Identitas (Logo, Nama/Alamat)  ->  2. Tanggal (Rata Kanan)
                MosqueIdentityBlock(
                    brand = brand,
                    assetsRootDir = assetsRootDir,
                    isReversed = false,
                    isMenuOpen = isLogoMenuOpen,
                    onLogoClick = onLogoClick,
                    showAddress = showAddress,
                    modifier = Modifier.weight(1.04f)
                )
                HeaderDateBlock(
                    masehiDateText = masehiDateText,
                    brand = brand,
                    hijriDate = hijriDate,
                    alignStart = false,
                    modifier = Modifier.weight(0.96f)
                )
            } else {
                // VERTICAL_LEFT: 1. Tanggal (Rata Kiri)  ->  2. Identitas (Nama/Alamat, Logo)
                HeaderDateBlock(
                    masehiDateText = masehiDateText,
                    brand = brand,
                    hijriDate = hijriDate,
                    alignStart = true,
                    modifier = Modifier.weight(0.96f)
                )
                MosqueIdentityBlock(
                    brand = brand,
                    assetsRootDir = assetsRootDir,
                    isReversed = true,
                    isMenuOpen = isLogoMenuOpen,
                    onLogoClick = onLogoClick,
                    showAddress = showAddress,
                    modifier = Modifier.weight(1.04f)
                )
            }
        }
    }
}

/**
 * Dedicated Clock Card (`480 x 180 dp`) placed beside `HeaderBar`:
 * The clock component itself fills the entire `480 x 180 dp` card area (minus standard 4.dp grid gap)
 * with compact inner padding and enlarged proportional font size (`ClockVariant.HEADER_CARD`).
 */
@Composable
fun HeaderClockCard(
    now: ZonedDateTime,
    clockStyle: String,
    modifier: Modifier = Modifier
) {
    ArtisticClock(
        now = now,
        style = clockStyle,
        variant = ClockVariant.HEADER_CARD,
        modifier = modifier
            .size(width = 480.dp, height = 180.dp)
            .padding(4.dp)
    )
}

/**
 * Floating Quick Menu opened when the Mosque Logo in `HeaderBar` is clicked (D-Pad & Touch).
 * Contains the 5 quick actions: Tema, Layout, Jam, Simulasi, Pengaturan.
 */
@Composable
fun LogoQuickMenuCard(
    onCycleTheme: () -> Unit,
    onCycleLayout: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onOpenDebugModal: () -> Unit,
    onOpenSettingsModal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val firstItemFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstItemFocus.requestFocus() }
    }

    Column(
        modifier = modifier
            .width(300.dp)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgPrimary.copy(alpha = 0.96f))
            .border(2.dp, palette.accent, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MENU CEPAT JWS",
                color = palette.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "D-Pad ▲ / Sentuh",
                color = palette.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LogoQuickMenuItem(
            icon = Icons.Default.Palette,
            label = "Tema",
            subtitle = "Ganti tema warna",
            onClick = onCycleTheme,
            modifier = Modifier.focusRequester(firstItemFocus)
        )
        LogoQuickMenuItem(
            icon = Icons.AutoMirrored.Filled.ViewQuilt,
            label = "Layout",
            subtitle = "Ganti posisi tata letak",
            onClick = onCycleLayout
        )
        LogoQuickMenuItem(
            icon = Icons.Default.Schedule,
            label = "Jam",
            subtitle = "Ganti gaya jam digital",
            onClick = onCycleClockStyle
        )
        LogoQuickMenuItem(
            icon = Icons.Default.BugReport,
            label = "Simulasi",
            subtitle = "Panel uji 31 state & audio",
            onClick = {
                onDismiss()
                onOpenDebugModal()
            }
        )
        LogoQuickMenuItem(
            icon = Icons.Default.Settings,
            label = "Pengaturan",
            subtitle = "Buka pengaturan lengkap",
            isHighlighted = true,
            onClick = {
                onDismiss()
                onOpenSettingsModal()
            }
        )
        LogoQuickMenuItem(
            icon = Icons.Default.Close,
            label = "Tutup Menu",
            subtitle = "",
            onClick = onDismiss
        )
    }
}

@Composable
private fun LogoQuickMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isFocused -> palette.accent
        isHighlighted -> palette.accent.copy(alpha = 0.2f)
        else -> palette.bgSecondary.copy(alpha = 0.85f)
    }
    val borderColor = when {
        isFocused -> Color(0xFFFBBF24)
        isHighlighted -> palette.accent.copy(alpha = 0.65f)
        else -> palette.border
    }
    val fgColor = if (isFocused) palette.bgPrimary else palette.textPrimary
    val subColor = if (isFocused) palette.bgPrimary.copy(alpha = 0.8f) else palette.textSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isFocused) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) palette.bgPrimary else palette.accent,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = fgColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = subColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MosqueIdentityBlock(
    brand: BrandSettings,
    assetsRootDir: File?,
    isReversed: Boolean,
    isMenuOpen: Boolean,
    onLogoClick: () -> Unit,
    showAddress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val addressLine = remember(brand.address, brand.phone) {
        buildString {
            append(brand.address)
            if (brand.phone.isNotBlank()) {
                append("  •  Telp: ")
                append(brand.phone)
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isReversed) Arrangement.spacedBy(18.dp, Alignment.End) else Arrangement.spacedBy(18.dp, Alignment.Start)
    ) {
        if (!isReversed) {
            MosqueLogoBadge(
                logoPath = brand.logo,
                assetsRootDir = assetsRootDir,
                isMenuOpen = isMenuOpen,
                onClick = onLogoClick
            )
        }

        val titleFontSp = (brand.titleFontSize * 1.35f).roundToInt().coerceIn(30, 64)
        val addressFontSp = (brand.addressFontSize * 1.35f).roundToInt().coerceIn(18, 34)

        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (isReversed) Alignment.End else Alignment.Start
        ) {
            Text(
                text = brand.name.ifBlank { "MASJID ANDA" },
                color = palette.textPrimary,
                fontSize = titleFontSp.sp,
                lineHeight = (titleFontSp + 4).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isReversed) TextAlign.End else TextAlign.Start,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            if (showAddress && addressLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = addressLine,
                    color = palette.textSecondary,
                    fontSize = addressFontSp.sp,
                    lineHeight = (addressFontSp + 6).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isReversed) TextAlign.End else TextAlign.Start,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        if (isReversed) {
            MosqueLogoBadge(
                logoPath = brand.logo,
                assetsRootDir = assetsRootDir,
                isMenuOpen = isMenuOpen,
                onClick = onLogoClick
            )
        }
    }
}

@Composable
private fun HeaderDateBlock(
    masehiDateText: String,
    brand: BrandSettings,
    hijriDate: HijriDateInfo,
    alignStart: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val horizontalAlign = if (alignStart) Alignment.Start else Alignment.End
    val textAlign = if (alignStart) TextAlign.Start else TextAlign.End

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlign,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        // Baris 1 (Atas): Tanggal Masehi (disesuaikan untuk tinggi header 180dp)
        if (brand.showMasehiDate) {
            Text(
                text = masehiDateText,
                color = palette.textPrimary,
                fontSize = 40.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Baris 2 (Bawah): Tanggal Hijriyah (tanpa badge Ba'da Maghrib)
        if (brand.showHijriDate) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent.copy(alpha = 0.18f))
                    .border(1.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = hijriDate.formatted,
                    color = palette.accent,
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
internal fun MosqueLogoBadge(
    logoPath: String,
    assetsRootDir: File?,
    isMenuOpen: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val resolvedModel: Any? = remember(logoPath, assetsRootDir) {
        if (logoPath.isBlank()) {
            null
        } else if (logoPath.startsWith("http://") || logoPath.startsWith("https://") || logoPath.startsWith("file://") || logoPath.startsWith("content://")) {
            logoPath
        } else if (assetsRootDir != null) {
            val cleanRel = logoPath.removePrefix("jws-assets/").removePrefix("/")
            File(assetsRootDir, cleanRel).takeIf { it.exists() } ?: File(logoPath).takeIf { it.exists() }
        } else {
            null
        }
    }

    val borderColor = when {
        isFocused -> Color(0xFFFBBF24)
        isMenuOpen -> palette.accentSecondary
        else -> palette.accent
    }
    val borderWidth = if (isFocused || isMenuOpen) 3.5.dp else 2.dp

    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.28f), palette.bgPrimary)
                )
            )
            .border(borderWidth, borderColor, CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (resolvedModel != null) {
            AsyncImage(
                model = resolvedModel,
                contentDescription = "Logo Masjid (Klik untuk Menu)",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
            )
        } else {
            val domeColor = palette.accent
            val goldColor = palette.accentSecondary
            Canvas(modifier = Modifier.size(74.dp)) {
                val w = size.width
                val h = size.height

                drawRect(
                    color = domeColor,
                    topLeft = Offset(w * 0.12f, h * 0.72f),
                    size = Size(w * 0.76f, h * 0.14f)
                )

                val domePath = Path().apply {
                    moveTo(w * 0.18f, h * 0.72f)
                    cubicTo(
                        w * 0.18f, h * 0.35f,
                        w * 0.50f, h * 0.18f,
                        w * 0.50f, h * 0.18f
                    )
                    cubicTo(
                        w * 0.50f, h * 0.18f,
                        w * 0.82f, h * 0.35f,
                        w * 0.82f, h * 0.72f
                    )
                    close()
                }
                drawPath(path = domePath, color = domeColor)

                drawCircle(
                    color = goldColor,
                    radius = w * 0.06f,
                    center = Offset(w * 0.50f, h * 0.11f)
                )
            }
        }
    }
}
