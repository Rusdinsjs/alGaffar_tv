package id.algaffar.jwstv.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.algaffar.jwstv.AppContainer
import id.algaffar.jwstv.data.audio.JwsAudioEngine
import id.algaffar.jwstv.data.backup.BackupRestoreManager
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.cctv.OnlineFinanceManager
import id.algaffar.jwstv.data.excel.TakjilExcelManager
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.logging.AppLogger
import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
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
import android.view.KeyEvent
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerStageConfig
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.ui.theme.ThemeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val appContext: Context? = null,
    val assetsRootDir: File,
    val settingsRepository: SettingsRepository,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val hijriCalculator: HijriCalculator,
    private val stateMachine: JwsStateMachine,
    private val audioEngine: JwsAudioEngine,
    private val switchController: SwitchController = SwitchController(appContext)
) : ViewModel() {

    private var cachedAudioDurations = AudioFileDurations()

    private val _storageHealthFlow = MutableStateFlow(
        MediaStorageManager.inspectStorage(assetsRootDir, settingsRepository.currentSettings)
    )
    val storageHealthFlow: StateFlow<StorageHealthSnapshot> = _storageHealthFlow.asStateFlow()

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
        audioInventoryProvider = { buildAudioInventoryMap() },
        storageHealthProvider = { _storageHealthFlow.value },
        onCommandReceived = { payload ->
            handleRemoteCommand(payload)
        },
        onUploadReceived = { category, slot, fileName, bytes ->
            handleRemoteUploadStream(category, slot, fileName, bytes.inputStream())
        },
        onUploadStreamReceived = { category, slot, fileName, inputStream ->
            handleRemoteUploadStream(category, slot, fileName, inputStream)
        }
    )

    private val _snapshotFlow = MutableStateFlow(computeSnapshot(ZonedDateTime.now()))
    val snapshotFlow: StateFlow<JwsRuntimeSnapshot> = _snapshotFlow.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow("")
    val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

    private val _takjilPreviewFlow = MutableStateFlow<List<TakjilImportPreviewRow>?>(null)
    val takjilPreviewFlow: StateFlow<List<TakjilImportPreviewRow>?> = _takjilPreviewFlow.asStateFlow()

    val relayPinStatesFlow: StateFlow<Map<Int, Boolean>> = switchController.pinStateFlow

    private val _openSettingsRequestCount = MutableStateFlow(0)
    val openSettingsRequestCount: StateFlow<Int> = _openSettingsRequestCount.asStateFlow()

    private val _remoteNavKeyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 24)
    val remoteNavKeyEvents: SharedFlow<Int> = _remoteNavKeyEvents.asSharedFlow()

    private val onlineFinanceManager = OnlineFinanceManager()

    private val _onlineFinanceTestResult = MutableStateFlow<FinancialSummaryData?>(null)
    val onlineFinanceTestResult: StateFlow<FinancialSummaryData?> = _onlineFinanceTestResult.asStateFlow()

    private val _audioInventoryTick = MutableStateFlow(0)
    val audioInventoryTick: StateFlow<Int> = _audioInventoryTick.asStateFlow()

    private val _previewingAudioKey = MutableStateFlow<String?>(null)
    val previewingAudioKey: StateFlow<String?> = _previewingAudioKey.asStateFlow()

    fun postStatusMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    fun refreshStorageHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageHealthFlow.value = MediaStorageManager.inspectStorage(
                assetsRootDir = assetsRootDir,
                settings = settingsRepository.currentSettings
            )
        }
    }

    private fun buildAudioInventoryMap(): Map<String, List<String>> {
        val slots = listOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumat", "default")
        val categories = listOf("tartil", "tarhim", "adzan")
        val map = mutableMapOf<String, List<String>>()
        for (cat in categories) {
            map[cat] = slots.filter { slot -> audioEngine.findSlotAudioFile(cat, slot) != null }
        }
        map["custom"] = audioEngine.listCustomAudioFiles().map { it.name }
        return map
    }

    private fun handleRemoteUploadStream(
        category: String,
        slot: String,
        rawFileName: String,
        inputStream: InputStream
    ): Result<String> {
        return runCatching {
            val cleanName = MediaStorageManager.sanitizeFileName(rawFileName)

            val resultMsg = when (category) {
                "logo" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(
                        assetsRootDir = assetsRootDir,
                        subFolder = "logo",
                        rawFileName = cleanName,
                        inputStream = inputStream
                    ).getOrThrow()
                    updateSettings { prev -> prev.copy(brand = prev.brand.copy(logo = saved.relativePath)) }
                    AppLogger.info(
                        "REMOTE",
                        "Logo diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)}): ${saved.relativePath}"
                    )
                    "Logo masjid berhasil diperbarui (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "slide_image" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(
                        assetsRootDir = assetsRootDir,
                        subFolder = "slides",
                        rawFileName = cleanName,
                        inputStream = inputStream
                    ).getOrThrow()
                    val title = cleanName.substringBeforeLast('.').replace('_', ' ').ifBlank { "Poster Masjid" }
                    val modeSet = if (slot.isNotBlank()) slot.split(',').map { it.trim().lowercase() }.toSet() else setOf("main", "slideshow")
                    val newSlide = SlideItem(
                        id = "slide_${System.currentTimeMillis()}",
                        title = title,
                        type = "image",
                        imageUrl = saved.relativePath,
                        duration = 12,
                        enabled = true,
                        showInMain = "main" in modeSet,
                        showInSlideshow = "slideshow" in modeSet,
                        showInJumat = "jumat" in modeSet,
                        showInRamadhan = "ramadhan" in modeSet,
                        showInIdulFitri = "idul_fitri" in modeSet,
                        showInIdulAdha = "idul_adha" in modeSet
                    )
                    updateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
                    val savedInfo = if (saved.wasOptimized) {
                        "dioptimasi ${MediaStorageManager.formatByteSize(saved.originalBytes)} → ${MediaStorageManager.formatByteSize(saved.savedBytes)}"
                    } else {
                        MediaStorageManager.formatByteSize(saved.savedBytes)
                    }
                    AppLogger.info("REMOTE", "Slide gambar diunggah ($savedInfo): ${saved.relativePath}")
                    "Poster '$title' berhasil ditambahkan ($savedInfo)!"
                }

                "slide_video", "video" -> {
                    val ext = cleanName.substringAfterLast('.', "mp4").lowercase()
                        .takeIf { it in MediaStorageManager.VIDEO_EXTENSIONS } ?: "mp4"
                    val base = cleanName.substringBeforeLast('.').ifBlank { "video" }
                    val fileName = "${System.currentTimeMillis()}_${base}.$ext"
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = "videos/$fileName",
                        inputStream = inputStream,
                        maxBytes = MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
                    ).getOrThrow()
                    val title = base.replace('_', ' ').ifBlank { "Video Masjid" }
                    val modeSet = if (slot.isNotBlank()) slot.split(',').map { it.trim().lowercase() }.toSet() else setOf("main", "slideshow")
                    val newSlide = SlideItem(
                        id = "slide_vid_${System.currentTimeMillis()}",
                        title = title,
                        type = "video",
                        videoUrl = saved.relativePath,
                        duration = 30,
                        enabled = true,
                        showInMain = "main" in modeSet,
                        showInSlideshow = "slideshow" in modeSet,
                        showInJumat = "jumat" in modeSet,
                        showInRamadhan = "ramadhan" in modeSet,
                        showInIdulFitri = "idul_fitri" in modeSet,
                        showInIdulAdha = "idul_adha" in modeSet
                    )
                    updateSettings { prev -> prev.copy(slides = prev.slides + newSlide) }
                    AppLogger.info(
                        "REMOTE",
                        "Slide video diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)}): ${saved.relativePath}"
                    )
                    "Video slide '$title' berhasil ditambahkan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "media_bg" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(
                        assetsRootDir = assetsRootDir,
                        subFolder = "media",
                        rawFileName = cleanName,
                        inputStream = inputStream
                    ).getOrThrow()
                    val label = when (slot) {
                        "doa_adzan" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(doaAdzanImage = saved.relativePath)) }
                            "Do'a Setelah Adzan"
                        }
                        "khutbah_jumat" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahJumatImage = saved.relativePath)) }
                            "Latar Khutbah Jum'at"
                        }
                        "khutbah_idul_fitri" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahIdulFitriImage = saved.relativePath)) }
                            "Latar Khutbah Idul Fitri"
                        }
                        "khutbah_idul_adha" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahIdulAdhaImage = saved.relativePath)) }
                            "Latar Khutbah Idul Adha"
                        }
                        "ceramah_tarawih" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(ceramahTarawihImage = saved.relativePath)) }
                            "Latar Ceramah Tarawih"
                        }
                        "sholat_5_waktu" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(sholat5WaktuImage = saved.relativePath)) }
                            "Latar Sholat 5 Waktu"
                        }
                        "sholat_jumat" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(sholatJumatImage = saved.relativePath)) }
                            "Latar Sholat Jum'at"
                        }
                        "sholat_idul_fitri" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(sholatIdulFitriImage = saved.relativePath)) }
                            "Latar Sholat Idul Fitri"
                        }
                        "sholat_idul_adha" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(sholatIdulAdhaImage = saved.relativePath)) }
                            "Latar Sholat Idul Adha"
                        }
                        "sholat_tarawih_witir" -> {
                            updateSettings { prev -> prev.copy(media = prev.media.copy(sholatTarawihWitirImage = saved.relativePath)) }
                            "Latar Sholat Tarawih & Witir"
                        }
                        else -> throw IllegalArgumentException("Slot media latar tidak dikenal: $slot")
                    }
                    AppLogger.info("REMOTE", "Gambar latar $label diunggah: ${saved.relativePath}")
                    "Gambar $label berhasil diperbarui (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tartil_image" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(
                        assetsRootDir = assetsRootDir,
                        subFolder = "media/tartil",
                        rawFileName = cleanName,
                        inputStream = inputStream
                    ).getOrThrow()
                    updateSettings { prev ->
                        prev.copy(
                            media = prev.media.copy(
                                tartilMediaType = "image",
                                tartilImages = prev.media.tartilImages + saved.relativePath
                            )
                        )
                    }
                    AppLogger.info("REMOTE", "Gambar slide Tartil diunggah: ${saved.relativePath}")
                    "Gambar slide Tartil berhasil diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tartil_video" -> {
                    val safeName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = "media/tartil/${System.currentTimeMillis()}_$safeName",
                        inputStream = inputStream,
                        maxBytes = MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
                    ).getOrThrow()
                    updateSettings { prev ->
                        prev.copy(
                            media = prev.media.copy(
                                tartilMediaType = "video",
                                tartilVideo = saved.relativePath
                            )
                        )
                    }
                    AppLogger.info("REMOTE", "Video Tartil diunggah: ${saved.relativePath}")
                    "Video Tartil berhasil diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tarhim_image" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(
                        assetsRootDir = assetsRootDir,
                        subFolder = "media/tarhim",
                        rawFileName = cleanName,
                        inputStream = inputStream
                    ).getOrThrow()
                    updateSettings { prev ->
                        prev.copy(
                            media = prev.media.copy(
                                tarhimMediaType = "image",
                                tarhimImages = prev.media.tarhimImages + saved.relativePath
                            )
                        )
                    }
                    AppLogger.info("REMOTE", "Gambar slide Tarhim diunggah: ${saved.relativePath}")
                    "Gambar slide Tarhim berhasil diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tarhim_video" -> {
                    val safeName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = "media/tarhim/${System.currentTimeMillis()}_$safeName",
                        inputStream = inputStream,
                        maxBytes = MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
                    ).getOrThrow()
                    updateSettings { prev ->
                        prev.copy(
                            media = prev.media.copy(
                                tarhimMediaType = "video",
                                tarhimVideo = saved.relativePath
                            )
                        )
                    }
                    AppLogger.info("REMOTE", "Video Tarhim diunggah: ${saved.relativePath}")
                    "Video Tarhim berhasil diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tartil", "tarhim", "adzan" -> {
                    val safeSlot = slot.takeIf { it in setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumat", "default") }
                        ?: "default"
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = "audio/$category/$safeSlot.mp3",
                        inputStream = inputStream,
                        maxBytes = MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
                    ).getOrThrow()
                    refreshAudioDurationsAsync()
                    _audioInventoryTick.value += 1
                    AppLogger.info(
                        "REMOTE",
                        "Audio $category/$safeSlot diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)})"
                    )
                    "Audio $category ($safeSlot) berhasil disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "custom" -> {
                    val finalName = if (cleanName.contains('.')) cleanName else "$cleanName.mp3"
                    val saved = MediaStorageManager.saveStreamWithGuard(
                        assetsRootDir = assetsRootDir,
                        targetRelativePath = "audio/custom/$finalName",
                        inputStream = inputStream,
                        maxBytes = MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
                    ).getOrThrow()
                    _audioInventoryTick.value += 1
                    AppLogger.info(
                        "REMOTE",
                        "Audio kustom diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)}): $finalName"
                    )
                    "Audio kustom '$finalName' berhasil ditambahkan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                else -> throw IllegalArgumentException("Kategori upload tidak dikenal: $category")
            }

            refreshStorageHealth()
            resultMsg
        }
    }

    private var lastLoggedState: AppState? = null

    init {
        AppLogger.info("SYSTEM", "JWS TV Masjid Al-Gaffar dimulai (31-State Engine aktif)")
        refreshAudioDurationsAsync()
        refreshStorageHealth()

        // Start Embedded Ktor Remote Server on Port 1421
        viewModelScope.launch(Dispatchers.IO) {
            remoteServer.start()
            AppLogger.info("REMOTE", "Ktor Remote Server aktif di ${remoteServer.getRemoteUrl(false)}")
            _snapshotFlow.value = computeSnapshot(ZonedDateTime.now())
        }

        // 1Hz real-time ticker driving StateMachine, Media3 AudioEngine & 8-Channel SwitchController
        viewModelScope.launch {
            while (isActive) {
                val snap = computeSnapshot(ZonedDateTime.now())
                _snapshotFlow.value = snap
                if (snap.tickResult.state != lastLoggedState) {
                    AppLogger.info("STATE", "Transisi state -> ${snap.tickResult.state.serialName} (${snap.tickResult.state.displayLabel})")
                    lastLoggedState = snap.tickResult.state
                }
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

        // Proactive Memory Watchdog (every 30s): stabilizes RAM if heap or system RAM >= 88-90%
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(30_000L)
                checkAndRecoverMemoryIfNeeded()
            }
        }
    }

    private fun checkAndRecoverMemoryIfNeeded() {
        val runtime = Runtime.getRuntime()
        val maxMem = runtime.maxMemory().coerceAtLeast(1L)
        val usedHeap = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
        val heapPct = usedHeap.toDouble() / maxMem.toDouble()

        var isSystemLow = false
        var sysRamPct = 0.0
        appContext?.let { ctx ->
            runCatching {
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                val mi = android.app.ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                if (mi.totalMem > 0L) {
                    sysRamPct = (mi.totalMem - mi.availMem).coerceAtLeast(0L).toDouble() / mi.totalMem.toDouble()
                }
                isSystemLow = mi.lowMemory || sysRamPct >= 0.90
            }
        }

        if (heapPct >= 0.85 || isSystemLow) {
            val aggressive = heapPct >= 0.90 || isSystemLow
            val reason = "Watchdog(heap=${(heapPct * 100).toInt()}%, sys=${(sysRamPct * 100).toInt()}%)"
            val app = appContext?.applicationContext as? id.algaffar.jwstv.JwsApplication
            if (app != null) {
                app.performMemoryRecovery(reason, aggressive = aggressive)
            } else if (aggressive) {
                runCatching { audioEngine.releaseIfIdle() }
                AppLogger.trimTo(50)
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
        val schedule = prayerTimeCalculator.calculateSchedule(
            now = zonedNow,
            settings = settings.prayerSettings,
            isJumatEnabled = settings.jumat.enabled
        )
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
            maxDays = settings.infoSettings.lookaheadDays.coerceIn(7, 365),
            customEvents = settings.infoSettings.islamicEvents
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

    private fun toggleOrSimulate(target: AppState, durationSeconds: Long) {
        val cur = _snapshotFlow.value?.tickResult
        if (cur?.state == target && cur.isManualOverride) {
            stopAllAndReturnToStandby()
        } else {
            simulateState(target, durationSeconds)
        }
    }

    private fun stopAllAndReturnToStandby() {
        audioEngine.stopPlayback()
        updateSettings { prev ->
            if (!prev.customAudio.isPlayingLive && !prev.live.directLiveEnabled) {
                prev
            } else {
                prev.copy(
                    customAudio = prev.customAudio.copy(isPlayingLive = false),
                    live = prev.live.copy(directLiveEnabled = false)
                )
            }
        }
        clearSimulation()
        // If automatic schedule currently lands on standby_slideshow or another non-standby idle state
        // right when user explicitly hits "Kembali ke Standby", ensure immediate STANDBY transition.
        if (_snapshotFlow.value?.tickResult?.state == AppState.STANDBY_SLIDESHOW) {
            simulateState(AppState.STANDBY, 10L)
        }
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

    internal fun handleRemoteCommand(payload: RemoteCommandPayload) {
        viewModelScope.launch {
            when (payload.action.lowercase()) {
                // Virtual Remote D-Pad Navigation dispatched directly to MainActivity
                "nav_up" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_UP)
                "nav_down" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_DOWN)
                "nav_left" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_LEFT)
                "nav_right" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_RIGHT)
                "nav_enter" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_DPAD_CENTER)
                "nav_back" -> _remoteNavKeyEvents.tryEmit(KeyEvent.KEYCODE_BACK)

                "screensaver" -> toggleOrSimulate(AppState.SCREENSAVER, 3600L)
                "stop", "refresh" -> stopAllAndReturnToStandby()
                "play_slideshow" -> toggleOrSimulate(AppState.STANDBY_SLIDESHOW, 600L)
                "next_theme" -> cycleTheme()
                "next_layout" -> cycleLayout()
                "next_clock_style" -> cycleClockStyle()
                "open_settings" -> _openSettingsRequestCount.value += 1
                "play_jumat" -> toggleOrSimulate(AppState.WAKTU_JUMAT, 600L)
                "play_ramadhan" -> toggleOrSimulate(AppState.TARAWIH, 600L)
                "play_idul_fitri" -> toggleOrSimulate(AppState.IDUL_FITRI, 600L)
                "play_idul_adha" -> toggleOrSimulate(AppState.IDUL_ADHA, 600L)
                "play_live" -> toggleOrSimulate(AppState.LIVE, 600L)
                "play_infaq" -> toggleOrSimulate(AppState.MASJID_QR, 300L)
                "play_online_finance" -> toggleOrSimulate(AppState.ONLINE_FINANCE, 300L)
                "test_adzan" -> toggleOrSimulate(AppState.ADZAN, 180L)
                "test_beep" -> testPlayBeep1000Hz()

                "set_theme" -> {
                    val themeId = payload.stringArg("themeId").orEmpty()
                    if (themeId.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                themeSettings = prev.themeSettings.copy(
                                    currentThemeId = themeId,
                                    autoThemeEnabled = false
                                )
                            )
                        }
                    }
                }

                "set_layout" -> {
                    val posStr = payload.stringArg("position").orEmpty()
                    val pos = LayoutPosition.entries.firstOrNull { it.serialName == posStr }
                    if (pos != null) {
                        updateSettings { prev ->
                            prev.copy(
                                layoutSettings = prev.layoutSettings.copy(
                                    position = pos,
                                    autoRotateLayout = false
                                )
                            )
                        }
                    }
                }

                "set_clock_style" -> {
                    val style = payload.stringArg("style").orEmpty().lowercase()
                    if (style.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(brand = prev.brand.copy(clockStyle = style))
                        }
                    }
                }

                "update_volume" -> {
                    val vol = payload.doubleArg("volume")?.toFloat()?.coerceIn(0f, 1f)
                    if (vol != null) {
                        updateSettings { prev -> prev.copy(globalVolume = vol) }
                    }
                }

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
                    val requestedSource = payload.stringArg("source")?.lowercase()
                    val videoId = payload.stringArg("videoId").orEmpty().trim()
                    updateSettings { prev ->
                        val src = when (requestedSource) {
                            "makkah", "madinah", "custom" -> requestedSource
                            else -> when {
                                videoId == prev.live.makkahVideoId || videoId == "Cm1v4bteXbI" -> "makkah"
                                videoId == prev.live.madinahVideoId || videoId == "9A1S0xAPVIs" -> "madinah"
                                else -> "custom"
                            }
                        }
                        val updatedLive = when (src) {
                            "makkah" -> prev.live.copy(
                                enabled = true,
                                source = "makkah",
                                makkahVideoId = videoId.ifBlank { prev.live.makkahVideoId }
                            )
                            "madinah" -> prev.live.copy(
                                enabled = true,
                                source = "madinah",
                                madinahVideoId = videoId.ifBlank { prev.live.madinahVideoId }
                            )
                            else -> prev.live.copy(
                                enabled = true,
                                source = "custom",
                                customVideoId = videoId.ifBlank { prev.live.customVideoId }
                            )
                        }
                        prev.copy(live = updatedLive)
                    }
                    simulateState(AppState.LIVE, 900L)
                }

                "save_live_presets" -> {
                    val makkahId = payload.stringArg("makkahVideoId")?.trim()
                    val madinahId = payload.stringArg("madinahVideoId")?.trim()
                    val customId = payload.stringArg("customVideoId")?.trim()
                    updateSettings { prev ->
                        prev.copy(
                            live = prev.live.copy(
                                makkahVideoId = makkahId?.ifBlank { prev.live.makkahVideoId } ?: prev.live.makkahVideoId,
                                madinahVideoId = madinahId?.ifBlank { prev.live.madinahVideoId } ?: prev.live.madinahVideoId,
                                customVideoId = customId ?: prev.live.customVideoId
                            )
                        )
                    }
                }

                "toggle_live_mute" -> {
                    val muted = payload.booleanArg("muted")
                    updateSettings { prev ->
                        prev.copy(
                            live = prev.live.copy(
                                muted = muted ?: !prev.live.muted
                            )
                        )
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
                        "isTartil" -> {
                            val nextEnabled = !(cfg.isTartil && cfg.tartilDuration > 0)
                            cfg.copy(
                                isTartil = nextEnabled,
                                tartilDuration = if (nextEnabled) cfg.tartilDuration.coerceAtLeast(10) else cfg.tartilDuration
                            )
                        }
                        "isTarhim" -> {
                            val nextEnabled = !(cfg.isTarhim && cfg.tarhimDuration > 0)
                            cfg.copy(
                                isTarhim = nextEnabled,
                                tarhimDuration = if (nextEnabled) cfg.tarhimDuration.coerceAtLeast(5) else cfg.tarhimDuration
                            )
                        }
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

                "update_prayer_iqomah" -> {
                    val prayer = payload.stringArg("prayer").orEmpty().lowercase()
                    val iqomah = payload.intArg("iqomahDuration")?.coerceIn(1, 60)
                    val sholat = payload.intArg("sholatDuration")?.coerceIn(5, 60)
                    fun applyDurations(cfg: PrayerStageConfig): PrayerStageConfig = cfg.copy(
                        iqomahDuration = iqomah ?: cfg.iqomahDuration,
                        sholatDuration = sholat ?: cfg.sholatDuration
                    )
                    updateSettings { prev ->
                        val p = prev.prayers
                        val updatedPrayers = when (prayer) {
                            "fajr" -> p.copy(fajr = applyDurations(p.fajr))
                            "dhuhr" -> p.copy(dhuhr = applyDurations(p.dhuhr))
                            "asr" -> p.copy(asr = applyDurations(p.asr))
                            "maghrib" -> p.copy(maghrib = applyDurations(p.maghrib))
                            "isha" -> p.copy(isha = applyDurations(p.isha))
                            else -> p
                        }
                        prev.copy(prayers = updatedPrayers)
                    }
                }

                "update_prayer_config" -> {
                    val lat = payload.doubleArg("latitude")?.coerceIn(-90.0, 90.0)
                    val lng = payload.doubleArg("longitude")?.coerceIn(-180.0, 180.0)
                    val ihtiyati = payload.intArg("ihtiyati")?.coerceIn(0, 10)
                    val hijriAdj = payload.intArg("hijriAdjustment")?.coerceIn(-3, 3)
                    val method = payload.stringArg("calculationMethod")?.takeIf { it.isNotBlank() }
                    val madhab = payload.stringArg("madhab")?.takeIf { it.isNotBlank() }
                    val tz = payload.stringArg("timezone")?.takeIf { it.isNotBlank() }
                    updateSettings { prev ->
                        val ps = prev.prayerSettings
                        prev.copy(
                            prayerSettings = ps.copy(
                                latitude = lat ?: ps.latitude,
                                longitude = lng ?: ps.longitude,
                                ihtiyati = ihtiyati ?: ps.ihtiyati,
                                hijriAdjustment = hijriAdj ?: ps.hijriAdjustment,
                                calculationMethod = method ?: ps.calculationMethod,
                                madhab = madhab ?: ps.madhab,
                                timezone = tz ?: ps.timezone
                            )
                        )
                    }
                }

                "update_identity" -> {
                    val newName = payload.stringArg("name")
                    val newAddr = payload.stringArg("address")
                    val newPhone = payload.stringArg("phone")
                    updateSettings { prev ->
                        prev.copy(
                            brand = prev.brand.copy(
                                name = newName?.takeIf { it.isNotBlank() } ?: prev.brand.name,
                                address = newAddr?.takeIf { it.isNotBlank() } ?: prev.brand.address,
                                phone = newPhone ?: prev.brand.phone
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

                "remove_overlay_text" -> {
                    val idx = payload.intArg("index")
                    if (idx != null) {
                        updateSettings { prev ->
                            prev.copy(overlayTextList = prev.overlayTextList.filterIndexed { i, _ -> i != idx })
                        }
                    }
                }

                "clear_overlay_text" -> {
                    updateSettings { prev -> prev.copy(overlayTextList = emptyList()) }
                }

                "add_info" -> {
                    val title = payload.stringArg("title").orEmpty().ifBlank { "INFO MASJID" }
                    val subtitle = payload.stringArg("subtitle").orEmpty()
                    val content = payload.stringArg("content").orEmpty()
                    if (content.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                mosqueInfoList = prev.mosqueInfoList + InfoCardItem(
                                    id = "remote-${UUID.randomUUID()}",
                                    title = title,
                                    content = content,
                                    subtitle = subtitle,
                                    enabled = true
                                )
                            )
                        }
                    }
                }

                "toggle_info" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                mosqueInfoList = prev.mosqueInfoList.map {
                                    if (it.id == id) it.copy(enabled = !it.enabled) else it
                                }
                            )
                        }
                    }
                }

                "remove_info" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(mosqueInfoList = prev.mosqueInfoList.filterNot { it.id == id })
                        }
                    }
                }

                "clear_info" -> {
                    updateSettings { prev -> prev.copy(mosqueInfoList = emptyList()) }
                }

                "toggle_sunnah_fasting" -> {
                    updateSettings { prev ->
                        prev.copy(
                            infoSettings = prev.infoSettings.copy(
                                enableSunnahFasting = !prev.infoSettings.enableSunnahFasting
                            )
                        )
                    }
                }

                "update_jumat_petugas" -> {
                    val khotib = payload.stringArg("khotib").orEmpty()
                    val imam = payload.stringArg("imam").orEmpty()
                    val muadzin = payload.stringArg("muadzin").orEmpty()
                    updateSettings { prev ->
                        val dateKey = prev.jumat.petugas.selectedDate.ifBlank {
                            JwsStateMachine.snapToNearestFriday(_snapshotFlow.value.schedule.date).toString()
                        }
                        val existing = prev.jumat.petugas.records[dateKey] ?: JumatPetugasRecord()
                        val updatedRec = existing.copy(
                            khotib = khotib.ifBlank { existing.khotib },
                            imam = imam.ifBlank { existing.imam },
                            muadzin = muadzin.ifBlank { existing.muadzin }
                        )
                        val newMap = prev.jumat.petugas.records.toMutableMap().apply { put(dateKey, updatedRec) }
                        prev.copy(
                            jumat = prev.jumat.copy(
                                petugas = prev.jumat.petugas.copy(selectedDate = dateKey, records = newMap)
                            )
                        )
                    }
                }

                "update_ramadhan_night" -> {
                    val night = payload.intArg("currentNight")?.coerceIn(1, 30)
                    if (night != null) {
                        updateSettings { prev ->
                            prev.copy(ramadhan = prev.ramadhan.copy(currentNight = night))
                        }
                    }
                }

                "update_masjid_qr" -> {
                    val enabled = payload.booleanArg("enabled")
                    val showFloating = payload.booleanArg("showFloatingOnMain")
                    val link = payload.rawStringArg("link")
                    val label = payload.stringArg("label")
                    val bankName = payload.stringArg("bankName")
                    val nomorRekening = payload.stringArg("nomorRekening")
                    val atasNama = payload.stringArg("atasNama")
                    updateSettings { prev ->
                        val q = prev.masjidQr
                        prev.copy(
                            masjidQr = q.copy(
                                enabled = enabled ?: q.enabled,
                                showFloatingOnMain = showFloating ?: q.showFloatingOnMain,
                                link = link ?: q.link,
                                label = label ?: q.label,
                                bankName = bankName ?: q.bankName,
                                nomorRekening = nomorRekening ?: q.nomorRekening,
                                atasNama = atasNama ?: q.atasNama
                            )
                        )
                    }
                }

                "update_online_finance" -> {
                    val enabled = payload.booleanArg("enabled")
                    val url = payload.rawStringArg("url")
                    val reportUrl = payload.rawStringArg("reportUrl")
                    val bankName = payload.rawStringArg("bankName")
                    val nomorRekening = payload.rawStringArg("nomorRekening")
                    val atasNama = payload.rawStringArg("atasNama")
                    val refreshInterval = payload.intArg("refreshIntervalMinutes")
                    updateSettings { prev ->
                        val f = prev.onlineFinance
                        prev.copy(
                            onlineFinance = f.copy(
                                enabled = enabled ?: f.enabled,
                                sheetUrl = url ?: f.sheetUrl,
                                reportUrl = reportUrl ?: f.reportUrl,
                                bankName = bankName ?: f.bankName,
                                nomorRekening = nomorRekening ?: f.nomorRekening,
                                atasNama = atasNama ?: f.atasNama,
                                refreshIntervalMinutes = refreshInterval?.coerceIn(1, 60) ?: f.refreshIntervalMinutes
                            )
                        )
                    }
                }

                "toggle_slide" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slides = prev.slides.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
                            )
                        }
                    }
                }

                "delete_slide" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(slides = prev.slides.filterNot { it.id == id })
                        }
                        refreshStorageHealth()
                    }
                }

                "reset_default_slides" -> {
                    updateSettings { prev ->
                        prev.copy(slides = SlideItem.defaultSystemSlides())
                    }
                    refreshStorageHealth()
                }

                "add_text_slide" -> {
                    val title = payload.stringArg("title").orEmpty().ifBlank { "Pengumuman Masjid" }
                    val content = payload.stringArg("content").orEmpty()
                    val duration = payload.intArg("duration")?.coerceIn(4, 120) ?: 12
                    if (content.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slides = prev.slides + SlideItem(
                                    id = "slide_${System.currentTimeMillis()}",
                                    title = title,
                                    content = content,
                                    type = "text",
                                    duration = duration,
                                    enabled = true,
                                    showInMain = payload.booleanArg("showInMain") ?: true,
                                    showInSlideshow = payload.booleanArg("showInSlideshow") ?: true,
                                    showInJumat = payload.booleanArg("showInJumat") ?: false,
                                    showInRamadhan = payload.booleanArg("showInRamadhan") ?: false,
                                    showInIdulFitri = payload.booleanArg("showInIdulFitri") ?: false,
                                    showInIdulAdha = payload.booleanArg("showInIdulAdha") ?: false
                                )
                            )
                        }
                    }
                }

                "add_system_slide" -> {
                    val type = payload.stringArg("type").orEmpty().trim()
                    val title = payload.stringArg("title").orEmpty()
                    val duration = payload.intArg("duration")?.coerceIn(4, 120) ?: 15
                    if (type.isNotBlank()) {
                        val isUniversalDefault = type in setOf("welcome", "analog_clock", "qr_infaq", "qr_finance", "online_finance")
                        val newSlide = SlideItem(
                            id = "slide_${type}_${System.currentTimeMillis()}",
                            type = type,
                            title = title.ifBlank { type.replace('_', ' ').uppercase() },
                            duration = duration,
                            enabled = true,
                            showInMain = payload.booleanArg("showInMain") ?: true,
                            showInSlideshow = payload.booleanArg("showInSlideshow") ?: true,
                            showInJumat = payload.booleanArg("showInJumat") ?: (isUniversalDefault || type in setOf("jumat_petugas", "jumat_keuangan", "qr_jumat")),
                            showInRamadhan = payload.booleanArg("showInRamadhan") ?: (isUniversalDefault || type.startsWith("ramadhan_")),
                            showInIdulFitri = payload.booleanArg("showInIdulFitri") ?: (isUniversalDefault || type in setOf("idulfitri_petugas", "ramadhan_laporan_akhir")),
                            showInIdulAdha = payload.booleanArg("showInIdulAdha") ?: (isUniversalDefault || type in setOf("iduladha_petugas", "iduladha_qurban"))
                        )
                        updateSettings { prev ->
                            prev.copy(slides = prev.slides + newSlide)
                        }
                    }
                }

                "toggle_slide_mode" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val mode = payload.stringArg("mode").orEmpty().lowercase()
                    if (id.isNotBlank() && mode.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slides = prev.slides.map { s ->
                                    if (s.id != id) s
                                    else when (mode) {
                                        "main" -> s.copy(showInMain = !s.showInMain)
                                        "slideshow" -> s.copy(showInSlideshow = !s.showInSlideshow)
                                        "jumat" -> s.copy(showInJumat = !s.showInJumat)
                                        "ramadhan" -> s.copy(showInRamadhan = !s.showInRamadhan)
                                        "idul_fitri" -> s.copy(showInIdulFitri = !s.showInIdulFitri)
                                        "idul_adha" -> s.copy(showInIdulAdha = !s.showInIdulAdha)
                                        else -> s
                                    }
                                }
                            )
                        }
                    }
                }

                "update_tartil_tarhim_media" -> {
                    val target = payload.stringArg("target")?.lowercase() ?: "tartil"
                    val mediaType = payload.stringArg("mediaType")?.lowercase()
                    val interval = payload.intArg("interval")?.coerceIn(3, 60)
                    val deleteImageIndex = payload.intArg("deleteImageIndex")
                    val clearVideo = payload.booleanArg("clearVideo") == true
                    updateSettings { prev ->
                        val m = prev.media
                        val updatedMedia = if (target == "tarhim") {
                            m.copy(
                                tarhimMediaType = mediaType?.takeIf { it in setOf("image", "video") } ?: m.tarhimMediaType,
                                tarhimSlideInterval = interval ?: m.tarhimSlideInterval,
                                tarhimImages = if (deleteImageIndex != null) {
                                    m.tarhimImages.filterIndexed { i, _ -> i != deleteImageIndex }
                                } else m.tarhimImages,
                                tarhimVideo = if (clearVideo) "" else m.tarhimVideo
                            )
                        } else {
                            m.copy(
                                tartilMediaType = mediaType?.takeIf { it in setOf("image", "video") } ?: m.tartilMediaType,
                                tartilSlideInterval = interval ?: m.tartilSlideInterval,
                                tartilImages = if (deleteImageIndex != null) {
                                    m.tartilImages.filterIndexed { i, _ -> i != deleteImageIndex }
                                } else m.tartilImages,
                                tartilVideo = if (clearVideo) "" else m.tartilVideo
                            )
                        }
                        prev.copy(media = updatedMedia)
                    }
                    refreshStorageHealth()
                }

                "delete_audio" -> {
                    val cat = payload.stringArg("category").orEmpty().lowercase()
                    val slot = payload.stringArg("slot").orEmpty()
                    if (cat == "custom" && slot.isNotBlank()) {
                        deleteCustomAudioFile(slot)
                    } else if (cat in setOf("tartil", "tarhim", "adzan") && slot.isNotBlank()) {
                        deletePrayerAudioSlot(cat, slot.lowercase())
                    }
                }

                "cleanup_orphan_media" -> {
                    cleanOrphanMedia()
                }

                "update_standby_slideshow" -> {
                    val enabled = payload.booleanArg("enabled")
                    val slideDuration = payload.intArg("slideDuration")?.coerceIn(5, 120)
                    updateSettings { prev ->
                        prev.copy(
                            standbySlideshow = prev.standbySlideshow.copy(
                                enabled = enabled ?: prev.standbySlideshow.enabled,
                                durationMain = slideDuration ?: prev.standbySlideshow.durationMain
                            )
                        )
                    }
                }

                "update_jumat_keuangan" -> {
                    val saldoKasLalu = payload.stringArg("saldoKasLalu")?.toLongOrNull() ?: payload.intArg("saldoKasLalu")?.toLong()
                    val pemasukan = payload.stringArg("pemasukan")?.toLongOrNull() ?: payload.intArg("pemasukan")?.toLong()
                    val pengeluaran = payload.stringArg("pengeluaran")?.toLongOrNull() ?: payload.intArg("pengeluaran")?.toLong()
                    updateSettings { prev ->
                        val today = java.time.LocalDate.now()
                        val dateKey = prev.jumat.keuangan.selectedDate.ifBlank {
                            JwsStateMachine.findEffectiveJumatKeuanganDate(prev.jumat.keuangan.records, today)
                                ?: JwsStateMachine.snapToNearestFriday(today).toString()
                        }
                        val existing = prev.jumat.keuangan.records[dateKey] ?: id.algaffar.jwstv.domain.model.JumatKeuanganRecord(
                            saldoKasLalu = prev.jumat.keuangan.saldoKasLalu
                        )
                        val updatedRec = existing.copy(
                            saldoKasLalu = saldoKasLalu ?: existing.saldoKasLalu,
                            pemasukan = pemasukan ?: existing.pemasukan,
                            pengeluaran = pengeluaran ?: existing.pengeluaran
                        )
                        val newMap = prev.jumat.keuangan.records.toMutableMap().apply { put(dateKey, updatedRec) }
                        prev.copy(
                            jumat = prev.jumat.copy(
                                keuangan = prev.jumat.keuangan.copy(
                                    selectedDate = dateKey,
                                    saldoKasLalu = updatedRec.saldoKasLalu,
                                    records = newMap
                                )
                            )
                        )
                    }
                }

                "update_ramadhan_full" -> {
                    val isRamadhan = payload.booleanArg("isRamadhan")
                    val currentNight = payload.intArg("currentNight")?.coerceIn(1, 30)
                    val hijriYear = payload.stringArg("hijriYear")
                    val penceramah = payload.rawStringArg("penceramah")
                    val imamTarawih = payload.rawStringArg("imamTarawih")
                    val imamWitir = payload.rawStringArg("imamWitir")
                    val saldoAwal = payload.stringArg("saldoAwal")?.toLongOrNull() ?: payload.intArg("saldoAwal")?.toLong()
                    val penerimaanPria = payload.stringArg("penerimaanPria")?.toLongOrNull() ?: payload.intArg("penerimaanPria")?.toLong()
                    val penerimaanWanita = payload.stringArg("penerimaanWanita")?.toLongOrNull() ?: payload.intArg("penerimaanWanita")?.toLong()
                    val pengeluaran = payload.stringArg("pengeluaran")?.toLongOrNull() ?: payload.intArg("pengeluaran")?.toLong()
                    val addTakjilDonor = payload.stringArg("addTakjilDonor")?.trim()
                    val clearTakjilNight = payload.booleanArg("clearTakjilNight") == true
                    val sirineEnabled = payload.booleanArg("sirineEnabled")
                    val sirineMaghrib = payload.booleanArg("sirineMaghrib")
                    val sirineImsak = payload.booleanArg("sirineImsak")
                    val sirineSahur = payload.booleanArg("sirineSahur")
                    val sirineSahurTime = payload.stringArg("sirineSahurTime")
                    val sirineRepeat = payload.intArg("sirineRepeat")?.coerceIn(1, 5)

                    updateSettings { prev ->
                        val ram = prev.ramadhan
                        val targetNight = currentNight ?: ram.currentNight
                        val idx = (targetNight - 1).coerceIn(0, 29)

                        val petugasList = ram.petugas.toMutableList()
                        val curPet = petugasList.getOrNull(idx) ?: id.algaffar.jwstv.domain.model.RamadhanPetugasItem(malamKe = targetNight)
                        if (penceramah != null || imamTarawih != null || imamWitir != null) {
                            while (petugasList.size <= idx) petugasList.add(id.algaffar.jwstv.domain.model.RamadhanPetugasItem(malamKe = petugasList.size + 1))
                            petugasList[idx] = curPet.copy(
                                penceramah = penceramah ?: curPet.penceramah,
                                imamTarawih = imamTarawih ?: curPet.imamTarawih,
                                imamWitir = imamWitir ?: curPet.imamWitir
                            )
                        }

                        val keuList = ram.keuangan.toMutableList()
                        val curKeu = keuList.getOrNull(idx) ?: id.algaffar.jwstv.domain.model.RamadhanKeuanganItem(malamKe = targetNight)
                        if (saldoAwal != null || penerimaanPria != null || penerimaanWanita != null || pengeluaran != null) {
                            while (keuList.size <= idx) keuList.add(id.algaffar.jwstv.domain.model.RamadhanKeuanganItem(malamKe = keuList.size + 1))
                            keuList[idx] = curKeu.copy(
                                saldoAwal = saldoAwal ?: curKeu.saldoAwal,
                                penerimaanPria = penerimaanPria ?: curKeu.penerimaanPria,
                                penerimaanWanita = penerimaanWanita ?: curKeu.penerimaanWanita,
                                pengeluaran = pengeluaran ?: curKeu.pengeluaran
                            )
                        }

                        val takjilMap = ram.takjil.toMutableMap()
                        val nightKey = targetNight.toString()
                        if (clearTakjilNight) {
                            takjilMap[nightKey] = emptyList()
                        } else if (!addTakjilDonor.isNullOrBlank()) {
                            takjilMap[nightKey] = (takjilMap[nightKey].orEmpty() + addTakjilDonor).distinct()
                        }

                        prev.copy(
                            ramadhan = ram.copy(
                                isRamadhan = isRamadhan ?: ram.isRamadhan,
                                currentNight = targetNight,
                                hijriYear = hijriYear ?: ram.hijriYear,
                                petugas = petugasList,
                                keuangan = keuList,
                                takjil = takjilMap,
                                sirine = ram.sirine.copy(
                                    enabled = sirineEnabled ?: ram.sirine.enabled,
                                    playAtMaghrib = sirineMaghrib ?: ram.sirine.playAtMaghrib,
                                    playAtImsak = sirineImsak ?: ram.sirine.playAtImsak,
                                    playAtSahur = sirineSahur ?: ram.sirine.playAtSahur,
                                    sahurTime = sirineSahurTime ?: ram.sirine.sahurTime,
                                    repeatCount = sirineRepeat ?: ram.sirine.repeatCount
                                )
                            )
                        )
                    }
                }

                "update_idul_fitri" -> {
                    val enabled = payload.booleanArg("enabled")
                    val khotib = payload.rawStringArg("khotib")
                    val imam = payload.rawStringArg("imam")
                    val bilal = payload.rawStringArg("bilal")
                    val sholatDuration = payload.intArg("sholatDuration")?.coerceIn(10, 90)
                    val khutbahDuration = payload.intArg("khutbahDuration")?.coerceIn(10, 60)
                    val sholatMessage = payload.rawStringArg("sholatMessage")
                    val khutbahMessage = payload.rawStringArg("khutbahMessage")
                    updateSettings { prev ->
                        val f = prev.idulFitri
                        prev.copy(
                            idulFitri = f.copy(
                                enabled = enabled ?: f.enabled,
                                sholatDuration = sholatDuration ?: f.sholatDuration,
                                khutbahDuration = khutbahDuration ?: f.khutbahDuration,
                                sholatMessage = sholatMessage ?: f.sholatMessage,
                                khutbahMessage = khutbahMessage ?: f.khutbahMessage,
                                petugas = f.petugas.copy(
                                    khotib = khotib ?: f.petugas.khotib,
                                    imam = imam ?: f.petugas.imam,
                                    bilal = bilal ?: f.petugas.bilal
                                )
                            )
                        )
                    }
                }

                "update_idul_adha" -> {
                    val enabled = payload.booleanArg("enabled")
                    val infoQurbanEnabled = payload.booleanArg("infoQurbanEnabled")
                    val khotib = payload.rawStringArg("khotib")
                    val imam = payload.rawStringArg("imam")
                    val bilal = payload.rawStringArg("bilal")
                    val addGroupName = payload.stringArg("addGroupName")?.trim()
                    val addAnimalType = payload.stringArg("addAnimalType")?.trim()?.ifBlank { "Sapi" }
                    val addMembersCsv = payload.stringArg("addMembersCsv").orEmpty()
                    val deleteGroupId = payload.stringArg("deleteGroupId")

                    updateSettings { prev ->
                        val a = prev.idulAdha
                        var groups = a.qurbanGroups
                        if (!deleteGroupId.isNullOrBlank()) {
                            groups = groups.filterNot { it.id == deleteGroupId }
                        }
                        if (!addGroupName.isNullOrBlank()) {
                            val members = addMembersCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            if (members.isNotEmpty()) {
                                groups = groups + id.algaffar.jwstv.domain.model.QurbanGroup(
                                    id = System.currentTimeMillis().toString(),
                                    animalType = addAnimalType ?: "Sapi",
                                    groupName = addGroupName,
                                    members = members
                                )
                            }
                        }
                        prev.copy(
                            idulAdha = a.copy(
                                enabled = enabled ?: a.enabled,
                                petugas = a.petugas.copy(
                                    khotib = khotib ?: a.petugas.khotib,
                                    imam = imam ?: a.petugas.imam,
                                    bilal = bilal ?: a.petugas.bilal
                                ),
                                qurbanGroups = groups
                            ),
                            infoQurban = prev.infoQurban.copy(
                                enabled = infoQurbanEnabled ?: prev.infoQurban.enabled
                            )
                        )
                    }
                }

                "update_screensaver" -> {
                    val enabled = payload.booleanArg("enabled")
                    val startTime = payload.stringArg("startTime")
                    val endTime = payload.stringArg("endTime")
                    updateSettings { prev ->
                        val curList = prev.screensaver.schedules.toMutableList()
                        val first = curList.firstOrNull() ?: id.algaffar.jwstv.domain.model.ScreenSaverSchedule()
                        val updatedFirst = first.copy(
                            startTime = startTime ?: first.startTime,
                            endTime = endTime ?: first.endTime
                        )
                        if (curList.isEmpty()) curList.add(updatedFirst) else curList[0] = updatedFirst
                        prev.copy(
                            screensaver = prev.screensaver.copy(
                                enabled = enabled ?: prev.screensaver.enabled,
                                schedules = curList
                            )
                        )
                    }
                }

                "simulate_state" -> {
                    val stateName = payload.stringArg("state").orEmpty()
                    val durationSec = payload.intArg("durationSeconds")?.toLong()?.coerceIn(10L, 3600L) ?: 300L
                    val target = AppState.fromSerialName(stateName)
                    if (target == AppState.STANDBY) {
                        clearSimulation()
                    } else {
                        simulateState(target, durationSec)
                    }
                }

                "toggle_switch_master" -> {
                    val enabled = payload.booleanArg("enabled")
                    val connectionType = payload.stringArg("connectionType")?.lowercase()
                    val wifiIp = payload.stringArg("wifiIp")?.trim()
                    val usbPort = payload.stringArg("usbPort")?.trim()
                    val baudRate = payload.intArg("baudRate")?.coerceIn(9600, 115200)
                    updateSettings { prev ->
                        prev.copy(
                            switchSettings = prev.switchSettings.copy(
                                enabled = enabled ?: prev.switchSettings.enabled,
                                connectionType = connectionType?.takeIf { it in setOf("wifi", "usb") } ?: prev.switchSettings.connectionType,
                                wifiIp = wifiIp ?: prev.switchSettings.wifiIp,
                                usbPort = usbPort ?: prev.switchSettings.usbPort,
                                baudRate = baudRate ?: prev.switchSettings.baudRate
                            )
                        )
                    }
                }

                "toggle_switch_channel" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                switchSettings = prev.switchSettings.copy(
                                    switches = prev.switchSettings.switches.map { ch ->
                                        if (ch.id == id) ch.copy(enabled = !ch.enabled) else ch
                                    }
                                )
                            )
                        }
                    }
                }

                "update_switch_channel" -> {
                    val channelId = payload.stringArg("id").orEmpty()
                    if (channelId.isNotBlank()) {
                        val name = payload.stringArg("name")?.trim()?.uppercase()
                        val enabled = payload.booleanArg("enabled")
                        val triggerMode = payload.stringArg("triggerMode")?.lowercase()?.takeIf { it in setOf("prayer", "state") }
                        val onState = payload.stringArg("onState")?.lowercase()
                        val onStateOffset = payload.intArg("onStateOffset")?.coerceIn(-30, 60)
                        val offState = payload.stringArg("offState")?.lowercase()
                        val offStateOffset = payload.intArg("offStateOffset")?.coerceIn(-30, 60)
                        val schedObj = payload.rawJson["schedules"] as? kotlinx.serialization.json.JsonObject

                        updateSettings { prev ->
                            prev.copy(
                                switchSettings = prev.switchSettings.copy(
                                    switches = prev.switchSettings.switches.map { ch ->
                                        if (ch.id != channelId) ch
                                        else {
                                            val updatedSchedules = if (schedObj != null) {
                                                val map = ch.schedules.toMutableMap()
                                                listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { pid ->
                                                    val pObj = schedObj[pid] as? kotlinx.serialization.json.JsonObject
                                                    if (pObj != null) {
                                                        val onVal = (pObj["onOffset"] as? kotlinx.serialization.json.JsonPrimitive)
                                                            ?.takeIf { !it.isString || it.content.isNotBlank() }
                                                            ?.content?.toIntOrNull()
                                                        val offVal = (pObj["offOffset"] as? kotlinx.serialization.json.JsonPrimitive)
                                                            ?.takeIf { !it.isString || it.content.isNotBlank() }
                                                            ?.content?.toIntOrNull()
                                                        map[pid] = id.algaffar.jwstv.domain.model.PrayerSwitchSchedule(
                                                            onOffset = onVal,
                                                            offOffset = offVal
                                                        )
                                                    }
                                                }
                                                map
                                            } else {
                                                ch.schedules
                                            }
                                            ch.copy(
                                                name = name?.ifBlank { ch.name } ?: ch.name,
                                                enabled = enabled ?: ch.enabled,
                                                triggerMode = triggerMode ?: ch.triggerMode,
                                                schedules = updatedSchedules,
                                                onState = onState ?: ch.onState,
                                                onStateOffset = onStateOffset ?: ch.onStateOffset,
                                                offState = offState ?: ch.offState,
                                                offStateOffset = offStateOffset ?: ch.offStateOffset
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }

                "test_switch_pin" -> {
                    val pin = payload.intArg("pin") ?: 1
                    val turnOn = payload.booleanArg("turnOn") ?: true
                    toggleManualRelay(pin, turnOn)
                }

                "update_admin_pin" -> {
                    val roleId = payload.stringArg("role")?.lowercase() ?: "user"
                    val newPin = payload.stringArg("newPin")?.trim().orEmpty()
                    if (newPin.length >= 4) {
                        val newHash = id.algaffar.jwstv.data.auth.AuthManager.sha256Hex(newPin)
                        updateSettings { prev ->
                            val existing = prev.users.toMutableList()
                            val idx = existing.indexOfFirst { it.role.equals(roleId, ignoreCase = true) }
                            if (idx >= 0) {
                                existing[idx] = existing[idx].copy(pinHash = newHash)
                            } else {
                                existing.add(
                                    id.algaffar.jwstv.domain.model.UserAccount(
                                        id = "$roleId-${System.currentTimeMillis()}",
                                        username = roleId,
                                        pinHash = newHash,
                                        role = roleId
                                    )
                                )
                            }
                            prev.copy(users = existing)
                        }
                        AppLogger.info("AUTH", "PIN akun '$roleId' diperbarui melalui Remote PWA")
                    }
                }

                "clear_logs" -> {
                    AppLogger.clear()
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
        val styles = listOf("classic", "golden", "glass", "wood", "neon", "segmented", "emerald", "monochrome")
        updateSettings { prev ->
            val currentIdx = styles.indexOf(prev.brand.clockStyle.lowercase())
            val nextStyle = styles[(currentIdx + 1) % styles.size]
            prev.copy(
                brand = prev.brand.copy(clockStyle = nextStyle)
            )
        }
    }

    fun startDirectLive(source: String, customId: String, durationMinutes: Int) {
        updateSettings { prev ->
            prev.copy(
                live = prev.live.copy(
                    enabled = true,
                    source = source,
                    customVideoId = customId,
                    directLiveEnabled = true,
                    directLiveDurationMinutes = durationMinutes,
                    directLiveStartEpochMs = System.currentTimeMillis()
                )
            )
        }
        AppLogger.info("LIVE", "Direct Live dimulai: sumber=$source, durasi=${durationMinutes}m")
        _statusBannerMessage.value = "Siaran Langsung ($source) dimulai!"
    }

    fun stopDirectLive() {
        updateSettings { prev ->
            prev.copy(
                live = prev.live.copy(
                    directLiveEnabled = false,
                    directLiveStartEpochMs = 0L
                )
            )
        }
        clearSimulation()
        AppLogger.info("LIVE", "Direct Live dihentikan")
        _statusBannerMessage.value = "Siaran Langsung dihentikan."
    }

    fun testOnlineFinanceConnection() {
        viewModelScope.launch {
            _statusBannerMessage.value = "Menguji koneksi Google Sheets Keuangan Online..."
            val s = settingsRepository.currentSettings
            val result = onlineFinanceManager.fetchFinancialData(
                onlineFinance = s.onlineFinance.copy(enabled = true),
                localJumatKeuangan = s.jumat.keuangan
            )
            _onlineFinanceTestResult.value = result
            if (result.isLocalFallback) {
                AppLogger.warn("FINANCE", "Koneksi Google Sheets gagal, fallback ke kas lokal")
                _statusBannerMessage.value = "Gagal terhubung ke Google Sheets (menggunakan fallback kas lokal)."
            } else {
                AppLogger.info("FINANCE", "Google Sheets terhubung: ${result.recentTransactions.size} transaksi terbaca")
                _statusBannerMessage.value = "Berhasil terhubung ke Google Sheets (${result.recentTransactions.size} transaksi)!"
            }
        }
    }

    // ==================== AUDIO & MEDIA ASSET MANAGEMENT ====================

    fun findPrayerAudioSlot(category: String, slotName: String): Pair<Boolean, String> {
        val f = audioEngine.findSlotAudioFile(category, slotName)
        return if (f != null) true to audioEngine.readDurationFormatted(f) else false to ""
    }

    fun listCustomAudioFilesWithDurations(): List<Pair<String, String>> {
        return audioEngine.listCustomAudioFiles().map { file ->
            file.name to audioEngine.readDurationFormatted(file)
        }
    }

    fun togglePreviewPrayerAudio(category: String, slotName: String) {
        val key = "$category:$slotName"
        if (_previewingAudioKey.value == key) {
            audioEngine.stopPlayback()
            _previewingAudioKey.value = null
            return
        }
        val f = audioEngine.findSlotAudioFile(category, slotName)
        if (f != null) {
            val vol = settingsRepository.currentSettings.globalVolume
            if (audioEngine.previewAudioFile(f, vol)) {
                _previewingAudioKey.value = key
                AppLogger.info("AUDIO", "Pratinjau audio: $category/$slotName")
            }
        }
    }

    fun togglePreviewCustomAudio(fileName: String) {
        val key = "custom:$fileName"
        if (_previewingAudioKey.value == key) {
            audioEngine.stopPlayback()
            _previewingAudioKey.value = null
            return
        }
        val f = File(assetsRootDir, "audio/custom/$fileName")
        if (f.exists()) {
            val vol = settingsRepository.currentSettings.globalVolume
            if (audioEngine.previewAudioFile(f, vol)) {
                _previewingAudioKey.value = key
                AppLogger.info("AUDIO", "Pratinjau audio kustom: $fileName")
            }
        }
    }

    fun stopAudioPreview() {
        audioEngine.stopPlayback()
        _previewingAudioKey.value = null
    }

    fun importPrayerAudioSlot(category: String, slotName: String, inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = inputStream.use { input ->
                MediaStorageManager.saveStreamWithGuard(
                    assetsRootDir = assetsRootDir,
                    targetRelativePath = "audio/$category/$slotName.mp3",
                    inputStream = input,
                    maxBytes = MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
                )
            }
            res.onSuccess { saved ->
                refreshAudioDurationsAsync()
                _audioInventoryTick.value += 1
                refreshStorageHealth()
                AppLogger.info("AUDIO", "Audio $category/$slotName berhasil disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)})")
                _statusBannerMessage.value = "Audio $category ($slotName) berhasil disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
            }.onFailure { err ->
                AppLogger.error("AUDIO", "Gagal impor audio $category/$slotName: ${err.message}")
                _statusBannerMessage.value = "Gagal menyimpan audio: ${err.message}"
            }
        }
    }

    fun deletePrayerAudioSlot(category: String, slotName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = audioEngine.findSlotAudioFile(category, slotName)
            if (f != null && f.delete()) {
                refreshAudioDurationsAsync()
                _audioInventoryTick.value += 1
                refreshStorageHealth()
                AppLogger.info("AUDIO", "Audio $category/$slotName dihapus")
                _statusBannerMessage.value = "Audio $category ($slotName) dihapus."
            }
        }
    }

    fun importCustomAudioFile(rawName: String, inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = MediaStorageManager.sanitizeFileName(rawName)
            val finalName = if (cleanName.contains('.')) cleanName else "$cleanName.mp3"
            val res = inputStream.use { input ->
                MediaStorageManager.saveStreamWithGuard(
                    assetsRootDir = assetsRootDir,
                    targetRelativePath = "audio/custom/$finalName",
                    inputStream = input,
                    maxBytes = MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
                )
            }
            res.onSuccess { saved ->
                _audioInventoryTick.value += 1
                refreshStorageHealth()
                AppLogger.info("AUDIO", "Audio kustom diimpor: $finalName (${MediaStorageManager.formatByteSize(saved.savedBytes)})")
                _statusBannerMessage.value = "Audio kustom '$finalName' berhasil ditambahkan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
            }.onFailure { err ->
                AppLogger.error("AUDIO", "Gagal impor audio kustom: ${err.message}")
                _statusBannerMessage.value = "Gagal mengimpor audio kustom: ${err.message}"
            }
        }
    }

    fun deleteCustomAudioFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(assetsRootDir, "audio/custom/$fileName")
            if (f.exists() && f.delete()) {
                _audioInventoryTick.value += 1
                refreshStorageHealth()
                AppLogger.info("AUDIO", "Audio kustom dihapus: $fileName")
                _statusBannerMessage.value = "Audio kustom '$fileName' dihapus."
            }
        }
    }

    fun importImageAsset(subFolder: String, fileName: String, inputStream: InputStream, onSaved: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = inputStream.use { input ->
                MediaStorageManager.optimizeAndSaveImage(
                    assetsRootDir = assetsRootDir,
                    subFolder = subFolder,
                    rawFileName = fileName,
                    inputStream = input
                )
            }
            res.onSuccess { saved ->
                withContext(Dispatchers.Main) {
                    onSaved(saved.relativePath)
                }
                refreshStorageHealth()
                val optInfo = if (saved.wasOptimized) {
                    "dioptimasi ${MediaStorageManager.formatByteSize(saved.originalBytes)} → ${MediaStorageManager.formatByteSize(saved.savedBytes)}"
                } else {
                    MediaStorageManager.formatByteSize(saved.savedBytes)
                }
                AppLogger.info("MEDIA", "Gambar disimpan ($optInfo): ${saved.relativePath}")
                _statusBannerMessage.value = "Gambar berhasil disimpan ($optInfo)"
            }.onFailure { err ->
                AppLogger.error("MEDIA", "Gagal menyimpan gambar: ${err.message}")
                _statusBannerMessage.value = "Gagal menyimpan gambar: ${err.message}"
            }
        }
    }

    fun importVideoAsset(fileName: String, inputStream: InputStream, onSaved: (String, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val clean = MediaStorageManager.sanitizeFileName(fileName)
            val ext = clean.substringAfterLast('.', "mp4").lowercase()
                .takeIf { it in MediaStorageManager.VIDEO_EXTENSIONS } ?: "mp4"
            val base = clean.substringBeforeLast('.').ifBlank { "video" }
            val targetName = "${System.currentTimeMillis()}_${base}.$ext"
            val res = inputStream.use { input ->
                MediaStorageManager.saveStreamWithGuard(
                    assetsRootDir = assetsRootDir,
                    targetRelativePath = "videos/$targetName",
                    inputStream = input,
                    maxBytes = MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
                )
            }
            res.onSuccess { saved ->
                val displayTitle = base.replace('_', ' ').replace('-', ' ').ifBlank { "Video Masjid" }
                withContext(Dispatchers.Main) {
                    onSaved(saved.relativePath, displayTitle)
                }
                refreshStorageHealth()
                AppLogger.info("MEDIA", "Video disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)}): ${saved.relativePath}")
                _statusBannerMessage.value = "Video '$displayTitle' berhasil disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
            }.onFailure { err ->
                AppLogger.error("MEDIA", "Gagal menyimpan video: ${err.message}")
                _statusBannerMessage.value = "Gagal menyimpan video: ${err.message}"
            }
        }
    }

    fun syncMediaFromExternalFolder(sourceDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _statusBannerMessage.value = "Menyinkronkan media dari ${sourceDir.name} ke memori internal..."
            val res = MediaStorageManager.syncFromExternalFolder(
                sourceDir = sourceDir,
                assetsRootDir = assetsRootDir
            )
            res.onSuccess { summary ->
                if (summary.updatedLogoPath != null || summary.createdSlides.isNotEmpty()) {
                    updateSettings { prev ->
                        prev.copy(
                            brand = if (summary.updatedLogoPath != null) {
                                prev.brand.copy(logo = summary.updatedLogoPath)
                            } else {
                                prev.brand
                            },
                            slides = if (summary.createdSlides.isNotEmpty()) {
                                prev.slides + summary.createdSlides
                            } else {
                                prev.slides
                            }
                        )
                    }
                }
                if (summary.importedAudio > 0) {
                    refreshAudioDurationsAsync()
                    _audioInventoryTick.value += 1
                }
                refreshStorageHealth()
                val msg = if (summary.totalImported == 0) {
                    "Tidak ditemukan file gambar/audio/video baru di folder '${sourceDir.name}'."
                } else {
                    "Sync USB selesai! Diimpor ${summary.importedImages} gambar, ${summary.importedAudio} audio, " +
                        "${summary.importedVideos} video (${MediaStorageManager.formatByteSize(summary.totalBytesCopied)}). " +
                        "Flashdisk aman dicabut!"
                }
                AppLogger.info("MEDIA", msg)
                _statusBannerMessage.value = msg
            }.onFailure { err ->
                AppLogger.error("MEDIA", "Gagal sync folder USB: ${err.message}")
                _statusBannerMessage.value = "Gagal sync folder USB: ${err.message}"
            }
        }
    }

    fun cleanOrphanMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = MediaStorageManager.cleanOrphanMedia(
                assetsRootDir = assetsRootDir,
                settings = settingsRepository.currentSettings
            )
            refreshStorageHealth()
            val msg = if (res.deletedCount == 0) {
                "Penyimpanan sudah bersih — tidak ada file media tidak terpakai."
            } else {
                "Berhasil membersihkan ${res.deletedCount} file tidak terpakai (membebaskan ${MediaStorageManager.formatByteSize(res.freedBytes)})!"
            }
            AppLogger.info("MEDIA", msg)
            _statusBannerMessage.value = msg
        }
    }

    fun updateSettings(transform: (AppStateSettings) -> AppStateSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
            _snapshotFlow.value = computeSnapshot(ZonedDateTime.now())
            refreshStorageHealth()
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
            val res = inputStream.use { stream ->
                BackupRestoreManager.restoreFromBackupZip(
                    inputStream = stream,
                    settingsRepository = settingsRepository,
                    assetsRootDir = assetsRootDir
                )
            }
            res.onSuccess { summary ->
                refreshAudioDurationsAsync()
                refreshStorageHealth()
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
