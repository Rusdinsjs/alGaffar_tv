package id.algaffar.jwstv.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.algaffar.jwstv.MainActivity

/**
 * Automatically launches JWS TV Masjid Al-Gaffar when the Android TV / STB powers on.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            JwsForegroundService.startServiceSafely(context)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            runCatching {
                context.startActivity(launchIntent)
            }
        }
    }
}
