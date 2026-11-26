package id.algaffar.jwstv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.algaffar.jwstv.AppContainer
import id.algaffar.jwstv.data.cctv.FinancialSummaryData
import id.algaffar.jwstv.data.excel.TakjilImportPreviewRow
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.data.update.OtaCheckResult
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.engine.FastingInfoSummary
import id.algaffar.jwstv.domain.engine.HijriDateInfo
import id.algaffar.jwstv.domain.engine.IslamicEventItem
import id.algaffar.jwstv.domain.engine.StateMachineTickResult
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.runtime.JwsRuntimeEngine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.ZonedDateTime

data class RemoteUiInfo(
    val remoteServerUrl: String = "http://127.0.0.1:1421/remote",
    val remoteQrPairingUrl: String = "http://127.0.0.1:1421/remote",
    val remoteAuthToken: String = "",
    val deviceCode: String = ""
)

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
    val remoteAuthToken: String = "",
    val deviceCode: String = "",
    val isClockSuspect: Boolean = false
)

/**
 * Thin UI Facade ViewModel delegating all 24/7 background operations to the process-lifetime
 * [JwsRuntimeEngine]. Clearing this ViewModel on Activity recreation never interrupts
 * Ktor port 1421, prayer audio playback, or relay automation.
 */
class MainViewModel(
    private val engine: JwsRuntimeEngine
) : ViewModel() {

    val assetsRootDir: File get() = engine.assetsRootDir
    val remoteServer: KtorRemoteServer get() = engine.remoteServer
    val deviceCode: String get() = engine.deviceCode

    val snapshotFlow: StateFlow<JwsRuntimeSnapshot> = engine.snapshotFlow
    val settingsFlow: StateFlow<AppStateSettings> = engine.settingsRepository.settingsFlow
    val currentHourFlow: StateFlow<Int> = engine.currentHourFlow
    val activeAppStateFlow: StateFlow<AppState> = engine.activeAppStateFlow
    val remoteUiInfoFlow: StateFlow<RemoteUiInfo> = engine.remoteUiInfoFlow
    val storageHealthFlow: StateFlow<StorageHealthSnapshot> = engine.storageHealthFlow
    val statusBannerMessage: StateFlow<String> = engine.statusBannerMessage
    val takjilPreviewFlow: StateFlow<List<TakjilImportPreviewRow>?> = engine.takjilPreviewFlow
    val relayPinStatesFlow: StateFlow<Map<Int, Boolean>> = engine.relayPinStatesFlow
    val openSettingsRequestCount: StateFlow<Int> = engine.openSettingsRequestCount
    val remoteNavKeyEvents: SharedFlow<Int> = engine.remoteNavKeyEvents
    val onlineFinanceTestResult: StateFlow<FinancialSummaryData?> = engine.onlineFinanceTestResult
    val audioInventoryTick: StateFlow<Int> = engine.audioInventoryTick
    val previewingAudioKey: StateFlow<String?> = engine.previewingAudioKey
    val otaCheckResultFlow: StateFlow<OtaCheckResult?> = engine.otaCheckResultFlow

    init {
        engine.ensureStarted()
    }

    fun isManualOverrideNow(): Boolean = engine.snapshotFlow.value.tickResult.isManualOverride
    fun postStatusMessage(msg: String) = engine.postStatusMessage(msg)
    fun refreshStorageHealth() = engine.refreshStorageHealth()
    fun refreshAudioDurationsAsync() = engine.refreshAudioDurationsAsync()

    fun simulateState(state: AppState?, durationSeconds: Long? = 120L) = engine.simulateState(state, durationSeconds)
    fun clearSimulation() = engine.clearSimulation()
    fun testPlayBeep1000Hz() = engine.testPlayBeep1000Hz()
    fun testPlayRamadhanSirine() = engine.testPlayRamadhanSirine()
    fun toggleManualRelay(pin: Int, turnOn: Boolean) = engine.toggleManualRelay(pin, turnOn)

    fun cycleTheme() = engine.cycleTheme()
    fun cycleLayout() = engine.cycleLayout()
    fun cycleClockStyle() = engine.cycleClockStyle()
    fun startDirectLive(source: String, customId: String, durationMinutes: Int) =
        engine.startDirectLive(source, customId, durationMinutes)
    fun stopDirectLive() = engine.stopDirectLive()
    fun testOnlineFinanceConnection() = engine.testOnlineFinanceConnection()

    fun regenerateRemoteToken() = engine.regenerateRemoteToken()
    fun resetSuperadminPin(newPin: String) = engine.resetSuperadminPin(newPin)
    fun upgradeUserPinHash(userId: String, rawPin: String) = engine.upgradeUserPinHash(userId, rawPin)
    fun requestUsbRelayPermission() = engine.requestUsbRelayPermission()
    fun checkOtaUpdate() = engine.checkOtaUpdate()
    fun installOfflineApk(inputStream: InputStream) = engine.installOfflineApk(inputStream)
    fun downloadAudioPack(packId: String) = engine.downloadAudioPack(packId)
    fun triggerAppRestartNow() = engine.triggerAppRestartNow()
    fun triggerDeviceRebootNow(): Boolean = engine.triggerDeviceRebootNow()
    fun setOnBeforeLaunchExternalInstaller(callback: (() -> Unit)?) {
        engine.onBeforeLaunchExternalInstaller = callback
    }

    fun findPrayerAudioSlot(category: String, slotName: String): Pair<Boolean, String> =
        engine.findPrayerAudioSlot(category, slotName)
    fun listCustomAudioFilesWithDurations(): List<Pair<String, String>> =
        engine.listCustomAudioFilesWithDurations()
    fun togglePreviewPrayerAudio(category: String, slotName: String) =
        engine.togglePreviewPrayerAudio(category, slotName)
    fun togglePreviewCustomAudio(fileName: String) =
        engine.togglePreviewCustomAudio(fileName)
    fun stopAudioPreview() = engine.stopAudioPreview()

    fun importPrayerAudioSlot(category: String, slotName: String, inputStream: InputStream) =
        engine.importPrayerAudioSlot(category, slotName, inputStream)
    fun deletePrayerAudioSlot(category: String, slotName: String) =
        engine.deletePrayerAudioSlot(category, slotName)
    fun importCustomAudioFile(rawName: String, inputStream: InputStream) =
        engine.importCustomAudioFile(rawName, inputStream)
    fun deleteCustomAudioFile(fileName: String) = engine.deleteCustomAudioFile(fileName)
    fun importImageAsset(subFolder: String, fileName: String, inputStream: InputStream, onSaved: (String) -> Unit) =
        engine.importImageAsset(subFolder, fileName, inputStream, onSaved)
    fun importVideoAsset(fileName: String, inputStream: InputStream, onSaved: (String, String) -> Unit) =
        engine.importVideoAsset(fileName, inputStream, onSaved)
    fun syncMediaFromExternalFolder(sourceDir: File) = engine.syncMediaFromExternalFolder(sourceDir)
    fun cleanOrphanMedia() = engine.cleanOrphanMedia()
    fun updateSettings(transform: (AppStateSettings) -> AppStateSettings) = engine.updateSettings(transform)

    fun exportTakjilExcelTemplate(outputStream: OutputStream) = engine.exportTakjilExcelTemplate(outputStream)
    fun parseTakjilExcelPreview(inputStream: InputStream) = engine.parseTakjilExcelPreview(inputStream)
    fun confirmTakjilImport() = engine.confirmTakjilImport()
    fun dismissTakjilImport() = engine.dismissTakjilImport()

    fun exportFullBackupZip(outputStream: OutputStream) = engine.exportFullBackupZip(outputStream)
    fun importFullBackupZip(inputStream: InputStream) = engine.importFullBackupZip(inputStream)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container.runtimeEngine) as T
                }
            }
    }
}
