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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import java.io.File
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
    val onOpenDebugSimulator: () -> Unit,
    val onToggleManualRelay: (Int, Boolean) -> Unit,
    val onDiscoverOnvifCameras: () -> Unit,
    val onTestOnlineFinance: () -> Unit,
    val onStartDirectLive: (source: String, customId: String, durationMinutes: Int) -> Unit,
    val onStopDirectLive: () -> Unit,
    val onPickImageAsset: (subFolder: String, onSaved: (String) -> Unit) -> Unit,
    val onPickPrayerAudio: (category: String, slotName: String) -> Unit,
    val onDeletePrayerAudio: (category: String, slotName: String) -> Unit,
    val onTogglePreviewPrayerAudio: (category: String, slotName: String) -> Unit,
    val onPickCustomAudio: () -> Unit,
    val onDeleteCustomAudio: (fileName: String) -> Unit,
    val onTogglePreviewCustomAudio: (fileName: String) -> Unit,
    val onFindPrayerAudioSlot: (category: String, slotName: String) -> Pair<Boolean, String>,
    val onListCustomAudioFiles: () -> List<Pair<String, String>>,
    val assetsRootDir: File?,
    val remoteServerUrl: String,
    val remoteQrPairingUrl: String,
    val remoteAuthToken: String,
    val relayPinStates: Map<Int, Boolean>,
    val onlineFinanceTestResult: FinancialSummaryData?,
    val audioInventoryTick: Int,
    val previewingAudioKey: String?,
    val isLiveActiveNow: Boolean
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
    var textVal by remember(value) { mutableStateOf(String.format(Locale.US, "%.4f", value)) }

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
            BasicTextField(
                value = textVal,
                onValueChange = { raw ->
                    textVal = raw
                    raw.toDoubleOrNull()?.let { parsed ->
                        if (parsed in min..max) onValueChange(parsed)
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.accentSecondary,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.bgSecondary)
                    .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
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
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label.uppercase(),
                color = palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgPrimary.copy(alpha = 0.85f))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) TvFocusAmber else palette.border,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
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

/**
 * Multi-Role PIN Login Guard with On-Screen TV D-Pad & Touch Numeric Keypad.
 * Default Superadmin: username `admin`, PIN `732911`.
 */
@Composable
fun PinLoginGuardScreen(
    users: List<UserAccount>,
    onAuthenticated: (UserAccount) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun attemptLogin(candidatePin: String) {
        val matched = AuthManager.authenticate(
            username = "",
            rawPin = candidatePin,
            users = users
        )
        if (matched != null) {
            onAuthenticated(matched)
        } else {
            errorMessage = "PIN tidak sesuai (Default Superadmin: 732911)"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(540.dp)
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
                text = "Masukkan 6 digit PIN menggunakan Remote TV (D-Pad) atau sentuh tombol angka",
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSecondary)
                    .border(1.5.dp, palette.accent, RoundedCornerShape(14.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pinInput.isEmpty()) "• • • • • •" else "• ".repeat(pinInput.length).trim(),
                    color = palette.accentSecondary,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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
                            HybridButton(
                                label = key,
                                isPrimary = (key == "MASUK"),
                                isDanger = (key == "HAPUS"),
                                onClick = {
                                    errorMessage = ""
                                    when (key) {
                                        "HAPUS" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                        "MASUK" -> attemptLogin(pinInput)
                                        else -> {
                                            if (pinInput.length < 8) {
                                                pinInput += key
                                                if (pinInput.length == 6) {
                                                    AuthManager.authenticate("", pinInput, users)?.let(onAuthenticated)
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
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
                    label = "Login Cepat Default (732911)",
                    onClick = { attemptLogin(AuthManager.DEFAULT_SUPERADMIN_PIN) }
                )
                HybridButton(
                    label = "Batal / Tutup",
                    onClick = onCancel
                )
            }
        }
    }
}
