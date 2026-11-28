package id.algaffar.jwstv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.OfflinePinRecoveryEngine
import id.algaffar.jwstv.data.auth.PinLockoutTracker
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Unified high-contrast Vibrant Cyan (#38BDF8) focus ring for Android TV D-Pad & Mouse hover. */
val TvFocusCyan = Color(0xFF38BDF8)

/** Legacy alias kept for Amber badge/status accents; focus rings use [TvFocusCyan]. */
val TvFocusAmber = Color(0xFFFBBF24)

/** Clean dark surface tokens aligned with Leanback Two-Pane Settings UI. */
val TvSurfaceCanvas = Color(0xFF0B0F17)
val TvSurfacePane = Color(0xFF0F1521)
val TvSurfaceRow = Color(0xFF151E2E)
val TvSurfaceRowFocused = Color(0xFF1B2940)
val TvSurfaceBorder = Color(0xFF263348)

/**
 * In-Canvas Overlay Entry & Host State so Level-3 Drawers and Select Modals render
 * directly inside `Responsive1080pCanvas` (1920x1080 virtual reference coordinates)
 * at 1:1 proportional scale instead of escaping into an unscaled Android OS `Dialog` window.
 */
class SettingsOverlayEntry(
    val id: Any,
    onDismissInit: () -> Unit,
    contentInit: @Composable () -> Unit
) {
    var onDismiss by mutableStateOf(onDismissInit)
    var content by mutableStateOf(contentInit)
}

class SettingsOverlayHostState {
    val entries = mutableStateListOf<SettingsOverlayEntry>()
    private val lastFocusedByDepth = mutableMapOf<Int, FocusRequester>()

    val hasActiveOverlay: Boolean
        get() = entries.isNotEmpty()

    fun attach(entry: SettingsOverlayEntry) {
        if (entries.none { it.id == entry.id }) {
            entries.add(entry)
        }
    }

    fun detach(id: Any) {
        entries.removeAll { it.id == id }
    }

    fun dismissTop(): Boolean {
        val top = entries.lastOrNull() ?: return false
        top.onDismiss()
        return true
    }

    fun dismissAll() {
        entries.toList().asReversed().forEach { it.onDismiss() }
    }

    fun recordFocus(depth: Int, requester: FocusRequester) {
        lastFocusedByDepth[depth] = requester
    }

    fun lastFocusForDepth(depth: Int): FocusRequester? = lastFocusedByDepth[depth]
}

val LocalRightPaneOverlayHost = compositionLocalOf<SettingsOverlayHostState?> { null }
val LocalModalOverlayHost = compositionLocalOf<SettingsOverlayHostState?> { null }
val LocalOverlayLayerDepth = compositionLocalOf { 0 }
val LocalIsActiveFocusLayer = compositionLocalOf { true }

/**
 * Synchronizes Mouse Hover (`PointerEventType.Enter`) directly to D-Pad/Keyboard Focus
 * so switching between mouse and remote/keyboard continues seamlessly from the hovered item,
 * while recording per-layer focus memory for automatic restoration on drawer close.
 */
@Composable
fun Modifier.syncHoverToFocus(
    focusRequester: FocusRequester,
    enabled: Boolean = true
): Modifier {
    val isActiveLayer = LocalIsActiveFocusLayer.current
    val layerDepth = LocalOverlayLayerDepth.current
    val rightPaneHost = LocalRightPaneOverlayHost.current

    val tracked = this.onFocusChanged { state ->
        if (state.isFocused) {
            rightPaneHost?.recordFocus(layerDepth, focusRequester)
        }
    }
    return if (!enabled || !isActiveLayer) {
        tracked
    } else {
        tracked.pointerInput(focusRequester) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type == PointerEventType.Enter) {
                        runCatching { focusRequester.requestFocus() }
                    }
                }
            }
        }
    }
}

/**
 * Bundled callbacks passed to the modular settings tab screens.
 */
