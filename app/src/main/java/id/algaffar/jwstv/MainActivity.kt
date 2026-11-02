package id.algaffar.jwstv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.service.JwsForegroundService
import id.algaffar.jwstv.ui.MainViewModel
import id.algaffar.jwstv.ui.components.Responsive1080pCanvas
import id.algaffar.jwstv.ui.screens.fullscreen.FullScreenRouter
import id.algaffar.jwstv.ui.settings.SettingsModal
import id.algaffar.jwstv.ui.theme.IslamicAmbientBackground
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.ThemeEngine

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as JwsApplication).container)
    }

    private val exportTakjilExcelLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.exportTakjilExcelTemplate(out)
            }
        }
    }

    private val importTakjilExcelLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.parseTakjilExcelPreview(input)
            }
        }
    }

    private val exportBackupZipLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.let { out ->
                viewModel.exportFullBackupZip(out)
            }
        }
    }

    private val importBackupZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importFullBackupZip(input)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always keep TV screen awake for 24/7 digital signage
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        JwsForegroundService.startServiceSafely(this)

        setContent {
            MaterialTheme {
                val snapshot by viewModel.snapshotFlow.collectAsState()
                val statusBanner by viewModel.statusBannerMessage.collectAsState()
                val takjilPreview by viewModel.takjilPreviewFlow.collectAsState()
                val relayPinStates by viewModel.relayPinStatesFlow.collectAsState()
                val openSettingsReq by viewModel.openSettingsRequestCount.collectAsState()

                val activePalette = remember(snapshot.settings.themeSettings, snapshot.now.hour) {
                    ThemeEngine.resolveActivePalette(
                        themeSettings = snapshot.settings.themeSettings,
                        now = snapshot.now
                    )
                }

                var isDebugModalOpen by remember { mutableStateOf(false) }
                var isSettingsModalOpen by remember { mutableStateOf(false) }

                LaunchedEffect(openSettingsReq) {
                    if (openSettingsReq > 0) {
                        isSettingsModalOpen = true
                    }
                }

                CompositionLocalProvider(LocalJwsTheme provides activePalette) {
                    Responsive1080pCanvas(
                        scaleMode = snapshot.settings.layoutSettings.scaleMode
                    ) {
                        IslamicAmbientBackground(palette = activePalette) {
                            FullScreenRouter(
                                snapshot = snapshot,
                                assetsRootDir = viewModel.assetsRootDir,
                                onCycleTheme = { viewModel.cycleTheme() },
                                onCycleLayout = { viewModel.cycleLayout() },
                                onCycleClockStyle = { viewModel.cycleClockStyle() },
                                onOpenDebugModal = { isDebugModalOpen = true },
                                onOpenSettingsModal = { isSettingsModalOpen = true },
                                onExitManualSimulation = { viewModel.clearSimulation() }
                            )

                            if (isSettingsModalOpen) {
                                SettingsModal(
                                    settings = snapshot.settings,
                                    statusBannerMessage = statusBanner,
                                    takjilImportPreview = takjilPreview,
                                    onConfirmTakjilImport = { viewModel.confirmTakjilImport() },
                                    onDismissTakjilImport = { viewModel.dismissTakjilImport() },
                                    onUpdateSettings = { viewModel.updateSettings(it) },
                                    onRequestExportTakjilTemplate = {
                                        exportTakjilExcelLauncher.launch("Template_Takjil_Ramadhan_AlGaffar.xlsx")
                                    },
                                    onRequestImportTakjilExcel = {
                                        importTakjilExcelLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel"
                                            )
                                        )
                                    },
                                    onRequestExportBackupZip = {
                                        exportBackupZipLauncher.launch("Backup_JWS_Masjid_AlGaffar.zip")
                                    },
                                    onRequestImportBackupZip = {
                                        importBackupZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    },
                                    onOpenDebugSimulator = {
                                        isSettingsModalOpen = false
                                        isDebugModalOpen = true
                                    },
                                    onClose = { isSettingsModalOpen = false },
                                    remoteServerUrl = snapshot.remoteServerUrl,
                                    remoteQrPairingUrl = snapshot.remoteQrPairingUrl,
                                    remoteAuthToken = snapshot.remoteAuthToken,
                                    relayPinStates = relayPinStates,
                                    onToggleManualRelay = { pin, turnOn ->
                                        viewModel.toggleManualRelay(pin, turnOn)
                                    },
                                    onDiscoverOnvifCameras = {
                                        viewModel.discoverOnvifCameras()
                                    }
                                )
                            }

                            if (isDebugModalOpen) {
                                QuickDebugModalOverlay(
                                    activeState = snapshot.tickResult.state,
                                    isManualOverride = snapshot.tickResult.isManualOverride,
                                    currentThemeName = activePalette.name,
                                    currentLayoutName = snapshot.settings.layoutSettings.position.serialName,
                                    currentClockStyle = snapshot.settings.brand.clockStyle,
                                    onSimulateState = { state ->
                                        viewModel.simulateState(state, 180L)
                                        isDebugModalOpen = false
                                    },
                                    onClearSimulation = {
                                        viewModel.clearSimulation()
                                        isDebugModalOpen = false
                                    },
                                    onTestBeep = { viewModel.testPlayBeep1000Hz() },
                                    onTestSirine = { viewModel.testPlayRamadhanSirine() },
                                    onCycleTheme = { viewModel.cycleTheme() },
                                    onCycleLayout = { viewModel.cycleLayout() },
                                    onCycleClock = { viewModel.cycleClockStyle() },
                                    onDismiss = { isDebugModalOpen = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickDebugModalOverlay(
    activeState: AppState,
    isManualOverride: Boolean,
    currentThemeName: String,
    currentLayoutName: String,
    currentClockStyle: String,
    onSimulateState: (AppState) -> Unit,
    onClearSimulation: () -> Unit,
    onTestBeep: () -> Unit,
    onTestSirine: () -> Unit,
    onCycleTheme: () -> Unit,
    onCycleLayout: () -> Unit,
    onCycleClock: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJwsTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accent, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Panel Simulasi 27-State & Uji Audio (D-Pad / Touch)",
                        color = palette.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Tema: $currentThemeName  •  Layout: $currentLayoutName  •  Jam: $currentClockStyle",
                        color = palette.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HybridActionChip(label = "Uji Beep 1000Hz", isActive = false, onClick = onTestBeep)
                    HybridActionChip(label = "Uji Sirine", isActive = false, onClick = onTestSirine)
                    HybridActionChip(label = "Ganti Tema", isActive = false, onClick = onCycleTheme)
                    HybridActionChip(label = "Ganti Layout", isActive = false, onClick = onCycleLayout)
                    HybridActionChip(label = "Ganti Jam", isActive = false, onClick = onCycleClock)
                    HybridActionChip(
                        label = "Mode Otomatis",
                        isActive = !isManualOverride,
                        onClick = onClearSimulation
                    )
                    HybridActionChip(label = "Tutup [X]", isActive = false, onClick = onDismiss)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppState.entries.forEach { state ->
                    HybridActionChip(
                        label = "${state.serialName} (${state.displayLabel})",
                        isActive = activeState == state,
                        onClick = { onSimulateState(state) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HybridActionChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isActive -> palette.accent
        isFocused -> palette.accent.copy(alpha = 0.35f)
        else -> palette.bgSecondary
    }
    val borderColor = if (isFocused) palette.accentSecondary else palette.border

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) palette.bgPrimary else palette.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (isActive || isFocused) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
