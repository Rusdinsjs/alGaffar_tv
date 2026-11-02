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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import id.algaffar.jwstv.domain.engine.HijriDateInfo
import id.algaffar.jwstv.domain.model.BrandSettings
import id.algaffar.jwstv.ui.components.clock.ArtisticClock
import id.algaffar.jwstv.ui.components.clock.ClockVariant
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Top Header Bar displaying Mosque Brand (Logo, Name, Address), Masehi & Hijri Dates,
 * Artistic Header Clock, and quick Hybrid (D-Pad + Touch) action buttons.
 * Mirrors `src/components/Screens/Header/Header.tsx`, `Brand.tsx`, `Tanggal.tsx`, and `SmallClock.tsx`.
 */
@Composable
fun HeaderBar(
    now: ZonedDateTime,
    brand: BrandSettings,
    hijriDate: HijriDateInfo,
    assetsRootDir: File?,
    onCycleTheme: () -> Unit,
    onCycleLayout: () -> Unit,
    onCycleClockStyle: () -> Unit,
    onOpenDebugModal: () -> Unit,
    onOpenSettingsModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val masehiFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    }

    Row(
        modifier = modifier
            .size(width = 1920.dp, height = 120.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgCard)
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left: Mosque Logo + Brand Name & Address
        Row(
            modifier = Modifier.weight(1.15f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MosqueLogoBadge(
                logoPath = brand.logo,
                assetsRootDir = assetsRootDir
            )

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = brand.name.ifBlank { "MASJID AL-GAFFAR" },
                    color = palette.textPrimary,
                    fontSize = brand.titleFontSize.coerceIn(22, 42).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(brand.address)
                        if (brand.phone.isNotBlank()) {
                            append("  •  Telp: ")
                            append(brand.phone)
                        }
                    },
                    color = palette.textSecondary,
                    fontSize = brand.addressFontSize.coerceIn(13, 22).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Center-Right: Masehi & Hijri Date + Quick Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (brand.showMasehiDate) {
                        Text(
                            text = now.format(masehiFormatter),
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (brand.showHijriDate) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accent.copy(alpha = 0.18f))
                                .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = hijriDate.formatted,
                                color = palette.accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (hijriDate.isAfterMaghribRollover) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.accentSecondary.copy(alpha = 0.22f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ba'da Maghrib",
                                    color = palette.accentSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Hybrid Toolbar (D-Pad Focusable & Touch Clickable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderIconButton(
                        icon = Icons.Default.Palette,
                        tooltip = "Tema",
                        onClick = onCycleTheme
                    )
                    HeaderIconButton(
                        icon = Icons.AutoMirrored.Filled.ViewQuilt,
                        tooltip = "Layout",
                        onClick = onCycleLayout
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Schedule,
                        tooltip = "Jam",
                        onClick = onCycleClockStyle
                    )
                    HeaderIconButton(
                        icon = Icons.Default.BugReport,
                        tooltip = "Simulasi",
                        onClick = onOpenDebugModal
                    )
                    HeaderIconButton(
                        icon = Icons.Default.Settings,
                        tooltip = "Pengaturan",
                        onClick = onOpenSettingsModal
                    )
                }
            }

            // 3. Far Right: Artistic Clock
            ArtisticClock(
                now = now,
                style = brand.clockStyle,
                variant = ClockVariant.HEADER
            )
        }
    }
}

@Composable
private fun MosqueLogoBadge(
    logoPath: String,
    assetsRootDir: File?
) {
    val palette = LocalJwsTheme.current
    val resolvedModel: Any? = remember(logoPath, assetsRootDir) {
        if (logoPath.isBlank()) {
            null
        } else if (logoPath.startsWith("http://") || logoPath.startsWith("https://") || logoPath.startsWith("file://")) {
            logoPath
        } else if (assetsRootDir != null) {
            val cleanRel = logoPath.removePrefix("jws-assets/").removePrefix("/")
            File(assetsRootDir, cleanRel).takeIf { it.exists() } ?: File(logoPath).takeIf { it.exists() }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.28f), palette.bgPrimary)
                )
            )
            .border(2.dp, palette.accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedModel != null) {
            AsyncImage(
                model = resolvedModel,
                contentDescription = "Logo Masjid",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
            )
        } else {
            // Vector Mosque Dome & Crescent Crest
            val domeColor = palette.accent
            val goldColor = palette.accentSecondary
            Canvas(modifier = Modifier.size(52.dp)) {
                val w = size.width
                val h = size.height

                // Base platform
                drawRect(
                    color = domeColor,
                    topLeft = Offset(w * 0.12f, h * 0.72f),
                    size = Size(w * 0.76f, h * 0.14f)
                )

                // Dome arch path
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

                // Finial spire on top of dome
                drawCircle(
                    color = goldColor,
                    radius = w * 0.06f,
                    center = Offset(w * 0.50f, h * 0.11f)
                )
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFocused) palette.accent else palette.bgPrimary.copy(alpha = 0.65f)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) palette.accentSecondary else palette.border,
                shape = RoundedCornerShape(10.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isFocused) palette.bgPrimary else palette.textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = tooltip,
            color = if (isFocused) palette.bgPrimary else palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