data class SettingsActions(
    val onUpdateSettings: ((AppStateSettings) -> AppStateSettings) -> Unit,
    val onExportTakjilTemplate: () -> Unit,
    val onImportTakjilExcel: () -> Unit,
    val onExportBackupZip: () -> Unit,
    val onImportBackupZip: () -> Unit,
    val onSyncUsbFolder: () -> Unit,
    val onRefreshStorageHealth: () -> Unit,
    val onCleanOrphanMedia: () -> Unit,
    val onOpenDebugSimulator: () -> Unit,
    val onToggleManualRelay: (Int, Boolean) -> Unit,
    val onTestOnlineFinance: () -> Unit,
    val onStartDirectLive: (source: String, customId: String, durationMinutes: Int) -> Unit,
    val onStopDirectLive: () -> Unit,
    val onPickImageAsset: (subFolder: String, onSaved: (String) -> Unit) -> Unit,
    val onPickVideoAsset: (onSaved: (path: String, title: String) -> Unit) -> Unit,
    val onPickPrayerAudio: (category: String, slotName: String) -> Unit,
    val onDeletePrayerAudio: (category: String, slotName: String) -> Unit,
    val onTogglePreviewPrayerAudio: (category: String, slotName: String) -> Unit,
    val onPickCustomAudio: () -> Unit,
    val onDeleteCustomAudio: (fileName: String) -> Unit,
    val onTogglePreviewCustomAudio: (fileName: String) -> Unit,
    val onFindPrayerAudioSlot: (category: String, slotName: String) -> Pair<Boolean, String>,
    val onListCustomAudioFiles: () -> List<Pair<String, String>>,
    val assetsRootDir: File?,
    val storageHealth: StorageHealthSnapshot,
    val remoteServerUrl: String,
    val remoteQrPairingUrl: String,
    val remoteAuthToken: String,
    val deviceCode: String = "",
    val relayPinStates: Map<Int, Boolean>,
    val onlineFinanceTestResult: FinancialSummaryData?,
    val audioInventoryTick: Int,
    val previewingAudioKey: String?,
    val isLiveActiveNow: Boolean,
    val onRegenerateRemoteToken: () -> Unit = {},
    val onResetSuperadminPin: (newPin: String) -> Unit = {},
    val onUpgradeUserPinHash: (userId: String, rawPin: String) -> Unit = { _, _ -> },
    val onRequestUsbPermission: () -> Unit = {},
    val onReopenOnboarding: () -> Unit = {},
    val onCheckOtaUpdate: () -> Unit = {},
    val onInstallOtaApkFromUsb: () -> Unit = {},
    val onDownloadAudioPack: (packId: String) -> Unit = {},
    val onDownloadImagePack: (packId: String) -> Unit = {},
    val isDeviceOwner: Boolean = false,
    val isKioskMaintenanceUnlocked: Boolean = false,
    val onToggleKioskMaintenance: () -> Unit = {},
    val onOpenAndroidSettings: () -> Unit = {},
    val onOpenWifiSettings: () -> Unit = {},
    val onTriggerAppRestartNow: () -> Unit = {},
    val onTriggerDeviceRebootNow: () -> Unit = {}
)

@Composable
fun HybridSectionCard(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TvSurfacePane.copy(alpha = 0.92f))
            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFF8FAFC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            if (headerAction != null) {
                Spacer(modifier = Modifier.width(12.dp))
                headerAction()
            }
        }
        content()
    }
}

@Composable
fun HybridInfoBanner(
    text: String,
    accentColor: Color = TvFocusCyan,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.11f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
fun HybridButton(
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        !enabled -> TvSurfaceRow.copy(alpha = 0.4f)
        isDanger -> if (isFocused) Color(0xFFDC2626) else Color(0xFF991B1B)
        isPrimary -> if (isFocused) TvFocusCyan else Color(0xFF0EA5E9)
        isFocused -> TvSurfaceRowFocused
        else -> TvSurfaceRow
    }
    val textColor = when {
        !enabled -> Color(0xFF64748B)
        isDanger -> Color.White
        isPrimary -> Color(0xFF04111E)
        else -> Color(0xFFF8FAFC)
    }
    val borderColor = when {
        isFocused -> TvFocusCyan
        isDanger -> Color(0xFFEF4444).copy(alpha = 0.55f)
        isPrimary -> TvFocusCyan.copy(alpha = 0.7f)
        else -> TvSurfaceBorder
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isFocused) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester, enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HybridSegmentedRow(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (id, label) ->
            HybridButton(
                label = label,
                isPrimary = (id == selectedId),
                onClick = { onSelect(id) }
            )
        }
    }
}

data class HybridSelectOption(
    val id: String,
    val label: String,
    val subtitle: String = ""
)

/**
 * TV D-Pad & Mouse friendly select/dropdown control.
 * Renders a compact select box that opens a focus-trapped scrollable modal picker dialog on click/Enter.
 */
