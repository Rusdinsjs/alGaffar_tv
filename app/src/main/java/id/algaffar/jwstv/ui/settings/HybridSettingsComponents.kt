package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
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

/** High-contrast Amber focus ring color modeled after `MobileSettingsLayout.tsx` for TV D-Pad visibility. */
val TvFocusAmber = Color(0xFFFBBF24)

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
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSecondary.copy(alpha = 0.72f))
            .border(1.5.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.accent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = palette.textSecondary,
                        fontSize = 13.sp
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
    accentColor: Color = Color(0xFF38BDF8),
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.12f))
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
            color = Color.White.copy(alpha = 0.88f),
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
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        !enabled -> palette.bgSecondary.copy(alpha = 0.35f)
        isDanger -> if (isFocused) Color(0xFFEF4444) else Color(0xFF991B1B)
        isPrimary -> if (isFocused) palette.accentSecondary else palette.accent
        isFocused -> palette.accent.copy(alpha = 0.35f)
        else -> palette.bgPrimary.copy(alpha = 0.85f)
    }
    val textColor = when {
        !enabled -> palette.textSecondary.copy(alpha = 0.45f)
        isDanger -> Color.White
        isPrimary -> palette.bgPrimary
        else -> palette.textPrimary
    }
    val borderColor = if (isFocused) TvFocusAmber else palette.border

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isFocused) 3.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
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
 * TV D-Pad & Touch friendly select/dropdown control.
 * Renders a compact select box that opens a scrollable modal picker dialog on click/Enter.
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
    val palette = LocalJwsTheme.current
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
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isFocused) palette.accent.copy(alpha = 0.22f)
                    else palette.bgPrimary.copy(alpha = 0.85f)
                )
                .border(
                    width = if (isFocused) 3.dp else 1.5.dp,
                    color = if (isFocused) TvFocusAmber else palette.border,
                    shape = RoundedCornerShape(12.dp)
                )
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
                    color = if (selectedOption != null) palette.textPrimary else palette.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!selectedOption?.subtitle.isNullOrBlank()) {
                    Text(
                        text = selectedOption!!.subtitle,
                        color = palette.textSecondary,
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
                tint = if (isFocused) TvFocusAmber else palette.accentSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showDialog) {
        val initialIdx = remember(selectedId, options) {
            options.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)

        Dialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier = Modifier
                    .widthIn(min = 420.dp, max = 640.dp)
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgSecondary)
                    .border(2.dp, palette.accentSecondary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dialogTitle,
                        color = palette.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    HybridButton(
                        label = "Tutup",
                        isPrimary = false,
                        onClick = { showDialog = false }
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
                        var itemFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        itemFocused -> palette.accent.copy(alpha = 0.28f)
                                        isSelected -> palette.accent.copy(alpha = 0.16f)
                                        else -> palette.bgPrimary.copy(alpha = 0.72f)
                                    }
                                )
                                .border(
                                    width = if (itemFocused) 3.dp else 1.dp,
                                    color = when {
                                        itemFocused -> TvFocusAmber
                                        isSelected -> palette.accent
                                        else -> palette.border
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onFocusChanged { itemFocused = it.isFocused }
                                .focusable()
                                .clickable {
                                    onSelect(opt.id)
                                    showDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.label,
                                    color = if (isSelected) palette.accentSecondary else palette.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                )
                                if (opt.subtitle.isNotBlank()) {
                                    Text(
                                        text = opt.subtitle,
                                        color = palette.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(20.dp)
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
fun HybridToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String = "",
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) palette.accent.copy(alpha = 0.2f)
                else palette.bgPrimary.copy(alpha = 0.7f)
            )
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) TvFocusAmber else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = palette.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (checked) "AKTIF" else "NONAKTIF",
                color = if (checked) palette.accent else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                imageVector = if (checked) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                contentDescription = null,
                tint = if (checked) palette.accent else palette.textSecondary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

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
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var decFocused by remember { mutableStateOf(false) }
    var incFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgPrimary.copy(alpha = if (enabled) 0.7f else 0.35f))
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (decFocused) palette.accent else palette.bgSecondary)
                    .border(
                        width = if (decFocused) 3.dp else 1.dp,
                        color = if (decFocused) TvFocusAmber else palette.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { decFocused = it.isFocused }
                    .focusable(enabled = enabled)
                    .clickable(enabled = enabled) {
                        onValueChange((value - step).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Kurangi",
                    tint = if (decFocused) palette.bgPrimary else palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = if (unit.isBlank()) "$value" else "$value $unit",
                color = palette.accentSecondary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(102.dp)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (incFocused) palette.accent else palette.bgSecondary)
                    .border(
                        width = if (incFocused) 3.dp else 1.dp,
                        color = if (incFocused) TvFocusAmber else palette.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { incFocused = it.isFocused }
                    .focusable(enabled = enabled)
                    .clickable(enabled = enabled) {
                        onValueChange((value + step).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah",
                    tint = if (incFocused) palette.bgPrimary else palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
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
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier
                        .width(128.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.bgSecondary)
                        .border(
                            width = if (isFocused) 2.5.dp else 1.dp,
                            color = if (isFocused) TvFocusAmber else palette.border,
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
                    color = if (isFocused) TvFocusAmber else palette.textSecondary,
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
                        color = palette.accent,
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
                    color = palette.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isFocused) palette.bgPrimary else palette.bgPrimary.copy(alpha = 0.85f)
                    )
                    .border(
                        width = if (isFocused) 2.5.dp else 1.dp,
                        color = if (isFocused) TvFocusAmber else palette.border,
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
