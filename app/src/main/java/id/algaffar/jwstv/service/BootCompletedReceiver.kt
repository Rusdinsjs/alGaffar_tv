package id.algaffar.jwstv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import id.algaffar.jwstv.MainActivity
import id.algaffar.jwstv.data.logging.AppLogger

/**
 * Automatically starts the 24/7 Foreground Service and launches [MainActivity] when the STB powers on.
 * Notes on Android 10+ (BAL restrictions): launching an Activity from BOOT_COMPLETED succeeds
 * when the app is set as Default Home Launcher OR has "Display over other apps" (`SYSTEM_ALERT_WINDOW`) granted.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            AppLogger.info("BOOT", "Perangkat menyala ($action) — memulai Layanan Al-Gaffar TV 24/7")
            JwsForegroundService.startServiceSafely(context)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            runCatching {
                context.startActivity(launchIntent)
            }.onFailure { err ->
                val canOverlay = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
                AppLogger.warn(
                    "BOOT",
                    "Auto-launch layar utama tertahan sistem (overlay=$canOverlay): ${err.message}. " +
                        "Aktifkan izin 'Tampil di atas aplikasi lain' atau jadikan Al-Gaffar TV sebagai Home Launcher."
                )
            }
        }
    }
}
