package id.algaffar.jwstv

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Process
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import id.algaffar.jwstv.data.audio.JwsAudioEngine
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.logging.AppLogger
import id.algaffar.jwstv.data.media.AudioPackDownloader
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.data.server.RemoteTokenStore
import id.algaffar.jwstv.data.update.OtaUpdateManager
import id.algaffar.jwstv.domain.engine.ClockIntegrityGuard
import id.algaffar.jwstv.domain.engine.HijriCalculator
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.runtime.JwsRuntimeEngine
import okhttp3.OkHttpClient
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shared singleton [OkHttpClient] pool to prevent creating duplicate thread pools
 * and connection pools across [OnlineFinanceManager], [OtaUpdateManager], and [SwitchController].
 */
object SharedHttpClient {
    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val relayClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build()
    }
}

class AppContainer(application: Application) {
    val appContext: Context = application.applicationContext

    val assetsRootDir: File = File(application.filesDir, "jws-assets").apply {
        mkdirs()
        listOf("tartil", "tarhim", "adzan").forEach { cat ->
            File(this, "audio/$cat/default").mkdirs()
        }
        File(this, "images").mkdirs()
        File(this, "videos").mkdirs()
    }
    private val settingsFile: File = File(application.filesDir, "jws_settings.json")
    private val clockAnchorFile: File = File(application.filesDir, ".jws_clock_anchor")

    val settingsRepository: SettingsRepository = SettingsRepository(settingsFile)
    val tokenStore: RemoteTokenStore = RemoteTokenStore(application.filesDir, appContext)
    val clockIntegrityGuard: ClockIntegrityGuard = ClockIntegrityGuard(clockAnchorFile)
    val prayerTimeCalculator: PrayerTimeCalculator = PrayerTimeCalculator()
    val hijriCalculator: HijriCalculator = HijriCalculator()
    val stateMachine: JwsStateMachine = JwsStateMachine(prayerTimeCalculator)
    val audioEngine: JwsAudioEngine = JwsAudioEngine(appContext, assetsRootDir)
    val switchController: SwitchController = SwitchController(appContext)
    val otaUpdateManager: OtaUpdateManager = OtaUpdateManager(appContext)
    val audioPackDownloader: AudioPackDownloader = AudioPackDownloader()

    val runtimeEngine: JwsRuntimeEngine = JwsRuntimeEngine(
        appContext = appContext,
        assetsRootDir = assetsRootDir,
        settingsRepository = settingsRepository,
        tokenStore = tokenStore,
        clockIntegrityGuard = clockIntegrityGuard,
        prayerTimeCalculator = prayerTimeCalculator,
        hijriCalculator = hijriCalculator,
        stateMachine = stateMachine,
        audioEngine = audioEngine,
        switchController = switchController,
        otaUpdateManager = otaUpdateManager,
        audioPackDownloader = audioPackDownloader
    )
}

class JwsApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    @Volatile
    private var lastGcTrimMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        installCrashRecoveryHandler()
        container = AppContainer(this)
        container.runtimeEngine.ensureStarted()
    }

    private fun installCrashRecoveryHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText(
                    "Time: ${Instant.now()}\nThread: ${thread.name}\n\n$sw",
                    Charsets.UTF_8
                )

                val markerFile = File(filesDir, ".last_crash_restart_ms")
                val nowMs = System.currentTimeMillis()
                val prevRestartMs = if (markerFile.exists()) {
                    markerFile.readText(Charsets.UTF_8).trim().toLongOrNull() ?: 0L
                } else {
                    0L
                }
                val elapsed = nowMs - prevRestartMs
                // Guard against tight crash loops: if crashed within 15s of last restart, delay 15s
                val delayMs = if (prevRestartMs > 0L && elapsed in 0L..15_000L) 15_000L else 2_000L
                markerFile.writeText(nowMs.toString(), Charsets.UTF_8)

                scheduleAppRestart(applicationContext, delayMs)
            }.onFailure {
                defaultHandler?.uncaughtException(thread, throwable)
            }

            Process.killProcess(Process.myPid())
            exitProcess(2)
        }
    }

    override fun newImageLoader(): ImageLoader {
        val isPerf = if (::container.isInitialized) {
            container.settingsRepository.currentSettings.performanceMode
        } else {
            true
        }
        return ImageLoader.Builder(this)
            .allowRgb565(isPerf)
            .crossfade(!isPerf)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(if (isPerf) 0.12 else 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(if (isPerf) 32L * 1024L * 1024L else 64L * 1024L * 1024L)
                    .build()
            }
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                performMemoryRecovery("onTrimMemory($level)", aggressive = true)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                performMemoryRecovery("onTrimMemory($level)", aggressive = false)
            }
        }
    }

    fun performMemoryRecovery(reason: String, aggressive: Boolean) {
        runCatching {
            Coil.imageLoader(this).memoryCache?.clear()
        }
        if (aggressive) {
            if (::container.isInitialized) {
                runCatching { container.audioEngine.releaseIfIdle() }
            }
            AppLogger.trimTo(50)
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastGcTrimMs >= 60_000L) {
                lastGcTrimMs = nowMs
                AppLogger.warn("MEMORY", "Pemulihan memori agresif dijalankan ($reason)")
                Runtime.getRuntime().gc()
            }
        } else {
            AppLogger.debug("MEMORY", "Trim cache gambar dijalankan ($reason)")
        }
    }

    companion object {
        private const val RESTART_REQUEST_CODE = 14210

        fun scheduleAppRestart(context: Context, delayMs: Long = 1_500L) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                RESTART_REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + delayMs,
                    pendingIntent
                )
            } else {
                context.startActivity(launchIntent)
            }
        }

        fun restartAppNow(context: Context, delayMs: Long = 1_000L) {
            runCatching { scheduleAppRestart(context, delayMs) }
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }
}
