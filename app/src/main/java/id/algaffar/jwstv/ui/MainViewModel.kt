package id.algaffar.jwstv.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.algaffar.jwstv.AppContainer
import id.algaffar.jwstv.data.audio.JwsAudioEngine
import id.algaffar.jwstv.data.backup.BackupRestoreManager
import id.algaffar.jwstv.data.cctv.DiscoveredOnvifCamera
import id.algaffar.jwstv.data.cctv.OnvifDiscoveryManager
import id.algaffar.jwstv.data.excel.TakjilExcelManager
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.data.server.RemoteCommandPayload
import id.algaffar.jwstv.domain.engine.AudioFileDurations
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.FastingInfoSummary
import id.algaffar.jwstv.domain.engine.HijriCalculator
import id.algaffar.jwstv.domain.engine.HijriDateInfo
import id.algaffar.jwstv.domain.engine.IslamicEventItem
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.engine.StateMachineTickResult
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.CctvCameraConfig
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerStageConfig
import id.algaffar.jwstv.ui.theme.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.ZonedDateTime
import java.util.UUID

data class JwsRuntimeSnapshot(
    val now: ZonedDateTime,
    val settings: AppStateSettings,
    val schedule: DailyPrayerSchedule,
    val hijriDate: HijriDateInfo,
    val fastingInfo: FastingInfoSummary,
    val upcomingEvents: List<IslamicEventItem>,
    val tickResult: StateMachineTickResult,
    val remoteServerUrl: String = "http://127.0.0.1:1421/remote",
    val remoteQrPairingUrl: String = "http://127.0.0.1:1421/remote",
    val remoteAuthToken: String = ""
)

