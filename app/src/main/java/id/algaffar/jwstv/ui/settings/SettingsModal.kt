package id.algaffar.jwstv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Android TV Leanback-inspired Two-Pane Master-Detail Settings UI (`d-pad.jpg`):
 * - Slim Top Bar (`SETTINGS • HH:mm`, Active Category, Simulasi, User/Logout, Tutup)
 * - Left Master Pane (27% width): 5 Main Categories with Cyan (`#38BDF8`) focus ring, subtle scale,
 *   live right-pane preview on D-Pad Up/Down, and Mouse Hover → Focus synchronization.
 * - Right Detail Pane (73% width): Modular Sub-Tab Bar (max 4 pills per category),
 *   readable max-width content area, Level-3 Slide-Over Drawers, Focus Memory, and Contextual Footer Hints.
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
    var authenticatedUser by remember { mutableStateOf<UserAccount?>(null) }

    val currentUser = authenticatedUser
    if (currentUser == null) {
        PinLoginGuardScreen(
            users = settings.users,
            deviceCode = actions.deviceCode,
            onAuthenticated = { authenticatedUser = it },
            onUpgradePinHash = actions.onUpgradeUserPinHash,
            onResetSuperadminPin = actions.onResetSuperadminPin,
            onCancel = onClose
        )
        return
    }

    val activeRole = remember(currentUser.role) { UserRole.fromId(currentUser.role) }
    val filteredCategories = remember(activeRole) {
        SettingsNavigationModel.filterForRole(activeRole)
    }

    var activeCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.id ?: "masjid")
    }
    val activeCategory = filteredCategories.firstOrNull { it.id == activeCategoryId }
        ?: filteredCategories.first()

    // Remember last active sub-tab per category for seamless switching
    var activeTabIdByCategory by remember(filteredCategories) {
        mutableStateOf(
            filteredCategories.associate { cat -> cat.id to (cat.tabs.firstOrNull()?.id ?: "") }
        )
    }
    val activeTabId = activeTabIdByCategory[activeCategory.id]
        ?.takeIf { id -> activeCategory.tabs.any { it.id == id } }
        ?: activeCategory.tabs.first().id
    val activeTabObj = activeCategory.tabs.firstOrNull { it.id == activeTabId }
        ?: activeCategory.tabs.first()

    var focusZone by remember { mutableStateOf(SettingsFocusZone.MASTER_PANE) }
    val categoryFocusRequesters = remember(filteredCategories) {
        filteredCategories.associate { it.id to FocusRequester() }
    }
    val subTabFocusRequesters = remember(filteredCategories) {
        filteredCategories.flatMap { it.tabs }.associate { it.id to FocusRequester() }
    }
    val contentScrollState = rememberScrollState()
    val masterScrollState = rememberScrollState()

    // Live header clock (HH:mm)
    var headerClock by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) {
        while (true) {
            headerClock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            delay(15_000L)
        }
    }

    // Place initial focus on active category in Master Pane upon login
    LaunchedEffect(currentUser.id) {
        delay(80L)
        runCatching { categoryFocusRequesters[activeCategory.id]?.requestFocus() }
    }

    // Reset detail scroll position when switching sub-tab
    LaunchedEffect(activeCategory.id, activeTabObj.id) {
        contentScrollState.scrollTo(0)
    }

    val rightPaneOverlayHost = remember { SettingsOverlayHostState() }
    val modalOverlayHost = remember { SettingsOverlayHostState() }
    val hasRightDrawer = rightPaneOverlayHost.hasActiveOverlay
    val hasModalOverlay = modalOverlayHost.hasActiveOverlay
    val hasAnyOverlay = hasRightDrawer || hasModalOverlay

    var wasRightDrawerOpen by remember { mutableStateOf(false) }
    LaunchedEffect(hasRightDrawer) {
        if (wasRightDrawerOpen && !hasRightDrawer) {
            delay(16L)
            runCatching { rightPaneOverlayHost.lastFocusForDepth(0)?.requestFocus() }
        }
        wasRightDrawerOpen = hasRightDrawer
    }

    fun selectCategory(cat: SettingsCategorySpec) {
        if (rightPaneOverlayHost.hasActiveOverlay) {
            rightPaneOverlayHost.dismissAll()
        }
        if (activeCategoryId != cat.id) {
            activeCategoryId = cat.id
        }
    }

    fun selectSubTab(tabId: String) {
        if (rightPaneOverlayHost.hasActiveOverlay) {
            rightPaneOverlayHost.dismissAll()
        }
        activeTabIdByCategory = activeTabIdByCategory + (activeCategory.id to tabId)
    }

    fun focusActiveCategoryInMaster() {
        focusZone = SettingsFocusZone.MASTER_PANE
        runCatching { categoryFocusRequesters[activeCategory.id]?.requestFocus() }
    }

    // Smart Back navigation: Modal/Drawer -> Detail -> Master Pane (Focus Memory) -> Close Modal
    BackHandler(enabled = takjilImportPreview == null && !hasAnyOverlay) {
        if (focusZone != SettingsFocusZone.MASTER_PANE) {
            focusActiveCategoryInMaster()
        } else {
            onClose()
        }
    }

    CompositionLocalProvider(
        LocalRightPaneOverlayHost provides rightPaneOverlayHost,
        LocalModalOverlayHost provides modalOverlayHost,
        LocalOverlayLayerDepth provides 0,
        LocalIsActiveFocusLayer provides (!hasAnyOverlay)
    ) {
    @OptIn(ExperimentalComposeUiApi::class)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvSurfaceCanvas)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape && takjilImportPreview == null && !hasAnyOverlay) {
                    if (focusZone != SettingsFocusZone.MASTER_PANE) {
                        focusActiveCategoryInMaster()
                        true
                    } else {
                        onClose()
                        true
                    }
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==================== 1. SLIM TOP HEADER BAR ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfacePane)
                    .border(1.dp, TvSurfaceBorder)
                    .focusProperties { if (hasAnyOverlay) enter = { FocusRequester.Cancel } }
                    .focusGroup()
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TvFocusCyan.copy(alpha = 0.16f))
                            .border(1.dp, TvFocusCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = TvFocusCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PENGATURAN JWS  •  $headerClock",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TvFocusCyan.copy(alpha = 0.14f))
                                    .border(1.dp, TvFocusCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = TvFocusCyan, modifier = Modifier.size(11.dp))
                                Text("D-PAD + MOUSE", color = TvFocusCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(
                            text = "${activeCategory.label}  ›  ${activeTabObj.label}",
                            color = Color(0xFFF8FAFC),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HybridButton(
                        label = "${currentUser.username} (${activeRole.id.uppercase()})",
                        icon = Icons.AutoMirrored.Filled.Logout,
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

            // ==================== 2. TWO-PANE MASTER-DETAIL BODY (30% : 70%) ====================
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // ---------- LEFT MASTER PANE (30% WIDTH) ----------
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.30f)
                        .background(TvSurfacePane)
                        .border(1.dp, TvSurfaceBorder)
                        .focusProperties { if (hasAnyOverlay) enter = { FocusRequester.Cancel } }
                        .focusGroup()
                        .verticalScroll(masterScrollState)
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "KATEGORI UTAMA",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )

                    filteredCategories.forEach { cat ->
                        val isSelected = cat.id == activeCategory.id
                        val requester = categoryFocusRequesters[cat.id] ?: remember { FocusRequester() }
                        var isFocused by remember { mutableStateOf(false) }
                        var isHovered by remember { mutableStateOf(false) }
                        val cardScale by animateFloatAsState(
                            targetValue = if (isFocused && isSelected) 1.02f else 1f,
                            label = "masterScale"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(cardScale)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected && isFocused -> TvFocusCyan.copy(alpha = 0.18f)
                                        isSelected -> cat.accentColor.copy(alpha = 0.14f)
                                        isHovered -> TvSurfaceRowFocused
                                        else -> TvSurfaceRow.copy(alpha = 0.72f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) (if (isFocused) 2.5.dp else 1.8.dp) else 1.dp,
                                    color = when {
                                        isSelected && isFocused -> TvFocusCyan
                                        isSelected -> cat.accentColor.copy(alpha = 0.85f)
                                        else -> TvSurfaceBorder
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .focusRequester(requester)
                                .pointerInput(cat.id, hasAnyOverlay) {
                                    if (hasAnyOverlay) return@pointerInput
                                    awaitPointerEventScope {
                                        while (true) {
                                            val ev = awaitPointerEvent(PointerEventPass.Main)
                                            when (ev.type) {
                                                PointerEventType.Enter -> isHovered = true
                                                PointerEventType.Exit -> isHovered = false
                                            }
                                        }
                                    }
                                }
                                .onFocusChanged { state ->
                                    isFocused = state.isFocused
                                    if (state.isFocused) {
                                        focusZone = SettingsFocusZone.MASTER_PANE
                                        selectCategory(cat)
                                    }
                                }
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (keyEvent.key) {
                                        Key.DirectionRight, Key.DirectionCenter, Key.Enter -> {
                                            selectCategory(cat)
                                            val targetTabId = activeTabIdByCategory[cat.id]
                                                ?.takeIf { id -> cat.tabs.any { it.id == id } }
                                                ?: cat.tabs.first().id
                                            focusZone = SettingsFocusZone.DETAIL_SUBTABS
                                            runCatching { subTabFocusRequesters[targetTabId]?.requestFocus() }
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .focusable()
                                .clickable {
                                    selectCategory(cat)
                                    val targetTabId = activeTabIdByCategory[cat.id]
                                        ?.takeIf { id -> cat.tabs.any { it.id == id } }
                                        ?: cat.tabs.first().id
                                    focusZone = SettingsFocusZone.DETAIL_SUBTABS
                                    runCatching { subTabFocusRequesters[targetTabId]?.requestFocus() }
                                }
                                .padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Vertical active indicator bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFocused -> TvFocusCyan
                                            isSelected -> cat.accentColor
                                            else -> Color.Transparent
                                        }
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (isFocused) TvFocusCyan.copy(alpha = 0.22f)
                                        else cat.accentColor.copy(alpha = if (isSelected) 0.22f else 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = if (isFocused) TvFocusCyan else cat.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.label,
                                    color = Color(0xFFF8FAFC),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${cat.tabs.size} Sub-Menu",
                                    color = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isFocused) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(TvFocusCyan.copy(alpha = 0.22f))
                                        .border(1.5.dp, TvFocusCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ControlCamera,
                                        contentDescription = null,
                                        tint = TvFocusCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Compact Role & Connection Info Card at bottom of Master Pane
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TvSurfaceCanvas)
                            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "REMOTE PWA PORT 1421",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = actions.remoteServerUrl.ifBlank { "Menunggu jaringan WiFi..." },
                            color = TvFocusCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ---------- RIGHT DETAIL PANE (70% WIDTH) ----------
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.70f)
                        .background(TvSurfaceCanvas)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TvSurfaceCanvas)
                        .focusProperties { if (hasAnyOverlay) enter = { FocusRequester.Cancel } }
                        .focusGroup()
                ) {
                    // Detail Header + Modular Sub-Tab Pills (Max 4 Pills — Single Row!)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvSurfaceCanvas)
                            .padding(horizontal = 32.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = activeTabObj.label,
                                color = Color(0xFFF8FAFC),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = activeTabObj.subtitle,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeCategory.tabs.forEachIndexed { idx, tab ->
                                val isActive = tab.id == activeTabObj.id
                                val pillRequester = subTabFocusRequesters[tab.id]
                                    ?: remember(activeCategory.id, tab.id) { FocusRequester() }
                                var pillFocused by remember { mutableStateOf(false) }
                                var pillHovered by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isActive && pillFocused -> TvFocusCyan.copy(alpha = 0.24f)
                                                isActive -> activeCategory.accentColor.copy(alpha = 0.20f)
                                                pillHovered -> TvSurfaceRowFocused
                                                else -> TvSurfaceRow
                                            }
                                        )
                                        .border(
                                            width = if (isActive) (if (pillFocused) 2.5.dp else 1.8.dp) else 1.dp,
                                            color = when {
                                                isActive && pillFocused -> TvFocusCyan
                                                isActive -> activeCategory.accentColor
                                                else -> TvSurfaceBorder
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .focusRequester(pillRequester)
                                        .pointerInput(tab.id, hasAnyOverlay) {
                                            if (hasAnyOverlay) return@pointerInput
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                                    when (ev.type) {
                                                        PointerEventType.Enter -> pillHovered = true
                                                        PointerEventType.Exit -> pillHovered = false
                                                    }
                                                }
                                            }
                                        }
                                        .onFocusChanged { state ->
                                            pillFocused = state.isFocused
                                            if (state.isFocused) {
                                                focusZone = SettingsFocusZone.DETAIL_SUBTABS
                                                rightPaneOverlayHost.recordFocus(0, pillRequester)
                                                selectSubTab(tab.id)
                                            }
                                        }
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown &&
                                                keyEvent.key == Key.DirectionLeft && idx == 0
                                            ) {
                                                focusActiveCategoryInMaster()
                                                true
                                            } else false
                                        }
                                        .focusable()
                                        .clickable {
                                            selectSubTab(tab.id)
                                            runCatching { pillRequester.requestFocus() }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = when {
                                            isActive && pillFocused -> TvFocusCyan
                                            isActive -> activeCategory.accentColor
                                            pillHovered -> Color(0xFFE2E8F0)
                                            else -> Color(0xFF94A3B8)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        color = if (isActive || pillHovered) Color(0xFFF8FAFC) else Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TvSurfaceBorder)
                    )

                    // Scrollable Modular Content Area (Readable 10-foot max-width container)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onFocusChanged { state ->
                                if (state.hasFocus) {
                                    focusZone = SettingsFocusZone.DETAIL_CONTENT
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(contentScrollState)
                                .padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 1160.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (statusBannerMessage.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(TvFocusCyan.copy(alpha = 0.14f))
                                            .border(1.5.dp, TvFocusCyan, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 18.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = statusBannerMessage,
                                            color = TvFocusCyan,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                when (activeTabObj.id) {
                                    // Category 1: Masjid
                                    "brand" -> BrandSettingsTab(settings, actions)
                                    "masjidQr" -> MasjidQrSettingsTab(settings, actions)
                                    "onlineFinance" -> OnlineFinanceSettingsTab(settings, actions)

                                    // Category 2: Sholat & Audio
                                    "prayerLocation" -> PrayerLocationSettingsTab(settings, actions)
                                    "prayerStages" -> PrayerStagesSettingsTab(settings, actions)
                                    "audio" -> AudioSholatSettingsTab(settings, actions)
                                    "customAudio" -> CustomAudioSettingsTab(settings, actions)

                                    // Category 3: Tampilan
                                    "visualStyle" -> VisualStyleSettingsTab(settings, actions)
                                    "overlayInfo" -> OverlayAndInfoSettingsTab(settings, actions)
                                    "slides" -> SlideSettingsTab(settings, actions)
                                    "mediaStandby" -> MediaAndStandbySettingsTab(settings, actions)

                                    // Category 4: Acara & Perangkat
                                    "jumatRamadhan" -> JumatAndRamadhanSettingsTab(settings, actions)
                                    "hariRayaQurban" -> HariRayaAndQurbanSettingsTab(settings, actions)
                                    "liveScreensaver" -> LiveAndScreensaverSettingsTab(settings, actions, activeRole)
                                    "switches" -> SwitchHardwareSettingsTab(settings, actions)

                                    // Category 5: Sistem
                                    "network" -> RemoteNetworkSettingsTab(actions)
                                    "accessInput" -> AccessAndInputSettingsTab(settings, actions, activeRole)
                                    "backupUpdate" -> BackupAndUpdateSettingsTab(settings, actions)
                                    "logsDevice" -> LogsAndDeviceSettingsTab(settings, actions, activeRole)
                                }
                            }
                        }
                    }

                    // Contextual Footer Key Hints Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvSurfacePane)
                            .border(1.dp, TvSurfaceBorder)
                            .padding(horizontal = 32.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SettingsNavigationModel.footerHintForZone(focusZone),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "FOKUS: ${focusZone.name.replace('_', ' ')}",
                            color = TvFocusCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Level-3 Detail Drawer Overlay aligned 1:1 with the 70% Right Detail Pane
                if (hasRightDrawer) {
                    val topRightId = rightPaneOverlayHost.entries.lastOrNull()?.id
                    rightPaneOverlayHost.entries.forEach { entry ->
                        key(entry.id) {
                            CompositionLocalProvider(
                                LocalOverlayLayerDepth provides 1,
                                LocalIsActiveFocusLayer provides (!hasModalOverlay && entry.id == topRightId)
                            ) {
                                entry.content()
                            }
                        }
                    }
                }
                }
            }
        }

        // In-Canvas Modal Picker Overlay (e.g. HybridSelectDropdown) rendered at 1:1 1920x1080 scale
        if (hasModalOverlay) {
            val topModalId = modalOverlayHost.entries.lastOrNull()?.id
            modalOverlayHost.entries.forEach { entry ->
                key(entry.id) {
                    CompositionLocalProvider(
                        LocalOverlayLayerDepth provides 2,
                        LocalIsActiveFocusLayer provides (entry.id == topModalId)
                    ) {
                        entry.content()
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
                .background(TvSurfacePane)
                .border(2.dp, TvFocusCyan, RoundedCornerShape(22.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Preview Import Jadwal Ta'jil Excel (.xlsx)",
                color = Color(0xFFF8FAFC),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Ditemukan ${previewRows.size} hari dengan total $totalNames nama donatur:",
                color = TvFocusCyan,
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
                            .background(TvSurfaceRow)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Hari ke-${row.day}: ${row.names.joinToString(", ")}",
                            color = Color(0xFFF8FAFC),
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