@Composable
fun HybridSelectDropdown(
    label: String,
    selectedId: String,
    options: List<HybridSelectOption>,
    onSelect: (String) -> Unit,
    placeholder: String = "Pilih...",
    dialogTitle: String = label,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val selectedOption = remember(selectedId, options) {
        options.firstOrNull { it.id == selectedId }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            color = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isFocused) TvSurfaceRowFocused else TvSurfaceRow)
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .focusRequester(focusRequester)
                .syncHoverToFocus(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .clickable { showDialog = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedOption?.label ?: placeholder,
                    color = if (selectedOption != null) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!selectedOption?.subtitle.isNullOrBlank()) {
                    Text(
                        text = selectedOption!!.subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showDialog) {
        val modalHost = LocalModalOverlayHost.current
        val dismissPicker: () -> Unit = {
            showDialog = false
            runCatching { focusRequester.requestFocus() }
        }
        val pickerBody: @Composable () -> Unit = {
            val initialIdx = remember(selectedId, options) {
                options.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            }
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)
            val firstItemFocus = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                delay(16L)
                runCatching { firstItemFocus.requestFocus() }
            }

            BackHandler(enabled = true) {
                dismissPicker()
            }

            @OptIn(ExperimentalComposeUiApi::class)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.68f))
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusGroup()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                            dismissPicker()
                            true
                        } else false
                    }
                    .clickable { dismissPicker() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 520.dp, max = 720.dp)
                        .heightIn(max = 640.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurfacePane)
                        .border(2.dp, TvFocusCyan.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .clickable(enabled = false) {}
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dialogTitle,
                            color = Color(0xFFF8FAFC),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        HybridButton(
                            label = "Tutup",
                            icon = Icons.Default.Close,
                            onClick = dismissPicker
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(options, key = { it.id }) { opt ->
                            val isSelected = opt.id == selectedId
                            val itemRequester = remember { FocusRequester() }
                            var itemFocused by remember { mutableStateOf(false) }
                            val isTargetInitial = (opt.id == selectedId) || (selectedOption == null && opt == options.firstOrNull())

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            itemFocused -> TvSurfaceRowFocused
                                            isSelected -> TvFocusCyan.copy(alpha = 0.14f)
                                            else -> TvSurfaceRow
                                        }
                                    )
                                    .border(
                                        width = if (itemFocused) 2.5.dp else 1.dp,
                                        color = when {
                                            itemFocused -> TvFocusCyan
                                            isSelected -> TvFocusCyan.copy(alpha = 0.65f)
                                            else -> TvSurfaceBorder
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .then(if (isTargetInitial) Modifier.focusRequester(firstItemFocus) else Modifier.focusRequester(itemRequester))
                                    .syncHoverToFocus(if (isTargetInitial) firstItemFocus else itemRequester)
                                    .onFocusChanged { itemFocused = it.isFocused }
                                    .focusable()
                                    .clickable {
                                        onSelect(opt.id)
                                        dismissPicker()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.label,
                                        color = if (isSelected) TvFocusCyan else Color(0xFFF8FAFC),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                    if (opt.subtitle.isNotBlank()) {
                                        Text(
                                            text = opt.subtitle,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TvFocusCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (modalHost != null) {
            val entryId = remember { Any() }
            val entry = remember(entryId) { SettingsOverlayEntry(entryId, dismissPicker, pickerBody) }
            entry.onDismiss = dismissPicker
            entry.content = pickerBody
            DisposableEffect(modalHost, entryId) {
                modalHost.attach(entry)
                onDispose { modalHost.detach(entryId) }
            }
        } else {
            Dialog(
                onDismissRequest = dismissPicker,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                pickerBody()
            }
        }
    }
}

/**
 * Tactile Pill Toggle Switch row modeled after Android TV Leanback settings (`d-pad.jpg`).
 * Single D-Pad focus stop + Mouse hover-to-focus sync.
 */
@Composable
fun HybridToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val trackColor by animateColorAsState(
        targetValue = if (checked) TvFocusCyan else Color(0xFF334155),
        label = "toggleTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "toggleThumb"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) TvSurfaceRowFocused else TvSurfaceRow)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused || checked) TvFocusCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color(0xFFF8FAFC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (checked) "AKTIF" else "NONAKTIF",
                color = if (checked) TvFocusCyan else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(trackColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * Single-Focus Inline Numeric Stepper (`< nilai >`).
 * - D-Pad: Focus the entire row in 1 stop, press Left/Right to decrement/increment immediately.
 * - Mouse / Touch: Click `<` or `>` chips directly (hover syncs focus to the row).
 */
@Composable
fun HybridNumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = -60,
    max: Int = 999,
    step: Int = 1,
    unit: String = "menit",
    enabled: Boolean = true,
    description: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    fun adjust(deltaSteps: Int) {
        if (!enabled) return
        val next = SettingsNavigationModel.computeSteppedInt(value, deltaSteps, min, max, step)
        if (next != value) onValueChange(next)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    !enabled -> TvSurfaceRow.copy(alpha = 0.4f)
                    isFocused -> TvSurfaceRowFocused
                    else -> TvSurfaceRow
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester, enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (!enabled || keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        adjust(-1)
                        true
                    }
                    Key.DirectionRight -> {
                        adjust(+1)
                        true
                    }
                    else -> false
                }
            }
            .focusable(enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (enabled) Color(0xFFF8FAFC) else Color(0xFF64748B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (enabled && value > min) {
                            if (isFocused) TvFocusCyan else Color(0xFF1E293B)
                        } else Color(0xFF111827)
                    )
                    .border(
                        1.dp,
                        if (isFocused && enabled) TvFocusCyan else TvSurfaceBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = enabled && value > min) { adjust(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Kurangi",
                    tint = if (isFocused && enabled && value > min) Color(0xFF04111E) else Color(0xFFF8FAFC),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = if (unit.isBlank()) "$value" else "$value $unit",
                color = if (enabled) Color(0xFFF8FAFC) else Color(0xFF64748B),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 84.dp)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (enabled && value < max) {
                            if (isFocused) TvFocusCyan else Color(0xFF1E293B)
                        } else Color(0xFF111827)
                    )
                    .border(
                        1.dp,
                        if (isFocused && enabled) TvFocusCyan else TvSurfaceBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = enabled && value < max) { adjust(+1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Tambah",
                    tint = if (isFocused && enabled && value < max) Color(0xFF04111E) else Color(0xFFF8FAFC),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Single-Focus Inline Option Stepper (`< Opsi >`) for 2–4 discrete choices.
 * Modeled directly after `Resolution < 4K 60Hz >` in `d-pad.jpg`.
 */
@Composable
fun HybridInlineOptionStepper(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    description: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val currentIndex = options.indexOfFirst { it.first == selectedId }.coerceAtLeast(0)
    val currentLabel = options.getOrNull(currentIndex)?.second ?: selectedId

    fun stepOption(delta: Int) {
        if (!enabled || options.isEmpty()) return
        val nextIdx = SettingsNavigationModel.computeSteppedOptionIndex(currentIndex, delta, options.size)
        onSelect(options[nextIdx].first)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) TvSurfaceRowFocused else TvSurfaceRow)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester, enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (!enabled || keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        stepOption(-1)
                        true
                    }
                    Key.DirectionRight, Key.DirectionCenter, Key.Enter -> {
                        stepOption(+1)
                        true
                    }
                    else -> false
                }
            }
            .focusable(enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color(0xFFF8FAFC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isFocused) TvFocusCyan else Color(0xFF1E293B))
                    .clickable(enabled = enabled) { stepOption(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Sebelumnya",
                    tint = if (isFocused) Color(0xFF04111E) else Color(0xFFF8FAFC),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = currentLabel,
                color = if (isFocused) TvFocusCyan else Color(0xFFF8FAFC),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 120.dp, max = 230.dp)
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isFocused) TvFocusCyan else Color(0xFF1E293B))
                    .clickable(enabled = enabled) { stepOption(+1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Berikutnya",
                    tint = if (isFocused) Color(0xFF04111E) else Color(0xFFF8FAFC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Horizontal Slider Row (`System Volume ━━━━●━━ 85%`) supporting D-Pad Left/Right & Mouse click/drag.
 */
@Composable
fun HybridSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 100,
    step: Int = 5,
    unit: String = "%",
    description: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableStateOf(1f) }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val fraction = ((value - min).toFloat() / (max - min).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    fun updateFromX(xPx: Float) {
        val ratio = (xPx / trackWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val raw = min + (ratio * (max - min))
        val snapped = (min + ((raw - min) / step).roundToInt() * step).coerceIn(min, max)
        if (snapped != value) latestOnValueChange(snapped)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) TvSurfaceRowFocused else TvSurfaceRow)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        val next = SettingsNavigationModel.computeSteppedInt(value, -1, min, max, step)
                        if (next != value) onValueChange(next)
                        true
                    }
                    Key.DirectionRight -> {
                        val next = SettingsNavigationModel.computeSteppedInt(value, +1, min, max, step)
                        if (next != value) onValueChange(next)
                        true
                    }
                    else -> false
                }
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (description.isNotBlank()) {
                    Text(description, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(28.dp)
                    .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(min, max, step) {
                        detectTapGestures { offset -> updateFromX(offset.x) }
                    }
                    .pointerInput(min, max, step) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            updateFromX(change.position.x)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(TvFocusCyan)
                )
                val thumbX = (fraction * 200f).dp
                Box(
                    modifier = Modifier
                        .offset(x = thumbX)
                        .size(if (isFocused) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, TvFocusCyan, CircleShape)
                )
            }

            Text(
                text = "$value$unit",
                color = if (isFocused) TvFocusCyan else Color(0xFFF8FAFC),
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp)
            )
        }
    }
}

/**
 * Level-3 Navigation Row (`Sound Output — [HDMI ARC ❯]`) that opens a Slide-Over Side Drawer or Modal.
 */
@Composable
fun HybridNavigationRow(
    label: String,
    valueSummary: String,
    onClick: () -> Unit,
    description: String = "",
    icon: ImageVector? = null,
    accentBadgeColor: Color = TvFocusCyan,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) TvSurfaceRowFocused else TvSurfaceRow)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .focusRequester(focusRequester)
            .syncHoverToFocus(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight) {
                    onClick()
                    true
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) TvFocusCyan else accentBadgeColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color(0xFFF8FAFC),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(if (isFocused) TvFocusCyan.copy(alpha = 0.2f) else Color(0xFF0F172A))
                .border(
                    1.dp,
                    if (isFocused) TvFocusCyan else TvSurfaceBorder,
                    RoundedCornerShape(99.dp)
                )
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = valueSummary,
                color = if (isFocused) TvFocusCyan else Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Level-3 Detail Panel / Slide-Over Drawer aligned 1:1 with the 70% Right Detail Pane
 * inside `Responsive1080pCanvas` (`1920x1080` virtual reference coordinates).
 * Matches exact typography, row heights, padding, and footer bar of the main settings pane,
 * with strict D-Pad focus containment and `Back` / `Esc` dismissal.
 */
@Composable
fun HybridSideDrawer(
    title: String,
    subtitle: String = "",
    accentColor: Color = TvFocusCyan,
    onDismiss: () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val rightPaneHost = LocalRightPaneOverlayHost.current
    val drawerPanel: @Composable () -> Unit = {
        val closeFocus = remember { FocusRequester() }
        val bodyFocus = remember { FocusRequester() }
        var bodyHasFocus by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            delay(16L)
            runCatching { bodyFocus.requestFocus() }
            delay(24L)
            if (!bodyHasFocus) {
                runCatching { closeFocus.requestFocus() }
            }
        }

        BackHandler(enabled = true) {
            onDismiss()
        }

        @OptIn(ExperimentalComposeUiApi::class)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TvSurfaceCanvas)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else false
                }
                .clickable(enabled = false) {}
        ) {
            // Proportional Right-Pane Header (aligned with Detail Pane 32.dp horizontal rhythm)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfaceCanvas)
                    .padding(horizontal = 32.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.16f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PANEL DETAIL",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Text(
                        text = title,
                        color = Color(0xFFF8FAFC),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    headerAction?.invoke()
                    HybridButton(
                        label = "Selesai",
                        icon = Icons.Default.Close,
                        isPrimary = true,
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(closeFocus)
                    )
                }
            }

            // Divider matching Right Detail Pane
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accentColor.copy(alpha = 0.35f))
            )

            // Scrollable Body matching Right Detail Pane max-width & padding
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1160.dp)
                        .focusRequester(bodyFocus)
                        .focusGroup()
                        .onFocusChanged { bodyHasFocus = it.hasFocus },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }

            // Contextual Footer Bar matching Right Detail Pane footer
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
                    text = SettingsNavigationModel.footerHintForZone(SettingsFocusZone.OVERLAY_DRAWER),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "FOKUS: PANEL DETAIL",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    if (rightPaneHost != null) {
        val entryId = remember { Any() }
        val entry = remember(entryId) { SettingsOverlayEntry(entryId, onDismiss, drawerPanel) }
        entry.onDismiss = onDismiss
        entry.content = drawerPanel
        DisposableEffect(rightPaneHost, entryId) {
            rightPaneHost.attach(entry)
            onDispose { rightPaneHost.detach(entryId) }
        }
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.66f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.70f)
                ) {
                    drawerPanel()
                }
            }
        }
    }
}

