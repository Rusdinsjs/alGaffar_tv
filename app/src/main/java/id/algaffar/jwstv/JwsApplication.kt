package id.algaffar.jwstv

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import id.algaffar.jwstv.data.audio.JwsAudioEngine
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.repository.SettingsRepository
import id.algaffar.jwstv.domain.engine.HijriCalculator
import id.algaffar.jwstv.domain.engine.JwsStateMachine
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import java.io.File

class AppContainer(application: Application) {
    val appContext: Context = application.applicationContext

    val assetsRootDir: File = File(application.filesDir, "jws-assets").apply {
        mkdirs()
        // Pre-create standard audio directories for Tartil, Tarhim, Adzan & Sirine
        listOf("tartil", "tarhim", "adzan").forEach { cat ->
            File(this, "audio/$cat/default").mkdirs()
        }
        File(this, "images").mkdirs()
        File(this, "videos").mkdirs()
    }
    private val settingsFile: File = File(application.filesDir, "jws_settings.json")

    val settingsRepository: SettingsRepository = SettingsRepository(settingsFile)
    val prayerTimeCalculator: PrayerTimeCalculator = PrayerTimeCalculator()
    val hijriCalculator: HijriCalculator = HijriCalculator()
    val stateMachine: JwsStateMachine = JwsStateMachine(prayerTimeCalculator)
    val audioEngine: JwsAudioEngine = JwsAudioEngine(appContext, assetsRootDir)
    val switchController: SwitchController = SwitchController(appContext)
}

class JwsApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
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
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
        ) {
            runCatching {
                Coil.imageLoader(this).memoryCache?.clear()
            }
        }
    }
}
