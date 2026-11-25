package id.algaffar.jwstv.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import id.algaffar.jwstv.JwsApplication
import id.algaffar.jwstv.MainActivity

/**
 * 24/7 Kiosk Foreground Service for Al-Gaffar TV on Android TV / STB.
 * Ensures [id.algaffar.jwstv.runtime.JwsRuntimeEngine] (1Hz prayer state engine,
 * Media3 audio engine, Ktor port 1421 server, and 8-channel relay controller)
 * stays alive continuously even if [MainActivity] is recreated or paused.
 */
class JwsForegroundService : Service() {

    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireKioskLocks()
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
        (application as? JwsApplication)?.container?.runtimeEngine?.ensureStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (application as? JwsApplication)?.container?.runtimeEngine?.ensureStarted()
        return START_STICKY
    }

    override fun onDestroy() {
        releaseKioskLocks()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun acquireKioskLocks() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && cpuWakeLock == null) {
                cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JwsTv::KioskCpuWakeLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        }
        runCatching {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm != null && wifiLock == null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "JwsTv::KioskWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        }
    }

    private fun releaseKioskLocks() {
        runCatching {
            cpuWakeLock?.let { if (it.isHeld) it.release() }
            cpuWakeLock = null
        }
        runCatching {
            wifiLock?.let { if (it.isHeld) it.release() }
            wifiLock = null
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Layanan Al-Gaffar TV 24/7",
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
            .setContentTitle("Al-Gaffar TV Aktif 24/7")
            .setContentText("Mesin Waktu Sholat, Audio Adzan, Saklar Relay & Remote Server (Port 1421) Berjalan")
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
