package id.algaffar.jwstv

import android.app.Application
import android.content.Context
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

class JwsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
