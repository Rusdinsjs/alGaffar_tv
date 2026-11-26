package id.algaffar.jwstv.runtime

import android.content.Context
import android.view.KeyEvent
import id.algaffar.jwstv.BuildConfig
import id.algaffar.jwstv.data.audio.JwsAudioEngine
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.backup.BackupRestoreManager
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.cctv.OnlineFinanceManager
import id.algaffar.jwstv.data.excel.TakjilExcelManager
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.logging.AppLogger
import id.algaffar.jwstv.data.media.AudioPackDownloader
import id.algaffar.jwstv.data.media.ImagePackDownloader
import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.data.server.RemoteCommandPayload
import id.algaffar.jwstv.data.server.RemoteTokenStore
import id.algaffar.jwstv.data.update.OtaCheckResult
import id.algaffar.jwstv.data.update.OtaUpdateManager
import id.algaffar.jwstv.domain.engine.AudioFileDurations
import id.algaffar.jwstv.domain.engine.ClockIntegrityGuard
import id.algaffar.jwstv.domain.engine.FastingInfoSummary
import id.algaffar.jwstv.domain.engine.HijriCalculator
import id.algaffar.jwstv.domain.engine.HijriDateInfo
import id.algaffar.jwstv.domain.engine.IslamicEventItem
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.CustomIslamicEventItem
import id.algaffar.jwstv.domain.model.FloatingOverlayItem
import id.algaffar.jwstv.domain.model.InfoCardItem
import id.algaffar.jwstv.domain.model.JumatPetugasRecord
import id.algaffar.jwstv.domain.model.LayoutPosition
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerStageConfig
import id.algaffar.jwstv.domain.model.SlideItem
import id.algaffar.jwstv.domain.model.SlideshowGroup
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.RemoteUiInfo
import id.algaffar.jwstv.ui.theme.ThemeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-lifetime 24/7 Kiosk Runtime Engine for Al-Gaffar TV.
 * Owned by [id.algaffar.jwstv.AppContainer] and kept alive by [id.algaffar.jwstv.service.JwsForegroundService]
 * so the 1Hz prayer ticker, Ktor Port 1421 Remote Server, Media3 audio engine, and 8-channel Relay controller
 * survive Activity recreations and never shut down on `MainViewModel.onCleared()`.
 */
