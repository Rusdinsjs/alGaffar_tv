package id.algaffar.jwstv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import id.algaffar.jwstv.MainActivity

/**
 * 24/7 Kiosk Foreground Service for Android TV / STB / Mini PC.
 * Prevents the OS from reclaiming the JWS TV process during long idle periods,
 * keeping the 1Hz prayer state engine, Media3 audio engine, Ktor port 1421 server,
 * and 8-channel relay controller active continuously.
 */
class JwsForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        val notification = buildOngoingNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Graceful fallback if started while background restrictions apply
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan JWS TV Masjid Al-Gaffar 24/7",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Menjaga jadwal sholat, adzan otomatis, saklar relay, dan remote server tetap aktif 24/7."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildOngoingNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("JWS TV Masjid Al-Gaffar Aktif")
            .setContentText("Mesin Waktu Sholat, Audio Adzan & Remote Server (Port 1421) Berjalan")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "jws_tv_kiosk_channel"
        private const val NOTIFICATION_ID = 1421

        fun startServiceSafely(context: Context) {
            runCatching {
                val intent = Intent(context, JwsForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