@Composable
fun HybridCoordinateStepper(
    label: String,
    value: Double,
    min: Double,
    max: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val vkPolicy = LocalVirtualKeyboardPolicy.current
    var isFocused by remember { mutableStateOf(false) }
    var textVal by remember {
        val initial = String.format(Locale.US, "%.4f", value)
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            val formatted = String.format(Locale.US, "%.4f", value)
            if (formatted != textVal.text) {
                textVal = TextFieldValue(formatted, TextRange(formatted.length))
            }
        }
    }

    LaunchedEffect(isFocused, vkPolicy.allowVirtualKeyboard) {
        if (isFocused && !vkPolicy.allowVirtualKeyboard) {
            keyboardController?.hide()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgPrimary.copy(alpha = 0.7f))
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HybridButton(
                label = "-0.01",
                onClick = { onValueChange((value - 0.01).coerceIn(min, max)) }
            )
            VirtualKeyboardGuard(allowVirtualKeyboard = vkPolicy.allowVirtualKeyboard) {
                BasicTextField(
                    value = textVal,
                    onValueChange = { raw ->
                        textVal = raw
                        raw.text.replace(',', '.').toDoubleOrNull()?.let { parsed ->
                            if (parsed in min..max) onValueChange(parsed)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    ),
                    textStyle = TextStyle(
                        color = palette.accentSecondary,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(TvFocusCyan),
                    modifier = Modifier
                        .width(128.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TvSurfaceRow)
                        .border(
                            width = if (isFocused) 2.5.dp else 1.dp,
                            color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (keyEvent.key) {
                                Key.DirectionCenter -> {
                                    if (vkPolicy.allowVirtualKeyboard) {
                                        keyboardController?.show()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    focusManager.moveFocus(FocusDirection.Up)
                                    true
                                }
                                Key.DirectionDown -> {
                                    focusManager.moveFocus(FocusDirection.Down)
                                    true
                                }
                                Key.Enter, Key.NumPadEnter -> {
                                    keyboardController?.hide()
                                    focusManager.moveFocus(FocusDirection.Next)
                                    true
                                }
                                Key.Tab -> {
                                    focusManager.moveFocus(if (keyEvent.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                                    true
                                }
                                Key.Escape -> {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    true
                                }
                                else -> false
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
            HybridButton(
                label = "+0.01",
                onClick = { onValueChange((value + 0.01).coerceIn(min, max)) }
            )
        }
    }
}

@Composable
fun HybridTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    debounceMs: Long = 250L,
    onSubmit: (() -> Unit)? = null
) {
    val palette = LocalJwsTheme.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val vkPolicy = LocalVirtualKeyboardPolicy.current
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnSubmit by rememberUpdatedState(onSubmit)

    var isFocused by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    var lastEmittedText by remember { mutableStateOf(value) }

    fun flushNow() {
        val currentText = fieldValue.text
        if (currentText != lastEmittedText) {
            lastEmittedText = currentText
            latestOnValueChange(currentText)
        }
    }

    // Sync external value ONLY when field is NOT focused so 1Hz ticks or async disk saves
    // NEVER overwrite active IME composition or jump the cursor while typing!
    LaunchedEffect(value, isFocused) {
        if (!isFocused && value != fieldValue.text) {
            fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
            lastEmittedText = value
        }
    }

    LaunchedEffect(isFocused, vkPolicy.allowVirtualKeyboard) {
        if (isFocused && !vkPolicy.allowVirtualKeyboard) {
            keyboardController?.hide()
        }
    }

    // Debounced commit while typing (smooth zero-jank typing on both D-Pad IME & PC keyboard)
    LaunchedEffect(fieldValue.text, debounceMs) {
        val pending = fieldValue.text
        if (debounceMs > 0L && pending != lastEmittedText) {
            delay(debounceMs)
            if (fieldValue.text == pending && pending != lastEmittedText) {
                lastEmittedText = pending
                latestOnValueChange(pending)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { flushNow() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (label.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (isFocused) TvFocusCyan else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isFocused) {
                    val hintText = if (vkPolicy.allowVirtualKeyboard) {
                        if (singleLine) "OK: Keyboard • ▲▼: Pindah • Enter: Selesai" else "OK: Keyboard • Esc: Selesai"
                    } else {
                        if (singleLine) "Mode Keyboard PC • ▲▼: Pindah • Enter: Selesai" else "Mode Keyboard PC • Esc: Selesai"
                    }
                    Text(
                        text = hintText,
                        color = TvFocusCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        VirtualKeyboardGuard(allowVirtualKeyboard = vkPolicy.allowVirtualKeyboard) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { updated ->
                    val sanitized = if (singleLine && updated.text.contains('\n')) {
                        val clean = updated.text.replace("\r", "").replace("\n", "")
                        updated.copy(text = clean, selection = TextRange(clean.length))
                    } else {
                        updated
                    }
                    fieldValue = sanitized
                    if (debounceMs <= 0L && sanitized.text != lastEmittedText) {
                        lastEmittedText = sanitized.text
                        latestOnValueChange(sanitized.text)
                    }
                },
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else 6,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        flushNow()
                        if (latestOnSubmit != null) {
                            latestOnSubmit?.invoke()
                        } else {
                            keyboardController?.hide()
                        }
                    }
                ),
                textStyle = TextStyle(
                    color = Color(0xFFF8FAFC),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(TvFocusCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isFocused) TvSurfaceRowFocused else TvSurfaceRow
                    )
                    .border(
                        width = if (isFocused) 2.5.dp else 1.dp,
                        color = if (isFocused) TvFocusCyan else TvSurfaceBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .onFocusChanged { state ->
                        val wasFocused = isFocused
                        isFocused = state.isFocused
                        if (state.isFocused && !wasFocused) {
                            // Place cursor at end on initial focus for natural appending/backspacing
                            fieldValue = fieldValue.copy(selection = TextRange(fieldValue.text.length))
                            if (!vkPolicy.allowVirtualKeyboard) {
                                keyboardController?.hide()
                            }
                        } else if (!state.isFocused && wasFocused) {
                            flushNow()
                        }
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (keyEvent.key) {
                            Key.DirectionCenter -> {
                                if (vkPolicy.allowVirtualKeyboard) {
                                    keyboardController?.show()
                                } else {
                                    keyboardController?.hide()
                                }
                                true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                if (singleLine) {
                                    flushNow()
                                    if (latestOnSubmit != null) {
                                        latestOnSubmit?.invoke()
                                    } else {
                                        keyboardController?.hide()
                                        focusManager.moveFocus(FocusDirection.Down)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.Tab -> {
                                flushNow()
                                focusManager.moveFocus(
                                    if (keyEvent.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                                )
                                true
                            }
                            Key.Escape -> {
                                flushNow()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                true
                            }
                            Key.DirectionUp -> {
                                if (singleLine) {
                                    flushNow()
                                    focusManager.moveFocus(FocusDirection.Up)
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionDown -> {
                                if (singleLine) {
                                    flushNow()
                                    focusManager.moveFocus(FocusDirection.Down)
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionLeft -> {
                                if (fieldValue.selection.collapsed && fieldValue.selection.start == 0) {
                                    flushNow()
                                    focusManager.moveFocus(FocusDirection.Left)
                                    true
                                } else {
                                    false
                                }
                            }
                            Key.DirectionRight -> {
                                if (fieldValue.selection.collapsed && fieldValue.selection.end == fieldValue.text.length) {
                                    flushNow()
                                    focusManager.moveFocus(FocusDirection.Right)
                                    true
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (fieldValue.text.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                color = palette.textSecondary.copy(alpha = 0.45f),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * Multi-Role PIN Login Guard with On-Screen TV D-Pad, Touch & Physical PC Keyboard Numeric Input.
 * Hardened with:
 * - Zero hardcoded quick-login backdoor button
 * - Brute-force lockout (5 wrong attempts -> 60s cooldown)
 * - Automatic one-way PBKDF2 hash upgrade on login
 * - Offline Challenge-Response PIN Recovery via CS Al-Gaffar TV
 */
@Composable
fun PinLoginGuardScreen(
    users: List<UserAccount>,
    deviceCode: String = "STB00001",
    onAuthenticated: (UserAccount) -> Unit,
    onUpgradePinHash: (userId: String, rawPin: String) -> Unit = { _, _ -> },
    onResetSuperadminPin: (newPin: String) -> Unit = {},
    onCancel: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val lockoutTracker = remember { PinLockoutTracker() }
    var lockoutRemainingSec by remember { mutableStateOf(0L) }
    var showRecoveryMode by remember { mutableStateOf(false) }
    val firstKeyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { firstKeyFocus.requestFocus() }
    }

    LaunchedEffect(lockoutRemainingSec) {
        if (lockoutRemainingSec > 0L) {
            delay(1_000L)
            lockoutRemainingSec = lockoutTracker.remainingLockoutSeconds()
            if (lockoutRemainingSec == 0L) {
                errorMessage = ""
            }
        }
    }

    if (showRecoveryMode) {
        OfflinePinRecoveryScreen(
            deviceCode = deviceCode.ifBlank { "STB00001" },
            onPinResetCompleted = { newPin ->
                onResetSuperadminPin(newPin)
                showRecoveryMode = false
                pinInput = ""
                errorMessage = "PIN Superadmin berhasil direset! Silakan masuk dengan PIN baru Anda."
            },
            onBack = {
                showRecoveryMode = false
                pinInput = ""
            }
        )
        return
    }

    fun attemptLogin(candidatePin: String) {
        val lockedSec = lockoutTracker.remainingLockoutSeconds()
        if (lockedSec > 0L) {
            lockoutRemainingSec = lockedSec
            errorMessage = "Terlalu banyak percobaan salah. Coba lagi dalam ${lockedSec} detik."
            return
        }
        val matched = AuthManager.authenticate(
            username = "",
            rawPin = candidatePin,
            users = users
        )
        if (matched != null) {
            lockoutTracker.recordSuccess()
            if (AuthManager.needsHashUpgrade(matched.pinHash)) {
                onUpgradePinHash(matched.id, candidatePin)
            }
            onAuthenticated(matched)
        } else {
            val remainingAttempts = lockoutTracker.recordFailure()
            pinInput = ""
            if (remainingAttempts <= 0) {
                lockoutRemainingSec = lockoutTracker.remainingLockoutSeconds()
                errorMessage = "Akses dikunci sementara (${lockoutRemainingSec} detik) karena 5x gagal."
            } else {
                errorMessage = "PIN tidak sesuai. Sisa percobaan: $remainingAttempts kali."
            }
        }
    }

    fun appendDigit(digit: String) {
        if (lockoutTracker.isLocked()) {
            lockoutRemainingSec = lockoutTracker.remainingLockoutSeconds()
            errorMessage = "Akses dikunci sementara (${lockoutRemainingSec} detik)."
            return
        }
        errorMessage = ""
        if (pinInput.length < 8) {
            pinInput += digit
            if (pinInput.length == 6) {
                val matched = AuthManager.authenticate("", pinInput, users)
                if (matched != null) {
                    lockoutTracker.recordSuccess()
                    if (AuthManager.needsHashUpgrade(matched.pinHash)) {
                        onUpgradePinHash(matched.id, pinInput)
                    }
                    onAuthenticated(matched)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.Zero, Key.NumPad0 -> { appendDigit("0"); true }
                    Key.One, Key.NumPad1 -> { appendDigit("1"); true }
                    Key.Two, Key.NumPad2 -> { appendDigit("2"); true }
                    Key.Three, Key.NumPad3 -> { appendDigit("3"); true }
                    Key.Four, Key.NumPad4 -> { appendDigit("4"); true }
                    Key.Five, Key.NumPad5 -> { appendDigit("5"); true }
                    Key.Six, Key.NumPad6 -> { appendDigit("6"); true }
                    Key.Seven, Key.NumPad7 -> { appendDigit("7"); true }
                    Key.Eight, Key.NumPad8 -> { appendDigit("8"); true }
                    Key.Nine, Key.NumPad9 -> { appendDigit("9"); true }
                    Key.Backspace, Key.Delete -> {
                        errorMessage = ""
                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                        true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        attemptLogin(pinInput)
                        true
                    }
                    Key.Escape -> {
                        onCancel()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accent, RoundedCornerShape(26.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TvFocusAmber,
                modifier = Modifier.size(46.dp)
            )
            Text(
                text = "Autentikasi PIN Pengurus DKM",
                color = palette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Masukkan PIN pengurus lewat Remote TV (D-Pad), Keyboard, atau sentuh layar",
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSecondary)
                    .border(
                        1.5.dp,
                        if (lockoutRemainingSec > 0L) Color(0xFFEF4444) else palette.accent,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (lockoutRemainingSec > 0L) {
                        "TERKUNCI (${lockoutRemainingSec}s)"
                    } else if (pinInput.isEmpty()) {
                        "• • • • • •"
                    } else {
                        "• ".repeat(pinInput.length).trim()
                    },
                    color = if (lockoutRemainingSec > 0L) Color(0xFFF87171) else palette.accentSecondary,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = if (errorMessage.startsWith("PIN Superadmin berhasil")) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("HAPUS", "0", "MASUK")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        r.forEach { key ->
                            val btnMod = if (key == "1") {
                                Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .focusRequester(firstKeyFocus)
                            } else {
                                Modifier
                                    .weight(1f)
                                    .height(54.dp)
                            }
                            HybridButton(
                                label = key,
                                isPrimary = (key == "MASUK"),
                                isDanger = (key == "HAPUS"),
                                enabled = (lockoutRemainingSec == 0L),
                                onClick = {
                                    errorMessage = ""
                                    when (key) {
                                        "HAPUS" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                        "MASUK" -> attemptLogin(pinInput)
                                        else -> appendDigit(key)
                                    }
                                },
                                modifier = btnMod
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HybridButton(
                    label = "Lupa PIN? (Pemulihan CS)",
                    onClick = { showRecoveryMode = true }
                )
                HybridButton(
                    label = "Batal / Tutup",
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun OfflinePinRecoveryScreen(
    deviceCode: String,
    onPinResetCompleted: (newPin: String) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalJwsTheme.current
    val challengeCode = remember { OfflinePinRecoveryEngine.generateChallengeCode() }
    val today = remember { LocalDate.now() }
    var recoveryInput by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var statusError by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.90f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(640.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(palette.bgPrimary)
                .border(2.dp, TvFocusAmber, RoundedCornerShape(26.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pemulihan PIN Offline — CS Al-Gaffar TV",
                color = TvFocusAmber,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            if (!isVerified) {
                Text(
                    text = "Kirimkan Device ID, Kode Tantangan, dan Tanggal berikut ke WhatsApp CS Al-Gaffar TV untuk mendapatkan 6 digit Kode Pemulihan harian (100% offline):",
                    color = palette.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RecoveryInfoTile("DEVICE ID STB", deviceCode, palette.accent, Modifier.weight(1f))
                    RecoveryInfoTile("KODE TANTANGAN", challengeCode, TvFocusAmber, Modifier.weight(1f))
                    RecoveryInfoTile("TANGGAL TV", today.toString(), palette.accentSecondary, Modifier.weight(1f))
                }

                HybridTextInput(
                    label = "Masukkan 6 Digit Kode Pemulihan dari CS",
                    value = recoveryInput,
                    placeholder = "Contoh: 482910",
                    onValueChange = {
                        statusError = ""
                        recoveryInput = it.filter { c -> c.isDigit() }.take(6)
                    },
                    keyboardType = KeyboardType.Number
                )

                if (statusError.isNotBlank()) {
                    Text(statusError, color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HybridButton(label = "Kembali ke Login PIN", onClick = onBack)
                    HybridButton(
                        label = "Verifikasi Kode Pemulihan",
                        isPrimary = true,
                        enabled = recoveryInput.length == 6,
                        onClick = {
                            if (OfflinePinRecoveryEngine.verifyRecoveryCode(deviceCode, challengeCode, recoveryInput, today)) {
                                isVerified = true
                                statusError = ""
                            } else {
                                statusError = "Kode Pemulihan tidak cocok. Pastikan Device ID, Kode Tantangan & Tanggal sesuai."
                            }
                        }
                    )
                }
            } else {
                Text(
                    text = "Kode Pemulihan Terverifikasi! Buat PIN Superadmin baru (6 digit angka):",
                    color = Color(0xFF34D399),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                HybridTextInput(
                    label = "PIN Superadmin Baru (6 Digit)",
                    value = newPin,
                    placeholder = "Minimal 6 digit angka",
                    onValueChange = {
                        statusError = ""
                        newPin = it.filter { c -> c.isDigit() }.take(8)
                    },
                    keyboardType = KeyboardType.Number
                )
                HybridTextInput(
                    label = "Ulangi PIN Superadmin Baru",
                    value = confirmPin,
                    placeholder = "Ketik ulang PIN baru",
                    onValueChange = {
                        statusError = ""
                        confirmPin = it.filter { c -> c.isDigit() }.take(8)
                    },
                    keyboardType = KeyboardType.Number
                )
                if (statusError.isNotBlank()) {
                    Text(statusError, color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HybridButton(label = "Batal", onClick = onBack)
                    HybridButton(
                        label = "Simpan PIN Superadmin Baru",
                        isPrimary = true,
                        enabled = newPin.length >= 6 && confirmPin.length >= 6,
                        onClick = {
                            if (newPin != confirmPin) {
                                statusError = "Konfirmasi PIN tidak sama."
                            } else if (newPin == AuthManager.DEFAULT_SUPERADMIN_PIN) {
                                statusError = "Gunakan PIN baru yang berbeda dari PIN bawaan pabrik."
                            } else {
                                onPinResetCompleted(newPin)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecoveryInfoTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSecondary)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = palette.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = valueColor,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black
        )
    }
}