class JwsRuntimeEngine(
    private val appContext: Context?,
    val assetsRootDir: File,
    val settingsRepository: SettingsRepository,
    val tokenStore: RemoteTokenStore,
    val clockIntegrityGuard: ClockIntegrityGuard,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val hijriCalculator: HijriCalculator,
    private val stateMachine: JwsStateMachine,
    val audioEngine: JwsAudioEngine,
    val switchController: SwitchController,
    val otaUpdateManager: OtaUpdateManager = OtaUpdateManager(appContext),
    val audioPackDownloader: AudioPackDownloader = AudioPackDownloader(),
    val imagePackDownloader: ImagePackDownloader = ImagePackDownloader()
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val started = AtomicBoolean(false)

    @Volatile
    private var cachedAudioDurations = AudioFileDurations()

    private val _storageHealthFlow = MutableStateFlow(
        MediaStorageManager.inspectStorage(assetsRootDir, settingsRepository.currentSettings)
    )
    val storageHealthFlow: StateFlow<StorageHealthSnapshot> = _storageHealthFlow.asStateFlow()

    val deviceCode: String
        get() = tokenStore.getOrCreateDeviceCode()

    val remoteServer: KtorRemoteServer = KtorRemoteServer(
        context = appContext,
        port = KtorRemoteServer.DEFAULT_PORT,
        tokenStore = tokenStore,
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
        clockStatusProvider = {
            val effective = clockIntegrityGuard.effectiveNow()
            KtorRemoteServer.ClockStatusSnapshot(
                isClockSuspect = clockIntegrityGuard.isEffectiveClockSuspect(),
                clockOffsetMs = clockIntegrityGuard.currentOffsetMs,
                deviceTimeIso = effective.toOffsetDateTime().toString()
            )
        },
        onSyncTimeReceived = { epochMs ->
            syncTimeFromTrustedReference(epochMs)
        },
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

    private data class HijriCacheKey(
        val effectiveLocalDate: LocalDate,
        val hijriAdjustment: Int,
        val isAfterMaghrib: Boolean
    )

    private data class UpcomingEventsCacheKey(
        val effectiveLocalDate: LocalDate,
        val hijriAdjustment: Int,
        val maxDays: Int,
        val customEvents: List<CustomIslamicEventItem>
    )

    @Volatile
    private var cachedHijriKey: HijriCacheKey? = null

    @Volatile
    private var cachedHijriDate: HijriDateInfo? = null

    @Volatile
    private var cachedFastingInfo: FastingInfoSummary? = null

    @Volatile
    private var cachedEventsKey: UpcomingEventsCacheKey? = null

    @Volatile
    private var cachedUpcomingEvents: List<IslamicEventItem> = emptyList()

    private val initialSnapshot = computeSnapshot(currentEffectiveNow())
    private val _snapshotFlow = MutableStateFlow(initialSnapshot)
    val snapshotFlow: StateFlow<JwsRuntimeSnapshot> = _snapshotFlow.asStateFlow()

    private val _currentHourFlow = MutableStateFlow(initialSnapshot.now.hour)
    val currentHourFlow: StateFlow<Int> = _currentHourFlow.asStateFlow()

    private val _activeAppStateFlow = MutableStateFlow(initialSnapshot.tickResult.state)
    val activeAppStateFlow: StateFlow<AppState> = _activeAppStateFlow.asStateFlow()

    private val _remoteUiInfoFlow = MutableStateFlow(
        RemoteUiInfo(
            remoteServerUrl = initialSnapshot.remoteServerUrl,
            remoteQrPairingUrl = initialSnapshot.remoteQrPairingUrl,
            remoteAuthToken = initialSnapshot.remoteAuthToken,
            deviceCode = initialSnapshot.deviceCode
        )
    )
    val remoteUiInfoFlow: StateFlow<RemoteUiInfo> = _remoteUiInfoFlow.asStateFlow()

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

    private val _otaCheckResultFlow = MutableStateFlow<OtaCheckResult?>(null)
    val otaCheckResultFlow: StateFlow<OtaCheckResult?> = _otaCheckResultFlow.asStateFlow()

    private var lastLoggedState: AppState? = null
    private var lastRamadhanAutoSyncDayKey: String? = null
    private var lastAutoRestartCheckDayKey: String? = null
    private val engineBootElapsedMs: Long = android.os.SystemClock.elapsedRealtime()

    @Volatile
    var onBeforeLaunchExternalInstaller: (() -> Unit)? = null

    private fun publishSnapshot(snap: JwsRuntimeSnapshot) {
        _snapshotFlow.value = snap
        _currentHourFlow.value = snap.now.hour
        _activeAppStateFlow.value = snap.tickResult.state
        val nextRemote = RemoteUiInfo(
            remoteServerUrl = snap.remoteServerUrl,
            remoteQrPairingUrl = snap.remoteQrPairingUrl,
            remoteAuthToken = snap.remoteAuthToken,
            deviceCode = snap.deviceCode
        )
        if (_remoteUiInfoFlow.value != nextRemote) {
            _remoteUiInfoFlow.value = nextRemote
        }
    }

    fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return

        AppLogger.info("SYSTEM", "Al-Gaffar TV Runtime Engine dimulai (30-State 24/7 Kiosk Engine, Device ID: $deviceCode)")
        refreshAudioDurationsAsync()
        refreshStorageHealth()

        // Start Embedded Ktor Remote Server on Port 1421 (survives Activity lifecycle!)
        engineScope.launch(Dispatchers.IO) {
            KtorRemoteServer.refreshCachedLocalIp()
            remoteServer.start()
            AppLogger.info("REMOTE", "Ktor Remote Server aktif di ${remoteServer.getRemoteUrl(false)}")
            publishSnapshot(computeSnapshot(currentEffectiveNow()))
        }

        // Refresh cached LAN IP off the Main thread every 15s (zero netlink syscalls on 1Hz UI ticker)
        engineScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(15_000L)
                KtorRemoteServer.refreshCachedLocalIp()
                val (url, qrUrl) = remoteServer.getRemoteUrlsNonBlocking()
                val nextRemote = RemoteUiInfo(
                    remoteServerUrl = url,
                    remoteQrPairingUrl = qrUrl,
                    remoteAuthToken = remoteServer.authToken,
                    deviceCode = deviceCode
                )
                if (_remoteUiInfoFlow.value != nextRemote) {
                    _remoteUiInfoFlow.value = nextRemote
                }
            }
        }

        // 1Hz real-time ticker driving StateMachine, Media3 AudioEngine & 8-Channel SwitchController
        engineScope.launch {
            while (isActive) {
                val snap = computeSnapshot(currentEffectiveNow())
                publishSnapshot(snap)

                if (snap.tickResult.state != lastLoggedState) {
                    AppLogger.info(
                        "STATE",
                        "Transisi state -> ${snap.tickResult.state.serialName} (${snap.tickResult.state.displayLabel})"
                    )
                    lastLoggedState = snap.tickResult.state
                }

                // Auto-sync Ramadhan night from Hijri calendar if enabled
                val ram = snap.settings.ramadhan
                if (ram.isRamadhan && ram.autoSyncNightFromHijri && snap.hijriDate.month == 9) {
                    val expectedNight = snap.hijriDate.day.coerceIn(1, 30)
                    val syncKey = "${snap.hijriDate.year}-9-$expectedNight"
                    if (ram.currentNight != expectedNight && lastRamadhanAutoSyncDayKey != syncKey) {
                        lastRamadhanAutoSyncDayKey = syncKey
                        updateSettings { prev ->
                            prev.copy(ramadhan = prev.ramadhan.copy(currentNight = expectedNight))
                        }
                        AppLogger.info("RAMADHAN", "Malam Ramadhan otomatis disinkronkan ke malam ke-$expectedNight")
                    }
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
                evaluateScheduledKioskMaintenance(snap)
                delay(1_000L)
            }
        }

        // Proactive Memory Watchdog (every 30s)
        engineScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(30_000L)
                checkAndRecoverMemoryIfNeeded()
            }
        }
    }

    private fun evaluateScheduledKioskMaintenance(snap: JwsRuntimeSnapshot) {
        val ctx = appContext ?: return
        val cfg = snap.settings.kioskMaintenance
        if (cfg.autoRestartMode == "off") return
        if (snap.tickResult.state != AppState.STANDBY || snap.tickResult.isManualOverride) return
        // Avoid restarting within the first 90s of process start
        if (android.os.SystemClock.elapsedRealtime() - engineBootElapsedMs < 90_000L) return

        val now = snap.now
        if (cfg.autoRestartMode == "weekly" && now.dayOfWeek.value != cfg.autoRestartDayOfWeek.coerceIn(1, 7)) {
            return
        }
        if (now.hour != cfg.autoRestartHour.coerceIn(0, 23) ||
            now.minute != cfg.autoRestartMinute.coerceIn(0, 59)
        ) {
            return
        }

        val todayKey = now.toLocalDate().toString()
        if (lastAutoRestartCheckDayKey == todayKey) return
        val markerFile = File(ctx.filesDir, ".last_auto_restart_date")
        val persistedDay = runCatching {
            if (markerFile.exists()) markerFile.readText(Charsets.UTF_8).trim() else ""
        }.getOrDefault("")
        if (persistedDay == todayKey) {
            lastAutoRestartCheckDayKey = todayKey
            return
        }

        lastAutoRestartCheckDayKey = todayKey
        runCatching { markerFile.writeText(todayKey, Charsets.UTF_8) }

        if (cfg.autoRestartAction == "reboot_device" &&
            id.algaffar.jwstv.service.JwsDeviceAdminReceiver.isDeviceOwner(ctx)
        ) {
            AppLogger.warn("KIOSK", "Menjalankan Reboot OS TV Box terjadwal ($todayKey ${now.hour}:${now.minute})")
            id.algaffar.jwstv.service.JwsDeviceAdminReceiver.rebootDeviceIfOwner(ctx)
        } else {
            AppLogger.warn("KIOSK", "Menjalankan Restart Aplikasi terjadwal ($todayKey ${now.hour}:${now.minute})")
            id.algaffar.jwstv.JwsApplication.restartAppNow(ctx, 1_500L)
        }
    }

    fun triggerAppRestartNow() {
        val ctx = appContext ?: return
        AppLogger.warn("KIOSK", "Restart aplikasi manual dipicu dari Pengaturan")
        id.algaffar.jwstv.JwsApplication.restartAppNow(ctx, 800L)
    }

    fun triggerDeviceRebootNow(): Boolean {
        val ctx = appContext ?: return false
        AppLogger.warn("KIOSK", "Reboot OS TV Box manual dipicu dari Pengaturan")
        val ok = id.algaffar.jwstv.service.JwsDeviceAdminReceiver.rebootDeviceIfOwner(ctx)
        if (!ok) {
            _statusBannerMessage.value = "Reboot OS membutuhkan status Device Owner (dpm set-device-owner)."
        }
        return ok
    }

    private fun currentEffectiveNow(): ZonedDateTime {
        val zoneId = prayerTimeCalculator.resolveZoneId(settingsRepository.currentSettings.prayerSettings.timezone)
        return clockIntegrityGuard.effectiveNow(zoneId)
    }

    fun syncTimeFromTrustedReference(epochMs: Long): Result<String> {
        val res = clockIntegrityGuard.syncFromTrustedReference(epochMs)
        return res.map { offsetMs ->
            val snap = computeSnapshot(currentEffectiveNow())
            publishSnapshot(snap)
            val msg = "Jam TV berhasil disinkronkan dengan HP (offset ${offsetMs / 1000L} detik)."
            AppLogger.info("CLOCK", msg)
            _statusBannerMessage.value = msg
            msg
        }
    }

    fun postStatusMessage(msg: String) {
        _statusBannerMessage.value = msg
    }

    fun refreshStorageHealth() {
        engineScope.launch(Dispatchers.IO) {
            _storageHealthFlow.value = MediaStorageManager.inspectStorage(
                assetsRootDir = assetsRootDir,
                settings = settingsRepository.currentSettings
            )
        }
    }

    fun refreshAudioDurationsAsync() {
        engineScope.launch {
            cachedAudioDurations = withContext(Dispatchers.IO) {
                audioEngine.scanAudioFileDurations()
            }
        }
    }

    fun regenerateRemoteToken() {
        val newToken = remoteServer.regenerateAuthToken()
        publishSnapshot(computeSnapshot(currentEffectiveNow()))
        val msg = "Token Remote baru dibuat (${newToken.take(6)}...). Semua HP lama otomatis terputus."
        AppLogger.warn("SECURITY", msg)
        _statusBannerMessage.value = msg
    }

    fun resetSuperadminPin(newPin: String) {
        val clean = newPin.trim()
        if (clean.length < 4) return
        val newHash = AuthManager.hashPinPbkdf2(clean)
        updateSettings { prev ->
            val list = prev.users.toMutableList()
            val idx = list.indexOfFirst { it.role.equals("superadmin", ignoreCase = true) }
            if (idx >= 0) {
                list[idx] = list[idx].copy(pinHash = newHash)
            } else {
                list.add(
                    UserAccount(
                        id = "superadmin-1",
                        username = AuthManager.DEFAULT_SUPERADMIN_USERNAME,
                        pinHash = newHash,
                        role = "superadmin"
                    )
                )
            }
            prev.copy(users = list)
        }
        AppLogger.warn("SECURITY", "PIN Superadmin direset melalui Pemulihan Offline CS.")
        _statusBannerMessage.value = "PIN Superadmin baru berhasil disimpan (PBKDF2-SHA256)."
    }

    fun upgradeUserPinHash(userId: String, rawPin: String) {
        val newHash = AuthManager.hashPinPbkdf2(rawPin)
        updateSettings { prev ->
            prev.copy(
                users = prev.users.map { acc ->
                    if (acc.id == userId && AuthManager.needsHashUpgrade(acc.pinHash)) {
                        acc.copy(pinHash = newHash)
                    } else {
                        acc
                    }
                }
            )
        }
    }

    fun requestUsbRelayPermission() {
        val res = switchController.requestUsbRelayPermission()
        res.onSuccess { msg ->
            AppLogger.info("USB", msg)
            _statusBannerMessage.value = msg
        }.onFailure { err ->
            AppLogger.warn("USB", err.message ?: "Gagal mendeteksi USB Relay")
            _statusBannerMessage.value = err.message ?: "Gagal mendeteksi USB Relay"
        }
    }

    fun checkOtaUpdate() {
        engineScope.launch {
            _statusBannerMessage.value = "Memeriksa pembaruan Al-Gaffar TV..."
            val res = otaUpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
            res.onSuccess { check ->
                _otaCheckResultFlow.value = check
                _statusBannerMessage.value = check.message
                val manifest = check.manifest
                if (check.hasUpdate && manifest != null) {
                    val targetDir = File(appContext?.cacheDir ?: assetsRootDir.parentFile, "ota_updates")
                    _statusBannerMessage.value = "Mengunduh Al-Gaffar TV v${manifest.versionName}..."
                    val dlRes = otaUpdateManager.downloadAndVerifyApk(manifest, targetDir) { pct ->
                        if (pct % 20 == 0) {
                            _statusBannerMessage.value = "Mengunduh pembaruan v${manifest.versionName}: $pct%"
                        }
                    }
                    dlRes.onSuccess { apkFile ->
                        _statusBannerMessage.value = "APK terverifikasi SHA-256! Membuka installer pembaruan..."
                        otaUpdateManager.launchApkInstaller(apkFile) { onBeforeLaunchExternalInstaller?.invoke() }.onFailure { err ->
                            _statusBannerMessage.value = "Gagal membuka installer APK: ${err.message}"
                        }
                    }.onFailure { err ->
                        AppLogger.error("OTA", "Gagal mengunduh OTA: ${err.message}")
                        _statusBannerMessage.value = "Gagal mengunduh OTA: ${err.message}"
                    }
                }
            }.onFailure { err ->
                AppLogger.warn("OTA", "Cek pembaruan gagal: ${err.message}")
                _statusBannerMessage.value = "Gagal memeriksa pembaruan: ${err.message}"
            }
        }
    }

    fun installOfflineApk(inputStream: InputStream) {
        engineScope.launch(Dispatchers.IO) {
            val targetDir = File(appContext?.cacheDir ?: assetsRootDir.parentFile, "ota_updates")
            val res = inputStream.use { stream -> otaUpdateManager.stageOfflineApk(stream, targetDir) }
            res.onSuccess { apkFile ->
                withContext(Dispatchers.Main) {
                    otaUpdateManager.launchApkInstaller(apkFile) { onBeforeLaunchExternalInstaller?.invoke() }.onSuccess {
                        _statusBannerMessage.value = "Installer pembaruan APK dibuka. Ikuti konfirmasi di layar TV."
                    }.onFailure { err ->
                        _statusBannerMessage.value = "Gagal membuka installer APK: ${err.message}"
                    }
                }
            }.onFailure { err ->
                _statusBannerMessage.value = "APK tidak valid: ${err.message}"
            }
        }
    }

    fun downloadAudioPack(packId: String) {
        engineScope.launch {
            _statusBannerMessage.value = "Mengambil katalog paket audio resmi Al-Gaffar TV..."
            val catalogRes = audioPackDownloader.fetchCatalog()
            catalogRes.onSuccess { packs ->
                val target = packs.firstOrNull { it.id == packId } ?: packs.firstOrNull()
                if (target == null) {
                    _statusBannerMessage.value = "Katalog paket audio belum tersedia di server."
                    return@onSuccess
                }
                _statusBannerMessage.value = "Mengunduh ${target.title}..."
                val dl = audioPackDownloader.downloadPack(target, assetsRootDir) { done, total ->
                    _statusBannerMessage.value = "Mengunduh ${target.title} ($done/$total file)..."
                }
                dl.onSuccess { count ->
                    refreshAudioDurationsAsync()
                    _audioInventoryTick.value += 1
                    refreshStorageHealth()
                    _statusBannerMessage.value = "Paket '${target.title}' selesai diunduh ($count file audio)!"
                }.onFailure { err ->
                    _statusBannerMessage.value = "Gagal mengunduh paket audio: ${err.message}"
                }
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal terhubung ke server paket audio: ${err.message}. Gunakan Sync USB / Remote PWA."
            }
        }
    }

    fun downloadImagePack(packId: String) {
        engineScope.launch {
            _statusBannerMessage.value = "Mengambil katalog paket gambar resmi Al-Gaffar TV..."
            val catalogRes = imagePackDownloader.fetchCatalog()
            catalogRes.onSuccess { packs ->
                val target = packs.firstOrNull { it.id == packId } ?: packs.firstOrNull()
                if (target == null) {
                    _statusBannerMessage.value = "Katalog paket gambar belum tersedia di server."
                    return@onSuccess
                }
                _statusBannerMessage.value = "Mengunduh ${target.title}..."
                val dl = imagePackDownloader.downloadPack(target, assetsRootDir) { done, total ->
                    _statusBannerMessage.value = "Mengunduh & optimasi ${target.title} ($done/$total gambar)..."
                }
                dl.onSuccess { result ->
                    updateSettings { prev ->
                        ImagePackDownloader.applyToSettings(prev, target, result)
                    }
                    refreshStorageHealth()
                    val sizeText = MediaStorageManager.formatByteSize(result.totalSavedBytes)
                    val msg = "Paket '${result.packTitle}' terpasang (${result.downloadedCount} gambar • $sizeText) ke seluruh slot media!"
                    AppLogger.info("MEDIA", msg)
                    _statusBannerMessage.value = msg
                }.onFailure { err ->
                    AppLogger.error("MEDIA", "Gagal mengunduh paket gambar: ${err.message}")
                    _statusBannerMessage.value = "Gagal mengunduh paket gambar: ${err.message}"
                }
            }.onFailure { err ->
                AppLogger.warn("MEDIA", "Gagal terhubung ke server paket gambar: ${err.message}")
                _statusBannerMessage.value = "Gagal terhubung ke server paket gambar: ${err.message}. Gunakan Sync USB / Remote PWA."
            }
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
        val isAfterMaghrib = !zonedNow.isBefore(maghribTime)
        val effectiveHijriDate = zonedNow.toLocalDate().plusDays(if (isAfterMaghrib) 1L else 0L)

        val hijriKey = HijriCacheKey(
            effectiveLocalDate = effectiveHijriDate,
            hijriAdjustment = hijriAdjustment,
            isAfterMaghrib = isAfterMaghrib
        )
        val hijriDate: HijriDateInfo
        val fastingInfo: FastingInfoSummary
        val prevHijri = cachedHijriDate
        val prevFasting = cachedFastingInfo
        if (cachedHijriKey == hijriKey && prevHijri != null && prevFasting != null) {
            hijriDate = prevHijri
            fastingInfo = prevFasting
        } else {
            hijriDate = hijriCalculator.calculateHijriDate(
                now = zonedNow,
                maghribTime = maghribTime,
                hijriAdjustment = hijriAdjustment
            )
            fastingInfo = hijriCalculator.getFastingInfo(
                now = zonedNow,
                maghribTime = maghribTime,
                hijriAdjustment = hijriAdjustment
            )
            cachedHijriDate = hijriDate
            cachedFastingInfo = fastingInfo
            cachedHijriKey = hijriKey
        }

        val safeMaxDays = settings.infoSettings.lookaheadDays.coerceIn(7, 365)
        val eventsKey = UpcomingEventsCacheKey(
            effectiveLocalDate = effectiveHijriDate,
            hijriAdjustment = hijriAdjustment,
            maxDays = safeMaxDays,
            customEvents = settings.infoSettings.islamicEvents
        )
        val upcomingEvents = if (cachedEventsKey == eventsKey) {
            cachedUpcomingEvents
        } else {
            val computed = hijriCalculator.getUpcomingIslamicEvents(
                now = zonedNow,
                maghribTime = maghribTime,
                hijriAdjustment = hijriAdjustment,
                maxDays = safeMaxDays,
                customEvents = settings.infoSettings.islamicEvents
            )
            cachedUpcomingEvents = computed
            cachedEventsKey = eventsKey
            computed
        }

        val tickResult = stateMachine.evaluateTick(
            now = zonedNow,
            settings = settings,
            schedule = schedule,
            audioDurations = cachedAudioDurations
        )
        val (remoteUrl, remoteQrUrl) = remoteServer.getRemoteUrlsNonBlocking()

        return JwsRuntimeSnapshot(
            now = zonedNow,
            settings = settings,
            schedule = schedule,
            hijriDate = hijriDate,
            fastingInfo = fastingInfo,
            upcomingEvents = upcomingEvents,
            tickResult = tickResult,
            remoteServerUrl = remoteUrl,
            remoteQrPairingUrl = remoteQrUrl,
            remoteAuthToken = remoteServer.authToken,
            deviceCode = deviceCode,
            isClockSuspect = clockIntegrityGuard.isEffectiveClockSuspect()
        )
    }

    fun simulateState(state: AppState?, durationSeconds: Long? = 120L) {
        val nowEpoch = currentEffectiveNow().toEpochSecond()
        stateMachine.setManualOverride(state, durationSeconds, nowEpoch)
        val snap = computeSnapshot(currentEffectiveNow())
        publishSnapshot(snap)
        audioEngine.onTick(snap.now, snap.settings, snap.schedule, snap.tickResult)
    }

    fun clearSimulation() {
        stateMachine.clearManualOverride()
        val snap = computeSnapshot(currentEffectiveNow())
        publishSnapshot(snap)
        audioEngine.onTick(snap.now, snap.settings, snap.schedule, snap.tickResult)
    }

    private fun toggleOrSimulate(target: AppState, durationSeconds: Long) {
        val cur = _snapshotFlow.value.tickResult
        if (cur.state == target && cur.isManualOverride) {
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
        if (_snapshotFlow.value.tickResult.state == AppState.STANDBY_SLIDESHOW) {
            simulateState(AppState.STANDBY, 10L)
        }
    }

    fun testPlayBeep1000Hz() {
        audioEngine.beepGenerator.playBeepImmediate(settingsRepository.currentSettings.globalVolume)
    }

    fun testPlayRamadhanSirine() {
        audioEngine.beepGenerator.playSynthesizedSirenSweep(settingsRepository.currentSettings.globalVolume)
    }

    fun toggleManualRelay(pin: Int, turnOn: Boolean) {
        engineScope.launch {
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
            prev.copy(brand = prev.brand.copy(clockStyle = nextStyle))
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
        engineScope.launch {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
            _statusBannerMessage.value = "Menyinkronkan media dari ${sourceDir.name} ke memori internal..."
            val res = MediaStorageManager.syncFromExternalFolder(
                sourceDir = sourceDir,
                assetsRootDir = assetsRootDir
            )
            res.onSuccess { summary ->
                if (summary.updatedLogoPath != null || summary.createdSlides.isNotEmpty()) {
                    updateSettings { prev ->
                        val updatedGroups = if (summary.createdSlides.isNotEmpty()) {
                            appendSlidesToGroup(prev.slideGroups, null, summary.createdSlides)
                        } else prev.slideGroups
                        prev.copy(
                            brand = if (summary.updatedLogoPath != null) {
                                prev.brand.copy(logo = summary.updatedLogoPath)
                            } else prev.brand,
                            slideGroups = updatedGroups,
                            slides = updatedGroups.flatMap { it.slides }
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
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch {
            settingsRepository.updateSettings(transform)
            publishSnapshot(computeSnapshot(currentEffectiveNow()))
            refreshStorageHealth()
        }
    }

    fun exportTakjilExcelTemplate(outputStream: OutputStream) {
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
            rows.forEach { row -> newMap[row.day.toString()] = row.names }
            prev.copy(ramadhan = prev.ramadhan.copy(takjil = newMap))
        }
        val totalNames = rows.sumOf { it.names.size }
        _statusBannerMessage.value = "Berhasil mengimpor $totalNames nama penyumbang Ta'jil untuk ${rows.size} hari!"
    }

    fun dismissTakjilImport() {
        _takjilPreviewFlow.value = null
    }

    fun exportFullBackupZip(outputStream: OutputStream) {
        engineScope.launch(Dispatchers.IO) {
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
        engineScope.launch(Dispatchers.IO) {
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
                publishSnapshot(computeSnapshot(currentEffectiveNow()))
                _statusBannerMessage.value = "Restore berhasil (${summary.restoredMosqueName})! " +
                    "Diekstrak ${summary.extractedAssetFilesCount} file media."
            }.onFailure { err ->
                _statusBannerMessage.value = "Gagal memulihkan Backup (.zip): ${err.message}"
            }
        }
    }

    private fun handleRemoteUploadStream(
        category: String,
        slot: String,
        rawFileName: String,
        inputStream: InputStream
    ): Result<String> {
        return runCatching {
            if (category == "ota_apk") {
                val targetDir = File(appContext?.cacheDir ?: assetsRootDir.parentFile, "ota_updates")
                val apkFile = otaUpdateManager.stageOfflineApk(inputStream, targetDir).getOrThrow()
                engineScope.launch(Dispatchers.Main) {
                    otaUpdateManager.launchApkInstaller(apkFile) { onBeforeLaunchExternalInstaller?.invoke() }
                }
                AppLogger.info("OTA", "APK pembaruan offline diterima via Remote PWA: ${apkFile.name}")
                return@runCatching "APK pembaruan berhasil diunggah! Konfirmasi instalasi di layar TV."
            }

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
                    AppLogger.info("REMOTE", "Logo diunggah (${MediaStorageManager.formatByteSize(saved.savedBytes)}): ${saved.relativePath}")
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
                    val targetGroupId = slot.trim().takeIf { it.isNotBlank() && !it.contains(',') }
                    val newSlide = SlideItem(
                        id = "slide_${System.currentTimeMillis()}",
                        title = title,
                        type = "image",
                        imageUrl = saved.relativePath,
                        duration = 12,
                        enabled = true
                    )
                    updateSettings { prev ->
                        val updatedGroups = appendSlidesToGroup(prev.slideGroups, targetGroupId, listOf(newSlide))
                        prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                    }
                    "Poster '$title' berhasil ditambahkan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
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
                    val targetGroupId = slot.trim().takeIf { it.isNotBlank() && !it.contains(',') }
                    val newSlide = SlideItem(
                        id = "slide_vid_${System.currentTimeMillis()}",
                        title = title,
                        type = "video",
                        videoUrl = saved.relativePath,
                        duration = 30,
                        enabled = true
                    )
                    updateSettings { prev ->
                        val updatedGroups = appendSlidesToGroup(prev.slideGroups, targetGroupId, listOf(newSlide))
                        prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                    }
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
                        "doa_adzan" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(doaAdzanImage = saved.relativePath)) }; "Do'a Setelah Adzan" }
                        "khutbah_jumat" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahJumatImage = saved.relativePath)) }; "Latar Khutbah Jum'at" }
                        "khutbah_idul_fitri" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahIdulFitriImage = saved.relativePath)) }; "Latar Khutbah Idul Fitri" }
                        "khutbah_idul_adha" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(khutbahIdulAdhaImage = saved.relativePath)) }; "Latar Khutbah Idul Adha" }
                        "ceramah_tarawih" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(ceramahTarawihImage = saved.relativePath)) }; "Latar Ceramah Tarawih" }
                        "sholat_5_waktu" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(sholat5WaktuImage = saved.relativePath)) }; "Latar Sholat 5 Waktu" }
                        "sholat_jumat" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(sholatJumatImage = saved.relativePath)) }; "Latar Sholat Jum'at" }
                        "sholat_idul_fitri" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(sholatIdulFitriImage = saved.relativePath)) }; "Latar Sholat Idul Fitri" }
                        "sholat_idul_adha" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(sholatIdulAdhaImage = saved.relativePath)) }; "Latar Sholat Idul Adha" }
                        "sholat_tarawih_witir" -> { updateSettings { prev -> prev.copy(media = prev.media.copy(sholatTarawihWitirImage = saved.relativePath)) }; "Latar Sholat Tarawih & Witir" }
                        else -> throw IllegalArgumentException("Slot media latar tidak dikenal: $slot")
                    }
                    "Gambar $label berhasil diperbarui (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "tartil_image" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(assetsRootDir, "media/tartil", cleanName, inputStream).getOrThrow()
                    updateSettings { prev -> prev.copy(media = prev.media.copy(tartilMediaType = "image", tartilImages = prev.media.tartilImages + saved.relativePath)) }
                    "Gambar slide Tartil berhasil diunggah!"
                }

                "tartil_video" -> {
                    val safeName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val saved = MediaStorageManager.saveStreamWithGuard(assetsRootDir, "media/tartil/${System.currentTimeMillis()}_$safeName", inputStream, MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES).getOrThrow()
                    updateSettings { prev -> prev.copy(media = prev.media.copy(tartilMediaType = "video", tartilVideo = saved.relativePath)) }
                    "Video Tartil berhasil diunggah!"
                }

                "tarhim_image" -> {
                    val saved = MediaStorageManager.optimizeAndSaveImage(assetsRootDir, "media/tarhim", cleanName, inputStream).getOrThrow()
                    updateSettings { prev -> prev.copy(media = prev.media.copy(tarhimMediaType = "image", tarhimImages = prev.media.tarhimImages + saved.relativePath)) }
                    "Gambar slide Tarhim berhasil diunggah!"
                }

                "tarhim_video" -> {
                    val safeName = cleanName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val saved = MediaStorageManager.saveStreamWithGuard(assetsRootDir, "media/tarhim/${System.currentTimeMillis()}_$safeName", inputStream, MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES).getOrThrow()
                    updateSettings { prev -> prev.copy(media = prev.media.copy(tarhimMediaType = "video", tarhimVideo = saved.relativePath)) }
                    "Video Tarhim berhasil diunggah!"
                }

                "tartil", "tarhim", "adzan" -> {
                    val safeSlot = slot.takeIf { it in setOf("fajr", "dhuhr", "asr", "maghrib", "isha", "jumat", "default") } ?: "default"
                    val saved = MediaStorageManager.saveStreamWithGuard(assetsRootDir, "audio/$category/$safeSlot.mp3", inputStream, MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES).getOrThrow()
                    refreshAudioDurationsAsync()
                    _audioInventoryTick.value += 1
                    "Audio $category ($safeSlot) berhasil disimpan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                "custom" -> {
                    val finalName = if (cleanName.contains('.')) cleanName else "$cleanName.mp3"
                    val saved = MediaStorageManager.saveStreamWithGuard(assetsRootDir, "audio/custom/$finalName", inputStream, MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES).getOrThrow()
                    _audioInventoryTick.value += 1
                    "Audio kustom '$finalName' berhasil ditambahkan (${MediaStorageManager.formatByteSize(saved.savedBytes)})!"
                }

                else -> throw IllegalArgumentException("Kategori upload tidak dikenal: $category")
            }

            refreshStorageHealth()
            resultMsg
        }
    }

    internal fun handleRemoteCommand(payload: RemoteCommandPayload) {
        engineScope.launch {
            when (payload.action.lowercase()) {
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

                "regenerate_remote_token" -> regenerateRemoteToken()

                "complete_onboarding" -> {
                    val mosqueName = payload.stringArg("name")?.trim()
                    val mosqueAddr = payload.stringArg("address")?.trim()
                    val mosquePhone = payload.stringArg("phone")?.trim()
                    val province = payload.stringArg("provinceName")?.trim()
                    val city = payload.stringArg("cityName")?.trim()
                    val lat = payload.doubleArg("latitude")?.coerceIn(-90.0, 90.0)
                    val lng = payload.doubleArg("longitude")?.coerceIn(-180.0, 180.0)
                    val tz = payload.stringArg("timezone")?.takeIf { it.isNotBlank() }
                    val newPin = payload.stringArg("superadminPin")?.trim()

                    updateSettings { prev ->
                        val updatedUsers = if (!newPin.isNullOrBlank() && newPin.length >= 6) {
                            val newHash = AuthManager.hashPinPbkdf2(newPin)
                            val list = prev.users.toMutableList()
                            val idx = list.indexOfFirst { it.role.equals("superadmin", ignoreCase = true) }
                            if (idx >= 0) list[idx] = list[idx].copy(pinHash = newHash)
                            else list.add(UserAccount("superadmin-1", AuthManager.DEFAULT_SUPERADMIN_USERNAME, newHash, "superadmin"))
                            list
                        } else prev.users

                        prev.copy(
                            onboardingCompleted = true,
                            brand = prev.brand.copy(
                                name = mosqueName?.ifBlank { prev.brand.name } ?: prev.brand.name,
                                address = mosqueAddr?.ifBlank { prev.brand.address } ?: prev.brand.address,
                                phone = mosquePhone ?: prev.brand.phone
                            ),
                            prayerSettings = prev.prayerSettings.copy(
                                provinceName = province ?: prev.prayerSettings.provinceName,
                                cityName = city ?: prev.prayerSettings.cityName,
                                latitude = lat ?: prev.prayerSettings.latitude,
                                longitude = lng ?: prev.prayerSettings.longitude,
                                timezone = tz ?: prev.prayerSettings.timezone
                            ),
                            users = updatedUsers
                        )
                    }
                    AppLogger.info("ONBOARDING", "Setup awal masjid selesai melalui Remote PWA.")
                }

                "set_theme" -> {
                    val themeId = payload.stringArg("themeId").orEmpty()
                    if (themeId.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(themeSettings = prev.themeSettings.copy(currentThemeId = themeId, autoThemeEnabled = false))
                        }
                    }
                }

                "set_layout" -> {
                    val posStr = payload.stringArg("position").orEmpty()
                    val pos = LayoutPosition.entries.firstOrNull { it.serialName == posStr }
                    if (pos != null) {
                        updateSettings { prev ->
                            prev.copy(layoutSettings = prev.layoutSettings.copy(position = pos, autoRotateLayout = false))
                        }
                    }
                }

                "set_clock_style" -> {
                    val style = payload.stringArg("style").orEmpty().lowercase()
                    if (style.isNotBlank()) {
                        updateSettings { prev -> prev.copy(brand = prev.brand.copy(clockStyle = style)) }
                    }
                }

                "update_volume" -> {
                    val vol = payload.doubleArg("volume")?.toFloat()?.coerceIn(0f, 1f)
                    if (vol != null) updateSettings { prev -> prev.copy(globalVolume = vol) }
                }

                "play_custom_audio" -> {
                    updateSettings { prev ->
                        prev.copy(customAudio = prev.customAudio.copy(isPlayingLive = !prev.customAudio.isPlayingLive))
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
                            "makkah" -> prev.live.copy(enabled = true, source = "makkah", makkahVideoId = videoId.ifBlank { prev.live.makkahVideoId })
                            "madinah" -> prev.live.copy(enabled = true, source = "madinah", madinahVideoId = videoId.ifBlank { prev.live.madinahVideoId })
                            else -> prev.live.copy(enabled = true, source = "custom", customVideoId = videoId.ifBlank { prev.live.customVideoId })
                        }
                        prev.copy(live = updatedLive)
                    }
                    simulateState(AppState.LIVE, 900L)
                }

                "save_live_presets" -> {
                    val makkahId = payload.stringArg("makkahVideoId")?.trim()
                    val madinahId = payload.stringArg("madinahVideoId")?.trim()
                    val customId = payload.stringArg("customVideoId")?.trim()
                    val makkahHls = payload.stringArg("makkahHlsUrl")?.trim()
                    val madinahHls = payload.stringArg("madinahHlsUrl")?.trim()
                    val customHls = payload.stringArg("customStreamUrl")?.trim()
                    val preferHls = payload.booleanArg("preferNativeHls")
                    updateSettings { prev ->
                        prev.copy(
                            live = prev.live.copy(
                                makkahVideoId = makkahId?.ifBlank { prev.live.makkahVideoId } ?: prev.live.makkahVideoId,
                                madinahVideoId = madinahId?.ifBlank { prev.live.madinahVideoId } ?: prev.live.madinahVideoId,
                                customVideoId = customId ?: prev.live.customVideoId,
                                makkahHlsUrl = makkahHls ?: prev.live.makkahHlsUrl,
                                madinahHlsUrl = madinahHls ?: prev.live.madinahHlsUrl,
                                customStreamUrl = customHls ?: prev.live.customStreamUrl,
                                preferNativeHls = preferHls ?: prev.live.preferNativeHls
                            )
                        )
                    }
                }

                "toggle_prefer_hls" -> {
                    val prefer = payload.booleanArg("preferNativeHls")
                    updateSettings { prev ->
                        prev.copy(live = prev.live.copy(preferNativeHls = prefer ?: !prev.live.preferNativeHls))
                    }
                }

                "toggle_live_mute" -> {
                    val muted = payload.booleanArg("muted")
                    updateSettings { prev -> prev.copy(live = prev.live.copy(muted = muted ?: !prev.live.muted)) }
                }

                "download_audio_pack" -> {
                    val packId = payload.stringArg("packId")?.trim().orEmpty().ifBlank { "standard_pack" }
                    downloadAudioPack(packId)
                }

                "download_image_pack" -> {
                    val packId = payload.stringArg("packId")?.trim().orEmpty().ifBlank { "standard_image_pack" }
                    downloadImagePack(packId)
                }

                "check_ota_update" -> {
                    checkOtaUpdate()
                }

                "toggle_mode" -> {
                    val mode = payload.stringArg("mode").orEmpty().lowercase()
                    updateSettings { prev ->
                        when (mode) {
                            "jumat" -> prev.copy(jumat = prev.jumat.copy(enabled = !prev.jumat.enabled))
                            "ramadhan" -> prev.copy(ramadhan = prev.ramadhan.copy(isRamadhan = !prev.ramadhan.isRamadhan))
                            "idul_fitri" -> prev.copy(idulFitri = prev.idulFitri.copy(enabled = !prev.idulFitri.enabled))
                            "idul_adha" -> prev.copy(idulAdha = prev.idulAdha.copy(enabled = !prev.idulAdha.enabled))
                            "slideshow" -> prev.copy(standbySlideshow = prev.standbySlideshow.copy(enabled = !prev.standbySlideshow.enabled))
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
                            cfg.copy(isTartil = nextEnabled, tartilDuration = if (nextEnabled) cfg.tartilDuration.coerceAtLeast(10) else cfg.tartilDuration)
                        }
                        "isTarhim" -> {
                            val nextEnabled = !(cfg.isTarhim && cfg.tarhimDuration > 0)
                            cfg.copy(isTarhim = nextEnabled, tarhimDuration = if (nextEnabled) cfg.tarhimDuration.coerceAtLeast(5) else cfg.tarhimDuration)
                        }
                        "isAdzan" -> cfg.copy(isAdzan = !cfg.isAdzan)
                        else -> cfg
                    }
                    updateSettings { prev ->
                        val p = prev.prayers
                        val updated = when (prayer) {
                            "fajr" -> p.copy(fajr = toggleStage(p.fajr))
                            "dhuhr" -> p.copy(dhuhr = toggleStage(p.dhuhr))
                            "asr" -> p.copy(asr = toggleStage(p.asr))
                            "maghrib" -> p.copy(maghrib = toggleStage(p.maghrib))
                            "isha" -> p.copy(isha = toggleStage(p.isha))
                            else -> p
                        }
                        prev.copy(prayers = updated)
                    }
                }

                "update_prayer_iqomah", "update_prayer_stage" -> {
                    val prayer = payload.stringArg("prayer").orEmpty().lowercase()
                    val tartil = payload.intArg("tartilDuration")?.coerceIn(5, 60)
                    val adzan = payload.intArg("adzanDuration")?.coerceIn(1, 15)
                    val iqomah = payload.intArg("iqomahDuration")?.coerceIn(1, 60)
                    val sholat = payload.intArg("sholatDuration")?.coerceIn(5, 60)
                    val imam = payload.rawStringArg("imam")
                    val muadzin = payload.rawStringArg("muadzin")
                    val adzanTagline = payload.rawStringArg("adzanTagline")
                    val adzanMessage = payload.rawStringArg("adzanMessage")
                    val sholatMessage = payload.rawStringArg("sholatMessage")
                    val correction = payload.intArg("correction")?.coerceIn(-15, 15)
                    fun applyStage(cfg: PrayerStageConfig): PrayerStageConfig = cfg.copy(
                        tartilDuration = tartil ?: cfg.tartilDuration,
                        adzanDuration = adzan ?: cfg.adzanDuration,
                        iqomahDuration = iqomah ?: cfg.iqomahDuration,
                        sholatDuration = sholat ?: cfg.sholatDuration,
                        imam = imam ?: cfg.imam,
                        muadzin = muadzin ?: cfg.muadzin,
                        adzanTagline = adzanTagline ?: cfg.adzanTagline,
                        adzanMessage = adzanMessage ?: cfg.adzanMessage,
                        sholatMessage = sholatMessage ?: cfg.sholatMessage
                    )
                    updateSettings { prev ->
                        val p = prev.prayers
                        val updatedPrayers = when (prayer) {
                            "fajr" -> p.copy(fajr = applyStage(p.fajr))
                            "dhuhr" -> p.copy(dhuhr = applyStage(p.dhuhr))
                            "asr" -> p.copy(asr = applyStage(p.asr))
                            "maghrib" -> p.copy(maghrib = applyStage(p.maghrib))
                            "isha" -> p.copy(isha = applyStage(p.isha))
                            else -> p
                        }
                        val c = prev.prayerSettings.corrections
                        val updatedCorrections = if (correction != null) {
                            when (prayer) {
                                "fajr" -> c.copy(fajr = correction)
                                "dhuhr" -> c.copy(dhuhr = correction)
                                "asr" -> c.copy(asr = correction)
                                "maghrib" -> c.copy(maghrib = correction)
                                "isha" -> c.copy(isha = correction)
                                else -> c
                            }
                        } else c
                        prev.copy(
                            prayers = updatedPrayers,
                            prayerSettings = prev.prayerSettings.copy(corrections = updatedCorrections)
                        )
                    }
                }

                "update_jumat_stage" -> {
                    val enabled = payload.booleanArg("enabled")
                    val waktuJumat = payload.intArg("waktuJumatDuration")?.coerceIn(3, 45)
                    val khutbah = payload.intArg("khutbahDuration")?.coerceIn(5, 90)
                    val sholatJumat = payload.intArg("sholatJumatDuration")?.coerceIn(5, 45)
                    val khutbahMsg = payload.rawStringArg("khutbahMessage")
                    val sholatMsg = payload.rawStringArg("sholatMessage")
                    val khotib = payload.rawStringArg("khotib")
                    val imam = payload.rawStringArg("imam")
                    val muadzin = payload.rawStringArg("muadzin")
                    updateSettings { prev ->
                        val j = prev.jumat
                        val updatedPet = if (khotib != null || imam != null || muadzin != null) {
                            val dateKey = j.petugas.selectedDate.ifBlank {
                                JwsStateMachine.snapToNearestFriday(_snapshotFlow.value.schedule.date).toString()
                            }
                            val existing = j.petugas.records[dateKey] ?: JumatPetugasRecord()
                            val updatedRec = existing.copy(
                                khotib = khotib ?: existing.khotib,
                                imam = imam ?: existing.imam,
                                muadzin = muadzin ?: existing.muadzin
                            )
                            val newMap = j.petugas.records.toMutableMap().apply { put(dateKey, updatedRec) }
                            j.petugas.copy(selectedDate = dateKey, records = newMap)
                        } else j.petugas
                        prev.copy(
                            jumat = j.copy(
                                enabled = enabled ?: j.enabled,
                                waktuJumatDuration = waktuJumat ?: j.waktuJumatDuration,
                                khutbahDuration = khutbah ?: j.khutbahDuration,
                                sholatJumatDuration = sholatJumat ?: j.sholatJumatDuration,
                                khutbahMessage = khutbahMsg ?: j.khutbahMessage,
                                sholatMessage = sholatMsg ?: j.sholatMessage,
                                petugas = updatedPet
                            )
                        )
                    }
                }

                "update_custom_audio_live" -> {
                    val file = payload.rawStringArg("liveAudioFile")
                    val dur = payload.intArg("liveDurationMinutes")?.coerceIn(1, 120)
                    val playing = payload.booleanArg("isPlayingLive")
                    updateSettings { prev ->
                        val ca = prev.customAudio
                        prev.copy(
                            customAudio = ca.copy(
                                liveAudioFile = file ?: ca.liveAudioFile,
                                liveDurationMinutes = dur ?: ca.liveDurationMinutes,
                                isPlayingLive = playing ?: ca.isPlayingLive
                            )
                        )
                    }
                }

                "add_custom_audio_schedule" -> {
                    val name = payload.stringArg("name").orEmpty().ifBlank { "Jadwal Audio" }
                    val audioFile = payload.rawStringArg("audioFile").orEmpty()
                    val scheduleType = payload.stringArg("scheduleType")?.takeIf { it == "once" || it == "weekly" } ?: "weekly"
                    val dayOfWeek = (payload.intArg("dayOfWeek") ?: 5).coerceIn(0, 6)
                    val date = payload.rawStringArg("date").orEmpty()
                    val time = payload.rawStringArg("time").orEmpty().ifBlank { "11:00" }
                    val dur = (payload.intArg("durationMinutes") ?: 15).coerceIn(1, 120)
                    val item = id.algaffar.jwstv.domain.model.ScheduledAudioItem(
                        id = "sched-${System.currentTimeMillis()}",
                        name = name,
                        audioFile = audioFile,
                        scheduleType = scheduleType,
                        dayOfWeek = dayOfWeek,
                        date = date,
                        time = time,
                        durationMinutes = dur,
                        enabled = true
                    )
                    updateSettings { prev ->
                        prev.copy(customAudio = prev.customAudio.copy(schedules = prev.customAudio.schedules + item))
                    }
                }

                "update_custom_audio_schedule" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val idxArg = payload.intArg("index")
                    val toggleEnabled = payload.booleanArg("toggleEnabled") == true
                    val enabled = payload.booleanArg("enabled")
                    val name = payload.rawStringArg("name")
                    val audioFile = payload.rawStringArg("audioFile")
                    val scheduleType = payload.stringArg("scheduleType")?.takeIf { it == "once" || it == "weekly" }
                    val dayOfWeek = payload.intArg("dayOfWeek")?.coerceIn(0, 6)
                    val date = payload.rawStringArg("date")
                    val time = payload.rawStringArg("time")
                    val dur = payload.intArg("durationMinutes")?.coerceIn(1, 120)
                    updateSettings { prev ->
                        val list = prev.customAudio.schedules.toMutableList()
                        val idx = if (id.isNotBlank()) list.indexOfFirst { it.id == id } else (idxArg ?: -1)
                        if (idx in list.indices) {
                            val cur = list[idx]
                            list[idx] = cur.copy(
                                name = name ?: cur.name,
                                audioFile = audioFile ?: cur.audioFile,
                                scheduleType = scheduleType ?: cur.scheduleType,
                                dayOfWeek = dayOfWeek ?: cur.dayOfWeek,
                                date = date ?: cur.date,
                                time = time ?: cur.time,
                                durationMinutes = dur ?: cur.durationMinutes,
                                enabled = if (toggleEnabled) !cur.enabled else (enabled ?: cur.enabled)
                            )
                        }
                        prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                    }
                }

                "delete_custom_audio_schedule" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val idxArg = payload.intArg("index")
                    updateSettings { prev ->
                        val list = prev.customAudio.schedules.toMutableList()
                        val idx = if (id.isNotBlank()) list.indexOfFirst { it.id == id } else (idxArg ?: -1)
                        if (idx in list.indices) list.removeAt(idx)
                        prev.copy(customAudio = prev.customAudio.copy(schedules = list))
                    }
                }

                "delete_custom_audio_file" -> {
                    val fileName = payload.stringArg("fileName").orEmpty().substringAfterLast('/').substringAfterLast('\\')
                    if (fileName.isNotBlank()) {
                        val file = File(assetsRootDir, "audio/custom/$fileName")
                        if (file.exists() && file.isFile) {
                            file.delete()
                            _audioInventoryTick.value += 1
                        }
                    }
                }

                "update_prayer_config" -> {
                    val province = payload.stringArg("provinceName")
                    val city = payload.stringArg("cityName")
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
                                provinceName = province ?: ps.provinceName,
                                cityName = city ?: ps.cityName,
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
                    val title = payload.stringArg("title")?.trim().orEmpty()
                    val text = payload.stringArg("text")?.trim().orEmpty()
                    if (title.isNotBlank() || text.isNotBlank()) {
                        updateSettings { prev ->
                            val nextNum = prev.floatingOverlays.size + 1
                            val item = FloatingOverlayItem(
                                id = "overlay_${System.currentTimeMillis()}",
                                title = title.ifBlank { "Overlay $nextNum" },
                                message = text
                            )
                            val updated = prev.floatingOverlays + item
                            prev.copy(
                                floatingOverlays = updated,
                                overlayTextList = updated.map { it.message }
                            )
                        }
                    }
                }

                "remove_overlay_text" -> {
                    val id = payload.stringArg("id")?.trim().orEmpty()
                    val idx = payload.intArg("index")
                    updateSettings { prev ->
                        val updated = when {
                            id.isNotBlank() -> prev.floatingOverlays.filterNot { it.id == id }
                            idx != null -> prev.floatingOverlays.filterIndexed { i, _ -> i != idx }
                            else -> prev.floatingOverlays
                        }
                        val validIds = updated.map { it.id }.toSet()
                        prev.copy(
                            floatingOverlays = updated,
                            overlayTextList = updated.map { it.message },
                            slideGroups = prev.slideGroups.map { grp ->
                                grp.copy(overlayIds = grp.overlayIds.filter { it in validIds })
                            }
                        )
                    }
                }

                "clear_overlay_text" -> updateSettings { prev ->
                    prev.copy(
                        floatingOverlays = emptyList(),
                        overlayTextList = emptyList(),
                        slideGroups = prev.slideGroups.map { it.copy(overlayIds = emptyList()) }
                    )
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
                            prev.copy(mosqueInfoList = prev.mosqueInfoList.map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
                        }
                    }
                }

                "remove_info" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) updateSettings { prev -> prev.copy(mosqueInfoList = prev.mosqueInfoList.filterNot { it.id == id }) }
                }

                "clear_info" -> updateSettings { prev -> prev.copy(mosqueInfoList = emptyList()) }

                "toggle_sunnah_fasting" -> {
                    updateSettings { prev ->
                        prev.copy(infoSettings = prev.infoSettings.copy(enableSunnahFasting = !prev.infoSettings.enableSunnahFasting))
                    }
                }

                "update_info_settings" -> {
                    val enableSunnah = payload.booleanArg("enableSunnahFasting")
                    val lookahead = payload.intArg("lookaheadDays")?.coerceIn(7, 365)
                    updateSettings { prev ->
                        prev.copy(
                            infoSettings = prev.infoSettings.copy(
                                enableSunnahFasting = enableSunnah ?: prev.infoSettings.enableSunnahFasting,
                                lookaheadDays = lookahead ?: prev.infoSettings.lookaheadDays
                            )
                        )
                    }
                }

                "reset_islamic_events" -> {
                    updateSettings { prev ->
                        prev.copy(
                            infoSettings = id.algaffar.jwstv.domain.model.InfoDisplaySettings(
                                enableSunnahFasting = true,
                                lookaheadDays = 60,
                                islamicEvents = id.algaffar.jwstv.domain.model.InfoDisplaySettings.defaultIslamicEvents()
                            )
                        )
                    }
                }

                "add_islamic_event" -> {
                    val name = payload.stringArg("name").orEmpty().ifBlank { "Agenda Masjid Baru" }
                    val type = payload.stringArg("type")?.takeIf { it == "hijri" || it == "gregorian" } ?: "hijri"
                    val hijriMonth = (payload.intArg("hijriMonth") ?: 1).coerceIn(1, 12)
                    val hijriDay = (payload.intArg("hijriDay") ?: 1).coerceIn(1, 30)
                    val gregorianDate = payload.rawStringArg("gregorianDate").orEmpty()
                    val ev = id.algaffar.jwstv.domain.model.CustomIslamicEventItem(
                        id = "agenda-${System.currentTimeMillis()}",
                        name = name,
                        type = type,
                        hijriMonth = hijriMonth,
                        hijriDay = hijriDay,
                        gregorianDate = gregorianDate,
                        enabled = true
                    )
                    updateSettings { prev ->
                        prev.copy(
                            infoSettings = prev.infoSettings.copy(
                                islamicEvents = listOf(ev) + prev.infoSettings.islamicEvents
                            )
                        )
                    }
                }

                "toggle_islamic_event" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val idxArg = payload.intArg("index")
                    updateSettings { prev ->
                        val list = prev.infoSettings.islamicEvents.toMutableList()
                        val idx = if (id.isNotBlank()) list.indexOfFirst { it.id == id } else (idxArg ?: -1)
                        if (idx in list.indices) {
                            list[idx] = list[idx].copy(enabled = !list[idx].enabled)
                        }
                        prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
                    }
                }

                "delete_islamic_event" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val idxArg = payload.intArg("index")
                    updateSettings { prev ->
                        val list = prev.infoSettings.islamicEvents.toMutableList()
                        val idx = if (id.isNotBlank()) list.indexOfFirst { it.id == id } else (idxArg ?: -1)
                        if (idx in list.indices) list.removeAt(idx)
                        prev.copy(infoSettings = prev.infoSettings.copy(islamicEvents = list))
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
                        prev.copy(jumat = prev.jumat.copy(petugas = prev.jumat.petugas.copy(selectedDate = dateKey, records = newMap)))
                    }
                }

                "update_ramadhan_night" -> {
                    val night = payload.intArg("currentNight")?.coerceIn(1, 30)
                    if (night != null) {
                        updateSettings { prev ->
                            prev.copy(ramadhan = prev.ramadhan.copy(currentNight = night, autoSyncNightFromHijri = false))
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
                    val showFloating = payload.booleanArg("showFloatingOnMain")
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
                                showFloatingOnMain = showFloating ?: f.showFloatingOnMain,
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
                            val updatedGroups = prev.slideGroups.map { grp ->
                                grp.copy(slides = grp.slides.map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
                            }
                            prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                        }
                    }
                }

                "delete_slide" -> {
                    val id = payload.stringArg("id").orEmpty()
                    if (id.isNotBlank()) {
                        updateSettings { prev ->
                            val updatedGroups = prev.slideGroups.map { grp ->
                                grp.copy(slides = grp.slides.filterNot { it.id == id })
                            }
                            prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                        }
                        refreshStorageHealth()
                    }
                }

                "reset_default_slides" -> {
                    updateSettings { prev ->
                        val defaults = SlideshowGroup.defaultGroups()
                        prev.copy(slideGroups = defaults, slides = defaults.flatMap { it.slides })
                    }
                    refreshStorageHealth()
                }

                "add_text_slide" -> {
                    val title = payload.stringArg("title").orEmpty().ifBlank { "Pengumuman Masjid" }
                    val content = payload.stringArg("content").orEmpty()
                    val duration = payload.intArg("duration")?.coerceIn(4, 120) ?: 12
                    val groupId = payload.stringArg("groupId")?.trim()
                    if (content.isNotBlank()) {
                        val newSlide = SlideItem(
                            id = "slide_${System.currentTimeMillis()}",
                            title = title,
                            content = content,
                            type = "text",
                            duration = duration,
                            enabled = true
                        )
                        updateSettings { prev ->
                            val updatedGroups = appendSlidesToGroup(prev.slideGroups, groupId, listOf(newSlide))
                            prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                        }
                    }
                }

                "add_system_slide" -> {
                    val type = payload.stringArg("type").orEmpty().trim()
                    val title = payload.stringArg("title").orEmpty()
                    val duration = payload.intArg("duration")?.coerceIn(4, 120) ?: 15
                    val groupId = payload.stringArg("groupId")?.trim()
                    if (type.isNotBlank() && type != "qr_jumat") {
                        val newSlide = SlideItem(
                            id = "slide_${type}_${System.currentTimeMillis()}",
                            type = type,
                            title = title.ifBlank { type.replace('_', ' ').uppercase() },
                            duration = duration,
                            enabled = true
                        )
                        updateSettings { prev ->
                            val updatedGroups = appendSlidesToGroup(prev.slideGroups, groupId, listOf(newSlide))
                            prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                        }
                    }
                }

                "add_slide_group" -> {
                    val reqName = payload.stringArg("name")?.trim().orEmpty()
                    updateSettings { prev ->
                        val nextName = reqName.ifBlank { SlideshowGroup.nextDefaultGroupName(prev.slideGroups) }
                        val newGroup = SlideshowGroup(
                            id = "group_${System.currentTimeMillis()}",
                            name = nextName,
                            enabled = true,
                            slides = emptyList(),
                            overlayIds = emptyList(),
                            overlayPosition = SlideshowGroup.POS_BOTTOM_LEFT,
                            showQrCard = true,
                            showInMain = true,
                            showInSlideshow = true
                        )
                        prev.copy(slideGroups = prev.slideGroups + newGroup)
                    }
                }

                "update_slide_group" -> {
                    val groupId = payload.stringArg("id").orEmpty()
                    if (groupId.isNotBlank()) {
                        val name = payload.stringArg("name")?.trim()
                        val enabled = payload.booleanArg("enabled")
                        val overlayPos = payload.stringArg("overlayPosition")?.trim()
                        val showQr = payload.booleanArg("showQrCard")
                        updateSettings { prev ->
                            prev.copy(
                                slideGroups = prev.slideGroups.map { grp ->
                                    if (grp.id != groupId) grp
                                    else grp.copy(
                                        name = name?.ifBlank { grp.name } ?: grp.name,
                                        enabled = enabled ?: grp.enabled,
                                        overlayPosition = overlayPos?.let { SlideshowGroup.normalizeCornerPosition(it) } ?: grp.overlayPosition,
                                        showQrCard = showQr ?: grp.showQrCard
                                    )
                                }
                            )
                        }
                    }
                }

                "delete_slide_group" -> {
                    val groupId = payload.stringArg("id").orEmpty()
                    if (groupId.isNotBlank()) {
                        updateSettings { prev ->
                            val updatedGroups = prev.slideGroups.filterNot { it.id == groupId }
                            prev.copy(slideGroups = updatedGroups, slides = updatedGroups.flatMap { it.slides })
                        }
                        refreshStorageHealth()
                    }
                }

                "toggle_slide_group_state" -> {
                    val groupId = payload.stringArg("id").orEmpty()
                    val mode = payload.stringArg("mode").orEmpty().lowercase()
                    if (groupId.isNotBlank() && mode.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slideGroups = prev.slideGroups.map { grp ->
                                    if (grp.id != groupId) grp else when (mode) {
                                        "main" -> grp.copy(showInMain = !grp.showInMain)
                                        "slideshow" -> grp.copy(showInSlideshow = !grp.showInSlideshow)
                                        "jumat" -> grp.copy(showInJumat = !grp.showInJumat)
                                        "ramadhan" -> grp.copy(showInRamadhan = !grp.showInRamadhan)
                                        "idul_fitri" -> grp.copy(showInIdulFitri = !grp.showInIdulFitri)
                                        "idul_adha" -> grp.copy(showInIdulAdha = !grp.showInIdulAdha)
                                        else -> grp
                                    }
                                }
                            )
                        }
                    }
                }

                "toggle_slide_group_overlay" -> {
                    val groupId = payload.stringArg("id").orEmpty()
                    val overlayId = payload.stringArg("overlayId").orEmpty()
                    if (groupId.isNotBlank() && overlayId.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slideGroups = prev.slideGroups.map { grp ->
                                    if (grp.id != groupId) grp else {
                                        val nextIds = if (overlayId in grp.overlayIds) {
                                            grp.overlayIds.filterNot { it == overlayId }
                                        } else {
                                            grp.overlayIds + overlayId
                                        }
                                        grp.copy(overlayIds = nextIds)
                                    }
                                }
                            )
                        }
                    }
                }

                "toggle_slide_mode" -> {
                    val id = payload.stringArg("id").orEmpty()
                    val mode = payload.stringArg("mode").orEmpty().lowercase()
                    if (id.isNotBlank() && mode.isNotBlank()) {
                        updateSettings { prev ->
                            prev.copy(
                                slideGroups = prev.slideGroups.map { grp ->
                                    if (grp.id != id) grp else when (mode) {
                                        "main" -> grp.copy(showInMain = !grp.showInMain)
                                        "slideshow" -> grp.copy(showInSlideshow = !grp.showInSlideshow)
                                        "jumat" -> grp.copy(showInJumat = !grp.showInJumat)
                                        "ramadhan" -> grp.copy(showInRamadhan = !grp.showInRamadhan)
                                        "idul_fitri" -> grp.copy(showInIdulFitri = !grp.showInIdulFitri)
                                        "idul_adha" -> grp.copy(showInIdulAdha = !grp.showInIdulAdha)
                                        else -> grp
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
                                tarhimImages = if (deleteImageIndex != null) m.tarhimImages.filterIndexed { i, _ -> i != deleteImageIndex } else m.tarhimImages,
                                tarhimVideo = if (clearVideo) "" else m.tarhimVideo
                            )
                        } else {
                            m.copy(
                                tartilMediaType = mediaType?.takeIf { it in setOf("image", "video") } ?: m.tartilMediaType,
                                tartilSlideInterval = interval ?: m.tartilSlideInterval,
                                tartilImages = if (deleteImageIndex != null) m.tartilImages.filterIndexed { i, _ -> i != deleteImageIndex } else m.tartilImages,
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
                    if (cat == "custom" && slot.isNotBlank()) deleteCustomAudioFile(slot)
                    else if (cat in setOf("tartil", "tarhim", "adzan") && slot.isNotBlank()) deletePrayerAudioSlot(cat, slot.lowercase())
                }

                "cleanup_orphan_media" -> cleanOrphanMedia()

                "update_standby_slideshow" -> {
                    val enabled = payload.booleanArg("enabled")
                    val slideDuration = payload.intArg("slideDuration")?.coerceIn(5, 600)
                    val repeatCount = payload.intArg("repeatCount")?.coerceIn(1, 10)
                    val showNextPrayerCard = payload.booleanArg("showNextPrayerCard")
                    val nextPrayerThreshold = payload.intArg("nextPrayerThresholdMinutes")?.coerceIn(5, 120)
                    updateSettings { prev ->
                        val s = prev.standbySlideshow
                        prev.copy(
                            standbySlideshow = s.copy(
                                enabled = enabled ?: s.enabled,
                                durationMain = slideDuration ?: s.durationMain,
                                repeatCount = repeatCount ?: s.repeatCount,
                                showNextPrayerCard = showNextPrayerCard ?: s.showNextPrayerCard,
                                nextPrayerThresholdMinutes = nextPrayerThreshold ?: s.nextPrayerThresholdMinutes
                            )
                        )
                    }
                }

                "update_jumat_keuangan" -> {
                    val selectedDate = payload.rawStringArg("selectedDate")?.takeIf { it.isNotBlank() }
                    val customTitle = payload.rawStringArg("customTitle")
                    val source = payload.stringArg("source")?.takeIf { it == "manual" || it == "online" }
                    val saldoKasLalu = payload.stringArg("saldoKasLalu")?.toLongOrNull() ?: payload.intArg("saldoKasLalu")?.toLong()
                    val pemasukan = payload.stringArg("pemasukan")?.toLongOrNull() ?: payload.intArg("pemasukan")?.toLong()
                    val pengeluaran = payload.stringArg("pengeluaran")?.toLongOrNull() ?: payload.intArg("pengeluaran")?.toLong()
                    updateSettings { prev ->
                        val today = _snapshotFlow.value.schedule.date
                        val dateKey = selectedDate ?: prev.jumat.keuangan.selectedDate.ifBlank {
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
                                    customTitle = customTitle ?: prev.jumat.keuangan.customTitle,
                                    source = source ?: prev.jumat.keuangan.source,
                                    saldoKasLalu = updatedRec.saldoKasLalu,
                                    records = newMap
                                )
                            )
                        )
                    }
                }

                "add_jumat_journal" -> {
                    val desc = payload.stringArg("description").orEmpty().trim()
                    val amount = (payload.stringArg("amount")?.toLongOrNull() ?: payload.intArg("amount")?.toLong() ?: 0L).coerceAtLeast(0L)
                    val type = payload.stringArg("type")?.takeIf { it == "in" || it == "out" } ?: "in"
                    val selectedDate = payload.rawStringArg("selectedDate")?.takeIf { it.isNotBlank() }
                    if (desc.isNotBlank() && amount > 0L) {
                        updateSettings { prev ->
                            val today = _snapshotFlow.value.schedule.date
                            val dateKey = selectedDate ?: prev.jumat.keuangan.selectedDate.ifBlank {
                                JwsStateMachine.findEffectiveJumatKeuanganDate(prev.jumat.keuangan.records, today)
                                    ?: JwsStateMachine.snapToNearestFriday(today).toString()
                            }
                            val existing = prev.jumat.keuangan.records[dateKey] ?: id.algaffar.jwstv.domain.model.JumatKeuanganRecord(
                                saldoKasLalu = prev.jumat.keuangan.saldoKasLalu
                            )
                            val newEntry = id.algaffar.jwstv.domain.model.JumatJournalEntry(
                                id = System.currentTimeMillis().toString(),
                                description = desc,
                                amount = amount,
                                type = type
                            )
                            val updatedEntries = existing.entries + newEntry
                            val totalIn = updatedEntries.filter { it.type == "in" }.sumOf { it.amount }
                            val totalOut = updatedEntries.filter { it.type == "out" }.sumOf { it.amount }
                            val newMap = prev.jumat.keuangan.records.toMutableMap()
                            newMap[dateKey] = existing.copy(pemasukan = totalIn, pengeluaran = totalOut, entries = updatedEntries)
                            prev.copy(
                                jumat = prev.jumat.copy(
                                    keuangan = prev.jumat.keuangan.copy(selectedDate = dateKey, records = newMap)
                                )
                            )
                        }
                    }
                }

                "delete_jumat_journal" -> {
                    val entryId = payload.stringArg("id").orEmpty()
                    val selectedDate = payload.rawStringArg("selectedDate")?.takeIf { it.isNotBlank() }
                    if (entryId.isNotBlank()) {
                        updateSettings { prev ->
                            val today = _snapshotFlow.value.schedule.date
                            val dateKey = selectedDate ?: prev.jumat.keuangan.selectedDate.ifBlank {
                                JwsStateMachine.findEffectiveJumatKeuanganDate(prev.jumat.keuangan.records, today)
                                    ?: JwsStateMachine.snapToNearestFriday(today).toString()
                            }
                            val existing = prev.jumat.keuangan.records[dateKey] ?: return@updateSettings prev
                            val remaining = existing.entries.filterNot { it.id == entryId }
                            val totalIn = remaining.filter { it.type == "in" }.sumOf { it.amount }
                            val totalOut = remaining.filter { it.type == "out" }.sumOf { it.amount }
                            val newMap = prev.jumat.keuangan.records.toMutableMap()
                            newMap[dateKey] = existing.copy(pemasukan = totalIn, pengeluaran = totalOut, entries = remaining)
                            prev.copy(
                                jumat = prev.jumat.copy(
                                    keuangan = prev.jumat.keuangan.copy(selectedDate = dateKey, records = newMap)
                                )
                            )
                        }
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
                        if (clearTakjilNight) takjilMap[nightKey] = emptyList()
                        else if (!addTakjilDonor.isNullOrBlank()) takjilMap[nightKey] = (takjilMap[nightKey].orEmpty() + addTakjilDonor).distinct()

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
                                petugas = f.petugas.copy(khotib = khotib ?: f.petugas.khotib, imam = imam ?: f.petugas.imam, bilal = bilal ?: f.petugas.bilal)
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

                    // Full CRUD payloads for Tabbed Qurban Groups (max 7 members per group)
                    val createGroup = payload.booleanArg("createGroup") == true
                    val groupIdArg = payload.stringArg("groupId")?.trim()
                    val groupNameArg = payload.rawStringArg("groupName", maxLen = 120)
                    val animalTypeArg = payload.stringArg("animalType")?.trim()
                    val membersArg = payload.stringListArg("members", maxItems = id.algaffar.jwstv.domain.model.QurbanGroup.MAX_MEMBERS, maxLen = 120)

                    val updateGroupId = payload.stringArg("updateGroupId")?.trim()
                    val deleteGroupId = payload.stringArg("deleteGroupId")?.trim()
                    val moveGroupId = payload.stringArg("moveGroupId")?.trim()
                    val moveDirection = payload.intArg("moveDirection")

                    val addMemberGroupId = payload.stringArg("addMemberGroupId")?.trim()
                    val updateMemberGroupId = payload.stringArg("updateMemberGroupId")?.trim()
                    val deleteMemberGroupId = payload.stringArg("deleteMemberGroupId")?.trim()
                    val moveMemberGroupId = payload.stringArg("moveMemberGroupId")?.trim()
                    val memberNameArg = payload.rawStringArg("memberName", maxLen = 120)
                    val memberIndexArg = payload.intArg("memberIndex")
                    val moveMemberDelta = payload.intArg("moveMemberDelta")

                    // Legacy add-group CSV payload (backward compatible, capped at 7 members)
                    val addGroupName = payload.stringArg("addGroupName")?.trim()
                    val addAnimalType = payload.stringArg("addAnimalType")?.trim()?.ifBlank { "Sapi" }
                    val addMembersCsv = payload.stringArg("addMembersCsv").orEmpty()

                    updateSettings { prev ->
                        val a = prev.idulAdha
                        var groups = a.qurbanGroups
                        if (!deleteGroupId.isNullOrBlank()) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.deleteGroup(groups, deleteGroupId)
                        }
                        if (createGroup) {
                            val (updated, _) = id.algaffar.jwstv.domain.model.QurbanGroup.addGroup(
                                groups = groups,
                                animalType = animalTypeArg ?: id.algaffar.jwstv.domain.model.QurbanGroup.ANIMAL_SAPI,
                                customName = groupNameArg,
                                initialMembers = membersArg ?: emptyList(),
                                id = groupIdArg?.takeIf { it.isNotBlank() }
                                    ?: "qgrp_${System.currentTimeMillis()}_${groups.size + 1}"
                            )
                            groups = updated
                        }
                        if (!updateGroupId.isNullOrBlank()) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.updateGroup(
                                groups = groups,
                                groupId = updateGroupId,
                                groupName = groupNameArg,
                                animalType = animalTypeArg,
                                members = membersArg
                            )
                        }
                        if (!moveGroupId.isNullOrBlank() && moveDirection != null) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.moveGroup(groups, moveGroupId, moveDirection)
                        }
                        if (!addMemberGroupId.isNullOrBlank() && !memberNameArg.isNullOrBlank()) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.addMember(groups, addMemberGroupId, memberNameArg)
                        }
                        if (!updateMemberGroupId.isNullOrBlank() && memberIndexArg != null && memberNameArg != null) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.updateMember(groups, updateMemberGroupId, memberIndexArg, memberNameArg)
                        }
                        if (!deleteMemberGroupId.isNullOrBlank() && memberIndexArg != null) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.deleteMember(groups, deleteMemberGroupId, memberIndexArg)
                        }
                        if (!moveMemberGroupId.isNullOrBlank() && memberIndexArg != null && moveMemberDelta != null) {
                            groups = id.algaffar.jwstv.domain.model.QurbanGroup.moveMember(groups, moveMemberGroupId, memberIndexArg, moveMemberDelta)
                        }
                        if (!addGroupName.isNullOrBlank()) {
                            val members = addMembersCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            if (members.isNotEmpty()) {
                                val (updated, _) = id.algaffar.jwstv.domain.model.QurbanGroup.addGroup(
                                    groups = groups,
                                    animalType = addAnimalType ?: id.algaffar.jwstv.domain.model.QurbanGroup.ANIMAL_SAPI,
                                    customName = addGroupName,
                                    initialMembers = members
                                )
                                groups = updated
                            }
                        }
                        prev.copy(
                            idulAdha = a.copy(
                                enabled = enabled ?: a.enabled,
                                petugas = a.petugas.copy(khotib = khotib ?: a.petugas.khotib, imam = imam ?: a.petugas.imam, bilal = bilal ?: a.petugas.bilal),
                                qurbanGroups = groups
                            ),
                            infoQurban = prev.infoQurban.copy(enabled = infoQurbanEnabled ?: prev.infoQurban.enabled)
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
                        val updatedFirst = first.copy(startTime = startTime ?: first.startTime, endTime = endTime ?: first.endTime)
                        if (curList.isEmpty()) curList.add(updatedFirst) else curList[0] = updatedFirst
                        prev.copy(screensaver = prev.screensaver.copy(enabled = enabled ?: prev.screensaver.enabled, schedules = curList))
                    }
                }

                "simulate_state" -> {
                    val stateName = payload.stringArg("state").orEmpty()
                    val durationSec = payload.intArg("durationSeconds")?.toLong()?.coerceIn(10L, 3600L) ?: 300L
                    val target = AppState.fromSerialName(stateName)
                    if (target == AppState.STANDBY) clearSimulation() else simulateState(target, durationSec)
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
                                    switches = prev.switchSettings.switches.map { ch -> if (ch.id == id) ch.copy(enabled = !ch.enabled) else ch }
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
                                                            ?.takeIf { !it.isString || it.content.isNotBlank() }?.content?.toIntOrNull()
                                                        val offVal = (pObj["offOffset"] as? kotlinx.serialization.json.JsonPrimitive)
                                                            ?.takeIf { !it.isString || it.content.isNotBlank() }?.content?.toIntOrNull()
                                                        map[pid] = id.algaffar.jwstv.domain.model.PrayerSwitchSchedule(onOffset = onVal, offOffset = offVal)
                                                    }
                                                }
                                                map
                                            } else ch.schedules
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
                        val newHash = AuthManager.hashPinPbkdf2(newPin)
                        updateSettings { prev ->
                            val existing = prev.users.toMutableList()
                            val idx = existing.indexOfFirst { it.role.equals(roleId, ignoreCase = true) }
                            if (idx >= 0) {
                                existing[idx] = existing[idx].copy(pinHash = newHash)
                            } else {
                                existing.add(
                                    UserAccount(
                                        id = "$roleId-${System.currentTimeMillis()}",
                                        username = roleId,
                                        pinHash = newHash,
                                        role = roleId
                                    )
                                )
                            }
                            prev.copy(users = existing)
                        }
                        AppLogger.info("AUTH", "PIN akun '$roleId' diperbarui (PBKDF2) melalui Remote PWA")
                    }
                }

                "clear_logs" -> AppLogger.clear()
            }
        }
    }

    private fun appendSlidesToGroup(
        existingGroups: List<SlideshowGroup>,
        targetGroupId: String?,
        newSlides: List<SlideItem>
    ): List<SlideshowGroup> {
        if (newSlides.isEmpty()) return existingGroups
        if (existingGroups.isEmpty()) {
            return listOf(
                SlideshowGroup(
                    id = "group_${System.currentTimeMillis()}",
                    name = "Group 1",
                    enabled = true,
                    slides = newSlides,
                    overlayPosition = SlideshowGroup.POS_BOTTOM_LEFT,
                    showQrCard = true,
                    showInMain = true,
                    showInSlideshow = true
                )
            )
        }
        val targetIdx = if (!targetGroupId.isNullOrBlank()) {
            existingGroups.indexOfFirst { it.id == targetGroupId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
        return existingGroups.mapIndexed { idx, grp ->
            if (idx == targetIdx) grp.copy(slides = grp.slides + newSlides) else grp
        }
    }
}
