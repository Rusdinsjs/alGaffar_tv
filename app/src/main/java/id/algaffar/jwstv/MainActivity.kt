package id.algaffar.jwstv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.service.JwsForegroundService
import id.algaffar.jwstv.ui.MainViewModel
import id.algaffar.jwstv.ui.components.Responsive1080pCanvas
import id.algaffar.jwstv.ui.screens.fullscreen.FullScreenRouter
import id.algaffar.jwstv.ui.screens.standby.LogoQuickMenuCard
import id.algaffar.jwstv.ui.settings.LocalVirtualKeyboardPolicy
import id.algaffar.jwstv.ui.settings.SettingsActions
import id.algaffar.jwstv.ui.settings.SettingsModal
import id.algaffar.jwstv.ui.settings.TvFilePickerDialog
import id.algaffar.jwstv.ui.settings.TvFilePickerRequest
import id.algaffar.jwstv.ui.settings.VirtualKeyboardPolicy
import id.algaffar.jwstv.ui.settings.rememberPhysicalKeyboardNames
import id.algaffar.jwstv.ui.theme.IslamicAmbientBackground
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.LocalPerformanceMode
import id.algaffar.jwstv.ui.theme.ThemeEngine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as JwsApplication).container)
    }

    private var isLogoMenuOpen by mutableStateOf(false)
    private var isDebugModalOpen by mutableStateOf(false)
    private var isSettingsModalOpen by mutableStateOf(false)
    private var activeFilePickerRequest by mutableStateOf<TvFilePickerRequest?>(null)
    private var hasStoragePermission by mutableStateOf(false)
    private var livePhysicalKeyboardDetected by mutableStateOf(false)
    private var consumeNextDpadUpRelease = false
    private var isMouseCursorHidden by mutableStateOf(true)
    private var mouseActivityTick by mutableLongStateOf(0L)
    private var lastMouseX = Float.NaN
    private var lastMouseY = Float.NaN

    private companion object {
        const val MOUSE_IDLE_TIMEOUT_MS = 3000L
    }

    private fun handlePotentialMouseMotion(ev: MotionEvent) {
        val isMouse = ev.isFromSource(InputDevice.SOURCE_MOUSE) ||
            (ev.pointerCount > 0 && ev.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE)
        if (!isMouse) return

        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_HOVER_EXIT) return

        if (action == MotionEvent.ACTION_HOVER_MOVE) {
            if (!lastMouseX.isNaN() && !lastMouseY.isNaN() &&
                abs(ev.x - lastMouseX) <= 1f && abs(ev.y - lastMouseY) <= 1f
            ) {
                return
            }
        }
        lastMouseX = ev.x
        lastMouseY = ev.y

        isMouseCursorHidden = false
        mouseActivityTick++
    }

    private fun applyWindowPointerIcon(hidden: Boolean) {
        val icon = if (hidden) {
            android.view.PointerIcon.getSystemIcon(this, android.view.PointerIcon.TYPE_NULL)
        } else {
            null
        }
        window.decorView.pointerIcon = icon
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        contentView?.pointerIcon = icon
        for (i in 0 until (contentView?.childCount ?: 0)) {
            contentView?.getChildAt(i)?.pointerIcon = icon
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        handlePotentialMouseMotion(ev)
        return super.dispatchGenericMotionEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        handlePotentialMouseMotion(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val dev = event.device
            if (dev != null && !dev.isVirtual && dev.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                livePhysicalKeyboardDetected = true
            }
        }
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0 &&
                !isSettingsModalOpen &&
                !isDebugModalOpen &&
                !isLogoMenuOpen &&
                activeFilePickerRequest == null
            ) {
                isLogoMenuOpen = true
                consumeNextDpadUpRelease = true
                return true
            }
            if (event.action == KeyEvent.ACTION_UP && consumeNextDpadUpRelease) {
                consumeNextDpadUpRelease = false
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasStoragePermission = checkStorageReadPermission()
    }

    private fun requiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkStorageReadPermission(): Boolean {
        return requiredStoragePermissions().any { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissionsSafely() {
        runCatching {
            storagePermissionLauncher.launch(requiredStoragePermissions())
        }
    }

    private fun canHandleOpenDocument(mimeTypes: Array<String>): Boolean {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        return intent.resolveActivity(packageManager) != null
    }

    private fun canHandleCreateDocument(mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
        return intent.resolveActivity(packageManager) != null
    }

    private fun saveDirectToDownloads(fileName: String, writer: (FileOutputStream) -> Unit) {
        val result = runCatching {
            val publicDl = runCatching {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }.getOrNull()
            val targetDir = when {
                publicDl != null && (publicDl.exists() || publicDl.mkdirs()) && publicDl.canWrite() -> publicDl
                else -> (getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir).apply { mkdirs() }
            }
            val targetFile = File(targetDir, fileName)
            FileOutputStream(targetFile).use { out -> writer(out) }
            targetFile.absolutePath
        }
        result.onSuccess { path ->
            viewModel.postStatusMessage("File berhasil disimpan langsung ke: $path")
        }.onFailure { err ->
            viewModel.postStatusMessage("Gagal menyimpan file ekspor: ${err.message}")
        }
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

    private var pendingImageSubFolder: String = "media"
    private var pendingImageCallback: ((String) -> Unit)? = null
    private val pickImageAssetLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val cb = pendingImageCallback
        if (uri != null && cb != null) {
            val fileName = resolveFileName(uri) ?: "image_${System.currentTimeMillis()}.png"
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importImageAsset(pendingImageSubFolder, fileName, input, cb)
            }
        }
        pendingImageCallback = null
    }

    private var pendingAudioCategory: String = "tartil"
    private var pendingAudioSlot: String = "fajr"
    private val pickPrayerAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importPrayerAudioSlot(pendingAudioCategory, pendingAudioSlot, input)
            }
        }
    }

    private val pickCustomAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = resolveFileName(uri) ?: "custom_${System.currentTimeMillis()}.mp3"
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importCustomAudioFile(fileName, input)
            }
        }
    }

    private var pendingVideoCallback: ((String, String) -> Unit)? = null
    private val pickVideoAssetLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val cb = pendingVideoCallback
        if (uri != null && cb != null) {
            val fileName = resolveFileName(uri) ?: "video_${System.currentTimeMillis()}.mp4"
            contentResolver.openInputStream(uri)?.let { input ->
                viewModel.importVideoAsset(fileName, input, cb)
            }
        }
        pendingVideoCallback = null
    }

    private fun openInAppOrSystemFilePicker(
        title: String,
        subtitle: String,
        allowedExtensions: Set<String>,
        mimeTypes: Array<String>,
        onFileSelected: (File) -> Unit,
        systemLauncherInvoke: () -> Unit
    ) {
        hasStoragePermission = checkStorageReadPermission()
        if (!hasStoragePermission) {
            requestStoragePermissionsSafely()
        }
        val systemOption: (() -> Unit)? = if (canHandleOpenDocument(mimeTypes)) {
            {
                activeFilePickerRequest = null
                runCatching { systemLauncherInvoke() }.onFailure {
                    viewModel.postStatusMessage("Picker sistem tidak tersedia di perangkat TV ini. Gunakan File Explorer bawaan.")
                }
            }
        } else {
            null
        }
        activeFilePickerRequest = TvFilePickerRequest(
            title = title,
            subtitle = subtitle,
            allowedExtensions = allowedExtensions,
            mimeTypes = mimeTypes,
            onFileSelected = onFileSelected,
            onOpenSystemPicker = systemOption
        )
    }

    private fun resolveFileName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always keep TV screen awake for 24/7 digital signage
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hasStoragePermission = checkStorageReadPermission()
        JwsForegroundService.startServiceSafely(this)

        setContent {
            MaterialTheme {
                val snapshot by viewModel.snapshotFlow.collectAsState()
                val statusBanner by viewModel.statusBannerMessage.collectAsState()
                val takjilPreview by viewModel.takjilPreviewFlow.collectAsState()
                val relayPinStates by viewModel.relayPinStatesFlow.collectAsState()
                val openSettingsReq by viewModel.openSettingsRequestCount.collectAsState()
                val onlineFinanceTestResult by viewModel.onlineFinanceTestResult.collectAsState()
                val audioInventoryTick by viewModel.audioInventoryTick.collectAsState()
                val previewingAudioKey by viewModel.previewingAudioKey.collectAsState()
                val storageHealth by viewModel.storageHealthFlow.collectAsState()

                val activePalette = remember(snapshot.settings.themeSettings, snapshot.now.hour) {
                    ThemeEngine.resolveActivePalette(
                        themeSettings = snapshot.settings.themeSettings,
                        now = snapshot.now
                    )
                }

                val effectiveLayout = remember(
                    snapshot.settings.layoutSettings.position,
                    snapshot.settings.layoutSettings.autoRotateLayout,
                    snapshot.now.hour
                ) {
                    if (snapshot.settings.layoutSettings.autoRotateLayout) {
                        val block = (snapshot.now.hour / 4) % 3
                        LayoutPosition.entries[block]
                    } else {
                        snapshot.settings.layoutSettings.position
                    }
                }
                val isVerticalLeft = (effectiveLayout == LayoutPosition.VERTICAL_LEFT)

                LaunchedEffect(openSettingsReq) {
                    if (openSettingsReq > 0) {
                        isLogoMenuOpen = false
                        isSettingsModalOpen = true
                    }
                }

                // Dispatch Remote Control PWA virtual D-Pad keys directly into Activity key pipeline
                LaunchedEffect(Unit) {
                    viewModel.remoteNavKeyEvents.collect { keyCode ->
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                            dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                        }
                    }
                }

                // Prevent TV remote Back button from exiting the Home Launcher activity
                BackHandler(enabled = true) {
                    when {
                        activeFilePickerRequest != null -> activeFilePickerRequest = null
                        isLogoMenuOpen -> isLogoMenuOpen = false
                        isDebugModalOpen -> isDebugModalOpen = false
                        isSettingsModalOpen && takjilPreview != null -> viewModel.dismissTakjilImport()
                        isSettingsModalOpen -> {
                            viewModel.stopAudioPreview()
                            isSettingsModalOpen = false
                        }
                        snapshot.tickResult.isManualOverride -> viewModel.clearSimulation()
                    }
                }

                val detectedKeyboards = rememberPhysicalKeyboardNames(livePhysicalKeyboardDetected)
                val virtualKeyboardPolicy = remember(snapshot.settings.virtualKeyboardMode, detectedKeyboards) {
                    VirtualKeyboardPolicy(
                        mode = snapshot.settings.virtualKeyboardMode,
                        hasPhysicalKeyboard = detectedKeyboards.isNotEmpty(),
                        detectedKeyboardNames = detectedKeyboards
                    )
                }

                LaunchedEffect(virtualKeyboardPolicy.allowVirtualKeyboard) {
                    if (!virtualKeyboardPolicy.allowVirtualKeyboard) {
                        window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                        )
                        WindowCompat.getInsetsController(window, window.decorView)
                            .hide(WindowInsetsCompat.Type.ime())
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                    } else {
                        window.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                        )
                    }
                }

                val context = LocalContext.current
                val hiddenPointerIcon = remember(context) {
                    PointerIcon(
                        android.view.PointerIcon.getSystemIcon(
                            context,
                            android.view.PointerIcon.TYPE_NULL
                        )
                    )
                }

                LaunchedEffect(mouseActivityTick, isMouseCursorHidden) {
                    if (!isMouseCursorHidden) {
                        delay(MOUSE_IDLE_TIMEOUT_MS)
                        isMouseCursorHidden = true
                    }
                }

                LaunchedEffect(isMouseCursorHidden) {
                    applyWindowPointerIcon(isMouseCursorHidden)
                }

                CompositionLocalProvider(
                    LocalJwsTheme provides activePalette,
                    LocalPerformanceMode provides snapshot.settings.performanceMode,
                    LocalVirtualKeyboardPolicy provides virtualKeyboardPolicy
                ) {
                    Responsive1080pCanvas(
                        scaleMode = snapshot.settings.layoutSettings.scaleMode,
                        modifier = if (isMouseCursorHidden) {
                            Modifier.pointerHoverIcon(hiddenPointerIcon, overrideDescendants = true)
                        } else {
                            Modifier
                        }
                    ) {
                        IslamicAmbientBackground(palette = activePalette) {
                            FullScreenRouter(
                                snapshot = snapshot,
                                assetsRootDir = viewModel.assetsRootDir,
                                isLogoMenuOpen = isLogoMenuOpen,
                                onToggleLogoMenu = { isLogoMenuOpen = !isLogoMenuOpen },
                                onOpenDebugModal = { isDebugModalOpen = true },
                                onExitManualSimulation = { viewModel.clearSimulation() }
                            )

                            // Floating Quick Action Menu (triggered by clicking the Mosque Logo OR pressing D-Pad UP)
                            if (isLogoMenuOpen && !isSettingsModalOpen && !isDebugModalOpen && activeFilePickerRequest == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { isLogoMenuOpen = false }
                                )

                                LogoQuickMenuCard(
                                    onCycleTheme = { viewModel.cycleTheme() },
                                    onCycleLayout = { viewModel.cycleLayout() },
                                    onCycleClockStyle = { viewModel.cycleClockStyle() },
                                    onOpenDebugModal = { isDebugModalOpen = true },
                                    onOpenSettingsModal = { isSettingsModalOpen = true },
                                    onDismiss = { isLogoMenuOpen = false },
                                    modifier = Modifier
                                        .align(if (isVerticalLeft) Alignment.TopEnd else Alignment.TopStart)
                                        .padding(
                                            top = 182.dp,
                                            start = if (isVerticalLeft) 0.dp else 18.dp,
                                            end = if (isVerticalLeft) 18.dp else 0.dp
                                        )
                                )
                            }

                            if (isSettingsModalOpen) {
                                SettingsModal(
                                    settings = snapshot.settings,
                                    statusBannerMessage = statusBanner,
                                    takjilImportPreview = takjilPreview,
                                    onConfirmTakjilImport = { viewModel.confirmTakjilImport() },
                                    onDismissTakjilImport = { viewModel.dismissTakjilImport() },
                                    actions = SettingsActions(
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onExportTakjilTemplate = {
                                            val fileName = "Template_Takjil_Ramadhan_AlGaffar.xlsx"
                                            val mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                            if (canHandleCreateDocument(mime)) {
                                                runCatching { exportTakjilExcelLauncher.launch(fileName) }.onFailure {
                                                    saveDirectToDownloads(fileName) { out -> viewModel.exportTakjilExcelTemplate(out) }
                                                }
                                            } else {
                                                saveDirectToDownloads(fileName) { out -> viewModel.exportTakjilExcelTemplate(out) }
                                            }
                                        },
                                        onImportTakjilExcel = {
                                            val mimes = arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel"
                                            )
                                            openInAppOrSystemFilePicker(
                                                title = "Pilih File Excel Jadwal Ta'jil (.xlsx)",
                                                subtitle = "Pilih file Excel dari Memori Internal atau Flashdisk USB OTG",
                                                allowedExtensions = setOf("xlsx", "xls"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        FileInputStream(file).use { input ->
                                                            viewModel.parseTakjilExcelPreview(input)
                                                        }
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal membaca Excel: ${err.message}")
                                                    }
                                                },
                                                systemLauncherInvoke = { importTakjilExcelLauncher.launch(mimes) }
                                            )
                                        },
                                        onExportBackupZip = {
                                            val fileName = "Backup_JWS_Masjid_AlGaffar.zip"
                                            val mime = "application/zip"
                                            if (canHandleCreateDocument(mime)) {
                                                runCatching { exportBackupZipLauncher.launch(fileName) }.onFailure {
                                                    saveDirectToDownloads(fileName) { out -> viewModel.exportFullBackupZip(out) }
                                                }
                                            } else {
                                                saveDirectToDownloads(fileName) { out -> viewModel.exportFullBackupZip(out) }
                                            }
                                        },
                                        onImportBackupZip = {
                                            val mimes = arrayOf("application/zip", "application/octet-stream")
                                            openInAppOrSystemFilePicker(
                                                title = "Pilih File Backup JWS (.zip)",
                                                subtitle = "Restore pengaturan & aset dari file backup ZIP di Memori TV / USB",
                                                allowedExtensions = setOf("zip"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        viewModel.importFullBackupZip(FileInputStream(file))
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal membuka ZIP: ${err.message}")
                                                    }
                                                },
                                                systemLauncherInvoke = { importBackupZipLauncher.launch(mimes) }
                                            )
                                        },
                                        onSyncUsbFolder = {
                                            hasStoragePermission = checkStorageReadPermission()
                                            if (!hasStoragePermission) {
                                                requestStoragePermissionsSafely()
                                            }
                                            activeFilePickerRequest = TvFilePickerRequest(
                                                title = "Sync Massal 1 Folder dari Flashdisk USB",
                                                subtitle = "Buka folder berisi gambar, video (.mp4), atau audio MP3 lalu tekan 'Sinkronkan Folder Ini ✓'",
                                                allowedExtensions = setOf(
                                                    "png", "jpg", "jpeg", "webp", "bmp",
                                                    "mp3", "wav", "ogg", "m4a", "aac",
                                                    "mp4", "mkv", "webm", "mov", "m4v"
                                                ),
                                                mimeTypes = arrayOf("*/*"),
                                                pickDirectoryMode = true,
                                                onDirectorySelected = { dir ->
                                                    viewModel.syncMediaFromExternalFolder(dir)
                                                }
                                            )
                                        },
                                        onRefreshStorageHealth = {
                                            viewModel.refreshStorageHealth()
                                            viewModel.postStatusMessage("Statistik penyimpanan internal diperbarui.")
                                        },
                                        onCleanOrphanMedia = {
                                            viewModel.cleanOrphanMedia()
                                        },
                                        onOpenDebugSimulator = {
                                            isSettingsModalOpen = false
                                            isDebugModalOpen = true
                                        },
                                        onToggleManualRelay = { pin, turnOn ->
                                            viewModel.toggleManualRelay(pin, turnOn)
                                        },
                                        onTestOnlineFinance = {
                                            viewModel.testOnlineFinanceConnection()
                                        },
                                        onStartDirectLive = { src, customId, mins ->
                                            viewModel.startDirectLive(src, customId, mins)
                                            isSettingsModalOpen = false
                                        },
                                        onStopDirectLive = {
                                            viewModel.stopDirectLive()
                                        },
                                        onPickImageAsset = { subFolder, onSaved ->
                                            pendingImageSubFolder = subFolder
                                            pendingImageCallback = onSaved
                                            val mimes = arrayOf("image/*")
                                            openInAppOrSystemFilePicker(
                                                title = if (subFolder == "logo") "Pilih Logo Masjid (PNG / JPG)" else "Pilih Gambar / Poster ($subFolder)",
                                                subtitle = "Pilih gambar dari Memori TV, Flashdisk USB, atau unggah langsung lewat Remote PWA",
                                                allowedExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        viewModel.importImageAsset(subFolder, file.name, FileInputStream(file), onSaved)
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal mengimpor gambar: ${err.message}")
                                                    }
                                                    pendingImageCallback = null
                                                },
                                                systemLauncherInvoke = { pickImageAssetLauncher.launch(mimes) }
                                            )
                                        },
                                        onPickVideoAsset = { onSaved ->
                                            pendingVideoCallback = onSaved
                                            val mimes = arrayOf("video/*")
                                            openInAppOrSystemFilePicker(
                                                title = "Pilih Video Slide Masjid (.mp4 / .webm / .mkv)",
                                                subtitle = "Video akan disalin ke memori internal TV (maks 250 MB) dan diputar otomatis tanpa suara",
                                                allowedExtensions = setOf("mp4", "mkv", "webm", "mov", "m4v"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        viewModel.importVideoAsset(file.name, FileInputStream(file), onSaved)
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal mengimpor video: ${err.message}")
                                                    }
                                                    pendingVideoCallback = null
                                                },
                                                systemLauncherInvoke = { pickVideoAssetLauncher.launch(mimes) }
                                            )
                                        },
                                        onPickPrayerAudio = { category, slotName ->
                                            pendingAudioCategory = category
                                            pendingAudioSlot = slotName
                                            val mimes = arrayOf("audio/*", "application/octet-stream")
                                            openInAppOrSystemFilePicker(
                                                title = "Pilih Audio ${category.uppercase()} (${slotName.uppercase()})",
                                                subtitle = "Pilih file MP3 / WAV / M4A / OGG dari Memori TV / USB atau unggah lewat Remote PWA",
                                                allowedExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        viewModel.importPrayerAudioSlot(category, slotName, FileInputStream(file))
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal mengimpor audio: ${err.message}")
                                                    }
                                                },
                                                systemLauncherInvoke = { pickPrayerAudioLauncher.launch(mimes) }
                                            )
                                        },
                                        onDeletePrayerAudio = { category, slotName ->
                                            viewModel.deletePrayerAudioSlot(category, slotName)
                                        },
                                        onTogglePreviewPrayerAudio = { category, slotName ->
                                            viewModel.togglePreviewPrayerAudio(category, slotName)
                                        },
                                        onPickCustomAudio = {
                                            val mimes = arrayOf("audio/*", "application/octet-stream")
                                            openInAppOrSystemFilePicker(
                                                title = "Pilih File Audio Kustom / Murottal",
                                                subtitle = "Pilih file MP3 / WAV / M4A / OGG dari Memori TV atau Flashdisk USB OTG",
                                                allowedExtensions = setOf("mp3", "wav", "ogg", "m4a", "aac"),
                                                mimeTypes = mimes,
                                                onFileSelected = { file ->
                                                    runCatching {
                                                        viewModel.importCustomAudioFile(file.name, FileInputStream(file))
                                                    }.onFailure { err ->
                                                        viewModel.postStatusMessage("Gagal mengimpor audio kustom: ${err.message}")
                                                    }
                                                },
                                                systemLauncherInvoke = { pickCustomAudioLauncher.launch(mimes) }
                                            )
                                        },
                                        onDeleteCustomAudio = { fileName ->
                                            viewModel.deleteCustomAudioFile(fileName)
                                        },
                                        onTogglePreviewCustomAudio = { fileName ->
                                            viewModel.togglePreviewCustomAudio(fileName)
                                        },
                                        onFindPrayerAudioSlot = { category, slotName ->
                                            viewModel.findPrayerAudioSlot(category, slotName)
                                        },
                                        onListCustomAudioFiles = {
                                            viewModel.listCustomAudioFilesWithDurations()
                                        },
                                        assetsRootDir = viewModel.assetsRootDir,
                                        storageHealth = storageHealth,
                                        remoteServerUrl = snapshot.remoteServerUrl,
                                        remoteQrPairingUrl = snapshot.remoteQrPairingUrl,
                                        remoteAuthToken = snapshot.remoteAuthToken,
                                        relayPinStates = relayPinStates,
                                        onlineFinanceTestResult = onlineFinanceTestResult,
                                        audioInventoryTick = audioInventoryTick,
                                        previewingAudioKey = previewingAudioKey,
                                        isLiveActiveNow = snapshot.tickResult.state == AppState.LIVE
                                    ),
                                    onClose = {
                                        viewModel.stopAudioPreview()
                                        isSettingsModalOpen = false
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

                            // Built-in Android TV / USB File Picker Overlay (eliminates "Tidak ada aplikasi yang dapat menangani tindakan ini")
                            val pickerReq = activeFilePickerRequest
                            if (pickerReq != null) {
                                TvFilePickerDialog(
                                    request = pickerReq,
                                    assetsRootDir = viewModel.assetsRootDir,
                                    remoteServerUrl = snapshot.remoteServerUrl,
                                    hasStoragePermission = hasStoragePermission,
                                    onRequestStoragePermission = { requestStoragePermissionsSafely() },
                                    onDismiss = { activeFilePickerRequest = null }
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
                        text = "Panel Simulasi 31-State & Uji Audio (D-Pad / Touch)",
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