class MainViewModel(
    appContext: Context? = null,
    val assetsRootDir: File,
    val settingsRepository: SettingsRepository,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val hijriCalculator: HijriCalculator,
    private val stateMachine: JwsStateMachine,
    private val audioEngine: JwsAudioEngine,
    private val switchController: SwitchController = SwitchController(appContext)
) : ViewModel() {

    private var cachedAudioDurations = AudioFileDurations()

    val remoteServer: KtorRemoteServer = KtorRemoteServer(
        context = appContext,
        port = KtorRemoteServer.DEFAULT_PORT,
        settingsProvider = { settingsRepository.currentSettings },
        runtimeStatusProvider = {
            val currentSnap = _snapshotFlow.value
            val st = currentSnap.tickResult.state
            KtorRemoteServer.RuntimeServerSnapshot(
                currentState = st,
                isAudioPlaying = st == AppState.TARTIL || st == AppState.TARHIM || st == AppState.ADZAN,
                isCustomAudioPlaying = currentSnap.settings.customAudio.isPlayingLive
            )
        },
        onCommandReceived = { payload ->
            handleRemoteCommand(payload)
        }
    )

    private val _snapshotFlow = MutableStateFlow(computeSnapshot(ZonedDateTime.now()))
    val snapshotFlow: StateFlow<JwsRuntimeSnapshot> = _snapshotFlow.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow("")
    val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

    private val _takjilPreviewFlow = MutableStateFlow<List<TakjilImportPreviewRow>?>(null)
    val takjilPreviewFlow: StateFlow<List<TakjilImportPreviewRow>?> = _takjilPreviewFlow.asStateFlow()

    val relayPinStatesFlow: StateFlow<Map<Int, Boolean>> = switchController.pinStateFlow

    private val _discoveredOnvifCameras = MutableStateFlow<List<DiscoveredOnvifCamera>>(emptyList())
    val discoveredOnvifCameras: StateFlow<List<DiscoveredOnvifCamera>> = _discoveredOnvifCameras.asStateFlow()

    private val _openSettingsRequestCount = MutableStateFlow(0)
    val openSettingsRequestCount: StateFlow<Int> = _openSettingsRequestCount.asStateFlow()

    init {
        refreshAudioDurationsAsync()

        // Start Embedded Ktor Remote Server on Port 1421
        viewModelScope.launch(Dispatchers.IO) {
            remoteServer.start()
            _snapshotFlow.value = computeSnapshot(ZonedDateTime.now())
        }

        // 1Hz real-time ticker driving StateMachine, Media3 AudioEngine & 8-Channel SwitchController
        viewModelScope.launch {
            while (isActive) {
                val snap = computeSnapshot(ZonedDateTime.now())
                _snapshotFlow.value = snap
                audioEngine.onTick(
                    now = snap.now,
                    settings = snap.settings,
                    schedule = snap.schedule,
                    tickResult = snap.tickResult
                )
                switchController.evaluateAndDispatch(
                    now = snap.now,
                    currentState = snap.tickResult.state,
                    switchSettings = snap.settings.switchSettings,
                    schedule = snap.schedule
                )
                delay(1_000L)
            }
        }
    }

    fun refreshAudioDurationsAsync() {
        viewModelScope.launch {
            cachedAudioDurations = withContext(Dispatchers.IO) {
                audioEngine.scanAudioFileDurations()
            }
        }
    }

    private fun computeSnapshot(now: ZonedDateTime): JwsRuntimeSnapshot {
        val settings = settingsRepository.currentSettings
        val zoneId = prayerTimeCalculator.resolveZoneId(settings.prayerSettings.timezone)
        val zonedNow = now.withZoneSameInstant(zoneId)
        val schedule = prayerTimeCalculator.calculateSchedule(zonedNow, settings.prayerSettings)
        val maghribTime = schedule.timeFor(PrayerId.MAGHRIB)
        val hijriAdjustment = settings.prayerSettings.hijriAdjustment

        val hijriDate = hijriCalculator.calculateHijriDate(
            now = zonedNow,
            maghribTime = maghribTime,
            hijriAdjustment = hijriAdjustment
        )
        val fastingInfo = hijriCalculator.getFastingInfo(
            now = zonedNow,
            maghribTime = maghribTime,
            hijriAdjustment = hijriAdjustment
        )
        val upcomingEvents = hijriCalculator.getUpcomingIslamicEvents(
            now = zonedNow,
            maghribTime = maghribTime,
            hijriAdjustment = hijriAdjustment,
            maxDays = 60
        )
        val tickResult = stateMachine.evaluateTick(
            now = zonedNow,
            settings = settings,
            schedule = schedule,
            audioDurations = cachedAudioDurations
        )

        return JwsRuntimeSnapshot(
            now = zonedNow,
            settings = settings,
            schedule = schedule,
            hijriDate = hijriDate,
            fastingInfo = fastingInfo,
            upcomingEvents = upcomingEvents,
            tickResult = tickResult,
            remoteServerUrl = remoteServer.getRemoteUrl(includeTokenQuery = false),
            remoteQrPairingUrl = remoteServer.getRemoteUrl(includeTokenQuery = true),
            remoteAuthToken = remoteServer.authToken
        )
    }

    fun simulateState(state: AppState?, durationSeconds: Long? = 120L) {
        val nowEpoch = ZonedDateTime.now().toEpochSecond()
        stateMachine.setManualOverride(state, durationSeconds, nowEpoch)
        val snap = computeSnapshot(ZonedDateTime.now())
        _snapshotFlow.value = snap
        audioEngine.onTick(snap.now, snap.settings, snap.schedule, snap.tickResult)
    }

    fun clearSimulation() {
        stateMachine.clearManualOverride()
        val snap = computeSnapshot(ZonedDateTime.now())
        _snapshotFlow.value = snap
        audioEngine.onTick(snap.now, snap.settings, snap.schedule, snap.tickResult)
    }

    fun testPlayBeep1000Hz() {
        audioEngine.beepGenerator.playBeepImmediate()
    }

    fun testPlayRamadhanSirine() {
        audioEngine.beepGenerator.playSynthesizedSirenSweep()
    }

    fun toggleManualRelay(pin: Int, turnOn: Boolean) {
        viewModelScope.launch {
            val res = switchController.setManualPinState(
                pin = pin,
                turnOn = turnOn,
                settings = settingsRepository.currentSettings.switchSettings
            )
            res.onSuccess {
                _statusBannerMessage.value = "Saklar Relay Pin $pin -> ${if (turnOn) "ON" else "OFF"} terkirim."
            }.onFailure { err ->
                _statusBannerMessage.value = "Saklar Relay Pin $pin (${if (turnOn) "ON" else "OFF"}): ${err.message}"
            }
        }
    }

    fun discoverOnvifCameras() {
        viewModelScope.launch {
            _statusBannerMessage.value = "Memindai kamera ONVIF di jaringan lokal (UDP 239.255.255.250:3702)..."
            val found = OnvifDiscoveryManager.discoverCameras()
            _discoveredOnvifCameras.value = found
            if (found.isEmpty()) {
                _statusBannerMessage.value = "Tidak ditemukan kamera ONVIF baru pada jaringan lokal."
            } else {
                updateSettings { prev ->
                    val existingUrls = prev.cctv.cameras.map { it.url }.toSet()
                    val added = found.filter { it.url !in existingUrls }.mapIndexed { idx, cam ->
                        CctvCameraConfig(
                            id = "onvif-${System.currentTimeMillis()}-$idx",
                            name = cam.name,
                            type = "ip",
                            url = cam.url,
                            enabled = true
                        )
                    }
                    prev.copy(cctv = prev.cctv.copy(cameras = prev.cctv.cameras + added))
                }
                _statusBannerMessage.value = "Ditemukan ${found.size} kamera ONVIF di jaringan lokal!"
            }
        }
    }

    internal fun handleRemoteCommand(payload: RemoteCommandPayload) {
        viewModelScope.launch {
            when (payload.action.lowercase()) {
                "screensaver" -> simulateState(AppState.SCREENSAVER, 3600L)
                "stop", "refresh", "play_slideshow" -> clearSimulation()
                "next_theme" -> cycleTheme()
                "next_layout" -> cycleLayout()
                "next_clock_style" -> cycleClockStyle()
                "open_settings" -> _openSettingsRequestCount.value += 1
                "play_jumat" -> simulateState(AppState.WAKTU_JUMAT, 600L)
                "play_ramadhan" -> simulateState(AppState.TARAWIH, 600L)
                "play_idul_fitri" -> simulateState(AppState.IDUL_FITRI, 600L)
                "play_idul_adha" -> simulateState(AppState.IDUL_ADHA, 600L)
                "play_cctv" -> simulateState(AppState.CCTV, 600L)
                "play_live" -> simulateState(AppState.LIVE, 600L)
                "play_infaq" -> simulateState(AppState.MASJID_QR, 300L)
                "play_online_finance" -> simulateState(AppState.ONLINE_FINANCE, 300L)
                "test_adzan" -> simulateState(AppState.ADZAN, 180L)

                "play_custom_audio" -> {
                    updateSettings { prev ->
                        prev.copy(
                            customAudio = prev.customAudio.copy(
                                isPlayingLive = !prev.customAudio.isPlayingLive
                            )
                        )
                    }
                }

                "set_live_source" -> {
                    val videoId = payload.stringArg("videoId").orEmpty()
                    if (videoId.isNotBlank()) {
                        val src = when (videoId) {
                            "Cm1v4bteXbI" -> "makkah"
                            "9A1S0xAPVIs" -> "madinah"
                            else -> "custom"
                        }
                        updateSettings { prev ->
                            prev.copy(
                                live = prev.live.copy(
                                    enabled = true,
                                    source = src,
                                    customVideoId = videoId
                                )
                            )
                        }
                        simulateState(AppState.LIVE, 900L)
                    }
                }

                "toggle_mode" -> {
                    val mode = payload.stringArg("mode").orEmpty().lowercase()
                    updateSettings { prev ->
                        when (mode) {
                            "jumat" -> prev.copy(jumat = prev.jumat.copy(enabled = !prev.jumat.enabled))
                            "ramadhan" -> prev.copy(ramadhan = prev.ramadhan.copy(isRamadhan = !prev.ramadhan.isRamadhan))
                            "idul_fitri" -> prev.copy(idulFitri = prev.idulFitri.copy(enabled = !prev.idulFitri.enabled))
                            "idul_adha" -> prev.copy(idulAdha = prev.idulAdha.copy(enabled = !prev.idulAdha.enabled))
                            "slideshow" -> prev.copy(
                                standbySlideshow = prev.standbySlideshow.copy(
                                    enabled = !prev.standbySlideshow.enabled
                                )
                            )
                            else -> prev
                        }
                    }
                }

                "toggle_prayer_setting" -> {
                    val prayer = payload.stringArg("prayer").orEmpty().lowercase()
                    val settingKey = payload.stringArg("setting").orEmpty()
                    fun toggleStage(cfg: PrayerStageConfig): PrayerStageConfig = when (settingKey) {
                        "isTartil" -> cfg.copy(tartilDuration = if (cfg.tartilDuration > 0) 0 else 10)
                        "isTarhim" -> cfg.copy(tarhimDuration = if (cfg.tarhimDuration > 0) 0 else 5)
                        "isAdzan" -> cfg.copy(isAdzan = !cfg.isAdzan)
                        else -> cfg
                    }
                    updateSettings { prev ->
                        val p = prev.prayers
                        val updatedPrayers = when (prayer) {
                            "fajr" -> p.copy(fajr = toggleStage(p.fajr))
                            "dhuhr" -> p.copy(dhuhr = toggleStage(p.dhuhr))
                            "asr" -> p.copy(asr = toggleStage(p.asr))
                            "maghrib" -> p.copy(maghrib = toggleStage(p.maghrib))
                            "isha" -> p.copy(isha = toggleStage(p.isha))
                            else -> p
                        }
                        prev.copy(prayers = updatedPrayers)
                    }
                }

                "update_identity" -> {
                    val newName = payload.stringArg("name")
                    val newAddr = payload.stringArg("address")
                    updateSettings { prev ->
                        prev.copy(
                            brand = prev.brand.copy(
                                name = newName?.takeIf { it.isNotBlank() } ?: prev.brand.name,
                                address = newAddr?.takeIf { it.isNotBlank() } ?: prev.brand.address
                            )
                        )
                    }
                }

                "add_overlay_text" -> {
                    val text = payload.stringArg("text")
                    if (!text.isNullOrBlank()) {
                        updateSettings { prev ->
                            prev.copy(overlayTextList = prev.overlayTextList + text)
                        }
                    }
                }

                "clear_overlay_text" -> {
                    updateSettings { prev -> prev.copy(overlayTextList = emptyList()) }
                }

                "add_info" -> {
                    val title = payload.stringArg("title").orEmpty().ifBlank { "Info Masjid" }
                    val subtitle = payload.stringArg("subtitle").orEmpty()
                    val content = payload.stringArg("content").orEmpty()
                    if (content.isNotBlank()) {
                        val combinedContent = if (subtitle.isNotBlank()) "$subtitle — $content" else content
                        updateSettings { prev ->
                            prev.copy(
                                mosqueInfoList = prev.mosqueInfoList + InfoCardItem(
                                    id = "remote-${UUID.randomUUID()}",
                                    title = title,
                                    content = combinedContent,
                                    enabled = true
                                )
                            )
                        }
                    }
                }

                "clear_info" -> {
                    updateSettings { prev -> prev.copy(mosqueInfoList = emptyList()) }
                }
            }
        }
    }

    fun cycleTheme() {
        val builtInIds = ThemeEngine.BuiltInThemes.map { it.id }
        updateSettings { prev ->
            val currentIdx = builtInIds.indexOf(prev.themeSettings.currentThemeId)
            val nextId = builtInIds[(currentIdx + 1) % builtInIds.size]
            prev.copy(
                themeSettings = prev.themeSettings.copy(
                    currentThemeId = nextId,
                    autoThemeEnabled = false
                )
            )
        }
    }

    fun cycleLayout() {
        val positions = LayoutPosition.entries
        updateSettings { prev ->
            val currentIdx = positions.indexOf(prev.layoutSettings.position)
            val nextPos = positions[(currentIdx + 1) % positions.size]
            prev.copy(
                layoutSettings = prev.layoutSettings.copy(
                    position = nextPos,
                    autoRotateLayout = false
                )
            )
        }
    }

    fun cycleClockStyle() {
        val styles = listOf("classic", "golden", "glass", "wood", "neon")
        updateSettings { prev ->
            val currentIdx = styles.indexOf(prev.brand.clockStyle.lowercase())
            val nextStyle = styles[(currentIdx + 1) % styles.size]
            prev.copy(
                brand = prev.brand.copy(clockStyle = nextStyle)
            )
        }
    }

    fun updateSettings(transform: (AppStateSettings) -> AppStateSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
            _snapshotFlow.value = computeSnapshot(ZonedDateTime.now())
        }
    }

    // ==================== EXCEL TA'JIL IMPORT & EXPORT ====================

    fun exportTakjilExcelTemplate(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                TakjilExcelManager.exportTemplateExcel(
                    outputStream = outputStream,
                    existingTakjil = settingsRepository.currentSettings.ramadhan.takjil
                )
            }.onSuccess {
                _statusBannerMessage.value = "Template Excel Ta'jil (.xlsx) berhasil disimpan."
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal menyimpan template Excel: ${err.message}"
            }
        }
    }

    fun parseTakjilExcelPreview(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                TakjilExcelManager.parseTakjilExcel(inputStream)
            }.onSuccess { rows ->
                if (rows.isEmpty()) {
                    _statusBannerMessage.value = "File Excel tidak berisi data jadwal Ta'jil yang valid."
                } else {
                    _takjilPreviewFlow.value = rows
                }
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal membaca file Excel: ${err.message}"
            }
        }
    }

    fun confirmTakjilImport() {
        val rows = _takjilPreviewFlow.value ?: return
        _takjilPreviewFlow.value = null
        updateSettings { prev ->
            val newMap = prev.ramadhan.takjil.toMutableMap()
            rows.forEach { row ->
                newMap[row.day.toString()] = row.names
            }
            prev.copy(ramadhan = prev.ramadhan.copy(takjil = newMap))
        }
        val totalNames = rows.sumOf { it.names.size }
        _statusBannerMessage.value = "Berhasil mengimpor $totalNames nama penyumbang Ta'jil untuk ${rows.size} hari!"
    }

    fun dismissTakjilImport() {
        _takjilPreviewFlow.value = null
    }

    // ==================== ZIP BACKUP & LEGACY TAURI RESTORE ====================

    fun exportFullBackupZip(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                BackupRestoreManager.exportFullBackupZip(
                    outputStream = outputStream,
                    settingsRepository = settingsRepository,
                    assetsRootDir = assetsRootDir
                )
            }.onSuccess {
                _statusBannerMessage.value = "Full Backup (.zip) berhasil diekspor!"
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal mengekspor Backup (.zip): ${err.message}"
            }
        }
    }

    fun importFullBackupZip(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = BackupRestoreManager.restoreFromBackupZip(
                inputStream = inputStream,
                settingsRepository = settingsRepository,
                assetsRootDir = assetsRootDir
            )
            res.onSuccess { summary ->
                refreshAudioDurationsAsync()
                _snapshotFlow.value = computeSnapshot(ZonedDateTime.now())
                _statusBannerMessage.value = "Restore berhasil (${summary.restoredMosqueName})! " +
                    "Diekstrak ${summary.extractedAssetFilesCount} file media."
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal memulihkan Backup (.zip): ${err.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        remoteServer.stop()
        audioEngine.release()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        appContext = container.appContext,
                        assetsRootDir = container.assetsRootDir,
                        settingsRepository = container.settingsRepository,
                        prayerTimeCalculator = container.prayerTimeCalculator,
                        hijriCalculator = container.hijriCalculator,
                        stateMachine = container.stateMachine,
                        audioEngine = container.audioEngine,
                        switchController = container.switchController
                    ) as T
                }
            }
    }
}
