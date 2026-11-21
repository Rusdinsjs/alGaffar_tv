package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.theme.LocalJwsTheme

private data class SettingsSubTab(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private data class SettingsCategoryGroup(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tabs: List<SettingsSubTab>
)

private val SETTINGS_CATEGORIES = listOf(
    SettingsCategoryGroup(
        id = "masjid",
        label = "Masjid",
        icon = Icons.Default.Home,
        accentColor = Color(0xFF10B981), // Emerald
        tabs = listOf(
            SettingsSubTab("brand", "Identitas Masjid", Icons.Default.Home),
            SettingsSubTab("masjidQr", "QR Infaq Digital", Icons.Default.QrCode2),
            SettingsSubTab("onlineFinance", "Keuangan Online", Icons.Default.AccountBalanceWallet)
        )
    ),
    SettingsCategoryGroup(
        id = "sholat",
        label = "Sholat",
        icon = Icons.Default.Book,
        accentColor = Color(0xFF38BDF8), // Sky Blue
        tabs = listOf(
            SettingsSubTab("prayer", "Waktu & Tahapan Sholat", Icons.Default.Schedule),
            SettingsSubTab("audio", "Audio Sholat", Icons.Default.VolumeUp),
            SettingsSubTab("customAudio", "Audio Kustom", Icons.Default.PlayArrow)
        )
    ),
    SettingsCategoryGroup(
        id = "tampilan",
        label = "Tampilan",
        icon = Icons.Default.Palette,
        accentColor = Color(0xFFC084FC), // Purple
        tabs = listOf(
            SettingsSubTab("themes", "Tema Visual", Icons.Default.Palette),
            SettingsSubTab("layout", "Tata Letak", Icons.Default.ViewQuilt),
            SettingsSubTab("clock", "Gaya Jam (8)", Icons.Default.AccessTime),
            SettingsSubTab("overlayText", "Overlay Text", Icons.Default.TextFields),
            SettingsSubTab("slides", "Slideshow Info", Icons.Default.Slideshow),
            SettingsSubTab("info", "Kartu Info & PHBI", Icons.Default.Info),
            SettingsSubTab("media", "Galeri Media", Icons.Default.Image),
            SettingsSubTab("standbySlideshow", "Mode Standby", Icons.Default.Monitor)
        )
    ),
    SettingsCategoryGroup(
        id = "acara",
        label = "Acara",
        icon = Icons.Default.Star,
        accentColor = Color(0xFFFBBF24), // Amber
        tabs = listOf(
            SettingsSubTab("jumat", "Jum'at", Icons.Default.CalendarMonth),
            SettingsSubTab("ramadhan", "Ramadhan", Icons.Default.NightsStay),
            SettingsSubTab("idulFitri", "Idul Fitri", Icons.Default.Event),
            SettingsSubTab("idulAdha", "Idul Adha & Qurban", Icons.Default.Event),
            SettingsSubTab("state", "Screensaver & State", Icons.Default.AutoAwesome),
            SettingsSubTab("live", "Live & CCTV", Icons.Default.LiveTv),
            SettingsSubTab("switches", "Hardware Saklar", Icons.Default.PowerSettingsNew)
        )
    ),
    SettingsCategoryGroup(
        id = "sistem",
        label = "Sistem",
        icon = Icons.Default.Build,
        accentColor = Color(0xFF2DD4BF), // Teal
        tabs = listOf(
            SettingsSubTab("network", "Remote & Jaringan", Icons.Default.Wifi),
            SettingsSubTab("keyboard", "Keyboard & Input", Icons.Default.Keyboard),
            SettingsSubTab("userManagement", "Kelola Admin", Icons.Default.People),
            SettingsSubTab("backup", "Data & Backup", Icons.Default.Download),
            SettingsSubTab("logs", "Log Aktivitas", Icons.Default.Terminal),
            SettingsSubTab("remote_help", "Panduan Remote", Icons.Default.Tv)
        )
    )
)

/**
 * Redesigned Hybrid (Android TV D-Pad + Touchscreen) Settings Modal.
 * Modeled directly after `MobileSettingsLayout.tsx` & `SettingsModal.tsx` from `jws-android-algaffar`:
 * - Top Header Bar (Title, Active Sub-Tab, TV D-Pad Badge, Simulator, User Logout, Close)
 * - Full-Width Scrollable Form Content Area
 * - Elevated High-Contrast Navy Sub-Menu Bar (`#121C32`)
 * - Solid High-Contrast 5-Category Bottom Bar (`#060913`) with color-coded glow indicators
 */
@Composable
fun SettingsModal(
    settings: AppStateSettings,
    statusBannerMessage: String,
    takjilImportPreview: List<TakjilImportPreviewRow>?,
    onConfirmTakjilImport: () -> Unit,
    onDismissTakjilImport: () -> Unit,
    actions: SettingsActions,
    onClose: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var authenticatedUser by remember { mutableStateOf<UserAccount?>(null) }

    val currentUser = authenticatedUser
    if (currentUser == null) {
        PinLoginGuardScreen(
            users = settings.users,
            onAuthenticated = { authenticatedUser = it },
            onCancel = onClose
        )
        return
    }

    val activeRole = remember(currentUser.role) { UserRole.fromId(currentUser.role) }
    val filteredCategories = remember(activeRole) {
        SETTINGS_CATEGORIES.map { cat ->
            cat.copy(tabs = cat.tabs.filter { AuthManager.canAccessTab(activeRole, it.id) })
        }.filter { it.tabs.isNotEmpty() }
    }

    var activeCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.id ?: "masjid")
    }
    val activeCategory = filteredCategories.firstOrNull { it.id == activeCategoryId }
        ?: filteredCategories.first()

    var activeTabId by remember(filteredCategories) {
        mutableStateOf(activeCategory.tabs.firstOrNull()?.id ?: "brand")
    }
    val activeTabObj = activeCategory.tabs.firstOrNull { it.id == activeTabId }
        ?: activeCategory.tabs.first()

    val contentScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==================== 1. TOP HEADER BAR ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.bgSecondary)
                    .border(1.dp, palette.border)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accent.copy(alpha = 0.2f))
                            .border(1.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PENGATURAN DISPLAY JWS",
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TvFocusAmber.copy(alpha = 0.16f))
                                    .border(1.dp, TvFocusAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = TvFocusAmber, modifier = Modifier.size(12.dp))
                                Text("ANDROID TV / D-PAD", color = TvFocusAmber, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            text = activeTabObj.label,
                            color = palette.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HybridButton(
                        label = "Simulasi 31-State",
                        icon = Icons.Default.PlayArrow,
                        onClick = actions.onOpenDebugSimulator
                    )
                    HybridButton(
                        label = "${currentUser.username} (${activeRole.id.uppercase()})",
                        icon = Icons.Default.Logout,
                        isDanger = true,
                        onClick = {
                            authenticatedUser = null
                            onClose()
                        }
                    )
                    HybridButton(
                        label = "TUTUP",
                        icon = Icons.Default.Close,
                        isPrimary = true,
                        onClick = onClose
                    )
                }
            }

            // ==================== 2. FULL-WIDTH CONTENT AREA ====================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 36.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (statusBannerMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accent.copy(alpha = 0.18f))
                            .border(1.5.dp, palette.accent, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = statusBannerMessage,
                            color = palette.accentSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when (activeTabObj.id) {
                    // Category 1: Masjid
                    "brand" -> BrandSettingsTab(settings, actions)
                    "masjidQr" -> MasjidQrSettingsTab(settings, actions)
                    "onlineFinance" -> OnlineFinanceSettingsTab(settings, actions)

                    // Category 2: Sholat
                    "prayer" -> PrayerAndStagesSettingsTab(settings, actions)
                    "audio" -> AudioSholatSettingsTab(settings, actions)
                    "customAudio" -> CustomAudioSettingsTab(settings, actions)

                    // Category 3: Tampilan
                    "themes" -> ThemeSettingsTab(settings, actions)
                    "layout" -> LayoutSettingsTab(settings, actions)
                    "clock" -> ClockStyleSettingsTab(settings, actions)
                    "overlayText" -> OverlayTextSettingsTab(settings, actions)
                    "slides" -> SlideSettingsTab(settings, actions)
                    "info" -> InfoAndPhbiSettingsTab(settings, actions)
                    "media" -> MediaGallerySettingsTab(settings, actions)
                    "standbySlideshow" -> StandbyModeSettingsTab(settings, actions)

                    // Category 4: Acara
                    "jumat" -> JumatSettingsTab(settings, actions)
                    "ramadhan" -> RamadhanSettingsTab(settings, actions)
                    "idulFitri" -> IdulFitriSettingsTab(settings, actions)
                    "idulAdha" -> IdulAdhaQurbanSettingsTab(settings, actions)
                    "state" -> ScreensaverStateSettingsTab(settings, actions)
                    "live" -> LiveAndCctvSettingsTab(settings, actions)
                    "switches" -> SwitchHardwareSettingsTab(settings, actions)

                    // Category 5: Sistem
                    "network" -> RemoteNetworkSettingsTab(actions)
                    "keyboard" -> KeyboardInputSettingsTab(settings, actions)
                    "userManagement" -> UserManagementSettingsTab(settings, actions)
                    "backup" -> BackupRestoreSettingsTab(actions)
                    "logs" -> LogViewerSettingsTab()
                    "remote_help" -> RemoteHelpSettingsTab()
                }
            }

            // ==================== 3. ELEVATED NAVY SUB-MENU BAR (#121C32) ====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121C32))
                    .border(1.5.dp, Color(0xFF475569).copy(alpha = 0.65f))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(activeCategory.accentColor.copy(alpha = 0.2f))
                                .border(1.dp, activeCategory.accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = activeCategory.label.uppercase(),
                                color = activeCategory.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = "Sub-Menu Pilihan (Gunakan D-Pad ⬅ ➡ untuk memilih)",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "⬆ Masuk Form  •  ⬇ Kategori Utama  •  OK Pilih",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeCategory.tabs.forEach { tab ->
                        val isActive = tab.id == activeTabObj.id
                        var isFocused by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isActive) {
                                        Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFF1A2948), Color(0xFF1E3259)))
                                    }
                                )
                                .border(
                                    width = if (isFocused) 3.dp else if (isActive) 2.dp else 1.dp,
                                    color = when {
                                        isFocused -> TvFocusAmber
                                        isActive -> Color(0xFF93C5FD)
                                        else -> Color(0xFF64748B).copy(alpha = 0.5f)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clickable { activeTabId = tab.id }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isActive) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tab.label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // ==================== 4. BOTTOM 5-CATEGORY BAR (#060913) ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF060913))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filteredCategories.forEach { cat ->
                    val isSelected = cat.id == activeCategory.id
                    var isFocused by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isSelected -> cat.accentColor.copy(alpha = 0.2f)
                                    isFocused -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isFocused -> TvFocusAmber
                                    isSelected -> cat.accentColor
                                    else -> Color(0xFF1E293B)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .clickable {
                                activeCategoryId = cat.id
                                if (cat.tabs.none { it.id == activeTabId }) {
                                    activeTabId = cat.tabs.first().id
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 48.dp else 0.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) cat.accentColor else Color.Transparent)
                        )
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.label,
                            tint = if (isSelected) cat.accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = cat.label.uppercase(),
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Ta'jil Excel Import Preview Modal
        if (takjilImportPreview != null) {
            TakjilExcelPreviewDialog(
                previewRows = takjilImportPreview,
                onConfirm = onConfirmTakjilImport,
                onCancel = onDismissTakjilImport
            )
        }
    }
}

@Composable
private fun TakjilExcelPreviewDialog(
    previewRows: List<TakjilImportPreviewRow>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val totalNames = previewRows.sumOf { it.names.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accentSecondary, RoundedCornerShape(22.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Preview Import Jadwal Ta'jil Excel (.xlsx)",
                color = palette.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Ditemukan ${previewRows.size} hari dengan total $totalNames nama donatur:",
                color = palette.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                previewRows.forEach { row ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.bgSecondary)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Hari ke-${row.day}: ${row.names.joinToString(", ")}",
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                HybridButton(label = "Batal", onClick = onCancel)
                Spacer(modifier = Modifier.width(12.dp))
                HybridButton(
                    label = "Terapkan ($totalNames Nama)",
                    isPrimary = true,
                    onClick = onConfirm
                )
            }
        }
    }
}
